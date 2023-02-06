package ru.yandex.market.pers.author.video;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import ru.yandex.market.pers.author.client.api.dto.VideoInfoDto;
import ru.yandex.market.pers.author.client.api.dto.pager.DtoList;
import ru.yandex.market.pers.author.client.api.dto.pager.DtoPager;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class VideoControllerInfoByParamsTest extends VideoControllerTest {

    @Test
    public void testGetVideos() throws Exception {
        List<Long> videoIds = Arrays.asList(
            videoMvcMocks.saveVideoByUid(VIDEO_ID, TITLE, MODEL_ID, UID).getId(),
            videoMvcMocks.saveVideoByUid(VIDEO_ID + 1, TITLE, MODEL_ID + 1, UID).getId(),
            videoMvcMocks.saveVideoByUid(VIDEO_ID + 2, TITLE, MODEL_ID, UID + 1).getId());

        DtoPager<VideoInfoDto> videos = videoMvcMocks.getPagedVideoInfoByParams(MODERATOR_ID, null, null);
        assertEquals(List.of(videoIds.get(2), videoIds.get(1), videoIds.get(0)), getIdsForVideo(videos));
        assertEquals(3, videos.getPager().getCount());

        videos = videoMvcMocks.getPagedVideoInfoByParams(MODERATOR_ID, UID, null);
        assertEquals(List.of(videoIds.get(1), videoIds.get(0)), getIdsForVideo(videos));
        assertEquals(2, videos.getPager().getCount());

        videos = videoMvcMocks.getPagedVideoInfoByParams(MODERATOR_ID, UID + 1, null);
        assertEquals(List.of(videoIds.get(2)), getIdsForVideo(videos));
        assertEquals(1, videos.getPager().getCount());

        videos = videoMvcMocks.getPagedVideoInfoByParams(MODERATOR_ID, null,
            List.of(videoIds.get(0), videoIds.get(2), 1342312L));
        assertEquals(List.of(videoIds.get(2), videoIds.get(0)), getIdsForVideo(videos));
        assertEquals(2, videos.getPager().getCount());
    }

    private List<Long> getIdsForVideo(DtoPager<VideoInfoDto> videos) {
        return videos.getData().stream().map(VideoInfoDto::getId).collect(Collectors.toList());
    }

    @Test
    public void testGetPagedVideosByIds() throws Exception {
        String videoId1 = VIDEO_ID;
        long id1 = videoMvcMocks.saveVideoByUid(videoId1, TITLE, MODEL_ID, UID).getId();

        DtoList<VideoInfoDto> result = videoMvcMocks.getPagedVideoInfoByParams(UID, Collections.singletonList(id1));
        assertEquals(1, result.getData().size());

        String videoId2 = VIDEO_ID + 1;
        long id2 = videoMvcMocks.saveVideoByUid(videoId2, TITLE, MODEL_ID + 1, UID).getId();

        result = videoMvcMocks.getPagedVideoInfoByParams(UID, List.of(id1, id2));
        assertEquals(2, result.getData().size());

        callbackReady(videoId1);

        result = videoMvcMocks.getPagedVideoInfoByParams(UID, List.of(id1, id2));
        assertEquals(2, result.getData().size());
    }

    @Test
    public void testGetPagedVideosWithoutIds() throws Exception {
        videoMvcMocks.saveVideoByUid(VIDEO_ID, TITLE, MODEL_ID, UID);
        videoMvcMocks.saveVideoByUid(VIDEO_ID + 1, TITLE, MODEL_ID + 1, UID);

        DtoList<VideoInfoDto> result = videoMvcMocks.getPagedVideoInfoByParams(UID, null);
        assertEquals(2, result.getData().size());

        callbackReady(VIDEO_ID);

        result = videoMvcMocks.getPagedVideoInfoByParams(UID, null);
        assertEquals(2, result.getData().size());
    }

    @Test
    public void testGetPagedVideosWithEmptyIds() throws Exception {
        videoMvcMocks.saveVideoByUid(VIDEO_ID, TITLE, MODEL_ID, UID);
        videoMvcMocks.saveVideoByUid(VIDEO_ID + 1, TITLE, MODEL_ID + 1, UID);

        DtoList<VideoInfoDto> result = videoMvcMocks.getPagedVideoInfoByParams(UID, Collections.emptyList());
        assertEquals(2, result.getData().size());

        callbackReady(VIDEO_ID);

        result = videoMvcMocks.getPagedVideoInfoByParams(UID, Collections.emptyList());
        assertEquals(2, result.getData().size());
    }

    @Test
    public void testGetPagedVideosByAuthorId() throws Exception {
        videoMvcMocks.saveVideoByUid(VIDEO_ID, TITLE, MODEL_ID, UID);
        videoMvcMocks.saveVideoByUid(VIDEO_ID + 1, TITLE, MODEL_ID + 1, UID);
        videoMvcMocks.saveVideoByUid(VIDEO_ID + 2, TITLE, MODEL_ID, UID + 1);

        DtoList<VideoInfoDto> result = videoMvcMocks.getPagedVideoInfoByParams(UID + 1, UID, null);
        assertEquals(2, result.getData().size());

        callbackReady(VIDEO_ID);

        result = videoMvcMocks.getPagedVideoInfoByParams(UID + 1, UID, null);
        assertEquals(2, result.getData().size());
    }

}
