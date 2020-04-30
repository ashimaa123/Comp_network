import java.io.*;
import java.net.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Client
{
    //DECLARATIONS

    Socket socket = new Socket("127.0.0.1", 3000);
    // reading from keyboard (keyRead object)
    BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
    // sending to client (pwrite object)
    OutputStream ostream = socket.getOutputStream();
    PrintWriter pwrite = new PrintWriter(ostream, true);

    // receiving from server ( receiveRead  object)
    InputStream istream = socket.getInputStream();
    BufferedReader receiveRead = new BufferedReader(new InputStreamReader(istream));
    String receiveMessage="", sendMessage="", filename = "";
    ExecutorService executorService = Executors.newFixedThreadPool(2);

    public Client() throws IOException {
    }
    //this function sends messages to the server
    void send(){
        while(!(sendMessage.equals("Quit") || sendMessage.equals("quit"))) {
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

    void receive(){
        //made FTP object for receiving file
        new FTP();
        while(!(sendMessage.equals("Quit") || sendMessage.equals("quit"))) {
            try {
                //read from network
                if ((receiveMessage = receiveRead.readLine()) != null) //receive from server
                {
                    //close connection
                    if (receiveMessage.equals("Quit") || receiveMessage.equals("quit")) {
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

    public void execute(){
        // method reference introduced in Java 8
        //execute both send and receive at once in two threads
        executorService.submit(this::send);
        executorService.submit(this::receive);
        // close executorService
        executorService.shutdown();
    }

    public static void main(String[] args) throws Exception
    {
//        System.out.print("Enter IP Address: ");
//        String ipAddress = input.readLine();
//        System.out.println("");
//        System.out.print("Enter Port Number: ");
//        String port = input.readLine();
//        System.out.println((""));
        System.out.println("Ready. Type a message and press Enter to send.\nTo send a file use the command \"requestFile filename\"\nUse 'quit' to end\n\n");
        new Client().execute();
    }
}