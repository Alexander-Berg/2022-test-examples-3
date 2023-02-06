package ru.yandex.market.logshatter.useragent;

import com.google.common.collect.ImmutableMap;

import java.util.Map;

public class FakeUserAgentDetector implements UserAgentDetector {
    private Map<String, String> detectionResult;

    public FakeUserAgentDetector() {
        this.detectionResult = ImmutableMap.<String, String>builder()
            .put(UserAgentDetector.BROWSER_BASE, "Chromium")
            .put(UserAgentDetector.BROWSER_BASE_VERSION, "68.0.3440.106")
            .put(UserAgentDetector.BROWSER_ENGINE, "WebKit")
            .put(UserAgentDetector.BROWSER_ENGINE_VERSION, "537.36")
            .put(UserAgentDetector.BROWSER_NAME, "YandexBrowser")
            .put(UserAgentDetector.BROWSER_VERSION, "18.9.0.3363")
            .put("CSP1Support", "true")
            .put("CSP2Support", "true")
            .put(UserAgentDetector.OS_FAMILY, "MacOS")
            .put(UserAgentDetector.OS_NAME, "Mac OS X Sierra")
            .put(UserAgentDetector.OS_VERSION, "10.12.6")
            .put("SVGSupport", "true")
            .put("WebPSupport", "true")
            .put("YaGUI", "2.5")
            .put("historySupport", "true")
            .put("isBrowser", "true")
            .put(UserAgentDetector.IS_MOBILE, "false")
            .put("isTouch", "false")
            .put("localStorageSupport", "true")
            .put("postMessageSupport", "true")
            .build();
    }

    public void setDetectionResult(Map<String, String> detectionResult) {
        this.detectionResult = detectionResult;
    }

    @Override
    public Map<String, String> detect(String ua) {
        return detectionResult;
    }
}
