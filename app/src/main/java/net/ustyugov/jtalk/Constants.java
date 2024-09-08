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

import android.os.Environment;
import android.util.Patterns;

import java.util.regex.Pattern;

public final class Constants {
	public static final String PREVIEW_SERVICE = "https://ustyugov.net/api/preview.php";

	public static final Pattern LINK_PATTERN = Patterns.WEB_URL;

	public static final String NOTIFICATION_CHANNEL = "net.ustyugov.jtalk";
	public static final String NOTIFICATION_CHANNEL_MESSAGE  = "net.ustyugov.jtalk.message";
	public static final String NOTIFICATION_CHANNEL_GROUP_MESSAGE  = "net.ustyugov.jtalk.message.groupchat";
	public static final String NOTIFICATION_CHANNEL_HIGHLIGHT_MESSAGE  = "net.ustyugov.jtalk.message.highlight";
	public static final String NOTIFICATION_CHANNEL_FILES  = "net.ustyugov.jtalk.files";

	public static final String PATH = Environment.getExternalStorageDirectory().getAbsolutePath() + "/com.jtalk2/cache/";
	public static final String PATH_SMILES = Environment.getExternalStorageDirectory().getAbsolutePath() + "/com.jtalk2/smiles/";
    public static final String PATH_COLORS = Environment.getExternalStorageDirectory().getAbsolutePath() + "/com.jtalk2/colors/";
	public static final String PATH_LOG = Environment.getExternalStorageDirectory().getAbsolutePath() + "/com.jtalk2/log/";
	
//	public static final int MAX_MUC_MESSAGES = 100;
	public static final long PING_DELAY = 45000;

	public static final int AVATAR_SIZE = 42;

	
	// Statuses
	public static final int STATUS_ONLINE = 0;
	public static final int STATUS_AWAY = 1;
	public static final int STATUS_E_AWAY = 2;
	public static final int STATUS_DND = 3;
	public static final int STATUS_FREE = 4;
	public static final int STATUS_OFFLINE = 5;

	// Broadcast
	public static final String UPDATE					= "net.ustyugov.jtalk.UPDATE";
	public static final String FINISH					= "net.ustyugov.jtalk.FINISH";
	public static final String RECEIVED 				= "net.ustyugov.jtalk.RECEIVED";
	public static final String PASTE_TEXT 				= "net.ustyugov.jtalk.PASTE_TEXT";
	public static final String CONNECTION_CLOSED 		= "net.ustyugov.jtalk.CONNECTION_CLOSED";
	public static final String CONNECTION_RECONNECT		= "net.ustyugov.jtalk.CONNECTION_RECONNECT";
	public static final String PRESENCE_CHANGED		 	= "net.ustyugov.jtalk.PRESENCE_CHANGED";
	public static final String NEW_MESSAGE 				= "net.ustyugov.jtalk.NEW_MESSAGE";
	public static final String ERROR					= "net.ustyugov.jtalk.ERROR";
	public static final String CHANGE_CHAT				= "net.ustyugov.jtalk.CHANGE_CHAT";
    public static final String XML                      = "net.ustyugov.jtalk.UPDATE";

    public static final int LOAD_MESSAGES_COUNT = 30;
}
