package net.ustyugov.jtalk.view;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.SharedPreferences;
import android.text.method.LinkMovementMethod;
import android.text.method.MovementMethod;
import android.text.style.ForegroundColorSpan;
import android.util.Patterns;
import com.amulyakhare.textdrawable.util.ColorGenerator;
import net.ustyugov.jtalk.Colors;
import net.ustyugov.jtalk.listener.TextLinkClickListener;

import android.content.Context;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ClickableSpan;
import android.text.style.StyleSpan;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

public class MyTextView  extends TextView {
	public enum Mode {juick, point}
	private TextLinkClickListener mListener;

	private Pattern linkPattern = Pattern.compile("https?://[a-z0-9\\-\\.]+[a-z]{2,}/?[^\\s\\n]*", Pattern.CASE_INSENSITIVE);
	private Pattern xmppPattern = Pattern.compile("xmpp\\:(?:(?:["
                    + Patterns.GOOD_IRI_CHAR
                    + "\\;\\/\\?\\@\\&\\=\\#\\~\\-\\.\\+\\!\\*\\'\\(\\)\\,\\_])"
                    + "|(?:\\%[a-fA-F0-9]{2}))+", Pattern.CASE_INSENSITIVE);
	private Pattern mdPattern = Pattern.compile("\\[((?!\\[).+?)\\][\\(|\\[](https?://[a-z0-9\\-\\.]+[a-z]{2,}/?[^\\s\\n]*)[\\)|\\]]", Pattern.CASE_INSENSITIVE);

	public MyTextView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

    public void setTextWithLinks(String string) {
        setTextWithLinks(new SpannableStringBuilder(string));
    }

	public void setTextWithLinks(SpannableStringBuilder ssb) {
        ssb = parseMdLinks(ssb);

        ArrayList<Hyperlink> linkList = new ArrayList<>();
        ArrayList<Hyperlink> xmppList = new ArrayList<>();
        getLinks(linkList, ssb, linkPattern);
        getLinks(xmppList, ssb, xmppPattern);

		for (Hyperlink link : linkList) {
			ssb.setSpan(link.span, link.start, link.end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            ssb.setSpan(new ForegroundColorSpan(Colors.LINK), link.start, link.end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		}

        for (Hyperlink link : xmppList) {
            ssb.setSpan(link.span, link.start, link.end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            ssb.setSpan(new ForegroundColorSpan(Colors.LINK), link.start, link.end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        }

		setText(ssb);
        setMovementMethod();
	}
	
	public void setTextWithLinks(SpannableStringBuilder ssb, String nick, boolean out, SharedPreferences prefs) {
        ssb = parseMdLinks(ssb);

		int start = ssb.toString().indexOf(nick);
		if (start >= 0 && nick.length() > 0) {
			int end = start + nick.length();
			
			Hyperlink spec = new Hyperlink();
			spec.textSpan = ssb.subSequence(start, end);
			spec.span = new InternalURLSpan(spec.textSpan.toString());
			spec.start = start;
			spec.end = end;
			ssb.setSpan(spec.span, spec.start, spec.end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

			int color = Colors.INBOX_MESSAGE;
			if (out) {
				color = Colors.OUTBOX_MESSAGE;
			} else {
				if (prefs.getBoolean("ColorNick", false)) {
					String letter = nick.substring(0, 1);
					ColorGenerator generator = ColorGenerator.MATERIAL;
					color = generator.getColor(letter);
				}
			}

			ForegroundColorSpan fcs = new ForegroundColorSpan(color);

			ssb.setSpan(fcs, spec.start, spec.end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		}
		
		ArrayList<Hyperlink> linkList = new ArrayList<>();
        ArrayList<Hyperlink> xmppList = new ArrayList<>();
		getLinks(linkList, ssb, linkPattern);
        getLinks(xmppList, ssb, xmppPattern);


		for (Hyperlink link : linkList) {
			ssb.setSpan(link.span, link.start, link.end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            ssb.setSpan(new ForegroundColorSpan(Colors.LINK), link.start, link.end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

		}

        for (Hyperlink link : xmppList) {
            ssb.setSpan(link.span, link.start, link.end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            ssb.setSpan(new ForegroundColorSpan(Colors.LINK), link.start, link.end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        }

		setText(ssb);
        setMovementMethod();
	}

    private void setMovementMethod() {
        MovementMethod m = getMovementMethod();
        if ((m == null) || !(m instanceof LinkMovementMethod)) {
            if (getLinksClickable()) {
                setMovementMethod(LinkMovementMethod.getInstance());
            }
        }
    }

	public void setOnTextLinkClickListener(TextLinkClickListener newListener) {
		mListener = newListener;
	}

	private void getLinks(ArrayList<Hyperlink> links, Spannable s, Pattern pattern) {
		Matcher m = pattern.matcher(s);

		while (m.find())
		{
			int start = m.start();
			int end = m.end();
        
			Hyperlink spec = new Hyperlink();

			spec.textSpan = s.subSequence(start, end);
			spec.span = new InternalURLSpan(spec.textSpan.toString().replaceAll("\\[/?img\\]", ""));
			spec.start = start;
			spec.end = end;

			links.add(spec);
		}
	}

    private SpannableStringBuilder parseMdLinks(SpannableStringBuilder s) {
        boolean done = false;
        while (!done) {
            Matcher m = mdPattern.matcher(s);
            if (m.find()) {
                int start = m.start();
                int end = m.end();
                CharSequence title = m.group(1);
                CharSequence link = m.group(2);

                s.replace(start, end, title);
                s.setSpan(new StyleSpan(android.graphics.Typeface.SANS_SERIF.getStyle()), start, start+title.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                s.setSpan(new InternalURLSpan(link.toString()), start, start+title.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                s.setSpan(new ForegroundColorSpan(Colors.LINK), start, start+title.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else {
                done = true;
            }
        }
        return s;
    }

	public class InternalURLSpan extends ClickableSpan {
		private String clickedSpan;
		private InternalURLSpan (String clickedString) {
			clickedSpan = clickedString;
		}

		@Override
		public void onClick(View textView) {
			mListener.onTextLinkClick(textView, clickedSpan);
		}
	}

	class Hyperlink {
		CharSequence textSpan;
		InternalURLSpan span;
		int start;
		int end;
	}
}
