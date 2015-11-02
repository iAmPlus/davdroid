package at.bitfire.davdroid.syncadapter;

import android.content.Context;
import android.util.AttributeSet;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.webkit.WebView;

public class MyWebView extends WebView {

	public MyWebView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	public MyWebView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
		InputConnection connection = super.onCreateInputConnection(outAttrs);

		outAttrs.imeOptions = (EditorInfo.IME_MASK_ACTION & EditorInfo.IME_ACTION_DONE)
				        | (EditorInfo.IME_MASK_ACTION & EditorInfo.IME_FLAG_NO_ENTER_ACTION)
				        | EditorInfo.TYPE_TEXT_FLAG_NO_SUGGESTIONS;
		return connection;
	}

	public MyWebView(Context context) {
		super(context);
	}

}
