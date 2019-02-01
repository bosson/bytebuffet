package se.illuminet.bytebuffet;

/**
 * Byte parsing 
 *
 * 
 */
public final class EpochDate {

    private static int mdays[] = {
            0,
            31,
            31 + 28, // leap add
            31 + 28 + 31,
            31 + 28 + 31 + 30,
            31 + 28 + 31 + 30 + 31,
            31 + 28 + 31 + 30 + 31 + 30,
            31 + 28 + 31 + 30 + 31 + 30 + 31,
            31 + 28 + 31 + 30 + 31 + 30 + 31 + 31,
            31 + 28 + 31 + 30 + 31 + 30 + 31 + 31 + 30,
            31 + 28 + 31 + 30 + 31 + 30 + 31 + 31 + 30 + 31,
            31 + 28 + 31 + 30 + 31 + 30 + 31 + 31 + 30 + 31 + 30,
            31 + 28 + 31 + 30 + 31 + 30 + 31 + 31 + 30 + 31 + 30 + 31 };

    /**
     * turn a days date into milliseconds from epoch
     * @param yyyy year as is.
     * @param mm month (1 is january)
     * @param dd day of month (1 is first day)
     * @return
     */
    public static long epochTime(final int yyyy,final int mm,final int dd) {

        long days = (yyyy - 1970) * 365;
        int leapYears = ((yyyy-1) - 1968)/4; // first epoch leap year was 1972
        if (yyyy >= 2100) leapYears -= (yyyy-2001)/100; // exempt 100 every 400
        days+=leapYears;
        days+=mdays[mm-1];
        days+=(dd - 1);

        final boolean is100YearNonLeap = ((yyyy-2000) % 100) == 0 && ((yyyy-2000) % 400) != 0;
        boolean isLeap = ((yyyy-1972)&3) == 0 && !is100YearNonLeap;
        if (isLeap && mm > 2) days++; // leaped this year!

        return 24L*3600L*1000L*days;
    }

    /**
     * conversts millis since epoch into long with yyyymmdd value.
     * @param epocMillis
     * @return date in numeric/decimal yyyymmdd representation.
     */
    public static int epochToDate(final long epocMillis) {

        long ldays = epocMillis / (24*3600*1000);
        final int days = (int)ldays;
        final int yyyy = (int)days / 365 + 1970;
        int leapYears = ((yyyy-1) - 1968)/4;
        if (yyyy >= 2100) leapYears -= (yyyy-2001)/100;
        int dd=days-((yyyy-1970)*365)-leapYears;

        final boolean is100YearNonLeap = ((yyyy-2000) % 100) == 0 && ((yyyy-2000) % 400) != 0;
        final boolean isLeap = ((yyyy-1972)&3) == 0 && !is100YearNonLeap;

        int mm = 0;
        while(dd >= mdays[mm]) mm++;

        if (isLeap) {
            if (dd == 31+28) mm--;
            else if (dd > 31+28) dd--;
        }

        dd=dd-mdays[mm-1];

        return yyyy*10000 + (mm) * 100 + dd + 1;

    }

    /**
     * Time of day.
     */
    public static long parseEpocTimeOfDay(final byte b[], final int start) {
        int s = (b[start+5]-'0');
        s += (b[start+4]-'0') * 10;
        s += (b[start+3]-'0') * 60;
        s += (b[start+2]-'0') * 600;
        s += (b[start+1]-'0') * 3600;
        s += (b[start+0]-'0') * 36000;
        return (long)s * 1000L;
    }

}
