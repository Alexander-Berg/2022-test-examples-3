package ru.yandex.market.pers.author.video;

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import ru.yandex.market.pers.author.client.api.dto.pager.DtoList;
import ru.yandex.market.pers.author.client.api.dto.VideoInfoDto;
import ru.yandex.market.pers.author.client.api.model.VideoModState;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class VideoControllerDeleteTest extends VideoControllerTest {

    @Test
    public void testDeleteVideoBeforeReadyFromVh() throws Exception {
        String videoId = VIDEO_ID;
        videoMvcMocks.saveVideoByUid(videoId, TITLE, MODEL_ID, UID);
        long id = getId(videoId);
        DtoList<VideoInfoDto> videoInfo = videoMvcMocks.getVideoInfoDtosByUid(Arrays.asList(id), UID + 2);
        assertEquals(0, videoInfo.getData().size());

        videoMvcMocks.deleteVideo(videoId, UID, status().is2xxSuccessful());
        videoInfo = videoMvcMocks.getVideoInfoDtosByUid(Arrays.asList(id), UID + 2);
        assertEquals(0, videoInfo.getData().size());
    }
    @Test
    public void testDeleteVideoAfterReadyFromVh() throws Exception {
        String videoId = VIDEO_ID;
        videoMvcMocks.saveVideoByUid(videoId, TITLE, MODEL_ID, UID);
        long id = getId(videoId);
        markVideoAsReadyByVh(videoId);
        videoMvcMocks.publishVideo(id, status().is2xxSuccessful());
        DtoList<VideoInfoDto> videoInfo = videoMvcMocks.getVideoInfoDtosByUid(Arrays.asList(id), UID + 2);
        assertEquals(1, videoInfo.getData().size());

        videoMvcMocks.deleteVideo(videoId, UID, status().is2xxSuccessful());
        videoInfo = videoMvcMocks.getVideoInfoDtosByUid(Arrays.asList(id), UID + 2);
        assertEquals(0, videoInfo.getData().size());
    }

    @Test
    public void testDeleteNonExistVideo() throws Exception {
        videoMvcMocks.deleteVideo(VIDEO_ID, UID, status().is2xxSuccessful());
    }

    @Test
    public void testDeleteForeignVideo() throws Exception {
        String videoId = VIDEO_ID;
        videoMvcMocks.saveVideoByUid(videoId, TITLE, MODEL_ID, UID);
        videoMvcMocks.deleteVideo(VIDEO_ID, UID + 1, status().is4xxClientError());
    }

    // ---

    @Test
    public void testDeleteVideoByVideoIdBeforeReadyFromVh() throws Exception {
        String videoId = VIDEO_ID;
        videoMvcMocks.saveVideoByUid(videoId, TITLE, MODEL_ID, UID);
        long id = getId(videoId);
        DtoList<VideoInfoDto> videoInfo = videoMvcMocks.getVideoInfoDtosByUid(Collections.singletonList(id), UID + 2);
        assertEquals(0, videoInfo.getData().size());

        videoMvcMocks.deleteVideoByVideoId(videoId, UID, status().is2xxSuccessful());
        videoInfo = videoMvcMocks.getVideoInfoDtosByUid(Collections.singletonList(id), UID + 2);
        assertEquals(0, videoInfo.getData().size());
    }

    @Test
    public void testDeleteVideoByVideoIdAfterReadyFromVh() throws Exception {
        String videoId = VIDEO_ID;
        videoMvcMocks.saveVideoByUid(videoId, TITLE, MODEL_ID, UID);
        long id = getId(videoId);
        markVideoAsReadyByVh(videoId);
        DtoList<VideoInfoDto> videoInfo = videoMvcMocks.getPagedVideoInfoByParams(UID + 1, UID, Collections.singletonList(id));
        assertEquals(VideoModState.READY, videoInfo.getData().get(0).getModState());
        videoInfo = videoMvcMocks.getVideoInfoDtosByUid(Collections.singletonList(id), UID + 2);
        assertEquals(0, videoInfo.getData().size());

        videoMvcMocks.deleteVideoByVideoId(videoId, UID, status().is2xxSuccessful());
        videoInfo = videoMvcMocks.getVideoInfoDtosByUid(Collections.singletonList(id), UID + 2);
        assertEquals(0, videoInfo.getData().size());
    }

    @Test
    public void testDeleteVideoByVideoIdAfterPublish() throws Exception {
        String videoId = VIDEO_ID;
        videoMvcMocks.saveVideoByUid(videoId, TITLE, MODEL_ID, UID);
        long id = getId(videoId);
        markVideoAsReadyByVh(videoId);
        videoMvcMocks.publishVideo(id, status().is2xxSuccessful());
        DtoList<VideoInfoDto> videoInfo = videoMvcMocks.getVideoInfoDtosByUid(Collections.singletonList(id), UID + 2);
        assertEquals(1, videoInfo.getData().size());

        videoMvcMocks.deleteVideoByVideoId(videoId, UID, status().is2xxSuccessful());
        videoInfo = videoMvcMocks.getVideoInfoDtosByUid(Collections.singletonList(id), UID + 2);
        assertEquals(0, videoInfo.getData().size());
    }

    @Test
    public void testDeleteByVideoIdNonExistVideo() throws Exception {
        videoMvcMocks.deleteVideoByVideoId(VIDEO_ID, UID, status().is2xxSuccessful());
    }

    @Test
    public void testDeleteVideoByVideoIdForeignVideo() throws Exception {
        String videoId = VIDEO_ID;
        videoMvcMocks.saveVideoByUid(videoId, TITLE, MODEL_ID, UID);
        videoMvcMocks.deleteVideoByVideoId(VIDEO_ID, UID + 1, status().is4xxClientError());
    }

    // ---

    @Test
    public void testDeleteVideoByIdBeforeReadyFromVh() throws Exception {
        String videoId = VIDEO_ID;
        videoMvcMocks.saveVideoByUid(videoId, TITLE, MODEL_ID, UID);
        long id = getId(videoId);
        DtoList<VideoInfoDto> videoInfo = videoMvcMocks.getVideoInfoDtosByUid(Collections.singletonList(id), UID + 2);
        assertEquals(0, videoInfo.getData().size());

        videoMvcMocks.deleteVideoById(id, UID, status().is2xxSuccessful());
        videoInfo = videoMvcMocks.getVideoInfoDtosByUid(Collections.singletonList(id), UID + 2);
        assertEquals(0, videoInfo.getData().size());
    }

    @Test
    public void testDeleteVideoByIdAfterReadyFromVh() throws Exception {
        String videoId = VIDEO_ID;
        videoMvcMocks.saveVideoByUid(videoId, TITLE, MODEL_ID, UID);
        long id = getId(videoId);
        markVideoAsReadyByVh(videoId);

        DtoList<VideoInfoDto> videoInfo = videoMvcMocks.getVideoInfoDtosByUid(Collections.singletonList(id), UID + 2);
        assertEquals(0, videoInfo.getData().size());

        videoMvcMocks.deleteVideoById(id, UID, status().is2xxSuccessful());
        videoInfo = videoMvcMocks.getVideoInfoDtosByUid(Collections.singletonList(id), UID + 2);
        assertEquals(0, videoInfo.getData().size());
    }

    @Test
    public void testDeleteVideoByIdAfterPublish() throws Exception {
        String videoId = VIDEO_ID;
        videoMvcMocks.saveVideoByUid(videoId, TITLE, MODEL_ID, UID);
        long id = getId(videoId);
        markVideoAsReadyByVh(videoId);
        videoMvcMocks.publishVideo(id, status().is2xxSuccessful());
        DtoList<VideoInfoDto> videoInfo = videoMvcMocks.getVideoInfoDtosByUid(Collections.singletonList(id), UID + 2);
        assertEquals(1, videoInfo.getData().size());

        videoMvcMocks.deleteVideoById(id, UID, status().is2xxSuccessful());
        videoInfo = videoMvcMocks.getVideoInfoDtosByUid(Collections.singletonList(id), UID + 2);
        assertEquals(0, videoInfo.getData().size());
    }

    @Test
    public void testDeleteByIdNonExistVideo() throws Exception {
        videoMvcMocks.deleteVideoById(123, UID, status().is2xxSuccessful());
    }

    @Test
    public void testDeleteVideoByIdForeignVideo() throws Exception {
        String videoId = VIDEO_ID;
        videoMvcMocks.saveVideoByUid(videoId, TITLE, MODEL_ID, UID);
        long id = getId(videoId);
        videoMvcMocks.deleteVideoById(id, UID + 1, status().is4xxClientError());
    }


}
