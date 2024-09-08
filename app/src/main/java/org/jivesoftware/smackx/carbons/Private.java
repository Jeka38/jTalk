package org.jivesoftware.smackx.carbons;

import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smack.provider.PacketExtensionProvider;
import org.xmlpull.v1.XmlPullParser;

public class Private implements PacketExtension {
    public static String XMLNS = "urn:xmpp:carbons:2";

    public Private() { }

    public String toXML() { return "<private xmlns=\"" + XMLNS + "\"/>"; }
    
	public String getElementName() {
		return "private";
	}

	public String getNamespace() {
		return XMLNS;
	}

    public static class Provider implements PacketExtensionProvider {

        public PacketExtension parseExtension(XmlPullParser parser) throws Exception {
            return new Private();
        }
    }
}
