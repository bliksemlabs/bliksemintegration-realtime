
package nl.tt_solutions.schemas.ns.rti._1;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;

import lombok.ToString;


/**
 * Description of the stop information
 * 
 * <p>Java class for ServiceInfoStopType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="ServiceInfoStopType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="StopCode" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="StopServiceCode" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="Arrival" type="{http://www.w3.org/2001/XMLSchema}dateTime" minOccurs="0"/>
 *         &lt;element name="ArrivalTimeDelay" type="{http://www.w3.org/2001/XMLSchema}duration" minOccurs="0"/>
 *         &lt;element name="Departure" type="{http://www.w3.org/2001/XMLSchema}dateTime" minOccurs="0"/>
 *         &lt;element name="DepartureTimeDelay" type="{http://www.w3.org/2001/XMLSchema}duration" minOccurs="0"/>
 *         &lt;element name="ArrivalPlatform" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="ActualArrivalPlatform" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="DeparturePlatform" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="ActualDeparturePlatform" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="StopType" type="{http://www.tt-solutions.nl/schemas/NS/RTI/1.1/}ServiceInfoStopKind" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ServiceInfoStopType", propOrder = {
    "stopCode",
    "stopServiceCode",
    "arrival",
    "arrivalTimeDelay",
    "departure",
    "departureTimeDelay",
    "arrivalPlatform",
    "actualArrivalPlatform",
    "departurePlatform",
    "actualDeparturePlatform"
})
@ToString
public class ServiceInfoStopType {

    @XmlElement(name = "StopCode", required = true)
    protected String stopCode;
    @XmlElement(name = "StopServiceCode", required = true)
    protected String stopServiceCode;
    @XmlElement(name = "Arrival")
    @XmlSchemaType(name = "dateTime")
    protected XMLGregorianCalendar arrival;
    @XmlElement(name = "ArrivalTimeDelay")
    protected Duration arrivalTimeDelay;
    @XmlElement(name = "Departure")
    @XmlSchemaType(name = "dateTime")
    protected XMLGregorianCalendar departure;
    @XmlElement(name = "DepartureTimeDelay")
    protected Duration departureTimeDelay;
    @XmlElement(name = "ArrivalPlatform")
    protected String arrivalPlatform;
    @XmlElement(name = "ActualArrivalPlatform")
    protected String actualArrivalPlatform;
    @XmlElement(name = "DeparturePlatform")
    protected String departurePlatform;
    @XmlElement(name = "ActualDeparturePlatform")
    protected String actualDeparturePlatform;
    @XmlAttribute(name = "StopType")
    protected ServiceInfoStopKind stopType;

    /**
     * Gets the value of the stopCode property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getStopCode() {
        return stopCode == null ? null : stopCode.toLowerCase();
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
     * Gets the value of the stopServiceCode property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getStopServiceCode() {
        return stopServiceCode;
    }

    /**
     * Sets the value of the stopServiceCode property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setStopServiceCode(String value) {
        this.stopServiceCode = value;
    }

    /**
     * Gets the value of the arrival property.
     * 
     * @return
     *     possible object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public XMLGregorianCalendar getArrival() {
        return arrival;
    }

    /**
     * Sets the value of the arrival property.
     * 
     * @param value
     *     allowed object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public void setArrival(XMLGregorianCalendar value) {
        this.arrival = value;
    }

    /**
     * Gets the value of the arrivalTimeDelay property.
     * 
     * @return
     *     possible object is
     *     {@link Duration }
     *     
     */
    public Duration getArrivalTimeDelay() {
        return arrivalTimeDelay;
    }

    /**
     * Sets the value of the arrivalTimeDelay property.
     * 
     * @param value
     *     allowed object is
     *     {@link Duration }
     *     
     */
    public void setArrivalTimeDelay(Duration value) {
        this.arrivalTimeDelay = value;
    }

    /**
     * Gets the value of the departure property.
     * 
     * @return
     *     possible object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public XMLGregorianCalendar getDeparture() {
        return departure;
    }

    /**
     * Sets the value of the departure property.
     * 
     * @param value
     *     allowed object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public void setDeparture(XMLGregorianCalendar value) {
        this.departure = value;
    }

    /**
     * Gets the value of the departureTimeDelay property.
     * 
     * @return
     *     possible object is
     *     {@link Duration }
     *     
     */
    public Duration getDepartureTimeDelay() {
        return departureTimeDelay;
    }

    /**
     * Sets the value of the departureTimeDelay property.
     * 
     * @param value
     *     allowed object is
     *     {@link Duration }
     *     
     */
    public void setDepartureTimeDelay(Duration value) {
        this.departureTimeDelay = value;
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
     * Gets the value of the actualArrivalPlatform property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getActualArrivalPlatform() {
        return actualArrivalPlatform;
    }

    /**
     * Sets the value of the actualArrivalPlatform property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setActualArrivalPlatform(String value) {
        this.actualArrivalPlatform = value;
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
     * Gets the value of the actualDeparturePlatform property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getActualDeparturePlatform() {
        return actualDeparturePlatform;
    }

    /**
     * Sets the value of the actualDeparturePlatform property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setActualDeparturePlatform(String value) {
        this.actualDeparturePlatform = value;
    }

    /**
     * Gets the value of the stopType property.
     * 
     * @return
     *     possible object is
     *     {@link ServiceInfoStopKind }
     *     
     */
    public ServiceInfoStopKind getStopType() {
        return stopType;
    }

    /**
     * Sets the value of the stopType property.
     * 
     * @param value
     *     allowed object is
     *     {@link ServiceInfoStopKind }
     *     
     */
    public void setStopType(ServiceInfoStopKind value) {
        this.stopType = value;
    }

}
