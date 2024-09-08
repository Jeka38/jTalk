package org.jivesoftware.smackx.packet;

import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smack.provider.PacketExtensionProvider;
import org.xmlpull.v1.XmlPullParser;

public class Forwarded implements PacketExtension {
    public static String XMLNS = "urn:xmpp:forward:0";
    private Message msg;

    public Forwarded(Message msg) { this.msg = msg; }

    public String toXML() {
        String xml = "<forwarded xmlns=\"" + XMLNS + "\">";
        xml += msg.toXML();
        xml += "</forwarded>";
        return xml;
    }

    public Message getMessage() {
        return msg;
    }
    
	public String getElementName() {
		return "forwarded";
	}

	public String getNamespace() {
		return XMLNS;
	}

    public static class Provider implements PacketExtensionProvider {

        public PacketExtension parseExtension(XmlPullParser parser) throws Exception {
            Message msg = new Message();

            boolean done = false;
            while (!done) {
                switch (parser.getEventType()) {
                    case XmlPullParser.START_TAG:
                        if (parser.getName().equals("message")) {
                            for (int i = 0; i < parser.getAttributeCount(); i++) {

                                if (parser.getAttributeName(i).equals("from")) {
                                    msg.setFrom(parser.getAttributeValue(i));
                                } else if (parser.getAttributeName(i).equals("to")) {
                                    msg.setTo(parser.getAttributeValue(i));
                                } else if (parser.getAttributeName(i).equals("type")) {
                                    msg.setType(Message.Type.fromString(parser.getAttributeValue(i)));
                                }
                            }
                        } else if (parser.getName().equals("body")) {
                            msg.setBody(parser.nextText());
                        } else if (parser.getName().equals("x")) {
                            msg.addExtension(new MUCUser());
                        }
                        break;
                    case XmlPullParser.END_TAG:
                        if (parser.getName().equals("forwarded")) {
                            done = true;
                        }
                        break;
                    default:
                        break;
                }
                parser.next();
            }

            return new Forwarded(msg);
        }
    }
}
