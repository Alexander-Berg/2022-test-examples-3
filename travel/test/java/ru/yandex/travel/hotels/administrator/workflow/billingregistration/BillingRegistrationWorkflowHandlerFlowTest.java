package ru.yandex.travel.hotels.administrator.workflow.billingregistration;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import javax.persistence.EntityManager;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.MockBeans;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.travel.commons.http.apiclient.HttpApiRetryableException;
import ru.yandex.travel.hotels.administrator.cache.HotelClusteringDictionary;
import ru.yandex.travel.hotels.administrator.entity.BillingRegistration;
import ru.yandex.travel.hotels.administrator.entity.KnownWorkflow;
import ru.yandex.travel.hotels.administrator.entity.LegalDetails;
import ru.yandex.travel.hotels.administrator.repository.BillingRegistrationRepository;
import ru.yandex.travel.hotels.administrator.service.ClusterizationService;
import ru.yandex.travel.hotels.administrator.task.HotelsYtPublishTask;
import ru.yandex.travel.hotels.administrator.workflow.proto.EBillingRegistrationState;
import ru.yandex.travel.hotels.administrator.workflow.proto.ELegalDetailsState;
import ru.yandex.travel.hotels.administrator.workflow.proto.TBillingRegistrationFinish;
import ru.yandex.travel.hotels.administrator.workflow.proto.TBillingRegistrationStart;
import ru.yandex.travel.hotels.administrator.workflow.proto.TBillingRegistrationStartUpdate;
import ru.yandex.travel.hotels.administrator.workflow.proto.TBillingRegistrationUpdateFinished;
import ru.yandex.travel.hotels.common.PartnerConfigService;
import ru.yandex.travel.hotels.geosearch.GeoSearchService;
import ru.yandex.travel.hotels.proto.EPartnerId;
import ru.yandex.travel.integration.balance.BillingApiClient;
import ru.yandex.travel.integration.balance.responses.BillingCreateContractResponse;
import ru.yandex.travel.testing.misc.MockitoUtils;
import ru.yandex.travel.workflow.EWorkflowState;
import ru.yandex.travel.workflow.WorkflowEventQueue;
import ru.yandex.travel.workflow.entities.Workflow;
import ru.yandex.travel.workflow.entities.WorkflowEvent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static ru.yandex.travel.testing.TestUtils.waitForState;

@RunWith(SpringRunner.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK
)
@ActiveProfiles("test")
@DirtiesContext
@MockBeans({
        @MockBean(GeoSearchService.class),
        @MockBean(HotelClusteringDictionary.class),
        @MockBean(HotelsYtPublishTask.class),
        @MockBean(ClusterizationService.class),
        @MockBean(PartnerConfigService.class),
})
public class BillingRegistrationWorkflowHandlerFlowTest {
    public static final Duration TIMEOUT = Duration.ofSeconds(10);
    public static final Duration RETRY = Duration.ofMillis(10);

    @Autowired
    private TransactionTemplate transactionTemplate;
    @Autowired
    private BillingRegistrationRepository billingRegistrationRepository;
    @Autowired
    private WorkflowEventQueue workflowEventQueue;
    @Autowired
    private EntityManager em;

    @MockBean
    private BillingApiClient apiClient;

    @Test
    public void testRegistration_happyPath() {
        UUID registrationId = UUID.randomUUID();

        doReturn(765423434244L)
                .when(apiClient).createClient(anyLong(), any());
        doThrow(new HttpApiRetryableException("some DC has burned down", null, -1, null))
                .doThrow(new HttpApiRetryableException("another one has burned down too", null, -1, null))
                .doReturn(837646246238L)
                .when(apiClient).createPerson(anyLong(), any());
        doReturn(BillingCreateContractResponse.builder().contractId(273456264324L).externalId("ext_id/#54342").build())
                .when(apiClient).createContract(anyLong(), any());

        // starting a new registration workflow
        runInTx(() -> {
            LegalDetails legalDetails = createLegalDetails("br_wf_create_hp");
            BillingRegistration registration = BillingRegistration.builder()
                    .id(registrationId)
                    .legalDetails(legalDetails)
                    .state(EBillingRegistrationState.BRS_NEW)
                    .parentWorkflowId(legalDetails.getWorkflow().getId())
                    .build();
            Workflow workflow = Workflow.createWorkflowForEntity(registration,
                    KnownWorkflow.GENERIC_SUPERVISOR.getUuid());
            em.persist(workflow);
            billingRegistrationRepository.saveAndFlush(registration);
            workflowEventQueue.enqueueMessage(workflow.getId(), TBillingRegistrationStart.newBuilder().build());
        });

        waitForState("Partner Registered", Duration.ofSeconds(10), () -> callInTx(() -> {
            BillingRegistration registration = billingRegistrationRepository.getOne(registrationId);
            return registration.getState() == EBillingRegistrationState.BRS_REGISTERED;
        }));

        runInTx(() -> {
            BillingRegistration registration = billingRegistrationRepository.getOne(registrationId);
            assertThat(registration.getClientId()).isEqualTo(765423434244L);
            assertThat(registration.getPersonId()).isEqualTo(837646246238L);
            assertThat(registration.getContractId()).isEqualTo(273456264324L);
            assertThat(registration.getExternalContractId()).isEqualTo("ext_id/#54342");
            assertThat(registration.getRegisteredAt()).isNotNull();

            Optional<WorkflowEvent> wfEvent = workflowEventQueue.peekEvent(registration.getParentWorkflowId());
            assertThat(wfEvent).isPresent();
            assertThat(wfEvent.get().getData()).isExactlyInstanceOf(TBillingRegistrationFinish.class);
            TBillingRegistrationFinish event = (TBillingRegistrationFinish) wfEvent.get().getData();
            assertThat(event.getClientId()).isEqualTo(765423434244L);
            assertThat(event.getPersonId()).isEqualTo(837646246238L);
            assertThat(event.getContractId()).isEqualTo(273456264324L);
            assertThat(event.getExternalContractId()).isEqualTo("ext_id/#54342");
        });

        verify(apiClient, times(3)).createPerson(anyLong(), any());
    }

    @Test
    public void testLegalDetailsUpdate_happyPath() {
        UUID registrationId = UUID.randomUUID();
        long clientId = 912698364234L;
        long personId = 916246123943L;

        // imitating an existing registered entity and its workflow, starting update
        runInTx(() -> {
            LegalDetails legalDetails = createLegalDetails("br_wf_update_hp");
            legalDetails.setBalanceClientId(clientId);
            legalDetails.setBalancePersonId(personId);
            em.persist(legalDetails);
            BillingRegistration registration = BillingRegistration.builder()
                    .id(registrationId)
                    .legalDetails(legalDetails)
                    .state(EBillingRegistrationState.BRS_REGISTERED)
                    .parentWorkflowId(legalDetails.getWorkflow().getId())
                    .clientId(clientId)
                    .personId(personId)
                    .build();
            Workflow workflow = Workflow.createWorkflowForEntity(registration,
                    KnownWorkflow.GENERIC_SUPERVISOR.getUuid());
            em.persist(workflow);
            billingRegistrationRepository.saveAndFlush(registration);
            workflowEventQueue.enqueueMessage(workflow.getId(), TBillingRegistrationStartUpdate.newBuilder()
                    .setNewParentWorkflowId(legalDetails.getWorkflow().getId().toString())
                    .build());
        });

        int initialApiCalls = 0;
        int targetApiCalls = 1;
        // we need to make sure that the workflow left the BRS_REGISTERED state first
        MockitoUtils.waitForMockCalls(apiClient,
                mock -> mock.updateClient(anyLong(), argThat(arg -> arg.getClientId() == clientId)),
                initialApiCalls, targetApiCalls, TIMEOUT, RETRY, "Update started");
        // now we can wait for for the workflow to return to the initial BRS_REGISTERED state
        waitForState("Legal Data synchronized", TIMEOUT, () -> callInTx(() -> {
            BillingRegistration registration = billingRegistrationRepository.getOne(registrationId);
            return registration.getState() == EBillingRegistrationState.BRS_REGISTERED;
        }));

        runInTx(() -> {
            BillingRegistration registration = billingRegistrationRepository.getOne(registrationId);
            assertThat(registration.getClientId()).isEqualTo(clientId);
            assertThat(registration.getPersonId()).isEqualTo(personId);

            Optional<WorkflowEvent> wfEvent = workflowEventQueue.peekEvent(registration.getParentWorkflowId());
            assertThat(wfEvent).isPresent();
            assertThat(wfEvent.get().getData()).isExactlyInstanceOf(TBillingRegistrationUpdateFinished.class);
        });

        verify(apiClient).updateClient(anyLong(), argThat(arg -> arg.getClientId() == clientId));
        verify(apiClient).updatePerson(anyLong(), argThat(arg -> arg.getClientId() == clientId));
    }

    private LegalDetails createLegalDetails(String inn) {
        return callInTx(() -> {
            LegalDetails legalDetails = LegalDetails.builder()
                    .id(UUID.randomUUID())
                    .partnerId(EPartnerId.PI_TRAVELLINE)
                    .inn(inn)
                    .kpp("kpp")
                    .bic("bic")
                    .paymentAccount("payment_account")
                    .legalPostCode("111112")
                    .legalAddress("Москва, ул. Льва Толстого, 777")
                    .postCode("111111")
                    .postAddress("Москва, ул. Льва Толстого, 999")
                    .phone("7 495 999-99-99")
                    .state(ELegalDetailsState.DS_REGISTERING)
                    .managedByAdministrator(true)
                    .build();

            Workflow workflow = Workflow.createWorkflowForEntity(legalDetails,
                    KnownWorkflow.GENERIC_SUPERVISOR.getUuid());
            // to safely receive final events
            workflow.setState(EWorkflowState.WS_PAUSED);
            em.persist(workflow);
            em.persist(legalDetails);
            return legalDetails;
        });
    }

    private void runInTx(Runnable action) {
        callInTx(() -> {
            action.run();
            return null;
        });
    }

    private <T> T callInTx(Supplier<T> action) {
        return transactionTemplate.execute(txStatus -> action.get());
    }
}
