package ru.yandex.market.pers.tms.ugc.stat;

import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pers.grade.client.model.ModState;
import ru.yandex.market.pers.grade.core.GradeCreator;
import ru.yandex.market.pers.grade.core.model.notification.NotificationEvent;
import ru.yandex.market.pers.grade.core.moderation.GradeModeratorModificationProxy;
import ru.yandex.market.pers.grade.core.service.NotificationQueueService;
import ru.yandex.market.pers.grade.core.util.MarketUtilsService;
import ru.yandex.market.pers.tms.MockedPersTmsTest;
import ru.yandex.market.saas.search.SaasSearchService;
import ru.yandex.market.saas.search.response.SaasSearchDocument;
import ru.yandex.market.saas.search.response.SaasSearchResponse;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.market.pers.grade.client.model.ModState.APPROVED;
import static ru.yandex.market.pers.grade.core.model.notification.NotificationEventType.NOTIFY_GRADE_MOD_SHOP;
import static ru.yandex.market.pers.grade.core.model.notification.NotificationEventType.NOTIFY_GRADE_MOD_USER;

/**
 * @author Ilya Kislitsyn / ilyakis@ / 29.04.2020
 */
public class SaasIndexMetricsServiceTest extends MockedPersTmsTest {

    @Autowired
    private GradeModeratorModificationProxy gradeModeratorModificationProxy;
    @Autowired
    private SaasIndexMetricsService executor;
    @Autowired
    private GradeCreator gradeCreator;
    @Autowired
    private NotificationQueueService notificationQueueService;
    @Autowired
    private SaasSearchService saasSearchService;

    @Test
    public void testIndexTime() throws Exception {
        long modelId = 1;
        long userId = 100;

        long gradeId = gradeCreator.createModelGradeUnmoderated(modelId, userId);
        long gradeIdNotFound = gradeCreator.createModelGradeUnmoderated(modelId, userId);

        // moderate
        gradeModeratorModificationProxy.moderateGradeReplies(
            List.of(gradeId, gradeIdNotFound),
            Collections.emptyList(),
            0L,
            ModState.APPROVED
        );

        // get saas indexing result
        SaasSearchResponse mockedResponse = mock(SaasSearchResponse.class);
        when(mockedResponse.getDocuments()).then(invocation -> {
            return List.of(
                mockedDocument(gradeId)
            );
        });
        when(saasSearchService.searchNoCache(any())).thenReturn(mockedResponse);

        assertEquals(0, notificationQueueService.getNewEventsCount());
        executor.updateIndexTime();

        // check events added
        assertEquals(2, notificationQueueService.getNewEventsCount());

        // shop mail
        List<NotificationEvent> events = notificationQueueService.getNewEvents(NOTIFY_GRADE_MOD_SHOP, 10);
        assertEquals(1, events.size());

        NotificationEvent event = events.get(0);
        assertEquals(gradeId, event.getDataLong(MarketUtilsService.KEY_GRADE_ID));

        // use mail
        events = notificationQueueService.getNewEvents(NOTIFY_GRADE_MOD_USER, 10);
        assertEquals(1, events.size());

        event = events.get(0);
        assertEquals(gradeId, event.getDataLong(MarketUtilsService.KEY_GRADE_ID));
        assertEquals(APPROVED.value(), event.getDataLong(MarketUtilsService.KEY_MOD_STATE));

    }

    private SaasSearchDocument mockedDocument(long id) {
        SaasSearchDocument doc = mock(SaasSearchDocument.class);
        when(doc.getId()).thenReturn(String.valueOf(id));
        return doc;
    }
}
