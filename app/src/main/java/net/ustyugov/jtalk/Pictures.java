package net.ustyugov.jtalk;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import androidx.core.content.FileProvider;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ClickableSpan;
import android.text.style.ImageSpan;
import android.util.DisplayMetrics;
import android.view.View;
import net.ustyugov.jtalk.view.MyTextView;

import java.io.*;
import java.math.BigInteger;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Pictures {
    private static Pattern linkPattern = Pattern.compile("(\\[img\\])?https?://[a-z0-9\\-\\.]+[a-z]{2,}(:[0-9]{1,5})?/.+\\.(png|jpg|jpeg|gif|webp)[^\\s\\n]*", Pattern.CASE_INSENSITIVE);

    public static void loadPicture(final Activity activity, final String jid, final SpannableStringBuilder ssb, final MyTextView tv, final int maxSize, final boolean isMuc) {
        Matcher m = linkPattern.matcher(ssb);
        int startOffset = 0;
        int endOffset = 0;
        while (m.find()) {
            int start = m.start()+startOffset;
            int end = m.end()+endOffset;

            final String url = ssb.subSequence(start, end).toString().replaceAll("\\[/?img\\]", "");
            String file = url.substring(url.lastIndexOf("/")+1, url.length());
            try {
                MessageDigest messageDigest = MessageDigest.getInstance("MD5");
                messageDigest.reset();
                messageDigest.update(url.getBytes(Charset.forName("UTF8")));
                byte[] resultByte = messageDigest.digest();
                BigInteger bigInt = new BigInteger(1,resultByte);
                file = bigInt.toString(16);
            } catch (NoSuchAlgorithmException ignored) {}

            final String fname = Constants.PATH + "Pictures/" + file;
            final File myFile = new File(fname);
            if (!myFile.exists()) {
                new Thread() {
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(2000);
                            File folder = new File(Constants.PATH + "Pictures/");
                            if (!folder.exists()) folder.mkdirs();

                            URL obj = new URL(url);
                            URLConnection conn = obj.openConnection();
                            int length = conn.getContentLength();
                            if ((length / 1024) < maxSize) {
                                BufferedInputStream in = new BufferedInputStream(new URL(url).openStream());
                                FileOutputStream fout = new FileOutputStream(fname);

                                final byte data[] = new byte[1024];
                                int count;
                                while ((count = in.read(data, 0, 1024)) != -1) {
                                    fout.write(data, 0, count);
                                }
                                in.close();
                                fout.close();

                                activity.sendBroadcast(new Intent(Constants.NEW_MESSAGE).putExtra("jid", jid));
                            }
                        } catch (Exception ignored) { }
                    }
                }.start();
            } else {
                Bitmap bitmap;
                DisplayMetrics metrics = new DisplayMetrics();
                activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);

                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);

                int sidebar = 48;
                if (isMuc) {
                    if (!prefs.getBoolean("ShowSidebar", true) || !prefs.getBoolean("OldUserList", false)) {
                        sidebar = 48;
                    } else {
                        sidebar = prefs.getInt("SideBarSize", 0);
                        if (sidebar == 0) {
                            sidebar = (int)(160 * activity.getResources().getDisplayMetrics().density) + 48;
                        }
                    }
                }

                int maxWidth = metrics.widthPixels - sidebar;
                int maxHeight = metrics.heightPixels;

                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeFile(fname, options);

                options.inSampleSize = calculateInSampleSize(options, maxWidth, maxHeight);
                options.inJustDecodeBounds = false;
                bitmap = BitmapFactory.decodeFile(fname, options);

                if (bitmap != null) {
                    int width = bitmap.getWidth();
                    if (width > maxWidth)  {
                        double k = (double)width/(double)maxWidth;
                        int h = (int) (bitmap.getHeight()/k);
                        bitmap = Bitmap.createScaledBitmap(bitmap, maxWidth, h, true);
                    }

                    ssb.setSpan(new ClickableSpan() {
                        @Override
                        public void onClick(View view) {
                            Intent intent = new Intent(Intent.ACTION_VIEW);
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                Uri contentUri = FileProvider.getUriForFile(activity, "com.jtalk2.fileProvider", myFile);
                                intent.setDataAndType(contentUri, "image/*");
                                intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            } else {
                                intent.setDataAndType(Uri.fromFile(myFile), "image/*");
                            }
                            activity.startActivity(intent);
                        }
                    }, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    ssb.setSpan(new ImageSpan(activity, bitmap, ImageSpan.ALIGN_BASELINE), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    ssb.insert(start, "\n");
                    ssb.insert(end+1, "\n");
                    tv.setText(ssb);

                    startOffset++;
                    endOffset = endOffset + 2;
                }
            }
        }
    }

    public static int calculateInSampleSize(BitmapFactory.Options options, int maxWidth, int maxHeight) {
        final int width = options.outWidth;
        final int height = options.outHeight;
        int inSampleSize = 1;

        if (width > maxWidth || height > maxHeight) {
            final int halfWidth = width / 2;
            final int halfHeight = height / 2;

            while (((halfWidth / inSampleSize) > maxWidth) || ((halfHeight / inSampleSize) > maxHeight)) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }
}
