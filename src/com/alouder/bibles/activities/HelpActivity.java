package com.alouder.bibles.activities;

import android.content.res.Resources;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.alouder.bibles.R;
import com.alouder.bibles.tab.CompatTab;
import com.alouder.bibles.tab.CompatTabListener;
import com.alouder.bibles.tab.TabCompatActivity;
import com.alouder.bibles.tab.TabHelper;

public class HelpActivity extends TabCompatActivity {
	private static final String TAG = "HelpActivity";
	
	LayoutInflater inflater;
//	static EasyGestureListener myGestureListener = null;
//	LinearLayout host;
//	float adjusted = AloudBibleApplication.getSizeProportion();
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.tabhost);
		inflater = this.getLayoutInflater();
//		host = (LinearLayout) findViewById(R.id.container);
//		
//		if (myGestureListener == null ) {
//			myGestureListener = new EasyGestureListener(false);
//			myGestureListener.setOnDetectedAction(this);
////			host.setOnTouchListener(myGestureListener);
//		}
		
		Resources resources = this.getResources();
		String[] topics, hints;
		
        TabHelper tabHelper = getTabHelper();

        topics = resources.getStringArray(R.array.help_abouts_topics);
        hints = resources.getStringArray(R.array.help_abouts_hints);
        CompatTab aboutsTab = tabHelper.newTab("about")
                .setText(R.string.about)
                .setTabListener(new HelpTabListener(this, topics, hints));
        tabHelper.addTab(aboutsTab);

        topics = resources.getStringArray(R.array.help_bibles_topics);
        hints = resources.getStringArray(R.array.help_bibles_hints);
        CompatTab biblesTab = tabHelper.newTab("bibles")
                .setText(R.string.bibles)
                .setTabListener(new HelpTabListener(this, topics, hints));
        tabHelper.addTab(biblesTab);

        topics = resources.getStringArray(R.array.help_gestures_topics);
        hints = resources.getStringArray(R.array.help_gestures_hints);
        CompatTab gesturesTab = tabHelper.newTab("gesture")
                .setText(R.string.gestures)
                .setTabListener(new HelpTabListener(this, topics, hints));
        tabHelper.addTab(gesturesTab);

        topics = resources.getStringArray(R.array.help_voices_topics);
        hints = resources.getStringArray(R.array.help_voices_hints);
        CompatTab voicesTab = tabHelper.newTab("voices")
                .setText(R.string.voices)
                .setTabListener(new HelpTabListener(this, topics, hints));
        tabHelper.addTab(voicesTab);
	}
	
	public static class HelpTabListener implements CompatTabListener {

        private final TabCompatActivity mActivity;
        private final String[] mTopics, mHints;
        
        public HelpTabListener(TabCompatActivity activity, String[] topics, String[] hints) {
        	mActivity = activity;
        	
        	mTopics = topics;
        	mHints = hints;
        }
        
		@Override
		public void onTabSelected(CompatTab tab, FragmentTransaction ft) {
            // Check if the fragment is already initialized
            TextViewFragment fragment = (TextViewFragment) tab.getFragment();
            if (fragment == null) {
                // If not, instantiate and add it to the activity
                fragment = (TextViewFragment) Fragment.instantiate(mActivity, TextViewFragment.ClassName);
                Spannable content = getSpannableHelp(mTopics, mHints);
                fragment.setText(content);
                tab.setFragment(fragment);
                ft.add(android.R.id.tabcontent, fragment, tab.getTag());
            } else {
                // If it exists, simply attach it in order to show it
                ft.attach(fragment);
                Spannable content = getSpannableHelp(mTopics, mHints);
                fragment.setText(content);
//                CharSequence text = fragment.getText();
//                Log.d(TAG, text.toString());
            }
		}

		@Override
		public void onTabUnselected(CompatTab tab, FragmentTransaction ft) {
            Fragment fragment = tab.getFragment();
            if (fragment != null) {
                // Detach the fragment, because another one is being attached
                ft.detach(fragment);
            }
		}

		@Override
		public void onTabReselected(CompatTab tab, FragmentTransaction ft) {
		}
		
	}

    public static class TextViewFragment extends Fragment {
    	public static final String ClassName = TextViewFragment.class.getName();
    	private CharSequence text = "";
    	public TextView textView;
    	
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
        	View v = inflater.inflate(R.layout.help_text, null);
            textView = (TextView) v.findViewById(android.R.id.text1);
            textView.setText(text);
//            textView.setLinksClickable(true);
//            textView.setOnTouchListener(HelpActivity.myGestureListener);
            return v;
        }
        
        public void setText(CharSequence text) {
        	this.text = text;
//        	Log.i(TAG, text.toString());
//        	textView.setText(text);
        }
        
        public CharSequence getText() {
        	return text;
        }
    }

	public static Spannable getSpannableHelp(String[] topics, String[] texts)
	{
		SpannableStringBuilder sb = new SpannableStringBuilder();
		String topic;
		int length, start;
		
		for (int i = 0; i < topics.length; i++) {
			topic = topics[i] + "\n";
			length = topic.length();
			start = sb.length();
			sb.append(topic);
			sb.setSpan(new StyleSpan(Typeface.BOLD_ITALIC), start, start+length, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
			sb.setSpan(new UnderlineSpan(), start, start+length, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
			CharSequence text = Html.fromHtml(texts[i]);
			sb.append(text);
			sb.append("\n\n");
		}
		sb.setSpan(AloudBibleApplication.sizeSpan, 0, sb.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
		return sb;
	}

//	@Override
//	public boolean onDetectedAction(DetectedActionCode code) {
//		switch (code)
//		{
//			case ZOOM_IN:
//				refresh(MyRelativeSizeSpan.AdjustStep);
////				Log.i(TAG, "Zoom_In handled, adjustedProportion=" + adjusted);
//				return true;
//			case ZOOM_OUT:
//				refresh(-MyRelativeSizeSpan.AdjustStep);
////				Log.i(TAG, "Zoom_Out handled, adjustedProportion=" + adjusted);
//				return true;
//			default:
//				break;
//		}
//		
//		return false;
//	}
//	
//	private void refresh(float delta)
//	{
//		adjusted = AloudBibleApplication.adjustSizeSpan(delta);
//		host.requestLayout();
//	}
}
