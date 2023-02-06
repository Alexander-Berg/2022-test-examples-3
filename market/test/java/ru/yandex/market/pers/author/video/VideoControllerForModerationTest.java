package ru.yandex.market.pers.author.video;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import ru.yandex.market.pers.author.client.api.dto.VideoInfoDto;
import ru.yandex.market.pers.author.client.api.dto.pager.DtoPager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class VideoControllerForModerationTest extends VideoControllerTest {

    @Test
    public void testGetVideoForModeration() throws Exception {
        videoMvcMocks.saveVideoByUid(VIDEO_ID, TITLE, MODEL_ID, UID);
        videoMvcMocks.saveVideoByUid(VIDEO_ID + 1, TITLE, MODEL_ID + 1, UID);
        videoMvcMocks.saveVideoByUid(VIDEO_ID + 2, TITLE, MODEL_ID, UID + 1);

        List<VideoInfoDto> result = videoMvcMocks.getVideoInfoForModeration(MODERATOR_ID);
        assertEquals(0, result.size());

        callbackReady(VIDEO_ID);
        callbackReady(VIDEO_ID + 1);

        result = videoMvcMocks.getVideoInfoForModeration(MODERATOR_ID);
        assertEquals(2, result.size());
    }

    @Test
    public void testGetOnlyReadyVideosForModeration() throws Exception {
        int n = 3;
        createVideos(10, n);
        jdbcTemplate.update("update pers.video set state = 1"); // deleted
        createVideos(20, n);
        jdbcTemplate.update("update pers.video set mod_state = 2 where state != 1"); // rejected
        createVideos(30, n);
        jdbcTemplate.update("update pers.video set mod_state = 3 where state = 0 and mod_state = 1"); // approved
        createVideos(40, n); // ready
        videoMvcMocks.saveVideoByUid(VIDEO_ID, TITLE, MODEL_ID, UID); // without info from VH

        DtoPager<VideoInfoDto> videos = videoMvcMocks.getPagedVideoInfoByParams(MODERATOR_ID, null, null);
        assertEquals(4 * n + 1, videos.getPager().getCount());
        assertEquals(n, videoMvcMocks.getVideoInfoForModeration(MODERATOR_ID).size());
    }

    @Test
    public void testGetVideoForModerationWithBan() throws Exception {
        videoMvcMocks.saveVideoByUid(VIDEO_ID, TITLE, MODEL_ID, UID);
        long idForBan = videoMvcMocks.saveVideoByUid(VIDEO_ID + 1, TITLE, MODEL_ID + 1, UID).getId();
        videoMvcMocks.saveVideoByUid(VIDEO_ID + 2, TITLE, MODEL_ID, UID + 1);

        List<VideoInfoDto> result = videoMvcMocks.getVideoInfoForModeration(MODERATOR_ID);
        assertEquals(0, result.size());

        callbackReady(VIDEO_ID);
        callbackReady(VIDEO_ID + 1);
        videoMvcMocks.banVideo(idForBan, status().is2xxSuccessful());

        result = videoMvcMocks.getVideoInfoForModeration(MODERATOR_ID);
        assertEquals(1, result.size());
    }

    @Test
    public void testVideoInboxForAnotherUser() throws Exception {
        createVideos(12);

        List<Long> videosFirst = getIds(videoMvcMocks.getVideoInfoForModeration(MODERATOR_ID));
        assertEquals(10, videosFirst.size());

        List<Long> videosSecond = getIds(videoMvcMocks.getVideoInfoForModeration(MODERATOR_ID + 1));
        assertEquals(2, videosSecond.size());

        // intersection
        videosFirst.retainAll(videosSecond);
        assertEquals(0, videosFirst.size());
    }

    @Test
    public void testVideoInboxWithUnmoderatedVideo() throws Exception {
        createVideos(4);

        List<Long> firstModeratorVideos = getIds(videoMvcMocks.getVideoInfoForModeration(MODERATOR_ID, 2L));
        List<Long> secondModeratorVideos = getIds(videoMvcMocks.getVideoInfoForModeration(MODERATOR_ID, 2L));

        // after 30 minutes
        removeFromInbox(firstModeratorVideos);

        firstModeratorVideos = getIds(videoMvcMocks.getVideoInfoForModeration(MODERATOR_ID, 2L));
        assertEquals(2, firstModeratorVideos.size());
        // intersection
        secondModeratorVideos.retainAll(firstModeratorVideos);
        assertEquals(0, secondModeratorVideos.size());

        // third moderator videos
        assertEquals(0, videoMvcMocks.getVideoInfoForModeration(MODERATOR_ID).size());
    }

    private void removeFromInbox(List<Long> videoIds) {
        jdbcTemplate.batchUpdate(
            "UPDATE inbox SET deleted = 1, deleted_time = now() \n" +
                "WHERE source_id = ? and type = 9 and deleted = 0",
            videoIds,
            videoIds.size(),
            (ps, id) -> ps.setLong(1, id)
        );
    }

    private List<Long> getIds(List<VideoInfoDto> videoInfoDtos) {
        return videoInfoDtos.stream().map(VideoInfoDto::getId).collect(Collectors.toList());
    }

    @Test
    public void testGetVideoForModerationWithFixedSize() throws Exception {
        long fixedListSize = 10;
        createVideos(12);

        List<VideoInfoDto> videos = videoMvcMocks.getVideoInfoForModeration(MODERATOR_ID);

        assertEquals(fixedListSize, videos.size());
    }

    @Test
    public void testGetVideoCountForModeration() throws Exception {
        int expectedVideosCount = 3;
        createVideos(expectedVideosCount);

        long videosCount = videoMvcMocks.getVideoCountForModeration(MODERATOR_ID);
        assertEquals(expectedVideosCount, videosCount);

        // get videos for moderation
        List<VideoInfoDto> videos = videoMvcMocks.getVideoInfoForModeration(MODERATOR_ID);

        videosCount = videoMvcMocks.getVideoCountForModeration(MODERATOR_ID);
        assertEquals(expectedVideosCount, videosCount);

        // moderate video
        videoMvcMocks.publishVideo(videos.get(0).getId(), status().is2xxSuccessful());
        videoMvcMocks.banVideo(videos.get(1).getId(), status().is2xxSuccessful());

        videosCount = videoMvcMocks.getVideoCountForModeration(MODERATOR_ID);
        assertEquals(1, videosCount);

        // delete video
        videoMvcMocks.deleteVideoById(videos.get(2).getId(), UID, status().is2xxSuccessful());

        videosCount = videoMvcMocks.getVideoCountForModeration(MODERATOR_ID);
        assertEquals(0, videosCount);
    }

    private void createVideos(int start, int count) throws Exception {
        for (int i = start; i < start + count; i++) {
            videoMvcMocks.saveVideoByUid(VIDEO_ID + i, TITLE, MODEL_ID + i, UID);
            callbackReady(VIDEO_ID + i);
        }
    }

    private void createVideos(int count) throws Exception {
        createVideos(0, count);
    }

}
