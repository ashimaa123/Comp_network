
/*
 * Filename     FTP.java
 * Date         5/1/2020
 * Author       Ashima Soni, Mira Jambusaria
 * Email        ashima.soni@utdallas.edu mmj170530@utdallas.edu
 * Course       CE 4390.502 Spring 2020
 * Version      1.0
 * Copyright    2020, All Rights Reserved
 *
 * Description
 *
 * This is the file that handles the File Transfer Protocol
 *
 */

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

/*
 *class FTP 
 *Description: this class handles the file transfer protocols 
 */
public class FTP
{
    /* hardcode packet size */ 
    private static final int packetsize = 1000;
    /* hardcode timeout */ 
    private static final long timeout = 500000000;
    /* hardcode starttime */ 
    private static long starttime = 0;

    //use this to cacluate checksum 
    public static String getChecksum(String filename)
            throws NoSuchAlgorithmException, IOException
    {
        /* create a file to hold the filename */ 
        File file = new File(filename);
        FileInputStream fis = new FileInputStream(file);
        
        /* use to verify the file */ 
        MessageDigest md5Digest = MessageDigest.getInstance("MD5");
        
        byte[] byteArray = new byte[1024];
        int byteCount = 0;
        
        /* count through the number of bytes  and read the file*/ 
        while ((byteCount = fis.read(byteArray)) != -1)
        {
            md5Digest.update(byteArray, 0, byteCount);
        }
        fis.close();
        byte[] bytes = md5Digest.digest();
        
        /*build the checksum */ 
        StringBuilder sb = new StringBuilder();

        for(int i=0; i<bytes.length; i++)
        {
            sb.append(Integer.toString((bytes[i]&0xff)+0x100, 16).substring(1));
        }

        return sb.toString();
    }
    
    /*
     *void send 
     *Description:          The purpose of this function is to send the file from the Server to the Clinet 
     */
    public static void send(String filename, Socket socket)
            throws IOException, NoSuchAlgorithmException
    {
        /*create and open the file  */ 
        File file = new File(filename);
        /* create a new output stream */ 
        OutputStream outputStream = socket.getOutputStream();
        /* object output stream */ 
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);
        
        /* create input object stream to read through the project  */ 
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
        /* begin sending file */
        
        while (sequence != endsequence)
        {
            /* the packet size will remain consistant */ 
            size = packetsize;
            if (endsequence - sequence >= size)
                sequence += size;
            else
            {
                size = (int) (endsequence - sequence);
                sequence = endsequence;
            }
            /* create the packet */ 
            byte[] packet = new byte[size];
            bufferedInputStream.read(packet, 0, size);
            /* append the packet to the header*/ 
            Header testheader = new Header(packet, sequence, checksum);

            /* add to the buffer */ 
            buffer.add(testheader);

            System.out.println("Sending packet #" + testheader.num);
            
            objectOutputStream.writeObject(testheader);

            /* wait for acknowledgement */ 
            if ((message = bufferedReader.readLine()) != null)
            {
                /* print out acknoledgement */  
                System.out.println("\t\t\t\t\t\"" + message + "\"");
                /* if ACK is not returned within timeout, send timeout message */ 
                if ((System.nanoTime() - starttime) > timeout)
                {
                    timeout(message, testheader.num, buffer, objectOutputStream, socket);
                }
            }
        }
        
        /* send file  */ 
        System.out.println("\"" + filename + "\" sent successfully to "
                + socket.getRemoteSocketAddress());
  
        objectOutputStream.writeObject(null);
        bufferedInputStream.close();
        buffer.clear();
    }

    /*
     *void receive 
     *Description:          The purpose of this function is to receive the file from the Client to Server 
     */
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
