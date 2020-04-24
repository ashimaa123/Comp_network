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

    void send(){
        while(!(sendMessage.equals("Quit") || sendMessage.equals("quit"))) {
            try {
                sendMessage = input.readLine();  // keyboard reading
                if (sendMessage.startsWith("sendFile")) {
                    String[] command = sendMessage.split(" ");
                    if (command.length < 2) {
                        System.out.println("No filename provided. Try again.");
                        continue;
                    } else {
                        filename = sendMessage.split(" ")[1];
                    }
                }
                pwrite.println(sendMessage);       // sending to server
                pwrite.flush();                    // flush the data
                if (sendMessage.equals("Quit") || sendMessage.equals("quit")) {
                    ostream.close();
                    istream.close();
                    input.close();
                    socket.close();
                    executorService.shutdownNow();
                    return;
                }
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
    }

    void receive(){
        new FTP();
        while(!(sendMessage.equals("Quit") || sendMessage.equals("quit"))) {
            try {
                if ((receiveMessage = receiveRead.readLine()) != null) //receive from server
                {
                    if (receiveMessage.equals("Quit") || receiveMessage.equals("quit")) {
                        System.out.println(receiveMessage);
                        ostream.close();
                        istream.close();
                        input.close();
                        socket.close();
                        executorService.shutdownNow();
                        return;
                    }
                    if (receiveMessage.equals("receiveFile")) {
                        System.out.println("Receiving File...");
                        FTP.receive(filename, socket);
                    } else
                        System.out.println(receiveMessage); // displaying at DOS prompt
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
        System.out.println("Ready. Type a message and press Enter to send.\nTo send a file use the command \"sendFile filename\"\n\n");
        //
        new Client().execute();
    }
}