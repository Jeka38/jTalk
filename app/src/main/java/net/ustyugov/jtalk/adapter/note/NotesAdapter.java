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

package net.ustyugov.jtalk.adapter.note;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.preference.PreferenceManager;
import android.text.SpannableStringBuilder;
import android.text.method.LinkMovementMethod;
import android.text.method.MovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import com.jtalk2.R;
import net.ustyugov.jtalk.Colors;
import net.ustyugov.jtalk.Constants;
import net.ustyugov.jtalk.IconPicker;
import net.ustyugov.jtalk.listener.MyTextLinkClickListener;
import net.ustyugov.jtalk.service.JTalkService;
import net.ustyugov.jtalk.view.MyTextView;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.XMPPError;
import org.jivesoftware.smackx.note.Note;
import org.jivesoftware.smackx.note.NoteManager;

import java.util.Collection;

public class NotesAdapter extends ArrayAdapter<Note> {
    Context context;
    String account;

    static class ViewHolder {
        protected ImageView icon;
        protected TextView title;
        protected MyTextView text;
        protected TextView tag;
    }

    public NotesAdapter(Context context, String account) {
        super(context, R.id.name);
        this.context = context;
        this.account = account;
        try {
            NoteManager nm = NoteManager.getNoteManager(JTalkService.getInstance().getConnection(account));
            Collection<Note> collection = nm.getNotes();
            for (Note note : collection) {
                add(note);
            }
        } catch (XMPPException e) {
            XMPPError error = e.getXMPPError();
            if (error != null) {
                Intent intent = new Intent(Constants.ERROR);
                intent.putExtra("error", "[" + error.getCode() + "] " + error.getMessage());
                context.sendBroadcast(intent);
            }
        }
    }

    public String getAccount() { return account; }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        IconPicker ip = JTalkService.getInstance().getIconPicker();
        int fontSize;
        try {
            fontSize = Integer.parseInt(prefs.getString("RosterSize", context.getResources().getString(R.string.DefaultFontSize)));
        } catch (NumberFormatException e) {
            fontSize = Integer.parseInt(context.getResources().getString(R.string.DefaultFontSize));
        }

        ViewHolder holder;
        if (convertView == null) {
            LayoutInflater vi = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = vi.inflate(R.layout.note_item, null);

            holder = new ViewHolder();
            holder.icon = (ImageView) convertView.findViewById(R.id.icon);
            holder.icon.setVisibility(View.VISIBLE);
            holder.icon.setImageBitmap(ip.getMsgBitmap());

            holder.title = (TextView) convertView.findViewById(R.id.title);
            holder.title.setTextColor(Colors.PRIMARY_TEXT);
            holder.title.setTextSize(fontSize + 2);
            holder.title.setTypeface(Typeface.DEFAULT_BOLD);

            holder.text = (MyTextView) convertView.findViewById(R.id.text);
            holder.text.setTextColor(Colors.PRIMARY_TEXT);
            holder.text.setTextSize(fontSize);
            holder.text.setOnTextLinkClickListener(new MyTextLinkClickListener(context));

            holder.tag = (TextView) convertView.findViewById(R.id.tag);
            holder.tag.setTextColor(Colors.SECONDARY_TEXT);
            holder.tag.setTextSize(fontSize - 4);

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        Note note = getItem(position);
        holder.title.setText(note.getTittle());
        holder.text.setTextWithLinks(new SpannableStringBuilder(note.getText()));
        holder.tag.setText(note.getTag());

        return convertView;
    }
}
