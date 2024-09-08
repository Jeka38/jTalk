package net.ustyugov.jtalk;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.preference.PreferenceManager;
import android.util.Log;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.FileReader;

public class Colors {
    public static boolean isLight = true;
    public static String currentTheme = "Light";
    public static int BACKGROUND = 0xFFFFFFFF;
    public static int ACCOUNT_BACKGROUND = 0xEE999999;
    public static int GROUP_BACKGROUND = 0xEECCCCCC;
    public static int ENTRY_BACKGROUND = 0xEEEFEFEF;
    public static int ACCOUNT_FOREGROUND = 0xFF000000;
    public static int GROUP_FOREGROUND = 0xFF000000;
    public static int ENTRY_FOREGROUND = 0xFF000000;
    public static int SEARCH_FOREGROUND = 0xFF000000;
    public static int PRIMARY_TEXT = 0xFF000000;
    public static int SECONDARY_TEXT = 0xFF666666;
    public static int HIGHLIGHT_TEXT = 0xFFCC0000;
    public static int SEARCH_BACKGROUND = 0xFFAAAA66;
    public static int LINK = 0xFF2323AA;
    public static int INBOX_MESSAGE = 0xFF2323AA;
    public static int OUTBOX_MESSAGE = 0xFFAA2323;
    public static int STATUS_MESSAGE = 0xFF239923;
    public static int AFFILIATION_NONE = 0xFF777777;
    public static int AFFILIATION_MEMBER = 0xFF000000;
    public static int AFFILIATION_ADMIN = 0xFF000000;
    public static int AFFILIATION_OWNER = 0xFFDD0000;

    public static int ACTIONBAR_ONLINE = Color.parseColor("#151235");
    public static int STATUSBAR_ONLINE = Color.parseColor("#151235");

    public static int ACTIONBAR_CHAT = Color.parseColor("#151235");
    public static int STATUSBAR_CHAT = Color.parseColor("#151235");

    public static int ACTIONBAR_AWAY = Color.parseColor("#151235");
    public static int STATUSBAR_AWAY = Color.parseColor("#151235");

    public static int ACTIONBAR_XA = Color.parseColor("#151235");
    public static int STATUSBAR_XA = Color.parseColor("#151235");

    public static int ACTIONBAR_DND = Color.parseColor("#151235");
    public static int STATUSBAR_DND = Color.parseColor("#151235");

    public static int ACTIONBAR_OFFLINE = Color.parseColor("#212121");
    public static int STATUSBAR_OFFLINE = Color.parseColor("#000000");

    public static int DRAWER_ENTRY_FOREGROUND = 0xFFFFFFFF;
    public static int DRAWER_SECONDARY_TEXT = 0xFF999999;
    public static int DRAWER_GROUP_BACKGROUND = 0x77404040;
    public static int DRAWER_GROUP_FOREGROUND = 0xFFFFFFFF;
    public static int DRAWER_STATUS_TEXT = 0xFF666666;

    public static void updateColors(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String theme = prefs.getString("ColorTheme", "Light");
        if (theme.equals(currentTheme)) return;
        else if (theme.equals("Light")) {
            isLight = true;
            BACKGROUND = 0xFFFFFFFF;
            ACCOUNT_BACKGROUND = 0xEE999999;
            GROUP_BACKGROUND = 0xEECCCCCC;
            ENTRY_BACKGROUND = 0xEEEFEFEF;
            ACCOUNT_FOREGROUND = 0xFF000000;
            GROUP_FOREGROUND = 0xFF000000;
            ENTRY_FOREGROUND = 0xFF000000;
            SEARCH_FOREGROUND = 0xFF000000;
            PRIMARY_TEXT = 0xFF000000;
            SECONDARY_TEXT = 0xFF666666;
            HIGHLIGHT_TEXT = 0xFFCC0000;
            SEARCH_BACKGROUND = 0xFFAAAA66;
            LINK = 0xFF2323AA;
            INBOX_MESSAGE = 0xFF2323AA;
            OUTBOX_MESSAGE = 0xFFAA2323;
            STATUS_MESSAGE = 0xFF239923;
            AFFILIATION_NONE = 0xFF777777;
            AFFILIATION_MEMBER = 0xFF000000;
            AFFILIATION_ADMIN = 0xFF000000;
            AFFILIATION_OWNER = 0xFFDD0000;
            currentTheme = "Light";
        } else if (theme.equals("Dark")) {
            isLight = false;
            BACKGROUND = 0xFF000000;
            ACCOUNT_BACKGROUND = 0xFF333333;
            GROUP_BACKGROUND = 0xFF333333;
            ENTRY_BACKGROUND = 0xFF333333;
            ACCOUNT_FOREGROUND = 0xFFFFFFFF;
            GROUP_FOREGROUND = 0xFFF4E431;
            ENTRY_FOREGROUND = 0xFFFFFFFF;
            SEARCH_FOREGROUND = 0xFFFFFFFF;
            PRIMARY_TEXT = 0xFFFFFFFF;
            SECONDARY_TEXT = 0xFF7EB8FF; //мой текст в конфе
            HIGHLIGHT_TEXT = 0xFFFFDD00;
            SEARCH_BACKGROUND = 0xFFAAAA66;
            LINK = 0xFF5180b7;
            INBOX_MESSAGE = 0xFFFF6767;
            OUTBOX_MESSAGE = 0xFF7EB8FF;
            STATUS_MESSAGE = 0xFF99FFA0;
            AFFILIATION_NONE = 0xFF7171CA;
            AFFILIATION_MEMBER = 0xFFFFFFFF;
            AFFILIATION_ADMIN = 0xFFDD0000;
            AFFILIATION_OWNER = 0xFFEEFF00;
            currentTheme = "Dark";
        } else {
            try {
                XmlPullParser parser = XmlPullParserFactory.newInstance().newPullParser();
                parser.setInput(new FileReader(Constants.PATH_COLORS + theme));

                boolean end = false;
                while(!end) {
                    int eventType = parser.next();
                    if (eventType == XmlPullParser.START_TAG) {
                        if (parser.getName().equals("theme")) {
                            isLight = Boolean.valueOf(parser.getAttributeValue("", "isLight"));
                            do {
                                eventType = parser.next();
                                if (eventType == XmlPullParser.START_TAG && parser.getName().equals("color")) {
                                    String name = parser.getAttributeValue("", "name");
                                    int parserDepth = parser.getDepth();
                                    while (!(parser.next() == XmlPullParser.END_TAG && parser.getDepth() == parserDepth)) {
                                        String text = parser.getText();
                                        Long i = Long.parseLong(text, 16);
                                        int color = i.intValue();
                                        if (name.equals("ACCOUNT_BACKGROUND")) {
                                            ACCOUNT_BACKGROUND = color;
                                        } else if (name.equals("BACKGROUND")) {
                                            BACKGROUND = color;
                                        } else if (name.equals("GROUP_BACKGROUND")) {
                                            GROUP_BACKGROUND = color;
                                        } else if (name.equals("ENTRY_BACKGROUND")) {
                                            ENTRY_BACKGROUND = color;
                                        } else if (name.equals("SEARCH_BACKGROUND")) {
                                            SEARCH_BACKGROUND = color;
                                        } else if (name.equals("ACCOUNT_FOREGROUND")) {
                                            ACCOUNT_FOREGROUND = color;
                                        } else if (name.equals("GROUP_FOREGROUND")) {
                                            GROUP_FOREGROUND = color;
                                        } else if (name.equals("ENTRY_FOREGROUND")) {
                                            ENTRY_FOREGROUND = color;
                                        } else if (name.equals("SEARCH_FOREGROUND")) {
                                            SEARCH_FOREGROUND = color;
                                        } else if (name.equals("PRIMARY_TEXT")) {
                                            PRIMARY_TEXT = color;
                                        } else if (name.equals("SECONDARY_TEXT")) {
                                            SECONDARY_TEXT = color;
                                        } else if (name.equals("LINK")) {
                                            LINK = color;
                                        } else if (name.equals("HIGHLIGHT_TEXT")) {
                                            HIGHLIGHT_TEXT = color;
                                        } else if (name.equals("INBOX_MESSAGE")) {
                                            INBOX_MESSAGE = color;
                                        } else if (name.equals("OUTBOX_MESSAGE")) {
                                            OUTBOX_MESSAGE = color;
                                        } else if (name.equals("STATUS_MESSAGE")) {
                                            STATUS_MESSAGE = color;
                                        } else if (name.equals("AFFILIATION_NONE")) {
                                            AFFILIATION_NONE = color;
                                        } else if (name.equals("AFFILIATION_MEMBER")) {
                                            AFFILIATION_MEMBER = color;
                                        } else if (name.equals("AFFILIATION_ADMIN")) {
                                            AFFILIATION_ADMIN = color;
                                        } else if (name.equals("AFFILIATION_OWNER")) {
                                            AFFILIATION_OWNER = color;
                                        }
                                    }
                                }
                            }
                            while (eventType != XmlPullParser.END_TAG);
                        }
                    } else if (eventType == XmlPullParser.END_DOCUMENT) {
                        end = true;
                    }
                }
            } catch (Exception e) {
                Log.e("COLORS", e.getLocalizedMessage());
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString("ColorTheme", "Light");
                editor.apply();
                updateColors(context);
            }
        }
    }
}
