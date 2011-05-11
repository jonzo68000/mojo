/*
 * Chronos - Copyright (C) 2011 National Board of e-Health (NSI), Denmark (http://www.nsi.dk)
 *
 * All source code and information supplied as part of Chronos is
 * copyright to National Board of e-Health.
 *
 * The source code has been released under a dual license - meaning you can
 * use either licensed version of the library with your code.
 *
 * It is released under the LGPL (GNU Lesser General Public License), either
 * version 2.1 of the License, or (at your option) any later version. A copy
 * of which can be found at the link below.
 * http://www.gnu.org/copyleft/lesser.html
 *
 * $HeadURL$
 * $Id$
 */
package org.codehaus.mojo.chronos.jmeter;

import java.util.Properties;

import org.codehaus.mojo.chronos.responsetime.ResponsetimeSample;
import org.jdom.Element;

/**
 * This class represents info from a jmeter logentry, in a jtl2.0 format.
 * 
 * @author ksr@lakeside.dk
 */
public class Jtl20Sample extends ResponsetimeSample {

    private static final long serialVersionUID = -804458217898447540L;

    private static final String JUNIT_SAMPLER = "org.apache.jmeter.protocol.java.sampler.JUnitSampler";

    private final int responsetime;

    private final long timestamp;

    private final boolean success;

    private final String threadId;

    /**
     * @param attributes
     */
    public Jtl20Sample(Properties attributes) {
        responsetime = Integer.parseInt(attributes.getProperty("time"));
        timestamp = Long.parseLong(attributes.getProperty("timeStamp"));
        success = "true".equals(attributes.getProperty("success"));
        threadId = attributes.getProperty("threadName").intern();
    }

    private Jtl20Sample(int responsetime, long timestamp, boolean success, String threadId) {
        this.responsetime = responsetime;
        this.timestamp = timestamp;
        this.success = success;
        this.threadId = threadId;
    }

    /**
     * This will normally contain the name of this sample, as defined in JMeter.<br/>
     * If the report is generated by JMeter2.1, the name will be derived from an embedded property instead
     * 
     * @return the name of this sample
     */
    public static String getSampleName(Properties attributes, String embeddedPropertyValue) {
        // it seems like when generated from Jmeter 2.1, the label will always
        // contain the String
        // 'org.apache.jmeter.protocol.java.sampler.JUnitSampler'
        String label = attributes.getProperty("label");
        if(JUNIT_SAMPLER.equals(label) && !"".equals(embeddedPropertyValue)) {
            return embeddedPropertyValue;
        } else {
            return label;
        }
    }

    /**
     * @see ResponsetimeSample#getResponsetime()
     * @return Returns the responsetime.
     */
    public final int getResponsetime() {
        return responsetime;
    }

    /**
     * @see ResponsetimeSample#getTimestamp()
     * @return Returns the timestamp.
     */
    public final long getTimestamp() {
        return timestamp;
    }

    /**
     * @see ResponsetimeSample#isSuccess()
     * @return Returns the success.
     */
    public final boolean isSuccess() {
        return success;
    }

    /**
     * @see ResponsetimeSample#getThreadId()
     * @return Returns the threadgroupId.
     */
    public final String getThreadId() {
        return threadId;
    }

    public final Element toXML() {
        Element xml = new Element("jtl20sample");
        xml.setAttribute("responsetime", Integer.toString(responsetime));
        xml.setAttribute("timestamp", Long.toString(timestamp));
        xml.setAttribute("success", Boolean.toString(success));
        xml.setAttribute("threadId", threadId);
        return xml;
    }

    /**
     * Transforms the xml into a <code>Jtl20Sample</code> entity.
     * 
     * @param xml
     *            The xml to parse.
     * @return The corresponding <code>Jtl20Sample</code> instance.
     */
    public static Jtl20Sample fromXML(Element xml) {
        int responsetime = Integer.parseInt(xml.getAttributeValue("responsetime"));
        long timestamp = Long.parseLong(xml.getAttributeValue("timestamp"));
        boolean success = Boolean.parseBoolean(xml.getAttributeValue("success"));
        String threadId = xml.getAttributeValue("threadId");
        
        return new Jtl20Sample(responsetime, timestamp, success, threadId);
    }
}