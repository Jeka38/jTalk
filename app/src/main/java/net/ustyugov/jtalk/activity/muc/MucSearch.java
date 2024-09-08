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

package net.ustyugov.jtalk.activity.muc;

import java.util.Collection;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.*;
import net.ustyugov.jtalk.Colors;
import net.ustyugov.jtalk.adapter.muc.MucSearchAdapter;
import net.ustyugov.jtalk.dialog.BookmarksDialogs;
import net.ustyugov.jtalk.dialog.MucDialogs;
import net.ustyugov.jtalk.service.JTalkService;

import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.muc.HostedRoom;
import org.jivesoftware.smackx.muc.MultiUserChat;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;

import com.jtalk2.R;

public class MucSearch extends Activity implements OnClickListener, OnItemClickListener, OnItemLongClickListener {
    private static final String PREF_KEY = "lastMucServer";

	private JTalkService service;
	private String account;
	private EditText searchInput;
	private SharedPreferences prefs;
	private ProgressBar progress;
	private ListView list;
	private MucSearchAdapter adapter;
	private GetRooms task;
	
	@Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        account = getIntent().getStringExtra("account");
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        setTheme(Colors.isLight ? R.style.AppThemeLight : R.style.AppThemeDark);
        setContentView(R.layout.muc_search);
        setTitle(android.R.string.search_go);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        	
        LinearLayout linear = (LinearLayout) findViewById(R.id.muc_search);
        linear.setBackgroundColor(Colors.BACKGROUND);
        
        ImageButton searchButton = (ImageButton) findViewById(R.id.search_button);
        searchButton.setOnClickListener(this);
        
        searchInput = (EditText) findViewById(R.id.search_input);
        progress = (ProgressBar) findViewById(R.id.progress);

        list = (ListView) findViewById(R.id.search_list);
        list.setDividerHeight(0);
        list.setCacheColorHint(0x00000000);
        list.setOnItemClickListener(this);
        list.setOnItemLongClickListener(this);
	}
	
	@Override
	public void onResume() {
		super.onResume();
		service = JTalkService.getInstance();
		String server = prefs.getString(PREF_KEY, getString(R.string.DefaultMucServer));
		searchInput.setText(server);
	}
	
	@Override
	public void onClick(View v) {
        String server = searchInput.getText().toString();
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(service).edit();
        editor.putString(PREF_KEY, server).apply();

		if (task != null && task.getStatus() == AsyncTask.Status.RUNNING) task.cancel(true);
		task = new GetRooms(server);
		task.execute();
	}
	
	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch (item.getItemId()) {
    		case android.R.id.home:
    			finish();
    			break;
    	}
    	return true;
    }
	
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		HostedRoom item = (HostedRoom) parent.getItemAtPosition(position);
		String jid = item.getJid();
		MucDialogs.joinDialog(this, account, jid, null);
	}
	
	@Override
	public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
		final  HostedRoom item = (HostedRoom) parent.getItemAtPosition(position);
        CharSequence[] items = new CharSequence[2];
        items[0] = getString(R.string.Users);
        items[1] = getString(R.string.Add);

        AlertDialog.Builder builder = new AlertDialog.Builder(MucSearch.this);
        builder.setTitle(R.string.Actions);
        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0:
                        MucDialogs.showUsersDialog(MucSearch.this, account, item.getJid());
                        break;
                    case 1:
                        BookmarksDialogs.AddDialog(MucSearch.this, account, item.getJid(), item.getName());
                        break;
                }
            }
        });
        builder.create().show();
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.muc_search, menu);

        SearchView searchView = new SearchView(this);
        searchView.setQueryHint(getString(android.R.string.search_go));
        searchView.setSubmitButtonEnabled(true);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextChange(String newText) {
                return true;
            }
            @Override
            public boolean onQueryTextSubmit(String query) {
                if (adapter != null) adapter.update(query);
                return true;
            }
        });

        MenuItem item = menu.findItem(R.id.search);
        item.setActionView(searchView);
        return super.onCreateOptionsMenu(menu);
    }
	
	private class GetRooms extends AsyncTask<String, Void, Void> {
        String server;

        public GetRooms(String server) {
            this.server = server;
        }
		@Override
		protected Void doInBackground(String... params) {
			try {
                Collection<HostedRoom> rooms = MultiUserChat.getHostedRooms(service.getConnection(account), server);
				if (!rooms.isEmpty()) adapter = new MucSearchAdapter(MucSearch.this, rooms);
			} catch (XMPPException ignored) { }
			return null;
		}
		
		@Override
		protected void onPostExecute(Void v) {
			super.onPostExecute(v);
            list.setAdapter(adapter);
            list.setVisibility(View.VISIBLE);
		    progress.setVisibility(View.GONE);
		}
		
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			list.setVisibility(View.GONE);
			progress.setVisibility(View.VISIBLE);
		}
	}
}
