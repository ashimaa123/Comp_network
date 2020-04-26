import java.io.*;
import java.net.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Client
{
    //DECLARATIONS

    Socket socket = new Socket("127.0.0.1", 3100);
    // reading from keyboard (keyRead object)
    BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
    // sending to client (pwrite object)
    OutputStream ostream = socket.getOutputStream();
    PrintWriter pwrite = new PrintWriter(ostream, true);

    // receiving from server ( receiveRead  object)
    InputStream istream = socket.getInputStream();
    BufferedReader receiveRead = new BufferedReader(new InputStreamReader(istream));
    ExecutorService executorService = Executors.newFixedThreadPool(2);
    String receiveMessage = "", sendMessage = "", filename = "";

    public Client() throws IOException {
    }

    void send(){
        while(!(sendMessage.equals("End") || sendMessage.equals("end"))) {
            try {
                sendMessage = input.readLine();  // keyboard reading
                if (sendMessage.startsWith("requestFile")) {
                    String[] command = sendMessage.split(" ");
                    if (command.length < 2) {
                        System.out.println("NO FILENAME provided. Try again.");
                        continue;
                    } else {
                        filename = sendMessage.split(" ")[1];
                    }
                }

                pwrite.println(sendMessage);       // sending to server
                pwrite.flush();                    // flush the data
                System.out.println("MESSAGE SUCCESSFULLY SENT");
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

    void receive(){
        new FTP();
        while(!(sendMessage.equals("End") || sendMessage.equals("end"))) {
            try {
                if ((receiveMessage = receiveRead.readLine()) != null) //receive from server
                {
                    if (receiveMessage.equals("End") || receiveMessage.equals("end")) {
                        System.out.println(receiveMessage);
                        ostream.close();
                        istream.close();
                        input.close();
                        socket.close();
                        System.out.println("Server closed connection. Press enter to stop the server.");
                        executorService.shutdownNow();
                        System.exit(0);
                    }
                    if (receiveMessage.equals("receiveFile")) {
                        System.out.println("Receiving File...");
                        FTP.receive(filename, socket);
                    }
                    else {
                        System.out.println("THIS IS CHAT DATA");
                        System.out.println(receiveMessage); // displaying at DOS prompt

                    }
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
        System.out.println("CONNECT ACKNOWLEDGMENT");

        System.out.println("Ready. To send a message, type the message and press 'Enter' to send. To request a file, type 'requestFile' and the file name. \n\n");

        new Client().execute();
    }
}