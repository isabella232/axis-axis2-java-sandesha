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
package org.apache.sandesha.client;

import org.apache.axis.AxisFault;
import org.apache.axis.client.Call;
import org.apache.sandesha.Constants;
import org.apache.sandesha.RMMessageContext;
import org.apache.sandesha.SandeshaContext;

import javax.xml.namespace.QName;

/**
 * This class is used to get the client side properties and to set them to the RMMessageContext.
 *
 * @auther Jaliya Ekanayake
 */
public class ClientPropertyValidator {

    public static synchronized RMMessageContext validate(Call call) throws AxisFault {

        RMMessageContext rmMessageContext = null;

        boolean inOut = getInOut(call);
        long msgNumber = getMessageNumber(call);
        boolean lastMessage = getLastMessage(call);
        boolean sync = getSync(call);
        String action = getAction(call);
        String sourceURL = null;
        String from = getFrom(call);
        String replyTo = getReplyTo(call);
        String acksTo = getAcksTo(call);
        String to = getTo(call);
        String faultTo = getFaultTo(call);
        boolean sendOffer = getOffer(call);
        String key = getKey(call);
        SandeshaContext ctx = getCtx(call);

        try {
            sourceURL = getSourceURL(call);
        } catch (Exception e) {
            throw new AxisFault(e.getMessage());
        }

        String errorMsg = getValidated(msgNumber, action, replyTo, sync, inOut);
        if (errorMsg == null) {
            rmMessageContext = new RMMessageContext();

            rmMessageContext.setSync(sync);
            rmMessageContext.setHasResponse(inOut);
            rmMessageContext.setMsgNumber(msgNumber);
            rmMessageContext.setLastMessage(lastMessage);
            rmMessageContext.setSourceURL(sourceURL);
            rmMessageContext.setSequenceID(key);
            rmMessageContext.setReplyTo(replyTo);
            rmMessageContext.setFrom(from);
            rmMessageContext.setAction(action);
            rmMessageContext.setAcksTo(acksTo);
            rmMessageContext.setTo(to);
            rmMessageContext.setFaultTo(faultTo);
            rmMessageContext.setSendOffer(sendOffer);
            rmMessageContext.setCtx(ctx);
            return rmMessageContext;

        } else
            throw new AxisFault(errorMsg);

    }

    private static SandeshaContext getCtx(Call call) {
        return (SandeshaContext) call.getProperty("context");
    }

    private static String getKey(Call call) {
        return (String) call.getProperty(Constants.ClientProperties.CALL_KEY);
    }

    private static boolean getOffer(Call call) {
        Boolean sendOffer = (Boolean) call.getProperty(Constants.ClientProperties.SEND_OFFER);
        if (sendOffer != null) {
            return sendOffer.booleanValue();
        } else
            return false;
    }

    private static String getFaultTo(Call call) {
        String faultTo = (String) call.getProperty(Constants.ClientProperties.FAULT_TO);
        if (faultTo != null)
            return faultTo;
        else
            return null;
    }

    private static String getTo(Call call) {
        String to = (String) call.getProperty(Constants.ClientProperties.TO);
        if (to != null)
            return to;
        else
            return call.getTargetEndpointAddress();
    }


    /**
     * This will decide whether we have an IN-OUT style service request
     * or IN-ONLY service request by checking the value of the QName
     * returned by the call.getReturnType().
     *
     * @param call
     * @return
     */
    private static boolean getInOut(Call call) {
        QName returnQName = (QName) call.getReturnType();
        if (returnQName != null)
            return true;
        else
            return false;

    }


    private static String getAction(Call call) {
        String action = (String) call.getProperty(Constants.ClientProperties.ACTION);
        if (action != null)
            return action;
        else
            return null;
    }

    private static String getAcksTo(Call call) {
        String acksTo = (String) call.getProperty(Constants.ClientProperties.ACKS_TO);
        if (acksTo != null)
            return acksTo;
        else
            return null;
    }

    private static boolean getSync(Call call) {
        Boolean synchronous = (Boolean) call.getProperty(Constants.ClientProperties.SYNC);
        if (synchronous != null) {
            return synchronous.booleanValue();
        } else
            return false;//If the user has not specified the synchronous
    }


    private static String getSourceURL(Call call) throws Exception {
        String sourceURL = null;
        sourceURL = (String) call.getProperty(Constants.ClientProperties.SOURCE_URL);
        if (sourceURL != null) {
            return sourceURL;
        } else {
            throw new Exception("No Source URL specified");

        }
    }

    /**
     * @param call
     * @return
     */
    private static long getMessageNumber(Call call) {
        Object temp = call.getProperty(Constants.ClientProperties.MSG_NUMBER);
        SandeshaContext ctx = (SandeshaContext) call.getProperty("context");
        long msgNo = ctx.getMessageNumber();
        if (temp == null) {
            ctx.setMessageNumber(++msgNo);
        } else {
            msgNo = ((Long) call.getProperty(Constants.ClientProperties.MSG_NUMBER)).longValue();

        }

        return msgNo;
    }

    private static boolean getLastMessage(Call call) {
        Boolean lastMessage = (Boolean) call.getProperty(Constants.ClientProperties.LAST_MESSAGE);
        if (lastMessage != null)
            return lastMessage.booleanValue();
        else
            return false;

    }

    private static String getValidated(long msgNumber, String action, String replyTo, boolean sync,
                                       boolean inOut) {
        String errorMsg = null;

        if (sync && inOut && replyTo == null) {
            errorMsg = Constants.ErrorMessages.CLIENT_PROPERTY_VALIDATION_ERROR;
            return errorMsg;
        }

        if ((action == null)) {
            errorMsg = Constants.ErrorMessages.MESSAGE_NUMBER_NOT_SPECIFIED;
            return errorMsg;
        }
        return errorMsg;
    }

    private static String getFrom(Call call) {
        return (String) call.getProperty(Constants.ClientProperties.FROM);
    }

    private static String getReplyTo(Call call) {
        return (String) call.getProperty(Constants.ClientProperties.REPLY_TO);
    }

}