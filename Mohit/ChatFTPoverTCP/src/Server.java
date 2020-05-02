/*
 * Filename     Server.java
 * Date         5/1/2020
 * Author       Ashima Soni, Mira Jambusaria
 * Email        ashima.soni@utdallas.edu mmj170530@utdallas.edu
 * Course       CE 4390.502 Spring 2020
 * Version      1.0
 * Copyright    2020, All Rights Reserved
 *
 * Description
 *
 * This is the file that creates the Server side connection
 *
 */
import javax.sound.midi.SysexMessage;
import java.io.*;
import java.net.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/*
 * Class Server
 * Description:         Handles all the server side functions
 */
public class Server
{
    private String receiveMessage, sendMessage = "";

    //establish connection by hardcoding port number
    ServerSocket server = new ServerSocket(3020);
    
    /* create the socket */
    Socket socket = server.accept( );

    //DECLARING SERVERSIDE CONSTANTS
    /* read input from the keyboard */
    static BufferedReader input = new BufferedReader(new InputStreamReader(System.in));

    /* output stream */
    OutputStream ostream = socket.getOutputStream();
    /* Prints formatted representations of objects to a text-output stream - useful for displaying messages */
    PrintWriter pwrite = new PrintWriter(ostream, true);

    /* receiving from client */
    InputStream istream = socket.getInputStream();
    /* read input from the client */
    BufferedReader receiveRead = new BufferedReader(new InputStreamReader(istream));

    /* allows us to execute a class in an asynchronous way */
    ExecutorService executorService = Executors.newFixedThreadPool(2);

    /*
     * Constructor for Server Class
     */
    public Server() throws IOException {
    }

    /*
     *void receive ()
     *Description:          Function to process incoming messages from Client
     */
    void receive(){
        /* handle incoming file messages */
        new FTP();
        /* ends connection if Server sends "end" or "End" */
        while(!(sendMessage.equals("End") || sendMessage.equals("end"))) {
            try {
                if ((receiveMessage = receiveRead.readLine()) != null)  //receive from client
                {
                    /* if client requests to end connection */
                    if (receiveMessage.equals("End") || receiveMessage.equals("end")) {
                        System.out.println("Client has closed connection. Press Enter to stop the server.");
                        ostream.close();
                        istream.close();
                        input.close();
                        socket.close();
                        executorService.shutdownNow();
                        System.exit(0);
                    }
                    /* if the client has requested a file */
                    if (receiveMessage.startsWith("requestFile")) {
                        // split the message at the " " into 2 parts
                        System.out.println(receiveMessage); // displaying at DOS prompt
                        String[] command = (receiveMessage.split(" ", 2));

                        /* we will have a valid filename if the array has two elements  */
                        if (command.length < 2) {
                            System.out.println("No filename provided.");
                            pwrite.println("No filename provided. Try again.");
                            pwrite.flush();
                        } else {
                            //create a file variable to hold the file
                            File file = new File(command[1]);
                            /* if we do not have the file  */
                            if (!file.exists()) {
                                /* send error message to Server */
                                System.out.println("The file the client requested does not exists.");
                                /* send error message to Client */
                                pwrite.println("The requested file does not exist. Please try again.");
                                pwrite.flush();
                            }
                            /* file found */
                            else {
                                //send file if it exists
                                System.out.println("File exists. Sending... ");
                                //send client a message so that client can start receiving a file
                                pwrite.println("FILE DATA");
                                pwrite.flush();
                                //use the function in FTP
                                FTP.send(command[1], socket);
                            }
                        }
                    }
                    /* if there is no filename provided or there was an error with reading the file */
                    else {
                        /* print error message to client */
                        if(receiveMessage.startsWith("sendChat")){
                            /*if not a file request, just receive the message and print it*/
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
     *void send ()
     *Description:          Function to send messages to Client
     */
    void send() {
        /* continue to send messages until server writes "end" or "End" */
        while(!(sendMessage.equals("end") || sendMessage.equals("End"))) {
            try {
                // read the message from keyboard
                sendMessage = input.readLine();

                //create the header
                MessageHeader header = new MessageHeader("sendChat",sendMessage);
                //send the message to the client
                pwrite.println(header.messageToString());       // sending to server
                pwrite.flush();                    // flush the data

                System.out.println("MESSAGE SUCCESSFULLY SENT\n");

                /* if server wants to end close the stream */
                if (sendMessage.equals("Quit") || sendMessage.equals("quit")) {
                    /* safely exit out of the socket */
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
     *void execute ()
     *Description:          Allows for the Server side to be run asynchronously
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
     *Description:          method that executes when we run Server.Java
     */
    public static void main(String[] args) throws Exception
    {

        //DECLARATIONS
        System.out.println("Preparing...");
        System.out.println("Server running...");
        System.out.println("Client connected.\n\n");

        System.out.println("Ready. Type a message and press Enter to send. \n\n");

        //create the connection and run it asynchronously
        new Server().execute();
    }
}
