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

package net.ustyugov.jtalk.activity;

import android.Manifest;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.*;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;

import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.customview.widget.ViewDragHelper;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.widget.*;
import net.ustyugov.jtalk.*;
import net.ustyugov.jtalk.activity.account.Accounts;
import net.ustyugov.jtalk.activity.muc.Bookmarks;
import net.ustyugov.jtalk.activity.note.NotesActivity;
import net.ustyugov.jtalk.adapter.DrawerAdapter;
import net.ustyugov.jtalk.adapter.NoGroupsAdapter;
import net.ustyugov.jtalk.adapter.RosterAdapter;
import net.ustyugov.jtalk.adapter.SearchAdapter;
import net.ustyugov.jtalk.db.AccountDbHelper;
import net.ustyugov.jtalk.db.JTalkProvider;
import net.ustyugov.jtalk.dialog.ErrorDialog;
import net.ustyugov.jtalk.dialog.MucDialogs;
import net.ustyugov.jtalk.dialog.RosterDialogs;
import net.ustyugov.jtalk.listener.*;
import net.ustyugov.jtalk.service.JTalkService;

import net.ustyugov.jtalk.utils.XMPPUri;

import org.jivesoftware.smack.*;

import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;

import com.jtalk2.R;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.util.StringUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.*;

public class RosterActivity extends Activity implements OnItemClickListener, OnItemLongClickListener {
    private static final int WRITE_PERMISSION = 1;
    private static final int ACTIVITY_PREFERENCES = 10;
    private final static int UPDATE_INTERVAL = 500;
    private static long lastUpdateReceived;
    
	private BroadcastReceiver updateReceiver;
    private BroadcastReceiver errorReceiver;

    private Menu menu = null;

    private JTalkService service;
    private SharedPreferences prefs;

    private GridView gridView;
    private SearchAdapter searchAdapter;
    private NoGroupsAdapter simpleAdapter;
    private RosterAdapter rosterAdapter;
    private DrawerAdapter drawerAdapter;
    private String[] statusArray;
    private String searchString = "";
    private boolean permissionGranted = true;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        Thread.setDefaultUncaughtExceptionHandler(new MyExceptionHandler(this));

        if (Build.VERSION.SDK_INT >= 23 && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
            permissionGranted = false;
            requestPermissions(new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE}, WRITE_PERMISSION);
        }

        Colors.updateColors(this);
        startService(new Intent(this, JTalkService.class));
        service = JTalkService.getInstance();
        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        setTheme(Colors.isLight ? R.style.AppThemeLight : R.style.AppThemeDark);
        setContentView(R.layout.roster);

        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setHomeButtonEnabled(true);
            actionBar.setLogo(R.drawable.ic_menu);
        }

        LinearLayout roster = (LinearLayout) findViewById(R.id.roster_linear);
        roster.setBackgroundColor(Colors.BACKGROUND);

        try {
            DrawerLayout mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
            Field mLeftDragger = mDrawerLayout.getClass().getDeclaredField("mLeftDragger");
            mLeftDragger.setAccessible(true);
            ViewDragHelper draggerObj = (ViewDragHelper) mLeftDragger.get(mDrawerLayout);
            Field mEdgeSize = draggerObj.getClass().getDeclaredField("mEdgeSize");
            mEdgeSize.setAccessible(true);
            int edge = mEdgeSize.getInt(draggerObj);
            mEdgeSize.setInt(draggerObj, edge * 5);
        } catch (Exception ignored) { }

        statusArray = getResources().getStringArray(R.array.statusArray);
        rosterAdapter = new RosterAdapter(this);
        simpleAdapter = new NoGroupsAdapter(this);
        searchAdapter = new SearchAdapter(this);

        int cols = 1;
        if (!prefs.getBoolean("ShowGroups", true) && !prefs.getBoolean("ShowMucGroup", false)) {
            try {
                cols = Integer.parseInt(prefs.getString("RosterColumns", "1"));
            } catch (Exception e) {
                cols = 1;
            }
        }
        gridView = (GridView) findViewById(R.id.users);
        gridView.setNumColumns(cols);
        gridView.setCacheColorHint(0x00000000);
        gridView.setOnItemClickListener(this);
        gridView.setOnItemLongClickListener(this);
        gridView.setAdapter(rosterAdapter);

        if (getIntent().getBooleanExtra("status", false)) {
            RosterDialogs.changeStatusDialog(this, null, null);
        }

        if (getIntent().getBooleanExtra("password", false)) {
            String account = getIntent().getStringExtra("account");
            RosterDialogs.passwordDialog(this, account);
        }

        if (getIntent().getBooleanExtra("subscribtion", false)) {
            String account = getIntent().getStringExtra("account");
            String jid = getIntent().getStringExtra("jid");
            RosterDialogs.subscribtionRequestDialog(this, account, jid);
        }

        File table = new File(Constants.PATH_SMILES + "/default/table.xml");
        File icondef = new File(Constants.PATH_SMILES + "/default/icondef.xml");
        if (!table.exists() && !icondef.exists()) {
            new CreateDefaultSmiles().execute();
        } else {
            if (permissionGranted) {
                Cursor cursor = getContentResolver().query(JTalkProvider.ACCOUNT_URI, null, AccountDbHelper.ENABLED + " = '1'", null, null);
                if (cursor == null || cursor.getCount() < 1) startActivity(new Intent(this, Accounts.class));
                else cursor.close();
            }
        }

        if (prefs.getBoolean("BUG", false)) {
            new ErrorDialog(this).show();
        }

        String action = getIntent().getAction();
        if (action != null && action.equals(Intent.ACTION_VIEW)) {
            Uri data = getIntent().getData();
            if (data != null && data.getScheme().equals("xmpp")) {
                XMPPUri xmppUri;
                try {
                    xmppUri = new XMPPUri(data);
                } catch (IllegalArgumentException e) {
                    xmppUri = null;
                }

                List<String> accounts = new ArrayList<>();
                for(XMPPConnection connection : service.getAllConnections()) {
                    accounts.add(StringUtils.parseBareAddress(connection.getUser()));
                }

                if (xmppUri != null && !accounts.isEmpty()) {
                    final String xmppJid = xmppUri.getJid();
                    final String body = xmppUri.getBody();
                    String queryType = xmppUri.getQueryType();

                    final ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, accounts);

                    switch(queryType) {
                        case "roster":
                            RosterDialogs.addDialog(this, xmppUri.getJid());
                            break;
                        case "join":
                            if (accounts.size() > 1) {
                                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                                builder.setTitle(R.string.Accounts);
                                builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        String account = adapter.getItem(which);
                                        MucDialogs.joinDialog(RosterActivity.this, account, xmppJid, null);
                                    }
                                });
                                builder.create().show();
                            } else MucDialogs.joinDialog(RosterActivity.this, accounts.get(0), xmppJid, null);
                            break;
                        default:
                            service.setText(xmppJid, body);
                            if (accounts.size() > 1) {
                                service.setText(xmppJid, body);
                                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                                builder.setTitle(R.string.Accounts);
                                builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        String account = adapter.getItem(which);
                                        Intent intent = new Intent(RosterActivity.this, Chat.class);
                                        intent.putExtra("account", account);
                                        intent.putExtra("jid", xmppJid);
                                        startActivity(intent);
                                    }
                                });
                                builder.create().show();
                            } else {
                                Intent intent = new Intent(RosterActivity.this, Chat.class);
                                intent.putExtra("account", accounts.get(0));
                                intent.putExtra("jid", xmppJid);
                                startActivity(intent);
                            }
                            break;
                    }
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case WRITE_PERMISSION: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    finish();
                    startActivity(getIntent());
                } else {
                    Notify.cancelAll(this);
                    stopService(new Intent(this, JTalkService.class));
                    finish();
                    System.exit(0);
                }
            }
        }
    }
    
    @Override
    public void onResume() {
        super.onResume();

        errorReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                service = JTalkService.getInstance();
                String error = intent.getStringExtra("error");
                Toast.makeText(context, error, Toast.LENGTH_LONG).show();
            }
        };

        updateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                service = JTalkService.getInstance();
                updateMenu();
                updateStatus();

                long now = System.currentTimeMillis();
                if ((now - lastUpdateReceived) < RosterActivity.UPDATE_INTERVAL) return;
                lastUpdateReceived = now;
                updateList();
            }
        };

        service = JTalkService.getInstance();
        service.setCurrentJid("me");

        registerReceiver(errorReceiver, new IntentFilter(Constants.ERROR));
        registerReceiver(updateReceiver, new IntentFilter(Constants.UPDATE));
        registerReceiver(updateReceiver, new IntentFilter(Constants.NEW_MESSAGE));

        if (service != null) service.resetTimer();

        drawerAdapter = new DrawerAdapter(this);
        prepareDrawer();

        updateList();
        updateMenu();
        updateStatus();
    }

    @Override
    public boolean onKeyUp(int key, KeyEvent event) {
        if (key == KeyEvent.KEYCODE_SEARCH) {
            MenuItem item = menu.findItem(R.id.search);
            item.expandActionView();
        }
        return super.onKeyUp(key, event);
    }

    @Override
    public void onPause() {
    	super.onPause();
        DrawerLayout drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawerLayout.closeDrawers();
    	unregisterReceiver(errorReceiver);
	    unregisterReceiver(updateReceiver);
    }

    @Override
    public void onDestroy() {
    	super.onDestroy();
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ACTIVITY_PREFERENCES) {
            if (resultCode == RESULT_OK) {
                Intent intent = getIntent();
                intent.putExtra("password", false);
                finish();
                startActivity(intent);

                service.removeSmiles();
            }
        }
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        this.menu = menu;
        updateMenu();
        return true;
    }
    
    private void updateMenu() {
    	if (menu != null) {
            if (gridView.getAdapter() instanceof SearchAdapter) return;
            menu.clear();
            getMenuInflater().inflate(R.menu.roster, menu);
    		menu.findItem(R.id.add).setEnabled(service.isAuthenticated());
            menu.findItem(R.id.offline).setTitle(prefs.getBoolean("hideOffline", false) ? R.string.ShowOfflineContacts : R.string.HideOfflineContacts);

            MenuItem sound = menu.findItem(R.id.notify);
            sound.setShowAsActionFlags(prefs.getBoolean("showSound", false) ? MenuItem.SHOW_AS_ACTION_ALWAYS : MenuItem.SHOW_AS_ACTION_NEVER);
            if (prefs.getBoolean("soundDisabled", false)) {
                sound.setTitle(R.string.EnableSound);
                sound.setIcon(R.drawable.ic_menu_sound_off);

            } else {
                sound.setTitle(R.string.DisableSound);
                sound.setIcon(R.drawable.ic_menu_sound_on);
            }

            MenuItem counter = menu.findItem(R.id.count);
            if (counter != null) {
                int count = service.getMessagesCount();
                counter.setTitle(count+"");
                if (count > 0) {
                    counter.setVisible(true);
                    if (service.isHighlight()) counter.setIcon(R.drawable.ic_mail_outline_highlight);
                } else {
                    counter.setVisible(false);
                }
            }

            menu.findItem(R.id.xml).setVisible(prefs.getBoolean("XMLConsole", false));

            MenuItem.OnActionExpandListener listener = new MenuItem.OnActionExpandListener() {
                @Override
                public boolean onMenuItemActionCollapse(MenuItem item) {
                    gridView.setAdapter(null);
                    searchString = null;
                    updateList();
                    updateMenu();
                    return true;
                }

                @Override
                public boolean onMenuItemActionExpand(MenuItem item) {
                    gridView.setAdapter(searchAdapter);
                    searchString = "";
                    updateList();
                    return true;
                }
            };

            SearchView searchView = new SearchView(this);
            searchView.setQueryHint(getString(android.R.string.search_go));
            searchView.setSubmitButtonEnabled(false);
            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextChange(String newText) {
                    searchString = newText;
                    updateList();
                    return true;
                }
                @Override
                public boolean onQueryTextSubmit(String query) {
                    return true;
                }
            });

            MenuItem item = menu.findItem(R.id.search);
            item.setActionView(searchView);
            item.setShowAsAction(MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);
            item.setOnActionExpandListener(listener);
            super.onCreateOptionsMenu(menu);
    	}
    }
  
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch (item.getItemId()) {
            case R.id.count:
                Intent nextChat = service.getNextChat();
                if (nextChat != null) this.startActivity(nextChat);
                break;
    		case R.id.offline:
    			if (prefs.getBoolean("hideOffline", false)) service.setPreference("hideOffline", false);
    			else service.setPreference("hideOffline", true);
    			updateMenu();
    			updateList();
    			break;
    		case android.R.id.home:
                DrawerLayout drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
                if (!drawerLayout.isDrawerOpen(GravityCompat.START)) drawerLayout.openDrawer(GravityCompat.START);
                else drawerLayout.closeDrawer(GravityCompat.START);
    			break;
  	    	case R.id.add:
  	    		RosterDialogs.addDialog(this, null);
  	    		break;
            case R.id.status:
                RosterDialogs.changeStatusDialog(RosterActivity.this, null, null);
                break;
            case R.id.search:
                item.expandActionView();
                break;
  	    	case R.id.bookmarks:
  	    		Intent bIntent = new Intent(this, Bookmarks.class);
  	    		startActivity(bIntent);
  	    		break;
  	    	case R.id.accounts:
  	    		Intent aIntent = new Intent(this, Accounts.class);
  	    		startActivity(aIntent);
  	    		break;
            case R.id.notify:
                if (prefs.getBoolean("soundDisabled", false)) {
                    service.setPreference("soundDisabled", false);

                    if (prefs.getBoolean("AutoStatusOnSoundOff", false)) {
                        Presence presence = service.getOldPresence();
                        if (presence != null) {
                            service.setPreference("currentMode", presence.getMode().name());
                            switch (presence.getMode()) {
                                case available:
                                    service.setPreference("currentSelection", Constants.STATUS_ONLINE);
                                    break;
                                case away:
                                    service.setPreference("currentSelection", Constants.STATUS_AWAY);
                                    break;
                                case xa:
                                    service.setPreference("currentSelection", Constants.STATUS_E_AWAY);
                                    break;
                                case dnd:
                                    service.setPreference("currentSelection", Constants.STATUS_DND);
                                    break;
                                case chat:
                                    service.setPreference("currentSelection", Constants.STATUS_FREE);
                                    break;
                            }
                            service.setPreference("currentStatus", presence.getStatus());
                            service.sendPresence(presence.getStatus(), presence.getMode().name(), presence.getPriority());
                        }
                    }
                } else {
                    service.setPreference("soundDisabled", true);

                    if (prefs.getBoolean("AutoStatusOnSoundOff", false)) {
                        String currentMode = prefs.getString("currentMode", "available");
                        String currentStatus = prefs.getString("currentStatus", "");
                        int currentPriority = prefs.getInt("currentPriority", 0);

                        Presence presence = new Presence(Presence.Type.available);
                        presence.setStatus(currentStatus);
                        presence.setMode(Presence.Mode.valueOf(currentMode));
                        presence.setPriority(currentPriority);

                        service.setOldPresence(presence);
                        service.setPreference("currentMode", "dnd");
                        service.setPreference("currentSelection", 3);
                        service.setPreference("currentStatus", "");
                        service.sendPresence("", Presence.Mode.dnd.name(), 0);
                    }
                }
                updateMenu();
                break;
            case R.id.xml:
                Intent xIntent = new Intent(this, XMLConsole.class);
                startActivity(xIntent);
                break;
  	    	case R.id.exit:
  	    		if (prefs.getBoolean("DeleteHistory", false)) {
  	    			getContentResolver().delete(JTalkProvider.CONTENT_URI, null, null);
  	    		}

                if (prefs.getBoolean("soundDisabled", false)) {
                    service.setPreference("soundDisabled", false);
                }

                if (!prefs.getBoolean("RememberPresence", true)) {
                    service.setPreference("currentPriority", 0);
                    service.setPreference("currentSelection", Constants.STATUS_ONLINE);
                    service.setPreference("currentMode", "available");
                    service.setPreference("currentStatus", "");
                }

  	    		Notify.cancelAll(this);
                stopService(new Intent(this, JTalkService.class));
                finish();
                System.exit(0);
  	    		break;
  	    	default:
  	    		return false;
    	}
    	return true;
    }
  
    private void updateList() {

        drawerAdapter.update();

        if (gridView.getAdapter() != null && gridView.getAdapter() instanceof  SearchAdapter) {
            searchAdapter.update(searchString);
            searchAdapter.notifyDataSetChanged();
        } else {
            if (prefs.getBoolean("ShowGroups", true)) {
                if (gridView.getAdapter() instanceof NoGroupsAdapter || gridView.getAdapter() == null) gridView.setAdapter(rosterAdapter);
                rosterAdapter.update();
                rosterAdapter.notifyDataSetChanged();
            } else {
                if (gridView.getAdapter() instanceof RosterAdapter || gridView.getAdapter() == null) gridView.setAdapter(simpleAdapter);
                simpleAdapter.update();
                simpleAdapter.notifyDataSetChanged();
            }
        }
    }
    
    private void updateStatus() {
        drawerAdapter.update();
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            if (Build.VERSION.SDK_INT >= 21) {
                actionBar.setDisplayHomeAsUpEnabled(true);
                actionBar.setHomeAsUpIndicator(R.drawable.ic_menu);
            }
            if (service.isAuthenticated()) {
                String status = statusArray[prefs.getInt("currentSelection", 0)];
                actionBar.setTitle(status);
                actionBar.setSubtitle(service.getGlobalState());
                String mode = prefs.getString("currentMode", "available");

                if (Build.VERSION.SDK_INT >= 21) {
                    Window window = getWindow();
                    switch(mode) {
                        case "chat":
                            window.setStatusBarColor(Colors.STATUSBAR_CHAT);
                            actionBar.setBackgroundDrawable(new ColorDrawable(Colors.ACTIONBAR_CHAT));
                            break;
                        case "away":
                            window.setStatusBarColor(Colors.STATUSBAR_AWAY);
                            actionBar.setBackgroundDrawable(new ColorDrawable(Colors.ACTIONBAR_AWAY));
                            break;
                        case "xa":
                            window.setStatusBarColor(Colors.STATUSBAR_XA);
                            actionBar.setBackgroundDrawable(new ColorDrawable(Colors.ACTIONBAR_XA));
                            break;
                        case "dnd":
                            window.setStatusBarColor(Colors.STATUSBAR_DND);
                            actionBar.setBackgroundDrawable(new ColorDrawable(Colors.ACTIONBAR_DND));
                            break;
                        default:
                            window.setStatusBarColor(Colors.STATUSBAR_ONLINE);
                            actionBar.setBackgroundDrawable(new ColorDrawable(Colors.ACTIONBAR_ONLINE));
                            break;
                    }
                }
            } else {
                actionBar.setTitle(getString(R.string.NotConnected));
                actionBar.setSubtitle(service.getGlobalState());

                if (Build.VERSION.SDK_INT >= 21) {
                    Window window = getWindow();
                    window.setStatusBarColor(Colors.STATUSBAR_OFFLINE);
                    actionBar.setBackgroundDrawable(new ColorDrawable(Colors.ACTIONBAR_OFFLINE));
                }
            }
        }
    }
    
	@Override
	public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
		RosterItem item = (RosterItem) parent.getItemAtPosition(position);
		String name = item.getName();
		String account = item.getAccount();
		
		if (item.isGroup() || item.isAccount()) {
			if (item.isCollapsed()) {
				while (service.getCollapsedGroups().contains(name)) service.getCollapsedGroups().remove(name);
				item.setCollapsed(false);
			} else {
				service.getCollapsedGroups().add(name);
				item.setCollapsed(true);
			}
			updateList();
		} else if (item.isEntry() || item.isSelf()) {
			RosterEntry re = item.getEntry();
			String jid = re.getUser();
			Intent i = new Intent(this, Chat.class);
			i.putExtra("account", account);
	        i.putExtra("jid", jid);
	        startActivity(i);
		} else if (item.isMuc()) {
			Intent i = new Intent(this, Chat.class);
			i.putExtra("account", account);
	        i.putExtra("jid", item.getName());
	        startActivity(i);
		}
	}
	
	@Override
	public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
		RosterItem item = (RosterItem) parent.getItemAtPosition(position);
		if (item.isGroup()) {
			String name = item.getName();
			if (!name.equals(getString(R.string.Nogroup)) && !name.equals(getString(R.string.SelfGroup)) && !name.equals(getString(R.string.MUC)) && !name.equals(getString(R.string.Privates)) && !name.equals(getString(R.string.ActiveChats))) RosterDialogs.renameGroupDialog(this, item.getAccount(), item.getName());
		} else if (item.isAccount()) {
			RosterDialogs.AccountMenuDialog(this, item);
		} else if (item.isEntry()) {
            String j = item.getEntry().getUser();
			if (!service.getPrivateMessages(item.getAccount()).contains(j)) RosterDialogs.ContactMenuDialog(this, item);
            else RosterDialogs.PrivateMenuDialog(this, item);
		} else if (item.isSelf()) {
			RosterDialogs.SelfContactMenuDialog(this, item);
		} else if (item.isMuc()) {
			MucDialogs.roomMenu(this, item.getAccount(), item.getName());
		}
		return true;
	}

    private class CreateDefaultSmiles extends AsyncTask<Integer, Integer, Integer> {
        AlertDialog dialog;

        @Override
        protected void onPreExecute() {
            AlertDialog.Builder builder = new AlertDialog.Builder(RosterActivity.this);
            builder.setMessage("Please wait...");
            builder.setCancelable(false);
            dialog = builder.create();
            dialog.show();
        }

        @Override
        protected void onPostExecute(Integer result) {
            dialog.cancel();
            if (permissionGranted) {
                Cursor cursor = getContentResolver().query(JTalkProvider.ACCOUNT_URI, null, AccountDbHelper.ENABLED + " = '1'", null, null);
                if (cursor == null || cursor.getCount() < 1) startActivity(new Intent(RosterActivity.this, Accounts.class));
                else cursor.close();
            }
        }

        @Override
        protected Integer doInBackground(Integer... integers) {
            File folder = new File(Constants.PATH_SMILES + "/default");
            if (folder.exists() || folder.mkdirs()) {
                try {
                    AssetManager assetManager = getAssets();
                    for (String file : assetManager.list("emotion")) {
                        InputStream in = assetManager.open("emotion/" + file);
                        byte[] buffer = new byte[in.available()];
                        in.read(buffer);
                        in.close();

                        FileOutputStream out = new FileOutputStream(Constants.PATH_SMILES + "/default/" + file);
                        out.write(buffer);
                        out.close();
                    }
                } catch (Exception ignored) { }
            }
            return 0;
        }
    }

    private void prepareDrawer() {
        DrawerClickListener drawerClickListener = new DrawerClickListener(this);
        ListView drawerList = (ListView) findViewById(R.id.left_drawer);
        drawerList.setAdapter(drawerAdapter);
        drawerList.setOnItemClickListener(drawerClickListener);
        drawerList.setOnItemLongClickListener(drawerClickListener);

        ImageView icon = (ImageView) findViewById(R.id.status_button);
        icon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RosterDialogs.changeStatusDialog(RosterActivity.this, null, null);
            }
        });
        icon = (ImageView) findViewById(R.id.settings_button);
        icon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(RosterActivity.this, Preferences.class));
            }
        });
        icon = (ImageView) findViewById(R.id.accounts_button);
        icon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(RosterActivity.this, Accounts.class));
            }
        });
        icon = (ImageView) findViewById(R.id.notes_button);
        icon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (service.isAuthenticated()) startActivity(new Intent(RosterActivity.this, NotesActivity.class));
                else Toast.makeText(RosterActivity.this, R.string.Error, Toast.LENGTH_LONG).show();
            }
        });
        icon = (ImageView) findViewById(R.id.disco_button);
        icon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (service.isAuthenticated()) startActivity(new Intent(RosterActivity.this, ServiceDiscovery.class));
                else Toast.makeText(RosterActivity.this, R.string.Error, Toast.LENGTH_LONG).show();
            }
        });
    }

}
