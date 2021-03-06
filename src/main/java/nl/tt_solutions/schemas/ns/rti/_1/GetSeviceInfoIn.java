
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
 *         &lt;element name="ServiceInput" type="{http://www.tt-solutions.nl/schemas/NS/RTI/1.1/}ServiceInputType"/>
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
    "serviceInput"
})
@XmlRootElement(name = "GetSeviceInfoIn")
public class GetSeviceInfoIn {

    @XmlElement(name = "ServiceInput", required = true)
    protected ServiceInputType serviceInput;

    /**
     * Gets the value of the serviceInput property.
     * 
     * @return
     *     possible object is
     *     {@link ServiceInputType }
     *     
     */
    public ServiceInputType getServiceInput() {
        return serviceInput;
    }

    /**
     * Sets the value of the serviceInput property.
     * 
     * @param value
     *     allowed object is
     *     {@link ServiceInputType }
     *     
     */
    public void setServiceInput(ServiceInputType value) {
        this.serviceInput = value;
    }

}
