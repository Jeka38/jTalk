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

package net.ustyugov.jtalk.adapter.muc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import android.text.TextUtils;
import net.ustyugov.jtalk.*;
import net.ustyugov.jtalk.Holders.GroupHolder;
import net.ustyugov.jtalk.Holders.ItemHolder;
import net.ustyugov.jtalk.service.JTalkService;

import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.RosterPacket;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.ChatState;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smackx.packet.MUCUser;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.jtalk2.R;

public class MucUserAdapter extends ArrayAdapter<RosterItem> {
	private String group;
	private String account;
    private String mynick;
	private Activity activity;
    private SharedPreferences prefs;

    enum Mode { nick, status, all }
	
	public MucUserAdapter(Activity activity, String account, String group) {
		super(activity, 0);
		this.activity = activity;
		this.group = group;
		this.account = account;
        this.prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        update();
	}
	
	public void setGroup(String group) {
		this.group = group;
	}
	
	public void update() {

		List<String> mOnline = new ArrayList<>();
		List<String> mChat = new ArrayList<>();
		List<String> mAway = new ArrayList<>();
		List<String> mXa = new ArrayList<>();
		List<String> mDnd = new ArrayList<>();
		
		List<String> pOnline = new ArrayList<>();
		List<String> pChat = new ArrayList<>();
		List<String> pAway = new ArrayList<>();
		List<String> pXa = new ArrayList<>();
		List<String> pDnd = new ArrayList<>();
		
		List<String> vOnline = new ArrayList<>();
		List<String> vChat = new ArrayList<>();
		List<String> vAway = new ArrayList<>();
		List<String> vXa = new ArrayList<>();
		List<String> vDnd = new ArrayList<>();
		
		JTalkService service = JTalkService.getInstance();
		Roster roster = service.getRoster(account);
		XMPPConnection connection = service.getConnection(account);
		clear();

        if (group != null && service.getConferencesHash(account).containsKey(group)) {
            MultiUserChat muc = service.getConferencesHash(account).get(group);
            this.mynick = muc.getNickname();

            Iterator<String> it = muc.getOccupants();
			while (it.hasNext()) {
				Presence p = muc.getOccupantPresence(it.next());
                if (p != null && p.isAvailable()) {
                    Presence.Mode m = p.getMode();
                    if (m == null) m = Presence.Mode.available;
                    String jid = p.getFrom();
                    MUCUser mucUser = (MUCUser) p.getExtension("x", "http://jabber.org/protocol/muc#user");
                    if (mucUser != null) {
                        String role = mucUser.getItem().getRole();
                        switch (role) {
                            case "visitor":
                                if (m == Presence.Mode.chat) vChat.add(jid);
                                else if (m == Presence.Mode.away) vAway.add(jid);
                                else if (m == Presence.Mode.xa) vXa.add(jid);
                                else if (m == Presence.Mode.dnd) vDnd.add(jid);
                                else if (m == Presence.Mode.available) vOnline.add(jid);
                                break;
                            case "participant":
                                if (m == Presence.Mode.chat) pChat.add(jid);
                                else if (m == Presence.Mode.away) pAway.add(jid);
                                else if (m == Presence.Mode.xa) pXa.add(jid);
                                else if (m == Presence.Mode.dnd) pDnd.add(jid);
                                else if (m == Presence.Mode.available) pOnline.add(jid);
                                break;
                            case "moderator":
                                if (m == Presence.Mode.chat) mChat.add(jid);
                                else if (m == Presence.Mode.away) mAway.add(jid);
                                else if (m == Presence.Mode.xa) mXa.add(jid);
                                else if (m == Presence.Mode.dnd) mDnd.add(jid);
                                else if (m == Presence.Mode.available) mOnline.add(jid);
                                break;
                        }
                    }
                }
			}

            Collections.sort(vChat, new SortList.StringComparator());
            Collections.sort(vAway, new SortList.StringComparator());
            Collections.sort(vXa, new SortList.StringComparator());
            Collections.sort(vDnd, new SortList.StringComparator());
            Collections.sort(vOnline, new SortList.StringComparator());

            Collections.sort(pChat, new SortList.StringComparator());
            Collections.sort(pAway, new SortList.StringComparator());
            Collections.sort(pXa, new SortList.StringComparator());
            Collections.sort(pDnd, new SortList.StringComparator());
            Collections.sort(pOnline, new SortList.StringComparator());

            Collections.sort(mChat, new SortList.StringComparator());
            Collections.sort(mAway, new SortList.StringComparator());
            Collections.sort(mXa, new SortList.StringComparator());
            Collections.sort(mDnd, new SortList.StringComparator());
            Collections.sort(mOnline, new SortList.StringComparator());
			
			int mCount = mOnline.size() + mAway.size() + mXa.size() + mDnd.size() + mChat.size();
			int pCount = pOnline.size() + pAway.size() + pXa.size() + pDnd.size() + pChat.size();
			int vCount = vOnline.size() + vAway.size() + vXa.size() + vDnd.size() + vChat.size();
			
			if (mCount > 0) {
				RosterItem item = new RosterItem(account, RosterItem.Type.group, null);
				item.setName(mCount+"");
				item.setObject("moderator");
				add(item);
                if (!prefs.getBoolean("SortByStatuses", true)) {
                    List<String> moderators = new ArrayList<>();
                    moderators.addAll(mOnline);
                    moderators.addAll(mChat);
                    moderators.addAll(mAway);
                    moderators.addAll(mXa);
                    moderators.addAll(mDnd);
                    Collections.sort(moderators, new SortList.StringComparator());

                    for (String jid : moderators) {
                        RosterEntry entry = new RosterEntry(jid, StringUtils.parseResource(jid), RosterPacket.ItemType.both, RosterPacket.ItemStatus.SUBSCRIPTION_PENDING, roster, connection);
                        RosterItem i = new RosterItem(account, RosterItem.Type.entry, entry);
                        add(i);
                    }
                } else {
                    for (String jid : mChat) {
                        RosterEntry entry = new RosterEntry(jid, StringUtils.parseResource(jid), RosterPacket.ItemType.both, RosterPacket.ItemStatus.SUBSCRIPTION_PENDING, roster, connection);
                        RosterItem i = new RosterItem(account, RosterItem.Type.entry, entry);
                        add(i);
                    }
                    for (String jid : mOnline) {
                        RosterEntry entry = new RosterEntry(jid, StringUtils.parseResource(jid), RosterPacket.ItemType.both, RosterPacket.ItemStatus.SUBSCRIPTION_PENDING, roster, connection);
                        RosterItem i = new RosterItem(account, RosterItem.Type.entry, entry);
                        add(i);
                    }
                    for (String jid : mAway) {
                        RosterEntry entry = new RosterEntry(jid, StringUtils.parseResource(jid), RosterPacket.ItemType.both, RosterPacket.ItemStatus.SUBSCRIPTION_PENDING, roster, connection);
                        RosterItem i = new RosterItem(account, RosterItem.Type.entry, entry);
                        add(i);
                    }
                    for (String jid : mXa) {
                        RosterEntry entry = new RosterEntry(jid, StringUtils.parseResource(jid), RosterPacket.ItemType.both, RosterPacket.ItemStatus.SUBSCRIPTION_PENDING, roster, connection);
                        RosterItem i = new RosterItem(account, RosterItem.Type.entry, entry);
                        add(i);
                    }
                    for (String jid : mDnd) {
                        RosterEntry entry = new RosterEntry(jid, StringUtils.parseResource(jid), RosterPacket.ItemType.both, RosterPacket.ItemStatus.SUBSCRIPTION_PENDING, roster, connection);
                        RosterItem i = new RosterItem(account, RosterItem.Type.entry, entry);
                        add(i);
                    }
                }
			}
			
			if (pCount > 0) {
				RosterItem item = new RosterItem(account, RosterItem.Type.group, null);
				item.setName(pCount+"");
				item.setObject("participant");
				add(item);
                if (!prefs.getBoolean("SortByStatuses", true)) {
                    List<String> participants = new ArrayList<>();
                    participants.addAll(pOnline);
                    participants.addAll(pChat);
                    participants.addAll(pAway);
                    participants.addAll(pXa);
                    participants.addAll(pDnd);
                    Collections.sort(participants, new SortList.StringComparator());

                    for (String jid : participants) {
                        RosterEntry entry = new RosterEntry(jid, StringUtils.parseResource(jid), RosterPacket.ItemType.both, RosterPacket.ItemStatus.SUBSCRIPTION_PENDING, roster, connection);
                        RosterItem i = new RosterItem(account, RosterItem.Type.entry, entry);
                        add(i);
                    }
                } else {
                    for (String jid : pChat) {
                        RosterEntry entry = new RosterEntry(jid, StringUtils.parseResource(jid), RosterPacket.ItemType.both, RosterPacket.ItemStatus.SUBSCRIPTION_PENDING, roster, connection);
                        RosterItem i = new RosterItem(account, RosterItem.Type.entry, entry);
                        add(i);
                    }
                    for (String jid : pOnline) {
                        RosterEntry entry = new RosterEntry(jid, StringUtils.parseResource(jid), RosterPacket.ItemType.both, RosterPacket.ItemStatus.SUBSCRIPTION_PENDING, roster, connection);
                        RosterItem i = new RosterItem(account, RosterItem.Type.entry, entry);
                        add(i);
                    }
                    for (String jid : pAway) {
                        RosterEntry entry = new RosterEntry(jid, StringUtils.parseResource(jid), RosterPacket.ItemType.both, RosterPacket.ItemStatus.SUBSCRIPTION_PENDING, roster, connection);
                        RosterItem i = new RosterItem(account, RosterItem.Type.entry, entry);
                        add(i);
                    }
                    for (String jid : pXa) {
                        RosterEntry entry = new RosterEntry(jid, StringUtils.parseResource(jid), RosterPacket.ItemType.both, RosterPacket.ItemStatus.SUBSCRIPTION_PENDING, roster, connection);
                        RosterItem i = new RosterItem(account, RosterItem.Type.entry, entry);
                        add(i);
                    }
                    for (String jid : pDnd) {
                        RosterEntry entry = new RosterEntry(jid, StringUtils.parseResource(jid), RosterPacket.ItemType.both, RosterPacket.ItemStatus.SUBSCRIPTION_PENDING, roster, connection);
                        RosterItem i = new RosterItem(account, RosterItem.Type.entry, entry);
                        add(i);
                    }
                }
			}
			
			if (vCount > 0) {
				RosterItem item = new RosterItem(account, RosterItem.Type.group, null);
				item.setName(vCount+"");
				item.setObject("visitor");
				add(item);
                if (!prefs.getBoolean("SortByStatuses", true)) {
                    List<String> visitors = new ArrayList<>();
                    visitors.addAll(vOnline);
                    visitors.addAll(vChat);
                    visitors.addAll(vAway);
                    visitors.addAll(vXa);
                    visitors.addAll(vDnd);
                    Collections.sort(visitors, new SortList.StringComparator());

                    for (String jid : visitors) {
                        RosterEntry entry = new RosterEntry(jid, StringUtils.parseResource(jid), RosterPacket.ItemType.both, RosterPacket.ItemStatus.SUBSCRIPTION_PENDING, roster, connection);
                        RosterItem i = new RosterItem(account, RosterItem.Type.entry, entry);
                        add(i);
                    }
                } else {
                    for (String jid : vChat) {
                        RosterEntry entry = new RosterEntry(jid, StringUtils.parseResource(jid), RosterPacket.ItemType.both, RosterPacket.ItemStatus.SUBSCRIPTION_PENDING, roster, connection);
                        RosterItem i = new RosterItem(account, RosterItem.Type.entry, entry);
                        add(i);
                    }
                    for (String jid : vOnline) {
                        RosterEntry entry = new RosterEntry(jid, StringUtils.parseResource(jid), RosterPacket.ItemType.both, RosterPacket.ItemStatus.SUBSCRIPTION_PENDING, roster, connection);
                        RosterItem i = new RosterItem(account, RosterItem.Type.entry, entry);
                        add(i);
                    }
                    for (String jid : vAway) {
                        RosterEntry entry = new RosterEntry(jid, StringUtils.parseResource(jid), RosterPacket.ItemType.both, RosterPacket.ItemStatus.SUBSCRIPTION_PENDING, roster, connection);
                        RosterItem i = new RosterItem(account, RosterItem.Type.entry, entry);
                        add(i);
                    }
                    for (String jid : vXa) {
                        RosterEntry entry = new RosterEntry(jid, StringUtils.parseResource(jid), RosterPacket.ItemType.both, RosterPacket.ItemStatus.SUBSCRIPTION_PENDING, roster, connection);
                        RosterItem i = new RosterItem(account, RosterItem.Type.entry, entry);
                        add(i);
                    }
                    for (String jid : vDnd) {
                        RosterEntry entry = new RosterEntry(jid, StringUtils.parseResource(jid), RosterPacket.ItemType.both, RosterPacket.ItemStatus.SUBSCRIPTION_PENDING, roster, connection);
                        RosterItem i = new RosterItem(account, RosterItem.Type.entry, entry);
                        add(i);
                    }
                }
			}
		}
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		JTalkService service = JTalkService.getInstance();
		IconPicker ip = service.getIconPicker();
		int fontSize = Integer.parseInt(service.getResources().getString(R.string.DefaultFontSize));
		try {
			fontSize = Integer.parseInt(prefs.getString("RosterSize", service.getResources().getString(R.string.DefaultFontSize)));
		} catch (NumberFormatException ignored) { }
		
		RosterItem item = getItem(position);
		if (item.isGroup()) {
			GroupHolder holder;
			if (convertView == null || convertView.getTag() == null || convertView.getTag() instanceof ItemHolder) {
				LayoutInflater inflater = activity.getLayoutInflater();
				convertView = inflater.inflate(R.layout.group, null, false);
				
				holder = new GroupHolder();
	            holder.text = (TextView) convertView.findViewById(R.id.name);
	            holder.text.setTextSize(fontSize-2);
	            holder.text.setTextColor(Colors.DRAWER_GROUP_FOREGROUND);
                holder.text.setEllipsize(TextUtils.TruncateAt.MIDDLE);

	            holder.state = (ImageView) convertView.findViewById(R.id.state);
	            holder.state.setVisibility(View.GONE);
	            convertView.setTag(holder);
	            convertView.setBackgroundColor(Colors.DRAWER_GROUP_BACKGROUND);
			} else {
				holder = (GroupHolder) convertView.getTag();
			}
	        holder.text.setText(item.getName());

            String role = (String) item.getObject();
            switch (role) {
                case "moderator":
                    holder.text.setText(activity.getString(R.string.Moderators) + " " + item.getName());
                    break;
                case "participant":
                    holder.text.setText(activity.getString(R.string.Participants) + " " + item.getName());
                    break;
                default:
                    holder.text.setText(activity.getString(R.string.Visitors) + " " + item.getName());
                    break;
            }

			return convertView;
		} else if (item.isEntry()) {
			String name = item.getName();
			String jid = item.getEntry().getUser();
			if (name == null || name.length() <= 0 ) name = jid;
            int count = service.getMessagesCount(account, jid);
			Presence presence = service.getPresence(item.getAccount(), jid);

            ItemHolder holder = new ItemHolder();
			if(convertView == null || convertView.getTag() == null || convertView.getTag() instanceof GroupHolder) {
				LayoutInflater inflater = activity.getLayoutInflater();
				convertView = inflater.inflate(R.layout.entry, null, false);

				holder.name = (TextView) convertView.findViewById(R.id.name);
				holder.name.setTextSize(fontSize);
				holder.status = (TextView) convertView.findViewById(R.id.status);
                holder.status.setTextSize(fontSize-4);
				holder.status.setVisibility(View.GONE);
				holder.counter = (TextView) convertView.findViewById(R.id.msg_counter);
				holder.counter.setTextSize(fontSize);
				holder.messageIcon = (ImageView) convertView.findViewById(R.id.msg);
				holder.messageIcon.setImageBitmap(ip.getMsgBitmap());
				holder.statusIcon = (ImageView) convertView.findViewById(R.id.status_icon);
				holder.statusIcon.setPadding(3, 3, 0, 0);
				holder.statusIcon.setVisibility(View.VISIBLE);
				holder.avatar = (ImageView) convertView.findViewById(R.id.contactlist_pic);
				holder.caps = (ImageView) convertView.findViewById(R.id.caps);
				convertView.setTag(holder);
			} else {
                holder = (ItemHolder) convertView.getTag();
            }

			holder.name.setText(name);
			if (service.getActiveChats(account).contains(jid)) {
				holder.name.setTypeface(Typeface.DEFAULT_BOLD);
			} else {
                holder.name.setTypeface(Typeface.DEFAULT);
            }

            MUCUser mucUser = (MUCUser) presence.getExtension("x", "http://jabber.org/protocol/muc#user");
            if (mucUser != null) {
                String affiliation = mucUser.getItem().getAffiliation();
                if (affiliation != null) {
                    if (prefs.getBoolean("OldUserList", false)) {
                        switch (affiliation) {
                            case "admin":
                                holder.name.setTextColor(Colors.AFFILIATION_ADMIN);
                                break;
                            case "owner":
                                holder.name.setTextColor(Colors.AFFILIATION_OWNER);
                                break;
                            case "member":
                                holder.name.setTextColor(Colors.AFFILIATION_MEMBER);
                                break;
                            default:
                                holder.name.setTextColor(Colors.AFFILIATION_NONE);
                                break;
                        }
                    } else {
                        switch (affiliation) {
                            case "admin":
                                holder.name.setTextColor(Colors.DRAWER_ENTRY_FOREGROUND);
                                break;
                            case "owner":
                                holder.name.setTextColor(Colors.AFFILIATION_OWNER);
                                break;
                            case "member":
                                holder.name.setTextColor(Colors.DRAWER_ENTRY_FOREGROUND);
                                break;
                            default:
                                holder.name.setTextColor(Colors.DRAWER_SECONDARY_TEXT);
                                break;
                        }
                    }
                }
            }
			
	        if (count > 0) {
	        	holder.messageIcon.setVisibility(View.VISIBLE);
				holder.counter.setVisibility(View.VISIBLE);
				holder.counter.setText(count+"");
			} else {
                holder.messageIcon.setVisibility(View.GONE);
                holder.counter.setVisibility(View.GONE);
			}

            String statusText = service.getStatus(account, jid);
            Roster roster = service.getRoster(account);
            if (roster != null) {
                ChatState state = roster.getChatState(jid);
                if (state != null && state == ChatState.composing) statusText = service.getString(R.string.Composes);
            }

            holder.status.setTextColor(Colors.SECONDARY_TEXT);
            holder.status.setVisibility(statusText.length() > 0 ? View.VISIBLE : View.GONE);
            holder.status.setText(statusText);

            if (ip != null) {
                holder.statusIcon.setImageBitmap(ip.getIconByPresence(presence));
                holder.statusIcon.setVisibility(View.VISIBLE);
            }
            if (prefs.getBoolean("ShowCaps", false)) {
                if (prefs.getBoolean("OldUserList", false) && prefs.getInt("SideBarSize", 200) < (int) (128 * activity.getResources().getDisplayMetrics().density)) {
                    holder.caps.setVisibility(View.GONE);
                } else {
                    String node = service.getNode(account, jid);
                    ClientIcons.loadClientIcon(activity, holder.caps, node);
                }
            } else {
                holder.caps.setVisibility(View.GONE);
            }

            if (mynick != null && mynick.equals(name)) {
                Avatars.loadAvatar(account, jid);
            }

            if (prefs.getBoolean("LoadAvatar", true)) {
                Avatars.loadAvatar(activity, jid.replaceAll("/", "%"), holder.avatar);
            } else {
                holder.avatar.setVisibility(View.GONE);
            }
			return convertView;
		}
        return null;
    }
}
