
package nl.tt_solutions.schemas.ns.rti._1;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * Description of the service information
 * 
 * <p>Java class for ServiceInfoServiceType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="ServiceInfoServiceType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="CompanyCode" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="ServiceCode" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="TransportModeCode" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="StopList">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 &lt;sequence>
 *                   &lt;element name="Stop" type="{http://www.tt-solutions.nl/schemas/NS/RTI/1.1/}ServiceInfoStopType" maxOccurs="unbounded"/>
 *                 &lt;/sequence>
 *               &lt;/restriction>
 *             &lt;/complexContent>
 *           &lt;/complexType>
 *         &lt;/element>
 *       &lt;/sequence>
 *       &lt;attribute name="ServiceType" use="required" type="{http://www.tt-solutions.nl/schemas/NS/RTI/1.1/}ServiceInfoKind" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ServiceInfoServiceType", propOrder = {
    "companyCode",
    "serviceCode",
    "transportModeCode",
    "stopList"
})
public class ServiceInfoServiceType {

    @XmlElement(name = "CompanyCode", required = true)
    protected String companyCode;
    @XmlElement(name = "ServiceCode", required = true)
    protected String serviceCode;
    @XmlElement(name = "TransportModeCode")
    protected String transportModeCode;
    @XmlElement(name = "StopList", required = true)
    protected ServiceInfoServiceType.StopList stopList;
    @XmlAttribute(name = "ServiceType", required = true)
    protected ServiceInfoKind serviceType;

    /**
     * Gets the value of the companyCode property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getCompanyCode() {
        return companyCode;
    }

    /**
     * Sets the value of the companyCode property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setCompanyCode(String value) {
        this.companyCode = value;
    }

    /**
     * Gets the value of the serviceCode property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getServiceCode() {
        return serviceCode;
    }

    /**
     * Sets the value of the serviceCode property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setServiceCode(String value) {
        this.serviceCode = value;
    }

    /**
     * Gets the value of the transportModeCode property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getTransportModeCode() {
        return transportModeCode;
    }

    /**
     * Sets the value of the transportModeCode property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setTransportModeCode(String value) {
        this.transportModeCode = value;
    }

    /**
     * Gets the value of the stopList property.
     * 
     * @return
     *     possible object is
     *     {@link ServiceInfoServiceType.StopList }
     *     
     */
    public ServiceInfoServiceType.StopList getStopList() {
        return stopList;
    }

    /**
     * Sets the value of the stopList property.
     * 
     * @param value
     *     allowed object is
     *     {@link ServiceInfoServiceType.StopList }
     *     
     */
    public void setStopList(ServiceInfoServiceType.StopList value) {
        this.stopList = value;
    }

    /**
     * Gets the value of the serviceType property.
     * 
     * @return
     *     possible object is
     *     {@link ServiceInfoKind }
     *     
     */
    public ServiceInfoKind getServiceType() {
        return serviceType;
    }

    /**
     * Sets the value of the serviceType property.
     * 
     * @param value
     *     allowed object is
     *     {@link ServiceInfoKind }
     *     
     */
    public void setServiceType(ServiceInfoKind value) {
        this.serviceType = value;
    }


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
     *         &lt;element name="Stop" type="{http://www.tt-solutions.nl/schemas/NS/RTI/1.1/}ServiceInfoStopType" maxOccurs="unbounded"/>
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
        "stop"
    })
    public static class StopList {

        @XmlElement(name = "Stop", required = true)
        protected List<ServiceInfoStopType> stop;

        /**
         * Gets the value of the stop property.
         * 
         * <p>
         * This accessor method returns a reference to the live list,
         * not a snapshot. Therefore any modification you make to the
         * returned list will be present inside the JAXB object.
         * This is why there is not a <CODE>set</CODE> method for the stop property.
         * 
         * <p>
         * For example, to add a new item, do as follows:
         * <pre>
         *    getStop().add(newItem);
         * </pre>
         * 
         * 
         * <p>
         * Objects of the following type(s) are allowed in the list
         * {@link ServiceInfoStopType }
         * 
         * 
         */
        public List<ServiceInfoStopType> getStop() {
            if (stop == null) {
                stop = new ArrayList<ServiceInfoStopType>();
            }
            return this.stop;
        }

    }

}
