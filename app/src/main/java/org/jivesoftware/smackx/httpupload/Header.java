package org.jivesoftware.smackx.httpupload;

public class Header {
    private String name;
    private String value;

    public Header(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    public String toXml() {
        return "<header name=\"" + name + "\">" + value + "</header>";
    }
}
