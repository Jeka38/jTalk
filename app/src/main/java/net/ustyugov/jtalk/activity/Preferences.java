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

package net.ustyugov.jtalk.activity;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.database.Cursor;
import android.preference.*;
import android.view.MenuItem;
import net.ustyugov.jtalk.Constants;
import net.ustyugov.jtalk.IconPicker;
import net.ustyugov.jtalk.db.AccountDbHelper;
import net.ustyugov.jtalk.db.JTalkProvider;
import net.ustyugov.jtalk.service.JTalkService;

import com.jtalk2.R;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageInfo;
import android.content.res.Resources;
import android.os.Bundle;
import org.jivesoftware.smack.Roster;

public class Preferences extends PreferenceActivity implements OnSharedPreferenceChangeListener {
	private ListPreference smilespack;

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
        getActionBar().setDisplayHomeAsUpEnabled(true);
		CharSequence[] smiles = new CharSequence[1];
        CharSequence[] colors;
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		addPreferencesFromResource(R.xml.preferences);  // TODO!
		
		File file = new File(Constants.PATH_SMILES);
		file.mkdirs();
		File[] files = file.listFiles();
		if (files != null) {
			smiles = new CharSequence[files.length];
			for (int i = 0; i < files.length; i++) {
				smiles[i] = files[i].getName();
			}
		}

        File file_colors = new File(Constants.PATH_COLORS);
        file_colors.mkdirs();
        File[] files_colors = file_colors.listFiles();
        if (files_colors != null) {
            colors = new CharSequence[files_colors.length + 2];
            for (int i = 0; i < files_colors.length; i++) {
                colors[i+2] = files_colors[i].getName();
            }
        }
        else colors = new CharSequence[2];
        colors[0] = "Light";
        colors[1] = "Dark";
		
		List<CharSequence> icons = new ArrayList<CharSequence>();
		icons.add("default");
		List<CharSequence> names = new ArrayList<CharSequence>();
		names.add("default");
		
		List<PackageInfo> list = getPackageManager().getInstalledPackages(0);
		for (PackageInfo pi : list) {
			String pn = pi.packageName;
			if (pn.startsWith("com.jtalk2.iconpack.")) {
				icons.add(pn);
				
				try {
					Resources res = getPackageManager().getResourcesForApplication(pn);
					int resID = res.getIdentifier(pn + ":string/app_name", null, null);
					names.add(res.getString(resID));
				} catch (Exception e) {
					names.add(pn);
				}
			}
		}

        CharSequence[] subscriptionEntries = new CharSequence[3];
        subscriptionEntries[0] = getString(R.string.AcceptAll);
        subscriptionEntries[1] = getString(R.string.Manual);
        subscriptionEntries[2] = getString(R.string.RejectAll);

        CharSequence[] subscriptionValues = new CharSequence[3];
        subscriptionValues[0] = Roster.SubscriptionMode.accept_all.name();
        subscriptionValues[1] = Roster.SubscriptionMode.manual.name();
        subscriptionValues[2] = Roster.SubscriptionMode.reject_all.name();

        ListPreference subscription = (ListPreference) getPreferenceScreen().findPreference("SubscriptionMode");
        subscription.setEntries(subscriptionEntries);
        subscription.setEntryValues(subscriptionValues);
		
		smilespack = (ListPreference) getPreferenceScreen().findPreference("SmilesPack");
		smilespack.setEntries(smiles);
		smilespack.setEntryValues(smiles);

        ListPreference colortheme = (ListPreference) getPreferenceScreen().findPreference("ColorTheme");
        colortheme.setEntries(colors);
        colortheme.setEntryValues(colors);
        if (colors.length == 1) colortheme.setValue("Dark");
		
		ListPreference iconspack = (ListPreference) getPreferenceScreen().findPreference("IconPack");
		iconspack.setEntries(names.toArray(new CharSequence[1]));
		iconspack.setEntryValues(icons.toArray(new CharSequence[1]));
		if (icons.size() == 1) iconspack.setValue("default");
		
		if (smiles.length > 0) {
			smilespack.setEnabled(prefs.getBoolean("ShowSmiles", true));
		} else smilespack.setEnabled(false);
		
		getPreferenceScreen().findPreference("version").setSummary(R.string.version);
		getPreferenceScreen().findPreference("build").setSummary(R.string.build);
	}
		
	@Override
	public void onDestroy() {
		super.onDestroy();
	}
		
	@Override
	public void onResume() {
		super.onResume();
		getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
	}
		
	@Override
	public void onPause() {
		super.onPause();
		getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
	}

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
            default:
                return false;
        }
        return true;
    }

	@Override
	public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
		smilespack.setEnabled(prefs.getBoolean("ShowSmiles", true));

		String iconPack = prefs.getString("IconPack", "default");
		IconPicker ip = JTalkService.getInstance().getIconPicker();
		if (ip != null && !iconPack.equals(ip.getPackName())) ip.loadIconPack();
		setResult(RESULT_OK);

        JTalkService service = JTalkService.getInstance();
        Cursor cursor = service.getContentResolver().query(JTalkProvider.ACCOUNT_URI, null, AccountDbHelper.ENABLED + " = '1'", null, null);
        if (cursor != null && cursor.getCount() > 0) {
            cursor.moveToFirst();
            do {
                String acc = cursor.getString(cursor.getColumnIndex(AccountDbHelper.JID)).trim();
                if (service.isAuthenticated(acc)) {
                    String mode = prefs.getString("SubscriptionMode", Roster.SubscriptionMode.accept_all.name());
                    Roster roster = service.getRoster(acc);
                    if (roster != null) roster.setSubscriptionMode(Roster.SubscriptionMode.valueOf(mode));
                }
            } while(cursor.moveToNext());
            cursor.close();
        }
	}
}
