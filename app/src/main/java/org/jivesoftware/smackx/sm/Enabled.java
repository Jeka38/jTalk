package org.jivesoftware.smackx.sm;

import org.jivesoftware.smack.packet.Packet;

public class Enabled extends Packet {
    public static String XMLNS = "urn:xmpp:sm:3";
    private int max = 0;
    private boolean resume = false;
    private String id;
    private String location;

    public Enabled() { }

    public String toXML() {
        StringBuilder sb = new StringBuilder("<");
        sb.append(getElementName());
        sb.append(" xmlns=\"").append(getNamespace()).append("\"");
        if (getId() != null) sb.append(" id=\"").append(getId()).append("\"");
        if (getLocation() != null) sb.append(" location=\"").append(getLocation()).append("\"");
        if (getMax() > 0) sb.append(" max=\"").append(getMax()).append("\"");
        if (getResume()) sb.append(" resume=\"").append(getResume()).append("\"");
        sb.append("/>");
        return sb.toString();
    }

    public String getElementName() {
        return "enabled";
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

    public String getId() {
        return this.id;
    }

    public String getLocation() {
        return this.location;
    }

    public void setMax(int max) {
        this.max = max;
    }

    public void setResume(boolean resume) {
        this.resume = resume;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setLocation(String location) {
        this.location = location;
    }
}
