/*
 * Filename     Header.java
 * Date         5/1/2020
 * Author       Ashima Soni, Mira Jambusaria
 * Email        ashima.soni@utdallas.edu mmj170530@utdallas.edu
 * Course       CE 4390.502 Spring 2020
 * Version      1.0
 * Copyright    2020, All Rights Reserved
 *
 * Description
 *
 * This is the file that holds the headers that we will use to send our packets
 */

import java.io.Serializable;
import java.util.Comparator;

public class Header implements Serializable
{
    private static final long serialVersionUID = 1L;

    byte[] payload = null;
    long num = 0;
    String checksum = null;

    /*
     * header object with a byte array, a sequence number, and a checksum string
     */
    public Header(byte[] data, long sequence, String chksm)
    {
        payload = data;
        num = sequence;
        checksum = chksm;
    }

    /*
     * Compares one header object with another header object by taking the difference between the two sequence numbers
     */
    public static Comparator<Header> compareheader = new Comparator<Header>()
    {
        public int compare(Header h1, Header h2)
        {
            int num1 = (int) h1.num;
            int num2 = (int) h2.num;

            return (num1 - num2);
        }
    };
}
