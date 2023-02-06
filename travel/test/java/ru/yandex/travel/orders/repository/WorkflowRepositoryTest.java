package ru.yandex.travel.orders.repository;

import java.util.UUID;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.travel.orders.entities.HotelOrder;
import ru.yandex.travel.orders.entities.TrustInvoice;
import ru.yandex.travel.orders.workflows.order.OrderCreateHelper;
import ru.yandex.travel.workflow.entities.Workflow;
import ru.yandex.travel.workflow.repository.WorkflowEventRepository;
import ru.yandex.travel.workflow.repository.WorkflowRepository;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@DataJpaTest
@ActiveProfiles("test")
public class WorkflowRepositoryTest {
    @Autowired
    private WorkflowRepository workflowRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private WorkflowEventRepository workflowEventRepository;

    @Test
    public void testWorkflowEntityOrderAssociation() {
        HotelOrder order = OrderCreateHelper.createTestHotelOrder();
        order = orderRepository.saveAndFlush(order);

        Workflow workflow = Workflow.createWorkflowForEntity(order);

        workflowRepository.saveAndFlush(workflow);

        Workflow savedWorkflow = workflowRepository.getOne(workflow.getId());

        assertThat(savedWorkflow.getEntityType()).isEqualTo(order.getEntityType());
        assertThat(savedWorkflow.getEntityId()).isEqualTo(order.getId());
    }

    @Test
    public void testWorkflowEntityInvoiceAssociation() {
        TrustInvoice invoice = TrustInvoice.createEmptyInvoice();
        invoice = invoiceRepository.saveAndFlush(invoice);

        Workflow workflow = Workflow.createWorkflowForEntity(invoice);
        workflowRepository.saveAndFlush(workflow);

        Workflow savedWorkflow = workflowRepository.getOne(workflow.getId());

        assertThat(savedWorkflow.getEntityType()).isEqualTo(invoice.getEntityType());
        assertThat(savedWorkflow.getEntityId()).isEqualTo(invoice.getId());
    }

    private UUID createWorkflowWithOrder() {
        HotelOrder order = OrderCreateHelper.createTestHotelOrder();
        order = orderRepository.saveAndFlush(order);

        Workflow workflow = Workflow.createWorkflowForEntity(order);
        workflowRepository.saveAndFlush(workflow);
        return workflow.getId();
    }
}
