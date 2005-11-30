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
package org.apache.sandesha2.storage.beans;

/**
 * This bean is used to store properties of a certain sequence.
 * Used by both sending and receiving sides.
 * 
 * @author Chamikara Jayalath <chamikaramj@gmail.com>
 * @author Sanka Samaranayaka <ssanka@gmail.com>
 */

public class SequencePropertyBean implements RMBean {

	/**
	 * Comment for <code>sequenceId</code>
	 * Sequence ID of the sequence this property belong to.
	 */
	private String sequenceId;

	/**
	 * Comment for <code>name</code>
	 * The name of the property. Possible names are given in the Sandesha2Constants.SequenceProperties interface.
	 */
	private String name;

	/**
	 * Comment for <code>value</code>
	 * The value of the property.
	 */
	private Object value;

	public SequencePropertyBean(String seqId, String propertyName, Object value) {
		this.sequenceId = seqId;
		this.name = propertyName;
		this.value = value;
	}

	public SequencePropertyBean() {

	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getSequenceId() {
		return sequenceId;
	}

	public void setSequenceId(String sequenceId) {
		this.sequenceId = sequenceId;
	}

	public Object getValue() {
		return value;
	}

	public void setValue(Object value) {
		this.value = value;
	}
}