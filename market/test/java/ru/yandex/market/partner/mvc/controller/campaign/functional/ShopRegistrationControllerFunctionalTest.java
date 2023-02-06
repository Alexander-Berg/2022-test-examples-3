package ru.yandex.market.partner.mvc.controller.campaign.functional;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

import io.grpc.stub.StreamObserver;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.unitils.reflectionassert.ReflectionAssert;

import ru.yandex.market.checkout.checkouter.client.CheckouterAPI;
import ru.yandex.market.checkout.checkouter.client.CheckouterShopApi;
import ru.yandex.market.common.balance.model.ClientType;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.core.balance.BalanceContactService;
import ru.yandex.market.core.balance.BalanceService;
import ru.yandex.market.core.balance.model.ClientInfo;
import ru.yandex.market.core.billing.CampaignSpendingService;
import ru.yandex.market.core.business.BusinessDao;
import ru.yandex.market.core.business.MarketServiceType;
import ru.yandex.market.core.campaign.model.CampaignInfo;
import ru.yandex.market.core.campaign.model.CampaignType;
import ru.yandex.market.core.campaign.model.PartnerId;
import ru.yandex.market.core.contact.model.ContactEmail;
import ru.yandex.market.core.contact.model.ContactWithEmail;
import ru.yandex.market.core.delivery.AsyncTarifficatorService;
import ru.yandex.market.core.ds.model.DatasourceInfo;
import ru.yandex.market.core.matchers.HttpClientErrorMatcher;
import ru.yandex.market.core.notification.service.NotificationSendContext;
import ru.yandex.market.core.notification.service.NotificationService;
import ru.yandex.market.core.partner.PartnerService;
import ru.yandex.market.core.partner.placement.PartnerPlacementProgramType;
import ru.yandex.market.core.passport.PassportService;
import ru.yandex.market.core.passport.model.EmailInfo;
import ru.yandex.market.core.passport.model.UserInfo;
import ru.yandex.market.core.protocol.model.ActionType;
import ru.yandex.market.core.xml.impl.NamedContainer;
import ru.yandex.market.id.ContactModificationResponse;
import ru.yandex.market.id.MarketIdServiceGrpc;
import ru.yandex.market.mbi.environment.EnvironmentService;
import ru.yandex.market.mbi.util.MbiMatchers;
import ru.yandex.market.mbi.util.MoreMbiMatchers;
import ru.yandex.market.partner.campaign.impl.ShopRegistrationServiceImpl;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.common.util.collections.CollectionFactory.list;
import static ru.yandex.market.core.matchers.HttpClientErrorMatcher.errorListMatchesInAnyOrder;
import static ru.yandex.market.core.matchers.HttpClientErrorMatcher.errorMatches;
import static ru.yandex.market.core.matchers.HttpClientErrorMatcher.hasErrorCode;
import static ru.yandex.market.core.notification.service.PartnerNotificationApiServiceTest.verifySentNotificationType;

/**
 * Функциональные тесты для регистрации магазина.
 * {@link ru.yandex.market.partner.mvc.controller.campaign.ShopRegistrationController}
 *
 * @author Kirill Lakhtin (klaktin@yandex-team.ru)
 */
@DbUnitDataSet(before = "ShopRegistrationControllerFunctionalTest.environment.before.csv")
class ShopRegistrationControllerFunctionalTest extends FunctionalTest {

    private static final String FILE_PREFIX = "/mvc/campaign/";
    private static final long USER_ID = 67282295L;
    private static final long EFFECTIVE_USER_ID = 67282296L;
    private static final long CLIENT_ID = 2334245L;

    @Autowired
    private CampaignSpendingService campaignSpendingService;

    @Autowired
    private CheckouterAPI checkouterClient;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private BalanceContactService balanceContactService;

    @Autowired
    private BalanceService balanceService;

    @Autowired
    private PassportService passportService;

    @Autowired
    @Qualifier("environmentService")
    private EnvironmentService environmentService;

    @Autowired
    private PartnerService partnerService;

    @Autowired
    private BusinessDao businessDao;

    @Autowired
    private MarketIdServiceGrpc.MarketIdServiceImplBase marketIdServiceImplBase;

    @Autowired
    private AsyncTarifficatorService asyncTarifficatorService;

    private static Stream<Arguments> provideGetSputnikData() {
        return Stream.of(
                Arguments.of(11, PartnerPlacementProgramType.DROPSHIP_BY_SELLER, "{\n" +
                        "  \"parentPartnerId\": 10,\n" +
                        "  \"childPartnerId\": 21,\n" +
                        "  \"programType\": \"DROPSHIP_BY_SELLER\"\n" +
                        "}"),
                Arguments.of(21, PartnerPlacementProgramType.DROPSHIP_BY_SELLER, "{\n" +
                        "  \"parentPartnerId\": 20,\n" +
                        "  \"programType\": \"DROPSHIP_BY_SELLER\"\n" +
                        "}")
        );
    }

    @BeforeEach
    public void prepareMocks() {
        doAnswer(invocation -> {
            StreamObserver<ContactModificationResponse> responseObserver = invocation.getArgument(1);
            responseObserver.onNext(
                    ContactModificationResponse
                            .newBuilder()
                            .setSuccess(true)
                            .setMessage("OK")
                            .build());
            responseObserver.onCompleted();
            return null;
        }).when(marketIdServiceImplBase).updateConatctAccesses(any(), any());
    }

    @BeforeEach
    void onBefore() {
        when(balanceContactService.getClientIdByUid(USER_ID)).thenReturn(-1L);
        when(balanceService.createClient(any(), anyLong(), anyLong())).thenReturn(CLIENT_ID);
        when(balanceService.getClient(CLIENT_ID)).thenReturn(new ClientInfo(CLIENT_ID, ClientType.OOO));

        environmentService.setValue(ShopRegistrationServiceImpl.ENV_CAMPAIGNS_LIMIT_PARAM, "5");
        environmentService.removeAllValues("shop.registration.valid-offer-percent");

        when(checkouterClient.shops()).thenReturn(mock(CheckouterShopApi.class));
    }

    /**
     * Тест проверяет ручку, проверяющую что присланный домен входит в список популярных.
     * Список популярных доменов хранится в танкере
     */
    @Test
    @DbUnitDataSet(before = "ForbiddenDomains.csv")
    void testIsInvalidDomain() {
        JsonTestUtil.assertEquals(FunctionalTestHelper.get(getIsValidDomainUrl(), "vk.com"), "false");
    }

    /**
     * Тест проверяет ручку, проверяющую что присланный домен не входит в список популярных.
     * Список популярных доменов хранится в танкере
     */
    @Test
    @DbUnitDataSet(before = "ForbiddenDomains.csv")
    void testIsValidDomain() {
        JsonTestUtil.assertEquals(FunctionalTestHelper.get(getIsValidDomainUrl(), "avk.com"), "true");
    }

    /**
     * Проверяет, что произвольный урл проходит валидацию только для подтипа SMB.
     */
    @Test
    @DbUnitDataSet(before = "ForbiddenDomains.csv")
    void testUrlIsNotDomain() {
        JsonTestUtil.assertEquals(FunctionalTestHelper.get(getIsValidDomainUrl(), "https://www.instagram.com/parashop"),
                "false");
        JsonTestUtil.assertEquals(FunctionalTestHelper.get(getIsValidDomainUrl() + "&shop_subtype=REGULAR",
                "https://www.instagram.com/parashop"), "false");
        JsonTestUtil.assertEquals(FunctionalTestHelper.get(getIsValidDomainUrl() + "&shop_subtype=SMB",
                "https://www.instagram.com/parashop"), "true");
    }

    private String getIsValidDomainUrl() {
        return baseUrl + "/register-shop/is-valid-domain?domain={domain}" +
                "&_user_id=" + USER_ID +
                "&euid=" + EFFECTIVE_USER_ID;
    }

    /**
     * Проверяет корректную регистрацию SMB магазина.
     */
    @Test
    @DbUnitDataSet(before = "LocalShopRegistration.before.csv", after = "SmbShopRegistration.after.csv")
    void testSmbShopRegistration() {
        ResponseEntity<String> response = sendRequestFromFile("smb_shop.json");
        assertThat(response, MoreMbiMatchers.responseBodyMatches(MbiMatchers.jsonPropertyEquals("result", "{" +
                "\"datasourceId\": 1," +
                "\"campaignId\": 1," +
                "\"agencyId\": 0," +
                "\"managerId\": -2," +
                "\"businessId\": 2," +
                "\"ownerId\": " + USER_ID +
                "}")));
        verify(asyncTarifficatorService).syncShopMetaData(eq(1L), eq(ActionType.CAMPAIGN_CREATION));
        verifySentNotificationType(partnerNotificationClient, 1, 1590582727L);
    }

    /**
     * Проверяет корректную регистрацию SMB-магазина c минимальным допустимым набором полей.
     */
    @Test
    @DbUnitDataSet(before = "NewContactRegistration.before.csv", after = "SmbShopMinimalRegistration.after.csv")
    void testSmbShopMinimalRegistration() {
        when(passportService.getUserInfo(USER_ID))
                .thenReturn(new UserInfo(USER_ID, "Иванов Иван", null, "spbtester"));

        when(passportService.getEmails(USER_ID))
                .thenReturn(List.of(
                        new EmailInfo("spbtester@yandex.ru", true),
                        new EmailInfo("passportEmail@yandex.ru", false)
                ));

        ResponseEntity<String> response = sendRequestFromFile("smb_shop_minimal.json");
        assertThat(response, MoreMbiMatchers.responseBodyMatches(MbiMatchers.jsonPropertyEquals("result", "{" +
                "\"datasourceId\": 1," +
                "\"campaignId\": 1," +
                "\"agencyId\": 0," +
                "\"managerId\": -2," +
                "\"businessId\": 2," +
                "\"ownerId\": " + USER_ID +
                "}")));

        verify(passportService).getEmails(USER_ID);
        verify(passportService).getUserInfo(USER_ID);
        verifyNoMoreInteractions(passportService);
        verify(asyncTarifficatorService).syncShopMetaData(eq(1L), eq(ActionType.CAMPAIGN_CREATION));
    }

    /**
     * Проверяет, что опциональные для SMB-магазинов поля ({@code regionId, domain})
     * по-прежнему обязательны для обычных белых магазинов.
     */
    @Test
    @DbUnitDataSet(before = "LocalShopRegistration.before.csv")
    void testRegionAndDomainRequiredForRegularShops() {
        sendBadRequest("regular_as_smb.json", e -> {
            assertThat(e, hasErrorCode(HttpStatus.BAD_REQUEST));
            assertThat(e, errorListMatchesInAnyOrder(
                    errorMatches("BAD_PARAM", "domain", "MISSING"),
                    errorMatches("BAD_PARAM", "localRegionId", "MISSING")
            ));
        });
    }

    /**
     * Проверяет, что SMB-магазин при регистрации не может указать произвольный регион-страну.
     */
    @Test
    @DbUnitDataSet(before = "LocalShopRegistration.before.csv")
    void testNonRussianRegionNotAllowedForSmb() {
        sendBadRequest("smb_shop_foreign_region.json", e -> {
            assertThat(e, hasErrorCode(HttpStatus.BAD_REQUEST));
            assertThat(e, errorListMatchesInAnyOrder(
                    errorMatches("BAD_PARAM", "regionId", "INVALID")
            ));
        });
    }

    /**
     * Тест проверяет поведение, когда привязываемый контакт клиента в балансе уже привязан к кампании другого
     * клиента в базе MBI.
     * Случай самоходного магазина.
     */
    @Test
    @DbUnitDataSet(before = "NonSubclientContactAlreadyLinked.before.csv")
    void testNonSubclientRegistration_ContactAlreadyLinkedToAnotherClient() {
        final long ownerClientId = 555L;
        final long ownerUid = USER_ID;
        final String ownerLogin = "test-owner";
        ClientInfo ownerClientInfo = new ClientInfo(555L, ClientType.PHYSICAL);

        final long otherUid = 99999999L;

        when(passportService.findUid(ownerLogin)).thenReturn(ownerUid);
        when(balanceService.getClient(ownerClientId)).thenReturn(ownerClientInfo);
        when(balanceContactService.getUidsByClient(ownerClientId)).thenReturn(Arrays.asList(ownerUid, otherUid));

        sendBadRequest("known_owner.json", e -> {
            assertThat(e, hasErrorCode(HttpStatus.BAD_REQUEST));
            assertThat(e, errorListMatchesInAnyOrder(errorMatches("CONTACT_LINKED_TO_ANOTHER_CLIENT", null, null)));
        });
    }

    /**
     * Тест проверяет поведение, когда привязываемый контакт клиента в балансе уже привязан к кампании другого
     * клиента в базе MBI.
     * Случай магазина под управлением агентства.
     */
    @Test
    @DbUnitDataSet(before = "ContactAlreadyLinkedToAnotherClient.before.csv")
    void testSubclientRegistration_ContactAlreadyLinkedToAnotherClient() {
        final long agencyClientId = 777L;
        ClientInfo agencyClientInfo = new ClientInfo(agencyClientId, ClientType.PHYSICAL, true, agencyClientId);

        final long ownerClientId = 555L;
        final long ownerUid = 88888888L;
        final String ownerLogin = "test-owner";
        ClientInfo ownerClientInfo = new ClientInfo(ownerClientId, ClientType.PHYSICAL, false, agencyClientId);

        final long otherUid = 99999999L;

        when(balanceContactService.getClientIdByUid(USER_ID)).thenReturn(agencyClientId);
        when(passportService.findUid(ownerLogin)).thenReturn(ownerUid);
        when(balanceService.getClientByUid(USER_ID)).thenReturn(agencyClientInfo);
        when(balanceService.getClientByUid(ownerClientId)).thenReturn(ownerClientInfo);
        when(balanceService.getClient(agencyClientId)).thenReturn(agencyClientInfo);
        when(balanceService.getClient(ownerClientId)).thenReturn(ownerClientInfo);
        when(balanceContactService.getUidsByClient(ownerClientId)).thenReturn(Arrays.asList(ownerUid, otherUid));

        sendBadRequest("known_owner.json", e -> {
            assertThat(e, hasErrorCode(HttpStatus.BAD_REQUEST));
            assertThat(e, errorListMatchesInAnyOrder(errorMatches("CONTACT_LINKED_TO_ANOTHER_CLIENT", null, null)));
        });
    }

    /**
     * Тест проверяет поведение, когда привязываемый контакт клиента в балансе уже привязан к кампании другого
     * клиента в базе MBI.
     * Случай магазина под управлением агентства.
     */
    @Test
    @DbUnitDataSet(before = "BalanceClientNotRegistered.before.csv")
    void testBalanceClientNotRegistered() {
        final long agencyClientId = 777L;
        final long ownerClientId = 555L;
        final long ownerUid = 88888888L;
        final String ownerLogin = "test-owner";
        ClientInfo agencyClient = new ClientInfo(agencyClientId, ClientType.PHYSICAL, true, agencyClientId);

        when(balanceContactService.getClientIdByUid(USER_ID)).thenReturn(agencyClientId);
        when(passportService.findUid(ownerLogin)).thenReturn(ownerUid);
        when(balanceService.getClientByUid(USER_ID)).thenReturn(agencyClient);
        when(balanceService.getClient(agencyClientId)).thenReturn(agencyClient);
        when(balanceService.getClient(ownerClientId)).thenReturn(null);

        sendBadRequest("known_owner.json", e -> {
            assertThat(e, hasErrorCode(HttpStatus.FORBIDDEN));
            assertThat(e, errorListMatchesInAnyOrder(errorMatches("CLIENT_NOT_REGISTERED", null, null)));
        });
    }

    /**
     * Тест проверяет поведение, когда агентство в запросе на регистрацию
     * передает логин владельца магазина, привязанному к другому агенту.
     */
    @Test
    @DbUnitDataSet(before = "ContactAlreadyLinkedToAnotherClient.before.csv")
    void testOwnerBoundToAnotherAgency() {
        final long agencyClientId = 777L;
        ClientInfo agencyClientInfo = new ClientInfo(agencyClientId, ClientType.PHYSICAL, true, agencyClientId);

        final long otherAgencyId = 888L;

        final long ownerClientId = 555L;
        final long ownerUid = 88888888L;
        final String ownerLogin = "test-owner";
        ClientInfo ownerClientInfo = new ClientInfo(ownerClientId, ClientType.PHYSICAL, false, otherAgencyId);

        when(balanceContactService.getClientIdByUid(USER_ID)).thenReturn(agencyClientId);
        when(passportService.findUid(ownerLogin)).thenReturn(ownerUid);
        when(balanceService.getClientByUid(USER_ID)).thenReturn(agencyClientInfo);
        when(balanceService.getClientByUid(ownerUid)).thenReturn(ownerClientInfo);

        sendBadRequest("known_owner.json", e -> {
            assertThat(e, hasErrorCode(HttpStatus.FORBIDDEN));
            assertThat(e, errorListMatchesInAnyOrder(errorMatches("NOT_AN_OWNER", null, null)));
        });
    }

    /**
     * Тест проверяет регистрацию онлайн FMCG магазина.
     */
    @Test
    @DbUnitDataSet(before = "FMCGShopRegistration.before.csv", after = "OnlineFMCGShopRegistration.after.csv")
    void testOnlineFmcgShopRegistration() {
        ResponseEntity<String> response = sendRequestFromFile("online_fmcg_shop.json");
        assertThat(response, MoreMbiMatchers.responseBodyMatches(MbiMatchers.jsonPropertyEquals("result", "{" +
                "\"datasourceId\": 1," +
                "\"campaignId\": 1," +
                "\"agencyId\": 0," +
                "\"managerId\": -2," +
                "\"ownerId\": " + USER_ID +
                "}")));
    }

    /**
     * Тест проверяет регистрацию оффлайн FMCG магазина.
     */
    @Test
    @DbUnitDataSet(before = "FMCGShopRegistration.before.csv", after = "OfflineFMCGShopRegistration.after.csv")
    void testOfflineFmcgShopRegistration() {
        ResponseEntity<String> response = sendRequestFromFile("offline_fmcg_shop.json");
        assertThat(response, MoreMbiMatchers.responseBodyMatches(MbiMatchers.jsonPropertyEquals("result", "{" +
                "\"datasourceId\": 1," +
                "\"campaignId\": 1," +
                "\"agencyId\": 0," +
                "\"managerId\": -2," +
                "\"ownerId\": " + USER_ID +
                "}")));
    }

    /**
     * Тест проверяет регистрацию российского (не глобал) магазина.
     * Тест проверяет возвращаемые из ручки значения и наличие в итоге в БД нужных записей: датасорса, кампании,
     * кат-оффов, параметров и тд.
     * В тесте задано кастомное значение параметра
     * {@link ru.yandex.market.core.param.model.ParamType#VALID_OFFER_PERCENT}.
     */
    @Test
    @DbUnitDataSet(before = "LocalShopRegistration.before.csv",
            after = "LocalShopRegistration.after.csv")
    void testLocalShopRegistration() {
        environmentService.setValues("shop.registration.valid-offer-percent", List.of("30"));

        ResponseEntity<String> response = sendRequestFromFile("local_shop.json");
        assertThat(response, MoreMbiMatchers.responseBodyMatches(MbiMatchers.jsonPropertyEquals("result", "{" +
                "\"datasourceId\": 1," +
                "\"businessId\": 2," +
                "\"campaignId\": 1," +
                "\"agencyId\": 0," +
                "\"managerId\": -2," +
                "\"ownerId\": " + USER_ID +
                "}")));

        verify(asyncTarifficatorService).syncShopMetaData(eq(1L), eq(ActionType.CAMPAIGN_CREATION));
        verifySentNotificationType(partnerNotificationClient, 1, 95L);
    }

    /**
     * Тест проверяет регистрацию российского (не глобал) магазина.
     * Тест проверяет возвращаемые из ручки значения и наличие в итоге в БД нужных записей: датасорса, кампании,
     * кат-оффов, параметров и тд.
     * В тесте задано кастомное значение параметра
     * {@link ru.yandex.market.core.param.model.ParamType#VALID_OFFER_PERCENT},
     * а также включен флаг единого каталога для ADV, поэтому параметр единого каталога должен проставиться
     */
    @Test
    @DbUnitDataSet(before = "LocalShopRegistration.before.csv",
            after = "LocalShopRegistrationWithAdvUnitedCatalogFlag.after.csv")
    void testLocalShopRegistrationWithAdvUnitedCatalogFlag() {
        environmentService.setValues("shop.registration.valid-offer-percent", List.of("30"));

        ResponseEntity<String> response = sendRequestFromFile("local_shop.json");
        assertThat(response, MoreMbiMatchers.responseBodyMatches(MbiMatchers.jsonPropertyEquals("result", "{" +
                "\"datasourceId\": 1," +
                "\"businessId\": 2," +
                "\"campaignId\": 1," +
                "\"agencyId\": 0," +
                "\"managerId\": -2," +
                "\"ownerId\": " + USER_ID +
                "}")));

        verify(asyncTarifficatorService).syncShopMetaData(eq(1L), eq(ActionType.CAMPAIGN_CREATION));
        verifySentNotificationType(partnerNotificationClient, 1, 95L);
    }

    /**
     * Тест проверяет регистрацию российского магазина на субклиента агентства.
     * Тест проверяет возвращаемые из ручки значения и наличие в итоге в БД нужных записей: датасорса, кампании,
     * кат-оффов, параметров и тд, и запись истории связки магазина с агентством
     */
    @Test
    @DbUnitDataSet(before = "LocalShopRegistrationByAgency.before.csv",
            after = "LocalShopRegistrationByAgency.after.csv")
    void testLocalShopRegistrationByAgency() {
        long currentUser = 100500L;
        subclientRegMock(currentUser);

        ResponseEntity<String> response = sendRequestFromFile("known_owner.json");
        assertThat(response, MoreMbiMatchers.responseBodyMatches(MbiMatchers.jsonPropertyEquals("result", "{" +
                "\"datasourceId\": 1," +
                "\"campaignId\": 1," +
                "\"agencyId\": 777," +
                "\"managerId\": -2," +
                "\"businessId\": 10," +
                "\"ownerId\": " + currentUser +
                "}")));

        verify(asyncTarifficatorService).syncShopMetaData(eq(1L), eq(ActionType.CAMPAIGN_CREATION));
    }

    /**
     * Тест проверяет регистрацию магазина на субклиента агентства в существующий бизнес по клиенту и домену.
     */
    @Test
    @DbUnitDataSet(before = "BusinessOnSubclientRegistration.before.csv",
            after = "BusinessOnSubclientRegistration.after.csv")
    void testBusinessOnRegistrationByAgency() {
        long currentUser = 100500L;
        subclientRegMock(currentUser);

        ResponseEntity<String> response = sendRequestFromFile("known_owner_without_business.json");
        assertThat(response, MoreMbiMatchers.responseBodyMatches(
                MbiMatchers.jsonPropertyEquals("result",
                        "{" +
                                "\"datasourceId\": 1," +
                                "\"campaignId\": 1," +
                                "\"agencyId\": 777," +
                                "\"managerId\": -2," +
                                "\"businessId\": 2," +
                                "\"ownerId\": " + currentUser +
                                "}")));

        verify(asyncTarifficatorService).syncShopMetaData(eq(1L), eq(ActionType.CAMPAIGN_CREATION));
    }

    private void subclientRegMock(long currentUser) {
        environmentService.setValues("shop.registration.valid-offer-percent", List.of("30"));

        when(balanceContactService.getClientIdByUid(USER_ID)).thenReturn(777L);
        when(balanceContactService.getClientIdByUid(currentUser)).thenReturn(666L);
        when(balanceService.getClient(777L)).thenReturn(new ClientInfo(777L, ClientType.OOO, true, 777L));
        when(balanceService.getClient(666L)).thenReturn(new ClientInfo(666L, ClientType.OOO, false, 777L));
        when(balanceService.getClientByUid(USER_ID)).thenReturn(new ClientInfo(777L, ClientType.OOO, true, 777L));
        when(balanceService.getClientByUid(currentUser)).thenReturn(new ClientInfo(666L, ClientType.OOO, false, 777L));
        when(passportService.findUid("test-owner")).thenReturn(currentUser);
    }

    /**
     * Тест проверяет регистрацию магазина, подключенного к Доставке 3.0.
     */
    @Test
    @DbUnitDataSet(
            before = "DeliveryShopRegistration.before.csv",
            after = "DeliveryShopRegistration.after.csv"
    )
    void testDeliveryShopRegistration() {
        ResponseEntity<String> response = sendRequestFromFile("delivery_shop.json");
        assertThat(response, MoreMbiMatchers.responseBodyMatches(MbiMatchers.jsonPropertyEquals("result", "{" +
                "\"datasourceId\": 1," +
                "\"campaignId\": 1," +
                "\"agencyId\": 0," +
                "\"managerId\": -2," +
                "\"businessId\": 2," +
                "\"ownerId\": " + USER_ID +
                "}")));

        assertDeliveryNotifications();
    }

    private void assertDeliveryNotifications() {
        ContactWithEmail contact = new ContactWithEmail();
        contact.setFirstName("Vasily");
        contact.setLastName("Pupkin");
        contact.setPosition("менеджер");
        contact.setPhone("+71234567890");
        contact.setEmails(Set.of(new ContactEmail(0, "tester@yandex.ru", false, true)));

        CampaignInfo campaignInfo = new CampaignInfo();
        campaignInfo.setId(1);
        campaignInfo.setDatasourceId(1);
        campaignInfo.setClientId(CLIENT_ID);
        campaignInfo.setTariffId(1015);
        campaignInfo.setType(CampaignType.DELIVERY);

        DatasourceInfo datasourceInfo = new DatasourceInfo();
        datasourceInfo.setId(1);
        datasourceInfo.setInternalName("test");
        datasourceInfo.setManagerId(-2);
        datasourceInfo.setPlacementTypes(List.of(PartnerPlacementProgramType.CPC));

        NamedContainer businessContainer = new NamedContainer("business-id", 2);

        ArgumentCaptor<NotificationSendContext> notificationCaptor =
                ArgumentCaptor.forClass(NotificationSendContext.class);
        verify(notificationService, Mockito.times(2)).send(notificationCaptor.capture());
        NotificationSendContext welcomeNotification = notificationCaptor.getAllValues().get(0);
        NotificationSendContext welcomeSalesNotification = notificationCaptor.getAllValues().get(1);

        ReflectionAssert.assertReflectionEquals(
                welcomeNotification.getData(),
                List.of(
                        datasourceInfo,
                        campaignInfo,
                        businessContainer
                )
        );

        ReflectionAssert.assertReflectionEquals(
                welcomeSalesNotification.getData(),
                List.of(
                        contact,
                        datasourceInfo,
                        new NamedContainer("domain", "www.test.ru"),
                        new NamedContainer("local-delivery-region", "Москва"),
                        new NamedContainer("assessed-orders-count-per-day", "10-20")
                )
        );
    }

    /**
     * Тест проверяет регистрацию партнера 3PL
     */
    @Test
    @DbUnitDataSet(
            before = "TplShopRegistration.before.csv",
            after = "TplShopRegistration.after.csv")
    void testTplShopRegistration() {
        environmentService.setValues("shop.registration.valid-offer-percent", List.of("30"));

        ResponseEntity<String> response = sendRequestFromFile("tpl_shop.json");
        assertThat(response, MoreMbiMatchers.responseBodyMatches(MbiMatchers.jsonPropertyEquals("result", "{" +
                "\"datasourceId\": 1," +
                "\"campaignId\": 1," +
                "\"agencyId\": 0," +
                "\"managerId\": -2," +
                "\"ownerId\": " + USER_ID +
                "}")));
    }

    /**
     * Тест проверяет регистрацию сортировочного центра
     */
    @Test
    @DbUnitDataSet(
            before = "SortingCenterRegistration.before.csv",
            after = "SortingCenterRegistration.after.csv")
    void testSortingCenterRegistration() {
        environmentService.setValues("shop.registration.valid-offer-percent", List.of("30"));

        ResponseEntity<String> response = sendRequestFromFile("sorting_center.json");
        assertThat(response, MoreMbiMatchers.responseBodyMatches(MbiMatchers.jsonPropertyEquals("result", "{" +
                "\"datasourceId\": 1," +
                "\"campaignId\": 1," +
                "\"agencyId\": 0," +
                "\"managerId\": -2," +
                "\"ownerId\": " + USER_ID +
                "}")));
    }

    /**
     * Тест проверяет регистрацию пункта выдачи заказов
     */
    @Test
    @DbUnitDataSet(
            before = "TplOutletRegistration.before.csv",
            after = "TplOutletRegistration.after.csv")
    void testTplOutletRegistration() {
        environmentService.setValues("shop.registration.valid-offer-percent", List.of("30"));

        ResponseEntity<String> response = sendRequestFromFile("tpl_outlet.json");
        assertThat(response, MoreMbiMatchers.responseBodyMatches(MbiMatchers.jsonPropertyEquals("result", "{" +
                "\"datasourceId\": 1," +
                "\"campaignId\": 1," +
                "\"agencyId\": 0," +
                "\"managerId\": -2," +
                "\"ownerId\": " + USER_ID +
                "}")));
    }

    /**
     * Тест проверяет регистрацию партнера 3PL
     */
    @Test
    @DbUnitDataSet(
            before = "TplPartnerRegistration.before.csv",
            after = "TplPartnerRegistration.after.csv")
    void testTplPartnerRegistration() {
        environmentService.setValues("shop.registration.valid-offer-percent", List.of("30"));

        ResponseEntity<String> response = sendRequestFromFile("tpl_partner.json");
        assertThat(response, MoreMbiMatchers.responseBodyMatches(MbiMatchers.jsonPropertyEquals("result", "{" +
                "\"datasourceId\": 1," +
                "\"campaignId\": 1," +
                "\"agencyId\": 0," +
                "\"managerId\": -2," +
                "\"ownerId\": " + USER_ID +
                "}")));
    }

    /**
     * Тест проверяет регистрацию магистрального перевозчика 3PL.
     */
    @Test
    @DbUnitDataSet(
            before = "TplCarrierRegistration.before.csv",
            after = "TplCarrierRegistration.after.csv")
    void testTplCarrierRegistration() {
        environmentService.setValues("shop.registration.valid-offer-percent", List.of("30"));

        ResponseEntity<String> response = sendRequestFromFile("tpl_carrier.json");
        assertThat(response, MoreMbiMatchers.responseBodyMatches(MbiMatchers.jsonPropertyEquals("result", "{" +
                "\"datasourceId\": 1," +
                "\"campaignId\": 1," +
                "\"agencyId\": 0," +
                "\"managerId\": -2," +
                "\"ownerId\": " + USER_ID +
                "}")));
    }

    /**
     * Тест проверяет регистрацию кабинета ЦУП.
     */
    @Test
    @DbUnitDataSet(before = "TsupCabinetRegistration.before.csv", after = "TsupCabinetRegistration.after.csv")
    void testTsupCabinetRegistration() {
        environmentService.setValues("shop.registration.valid-offer-percent", List.of("30"));

        ResponseEntity<String> response = sendRequestFromFile("tsup.json");
        assertThat(response, MoreMbiMatchers.responseBodyMatches(MbiMatchers.jsonPropertyEquals("result", "{" +
                "\"datasourceId\": 1," +
                "\"campaignId\": 1," +
                "\"agencyId\": 0," +
                "\"managerId\": -2," +
                "\"ownerId\": " + USER_ID +
                "}")));
    }

    /**
     * Тест проверяет регистрацию кабинета Логистратор.
     */
    @Test
    @DbUnitDataSet(
            before = "LogistratorCabinetRegistration.before.csv",
            after = "LogistratorCabinetRegistration.after.csv"
    )
    void testLogistratorCabinetRegistration() {
        environmentService.setValues("shop.registration.valid-offer-percent", List.of("30"));

        ResponseEntity<String> response = sendRequestFromFile("logistrator.json");
        assertThat(response, MoreMbiMatchers.responseBodyMatches(MbiMatchers.jsonPropertyEquals("result", "{" +
                "\"datasourceId\": 1," +
                "\"campaignId\": 1," +
                "\"agencyId\": 0," +
                "\"managerId\": -2," +
                "\"ownerId\": " + USER_ID +
                "}")));
    }

    /**
     * Тест, который проверяет, что одному юзеру можно зарегать более 1 магазина.
     * По сути, портирован из mbi-at.
     */
    @Test
    @DbUnitDataSet(before = "LocalShopRegistration.before.csv", after = "TwoShopsRegistration.after.csv")
    void testTwoShopsRegistration() {
        ResponseEntity<String> firstResponse = sendRequestFromFile("local_shop.json");
        ResponseEntity<String> secondResponse = sendRequestFromFile("second_shop.json");

        //проверяем хттп код, саму успешность создания проверяем db-unit-ом.
        assertEquals(HttpStatus.OK, firstResponse.getStatusCode());
        assertEquals(HttpStatus.OK, secondResponse.getStatusCode());

        verify(asyncTarifficatorService).syncShopMetaData(eq(1L), eq(ActionType.CAMPAIGN_CREATION));
    }

    /**
     * Тест проверяет, что ручка валидирует корректность входных данных и в случае отсутствия обязательного поля
     * генерирует ответ об ошибке в требуемом формате.
     */
    @Test
    void testInvalidInputData() {
        sendBadRequest("insufficient_data.json", e -> {
            assertThat(e, hasErrorCode(HttpStatus.BAD_REQUEST));
            assertThat(e, errorListMatchesInAnyOrder(errorMatches("BAD_PARAM", "regionId", "MISSING")));
        });
    }

    /**
     * Тест проверяет, что ручка валидирует ownerLogin и, если такого пользователя не существует, отвечает 400 с
     * кодом BAD_PARAM.
     */
    @Test
    void testUnknownOwner() {
        sendBadRequest("unknown_owner.json", e -> {
            assertThat(e, hasErrorCode(HttpStatus.BAD_REQUEST));
            assertThat(e, errorListMatchesInAnyOrder(errorMatches("BAD_PARAM", "ownerLogin", "INVALID")));
        });
    }

    /**
     * Тест проверяет, что ручка не даст создать магазин с невалидным внутренним именем
     * и ответит 400 c кодом BAD_PARAM исабкодом INVALID.
     */
    @Test
    @DbUnitDataSet(before = "LocalShopRegistration.before.csv")
    void testInvalidInternalShopName() {
        sendBadRequest("incorrect_shop_internal_name.json", e -> {
            assertThat(e, hasErrorCode(HttpStatus.BAD_REQUEST));
            assertThat(e, errorListMatchesInAnyOrder(errorMatches("BAD_PARAM", "internalShopName", "INVALID")));
        });
    }

    /**
     * Тест проверяет, что ручка не даст создать магазин с пустым внутренним именем и невалидным shopName
     * и ответит 400 c кодом BAD_PARAM исабкодом INVALID.
     */
    @Test
    @DbUnitDataSet(before = "LocalShopRegistration.before.csv")
    void testInvalidShopName() {
        sendBadRequest("incorrect_shop_name.json", e -> {
            assertThat(e, hasErrorCode(HttpStatus.BAD_REQUEST));
            assertThat(e, errorListMatchesInAnyOrder(errorMatches("BAD_PARAM", "shopName", "INVALID")));
        });
    }

    /**
     * Тест проверяет, что ручка не принимает невалидный регион и ответит 400 с кодом BAD_PARAM и сабкодом INVALID.
     * На это необходим дополнительный тест, т.к. регион валидируется отдельным сервисом, а не встроенным в ручку
     * валидатором.
     */
    @Test
    @DbUnitDataSet(before = "LocalShopRegistration.before.csv")
    void testInvalidRegionId() {
        sendBadRequest("incorrect_region_id.json", e -> {
            assertThat(e, hasErrorCode(HttpStatus.BAD_REQUEST));
            assertThat(e, errorListMatchesInAnyOrder(errorMatches("BAD_PARAM", "wrong-region", "INVALID")));
        });
    }

    /**
     * Тест проверяет, что в случае, если агентство, используемое в запросе, еще не зарегистрированно (это происходит
     * руками через саппорт), то ручка откинет 400 с кодом AGENCY_HAS_NOT_REGISTERED.
     */
    @Test
    @DbUnitDataSet(before = "LocalShopRegistration.before.csv")
    void testAgencyNotRegistered() {
        when(balanceContactService.getClientIdByUid(USER_ID)).thenReturn(1L);
        when(balanceService.getClientByUid(USER_ID)).thenReturn(new ClientInfo(1L, ClientType.OOO, true, 1));

        sendBadRequest("local_shop.json", e -> {
            assertThat(e, hasErrorCode(HttpStatus.BAD_REQUEST));
            assertThat(e, errorListMatchesInAnyOrder(errorMatches("AGENCY_HAS_NOT_REGISTERED", null, null)));
        });
    }

    /**
     * Тест проверяет, что ручка устанавливает лимит магазинов на одного юзера. При превышении лимита отдается
     * 400 с кодом TOO_MANY_CAMPAIGNS.
     */
    @Test
    @DbUnitDataSet(before = {
            "TooManyCampaigns.before.csv",
            "TooManyCampaigns.linksToContact.before.csv"
    })
    void testTooManyCampaigns() {
        environmentService.setValue("shop.registration.campaigns.limit", "2");

        sendBadRequest("local_shop.json", e -> {
            assertThat(e, hasErrorCode(HttpStatus.BAD_REQUEST));
            assertThat(e, errorListMatchesInAnyOrder(errorMatches("TOO_MANY_CAMPAIGNS", null, null)));
        });
    }

    /**
     * Проверяет 400 для неуникального имени бизнеса.
     */
    @Test
    @DbUnitDataSet(before = "LocalBusinessShopRegister.before.csv")
    void testNonUniqueBusinessName() {
        sendBadRequest("non_unique_business_name.json", e -> {
            assertThat(e, hasErrorCode(HttpStatus.BAD_REQUEST));
            assertThat(e, errorListMatchesInAnyOrder(errorMatches("BAD_PARAM", "shopName", "ALREADY_EXISTS")));
        });
    }

    /**
     * Тест проверяет, что лимит кампаний не действует на агентства.
     */
    @Test
    @DbUnitDataSet(before = {
            "TooManyCampaigns.before.csv",
            "TooManyCampaigns.linksToSubClient.before.csv"
    })
    void testTooManyCampaignsForAgencies() {
        when(balanceContactService.getClientIdByUid(EFFECTIVE_USER_ID)).thenReturn(1L);
        when(balanceService.getClientByUid(EFFECTIVE_USER_ID)).thenReturn(new ClientInfo(1L, ClientType.OOO, true, 1));
        when(balanceService.getClient(1L)).thenReturn(new ClientInfo(1L, ClientType.OOO, true, 1));
        when(passportService.findUid("spbtester")).thenReturn(USER_ID);
        environmentService.setValue("shop.registration.campaigns.limit", "2");

        ResponseEntity<String> response = sendRequestFromFile("local_shop_agency.json", USER_ID,
                EFFECTIVE_USER_ID);
        assertThat(HttpStatus.OK, equalTo(response.getStatusCode()));
        verify(asyncTarifficatorService).syncShopMetaData(eq(1L), eq(ActionType.CAMPAIGN_CREATION));
    }

    /**
     * Проверяет лимит на количество кампаний в бизнесе.
     */
    @Test
    @DbUnitDataSet(before = "TooManyCampaignsInBusiness.before.csv")
    void testTooManyCampaignsInBusiness() {
        when(balanceContactService.getClientIdByUid(USER_ID)).thenReturn(1L);
        when(balanceService.getClient(1L)).thenReturn(new ClientInfo(101L, ClientType.OOO));

        // Добавляем в бизнес 1000 партнеров, до лимита
        for (int i = 0; i < 1000; i++) {
            PartnerId partner = partnerService.createPartner(CampaignType.SHOP);
            businessDao.createBusinessServiceLink(3000, partner.toLong(), MarketServiceType.SHOP);
        }

        sendBadRequest("local_business_shop.json", e -> {
            assertThat(e, hasErrorCode(HttpStatus.BAD_REQUEST));
            assertThat(e, errorListMatchesInAnyOrder(errorMatches("TOO_MANY_CAMPAIGNS", null, null)));
        });
    }

    /**
     * Проверяет регистрацию с существующим бизнесом.
     */
    @Test
    @DbUnitDataSet(before = "LocalBusinessShopRegister.before.csv")
    void testLocalBusinessShopRegister() {
        when(balanceContactService.getClientIdByUid(USER_ID)).thenReturn(1L);
        when(balanceService.getClient(1L)).thenReturn(new ClientInfo(101L, ClientType.OOO));

        ResponseEntity<String> response = sendRequestFromFile("local_business_shop.json");
        assertThat(response, MoreMbiMatchers.responseBodyMatches(MbiMatchers.jsonPropertyEquals("result", "{" +
                "  \"datasourceId\": 1,\n" +
                "  \"campaignId\": 2,\n" +
                "  \"agencyId\": 0,\n" +
                "  \"managerId\": -2,\n" +
                "  \"ownerId\": 67282295,\n" +
                "  \"businessId\": 3000\n" +
                "}")));

        verify(asyncTarifficatorService).syncShopMetaData(eq(1L), eq(ActionType.CAMPAIGN_CREATION));
    }

    /**
     * Проверяет регистрацию с существующим бизнесом у которого уже есть сервис.
     */
    @Test
    @DbUnitDataSet(before = "testLocalBusinessShopRegisterWithService.before.csv")
    void testLocalBusinessShopRegisterWithService() {
        when(balanceContactService.getClientIdByUid(USER_ID)).thenReturn(1L);
        when(balanceService.getClient(1L)).thenReturn(new ClientInfo(101L, ClientType.OOO));

        ResponseEntity<String> response = sendRequestFromFile("local_business_shop.json");
        assertThat(response, MoreMbiMatchers.responseBodyMatches(MbiMatchers.jsonPropertyEquals("result", "{" +
                "  \"datasourceId\": 1,\n" +
                "  \"campaignId\": 2,\n" +
                "  \"agencyId\": 0,\n" +
                "  \"managerId\": -2,\n" +
                "  \"ownerId\": 67282295,\n" +
                "  \"businessId\": 3000\n" +
                "}")));
        verify(asyncTarifficatorService).syncShopMetaData(eq(1L), eq(ActionType.CAMPAIGN_CREATION));
    }

    /**
     * Проверяет регистрацию с существующим бизнесом у которого есть сервис с 0 clientId.
     */
    @Test
    @DbUnitDataSet(before = "testLocalBusinessShopRegisterZeroClient.before.csv")
    void testLocalBusinessShopRegisterZeroClient() {
        when(balanceContactService.getClientIdByUid(USER_ID)).thenReturn(1L);
        when(balanceService.getClient(1L)).thenReturn(new ClientInfo(101L, ClientType.OOO));

        ResponseEntity<String> response = sendRequestFromFile("local_business_shop.json");
        assertThat(response, MoreMbiMatchers.responseBodyMatches(MbiMatchers.jsonPropertyEquals("result", "{" +
                "  \"datasourceId\": 1,\n" +
                "  \"campaignId\": 2,\n" +
                "  \"agencyId\": 0,\n" +
                "  \"managerId\": -2,\n" +
                "  \"ownerId\": 67282295,\n" +
                "  \"businessId\": 3000\n" +
                "}")));

        verify(asyncTarifficatorService).syncShopMetaData(eq(1L), eq(ActionType.CAMPAIGN_CREATION));
    }

    /**
     * Проверяет регистрацию с существующим бизнесом у которого есть сервис с другим clientId.
     */
    @Test
    @DbUnitDataSet(before = "testLocalBusinessShopRegisterDifferentClient.before.csv")
    void testLocalBusinessShopRegisterDifferentClient() {
        ResponseEntity<String> response = sendRequestFromFile("local_business_shop.json");
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    /**
     * Проверяет регистрацию с существующим бизнесом и не уникальным внутренним названием.
     */
    @Test
    @DbUnitDataSet(before = {"LocalBusinessShopRegister.before.csv", "NotUniqueInternalName.before.csv"})
    void testLocalBusinessShopRegisterWithInternalName() {
        when(balanceContactService.getClientIdByUid(USER_ID)).thenReturn(1L);
        when(balanceService.getClient(1L)).thenReturn(new ClientInfo(101L, ClientType.OOO));


        sendBadRequest("local_business_shop.json", e -> {
            assertThat(e, hasErrorCode(HttpStatus.BAD_REQUEST));
            assertThat(e, errorListMatchesInAnyOrder(errorMatches("BAD_PARAM", "internalShopName",
                    "ALREADY_EXISTS")));
        });
    }

    @Test
    @DisplayName("Тест проверяет, что ручка принимает email с доменом shop.")
    @DbUnitDataSet(before = "ShopContactEmailShouldNotFail.before.csv", after = "ShopContactEmailShouldNotFail.after" +
            ".csv")
    void shopContactEmailShouldNotFail() {
        ResponseEntity<String> responseEntity = sendRequestFromFile("shop_domain_email.json");
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        verify(asyncTarifficatorService).syncShopMetaData(eq(1L), eq(ActionType.CAMPAIGN_CREATION));
    }

    @Test
    @DisplayName("Тест проверяет, что ручка принимает email тест@yandex.ru")
    @DbUnitDataSet(before = "ShopContactEmailShouldNotFail.before.csv",
            after = "ShopContactCyrillicEmailShouldNotFail.after.csv")
    void testCyrillicContactEmailShouldNotFail() {
        ResponseEntity<String> responseEntity = sendRequestFromFile("shop_cyrillic_email.json");
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());

        verify(asyncTarifficatorService).syncShopMetaData(eq(1L), eq(ActionType.CAMPAIGN_CREATION));
    }

    @Test
    @DisplayName("Тест проверяет, что ручка не принимает невалидный email.")
    void testInvalidEmail() {
        sendBadRequest("incorrect_email.json", e -> {
            assertThat(e, hasErrorCode(HttpStatus.BAD_REQUEST));
            assertThat(e, errorListMatchesInAnyOrder(errorMatches("BAD_PARAM", "notificationContact.email",
                    "INVALID")));
        });
    }

    @Test
    @DisplayName("Тест проверяет, что ручка не принимает пустой email.")
    void testMissingEmail() {
        sendBadRequest("missing_email.json", e -> {
            assertThat(e, hasErrorCode(HttpStatus.BAD_REQUEST));
            assertThat(e, errorListMatchesInAnyOrder(errorMatches("BAD_PARAM", "notificationContact.email",
                    "MISSING")));
        });
    }

    @DisplayName("Тест проверяет, что зелёный магазин успешно создаётся при существующем синем.")
    @Test
    @DbUnitDataSet(before = "testGreenWhenBlueExists.before.csv",
            after = "LinksWithRoles.after.csv")
    void testGreenWhenBlueExists() {
        when(balanceService.getClient(101L)).thenReturn(new ClientInfo(100L, ClientType.OOO));
        when(balanceContactService.getUidsByClient(101L)).thenReturn(list(67282295L));

        sendRequestFromFile("local_shop.json");
        verify(asyncTarifficatorService).syncShopMetaData(eq(1L), eq(ActionType.CAMPAIGN_CREATION));
    }

    @DisplayName("Тест проверяет, что пользователь-агентство не может быть использован " +
            "для регистрации магазина клиента")
    @Test
    @DbUnitDataSet(before = "GlobalShopRegistration.before.csv")
    void testAgencyShopCreationException() {
        when(balanceContactService.getClientIdByUid(USER_ID)).thenReturn(100L);
        when(balanceService.getClient(100L)).thenReturn(new ClientInfo(100L, ClientType.OOO, true, 100));
        when(balanceService.getClientByUid(USER_ID)).thenReturn(new ClientInfo(100L, ClientType.OOO, true, 100));

        sendBadRequest("local_shop.json", e -> {
            assertThat(e, hasErrorCode(HttpStatus.FORBIDDEN));
            assertThat(e, errorListMatchesInAnyOrder(errorMatches("AGENCY", null, null)));
        });
    }

    @DisplayName("Тест проверяет, что ручка отбросит ошибку, если передан ownerLogin, но сам пользователь не" +
            " является агентством")
    @Test
    @DbUnitDataSet(before = "GlobalShopRegistration.before.csv")
    void testNotAnAgency() {
        when(passportService.findUid("test-owner")).thenReturn(100500L);
        when(balanceContactService.getClientIdByUid(USER_ID)).thenReturn(100L);
        when(balanceService.getClientByUid(USER_ID)).thenReturn(new ClientInfo(100L, ClientType.OOO, false, 1));

        sendBadRequest("known_owner.json", e -> {
            assertThat(e, hasErrorCode(HttpStatus.FORBIDDEN));
            assertThat(e, errorListMatchesInAnyOrder(errorMatches("NOT_AN_AGENCY", null, null)));
        });
    }

    @DisplayName("Тест проверяет, что субклиент не может зарегистрировать adv магазин")
    @Test
    @DbUnitDataSet(before = "GlobalShopRegistration.before.csv")
    void testSubclientADV() {
        when(balanceContactService.getClientIdByUid(USER_ID)).thenReturn(100L);
        when(balanceService.getClientByUid(USER_ID)).thenReturn(new ClientInfo(100L, ClientType.OOO, false, 1));

        sendBadRequest("local_shop.json", e -> {
            assertThat(e, hasErrorCode(HttpStatus.FORBIDDEN));
            assertThat(e, errorListMatchesInAnyOrder(errorMatches("SUBCLIENT", null, null)));
        });
    }

    @DisplayName("Тест проверяет, что субклиент может зарегистрировать dbs магазин")
    @Test
    @DbUnitDataSet(before = {"dsbs.registration.before.csv", "dsbs.registration.agency.before.csv"},
            after = {"testDsbsShopRegistrationPushWithFeed.after.csv", "dsbs.registration.agency.after.csv"})
    void testSubclientDBS() {
        when(balanceContactService.getClientIdByUid(USER_ID)).thenReturn(100L);
        when(balanceService.getClient(100L)).thenReturn(new ClientInfo(100L, ClientType.OOO, false, 1));
        when(balanceService.getClientByUid(USER_ID)).thenReturn(new ClientInfo(100L, ClientType.OOO, false, 1));


        checkDsbs(true);

        verify(asyncTarifficatorService).syncShopMetaData(eq(1L), eq(ActionType.CAMPAIGN_CREATION));
    }

    @DisplayName("Тест проверяет, что субклиент может среплицировать dbs магазин")
    @Test
    @DbUnitDataSet(before = {"testShopToDbsReplication.before.csv", "dsbs.registration.agency.before.csv"},
            after = {"testShopToDbsReplication.after.csv", "dsbs.replication.agency.after.csv",
                    "testShopToDbsReplication.price.tariffs.after.csv"})
    void testSubclientDBSReplication() {
        when(balanceContactService.getClientIdByUid(USER_ID)).thenReturn(100L);
        when(balanceService.getClient(100L)).thenReturn(new ClientInfo(100L, ClientType.OOO, false, 1));
        when(balanceService.getClientByUid(USER_ID)).thenReturn(new ClientInfo(100L, ClientType.OOO, false, 1));

        ResponseEntity<String> response = FunctionalTestHelper.post(
                baseUrl + "/sputnik/{program}?_user_id={userId}&id=11", null, PartnerPlacementProgramType.DROPSHIP_BY_SELLER, USER_ID);

        assertThat(response, MoreMbiMatchers.responseBodyMatches(MbiMatchers.jsonPropertyEquals("result", "{" +
                "\"datasourceId\": 1," +
                "\"campaignId\": 1," +
                "\"agencyId\": 1," +
                "\"managerId\": -2," +
                "\"businessId\": 12," +
                "\"ownerId\": " + USER_ID +
                "}")));
        verify(balanceContactService, never()).linkUid(eq(USER_ID), anyLong(), anyLong(), anyLong());
    }

    @DisplayName("Тест проверяет, что передаваемый в register-shop параметр registrationSource успешно сохраняется в " +
            "базу")
    @Test
    @DbUnitDataSet(
            before = "LocalShopRegistration.before.csv",
            after = "testRegistrationSourcePersistence.after.csv"
    )
    void testRegistrationSourcePersistence() {
        ResponseEntity<String> response = sendRequestFromFile("from_promo_shop.json");

        assertThat(HttpStatus.OK, equalTo(response.getStatusCode()));

        verify(asyncTarifficatorService).syncShopMetaData(eq(1L), eq(ActionType.CAMPAIGN_CREATION));
    }

    @Test
    @DisplayName("Регистрация DSBS происходит в пуш-схеме и с созданием дефолтного фида")
    @DbUnitDataSet(before = "dsbs.registration.before.csv", after = "testDsbsShopRegistrationPushWithFeed.after.csv")
    void testDsbsShopRegistrationPushWithFeed() {
        checkDsbs(false);
    }

    /**
     * Проверяет создание DBS-партнера из CPC-магазина с ценовыми настройками СиС
     */
    @Test
    @DbUnitDataSet(before = "testShopToDbsReplication.before.csv",
            after = {"testShopToDbsReplication.after.csv", "testShopToDbsReplication.price.tariffs.after.csv"})
    void testShopToDbsWithPriceTariffsReplication() {
        ResponseEntity<String> response = FunctionalTestHelper.post(
                baseUrl + "/sputnik/{program}?_user_id={userId}&id=11", null, PartnerPlacementProgramType.DROPSHIP_BY_SELLER, USER_ID);

        assertThat(response, MoreMbiMatchers.responseBodyMatches(MbiMatchers.jsonPropertyEquals("result", "{" +
                "\"datasourceId\": 1," +
                "\"campaignId\": 1," +
                "\"agencyId\": 0," +
                "\"managerId\": -2," +
                "\"businessId\": 12," +
                "\"ownerId\": " + USER_ID +
                "}")));
    }

    /**
     * Проверяет создание DBS-партнера из CPC-магазина с весовыми настройками СиС
     */
    @Test
    @DbUnitDataSet(before = "testShopToDbsReplication.before.csv",
            after = {"testShopToDbsReplication.after.csv", "testShopToDbsReplication.weight.tariffs.after.csv"})
    void testShopToDbsWithWeightTariffsReplication() {
        ResponseEntity<String> response = FunctionalTestHelper.post(
                baseUrl + "/sputnik/{program}?_user_id={userId}&id=9", null, PartnerPlacementProgramType.DROPSHIP_BY_SELLER, USER_ID);

        assertThat(response, MoreMbiMatchers.responseBodyMatches(MbiMatchers.jsonPropertyEquals("result", "{" +
                "\"datasourceId\": 1," +
                "\"campaignId\": 1," +
                "\"agencyId\": 0," +
                "\"managerId\": -2," +
                "\"businessId\": 12," +
                "\"ownerId\": " + USER_ID +
                "}")));
    }

    /**
     * Проверяет создание DBS-партнера из CPC-магазина с весовыми и категорийными настройками СиС
     */
    @Test
    @DbUnitDataSet(before = "testShopToDbsReplication.before.csv",
            after = {"testShopToDbsReplication.after.csv", "testShopToDbsReplication.weightcategory.tariffs.after.csv"})
    void testShopToDbsWithWeightAndCategoryTariffsReplication() {
        ResponseEntity<String> response = FunctionalTestHelper.post(
                baseUrl + "/sputnik/{program}?_user_id={userId}&id=10", null, PartnerPlacementProgramType.DROPSHIP_BY_SELLER, USER_ID);

        assertThat(response, MoreMbiMatchers.responseBodyMatches(MbiMatchers.jsonPropertyEquals("result", "{" +
                "\"datasourceId\": 1," +
                "\"campaignId\": 1," +
                "\"agencyId\": 0," +
                "\"managerId\": -2," +
                "\"businessId\": 12," +
                "\"ownerId\": " + USER_ID +
                "}")));
    }

    @Test
    @DbUnitDataSet(before = "testRegistrationOnSameContact.failed.before.csv",
            after = "testRegistrationOnSameContact.failed.after.csv")
    void testRegisterShopOnSameContactAsTpl() {
        environmentService.setValues("shop.registration.valid-offer-percent", List.of("30"));
        assertThrowsDifferentBusinessTypePartnersOnContact("local_shop.json", 1);
    }

    @Test
    @DbUnitDataSet(before = "testRegistrationOnSameContact.success.before.csv",
            after = "testRegistrationOnSameContact.success.after.csv")
    void testRegisterTplOnSameContactAsAnotherTplAndShop() {
        environmentService.setValues("shop.registration.valid-offer-percent", List.of("30"));
        ResponseEntity<String> response = sendRequestFromFile("tpl_shop.json");
        assertThat(response, MoreMbiMatchers.responseBodyMatches(MbiMatchers.jsonPropertyEquals("result", "{" +
                "\"datasourceId\": 1," +
                "\"campaignId\": 1," +
                "\"agencyId\": 0," +
                "\"managerId\": -2," +
                "\"ownerId\": " + USER_ID +
                "}")));
    }

    @Test
    @DbUnitDataSet(before = "testRegistrationWithDirectOnSameContact.failed.before.csv")
    void testRegisterShopOnSameContactAsAnotherTplAndDirect() {
        //TODO fix
        assertThrowsDifferentBusinessTypePartnersOnContact("dsbs_shop.json", 1);
    }

    @Test
    @DbUnitDataSet(before = "testRegistrationWithSupplier1POnSameContact.failed.before.csv")
    void testRegisterTplOnSameContactAsSupplier1P() {
        environmentService.setValues("shop.registration.valid-offer-percent", List.of("30"));
        ResponseEntity<String> response = sendRequestFromFile("tpl_shop.json");
        assertThat(response, MoreMbiMatchers.responseBodyMatches(MbiMatchers.jsonPropertyEquals("result", "{" +
                "\"datasourceId\": 1," +
                "\"campaignId\": 1," +
                "\"agencyId\": 0," +
                "\"managerId\": -2," +
                "\"ownerId\": " + USER_ID +
                "}")));
    }


    @DisplayName("Не можем зарегистрировать 1P поставщика на контакта с TPL")
    @Test
    @DbUnitDataSet(
            before = {"Supplier1PRegistration.before.csv", "Supplier1PRegistration.contactWithTpl.before.csv"}
    )
    void testSupplier1PRegistrationOnContactWithTpl() {
        String ownerLogin = "test-owner";
        when(balanceService.getClient(10L)).thenReturn(new ClientInfo(10L, ClientType.OOO));
        when(passportService.findUid(ownerLogin)).thenReturn(USER_ID);
        when(passportService.getUserInfo(USER_ID))
                .thenReturn(new UserInfo(USER_ID, "Pupkin Vasily", null, ownerLogin));

        ResponseEntity<String> response = sendRequestFromFile("supplier_1p.json");

        assertThat(HttpStatus.OK, equalTo(response.getStatusCode()));
        assertThat(response, MoreMbiMatchers.responseBodyMatches(MbiMatchers.jsonPropertyEquals("result", "{" +
                "\"datasourceId\": 1," +
                "\"campaignId\": 1," +
                "\"agencyId\": 0," +
                "\"managerId\": -2," +
                "\"ownerId\": " + USER_ID +
                "}")));
    }

    @DisplayName("Успешно регистрируем 1P поставщика на контакта с бизнесом и магазином")
    @Test
    @DbUnitDataSet(
            before = {"Supplier1PRegistration.before.csv", "Supplier1PRegistration.contactWithShop.before.csv"}
    )
    void testSupplier1PRegistrationOnContactWithShop() {
        String ownerLogin = "test-owner";
        when(balanceService.getClient(10L)).thenReturn(new ClientInfo(10L, ClientType.OOO));
        when(passportService.findUid(ownerLogin)).thenReturn(USER_ID);
        when(passportService.getUserInfo(USER_ID))
                .thenReturn(new UserInfo(USER_ID, "Pupkin Vasily", null, ownerLogin));

        assertThrowsDifferentBusinessTypePartnersOnContact("supplier_1p.json", 13);
    }

    @DisplayName("Успешно регистрируем 1P поставщика на контакта с TPL, бизнесом и магазином")
    @Test
    @DbUnitDataSet(
            before = {"Supplier1PRegistration.before.csv", "Supplier1PRegistration.contactWithShopAndTpl.before.csv"}
    )
    void testSupplier1PRegistrationOnContactWithShopAndTpl() {
        String ownerLogin = "test-owner";
        when(balanceService.getClient(10L)).thenReturn(new ClientInfo(10L, ClientType.OOO));
        when(passportService.findUid(ownerLogin)).thenReturn(USER_ID);
        when(passportService.getUserInfo(USER_ID))
                .thenReturn(new UserInfo(USER_ID, "Pupkin Vasily", null, ownerLogin));

        ResponseEntity<String> response = sendRequestFromFile("supplier_1p.json");

        assertThat(HttpStatus.OK, equalTo(response.getStatusCode()));
        assertThat(response, MoreMbiMatchers.responseBodyMatches(MbiMatchers.jsonPropertyEquals("result", "{" +
                "\"datasourceId\": 1," +
                "\"campaignId\": 1," +
                "\"agencyId\": 0," +
                "\"managerId\": -2," +
                "\"ownerId\": " + USER_ID +
                "}")));
    }

    private void assertThrowsDifferentBusinessTypePartnersOnContact(String filename, long contactId) {
        HttpClientErrorException exception = assertThrows(
                HttpClientErrorException.class, () -> sendRequestFromFile(filename)
        );
        assertThat(
                exception,
                Matchers.allOf(
                        hasErrorCode(HttpStatus.BAD_REQUEST),
                        HttpClientErrorMatcher.bodyMatches(
                                MbiMatchers.jsonPropertyMatches(
                                        "errors",
                                        MbiMatchers.jsonArrayEquals(""
                                                + "{"
                                                + "\"code\":\"DIFFERENT_BUSINESS_TYPE_PARTNERS_ON_CONTACT\","
                                                + "\"details\":" +
                                                "{" +
                                                "\"contact_id\":" + contactId + "," +
                                                "}"
                                                + "}"
                                        )
                                )
                        )
                )
        );
    }

    @Test
    @DbUnitDataSet(before = "testGetSputnik.before.csv")
    public void testReplicateAlreadyCopiedShop() {
        HttpClientErrorException exception = assertThrows(
                HttpClientErrorException.class, () -> FunctionalTestHelper.post(baseUrl +
                        "/sputnik/{program}?_user_id={userId}&id={partner_id}", null, PartnerPlacementProgramType.DROPSHIP_BY_SELLER, USER_ID, 11)
        );
        assertThat(
                exception,
                Matchers.allOf(
                        hasErrorCode(HttpStatus.BAD_REQUEST),
                        HttpClientErrorMatcher.bodyMatches(
                                MbiMatchers.jsonPropertyMatches(
                                        "errors",
                                        MbiMatchers.jsonArrayEquals(""
                                                + "{"
                                                + "    \"code\":\"BAD_PARAM\","
                                                + "    \"message\":\"The given shop was already replicated with this " +
                                                "type\","
                                                + "\"details\":" +
                                                "{" +
                                                "\"field\":\"program\"," +
                                                "\"campaignId\":21," +
                                                "\"subcode\":\"ALREADY_EXISTS\"" +
                                                "}"
                                                + "}"
                                        )
                                )
                        )
                )
        );
    }

    @ParameterizedTest
    @MethodSource("provideGetSputnikData")
    @DbUnitDataSet(before = "testGetSputnik.before.csv")
    void testGetSputnik(long donorCampaignId, PartnerPlacementProgramType programType, String expected) {
        ResponseEntity<String> response = FunctionalTestHelper.get(
                baseUrl + "/sputnik/{program}?_user_id={userId}&id={partner_id}",
                programType, USER_ID, donorCampaignId);
        assertThat(response, MoreMbiMatchers.responseBodyMatches(MbiMatchers.jsonPropertyEquals("result", expected)));
    }

    @DisplayName("Успешно регистрируем 1P поставщика")
    @Test
    @DbUnitDataSet(
            before = "Supplier1PRegistration.before.csv",
            after = "Supplier1PRegistration.after.csv"
    )
    void testSupplier1PRegistration() {
        String ownerLogin = "test-owner";
        when(passportService.findUid(ownerLogin)).thenReturn(USER_ID);
        when(passportService.getUserInfo(USER_ID))
                .thenReturn(new UserInfo(USER_ID, "Pupkin Vasily", null, "test-owner"));

        ResponseEntity<String> response = sendRequestFromFile("supplier_1p.json");

        assertThat(HttpStatus.OK, equalTo(response.getStatusCode()));
        assertThat(response, MoreMbiMatchers.responseBodyMatches(MbiMatchers.jsonPropertyEquals("result", "{" +
                "\"datasourceId\": 1," +
                "\"campaignId\": 1," +
                "\"agencyId\": 0," +
                "\"managerId\": -2," +
                "\"ownerId\": " + USER_ID +
                "}")));
    }

    @Test
    @DisplayName("Регистрация DSBS происходит в пуш-схеме и с созданием дефолтного фида и работе через ПИ")
    @DbUnitDataSet(before = "dsbs.registration.before.csv",
            after = "testDsbsShopRegistrationPushWithFeedAndPI.after.csv")
    void testDsbsShopRegistrationPushWithFeedAndPI() {
        checkDsbs(false);
    }


    private void checkDsbs(boolean isAgency) {
        environmentService.setValues("shop.registration.valid-offer-percent", List.of("30"));

        ResponseEntity<String> response = sendRequestFromFile("dsbs_shop.json");
        JsonTestUtil.assertEquals(response, getClass(), FILE_PREFIX +
                (isAgency ? "dsbs_registration_agency.response.json" : "dsbs_registration.response.json"));

        verify(asyncTarifficatorService).syncShopMetaData(eq(1L), eq(ActionType.CAMPAIGN_CREATION));

        verifySentNotificationType(partnerNotificationClient, 1, 95L);
    }

    private void sendBadRequest(String fileName, Consumer<HttpClientErrorException> onError) {
        HttpClientErrorException error = assertThrows(
                HttpClientErrorException.class, () -> sendRequestFromFile(fileName)
        );
        onError.accept(error);
    }

    /**
     * Посылает POST-запрос на регистрацию магазина, в теле которого будут содержаться данные из указанного файла.
     */
    private ResponseEntity<String> sendRequestFromFile(String filename,
                                                       long uid,
                                                       long eUid) {
        StringBuilder urlBuilder = new StringBuilder(baseUrl)
                .append("/register-shop?_user_id=")
                .append(uid)
                .append("&euid=")
                .append(eUid);

        HttpEntity request = JsonTestUtil.getJsonHttpEntity(getClass(), FILE_PREFIX + filename);

        return FunctionalTestHelper.post(urlBuilder.toString(), request);
    }

    private ResponseEntity<String> sendRequestFromFile(String filename) {
        return sendRequestFromFile(filename, USER_ID, USER_ID);
    }
}
