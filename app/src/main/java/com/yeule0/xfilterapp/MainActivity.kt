package com.yeule0.xfilterapp

import androidx.core.view.WindowCompat
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.widget.ImageButton
import android.widget.ProgressBar
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray
import org.json.JSONObject

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar
    private lateinit var settingsButton: ImageButton // Changed from FloatingActionButton
    private var siteUrl = "https://mobile.twitter.com" // Or mobile.x.com
    private var isWebViewLoaded = false

    private val settingsResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        Log.d("MainActivity", "Returned from settings, result code: ${result.resultCode}")
        if (result.resultCode == RESULT_OK) {
            Log.d("MainActivity", "Applying settings after return from SettingsActivity.")
            applySettingsToWebView()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webView)
        progressBar = findViewById(R.id.progressBar)
        setupWebView()

        settingsButton = findViewById(R.id.buttonSettingsEdge) // Updated ID and type
        settingsButton.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            settingsResultLauncher.launch(intent)
        }

        if (savedInstanceState == null) {
            Log.d("MainActivity", "onCreate: Loading initial URL: $siteUrl")
            webView.loadUrl(siteUrl)
        } else {
            Log.d("MainActivity", "onCreate: Restoring WebView state.")
            webView.restoreState(savedInstanceState)
            isWebViewLoaded = savedInstanceState.getBoolean("webViewLoaded", false)

            if (isWebViewLoaded) {
                applySettingsToWebView()
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.databaseEnabled = true
        webView.settings.loadWithOverviewMode = true
        webView.settings.useWideViewPort = true
        webView.settings.userAgentString = webView.settings.userAgentString.replace("; wv", "").replace("Version\\/\\d+\\.\\d+", "")

        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null)

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                progressBar.progress = newProgress
                if (newProgress < 100 && progressBar.visibility == View.GONE) {
                    progressBar.visibility = View.VISIBLE
                }
                if (newProgress == 100) {
                    progressBar.visibility = View.GONE
                }
            }
            override fun onConsoleMessage(message: android.webkit.ConsoleMessage): Boolean {
                Log.d("WebViewConsole", "${message.message()} -- From line ${message.lineNumber()} of ${message.sourceId()}")
                return true
            }
        }

        webView.webViewClient = XFilterWebViewClient { loadedWebView ->
            Log.i("MainActivity", "WebViewClient finished, applying settings.")
            applySettingsToWebView()
            isWebViewLoaded = true
        }
    }

    private fun applySettingsToWebView() {
        val settings = SettingsManager.getAllSettings(this)
        Log.d("MainActivity", "Applying settings: Flags=${settings.flagsToHide.size}, Words=${settings.wordsToHide.size}, Ads=${settings.filterAds}, IRC=${settings.ircMode}")
        injectJavascript(settings)
    }

    private fun injectJavascript(settings: SettingsManager.FilterSettings) {
        val flagsJson = JSONArray(settings.flagsToHide).toString()
        val wordsJson = JSONArray(settings.wordsToHide).toString()

        val debounceDelayMs = 300

        val script = """
        javascript:(function() {
            const XFILTER_PREFIX = 'xfilter-mobile';
            const DEBOUNCE_DELAY = ${debounceDelayMs}; 
            let currentSettings = {};

            const SELECTORS = {
                 adIndicatorContainer: '',
                 adIndicatorText:      'span.css-1jxf684.r-bcqeeo',
                 tweet:                  '[data-testid="tweet"]', 
                 tweetUsernameWrapper:   '[data-testid="UserName"]',
                 tweetDisplayname:       '[data-testid="UserName"] span',
                 tweetUserAvatarContainer: '.css-175oi2r.r-18kxxzh.r-1wron08.r-onrtq4.r-1awozwy',
                 tweetPhoto:             '[data-testid="tweetPhoto"], [aria-label="Image"], [data-testid="testCondensedMedia"]',
                 tweetCardImage:         '[data-testid="article-cover-image"], [data-testid="card.layoutSmall.media"], [data-testid="card.layoutLarge.media"], a[href*="photo"] > div, [style*="padding-bottom: 56.25%"]',
                 tweetVideo:             '[data-testid="videoPlayer"]',
                 verifiedBadge:          'div[data-testid="User-Name"] svg[aria-label="Verified account"]',
                 otherBadges:            'div[data-testid="User-Name"] img:not([src*="profile_images"])'
             };

            function log(...args) { console.log("XFilter:", ...args); }
            function warn(...args) { console.warn("XFilter:", ...args); }
            function error(...args) { console.error("XFilter:", ...args); }

            function ensureStyleTag(id) {
                let tag = document.getElementById(id);
                if (!tag) { tag = document.createElement('style'); tag.id = id; document.head.appendChild(tag); log('Created style tag #' + id); }
                return tag;
            }

            function debounce(func, wait) {
                let timeout;
                return function (...args) {
                    const context = this;
                    const later = () => { timeout = null; func.apply(context, args); };
                    clearTimeout(timeout);
                    timeout = setTimeout(later, wait);
                };
            }

            const ircStyleTag = ensureStyleTag('xfilter-irc-style');

            function filterSingleTweet(tweet) {
                if (tweet.hasAttribute('data-' + XFILTER_PREFIX + '-processed') || tweet.style.display === 'none') return;
                 tweet.setAttribute('data-' + XFILTER_PREFIX + '-processed', 'true');
                 let shouldHide = false; let reason = '';

                 if (!shouldHide && currentSettings.filterAds) {
                      if (SELECTORS.adIndicatorText) { 
                          try {
                              const adSpans = tweet.querySelectorAll(SELECTORS.adIndicatorText);
                              for (let adSpan of adSpans) {
                                  if (adSpan && (adSpan.textContent || "").trim() === 'Ad') {
                                      if (adSpan.offsetParent !== null) {
                                          shouldHide = true;
                                          reason = 'Ad Span Text';
                                          log("Ad detected via text:", tweet);
                                          break;
                                      }
                                  }
                              }
                          } catch(e) { error("Error during ad querySelectorAll:", e, "Selector:", SELECTORS.adIndicatorText); }
                      }
                      if (!shouldHide && tweet.matches('[aria-label*="Ad"]')) {
                           shouldHide = true; reason = 'Ad Aria Label';
                           log("Ad detected via aria-label:", tweet);
                      }
                  }

                 if (!shouldHide && (currentSettings.flagsToHide?.length > 0 || currentSettings.wordsToHide?.length > 0)) {
                      const userNameWrapper = tweet.querySelector(SELECTORS.tweetUsernameWrapper);
                      if (userNameWrapper) {
                          const displayNameElement = userNameWrapper.querySelector(SELECTORS.tweetDisplayname);
                          const nameToCheck = (displayNameElement?.textContent || userNameWrapper.textContent || '').toLowerCase().trim();
                          if (nameToCheck) {
                              if (currentSettings.flagsToHide.some(flag => flag && nameToCheck.includes(flag))) { shouldHide = true; reason = 'Flag in Name'; }
                              if (!shouldHide && currentSettings.wordsToHide.some(word => word && nameToCheck.includes(word.toLowerCase()))) { shouldHide = true; reason = 'Word in Name'; }
                          }
                      }
                 }

                 const isHiddenByUs = tweet.hasAttribute('data-' + XFILTER_PREFIX + '-hidden');
                 if (shouldHide) {
                     if (!isHiddenByUs) { tweet.style.display = 'none'; tweet.setAttribute('data-' + XFILTER_PREFIX + '-hidden', reason); }
                 } else {
                     if (isHiddenByUs) { tweet.style.display = ''; tweet.removeAttribute('data-' + XFILTER_PREFIX + '-hidden'); }
                 }
             }

             function runTweetFilters() {
                 let count = 0;
                 try {
                     const tweets = document.querySelectorAll(SELECTORS.tweet);
                     tweets.forEach(tweet => {
                         tweet.removeAttribute('data-' + XFILTER_PREFIX + '-processed');
                         filterSingleTweet(tweet); count++;
                     });
                 } catch (e) { error("Error during tweet filtering:", e); }
             }

             function applyIRCModeStyles() {
                 let css = '';
                 if (currentSettings.ircMode) {
                     const hideSelectors = [ SELECTORS.tweetUserAvatarContainer, SELECTORS.tweetPhoto, SELECTORS.tweetCardImage, SELECTORS.tweetVideo ].filter(Boolean).join(',\n');
                     if (hideSelectors) { css += hideSelectors + ' { display: none !important; }'; }
                     css += ' div[data-testid="User-Name"][data-irc-preserve] { display: inline-flex !important; align-items: center !important; visibility: visible !important; vertical-align: text-bottom; }';
                     css += ' div[data-testid="User-Name"][data-irc-preserve] ' + SELECTORS.verifiedBadge + ',';
                     css += ' div[data-testid="User-Name"][data-irc-preserve] ' + SELECTORS.otherBadges + ' { display: inline !important; visibility: visible !important; opacity: 1 !important; height: 1em; width: auto; }';
                 }
                 if (ircStyleTag.textContent !== css) { ircStyleTag.textContent = css; log("IRC Style Tag Updated.");}
             }

            function processPageChanges() {
                 runTweetFilters();
                 applyIRCModeStyles();
            }

            const debouncedProcessPage = debounce(processPageChanges, DEBOUNCE_DELAY);

            let observer = null;
            function startObserver() {
                if (observer) return;
                try {
                    const targetNode = document.body;
                    if (!targetNode) { error("Observer target (document.body) not found!"); return; }
                    const observerCallback = (mutationsList) => {
                       let relevantMutation = false;
                       for (const mutation of mutationsList) {
                           if (mutation.type === 'childList' && mutation.addedNodes.length > 0) {
                               for (const node of mutation.addedNodes) {
                                   if (node.nodeType === Node.ELEMENT_NODE && SELECTORS.tweet &&
                                       (node.matches?.(SELECTORS.tweet) || node.querySelector?.(SELECTORS.tweet))) {
                                       relevantMutation = true; break;
                                   }
                               }
                           }
                           if (relevantMutation) break;
                       }
                       if (relevantMutation) {
                            try { debouncedProcessPage(); } catch (e) { error("Error calling debouncedProcessPage:", e); }
                       }
                    };
                    observer = new MutationObserver(observerCallback);
                    observer.observe(targetNode, { childList: true, subtree: true });
                    log("Mutation observer started on document.body.");
                } catch (e) { error("Failed to start Mutation Observer:", e); }
            }
            function stopObserver() { if (observer) { observer.disconnect(); observer = null; log("Mutation observer stopped."); } }

            window.applyXFilterSettings = function(flags, words, filterAds, ircMode) {
                 const newSettings = { flagsToHide: flags || [], wordsToHide: words || [], filterAds: filterAds, ircMode: ircMode };
                 let settingsChanged = JSON.stringify(currentSettings) !== JSON.stringify(newSettings);
                 let filtersNeedReset = currentSettings.filterAds !== newSettings.filterAds || JSON.stringify(currentSettings.flagsToHide) !== JSON.stringify(newSettings.flagsToHide) || JSON.stringify(currentSettings.wordsToHide) !== JSON.stringify(newSettings.wordsToHide);
                 currentSettings = newSettings;

                 if (settingsChanged) {
                     log("Settings have changed. Reprocessing page.");
                     if (filtersNeedReset) { log("Filter settings changed, resetting hidden elements.");
                        document.querySelectorAll(SELECTORS.tweet + '[data-' + XFILTER_PREFIX + '-hidden]').forEach(tweet => { tweet.style.display = ''; tweet.removeAttribute('data-' + XFILTER_PREFIX + '-hidden'); });
                        document.querySelectorAll(SELECTORS.tweet).forEach(tweet => { tweet.removeAttribute('data-' + XFILTER_PREFIX + '-processed'); });
                     }
                     processPageChanges();
                 }
                 startObserver();
            };

             if (!window.xFilterInitialized) {
                 window.xFilterInitialized = true;
                 log("XFilter: Initializing script...");
                 window.applyXFilterSettings(${flagsJson}, ${wordsJson}, ${settings.filterAds}, ${settings.ircMode});
                 log("XFilter initialization complete.");
             } else {
                  log("XFilter: Script already initialized. Re-applying settings.");
                  window.applyXFilterSettings(${flagsJson}, ${wordsJson}, ${settings.filterAds}, ${settings.ircMode});
             }
        })();
        """.trimIndent()

        webView.post { webView.evaluateJavascript(script, null) }
    }

    override fun onBackPressed() { if (webView.canGoBack()) { webView.goBack() } else { super.onBackPressed() } }
    override fun onSaveInstanceState(outState: Bundle) { super.onSaveInstanceState(outState); webView.saveState(outState); outState.putBoolean("webViewLoaded", isWebViewLoaded); }
    override fun onDestroy() { webView.stopLoading(); webView.destroy(); Log.d("MainActivity", "onDestroy: WebView destroyed."); super.onDestroy(); }
}