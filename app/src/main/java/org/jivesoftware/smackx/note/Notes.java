/**
 *
 * Copyright 2014 Igor Ustyugov <igor@ustyugov.net>.
 *
 * All rights reserved. Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jivesoftware.smackx.note;

import org.jivesoftware.smackx.packet.PrivateData;
import org.jivesoftware.smackx.provider.PrivateDataProvider;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Notes implements PrivateData {
    public static String NAMESPACE = "http://miranda-im.org/storage#notes";

    private List<Note> notes;

    public Notes() { notes = new ArrayList<Note>(); }

    public void addNote(Note note) { notes.add(note); }
    public void removeNote(Note note) { notes.remove(note); }
    public void clearNotes() { notes.clear(); }
    public List<Note> getNotes() {
        return notes;
    }
    public String getElementName() { return "storage"; }
    public String getNamespace() { return NAMESPACE; }

    public String toXML() {
        StringBuilder buf = new StringBuilder();
        buf.append("<" + getElementName() + " xmlns=\"" + getNamespace() + "\">");

        final Iterator<Note> notes = getNotes().iterator();
        while (notes.hasNext()) {
            Note note = notes.next();
            buf.append("<note tags=\"").append(note.getTag()).append("\">");
            buf.append("<title>").append(note.getTittle()).append("</title>");
            buf.append("<text>").append(note.getText()).append("</text>");
            buf.append("</note>");
        }
        buf.append("</" + getElementName() + ">");
        return buf.toString();
    }

    public static class Provider implements PrivateDataProvider {
        public Provider() { super(); }

        public PrivateData parsePrivateData(XmlPullParser parser) throws Exception {
            Notes storage = new Notes();

            boolean done = false;
            while (!done) {
                int eventType = parser.next();
                if (eventType == XmlPullParser.START_TAG && "note".equals(parser.getName())) {
                    final Note noteStorage = getNoteStorage(parser);
                    if (noteStorage != null) {
                        storage.addNote(noteStorage);
                    }
                }
                else if (eventType == XmlPullParser.END_TAG && "storage".equals(parser.getName())) {
                    done = true;
                }
            }

            return storage;
        }
    }

    private static Note getNoteStorage(XmlPullParser parser) throws IOException, XmlPullParserException {
        String tag = parser.getAttributeValue("", "tags");
        String title = "";
        String text = "";

        boolean done = false;
        while (!done) {
            int eventType = parser.next();
            if (eventType == XmlPullParser.START_TAG && "title".equals(parser.getName())) {
                title = parser.nextText();
            }
            else if (eventType == XmlPullParser.START_TAG && "text".equals(parser.getName())) {
                text = parser.nextText();
            }
            else if (eventType == XmlPullParser.END_TAG && "note".equals(parser.getName())) {
                done = true;
            }
        }

        Note note = new Note(title, text, tag);
        return note;
    }
}
