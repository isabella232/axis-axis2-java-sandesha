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
package org.apache.sandesha.server;

import javax.xml.soap.SOAPEnvelope;

import org.apache.axis.Message;
import org.apache.axis.MessageContext;
import org.apache.axis.components.uuid.UUIDGen;
import org.apache.axis.components.uuid.UUIDGenFactory;
import org.apache.axis.providers.java.RPCProvider;
import org.apache.sandesha.Constants;
import org.apache.sandesha.EnvelopeCreator;
import org.apache.sandesha.IStorageManager;
import org.apache.sandesha.RMMessageContext;
import org.apache.sandesha.server.queue.ServerQueue;

/**
 * @author JEkanayake
 * 
 * This class will act as the service dispatcher for Sandesha. By default it
 * will use the RPCProvider to invoke the service but need to improve this to
 * use any Provider depending on the configuration.
 */
public class RMInvoker implements Runnable {
    private IStorageManager storageManager = null;

    public RMInvoker() {
        storageManager = new ServerStorageManager();
        storageManager.init();

    }

    public void run() {
        while (true) {
            try {

                System.out
                        .println("RMInvoker THREAD IS SLEEPING -----------000----------\n");

                //Sleep for Constants.RMINVOKER_SLEEP_TIME.
                //Currently the RMInvoker is a single thread, but this needs to
                // be
                //replaced by a thread pool so that the performance can be
                // improved.
                Thread.sleep(Constants.RMINVOKER_SLEEP_TIME);

                //Get the next message to invoke.
                //storeManager is responsible of giving the correct message to
                // invoke.
                RMMessageContext rmMessageContext = storageManager
                        .getNextMessageToProcess();

                //If not return is null then proceed with invokation.
                if (rmMessageContext != null) {

                    //Currently RPCProvider is used as the default provider and
                    // this is
                    //used to actually invoke the service.
                    //To provide a maximum flexibility the actual provider
                    // should be a
                    //configurable entity where the class can be loaded at
                    // runtime.
                    RPCProvider rpcProvider = new RPCProvider();

                    System.out.println("INVOKE THE SERVICE.....\n");
                    //Call for the processMessage method.
                    rpcProvider.processMessage(
                            rmMessageContext.getMsgContext(), rmMessageContext
                                    .getReqEnv(), rmMessageContext.getResEnv(),
                            rmMessageContext.getObj());

                    //Check whether we have an output (response) or not.

                    if (rmMessageContext.getMsgContext().getOperation()
                            .getMethod().getReturnType() != Void.TYPE) {
                        System.out
                                .println("STORING THE RESPONSE MESSAGE.....\n");
                        //Store the message in the response queue.
                        //If there is an application response then that
                        // response is always sent using
                        //a new HTTP connection and the <replyTo> header is
                        // used in this case.
                        //This is done by the RMSender.
                        rmMessageContext
                                .setMessageType(Constants.MSG_TYPE_SERVICE_RESPONSE);

                        //System.out.println("TESTING FOR RESPONSE SEQUENCE");

                        boolean firstMsgOfResponseSeq = !storageManager
                                .isResponseSequenceExist(rmMessageContext
                                        .getSequenceID());
                                                
                        storageManager.insertResponseMessage(rmMessageContext); //This
                                                                                // will
                                                                                // automatically
                                                                                // create
                                                                                // a
                                                                                // response
                                                                                // requence
                                                                                // and
                                                                                // add
                                                                                // the
                                                                                // message.

                        //Need to decide whether the server needs resource
                        // reclamtion or not.
                        //This can be a property set by adminstrator and may be
                        // in the deploy.wsdd
                        //If it is not required to send the
                        // CreateSequenceRequest messages before sending
                        //application respones then we can use the same
                        // sequence as the incoming one or
                        //can create a new UUID from here.

                        if (firstMsgOfResponseSeq) {
                            System.out.println("NO RESPONSE SEQUENCE");
                            RMMessageContext rmMsgContext = new RMMessageContext();
                            rmMessageContext.copyContents(rmMsgContext);
                            
                            MessageContext msgContext = new MessageContext(
                                    rmMessageContext.getMsgContext()
                                            .getAxisEngine());
                            
                           // RMMessageContext.copyMessageContext(
                           //         rmMessageContext.getMsgContext(),
                           //         msgContext);
                            //Set this new msgContext to the rmMsgContext.
                                                       
                            rmMessageContext.setMsgContext(msgContext);

                            rmMsgContext
                                    .setMessageType(Constants.MSG_TYPE_CREATE_SEQUENCE_REQUEST);

                            UUIDGen uuid = UUIDGenFactory.getUUIDGen();
                            String id = uuid.nextUUID();
                            //String id = "abcdefghijk";
                            
                            //Need to add "uuid" or we can always remove this part
                            //Posible Problem
                            //TODO
                            storageManager.setTemporaryOutSequence(rmMsgContext
                                    .getSequenceID(), "uuid:"+id);
                            SOAPEnvelope createSequenceEnvelope = EnvelopeCreator
                                    .createCreateSequenceEnvelope(id,
                                            rmMsgContext, Constants.SERVER);

                            
                            rmMsgContext.getMsgContext().setRequestMessage(
                                    new Message(createSequenceEnvelope));

                            //TODO Check : This line is needed right ?
                            rmMsgContext.setOutGoingAddress(rmMsgContext
                                    .getAddressingHeaders().getReplyTo()
                                    .getAddress().toString());

                    
                            
                            rmMsgContext.setMessageID("uuid:"+id);
                            storageManager
                                    .addCreateSequenceRequest(rmMsgContext);
                            System.out.println("END OF IF");

                        }

                        ServerQueue sq = ServerQueue.getInstance();
                        sq.displayPriorityQueue();
                        sq.displayOutgoingMap();
                        sq.displayIncomingMap();

                        //we need to check whether we have this sequence in the
                        // response queue.
                        //storageManager.isResponseSequenceExist();
                        //If this is false then we need to create a new create
                        // sequence request messag
                        //and add it to the queue.
                        /*
                         * if(storageManager.isResponseSequenceExist(rmMessageContext.getSequenceID())){
                         * 
                         * RMMessageContext rmMsgContext= new
                         * RMMessageContext();
                         * rmMessageContext.copyContents(rmMsgContext);
                         * rmMsgContext.setMessageType(Constants.MSG_TYPE_CREATE_SEQUENCE_REQUEST);
                         * 
                         * //Use UUIDGen to genrrate a temp messageID
                         * 
                         * //We will create a new out sequence with this
                         * right??.
                         * storageManager.setTemporaryOutSequence(rmMessageContext.getSequenceID(),"abcdefghijklmnop");
                         * 
                         * SOAPEnvelope
                         * createSequenceEnvelop=EnvelopeCreator.createCreateSequenceRequestEnvelope(String
                         * messageID);
                         * 
                         * MessageContext msgContext = new
                         * MessageContext(rmMessageContext.getMsgContext().getAxisEngine());
                         * RMMessageContext.copyMessageContext(rmMessageContext.getMsgContext(),msgContext);
                         * 
                         * rmMsgContext.getMsgContext().setRequestMessage(new
                         * Message(createSequenceEnvelop));
                         * 
                         * storageManager.addCreateSequenceRequest(rmMsgContext); }
                         */

                        //At this point we don't know the new sequence that we
                        // are going to use for sending
                        //the response messages. So we will just add the
                        // response message as it is,
                        //without RM or Addressing headers

                        /*
                         * //@@@@@@@@@@ ----THIS PART IS JUST FOR
                         * TESTING-----@@@@@@@@@@@@@@@@@@@@@ //If we enable this
                         * part, we can just send responses asynchronously.
                         * 
                         * //We have to get this using create sequence from the
                         * client. //This should be able to switched off
                         * depending on the server config.
                         * storageManager.setTemporaryOutSequence(rmMessageContext.getSequenceID(),"abcdefghijklmnop");
                         * storageManager.setApprovedOutSequence("abcdefghijklmnop","qrstuvwxyz1234");
                         * 
                         * //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
                         */

                    }
                }
            } catch (InterruptedException e2) {
                // TODO Auto-generated catch block
                e2.printStackTrace();
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        }

    }
}