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

package net.ustyugov.jtalk.dialog;

import android.widget.Toast;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.muc.MultiUserChat;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;

import com.jtalk2.R;
import org.jivesoftware.smackx.muc.Occupant;

public class MucAdminMenu implements OnClickListener {
	private Activity activity;
	private MultiUserChat muc;
	private String nick;
	private String group;

	public MucAdminMenu(Activity activity, MultiUserChat muc, String nick) {
		this.activity = activity;
		this.muc = muc;
		this.nick = nick;
		this.group = muc.getRoom();
	}
	
	public void show() {
        String accountAffil = "none";
        try {
            Occupant occupant = muc.getOccupant(muc.getRoom() + "/" + muc.getNickname());
            accountAffil = occupant.getAffiliation();
        } catch (Exception ignored) { }

        CharSequence[] items = null;
        if (accountAffil.equals("owner")) items = new CharSequence[10];
        else if (accountAffil.equals("admin")) items = new CharSequence[6];
        else items = new CharSequence[2];

		items[0] = activity.getString(R.string.GrantVoice);
        items[1] = activity.getString(R.string.RevokeVoice);
        if (accountAffil.equals("owner")) {
            items[2] = activity.getString(R.string.GrantMember);
            items[3] = activity.getString(R.string.RevokeMember);
            items[4] = activity.getString(R.string.GrantModer);
            items[5] = activity.getString(R.string.RevokeModer);
            items[6] = activity.getString(R.string.GrantAdmin);
            items[7] = activity.getString(R.string.RevokeAdmin);
            items[8] = activity.getString(R.string.GrantOwner);
            items[9] = activity.getString(R.string.RevokeOwner);
        } else if (accountAffil.equals("admin")) {
            items[2] = activity.getString(R.string.GrantMember);
            items[3] = activity.getString(R.string.RevokeMember);
            items[4] = activity.getString(R.string.GrantModer);
            items[5] = activity.getString(R.string.RevokeModer);
        }

		AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(R.string.Actions);
        builder.setItems(items, this);
        builder.create().show();
	}
	
	public void onClick(DialogInterface dialog, int which) { 
		switch (which) {
			case 0:
				try {
					muc.grantVoice(nick);
				} catch (XMPPException e) {
                    Toast.makeText(activity, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                }
				break;
            case 1:
                try {
                    muc.revokeVoice(nick);
                } catch (XMPPException e) {
                    Toast.makeText(activity, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                }
                break;
			case 2:
				try {
					String jid = muc.getOccupant(group + "/" + nick).getJid();
					if (jid != null) muc.grantMembership(jid);
				} catch(XMPPException e) {
                    Toast.makeText(activity, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                }
				break;
            case 3:
                try {
                    String jid = muc.getOccupant(group + "/" + nick).getJid();
                    if (jid != null) muc.revokeMembership(jid);
                } catch (XMPPException e) {
                    Toast.makeText(activity, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                }
                break;
			case 4:
				try {
					muc.grantModerator(nick);
				} catch (XMPPException e) {
                    Toast.makeText(activity, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                }
				break;
            case 5:
                try {
                    muc.revokeModerator(nick);
                } catch (XMPPException e) {
                    Toast.makeText(activity, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                }
                break;
			case 6:
				try {
					String jid = muc.getOccupant(group + "/" + nick).getJid();
					if (jid != null) muc.grantAdmin(jid);
				} catch (XMPPException e) {
                    Toast.makeText(activity, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                }
				break;
            case 7:
                try {
                    String jid = muc.getOccupant(group + "/" + nick).getJid();
                    if (jid != null) muc.revokeAdmin(jid);
                } catch (XMPPException e) {
                    Toast.makeText(activity, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                }
                break;
			case 8:
				try {
					String jid = muc.getOccupant(group + "/" + nick).getJid();
					if (jid != null) muc.grantOwnership(jid);
				} catch (XMPPException e) {
                    Toast.makeText(activity, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                }
				break;
			case 9:
				try {
					String jid = muc.getOccupant(group + "/" + nick).getJid();
					if (jid != null) muc.revokeOwnership(jid);
				} catch (XMPPException e) {
                    Toast.makeText(activity, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                }
				break;
		}
	}
}
