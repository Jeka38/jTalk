package net.ustyugov.jtalk.listener;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import net.ustyugov.jtalk.Constants;

public class MyTextLinkClickListener implements TextLinkClickListener {
    private Context context;

    public MyTextLinkClickListener(Context context) {
        this.context = context;
    }

    @Override
    public void onTextLinkClick(View textView, String clickedString) {
        if (clickedString.length() > 1) {
            Uri uri = Uri.parse(clickedString);
            if ((uri != null && uri.getScheme() != null)) {
                String scheme = uri.getScheme().toLowerCase();
                if (scheme.contains("http") || scheme.contains("xmpp")) {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(uri);
                    context.startActivity(intent);
                }
            } else {
                Intent intent = new Intent(Constants.PASTE_TEXT);
                intent.putExtra("text", clickedString);
                context.sendBroadcast(intent);
            }
        }
    }
}
