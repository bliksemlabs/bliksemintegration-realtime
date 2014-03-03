
package nl.tt_solutions.schemas.ns.rti._1;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * Description of platform information
 * 
 * <p>Java class for PlatformInfoType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="PlatformInfoType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="CompanyCode" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="ServiceCode" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="StopList">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 &lt;sequence>
 *                   &lt;element name="Stop" type="{http://www.tt-solutions.nl/schemas/NS/RTI/1.1/}PlatformInfoStopType" maxOccurs="unbounded"/>
 *                 &lt;/sequence>
 *               &lt;/restriction>
 *             &lt;/complexContent>
 *           &lt;/complexType>
 *         &lt;/element>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "PlatformInfoType", propOrder = {
    "companyCode",
    "serviceCode",
    "stopList"
})
public class PlatformInfoType {

    @XmlElement(name = "CompanyCode", required = true)
    protected String companyCode;
    @XmlElement(name = "ServiceCode", required = true)
    protected String serviceCode;
    @XmlElement(name = "StopList", required = true)
    protected PlatformInfoType.StopList stopList;

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
     * Gets the value of the stopList property.
     * 
     * @return
     *     possible object is
     *     {@link PlatformInfoType.StopList }
     *     
     */
    public PlatformInfoType.StopList getStopList() {
        return stopList;
    }

    /**
     * Sets the value of the stopList property.
     * 
     * @param value
     *     allowed object is
     *     {@link PlatformInfoType.StopList }
     *     
     */
    public void setStopList(PlatformInfoType.StopList value) {
        this.stopList = value;
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
     *         &lt;element name="Stop" type="{http://www.tt-solutions.nl/schemas/NS/RTI/1.1/}PlatformInfoStopType" maxOccurs="unbounded"/>
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
        protected List<PlatformInfoStopType> stop;

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
         * {@link PlatformInfoStopType }
         * 
         * 
         */
        public List<PlatformInfoStopType> getStop() {
            if (stop == null) {
                stop = new ArrayList<PlatformInfoStopType>();
            }
            return this.stop;
        }

    }

}
