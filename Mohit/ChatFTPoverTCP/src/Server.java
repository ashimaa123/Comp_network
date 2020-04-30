import javax.sound.midi.SysexMessage;
import java.io.*;
import java.net.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server
{
    private String receiveMessage, sendMessage = "";
    ServerSocket server = new ServerSocket(3000);
    Socket socket = server.accept( );
    // reading from keyboard (keyRead object)
    static BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
    // sending to client (pwrite object)
    OutputStream ostream = socket.getOutputStream();
    PrintWriter pwrite = new PrintWriter(ostream, true);

    // receiving from server ( receiveRead  object)
    InputStream istream = socket.getInputStream();
    BufferedReader receiveRead = new BufferedReader(new InputStreamReader(istream));
    ExecutorService executorService = Executors.newFixedThreadPool(2);

    public Server() throws IOException {
    }

    //This function receives messages
    void receive(){
        //Make new FTP object for sending files
        new FTP();
        while(!(sendMessage.equals("Quit") || sendMessage.equals("quit"))) {
            try {
                //read from socket
                if ((receiveMessage = receiveRead.readLine()) != null) //receive from server
                {
                    //close connection and return
                    if (receiveMessage.equals("Quit") || receiveMessage.equals("quit")) {
                        System.out.println("Client closed connection. Press Enter to stop the server.");
                        ostream.close();
                        istream.close();
                        input.close();
                        socket.close();
                        executorService.shutdownNow();
                        System.exit(0);
                    }
                    //if client is requesting a file
                    if (receiveMessage.startsWith("requestFile")) {
                        System.out.println(receiveMessage); // displaying at DOS prompt
                        String[] command = (receiveMessage.split(" ", 2));
                        //command[0] = receiveFile, command[1] = filename
                        if (command.length < 2) {
                            System.out.println("No filename provided.");
                            pwrite.println("No filename provided. Try again.");
                            pwrite.flush();
                        } else {
                            //open the file
                            File file = new File(command[1]);
                            //see if it exists
                            if (!file.exists()) {
                                System.out.println("The file the client requested does not exists.");
                                pwrite.println("The requested file does not exist. Please try again.");
                                pwrite.flush();
                            } else {
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
                    else {
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

    //this function sends messages to client
    void send() {
            while(!(sendMessage.equals("Quit") || sendMessage.equals("quit"))) {
            try {
                //take input for message
                sendMessage = input.readLine();  // keyboard reading
                MessageHeader header = new MessageHeader("sendChat",sendMessage);
                pwrite.println(header.messageToString());       // sending to server
                pwrite.flush();                    // flush the data
                System.out.println("MESSAGE SUCCESSFULLY SENT\n");
                //close connection
                if (sendMessage.equals("Quit") || sendMessage.equals("quit")) {
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

    public void execute(){

        // method reference introduced in Java 8
        //run both send and receive in separate threads
        executorService.submit(this::send);
        executorService.submit(this::receive);

        // close executorService
        executorService.shutdown();
    }

    public static void main(String[] args) throws Exception
    {
//        System.out.print("Enter port number: ");
//        String port = input.readLine();
//        System.out.println("");
//        server = new ServerSocket(Integer.parseInt(port));
//        socket = server.accept();

        //DECLARATIONS
        System.out.println("Preparing...");
        System.out.println("Server running...");
        System.out.println("Client connected.\n\n");

        System.out.println("Ready. Type a message and press Enter to send. \n\n");
        //

        new Server().execute();
    }
}