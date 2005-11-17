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
 * 
 */
package org.apache.sandesha2.storage.inmemory;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;

import org.apache.axis2.context.AbstractContext;
import org.apache.sandesha2.Constants;
import org.apache.sandesha2.SandeshaException;
import org.apache.sandesha2.storage.beanmanagers.RetransmitterBeanMgr;
import org.apache.sandesha2.storage.beans.RetransmitterBean;

/**
 * @author Chamikara Jayalath <chamikara@wso2.com>
 * @author Sanka Samaranayake <ssanka@gmail.com>
 */
public class InMemoryRetransmitterBeanMgr implements RetransmitterBeanMgr {
	private Hashtable table = null;

	public InMemoryRetransmitterBeanMgr(AbstractContext context) {
		Object obj = context.getProperty(Constants.BeanMAPs.RETRANSMITTER);
		if (obj != null) {
			table = (Hashtable) obj;
		} else {
			table = new Hashtable();
			context.setProperty(Constants.BeanMAPs.RETRANSMITTER, table);
		}
	}

	public boolean delete(String MessageId) {
		return table.remove(MessageId) != null;
	}

	public RetransmitterBean retrieve(String MessageId) {
		return (RetransmitterBean) table.get(MessageId);
	}

	public boolean insert(RetransmitterBean bean) throws SandeshaException {
		if (bean.getMessageId() == null)
			throw new SandeshaException("Key (MessageId) is null. Cant insert.");
		table.put(bean.getMessageId(), bean);
		return true;
	}

	public ResultSet find(String query) {
		throw new UnsupportedOperationException("selectRS() is not supported");
	}

	public Collection find(RetransmitterBean bean) {
		ArrayList beans = new ArrayList();
		Iterator iterator = table.values().iterator();

		RetransmitterBean temp;
		while (iterator.hasNext()) {
			
			temp = (RetransmitterBean) iterator.next();

			boolean add = true;

			if (bean.getKey() != null && !bean.getKey().equals(temp.getKey()))
				add = false;

			if (bean.getTimeToSend() > 0
					&& bean.getTimeToSend() != temp.getTimeToSend())
				add = false;

			if (bean.getMessageId() != null
					&& !bean.getMessageId().equals(temp.getMessageId()))
				add = false;

			if (bean.getTempSequenceId() != null
					&& !bean.getTempSequenceId().equals(
							temp.getTempSequenceId()))
				add = false;

			if (bean.getMessageNumber() > 0
					&& bean.getMessageNumber() != temp.getMessageNumber())
				add = false;

			if (bean.getMessagetype()!=Constants.MessageTypes.UNKNOWN && bean.getMessagetype()!=temp.getMessagetype())
				add = false;
			
			if (add)
				beans.add(temp);
		}

		return beans;
	}

	public Collection findMsgsToSend() {
		ArrayList beans = new ArrayList();
		Iterator iterator = table.values().iterator();

		RetransmitterBean temp;
		while (iterator.hasNext()) {
			temp = (RetransmitterBean) iterator.next();
			if (temp.isSend()) {
				
				long timeToSend = temp.getTimeToSend();
				
				int count = temp.getSentCount();
				
				long timeNow = System.currentTimeMillis();
				if (count == 0
						|| (timeNow >= timeToSend)) {
					beans.add(temp);
				}
			}
		}

		//temp hack for microsoft
		sort (beans);
		
		return sort(beans);
	}
	
	private ArrayList sort (ArrayList beans) {
		ArrayList newBeans = new ArrayList ();
		HashMap tempHash = new HashMap ();
		
		Iterator iter = beans.iterator();
		while (iter.hasNext()){
			RetransmitterBean bean = (RetransmitterBean) iter.next();
			if (bean.getMessageNumber()>0)
				tempHash.put(new Long (bean.getMessageNumber()),bean);
			else
				newBeans.add(bean);
		}
		
		long tempNo = 1;
		RetransmitterBean tempBean = (RetransmitterBean) tempHash.get(new Long (tempNo));
		while (tempBean!=null) {
			newBeans.add(tempBean);
			tempNo++;
			tempBean = (RetransmitterBean) tempHash.get(new Long (tempNo));
		}
		

		return newBeans;
	}

	public boolean update(RetransmitterBean bean) {
		if (!table.contains(bean))
			return false;

		return table.put(bean.getMessageId(), bean) != null;
	}

}