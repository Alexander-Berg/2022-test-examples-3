package ru.yandex.market.mbi.api.controller.operation.partner.registration;

import java.util.Arrays;
import java.util.List;

import org.apache.http.HttpStatus;
import org.assertj.core.api.Assertions;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.market.checkout.checkouter.client.CheckouterAPI;
import ru.yandex.market.common.balance.model.ClientType;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.balance.BalanceContactService;
import ru.yandex.market.core.balance.BalanceService;
import ru.yandex.market.core.balance.model.ClientInfo;
import ru.yandex.market.core.balance.model.FullClientInfo;
import ru.yandex.market.core.balance.model.OrderInfo;
import ru.yandex.market.core.geobase.model.RegionConstants;
import ru.yandex.market.core.passport.PassportService;
import ru.yandex.market.core.passport.model.UserInfo;
import ru.yandex.market.core.stocks.FF4ShopsClient;
import ru.yandex.market.fulfillment.stockstorage.client.StockStorageFFIntervalClient;
import ru.yandex.market.id.MarketIdServiceGrpc;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.nesu.client.NesuClient;
import ru.yandex.market.mbi.api.config.FunctionalTest;
import ru.yandex.market.mbi.environment.EnvironmentService;
import ru.yandex.market.mbi.open.api.client.model.ApiError;
import ru.yandex.market.mbi.open.api.client.model.BusinessNameCheckResponse;
import ru.yandex.market.mbi.open.api.client.model.BusinessRegistrationRequest;
import ru.yandex.market.mbi.open.api.client.model.ContactInfo;
import ru.yandex.market.mbi.open.api.client.model.OrganizationInfo;
import ru.yandex.market.mbi.open.api.client.model.OrganizationType;
import ru.yandex.market.mbi.open.api.client.model.PartnerApplicationRequest;
import ru.yandex.market.mbi.open.api.client.model.PartnerManagerSetUpRequest;
import ru.yandex.market.mbi.open.api.client.model.PartnerModerationSetUpRequest;
import ru.yandex.market.mbi.open.api.client.model.PartnerNotificationContact;
import ru.yandex.market.mbi.open.api.client.model.PartnerPlacementType;
import ru.yandex.market.mbi.open.api.client.model.PartnerRegistrationRequest;
import ru.yandex.market.mbi.open.api.client.model.PartnerRegistrationResponse;
import ru.yandex.market.mbi.open.api.client.model.SignatoryDocType;
import ru.yandex.market.mbi.open.api.client.model.SignatoryInfo;
import ru.yandex.market.mbi.open.api.client.model.TaxSystemDTO;
import ru.yandex.market.mbi.open.api.client.model.VatInfo;
import ru.yandex.market.mbi.open.api.client.model.VatRateDTO;
import ru.yandex.market.mbi.open.api.client.model.VatSourceDTO;
import ru.yandex.market.mbi.open.api.exception.MbiOpenApiClientResponseException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.core.notification.service.PartnerNotificationApiServiceTest.verifySentNotificationType;
import static ru.yandex.market.mbi.api.service.shop.SimpleShopRegistrationHelper.MANAGER_ID;

/**
 * Тесты для {@link PartnerRegistrationController}
 */
@DbUnitDataSet(before = "PartnerRegistrationControllerTest.before.csv")
class PartnerRegistrationControllerTest extends FunctionalTest {
    private static final long UID = 1001001;
    private static final long MANAGER_UID = 1001002;
    private static final long DBS_AGENCY_MANAGER_ID = 501;
    private static final long AGENCY_USER_ID = 222222;
    private static final long CLIENT_ID = 1001;

    private static final PartnerNotificationContact CONTACT = new PartnerNotificationContact()
            .firstName("Alex")
            .lastName("Tr")
            .email("lex@yandex.ru")
            .phone("89164490000");

    @Autowired
    @Qualifier("impatientBalanceService")
    private BalanceContactService balanceContactService;

    @Autowired
    @Qualifier("impatientBalanceService")
    private BalanceService balanceService;

    @Autowired
    private PassportService passportService;

    @Autowired
    private MarketIdServiceGrpc.MarketIdServiceImplBase marketIdServiceImplBase;

    @Autowired
    private CheckouterAPI checkouterClient;

    @Autowired
    private StockStorageFFIntervalClient intervalRestClient;

    @Autowired
    private LMSClient lmsClient;

    @Autowired
    private NesuClient nesuClient;

    @Autowired
    private FF4ShopsClient ff4ShopsClient;

    @Autowired
    @Qualifier("environmentService")
    private EnvironmentService environmentService;

    void verifyInteractions() {
        verifyNoInteractions(marketIdServiceImplBase);
        verifyNoInteractions(intervalRestClient);
        verifyNoInteractions(lmsClient);
        verifyNoInteractions(nesuClient);
        verifyNoInteractions(checkouterClient);
        verifyNoInteractions(ff4ShopsClient);
    }

    @Test
    void testPlacementTypeNotValid() {
        var exception = assertThrows(
                MbiOpenApiClientResponseException.class,
                () -> getMbiOpenApiClient().registerPartner(
                        UID,
                        new PartnerRegistrationRequest()
                )
        );
        assertThat(exception.getHttpErrorCode()).isEqualTo(HttpStatus.SC_BAD_REQUEST);
        assertThat(exception.getApiError().getMessage()).isEqualTo("partnerPlacementType must not be null");
        verifyInteractions();
    }

    @Test
    void testRegistrationByAgencyNoShopOwner() {
        when(balanceService.getClientByUid(eq(AGENCY_USER_ID)))
                .thenReturn(new ClientInfo(AGENCY_USER_ID, ClientType.OOO, true, AGENCY_USER_ID));
        var exception = assertThrows(
                MbiOpenApiClientResponseException.class,
                () ->  getMbiOpenApiClient().registerPartner(
                        AGENCY_USER_ID,
                        new PartnerRegistrationRequest()
                                .partnerPlacementType(PartnerPlacementType.FBY)
                                .businessName("New business")
                                .regionId(RegionConstants.MOSCOW)
                                .partnerNotificationContact(CONTACT)
                )
        );
        assertThat(exception.getHttpErrorCode()).isEqualTo(HttpStatus.SC_BAD_REQUEST);
        assertThat(exception.getApiError().getMessageCode())
                .isEqualTo(ApiError.MessageCodeEnum.PARTNER_OWNER_NOT_SET);
        verifyInteractions();
    }

    @Test
    @DisplayName("Ошибка, когда агентство регистрирует магазин на агентство")
    void testRegistrationByAgencyOnAgency() {
        when(balanceService.getClientByUid(eq(AGENCY_USER_ID)))
                .thenReturn(new ClientInfo(AGENCY_USER_ID, ClientType.OOO, true, AGENCY_USER_ID));
        var exception = assertThrows(
                MbiOpenApiClientResponseException.class,
                () ->  getMbiOpenApiClient().registerPartner(
                        AGENCY_USER_ID,
                        new PartnerRegistrationRequest()
                                .partnerPlacementType(PartnerPlacementType.FBY)
                                .businessName("New business")
                                .regionId(RegionConstants.MOSCOW)
                                .shopOwnerId(AGENCY_USER_ID)
                                .partnerNotificationContact(CONTACT)
                )
        );
        assertThat(exception.getHttpErrorCode()).isEqualTo(HttpStatus.SC_BAD_REQUEST);
        assertThat(exception.getApiError().getMessageCode())
                .isEqualTo(ApiError.MessageCodeEnum.PARTNER_REGISTRATION_BY_AGENCY);
        verifyInteractions();
    }

    @Test
    void testContactNotFound() {
        when(balanceService.getClientByUid(eq(UID)))
                .thenReturn(new ClientInfo(CLIENT_ID, ClientType.OOO, false, 0));
        var exception = assertThrows(
                MbiOpenApiClientResponseException.class,
                () -> getMbiOpenApiClient().registerPartner(
                        UID,
                        new PartnerRegistrationRequest()
                                .partnerPlacementType(PartnerPlacementType.FBY)
                                .businessName("Business")
                )
        );
        assertThat(exception.getHttpErrorCode()).isEqualTo(HttpStatus.SC_BAD_REQUEST);
        assertThat(exception.getApiError().getMessageCode()).isEqualTo(ApiError.MessageCodeEnum.CONTACT_NOT_FOUND);
        verifyInteractions();
    }

    @Test
    @DbUnitDataSet(before = "PartnerRegistrationControllerTest.testBusinessNameIsNotUnique.csv")
    void testBusinessNameIsNotUnique() {
        when(balanceService.getClientByUid(eq(UID)))
                .thenReturn(new ClientInfo(CLIENT_ID, ClientType.OOO, false, 0));
        when(passportService.getUserInfo(eq(UID)))
                .thenReturn(new UserInfo(UID, "Tr Alex", "lex@yandex.ru", "lex"));
        var exception = assertThrows(
                MbiOpenApiClientResponseException.class,
                () -> getMbiOpenApiClient().registerPartner(
                        UID,
                        new PartnerRegistrationRequest()
                                .partnerPlacementType(PartnerPlacementType.FBY)
                                .businessName("Business")
                                .partnerNotificationContact(CONTACT)
                )
        );
        assertThat(exception.getHttpErrorCode()).isEqualTo(HttpStatus.SC_BAD_REQUEST);
        assertThat(exception.getApiError().getMessageCode())
                .isEqualTo(ApiError.MessageCodeEnum.NOT_UNIQUE_BUSINESS_NAME);
        verifyInteractions();
    }

    @Test
    @DbUnitDataSet(before = {"PartnerRegistrationControllerTest.testBusinessNameIsNotUnique.csv",
            "PartnerRegistrationControllerTest.prepaid.csv"},
            after = "testBusinessHasDifferentClientPostpaid.after.csv")
    @DisplayName("Разрешаем регать на бизнес с предоплатным партнером нового поставщика, тк он постоплатный")
    void testBusinessHasDifferentClientPrepaid() {
        when(balanceService.getClientByUid(eq(UID)))
                .thenReturn(new ClientInfo(CLIENT_ID, ClientType.OOO, false, 0));
        when(passportService.getUserInfo(eq(UID)))
                .thenReturn(new UserInfo(UID, "Tr Alex", "lex@yandex.ru", "lex"));
        getMbiOpenApiClient().registerPartner(
                UID,
                new PartnerRegistrationRequest()
                        .partnerPlacementType(PartnerPlacementType.FBY)
                        .businessId(10L)
                        .partnerNotificationContact(CONTACT)
        );
        verifyInteractions();
    }

    @Test
    @DbUnitDataSet(before = "PartnerRegistrationControllerTest.testBusinessNameIsNotUnique.csv",
            after = "testBusinessHasDifferentClientPostpaid.after.csv")
    @DisplayName("Разрешаем регать на постоплатный бизнес еще одного постоплатного партнера")
    void testBusinessHasDifferentClientPostpaid() {
        when(balanceService.getClientByUid(eq(UID)))
                .thenReturn(new ClientInfo(CLIENT_ID, ClientType.OOO, false, 0));
        when(passportService.getUserInfo(eq(UID)))
                .thenReturn(new UserInfo(UID, "Tr Alex", "lex@yandex.ru", "lex"));
        getMbiOpenApiClient().registerPartner(
                UID,
                new PartnerRegistrationRequest()
                        .partnerPlacementType(PartnerPlacementType.FBY)
                        .businessId(10L)
                        .partnerNotificationContact(CONTACT)
        );
        verifyInteractions();
    }

    @Test
    void testBusinessHasWrongName() {
        when(balanceService.getClientByUid(eq(UID)))
                .thenReturn(new ClientInfo(CLIENT_ID, ClientType.OOO, false, 0));
        when(passportService.getUserInfo(eq(UID)))
                .thenReturn(new UserInfo(UID, "Tr Alex", "lex@yandex.ru", "lex"));
        var exception = assertThrows(
                MbiOpenApiClientResponseException.class,
                () -> getMbiOpenApiClient().registerPartner(
                        UID,
                        new PartnerRegistrationRequest()
                                .partnerPlacementType(PartnerPlacementType.FBY)
                                .businessName("h@h@h@h@")
                                .partnerNotificationContact(CONTACT)
                )
        );
        assertThat(exception.getHttpErrorCode()).isEqualTo(HttpStatus.SC_BAD_REQUEST);
        assertThat(exception.getApiError().getMessageCode())
                .isEqualTo(ApiError.MessageCodeEnum.INCORRECT_BUSINESS_NAME);
        verifyInteractions();
    }

    @Test
    @DbUnitDataSet(before = "PartnerRegistrationControllerTest.testBusinessHasDifferentServiceTypes.csv")
    void testBusinessHasDifferentServiceTypes() {
        when(balanceService.getClientByUid(eq(UID)))
                .thenReturn(new ClientInfo(CLIENT_ID, ClientType.OOO, false, 0));
        when(passportService.getUserInfo(eq(UID)))
                .thenReturn(new UserInfo(UID, "Tr Alex", "lex@yandex.ru", "lex"));
        var exception = assertThrows(
                MbiOpenApiClientResponseException.class,
                () -> getMbiOpenApiClient().registerPartner(
                        UID,
                        new PartnerRegistrationRequest()
                                .partnerPlacementType(PartnerPlacementType.FBY)
                                .businessId(10L)
                                .partnerNotificationContact(CONTACT)
                )
        );
        assertThat(exception.getHttpErrorCode()).isEqualTo(HttpStatus.SC_BAD_REQUEST);
        assertThat(exception.getApiError().getMessageCode())
                .isEqualTo(ApiError.MessageCodeEnum.DIFFERENT_BUSINESS_TYPE_PARTNERS_ON_CONTACT);
        verifyInteractions();
    }

    @Test
    @DbUnitDataSet(
            before = "PartnerRegistrationControllerTest.testCreateSupplierFromBusinessData.before.csv",
            after = "PartnerRegistrationControllerTest.testCreateSupplierFromBusinessData.after.csv"
    )
    void testCreateSupplierFromBusinessData() {
        when(balanceService.getClientByUid(eq(UID)))
                .thenReturn(new ClientInfo(CLIENT_ID, ClientType.OOO, false, 0));
        when(passportService.getUserInfo(eq(UID)))
                .thenReturn(new UserInfo(UID, "Tr Alex", "lex@yandex.ru", "pup"));
        when(balanceContactService.getUidsByClient(eq(CLIENT_ID))).thenReturn(List.of(UID));
        getMbiOpenApiClient().registerPartner(
                UID,
                new PartnerRegistrationRequest()
                        .partnerPlacementType(PartnerPlacementType.FBY)
                        .businessId(10L)
        );
        verifyInteractions();
    }

    @Test
    @DbUnitDataSet(
            before = "PartnerRegistrationControllerTest.testRegisterSupplierOnBusinessWithSubclients.before.csv",
            after = "PartnerRegistrationControllerTest.testRegisterSupplierOnBusinessWithSubclients.after.csv"
    )
    void testRegisterSupplierOnBusinessWithSubclients() {
        when(balanceService.getClientByUid(eq(UID)))
                .thenReturn(new ClientInfo(CLIENT_ID, ClientType.OOO, false, 0));
        when(passportService.getUserInfo(eq(UID)))
                .thenReturn(new UserInfo(UID, "Tr Alex", "lex@yandex.ru", "lex"));
        getMbiOpenApiClient().registerPartner(
                UID,
                new PartnerRegistrationRequest()
                        .partnerPlacementType(PartnerPlacementType.FBY)
                        .businessId(400L)
                        .domain("dom.ru")
                        .partnerNotificationContact(CONTACT)
        );
        verifyInteractions();
    }

    @Test
    @DbUnitDataSet(
            after = "PartnerRegistrationControllerTest.testRegisterFbyPlus.after.csv"
    )
    void testRegisterFbyPlus() {
        when(balanceService.getClientByUid(eq(UID)))
                .thenReturn(new ClientInfo(CLIENT_ID, ClientType.OOO, false, 0));
        when(passportService.getUserInfo(eq(UID)))
                .thenReturn(new UserInfo(UID, "Tr Alex", "lex@yandex.ru", "lex"));
        getMbiOpenApiClient().registerPartner(
                UID,
                new PartnerRegistrationRequest()
                        .partnerPlacementType(PartnerPlacementType.FBY_PLUS)
                        .businessName("New business")
                        .regionId(RegionConstants.MOSCOW)
                        .partnerNotificationContact(CONTACT)
        );
        verifyInteractions();
    }

    @Test
    @DbUnitDataSet(
            after = "PartnerRegistrationControllerTest.testRegisterFbs.after.csv"
    )
    void testRegisterFbs() {
        when(balanceService.getClientByUid(eq(UID)))
                .thenReturn(new ClientInfo(CLIENT_ID, ClientType.OOO, false, 0));
        when(passportService.getUserInfo(eq(UID)))
                .thenReturn(new UserInfo(UID, "Tr Alex", "lex@yandex.ru", "lex"));
        var response = getMbiOpenApiClient().registerPartner(
                UID,
                new PartnerRegistrationRequest()
                        .partnerPlacementType(PartnerPlacementType.FBS)
                        .businessName("New business")
                        .regionId(RegionConstants.MOSCOW)
                        .partnerNotificationContact(CONTACT)
        );
        assertThat(response).isEqualTo(new PartnerRegistrationResponse()
                .partnerId(1L)
                .businessId(2L)
                .contactId(1L)
                .campaignId(1L)
                .partnerContact(CONTACT)
        );
        verifyInteractions();
    }

    @Test
    @DbUnitDataSet(after = "PartnerRegistrationControllerTest.testRegisterSelfEmployedFbs.after.csv")
    void testRegisterSelfEmployedFBS() {
        when(balanceService.getClientByUid(eq(UID)))
                .thenReturn(new ClientInfo(CLIENT_ID, ClientType.OOO, false, 0));
        when(passportService.getUserInfo(eq(UID)))
                .thenReturn(new UserInfo(UID, "Tr Alex", "lex@yandex.ru", "lex"));
        var response = getMbiOpenApiClient().registerPartner(
                UID,
                new PartnerRegistrationRequest()
                        .partnerPlacementType(PartnerPlacementType.FBS)
                        .businessName("New business")
                        .regionId(RegionConstants.MOSCOW)
                        .partnerNotificationContact(CONTACT)
                        .isSelfEmployed(true)
        );
        assertThat(response).isEqualTo(new PartnerRegistrationResponse()
                .partnerId(1L)
                .businessId(2L)
                .contactId(1L)
                .campaignId(1L)
                .partnerContact(CONTACT)
        );
        verifyInteractions();
    }

    @Test
    @DbUnitDataSet(after = "PartnerRegistrationControllerTest.testRegisterSelfEmployedDbs.after.csv")
    void testRegisterSelfEmployedDBS() {
        when(balanceService.getClientByUid(eq(UID)))
                .thenReturn(new ClientInfo(CLIENT_ID, ClientType.OOO, false, 0));
        when(passportService.getUserInfo(eq(UID)))
                .thenReturn(new UserInfo(UID, "Tr Alex", "lex@yandex.ru", "lex"));
        var response = getMbiOpenApiClient().registerPartner(
                UID,
                new PartnerRegistrationRequest()
                        .partnerPlacementType(PartnerPlacementType.DBS)
                        .businessName("New business")
                        .regionId(10740L)
                        .partnerNotificationContact(CONTACT)
                        .isSelfEmployed(true)
        );
        assertThat(response).isEqualTo(new PartnerRegistrationResponse()
                .partnerId(1L)
                .businessId(2L)
                .contactId(1L)
                .campaignId(1L)
                .partnerContact(CONTACT)
        );
        verifyInteractions();
    }

    @Test
    @DbUnitDataSet(after = "PartnerRegistrationControllerTest.testRegisterB2BFBY.after.csv")
    void testRegisterB2BFBY() {
        when(balanceService.getClientByUid(eq(UID)))
                .thenReturn(new ClientInfo(CLIENT_ID, ClientType.OOO, false, 0));
        when(passportService.getUserInfo(eq(UID)))
                .thenReturn(new UserInfo(UID, "Tr Alex", "lex@yandex.ru", "lex"));
        var response = getMbiOpenApiClient().registerPartner(
                UID,
                new PartnerRegistrationRequest()
                        .partnerPlacementType(PartnerPlacementType.FBY)
                        .businessName("New business")
                        .regionId(10740L)
                        .partnerNotificationContact(CONTACT)
                        .isB2BSeller(true)
        );
        assertThat(response).isEqualTo(new PartnerRegistrationResponse()
                .partnerId(1L)
                .businessId(2L)
                .contactId(1L)
                .campaignId(1L)
                .partnerContact(CONTACT)
        );
        verifyInteractions();
    }

    @Test
    @DbUnitDataSet(before = "PartnerRegistrationControllerTest.testBusinessNotFoundException.csv")
    void testBusinessNotFoundException() {
        when(balanceService.getClientByUid(eq(UID)))
                .thenReturn(new ClientInfo(CLIENT_ID, ClientType.OOO, false, 0));
        var exception = assertThrows(
                MbiOpenApiClientResponseException.class,
                () -> getMbiOpenApiClient().registerPartner(
                        UID,
                        new PartnerRegistrationRequest()
                                .partnerPlacementType(PartnerPlacementType.DBS)
                                .businessId(404L)
                                .partnerNotificationContact(CONTACT)
                )
        );
        assertThat(exception.getHttpErrorCode()).isEqualTo(HttpStatus.SC_BAD_REQUEST);
        assertThat(exception.getApiError().getMessageCode()).isEqualTo(ApiError.MessageCodeEnum.BUSINESS_NOT_FOUND);
        verifyInteractions();
    }

    @Test
    @DbUnitDataSet(before = "PartnerRegistrationControllerTest.nonSubclientContactAlreadyLinked.before.csv")
    void testNonSubclientRegistration_ContactAlreadyLinkedToAnotherClient_noException() {
        final var ownerClientId = 555L;
        final var ownerUid = UID;
        final var ownerLogin = "lex";
        var ownerClientInfo = new ClientInfo(555L, ClientType.PHYSICAL);

        final var otherUid = 99999999L;

        when(passportService.findUid(eq(ownerLogin))).thenReturn(ownerUid);
        when(passportService.getUserInfo(eq(ownerUid)))
                .thenReturn(new UserInfo(UID, "Tr Alex", "lex@yandex.ru", "lex"));
        when(balanceService.getClient(eq(ownerClientId))).thenReturn(ownerClientInfo);
        when(balanceContactService.getUidsByClient(eq(ownerClientId))).thenReturn(Arrays.asList(ownerUid, otherUid));

        getMbiOpenApiClient().registerPartner(
                UID,
                new PartnerRegistrationRequest()
                        .partnerPlacementType(PartnerPlacementType.DBS)
                        .businessName("business")
                        .partnerNotificationContact(CONTACT)
        );
        verifyInteractions();
    }

    @Test
    void testInvalidPartnerName() {
        when(balanceService.getClientByUid(eq(UID)))
                .thenReturn(new ClientInfo(CLIENT_ID, ClientType.OOO, false, 0));
        when(passportService.getUserInfo(eq(UID)))
                .thenReturn(new UserInfo(UID, "Tr Alex", "lex@yandex.ru", "lex"));
        var exception = assertThrows(
                MbiOpenApiClientResponseException.class,
                () -> getMbiOpenApiClient().registerPartner(
                        UID,
                        new PartnerRegistrationRequest()
                                .partnerPlacementType(PartnerPlacementType.DBS)
                                .businessName("business")
                                .partnerName("@@@@AAA@@@232323")
                                .partnerNotificationContact(CONTACT)
                                .regionId(RegionConstants.MOSCOW)
                )
        );
        assertThat(exception.getHttpErrorCode()).isEqualTo(HttpStatus.SC_BAD_REQUEST);
        assertThat(exception.getApiError().getMessageCode()).isEqualTo(ApiError.MessageCodeEnum.INCORRECT_PARTNER_NAME);
        verifyInteractions();
    }

    @Test
    @DbUnitDataSet(
            before = "PartnerRegistrationControllerTest.testRegistrationExistedContact.before.csv",
            after = "PartnerRegistrationControllerTest.testRegistrationExistedContact.after.csv"
    )
    void testRegistrationExistedContact() {
        when(balanceService.getClientByUid(eq(UID)))
                .thenReturn(new ClientInfo(CLIENT_ID, ClientType.OOO, false, 0));
        when(passportService.getUserInfo(eq(UID)))
                .thenReturn(new UserInfo(UID, "PUP VAS", "pup@yandex.ru", "pup"));
        var response = getMbiOpenApiClient().registerPartner(
                UID,
                new PartnerRegistrationRequest()
                        .partnerPlacementType(PartnerPlacementType.DBS)
                        .businessName("new business")
                        .partnerNotificationContact(CONTACT)
                        .regionId(RegionConstants.MOSCOW)
        );
        assertThat(response).isEqualTo(new PartnerRegistrationResponse()
                .partnerId(1L)
                .contactId(10L)
                .campaignId(1L)
                .businessId(2L)
                .partnerContact(CONTACT)
        );
        verifyInteractions();
    }

    @Test
    @DbUnitDataSet(after = "PartnerRegistrationControllerTest.testNoBalanceClient.after.csv")
    void testNoBalanceClient() {
        when(passportService.getUserInfo(eq(UID)))
                .thenReturn(new UserInfo(UID, "PUP VAS", "lex@yandex.ru", "lex", "89164490000", "lex"));
        var response = getMbiOpenApiClient().registerPartner(
                UID,
                new PartnerRegistrationRequest()
                        .partnerPlacementType(PartnerPlacementType.DBS)
                        .businessName("new business")
                        .partnerNotificationContact(CONTACT)
                        .regionId(RegionConstants.MOSCOW)
        );
        assertThat(response).isEqualTo(new PartnerRegistrationResponse()
                .partnerId(1L)
                .contactId(1L)
                .campaignId(1L)
                .businessId(2L)
                .partnerContact(CONTACT)
        );
        verify(balanceContactService, never()).linkUid(anyLong(), anyLong(), anyLong(), anyLong());
        verify(balanceService, never()).createClient(any(), anyLong(), anyLong());
        verifyInteractions();
    }

    @Test
    @DbUnitDataSet(
            before = "PartnerRegistrationControllerTest.testRegisterOnSubclient.before.csv",
            after = "PartnerRegistrationControllerTest.testRegisterOnSubclient.after.csv"
    )
    void testRegisterOnSubclient() {
        when(balanceService.getClientByUid(eq(UID)))
                .thenReturn(new ClientInfo(CLIENT_ID, ClientType.OOO, false, 12345));
        when(passportService.getUserInfo(eq(UID)))
                .thenReturn(new UserInfo(UID, "Tr Alex", "lex@yandex.ru", "lex"));
        getMbiOpenApiClient().registerPartner(
                UID,
                new PartnerRegistrationRequest()
                        .partnerPlacementType(PartnerPlacementType.DBS)
                        .businessName("new business")
                        .partnerNotificationContact(CONTACT)
                        .regionId(RegionConstants.MOSCOW)
        );
        verifyInteractions();
    }

    @Test
    @DbUnitDataSet(
            before = "PartnerRegistrationControllerTest.testRegisterOnMarketOnlyContact.before.csv",
            after = "PartnerRegistrationControllerTest.testRegisterOnMarketOnlyContact.after.csv"
    )
    void testRegisterOnMarketOnlyContact() {
        when(passportService.getUserInfo(eq(UID)))
                .thenReturn(new UserInfo(UID, "Tr Alex", "lex@yandex.ru", "lex"));
        getMbiOpenApiClient().registerPartner(
                UID,
                new PartnerRegistrationRequest()
                        .partnerPlacementType(PartnerPlacementType.FBS)
                        .businessName("new business")
                        .partnerNotificationContact(CONTACT)
                        .regionId(RegionConstants.MOSCOW)
        );
        verifyInteractions();
    }

    @Test
    @DbUnitDataSet(before = "PartnerRegistrationControllerTest.testSupplierRegistrationNotification.before.csv")
    void testSupplierRegistrationNotification() {
        getMbiOpenApiClient().notifyPartnerRegistration(
                UID,
                1
        );
        verifySentNotificationType(partnerNotificationClient, 2, 155498860L, 1549300852L);
    }

    @Test
    @DbUnitDataSet(before = "PartnerRegistrationControllerTest.testShopRegistrationNotification.before.csv")
    void testShopRegistrationNotification() {
        getMbiOpenApiClient().notifyPartnerRegistration(
                UID,
                1
        );
        verifyNoMoreInteractions(nesuClient);
        verifySentNotificationType(partnerNotificationClient, 1, 95L);
    }

    @Test
    @DbUnitDataSet(
            after = "PartnerRegistrationControllerTest.testRegisterFby.byAgency.after.csv"
    )
    void testRegistrationByAgency() {
        when(balanceService.getClientByUid(eq(AGENCY_USER_ID)))
                .thenReturn(new ClientInfo(AGENCY_USER_ID, ClientType.OOO, true, AGENCY_USER_ID));
        when(passportService.getUserInfo(eq(UID)))
                .thenReturn(new UserInfo(UID, "Tr Alex", "lex@yandex.ru", "lex"));
        PartnerNotificationContact contact = new PartnerNotificationContact()
                .firstName("Vas")
                .lastName("P")
                .email("nat@yandex.ru")
                .phone("89164490001");
        getMbiOpenApiClient().registerPartner(
                AGENCY_USER_ID,
                new PartnerRegistrationRequest()
                        .partnerPlacementType(PartnerPlacementType.FBY)
                        .businessName("New business")
                        .regionId(RegionConstants.MOSCOW)
                        .partnerNotificationContact(contact)
                        .shopOwnerId(UID)
        );
    }

    @Test
    @DbUnitDataSet(
            before = "PartnerRegistrationControllerTest.testRegisterInBalanceExistedClient.before.csv",
            after = "PartnerRegistrationControllerTest.testRegisterInBalanceExistedClient.after.csv"
    )
    void testRegisterInBalanceExistedClient() {
        when(balanceService.getClientByUid(eq(UID)))
                .thenReturn(new ClientInfo(1001, ClientType.OOO, false, 0));
        when(balanceService.getClient(eq(1001L)))
                .thenReturn(new ClientInfo(1001, ClientType.OOO, false, 0));
        when(balanceContactService.getUidsByClient(eq(1001L)))
                .thenReturn(List.of(UID));
        when(balanceService.createClient(any(), eq(UID), anyLong()))
                .thenReturn(1001L);
        getMbiOpenApiClient().registerPartnerInBalance(UID, 1);

        verify(balanceService).createOrUpdateOrderByCampaign(
                any(OrderInfo.class),
                eq(UID)
        );
    }

    @Test
    @DbUnitDataSet(
            before = "PartnerRegistrationControllerTest.testRegisterInBalanceNewClient.before.csv",
            after = "PartnerRegistrationControllerTest.testRegisterInBalanceNewClient.after.csv"
    )
    void testRegisterInBalanceNewClient() {
        when(balanceService.getClientByUid(eq(UID)))
                .thenReturn(null, new ClientInfo(1001, ClientType.OOO, false, 0));
        when(balanceService.createClient(any(), eq(UID), anyLong()))
                .thenReturn(1001L);
        when(balanceService.getClient(eq(1001L)))
                .thenReturn(new ClientInfo(1001, ClientType.OOO, false, 0));
        when(balanceContactService.getUidsByClient(eq(1001L)))
                .thenReturn(Lists.emptyList());
        getMbiOpenApiClient().registerPartnerInBalance(UID, 1);
        verify(balanceService).createOrUpdateOrderByCampaign(
                any(OrderInfo.class),
                eq(UID)
        );
        verify(balanceService).createClient(any(), eq(UID), anyLong());
    }

    @Test
    @DbUnitDataSet(
            before = "PartnerRegistrationControllerTest.testRegisterInBalanceNewClient.before.csv",
            after = "PartnerRegistrationControllerTest.testRegisterInBalanceNewClient.byAgency.after.csv"
    )
    void testRegisterInBalanceByAgency() {
        ArgumentCaptor<FullClientInfo> captor = ArgumentCaptor.forClass(FullClientInfo.class);
        when(balanceService.createClient(captor.capture(), eq(AGENCY_USER_ID), anyLong()))
                .thenReturn(222L);
        when(balanceService.getClient(eq(222L)))
                .thenReturn(new ClientInfo(222L, ClientType.OOO, false, 2L));

        getMbiOpenApiClient().registerPartnerInBalance(AGENCY_USER_ID, 1);

        FullClientInfo value = captor.getValue();
        Assertions.assertThat(value)
                .isNotNull()
                .matches(val -> val.getAgencyId() == 2L);
    }

    @Test
    @DbUnitDataSet(
            before = "application/PartnerRegistrationControllerTest.testCreatePartnerApplication.before.csv",
            after = "application/PartnerRegistrationControllerTest.testCreatePartnerApplication.after.csv"
    )
    void testCreatePrepayApplication() {
        getMbiOpenApiClient().createPartnerApplicationRequest(UID, 1, new PartnerApplicationRequest().contactId(1L));
    }

    @Test
    @DbUnitDataSet(
            before = "application/PartnerRegistrationControllerTest.testCreatePartnerApplication.before.csv",
            after = "application/PartnerRegistrationControllerTest.testCreatePartnerApplication.after.csv"
    )
    void testCreatePrepayApplicationNoContact() {
        getMbiOpenApiClient().createPartnerApplicationRequest(UID, 1, new PartnerApplicationRequest());
    }

    @Test
    @DbUnitDataSet(
            before = "application/PartnerRegistrationControllerTest.testCreatePartnerApplicationToBOwner.before.csv",
            after = "application/PartnerRegistrationControllerTest.testCreatePartnerApplicationToBOwner.after.csv"
    )
    void testCreatePrepayApplicationByBusinessOwnerContact() {
        getMbiOpenApiClient().createPartnerApplicationRequest(UID, 3, new PartnerApplicationRequest());
    }

    @Test
    void testRegisterBusinessEmptyBusinessName() {
        var ex = assertThrows(
                MbiOpenApiClientResponseException.class,
                () -> getMbiOpenApiClient().registerBusiness(UID, new BusinessRegistrationRequest())
        );
        assertThat(ex.getHttpErrorCode()).isEqualTo(HttpStatus.SC_BAD_REQUEST);
        assertThat(ex.getApiError().getMessage()).isEqualTo("businessName must not be null");
    }

    @Test
    @DbUnitDataSet(
            after = "PartnerRegistrationControllerTest.testRegisterBusiness.after.csv"
    )
    void testRegisterBusiness() {
        when(passportService.getUserInfo(eq(UID)))
                .thenReturn(new UserInfo(UID, "Tr Alex", "lex@yandex.ru", "lex"));
        var response = getMbiOpenApiClient().registerBusiness(
                UID,
                new BusinessRegistrationRequest().businessName("super business").marketOnly(true)
        );
        assertThat(response.getBusinessId()).isEqualTo(1L);
        verify(balanceService, never()).getClient(anyLong());
        verify(balanceContactService, never()).linkUid(anyLong(), anyLong(), anyLong(), anyLong());
    }

    @Test
    void testRegisterBusinessLitePassport() {
        var ex = assertThrows(
                MbiOpenApiClientResponseException.class,
                () -> getMbiOpenApiClient().registerBusiness(
                        UID,
                        new BusinessRegistrationRequest()
                                .notificationContact(CONTACT)
                                .businessName("super business")
                )
        );
        assertThat(ex.getHttpErrorCode()).isEqualTo(HttpStatus.SC_BAD_REQUEST);
        assertThat(ex.getApiError().getMessage()).isEqualTo("Lite passport registration");
        assertThat(ex.getApiError().getMessageCode()).isEqualTo(ApiError.MessageCodeEnum.LITE_PASSPORT_REGISTRATION);
    }

    @Test
    void testRegisterBusinessIncorrectName() {
        when(passportService.getUserInfo(eq(UID)))
                .thenReturn(new UserInfo(UID, "Tr Alex", "lex@yandex.ru", "lex"));
        var ex = assertThrows(
                MbiOpenApiClientResponseException.class,
                () -> getMbiOpenApiClient().registerBusiness(
                        UID,
                        new BusinessRegistrationRequest()
                                .notificationContact(CONTACT)
                                .businessName("Аякс•Спорт")
                )
        );
        assertThat(ex.getHttpErrorCode()).isEqualTo(HttpStatus.SC_BAD_REQUEST);
        assertThat(ex.getApiError().getMessage()).isEqualTo("Incorrect business name");
        assertThat(ex.getApiError().getMessageCode()).isEqualTo(ApiError.MessageCodeEnum.INCORRECT_BUSINESS_NAME);
    }

    @Test
    void testNotRegisteredAgencySublclient() {
        when(balanceService.getClientByUid(eq(UID)))
                .thenReturn(new ClientInfo(180L, ClientType.OOO, false, 100500L));
        var exception = assertThrows(
                MbiOpenApiClientResponseException.class,
                () -> getMbiOpenApiClient().registerBusiness(
                        UID,
                        new BusinessRegistrationRequest()
                                .businessName("Business")
                                .notificationContact(CONTACT)
                )
        );
        assertThat(exception.getHttpErrorCode()).isEqualTo(HttpStatus.SC_BAD_REQUEST);
        assertThat(exception.getApiError().getMessageCode())
                .isEqualTo(ApiError.MessageCodeEnum.AGENCY_NOT_REGISTERED);
        verifyInteractions();
    }

    @Test
    void testRegisterBusinessInvalidRegion() {
        var ex = assertThrows(
                MbiOpenApiClientResponseException.class,
                () -> getMbiOpenApiClient().registerBusiness(
                        UID,
                        new BusinessRegistrationRequest().businessName("super business").localRegionId(131692L)
                ));
        assertThat(ex.getHttpErrorCode()).isEqualTo(HttpStatus.SC_BAD_REQUEST);
        assertThat(ex.getApiError().getMessageCode()).isEqualTo(ApiError.MessageCodeEnum.INCORRECT_REGION_ID);
    }

    @Test
    void testRegisterBusinessInvalidDomain() {
        var ex = assertThrows(
                MbiOpenApiClientResponseException.class,
                () -> getMbiOpenApiClient().registerBusiness(
                        UID,
                        new BusinessRegistrationRequest()
                                .businessName("super business")
                                .localRegionId(213L)
                                .domain("tralivali")
                ));
        assertThat(ex.getHttpErrorCode()).isEqualTo(HttpStatus.SC_BAD_REQUEST);
        assertThat(ex.getApiError().getMessageCode()).isEqualTo(ApiError.MessageCodeEnum.INCORRECT_DOMAIN);
    }

    @Test
    void testRegisterForeignShopEmptyBusinessId() {
        var ex = assertThrows(
                MbiOpenApiClientResponseException.class,
                () -> getMbiOpenApiClient().registerPartner(
                        UID,
                        new PartnerRegistrationRequest().partnerPlacementType(PartnerPlacementType.FOREIGN_SHOP)
                )
        );
        assertThat(ex.getHttpErrorCode()).isEqualTo(HttpStatus.SC_BAD_REQUEST);
        assertThat(ex.getApiError().getMessageCode()).isEqualTo(ApiError.MessageCodeEnum.INCORRECT_BUSINESS_ID);
    }

    @Test
    @DbUnitDataSet(
            before = "PartnerRegistrationControllerTest.testRegisterForeignShop.before.csv",
            after = "PartnerRegistrationControllerTest.testRegisterForeignShop.after.csv"
    )
    void testRegisterForeignShop() {
        getMbiOpenApiClient().registerPartner(
                UID,
                new PartnerRegistrationRequest()
                        .partnerPlacementType(PartnerPlacementType.FOREIGN_SHOP)
                        .partnerName("foreignShop")
                        .businessId(10L)
        );
    }

    @Test
    @DbUnitDataSet(
            before = "manager/PartnerRegistrationControllerTest.testSetUpManagerForDbs.before.csv",
            after = "manager/PartnerRegistrationControllerTest.testSetUpManagerForDbs.after.csv"
    )
    void testSetUpManagerForDbs() {
        var resp = getMbiOpenApiClient().setUpManagerForDbs(
                UID,
                new PartnerManagerSetUpRequest()
                        .partnerId(1L)
                        .creatorUid(UID)
        );

        assertThat(resp.getManagerId()).isEqualTo(MANAGER_ID);
    }

    @Test
    @DbUnitDataSet(
            before = "moderation/PartnerRegistrationControllerTest.testSetUpModerationForDbsWithPi.before.csv",
            after = "moderation/PartnerRegistrationControllerTest.testSetUpModerationForDbsWithPi.after.csv"
    )
    void testSetUpModerationForDbsWithPi() {
        getMbiOpenApiClient().setUpModeration(UID, new PartnerModerationSetUpRequest()
                .partnerId(1L));
    }

    @Test
    @DbUnitDataSet(
            before = "moderation/PartnerRegistrationControllerTest.testSetUpModerationForDbsWithApi.before.csv",
            after = "moderation/PartnerRegistrationControllerTest.testSetUpModerationForDbsWithApi.after.csv"
    )
    void testSetUpModerationForDbsWithApi() {
        getMbiOpenApiClient().setUpModeration(UID, new PartnerModerationSetUpRequest()
                .partnerId(1L));
    }

    @Test
    @DbUnitDataSet(
            before = "moderation/PartnerRegistrationControllerTest.testSetUpModerationForWrongDbs.before.csv",
            after = "moderation/PartnerRegistrationControllerTest.testSetUpModerationForWrongDbs.after.csv"
    )
    void testSetUpModerationForWrongDbs() {
        var exception = assertThrows(
                MbiOpenApiClientResponseException.class,
                () -> getMbiOpenApiClient().setUpModeration(
                        UID,
                        new PartnerModerationSetUpRequest()
                                .partnerId(1L)
                )
        );
        assertThat(exception.getHttpErrorCode()).isEqualTo(HttpStatus.SC_BAD_REQUEST);
    }

    @Test
    @DbUnitDataSet(
            before = "manager/PartnerRegistrationControllerTest.testSetUpManagerForDbsWithCreatorUid.before.csv",
            after = "manager/PartnerRegistrationControllerTest.testSetUpManagerForDbsWithCreatorUid.after.csv"
    )
    void testSetUpManagerForDbsWithCreatorUid() {
        var resp = getMbiOpenApiClient().setUpManagerForDbs(
                UID,
                new PartnerManagerSetUpRequest()
                        .partnerId(1L)
                        .creatorUid(MANAGER_UID)
        );

        assertThat(resp.getManagerId()).isEqualTo(MANAGER_UID);
    }

    @Test
    @DbUnitDataSet(
            before = "manager/PartnerRegistrationControllerTest.testSetUpManagerForDbsUnderAgency.before.csv",
            after = "manager/PartnerRegistrationControllerTest.testSetUpManagerForDbsUnderAgency.after.csv"
    )
    void testSetUpManagerForDbsUnderAgency() {
        var resp = getMbiOpenApiClient().setUpManagerForDbs(
                UID,
                new PartnerManagerSetUpRequest()
                        .partnerId(1L)
                        .creatorUid(UID)
        );

        assertThat(resp.getManagerId()).isEqualTo(DBS_AGENCY_MANAGER_ID);
    }

    @Test
    @DbUnitDataSet(
            before = "manager/PartnerRegistrationControllerTest.testSetUpManagerForWrongDbs.before.csv",
            after = "manager/PartnerRegistrationControllerTest.testSetUpManagerForWrongDbs.after.csv"
    )
    void testSetUpManagerForWrongDbs() {
        var exception = assertThrows(
                MbiOpenApiClientResponseException.class,
                () -> getMbiOpenApiClient().setUpManagerForDbs(
                        UID,
                        new PartnerManagerSetUpRequest()
                                .partnerId(1L)
                                .creatorUid(MANAGER_UID)
                )
        );
        assertThat(exception.getHttpErrorCode()).isEqualTo(HttpStatus.SC_BAD_REQUEST);
    }

    @Test
    @DbUnitDataSet(
            before = "application/PartnerRegistrationControllerTest.testReadyPartnerApplication.before.csv",
            after = "application/PartnerRegistrationControllerTest.testReadyPartnerApplication.after.csv"
    )
    void testUpdatePartnerApplication() {
        getMbiOpenApiClient().updatePartnerApplicationRequest(
                UID,
                1L,
                new PartnerApplicationRequest()
                        .contactInfo(
                                new ContactInfo()
                                        .firstName("Alex")
                                        .lastName("Tr")
                                        .phone("+79164490000")
                                        .email("lex@yandex.ru")
                                        .shopPhone("+79999999999")
                                        .shopAddress("Moscow")
                        )
                        .signatoryInfo(
                                new SignatoryInfo()
                                        .firstName("Boris")
                                        .lastName("Borisov")
                                        .docType(SignatoryDocType.ORDER)
                                        .position("Direktor")
                        )
                        .vatInfo(
                                new VatInfo()
                                        .vatRate(VatRateDTO.VAT_20)
                                        .vatSource(VatSourceDTO.WEB)
                                        .taxSystem(TaxSystemDTO.OSN)
                                        .deliveryVatRate(VatRateDTO.VAT_18)
                        )
                        .organizationInfo(
                                new OrganizationInfo()
                                        .name("Romashka")
                                        .type(OrganizationType.OOO)
                                        .ogrn("12345678900000")
                                        .inn("123456789000")
                                        .kpp("123456789")
                                        .postcode("129085")
                                        .factAddress("Moscow")
                                        .factAddressRegionId(213)
                                        .jurAddress("Jur Moscow")
                                        .accountNumber("12345678901234567890")
                                        .workSchedule("Пн-Вс 00:00-23:59")
                        )
        );
    }

    @Test
    @DbUnitDataSet(before = "application/PartnerRegistrationControllerTest.testIncorrectPartnerApplication.before.csv")
    void testIncorrectApplicationStatus() {
        var request = (Executable) () -> getMbiOpenApiClient().updatePartnerApplicationRequest(
                UID,
                1L,
                new PartnerApplicationRequest()
                        .contactInfo(
                                new ContactInfo()
                                        .firstName("Alex")
                                        .lastName("Tr")
                                        .phone("+79164490000")
                                        .email("lex@yandex.ru")
                                        .shopPhone("+79999999999")
                                        .shopAddress("Moscow")
                        )
                        .signatoryInfo(
                                new SignatoryInfo()
                                        .firstName("Boris")
                                        .lastName("Borisov")
                                        .docType(SignatoryDocType.ORDER)
                                        .position("Direktor")
                        )
                        .vatInfo(
                                new VatInfo()
                                        .vatRate(VatRateDTO.VAT_20)
                                        .vatSource(VatSourceDTO.WEB)
                                        .taxSystem(TaxSystemDTO.OSN)
                        )
                        .organizationInfo(
                                new OrganizationInfo()
                                        .name("Romashka")
                                        .type(OrganizationType.OOO)
                                        .ogrn("12345678900000")
                                        .inn("123456789000")
                                        .kpp("123456789")
                                        .postcode("129085")
                                        .factAddress("Moscow")
                                        .factAddressRegionId(213)
                                        .jurAddress("Jur Moscow")
                                        .accountNumber("12345678901234567890")
                                        .workSchedule("Пн-Вс 00:00-23:59")
                        )
        );
        var exception = assertThrows(
                MbiOpenApiClientResponseException.class,
                request
        );
        assertThat(exception.getApiError().getMessageCode())
                .isEqualTo(ApiError.MessageCodeEnum.INCORRECT_PARTNER_APPLICATION_STATUS);
    }

    @Test
    void testInvalidRegion() {
        when(balanceService.getClientByUid(eq(UID)))
                .thenReturn(new ClientInfo(CLIENT_ID, ClientType.OOO, false, 0));
        when(passportService.getUserInfo(eq(UID)))
                .thenReturn(new UserInfo(UID, "Tr Alex", "lex@yandex.ru", "lex"));
        var exception = assertThrows(
                MbiOpenApiClientResponseException.class,
                () -> getMbiOpenApiClient().registerPartner(
                        UID,
                        new PartnerRegistrationRequest()
                                .regionId(216L)
                                .partnerPlacementType(PartnerPlacementType.FBS)
                                .businessName("Business")
                                .partnerNotificationContact(CONTACT)
                )
        );
        assertThat(exception.getHttpErrorCode()).isEqualTo(HttpStatus.SC_BAD_REQUEST);
        assertThat(exception.getApiError().getMessage()).isEqualTo("Incorrect regionId");
        assertThat(exception.getApiError().getMessageCode()).isEqualTo(ApiError.MessageCodeEnum.INCORRECT_REGION_ID);
        verifyInteractions();
    }

    @Test
    void testOkBusinessName() {
        var response = getMbiOpenApiClient().checkBusinessName("СуперБизнес");
        assertThat(response.getCheckResult()).isEqualTo(BusinessNameCheckResponse.CheckResultEnum.OK);
    }

    @Test
    void testNotValidName() {
        var response = getMbiOpenApiClient().checkBusinessName("***");
        assertThat(response.getCheckResult()).isEqualTo(BusinessNameCheckResponse.CheckResultEnum.NOT_VALID);
    }

    @Test
    @DbUnitDataSet(before = "PartnerRegistrationControllerTest.testBusinessUniqueName.before.csv")
    void testNotUniqueName() {
        var response = getMbiOpenApiClient().checkBusinessName("super business");
        assertThat(response.getCheckResult()).isEqualTo(BusinessNameCheckResponse.CheckResultEnum.NOT_UNIQUE);
    }

    @Test
    @DbUnitDataSet(before = "PartnerRegistrationControllerTest.testDifferentTypePartners.before.csv")
    void testDifferentBusinessTypePartners() {
        var ex = assertThrows(
                MbiOpenApiClientResponseException.class,
                () -> getMbiOpenApiClient().registerBusiness(
                        UID,
                        new BusinessRegistrationRequest().businessName("super business").marketOnly(false)
                )
        );

        assertThat(ex.getHttpErrorCode()).isEqualTo(HttpStatus.SC_BAD_REQUEST);
        assertThat(ex.getApiError().getMessage()).isEqualTo("Contact 1001 has different business partners types");
        assertThat(ex.getApiError().getMessageCode())
                .isEqualTo(ApiError.MessageCodeEnum.DIFFERENT_BUSINESS_TYPE_PARTNERS_ON_CONTACT);
    }
}
