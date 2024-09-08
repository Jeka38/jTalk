package net.ustyugov.jtalk.activity;

import android.app.Activity;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;
import com.jtalk2.R;
import net.ustyugov.jtalk.Colors;
import net.ustyugov.jtalk.MessageItem;
import net.ustyugov.jtalk.db.JTalkProvider;
import net.ustyugov.jtalk.db.MessageDbHelper;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class ExportActivity extends Activity implements View.OnClickListener {
    private String[] FORMATS = new String[] {"txt", "html", "xml", "json"};
    private String account;
    private String jid;
    private ProgressBar progress;
    private EditText pathEdit;
    private Spinner spinner;
    private Button exportButton;
    private CheckBox statusCheck;


    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        account = getIntent().getStringExtra("account");
        jid = getIntent().getStringExtra("jid");

        setTheme(Colors.isLight ? R.style.AppThemeLight : R.style.AppThemeDark);
        setTitle(R.string.Export);
        setContentView(R.layout.export);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setSubtitle(jid);

        LinearLayout linear = (LinearLayout) findViewById(R.id.linear);
        linear.setBackgroundColor(Colors.BACKGROUND);

        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, FORMATS);
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        progress = (ProgressBar) findViewById(R.id.progress);

        spinner = (Spinner) findViewById(R.id.formatSpinner);
        spinner.setAdapter(arrayAdapter);

        statusCheck = (CheckBox) findViewById(R.id.statusCheck);

        pathEdit = (EditText) findViewById(R.id.pathEdit);
        pathEdit.setText(Environment.getExternalStorageDirectory().getAbsolutePath() + "/jTalk/");

        exportButton = (Button) findViewById(R.id.exportButton);
        exportButton.setOnClickListener(this);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
        }
        return true;
    }

    @Override
    public void onClick(View view) {
        if (view == exportButton) {
            new Export().execute();
        }
    }

    private class Export extends AsyncTask<Void, Void, String> {
        @Override
        protected void onPreExecute() {
            progress.setVisibility(View.VISIBLE);
            exportButton.setEnabled(false);
        }

        @Override
        protected void onPostExecute(String result) {
            if (result == null) {
                Toast.makeText(ExportActivity.this, "Export " + jid + " is completed!", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(ExportActivity.this, "Error: " + result, Toast.LENGTH_LONG).show();
            }

            progress.setVisibility(View.GONE);
            exportButton.setEnabled(true);
        }

        @Override
        protected String doInBackground(Void... voids) {
            StringBuffer buffer = new StringBuffer();
            String format = (String) spinner.getSelectedItem();
            if ("html".equals(format)) buffer.append("<html>\n<head><meta charset='utf-8'/></head>\n<body>\n");

            String selection = "jid = '" + jid + "'";
            if (!statusCheck.isChecked()) selection += " AND type = 'message'";
            Cursor cursor = getContentResolver().query(JTalkProvider.CONTENT_URI, null, selection, null, MessageDbHelper._ID);
            if (cursor != null && cursor.getCount() > 0) {
                cursor.moveToFirst();
                do {
                    String nick = cursor.getString(cursor.getColumnIndex(MessageDbHelper.NICK));
                    String type = cursor.getString(cursor.getColumnIndex(MessageDbHelper.TYPE));
                    String stamp = cursor.getString(cursor.getColumnIndex(MessageDbHelper.STAMP));
                    String body = cursor.getString(cursor.getColumnIndex(MessageDbHelper.BODY));

                    MessageItem item = new MessageItem(account, jid, null);
                    item.setName(nick);
                    item.setType(MessageItem.Type.valueOf(type));
                    item.setTime(stamp);
                    item.setBody(body);

                    if ("txt".equals(format)) {
                        buffer.append(item.toString());
                    } else if ("xml".equals(format)) {
                        buffer.append(item.toXml());
                    } else if ("html".equals(format)) {
                        buffer.append(item.toHtml());
                    } else if ("json".equals(format)) {
                        buffer.append(item.toJson());
                    }
                } while (cursor.moveToNext());
                cursor.close();
            }

            if ("html".equals(format)) buffer.append("\n</body>\n</html>");

            String path = pathEdit.getText().toString() + "/";
            String filename =  path + jid.replaceAll("/", "_") + "."+format;
            File file = new File(path);
            file.mkdirs();
            try {
                FileWriter fw = new FileWriter(filename);
                fw.write(buffer.toString());
                fw.close();
            } catch (IOException ioe) {
                return ioe.getLocalizedMessage();
            }

            return null;
        }
    }
}
