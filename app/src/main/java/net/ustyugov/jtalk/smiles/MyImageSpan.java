package net.ustyugov.jtalk.smiles;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.text.style.DynamicDrawableSpan;
import net.ustyugov.jtalk.service.JTalkService;

public class MyImageSpan extends DynamicDrawableSpan {

    private SmileDrawable mDrawable;

    public MyImageSpan(SmileDrawable d, final String account, final String jid) {
        super();
        mDrawable = d;
        if (mDrawable.isAnimated()) {
            // Use handler for 'ticks' to proceed to next frame
            final Handler mHandler = new Handler();
            mHandler.post(new Runnable() {
                public void run() {
                    mDrawable.nextFrame();
                    if (JTalkService.getInstance() != null &&
                            (JTalkService.getInstance().getActiveChats(account).contains(jid) ||
                            JTalkService.getInstance().getJoinedConferences(account).containsKey(jid))) {

                        // Set next with a delay depending on the duration for this frame
                        mHandler.postDelayed(this, mDrawable.getFrameDuration());
                    }
                }
            });
        }
    }

    /*
     * Return current frame from animated drawable. Also acts as replacement for super.getCachedDrawable(),
     * since we can't cache the 'image' of an animated image.
     */
    @Override
    public Drawable getDrawable() {
        return mDrawable.getDrawable();
    }

    /*
     * Copy-paste of super.getSize(...) but use getDrawable() to get the image/frame to calculate the size,
     * in stead of the cached drawable.
     */
    @Override
    public int getSize(Paint paint, CharSequence text, int start, int end, Paint.FontMetricsInt fm) {
        Drawable d = getDrawable();
        Rect rect = d.getBounds();

        if (fm != null) {
            fm.ascent = -rect.bottom;
            fm.descent = 0;

            fm.top = fm.ascent;
            fm.bottom = 0;
        }

        return rect.right;
    }

    /*
     * Copy-paste of super.draw(...) but use getDrawable() to get the image/frame to draw, in stead of
     * the cached drawable.
     */
    @Override
    public void draw(Canvas canvas, CharSequence text, int start, int end, float x, int top, int y, int bottom, Paint paint) {
        Drawable b = getDrawable();
        canvas.save();

        int transY = bottom - b.getBounds().bottom;
        if (mVerticalAlignment == ALIGN_BASELINE) {
            transY -= paint.getFontMetricsInt().descent;
        }

        canvas.translate(x, transY);
        b.draw(canvas);
        canvas.restore();

    }

}
