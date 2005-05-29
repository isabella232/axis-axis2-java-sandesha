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
package org.apache.sandesha.intergratedtest;

import junit.framework.TestCase;
import org.apache.axis.client.Call;
import org.apache.axis.client.Service;
import org.apache.axis.components.uuid.UUIDGen;
import org.apache.axis.components.uuid.UUIDGenFactory;
import org.apache.axis.encoding.XMLType;
import org.apache.axis.transport.http.SimpleAxisServer;
import org.apache.axis.EngineConfiguration;
import org.apache.axis.deployment.wsdd.WSDDDeployment;
import org.apache.axis.deployment.wsdd.WSDDDocument;
import org.apache.sandesha.Constants;
import org.apache.sandesha.RMReport;
import org.apache.sandesha.SandeshaContext;
import org.w3c.dom.Document;

import javax.xml.namespace.QName;
import javax.xml.rpc.ParameterMode;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import java.net.ServerSocket;
import java.io.File;

/**
 * Created by IntelliJ IDEA.
 * User: Jaliya
 * Date: May 20, 2005
 * Time: 4:47:43 PM
 */
public class TCScenarioTest extends TestCase {
    private static SimpleAxisServer sas = null;

    private static String defaultServerPort = "5555";
    private static String defaultClientPort = "9090";
    private static boolean serverStarted = false;
    private static int testCount = 5;

    private static String targetURL = "http://127.0.0.1:" + defaultServerPort +
            "/axis/services/RMTestService";


    public void setUp() throws Exception {
        if (!serverStarted) {
            sas = new SimpleAxisServer();

            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            DocumentBuilder db = dbf.newDocumentBuilder();

            Document doc = db.parse(new File("test-resources/server-config.wsdd"));
            WSDDDocument wsdddoc = new WSDDDocument(doc);
            WSDDDeployment wsdddep = wsdddoc.getDeployment();
            sas.setMyConfig(wsdddep);

            sas.setServerSocket(new ServerSocket((new Integer(defaultServerPort)).intValue()));
            sas.start();
            serverStarted = true;
        }
    }

    public void tearDown() throws InterruptedException {
        if (testCount == 0) {
            Thread.sleep(5000);
            sas.stop(); 
        }
    }

    /**
     * This test will test the Ping interop scenario. 3 One-way messages are sent with
     * <wsrm:AckTo> set to ANONYMOUS URI and acknowledgements are received.
     *
     * @throws Exception
     */
    public void testPingSync() throws Exception {
        System.out.println("===================Synchronous Ping Test Started=====================");

        Service service = new Service();
        Call call = (Call) service.createCall();

        SandeshaContext ctx = new SandeshaContext();
        ctx.addNewSequeceContext(call, targetURL, "urn:wsrm:Ping",
                Constants.ClientProperties.IN_ONLY);
        ctx.setSynchronous(call);

        call.setOperationName(new QName("http://tempuri.org/", "Ping"));
        call.addParameter("arg1", XMLType.XSD_STRING, ParameterMode.IN);


        call.invoke(new Object[]{"Ping Message Number One"});
        call.invoke(new Object[]{"Ping Message Number Two"});
        ctx.setLastMessage(call);
        call.invoke(new Object[]{"Ping Message Number Three"});

        RMReport report = ctx.endSequence(call);

        assertEquals(report.isAllAcked(), true);
        assertEquals(report.getNumberOfReturnMessages(), 0);
        testCount--;
        System.out.println("===================Synchronous Ping Test Finished====================");
    }

    /**
     * This test will test the Ping interop scenario. 3 One-way messages are sent with
     * <wsrm:AckTo> set to asynchronous client URI and acknowledgements are received.
     *
     * @throws Exception
     */
    public void testPingAsync() throws Exception {
        System.out.println("==================ASynchronous Ping Test Started=====================");

        Service service = new Service();
        Call call = (Call) service.createCall();

        SandeshaContext ctx = new SandeshaContext();
        ctx.addNewSequeceContext(call, targetURL, "urn:wsrm:ping",
                Constants.ClientProperties.IN_ONLY);
        ctx.setAcksToUrl(call,
                "http://127.0.0.1:" + defaultClientPort + "/axis/services/RMService");

        call.setOperationName(new QName("http://tempuri.org", "Ping"));
        call.addParameter("Text", XMLType.XSD_STRING, ParameterMode.IN);

        call.invoke(new Object[]{"Ping Message Number One"});
        call.invoke(new Object[]{"Ping Message Number Two"});
        ctx.setLastMessage(call);
        call.invoke(new Object[]{"Ping Message Number Three"});

        RMReport report = ctx.endSequence(call);

        assertEquals(report.isAllAcked(), true);
        assertEquals(report.getNumberOfReturnMessages(), 0);
        testCount--;
        System.out.println("==================ASynchronous Ping Test Finished====================");

    }

    /**
     * This test will test the echoString interop scenario. 3 echo messages are sent with
     * <wsrm:AckTo> set to ANONYMOUS URI. Acknowledgements relating to the scenario is received
     * using the same HTTP connection used in the request message while the responses are
     * received using the asynchronous client side endpoint.
     *
     * @throws Exception
     */

    public void testEchoSyncAck() throws Exception {
        System.out.println("=====================Echo(Sync Ack) Test Started=====================");

        UUIDGen uuidGen = UUIDGenFactory.getUUIDGen(); //Can use this for continuous testing.
        String str = uuidGen.nextUUID();

        Service service = new Service();
        Call call = (Call) service.createCall();

        SandeshaContext ctx = new SandeshaContext();
        ctx.addNewSequeceContext(call, targetURL, "urn:wsrm:echoString",
                Constants.ClientProperties.IN_OUT);
        ctx.setAcksToUrl(call, Constants.WSA.NS_ADDRESSING_ANONYMOUS);
        ctx.setReplyToUrl(call,
                "http://127.0.0.1:" + defaultClientPort + "/axis/services/RMService");
        ctx.setSendOffer(call);

        call.setOperationName(new QName("http://tempuri.org/", "echoString"));

        call.addParameter("arg1", XMLType.XSD_STRING, ParameterMode.IN);
        call.addParameter("arg2", XMLType.XSD_STRING, ParameterMode.IN);
        call.setReturnType(org.apache.axis.encoding.XMLType.XSD_STRING);

        String ret = (String) call.invoke(new Object[]{"Sandesha Echo 1", str});
        System.out.println("The Response for First Messsage is  :" + ret);

        ret = (String) call.invoke(new Object[]{"Sandesha Echo 2", str});
        System.out.println("The Response for Second Messsage is  :" + ret);

        ctx.setLastMessage(call);
        ret = (String) call.invoke(new Object[]{"Sandesha Echo 3", str});
        System.out.println("The Response for Third Messsage is  :" + ret);

        RMReport report = ctx.endSequence(call);

        assertEquals(report.isAllAcked(), true);
        assertEquals(report.getNumberOfReturnMessages(), 3);
        testCount--;
        System.out.println("====================Echo(Sync Ack) Test Finished=====================");
    }

    /**
     * This test will test the echoString interop scenario. 3 echo messages are sent with
     * <wsrm:AckTo> set to asynchronous client side endpoint. Acknowledgements and responses
     * are both received using the asynchronous client side endpoint.
     *
     * @throws Exception
     */
    public void testEchoAsyncAck() throws Exception {
        System.out.println("=====================Echo(Aync Ack) Test Started=====================");

        UUIDGen uuidGen = UUIDGenFactory.getUUIDGen(); //Can use this for continuous testing.
        String str = uuidGen.nextUUID();

        Service service = new Service();
        Call call = (Call) service.createCall();

        SandeshaContext ctx = new SandeshaContext();
        ctx.addNewSequeceContext(call, targetURL, "urn:wsrm:echoString",
                Constants.ClientProperties.IN_OUT);
        ctx.setAcksToUrl(call,
                "http://127.0.0.1:" + defaultClientPort + "/axis/services/RMService");
        ctx.setReplyToUrl(call,
                "http://127.0.0.1:" + defaultClientPort + "/axis/services/RMService");
        ctx.setSendOffer(call);

        call.setOperationName(new QName("http://tempuri.org/", "echoString"));

        call.addParameter("arg1", XMLType.XSD_STRING, ParameterMode.IN);
        call.addParameter("arg2", XMLType.XSD_STRING, ParameterMode.IN);
        call.setReturnType(org.apache.axis.encoding.XMLType.XSD_STRING);

        String ret = (String) call.invoke(new Object[]{"Sandesha Echo 1", str});
        System.out.println("The Response for First Messsage is  :" + ret);

        ret = (String) call.invoke(new Object[]{"Sandesha Echo 2", str});
        System.out.println("The Response for Second Messsage is  :" + ret);

        ctx.setLastMessage(call);
        ret = (String) call.invoke(new Object[]{"Sandesha Echo 3", str});
        System.out.println("The Response for Third Messsage is  :" + ret);

        RMReport report = ctx.endSequence(call);

        assertEquals(report.isAllAcked(), true);
        assertEquals(report.getNumberOfReturnMessages(), 3);
        testCount--;
        System.out.println("===================Echo(Async Ack) Test Finished=====================");
    }

    /**
     * This test will test the echoString interop scenario and Ping scenario together.
     * Response of each echoString request is used to invoke a Ping service.  This test tests the
     * capability of Sandesha Client side endpoint to handle multiple web service requests
     * at the same time.
     *
     * @throws Exception
     */
    public void testEchoPing() throws Exception {
        System.out.println("================Echo and Ping Combined Test Started==================");
        UUIDGen uuidGen = UUIDGenFactory.getUUIDGen(); //Can use this for continuous testing.
        String str = uuidGen.nextUUID();

        Service service = new Service();
        Call echoCall = (Call) service.createCall();

        SandeshaContext ctx = new SandeshaContext();
        //------------------------ECHO--------------------------------------------
        ctx.addNewSequeceContext(echoCall, targetURL, "urn:wsrm:echoString",
                Constants.ClientProperties.IN_OUT);
        ctx.setAcksToUrl(echoCall,
                "http://127.0.0.1:" + defaultClientPort + "/axis/services/RMService");
        ctx.setReplyToUrl(echoCall,
                "http://127.0.0.1:" + defaultClientPort + "/axis/services/RMService");
        ctx.setSendOffer(echoCall);

        echoCall.setOperationName(new QName("http://tempuri.org/", "echoString"));

        echoCall.addParameter("arg1", XMLType.XSD_STRING, ParameterMode.IN);
        echoCall.addParameter("arg2", XMLType.XSD_STRING, ParameterMode.IN);
        echoCall.setReturnType(org.apache.axis.encoding.XMLType.XSD_STRING);
        //----------------------ECHO------------------------------------------------

        //------------------------PING--------------------------------------------
        Call pingCall = (Call) service.createCall();
        ctx.addNewSequeceContext(pingCall, targetURL, "urn:wsrm:Ping",
                Constants.ClientProperties.IN_ONLY);
        ctx.setAcksToUrl(pingCall,
                "http://127.0.0.1:" + defaultClientPort + "/axis/services/RMService");

        pingCall.setOperationName(new QName("http://tempuri.org/", "ping"));
        pingCall.addParameter("arg2", XMLType.XSD_STRING, ParameterMode.IN);
        //----------------------PING------------------------------------------------


        String ret = (String) echoCall.invoke(new Object[]{"Sandesha Echo 1", str});
        System.out.println("The Response for First Messsage is  :" + ret);
        pingCall.invoke(new Object[]{ret});

        ret = (String) echoCall.invoke(new Object[]{"Sandesha Echo 2", str});
        System.out.println("The Response for Second Messsage is  :" + ret);
        pingCall.invoke(new Object[]{ret});

        ctx.setLastMessage(echoCall);
        ret = (String) echoCall.invoke(new Object[]{"Sandesha Echo 3", str});
        System.out.println("The Response for Third Messsage is  :" + ret);
        ctx.setLastMessage(pingCall);
        pingCall.invoke(new Object[]{ret});

        RMReport echoReport = ctx.endSequence(echoCall);
        RMReport pingReport=ctx.endSequence(pingCall);

        assertEquals(echoReport.isAllAcked(), true);
        assertEquals(echoReport.getNumberOfReturnMessages(), 3);

        assertEquals(pingReport.isAllAcked(), true);
        assertEquals(pingReport.getNumberOfReturnMessages(), 0);
        testCount--;
        System.out.println("===============Echo and Ping Combined Test Finished==================");

    }

}
