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

package net.ustyugov.jtalk.activity.vcard;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.*;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import androidx.core.content.FileProvider;
import android.view.*;
import android.widget.*;
import net.ustyugov.jtalk.Colors;
import net.ustyugov.jtalk.Constants;
import net.ustyugov.jtalk.Pictures;
import net.ustyugov.jtalk.adapter.MainPageAdapter;
import net.ustyugov.jtalk.adapter.VCardAdapter;
import net.ustyugov.jtalk.listener.MyTextLinkClickListener;
import net.ustyugov.jtalk.service.JTalkService;

import net.ustyugov.jtalk.view.MyTextView;
import org.jivesoftware.smack.PacketCollector;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.filter.PacketIDFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.LastActivityManager;
import org.jivesoftware.smackx.packet.*;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import androidx.viewpager.widget.ViewPager;
import android.util.DisplayMetrics;

import com.jtalk2.R;
import com.viewpagerindicator.TitlePageIndicator;

public class VCardActivity extends Activity {
	private JTalkService service;
	private String account;
	private String jid;

	private MyTextView nick, first, last, middle, bday, url, about, ctry, locality, street, emailHome, phoneHome, org, unit, role, emailWork, phoneWork;
	private ProgressBar aboutProgress, homeProgress, workProgress, avatarProgress, statusProgress;
	private ScrollView aboutScroll, homeScroll, workScroll, avatarScroll;
	private ListView list;
	private ImageView av;
	private VCard vCard;
	private VCardAdapter adapter;
	
	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		service = JTalkService.getInstance();
		account = getIntent().getStringExtra("account");
		jid = getIntent().getStringExtra("jid");
        setTheme(Colors.isLight ? R.style.AppThemeLight : R.style.AppThemeDark);
		setContentView(R.layout.paged_activity);
		getActionBar().setDisplayHomeAsUpEnabled(true);
	    setTitle("vCard");
	    getActionBar().setSubtitle(jid);

        if (service.getConferencesHash(account).containsKey(StringUtils.parseBareAddress(jid))) {
            Presence p = service.getConferencesHash(account).get(StringUtils.parseBareAddress(jid)).getOccupantPresence(jid);
            if (p != null) {
                MUCUser mucUser = (MUCUser) p.getExtension("x", "http://jabber.org/protocol/muc#user");
                if (mucUser != null) {
                    String j = mucUser.getItem().getJid();
                    if (j != null && j.length() > 3) getActionBar().setSubtitle(j);
                }
            }
        }

		LinearLayout linear = (LinearLayout) findViewById(R.id.linear);
       	linear.setBackgroundColor(Colors.BACKGROUND);
		
       	LayoutInflater inflater = LayoutInflater.from(this);
		View aboutPage = inflater.inflate(R.layout.vcard_about, null);
		View homePage = inflater.inflate(R.layout.vcard_home, null);
		View workPage = inflater.inflate(R.layout.vcard_work, null);
		View avatarPage = inflater.inflate(R.layout.vcard_avatar, null);
		View statusPage = inflater.inflate(R.layout.list_activity, null);

		first = (MyTextView) aboutPage.findViewById(R.id.firstname);
		middle = (MyTextView) aboutPage.findViewById(R.id.middlename);
		last = (MyTextView) aboutPage.findViewById(R.id.lastname);
		nick = (MyTextView) aboutPage.findViewById(R.id.nickname);
		bday = (MyTextView) aboutPage.findViewById(R.id.bday);
		url = (MyTextView) aboutPage.findViewById(R.id.url);
		about = (MyTextView) aboutPage.findViewById(R.id.desc);
		
		ctry = (MyTextView) homePage.findViewById(R.id.ctry);
		locality = (MyTextView) homePage.findViewById(R.id.locality);
		street = (MyTextView) homePage.findViewById(R.id.street);
		emailHome = (MyTextView) homePage.findViewById(R.id.homemail);
		phoneHome = (MyTextView) homePage.findViewById(R.id.homephone);
		
		org = (MyTextView) workPage.findViewById(R.id.org);
		unit = (MyTextView) workPage.findViewById(R.id.unit);
		role = (MyTextView) workPage.findViewById(R.id.role);
		emailWork = (MyTextView) workPage.findViewById(R.id.workmail);
		phoneWork = (MyTextView) workPage.findViewById(R.id.workphone);
		
		av = (ImageView) avatarPage.findViewById(R.id.av);
        av.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                File file = new File(Constants.PATH + jid.replaceAll("/", "%"));
                Uri uri = Uri.fromFile(file);
                Intent intent = new Intent(Intent.ACTION_VIEW);
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
					Uri contentUri = FileProvider.getUriForFile(VCardActivity.this, "com.jtalk2.fileProvider", file);
					intent.setDataAndType(contentUri, "image/*");
					intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
				} else {
					intent.setDataAndType(uri, "image/*");
				}
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                try {
                    startActivity(intent);
                } catch(ActivityNotFoundException ignored) { }
            }
        });

        av.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                String fname = Constants.PATH + jid.replaceAll("/", "%");
                String saveto = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Pictures/Avatars/";

                File folder = new File(saveto);
                folder.mkdirs();

                try {
                    FileInputStream fis = new FileInputStream(fname);
                    byte[] buffer = new byte[fis.available()];
                    fis.read(buffer);
                    fis.close();

                    FileOutputStream fos = new FileOutputStream(saveto + "/" + jid.replaceAll("/", "%") + ".png");
                    fos.write(buffer);
                    fos.close();
                    Toast.makeText(VCardActivity.this, "Copied to " + saveto, Toast.LENGTH_LONG).show();
                } catch (Exception e) {
                    Toast.makeText(VCardActivity.this, "Failed to copy", Toast.LENGTH_LONG).show();
                }
                return true;
            }
        });
		
		statusProgress = (ProgressBar) statusPage.findViewById(R.id.progress);
		aboutProgress = (ProgressBar) aboutPage.findViewById(R.id.progress);
		homeProgress = (ProgressBar) homePage.findViewById(R.id.progress);
		workProgress = (ProgressBar) workPage.findViewById(R.id.progress);
		avatarProgress = (ProgressBar) avatarPage.findViewById(R.id.progress);
		
		aboutScroll = (ScrollView) aboutPage.findViewById(R.id.scroll);
		homeScroll = (ScrollView) homePage.findViewById(R.id.scroll);
		workScroll = (ScrollView) workPage.findViewById(R.id.scroll);
		avatarScroll = (ScrollView) avatarPage.findViewById(R.id.scroll);
		
		list = (ListView) statusPage.findViewById(R.id.list);
		list.setDividerHeight(0);
        list.setCacheColorHint(0x00000000);
		
		aboutPage.setTag(getString(R.string.About));
		homePage.setTag(getString(R.string.Home));
		workPage.setTag(getString(R.string.Work));
		avatarPage.setTag(getString(R.string.Photo));
		statusPage.setTag(getString(R.string.Status));

		ArrayList<View> mPages = new ArrayList<View>();
	    mPages.add(aboutPage);
	    mPages.add(homePage);
	    mPages.add(workPage);
	    mPages.add(avatarPage);
	    mPages.add(statusPage);

	    MainPageAdapter adapter = new MainPageAdapter(mPages);
	    ViewPager mPager = (ViewPager) findViewById(R.id.pager);
	    mPager.setAdapter(adapter);
	    mPager.setCurrentItem(0);
	        
	    TitlePageIndicator mTitleIndicator = (TitlePageIndicator) findViewById(R.id.indicator);
	    mTitleIndicator.setTextColor(0xFF555555);
	    mTitleIndicator.setViewPager(mPager);
	    mTitleIndicator.setCurrentItem(0);

        new LoadTask().execute();
	}

    @Override
    public void onResume() {
        super.onResume();
    }
	
	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.view_vcard, menu);
        return super.onCreateOptionsMenu(menu);
    }
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				finish();
				break;
			case R.id.refresh:
                new LoadTask().execute();
				break;
            case R.id.copy:
                ClipData.Item clipItem = new ClipData.Item(jid);
                String[] mimes = {"text/plain"};
                ClipData copyData = new ClipData(jid, mimes, clipItem);
                android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                clipboard.setPrimaryClip(copyData);
		}
		return true;
	}

	private class LoadTask extends AsyncTask<Integer, Integer, Integer> {
		private Bitmap bitmap = null;
		private RosterEntry re = null;
		private Hashtable<String, String> strings = new Hashtable<String, String>();

		@Override
		protected Integer doInBackground(Integer... arg0) {
			vCard = new VCard();
    		try {
    			vCard.load(service.getConnection(account), jid);
    			byte[] buffer = vCard.getAvatar();
    			
				if (buffer != null) {
					DisplayMetrics metrics = new DisplayMetrics();
					getWindowManager().getDefaultDisplay().getMetrics(metrics);
					int maxWidth = metrics.widthPixels;
					int maxHeight = metrics.heightPixels;

					BitmapFactory.Options options = new BitmapFactory.Options();
					options.inJustDecodeBounds = true;
					bitmap = BitmapFactory.decodeByteArray(buffer, 0, buffer.length, options);

					options.inSampleSize = Pictures.calculateInSampleSize(options, maxWidth, maxHeight);
					options.inJustDecodeBounds = false;
					bitmap = BitmapFactory.decodeByteArray(buffer, 0, buffer.length, options);

                    if (bitmap != null) {
						int width = bitmap.getWidth();
						if (width > maxWidth)  {
							double k = (double)width/(double)maxWidth;
							int h = (int) (bitmap.getHeight()/k);
							bitmap = Bitmap.createScaledBitmap(bitmap, maxWidth, h, true);
						} else {
                            bitmap = Bitmap.createScaledBitmap(bitmap, (int) (width * VCardActivity.this.getResources().getDisplayMetrics().density), (int) (bitmap.getHeight() * VCardActivity.this.getResources().getDisplayMetrics().density), true);
                        }
						av.setImageBitmap(bitmap);
					}

					try {
						String fname = jid.replaceAll("/", "%");
						File f = new File(Constants.PATH);
						f.mkdirs();
						FileOutputStream fos = new FileOutputStream(Constants.PATH + "/" + fname);
						fos.write(buffer);
						fos.close();
					} catch (Throwable t) { }
				}
    		} catch (Exception e) { }
    		
    		// Load info
    		try {
    			re = service.getRoster(account).getEntry(jid);
    			if (!jid.contains("/")) {
    				Iterator<Presence> it =  service.getRoster(account).getPresences(jid);
    				int i = 0;
    				while (it.hasNext()) {
    					i++;
    					Presence p = it.next();
    					if (p.getType() != Presence.Type.unavailable) {
                            String lastString = "";
                            try {
                                LastActivity activity = LastActivityManager.getLastActivity(service.getConnection(account), p.getFrom());
                                if (activity != null) {
                                    long idle = activity.getIdleTime() * 1000;

                                    Date date = new Date();
                                    date.setTime(System.currentTimeMillis()-idle);
                                    String time = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(date);

                                    if (idle != 0) lastString = getString(R.string.LastActivity) + ": " + time + "\n";
                                }
                            } catch (Exception ignored) { }

    						String vstr = "";
    						Version versionRequest = new Version();
    						versionRequest.setPacketID(System.currentTimeMillis()+i+"");
    						versionRequest.setType(IQ.Type.GET);
    						versionRequest.setTo(p.getFrom());

    						PacketCollector collector = service.getConnection(account).createPacketCollector(new PacketIDFilter(versionRequest.getPacketID()));
    						service.getConnection(account).sendPacket(versionRequest);
    						 
    						IQ result = (IQ)collector.nextResult(5000);
    						try {
    							if (result != null && result.getType() == IQ.Type.RESULT) {
    							    Version versionResult = (Version) result;
    							    vstr = getVersionString(versionResult);
    							}
    						} catch (ClassCastException e) { }

    						if (vstr.length() < 3) vstr += "???";

    						String str = "";
    						if (p.getStatus() != null) {
    							str += getString(R.string.Status) + ": " + p.getStatus() + "\n";
    						}
                            str += lastString;
    						str += getString(R.string.Client) + ": " + vstr;

    						strings.put(StringUtils.parseResource(p.getFrom()) + " (" + p.getPriority() + ")", str);
    					} else {
                            LastActivity activity = LastActivityManager.getLastActivity(service.getConnection(account), jid);
                            if (activity != null) {
                                long idle = activity.getIdleTime() * 1000;
                                String lastStatus = activity.getStatusMessage();
                                if (lastStatus == null) lastStatus = "";

                                Date date = new Date();
                                date.setTime(System.currentTimeMillis()-idle);
                                String time = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(date);

                                strings.put(getString(R.string.LastActivity), time + " " + lastStatus);
                            }
                        }
    				}
    			} else {
                    String lastActivity = "";
                    try {
                        LastActivity activity = LastActivityManager.getLastActivity(service.getConnection(account), jid);
                        if (activity != null) {
                            long idle = activity.getIdleTime() * 1000;
                            String lastStatus = activity.getStatusMessage();
                            if (lastStatus == null) lastStatus = "";

                            Date date = new Date();
                            date.setTime(System.currentTimeMillis()-idle);
                            String time = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(date);

                            lastActivity = getString(R.string.LastActivity) + ": " + time + "\n";
                        }
                    } catch (Exception ignored) { }

    				Version request = new Version();
    				request.setPacketID(System.currentTimeMillis()+"");
    				request.setType(IQ.Type.GET);
    				request.setTo(jid);

    				PacketCollector collector = service.getConnection(account).createPacketCollector(new PacketIDFilter(request.getPacketID()));
    				service.getConnection(account).sendPacket(request);
    					 
    				String vstr = "";
    				IQ result = (IQ)collector.nextResult(5000);
    				try {
    					if (result != null && result.getType() == IQ.Type.RESULT) {
    					    Version versionResult = (Version) result;
    					    vstr = getVersionString(versionResult);
    					}
    				} catch (ClassCastException e) { }
    					
    				if (vstr.length() < 3) vstr += "???";

                    String key = StringUtils.parseResource(jid);
    				String value = getString(R.string.Status) + ": " + service.getStatus(account, jid) + "\n"
                            + lastActivity
    						+ getString(R.string.Client) + ": " + vstr;

                    if (service.getConferencesHash(account).containsKey(StringUtils.parseBareAddress(jid))) {
                        Presence presence = service.getPresence(account, jid);
                        MUCUser mucUser = (MUCUser) presence.getExtension("x", "http://jabber.org/protocol/muc#user");
                        if (mucUser != null) {
                            String affiliation = mucUser.getItem().getAffiliation();
                            String role = mucUser.getItem().getRole();
                            key += " (" + role + "/" + affiliation + ")";
                        }
                    }

    				strings.put(key, value);
    			}
    		} catch (Exception e) { }
			return 1;
		}

		@Override
		protected void onPreExecute() {
			adapter = new VCardAdapter(VCardActivity.this);
			list.setVisibility(View.GONE);
			
			aboutProgress.setVisibility(View.VISIBLE);
		    homeProgress.setVisibility(View.VISIBLE);
		    workProgress.setVisibility(View.VISIBLE);
		    avatarProgress.setVisibility(View.VISIBLE);
		    statusProgress.setVisibility(View.VISIBLE);
		    
		    aboutScroll.setVisibility(View.GONE);
		    homeScroll.setVisibility(View.GONE);
		    workScroll.setVisibility(View.GONE);
		    avatarScroll.setVisibility(View.GONE);
		}
		
		@Override
		protected void onPostExecute(Integer result) {
			VCardActivity.this.runOnUiThread(new Runnable() {
				@Override
				public void run() {
                    MyTextLinkClickListener clickListener = new MyTextLinkClickListener(VCardActivity.this);
//	    			if (vCard.getField("FN") != null) {
//    				}
					
					if (vCard.getFirstName() != null) {
						first.setTextWithLinks(vCard.getFirstName());
                        first.setOnTextLinkClickListener(clickListener);
	    			}
	    			
	    			if (vCard.getMiddleName() != null) {
	    				middle.setTextWithLinks(vCard.getMiddleName());
                        middle.setOnTextLinkClickListener(clickListener);
	    			}
	    			
	    			if (vCard.getLastName() != null) {
	    				last.setTextWithLinks(vCard.getLastName());
                        last.setOnTextLinkClickListener(clickListener);
	    			}
	    			
	    			if (vCard.getNickName() != null) {
	    				nick.setTextWithLinks(vCard.getNickName());
                        nick.setOnTextLinkClickListener(clickListener);
	    			}
	    			
	    			if (vCard.getField("BDAY") != null) {
	    				bday.setTextWithLinks(vCard.getField("BDAY"));
                        bday.setOnTextLinkClickListener(clickListener);
	    			}
	    			
	    			if (vCard.getAddressFieldHome("CTRY") != null) {
	    				ctry.setTextWithLinks(vCard.getAddressFieldHome("CTRY"));
                        ctry.setOnTextLinkClickListener(clickListener);
	    			}
	    			
	    			if (vCard.getAddressFieldHome("LOCALITY") != null) {
	    				locality.setTextWithLinks(vCard.getAddressFieldHome("LOCALITY"));
                        locality.setOnTextLinkClickListener(clickListener);
	    			}
	    			
	    			if (vCard.getAddressFieldHome("STREET") != null) {
	    				street.setTextWithLinks(vCard.getAddressFieldHome("STREET"));
                        street.setOnTextLinkClickListener(clickListener);
	    			}
	    			
	    			if (vCard.getOrganization() != null) {
	    				org.setTextWithLinks(vCard.getOrganization());
                        org.setOnTextLinkClickListener(clickListener);
	    			}
	    			
	    			if (vCard.getOrganizationUnit() != null) {
	    				unit.setTextWithLinks(vCard.getOrganizationUnit());
                        unit.setOnTextLinkClickListener(clickListener);
	    			}
	    			
	    			if (vCard.getField("ROLE") != null) {
	    				role.setTextWithLinks(vCard.getField("ROLE"));
                        role.setOnTextLinkClickListener(clickListener);
	    			}
	    			
	    			if (vCard.getEmailHome() != null) {
	    				emailHome.setTextWithLinks(vCard.getEmailHome());
                        emailHome.setOnTextLinkClickListener(clickListener);
	    			}
	    			
	    			if (vCard.getEmailWork() != null) {
	    				emailWork.setTextWithLinks(vCard.getEmailWork());
                        emailWork.setOnTextLinkClickListener(clickListener);
	    			}
	    			
	    			if (vCard.getPhoneHome("VOICE") != null) {
	    				phoneHome.setTextWithLinks(vCard.getPhoneHome("VOICE"));
                        phoneHome.setOnTextLinkClickListener(clickListener);
	    			}
	    			
	    			if (vCard.getPhoneWork("VOICE") != null) {
	    				phoneWork.setTextWithLinks(vCard.getPhoneWork("VOICE"));
                        phoneWork.setOnTextLinkClickListener(clickListener);
	    			}
	    			
	    			if (vCard.getField("URL") != null) {
	    				url.setTextWithLinks(vCard.getField("URL"));
                        url.setOnTextLinkClickListener(clickListener);
	    			}
	    			
	    			if (vCard.getField("DESC") != null) {
	    				about.setTextWithLinks(vCard.getField("DESC"));
                        about.setOnTextLinkClickListener(clickListener);
	    			}

					if (re != null) {
	    				LinearLayout linear = (LinearLayout) ((LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.vcard_item, null);

						TextView resource = (TextView) linear.findViewById(R.id.resource);
						resource.setText(getString(R.string.Subscribtion) + ":");

						MyTextView value = (MyTextView) linear.findViewById(R.id.value);
						value.setText(re.getType().name());
						adapter.add(linear);
	    			}

					Enumeration<String> keys = strings.keys();
					while (keys.hasMoreElements()) {
						String key = keys.nextElement();

						LinearLayout linear = (LinearLayout) ((LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.vcard_item, null);
						TextView t1 = (TextView) linear.findViewById(R.id.resource);
						t1.setText(key);
						MyTextView t2 = (MyTextView) linear.findViewById(R.id.value);
						t2.setTextWithLinks(strings.get(key));
                        t2.setOnTextLinkClickListener(clickListener);
						adapter.add(linear);
					}

					list.refreshDrawableState();
				    list.setAdapter(adapter);
				    list.setVisibility(View.VISIBLE);
				    
				    aboutProgress.setVisibility(View.GONE);
				    homeProgress.setVisibility(View.GONE);
				    workProgress.setVisibility(View.GONE);
				    avatarProgress.setVisibility(View.GONE);
				    statusProgress.setVisibility(View.GONE);
				    
				    aboutScroll.setVisibility(View.VISIBLE);
				    homeScroll.setVisibility(View.VISIBLE);
				    workScroll.setVisibility(View.VISIBLE);
				    avatarScroll.setVisibility(View.VISIBLE);
				}
			});
		}
		
		private String getVersionString(Version versionResult) {
			String vstr;
			String os = versionResult.getOs();
			String ver = versionResult.getVersion();
			String name = versionResult.getName();
			vstr = name + " " + ver;
			if (os != null) {
				vstr += " (" + os + ")";
			}
			return vstr;
		}
	}
}
