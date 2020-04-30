import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

public class FTP
{
    private static final int packetsize = 1000;
    private static final long timeout = 500000000;
    private static long starttime = 0;

    //this function calculates the checksum of the file
    public static String getChecksum(String filename)
            throws NoSuchAlgorithmException, IOException
    {
        //read the file
        File file = new File(filename);
        FileInputStream fis = new FileInputStream(file);
        //use md5 to make a checksum
        MessageDigest md5Digest = MessageDigest.getInstance("MD5");
        byte[] byteArray = new byte[1024];
        int byteCount = 0;

        while ((byteCount = fis.read(byteArray)) != -1)
        {
            md5Digest.update(byteArray, 0, byteCount);
        }
        fis.close();
        byte[] bytes = md5Digest.digest();
        //write checksum
        StringBuilder sb = new StringBuilder();

        for(int i=0; i<bytes.length; i++)
        {
            sb.append(Integer.toString((bytes[i]&0xff)+0x100, 16).substring(1));
        }

        return sb.toString();
    }
    //this function is used to send a file over sockets
    public static void send(String filename, Socket socket)
            throws IOException, NoSuchAlgorithmException
    {
        //open the file and required streams
        File file = new File(filename);
        OutputStream outputStream = socket.getOutputStream();
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);
        InputStream inputStream = socket.getInputStream();
        InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
        FileInputStream fileInputStream = new FileInputStream(file);
        BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream);

        ArrayList<Header> buffer = new ArrayList<Header>();
        Random rand = new Random();

        String message = null;
        String checksum = getChecksum(filename);
        int size = 0;
        long filelen = file.length();
        long sequence = (long) rand.nextInt(Integer.SIZE - 1) + 1;
        long endsequence = sequence + filelen;
        starttime = System.nanoTime();

        System.out.println("Sending " + filename);
        //start sending the file
        //if sequence = endsequence, reached the end
        while (sequence != endsequence)
        {
            //packet size remains PACKETSIZE except for the last packet for that file, which can be smaller
            size = packetsize;
            if (endsequence - sequence >= size)
                sequence += size;
            else
            {
                size = (int) (endsequence - sequence);
                sequence = endsequence;
            }
            //make a packet to send
            byte[] packet = new byte[size];
            bufferedInputStream.read(packet, 0, size);
            //add the packet to a header format
            Header testheader = new Header(packet, sequence, checksum);

            //add the header+packet to the buffer
            buffer.add(testheader);

            System.out.println("Sending packet #" + testheader.num);
            //write the object to socket(send it)
            objectOutputStream.writeObject(testheader);

            //check for ACKS. (wait for a message)
            if ((message = bufferedReader.readLine()) != null)
            {
                //if ACK is sent, print it
                System.out.println("\t\t\t\t\t\"" + message + "\"");
                //if ACK is sent too late, TIMEOUT
                if ((System.nanoTime() - starttime) > timeout)
                {
                    timeout(message, testheader.num, buffer, objectOutputStream, socket);
                }
            }
        }
        //file is sent
        System.out.println("\"" + filename + "\" sent successfully to "
                + socket.getRemoteSocketAddress());
        objectOutputStream.writeObject(null);
        bufferedInputStream.close();
        buffer.clear();
    }

    //this function is used to receive a file at the client when a server is sending it
    public static void receive(String filename, Socket socket)
            throws IOException, ClassNotFoundException, NoSuchAlgorithmException
    {
        //declare streams
        System.out.println("In RECEIVE TCP");
        FileOutputStream fileOutputStream = new FileOutputStream("receivedTCP " + filename);
        BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream);
        InputStream inputStream = socket.getInputStream();
        ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);
        OutputStream outputStream = socket.getOutputStream();
        OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream);
        BufferedWriter bufferedWriter = new BufferedWriter(outputStreamWriter);

        Header testheader = null;
        ArrayList<Header> buffer = new ArrayList<Header>();
        String testchecksum = null;

        //counters for drops and acks
        int drop = 0;
        long ack = 0;
        //read the packet(testheader variable is actually used for the header+packet as seen in send)
        while ((testheader = (Header) objectInputStream.readObject()) != null)
        {
            //cant let 3+ drop
            drop++;

            if (drop != 3)
            {
                //print that a packet was received
                System.out.println("Received packet #" + testheader.num);
                //add received packet to a buffer (Later then written to a file)
                buffer.add(testheader);
                Collections.sort(buffer, Header.compareheader);

                //make an ACK to send to server
                if (testheader.num == ack || ack == 0)
                {
                    ack = buffer.get(buffer.size() - 1).num + packetsize;
                }
                System.out.println("\t\t\t\t\t Sending ACK #" + ack);
            }
            //send ACK to server that the packet was received
            bufferedWriter.write("ACK " + ack + "\n");
            bufferedWriter.flush();

            testchecksum = testheader.checksum;
        }
        //This part checks if the received file has the right checksum
        //Write the fileContents from the buffer to the file
        for (Header tempheader : buffer)
            bufferedOutputStream.write(tempheader.payload, 0, tempheader.payload.length);
        bufferedOutputStream.close();
        buffer.clear();
        //get checksum of the saved (received) file
        String checksum = getChecksum("receivedTCP " + filename);
        System.out.println("Testing Checksum");
        //compare checksum to the one sent by the server, and send messages accordingly
        if (checksum.equals(testchecksum))
        {
            System.out.println("Checksum matches.");
            bufferedWriter.write("Checksum matches.\n\n");
        }
        else
        {
            System.out.println("Checksum doesn't match.");
            bufferedWriter.write("Checksum doesn't match.\n\n");
        }
        bufferedWriter.flush();
        System.out.println(
                "\"received " + filename + "\" saved successfully from "
                        + socket.getRemoteSocketAddress() + "\n");
        bufferedOutputStream.close();
    }

    //this function handles what happens if theres a timeout
    public static void timeout(String message, long seq,
                               ArrayList<Header> buffer, ObjectOutputStream oos, Socket socket)
            throws IOException
    {
        //declare streams
        InputStream inputStream = socket.getInputStream();
        InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

        //tell server there was a timeout
        System.out.println("\t\t\tTIMEOUT\n");
        starttime = System.nanoTime();

        //retransmit the packet
        String[] temp = message.split(" ", 2);
        long ack = Long.parseLong(temp[1]);

        //ack<seq if file wasn't successfully received by the client
        if ((ack - seq) < 0)
        {
            for (Header t : buffer)
            {
                //find the exact packet lost in the buffer, and retransmit it
                if (t.num == ack)
                {
                    System.out.println("Retransmitting packet #" + t.num);
                    oos.writeObject(t);
                    if ((message = bufferedReader.readLine()) != null)
                    {
                        System.out.println("\t\t\t\t\t\"" + message + "\"");
                    }
                }
            }
        }
    }
}