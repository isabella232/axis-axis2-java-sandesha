/*
 * Copyright 2004,2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.sandesha2;

import java.io.File;
import java.util.List;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMNamespace;
import org.apache.axiom.soap.SOAP11Constants;
import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.Constants.Configuration;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.ConfigurationContextFactory;
import org.apache.axis2.context.MessageContextConstants;
import org.apache.axis2.transport.http.SimpleHTTPServer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.sandesha2.client.SandeshaClient;
import org.apache.sandesha2.client.SandeshaClientConstants;
import org.apache.sandesha2.client.SequenceReport;
import org.apache.sandesha2.storage.StorageManager;
import org.apache.sandesha2.storage.beanmanagers.SenderBeanMgr;
import org.apache.sandesha2.storage.beans.SenderBean;
import org.apache.sandesha2.util.SandeshaUtil;

public class SandeshaClientTest extends SandeshaTestCase {

	SimpleHTTPServer httpServer = null;
	private Log log = LogFactory.getLog(getClass());
	int serverPort = DEFAULT_SERVER_TEST_PORT;
	
	private static final String applicationNamespaceName = "http://tempuri.org/"; 
	private static final String ping = "ping";
	private static final String Text = "Text";

	
	public SandeshaClientTest () {
		super ("SandeshaClientTest");
	}
	
	public void setUp () {
		
		String serverPortStr = getTestProperty("test.server.port");
		if (serverPortStr!=null) {
		
			try {
				serverPort = Integer.parseInt(serverPortStr);
			} catch (NumberFormatException e) {
				log.error(e);
			}
		}
		
	}
	
	private void startServer() throws AxisFault {
		
		String repoPath = "target" + File.separator + "repos" + File.separator + "server";
		String axis2_xml = "target" + File.separator + "repos" + File.separator + "server" + File.separator + "server_axis2.xml";


		ConfigurationContext configContext = ConfigurationContextFactory.createConfigurationContextFromFileSystem(repoPath,axis2_xml);
		httpServer = new SimpleHTTPServer (configContext,serverPort);
		httpServer.start();
		try {
			Thread.sleep(300);
		} catch (InterruptedException e) {
			throw new SandeshaException ("sleep interupted");
		}

	}
	
	public void tearDown () throws SandeshaException {
		if (httpServer!=null)
			httpServer.stop();
		
		try {
			Thread.sleep(300);
		} catch (InterruptedException e) {
			throw new SandeshaException ("sleep interupted");
		}
	}
	
	public void testCreateSequenceWithOffer () throws AxisFault,InterruptedException {
		
		String to = "http://127.0.0.1:" + serverPort + "/axis2/services/RMSampleService";
		String transportTo = "http://127.0.0.1:" + serverPort + "/axis2/services/RMSampleService";
		
		String repoPath = "target" + File.separator + "repos" + File.separator + "client";
		String axis2_xml = "target" + File.separator + "repos" + File.separator + "client" + File.separator + "client_axis2.xml";
		
		ConfigurationContext configContext = ConfigurationContextFactory.createConfigurationContextFromFileSystem(repoPath,axis2_xml);
		Options clientOptions = new Options ();
		
		clientOptions.setTo(new EndpointReference (to));
		clientOptions.setProperty(Configuration.TRANSPORT_URL,transportTo);
		
//		String sequenceKey = SandeshaUtil.getUUID();
//		clientOptions.setProperty(SandeshaClientConstants.SEQUENCE_KEY,sequenceKey);
		
		ServiceClient serviceClient = new ServiceClient (configContext,null);
		
		startServer();
		try
		{
			String acksTo = serviceClient.getMyEPR(Constants.TRANSPORT_HTTP).getAddress();
			clientOptions.setProperty(SandeshaClientConstants.AcksTo,acksTo);
			clientOptions.setTransportInProtocol(Constants.TRANSPORT_HTTP);
			
			String offeredSequenceID = SandeshaUtil.getUUID();
			clientOptions.setProperty(SandeshaClientConstants.OFFERED_SEQUENCE_ID,offeredSequenceID);
			
			serviceClient.setOptions(clientOptions);
			//serviceClient.
			
			clientOptions.setTransportInProtocol(Constants.TRANSPORT_HTTP);
			clientOptions.setUseSeparateListener(true);
			
			serviceClient.setOptions(clientOptions);
			
			String sequenceKey = SandeshaClient.createSequence(serviceClient,true);
			clientOptions.setProperty(SandeshaClientConstants.SEQUENCE_KEY, sequenceKey);
			
			Thread.sleep(10000);
			
			SequenceReport sequenceReport = SandeshaClient.getOutgoingSequenceReport(serviceClient);
			
			assertNotNull(sequenceReport.getSequenceID());
			assertFalse(sequenceReport.isSecureSequence());
		}
		finally
		{
			configContext.getListenerManager().stop();
			serviceClient.cleanup();			
		}

	}
	
	public void testSequenceCloseTerminate()throws Exception{
			startServer();
			String to = "http://127.0.0.1:" + serverPort + "/axis2/services/RMSampleService";
			String transportTo = "http://127.0.0.1:" + serverPort + "/axis2/services/RMSampleService";
			
			String repoPath = "target" + File.separator + "repos" + File.separator + "client";
			String axis2_xml = "target" + File.separator + "repos" + File.separator + "client" + File.separator + "client_axis2.xml";
			
			ConfigurationContext configContext = ConfigurationContextFactory.createConfigurationContextFromFileSystem(repoPath,axis2_xml);
			
			Options clientOptions = new Options ();
			clientOptions.setSoapVersionURI(SOAP11Constants.SOAP_ENVELOPE_NAMESPACE_URI);
		   clientOptions.setProperty(SandeshaClientConstants.RM_SPEC_VERSION, 
		       Sandesha2Constants.SPEC_VERSIONS.v1_1);
			clientOptions.setTo(new EndpointReference (to));
			clientOptions.setProperty(MessageContextConstants.TRANSPORT_URL,transportTo);
			
			String sequenceKey = "some_sequence_key";
			clientOptions.setProperty(SandeshaClientConstants.SEQUENCE_KEY,sequenceKey);
			
			ServiceClient serviceClient = new ServiceClient (configContext,null);
			
			String acksTo = serviceClient.getMyEPR(Constants.TRANSPORT_HTTP).getAddress();
			clientOptions.setProperty(SandeshaClientConstants.AcksTo,acksTo);
			clientOptions.setTransportInProtocol(Constants.TRANSPORT_HTTP);
				//serviceClient.
			serviceClient.setOptions(clientOptions);
				
			try{
				
				serviceClient.fireAndForget(getPingOMBlock("ping1"));
				
				Thread.sleep(10000);
				
				SequenceReport sequenceReport = SandeshaClient.getOutgoingSequenceReport(serviceClient);
				assertNotNull(sequenceReport.getSequenceID());
				
				//now close the sequence
				SandeshaClient.closeSequence(serviceClient);
				
				//try and send another msg - this should fail
				try{
					serviceClient.fireAndForget(getPingOMBlock("ping2"));
					fail(); //this should have failed
				}
				catch(Exception e){
					//good
				}
			
				//finally terminate the sequence
				terminateAndCheck(serviceClient);
			}
			finally{
				configContext.getListenerManager().stop();
				serviceClient.cleanup();			
			}
			
		}
		
		private void terminateAndCheck(ServiceClient srvcClient)throws Exception{
			SandeshaClient.terminateSequence(srvcClient);
			//wait
			Thread.sleep(1000);
			//now check the sequence is terminated
			SequenceReport report = SandeshaClient.getOutgoingSequenceReport(srvcClient);
			assertNotNull(report);
			assertEquals(report.getSequenceStatus(), SequenceReport.SEQUENCE_STATUS_TERMINATED);
			
		}
	
//	public void testCreateSequenceWithoutOffer () {
////		SandeshaClient.createSequence(serviceClient,true);
//		
//		
//	}
	
//	public void testCreateSequenceWithSequenceKey () {
//		
//	}
//	
//	public void testTerminateSequence () {
//		
//	}
//	
//	public void testCloseSequence () {
//		
//	}
//
	/**
	 * Test that sending an ACK request gets transmitted
	 * This doesn't check the content of the Ack Request, only that the
	 * SenderBean no longer exists for it.
	 */
	public void testAckRequest () throws Exception {
		startServer();
		
		String to = "http://127.0.0.1:" + serverPort + "/axis2/services/RMSampleService";
		String transportTo = "http://127.0.0.1:" + serverPort + "/axis2/services/RMSampleService";
		
		String repoPath = "target" + File.separator + "repos" + File.separator + "client";
		String axis2_xml = "target" + File.separator + "repos" + File.separator + "client" + File.separator + "client_axis2.xml";
		
		ConfigurationContext configContext = ConfigurationContextFactory.createConfigurationContextFromFileSystem(repoPath,axis2_xml);

		Options clientOptions = new Options ();

		clientOptions.setTo(new EndpointReference (to));
		clientOptions.setProperty(Configuration.TRANSPORT_URL,transportTo);
				
		ServiceClient serviceClient = new ServiceClient (configContext,null);
		
		clientOptions.setTransportInProtocol(Constants.TRANSPORT_HTTP);
		clientOptions.setProperty(SandeshaClientConstants.RM_SPEC_VERSION, Sandesha2Constants.SPEC_VERSIONS.v1_1);
		clientOptions.setUseSeparateListener(true);
		
		serviceClient.setOptions(clientOptions);
				
		// Create a sequence 
		SandeshaClient.createSequence(serviceClient, false, null);
		
		Thread.sleep(5000);
		
		// Send the ACK request
		SandeshaClient.sendAckRequest(serviceClient);
		
		Thread.sleep(10000);
		
		// Get the storage manager from the ConfigurationContext
		StorageManager storageManager = SandeshaUtil.getSandeshaStorageManager(configContext, configContext.getAxisConfiguration());
		
		// Get the sequence id for this sequence.
		String sequenceId = SandeshaClient.getSequenceID(serviceClient);
		
		// Get the SenderBeanManager
		SenderBeanMgr senderManager = storageManager.getRetransmitterBeanMgr();
				
		// Check that there are no sender beans inside the SenderBeanMgr.
		SenderBean senderBean = new SenderBean();
		senderBean.setSequenceID(sequenceId);
		senderBean.setSend(true);
		senderBean.setReSend(false);
		
		// Find any sender beans for the to address.
		List beans = senderManager.find(senderBean);
		assertTrue("SenderBeans found when the list should be empty", beans.isEmpty());
		
		SequenceReport sequenceReport = SandeshaClient.getOutgoingSequenceReport(serviceClient);
		
		assertNotNull(sequenceReport.getSequenceID());
		assertFalse(sequenceReport.isSecureSequence());
		
		configContext.getListenerManager().stop();
		serviceClient.cleanup();
	}
//	
//	public void getSequenceIDTest () {
//		
//	}
	
	/**
	 * Tests that the last error and timestamp are set for the simple case of the target service not being available
	 */
	public void testLastErrorAndTimestamp() throws Exception
	{
		String to = "http://127.0.0.1:" + serverPort + "/axis2/services/RMSampleService";
		String transportTo = "http://127.0.0.1:" + serverPort + "/axis2/services/RMSampleService";

		String repoPath = "target" + File.separator + "repos" + File.separator + "client";
		String axis2_xml = "target" + File.separator + "repos" + File.separator + "client" + File.separator + "client_axis2.xml";

		ConfigurationContext configContext = ConfigurationContextFactory.createConfigurationContextFromFileSystem(repoPath,axis2_xml);

		//clientOptions.setSoapVersionURI(SOAP12Constants.SOAP_ENVELOPE_NAMESPACE_URI);
		Options clientOptions = new Options ();
		clientOptions.setSoapVersionURI(SOAP11Constants.SOAP_ENVELOPE_NAMESPACE_URI);
		
		clientOptions.setTo(new EndpointReference (to));
		clientOptions.setProperty(Configuration.TRANSPORT_URL,transportTo);
		
		String sequenceKey = "sequence1";
		clientOptions.setProperty(SandeshaClientConstants.SEQUENCE_KEY,sequenceKey);
		
		ServiceClient serviceClient = new ServiceClient (configContext,null);
		
		serviceClient.setOptions(clientOptions);
		
		serviceClient.fireAndForget(getPingOMBlock("ping1"));
		
		//starting the server after a wait
		Thread.sleep(10000);
				
		// Check that the last error and last error time stamp have been set
		String lastSendError = SandeshaClient.getLastSendError(serviceClient);
		long lastSendErrorTime = SandeshaClient.getLastSendErrorTimestamp(serviceClient);
		
		// Check the values are valid
		assertNotNull(lastSendError);
		assertTrue(lastSendErrorTime > -1);
		
		startServer();

		clientOptions.setProperty(SandeshaClientConstants.LAST_MESSAGE, "true");
		serviceClient.fireAndForget(getPingOMBlock("ping2"));
		
		
		Thread.sleep(10000);
	
		SequenceReport sequenceReport = SandeshaClient.getOutgoingSequenceReport(serviceClient);
		assertTrue(sequenceReport.getCompletedMessages().contains(new Long(1)));
		assertTrue(sequenceReport.getCompletedMessages().contains(new Long(2)));
		assertEquals(sequenceReport.getSequenceStatus(),SequenceReport.SEQUENCE_STATUS_TERMINATED);
		assertEquals(sequenceReport.getSequenceDirection(),SequenceReport.SEQUENCE_DIRECTION_OUT);
	
		configContext.getListenerManager().stop();
		serviceClient.cleanup();
	}
	
	private OMElement getPingOMBlock(String text) {
		OMFactory fac = OMAbstractFactory.getOMFactory();
		OMNamespace namespace = fac.createOMNamespace(applicationNamespaceName,"ns1");
		OMElement pingElem = fac.createOMElement(ping, namespace);
		OMElement textElem = fac.createOMElement(Text, namespace);
		
		textElem.setText(text);
		pingElem.addChild(textElem);

		return pingElem;
	}

	
}
