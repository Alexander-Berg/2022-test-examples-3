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
import ru.yandex.market.psku.postprocessor.common.db.jooq.enums.GenerationTaskType;
import ru.yandex.market.psku.postprocessor.common.db.jooq.enums.PskuStorageState;
import ru.yandex.market.psku.postprocessor.common.db.jooq.enums.PskuTrackerTicketType;
import ru.yandex.market.psku.postprocessor.common.db.jooq.tables.pojos.PskuResultStorage;
import ru.yandex.market.psku.postprocessor.service.tracker.PskuTrackerService;
import ru.yandex.market.psku.postprocessor.service.tracker.mock.IssueMock;
import ru.yandex.market.psku.postprocessor.service.tracker.mock.TrackerServiceMock;
import ru.yandex.market.psku.postprocessor.service.tracker.models.CategoryTrackerInfo;
import ru.yandex.market.psku.postprocessor.service.tracker.models.InfoTicketProcessingResult;
import ru.yandex.market.psku.postprocessor.service.tracker.models.MskuTrackerInfo;
import ru.yandex.market.psku.postprocessor.service.tracker.models.PskuTrackerInfo;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static ru.yandex.market.psku.postprocessor.common.db.jooq.Tables.TRACKER_TICKET_PSKU_STATUS;

public class MskuFromPskuGenProcessingStrategyTest extends BaseDBTest implements ProcessingStrategyTest {

    @Autowired
    private TrackerTicketPskuStatusDao trackerTicketPskuStatusDao;

    @Autowired
    private PskuResultStorageDao pskuResultStorageDao;

    @Mock
    private MboUsersService mboUsersService;

    private TrackerServiceMock trackerServiceMock = new TrackerServiceMock();

    private MskuFromPskuGenProcessingStrategy mskuFromPskuGenProcessingStrategy;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(mboUsersService.getMboUsers(any(MboUsers.GetMboUsersRequest.class)))
                .thenReturn(getMboUsersResponce());

        mskuFromPskuGenProcessingStrategy = new MskuFromPskuGenProcessingStrategy(
                trackerServiceMock,
                trackerTicketPskuStatusDao,
                mboUsersService,
                MBO_ENTITY_BASE_URL,
                MBOC_SUPPLIERS_BASE_URL,
                COMPONENT_ID,
                GenerationTaskType.CLUSTER);

        PskuResultStorage pskuResultStorage2 = getPskuResultStorage(PSKU_ID_2, PskuStorageState.FOR_REMAPPING);
        PskuResultStorage pskuResultStorage3 = getPskuResultStorage(PSKU_ID_3, PskuStorageState.FOR_REMAPPING);

        pskuResultStorageDao.insert(pskuResultStorage2, pskuResultStorage3);
    }

    @Test
    public void testCreateTrackerTicket() {
        CategoryTrackerInfo categoryInfo = new CategoryTrackerInfo(CATEGORY_ID, CATEGORY_NAME);
        PskuTrackerInfo pskuInfo = new PskuTrackerInfo(PSKU_ID_2, PSKU_NAME);

        IssueMock actualTicket = (IssueMock) mskuFromPskuGenProcessingStrategy
                .createTrackerTicket(categoryInfo, Collections.singleton(pskuInfo))
                .orElse(null);

        Assertions.assertThat(actualTicket).isNotNull();
        Assertions.assertThat(actualTicket.getSummary()).isEqualTo(getExpectedTitle());
        Assertions.assertThat(actualTicket.getDescription().getOrNull()).isEqualTo(getExpectedDescription());

        Assertions.assertThat(actualTicket.getCustomField(PskuTrackerService.CATEGORY_FIELD)).isEqualTo(CATEGORY_ID);
        Assertions.assertThat(actualTicket.getCustomField(PskuTrackerService.TOTAL_PSKU_FIELD)).isEqualTo(1);
        Assertions.assertThat((long[]) actualTicket.getCustomField("components")).contains(COMPONENT_ID);

        int createdRecordInDB = trackerTicketPskuStatusDao.dsl()
                .selectCount()
                .from(TRACKER_TICKET_PSKU_STATUS)
                .where(TRACKER_TICKET_PSKU_STATUS.TICKET_TYPE.eq(PskuTrackerTicketType.MSKU_FROM_PSKU_GEN))
                .and(TRACKER_TICKET_PSKU_STATUS.IS_CLOSED.eq(false))
                .and(TRACKER_TICKET_PSKU_STATUS.PSKU_ID.in(PSKU_ID_2))
                .and(TRACKER_TICKET_PSKU_STATUS.TRACKER_TICKET_KEY.eq(actualTicket.getKey()))
                .fetchOneInto(Integer.class);

        Assertions.assertThat(createdRecordInDB).isEqualTo(1);
    }

    @Test
    public void testGetTicketType() {
        Assertions.assertThat(mskuFromPskuGenProcessingStrategy.getTicketType()).isEqualTo(TicketType.MATCHING);
    }

    @Test
    public void testCloseTrackerTicket() {
        IssueMock defaultIssue = getDefaultTicket();
        defaultIssue.setKey(defaultIssue.getKey() + 1);
        trackerServiceMock.putTicket(defaultIssue);

        InfoTicketProcessingResult result = getClusterizationPskuResult();
        mskuFromPskuGenProcessingStrategy.closeTrackerTicket(defaultIssue.getKey(), result);

        IssueMock actualTicket = (IssueMock) trackerServiceMock.getTicket(defaultIssue.getKey());
        List<String> actualComments = trackerServiceMock.getRawComments(actualTicket);

        Assertions.assertThat(actualTicket).isNotNull();

        Assertions.assertThat(actualTicket.getIssueStatus()).isEqualTo(IssueStatus.CLOSE);
        Assertions.assertThat(actualTicket.getCustomField(PskuTrackerService.CREATED_FIELD)).isEqualTo(2);
        Assertions.assertThat(actualTicket.getCustomField(PskuTrackerService.NEW_FIELD)).isEqualTo(1);
        Assertions.assertThat(actualTicket.getCustomField(PskuTrackerService.NOT_CREATED_FIELD)).isEqualTo(1);

        Assertions.assertThat(actualComments.size()).isEqualTo(1);
        Assertions.assertThat(actualComments.get(0)).isEqualTo(getExpectedSuccessTicketComment());
    }

    @Test
    public void testCloseCanceledTrackerTicket() {
        IssueMock defaultIssue = getDefaultTicket();
        defaultIssue.setKey(defaultIssue.getKey() + 2);
        trackerServiceMock.putTicket(defaultIssue);

        InfoTicketProcessingResult result = getClusterizationPskuResult();
        result.setCanceled(true);
        mskuFromPskuGenProcessingStrategy.closeTrackerTicket(defaultIssue.getKey(), result);

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
        return "Создание MSKU из PSKU (1 - Category name) [cluster]";
    }

    private String getExpectedDescription() {
        return "#|\n" +
                "|| Менеджер группы ввода | Всего PSKU ||\n" +
                "|| User full name | 1 ||\n" +
                "|#\n" +
                "<{Выдано PSKU, 1 шт.\n" +
                "1. ((https://mbo-testing.market.yandex.ru/gwt/#modelEditor/entity-id=2 2)) %%PSKU name%%}>";
    }

    private String getExpectedSuccessTicketComment() {
        return "**Выданные psku обработаны, тикет закрывается автоматически.**\n" +
                "<{Создано 1 msku:\n" +
                "1. ((https://mbo-testing.market.yandex.ru/gwt/#modelEditor/entity-id=2 2))-%%PSKU name%% " +
                "((https://mbo-testing.market.yandex.ru/gwt/#modelEditor/entity-id=3 3))-%%PSKU name%% => " +
                "((https://mbo-testing.market.yandex.ru/gwt/#modelEditor/entity-id=7 7)) %%MSKU name%%\n" +
                "}>\n" +
                "<{Будут перенесены маппинги на следующих 1 офферах:\n" +
                "1. ((http://mbo-http-exporter.tst.vs.market.yandex.net:8084/mboUsers/5/?search=6 6))\n" +
                "}>\n" +
                "<{Недостаточно информации в 1 psku:\n" +
                "1. ((https://mbo-testing.market.yandex.ru/gwt/#modelEditor/entity-id=4 4)) %%PSKU name%%}>";
    }

    private String getExpectedCanceledTicketComment() {
        return "Обработка выданных psku отменена.";
    }

    private InfoTicketProcessingResult getClusterizationPskuResult() {
        MskuTrackerInfo mskuInfo = new MskuTrackerInfo(MSKU_ID, MSKU_NAME);
        PskuTrackerInfo pskuInfo1 = new PskuTrackerInfo(PSKU_ID_2, PSKU_NAME);
        PskuTrackerInfo pskuInfo2 = new PskuTrackerInfo(PSKU_ID_3, PSKU_NAME);
        Map<MskuTrackerInfo, Collection<PskuTrackerInfo>> pskusByCreatedMsku = new HashMap<>();
        pskusByCreatedMsku.put(mskuInfo, Arrays.asList(pskuInfo1, pskuInfo2));

        Map<Long, Set<String>> movedMappingsBySupplier = new HashMap<>();
        movedMappingsBySupplier.put(SUPLIER_ID, Collections.singleton(MAPPING_ID));

        InfoTicketProcessingResult result = new InfoTicketProcessingResult();
        result.setPskusMappingsByMsku(pskusByCreatedMsku);
        result.setShopSkuBySupplier(movedMappingsBySupplier);
        result.setNeedInfoPskus(Collections.singleton(new PskuTrackerInfo(PSKU_ID_4, PSKU_NAME)));

        return result;
    }
}
