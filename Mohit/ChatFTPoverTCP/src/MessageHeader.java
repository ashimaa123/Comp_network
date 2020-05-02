public class MessageHeader {
    String messageType = "";
    String data = "";

    /*
    this creates a header for a message 
    */
    MessageHeader(String x, String y){
        //if message is to request a file
        if(x.equals("requestFile"))
        {
            this.messageType = x;
            this.data = y; 
        } 
        //if message is to send a chat
        else if (x.equals("sendChat"))
        {
            this.messageType = x;
            this.data = y;
        } 
        else 
        {
            System.out.println("This header message type is invalid.");
        }
    }

    //return the message type and the data 
    String messageToString(){
        return(messageType + " " + data);
    }
}
