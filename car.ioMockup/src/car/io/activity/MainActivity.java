/* 
 * enviroCar 2013
 * Copyright (C) 2013  
 * Martin Dueren, Jakob Moellers, Gerald Pape, Christopher Stephan
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301  USA
 * 
 */

package car.io.activity;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import android.app.Application;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import car.io.R;
import car.io.adapter.DbAdapterLocal;
import car.io.adapter.UploadManager;
import car.io.application.ECApplication;
import car.io.application.NavMenuItem;
import car.io.exception.TracksException;
import car.io.views.TYPEFACE;
import car.io.views.Utils;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.MenuItem;

import de.keyboardsurfer.android.widget.crouton.Crouton;


/**
 * @author jakob
 *
 * @param <AndroidAlarmService>
 */
public class MainActivity<AndroidAlarmService> extends SherlockFragmentActivity implements OnItemClickListener {

	private int actionBarTitleID = 0;
	private ActionBar actionBar;

	public ECApplication application;

	private FragmentManager manager;
	//Navigation Drawer
	private DrawerLayout drawer;
	private ListView drawerList;
	private NavAdapter navDrawerAdapter;

	// Menu Items
	private NavMenuItem[] navDrawerItems;

	static final int START_STOP_MEASUREMENT = 3;
	static final int SETTINGS = 4;
	static final int LOGIN = 1;
	static final int MY_TRACKS = 2;
	static final int DASHBOARD = 0;

	public static final int REQUEST_MY_GARAGE = 1336;
	public static final int REQUEST_REDIRECT_TO_GARAGE = 1337;
	
	private SharedPreferences preferences = null;
	boolean alwaysUpload = false;
	boolean uploadOnlyInWlan = true;
		
	// Upload in Wlan

	// private boolean uploadOnlyInWlan;
	
	private void prepareNavDrawerItems(){
		if(this.navDrawerItems == null){
			navDrawerItems = new NavMenuItem[5];
			navDrawerItems[LOGIN] = new NavMenuItem(LOGIN, getResources().getString(R.string.menu_login),R.drawable.device_access_accounts);
			navDrawerItems[SETTINGS] = new NavMenuItem(SETTINGS, getResources().getString(R.string.menu_settings),R.drawable.action_settings);
			navDrawerItems[START_STOP_MEASUREMENT] = new NavMenuItem(START_STOP_MEASUREMENT, getResources().getString(R.string.menu_start),R.drawable.av_play);
			navDrawerItems[DASHBOARD] = new NavMenuItem(DASHBOARD, getResources().getString(R.string.dashboard), R.drawable.dashboard);
			navDrawerItems[MY_TRACKS] = new NavMenuItem(MY_TRACKS, getResources().getString(R.string.my_tracks),R.drawable.device_access_storage);
		}
		
		if (application.requirementsFulfilled()) { // was requirementsFulfilled
			try {
				if (application.getServiceConnector().isRunning()) {
					navDrawerItems[START_STOP_MEASUREMENT].setTitle(getResources().getString(R.string.menu_stop));
					navDrawerItems[START_STOP_MEASUREMENT].setIconRes(R.drawable.av_pause);
				} else {
					navDrawerItems[START_STOP_MEASUREMENT].setTitle(getResources().getString(R.string.menu_start));
					navDrawerItems[START_STOP_MEASUREMENT].setIconRes(R.drawable.av_play);
					// Only enable start button when adapter is selected
	
					SharedPreferences preferences = PreferenceManager
							.getDefaultSharedPreferences(this);
	
					String remoteDevice = preferences.getString(
							car.io.activity.SettingsActivity.BLUETOOTH_KEY,
							null);
	
					if (remoteDevice != null) {
						if(!preferences.contains(ECApplication.PREF_KEY_SENSOR_ID)){
							navDrawerItems[START_STOP_MEASUREMENT].setEnabled(false);
							navDrawerItems[START_STOP_MEASUREMENT].setIconRes(R.drawable.not_available);
							navDrawerItems[START_STOP_MEASUREMENT].setSubtitle(getResources().getString(R.string.no_sensor_selected));
						} else {
							navDrawerItems[START_STOP_MEASUREMENT].setEnabled(true);
							navDrawerItems[START_STOP_MEASUREMENT].setSubtitle(preferences.getString(SettingsActivity.BLUETOOTH_NAME, ""));
						}
					} else {
						navDrawerItems[START_STOP_MEASUREMENT].setEnabled(false);
						navDrawerItems[START_STOP_MEASUREMENT].setIconRes(R.drawable.not_available);
						navDrawerItems[START_STOP_MEASUREMENT].setSubtitle(getResources().getString(R.string.pref_summary_chose_adapter));
					}

				}
			} catch (NullPointerException e) {
				Log.e("obd2", "The Service Connector is null.");
				navDrawerItems[START_STOP_MEASUREMENT].setEnabled(false);
				navDrawerItems[START_STOP_MEASUREMENT].setIconRes(R.drawable.not_available);
				e.printStackTrace();
			}
		} else {
			navDrawerItems[START_STOP_MEASUREMENT].setTitle(getResources().getString(R.string.menu_start));
			navDrawerItems[START_STOP_MEASUREMENT].setIconRes(R.drawable.av_play);
			navDrawerItems[START_STOP_MEASUREMENT].setSubtitle(getResources().getString(R.string.pref_bluetooth_disabled));
			navDrawerItems[START_STOP_MEASUREMENT].setEnabled(false);
			navDrawerItems[START_STOP_MEASUREMENT].setIconRes(R.drawable.not_available);
		}
	
		if (application.isLoggedIn()) {
			navDrawerItems[LOGIN].setTitle(getResources().getString(R.string.menu_logout));
			navDrawerItems[LOGIN].setSubtitle(String.format(getResources().getString(R.string.logged_in_as),application.getUser().getUsername()));
		} else {
			navDrawerItems[LOGIN].setTitle(getResources().getString(R.string.menu_login));
			navDrawerItems[LOGIN].setSubtitle("");		
		}

		navDrawerAdapter.notifyDataSetChanged();

	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.main_layout);

		application = ((ECApplication) getApplication());
		
		preferences = PreferenceManager.getDefaultSharedPreferences(this);
		alwaysUpload = preferences.getBoolean(SettingsActivity.ALWAYS_UPLOAD, false);
        uploadOnlyInWlan = preferences.getBoolean(SettingsActivity.WIFI_UPLOAD, true);

		actionBar = getSupportActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
		actionBar.setHomeButtonEnabled(true);
		actionBar.setDisplayHomeAsUpEnabled(true);
		actionBar.setTitle("");

		// font stuff
		actionBarTitleID = Utils.getActionBarId();
		if (Utils.getActionBarId() != 0) {
			((TextView) this.findViewById(actionBarTitleID))
					.setTypeface(TYPEFACE.Newscycle(this));
		}

		actionBar.setLogo(getResources().getDrawable(R.drawable.actionbarlogo_with_padding));
		
		manager = getSupportFragmentManager();

		DashboardFragment initialFragment = new DashboardFragment();
		manager.beginTransaction().replace(R.id.content_frame, initialFragment)
				.commit();
		
		drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
		drawerList = (ListView) findViewById(R.id.left_drawer);
		navDrawerAdapter = new NavAdapter();
		prepareNavDrawerItems();
		drawerList.setAdapter(navDrawerAdapter);
		ActionBarDrawerToggle actionBarDrawerToggle = new ActionBarDrawerToggle(
				this, drawer, R.drawable.ic_drawer, R.string.open_drawer,
				R.string.close_drawer) {
		
			@Override
			public void onDrawerOpened(View drawerView) {
				prepareNavDrawerItems();
				super.onDrawerOpened(drawerView);
			}
		};

		drawer.setDrawerListener(actionBarDrawerToggle);
		drawerList.setOnItemClickListener(this);

		// --------------------------
		// --------------------------
		// --------------------------

		// AutoConnect checkbox and service

		// final CheckBox connectAutomatically = (CheckBox)
		// this.findViewById(R.id.checkBox1);
		//
		// connectAutomatically
		// .setOnCheckedChangeListener(new
		// CompoundButton.OnCheckedChangeListener() {
		//
		// @Override
		// public void onCheckedChanged(CompoundButton buttonView,
		// boolean isChecked) {
		// if (connectAutomatically.isChecked()) { // Start Service
		// // every minute

		// application.startServiceConnector();
		// } else { // Stop Service
		// application.stopServiceConnector();
		// }
		// }
		//
		// });

		// Toggle Button for WLan Upload
		/*
		 * final ToggleButton wlanToggleButton = (ToggleButton)
		 * findViewById(R.id.toggleButton1); wlanToggleButton.setChecked(true);
		 * uploadOnlyInWlan = true;
		 * 
		 * wlanToggleButton .setOnCheckedChangeListener(new
		 * CompoundButton.OnCheckedChangeListener() {
		 * 
		 * @Override public void onCheckedChanged(CompoundButton buttonView,
		 * boolean isChecked) { if (wlanToggleButton.isChecked()) {
		 * uploadOnlyInWlan = true; } else { uploadOnlyInWlan = false; }
		 * 
		 * } });
		 */

		// Button closeButton = (Button) findViewById(R.id.uploadnow);
		// closeButton.setOnClickListener(new OnClickListener() {
		//
		// @Override
		// public void onClick(View v) {
		// }
		// });

		// initDbAdapter();
		// testMethode();

		// Upload data every 10 minutes and only if there are more than 50
		// measurements stored in the database
		
		alwaysUpload = preferences.getBoolean(SettingsActivity.ALWAYS_UPLOAD, false);
		uploadOnlyInWlan = preferences.getBoolean(SettingsActivity.WIFI_UPLOAD, true);
        
		/*
		 * Auto-Uploader of tracks.
		 */

		ScheduledExecutorService uploadTaskExecutor = Executors
				.newScheduledThreadPool(1);
		uploadTaskExecutor.scheduleAtFixedRate(new Runnable() {

			@Override
			public void run() {
				ConnectivityManager connManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
				NetworkInfo mWifi = connManager
						.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

				Log.e("obd2", "Automatic upload");
                if (application.isLoggedIn()) {
                    try {
                        if (!application.getServiceConnector().isRunning()) {
                            Log.e("obd2", "Service connector not running");
                            if (alwaysUpload == true) {
                                if (uploadOnlyInWlan == true) {
                                    if (mWifi.isConnected()) {
                                        Log.e("obd2", "Uploading tracks");
                                        uploadTracks();
                                    }
                                } else {
                                    Log.e("obd2", "Uploading tracks");
                                    uploadTracks();
                                }
                            }
                        }
                    } catch (NullPointerException e) {
                        Log.e("obd2", "Service connector is null");
                        if (alwaysUpload == true) {
                            if (uploadOnlyInWlan == true) {
                                if (mWifi.isConnected()) {
                                    Log.e("obd2", "Uploading tracks");
                                    uploadTracks();
                                }
                            } else {
                                Log.e("obd2", "Uploading tracks");
                                uploadTracks();
                            }
                        }
                    }
                } 
			}
		}, 0, 2, TimeUnit.MINUTES);

	}
	
    private void uploadTracks() {
        DbAdapterLocal dbAdapter = (DbAdapterLocal) application
                .getDbAdapterLocal();
        
            try {
                if (dbAdapter.getNumberOfStoredTracks() > 0
                        && dbAdapter.getLastUsedTrack()
                                .getNumberOfMeasurements() > 0) {
                    UploadManager uploadManager = new UploadManager(dbAdapter,
                            application.getApplicationContext());
                    uploadManager.uploadAllTracks();
                }
            } catch (TracksException e) {
                Log.e("obd2", "Auto-Upload failed.");
                e.printStackTrace();
            }
        
    }

	private class NavAdapter extends BaseAdapter {
		

		@Override
		public boolean isEnabled(int position) {
			//to allow things like start bluetooth or go to settings from "disabled" action
			return (position == START_STOP_MEASUREMENT ? true : navDrawerItems[position].isEnabled());
		}
		
		@Override
		public int getCount() {
			return navDrawerItems.length;
		}

		@Override
		public Object getItem(int position) {
			return navDrawerItems[position];
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			NavMenuItem currentItem = ((NavMenuItem) getItem(position));
			View item;
			if(currentItem.getSubtitle().equals("")){
				item = View.inflate(MainActivity.this,R.layout.nav_item_1, null);
				
			} else {
				item = View.inflate(MainActivity.this,R.layout.nav_item_2, null);
				TextView textView2 = (TextView) item.findViewById(android.R.id.text2);
				textView2.setText(currentItem.getSubtitle());
				if(!currentItem.isEnabled()) textView2.setTextColor(Color.GRAY);
			}
			ImageView icon = ((ImageView) item.findViewById(R.id.nav_item_icon));
			icon.setImageResource(currentItem.getIconRes());
			TextView textView = (TextView) item.findViewById(android.R.id.text1);
			textView.setText(currentItem.getTitle());
			if(!currentItem.isEnabled()){
				textView.setTextColor(Color.GRAY);
				icon.setColorFilter(Color.GRAY);
			}
			TYPEFACE.applyCustomFont((ViewGroup) item, TYPEFACE.Raleway(MainActivity.this));
			return item;
		}

	}
	
    @Override
    public void onItemClick(AdapterView parent, View view, int position, long id) {
        openFragment(position);
    }

    private void openFragment(int position) {
        FragmentManager manager = getSupportFragmentManager();
        switch (position) {
        case DASHBOARD:
        	DashboardFragment dashboardFragment = new DashboardFragment();
            manager.beginTransaction().replace(R.id.content_frame, dashboardFragment).commit();
            break;
        case LOGIN:
        	if(application.isLoggedIn()){
        		application.logOut();
        		((ListMeasurementsFragment) getSupportFragmentManager().findFragmentByTag("MY_TRACKS")).clearRemoteTracks();
        	} else {
                LoginFragment loginFragment = new LoginFragment();
                manager.beginTransaction().replace(R.id.content_frame, loginFragment, "LOGIN").addToBackStack(null).commit();
        	}
            break;
        case SETTINGS:
			Intent configIntent = new Intent(this, SettingsActivity.class);
			startActivity(configIntent);
            break;
        case MY_TRACKS:
            ListMeasurementsFragment listMeasurementFragment = new ListMeasurementsFragment();
            manager.beginTransaction().replace(R.id.content_frame, listMeasurementFragment, "MY_TRACKS").addToBackStack(null).commit();
            break;
		case START_STOP_MEASUREMENT:
			SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

			String remoteDevice = preferences.getString(car.io.activity.SettingsActivity.BLUETOOTH_KEY,null);

			if (application.requirementsFulfilled() && remoteDevice != null) {
				if(!preferences.contains(ECApplication.PREF_KEY_SENSOR_ID)){
					if(application.isLoggedIn()){
			        	MyGarage garageFragment = new MyGarage();
			            getSupportFragmentManager().beginTransaction().replace(R.id.content_frame, garageFragment).addToBackStack(null).commit();
					}else{
		                LoginFragment loginFragment = new LoginFragment();
		                getSupportFragmentManager().beginTransaction().replace(R.id.content_frame, loginFragment, "LOGIN").addToBackStack(null).commit();
					}
				} else {
					if (!application.getServiceConnector().isRunning()) {
						application.startConnection();
					} else {
						application.stopConnection();
					}
				}
			} else {
				Intent settingsIntent = new Intent(this, SettingsActivity.class);
				startActivity(settingsIntent);
			}
			break;
        default:
            break;
        }
        drawer.closeDrawer(drawerList);

    }

	@Override
	protected void onDestroy() {
		super.onDestroy();

		// Close db connection

		application.closeDb();

		// Remove the services etc.

		application.destroyStuff();
		
		Crouton.cancelAllCroutons();

	}
	
	@Override
	protected void onResume() {
		super.onResume();
		drawer.closeDrawer(drawerList);
		alwaysUpload = preferences.getBoolean(SettingsActivity.ALWAYS_UPLOAD, false);
        uploadOnlyInWlan = preferences.getBoolean(SettingsActivity.WIFI_UPLOAD, true);
		
	}


	/**
	 * Determine what the menu buttons do
	 */
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {

		case android.R.id.home:
			if (drawer.isDrawerOpen(drawerList)) {
				drawer.closeDrawer(drawerList);
			} else {
				drawer.openDrawer(drawerList);
			}
			return true;
		}
		return false;
	}

	// @Override
	// public void onActivityResult(int requestCode, int resultCode, Intent
	// data) {
	// super.onActivityResult(requestCode, resultCode, data);
	// switch(requestCode){
	// case REQUEST_MY_GARAGE:
	// ((DashboardFragment) getFragmentByPosition(2)).updateSensorOnDashboard();
	// break;
	// case REQUEST_REDIRECT_TO_GARAGE:
	// if(resultCode == REQUEST_MY_GARAGE){
	// Intent garageIntent = new Intent(this, MyGarage.class);
	// startActivityForResult(garageIntent,REQUEST_MY_GARAGE);
	// }
	// ((DashboardFragment) getFragmentByPosition(2)).updateSensorOnDashboard();
	// break;
	// }
	// }
}
