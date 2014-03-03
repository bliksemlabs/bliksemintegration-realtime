
package nl.tt_solutions.schemas.ns.rti._1;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for ReturnValueKind.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="ReturnValueKind">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="Return-OK"/>
 *     &lt;enumeration value="Return-Error-Resend"/>
 *     &lt;enumeration value="Return-Error-DoNotResend"/>
 *     &lt;enumeration value="Return-Restart"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "ReturnValueKind")
@XmlEnum
public enum ReturnValueKind {

    @XmlEnumValue("Return-OK")
    RETURN_OK("Return-OK"),
    @XmlEnumValue("Return-Error-Resend")
    RETURN_ERROR_RESEND("Return-Error-Resend"),
    @XmlEnumValue("Return-Error-DoNotResend")
    RETURN_ERROR_DO_NOT_RESEND("Return-Error-DoNotResend"),
    @XmlEnumValue("Return-Restart")
    RETURN_RESTART("Return-Restart");
    private final String value;

    ReturnValueKind(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static ReturnValueKind fromValue(String v) {
        for (ReturnValueKind c: ReturnValueKind.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
