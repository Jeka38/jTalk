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

package net.ustyugov.jtalk.db;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

public class JTalkProvider  extends ContentProvider {
	public static final Uri ACCOUNT_URI = Uri.parse("content://com.jtalk2/account");
	public static final Uri CONTENT_URI = Uri.parse("content://com.jtalk2/message");
	public static final Uri TEMPLATES_URI = Uri.parse("content://com.jtalk2/template");
	public static final Uri ATTACHMENTS_URI = Uri.parse("content://com.jtalk2/attachments");
	
	private SQLiteDatabase msg_db;
	private SQLiteDatabase acc_db;
	private SQLiteDatabase tmp_db;
	private SQLiteDatabase att_db;
	
	@Override
	public boolean onCreate() {
        String path = "msg.db";

		msg_db = new MessageDbHelper(getContext(), path).getWritableDatabase();
		acc_db = new AccountDbHelper(getContext()).getWritableDatabase();
		tmp_db = new TemplatesDbHelper(getContext()).getWritableDatabase();
		att_db = new AttachmentsDbHelper(getContext()).getWritableDatabase();

		return (msg_db != null);
	}
	
	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		Cursor c = null;
		if (uri.equals(CONTENT_URI)) c = msg_db.query(MessageDbHelper.TABLE_NAME, projection, selection, selectionArgs, null, null, sortOrder);
		else if (uri.equals(ACCOUNT_URI)) c = acc_db.query(AccountDbHelper.TABLE_NAME, projection, selection, selectionArgs, null, null, sortOrder);
		else if (uri.equals(TEMPLATES_URI)) c = tmp_db.query(TemplatesDbHelper.TABLE_NAME, projection, selection, selectionArgs, null, null, sortOrder);
		else if (uri.equals(ATTACHMENTS_URI)) c = att_db.query(AttachmentsDbHelper.TABLE_NAME, projection, selection, selectionArgs, null, null, sortOrder);

		if (c != null && getContext() != null) c.setNotificationUri(getContext().getContentResolver(), uri);

		return c;
	}
	
	@Override
	public Uri insert(Uri url, ContentValues values) {
		long rowId = 0;

		if (url.equals(CONTENT_URI)) {
			rowId = msg_db.insert(MessageDbHelper.TABLE_NAME, MessageDbHelper.ID, values);
		} else if (url.equals(ACCOUNT_URI)) {
			rowId = acc_db.insert(AccountDbHelper.TABLE_NAME, AccountDbHelper.JID, values);
		} else if (url.equals(TEMPLATES_URI)) {
			rowId = tmp_db.insert(TemplatesDbHelper.TABLE_NAME, TemplatesDbHelper.TEXT, values);
		} else if (url.equals(ATTACHMENTS_URI)) {
			rowId = att_db.insert(AttachmentsDbHelper.TABLE_NAME, AttachmentsDbHelper.URL, values);
		}

		if (rowId > 0) {
			Uri uri = ContentUris.withAppendedId(url, rowId);
			if (getContext() != null) getContext().getContentResolver().notifyChange(uri, null);
			return uri;
		} else {
			return null;
		}
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		int retVal = 0;
		if (uri.equals(CONTENT_URI)) retVal = msg_db.update(MessageDbHelper.TABLE_NAME, values, selection, selectionArgs);
		else if (uri.equals(ACCOUNT_URI)) retVal = acc_db.update(AccountDbHelper.TABLE_NAME, values, selection, selectionArgs);
		else if (uri.equals(TEMPLATES_URI)) retVal = tmp_db.update(TemplatesDbHelper.TABLE_NAME, values, selection, selectionArgs);
		else if (uri.equals(ATTACHMENTS_URI)) retVal = att_db.update(AttachmentsDbHelper.TABLE_NAME, values, selection, selectionArgs);

		if (getContext() != null) getContext().getContentResolver().notifyChange(uri, null);

		return retVal;
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		int retVal = 0;
		if (uri.equals(CONTENT_URI)) retVal = msg_db.delete(MessageDbHelper.TABLE_NAME, selection, selectionArgs);
		else if (uri.equals(ACCOUNT_URI)) retVal = acc_db.delete(AccountDbHelper.TABLE_NAME, selection, selectionArgs);
		else if (uri.equals(TEMPLATES_URI)) retVal = tmp_db.delete(TemplatesDbHelper.TABLE_NAME, selection, selectionArgs);
		else if (uri.equals(ATTACHMENTS_URI)) retVal = att_db.delete(AttachmentsDbHelper.TABLE_NAME, selection, selectionArgs);

		if (getContext() != null) getContext().getContentResolver().notifyChange(uri, null);

		return retVal;
	}

	@Override
	public String getType(Uri uri) {
		return null;
	}
}
