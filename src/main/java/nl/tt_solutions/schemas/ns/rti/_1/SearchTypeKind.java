
package nl.tt_solutions.schemas.ns.rti._1;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for SearchTypeKind.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="SearchTypeKind">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="STATIC"/>
 *     &lt;enumeration value="REALTIME"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "SearchTypeKind")
@XmlEnum
public enum SearchTypeKind {

    STATIC,
    REALTIME;

    public String value() {
        return name();
    }

    public static SearchTypeKind fromValue(String v) {
        return valueOf(v);
    }

}
