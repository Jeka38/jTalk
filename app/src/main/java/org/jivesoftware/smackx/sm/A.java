package org.jivesoftware.smackx.sm;

import org.jivesoftware.smack.packet.Packet;

public class A extends Packet {
    public static String XMLNS = "urn:xmpp:sm:3";
    private int h = 0;

    public A() { }

    public String toXML() {
        StringBuilder sb = new StringBuilder("<");
        sb.append(getElementName());
        sb.append(" xmlns=\"").append(getNamespace()).append("\"");
        if (getH() > 0) sb.append(" h=\"").append(getH()).append("\"");
        sb.append("/>");
        return sb.toString();
    }

    public String getElementName() {
        return "a";
    }

    public String getNamespace() {
        return XMLNS;
    }

    public int getH() {
        return this.h;
    }

    public void setH(int h) {
        this.h = h;
    }
}
