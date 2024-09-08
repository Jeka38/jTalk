package net.ustyugov.jtalk;

import android.os.AsyncTask;
import net.ustyugov.jtalk.service.JTalkService;
import org.jivesoftware.smackx.filetransfer.FileTransfer;
import org.jivesoftware.smackx.filetransfer.FileTransferManager;
import org.jivesoftware.smackx.filetransfer.OutgoingFileTransfer;
import java.io.File;

public class SendFileTask extends AsyncTask<Void, Void, String> {
    private String account;
    private String jid;
    private String text;
    private String path;

    public SendFileTask(String account, String jid, String text, String path) {
        this.account = account;
        this.jid = jid;
        this.text = text;
        this.path = path;
    }

    @Override
    protected String doInBackground(Void... voids) {
        if (path == null) {
            Notify.fileProgress("File not found", FileTransfer.Status.error);
            return null;
        }
        File file = new File(path);
        if (file.exists()) {
            String name = file.getName();
            try {
                JTalkService service = JTalkService.getInstance();

                FileTransferManager ftm = service.getFileTransferManager(account);
                if (ftm == null) return "FileTransferManager not initialized";

                OutgoingFileTransfer out = ftm.createOutgoingFileTransfer(jid);
                out.sendFile(file, text);

                FileTransfer.Status lastStatus = FileTransfer.Status.initial;
                while (!out.isDone()) {
                    FileTransfer.Status status = out.getStatus();
                    if (status != lastStatus) {
                        Notify.fileProgress(name, status);
                        lastStatus = status;
                    }
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException ignored) { }
                }
                Notify.fileProgress(name, out.getStatus());
            } catch (Exception e) {
                Notify.fileProgress(name, FileTransfer.Status.error);
            }
        } else {
            Notify.fileProgress("File not found", FileTransfer.Status.error);
        }
        return null;
    }
}