
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
 *         &lt;element name="PlatformInfoList" type="{http://www.tt-solutions.nl/schemas/NS/RTI/1.1/}PlatFormInfoList"/>
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
    "platformInfoList"
})
@XmlRootElement(name = "PutPlatformInfoIn")
public class PutPlatformInfoIn {

    @XmlElement(name = "PlatformInfoList", required = true)
    protected PlatFormInfoList platformInfoList;

    /**
     * Gets the value of the platformInfoList property.
     * 
     * @return
     *     possible object is
     *     {@link PlatFormInfoList }
     *     
     */
    public PlatFormInfoList getPlatformInfoList() {
        return platformInfoList;
    }

    /**
     * Sets the value of the platformInfoList property.
     * 
     * @param value
     *     allowed object is
     *     {@link PlatFormInfoList }
     *     
     */
    public void setPlatformInfoList(PlatFormInfoList value) {
        this.platformInfoList = value;
    }

}
