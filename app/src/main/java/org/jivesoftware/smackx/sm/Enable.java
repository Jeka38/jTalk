package org.jivesoftware.smackx.sm;

import org.jivesoftware.smack.packet.Packet;

public class Enable extends Packet {
    public static String XMLNS = "urn:xmpp:sm:3";
    private int max = 0;
    private boolean resume = false;

    public Enable() { }

    public String toXML() {
        StringBuilder sb = new StringBuilder("<");
        sb.append(getElementName());
        sb.append(" xmlns=\"").append(getNamespace()).append("\"");
        if (getMax() > 0) sb.append(" max=\"").append(getMax()).append("\"");
        if (getResume()) sb.append(" resume=\"").append(getResume()).append("\"");
        sb.append("/>");
        return sb.toString();
    }

    public String getElementName() {
        return "enable";
    }

    public String getNamespace() {
        return XMLNS;
    }

    public int getMax() {
        return this.max;
    }

    public boolean getResume() {
        return this.resume;
    }

    public void setMax(int max) {
        this.max = max;
    }

    public void setResume(boolean resume) {
        this.resume = resume;
    }

}
