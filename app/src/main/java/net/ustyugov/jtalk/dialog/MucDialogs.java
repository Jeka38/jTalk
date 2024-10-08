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

package net.ustyugov.jtalk.dialog;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.ustyugov.jtalk.Constants;
import net.ustyugov.jtalk.activity.Chat;
import net.ustyugov.jtalk.activity.CommandsActivity;
import net.ustyugov.jtalk.activity.DataFormActivity;
import net.ustyugov.jtalk.activity.muc.MucUsers;
import net.ustyugov.jtalk.activity.vcard.VCardActivity;
import net.ustyugov.jtalk.adapter.OnlineUsersAdapter;
import net.ustyugov.jtalk.service.JTalkService;

import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.ServiceDiscoveryManager;
import org.jivesoftware.smackx.bookmark.BookmarkedConference;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smackx.muc.Occupant;
import org.jivesoftware.smackx.packet.DiscoverItems;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnClickListener;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import com.jtalk2.R;

public class MucDialogs {

	public static void roomMenu(final Activity activity, final String account, final String group) {
        String affil = "none";
        try {
            JTalkService service = JTalkService.getInstance();
            MultiUserChat muc = service.getConferencesHash(account).get(group);
            Occupant occupant = muc.getOccupant(muc.getRoom() + "/" + muc.getNickname());
            affil = occupant.getAffiliation();
        } catch (Exception ignored) { }

		CharSequence[] items;
        if (affil.equals("owner")) items = new CharSequence[6];
        else if (affil.equals("admin")) items = new CharSequence[5];
        else items = new CharSequence[4];

        items[0] = activity.getString(R.string.Open);
        items[1] = activity.getString(R.string.ChangeNick);
        items[2] = activity.getString(R.string.SendStatus);
        items[3] = activity.getString(R.string.Leave);
        if (affil.equals("owner")) {
            items[4] = activity.getString(R.string.Users);
            items[5] = activity.getString(R.string.Configuration);
        } else if (affil.equals("admin")) {
            items[4] = activity.getString(R.string.Users);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(group);
        builder.setItems(items, new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
		        switch (which) {
		        	case 0:
		        		Intent intent = new Intent(activity, net.ustyugov.jtalk.activity.Chat.class);
		        		intent.putExtra("jid", group);
		        		intent.putExtra("account", account);
		        		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		        		activity.startActivity(intent);
		        		break;
		        	case 1:
		        		MucDialogs.setNickDialog(activity, account, group);
		        		break;
		        	case 2:
		        		RosterDialogs.changeStatusDialog(activity, account, group);
		        		break;
                    case 3:
                        JTalkService.getInstance().leaveRoom(account, group);
                        activity.sendBroadcast(new Intent(Constants.UPDATE));
                        break;
                    case 4:
                        Intent uIntent = new Intent(activity, MucUsers.class);
                        uIntent.putExtra("account", account);
                        uIntent.putExtra("group", group);
                        activity.startActivity(uIntent);
                        break;
		        	case 5:
		        		Intent cIntent = new Intent(activity, DataFormActivity.class);
                        cIntent.putExtra("account", account);
		           	 	cIntent.putExtra("group", group);
		           	 	cIntent.putExtra("muc", true);
		           	 	activity.startActivity(cIntent);
		        		break;
		        }
			}
        });
        builder.create().show();
	}
	
	public static void userMenu(final Activity activity, final String account, final String group, final String nick) {
        boolean isAccountModerator = false;
        boolean isUserModerator = false;
        String accountAffil = "none";
        String userAffil = "none";

        final JTalkService service = JTalkService.getInstance();
        try {
            MultiUserChat muc = service.getConferencesHash(account).get(group);
            Occupant occupant = muc.getOccupant(muc.getRoom() + "/" + muc.getNickname());
            if (occupant.getRole().equals("moderator")) isAccountModerator = true;
            accountAffil = occupant.getAffiliation();

            occupant = muc.getOccupant(group + "/" + nick);
            if (occupant.getRole().equals("moderator")) isUserModerator = true;
            userAffil = occupant.getAffiliation();
        } catch (Exception ignored) { }

		CharSequence[] items;
        if (accountAffil.equals("owner")) items = new CharSequence[6];
        else if (accountAffil.equals("admin") && !userAffil.equals("admin") && !userAffil.equals("owner")) items = new CharSequence[6];
        else {
            if (isAccountModerator && !isUserModerator) items = new CharSequence[5];
            else items = new CharSequence[3];
        }

        items[0] = activity.getString(R.string.Chat);
        items[1] = activity.getString(R.string.Info);
        items[2] = activity.getString(R.string.ExecuteCommand);
        if (accountAffil.equals("owner") || (accountAffil.equals("admin") && !userAffil.equals("admin") && !userAffil.equals("owner"))) {
            items[3] = activity.getString(R.string.Actions);
            items[4] = activity.getString(R.string.Kick);
            items[5] = activity.getString(R.string.Ban);
        } else if (isAccountModerator && !isUserModerator) {
            items[3] = activity.getString(R.string.Actions);
            items[4] = activity.getString(R.string.Kick);
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(group);
        builder.setItems(items, new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
		        switch (which) {
		        	case 0:
		        		Intent intent = new Intent(activity, Chat.class);
		        		intent.putExtra("jid", group + "/" + nick);
		        		intent.putExtra("account", account);
		        		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		        		activity.startActivity(intent);
		        		break;
		        	case 1:
		        		Intent infoIntent = new Intent(activity, VCardActivity.class);
		        		infoIntent.putExtra("jid", group + "/" + nick);
		        		infoIntent.putExtra("account", account);
		        		activity.startActivity(infoIntent);
		        		break;
		        	case 2:
		        		Intent cintent = new Intent(activity, CommandsActivity.class);
                        cintent.putExtra("account", account);
		    			cintent.putExtra("jid", group + "/" + nick);
		    	        activity.startActivity(cintent);
		        		break;
					case 3:
						if (service.getConferencesHash(account).containsKey(group)) {
							MultiUserChat muc = service.getConferencesHash(account).get(group);
							new MucAdminMenu(activity, muc, nick).show();
						}
						break;
                    case 4:
                        if (service.getConferencesHash(account).containsKey(group)) {
                            MultiUserChat muc = service.getConferencesHash(account).get(group);
                            kickDialog(activity, muc, nick);
                        }
                        break;
                    case 5:
                        if (service.getConferencesHash(account).containsKey(group)) {
                            MultiUserChat muc = service.getConferencesHash(account).get(group);
                            String jid = muc.getOccupant(group + "/" + nick).getJid();
                            if (jid != null) banDialog(activity, muc, jid);
                        }
                        break;
		        }
			}
        });
        builder.create().show();
	}
	
	public static void kickDialog(final Activity activity, final MultiUserChat muc, final String nick) {
		LayoutInflater inflater = activity.getLayoutInflater();
		View layout = inflater.inflate(R.layout.set_nick_dialog, (ViewGroup) activity.findViewById(R.id.set_nick_linear));
		
		final EditText et = (EditText) layout.findViewById(R.id.nick_edit);
	    
	    AlertDialog.Builder builder = new AlertDialog.Builder(activity);
		builder.setView(layout);
		builder.setTitle(activity.getString(R.string.Reason));
		builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				try {
					String reason = et.getText().toString();
					muc.kickParticipant(nick, reason);
				} catch (XMPPException e) {
                    Toast.makeText(activity, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                }
			}
		});
		builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});
		builder.create().show();
	}
	
	public static void banDialog(final Activity activity, final MultiUserChat muc, final String jid) {
		LayoutInflater inflater = activity.getLayoutInflater();
		View layout = inflater.inflate(R.layout.set_nick_dialog, (ViewGroup) activity.findViewById(R.id.set_nick_linear));
		
		final EditText et = (EditText) layout.findViewById(R.id.nick_edit);
	    
	    AlertDialog.Builder builder = new AlertDialog.Builder(activity);
		builder.setView(layout);
		builder.setTitle(activity.getString(R.string.Reason));
		builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
					try {
						String reason = et.getText().toString();
						muc.banUser(jid, reason);
					} catch (XMPPException e) {
                        Toast.makeText(activity, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                    }
			}
		});
		builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});
		builder.create().show();
	}
	
	public static void joinDialog(final Activity activity, final String account, String jid, String password) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
		
		LayoutInflater inflater = activity.getLayoutInflater();
		View layout = inflater.inflate(R.layout.muc_join, (ViewGroup) activity.findViewById(R.id.muc_join_linear));
	    
	    final EditText passEdit = (EditText) layout.findViewById(R.id.muc_join_password_entry);
	    if (password != null) passEdit.setText(password);
	    final EditText groupEdit = (EditText) layout.findViewById(R.id.muc_join_group_entry);
	    if (jid != null) groupEdit.setText(jid);
	    else groupEdit.setText(prefs.getString("lastGroup", ""));
	    
	    final EditText nickEdit = (EditText) layout.findViewById(R.id.muc_join_nick_entry);
	    nickEdit.setText(prefs.getString("lastNick", ""));
	    
		AlertDialog.Builder builder = new AlertDialog.Builder(activity);
		builder.setView(layout);
		builder.setTitle(activity.getString(R.string.Join));
		builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				JTalkService service = JTalkService.getInstance();
				String group = groupEdit.getText().toString();
				String nick = nickEdit.getText().toString();
				String pass = passEdit.getText().toString();
				
				if (group.length() > 0) {
					if (nick == null || nick.length() < 1) {
						nick = service.getDerivedNick(service.getConnection(account).getUser(), null);
					}
					service.setPreference("lastGroup", group);
  	  				service.setPreference("lastNick", nick);
  	  				service.joinRoom(account, group, nick, pass);
                    Toast.makeText(activity, "Attempt joining to " + group, Toast.LENGTH_SHORT).show();
  	  				
					Intent i = new Intent(Constants.PRESENCE_CHANGED);
	             	activity.sendBroadcast(i);
				}
			}
		});
		builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});
		builder.create().show();
	}
	
	public static void setNickDialog(final Activity activity, final String account, final String group) {
		final JTalkService service = JTalkService.getInstance();
		
		if (service.getConferencesHash(account).containsKey(group)) {
			final MultiUserChat muc = service.getConferencesHash(account).get(group);
			if (muc.isJoined()) {
				final String oldnick = muc.getNickname();
				
				LayoutInflater inflater = activity.getLayoutInflater();
				View layout = inflater.inflate(R.layout.set_nick_dialog, (ViewGroup) activity.findViewById(R.id.set_nick_linear));
				
				final EditText nickEdit = (EditText) layout.findViewById(R.id.nick_edit);
			    nickEdit.setText(oldnick);
			    
			    AlertDialog.Builder builder = new AlertDialog.Builder(activity);
				builder.setView(layout);
				builder.setTitle(activity.getString(R.string.ChangeNick));
				builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						String newnick = nickEdit.getText().toString();
						if(newnick.length() > 0 && !newnick.equals(oldnick)) {
							try {
								muc.changeNickname(newnick);
								if (service.getJoinedConferences(account).containsKey(group))
									service.getJoinedConferences(account).get(group).setNick(newnick);
							} catch(XMPPException e) {
								Toast.makeText(activity, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
							}
						}
					}
				});
				builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				});
				builder.create().show();
			}
		}
	}
	
	public static void inviteDialog(final Activity activity, final String account, final String group) {
		final OnlineUsersAdapter adapter = new OnlineUsersAdapter(activity);
		
		AlertDialog.Builder builder = new AlertDialog.Builder(activity);
		builder.setTitle("Invite");
		builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				JTalkService service = JTalkService.getInstance();
				final RosterEntry entry = adapter.getItem(which).getEntry();
				
				if (service.getConferencesHash(account).containsKey(group)) {
					final MultiUserChat muc = service.getConferencesHash(account).get(group);
					
					LayoutInflater inflater = activity.getLayoutInflater();
					View layout = inflater.inflate(R.layout.set_nick_dialog, (ViewGroup) activity.findViewById(R.id.set_nick_linear));
					
					final EditText et = (EditText) layout.findViewById(R.id.nick_edit);
				    
				    AlertDialog.Builder builder = new AlertDialog.Builder(activity);
					builder.setView(layout);
					builder.setTitle(activity.getString(R.string.Reason));
					builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							String reason = et.getText().toString();
							muc.invite(entry.getUser(), reason);
						}
					});
					builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
						}
					});
					builder.create().show();
				}
			}
		});
        builder.create().show();
	}
	
	public static void showUsersDialog(final Activity activity, final String account, final BookmarkedConference bc) {
		final JTalkService service = JTalkService.getInstance();
		final String group = bc.getJid();

		try {
			List<String> list = new ArrayList<String>();
			ServiceDiscoveryManager discoManager = ServiceDiscoveryManager.getInstanceFor(service.getConnection(account));
			DiscoverItems items = discoManager.discoverItems(group);
			
			Iterator<DiscoverItems.Item> it = items.getItems();
			while(it.hasNext()) {
				DiscoverItems.Item item = it.next();
                String nick = StringUtils.parseResource(item.getEntityID());
				if (!list.contains(nick)) list.add(nick);
			}
			
			CharSequence[] array = new CharSequence[list.size()];
			list.toArray(array);
			
			AlertDialog.Builder builder = new AlertDialog.Builder(activity);
	        builder.setTitle(group);
	        builder.setItems(array, null);
	        builder.setPositiveButton(R.string.Join, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					String jid  = bc.getJid();
					String nick = bc.getNickname();
					if (nick == null || nick.length() < 1) nick = StringUtils.parseName(service.getConnection(account).getUser());
					String pass = bc.getPassword();
					service.joinRoom(account, jid, nick, pass);
				}
			});
			builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
				}
			});
	        builder.create().show();
		} catch (XMPPException e1) {
			e1.printStackTrace();
		}
	}

    public static void showUsersDialog(final Activity activity, final String account, final String group) {
        final JTalkService service = JTalkService.getInstance();

        try {
            List<String> list = new ArrayList<String>();
            ServiceDiscoveryManager discoManager = ServiceDiscoveryManager.getInstanceFor(service.getConnection(account));
            DiscoverItems items = discoManager.discoverItems(group);

            Iterator<DiscoverItems.Item> it = items.getItems();
            while (it.hasNext()) {
                DiscoverItems.Item item = it.next();
                String nick = StringUtils.parseResource(item.getEntityID());
                if (!list.contains(nick)) list.add(nick);
            }

            CharSequence[] array = new CharSequence[list.size()];
            list.toArray(array);

            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setTitle(group);
            builder.setItems(array, null);
            builder.setPositiveButton(R.string.Join, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    MucDialogs.joinDialog(activity, account, group, null);
                }
            });
            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            builder.create().show();
        } catch (XMPPException e1) {
            e1.printStackTrace();
        }
    }
}
