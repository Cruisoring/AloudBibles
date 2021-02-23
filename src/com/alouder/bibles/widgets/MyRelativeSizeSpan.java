package com.alouder.bibles.widgets;

import android.text.TextPaint;
import android.text.style.RelativeSizeSpan;

public class MyRelativeSizeSpan extends RelativeSizeSpan
{
	// Make sure AdjustStep can be divided by 1f
	public static final float AdjustStep = 0.1f;
	private static final int factor = Math.round(1f / AdjustStep);
	
	private final float MIN_PROPORTION = 0.5f;
	private final float MAX_PROPORTION = 2.5f;
	private float adjustedProportion;
	
	public MyRelativeSizeSpan(float proportion)
	{
		super(proportion);
		adjustedProportion = proportion;
	}
	
	@Override
	public float getSizeChange()
	{
		return adjustedProportion;
	}
	
	@Override
	public void updateDrawState(TextPaint ds)
	{
		ds.setTextSize(ds.getTextSize() * adjustedProportion);
	}
	
	@Override
	public void updateMeasureState(TextPaint ds)
	{
		ds.setTextSize(ds.getTextSize() * adjustedProportion);
	}
	
	public float adjust(float delta)
	{
		adjustedProportion += delta;
		
		if (adjustedProportion <= MIN_PROPORTION)
		{
			adjustedProportion = MIN_PROPORTION;
		}
		else if (adjustedProportion >= MAX_PROPORTION)
		{
			adjustedProportion = MAX_PROPORTION;
		}
		else
		{
			adjustedProportion = (float) Math
					.round(adjustedProportion * factor) / factor;
		}
		
		return adjustedProportion;
	}
	
}
