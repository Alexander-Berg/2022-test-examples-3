package ru.yandex.canvas.service.video;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.util.CloseableIterator;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.canvas.model.direct.CreativeCampaignResult;
import ru.yandex.canvas.model.video.Addition;
import ru.yandex.canvas.repository.ItemsWithTotal;
import ru.yandex.canvas.repository.video.VideoAdditionsRepository;
import ru.yandex.canvas.service.DateTimeService;
import ru.yandex.canvas.service.DirectService;
import ru.yandex.canvas.service.PackshotService;
import ru.yandex.canvas.service.ScreenshooterService;
import ru.yandex.canvas.service.SequenceService;
import ru.yandex.canvas.service.screenshooters.VideoAdditionScreenshooterHelperService;
import ru.yandex.canvas.service.video.overlay.OverlayService;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toMap;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@RunWith(SpringJUnit4ClassRunner.class)
public class GetAdditionsTest {

    @Autowired
    VideoAdditionsService videoAdditionsService;

    @Autowired
    MongoOperations mongoOperations;

    @Autowired
    DirectService directService;

    @Configuration
    public static class TestConf {
        @MockBean
        private SequenceService sequenceService;

        @MockBean
        private VideoPresetsService presetsService;

        @MockBean
        private MovieServiceInterface movieService;

        @MockBean
        private ScreenshooterService screenshooterService;

        @MockBean
        private PackshotService packshotService;

        @MockBean
        private AudioService audioService;

        @MockBean
        private OverlayService overlayService;

        @MockBean
        private DirectService directService;

        @MockBean
        private MongoOperations mongoOperations;

        @MockBean
        private VideoPreviewUrlBuilder videoPreviewUrlBuilder;

        @Bean
        public VideoAdditionsRepository videoAdditionsRepository(MongoOperations mongoOperations) {
            return new VideoAdditionsRepository(mongoOperations);
        }

        @MockBean
        private VideoAdditionScreenshooterHelperService videoAdditionScreenshooterHelperService;

        @Bean
        public VideoAdditionsService videoAdditionsService(SequenceService sequenceService,
                                                           VideoAdditionsRepository videoAdditionsRepository, VideoPresetsService videoPresetsService,
                                                           MovieServiceInterface movieService,
                                                           PackshotService packshotService, DirectService directService,
                                                           AudioService audioService,
                                                           VideoAdditionScreenshooterHelperService videoAdditionScreenshooterHelperService) {
            return new VideoAdditionsService(sequenceService, videoAdditionsRepository, videoPresetsService,
                    "canvas.preview.host", movieService, packshotService, directService,
                    new DateTimeService(), audioService, videoAdditionScreenshooterHelperService, videoPreviewUrlBuilder);
        }
    }

    void mockStream(int size) {
        List<Addition> additions = new ArrayList<>();

        for (int i = 0; i < size; i++) {
            Addition addition = new Addition();
            addition.setId("deadbeef0" + i);
            addition.setCreativeId((long) i);

            additions.add(addition);
        }

        Iterator<Addition> additionIterator = additions.iterator();

        when(mongoOperations.count(any(Query.class), any(Class.class))).thenReturn((long) size);
        when(mongoOperations.stream(any(Query.class), any(Class.class))).thenReturn(
                new CloseableIterator<Addition>() {
                    @Override
                    public void close() {

                    }

                    @Override
                    public boolean hasNext() {
                        return additionIterator.hasNext();
                    }

                    @Override
                    public Addition next() {
                        return additionIterator.next();
                    }
                }
        );
    }

    void mockDirect(Long... creativeIds) {

        Map<Long, List<CreativeCampaignResult>> mockedResult =
                Arrays.asList(creativeIds).stream().collect(toMap(e -> e, e -> new ArrayList<>()));

        when(directService.getCreativesCampaigns(anyList(), anyLong(), anyLong())).thenReturn(mockedResult);
    }

    @Test
    public void testScrollToMiddleElelement() {
        mockStream(10);

        ItemsWithTotal<Addition> result = videoAdditionsService
                .getAdditions(10L, 12L, 9, 0, Sort.Direction.ASC, false, "",
                        true, emptyList(), 5, false);

        assertEquals(result.getItems().size(), 9);
        assertEquals(result.getTotal(), 10);
        assertEquals(result.getRealOffset(), 9);
    }

    @Test
    public void testScrollBehindLimits() {
        mockStream(10);

        ItemsWithTotal<Addition> result = videoAdditionsService
                .getAdditions(10L, 12L, 4, 0, Sort.Direction.ASC, false, "",
                        true, emptyList(), 5, false);

        assertEquals(result.getItems().size(), 6);
        assertEquals(result.getTotal(), 10);
        assertEquals(result.getRealOffset(), 6);
    }

    @Test
    public void testScrollToUnexistElement() {
        mockStream(10);

        ItemsWithTotal<Addition> result = videoAdditionsService
                .getAdditions(10L, 12L, 4, 0, Sort.Direction.ASC, false, "",
                        true, emptyList(), 50, false);

        assertEquals(result.getItems().size(), 4);
        assertEquals(result.getTotal(), 10);
        assertEquals(result.getRealOffset(), 10);
    }

    @Test
    public void testTiedCampaigns() {
        mockStream(10);
        mockDirect(1L, 2L, 3L);

        ItemsWithTotal<Addition> result = videoAdditionsService
                .getAdditions(10L, 12L, 10, 0, Sort.Direction.ASC, false, "",
                        true, emptyList(), null, true);

        assertEquals(result.getItems().size(), 3);
        assertEquals(result.getTotal(), 10);
        assertEquals(result.getRealOffset(), 10);
    }

    @Test
    public void testTiedCampaignsWithSmallLimit() {
        mockStream(10);
        mockDirect(1L, 2L, 3L);

        ItemsWithTotal<Addition> result = videoAdditionsService
                .getAdditions(10L, 12L, 4, 0, Sort.Direction.ASC, false, "",
                        true, emptyList(), null, true);

        assertEquals(result.getItems().size(), 3);
        assertEquals(result.getTotal(), 10);
        assertEquals(result.getRealOffset(), 10);
    }


    @Test
    public void testTiedCampaignsOnlyFirst() {
        mockStream(2);
        mockDirect(0L);

        ItemsWithTotal<Addition> result = videoAdditionsService
                .getAdditions(10L, 12L, 10, 0, Sort.Direction.ASC, false, "",
                        true, emptyList(), null, true);

        assertEquals(result.getItems().size(), 1);
        assertEquals(result.getTotal(), 2);
        assertEquals(result.getRealOffset(), 2);
    }

    @Test
    public void testTiedCampaignsWithNoTiedCamps() {
        mockStream(10);
        mockDirect();

        ItemsWithTotal<Addition> result = videoAdditionsService
                .getAdditions(10L, 12L, 10, 0, Sort.Direction.ASC, false, "",
                        true, emptyList(), null, true);

        assertEquals(result.getItems().size(), 0);
        assertEquals(result.getTotal(), 10);
        assertEquals(result.getRealOffset(), 10);
    }

    @Test
    public void testTiedCampaignsWithAllCampsTied() {
        mockStream(10);
        mockDirect(0L, 1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L);

        ItemsWithTotal<Addition> result = videoAdditionsService
                .getAdditions(10L, 12L, 10, 0, Sort.Direction.ASC, false, "",
                        true, emptyList(), null, true);

        assertEquals(result.getItems().size(), 10);
        assertEquals(result.getTotal(), 10);
        assertEquals(result.getRealOffset(), 10);
    }

    @Test
    public void testTiedCampaignsWithFirstTied() {
        mockStream(2);
        mockDirect(0L);

        ItemsWithTotal<Addition> result = videoAdditionsService
                .getAdditions(10L, 12L, 10, 0, Sort.Direction.ASC, false, "",
                        true, emptyList(), null, true);

        assertEquals(result.getItems().size(), 1);
        assertEquals(result.getTotal(), 2);
        assertEquals(result.getRealOffset(), 2);
    }

    @Test
    public void testTiedCampaignsWithScrollTo() {
        mockStream(10);
        mockDirect(1L, 3L, 5L);

        ItemsWithTotal<Addition> result = videoAdditionsService
                .getAdditions(10L, 12L, 10, 0, Sort.Direction.ASC, false, "",
                        true, emptyList(), 5, true);

        assertEquals(result.getItems().size(), 3);
        assertEquals(result.getTotal(), 10);
        assertEquals(result.getRealOffset(), 10);
    }

    @Test
    public void testTiedEvenCampaignsWithScrollToUnexestedElement() {
        mockStream(10);
        mockDirect(0L, 2L, 4L, 6L, 8L);

        ItemsWithTotal<Addition> result = videoAdditionsService
                .getAdditions(10L, 12L, 2, 0, Sort.Direction.ASC, false, "",
                        true, emptyList(), 5, true);

        assertEquals(result.getItems().size(), 2);
        assertEquals(result.getTotal(), 10);
        assertEquals(result.getRealOffset(), 10);
    }


    @Test
    public void testTiedEvenCampaignsWithScrollToExistedElement() {
        mockStream(10);
        mockDirect(0L, 2L, 4L, 6L, 8L);

        ItemsWithTotal<Addition> result = videoAdditionsService
                .getAdditions(10L, 12L, 2, 0, Sort.Direction.ASC, false, "",
                        true, emptyList(), 6, true);

        assertEquals(result.getItems().size(), 4);
        assertEquals(result.getTotal(), 10);
        assertEquals(result.getRealOffset(), 7);
    }

    @Test
    //If we dont have "scrollTo" creativeId, then we scroll to the end
    public void testTiedCampaignsWithScrollToNonTiedCamp() {
        mockStream(10);
        mockDirect(6L, 7L, 8L);

        ItemsWithTotal<Addition> result = videoAdditionsService
                .getAdditions(10L, 12L, 10, 0, Sort.Direction.ASC, false, "",
                        true, emptyList(), 5, true);

        assertEquals(result.getItems().size(), 3);
        assertEquals(result.getTotal(), 10);
        assertEquals(result.getRealOffset(), 10);
    }

}
