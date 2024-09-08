package net.ustyugov.jtalk.listener;

import android.util.Log;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smackx.sm.*;

public class SMListener {
    private String id;
    private int inH = 0;
    private int outH = 0;
    private boolean enabled = false;
    private XMPPConnection connection;

    public SMListener(final XMPPConnection connection) {
        this.connection = connection;
    }

    public void addListeners() {
        connection.addPacketListener(new PacketListener() {
            public void processPacket(final Packet packet) {
                if (packet instanceof Message || packet instanceof IQ || packet instanceof Presence) {
                    if (enabled) {
                        inH++;
//                        A a = new A();
//                        a.setH(inH);
//                        connection.sendPacket(a);
                    }
                } else if (packet instanceof Enabled) {
                    Log.e("SM", "received enabled: " + packet.toXML());
                    id = ((Enabled) packet).getId();
                    inH = 0;
                    enabled = true;
//                    connection.sendPacket(new R());
                } else if (packet instanceof Resumed) {
                    Log.e("SM", "received resumed: " + packet.toXML());
                } else if (packet instanceof R) {
                    A a = new A();
                    a.setH(inH);
                    connection.sendPacket(a);
                } else if (packet instanceof A) {
                    outH = ((A) packet).getH();
                }
            }
        }, new PacketFilter() {
            public boolean accept(Packet packet) {
                return true;
            }
        });

        connection.addPacketSendingListener(new PacketListener() {
            public void processPacket(Packet packet) {
                if (packet instanceof Message || packet instanceof IQ || packet instanceof Presence) {
                    if (enabled) {
                        try {
                            outH++;
                            R r = new R();
                            connection.sendPacket(r);
                        } catch (Exception ignored) {}
                    }
                }
            }
        }, new PacketFilter() {
            public boolean accept(Packet packet) {
                return true;
            }
        });
    }

    public String getId() {
        return id;
    }

    public int getInH() {
        return inH;
    }

    public int getOutH() {
        return outH;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
