package org.jivesoftware.smackx.carbons;

import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smack.provider.PacketExtensionProvider;
import org.xmlpull.v1.XmlPullParser;

public class Disable implements PacketExtension {
    public static String XMLNS = "urn:xmpp:carbons:2";

    public Disable() { }

    public String toXML() { return "<disable xmlns=\"" + XMLNS + "\"/>"; }
    
	public String getElementName() {
		return "disable";
	}

	public String getNamespace() {
		return XMLNS;
	}

    public static class Provider implements PacketExtensionProvider {

        public PacketExtension parseExtension(XmlPullParser parser) throws Exception {
            return new Disable();
        }
    }
}
