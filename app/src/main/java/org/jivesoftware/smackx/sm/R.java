package org.jivesoftware.smackx.sm;

import org.jivesoftware.smack.packet.Packet;

public class R extends Packet {
    public static String XMLNS = "urn:xmpp:sm:3";

    public R() { }

    public String toXML() {
        return "<" + getElementName() + " xmlns=\"" + getNamespace() + "\"/>";
    }

    public String getElementName() {
        return "r";
    }

    public String getNamespace() {
        return XMLNS;
    }

}
