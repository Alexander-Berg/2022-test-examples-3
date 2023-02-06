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

import ru.yandex.travel.orders.entities.AeroflotInvoice;
import ru.yandex.travel.orders.workflow.invoice.aeroflot.proto.EAeroflotInvoiceState;
import ru.yandex.travel.workflow.EWorkflowState;
import ru.yandex.travel.workflow.entities.Workflow;
import ru.yandex.travel.workflow.repository.WorkflowRepository;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@DataJpaTest
@ActiveProfiles("test")
public class AeroflotInvoiceRepositoryTest {
    @Autowired
    AeroflotInvoiceRepository aeroflotInvoiceRepository;

    @Autowired
    WorkflowRepository workflowRepository;

    @Autowired
    EntityManager em;

    @Test
    public void testGetInvoicesWaitingTokenizationWithExclusion() {
        testGetInvoicesWaitingTrustTokenization(true);
    }

    @Test
    public void testGetInvoicesWaitingTokenizationWithoutExclusion() {
        testGetInvoicesWaitingTrustTokenization(false);
    }

    public void testGetInvoicesWaitingTrustTokenization(boolean withExclusion) {
        AeroflotInvoice invoice1 =
                createInvoiceWithWorkflowForBackgroundJob(EAeroflotInvoiceState.IS_WAIT_ORDER_CREATED);
        invoice1.setNextCheckStatusAt(Instant.now().minus(1, ChronoUnit.MINUTES));
        List<UUID> pendingInvoiceIds = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            AeroflotInvoice invoice2 =
                    createInvoiceWithWorkflowForBackgroundJob(EAeroflotInvoiceState.IS_WAIT_TRUST_TOKENIZATION);
            invoice2.setNextCheckStatusAt(Instant.now().minus(2, ChronoUnit.MINUTES));
            pendingInvoiceIds.add(invoice2.getId());
        }
        Set<UUID> excludedInvoice = Set.of();
        if (withExclusion) {
            excludedInvoice = Set.of(pendingInvoiceIds.remove(0));

        }
        List<UUID> invoiceIds = aeroflotInvoiceRepository.getInvoiceIdsAwaitingRefreshInStateWithExclusions(
                Instant.now(), EAeroflotInvoiceState.IS_WAIT_TRUST_TOKENIZATION, EWorkflowState.WS_RUNNING,
                excludedInvoice,
                PageRequest.of(0, 6)
        );
        assertThat(invoiceIds).isEqualTo(pendingInvoiceIds);
    }

    private AeroflotInvoice createInvoiceWithWorkflowForBackgroundJob(EAeroflotInvoiceState invoiceState) {
        return createInvoiceWithWorkflow(invoiceState, true);
    }


    private AeroflotInvoice createInvoiceWithWorkflow(EAeroflotInvoiceState invoiceState, boolean backgroundJobActive) {
        AeroflotInvoice invoice = new AeroflotInvoice();
        invoice.setId(UUID.randomUUID());
        invoice.setState(invoiceState);
        invoice.setBackgroundJobActive(backgroundJobActive);
        Workflow workflow = Workflow.createWorkflowForEntity(invoice);
        workflowRepository.save(workflow);
        return aeroflotInvoiceRepository.save(invoice);
    }
}
