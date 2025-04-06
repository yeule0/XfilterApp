# Xfilter - Lightweight X/Twitter Android Wrapper

A lightweight Android application designed to provide a simpler, potentially faster experience for browsing X (formerly Twitter) by wrapping its mobile website within a WebView. Includes features to filter content and customize the view.

This app was primarily built as an alternative for older or lower-end Android devices where the official X app might feel sluggish or consume too many resources.

## Features

*   **Ad Filtering:** Attempts to hide promoted posts/ads within the timeline.
*   **IRC Mode:** A text-only mode that hides images, videos, profile pictures (mostly!), and link preview cards for a very streamlined, low-bandwidth, and potentially faster experience.
*   **Flag Filtering:** Hide tweets from users who have specific flag emojis in their display name.
*   **Word Filtering:** Hide tweets from users who have specific keywords in their display name.
*   Simple, native settings interface accessible via a floating button.

## Motivation

The official X app can sometimes be resource-intensive. This project aims to offer a focused alternative by leveraging the mobile website directly inside a WebView component and layering useful filtering capabilities on top using injected JavaScript.

## Setup & Building

1.  **Prerequisites:**
    *   Android Studio (latest stable version recommended)
    *   Android SDK Platform corresponding to the `compileSdk` in `app/build.gradle.kts`
    *   Git (to clone the repository)

2.  **Clone the Repository:**
    ```bash
    git clone https://github.com/yeule0/XfilterApp.git
    cd XfilterApp
    ```
    

3.  **Open in Android Studio:**
    *   Launch Android Studio.
    *   Select "Open" or "Open an Existing Project".
    *   Navigate to and select the cloned project directory.
    *   Allow Gradle to sync and build the project structure.

4.  **CRITICAL STEP: Update JavaScript Selectors**

    This app injects JavaScript into the X mobile website to perform filtering. Because X frequently changes its website code, the **CSS selectors used to identify elements (especially Ads and the main timeline) will break over time.**

    **Ad Filtering and the Timeline Observer WILL NOT WORK reliably without updating these selectors!(IT's Updated for now)**


5.  **Build the APK:**
    *   Go to **Build** -> **Build Bundle(s) / APK(s)** -> **Build APK(s)**.
    *   Android Studio will build the debug APK. You can find it in the `app/build/outputs/apk/debug/` directory.
    *   Install this `app-debug.apk` file on your Android device.

## Usage

Once built and installed (and after updating the necessary selectors!), simply open the "Xfilter" app. It will load the mobile X/Twitter website.

*   Browse as usual.
*   Tap the floating settings button (bottom-right) to configure filters (Ad Blocking, IRC Mode, Flags, Words).
*   Tap "Save and Apply" in settings. Changes should take effect shortly on the timeline.

## Known Issues & Limitations

*   **Selector Fragility:** The app's core filtering relies heavily on CSS selectors found within the X mobile website's HTML structure. These selectors **will break** when X changes its website design, requiring manual inspection and code updates (as described in Setup Step 4). Ad filtering is particularly prone to breaking.
*   **Performance:** While designed to be lightweight, performance on very low-end devices still depends significantly on the device's hardware capabilities, available RAM, and the complexity of the X website itself. You may still experience lag or occasional blank screens (WebView renderer crashing due to memory pressure) during heavy scrolling, although hopefully less frequently than with the official app, especially with IRC mode enabled.
*   **IRC Mode:** The profile picture hiding in IRC mode may not always work reliably due to the specific CSS structure used by X.
*   **Website Dependency:** The app is fundamentally a wrapper. If X makes drastic changes to its mobile site functionality or login process, the app could stop working entirely.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Contact

For questions, suggestions, or issues, please open an issue on this repository or contact me on [X (Twitter)](https://twitter.com/yeule0).
