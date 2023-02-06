package ru.yandex.market.api.partner.controllers.feed.model;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Assert;
import org.junit.Test;

@ParametersAreNonnullByDefault
public class FeedIndexLogErrorDTOTest {
    @Test
    public void testHttpError() {
        FeedIndexLogErrorDTO error = FeedIndexLogErrorDTO.createDownloadError("server status is 502; ERR: ");
        Assert.assertEquals(FeedIndexLogErrorTypeDTO.DOWNLOAD_HTTP_ERROR, error.getType());
        Assert.assertEquals(Integer.valueOf(502), error.getHttpStatusCode());
        Assert.assertNull(error.getDescription());
    }

    @Test
    public void testGenericNetworkError() {
        String downloadStatus = "server status is no status code;" +
                " ERR: errcode: 28, msg: Connection timed out after 60000 milliseconds";
        FeedIndexLogErrorDTO error = FeedIndexLogErrorDTO.createDownloadError(downloadStatus);
        Assert.assertEquals(FeedIndexLogErrorTypeDTO.DOWNLOAD_ERROR, error.getType());
        Assert.assertNull(error.getHttpStatusCode());
        Assert.assertEquals(downloadStatus, error.getDescription());
    }

    @Test
    public void testEmptyFeedError() {
        String downloadStatus = "downloaded feed is empty";
        FeedIndexLogErrorDTO error = FeedIndexLogErrorDTO.createDownloadError(downloadStatus);
        Assert.assertEquals(FeedIndexLogErrorTypeDTO.PARSE_ERROR, error.getType());
        Assert.assertNull(error.getHttpStatusCode());
        Assert.assertNull(error.getDescription());
    }
}
