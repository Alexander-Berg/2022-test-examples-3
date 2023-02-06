package ru.yandex.travel.hotels.administrator;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import io.grpc.stub.StreamObserver;
import org.assertj.core.api.Condition;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.MockBeans;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.support.TransactionTemplate;
import yandex.maps.proto.common2.geo_object.GeoObjectOuterClass;

import ru.yandex.travel.hotels.administrator.cache.HotelClusteringDictionary;
import ru.yandex.travel.hotels.administrator.entity.Agreement;
import ru.yandex.travel.hotels.administrator.entity.AgreementType;
import ru.yandex.travel.hotels.administrator.entity.BillingRegistration;
import ru.yandex.travel.hotels.administrator.entity.CallGeoSearchStep;
import ru.yandex.travel.hotels.administrator.entity.Commission;
import ru.yandex.travel.hotels.administrator.entity.ConnectionStep;
import ru.yandex.travel.hotels.administrator.entity.HotelConnection;
import ru.yandex.travel.hotels.administrator.entity.HotelConnectionStep;
import ru.yandex.travel.hotels.administrator.entity.HotelConnectionUpdate;
import ru.yandex.travel.hotels.administrator.entity.HotelTaxType;
import ru.yandex.travel.hotels.administrator.entity.LegalDetails;
import ru.yandex.travel.hotels.administrator.entity.LegalDetailsConnectionStep;
import ru.yandex.travel.hotels.administrator.entity.LegalDetailsUpdate;
import ru.yandex.travel.hotels.administrator.entity.LegalDetailsUpdateState;
import ru.yandex.travel.hotels.administrator.entity.RegisterInBalanceStep;
import ru.yandex.travel.hotels.administrator.entity.VerifyClusteringStep;
import ru.yandex.travel.hotels.administrator.grpc.proto.TAcceptConnectionUpdateReq;
import ru.yandex.travel.hotels.administrator.grpc.proto.THotelDetailsChangedReq;
import ru.yandex.travel.hotels.administrator.grpc.proto.TRejectConnectionUpdateReq;
import ru.yandex.travel.hotels.administrator.repository.AgreementRepository;
import ru.yandex.travel.hotels.administrator.repository.BillingRegistrationRepository;
import ru.yandex.travel.hotels.administrator.repository.CommissionRepository;
import ru.yandex.travel.hotels.administrator.repository.ConnectionStepRepository;
import ru.yandex.travel.hotels.administrator.repository.HotelConnectionRepository;
import ru.yandex.travel.hotels.administrator.repository.HotelConnectionUpdateRepository;
import ru.yandex.travel.hotels.administrator.repository.LegalDetailsRepository;
import ru.yandex.travel.hotels.administrator.repository.LegalDetailsUpdateRepository;
import ru.yandex.travel.hotels.administrator.service.AddressUnificationService;
import ru.yandex.travel.hotels.administrator.service.ClusterizationService;
import ru.yandex.travel.hotels.administrator.service.StarTrekService;
import ru.yandex.travel.hotels.administrator.service.UpdateResult;
import ru.yandex.travel.hotels.administrator.task.HotelsYtPublishTask;
import ru.yandex.travel.hotels.administrator.task.PermalinkUpdateTask;
import ru.yandex.travel.hotels.administrator.workflow.proto.EBillingRegistrationState;
import ru.yandex.travel.hotels.administrator.workflow.proto.EConnectionStepState;
import ru.yandex.travel.hotels.administrator.workflow.proto.EHotelConnectionState;
import ru.yandex.travel.hotels.administrator.workflow.proto.EHotelConnectionUpdateState;
import ru.yandex.travel.hotels.administrator.workflow.proto.ELegalDetailsState;
import ru.yandex.travel.hotels.common.PartnerConfigService;
import ru.yandex.travel.hotels.common.Permalink;
import ru.yandex.travel.hotels.common.partners.travelline.TravellineClient;
import ru.yandex.travel.hotels.common.partners.travelline.model.ContactType;
import ru.yandex.travel.hotels.common.partners.travelline.model.HotelDetails;
import ru.yandex.travel.hotels.common.partners.travelline.model.HotelDetailsAddress;
import ru.yandex.travel.hotels.common.partners.travelline.model.HotelDetailsResponse;
import ru.yandex.travel.hotels.common.partners.travelline.model.HotelOfferStatus;
import ru.yandex.travel.hotels.common.partners.travelline.model.HotelRef;
import ru.yandex.travel.hotels.common.partners.travelline.model.TaxType;
import ru.yandex.travel.hotels.geosearch.GeoSearchService;
import ru.yandex.travel.hotels.geosearch.model.GeoHotel;
import ru.yandex.travel.hotels.geosearch.model.GeoSearchRsp;
import ru.yandex.travel.hotels.proto.EPartnerId;
import ru.yandex.travel.hotels.proto.TPartner;
import ru.yandex.travel.integration.balance.BillingApiClient;
import ru.yandex.travel.integration.balance.responses.BillingCheckRUBankAccountResponse;
import ru.yandex.travel.integration.balance.responses.BillingCreateContractResponse;
import ru.yandex.travel.integration.spark.SparkClient;
import ru.yandex.travel.integration.spark.responses.EntrepreneurShortReportResponse;
import ru.yandex.travel.orders.commons.proto.EVat;
import ru.yandex.travel.testing.TestUtils;
import ru.yandex.travel.workflow.entities.Workflow;
import ru.yandex.travel.workflow.repository.WorkflowRepository;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.travel.hotels.administrator.entity.KnownWorkflow.GENERIC_SUPERVISOR;

@RunWith(SpringRunner.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK
)
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@DirtiesContext
@MockBeans({
        @MockBean(HotelClusteringDictionary.class),
        @MockBean(HotelsYtPublishTask.class),
        @MockBean(PermalinkUpdateTask.class),
        @MockBean(ClusterizationService.class),
})
public class HotelConnectionWorkflowTest {

    private static final String HOTEL_CONNECTION_TICKET = "MOCKTICKET-123";
    private static final String LEGAL_DETAILS_TICKET = "MOCKTICKET-124";
    private static final String VERIFY_CLUSTERING_TICKET = null;
    private static final String MANUAL_VERIFICATION_TICKET = "MOCKTICKET-127";
    private static final String SIGNED_HOTEL_CODE = "123-1";
    private static final String SIGNED_HOTEL_INN = "INN-12345678";
    private static final Long BALANCE_CLIENT_ID = 111L;
    private static final Long BALANCE_PERSON_ID = 222L;
    private static final Long BALANCE_CONTRACT_ID = 333L;
    private static final String BALANCE_EXTERNAL_CONTRACT_ID = "externalId";
    private static final String HOTEL_NAME = "Тестовый отель";
    private static final long HOTEL_PERMALINK = 111222333;
    private static final String OGRNIP = "OGRNIP";

    @MockBean
    private StarTrekService starTrekService;

    @MockBean
    private TravellineClient travellineClient;

    @MockBean
    private GeoSearchService geoSearchService;

    @MockBean
    private PartnerConfigService partnerConfigService;

    @MockBean
    private HotelClusteringDictionary mockedHotelClusteringDictionary;

    @MockBean
    private AddressUnificationService addressUnificationService;

    @MockBean
    private BillingApiClient billingApiClient;

    @MockBean
    private SparkClient sparkClient;

    @Autowired
    private HotelConnectionRepository hotelConnectionRepository;

    @Autowired
    private ConnectionStepRepository connectionStepRepository;

    @Autowired
    private AdministratorService administratorService;

    @Autowired
    private AdministratorAdminService administratorAdminService;

    @Autowired
    private CommissionRepository commissionRepository;

    @Autowired
    private WorkflowRepository workflowRepository;

    @Autowired
    private LegalDetailsRepository legalDetailsRepository;

    @Autowired
    private BillingRegistrationRepository billingRegistrationRepository;

    @Autowired
    private LegalDetailsUpdateRepository legalDetailsUpdateRepository;

    @Autowired
    private AgreementRepository agreementRepository;

    @Autowired
    private HotelConnectionUpdateRepository hotelConnectionUpdateRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @After
    public void tearDown() {
        transactionTemplate.execute(ignored -> {
            commissionRepository.deleteAll();
            legalDetailsUpdateRepository.deleteAll();
            connectionStepRepository.deleteAll();
            billingRegistrationRepository.deleteAll();
            hotelConnectionUpdateRepository.deleteAll();
            hotelConnectionRepository.deleteAll();
            legalDetailsRepository.deleteAll();
            workflowRepository.deleteAll();
            agreementRepository.deleteAll();
            return null;
        });
    }

    @Test
    public void runConnectionWorkflowForHotel() {
        when(addressUnificationService.unifyAddressSync(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
        when(mockedHotelClusteringDictionary.isHotelClusteringVerified(eq(EPartnerId.PI_TRAVELLINE), eq(EntityCreatingUtils.HOTEL_CODE)))
                .thenReturn(false, true);
        when(partnerConfigService.getByKey(EPartnerId.PI_TRAVELLINE)).thenReturn(TPartner.newBuilder().setCode(
                "ytravel_travelline_test").build());
        when(geoSearchService.query(
                ArgumentMatchers.argThat(req ->
                        req.getOriginalId().getPartnerCode().equals("ytravel_travelline_test")
                                && req.getOriginalId().getOriginalId().equals(EntityCreatingUtils.HOTEL_CODE)))
        ).thenReturn(CompletableFuture.completedFuture(testGeoSearchResponse(HOTEL_NAME, HOTEL_PERMALINK)));

        EntrepreneurShortReportResponse reportResponse = new EntrepreneurShortReportResponse();
        EntrepreneurShortReportResponse.ReportData reportData = new EntrepreneurShortReportResponse.ReportData();
        EntrepreneurShortReportResponse.Report report = new EntrepreneurShortReportResponse.Report();
        report.setOGRNIP(OGRNIP);
        reportData.setReport(report);
        reportResponse.setData(reportData);
        when(sparkClient.getEntrepreneurShortReportSync(eq(EntityCreatingUtils.INN))).thenReturn(reportResponse);

        //1. Administrator retrieves hotels details from travelline API
        when(travellineClient.getHotelDetailsSync(eq(EntityCreatingUtils.HOTEL_CODE)))
                .thenReturn(EntityCreatingUtils.hotelDetailsResponse(false, false, false, false));

        //2. Then ticket for Hotel Connection is created
        when(starTrekService.createHotelConnectionTicket(ArgumentMatchers.argThat(hotelConnection ->
                hotelConnection.getHotelCode().equals(EntityCreatingUtils.HOTEL_CODE)
                        && hotelConnection.getPermalink() == null
                        && hotelConnection.getHotelName() == null))).thenReturn(HOTEL_CONNECTION_TICKET);
        //3. Along with ticket for Legal Details
        when(starTrekService.createOrUpdateLegalDetailsTicket(ArgumentMatchers.argThat(legalDetails ->
                legalDetails.getLegalName().equals(EntityCreatingUtils.LEGAL_NAME)
                        && legalDetails.getInn().equals(EntityCreatingUtils.INN)
                        && legalDetails.getPartnerId().equals(EPartnerId.PI_TRAVELLINE)))).thenReturn(LEGAL_DETAILS_TICKET);

        //4.1. For legal Details a workflow for partner registration in Billing is started
        when(billingApiClient.checkRUBankAccount(any(), any())).thenReturn(new BillingCheckRUBankAccountResponse(0, ""));
        when(billingApiClient.createClient(anyLong(), any())).thenReturn(BALANCE_CLIENT_ID);
        when(billingApiClient.createPerson(anyLong(), any())).thenReturn(BALANCE_PERSON_ID);
        when(billingApiClient.createContract(anyLong(), any())).thenReturn(BillingCreateContractResponse
                .builder().contractId(BALANCE_CONTRACT_ID).externalId(BALANCE_EXTERNAL_CONTRACT_ID).build());

        //6. When "update billing partner config" is done, ticket for Legal Details is closed

        //actual call - Push notification, that hotels details have changed
        administratorService.hotelDetailsChanged(
                THotelDetailsChangedReq.newBuilder()
                        .setHotelCode(EntityCreatingUtils.HOTEL_CODE)
                        .setPartnerId(EPartnerId.PI_TRAVELLINE)
                        .build(),
                TestUtils.genericsFriendlyMock(StreamObserver.class));

        TestUtils.waitForState("Hotel has got published", Duration.ofSeconds(10),
                () -> hotelConnectionRepository.findByPartnerIdAndHotelCode(EPartnerId.PI_TRAVELLINE, EntityCreatingUtils.HOTEL_CODE) != null &&
                        hotelConnectionRepository.findByPartnerIdAndHotelCode(EPartnerId.PI_TRAVELLINE, EntityCreatingUtils.HOTEL_CODE).getState() == EHotelConnectionState.CS_PUBLISHED);

        //Check that comments are left
        verify(starTrekService, times(1)).commentRegisteredInBalance(
                LEGAL_DETAILS_TICKET,
                BALANCE_CLIENT_ID,
                BALANCE_CONTRACT_ID);
        verify(starTrekService, times(1)).closeLegalDetailsTicket(LEGAL_DETAILS_TICKET);
        verify(starTrekService, times(1)).commentLegalDetailsReady(HOTEL_CONNECTION_TICKET);
        verify(starTrekService, times(1)).closeHotelConnectionTicket(HOTEL_CONNECTION_TICKET);
        verify(starTrekService, times(1)).linkTickets(LEGAL_DETAILS_TICKET, HOTEL_CONNECTION_TICKET);

        verify(starTrekService, atLeastOnce()).updateHotelConnectionTicket(ArgumentMatchers.argThat(hotelConnection ->
                hotelConnection.getHotelCode().equals(EntityCreatingUtils.HOTEL_CODE)
                        && hotelConnection.getPermalink().equals(HOTEL_PERMALINK)
                        && hotelConnection.getHotelName().equals(HOTEL_NAME)));

        HotelConnection hotelConnection =
                hotelConnectionRepository.findByPartnerIdAndHotelCode(EPartnerId.PI_TRAVELLINE, EntityCreatingUtils.HOTEL_CODE);
        Assert.assertNotNull(hotelConnection);
        Assert.assertEquals(HOTEL_CONNECTION_TICKET, hotelConnection.getStTicket());
        Assert.assertEquals(EPartnerId.PI_TRAVELLINE, hotelConnection.getPartnerId());
        Assert.assertEquals(EHotelConnectionState.CS_PUBLISHED, hotelConnection.getState());
        Assert.assertEquals(HOTEL_CONNECTION_TICKET, hotelConnection.getStTicket());
        Assert.assertEquals(HotelTaxType.COMMON, hotelConnection.getTaxType());
        Assert.assertEquals(EntityCreatingUtils.POST_CODE + ", " + EntityCreatingUtils.FACT_ADDRESS, hotelConnection.getAddress());
        Assert.assertEquals("City Name", hotelConnection.getCityName());
        Assert.assertEquals(EntityCreatingUtils.EMAIL, hotelConnection.getAccountantEmail());
        Assert.assertEquals("ReservationPhone", hotelConnection.getReservationPhone());
        Assert.assertEquals("Name", hotelConnection.getContractPersonName());
        Assert.assertEquals("Position", hotelConnection.getContractPersonPosition());
        Assert.assertEquals("Contract Email", hotelConnection.getContractPersonEmail());
        Assert.assertEquals("Phone", hotelConnection.getContractPersonPhone());
        Assert.assertEquals(EVat.VAT_20_120, hotelConnection.getVatType());
        Assert.assertTrue(hotelConnection.isClusteringVerified());
        Assert.assertTrue(hotelConnection.isGeoSearchCalled());

        List<Commission> commissionList = commissionRepository.findAll().stream()
                .filter(commission -> commission.getHotelConnection().getId().equals(hotelConnection.getId()))
                .collect(toList());
        Assert.assertEquals(2, commissionList.size());
        Commission commission = findCommission(commissionList, false);
        Assert.assertNotNull(commission);
        Assert.assertNull(commission.getAgreementEndDate());
        Assert.assertEquals("0.14", commission.getOrderConfirmedRate().toString());
        Assert.assertEquals("0.14", commission.getOrderRefundedRate().toString());
        Assert.assertTrue(commission.isEnabled());
        Commission promoCommission = findCommission(commissionList, true);
        Assert.assertNotNull(promoCommission);
        Assert.assertNotNull(promoCommission.getAgreementEndDate());
        Assert.assertEquals("0.07", promoCommission.getOrderConfirmedRate().toString());
        Assert.assertEquals("0.07", promoCommission.getOrderRefundedRate().toString());
        Assert.assertTrue(promoCommission.isEnabled());

        LegalDetails legalDetails = hotelConnection.getLegalDetails();
        Assert.assertNotNull(legalDetails);
        Assert.assertEquals(LEGAL_DETAILS_TICKET, legalDetails.getStTicket());
        Assert.assertEquals(ELegalDetailsState.DS_REGISTERED, legalDetails.getState());
        Assert.assertEquals(EntityCreatingUtils.INN, legalDetails.getInn());
        Assert.assertEquals(OGRNIP, legalDetails.getOgrn());
        Assert.assertEquals(EntityCreatingUtils.KPP, legalDetails.getKpp());
        Assert.assertEquals(EntityCreatingUtils.BIC, legalDetails.getBic());
        Assert.assertEquals(EntityCreatingUtils.CURRENT_ACCOUNT, legalDetails.getPaymentAccount());
        Assert.assertEquals(EntityCreatingUtils.LEGAL_NAME, legalDetails.getLegalName());
        Assert.assertEquals(EntityCreatingUtils.FULL_LEGAL_NAME, legalDetails.getFullLegalName());
        Assert.assertFalse(legalDetails.isBlockedByYandex());
        Assert.assertTrue(legalDetails.isOfferAccepted());
        Assert.assertEquals(BALANCE_CLIENT_ID, legalDetails.getBalanceClientId());
        Assert.assertEquals(BALANCE_PERSON_ID, legalDetails.getBalancePersonId());
        Assert.assertEquals(BALANCE_CONTRACT_ID, legalDetails.getBalanceContractId());
        Assert.assertEquals(BALANCE_EXTERNAL_CONTRACT_ID, legalDetails.getBalanceExternalContractId());
        Assert.assertNotNull(legalDetails.getRegisteredAt());
        Assert.assertTrue(legalDetails.isRegisteredInBalance());

        List<ConnectionStep> connectionSteps = connectionStepRepository.findAll().stream()
                .filter(cs -> (cs instanceof HotelConnectionStep && ((HotelConnectionStep) cs).getHotelConnection().getId().equals(hotelConnection.getId()))
                        || (cs instanceof LegalDetailsConnectionStep && ((LegalDetailsConnectionStep) cs).getLegalDetails().getId().equals(legalDetails.getId())))
                .collect(toList());
        Assert.assertEquals(3, connectionSteps.size());

        RegisterInBalanceStep registerInBalanceStep = tryFindConnectionStep(connectionSteps,
                RegisterInBalanceStep.class);
        Assert.assertNotNull(registerInBalanceStep);
        Assert.assertEquals(EConnectionStepState.CSS_DONE, registerInBalanceStep.getState());
        Assert.assertEquals(BALANCE_CLIENT_ID, registerInBalanceStep.getBalanceClientId());
        Assert.assertEquals(BALANCE_CONTRACT_ID, registerInBalanceStep.getBalanceContractId());
        Assert.assertFalse(registerInBalanceStep.isTicketResultFetched());

        VerifyClusteringStep verifyClusteringStep = tryFindConnectionStep(connectionSteps,
                VerifyClusteringStep.class);
        Assert.assertNotNull(verifyClusteringStep);
        Assert.assertEquals(EConnectionStepState.CSS_DONE, verifyClusteringStep.getState());
        Assert.assertEquals(VERIFY_CLUSTERING_TICKET, verifyClusteringStep.getStTicket());
        Assert.assertFalse(verifyClusteringStep.isTicketResultFetched());

        CallGeoSearchStep callGeoSearchStep = tryFindConnectionStep(connectionSteps,
                CallGeoSearchStep.class);
        Assert.assertNotNull(callGeoSearchStep);
        Assert.assertEquals(EConnectionStepState.CSS_DONE, callGeoSearchStep.getState());
        Assert.assertEquals(EntityCreatingUtils.HOTEL_CODE, callGeoSearchStep.getHotelCode());
        Assert.assertEquals(HOTEL_NAME, callGeoSearchStep.getHotelName());
        Assert.assertEquals(HOTEL_PERMALINK, (long) callGeoSearchStep.getPermalink());
        Assert.assertEquals(EPartnerId.PI_TRAVELLINE, callGeoSearchStep.getPartnerId());
        Assert.assertFalse(callGeoSearchStep.isTicketResultFetched());
    }

    @Test
    public void connectHotelAndThenChangeLegalEntityCompletely() {
        runConnectionWorkflowForHotel();

        EntrepreneurShortReportResponse reportResponse = new EntrepreneurShortReportResponse();
        EntrepreneurShortReportResponse.ReportData reportData = new EntrepreneurShortReportResponse.ReportData();
        EntrepreneurShortReportResponse.Report report = new EntrepreneurShortReportResponse.Report();
        report.setOGRNIP(OGRNIP);
        reportData.setReport(report);
        reportResponse.setData(reportData);
        when(sparkClient.getEntrepreneurShortReportSync(eq(EntityCreatingUtils.NEW_INN))).thenReturn(reportResponse);

        when(travellineClient.getHotelDetailsSync(eq(EntityCreatingUtils.HOTEL_CODE)))
                .thenReturn(EntityCreatingUtils.hotelDetailsResponse(true, false, false, false));
        administratorService.hotelDetailsChanged(
                THotelDetailsChangedReq.newBuilder()
                        .setHotelCode(EntityCreatingUtils.HOTEL_CODE)
                        .setPartnerId(EPartnerId.PI_TRAVELLINE)
                        .build(),
                TestUtils.genericsFriendlyMock(StreamObserver.class));
        doAnswer(invocation -> {
            Object[] args = invocation.getArguments();
            ((UpdateResult)args[0]).getHotelConnectionUpdate().setStTicket(MANUAL_VERIFICATION_TICKET);
            return null;
        }).when(starTrekService).createManualVerificationTicket(any());

        TestUtils.waitForState("Hotel has been moved to Manual verification", Duration.ofSeconds(10),
                () -> hotelConnectionRepository.findByPartnerIdAndHotelCode(EPartnerId.PI_TRAVELLINE, EntityCreatingUtils.HOTEL_CODE) != null &&
                        hotelConnectionRepository.findByPartnerIdAndHotelCode(EPartnerId.PI_TRAVELLINE, EntityCreatingUtils.HOTEL_CODE).getState() == EHotelConnectionState.CS_MANUAL_VERIFICATION);

        List<HotelConnectionUpdate> updateList = hotelConnectionUpdateRepository.findAll().stream().filter(update ->
                update.getHotelCode().equals(EntityCreatingUtils.HOTEL_CODE)).collect(toList());
        Assert.assertEquals(1, updateList.size());
        HotelConnectionUpdate connectionUpdate = updateList.get(0);
        Assert.assertEquals(MANUAL_VERIFICATION_TICKET, connectionUpdate.getStTicket());

        administratorAdminService.acceptHotelConnectionUpdate(TAcceptConnectionUpdateReq.newBuilder()
                .setUpdateId(connectionUpdate.getId().toString())
                .build(),
                TestUtils.genericsFriendlyMock(StreamObserver.class));

        TestUtils.waitForState("Hotel Connection Update has been closed", Duration.ofSeconds(10),
                () -> hotelConnectionUpdateRepository.findById(connectionUpdate.getId()).get().getState() == EHotelConnectionUpdateState.HCU_APPLIED);

        HotelConnection hotelConnection = hotelConnectionRepository.findByPartnerIdAndHotelCode(EPartnerId.PI_TRAVELLINE, EntityCreatingUtils.HOTEL_CODE);
        Assert.assertEquals(EHotelConnectionState.CS_PUBLISHED, hotelConnection.getState());
        Assert.assertEquals(EntityCreatingUtils.NEW_INN, hotelConnection.getLegalDetails().getInn());

        HotelConnectionUpdate resultingConnectionUpdate = hotelConnectionUpdateRepository.findById(connectionUpdate.getId()).get();
        Assert.assertTrue(resultingConnectionUpdate.isFinancialEventsUpdated());
        Assert.assertTrue(resultingConnectionUpdate.isLegalDetailsPublished());

        verify(starTrekService, times(1))
                .commentHotelConnectionUpdateProcessing(MANUAL_VERIFICATION_TICKET);
        verify(starTrekService, times(1))
                .commentLegalDetailsHaveBeenPublished(MANUAL_VERIFICATION_TICKET);
        verify(starTrekService, times(1))
                .closeHotelConnectionUpdateTicket(MANUAL_VERIFICATION_TICKET, BALANCE_CLIENT_ID);
    }

    @Test
    public void connectHotelAndThenApplyMajorChange() {
        runConnectionWorkflowForHotel();

        when(travellineClient.getHotelDetailsSync(eq(EntityCreatingUtils.HOTEL_CODE)))
                .thenReturn(EntityCreatingUtils.hotelDetailsResponse(false, false, true, false));
        administratorService.hotelDetailsChanged(
                THotelDetailsChangedReq.newBuilder()
                        .setHotelCode(EntityCreatingUtils.HOTEL_CODE)
                        .setPartnerId(EPartnerId.PI_TRAVELLINE)
                        .build(),
                TestUtils.genericsFriendlyMock(StreamObserver.class));
        doAnswer(invocation -> {
            Object[] args = invocation.getArguments();
            ((UpdateResult)args[0]).getHotelConnectionUpdate().setStTicket(MANUAL_VERIFICATION_TICKET);
            return null;
        }).when(starTrekService).createManualVerificationTicket(any());

        TestUtils.waitForState("Hotel has been moved to Manual verification", Duration.ofSeconds(10),
                () -> hotelConnectionRepository.findByPartnerIdAndHotelCode(EPartnerId.PI_TRAVELLINE, EntityCreatingUtils.HOTEL_CODE) != null &&
                        hotelConnectionRepository.findByPartnerIdAndHotelCode(EPartnerId.PI_TRAVELLINE, EntityCreatingUtils.HOTEL_CODE).getState() == EHotelConnectionState.CS_MANUAL_VERIFICATION);

        List<HotelConnectionUpdate> updateList = hotelConnectionUpdateRepository.findAll().stream().filter(update ->
                update.getHotelCode().equals(EntityCreatingUtils.HOTEL_CODE)).collect(toList());
        Assert.assertEquals(1, updateList.size());
        HotelConnectionUpdate connectionUpdate = updateList.get(0);
        Assert.assertEquals(MANUAL_VERIFICATION_TICKET, connectionUpdate.getStTicket());

        administratorAdminService.acceptHotelConnectionUpdate(TAcceptConnectionUpdateReq.newBuilder()
                        .setUpdateId(connectionUpdate.getId().toString())
                        .setBankChangeInplaceMode(true)
                        .build(),
                TestUtils.genericsFriendlyMock(StreamObserver.class));

        TestUtils.waitForState("Hotel Connection Update has been closed", Duration.ofSeconds(10),
                () -> hotelConnectionUpdateRepository.findById(connectionUpdate.getId()).get().getState() == EHotelConnectionUpdateState.HCU_APPLIED);

        HotelConnection hotelConnection = hotelConnectionRepository.findByPartnerIdAndHotelCode(EPartnerId.PI_TRAVELLINE, EntityCreatingUtils.HOTEL_CODE);
        Assert.assertEquals(EHotelConnectionState.CS_PUBLISHED, hotelConnection.getState());
        Assert.assertEquals(EntityCreatingUtils.INN, hotelConnection.getLegalDetails().getInn());
        Assert.assertEquals(EntityCreatingUtils.POST_CODE + ", " + EntityCreatingUtils.NEW_ADDRESS,
                hotelConnection.getLegalDetails().getLegalAddress());

        HotelConnectionUpdate resultingConnectionUpdate = hotelConnectionUpdateRepository.findById(connectionUpdate.getId()).get();
        Assert.assertTrue(resultingConnectionUpdate.isFinancialEventsUpdated());
        Assert.assertTrue(resultingConnectionUpdate.isLegalDetailsPublished());

        verify(starTrekService, times(1))
                .commentHotelConnectionUpdateProcessing(MANUAL_VERIFICATION_TICKET);
        verify(starTrekService, times(1))
                .commentLegalDetailsHaveBeenPublished(MANUAL_VERIFICATION_TICKET);
        verify(starTrekService, times(1))
                .closeHotelConnectionUpdateTicket(MANUAL_VERIFICATION_TICKET, BALANCE_CLIENT_ID);
    }

    @Test
    public void connectHotelAndThenChangeRequisitesInplace() {
        runConnectionWorkflowForHotel();
        UUID originalLegalDetailsId = hotelConnectionRepository
                .findByPartnerIdAndHotelCode(EPartnerId.PI_TRAVELLINE, EntityCreatingUtils.HOTEL_CODE).getLegalDetails().getId();

        when(travellineClient.getHotelDetailsSync(eq(EntityCreatingUtils.HOTEL_CODE)))
                .thenReturn(EntityCreatingUtils.hotelDetailsResponse(false, false, true, true));
        administratorService.hotelDetailsChanged(
                THotelDetailsChangedReq.newBuilder()
                        .setHotelCode(EntityCreatingUtils.HOTEL_CODE)
                        .setPartnerId(EPartnerId.PI_TRAVELLINE)
                        .build(),
                TestUtils.genericsFriendlyMock(StreamObserver.class));
        doAnswer(invocation -> {
            Object[] args = invocation.getArguments();
            ((UpdateResult)args[0]).getHotelConnectionUpdate().setStTicket(MANUAL_VERIFICATION_TICKET);
            return null;
        }).when(starTrekService).createManualVerificationTicket(any());

        TestUtils.waitForState("Hotel has been moved to Manual verification", Duration.ofSeconds(10),
                () -> hotelConnectionRepository.findByPartnerIdAndHotelCode(EPartnerId.PI_TRAVELLINE, EntityCreatingUtils.HOTEL_CODE) != null &&
                        hotelConnectionRepository.findByPartnerIdAndHotelCode(EPartnerId.PI_TRAVELLINE, EntityCreatingUtils.HOTEL_CODE).getState() == EHotelConnectionState.CS_MANUAL_VERIFICATION);

        List<HotelConnectionUpdate> updateList = hotelConnectionUpdateRepository.findAll().stream().filter(update ->
                update.getHotelCode().equals(EntityCreatingUtils.HOTEL_CODE)).collect(toList());
        Assert.assertEquals(1, updateList.size());
        HotelConnectionUpdate connectionUpdate = updateList.get(0);
        Assert.assertEquals(MANUAL_VERIFICATION_TICKET, connectionUpdate.getStTicket());

        administratorAdminService.acceptHotelConnectionUpdate(TAcceptConnectionUpdateReq.newBuilder()
                        .setUpdateId(connectionUpdate.getId().toString())
                        .setBankChangeInplaceMode(true)
                        .build(),
                TestUtils.genericsFriendlyMock(StreamObserver.class));

        TestUtils.waitForState("Hotel Connection Update has been closed", Duration.ofSeconds(10),
                () -> hotelConnectionUpdateRepository.findById(connectionUpdate.getId()).get().getState() == EHotelConnectionUpdateState.HCU_APPLIED);

        HotelConnection hotelConnection = hotelConnectionRepository.findByPartnerIdAndHotelCode(EPartnerId.PI_TRAVELLINE, EntityCreatingUtils.HOTEL_CODE);
        Assert.assertEquals(EHotelConnectionState.CS_PUBLISHED, hotelConnection.getState());
        Assert.assertEquals(originalLegalDetailsId, hotelConnection.getLegalDetails().getId()); //Legal details have remained the same
        Assert.assertEquals(EntityCreatingUtils.INN, hotelConnection.getLegalDetails().getInn());
        Assert.assertEquals(EntityCreatingUtils.NEW_CURRENT_ACCOUNT, hotelConnection.getLegalDetails().getPaymentAccount());
        Assert.assertEquals(EntityCreatingUtils.POST_CODE + ", " + EntityCreatingUtils.NEW_ADDRESS,
                hotelConnection.getLegalDetails().getLegalAddress());

        HotelConnectionUpdate resultingConnectionUpdate = hotelConnectionUpdateRepository.findById(connectionUpdate.getId()).get();
        Assert.assertTrue(resultingConnectionUpdate.isFinancialEventsUpdated());
        Assert.assertTrue(resultingConnectionUpdate.isLegalDetailsPublished());

        verify(starTrekService, times(1))
                .commentHotelConnectionUpdateProcessing(MANUAL_VERIFICATION_TICKET);
        verify(starTrekService, times(1))
                .commentLegalDetailsHaveBeenPublished(MANUAL_VERIFICATION_TICKET);
        verify(starTrekService, times(1))
                .closeHotelConnectionUpdateTicket(MANUAL_VERIFICATION_TICKET, BALANCE_CLIENT_ID);
    }

    @Test
    public void connectHotelAndThenRejectChanges() {
        runConnectionWorkflowForHotel();

        EntrepreneurShortReportResponse reportResponse = new EntrepreneurShortReportResponse();
        EntrepreneurShortReportResponse.ReportData reportData = new EntrepreneurShortReportResponse.ReportData();
        EntrepreneurShortReportResponse.Report report = new EntrepreneurShortReportResponse.Report();
        report.setOGRNIP(OGRNIP);
        reportData.setReport(report);
        reportResponse.setData(reportData);
        when(sparkClient.getEntrepreneurShortReportSync(eq(EntityCreatingUtils.NEW_INN))).thenReturn(reportResponse);

        when(travellineClient.getHotelDetailsSync(eq(EntityCreatingUtils.HOTEL_CODE)))
                .thenReturn(EntityCreatingUtils.hotelDetailsResponse(true, false, false, false));
        administratorService.hotelDetailsChanged(
                THotelDetailsChangedReq.newBuilder()
                        .setHotelCode(EntityCreatingUtils.HOTEL_CODE)
                        .setPartnerId(EPartnerId.PI_TRAVELLINE)
                        .build(),
                TestUtils.genericsFriendlyMock(StreamObserver.class));
        doAnswer(invocation -> {
            Object[] args = invocation.getArguments();
            ((UpdateResult)args[0]).getHotelConnectionUpdate().setStTicket(MANUAL_VERIFICATION_TICKET);
            return null;
        }).when(starTrekService).createManualVerificationTicket(any());

        TestUtils.waitForState("Hotel has been moved to Manual verification", Duration.ofSeconds(10),
                () -> hotelConnectionRepository.findByPartnerIdAndHotelCode(EPartnerId.PI_TRAVELLINE, EntityCreatingUtils.HOTEL_CODE) != null &&
                        hotelConnectionRepository.findByPartnerIdAndHotelCode(EPartnerId.PI_TRAVELLINE, EntityCreatingUtils.HOTEL_CODE).getState() == EHotelConnectionState.CS_MANUAL_VERIFICATION);

        List<HotelConnectionUpdate> updateList = hotelConnectionUpdateRepository.findAll().stream().filter(update ->
                update.getHotelCode().equals(EntityCreatingUtils.HOTEL_CODE)).collect(toList());
        Assert.assertEquals(1, updateList.size());
        HotelConnectionUpdate connectionUpdate = updateList.get(0);
        Assert.assertEquals(MANUAL_VERIFICATION_TICKET, connectionUpdate.getStTicket());

        //Emulate partner reverted changes and we want to reject changes
        when(travellineClient.getHotelDetailsSync(eq(EntityCreatingUtils.HOTEL_CODE)))
                .thenReturn(EntityCreatingUtils.hotelDetailsResponse(false, false, false, false));
        administratorAdminService.rejectHotelConnectionUpdate(TRejectConnectionUpdateReq.newBuilder()
                        .setUpdateId(connectionUpdate.getId().toString())
                        .build(),
                TestUtils.genericsFriendlyMock(StreamObserver.class));

        TestUtils.waitForState("Hotel Connection Update has been closed", Duration.ofSeconds(10),
                () -> hotelConnectionUpdateRepository.findById(connectionUpdate.getId()).get().getState() == EHotelConnectionUpdateState.HCU_REJECTED);

        HotelConnection hotelConnection = hotelConnectionRepository.findByPartnerIdAndHotelCode(EPartnerId.PI_TRAVELLINE, EntityCreatingUtils.HOTEL_CODE);
        Assert.assertEquals(EHotelConnectionState.CS_PUBLISHED, hotelConnection.getState());
        Assert.assertEquals(EntityCreatingUtils.INN, hotelConnection.getLegalDetails().getInn());

        HotelConnectionUpdate resultingConnectionUpdate = hotelConnectionUpdateRepository.findById(connectionUpdate.getId()).get();
        Assert.assertFalse(resultingConnectionUpdate.isFinancialEventsUpdated());
        Assert.assertFalse(resultingConnectionUpdate.isLegalDetailsPublished());

        verify(starTrekService, times(1))
                .rejectConnectionUpdate(MANUAL_VERIFICATION_TICKET);
    }

    @Test
    public void runConnectionWorkflowForHotelWithPaperAgreement() {
        transactionTemplate.execute(ingored -> {
            Agreement agreement = createAgreement();
            agreementRepository.saveAndFlush(agreement);
            return null;
        });
        when(billingApiClient.checkRUBankAccount(any(), any())).thenReturn(new BillingCheckRUBankAccountResponse(0, ""));
        when(addressUnificationService.unifyAddressSync(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
        when(partnerConfigService.getByKey(EPartnerId.PI_TRAVELLINE)).thenReturn(TPartner.newBuilder().setCode(
                "ytravel_travelline_test").build());
        when(geoSearchService.query(
                ArgumentMatchers.argThat(req ->
                        req.getOriginalId().getPartnerCode().equals("ytravel_travelline_test")
                                && req.getOriginalId().getOriginalId().equals(SIGNED_HOTEL_CODE)))
        ).thenReturn(CompletableFuture.completedFuture(testGeoSearchResponse(HOTEL_NAME, HOTEL_PERMALINK)));

        HotelDetailsResponse hotelDetailsResponse = new HotelDetailsResponse();
        hotelDetailsResponse.setHotelDetails(HotelDetails
                .builder()
                .hotelRef(HotelRef.builder().code(SIGNED_HOTEL_CODE).build())
                .addressDetails(HotelDetailsAddress.builder()
                        .fullAddress("Address Line")
                        .cityName("City Name")
                        .postalCode("123123")
                        .build())
                .contactInfo(List.of(
                        HotelDetails.HotelContactInfo.builder()
                                .contactType(ContactType.ACCOUNTANT)
                                // the minor incoming partner data change that should be applied to our system:
                                .email("accountant email")
                                .build(),
                        HotelDetails.HotelContactInfo.builder()
                                .contactType(ContactType.CONTRACT)
                                .name("Name")
                                .position("Position")
                                .phone("Phone")
                                .email("contract email")
                                .build(),
                        HotelDetails.HotelContactInfo.builder()
                                .contactType(ContactType.RESERVATION)
                                .phone("reservation phone")
                                .build()
                ))
                .bankAccountDetails(HotelDetails.BankAccountDetails
                        .builder()
                        .inn(SIGNED_HOTEL_INN)
                        .kpp(null)
                        .bic(EntityCreatingUtils.BIC)
                        .personLegalName(EntityCreatingUtils.LEGAL_NAME)
                        .addressDetails(HotelDetailsAddress.builder()
                                .fullAddress("Address Line")
                                .cityName("City Name")
                                .postalCode("123123")
                                .build())
                        .currentAccount(EntityCreatingUtils.CURRENT_ACCOUNT)
                        .tax(TaxType.COMMON_WITHOUT_VAT)
                        .build())
                .offerStatus(HotelOfferStatus.ACCEPTED)
                .build());
        //1. Administrator retrieves hotels details from travelline API
        when(travellineClient.getHotelDetailsSync(eq(SIGNED_HOTEL_CODE))).thenReturn(hotelDetailsResponse);

        //2. Then ticket for Hotel Connection is created
        when(starTrekService.createHotelConnectionTicket(ArgumentMatchers.argThat(hotelConnection ->
                hotelConnection.getHotelCode().equals(SIGNED_HOTEL_CODE)
                        && hotelConnection.getPermalink() == null
                        && hotelConnection.getHotelName() == null))).thenReturn(HOTEL_CONNECTION_TICKET);


        //actual call - Push notification, that hotels details have changed
        administratorService.hotelDetailsChanged(
                THotelDetailsChangedReq.newBuilder()
                        .setHotelCode(SIGNED_HOTEL_CODE)
                        .setPartnerId(EPartnerId.PI_TRAVELLINE)
                        .build(),
                TestUtils.genericsFriendlyMock(StreamObserver.class));

        TestUtils.waitForState("Hotel Connection state changed to Manual_Verification", Duration.ofSeconds(10),
                () -> transactionTemplate.execute(ignored ->
                        hotelConnectionRepository.findByPartnerIdAndHotelCode(EPartnerId.PI_TRAVELLINE, SIGNED_HOTEL_CODE) != null
                                && hotelConnectionRepository.findByPartnerIdAndHotelCode(EPartnerId.PI_TRAVELLINE,
                                SIGNED_HOTEL_CODE).getState() == EHotelConnectionState.CS_MANUAL_VERIFICATION));

        verify(starTrekService, times(1)).createManualVerificationTicket(ArgumentMatchers.argThat(actualizeResult ->
                actualizeResult.getHotelConnection().getHotelCode().equals(SIGNED_HOTEL_CODE)
                        && actualizeResult.getOldLegalDetails() == null
                        && actualizeResult.getExistingLegalDetails() == null
                        && actualizeResult.getNewLegalDetails().getLegalName().equals(EntityCreatingUtils.LEGAL_NAME)
                        && actualizeResult.getNewLegalDetails().getInn().equals(SIGNED_HOTEL_INN)
        ));

        transactionTemplate.execute(ingored -> {
            HotelConnection hotelConnection =
                    hotelConnectionRepository.findByPartnerIdAndHotelCode(EPartnerId.PI_TRAVELLINE, SIGNED_HOTEL_CODE);
            Assert.assertNotNull(hotelConnection);
            Assert.assertEquals(HOTEL_CONNECTION_TICKET, hotelConnection.getStTicket());
            Assert.assertEquals(EPartnerId.PI_TRAVELLINE, hotelConnection.getPartnerId());
            Assert.assertEquals(EHotelConnectionState.CS_MANUAL_VERIFICATION, hotelConnection.getState());
            Assert.assertEquals(HOTEL_CONNECTION_TICKET, hotelConnection.getStTicket());
            Assert.assertNull(hotelConnection.getTaxType());
            Assert.assertNull(hotelConnection.getAddress());
            Assert.assertNull(hotelConnection.getCityName());
            Assert.assertNull(hotelConnection.getAccountantEmail());
            Assert.assertNull(hotelConnection.getReservationPhone());
            Assert.assertNull(hotelConnection.getContractPersonName());
            Assert.assertNull(hotelConnection.getContractPersonPosition());
            Assert.assertNull(hotelConnection.getContractPersonEmail());
            Assert.assertNull(hotelConnection.getContractPersonPhone());
            Assert.assertFalse(hotelConnection.isClusteringVerified());
            Assert.assertTrue(hotelConnection.isGeoSearchCalled());

            List<Commission> commissionList = commissionRepository.findAll().stream()
                    .filter(commission -> commission.getHotelConnection().getId().equals(hotelConnection.getId()))
                    .collect(toList());
            Assert.assertTrue(commissionList.isEmpty());

            Assert.assertNull(hotelConnection.getLegalDetails());

            List<ConnectionStep> connectionSteps = connectionStepRepository.findAll().stream()
                    .filter(cs -> (cs instanceof HotelConnectionStep && ((HotelConnectionStep) cs).getHotelConnection().getId().equals(hotelConnection.getId())))
                    .collect(toList());
            Assert.assertEquals(1, connectionSteps.size());

            CallGeoSearchStep callGeoSearchStep = tryFindConnectionStep(connectionSteps,
                    CallGeoSearchStep.class);
            Assert.assertNotNull(callGeoSearchStep);
            Assert.assertEquals(EConnectionStepState.CSS_DONE, callGeoSearchStep.getState());
            Assert.assertEquals(SIGNED_HOTEL_CODE, callGeoSearchStep.getHotelCode());
            Assert.assertEquals(HOTEL_NAME, callGeoSearchStep.getHotelName());
            Assert.assertEquals(HOTEL_PERMALINK, (long) callGeoSearchStep.getPermalink());
            Assert.assertEquals(EPartnerId.PI_TRAVELLINE, callGeoSearchStep.getPartnerId());
            Assert.assertFalse(callGeoSearchStep.isTicketResultFetched());

            List<HotelConnectionUpdate> connectionUpdates = hotelConnectionUpdateRepository.findAll();
            Assert.assertEquals(1, connectionUpdates.size());
            HotelConnectionUpdate connectionUpdate = connectionUpdates.get(0);
            Assert.assertEquals(HotelTaxType.COMMON_WITHOUT_VAT, connectionUpdate.getTaxType());
            Assert.assertEquals("Address Line", connectionUpdate.getAddress());
            Assert.assertEquals("City Name", connectionUpdate.getCityName());
            Assert.assertEquals("accountant email", connectionUpdate.getAccountantEmail());
            Assert.assertEquals("reservation phone", connectionUpdate.getReservationPhone());
            Assert.assertEquals("Name", connectionUpdate.getContractPersonName());
            Assert.assertEquals("Position", connectionUpdate.getContractPersonPosition());
            Assert.assertEquals("contract email", connectionUpdate.getContractPersonEmail());
            Assert.assertEquals("Phone", connectionUpdate.getContractPersonPhone());
            return null;
        });
    }

    private Agreement createAgreement() {
        return Agreement.builder()
                .id(UUID.randomUUID())
                .agreementType(AgreementType.AGREEMENT)
                .inn(SIGNED_HOTEL_INN)
                .hotelCode(SIGNED_HOTEL_CODE)
                .kpp(null)
                .bic(EntityCreatingUtils.BIC)
                .paymentAccount(EntityCreatingUtils.CURRENT_ACCOUNT)
                .balanceClientId(BALANCE_CLIENT_ID)
                .balanceContractId(BALANCE_CONTRACT_ID)
                .balancePersonId(BALANCE_PERSON_ID)
                .balanceExternalContractId(BALANCE_EXTERNAL_CONTRACT_ID)
                .partnerId(EPartnerId.PI_TRAVELLINE)
                .active(true)
                .blocked(false)
                .build();
    }

    private Commission findCommission(List<Commission> commissionList, boolean promoCommission) {
        for (Commission commission : commissionList) {
            if (promoCommission) {
                if (commission.getAgreementEndDate() != null) {
                    return commission;
                }
            } else {
                if (commission.getAgreementEndDate() == null) {
                    return commission;
                }
            }
        }
        return null;
    }

    @Test
    public void existingConnectionUpdate() {
        when(billingApiClient.checkRUBankAccount(any(), any())).thenReturn(new BillingCheckRUBankAccountResponse(0, ""));
        when(addressUnificationService.unifyAddressSync(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
        String code1 = "existingConnectionUpdate_code1";
        String code2 = "existingConnectionUpdate_code2";
        long billingClientId = 8176238612479327L;
        Duration timeout = Duration.ofSeconds(10);
        Duration retry = Duration.ofMillis(10);

        LegalDetails legalDetails = LegalDetails.builder()
                .id(UUID.randomUUID())
                .state(ELegalDetailsState.DS_REGISTERED)
                .partnerId(EPartnerId.PI_TRAVELLINE)
                .inn("inn_existingConnectionUpdate")
                .kpp("kpp")
                .bic("bic")
                .paymentAccount("paymentAccount")
                .legalName(EntityCreatingUtils.LEGAL_NAME)
                .fullLegalName(EntityCreatingUtils.LEGAL_NAME)
                .phone("+1 999 999-99-99")
                .legalAddressUnified(true)
                .legalAddress("Address Line")
                .legalPostCode("123123")
                .postAddress("Address Line")
                .postCode("123123")
                .balanceClientId(billingClientId)
                .offerAccepted(true)
                .managedByAdministrator(true)
                .build();
        transactionTemplate.execute(ignored -> {
            workflowRepository.save(Workflow.createWorkflowForEntity(legalDetails, GENERIC_SUPERVISOR.getUuid()));
            legalDetailsRepository.save(legalDetails);

            BillingRegistration billingRegistration = BillingRegistration.builder()
                    .id(UUID.randomUUID())
                    .legalDetails(legalDetails)
                    .state(EBillingRegistrationState.BRS_REGISTERED)
                    .parentWorkflowId(legalDetails.getWorkflow().getId())
                    .build();
            workflowRepository.save(Workflow.createWorkflowForEntity(billingRegistration,
                    GENERIC_SUPERVISOR.getUuid()));
            billingRegistrationRepository.save(billingRegistration);
            return null;
        });

        UUID connection1Id = createPublishedConnection(code1, "email-1@example.com", legalDetails);
        UUID connection2Id = createPublishedConnection(code2, "email-2@example.com", legalDetails);

        HotelDetails hotelDetails1 = createHotelDetails(code1, legalDetails, "email-3@example.com", "+2 999 999-99-99");
        HotelDetails hotelDetails2 = createHotelDetails(code2, legalDetails, "email-2@example.com", "+2 999 999-99-99");

        doReturn(HotelDetailsResponse.builder().hotelDetails(hotelDetails1).build())
                .when(travellineClient).getHotelDetailsSync(code1);
        doReturn(HotelDetailsResponse.builder().hotelDetails(hotelDetails2).build())
                .when(travellineClient).getHotelDetailsSync(code2);

        administratorService.hotelDetailsChanged(
                THotelDetailsChangedReq.newBuilder()
                        .setHotelCode(code1)
                        .setPartnerId(EPartnerId.PI_TRAVELLINE)
                        .build(),
                TestUtils.genericsFriendlyMock(StreamObserver.class));
        administratorService.hotelDetailsChanged(
                THotelDetailsChangedReq.newBuilder()
                        .setHotelCode(code2)
                        .setPartnerId(EPartnerId.PI_TRAVELLINE)
                        .build(),
                TestUtils.genericsFriendlyMock(StreamObserver.class));

        TestUtils.waitForState("Legal details update applied", timeout, retry, () ->
                transactionTemplate.execute(ignored -> {
            var updates = legalDetailsUpdateRepository.findByLegalDetailsAndStateInOrderByCreatedAt(legalDetails,
                    Set.of(LegalDetailsUpdateState.APPLIED));
            // both updates should be applied
            return updates.size() >= 2;
        }));

        transactionTemplate.execute(ignored -> {
            Map<UUID, HotelConnection> updatedConnections = hotelConnectionRepository
                    .findAllById(List.of(connection1Id, connection2Id)).stream()
                    .collect(toMap(HotelConnection::getId, e -> e));
            assertThat(updatedConnections).hasSize(2);
            assertThat(updatedConnections.get(connection1Id).getAccountantEmail()).isEqualTo("email-3@example.com");
            assertThat(updatedConnections.get(connection2Id).getAccountantEmail()).isEqualTo("email-2@example.com");

            LegalDetails updatedLegalDetails = legalDetailsRepository.findAllById(List.of(legalDetails.getId())).get(0);
            assertThat(updatedLegalDetails.getPhone()).isEqualTo("+2 999 999-99-99");

            List<LegalDetailsUpdate> updates = legalDetailsUpdateRepository.findByLegalDetails(legalDetails);
            assertThat(updates).haveAtLeast(1, new Condition<>(update -> true, "any update"))
                    .anySatisfy(update -> {
                        assertThat(List.of(connection1Id, connection2Id))
                                .contains(update.getHotelConnection().getId());
                        assertThat(update.getState()).isEqualTo(LegalDetailsUpdateState.APPLIED);
                        assertThat(update.getPhone()).isEqualTo("+2 999 999-99-99");
                    });
            return null;
        });

        verify(billingApiClient, atLeastOnce()).updatePerson(anyLong(), argThat(
                person -> person.getClientId() == billingClientId
                        && person.getPhone().equals("+2 999 999-99-99")
                        && person.getEmail().contains("email-3@example.com")
                        && person.getEmail().contains("email-2@example.com")
                        && person.getEmail().contains("; ") // separator
        ));
    }

    //TODO (bsv5555, mbobrov): refactor it for a more concise transaction boundary
    @Test
    public void bindToExistingLegalDetails() {
        when(billingApiClient.checkRUBankAccount(any(), any())).thenReturn(new BillingCheckRUBankAccountResponse(0, ""));
        when(addressUnificationService.unifyAddressSync(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
        long billingClientId = 8276382464326949364L;
        Duration timeout = Duration.ofSeconds(10);
        Duration retry = Duration.ofMillis(10);

        LegalDetails legalDetails = LegalDetails.builder()
                .id(UUID.randomUUID())
                .state(ELegalDetailsState.DS_REGISTERED)
                .partnerId(EPartnerId.PI_TRAVELLINE)
                .inn("inn_bindToExistingLegalDetails")
                .kpp("kpp")
                .bic("bic")
                .legalName(EntityCreatingUtils.LEGAL_NAME)
                .fullLegalName(EntityCreatingUtils.LEGAL_NAME)
                .paymentAccount("paymentAccount")
                .balanceClientId(billingClientId)
                .offerAccepted(true)
                .legalAddressUnified(true)
                .legalAddress("Address Line")
                .legalPostCode("123123")
                .postAddress("Address Line")
                .postCode("123123")
                .managedByAdministrator(true)
                .build();

        transactionTemplate.execute(ignored -> {
            workflowRepository.save(Workflow.createWorkflowForEntity(legalDetails, GENERIC_SUPERVISOR.getUuid()));
            legalDetailsRepository.save(legalDetails);
            BillingRegistration billingRegistration = BillingRegistration.builder()
                    .id(UUID.randomUUID())
                    .legalDetails(legalDetails)
                    .state(EBillingRegistrationState.BRS_REGISTERED)
                    .parentWorkflowId(legalDetails.getWorkflow().getId())
                    .build();
            workflowRepository.save(Workflow.createWorkflowForEntity(billingRegistration,
                    GENERIC_SUPERVISOR.getUuid()));
            billingRegistrationRepository.save(billingRegistration);
            return null;
        });


        UUID connectionId = createPublishedConnection("existing_hotel_3", "email-3@example.com", legalDetails);

        HotelDetails hotelDetails = createHotelDetails("new_hotel_4", legalDetails, "email-4@example.com", null);
        doReturn(HotelDetailsResponse.builder().hotelDetails(hotelDetails).build())
                .when(travellineClient).getHotelDetailsSync("new_hotel_4");
        doReturn(TPartner.newBuilder().setCode("ytravel_travelline_bindToExistingLegalDetails").build())
                .when(partnerConfigService).getByKey(EPartnerId.PI_TRAVELLINE);
        doReturn(CompletableFuture.completedFuture(testGeoSearchResponse("Hotel 4", 8275381531232424L)))
                .when(geoSearchService).query(argThat(req -> req.getOriginalId().getOriginalId().equals("new_hotel_4")));
        doReturn("SOME-TICKET-835238123")
                .when(starTrekService).createHotelConnectionTicket(ArgumentMatchers.argThat(hotelConnection ->
                hotelConnection.getHotelCode().equals("new_hotel_4")
        ));
        doReturn(true).when(mockedHotelClusteringDictionary).isHotelClusteringVerified(
                EPartnerId.PI_TRAVELLINE,
                "new_hotel_4"
        );

        administratorService.hotelDetailsChanged(
                THotelDetailsChangedReq.newBuilder()
                        .setHotelCode("new_hotel_4")
                        .setPartnerId(EPartnerId.PI_TRAVELLINE)
                        .build(),
                TestUtils.genericsFriendlyMock(StreamObserver.class));

        TestUtils.waitForState("Hotel is published (binding)", timeout, retry,
                () -> transactionTemplate.execute(ignored -> {
                    HotelConnection newConnection =
                            hotelConnectionRepository.findByPartnerIdAndHotelCode(EPartnerId.PI_TRAVELLINE,
                                    "new_hotel_4");
                    var updates = legalDetailsUpdateRepository.findByLegalDetailsAndStateInOrderByCreatedAt(legalDetails,
                            Set.of(LegalDetailsUpdateState.APPLIED));
                    // the new hotel is published and the existing legal details are updated
                    return newConnection != null && newConnection.getState() == EHotelConnectionState.CS_PUBLISHED && updates.size() == 1;
                })
        );

        transactionTemplate.execute(ignored -> {
            HotelConnection oldConnection =
                    hotelConnectionRepository.findByPartnerIdAndHotelCode(EPartnerId.PI_TRAVELLINE, "existing_hotel_3");
            assertThat(oldConnection.getAccountantEmail()).isEqualTo("email-3@example.com");
            HotelConnection newConnection =
                    hotelConnectionRepository.findByPartnerIdAndHotelCode(EPartnerId.PI_TRAVELLINE, "new_hotel_4");
            assertThat(newConnection).isNotNull();
            assertThat(newConnection.getAccountantEmail()).isEqualTo("email-4@example.com");
            assertThat(newConnection.getId()).isNotEqualTo(connectionId);
            assertThat(newConnection.getLegalDetails().getId()).isEqualTo(legalDetails.getId());

            List<LegalDetailsUpdate> updates = legalDetailsUpdateRepository.findByLegalDetails(legalDetails);
            assertThat(updates).hasSize(1).first().satisfies(update -> {
                assertThat(update.getHotelConnection().getId()).isEqualTo(newConnection.getId());
                assertThat(update.getLegalDetails().getId()).isEqualTo(legalDetails.getId());
                assertThat(update.getState()).isEqualTo(LegalDetailsUpdateState.APPLIED);
            });
            return null;
        });

        verify(billingApiClient).updatePerson(anyLong(), argThat(
                person -> person.getClientId() == billingClientId
                        && person.getPhone() == null
                        && person.getEmail().contains("email-3@example.com")
                        && person.getEmail().contains("email-4@example.com")
                        && person.getEmail().contains("; ") // separator
        ));
    }

    private HotelDetails createHotelDetails(String code, LegalDetails legalDetails, String email, String phone) {
        return HotelDetails.builder()
                .offerStatus(HotelOfferStatus.ACCEPTED)
                .hotelRef(HotelRef.builder().code(code).build())
                .contactInfo(
                        List.of(
                                HotelDetails.HotelContactInfo.builder()
                                        .contactType(ContactType.ACCOUNTANT)
                                        // the minor incoming partner data change that should be applied to our system:
                                        .email(email)
                                        .build(),
                                HotelDetails.HotelContactInfo.builder()
                                        .contactType(ContactType.CONTRACT)
                                        .name("Name")
                                        .position("Position")
                                        .phone("Phone")
                                        .email("Email")
                                        .build(),
                                HotelDetails.HotelContactInfo.builder()
                                        .contactType(ContactType.RESERVATION)
                                        .phone("ReservationPhone")
                                        .build()
                        ))
                .addressDetails(HotelDetailsAddress.builder()
                        .fullAddress("Address Line")
                        .postalCode("123123")
                        .build())
                .bankAccountDetails(HotelDetails.BankAccountDetails.builder()
                        .inn(legalDetails.getInn())
                        .kpp(legalDetails.getKpp())
                        .bic(legalDetails.getBic())
                        .personLegalName(EntityCreatingUtils.LEGAL_NAME)
                        .currentAccount(legalDetails.getPaymentAccount())
                        .addressDetails(HotelDetailsAddress.builder()
                                .fullAddress("Address Line")
                                .postalCode("123123")
                                .build())
                        .phone(phone)
                        .tax(TaxType.COMMON)
                        .build())
                .build();
    }

    private UUID createPublishedConnection(String code, String email, LegalDetails legalDetails) {
        return transactionTemplate.execute(ignored -> {
            HotelConnection connection = HotelConnection.builder()
                    .id(UUID.randomUUID())
                    .state(EHotelConnectionState.CS_PUBLISHED)
                    .hotelCode(code)
                    .partnerId(EPartnerId.PI_TRAVELLINE)
                    .legalDetails(legalDetails)
                    .accountantEmail(email)
                    .build();
            workflowRepository.save(Workflow.createWorkflowForEntity(connection, GENERIC_SUPERVISOR.getUuid()));
            hotelConnectionRepository.save(connection);
            return connection.getId();
        });
    }

    private GeoSearchRsp testGeoSearchResponse(String name, Long permalink) {
        GeoObjectOuterClass.GeoObject geoObject = GeoObjectOuterClass.GeoObject.newBuilder()
                .setName(name)
                .build();
        GeoHotel geoHotel = new GeoHotel();
        geoHotel.setGeoObject(geoObject);
        geoHotel.setPermalink(Permalink.of(permalink));
        GeoSearchRsp geoSearchRsp = new GeoSearchRsp();
        geoSearchRsp.setHotels(List.of(geoHotel));
        return geoSearchRsp;
    }

    private <T> T tryFindConnectionStep(List<? extends ConnectionStep> connectionSteps, Class<T> clazz) {
        for (ConnectionStep connectionStep : connectionSteps) {
            if (clazz.isInstance(connectionStep)) {
                return clazz.cast(connectionStep);
            }
        }
        return null;
    }
}
