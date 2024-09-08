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

import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.widget.AdapterView;

import net.ustyugov.jtalk.RosterItem;
import net.ustyugov.jtalk.activity.Chat;
import net.ustyugov.jtalk.dialog.MucDialogs;
import net.ustyugov.jtalk.dialog.RosterDialogs;
import net.ustyugov.jtalk.service.JTalkService;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.util.StringUtils;

public class DrawerClickListener implements AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener{
    private Activity activity;

    public DrawerClickListener(Activity activity) {
        this.activity = activity;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long arg3) {
        JTalkService service = JTalkService.getInstance();
        if (position > 0) {
            RosterItem item = (RosterItem) parent.getItemAtPosition(position);
            String j = null;
            if (item.isEntry()) j = item.getEntry().getUser();
            else if (item.isMuc()) j = item.getName();
            if (j != null && !j.equals(service.getCurrentJid())) {
                Intent intent = new Intent(activity, Chat.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.putExtra("jid", j);
                intent.putExtra("account", item.getAccount());
                activity.startActivity(intent);
            }
        }
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View v, int position, long id) {
        JTalkService service = JTalkService.getInstance();
        if (position > 0) {
            RosterItem item = (RosterItem) parent.getItemAtPosition(position);
            if (item.isEntry()) {
                RosterEntry entry = item.getEntry();
                if (entry != null) {
                    String j = entry.getUser();
                    if (service.getConferencesHash(item.getAccount()).containsKey(j)) {
                        String group = StringUtils.parseBareAddress(j);
                        String nick = StringUtils.parseResource(j);
                        MucDialogs.userMenu(activity, item.getAccount(), group, nick);
                    } else RosterDialogs.ContactMenuDialog(activity, item);
                }
            } else if (item.isMuc()) {
                MucDialogs.roomMenu(activity, item.getAccount(), item.getName());
            }
        }
        return true;
    }
}
