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

package net.ustyugov.jtalk.activity;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import android.app.ActionBar;
import android.app.Activity;
import android.content.*;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.customview.widget.ViewDragHelper;
import android.text.InputType;
import android.util.DisplayMetrics;
import android.view.*;
import android.view.inputmethod.EditorInfo;
import android.widget.*;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import net.ustyugov.jtalk.*;
import net.ustyugov.jtalk.activity.account.Accounts;
import net.ustyugov.jtalk.activity.muc.SubjectActivity;
import net.ustyugov.jtalk.activity.note.NotesActivity;
import net.ustyugov.jtalk.activity.note.TemplatesActivity;
import net.ustyugov.jtalk.activity.vcard.VCardActivity;
import net.ustyugov.jtalk.adapter.*;
import net.ustyugov.jtalk.adapter.muc.MucChatAdapter;
import net.ustyugov.jtalk.adapter.muc.MucUserAdapter;
import net.ustyugov.jtalk.db.JTalkProvider;
import net.ustyugov.jtalk.db.MessageDbHelper;
import net.ustyugov.jtalk.dialog.*;
import net.ustyugov.jtalk.listener.DrawerClickListener;
import net.ustyugov.jtalk.service.JTalkService;
import net.ustyugov.jtalk.smiles.Smiles;
import net.ustyugov.jtalk.utils.FileUtils;
import net.ustyugov.jtalk.view.FloatingActionButton;
import net.ustyugov.jtalk.view.MyListView;

import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.ChatState;
import org.jivesoftware.smackx.muc.MultiUserChat;

import android.app.AlertDialog;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View.OnLongClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;

import com.jtalk2.R;

public class Chat extends Activity implements View.OnClickListener, OnScrollListener, SwipeRefreshLayout.OnRefreshListener {
    private static final int REQUEST_TEMPLATES = 1;
    private static final int REQUEST_FILE = 2;

    private boolean isMuc = false;
    private boolean isPrivate = false;
    private MultiUserChat muc;

    private SharedPreferences prefs;
    private Menu menu;

    private LinearLayout sidebar;
    private LinearLayout attachPanel;
    private ChatAdapter  listAdapter;
    private MucChatAdapter listMucAdapter;
    private DrawerAdapter drawerAdapter;
    private MucUserAdapter usersAdapter;
    private MyListView listView;
    private ListView nickList;
    private ListView rightNickList;
    private EditText messageInput;
    private TextView attachPath;
    private ImageButton sendButton;
    private ImageView removeAttach;
    private SwipeRefreshLayout swipeRefreshLayout;

    private String jid;
    private String account;
    private String resource;
    private String searchString = "";
    private boolean compose = false;
    private boolean move = false;
    private int unreadMessages = 0;
    private int separatorPosition = 0;
    private boolean first = true;

    private BroadcastReceiver changeChatReceiver;
    private BroadcastReceiver textReceiver;
    private BroadcastReceiver finishReceiver;
    private BroadcastReceiver msgReceiver;
    private BroadcastReceiver receivedReceiver;
    private BroadcastReceiver presenceReceiver;
    private BroadcastReceiver composeReceiver;

    private JTalkService service;
    private Smiles smiles;

    private ChatAdapter.ViewMode viewMode = ChatAdapter.ViewMode.single;
    private View.OnTouchListener onTouchListener;

    private FloatingActionButton fab;

    private boolean sendFileDirect = false;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        Thread.setDefaultUncaughtExceptionHandler(new MyExceptionHandler(this));
        service = JTalkService.getInstance();
        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        setTheme(Colors.isLight ? R.style.AppThemeLight : R.style.AppThemeDark);

        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowTitleEnabled(true);
        }
        setContentView(R.layout.chat);

        prepareDrawer();

        swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.refresh);
        swipeRefreshLayout.setOnRefreshListener(this);

        LinearLayout linear = (LinearLayout) findViewById(R.id.chat_linear);
        linear.setBackgroundColor(Colors.BACKGROUND);

        smiles = service.getSmiles(this);

        fab = new FloatingActionButton.Builder(this)
                .withDrawable(getResources().getDrawable(R.drawable.ic_action_end))
                .withMargins(0, 0, 0, 72)
                .create();
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listView.setSelection(listView.getCount());
            }
        });

        DrawerLayout mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerLayout.setDrawerListener(new DrawerLayout.DrawerListener() {
            private boolean opened = false;

            @Override
            public void onDrawerOpened(View view) {
                opened = !fab.isHidden();
                fab.hideFloatingActionButton();
            }

            @Override
            public void onDrawerClosed(View view) {
                if (opened) fab.showFloatingActionButton();
            }

            @Override
            public void onDrawerSlide(View view, float v) { }

            @Override
            public void onDrawerStateChanged(int i) { }
        });

        try {
            Field mLeftDragger = mDrawerLayout.getClass().getDeclaredField("mLeftDragger");
            mLeftDragger.setAccessible(true);
            ViewDragHelper draggerObj = (ViewDragHelper) mLeftDragger.get(mDrawerLayout);
            Field mEdgeSize = draggerObj.getClass().getDeclaredField("mEdgeSize");
            mEdgeSize.setAccessible(true);
            int edge = mEdgeSize.getInt(draggerObj);
            mEdgeSize.setInt(draggerObj, edge * 5);

            Field mRightDragger = mDrawerLayout.getClass().getDeclaredField("mRightDragger");
            mRightDragger.setAccessible(true);
            draggerObj = (ViewDragHelper) mRightDragger.get(mDrawerLayout);
            mEdgeSize = draggerObj.getClass().getDeclaredField("mEdgeSize");
            mEdgeSize.setAccessible(true);
            edge = mEdgeSize.getInt(draggerObj);
            mEdgeSize.setInt(draggerObj, edge * 5);
        } catch (Exception ignored) { }

        listAdapter = new ChatAdapter(this, smiles);
        listMucAdapter = new MucChatAdapter(this, smiles);
        listView = (MyListView) findViewById(R.id.chat_list);
        listView.setFocusable(true);
        listView.setCacheColorHint(0x00000000);
        listView.setOnScrollListener(this);
        listView.setDividerHeight(0);
        listView.setAdapter(listAdapter);
        listView.setOnItemLongClickListener(new OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View v, int position, long id) {
                MessageItem item = (MessageItem) parent.getItemAtPosition(position);
                if (item.getType() != MessageItem.Type.separator) MessageDialogs.messageMenu(Chat.this, item, isMuc);
                return true;
            }
        });

        nickList = (ListView) findViewById(R.id.muc_user_list);
        rightNickList = (ListView) findViewById(R.id.right_drawer);
        if (prefs.getBoolean("OldUserList", false)) {
            nickList.setCacheColorHint(0x00000000);
            nickList.setOnItemClickListener(new OnItemClickListener() {
                public void onItemClick(AdapterView<?> parent, View view, int position, long arg3) {
                    if (move) return;
                    RosterItem item = (RosterItem) parent.getItemAtPosition(position);
                    if (item.isEntry()) {
                        String separator = prefs.getString("nickSeparator", ", ");

                        int pos = messageInput.getSelectionEnd();
                        String oldText = messageInput.getText().toString();
                        String newText;
                        String nick = item.getName();
                        String text = "";
                        if (oldText.length() > 0) {
                            text = nick + " ";
                        } else {
                            text = nick + separator;
                        }
                        if (pos > 0 && oldText.length() > 1 && !oldText.substring(pos-1).equals(" ")) newText = oldText.substring(0, pos) + " " + text + oldText.substring(pos);
                        else newText = oldText.substring(0, pos) + text + oldText.substring(pos);

                        messageInput.setText(newText);
                        messageInput.setSelection(messageInput.getText().length());
                    }
                }
            });
            nickList.setOnItemLongClickListener(new OnItemLongClickListener() {
                @Override
                public boolean onItemLongClick(AdapterView<?> parent, View v, int position, long id) {
                    if (move) return false;
                    RosterItem item = (RosterItem) parent.getItemAtPosition(position);
                    if (item.isEntry()) {
                        String nick = item.getName();
                        MucDialogs.userMenu(Chat.this, account, jid, nick);
                        return true;
                    } else return false;
                }
            });
        } else {
            rightNickList.setOnItemClickListener(new OnItemClickListener() {
                public void onItemClick(AdapterView<?> parent, View view, int position, long arg3) {
                    RosterItem item = (RosterItem) parent.getItemAtPosition(position);
                    if (item.isEntry()) {
                        String separator = prefs.getString("nickSeparator", ", ");

                        int pos = messageInput.getSelectionEnd();
                        String oldText = messageInput.getText().toString();
                        String newText;
                        String nick = item.getName();
                        String text = "";
                        if (oldText.length() > 0) {
                            text = nick + " ";
                        } else {
                            text = nick + separator;
                        }

                        if (pos > 0 && oldText.length() > 1 && !oldText.substring(pos-1).equals(" ")) newText = oldText.substring(0, pos) + " " + text + oldText.substring(pos);
                        else newText = oldText.substring(0, pos) + text + oldText.substring(pos);

                        messageInput.setText(newText);
                        messageInput.setSelection(messageInput.getText().length());

                        DrawerLayout drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
                        drawerLayout.closeDrawers();
                    }
                }
            });
            rightNickList.setOnItemLongClickListener(new OnItemLongClickListener() {
                @Override
                public boolean onItemLongClick(AdapterView<?> parent, View v, int position, long id) {
                    RosterItem item = (RosterItem) parent.getItemAtPosition(position);
                    if (item.isEntry()) {
                        String nick = item.getName();
                        MucDialogs.userMenu(Chat.this, account, jid, nick);
                        return true;
                    } else return false;
                }
            });
        }

        sendButton = (ImageButton)findViewById(R.id.SendButton);
        // sendButton.setEnabled(false);
        sendButton.setOnClickListener(this);
        sendButton.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {

                CharSequence[] items = new CharSequence[4];

                items[0] = "/me ";
                items[1] = getString(R.string.PasteSmile);
                items[2] = getString(R.string.SendFile);
                items[3] = getString(R.string.Templates);

                AlertDialog.Builder builder = new AlertDialog.Builder(Chat.this);
                builder.setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0:
                                String text = messageInput.getText().toString();
                                messageInput.setText("/me " + text);
                                messageInput.setSelection(messageInput.getText().length());
                                break;
                            case 1:
                                smiles.showDialog();
                                break;
                            case 2:
                                Intent fileIntent = new Intent(Intent.ACTION_GET_CONTENT);
                                fileIntent.setType("*/*");
                                fileIntent.addCategory(Intent.CATEGORY_OPENABLE);
                                startActivityForResult(Intent.createChooser(fileIntent, getString(R.string.SelectFile)), REQUEST_FILE);
                                break;
                            case 3:
                                startActivityForResult(new Intent(Chat.this, TemplatesActivity.class), REQUEST_TEMPLATES);
                                break;
                        }
                    }
                });
                builder.create().show();
                return true;
            }
        });

        messageInput = (EditText)findViewById(R.id.messageInput);
        messageInput.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEND) {
                    onClick(sendButton);
                    return true;
                }
                return false;
            }
        });

        ImageView smileImage = (ImageView) findViewById(R.id.smileImage);
        smileImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                smiles.showDialog();
            }
        });
        if (prefs.getBoolean("ShowSmiles", true)) {
            if (!prefs.getBoolean("ShowSmilesButton", true)) {
                smileImage.setVisibility(View.GONE);
            } else {
                smileImage.setVisibility(View.VISIBLE);
            }
        } else {
            smileImage.setVisibility(View.GONE);
        }

        if (prefs.getBoolean("SendOnEnter", false)) {
            messageInput.setImeOptions(EditorInfo.IME_ACTION_SEND);
            messageInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
            sendButton.setVisibility(View.GONE);
        } else {
            messageInput.setImeOptions(EditorInfo.IME_ACTION_NONE);
            messageInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
            sendButton.setVisibility(View.VISIBLE);
        }

        if (prefs.getBoolean("SmileButtonOnKeyboard", false)) {
            messageInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES | InputType.TYPE_TEXT_VARIATION_SHORT_MESSAGE | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
            sendButton.setVisibility(View.VISIBLE);
        }

        attachPanel = (LinearLayout) findViewById(R.id.attachPanel);
        attachPath = (TextView) findViewById(R.id.attachPath);
        removeAttach = (ImageView) findViewById(R.id.attachRemove);
        removeAttach.setOnClickListener(this);

        sidebar = (LinearLayout) findViewById(R.id.sidebar);
        if (prefs.getBoolean("OldUserList", false)) {
            onTouchListener = new View.OnTouchListener() {
                int firstY = 0;
                int firstX = 0;
                boolean lock = false;

                @Override
                public boolean onTouch(View view, MotionEvent event) {
                    DisplayMetrics metrics = new DisplayMetrics();
                    getWindowManager().getDefaultDisplay().getMetrics(metrics);
                    int displayWidth = metrics.widthPixels;

                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            firstX = (int) event.getRawX();
                            firstY = (int) event.getRawY();
                            break;
                        case MotionEvent.ACTION_UP:
                            lock = false;
                            updateUsers();
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        Thread.sleep(200);
                                        move = false;
                                    } catch (InterruptedException ignored) { }
                                }
                            }).start();
                            break;
                        case MotionEvent.ACTION_MOVE:
                            // Detect vertical scroll
                            int nowY = (int) event.getRawY();
                            int offsetY = nowY - firstY;
                            if (Math.abs(offsetY) > 32 && !move) {
                                lock = true;
                                return false;
                            }

                            // Horizontal scroll
                            int nowX = (int) event.getRawX();
                            int offsetX = nowX - firstX;
                            if (Math.abs(offsetX) > 32 || move) {
                                if (lock) return false;
                                move = true; // block OnClickListener and OnLongClickListener
                                ViewGroup.LayoutParams lp = sidebar.getLayoutParams();
                                int lastSize = lp.width;
                                int newSize = lastSize - offsetX;
                                if (newSize < 72) newSize = 72;
                                if (newSize > displayWidth) newSize = displayWidth - 72;
                                lp.width = newSize;
                                sidebar.setLayoutParams(lp);
                                firstX = nowX;

                                SharedPreferences.Editor editor = prefs.edit();
                                editor.putInt("SideBarSize", sidebar.getLayoutParams().width);
                                editor.apply();
                            }
                            break;
                    }
                    return move;
                }
            };
        } else {
            sidebar.setVisibility(View.GONE);
        }

        TextView chat_state = (TextView) findViewById(R.id.chat_state);
        chat_state.setBackgroundColor(Colors.GROUP_BACKGROUND);
        chat_state.setTextColor(Colors.GROUP_FOREGROUND);
    }

    @Override
    public void onResume() {
        super.onResume();
        compose = false;
        jid = getIntent().getStringExtra("jid");
        account = getIntent().getStringExtra("account");

        if (getIntent().getBooleanExtra("file", false)) onActivityResult(REQUEST_FILE, RESULT_OK, getIntent());

        if (service.getConferencesHash(account).containsKey(jid)) {
            isMuc = true;
            muc = service.getConferencesHash(account).get(jid);
            String nick = muc.getNickname();
            if (nick == null) nick = StringUtils.parseName(account);
            if (prefs.getBoolean("ShowInputHints", true)) messageInput.setHint(getString(R.string.From) + " " + nick);

            String group = listMucAdapter.getGroup();
            if (listView.getAdapter() instanceof ChatAdapter) {
                listView.setAdapter(listMucAdapter);
                listView.setScroll(true);
            }
            else {
                if (group != null && group.equals(jid)) listView.setScroll(false); else listView.setScroll(true);
            }

            if (prefs.getBoolean("OldUserList", false)) {
                int width = prefs.getInt("SideBarSize", 0);
                if (width == 0) {
                    width = (int)(160 * this.getResources().getDisplayMetrics().density);
                }

                ViewGroup.LayoutParams lp = sidebar.getLayoutParams();
                lp.width = width;
                sidebar.setLayoutParams(lp);
                if (prefs.getBoolean("ShowSidebar", true)) {
                    sidebar.setVisibility(View.VISIBLE);
                } else {
                    sidebar.setVisibility(View.GONE);
                }

                nickList.setOnTouchListener(onTouchListener);
            }
        } else {
            isMuc = false;
            muc = null;
            resource = StringUtils.parseResource(jid);

            if (!service.getConferencesHash(account).containsKey(StringUtils.parseBareAddress(jid))) {
                jid = StringUtils.parseBareAddress(jid);
                isPrivate = false;
            } else isPrivate = true;

            if (resource == null || resource.equals("")) resource = service.getResource(account, jid);

            if (prefs.getBoolean("ShowInputHints", true)) {
                if (resource != null && !resource.equals("")) {
                    messageInput.setHint(getString(R.string.To) + " " + resource + " " + getString(R.string.From) + " " + StringUtils.parseName(account));
                } else messageInput.setHint(getString(R.string.From) + " " + StringUtils.parseName(account));
            }

            String j = listAdapter.getJid();
            listAdapter.update(account, jid, searchString, viewMode);
            if (listView.getAdapter() instanceof MucChatAdapter) {
                listView.setAdapter(listAdapter);
                listView.setScroll(true);
            }
            else {
                if (j != null && j.equals(jid)) listView.setScroll(false); else listView.setScroll(true);
            }

            if (!service.getActiveChats(account).contains(jid)) {
                service.addActiveChat(account, jid);
            }

            sidebar.setVisibility(View.GONE);
        }

        if (!isMuc || prefs.getBoolean("OldUserList", false)) {
            DrawerLayout drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
            LinearLayout rightDrawer = (LinearLayout) findViewById(R.id.right_drawer_layout);
            drawerLayout.removeView(rightDrawer);
        }

        service.setCurrentJid(jid);
        service.removeUnreadMesage(account, jid);
        service.removeHighlight(account, jid);

        usersAdapter = new MucUserAdapter(this, account, jid);
        if (prefs.getBoolean("OldUserList", false)) {
            nickList.setAdapter(usersAdapter);
        } else {
            rightNickList.setAdapter(usersAdapter);
        }

        messageInput.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {
                if (s != null && s.length() > 0) {
                    if (!isMuc && prefs.getBoolean("SendChatState", true)) {
                        if (!compose) {
                            compose = true;
                            service.setChatState(account, jid, ChatState.composing);
                        }
                    }
                    // sendButton.setEnabled(service.isAuthenticated(account));
                } else {
                    if (!isMuc && prefs.getBoolean("SendChatState", true)) {
                        if (compose) {
                            compose = false;
                            service.setChatState(account, jid, ChatState.active);
                        }
                    }
                    // sendButton.setEnabled(false);
                }
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
        });

        messageInput.setText(service.getText(jid));
        messageInput.setSelection(messageInput.getText().length());

        if (service.isAuthenticated()) Notify.updateNotify();
        else Notify.offlineNotify(this, service.getGlobalState());
        Notify.cancelNotify(this, account, jid);

        updateUsers();
        updateStatus();

        registerReceivers();
        service.resetTimer();

        createOptionMenu();

        if (searchString.length() > 0) {
            if (menu != null) {
                MenuItem item = menu.findItem(R.id.search);
                item.expandActionView();
            }
        }

        unreadMessages = service.getMessagesCount(account, jid);
        if (unreadMessages > 0) separatorPosition = 0;
        if (service.getMessageList(account, jid).isEmpty()) onRefresh();
        if (account.equals(jid)) {
            service.removeMessagesCountForJid(account, jid);
        }

        service.removeMessagesCount(account, jid);

        updateChatState();
        updateList();
        drawerAdapter.update();

        if (!isMuc && prefs.getBoolean("SendChatState", true))
            service.setChatState(account, jid, ChatState.active);
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceivers();
        compose = false;
        if (!isMuc)  {
            service.setResource(account, jid, resource);
            if (service.getMessageList(account, jid).isEmpty()) service.removeActiveChat(account, jid);
        } else {
            if (prefs.getBoolean("OldUserList", false)) {
                SharedPreferences.Editor editor = prefs.edit();
                editor.putInt("SideBarSize", sidebar.getLayoutParams().width);
                editor.apply();
            }
        }
        service.setCurrentJid("me");
        service.setText(jid, messageInput.getText().toString());
        if (!listView.isScroll()) service.addLastPosition(jid, listView.getFirstVisiblePosition());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (service.getMessageList(account,jid).isEmpty()) {
            if (!isMuc && prefs.getBoolean("SendChatState", true)) service.setChatState(account, jid, ChatState.gone);
        } else {
            if (!isMuc && prefs.getBoolean("SendChatState", true)) service.setChatState(account, jid, ChatState.inactive);
        }
        jid = null;
        account = null;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString("account", account);
    }

    @Override
    protected void onRestoreInstanceState(Bundle inState) {
        account = inState.getString("account");
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
    public void onRefresh() {
        swipeRefreshLayout.setRefreshing(true);
        swipeRefreshLayout.postDelayed(new Runnable() {
            @Override
            public void run() {
                String where = "jid = '" + jid + "'";

                String msgId = null;
                if (listView.getCount() > 0) {
                    for(int i = 0; i < listView.getCount(); i++) {
                        MessageItem msgItem = (MessageItem) listView.getItemAtPosition(i);
                        if (msgItem.getType() == MessageItem.Type.message || msgItem.getType() == MessageItem.Type.important) {
                            if (msgItem.getBaseId() != null) {
                                msgId = msgItem.getBaseId();
                                break;
                            }
                        }
                    }
                }

                if (msgId != null) {
                    try {
                        where += " AND _id < " + Integer.parseInt(msgId);
                    } catch (NumberFormatException ignored) { }
                }

                where += " AND (type = '" + MessageItem.Type.message.name() + "' OR type = '" + MessageItem.Type.important + "')";

                Cursor cursor = getContentResolver().query(JTalkProvider.CONTENT_URI, null, where, null, "_id DESC, stamp DESC LIMIT " + Constants.LOAD_MESSAGES_COUNT);
                if (cursor != null && cursor.getCount() > 0) {
                    cursor.moveToFirst();

                    List<MessageItem> list = service.getMessageList(account, jid);

                    do {
                        String baseId = cursor.getString(cursor.getColumnIndex(MessageDbHelper._ID));
                        String id = cursor.getString(cursor.getColumnIndex(MessageDbHelper.ID));
                        String nick = cursor.getString(cursor.getColumnIndex(MessageDbHelper.NICK));
                        String type = cursor.getString(cursor.getColumnIndex(MessageDbHelper.TYPE));
                        String stamp = cursor.getString(cursor.getColumnIndex(MessageDbHelper.STAMP));
                        String body = cursor.getString(cursor.getColumnIndex(MessageDbHelper.BODY));
                        boolean received = Boolean.valueOf(cursor.getString(cursor.getColumnIndex(MessageDbHelper.RECEIVED)));

                        MessageItem item = new MessageItem(account, jid, id);
                        item.setBaseId(baseId);
                        item.setName(nick);
                        item.setType(MessageItem.Type.valueOf(type));
                        item.setTime(stamp);
                        item.setBody(body);
                        item.setReceived(received);

                        if (!list.contains(item)) list.add(0, item);
                    } while (cursor.moveToNext());

                    service.setMessageList(account, jid, list);
                    cursor.close();
                }

                swipeRefreshLayout.setRefreshing(false);
                updateList();
            }
        }, 1000);
        swipeRefreshLayout.setRefreshing(true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        this.menu = menu;
        createOptionMenu();
        return true;
    }

    private void createOptionMenu() {
        if (menu != null) {
            menu.clear();
            final MenuInflater inflater = getMenuInflater();

            if (viewMode == ChatAdapter.ViewMode.multi) {
                inflater.inflate(R.menu.select_messages, menu);
                super.onCreateOptionsMenu(menu);
            } else {
                if (isMuc) inflater.inflate(R.menu.muc_chat, menu);
                else {
                    inflater.inflate(R.menu.chat, menu);
                    menu.findItem(R.id.resource).setVisible(!isPrivate);

                    if (!isPrivate && prefs.getBoolean("DirectFileTransfer", true))
                        menu.findItem(R.id.file_direct).setVisible(true);
                }

                MenuItem.OnActionExpandListener listener = new MenuItem.OnActionExpandListener() {
                    @Override
                    public boolean onMenuItemActionCollapse(MenuItem item) {
                        searchString = "";
                        updateList();
                        createOptionMenu();
                        return true;
                    }

                    @Override
                    public boolean onMenuItemActionExpand(MenuItem item) {
                        return true;
                    }
                };

                SearchView searchView = new SearchView(this);
                searchView.setQueryHint(getString(android.R.string.search_go));
                searchView.setSubmitButtonEnabled(false);
                searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                    @Override
                    public boolean onQueryTextChange(String newText) {
                        return true;
                    }

                    @Override
                    public boolean onQueryTextSubmit(String query) {
                        searchString = query;
                        updateList();
                        return true;
                    }
                });

                MenuItem item = menu.findItem(R.id.search);
                item.setActionView(searchView);
                item.setShowAsAction(MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);
                item.setOnActionExpandListener(listener);

                MenuItem countMenu = menu.findItem(R.id.count);
                int count = service.getMessagesCount(jid);
                countMenu.setTitle(count+"");
                if (count > 0) {
                    countMenu.setVisible(true);
                    if (service.isHighlight()) countMenu.setIcon(R.drawable.ic_mail_outline_highlight);
                } else {
                    countMenu.setVisible(false);
                }

                super.onCreateOptionsMenu(menu);
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.count:
                Intent nextChat = service.getNextChat();
                if (nextChat != null) this.startActivity(nextChat);
                break;
            case R.id.sidebar:
                if (prefs.getBoolean("OldUserList", false)) {
                    SharedPreferences.Editor editor = prefs.edit();
                    if (sidebar.getVisibility() == View.GONE) {
                        sidebar.setVisibility(View.VISIBLE);
                        editor.putBoolean("ShowSidebar", true);
                    } else {
                        sidebar.setVisibility(View.GONE);
                        editor.putBoolean("ShowSidebar", false);
                    }
                    editor.apply();
                } else {
                    DrawerLayout drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
                    if (drawerLayout.isDrawerOpen(GravityCompat.START)) drawerLayout.closeDrawer(GravityCompat.START);
                    try {
                        if (!drawerLayout.isDrawerOpen(GravityCompat.END)) drawerLayout.openDrawer(GravityCompat.END);
                        else drawerLayout.closeDrawer(GravityCompat.END);
                    } catch(IllegalArgumentException ignored) {}
                }
                updateUsers();
                break;
            case android.R.id.home:
                startActivity(new Intent(this, RosterActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
                finish();
                break;
            case R.id.subj:
                Intent sIntent = new Intent(this, SubjectActivity.class);
                sIntent.putExtra("account", account);
                sIntent.putExtra("jid", jid);
                startActivity(sIntent);
                break;
            case R.id.templates:
                startActivityForResult(new Intent(this, TemplatesActivity.class), REQUEST_TEMPLATES);
                break;
            case R.id.resource:
                final List<String> list = new ArrayList<String>();
                list.add("Auto");
                Iterator<Presence> it =  service.getRoster(account).getPresences(jid);
                while (it.hasNext()) {
                    Presence p = it.next();
                    if (p.isAvailable()) list.add(StringUtils.parseResource(p.getFrom()));
                }

                CharSequence[] array = new CharSequence[list.size()];
                for (int i = 0; i < list.size(); i++) array[i] = list.get(i);

                AlertDialog.Builder b = new AlertDialog.Builder(this);
                b.setTitle(R.string.SelectResource);
                b.setItems(array, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        String res = "" ;
                        if (which > 0) {
                            res = list.get(which);
                        }
                        if (res.length() > 0) res = jid + "/" + res;
                        else res = jid;
                        resource = "";
                        Intent intent = new Intent();
                        intent.putExtra("account", account);
                        intent.putExtra("jid", res);
                        setIntent(intent);
                        onPause();
                        onResume();
                    }
                });
                b.create().show();
                break;
            case R.id.info:
                Intent infoIntent = new Intent(this, VCardActivity.class);
                infoIntent.putExtra("account", account);
                infoIntent.putExtra("jid", jid);
                startActivity(infoIntent);
                break;
            case R.id.file_direct:
                sendFileDirect = true;
            case R.id.file:
                Intent fileIntent = new Intent(Intent.ACTION_GET_CONTENT);
                fileIntent.setType("*/*");
                fileIntent.addCategory(Intent.CATEGORY_OPENABLE);
                startActivityForResult(Intent.createChooser(fileIntent, getString(R.string.SelectFile)), REQUEST_FILE);
                break;
            case R.id.invite:
                MucDialogs.inviteDialog(this, account, jid);
                break;
            case R.id.delete_history:
                service.setMessageList(account, jid, new ArrayList<MessageItem>());
                new Thread() {
                    public void run() {
                        Chat.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                getContentResolver().delete(JTalkProvider.CONTENT_URI, "jid = '" + jid + "'", null);
                                updateList();
                            }
                        });
                    }
                }.start();
                break;
            case R.id.export_history:
                Intent export = new Intent(this, ExportActivity.class);
                export.putExtra("jid", jid);
                startActivity(export);
                break;
            case R.id.close:
                closeChat();
                break;
            case R.id.leave:
                finish();
                service.leaveRoom(account, jid);
                break;
            case R.id.search:
                if (!item.isActionViewExpanded()) {
                    menu.removeItem(R.id.sidebar);
                    menu.removeItem(R.id.smile);
                    item.expandActionView();
                }
                break;
            case R.id.select:
                viewMode = ChatAdapter.ViewMode.multi;
                createOptionMenu();
                updateList();
                break;
            case R.id.copy:
                if (listView.getAdapter() instanceof ChatAdapter) listAdapter.copySelectedMessages();
                else if (listView.getAdapter() instanceof MucChatAdapter) listMucAdapter.copySelectedMessages();
                break;
            case R.id.finish:
                viewMode = ChatAdapter.ViewMode.single;
                createOptionMenu();
                updateList();
                break;
            case R.id.add_bookmark:
                BookmarksDialogs.AddDialog(this, account, jid, StringUtils.parseName(jid));
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != RESULT_OK) return;
        if (requestCode == REQUEST_TEMPLATES) {
            String text = data.getStringExtra("text");
            String oldtext = service.getText(jid);
            if (oldtext.length() > 0) text = oldtext + " " + text;
            service.setText(jid, text);
        } else {
            Uri uri = data.getData();
            if (uri == null) return;
            attachPath.setText(uri.toString());
            attachPanel.setVisibility(View.VISIBLE);
            service.setText(jid, messageInput.getText().toString());
        }
    }

    @Override
    public void onClick(View v) {
        if (v == sendButton) {
            if (!service.isAuthenticated(account) || (isMuc && (muc == null || !muc.isJoined()))) {
                Toast.makeText(this, "You offline", Toast.LENGTH_LONG).show();
                return;
            }

            String attach = attachPath.getText().toString();
            if (attach.isEmpty()) {
                if (messageInput.getText().length() > 0) sendMessage();
            } else {
                Uri uri = Uri.parse(attach);
                if (uri == null) return;
                String text = messageInput.getText().toString();
                String j = jid;

                if (isPrivate) j = jid;
                else if (resource != null && resource.length() > 0) j = jid + "/" + resource;

                if (sendFileDirect || jid.contains("@disk.") || jid.contains("@jdisk.")) {
                    if (resource == null || resource.isEmpty()) {
                        Presence presence = service.getRoster(account).getPresence(jid);
                        if (presence != null) j = jid + "/" + StringUtils.parseResource(presence.getFrom());
                    }
                    new SendFileTask(account, j, text, FileUtils.getPath(this, uri)).execute();
                    sendFileDirect = false;
                } else new FileHttpUploadTask(FileUtils.getPath(this, uri), account, j, text, muc).execute();

                removeAttach.callOnClick();
                attachPath.setText("");
                attachPanel.setVisibility(View.GONE);
            }

            service.resetTimer();
            messageInput.setText("");
            if (prefs.getBoolean("HideKeyboard", true)) {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(messageInput.getWindowToken(), 0, null);
            }
        } else if (v == removeAttach) {
            attachPanel.setVisibility(View.GONE);
            attachPath.setText("");
//            if (messageInput.getText().length() < 1) {
                // sendButton.setEnabled(false);
//            }
        }
    }

//    private void updateMessage(String id, String body) {
//        for (MessageItem item : msgList) {
//            if (item.getType() == MessageItem.Type.message) {
//                if (id.equals(item.getId())) {
//                    item.setBody(body);
//                    item.setEdited(true);
//                    listAdapter.notifyDataSetChanged();
//                }
//            }
//        }
//    }

    private void updateList() {
        if (isMuc) {
            listMucAdapter.update(account, jid, muc.getNickname(), searchString, viewMode);
            listMucAdapter.notifyDataSetChanged();
        } else {
            listAdapter.update(account, jid, searchString, viewMode);
            listAdapter.notifyDataSetChanged();
        }

        try {
            if (unreadMessages > 0 && separatorPosition == 0) {
                for (int i = 1; i <= unreadMessages; i++) {
                    MessageItem item = (MessageItem) listView.getAdapter().getItem(listView.getCount() - i);
                    if (item.getType() == MessageItem.Type.connectionstatus || item.getType() == MessageItem.Type.status) {
                        unreadMessages++;
                    }
                }
                listView.setScroll(false);
                separatorPosition = listView.getCount() - unreadMessages;
            }

            if (separatorPosition > 0 && separatorPosition < listView.getCount()) {
                MessageItem item = new MessageItem(null, null, null);
                item.setType(MessageItem.Type.separator);
                if (!isMuc) listAdapter.insert(item, separatorPosition);
                else listMucAdapter.insert(item, separatorPosition);

                if (first){
                    first = false;
                    listView.setSelection(separatorPosition);
                }
            }
        } catch (Exception ignored) {}

        if (prefs.getBoolean("AutoScroll", true)) {
            if (listView.isScroll() && listView.getCount() >= 1) {
                listView.setSelection(listView.getCount());
            }
        }
    }


    private void updateUsers() {
        new Thread() {
            public void run() {
                Chat.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        usersAdapter.update();
                        usersAdapter.notifyDataSetChanged();
                    }
                });
            }
        }.start();
    }

    private void updateStatus() {
        if (service != null) {
            drawerAdapter.update();
            IconPicker ip = service.getIconPicker();
            ActionBar ab = getActionBar();
            if (ip != null && ab != null) {
                ab.setDisplayUseLogoEnabled(true);
                if (isMuc) {
                    ab.setLogo(ip.getMucDrawable());
                    ab.setTitle(StringUtils.parseName(muc.getRoom()));
                    ab.setSubtitle(muc.getSubject());

                    Window window = getWindow();
                    if (muc.isJoined()) {
                        if (service.isAuthenticated()) {
                            String mode = prefs.getString("currentMode", "available");
                            if (mode.equals("chat")) {
                                if (Build.VERSION.SDK_INT >= 21) {
                                    window.setStatusBarColor(Colors.STATUSBAR_CHAT);
                                    ab.setBackgroundDrawable(new ColorDrawable(Colors.ACTIONBAR_CHAT));
                                }
                                fab.setFloatingActionButtonColor(Colors.ACTIONBAR_CHAT);
                                swipeRefreshLayout.setColorSchemeColors(Colors.ACTIONBAR_CHAT);
                            } else if (mode.equals("away")) {
                                if (Build.VERSION.SDK_INT >= 21) {
                                    window.setStatusBarColor(Colors.STATUSBAR_AWAY);
                                    ab.setBackgroundDrawable(new ColorDrawable(Colors.ACTIONBAR_AWAY));
                                }
                                fab.setFloatingActionButtonColor(Colors.STATUSBAR_AWAY);
                                swipeRefreshLayout.setColorSchemeColors(Colors.ACTIONBAR_AWAY);
                            } else if (mode.equals("xa")) {
                                if (Build.VERSION.SDK_INT >= 21) {
                                    window.setStatusBarColor(Colors.STATUSBAR_XA);
                                    ab.setBackgroundDrawable(new ColorDrawable(Colors.ACTIONBAR_XA));
                                }
                                fab.setFloatingActionButtonColor(Colors.ACTIONBAR_XA);
                                swipeRefreshLayout.setColorSchemeColors(Colors.ACTIONBAR_XA);
                            } else if (mode.equals("dnd")) {
                                if (Build.VERSION.SDK_INT >= 21) {
                                    window.setStatusBarColor(Colors.STATUSBAR_DND);
                                    ab.setBackgroundDrawable(new ColorDrawable(Colors.ACTIONBAR_DND));
                                }
                                fab.setFloatingActionButtonColor(Colors.ACTIONBAR_DND);
                                swipeRefreshLayout.setColorSchemeColors(Colors.ACTIONBAR_DND);
                            } else {
                                if (Build.VERSION.SDK_INT >= 21) {
                                    window.setStatusBarColor(Colors.STATUSBAR_ONLINE);
                                    ab.setBackgroundDrawable(new ColorDrawable(Colors.ACTIONBAR_ONLINE));
                                }
                                fab.setFloatingActionButtonColor(Colors.ACTIONBAR_ONLINE);
                                swipeRefreshLayout.setColorSchemeColors(Colors.ACTIONBAR_ONLINE);
                            }
                        } else {
                            if (Build.VERSION.SDK_INT >= 21) {
                                window.setStatusBarColor(Colors.STATUSBAR_OFFLINE);
                                ab.setBackgroundDrawable(new ColorDrawable(Colors.ACTIONBAR_OFFLINE));
                            }
                            fab.setFloatingActionButtonColor(Colors.ACTIONBAR_OFFLINE);
                            swipeRefreshLayout.setColorSchemeColors(Colors.ACTIONBAR_OFFLINE);
                        }
                    } else {
                        if (Build.VERSION.SDK_INT >= 21) {
                            window.setStatusBarColor(Colors.STATUSBAR_OFFLINE);
                            ab.setBackgroundDrawable(new ColorDrawable(Colors.ACTIONBAR_OFFLINE));
                        }
                        fab.setFloatingActionButtonColor(Colors.ACTIONBAR_OFFLINE);
                        swipeRefreshLayout.setColorSchemeColors(Colors.ACTIONBAR_OFFLINE);
                    }
                }
                else {
                    Presence presence = service.getPresence(account, jid);
                    ab.setSubtitle(presence.getStatus());
                    ab.setLogo(ip.getDrawableByPresence(presence));

                    if (isPrivate) {
                        ab.setTitle(StringUtils.parseResource(jid));
                    } else {
                        Roster roster = service.getRoster(account);
                        if (roster != null && roster.getEntry(StringUtils.parseBareAddress(jid)) != null) {
                            RosterEntry entry = roster.getEntry(StringUtils.parseBareAddress(jid));
                            ab.setTitle(entry.getName());
                        } else {
                            ab.setTitle(StringUtils.parseBareAddress(jid));
                        }
                    }

                    Window window = getWindow();
                    if (presence.getType() == Presence.Type.available) {
                        Presence.Mode mode = presence.getMode();
                        if(mode == Presence.Mode.away) {
                            if (Build.VERSION.SDK_INT >= 21) {
                                window.setStatusBarColor(Colors.STATUSBAR_AWAY);
                                ab.setBackgroundDrawable(new ColorDrawable(Colors.ACTIONBAR_AWAY));
                            }
                            fab.setFloatingActionButtonColor(Colors.ACTIONBAR_AWAY);
                            swipeRefreshLayout.setColorSchemeColors(Colors.ACTIONBAR_AWAY);

                        } else if (mode == Presence.Mode.xa) {
                            if (Build.VERSION.SDK_INT >= 21) {
                                window.setStatusBarColor(Colors.STATUSBAR_XA);
                                ab.setBackgroundDrawable(new ColorDrawable(Colors.ACTIONBAR_XA));
                            }
                            fab.setFloatingActionButtonColor(Colors.ACTIONBAR_XA);
                            swipeRefreshLayout.setColorSchemeColors(Colors.ACTIONBAR_XA);
                        } else if (mode == Presence.Mode.dnd) {
                            if (Build.VERSION.SDK_INT >= 21) {
                                window.setStatusBarColor(Colors.STATUSBAR_DND);
                                ab.setBackgroundDrawable(new ColorDrawable(Colors.ACTIONBAR_DND));
                            }
                            fab.setFloatingActionButtonColor(Colors.ACTIONBAR_DND);
                            swipeRefreshLayout.setColorSchemeColors(Colors.ACTIONBAR_DND);
                        } else if (mode == Presence.Mode.chat) {
                            if (Build.VERSION.SDK_INT >= 21) {
                                window.setStatusBarColor(Colors.STATUSBAR_CHAT);
                                ab.setBackgroundDrawable(new ColorDrawable(Colors.ACTIONBAR_CHAT));
                            }
                            fab.setFloatingActionButtonColor(Colors.ACTIONBAR_CHAT);
                            swipeRefreshLayout.setColorSchemeColors(Colors.ACTIONBAR_CHAT);
                        }
                        else {
                            if (Build.VERSION.SDK_INT >= 21) {
                                window.setStatusBarColor(Colors.STATUSBAR_ONLINE);
                                ab.setBackgroundDrawable(new ColorDrawable(Colors.ACTIONBAR_ONLINE));
                            }
                            fab.setFloatingActionButtonColor(Colors.ACTIONBAR_ONLINE);
                            swipeRefreshLayout.setColorSchemeColors(Colors.ACTIONBAR_ONLINE);
                        }
                    } else {
                        if (Build.VERSION.SDK_INT >= 21) {
                            window.setStatusBarColor(Colors.STATUSBAR_OFFLINE);
                            ab.setBackgroundDrawable(new ColorDrawable(Colors.ACTIONBAR_OFFLINE));
                        }
                        fab.setFloatingActionButtonColor(Colors.ACTIONBAR_OFFLINE);
                        swipeRefreshLayout.setColorSchemeColors(Colors.ACTIONBAR_OFFLINE);
                    }
                }
            }
        }
    }

    private void registerReceivers() {
        textReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context c, Intent i) {
                String text = i.getExtras().getString("text");

                if (isMuc && !i.getBooleanExtra("smile", false)) {
                    Presence presence = service.getPresence(account, jid+"/"+text);
                    if (presence == null || presence.getType() == Presence.Type.unavailable) {
                        Toast.makeText(Chat.this, text + " is offline", Toast.LENGTH_SHORT).show();
                    }

                    if (messageInput.getText().length() < 1) {
                        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(Chat.this);
                        String separator = prefs.getString("nickSeparator", ", ");
                        text += separator;
                    }
                }

                int pos = messageInput.getSelectionEnd();
                String oldText = messageInput.getText().toString();
                String newText;
                if (pos > 0 && oldText.length() > 1 && !oldText.substring(pos-1).equals(" ")) newText = oldText.substring(0, pos) + " " + text + oldText.substring(pos);
                else newText = oldText.substring(0, pos) + text + oldText.substring(pos);
                messageInput.setText(newText);
                messageInput.setSelection(messageInput.getText().length());
                messageInput.requestFocus();
            }
        };

        msgReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                drawerAdapter.update();
                String user = intent.getExtras().getString("jid");
                boolean clear = intent.getBooleanExtra("clear", false);
                if (user != null && user.equals(jid)) {
                    updateList();
                } else {
                    updateUsers();
                }
                if (clear) messageInput.setText("");

                updateChatState();

                if (menu != null) {
                    MenuItem item = menu.findItem(R.id.count);
                    if (item != null) {
                        int count = service.getMessagesCount(jid);
                        item.setTitle(count+"");
                        if (count > 0) {
                            item.setVisible(true);
                            if (service.isHighlight()) item.setIcon(R.drawable.ic_mail_outline_highlight);
                        } else {
                            item.setVisible(false);
                        }
                    }
                }
            }
        };

        receivedReceiver =  new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (!isMuc) updateList();
            }
        };

        composeReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                drawerAdapter.update();
                updateStatus();
                updateChatState();
            }
        };

        presenceReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                drawerAdapter.update();
                updateUsers();

                Bundle extras = intent.getExtras();
                if (extras != null) {
                    if (!isMuc) {
                        String j = extras.getString("jid");
                        if (j != null && j.equals(jid)) {
                            updateStatus();
                            updateList();
                        }
                    } else {
                        if (extras.getBoolean("join", false) && jid.equals(extras.getString("group", ""))) {
                            if (service.getConferencesHash(account).containsKey(jid))
                            muc = service.getConferencesHash(account).get(jid);
                        }
                    }
                }
            }
        };

        changeChatReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent i) {
                String account = i.getStringExtra("account");
                String jid = i.getStringExtra("jid");
                if (account == null || jid == null) return;

                Intent intent = new Intent();
                intent.putExtra("account", account);
                intent.putExtra("jid", jid);
                setIntent(intent);
                onPause();
                onResume();
            }
        };

        finishReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                finish();
            }
        };

        registerReceiver(finishReceiver, new IntentFilter(Constants.FINISH));
        registerReceiver(textReceiver, new IntentFilter(Constants.PASTE_TEXT));
        registerReceiver(msgReceiver, new IntentFilter(Constants.NEW_MESSAGE));
        registerReceiver(receivedReceiver, new IntentFilter(Constants.RECEIVED));
        registerReceiver(composeReceiver, new IntentFilter(Constants.UPDATE));
        registerReceiver(presenceReceiver, new IntentFilter(Constants.PRESENCE_CHANGED));
        registerReceiver(changeChatReceiver, new IntentFilter(Constants.CHANGE_CHAT));
    }

    private void unregisterReceivers() {
        try {
            unregisterReceiver(textReceiver);
            unregisterReceiver(finishReceiver);
            unregisterReceiver(msgReceiver);
            unregisterReceiver(receivedReceiver);
            unregisterReceiver(composeReceiver);
            unregisterReceiver(presenceReceiver);
            unregisterReceiver(changeChatReceiver);
        } catch (Exception ignored) { }
    }

    private void sendMessage() {
        String message = messageInput.getText().toString();
        if (isMuc) {
            try {
                muc.sendMessage(message);
            } catch (Exception ignored) {}
        }
        else {
            String to = jid;
            if (isPrivate) to = jid;
            else if (resource.length() > 0) to = jid + "/" + resource;
            service.sendMessage(account, to, message);
            if (prefs.getBoolean("SendChatState", true)) service.setChatState(account, jid, ChatState.active);
        }
    }

    public void onScrollStateChanged(AbsListView view, int scrollState) { }
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        if (firstVisibleItem + visibleItemCount == totalItemCount) {
            listView.setScroll(true);
            fab.hideFloatingActionButton();
        }
        else {
            listView.setScroll(false);
            if (firstVisibleItem + visibleItemCount < totalItemCount - 1) {
                DrawerLayout mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
                if (!mDrawerLayout.isDrawerOpen(GravityCompat.END) && !mDrawerLayout.isDrawerOpen(GravityCompat.START)) fab.showFloatingActionButton();
            }
        }
    }

    private void closeChat() {
        service.setMessageList(account, jid, new ArrayList<MessageItem>());
        service.removeActiveChat(account, jid);
        finish();
    }

    private void updateChatState() {
        TextView chat_state = (TextView) findViewById(R.id.chat_state);
        Roster roster = service.getRoster(account);
        chat_state.setVisibility(View.GONE);
        if (roster != null) {
            ChatState state = roster.getChatState(jid);
            if (state != null && prefs.getBoolean("ShowChatState", true)) {
                if (state == ChatState.composing) {
                    chat_state.setText(R.string.UserComposing);
                } else if (state == ChatState.active) {
                    chat_state.setText(R.string.UserActive);
                } else if (state == ChatState.inactive) {
                    chat_state.setText(R.string.UserInactive);
                } else if (state == ChatState.paused) {
                    chat_state.setText(R.string.UserPaused);
                } else if (state == ChatState.gone) {
                    chat_state.setText(R.string.UserGone);
                }
                chat_state.setVisibility(View.VISIBLE);
            } else chat_state.setVisibility(View.GONE);
        }
    }

    private void prepareDrawer() {
        drawerAdapter = new DrawerAdapter(this);

        DrawerClickListener drawerClickListener = new DrawerClickListener(this);
        ListView drawerList = (ListView) findViewById(R.id.left_drawer);
        drawerList.setAdapter(drawerAdapter);
        drawerList.setOnItemClickListener(drawerClickListener);
        drawerList.setOnItemLongClickListener(drawerClickListener);

        ImageView icon = (ImageView) findViewById(R.id.status_button);
        icon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RosterDialogs.changeStatusDialog(Chat.this, null, null);
            }
        });
        icon = (ImageView) findViewById(R.id.settings_button);
        icon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Chat.this, Preferences.class));
            }
        });
        icon = (ImageView) findViewById(R.id.accounts_button);
        icon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Chat.this, Accounts.class));
            }
        });
        icon = (ImageView) findViewById(R.id.notes_button);
        icon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Chat.this, NotesActivity.class));
            }
        });
        icon = (ImageView) findViewById(R.id.disco_button);
        icon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Chat.this, ServiceDiscovery.class));
            }
        });
    }
}
