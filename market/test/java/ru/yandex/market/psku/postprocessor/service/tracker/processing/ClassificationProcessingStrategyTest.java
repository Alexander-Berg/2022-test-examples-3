package ru.yandex.market.psku.postprocessor.service.tracker.processing;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.market.mbo.tracker.utils.IssueStatus;
import ru.yandex.market.mbo.tracker.utils.TicketType;
import ru.yandex.market.mbo.users.MboUsers;
import ru.yandex.market.mbo.users.MboUsersService;
import ru.yandex.market.psku.postprocessor.common.BaseDBTest;
import ru.yandex.market.psku.postprocessor.common.db.dao.PskuResultStorageDao;
import ru.yandex.market.psku.postprocessor.common.db.dao.TrackerTicketPskuStatusDao;
import ru.yandex.market.psku.postprocessor.common.db.jooq.enums.PskuStorageState;
import ru.yandex.market.psku.postprocessor.common.db.jooq.enums.PskuTrackerTicketType;
import ru.yandex.market.psku.postprocessor.common.db.jooq.tables.pojos.PskuResultStorage;
import ru.yandex.market.psku.postprocessor.service.tracker.PskuTrackerService;
import ru.yandex.market.psku.postprocessor.service.tracker.mock.IssueMock;
import ru.yandex.market.psku.postprocessor.service.tracker.mock.TrackerServiceMock;
import ru.yandex.market.psku.postprocessor.service.tracker.models.CategoryTrackerInfo;
import ru.yandex.market.psku.postprocessor.service.tracker.models.InfoTicketProcessingResult;
import ru.yandex.market.psku.postprocessor.service.tracker.models.PskuTrackerInfo;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static ru.yandex.market.psku.postprocessor.common.db.jooq.Tables.TRACKER_TICKET_PSKU_STATUS;

public class ClassificationProcessingStrategyTest extends BaseDBTest implements ProcessingStrategyTest {

    @Autowired
    private TrackerTicketPskuStatusDao trackerTicketPskuStatusDao;

    @Autowired
    private PskuResultStorageDao pskuResultStorageDao;

    @Mock
    private MboUsersService mboUsersService;

    private TrackerServiceMock trackerServiceMock = new TrackerServiceMock();

    private ClassificationProcessingStrategy classificationProcessingStrategy;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(mboUsersService.getMboUsers(any(MboUsers.GetMboUsersRequest.class)))
                .thenReturn(getMboUsersResponce());

        classificationProcessingStrategy = new ClassificationProcessingStrategy(
                trackerServiceMock,
                trackerTicketPskuStatusDao,
                mboUsersService,
                MBO_ENTITY_BASE_URL,
                MBOC_SUPPLIERS_BASE_URL,
                COMPONENT_ID);

        PskuResultStorage pskuResultStorage2 = getPskuResultStorage(PSKU_ID_2, PskuStorageState.WRONG_CATEGORY);
        PskuResultStorage pskuResultStorage3 = getPskuResultStorage(PSKU_ID_3, PskuStorageState.WRONG_CATEGORY);

        pskuResultStorageDao.insert(pskuResultStorage2, pskuResultStorage3);
    }

    @Test
    public void testCreateTrackerTicket() {
        CategoryTrackerInfo categoryInfo = new CategoryTrackerInfo(CATEGORY_ID, CATEGORY_NAME);
        String comment = "Неверная категория: по инструкции не относится к этой категории";
        PskuTrackerInfo pskuInfo = new PskuTrackerInfo(PSKU_ID_2, PSKU_NAME, comment,
                new CategoryTrackerInfo(CATEGORY_ID, CATEGORY_NAME));

        IssueMock actualTicket = (IssueMock) classificationProcessingStrategy
                .createTrackerTicket(categoryInfo, Collections.singleton(pskuInfo))
                .orElse(null);

        Assertions.assertThat(actualTicket).isNotNull();
        Assertions.assertThat(actualTicket.getSummary()).isEqualTo(getExpectedTitle());
        Assertions.assertThat(actualTicket.getDescription().getOrNull()).isEqualTo(getExpectedDescription());

        Assertions.assertThat(actualTicket.getCustomField(PskuTrackerService.CATEGORY_FIELD)).isEqualTo(null);
        Assertions.assertThat(actualTicket.getCustomField(PskuTrackerService.TOTAL_PSKU_FIELD)).isEqualTo(1);
        Assertions.assertThat((long[]) actualTicket.getCustomField("components")).contains(COMPONENT_ID);

        int createdRecordInDB = trackerTicketPskuStatusDao.dsl()
                .selectCount()
                .from(TRACKER_TICKET_PSKU_STATUS)
                .where(TRACKER_TICKET_PSKU_STATUS.TICKET_TYPE.eq(PskuTrackerTicketType.CLASSIFITATION))
                .and(TRACKER_TICKET_PSKU_STATUS.IS_CLOSED.eq(false))
                .and(TRACKER_TICKET_PSKU_STATUS.PSKU_ID.in(PSKU_ID_2))
                .and(TRACKER_TICKET_PSKU_STATUS.TRACKER_TICKET_KEY.eq(actualTicket.getKey()))
                .fetchOneInto(Integer.class);

        Assertions.assertThat(createdRecordInDB).isEqualTo(1);
    }

    @Test
    public void testGetTicketType() {
        Assertions.assertThat(classificationProcessingStrategy.getTicketType()).isEqualTo(TicketType.CLASSIFICATION);
    }

    @Test
    public void testCloseTrackerTicket() {
        IssueMock defaultIssue = getDefaultTicket();
        defaultIssue.setKey(defaultIssue.getKey() + 1);
        trackerServiceMock.putTicket(defaultIssue);

        InfoTicketProcessingResult result = getClassificationPskuResult();
        classificationProcessingStrategy.closeTrackerTicket(defaultIssue.getKey(), result);

        IssueMock actualTicket = (IssueMock) trackerServiceMock.getTicket(defaultIssue.getKey());
        List<String> actualComments = trackerServiceMock.getRawComments(actualTicket);

        Assertions.assertThat(actualTicket).isNotNull();
        Assertions.assertThat(actualTicket.getIssueStatus()).isEqualTo(IssueStatus.CLOSE);

        Assertions.assertThat(actualComments.size()).isEqualTo(1);
        Assertions.assertThat(actualComments.get(0)).isEqualTo(getExpectedSuccessTicketComment());
    }

    @Test
    public void testCloseCanceledTrackerTicket() {
        IssueMock defaultIssue = getDefaultTicket();
        defaultIssue.setKey(defaultIssue.getKey() + 2);
        trackerServiceMock.putTicket(defaultIssue);

        InfoTicketProcessingResult result = getClassificationPskuResult();
        result.setCanceled(true);
        classificationProcessingStrategy.closeTrackerTicket(defaultIssue.getKey(), result);

        IssueMock actualTicket = (IssueMock) trackerServiceMock.getTicket(defaultIssue.getKey());
        List<String> actualComments = trackerServiceMock.getRawComments(actualTicket);

        Assertions.assertThat(actualTicket).isNotNull();

        Assertions.assertThat(actualTicket.getIssueStatus()).isEqualTo(IssueStatus.CLOSE);
        Assertions.assertThat(actualTicket.getCustomField(PskuTrackerService.CREATED_FIELD)).isNull();
        Assertions.assertThat(actualTicket.getCustomField(PskuTrackerService.NEW_FIELD)).isNull();
        Assertions.assertThat(actualTicket.getCustomField(PskuTrackerService.NOT_CREATED_FIELD)).isNull();

        Assertions.assertThat(actualComments.size()).isEqualTo(1);
        Assertions.assertThat(actualComments.get(0)).isEqualTo(getExpectedCanceledTicketComment());
    }

    private MboUsers.GetMboUsersResponse getMboUsersResponce() {
        return MboUsers.GetMboUsersResponse.newBuilder()
                .addUser(MboUsers.MboUser.newBuilder()
                        .addInputManagerCategories((int) CATEGORY_ID)
                        .setMboFullname(USER_FULL_NAME))
                .build();
    }

    @Override
    public String getExpectedTitle() {
        return "Классификация PSKU (1 и другие)";
    }

    private String getExpectedDescription() {
        return "<{PSKU на классификацию, 1 шт.\n" +
                "1. **(1 - Category name)**, " +
                "((https://mbo-testing.market.yandex.ru/gwt/#modelEditor/entity-id=2 2)) " +
                "%%PSKU name%%\n" +
                "}>";
    }

    private String getExpectedSuccessTicketComment() {
        return "**Выданные psku обработаны, тикет закрывается автоматически.**\n" +
                "<{Установлена категория у 2 psku:\n" +
                "1. **(1 - Category name)**, " +
                "((https://mbo-testing.market.yandex.ru/gwt/#modelEditor/entity-id=2 2)) %%PSKU name%% \n" +
                "2. **(1 - Category name)**, " +
                "((https://mbo-testing.market.yandex.ru/gwt/#modelEditor/entity-id=3 3)) %%PSKU name%% \n" +
                "}>\n" +
                "<{Недостаточно информации в 1 psku:\n" +
                "1. ((https://mbo-testing.market.yandex.ru/gwt/#modelEditor/entity-id=4 4)) %%PSKU name%%}>";
    }

    private String getExpectedCanceledTicketComment() {
        return "Обработка выданных psku отменена.";
    }

    private InfoTicketProcessingResult getClassificationPskuResult() {
        PskuTrackerInfo pskuInfo1 = new PskuTrackerInfo(PSKU_ID_2, PSKU_NAME,
                new CategoryTrackerInfo(CATEGORY_ID, CATEGORY_NAME));
        PskuTrackerInfo pskuInfo2 = new PskuTrackerInfo(PSKU_ID_3, PSKU_NAME,
                new CategoryTrackerInfo(CATEGORY_ID, CATEGORY_NAME));

        InfoTicketProcessingResult result = new InfoTicketProcessingResult();
        result.setUpdatedCategoryPskus(Arrays.asList(pskuInfo1, pskuInfo2));
        result.setNeedInfoPskus(Collections.singleton(new PskuTrackerInfo(PSKU_ID_4, PSKU_NAME)));

        return result;
    }
}
