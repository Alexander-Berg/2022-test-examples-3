package ru.yandex.market.gutgin.tms.service;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import ru.yandex.passport.tvmauth.TvmClient;
import ru.yandex.market.gutgin.tms.utils.DummyTvmClient;

@Ignore // Run once
public class YaDiskServiceTest {
    private static final String USER_AGENT = "partner-content";
    private static final String YA_DISK_SERVICE_URL =
        "https://cloud-api.yandex.net:443/v1/disk/public/resources/download";
    private static final int SOCKET_TIMEOUT_SECONDS = 5;
    private static final int CONNECT_TIMEOUT_SECONDS = 5;
    private static final int MAX_REDIRECTS = 3;
    private static final int RETRY_COUNT = 2;
    private static final boolean REQUEST_SENT_RETRY_ENABLED = true;

    @Test
    public void convertUrlToFileDownload() throws Exception {
        YaDiskService yaDiskService = new YaDiskService(
            YA_DISK_SERVICE_URL,
            USER_AGENT,
            SOCKET_TIMEOUT_SECONDS,
            CONNECT_TIMEOUT_SECONDS,
            MAX_REDIRECTS,
            RETRY_COUNT,
            REQUEST_SENT_RETRY_ENABLED,
            getTvmClient()
        );
        String directUrlYadisk = yaDiskService.convertUrlToFileDownloadIfNeed("https://yadi.sk/i/a1nj-D7XLFnHcw");
        Assert.assertTrue(directUrlYadisk.startsWith("https://downloader.disk.yandex.ru/disk/"));

        String directUrlYandexDisk = yaDiskService.convertUrlToFileDownloadIfNeed("https://yadi.sk/i/a1nj-D7XLFnHcw");
        Assert.assertTrue(directUrlYandexDisk.startsWith("https://downloader.disk.yandex.ru/disk/"));

        String urlWithPath = yaDiskService.convertUrlToFileDownloadIfNeed(
            "https://yadi.sk/d/Yavdez8VR27WS/2014-04-30%2014-13-02.JPG"
        );
        Assert.assertTrue(urlWithPath.startsWith("https://downloader.disk.yandex.ru/disk/"));
    }

    private TvmClient getTvmClient() {
        return new DummyTvmClient();
    }
}
