
package nl.tt_solutions.schemas.ns.rti._1;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * List of services with changed platform information
 * 
 * <p>Java class for PlatFormInfoList complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="PlatFormInfoList">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="PlatformInfo" type="{http://www.tt-solutions.nl/schemas/NS/RTI/1.1/}PlatformInfoType" maxOccurs="unbounded"/>
 *       &lt;/sequence>
 *       &lt;attribute name="Initial" type="{http://www.w3.org/2001/XMLSchema}boolean" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "PlatFormInfoList", propOrder = {
    "platformInfo"
})
public class PlatFormInfoList {

    @XmlElement(name = "PlatformInfo", required = true)
    protected List<PlatformInfoType> platformInfo;
    @XmlAttribute(name = "Initial")
    protected Boolean initial;

    /**
     * Gets the value of the platformInfo property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the platformInfo property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getPlatformInfo().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link PlatformInfoType }
     * 
     * 
     */
    public List<PlatformInfoType> getPlatformInfo() {
        if (platformInfo == null) {
            platformInfo = new ArrayList<PlatformInfoType>();
        }
        return this.platformInfo;
    }

    /**
     * Gets the value of the initial property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isInitial() {
        return initial;
    }

    /**
     * Sets the value of the initial property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setInitial(Boolean value) {
        this.initial = value;
    }

}
