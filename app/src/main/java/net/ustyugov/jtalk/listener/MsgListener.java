/*
 * Copyright (C) 2014, Igor Ustyugov <igor@ustyugov.net>
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

package net.ustyugov.jtalk.listener;

import java.math.BigInteger;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import android.database.Cursor;
import net.ustyugov.jtalk.Constants;
import net.ustyugov.jtalk.MessageItem;
import net.ustyugov.jtalk.MessageLog;
import net.ustyugov.jtalk.Notify;
import net.ustyugov.jtalk.db.JTalkProvider;
import net.ustyugov.jtalk.db.MessageDbHelper;
import net.ustyugov.jtalk.service.JTalkService;
import net.ustyugov.jtalk.utils.Base64;

import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.ChatState;
import org.jivesoftware.smackx.carbons.Received;
import org.jivesoftware.smackx.carbons.Sent;
import org.jivesoftware.smackx.packet.*;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.jtalk2.R;

public class MsgListener implements PacketListener {
	private XMPPConnection connection;
	private String account;
	private Context context;
	private SharedPreferences prefs;
	private JTalkService service;
	
    public MsgListener(Context c, XMPPConnection connection, String account) {
    	this.context = c;
    	this.connection = connection;
    	this.account = account;
    	this.prefs = PreferenceManager.getDefaultSharedPreferences(context);
    	this.service = JTalkService.getInstance();
    }

	public void processPacket(Packet packet) {
		Message msg = (Message) packet;
		String from = msg.getFrom();
		String id = msg.getPacketID();
		String user = StringUtils.parseBareAddress(from).toLowerCase();
		String type = msg.getType().name();
		String body = msg.getBody();
		
        // XEP-0085: Chat State Notifications
        PacketExtension stateExt = msg.getExtension("http://jabber.org/protocol/chatstates");
		if (stateExt != null && !type.equals("error") && !service.getConferencesHash(account).containsKey(user)) {
            try {
                ChatState state = ChatState.valueOf(stateExt.getElementName());
                service.getRoster(account).setChatState(user, state);
                Intent i = new Intent(Constants.UPDATE);
                context.sendBroadcast(i);
            } catch (Exception ignored) {}
		}

        // XEP-0184: Message Delivery Receipts
		ReceiptExtension receiptExt = (ReceiptExtension) msg.getExtension(ReceiptExtension.XMLNS);
		if (receiptExt != null && !type.equals("error")) {
			String receipt = receiptExt.getElementName();

            // message with receipt requested
			if (receipt.equals("request")) {
				service.sendReceivedPacket(connection, user, id);

            // message delivery receipt
			} else if (receipt.equals("received")) {
                String rid = receiptExt.getId();
                if (rid == null || rid.isEmpty()) rid = id;
                String[] selectionArgs = {user, rid};
                Cursor cursor = context.getContentResolver().query(JTalkProvider.CONTENT_URI, null, "jid = ? and id = ?", selectionArgs, MessageDbHelper._ID);
                if (cursor != null && cursor.getCount() > 0) {
                    cursor.moveToLast();
                    String nick = cursor.getString(cursor.getColumnIndex(MessageDbHelper.NICK));
                    String t = cursor.getString(cursor.getColumnIndex(MessageDbHelper.TYPE));
                    String stamp = cursor.getString(cursor.getColumnIndex(MessageDbHelper.STAMP));
                    String b = cursor.getString(cursor.getColumnIndex(MessageDbHelper.BODY));
                    String collapsed = cursor.getString(cursor.getColumnIndex(MessageDbHelper.COLLAPSED));

                    ContentValues values = new ContentValues();
                    values.put(MessageDbHelper.TYPE, t);
                    values.put(MessageDbHelper.JID, user);
                    values.put(MessageDbHelper.ID, rid);
                    values.put(MessageDbHelper.STAMP, stamp);
                    values.put(MessageDbHelper.NICK, nick);
                    values.put(MessageDbHelper.BODY, b);
                    values.put(MessageDbHelper.COLLAPSED, collapsed);
                    values.put(MessageDbHelper.RECEIVED, "true");
                    values.put(MessageDbHelper.FORM, "NULL");
                    values.put(MessageDbHelper.BOB, "NULL");
                    service.getContentResolver().update(JTalkProvider.CONTENT_URI, values, MessageDbHelper.ID + " = '" + rid + "'", null);

                    List<MessageItem> list = service.getMessageList(account, user);
                    if (!list.isEmpty()) {
                        for(MessageItem item : list) {
                            if (item.getId().equals(rid)) {
                                item.setReceived(true);
                                service.setMessageList(account, user, list);
                                context.sendBroadcast(new Intent(Constants.RECEIVED));
                                return;
                            }
                        }
                    }
                    cursor.close();
                    return;
                }
			}
		}

        if (type.equals("groupchat")) { // Group Chat Message
            if (body != null && body.length() > 0) processGroupchatMessage(msg);
        } else if (type.equals("chat") || type.equals("normal") || type.equals("headline")) {
            Received received = (Received) msg.getExtension("received", Received.XMLNS);
            Sent sent = (Sent) msg.getExtension("sent", Received.XMLNS);
            if (sent != null || received != null) {
                Forwarded forwarded = (Forwarded) msg.getExtension("forwarded", Forwarded.XMLNS);
                if (forwarded != null) {
                    msg = forwarded.getMessage();

                    if (sent != null) {
                        if (!service.getRoster(account).contains(StringUtils.parseBareAddress(msg.getTo())) && !service.getConferencesHash(account).containsKey(StringUtils.parseBareAddress(msg.getTo()))) {
                            return;
                        }

                        if (msg.getBody() != null && msg.getBody().length() > 0) {
                            id = msg.getPacketID();
                            if (id == null || id.isEmpty()) id = Base64.encode((from + body).getBytes());

                            Date date = new java.util.Date();
                            String time = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(date);

                            MessageItem msgItem = new MessageItem(account, StringUtils.parseBareAddress(msg.getTo()), id);
                            msgItem.setTime(time);
                            msgItem.setName(context.getResources().getString(R.string.Me));
                            msgItem.setBody(msg.getBody());
                            msgItem.setReceived(false);

                            if (!service.getConferencesHash(account).containsKey(StringUtils.parseBareAddress(msg.getTo()))) {
                                MessageLog.writeMessage(account, StringUtils.parseBareAddress(msg.getTo()), msgItem);
                            } else {
                                msgItem.setJid(msg.getTo());
                                MessageLog.writeMessage(account, msg.getTo(), msgItem);
                            }
                            return;
                        }
                    } else {
                        MUCUser x = (MUCUser) msg.getExtension("x", "http://jabber.org/protocol/muc#user");
                        if (x != null) return;

                        if (service.getConferencesHash(account).containsKey(StringUtils.parseBareAddress(msg.getFrom())))
                            return;
                    }
                }
            }

            if (msg.getBody() != null && msg.getBody().length() > 0) processChatMessage(msg);
        }
	}

    private void processGroupchatMessage(Message msg) {
        String from = msg.getFrom();
        String body = msg.getBody();
        String group = StringUtils.parseBareAddress(from);
        String id = msg.getPacketID();

        if (id == null || id.isEmpty()) {
            try {
                MessageDigest messageDigest = MessageDigest.getInstance("MD5");
                messageDigest.reset();
                messageDigest.update((from+body).getBytes(Charset.forName("UTF8")));
                byte[] resultByte = messageDigest.digest();
                BigInteger bigInt = new BigInteger(1,resultByte);
                id = bigInt.toString(16);
            } catch (NoSuchAlgorithmException ignored) {}
        }

        if (service.getConferencesHash(account).containsKey(group)) {
            ReplaceExtension replace = (ReplaceExtension) msg.getExtension(ReplaceExtension.NAMESPACE);
            if (replace != null && replace.getId() != null) {
                String rid = replace.getId();
                MessageLog.editMucMessage(account, from, rid, body);
            } else {
                Date date = new java.util.Date();
                DelayInfo delayExt = (DelayInfo) msg.getExtension(DelayInfo.XMLNS);
                if (delayExt != null) {

                    // Check double message
                    String[] selectionArgs = {group, id};
                    Cursor cursor = service.getContentResolver().query(JTalkProvider.CONTENT_URI, null, "jid = ? AND id = ?", selectionArgs, "_id");
                    if (cursor != null && cursor.getCount() > 0) {
                        cursor.close();
                        return;
                    }

                    date.setTime(delayExt.getStamp().getTime());
                }
                String time = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(date);

                String nick  = StringUtils.parseResource(from);
                MessageItem item = new MessageItem(account, from, id);
                item.setBody(body);
                item.setTime(time);
                item.setReceived(false);
                item.setName(nick);
                if (nick == null || nick.isEmpty()) item.setType(MessageItem.Type.status);

                if (!service.getCurrentJid().equals(group)) {
                    service.addMessagesCount(account, group);
                }

                boolean highlight = false;
                String mynick = service.getConferencesHash(account).get(group).getNickname();
                if (mynick != null && !mynick.isEmpty() && item.contains(mynick, prefs.getBoolean("HighlightFullWord", false))) highlight = true;
                else {
                    String highString = prefs.getString("Highlights", "");
                    String[] highArray = highString.split(" ");
                    for (String light : highArray) {
                        if (!light.isEmpty() && item.contains(light, prefs.getBoolean("HighlightFullWord", false))) highlight = true;
                    }
                }

                if (highlight) {
                    if (!service.getCurrentJid().equals(group)) {
                        item.setJid(group);
                        service.addHighlight(account, group);
                        service.addUnreadMessage(item);
                        if (delayExt == null) Notify.messageNotify(account, from, Notify.Type.Direct, body);
                    }
                } else {
                    if (delayExt == null) Notify.messageNotify(account, group, Notify.Type.Conference, body);
                }
                MessageLog.writeMucMessage(account, group, nick, item);
            }
        }
    }

    private void processChatMessage(Message msg) {
        String name = null;
        String group = null;
        String from = msg.getFrom();
        String body = msg.getBody();
        String ofrom = from;
        String user = StringUtils.parseBareAddress(from).toLowerCase();
        String id = msg.getPacketID();

        if (id == null || id.isEmpty()) id = Base64.encode((from + body).getBytes());

        boolean fromRoom = service.getConferencesHash(account).containsKey(user);

        // If invite to room
        PacketExtension extension = msg.getExtension("jabber:x:conference");
        if (extension != null) return;

        ReplaceExtension replace = (ReplaceExtension) msg.getExtension(ReplaceExtension.NAMESPACE);
        if (replace != null) {
            String rid = replace.getId();
            if (fromRoom) {
                MessageLog.editMessage(account, from, rid, body);
            } else {
                MessageLog.editMessage(account, user, rid, body);
            }
            Notify.messageNotify(account, user, Notify.Type.Chat, body);
        } else {
            // from room
            if (fromRoom) {
                group = StringUtils.parseBareAddress(from);
                name = StringUtils.parseResource(from);

                if (name == null || name.length() <= 0) {
                    Date date = new java.util.Date();
                    String time = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(date);

                    MessageItem mucMsg = new MessageItem(account, from, id);
                    mucMsg.setBody(body);
                    mucMsg.setTime(time);
                    mucMsg.setName(name);
                    mucMsg.setReceived(false);

                    CaptchaExtension captcha = (CaptchaExtension) msg.getExtension("captcha", "urn:xmpp:captcha");
                    if (captcha != null) {
                        BobExtension bob = (BobExtension) msg.getExtension("data","urn:xmpp:bob");
                        mucMsg.setBob(bob);
                        mucMsg.setCaptcha(true);
                        mucMsg.setForm(captcha.getForm());
                        mucMsg.setName(group);

                        service.addDataForm(id, captcha.getForm());

                        Notify.captchaNotify(account, mucMsg);
                    }

                    if (!service.getCurrentJid().equals(group)) {
                        service.addUnreadMessage(mucMsg);
                    }

                    MessageLog.writeMessage(account, group, mucMsg);
                    return;
                }
            } else { // from user
                Roster roster = service.getRoster(account);
                if (roster != null) {
                    RosterEntry entry = roster.getEntry(user);
                    if (entry != null) name = entry.getName();
                    else {
                        if (prefs.getBoolean("IgnoreNoRoster", false)) {
                            return;
                        }
                    }
                }
            }

            if (name == null || name.equals("")) name = user;

            Date date = new java.util.Date();
            DelayInformation delayExt = (DelayInformation) msg.getExtension("jabber:x:delay");
            if (delayExt != null) date.setTime(delayExt.getStamp().getTime());
            String time = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(date);

            MultipleAddresses ma = (MultipleAddresses) msg.getExtension("addresses", "http://jabber.org/protocol/address");
            if (ma != null) {
                List<MultipleAddresses.Address> list = ma.getAddressesOfType(MultipleAddresses.OFROM);
                if (!list.isEmpty()) {
                    ofrom = list.get(0).getJid();
                    user = StringUtils.parseBareAddress(ofrom);
                }
            }

            MessageItem item = new MessageItem(account, ofrom, id);
            item.setSubject(msg.getSubject());
            item.setBody(body);
            item.setTime(time);
            item.setName(name);

            if (group != null && group.length() > 0) user = group + "/" + name;

            if (!service.getCurrentJid().equals(user)) {
                if (account.equals(user)) service.addMessagesCount(account, from);
                service.addMessagesCount(account, user);
                service.addUnreadMessage(item);
            }

            MessageLog.writeMessage(account, user, item);
            if (delayExt == null) Notify.messageNotify(account, user, Notify.Type.Chat, body);
        }
    }
}
