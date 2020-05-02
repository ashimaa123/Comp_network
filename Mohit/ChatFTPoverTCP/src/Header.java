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
 *
 */

import java.io.Serializable;
import java.util.Comparator;

public class Header implements Serializable
{
    private static final long serialVersionUID = 1L;

    byte[] payload = null;
    long number = 0;
    String checksum = null;

    /*
     * Creates a header object that contains a byte array, a sequence number & 
     * checksum string
     */
    public Header(byte[] data, long sequence, String chksm)
    {
        payload = data;
        number = sequence;
        checksum = chksm;
    }

    /*
     * Compares header object to other header object by checking the sequence number of each object and
     * returns the difference between the two numbers. 
     * If difference is negative, second object has a larger sequence number 
     * If difference is positive, first object has a larger sequence number 
     */
    public static Comparator<Header> compareheader = new Comparator<Header>()
    {
        public int compare(Header h1, Header h2)
        {
            int number1 = (int) h1.number;
            int number2 = (int) h2.number;

            return (number1 - number2);
        }
    };
}
