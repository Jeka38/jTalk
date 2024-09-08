package net.ustyugov.jtalk.smiles;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.preference.PreferenceManager;
import net.ustyugov.jtalk.service.JTalkService;

import java.io.File;
import java.io.FileInputStream;

class SmileDrawable extends AnimationDrawable {
    private int mCurrentIndex = 0;
    private UpdateListener mListener;
    private boolean animated = false;

    SmileDrawable(String path, UpdateListener listener) {
        try {
            JTalkService service = JTalkService.getInstance();
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(service);
            int size = 18;
            try {
                size = Integer.parseInt(prefs.getString("SmilesSize", size+""));
            } catch (NumberFormatException ignored) {	}
            size = (int) (size * service.getResources().getDisplayMetrics().density);

            String mimeType = new File(path).toURI().toURL().openConnection().getContentType();
            if (mimeType.equals("image/gif")) {
                animated = true;
                GifDecoder decoder = new GifDecoder();
                decoder.read(new FileInputStream(path));

                mListener = listener;

                // Iterate through the gif frames, add each as animation frame
                for (int i = 0; i < decoder.getFrameCount(); i++) {
                    Bitmap smile = decoder.getFrame(i);

                    double k;
                    int newSize;
                    int h = smile.getHeight();
                    int w = smile.getWidth();
                    Bitmap bitmap = Bitmap.createScaledBitmap(smile, size, size, true);

                    if (h < w) {
                        k = (double)h/(double)size;
                        newSize = (int) (w/k);
                        bitmap = Bitmap.createScaledBitmap(smile, newSize, size, true);
                    } else if (h > w) {
                        k = (double)w/(double)size;
                        newSize = (int) (h/k);
                        bitmap = Bitmap.createScaledBitmap(smile, size, newSize, true);
                    }

                    BitmapDrawable drawable = new BitmapDrawable(bitmap);
                    drawable.setBounds(0, 0, bitmap.getWidth(), bitmap.getHeight());
                    addFrame(drawable, decoder.getDelay(i));
                    if (i == 0) {
                        setBounds(0, 0, bitmap.getWidth(), bitmap.getHeight());
                    }
                }
            } else {
                animated = false;
                Bitmap smile = BitmapFactory.decodeFile(path);

                int h = smile.getHeight();
                int w = smile.getWidth();
                double k = (double)h/(double)size;
                int ws = (int) (w/k);

                Bitmap bitmap = Bitmap.createScaledBitmap(smile, ws, size, true);
                BitmapDrawable drawable = new BitmapDrawable(bitmap);
                addFrame(drawable, 0);
                setBounds(0, 0, bitmap.getWidth(), bitmap.getHeight());

            }
        } catch (Exception ignored) { }

    }

    boolean isAnimated() {
        return animated;
    }

    /**
     * Naive method to proceed to next frame. Also notifies listener.
     */
    void nextFrame() {
        mCurrentIndex = (mCurrentIndex + 1) % getNumberOfFrames();
        if (mListener != null) mListener.update();
    }

    /**
     * Return display duration for current frame
     */
    int getFrameDuration() {
        return getDuration(mCurrentIndex);
    }

    /**
     * Return drawable for current frame
     */
    public Drawable getDrawable() {
        return getFrame(mCurrentIndex);
    }

    /**
     * Interface to notify listener to update/redraw
     * Can't figure out how to invalidate the drawable (or span in which it sits) itself to force redraw
     */
    interface UpdateListener {
        void update();
    }

}