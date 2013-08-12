package nl.ovapi.bison.sax;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import lombok.Getter;
import nl.ovapi.bison.model.DataOwnerCode;
import nl.ovapi.bison.model.JourneyStopType;
import nl.ovapi.bison.model.KV17cvlinfo;
import nl.ovapi.bison.model.KV17cvlinfo.Mutation;
import nl.ovapi.bison.model.KV17cvlinfo.Mutation.MessageType;
import nl.ovapi.bison.model.KV17cvlinfo.Mutation.MutationType;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class KV17SAXHandler extends DefaultHandler {

	private StringBuilder builder;
	@Getter
	private ArrayList<KV17cvlinfo> cvlinfos = null;
	private KV17cvlinfo cvlinfo = null;

	private Set<String> possiblemutations;

	public KV17SAXHandler() {
		possiblemutations = new HashSet<String>();
		cvlinfos = new ArrayList<KV17cvlinfo>();
		for (MutationType type : KV17cvlinfo.Mutation.MutationType.values()) {
			possiblemutations.add(type.name());
		}
	}

	@Override
	public void startDocument() throws SAXException {
		super.startDocument();
		builder = new StringBuilder();
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
        if ("KV17JOURNEY".equals(localName)) {
			kv17journey = true;
			cvlinfo = new KV17cvlinfo();
			messagetype = null;
			mutationtype = null;
		} else if ("KV17MUTATEJOURNEY".equals(localName)) {
			messagetype = MessageType.KV17MUTATEJOURNEY;
		} else if ("KV17MUTATEJOURNEYSTOP".equals(localName)) {
			messagetype = MessageType.KV17MUTATEJOURNEYSTOP;
		} else if (possiblemutations.contains(localName)) {
			mutationtype = MutationType.valueOf(localName);
			mutation = new Mutation();
			mutation.setMessagetype(messagetype);
			mutation.setMutationtype(mutationtype);
		}
	}

	private boolean kv17journey;
	private MessageType messagetype;
	private MutationType mutationtype;
	private Mutation mutation;

	@Override
	public void endDocument() throws SAXException {
	}

	private int secondssincemidnight(String time) {
		String[] values = time.split(":");
		int seconds = Integer.parseInt(values[2]);
		seconds += Integer.parseInt(values[1]) * 60;
		seconds += Integer.parseInt(values[0]) * 3600;
		return seconds;
	}

	@Override
	public void endElement(String uri, String localName, String name)
			throws SAXException {
		super.endElement(uri, localName, name);
		if ("KV17MUTATEJOURNEY".equals(localName)) {
			messagetype = null;
		} else if ("KV17MUTATEJOURNEYSTOP".equals(localName)) {
			messagetype = null;
		}
		if (kv17journey) {
			if ("dataownercode".equals(localName)) {
				cvlinfo.setDataownercode(DataOwnerCode.valueOf(builder
						.toString()));
			} else if ("lineplanningnumber".equals(localName)) {
				cvlinfo.setLineplanningnumber(builder.toString());
			} else if ("operatingday".equals(localName)) {
				cvlinfo.setOperatingday(builder.toString());
			} else if ("journeynumber".equals(localName)) {
				cvlinfo.setJourneynumber(Integer.valueOf(builder.toString()));
			} else if ("reinforcementnumber".equals(localName)) {
				cvlinfo.setReinforcementnumber(Integer.valueOf(builder
						.toString()));
			} else if ("KV17cvlinfo".equals(localName)) {
				kv17journey = false;
				cvlinfos.add(cvlinfo);
				cvlinfo = null;
				messagetype = null;
			}
		}
		if (mutation != null) {
			if (localName.equals(mutationtype.name())) {
				cvlinfo.getMutations().add(mutation);
				mutation = null;
				mutationtype = null;
			} else if ("userstopcode".equals(localName)) {
				mutation.setUserstopcode(builder.toString());
			} else if ("passagesequencenumber".equals(localName)) {
				mutation.setPassagesequencenumber(Integer.valueOf(builder
						.toString()));
			} else if ("advicetype".equals(localName)) {
				mutation.setAdvicetype(builder.toString());
			} else if ("subadvicetype".equals(localName)) {
				mutation.setSubadvicetype(builder.toString());
			} else if ("advicecontent".equals(localName)) {
				mutation.setAdvicecontent(builder.toString());
			} else if ("reasontype".equals(localName)) {
				mutation.setReasontype(builder.toString());
			} else if ("subreasontype".equals(localName)) {
				mutation.setSubreasontype(builder.toString());
			} else if ("reasoncontent".equals(localName)) {
				mutation.setReasoncontent(builder.toString());
			} else if ("lagtime".equals(localName)) {
				mutation.setLagtime(Integer.valueOf(builder.toString()));
				;
			} else if ("targetarrivaltime".equals(localName)) {
				mutation.setTargetarrivaltime(secondssincemidnight(builder
						.toString()));
			} else if ("targetdeparturetime".equals(localName)) {
				mutation.setTargetdeparturetime(secondssincemidnight(builder
						.toString()));
			} else if ("journeystoptype".equals(localName)) {
				mutation.setJourneystoptype(JourneyStopType.valueOf(builder
						.toString()));
			} else if ("destinationcode".equals(localName)) {
				mutation.setDestinationcode(builder.toString());
			} else if ("destinationname50".equals(localName)) {
				mutation.setDestinationname50(builder.toString());
			} else if ("destinationname16".equals(localName)) {
				mutation.setDestinationname16(builder.toString());
			} else if ("destinationdetail16".equals(localName)) {
				mutation.setDestinationdetail16(builder.toString());
			} else if ("destinationdisplay16".equals(localName)) {
				mutation.setDestinationdisplay16(builder.toString());
			}
		} else if ("timestamp".equals(localName) && cvlinfo != null) {
			cvlinfo.setTimestamp(DateUtils.parse(builder.toString()));
		}
		builder.setLength(0);
	}
}
