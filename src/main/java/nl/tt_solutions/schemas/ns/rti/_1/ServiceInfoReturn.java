
package nl.tt_solutions.schemas.ns.rti._1;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * Return value
 * 
 * <p>Java class for ServiceInfoReturn complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="ServiceInfoReturn">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="Code" type="{http://www.tt-solutions.nl/schemas/NS/RTI/1.1/}ReturnValueKind"/>
 *         &lt;element name="Description" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ServiceInfoReturn", propOrder = {
    "code",
    "description"
})
public class ServiceInfoReturn {

    @XmlElement(name = "Code", required = true)
    protected ReturnValueKind code;
    @XmlElement(name = "Description", required = true)
    protected String description;

    /**
     * Gets the value of the code property.
     * 
     * @return
     *     possible object is
     *     {@link ReturnValueKind }
     *     
     */
    public ReturnValueKind getCode() {
        return code;
    }

    /**
     * Sets the value of the code property.
     * 
     * @param value
     *     allowed object is
     *     {@link ReturnValueKind }
     *     
     */
    public void setCode(ReturnValueKind value) {
        this.code = value;
    }

    /**
     * Gets the value of the description property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the value of the description property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDescription(String value) {
        this.description = value;
    }

}
