package nl.ovapi.bison.sax;

import java.util.ArrayList;

import lombok.Getter;
import nl.ovapi.bison.model.AdviceType;
import nl.ovapi.bison.model.DataOwnerCode;
import nl.ovapi.bison.model.EffectType;
import nl.ovapi.bison.model.KV15message;
import nl.ovapi.bison.model.MessageDurationType;
import nl.ovapi.bison.model.MessagePriority;
import nl.ovapi.bison.model.MessageType;
import nl.ovapi.bison.model.ReasonType;
import nl.ovapi.bison.model.SubAdviceType;
import nl.ovapi.bison.model.SubEffectType;
import nl.ovapi.bison.model.SubMeasureType;
import nl.ovapi.bison.model.SubReasonType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class KV15SAXHandler extends DefaultHandler {

	private StringBuilder builder;
	private KV15message message = null;
	@Getter
	private ArrayList<KV15message> messages;

	public KV15SAXHandler() {
	}
	private static final Logger _log = LoggerFactory.getLogger(KV15SAXHandler.class);

	@Override
	public void startDocument() throws SAXException {
		super.startDocument();
		builder = new StringBuilder();
		messages = new ArrayList<KV15message>();
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
		if ("DELETEMESSAGE".equals(localName)) {
			message = new KV15message();
			message.setIsDelete(true);
		}else if ("STOPMESSAGE".equals(localName)) {
			message = new KV15message();
			message.setIsDelete(false);
		}
	}

	@Override
	public void endDocument() throws SAXException {
	}

	@Override
	public void endElement(String uri, String localName, String name)
			throws SAXException {
		super.endElement(uri, localName, name);
		try{
			if ("dataownercode".equals(localName)) {
				message.setDataOwnerCode(DataOwnerCode.valueOf(builder.toString()));
			} else if ("messagecodedate".equals(localName)) {
				message.setMessageCodeDate(builder.toString());
			} else if ("messagecodenumber".equals(localName)) {
				message.setMessageCodeNumber(Integer.valueOf(builder.toString()));
			} else if ("userstopcode".equals(localName)) {
				message.addUserstopCode(builder.toString());
			} else if ("lineplanningnumber".equals(localName)) {
				message.addLinePlanningNumber(builder.toString());
			} else if ("messagepriority".equals(localName)) {
				message.setMessagePriority(MessagePriority.valueOf(builder.toString()));
			} else if ("messagetype".equals(localName)) {
				message.setMessageType(MessageType.valueOf(builder.toString()));
			} else if ("messagedurationtype".equals(localName)) {
				message.setMessageDurationType(MessageDurationType.valueOf(builder.toString()));
			} else if ("messagestarttime".equals(localName)) {
				message.setMessageStartTime(DateUtils.parse(builder.toString()));
			} else if ("messageendtime".equals(localName)) {
				message.setMessageEndTime(DateUtils.parse(builder.toString()));
			}else if ("messagecontent".equals(localName)) {
				message.setMessageContent(builder.toString());
			}else if ("reasontype".equals(localName)) {
				message.setReasonType(ReasonType.parse(builder.toString()));
			} else if ("subreasontype".equals(localName)) {
				message.setSubReasonType(SubReasonType.parse(builder.toString()));
			} else if ("reasoncontent".equals(localName)) {
				message.setReasonContent(builder.toString());
			} else if ("effecttype".equals(localName)) {
				message.setEffectType(EffectType.parse(builder.toString()));
			} else if ("subeffecttype".equals(localName)) {
				message.setSubEffectType(SubEffectType.parse(builder.toString()));
			} else if ("effectcontent".equals(localName)) {
				message.setEffectContent(builder.toString());
			} else if ("advicetype".equals(localName)) {
				message.setAdviceType(AdviceType.parse(builder.toString()));
			} else if ("subadvicetype".equals(localName)) {
				message.setSubAdviceType(SubAdviceType.parse(builder.toString()));
			} else if ("advicecontent".equals(localName)) {
				message.setAdviceContent(builder.toString());
			} else if ("measuretype".equals(localName)) {
				message.setMeasureType(Integer.valueOf(builder.toString()));
			} else if ("submeasuretype".equals(localName)) {
				message.setSubMeasureType(SubMeasureType.parse(builder.toString()));
			} else if ("measurecontent".equals(localName)) {
				message.setMeasureContent(builder.toString());
			} else if ("messagetimestamp".equals(localName)) {
				message.setMessageTimeStamp(DateUtils.parse(builder.toString()));
			} else if ("DELETEMESSAGE".equals(localName)) {
				messages.add(message);
				message = null;
			} else if ("STOPMESSAGE".equals(localName)) {
				messages.add(message);
				message = null;
			}
		}catch (Exception e){
			_log.error("endElement",e);
		}
		builder.setLength(0);
	}
}
