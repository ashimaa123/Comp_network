import java.io.Serializable;
import java.util.Comparator;

public class Header implements Serializable
{
    private static final long serialVersionUID = 1L;

    byte[] payload = null;
    long number = 0;
    String checksum = null;

    /*
     * header object with a byte array, a sequence number, checksum string
     */
    public Header(byte[] data, long sequence, String chksm)
    {
        payload = data;
        number = sequence;
        checksum = chksm;
    }

    /*
     * Compared one header object with another header object by taking the difference between the two
     */
    public static Comparator<Header> compareheader = new Comparator<Header>()
    {
        public int compare(Header h1, Header h2)
        {
            int number1 = (int) h1.number;
            int num2 = (int) h2.number;

            return (number1 - number2);
        }
    };
}
