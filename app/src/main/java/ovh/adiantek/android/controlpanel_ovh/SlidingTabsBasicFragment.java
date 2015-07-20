package ovh.adiantek.android.controlpanel_ovh;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class SlidingTabsBasicFragment extends Fragment {
    private SlidingTabLayout mSlidingTabLayout;
    private ViewPager mViewPager;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_sample, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        mViewPager = (ViewPager) view.findViewById(R.id.viewpager);
        mViewPager.setAdapter(new SamplePagerAdapter());
        mSlidingTabLayout = (SlidingTabLayout) view.findViewById(R.id.sliding_tabs);
        mSlidingTabLayout.setViewPager(mViewPager);
    }

    class SamplePagerAdapter extends PagerAdapter {
        @Override
        public int getCount() {
            return 7;
        }

        @Override
        public boolean isViewFromObject(View view, Object o) {
            return o == view;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return getString(R.string.Web);
                case 1:
                    return getString(R.string.Dedicated);
                case 2:
                    return getString(R.string.Cloud);
                case 3:
                    return getString(R.string.Telecom);
                case 4:
                    return getString(R.string.Billing);
                case 5:
                    return getString(R.string.Support);
                case 6:
                    return getString(R.string.Myaccount);
            }
            return "You bugged system.";
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            View v;
            if (position == 0) {
                v = new Web(getActivity());
            } else {
                v = getActivity().getLayoutInflater().inflate(R.layout.pager_item,
                        container, false);
                TextView title = (TextView) v.findViewById(R.id.item_title);
                title.setText(String.valueOf(position + 1));
            }
            container.addView(v);
            return v;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((View) object);
        }
    }
}
