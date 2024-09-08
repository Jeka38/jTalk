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

import android.content.ContentUris;
import android.content.SharedPreferences;
import android.net.Uri;
import net.ustyugov.jtalk.db.AttachmentsDbHelper;
import net.ustyugov.jtalk.db.JTalkProvider;
import net.ustyugov.jtalk.db.MessageDbHelper;
import net.ustyugov.jtalk.service.JTalkService;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.preference.PreferenceManager;

import org.jivesoftware.smack.util.StringUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

public class MessageLog {
	
	public static void writeMessage(String account, String jid, MessageItem message) {
        JTalkService service = JTalkService.getInstance();

        if (message.getType() == MessageItem.Type.message || message.getType() == MessageItem.Type.important) {
            ContentValues values = new ContentValues();
            values.put(MessageDbHelper.TYPE, message.getType().name());
            values.put(MessageDbHelper.JID, jid);
            values.put(MessageDbHelper.ID, message.getId());
            values.put(MessageDbHelper.STAMP, message.getTime());
            values.put(MessageDbHelper.NICK, message.getName());
            values.put(MessageDbHelper.BODY, message.getBody());
            values.put(MessageDbHelper.COLLAPSED, false);
            values.put(MessageDbHelper.RECEIVED, message.isReceived() ? "true" : "false");
            values.put(MessageDbHelper.FORM, "NULL");
            values.put(MessageDbHelper.BOB, "NULL");

            Uri insert = service.getContentResolver().insert(JTalkProvider.CONTENT_URI, values);

            message.setBaseId(String.valueOf(ContentUris.parseId(insert)));
        }

        List<MessageItem> list = service.getMessageList(account, jid);
        if (message.getType() == MessageItem.Type.status || message.getType() == MessageItem.Type.connectionstatus) {
            if (service.getActiveChats(account).contains(jid)) list.add(message);
        } else {
            if (!service.getActiveChats(account).contains(jid)) service.addActiveChat(account, jid);
            list.add(message);
        }
        service.setMessageList(account, jid, list);

        service.sendBroadcast(new Intent(Constants.NEW_MESSAGE).putExtra("jid", jid));

        loadAttachments(message.getBody(), jid);
    }
	
	public static void writeMucMessage(String account, final String group, final String nick, final MessageItem message) {
        JTalkService service = JTalkService.getInstance();

        if (message.getType() == MessageItem.Type.message || message.getType() == MessageItem.Type.important) {
            ContentValues values = new ContentValues();
            values.put(MessageDbHelper.TYPE, message.getType().name());
            values.put(MessageDbHelper.JID, group);
            values.put(MessageDbHelper.ID, message.getId());
            values.put(MessageDbHelper.STAMP, message.getTime());
            values.put(MessageDbHelper.NICK, nick);
            values.put(MessageDbHelper.BODY, message.getBody());
            values.put(MessageDbHelper.COLLAPSED, false);
            values.put(MessageDbHelper.RECEIVED, message.isReceived() ? "true" : "false");
            values.put(MessageDbHelper.FORM, "NULL");
            values.put(MessageDbHelper.BOB, "NULL");

            Uri insert = service.getContentResolver().insert(JTalkProvider.CONTENT_URI, values);

            message.setBaseId(String.valueOf(ContentUris.parseId(insert)));
        }

        List<MessageItem> list = service.getMessageList(account, group);
        list.add(message);
        service.setMessageList(account, group, list);

        service.sendBroadcast(new Intent(Constants.NEW_MESSAGE).putExtra("jid", group));

        loadAttachments(message.getBody(), group);
	}
	
	public static void editMessage(final String account, final String jid, final String rid, final String text) {
		final JTalkService service = JTalkService.getInstance();
		new Thread() {
			@Override
			public void run() {
                String[] selectionArgs = {jid, rid};
                Cursor cursor = service.getContentResolver().query(JTalkProvider.CONTENT_URI, null, "jid = ? AND id = ?", selectionArgs, MessageDbHelper._ID);
                if (cursor != null && cursor.getCount() > 0 && text != null && text.length() > 0) {
                    cursor.moveToLast();
                    String _id = cursor.getString(cursor.getColumnIndex(MessageDbHelper._ID));
                    String id = cursor.getString(cursor.getColumnIndex(MessageDbHelper.ID));
                    String nick = cursor.getString(cursor.getColumnIndex(MessageDbHelper.NICK));
                    String type = cursor.getString(cursor.getColumnIndex(MessageDbHelper.TYPE));
                    String stamp = cursor.getString(cursor.getColumnIndex(MessageDbHelper.STAMP));
                    String received = cursor.getString(cursor.getColumnIndex(MessageDbHelper.RECEIVED));
                    cursor.close();

                    ContentValues values = new ContentValues();
                    values.put(MessageDbHelper.TYPE, type);
                    values.put(MessageDbHelper.JID, jid);
                    values.put(MessageDbHelper.ID, id);
                    values.put(MessageDbHelper.STAMP, stamp);
                    values.put(MessageDbHelper.NICK, nick);
                    values.put(MessageDbHelper.BODY, text);
                    values.put(MessageDbHelper.COLLAPSED, false);
                    values.put(MessageDbHelper.RECEIVED, received);
                    values.put(MessageDbHelper.FORM, "NULL");
                    values.put(MessageDbHelper.BOB, "NULL");

                    service.getContentResolver().update(JTalkProvider.CONTENT_URI, values, "_ID = '" + _id + "'", null);

                    List<MessageItem> list = service.getMessageList(account, jid);
                    for (MessageItem item : list) {
                        if (item != null && item.getId() != null && item.getId().equals(rid)) {
                            item.setBody(text);
                            item.setEdited(true);
                        }
                    }
                    service.sendBroadcast(new Intent(Constants.NEW_MESSAGE).putExtra("jid", jid));
                }
			}
		}.start();
	}

    public static void editMucMessage(final String account, final String from, final String rid, final String text) {
        final String group = StringUtils.parseBareAddress(from);
        final String nick = StringUtils.parseResource(from);
        final JTalkService service = JTalkService.getInstance();
        new Thread() {
            @Override
            public void run() {
                String[] selectionArgs = {group, rid};
                Cursor cursor = service.getContentResolver().query(JTalkProvider.CONTENT_URI, null, "jid = ? AND id = ?", selectionArgs, MessageDbHelper._ID);
                if (cursor != null && cursor.getCount() > 0 && text != null && text.length() > 0) {
                    cursor.moveToLast();
                    String _id = cursor.getString(cursor.getColumnIndex(MessageDbHelper._ID));
                    String type = cursor.getString(cursor.getColumnIndex(MessageDbHelper.TYPE));
                    String stamp = cursor.getString(cursor.getColumnIndex(MessageDbHelper.STAMP));
                    cursor.close();

                    ContentValues values = new ContentValues();
                    values.put(MessageDbHelper.TYPE, type);
                    values.put(MessageDbHelper.JID, group);
                    values.put(MessageDbHelper.ID, rid);
                    values.put(MessageDbHelper.STAMP, stamp);
                    values.put(MessageDbHelper.NICK, nick);
                    values.put(MessageDbHelper.BODY, text);
                    values.put(MessageDbHelper.COLLAPSED, false);
                    values.put(MessageDbHelper.RECEIVED, false);
                    values.put(MessageDbHelper.FORM, "NULL");
                    values.put(MessageDbHelper.BOB, "NULL");

                    service.getContentResolver().update(JTalkProvider.CONTENT_URI, values, "_ID = '" + _id + "'", null);

                    List<MessageItem> list = service.getMessageList(account, group);
                    for (MessageItem item : list) {
                        String msgID = item.getId();
                        if (rid.equals(msgID)) {
                            item.setBody(text);
                            item.setEdited(true);
                        }
                    }
                    service.sendBroadcast(new Intent(Constants.NEW_MESSAGE).putExtra("jid", group));
                }
            }
        }.start();
    }

    private static void loadAttachments(final String text, final String jid) {
	    if (text == null || text.isEmpty()) return;

        final JTalkService service = JTalkService.getInstance();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(service);
        if (prefs.getBoolean("PreloadLinks", false)) {
            new Thread() {
                @Override
                public void run() {
                    List<String> urlList = new ArrayList<>();

                    Matcher m = Constants.LINK_PATTERN.matcher(text);
                    int startOffset = 0;
                    int endOffset = 0;
                    while (m.find()) {
                        int start = m.start()+startOffset;
                        int end = m.end()+endOffset;

                        String url = text.substring(start, end);
                        String[] selectionArgs = {url};
                        Cursor cursor = service.getContentResolver().query(JTalkProvider.ATTACHMENTS_URI, null, "url = ?", selectionArgs, MessageDbHelper._ID);
                        if (cursor != null && cursor.getCount() == 0) {
                            urlList.add(url);
                            cursor.close();
                        }
                    }

                    if (urlList.size() > 0) {
                        JSONArray jsonArray = new JSONArray();
                        jsonArray.addAll(urlList);

                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put("urls", jsonArray);

                        HttpURLConnection conn = null;
                        try {
                            conn = (HttpURLConnection) new URL(Constants.PREVIEW_SERVICE).openConnection();
                            conn.setDoOutput(true);
                            conn.setDoInput(true);
                            conn.setUseCaches(false);
                            conn.setRequestMethod("POST");

                            byte[] bytes = jsonObject.toString().getBytes();

                            DataOutputStream out = new DataOutputStream(conn.getOutputStream());
                            out.write(bytes, 0, bytes.length);
                            out.flush();
                            out.close();

                            if (conn.getResponseCode() == 200) {
                                Reader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));

                                StringBuilder sb = new StringBuilder();
                                for (int c; (c = in.read()) >= 0;)  sb.append((char)c);

                                JSONObject jsonResponse = (JSONObject) new JSONParser().parse(sb.toString());

                                JSONArray jsonPreviews = (JSONArray) jsonResponse.get("previews");
                                if (jsonPreviews != null) {
                                    for(Object object : jsonPreviews) {
                                        JSONObject preview = (JSONObject) object;

                                        String url = (String) preview.get("url");
                                        String preview_url = (String) preview.get("preview_url");
                                        String type = (String) preview.get("type");
                                        String title = (String) preview.get("title");
                                        String description = (String) preview.get("description");
                                        String length = (String) preview.get("length");

                                        String file = url.substring(url.lastIndexOf("/")+1);
                                        try {
                                            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
                                            messageDigest.reset();
                                            messageDigest.update(url.getBytes(Charset.forName("UTF8")));
                                            byte[] resultByte = messageDigest.digest();
                                            BigInteger bigInt = new BigInteger(1,resultByte);
                                            file = bigInt.toString(16);
                                        } catch (NoSuchAlgorithmException ignored) {}

                                        String fname = Constants.PATH + "Pictures/" + file;
                                        File myFile = new File(fname);
                                        if (!myFile.exists() && !preview_url.isEmpty()) {
                                            try {
                                                File folder = new File(Constants.PATH + "Pictures/");
                                                if (!folder.exists()) folder.mkdirs();

                                                BufferedInputStream fin = new BufferedInputStream(new URL(preview_url).openStream());
                                                FileOutputStream fout = new FileOutputStream(fname);

                                                byte[] data = new byte[1024];
                                                int count;
                                                while ((count = fin.read(data, 0, 1024)) != -1) {
                                                    fout.write(data, 0, count);
                                                }
                                                fin.close();
                                                fout.close();
                                            } catch (Exception ignored) { }
                                        }

                                        ContentValues values = new ContentValues();
                                        values.put(AttachmentsDbHelper.URL, url);
                                        values.put(AttachmentsDbHelper.TYPE, type);
                                        values.put(AttachmentsDbHelper.TITLE, title);
                                        values.put(AttachmentsDbHelper.DESCRIPTION, description);
                                        values.put(AttachmentsDbHelper.LENGTH, length);

                                        service.getContentResolver().insert(JTalkProvider.ATTACHMENTS_URI, values);
                                        service.sendBroadcast(new Intent(Constants.NEW_MESSAGE).putExtra("jid", jid));
                                    }
                                }
                            }
                        }
                        catch (Exception ignored) { }
                        finally {
                            if (conn != null) conn.disconnect();
                        }
                    }
                }
            }.start();
        }
    }
}
