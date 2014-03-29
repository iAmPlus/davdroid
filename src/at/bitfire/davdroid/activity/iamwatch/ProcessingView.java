package at.bitfire.davdroid.activity.iamwatch;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

public class ProcessingView extends View {

	private static final int CIRCLE_HEIGHT = 45;
	private static final int CIRCLE_WIDTH = 45;
	private static final int STROKE = 4;
	private static final int DURATION = 1000;
	private static final int REFRESH_RATE = 16;
	private static final float START_ANGLE = 90;
	private static final float SWEEP_ANGLE = 180;
	private static final float SWAP_SIZE = 0.6f;
	//private static final String TAG = "SampleView";
	Paint mPaint = null;
	private State mState =  State.Begin; 
	private RectF mRectOuter = new RectF(10,10,CIRCLE_WIDTH, CIRCLE_HEIGHT);
	private RectF mRectCenter = null;
	private RectF mRectInner = null;

	private float mOffset;
	private float mDiminishingOffset;
	private float mAngleInnerRectGray;
	private float mAngleInnerRectWhite;
	private float mAngleCenterRect;
	private float mAngleOuterRect;

	enum State {
		Begin,
		Deminish,
		Roate,
		Expand
	}

	public ProcessingView(Context context) {
		super(context);

		mRectCenter = new RectF(mRectOuter.left + STROKE,mRectOuter.top + STROKE ,mRectOuter.right - STROKE, mRectOuter.bottom - STROKE);
		mRectInner = new RectF(mRectCenter.left + STROKE,mRectCenter.top + STROKE ,mRectCenter.right - STROKE, mRectCenter.bottom - STROKE);
		setBackgroundColor(Color.BLACK);
	}
	public ProcessingView(Context context, AttributeSet set) {
		super(context, set);

		mRectCenter = new RectF(mRectOuter.left + STROKE,mRectOuter.top + STROKE ,mRectOuter.right - STROKE, mRectOuter.bottom - STROKE);
		mRectInner = new RectF(mRectCenter.left + STROKE,mRectCenter.top + STROKE ,mRectCenter.right - STROKE, mRectCenter.bottom - STROKE);
		setBackgroundColor(Color.BLACK);
	}

	protected void onDraw(Canvas canvas) {

		 switch (mState) {
		case Begin:


			mAngleOuterRect = START_ANGLE - mOffset;
			mAngleCenterRect = -START_ANGLE + mOffset;
			mAngleInnerRectWhite = START_ANGLE + mOffset;
			mAngleInnerRectGray = -START_ANGLE + mOffset;

			drawArac(canvas, mRectInner, mAngleInnerRectWhite, Color.WHITE);
			drawArac(canvas, mRectInner, mAngleInnerRectGray, Color.GRAY);

			mOffset = mOffset + (float)(REFRESH_RATE * 360) / DURATION;

			if (mOffset >= SWEEP_ANGLE) {
				mOffset = 0.0f;
				mState = State.Deminish;
			}
			break;

		case Deminish: 

			mDiminishingOffset =  mDiminishingOffset + SWAP_SIZE ;//(float)(REFRESH_RATE * STROKE) / DURATION;

			updateRect(State.Deminish,mRectOuter, SWAP_SIZE );
			updateRect(State.Expand,mRectCenter, SWAP_SIZE );

			drawArac(canvas, mRectInner, mAngleInnerRectWhite,Color.WHITE);
			drawArac(canvas, mRectInner, mAngleInnerRectGray,Color.GRAY);

			if (mDiminishingOffset >= STROKE) {

				mDiminishingOffset = 0.0f;

				drawArac(canvas, mRectInner, mAngleInnerRectWhite, Color.GRAY);
				drawArac(canvas, mRectInner, mAngleInnerRectGray, Color.WHITE);

				mState = State.Roate;
			}
			break;

		case Roate:


			mAngleOuterRect = -START_ANGLE - mOffset;
			mAngleCenterRect = START_ANGLE + mOffset;
			mAngleInnerRectWhite = -START_ANGLE + mOffset;
			mAngleInnerRectGray = START_ANGLE + mOffset;

			drawArac(canvas, mRectInner, mAngleInnerRectWhite, Color.GRAY);
			drawArac(canvas, mRectInner, mAngleInnerRectGray, Color.WHITE);

			mOffset = mOffset + (float)(REFRESH_RATE * 360) / DURATION;

			if (mOffset >= SWEEP_ANGLE) {
				mOffset = 0.0f;
				mState = State.Expand;
			}
			break;
		case Expand:
			mDiminishingOffset = mDiminishingOffset + SWAP_SIZE ;//(float)(REFRESH_RATE * STROKE) / 200;

			updateRect(State.Deminish,mRectCenter, SWAP_SIZE );
			updateRect(State.Expand,mRectOuter, SWAP_SIZE );

			drawArac(canvas, mRectInner, mAngleInnerRectWhite,Color.GRAY);
			drawArac(canvas, mRectInner, mAngleInnerRectGray, Color.WHITE);

			if (mDiminishingOffset >= STROKE) {

				drawArac(canvas, mRectInner, mAngleInnerRectWhite, Color.WHITE);
				drawArac(canvas, mRectInner, mAngleInnerRectGray,Color.GRAY);

				mDiminishingOffset = 0.0f;
				mState = State.Begin;
			}

			break;

		default:
			break;
		}
		drawArac(canvas, mRectOuter, mAngleOuterRect,Color.GRAY);
		drawArac(canvas, mRectCenter, mAngleCenterRect,Color.WHITE);
		invalidate();
	}

	public void updateRect(State state,RectF rectF, float offset) {
		if (state == State.Deminish) {
			rectF.left = rectF.left + offset;
			rectF.top = rectF.top + offset;
			rectF.right = rectF.right - offset;
			rectF.bottom = rectF.bottom - offset;
		} else if (state == State.Expand){
			rectF.left = rectF.left - offset;
			rectF.top = rectF.top - offset;
			rectF.right = rectF.right + offset;
			rectF.bottom = rectF.bottom + offset;
		}
	}

	public void drawArac(Canvas canvas, RectF rect, float angle , int Color) {
		canvas.drawArc(rect, angle, SWEEP_ANGLE,false, getPaint(Color));
	}

	public Paint getPaint(int color) {
		if (mPaint == null) {
			mPaint = new Paint();
			mPaint.setStyle(Paint.Style.STROKE);
			mPaint.setDither(true);
			mPaint.setAntiAlias(true);
		}
		mPaint.setColor(color);
		mPaint.setStrokeWidth(STROKE);
		return mPaint; 
	}
}
