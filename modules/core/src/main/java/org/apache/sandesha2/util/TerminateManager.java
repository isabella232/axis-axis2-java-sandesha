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

package org.apache.sandesha2.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.sandesha2.RMMsgContext;
import org.apache.sandesha2.Sandesha2Constants;
import org.apache.sandesha2.SandeshaException;
import org.apache.sandesha2.client.SandeshaClient;
import org.apache.sandesha2.client.SandeshaClientConstants;
import org.apache.sandesha2.i18n.SandeshaMessageHelper;
import org.apache.sandesha2.i18n.SandeshaMessageKeys;
import org.apache.sandesha2.storage.SandeshaStorageException;
import org.apache.sandesha2.storage.StorageManager;
import org.apache.sandesha2.storage.beanmanagers.InvokerBeanMgr;
import org.apache.sandesha2.storage.beanmanagers.RMDBeanMgr;
import org.apache.sandesha2.storage.beanmanagers.SenderBeanMgr;
import org.apache.sandesha2.storage.beans.RMSBean;
import org.apache.sandesha2.storage.beans.InvokerBean;
import org.apache.sandesha2.storage.beans.RMDBean;
import org.apache.sandesha2.storage.beans.SenderBean;

import com.ibm.xslt4j.bcel.generic.RETURN;

/**
 * Contains logic to remove all the storad data of a sequence. Methods of this
 * are called by sending side and the receiving side when appropriate
 */

public class TerminateManager {

	private static Log log = LogFactory.getLog(TerminateManager.class);

	private static String CLEANED_ON_TERMINATE_MSG = "CleanedOnTerminateMsg";

	private static String CLEANED_AFTER_INVOCATION = "CleanedAfterInvocation";

	public static HashMap receivingSideCleanMap = new HashMap();

	public static void checkAndTerminate(ConfigurationContext configurationContext, StorageManager storageManager, RMSBean rmsBean)
	throws SandeshaStorageException, AxisFault {
		if(log.isDebugEnabled()) log.debug("Enter: TerminateManager::checkAndTerminate " +rmsBean);

		long lastOutMessage = rmsBean.getLastOutMessage ();

		if (lastOutMessage > 0 && !rmsBean.isTerminateAdded()) {
			
			boolean complete = AcknowledgementManager.verifySequenceCompletion(rmsBean.getClientCompletedMessages(), lastOutMessage);
			
			//If this is RM 1.1 and RMAnonURI scenario, dont do the termination unless the response side createSequence has been
			//received (RMDBean has been created) through polling, in this case termination will happen in the create sequence response processor.
			String rmVersion = rmsBean.getRMVersion();
			EndpointReference replyTo = rmsBean.getReplyToEndpointReference();

			if (complete &&
					Sandesha2Constants.SPEC_VERSIONS.v1_1.equals(rmVersion) && replyTo!=null && SandeshaUtil.isWSRMAnonymous(replyTo.getAddress())) {
				RMDBean findBean = new RMDBean ();
				findBean.setPollingMode(true);
				findBean.setToEndpointReference(replyTo);

				RMDBeanMgr rmdBeanMgr = storageManager.getRMDBeanMgr();
				List beans = rmdBeanMgr.find(findBean);
				if(beans.isEmpty()) {
					rmsBean.setTerminationPauserForCS(true);
					storageManager.getRMSBeanMgr().update(rmsBean);
					complete = false;
				}
			}
			
			// If we are doing sync 2-way over WSRM 1.0 then we may need to keep sending messages,
			// so check to see if all the senders have been removed
			if (complete &&
					Sandesha2Constants.SPEC_VERSIONS.v1_0.equals(rmVersion) && (replyTo==null || replyTo.hasAnonymousAddress())) {
				SenderBean matcher = new SenderBean();
				matcher.setMessageType(Sandesha2Constants.MessageTypes.APPLICATION);
				matcher.setSequenceID(rmsBean.getSequenceID());
				
				List matches = storageManager.getSenderBeanMgr().find(matcher);
				if(!matches.isEmpty()) complete = false;
			}
			
			if (complete) {
				
				String referenceMsgKey = rmsBean.getReferenceMessageStoreKey();
				if (referenceMsgKey==null) {
					String message = SandeshaMessageHelper.getMessage(SandeshaMessageKeys.referenceMessageNotSetForSequence,rmsBean.getSequenceID());
					throw new SandeshaException (message);
				}
				
				MessageContext referenceMessage = storageManager.retrieveMessageContext(referenceMsgKey, configurationContext);
				
				if (referenceMessage==null) {
					String message = SandeshaMessageHelper.getMessage(SandeshaMessageKeys.referencedMessageNotFound, rmsBean.getSequenceID());
					throw new SandeshaException (message);
				}
				
				RMMsgContext referenceRMMsg = MsgInitializer.initializeMessage(referenceMessage);
				addTerminateSequenceMessage(referenceRMMsg, rmsBean.getInternalSequenceID(), rmsBean.getSequenceID(), storageManager);
			}
			
		}

		if(log.isDebugEnabled()) log.debug("Exit: TerminateManager::checkAndTerminate");
	}
	
	
	/**
	 * Called by the receiving side to remove data related to a sequence. e.g.
	 * After sending the TerminateSequence message. Calling this methods will
	 * complete all the data if InOrder invocation is not sequired.
	 * 
	 * @param configContext
	 * @param sequenceID
	 * @throws SandeshaException
	 */
	public static void cleanReceivingSideOnTerminateMessage(ConfigurationContext configContext, String sequenceId,
			StorageManager storageManager) throws SandeshaException {

		// clean senderMap

		//removing any un-sent ack messages.
		SenderBean findAckBean = new SenderBean ();
		findAckBean.setSequenceID(sequenceId);
		findAckBean.setMessageType(Sandesha2Constants.MessageTypes.ACK);
		
		SenderBeanMgr senderBeanMgr = storageManager.getSenderBeanMgr();
		Iterator ackBeans = senderBeanMgr.find(findAckBean).iterator();
		while (ackBeans.hasNext()) {
			SenderBean ackBean = (SenderBean) ackBeans.next();
			senderBeanMgr.delete(ackBean.getMessageID());
			
			storageManager.removeMessageContext(ackBean.getMessageContextRefKey());
		}
		
		// Currently in-order invocation is done for default values.
		boolean inOrderInvocation = SandeshaUtil.getDefaultPropertyBean(configContext.getAxisConfiguration())
				.isInOrder();

		if (!inOrderInvocation) {
			// there is no invoking by Sandesha2. So clean invocations storages.
			
			receivingSideCleanMap.put(sequenceId, CLEANED_ON_TERMINATE_MSG);
			cleanReceivingSideAfterInvocation(sequenceId, storageManager);
		} else {

			String cleanStatus = (String) receivingSideCleanMap.get(sequenceId);
			if (cleanStatus != null
					&& CLEANED_AFTER_INVOCATION.equals(cleanStatus))
				// Remove the sequence from the map
				receivingSideCleanMap.remove(sequenceId);
				//completeTerminationOfReceivingSide(configContext,
				//		sequenceId, storageManager);
			else
				receivingSideCleanMap.put(sequenceId, CLEANED_ON_TERMINATE_MSG);
		}
	}

	/**
	 * When InOrder invocation is anabled this had to be called to clean the
	 * data left by the above method. This had to be called after the Invocation
	 * of the Last Message.
	 * 
	 * @param sequenceID
	 * @throws SandeshaException
	 */
	public static void cleanReceivingSideAfterInvocation(String sequenceId,
			StorageManager storageManager) throws SandeshaException {
		if(log.isDebugEnabled()) log.debug("Enter: TerminateManager::cleanReceivingSideAfterInvocation " +sequenceId);
		
		InvokerBeanMgr invokerBeanMgr = storageManager.getInvokerBeanMgr();

		// removing InvokerBean entries
		InvokerBean invokerFindBean = new InvokerBean();
		invokerFindBean.setSequenceID(sequenceId);
		Collection collection = invokerBeanMgr.find(invokerFindBean);
		Iterator iterator = collection.iterator();
		while (iterator.hasNext()) {
			InvokerBean invokerBean = (InvokerBean) iterator.next();
			String messageStoreKey = invokerBean.getMessageContextRefKey();
			invokerBeanMgr.delete(messageStoreKey);

			// removing the respective message context from the message store.
			storageManager.removeMessageContext(messageStoreKey);
		}

		String cleanStatus = (String) receivingSideCleanMap.get(sequenceId);
		if (cleanStatus != null && CLEANED_ON_TERMINATE_MSG.equals(cleanStatus))
			// Remove the sequence id from the map
			receivingSideCleanMap.remove(sequenceId);
			//completeTerminationOfReceivingSide(configContext, sequenceId, storageManager);
		else 
			receivingSideCleanMap.put(sequenceId, CLEANED_AFTER_INVOCATION);		
		
		if(log.isDebugEnabled()) log.debug("Exit: TerminateManager::cleanReceivingSideAfterInvocation");
	}

	/**
	 * This is called by the sending side to clean data related to a sequence.
	 * e.g. after sending the TerminateSequence message.
	 * 
	 * @param configContext
	 * @param sequenceID
	 * @throws SandeshaException
	 * 
	 * @return true if the reallocation happened sucessfully
	 */
	public static boolean terminateSendingSide(RMSBean rmsBean, 
			StorageManager storageManager, boolean reallocate) throws SandeshaException {

		// Indicate that the sequence is terminated
		rmsBean.setTerminated(true);
		rmsBean.setTerminateAdded(true);
		storageManager.getRMSBeanMgr().update(rmsBean);
		
		return cleanSendingSideData (rmsBean.getInternalSequenceID(), storageManager, rmsBean, reallocate);
	}

	public static void timeOutSendingSideSequence(String internalSequenceId,
			StorageManager storageManager) throws SandeshaException {

		RMSBean rmsBean = SandeshaUtil.getRMSBeanFromInternalSequenceId(storageManager, internalSequenceId);
		rmsBean.setTimedOut(true);
		rmsBean.setLastActivatedTime(System.currentTimeMillis());
		storageManager.getRMSBeanMgr().update(rmsBean);

		cleanSendingSideData(internalSequenceId, storageManager, rmsBean, false);
	}

	private static boolean cleanSendingSideData(String internalSequenceId, StorageManager storageManager, 
			RMSBean rmsBean, boolean reallocateIfPossible) throws SandeshaException {

		if(log.isDebugEnabled())
			log.debug("Enter: TerminateManager::cleanSendingSideData " + internalSequenceId + ", " + reallocateIfPossible);
		
		boolean reallocatedOK = false;
		SenderBeanMgr retransmitterBeanMgr = storageManager.getSenderBeanMgr();

		// removing retransmitterMgr entries and corresponding message contexts.
		Collection collection = retransmitterBeanMgr.find(internalSequenceId);
		Iterator iterator = collection.iterator();
		List msgsToReallocate = null;
		if(reallocateIfPossible){
			msgsToReallocate = new LinkedList();
		}
		Range[] ranges = rmsBean.getClientCompletedMessages().getRanges();
		long lastAckedMsg = -1;
		
		if(ranges.length==1){
			//a single contiguous acked range
			lastAckedMsg = ranges[0].upperValue;
		}
		else{
			//cannot reallocate as there are gaps
			reallocateIfPossible=false;
			if(log.isDebugEnabled())
				log.debug("cannot reallocate sequence as there are gaps");
		}
		
		while (iterator.hasNext()) {
			SenderBean retransmitterBean = (SenderBean) iterator.next();
			if(retransmitterBean.getMessageType()!=Sandesha2Constants.MessageTypes.TERMINATE_SEQ || rmsBean.isTerminated()){
				//remove all but terminate sequence messages
				String messageStoreKey = retransmitterBean.getMessageContextRefKey();
				if(reallocateIfPossible
					&& retransmitterBean.getMessageType()!=Sandesha2Constants.MessageTypes.APPLICATION
					&& retransmitterBean.getMessageNumber()==lastAckedMsg+1){
					
					//try to reallocate application msgs
					msgsToReallocate.add(storageManager.retrieveMessageContext(messageStoreKey, storageManager.getContext()));
					lastAckedMsg++;
				}
				retransmitterBeanMgr.delete(retransmitterBean.getMessageID());
				storageManager.removeMessageContext(messageStoreKey);				
			}
		}
		
		if(reallocateIfPossible && msgsToReallocate.size()>0){
			try{
			      SandeshaUtil.reallocateMessagesToNewSequence(storageManager, rmsBean, msgsToReallocate);	
			      reallocatedOK = true;
			}
			catch(Exception e){
				//want that the reallocation failed
				if(log.isDebugEnabled())
					log.warn(SandeshaMessageHelper.getMessage(SandeshaMessageKeys.reallocationFailed, rmsBean.getSequenceID(), e.toString()));				
			}			
		}
		
		if(log.isDebugEnabled())
			log.debug("Exit: TerminateManager::cleanSendingSideData " + reallocatedOK);
		return reallocatedOK;
	}

	public static void addTerminateSequenceMessage(RMMsgContext referenceMessage, String internalSequenceID, String outSequenceId, StorageManager storageManager) throws AxisFault {
	
		if(log.isDebugEnabled())
			log.debug("Enter: TerminateManager::addTerminateSequenceMessage " + outSequenceId + ", " + internalSequenceID);

		RMSBean rmsBean = SandeshaUtil.getRMSBeanFromInternalSequenceId(storageManager, internalSequenceID);

		if (rmsBean.isTerminateAdded()) {
			if(log.isDebugEnabled())
				log.debug("Exit: TerminateManager::addTerminateSequenceMessage - terminate was added previously.");
			return;
		}

		RMMsgContext terminateRMMessage = RMMsgCreator.createTerminateSequenceMessage(referenceMessage, rmsBean, storageManager);
		terminateRMMessage.setFlow(MessageContext.OUT_FLOW);
		terminateRMMessage.setProperty(Sandesha2Constants.APPLICATION_PROCESSING_DONE, "true");

		
		//setting the To EPR.
		//First try to get it from an Endpoint property.
		//If not get it from the To property.
		
		EndpointReference toEPR = null;
		
		if (rmsBean.getOfferedEndPoint() != null)
			toEPR = new EndpointReference (rmsBean.getOfferedEndPoint());
		
		if (toEPR==null) {

			if (rmsBean.getToEndpointReference()!=null) {
				toEPR = rmsBean.getToEndpointReference();
				if (toEPR == null) {
					String message = SandeshaMessageHelper.getMessage(SandeshaMessageKeys.toEPRNotValid, null);
					throw new SandeshaException(message);
				}
			}
		}

		if (toEPR!=null)
			terminateRMMessage.setTo(toEPR);
		
		if (rmsBean.getReplyToEndpointReference()!=null) {
			terminateRMMessage.setReplyTo(rmsBean.getReplyToEndpointReference());
		}
		
		String rmVersion = rmsBean.getRMVersion();
		terminateRMMessage.setWSAAction(SpecSpecificConstants.getTerminateSequenceAction(rmVersion));
		terminateRMMessage.setSOAPAction(SpecSpecificConstants.getTerminateSequenceSOAPAction(rmVersion));

		if (rmsBean.getTransportTo() != null) {
			terminateRMMessage.setProperty(Constants.Configuration.TRANSPORT_URL, rmsBean.getTransportTo());
		}

		terminateRMMessage.addSOAPEnvelope();

		String key = SandeshaUtil.getUUID();

		SenderBean terminateBean = new SenderBean();
		terminateBean.setInternalSequenceID(internalSequenceID);
		terminateBean.setSequenceID(outSequenceId);
		terminateBean.setMessageContextRefKey(key);
		terminateBean.setMessageType(Sandesha2Constants.MessageTypes.TERMINATE_SEQ);

		// Set a retransmitter lastSentTime so that terminate will be send with
		// some delay.
		// Otherwise this get send before return of the current request (ack).
		// TODO: refine the terminate delay.
		terminateBean.setTimeToSend(System.currentTimeMillis() + Sandesha2Constants.TERMINATE_DELAY);

		terminateBean.setMessageID(terminateRMMessage.getMessageId());

		// this will be set to true at the sender.
		terminateBean.setSend(true);

		terminateRMMessage.getMessageContext().setProperty(Sandesha2Constants.QUALIFIED_FOR_SENDING,
				Sandesha2Constants.VALUE_FALSE);

		terminateBean.setReSend(false);
		
		terminateBean.setSequenceID(outSequenceId);
		
		terminateBean.setMessageType(Sandesha2Constants.MessageTypes.TERMINATE_SEQ);
		terminateBean.setInternalSequenceID(internalSequenceID);
		
		
		EndpointReference to = terminateRMMessage.getTo();
		if (to!=null)
			terminateBean.setToAddress(to.getAddress());

		// If this message is targetted at an anonymous address then we must not have a transport
		// ready for it, as the terminate sequence is not a reply.
		if(to == null || to.hasAnonymousAddress())
			terminateBean.setTransportAvailable(false);

		rmsBean.setTerminateAdded(true);

		storageManager.getRMSBeanMgr().update(rmsBean);

		terminateRMMessage.setProperty(Sandesha2Constants.SET_SEND_TO_TRUE, Sandesha2Constants.VALUE_TRUE);
		
		//the propertyKey of the ackMessage will be the propertyKey for the terminate message as well.
//		terminateRMMessage.setProperty(Sandesha2Constants.MessageContextProperties.SEQUENCE_PROPERTY_KEY, sequencePropertyKey);
		
		// / addTerminateSeqTransaction.commit();
	    SandeshaUtil.executeAndStore(terminateRMMessage, key, storageManager);
		
		SenderBeanMgr retramsmitterMgr = storageManager.getSenderBeanMgr();
		
		
		retramsmitterMgr.insert(terminateBean);

		if(log.isDebugEnabled())
			log.debug("Exit: TerminateManager::addTerminateSequenceMessage");
	}

}
