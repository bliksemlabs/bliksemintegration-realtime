
package nl.tt_solutions.schemas.ns.rti._1;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for ServiceInfoKind.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="ServiceInfoKind">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="Normal-Service"/>
 *     &lt;enumeration value="New-Service"/>
 *     &lt;enumeration value="Cancelled-Service"/>
 *     &lt;enumeration value="Diverted-Service"/>
 *     &lt;enumeration value="Extended-Service"/>
 *     &lt;enumeration value="ScheduleChanged-Service"/>
 *     &lt;enumeration value="Split-Service"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "ServiceInfoKind")
@XmlEnum
public enum ServiceInfoKind {

    @XmlEnumValue("Normal-Service")
    NORMAL_SERVICE("Normal-Service"),
    @XmlEnumValue("New-Service")
    NEW_SERVICE("New-Service"),
    @XmlEnumValue("Cancelled-Service")
    CANCELLED_SERVICE("Cancelled-Service"),
    @XmlEnumValue("Diverted-Service")
    DIVERTED_SERVICE("Diverted-Service"),
    @XmlEnumValue("Extended-Service")
    EXTENDED_SERVICE("Extended-Service"),
    @XmlEnumValue("ScheduleChanged-Service")
    SCHEDULE_CHANGED_SERVICE("ScheduleChanged-Service"),
    @XmlEnumValue("Split-Service")
    SPLIT_SERVICE("Split-Service");
    private final String value;

    ServiceInfoKind(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static ServiceInfoKind fromValue(String v) {
        for (ServiceInfoKind c: ServiceInfoKind.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
