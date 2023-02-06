package ru.yandex.market.pers.author.video;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import ru.yandex.market.pers.author.client.api.dto.pager.DtoList;
import ru.yandex.market.pers.author.client.api.dto.VideoInfoDto;
import ru.yandex.market.pers.author.client.api.model.VideoModState;
import ru.yandex.market.pers.author.video.dto.VideoModStatesDto;
import ru.yandex.market.pers.author.video.modState.ModeratorModState;
import ru.yandex.market.pers.author.video.modState.VhModState;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class VideoControllerBanTest extends VideoControllerTest {

    @Test
    public void testBanAndPublishNotPublicVideo() throws Exception {
        String videoId = VIDEO_ID;
        long idForBan = videoMvcMocks.saveVideoByUid(videoId, TITLE, MODEL_ID, UID).getId();
        long id = getId(videoId);
        DtoList<VideoInfoDto> videoInfo = videoMvcMocks.getVideoInfoDtosByUid(Collections.singletonList(id), UID + 2);
        assertEquals(0, videoInfo.getData().size());

        VideoModStatesDto modStateDto = videoMvcMocks.banVideo(idForBan, status().is2xxSuccessful());
        checkModState(modStateDto, ModeratorModState.REJECTED, null, VideoModState.REJECTED);
        videoInfo = videoMvcMocks.getVideoInfoDtosByUid(Collections.singletonList(id), UID + 2);
        assertEquals(0, videoInfo.getData().size());

        modStateDto = videoMvcMocks.publishVideo(idForBan, status().is2xxSuccessful());
        checkModState(modStateDto, ModeratorModState.APPROVED, null, VideoModState.NEW);
        videoInfo = videoMvcMocks.getVideoInfoDtosByUid(Collections.singletonList(id), UID + 2);
        assertEquals(0, videoInfo.getData().size());
    }

    @Test
    public void testBanAndPublishPublicVideo() throws Exception {
        String videoId = VIDEO_ID;
        long idForBan = videoMvcMocks.saveVideoByUid(videoId, TITLE, MODEL_ID, UID).getId();
        long id = getId(videoId);
        markVideoAsReadyByVh(videoId);
        DtoList<VideoInfoDto> videoInfo = videoMvcMocks.getVideoInfoDtosByUid(Collections.singletonList(id), UID + 2);
        assertEquals(0, videoInfo.getData().size());
        videoInfo = videoMvcMocks.getPagedVideoInfoByParams(UID + 1, UID, Collections.singletonList(id));
        assertEquals(VideoModState.READY, videoInfo.getData().get(0).getModState());

        VideoModStatesDto modStateDto = videoMvcMocks.banVideo(idForBan, status().is2xxSuccessful());
        checkModState(modStateDto, ModeratorModState.REJECTED, VhModState.READY, VideoModState.REJECTED);
        videoInfo = videoMvcMocks.getVideoInfoDtosByUid(Collections.singletonList(id), UID + 2);
        assertEquals(0, videoInfo.getData().size());

        modStateDto = videoMvcMocks.publishVideo(idForBan, status().is2xxSuccessful());
        checkModState(modStateDto, ModeratorModState.APPROVED, VhModState.READY, VideoModState.APPROVED);
        videoInfo = videoMvcMocks.getVideoInfoDtosByUid(Collections.singletonList(id), UID + 2);
        assertEquals(1, videoInfo.getData().size());
    }

    private void checkModState(VideoModStatesDto dto, ModeratorModState moderatorModState, VhModState vhModState,
                               VideoModState finalModState) {
        assertEquals(moderatorModState, dto.getModeratorModState());
        assertEquals(vhModState, dto.getVhModState());
        assertEquals(finalModState, dto.getModState());
    }
}
