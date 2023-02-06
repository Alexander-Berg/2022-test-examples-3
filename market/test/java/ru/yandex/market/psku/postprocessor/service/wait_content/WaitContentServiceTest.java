package ru.yandex.market.psku.postprocessor.service.wait_content;

import com.google.common.collect.Iterables;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import ru.yandex.market.ir.http.Markup;
import ru.yandex.market.mbo.tracker.TrackerService;
import ru.yandex.market.psku.postprocessor.common.BaseDBTest;
import ru.yandex.market.psku.postprocessor.common.db.dao.PskuResultStorageDao;
import ru.yandex.market.psku.postprocessor.common.db.dao.PskuResultStorageHistoryDao;
import ru.yandex.market.psku.postprocessor.common.db.dao.TrackerTicketPskuStatusDao;
import ru.yandex.market.psku.postprocessor.common.db.jooq.enums.PskuInClusterState;
import ru.yandex.market.psku.postprocessor.common.db.jooq.enums.PskuStorageState;
import ru.yandex.market.psku.postprocessor.common.db.jooq.enums.PskuTrackerTicketType;
import ru.yandex.market.psku.postprocessor.common.db.jooq.tables.pojos.PskuResultStorage;
import ru.yandex.market.psku.postprocessor.common.db.jooq.tables.pojos.PskuResultStorageHistory;
import ru.yandex.market.psku.postprocessor.config.TrackerTestConfig;
import ru.yandex.market.psku.postprocessor.service.tracker.PskuTrackerService;
import ru.yandex.market.psku.postprocessor.service.tracker.mock.IssueMock;
import ru.yandex.market.psku.postprocessor.service.tracker.models.CategoryTrackerInfo;
import ru.yandex.market.psku.postprocessor.service.tracker.models.PskuTrackerInfo;
import ru.yandex.market.psku.postprocessor.service.tracker.processing.WaitContentProcessingStrategy;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.psku.postprocessor.common.db.jooq.Tables.CLUSTER;
import static ru.yandex.market.psku.postprocessor.common.db.jooq.Tables.PSKU_IN_CLUSTER_INFO;
import static ru.yandex.market.psku.postprocessor.common.db.jooq.Tables.PSKU_KNOWLEDGE;
import static ru.yandex.market.psku.postprocessor.common.db.jooq.Tables.PSKU_RESULT_STORAGE;
import static ru.yandex.market.psku.postprocessor.common.db.jooq.Tables.TRACKER_TICKET_PSKU_STATUS;

@ContextConfiguration(classes = {
    TrackerTestConfig.class
})
public class WaitContentServiceTest extends BaseDBTest {
    private static final Iterator<Long> CLUSTER_ID_ITERATOR = LongStream.iterate(1L, l -> l += 1).iterator();
    private static final Long CATEGORY_ID = 321L;
    private static final Long SUPPLIER_ID = 111L;
    private static final String COMMENT_FOR_OPERATOR = "comment for operator";
    private static final String TICKET_KEY = "ticket_key";
    private WaitContentService service;

    PskuTrackerService pskuTrackerService;

    @Captor
    ArgumentCaptor<Map<CategoryTrackerInfo, ? extends Collection<PskuTrackerInfo>>> sendCaptor;
    @Mock
    TrackerService trackerService;

    @Autowired
    PskuResultStorageDao pskuResultStorageDao;
    @Autowired
    PskuResultStorageHistoryDao historyDao;
    @Autowired
    WaitContentProxyDao waitContentProxyDao;
    @Autowired
    TrackerTicketPskuStatusDao trackerTicketPskuStatusDao;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        Mockito.when(trackerService.createTicket(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
            .thenReturn(new IssueMock().setKey("key"));

        WaitContentProcessingStrategy waitContentStrategy = new WaitContentProcessingStrategy(
            trackerService,
            trackerTicketPskuStatusDao,
            "", "", ""
        );
        pskuTrackerService = Mockito.spy(new PskuTrackerService(
            null, null, null, waitContentStrategy));
        service = new WaitContentService(pskuTrackerService, pskuResultStorageDao,
            historyDao, waitContentProxyDao);
    }

    @Test
    public void sendOnlyWaitContentState() {
        List<Long> pskusIdForSend = Arrays.asList(100L, 101L, 102L);
        List<Long> pskusIdNotForSend = Arrays.asList(200L, 201L, 202L);
        createPskus(pskusIdForSend, PskuStorageState.WAIT_CONTENT);
        createPskus(pskusIdNotForSend, PskuStorageState.NEED_INFO);

        service.doSend();

        Mockito.verify(pskuTrackerService, Mockito.times(1)).createWaitContentTickets(sendCaptor.capture());
        List<PskuTrackerInfo> sendPskus = sendCaptor.getValue().values().stream().flatMap(Collection::stream).collect(Collectors.toList());
        assertThat(sendPskus)
            .extracting(PskuTrackerInfo::getComment)
            .containsOnly(COMMENT_FOR_OPERATOR);
        assertThat(sendPskus)
            .extracting(PskuTrackerInfo::getId)
            .containsExactlyElementsOf(pskusIdForSend);
        assertThat(pskuResultStorageDao.fetchByPskuId(pskusIdForSend.toArray(new Long[0])))
            .extracting(PskuResultStorage::getState)
            .containsOnly(PskuStorageState.WAIT_CONTENT_IN_PROCESS);
        assertThat(pskuResultStorageDao.fetchByPskuId(pskusIdNotForSend.toArray(new Long[0])))
            .extracting(PskuResultStorage::getState)
            .containsOnly(PskuStorageState.NEED_INFO);
    }

    @Test
    public void shouldRotateToHistoryIfClosedWaitContentTicket() {
        List<Long> pskusIdForRotate = Arrays.asList(100L, 101L, 102L);
        createPskus(pskusIdForRotate, PskuStorageState.WAIT_CONTENT_IN_PROCESS);
        createTicketsStatus(pskusIdForRotate, true);

        List<Long> pskusIdInProcessNotForRotate = Arrays.asList(200L, 201L, 202L);
        createPskus(pskusIdInProcessNotForRotate, PskuStorageState.WAIT_CONTENT_IN_PROCESS);
        createTicketsStatus(pskusIdInProcessNotForRotate, false);

        List<Long> pskusIdNeedInfoNotForRotate = Arrays.asList(300L, 301L, 302L);
        createPskus(pskusIdNeedInfoNotForRotate, PskuStorageState.NEED_INFO);

        service.rotateProcessedPskus();

        assertThat(historyDao.findAll())
            .extracting(PskuResultStorageHistory::getPskuId)
            .containsExactlyElementsOf(pskusIdForRotate);
        assertThat(pskuResultStorageDao.findAll())
            .extracting(PskuResultStorage::getPskuId)
            .containsExactlyElementsOf(Iterables.concat(pskusIdInProcessNotForRotate, pskusIdNeedInfoNotForRotate));
    }

    private void createTicketsStatus(List<Long> pskuIds, boolean isClosed) {
        pskuIds.forEach(id -> createTicketStatus(id, isClosed));
    }

    private void createTicketStatus(long pskuId, boolean isClosed) {
        Timestamp timestamp = Timestamp.from(Instant.now());
        trackerTicketPskuStatusDao.dsl()
            .insertInto(TRACKER_TICKET_PSKU_STATUS)
            .set(TRACKER_TICKET_PSKU_STATUS.TRACKER_TICKET_KEY, TICKET_KEY)
            .set(TRACKER_TICKET_PSKU_STATUS.PSKU_ID, pskuId)
            .set(TRACKER_TICKET_PSKU_STATUS.TICKET_TYPE, PskuTrackerTicketType.WAIT_CONTENT)
            .set(TRACKER_TICKET_PSKU_STATUS.IS_CLOSED, isClosed)
            .set(TRACKER_TICKET_PSKU_STATUS.CREATE_TS, timestamp)
            .execute();
    }

    private void createPskus(List<Long> ids, PskuStorageState state) {
        ids.forEach(id -> createPsku(id, state));
    }

    private void createPsku(long pskuId, PskuStorageState state) {
        Timestamp timestamp = Timestamp.from(Instant.now());
        long clusterId = CLUSTER_ID_ITERATOR.next();
        dsl().insertInto(CLUSTER)
            .set(CLUSTER.ID, clusterId)
            .set(CLUSTER.CATEGORY_ID, CATEGORY_ID)
            .set(CLUSTER.CREATE_TIME, timestamp)
            .execute();
        dsl().insertInto(PSKU_IN_CLUSTER_INFO)
            .set(PSKU_IN_CLUSTER_INFO.CLUSTER_ID, clusterId)
            .set(PSKU_IN_CLUSTER_INFO.PSKU_ID, pskuId)
            .set(PSKU_IN_CLUSTER_INFO.CATEGORY_ID, CATEGORY_ID)
            .set(PSKU_IN_CLUSTER_INFO.STATE, PskuInClusterState.PROCESSED)
            .set(PSKU_IN_CLUSTER_INFO.RECEIVE_TS, timestamp)
            .set(PSKU_IN_CLUSTER_INFO.SEND_TS, timestamp)
            .set(PSKU_IN_CLUSTER_INFO.MARKUP_RAW_RESULT, Markup.MskuFromPskuGenerationTaskResult.newBuilder()
                .addAllComments(Collections.singletonList(Markup.MskuFromPskuGenerationTaskResult.Comment.newBuilder()
                    .addItems(COMMENT_FOR_OPERATOR)
                    .setType("FOR_REVISION")
                    .build()))
                .build())
            .execute();

        dsl().insertInto(PSKU_RESULT_STORAGE)
            .set(PSKU_RESULT_STORAGE.PSKU_ID, pskuId)
            .set(PSKU_RESULT_STORAGE.CATEGORY_ID, CATEGORY_ID)
            .set(PSKU_RESULT_STORAGE.STATE, state)
            .set(PSKU_RESULT_STORAGE.CREATE_TIME, timestamp)
            .execute();

        dsl().insertInto(PSKU_KNOWLEDGE)
            .set(PSKU_KNOWLEDGE.ID, pskuId)
            .set(PSKU_KNOWLEDGE.SUPPLIER_ID, SUPPLIER_ID)
            .set(PSKU_KNOWLEDGE.PSKU_TITLE, "psku_base_title_" + pskuId)
            .execute();
    }
}