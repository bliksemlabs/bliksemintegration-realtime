
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
 *         &lt;element name="ReturnValue" type="{http://www.tt-solutions.nl/schemas/NS/RTI/1.1/}ServiceInfoReturn"/>
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
@XmlRootElement(name = "PutServiceInfoOut")
public class PutServiceInfoOut {

    @XmlElement(name = "ReturnValue", required = true)
    protected ServiceInfoReturn returnValue;

    /**
     * Gets the value of the returnValue property.
     * 
     * @return
     *     possible object is
     *     {@link ServiceInfoReturn }
     *     
     */
    public ServiceInfoReturn getReturnValue() {
        return returnValue;
    }

    /**
     * Sets the value of the returnValue property.
     * 
     * @param value
     *     allowed object is
     *     {@link ServiceInfoReturn }
     *     
     */
    public void setReturnValue(ServiceInfoReturn value) {
        this.returnValue = value;
    }

}
