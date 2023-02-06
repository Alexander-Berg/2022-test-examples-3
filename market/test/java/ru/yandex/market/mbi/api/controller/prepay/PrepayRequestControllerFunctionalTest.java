package ru.yandex.market.mbi.api.controller.prepay;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.OptionalLong;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.unitils.reflectionassert.ReflectionAssert;
import org.unitils.reflectionassert.ReflectionComparatorMode;

import ru.yandex.market.api.cpa.checkout.AsyncCheckouterService;
import ru.yandex.market.api.cpa.checkout.PersistentAsyncCheckouterService;
import ru.yandex.market.api.cpa.yam.dto.AssessorRequestForm;
import ru.yandex.market.api.cpa.yam.dto.ContactInfoDTO;
import ru.yandex.market.api.cpa.yam.dto.OrganizationInfoDTO;
import ru.yandex.market.api.cpa.yam.dto.PrepayRequestDTO;
import ru.yandex.market.api.cpa.yam.dto.PrepayRequestDocumentDTO;
import ru.yandex.market.api.cpa.yam.dto.PrepayTypeDTO;
import ru.yandex.market.api.cpa.yam.dto.RequestStatusForm;
import ru.yandex.market.api.cpa.yam.dto.RequestsInfoDTO;
import ru.yandex.market.api.cpa.yam.dto.SignatoryInfoDTO;
import ru.yandex.market.api.cpa.yam.entity.Gender;
import ru.yandex.market.api.cpa.yam.entity.PartnerApplicationDocumentType;
import ru.yandex.market.api.cpa.yam.entity.RequestType;
import ru.yandex.market.api.cpa.yam.entity.SignatoryDocType;
import ru.yandex.market.api.cpa.yam.service.PrepayRequestService;
import ru.yandex.market.common.balance.xmlrpc.model.ClientContractsStructure;
import ru.yandex.market.common.balance.xmlrpc.model.OwnershipType;
import ru.yandex.market.common.balance.xmlrpc.model.PersonStructure;
import ru.yandex.market.common.balance.xmlrpc.model.ReviseActPeriodType;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.application.PartnerApplicationStatus;
import ru.yandex.market.core.application.crossborder.CrossborderPaymentMethod;
import ru.yandex.market.core.application.crossborder.dto.CrossborderOrganizationInfoDTO;
import ru.yandex.market.core.application.delivery.dto.DeliveryPartnerApplicationDetailsDTO;
import ru.yandex.market.core.application.delivery.dto.DeliveryPartnerApplicationDetailsPaymentInfoDTO;
import ru.yandex.market.core.application.delivery.dto.DeliveryPartnerApplicationDetailsPersonDTO;
import ru.yandex.market.core.balance.BalanceService;
import ru.yandex.market.core.balance.model.ClientContractInfo;
import ru.yandex.market.core.balance.model.FullClientInfo;
import ru.yandex.market.core.currency.Currency;
import ru.yandex.market.core.delivery.DeliveryServiceType;
import ru.yandex.market.core.delivery.PersonFormationType;
import ru.yandex.market.core.feature.model.FeatureType;
import ru.yandex.market.core.ff4shops.FF4ShopsPartnerState;
import ru.yandex.market.core.ff4shops.PartnerFulfillmentLinkForFF4Shops;
import ru.yandex.market.core.id.service.MarketIdGrpcService;
import ru.yandex.market.core.notification.service.NotificationSendContext;
import ru.yandex.market.core.notification.service.NotificationService;
import ru.yandex.market.core.orginfo.model.OrganizationType;
import ru.yandex.market.core.param.model.ParamCheckStatus;
import ru.yandex.market.core.protocol.ProtocolService;
import ru.yandex.market.core.protocol.model.UIDActionContext;
import ru.yandex.market.core.stocks.FF4ShopsClient;
import ru.yandex.market.core.supplier.notification.PartnerApplicationNotificationFactory;
import ru.yandex.market.core.supplier.notification.PartnerNotificationEventType;
import ru.yandex.market.core.tax.model.TaxSystem;
import ru.yandex.market.core.xml.impl.NamedContainer;
import ru.yandex.market.id.GetByPartnerRequest;
import ru.yandex.market.id.GetByPartnerResponse;
import ru.yandex.market.id.MarketAccount;
import ru.yandex.market.id.MarketIdServiceGrpc;
import ru.yandex.market.logistics.nesu.client.NesuClient;
import ru.yandex.market.logistics.nesu.client.enums.ShopRole;
import ru.yandex.market.logistics.nesu.client.model.RegisterShopDto;
import ru.yandex.market.logistics.util.client.exception.HttpTemplateException;
import ru.yandex.market.mbi.api.config.FunctionalTest;
import ru.yandex.market.mbi.util.MbiAsserts;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.core.protocol.model.ActionType.PREPAY_REQUEST_UPDATE;

/**
 * Функциональный тест для {@link PrepayRequestController}.
 *
 * @author fbokovikov
 */
@DbUnitDataSet(before = "prepayRequestControllerFunctionalTestFlag.csv")
class PrepayRequestControllerFunctionalTest extends FunctionalTest {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    @Qualifier("patientBalanceService")
    private BalanceService balanceService;

    @Autowired
    private PersistentAsyncCheckouterService persistentAsyncCheckouterService;

    @Autowired
    private NesuClient nesuClient;

    @Autowired
    private MarketIdServiceGrpc.MarketIdServiceImplBase marketIdServiceImplBase;

    @Autowired
    private MarketIdGrpcService marketIdGrpcService;

    @Autowired
    private FF4ShopsClient ff4ShopsClient;

    @Autowired
    private AsyncCheckouterService mockCheckouterClient;

    @Autowired
    private PrepayRequestService prepayRequestService;

    @Autowired
    private ProtocolService protocolService;

    @Captor
    private ArgumentCaptor<RegisterShopDto> nesuClientShopCaptor;

    private static Stream<Arguments> testGetPartnerApplicationArgs() {
        return Stream.of(
                Arguments.of(30013L, expectedPartnerApplicationDTODelivery(), "delivery")
        );
    }

    private static PrepayRequestDTO expectedPartnerApplicationDTODelivery() {
        DeliveryPartnerApplicationDetailsPersonDTO accountant = new DeliveryPartnerApplicationDetailsPersonDTO();
        accountant.setEmail("applicantEmail@yandex.ru");
        accountant.setFirstName("firstName");
        accountant.setLastName("lastName");
        accountant.setMiddleName(" ");

        DeliveryPartnerApplicationDetailsPaymentInfoDTO paymentInfo =
                new DeliveryPartnerApplicationDetailsPaymentInfoDTO();
        paymentInfo.setBankAccount("bank account");
        paymentInfo.setBankBik("bik");
        paymentInfo.setBankCorrAccount("bank corr account");
        paymentInfo.setBankName("bank name");
        paymentInfo.setPersonEmail("personEmail@yandex.ru");
        paymentInfo.setPersonFirstName("personFirstName");
        paymentInfo.setPersonLastName("personLastName");
        paymentInfo.setPersonMiddleName("personMiddleName");
        paymentInfo.setPersonPhone("+70123456789");
        paymentInfo.setTaxSystem(TaxSystem.USN);
        paymentInfo.setPersonPosition("position");
        paymentInfo.setPersonFormation(PersonFormationType.ORDER);

        DeliveryPartnerApplicationDetailsDTO details = new DeliveryPartnerApplicationDetailsDTO();
        details.setAccountant(accountant);
        details.setPaymentInfo(paymentInfo);
        details.setInn("inn");
        details.setKpp("kpp");
        details.setOgrn("ogrn");
        details.setLegalAddress("legal address");
        details.setOrganizationName("organizationName");
        details.setOrganizationType(OrganizationType.OOO);
        details.setPostAddress("post address");
        details.setPostAddressEqualsToLegal(false);
        details.setPostcode("000000");
        details.setWorkSchedule("work schedule");

        PrepayRequestDTO prepayRequest = new PrepayRequestDTO();
        prepayRequest.setRequestId(30013L);
        prepayRequest.setDatasourceIds(Collections.singletonList(33001L));
        prepayRequest.setStatus(PartnerApplicationStatus.INIT);
        prepayRequest.setPrepayType(PrepayTypeDTO.UNKNOWN);
        prepayRequest.setRequestType(RequestType.DELIVERY);
        prepayRequest.setDocuments(Collections.emptyList());
        prepayRequest.setFilled(true);
        prepayRequest.setDeliveryPartnerApplicationDetails(details);
        prepayRequest.setContactInfo(new ContactInfoDTO(
                "lastName firstName",
                "applicantEmail@yandex.ru",
                "+71234567890"
        ));
        return prepayRequest;
    }

    private static AssessorRequestForm getFormUpdatingContactsWithFioFields() {
        AssessorRequestForm form = new AssessorRequestForm();
        form.setDatasourceId(500L);
        AssessorRequestForm.ContactInfo contactInfo = new AssessorRequestForm.ContactInfo();
        contactInfo.setFirstName("Сережа");
        contactInfo.setMiddleName("");
        contactInfo.setLastName("Молодец");
        form.setContactInfo(contactInfo);

        AssessorRequestForm.SignatoryInfo signatoryInfo = new AssessorRequestForm.SignatoryInfo();
        signatoryInfo.setFirstName("Вера");
        signatoryInfo.setMiddleName("Владимировна");
        signatoryInfo.setLastName("Павлова");
        form.setSignatoryInfo(signatoryInfo);

        return form;
    }

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    /**
     * Протестировать выдачу ручки GET /prepay-request без параметров.
     */
    @Test
    @DbUnitDataSet(before = "prepayRequestControllerFunctionalTest.csv")
    void getRequestsNoParams() {
        RequestsInfoDTO prepayRequestIds = mbiApiClient.getPrepayRequestIds(null, null);
        ReflectionAssert.assertReflectionEquals(
                new RequestsInfoDTO(
                        Arrays.asList(
                                new RequestsInfoDTO.Data(1L,
                                        PartnerApplicationStatus.INIT,
                                        RequestType.MARKETPLACE,
                                        null,
                                        false
                                ),
                                new RequestsInfoDTO.Data(2L,
                                        PartnerApplicationStatus.IN_PROGRESS,
                                        RequestType.MARKETPLACE,
                                        null,
                                        false),
                                new RequestsInfoDTO.Data(22L,
                                        PartnerApplicationStatus.NEW,
                                        RequestType.MARKETPLACE,
                                        null,
                                        false),
                                new RequestsInfoDTO.Data(3L,
                                        PartnerApplicationStatus.INIT,
                                        RequestType.MARKETPLACE,
                                        "16723",
                                        true),
                                new RequestsInfoDTO.Data(5L,
                                        PartnerApplicationStatus.INIT,
                                        RequestType.MARKETPLACE,
                                        null,
                                        false)
                        )
                ),
                prepayRequestIds,
                ReflectionComparatorMode.LENIENT_ORDER
        );
    }

    /**
     * Протестировать выдачу ручки GET /prepay-request с некорректными параметрами.
     */
    @Test
    @DbUnitDataSet
    void getRequestsBadParams() {
        RequestsInfoDTO prepayRequestIds = mbiApiClient.getPrepayRequestIds(PartnerApplicationStatus.CLOSED, -1L);
        ReflectionAssert.assertReflectionEquals(
                new RequestsInfoDTO(Collections.emptyList()),
                prepayRequestIds
        );
    }

    /**
     * Протестировать выдачу ручки GET /prepay-request с фильтрами.
     */
    @Test
    @DbUnitDataSet(before = "prepayRequestControllerFunctionalTest.csv")
    void getRequests() {
        RequestsInfoDTO prepayRequestIds = mbiApiClient.getPrepayRequestIds(null,
                200L);
        ReflectionAssert.assertReflectionEquals(
                new RequestsInfoDTO(
                        List.of(
                                new RequestsInfoDTO.Data(2L,
                                        PartnerApplicationStatus.IN_PROGRESS,
                                        RequestType.MARKETPLACE,
                                        null,
                                        false),
                                new RequestsInfoDTO.Data(22L,
                                        PartnerApplicationStatus.NEW,
                                        RequestType.MARKETPLACE,
                                        null,
                                        false)
                        )
                ),
                prepayRequestIds,
                ReflectionComparatorMode.LENIENT_ORDER
        );
    }


    /**
     * Протестировать выдачу ручки GET /prepay-request с самозанятым.
     */
    @Test
    @DbUnitDataSet(before = "prepayRequestControllerFunctionalTest.csv")
    void getRequestsSelfEmployed() {
        RequestsInfoDTO prepayRequestIds = mbiApiClient.getPrepayRequestIds(null,
                301L);
        ReflectionAssert.assertReflectionEquals(
                new RequestsInfoDTO(
                        List.of(
                                new RequestsInfoDTO.Data(5L,
                                        PartnerApplicationStatus.INIT,
                                        RequestType.MARKETPLACE,
                                        null,
                                        false)
                        )
                ),
                prepayRequestIds,
                ReflectionComparatorMode.LENIENT_ORDER
        );
    }

    /**
     * Протестировать выдачу ручки GET /prepay-request/{requestId} с несуществующим request_id.
     */
    @Test
    @DbUnitDataSet
    void getRequestNotFound() {
        assertThrows(
                HttpClientErrorException.class,
                () -> mbiApiClient.getPrepayRequest(101L, null)
        );
    }

    /**
     * Протестировать выдачу ручки GET /prepay-request/{requestId} с существующим request_id, который хранится
     * в таблице {@code shops_web.V_PARTNER_APP_BUSINESS}.
     */
    @Test
    @DbUnitDataSet(before = "prepayRequestControllerFunctionalTest.csv")
    void getRequest() {
        PrepayRequestDTO prepayRequest = mbiApiClient.getPrepayRequest(1L, null);
        var expected = expectedRequestDTO(RequestType.MARKETPLACE);
        expected.setUpdatedAt(prepayRequest.getUpdatedAt());
        ReflectionAssert.assertReflectionEquals(expected, prepayRequest);
    }

    /**
     * Протестировать выдачу ручки GET /prepay-request/{requestId} с самозанятым
     */
    @Test
    @DbUnitDataSet(before = "prepayRequestControllerFunctionalTest.csv")
    void getSelfEmployedRequest() {
        PrepayRequestDTO prepayRequest = mbiApiClient.getPrepayRequest(5L, null);
        var expected = expectedSelfEmployedRequestDTO(RequestType.MARKETPLACE);
        expected.setUpdatedAt(prepayRequest.getUpdatedAt());
        ReflectionAssert.assertReflectionEquals(expected, prepayRequest);
    }

    /**
     * Протестировать выдачу ручки GET /prepay-request/{requestId} с существующим request_id, который хранится
     * в таблицах {@code shops_web.partner_app*}.
     */
    @ParameterizedTest(name = "{2}")
    @MethodSource("testGetPartnerApplicationArgs")
    @DbUnitDataSet(before = {"testGetPartnerApplication.csv", "prepayRequestControllerFunctionalTest.csv"})
    void testGetPartnerApplication(long requestId, PrepayRequestDTO expected, String desc) {
        ReflectionAssert.assertReflectionEquals(
                expected,
                mbiApiClient.getPrepayRequest(requestId, null),
                ReflectionComparatorMode.IGNORE_DEFAULTS
        );
    }

    @Test
    @DbUnitDataSet(before = "dontSendCompletedNotificationToDropship.before.csv",
            after = "dontSendCompletedNotificationToDropship.after.csv")
    void dontSendCompletedNotificationToDropship() {
        mockGetOrCreateMarketId(350);
        mbiApiClient.updatePrepayRequestStatus(50L,
                new RequestStatusForm(
                        PartnerApplicationStatus.COMPLETED,
                        "OK",
                        (List<Long>) null
                ), 1);
    }

    @Test
    @DbUnitDataSet(before = "testPrepayMarketId.before.csv")
    void testLinkMarketId() {
        mockLinkMarketId(1001);
        mbiApiClient.updatePrepayRequestStatus(
                11L,
                new RequestStatusForm(PartnerApplicationStatus.COMPLETED, "comment", (Long) null),
                1L
        );
        verify(marketIdServiceImplBase, times(1)).linkMarketIdRequest(any(), any());
        verify(marketIdServiceImplBase, never()).getOrCreateMarketId(any(), any());
        verify(mockCheckouterClient, times(1)).pushPartnerSettingsToCheckout(eq(Set.of(101L)));
    }

    @Test
    @DbUnitDataSet(
            before = "testPrepayMarketId.before.csv",
            after = "testPrepayMarketId.after.csv"
    )
    void testCreateMarketId() {
        mockGetOrCreateMarketId(1002);
        mbiApiClient.updatePrepayRequestStatus(
                12L,
                new RequestStatusForm(PartnerApplicationStatus.COMPLETED, "comment", (Long) null),
                2L
        );
        verify(marketIdServiceImplBase, never()).linkMarketIdRequest(any(), any());
        verify(marketIdServiceImplBase, times(1)).getOrCreateMarketId(any(), any());
    }

    @Test
    @DbUnitDataSet(before = "testSupplierStateSuccess.before.csv")
    void testDropshipSupplierStateSuccess() {
        mockLinkMarketId(1001);
        mbiApiClient.updatePrepayRequestStatus(
                11L,
                new RequestStatusForm(PartnerApplicationStatus.COMPLETED, "comment", (Long) null),
                1L
        );
        verify(ff4ShopsClient, times(1)).updatePartnerState(
                FF4ShopsPartnerState.newBuilder()
                        .withPartnerId(101L)
                        .withBusinessId(110L)
                        .withFeatureType(FeatureType.DROPSHIP)
                        .withFeatureStatus(ParamCheckStatus.DONT_WANT)
                        .withCpaIsPartnerInterface(false)
                        .withPushStocksIsEnabled(false)
                        .withFulfillmentLinks(List.of(
                                PartnerFulfillmentLinkForFF4Shops.newBuilder()
                                        .withServiceId(177)
                                        .withDeliveryServiceType(DeliveryServiceType.DROPSHIP)
                                        .build()
                        ))
                        .build());
    }

    private void mockLinkMarketId(long marketId) {
        doAnswer(invocation -> {
            StreamObserver<MarketAccount> marketAccountStreamObserver = invocation.getArgument(1);
            marketAccountStreamObserver.onNext(MarketAccount.newBuilder().setMarketId(marketId).build());
            marketAccountStreamObserver.onCompleted();
            return true;
        }).when(marketIdServiceImplBase).linkMarketIdRequest(any(), any());
    }

    private void mockGetOrCreateMarketId(long marketId) {
        doAnswer(invocation -> {
            StreamObserver<MarketAccount> marketAccountStreamObserver = invocation.getArgument(1);
            marketAccountStreamObserver.onNext(MarketAccount.newBuilder().setMarketId(marketId).build());
            marketAccountStreamObserver.onCompleted();
            return marketId;
        }).when(marketIdServiceImplBase).getOrCreateMarketId(any(), any());
        doAnswer(invocation -> {
            StreamObserver<MarketAccount> marketAccountStreamObserver = invocation.getArgument(1);
            marketAccountStreamObserver.onNext(MarketAccount.newBuilder().setMarketId(marketId).build());
            marketAccountStreamObserver.onCompleted();
            return null;
        }).when(marketIdServiceImplBase).confirmLegalInfo(any(), any());
        doAnswer(invocation -> {
            StreamObserver<GetByPartnerResponse> marketAccountStreamObserver = invocation.getArgument(1);
            GetByPartnerResponse response = GetByPartnerResponse.newBuilder()
                    .setMarketId(MarketAccount.newBuilder().setMarketId(marketId).build())
                    .setSuccess(true)
                    .build();

            marketAccountStreamObserver.onNext(response);
            marketAccountStreamObserver.onCompleted();
            return null;
        }).when(marketIdServiceImplBase).getByPartner(any(GetByPartnerRequest.class), any());
    }

    /**
     * Протестировать выдачу ручки PUT /prepay-request/{requestId}/status с несуществующим request_id.
     */
    @Test
    @DbUnitDataSet(before = "prepayRequestControllerFunctionalTest.csv")
    void putRequestStatusNotFound() {
        assertThrows(
                HttpClientErrorException.class,
                () -> mbiApiClient.updatePrepayRequestStatus(
                        1L,
                        new RequestStatusForm(PartnerApplicationStatus.CLOSED, "comment", 231651L),
                        123581321L)
        );
    }

    /**
     * Протестировать выдачу ручки PUT /prepay-request/{requestId}/status, валидный кейз для заявки из
     * {@code SHOPS_WEB.V_PARTNER_APP_BUSINESS}.
     */
    @Test
    @DbUnitDataSet(before = "putRequestStatus.before.csv",
                    after = "putRequestStatus.after.csv")
    void putRequestStatus() {
        mbiApiClient.updatePrepayRequestStatus(
                4L,
                new RequestStatusForm(PartnerApplicationStatus.NEED_INFO, "comment", (Long) null),
                123581321L
        );
    }

    /**
     * Протестировать выдачу ручки PUT /prepay-request/{requestId}/status, валидный кейз для заявки из
     * в таблицах {@code shops_web.partner_app*}.
     */
    @Test
    @DbUnitDataSet(
            before = "PrepayRequestControllerTestYaBu.before.csv",
            after = "putPartnerApplicationStatus.after.csv"
    )
    void putPartnerApplicationStatus() {
        createMockMarketIdGrpcService(100500L, 400L, true);
        mbiApiClient.updatePrepayRequestStatus(
                4L,
                new RequestStatusForm(PartnerApplicationStatus.NEED_INFO, "comment", (Long) null),
                123581321L
        );
    }

    /**
     * Протестировать выдачу ручки PUT /prepay-request/{requestId}/status
     * Не создается плательщик если партнер из БЮ яндекса
     */
    @Test
    @DbUnitDataSet(
            before = {"PrepayRequestControllerTestYaBu.before.csv"}
    )
    @DisplayName("Проверяем, что не создается плательщик для доставочного партнера, если партнер - БЮ Яндекса")
    void updatePrepayRequestStatusDoNotCreatePersonTest() {
        createMockMarketIdGrpcService(100500L, 400L, true);
        mbiApiClient.updatePrepayRequestStatus(
                4L,
                new RequestStatusForm(PartnerApplicationStatus.COMPLETED, "comment", (Long) null),
                123581321L
        );
        verify(balanceService).createClient(any(), anyLong(), anyLong());
        verifyNoMoreInteractions(balanceService, nesuClient);
    }

    /**
     * Мок для MarketIdGrpcService.
     * Это не тоже самое, что mockGetOrCreateMarketId()
     * Смотрите в каждом конкретном случае какой мок нужен
     */
    private void createMockMarketIdGrpcService(long marketId, long datasourceId, boolean isExist) {
        willAnswer(invocation -> isExist)
                .given(marketIdGrpcService).linkPartner(marketId, datasourceId);
        willDoNothing()
                .given(marketIdGrpcService).syncLegalInfo(anyLong(), anyLong(), anyBoolean());
        willAnswer(invocation -> OptionalLong.of(marketId))
                .given(marketIdGrpcService).getOrCreateMarketId(eq(datasourceId), anyBoolean());
    }

    /**
     * Повторная установка статуса, идемпотентноcть ручки.
     */
    @Test
    @DbUnitDataSet(
            before = "PrepayRequestControllerTestYaBu.before.csv",
            after = "putPartnerApplicationStatusNotChanged.after.csv"
    )
    void putPartnerApplicationStatusAlreadySet() {
        createMockMarketIdGrpcService(100500L, 400L, true);
        mbiApiClient.updatePrepayRequestStatus(
                4L,
                new RequestStatusForm(PartnerApplicationStatus.IN_PROGRESS, "comment", (Long) null),
                123581321L
        );

        verifyNoMoreInteractions(notificationService);
    }

    /**
     * Протестировать выдачу ручки PUT /prepay-request/{requestId}/status в случае некорректных параметров.
     */
    @Test
    @DbUnitDataSet
    void putRequestStatusDeclined() {
        HttpClientErrorException clientErrorException = assertThrows(
                HttpClientErrorException.class,
                () -> mbiApiClient.updatePrepayRequestStatus(
                        1L,
                        new RequestStatusForm(PartnerApplicationStatus.DECLINED, "comment", 100L),
                        1010101)
        );
        MbiAsserts.assertXmlEquals(
                "<validation-error>" +
                        "    <message>Status DECLINED may not be applied with datasourceId</message>" +
                        "    <codes>" +
                        "        <code>UNEXPECTED_DATASOURCE_ID</code>" +
                        "     </codes>" +
                        "</validation-error>",
                clientErrorException.getResponseBodyAsString()
        );
    }

    /**
     * Протестировать выдачу ручки PUT /prepay-request/{requestId}/status в случае некорректных параметров.
     */
    @Test
    @DbUnitDataSet
    void putRequestStatusNew() {
        HttpClientErrorException clientErrorException = assertThrows(
                HttpClientErrorException.class,
                () -> mbiApiClient.updatePrepayRequestStatus(
                        1L,
                        new RequestStatusForm(PartnerApplicationStatus.NEW, "comment", 100L),
                        1010101)
        );
        MbiAsserts.assertXmlEquals(
                "<validation-error>" +
                        "    <message>Status is not allowed: NEW</message>" +
                        "    <codes>" +
                        "        <code>FORBIDDEN_STATUS</code>" +
                        "     </codes>" +
                        "</validation-error>",
                clientErrorException.getResponseBodyAsString()
        );
    }

    /**
     * Протестировать выдачу ручки PUT /prepay-request/{requestId}/status в случае некорректных параметров.
     */
    @Test
    @DbUnitDataSet
    void putRequestStatusFrozen() {
        HttpClientErrorException clientErrorException = assertThrows(
                HttpClientErrorException.class,
                () -> mbiApiClient.updatePrepayRequestStatus(
                        1L,
                        new RequestStatusForm(PartnerApplicationStatus.FROZEN, "comment", (Long) null),
                        1010101)
        );
        MbiAsserts.assertXmlEquals(
                "<validation-error>" +
                        "    <message>Status FROZEN may not be applied without datasourceId</message>" +
                        "    <codes>" +
                        "        <code>EMPTY_DATASOURCE_ID</code>" +
                        "     </codes>" +
                        "</validation-error>",
                clientErrorException.getResponseBodyAsString()
        );
    }

    /**
     * Протестировать выдачу ручки PUT /prepay-request/{requestId}/status при переводе в статус completed.
     */
    @Test
    @DbUnitDataSet(before = "putRequestStatus.before.csv",
                    after = "putRequestStatusCompleted.after.csv")
    void putRequestStatusCompleted() {
        mockGetOrCreateMarketId(500);
        mbiApiClient.updatePrepayRequestStatus(
                5L,
                new RequestStatusForm(PartnerApplicationStatus.COMPLETED, "comment", (List<Long>) null),
                1010101);
    }

    @Test
    @DisplayName("прохождение проверки хотя бы по одному товару поставщика не маппится ни на один PartnerNotificationEvent")
    //проверяем что уведомление из MappingApprovedStrategy никогда не отправляется - оно будет отправляться по другой схеме
    void testEmptyNotificationEventForMappingApproved() {
        boolean check = Stream.of(PartnerApplicationStatus.values())
                              .map(PartnerApplicationNotificationFactory::mapRequestToEvent)
                              .noneMatch(optEvent -> optEvent.isPresent() &&
                                                     optEvent.get().equals(PartnerNotificationEventType.MAPPING_APPROVED));
        assertTrue(check);
    }

    @Test
    @DbUnitDataSet(before = "putRequestStatusCompletedToFrozen.before.csv",
                    after = {"putRequestStatusCompletedToFrozen.after.csv",
                            "prepayRequestControllerFunctionalTestFlag.csv"})
    void putRequestStatusFrozenWithNotify() {
        doReturn(1L).when(notificationService).send(any(NotificationSendContext.class));
        mbiApiClient.updatePrepayRequestStatus(
                6L,
                new RequestStatusForm(PartnerApplicationStatus.FROZEN, "comment", List.of(600L)),
                1010101);
        ArgumentCaptor<NotificationSendContext> captor = ArgumentCaptor.forClass(NotificationSendContext.class);
        verify(notificationService).send(captor.capture());
        List<NamedContainer> actual = captor.getValue().getData();
        List<NamedContainer> expected = List.of(
                        new NamedContainer("partner-name", "test_organization"),
                        new NamedContainer("campaign-id", 3L),
                        new NamedContainer("partner-contract-id", "951480/20"),
                        new NamedContainer("is-supplier", false),
                        new NamedContainer("is-shop", true),
                        new NamedContainer("business-id", 1001L)
                );
        if (expected.size() != actual.size()) {
            throw new IllegalStateException("number of params are differs!");
        }
        IntStream.range(0, actual.size()).forEach( i -> {
            assertEquals(expected.get(i), actual.get(i));
        });

    }

    /**
     * Протестировать переход в статус NEED_INFO из статуса COMPLETED.
     * Проверяем, что фейлим фичу MARKETPLACE, переводим заявку в NEED_INFO и не ломаем контракты.
     */
    @Test
    @DbUnitDataSet(
            before = "putRequestStatusCompletedToNeedInfo.before.csv",
            after = "putRequestStatusCompletedToNeedInfo.after.csv")
    void putRequestStatusCompletedToNeedInfo() {
        mockGetOrCreateMarketId(2014092L);
        mbiApiClient.updatePrepayRequestStatus(
                426892L,
                new RequestStatusForm(PartnerApplicationStatus.NEED_INFO, "comment", (List<Long>) null),
                1527891797L);
    }

    /**
     * Тестируем полный переход из COMPLETED  в COMPLETED через NEED_INFO.
     */
    @Test
    @DbUnitDataSet(
            before = "putRequestStatusCompletedToNeedInfo.before.csv",
            after = "putRequestStatusCompletedToCompleted.after.csv")
    void putRequestStatusCompletedToCompleted() {
        mockGetOrCreateMarketId(2014092L);
        mockLinkMarketId(2014092L);

        mbiApiClient.updatePrepayRequestStatus(
                426892L,
                new RequestStatusForm(PartnerApplicationStatus.NEED_INFO, "comment", (List<Long>) null),
                1527891797L);
        protocolService.operationInTransaction(
                new UIDActionContext(PREPAY_REQUEST_UPDATE, 1527891797L),
                (transactionStatus, actionId) -> {
                    prepayRequestService.updateRequestStatus(426892L, PartnerApplicationStatus.INIT, null,
                            "comment", 2014092L, 1527891797L, 1);
                });
        mbiApiClient.updatePrepayRequestStatus(
                426892L,
                new RequestStatusForm(PartnerApplicationStatus.COMPLETED, "comment", (List<Long>) null),
                1527891797);
    }

    /**
     * Протестировать выдачу ручки PUT /prepay-request/{requestId} валидный кейз.
     * Обновляются не все поля.
     */
    @Test
    @DbUnitDataSet(before = "putRequest.before.csv", after = "putRequestSomeFields.after.csv")
    void putRequestSomeFields() {
        // when
        mbiApiClient.updatePrepayRequest(5L, prepareForm(), 321L);

        // then
        var argClient = ArgumentCaptor.forClass(FullClientInfo.class);
        verify(balanceService).createClient(argClient.capture(), anyLong(), anyLong());
        var clientInfo = argClient.getValue();
        assertThat(clientInfo.getEmail(), equalTo("test@mail.ru"));
        assertThat(clientInfo.getName(), nullValue());

        var argPerson = ArgumentCaptor.forClass(PersonStructure.class);
        verify(balanceService).createOrUpdatePerson(argPerson.capture(), anyLong());
        var person = argPerson.getValue();
        assertThat(person.getLongName(), nullValue());
        assertThat(person.getName(), nullValue());
        assertThat(person.getReviseActPeriodType(), equalTo(ReviseActPeriodType.MONTHLY));
        assertThat(person.getString("EMAIL"), equalTo("test@mail.ru"));
    }

    /**
     * Протестировать выдачу ручки PUT /prepay-request/{requestId} валидный кейс.
     * Обновление всех полей заявки.
     */
    @Test
    @DbUnitDataSet(before = "putRequest.before.csv", after = "putRequestAllFields.after.csv")
    void putRequestAllFields() {
        // when
        mbiApiClient.updatePrepayRequest(5L, prepareFullForm(), 321L);

        // then
        var argClient = ArgumentCaptor.forClass(FullClientInfo.class);
        verify(balanceService).createClient(argClient.capture(), anyLong(), anyLong());
        var clientInfo = argClient.getValue();
        assertThat(clientInfo.getEmail(), equalTo("test@mail.ru"));
        assertThat(clientInfo.getName(), equalTo("ОАО DMF"));

        var argPerson = ArgumentCaptor.forClass(PersonStructure.class);
        verify(balanceService).createOrUpdatePerson(argPerson.capture(), anyLong());
        var person = argPerson.getValue();
        assertThat(person.getLongName(), equalTo("ОАО DMF"));
        assertThat(person.getName(), equalTo("DMF"));
        assertThat(person.getString("EMAIL"), equalTo("test@mail.ru"));
    }

    /**
     * Протестировать выдачу ручки PUT /prepay-request/{requestId}.
     * при передаче непечатного символа в полях информации об организации.
     */
    @Test
    @DbUnitDataSet(before = "putRequest.before.csv")
    void putRequestWithForbiddenSymbol() {
        HttpClientErrorException exception = assertThrows(
                HttpClientErrorException.class,
                () -> mbiApiClient.updatePrepayRequest(5L, prepareFormWithForbiddenSymbol(), 321L)
        );
        String errorMsg = exception.getResponseBodyAsString();
        assertThat(errorMsg, containsString("'Organization name' field contains illegal characters"));
        assertThat(errorMsg, containsString("'Bank name' field contains illegal characters"));
        assertThat(errorMsg, containsString("'KPP' field contains illegal characters"));
        assertThat(errorMsg, containsString("'License number' field contains illegal characters"));
        assertThat(errorMsg, containsString("'Work schedule' field contains illegal characters"));
        assertThat(errorMsg, containsString("'OGRN' field contains illegal characters"));
        assertThat(errorMsg, containsString("'Juridical address' field contains illegal characters"));
    }

    /**
     * Протестировать выдачу ручки PUT /prepay-request/{requestId} с пустым datasource_id.
     */
    @Test
    void putRequestNoDatasource() {
        assertThrows(
                HttpClientErrorException.class,
                () -> mbiApiClient.updatePrepayRequest(3L, new AssessorRequestForm(), 321L)
        );
    }

    @Test
    @DbUnitDataSet(
            before = "supplierApplicationCompleted.before.csv",
            after = "supplierApplicationCompleted.after.csv"
    )
    @DisplayName("Выставление статуса COMPLETED для заявки поставщика")
    void supplierApplicationCompleted() {
        mockBalance();
        mockGetOrCreateMarketId(400);
        mbiApiClient.updatePrepayRequestStatus(
                4L,
                new RequestStatusForm(PartnerApplicationStatus.COMPLETED, "comment", (List<Long>) null),
                1010101
        );
        verify(notificationService).send(
                new NotificationSendContext.Builder()
                        .setTypeId(1537796797)
                        .setShopId(400L)
                        .setData(List.of(
                                new NamedContainer("supplier-name", "supplier1"),
                                new NamedContainer("supplier-application-id", "151511/18"),
                                new NamedContainer("campaign-id", 10L),
                                new NamedContainer("supplier-dropship", false),
                                new NamedContainer("supplier-crossdock", false)
                        ))
                        .build()
        );

        verifyNoMoreInteractions(nesuClient);
    }

    @Test
    @DbUnitDataSet(
            before = "shopPrepayRequestCompleted.before.csv",
            after = "shopPrepayRequestCompleted.after.csv"
    )
    @DisplayName("Выставление статуса COMPLETED для заявки белого магазина")
    void whiteShopPrepayApplicationCompleted() {
        mockBalance();
        mockGetOrCreateMarketId(500);

        mbiApiClient.updatePrepayRequestStatus(
                500L,
                new RequestStatusForm(PartnerApplicationStatus.COMPLETED, "comment", (List<Long>) null),
                1010101
        );

        verify(mockCheckouterClient, times(1)).pushPartnerSettingsToCheckout(eq(Set.of(500L)));
    }

    @Test
    @DbUnitDataSet(
            before = "shopApplicationCompleted.before.csv",
            after = "shopApplicationCompleted.after.csv"
    )
    @DisplayName("Выставление статуса COMPLETED для заявки DBS-магазина")
    void shopApplicationCompleted() {
        mockBalance();
        mockGetOrCreateMarketId(300);

        mbiApiClient.updatePrepayRequestStatus(
                3L,
                new RequestStatusForm(PartnerApplicationStatus.COMPLETED, "comment", (List<Long>) null),
                1010101
        );

        ArgumentCaptor<NotificationSendContext> captor = ArgumentCaptor.forClass(NotificationSendContext.class);
        verify(notificationService).send(captor.capture());
        verify(nesuClient).configureShop(eq(300L), any());
        verifyNoMoreInteractions(nesuClient);

        NotificationSendContext ctx = captor.getValue();
        assertThat(ctx.getTypeId(), is(1615204491));
        assertThat(ctx.getShopId(), is(300L));
        assertThat((List<NamedContainer>) ctx.getData(), containsInAnyOrder(
                new NamedContainer("campaign-id", 11001L),
                new NamedContainer("shop-name", "Workout")
        ));
    }

    @Test
    @DbUnitDataSet(
            before = "shopApplicationCompleted.before.csv"
    )
    @DisplayName("Проверка ретраев при 404 от несу")
    void nesu404Retries() {
        mockBalance();
        mockGetOrCreateMarketId(300);
        Mockito.doThrow(new HttpTemplateException(HttpStatus.NOT_FOUND.value(), "resource not found"))
                .doNothing()
                .when(nesuClient).configureShop(eq(300L), any());
        mbiApiClient.updatePrepayRequestStatus(
                3L,
                new RequestStatusForm(PartnerApplicationStatus.COMPLETED, "comment", (List<Long>) null),
                1010101
        );

        verify(nesuClient, times(2)).configureShop(eq(300L), any());
    }


    @Test
    @DbUnitDataSet(
            before = {"shopApplicationCompleted.before.csv", "shopApplicationCompletedForBUYandex.before.csv"},
            after = "shopApplicationCompletedForBUYandex.after.csv"
    )
    @DisplayName("Выставление статуса COMPLETED для заявки DBS-магазина бизнес юнита Яндекса")
    void shopApplicationCompletedForBUYandex() {
        when(balanceService.createClient(any(), anyLong(), anyLong())).thenReturn(1L);
        mockGetOrCreateMarketId(300);

        mbiApiClient.updatePrepayRequestStatus(
                3L,
                new RequestStatusForm(PartnerApplicationStatus.COMPLETED, "comment", (List<Long>) null),
                1010101
        );

        verifyNoMoreInteractions(nesuClient);
    }

    @Test
    @DbUnitDataSet(
            before = "supplierApplicationNeedInfo.before.csv",
            after = "supplierApplicationNeedInfo.after.csv"
    )
    @DisplayName("Выставление статуса NEED_INFO для заявки поставщика")
    void supplierApplicationNeedInfo() {
        mbiApiClient.updatePrepayRequestStatus(
                3L,
                new RequestStatusForm(PartnerApplicationStatus.NEED_INFO, "comment", (List<Long>) null),
                1010101
        );

        ArgumentCaptor<NotificationSendContext> captor = ArgumentCaptor.forClass(NotificationSendContext.class);
        verify(notificationService).send(captor.capture());

        NotificationSendContext ctx = captor.getValue();
        List<NamedContainer> data = ctx.getData();
        assertThat(ctx.getTypeId(), is(1537796763));
        assertThat(ctx.getShopId(), is(300L));
        assertThat(data,
                containsInAnyOrder(
                        new NamedContainer("supplier-name", "Workout"),
                        new NamedContainer("assessor-comment", "comment"),
                        new NamedContainer("campaign-id", 200L)
                )
        );
    }

    @Test
    @DbUnitDataSet(
            before = "shopApplicationNeedInfo.before.csv",
            after = "shopApplicationNeedInfo.after.csv"
    )
    @DisplayName("Выставление статуса NEED_INFO для заявки магазина")
    void shopApplicationNeedInfo() {
        mbiApiClient.updatePrepayRequestStatus(
                3L,
                new RequestStatusForm(PartnerApplicationStatus.NEED_INFO, "comment", (List<Long>) null),
                1010101
        );

        ArgumentCaptor<NotificationSendContext> captor = ArgumentCaptor.forClass(NotificationSendContext.class);
        verify(notificationService).send(captor.capture());

        NotificationSendContext ctx = captor.getValue();
        assertThat(ctx.getTypeId(), is(1614942847));
        assertThat(ctx.getShopId(), is(300L));
        assertThat((List<NamedContainer>) ctx.getData(), containsInAnyOrder(
                new NamedContainer("campaign-id", 11001L),
                new NamedContainer("shop-name", "Workout")
        ));
    }

    @Test
    @DbUnitDataSet(
            before = "supplierApplicationInProgress.before.csv",
            after = "supplierApplicationInProgress.after.csv"
    )
    @DisplayName("Выставление статуса IN_PROGRESS для заявки поставщика")
    void supplierApplicationInProgress() {
        mbiApiClient.updatePrepayRequestStatus(
                4L,
                new RequestStatusForm(PartnerApplicationStatus.IN_PROGRESS, "comment", (List<Long>) null),
                1010101
        );

        ArgumentCaptor<NotificationSendContext> captor = ArgumentCaptor.forClass(NotificationSendContext.class);
        verify(notificationService).send(captor.capture());

        NotificationSendContext ctx = captor.getValue();
        assertThat(ctx.getTypeId(), is(1537796735));
        assertThat(ctx.getShopId(), is(400L));
        assertThat((List<NamedContainer>) ctx.getData(),
                containsInAnyOrder(new NamedContainer("supplier-name", "Stride")));
    }

    @Test
    @DbUnitDataSet(
            before = "supplierApplicationDeclined.before.csv",
            after = "supplierApplicationDeclined.after.csv"
    )
    @DisplayName("Выставление статуса DECLINED для заявки поставщика (без флагов)")
    void supplierApplicationDeclined() {
        //FBS - IN_PROGRESS -> DECLINED
        mbiApiClient.updatePrepayRequestStatus(
                4L,
                new RequestStatusForm(PartnerApplicationStatus.DECLINED, "comment", (List<Long>) null),
                1010101
        );

        ArgumentCaptor<NotificationSendContext> captor = ArgumentCaptor.forClass(NotificationSendContext.class);
        verify(notificationService).send(captor.capture());

        NotificationSendContext ctx = captor.getValue();
        assertThat(ctx.getTypeId(), is(1537796781));
        assertThat(ctx.getShopId(), is(400L));
        assertThat((List<NamedContainer>) ctx.getData(),
                containsInAnyOrder(
                        new NamedContainer("supplier-name", "Supplier-400"),
                        new NamedContainer("campaign-id", 10L)
                ));
        //FBS - INIT -> DECLINED
        mbiApiClient.updatePrepayRequestStatus(
                5L,
                new RequestStatusForm(PartnerApplicationStatus.DECLINED, "comment", (List<Long>) null),
                1010101
        );

        verify(notificationService, times(2)).send(captor.capture());

        ctx = captor.getValue();
        assertThat(ctx.getTypeId(), is(1537796781));
        assertThat(ctx.getShopId(), is(500L));
        assertThat((List<NamedContainer>) ctx.getData(),
                containsInAnyOrder(
                        new NamedContainer("supplier-name", "Supplier-500"),
                        new NamedContainer("campaign-id", 20L)
                ));
        //FBS - COMPLETED -> DECLINED
        mbiApiClient.updatePrepayRequestStatus(
                6L,
                new RequestStatusForm(PartnerApplicationStatus.DECLINED, "comment", (List<Long>) null),
                1010101
        );

        verify(notificationService, times(3)).send(captor.capture());

        ctx = captor.getValue();
        assertThat(ctx.getTypeId(), is(1537796781));
        assertThat(ctx.getShopId(), is(600L));
        assertThat((List<NamedContainer>) ctx.getData(),
                containsInAnyOrder(
                        new NamedContainer("supplier-name", "Supplier-600"),
                        new NamedContainer("campaign-id", 30L)
                ));
        //DBS - COMPLETED -> DECLINED
        mbiApiClient.updatePrepayRequestStatus(
                7L,
                new RequestStatusForm(PartnerApplicationStatus.DECLINED, "comment", (List<Long>) null),
                1010101
        );

        verify(notificationService, times(3)).send(any(NotificationSendContext.class));
    }

    @Test
    @DbUnitDataSet(
            before = {"supplierApplicationDeclined.before.csv", "supplierApplicationDeclined-env1.before.csv"},
            after = "supplierApplicationDeclined.after.csv"
    )
    @DisplayName("Выставление статуса DECLINED для заявки поставщика (подбор шаблона по прошлому статусу)")
    void supplierApplicationDeclinedUseTwoTemplates() {
        //FBS - IN_PROGRESS -> DECLINED
        mbiApiClient.updatePrepayRequestStatus(
                4L,
                new RequestStatusForm(PartnerApplicationStatus.DECLINED, "comment", (List<Long>) null),
                1010101
        );

        ArgumentCaptor<NotificationSendContext> captor = ArgumentCaptor.forClass(NotificationSendContext.class);
        verify(notificationService).send(captor.capture());

        NotificationSendContext ctx = captor.getValue();
        assertThat(ctx.getTypeId(), is(1537796781));
        assertThat(ctx.getShopId(), is(400L));
        assertThat((List<NamedContainer>) ctx.getData(),
                containsInAnyOrder(
                        new NamedContainer("supplier-name", "Supplier-400"),
                        new NamedContainer("campaign-id", 10L)
                ));
        //FBS - INIT -> DECLINED
        mbiApiClient.updatePrepayRequestStatus(
                5L,
                new RequestStatusForm(PartnerApplicationStatus.DECLINED, "comment", (List<Long>) null),
                1010101
        );

        verify(notificationService, times(2)).send(captor.capture());

        ctx = captor.getValue();
        assertThat(ctx.getTypeId(), is(1537796781));
        assertThat(ctx.getShopId(), is(500L));
        assertThat((List<NamedContainer>) ctx.getData(),
                containsInAnyOrder(
                        new NamedContainer("supplier-name", "Supplier-500"),
                        new NamedContainer("campaign-id", 20L)
                ));
        //FBS - COMPLETED -> DECLINED
        mbiApiClient.updatePrepayRequestStatus(
                6L,
                new RequestStatusForm(PartnerApplicationStatus.DECLINED, "comment", (List<Long>) null),
                1010101
        );

        verify(notificationService, times(3)).send(captor.capture());

        ctx = captor.getValue();
        assertThat(ctx.getTypeId(), is(1656292800));
        assertThat(ctx.getShopId(), is(600L));
        assertThat((List<NamedContainer>) ctx.getData(),
                containsInAnyOrder(
                        new NamedContainer("supplier-name", "Supplier-600"),
                        new NamedContainer("campaign-id", 30L),
                        new NamedContainer("supplier-id", 600L),
                        new NamedContainer("partner-contract-id", "183464/20"),
                        new NamedContainer("partners-count", 1)
                ));

        //DBS - COMPLETED -> DECLINED
        mbiApiClient.updatePrepayRequestStatus(
                7L,
                new RequestStatusForm(PartnerApplicationStatus.DECLINED, "comment", (List<Long>) null),
                1010101
        );

        verify(notificationService, times(3)).send(any(NotificationSendContext.class));
    }

    @Test
    @DbUnitDataSet(
            before = {"supplierApplicationDeclined.before.csv", "supplierApplicationDeclined-env2.before.csv"},
            after = "supplierApplicationDeclined.after.csv"
    )
    @DisplayName("Выставление статуса DECLINED для заявки поставщика (шлем письма FBS и DBS)")
    void supplierApplicationDeclinedSendDbsAndFbs() {
        //FBS - IN_PROGRESS -> DECLINED
        mbiApiClient.updatePrepayRequestStatus(
                4L,
                new RequestStatusForm(PartnerApplicationStatus.DECLINED, "comment", (List<Long>) null),
                1010101
        );

        ArgumentCaptor<NotificationSendContext> captor = ArgumentCaptor.forClass(NotificationSendContext.class);
        verify(notificationService).send(captor.capture());

        NotificationSendContext ctx = captor.getValue();
        assertThat(ctx.getTypeId(), is(1537796781));
        assertThat(ctx.getShopId(), is(400L));
        assertThat((List<NamedContainer>) ctx.getData(),
                containsInAnyOrder(
                        new NamedContainer("supplier-name", "Supplier-400"),
                        new NamedContainer("campaign-id", 10L)
                ));
        //FBS - INIT -> DECLINED
        mbiApiClient.updatePrepayRequestStatus(
                5L,
                new RequestStatusForm(PartnerApplicationStatus.DECLINED, "comment", (List<Long>) null),
                1010101
        );

        verify(notificationService, times(2)).send(captor.capture());

        ctx = captor.getValue();
        assertThat(ctx.getTypeId(), is(1537796781));
        assertThat(ctx.getShopId(), is(500L));
        assertThat((List<NamedContainer>) ctx.getData(),
                containsInAnyOrder(
                        new NamedContainer("supplier-name", "Supplier-500"),
                        new NamedContainer("campaign-id", 20L)
                ));
        //FBS - COMPLETED -> DECLINED
        mbiApiClient.updatePrepayRequestStatus(
                6L,
                new RequestStatusForm(PartnerApplicationStatus.DECLINED, "comment", (List<Long>) null),
                1010101
        );

        verify(notificationService, times(3)).send(captor.capture());

        ctx = captor.getValue();
        assertThat(ctx.getTypeId(), is(1537796781));
        assertThat(ctx.getShopId(), is(600L));
        assertThat((List<NamedContainer>) ctx.getData(),
                containsInAnyOrder(
                        new NamedContainer("supplier-name", "Supplier-600"),
                        new NamedContainer("campaign-id", 30L)
                ));

        //DBS - COMPLETED -> DECLINED
        mbiApiClient.updatePrepayRequestStatus(
                7L,
                new RequestStatusForm(PartnerApplicationStatus.DECLINED, "comment", (List<Long>) null),
                1010101
        );

        verify(notificationService, times(4)).send(captor.capture());

        ctx = captor.getValue();
        assertThat(ctx.getTypeId(), is(1537796781));
        assertThat(ctx.getShopId(), is(700L));
        assertThat((List<NamedContainer>) ctx.getData(),
                containsInAnyOrder(
                        new NamedContainer("supplier-name", "Shop-700"),
                        new NamedContainer("campaign-id", 40L)
                ));
    }

    @Test
    @DisplayName("Заявка найдена, а поставщик - нет")
    @DbUnitDataSet(before = "prepayRequestControllerFunctionalTest.csv")
    void supplierNotFound() {
        HttpClientErrorException exception = assertThrows(
                HttpClientErrorException.class,
                () -> mbiApiClient.updatePrepayRequestStatus(
                        3L,
                        new RequestStatusForm(PartnerApplicationStatus.DECLINED, "comment", (List<Long>) null),
                        1010101
                )
        );
        MbiAsserts.assertXmlEquals(
                "<validation-error>" +
                        "    <message>All datasources must have campaigns</message>" +
                        "    <codes>" +
                        "        <code>REQUEST_INCONSISTENCY</code>" +
                        "    </codes>" +
                        "</validation-error>",
                exception.getResponseBodyAsString()
        );
    }

    @Test
    @DbUnitDataSet(
            before = "csv/delivery-partner-application-completed.before.csv",
            after = "csv/delivery-partner-application-completed.after.csv"
    )
    @DisplayName("Выставление статуса COMPLETED для заявки на подключение к доставке")
    void deliveryPartnerApplicationCompleted() {
        doNothing().when(persistentAsyncCheckouterService).handlePendingTasks(anySet());
        mockGetOrCreateMarketId(2000000);
        mbiApiClient.updatePrepayRequestStatus(
                4L,
                new RequestStatusForm(PartnerApplicationStatus.COMPLETED, "comment", (List<Long>) null),
                1010101L
        );
        verify(nesuClient, times(1)).registerShop(any());
        verify(marketIdServiceImplBase, times(1)).getOrCreateMarketId(any(), any());

        verify(mockCheckouterClient, never()).pushPartnerSettingsToCheckout(anySet());
    }

    @Test
    @DbUnitDataSet(
            before = "csv/delivery-partner-application-completed-before-market-id.csv",
            after = "csv/delivery-partner-application-completed.after.csv"
    )
    @DisplayName("Выставление статуса COMPLETED для заявки на подключение к доставке с указанным маркетИД")
    void deliveryPartnerApplicationCompletedWithMarketId() {
        doNothing().when(persistentAsyncCheckouterService).handlePendingTasks(anySet());
        mockLinkMarketId(2000000);
        doAnswer(invocation -> {
            throw new IllegalArgumentException();
        }).when(marketIdServiceImplBase).getOrCreateMarketId(any(), any());
        mbiApiClient.updatePrepayRequestStatus(
                4L,
                new RequestStatusForm(PartnerApplicationStatus.COMPLETED, "comment", (List<Long>) null),
                1010101L
        );
        verify(nesuClient, times(1)).registerShop(any());
        verify(marketIdServiceImplBase, times(1)).linkMarketIdRequest(any(), any());
    }

    @Test
    @DbUnitDataSet(before = "registerNesuForSupplier.before.csv")
    void registerNesuForDropship() {
        doNothing().when(persistentAsyncCheckouterService).handlePendingTasks(anySet());
        doAnswer(invocation -> {
            StreamObserver<MarketAccount> marketAccountStreamObserver = invocation.getArgument(1);
            marketAccountStreamObserver.onNext(MarketAccount.newBuilder().setMarketId(10505L).build());
            marketAccountStreamObserver.onCompleted();
            return null;
        }).when(marketIdServiceImplBase).getOrCreateMarketId(any(), any());
        doAnswer(invocation -> {
            StreamObserver<MarketAccount> marketAccountStreamObserver = invocation.getArgument(1);
            marketAccountStreamObserver.onNext(MarketAccount.newBuilder().setMarketId(10505L).build());
            marketAccountStreamObserver.onCompleted();
            return null;
        }).when(marketIdServiceImplBase).confirmLegalInfo(any(), any());
        mbiApiClient.updatePrepayRequestStatus(55L,
                new RequestStatusForm(
                        PartnerApplicationStatus.COMPLETED,
                        "OK",
                        (Long) null
                ), 1);

        verify(nesuClient, times(1)).registerShop(nesuClientShopCaptor.capture());
        assertEquals(RegisterShopDto.builder()
                        .id(555L)
                        .marketId(10505L)
                        .regionId(213)
                        .businessId(155L)
                        .role(ShopRole.DROPSHIP)
                        .balanceClientId(51L)
                        .balanceContractId(10005L)
                        .balancePersonId(1L)
                        .name("Universal")
                        .build(),
                nesuClientShopCaptor.getValue());
    }

    @DisplayName("В Несу может зарегистрироваться только партнёр у которого заявка в статусе COMPLETED")
    @Test
    @DbUnitDataSet(before = "registerNesuForSupplier.before.csv")
    void dontRegisterNesuForNotComletedStatus() {
        doNothing().when(persistentAsyncCheckouterService).handlePendingTasks(anySet());
        mbiApiClient.updatePrepayRequestStatus(66L,
                new RequestStatusForm(PartnerApplicationStatus.IN_PROGRESS, "OK", (Long) null), 1);
        mbiApiClient.updatePrepayRequestStatus(66L,
                new RequestStatusForm(PartnerApplicationStatus.DECLINED, "OK", (Long) null), 1);

        verifyZeroInteractions(nesuClient);
    }

    @Test
    @DbUnitDataSet(
            before = "csv/delivery-partner-application-completed.before.csv",
            after = "csv/delivery-partner-application-need-info.after.csv"
    )
    @DisplayName("Выставление статуса NEED_INFO для заявки на подключение к доставке")
    void deliveryPartnerApplicationNeedInfo() {
        mbiApiClient.updatePrepayRequestStatus(
                4L,
                new RequestStatusForm(PartnerApplicationStatus.NEED_INFO, "comment", (List<Long>) null),
                1010101L
        );
    }

    @Test
    @DbUnitDataSet(
            before = "csv/delivery-partner-application-completed.before.csv",
            after = "csv/delivery-partner-application-in-progress.after.csv"
    )
    @DisplayName("Выставление статуса IN_PROGRESS для заявки на подключение к доставке")
    void deliveryPartnerApplicationInProgress() {
        mbiApiClient.updatePrepayRequestStatus(
                4L,
                new RequestStatusForm(PartnerApplicationStatus.IN_PROGRESS, "comment", (List<Long>) null),
                1010101
        );
    }

    @Test
    @DbUnitDataSet(
            before = "csv/delivery-partner-application-completed.before.csv",
            after = "csv/delivery-partner-application-decline.after.csv"
    )
    @DisplayName("Выставление статуса DECLINED для заявки на подключение к доставке")
    void deliveryPartnerApplicationDeclined() {
        mbiApiClient.updatePrepayRequestStatus(
                4L,
                new RequestStatusForm(PartnerApplicationStatus.DECLINED, "comment", (List<Long>) null),
                1010101
        );
    }

    @DbUnitDataSet(
            before = "updateContactAndSignatoryNames.before.csv",
            after = "updateContactAndSignatoryNamesWithFields.after.csv"
    )
    @Test
    void updateContactAndSignatoryNames_withFields() {
        mbiApiClient.updatePrepayRequest(
                5L,
                getFormUpdatingContactsWithFioFields(),
                1010101
        );
    }

    @DbUnitDataSet(before = "getPrepayRequest.before.csv")
    @Test
    void testGetPrepayRequest() {
        PrepayRequestDTO actual = mbiApiClient.getPrepayRequest(4, 400L);
        var expected = createExpectedPrepayRequestWithNames();
        expected.setUpdatedAt(actual.getUpdatedAt());
        ReflectionAssert.assertReflectionEquals(expected, actual);
    }

    @DbUnitDataSet(before = "PrepayRequestControllerTest.supplier.purge.before.csv")
    @Test
    void purgeValidateInput() {
        assertThrows(HttpClientErrorException.class,
                () -> mbiApiClient.purgeRequestApplication(122349, 1234, null));
        assertThrows(HttpClientErrorException.class,
                () -> mbiApiClient.purgeRequestApplication(122349, 1234, ""));
    }

    @DbUnitDataSet(before = "PrepayRequestControllerTest.supplier.purge.before.csv")
    @Test
    void purgeWrongApplication() {
        assertThrows(HttpClientErrorException.class,
                () -> mbiApiClient.purgeRequestApplication(404, 1234, "MBI-00000"));
        assertThrows(HttpClientErrorException.class,
                () -> mbiApiClient.purgeRequestApplication(122500, 1234, "MBI-00000"));
    }

    @Test
    @DbUnitDataSet(before = "PrepayRequestControllerTest.supplier.purge.before.csv",
            after = "PrepayRequestControllerTest.supplier.purge.after.csv")
    void purge() {
        mbiApiClient.purgeRequestApplication(122349, 1234, "MBI-00000"); // дропшип
        mbiApiClient.purgeRequestApplication(100003, 1234, "MBI-00000"); // фф
    }

    @Test
    @DbUnitDataSet(before = "PrepayRequestControllerTest.dsbs.purge.before.csv",
            after = "PrepayRequestControllerTest.dsbs.purge.after.csv")
    void purgeDsbsPrepay() {
        mbiApiClient.purgeRequestApplication(122349, 1234, "MBI-00000");
    }

    private void mockBalance() {
        when(balanceService.createClient(any(), anyLong(), anyLong())).thenReturn(1L);
        when(balanceService.getClientContracts(eq(1L), any()))
                .thenReturn(Collections.singletonList(ClientContractInfo.fromBalanceStructure(
                        new ClientContractsStructure() {{
                            setId(1);
                            setExternalId("151511/18");
                            setIsActive(1);
                            setIsSuspended(0);
                            setCurrency("RUR");
                        }})));
    }

    /**
     * Протестировать переход в статус COMPLETED из статуса COMPLETED.
     * Проверяем, что ничего не изменится.
     */
    @Test
    @DbUnitDataSet(
            before = "putRequestStatusCompletedToSame.before.csv",
            after = {"putRequestStatusCompletedToSame.before.csv",
                    "prepayRequestControllerFunctionalTestFlag.csv"})
    void putRequestStatusCompletedToSame() {
        mockGetOrCreateMarketId(2014092L);
        mbiApiClient.updatePrepayRequestStatus(
                426892L,
                new RequestStatusForm(PartnerApplicationStatus.COMPLETED, "comment", (List<Long>) null),
                1527891797L);
    }

    /**
     * Тестируем перевод в COMPLETED для нескольких партнеров.
     */
    @Test
    @DbUnitDataSet(
            before = "putRequestStatusCompletedToMany.before.csv",
            after = "putRequestStatusCompletedToMany.after.csv"
    )
    void putRequestStatusCompletedToMany() {
        mockGetOrCreateMarketId(2014092L);
        mockLinkMarketId(2014092L);

        mbiApiClient.updatePrepayRequestStatus(
                426892L,
                new RequestStatusForm(PartnerApplicationStatus.COMPLETED, "comment", (List<Long>) null),
                1527891797);
    }

    /**
     * Тестируем перевод в COMPLETED для самозанятых (есть DONE от ФНС, фича MARKETPLACE включена).
     */
    @Test
    @DbUnitDataSet(
            before = "putRequestStatusCompletedSelfEmployed1.before.csv",
            after = "putRequestStatusCompletedSelfEmployed1.after.csv"
    )
    void putRequestStatusCompletedSelfEmployed1() {
        mockGetOrCreateMarketId(1L);
        mockLinkMarketId(1L);
        mockBalance();

        mbiApiClient.updatePrepayRequestStatus(
                2L,
                new RequestStatusForm(PartnerApplicationStatus.COMPLETED, "comment", (List<Long>) null),
                1527891797);
        verify(mockCheckouterClient, times(1)).pushPartnerSettingsToCheckout(eq(Set.of(1L)));

        verify(balanceService, times(2)).createOrUpdatePerson(
                argThat(p -> p.getOwnershipType() == OwnershipType.SELF_EMPLOYED
                        && "Иванов".equals(p.getLName())
                        && "Иван".equals(p.getFName())
                        && "Иванович".equals(p.getMName())
                        && "some@email.com".equals(p.getEmail())
                        && "+7 916 1234455".equals(p.getPhone())
                        && "jurAddrr".equals(p.getLegalAddress())
                        && "123456789".equals(p.getBik())
                        && "7743880975".equals(p.getInn())
                ),
                anyLong()
        );

        //всего два договора
        verify(balanceService, times(2)).createOffer(
                argThat(o -> o.getString("nds").equals("0")),
                anyLong()
        );
        //один расходный, у котрого выставлен selfemployed = 1
        verify(balanceService, times(1)).createOffer(
                argThat(o -> o.getString("selfemployed").equals("1")),
                anyLong()
        );
    }

    /**
     * Тестируем перевод в COMPLETED для самозанятых (от ФНС не DONE, фича MARKETPLACE отключена).
     */
    @Test
    @DbUnitDataSet(
            before = "putRequestStatusCompletedSelfEmployed2.before.csv",
            after = "putRequestStatusCompletedSelfEmployed2.after.csv"
    )
    void putRequestStatusCompletedSelfEmployed2() {
        mockGetOrCreateMarketId(1L);
        mockLinkMarketId(1L);
        mockBalance();

        mbiApiClient.updatePrepayRequestStatus(
                2L,
                new RequestStatusForm(PartnerApplicationStatus.COMPLETED, "comment", (List<Long>) null),
                1527891797);
    }

    /**
     * Протестировать переход в статус NEED_INFO из статуса COMPLETED.
     * Проверяем, что сбрасывается флаг is_auto_completed.
     */
    @Test
    @DbUnitDataSet(
            before = "putRequestStatusCompletedToNeedInfoAutoFilled.before.csv",
            after = "putRequestStatusCompletedToNeedInfoAutoFilled.after.csv")
    void putRequestStatusCompletedToNeedInfoAutoFilled() {
        mockGetOrCreateMarketId(2014092L);
        mbiApiClient.updatePrepayRequestStatus(
                426892L,
                new RequestStatusForm(PartnerApplicationStatus.NEED_INFO, "comment", (List<Long>) null),
                1527891797L);
    }

    @Test
    @DbUnitDataSet(
            before = "csv/delivery-partner-application-completed-empty-contacts.before.csv",
            after = "csv/delivery-partner-application-completed.after.csv"
    )
    @DisplayName("Выставление статуса Complete c пустыми контактами")
    void completeWithEmptyContacts() {
        doNothing().when(persistentAsyncCheckouterService).handlePendingTasks(anySet());
        mockGetOrCreateMarketId(2000000);
        mbiApiClient.updatePrepayRequestStatus(
                4L,
                new RequestStatusForm(PartnerApplicationStatus.COMPLETED, "comment", (List<Long>) null),
                1010101L
        );
    }

    @Test
    @DbUnitDataSet(
            before = "csv/delivery-partner-application-completed-empty-contacts.before.csv",
            after = "csv/delivery-partner-application-need-info.after.csv"
    )
    @DisplayName("Выставление статуса NEED_INFO c пустыми контактами")
    void declineWithEmptyContacts() {
        doNothing().when(persistentAsyncCheckouterService).handlePendingTasks(anySet());
        mockGetOrCreateMarketId(2000000);
        mbiApiClient.updatePrepayRequestStatus(
                4L,
                new RequestStatusForm(PartnerApplicationStatus.NEED_INFO, "comment", (List<Long>) null),
                1010101L
        );
    }

    @Test
    @DbUnitDataSet(
            before = "csv/delivery-partner-application-completed-empty-contacts.before.csv",
            after = "csv/delivery-partner-application-decline.after.csv"
    )
    @DisplayName("Выставление статуса DECLINED c пустыми контактами")
    void needInfoWithEmptyContacts() {
        doNothing().when(persistentAsyncCheckouterService).handlePendingTasks(anySet());
        mockGetOrCreateMarketId(2000000);
        mbiApiClient.updatePrepayRequestStatus(
                4L,
                new RequestStatusForm(PartnerApplicationStatus.DECLINED, "comment", (List<Long>) null),
                1010101L
        );
    }

    @Test
    @DbUnitDataSet(
            before = "csv/delivery-partner-application-completed-empty-contacts.before.csv",
            after = "csv/delivery-partner-application-frozen.after.csv"
    )
    @DisplayName("Выставление статуса FROZEN c пустыми контактами")
    void frozenWithEmptyContacts() {
        doNothing().when(persistentAsyncCheckouterService).handlePendingTasks(anySet());
        mockGetOrCreateMarketId(2000000);
        mbiApiClient.updatePrepayRequestStatus(
                4L,
                new RequestStatusForm(PartnerApplicationStatus.FROZEN, "comment", List.of(400L)),
                1010101L
        );
    }

    private PrepayRequestDTO createExpectedPrepayRequestWithNames() {
        PrepayRequestDTO prepayRequestDTO = new PrepayRequestDTO();
        prepayRequestDTO.setRequestId(4L);
        prepayRequestDTO.setDatasourceIds(Collections.singletonList(400L));
        prepayRequestDTO.setStatus(PartnerApplicationStatus.IN_PROGRESS);
        prepayRequestDTO.setPrepayType(PrepayTypeDTO.UNKNOWN);
        prepayRequestDTO.setRequestType(RequestType.MARKETPLACE);
        prepayRequestDTO.setUpdatedAt(Instant.ofEpochSecond(1525573800));
        prepayRequestDTO.setDocuments(Collections.emptyList());

        ContactInfoDTO contactInfoDTO = ContactInfoDTO.builder()
                .name("Сережа Молодец")
                .lastName("Молодец")
                .firstName("Сережа")
                .build();
        prepayRequestDTO.setContactInfo(contactInfoDTO);

        SignatoryInfoDTO signatoryInfoDTO = SignatoryInfoDTO.builder()
                .name("Вера Владимировна Павлова")
                .firstName("Вера")
                .secondName("Владимировна")
                .lastName("Павлова")
                .build();

        prepayRequestDTO.setSignatoryInfo(signatoryInfoDTO);
        prepayRequestDTO.setSellerClientId(100500L);
        prepayRequestDTO.setPersonId(10L);
        prepayRequestDTO.setMarketId(112358L);

        return prepayRequestDTO;
    }

    private AssessorRequestForm prepareForm() {
        AssessorRequestForm form = new AssessorRequestForm();
        form.setDatasourceId(500L);
        AssessorRequestForm.ContactInfo contactInfo = new AssessorRequestForm.ContactInfo();
        contactInfo.setEmail("test@mail.ru");
        contactInfo.setName("Тяпкин Владимир");
        contactInfo.setFirstName("Владимир");
        contactInfo.setLastName("Тяпкин");
        contactInfo.setPhoneNumber("+74991234567");
        contactInfo.setShopPhoneNumber("+74991234568");
        form.setContactInfo(contactInfo);
        return form;
    }

    private AssessorRequestForm prepareFullForm() {
        AssessorRequestForm form = prepareForm();
        AssessorRequestForm.OrganizationInfo organizationInfo = new AssessorRequestForm.OrganizationInfo();
        organizationInfo.setOrganizationName("DMF");
        organizationInfo.setAccountNumber("12345678901234567890");
        organizationInfo.setType(OrganizationType.OAO);
        organizationInfo.setInn("123456789012");
        organizationInfo.setFactAddress("3я улица Строителей");
        organizationInfo.setBik("123456789");
        organizationInfo.setBankName("ВТБ");
        organizationInfo.setOgrn("4455563445");
        organizationInfo.setJuridicalAddress("ул  Мира дом 3");
        organizationInfo.setCorrAccountNumber("09876543210987654321");
        organizationInfo.setLicenseNumber("123123");
        organizationInfo.setKpp("987654321");
        organizationInfo.setPostcode("122323");
        organizationInfo.setWorkSchedule("Пон-Пятн с 9-20");
        AssessorRequestForm.SignatoryInfo signatoryInfo = new AssessorRequestForm.SignatoryInfo();
        signatoryInfo.setName("Петров Иван");
        signatoryInfo.setFirstName("Иван");
        signatoryInfo.setLastName("Петров");
        signatoryInfo.setSignatoryGender(Gender.MALE);
        signatoryInfo.setDocInfo("Инфа по документу");
        signatoryInfo.setDocType(SignatoryDocType.PROTOCOL);
        signatoryInfo.setPosition("Директор");
        form.setSignatoryInfo(signatoryInfo);
        form.setOrganizationInfo(organizationInfo);
        return form;
    }

    private PrepayRequestDTO expectedRequestDTO(RequestType requestType) {
        OrganizationInfoDTO organizationInfoDTO = OrganizationInfoDTO.builder()
                .juridicalAddress("Moscow")
                .build();

        PrepayRequestDTO prepayRequestDTO = new PrepayRequestDTO();
        prepayRequestDTO.setRequestId(1L);
        prepayRequestDTO.setDatasourceIds(Collections.singletonList(100L));
        prepayRequestDTO.setStatus(PartnerApplicationStatus.INIT);
        prepayRequestDTO.setPrepayType(PrepayTypeDTO.UNKNOWN);
        prepayRequestDTO.setRequestType(requestType);
        prepayRequestDTO.setSellerClientId(100500L);
        prepayRequestDTO.setPersonId(1L);
        prepayRequestDTO.setContractId(11L);
        prepayRequestDTO.setMarketId(123321L);
        prepayRequestDTO.setUpdatedAt(Instant.ofEpochSecond(1529368800));
        prepayRequestDTO.setDocuments(Collections.emptyList());
        prepayRequestDTO.setOrganizationInfo(organizationInfoDTO);
        return prepayRequestDTO;
    }

    private PrepayRequestDTO expectedSelfEmployedRequestDTO(RequestType requestType) {
        OrganizationInfoDTO organizationInfoDTO = OrganizationInfoDTO.builder()
                .type(OrganizationType.PHYSIC)
                .name("samoZad")
                .build();

        PrepayRequestDTO prepayRequestDTO = new PrepayRequestDTO();
        prepayRequestDTO.setRequestId(5L);
        prepayRequestDTO.setDatasourceIds(Collections.singletonList(301L));
        prepayRequestDTO.setStatus(PartnerApplicationStatus.INIT);
        prepayRequestDTO.setPrepayType(PrepayTypeDTO.UNKNOWN);
        prepayRequestDTO.setRequestType(requestType);
        prepayRequestDTO.setSellerClientId(null);
        prepayRequestDTO.setPersonId(null);
        prepayRequestDTO.setContractId(null);
        prepayRequestDTO.setMarketId(null);
        prepayRequestDTO.setUpdatedAt(Instant.ofEpochSecond(1529368800));
        prepayRequestDTO.setDocuments(
                List.of(
                        new PrepayRequestDocumentDTO(
                                1,
                                PartnerApplicationDocumentType.SIGNATORY_DOC,
                                "doc1",
                                8374,
                                Instant.ofEpochSecond(1483218000),
                                "someurl1"
                        ),
                        new PrepayRequestDocumentDTO(
                                2,
                                PartnerApplicationDocumentType.SIGNATORY_DOC,
                                "doc2",
                                8374,
                                Instant.ofEpochSecond(1483218000),
                                "someurl2"
                        )
                )
        );
        prepayRequestDTO.setOrganizationInfo(organizationInfoDTO);
        return prepayRequestDTO;
    }

    private CrossborderOrganizationInfoDTO expectedCrossborderPaymentSystemRequestDto() {
        CrossborderOrganizationInfoDTO crossborderOrganizationInfo = new CrossborderOrganizationInfoDTO();
        crossborderOrganizationInfo.setOrganizationNameOriginal("organizationNameOriginal text");
        crossborderOrganizationInfo.setOrganizationNameEnglish("organizationNameEnglish text");
        crossborderOrganizationInfo.setPhysicalAddress("physicalAddress text");
        crossborderOrganizationInfo.setRegistrationNumber("registrationNumber text");
        crossborderOrganizationInfo.setLegalAddress("legalAddress text");
        crossborderOrganizationInfo.setRepresentativeFullName("representativeFullName text");
        crossborderOrganizationInfo.setRepresentativeIdCardNumber("representativeIdNumber text");
        crossborderOrganizationInfo.setRepresentativeAddress("representativeAddress text");
        crossborderOrganizationInfo.setPostcode("postcode text");
        crossborderOrganizationInfo.setContractCurrency(Currency.USD);
        crossborderOrganizationInfo.setBrandName("brandName text");
        crossborderOrganizationInfo.setPaymentMethod(CrossborderPaymentMethod.PAYONEER);
        crossborderOrganizationInfo.setPayeeId("abc123");
        return crossborderOrganizationInfo;
    }

    private AssessorRequestForm prepareFormWithForbiddenSymbol() {
        AssessorRequestForm form = prepareForm();
        AssessorRequestForm.OrganizationInfo organizationInfo = new AssessorRequestForm.OrganizationInfo();
        organizationInfo.setOrganizationName("Меня зовут Я\r");
        organizationInfo.setBankName("ВТБ\r");
        organizationInfo.setOgrn("4455563445\n");
        organizationInfo.setJuridicalAddress("ул \n Мира дом 3");
        organizationInfo.setLicenseNumber("123123\n");
        organizationInfo.setKpp("98765432\r");
        organizationInfo.setWorkSchedule("Пон-Пятн с 9-20\n");
        form.setOrganizationInfo(organizationInfo);
        return form;
    }
}
