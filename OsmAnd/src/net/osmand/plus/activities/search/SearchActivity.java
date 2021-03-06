package net.osmand.plus.activities.search;


import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Formatter;
import java.util.List;
import java.util.Locale;

import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.plus.OsmAndLocationProvider;
import net.osmand.plus.OsmAndLocationProvider.OsmAndLocationListener;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.FavouritesListActivity;
import net.osmand.plus.activities.FavouritesListFragment;
import net.osmand.plus.activities.NavigatePointFragment;
import net.osmand.util.Algorithms;
import android.app.ActionBar;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.support.v7.app.ActionBar.OnNavigationListener;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;

import com.example.android.common.view.SlidingTabLayout;

public class SearchActivity extends ActionBarActivity implements OsmAndLocationListener {
	public static final int POI_TAB_INDEX = 0;
	public static final int ADDRESS_TAB_INDEX = 1;
	public static final int LOCATION_TAB_INDEX = 2;
	public static final int FAVORITES_TAB_INDEX = 3;
	public static final int HISTORY_TAB_INDEX = 4;
	public static final int TRANSPORT_TAB_INDEX = 5;
	
	protected static final int POSITION_CURRENT_LOCATION = 1;
	protected static final int POSITION_LAST_MAP_VIEW = 2;
	protected static final int POSITION_FAVORITES = 3;
	protected static final int POSITION_ADDRESS = 4;
	
	private static final int REQUEST_FAVORITE_SELECT = 1;
	private static final int REQUEST_ADDRESS_SELECT = 2;
	
	public static final String SEARCH_LAT = "net.osmand.search_lat"; //$NON-NLS-1$
	public static final String SEARCH_LON = "net.osmand.search_lon"; //$NON-NLS-1$
	public static final String SHOW_ONLY_ONE_TAB = "SHOW_ONLY_ONE_TAB"; //$NON-NLS-1$

	Button searchPOIButton;
	private LatLon searchPoint = null;
	private LatLon reqSearchPoint = null;
	private boolean searchAroundCurrentLocation = false;

	private static boolean searchOnLine = false;
	private ArrayAdapter<String> spinnerAdapter;
	private OsmandSettings settings;
	List<WeakReference<Fragment>> fragList = new ArrayList<WeakReference<Fragment>>();
	private boolean showOnlyOneTab;

	private List<TabItem> mTabs = new ArrayList<TabItem>();
	private ViewPager mViewPager;
	private SlidingTabLayout mSlidingTabLayout;
	
	public interface SearchActivityChild {
		
		public void locationUpdate(LatLon l);
	}

	private static class TabItem {
		private final CharSequence mTitle;
		private final int mIcon;
        private final int mIndicatorColor;
        private final int mDividerColor;
        private final Class<?> fragment;
        
		public TabItem(CharSequence mTitle, int mIcon, int mIndicatorColor, int mDividerColor, Class<?> fragment) {
			this.mTitle = mTitle;
			this.mIcon = mIcon;
			this.mIndicatorColor = mIndicatorColor;
			this.mDividerColor = mDividerColor;
			this.fragment = fragment;
		}
        
	}


	private TabItem getTabIndicator(int iconId, int resId, Class<?> fragment){
		return new TabItem(getString(resId), iconId, Color.DKGRAY, Color.LTGRAY, fragment);
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		((OsmandApplication) getApplication()).applyTheme(this);
		super.onCreate(savedInstanceState);
		long t = System.currentTimeMillis();
		getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
		setContentView(R.layout.search_main);
		settings = ((OsmandApplication) getApplication()).getSettings();
		Integer tab = settings.SEARCH_TAB.get();
		showOnlyOneTab = getIntent() != null && getIntent().getBooleanExtra(SHOW_ONLY_ONE_TAB, false);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setTitle("");
		getSupportActionBar().setBackgroundDrawable(new ColorDrawable(Color.parseColor("#39464d")));
//		getSupportActionBar().setTitle(R.string.select_search_position);

        
		
		if (!showOnlyOneTab) {
	        mViewPager = (ViewPager)findViewById(R.id.pager);
	        mSlidingTabLayout = (SlidingTabLayout) findViewById(R.id.sliding_tabs);

			mTabs.add(getTabIndicator(R.drawable.tab_search_poi_icon, R.string.poi, getFragment(POI_TAB_INDEX)));
			mTabs.add(getTabIndicator(R.drawable.tab_search_address_icon, R.string.address, getFragment(ADDRESS_TAB_INDEX)));
			mTabs.add(getTabIndicator(R.drawable.tab_search_location_icon, R.string.search_tabs_location, getFragment(LOCATION_TAB_INDEX)));
			mTabs.add(getTabIndicator(R.drawable.tab_search_favorites_icon, R.string.favorite, getFragment(FAVORITES_TAB_INDEX)));
			mTabs.add(getTabIndicator(R.drawable.tab_search_history_icon, R.string.history, getFragment(HISTORY_TAB_INDEX)));

			
			mViewPager.setAdapter(new SearchFragmentPagerAdapter(getSupportFragmentManager()));
			mSlidingTabLayout.setViewPager(mViewPager);
			
			mViewPager.setCurrentItem(Math.min(tab , HISTORY_TAB_INDEX));
			mSlidingTabLayout.setOnPageChangeListener(new OnPageChangeListener() {
				
				@Override
				public void onPageSelected(int arg0) {
					settings.SEARCH_TAB.set(arg0);
				}
				
				@Override
				public void onPageScrolled(int arg0, float arg1, int arg2) {
				}
				
				@Override
				public void onPageScrollStateChanged(int arg0) {
					
				}
			});
		} else {
			setContentView(R.layout.search_activity_single);
			Class<?> cl = getFragment(tab);
			try {
				getSupportFragmentManager().beginTransaction().replace(R.id.layout, (Fragment) cl.newInstance()).commit();
			} catch (InstantiationException e) {
				throw new IllegalStateException(e);
			} catch (IllegalAccessException e) {
				throw new IllegalStateException(e);
			}
        }
        setTopSpinner();
		
		Log.i("net.osmand", "Start on create " + (System.currentTimeMillis() - t ));
		
		Intent intent = getIntent();
		OsmandSettings settings = ((OsmandApplication) getApplication()).getSettings();
		LatLon last = settings.getLastKnownMapLocation();
		if (intent != null) {
			double lat = intent.getDoubleExtra(SEARCH_LAT, 0);
			double lon = intent.getDoubleExtra(SEARCH_LON, 0);
			if (lat != 0 || lon != 0) {
				LatLon l = new LatLon(lat, lon);
				if(!Algorithms.objectEquals(reqSearchPoint, l)){
					reqSearchPoint = l;
					if ((Math.abs(lat - last.getLatitude()) < 0.00001) && (Math.abs(lon - last.getLongitude()) < 0.00001)) {
						updateSearchPoint(reqSearchPoint, getString(R.string.select_search_position) + " " + getString(R.string.search_position_map_view), false);
					} else {
						updateSearchPoint(reqSearchPoint, getString(R.string.select_search_position) + " ", true);
					}
				}
			}
		}
		if(searchPoint == null){
			if(!Algorithms.objectEquals(reqSearchPoint, last)){
				reqSearchPoint = last;
				updateSearchPoint(last, getString(R.string.select_search_position) + " " + getString(R.string.search_position_map_view), false);
			}
		}
    }

	protected Class<?> getFragment(int tab) {
		if(tab == POI_TAB_INDEX) {
			return SearchPoiFilterFragment.class;
		} else if(tab == ADDRESS_TAB_INDEX) {
			return searchOnLine ? SearchAddressOnlineFragment.class : SearchAddressFragment.class;
		} else if(tab == LOCATION_TAB_INDEX) {
			return NavigatePointFragment.class;
		} else if(tab == HISTORY_TAB_INDEX) {
			return SearchHistoryFragment.class;
		} else if(tab == TRANSPORT_TAB_INDEX) {
			return SearchTransportFragment.class;
		} else if(tab == FAVORITES_TAB_INDEX) {
			return FavouritesListFragment.class;
		}
		return SearchPoiFilterFragment.class;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int itemId = item.getItemId();
		switch (itemId) {
		case android.R.id.home:
			finish();
			return true;

		}
		return false;
	}

	private void setTopSpinner() {
		spinnerAdapter = new ArrayAdapter<String>(getSupportActionBar().getThemedContext(), android.R.layout.simple_spinner_item,
				new ArrayList<String>(Arrays.asList(new String[]{
						getString(R.string.search_position_undefined),
						getString(R.string.search_position_current_location),
						getString(R.string.search_position_map_view),
						getString(R.string.search_position_favorites),
						getString(R.string.search_position_address)
					}))
				);
		spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        getSupportActionBar().setListNavigationCallbacks(spinnerAdapter, new OnNavigationListener() {
			
			@Override
			public boolean onNavigationItemSelected(int position, long itemId) {
				if (position != 0) {
					if (position == POSITION_CURRENT_LOCATION) {
						net.osmand.Location loc = getLocationProvider().getLastKnownLocation();
						if(loc != null && System.currentTimeMillis() - loc.getTime() < 10000) {
							updateLocation(loc);
						} else {
							startSearchCurrentLocation();
							searchAroundCurrentLocation = true;
						}
					} else {
						searchAroundCurrentLocation = false;
						endSearchCurrentLocation();
						if (position == POSITION_LAST_MAP_VIEW) {
							updateSearchPoint(settings.getLastKnownMapLocation(), getString(R.string.select_search_position) + " " + getString(R.string.search_position_map_view), false);
						} else if (position == POSITION_FAVORITES) {
							Intent intent = new Intent(SearchActivity.this, FavouritesListActivity.class);
							intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
							intent.putExtra(FavouritesListFragment.SELECT_FAVORITE_POINT_INTENT_KEY, (Serializable) null);
							startActivityForResult(intent, REQUEST_FAVORITE_SELECT);
							getSupportActionBar().setSelectedNavigationItem(0);
						} else if (position == POSITION_ADDRESS) {
							Intent intent = new Intent(SearchActivity.this, SearchAddressActivity.class);
							intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
							intent.putExtra(SearchAddressFragment.SELECT_ADDRESS_POINT_INTENT_KEY, (String) null);
							startActivityForResult(intent, REQUEST_ADDRESS_SELECT);
							getSupportActionBar().setSelectedNavigationItem(0);
						}
					}
				}
				return true;
			}
		});
	}

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

		
		
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		
		if(requestCode == REQUEST_FAVORITE_SELECT && resultCode == FavouritesListFragment.SELECT_FAVORITE_POINT_RESULT_OK){
			FavouritePoint p = (FavouritePoint) data.getSerializableExtra(FavouritesListFragment.SELECT_FAVORITE_POINT_INTENT_KEY);
			if (p != null) {
				LatLon latLon = new LatLon(p.getLatitude(), p.getLongitude());
				updateSearchPoint(latLon, getString(R.string.select_search_position) + " " + p.getName(), false);
			}
		} else if(requestCode == REQUEST_ADDRESS_SELECT && resultCode == SearchAddressFragment.SELECT_ADDRESS_POINT_RESULT_OK){
			String name = data.getStringExtra(SearchAddressFragment.SELECT_ADDRESS_POINT_INTENT_KEY);
			LatLon latLon = new LatLon(
					data.getDoubleExtra(SearchAddressFragment.SELECT_ADDRESS_POINT_LAT, 0), 
					data.getDoubleExtra(SearchAddressFragment.SELECT_ADDRESS_POINT_LON, 0));
			if(name != null){
				updateSearchPoint(latLon, getString(R.string.select_search_position) + " " + name, false);
			} else {
				updateSearchPoint(latLon, getString(R.string.select_search_position) + " ", true);
			}
		}
	}
	
	public Toolbar getClearToolbar(boolean visible) {
		final Toolbar tb = (Toolbar) findViewById(R.id.bottomControls);
		tb.setTitle(null);
		tb.getMenu().clear();
		tb.setVisibility(visible? View.VISIBLE : View.GONE);
		return tb;
	}


	public void updateLocation(net.osmand.Location location){
		if (location != null) {
			updateSearchPoint(new LatLon(location.getLatitude(), location.getLongitude()),
					getString(R.string.select_search_position) + " " + getString(R.string.search_position_current_location_found), false);
			if (location.getAccuracy() < 20) {
				endSearchCurrentLocation();
			}
		}
	}
	public void startSearchCurrentLocation(){
		getLocationProvider().resumeAllUpdates();
		getLocationProvider().addLocationListener(this);
		updateSearchPoint(null,
				getString(R.string.search_position_current_location_search), false);
	}

	private OsmAndLocationProvider getLocationProvider() {
		return ((OsmandApplication) getApplication()).getLocationProvider();
	}
	
	public void endSearchCurrentLocation(){
		getLocationProvider().pauseAllUpdates();
		getLocationProvider().removeLocationListener(this);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		endSearchCurrentLocation();
	}
	
	private String formatLatLon(LatLon searchPoint){
		return new Formatter(Locale.US).format(" %.2f;%.2f", searchPoint.getLatitude(), searchPoint.getLongitude()).toString();
	}
	
	@Override
	public void onAttachFragment (Fragment fragment) {
	    fragList.add(new WeakReference<Fragment>(fragment));
	}
	
	public void updateSearchPoint(LatLon searchPoint, String message, boolean showLoc){
		spinnerAdapter.remove(spinnerAdapter.getItem(0));
		String suffix = "";
		if(showLoc && searchPoint != null){
			suffix = formatLatLon(searchPoint);
		}
		spinnerAdapter.insert(message + suffix, 0);
		this.searchPoint = searchPoint;
		for(WeakReference<Fragment> ref : fragList) {
	        Fragment f = ref.get();
	        if(f instanceof SearchActivityChild) {
	            if(!f.isDetached()) {
	            	((SearchActivityChild) f).locationUpdate(searchPoint);
	            }
	        }
	    }
		getSupportActionBar().setSelectedNavigationItem(0);
	}
	
	public LatLon getSearchPoint() {
		return searchPoint;
	}
	

	public boolean isSearchAroundCurrentLocation() {
		return searchAroundCurrentLocation;
	}
	
	public void startSearchAddressOffline(){
		searchOnLine = false;
		setAddressSpecContent();
	}
	
	public void startSearchAddressOnline(){
		searchOnLine = true;
		setAddressSpecContent();
	}
	
	public void setAddressSpecContent() {
//		mTabsAdapter.mViewPager.setCurrentItem(0);
//		mTabsAdapter.mTabHost.setCurrentTab(0);
//		if (searchOnLine) {
//			mTabsAdapter.mTabs.get(1).clss = SearchAddressOnlineFragment.class;
//		} else {
//			mTabsAdapter.mTabs.get(1).clss = SearchAddressFragment.class;
//		}
//		mTabsAdapter.notifyDataSetChanged();
//		mTabsAdapter.mViewPager.invalidate();
		Intent intent = getIntent();
		finish();
		startActivity(intent);
	}
	

    class SearchFragmentPagerAdapter extends FragmentPagerAdapter
            /*implements TabHost.OnTabChangeListener, ViewPager.OnPageChangeListener*/ {

        SearchFragmentPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        /**
         * Return the {@link android.support.v4.app.Fragment} to be displayed at {@code position}.
         * <p>
         * Here we return the value returned from {@link SamplePagerItem#createFragment()}.
         */
        @Override
        public Fragment getItem(int i) {
            try {
				return (Fragment) mTabs.get(i).fragment.newInstance();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
        }

        @Override
        public int getCount() {
            return mTabs.size();
        }

        // BEGIN_INCLUDE (pageradapter_getpagetitle)
        /**
         * Return the title of the item at {@code position}. This is important as what this method
         * returns is what is displayed in the {@link SlidingTabLayout}.
         * <p>
         * Here we return the value returned from {@link SamplePagerItem#getTitle()}.
         */
        @Override
        public CharSequence getPageTitle(int position) {
            return mTabs.get(position).mTitle;
        }
    }

}
