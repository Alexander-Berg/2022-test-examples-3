package ru.yandex.travel.hotels.administrator;

import java.util.UUID;

import io.grpc.stub.StreamObserver;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.travel.hotels.administrator.entity.HotelConnection;
import ru.yandex.travel.hotels.administrator.entity.HotelConnectionStep;
import ru.yandex.travel.hotels.administrator.entity.LegalDetails;
import ru.yandex.travel.hotels.administrator.entity.VerifyClusteringStep;
import ru.yandex.travel.hotels.administrator.grpc.proto.EHotelStatus;
import ru.yandex.travel.hotels.administrator.grpc.proto.EUnpublishedReason;
import ru.yandex.travel.hotels.administrator.grpc.proto.THotelStatusReq;
import ru.yandex.travel.hotels.administrator.grpc.proto.THotelStatusRsp;
import ru.yandex.travel.hotels.administrator.repository.ConnectionStepRepository;
import ru.yandex.travel.hotels.administrator.repository.HotelConnectionRepository;
import ru.yandex.travel.hotels.administrator.repository.LegalDetailsRepository;
import ru.yandex.travel.hotels.administrator.workflow.proto.EConnectionStepState;
import ru.yandex.travel.hotels.administrator.workflow.proto.EHotelConnectionState;
import ru.yandex.travel.hotels.administrator.workflow.proto.ELegalDetailsState;
import ru.yandex.travel.hotels.proto.EPartnerId;
import ru.yandex.travel.workflow.EWorkflowState;
import ru.yandex.travel.workflow.entities.Workflow;
import ru.yandex.travel.workflow.repository.WorkflowRepository;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;

@RunWith(SpringRunner.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK
)
//@TestExecutionListeners(
//        listeners = TruncateDatabaseTestExecutionListener.class,
//        mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
@ActiveProfiles("test")
public class AdministratorServiceTest {

    private static final String HOTEL_CODE = "123";

    @Autowired
    private AdministratorService administratorService;

    @Autowired
    private HotelConnectionRepository hotelConnectionRepository;

    @Autowired
    private WorkflowRepository workflowRepository;

    @Autowired
    private LegalDetailsRepository legalDetailsRepository;

    @Autowired
    private ConnectionStepRepository connectionStepRepository;

    @Mock
    private StreamObserver<THotelStatusRsp> streamObserver;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Before
    public void init() {
        transactionTemplate.execute(ignored -> {
            HotelConnection hotelConnection = hotelConnection();
            LegalDetails legalDetails = legalDetails();
            hotelConnection.setLegalDetails(legalDetails);
            legalDetailsRepository.saveAndFlush(legalDetails);
            hotelConnectionRepository.saveAndFlush(hotelConnection);
            return null;
        });
    }

    @After
    public void tearDown() {
        transactionTemplate.execute(ignored -> {
            connectionStepRepository.deleteAll();
            hotelConnectionRepository.deleteAll();
            legalDetailsRepository.deleteAll();
            workflowRepository.deleteAll();
            return null;
        });
    }

    @Test
    public void testHotelNotFound() {
        administratorService.hotelStatus(THotelStatusReq.newBuilder()
                        .setPartnerId(EPartnerId.PI_TRAVELLINE)
                        .setHotelCode("MISSING_HOTEL_CODE")
                        .build(),
                streamObserver);
        verify(streamObserver).onNext(argThat(response -> {
            Assert.assertEquals("MISSING_HOTEL_CODE", response.getHotelCode());
            Assert.assertEquals(EHotelStatus.H_NOT_FOUND, response.getHotelStatus());
            Assert.assertEquals(EUnpublishedReason.UR_NONE, response.getUnpublishedReason());
            return true;
        }));
    }

    @Test
    public void testNewHotel() {
        administratorService.hotelStatus(THotelStatusReq.newBuilder()
                        .setPartnerId(EPartnerId.PI_TRAVELLINE)
                        .setHotelCode(HOTEL_CODE)
                        .build(),
                streamObserver);
        verify(streamObserver).onNext(argThat(response -> {
            Assert.assertEquals(HOTEL_CODE, response.getHotelCode());
            Assert.assertEquals(EHotelStatus.H_PUBLISHING, response.getHotelStatus());
            Assert.assertEquals(EUnpublishedReason.UR_NONE, response.getUnpublishedReason());
            return true;
        }));
    }

    @Test
    public void testPublishingHotel() {
        changeHotelConnectionState(EHotelConnectionState.CS_PUBLISHING);
        administratorService.hotelStatus(THotelStatusReq.newBuilder()
                        .setPartnerId(EPartnerId.PI_TRAVELLINE)
                        .setHotelCode(HOTEL_CODE)
                        .build(),
                streamObserver);
        verify(streamObserver).onNext(argThat(response -> {
            Assert.assertEquals(HOTEL_CODE, response.getHotelCode());
            Assert.assertEquals(EHotelStatus.H_PUBLISHING, response.getHotelStatus());
            Assert.assertEquals(EUnpublishedReason.UR_NONE, response.getUnpublishedReason());
            return true;
        }));
    }

    @Test
    public void testPublishingHotelWithOpenedTicket() {
        transactionTemplate.execute(ignored -> {
            HotelConnection hotelConnection = hotelConnectionRepository.findAll().get(0);
            hotelConnection.setState(EHotelConnectionState.CS_PUBLISHING);
            hotelConnectionRepository.saveAndFlush(hotelConnection);
            HotelConnectionStep connectionStep = hotelConnectionStep();
            connectionStep.setHotelConnection(hotelConnection);
            connectionStepRepository.saveAndFlush(connectionStep);
            return null;
        });
        administratorService.hotelStatus(THotelStatusReq.newBuilder()
                        .setPartnerId(EPartnerId.PI_TRAVELLINE)
                        .setHotelCode(HOTEL_CODE)
                        .build(),
                streamObserver);
        verify(streamObserver).onNext(argThat(response -> {
            Assert.assertEquals(HOTEL_CODE, response.getHotelCode());
            Assert.assertEquals(EHotelStatus.H_PUBLISHING, response.getHotelStatus());
            Assert.assertEquals(EUnpublishedReason.UR_DELAYED, response.getUnpublishedReason());
            return true;
        }));
    }

    @Test
    public void testNewHotelWithFailedWorkflow() {
        HotelConnection hotelConnection = hotelConnectionRepository.findAll().get(0);
        hotelConnection.getWorkflow().setState(EWorkflowState.WS_CRASHED);
        workflowRepository.saveAndFlush(hotelConnection.getWorkflow());
        administratorService.hotelStatus(THotelStatusReq.newBuilder()
                        .setPartnerId(EPartnerId.PI_TRAVELLINE)
                        .setHotelCode(HOTEL_CODE)
                        .build(),
                streamObserver);
        verify(streamObserver).onNext(argThat(response -> {
            Assert.assertEquals(HOTEL_CODE, response.getHotelCode());
            Assert.assertEquals(EHotelStatus.H_PUBLISHING, response.getHotelStatus());
            Assert.assertEquals(EUnpublishedReason.UR_DELAYED, response.getUnpublishedReason());
            return true;
        }));
    }

    @Test
    public void testPublishedHotel() {
        changeHotelConnectionState(EHotelConnectionState.CS_PUBLISHED);
        administratorService.hotelStatus(THotelStatusReq.newBuilder()
                        .setPartnerId(EPartnerId.PI_TRAVELLINE)
                        .setHotelCode(HOTEL_CODE)
                        .build(),
                streamObserver);
        verify(streamObserver).onNext(argThat(response -> {
            Assert.assertEquals(HOTEL_CODE, response.getHotelCode());
            Assert.assertEquals(EHotelStatus.H_PUBLISHED, response.getHotelStatus());
            Assert.assertEquals(EUnpublishedReason.UR_NONE, response.getUnpublishedReason());
            return true;
        }));
    }

    @Test
    public void testManualVerificationHotel() {
        changeHotelConnectionState(EHotelConnectionState.CS_MANUAL_VERIFICATION);
        administratorService.hotelStatus(THotelStatusReq.newBuilder()
                        .setPartnerId(EPartnerId.PI_TRAVELLINE)
                        .setHotelCode(HOTEL_CODE)
                        .build(),
                streamObserver);
        verify(streamObserver).onNext(argThat(response -> {
            Assert.assertEquals(HOTEL_CODE, response.getHotelCode());
            Assert.assertEquals(EHotelStatus.H_UNPUBLISHED, response.getHotelStatus());
            Assert.assertEquals(EUnpublishedReason.UR_UPDATING, response.getUnpublishedReason());
            return true;
        }));
    }

    @Test
    public void testUnpublishedNoAgreementHotel() {
        transactionTemplate.execute(ignored -> {
            HotelConnection hotelConnection = hotelConnectionRepository.findAll().get(0);
            hotelConnection.setState(EHotelConnectionState.CS_UNPUBLISHED);
            hotelConnectionRepository.saveAndFlush(hotelConnection);
            LegalDetails legalDetails = hotelConnection.getLegalDetails();
            legalDetails.setOfferAccepted(false);
            legalDetailsRepository.saveAndFlush(legalDetails);
            return null;
        });
        administratorService.hotelStatus(THotelStatusReq.newBuilder()
                        .setPartnerId(EPartnerId.PI_TRAVELLINE)
                        .setHotelCode(HOTEL_CODE)
                        .build(),
                streamObserver);
        verify(streamObserver).onNext(argThat(response -> {
            Assert.assertEquals(HOTEL_CODE, response.getHotelCode());
            Assert.assertEquals(EHotelStatus.H_UNPUBLISHED, response.getHotelStatus());
            Assert.assertEquals(EUnpublishedReason.UR_NO_AGREEMENT, response.getUnpublishedReason());
            return true;
        }));
    }

    @Test
    public void testUnpublishedSuspendedHotel() {
        changeHotelConnectionState(EHotelConnectionState.CS_UNPUBLISHED);
        administratorService.hotelStatus(THotelStatusReq.newBuilder()
                        .setPartnerId(EPartnerId.PI_TRAVELLINE)
                        .setHotelCode(HOTEL_CODE)
                        .build(),
                streamObserver);
        verify(streamObserver).onNext(argThat(response -> {
            Assert.assertEquals(HOTEL_CODE, response.getHotelCode());
            Assert.assertEquals(EHotelStatus.H_UNPUBLISHED, response.getHotelStatus());
            Assert.assertEquals(EUnpublishedReason.UR_SUSPENDED, response.getUnpublishedReason());
            return true;
        }));
    }

    private HotelConnection hotelConnection() {
        HotelConnection connection = new HotelConnection();
        connection.setId(UUID.randomUUID());
        connection.setHotelCode(HOTEL_CODE);
        connection.setPartnerId(EPartnerId.PI_TRAVELLINE);
        connection.setState(EHotelConnectionState.CS_NEW);
        Workflow workflow = Workflow.createWorkflowForEntity(connection);
        workflowRepository.save(workflow);
        return connection;
    }

    private LegalDetails legalDetails() {
        LegalDetails legalDetails = LegalDetails.builder()
                .id(UUID.randomUUID())
                .inn("inn")
                .kpp("kpp")
                .bic("bic")
                .paymentAccount("paymentAccount")
                .legalName("legalName")
                .legalPostCode("legalPostCode")
                .legalAddress("legalAddress")
                .postCode("postCode")
                .postAddress("postAddress")
                .phone("phone")
                .balanceExternalContractId("externalContract/20")
                .state(ELegalDetailsState.DS_NEW)
                .partnerId(EPartnerId.PI_TRAVELLINE)
                .offerAccepted(true)
                .managedByAdministrator(true)
                .build();
        Workflow workflow = Workflow.createWorkflowForEntity(legalDetails);
        workflowRepository.save(workflow);
        return legalDetails;
    }

    private HotelConnectionStep hotelConnectionStep() {
        VerifyClusteringStep step = new VerifyClusteringStep();
        step.setId(UUID.randomUUID());
        step.setPartnerId(EPartnerId.PI_TRAVELLINE);
        step.setHotelCode(HOTEL_CODE);
        step.setState(EConnectionStepState.CSS_WAIT_FOR_TICKET_RESOLUTION);
        step.setParentWorkflowId(UUID.randomUUID());
        return step;
    }

    private void changeHotelConnectionState(EHotelConnectionState targetState) {
        transactionTemplate.execute(ignored -> {
            HotelConnection hotelConnection = hotelConnectionRepository.findAll().get(0);
            hotelConnection.setState(targetState);
            hotelConnectionRepository.saveAndFlush(hotelConnection);
            return null;
        });
    }
}
