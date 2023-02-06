package ru.yandex.travel.orders.repository;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.assertj.core.util.Sets;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.travel.orders.entities.SuburbanOrderItem;
import ru.yandex.travel.orders.workflow.orderitem.generic.proto.EOrderItemState;
import ru.yandex.travel.workflow.EWorkflowState;
import ru.yandex.travel.workflow.entities.Workflow;
import ru.yandex.travel.workflow.entities.WorkflowEntity;
import ru.yandex.travel.workflow.repository.WorkflowRepository;

import static org.assertj.core.api.Assertions.assertThat;


@RunWith(SpringRunner.class)
@DataJpaTest
@ActiveProfiles("test")
public class SuburbanOrderItemRepositoryTest {
    @Autowired
    private SuburbanOrderItemRepository orderItemRepository;

    @Autowired
    private WorkflowRepository workflowRepository;

    @Test
    public void testGetExpiredOrderItems() {
        SuburbanOrderItem orderItem1 = createOrderItem(EOrderItemState.IS_RESERVED, -120);
        createOrderItem(EOrderItemState.IS_RESERVED, 120);
        SuburbanOrderItem orderItem3 = createOrderItem(EOrderItemState.IS_RESERVED, -30);
        createOrderItem(EOrderItemState.IS_CONFIRMED, -30);

        checkExpired(Set.of(), 10, Set.of(orderItem1, orderItem3), 2);
        checkExpired(Set.of(), 1, Set.of(orderItem1), 2);
        checkExpired(Set.of(orderItem1.getId()), 1, Set.of(orderItem3), 1);

        Workflow workflow = orderItem1.getWorkflow();
        workflow.setState(EWorkflowState.WS_CRASHED);
        workflowRepository.saveAndFlush(workflow);
        checkExpired(Set.of(), 10, Set.of(orderItem3), 1);
    }

    private void checkExpired(Collection<UUID> exclude, int pageSize, Collection<SuburbanOrderItem> expected,
                              int expectedTotal) {
        List<UUID> expiredItems = orderItemRepository.getExpiredOrderItems(
                Instant.now(),
                Arrays.asList(EOrderItemState.IS_RESERVED),
                EWorkflowState.WS_RUNNING,
                exclude.isEmpty() ? SuburbanOrderItemRepository.EMPTY_EXCLUDE_UIDS : exclude,
                PageRequest.of(0, pageSize)
        );
        long totalExpiredCount = orderItemRepository.countExpiredOrderItems(
                Instant.now(),
                Arrays.asList(EOrderItemState.IS_RESERVED),
                EWorkflowState.WS_RUNNING,
                exclude.isEmpty() ? SuburbanOrderItemRepository.EMPTY_EXCLUDE_UIDS : exclude
        );

        var expectedUids = expected.stream().map(SuburbanOrderItem::getId).collect(Collectors.toSet());
        assertThat(Sets.newHashSet(expiredItems)).isEqualTo(expectedUids);
        assertThat(totalExpiredCount).isEqualTo(expectedTotal);
    }

    private Workflow createWorkflowForEntity(WorkflowEntity<?> workflowEntity) {
        Workflow workflow = Workflow.createWorkflowForEntity(workflowEntity);
        return workflowRepository.saveAndFlush(workflow);
    }

    private SuburbanOrderItem createOrderItem(EOrderItemState state, Integer expiresFromNow) {
        var orderItem = new SuburbanOrderItem();
        orderItem.setId(UUID.randomUUID());
        orderItem.setState(state);
        orderItem.setWorkflow(createWorkflowForEntity(orderItem));
        orderItem.setExpiresAt(Instant.now().plusSeconds(expiresFromNow));
        orderItemRepository.saveAndFlush(orderItem);

        return orderItem;
    }
}
