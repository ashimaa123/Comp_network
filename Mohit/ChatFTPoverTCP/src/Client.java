/*
 * Filename     Client.java
 * Date         5/1/2020
 * Author       Ashima Soni, Mira Jambusaria
 * Email        ashima.soni@utdallas.edu mmj170530@utdallas.edu
 * Course       CE 4390.502 Spring 2020
 * Version      1.0
 * Copyright    2020, All Rights Reserved
 *
 * Description
 *
 * This is the file that creates the Client side connection
 *
 */
import java.io.*;
import java.net.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/*
 * Class Client
 * Description:         Handles all the client  side functions
 */
public class Client
{
    //DECLARATIONS

    /* create the socket */
    Socket socket = new Socket("127.0.0.1", 3020);

    //DECLARING CLIENTSIDE CONSTANTS
    /* read input from the keyboard */
    BufferedReader input = new BufferedReader(new InputStreamReader(System.in));

    /* create a stream to send output to the server */
    OutputStream ostream = socket.getOutputStream();
    /* Prints formatted representations of objects to a text-output stream - useful for displaying messages */
    PrintWriter pwrite = new PrintWriter(ostream, true);

    /* receiving from server */
    InputStream istream = socket.getInputStream();
    /* read input from the server */
    BufferedReader receiveRead = new BufferedReader(new InputStreamReader(istream));


    String receiveMessage="", sendMessage="", filename = "";

    /* allows us to execute a class in an asynchronous way */
    ExecutorService executorService = Executors.newFixedThreadPool(2);

    /*
     * Constructor for the Client Class
     */
    public Client() throws IOException {
    }

    /*
     *void send ()
     *Description:          Function to send messages to Server
     */
    void send(){
        /* continue to send messages until server writes "end" or "End" */
        while(!(sendMessage.equals("End") || sendMessage.equals("end"))) {
            try {
                //read message from input
                sendMessage = input.readLine();  // keyboard reading
                //if requestFile
                if (sendMessage.startsWith("requestFile")) {
                    //check is filename is provided, and then save filename to use for receiving
                    String[] command = sendMessage.split(" ");
                    if (command.length < 2) {
                        System.out.println("No filename provided. Try again.");
                        continue;
                    } else {
                        filename = sendMessage.split(" ")[1];
                    }
                    MessageHeader header = new MessageHeader("requestFile",filename);
                    pwrite.println(sendMessage);       // sending to server
                    pwrite.flush();                    // flush the data
                } else {
                    MessageHeader header = new MessageHeader("sendChat",sendMessage);
                    //send request to server
                    pwrite.println(header.messageToString());       // sending to server
                    pwrite.flush();                    // flush the data
                }
                System.out.println("MESSAGE SUCCESSFULLY SENT\n");
                //close connection
                if (sendMessage.equals("End") || sendMessage.equals("end")) {
                    ostream.close();
                    istream.close();
                    input.close();
                    socket.close();
                    executorService.shutdownNow();
                    System.exit(0);
                }
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
    }

    /*
     *void receive ()
     *Description:          Function to process incoming messages from Server
     */
    void receive(){
        //made FTP object for receiving file
        new FTP();
        while(!(sendMessage.equals("End") || sendMessage.equals("end"))) {
            try {
                //read from network
                if ((receiveMessage = receiveRead.readLine()) != null) //receive from server
                {
                    //close connection
                    if (receiveMessage.equals("End") || receiveMessage.equals("end")) {
                        System.out.println("Client closed connection. Press Enter to stop the server.");
                        ostream.close();
                        istream.close();
                        input.close();
                        socket.close();
                        executorService.shutdownNow();
                        System.exit(0);
                    }
                    //if message is 'FILE DATA', then start receiving messages using the FTP function
                    if (receiveMessage.equals("FILE DATA")) {
                        System.out.println("RECEIVING FILE DATA"+"\n"); // displaying at DOS prompt
                        FTP.receive(filename, socket);
                    } else {
                        if(receiveMessage.startsWith("sendChat")){
                            //if not a file request, just receive the message and print it
                            System.out.println("CHAT RECEIVED\n"+receiveMessage.split(" ")[1]+"\n"); // displaying at DOS prompt
                        } else {
                            System.out.println("CHAT RECEIVED\n"+receiveMessage+"\n");
                        }
                    }
                }
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
    }

    /*
     *void execute ()
     *Description:          Allows for the Client side to be run asynchronously
     */
    public void execute(){

        // activate the send function
        executorService.submit(this::send);

        // activate the receive function
        executorService.submit(this::receive);

        // close executorService
        executorService.shutdown();
    }

    /*
     *Main
     *Description:          method that executes when we run Client.Java
     */
    public static void main(String[] args) throws Exception
    {
        System.out.println("Ready! To send a message, type the message and press 'Enter' to send..\nTo send a file use the command \"requestFile filename\"\nUse 'quit' to end\n\n");
        
        new Client().execute();
    }
}
