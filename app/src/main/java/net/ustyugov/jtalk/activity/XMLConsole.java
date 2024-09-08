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

package net.ustyugov.jtalk.activity;

import android.app.Activity;
import android.content.*;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.viewpager.widget.ViewPager;
import com.jtalk2.R;
import com.viewpagerindicator.TitlePageIndicator;
import net.ustyugov.jtalk.Colors;
import net.ustyugov.jtalk.Constants;
import net.ustyugov.jtalk.adapter.MainPageAdapter;
import net.ustyugov.jtalk.adapter.XmlAdapter;
import net.ustyugov.jtalk.db.AccountDbHelper;
import net.ustyugov.jtalk.db.JTalkProvider;
import net.ustyugov.jtalk.listener.XmlListener;
import net.ustyugov.jtalk.service.JTalkService;

import java.util.ArrayList;

public class XMLConsole extends Activity {
	private ViewPager mPager;
	private ArrayList<View> mPages = new ArrayList<View>();
	private BroadcastReceiver updateReceiver;
	private JTalkService service;
    private String searchString = "";

	@Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        service = JTalkService.getInstance();
        setTheme(Colors.isLight ? R.style.AppThemeLight : R.style.AppThemeDark);
		setTitle("XML Console");
        getActionBar().setDisplayHomeAsUpEnabled(true);
		setContentView(R.layout.paged_activity);
        
       	LinearLayout linear = (LinearLayout) findViewById(R.id.linear);
       	linear.setBackgroundColor(Colors.BACKGROUND);
        
        LayoutInflater inflater = LayoutInflater.from(this);
        MainPageAdapter adapter = new MainPageAdapter(mPages);
        
        Cursor cursor = service.getContentResolver().query(JTalkProvider.ACCOUNT_URI, null, AccountDbHelper.ENABLED + " = '1'", null, null);
		if (cursor != null && cursor.getCount() > 0) {
			cursor.moveToFirst();
			do {
				final String account = cursor.getString(cursor.getColumnIndex(AccountDbHelper.JID)).trim();
				
				View page = inflater.inflate(R.layout.list_activity, null);
				page.setTag(account);
				mPages.add(page);
				
		        ListView list = (ListView) page.findViewById(R.id.list);
		        list.setDividerHeight(0);
		        list.setCacheColorHint(0x00000000);
			} while (cursor.moveToNext());
			cursor.close();
		}
        
	    mPager = (ViewPager) findViewById(R.id.pager);
	    mPager.setAdapter(adapter);
	    mPager.setCurrentItem(0);
	        
	    TitlePageIndicator mTitleIndicator = (TitlePageIndicator) findViewById(R.id.indicator);
	    mTitleIndicator.setTextColor(0xFF555555);
	    mTitleIndicator.setViewPager(mPager);
    }
	
	@Override
	public void onResume() {
		super.onResume();
		service.resetTimer();
	    
	    updateReceiver = new BroadcastReceiver() {
	        @Override
	        public void onReceive(Context context, Intent intent) {
                updateList();
	        }
	    };
	    registerReceiver(updateReceiver, new IntentFilter(Constants.XML));
	    
	    updateList();
	}
	
	@Override
	public void onPause() {
		super.onPause();
		unregisterReceiver(updateReceiver);
	}
    
	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.xml, menu);

        MenuItem.OnActionExpandListener listener = new MenuItem.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                searchString = "";
                updateList();
                return true;
            }

            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                return true;
            }
        };

        SearchView searchView = new SearchView(this);
        searchView.setQueryHint(getString(android.R.string.search_go));
        searchView.setSubmitButtonEnabled(true);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
            @Override
            public boolean onQueryTextSubmit(String query) {
                searchString = query;
                updateList();
                return true;
            }
        });

        MenuItem item = menu.findItem(R.id.search);
        item.setActionView(searchView);
        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);
        item.setOnActionExpandListener(listener);
        return super.onCreateOptionsMenu(menu);
    }
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
        String acc = (String) mPages.get(mPager.getCurrentItem()).getTag();
		switch (item.getItemId()) {
			case android.R.id.home:
				finish();
				break;
            case R.id.search:
                if (!item.isActionViewExpanded()) {
                    item.expandActionView();
                }
                break;
  	    	case R.id.clear:
                XmlListener listener = JTalkService.getInstance().getXmlListener(acc);
  	    		if (listener != null) listener.clear();
                updateList();
  	    		break;
  	    	default:
  	    		return false;
		}
		return true;
	}
	
	private void updateList() {
		for (View view : mPages) {
			ProgressBar progress = (ProgressBar) view.findViewById(R.id.progress);
	        ListView list = (ListView) view.findViewById(R.id.list);
	        String account = (String) view.getTag();
			new Init(account, list, progress).execute();
		}
	}
	
	private class Init extends AsyncTask<String, Void, Void> {
		String account;
		XmlAdapter adapter;
		ListView list;
		ProgressBar progress;
		
		public Init(String account, ListView list, ProgressBar progress) {
			this.account = account;
			this.list = list;
			this.progress = progress;
		}
		
		@Override
		protected Void doInBackground(String... params) {
			adapter = new XmlAdapter(XMLConsole.this, account, searchString);
			return null;
		}
		
		@Override
		protected void onPostExecute(Void v) {
			super.onPostExecute(v);
		    list.setAdapter(adapter);
		    list.setVisibility(View.VISIBLE);
		    progress.setVisibility(View.GONE);
            list.setSelection(list.getCount());
		}
		
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			list.setVisibility(View.GONE);
			progress.setVisibility(View.VISIBLE);
		}
	}
}