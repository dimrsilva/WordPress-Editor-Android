package org.wordpress.android.editor;

import android.content.Context;
import android.text.InputType;
import android.util.AttributeSet;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.webkit.WebView;

public class EditorWebView extends WebView {

    private boolean mHideSuggestions = false;

    public EditorWebView(Context context) {
        super(context);
    }

    public EditorWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public EditorWebView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void hideSuggestions(boolean hideSuggestions) {
        mHideSuggestions = hideSuggestions;
    }

    @Override
    public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
        InputConnection connection = super.onCreateInputConnection(outAttrs);

        if (mHideSuggestions) {
            outAttrs.inputType += InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS;
            outAttrs.inputType += InputType.TYPE_TEXT_VARIATION_FILTER;
        }

        return connection;
    }

    @Override
    public boolean onCheckIsTextEditor() {
        return true;
    }

}
