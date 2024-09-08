/*
 * Copyright (C) 2016, Igor Ustyugov <igor@ustyugov.net>
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

import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smackx.packet.Ping;

public class PingListener implements PacketListener {
	private XMPPConnection connection;

    public PingListener(XMPPConnection connection) {
    	this.connection = connection;
    }

	public void processPacket(Packet packet) {

        if (((Ping) packet).getType() == Ping.Type.GET) {
            Ping ping = new Ping();
            ping.setType(Ping.Type.RESULT);
            ping.setPacketID(packet.getPacketID());
            ping.setTo(packet.getFrom());

            try {
                connection.sendPacket(ping);
            } catch (Exception ignored) { }
        }
	}
}
