package ru.yandex.market.pers.qa.controller.api.post;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pers.qa.controller.QAControllerTest;
import ru.yandex.market.pers.qa.controller.dto.VideoDto;
import ru.yandex.market.pers.qa.controller.dto.post.PostV2Request;
import ru.yandex.market.pers.qa.mock.mvc.PostV2MvcMocks;
import ru.yandex.market.pers.qa.model.ModState;
import ru.yandex.market.pers.qa.model.QaEntityType;
import ru.yandex.market.pers.qa.model.post.PostV2;
import ru.yandex.market.pers.qa.model.post.Video;
import ru.yandex.market.pers.qa.service.post.PostV2Service;
import ru.yandex.market.pers.qa.service.post.PostVideoService;
import ru.yandex.market.pers.service.common.video.VideoHostingCallback;
import ru.yandex.market.pers.service.common.video.VideoMetaInfo;
import ru.yandex.market.pers.service.common.video.modstate.VHModState;
import ru.yandex.market.pers.service.common.video.modstate.VideoModState;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static ru.yandex.market.pers.qa.controller.api.post.PostV2ControllerTest.AUTHOR_ID;
import static ru.yandex.market.pers.qa.controller.api.post.PostV2ControllerTest.POST_V2_TEXT;
import static ru.yandex.market.pers.qa.controller.api.post.PostV2ControllerTest.P_UID;

/**
 * @author grigor-vlad
 * 13.07.2022
 */
public class PostV2VideoControllerTest extends QAControllerTest {
    private static final ObjectMapper OM = new ObjectMapper();

    @Autowired
    public PostV2MvcMocks postV2MvcMocks;
    @Autowired
    public PostVideoService postVideoService;
    @Autowired
    public PostV2Service postV2Service;

    @Test
    public void testReadyVideoCallbackOnExistedPost() throws Exception {
        String videoId = "video_id";
        VideoDto videoDto = new VideoDto(videoId);

        PostV2Request postRequest = new PostV2Request();
        postRequest.setText(POST_V2_TEXT);
        postRequest.setContent(List.of(videoDto));

        long postId = postV2MvcMocks.createBrandPostV2(postRequest, AUTHOR_ID, P_UID);

        //check video existence in table and empty meta_info
        List<Video> postVideosBefore = postVideoService.getVideoByEntity(QaEntityType.POST_V2, String.valueOf(postId));
        assertEquals(1, postVideosBefore.size());
        assertEquals(videoId, postVideosBefore.get(0).getVideoId());
        assertEquals(new VideoMetaInfo(), postVideosBefore.get(0).getMetaInfo());

        //callback request
        VideoHostingCallback callback = OM.readValue(
            fileToString("/data/post/vh_callback.json"), VideoHostingCallback.class);
        callback.setVideoId(videoId);
        callback.setStatus(VHModState.READY.getName());
        postV2MvcMocks.videoHostingCallback(callback);

        //check video meta_info update
        List<Video> postVideosAfter = postVideoService.getVideoByEntity(QaEntityType.POST_V2, String.valueOf(postId));
        assertEquals(1, postVideosAfter.size());
        assertEquals(videoId, postVideosAfter.get(0).getVideoId());
        assertEquals(new VideoMetaInfo(callback.getPlayerUrl(),
                callback.getShortId(),
                callback.getStreamUrl(),
                callback.getThumbnail(),
                callback.getDuration(),
                callback.getHeight(),
                callback.getWidth()),
            postVideosAfter.get(0).getMetaInfo());
    }

    @Test
    public void testReadyVideoCallbackOnNotExisted() throws Exception {
        String videoId = "video_id";
        //callback request
        VideoHostingCallback callback = OM.readValue(
            fileToString("/data/post/vh_callback.json"), VideoHostingCallback.class);
        callback.setVideoId(videoId);
        callback.setStatus(VHModState.READY.getName());
        postV2MvcMocks.videoHostingCallback(callback);

        //check video in table
        Video video = postVideoService.getVideoByVideoId(videoId);
        assertEquals(videoId, video.getVideoId());
        assertNull(video.getEntityId());
        assertEquals(new VideoMetaInfo(callback.getPlayerUrl(),
                callback.getShortId(),
                callback.getStreamUrl(),
                callback.getThumbnail(),
                callback.getDuration(),
                callback.getHeight(),
                callback.getWidth()),
            video.getMetaInfo());
    }

    @Test
    public void testModeratingCallback() throws Exception {
        String videoId = "video_id";
        VideoDto videoDto = new VideoDto(videoId);
        PostV2Request postRequest = new PostV2Request();
        postRequest.setText(POST_V2_TEXT);
        postRequest.setContent(List.of(videoDto));
        postV2MvcMocks.createBrandPostV2(postRequest, AUTHOR_ID, P_UID);

        //callback request
        VideoHostingCallback callback = OM.readValue(
            fileToString("/data/post/vh_callback.json"), VideoHostingCallback.class);
        callback.setVideoId(videoId);
        callback.setStatus(VHModState.MODERATING.getName());
        postV2MvcMocks.videoHostingCallback(callback);

        //check video meta_info update
        Video video = postVideoService.getVideoByVideoId(videoId);
        assertEquals(VideoModState.NEW, video.getModState());
        assertEquals(new VideoMetaInfo(), video.getMetaInfo());
    }

    @Test
    public void testBannedCallback() throws Exception {
        String videoId = "video_id";
        VideoDto videoDto = new VideoDto(videoId);
        PostV2Request postRequest = new PostV2Request();
        postRequest.setText(POST_V2_TEXT);
        postRequest.setContent(List.of(videoDto));
        long postId = postV2MvcMocks.createBrandPostV2(postRequest, AUTHOR_ID, P_UID);

        //callback request
        VideoHostingCallback callback = OM.readValue(
            fileToString("/data/post/vh_callback.json"), VideoHostingCallback.class);
        callback.setVideoId(videoId);
        callback.setStatus(VHModState.BANNED.getName());
        postV2MvcMocks.videoHostingCallback(callback);

        //check video mod_state
        Video video = postVideoService.getVideoByVideoId(videoId);
        assertEquals(VideoModState.AUTOMATICALLY_REJECTED, video.getModState());

        //check post mod_state
        PostV2 postById = postV2Service.getPostV2ByIdInternal(postId);
        assertEquals(ModState.AUTO_FILTER_REJECTED, postById.getModState());
    }

    @Test
    public void testBannedCallbackBeforePostCreation() throws Exception {
        String videoId = "video_id";
        //callback request
        VideoHostingCallback callback = OM.readValue(
            fileToString("/data/post/vh_callback.json"), VideoHostingCallback.class);
        callback.setVideoId(videoId);
        callback.setStatus(VHModState.READY.getName());
        postV2MvcMocks.videoHostingCallback(callback);

        callback.setStatus(VHModState.BANNED.getName());
        postV2MvcMocks.videoHostingCallback(callback);

        VideoDto videoDto = new VideoDto(videoId);
        PostV2Request postRequest = new PostV2Request();
        postRequest.setText(POST_V2_TEXT);
        postRequest.setContent(List.of(videoDto));
        long postId = postV2MvcMocks.createBrandPostV2(postRequest, AUTHOR_ID, P_UID);

        //check post
        PostV2 postById = postV2Service.getPostV2ByIdInternal(postId);
        assertEquals(ModState.AUTO_FILTER_REJECTED, postById.getModState());
    }

}
