package org.jivesoftware.smackx.packet;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.provider.IQProvider;
import org.xmlpull.v1.XmlPullParser;

public class Ping extends IQ {

    public Ping() {
        setType(IQ.Type.GET);
    }

    @Override
    public String getChildElementXML() {
        return "<ping xmlns=\"urn:xmpp:ping\" />";
    }

    public static class Provider implements IQProvider {
        @Override
        public IQ parseIQ(XmlPullParser parser) throws Exception {
            return new Ping();
        }
    }
}
