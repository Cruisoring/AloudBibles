package com.alouder.bibles.widgets;

import java.util.ArrayList;

import com.alouder.bibles.R;
import com.alouder.bibles.activities.AloudBibleApplication;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

public class EasyTableLayout extends TableLayout implements OnClickListener
{
	public static final String TAG = EasyTableLayout.class.getSimpleName();
	public static final int DEFAULT_COLUMN_COUNT = 2;
	
	public static final int DEFAULT_CELL_VIEW_STYLE = android.R.attr.textViewStyle;
	
	public static final int widthMeasureSpec = MeasureSpec.makeMeasureSpec(
			LayoutParams.MATCH_PARENT, MeasureSpec.EXACTLY);
	public static final int heightMeasureSpec = MeasureSpec.makeMeasureSpec(
			LayoutParams.WRAP_CONTENT, MeasureSpec.EXACTLY);
	
	public static final int DEFAULT_POSITION_OFFSET = 1;
	
	// Notice: the third parameter "1" would result in the space of the cell be
	// equal
	public static TableRow.LayoutParams DEFAULT_CELL_LAYOUTPARAMS = new TableRow.LayoutParams(
			0, android.widget.TableRow.LayoutParams.WRAP_CONTENT, 1);
	
	onCellSelectedListener mCallback;
	
	// Container activity must implement this interface to enable book selection
	public interface onCellSelectedListener
	{
		public void onCellSelected(int position);
	}
	
	Context mContext;
	
	// The cell would be TextView by default
	int cellViewStyle = DEFAULT_CELL_VIEW_STYLE;
	
	int columnCount;
	ArrayList<View> cellViewsList;
	int selectedCellIndex = -1;
	int itemCount = 0;
	int rowWidth;
	
	public void setCellViewStyle(int cellViewStyle)
	{
		this.cellViewStyle = cellViewStyle;
	}
	
	public void setColumnCount(int columnCount)
	{
		this.columnCount = columnCount;
	}
	
	public EasyTableLayout(Context context)
	{
		this(context, null);
	}
	
	public EasyTableLayout(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		if (this.isInEditMode())
		{
			return;
		}
		
		init(context, attrs);
	}
	
	private void init(Context context, AttributeSet attrs)
	{
		try
		{
			mCallback = (onCellSelectedListener) context;
		}
		catch (ClassCastException e)
		{
			throw new ClassCastException(context.toString()
					+ " must implement onBookItemSelectedListener");
		}
		
		mContext = context;
		cellViewsList = new ArrayList<View>();
		
		TypedArray a = getContext().obtainStyledAttributes(attrs,
				R.styleable.EasyTableLayout);
		
		try
		{
			final int N = a.getIndexCount();
			for (int i = 0; i < N; i++)
			{
				int attr = a.getIndex(i);
				switch (attr)
				{
					case R.styleable.EasyTableLayout_columnCount:
						columnCount = a.getInt(attr, DEFAULT_COLUMN_COUNT);
						break;
					
					case R.styleable.EasyTableLayout_cellViewStyle:
						cellViewStyle = a.getInt(attr, DEFAULT_CELL_VIEW_STYLE);
						break;
					
					default:
						break;
				}
			}
		}
		finally
		{
			a.recycle();
		}
		
	}
		
	public int getRowWidth(Context context)
	{
		int width = context.getResources().getDisplayMetrics().widthPixels;
		width -= getPaddingRight();
		width -= getPaddingLeft();
		
		View view = this;
		ViewGroup parentGroup = (ViewGroup) view.getParent();
		while (parentGroup != null)
		{
			width -= parentGroup.getPaddingRight()
					+ parentGroup.getPaddingLeft();
			view = parentGroup;
			parentGroup = (ViewGroup) view.getParent();
		}
		
		return width;
	}

	public void load(int toIndex, boolean adjustable)
	{
		if (toIndex < 1)
		{
			Log.w(TAG, "Failed to load index com.alouder.views when toIndex="
					+ toIndex);
			return;
		}
		
		cellViewsList.clear();
		this.removeAllViews();
		
		TableRow row = null;
		for (int i = 0; i < toIndex; i++)
		{
			if ((i % columnCount) == 0)
			{
				row = new TableRow(mContext);
				this.addView(row);
			}
			// MarginLayoutParams rowLayoutParams = (MarginLayoutParams) row
			// .getLayoutParams();
			// int leftSpace = rowLayoutParams.width;
			//
			// TextView tView = new TextView(mContext, null,
			// R.style.EasyTableCellStyle);
			// tView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 25f);
			// int l = tView.getPaddingLeft();
			// int r = tView.getPaddingRight();
			//
			// row.addView(tView);
			// tView.setLayoutParams(DEFAULT_CELL_LAYOUTPARAMS);
			// tView.setText(String.valueOf(i + 1));
			// tView.setGravity(Gravity.CENTER);
			// tView.setBackgroundResource(R.drawable.selectable_selector);
			// tView.setOnClickListener(this);
			
			TextView tView = new TextView(mContext);
			tView.setTextAppearance(mContext, R.style.EasyTableCellStyle);
			tView.setLayoutParams(DEFAULT_CELL_LAYOUTPARAMS);
			tView.setGravity(Gravity.CENTER);
			tView.setBackgroundResource(R.drawable.text_selector);
			
			if (adjustable)
			{
				AloudBibleApplication.setText(tView, String.valueOf(i + 1));
			}
			else
			{
				tView.setText(String.valueOf(i + 1));
			}
			
			tView.setOnClickListener(this);
			row.addView(tView);
			cellViewsList.add(tView);
			
			if ((i == (toIndex - 1)) && ((toIndex % columnCount) != 0))
			{
				int rem = toIndex % columnCount;
				TextView filler = new TextView(mContext, null,
						R.style.EasyTableCellStyle);
				filler.setLayoutParams(new TableRow.LayoutParams(0, -2,
						columnCount - rem));
				row.addView(filler);
			}
		}
		
		itemCount = cellViewsList.size();
	}
	
	/*
	 * / public void load(ArrayList<String> items) { cellViewsList.clear();
	 * 
	 * int count = items.size(); this.removeAllViews();
	 * 
	 * TableRow row = null; for (int i = 0; i < count; i++) { if ((i %
	 * columnCount) == 0) { row = new TableRow(mContext);
	 * row.setBackgroundColor(Color.GRAY); this.addView(row); } String item =
	 * items.get(i);
	 * 
	 * // btn = new Button(mContext); btn.setText(item); //
	 * btn.setWidth(btnWidth); btn.setOnClickListener(this); //
	 * row.addView(btn); cellViewsList.add(btn);
	 * 
	 * // TextView tView = new TextView(mContext, null, //
	 * R.style.EasyTableCellStyle); TextView tView = new TextView(mContext,
	 * null); row.addView(tView);
	 * tView.setLayoutParams(DEFAULT_CELL_LAYOUTPARAMS); tView.setText(item); if
	 * (item.length() > 0) { tView.setGravity(Gravity.CENTER); //
	 * tView.setBackgroundColor(Color.WHITE); tView.setTextColor(Color.BLUE);
	 * tView.setOnClickListener(this); }
	 * 
	 * if ((i == (count - 1)) && ((count % columnCount) != 0)) { int rem = count
	 * % columnCount; TextView filler = new TextView(mContext, null,
	 * R.style.EasyTableCellStyle); filler.setLayoutParams(new
	 * TableRow.LayoutParams(0, -2, columnCount - rem)); row.addView(filler); }
	 * cellViewsList.add(tView); }
	 * 
	 * itemCount = cellViewsList.size(); } //
	 */
	
	@Override
	public void onClick(View v)
	{
		if (cellViewsList.contains(v) && (mCallback != null))
		{
			int position = cellViewsList.indexOf(v);
			setSelectedPosition(position);
			
			mCallback.onCellSelected(position);
		}
	}
	
	public void setSelectedPosition(int position)
	{
		if ((selectedCellIndex >= 0) && (selectedCellIndex < itemCount))
		{
			View previousView = cellViewsList.get(selectedCellIndex);
			previousView.setSelected(false);
		}
		selectedCellIndex = position;
		
		if ((position >= 0) && (position < cellViewsList.size()))
		{
			View current = cellViewsList.get(position);
			current.setSelected(true);
		}
	}
}
