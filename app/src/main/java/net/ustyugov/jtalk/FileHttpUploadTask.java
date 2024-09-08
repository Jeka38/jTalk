package net.ustyugov.jtalk;

import android.os.AsyncTask;
import android.util.Log;

import net.ustyugov.jtalk.service.JTalkService;
import org.jivesoftware.smack.PacketCollector;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.XMPPError;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.FormField;
import org.jivesoftware.smackx.ServiceDiscoveryManager;
import org.jivesoftware.smackx.filetransfer.FileTransfer;
import org.jivesoftware.smackx.httpupload.Request;
import org.jivesoftware.smackx.httpupload.Slot;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smackx.packet.DataForm;
import org.jivesoftware.smackx.packet.DiscoverInfo;
import org.jivesoftware.smackx.packet.DiscoverItems;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Iterator;

public class FileHttpUploadTask extends AsyncTask<Void, Void, String> {
    private final String XMLNS = "urn:xmpp:http:upload";
    private String service = null;
    private int maxSize = 0;

    private String jid;
    private String account;
    private String text;
    private MultiUserChat muc;
    private String path;

    public FileHttpUploadTask(String path, String account, String jid, String text, MultiUserChat muc) {
        this.path = path;
        this.account = account;
        this.jid = jid;
        this.muc = muc;
        this.text = text;
    }

    @Override
    protected String doInBackground(Void... params) {
        if (path == null) return null;

        File file = new File(path);
        if (file.exists() && file.isFile()) {
            XMPPConnection connection = JTalkService.getInstance().getConnection(account);
            if (!connection.isAuthenticated()) {
                Notify.uploadFileProgress(FileTransfer.Status.error, "Not connected to server");
                return null;
            }

            Notify.uploadFileProgress(FileTransfer.Status.in_progress, "Discovering for http-upload service");
            try {
                ServiceDiscoveryManager serviceDiscoveryManager = ServiceDiscoveryManager.getInstanceFor(connection);
                DiscoverItems items = serviceDiscoveryManager.discoverItems(StringUtils.parseServer(account));

                Iterator<DiscoverItems.Item> it = items.getItems();
                if (it.hasNext()) {
                    while (it.hasNext() && service == null) {
                        try {
                            DiscoverItems.Item item = it.next();
                            String jid = item.getEntityID();
                            DiscoverInfo info = serviceDiscoveryManager.discoverInfo(jid);

                            Iterator<DiscoverInfo.Feature> features = info.getFeatures();
                            while(features.hasNext()) {
                                DiscoverInfo.Feature feature = features.next();
                                if (feature.getVar().equals(XMLNS)) {
                                    service = jid;
                                    DataForm dataForm = (DataForm) info.getExtension("x", "jabber:x:data");
                                    if (dataForm != null) {
                                        Iterator<FormField> fields = dataForm.getFields();
                                        while(fields.hasNext()) {
                                            FormField field = fields.next();
                                            if (field.getVariable().equals("max-file-size")) {
                                                try {
                                                    maxSize = Integer.parseInt(field.getValue());
                                                } catch (NumberFormatException nfe) {
                                                    maxSize = 0;
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                        } catch (Exception e) {
//                            Notify.uploadFileProgress(FileTransfer.Status.error,  jid+":"+e.getMessage());
                            continue;
                        }
                    }
                }
            } catch (Exception e) {
                Notify.uploadFileProgress(FileTransfer.Status.error, "Error searching upload service:\n"+e.getMessage());
                return null;
            }

            if (service == null) {
                Notify.uploadFileProgress(FileTransfer.Status.error, "Server no support http upload.");
                return null;
            }

            if (maxSize > 0 && file.length() > maxSize) {
                Notify.uploadFileProgress(FileTransfer.Status.error, "File too large. Max size: " + maxSize);
                return null;
            }
            Notify.uploadFileProgress(FileTransfer.Status.in_progress, "Uploading file");

            String mimeType = URLConnection.guessContentTypeFromName(file.getName());

            Request request = new Request(file.getName(), String.valueOf(file.length()), mimeType);
            request.setTo(service);
            request.setType(IQ.Type.GET);

            PacketCollector collector = connection.createPacketCollector(new PacketFilter() {
                @Override
                public boolean accept(Packet packet) {
                    return (packet != null && packet.getFrom() != null && packet.getFrom().equals(service));
                }
            });
            connection.sendPacket(request);

            IQ iq = (IQ) collector.nextResult(10000);
            if (iq != null) {
                if (iq.getType() == IQ.Type.ERROR) {
                    XMPPError error = iq.getError();
                    Notify.uploadFileProgress(FileTransfer.Status.error, error.getMessage());
                } else {
                    try {
                        Slot slot = (Slot) iq;
                        String putUrl = slot.getPutUrl();
                        String getUrl = slot.getGetUrl();

                        HttpURLConnection conn = null;

                        try {
                            conn = (HttpURLConnection) new URL(putUrl).openConnection();
                            conn.setDoOutput(true);
                            conn.setDoInput(true);
                            conn.setUseCaches(false);
                            conn.setRequestMethod("PUT");

                            try {
                                DataOutputStream out = new DataOutputStream(conn.getOutputStream());
                                byte[] bytes = readFile(file);
                                out.write(bytes, 0, bytes.length);
                                out.flush();
                                out.close();
                            } catch (Exception e) {
                                Notify.uploadFileProgress(FileTransfer.Status.error, "Error sending file");
                                return null;
                            }

                            int responseCode = conn.getResponseCode();
                            if (responseCode != 200 && responseCode != 201) {
                                Notify.uploadFileProgress(FileTransfer.Status.error, "Error uploading file");
                                return null;
                            } else {
                                return getUrl;
                            }
                        } catch (Exception e) {
                            Notify.uploadFileProgress(FileTransfer.Status.error, e.getLocalizedMessage());
                            return null;
                        } finally {
                            if (conn != null) {
                                conn.disconnect();
                            }
                        }
                    } catch (ClassCastException cce) {
                        return null;
                    }
                }
            } else {
                Notify.uploadFileProgress(FileTransfer.Status.error, "No response from service");
            }

            return null;
        } else {
            Notify.uploadFileProgress(FileTransfer.Status.error, "File not found");
            return null;
        }
    }

    @Override
    protected void onPreExecute() {
        if (path == null) return;
        Notify.uploadFileProgress(FileTransfer.Status.in_progress, "Waiting for other background operations");
    }

    @Override
    protected void onPostExecute(String result) {
        if (result == null || result.isEmpty()) {
            return;
        }
        try {
            String message = result;
            if (text != null && !text.isEmpty()) message = text + "\n" + message;

            if (muc != null && muc.isJoined()) {
                try {
                    muc.sendMessage(message, result);
                } catch (Exception ignored) {}
            }
            else {
                JTalkService.getInstance().sendMessage(account, jid, message, null, result);
            }

            Notify.uploadCancel();
        } catch (Exception e) {
            Notify.uploadFileProgress(FileTransfer.Status.error, e.getLocalizedMessage());
        }
    }

    private static byte[] readFile(File file) throws IOException {
        // Open file
        RandomAccessFile f = new RandomAccessFile(file, "r");
        try {
            // Get and check length
            long longlength = f.length();
            int length = (int) longlength;
            if (length != longlength)
                throw new IOException("File size >= 2 GB");
            // Read file and return data
            byte[] data = new byte[length];
            f.readFully(data);
            return data;
        } finally {
            f.close();
        }
    }
}