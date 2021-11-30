package com.mybank;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.StaxDriver;

/**
 * {@link BankAccountXStreamSerializer} serializes objects to arrays of bytes
 * representing XML trees using the XStream library.
 * 
 * @see BankAccountSerializer
 */
public final class BankAccountXStreamSerializer implements BankAccountSerializer {

	/** The XML stream. */
	private final XStream xmlStream = new XStream(new StaxDriver());

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.acertainbookstore.interfaces.BookStoreSerializer#serialize(java.lang.
	 * Object)
	 */
	public byte[] serialize(Object object) {
		String xml = xmlStream.toXML(object);
		return xml.getBytes();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.acertainbookstore.interfaces.BookStoreSerializer#deserialize(byte[])
	 */
	public Object deserialize(byte[] bytes) {
		String xml = new String(bytes);
		return xmlStream.fromXML(xml);
	}
}
