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

    public static String getChecksum(String filename)
            throws NoSuchAlgorithmException, IOException
    {
        File file = new File(filename);
        FileInputStream fis = new FileInputStream(file);
        MessageDigest md5Digest = MessageDigest.getInstance("MD5");
        byte[] byteArray = new byte[1024];
        int byteCount = 0;

        while ((byteCount = fis.read(byteArray)) != -1)
        {
            md5Digest.update(byteArray, 0, byteCount);
        }
        fis.close();
        byte[] bytes = md5Digest.digest();
        StringBuilder sb = new StringBuilder();

        for(int i=0; i<bytes.length; i++)
        {
            sb.append(Integer.toString((bytes[i]&0xff)+0x100, 16).substring(1));
        }

        return sb.toString();
    }

    public static void send(String filename, Socket socket)
            throws IOException, NoSuchAlgorithmException
    {
        File file = new File(filename);
        OutputStream os = socket.getOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(os);
        InputStream is = socket.getInputStream();
        InputStreamReader isr = new InputStreamReader(is);
        BufferedReader br = new BufferedReader(isr);
        FileInputStream fin = new FileInputStream(file);
        BufferedInputStream bin = new BufferedInputStream(fin);

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

        while (sequence != endsequence)
        {
            size = packetsize;
            if (endsequence - sequence >= size)
                sequence += size;
            else
            {
                size = (int) (endsequence - sequence);
                sequence = endsequence;
            }
            byte[] packet = new byte[size];
            bin.read(packet, 0, size);
            Header testheader = new Header(packet, sequence, checksum);

            buffer.add(testheader);

            System.out.println("Sending packet #" + testheader.num);
            oos.writeObject(testheader);
            if ((message = br.readLine()) != null)
            {
                System.out.println("\t\t\t\t\t\"" + message + "\"");
                if ((System.nanoTime() - starttime) > timeout)
                {
                    timeout(message, testheader.num, buffer, oos, socket);
                }
            }
        }
        System.out.println("\"" + filename + "\" sent successfully to "
                + socket.getRemoteSocketAddress());
        oos.writeObject(null);
        bin.close();
        buffer.clear();
    }


    public static void receive(String filename, Socket socket)
            throws IOException, ClassNotFoundException, NoSuchAlgorithmException
    {
        System.out.println("In RECEIVE TCP");
        FileOutputStream fout = new FileOutputStream("receivedTCP " + filename);
        BufferedOutputStream bout = new BufferedOutputStream(fout);
        InputStream is = socket.getInputStream();
        ObjectInputStream ois = new ObjectInputStream(is);
        OutputStream os = socket.getOutputStream();
        OutputStreamWriter osw = new OutputStreamWriter(os);
        BufferedWriter bw = new BufferedWriter(osw);

        Header testheader = null;
        ArrayList<Header> buffer = new ArrayList<Header>();
        String testchecksum = null;

        int drop = 0;
        long ack = 0;
        while ((testheader = (Header) ois.readObject()) != null)
        {
            drop++;

            if (drop != 3)
            {
                System.out.println("Received packet #" + testheader.num);
                buffer.add(testheader);
                Collections.sort(buffer, Header.compareheader);

                if (testheader.num == ack || ack == 0)
                {
                    ack = buffer.get(buffer.size() - 1).num + packetsize;
                }
                System.out.println("\t\t\t\t\t Sending ACK #" + ack);
            }
            bw.write("ACK " + ack + "\n");
            bw.flush();

            testchecksum = testheader.checksum;
        }
        for (Header tempheader : buffer)
            bout.write(tempheader.payload, 0, tempheader.payload.length);
        bout.close();
        buffer.clear();

        String checksum = getChecksum("receivedTCP " + filename);
        System.out.println("Testing Checksum");
        if (checksum.equals(testchecksum))
        {
            System.out.println("Checksum matches.");
            bw.write("Checksum matches.\n\n");
            bw.flush();
        }
        else
        {
            System.out.println("Checksum doesn't match.");
            bw.write("Checksum doesn't match.\n\n");
            bw.flush();
        }
        System.out.println(
                "\"received " + filename + "\" saved successfully from "
                        + socket.getRemoteSocketAddress() + "\n");
        bout.close();
    }


    public static void timeout(String message, long seq,
                               ArrayList<Header> buffer, ObjectOutputStream oos, Socket socket)
            throws IOException
    {
        InputStream is = socket.getInputStream();
        InputStreamReader isr = new InputStreamReader(is);
        BufferedReader br = new BufferedReader(isr);

        System.out.println("\t\t\tTIMEOUT\n");
        starttime = System.nanoTime();

        String[] temp = message.split(" ", 2);
        long ack = Long.parseLong(temp[1]);
        if ((ack - seq) < 0)
        {
            for (Header t : buffer)
            {
                if (t.num == ack)
                {
                    System.out.println("Retransmitting packet #" + t.num);
                    oos.writeObject(t);
                    if ((message = br.readLine()) != null)
                    {
                        System.out.println("\t\t\t\t\t\"" + message + "\"");
                    }
                }
            }
        }
    }
}
