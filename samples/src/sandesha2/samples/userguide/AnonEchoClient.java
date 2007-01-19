package sandesha2.samples.userguide;

import java.io.File;

import javax.xml.namespace.QName;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMNamespace;
import org.apache.axiom.soap.SOAP12Constants;
import org.apache.axiom.soap.SOAPBody;
import org.apache.axis2.Constants;
import org.apache.axis2.addressing.AddressingConstants;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.client.async.AsyncResult;
import org.apache.axis2.client.async.Callback;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.ConfigurationContextFactory;
import org.apache.axis2.context.MessageContextConstants;
import org.apache.sandesha2.Sandesha2Constants;
import org.apache.sandesha2.client.SandeshaClientConstants;
import org.apache.sandesha2.util.SandeshaUtil;

public class AnonEchoClient {
	
	private final static String applicationNamespaceName = "http://tempuri.org/"; 
	private final static String echoString = "echoString";
	private final static String Text = "Text";
	private final static String Sequence = "Sequence";
	private final static String echoStringResponse = "echoStringResponse";
	private final static String EchoStringReturn = "EchoStringReturn";
	
	private String toIP = "127.0.0.1";
	
	private String toPort = "8070";
	
	private String transportToPort = "8070";
	
	private String toEPR = "http://" + toIP +  ":" + toPort + "/axis2/services/RMSampleService";
	
	private String transportToEPR = "http://" + toIP +  ":" + transportToPort + "/axis2/services/RMSampleService";
	
	private static String SANDESHA2_HOME = "<SANDESHA2_HOME>"; //Change this to ur path.
	
	private static String AXIS2_CLIENT_PATH = SANDESHA2_HOME + File.separator + "target" + File.separator +"repos" + File.separator + "client" + File.separator;   //this will be available after a maven build
	
	public static void main(String[] args) throws Exception {
		
		
		String axisClientRepo = null;
		if (args!=null && args.length>0)
			axisClientRepo = args[0];
		
		if (axisClientRepo!=null && !"".equals(axisClientRepo)) {
			AXIS2_CLIENT_PATH = axisClientRepo;
			SANDESHA2_HOME = "";
		}
		
		new AnonEchoClient ().run();
	}
	
	private void run () throws Exception {
		
		if ("<SANDESHA2_HOME>".equals(SANDESHA2_HOME)){
			System.out.println("ERROR: Please set the directory you unzipped Sandesha2 as the first option.");
			return;
		}

		String axis2_xml = AXIS2_CLIENT_PATH + "client_axis2.xml";
     
		ConfigurationContext configContext = ConfigurationContextFactory.createConfigurationContextFromFileSystem(AXIS2_CLIENT_PATH,axis2_xml);

		ServiceClient serviceClient = new ServiceClient (configContext,null);	
		
		Options clientOptions = new Options ();
		
		clientOptions.setTo(new EndpointReference (toEPR));
		
		String sequenceKey = SandeshaUtil.getUUID();  //sequence key for thie sequence.
		clientOptions.setProperty(SandeshaClientConstants.SEQUENCE_KEY,sequenceKey);
		
		clientOptions.setReplyTo(new EndpointReference (AddressingConstants.Submission.WSA_ANONYMOUS_URL));
		
		clientOptions.setProperty(MessageContextConstants.TRANSPORT_URL,transportToEPR);
		
		clientOptions.setSoapVersionURI(SOAP12Constants.SOAP_ENVELOPE_NAMESPACE_URI);   //uncomment this to send messages in SOAP 1.2
		
		clientOptions.setProperty(AddressingConstants.WS_ADDRESSING_VERSION,AddressingConstants.Submission.WSA_NAMESPACE);
		
		clientOptions.setProperty(SandeshaClientConstants.OFFERED_SEQUENCE_ID,SandeshaUtil.getUUID());  //Uncomment this to offer a sequenceID for the incoming sequence.
		
		clientOptions.setAction("urn:wsrm:EchoString");
		
		clientOptions.setProperty(Constants.Configuration.USE_CUSTOM_LISTENER, Boolean.TRUE);
		clientOptions.setProperty(SandeshaClientConstants.RM_SPEC_VERSION, Sandesha2Constants.SPEC_VERSIONS.v1_0);
		
		serviceClient.engageModule(new QName ("sandesha2"));
		
		//You must set the following two properties in the request-reply case.
		clientOptions.setTransportInProtocol(Constants.TRANSPORT_HTTP);

		//This asks Axis2 not to start a listner.
		clientOptions.setUseSeparateListener(true);
		
		serviceClient.setOptions(clientOptions);

		Callback callback1 = new TestCallback ("Callback 1");
		serviceClient.sendReceiveNonBlocking(getEchoOMBlock("echo1",sequenceKey),callback1);
		
		Callback callback2 = new TestCallback ("Callback 2");
		serviceClient.sendReceiveNonBlocking(getEchoOMBlock("echo2",sequenceKey),callback2);
		
		clientOptions.setProperty(SandeshaClientConstants.LAST_MESSAGE, "true");
		
		Callback callback3 = new TestCallback ("Callback 3");
		serviceClient.sendReceiveNonBlocking(getEchoOMBlock("echo3",sequenceKey),callback3);
		
        while (!callback3.isComplete()) {
            Thread.sleep(1000);
        }
        
        Thread.sleep(5000);
        
	}

	private static OMElement getEchoOMBlock(String text, String sequenceKey) {
		OMFactory fac = OMAbstractFactory.getOMFactory();
		OMNamespace applicationNamespace = fac.createOMNamespace(applicationNamespaceName,"ns1");
		OMElement echoStringElement = fac.createOMElement(echoString, applicationNamespace);
		OMElement textElem = fac.createOMElement(Text,applicationNamespace);
		OMElement sequenceElem = fac.createOMElement(Sequence,applicationNamespace);
		
		textElem.setText(text);
		sequenceElem.setText(sequenceKey);
		echoStringElement.addChild(textElem);
		echoStringElement.addChild(sequenceElem);
		
		return echoStringElement;
	}

	public class TestCallback extends Callback {

		String name = null;
		
		public TestCallback () {
			
		}
		
		public TestCallback (String name) {
			this.name = name;
		}
		
		public void onComplete(AsyncResult result) {
			//System.out.println("On Complete Called for " + text);
			SOAPBody body = result.getResponseEnvelope().getBody();
			
			OMElement echoStringResponseElem = body.getFirstChildWithName(new QName (applicationNamespaceName,echoStringResponse));
			if (echoStringResponseElem==null) { 
				System.out.println("Error: SOAPBody does not have a 'echoStringResponse' child");
				return;
			}
			
			OMElement echoStringReturnElem = echoStringResponseElem.getFirstChildWithName(new QName (applicationNamespaceName,EchoStringReturn));
			if (echoStringReturnElem==null) { 
				System.out.println("Error: 'echoStringResponse' element does not have a 'EchoStringReturn' child");
				return;
			}
			
			String resultStr = echoStringReturnElem.getText();
			System.out.println("Callback '" + name +  "' got result:" + resultStr);
		}

		public void onError (Exception e) {
			System.out.println("Error reported for test call back");
			e.printStackTrace();
		}
	}

	
}
