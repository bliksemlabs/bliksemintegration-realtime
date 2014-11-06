
package nl.tt_solutions.schemas.ns.rti._1;

import org.joda.time.DateTime;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;


/**
 * Description of the stop information
 * 
 * <p>Java class for PlatformInfoStopType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="PlatformInfoStopType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="StopCode" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="ArrivalPlatform" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="DeparturePlatform" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="Departure" type="{http://www.w3.org/2001/XMLSchema}dateTime" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "PlatformInfoStopType", propOrder = {
    "stopCode",
    "arrivalPlatform",
    "departurePlatform",
    "departure"
})
public class PlatformInfoStopType {

    @XmlElement(name = "StopCode", required = true)
    protected String stopCode;
    @XmlElement(name = "ArrivalPlatform")
    protected String arrivalPlatform;
    @XmlElement(name = "DeparturePlatform")
    protected String departurePlatform;
    @XmlElement(name = "Departure")
    @XmlSchemaType(name = "dateTime")
    protected DateTime departure;

    /**
     * Gets the value of the stopCode property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getStopCode() {
        return stopCode;
    }

    /**
     * Sets the value of the stopCode property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setStopCode(String value) {
        this.stopCode = value;
    }

    /**
     * Gets the value of the arrivalPlatform property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getArrivalPlatform() {
        return arrivalPlatform;
    }

    /**
     * Sets the value of the arrivalPlatform property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setArrivalPlatform(String value) {
        this.arrivalPlatform = value;
    }

    /**
     * Gets the value of the departurePlatform property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDeparturePlatform() {
        return departurePlatform;
    }

    /**
     * Sets the value of the departurePlatform property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDeparturePlatform(String value) {
        this.departurePlatform = value;
    }

    /**
     * Gets the value of the departure property.
     * 
     * @return
     *     possible object is
     *     {@link DateTime }
     *     
     */
    public DateTime getDeparture() {
        return departure;
    }

    /**
     * Sets the value of the departure property.
     * 
     * @param value
     *     allowed object is
     *     {@link DateTime }
     *     
     */
    public void setDeparture(DateTime value) {
        this.departure = value;
    }

}
