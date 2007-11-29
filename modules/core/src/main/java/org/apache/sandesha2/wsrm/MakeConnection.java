/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.sandesha2.wsrm;

import java.util.Iterator;

import javax.xml.namespace.QName;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMException;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMNamespace;
import org.apache.axiom.soap.SOAPBody;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axis2.AxisFault;
import org.apache.sandesha2.Sandesha2Constants;
import org.apache.sandesha2.SandeshaException;
import org.apache.sandesha2.i18n.SandeshaMessageHelper;
import org.apache.sandesha2.i18n.SandeshaMessageKeys;

public class MakeConnection implements IOMRMPart {

	private String namespaceValue = null;
	
	Identifier identifier = null;
	
	Address address = null;
	
	QName unexpectedElement = null;
	
	public MakeConnection (String namespaceValue) throws SandeshaException {
		
		if (!isNamespaceSupported(namespaceValue))
			throw new SandeshaException (SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.specDoesNotSupportElement,
					namespaceValue,Sandesha2Constants.WSRM_COMMON.MAKE_CONNECTION));
		this.namespaceValue = namespaceValue;
	}

	public void toSOAPEnvelope(SOAPEnvelope envelope) {
		SOAPBody body = envelope.getBody();
		
		//detach if already exist.
		OMElement elem = body.getFirstChildWithName(new QName(namespaceValue,
				Sandesha2Constants.WSRM_COMMON.MAKE_CONNECTION));
		if (elem!=null)
			elem.detach();
		
		toOMElement(body);
	}

	public Object fromOMElement(OMElement makeConnectionElement) throws OMException, AxisFault {

		Iterator childElements = makeConnectionElement.getChildElements();
		
		OMElement identifierElement = null;
		OMElement addressElement = null;
		
		while (childElements.hasNext()) {
			OMElement element = (OMElement)childElements.next();
			if (Sandesha2Constants.SPEC_2007_02.QNames.Identifier.getLocalPart().equals(element.getLocalName()))
				identifierElement = element;
			else if (Sandesha2Constants.WSA.ADDRESS.equals(element.getLocalName()))
				addressElement = element;
			else 
				unexpectedElement = element.getQName();
			
		}

		if (identifierElement!=null) {
			identifier = new Identifier (identifierElement.getNamespace().getNamespaceURI());
			identifier.fromOMElement(identifierElement);
		}
		
		if (addressElement!=null) {
			address = new Address (namespaceValue);
			address.fromOMElement(makeConnectionElement);
		}
		
		return this;
	}

	public String getNamespaceValue() {
		return namespaceValue;
	}

	public boolean isNamespaceSupported(String namespaceName) {
		if (Sandesha2Constants.SPEC_2007_02.MC_NS_URI.equals(namespaceName))
			return true;
		
		return false;
	}

	public OMElement toOMElement(OMElement body) throws OMException {

		if (body == null || !(body instanceof SOAPBody)) {
			String message = "MakeConnection element can only be added to a SOAP Body ";
			throw new OMException(
					SandeshaMessageHelper.getMessage(message));
		}

		OMFactory factory = body.getOMFactory();
		OMNamespace rmNamespace = factory.createOMNamespace(namespaceValue,Sandesha2Constants.WSRM_COMMON.NS_PREFIX_MC);
		OMElement makeConnectionElement = factory.createOMElement(Sandesha2Constants.WSRM_COMMON.MAKE_CONNECTION,rmNamespace);
		
		if (identifier!=null)
			identifier.toOMElement(makeConnectionElement, Sandesha2Constants.SPEC_2007_02.OM_NS_URI);
		if (address!=null)
			address.toOMElement(makeConnectionElement);

		// Added to test Invalid MakeConnection messages
		if (unexpectedElement != null)
		{
			OMElement unexElement = factory.createOMElement(unexpectedElement);

			makeConnectionElement.addChild(unexElement);
		}
			
		body.addChild(makeConnectionElement);
		
		return body;
	}

	public Address getAddress() {
		return address;
	}

	public void setAddress(Address address) {
		this.address = address;
	}

	public Identifier getIdentifier() {
		return identifier;
	}

	public void setIdentifier(Identifier identifier) {
		this.identifier = identifier;
	}

	public void setUnexpectedElement(QName unexpectedElement) {
		  this.unexpectedElement = unexpectedElement;	
		}

	public QName getUnexpectedElement() {
		return unexpectedElement;
	}	
}
