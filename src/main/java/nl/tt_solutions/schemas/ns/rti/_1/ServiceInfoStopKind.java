
package nl.tt_solutions.schemas.ns.rti._1;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for ServiceInfoStopKind.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="ServiceInfoStopKind">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="Cancelled-Stop"/>
 *     &lt;enumeration value="Diverted-Stop"/>
 *     &lt;enumeration value="Split-Stop"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "ServiceInfoStopKind")
@XmlEnum
public enum ServiceInfoStopKind {

    @XmlEnumValue("Cancelled-Stop")
    CANCELLED_STOP("Cancelled-Stop"),
    @XmlEnumValue("Diverted-Stop")
    DIVERTED_STOP("Diverted-Stop"),
    @XmlEnumValue("Split-Stop")
    SPLIT_STOP("Split-Stop");
    private final String value;

    ServiceInfoStopKind(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static ServiceInfoStopKind fromValue(String v) {
        for (ServiceInfoStopKind c: ServiceInfoStopKind.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
