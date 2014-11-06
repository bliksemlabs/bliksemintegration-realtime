package nl.ovapi.rid.gtfsrt.services;

import nl.ovapi.arnu.FakeRidService;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.text.ParseException;
import java.util.TimeZone;

/**
 * Created by thomas on 6-11-14.
 */
public class DSTtest {

    @Before
    public void setUp() {
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Amsterdam"));
        DateTimeZone.setDefault(DateTimeZone.forID("Europe/Amsterdam"));
    }

    @Test
    public void testTime() throws ParseException {

        RIDservice api = new FakeRidService();
        Assert.assertEquals("2014-10-01T00:00:00.000+02:00", api.toDateTime(new LocalDate(2014, 10, 1), hour(0), minute(0)).toString());
        Assert.assertEquals("2014-10-01T01:00:00.000+02:00", api.toDateTime(new LocalDate(2014, 10, 1), hour(1), minute(0)).toString());
        Assert.assertEquals("2014-10-01T02:00:00.000+02:00", api.toDateTime(new LocalDate(2014, 10, 1), hour(2), minute(0)).toString());
        Assert.assertEquals("2014-10-01T03:00:00.000+02:00", api.toDateTime(new LocalDate(2014, 10, 1), hour(3), minute(0)).toString());
        Assert.assertEquals("2014-10-01T04:00:00.000+02:00", api.toDateTime(new LocalDate(2014, 10, 1), hour(4), minute(0)).toString());
        Assert.assertEquals("2014-10-01T05:00:00.000+02:00", api.toDateTime(new LocalDate(2014, 10, 1), hour(5), minute(0)).toString());
        Assert.assertEquals("2014-10-01T06:00:00.000+02:00", api.toDateTime(new LocalDate(2014, 10, 1), hour(6), minute(0)).toString());
        Assert.assertEquals("2014-10-01T07:00:00.000+02:00", api.toDateTime(new LocalDate(2014, 10, 1), hour(7), minute(0)).toString());
        Assert.assertEquals("2014-10-01T08:00:00.000+02:00", api.toDateTime(new LocalDate(2014, 10, 1), hour(8), minute(0)).toString());
        Assert.assertEquals("2014-10-01T09:00:00.000+02:00", api.toDateTime(new LocalDate(2014, 10, 1), hour(9), minute(0)).toString());
        Assert.assertEquals("2014-10-01T10:00:00.000+02:00", api.toDateTime(new LocalDate(2014, 10, 1), hour(10), minute(0)).toString());
        Assert.assertEquals("2014-10-01T11:00:00.000+02:00", api.toDateTime(new LocalDate(2014, 10, 1), hour(11), minute(0)).toString());
        Assert.assertEquals("2014-10-01T12:00:00.000+02:00", api.toDateTime(new LocalDate(2014, 10, 1), hour(12), minute(0)).toString());
        Assert.assertEquals("2014-10-01T13:00:00.000+02:00", api.toDateTime(new LocalDate(2014, 10, 1), hour(13), minute(0)).toString());
        Assert.assertEquals("2014-10-01T14:00:00.000+02:00", api.toDateTime(new LocalDate(2014, 10, 1), hour(14), minute(0)).toString());
        Assert.assertEquals("2014-10-01T15:00:00.000+02:00", api.toDateTime(new LocalDate(2014, 10, 1), hour(15), minute(0)).toString());
        Assert.assertEquals("2014-10-01T16:00:00.000+02:00", api.toDateTime(new LocalDate(2014, 10, 1), hour(16), minute(0)).toString());
        Assert.assertEquals("2014-10-01T17:00:00.000+02:00", api.toDateTime(new LocalDate(2014, 10, 1), hour(17), minute(0)).toString());
        Assert.assertEquals("2014-10-01T18:00:00.000+02:00", api.toDateTime(new LocalDate(2014, 10, 1), hour(18), minute(0)).toString());
        Assert.assertEquals("2014-10-01T19:00:00.000+02:00", api.toDateTime(new LocalDate(2014, 10, 1), hour(19), minute(0)).toString());
        Assert.assertEquals("2014-10-01T20:00:00.000+02:00", api.toDateTime(new LocalDate(2014, 10, 1), hour(20), minute(0)).toString());
        Assert.assertEquals("2014-10-01T21:00:00.000+02:00", api.toDateTime(new LocalDate(2014, 10, 1), hour(21), minute(0)).toString());
        Assert.assertEquals("2014-10-01T22:00:00.000+02:00", api.toDateTime(new LocalDate(2014, 10, 1), hour(22), minute(0)).toString());
        Assert.assertEquals("2014-10-01T23:00:00.000+02:00", api.toDateTime(new LocalDate(2014, 10, 1), hour(23), minute(0)).toString());
        Assert.assertEquals("2014-10-02T00:00:00.000+02:00", api.toDateTime(new LocalDate(2014, 10, 1), hour(24), minute(0)).toString());
        Assert.assertEquals("2014-10-02T01:00:00.000+02:00", api.toDateTime(new LocalDate(2014, 10, 1), hour(25), minute(0)).toString());
        Assert.assertEquals("2014-10-02T02:00:00.000+02:00", api.toDateTime(new LocalDate(2014, 10, 1), hour(26), minute(0)).toString());
        Assert.assertEquals("2014-10-02T03:00:00.000+02:00", api.toDateTime(new LocalDate(2014, 10, 1), hour(27), minute(0)).toString());

        Assert.assertEquals("2014-10-26T02:30:00.000+02:00", api.toDateTime(new LocalDate(2014, 10, 26), hour(2), minute(30)).toString());
        Assert.assertEquals("2014-10-26T04:30:00.000+01:00", api.toDateTime(new LocalDate(2014, 10, 26), hour(4), minute(30)).toString());
        Assert.assertEquals("2014-10-26T04:30:30.000+01:00", api.toDateTime(new LocalDate(2014, 10, 26), hour(4), minute(30) + 30).toString());

        //24:00 00:00
        //25:00 01:00
        //26:00 02:00
        //27:00 03:00
        //28:00 04:00

        Assert.assertEquals("2014-10-20T03:30:00.000+02:00", api.toDateTime(new LocalDate(2014, 10, 19), hour(27), minute(30)).toString());
        Assert.assertEquals("2014-10-20T00:00:00.000+02:00", api.toDateTime(new LocalDate(2014, 10, 19), hour(24), minute(0)).toString());
        Assert.assertEquals("2014-10-20T00:00:30.000+02:00", api.toDateTime(new LocalDate(2014, 10, 19), hour(24), second(30)).toString());

        Assert.assertEquals("2014-10-26T00:00:00.000+02:00", api.toDateTime(new LocalDate(2014, 10, 25), hour(24), minute(0)).toString());
        Assert.assertEquals("2014-10-26T01:00:00.000+02:00", api.toDateTime(new LocalDate(2014, 10, 25), hour(25), minute(0)).toString());
        Assert.assertEquals("2014-10-26T02:00:00.000+02:00", api.toDateTime(new LocalDate(2014, 10, 25), hour(26), minute(0)).toString());
        Assert.assertEquals("2014-10-26T02:00:00.000+01:00", api.toDateTime(new LocalDate(2014, 10, 25), hour(27), minute(0)).toString());
        Assert.assertEquals("2014-10-26T03:00:00.000+01:00", api.toDateTime(new LocalDate(2014, 10, 25), hour(28), minute(0)).toString());
        Assert.assertEquals("2014-10-26T04:00:00.000+01:00", api.toDateTime(new LocalDate(2014, 10, 25), hour(29), minute(0)).toString());

        Assert.assertEquals("2014-10-25T07:30:00.000+02:00", api.toDateTime(new LocalDate(2014, 10, 25), hour(07), minute(30)).toString());
        Assert.assertEquals("2014-10-25T14:30:00.000+02:00", api.toDateTime(new LocalDate(2014, 10, 25), hour(14), minute(30)).toString());
        Assert.assertEquals("2014-10-25T23:30:00.000+02:00", api.toDateTime(new LocalDate(2014, 10, 25), hour(23), minute(30)).toString());
        Assert.assertEquals("2014-10-26T00:30:00.000+02:00", api.toDateTime(new LocalDate(2014, 10, 25), hour(24), minute(30)).toString());
        Assert.assertEquals("2014-10-26T01:30:00.000+02:00", api.toDateTime(new LocalDate(2014, 10, 25), hour(25), minute(30)).toString());
        Assert.assertEquals("2014-10-26T02:30:00.000+02:00", api.toDateTime(new LocalDate(2014, 10, 25), hour(26), minute(30)).toString());
        Assert.assertEquals("2014-10-26T02:30:00.000+01:00", api.toDateTime(new LocalDate(2014, 10, 25), hour(27), minute(30)).toString());
        Assert.assertEquals("2014-10-26T03:30:00.000+01:00", api.toDateTime(new LocalDate(2014, 10, 25), hour(28), minute(30)).toString());
        Assert.assertEquals("2014-10-26T04:30:00.000+01:00", api.toDateTime(new LocalDate(2014, 10, 25), hour(29), minute(30)).toString());

        Assert.assertEquals("2014-10-26T00:30:00.000+02:00", api.toDateTime(new LocalDate(2014, 10, 26), hour(0), minute(30)).toString());
        Assert.assertEquals("2014-10-26T01:30:00.000+02:00", api.toDateTime(new LocalDate(2014, 10, 26), hour(1), minute(30)).toString());
        Assert.assertEquals("2014-10-26T02:30:00.000+02:00", api.toDateTime(new LocalDate(2014, 10, 26), hour(2), minute(30)).toString());
        Assert.assertEquals("2014-10-26T03:30:00.000+01:00", api.toDateTime(new LocalDate(2014, 10, 26), hour(3), minute(30)).toString());
        Assert.assertEquals("2014-10-26T04:30:00.000+01:00", api.toDateTime(new LocalDate(2014, 10, 26), hour(4), minute(30)).toString());
        Assert.assertEquals("2014-10-26T05:30:00.000+01:00", api.toDateTime(new LocalDate(2014, 10, 26), hour(5), minute(30)).toString());
        Assert.assertEquals("2014-10-26T14:30:00.000+01:00", api.toDateTime(new LocalDate(2014, 10, 26), hour(14), minute(30)).toString());
        Assert.assertEquals("2014-10-26T23:30:00.000+01:00", api.toDateTime(new LocalDate(2014, 10, 26), hour(23), minute(30)).toString());
        Assert.assertEquals("2014-10-27T00:30:00.000+01:00", api.toDateTime(new LocalDate(2014, 10, 26), hour(24), minute(30)).toString());
        Assert.assertEquals("2014-10-27T01:30:00.000+01:00", api.toDateTime(new LocalDate(2014, 10, 26), hour(25), minute(30)).toString());
        Assert.assertEquals("2014-10-27T02:30:00.000+01:00", api.toDateTime(new LocalDate(2014, 10, 26), hour(26), minute(30)).toString());

        Assert.assertEquals("2015-03-29T00:30:00.000+01:00", api.toDateTime(new LocalDate(2015, 3, 29), hour(0), minute(30)).toString());
        Assert.assertEquals("2015-03-29T01:30:00.000+01:00", api.toDateTime(new LocalDate(2015, 3, 29), hour(1), minute(30)).toString());
        Assert.assertTrue(api.tripDepartureInDSTGap(new LocalDate(2015, 3, 29), hour(2)));
        Assert.assertNull(api.toDateTime(new LocalDate(2015, 3, 29), hour(2), minute(30))); //Time does not exist
        Assert.assertEquals("2015-03-29T03:30:00.000+02:00", api.toDateTime(new LocalDate(2015, 3, 29), hour(3), minute(30)).toString());
        Assert.assertEquals("2015-03-29T04:30:00.000+02:00", api.toDateTime(new LocalDate(2015, 3, 29), hour(4), minute(30)).toString());
        Assert.assertEquals("2015-03-29T05:30:00.000+02:00", api.toDateTime(new LocalDate(2015, 3, 29), hour(5), minute(30)).toString());
        Assert.assertEquals("2015-03-29T06:30:00.000+02:00", api.toDateTime(new LocalDate(2015, 3, 29), hour(6), minute(30)).toString());
        Assert.assertEquals("2015-03-29T23:30:00.000+02:00", api.toDateTime(new LocalDate(2015, 3, 29), hour(23), minute(30)).toString());
        Assert.assertEquals("2015-03-30T00:30:00.000+02:00", api.toDateTime(new LocalDate(2015, 3, 29), hour(24), minute(30)).toString());
        Assert.assertEquals("2015-03-30T01:30:00.000+02:00", api.toDateTime(new LocalDate(2015, 3, 29), hour(25), minute(30)).toString());
        Assert.assertEquals("2015-03-30T02:30:00.000+02:00", api.toDateTime(new LocalDate(2015, 3, 29), hour(26), minute(30)).toString());
        Assert.assertEquals("2015-03-30T03:30:00.000+02:00", api.toDateTime(new LocalDate(2015, 3, 29), hour(27), minute(30)).toString());

        Assert.assertEquals("2015-03-29T00:30:00.000+01:00", api.toDateTime(new LocalDate(2015, 3, 28), hour(24), minute(30)).toString());
        Assert.assertEquals("2015-03-29T01:30:00.000+01:00", api.toDateTime(new LocalDate(2015, 3, 28), hour(25), minute(30)).toString());
        Assert.assertEquals("2015-03-29T03:30:00.000+02:00", api.toDateTime(new LocalDate(2015, 3, 28), hour(26), minute(30)).toString());
        Assert.assertEquals("2015-03-29T04:30:00.000+02:00", api.toDateTime(new LocalDate(2015, 3, 28), hour(27), minute(30)).toString());
        Assert.assertEquals("2015-03-29T05:30:00.000+02:00", api.toDateTime(new LocalDate(2015, 3, 28), hour(28), minute(30)).toString());
        Assert.assertEquals("2015-03-29T06:30:00.000+02:00", api.toDateTime(new LocalDate(2015, 3, 28), hour(29), minute(30)).toString());
        Assert.assertEquals("2015-03-29T07:30:00.000+02:00", api.toDateTime(new LocalDate(2015, 3, 28), hour(30), minute(30)).toString());

        Assert.assertEquals("2014-10-26T02:58:00.000+02:00", api.toDateTime(new LocalDate(2014, 10, 25), hour(26) + minute(58), 0).toString());
        Assert.assertEquals("2014-10-26T02:00:00.000+01:00", api.toDateTime(new LocalDate(2014, 10, 25), hour(27), minute(0)).toString());
    }

    private int hour(int hour) {
        return hour * 60 * 60;
    }

    private int minute(int minute) {
        return minute * 60;
    }

    private int second(int seconds) {
        return seconds;
    }

}
