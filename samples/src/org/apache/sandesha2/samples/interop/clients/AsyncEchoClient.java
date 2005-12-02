/*
 * Copyright 2004,2005 The Apache Software Foundation.
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
 */

package org.apache.sandesha2.samples.interop.clients;

import javax.xml.namespace.QName;

import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.client.Call;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.async.AsyncResult;
import org.apache.axis2.client.async.Callback;
import org.apache.axis2.context.MessageContextConstants;
import org.apache.axis2.om.OMAbstractFactory;
import org.apache.axis2.om.OMElement;
import org.apache.axis2.om.OMFactory;
import org.apache.axis2.om.OMNamespace;
import org.apache.axis2.soap.SOAP12Constants;
import org.apache.sandesha2.Sandesha2Constants.ClientAPI;
import org.apache.sandesha2.util.SandeshaUtil;

public class AsyncEchoClient {
	
	private String toIP = "127.0.0.1";
	
	private String toPort = "8070";
	
	private String ackIP = "127.0.0.1";
	
	private String ackPort = "9070";
	
	private String toEPR = "http://" + toIP +  ":" + toPort + "/axis2/services/RMInteropService";

	private String acksToEPR = "http://" + ackIP +  ":" + ackPort + "/axis2/services/AnonymousService/echoString";
	
	private String SANDESHA2_HOME = "<SANDESHA2_HOME>"; //Change this to ur path.
	
	private String AXIS2_CLIENT_PATH = SANDESHA2_HOME + "\\target\\client\\";   //this will be available after a maven build
	
	public static void main(String[] args) throws Exception {		
		new AsyncEchoClient ().run();
	}
	
	private void run () throws Exception {
		if ("<SANDESHA2_HOME>".equals(SANDESHA2_HOME)){
			System.out.println("ERROR: Please change <SANDESHA2_HOME> to your Sandesha2 installation directory.");
			return;
		}
		
		Call call = new Call(AXIS2_CLIENT_PATH);
		call.engageModule(new QName("sandesha"));
		Options clientOptions = new Options ();
		clientOptions.setProperty(Options.COPY_PROPERTIES,new Boolean (true));
		call.setClientOptions(clientOptions);
		clientOptions.setProperty(ClientAPI.AcksTo,acksToEPR);
		clientOptions.setSoapVersionURI(SOAP12Constants.SOAP_ENVELOPE_NAMESPACE_URI);
		clientOptions.setTo(new EndpointReference(toEPR));
		clientOptions.setProperty(MessageContextConstants.TRANSPORT_URL,toEPR);
		clientOptions.setProperty(ClientAPI.SEQUENCE_KEY,"sequence1");  //Optional
		clientOptions.setSoapAction("test:soap:action");
		clientOptions.setProperty(ClientAPI.OFFERED_SEQUENCE_ID,SandeshaUtil.getUUID());  //Optional
		Callback callback1 = new TestCallback ("Callback 1");
		call.invokeNonBlocking("echoString", getEchoOMBlock("echo1"),callback1);
		Callback callback2 = new TestCallback ("Callback 2");
		call.invokeNonBlocking("echoString", getEchoOMBlock("echo2"),callback2);
		clientOptions.setProperty(ClientAPI.LAST_MESSAGE, "true");
		Callback callback3 = new TestCallback ("Callback 3");
		call.invokeNonBlocking("echoString", getEchoOMBlock("echo3"),callback3);
		
        while (!callback3.isComplete()) {
            Thread.sleep(1000);
        }
		
		call.close();
	}

	private static OMElement getEchoOMBlock(String text) {
		OMFactory fac = OMAbstractFactory.getOMFactory();
		OMNamespace defaultNS = fac.createOMNamespace("http://tempuri.apache.org","ns1");
		OMElement echoElement = fac.createOMElement("echoString", null);
		OMElement paramElement = fac.createOMElement("text", null);
		echoElement.addChild(paramElement);
		paramElement.setText(text);

		return echoElement;
	}

	class TestCallback extends Callback {

		String name = null;
		
		public TestCallback (String name) {
			this.name = name;
		}
		
		public void onComplete(AsyncResult result) {
			//System.out.println("On Complete Called for " + text);
			OMElement responseElement = result.getResponseEnvelope().getBody().getFirstElement();
			if (responseElement==null) {
				System.out.println("Response element is null");
				return;
			}
			
			String tempText = responseElement.getText();
			if (tempText==null || "".equals(tempText)){
				OMElement child = responseElement.getFirstElement();
				if (child!=null)
					tempText = child.getText();
			}
			
			
			tempText = (tempText==null)?"":tempText;
			
			System.out.println("Callback '" + name +  "' got result:" + tempText);
			
		}

		public void reportError(Exception e) {
			// TODO Auto-generated method stub
			System.out.println("Error reported for test call back");
		}
	}

	
}
