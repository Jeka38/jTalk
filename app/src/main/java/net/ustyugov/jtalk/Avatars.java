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

package net.ustyugov.jtalk;


import java.io.File;
import java.io.FileOutputStream;
import java.util.Hashtable;
import java.util.Iterator;

import android.view.ViewGroup;
import com.amulyakhare.textdrawable.TextDrawable;
import com.amulyakhare.textdrawable.util.ColorGenerator;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.packet.VCard;

import net.ustyugov.jtalk.service.JTalkService;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.view.View;
import android.widget.ImageView;

public class Avatars {

	public static void loadAvatar(final Activity activity, final String jid, final ImageView image) {
		new Thread() {
			@Override
			public void run() {
				activity.runOnUiThread(new Runnable() {
					public void run() {
					    if (image != null) {
                            JTalkService service = JTalkService.getInstance();
                            Bitmap bitmap;

                            final Hashtable<String, Bitmap> avHash = service.getAvatarsHash();
                            if (avHash.containsKey(jid)) {
                                bitmap = avHash.get(jid);
                            } else {
                                String fname = Constants.PATH + "/" + jid;
                                File file = new File(fname);
                                if (file.exists()) {
                                    int maxWidth = (int) (Constants.AVATAR_SIZE * activity.getResources().getDisplayMetrics().density);
                                    int maxHeight = (int) (Constants.AVATAR_SIZE * activity.getResources().getDisplayMetrics().density);

                                    BitmapFactory.Options options = new BitmapFactory.Options();
                                    options.inJustDecodeBounds = true;
                                    bitmap = BitmapFactory.decodeFile(fname, options);

                                    options.inSampleSize = Pictures.calculateInSampleSize(options, maxWidth, maxHeight);
                                    options.inJustDecodeBounds = false;
                                    bitmap = BitmapFactory.decodeFile(fname, options);

                                    if (bitmap != null) avHash.put(jid, bitmap);
                                } else {
                                    String letter;
                                    String resource = StringUtils.parseResource(jid.replaceAll("%", "/"));
                                    if (!resource.isEmpty()) letter = resource.substring(0, 1);
                                    else letter = jid.substring(0, 1);

                                    ColorGenerator generator = ColorGenerator.MATERIAL; // or use DEFAULT
                                    int color = generator.getColor(letter);

                                    TextDrawable textDrawable = TextDrawable.builder().buildRect(letter, color);
                                    image.setImageDrawable(textDrawable);
                                    image.setVisibility(View.VISIBLE);
                                    return;
                                }
                            }

                            if (bitmap != null) {
                                int value = (int) (Constants.AVATAR_SIZE * activity.getResources().getDisplayMetrics().density);
                                ViewGroup.LayoutParams lp = image.getLayoutParams();
                                lp.width = value;

                                image.setLayoutParams(lp);
                                image.setImageBitmap(bitmap);
                                image.setVisibility(View.VISIBLE);
                            }
                        }
					}
				});
			}
		}.start();
	}

    public static void loadAvatar(String account, String jid) {
        new LoadAvatar(JTalkService.getInstance().getConnection(account), jid).execute();
    }

    private static class LoadAvatar extends AsyncTask<Void, Void, Void> {
        private String jid;
        private XMPPConnection connection;

        private LoadAvatar(XMPPConnection connection, String jid) {
            this.connection = connection;
            this.jid = jid;
        }

        @Override
        protected Void doInBackground(Void... params) {
            File file = new File(Constants.PATH);
            if (file.exists() || file.mkdir()) {
                try {
                    String avatar = jid.replaceAll("/", "%");
                    if (!new File(Constants.PATH + avatar).exists()) {
                        VCard vcard = new VCard();
                        vcard.load(connection, jid);
                        byte[] buffer = vcard.getAvatar();

                        if (buffer != null) {
                            FileOutputStream fos = new FileOutputStream(Constants.PATH + avatar);
                            fos.write(buffer);
                            fos.close();
                        }
                    }
                } catch (Exception ignored) { }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            JTalkService.getInstance().sendBroadcast(new Intent(Constants.UPDATE));
        }
    }
	
	public static void loadAllAvatars(XMPPConnection connection, String group) {
        new LoadAvatar(connection, group).execute(); // Load conference avatar

        String account = StringUtils.parseBareAddress(connection.getUser());
        new LoadAllAvatars(account, connection, group).execute();
	}
	
	private static class LoadAllAvatars extends AsyncTask<Void, Void, Void> {
	    private String account;
        private String group;
		private XMPPConnection connection;

		private LoadAllAvatars(String account, XMPPConnection connection, String group) {
		    this.account = account;
			this.connection = connection;
            this.group = group;
		}
		
		@Override
		protected Void doInBackground(Void... params) {
            JTalkService service = JTalkService.getInstance();
            Roster roster = service.getRoster(account);
            if (roster != null) {
                Iterator<Presence> it = roster.getPresences(group);
                while (it.hasNext()) {
                    Presence p = it.next();
                    String jid = p.getFrom();

                    try {
                        Thread.currentThread();
                        Thread.sleep(10000);

                        File file = new File(Constants.PATH);
                        if (file.exists() || file.mkdir()) {
                            String avatar = jid.replaceAll("/", "%");
                            if (!new File(Constants.PATH + avatar).exists()) {
                                VCard vcard = new VCard();
                                vcard.load(connection, jid);
                                byte[] buffer = vcard.getAvatar();

                                if (buffer != null) {
                                    FileOutputStream fos = new FileOutputStream(Constants.PATH + avatar);
                                    fos.write(buffer);
                                    fos.close();
                                }
                            }
                        }
                    } catch (Exception ignored) { }
                }
            }

			return null;
		}
		
		@Override
		protected void onPostExecute(Void result) {
			JTalkService.getInstance().sendBroadcast(new Intent(Constants.UPDATE));
		}
	}
}
