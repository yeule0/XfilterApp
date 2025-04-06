package com.yeule0.xfilterapp

import android.graphics.Bitmap
import android.util.Log
import android.webkit.WebView
import android.webkit.WebViewClient

class XFilterWebViewClient(private val onPageFinishedCallback: (WebView) -> Unit) : WebViewClient() {

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        Log.d("XFilterWebViewClient", "Page started loading: $url")
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        Log.d("XFilterWebViewClient", "Page finished loading: $url")
        if (view != null) {
            // Delay slightly to allow dynamic content to potentially render
            view.postDelayed({
                Log.d("XFilterWebViewClient", "Executing onPageFinishedCallback after delay.")
                onPageFinishedCallback(view)
            }, 1000) // 1 second delay
        }
    }

    // Keep navigation within the WebView for Twitter/X domains
    @Deprecated("Deprecated in Java")
    override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
        val lowerCaseUrl = url?.lowercase() ?: ""
        Log.d("XFilterWebViewClient", "shouldOverrideUrlLoading: $url")

        return if (lowerCaseUrl.startsWith("http://") || lowerCaseUrl.startsWith("https://")) {
            if (lowerCaseUrl.contains("twitter.com") || lowerCaseUrl.contains("x.com")) {
                // Let the WebView handle loading twitter/x links internally
                false // Returning false means WebView handles it
            } else {
                // Load non-twitter links in webview too for simplicity
                false
            }
        } else {
            // Let system handle non-http links (mailto:, tel:, etc.)
            false
        }
    }
}