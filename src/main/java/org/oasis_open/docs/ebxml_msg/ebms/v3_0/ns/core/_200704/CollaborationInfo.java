
package org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704;

import javax.xml.bind.annotation.*;
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.io.Serializable;


/**
 * <p>Java class for CollaborationInfo complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="CollaborationInfo"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="AgreementRef" type="{http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/}AgreementRef" minOccurs="0"/&gt;
 *         &lt;element name="Service" type="{http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/}Service"/&gt;
 *         &lt;element name="Action" type="{http://www.w3.org/2001/XMLSchema}token"/&gt;
 *         &lt;element name="ConversationId" type="{http://www.w3.org/2001/XMLSchema}token"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "CollaborationInfo", propOrder = {
    "agreementRef",
    "service",
    "action",
    "conversationId"
})
public class CollaborationInfo implements Serializable {

    @XmlElement(name = "AgreementRef")
    protected AgreementRef agreementRef;
    @XmlElement(name = "Service", required = true)
    protected Service service;
    @XmlElement(name = "Action", required = true)
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    @XmlSchemaType(name = "token")
    protected String action;
    @XmlElement(name = "ConversationId", required = true)
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    @XmlSchemaType(name = "token")
    protected String conversationId;

    /**
     * Gets the value of the agreementRef property.
     * 
     * @return
     *     possible object is
     *     {@link org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.AgreementRef }
     *
     */
    public AgreementRef getAgreementRef() {
        return agreementRef;
    }

    /**
     * Sets the value of the agreementRef property.
     *
     * @param value
     *     allowed object is
     *     {@link org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.AgreementRef }
     *
     */
    public void setAgreementRef(AgreementRef value) {
        this.agreementRef = value;
    }

    /**
     * Gets the value of the service property.
     *
     * @return
     *     possible object is
     *     {@link org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.Service }
     *
     */
    public Service getService() {
        return service;
    }

    /**
     * Sets the value of the service property.
     *
     * @param value
     *     allowed object is
     *     {@link org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.Service }
     *     
     */
    public void setService(Service value) {
        this.service = value;
    }

    /**
     * Gets the value of the action property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getAction() {
        return action;
    }

    /**
     * Sets the value of the action property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setAction(String value) {
        this.action = value;
    }

    /**
     * Gets the value of the conversationId property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getConversationId() {
        return conversationId;
    }

    /**
     * Sets the value of the conversationId property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setConversationId(String value) {
        this.conversationId = value;
    }

}
