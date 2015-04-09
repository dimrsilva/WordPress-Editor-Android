package org.wordpress.android.editor;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.AssetManager;
import android.os.Build;
import android.os.Bundle;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.webkit.ConsoleMessage;
import android.webkit.JavascriptInterface;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ToggleButton;

import com.android.volley.toolbox.ImageLoader;

import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.helpers.MediaFile;
import org.wordpress.android.util.helpers.MediaGallery;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class EditorFragment extends EditorFragmentAbstract implements View.OnClickListener {
    private static final String ARG_PARAM_TITLE = "param_title";
    private static final String ARG_PARAM_CONTENT = "param_content";

    private static final String JS_CALLBACK_HANDLER = "nativeCallbackHandler";

    private String mParamTitle;
    private String mParamContent;
    private EditorWebView mWebView;

    private ToggleButton mBoldButton;

    public static EditorFragment newInstance(String title, String content) {
        EditorFragment fragment = new EditorFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM_TITLE, title);
        args.putString(ARG_PARAM_CONTENT, content);
        fragment.setArguments(args);
        return fragment;
    }

    public EditorFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParamTitle = getArguments().getString(ARG_PARAM_TITLE);
            mParamContent = getArguments().getString(ARG_PARAM_CONTENT);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_editor, container, false);
        mWebView = (EditorWebView) view.findViewById(R.id.webview);
        initWebView();

        mBoldButton = (ToggleButton) view.findViewById(R.id.bold);
        mBoldButton.setOnClickListener(this);

        return view;
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void initWebView() {
        WebSettings webSettings = mWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDefaultTextEncodingName("utf-8");
        mWebView.setWebViewClient(new WebViewClient() {
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                AppLog.e(T.EDITOR, description);
            }
        });
        mWebView.setWebChromeClient(new WebChromeClient() {
            public boolean onConsoleMessage(ConsoleMessage cm) {
                AppLog.d(T.EDITOR, cm.message() + " -- From line " + cm.lineNumber() + " of " + cm.sourceId());
                return true;
            }

            @Override
            public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
                AppLog.d(T.EDITOR, message);
                return true;
            }

            @Override
            public void onConsoleMessage(String message, int lineNumber, String sourceId) {
                AppLog.d(T.EDITOR, message + " -- from line " + lineNumber + " of " + sourceId);
            }
        });

        String htmlEditor = getHtmlFromFile("android-editor.html");

        mWebView.addJavascriptInterface(new JsCallbackHandler(), JS_CALLBACK_HANDLER);

        mWebView.loadDataWithBaseURL("file:///android_asset/", htmlEditor, "text/html", "utf-8", "");

        enableWebDebugging(true);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.bold) {
            execJavaScriptFromString("ZSSEditor.setBold();");

            // Disable suggestions when bold (testing code only, needs to actually be context-aware, only hiding
            // suggestions if formatting is being applied inside a word (e.g. sometext_bold_ or _allbold_notbold)
            if (mBoldButton.isChecked()) {
                mWebView.hideSuggestions(true);
            } else {
                mWebView.hideSuggestions(false);
            }

            InputMethodManager inputMethodManager = (InputMethodManager) getActivity().
                    getSystemService(Context.INPUT_METHOD_SERVICE);
            inputMethodManager.restartInput(mWebView);
            // Clearing the anchored text seems to allow in-word bolding for custom keyboards (e.g. Swype),
            // but this needs more testing. inputMethodManager.updateSelection should be called every time an input is
            // made while (absorb callback-input) the current word has formatting, and updated with the current
            // x-position of the cursor
            inputMethodManager.updateSelection(mWebView, 20, 20, 20, 20);
            // Keeps the keyboard in place on API 18
            inputMethodManager.showSoftInput(mWebView, 0);
        }
    };

    private String getStringFromAsset(String filename) throws IOException {
        if (!isAdded()) {
            return null;
        }
        AssetManager assetManager = getActivity().getAssets();
        InputStream in = assetManager.open(filename);
        InputStreamReader is = new InputStreamReader(in);
        StringBuilder sb = new StringBuilder();
        BufferedReader br = new BufferedReader(is);
        String read = br.readLine();
        while (read != null) {
            sb.append(read);
            sb.append('\n');
            read = br.readLine();
        }
        return sb.toString();
    }

    private String getHtmlFromFile(String filename) {
        try {
            return getStringFromAsset(filename);
        } catch (IOException e) {
            AppLog.e(T.EDITOR, e.getMessage());
            return null;
        }
    }

    @SuppressLint("NewApi")
    private void enableWebDebugging(boolean enable) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            AppLog.i(T.EDITOR, "Enabling web debugging");
            WebView.setWebContentsDebuggingEnabled(enable);
        }
    }

    @Override
    public void setTitle(CharSequence text) {
        // TODO
    }

    @Override
    public void setContent(CharSequence text) {
        // TODO
    }

    @Override
    public CharSequence getTitle() {
        // TODO
        return null;
    }

    @Override
    public CharSequence getContent() {
        // TODO
        return null;
    }

    @Override
    public void appendMediaFile(MediaFile mediaFile, String imageUrl, ImageLoader imageLoader) {
        // TODO
    }

    @Override
    public void appendGallery(MediaGallery mediaGallery) {
        // TODO
    }

    @Override
    public Spanned getSpannedContent() {
        return null;
    }

    private void execJavaScriptFromString(String javaScript) {

        mWebView.loadUrl("javascript:" + javaScript);
    }

    class JsCallbackHandler {
        private final String CALLBACK_LOG = "callback-log";

        private final String CALLBACK_DOM_LOADED = "callback-dom-loaded";

        private final String CALLBACK_SELECTION_STYLE = "callback-selection-style";

        @JavascriptInterface
        public void executeCallback(final String callbackId, final String params) {
            switch (callbackId) {
                case CALLBACK_DOM_LOADED:
                    // Run on UI thread
                    mWebView.post(new Runnable() {
                        public void run() {
                            String title = "I'm editing a post!";
                            String contentHtml = getHtmlFromFile("example-content.html");

                            // Load example content into editor
                            execJavaScriptFromString("ZSSEditor.getField('zss_field_title').setHTML('" +
                                    Utils.escapeHtml(title) + "');");
                            execJavaScriptFromString("ZSSEditor.getField('zss_field_content').setHTML('" +
                                    Utils.escapeHtml(contentHtml) + "');");
                        }
                    });
                    break;
                case CALLBACK_SELECTION_STYLE:
                    mWebView.post(new Runnable() {
                        public void run() {
                            boolean bold = false;
                            String[] paramsArray = params.split("~");
                            for (String param : paramsArray) {
                                if (param.equals("bold")) {
                                    mBoldButton.setChecked(true);
                                    bold = true;
                                }
                            }
                            if (!bold) {
                                mBoldButton.setChecked(false);
                            }
                        }
                    });
                    break;
                case CALLBACK_LOG:
                    AppLog.d(T.EDITOR, callbackId + ": " + params.substring(4));
                    break;
                default:
                    AppLog.d(T.EDITOR, "unhandled callback: " + callbackId + ":" + params);
            }
        }
    }
}