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

package net.ustyugov.jtalk.receivers;

import android.database.Cursor;
import android.net.NetworkInfo;
import net.ustyugov.jtalk.Constants;
import net.ustyugov.jtalk.db.AccountDbHelper;
import net.ustyugov.jtalk.db.JTalkProvider;
import net.ustyugov.jtalk.listener.ConListener;
import net.ustyugov.jtalk.service.JTalkService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import org.jivesoftware.smack.XMPPConnection;

public class ChangeConnectionReceiver extends BroadcastReceiver {
    private boolean firstStart = true;

	@Override
	public void onReceive(Context context, Intent intent) {
        if (firstStart) {
            firstStart = false;
            return;
        }
		JTalkService service = JTalkService.getInstance();
        ConnectivityManager cm = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();

        Cursor cursor = service.getContentResolver().query(JTalkProvider.ACCOUNT_URI, null, AccountDbHelper.ENABLED + " = '1'", null, null);
        if (cursor != null && cursor.getCount() > 0) {
            cursor.moveToFirst();
            do {
                String account = cursor.getString(cursor.getColumnIndex(AccountDbHelper.JID)).trim();

                if (activeNetwork != null && activeNetwork.isConnected()) {
                    XMPPConnection connection = service.getConnection(account);
                    if (connection != null) {
//                        connection.disconnect();
                        service.reconnect(account);
                    }
                } else {
//                    XMPPConnection connection = service.getConnection(account);
//                    connection.disconnect();

                    ConListener listener = service.getConnectionListener(account);
                    if (listener != null) listener.connectionClosedOnError(new Exception("Network down!"));
                }

            } while(cursor.moveToNext());
            cursor.close();
        }

        context.sendBroadcast(new Intent(Constants.UPDATE));
	}
}
