/*
 * Copyright  1999-2004 The Apache Software Foundation.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.apache.sandesha2.wsrm;

import javax.xml.namespace.QName;

import org.apache.axis2.om.OMElement;
import org.apache.axis2.om.OMException;
import org.apache.axis2.om.OMNamespace;
import org.apache.axis2.soap.SOAPFactory;
import org.apache.sandesha2.Sandesha2Constants;

/**
 * @author Chamikara Jayalath <chamikaramj@gmail.com>
 * @author Sanka Samaranayaka <ssanka@gmail.com>
 * @author Saminda Abeyruwan  <saminda@opensource.lk>
 */

public class LastMessage implements IOMRMElement {

	private OMElement lastMessageElement;

	OMNamespace rmNamespace = null;
	
	SOAPFactory factory;

	public LastMessage(SOAPFactory factory) {
		this.factory = factory;
		rmNamespace = factory.createOMNamespace(
				Sandesha2Constants.WSRM.NS_URI_RM, Sandesha2Constants.WSRM.NS_PREFIX_RM);
		lastMessageElement = factory.createOMElement(
				Sandesha2Constants.WSRM.LAST_MSG, rmNamespace);
	}

	public OMElement getOMElement() throws OMException {
		return lastMessageElement;
	}

	public Object fromOMElement(OMElement element) throws OMException {
		OMElement lastMessagePart = element.getFirstChildWithName(new QName(
				Sandesha2Constants.WSRM.NS_URI_RM, Sandesha2Constants.WSRM.LAST_MSG));
		if (lastMessagePart == null)
			throw new OMException(
					"The passed element does not contain a Last Message part");

		lastMessageElement = factory.createOMElement(
				Sandesha2Constants.WSRM.LAST_MSG, rmNamespace);

		return this;
	}

	public OMElement toOMElement(OMElement sequenceElement) throws OMException {
		//soapheaderblock element will be given
		if (lastMessageElement == null)
			throw new OMException("Cant set last message element. It is null");

		sequenceElement.addChild(lastMessageElement);

		lastMessageElement = factory.createOMElement(
				Sandesha2Constants.WSRM.LAST_MSG, rmNamespace);

		return sequenceElement;
	}

	public void setLastMessageElement(OMElement lastMsgElement) {
		lastMessageElement = lastMsgElement;
	}

	public OMElement getLastMessageElement() {
		return lastMessageElement;
	}

	public boolean isPresent() {
		return (lastMessageElement != null) ? true : false;
	}
}