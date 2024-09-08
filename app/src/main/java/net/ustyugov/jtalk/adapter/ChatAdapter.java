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

package net.ustyugov.jtalk.adapter;

import java.io.File;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;

import android.app.Activity;
import android.content.*;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.Layout;
import android.text.Spanned;
import android.text.style.*;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.*;
import com.amulyakhare.textdrawable.util.ColorGenerator;
import net.ustyugov.jtalk.*;
import net.ustyugov.jtalk.db.AttachmentsDbHelper;
import net.ustyugov.jtalk.db.JTalkProvider;
import net.ustyugov.jtalk.db.MessageDbHelper;
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

public class ChatAdapter extends ArrayAdapter<MessageItem> {
    public enum ViewMode { single, multi }

	private String searchString = "";

	private SharedPreferences prefs;
	private Activity activity;
	private Smiles smiles;
    private String account;
	private String jid;
	private boolean showtime;
    private ViewMode viewMode = ViewMode.single;

	public ChatAdapter(Activity activity, Smiles smiles) {
        super(activity, 0);
        this.activity = activity;
        this.smiles  = smiles;
        this.prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        this.showtime = prefs.getBoolean("ShowTime", true);
    }
	
	public void update(String account, String jid, String searchString, ViewMode viewMode) {
        if (this.viewMode == ViewMode.multi && viewMode == ViewMode.multi) return;

        this.viewMode = viewMode;
        this.account = account;
		this.jid = jid;
        this.searchString = searchString;
		clear();

        String showStatusMode = prefs.getString("StatusMessagesMode", "2");

        List<MessageItem> list = JTalkService.getInstance().getMessageList(account, jid);
        for (int i = 0; i < list.size(); i++) {
            MessageItem item = list.get(i);
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
                if (showStatusMode.equals("0")) {
                    if (type != MessageItem.Type.status && type != MessageItem.Type.connectionstatus) add(item);
                } else if (showStatusMode.equals("1")) add(item);
                else {
                    if (type != MessageItem.Type.status) add(item);
                }
            }
        }
	}
	
	public String getJid() { return this.jid; }

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
            LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.chat_item, null, false);

            holder.linear = (LinearLayout) convertView.findViewById(R.id.chat_item);
            holder.linear.setMinimumHeight(Integer.parseInt(prefs.getString("SmilesSize", "24")));
            holder.check = (CheckBox) convertView.findViewById(R.id.check);
            holder.text = (MyTextView) convertView.findViewById(R.id.chat1);
            holder.text.setOnTextLinkClickListener(new MyTextLinkClickListener(activity));
            holder.text.setTextSize(fontSize);
            holder.attachments = (LinearLayout) convertView.findViewById(R.id.attachments);

            convertView.setBackgroundColor(0X00000000);
            convertView.setTag(holder);
        } else {
            holder = (Holders.MessageHolder) convertView.getTag();
        }

        final MessageItem item = getItem(position);
        String subj = "";
        String body = item.getBody();
        String name = item.getName();
        MessageItem.Type type = item.getType();
        String nick = item.getName();
        boolean received = item.isReceived();
        String time = createTimeString(item.getTime());

        if (item.getSubject().length() > 0) subj = "\n" + activity.getString(R.string.Subject) + ": " + item.getSubject() + "\n";
        body = subj + body;
        
        String message;
        SpannableStringBuilder ssb = new SpannableStringBuilder();
        message = "";
        if (type == MessageItem.Type.separator) {
            ssb.append("~ ~ ~ ~ ~");
            ssb.setSpan(new AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER), 0, ssb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            ssb.setSpan(new ForegroundColorSpan(Colors.HIGHLIGHT_TEXT), 0, ssb.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            holder.text.setText(ssb);
        }
        else if (type == MessageItem.Type.status || type == MessageItem.Type.connectionstatus) {
        	if (showtime) message = time + "  " + body;
        	else message = body;
            ssb.append(message);
            ssb.setSpan(new ForegroundColorSpan(Colors.STATUS_MESSAGE), 0, ssb.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        } else {
        	int colorLength = name.length();
        	int boldLength = colorLength;
        	
        	if (showtime) {
                if (body.length() > 4 && body.startsWith("/me")) {
                    message = time + " * " + name + " " + body.substring(3);
                } else {
                    message = time + " " + name + ": " + body;
                }
        		colorLength = name.length() + time.length() + 3;
        		boldLength = name.length() + time.length() + subj.length() + 3;
        	}
        	else {
                if (body.length() > 4 && body.startsWith("/me")) {
                    message = " * " + name + " " + body.substring(3);
                    colorLength = name.length() + 3;
                    boldLength = colorLength + subj.length();
                } else {
                    message = name + ": " + body;
                }
            }
        	ssb.append(message);
        	ssb.setSpan(new ForegroundColorSpan(Colors.PRIMARY_TEXT), 0, ssb.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        	ssb.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), 0, boldLength, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            if (!nick.equals(activity.getResources().getString(R.string.Me))) {
                int color = Colors.INBOX_MESSAGE;
                if (prefs.getBoolean("ColorNick", false)) {
                    String letter = nick.substring(0, 1);
                    ColorGenerator generator = ColorGenerator.MATERIAL; // or use DEFAULT
                    color = generator.getColor(letter);
                }
                ssb.setSpan(new ForegroundColorSpan(color), 0, colorLength, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            else {
                ssb.setSpan(new ForegroundColorSpan(Colors.OUTBOX_MESSAGE), 0, colorLength, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                if (received && prefs.getBoolean("ShowReceivedIcon", true)) {
                    int bsize = (int) ((fontSize) * activity.getResources().getDisplayMetrics().density) - 8;
                    Bitmap b = BitmapFactory.decodeResource(activity.getResources(), R.drawable.ic_delivered);
                    b = Bitmap.createScaledBitmap(b, bsize, bsize, true);
                    ssb.insert(colorLength, " ");
                    ssb.setSpan(new ImageSpan(activity, b, ImageSpan.ALIGN_BASELINE), colorLength, colorLength+1, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
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
            if (showtime) ssb.setSpan(new AbsoluteSizeSpan(timeSize, true), 0, time.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

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

            holder.check.setVisibility(viewMode == ViewMode.multi ? View.VISIBLE : View.GONE);
            holder.check.setChecked(item.isSelected());
            holder.check.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    item.select(b);
                    getItem(position).select(b);
                }
            });

            if (prefs.getBoolean("PreloadLinks", false)) {
                Matcher m = Constants.LINK_PATTERN.matcher(body);
                int startOffset = 0;
                int endOffset = 0;
                while (m.find()) {
                    int start = m.start()+startOffset;
                    int end = m.end()+endOffset;

                    String url = body.substring(start, end);
                    String[] selectionArgs = {url};
                    Cursor cursor = activity.getContentResolver().query(JTalkProvider.ATTACHMENTS_URI, null, "url = ?", selectionArgs, MessageDbHelper._ID);
                    if (cursor != null && cursor.getCount() > 0) {
                        Log.e("ATTACH", url);
//                        while(cursor.moveToNext()) { // TODO: multiple attachments
                        cursor.moveToLast();
                            String attachType = cursor.getString(cursor.getColumnIndex(AttachmentsDbHelper.TYPE));
                            String title = cursor.getString(cursor.getColumnIndex(AttachmentsDbHelper.TITLE));
                            String description = cursor.getString(cursor.getColumnIndex(AttachmentsDbHelper.DESCRIPTION));
                            String length = cursor.getString(cursor.getColumnIndex(AttachmentsDbHelper.LENGTH));

                            String file = url.substring(url.lastIndexOf("/")+1);
                            try {
                                MessageDigest messageDigest = MessageDigest.getInstance("MD5");
                                messageDigest.reset();
                                messageDigest.update(url.getBytes(Charset.forName("UTF8")));
                                byte[] resultByte = messageDigest.digest();
                                BigInteger bigInt = new BigInteger(1,resultByte);
                                file = bigInt.toString(16);
                            } catch (NoSuchAlgorithmException ignored) {}

                            Bitmap image = null;

                            String fname = Constants.PATH + "Pictures/" + file;
                            File myFile = new File(fname);
                            if (myFile.exists()) {
                                DisplayMetrics metrics = new DisplayMetrics();
                                activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);

                                int maxWidth = metrics.widthPixels;
                                int maxHeight = metrics.heightPixels;

                                BitmapFactory.Options options = new BitmapFactory.Options();
                                options.inJustDecodeBounds = true;
                                BitmapFactory.decodeFile(fname, options);

                                options.inSampleSize = Pictures.calculateInSampleSize(options, maxWidth, maxHeight);
                                options.inJustDecodeBounds = false;
                                image = BitmapFactory.decodeFile(fname, options);

                                if (image != null) {
                                    int width = image.getWidth();
                                    if (width > maxWidth) {
                                        double k = (double) width / (double) maxWidth;
                                        int h = (int) (image.getHeight() / k);
                                        image = Bitmap.createScaledBitmap(image, maxWidth, h, true);
                                    }
                                }
                            }

                            switch(attachType) {
                                case "image":
                                    Log.e("ATTACH", "IMAGE");
                                    if (image != null) {
                                        holder.attachments.setVisibility(View.VISIBLE);
                                        LinearLayout imageLayout = (LinearLayout) holder.attachments.findViewById(R.id.attachment_image_layout);
                                        imageLayout.setVisibility(View.VISIBLE);

                                        ImageView imageView = (ImageView) imageLayout.findViewById(R.id.attachment_image_image);
                                        imageView.setImageBitmap(image);
                                        imageView.setVisibility(View.VISIBLE);
                                    } else {
                                        holder.attachments.setVisibility(View.GONE);
                                    }
                                    break;
                                case "html":
                                    Log.e("ATTACH", "HTML");
                                    if (!title.isEmpty()) {
                                        holder.attachments.setVisibility(View.VISIBLE);
                                        LinearLayout htmlLayout = (LinearLayout) holder.attachments.findViewById(R.id.attachment_html_layout);
                                        htmlLayout.setVisibility(View.VISIBLE);

                                        ImageView htmlImage = (ImageView) htmlLayout.findViewById(R.id.attachment_html_image);
                                        if (image != null) {
                                            htmlImage.setImageBitmap(image);
                                            htmlImage.setVisibility(View.VISIBLE);
                                        } else htmlImage.setVisibility(View.GONE);

                                        TextView htmlTitle = (TextView) htmlLayout.findViewById(R.id.attachment_html_title);
                                        htmlTitle.setText(title);

                                        TextView htmlDescription = (TextView) htmlLayout.findViewById(R.id.attachment_html_description);
                                        htmlDescription.setText(description);
                                    }
                                    break;
                                case "file":
                                default:

                            }
//                        }
                        cursor.close();
                    } else {
                        holder.attachments.setVisibility(View.GONE);
                    }
                }
            } else if (prefs.getBoolean("LoadPictures", true)) Pictures.loadPicture(activity, jid, ssb, holder.text, Integer.parseInt(prefs.getString("MaxPictureSize", "1024")), false);

            if (prefs.getBoolean("ShowSmiles", true)) {
                int startPosition = message.length() - body.length();
                ssb = smiles.parseSmiles(holder.text, ssb, startPosition, account, jid);
            }

            holder.text.setTextWithLinks(ssb);
        } else {
            holder.check.setVisibility(View.GONE);
        }

        return convertView;
    }
	
    private String createTimeString(String time) {
        Date d = new Date();
        java.text.DateFormat df = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
        String currentDate = df.format(d).substring(0,10);
        if (currentDate.equals(time.substring(0,10))) return "(" + time.substring(11) + ")";
        else return "(" + time + ")";
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
