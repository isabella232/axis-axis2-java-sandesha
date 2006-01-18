/*
 * Copyright 1999-2004 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *  
 */

package org.apache.sandesha2.handlers;

import java.util.ArrayList;
import java.util.Iterator;

import javax.xml.namespace.QName;

import org.apache.axis2.AxisFault;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.context.ServiceContext;
import org.apache.axis2.engine.AxisEngine;
import org.apache.axis2.handlers.AbstractHandler;
import org.apache.axis2.soap.SOAPBody;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.sandesha2.RMMsgContext;
import org.apache.sandesha2.Sandesha2Constants;
import org.apache.sandesha2.SandeshaException;
import org.apache.sandesha2.msgprocessors.ApplicationMsgProcessor;
import org.apache.sandesha2.storage.StorageManager;
import org.apache.sandesha2.storage.beanmanagers.SequencePropertyBeanMgr;
import org.apache.sandesha2.storage.beans.SequencePropertyBean;
import org.apache.sandesha2.util.FaultManager;
import org.apache.sandesha2.util.MsgInitializer;
import org.apache.sandesha2.util.SandeshaUtil;
import org.apache.sandesha2.wsrm.Sequence;

/**
 * The Global handler of Sandesha2. This is used to perform things that should be done before
 * diapatching such as duplicate detection.
 * 
 * @author Chamikara Jayalath <chamikaramj@gmail.com>
 */

public class SandeshaGlobalInHandler extends AbstractHandler {

	protected Log log = LogFactory.getLog(SandeshaGlobalInHandler.class
			.getName());

	public void invoke(MessageContext msgContext) throws AxisFault {

		//Quitting the message with minimum processing if not intended for RM.
		boolean isRMGlobalMessage = SandeshaUtil.isRMGlobalMessage(msgContext);
		if (!isRMGlobalMessage) {
			return;
		}

		FaultManager faultManager = new FaultManager();
		RMMsgContext faultMessageContext = faultManager
				.checkForPossibleFaults(msgContext);
		if (faultMessageContext != null) {
			ConfigurationContext configurationContext = msgContext
					.getConfigurationContext();
			AxisEngine engine = new AxisEngine(configurationContext);
			engine.send(faultMessageContext.getMessageContext());
			return;
		}

		RMMsgContext rmMessageContext = MsgInitializer
				.initializeMessage(msgContext);

		ConfigurationContext context = rmMessageContext.getMessageContext()
				.getConfigurationContext();

		ServiceContext serviceContext = msgContext.getServiceContext();
		Object debug = null;

		//Dropping duplicates
		boolean dropped = dropIfDuplicate(rmMessageContext);
		if (dropped) {
			processDroppedMessage(rmMessageContext);
			return;
		}

		//Process if global processing possible. - Currently none
		if (SandeshaUtil.isGloballyProcessableMessageType(rmMessageContext
				.getMessageType())) {
			doGlobalProcessing(rmMessageContext);
		}
	}

	private boolean dropIfDuplicate(RMMsgContext rmMsgContext)
			throws SandeshaException {

		boolean drop = false;

		if (rmMsgContext.getMessageType() == Sandesha2Constants.MessageTypes.APPLICATION) {

			Sequence sequence = (Sequence) rmMsgContext
					.getMessagePart(Sandesha2Constants.MessageParts.SEQUENCE);
			String sequenceId = null;

			if (sequence != null) {
				sequenceId = sequence.getIdentifier().getIdentifier();
			}

			long msgNo = sequence.getMessageNumber().getMessageNumber();

			if (sequenceId != null && msgNo > 0) {
				StorageManager storageManager = SandeshaUtil
						.getSandeshaStorageManager(rmMsgContext
								.getMessageContext().getConfigurationContext());
				SequencePropertyBeanMgr seqPropMgr = storageManager
						.getSequencePropretyBeanMgr();
				SequencePropertyBean receivedMsgsBean = seqPropMgr.retrieve(
						sequenceId,
						Sandesha2Constants.SequenceProperties.COMPLETED_MESSAGES);
				if (receivedMsgsBean != null) {
					String receivedMsgStr = (String) receivedMsgsBean
							.getValue();
					ArrayList msgNoArrList = SandeshaUtil
							.getSplittedMsgNoArraylist(receivedMsgStr);

					Iterator iterator = msgNoArrList.iterator();
					while (iterator.hasNext()) {
						String temp = (String) iterator.next();
						String msgNoStr = new Long(msgNo).toString();
						if (msgNoStr.equals(temp)) {
							drop = true;
						}
					}
				}

				if (drop == false) {
					//Checking for RM specific EMPTY_BODY LASTMESSAGE.
					SOAPBody body = rmMsgContext.getSOAPEnvelope().getBody();
					boolean emptyBody = false;
					if (body.getChildElements().hasNext() == false) {
						emptyBody = true;
					}

					if (emptyBody) {
						if (sequence.getLastMessage() != null) {
							log.info ("Empty Body LastMessage Received");
							drop = true;

							if (receivedMsgsBean == null) {
								receivedMsgsBean = new SequencePropertyBean(
										sequenceId,
										Sandesha2Constants.SequenceProperties.COMPLETED_MESSAGES,
										"");
								seqPropMgr.insert(receivedMsgsBean);
							}

							String receivedMsgStr = (String) receivedMsgsBean
									.getValue();
							if (receivedMsgStr != "" && receivedMsgStr != null)
								receivedMsgStr = receivedMsgStr + ","
										+ Long.toString(msgNo);
							else
								receivedMsgStr = Long.toString(msgNo);

							receivedMsgsBean.setValue(receivedMsgStr);
							seqPropMgr.update(receivedMsgsBean);

							ApplicationMsgProcessor ackProcessor = new ApplicationMsgProcessor();
							ackProcessor.sendAckIfNeeded(rmMsgContext,
									receivedMsgStr);

						}
					}
				}
			}
		}

		if (drop) {
			rmMsgContext.getMessageContext().pause();
			return true;
		}

		return false;
	}

	private void processDroppedMessage(RMMsgContext rmMsgContext)
			throws SandeshaException {
		if (rmMsgContext.getMessageType() == Sandesha2Constants.MessageTypes.APPLICATION) {
			Sequence sequence = (Sequence) rmMsgContext
					.getMessagePart(Sandesha2Constants.MessageParts.SEQUENCE);
			String sequenceId = null;

			if (sequence != null) {
				sequenceId = sequence.getIdentifier().getIdentifier();
			}

			StorageManager storageManager = SandeshaUtil
					.getSandeshaStorageManager(rmMsgContext.getMessageContext()
							.getConfigurationContext());
			SequencePropertyBeanMgr seqPropMgr = storageManager
					.getSequencePropretyBeanMgr();
			SequencePropertyBean receivedMsgsBean = seqPropMgr.retrieve(
					sequenceId, Sandesha2Constants.SequenceProperties.COMPLETED_MESSAGES);
			String receivedMsgStr = (String) receivedMsgsBean.getValue();

			ApplicationMsgProcessor ackProcessor = new ApplicationMsgProcessor();
			//Even though the duplicate message is dropped, hv to send the ack if needed.
			ackProcessor.sendAckIfNeeded(rmMsgContext, receivedMsgStr);

		}
	}

	private void doGlobalProcessing(RMMsgContext rmMsgCtx)
			throws SandeshaException {
		switch (rmMsgCtx.getMessageType()) {
		case Sandesha2Constants.MessageTypes.ACK:
			rmMsgCtx.setRelatesTo(null); 
			//Removing the relatesTo part from ackMessageIf present. Some Frameworks tend to send this.
		}
	}

	public QName getName() {
		return new QName(Sandesha2Constants.GLOBAL_IN_HANDLER_NAME);
	}
}