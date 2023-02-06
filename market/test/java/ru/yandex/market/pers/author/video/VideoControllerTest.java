package ru.yandex.market.pers.author.video;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import org.json.JSONException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pers.author.PersAuthorTest;
import ru.yandex.market.pers.author.client.api.dto.pager.DtoPager;
import ru.yandex.market.pers.author.client.api.model.VideoModState;
import ru.yandex.market.pers.author.mock.mvc.VideoMvcMocks;
import ru.yandex.market.pers.author.client.api.dto.VideoInfoDto;
import ru.yandex.market.pers.author.video.modState.VhModState;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author varvara
 * 27.01.2020
 */
public class VideoControllerTest extends PersAuthorTest {

    @Autowired
    protected VideoMvcMocks videoMvcMocks;

    @Autowired
    protected VideoService videoService;

    protected static final long MODEL_ID = 12463467;
    protected static final long UID = 251357;
    protected static final long SKU = 3222233L;
    protected static final String YANDEX_UID = "yandexuid1111";
    protected static final String TITLE = "Это тайтл к видео про ничего";
    protected static final String VIDEO_ID = "video_id_111";
    protected static final Instant CR_TIME = Instant.ofEpochSecond(1586181189);
    protected static final long MODERATOR_ID = UID + 34567;

    protected String CALLBACK_TEMPLATE;
    protected String SAVE_BODY_TEMPLATE;
    protected String EMPTY_PAGER_JSON;

    @BeforeEach
    public void init() throws IOException {
        setUp();
        CALLBACK_TEMPLATE = fileToString("/data/callback_template.json");
        SAVE_BODY_TEMPLATE = fileToString("/data/example_video_create_request_template.json");
        EMPTY_PAGER_JSON = fileToString("/data/get_videos_empty_result_paging.json");
    }

    protected void saveVideo(String videoId, long modelId, long uid) throws Exception {
        String body = String.format(SAVE_BODY_TEMPLATE, modelId, videoId);
        videoMvcMocks.saveVideoByUid(body, uid, status().is2xxSuccessful());
    }

    protected void callbackReady(String videoId) throws Exception {
        String body = getCallbackBody(videoId, VhModState.READY);
        videoMvcMocks.callback(body, status().is2xxSuccessful());
    }

    protected String getCallbackBody(String videoId, VhModState vhModState) throws IOException {
        String callbackTemplate = CALLBACK_TEMPLATE;
        return String.format(callbackTemplate, videoId, vhModState.getName());
    }

    protected String getCallbackMeta() throws IOException {
        return fileToString("/data/meta_info.json");
    }

    protected void markVideoAsReadyByVh(String videoId) throws Exception {
        String body = getCallbackBody(videoId, VhModState.READY);
        videoMvcMocks.callback(body, status().is2xxSuccessful());
    }

    protected void checkMetaInfo(String videoId, String metaInfo) throws JSONException {
        final List<String> metaResult = jdbcTemplate.queryForList("select meta_info from pers.video where video_id " +
            "= ?", String.class, videoId);
        assertEquals(metaResult.size(), 1);
        JSONAssert.assertEquals(metaInfo, metaResult.get(0), true);
    }

    protected void checkModState(String videoId, VideoModState modState) {
        final List<Long> states = jdbcTemplate.queryForList("select mod_state from pers.video where video_id = ?",
            Long.class, videoId);
        assertEquals(states.size(), 1);
        assertEquals(states.get(0), modState.getValue());
    }

    protected void checkEquals(DtoPager<VideoInfoDto> dtos, List<Long> expectedIds) {
        List<Long> result = dtos.getData().stream().map(VideoInfoDto::getId).collect(Collectors.toList());
        Assertions.assertEquals(expectedIds, result);
    }

    protected void checkLastVhModState(String videoId, VhModState vhModState) {
        final List<Long> states = jdbcTemplate.queryForList("select vh_mod_state from pers.last_mod_state where " +
            "video_id = ?", Long.class, videoId);
        assertEquals(states.size(), 1);
        assertEquals(states.get(0), vhModState.getValue());
    }

    protected Long getId(String videoId) {
        return jdbcTemplate.queryForObject("select id from pers.video where video_id = ?",
            Long.class, videoId);
    }

    protected void updateVideoCrTime(String videoId, Instant time) {
        jdbcTemplate.update("update pers.video set cr_time = ? where video_id = ?",
            new java.sql.Timestamp(time.toEpochMilli()), videoId);
    }

    protected Instant getCrTime(String videoId) {
        java.sql.Timestamp crTime = jdbcTemplate.queryForObject("select cr_time from  pers.video where video_id = ?", java.sql.Timestamp.class,
            videoId);
        return crTime.toInstant();
    }
}
