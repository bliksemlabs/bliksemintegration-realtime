package nl.tt_solutions.schemas.ns.rti._1;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;

public class DelayAdapter extends XmlAdapter<String, Duration> {

	@Override
	public String marshal(Duration v) throws Exception {
		int seconds = v.getDays()*60*60*24+v.getHours()*60*60+v.getMinutes()*60+v.getSeconds();
		StringBuilder sb = new StringBuilder();
		sb.append("PT");
		sb.append(seconds/60);
		sb.append("M");
		if (seconds%60 > 0){
			sb.append(seconds%60);
			sb.append("S");
		}
		return sb.toString();
	}

	@Override
	public Duration unmarshal(String v) throws Exception {
		return DatatypeFactory.newInstance().newDuration(v);
	}

}
