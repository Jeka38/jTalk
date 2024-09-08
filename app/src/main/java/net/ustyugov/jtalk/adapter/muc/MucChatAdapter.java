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

package net.ustyugov.jtalk.adapter.muc;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import android.app.Activity;
import android.content.*;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.Layout;
import android.text.Spanned;
import android.text.style.*;
import android.widget.*;
import net.ustyugov.jtalk.*;
import net.ustyugov.jtalk.adapter.ChatAdapter;
import net.ustyugov.jtalk.listener.MyTextLinkClickListener;
import net.ustyugov.jtalk.service.JTalkService;
import net.ustyugov.jtalk.smiles.Smiles;
import net.ustyugov.jtalk.view.MyTextView;

import com.jtalk2.R;

import android.preference.PreferenceManager;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class MucChatAdapter extends ArrayAdapter<MessageItem> {
    private String searchString = "";
    private String[] highArray;

    private Activity activity;
    private Smiles smiles;
    private String account;
    private String nick;
    private String group;
    private boolean showtime = false;
    private ChatAdapter.ViewMode viewMode = ChatAdapter.ViewMode.single;

    private SharedPreferences prefs;

    public MucChatAdapter(Activity activity, Smiles smiles) {
        super(activity, R.id.chat1);
        this.activity = activity;
        this.smiles = smiles;
        this.prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        this.showtime = prefs.getBoolean("ShowTime", true);

        String highString = prefs.getString("Highlights", "");
        highArray = highString.split(" ");
    }

    public String getGroup() { return this.group; }

    public void update(String account, String group, String nick, String searchString, ChatAdapter.ViewMode viewMode) {
        if (this.viewMode == ChatAdapter.ViewMode.multi && viewMode == ChatAdapter.ViewMode.multi) return;
        this.viewMode = viewMode;
        this.account = account;
        this.group = group;
        this.nick = nick;
        this.searchString = searchString;
        clear();

        String showStatusMode = prefs.getString("StatusMessagesMode", "2");
        List<MessageItem> messages = JTalkService.getInstance().getMessageList(account, group);
        for (int i = 0; i < messages.size(); i++) {
            MessageItem item = messages.get(i);
            MessageItem.Type type = item.getType();
            if (searchString.length() > 0) {
                String name = item.getName();
                String body = item.getBody();
                String time = createTimeString(item.getTime());
                if (type == MessageItem.Type.status || type == MessageItem.Type.connectionstatus) {
                    if (showtime) body = time + "  " + body;
                } else {
                    if (showtime) body = time + " " + name + ": " + body;
                    else body = name + ": " + body;
                }

                if (body.toLowerCase().contains(searchString.toLowerCase())) {
                    add(item);
                }
            } else {
                switch (showStatusMode) {
                    case "0":
                        if (type != MessageItem.Type.status && type != MessageItem.Type.connectionstatus) add(item);
                        break;
                    case "1":
                        add(item);
                        break;
                    default:
                        if (type != MessageItem.Type.status) add(item);
                        break;
                }
            }
        }
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        int fontSize = Integer.parseInt(activity.getResources().getString(R.string.DefaultFontSize));
        int timeSize = Integer.parseInt(activity.getResources().getString(R.string.DefaultTimeSize));
        try {
            fontSize = Integer.parseInt(prefs.getString("FontSize", activity.getResources().getString(R.string.DefaultFontSize)));
            timeSize = Integer.parseInt(prefs.getString("TimeSize", activity.getResources().getString(R.string.DefaultTimeSize)));
        } catch (NumberFormatException ignored) {	}

        Holders.MessageHolder holder = new Holders.MessageHolder();
        if (convertView == null) {
            LayoutInflater vi = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = vi.inflate(R.layout.chat_item, null);

            holder.linear = (LinearLayout) convertView.findViewById(R.id.chat_item);
            holder.linear.setMinimumHeight(Integer.parseInt(prefs.getString("SmilesSize", "24")));
            holder.check = (CheckBox) convertView.findViewById(R.id.check);
            holder.text = (MyTextView) convertView.findViewById(R.id.chat1);
            holder.text.setOnTextLinkClickListener(new MyTextLinkClickListener(activity));
            holder.text.setTextSize(fontSize);

            convertView.setBackgroundColor(0X00000000);
            convertView.setTag(holder);
        } else {
            holder = (Holders.MessageHolder) convertView.getTag();
        }

        final MessageItem item = getItem(position);
        String time = createTimeString(item.getTime());
        String body = item.getBody();
        String name = item.getName();
        String n    = item.getName();
        MessageItem.Type type = item.getType();

        if (showtime) name = time + " " + name;

        String message = "";
        if (type == MessageItem.Type.status || type == MessageItem.Type.connectionstatus) message = name + " " + body;
        else if (type == MessageItem.Type.important) {
            if (showtime && !time.isEmpty()) message = time + " *** " + n + " " + body;
            else message = " *** " + n + " " + body;
        }
        else {
            if (body.length() > 4 && body.startsWith("/me")) {
                String meBody = body.substring(3);
                if (showtime && !time.isEmpty()) message = time + " * " + n + " " + meBody;
                else message = " * " + n + " " + meBody;
            } else {
                message = name + ": " + body;
            }
        }

        boolean out = false;

        SpannableStringBuilder ssb = new SpannableStringBuilder();
        ssb.append(message);
        if (showtime) ssb.setSpan(new AbsoluteSizeSpan(timeSize, true), 0, time.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        ssb.setSpan(new ForegroundColorSpan(Colors.PRIMARY_TEXT), 0, ssb.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        if (type == MessageItem.Type.separator) {
            ssb.clear();
            ssb.append("~ ~ ~ ~ ~");
            ssb.setSpan(new AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER), 0, ssb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            ssb.setSpan(new ForegroundColorSpan(Colors.HIGHLIGHT_TEXT), 0, ssb.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            holder.text.setText(ssb);
        }
        else if (type == MessageItem.Type.status || type == MessageItem.Type.connectionstatus || type == MessageItem.Type.important) {
            ssb.setSpan(new ForegroundColorSpan(Colors.STATUS_MESSAGE), 0, message.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        } else {
            if (showtime && time.length() > 2) {
                ssb.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), 0, time.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            if (nick != null && n.equals(nick)) {
                out = true;
                int idx = message.indexOf(n);
                ssb.setSpan(new ForegroundColorSpan(Colors.SECONDARY_TEXT), 0, message.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                ssb.setSpan(new ForegroundColorSpan(Colors.OUTBOX_MESSAGE), idx, idx + n.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                ssb.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), idx, idx + n.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else {
                boolean highlight = false;
                if (nick != null && item.contains(nick, prefs.getBoolean("HighlightFullWord", false))) highlight = true;
                else {
                    for (String light : highArray) {
                        if (!light.isEmpty() && item.contains(light, prefs.getBoolean("HighlightFullWord", false))) highlight = true;
                    }
                }
                if (highlight) {
                    ssb.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), 0, message.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    ssb.setSpan(new ForegroundColorSpan(Colors.HIGHLIGHT_TEXT), 0, message.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                }

                if (n.length() > 0) {
                    int idx = message.indexOf(n);
                    ssb.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), idx, idx + n.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }

            if (item.isEdited()) {
                int bsize = (int) ((fontSize) * activity.getResources().getDisplayMetrics().density) - 8;
                Bitmap b = BitmapFactory.decodeResource(activity.getResources(), R.drawable.ic_edited);
                b = Bitmap.createScaledBitmap(b, bsize, bsize, true);
                ssb.append(" ");
                ssb.setSpan(new ImageSpan(activity, b, ImageSpan.ALIGN_BASELINE), ssb.length()-1, ssb.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }

        if (type != MessageItem.Type.separator) {
            // Search highlight
            if (searchString.length() > 0) {
                if (ssb.toString().toLowerCase().contains(searchString.toLowerCase())) {
                    int from = 0;
                    int start = -1;
                    while ((start = ssb.toString().toLowerCase().indexOf(searchString.toLowerCase(), from)) != -1) {
                        from = start + searchString.length();
                        ssb.setSpan(new BackgroundColorSpan(Colors.SEARCH_BACKGROUND), start, start + searchString.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
                }
            }

            holder.check.setVisibility(viewMode == ChatAdapter.ViewMode.multi ? View.VISIBLE : View.GONE);
            holder.check.setChecked(item.isSelected());
            holder.check.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    item.select(b);
                    getItem(position).select(b);
                }
            });

            if (prefs.getBoolean("LoadPictures", true)) Pictures.loadPicture(activity, group, ssb, holder.text, Integer.parseInt(prefs.getString("MaxPictureSize", "1024")), true);

            if (prefs.getBoolean("ShowSmiles", true)) {
                int startPosition = message.length() - body.length();
                ssb = smiles.parseSmiles(holder.text, ssb, startPosition, account, group);
            }

            holder.text.setTextWithLinks(ssb, n, out, prefs);
        }
        return convertView;
    }

    private String createTimeString(String time) {
        try {
            Date d = new Date();
            java.text.DateFormat df = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
            String currentDate = df.format(d).substring(0,10);
            if (currentDate.equals(time.substring(0,10))) return "(" + time.substring(11) + ")";
            else return "(" + time + ")";
        } catch (Exception e) { return ""; }
    }

    public void copySelectedMessages() {
        String text = "";
        for(int i = 0; i < getCount(); i++) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
            boolean showtime = prefs.getBoolean("ShowTime", false);

            MessageItem message = getItem(i);
            if (message.isSelected() && message.getType() != MessageItem.Type.separator) {
                String body = message.getBody();
                String time = message.getTime();
                String name = message.getName();
                String t = "(" + time + ")";
                if (showtime) name = t + " " + name;
                text += "> " + name + ": " + body + "\n";
            }
        }

        ClipData.Item item = new ClipData.Item(text);

        String[] mimes = {"text/plain"};
        ClipData copyData = new ClipData(text, mimes, item);

        ClipboardManager clipboard = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(copyData);
        Toast.makeText(activity, R.string.MessagesCopied, Toast.LENGTH_SHORT).show();
    }
}
