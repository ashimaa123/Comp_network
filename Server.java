import javax.sound.midi.SysexMessage;
import java.io.*;
import java.net.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server
{
    private String receiveMessage = "", sendMessage = "";
    ServerSocket server = new ServerSocket(3100);
    Socket socket = server.accept( );
    // reading from keyboard (keyRead object)
    BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
    // sending to client (pwrite object)
    OutputStream ostream = socket.getOutputStream();
    PrintWriter pwrite = new PrintWriter(ostream, true);

    // receiving from server ( receiveRead  object)
    InputStream istream = socket.getInputStream();
    BufferedReader receiveRead = new BufferedReader(new InputStreamReader(istream));
    ExecutorService executorService = Executors.newFixedThreadPool(2);

    public Server() throws IOException {
    }

    void receive(){
        new FTP();
        while(!(sendMessage.equals("End") || sendMessage.equals("end"))) {
            try {
                if ((receiveMessage = receiveRead.readLine()) != null) //receive from server
                {
                    if (receiveMessage.equals("End") || receiveMessage.equals("end")) {
                        System.out.println("Client closed connection. Press Enter to stop the server.");
                        ostream.close();
                        istream.close();
                        input.close();
                        socket.close();
                        executorService.shutdownNow();
                        System.exit(0);
                    }
                    if (receiveMessage.startsWith("requestFile")) {
                        String[] command = (receiveMessage.split(" ", 2));
                        if (command.length < 2) {
                            System.out.println("NO FILENAME provided.");
                            pwrite.println("NO FILENAME provided. Try again.");
                            pwrite.flush();
                        } else {
                            File file = new File(command[1]);
                            if (!file.exists()) {
                                System.out.println("FILE DOES NOT EXIST.");
                                pwrite.println("FILE DOES NOT EXIST . Please try again.");
                                pwrite.flush();
                            } else {
                                System.out.println("FILE FOUND, get ready. Sending... ");
                                pwrite.println("receiveFile");
                                pwrite.flush();
                                FTP.send(command[1], socket);
                            }
                        }
                    }

                    else{
                        System.out.println("THIS IS CHAT DATA");
                        System.out.println(receiveMessage); // displaying at DOS prompt
                    }
                }
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
    }

    void send() {
        while(!(sendMessage.equals("End") || sendMessage.equals("end"))){
            try {
                sendMessage = input.readLine() ;  // keyboard reading
                pwrite.println(sendMessage);       // sending to server
                pwrite.flush();                    // flush the data
                System.out.println("MESSAGE SUCCESSFULLY SENT");
                if (sendMessage.equals("End") || sendMessage.equals("end")) {
                    ostream.close();
                    istream.close();
                    input.close();
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
        executorService.submit(this::send);
        executorService.submit(this::receive);

        // close executorService
        executorService.shutdown();
    }

    public static void main(String[] args) throws Exception
    {

        //DECLARATIONS
        System.out.println("Preparing...");
        System.out.println("Server running...");
        System.out.println("CONNECTION ESTABLISHED.\n\n");

        System.out.println("Ready. To send a message, type the message and press 'Enter' to send. To request a file, type 'requestFile' and the file name. \n\n");        //

        new Server().execute();
    }
}