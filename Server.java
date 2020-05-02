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

import java.io.*;
import java.net.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/*
 * Class Server
 * Description:         Handles all the server side functions
 */
public class Server
{
    /* hardcoded port number */
    int PORT = 3100;

    /* establish the connection */
    ServerSocket server;
    /* create the socket */
    Socket socket;

    //DECLARING SERVERSIDE CONSTANTS
    /* read input from the keyboard */
    BufferedReader input = new BufferedReader(new InputStreamReader(System.in));

    /* receiving from client */
    InputStream inputStream;
    /* read input from the client */
    BufferedReader reader;

    /* create a stream to send output to the user */
    OutputStream outputStream;

    /* Prints formatted representations of objects to a text-output stream - useful for displaying messages */
    PrintWriter printWriter;

    /* allows us to execute a class in an asynchronous way */
    ExecutorService executorService = Executors.newFixedThreadPool(2);

    private String receiveMessage = "", sendMessage = "";

    /*
     * Constructor for Server Class
     */
    public Server() throws IOException {
        System.out.println("Preparing...");
        System.out.println("Server running...");

        server = new ServerSocket(PORT);
        socket = server.accept( );

        System.out.println("CONNECTION ESTABLISHED.\n\n");

        inputStream = socket.getInputStream();
        reader = new BufferedReader(new InputStreamReader(inputStream));

        outputStream = socket.getOutputStream();
        printWriter = new PrintWriter(outputStream, true);

        System.out.println("Ready. To send a message, type the message and press 'Enter' to send. \n\n");
    }

    /*
     *Main
     *Description:          method that executes when we run Server.Java
     */
    public static void main(String[] args) throws Exception
    {
        //create the connection
        Server s = new Server();
        //run it asynchronously
        s.execute();
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

    void send() {
        while(!(sendMessage.equals("End") || sendMessage.equals("end"))){
            try {
                sendMessage = input.readLine() ;  // keyboard reading
                printWriter.println(sendMessage);       // sending to client
                printWriter.flush();                    // flush the data
                System.out.println("MESSAGE SUCCESSFULLY SENT");
                if (sendMessage.equals("End") || sendMessage.equals("end")) {
                    outputStream.close();
                    inputStream.close();
                    input.close();
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
     *Description:          Function to process incoming messages from Client
     */
    void receive(){
        /* handle incoming file messages */
        new FTP();

        /* we continue to run this function until Server sends "end" or "End" */
        while(!(sendMessage.equals("End") || sendMessage.equals("end"))) {
            try {
                if ((receiveMessage = reader.readLine()) != null) //receive from client
                {
                    /* if the client has requested to end the connection */
                    if (receiveMessage.equals("End") || receiveMessage.equals("end")) {
                        System.out.println("Client closed connection. Press Enter to stop the server.");
                        input.close();
                        inputStream.close();
                        outputStream.close();
                        socket.close();
                        executorService.shutdownNow();
                        System.exit(0);
                    }
                    /* if the client has requested a file */
                    if (receiveMessage.startsWith("requestFile")) {
                        // split the message at the " " into 2 parts
                        String[] requestFilename = (receiveMessage.split(" ", 2));
                        /* we will have a valid filename if the array has two elements  */
                        if (requestFilename.length == 2) {
                            //create a file variable to hold the file
                            File file = new File(requestFilename[1]);
                            /* if we do not have the file  */
                            if (!file.exists()) {
                                /* send error message to Server */
                                System.out.println("FILE DOES NOT EXIST.");
                                /* send error message to Client */
                                printWriter.println("FILE DOES NOT EXIST . Please try again.");
                                printWriter.flush();
                            }
                            /* file found */
                            else {
                                //

                                //COME BACK

                                //
                                System.out.println("FILE FOUND, get ready. Sending... ");
                                FTP.send(requestFilename[1], socket);
                                printWriter.println("receiveFile");
                                printWriter.flush();
                            }
                        }
                        /* if there is no filename provided or there was an error with reading the file */
                        else {
                            /* print error message to screen */
                            System.out.println("NO FILENAME provided.");
                            /* print error message to client */
                            printWriter.println("NO FILENAME provided. Try again.");
                            printWriter.flush();
                        }

                    }
                    /* The client is attempting to send regular data */
                    else {
                        // display message to console
                        System.out.println(receiveMessage);
                    }
                }
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
    }




}