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

package net.ustyugov.jtalk;

import java.io.File;
import java.util.HashMap;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.RingtoneManager;
import net.ustyugov.jtalk.activity.Chat;
import net.ustyugov.jtalk.activity.DataFormActivity;
import net.ustyugov.jtalk.activity.RosterActivity;
import net.ustyugov.jtalk.activity.filetransfer.ReceiveFileActivity;
import net.ustyugov.jtalk.activity.muc.Invite;
import net.ustyugov.jtalk.receivers.DeleteNotificationReceiver;
import net.ustyugov.jtalk.service.JTalkService;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.filetransfer.FileTransfer.Status;
import com.jtalk2.R;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.provider.Settings;

import androidx.core.app.NotificationCompat;

public class Notify {
    private static final HashMap<String, Integer> ids = new HashMap<>();
	public static final int NOTIFICATION = 1;
	private static final int NOTIFICATION_FILE = 2;
	private static final int NOTIFICATION_IN_FILE = 3;
	private static final int NOTIFICATION_FILE_REQUEST = 4;
//	private static final int NOTIFICATION_SUBSCRIBTION = 5;
	private static final int NOTIFICATION_CAPTCHA = 6;
	private static final int NOTIFICATION_INVITE = 7;
    private static final int NOTIFICATION_UPLOAD = 8;
	
	public enum Type {Chat, Conference, Direct}
	
    public static void updateNotify() {
        JTalkService service = JTalkService.getInstance();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(service);

        NotificationManager mng = (NotificationManager) service.getSystemService(Context.NOTIFICATION_SERVICE);

        String mode = prefs.getString("currentMode", "available");
        int pos = prefs.getInt("currentSelection", 0);
        String text = prefs.getString("currentStatus", null);
        String[] statusArray = service.getResources().getStringArray(R.array.statusArray);

        service.setGlobalState(text);
        service.sendBroadcast(new Intent(Constants.UPDATE));

        int icon = R.drawable.stat_online;
        if (mode.equals("available")) {
            icon = R.drawable.stat_online;
        }
        else if (mode.equals("chat")) {
            icon = R.drawable.stat_chat;
        }
        else if (mode.equals("away")) {
            icon = R.drawable.stat_away;
        }
        else if (mode.equals("xa")) {
            icon = R.drawable.stat_xaway;
        }
        else if (mode.equals("dnd")) {
            icon = R.drawable.stat_dnd;
        }

        Intent i = new Intent(service, RosterActivity.class);
        i.setAction(Constants.UPDATE);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        i.putExtra("status", false);
        PendingIntent piRoster = PendingIntent.getActivity(service, 0, i, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(service, Constants.NOTIFICATION_CHANNEL);
        mBuilder.setLargeIcon(BitmapFactory.decodeResource(service.getResources(), R.drawable.ic_launcher));
        mBuilder.setSmallIcon(icon);
        mBuilder.setContentTitle(statusArray[pos]);
        mBuilder.setContentText(text);
        mBuilder.setContentIntent(piRoster);
        mBuilder.setPriority(NotificationCompat.VISIBILITY_PRIVATE);

        mng.notify(NOTIFICATION, mBuilder.build());
    }

    public static void offlineNotify(Context context, String state) {
        if (state == null) state = "";
        Intent i = new Intent(context, RosterActivity.class);
        i.setAction(Intent.ACTION_MAIN);
        i.addCategory(Intent.CATEGORY_LAUNCHER);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(context, 0, i, PendingIntent.FLAG_UPDATE_CURRENT);

        JTalkService service = JTalkService.getInstance();
        service.setGlobalState(state);
        context.sendBroadcast(new Intent(Constants.UPDATE));

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context, Constants.NOTIFICATION_CHANNEL);
        mBuilder.setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_launcher));
        mBuilder.setSmallIcon(R.drawable.stat_offline);
        mBuilder.setContentTitle(context.getString(R.string.app_name));
        mBuilder.setContentText(state);
        mBuilder.setContentIntent(contentIntent);
        mBuilder.setPriority(NotificationCompat.VISIBILITY_PRIVATE);

        NotificationManager mng = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        mng.notify(NOTIFICATION, mBuilder.build());
    }

    public static void connectingNotify(String account) {
        JTalkService service = JTalkService.getInstance();
        Intent i = new Intent(service, RosterActivity.class);
        i.setAction(Intent.ACTION_MAIN);
        i.addCategory(Intent.CATEGORY_LAUNCHER);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(service, 0, i, 0);

        String str = service.getString(R.string.Connecting);
        service.setGlobalState(str + ": " + account);
        service.sendBroadcast(new Intent(Constants.UPDATE));

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(service, Constants.NOTIFICATION_CHANNEL);
        mBuilder.setLargeIcon(BitmapFactory.decodeResource(service.getResources(), R.drawable.ic_launcher));
        mBuilder.setSmallIcon(R.drawable.stat_offline);
        mBuilder.setContentTitle(str);
        mBuilder.setContentText(account);
        mBuilder.setContentIntent(contentIntent);
        mBuilder.setPriority(NotificationCompat.VISIBILITY_PRIVATE);
        mBuilder.setProgress(0, 0, true);

        NotificationManager mng = (NotificationManager) service.getSystemService(Context.NOTIFICATION_SERVICE);
        mng.notify(NOTIFICATION, mBuilder.build());
    }

    public static void cancelAll(Context context) {
    	NotificationManager mng = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    	mng.cancelAll();
    }

    public static void cancelNotify(Context context, String account, String jid) {
        String key = account + "/" + jid;
        if (ids.containsKey(key)) {
            NotificationManager mng = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            mng.cancel(ids.get(key));
            ids.remove(key);
        }
    }

    public static void messageNotify(String account, String fullJid, Type type, String text) {
        JTalkService service = JTalkService.getInstance();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(service);

        if (prefs.getBoolean("soundDisabled", false)) return;

        String currentJid = JTalkService.getInstance().getCurrentJid();
        String from = fullJid;
        if (type == Type.Direct) {
            from = StringUtils.parseBareAddress(fullJid);
            text = StringUtils.parseResource(fullJid) + ": " + text;
        }

        String ignored = prefs.getString("IgnoreJids","");
        if (ignored.toLowerCase().contains(from.toLowerCase())) return;

        int color = Color.GREEN;
        try {
            color = Integer.parseInt(prefs.getString("lightsColor", Color.GREEN+""));
        } catch (NumberFormatException nfe) {}

    	String nick = from;
        if (service.getConferencesHash(account).containsKey(from)) {
            nick = StringUtils.parseName(from);
        } else if (service.getConferencesHash(account).containsKey(StringUtils.parseBareAddress(from))) {
            nick = StringUtils.parseResource(from);
        } else {
            Roster roster = JTalkService.getInstance().getRoster(account);
            if (roster != null) {
                RosterEntry re = roster.getEntry(from);
                if (re != null && re.getName() != null) nick = re.getName();
            }
        }

    	String vibration = prefs.getString("vibrationMode", "3");

        String soundPath = "";

    	boolean vibro = false;
    	boolean sound = true;

        if (Build.VERSION.SDK_INT < 26) {
            if (type == Type.Conference) {
                if (!currentJid.equals(from) || currentJid.equals("me")) {
                    if (vibration.equals("1") || vibration.equals("4")) {
                        Vibrator vibrator = (Vibrator) service.getSystemService(Context.VIBRATOR_SERVICE);
                        vibrator.vibrate(200);
                    }
                    new SoundTask().execute("");
                }
                return;
            } else if (type == Type.Direct) {
                if (vibration.equals("1") || vibration.equals("3") || vibration.equals("4")) vibro = true;
                soundPath = prefs.getString("ringtone_direct", RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION).toString());
            } else {
                if (vibration.equals("1") || vibration.equals("2") || vibration.equals("3")) vibro = true;
                soundPath = prefs.getString("ringtone", RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION).toString());
            }

            if (soundPath.equals("")) sound = false;
        }

    	if (!currentJid.equals(from) || currentJid.equals("me")) {
            String ticker = service.getString(R.string.NewMessageFrom) + " " + nick;

            String key = account + "/" + from;
            int id = 11 + ids.size();
            if (ids.containsKey(key)) id = ids.get(key);
            else ids.put(key, id);

        	Intent i = new Intent(service, Chat.class);
        	i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            i.setAction(String.valueOf(ids));
            i.putExtra("jid", from);
        	i.putExtra("account", account);
            PendingIntent contentIntent = PendingIntent.getActivity(service, 0, i, PendingIntent.FLAG_UPDATE_CURRENT);

            Bitmap largeIcon = BitmapFactory.decodeResource(service.getResources(), R.drawable.stat_msg);
            String filePath = Constants.PATH + fullJid.replaceAll("/", "%");
            File a = new File(filePath);
            if (a.exists()) {
                largeIcon = BitmapFactory.decodeFile(filePath);

                int width = largeIcon.getWidth();
                if (width > 96)  {
                    double k = (double)width/(double)96;
                    int h = (int) (largeIcon.getHeight()/k);
                    largeIcon = Bitmap.createScaledBitmap(largeIcon, 96, h, true);
                }
            }

            String channelId = Constants.NOTIFICATION_CHANNEL_MESSAGE;
            if (type == Type.Conference) channelId = Constants.NOTIFICATION_CHANNEL_GROUP_MESSAGE;
            else if (type == Type.Direct) channelId = Constants.NOTIFICATION_CHANNEL_HIGHLIGHT_MESSAGE;

            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(service, channelId);
            mBuilder.setLargeIcon(largeIcon);
            mBuilder.setSmallIcon(R.drawable.stat_msg);
            mBuilder.setLights(color, 2000, 3000);
            mBuilder.setContentTitle(nick);
            mBuilder.setContentText(text);
            mBuilder.setContentIntent(contentIntent);
            mBuilder.setTicker(ticker);
            mBuilder.setNumber(service.getMessagesCount(account, from));
            mBuilder.setPriority(NotificationCompat.PRIORITY_MAX);
            if (vibro) mBuilder.setVibrate(new long[] {200, 200});
            if (sound) mBuilder.setSound(Uri.parse(soundPath));

            if (prefs.getBoolean("MakeChatRead", true)) {
                Intent d = new Intent(service, DeleteNotificationReceiver.class);
                d.setAction(String.valueOf(ids));
                d.putExtra("jid", from);
                d.putExtra("account", account);
                PendingIntent deleteIntent = PendingIntent.getBroadcast(service, 0, d, 0);

                mBuilder.setDeleteIntent(deleteIntent);
            }

            NotificationCompat.BigTextStyle bts = new NotificationCompat.BigTextStyle();
            bts.setBigContentTitle(nick);
            bts.bigText(text);
            mBuilder.setStyle(bts);

            NotificationManager mng = (NotificationManager) service.getSystemService(Context.NOTIFICATION_SERVICE);
            mng.notify(id, mBuilder.build());
    	}
    }

    public static void uploadFileProgress(Status status, String text) {
        JTalkService service = JTalkService.getInstance();
        Intent i = new Intent(service, RosterActivity.class);
        i.setAction(Intent.ACTION_MAIN);
        i.addCategory(Intent.CATEGORY_LAUNCHER);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(service, 0, i, 0);

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(service, Constants.NOTIFICATION_CHANNEL_FILES);
        mBuilder.setContentTitle(service.getString(R.string.SendFile));
        mBuilder.setContentText(text);
        mBuilder.setContentIntent(contentIntent);

        if (status == Status.in_progress) {
            mBuilder.setProgress(0, 0, true);
            mBuilder.setSmallIcon(android.R.drawable.stat_sys_upload);
            mBuilder.setTicker(service.getString(R.string.SendFile));
        } else if (status == Status.error) {
            mBuilder.setSmallIcon(android.R.drawable.stat_sys_warning);
            mBuilder.setTicker(service.getString(R.string.Error));
            mBuilder.setAutoCancel(true);
        }

        NotificationManager mng = (NotificationManager) service.getSystemService(Context.NOTIFICATION_SERVICE);
        mng.notify(NOTIFICATION_UPLOAD, mBuilder.build());
    }

    public static void uploadCancel() {
        NotificationManager mng = (NotificationManager) JTalkService.getInstance().getSystemService(Context.NOTIFICATION_SERVICE);
        mng.cancel(NOTIFICATION_UPLOAD);
    }

    public static void fileProgress(String filename, Status status) {
    	JTalkService service = JTalkService.getInstance();

    	Intent i = new Intent(service, RosterActivity.class);
    	i.setAction(Intent.ACTION_MAIN);
        i.addCategory(Intent.CATEGORY_LAUNCHER);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(service, 0, i, 0);

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(service, Constants.NOTIFICATION_CHANNEL_FILES);
        mBuilder.setContentTitle(filename);
        mBuilder.setContentIntent(contentIntent);
        mBuilder.setOngoing(false);

        if (status == Status.complete) {
        	mBuilder.setSmallIcon(android.R.drawable.stat_sys_download_done);
        	mBuilder.setTicker(service.getString(R.string.Completed));
        	mBuilder.setContentText(service.getString(R.string.Completed));
        	mBuilder.setAutoCancel(true);
        } else if (status == Status.cancelled) {
        	mBuilder.setSmallIcon(android.R.drawable.stat_sys_warning);
        	mBuilder.setTicker(service.getString(R.string.Canceled));
        	mBuilder.setContentText(service.getString(R.string.Canceled));
        	mBuilder.setAutoCancel(true);
        } else if (status == Status.refused) {
        	mBuilder.setSmallIcon(android.R.drawable.stat_sys_warning);
        	mBuilder.setTicker(service.getString(R.string.Canceled));
        	mBuilder.setContentText(service.getString(R.string.Canceled));
        	mBuilder.setAutoCancel(true);
        } else if (status == Status.negotiating_transfer) {
        	mBuilder.setSmallIcon(android.R.drawable.stat_sys_download_done);
        	mBuilder.setTicker(service.getString(R.string.Waiting));
        	mBuilder.setContentText(service.getString(R.string.Waiting));
        } else if (status == Status.in_progress) {
        	mBuilder.setSmallIcon(android.R.drawable.stat_sys_download);
        	mBuilder.setTicker(service.getString(R.string.Downloading));
        	mBuilder.setContentText(service.getString(R.string.Downloading));
        } else if (status == Status.error) {
        	mBuilder.setSmallIcon(android.R.drawable.stat_sys_warning);
        	mBuilder.setTicker(service.getString(R.string.Error));
        	mBuilder.setContentText(service.getString(R.string.Error));
        	mBuilder.setAutoCancel(true);
        } else {
        	return;
        }

    	NotificationManager mng = (NotificationManager) service.getSystemService(Context.NOTIFICATION_SERVICE);
        mng.notify(NOTIFICATION_FILE, mBuilder.build());
    }

    public static void incomingFile() {
    	JTalkService service = JTalkService.getInstance();
    	Intent i = new Intent(service, ReceiveFileActivity.class);
    	i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(service, 0, i, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(service, Constants.NOTIFICATION_CHANNEL_MESSAGE);
        mBuilder.setSmallIcon(android.R.drawable.stat_sys_warning);
        mBuilder.setLights(0xFF0000FF, 2000, 3000);
        mBuilder.setContentTitle(service.getString(R.string.app_name));
        mBuilder.setContentText(service.getString(R.string.AcceptFile));
        mBuilder.setContentIntent(contentIntent);
        mBuilder.setAutoCancel(true);

    	NotificationManager mng = (NotificationManager) service.getSystemService(Context.NOTIFICATION_SERVICE);
        mng.notify(NOTIFICATION_FILE_REQUEST, mBuilder.build());
    }

    public static void inviteNotify(String account, String room, String from, String reason, String password) {
    	JTalkService service = JTalkService.getInstance();

    	Intent i = new Intent(service, Invite.class);
    	i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
    	i.putExtra("account", account);
        i.putExtra("room", room);
        i.putExtra("from", from);
        i.putExtra("reason", reason);
        i.putExtra("password", password);
        PendingIntent contentIntent = PendingIntent.getActivity(service, 0, i, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(service, Constants.NOTIFICATION_CHANNEL_MESSAGE);
        mBuilder.setSmallIcon(R.drawable.icon_muc);
        mBuilder.setLights(0xFF0000FF, 2000, 3000);
        mBuilder.setAutoCancel(true);
        mBuilder.setTicker(service.getString(R.string.InviteTo) + " " + room);
        mBuilder.setContentTitle(service.getString(R.string.InviteTo));
        mBuilder.setContentText(room);
        mBuilder.setContentIntent(contentIntent);

    	NotificationManager mng = (NotificationManager) service.getSystemService(Context.NOTIFICATION_SERVICE);
        mng.notify(NOTIFICATION_INVITE, mBuilder.build());
    }

    public static void incomingFileProgress(String filename, Status status) {
    	JTalkService service = JTalkService.getInstance();

    	Intent i = new Intent(service, RosterActivity.class);
    	i.setAction(Intent.ACTION_MAIN);
        i.addCategory(Intent.CATEGORY_LAUNCHER);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent contentIntent = PendingIntent.getActivity(service, 0, i, 0);

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(service, Constants.NOTIFICATION_CHANNEL_FILES);
        mBuilder.setContentTitle(filename);
        mBuilder.setContentIntent(contentIntent);

        if (status == Status.complete) {
        	mBuilder.setSmallIcon(android.R.drawable.stat_sys_download_done);
        	mBuilder.setTicker(service.getString(R.string.Completed));
        	mBuilder.setContentText(service.getString(R.string.Completed));
        	mBuilder.setAutoCancel(true);
        	mBuilder.setOngoing(false);
        } else if (status == Status.cancelled) {
        	mBuilder.setSmallIcon(android.R.drawable.stat_sys_warning);
        	mBuilder.setTicker(service.getString(R.string.Canceled));
        	mBuilder.setContentText(service.getString(R.string.Canceled));
        	mBuilder.setAutoCancel(true);
        	mBuilder.setOngoing(false);
        } else if (status == Status.refused) {
        	mBuilder.setSmallIcon(android.R.drawable.stat_sys_warning);
        	mBuilder.setTicker(service.getString(R.string.Canceled));
        	mBuilder.setContentText(service.getString(R.string.Canceled));
        	mBuilder.setAutoCancel(true);
        	mBuilder.setOngoing(false);
        } else if (status == Status.negotiating_transfer) {
        	mBuilder.setOngoing(true);
        	mBuilder.setSmallIcon(android.R.drawable.stat_sys_download_done);
        	mBuilder.setTicker(service.getString(R.string.Waiting));
        	mBuilder.setContentText(service.getString(R.string.Waiting));
        } else if (status == Status.in_progress) {
        	mBuilder.setOngoing(true);
        	mBuilder.setSmallIcon(android.R.drawable.stat_sys_download);
        	mBuilder.setTicker(service.getString(R.string.Downloading));
        	mBuilder.setContentText(service.getString(R.string.Downloading));
        } else if (status == Status.error) {
        	mBuilder.setSmallIcon(android.R.drawable.stat_sys_warning);
        	mBuilder.setTicker(service.getString(R.string.Error));
        	mBuilder.setContentText(service.getString(R.string.Error));
        	mBuilder.setAutoCancel(true);
        	mBuilder.setOngoing(false);
        } else {
        	return;
        }

    	NotificationManager mng = (NotificationManager) service.getSystemService(Context.NOTIFICATION_SERVICE);
        mng.notify(NOTIFICATION_IN_FILE, mBuilder.build());
    }

    public static void cancelFileRequest() {
    	NotificationManager mng = (NotificationManager) JTalkService.getInstance().getSystemService(Context.NOTIFICATION_SERVICE);
    	mng.cancel(NOTIFICATION_FILE_REQUEST);
    }

    public static void captchaNotify(String account, MessageItem message) {
    	JTalkService service = JTalkService.getInstance();

    	Intent intent = new Intent(service, DataFormActivity.class);
        intent.putExtra("account", account);
    	intent.putExtra("id", message.getId());
    	intent.putExtra("cap", true);
        intent.putExtra("jid", message.getName());
        intent.putExtra("bob", message.getBob().getData());
        intent.putExtra("cid", message.getBob().getCid());
        PendingIntent contentIntent = PendingIntent.getActivity(service, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        String str = "Captcha";

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(service, Constants.NOTIFICATION_CHANNEL_MESSAGE);
        mBuilder.setAutoCancel(true);
        mBuilder.setOngoing(false);
        mBuilder.setSmallIcon(R.drawable.icon_muc);
        mBuilder.setLights(0xFF0000FF, 2000, 3000);
        mBuilder.setContentTitle(str);
        mBuilder.setContentText(message.getBody());
        mBuilder.setContentIntent(contentIntent);
        mBuilder.setTicker(str);

        NotificationManager mng = (NotificationManager) service.getSystemService(Context.NOTIFICATION_SERVICE);
        mng.notify(NOTIFICATION_CAPTCHA, mBuilder.build());
    }

    public static void passwordNotify(String account) {
        JTalkService service = JTalkService.getInstance();
        Intent i = new Intent(service, RosterActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        i.putExtra("password", true);
        i.putExtra("account", account);
        PendingIntent contentIntent = PendingIntent.getActivity(service, 0, i, PendingIntent.FLAG_UPDATE_CURRENT);

        String str = "Enter password!";

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(service, Constants.NOTIFICATION_CHANNEL);
        mBuilder.setAutoCancel(true);
        mBuilder.setOngoing(false);
        mBuilder.setSmallIcon(android.R.drawable.stat_sys_warning);
        mBuilder.setLights(0xFFFF0000, 2000, 3000);
        mBuilder.setContentTitle(service.getString(R.string.app_name));
        mBuilder.setContentText(str);
        mBuilder.setTicker(str);
        mBuilder.setContentIntent(contentIntent);

        NotificationManager mng = (NotificationManager) service.getSystemService(Context.NOTIFICATION_SERVICE);
        mng.notify(Integer.parseInt((System.currentTimeMillis()+"").substring(7)), mBuilder.build());
    }

    public static void subscribtionNotify(Context context, String account, String from) {
        Intent i = new Intent(context, RosterActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        i.putExtra("subscribtion", true);
        i.putExtra("account", account);
        i.putExtra("jid", from);
        PendingIntent contentIntent = PendingIntent.getActivity(context, 0, i, PendingIntent.FLAG_UPDATE_CURRENT);

        String soundPath = "";
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if (!prefs.getBoolean("soundDisabled", false)) {
            soundPath = prefs.getString("ringtone", "");
        }

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context, Constants.NOTIFICATION_CHANNEL);
        mBuilder.setAutoCancel(true);
        mBuilder.setOngoing(false);
        mBuilder.setSmallIcon(R.drawable.noface);
        mBuilder.setLights(0xFF0000FF, 2000, 3000);
        mBuilder.setContentTitle(from);
        mBuilder.setContentText("Subscription request");
        mBuilder.setContentIntent(contentIntent);
        mBuilder.setTicker("Subscription request from " + from);
        mBuilder.setVibrate(new long[] {200, 200});
        if (!soundPath.isEmpty()) mBuilder.setSound(Uri.parse(soundPath));

        NotificationManager mng = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        mng.notify(Integer.parseInt((System.currentTimeMillis()+"").substring(7)), mBuilder.build());
    }
}
