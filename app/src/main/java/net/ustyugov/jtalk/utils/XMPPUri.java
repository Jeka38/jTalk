/**
 * Copyright (c) 2013, Redsolution LTD. All rights reserved.
 * 
 * This file is part of Xabber project; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License, Version 3.
 * 
 * Xabber is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License,
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */
package net.ustyugov.jtalk.utils;

import android.net.Uri;

/**
 * Helper class to parse xmpp uri.
 * 
 * http://xmpp.org/extensions/xep-0147.html
 * 
 * @author alexander.ivanov
 * 
 */
public class XMPPUri {
	private static final String XMPP_SCHEME = "xmpp";

	private String jid;
	private String queryType = "";
    private String body = "";
    private String name = "";

	public XMPPUri(Uri uri) throws IllegalArgumentException {
		if (uri == null)
			throw new IllegalArgumentException();
		if (!XMPP_SCHEME.equals(uri.getScheme()))
			throw new IllegalArgumentException();
		// Fix processing path without leading slash
		uri = Uri.parse(uri.getEncodedSchemeSpecificPart());
		if (uri.getPath() == null)
			throw new IllegalArgumentException();
		if (uri.getPath().startsWith("/"))
			jid = uri.getPath().substring(1);
		else
			jid = uri.getPath();
		String query = uri.getEncodedQuery();
		String action = null;
		if (query != null) {
			String parts[] = query.split(";");
			for (String part : parts)
				if (action == null) {
					if (part.contains("="))
						throw new IllegalArgumentException();
					action = part;
				} else {
					int index = part.indexOf("=");
					if (index == -1)
						continue;
					String key = part.substring(0, index);
                    String value = part.substring(index + 1);
                    if (key.equals("body")) body = value;
                    else if (key.equals("name")) name = value;
				}
		}
		if (action != null) queryType = action;
	}

	public String getJid() {
		return jid;
	}
    public String getBody() { return body; }
    public String getName() { return name; }
	public String getQueryType() { return queryType; }
}
