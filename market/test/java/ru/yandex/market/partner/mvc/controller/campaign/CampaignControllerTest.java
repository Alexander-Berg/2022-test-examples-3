package ru.yandex.market.partner.mvc.controller.campaign;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import org.assertj.core.api.HamcrestCondition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.skyscreamer.jsonassert.Customization;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.abo.api.client.AboPublicRestClient;
import ru.yandex.market.common.balance.model.ClientType;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.StringTestUtil;
import ru.yandex.market.core.balance.BalanceContactService;
import ru.yandex.market.core.balance.BalanceService;
import ru.yandex.market.core.balance.model.ClientInfo;
import ru.yandex.market.core.business.BusinessService;
import ru.yandex.market.core.business.MarketServiceType;
import ru.yandex.market.core.campaign.model.BriefCampaignDto;
import ru.yandex.market.core.datacamp.DataCampService;
import ru.yandex.market.core.partner.placement.PartnerPlacementProgramStatus;
import ru.yandex.market.core.partner.placement.PartnerPlacementProgramType;
import ru.yandex.market.core.state.event.PartnerChangesProtoLBEvent;
import ru.yandex.market.logbroker.LogbrokerEventPublisher;
import ru.yandex.market.logbroker.LogbrokerService;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.mbi.logprocessor.client.MbiLogProcessorClient;
import ru.yandex.market.mbi.util.MbiMatchers;
import ru.yandex.market.mbi.util.MoreMbiMatchers;
import ru.yandex.market.mbi.web.paging.PageTokenHelper;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.common.test.util.JsonTestUtil.assertEquals;
import static ru.yandex.market.core.notification.service.PartnerNotificationApiServiceTest.verifySentNotificationType;
import static ru.yandex.market.mbi.util.MoreMbiMatchers.jsonPropertyMatches;

/**
 * Тесты для {@link CampaignController}.
 *
 * @author Vadim Lyalin
 */
@DbUnitDataSet(before = "CampaignControllerTest.before.csv")
class CampaignControllerTest extends FunctionalTest {
    private static final long AGENCY_UID = 60;
    private static final long AGENCY_CLIENT_ID = 600;
    private static final PageTokenHelper PAGE_TOKEN_HELPER = new PageTokenHelper(OBJECT_MAPPER);
    @Autowired
    private BalanceContactService balanceContactService;

    @Autowired
    private BalanceService balanceService;
    @Autowired
    private LogbrokerEventPublisher<PartnerChangesProtoLBEvent> logbrokerPartnerChangesEventPublisher;
    @Autowired
    private DataCampService dataCampService;
    @Autowired
    private AboPublicRestClient aboPublicRestClient;
    @Autowired
    private LMSClient lmsClient;
    @Autowired
    private MbiLogProcessorClient logProcessorClient;
    @Autowired
    @Qualifier("mboPartnerExportLogbrokerService")
    private LogbrokerService mboPartnerExportLogbrokerService;
    @Autowired
    private BusinessService businessService;

    @BeforeEach
    void setUp() {
        when(logbrokerPartnerChangesEventPublisher.publishEventAsync(any(PartnerChangesProtoLBEvent.class)))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(invocation.getArgument(0)));
        doNothing().when(dataCampService).removeWarehouse(anyLong(), anyLong());
    }

    private static Stream<Arguments> testCheckAuthorityToCampaignArgs() {
        return Stream.of(
                //Нет контакта
                Arguments.of(99, 99, false),
                //Нет клиента
                Arguments.of(10, 99, false),
                //Нет кампании
                Arguments.of(20, 99, false),
                //Нет доступа
                Arguments.of(20, 200, false),
                //client_id кампании и пользователя не совпадает
                Arguments.of(20, 210, false),
                //Агентский без доступа
                Arguments.of(AGENCY_UID, 200, false),
                //Агентский с доступом
                Arguments.of(AGENCY_UID, 210, true),
                //Есть доступ
                Arguments.of(40, 210, true),
                //client_id кампании и пользователя не совпадает, но пользователь БО
                Arguments.of(201, 10003, true),
                // кампания без клиента
                Arguments.of(445, 30002, true)
        );
    }

    private static Stream<Arguments> testSearchCampaignsArgs() {
        return Stream.of(
                // нет контакта
                Arguments.of(99, null, null, null, null, null, null, null, 1, "search-campaign-empty"),
                // контакт без клиента
                Arguments.of(10, null, null, null, List.of(MarketServiceType.SHOP), null, null, null, 1,
                        "search-campaign-empty"),
                // контакт агентский с доступом только к 211, без 212
                Arguments.of(AGENCY_UID, null, null, null, List.of(MarketServiceType.SUPPLIER), null, null,
                        null, 1,
                        "search-campaign-oduvanchik-agency"),
                // контакт с клиентом, но без доступных кампаний
                Arguments.of(20, null, null, null, List.of(MarketServiceType.SHOP, MarketServiceType.SUPPLIER),
                        null, null, null, 1, "search-campaign-empty"),
                // представитель бизнеса
                Arguments.of(30, null, 100, null, List.of(MarketServiceType.SHOP, MarketServiceType.SUPPLIER),
                        null, null, null, 1, "search-campaign-romashka"),
                // представитель бизнеса
                Arguments.of(30, null, 100, null, List.of(MarketServiceType.SHOP, MarketServiceType.SUPPLIER),
                        null, null, null, 2, "search-campaign-romashka2"),
                // фильтр по программе размещения
                Arguments.of(30, null, 100, null, List.of(MarketServiceType.SHOP, MarketServiceType.SUPPLIER),
                        List.of(PartnerPlacementProgramType.CPC), null, null, 1, "search-campaign-romashka" +
                                "-status-sorted"),
                // фильтр по левой программе размещения
                Arguments.of(30, null, 100, null, List.of(MarketServiceType.SHOP, MarketServiceType.SUPPLIER),
                        List.of(PartnerPlacementProgramType.FULFILLMENT), null, null, 1, "search-campaign-empty"),
                // представитель бизнеса с пагинацией и фильтрацией по бизнесу
                Arguments.of(30, null, 100, null, null, null, 210, null, 1,
                        "search-campaign-oduvanchik"),
                // представитель бизнеса с пагинацией и фильтрацией по типу услуги
                Arguments.of(30, null, 100, null, List.of(MarketServiceType.SUPPLIER), null, 0, null, 1,
                        "search-campaign-oduvanchik"),
                // фильтр по левому бизнесу
                Arguments.of(30, null, 999, null, null, null, null, null, 1, "search-campaign-empty"),
                // фильтр по имени поставщика
                Arguments.of(30, null, null, "одув", null, null, 0, null, 1,
                        "search-campaign-oduvanchik"),
                // фильтр по partner_id поставщика
                Arguments.of(30, null, null, "102", null, null, 0, null, 1,
                        "search-campaign-oduvanchik"),
                // главный представитель клиента
                Arguments.of(40, null, null, " ", null, null, null, null, 1, "search-campaign-romashka"),
                // contact link с поиском по подстроке
                Arguments.of(50, null, null, "ромашка", null, null, null, null, 1, "search-campaign-romashka"),
                // contact link с поиском по айди кампании
                Arguments.of(50, null, null, "210", null, null, null, null, 1, "search-campaign-romashka"),
                // агенство
                Arguments.of(AGENCY_UID, null, null, null, null, null, null, null, 1, "search-campaign-romashka"),
                //поиск по существующему campaignId
                Arguments.of(40, 210L, null, " ", null, null, null, null, 1, "search-campaign-romashka"),
                //поиск по несуществующему campaignId
                Arguments.of(40, 211L, null, " ", null, null, null, null, 1, "search-campaign-empty"),
                // contact link без роли
                Arguments.of(70, null, null, " ", null, null, null, null, 1, "search-campaign-empty"),
                //доставка
                Arguments.of(100, null, null, null, null, null, null, null, 1, "search-campaign-roza"),
                //доставка поиск по имени
                Arguments.of(100, null, null, "дост", null, null, null, null, 1, "search-campaign-roza"),
                // Проверяем что не вернем бизнес
                Arguments.of(30, null, null, null, null, null, null, null, 1, "search-campaign-romashka"),
                //магаз с двумя ролями в кампании
                Arguments.of(90, null, 400, null, null, null, null, null, 5, "search-campaign-magaz"),
                //поставщик кроссдок, с фильтром по программе
                Arguments.of(111, null, 501, null, null, List.of(PartnerPlacementProgramType.CROSSDOCK),
                        null, null, 5, "search-campaign-cd"),
                //фулфиллмент без фильтра в статусе «подключается»
                Arguments.of(150, null, 701, null, null,
                        null, null, null, 5, "fulfillment-status"),
                //кроссдок без фильтра в статусе «подключается»
                Arguments.of(160, null, 801, null, null,
                        null, null, null, 5, "crossdock-status"),
                //кроссдок без фильтра в статусе «отключен»
                Arguments.of(170, null, 901, null, null,
                        null, null, null, 5, "crossdock-disabled-status"),
                //кроссдок без фильтра в статусе "количество заказов ограничено"
                Arguments.of(220, null, 10091, null, null,
                        null, null, null, 5, "crossdock-limited-status"),
                //кроссдок без фильтра в статусе "количество заказов ограничено"
                Arguments.of(230, null, 10101, null, null,
                        null, null, null, 5, "crossdock-failed-with-cutoff"),
                //включенный ДСБС
                Arguments.of(190, 1001L, null, null, null,
                        null, null, null, 5, "dsbs-success-status"),
                //подключающийся ДСБС
                Arguments.of(190, 1002L, null, null, null,
                        null, null, null, 5, "dsbs-onboarding-status"),
                //ДСБС на модерации
                Arguments.of(190, 1003L, null, null, null,
                        null, null, null, 5, "dsbs-moderation-status"),
                //ДСБС зафейленный
                Arguments.of(190, 1004L, null, null, null,
                        null, null, null, 5, "dsbs-failed-status"),
                //ДСБС зафейленный
                Arguments.of(190, 1005L, null, null, null,
                        null, null, null, 5, "dsbs-lightcheck-status"),
                //ДСБС, отключенный динамиком
                Arguments.of(190, 1006L, null, null, null,
                        null, null, null, 5, "dsbs-dinamic-status"),
                //проверяем сортировку — «подключающиеся» в начале без переданного ключа по статусу
                Arguments.of(180, null, 911, null, null, List.of(PartnerPlacementProgramType.CROSSDOCK),
                        908, PartnerPlacementProgramStatus.CONFIGURE, 2, "enabling-suppliers-order"),
                //проверяем что строка поиска учитывает и модель и название
                Arguments.of(30, null, 100, "АИН", null, null, 0, null, 10,
                        "search-campaign-by-model"),
                //1 страница корнер кейса для campaigns/with-status, выдается партнер с большим campaignId и менее
                //приоритетным статусом, тк программа приоритетнее
                Arguments.of(301, null, 20202, null, null, null, 0, null, 1,
                        "search-campaign-ordered-1"),
                //2 страница корнер кейса для campaigns/with-status, выдается партнер с меньшим campaignId и более
                //приоритетным статусом, тк программа менее приоритетна
                Arguments.of(301, null, 20202, null, null, null, 20005,
                        PartnerPlacementProgramStatus.CONFIGURE, 1, "search-campaign-ordered-2")
        );
    }

    @Test
    void testGetAllPlacementTypes() {
        String url = baseUrl + "/placementTypes";
        ResponseEntity<String> response = FunctionalTestHelper.get(url);
        assertEquals(response, getClass(), "expected/placementTypes.json");
    }

    /**
     * Тестирует ручку /campaigns поиск доступных пользователю кампаний.
     * За статусом не ходим во внешние системы.
     */
    @SuppressWarnings("checkstyle:ParameterNumber")
    @ParameterizedTest
    @MethodSource("testSearchCampaignsArgs")
    void testSearchCampaignsWithoutExternal(long uid, Long campaignId, Integer businessId, String searchString,
                                            List<MarketServiceType> serviceTypes,
                                            List<PartnerPlacementProgramType> partnerPlacementProgramTypes,
                                            Integer campaignIdForPageToken, PartnerPlacementProgramStatus status,
                                            int limit,
                                            String expectedPath) {
        testSearchCampaigns(uid, campaignId, businessId, searchString, serviceTypes, partnerPlacementProgramTypes,
                campaignIdForPageToken, status, limit, expectedPath);
        Mockito.verifyNoMoreInteractions(aboPublicRestClient, lmsClient, logProcessorClient);
    }

    void testSearchCampaigns(long uid, Long campaignId, Integer businessId, String searchString,
                             List<MarketServiceType> serviceTypes,
                             List<PartnerPlacementProgramType> partnerPlacementProgramTypes,
                             Integer campaignIdForPageToken, PartnerPlacementProgramStatus status, int limit,
                             String expectedPath) {
        String pageToken = campaignIdForPageToken == null
                ? null
                :
                PAGE_TOKEN_HELPER.createNextToken(new CampaignInfoSortKey(campaignIdForPageToken,
                        status == null
                                ? null
                                : status.name(), null));
        when(balanceContactService.getClientIdByUid(AGENCY_UID)).thenReturn(AGENCY_CLIENT_ID);

        String url = baseUrl + "/campaigns?_user_id=" + uid + "&limit=" + limit +
                (campaignId == null
                        ? ""
                        : "&search_campaign_id=" + campaignId) +
                (businessId == null
                        ? ""
                        : "&businessId=" + businessId) +
                (searchString == null
                        ? ""
                        : "&search=" + searchString) +
                (pageToken == null
                        ? ""
                        : "&page_token=" + pageToken);
        if (serviceTypes != null) {
            url = serviceTypes.stream()
                    .map(type -> "&service_type=" + type.name())
                    .reduce(url, String::concat);
        }
        if (partnerPlacementProgramTypes != null) {
            url = partnerPlacementProgramTypes.stream()
                    .map(partnerPlacementProgramType -> "&placement_type=" + partnerPlacementProgramType.name())
                    .reduce(url, String::concat);
        }

        ResponseEntity<String> response = FunctionalTestHelper.get(url);
        assertEquals(response, getClass(), "expected/" + expectedPath + ".json");

        if (businessId != null) {
            response = FunctionalTestHelper.get(url.replace("campaigns?", "businesses/" + businessId
                    + "/campaigns/with-status?"));
            assertEquals("expected/" + expectedPath + "-with-status.json", getClass(), response);
        }
    }

    /**
     * Тестирует ручку /campaigns/full-info поиск доступных пользователю кампаний.
     */
    @SuppressWarnings("checkstyle:ParameterNumber")
    @ParameterizedTest
    @MethodSource("testSearchCampaignsArgs")
    void testSearchFullCampaignInfos(long uid, Long campaignId, Integer businessId, String searchString,
                                     List<MarketServiceType> serviceTypes,
                                     List<PartnerPlacementProgramType> partnerPlacementProgramTypes,
                                     Integer campaignIdForPageToken, PartnerPlacementProgramStatus status,
                                     int limit, String expectedPath) {
        String pageToken = campaignIdForPageToken == null
                ? null
                :
                PAGE_TOKEN_HELPER.createNextToken(new CampaignInfoSortKey(campaignIdForPageToken, status == null
                        ?
                        null
                        : status.name(), null));
        when(balanceContactService.getClientIdByUid(AGENCY_UID)).thenReturn(AGENCY_CLIENT_ID);

        String url = baseUrl + "/campaigns/full-info?_user_id=" + uid + "&limit=" + limit +
                (campaignId == null
                        ? ""
                        : "&search_campaign_id=" + campaignId) +
                (businessId == null
                        ? ""
                        : "&businessId=" + businessId) +
                (searchString == null
                        ? ""
                        : "&search=" + searchString) +
                (pageToken == null
                        ? ""
                        : "&page_token=" + pageToken);
        if (serviceTypes != null) {
            url = serviceTypes.stream()
                    .map(type -> "&service_type=" + type.name())
                    .reduce(url, String::concat);
        }
        if (partnerPlacementProgramTypes != null) {
            url = partnerPlacementProgramTypes.stream()
                    .map(partnerPlacementProgramType -> "&placement_type=" + partnerPlacementProgramType.name())
                    .reduce(url, String::concat);
        }

        ResponseEntity<String> response = FunctionalTestHelper.get(url);

        String expected = StringTestUtil.getString(getClass(), "expected/" + expectedPath + "-full.json");
        assertThat(response).satisfies(HamcrestCondition.matching(
                MoreMbiMatchers.responseBodyMatches(jsonPropertyMatches("result",
                        MbiMatchers.jsonEquals(expected,
                                List.of(new Customization("campaignFullInfos[*].startDate", (v1, v2) -> true)))))
        ));
    }

    @Test
    void testAgencyName() {
        when(balanceContactService.getClientIdByUid(AGENCY_UID)).thenReturn(AGENCY_CLIENT_ID);
        String url = baseUrl + "/campaigns/full-info?_user_id=" + AGENCY_UID + "&limit=1";
        assertEquals(FunctionalTestHelper.get(url), getClass(), "expected/agency-name-check.json");
    }

    @ParameterizedTest
    @MethodSource("testCheckAuthorityToCampaignArgs")
    void testCheckAuthorityToCampaign(long uid, long campaignId, boolean haveAccess) {
        mockBalance();
        String url = baseUrl + "/campaigns/" + campaignId + "/check?_user_id=" + uid;
        ResponseEntity<String> response = FunctionalTestHelper.get(url);
        assertEquals(response, String.valueOf(haveAccess));
    }

    private void mockBalance() {
        when(balanceService.getClient(201L)).thenReturn(new ClientInfo(201L, ClientType.OOO, false, -1));
        when(balanceService.getClient(211L)).thenReturn(new ClientInfo(211L, ClientType.OOO, false, -1));
        when(balanceService.getClient(600L)).thenReturn(new ClientInfo(600L, ClientType.OOO, true, 100));
        when(balanceService.getClient(301L)).thenReturn(new ClientInfo(801L, ClientType.OOO, false, -1));
        when(balanceService.getClientByUid(40)).thenReturn(new ClientInfo(211L, ClientType.OOO, false, -1));
        when(balanceService.getClientByUid(AGENCY_UID)).thenReturn(new ClientInfo(600L, ClientType.OOO, true, 100));
        when(balanceContactService.getClientIdByUid(AGENCY_UID)).thenReturn(AGENCY_CLIENT_ID);
        when(balanceContactService.getClientIdByUid(40)).thenReturn(211L);
    }

    @ParameterizedTest
    @DbUnitDataSet(before = "CampaignControllerTest.testSearchCampaignsByContact.csv")
    @MethodSource("testSearchCampaignsByContactArgs")
    void testSearchCampaignsByContact(long userId, Long contactId, Integer limit, String pageToken,
                                      PartnerPlacementProgramType placementType,
                                      String expectedFileName) {
        String url = baseUrl + "/businesses/100/campaigns/by-contact?_user_id=" + userId
                + (contactId == null
                ? ""
                : "&editable_contact_id=" + contactId)
                + (limit == null
                ? ""
                : "&limit=" + limit)
                + (pageToken == null
                ? ""
                : "&page_token=" + pageToken)
                + (placementType == null
                ? ""
                : "&placement_type=" + placementType.name());

        ResponseEntity<String> response = FunctionalTestHelper.get(url);
        assertEquals(response, getClass(), "expected/" + expectedFileName);
    }

    @ParameterizedTest
    @DbUnitDataSet(before = "CampaignControllerTest.testSearchCampaignsByContact.csv")
    @MethodSource("testNewbieServicesArgs")
    void testNewbieServices(long businessId, long userId, Integer limit, String search,
                            String expectedFileName) {
        when(balanceService.getClientByUid(eq(AGENCY_UID)))
                .thenReturn(new ClientInfo(600L, ClientType.OOO, true, 100));
        when(balanceContactService.getClientIdByUid(eq(AGENCY_UID))).thenReturn(AGENCY_CLIENT_ID);
        String url = baseUrl + "/businesses/" + businessId + "/newbie-services?_user_id=" + userId
                + (limit == null
                ? ""
                : "&limit=" + limit)
                + (search == null
                ? ""
                : "&search=" + search);

        ResponseEntity<String> response = FunctionalTestHelper.get(url);
        assertEquals("expected/" + expectedFileName, getClass(), response);
    }

    @Test
    @DbUnitDataSet(before = "CampaignControllerTest.testDeleteCampaign.before.csv",
            after = "CampaignControllerTest.testDeleteCampaign.after.csv")
    void testDeleteCampaign() {
        String url = baseUrl + "/campaigns/1002?_user_id=190";

        FunctionalTestHelper.delete(url);
        verify(logbrokerPartnerChangesEventPublisher, times(4)).publishEventAsync(any());
        verify(mboPartnerExportLogbrokerService, times(2)).publishEvent(any());
        verifySentNotificationType(partnerNotificationClient, 1, 1639028658L);
    }

    @Test
    @DbUnitDataSet(
            before = "CampaignControllerTest.testDeleteSupplierCampaign.before.csv",
            after = "CampaignControllerTest.testDeleteSupplierCampaign.after.csv"
    )
    void testDeleteSupplierCampaign() {
        String url = baseUrl + "/campaigns/10223222?_user_id=190";

        FunctionalTestHelper.delete(url);

        verify(mboPartnerExportLogbrokerService, times(3)).publishEvent(any());
        assertThat(businessService.getBusiness(99999).isDeleted()).isTrue();
        verifySentNotificationType(partnerNotificationClient, 1, 1639028658L);
    }

    @Test
    @DbUnitDataSet(before = "CampaignControllerTest.testDeleteSupplierCampaign.before.csv",
            after = "CampaignControllerTest.testCloseCampaignWithoutCreationNewOne.after.csv")
    void testCloseCampaignWithoutCreationNewOne() {
        String url = baseUrl + "/campaigns/10223222?_user_id=190";
        FunctionalTestHelper.delete(url);
        verify(mboPartnerExportLogbrokerService, times(3)).publishEvent(any());
        assertThat(businessService.getBusiness(99999).isDeleted()).isTrue();
    }

    private static Stream<Arguments> testNewbieServicesArgs() {
        return Stream.of(
                Arguments.of(911, 180, 2, null, "newbie-services-1.json"),  // пагинация
                Arguments.of(911, 180, null, null, "newbie-services-2.json"), // без пагинации
                Arguments.of(911, 180, null, "Мо", "newbie-services-3.json"),  // со строкой для поиска
                Arguments.of(100, AGENCY_UID, null, null, "newbie-services-4.json")  // агентство

        );
    }

    private static Stream<Arguments> testSearchCampaignsByContactArgs() {
        return Stream.of(
                Arguments.of(50, null, null, null, null, "campaigns-by-contact-5.json"),
                Arguments.of(50, null, null, null, PartnerPlacementProgramType.CROSSDOCK,
                        "campaigns-by-contact-with-placement-type.json"),
                Arguments.of(50, 8L, 2, null, null, "campaigns-by-contact-5-8.json"),
                Arguments.of(50, 8L, 2, PAGE_TOKEN_HELPER.createNextToken(new BriefCampaignDto.ContactSortKey(210,
                                null)), null,
                        "search-campaign-empty.json")
        );
    }
}
