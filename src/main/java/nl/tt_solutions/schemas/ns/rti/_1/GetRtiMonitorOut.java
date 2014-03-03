
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
 *         &lt;element name="ServiceInfoList" type="{http://www.tt-solutions.nl/schemas/NS/RTI/1.1/}ServiceInfoServiceJitList"/>
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
    "serviceInfoList"
})
@XmlRootElement(name = "GetRtiMonitorOut")
public class GetRtiMonitorOut {

    @XmlElement(name = "ServiceInfoList", required = true)
    protected ServiceInfoServiceJitList serviceInfoList;

    /**
     * Gets the value of the serviceInfoList property.
     * 
     * @return
     *     possible object is
     *     {@link ServiceInfoServiceJitList }
     *     
     */
    public ServiceInfoServiceJitList getServiceInfoList() {
        return serviceInfoList;
    }

    /**
     * Sets the value of the serviceInfoList property.
     * 
     * @param value
     *     allowed object is
     *     {@link ServiceInfoServiceJitList }
     *     
     */
    public void setServiceInfoList(ServiceInfoServiceJitList value) {
        this.serviceInfoList = value;
    }

}
