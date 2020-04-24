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
    BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
    // sending to client (pwrite object)
    OutputStream ostream = socket.getOutputStream();
    PrintWriter pwrite = new PrintWriter(ostream, true);

    // receiving from server ( receiveRead  object)
    InputStream istream = socket.getInputStream();
    BufferedReader receiveRead = new BufferedReader(new InputStreamReader(istream));

    public Server() throws IOException {
    }

    void receive(){
        new FTP();
        while(true) {
            try {
                if ((receiveMessage = receiveRead.readLine()) != null) //receive from server
                {
                    if (receiveMessage.equals("Quit") || receiveMessage.equals("quit")) {
                        ostream.close();
                        istream.close();
                        input.close();
                        socket.close();
                        break;
                    }
                    System.out.println(receiveMessage); // displaying at DOS prompt
                    if (receiveMessage.startsWith("sendFile")) {
                        String[] command = (receiveMessage.split(" ", 2));
                        if (command.length < 2) {
                            System.out.println("No filename provided.");
                            pwrite.println("No filename provided. Try again.");
                            pwrite.flush();
                        } else {
                            File file = new File(command[1]);
                            if (!file.exists()) {
                                System.out.println("The file the client requested does not exists.");
                                pwrite.println("The requested file does not exist. Please try again.");
                                pwrite.flush();
                            } else {
                                System.out.println("File exists. Sending... ");
                                pwrite.println("receiveFile");
                                pwrite.flush();
                                FTP.send(command[1], socket);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
    }

    void send() {
        while(true){
            try {
                sendMessage = input.readLine();  // keyboard reading
                pwrite.println(sendMessage);       // sending to server
                pwrite.flush();                    // flush the data
                if (sendMessage.equals("Quit") || sendMessage.equals("quit")) {
                    ostream.close();
                    istream.close();
                    input.close();
                    socket.close();
                    break;
                }
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
    }

    public void execute(){
        ExecutorService executorService = Executors.newFixedThreadPool(2);

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
        System.out.println("Client connected.\n\n");

        System.out.println("Ready. Type a message and press Enter to send. \n\n");
        //

        new Server().execute();
    }
}