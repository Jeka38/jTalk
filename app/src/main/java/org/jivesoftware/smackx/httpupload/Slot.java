package org.jivesoftware.smackx.httpupload;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.provider.IQProvider;
import org.xmlpull.v1.XmlPullParser;

import java.util.ArrayList;
import java.util.List;

public class Slot extends IQ {
    public static final String XMLNS = "urn:xmpp:http:upload";

    private String putUrl;
    private String getUrl;
    private List<Header> headers = new ArrayList<>();

    public Slot(String putUrl, String getUrl, List<Header> headers) {
        this.putUrl = putUrl;
        this.getUrl = getUrl;
        this.headers = headers;
        setType(Type.RESULT);
    }

    public String getElementName() {
        return "slot";
    }

    public String getNamespace() {
        return XMLNS;
    }

    @Override
    public String getChildElementXML() {
        String xml = "<slot xmlns=\"" + XMLNS + "\">";
        if (putUrl != null && !putUrl.isEmpty()) {
            xml += "<put>" + putUrl + "</put>";
        }
        if (getUrl != null && !getUrl.isEmpty()) {
            xml += "<get>"+getUrl+"</get>";
        }
        xml += "</slot>";
        return xml;
    }

    public String getPutUrl() { return putUrl; }

    public String getGetUrl() { return getUrl; }

    public List<Header> getHeaders() { return headers; }

    public static class SlotIQProvider implements IQProvider {

        @Override
        public IQ parseIQ(XmlPullParser parser) throws Exception {
            String putUrl = null;
            String getUrl = null;
            List<Header> headers = new ArrayList<>();

            boolean done = false;
            while (!done) {
                switch (parser.getEventType()) {
                    case XmlPullParser.START_TAG:
                        if (parser.getName().equals("put")) {
                            putUrl = parser.nextText();
                        } else if (parser.getName().equals("get")) {
                            getUrl = parser.nextText();
                        } else if (parser.getName().equals("header")) {
                            headers.add(new Header(parser.getAttributeValue("", "name"), parser.nextText()));
                        }
                        break;
                    case XmlPullParser.END_TAG:
                        if (parser.getName().equals("slot")) {
                            done = true;
                        }
                        break;
                    default:
                        break;
                }
                parser.next();
            }

            return new Slot(putUrl, getUrl, headers);
        }
    }

}
