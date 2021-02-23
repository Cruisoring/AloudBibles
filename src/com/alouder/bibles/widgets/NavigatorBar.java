package com.alouder.bibles.widgets;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.alouder.bibles.R;
import com.alouder.bibles.activities.AloudBibleApplication;
import com.alouder.bibles.data.INavigatable;

public class NavigatorBar extends LinearLayout {
	public final static String TAG = NavigatorBar.class.getSimpleName();

	onNavigationSelectedListener mCallback;

	// Container activity shall implement this interface to enable book
	// selection
	public interface onNavigationSelectedListener {
		public void onPrevious();

		public void onNext();
	}

	public enum useNamesFormat {
		fullNameCurrent, fullNameAll, abbreviationAll
	}

	// TextView currentView;
	TextView previousView;
	TextView nextView;
	INavigatable currentINavigatable;
	useNamesFormat useNames;

	public NavigatorBar(Context context) {
		this(context, null);
		// Log.d(TAG, "NavigatorBar(Context context, null): context=" +
		// context);
	}

	public NavigatorBar(Context context, AttributeSet attrs) {
		super(context, attrs);
		// Log.d(TAG, "NavigatorBar(Context context, attrs): attrs=" + attrs);

		// if (isInEditMode())
		// {
		// return;
		// }

		TypedArray a = getContext().obtainStyledAttributes(attrs,
				R.styleable.NavigatorBar);

		int ordinal = -1;
		try {
			ordinal = a.getInt(R.styleable.NavigatorBar_useNames, 0);
			useNames = (ordinal == 0) ? useNamesFormat.fullNameCurrent
					: (ordinal == 1) ? useNamesFormat.fullNameAll
							: useNamesFormat.abbreviationAll;
		} catch (Exception e) {
			Log.w(TAG, "Failed to retrieve useNames when ordinal=" + ordinal);
			useNames = useNamesFormat.fullNameAll;
		} finally {
			a.recycle();
		}

		initiate(context);
	}

	public void initiate(Context context) {
		// Log.d(TAG, "initiate(context) when previousView=" + previousView);

		LayoutInflater layoutInflater = LayoutInflater.from(context);
		layoutInflater.inflate(R.layout.navigation_bar, this, true);
		// currentView = (TextView) this.findViewById(R.id.current);

		previousView = (TextView) this.findViewById(R.id.previous);
		previousView.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				previousView.setClickable(false);
				if (mCallback != null) {
					mCallback.onPrevious();
				}
			}
		});

		nextView = (TextView) this.findViewById(R.id.next);
		nextView.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				nextView.setClickable(false);
				if (mCallback != null) {
					mCallback.onNext();
				}
			}
		});

		try {
			mCallback = (onNavigationSelectedListener) context;
		} catch (ClassCastException e) {
			Log.w(TAG,
					(context.toString() + " shall implement onNavigationSelectedListener interface"));
		}
	}

	public void setCurrent(INavigatable current) {
		if (current == null) {
			Log.e(TAG, "The current must be a valid INavigatable item!");
			return;
		}

		// Log.d(TAG, String.format("setCurrent(%s), currentINavigatable=%s",
		// current, currentINavigatable));

		String text = null;
		this.currentINavigatable = current;
		// currentView.setText(useNames != useNamesFormat.abbreviationAll ?
		// current
		// .getName() : current.getAbbreviation());

		if (current.hasPrevious()) {
			INavigatable prev = (INavigatable) current.getPrevious();
			if (prev == null) {
				previousView.setVisibility(View.INVISIBLE);
				Log.e(TAG,
						"Previous item equals to null when current.hasPrevious()==true");
			} else {
				text = (useNames != useNamesFormat.fullNameAll ? prev
						.getAbbreviation() : prev.getTitle());
				AloudBibleApplication.setText(previousView, text);
				previousView.setVisibility(View.VISIBLE);
			}
		} else {
			previousView.setVisibility(INVISIBLE);
		}

		if (current.hasNext()) {
			INavigatable next = (INavigatable) current.getNext();
			if (next == null) {
				nextView.setVisibility(View.INVISIBLE);
				Log.e(TAG,
						"Next item equals to null when current.hasNext()==true");
			} else {
				text = (useNames != useNamesFormat.fullNameAll ? next
						.getAbbreviation() : next.getTitle());
				AloudBibleApplication.setText(nextView, text);
				nextView.setVisibility(View.VISIBLE);
			}
		} else {
			nextView.setVisibility(INVISIBLE);
		}
		previousView.setClickable(true);
		nextView.setClickable(true);
	}
	//
	// public void deactivate()
	// {
	// previousView.setClickable(false);
	// nextView.setClickable(false);
	// }
}
