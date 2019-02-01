package se.illuminet.bytebuffet;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.math.BigInteger;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;


public class ByteBuffetTest {

    @Test
    public void testTrackingEventTimeParser() throws Exception {

        long now = System.currentTimeMillis();
        DateFormat dayFormat = new SimpleDateFormat("yyyyMMdd");
        dayFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

        assertDayEquals("19760205"); // just before epoch
        assertDayEquals("19700101"); // start (=0)
        assertDayEquals("19720101"); // just before leap
        assertDayEquals("19720301"); // just after leap
        assertDayEquals("19730301"); // year after leap
        assertDayEquals("19740114"); // my birthday
        assertDayEquals("19941230");
        assertDayEquals("19991231");
        assertDayEquals("20080121");
        assertDayEquals("20120501");
        assertDayEquals("20300101");
        assertDayEquals("21340101"); // after skipping leap year 2100

        // 100 year leap-day with exception.
        assertDayEquals("21000228");
        assertDayEquals("21000301");

        assertDayEquals(dayFormat.format(new Date(now)));


        final DateFormat dayHourFormat = new SimpleDateFormat("HHmmss");
        dayHourFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        final String time = dayFormat.format(new Date(now));

        final DateFormat dateTimeFormat = new SimpleDateFormat("yyyyMMddHHmmss");
        dateTimeFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        final String dateTime = dateTimeFormat.format(new Date(now));

        println(dateTime);
        long parsedTime = ByteBuffet.parseEpocDateTime(dateTime.getBytes(), 0, 14);
        long parsedT = dateTimeFormat.parse(dateTime).getTime();
        println("diff:"+(parsedTime-parsedT));

        Assert.assertEquals(parsedT,parsedTime);

    }

    @Test
    public void testAllDatesParsed() throws IOException {

        DateFormat dayFormat = new SimpleDateFormat("yyyyMMdd");
        dayFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

        GregorianCalendar c = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        c.setTimeInMillis(0L);

        while(c.get(Calendar.YEAR) < 2200) {
            long t0 = c.getTimeInMillis();
            String day = dayFormat.format(c.getTime());
            long t1 = ByteBuffet.parseEpocDate(day.getBytes(),0);
            // println(day);
            Assert.assertEquals(t0,t1);
            c.add(Calendar.DATE, 1);

        }
    }

    @Test
    public void testLongParser() throws IOException {

        Assert.assertEquals(10L,ByteBuffet.parseULong("10".getBytes(),0,2));
        Assert.assertEquals(1234567890L,ByteBuffet.parseULong("1234567890".getBytes(),0,10));
        Assert.assertEquals(123456789012345678L,ByteBuffet.parseULong("123456789012345678".getBytes(),0,18));

    }


    @Test
    public void testUTCDate() throws IOException {

        Assert.assertEquals("19740114", ""+ EpochDate.epochToDate(EpochDate.epochTime(1974, 1, 14)));
        Assert.assertEquals("19741214", "" + EpochDate.epochToDate(EpochDate.epochTime(1974, 12, 14)));
        Assert.assertEquals("20200814", "" + EpochDate.epochToDate(EpochDate.epochTime(2020, 8, 14)));
        Assert.assertEquals("20101129", "" + EpochDate.epochToDate(EpochDate.epochTime(2010, 11, 29)));

        // leap test
        Assert.assertEquals("20000301", ""+ EpochDate.epochToDate(EpochDate.epochTime(2000, 3, 1)));
        Assert.assertEquals("20000229", ""+ EpochDate.epochToDate(EpochDate.epochTime(2000, 2, 29)));

    }

    @Test
    public void testLongLongParse() throws Exception
    {
        testLongLongParse("0");
        testLongLongParse("1");
        testLongLongParse("12345678901234567890"); // 63bits
        testLongLongParse("123456789012345678900"); // 66 bits
        testLongLongParse("1234567890123456789001234012");
    }


    public void testLongLongParse(String n) throws Exception {

        BigInteger bi = new BigInteger(n,10);
        println("BigInteger:  "+bi+" / 0x" + bi.toString(16));

        UUID uuid = ByteBuffet.parseDecimalUUID(n.getBytes(),0,n.length());
        println("UUID: "+Long.toHexString(uuid.getMostSignificantBits()) + "."+ Long.toHexString(uuid.getLeastSignificantBits()));

        byte llbuffer[] = new byte[16];

        ByteBuffet.longLongToBytes(uuid.getMostSignificantBits(),uuid.getLeastSignificantBits(),llbuffer,0);
        StringBuffer sb = new StringBuffer();
        for (byte c : llbuffer) sb.append(Integer.toHexString((int)c & 0xff));
        println("Long-Long-Bytes: "+sb.toString());

        BigInteger bi2 = new BigInteger(llbuffer);
        Assert.assertEquals(bi,bi2);

        // to decimal string from long, long:

        byte[] b2 = new byte[40];
        int l = ByteBuffet.longLongToDecimal(uuid.getMostSignificantBits(), uuid.getLeastSignificantBits(), b2, 0);
        String n2 = new String(b2,0,l);

        Assert.assertEquals(n,n2);

    }


    private void assertDayEquals(String day) throws ParseException, IOException {

        DateFormat dayFormat = new SimpleDateFormat("yyyyMMdd");
        dayFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

        long parsedDayA = dayFormat.parse(day).getTime();
        long parsedDayB = ByteBuffet.parseEpocDate(day.getBytes(), 0);

        println("diff on "+day+": "+(parsedDayB-parsedDayA)/(24L*3600L*1000L));
        Assert.assertEquals(parsedDayA, parsedDayB);
    }

    private static void println(String s) { System.out.println(s);}

}
