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

package net.ustyugov.jtalk.listener;

import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import net.ustyugov.jtalk.Constants;
import net.ustyugov.jtalk.XmlObject;
import net.ustyugov.jtalk.service.JTalkService;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;

import java.util.ArrayList;
import java.util.List;

public class XmlListener {
    private List<XmlObject> list = new ArrayList<XmlObject>();
    public List<XmlObject> getList() { return list; }
    public void clear() { list.clear(); }

    public XmlListener(XMPPConnection connection) {
        final JTalkService service = JTalkService.getInstance();
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(service);

        connection.addPacketListener(new PacketListener() {
            public void processPacket(Packet packet) {
                if (prefs.getBoolean("XMLConsole", false)) {
                    XmlObject xml = new XmlObject();
                    xml.setOut(false);
                    xml.setXml(packet.toXML());
                    if (packet instanceof Message)
                        xml.setType(XmlObject.Type.message);
                    if (packet instanceof IQ)
                        xml.setType(XmlObject.Type.iq);
                    if (packet instanceof Presence)
                        xml.setType(XmlObject.Type.presence);
                    list.add(xml);

                    service.sendBroadcast(new Intent(Constants.XML));
                }
            }
        }, new PacketFilter() {
            public boolean accept(Packet packet) {
                return true;
            }
        });

        connection.addPacketSendingListener(new PacketListener() {
            public void processPacket(Packet packet) {
                if (prefs.getBoolean("XMLConsole", false)) {
                    XmlObject xml = new XmlObject();
                    xml.setOut(true);
                    xml.setXml(packet.toXML());
                    if (packet instanceof Message)
                        xml.setType(XmlObject.Type.message);
                    if (packet instanceof IQ)
                        xml.setType(XmlObject.Type.iq);
                    if (packet instanceof Presence)
                        xml.setType(XmlObject.Type.presence);
                    list.add(xml);

                    service.sendBroadcast(new Intent(Constants.XML));
                }
            }
        }, new PacketFilter() {
            public boolean accept(Packet packet) {
                return true;
            }
        });
    }
}
