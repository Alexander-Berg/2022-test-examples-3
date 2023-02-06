package ru.yandex.market.pers.author.video;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pers.author.client.api.dto.VideoInfoDto;
import ru.yandex.market.pers.author.security.AuthorEntityType;
import ru.yandex.market.pers.author.security.SecurityData;
import ru.yandex.market.pers.author.security.SecurityService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class VideoControllerSaveTest extends VideoControllerTest {

    private static final String SOURCE = "source";

    @Autowired
    private SecurityService securityService;

    @Test
    public void testSaveVideoAndGetResult() throws Exception {
        String result = videoMvcMocks.saveVideoByUid(VIDEO_ID, TITLE, MODEL_ID, UID, status().is2xxSuccessful());

        long id = getId(VIDEO_ID);
        Instant crTime = getCrTime(VIDEO_ID);
        String resultExpected = String.format(fileToString("/data/example_video_save_result.json"), id,
            crTime.toEpochMilli());
        JSONAssert.assertEquals(resultExpected, result, true);

        List<String> videoIds = jdbcTemplate.queryForList("select video_id from pers.video", String.class);
        assertEquals(1, videoIds.size());
        assertEquals(VIDEO_ID, videoIds.get(0));
    }

    @Test
    public void testSaveVideoWithSourceAndAddThisSource() {
        VideoInfoDto videoInfoDto = videoMvcMocks.saveVideoByUidWithSource(VIDEO_ID, MODEL_ID, UID, SOURCE);

        SecurityData securityData = getSecurityData(videoInfoDto);
        assertNotNull(securityData);
        assertEquals(SOURCE, securityData.getSource());
    }

    @Test
    public void testSaveVideoSavedBefore() {
        VideoInfoDto videoInfoDto = videoMvcMocks.saveVideoByUidWithSource(VIDEO_ID, MODEL_ID, UID, SOURCE);
        VideoInfoDto videoInfoDtoOther = videoMvcMocks.saveVideoByUidWithSource(VIDEO_ID, MODEL_ID, UID, SOURCE);
        //Don't save the same video
        assertEquals(videoInfoDto.getId(), videoInfoDtoOther.getId());

        SecurityData securityData = getSecurityData(videoInfoDto);
        SecurityData securityData2 = getSecurityData(videoInfoDtoOther);
        //Don't save the same video and same security
        assertEquals(securityData.getId(), securityData2.getId());
    }

    @Test
    public void testSaveVideoWithIp() {
        VideoInfoDto videoInfoDto = videoMvcMocks.saveVideoByUidWithIp(VIDEO_ID, MODEL_ID, UID,
            "123.123.123.123", null);

        SecurityData securityData = getSecurityData(videoInfoDto);
        assertNotNull(securityData);
        assertEquals("123.123.123.123", securityData.getIp());
        assertNull(securityData.getPort());
    }

    @Test
    public void testSaveVideoWithIpAndPort() {
        VideoInfoDto videoInfoDto = videoMvcMocks.saveVideoByUidWithIp(VIDEO_ID, MODEL_ID, UID,
            "123.123.123.123", 5424);

        SecurityData securityData = getSecurityData(videoInfoDto);
        assertNotNull(securityData);
        assertEquals("123.123.123.123", securityData.getIp());
        assertEquals(5424, securityData.getPort());
    }

    private SecurityData getSecurityData(VideoInfoDto videoInfoDto) {
        return securityService.getSecurityData(AuthorEntityType.VIDEO, videoInfoDto.getId());
    }

    @Test
    public void testSaveVideoWithEmptyTitle() throws Exception {
        String bodyWithEmptyTitle = String.format("{\n" +
                "    \"modelId\" : %s,\n" +
                "    \"videoId\" : \"%s\",\n" +
                "    \"title\" : \"%s\"\n" +
                "  }",
            MODEL_ID, VIDEO_ID, "");
        videoMvcMocks.saveVideoByUid(bodyWithEmptyTitle, UID, status().is2xxSuccessful());
    }

    @Test
    public void testSaveVideo4xxWithoutVideoId() throws Exception {
        String bodyWithoutVideoId = String.format("{\n" +
                "    \"modelId\" : %s,\n" +
                "    \"title\" : \"%s\"\n" +
                "  }",
            MODEL_ID, TITLE);
        videoMvcMocks.saveVideoByUid(bodyWithoutVideoId, UID, status().is4xxClientError());
    }

    @Test
    public void testSaveVideo4xxWithoutModelId() throws Exception {
        String bodyWithoutModelId = String.format("{\n" +
                "    \"videoId\" : \"%s\",\n" +
                "    \"title\" : \"%s\"\n" +
                "  }",
            VIDEO_ID, TITLE);
        videoMvcMocks.saveVideoByUid(bodyWithoutModelId, UID, status().is4xxClientError());
    }

    @Test
    public void test2xxWithoutTitle() throws Exception {
        String bodyWithoutTitle = String.format("{\n" +
                "    \"videoId\" : \"%s\",\n" +
                "    \"modelId\" : %s\n" +
                "  }",
            VIDEO_ID, MODEL_ID);
        videoMvcMocks.saveVideoByUid(bodyWithoutTitle, UID, status().is2xxSuccessful());
    }

    @Test
    public void test2xxWithSku() throws Exception {
        String bodyWithoutTitle = String.format("{\n" +
                "    \"videoId\" : \"%s\",\n" +
                "    \"modelId\" : %s,\n" +
                "    \"sku\" : %s\n" +
                "  }",
            VIDEO_ID, MODEL_ID, SKU);
        videoMvcMocks.saveVideoByUid(bodyWithoutTitle, UID, status().is2xxSuccessful());
    }

}
