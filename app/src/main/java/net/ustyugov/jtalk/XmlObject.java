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

package net.ustyugov.jtalk;

public class XmlObject {
    public enum Type {none, iq, presence, message}
    private Type type = Type.none;
    private String xml = "";
    private boolean out = false;

    public void setXml(String xml) { this.xml = xml; }
    public String getXml() { return xml; }

    public void setType(Type type) { this.type = type; }
    public Type getType() { return type; }

    public void setOut(boolean out) { this.out = out; }
    public boolean isOut() { return out; }
}
