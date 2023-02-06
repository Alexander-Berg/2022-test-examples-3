package ru.yandex.market.tsum.pipe.ui.common;

import java.net.URI;

/**
 * @author Alexander Kedrik <a href="mailto:alkedr@yandex-team.ru"></a>
 * @date 16.02.2018
 */
public class TsumUrls {
    private static final URI TSUM_URI = URI.create(TsumPipeUiTestsProperties.TSUM_URL);

    public static String mainPage() {
        return page("/");
    }

    public static String projectPage(String projectName) {
        return page(String.format("/pipe/projects/%s", projectName));
    }

    public static String projectMultitestingsPage(String projectName) {
        return page(String.format("/pipe/projects/%s/multitestings/environments", projectName));
    }

    public static String timelinePage() {
        return page("/timeline");
    }

    private static String page(String path) {
        return TSUM_URI.resolve(path).toString();
    }
}
