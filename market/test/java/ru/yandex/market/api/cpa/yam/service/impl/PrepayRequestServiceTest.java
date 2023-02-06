package ru.yandex.market.api.cpa.yam.service.impl;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.context.ApplicationEventPublisher;

import ru.yandex.common.geocoder.client.GeoClient;
import ru.yandex.common.geocoder.model.response.AddressInfo;
import ru.yandex.common.geocoder.model.response.AreaInfo;
import ru.yandex.common.geocoder.model.response.Boundary;
import ru.yandex.common.geocoder.model.response.CountryInfo;
import ru.yandex.common.geocoder.model.response.GeoObject;
import ru.yandex.common.geocoder.model.response.LocalityInfo;
import ru.yandex.common.geocoder.model.response.SimpleGeoObject;
import ru.yandex.common.geocoder.model.response.ToponymInfo;
import ru.yandex.market.api.cpa.CPAPlacementService;
import ru.yandex.market.api.cpa.CPAPrepaymentType;
import ru.yandex.market.api.cpa.yam.dao.PrepayRequestDao;
import ru.yandex.market.api.cpa.yam.dao.PrepayRequestDocumentDao;
import ru.yandex.market.api.cpa.yam.dto.AssessorRequestForm;
import ru.yandex.market.api.cpa.yam.entity.AvailabilityStatus;
import ru.yandex.market.api.cpa.yam.entity.Gender;
import ru.yandex.market.api.cpa.yam.entity.PrepayRequest;
import ru.yandex.market.api.cpa.yam.entity.RequestType;
import ru.yandex.market.api.cpa.yam.service.PrepayRequestBalanceHelperService;
import ru.yandex.market.api.cpa.yam.service.PrepayRequestHistoryService;
import ru.yandex.market.api.cpa.yam.service.PrepayRequestService;
import ru.yandex.market.api.cpa.yam.service.PrepayRequestValidatorService;
import ru.yandex.market.checkout.checkouter.shop.PaymentClass;
import ru.yandex.market.checkout.checkouter.shop.PrepayType;
import ru.yandex.market.core.application.PartnerApplicationStatus;
import ru.yandex.market.core.application.business.repository.BusinessPartnerApplicationDAO;
import ru.yandex.market.core.application.meta.PartnerApplicationBalanceDataDAO;
import ru.yandex.market.core.application.meta.PartnerApplicationContactDAO;
import ru.yandex.market.core.application.meta.PartnerApplicationConverter;
import ru.yandex.market.core.application.meta.PartnerApplicationDAO;
import ru.yandex.market.core.application.meta.PartnerDocumentDAO;
import ru.yandex.market.core.application.meta.impl.OrganizationInfoSyncServiceImpl;
import ru.yandex.market.core.application.selfemployed.SelfEmployedApplicationService;
import ru.yandex.market.core.campaign.model.CampaignType;
import ru.yandex.market.core.geocoder.RegionIdFetcher;
import ru.yandex.market.core.history.HistoryService;
import ru.yandex.market.core.orginfo.OrganizationInfoService;
import ru.yandex.market.core.orginfo.model.OrganizationInfo;
import ru.yandex.market.core.orginfo.model.OrganizationInfoSource;
import ru.yandex.market.core.param.ParamService;
import ru.yandex.market.core.param.model.ParamCheckStatus;
import ru.yandex.market.core.param.model.ParamType;
import ru.yandex.market.core.param.model.StringParamValue;
import ru.yandex.market.core.partner.PartnerLinkService;
import ru.yandex.market.core.partner.PartnerService;
import ru.yandex.market.core.partner.PartnerTypeAwareService;
import ru.yandex.market.core.partner.contract.PartnerContractService;
import ru.yandex.market.core.partner.model.PartnerInfo;
import ru.yandex.market.core.protocol.MockProtocolService;
import ru.yandex.market.core.protocol.ProtocolService;
import ru.yandex.market.core.protocol.model.ActionType;
import ru.yandex.market.core.protocol.model.UIDActionContext;
import ru.yandex.market.mbi.lock.LockService;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit тесты для {@link PrepayRequestServiceImpl}.
 *
 * @author avetokhin 14/04/17.
 */
public class PrepayRequestServiceTest {

    private static final long ACTION_ID = 9999L;
    private static final long UID = 10000L;

    private static final long DS_ID_1 = 10;
    private static final long DS_ID_2 = 11;
    private static final long DS_ID_3 = 12;
    private static final long DS_ID_4 = 13;
    private static final long DS_ID_5 = 14;
    private static final long DS_ID_6 = 15;
    private static final long DS_ID_7 = 16;
    private static final long DS_ID_8 = 17;

    private static final long REQ_ID_1 = 1;
    private static final long REQ_ID_2 = 2;
    private static final long REQ_ID_3 = 3;
    private static final long REQ_ID_4 = 4;
    private static final long REQ_ID_5 = 5;
    private static final long REQ_ID_6 = 6;

    private static final UIDActionContext UPDATE_ACTION_CONTEXT =
            new UIDActionContext(ActionType.PREPAY_REQUEST_UPDATE, UID);

    @Mock
    private PrepayRequestDao prepayRequestDao;

    @Mock
    private PartnerApplicationBalanceDataDAO partnerApplicationBalanceDataDAO;

    @Mock
    private PartnerApplicationDAO partnerApplicationDAO;

    @Mock
    private BusinessPartnerApplicationDAO businessPartnerApplicationDAO;

    @Mock
    private PartnerApplicationContactDAO partnerApplicationContactDAO;

    @Mock
    private PrepayRequestDocumentDao prepayRequestDocumentDao;

    @Mock
    private PartnerDocumentDAO partnerDocumentDAO;

    private ProtocolService protocolService = new MockProtocolService();

    @Mock
    private OrganizationInfoService organizationInfoService;

    @Mock
    private CPAPlacementService cpaPlacementService;

    @Mock
    private ParamService paramService;

    @Mock
    private PrepayRequestBalanceHelperService balanceHelperService;

    @Mock
    private PrepayRequestHistoryService requestHistoryService;

    @Mock
    private HistoryService historyService;

    @Mock
    private PrepayRequestValidatorService validatorService;

    @Mock
    private PartnerContractService supplierContractService;

    @Mock
    private LockService lockService;

    private PrepayRequestService service;

    @Captor
    private ArgumentCaptor<List<PrepayRequest>> prepayRequestArgumentCaptor;

    @Captor
    private ArgumentCaptor<OrganizationInfo> organizationInfoArgumentCaptor;

    @Mock
    private GeoClient geoClient;

    @Mock
    private PartnerLinkService partnerLinkService;

    @Mock
    private PartnerService partnerService;

    @Mock
    private PartnerTypeAwareService partnerTypeAwareService;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @Mock
    private SelfEmployedApplicationService selfEmployedApplicationService;

    private static CPAPrepaymentType sbxPaymentType(final PaymentClass paymentClass) {
        return new CPAPrepaymentType(paymentClass, null);
    }

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(partnerLinkService.getDonorPartnerId(anyLong())).thenReturn(null);

        service = Mockito.spy(new PrepayRequestServiceImpl(
                prepayRequestDao,
                prepayRequestDocumentDao,
                partnerDocumentDAO,
                protocolService,
                new OrganizationInfoSyncServiceImpl(organizationInfoService),
                cpaPlacementService,
                paramService,
                balanceHelperService,
                supplierContractService,
                requestHistoryService,
                historyService,
                validatorService,
                lockService,
                new AssessorFormUpdateService(),
                new RegionIdFetcher(geoClient),
                partnerApplicationDAO,
                businessPartnerApplicationDAO,
                partnerApplicationContactDAO,
                partnerApplicationBalanceDataDAO,
                partnerLinkService,
                partnerTypeAwareService,
                partnerService,
                applicationEventPublisher,
                selfEmployedApplicationService));
    }

    /**
     * Проверить логику получения статусов доступности предоплаты.
     */
    @Test
    public void getAvailabilityStatuses() {
        // Новая непроверенная заявка, YANDEX
        final PrepayRequest request1 = new PrepayRequest(REQ_ID_1, PrepayType.YANDEX_MARKET,
                PartnerApplicationStatus.NEW, DS_ID_1);
        when(cpaPlacementService.getCPAPaymentType(DS_ID_1)).thenReturn(sbxPaymentType(PaymentClass.YANDEX));

        // Новая проверенная заявка, YANDEX
        final PrepayRequest request2 = new PrepayRequest(REQ_ID_2, PrepayType.YANDEX_MARKET,
                PartnerApplicationStatus.COMPLETED, DS_ID_2);
        when(cpaPlacementService.getCPAPaymentType(DS_ID_2)).thenReturn(sbxPaymentType(PaymentClass.YANDEX));

        // Старая ЯДовская проверенная заявка, YANDEX
        final PrepayRequest request3 = new PrepayRequest(REQ_ID_3, PrepayType.YANDEX_MONEY,
                PartnerApplicationStatus.COMPLETED, DS_ID_3);
        when(cpaPlacementService.getCPAPaymentType(DS_ID_3)).thenReturn(sbxPaymentType(PaymentClass.YANDEX));

        // Старая ЯДовская непроверенная заявка, GLOBAL
        final PrepayRequest request4 = new PrepayRequest(REQ_ID_4, PrepayType.YANDEX_MONEY,
                PartnerApplicationStatus.NEW, DS_ID_4);
        when(cpaPlacementService.getCPAPaymentType(DS_ID_4)).thenReturn(sbxPaymentType(PaymentClass.GLOBAL));

        // Старая ЯДовская проверенная заявка, GLOBAL
        final PrepayRequest request5 = new PrepayRequest(REQ_ID_5, PrepayType.YANDEX_MONEY,
                PartnerApplicationStatus.COMPLETED, DS_ID_5);
        when(cpaPlacementService.getCPAPaymentType(DS_ID_5)).thenReturn(sbxPaymentType(PaymentClass.GLOBAL));

        // Новая проверенная заявка, OFF
        final PrepayRequest request6 = new PrepayRequest(REQ_ID_6, PrepayType.YANDEX_MARKET,
                PartnerApplicationStatus.COMPLETED, DS_ID_6);
        when(cpaPlacementService.getCPAPaymentType(DS_ID_6)).thenReturn(sbxPaymentType(PaymentClass.OFF));

        // Новая проверенная заявка, UNKNOWN
        final PrepayRequest request7 = new PrepayRequest(REQ_ID_6, PrepayType.YANDEX_MARKET,
                PartnerApplicationStatus.COMPLETED, DS_ID_7);
        when(cpaPlacementService.getCPAPaymentType(DS_ID_7)).thenReturn(sbxPaymentType(PaymentClass.UNKNOWN));

        // Новая DECLINED заявка, YANDEX
        final PrepayRequest request8 = new PrepayRequest(REQ_ID_6, PrepayType.YANDEX_MARKET,
                PartnerApplicationStatus.DECLINED, DS_ID_8);
        when(cpaPlacementService.getCPAPaymentType(DS_ID_8)).thenReturn(sbxPaymentType(PaymentClass.YANDEX));

        List<Long> datasourceIds = Arrays.asList(DS_ID_1, DS_ID_2, DS_ID_3, DS_ID_4,
                DS_ID_5, DS_ID_6, DS_ID_7, DS_ID_8);
        Map<Long, CPAPrepaymentType> prepaymentTypes = datasourceIds.stream().collect(Collectors.toMap(
                Function.identity(),
                id -> cpaPlacementService.getCPAPaymentType(id)
        ));

        when(cpaPlacementService.getCPAPaymentTypes(eq(datasourceIds))).thenReturn(prepaymentTypes);

        when(prepayRequestDao.find(any()))
                .thenReturn(Arrays.asList(request1, request2, request3, request4, request5, request6, request7,
                        request8));

        final Map<Long, AvailabilityStatus> availabilityStatuses =
                service.getAvailabilityStatuses(datasourceIds);

        assertThat(availabilityStatuses, notNullValue());
        assertThat(availabilityStatuses.get(DS_ID_1), equalTo(AvailabilityStatus.APPLIED));
        assertThat(availabilityStatuses.get(DS_ID_2), equalTo(AvailabilityStatus.APPLIED));
        assertThat(availabilityStatuses.get(DS_ID_3), equalTo(AvailabilityStatus.AVAILABLE));
        assertThat(availabilityStatuses.get(DS_ID_4), equalTo(AvailabilityStatus.APPLIED));
        assertThat(availabilityStatuses.get(DS_ID_5), equalTo(AvailabilityStatus.APPLIED));
        assertThat(availabilityStatuses.get(DS_ID_6), equalTo(AvailabilityStatus.NOT_AVAILABLE));
        assertThat(availabilityStatuses.get(DS_ID_7), equalTo(AvailabilityStatus.NOT_AVAILABLE));
        assertThat(availabilityStatuses.get(DS_ID_8), equalTo(AvailabilityStatus.NOT_AVAILABLE));
    }

    /**
     * Проверяем, что сервис дергает баланс и organizationService при изменении статуса заявки на COMPLETED.
     */
    @Test
    public void updateRequestStatusToCompleted() {
        final PrepayRequest request = new PrepayRequest(REQ_ID_1, PrepayType.YANDEX_MARKET,
                PartnerApplicationStatus.IN_PROGRESS, DS_ID_1);

        final List<PrepayRequest> requests = Collections.singletonList(request);
        when(prepayRequestDao.find(any())).thenReturn(requests);

        service.updateRequestStatus(
                REQ_ID_1,
                PartnerApplicationStatus.COMPLETED,
                null,
                null,
                null,
                UPDATE_ACTION_CONTEXT.getActorId(),
                ACTION_ID);

        verify(supplierContractService).createContracts(anyLong(), anyLong(), eq(request), eq(false));
    }


    @Test
    public void test_updateRequestStatus_when_toCompletedAndNewMechanics_then_callSigningHelper() {
        //новая механика работы с программами

        final List<PrepayRequest> requests = Collections.singletonList(
                new PrepayRequest(REQ_ID_1, PrepayType.YANDEX_MARKET, PartnerApplicationStatus.IN_PROGRESS, DS_ID_1)
        );
        when(prepayRequestDao.find(any())).thenReturn(requests);
        when(partnerService.getPartners(eq(Set.of(DS_ID_1)))).thenReturn(List.of(new PartnerInfo(DS_ID_1, CampaignType.SHOP, -2L)));

        service.updateRequestStatus(
                REQ_ID_1,
                PartnerApplicationStatus.COMPLETED,
                null,
                null,
                null,
                UPDATE_ACTION_CONTEXT.getActorId(),
                ACTION_ID);

        verify(validatorService).checkStatusChangeAllowed(PartnerApplicationStatus.IN_PROGRESS,
                PartnerApplicationStatus.COMPLETED);
        verify(organizationInfoService).createOrUpdateOrganizationInfo(any(), anyLong());
    }

    /**
     * Проверяем, что при изменении статуса заявки на COMPLETED для поставщика дергается соответствующий сервис.
     */
    @Test
    public void updateRequestStatusToCompletedForSupplier() {
        final PrepayRequest request =
                new PrepayRequest(REQ_ID_1, PrepayType.YANDEX_MARKET, PartnerApplicationStatus.IN_PROGRESS, DS_ID_1);
        request.setRequestType(RequestType.MARKETPLACE);

        final List<PrepayRequest> requests = Collections.singletonList(request);
        when(prepayRequestDao.find(any())).thenReturn(requests);

        service.updateRequestStatus(
                REQ_ID_1,
                PartnerApplicationStatus.COMPLETED,
                null,
                null,
                null,
                UPDATE_ACTION_CONTEXT.getActorId(),
                ACTION_ID);
        verify(partnerApplicationDAO, atMost(2))
                .updateStatus(PartnerApplicationConverter.convert(requests));
        verify(supplierContractService).createContracts(anyLong(), anyLong(), eq(request), eq(false));
        verify(validatorService).checkStatusChangeAllowed(PartnerApplicationStatus.IN_PROGRESS,
                PartnerApplicationStatus.COMPLETED);
        verify(organizationInfoService, never()).createOrUpdateOrganizationInfo(any(), anyLong());
    }

    /**
     * Проверяем, что при изменении статуса заявки на COMPLETED для поставщика при выключенной
     * опции создания договоров меняется только статус заявки.
     */
    @Test
    public void updateRequestStatusToCompletedForSupplierWithoutContractsCreation() {
        final PrepayRequest request =
                new PrepayRequest(REQ_ID_1, PrepayType.YANDEX_MARKET, PartnerApplicationStatus.IN_PROGRESS, DS_ID_1);
        request.setRequestType(RequestType.MARKETPLACE);

        final List<PrepayRequest> requests = Collections.singletonList(request);
        when(prepayRequestDao.find(any())).thenReturn(requests);

        service.updateRequestStatus(
                REQ_ID_1,
                PartnerApplicationStatus.COMPLETED,
                null,
                null,
                null,
                UPDATE_ACTION_CONTEXT.getActorId(),
                ACTION_ID);

        verify(partnerApplicationDAO, atMost(2))
                .updateStatus(PartnerApplicationConverter.convert(requests));
        verify(supplierContractService, times(1))
                .createContracts(anyLong(), anyLong(), eq(request), eq(false));
        verify(validatorService).checkStatusChangeAllowed(PartnerApplicationStatus.IN_PROGRESS,
                PartnerApplicationStatus.COMPLETED);
        verify(organizationInfoService, never()).createOrUpdateOrganizationInfo(any(), anyLong());
    }

    @Test
    public void updateRequestStatusToInit() {
        final PrepayRequest request1 = new PrepayRequest(REQ_ID_1, PrepayType.YANDEX_MARKET,
                PartnerApplicationStatus.NEW, DS_ID_1);
        final PrepayRequest request2 = new PrepayRequest(REQ_ID_1, PrepayType.YANDEX_MARKET,
                PartnerApplicationStatus.NEW, DS_ID_2);
        final PrepayRequest request3 = new PrepayRequest(REQ_ID_1, PrepayType.YANDEX_MARKET,
                PartnerApplicationStatus.NEW, DS_ID_3);

        final PrepayRequest request4 = new PrepayRequest(REQ_ID_2, PrepayType.YANDEX_MARKET,
                PartnerApplicationStatus.COMPLETED, DS_ID_1);
        final PrepayRequest request5 = new PrepayRequest(REQ_ID_2, PrepayType.YANDEX_MARKET,
                PartnerApplicationStatus.FROZEN, DS_ID_2);
        request5.setUpdatedAt(Instant.now().minus(2, ChronoUnit.DAYS));

        final List<PrepayRequest> requestsForUpdate = Arrays.asList(request1, request2, request3);
        final List<PrepayRequest> requestsVerifyPaymentStatus = Arrays.asList(request4, request5);
        // Два раза requestsForUpdate из-за double check'а после лока на клиенте
        when(prepayRequestDao.find(any()))
                .thenReturn(requestsForUpdate)
                .thenReturn(requestsForUpdate)
                .thenReturn(requestsVerifyPaymentStatus);

        service.updateRequestStatus(
                REQ_ID_1,
                PartnerApplicationStatus.INIT,
                null,
                null,
                null,
                UPDATE_ACTION_CONTEXT.getActorId(),
                ACTION_ID);

        verify(validatorService).checkStatusChangeAllowed(PartnerApplicationStatus.NEW, PartnerApplicationStatus.INIT);
        verify(validatorService).getClientIdsWithCheck(eq(Stream.of(DS_ID_1, DS_ID_2, DS_ID_3).collect(Collectors.toSet())));
        verify(validatorService).checkRequestIsFullFilled(request1);

        // Сброс КЗ.
        verify(paramService).getParamStringValue(ParamType.PAYMENT_CHECK_STATUS, DS_ID_1);
        verify(paramService).getParamStringValue(ParamType.PAYMENT_CHECK_STATUS, DS_ID_2);
        verify(paramService).getParamStringValue(ParamType.PAYMENT_CHECK_STATUS, DS_ID_3);
        verify(paramService).setParam(eq(new StringParamValue(ParamType.PAYMENT_CHECK_STATUS, DS_ID_3,
                ParamCheckStatus.NEW.name())), anyLong());
        verify(paramService).getParamBooleanValue(ParamType.IS_SELF_EMPLOYED, DS_ID_1, false);
        verifyNoMoreInteractions(paramService);
    }

    private Optional<GeoObject> createGeoObject(String geoId) {
        return Optional.of(SimpleGeoObject.newBuilder()
                .withToponymInfo(ToponymInfo.newBuilder().withGeoid(geoId).build())
                .withBoundary(Boundary.newBuilder().build())
                .withAddressInfo(AddressInfo.newBuilder()
                        .withCountryInfo(CountryInfo.newBuilder().build())
                        .withAreaInfo(AreaInfo.newBuilder().build())
                        .withLocalityInfo(LocalityInfo.newBuilder().build())
                        .build()
                )
                .build()
        );
    }

    private PrepayRequest currentPrepayRequest() {
        PrepayRequest currentPrepayRequest = new PrepayRequest(REQ_ID_1, PrepayType.YANDEX_MARKET,
                PartnerApplicationStatus.COMPLETED, DS_ID_1);

        currentPrepayRequest.setFactAddress("currentFactAddress");
        currentPrepayRequest.setFactAddressRegionId(42L);
        currentPrepayRequest.setJurAddress("currentJurAddress");
        currentPrepayRequest.setAccountNumber("currentAccountNumber");
        currentPrepayRequest.setBankName("currentBankName");
        currentPrepayRequest.setBik("currentBik");
        currentPrepayRequest.setLicenseNum("currentLicenseNumber");
        currentPrepayRequest.setLicenseDate(Instant.now());
        currentPrepayRequest.setEmail("currentEmail");
        currentPrepayRequest.setContactPerson("currentName");
        currentPrepayRequest.setPhoneNumber("currentPhoneNumber");
        currentPrepayRequest.setRequestType(RequestType.MARKETPLACE);

        return currentPrepayRequest;
    }

    private Map<Long, OrganizationInfo> currentOrganizationInfos(Long datasourceId) {
        OrganizationInfo currentOrganizationInfo = new OrganizationInfo();
        currentOrganizationInfo.setInfoSource(OrganizationInfoSource.YANDEX_MARKET);
        currentOrganizationInfo.setJuridicalAddress("currentJurAddress");

        return ImmutableMap.of(datasourceId, currentOrganizationInfo);
    }

    private AssessorRequestForm requestForm(Long requestId) {
        AssessorRequestForm assessorRequestForm = new AssessorRequestForm();
        assessorRequestForm.setDatasourceId(requestId);

        AssessorRequestForm.OrganizationInfo newOrganizationInfo = new AssessorRequestForm.OrganizationInfo();
        newOrganizationInfo.setFactAddress("newFactAddress");
        newOrganizationInfo.setJuridicalAddress("newJurAddress");
        newOrganizationInfo.setAccountNumber("newAccountNumber");
        newOrganizationInfo.setBankName("newBankName");
        newOrganizationInfo.setBik("newBik");
        newOrganizationInfo.setLicenseNumber("newLicenseNumber");
        newOrganizationInfo.setLicenseDate(LocalDate.now());
        newOrganizationInfo.setWorkSchedule("newWorkSchedule");

        AssessorRequestForm.ContactInfo newContactInfo = new AssessorRequestForm.ContactInfo();
        newContactInfo.setEmail("newEmail");
        newContactInfo.setName("newName");
        newContactInfo.setFirstName("newName");
        newContactInfo.setPhoneNumber("newPhoneNumber");

        AssessorRequestForm.SignatoryInfo signatoryInfo = new AssessorRequestForm.SignatoryInfo();
        signatoryInfo.setSignatoryGender(Gender.MALE);

        assessorRequestForm.setOrganizationInfo(newOrganizationInfo);
        assessorRequestForm.setContactInfo(newContactInfo);
        assessorRequestForm.setSignatoryInfo(signatoryInfo);

        return assessorRequestForm;
    }

}
