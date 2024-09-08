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

package net.ustyugov.jtalk.adapter;

import java.util.Enumeration;

import android.app.Activity;
import net.ustyugov.jtalk.*;
import net.ustyugov.jtalk.db.AccountDbHelper;
import net.ustyugov.jtalk.db.JTalkProvider;
import net.ustyugov.jtalk.service.JTalkService;

import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.RosterPacket;
import org.jivesoftware.smack.util.StringUtils;

import com.jtalk2.R;

import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Typeface;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import org.jivesoftware.smackx.ChatState;
import org.jivesoftware.smackx.muc.MultiUserChat;

public class OpenChatsAdapter extends ArrayAdapter<RosterItem> {
	private JTalkService service;
    private Activity activity;
    private SharedPreferences prefs;
    private IconPicker ip;
    private int fontSize = 14;
    private int statusSize = fontSize - 4;
    private int sidebarSize = 100;

    enum Mode { nick, status, all }
	
	public OpenChatsAdapter(Activity activity) {
		super(activity, R.id.name);
        this.activity = activity;
        this.service = JTalkService.getInstance();
        this.prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        this.ip = service.getIconPicker();

        try {
            this.fontSize = Integer.parseInt(prefs.getString("RosterSize", activity.getResources().getString(R.string.DefaultFontSize)));
        } catch (NumberFormatException ignored) { }
    }
	
	public void update(int sidebarSize) {
        this.sidebarSize = sidebarSize;
		clear();
		add(new RosterItem(null, RosterItem.Type.group, null));

		Cursor cursor = activity.getContentResolver().query(JTalkProvider.ACCOUNT_URI, null, AccountDbHelper.ENABLED + " = '1'", null, null);
		if (cursor != null && cursor.getCount() > 0) {
			cursor.moveToFirst();
			do {
				String account = cursor.getString(cursor.getColumnIndex(AccountDbHelper.JID)).trim();
				XMPPConnection connection = service.getConnection(account);

                for(String jid : service.getActiveChats(account)) {
                    if (!service.getConferencesHash(account).containsKey(jid)) {
                        Roster roster = service.getRoster(account);
                        RosterEntry entry = null;
                        if (roster != null) entry = roster.getEntry(jid);
                        if (entry == null) entry = new RosterEntry(jid, jid, RosterPacket.ItemType.both, RosterPacket.ItemStatus.SUBSCRIPTION_PENDING, roster, connection);
                        RosterItem item = new RosterItem(account, RosterItem.Type.entry, entry);
                        add(item);
                    }
                }

				Enumeration<String> groupEnum = service.getConferencesHash(account).keys();
				while(groupEnum.hasMoreElements()) {
					String name = groupEnum.nextElement();
					RosterItem item = new RosterItem(account, RosterItem.Type.muc, null);
					item.setName(name);
					add(item);
				}
			} while (cursor.moveToNext());
			cursor.close();
		}
	}
	
	@Override
	public View getView(int position, View v, ViewGroup parent) {
        Mode mode = Mode.nick;
        if (sidebarSize > 3 * (48 * activity.getResources().getDisplayMetrics().density)) {
            mode = Mode.all;
        } else if (sidebarSize > 2 * (48 * activity.getResources().getDisplayMetrics().density)) {
            mode = Mode.status;
        }

        RosterItem ri = getItem(position);
        if (ri.isGroup()) {
            Holders.GroupHolder holder = new Holders.GroupHolder();
            if (v == null || v.getTag() == null || v.getTag() instanceof Holders.ItemHolder) {
                LayoutInflater inflater = activity.getLayoutInflater();
                v = inflater.inflate(R.layout.group, null, false);
                holder.messageIcon = (ImageView) v.findViewById(R.id.msg);
                holder.messageIcon.setVisibility(View.GONE);
                holder.text = (TextView) v.findViewById(R.id.name);
                holder.text.setTypeface(Typeface.DEFAULT_BOLD);
                holder.text.setTextSize(fontSize - 2);
                holder.text.setTextColor(Colors.GROUP_FOREGROUND);
                holder.state = (ImageView) v.findViewById(R.id.state);
                holder.state.setVisibility(View.GONE);
                v.setBackgroundColor(Colors.GROUP_BACKGROUND);
                v.setTag(holder);
            } else {
                holder = (Holders.GroupHolder) v.getTag();
            }

            holder.text.setText(activity.getString(R.string.Chats) + ": " + (getCount() - 1));
            return v;
        } else {
            Holders.ItemHolder holder = new Holders.ItemHolder();
            if(v == null || v.getTag() == null || v.getTag() instanceof Holders.GroupHolder) {
                LayoutInflater inflater = activity.getLayoutInflater();
                v = inflater.inflate(R.layout.entry, null, false);
                holder.name = (TextView) v.findViewById(R.id.name);
                holder.name.setTextColor(Colors.ENTRY_FOREGROUND);
                holder.name.setTextSize(fontSize);
                holder.status = (TextView) v.findViewById(R.id.status);
                holder.status.setTextSize(statusSize);
                holder.status.setTextColor(Colors.SECONDARY_TEXT);
                holder.counter = (TextView) v.findViewById(R.id.msg_counter);
                holder.counter.setTextSize(fontSize);
                holder.messageIcon = (ImageView) v.findViewById(R.id.msg);
                if (ip != null) holder.messageIcon.setImageBitmap(ip.getMsgBitmap());
                holder.statusIcon = (ImageView) v.findViewById(R.id.status_icon);
                holder.statusIcon.setVisibility(View.GONE);
                holder.avatar = (ImageView) v.findViewById(R.id.contactlist_pic);
                holder.avatar.setVisibility(View.GONE);
                holder.caps = (ImageView) v.findViewById(R.id.caps);
                holder.caps.setVisibility(View.GONE);
                v.setTag(holder);
            } else {
                holder = (Holders.ItemHolder) v.getTag();
            }

            String account = ri.getAccount();
            String jid = "";
            if (ri.isEntry() || ri.isSelf()) jid = ri.getEntry().getUser();
            else if (ri.isMuc()) jid = ri.getName();
            String nick = ri.getName();

            if (service.getJoinedConferences(account).containsKey(jid)) {
                nick = StringUtils.parseName(jid);
            } else if (service.getJoinedConferences(account).containsKey(StringUtils.parseBareAddress(jid))) {
                nick = StringUtils.parseResource(jid);
            } else {
                RosterEntry re = ri.getEntry();
                if (re != null) nick = re.getName();
                if (nick == null || nick.equals("")) nick = jid;
            }

            holder.name.setText(nick);
            ChatState state = service.getRoster(account).getChatState(jid);
            if (state != null && state == ChatState.composing) holder.name.setTextColor(Colors.HIGHLIGHT_TEXT);
            else if (service.isHighlight(account, jid)) holder.name.setTextColor(Colors.HIGHLIGHT_TEXT);
            else holder.name.setTextColor(Colors.ENTRY_FOREGROUND);

            String statusText = "";
            if (ri.isMuc()) {
                if (service.getConferencesHash(account).containsKey(ri.getName())) {
                    MultiUserChat muc = service.getConferencesHash(account).get(ri.getName());
                    statusText = muc.getSubject();
                }
            } else {
                statusText = service.getStatus(account, jid);
            }
            if (statusText == null) statusText = "";

            if (prefs.getBoolean("StatusInBar", true)) {
                holder.status.setVisibility(statusText.length() > 0 ? View.VISIBLE : View.GONE);
                holder.status.setText(statusText);
            } else holder.status.setVisibility(View.GONE);

            if (mode == Mode.nick) {
                holder.statusIcon.setVisibility(View.GONE);
                holder.avatar.setVisibility(View.GONE);
                holder.caps.setVisibility(View.GONE);
            } else if (mode == Mode.status) {
                holder.statusIcon.setVisibility(View.VISIBLE);
                if (service.getJoinedConferences(account).containsKey(jid)) {
                    holder.statusIcon.setImageBitmap(ip.getMucBitmap());
                } else {
                    Presence presence = service.getPresence(ri.getAccount(), jid);
                    holder.statusIcon.setImageBitmap(ip.getIconByPresence(presence));
                }
            } else {
                holder.statusIcon.setVisibility(View.VISIBLE);
                if (service.getJoinedConferences(account).containsKey(jid)) {
                    holder.statusIcon.setImageBitmap(ip.getMucBitmap());
                } else {
                    Presence presence = service.getPresence(ri.getAccount(), jid);
                    holder.statusIcon.setImageBitmap(ip.getIconByPresence(presence));
                }

                if (!ri.isMuc()) {
                    if (prefs.getBoolean("ShowCaps", false)) {
                        String node = service.getNode(account, jid);
                        ClientIcons.loadClientIcon(activity, holder.caps, node);
                    }

                    if (service.getJoinedConferences(account).containsKey(StringUtils.parseBareAddress(jid)))
                        Avatars.loadAvatar(activity, jid.replaceAll("/", "%"), holder.avatar);
                    else Avatars.loadAvatar(activity, jid, holder.avatar);
                } else {
                    Avatars.loadAvatar(activity, jid, holder.avatar);
                }
            }

            int count = service.getMessagesCount(account, jid);
            if (count > 0) {
                holder.messageIcon.setVisibility(View.VISIBLE);
                holder.counter.setVisibility(View.VISIBLE);
                holder.counter.setText(String.valueOf(count));
            } else {
                holder.messageIcon.setVisibility(View.GONE);
                holder.counter.setVisibility(View.GONE);
            }

            if (jid.equals(service.getCurrentJid())) {
                holder.name.setTypeface(Typeface.DEFAULT_BOLD);
                holder.name.setTextColor(Colors.ENTRY_FOREGROUND);
                v.setBackgroundColor(Colors.ENTRY_BACKGROUND);
            } else {
                holder.name.setTypeface(Typeface.DEFAULT);
                v.setBackgroundColor(0x00000000);
            }

            if (prefs.getBoolean("ColorLines", false)) {
                if ((position % 2) != 0) v.setBackgroundColor(Colors.ENTRY_BACKGROUND);
                else v.setBackgroundColor(0x00000000);
            }
        }
        return v;
	}
}
