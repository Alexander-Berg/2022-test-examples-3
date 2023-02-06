package ru.yandex.market.pers.author.video;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.market.grade.statica.client.PersStaticClient;
import ru.yandex.market.pers.author.client.api.dto.VideoInfoDto;
import ru.yandex.market.pers.author.client.api.dto.pager.DtoList;
import ru.yandex.market.pers.author.client.api.dto.pager.DtoPager;
import ru.yandex.market.pers.author.client.api.model.VideoModState;
import ru.yandex.market.pers.author.video.modState.CompositeModState;
import ru.yandex.market.pers.author.video.modState.ModeratorModState;
import ru.yandex.market.pers.author.video.modState.VhModState;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import static org.mockito.AdditionalMatchers.not;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class VideoControllerProfileTest extends VideoControllerTest {

    @Autowired
    public PersStaticClient persStaticClient;

    @Test
    public void testGetUserVideo() throws Exception {
        String videoByUserString = videoMvcMocks.getVideoByUser(UID, 1, 10, status().is2xxSuccessful());
        JSONAssert.assertEquals(EMPTY_PAGER_JSON, videoByUserString, true);

        String videoId1 = VIDEO_ID;
        long modelId1 = MODEL_ID;
        saveVideo(videoId1, modelId1, UID);
        long id1 = getId(videoId1);
        updateVideoCrTime(videoId1, CR_TIME);

        String videoId2 = VIDEO_ID + "1";
        long modelId2 = MODEL_ID + 1;
        saveVideo(videoId2, modelId2, UID);
        long id2 = getId(videoId2);
        updateVideoCrTime(videoId2, CR_TIME);

        videoByUserString = videoMvcMocks.getVideoByUser(UID, 1, 10, status().is2xxSuccessful());
        String result = String.format(fileToString("/data/get_video_result_by_user.json"),
            videoId2, id2, modelId2,
            videoId1, id1, modelId1
        );
        JSONAssert.assertEquals(result, videoByUserString, true);

        videoByUserString = videoMvcMocks.getVideoByUser(UID + 1, 1, 10, status().is2xxSuccessful());
        JSONAssert.assertEquals(EMPTY_PAGER_JSON, videoByUserString, true);
    }

    @Test
    public void testGetUserVideoPaging() throws Exception {
        int pageSize = 10;
        DtoPager<VideoInfoDto> videoByUser = videoMvcMocks.getVideoByUser(UID, 1, pageSize);
        Assertions.assertEquals(videoByUser.getData().size(), 0);
        Assertions.assertEquals(videoByUser.getPager().getCount(), 0);

        int n = 16;
        List<Long> secondPage = new ArrayList<>();
        for (int i = 0; i < n - pageSize; i++) {
            String videoId = VIDEO_ID + i;
            long modelId = MODEL_ID + i;
            saveVideo(videoId, modelId, UID);
            long id = getId(videoId);
            secondPage.add(id);
        }
        // because sorting by novelty
        secondPage.sort(Collections.reverseOrder(Long::compareTo));

        List<Long> firstPage = new ArrayList<>();
        for (int i = n - pageSize; i < n; i++) {
            String videoId = VIDEO_ID + i;
            long modelId = MODEL_ID + i;
            saveVideo(videoId, modelId, UID);
            long id = getId(videoId);
            firstPage.add(id);
        }
        // because sorting by novelty
        firstPage.sort(Collections.reverseOrder(Long::compareTo));

        videoByUser = videoMvcMocks.getVideoByUser(UID, 1, pageSize);
        checkEquals(videoByUser, firstPage);
        Assertions.assertEquals(firstPage.size() + secondPage.size(), videoByUser.getPager().getCount());
        Assertions.assertEquals(1, videoByUser.getPager().getPageNum());

        videoByUser = videoMvcMocks.getVideoByUser(UID, 2, pageSize);
        checkEquals(videoByUser, secondPage);
        Assertions.assertEquals(firstPage.size() + secondPage.size(), videoByUser.getPager().getCount());
        Assertions.assertEquals(2, videoByUser.getPager().getPageNum());
    }

    @Test
    public void testNotGetUserDeletedVideo() throws Exception {
        String videoByUserString = videoMvcMocks.getVideoByUser(UID, 1, 10, status().is2xxSuccessful());
        JSONAssert.assertEquals(EMPTY_PAGER_JSON, videoByUserString, true);

        String videoId1 = VIDEO_ID;
        long modelId1 = MODEL_ID;
        saveVideo(videoId1, modelId1, UID);
        long id1 = getId(videoId1);
        updateVideoCrTime(videoId1, CR_TIME);

        String videoIdDeleted = VIDEO_ID + "1";
        long modelIdDeleted = MODEL_ID + 2;
        saveVideo(videoIdDeleted, modelIdDeleted, UID);
        long idDeleted = getId(videoIdDeleted);

        String videoId2 = VIDEO_ID + "2";
        long modelId2 = MODEL_ID + 1;
        saveVideo(videoId2, modelId2, UID);
        long id2 = getId(videoId2);
        updateVideoCrTime(videoId2, CR_TIME);

        videoMvcMocks.deleteVideoById(idDeleted, UID, status().is2xxSuccessful());

        videoByUserString = videoMvcMocks.getVideoByUser(UID, 1, 10, status().is2xxSuccessful());
        String result = String.format(fileToString("/data/get_video_result_by_user.json"),
            videoId2, id2, modelId2,
            videoId1, id1, modelId1
        );
        JSONAssert.assertEquals(result, videoByUserString, true);
    }

    @Test
    public void testGetUserVideoForDifferentStates() throws Exception {
        String videoByUserString = videoMvcMocks.getVideoByUser(UID, 1, 10, status().is2xxSuccessful());
        JSONAssert.assertEquals(EMPTY_PAGER_JSON, videoByUserString, true);

        Map<Long, VideoModState> videos = new HashMap<>();
        for (VhModState vhState : VhModState.values()) {
            for (ModeratorModState mState : ModeratorModState.values()) {
                int i = vhState.ordinal() + mState.ordinal();
                String videoId = VIDEO_ID + i;
                long modelId = MODEL_ID + i;
                saveVideo(videoId, modelId, UID);
                long id = getId(videoId);

                String body = getCallbackBody(videoId, vhState);
                videoMvcMocks.callback(body, status().is2xxSuccessful());

                videoService.updateModStateByModerator(videoId, mState);

                videos.put(id, CompositeModState.calcModState(Optional.of(vhState), Optional.of(mState)));
            }
        }

        DtoPager<VideoInfoDto> videoByUser = videoMvcMocks.getVideoByUser(UID, 1, 20);
        Assertions.assertEquals(videoByUser.getData().size(), videos.size());
        Assertions.assertEquals(videoByUser.getPager().getCount(), videos.size());
        Assertions.assertTrue(videoByUser.getData().stream().allMatch(it ->
            videos.get(it.getId()).equals(it.getModState())));
    }

    @Test
    public void testGetPublicUserVideo() throws Exception {
        String videoByUserString = videoMvcMocks.getVideoByUser(UID, 1, 10, status().is2xxSuccessful());
        JSONAssert.assertEquals(EMPTY_PAGER_JSON, videoByUserString, true);

        Map<Long, VideoModState> videos = new HashMap<>();
        for (VhModState vhState : VhModState.values()) {
            for (ModeratorModState mState : ModeratorModState.values()) {
                int i = vhState.ordinal() + mState.ordinal();
                String videoId = VIDEO_ID + i;
                long modelId = MODEL_ID + i;
                saveVideo(videoId, modelId, UID);
                long id = getId(videoId);

                String body = getCallbackBody(videoId, vhState);
                videoMvcMocks.callback(body, status().is2xxSuccessful());

                videoService.updateModStateByModerator(videoId, mState);

                videos.put(id, CompositeModState.calcModState(Optional.of(vhState), Optional.of(mState)));
            }
        }

        String videoIdDeleted = VIDEO_ID + "111";
        long modelIdDeleted = MODEL_ID + 111;
        saveVideo(videoIdDeleted, modelIdDeleted, UID);
        long idDeleted = getId(videoIdDeleted);
        videoMvcMocks.deleteVideoById(idDeleted, UID, status().is2xxSuccessful());

        Optional<Long> approvedVideoId = videos.entrySet()
            .stream()
            .filter(longVideoModStateEntry -> longVideoModStateEntry.getValue() == VideoModState.APPROVED)
            .map(Map.Entry::getKey)
            .findAny();


        DtoPager<VideoInfoDto> videoByUser = videoMvcMocks.getPublicVideoByUser(UID, 1, 20);
        Assertions.assertEquals(videoByUser.getData().size(), 1);
        Assertions.assertEquals(videoByUser.getPager().getCount(), 1);
        Assertions.assertTrue(approvedVideoId.isPresent());
        Assertions.assertEquals(approvedVideoId.get(), videoByUser.getData().get(0).getId());
    }

    @Test
    public void testAgitationForVideo() throws Exception {
        long modelIdNoVideoNoGrades = MODEL_ID;
        long modelIdNoVideo = MODEL_ID + 1;
        long modelIdHasNewVideo = MODEL_ID + 2;
        long modelIdHasReadyVideo = MODEL_ID + 3;
        long modelIdHasRejectedVideo = MODEL_ID + 4;
        long modelIdHasDeletedVideo = MODEL_ID + 5;
        long modelIdHasApprovedVideo = MODEL_ID + 6;

        List<Long> allModelsWithGrades = Arrays.asList(modelIdNoVideo, modelIdHasNewVideo, modelIdHasReadyVideo,
            modelIdHasRejectedVideo, modelIdHasDeletedVideo, modelIdHasApprovedVideo);
        List<Long> modelsForAgitation = Arrays.asList(modelIdNoVideo, modelIdHasDeletedVideo);

        //modelIdNoVideo with no video

        saveVideo(VIDEO_ID + modelIdHasNewVideo, modelIdHasNewVideo, UID);

        saveVideo(VIDEO_ID + modelIdHasReadyVideo, modelIdHasReadyVideo, UID);
        callbackReady(VIDEO_ID + modelIdHasReadyVideo);

        long idForBan = videoMvcMocks.saveVideoByUid(VIDEO_ID + modelIdHasRejectedVideo, TITLE, modelIdHasRejectedVideo,
                UID).getId();
        callbackReady(VIDEO_ID + modelIdHasRejectedVideo);
        videoMvcMocks.banVideo(idForBan, status().is2xxSuccessful());

        saveVideo(VIDEO_ID + modelIdHasDeletedVideo, modelIdHasDeletedVideo, UID);
        videoMvcMocks.deleteVideo(VIDEO_ID + modelIdHasDeletedVideo, UID, status().is2xxSuccessful());

        long idForPublish = videoMvcMocks
            .saveVideoByUid(VIDEO_ID + modelIdHasApprovedVideo, TITLE, modelIdHasApprovedVideo, UID).getId();
        callbackReady(VIDEO_ID + modelIdHasApprovedVideo);
        videoMvcMocks.publishVideo(idForPublish, status().is2xxSuccessful());

        when(persStaticClient.getUserModels(eq(UID), anyLong()))
            .thenReturn(allModelsWithGrades);
        when(persStaticClient.getUserModels(not(eq(UID)), anyLong()))
            .thenReturn(Collections.emptyList());

        DtoList<Long> result = videoMvcMocks.getAuthorModelVideoAgitationByUidDto(UID);
        MatcherAssert.assertThat(modelsForAgitation, Matchers.containsInAnyOrder(result.getData().toArray()));

        result = videoMvcMocks.getAuthorModelVideoAgitationByUidDto(UID + 1);
        Assertions.assertTrue(result.getData().isEmpty());
    }

    @Test
    public void testPersStaticError() throws Exception {
        when(persStaticClient.getUserModels(anyLong(), anyLong()))
            .thenThrow(new IllegalArgumentException("Static result not found"));

        String result = videoMvcMocks.getAuthorModelVideoAgitationByUid(UID,
            status().is4xxClientError());
        Assertions.assertTrue(result.contains("Static result not found"));
    }

    @Test
    public void testPageSizeForAgitation() throws Exception {
        long userId = UID; // has grades and no video
        int n = 20;
        List<Long> modelIds = new ArrayList<>();
        for (int i = 0; i <= n; i++) {
            modelIds.add(MODEL_ID + i);
        }
        when(persStaticClient.getUserModels(anyLong(), anyLong()))
            .thenReturn(modelIds);

        int pageSize = n;
        DtoList<Long> result = videoMvcMocks.getAuthorModelVideoAgitationByUidDto(userId, pageSize);
        Assertions.assertEquals(pageSize, result.getData().size());

        pageSize = 3;
        result = videoMvcMocks.getAuthorModelVideoAgitationByUidDto(userId, pageSize);
        Assertions.assertEquals(pageSize, result.getData().size());
    }

    @Test
    public void testAgitationFormat() throws Exception {
        List<Long> modelIds = LongStream.range(MODEL_ID, MODEL_ID + 4).boxed().collect(Collectors.toList());

        when(persStaticClient.getUserModels(eq(UID), anyLong()))
            .thenReturn(modelIds);

        String result = videoMvcMocks.getAuthorModelVideoAgitationByUid(UID, status().is2xxSuccessful());
        String expectedResult = fileToString("/data/get_agitation.json");
        JSONAssert.assertEquals(expectedResult, result, false);

        result = videoMvcMocks.getAuthorModelVideoAgitationByUid(UID + 1, status().is2xxSuccessful());
        expectedResult = fileToString("/data/get_agitation_empty.json");
        JSONAssert.assertEquals(expectedResult, result, false);
    }
}
