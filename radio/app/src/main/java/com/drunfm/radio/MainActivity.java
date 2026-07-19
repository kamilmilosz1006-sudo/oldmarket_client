package com.drunfm.radio;

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends Activity {

    private WebView webView;
    private Button btnPlay;
    private TextView txtStatus;
    private boolean isLoaded = false;

    private static final String STREAM_URL = "https://www.youtube.com/embed/eZJx14UhktY?autoplay=1&playsinline=1";

    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_main);

        webView = (WebView) findViewById(R.id.webView);
        btnPlay = (Button) findViewById(R.id.btnPlay);
        txtStatus = (TextView) findViewById(R.id.txtStatus);

        setupWebView();

        btnPlay.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (!isLoaded) {
                    webView.loadUrl(STREAM_URL);
                    isLoaded = true;
                    btnPlay.setText("Pause");
                    txtStatus.setText("Loading stream...");
                } else {
                    webView.loadUrl("javascript:togglePlay()");
                    toggleButtonText();
                }
            }
        });
    }

    private void setupWebView() {
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(false);
        s.setDomStorageEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        if (Build.VERSION.SDK_INT >= 21) {
            s.setMixedContentMode(0);
        }

        webView.setWebViewClient(new WebViewClient() {
            public void onPageFinished(WebView view, String url) {
                isLoaded = true;
                btnPlay.setText("Pause");
                txtStatus.setText("Now playing");
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            public void onReceivedTitle(WebView view, String title) {
                if (title != null && title.contains("YouTube")) {
                    txtStatus.setText("Now playing");
                }
            }
        });

        webView.loadUrl(STREAM_URL);
        txtStatus.setText("Starting...");
    }

    private void toggleButtonText() {
        if (btnPlay.getText().equals("Play")) {
            btnPlay.setText("Pause");
            txtStatus.setText("Now playing");
        } else {
            btnPlay.setText("Play");
            txtStatus.setText("Paused");
        }
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && webView.canGoBack()) {
            webView.goBack();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    protected void onDestroy() {
        if (webView != null) {
            webView.destroy();
        }
        super.onDestroy();
    }
}
