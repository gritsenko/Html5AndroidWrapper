package com.example.hsgame

import LocalWebServer
import android.os.Bundle
import android.util.Log
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.example.hsgame.ui.theme.HSGameTheme
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.rewarded.RewardItem
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import jp.hituzi.kamome.Client
import jp.hituzi.kamome.Command

class MainActivity : ComponentActivity() {

    private var _client: Client? = null
    private lateinit var webServer: LocalWebServer
    private var rewardedAd: RewardedAd? = null
    private var isAdLoading = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        MobileAds.initialize(this) {}
        loadRewardedAd()
        // Start the local web server
        webServer = LocalWebServer(this)
        webServer.start()

        setContent {
            HSGameTheme {
                WebViewScreen("http://localhost:8080/index.html",
                    onWebViewCreated = { webView ->
                        // Now you have the WebView instance here
                        setupWebView(webView)
                    })
            }
        }
    }

    private fun loadRewardedAd() {
        val adRequest = AdRequest.Builder().build()

        RewardedAd.load(
            this,
            "ca-app-pub-3940256099942544/5224354917", // Use the test ad unit ID here
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    isAdLoading = false
                    Log.d("AdMob", "Ad loaded successfully")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewardedAd = null
                    isAdLoading = false
                    Log.e("AdMob", "Ad failed to load: ${error.code}, ${error.message}")
                }
            }
        )
        isAdLoading = true
    }

    private fun showRewardedAd() {
        rewardedAd?.show(this) { rewardItem: RewardItem ->
            // Handle reward
            val rewardAmount = rewardItem.amount
            val rewardType = rewardItem.type
            // You can notify your web app about the reward if needed

            val data = HashMap<String?, Any?>()
            _client?.send(data, "adShown") { commandName, result, error ->
                // Received a result from the JS code.
                //Log.d(TAG, "result: $result")
            }

        } ?: run {
            // Reload the ad if it's not loaded yet
            loadRewardedAd()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        webServer.stop()  // Stop the server when the activity is destroyed
    }

    private fun setupWebView(wv: WebView) {
        // Creates the Client object with the webView.
        val client = Client(wv)
        _client = client;
        // Registers `echo` command.
        client.add(Command("echo") { commandName, data, completion ->
            // Received `echo` command.
            // Then sends resolved result to the JavaScript callback function.
            val map = HashMap<String?, Any?>()
            map["message"] = data!!.optString("message")
            completion.resolve(map)
            // Or, sends rejected result if failed.
            //completion.reject("Echo Error!")
        })

        // Register the command "showAd" that the web app will send
        client.add(Command("showAd") { commandName, data, completion ->
            // Show the rewarded ad when the message is received
            showRewardedAd()

            // Send a response back to the web app (optional)
            val response = HashMap<String, Any?>()
            response["status"] = "ad_shown"
            completion.resolve(response)
        })
    }
}

@Composable
fun WebViewScreen(url: String, onWebViewCreated: (WebView) -> Unit) {

    val context = LocalContext.current
    // Create and remember a WebView instance
    val webView = remember { WebView(context) }
    onWebViewCreated(webView)

    // Set up the WebView (configure it only once)
    webView.apply {
        settings.javaScriptEnabled = true
        webViewClient = WebViewClient()
        loadUrl(url)
    }

    // Display the WebView in the Compose UI
    AndroidView(
        factory = { webView },
        modifier = Modifier.fillMaxSize()
    )
}