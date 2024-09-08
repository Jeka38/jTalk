package org.jivesoftware.smackx.sm;

import org.jivesoftware.smack.packet.Packet;

public class Resumed extends Packet {
    public static String XMLNS = "urn:xmpp:sm:3";
    private int h = 0;
    private String previd;

    public Resumed() { }

    public String toXML() {
        StringBuilder sb = new StringBuilder("<");
        sb.append(getElementName());
        sb.append(" xmlns=\"").append(getNamespace()).append("\"");
        if (getH() > 0) sb.append(" h=\"").append(getH()).append("\"");
        if (getPrevid() != null) sb.append(" previd=\"").append(getPrevid()).append("\"");
        sb.append("/>");
        return sb.toString();
    }

    public String getElementName() {
        return "resumed";
    }

    public String getNamespace() {
        return XMLNS;
    }

    public int getH() {
        return this.h;
    }

    public String getPrevid() {
        return this.previd;
    }

    public void setH(int h) {
        this.h = h;
    }

    public void setPrevid(String previd) {
        this.previd = previd;
    }
}
