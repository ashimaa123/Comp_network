import java.io.*;
import java.net.*;
public class Client
{
    public static void main(String[] args) throws Exception
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
        new FTP();

        System.out.println("Ready. Type a message and press Enter to send. Press Enter to receive messages if you don't want to send. \n\n");
        //

        String receiveMessage, sendMessage, filename = "";

        while(true)
        {
            System.out.println("Waiting for input:");
            sendMessage = input.readLine();  // keyboard reading
            if (sendMessage.startsWith("sendFile")){
                String [] command = sendMessage.split(" ");
                if(command.length<2){
                    System.out.println("No filename provided. Try again.");
                    continue;
                } else {
                    filename = sendMessage.split(" ")[1];
                }
            }
            pwrite.println(sendMessage);       // sending to server
            pwrite.flush();                    // flush the data
            if(sendMessage.equals("Quit")|| sendMessage.equals("quit")){
                ostream.close();
                istream.close();
                input.close();
                socket.close();
                break;
            }

            if((receiveMessage = receiveRead.readLine()) != null) //receive from server
            {
                if(receiveMessage.equals("Quit") || receiveMessage.equals("quit")){
                    System.out.println(receiveMessage);
                    ostream.close();
                    istream.close();
                    input.close();
                    socket.close();
                    break;
                }
                if(receiveMessage.equals("receiveFile")){
                    System.out.println("Receiving File...");
                    FTP.receive(filename, socket);
                }
                else
                    System.out.println(receiveMessage); // displaying at DOS prompt
            }
        }
    }
}