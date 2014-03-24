package nl.tt_solutions.schemas.ns.rti._1;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

public class DateAdapter extends XmlAdapter<String, XMLGregorianCalendar> {

    private final static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
    
    static {
    	dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
    }
    
    @Override
    public String marshal(XMLGregorianCalendar v) throws Exception {
        return dateFormat.format(v.toGregorianCalendar().getTime());
    }

    @Override
    public XMLGregorianCalendar unmarshal(String v) throws Exception {
    	return DatatypeFactory.newInstance().newXMLGregorianCalendar(v);
    }

}
