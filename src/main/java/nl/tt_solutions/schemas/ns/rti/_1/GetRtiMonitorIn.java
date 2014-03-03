
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
 *         &lt;element name="RtiMonitorInput" type="{http://www.tt-solutions.nl/schemas/NS/RTI/1.1/}RtiMonitorInputType"/>
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
    "rtiMonitorInput"
})
@XmlRootElement(name = "GetRtiMonitorIn")
public class GetRtiMonitorIn {

    @XmlElement(name = "RtiMonitorInput", required = true)
    protected RtiMonitorInputType rtiMonitorInput;

    /**
     * Gets the value of the rtiMonitorInput property.
     * 
     * @return
     *     possible object is
     *     {@link RtiMonitorInputType }
     *     
     */
    public RtiMonitorInputType getRtiMonitorInput() {
        return rtiMonitorInput;
    }

    /**
     * Sets the value of the rtiMonitorInput property.
     * 
     * @param value
     *     allowed object is
     *     {@link RtiMonitorInputType }
     *     
     */
    public void setRtiMonitorInput(RtiMonitorInputType value) {
        this.rtiMonitorInput = value;
    }

}
