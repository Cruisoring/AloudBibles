package com.alouder.bibles.widgets;

import android.graphics.PointF;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

public class EasyGestureListener
// extends SimpleOnGestureListener
		implements OnTouchListener
{
	public static final String TAG = EasyGestureListener.class.getSimpleName();
	public static final int INVALID_POINTER_ID = -1;
	public static final int ACTION_HOVER_MOVE = 7;
	public static final int ACTION_SCROLL = 8;
	public static final int ACTION_HOVER_ENTER = 9;
	public static final int ACTION_HOVER_EXIT = 10;
	
	// Threshold to screen out vibrations
	public static final int SENSITIVITY = 10;
	private static final int SENSITIVITY_SQUARE = SENSITIVITY * SENSITIVITY;
	
	// Threshold to judge of the x/y movement is valid
	public static final int MININUM_MOVEMENT = 100;
	private static final int MIN_MOVEMENT_SQUARE = MININUM_MOVEMENT
			* MININUM_MOVEMENT;
	
	// The difference between two radians will be neglected, that is 30 degrees
	public static final double NEGLIGIBLE_RADIANS = Math.PI / 6;
	
	// Threshold ratio value between movements on x and y axis to get the major
	// one
	public static final double MIN_RATIO = 2;
	public static final double RECIPROCAL_MIN_RATIO = 1.0 / MIN_RATIO;
	
	public enum DetectedActionCode
	{
		NONE, SWIPE_LEFT, SWIPE_RIGHT, SWIPE_UP, SWIPE_DOWN, 
		ZOOM_IN, ZOOM_OUT, DUAL_MODE_BEGIN, DUAL_MODE_END,
		DUAL_LEFT, DUAL_RIGHT, DUAL_UP, DUAL_DOWN
	}
	
	enum TouchMode
	{
		NONE, SINGLE, DUAL
	}
	
	public interface OnDetectedActionListener
	{
		boolean onDetectedAction(DetectedActionCode code);
	}
	
	// Currently, only two touch points are supported
	// TODO: to support three or more touch points?
	private int pointerId0 = INVALID_POINTER_ID;
	private int pointerId1 = INVALID_POINTER_ID;
	
	PointF startPosition0 = new PointF();
	PointF startPosition1 = new PointF();
	PointF middleStart = new PointF();
	double startAngleRadians = 0;
	double startDistance = 0;
	
	PointF endPosition0 = new PointF();
	PointF endPosition1 = new PointF();
	
	TouchMode mode = TouchMode.NONE;
	
	private OnDetectedActionListener onDetectedActionListener = null;
	
	private boolean interceptTouch = false;
	
	public EasyGestureListener(boolean isTouchIntercepted)
	{
		this.interceptTouch = isTouchIntercepted;
	}
	
	public void setOnDetectedAction(OnDetectedActionListener listener)
	{
		onDetectedActionListener = listener;
	}
	
	public boolean isOnTouchIntercepted(MotionEvent event)
	{
		if (interceptTouch)
		{
			return true;
		}
		else
		{
			dumpEvent(null, event);
		}
		
		if (mode == TouchMode.DUAL)
		{
			return true;
		}
		
		final int action = event.getActionMasked();
		switch (action)
		{
			case MotionEvent.ACTION_POINTER_DOWN:
				return hasTwoPointers(event);
			default:
				break;
		}
		
		return false;
	}
	
	private boolean hasTwoPointers(MotionEvent event)
	{
		if (event.getPointerCount() < 2)
		{
			return false;
		}
		
		float deltaX = event.getX(0) - event.getX(1);
		float deltaY = event.getY(0) - event.getY(1);
		double square = (deltaX * deltaX) + (deltaY * deltaY);
		return square >= SENSITIVITY_SQUARE;
	}
	
	@Override
	public boolean onTouch(View v, MotionEvent event)
	{
		boolean result = false;
		
		// dumpEvent(v, event);
		final int action = event.getActionMasked();
		switch (action)
		{
			case MotionEvent.ACTION_DOWN:
				pointerId0 = event.getPointerId(0);
				
				// To reset the secondary point id
				pointerId1 = INVALID_POINTER_ID;
				
				startPosition0.set(event.getX(), event.getY());
				// Log.d(TAG, "mode from " + mode + " to SINGLE");
				mode = TouchMode.SINGLE;
				break;
			
			case MotionEvent.ACTION_POINTER_DOWN:
				// Skip further handling when there is already a secondary
				// pointer recorded
				if ((pointerId1 != INVALID_POINTER_ID)
						|| (event.findPointerIndex(pointerId1) != -1))
				{
					break;
				}
				else if (mode == TouchMode.DUAL)
				{
					Log.w(TAG, "mode=DUAL is not expected.");
					Log.w(TAG, "pointerId0=" + pointerId0 + ", pointerId1="
							+ pointerId1);
					dumpEvent(v, event);
					break;
				}
				
				result = initiatePointers(event);
				break;
			
			case MotionEvent.ACTION_UP:
				result = detectSinglePointAction(event);
				if (mode == TouchMode.DUAL)
				{
					if (onDetectedActionListener != null)
					{
						onDetectedActionListener
								.onDetectedAction(DetectedActionCode.DUAL_MODE_END);
					}
				}
				break;
			
			case MotionEvent.ACTION_POINTER_UP:
				// result = detectDualPointsAction(event);
				result = true;
				break;
			
			case MotionEvent.ACTION_MOVE:
				if (mode != TouchMode.DUAL)
				{
					break;
				}
				result = detectDualPointsMove(event);
				
				break;
			
			default:
				if (mode == TouchMode.DUAL)
				{
					dumpEvent(null, event);
				}
				break;
		}
		
		return result ? result : v.onTouchEvent(event);
	}
	
	private boolean detectSinglePointAction(MotionEvent event)
	{
		boolean result = false;
		
		// Avoid handling MotionEvents handled when ACTION_POINT_UP received
		if (mode == TouchMode.DUAL)
		{
			return true;
		}
		else if (mode != TouchMode.SINGLE)
		{
			// Log.w(TAG, "ACTION_UP received when mode=" + mode);
		}
		
		endPosition0.set(event.getX(), event.getY());
		final float xMove0 = endPosition0.x - startPosition0.x;
		final float yMove0 = endPosition0.y - startPosition0.y;
		DetectedActionCode code = getSwipe(xMove0, yMove0);
		if (code != DetectedActionCode.NONE)
		{
			if (onDetectedActionListener != null)
			{
				onDetectedActionListener.onDetectedAction(code);
			}
			result = true;
		}
		
		pointerId0 = INVALID_POINTER_ID;
		pointerId1 = INVALID_POINTER_ID;
		// Log.d(TAG, "mode from " + mode + " to NONE");
		mode = TouchMode.NONE;
		return result;
	}
	
	private boolean detectDualPointsMove(MotionEvent event)
	{
		if (mode != TouchMode.DUAL)
		{
			Log.w(TAG, "ACTION_POINTER_UP received when mode=" + mode);
			return false;
		}
		
		boolean result = true;
		
		// Get the index and pointerId that left the screen
		final int pointIndex = event.getActionIndex();
		final int pointId = event.getPointerId(pointIndex);
		final int remainedIndex = event.findPointerIndex((pointerId0 == pointId) ? pointerId1
						: pointerId0);
		
		// TODO: this might happen when there are more than two points
		if (remainedIndex == -1)
		{
//			Log.w(TAG, "Fail to get remainedIndex when pointId=" + pointId);
//			logUnexpected(event);
			return false;
		}
		
		if (pointId == pointerId0)
		{
			endPosition0.set(event.getX(pointIndex), event.getY(pointIndex));
			endPosition1.set(event.getX(remainedIndex),
					event.getY(remainedIndex));
		}
		else
		{
			endPosition0.set(event.getX(remainedIndex),
					event.getY(remainedIndex));
			endPosition1.set(event.getX(pointIndex), event.getY(pointIndex));
		}
		
		final DetectedActionCode code = getDualAction();
		if (code != DetectedActionCode.NONE)
		{
			if (onDetectedActionListener != null)
			{
				onDetectedActionListener.onDetectedAction(code);
			}
			
			startPosition0.set(endPosition0);
			startPosition1.set(endPosition1);
		}
		
		return result;
	}
	
	private DetectedActionCode getDualAction()
	{
		DetectedActionCode code = DetectedActionCode.NONE;
		
		final float xMove0 = endPosition0.x - startPosition0.x;
		final float yMove0 = endPosition0.y - startPosition0.y;
		final float xMove1 = endPosition1.x - startPosition1.x;
		final float yMove1 = endPosition1.y - startPosition1.y;
		
		final float xMovement = (xMove0 + xMove1) / 2;
		final float yMovement = (yMove0 + yMove1) / 2;
		final float movementSquare = (xMovement * xMovement)
				+ (yMovement * yMovement);
		
		if (movementSquare <= MIN_MOVEMENT_SQUARE)
		{
			// The middle point of the two points concerned is regarded as fixed
			
			// Check the angle of the end points is equal or similar to the
			// start angle
			final double endAngleRadians = Math.atan2(endPosition1.y
					- endPosition0.y, endPosition1.x - endPosition0.x);
			double deltaRadians = endAngleRadians - startAngleRadians;
			if (Math.abs(deltaRadians) > NEGLIGIBLE_RADIANS)
			{
				// The angle difference is too big to be regarded as a natural
				// Zoom-In or Zoom-Out
				Log.i(TAG, "Fail to get action code when deltaRadians="
						+ deltaRadians);
				return code;
			}
			
			// Determine ZoomIn and ZoomOut by making sure the distance between
			// the two points is increased/decreased enough
			final float xEndDistance = endPosition1.x - endPosition0.x;
			final float yEndDistance = endPosition1.y - endPosition0.y;
			final double endDistance = Math.sqrt((xEndDistance * xEndDistance)
					+ (yEndDistance * yEndDistance));
			
			if (endDistance > startDistance)
			{
				if ((endDistance - startDistance) >= MININUM_MOVEMENT)
				{
					// Log.i(TAG, "ZoomIn detected");
					code = DetectedActionCode.ZOOM_IN;
				}
				else
				{
					Log.w(TAG, "Pinch open is not big enough: endDistance="
							+ endDistance + ", " + "startDistance="
							+ startDistance);
				}
			}
			else
			{
				if ((startDistance - endDistance) >= MININUM_MOVEMENT)
				{
					// Log.i(TAG, "ZoomOut detected");
					code = DetectedActionCode.ZOOM_OUT;
				}
				else
				{
					Log.w(TAG, "Pinch close is not big enough: endDistance="
							+ endDistance + ", " + "startDistance="
							+ startDistance);
				}
			}
		}
		else
		{
			// The middle point of the two points concerned is moving
			code = getSwipe(xMovement, yMovement);
		}
		
		return code;
	}
	
	private DetectedActionCode getSwipe(float xMovement, float yMovement)
	{
		DetectedActionCode code = DetectedActionCode.NONE;
		
		// */ Option2: Get the major movement (x or y) by comparing their ratio
		// first
		double ratio = Math.abs(xMovement / yMovement);
		if (ratio >= MIN_RATIO) // xMovement is the major one
		{
			if (xMovement >= MININUM_MOVEMENT)
			{
				// Log.i(TAG, "SWIPE_RIGHT is detected.");
				code = mode == TouchMode.DUAL ? DetectedActionCode.DUAL_RIGHT : DetectedActionCode.SWIPE_RIGHT;
			}
			else if (xMovement <= -MININUM_MOVEMENT)
			{
				// Log.i(TAG, "SWIPE_LEFT is detected.");
				code = mode == TouchMode.DUAL ? DetectedActionCode.DUAL_LEFT : DetectedActionCode.SWIPE_LEFT;
			}
		}
		else if (ratio < RECIPROCAL_MIN_RATIO) // yMovement is the major one
		{
			if (yMovement <= MININUM_MOVEMENT)
			{
				// Log.i(TAG, "SWIPE_UP is detected.");
				code = mode == TouchMode.DUAL ? DetectedActionCode.DUAL_UP : DetectedActionCode.SWIPE_UP;
			}
			else if (yMovement >= -MININUM_MOVEMENT)
			{
				// Log.i(TAG, "SWIPE_DOWN is detected.");
				code =  mode == TouchMode.DUAL ? DetectedActionCode.DUAL_DOWN : DetectedActionCode.SWIPE_DOWN;
			}
		}
		
		return code;
	}
	
	private boolean initiatePointers(MotionEvent event)
	{
		boolean result;
		
		if (mode == TouchMode.DUAL)
		{
			Log.e(TAG, "the mode is DUAL already, state wrong!");
			return false;
		}
		
		final boolean hasTwoPointers = hasTwoPointers(event);
		if (hasTwoPointers)
		{
			final int pointer0 = event.getPointerId(0);
			final int pointer1 = event.getPointerId(1);
			final float x0 = event.getX(0);
			final float y0 = event.getY(0);
			final float x1 = event.getX(1);
			final float y1 = event.getY(1);
			
			// The previous ACTION_DOWN was not captured yet
			if (pointerId0 == INVALID_POINTER_ID)
			{
				pointerId0 = pointer0;
				startPosition0.set(x0, y0);
				pointerId1 = pointer1;
				startPosition1.set(x1, y1);
			}
			else if (pointerId0 == pointer0)
			{
				pointerId1 = pointer1;
				startPosition1.set(x1, y1);
			}
			else if (pointerId0 == pointer1)
			{
				pointerId1 = pointer0;
				startPosition1.set(x0, y0);
			}
			else
			{
				Log.w(TAG, "Unexpected: pointerId0=" + pointerId0
						+ ", pointerId1" + pointerId1);
				logUnexpected(event);
				return false;
			}
			
			final float deltaY = startPosition1.y - startPosition0.y;
			final float deltaX = startPosition1.x - startPosition0.x;
			startAngleRadians = Math.atan2(deltaY, deltaX);
			middleStart.set((startPosition0.x + startPosition1.x) / 2,
					(startPosition0.y + startPosition1.y) / 2);
			startDistance = Math.sqrt((deltaX * deltaX) + (deltaY * deltaY));
			
			if (mode != TouchMode.DUAL)
			{
				Log.d(TAG, "mode from " + mode + " to DUAL");
				mode = TouchMode.DUAL;
			}
			result = true;
		}
		else if (event.getPointerCount() < 2)
		{
			Log.e(TAG, "There should be two pointers detected!?");
			Log.d(TAG, "mode degrade from " + mode + " to SINGLE");
			mode = TouchMode.SINGLE;
			result = false;
		}
		else
		{
			Log.w(TAG, "The pointers are too close ?");
			result = false;
		}
		
		if (mode == TouchMode.DUAL)
		{
			if (onDetectedActionListener != null)
			{
				onDetectedActionListener
						.onDetectedAction(DetectedActionCode.DUAL_MODE_BEGIN);
			}
		}
		
		return result;
	}
	
	private void logUnexpected(MotionEvent event)
	{
		// Do nothing, just log the unexpected situation
		Log.e(TAG, "Something wrong: pointerIds are not as expected!");
		Log.e(TAG, "pointerId0=" + pointerId0 + ", pointerId1=" + pointerId1);
		dumpEvent(null, event);
		StackTraceElement[] traces = Thread.currentThread().getStackTrace();
		for (int i = 0; i < 3; i++)
		{
			Log.e(TAG, traces[i].toString());
		}
	}
	
	/** Show an event in the LogCat view, for debugging */
	private void dumpEvent(View v, MotionEvent event)
	{
		int action = event.getAction();
		String actionName = actionToString(action);
		
		StringBuilder sb = new StringBuilder();
		sb.append("event " + actionName + (v != null ? (" on " + v) : ""));
		
		sb.append("[");
		for (int i = 0; i < event.getPointerCount(); i++)
		{
			sb.append("#").append(i);
			sb.append("(pid ").append(event.getPointerId(i));
			sb.append(")=").append((int) event.getX(i));
			sb.append(",").append((int) event.getY(i));
			if ((i + 1) < event.getPointerCount())
			{
				sb.append(";");
			}
		}
		sb.append("]");
		Log.d(TAG, sb.toString());
	}
	
	/**
	 * Returns a string that represents the symbolic name of the specified
	 * action such as "ACTION_DOWN", "ACTION_POINTER_DOWN(3)" or an equivalent
	 * numeric constant such as "35" if unknown.
	 * 
	 * @param action
	 *            The action.
	 * @return The symbolic name of the specified action.
	 * @hide
	 */
	public static String actionToString(int action)
	{
		switch (action)
		{
			case MotionEvent.ACTION_DOWN:
				return "ACTION_DOWN";
			case MotionEvent.ACTION_UP:
				return "ACTION_UP";
			case MotionEvent.ACTION_CANCEL:
				return "ACTION_CANCEL";
			case MotionEvent.ACTION_OUTSIDE:
				return "ACTION_OUTSIDE";
			case MotionEvent.ACTION_MOVE:
				return "ACTION_MOVE";
			case ACTION_HOVER_MOVE:
				return "ACTION_HOVER_MOVE";
			case ACTION_SCROLL:
				return "ACTION_SCROLL";
			case ACTION_HOVER_ENTER:
				return "ACTION_HOVER_ENTER";
			case ACTION_HOVER_EXIT:
				return "ACTION_HOVER_EXIT";
		}
		int index = (action & MotionEvent.ACTION_POINTER_INDEX_MASK) >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
		switch (action & MotionEvent.ACTION_MASK)
		{
			case MotionEvent.ACTION_POINTER_DOWN:
				return "ACTION_POINTER_DOWN(" + index + ")";
			case MotionEvent.ACTION_POINTER_UP:
				return "ACTION_POINTER_UP(" + index + ")";
			default:
				return Integer.toString(action);
		}
	}
	
	/*
	 * / Obsolete of extends SimpleOnGestureListener since usually
	 * MotionEvent_UP cannot be notified // onFling should be triggered after
	 * MotionEvent.ACTION_UP, overriding it // might enable // detection of
	 * SWIPE_XX actions with one finger
	 * 
	 * @Override public boolean onFling(MotionEvent e1, MotionEvent e2, float
	 * velocityX, float velocityY) { final float xMovement = e2.getX() -
	 * e1.getX(); final float yMovement = e2.getY() - e1.getY();
	 * DetectedActionCode code = DetectedActionCode.NONE;
	 * 
	 * Log.d(TAG, "onFling(): xMovement=" + xMovement + ", yMovement=" +
	 * yMovement); dumpEvent(null, e1); dumpEvent(null, e2);
	 * 
	 * if (Math.abs(yMovement) < MININUM_MOVEMENT) { if (xMovement >=
	 * MININUM_MOVEMENT) { Log.i(TAG, "SWIPE_RIGHT is detected."); code =
	 * DetectedActionCode.SWIPE_RIGHT; } else if (xMovement <=
	 * -MININUM_MOVEMENT) { Log.i(TAG, "SWIPE_LEFT is detected."); code =
	 * DetectedActionCode.SWIPE_LEFT; } } else if (Math.abs(xMovement) <
	 * MININUM_MOVEMENT) { if (yMovement <= MININUM_MOVEMENT) { Log.i(TAG,
	 * "SWIPE_UP is detected."); code = DetectedActionCode.SWIPE_UP; } else if
	 * (yMovement >= -MININUM_MOVEMENT) { Log.i(TAG, "SWIPE_DOWN is detected.");
	 * code = DetectedActionCode.SWIPE_DOWN; } }
	 * 
	 * if (code != DetectedActionCode.NONE) { if (onDetectedActionListener !=
	 * null) { onDetectedActionListener.onDetectedAction(code); } return true; }
	 * else { return super.onFling(e1, e2, velocityX, velocityY); } }
	 * 
	 * 
	 * @Override public boolean onScroll(MotionEvent e1, MotionEvent e2, float
	 * distanceX, float distanceY) { // TODO Auto-generated method stub
	 * Log.d(TAG, "onScroll() received."); return super.onScroll(e1, e2,
	 * distanceX, distanceY); }
	 * 
	 * //
	 */// End of extends SimpleOnGestureListener
	
	/*
	 * / private boolean detectDualPointsAction(MotionEvent event) { if (mode !=
	 * TouchMode.DUAL) { Log.w(TAG, "ACTION_POINTER_UP received when mode=" +
	 * mode); }
	 * 
	 * boolean result = false;
	 * 
	 * // Get the index and pointerId that left the screen final int pointIndex
	 * = event.getActionIndex(); final int pointId =
	 * event.getPointerId(pointIndex); final int remainedIndex = event
	 * .findPointerIndex((pointerId0 == pointId) ? pointerId1 : pointerId0);
	 * 
	 * // TODO: this might happen when there are more than two points if
	 * (remainedIndex == -1) { Log.w(TAG,
	 * "Fail to get remainedIndex when pointId=" + pointId);
	 * logUnexpected(event); return result; }
	 * 
	 * if (pointId == pointerId0) { endPosition0.set(event.getX(pointIndex),
	 * event.getY(pointIndex)); endPosition1.set(event.getX(remainedIndex),
	 * event.getY(remainedIndex)); } else {
	 * endPosition0.set(event.getX(remainedIndex), event.getY(remainedIndex));
	 * endPosition1.set(event.getX(pointIndex), event.getY(pointIndex)); }
	 * 
	 * final DetectedActionCode code = getDualAction(); if (code !=
	 * DetectedActionCode.NONE) { if (onDetectedActionListener != null) {
	 * onDetectedActionListener.onDetectedAction(code); }
	 * 
	 * // Clear the pointerId0 to avoid duplicated handling later pointerId0 =
	 * INVALID_POINTER_ID; } else if (pointId == pointerId0) { pointerId0 =
	 * pointerId1; startPosition0.set(startPosition1); }
	 * 
	 * pointerId1 = INVALID_POINTER_ID;
	 * 
	 * return result; } //
	 */
}
