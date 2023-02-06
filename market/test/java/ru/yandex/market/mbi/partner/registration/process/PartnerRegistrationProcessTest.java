package ru.yandex.market.mbi.partner.registration.process;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.balance.model.ClientType;
import ru.yandex.market.common.retrofit.CommonRetrofitHttpExecutionException;
import ru.yandex.market.common.retrofit.ExecuteCall;
import ru.yandex.market.common.retrofit.RetryStrategy;
import ru.yandex.market.logbroker.LogbrokerEventPublisher;
import ru.yandex.market.mbi.bpmn.client.api.MbibpmnApi;
import ru.yandex.market.mbi.bpmn.client.model.ProcessInstanceRequest;
import ru.yandex.market.mbi.bpmn.client.model.ProcessStartInstance;
import ru.yandex.market.mbi.bpmn.client.model.ProcessStartResponse;
import ru.yandex.market.mbi.bpmn.client.model.ProcessType;
import ru.yandex.market.mbi.open.api.client.MbiOpenApiClient;
import ru.yandex.market.mbi.open.api.client.model.AgencyCommissionRequest;
import ru.yandex.market.mbi.open.api.client.model.ApiError;
import ru.yandex.market.mbi.open.api.client.model.PartnerApplicationRequest;
import ru.yandex.market.mbi.open.api.client.model.PartnerModerationSetUpRequest;
import ru.yandex.market.mbi.open.api.client.model.PartnerNotificationContact;
import ru.yandex.market.mbi.open.api.client.model.PartnerPlacementType;
import ru.yandex.market.mbi.open.api.client.model.PartnerRegistrationRequest;
import ru.yandex.market.mbi.open.api.client.model.PartnerRegistrationResponse;
import ru.yandex.market.mbi.open.api.exception.MbiOpenApiClientResponseException;
import ru.yandex.market.mbi.partner.registration.AbstractFunctionalTest;
import ru.yandex.market.mbi.partner.registration.balance.BalanceService;
import ru.yandex.market.mbi.partner.registration.environment.EnvironmentService;
import ru.yandex.market.mbi.partner.registration.logbroker.MbiPartnerRegistration;
import ru.yandex.market.mbi.partner.registration.model.BalanceClientInfo;
import ru.yandex.market.mbi.partner.registration.model.PartnerRegistrationInfo;
import ru.yandex.market.mbi.partner.registration.services.PartnerRegistrationService;
import ru.yandex.market.mbi.partner.registration.tasks.CpaIsPartnerInterfaceListenerTask;
import ru.yandex.market.mbi.partner.registration.util.CamundaTestUtil;
import ru.yandex.market.mbi.partner.registration.util.IsRegistrationByAgencyAllowed;
import ru.yandex.market.mbi.partner.registration.util.ModelConversions;
import ru.yandex.mj.generated.client.integration_npd.api.ApplicationApiClient;
import ru.yandex.mj.generated.client.integration_npd.model.PartnerAppNpdStatus;
import ru.yandex.mj.generated.client.integration_npd.model.PartnerApplicationResponse;
import ru.yandex.mj.generated.client.mbi_partner_registration.model.SuggestedPartnerType;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.mbi.partner.registration.util.ModelConversions.fromRegistrationClient;

class PartnerRegistrationProcessTest extends AbstractFunctionalTest {

    private static final long FBS_UID = 1001;
    private static final long AGENCY_ID = 12; //Идентификатор агентства, которое регистрирует магаз
    private static final long USER_ID = 1L; //Идентификатор пользователя, для которого регистрируют магаз
    private static final long FBS_BUSINESS_ID = 10;
    private static final long FBS_PARTNER_ID = 1;
    private static final long FBS_CONTACT_ID = 101;
    private static final long FBS_CAMPAIGN_ID = 1;

    private static final String FBS_NAME = "FBS name";

    private static final long DBS_UID = 2001;         //euid пользователя, от имени которого регистрируется магазин
    private static final long DBS_CREATOR_UID = 2002; //id менеджера, создающего DBS
    private static final long DBS_BUSINESS_ID = 20;
    private static final long DBS_PARTNER_ID = 2;
    private static final long DBS_CONTACT_ID = 201;
    private static final long DBS_CAMPAIGN_ID = 2;

    private static final String DBS_NAME = "DBS name";

    @Autowired
    private MbiOpenApiClient client;

    @Autowired
    private LogbrokerEventPublisher<PartnerRegistrationInfo> partnerRegistrationInfoPublisher;

    @Autowired
    private ApplicationApiClient applicationApiClient;

    @Autowired
    private EnvironmentService environmentService;

    @Autowired
    private MbibpmnApi mbibpmnApi;

    @Autowired
    private IsRegistrationByAgencyAllowed isRegistrationByAgencyAllowed;

    @Autowired
    private BalanceService balanceService;

    @BeforeEach
    void init() {
        environmentService.setValue(CpaIsPartnerInterfaceListenerTask.IS_PARTNER_INTERFACE_PROCESS_ENABLED, "true");
        clearInvocations(client);
        clearInvocations(applicationApiClient);
        clearInvocations(partnerRegistrationInfoPublisher);
        clearInvocations(mbiBpmnRetrofitService);

        ExecuteCall<Object, RetryStrategy> processMock = Mockito.mock(ExecuteCall.class);
        when(processMock.schedule())
                .thenReturn(CompletableFuture.completedFuture(
                        new ProcessStartResponse().records(List.of(new ProcessStartInstance()))
                ));
        when(mbiBpmnRetrofitService.caller(any())).thenReturn(processMock);
        when(isRegistrationByAgencyAllowed.get()).thenReturn(true);
        when(balanceService.getClientByUid(FBS_UID)).thenReturn(
            Optional.of(BalanceClientInfo.builder()
                    .withId(FBS_UID)
                    .withType(ClientType.OAO)
                    .withIsAgency(false)
                .build())
        );
        when(balanceService.getClientByUid(AGENCY_ID)).thenReturn(
            Optional.of(BalanceClientInfo.builder()
                .withId(AGENCY_ID)
                .withType(ClientType.OAO)
                .withIsAgency(true)
                .build())
        );
    }

    void fbsBefore(long uid) {
        var contact = new PartnerNotificationContact()
                .firstName("firstName")
                .lastName("lastName")
                .email("mail@ya.ru")
                .phone("89164490000");
        var fbsRegistrationRequest = new PartnerRegistrationRequest()
                .partnerPlacementType(PartnerPlacementType.FBS)
                .partnerNotificationContact(contact)
                .businessId(FBS_BUSINESS_ID)
                .partnerName(FBS_NAME)
                .shopOwnerId(FBS_UID)
                .isSelfEmployed(true)
                .isB2BSeller(false);
        when(client.registerPartner(eq(uid), eq(fbsRegistrationRequest)))
                .thenReturn(new PartnerRegistrationResponse()
                        .partnerId(FBS_PARTNER_ID)
                        .businessId(FBS_BUSINESS_ID)
                        .campaignId(FBS_CAMPAIGN_ID)
                        .contactId(FBS_CONTACT_ID)
                        .partnerContact(contact)
                );

        ExecuteCall<PartnerApplicationResponse, RetryStrategy> callMock = Mockito.mock(ExecuteCall.class);
        when(callMock.schedule())
                .thenReturn(
                        CompletableFuture.completedFuture(
                                new PartnerApplicationResponse()
                                        .partnerId(FBS_PARTNER_ID)
                                        .status(PartnerAppNpdStatus.PENDING)
                        )
                );
        when(applicationApiClient.createApplication(
                eq(FBS_PARTNER_ID),
                eq(uid),
                eq(new ru.yandex.mj.generated.client.integration_npd.model.PartnerApplicationRequest()
                        .phone(contact.getPhone()))
        )).thenReturn(callMock);
    }

    @Test
    void testSuccessFbs() throws InterruptedException {
        fbsBefore(FBS_UID);
        var request =
                new ru.yandex.mj.generated.client.mbi_partner_registration.model
                        .PartnerRegistrationRequest()
                        .partnerPlacementType(
                                ru.yandex.mj.generated.client.mbi_partner_registration.model
                                        .PartnerPlacementType.FBS
                        ).partnerNotificationContact(
                                new ru.yandex.mj.generated.client.mbi_partner_registration.model
                                        .PartnerNotificationContact()
                                        .firstName("firstName")
                                        .lastName("lastName")
                                        .email("mail@ya.ru")
                                        .phone("89164490000")
                        )
                        .businessId(FBS_BUSINESS_ID)
                        .partnerName(FBS_NAME)
                        .isSelfEmployed(Boolean.TRUE);
        partnerRegistrationApiClient.registerPartner(
                FBS_UID,
                request
        ).schedule().join();

        var process = historyService
                .createHistoricProcessInstanceQuery()
                .processInstanceBusinessKey(
                        PartnerRegistrationService.createBusinessKey(FBS_UID, ModelConversions.from(request))
                ).singleResult();
        assertTrue(CamundaTestUtil.waitUntilNoActiveJobs(
                processEngine,
                process.getId()
        ));

        CamundaTestUtil.checkIncidents(processEngine, process.getId());

        verify(client).registerPartner(eq(FBS_UID), eq(fromRegistrationClient(request, FBS_UID)));
        verify(client).registerPartnerInBalance(eq(FBS_UID), eq(FBS_PARTNER_ID));
        verify(client).createPartnerApplicationRequest(
                eq(FBS_UID),
                eq(FBS_PARTNER_ID),
                eq(new PartnerApplicationRequest().contactId(FBS_CONTACT_ID))
        );

        //Для FBS не устанавливаем менеджера и тип модерации
        verify(client, never()).setUpManagerForDbs(anyLong(),
                any());
        verify(client, never()).setUpModeration(anyLong(), any());
        verify(client).notifyPushApiParams(eq(FBS_UID), eq(FBS_PARTNER_ID));
        verify(client).notifyPartnerChange(eq(FBS_UID), eq(FBS_PARTNER_ID));
        verify(mbibpmnApi).processPost(eq(new ProcessInstanceRequest()
                .processType(ProcessType.CHANGE_ORDER_PROCESSING_METHOD)
                .params(Map.of(
                        "uid", FBS_UID,
                        "partnerId", FBS_PARTNER_ID,
                        "isPartnerInterface", true
                ))
        ));
        verify(client).notifyCpaPartner(eq(FBS_UID), eq(FBS_PARTNER_ID));
        verify(client).requestPartnerFeedDefault(eq(FBS_UID), eq(FBS_PARTNER_ID));
        verify(client).notifyPartnerRegistration(eq(FBS_UID), eq(FBS_PARTNER_ID));
        verify(partnerRegistrationInfoPublisher)
                .publishEvent(argThat(event -> event.getPayload().getPartnerId() == FBS_PARTNER_ID));
        verify(client).registerOrCreatePartnerNesu(eq(FBS_UID), eq(FBS_PARTNER_ID), eq(FBS_NAME),
                eq(null), eq(true));
        verify(applicationApiClient).createApplication(
                eq(FBS_PARTNER_ID),
                eq(FBS_UID),
                eq(new ru.yandex.mj.generated.client.integration_npd.model.PartnerApplicationRequest()
                        .phone("89164490000")
                )
        );
    }

    void dbsBefore() {
        var contact = new PartnerNotificationContact()
                .firstName("firstName")
                .lastName("lastName")
                .email("mail@ya.ru")
                .phone("89164490000");
        var dbsRegistrationRequest = new PartnerRegistrationRequest()
                .partnerPlacementType(PartnerPlacementType.DBS)
                .partnerNotificationContact(contact)
                .businessId(DBS_BUSINESS_ID)
                .partnerName(DBS_NAME)
                .shopOwnerId(DBS_CREATOR_UID)
                .isSelfEmployed(Boolean.FALSE)
                .isB2BSeller(Boolean.TRUE);
        when(client.registerPartner(eq(DBS_CREATOR_UID), eq(dbsRegistrationRequest)))
                .thenReturn(new PartnerRegistrationResponse()
                        .partnerId(DBS_PARTNER_ID)
                        .businessId(DBS_BUSINESS_ID)
                        .campaignId(DBS_CAMPAIGN_ID)
                        .contactId(DBS_CONTACT_ID)
                        .partnerContact(contact)
                );
    }

    @Test
    void testSuccessDbs() throws InterruptedException {
        dbsBefore();
        var request =
                new ru.yandex.mj.generated.client.mbi_partner_registration.model
                        .PartnerRegistrationRequest()
                        .partnerPlacementType(
                                ru.yandex.mj.generated.client.mbi_partner_registration.model
                                        .PartnerPlacementType.DBS
                        ).partnerNotificationContact(
                                new ru.yandex.mj.generated.client.mbi_partner_registration.model
                                        .PartnerNotificationContact()
                                        .firstName("firstName")
                                        .lastName("lastName")
                                        .email("mail@ya.ru")
                                        .phone("89164490000")
                        )
                        .businessId(DBS_BUSINESS_ID)
                        .partnerName(DBS_NAME)
                        .isSelfEmployed(Boolean.FALSE)
                        .isB2BSeller(Boolean.TRUE);
        partnerRegistrationApiClient.registerPartner(
                DBS_CREATOR_UID,
                request
        ).schedule().join();

        var process = historyService
                .createHistoricProcessInstanceQuery()
                .processInstanceBusinessKey(
                        PartnerRegistrationService.createBusinessKey(DBS_CREATOR_UID, ModelConversions.from(request))
                ).singleResult();
        assertTrue(CamundaTestUtil.waitUntilNoActiveJobs(
                processEngine,
                process.getId()
        ));

        CamundaTestUtil.checkIncidents(processEngine, process.getId());

        verify(client).registerPartner(eq(DBS_CREATOR_UID), eq(fromRegistrationClient(request, DBS_CREATOR_UID)));
        verify(client).registerPartnerInBalance(eq(DBS_CREATOR_UID), eq(DBS_PARTNER_ID));
        verify(client).createPartnerApplicationRequest(
                eq(DBS_CREATOR_UID),
                eq(DBS_PARTNER_ID),
                eq(new PartnerApplicationRequest().contactId(DBS_CONTACT_ID))
        );

        //Для DBS устанавливаем менеджера и тип модерации
        verify(client).setUpModeration(eq(DBS_CREATOR_UID), eq(new PartnerModerationSetUpRequest()
                .partnerId(DBS_PARTNER_ID)));
        verify(client).notifyPushApiParams(eq(DBS_CREATOR_UID), eq(DBS_PARTNER_ID));
        verify(client).notifyPartnerChange(eq(DBS_CREATOR_UID), eq(DBS_PARTNER_ID));
        verify(mbibpmnApi).processPost(eq(new ProcessInstanceRequest()
                .processType(ProcessType.CHANGE_ORDER_PROCESSING_METHOD)
                .params(Map.of(
                        "uid", DBS_CREATOR_UID,
                        "partnerId", DBS_PARTNER_ID,
                        "isPartnerInterface", true
                ))
        ));
        verify(client).notifyCpaPartner(eq(DBS_CREATOR_UID), eq(DBS_PARTNER_ID));
        verify(client).requestPartnerFeedDefault(eq(DBS_CREATOR_UID), eq(DBS_PARTNER_ID));
        verify(client).notifyPartnerRegistration(eq(DBS_CREATOR_UID), eq(DBS_PARTNER_ID));
        verify(partnerRegistrationInfoPublisher)
                .publishEvent(argThat(event -> event.getPayload().getPartnerId() == DBS_PARTNER_ID));
        verify(client).registerOrCreatePartnerNesu(eq(DBS_CREATOR_UID), eq(DBS_PARTNER_ID), eq(DBS_NAME),
                eq(null), eq(true));
    }

    @Test
    void testFailProcess() {
        var registrationRequest = new PartnerRegistrationRequest()
                .partnerPlacementType(PartnerPlacementType.FBY)
                .shopOwnerId(FBS_UID)
                .businessId(FBS_BUSINESS_ID)
                .isSelfEmployed(false)
                .isB2BSeller(false);
        when(client.registerPartner(eq(FBS_UID), eq(registrationRequest)))
                .thenThrow(
                        new MbiOpenApiClientResponseException(
                                "Contact not found",
                                HttpStatus.SC_BAD_REQUEST,
                                new ApiError()
                                        .code(HttpStatus.SC_BAD_REQUEST)
                                        .message("Contact not found")
                                        .messageCode(ApiError.MessageCodeEnum.INCORRECT_UID)
                        )
                );

        var request =
                new ru.yandex.mj.generated.client.mbi_partner_registration.model
                        .PartnerRegistrationRequest()
                        .partnerPlacementType(
                                ru.yandex.mj.generated.client.mbi_partner_registration.model
                                        .PartnerPlacementType.FBY
                        )
                        .businessId(FBS_BUSINESS_ID);
        Exception e = Assertions.assertThrows(
                CompletionException.class,
                () -> partnerRegistrationApiClient.registerPartner(
                        FBS_UID,
                        request
                ).schedule().join()
        );

        CommonRetrofitHttpExecutionException cause = (CommonRetrofitHttpExecutionException) e.getCause();
        Assertions.assertEquals(HttpStatus.SC_BAD_REQUEST, cause.getHttpCode());
    }

    @Test
    void testSuccessForeignShopRegistration() throws Exception {
        var registrationRequest = new PartnerRegistrationRequest()
                .partnerPlacementType(PartnerPlacementType.FOREIGN_SHOP)
                .businessId(FBS_BUSINESS_ID)
                .shopOwnerId(FBS_UID)
                .isB2BSeller(false)
                .isSelfEmployed(false);
        when(client.registerPartner(eq(FBS_UID), eq(registrationRequest)))
                .thenReturn(new PartnerRegistrationResponse()
                        .partnerId(FBS_PARTNER_ID)
                        .businessId(FBS_BUSINESS_ID)
                        .campaignId(FBS_CAMPAIGN_ID)
                );

        var request =
                new ru.yandex.mj.generated.client.mbi_partner_registration.model
                        .PartnerRegistrationRequest()
                        .partnerPlacementType(
                                ru.yandex.mj.generated.client.mbi_partner_registration.model
                                        .PartnerPlacementType.FOREIGN_SHOP
                        )
                        .businessId(FBS_BUSINESS_ID);
        partnerRegistrationApiClient.registerPartner(
                FBS_UID,
                request
        ).schedule().join();

        var process = historyService
                .createHistoricProcessInstanceQuery()
                .processInstanceBusinessKey(
                        PartnerRegistrationService.createBusinessKey(FBS_UID, ModelConversions.from(request))
                ).singleResult();
        assertTrue(CamundaTestUtil.waitUntilNoActiveJobs(
                processEngine,
                process.getId()
        ));

        CamundaTestUtil.checkIncidents(processEngine, process.getId());

        verify(client).registerPartner(eq(FBS_UID), eq(fromRegistrationClient(request, FBS_UID)));
    }

    @Test
    void testSuccessFbsByAgency() throws Exception {
        ArgumentCaptor<AgencyCommissionRequest> captor = ArgumentCaptor.forClass(AgencyCommissionRequest.class);
        ArgumentCaptor<PartnerRegistrationInfo> lbMesCaptor = ArgumentCaptor.forClass(PartnerRegistrationInfo.class);

        fbsBefore(AGENCY_ID);
        var request =
            new ru.yandex.mj.generated.client.mbi_partner_registration.model
                .PartnerRegistrationRequest()
                .partnerPlacementType(
                    ru.yandex.mj.generated.client.mbi_partner_registration.model
                        .PartnerPlacementType.FBS
                ).partnerNotificationContact(
                    new ru.yandex.mj.generated.client.mbi_partner_registration.model
                        .PartnerNotificationContact()
                        .firstName("firstName")
                        .lastName("lastName")
                        .email("mail@ya.ru")
                        .phone("89164490000")
                )
                .businessId(FBS_BUSINESS_ID)
                .partnerName(FBS_NAME)
                .isSelfEmployed(Boolean.TRUE)
                .shopOwnerId(FBS_UID)
                .suggestedPartnerType(SuggestedPartnerType.NEW_OGRN);
        partnerRegistrationApiClient.registerPartner(
            AGENCY_ID,
            request
        ).schedule().join();

        var process = historyService
            .createHistoricProcessInstanceQuery()
            .processInstanceBusinessKey(
                PartnerRegistrationService.createBusinessKey(AGENCY_ID, ModelConversions.from(request))
            ).singleResult();
        assertTrue(CamundaTestUtil.waitUntilNoActiveJobs(
            processEngine,
            process.getId()
        ));

        CamundaTestUtil.checkIncidents(processEngine, process.getId());

        verify(client).registerPartner(eq(AGENCY_ID), eq(fromRegistrationClient(request, AGENCY_ID)));
        verify(client).setAgencyCommission(
            eq(AGENCY_ID),
            captor.capture(),
            eq(AGENCY_ID)
        );
        verify(partnerRegistrationInfoPublisher).publishEvent(lbMesCaptor.capture());
        org.assertj.core.api.Assertions.assertThat(captor.getValue())
            .isNotNull()
            .matches(val -> val.getOnboardingRewardType() == AgencyCommissionRequest.OnboardingRewardTypeEnum.FULL);
        org.assertj.core.api.Assertions.assertThat(lbMesCaptor.getValue())
            .isNotNull()
            .extracting(val -> val.getPayload().getAgencyId(),
                val -> val.getPayload().getCreatorUid(),
                val -> val.getPayload().getCreatorType())
            .containsExactly(AGENCY_ID, AGENCY_ID, MbiPartnerRegistration.UserType.AGENCY);
    }
}
