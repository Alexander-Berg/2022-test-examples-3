package ru.yandex.market.mbo.videoreview;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.mbo.gwt.models.videoreview.VideoReviewManual;

public class VideoReviewFindServiceTest {

    private static final String URL_PREFIX = "urlPrefix";

    private static final String VIDEO_URL = "http://my.url?param=value";

    private static final String EXPECTED_VIDEO_URL = "urlPrefix?url=http%3A%2F%2Fmy.url%3Fparam%3Dvalue";
    private static final String EXPECTED_CREATED = "created";
    private static final Integer EXPECTED_DURATION = 365;
    private static final String EXPECTED_HOSTING = "www.youtube.com";
    private static final String EXPECTED_IMAGE_LINK = "image.link";
    private static final String EXPECTED_PLAYER = "<iframe src=\"player\"\\>";
    private static final String EXPECTED_PLAYER_AUTO = "<iframe src=\"player_auto\"\\>";
    private static final String EXPECTED_TITLE = "title";
    private static final String EXPECTED_JSON_URL = "https://video.url";
    private static final String EXPECTED_VTHUMB = "vthumb";
    private static final String EXPECTED_THUMBNAIL_LINK = "http://image.link";
    private static final String EXPECTED_THUMBNAIL_LINK_EXISTING = "http://thumbnail.link";

    private static final String JSON_SUCCESS = "{\n" +
        "  \"data\":{\n" +
        "    \"created\":\"created\",\n" +
        "    \"duration\": \"365\",\n" +
        "    \"hosting\": \"www.youtube.com\",\n" +
        "    \"image_link\": \"image.link\",\n" +
        "    \"player\": \"<iframe src=\\\"player\\\"\\\\>\",\n" +
        "    \"player_auto\": \"<iframe src=\\\"player_auto\\\"\\\\>\",\n" +
        "    \"title\": \"title\",\n" +
        "    \"url\": \"https://video.url\",\n" +
        "    \"vthumb\": \"vthumb\",\n" +
        "    \"thumbnail_link\": \"\"\n" +
        "  },\n" +
        "  \"success\":true,\n" +
        "  \"error\":\"\"\n" +
        "}";

    private static final String JSON_SUCCESS_WITH_THUMBNAIL_LINK = "{\n" +
        "  \"data\":{\n" +
        "    \"created\":\"created\",\n" +
        "    \"duration\": \"365\",\n" +
        "    \"hosting\": \"www.youtube.com\",\n" +
        "    \"image_link\": \"image.link\",\n" +
        "    \"player\": \"<iframe src=\\\"player\\\"\\\\>\",\n" +
        "    \"player_auto\": \"<iframe src=\\\"player_auto\\\"\\\\>\",\n" +
        "    \"title\": \"title\",\n" +
        "    \"url\": \"https://video.url\",\n" +
        "    \"vthumb\": \"vthumb\",\n" +
        "    \"thumbnail_link\": \"http://thumbnail.link\"\n" +
        "  },\n" +
        "  \"success\":true,\n" +
        "  \"error\":\"\"\n" +
        "}";

    private static final String JSON_UNSUCCESS = "{\n" +
        "  \"success\":false,\n" +
        "  \"error\":\"error find video\"\n" +
        "}";

    private VideoReviewFindService videoReviewFindService;

    @Before
    public void prepare() {
        videoReviewFindService = new VideoReviewFindService();
    }

    @Test
    public void testBuildUrl() {
        videoReviewFindService.setFindVideoUrl(URL_PREFIX);
        String builtUrl = videoReviewFindService.buildUrl(VIDEO_URL);
        Assert.assertEquals(EXPECTED_VIDEO_URL, builtUrl);
    }

    @Test
    public void testParseResponseSuccessWithoutThumbnailLink() {
        VideoReviewManual reviewManual = videoReviewFindService.parseResponse(JSON_SUCCESS);
        assertAllWithoutThumbnailLink(reviewManual);
        Assert.assertEquals(EXPECTED_THUMBNAIL_LINK, reviewManual.getThumbnailLink());
    }

    @Test
    public void testParseResponseSuccess() {
        VideoReviewManual reviewManual = videoReviewFindService.parseResponse(JSON_SUCCESS_WITH_THUMBNAIL_LINK);
        assertAllWithoutThumbnailLink(reviewManual);
        Assert.assertEquals(EXPECTED_THUMBNAIL_LINK_EXISTING, reviewManual.getThumbnailLink());
    }

    private void assertAllWithoutThumbnailLink(VideoReviewManual reviewManual) {
        Assert.assertEquals(EXPECTED_CREATED, reviewManual.getCreated());
        Assert.assertEquals(EXPECTED_DURATION, reviewManual.getDuration());
        Assert.assertEquals(EXPECTED_HOSTING, reviewManual.getHosting());
        Assert.assertEquals(EXPECTED_IMAGE_LINK, reviewManual.getImageLink());
        Assert.assertEquals(EXPECTED_PLAYER, reviewManual.getPlayer());
        Assert.assertEquals(EXPECTED_PLAYER_AUTO, reviewManual.getPlayerAuto());
        Assert.assertEquals(EXPECTED_TITLE, reviewManual.getTitle());
        Assert.assertEquals(EXPECTED_JSON_URL, reviewManual.getUrl());
        Assert.assertEquals(EXPECTED_VTHUMB, reviewManual.getVthumb());
    }

    @Test()
    public void testParseResponseFail() {
        VideoReviewManual reviewManual = videoReviewFindService.parseResponse(JSON_UNSUCCESS);
        Assert.assertNull(reviewManual);
    }
}
