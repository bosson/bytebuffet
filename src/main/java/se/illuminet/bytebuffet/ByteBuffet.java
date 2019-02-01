package se.illuminet.bytebuffet;

import java.io.IOException;
import java.util.UUID;

/**
 * Fast byte array parsing operation, without object creations.
 * Tries to be fast in parsing regular data from logs, UTC dates and numbers.
 */
public class ByteBuffet {

    final static long MASK32 = 0xFFFFFFFFL;
    final static long BIT32 = 0x100000000L;

    byte buffer[]; 
    int  index;

    public ByteBuffet(byte buffer[],int index) {
        setTarget(buffer,index);
    }

    public void setTarget(byte b[],int index) {
        this.buffer = b;
        this.index = index;
    }

    public int indexOf(byte c,int end) {
        return ByteBuffet.indexOf(c,buffer,index,end);
    }

    public int indexOf(byte c) {
        return indexOf(c,buffer.length);
    }

    public byte nextByte() { return this.buffer[this.index++]; }
    public int next() { return (int)nextByte() & 0xff; }
    public int nextDigit() { return (int)'0' - (int)next(); }
    public int nextOffset(final byte b) { return indexOf(b,buffer,index,buffer.length)-index; }


    public static int indexOf(final byte c,final byte b[],final int start, final int end) {
        int i = 0;
        if (end > b.length) throw new IndexOutOfBoundsException("end out of range: "+end+ ">" + b.length);
        while(start + i < end) {
            if (b[i+start] == c) return i+start;
            i++;
        }
        return -1;
    }

    private static int digitAt(final byte b[],final int i) throws IOException {
        int digit = b[i]-'0';
        if (digit > 9 || digit < 0) throw new IOException("not a digit: "+(char)b[i]);
        return digit;
    }

    public static long parseULong(final byte b[], final int start,final int len)
        throws IOException {
        long n = 0L;
        long pow = 1L;
        for (int i = len - 1; i >= 0; i--) {
            n+=pow*digitAt(b,start+i);
            pow = pow * 10L;
        }
        return n;
    }

    public static long[] pow10 = pow10x40();
    public static int[] pow10bits;

    private static long[] pow10x40() {

        long[] p = new long[40*2];
        int[] p10b = new int[40];
        long p0 = 1L;
        long p1 = 0L;
        long p2 = 0L;
        long p3 = 0L;

        for (int i = 0; i < 40; i++) {
            // carry bits up

            p[i*2] = (p3 << 32) + p2;
            p[i*2+1] = (p1 << 32) + p0;

            p10b[i] = p[i*2] != 0 ? firstBit(p[i*2]) : firstBit(p[i*2+1]);

            // pow = pow * 10
            p0 = p0 * 10L;
            p1 = p1 * 10L;
            p2 = p2 * 10L;
            p3 = p3 * 10L;

            p1 += (p0 >>> 32);
            p2 += (p1 >>> 32);
            p3 += (p2 >>> 32);

            p0 = p0 & MASK32;
            p1 = p1 & MASK32;
            p2 = p2 & MASK32;

        }
        pow10bits = p10b;
        return p;
    }

    static int firstBit(long l) {
        if (l < 0L) return 63;
        if (l == 0L) return -1;
        int i = 63;
        while (l > 0 && i > 0) { l = l << 1; i--; }
        return i;
    }

    public static int longLongToDecimal(
            long high, long low,
            byte b[], int start) throws IOException {

        long i3 = high >>> 32;
        long i2 = high & MASK32;
        long i1 = low >>> 32;
        long i0 = low & MASK32;

        final int highBit;
        if (i3 != 0) highBit = 96 + firstBit(i3);
        else if (i2 != 0) highBit = 64 + firstBit(i2);
        else if (i1 != 0) highBit = 32 + firstBit(i1);
        else if (i0 != 0) highBit = firstBit(i0);
        else { // zero
            b[start]=(byte)'0';
            return 1;
        }

        // match with first lower decimal power
        int p = 38;
        while(pow10bits[p] > highBit) p--;
        int i = 0;

        while(p >= 0) { // for each 10 power

           int digit = 0;
           long carry = 0;

           // load the 10^p value
           final long p3 = pow10[p*2] >>> 32;
           final long p2 = pow10[p*2] & MASK32;
           final long p1 = pow10[p*2+1] >>> 32;
           final long p0 = pow10[p*2+1] & MASK32;

           while(digit < 9) { // up the digit by subtracting.

               // if p > i when go to next lower p.
               if (p3 > i3) break;
               if (p3 == i3) {
                   if (p2 > i2) break;
                   if (p2 == i2) {
                       if (p1 > i1) break;
                       if (p1 == i1 && p0 > i0) break;
                   }
               }

               // i = i - p
               i0 -= p0; if (i0 < 0) { carry = 1; i0 = 0x100000000L + i0; }
               i1 -= (p1 + carry); if (i1 < 0) { carry = 1; i1 = 0x100000000L + i1; } else carry = 0;
               i2 -= (p2 + carry); if (i2 < 0) { carry = 1; i2 = 0x100000000L + i2; } else carry = 0;
               i3 -= (p3 + carry); if (i3 < 0) { carry = 1; i3 = 0x100000000L + i3; } else carry = 0;

               digit++;
           }

           if (i > 0 || digit > 0) {
               if (start + i > b.length) throw new IOException("buffer overflow at: "+start+i);
               b[start + i] = (byte) ('0' + digit);
               i++;
           }

           p--;
        }

        return i;
    }


    public static UUID parseDecimalUUID(
            final byte b[], final int start,final int len)
            throws IOException {

        // accumulator
        long i0 = 0L;
        long i1 = 0L;
        long i2 = 0L;
        long i3 = 0L;
        // power (*10)
        long p0 = 1L;
        long p1 = 0L;
        long p2 = 0L;
        long p3 = 0L;
        // mask down.


        for (int i = len - 1; i >= 0; i--) {

            long d = digitAt(b,start+i);

            i0 += p0 * d;
            i1 += p1 * d;
            i2 += p2 * d;
            i3 += p3 * d;

            // carry bits up
            i1 += (i0 >>> 32);
            i2 += (i1 >>> 32);
            i3 += (i2 >>> 32);
            i0 = i0 & MASK32;
            i1 = i1 & MASK32;
            i2 = i2 & MASK32;

            // pow = pow * 10
            p0 = p0 * 10L;
            p1 = p1 * 10L;
            p2 = p2 * 10L;
            p3 = p3 * 10L;
            // carry bits up
            p1 += (p0 >>> 32);
            p2 += (p1 >>> 32);
            p3 += (p2 >>> 32);
            p0 = p0 & MASK32;
            p1 = p1 & MASK32;
            p2 = p2 & MASK32;

        }

        long low = i0 | (i1 << 32);
        long high = i2 | (i3 << 32);

        return new UUID(high,low);

    }

    public static void uLongToBytes(long l, byte b[], int start) {
        for (int i = 7; i >= 0; i--) {
            b[start+i] = (byte)(l & 0xff);
            l = l >>> 8;
        }
    }

    public static void longLongToBytes(long hi,long low,byte b[], int start) {
        uLongToBytes(hi,b,start);
        uLongToBytes(low,b,start+8);
    }

    /**
     * Reads epoc date in format "yyyymmmdd" and return milliseconds since epoc at days start.
     * @param b
     * @param start
     * @return
     * @throws IOException
     */
    public static long parseEpocDate(final byte b[], final int start) throws IOException {

        if (b.length - start < 8) throw new IOException("missing digits.");

        final int yyyy = digitAt(b,start)*1000 + digitAt(b,start+1)*100 + digitAt(b,start+2)*10 + digitAt(b,start+3);
        final int mm = digitAt(b,start+4)*10 + digitAt(b,start+5);
        final int dd = digitAt(b,start+6)*10 + digitAt(b,start+7);

        return EpochDate.epochTime(yyyy, mm, dd);
    }

    /**
     * parse yyyyMMddHHmmss or yyyyMMdd or hhmmss
     * @param b
     * @param start
     * @param end
     * @return
     * @throws IOException
     */
    public static long parseEpocDateTime(final byte b[], final int start, final int end)
        throws IOException{

        if (b.length < end-start) throw new IOException("out of bounds, "+b.length+"+"+start+"<"+end);

        final int len = end-start;

        if (len == 8) { // YYYYMMDD
            return parseEpocDate(b, start);
        } else if (len == 6) { // HHMMSS
            return EpochDate.parseEpocTimeOfDay(b, start);
        } else if (len == 14) { // YYYYMMDDHHMMSS
            long d = parseEpocDate(b,start);
            long s = EpochDate.parseEpocTimeOfDay(b,start+8);
            return d + s;
        }

        throw new IOException("time not understood here: "+new String(b,start,len));

    }

    public static boolean byteEquals(byte b[],int start, byte val[]) {
        if (b.length-start < val.length) return false;
        for (int i = 0; i < val.length; i++)
            if (val[i] != b[i+start]) return false;

        return true;

    }


}
