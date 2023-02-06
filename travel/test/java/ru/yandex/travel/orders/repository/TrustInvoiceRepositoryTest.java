package ru.yandex.travel.orders.repository;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.persistence.EntityManager;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.travel.orders.entities.TrustInvoice;
import ru.yandex.travel.orders.workflow.invoice.proto.ETrustInvoiceState;
import ru.yandex.travel.workflow.EWorkflowState;
import ru.yandex.travel.workflow.entities.Workflow;
import ru.yandex.travel.workflow.repository.WorkflowRepository;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@DataJpaTest
@ActiveProfiles("test")
public class TrustInvoiceRepositoryTest {
    @Autowired
    private TrustInvoiceRepository trustInvoiceRepository;

    @Autowired
    private WorkflowRepository workflowRepository;

    @Autowired
    private EntityManager em;

    @Test
    public void testNotFoundByIdWithRefreshScheduled() {
        TrustInvoice entity = new TrustInvoice();
        entity.setId(UUID.randomUUID());
        entity.setBackgroundJobActive(true);
        trustInvoiceRepository.save(entity);
        trustInvoiceRepository.flush();

        assertThat(trustInvoiceRepository.findByIdAndBackgroundJobActive(entity.getId(), false)).isEmpty();
    }

    @Test
    public void testGetInvoiceWorkflowsWaitingForClearance() {

        TrustInvoice invoice1 = createInvoiceWithWorkflow(ETrustInvoiceState.IS_HOLD);
        invoice1.setClearAt(Instant.now().minus(1, ChronoUnit.MINUTES));
        TrustInvoice invoice2 = createInvoiceWithWorkflow(ETrustInvoiceState.IS_NEW);
        em.flush();
        em.clear();


        List<UUID> invoiceList = trustInvoiceRepository.getInvoiceIdsWaitingClearingWithExclusions(Instant.now(),
                ETrustInvoiceState.IS_HOLD, EWorkflowState.WS_RUNNING, Set.of(), PageRequest.of(0, 10));
        assertThat(invoiceList).hasSize(1);
        assertThat(invoiceList).contains(invoice1.getId());
    }

    @Test
    public void testGetInvoiceWorkflowsWaitingRefresh() {
        TrustInvoice invoice1 = createInvoiceWithWorkflowForBackgroundJob(ETrustInvoiceState.IS_HOLD);
        invoice1.setNextCheckStatusAt(Instant.now().minus(1, ChronoUnit.MINUTES));
        List<UUID> pendingInvoiceIds = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            TrustInvoice invoice2 = createInvoiceWithWorkflowForBackgroundJob(ETrustInvoiceState.IS_WAIT_FOR_PAYMENT);
            invoice2.setNextCheckStatusAt(Instant.now().minus(2, ChronoUnit.MINUTES));
            pendingInvoiceIds.add(invoice2.getId());
        }
        Set<UUID> excludedInvoice = Set.of(pendingInvoiceIds.remove(0));
        List<UUID> invoiceIds = trustInvoiceRepository.getInvoiceIdsAwaitingRefreshInStateWithExclusions(
                Instant.now(), ETrustInvoiceState.IS_WAIT_FOR_PAYMENT, EWorkflowState.WS_RUNNING,
                excludedInvoice,
                PageRequest.of(0, 6)
        );
        assertThat(invoiceIds).isEqualTo(pendingInvoiceIds);
    }

    private TrustInvoice createInvoiceWithWorkflow(ETrustInvoiceState invoiceState) {
        return createInvoiceWithWorkflow(invoiceState, false);
    }

    private TrustInvoice createInvoiceWithWorkflowForBackgroundJob(ETrustInvoiceState invoiceState) {
        return createInvoiceWithWorkflow(invoiceState, true);
    }


    private TrustInvoice createInvoiceWithWorkflow(ETrustInvoiceState invoiceState, boolean backgroundJobActive) {
        TrustInvoice invoice = TrustInvoice.createEmptyInvoice();
        invoice.setState(invoiceState);
        invoice.setBackgroundJobActive(backgroundJobActive);
        Workflow workflow = Workflow.createWorkflowForEntity(invoice);
        workflowRepository.save(workflow);
        return trustInvoiceRepository.save(invoice);
    }
}
