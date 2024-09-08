package net.ustyugov.jtalk.dialog;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.*;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;
import com.jtalk2.R;
import net.ustyugov.jtalk.Constants;
import net.ustyugov.jtalk.MessageItem;
import net.ustyugov.jtalk.service.JTalkService;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.muc.MultiUserChat;

public class MessageDialogs {

    public static void messageMenu(final Activity activity, final MessageItem item, final boolean isMuc) {
        final JTalkService service = JTalkService.getInstance();
        final String account = item.getAccount();
        final String id = item.getId();
        final String body = item.getBody();
        final String jid = item.getJid();
        final String nick = item.getName();
        String myNick = null;
        try {
            if (isMuc && service.getConferencesHash(account).containsKey(StringUtils.parseBareAddress(jid))) {
                MultiUserChat muc = service.getConferencesHash(account).get(StringUtils.parseBareAddress(jid));
                myNick = muc.getNickname();
            }
        } catch (Exception ignored) {}

        CharSequence[] items;
        if ((!isMuc && nick.equals(activity.getResources().getString(R.string.Me))) || (isMuc && nick.equals(myNick))) {
            items = new CharSequence[4];
        } else {
            items = new CharSequence[3];
        }

        items[0] = activity.getString(R.string.CopyMessage);
        items[1] = activity.getString(R.string.QuoteMessage);
        items[2] = activity.getString(R.string.SelectText);
        if ((!isMuc && nick.equals(activity.getResources().getString(R.string.Me))) || (isMuc && nick.equals(myNick))) {
            items[3] = activity.getString(R.string.Edit);
        }


        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String text = "";
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
                boolean showtime = prefs.getBoolean("ShowTime", false);

                if (item.getType() != MessageItem.Type.separator) {
                    String body = item.getBody();
                    String time = item.getTime();
                    String name = item.getName();
                    String t = "(" + time + ")";
                    if (showtime) name = t + " " + name;
                    text += "> " + name + ": " + body + "\n\n";
                }

                switch (which) {
                    case 0:
                        String[] mimes = {"text/plain"};
                        ClipData.Item item = new ClipData.Item(text);
                        ClipData copyData = new ClipData(text, mimes, item);

                        ClipboardManager clipboard = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
                        clipboard.setPrimaryClip(copyData);
                        Toast.makeText(activity, R.string.MessagesCopied, Toast.LENGTH_SHORT).show();
                        break;
                    case 1:
                        Intent intent = new Intent(Constants.PASTE_TEXT);
                        intent.putExtra("text", text);
                        intent.putExtra("smile", true);
                        activity.sendBroadcast(intent);
                        break;
                    case 2:
                        LayoutInflater inflater = activity.getLayoutInflater();
                        View layout = inflater.inflate(R.layout.select_text_dialog, (ViewGroup) activity.findViewById(R.id.text));

                        final EditText textEdit = (EditText) layout.findViewById(R.id.text);
                        textEdit.setText(text);

                        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                        builder.setView(layout);
                        builder.setTitle(activity.getString(R.string.SelectText));
                        builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                        builder.create().show();
                        break;
                    case 3:
                        inflater = activity.getLayoutInflater();
                        layout = inflater.inflate(R.layout.select_text_dialog, (ViewGroup) activity.findViewById(R.id.text));

                        final EditText editText = (EditText) layout.findViewById(R.id.text);
                        editText.setText(body);

                        builder = new AlertDialog.Builder(activity);
                        builder.setView(layout);
                        builder.setTitle(activity.getString(R.string.Edit));
                        builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                String newmsg = editText.getText().toString();
                                service.editMessage(account, StringUtils.parseBareAddress(jid), id, newmsg, isMuc);
                                dialog.dismiss();
                            }
                        });
                        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                        builder.create().show();
                        break;
                }
            }
        });
        builder.create().show();
    }
}
