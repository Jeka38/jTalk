/*
 * Copyright (C) 2012, Igor Ustyugov <igor@ustyugov.net>
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/
 */

package net.ustyugov.jtalk.service;

import java.io.File;
import java.io.FileOutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.graphics.Color;
import android.os.Build;

import net.ustyugov.jtalk.AutoAwayStatus;
import net.ustyugov.jtalk.AutoXaStatus;
import net.ustyugov.jtalk.Avatars;
import net.ustyugov.jtalk.Conference;
import net.ustyugov.jtalk.Constants;
import net.ustyugov.jtalk.IconPicker;
import net.ustyugov.jtalk.IgnoreList;
import net.ustyugov.jtalk.MessageItem;
import net.ustyugov.jtalk.MessageLog;
import net.ustyugov.jtalk.Notify;
import net.ustyugov.jtalk.PingTask;
import net.ustyugov.jtalk.activity.Chat;
import net.ustyugov.jtalk.activity.RosterActivity;
import net.ustyugov.jtalk.db.AccountDbHelper;
import net.ustyugov.jtalk.db.JTalkProvider;
import net.ustyugov.jtalk.db.MessageDbHelper;
import net.ustyugov.jtalk.listener.ConListener;
import net.ustyugov.jtalk.listener.IncomingFileListener;
import net.ustyugov.jtalk.listener.InviteListener;
import net.ustyugov.jtalk.listener.MsgListener;
import net.ustyugov.jtalk.listener.MucParticipantStatusListener;
import net.ustyugov.jtalk.listener.PingListener;
import net.ustyugov.jtalk.listener.RstListener;
import net.ustyugov.jtalk.listener.SMListener;
import net.ustyugov.jtalk.listener.XmlListener;
import net.ustyugov.jtalk.receivers.ChangeConnectionReceiver;
import net.ustyugov.jtalk.receivers.ScreenStateReceiver;
import net.ustyugov.jtalk.smiles.Smiles;

import org.jivesoftware.smack.*;
import org.jivesoftware.smack.ConnectionConfiguration.SecurityMode;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.filter.PacketTypeFilter;
import org.jivesoftware.smack.packet.*;
import org.jivesoftware.smack.provider.PrivacyProvider;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.*;
import org.jivesoftware.smackx.bookmark.BookmarkManager;
import org.jivesoftware.smackx.bookmark.BookmarkedConference;
import org.jivesoftware.smackx.carbons.*;
import org.jivesoftware.smackx.carbons.Enable;
import org.jivesoftware.smackx.commands.AdHocCommandManager;
import org.jivesoftware.smackx.filetransfer.FileTransferManager;
import org.jivesoftware.smackx.filetransfer.FileTransferRequest;
import org.jivesoftware.smackx.httpupload.Request;
import org.jivesoftware.smackx.httpupload.Slot;
import org.jivesoftware.smackx.muc.DiscussionHistory;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smackx.note.Notes;
import org.jivesoftware.smackx.packet.*;
import org.jivesoftware.smackx.provider.*;
import org.jivesoftware.smackx.search.UserSearch;

import org.xbill.DNS.Credibility;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.Record;
import org.xbill.DNS.SRVRecord;
import org.xbill.DNS.Type;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.jtalk2.R;
 
public class JTalkService extends Service {
    private boolean started = false;
    private boolean connecting = false;
	private static JTalkService js = new JTalkService();
    private Smiles smiles;
    private List<String> collapsedGroups = new ArrayList<>();
    private Hashtable<String, List<String>> activeChats = new Hashtable<>();
    private Hashtable<String, Integer> msgCounter = new Hashtable<>();
    private List<MessageItem> unreadMessages = new ArrayList<>();
    private Hashtable<String, List<String>> mucHighlightsList = new Hashtable<>();
    private Hashtable<String, String> passHash = new Hashtable<>();
    private Hashtable<String, String> textHash = new Hashtable<>();
    private Hashtable<String, String> stateHash = new Hashtable<>();
    private Hashtable<String, Hashtable<String, String>> resourceHash = new Hashtable<>();
    private Hashtable<String, Integer> positionHash = new Hashtable<>();
    private Hashtable<String, Hashtable> joinedConferences = new Hashtable<>();
    private Hashtable<String, Hashtable<String, MultiUserChat>> conferences = new Hashtable<>();
    private Hashtable<String, Bitmap> avatarsHash = new Hashtable<>();
    private Hashtable<String, Hashtable<String, Integer>> messagesCount = new Hashtable<>();
    private Hashtable<String, DataForm> formHash = new Hashtable<>();
    private Hashtable<String, XMPPConnection> connections = new Hashtable<>();
    private Hashtable<String, VCard> vcards = new Hashtable<>();
    private Hashtable<String, ConListener> conListeners = new Hashtable<>();
    private Hashtable<String, ConnectionTask> connectionTasks = new Hashtable<>();
    private Hashtable<String, Timer> pingTimers = new Hashtable<>();
    private Hashtable<String, XmlListener> XmlListeners = new Hashtable<>();
    private Hashtable<String, SMListener> SMListeners = new Hashtable<>();
    private String currentJid = "me";
    private String globalState = "";
    private SharedPreferences prefs;
//    private BroadcastReceiver updateReceiver;
    private ScreenStateReceiver screenStateReceiver;
    private ChangeConnectionReceiver connectionReceiver;
    private Hashtable<String, FileTransferManager> fileTransferManagers = new Hashtable<>();
    private List<FileTransferRequest> incomingRequests = new ArrayList<>();
    private Timer autoStatusTimer = new Timer();
    private boolean autoStatus = false;
    private Presence oldPresence;

    private WifiManager.WifiLock wifiLock;
    
    private IconPicker iconPicker;

    private Hashtable<String, Hashtable<String, List<MessageItem>>> messages = new Hashtable<>();

    public static JTalkService getInstance() { return js; }

    public Smiles getSmiles(Activity activity) {
        if (smiles != null) return smiles;
        else return new Smiles(activity);
    }

    public void removeSmiles() { smiles = null; }

    public void addPassword(String account, String password) {
        passHash.put(account, password);
    }

    public List<MessageItem> getMessageList(String account, String jid) {
        Hashtable<String, List<MessageItem>> hash = new Hashtable<>();
        if (messages.containsKey(account)) hash = messages.get(account);

        List<MessageItem> list = new ArrayList<>();
        if (hash != null && hash.containsKey(jid)) list = hash.get(jid);

        if (list.isEmpty()) {
            String[] selectionArgs = {jid};
            Cursor cursor = getContentResolver().query(JTalkProvider.CONTENT_URI, null, "jid = ?", selectionArgs, "_id DESC, stamp DESC LIMIT " + Constants.LOAD_MESSAGES_COUNT);
            if (cursor != null && cursor.getCount() > 0) {
                cursor.moveToFirst();

                do {
                    String baseId = cursor.getString(cursor.getColumnIndex(MessageDbHelper._ID));
                    String id = cursor.getString(cursor.getColumnIndex(MessageDbHelper.ID));
                    String nick = cursor.getString(cursor.getColumnIndex(MessageDbHelper.NICK));
                    String type = cursor.getString(cursor.getColumnIndex(MessageDbHelper.TYPE));
                    String stamp = cursor.getString(cursor.getColumnIndex(MessageDbHelper.STAMP));
                    String body = cursor.getString(cursor.getColumnIndex(MessageDbHelper.BODY));
                    boolean received = Boolean.valueOf(cursor.getString(cursor.getColumnIndex(MessageDbHelper.RECEIVED)));

                    MessageItem item = new MessageItem(account, jid, id);
                    item.setBaseId(baseId);
                    item.setName(nick);
                    item.setType(MessageItem.Type.valueOf(type));
                    item.setTime(stamp);
                    item.setBody(body);
                    item.setReceived(received);

                    if (!list.contains(item)) list.add(0, item);
                } while (cursor.moveToNext());

                setMessageList(account, jid, list);
                cursor.close();
            }

            return list;
        } else return list;
    }

    public void setMessageList(String account, String jid, List<MessageItem> list) {
        Hashtable<String, List<MessageItem>> hash = new Hashtable<>();
        if (messages.containsKey(account)) hash = messages.get(account);

        if (hash != null) {
            hash.put(jid, list);
            messages.put(account, hash);
        }
    }

    public XmlListener getXmlListener(String account) {
        if (XmlListeners.containsKey(account)) {
            return XmlListeners.get(account);
        } else return null;
    }

    public SMListener getSMListener(String account) {
        if (SMListeners.containsKey(account)) {
            return SMListeners.get(account);
        } else return null;
    }
    
    private void removeConnectionListener(String account) {
    	if (conListeners.containsKey(account)) {
    		ConListener listener = conListeners.remove(account);
    		XMPPConnection connection = getConnection(account);
    		if (connection != null) connection.removeConnectionListener(listener);
    	}
    }
    
    private void addConnectionListener(String account, XMPPConnection connection) {
        if (!conListeners.containsKey(account)) {
            ConListener listener = new ConListener(this, account);
            connection.addConnectionListener(listener);
            conListeners.put(account, listener);
        }
    }

    public ConListener getConnectionListener(String account) {
        if (conListeners.containsKey(account)) return conListeners.get(account);
        else return null;
    }
    
    public Collection<XMPPConnection> getAllConnections() {
    	return connections.values();
    }

    public void setState(String account, String state) {
        if (state == null) state = "null";
        if (account != null) stateHash.put(account, state);
    }

    public String getState(String account) {
        if (stateHash.containsKey(account)) return stateHash.get(account);
        else {
            if (isAuthenticated(account)) {
                return getStatus(account, account);
            } else {
                try {
                    return getString(R.string.Disconnect);
                } catch (Exception e) {
                    return "";
                }
            }
        }
    }

    public void addConnection(String account, XMPPConnection connection) {
        connections.put(account, connection);
    }

    public XMPPConnection getConnection(String account) {
    	if (account != null && connections.containsKey(account)) return connections.get(account);
    	else return null;
    }

    public Intent getNextChat() {
        Enumeration<String> accounts = mucHighlightsList.keys();
        while (accounts.hasMoreElements()) {
            String account = accounts.nextElement();
            Enumeration<String> jids = messagesCount.get(account).keys();
            if (jids.hasMoreElements()) {
                Intent intent = new Intent(this, Chat.class);
                intent.putExtra("account", account);
                intent.putExtra("jid", jids.nextElement());
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                return intent;
            }
        }

        accounts = messagesCount.keys();
        while (accounts.hasMoreElements()) {
            String account = accounts.nextElement();
            Enumeration<String> jids = messagesCount.get(account).keys();
            if (jids.hasMoreElements()) {
                Intent intent = new Intent(this, Chat.class);
                intent.putExtra("account", account);
                intent.putExtra("jid", jids.nextElement());
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                return intent;
            }
        }
        return null;
    }

    public int getMessagesCount() {
        int result = 0;
        for (Hashtable<String, Integer> hash : messagesCount.values()) {
            for (Integer i : hash.values()) {
                result = result + i;
            }
        }
        return result;
    }

    public int getMessagesCount(String jid) {
    	int result = 0;
    	for (Hashtable<String, Integer> hash : messagesCount.values()) {
            if (hash.containsKey(jid)) hash.remove(jid);
            for (Integer i : hash.values()) {
                result = result + i;
            }
    	}
    	return result;
    }
    
    public int getMessagesCount(String account, String jid) {
    	if (messagesCount.containsKey(account)) {
    		Hashtable<String, Integer> hash = messagesCount.get(account);
    		if (hash.containsKey(jid)) return hash.get(jid);
    	}
    	return 0;
    }
    
    public void addMessagesCount(String account, String jid) {
    	Hashtable<String, Integer> hash = new Hashtable<String, Integer>();
    	if (messagesCount.containsKey(account)) {
    		hash = messagesCount.get(account);
    		hash.put(jid, getMessagesCount(account, jid) + 1);
    	} else {
    		hash.put(jid, 1);
    	}
    	messagesCount.put(account, hash);
    }
    
    public void removeMessagesCount(String account, String jid) {
    	if (messagesCount.containsKey(account)) {
    		Hashtable<String, Integer> hash = messagesCount.get(account);
    		if (hash.containsKey(jid)) hash.remove(jid);
    	}
    }

    public void removeMessagesCountForJid(String account, String jid) {
        if (messagesCount.containsKey(account)) {
            Hashtable<String, Integer> hash = messagesCount.get(account);
            Enumeration<String> keys = hash.keys();
            while (keys.hasMoreElements()) {
                String key = keys.nextElement();
                if (key.startsWith(jid)) {
                    hash.remove(key);
                }
            }
        }
    }

    public void addDataForm(String id, DataForm form) {
    	formHash.put(id, form);
    }
    
    public DataForm getDataForm(String id) {
        Log.e("GETFORM", "ID: " + id + "; " + formHash.containsKey(id));
    	if (formHash.containsKey(id)) return formHash.get(id);
    	else return null;
    }
    
    public void addLastPosition(String jid, int position) {
    	positionHash.put(jid, position);
    }
    
    public int getLastPosition(String jid) {
    	if (positionHash.containsKey(jid)) return positionHash.remove(jid);
    	else return -1;
    }
    
    public IconPicker getIconPicker() {
        if (iconPicker == null && started) {
            iconPicker = new IconPicker(this);
        }
        return iconPicker;
    }
    public void setAutoStatus(boolean auto) { this.autoStatus = auto; }
    public boolean getAutoStatus() { return autoStatus; }
    public void setOldPresence(Presence presence) { this.oldPresence = presence; }
    public Presence getOldPresence() { return oldPresence; }

    public FileTransferManager getFileTransferManager(String account) {
        if (fileTransferManagers.containsKey(account)) return fileTransferManagers.get(account);
        else return null;
    }

    public void addIncomingFileRequest(FileTransferRequest request) {
        incomingRequests.add(request);
    }
    public FileTransferRequest getIncomingRequest() {
        if (incomingRequests.size() > 0) return incomingRequests.remove(0);
        else return null;
    }

    public void setCurrentJid(String jid) { this.currentJid = jid; }
    public String getCurrentJid() { return currentJid; }
    public String getGlobalState() { return globalState; }
    public void setGlobalState(String s) { globalState = s; }
    public Roster getRoster(String account) {
    	if (connections != null && account!= null && connections.containsKey(account)) {
            XMPPConnection connection = connections.get(account);
            if (connection != null) return connection.getRoster();
            else return null;
        }
    	else return null;
    }
    public List<String> getCollapsedGroups() { return collapsedGroups; }

    public Hashtable<String, Hashtable<String, MultiUserChat>> getConferences() {return conferences;}
    public Hashtable<String, MultiUserChat> getConferencesHash(String account) { 
    	if (conferences.containsKey(account)) return conferences.get(account); 
    	else {
    		conferences.put(account, new Hashtable<String, MultiUserChat>());
    		return conferences.get(account);
    	}
    }
    public Hashtable<String, Conference> getJoinedConferences(String account) {
        if (joinedConferences.containsKey(account)) return joinedConferences.get(account);
        else {
            joinedConferences.put(account, new Hashtable<String, Conference>());
            return joinedConferences.get(account);
        }
    }
    public Hashtable<String, Bitmap> getAvatarsHash() { return avatarsHash; }

    public void addUnreadMessage(MessageItem item) {
        String account = item.getAccount();
        String jid = item.getJid();
        if (!getConferencesHash(account).containsKey(jid) && !getConferencesHash(account).containsKey(StringUtils.parseBareAddress(jid)))
            jid = StringUtils.parseBareAddress(jid);

        for (MessageItem message : unreadMessages) {
            if (message.getAccount().equals(account)) {
                String j = StringUtils.parseBareAddress(message.getJid());
                if (j.equals(jid)) return;
            }
        }

        unreadMessages.add(item);
    }

    public MessageItem getUnreadMessage() {
        if (!unreadMessages.isEmpty()) return unreadMessages.remove(0);
        else return null;
    }

    public void removeUnreadMesage(String account, String jid) {
        if (!getConferencesHash(account).containsKey(jid) && !getConferencesHash(account).containsKey(StringUtils.parseBareAddress(jid)))
            jid = StringUtils.parseBareAddress(jid);

        for (MessageItem i : unreadMessages) {
            if (i.getAccount().equals(account)) {
                String j = i.getJid();
                if (j.startsWith(jid)) {
                    unreadMessages.remove(i);
                    return;
                }
            }
        }
    }

    public List<MessageItem> getUnreadMessages() {
        return unreadMessages;
    }

    public boolean isHighlight() {
        Enumeration<String> accounts = mucHighlightsList.keys();
        while (accounts.hasMoreElements()) {
            if (!mucHighlightsList.get(accounts.nextElement()).isEmpty()) return true;
        }

        accounts = messagesCount.keys();
        while (accounts.hasMoreElements()) {
            String account = accounts.nextElement();
            Enumeration<String> jids = messagesCount.get(account).keys();
            if (jids.hasMoreElements()) {
                String jid = jids.nextElement();
                if (!joinedConferences.get(account).containsKey(jid)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isHighlight(String account, String jid) {
    	if (!mucHighlightsList.containsKey(account)) return false;
    	List<String> list = mucHighlightsList.get(account);
        return list.contains(jid);
    }
    
    public void removeHighlight(String account, String jid) { 
    	if (!mucHighlightsList.containsKey(account)) return;
    	List<String> list = mucHighlightsList.get(account);
    	if (list.contains(jid)) list.remove(jid);
    	mucHighlightsList.put(account, list);
    }
    
    public void addHighlight(String account, String jid) {
    	List<String> list = new ArrayList<String>();
    	if (mucHighlightsList.containsKey(account)) {
    		list = mucHighlightsList.get(account);
    		if (!list.contains(jid)) list.add(jid);
    	} else {
    		list.add(jid);
    	}
    	mucHighlightsList.put(account, list);
    }

    public void addActiveChat(String account, String jid) {
        if (getConferencesHash(account).containsKey(jid)) return;
        if (activeChats.containsKey(account)) {
            List<String> chats = activeChats.get(account);
            if (!chats.contains(jid)) chats.add(jid);
        }
        else {
            List<String> chats = new ArrayList<String>();
            chats.add(jid);
            activeChats.put(account, chats);
        }
    }

    public void removeActiveChat(String account, String jid) {
        if (activeChats.containsKey(account)) {
            List<String> chats = activeChats.remove(account);
            while(chats.contains(jid)) chats.remove(jid);
            activeChats.put(account, chats);
        }
    }

    public List<String> getActiveChats(String account) {
        if (activeChats.containsKey(account)) return activeChats.get(account);
        else return new ArrayList<>();
    }

    public List<String> getPrivateMessages(String account) {
        List<String> list = new ArrayList<>();
        for (String jid : getActiveChats(account)) {
            if (getConferencesHash(account).containsKey(StringUtils.parseBareAddress(jid)) && !getConferencesHash(account).containsKey(jid)) {
                list.add(jid);
            }
        }
        return list;
    }
    
    public void setResource(String account, String jid, String resource) {
    	Hashtable<String,String> hash = new Hashtable<>();
    	hash.put(jid, resource);
    	resourceHash.put(account, hash);
    }
    public String getResource(String account, String jid) {
    	if (resourceHash.containsKey(account)) {
    		Hashtable<String,String> hash = resourceHash.get(account);
    		if (hash.containsKey(jid)) return hash.get(jid);
    	}
    	return "";
    }
    public void setText(String jid, String text) {
        if (text != null) textHash.put(jid, text);
    }

    public String getText(String jid) {
    	if (textHash.containsKey(jid)) return textHash.get(jid);
    	else return "";
    }
    public VCard getVCard(String account) { 
    	if (vcards.containsKey(account)) return vcards.get(account);
    	else return null; 
    }
    
    public void setVCard(final String account, VCard vcard) { 
    	if (vcard != null) {
    		vcards.put(account, vcard);
        	final byte[] buffer = vcard.getAvatar();
    		if (buffer != null) {
    			new Thread() {
    				public void run() {
    					try {
    						File f = new File(Constants.PATH);
    						f.mkdirs();
    						FileOutputStream fos = new FileOutputStream(Constants.PATH + "/" + account);
    						fos.write(buffer);
    						fos.close();
    					} catch (Throwable ignored) { }
    				}
    			}.start();
    		}
    		Intent intent = new Intent(Constants.UPDATE);
    		sendBroadcast(intent);
    	}
    }
    
    public Presence getPresence(String account, String user) {
    	Presence unavailable = new Presence(Presence.Type.unavailable);
    	if (connections.containsKey(account)) {
        	if (!connections.get(account).isAuthenticated()) return unavailable;
        	
    		if (StringUtils.parseResource(user).length() > 0) {
    			String bareJid = StringUtils.parseBareAddress(user);
    			if (getConferencesHash(account).containsKey(bareJid)) {
    				Presence p = getConferencesHash(account).get(bareJid).getOccupantPresence(user);
    				if (p != null) return p;
    				else return unavailable;
    			} else {
    				Presence p = getRoster(account).getPresenceResource(user);
    				if (p != null) return p;
    				else return unavailable;
    			}
    		} else {
    	    	Iterator<Presence> it = getRoster(account).getPresences(user);
    	    	if(it.hasNext()) {
    	    		return it.next();
    	    	} else {
    	    		return unavailable;
    	    	}
    		}
    	}
		return unavailable;
    }
    
    public Presence.Type getType(String account, String user) {
    	if (connections.containsKey(account)) {
    		if (StringUtils.parseResource(user).length() > 0) {
    			String g = StringUtils.parseBareAddress(user);
    			if (getConferencesHash(account).containsKey(g)) {
    				Presence p = getConferencesHash(account).get(g).getOccupantPresence(user);
    				if (p != null) return p.getType();
    				else return Presence.Type.unavailable;
    			} else {
    				Presence p = getRoster(account).getPresenceResource(user);
    				if (p != null) return p.getType();
    				else return Presence.Type.unavailable;
    			}
    		} else {
    	    	Presence p = getRoster(account).getPresence(user);
    	    	if (p != null) return p.getType();
                else return Presence.Type.unavailable;
    		}
    	}
		return Presence.Type.unavailable;
    }
    
    public Presence.Mode getMode(String account, String user) {
    	if (connections.containsKey(account)) {
    		if (StringUtils.parseResource(user).length() > 0) {
    			String g = StringUtils.parseBareAddress(user);
    			if (getConferencesHash(account).containsKey(g)) {
    				Presence p = getConferencesHash(account).get(g).getOccupantPresence(user);
    				if (p != null) {
    					Presence.Mode m = p.getMode();
    					if (m == null) return Presence.Mode.available;
    					else return m;
    				}
    				else return Presence.Mode.available;
    			} else {
    				Presence p = getRoster(account).getPresenceResource(user);
    				if (p != null) {
    					Presence.Mode m = p.getMode();
    					if (m == null) return Presence.Mode.available;
    					else return m;
    				}
    				else return Presence.Mode.available;
    			}
    		} 
        	
        	Iterator<Presence> it = getRoster(account).getPresences(user);
        	if(it.hasNext()) {
        		Presence presence = it.next();
        		if (presence.getType() != Presence.Type.unavailable) return presence.getMode();
        		else return Presence.Mode.available;
        	}
    	}
    	return Presence.Mode.available;
    }
    
    public String getStatus(String account, String user) {
    	if (connections.containsKey(account)) {
    		if (StringUtils.parseResource(user).length() > 0) {
    			String g = StringUtils.parseBareAddress(user);
    			if (getConferencesHash(account).containsKey(g)) {
    				Presence p = getConferencesHash(account).get(g).getOccupantPresence(user);
    				if (p != null) {
    					String s = p.getStatus();
    					if (s == null) return "";
    					else return s;
    				}
    				else return "";
    			} else {
                    Roster roster = getRoster(account);
                    if (roster != null) {
                        Presence p = getRoster(account).getPresenceResource(user);
                        if (p != null) {
                            String s = p.getStatus();
                            if (s == null) return "";
                            else return s;
                        }
                        else return "";
                    } else return "";
    			}
    		}
        	
        	Roster roster = getRoster(account);
        	if (roster != null) {
        		Iterator<Presence> it = roster.getPresences(user);
            	while(it.hasNext()) {
            		Presence presence = it.next();
            		if (presence.getType() != Presence.Type.unavailable)
            			if (presence.getStatus() == null) return "";
            			else return presence.getStatus();
            	}
        	}
    	}
    	return "";
    }
    
    public String getNode(String account, String user) {
        Roster roster = getRoster(account);
        if (roster == null) return null;

    	if (connections.containsKey(account)) {
    		if (StringUtils.parseResource(user).length() > 0) {
    			String g = StringUtils.parseBareAddress(user);
    			if (getConferencesHash(account).containsKey(g)) {
    				Presence p = getConferencesHash(account).get(g).getOccupantPresence(user);
    				if (p != null) {
    					CapsExtension caps = (CapsExtension) p.getExtension(CapsExtension.NODE_NAME, CapsExtension.XMLNS);
    					if (caps != null) return caps.getNode();
    					else return null;
    				} else return null;
    			} else {
    				Presence p = roster.getPresenceResource(user);
    				if (p != null) {
    					CapsExtension caps = (CapsExtension) p.getExtension(CapsExtension.NODE_NAME, CapsExtension.XMLNS);
    					if (caps != null) return caps.getNode();
    					else return null;
    				}
    			}
    		} 
        	
        	List<String> list = new ArrayList<>();
        	Iterator<Presence> it = roster.getPresences(user);
        	while(it.hasNext()) {
        		Presence presence = it.next();
        		if (presence.getType() != Presence.Type.unavailable) {
        			CapsExtension caps = (CapsExtension) presence.getExtension(CapsExtension.NODE_NAME, CapsExtension.XMLNS);
    				if (caps != null) list.add(caps.getNode());
        		}
        	}
        	
        	if (!list.isEmpty()) return list.get(0);
        	else return null;
    	}
    	return null;
    }
    
    public void resetTimer() {
    	if (prefs != null) {
            if (prefs.getBoolean("AutoStatusOnDisplay", false)) return;

    		autoStatusTimer.purge();
            autoStatusTimer.cancel();
            if (autoStatus) {
            	autoStatus = false;
            	
            	Enumeration<String> e = connections.keys();
            	while(e.hasMoreElements()) {
            		sendPresence(e.nextElement(), oldPresence.getStatus(), oldPresence.getMode().name(), oldPresence.getPriority());
            	}
            }
            autoStatusTimer = new Timer();
            int delayAway = 10;
            int delayXa = 20;
            try {
            	delayAway = Integer.parseInt(prefs.getString("AutoStatusAway", "10"));
                delayXa = Integer.parseInt(prefs.getString("AutoStatusXa", "20"));
            } catch(NumberFormatException ignored) { }
            if (delayAway < 1) delayAway = 1;
            if (delayXa < delayAway) delayXa = delayAway + 1;
            autoStatusTimer.schedule(new AutoAwayStatus(), delayAway * 60000);
            autoStatusTimer.schedule(new AutoXaStatus(), delayXa * 60000);
    	}
    }
    
    @Override
    public void onCreate() {
    	configure();
    	js = this;
    	prefs = PreferenceManager.getDefaultSharedPreferences(this);
        iconPicker = new IconPicker(this);

        connectionReceiver = new ChangeConnectionReceiver();
        registerReceiver(connectionReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));

        screenStateReceiver = new ScreenStateReceiver();
        registerReceiver(new ScreenStateReceiver(), new IntentFilter(Intent.ACTION_SCREEN_ON));
        registerReceiver(new ScreenStateReceiver(), new IntentFilter(Intent.ACTION_SCREEN_OFF));

        Intent i = new Intent(this, RosterActivity.class);
        i.setAction(Intent.ACTION_MAIN);
        i.addCategory(Intent.CATEGORY_LAUNCHER);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, i, 0);

        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel(Constants.NOTIFICATION_CHANNEL, "jTalk Background Service", NotificationManager.IMPORTANCE_NONE);
            channel.setShowBadge(false);
            channel.enableLights(false);
            channel.enableVibration(false);
            channel.setLockscreenVisibility(Notification.VISIBILITY_SECRET);

            NotificationChannel channelMessage = new NotificationChannel(Constants.NOTIFICATION_CHANNEL_MESSAGE, getString(R.string.NotificationChannelMessages), NotificationManager.IMPORTANCE_HIGH);
            channelMessage.setShowBadge(false);
            channelMessage.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            channelMessage.setLightColor(Color.GREEN);
            channelMessage.enableLights(true);
            channelMessage.setVibrationPattern(new long[] {200, 200});
            channelMessage.enableVibration(true);

            NotificationChannel channelMucMessage = new NotificationChannel(Constants.NOTIFICATION_CHANNEL_GROUP_MESSAGE, getString(R.string.NotificationChannelGroupMessages), NotificationManager.IMPORTANCE_NONE);
            channelMucMessage.setShowBadge(false);
            channelMucMessage.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            channelMucMessage.setLightColor(Color.GREEN);
            channelMucMessage.enableLights(true);
            channelMucMessage.setVibrationPattern(new long[] {200, 200});
            channelMucMessage.enableVibration(true);

            NotificationChannel channelHighlightMessage = new NotificationChannel(Constants.NOTIFICATION_CHANNEL_HIGHLIGHT_MESSAGE, getString(R.string.NotificationChannelGHighlightMessages), NotificationManager.IMPORTANCE_HIGH);
            channelHighlightMessage.setShowBadge(false);
            channelHighlightMessage.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            channelHighlightMessage.setLightColor(Color.GREEN);
            channelHighlightMessage.enableLights(true);
            channelHighlightMessage.setVibrationPattern(new long[] {200, 200});
            channelHighlightMessage.enableVibration(true);

            NotificationChannel channelFiles = new NotificationChannel(Constants.NOTIFICATION_CHANNEL_FILES, "File upload statuses", NotificationManager.IMPORTANCE_MIN);
            channelFiles.setShowBadge(false);
            channelFiles.enableLights(false);
            channelFiles.enableVibration(false);
            channelFiles.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);

            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) {
                manager.createNotificationChannel(channel);
                manager.createNotificationChannel(channelMessage);
                manager.createNotificationChannel(channelMucMessage);
                manager.createNotificationChannel(channelHighlightMessage);
                manager.createNotificationChannel(channelFiles);
            }
        }

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this, Constants.NOTIFICATION_CHANNEL);
        mBuilder.setSmallIcon(R.drawable.stat_offline);
        mBuilder.setContentTitle(getString(R.string.app_name));
        mBuilder.setContentIntent(contentIntent);
        mBuilder.setPriority(NotificationCompat.PRIORITY_MIN);

        startForeground(Notify.NOTIFICATION, mBuilder.build());

        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
		wifiLock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL, "jTalk");

        started = true;

        Cursor cursor = getContentResolver().query(JTalkProvider.ACCOUNT_URI, null, AccountDbHelper.ENABLED + " = '1'", null, null);
        if (cursor != null && cursor.getCount() > 0) {
            connect();
            cursor.close();
        }
    }

    @Override
	public IBinder onBind(Intent intent) {
	    return null;
	}

    @Override
    public void onDestroy() {
    	try {
//    		unregisterReceiver(updateReceiver);
    		unregisterReceiver(connectionReceiver);
            unregisterReceiver(screenStateReceiver);
    	} catch(Exception ignored) { }

        Notify.cancelAll(this);
        stopForeground(true);
        disconnect();
        clearAll();
        started = false;
    }

    public void disconnect() {
        if (!started) return;
        if (wifiLock != null && wifiLock.isHeld()) wifiLock.release();
    	Collection<XMPPConnection> con = getAllConnections();
		for (XMPPConnection connection: con) {
			String account = StringUtils.parseBareAddress(connection.getUser());
	    	if (isAuthenticated(account)) {
                if (connectionTasks.containsKey(account)) { connectionTasks.remove(account).cancel(true); }
                if (pingTimers.containsKey(account)) {
                    Timer timer = pingTimers.remove(account);
                    timer.cancel();
                    timer.purge();
                }
	    		removeConnectionListener(account);
				Presence presence = new Presence(Presence.Type.unavailable, "", 0, null);
				connection.disconnect(presence);
	    	} else if (connection.isConnected()) connection.disconnect();

            if (SMListeners.containsKey(account)) SMListeners.remove(account);

            setState(account, getString(R.string.Disconnect));
            connections.remove(account);
		}
    }
    
    public void disconnect(String account) {
        if (!started) return;
        Log.e("Disconnect", account);
    	if (connections.containsKey(account)) {
            if (connectionTasks.containsKey(account)) {
                connectionTasks.remove(account).cancel(true);
            }

            if (pingTimers.containsKey(account)) {
                Timer timer = pingTimers.remove(account);
                timer.cancel();
                timer.purge();
            }

            try {
                removeConnectionListener(account);
                Presence presence = new Presence(Presence.Type.unavailable, "", 0, null);
                XMPPConnection connection = connections.remove(account);
                connection.disconnect(presence);
            } catch (Exception ignored) { }

            setState(account, getString(R.string.Disconnect));
    	}

        if (SMListeners.containsKey(account)) SMListeners.remove(account);

    	sendBroadcast(new Intent(Constants.UPDATE));
    }
    
    public void reconnect() {
        if (!started) return;
    	globalState = getResources().getString(R.string.Reconnecting) + "...";
    	Intent i = new Intent(Constants.UPDATE);
    	sendBroadcast(i);
    	new Thread() {
    		@Override
    		public void run() {
    			disconnect();
    			connect();
    		}
    	}.start();
    }
    
    public void reconnect(final String account) {
        if (!started) return;
        setState(account, getResources().getString(R.string.Reconnecting) + "...");
    	Intent i = new Intent(Constants.UPDATE);
    	sendBroadcast(i);
    	new Thread() {
    		@Override
    		public void run() {
    			connect(account);
    		}
    	}.start();
    }
    
    public void connect() {
        if (!started) return;
    	if (prefs.getBoolean("WifiLock", false)) wifiLock.acquire();
    	
//		String text  = prefs.getString("currentStatus", "");
		String mode  = prefs.getString("currentMode", "available");
		
		if (mode.equals("online")) {
			mode = "available";
			setPreference("currentSelection", 0);
		}
		
		if (!mode.equals("unavailable")) {
//			globalState = getString(R.string.Connecting) + "...";
	    	Intent i = new Intent(Constants.UPDATE);
	    	sendBroadcast(i);
	    	
	    	Cursor cursor = getContentResolver().query(JTalkProvider.ACCOUNT_URI, null, AccountDbHelper.ENABLED + " = '1'", null, null);
			if (cursor != null && cursor.getCount() > 0) {
				cursor.moveToFirst();
				do {
					String username = cursor.getString(cursor.getColumnIndex(AccountDbHelper.JID)).trim();
					String password = cursor.getString(cursor.getColumnIndex(AccountDbHelper.PASS)).trim();
                    int port = 5222;

                    if (password.isEmpty()) {
                        if (passHash.containsKey(username)) {
                            password = passHash.get(username);
                        } else {
                            Notify.passwordNotify(username);
                            continue;
                        }
                    }

                    try {
                        port = Integer.parseInt(cursor.getString(cursor.getColumnIndex(AccountDbHelper.PORT)));
                    } catch (NumberFormatException ignored) { }

                    String resource = cursor.getString(cursor.getColumnIndex(AccountDbHelper.RESOURCE)).trim();
                    String service = cursor.getString(cursor.getColumnIndex(AccountDbHelper.SERVER));
                    String tls = cursor.getString(cursor.getColumnIndex(AccountDbHelper.TLS));
                    String sasl = cursor.getString(cursor.getColumnIndex(AccountDbHelper.SASL));
                    String compression = cursor.getString(cursor.getColumnIndex(AccountDbHelper.COMPRESSION));

                    ConnectionTask task = new ConnectionTask(username, password, service, port, resource, tls.equals("1"), sasl.equals("1"), compression.equals("1"));
                    if (connectionTasks.containsKey(username)) task = connectionTasks.get(username);
                    if (task.getStatus() != AsyncTask.Status.RUNNING && task.getStatus() != AsyncTask.Status.FINISHED) {
                        task.execute();
                        connectionTasks.put(username, task);
                    }
				} while(cursor.moveToNext());
				cursor.close();
			}
		} else {
//			globalState = text;
			Intent i = new Intent(Constants.UPDATE);
	    	sendBroadcast(i);
		}
    }
    
    public void connect(String account) {
        if (!started) return;

        String[] selectionArgs = {account};
    	Cursor cursor = getContentResolver().query(JTalkProvider.ACCOUNT_URI, null, AccountDbHelper.JID + " = ?", selectionArgs, null);
		if (cursor != null && cursor.getCount() > 0) {
			cursor.moveToFirst();
			
			String username = cursor.getString(cursor.getColumnIndex(AccountDbHelper.JID)).trim();

            if (connectionTasks.containsKey(username)) return;

			String password = cursor.getString(cursor.getColumnIndex(AccountDbHelper.PASS)).trim();
			String resource = cursor.getString(cursor.getColumnIndex(AccountDbHelper.RESOURCE)).trim();
			String service = cursor.getString(cursor.getColumnIndex(AccountDbHelper.SERVER));
			String tls = cursor.getString(cursor.getColumnIndex(AccountDbHelper.TLS));
            String sasl = cursor.getString(cursor.getColumnIndex(AccountDbHelper.SASL));
            String compression = cursor.getString(cursor.getColumnIndex(AccountDbHelper.COMPRESSION));

            int port = 5222;
            try {
                port = Integer.parseInt(cursor.getString(cursor.getColumnIndex(AccountDbHelper.PORT)));
            } catch (NumberFormatException ignored) { }

            if (password.isEmpty()) {
                if (passHash.containsKey(username)) {
                    password = passHash.get(username);
                }
            }

            ConnectionTask task = new ConnectionTask(username, password, service, port, resource, tls.equals("1"), sasl.equals("1"), compression.equals("1"));
            task.execute();
            connectionTasks.put(username, task);
			cursor.close();
		}
    }

    public void joinRoom(final String account, final String group, final String nick, final String password, final boolean loadHistory) {
        if (connections.containsKey(account)) {
            final XMPPConnection connection = connections.get(account);
            if (!connection.isConnected() || !connection.isAuthenticated()) return;

            new Thread() {
                @Override
                public void run() {
                    while (getConferencesHash(account).containsKey(group)) getConferencesHash(account).remove(group);

                    DiscussionHistory h = new DiscussionHistory();
                    try {
                        h.setMaxStanzas(Integer.parseInt(prefs.getString("MucHistorySize", "30")));
                    } catch (NumberFormatException nfe) {
                        h.setMaxStanzas(Constants.LOAD_MESSAGES_COUNT);
                    }

                    MultiUserChat muc = new MultiUserChat(connection, group);
                    muc.addParticipantListener(new PacketListener() {
                        @Override
                        public void processPacket(Packet packet) {
                            Presence p = (Presence) packet;
                            if (p.isAvailable()) sendBroadcast(new Intent(Constants.PRESENCE_CHANGED));
                        }
                    });

                    MucParticipantStatusListener mucParticipantStatusListener;
                    if (getJoinedConferences(account).containsKey(group)) {
                        mucParticipantStatusListener = getJoinedConferences(account).get(group).getListener();
                    } else {
                        mucParticipantStatusListener = new MucParticipantStatusListener(account, group);
                    }

                    muc.addParticipantStatusListener(mucParticipantStatusListener);
                    getConferencesHash(account).put(group, muc);

                    Presence presence = new Presence(Presence.Type.available);
                    presence.setStatus(prefs.getString("currentStatus", ""));
                    presence.setMode(Presence.Mode.valueOf(prefs.getString("currentMode", "available")));

                    try {
                        muc.join(nick, password, h, 10000, presence);

                        if (loadHistory) {
                            if (!getJoinedConferences(account).containsKey(group)) {
                                Conference conf = new Conference(group, nick, password, mucParticipantStatusListener);
                                getJoinedConferences(account).put(group, conf);
                            }
                        }

                        Intent updateIntent = new Intent(Constants.PRESENCE_CHANGED);
                        updateIntent.putExtra("join", true);
                        updateIntent.putExtra("group", group);
                        sendBroadcast(updateIntent);

                        writeMucMessage(account, group, nick, getString(R.string.YouJoin));

                        Avatars.loadAvatar(account, group);
//                         Avatars.loadAllAvatars(connection, group);
                    } catch (Exception e) {
                        Intent eIntent = new Intent(Constants.ERROR);
                        eIntent.putExtra("error", "[" + group + "] " + "Error: " + e.getLocalizedMessage());
                        sendBroadcast(eIntent);
                    }
                }
            }.start();
        }
    }

    public void joinRoom(final String account, final String group, final String nick, final String password) {
        joinRoom(account, group, nick, password, true);
    }
	
	public void leaveRoom(String account, String group) {
		if (getConferencesHash(account).containsKey(group)) {
			try {
				MultiUserChat muc = getConferencesHash(account).get(group);
				if (muc.isJoined()) {
                    writeMucMessage(account, group, muc.getNickname(), getString(R.string.YouLeave));
                    muc.leave();
                }
			} catch (IllegalStateException ignored) { }
			while (getConferencesHash(account).containsKey(group)) getConferencesHash(account).remove(group);
            setMessageList(account, group, new ArrayList<MessageItem>());
	    }
	    while (getJoinedConferences(account).containsKey(group)) getJoinedConferences(account).remove(group);
	    Intent updateIntent = new Intent(Constants.PRESENCE_CHANGED);
		sendBroadcast(updateIntent);
	}
	
    public void leaveAllRooms(String account) {
        Hashtable<String, MultiUserChat> hash = conferences.get(account);
        if (!hash.isEmpty()) {
            Enumeration<String> groups = hash.keys();
            while(groups.hasMoreElements()) {
                String group = groups.nextElement();
                MultiUserChat muc = hash.get(group);
                writeMucMessage(account, group, muc.getNickname(), getString(R.string.YouLeave));
                try {
                    if (muc.isJoined()) { muc.leave(); }
                } catch (IllegalStateException ignored) { }
            }
        }
        Intent updateIntent = new Intent(Constants.PRESENCE_CHANGED);
        sendBroadcast(updateIntent);
    }

    public void addContact(String account, String jid, String name, String group) {
	    try {
            Roster roster = getRoster(account);
            if (roster != null) {
                String[] groups = { group };
                roster.createEntry(jid, name, groups);
                if (roster.getSubscriptionMode() == Roster.SubscriptionMode.manual) {
                    Presence presence = new Presence(Presence.Type.subscribe);
                    presence.setTo(jid);
                    sendPacket(account, presence);
                }
            }
	    } catch (XMPPException ignored) {    }
    }
  
    public void removeContact(String account, String jid) {
    	try {
    		Roster roster = getRoster(account);
    		if (roster != null) {
    			RosterEntry entry = roster.getEntry(jid);
        		if (entry != null) roster.removeEntry(entry);
    		}
    		
//    		getContentResolver().delete(JTalkProvider.CONTENT_URI, "jid = '" + jid + "'", null);
//	    		if (getMessagesHash(account).containsKey(jid)) {
//	    			getMessagesHash(account).remove(jid);
//	    		}
    	} catch (XMPPException ignored) {  }
    }

    public boolean isAuthenticated() {
    	for(XMPPConnection connection : connections.values()) {
    		if (connection.isAuthenticated()) return true;
    	}
    	return false;
    }
    
  	public boolean isAuthenticated(String account) {
  		if (account != null && connections.containsKey(account)) {
  			XMPPConnection connection = connections.get(account);
            return connection.getUser() != null && connection.isAuthenticated();
  		} else return false;
  	}

  	public void sendMessage(String account, String user, String message) {
  		String resource = StringUtils.parseResource(user);
  		if (resource.length() > 0) sendMessage(account, StringUtils.parseBareAddress(user), message, resource);
  		else sendMessage(account, user, message, null);
  	}

    public void sendMessage(String account, String user, String message, String resource) {
        sendMessage(account, user, message, resource, null);
    }
  	
  	public void sendMessage(String account, String user, String message, String resource, String oobUrl) {
  		if (connections.containsKey(account)) {
  			final XMPPConnection connection = connections.get(account);
  			
  			String id = System.currentTimeMillis()+"";
  	  		
  	  		final Message msg;
  	  		if (resource != null && resource.length() > 0) {
  	  		    msg = new Message(user + "/" + resource, Message.Type.chat);
                if (getConferencesHash(account).containsKey(user)) user = user + "/" + resource;
  	  		}  else msg = new Message(user, Message.Type.chat);
  	  		msg.setPacketID(id);
  	  		msg.setBody(message);
  	  		
  	  		ReceiptExtension receipt = new ReceiptExtension(Receipt.request, "");
  	  		msg.addExtension(receipt);

  	  		if (oobUrl != null) msg.addExtension(new OobExtension(oobUrl));
  	  		
  	  		Date date = new java.util.Date();
            String time = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(date);
  	        
  	  		MessageItem msgItem = new MessageItem(account, user, id);
  	  		msgItem.setTime(time);
  	  		msgItem.setName(getResources().getString(R.string.Me));
  	  		msgItem.setBody(message);
  	  		msgItem.setReceived(false);
  	  		
  	  		MessageLog.writeMessage(account, user, msgItem);

  	  		new Thread() {
  	  			@Override
  	  			public void run() {
  	  				if(connection != null && connection.getUser() != null) {
  	  					connection.sendPacket(msg);
  	  				}
  	  			}
  	  		}.start();
  		}
  	}
  	
  	public void editMessage(String account, final String user, final String rid, final String message, boolean isMuc) {
  		if (connections.containsKey(account)) {
  			XMPPConnection connection = connections.get(account);
  	  		
  	  		final Message msg = new Message(user, Message.Type.chat);
            if (isMuc) msg.setType(Message.Type.groupchat);
  	  		msg.setPacketID(System.currentTimeMillis()+"");
  	  		msg.setBody(message);
  	  		
  	  		ReplaceExtension replace = new ReplaceExtension(rid);
  	  		msg.addExtension(replace);
  	  		
  	  		if(connection != null && connection.getUser() != null) {
                connection.sendPacket(msg);
                MessageLog.editMessage(account, user, rid, message);
  			}
  		}
  	}
  	
  	public void sendPacket(String from, final Packet packet) {
  		if (connections.containsKey(from)) {
  			final XMPPConnection connection = connections.get(from);
  			
  			new Thread() {
  	  			@Override
  	  			public void run() {
  	  				if(connection != null && connection.getUser() != null) {
  	  					connection.sendPacket(packet);
  	  				}
  	  			}
  	  		}.start();
  		}
  	}
  	
  	public void sendReceivedPacket(final XMPPConnection connection, String user, String id) {
          ReceiptExtension extension = new ReceiptExtension(Receipt.received, id);
          final Message msg = new Message(user);
          msg.setPacketID(id);
          msg.addExtension(extension);
          new Thread() {
              @Override
              public void run() {
                  if(connection != null && connection.getUser() != null) {
                      connection.sendPacket(msg);
                  }
              }
          }.start();
  	}

  	public void setChatState(String account, String user, ChatState state) {
  		if (connections.containsKey(account)) {
  			final XMPPConnection connection = connections.get(account);
  			
  			if (user != null && getType(account, user) != Presence.Type.unavailable) {
  	  			ChatStateExtension extension = new ChatStateExtension(state);
  	  	  		final Message msg = new Message(user, Message.Type.chat);
  	  	        msg.addExtension(extension);
  	  	        new Thread() {
  	  	  			@Override
  	  	  			public void run() {
  	  	  				if(connection != null && connection.getUser() != null) {
  	  	  					connection.sendPacket(msg);
  	  	  				}
  	  	  			}
  	  	  		}.start();
  	  		}
  		}
  	}
  	
  	// Global status
  	public void sendPresence(final String state, final String mode, final int priority) {
  		for (final XMPPConnection connection : connections.values()) {
  			new Thread() {
  	  			public void run() {
//  	  				setPreference(JTalkService.this, "currentPriority", priority);
//  	       			setPreference(JTalkService.this, "currentMode", mode);
//  	       			setPreference(JTalkService.this, "currentStatus", state);
//  					setPreference(JTalkService.this, "currentSelection", getPosition(mode));
//  					setPreference(JTalkService.this, "lastStatus"+mode, state);
  						
  	  				if (connection.isAuthenticated()) {
  	  					String account = StringUtils.parseBareAddress(connection.getUser());
  	  					if (!mode.equals("unavailable")) {
  	  						Presence presence = new Presence(Presence.Type.available);
  	  	  					if (state != null) presence.setStatus(state);
  	  	  					presence.setMode(Presence.Mode.valueOf(mode));
  	  	  					presence.setPriority(priority);
  	  	  					connection.sendPacket(presence);
  	  	  					
  	  	  					for (Hashtable<String, MultiUserChat> hash : conferences.values()) {
  	  	  						Enumeration<String> e = hash.keys();
  	  	  						while(e.hasMoreElements()) {
  	  	  							try {
  	  	  								MultiUserChat muc = hash.get(e.nextElement());
  	  	  								if (muc.isJoined())	muc.changeAvailabilityStatus(state, Presence.Mode.valueOf(mode));
  	  	  							} catch (IllegalStateException ignored) { }
  	  	  						}
  	  	  					}
  	  	  					Notify.updateNotify();
  	  					} else {
  	  						removeConnectionListener(account);
  	  						Presence presence = new Presence(Presence.Type.unavailable, state, priority, null);
  	  						connection.disconnect(presence);
  	  						if (!isAuthenticated()) Notify.offlineNotify(JTalkService.this, state);
  	  					}
  	  				} else {
  	  					if (mode.equals("unavailable")) {
  	  						if (!isAuthenticated()) Notify.offlineNotify(JTalkService.this, state);
  	  					}
  	  				}
  	  				
  					Intent i = new Intent(Constants.UPDATE);
  		            sendBroadcast(i);
  	  			}
  	  		}.start();
  		}
  	}
  	
  	public void sendPresence(final String account, final String state, final String mode, final int priority) {
  		if (connections.containsKey(account)) {
  			new Thread() {
  	  			public void run() {
                    XMPPConnection connection = connections.get(account);
  	  				if (connection.isAuthenticated()) {
  	  					String account = StringUtils.parseBareAddress(connection.getUser());
  	  					if (!mode.equals("unavailable")) {
  	  						Presence presence = new Presence(Presence.Type.available);
  	  	  					if (state != null) presence.setStatus(state);
  	  	  					presence.setMode(Presence.Mode.valueOf(mode));
  	  	  					presence.setPriority(priority);
  	  	  					connection.sendPacket(presence);
  	  	  					
  	  	  					Enumeration<String> e = getConferencesHash(account).keys();
  	  	  					while(e.hasMoreElements()) {
  	  	  						try {
  		  							MultiUserChat muc = getConferencesHash(account).get(e.nextElement());
  		  							if (muc != null && muc.isJoined())	muc.changeAvailabilityStatus(state, Presence.Mode.valueOf(mode));
  		  						} catch (IllegalStateException ignored) { }
  	  	  					}
  	  	  					
  	  	  					Notify.updateNotify();
  	  					} else {
  	  						removeConnectionListener(account);
  	  						Presence presence = new Presence(Presence.Type.unavailable, state, priority, null);
  	  						connection.disconnect(presence);
  	  						if (!isAuthenticated()) Notify.offlineNotify(JTalkService.this, state);
  	  					}
  	  				} else {
  	  					if (mode.equals("unavailable")) {
  	  						if (isAuthenticated()) Notify.offlineNotify(JTalkService.this, state);
  	  					}
  	  				}
                    setState(account, state);
  	  				
  					Intent i = new Intent(Constants.UPDATE);
  		            sendBroadcast(i);
  	  			}
  	  		}.start();
  		}
  	}
  	
  	public void sendPresenceTo(String account, final String to, final String state, final String mode, final int priority) {
  		if (connections.containsKey(account)) {
  			final XMPPConnection connection = connections.get(account);
  			
  			new Thread() {
  	  			public void run() {
  	  				if (connection.getUser() != null) {
  	  					Presence presence;
  	  					
  	  					if (!mode.equals("unavailable")) {
  	  						presence = new Presence(Presence.Type.available);
  	  						presence.setMode(Presence.Mode.valueOf(mode));
  	  					}
  	  					else presence = new Presence(Presence.Type.unavailable);
  	  	  				if (state != null) presence.setStatus(state);
  	  	  				presence.setTo(to);
  	  	  				presence.setPriority(priority);
  	  	  				connection.sendPacket(presence);
  	  				}
  	  			}
  	  		}.start();
  		}
  	}

  	public void setPreference(String name, Object value) {
        if (!started) return;
  		if (prefs == null) prefs = PreferenceManager.getDefaultSharedPreferences(this);
  		SharedPreferences.Editor editor = prefs.edit();
  		if(value instanceof String) editor.putString(name, String.valueOf(value));
  		else if(value instanceof Integer) editor.putInt(name, Integer.parseInt(String.valueOf(value)));
  		else if(value instanceof Boolean) editor.putBoolean(name, (Boolean)value);
  		editor.apply();
  	}

    private void writeMucMessage(String account, String group, String nick, String message) {
        String time = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(new java.util.Date());

        MessageItem item = new MessageItem(account, group + "/" + nick, System.currentTimeMillis()+"");
        item.setBody(message);
        item.setType(MessageItem.Type.status);
        item.setName(StringUtils.parseName(group));
        item.setTime(time);

        MessageLog.writeMucMessage(account, group, nick, item);
    }

  	private void clearAll() {
        activeChats.clear();
        msgCounter.clear();
  		autoStatusTimer.cancel();
        collapsedGroups.clear();
        unreadMessages.clear();
        conferences.clear();
        joinedConferences.clear();
        avatarsHash.clear();
        vcards.clear();
        textHash.clear();
        messagesCount.clear();
  	}

    public void configure() {
        ProviderManager pm = ProviderManager.getInstance();

        // HTTP file upload
        pm.addIQProvider("request", Request.XMLNS, new Request.Provider());
        pm.addIQProvider("slot", Slot.XMLNS, new Slot.SlotIQProvider());

        pm.addIQProvider("query","jabber:iq:private", new PrivateDataManager.PrivateDataIQProvider());
        pm.addIQProvider("query", "jabber:iq:version", new VersionProvider());

        //  Roster Exchange
        pm.addExtensionProvider("x","jabber:x:roster", new RosterExchangeProvider());

        //  Caps
        pm.addExtensionProvider("c", CapsExtension.XMLNS, new CapsExtensionProvider());

        // Messages Receipts
        pm.addExtensionProvider("request","urn:xmpp:receipts", new ReceiptExtension.Provider());
        pm.addExtensionProvider("received","urn:xmpp:receipts", new ReceiptExtension.Provider());

        // Last Message Correction
        pm.addExtensionProvider("replace", "urn:xmpp:message-correct:0", new ReplaceExtension.Provider());

        // Captcha
        pm.addExtensionProvider("captcha", "urn:xmpp:captcha", new CaptchaExtension.Provider());
        pm.addExtensionProvider("data", "urn:xmpp:bob", new BobExtension.Provider());

        //  Chat State
        pm.addExtensionProvider("active","http://jabber.org/protocol/chatstates", new ChatStateExtension.Provider());
        pm.addExtensionProvider("composing","http://jabber.org/protocol/chatstates", new ChatStateExtension.Provider());
        pm.addExtensionProvider("paused","http://jabber.org/protocol/chatstates", new ChatStateExtension.Provider());
        pm.addExtensionProvider("inactive","http://jabber.org/protocol/chatstates", new ChatStateExtension.Provider());
        pm.addExtensionProvider("gone","http://jabber.org/protocol/chatstates", new ChatStateExtension.Provider());

        // Carbons
        pm.addExtensionProvider("enable", Enable.XMLNS, new Enable.Provider());
        pm.addExtensionProvider("disable", Disable.XMLNS, new Disable.Provider());
        pm.addExtensionProvider("private", Private.XMLNS, new Private.Provider());
        pm.addExtensionProvider("sent", Sent.XMLNS, new Sent.Provider());
        pm.addExtensionProvider("received", Received.XMLNS, new Received.Provider());

        pm.addExtensionProvider("forwarded", Forwarded.XMLNS, new Forwarded.Provider());

        //  Group Chat Invitations
        pm.addExtensionProvider("x","jabber:x:conference", new GroupChatInvitation.Provider());

        //  Service Discovery # Items
        pm.addIQProvider("query","http://jabber.org/protocol/disco#items", new DiscoverItemsProvider());
        pm.addIQProvider("query","http://jabber.org/protocol/disco#info", new DiscoverInfoProvider());

        //  Data Forms
        pm.addExtensionProvider("x","jabber:x:data", new DataFormProvider());

        //  MUC User
        pm.addExtensionProvider("x","http://jabber.org/protocol/muc#user", new MUCUserProvider());

        //  MUC Admin
        pm.addIQProvider("query","http://jabber.org/protocol/muc#admin", new MUCAdminProvider());

        //  MUC Owner
        pm.addIQProvider("query","http://jabber.org/protocol/muc#owner", new MUCOwnerProvider());

        //  Delayed Delivery
        pm.addExtensionProvider("x","jabber:x:delay", new DelayInformationProvider());
        pm.addExtensionProvider("delay",DelayInfo.XMLNS, new DelayInfoProvider());

        //  VCard
        pm.addIQProvider("vCard","vcard-temp", new VCardProvider());

        //  Offline Message Requests
        pm.addIQProvider("offline","http://jabber.org/protocol/offline", new OfflineMessageRequest.Provider());

        //  Offline Message Indicator
        pm.addExtensionProvider("offline","http://jabber.org/protocol/offline", new OfflineMessageInfo.Provider());

        //  Last Activity
        pm.addIQProvider("query","jabber:iq:last", new LastActivity.Provider());

        //  User Search
        pm.addIQProvider("query","jabber:iq:search", new UserSearch.Provider());

        //  SharedGroupsInfo
        pm.addIQProvider("sharedgroup","http://www.jivesoftware.org/protocol/sharedgroup", new SharedGroupsInfo.Provider());

        // Ping
        pm.addIQProvider("ping", "urn:xmpp:ping", new Ping.Provider());

        //  JEP-33: Extended Stanza Addressing
        pm.addExtensionProvider("addresses","http://jabber.org/protocol/address", new MultipleAddresses.Provider());

        //   FileTransfer
        pm.addIQProvider("si","http://jabber.org/protocol/si", new StreamInitiationProvider());
        pm.addIQProvider("query","http://jabber.org/protocol/bytestreams", new BytestreamsProvider());
        pm.addIQProvider("open","http://jabber.org/protocol/ibb", new IBBProviders.Open());
        pm.addIQProvider("close","http://jabber.org/protocol/ibb", new IBBProviders.Close());
        pm.addExtensionProvider("data","http://jabber.org/protocol/ibb", new IBBProviders.Data());

        //  Privacy
        pm.addIQProvider("query","jabber:iq:privacy", new PrivacyProvider());
        pm.addIQProvider("command", "http://jabber.org/protocol/commands", new AdHocCommandDataProvider());
        pm.addExtensionProvider("malformed-action", "http://jabber.org/protocol/commands", new AdHocCommandDataProvider.MalformedActionError());
        pm.addExtensionProvider("bad-locale", "http://jabber.org/protocol/commands", new AdHocCommandDataProvider.BadLocaleError());
        pm.addExtensionProvider("bad-payload", "http://jabber.org/protocol/commands", new AdHocCommandDataProvider.BadPayloadError());
        pm.addExtensionProvider("bad-sessionid", "http://jabber.org/protocol/commands", new AdHocCommandDataProvider.BadSessionIDError());
        pm.addExtensionProvider("session-expired", "http://jabber.org/protocol/commands", new AdHocCommandDataProvider.SessionExpiredError());
    }
    
	public String getDerivedNick(final String username, BookmarkedConference bc) {
		String bareJid = StringUtils.parseBareAddress(username);
		String nick = null;
		
		// try bookmark if passed
		if (bc != null) {
			nick = bc.getNickname();
		}

		// no nickname with bookmark
		if (nick == null || nick.length() < 1) {
			// try account preference
            String[] selectionArgs = {bareJid};
			Cursor cursor = getContentResolver().query(JTalkProvider.ACCOUNT_URI, null, AccountDbHelper.JID + " = ?", selectionArgs, null);
			if (cursor != null && cursor.getCount() > 0) {
				cursor.moveToFirst();
                nick = cursor.getString(cursor.getColumnIndex(AccountDbHelper.NICK));
                cursor.close();
			}
		}
		// no nickname account preference
		if (nick == null || nick.length() < 1) {
			// use localpart of JID
			nick = StringUtils.parseName(bareJid);
		}
		return nick;
	}

    public class ConnectionTask extends AsyncTask<String, Integer, String> {
        Intent intent = new Intent(Constants.UPDATE);
        String username, password, resource, service;
        boolean tls = true, sasl = true, compression = false;
        int port = 5222;

        public ConnectionTask(String username, String password, String service, int port, String resource, boolean tls, boolean sasl, boolean compression) {
            this.username = username;
            this.password = password;
            this.service = service;
            this.port = port;
            this.resource = resource;
            this.tls = tls;
            this.sasl = sasl;
            this.compression = compression;
        }

        @Override
        protected String doInBackground(String... args) {
            if (connecting) return null;
            else connecting = true;

            if (username == null || username.indexOf("@") < 1) {
                setState(username, getString(R.string.ConnectionError));
                sendBroadcast(intent);
                return null;
            } else {
                Notify.connectingNotify(username);

                setState(username, getString(R.string.Connecting));
                sendBroadcast(new Intent(Constants.UPDATE));

                SmackConfiguration.setPacketReplyTimeout(10000);
                SmackConfiguration.setKeepAliveInterval(30000);

                String host = StringUtils.parseServer(username);
                String user = StringUtils.parseName(username);

                if (service == null || service.length() < 4) {
                    try {
                        Lookup lookup = new Lookup("_xmpp-client._tcp." + host, Type.SRV);
                        lookup.setCredibility(Credibility.ANY);
                        Record[] records = lookup.run();
                        if(lookup.getResult() == Lookup.SUCCESSFUL) {
                            if (records.length > 0) {
                                SRVRecord record = (SRVRecord) records[0];
                                service = record.getTarget().toString();
                                service = service.substring(0, service.length()-1);
                                port = record.getPort();
                            }
                        }
                    } catch(Exception ignored) { }
                }

                if (service == null || service.length() <= 3) service = host;

                ConnectionConfiguration cc = new ConnectionConfiguration(service, port, host);
                cc.setCapsNode("http://jtalk.ustyugov.net/caps");
                cc.setSelfSignedCertificateEnabled(true);
                cc.setReconnectionAllowed(false);
                cc.setRosterLoadedAtLogin(true);
                cc.setSendPresence(false);
                cc.setSASLAuthenticationEnabled(sasl);
                cc.setSecurityMode(tls ? SecurityMode.enabled : SecurityMode.disabled);
                cc.setCompressionEnabled(compression);

                SMListener smListener;
                if (prefs.getBoolean("EnableSM", false)) {
                    cc.setSmEnabled(true);
                    cc.setSmMax(3600);
                }

                XMPPConnection connection = new XMPPConnection(cc);
                connection.setSoftName(getString(R.string.app_name));
                connection.setSoftVersion(getString(R.string.version) + " (" + getString(R.string.build) + ")");
                connection.addFeature("http://jabber.org/protocol/disco#info");
                connection.addFeature("http://jabber.org/protocol/muc");
                connection.addFeature("http://jabber.org/protocol/chatstates");
                connection.addFeature("http://jabber.org/protocol/bytestreams");
                connection.addFeature("http://jabber.org/protocol/pubsub#event");
                connection.addFeature("jabber:iq:version");
                connection.addFeature("urn:xmpp:receipts");
                connection.addFeature("urn:xmpp:time");
                connection.addFeature("urn:xmpp:ping");
                connection.addFeature(ReplaceExtension.NAMESPACE);
                connection.addFeature(Notes.NAMESPACE);
                connection.addFeature(Enable.XMLNS);
//                connection.addFeature("urn:xmpp:sm:3");
                connection.addFeature(Slot.XMLNS);

                if (prefs.getBoolean("EnableSM", false)) {
                    if (SMListeners.containsKey(username)) {
                        smListener = SMListeners.get(username);
                        smListener.setEnabled(false);
                        cc.setSmResume(true);
                        cc.setSmInH(smListener.getInH());
                        cc.setSmPrevId(smListener.getId());
                    } else {
                        smListener = new SMListener(connection);
//                        smListener.setEnabled(true);
                        smListener.addListeners();
                        SMListeners.put(username, smListener);
                    }
                }

                if (prefs.getBoolean("XMLConsole", false)) XmlListeners.put(username, new XmlListener(connection));

                try {
                    if (!connection.isConnected()) connection.connect();
                } catch (XMPPException xe) {
                    if (connections.containsKey(username)) {
                        if (connectionTasks.containsKey(username)) connectionTasks.remove(username);
                        connecting = false;
                        reconnect(username);
                    } else {
                        String error = "[" + connection.getServiceName() + "] Error: " + xe.getLocalizedMessage();
                        setState(username, error);
                        sendBroadcast(intent);
                        if (!isAuthenticated()) Notify.offlineNotify(JTalkService.this, error);
                    }
                    return null;
                }

                try {
                    if (connection.isConnected()) {
                        if (!connection.isAuthenticated()) connection.login(user, password, resource);

                        connection.addPacketListener(new MsgListener(JTalkService.this, connection, username), new PacketTypeFilter(Message.class));
                        connection.addPacketListener(new PingListener(connection), new PacketFilter() {
                            @Override
                            public boolean accept(Packet packet) {
                                return packet instanceof Ping;
                            }
                        });

                        Roster roster = connection.getRoster();
                        roster.setSubscriptionMode(Roster.SubscriptionMode.valueOf(prefs.getString("SubscriptionMode", Roster.SubscriptionMode.accept_all.name())));
                        roster.addRosterListener(new RstListener(username));

                        new PrivacyListManager(connection);
                        new ServiceDiscoveryManager(connection);
                        new AdHocCommandManager(connection);

                        FileTransferManager fileTransferManager = new FileTransferManager(connection);
                        fileTransferManager.addFileTransferListener(new IncomingFileListener());
                        fileTransferManagers.put(username, fileTransferManager);

                        try {
                            MultiUserChat.addInvitationListener(connection, new InviteListener(username));
                        } catch (Exception ignored) { }

                        addConnectionListener(username, connection);
                        connections.put(username, connection);
                    }
                    return username;
                } catch (Exception e) {
                    if (connectionTasks.containsKey(username)) connectionTasks.remove(username);
                    connecting = false;

                    setState(username, e.getLocalizedMessage());
                    sendBroadcast(intent);
                    if (!isAuthenticated()) Notify.offlineNotify(JTalkService.this, e.getLocalizedMessage());
                    return null;
                }
            }
        }

        @Override
        public void onPostExecute(final String username) {
            connecting = false;
            if (username != null) {
                final XMPPConnection connection = connections.get(username);
                if (!connection.isAuthenticated() || !connection.isConnected()) return;

                int priority = prefs.getInt("currentPriority", 0);
                String status  = prefs.getString("currentStatus", "");
                String mode  = prefs.getString("currentMode", "available");
                sendPresence(username, status, mode, priority);
                setState(username, status);

                if (prefs.getBoolean("EnableCarbons", true)) {
                    IQ enableCarbons = new IQ() {
                        @Override
                        public String getChildElementXML() {
                            return new Enable().toXML();
                        }
                    };
                    enableCarbons.setPacketID(System.currentTimeMillis()+"");
                    enableCarbons.setType(IQ.Type.SET);
                    connection.sendPacket(enableCarbons);
                }

                if (prefs.getBoolean("Ping", false)) {
                    int timeout = 60000;
                    try {
                        timeout = Integer.parseInt(prefs.getString("PingTimeout", 60+"")) * 1000;
                    } catch (NumberFormatException ignored) { }
                    Timer pingTimer = new Timer();
                    pingTimer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            new PingTask(username).execute();
                        }
                    }, timeout, timeout * 2);
                    pingTimers.put(username, pingTimer);
                }

                Notify.updateNotify();

                new IgnoreList(connection).createIgnoreList();

                // Join to rooms if reconnect or autojoin is enabled
                if (!getJoinedConferences(username).isEmpty()) {
                    for (Conference conference : getJoinedConferences(username).values()) {
                        joinRoom(username, conference.getName(), conference.getNick(), conference.getPassword(), false);
                    }
                } else {
                    if (prefs.getBoolean("EnableAutojoin", true)) {
                        try {
                            BookmarkManager bm = BookmarkManager.getBookmarkManager(connection);
                            Collection<BookmarkedConference> bookmarks = bm.getBookmarkedConferences();
                            for(BookmarkedConference bc : bookmarks) {
                                if (bc.isAutoJoin()) {
                                    String nick = getDerivedNick(username, bc);
                                    joinRoom(username, bc.getJid(), nick, bc.getPassword());
                                }
                            }
                        } catch (XMPPException ignored) { }
                    }
                }

                if (connectionTasks.containsKey(username)) connectionTasks.remove(username);
                sendBroadcast(new Intent(Constants.UPDATE));

                resetTimer();
            }
        }
    }
}
