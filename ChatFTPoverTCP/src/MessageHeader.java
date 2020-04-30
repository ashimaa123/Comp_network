public class MessageHeader {
    String messageType = "";
    String data = "";

    MessageHeader(String x, String y){
        if(x.equals("requestFile")){
            this.messageType = x;
            this.data = y;
        } else if (x.equals("sendChat")){
            this.messageType = x;
            this.data = y;
        }  else {
            System.out.println("Invalid Header Message Type.");
        }
    }

    String messageToString(){
        return(messageType + " " + data);
    }
}
