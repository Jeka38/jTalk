/**
 *
 * Copyright 2014 Igor Ustyugov <igor@ustyugov.net>.
 *
 * All rights reserved. Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jivesoftware.smackx.note;

public class Note {
    private String tag;
    private String title;
    private String text;

    public Note(String title, String text, String tag) {
        this.title = title;
        this.text = text;
        this.tag = tag;
    }

    public String getTittle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public String getTag() { return tag; }
    public void setTag(String tag) { this.tag = tag; }
}
