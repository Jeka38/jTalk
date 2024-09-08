package net.ustyugov.jtalk.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import net.ustyugov.jtalk.Constants;
import net.ustyugov.jtalk.service.JTalkService;

public class DeleteNotificationReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String jid = intent.getStringExtra("jid");
        String account = intent.getStringExtra("account");

        JTalkService service = JTalkService.getInstance();
        service.removeUnreadMesage(account, jid);
        service.removeHighlight(account, jid);
        service.removeMessagesCount(account, jid);
        service.sendBroadcast(new Intent(Constants.UPDATE));
    }

}