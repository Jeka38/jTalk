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

package net.ustyugov.jtalk;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import com.jtalk2.R;
import net.ustyugov.jtalk.service.JTalkService;
import net.ustyugov.jtalk.utils.Base64;

import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.packet.BobExtension;
import org.jivesoftware.smackx.packet.DataForm;

public class MessageItem {
    public enum Type {message, status, connectionstatus, important, separator}

    private String account;
    private String jid;
	private String time = null;
	private String body = "";
	private String subj = "";
	private String name = null;
	private String id = null;
    private String baseId = null;
	private Type type = Type.message;
	private boolean edited = false;
	private boolean received = false;
	private boolean captcha = false;
	private DataForm form = null;
	private BobExtension bob = null;
    private boolean selected = false;
	
	public MessageItem(String account, String jid, String id) {
        this.account = account;
        this.jid = jid;
		Date d = new Date();
	    DateFormat df = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
	    this.time = df.format(d);
        this.edited = false;
		this.received = false;

        if (id != null && !id.isEmpty()) this.id = id; else id = Base64.encode((account + jid + this.time).getBytes());
	}

    public void setBaseId(String id) { this.baseId = id; }
    public String getBaseId() { return this.baseId; }
    public void setName(String name) { this.name = name; }
    public void setReceived(Boolean r) { this.received = r;}
    public void setEdited(Boolean e) { this.edited = e; }
    public String getTime() { return this.time; }
    public String getBody() { return this.body; }
    public String getSubject() { return this.subj; }
    public String getName() { return this.name; }
    public void setAccount(String account) { this.account = account; }
    public String getAccount() { return account; }
    public String getId() { return this.id; }
    public void setJid(String jid) { this.jid = jid; }
    public String getJid() { return jid; }
	public boolean isReceived() { return this.received; }
	public boolean isEdited() { return this.edited; }
	public boolean containsCaptcha() { return this.captcha; }
	public void setCaptcha(boolean c) { this.captcha = c; }
	public void setForm(DataForm form) { this.form = form; }
	public DataForm getForm() { return this.form; }
	public void setBob(BobExtension bob) { this.bob = bob; }
	public BobExtension getBob() { return this.bob; }
	public void setType(Type type) { if (type != null) this.type = type; }
	public Type getType() { return this.type; }
	public void setTime(String time) { this.time = time; }

    public void select(boolean isSelected) { this.selected = isSelected; }
    public boolean isSelected() {return selected;}

    public void setBody(String body) {
        this.body = body.replaceAll("&lt;", "<");
        this.body = body.replaceAll(";amp;", "&");
    }
	
	public void setSubject(String subject) {
		if (subject != null) {
            this.subj = subject.replaceAll("&lt;", "<");
            this.subj = subject.replaceAll(";amp;", "&");
        }
	}

    public boolean contains(String string, boolean correct) {
	    string = string.toLowerCase();
	    String body = this.body.toLowerCase();

        if (string.equals(body)) return true;
        if (correct) {
            int idx = body.indexOf(string);
            if (idx != -1) {
                if (string.length() < body.length()) {
                    boolean startOk = false;
                    boolean endOk = false;

                    if (idx > 0) {
                        String start = body.substring(idx-1, idx);
                        if (start.matches("\\W?")) startOk = true;
                    } else {
                        startOk = true;
                    }

                    if (body.length() > idx + string.length()) {
                        String end = body.substring(idx + string.length(), idx + string.length() + 1);
                        if (end.matches("\\W?")) endOk = true;
                    } else {
                        endOk = true;
                    }
                    if (startOk && endOk) return true;
                }
            }
            return false;
        } else {
            if (body.contains(string)) return true;
        }
        return false;
    }

	public String toXml() {
        StringBuilder sb = new StringBuilder();
		sb.append("<" + getType().name() + ">");
        sb.append("<name>"+getName()+"</name>");
        sb.append("<time>"+getTime()+"</time>");
		sb.append("<text>"+StringUtils.escapeForXML(getBody())+"</text>");
        sb.append("</" + getType().name() + ">\n");
		return sb.toString();
	}

    public String toJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"Type\": \"" + getType().name() + "\", ");
        sb.append("\"Time\": \"" + getTime() + "\", ");
        sb.append("\"Name\": \"" + getName() + "\", ");
        sb.append("\"Text\": \"" + getBody() + "\"");
        sb.append("}");
        return sb.toString();
    }
	
	public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getTime() + " ").append(getName());
        if (getType() == Type.message) sb.append(":\n"); else sb.append(" ");
        sb.append(getBody() + "\n\n");
        return sb.toString();
	}

    public String toHtml() {
        String body = StringUtils.escapeForXML(getBody());
        StringBuilder sb = new StringBuilder();
        sb.append("<p>");
        if (getType() == Type.message) {
            sb.append("<font color='");
            if (getName().equals(JTalkService.getInstance().getString(R.string.Me))) sb.append("red'>");
            else sb.append("blue'>");
            sb.append(getTime() + " " + getName() + ":</font>").append("<br>" + body);
        } else {
            sb.append("<font color='green'>").append(getTime() + " ").append(getName() + " ").append(body).append("</font>");
        }
        sb.append("</p>");
        return sb.toString();
    }
}
