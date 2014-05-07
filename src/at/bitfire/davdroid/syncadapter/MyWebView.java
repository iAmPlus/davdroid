package at.bitfire.davdroid.syncadapter;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.webkit.WebView;

public class MyWebView extends WebView {

	public MyWebView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		// TODO Auto-generated constructor stub
	}

	public MyWebView(Context context, AttributeSet attrs) {
		super(context, attrs);
		// TODO Auto-generated constructor stub
	}

	@Override
	public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
		InputConnection connection = super.onCreateInputConnection(outAttrs);

		outAttrs.imeOptions = EditorInfo.IME_FLAG_NO_ENTER_ACTION | EditorInfo.IME_FLAG_NO_EXTRACT_UI
						| EditorInfo.IME_FLAG_NO_FULLSCREEN | (EditorInfo.IME_MASK_ACTION & EditorInfo.IME_ACTION_NEXT);
		return connection;
	}

	public MyWebView(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
	}

}
