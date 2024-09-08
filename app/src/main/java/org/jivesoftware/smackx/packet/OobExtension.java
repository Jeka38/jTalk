package org.jivesoftware.smackx.packet;

import org.jivesoftware.smack.packet.PacketExtension;

public class OobExtension implements PacketExtension {
    private String url;

    public OobExtension(String url) { this.url = url; }

    public String getElementName() { return "x"; }

    public String getNamespace() { return "jabber:x:oob"; }

    public String toXML() {
        return "<" + getElementName() + " xmlns=\"" + getNamespace() + "\"><url>" + url + "</url></" + getElementName() + ">";
    }
}
