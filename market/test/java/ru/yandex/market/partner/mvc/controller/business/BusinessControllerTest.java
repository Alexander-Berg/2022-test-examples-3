package ru.yandex.market.partner.mvc.controller.business;

import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.common.balance.model.ClientType;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.core.balance.BalanceContactService;
import ru.yandex.market.core.balance.BalanceService;
import ru.yandex.market.core.balance.model.ClientInfo;
import ru.yandex.market.core.balance.model.FullClientInfo;
import ru.yandex.market.core.banner.model.BannerDisplayType;
import ru.yandex.market.core.geobase.model.RegionConstants;
import ru.yandex.market.core.passport.PassportService;
import ru.yandex.market.core.passport.model.EmailInfo;
import ru.yandex.market.core.passport.model.UserInfo;
import ru.yandex.market.core.state.event.BusinessChangesProtoLBEvent;
import ru.yandex.market.core.state.event.PartnerChangesProtoLBEvent;
import ru.yandex.market.logbroker.LogbrokerEventPublisher;
import ru.yandex.market.mbi.data.GeneralData;
import ru.yandex.market.mbi.environment.EnvironmentService;
import ru.yandex.market.mbi.util.MbiAsserts;
import ru.yandex.market.mbi.web.paging.PageTokenHelper;
import ru.yandex.market.partner.mvc.controller.business.model.BusinessRegistrationDTO;
import ru.yandex.market.partner.mvc.controller.business.model.OwnerChangeRequest;
import ru.yandex.market.partner.mvc.controller.supplier.NotificationContactDTO;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.skyscreamer.jsonassert.JSONCompareMode.NON_EXTENSIBLE;

/**
 * Тесты для контроллера {@link BusinessController}.
 *
 * @author Vadim Lyalin
 */
@DbUnitDataSet(before = "BusinessControllerTest.before.csv")
class BusinessControllerTest extends FunctionalTest {
    private static final PageTokenHelper pageTokenHelper = new PageTokenHelper(OBJECT_MAPPER);

    @Autowired
    private EnvironmentService environmentService;
    @Autowired
    private LogbrokerEventPublisher<PartnerChangesProtoLBEvent> logbrokerPartnerChangesEventPublisher;
    @Autowired
    private LogbrokerEventPublisher<BusinessChangesProtoLBEvent> logbrokerBusinessChangesEventPublisher;
    @Autowired
    private PassportService passportService;
    @Autowired
    private BalanceService balanceService;
    @Autowired
    private BalanceContactService balanceContactService;

    private static Stream<Arguments> testBusinessesArgs() {
        return Stream.of(
                // нет такого контакта
                Arguments.of(999, 1, "", null, "businesses-empty.json", true),
                // контакт есть, нет привязанных бизнесов
                Arguments.of(11, 1, "", null, "businesses-empty.json", true),
                // есть бизнесы и другие представители
                // В ответе nextToken: > 100
                Arguments.of(12, 1, "", null, "businesses-12.json", true),
                // Задаем условие id > 100
                // В ответе nextToken: > 101, prevToken: < 101
                Arguments.of(12, 1, "", pageTokenHelper.createNextToken(100), "businesses-12-101.json", true),
                // Тестируем поиск предыдущей
                Arguments.of(12, 1, "", pageTokenHelper.createPrevToken(999), "businesses-12-101.json", true),
                // Задаем условие id > 101
                // Ответ пустой, нет бизнесов с ид > 101
                Arguments.of(12, 1, "", pageTokenHelper.createNextToken(101), "businesses-12-empty.json", true),
                // поиск по подстроке
                // В ответе nextToken: > 101
                Arguments.of(12, 1, "ромашк", null, "businesses-12-romashka.json", true),
                // поиск по идентификатору бизнеса
                // В ответе nextToken: > 101
                Arguments.of(12, 1, "101", null, "businesses-12-romashka.json", true),
                // нет связи с бизнесом, но есть роль в кампании, которая в бизнесе
                Arguments.of(12, 1, "", null, "businesses-12.json", true),
                // нет связи с бизнесом, но есть линк с кампанией, которая в бизнесе, и нет роли в кампании
                Arguments.of(14, 1, "", null, "businesses-empty.json", true),
                // нет связи с бизнесом, но админ кампании, которая в бизнесе
                Arguments.of(16, 1, "", null, "businesses-16.json", true),
                // агентство с доступом к кампании под бизнесом
                Arguments.of(101, 5, "", null, "businesses-101.json", true),
                // агентство без доступа к кампаниям под бизнесами
                Arguments.of(102, 5, "", null, "businesses-102.json", true),
                // агентство с доступом в несколько бизнесов, в бизнесе 104 у поставщика выключен доступ
                // от агентства @code ParamType#AGENCY_SUPPLIER_ACCESS(132)
                Arguments.of(103, 5, "", null, "businesses-103.json", false),
                // агентство с доступом в бизнес только к одной кампании из двух
                Arguments.of(200, 5, "", null, "businesses-200.json", true),
                // доступ к директу и к поставщику, директ скрываем
                Arguments.of(201, 5, "", null, "businesses-201.json", true),
                Arguments.of(202, 5, "", null, "businesses-202.json", true)
        );
    }

    private static Stream<Arguments> testBusinessesForManagerArgs() {
        return Stream.of(
                Arguments.of("", null, 12, "businesses-12.json"),
                Arguments.of("", pageTokenHelper.createNextToken(100), 12, "businesses-12-101.json"),
                Arguments.of("ромашк", null, 12, "businesses-12-romashka.json"),
                Arguments.of("101", null, 12, "businesses-12-romashka.json"),
                Arguments.of("", null, 1007, "businesses-empty.json")
        );
    }

    private static Stream<Arguments> testBusinessExistsArgs() {
        return Stream.of(
                Arguments.of(101, "999", false), // несуществующий огрн
                Arguments.of(101, "123", true),  // огрн магазина в чужом бизнесе
                Arguments.of(101, "222", true),  // true??? огрн магазина не прошедшего проверку в чужом бизнесе
                Arguments.of(101, "321", true),  // огрн поставщика в чужом бизнесе
                Arguments.of(500, "123", false), // огрн магазина в своем бизнесе и чужом бизнесе
                Arguments.of(500, "321", false),  // огрн поставщика в своем бизнесе
                Arguments.of(101, "333", false)  // огрн поставщика не прошедшего проверку в чужом бизнесе
        );
    }

    private static Stream<Arguments> testBusinessCrossaleArgs() {
        return Stream.of(
                Arguments.of(501L, "{\"availableType\":\"SUPPLIER\"}"), // у бизнеса есть привязанный магазин
                Arguments.of(101L, "{\"availableType\":\"SHOP\"}"),        // у бизнеса есть привязанный поставщик
                Arguments.of(103L, "{}"),        // несуществующий бизнес
                Arguments.of(500L, "{}")         // у бизнеса есть привязанные и магаазины и поставщики
        );
    }

    private static Stream<Arguments> testGetBannersArgs() {
        return Stream.of(
                Arguments.of(101L, EnumSet.of(BannerDisplayType.ALERT),
                        "[{\"id\":\"banner101_2\"},{\"id\":\"banner101_3\"}]"),
                Arguments.of(101L, List.of(BannerDisplayType.values()),
                        "[{\"id\":\"banner101_1\"},{\"id\":\"banner101_2\"},{\"id\":\"banner101_3\"}]"),
                Arguments.of(102L, EnumSet.of(BannerDisplayType.ADVERTISING),
                        "[{\"id\":\"banner102\"}]"),
                Arguments.of(102L, EnumSet.of(BannerDisplayType.ALERT),
                        "[{\"id\":\"banner102uber\"}]")
        );
    }

    @BeforeEach
    void setUp() {
        when(logbrokerBusinessChangesEventPublisher.publishEventAsync(any(BusinessChangesProtoLBEvent.class)))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(invocation.getArgument(0)));
    }

    /**
     * Тестирует ручку /businesses.
     */
    @ParameterizedTest
    @MethodSource("testBusinessesArgs")
    @DbUnitDataSet(before = "BusinessControllerTest.testBusinesses.before.csv")
    void testBusinesses(long uid, long limit, String searchString, String pageToken, String expectedPath,
                        boolean ignoreOrder) {
        ResponseEntity<String> response = FunctionalTestHelper.get(
                baseUrl + "businesses?_user_id=" + uid + "&limit=" + limit + "&search=" + searchString +
                        (pageToken == null ? "" : "&page_token=" + pageToken));

        if (ignoreOrder) {
            JsonTestUtil.assertEquals("expected/" + expectedPath, getClass(), response);
        } else {
            JsonTestUtil.assertEquals(response, getClass(), "expected/" + expectedPath);
        }
    }

    /**
     * Тестирует ручку /businesses-for-manager.
     */
    @ParameterizedTest
    @MethodSource("testBusinessesForManagerArgs")
    void testBusinessesForManager(String searchString, String pageToken, long userId, String expectedPath) {
        ResponseEntity<String> response = FunctionalTestHelper.get(
                baseUrl + "businesses-for-manager?_user_id=" + userId + "&limit=1&search=" + searchString +
                        (pageToken == null ? "" : "&page_token=" + pageToken));
        JsonTestUtil.assertEquals("expected/" + expectedPath, getClass(), response);
    }

    /**
     * Тестирует получение бизнеса по ид.
     */
    @Test
    void testGetBusiness() {
        ResponseEntity<String> response = FunctionalTestHelper.get(baseUrl + "businesses/100?euid=12");
        JsonTestUtil.assertEquals("expected/business-100.json", getClass(), response);
    }

    /**
     * Тестирует получение бизнеса по ид.
     */
    @Test
    @DbUnitDataSet(before = "BusinessControllerTest.testGetBusinessWithDirect.before.csv")
    void testGetBusinessWithDirect() {
        ResponseEntity<String> response = FunctionalTestHelper.get(baseUrl + "businesses/1566?euid=1217");
        JsonTestUtil.assertEquals("expected/business-1566.json", getClass(), response);
    }

    /**
     * Тестирует получение бизнеса по ид для агентств. Возвращает только магазин, тк у поставщика не выставлен
     * {@link ru.yandex.market.core.param.model.ParamType#AGENCY_SUPPLIER_ACCESS}
     */
    @Test
    void testGetBusinessAgencyWithoutSupplierAccess() {
        ResponseEntity<String> response = FunctionalTestHelper.get(baseUrl + "businesses/100?euid=12132");
        JsonTestUtil.assertEquals("expected/business-100-agency-without-supplier.json", getClass(), response);
    }

    /**
     * Тестирует получение бизнеса по ид для агентств. Возвращает магазин и поставщика, тк у поставщика выставлен
     * {@link ru.yandex.market.core.param.model.ParamType#AGENCY_SUPPLIER_ACCESS}
     */
    @Test
    @DbUnitDataSet(before = "BusinessControllerTest.agency.with_supplier_access.before.csv")
    void testGetBusinessAgencyWithSupplierAccess() {
        ResponseEntity<String> response = FunctionalTestHelper.get(baseUrl + "businesses/100?euid=12132");
        JsonTestUtil.assertEquals("expected/business-100-agency-with-supplier.json", getClass(), response);
    }

    /**
     * Проверяет 404 при попытке получить бизнес несуществующим id.
     */
    @Test
    void testGetNonExistedBusiness() {
        HttpClientErrorException.BadRequest exception = assertThrows(HttpClientErrorException.BadRequest.class,
                () -> FunctionalTestHelper.get(baseUrl + "businesses/999"));

        MbiAsserts.assertJsonEquals("" +
                        "[{" +
                        "    \"code\":\"BAD_PARAM\"," +
                        "    \"message\":\"Business with id 999 was not found\"," +
                        "    \"details\":{}" +
                        "}]",
                JsonTestUtil.parseJson(exception.getResponseBodyAsString())
                        .getAsJsonObject()
                        .get("errors")
                        .toString()
        );
    }

    /**
     * Проверяет, что попытка обновить владельца бизнеса на контакт, не принадлежащий бизнесу, падает
     */
    @Test
    void trySetBusinessOwnerUidInAnotherBusiness() {
        var body = new OwnerChangeRequest();
        body.setUid(16L);

        HttpClientErrorException.NotFound exception = assertThrows(HttpClientErrorException.NotFound.class,
                () -> FunctionalTestHelper.put(baseUrl + "businesses/100/change-owner?_user_id=12", body));

        MbiAsserts.assertJsonEquals("" +
                        "[{" +
                        "    \"code\":\"BAD_PARAM\"," +
                        "    \"details\":{" +
                        "       \"entity_name\":\"contact\"," +
                        "       \"subcode\":\"ENTITY_NOT_FOUND\"," +
                        "       \"entity_id\":\"16\"" +
                        "    }" +
                        "}]",
                JsonTestUtil.parseJson(exception.getResponseBodyAsString())
                        .getAsJsonObject()
                        .get("errors")
                        .toString()
        );
    }

    /**
     * Проверяет, что попытка обновить владельца бизнеса на контакт, uid'а которого у нас нет в базе падает
     */
    @Test
    void trySetBusinessOwnerUidNotFound() {
        var body = new OwnerChangeRequest();
        body.setUid(99999L);

        HttpClientErrorException.NotFound exception = assertThrows(HttpClientErrorException.NotFound.class,
                () -> FunctionalTestHelper.put(baseUrl + "businesses/100/change-owner?_user_id=12", body));

        MbiAsserts.assertJsonEquals("" +
                        "[{" +
                        "    \"code\":\"BAD_PARAM\"," +
                        "    \"details\":{" +
                        "       \"entity_name\":\"contact\"," +
                        "       \"subcode\":\"ENTITY_NOT_FOUND\"," +
                        "       \"entity_id\":\"99999\"" +
                        "    }" +
                        "}]",
                JsonTestUtil.parseJson(exception.getResponseBodyAsString())
                        .getAsJsonObject()
                        .get("errors")
                        .toString()
        );
    }

    /**
     * Проверяет что можно сменить владельца бизнеса с контактом с бизнес ролью.
     */
    @Test
    @DbUnitDataSet(after = "BusinessControllerTest.changeOwnerBus.after.csv")
    void changeBusinessOwnerWithBusinessRole() {
        var body = new OwnerChangeRequest();
        body.setUid(13L);
        FunctionalTestHelper.put(baseUrl + "businesses/100/change-owner?_user_id=12", body);
    }

    /**
     * Проверяет что можно сменить владельца бизнеса с контактом без бизнес роли.
     */
    @Test
    @DbUnitDataSet(after = "BusinessControllerTest.changeOwnerNoBus.after.csv")
    void changeBusinessOwnerWithoutBusinessRole() {
        var body = new OwnerChangeRequest();
        body.setUid(17L);
        FunctionalTestHelper.put(baseUrl + "businesses/100/change-owner?_user_id=12", body);
    }

    /**
     * Тестирует ручку создания бизнеса.
     */
    @Test
    @DbUnitDataSet(after = "BusinessControllerTest.create.after.csv")
    void testCreateBusiness() {
        var businessEventsCaptor = ArgumentCaptor.forClass(BusinessChangesProtoLBEvent.class);
        var body = new BusinessRegistrationDTO();
        body.setBusinessName("новыйБизнес");
        body.setLocalRegionId(RegionConstants.SAINT_PETERSBURG);
        body.setDomain("pup.ru");

        var notificationContact = new NotificationContactDTO();
        notificationContact.setFirstName("Vas");
        notificationContact.setLastName("Pup");
        notificationContact.setEmail("pup123@yandex.ru");
        notificationContact.setPhone("+79161111111");
        body.setNotificationContact(notificationContact);

        environmentService.setValue("send.business.changes.events", "true");
        environmentService.setValue("register.business.client", "true");
        when(passportService.getUserInfo(eq(18L)))
                .thenReturn(new UserInfo(18, "Vas Pup", "pup@yandex.ru", "pup"));
        when(passportService.getEmails(eq(18L)))
                .thenReturn(List.of(new EmailInfo("pup@yandex.ru", true)));
        when(balanceService.createClient(any(FullClientInfo.class), eq(18L), anyLong())).thenReturn(180L);
        when(balanceContactService.getClientIdByUid(eq(18L))).thenReturn(180L);

        ResponseEntity<String> response = FunctionalTestHelper.post(
                baseUrl + "businesses?_user_id=18&euid=18", body
        );

        //language=json
        String expected = "" +
                "{\n" +
                "  \"businessId\": 1,\n" +
                "  \"name\": \"новыйБизнес\",\n" +
                "  \"slug\": \"novyibiznes\",\n" +
                "  \"generalPlacementTypes\": []\n" +
                "}";
        JsonTestUtil.assertEquals(response, expected);

        verify(logbrokerBusinessChangesEventPublisher, times(1)).publishEventAsync(businessEventsCaptor.capture());
        verify(balanceService).createClient(any(), eq(18L), anyLong());
        verify(balanceContactService).linkUid(eq(18L), eq(180L), eq(18L), anyLong());
        assertThat(businessEventsCaptor.getValue().getPayload().getBusinessId()).isEqualTo(1L);
        assertThat(businessEventsCaptor.getValue().getPayload().getGeneralInfo().getActionType())
                .isEqualTo(GeneralData.ActionType.CREATE);
        verifyNoInteractions(logbrokerPartnerChangesEventPublisher);
    }

    /**
     * Проверка ошибки при регистрации с лайт-паспорта.
     */
    @Test
    void testLitePassportBusinessRegistration() {
        var body = new BusinessRegistrationDTO();
        body.setBusinessName("новыйБизнес");
        body.setLocalRegionId(RegionConstants.SAINT_PETERSBURG);
        body.setDomain("pup.ru");

        var notificationContact = new NotificationContactDTO();
        notificationContact.setFirstName("Vas");
        notificationContact.setLastName("Pup");
        notificationContact.setEmail("pup@yandex.ru");
        notificationContact.setPhone("+79161111111");
        body.setNotificationContact(notificationContact);

        when(balanceService.createClient(any(FullClientInfo.class), eq(18L), anyLong())).thenReturn(180L);
        when(balanceContactService.getClientIdByUid(eq(18L))).thenReturn(180L);

        //language=json
        String expected = "" +
                "[{" +
                "    \"code\":\"LITE_PASSPORT_REGISTRATION\"," +
                "    \"details\":{}" +
                "}]";

        var exception = assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.post(baseUrl + "businesses?_user_id=18&euid=18", body)
        );
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        MbiAsserts.assertJsonEquals(
                expected,
                JsonTestUtil.parseJson(exception.getResponseBodyAsString())
                        .getAsJsonObject()
                        .get("errors")
                        .toString()
        );
    }

    /**
     * Проверка ошибки при регистрации с лайт-паспорта.
     */
    @Test
    void testIncorrectNameRegistration() {
        var body = new BusinessRegistrationDTO();
        body.setBusinessName("Аякс•Спорт");
        body.setLocalRegionId(RegionConstants.SAINT_PETERSBURG);
        body.setDomain("pup.ru");

        var notificationContact = new NotificationContactDTO();
        notificationContact.setFirstName("Vas");
        notificationContact.setLastName("Pup");
        notificationContact.setEmail("pup@yandex.ru");
        notificationContact.setPhone("+79161111111");
        body.setNotificationContact(notificationContact);

        when(passportService.getUserInfo(eq(9L)))
                .thenReturn(new UserInfo(9, "Vas Pup", "pup@yandex.ru", "pup"));
        when(passportService.getEmails(eq(9L)))
                .thenReturn(List.of(new EmailInfo("pup@yandex.ru", true)));
        when(balanceService.createClient(any(FullClientInfo.class), eq(9L), anyLong())).thenReturn(90L);
        when(balanceContactService.getClientIdByUid(eq(9L))).thenReturn(90L);

        //language=json
        String expected = "" +
                "[\n" +
                "  {\n" +
                "    \"code\": \"BAD_PARAM\",\n" +
                "    \"details\": {\n" +
                "      \"field\": \"businessName\",\n" +
                "      \"subcode\": \"INVALID\"\n" +
                "    }\n" +
                "  }\n" +
                "]";

        var exception = assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.post(baseUrl + "businesses?_user_id=9&euid=9", body)
        );
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        MbiAsserts.assertJsonEquals(
                expected,
                JsonTestUtil.parseJson(exception.getResponseBodyAsString())
                        .getAsJsonObject()
                        .get("errors")
                        .toString()
        );
    }

    /**
     * Тестирует ручку создания бизнеса с уже существующим контактом.
     */
    @Test
    @DbUnitDataSet(after = "BusinessControllerTest.create.existedContact.after.csv")
    void testCreateBusinessExistedContact() {
        var body = new BusinessRegistrationDTO();
        body.setBusinessName("новыйБизнес");
        body.setLocalRegionId(RegionConstants.SAINT_PETERSBURG);
        body.setDomain("pup.ru");

        var notificationContact = new NotificationContactDTO();
        notificationContact.setFirstName("Vas");
        notificationContact.setLastName("Pup");
        notificationContact.setEmail("yndx-11@yandex.ru");
        notificationContact.setPhone("+79161111111");
        body.setNotificationContact(notificationContact);

        when(passportService.getUserInfo(eq(11L)))
                .thenReturn(new UserInfo(11, "Vas Pup", "yndx-11@yandex.ru", "yndx-11"));
        when(passportService.getEmails(eq(11L)))
                .thenReturn(List.of(new EmailInfo("yndx-11@yandex.ru", true)));
        environmentService.setValue("register.business.client", "true");

        ResponseEntity<String> response = FunctionalTestHelper.post(
                baseUrl + "businesses?_user_id=11&euid=11", body
        );

        //language=json
        String expected = "" +
                "{\n" +
                "  \"businessId\": 1,\n" +
                "  \"name\": \"новыйБизнес\",\n" +
                "  \"slug\": \"novyibiznes\",\n" +
                "  \"generalPlacementTypes\": []\n" +
                "}";
        JsonTestUtil.assertEquals(response, expected);
    }

    @Test
    void testRegisterBusinessOnMarketOnly() {
        environmentService.setValue("register.business.client", "true");
        var body = new BusinessRegistrationDTO();
        body.setBusinessName("новыйБизнес");
        body.setLocalRegionId(RegionConstants.SAINT_PETERSBURG);
        body.setDomain("pup.ru");

        var notificationContact = new NotificationContactDTO();
        notificationContact.setFirstName("Vas");
        notificationContact.setLastName("Pup");
        notificationContact.setEmail("pup@yandex.ru");
        notificationContact.setPhone("+79161111111");
        body.setNotificationContact(notificationContact);
        var exception = assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.post(
                        baseUrl + "businesses?_user_id=17&euid=17", body
                )
        );
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    void testBusinessUniqueNameException() {
        var body = new BusinessRegistrationDTO();
        body.setBusinessName("бизнес");
        body.setLocalRegionId(RegionConstants.SAINT_PETERSBURG);
        body.setDomain("pup.ru");

        var notificationContact = new NotificationContactDTO();
        notificationContact.setFirstName("Vas");
        notificationContact.setLastName("Pup");
        notificationContact.setEmail("pup@yandex.ru");
        notificationContact.setPhone("+79161111111");
        body.setNotificationContact(notificationContact);

        var exception = assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.post(baseUrl + "businesses?_user_id=10&euid=10", body)
        );
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    @DbUnitDataSet(after = "BusinessControllerTest.testUpToCity.after.csv")
    void testUpToCity() {
        var body = new BusinessRegistrationDTO();
        body.setBusinessName("новыйБизнес");
        body.setLocalRegionId(216L);
        body.setDomain("pup.ru");

        var notificationContact = new NotificationContactDTO();
        notificationContact.setFirstName("Vas");
        notificationContact.setLastName("Pup");
        notificationContact.setEmail("yndx-11@yandex.ru");
        notificationContact.setPhone("+79161111111");
        body.setNotificationContact(notificationContact);

        when(passportService.getUserInfo(eq(11L)))
                .thenReturn(new UserInfo(11, "Vas Pup", "yndx-11@yandex.ru", "yndx-11"));
        when(passportService.getEmails(eq(11L)))
                .thenReturn(List.of(new EmailInfo("yndx-11@yandex.ru", true)));
        environmentService.setValue("register.business.client", "true");

        var response = FunctionalTestHelper.post(
                baseUrl + "businesses?_user_id=11&euid=11", body
        );
    }

    @Test
    void testExistedClient() {
        var body = new BusinessRegistrationDTO();
        body.setBusinessName("новыйБизнес");
        body.setLocalRegionId(RegionConstants.SAINT_PETERSBURG);
        body.setDomain("pup.ru");

        var notificationContact = new NotificationContactDTO();
        notificationContact.setFirstName("Vas");
        notificationContact.setLastName("Pup");
        notificationContact.setEmail("yndx-11@yandex.ru");
        notificationContact.setPhone("+79161111111");
        body.setNotificationContact(notificationContact);

        when(passportService.getUserInfo(eq(10L)))
                .thenReturn(new UserInfo(10, "Vas Pup", "yndx-11@yandex.ru", "yndx-10"));
        when(passportService.getEmails(eq(10L)))
                .thenReturn(List.of(new EmailInfo("yndx-11@yandex.ru", true)));
        environmentService.setValue("register.business.client", "true");
        when(balanceService.getClientByUid(eq(10L))).thenReturn(new ClientInfo(100L, ClientType.OOO));
        when(balanceContactService.getClientIdByUid(eq(10L))).thenReturn(100L);

        ResponseEntity<String> response = FunctionalTestHelper.post(
                baseUrl + "businesses?_user_id=10&euid=10", body
        );

        //language=json
        String expected = "" +
                "{\n" +
                "  \"businessId\": 1,\n" +
                "  \"name\": \"новыйБизнес\",\n" +
                "  \"slug\": \"novyibiznes\",\n" +
                "  \"generalPlacementTypes\": []\n" +
                "}";
        JsonTestUtil.assertEquals(response, expected);
        verify(balanceContactService, never()).linkUid(anyLong(), anyLong(), anyLong(), anyLong());
        verify(balanceService, never()).createClient(any(FullClientInfo.class), anyLong(), anyLong());
    }

    @Test
    void testNotValidDomain() {
        var body = new BusinessRegistrationDTO();
        body.setBusinessName("новыйБизнес");
        body.setLocalRegionId(RegionConstants.SAINT_PETERSBURG);
        body.setDomain("tralivali");

        var notificationContact = new NotificationContactDTO();
        notificationContact.setFirstName("Vas");
        notificationContact.setLastName("Pup");
        notificationContact.setEmail("yndx-11@yandex.ru");
        notificationContact.setPhone("+79161111111");
        body.setNotificationContact(notificationContact);

        when(passportService.getUserInfo(eq(11L)))
                .thenReturn(new UserInfo(11, "Vas Pup", "yndx-11@yandex.ru", "yndx-11"));
        when(passportService.getEmails(eq(11L)))
                .thenReturn(List.of(new EmailInfo("yndx-11@yandex.ru", true)));
        environmentService.setValue("register.business.client", "true");

        var httpClientException = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.post(baseUrl + "businesses?_user_id=11&euid=11", body)
        );

        Assertions.assertEquals(HttpStatus.BAD_REQUEST, httpClientException.getStatusCode());
    }

    @Test
    @DbUnitDataSet(before = "BusinessControllerTest.testDifferentBusinessTypes.before.csv")
    void testDifferentBusinessTypePartnersException() {
        var body = new BusinessRegistrationDTO();
        body.setBusinessName("новыйБизнес");
        body.setLocalRegionId(216L);
        body.setDomain("pup.ru");

        var notificationContact = new NotificationContactDTO();
        notificationContact.setFirstName("Vas");
        notificationContact.setLastName("Pup");
        notificationContact.setEmail("yndx-11@yandex.ru");
        notificationContact.setPhone("+79161111111");
        body.setNotificationContact(notificationContact);

        when(passportService.getUserInfo(eq(18L)))
                .thenReturn(new UserInfo(18, "Vas Pup", "yndx-11@yandex.ru", "yndx-18"));
        when(passportService.getEmails(eq(18L)))
                .thenReturn(List.of(new EmailInfo("yndx-18@yandex.ru", true)));
        environmentService.setValue("register.business.client", "true");

        //language=json
        String expected = "" +
                "[\n" +
                "  {\n" +
                "    \"code\": \"DIFFERENT_BUSINESS_TYPE_PARTNERS_ON_CONTACT\",\n" +
                "    \"details\": {\n" +
                "      \"contact_id\": 80\n" +
                "    }\n" +
                "  }\n" +
                "]";

        var exception = assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.post(baseUrl + "businesses?_user_id=18&euid=18", body)
        );
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        MbiAsserts.assertJsonEquals(
                expected,
                JsonTestUtil.parseJson(exception.getResponseBodyAsString())
                        .getAsJsonObject()
                        .get("errors")
                        .toString()
        );
    }

    @Test
    void testNotRegisteredAgencySubclientException() {
        var body = new BusinessRegistrationDTO();
        body.setBusinessName("новыйБизнес");
        body.setLocalRegionId(RegionConstants.SAINT_PETERSBURG);
        body.setDomain("pup.ru");

        var notificationContact = new NotificationContactDTO();
        notificationContact.setFirstName("Vas");
        notificationContact.setLastName("Pup");
        notificationContact.setEmail("pup@yandex.ru");
        notificationContact.setPhone("+79161111111");
        body.setNotificationContact(notificationContact);

        environmentService.setValue("send.business.changes.events", "true");
        environmentService.setValue("register.business.client", "true");
        when(passportService.getUserInfo(eq(18L)))
                .thenReturn(new UserInfo(18, "Vas Pup", "pup@yandex.ru", "pup"));
        when(passportService.getEmails(eq(18L)))
                .thenReturn(List.of(new EmailInfo("pup@yandex.ru", true)));
        when(balanceService.getClientByUid(eq(18L)))
                .thenReturn(new ClientInfo(180L, ClientType.OOO, false, 100500L));

        //language=json
        String expected = "" +
                "[\n" +
                "  {\n" +
                "    \"code\": \"AGENCY_HAS_NOT_REGISTERED\",\n" +
                "    \"details\": {\n" +
                "      \"id\": 100500\n" +
                "    }\n" +
                "  }\n" +
                "]";
        var exception = assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.post(baseUrl + "businesses?_user_id=18&euid=18", body)
        );
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        MbiAsserts.assertJsonEquals(
                expected,
                JsonTestUtil.parseJson(exception.getResponseBodyAsString())
                        .getAsJsonObject()
                        .get("errors")
                        .toString()
        );

    }

    @ParameterizedTest
    @MethodSource("testBusinessExistsArgs")
    @DbUnitDataSet(before = "testBusinessExists.csv")
    void testBusinessExists(long businessId, String ogrn, boolean expectedResult) {
        ResponseEntity<String> response = FunctionalTestHelper.get(
                baseUrl + "/businesses/" + businessId + "/show-ogrn-message?ogrn=" + ogrn);
        JsonTestUtil.assertEquals(response, Boolean.toString(expectedResult));
    }

    @ParameterizedTest
    @MethodSource("testBusinessCrossaleArgs")
    @DbUnitDataSet(before = "testBusinessExists.csv")
    void testBusinessCrossaleService(Long businessId, String expectedResponse) {
        ResponseEntity<String> response = FunctionalTestHelper.get(
                baseUrl + "/businesses/" + businessId + "/service-type/omitted");
        JsonTestUtil.assertEquals(response, expectedResponse);
    }

    @ParameterizedTest
    @CsvSource(value = {"100; [\"CPC\",\"DROPSHIP_BY_SELLER\"]",
            "101; [\"FULFILLMENT\",\"DROPSHIP\"]"}, delimiter = ';')
    void testGetPlacementTypes(long businessId, String expectedResponse) {
        ResponseEntity<String> response = FunctionalTestHelper.get(
                baseUrl + "/businesses/" + businessId + "/placement-types");
        JSONArray actualJson = new JSONObject(response.getBody()).getJSONArray("result");
        JSONAssert.assertEquals(expectedResponse, actualJson, NON_EXTENSIBLE);
    }

    @ParameterizedTest
    @MethodSource("testGetBannersArgs")
    void testGetBanners(long businessId, Collection<BannerDisplayType> displayTypes, String expectedResponse) {
        ResponseEntity<String> response = FunctionalTestHelper.get(baseUrl + "/businesses/" + businessId +
                "/banners?display_types=" + displayTypes.stream().map(Enum::name).collect(Collectors.joining(",")));
        JSONArray actualJson = new JSONObject(response.getBody()).getJSONObject("result").getJSONArray("banners");
        JSONAssert.assertEquals(expectedResponse, actualJson, NON_EXTENSIBLE);
    }

    @Test
    void testNoBanners() {
        ResponseEntity<String> response = FunctionalTestHelper.get(baseUrl + "/businesses/" + 100L +
                "/banners?display_types=" + BannerDisplayType.ALERT.name());
        JsonTestUtil.assertEquals(response, "{\"banners\":[]}");
    }
}
