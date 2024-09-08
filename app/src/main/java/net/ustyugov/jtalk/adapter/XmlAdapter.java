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

package net.ustyugov.jtalk.adapter;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.*;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.jtalk2.R;
import net.ustyugov.jtalk.*;
import net.ustyugov.jtalk.listener.XmlListener;
import net.ustyugov.jtalk.service.JTalkService;
import net.ustyugov.jtalk.view.MyTextView;
import java.util.List;

public class XmlAdapter extends ArrayAdapter<XmlObject> {
	private String searchString = "";

	private SharedPreferences prefs;
	private Activity activity;
    private String account;

	public XmlAdapter(Activity activity, String account, String search) {
        super(activity, R.id.chat1);
        this.activity = activity;
        this.prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        this.account = account;
        this.searchString = search;
        update();
    }
	
	public void update() {
		clear();
        XmlListener listener = JTalkService.getInstance().getXmlListener(account);
        if (listener != null) {
            List<XmlObject> list = listener.getList();
            for (int i = 0; i < list.size(); i++) {
                XmlObject xml = list.get(i);
                if (searchString.length() > 0) {
                    if (xml.getXml().toLowerCase().contains(searchString.toLowerCase())) {
                        add(xml);
                    }
                } else {
                    add(xml);
                }
            }
        }
	}

	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {
		int fontSize = Integer.parseInt(activity.getResources().getString(R.string.DefaultFontSize));
		try {
			fontSize = Integer.parseInt(prefs.getString("FontSize", activity.getResources().getString(R.string.DefaultFontSize)));
		} catch (NumberFormatException ignored) {	}

        Holders.MessageHolder holder = new Holders.MessageHolder();
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.chat_item, null, false);

            holder.linear = (LinearLayout) convertView.findViewById(R.id.chat_item);
            holder.check = (CheckBox) convertView.findViewById(R.id.check);
            holder.text = (MyTextView) convertView.findViewById(R.id.chat1);
            holder.text.setTextSize(fontSize);

            convertView.setBackgroundColor(0X00000000);
            convertView.setTag(holder);
        } else {
            holder = (Holders.MessageHolder) convertView.getTag();
        }

        final XmlObject xml = getItem(position);

        SpannableStringBuilder ssb = new SpannableStringBuilder();
        ssb.append(xml.getXml());

        if (xml.isOut()) {
            ssb.setSpan(new ForegroundColorSpan(Colors.OUTBOX_MESSAGE), 0, ssb.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        } else {
            ssb.setSpan(new ForegroundColorSpan(Colors.INBOX_MESSAGE), 0, ssb.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

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

        holder.text.setText(ssb);

        return convertView;
    }
}
