package ru.yandex.market.psku.postprocessor.service.tracker.processing;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.market.mbo.tracker.utils.IssueStatus;
import ru.yandex.market.mbo.tracker.utils.TicketType;
import ru.yandex.market.psku.postprocessor.common.BaseDBTest;
import ru.yandex.market.psku.postprocessor.common.db.dao.PskuResultStorageDao;
import ru.yandex.market.psku.postprocessor.common.db.dao.TrackerTicketPskuStatusDao;
import ru.yandex.market.psku.postprocessor.common.db.jooq.enums.PskuStorageState;
import ru.yandex.market.psku.postprocessor.common.db.jooq.enums.PskuTrackerTicketType;
import ru.yandex.market.psku.postprocessor.common.db.jooq.tables.pojos.PskuResultStorage;
import ru.yandex.market.psku.postprocessor.common.db.jooq.tables.pojos.TrackerTicketPskuStatus;
import ru.yandex.market.psku.postprocessor.service.tracker.mock.IssueMock;
import ru.yandex.market.psku.postprocessor.service.tracker.mock.TrackerServiceMock;
import ru.yandex.market.psku.postprocessor.service.tracker.models.CategoryTrackerInfo;
import ru.yandex.market.psku.postprocessor.service.tracker.models.PskuTrackerInfo;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static ru.yandex.market.psku.postprocessor.common.db.jooq.Tables.TRACKER_TICKET_PSKU_STATUS;

public class WaitContentProcessingStrategyTest extends BaseDBTest implements ProcessingStrategyTest {

    private static final String MBO_CATEGORY_BASE_URL =
            "https://mbo-testing.market.yandex.ru/gwt/#tovarTree/hyperId=";
    private static final String TRACKER_QUEUE = "PSKUSUPTEST";

    @Autowired
    private TrackerTicketPskuStatusDao trackerTicketPskuStatusDao;

    @Autowired
    private PskuResultStorageDao pskuResultStorageDao;

    private TrackerServiceMock trackerServiceMock = new TrackerServiceMock();

    private WaitContentProcessingStrategy waitContentProcessingStrategy;

    @Before
    public void setUp() {
        waitContentProcessingStrategy = new WaitContentProcessingStrategy(
                trackerServiceMock,
                trackerTicketPskuStatusDao,
                MskuFromPskuGenProcessingStrategyTest.MBO_ENTITY_BASE_URL,
                MBO_CATEGORY_BASE_URL,
                TRACKER_QUEUE);

        PskuResultStorage pskuResultStorage2 = getPskuResultStorage(PSKU_ID_2, PskuStorageState.NEED_INFO);
        PskuResultStorage pskuResultStorage3 = getPskuResultStorage(PSKU_ID_3, PskuStorageState.NEED_INFO);

        pskuResultStorageDao.insert(pskuResultStorage2, pskuResultStorage3);
    }

    @Test
    public void testCreateTrackerTicket() {
        CategoryTrackerInfo categoryInfo = new CategoryTrackerInfo(CATEGORY_ID, CATEGORY_NAME);
        PskuTrackerInfo pskuInfo2 = new PskuTrackerInfo(PSKU_ID_2, PSKU_NAME);
        PskuTrackerInfo pskuInfo3 = new PskuTrackerInfo(PSKU_ID_3, PSKU_NAME);

        List<IssueMock> actualTickets = waitContentProcessingStrategy
                .createTrackerTickets(categoryInfo, Arrays.asList(pskuInfo2, pskuInfo3)).stream()
                .map(ticket -> (IssueMock) ticket)
                .collect(Collectors.toList());

        Assertions.assertThat(actualTickets).isNotNull();
        Assertions.assertThat(actualTickets.size()).isEqualTo(2);
        checkCreatedTicket(actualTickets.get(0), PSKU_ID_2);
        checkCreatedTicket(actualTickets.get(1), PSKU_ID_3);

        int createdRecordInDB = trackerTicketPskuStatusDao.dsl()
                .selectCount()
                .from(TRACKER_TICKET_PSKU_STATUS)
                .where(TRACKER_TICKET_PSKU_STATUS.TICKET_TYPE.eq(PskuTrackerTicketType.WAIT_CONTENT))
                .and(TRACKER_TICKET_PSKU_STATUS.IS_CLOSED.eq(false))
                .and(TRACKER_TICKET_PSKU_STATUS.PSKU_ID.in(PSKU_ID_2, PSKU_ID_3))
                .and(TRACKER_TICKET_PSKU_STATUS.TRACKER_TICKET_KEY.in(
                        actualTickets.get(0).getKey(), actualTickets.get(1).getKey()))
                .fetchOneInto(Integer.class);

        Assertions.assertThat(createdRecordInDB).isEqualTo(2);
    }

    @Test
    public void testGetTicketType() {
        Assertions.assertThat(waitContentProcessingStrategy.getTicketType()).isEqualTo(TicketType.WAIT_CONTENT);
    }

    @Test
    public void testProcessResolvedTrackerTicket() {
        IssueMock defaultIssue = getDefaultTicket();
        defaultIssue.setKey(defaultIssue.getKey() + 1);
        defaultIssue.setIssueStatus(IssueStatus.RESOLVED);
        trackerServiceMock.putTicket(defaultIssue);
        TrackerTicketPskuStatus trackerTicketPskuStatus = createTrackerTicketPskuStatus(defaultIssue.getKey(),
                PSKU_ID_2, PskuTrackerTicketType.WAIT_CONTENT);
        trackerTicketPskuStatusDao.insertCreatedTicketsInfos(Collections.singleton(trackerTicketPskuStatus));

        waitContentProcessingStrategy.process();

        IssueMock actualTicket = (IssueMock) trackerServiceMock.getTicket(defaultIssue.getKey());
        List<String> actualComments = trackerServiceMock.getRawComments(actualTicket);

        Assertions.assertThat(actualTicket).isNotNull();

        Assertions.assertThat(actualTicket.getIssueStatus()).isEqualTo(IssueStatus.CLOSE);

        Assertions.assertThat(actualComments.size()).isEqualTo(1);
        Assertions.assertThat(actualComments.get(0)).isEqualTo(getExpectedSuccessTicketComment());
    }

    @Test
    public void testNotProcessOpenTrackerTicket() {
        IssueMock defaultIssue = getDefaultTicket();
        defaultIssue.setKey(defaultIssue.getKey() + 2);
        trackerServiceMock.putTicket(defaultIssue);
        TrackerTicketPskuStatus trackerTicketPskuStatus = createTrackerTicketPskuStatus(defaultIssue.getKey(),
                PSKU_ID_2, PskuTrackerTicketType.WAIT_CONTENT);
        trackerTicketPskuStatusDao.insertCreatedTicketsInfos(Collections.singleton(trackerTicketPskuStatus));

        waitContentProcessingStrategy.process();

        IssueMock actualTicket = (IssueMock) trackerServiceMock.getTicket(defaultIssue.getKey());
        List<String> actualComments = trackerServiceMock.getRawComments(actualTicket);

        Assertions.assertThat(actualTicket).isNotNull();
        Assertions.assertThat(actualTicket.getIssueStatus()).isEqualTo(IssueStatus.OPEN);

        Assertions.assertThat(actualComments).isEmpty();
    }

    private void checkCreatedTicket(IssueMock ticket, long epectedPskuId) {
        Assertions.assertThat(ticket.getSummary()).isEqualTo(getExpectedTitle());
        Assertions.assertThat(ticket.getDescription().getOrNull()).isEqualTo(getExpectedDescription(epectedPskuId));
    }

    @Override
    public String getExpectedTitle() {
        return "Контенту на доработку: Category name (1)";
    }

    private String getExpectedDescription(long pskuId) {
        return "Следующий оффер был помечен \"На доработку\"\n" +
                "#|\n" +
                "|| ID psku | Имя psku | Категория | Комментарий ||\n" +
                "|| ((https://mbo-testing.market.yandex.ru/gwt/#modelEditor/entity-id=" +
                pskuId + " " + pskuId + ")) " +
                "| PSKU name " +
                "| ((https://mbo-testing.market.yandex.ru/gwt/#tovarTree/hyperId=1 Category name)) |  ||\n" +
                "|#";
    }

    private String getExpectedSuccessTicketComment() {
        return "Работа по тикету завершена. Робот перестает просматривать этот тикет.";
    }
}
