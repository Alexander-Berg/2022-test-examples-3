package ru.yandex.travel.hotels.administrator.service;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.util.concurrent.MoreExecutors;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.misc.ExceptionUtils;
import ru.yandex.travel.hotels.administrator.EntityCreatingUtils;
import ru.yandex.travel.hotels.administrator.configuration.HotelConnectionProperties;
import ru.yandex.travel.hotels.administrator.entity.HotelConnection;
import ru.yandex.travel.hotels.administrator.entity.HotelConnectionUpdate;
import ru.yandex.travel.hotels.administrator.entity.LegalDetails;
import ru.yandex.travel.hotels.administrator.entity.LegalDetailsUpdate;
import ru.yandex.travel.hotels.administrator.repository.HotelConnectionRepository;
import ru.yandex.travel.hotels.administrator.repository.HotelConnectionUpdateRepository;
import ru.yandex.travel.hotels.administrator.repository.LegalDetailsRepository;
import ru.yandex.travel.hotels.administrator.repository.LegalDetailsUpdateRepository;
import ru.yandex.travel.hotels.administrator.service.partners.PartnerServiceProvider;
import ru.yandex.travel.hotels.administrator.service.partners.TravellineService;
import ru.yandex.travel.hotels.administrator.service.partners.mappers.HotelDetailsMapperImpl;
import ru.yandex.travel.hotels.administrator.workflow.proto.EHotelConnectionState;
import ru.yandex.travel.hotels.common.partners.travelline.TravellineClient;
import ru.yandex.travel.workflow.MessagingContext;
import ru.yandex.travel.workflow.StateContext;
import ru.yandex.travel.workflow.entities.Workflow;
import ru.yandex.travel.workflow.repository.WorkflowRepository;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;
import static ru.yandex.travel.testing.TestUtils.genericsFriendlyMock;

@RunWith(SpringRunner.class)
@DataJpaTest
@ActiveProfiles("test")
@Import(value = {
        PartnerServiceProvider.class,
        TravellineService.class,
        HotelDetailsMapperImpl.class,
        HotelConnectionProperties.class
})
@Slf4j
public class HotelConnectionServiceTest {

    private static final String UNIFIED_ADDRESS = "unified address";

    @Autowired
    private HotelConnectionRepository hotelConnectionRepository;
    @Autowired
    private WorkflowRepository workflowRepository;
    @Autowired
    private LegalDetailsRepository legalDetailsRepository;
    @Autowired
    private TransactionTemplate transactionTemplate;
    @Autowired
    private LegalDetailsUpdateRepository legalDetailsUpdateRepository;
    @Autowired
    private HotelConnectionUpdateRepository hotelConnectionUpdateRepository;
    @Autowired
    private PartnerServiceProvider partnerServiceProvider;
    @Autowired
    private HotelConnectionProperties hotelConnectionProperties;
    @MockBean
    private AddressUnificationService addressUnificationService;
    @MockBean
    private TravellineClient travellineClient;
    @MockBean
    private AgreementService agreementService;
    @MockBean
    private StarTrekService starTrekService;
    @MockBean
    private BillingService billingService;

    private LegalDetailsRepository legalDetailsRepositorySpy;
    private HotelConnectionService service;

    private ThreadLocal<Integer> threadId;

    @Before
    public void init() {
        // not using spy(obj) to overcome final class issues under "ya make -tt" runs
        legalDetailsRepositorySpy = Mockito.mock(LegalDetailsRepository.class, invocation ->
                forwardMethodCall(invocation, legalDetailsRepository));
        when(billingService.checkRuBankAccount(any(), any())).thenReturn(true);
        service = new HotelConnectionService(
                hotelConnectionRepository, workflowRepository, partnerServiceProvider, legalDetailsRepositorySpy,
                legalDetailsUpdateRepository, hotelConnectionUpdateRepository, addressUnificationService,
                agreementService, starTrekService, billingService, hotelConnectionProperties);
        threadId = new ThreadLocal<>();
    }

    @Test
    public void actualizeHotelConnection_newHotel() {
        StateContext<EHotelConnectionState, HotelConnection> stateContext = genericsFriendlyMock(StateContext.class);
        when(travellineClient.getHotelDetailsSync(any())).thenReturn(EntityCreatingUtils.hotelDetailsResponse(false, false, false, false));
        UpdateResult updateResult = service.actualizeHotelConnection(EntityCreatingUtils.hotelConnection(), stateContext);
        Assert.assertEquals(UpdateResult.ResultType.SUCCESS, updateResult.getType());
        Assert.assertEquals(0, legalDetailsUpdateRepository.findAll().size());
        List<HotelConnection> hotelConnectionList = hotelConnectionRepository.findAll();
        Assert.assertEquals(1, hotelConnectionList.size());
        HotelConnection hotelConnection = hotelConnectionList.get(0);
        LegalDetails legalDetails = hotelConnection.getLegalDetails();
        Assert.assertEquals(EntityCreatingUtils.INN, legalDetails.getInn());
        Assert.assertEquals(EntityCreatingUtils.KPP, legalDetails.getKpp());
        Assert.assertEquals(EntityCreatingUtils.BIC, legalDetails.getBic());
        Assert.assertEquals(EntityCreatingUtils.CURRENT_ACCOUNT, legalDetails.getPaymentAccount());
        Assert.assertEquals(EntityCreatingUtils.POST_CODE + ", " + EntityCreatingUtils.FACT_ADDRESS, legalDetails.getPostAddress());
        Assert.assertEquals(EntityCreatingUtils.PHONE, legalDetails.getPhone());
    }

    @Test
    public void actualizeHotelConnection_newHotelWithPaperAgreement() {
        HotelConnection hotelConnection = EntityCreatingUtils.hotelConnection();
        hotelConnection = hotelConnectionRepository.save(hotelConnection);
        StateContext<EHotelConnectionState, HotelConnection> stateContext = genericsFriendlyMock(StateContext.class);
        when(travellineClient.getHotelDetailsSync(any())).thenReturn(EntityCreatingUtils.hotelDetailsResponse(false, false, false, false));
        when(addressUnificationService.unifyAddressSync(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
        when(agreementService.existsPaperAgreementForInn(EntityCreatingUtils.INN)).thenReturn(true);
        UpdateResult updateResult = service.actualizeHotelConnection(hotelConnection, stateContext);
        Assert.assertEquals(UpdateResult.ResultType.VALIDATION_REQUIRED, updateResult.getType());
        Assert.assertEquals(UpdateResult.ChangeRequisitesType.PAPER_AGREEMENT_CHANGE, updateResult.getChangeType());
        Assert.assertNull(updateResult.getOldLegalDetails());
        Assert.assertNull(updateResult.getExistingLegalDetails());
        Assert.assertEquals(EntityCreatingUtils.INN, updateResult.getNewLegalDetails().getInn());
        Assert.assertEquals(EntityCreatingUtils.POST_CODE + ", " + EntityCreatingUtils.ADDRESS, updateResult.getNewLegalDetails().getLegalAddress());
        Assert.assertEquals(EntityCreatingUtils.PHONE, updateResult.getNewLegalDetails().getPhone());
        List<HotelConnection> hotelConnectionList = hotelConnectionRepository.findAll();
        Assert.assertEquals(1, hotelConnectionList.size());
        hotelConnection = hotelConnectionList.get(0);
        Assert.assertNull(hotelConnection.getLegalDetails());
        List<LegalDetailsUpdate> legalDetailsUpdateList = legalDetailsUpdateRepository.findAll();
        Assert.assertTrue(legalDetailsUpdateList.isEmpty());
    }

    @Test
    public void actualizeHotelConnection_newHotelWithExistingDetailsWithMinorChange() {
        LegalDetails existingLegalDetails = existingLegalDetails(false);
        existingLegalDetails = legalDetailsRepository.save(existingLegalDetails);
        workflowRepository.save(existingLegalDetails.getWorkflow());
        HotelConnection hotelConnection = EntityCreatingUtils.hotelConnection();
        hotelConnection = hotelConnectionRepository.save(hotelConnection);
        StateContext<EHotelConnectionState, HotelConnection> stateContext = genericsFriendlyMock(StateContext.class);
        when(travellineClient.getHotelDetailsSync(any())).thenReturn(EntityCreatingUtils.hotelDetailsResponse(false, true, false, false));
        when(addressUnificationService.unifyAddressSync(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
        UpdateResult updateResult = service.actualizeHotelConnection(hotelConnection, stateContext);
        Assert.assertEquals(UpdateResult.ResultType.SUCCESS, updateResult.getType());
        List<HotelConnection> hotelConnectionList = hotelConnectionRepository.findAll();
        Assert.assertEquals(1, hotelConnectionList.size());
        hotelConnection = hotelConnectionList.get(0);
        LegalDetails legalDetails = hotelConnection.getLegalDetails();
        Assert.assertEquals(EntityCreatingUtils.INN, legalDetails.getInn());
        Assert.assertEquals(EntityCreatingUtils.KPP, legalDetails.getKpp());
        Assert.assertEquals(EntityCreatingUtils.BIC, legalDetails.getBic());
        Assert.assertEquals(EntityCreatingUtils.CURRENT_ACCOUNT, legalDetails.getPaymentAccount());
        Assert.assertEquals(EntityCreatingUtils.POST_CODE + ", " + EntityCreatingUtils.ADDRESS, legalDetails.getPostAddress());
        Assert.assertEquals(EntityCreatingUtils.PHONE, legalDetails.getPhone());
        List<LegalDetailsUpdate> legalDetailsUpdateList = legalDetailsUpdateRepository.findAll();
        Assert.assertEquals(1, legalDetailsUpdateList.size());
        LegalDetailsUpdate legalDetailsUpdate = legalDetailsUpdateList.get(0);
        Assert.assertEquals(legalDetails.getId(), legalDetailsUpdate.getLegalDetails().getId());
        Assert.assertEquals(EntityCreatingUtils.NEW_PHONE, legalDetailsUpdate.getPhone());
    }

    @Test
    public void actualizeHotelConnection_newHotelWithExistingDetailsWithMajorChange() {
        LegalDetails existingLegalDetails = existingLegalDetails(false);
        existingLegalDetails = legalDetailsRepository.save(existingLegalDetails);
        workflowRepository.save(existingLegalDetails.getWorkflow());
        HotelConnection hotelConnection = EntityCreatingUtils.hotelConnection();
        hotelConnection = hotelConnectionRepository.save(hotelConnection);
        StateContext<EHotelConnectionState, HotelConnection> stateContext = genericsFriendlyMock(StateContext.class);
        when(travellineClient.getHotelDetailsSync(any())).thenReturn(EntityCreatingUtils.hotelDetailsResponse(false, true, true, false));
        when(addressUnificationService.unifyAddressSync(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
        UpdateResult updateResult = service.actualizeHotelConnection(hotelConnection, stateContext);
        Assert.assertEquals(UpdateResult.ResultType.VALIDATION_REQUIRED, updateResult.getType());
        Assert.assertEquals(UpdateResult.ChangeRequisitesType.OTHER_CHANGE, updateResult.getChangeType());
        Assert.assertNull(updateResult.getOldLegalDetails());
        Assert.assertEquals(EntityCreatingUtils.INN, updateResult.getExistingLegalDetails().getInn());
        Assert.assertEquals(EntityCreatingUtils.POST_CODE + ", " + EntityCreatingUtils.ADDRESS, updateResult.getExistingLegalDetails().getLegalAddress());
        Assert.assertEquals(EntityCreatingUtils.PHONE, updateResult.getExistingLegalDetails().getPhone());
        Assert.assertEquals(EntityCreatingUtils.INN, updateResult.getNewLegalDetails().getInn());
        Assert.assertEquals(EntityCreatingUtils.POST_CODE + ", " + EntityCreatingUtils.NEW_ADDRESS, updateResult.getNewLegalDetails().getLegalAddress());
        Assert.assertEquals(EntityCreatingUtils.NEW_PHONE, updateResult.getNewLegalDetails().getPhone());
        List<HotelConnection> hotelConnectionList = hotelConnectionRepository.findAll();
        Assert.assertEquals(1, hotelConnectionList.size());
        hotelConnection = hotelConnectionList.get(0);
        Assert.assertNull(hotelConnection.getLegalDetails());
        HotelConnectionUpdate connectionUpdate = updateResult.getHotelConnectionUpdate();
        Assert.assertNotNull(connectionUpdate);
        Assert.assertEquals(EntityCreatingUtils.INN, connectionUpdate.getInn());
        Assert.assertEquals(EntityCreatingUtils.KPP, connectionUpdate.getKpp());
        Assert.assertEquals(EntityCreatingUtils.BIC, connectionUpdate.getBic());
        Assert.assertEquals(EntityCreatingUtils.CURRENT_ACCOUNT, connectionUpdate.getPaymentAccount());
        Assert.assertEquals(EntityCreatingUtils.POST_CODE + ", " + EntityCreatingUtils.NEW_ADDRESS, connectionUpdate.getLegalAddress());
        Assert.assertEquals(EntityCreatingUtils.NEW_PHONE, connectionUpdate.getLegalPhone());
        List<LegalDetailsUpdate> legalDetailsUpdateList = legalDetailsUpdateRepository.findAll();
        Assert.assertTrue(legalDetailsUpdateList.isEmpty());
    }

    @Test
    public void actualizeHotelConnection_newHotelWithExistingDetailsWithNoChangeWithPaperAgreement() {
        LegalDetails existingLegalDetails = existingLegalDetails(false, false);
        existingLegalDetails = legalDetailsRepository.save(existingLegalDetails);
        workflowRepository.save(existingLegalDetails.getWorkflow());
        HotelConnection hotelConnection = EntityCreatingUtils.hotelConnection();
        hotelConnection = hotelConnectionRepository.save(hotelConnection);
        StateContext<EHotelConnectionState, HotelConnection> stateContext = genericsFriendlyMock(StateContext.class);
        when(travellineClient.getHotelDetailsSync(any())).thenReturn(EntityCreatingUtils.hotelDetailsResponse(false, false, false, false));
        when(addressUnificationService.unifyAddressSync(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
        when(agreementService.existsPaperAgreementForInn(EntityCreatingUtils.INN)).thenReturn(true);
        UpdateResult updateResult = service.actualizeHotelConnection(hotelConnection, stateContext);
        Assert.assertEquals(UpdateResult.ResultType.VALIDATION_REQUIRED, updateResult.getType());
        Assert.assertEquals(UpdateResult.ChangeRequisitesType.PAPER_AGREEMENT_CHANGE, updateResult.getChangeType());
        Assert.assertNull(updateResult.getOldLegalDetails());
        Assert.assertEquals(EntityCreatingUtils.INN, updateResult.getExistingLegalDetails().getInn());
        Assert.assertEquals(EntityCreatingUtils.POST_CODE + ", " + EntityCreatingUtils.ADDRESS, updateResult.getExistingLegalDetails().getLegalAddress());
        Assert.assertEquals(EntityCreatingUtils.PHONE, updateResult.getExistingLegalDetails().getPhone());
        Assert.assertEquals(EntityCreatingUtils.INN, updateResult.getNewLegalDetails().getInn());
        Assert.assertEquals(EntityCreatingUtils.POST_CODE + ", " + EntityCreatingUtils.ADDRESS, updateResult.getNewLegalDetails().getLegalAddress());
        Assert.assertEquals(EntityCreatingUtils.PHONE, updateResult.getNewLegalDetails().getPhone());
        List<HotelConnection> hotelConnectionList = hotelConnectionRepository.findAll();
        Assert.assertEquals(1, hotelConnectionList.size());
        hotelConnection = hotelConnectionList.get(0);
        Assert.assertNull(hotelConnection.getLegalDetails());
        List<LegalDetailsUpdate> legalDetailsUpdateList = legalDetailsUpdateRepository.findAll();
        Assert.assertTrue(legalDetailsUpdateList.isEmpty());

        ApplyUpdateResult applyUpdateResult = service.applyHotelConnectionUpdate(hotelConnection, updateResult.getHotelConnectionUpdate(),
                genericsFriendlyMock(MessagingContext.class), true);
        Assert.assertFalse(applyUpdateResult.isLegalDetailsUpdated());
        Assert.assertEquals(hotelConnection.getLegalDetails().getId(), existingLegalDetails.getId());
    }

    @Test
    public void actualizeHotelConnection_existingHotelWithSameDetails() {
        LegalDetails existingLegalDetails = existingLegalDetails(false);
        existingLegalDetails.setLegalAddressUnified(false);
        existingLegalDetails = legalDetailsRepository.save(existingLegalDetails);
        workflowRepository.save(existingLegalDetails.getWorkflow());
        HotelConnection hotelConnection = EntityCreatingUtils.hotelConnection();
        hotelConnection.setLegalDetails(existingLegalDetails);
        hotelConnection = hotelConnectionRepository.save(hotelConnection);
        StateContext<EHotelConnectionState, HotelConnection> stateContext = genericsFriendlyMock(StateContext.class);
        when(travellineClient.getHotelDetailsSync(any())).thenReturn(EntityCreatingUtils.hotelDetailsResponse(false, false, false, false));
        when(addressUnificationService.unifyAddressSync(anyString())).thenReturn(UNIFIED_ADDRESS);
        UpdateResult updateResult = service.actualizeHotelConnection(hotelConnection, stateContext);
        Assert.assertEquals(UpdateResult.ResultType.SUCCESS, updateResult.getType());
        List<HotelConnection> hotelConnectionList = hotelConnectionRepository.findAll();
        Assert.assertEquals(1, hotelConnectionList.size());
        hotelConnection = hotelConnectionList.get(0);
        LegalDetails legalDetails = hotelConnection.getLegalDetails();
        Assert.assertEquals(EntityCreatingUtils.INN, legalDetails.getInn());
        Assert.assertEquals(EntityCreatingUtils.KPP, legalDetails.getKpp());
        Assert.assertEquals(EntityCreatingUtils.BIC, legalDetails.getBic());
        Assert.assertEquals(EntityCreatingUtils.CURRENT_ACCOUNT, legalDetails.getPaymentAccount());
        Assert.assertEquals(EntityCreatingUtils.POST_CODE + ", " + EntityCreatingUtils.ADDRESS, legalDetails.getPostAddress());
        Assert.assertEquals(EntityCreatingUtils.PHONE, legalDetails.getPhone());
        List<LegalDetailsUpdate> legalDetailsUpdateList = legalDetailsUpdateRepository.findAll();
        Assert.assertEquals(1, legalDetailsUpdateList.size());
        LegalDetailsUpdate legalDetailsUpdate = legalDetailsUpdateList.get(0);
        Assert.assertEquals(legalDetails.getId(), legalDetailsUpdate.getLegalDetails().getId());
        Assert.assertEquals(UNIFIED_ADDRESS, legalDetailsUpdate.getLegalAddress());
        Assert.assertTrue(legalDetailsUpdate.isLegalAddressUnified());
    }

    @Test
    public void actualizeHotelConnection_existingHotelWithSameDetailsWithPaperAgreement() {
        LegalDetails existingLegalDetails = existingLegalDetails(false);
        existingLegalDetails = legalDetailsRepository.save(existingLegalDetails);
        workflowRepository.save(existingLegalDetails.getWorkflow());
        HotelConnection hotelConnection = EntityCreatingUtils.hotelConnection();
        hotelConnection.setLegalDetails(existingLegalDetails);
        hotelConnection = hotelConnectionRepository.save(hotelConnection);
        StateContext<EHotelConnectionState, HotelConnection> stateContext = genericsFriendlyMock(StateContext.class);
        when(travellineClient.getHotelDetailsSync(any())).thenReturn(EntityCreatingUtils.hotelDetailsResponse(false, false, false, false));
        when(addressUnificationService.unifyAddressSync(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
        when(agreementService.existsPaperAgreementForInn(EntityCreatingUtils.INN)).thenReturn(true);
        UpdateResult updateResult = service.actualizeHotelConnection(hotelConnection, stateContext);
        Assert.assertEquals(UpdateResult.ResultType.SUCCESS, updateResult.getType());
        List<HotelConnection> hotelConnectionList = hotelConnectionRepository.findAll();
        Assert.assertEquals(1, hotelConnectionList.size());
        hotelConnection = hotelConnectionList.get(0);
        LegalDetails legalDetails = hotelConnection.getLegalDetails();
        Assert.assertEquals(EntityCreatingUtils.INN, legalDetails.getInn());
        Assert.assertEquals(EntityCreatingUtils.KPP, legalDetails.getKpp());
        Assert.assertEquals(EntityCreatingUtils.BIC, legalDetails.getBic());
        Assert.assertEquals(EntityCreatingUtils.CURRENT_ACCOUNT, legalDetails.getPaymentAccount());
        Assert.assertEquals(EntityCreatingUtils.POST_CODE + ", " + EntityCreatingUtils.ADDRESS, legalDetails.getPostAddress());
        Assert.assertEquals(EntityCreatingUtils.PHONE, legalDetails.getPhone());
        List<LegalDetailsUpdate> legalDetailsUpdateList = legalDetailsUpdateRepository.findAll();
        Assert.assertTrue(legalDetailsUpdateList.isEmpty());
    }

    @Test
    public void actualizeHotelConnection_existingHotelWithMinorChangesWithPaperAgreement() {
        LegalDetails existingLegalDetails = existingLegalDetails(false);
        existingLegalDetails = legalDetailsRepository.save(existingLegalDetails);
        workflowRepository.save(existingLegalDetails.getWorkflow());
        HotelConnection hotelConnection = EntityCreatingUtils.hotelConnection();
        hotelConnection.setLegalDetails(existingLegalDetails);
        hotelConnection = hotelConnectionRepository.save(hotelConnection);
        StateContext<EHotelConnectionState, HotelConnection> stateContext = genericsFriendlyMock(StateContext.class);
        when(travellineClient.getHotelDetailsSync(any())).thenReturn(EntityCreatingUtils.hotelDetailsResponse(false, true, false, false));
        when(agreementService.existsPaperAgreementForInn(EntityCreatingUtils.INN)).thenReturn(true);
        UpdateResult updateResult = service.actualizeHotelConnection(hotelConnection, stateContext);
        Assert.assertEquals(UpdateResult.ResultType.VALIDATION_REQUIRED, updateResult.getType());
        Assert.assertEquals(UpdateResult.ChangeRequisitesType.PAPER_AGREEMENT_CHANGE, updateResult.getChangeType());
        Assert.assertNull(updateResult.getExistingLegalDetails());
        Assert.assertEquals(EntityCreatingUtils.INN, updateResult.getOldLegalDetails().getInn());
        Assert.assertEquals(EntityCreatingUtils.POST_CODE + ", " + EntityCreatingUtils.ADDRESS, updateResult.getOldLegalDetails().getLegalAddress());
        Assert.assertEquals(EntityCreatingUtils.PHONE, updateResult.getOldLegalDetails().getPhone());
        Assert.assertNull(updateResult.getExistingLegalDetails());
        Assert.assertEquals(EntityCreatingUtils.INN, updateResult.getNewLegalDetails().getInn());
        Assert.assertEquals(EntityCreatingUtils.POST_CODE + ", " + EntityCreatingUtils.ADDRESS, updateResult.getNewLegalDetails().getLegalAddress());
        Assert.assertEquals(EntityCreatingUtils.NEW_PHONE, updateResult.getNewLegalDetails().getPhone());
        List<HotelConnection> hotelConnectionList = hotelConnectionRepository.findAll();
        Assert.assertEquals(1, hotelConnectionList.size());
        hotelConnection = hotelConnectionList.get(0);
        LegalDetails legalDetails = hotelConnection.getLegalDetails();
        Assert.assertEquals(EntityCreatingUtils.INN, legalDetails.getInn());
        Assert.assertEquals(EntityCreatingUtils.KPP, legalDetails.getKpp());
        Assert.assertEquals(EntityCreatingUtils.BIC, legalDetails.getBic());
        Assert.assertEquals(EntityCreatingUtils.CURRENT_ACCOUNT, legalDetails.getPaymentAccount());
        Assert.assertEquals(EntityCreatingUtils.POST_CODE + ", " + EntityCreatingUtils.ADDRESS, legalDetails.getPostAddress());
        Assert.assertEquals(EntityCreatingUtils.PHONE, legalDetails.getPhone());
        List<LegalDetailsUpdate> legalDetailsUpdateList = legalDetailsUpdateRepository.findAll();
        Assert.assertTrue(legalDetailsUpdateList.isEmpty());
    }

    @Test
    public void actualizeHotelConnection_existingHotelWithSameDetailsMinorChange() {
        LegalDetails existingLegalDetails = existingLegalDetails(false);
        existingLegalDetails = legalDetailsRepository.save(existingLegalDetails);
        workflowRepository.save(existingLegalDetails.getWorkflow());
        HotelConnection hotelConnection = EntityCreatingUtils.hotelConnection();
        hotelConnection.setLegalDetails(existingLegalDetails);
        hotelConnection = hotelConnectionRepository.save(hotelConnection);
        StateContext<EHotelConnectionState, HotelConnection> stateContext = genericsFriendlyMock(StateContext.class);
        when(travellineClient.getHotelDetailsSync(any())).thenReturn(EntityCreatingUtils.hotelDetailsResponse(false, true, false, false));
        when(addressUnificationService.unifyAddressSync(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
        UpdateResult updateResult = service.actualizeHotelConnection(hotelConnection, stateContext);
        Assert.assertEquals(UpdateResult.ResultType.SUCCESS, updateResult.getType());
        List<HotelConnection> hotelConnectionList = hotelConnectionRepository.findAll();
        Assert.assertEquals(1, hotelConnectionList.size());
        hotelConnection = hotelConnectionList.get(0);
        LegalDetails legalDetails = hotelConnection.getLegalDetails();
        Assert.assertEquals(EntityCreatingUtils.INN, legalDetails.getInn());
        Assert.assertEquals(EntityCreatingUtils.KPP, legalDetails.getKpp());
        Assert.assertEquals(EntityCreatingUtils.BIC, legalDetails.getBic());
        Assert.assertEquals(EntityCreatingUtils.CURRENT_ACCOUNT, legalDetails.getPaymentAccount());
        Assert.assertEquals(EntityCreatingUtils.POST_CODE + ", " + EntityCreatingUtils.ADDRESS, legalDetails.getPostAddress());
        Assert.assertEquals(EntityCreatingUtils.PHONE, legalDetails.getPhone());
        List<LegalDetailsUpdate> legalDetailsUpdateList = legalDetailsUpdateRepository.findAll();
        Assert.assertEquals(1, legalDetailsUpdateList.size());
        LegalDetailsUpdate legalDetailsUpdate = legalDetailsUpdateList.get(0);
        Assert.assertEquals(legalDetails.getId(), legalDetailsUpdate.getLegalDetails().getId());
        Assert.assertEquals(EntityCreatingUtils.NEW_PHONE, legalDetailsUpdate.getPhone());
    }

    @Test
    public void actualizeHotelConnection_existingHotelWithSameDetailsMajorChange() {
        LegalDetails existingLegalDetails = existingLegalDetails(false);
        existingLegalDetails = legalDetailsRepository.save(existingLegalDetails);
        workflowRepository.save(existingLegalDetails.getWorkflow());
        HotelConnection hotelConnection = EntityCreatingUtils.hotelConnection();
        hotelConnection.setLegalDetails(existingLegalDetails);
        hotelConnection = hotelConnectionRepository.save(hotelConnection);
        StateContext<EHotelConnectionState, HotelConnection> stateContext = genericsFriendlyMock(StateContext.class);
        when(travellineClient.getHotelDetailsSync(any())).thenReturn(EntityCreatingUtils.hotelDetailsResponse(false, true, true, false));
        when(addressUnificationService.unifyAddressSync(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
        UpdateResult updateResult = service.actualizeHotelConnection(hotelConnection, stateContext);
        Assert.assertEquals(UpdateResult.ResultType.VALIDATION_REQUIRED, updateResult.getType());
        Assert.assertEquals(UpdateResult.ChangeRequisitesType.OTHER_CHANGE, updateResult.getChangeType());
        Assert.assertEquals(EntityCreatingUtils.INN, updateResult.getOldLegalDetails().getInn());
        Assert.assertEquals(EntityCreatingUtils.POST_CODE + ", " + EntityCreatingUtils.ADDRESS, updateResult.getOldLegalDetails().getLegalAddress());
        Assert.assertEquals(EntityCreatingUtils.PHONE, updateResult.getOldLegalDetails().getPhone());
        Assert.assertNull(updateResult.getExistingLegalDetails());
        Assert.assertEquals(EntityCreatingUtils.INN, updateResult.getNewLegalDetails().getInn());
        Assert.assertEquals(EntityCreatingUtils.POST_CODE + ", " + EntityCreatingUtils.NEW_ADDRESS, updateResult.getNewLegalDetails().getLegalAddress());
        Assert.assertEquals(EntityCreatingUtils.NEW_PHONE, updateResult.getNewLegalDetails().getPhone());
        List<HotelConnection> hotelConnectionList = hotelConnectionRepository.findAll();
        Assert.assertEquals(1, hotelConnectionList.size());
        hotelConnection = hotelConnectionList.get(0);
        LegalDetails legalDetails = hotelConnection.getLegalDetails();
        Assert.assertEquals(EntityCreatingUtils.INN, legalDetails.getInn());
        Assert.assertEquals(EntityCreatingUtils.KPP, legalDetails.getKpp());
        Assert.assertEquals(EntityCreatingUtils.BIC, legalDetails.getBic());
        Assert.assertEquals(EntityCreatingUtils.CURRENT_ACCOUNT, legalDetails.getPaymentAccount());
        Assert.assertEquals(EntityCreatingUtils.POST_CODE + ", " + EntityCreatingUtils.ADDRESS, legalDetails.getPostAddress());
        Assert.assertEquals(EntityCreatingUtils.PHONE, legalDetails.getPhone());
        Assert.assertTrue(legalDetailsUpdateRepository.findAll().isEmpty());
    }

    @Test
    public void actualizeHotelConnection_existingHotelWithDifferentDetailsWithPaperAgreement() {
        LegalDetails existingLegalDetails = existingLegalDetails(false);
        existingLegalDetails = legalDetailsRepository.save(existingLegalDetails);
        workflowRepository.save(existingLegalDetails.getWorkflow());
        HotelConnection hotelConnection = EntityCreatingUtils.hotelConnection();
        hotelConnection.setLegalDetails(existingLegalDetails);

        LegalDetails existingPaperLegalDetails = existingLegalDetails(true, false);
        existingPaperLegalDetails = legalDetailsRepository.save(existingPaperLegalDetails);
        workflowRepository.save(existingPaperLegalDetails.getWorkflow());

        hotelConnection = hotelConnectionRepository.save(hotelConnection);
        StateContext<EHotelConnectionState, HotelConnection> stateContext = genericsFriendlyMock(StateContext.class);
        when(travellineClient.getHotelDetailsSync(any())).thenReturn(EntityCreatingUtils.hotelDetailsResponse(true, false, false, false));
        when(addressUnificationService.unifyAddressSync(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
        when(agreementService.existsPaperAgreementForInn(EntityCreatingUtils.NEW_INN)).thenReturn(true);
        UpdateResult updateResult = service.actualizeHotelConnection(hotelConnection, stateContext);
        Assert.assertEquals(UpdateResult.ResultType.VALIDATION_REQUIRED, updateResult.getType());
        Assert.assertEquals(UpdateResult.ChangeRequisitesType.PAPER_AGREEMENT_CHANGE, updateResult.getChangeType());
        Assert.assertEquals(EntityCreatingUtils.INN, updateResult.getOldLegalDetails().getInn());
        Assert.assertEquals(EntityCreatingUtils.POST_CODE + ", " + EntityCreatingUtils.ADDRESS, updateResult.getOldLegalDetails().getLegalAddress());
        Assert.assertEquals(EntityCreatingUtils.PHONE, updateResult.getOldLegalDetails().getPhone());
        Assert.assertEquals(EntityCreatingUtils.NEW_INN, updateResult.getExistingLegalDetails().getInn());
        Assert.assertEquals(EntityCreatingUtils.POST_CODE + ", " + EntityCreatingUtils.ADDRESS, updateResult.getExistingLegalDetails().getLegalAddress());
        Assert.assertEquals(EntityCreatingUtils.PHONE, updateResult.getExistingLegalDetails().getPhone());
        Assert.assertEquals(EntityCreatingUtils.NEW_INN, updateResult.getNewLegalDetails().getInn());
        Assert.assertEquals(EntityCreatingUtils.POST_CODE + ", " + EntityCreatingUtils.ADDRESS, updateResult.getNewLegalDetails().getLegalAddress());
        Assert.assertEquals(EntityCreatingUtils.PHONE, updateResult.getNewLegalDetails().getPhone());
        List<HotelConnection> hotelConnectionList = hotelConnectionRepository.findAll();
        Assert.assertEquals(1, hotelConnectionList.size());
        hotelConnection = hotelConnectionList.get(0);
        LegalDetails legalDetails = hotelConnection.getLegalDetails();
        Assert.assertEquals(EntityCreatingUtils.INN, legalDetails.getInn());
        Assert.assertEquals(EntityCreatingUtils.KPP, legalDetails.getKpp());
        Assert.assertEquals(EntityCreatingUtils.BIC, legalDetails.getBic());
        Assert.assertEquals(EntityCreatingUtils.CURRENT_ACCOUNT, legalDetails.getPaymentAccount());
        Assert.assertEquals(EntityCreatingUtils.POST_CODE + ", " + EntityCreatingUtils.ADDRESS, legalDetails.getPostAddress());
        Assert.assertEquals(EntityCreatingUtils.PHONE, legalDetails.getPhone());
        List<LegalDetailsUpdate> legalDetailsUpdateList = legalDetailsUpdateRepository.findAll();
        Assert.assertTrue(legalDetailsUpdateList.isEmpty());

        ApplyUpdateResult applyUpdateResult = service.applyHotelConnectionUpdate(hotelConnection, updateResult.getHotelConnectionUpdate(),
                genericsFriendlyMock(MessagingContext.class), true);
        Assert.assertFalse(applyUpdateResult.isLegalDetailsUpdated());
        Assert.assertEquals(hotelConnection.getLegalDetails().getId(), existingPaperLegalDetails.getId());
    }

    @Test
    public void actualizeHotelConnection_existingHotelWithDifferentDetailsWhichAreNotFound() {
        LegalDetails existingLegalDetails = existingLegalDetails(false);
        existingLegalDetails = legalDetailsRepository.save(existingLegalDetails);
        workflowRepository.save(existingLegalDetails.getWorkflow());
        HotelConnection hotelConnection = EntityCreatingUtils.hotelConnection();
        hotelConnection.setLegalDetails(existingLegalDetails);
        hotelConnection = hotelConnectionRepository.save(hotelConnection);
        StateContext<EHotelConnectionState, HotelConnection> stateContext = genericsFriendlyMock(StateContext.class);
        when(travellineClient.getHotelDetailsSync(any())).thenReturn(EntityCreatingUtils.hotelDetailsResponse(true, false, true, false));
        when(addressUnificationService.unifyAddressSync(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
        UpdateResult updateResult = service.actualizeHotelConnection(hotelConnection, stateContext);
        Assert.assertEquals(UpdateResult.ResultType.VALIDATION_REQUIRED, updateResult.getType());
        Assert.assertEquals(UpdateResult.ChangeRequisitesType.INN_CHANGE, updateResult.getChangeType());
        Assert.assertEquals(EntityCreatingUtils.INN, updateResult.getOldLegalDetails().getInn());
        Assert.assertEquals(EntityCreatingUtils.POST_CODE + ", " + EntityCreatingUtils.ADDRESS, updateResult.getOldLegalDetails().getLegalAddress());
        Assert.assertEquals(EntityCreatingUtils.PHONE, updateResult.getOldLegalDetails().getPhone());
        Assert.assertNull(updateResult.getExistingLegalDetails());
        Assert.assertEquals(EntityCreatingUtils.NEW_INN, updateResult.getNewLegalDetails().getInn());
        Assert.assertEquals(EntityCreatingUtils.POST_CODE + ", " + EntityCreatingUtils.NEW_ADDRESS, updateResult.getNewLegalDetails().getLegalAddress());
        Assert.assertEquals(EntityCreatingUtils.PHONE, updateResult.getNewLegalDetails().getPhone());
        List<HotelConnection> hotelConnectionList = hotelConnectionRepository.findAll();
        Assert.assertEquals(1, hotelConnectionList.size());
        HotelConnectionUpdate connectionUpdate = updateResult.getHotelConnectionUpdate();
        Assert.assertEquals(EntityCreatingUtils.NEW_INN, connectionUpdate.getInn());
        Assert.assertEquals(EntityCreatingUtils.KPP, connectionUpdate.getKpp());
        Assert.assertEquals(EntityCreatingUtils.BIC, connectionUpdate.getBic());
        Assert.assertEquals(EntityCreatingUtils.CURRENT_ACCOUNT, connectionUpdate.getPaymentAccount());
        Assert.assertEquals(EntityCreatingUtils.POST_CODE + ", " + EntityCreatingUtils.NEW_ADDRESS, connectionUpdate.getLegalAddress());
        Assert.assertEquals(EntityCreatingUtils.PHONE, connectionUpdate.getLegalPhone());
        List<LegalDetailsUpdate> legalDetailsUpdateList = legalDetailsUpdateRepository.findAll();
        Assert.assertEquals(0, legalDetailsUpdateList.size());
    }

    @Test
    public void actualizeHotelConnection_existingHotelWithDifferentDetailsWhichAreFoundWithChange() {
        LegalDetails newExistingDetails = existingLegalDetails(true);
        legalDetailsRepository.save(newExistingDetails);
        workflowRepository.save(newExistingDetails.getWorkflow());
        LegalDetails existingLegalDetails = existingLegalDetails(false);
        existingLegalDetails = legalDetailsRepository.save(existingLegalDetails);
        workflowRepository.save(existingLegalDetails.getWorkflow());
        HotelConnection hotelConnection = EntityCreatingUtils.hotelConnection();
        hotelConnection.setLegalDetails(existingLegalDetails);
        hotelConnection = hotelConnectionRepository.save(hotelConnection);
        StateContext<EHotelConnectionState, HotelConnection> stateContext = genericsFriendlyMock(StateContext.class);
        when(travellineClient.getHotelDetailsSync(any())).thenReturn(EntityCreatingUtils.hotelDetailsResponse(true, true, true, false));
        when(addressUnificationService.unifyAddressSync(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
        UpdateResult updateResult = service.actualizeHotelConnection(hotelConnection, stateContext);
        Assert.assertEquals(UpdateResult.ResultType.VALIDATION_REQUIRED, updateResult.getType());
        Assert.assertEquals(UpdateResult.ChangeRequisitesType.OTHER_CHANGE, updateResult.getChangeType());
        Assert.assertEquals(EntityCreatingUtils.INN, updateResult.getOldLegalDetails().getInn());
        Assert.assertEquals(EntityCreatingUtils.POST_CODE + ", " + EntityCreatingUtils.ADDRESS, updateResult.getOldLegalDetails().getLegalAddress());
        Assert.assertEquals(EntityCreatingUtils.PHONE, updateResult.getOldLegalDetails().getPhone());
        Assert.assertEquals(EntityCreatingUtils.NEW_INN, updateResult.getExistingLegalDetails().getInn());
        Assert.assertEquals(EntityCreatingUtils.POST_CODE + ", " + EntityCreatingUtils.ADDRESS, updateResult.getExistingLegalDetails().getLegalAddress());
        Assert.assertEquals(EntityCreatingUtils.PHONE, updateResult.getExistingLegalDetails().getPhone());
        Assert.assertEquals(EntityCreatingUtils.NEW_INN, updateResult.getNewLegalDetails().getInn());
        Assert.assertEquals(EntityCreatingUtils.POST_CODE + ", " + EntityCreatingUtils.NEW_ADDRESS, updateResult.getNewLegalDetails().getLegalAddress());
        Assert.assertEquals(EntityCreatingUtils.NEW_PHONE, updateResult.getNewLegalDetails().getPhone());
        List<HotelConnection> hotelConnectionList = hotelConnectionRepository.findAll();
        Assert.assertEquals(1, hotelConnectionList.size());
        HotelConnectionUpdate connectionUpdate = updateResult.getHotelConnectionUpdate();
        Assert.assertNotNull(connectionUpdate);
        Assert.assertEquals(EntityCreatingUtils.NEW_INN, connectionUpdate.getInn());
        Assert.assertEquals(EntityCreatingUtils.KPP, connectionUpdate.getKpp());
        Assert.assertEquals(EntityCreatingUtils.BIC, connectionUpdate.getBic());
        Assert.assertEquals(EntityCreatingUtils.CURRENT_ACCOUNT, connectionUpdate.getPaymentAccount());
        Assert.assertEquals(EntityCreatingUtils.POST_CODE + ", " + EntityCreatingUtils.NEW_ADDRESS, connectionUpdate.getLegalAddress());
        Assert.assertEquals(EntityCreatingUtils.NEW_PHONE, connectionUpdate.getLegalPhone());
        List<LegalDetailsUpdate> legalDetailsUpdateList = legalDetailsUpdateRepository.findAll();
        Assert.assertTrue(legalDetailsUpdateList.isEmpty());
    }

    @Test
    public void actualizeHotelConnection_uniqueConstraintException() {
        StateContext<EHotelConnectionState, HotelConnection> stateContext = genericsFriendlyMock(StateContext.class);
        doReturn(EntityCreatingUtils.hotelDetailsResponse(false, false, false, false)).when(travellineClient).getHotelDetailsSync(any());
        doReturn(null).when(legalDetailsRepositorySpy)
                .findByInnAndKppAndBicAndPaymentAccount(any(), any(), any(), any());

        service.actualizeHotelConnection(EntityCreatingUtils.hotelConnection(), stateContext);
        assertThatThrownBy(() -> service.actualizeHotelConnection(EntityCreatingUtils.hotelConnection(), stateContext))
                .isExactlyInstanceOf(RetryableServiceException.class)
                .hasMessageContaining("Someone is trying to insert the same legal details");
    }

    @Test
    public void actualizeHotelConnection_pessimisticLockingException() {
        StateContext<EHotelConnectionState, HotelConnection> stateContext = genericsFriendlyMock(StateContext.class);
        doReturn(EntityCreatingUtils.hotelDetailsResponse(false, false, false, false)).when(travellineClient).getHotelDetailsSync(any());
        doReturn(null).when(legalDetailsRepositorySpy)
                .findByInnAndKppAndBicAndPaymentAccount(any(), any(), any(), any());

        CyclicBarrier dbSaveBarrier = new CyclicBarrier(2);
        CompletableFuture<Void> firstReady = new CompletableFuture<>();
        CompletableFuture<Void> secondReady = new CompletableFuture<>();
        doAnswer(invocation -> {
            log.info("{} has arrived to the saveAndFlush point", threadName());
            dbSaveBarrier.await();
            switch (threadId.get()) {
                case 1: {
                    log.info("{} starts inserting first", threadName());
                    Object result = forwardMethodCall(invocation, legalDetailsRepository);
                    log.info("{} has inserted the row and lets the other thread do the same", threadName());
                    firstReady.complete(null);
                    secondReady.join();
                    return result;
                }
                case 2: {
                    log.info("{} will try inserting the row after the other thread", threadName());
                    firstReady.join();
                    log.info("{} has received the notification from the other thread, starts inserting", threadName());
                    try {
                        return forwardMethodCall(invocation, legalDetailsRepository);
                    } finally {
                        secondReady.complete(null);
                    }
                }
                default:
                    throw new IllegalStateException("Unexpected thread id: " + threadId.get());
            }
        }).when(legalDetailsRepositorySpy).saveAndFlush(any());

        AtomicInteger tidGen = new AtomicInteger(0);
        runInParallel(dbSaveBarrier.getParties(), () -> runInTxAndRollback(() -> {
            threadId.set(tidGen.incrementAndGet());
            switch (threadId.get()) {
                case 1: {
                    service.actualizeHotelConnection(EntityCreatingUtils.hotelConnection(), stateContext);
                    log.info("{} has successfully completed", threadName());
                    break;
                }
                case 2: {
                    assertThatThrownBy(() -> service.actualizeHotelConnection(EntityCreatingUtils.hotelConnection(), stateContext))
                            .isExactlyInstanceOf(RetryableServiceException.class)
                            .hasMessageContaining("Someone is trying to insert the same legal details");
                    log.info("{} has completed with a RetryableServiceException", threadName());
                    break;
                }
                default:
                    throw new IllegalStateException("Unexpected thread id: " + threadId.get());
            }
        }));
    }

    private Object forwardMethodCall(InvocationOnMock invocation, Object object) {
        try {
            return invocation.getMethod().invoke(object, invocation.getArguments());
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw ExceptionUtils.throwException(e.getCause());
        }
    }

    private String threadName() {
        return "[T" + threadId.get() + "]";
    }

    private void runInParallel(int times, Runnable task) {
        ExecutorService executor = Executors.newFixedThreadPool(times);
        List<Future<?>> tasks = new ArrayList<>();
        for (int i = 0; i < times; i++) {
            tasks.add(executor.submit(task));
        }
        MoreExecutors.shutdownAndAwaitTermination(executor, 10, TimeUnit.SECONDS);
        for (Future<?> future : tasks) {
            try {
                // receiving any errors generated by the tasks
                future.get();
            } catch (Exception e) {
                throw new RuntimeException(e instanceof ExecutionException ? e.getCause() : e);
            }
        }
    }

    private void runInTxAndRollback(Runnable action) {
        transactionTemplate.execute(txStatus -> {
            action.run();
            txStatus.setRollbackOnly();
            return null;
        });
    }

    private LegalDetails existingLegalDetails(boolean newInn) {
        return existingLegalDetails(newInn, true);
    }

    private LegalDetails existingLegalDetails(boolean newInn, boolean managedByHotelsAdministrator) {
        LegalDetails legalDetails = EntityCreatingUtils.legalDetails(newInn, managedByHotelsAdministrator);
        Workflow workflow = Workflow.createWorkflowForEntity(legalDetails);
        workflowRepository.save(workflow);
        return legalDetails;
    }
}
