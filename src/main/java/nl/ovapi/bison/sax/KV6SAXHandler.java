package nl.ovapi.bison.sax;

import lombok.Getter;
import nl.ovapi.bison.DateUtils;
import nl.ovapi.bison.model.DataOwnerCode;
import nl.ovapi.bison.model.KV6posinfo;
import nl.ovapi.bison.model.KV6posinfo.Type;
import nl.ovapi.bison.model.Source;
import nl.ovapi.bison.model.WheelChairAccessible;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.ArrayList;

public class KV6SAXHandler extends DefaultHandler {

    private StringBuilder builder;
    private KV6posinfo posinfo = null;
    @Getter
    private ArrayList<KV6posinfo> posinfos;


    public KV6SAXHandler() {
    }

    @Override
    public void startDocument() throws SAXException {
        super.startDocument();
        builder = new StringBuilder();
        posinfos = new ArrayList<KV6posinfo>();
    }

    @Override
    public void characters(char[] ch, int start, int length)
            throws SAXException {
        super.characters(ch, start, length);
        builder.append(ch, start, length);
    }

    @Override
    public void startElement(String uri, String localName, String name,
                             Attributes attr) throws SAXException {
        super.startElement(uri, localName, name, attr);
        builder.setLength(0);
        if ("KV6posinfo".equals(localName)) {
            posinfo = new KV6posinfo();
        } else if (posinfo != null && posinfo.getMessagetype() == null && !"delimiter".equals(localName)) {
            posinfo.setMessagetype(Type.valueOf(localName));
        }
    }

    @Override
    public void endDocument() throws SAXException {
    }

    @Override
    public void endElement(String uri, String localName, String name)
            throws SAXException {
        super.endElement(uri, localName, name);
        if ("dataownercode".equals(localName)) {
            posinfo.setDataownercode(DataOwnerCode.valueOf(builder.toString()));
        } else if ("lineplanningnumber".equals(localName)) {
            posinfo.setLineplanningnumber(builder.toString());
        } else if ("journeynumber".equals(localName)) {
            posinfo.setJourneynumber(Integer.valueOf(builder.toString()));
        } else if ("reinforcementnumber".equals(localName)) {
            posinfo.setReinforcementnumber(Integer.valueOf(builder.toString()));
        } else if ("timestamp".equals(localName)) {
            posinfo.setTimestamp(DateUtils.parse(builder.toString()));
        } else if ("operatingday".equals(localName)) {
            posinfo.setOperatingday(builder.toString());
        } else if ("source".equals(localName)) {
            posinfo.setSource(Source.valueOf(builder.toString()));
        } else if ("userstopcode".equals(localName)) {
            posinfo.setUserstopcode(builder.toString());
        } else if ("passagesequencenumber".equals(localName)) {
            posinfo.setPassagesequencenumber(Integer.valueOf(builder.toString()));
        } else if ("vehiclenumber".equals(localName)) {
            posinfo.setVehiclenumber(Integer.valueOf(builder.toString()));
        } else if ("blockcode".equals(localName)) {
            posinfo.setBlockcode(Integer.valueOf(builder.toString()));
        } else if ("wheelchairaccessible".equals(localName)) {
            posinfo.setWheelchairaccessible(WheelChairAccessible
                    .valueOf(builder.toString()));
        } else if ("numberofcoaches".equals(localName)) {
            posinfo.setNumberofcoaches(Short.valueOf(builder.toString()));
        } else if ("distancesincelastuserstop".equals(localName)) {
            if (builder.length() > 0)
                posinfo.setDistancesincelastuserstop(Integer.valueOf(builder
                        .toString()));
        } else if ("rd-x".equals(localName)) {
            posinfo.setRd_x(Integer.valueOf(builder.toString()));
        } else if ("rd-y".equals(localName)) {
            posinfo.setRd_y(Integer.valueOf(builder.toString()));
        } else if ("punctuality".equals(localName)) {
            posinfo.setPunctuality(Integer.valueOf(builder.toString()));
        }
        if (posinfo != null && posinfo.getMessagetype() != null
                && posinfo.getMessagetype().toString().equals(localName)) {
            posinfos.add(posinfo);
            posinfo = new KV6posinfo();
        }
        builder.setLength(0);
    }
}
