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

import org.jivesoftware.smack.Connection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.PrivateDataManager;

import java.util.*;

public class NoteManager {
    private static final Map<Connection, NoteManager> noteManagerMap = new HashMap<Connection, NoteManager>();
    static {
        PrivateDataManager.addPrivateDataProvider("storage", Notes.NAMESPACE,
                new Notes.Provider());
    }

    public synchronized static NoteManager getNoteManager(Connection connection)
            throws XMPPException
    {
        NoteManager manager = noteManagerMap.get(connection);
        if(manager == null) {
            manager = new NoteManager(connection);
            noteManagerMap.put(connection, manager);
        }
        return manager;
    }

    private PrivateDataManager privateDataManager;
    private Notes notes;

    private NoteManager(Connection connection) throws XMPPException {
        if(connection == null || !connection.isAuthenticated()) {
            throw new XMPPException("Invalid connection.");
        }
        this.privateDataManager = new PrivateDataManager(connection);
    }

    public Collection<Note> getNotes() throws XMPPException {
        Notes notes = retrieveNotes();
        return Collections.unmodifiableCollection(notes.getNotes());
    }

    public void addNote(Note note) throws XMPPException {
        notes.addNote(note);
        privateDataManager.setPrivateData(notes);
    }

    public void removeNote(Note note) throws XMPPException {
        Notes notes = retrieveNotes();
        Iterator<Note> it = notes.getNotes().iterator();
        while(it.hasNext()) {
            Note n = it.next();
            if(note == n) {
                it.remove();
                privateDataManager.setPrivateData(notes);
                return;
            }
        }
    }

    private Notes retrieveNotes() throws XMPPException {
        if (notes == null) {
            notes = (Notes) privateDataManager.getPrivateData("storage", Notes.NAMESPACE);
        }
        return notes;
    }

    public void updateNotes() throws XMPPException {
        notes = (Notes) privateDataManager.getPrivateData("storage", Notes.NAMESPACE);
    }
}
