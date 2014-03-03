
package nl.tt_solutions.schemas.ns.rti._1;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for anonymous complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="ReturnValue" type="{http://www.tt-solutions.nl/schemas/NS/RTI/1.1/}PlatformInfoReturn"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "returnValue"
})
@XmlRootElement(name = "PutPlatformInfoOut")
public class PutPlatformInfoOut {

    @XmlElement(name = "ReturnValue", required = true)
    protected PlatformInfoReturn returnValue;

    /**
     * Gets the value of the returnValue property.
     * 
     * @return
     *     possible object is
     *     {@link PlatformInfoReturn }
     *     
     */
    public PlatformInfoReturn getReturnValue() {
        return returnValue;
    }

    /**
     * Sets the value of the returnValue property.
     * 
     * @param value
     *     allowed object is
     *     {@link PlatformInfoReturn }
     *     
     */
    public void setReturnValue(PlatformInfoReturn value) {
        this.returnValue = value;
    }

}
