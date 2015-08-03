package at.bitfire.davdroid.activity;

import at.bitfire.davdroid.R;
import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import aneeda.widget.BorderButton;
import android.widget.LinearLayout;

public class TwoButtonLayout extends LinearLayout {
    private static final int LAYOUT_MIN_HEIGHT = 50;
    private BorderButton leftButton;
    private BorderButton rightButton;
    final int horizontalThickness = 3;
    final int verticalThickness = 2;

    public TwoButtonLayout(Context context) {
        super(context);
    }

    public TwoButtonLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TwoButtonLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        intitLayout(context, attrs, defStyle);
    }

    private void intitLayout(Context context, AttributeSet attrs, int defStyle) {
        setOrientation(LinearLayout.HORIZONTAL);
        TypedArray array = context.obtainStyledAttributes(attrs,
                R.styleable.TwoButtonLayout);
        String leftButtonText = array
                .getString(R.styleable.TwoButtonLayout_leftButtonText);
        String rightButtonText = array
                .getString(R.styleable.TwoButtonLayout_rightButtonText);
        boolean leftButtonVisibility = array.getBoolean(
                R.styleable.TwoButtonLayout_leftButtonVisibility, true);
        boolean rightButtonVisibility = array.getBoolean(
                R.styleable.TwoButtonLayout_rightButtonVisibility, true);
        int minHeight = array.getInteger(R.styleable.TwoButtonLayout_minHeight,
                LAYOUT_MIN_HEIGHT);
        array.recycle();
        setMinimumHeight(minHeight);

        leftButton = new BorderButton(context, attrs);
        leftButton.setPadding(0, 0, 0, 0);
        rightButton = new BorderButton(context, attrs);
        rightButton.setPadding(0, 0, 0, 0);
        leftButton.setBorderThickness(verticalThickness, horizontalThickness,0,horizontalThickness);
        rightButton.setBorderThickness(verticalThickness, horizontalThickness, verticalThickness, horizontalThickness);
        final LinearLayout.LayoutParams left = new LinearLayout.LayoutParams(0,
                  LinearLayout.LayoutParams.MATCH_PARENT, 1);
        final LinearLayout.LayoutParams right = new LinearLayout.LayoutParams(
                  0, LinearLayout.LayoutParams.MATCH_PARENT, 1);
        left.gravity = Gravity.CENTER_VERTICAL | Gravity.LEFT;
        right.gravity = Gravity.CENTER_VERTICAL | Gravity.RIGHT;

        leftButton.setLayoutParams(left);
        rightButton.setLayoutParams(right);

        addView(leftButton, 0);
        addView(rightButton, 1);
        
                leftButton.setGravity(Gravity.CENTER_HORIZONTAL
                        | Gravity.CENTER_VERTICAL);
                rightButton.setGravity(Gravity.CENTER_HORIZONTAL
                        | Gravity.CENTER_VERTICAL);
                leftButton.setTextAppearance(context,
                        android.R.attr.textAppearanceSmall);
                rightButton.setTextAppearance(context,
                        android.R.attr.textAppearanceSmall);


        leftButton.setIncludeFontPadding(false);
        leftButton.setSingleLine(true);
        rightButton.setIncludeFontPadding(false);
        rightButton.setSingleLine(true);

        leftButton.setText(leftButtonText);
        rightButton.setText(rightButtonText);

        setLeftButtonVisibility(leftButtonVisibility);
        setRightButtonVisibility(rightButtonVisibility);
    }

    public void setLeftButtonText(String text) {
        if (leftButton != null)
            leftButton.setText(text);
    }

    public void setRightButtonText(String text) {
        if (rightButton != null)
            rightButton.setText(text);
    }

    public void setleftButtonText(int resId) {
        if (leftButton != null)
            leftButton.setText(resId);
    }

    public void setRightButtonText(int resId) {
        if (rightButton != null)
            rightButton.setText(resId);
    }

    public void setLeftButtonVisibility(boolean show) {
        if (leftButton != null)
            adjustButtonlayout(leftButton, show);
        adjustBorder();
    }

    public void setRightButtonVisibility(boolean show) {
        if (rightButton != null)
            adjustButtonlayout(rightButton, show);
        adjustBorder();
    }

    public void setLeftButtonClickListener(OnClickListener listener) {
        if (leftButton != null)
            leftButton.setOnClickListener(listener);
    }

    public void setRightButtonClickListener(OnClickListener listener) {
        if (rightButton != null)
            rightButton.setOnClickListener(listener);
    }

    /*
     * adjust buttonLayout bound on show/hide button when this button is hidden
     * other button takes the full screen width
     */
     private void adjustButtonlayout(Button button, boolean showButton) {
        LinearLayout.LayoutParams params = (LayoutParams) button
                .getLayoutParams();
        button.setVisibility(showButton ? View.VISIBLE : View.INVISIBLE);
        params.weight = showButton ? 1 : 0;
        button.setLayoutParams(params);
    }

    private void adjustBorder() {
        boolean showvertical = true;
        if(rightButton != null && rightButton.getVisibility() == View.VISIBLE) {
            showvertical = false;
        }
        if (leftButton != null) {
            leftButton.setBorderThickness(verticalThickness, horizontalThickness,showvertical ? verticalThickness : 0,horizontalThickness);
        }
    }

}
