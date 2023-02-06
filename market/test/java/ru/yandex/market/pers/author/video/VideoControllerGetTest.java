package ru.yandex.market.pers.author.video;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pers.author.agitation.AgitationService;
import ru.yandex.market.pers.author.agitation.model.AgitationCancel;
import ru.yandex.market.pers.author.agitation.model.AgitationUser;
import ru.yandex.market.pers.author.client.api.dto.pager.DtoPager;
import ru.yandex.market.pers.author.client.api.model.AgitationCancelReason;
import ru.yandex.market.pers.author.client.api.model.AgitationEntity;
import ru.yandex.market.pers.author.client.api.model.AgitationType;
import ru.yandex.market.pers.author.expertise.ExpertiseService;
import ru.yandex.market.pers.author.expertise.model.ExpertiseDiff;
import ru.yandex.market.pers.author.client.api.dto.VideoInfoDto;
import ru.yandex.market.pers.author.client.api.model.VideoModState;
import ru.yandex.market.pers.author.video.modState.CompositeModState;
import ru.yandex.market.pers.author.video.modState.ModeratorModState;
import ru.yandex.market.pers.author.video.modState.VhModState;
import ru.yandex.market.report.ReportService;
import ru.yandex.market.report.model.Category;
import ru.yandex.market.report.model.Model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class VideoControllerGetTest extends VideoControllerTest {
    @Autowired
    private ExpertiseService expertiseService;

    @Autowired
    private AgitationService agitationService;

    @Autowired
    private ReportService reportService;

    @Test
    public void testGetVideoByIdByUid() throws Exception {
        String videoId = VIDEO_ID;
        String videoCreateRequest = fileToString("/data/example_video_create_request.json");
        videoMvcMocks.saveVideoByUid(videoCreateRequest, UID, status().is2xxSuccessful());
        updateVideoCrTime(videoId, CR_TIME);
        long id = getId(videoId);

        String resultEmptyExpected = fileToString("/data/example_video_get_empty_result.json");
        String resultEmpty = videoMvcMocks.getVideoInfoByUid(Collections.singletonList(id), UID + 2);
        JSONAssert.assertEquals(resultEmptyExpected, resultEmpty, true);

        markVideoAsReadyByVh(videoId);
        videoMvcMocks.publishVideo(id, status().is2xxSuccessful());
        String resultExpected = String.format(fileToString("/data/example_video_get_result.json"), id);
        String result = videoMvcMocks.getVideoInfoByUid(Collections.singletonList(id), UID + 2);
        JSONAssert.assertEquals(resultExpected, result, true);
    }

    @Test
    public void testGetVideoByIdByYandexUid() throws Exception {
        String videoId = VIDEO_ID;
        String videoCreateRequest = fileToString("/data/example_video_create_request.json");
        videoMvcMocks.saveVideoByUid(videoCreateRequest, UID, status().is2xxSuccessful());
        updateVideoCrTime(videoId, CR_TIME);
        long id = getId(videoId);

        String resultEmptyExpected = fileToString("/data/example_video_get_empty_result.json");
        String resultEmpty = videoMvcMocks.getVideoInfoByYandexUid(Collections.singletonList(id), YANDEX_UID + 2);
        JSONAssert.assertEquals(resultEmptyExpected, resultEmpty, true);

        markVideoAsReadyByVh(videoId);
        videoMvcMocks.publishVideo(id, status().is2xxSuccessful());
        String resultExpected = String.format(fileToString("/data/example_video_get_result.json"), id);
        String result = videoMvcMocks.getVideoInfoByYandexUid(Collections.singletonList(id), YANDEX_UID + 2);
        JSONAssert.assertEquals(resultExpected, result, true);
    }

    @Test
    public void testGetVideoByModelIdByUid() throws Exception {
        String videoByUserString = videoMvcMocks.getVideosByModelByUid(MODEL_ID, UID, 1, 10);
        JSONAssert.assertEquals(EMPTY_PAGER_JSON, videoByUserString, true);

        videoByUserString = videoMvcMocks.getVideosByModelByUid(MODEL_ID + 1, UID, 1, 10);
        JSONAssert.assertEquals(EMPTY_PAGER_JSON, videoByUserString, true);

        String videoId1 = VIDEO_ID;
        long uid1 = UID;
        long id1 = createPublicVideo(videoId1, uid1);

        String videoId2 = VIDEO_ID + "1";
        long uid2 = UID + 1;
        long id2 = createPublicVideo(videoId2, uid2);

        videoByUserString = videoMvcMocks.getVideosByModelByUid(MODEL_ID, UID, 1, 10);
        String result = String.format(fileToString("/data/get_video_result_by_model.json"),
            videoId2, id2, uid2, MODEL_ID,
            videoId1, id1, uid1, MODEL_ID
        );
        JSONAssert.assertEquals(result, videoByUserString, true);

        videoByUserString = videoMvcMocks.getVideosByModelByUid(MODEL_ID + 1, UID + 2, 1, 10);
        JSONAssert.assertEquals(EMPTY_PAGER_JSON, videoByUserString, true);
    }

    @Test
    public void testGetVideoByModelIdByUidForDifferentUsers() throws Exception {
        long uid1 = UID;
        long uid2 = UID + 1;
        long uid3 = UID + 2;
        String videoByModel = videoMvcMocks.getVideosByModelByUid(MODEL_ID, uid1, 1, 10);
        JSONAssert.assertEquals(EMPTY_PAGER_JSON, videoByModel, true);
        videoByModel = videoMvcMocks.getVideosByModelByUid(MODEL_ID, uid2, 1, 10);
        JSONAssert.assertEquals(EMPTY_PAGER_JSON, videoByModel, true);
        videoByModel = videoMvcMocks.getVideosByModelByUid(MODEL_ID, uid3, 1, 10);
        JSONAssert.assertEquals(EMPTY_PAGER_JSON, videoByModel, true);

        String videoId1 = VIDEO_ID;
        long id1 = createPublicVideo(videoId1, uid1);

        String videoId2 = VIDEO_ID + "1";
        long id2 = createPublicVideo(videoId2, uid2);

        String result = String.format(fileToString("/data/get_video_result_by_model.json"),
            videoId2, id2, uid2, MODEL_ID,
            videoId1, id1, uid1, MODEL_ID
        );
        videoByModel = videoMvcMocks.getVideosByModelByUid(MODEL_ID, uid1, 1, 10);
        JSONAssert.assertEquals(result, videoByModel, true);
        videoByModel = videoMvcMocks.getVideosByModelByUid(MODEL_ID, uid2, 1, 10);
        JSONAssert.assertEquals(result, videoByModel, true);
        videoByModel = videoMvcMocks.getVideosByModelByUid(MODEL_ID, uid3, 1, 10);
        JSONAssert.assertEquals(result, videoByModel, true);
    }

    @Test
    public void testGetVideoByModelIdByYandexUid() throws Exception {
        String videoByUserString = videoMvcMocks.getVideosByModelByYandexUid(MODEL_ID, YANDEX_UID, 1, 10);
        JSONAssert.assertEquals(EMPTY_PAGER_JSON, videoByUserString, true);

        videoByUserString = videoMvcMocks.getVideosByModelByYandexUid(MODEL_ID + 1, YANDEX_UID, 1, 10);
        JSONAssert.assertEquals(EMPTY_PAGER_JSON, videoByUserString, true);

        String videoId1 = VIDEO_ID;
        long uid1 = UID;
        long id1 = createPublicVideo(videoId1, uid1);

        String videoId2 = VIDEO_ID + "1";
        long uid2 = UID + 1;
        long id2 = createPublicVideo(videoId2, uid2);

        videoByUserString = videoMvcMocks.getVideosByModelByYandexUid(MODEL_ID, YANDEX_UID, 1, 10);
        String result = String.format(fileToString("/data/get_video_result_by_model.json"),
            videoId2, id2, uid2, MODEL_ID,
            videoId1, id1, uid1, MODEL_ID
        );
        JSONAssert.assertEquals(result, videoByUserString, true);

        videoByUserString = videoMvcMocks.getVideosByModelByYandexUid(MODEL_ID + 1, YANDEX_UID, 1, 10);
        JSONAssert.assertEquals(EMPTY_PAGER_JSON, videoByUserString, true);
    }

    @Test
    public void testGetVideoByModelIdOnlyPublicVideo() throws Exception {
        String videoByUserString = videoMvcMocks.getVideosByModelByUid(MODEL_ID, UID, 1, 10);
        JSONAssert.assertEquals(EMPTY_PAGER_JSON, videoByUserString, true);

        List<Long> publicVideoIds = new ArrayList<>();
        for (VhModState vhState : VhModState.values()) {
            for (ModeratorModState mState : ModeratorModState.values()) {
                int i = vhState.ordinal() + mState.ordinal();
                String videoId = VIDEO_ID + i;
                saveVideo(videoId, MODEL_ID, UID + i);
                long id = getId(videoId);

                String body = getCallbackBody(videoId, vhState);
                videoMvcMocks.callback(body, status().is2xxSuccessful());

                videoService.updateModStateByModerator(videoId, mState);

                VideoModState finalVideoModState = CompositeModState.calcModState(Optional.of(vhState), Optional.of(mState));
                if (finalVideoModState == VideoModState.APPROVED) {
                    publicVideoIds.add(id);
                }
            }
        }
        // because sorting by novelty
        publicVideoIds.sort(Collections.reverseOrder(Long::compareTo));

        DtoPager<VideoInfoDto> videosByModel = videoMvcMocks.getVideoDtosByModelByUid(MODEL_ID, UID, 1, publicVideoIds.size() + 1);
        checkEquals(videosByModel, publicVideoIds);

        videosByModel = videoMvcMocks.getVideoDtosByModelByUid(MODEL_ID, UID, 1, publicVideoIds.size() + 1);
        checkEquals(videosByModel, publicVideoIds);
    }

    @Test
    public void testGetVideoByModelPaging() throws Exception {
        int pageSize = 10;
        DtoPager<VideoInfoDto> videoByModel = videoMvcMocks.getVideoDtosByModelByUid(MODEL_ID, UID, 1, pageSize);
        Assertions.assertEquals(videoByModel.getData().size(), 0);
        Assertions.assertEquals(videoByModel.getPager().getCount(), 0);

        int n = 16;
        List<Long> secondPage = new ArrayList<>();
        for (int i = 0; i < n - pageSize; i++) {
            secondPage.add(createPublicVideo(VIDEO_ID + i, UID + i));
        }
        // because sorting by novelty
        secondPage.sort(Collections.reverseOrder(Long::compareTo));

        List<Long> firstPage = new ArrayList<>();
        for (int i = n - pageSize; i < n; i++) {
            firstPage.add(createPublicVideo(VIDEO_ID + i, UID + i));
        }
        // because sorting by novelty
        firstPage.sort(Collections.reverseOrder(Long::compareTo));

        videoByModel = videoMvcMocks.getVideoDtosByModelByUid(MODEL_ID, UID, 1, pageSize);
        checkEquals(videoByModel, firstPage);
        Assertions.assertEquals(firstPage.size() + secondPage.size(), videoByModel.getPager().getCount());
        Assertions.assertEquals(1, videoByModel.getPager().getPageNum());

        videoByModel = videoMvcMocks.getVideoDtosByModelByUid(MODEL_ID, UID, 2, pageSize);
        checkEquals(videoByModel, secondPage);
        Assertions.assertEquals(firstPage.size() + secondPage.size(), videoByModel.getPager().getCount());
        Assertions.assertEquals(2, videoByModel.getPager().getPageNum());
    }

    @Test
    public void testGetVideoByIdByUidWithSku() throws Exception {
        String videoId = VIDEO_ID;
        String videoCreateRequest = fileToString("/data/example_video_with_sku_create_request.json");
        videoMvcMocks.saveVideoByUid(videoCreateRequest, UID, status().is2xxSuccessful());
        updateVideoCrTime(videoId, CR_TIME);
        long id = getId(videoId);

        String resultEmptyExpected = fileToString("/data/example_video_get_empty_result.json");
        String resultEmpty = videoMvcMocks.getVideoInfoByUid(Collections.singletonList(id), UID + 2);
        JSONAssert.assertEquals(resultEmptyExpected, resultEmpty, true);

        markVideoAsReadyByVh(videoId);
        videoMvcMocks.publishVideo(id, status().is2xxSuccessful());
        String resultExpected = String.format(fileToString("/data/example_video_with_sku_get_result.json"), id);
        String result = videoMvcMocks.getVideoInfoByUid(Collections.singletonList(id), UID + 2);
        JSONAssert.assertEquals(resultExpected, result, true);
    }

    @Test
    public void testCreateVideoGainExpertise() throws Exception {
        int hid = 123123;
        when(reportService.getModelById(anyLong())).then(invocation -> {
            Model model = mock(Model.class);
            when(model.getCategory()).thenReturn(new Category(hid, "name"));
            return Optional.of(model);
        });

        saveVideo("vid", MODEL_ID, UID);

        assertEquals(1, jdbcTemplate.queryForObject("select count(*) from pers.expertise_diff", Long.class));
        List<ExpertiseDiff> diffList = expertiseService.getFullDiff(List.of(UID), null);
        assertEquals(1, diffList.size());
        ExpertiseDiff diff = diffList.get(0);
        assertEquals(UID, diff.getUserId());
        assertEquals(AgitationEntity.MODEL, diff.getEntity());
        assertEquals(AgitationType.MODEL_VIDEO, diff.getAgitationType());
        assertEquals(1, diff.getAction());
        assertEquals(String.valueOf(MODEL_ID), diff.getEntityId());
        assertEquals(hid, diff.getHid());

        // no more expertise added on second call
        saveVideo("vid2", MODEL_ID, UID);
        assertEquals(1, expertiseService.getFullDiff(List.of(UID), null).size());

        // add more for other model
        saveVideo("vid3", MODEL_ID + 1, UID);
        assertEquals(2, expertiseService.getFullDiff(List.of(UID), null).size());
    }

    @Test
    public void testCreateVideoCompleteAgitation() throws Exception {
        int hid = 123123;
        when(reportService.getModelById(anyLong())).then(invocation -> {
            Model model = mock(Model.class);
            when(model.getCategory()).thenReturn(new Category(hid, "name"));
            return Optional.of(model);
        });

        AgitationUser user = AgitationUser.uid(UID);
        List<AgitationCancel> cancelledAgitations = agitationService.getCancelledAgitations(user, null);
        assertEquals(0, cancelledAgitations.size());

        saveVideo("vid", MODEL_ID, UID);

        // agitation canceled
        cancelledAgitations = agitationService.getCancelledAgitations(user, null);
        assertEquals(1, cancelledAgitations.size());
        assertEquals(AgitationCancelReason.COMPLETED, cancelledAgitations.get(0).getReason());
        assertEquals(AgitationType.MODEL_VIDEO, cancelledAgitations.get(0).getAgitation().getTypeEnum());
        assertEquals(String.valueOf(MODEL_ID), cancelledAgitations.get(0).getAgitation().getEntityId());

        // clean cancel table
        jdbcTemplate.update("delete from pers.agitation_cancel where 1=1");

        cancelledAgitations = agitationService.getCancelledAgitations(user, null);
        assertEquals(0, cancelledAgitations.size());

        // save another video - check agitation also added
        saveVideo("vid2", MODEL_ID, UID);

        cancelledAgitations = agitationService.getCancelledAgitations(user, null);
        assertEquals(1, cancelledAgitations.size());
        assertEquals(AgitationCancelReason.COMPLETED, cancelledAgitations.get(0).getReason());
        assertEquals(AgitationType.MODEL_VIDEO, cancelledAgitations.get(0).getAgitation().getTypeEnum());
        assertEquals(String.valueOf(MODEL_ID), cancelledAgitations.get(0).getAgitation().getEntityId());
    }

    private long createPublicVideo(String videoId, long uid) throws Exception {
        saveVideo(videoId, MODEL_ID, uid);
        long id1 = getId(videoId);
        updateVideoCrTime(videoId, CR_TIME);
        callbackReady(videoId);
        videoMvcMocks.publishVideo(id1, status().is2xxSuccessful());
        return id1;
    }

    @Test
    public void testGetInternalVideoInfo() throws Exception {
        String videoId = VIDEO_ID;
        String videoCreateRequest = fileToString("/data/example_video_create_request.json");
        videoMvcMocks.saveVideoByUid(videoCreateRequest, UID, status().is2xxSuccessful());
        updateVideoCrTime(videoId, CR_TIME);
        long id = getId(videoId);

        String resultEmptyExpected = fileToString("/data/example_video_get_empty_result.json");
        String resultEmpty = videoMvcMocks.getInternalVideoInfo(Collections.singletonList(id));
        JSONAssert.assertEquals(resultEmptyExpected, resultEmpty, true);

        markVideoAsReadyByVh(videoId);
        videoMvcMocks.publishVideo(id, status().is2xxSuccessful());
        String resultExpected = String.format(fileToString("/data/example_video_get_result.json"), id);
        String result = videoMvcMocks.getInternalVideoInfo(Collections.singletonList(id));
        JSONAssert.assertEquals(resultExpected, result, true);

        saveVideo(videoId + 1, MODEL_ID + 1, UID + 1);
        updateVideoCrTime(videoId + 1, CR_TIME);
        long id2 = getId(videoId + 1);
        markVideoAsReadyByVh(videoId + 1);
        videoMvcMocks.publishVideo(id2, status().is2xxSuccessful());

        resultExpected = String.format(fileToString("/data/example_two_video_get_result.json"), id, id2);
        result = videoMvcMocks.getInternalVideoInfo(Arrays.asList(id, id2));
        JSONAssert.assertEquals(resultExpected, result, true);
    }
}
