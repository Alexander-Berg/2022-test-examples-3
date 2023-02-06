package ru.yandex.market.partner.mvc.controller.business;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.json.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.common.balance.model.ClientType;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.common.test.util.StringTestUtil;
import ru.yandex.market.core.balance.BalanceContactService;
import ru.yandex.market.core.balance.BalanceService;
import ru.yandex.market.core.balance.model.BalancePassportInfo;
import ru.yandex.market.core.balance.model.ClientInfo;
import ru.yandex.market.core.contact.InnerRole;
import ru.yandex.market.core.matchers.HttpClientErrorMatcher;
import ru.yandex.market.core.passport.PassportService;
import ru.yandex.market.core.passport.model.UserInfo;
import ru.yandex.market.core.state.event.ContactChangesProtoLBEvent;
import ru.yandex.market.logbroker.LogbrokerEventPublisher;
import ru.yandex.market.mbi.data.ContactDataOuterClass;
import ru.yandex.market.mbi.data.GeneralData;
import ru.yandex.market.mbi.util.MbiAsserts;
import ru.yandex.market.mbi.util.MbiMatchers;
import ru.yandex.market.partner.mvc.controller.business.model.BusinessContactRequest;
import ru.yandex.market.partner.mvc.controller.business.model.ContactInfo;
import ru.yandex.market.partner.servant.PartnerDefaultRequestHandler;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.common.test.util.JsonTestUtil.assertEquals;

/**
 * Тесты для {@link BusinessContactController}.
 *
 * @author Vadim Lyalin
 */
@DbUnitDataSet(before = "BusinessContactControllerTest.before.csv")
public class BusinessContactControllerTest extends FunctionalTest {

    @Autowired
    private PassportService passportService;

    @Autowired
    private BalanceService balanceService;

    @Autowired
    private BalanceContactService balanceContactService;

    @Autowired
    private LogbrokerEventPublisher<ContactChangesProtoLBEvent> logbrokerContactChangesEventPublisher;

    private static Stream<Arguments> testInviteUserToBusiness400Args() {
        BusinessContactRequest ownerRequest = new BusinessContactRequest();
        ownerRequest.setLogin("vasily@ya.ru");
        ownerRequest.setBusinessRoles(Set.of(InnerRole.BUSINESS_OWNER));

        BusinessContactRequest nonPassportWithBalanceRequest = new BusinessContactRequest();
        nonPassportWithBalanceRequest.setHasBalanceAccess(true);

        BusinessContactRequest severalClientsRequest = new BusinessContactRequest();
        severalClientsRequest.setLogin("pupkin@ya.ru");
        severalClientsRequest.setCampaignRoles(List.of(
                new BusinessContactRequest.CampaignRolesDto(2, Set.of(InnerRole.SHOP_ADMIN)),
                new BusinessContactRequest.CampaignRolesDto(5, Set.of(InnerRole.SHOP_ADMIN))));

        BusinessContactRequest severalBusinessClientsRequest = new BusinessContactRequest();
        severalBusinessClientsRequest.setLogin("pupkin40@ya.ru");
        severalBusinessClientsRequest.setBusinessRoles(Set.of(InnerRole.BUSINESS_ADMIN));

        BusinessContactRequest withCampaignFromAnotherBusinessRequest = getBusinessContactRequest(6);

        BusinessContactRequest invalidLoginRequest = new BusinessContactRequest();
        invalidLoginRequest.setLogin("vas ily@ya.ru");

        BusinessContactRequest invalidEmailsRequest = new BusinessContactRequest();
        invalidEmailsRequest.setLogin("vasily@ya.ru");
        invalidEmailsRequest.setEmails(List.of("vasi ly@ya.ru"));

        return Stream.of(
                Arguments.of(ownerRequest, "Business role must not be OWNER"),
                Arguments.of(nonPassportWithBalanceRequest, "Non-passport contact must not have balance access"),
                Arguments.of(severalClientsRequest, "User has not rights to invite to campaign"),
                // pupkin40 - бизнес админ в бизнесе 101 с клиентом 2, пытаемся его привязать к бизнесу 100 с клиентом 1
                Arguments.of(severalBusinessClientsRequest, "HAS_BALANCE_ACCESS_TO_ANOTHER_CLIENT"),
                //запрос с кампанией из другого бизнеса
                Arguments.of(withCampaignFromAnotherBusinessRequest, "User has not rights to invite to campaign"),
                Arguments.of(invalidLoginRequest, "Wrong login email format"),
                Arguments.of(invalidEmailsRequest, "Wrong email format")
        );
    }

    private static Stream<Arguments> testContactNotFoundArgs() {
        return Stream.of(
                // Получение контакта
                Arguments.of((Consumer<String>) (baseUrl) ->
                        FunctionalTestHelper.get(baseUrl + "businesses/100/contacts/999?_user_id=2")),
                // Получение ролей
                Arguments.of((Consumer<String>) (baseUrl) ->
                        FunctionalTestHelper.get(baseUrl + "businesses/100/contacts/999/roles?_user_id=2&campaign_id" +
                                "=1")),
                // Частичное изменение контакта
                Arguments.of((Consumer<String>) (baseUrl) ->
                        FunctionalTestHelper.patch(baseUrl + "businesses/100/contacts/999?_user_id=2", "{}")),
                // Полное изменение контакта
                Arguments.of((Consumer<String>) (baseUrl) ->
                        FunctionalTestHelper.put(baseUrl + "businesses/100/contacts/999?_user_id=2", "{}")),
                // Удаление контакта
                Arguments.of((Consumer<String>) (baseUrl) ->
                        FunctionalTestHelper.delete(baseUrl + "businesses/100/contacts/999?_user_id=2"))
        );
    }

    @Nonnull
    private static BusinessContactRequest getBusinessContactRequest(int campaignId) {
        BusinessContactRequest request = new BusinessContactRequest();
        request.setLogin("pupkin@ya.ru");
        request.setBusinessRoles(Set.of(InnerRole.BUSINESS_ADMIN));
        request.setAllCampaignRoles(Set.of(InnerRole.SHOP_OPERATOR));
        request.setCampaignRoles(List.of(new BusinessContactRequest.CampaignRolesDto(
                campaignId, Set.of(InnerRole.SHOP_ADMIN, InnerRole.SHOP_TECHNICAL))));
        request.setHasBalanceAccess(true);
        return request;
    }

    @BeforeEach
    void setUp() {
        when(logbrokerContactChangesEventPublisher.publishEventAsync(any()))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(invocation.getArgument(0)));

    }

    /**
     * Проверяет, что для нового пользователя создается приглашение в бизнес.
     */
    @Test
    @DbUnitDataSet(after = "BusinessContactControllerTest.testInviteUserToBusinessForNewUser.csv")
    void testInviteUserToBusinessForNewUser() throws JsonProcessingException {
        when(passportService.getUserInfo("pupkin@ya.ru")).thenReturn(
                new UserInfo(10, "Pupkin Vasiliy Ivanovich", "pupkin@ya.ru", "pupkin"));

        BusinessContactRequest request = new BusinessContactRequest();
        request.setLogin("pupkin@ya.ru");
        request.setBusinessRoles(Set.of(InnerRole.BUSINESS_ADMIN));

        ResponseEntity<String> response = FunctionalTestHelper.post(getInvitationsUrl(),
                OBJECT_MAPPER.writeValueAsString(request));
        assertEquals(response, "{\"inviteId\":\"6be8bf2d0361b3a0e67d745b42957768\"}");
    }

    /**
     * Проверяет, что для нового пользователя создается приглашение в бизнес.
     */
    @Test
    @DbUnitDataSet(
            before = "BusinessContactControllerTest.testInviteUserToBusinessWithExistedInvite.before.csv",
            after = {"BusinessContactControllerTest.testInviteUserToBusinessForNewUser.csv",
                    "BusinessContactControllerTest.testInviteUserToBusinessWithExistedInvite.after.csv"})
    void testInviteUserToBusinessWithExistedInvite() throws JsonProcessingException {
        when(passportService.getUserInfo("pupkin@ya.ru")).thenReturn(
                new UserInfo(10, "Pupkin Vasiliy Ivanovich", "pupkin@ya.ru", "pupkin"));

        BusinessContactRequest request = new BusinessContactRequest();
        request.setLogin("pupkin@ya.ru");
        request.setBusinessRoles(Set.of(InnerRole.BUSINESS_ADMIN));

        ResponseEntity<String> response = FunctionalTestHelper.post(getInvitationsUrl(),
                OBJECT_MAPPER.writeValueAsString(request));
        assertEquals(response, "{\"inviteId\":\"6be8bf2d0361b3a0e67d745b42957768\"}");
    }

    /**
     * Проверяет, что для нового пользователя создается приглашение в бизнес и кампании.
     */
    @Test
    @DbUnitDataSet(after = "BusinessContactControllerTest.testInviteUserToBusinessAndCampaignsForNewUser.csv")
    void testInviteUserToBusinessAndCampaignsForNewUser() throws JsonProcessingException {
        when(passportService.getUserInfo("pupkin@ya.ru")).thenReturn(new UserInfo(10, "Pupkin", "pupkin@ya.ru",
                "pupkin"));

        BusinessContactRequest request = getBusinessContactRequest(1);

        ResponseEntity<String> response = FunctionalTestHelper.post(getInvitationsUrl(),
                OBJECT_MAPPER.writeValueAsString(request));
        assertEquals(response, "{\"inviteId\":\"6be8bf2d0361b3a0e67d745b42957768\"}");
    }

    /**
     * Проверяет, что для пользователя с другим клиентом не создастся приглашение в бизнес и кампании.
     */
    @Test
    void testInviteUserToBusinessAndCampaignsForNewUserWithAnotherClient() {
        long uid = 10000L;
        long clientId = 666L;
        when(passportService.getUserInfo("pupkin@ya.ru"))
                .thenReturn(new UserInfo(uid, "Pupkin", "pupkin@ya.ru", "pupkin"));
        when(balanceContactService.getClientIdByUid(uid)).thenReturn(clientId);
        when(balanceService.getClient(clientId)).thenReturn(new ClientInfo(clientId, ClientType.OOO));
        when(balanceService.getClientByUid(uid)).thenReturn(new ClientInfo(clientId, ClientType.OOO));

        checkBadParamHasBalanceAccess(getBusinessContactRequest(1), "hasBalanceAccess");
    }

    /**
     * Проверяет, что для пользователя с другим клиентом но marketOnly - true создастся приглашение в бизнес и кампании.
     */
    @Test
    void testInviteUserToBusinessAndCampaignsForMarketOnlyUserWithAnotherClient() throws JsonProcessingException {
        long uid = 10000L;
        long clientId = 666L;
        when(passportService.getUserInfo("pupkin@ya.ru"))
                .thenReturn(new UserInfo(uid, "Pupkin", "pupkin@ya.ru", "pupkin"));
        when(balanceContactService.getClientIdByUid(uid)).thenReturn(clientId);
        when(balanceService.getClient(clientId)).thenReturn(new ClientInfo(clientId, ClientType.OOO));
        when(balanceService.getClientByUid(uid)).thenReturn(new ClientInfo(clientId, ClientType.OOO));

        BusinessContactRequest businessContactRequest = getBusinessContactRequest(1);
        businessContactRequest.setHasBalanceAccess(false);
        ResponseEntity<String> response = FunctionalTestHelper.post(getInvitationsUrl(),
                OBJECT_MAPPER.writeValueAsString(businessContactRequest));
        assertEquals(response, "{\"inviteId\":\"158884b0428ba8df3bdaae349ff2f535\"}");
    }

    /**
     * Проверяет, что для пользователя бухгалтера не создастся приглашение в бизнес и кампании.
     */
    @Test
    void testInviteUserToBusinessAndCampaignsForSpecBalanceClient() {
        long uid = 10000L;
        when(passportService.getUserInfo("pupkin@ya.ru"))
                .thenReturn(new UserInfo(uid, "Pupkin", "pupkin@ya.ru", "pupkin"));

        BalancePassportInfo balancePassportInfo = BalancePassportInfo.builder().setRepresentedClientIds(List.of(101L,
                102L)).build();
        when(balanceContactService.getPassportByUid(uid)).thenReturn(balancePassportInfo);

        checkBadParamHasBalanceAccess(getBusinessContactRequest(1), "hasBalanceAccess");
    }

    /**
     * Проверяет, что для пользователя не создастся приглашение в бизнес и кампании.
     */
    @Test
    void testInviteUserToBusinessAndCampaignsForUserWithAnotherClient() {
        when(passportService.getUserInfo("vasily@ya.ru")).thenReturn(new UserInfo(20, "Vasily", "vasily@ya.ru",
                "vasily"));
        BalancePassportInfo balancePassportInfo = BalancePassportInfo.builder().setRepresentedClientIds(List.of(101L,
                102L)).build();
        when(balanceContactService.getPassportByUid(20)).thenReturn(balancePassportInfo);

        BusinessContactRequest request = new BusinessContactRequest();
        request.setLogin("vasily@ya.ru");
        request.setHasBalanceAccess(true);
        request.setBusinessRoles(Set.of(InnerRole.BUSINESS_ADMIN));

        checkBadParamHasBalanceAccess(request, "login");
    }

    private void checkBadParamHasBalanceAccess(BusinessContactRequest businessContactRequest, String expectedField) {
        HttpClientErrorException.BadRequest exception = assertThrows(HttpClientErrorException.BadRequest.class,
                () -> FunctionalTestHelper.post(getInvitationsUrl(),
                        OBJECT_MAPPER.writeValueAsString(businessContactRequest)));

        MatcherAssert.assertThat(exception, HttpClientErrorMatcher.hasErrorCode(HttpStatus.BAD_REQUEST));
        MbiAsserts.assertJsonEquals("" +
                        "[{" +
                        "    \"code\":\"BAD_PARAM\"," +
                        "    \"details\":{" +
                        "       \"field\": \"" + expectedField + "\"," +
                        "       \"subcode\": \"HAS_BALANCE_ACCESS_TO_ANOTHER_CLIENT\"}" +
                        "}]",
                JsonTestUtil.parseJson(exception.getResponseBodyAsString())
                        .getAsJsonObject()
                        .get("errors")
                        .toString()
        );
    }

    /**
     * Проверяет, что для непаспортного пользователя сразу создается контакт.
     */
    @Test
    @DbUnitDataSet(after = "BusinessContactControllerTest.testInviteNonPassportUser.csv")
    void testInviteNonPassportUser() throws JsonProcessingException {
        ContactInfo contactInfo = new ContactInfo();
        contactInfo.setFirstName("Василий");
        contactInfo.setLastName("Пупкин");
        contactInfo.setPhone("123");

        BusinessContactRequest request = new BusinessContactRequest();
        request.setContactInfo(contactInfo);
        request.setBusinessRoles(Set.of(InnerRole.BUSINESS_ADMIN));
        request.setAllCampaignRoles(Set.of(InnerRole.SHOP_OPERATOR));
        request.setCampaignRoles(List.of(new BusinessContactRequest.CampaignRolesDto(
                1, Set.of(InnerRole.SHOP_ADMIN, InnerRole.SHOP_TECHNICAL))));

        ResponseEntity<String> response = FunctionalTestHelper.post(getInvitationsUrl(),
                OBJECT_MAPPER.writeValueAsString(request));
        assertEquals(response, "{}");

        verifyNoInteractions(logbrokerContactChangesEventPublisher);

    }

    /**
     * Проверяет, что для существующего пользователя добавляется связь с бизнесом.
     */
    @Test
    @DbUnitDataSet(after = "BusinessContactControllerTest.testInviteUserToBusinessForExistedContact.csv")
    void testInviteUserToBusinessForExistedContact() throws JsonProcessingException {
        when(passportService.getUserInfo("vasily@ya.ru")).thenReturn(new UserInfo(20, "Vasily", "vasily@ya.ru",
                "vasily"));

        BusinessContactRequest request = new BusinessContactRequest();
        request.setLogin("vasily@ya.ru");
        request.setHasBalanceAccess(true);
        request.setBusinessRoles(Set.of(InnerRole.BUSINESS_ADMIN));

        ResponseEntity<String> response = FunctionalTestHelper.post(getInvitationsUrl(),
                OBJECT_MAPPER.writeValueAsString(request));
        assertEquals(response, "{}");
    }

    /**
     * Проверяет, что для существующего пользователя добавляется связь с бизнесом.
     */
    @Test
    @DbUnitDataSet(before = "BusinessContactControllerTest.failed.differentBusinessTypePartners.before.csv")
    void testUpdateFailedDueToDifferentBusinessTypePartnersOnContact() {
        when(passportService.getUserInfo("tpladmin@yandex.ru"))
                .thenReturn(new UserInfo(160, "Тплщик", "tpladmin@yandex.ru", "tpladmin"));

        BusinessContactRequest request = new BusinessContactRequest();
        request.setLogin("tpladmin@yandex.ru");
        request.setHasBalanceAccess(true);
        request.setBusinessRoles(Set.of(InnerRole.BUSINESS_ADMIN));

        final HttpClientErrorException.BadRequest exception = Assertions.assertThrows(
                HttpClientErrorException.BadRequest.class,
                () -> FunctionalTestHelper.post(getInvitationsUrl(), OBJECT_MAPPER.writeValueAsString(request))
        );
        MatcherAssert.assertThat(
                exception,
                Matchers.allOf(
                        HttpClientErrorMatcher.hasErrorCode(HttpStatus.BAD_REQUEST),
                        HttpClientErrorMatcher.bodyMatches(
                                MbiMatchers.jsonPropertyMatches(
                                        "errors",
                                        MbiMatchers.jsonArrayEquals(""
                                                + "{"
                                                + "\"code\":\"DIFFERENT_BUSINESS_TYPE_PARTNERS_ON_CONTACT\","
                                                + "\"details\":" +
                                                "{" +
                                                "\"contact_id\":16," +
                                                "}"
                                                + "}"
                                        )
                                )
                        )
                )
        );
        verifyNoInteractions(logbrokerContactChangesEventPublisher);
    }

    /**
     * Проверяет, что для существующего пользователя, привязаного к сабклиенту другого бизнеса не добавляется связь.
     */
    @ParameterizedTest
    @EnumSource(InnerRole.class)
    void testInviteUserToAnotherClientForAlreadyLinkedExistedContact(InnerRole role) {
        when(passportService.getUserInfo("subclpupkin@yandex.ru")).thenReturn(new UserInfo(150, "Василий",
                "subclpupkin@yandex.ru",
                "subclpupkin"));
        when(balanceContactService.getClientIdByUid(eq(150L))).thenReturn(600L);
        BusinessContactRequest request = new BusinessContactRequest();
        request.setLogin("subclpupkin@yandex.ru");
        request.setHasBalanceAccess(true);
        request.setAllCampaignRoles(Set.of(role));

        checkBadParamHasBalanceAccess(request, "login");
    }

    /**
     * Тестируем, что падаем с 400 если контакт уже привязан в балансе
     */
    @Test
    void testContactAlreadyLinked() {
        when(passportService.getUserInfo("pupkin30@yandex.ru")).thenReturn(new UserInfo(30, "pupkin30",
                "pupkin30@yandex.ru",
                "pupkin30"));
        when(balanceContactService.getPassportByUid(30)).thenReturn(BalancePassportInfo.builder()
                .setUid(30)
                .setClientId(100)
                .build());

        BusinessContactRequest request = new BusinessContactRequest();
        request.setLogin("pupkin30@yandex.ru");
        request.setHasBalanceAccess(true);
        request.setBusinessRoles(Set.of(InnerRole.BUSINESS_ADMIN));

        HttpClientErrorException.BadRequest exception = assertThrows(HttpClientErrorException.BadRequest.class,
                () -> FunctionalTestHelper.post(getInvitationsUrl(), OBJECT_MAPPER.writeValueAsString(request)));

        assertThat(exception.getResponseBodyAsString(), containsString("ALREADY_LINKED"));
        verifyNoInteractions(logbrokerContactChangesEventPublisher);
    }

    /**
     * Проверяет, что падаем с 400 для невалидного запроса.
     */
    @ParameterizedTest(name = "[{index}] {1}")
    @MethodSource("testInviteUserToBusiness400Args")
    @DbUnitDataSet(before = "BusinessContactControllerTest.testInviteUserToBusiness400.before.csv")
    void testInviteUserToBusiness400(BusinessContactRequest request, String message) {
        when(passportService.getUserInfo("pupkin@ya.ru")).thenReturn(new UserInfo(10, "Pupkin", "pupkin@ya.ru",
                "pupkin"));
        when(passportService.getUserInfo("pupkin40@ya.ru")).thenReturn(new UserInfo(400, "Pupkin", "pupkin@ya.ru",
                "pupkin"));

        when(balanceContactService.getClientIdByUid(400L)).thenReturn(200L);

        HttpClientErrorException.BadRequest exception = assertThrows(HttpClientErrorException.BadRequest.class,
                () -> FunctionalTestHelper.post(getInvitationsUrl(), OBJECT_MAPPER.writeValueAsString(request)));

        assertThat(exception.getResponseBodyAsString(), containsString(message));
    }

    /**
     * Проверяет получение ролей контакта.
     *
     * @see BusinessContactController#getCampaignRoles
     */
    @Test
    void testGetCampaignRoles() {
        ResponseEntity<String> response = FunctionalTestHelper.get(baseUrl +
                "businesses/100/contacts/3/roles?_user_id=2&campaign_id=1");
        assertEquals(response, "{\"campaignRoles\":" +
                "[{\"campaignId\":1,\"campaignRoleTypes\":[\"SHOP_ADMIN\"]}]}");
    }

    private String getInvitationsUrl() {
        return baseUrl + "businesses/100/contacts/invitations?_user_id=40";
    }

    /**
     * Тестирование частичного обновления контакта с пустым запросом. Проверяем, что ничего не изменилось
     *
     * @see BusinessContactController#mergeContactRoles
     */
    @Test
    @DbUnitDataSet(after = "BusinessContactControllerTest.before.csv")
    void testNoMergeContactRoles() throws JsonProcessingException {
        var body = new BusinessContactRequest();

        ResponseEntity<String> response = FunctionalTestHelper.patch(
                baseUrl + "businesses/100/contacts/3?_user_id=40",
                OBJECT_MAPPER.writeValueAsString(body)
        );
        assertThat(response.getStatusCode(), is(HttpStatus.OK));
    }

    /**
     * Тестирование частичного обновления контакта.
     *
     * @see BusinessContactController#mergeContactRoles
     */
    @Test
    @DbUnitDataSet(after = "BusinessContactControllerTest.testMergeContactRoles.after.csv")
    void testMergeContactRoles() throws JsonProcessingException {
        var body = new BusinessContactRequest();
        body.setAllCampaignRoles(Set.of(InnerRole.SHOP_ADMIN));
        body.setCampaignRoles(List.of(new BusinessContactRequest.CampaignRolesDto(
                2, Set.of(InnerRole.SHOP_OPERATOR))));
        body.setBusinessRoles(Set.of(InnerRole.BUSINESS_ADMIN));

        ResponseEntity<String> response = FunctionalTestHelper.patch(
                baseUrl + "businesses/100/contacts/3?_user_id=40",
                OBJECT_MAPPER.writeValueAsString(body)
        );
        assertThat(response.getStatusCode(), is(HttpStatus.OK));
    }

    /**
     * Тестирование частичного обновления контакта.
     *
     * @see BusinessContactController#mergeContactRoles
     */
    @Test
    @DbUnitDataSet(after = "BusinessContactControllerTest.testMergeContactRolesAllCampaigns.after.csv")
    void testMergeContactRolesAllCampaigns() throws JsonProcessingException {
        var body = new BusinessContactRequest();
        body.setAllCampaignRoles(Set.of(InnerRole.SHOP_ADMIN));

        ResponseEntity<String> response = FunctionalTestHelper.patch(
                baseUrl + "businesses/100/contacts/2?_user_id=30",
                OBJECT_MAPPER.writeValueAsString(body)
        );
        assertThat(response.getStatusCode(), is(HttpStatus.OK));
    }

    /**
     * Тестирование запрета на частичное обновление контакта в чужой кампании.
     *
     * @see BusinessContactController#mergeContactRoles
     */
    @Test
    void testMergeContactRolesWrongCampaign() {
        var body = new BusinessContactRequest();
        body.setAllCampaignRoles(Set.of(InnerRole.SHOP_ADMIN));
        body.setCampaignRoles(List.of(
                new BusinessContactRequest.CampaignRolesDto(2, Set.of(InnerRole.SHOP_OPERATOR)),
                new BusinessContactRequest.CampaignRolesDto(6, Set.of(InnerRole.SHOP_OPERATOR))
        ));
        body.setBusinessRoles(Set.of(InnerRole.BUSINESS_ADMIN));

        HttpClientErrorException.BadRequest badResponse = assertThrows(HttpClientErrorException.BadRequest.class,
                () -> FunctionalTestHelper.patch(
                        baseUrl + "businesses/100/contacts/3?_user_id=40",
                        OBJECT_MAPPER.writeValueAsString(body)));
        MatcherAssert.assertThat(badResponse, HttpClientErrorMatcher.hasErrorCode(HttpStatus.BAD_REQUEST));
        MbiAsserts.assertJsonEquals("" +
                        "[{" +
                        "    \"code\":\"BAD_PARAM\"," +
                        "    \"message\": \"User has not rights to invite to campaign 6\"," +
                        "    \"details\":{}" +
                        "}]",
                JsonTestUtil.parseJson(badResponse.getResponseBodyAsString())
                        .getAsJsonObject()
                        .get("errors")
                        .toString()
        );
    }

    /**
     * Тестирование полного обновления контакта. Проверяет, что
     * <ol>
     *     <li>Роли для непереданных кампаний 1 и 3 редактируются</li>
     *     <li>Роли для кампании 4 не из бизнеса остаются</li>
     * </ol>
     *
     * @see BusinessContactController#updateContactRoles(BusinessContactRequest, long, long,
     * PartnerDefaultRequestHandler.PartnerHttpServRequest)
     */
    @Test
    @DbUnitDataSet(after = "BusinessContactControllerTest.testUpdateContactRoles.after.csv")
    void testUpdateContactRoles() throws JsonProcessingException {
        var body = new BusinessContactRequest();
        body.setEmails(List.of("spbtester@yandex.ru", "spbtester@ya.ru"));
        body.setAllCampaignRoles(Set.of(InnerRole.SHOP_ADMIN));
        body.setCampaignRoles(List.of(new BusinessContactRequest.CampaignRolesDto(
                2, Set.of(InnerRole.SHOP_OPERATOR))));
        body.setBusinessRoles(Set.of(InnerRole.BUSINESS_ADMIN));
        body.setHasBalanceAccess(false);

        ResponseEntity<String> response = FunctionalTestHelper.put(
                baseUrl + "businesses/100/contacts/3?_user_id=40",
                OBJECT_MAPPER.writeValueAsString(body)
        );
        assertThat(response.getStatusCode(), is(HttpStatus.OK));

        ArgumentCaptor<ContactChangesProtoLBEvent> captor = ArgumentCaptor.forClass(ContactChangesProtoLBEvent.class);
        verify(logbrokerContactChangesEventPublisher).publishEventAsync(captor.capture());
        ContactChangesProtoLBEvent lbEvent = captor.getValue();
        ContactDataOuterClass.ContactData event = lbEvent.getPayload();
        assertThat(event.getContactId(), is(3L));
        assertThat(event.getGeneralInfo().getActionType(), is(GeneralData.ActionType.UPDATE));
    }

    /**
     * Тестирование полного удаления контакта.
     *
     * @see BusinessContactController#deleteContactRoles
     */
    @Test
    @DbUnitDataSet(before = "BusinessContactControllerTest.testDeleteContact.before.csv",
            after = "BusinessContactControllerTest.testDeleteContact.after.csv")
    void testDeleteContact() {
        FunctionalTestHelper.delete(baseUrl + "businesses/100/contacts/3?_user_id=2");

        ArgumentCaptor<ContactChangesProtoLBEvent> captor = ArgumentCaptor.forClass(ContactChangesProtoLBEvent.class);
        verify(logbrokerContactChangesEventPublisher).publishEventAsync(captor.capture());
        ContactChangesProtoLBEvent lbEvent = captor.getValue();
        ContactDataOuterClass.ContactData event = lbEvent.getPayload();
        assertThat(event.getContactId(), is(3L));
        assertThat(event.getGeneralInfo().getActionType(), is(GeneralData.ActionType.DELETE));
    }

    /**
     * Тестирование удаления контакта из бизнеса.
     *
     * @see BusinessContactController#deleteContactRoles
     */
    @Test
    @DbUnitDataSet(before = "BusinessContactControllerTest.testDeleteContactRoles.before.csv",
            after = "BusinessContactControllerTest.testDeleteContactRoles.after.csv")
    void testDeleteContactRoles() {
        FunctionalTestHelper.delete(baseUrl + "businesses/100/contacts/3?_user_id=2");
    }

    /**
     * Тестирование удаления владельца бизнеса.
     *
     * @see BusinessContactController#deleteContactRoles
     * @see InnerRole#BUSINESS_OWNER
     */
    @Test
    void testDeleteBusinessOwner() {
        HttpClientErrorException.BadRequest exception = assertThrows(HttpClientErrorException.BadRequest.class,
                () -> FunctionalTestHelper.delete(baseUrl + "businesses/100/contacts/4?_user_id=40")
        );
        MbiAsserts.assertJsonEquals("" +
                        "[{" +
                        "    \"code\":\"UNAUTHORIZED\"," +
                        "    \"details\":{" +
                        "       \"subcode\":\"FORBIDDEN_OPERATION\"," +
                        "       \"reason\":\"Could not remove Business Owner contact\"" +
                        "    }" +
                        "}]",
                JsonTestUtil.parseJson(exception.getResponseBodyAsString())
                        .getAsJsonObject()
                        .get("errors")
                        .toString()
        );
    }

    /**
     * Тестирование получения информации о пользователе.
     *
     * @see BusinessContactController#getContact
     */
    @Test
    @DbUnitDataSet(before = "BusinessContactControllerTest.emailParams.before.csv")
    void testGetContact() {
        ResponseEntity<String> response = FunctionalTestHelper.get(baseUrl + "businesses/100/contacts/3?_user_id=2");
        String expectedJson = StringTestUtil.getString(getClass(), "expected/contact-3.json");
        JSONObject actualJson = new JSONObject(response.getBody()).getJSONObject("result");
        JSONAssert.assertEquals(expectedJson, actualJson, JSONCompareMode.STRICT_ORDER);
    }

    /**
     * Тестирование получения информации о пользователе не из бизнеса.
     *
     * @see BusinessContactController#getContact
     */
    @Test
    @DbUnitDataSet(before = "BusinessContactControllerTest.emailParams.before.csv")
    void testGetNotBusinessContact() {
        HttpClientErrorException.BadRequest exception = assertThrows(HttpClientErrorException.BadRequest.class,
                () -> FunctionalTestHelper.get(baseUrl + "businesses/100/contacts/15?_user_id=2")
        );
        MbiAsserts.assertJsonEquals("" +
                        "[{" +
                        "    \"code\":\"UNAUTHORIZED\"," +
                        "    \"details\":{" +
                        "       \"reason\":\"contactId is not related to businessId\"," +
                        "       \"subcode\":\"FORBIDDEN_OPERATION\"," +
                        "    }" +
                        "}]",
                JsonTestUtil.parseJson(exception.getResponseBodyAsString())
                        .getAsJsonObject()
                        .get("errors")
                        .toString()
        );
    }

    /**
     * Тестирование получения информации о неподтвержденном пользователе из приглашения.
     *
     * @see BusinessContactController#getContact
     */
    @Test
    @DbUnitDataSet(before = "BusinessContactControllerTest.testGetInvite.before.csv")
    void testGetInviteUserToBusiness() {
        ResponseEntity<String> response = FunctionalTestHelper.get(baseUrl + "businesses/100/contacts/10?_user_id=2");
        String expectedJson = StringTestUtil.getString(getClass(), "expected/invitation.json");
        JSONObject actualJson = new JSONObject(response.getBody()).getJSONObject("result");
        JSONAssert.assertEquals(expectedJson, actualJson, JSONCompareMode.STRICT_ORDER);
    }

    /**
     * Проверяет, что для несуществующего контакта разные ручки возвращают 404.
     */
    @ParameterizedTest(name = "{index}")
    @MethodSource("testContactNotFoundArgs")
    void testContactNotFound(Consumer<String> consumer) {
        HttpClientErrorException.NotFound notFound = assertThrows(HttpClientErrorException.NotFound.class,
                () -> consumer.accept(baseUrl));

        MbiAsserts.assertJsonEquals("" +
                        "[{" +
                        "    \"code\":\"BAD_PARAM\"," +
                        "    \"details\":{" +
                        "       \"entity_name\":\"contact\"," +
                        "       \"subcode\":\"ENTITY_NOT_FOUND\"," +
                        "       \"entity_id\":\"999\"" +
                        "    }" +
                        "}]",
                JsonTestUtil.parseJson(notFound.getResponseBodyAsString())
                        .getAsJsonObject()
                        .get("errors")
                        .toString()
        );
    }

    /**
     * Проверяет, что пользователь не может пригласить сам себя с затиранием роли
     */
    @Test
    @DbUnitDataSet(after = "BusinessContactControllerTest.testSelfInvite.after.csv")
    void testSelfInvite() throws JsonProcessingException {
        when(passportService.getUserInfo("pupkin@ya.ru")).thenReturn(
                new UserInfo(40, "Pupkin Vasiliy Ivanovich", "pupkin@ya.ru", "pupkin"));

        BusinessContactRequest request = new BusinessContactRequest();
        request.setLogin("pupkin@ya.ru");
        request.setBusinessRoles(Set.of(InnerRole.BUSINESS_ADMIN));

        FunctionalTestHelper.post(getInvitationsUrl(),
                OBJECT_MAPPER.writeValueAsString(request));
    }
}
