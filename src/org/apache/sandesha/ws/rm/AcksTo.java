package org.apache.sandesha.ws.rm;

import org.apache.axis.message.MessageElement;
import org.apache.axis.message.addressing.Address;
import org.apache.sandesha.Constants;

import javax.xml.soap.SOAPException;
import java.util.Iterator;

public class AcksTo implements IRmElement {
    private Address address;
    private MessageElement acksToElement;

    /**
     * Constructor Nack
     */
    public AcksTo() {
    }

    public AcksTo(Address address) {
        acksToElement = new MessageElement();
        acksToElement.setName(Constants.WSRM.NS_PREFIX_RM + Constants.COLON + Constants.WSRM.ACKS_TO);
        this.address = address;
    }

    /**
     * Method fromSOAPEnvelope
     *
     * @param element
     * @return Nack
     */
    public AcksTo fromSOAPEnvelope(MessageElement element) throws Exception {
        Iterator iterator = element.getChildElements();
        MessageElement childElement;
        while (iterator.hasNext()) {
            childElement = (MessageElement) iterator.next();
            if (childElement.getName().equals(org.apache.axis.message.addressing.Constants.NS_PREFIX_ADDRESSING + Constants.COLON + org.apache.axis.message.addressing.Constants.ADDRESS)) {
                String uri = childElement.getFirstChild().getFirstChild().toString();
                address = new Address(uri);
            }
            if (childElement.getName().equals(org.apache.axis.message.addressing.Constants.ADDRESS)) {
                String uri = childElement.getFirstChild().getNodeValue();
                address = new Address(uri);
            }
        }
        return this;
    }

    /**
     * Method toSOAPEnvelope
     *
     * @param msgElement
     * @return MessageElement
     * @throws SOAPException
     */
    public MessageElement toSOAPEnvelope(MessageElement msgElement) throws SOAPException {
        MessageElement messageElement = new MessageElement("", Constants.WSRM.NS_PREFIX_RM, Constants.WSRM.NS_URI_RM);
        messageElement.setName("AcksTo");
        address.append(messageElement);
        msgElement.addChildElement(messageElement);
        return msgElement;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.sandesha.ws.rm.IRmElement#addChildElement(org.apache.axis.message.MessageElement)
     */
    public MessageElement getSoapElement() throws SOAPException {
        address.append(acksToElement);
        return acksToElement;
    }

    /**
     * Method addChildElement
     *
     * @param element
     */
    public void addChildElement(MessageElement element) {
        // TODO no child elements ?
    }

    /**
     * get the address
     * @return
     */
    public Address getAddress() {
        return address;
    }

    /**
     * set the address
     * @param address
     */
    public void setAddress(Address address) {
        this.address = address;
    }

}
