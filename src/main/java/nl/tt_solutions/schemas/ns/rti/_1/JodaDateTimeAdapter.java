package nl.tt_solutions.schemas.ns.rti._1;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import javax.xml.bind.annotation.adapters.XmlAdapter;

public class JodaDateTimeAdapter extends XmlAdapter<String, DateTime> {

    private static final DateTimeFormatter isoParser = ISODateTimeFormat.dateTimeParser();
    private static final DateTimeFormatter arnuFormat = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ssZ");

    public DateTime unmarshal(String v) throws Exception {
        return isoParser.parseDateTime(v);
    }

    public String marshal(DateTime v) throws Exception {
        return v.toString(arnuFormat);
    }
}