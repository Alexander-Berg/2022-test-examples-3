package ru.yandex.market.pers.author.takeout;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pers.author.PersAuthorTest;
import ru.yandex.market.pers.author.cache.VideoCache;
import ru.yandex.market.pers.author.client.api.model.VideoUserType;
import ru.yandex.market.pers.author.mock.mvc.TakeoutControllerMvcMocks;
import ru.yandex.market.pers.author.security.SecurityData;
import ru.yandex.market.pers.author.takeout.model.TakeoutDataWrapper;
import ru.yandex.market.pers.author.video.VideoService;
import ru.yandex.market.pers.author.video.model.VideoInfo;
import ru.yandex.market.pers.service.common.dto.TakeoutStatusDto;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class TakeoutControllerTest extends PersAuthorTest {

    private static final Long UID = 1L;

    @Autowired
    private TakeoutControllerMvcMocks mvcMocks;

    @Autowired
    private VideoService videoService;

    @Autowired
    private VideoCache videoCache;

    @Test
    void getData() {
        saveVideo(UID);
        TakeoutDataWrapper data = mvcMocks.getData(String.valueOf(UID));
        assertEquals(1, data.getVideos().size());
    }

    @Test
    void getEmptyData() {
        TakeoutDataWrapper data = mvcMocks.getData(String.valueOf(UID));
        assertEquals(0, data.getVideos().size());
    }

    @Test
    void getStatus() {
        saveVideo(UID);
        TakeoutStatusDto status = mvcMocks.getStatus(String.valueOf(UID));
        assertArrayEquals(new String[]{"author"}, status.getTypes());
    }

    @Test
    void getEmptyStatus() {
        TakeoutStatusDto status = mvcMocks.getStatus(String.valueOf(UID));
        assertArrayEquals(new String[]{}, status.getTypes());
    }

    @Test
    void delete() {
        saveVideo(UID);
        saveVideo(UID + 1);
        mvcMocks.delete(String.valueOf(UID));

        List<VideoInfo> savedVideos = videoService.getVideoByUser(VideoUserType.UID, String.valueOf(UID));
        List<VideoInfo> savedAnotherVideos = videoService.getVideoByUser(VideoUserType.UID, String.valueOf(UID + 1));
        List<VideoInfo> cachedVideos = videoCache.getVideoByUserId(UID);
        List<VideoInfo> cachedAnotherVideos = videoCache.getVideoByUserId(UID + 1);

        assertEquals(0, savedVideos.size());
        assertEquals(0, cachedVideos.size());
        assertEquals(1, savedAnotherVideos.size());
        assertEquals(1, cachedAnotherVideos.size());
    }

    private void saveVideo(long uid) {
        VideoInfo video = VideoInfo.buildModelVideo(String.valueOf(uid), "Video", uid, 101L, 1001L);
        videoService.saveVideo(video, new SecurityData());
    }
}
