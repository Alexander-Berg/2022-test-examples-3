package ru.yandex.market.pvz.core.domain.order.model.sibling;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.pvz.core.domain.order.OrderQueryService;
import ru.yandex.market.pvz.core.domain.order.model.params.SiblingOrderParams;
import ru.yandex.market.pvz.core.test.TransactionlessEmbeddedDbTest;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.apache.commons.lang3.RandomUtils.nextLong;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@TransactionlessEmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class SiblingOrderCommandServiceTest {

    public static final long PICKUP_POINT_ID = 123L;
    public static final String RECIPIENT_PHONE = randomAlphanumeric(10);
    public static final long BUYER_YANDEX_UID = nextLong();

    public static final long ANOTHER_PICKUP_POINT_ID = 345L;
    public static final String ANOTHER_RECIPIENT_PHONE = randomAlphanumeric(10);
    public static final long ANOTHER_BUYER_YANDEX_UID = nextLong();

    private final SiblingOrderCommandService siblingOrderCommandService;

    private final TestableClock clock;

    @MockBean
    private OrderQueryService orderQueryService;

    @MockBean
    private SiblingGroupRepository siblingGroupRepository;

    @ParameterizedTest(name = "{0} simultaneously arrived orders are grouped")
    @ValueSource(ints = {2, 3, 4, 5})
    void testCreateGroupOnOrderArrival(int arrivedOrdersNumber) {
        List<SiblingOrderParams> arrivedOrders = Stream.generate(() -> createOrder(null))
                .limit(arrivedOrdersNumber)
                .collect(Collectors.toList());

        whenFindByRecipientReturnEmpty();
        when(orderQueryService.findSiblingsByOrderIdIncludingSelf(anyLong()))
                .thenReturn(arrivedOrders);

        siblingOrderCommandService.receive(arrivedOrders.get(0).getId());

        var captor = ArgumentCaptor.forClass(SiblingGroup.class);
        verify(siblingGroupRepository).save(captor.capture());
        assertThat(captor.getValue().getSiblingOrders()).hasSize(arrivedOrdersNumber);
    }

    @Test
    void testUpdateGroupOnOneMoreSiblingArrival() {
        SiblingOrderParams firstOrder = createOrder(null);
        SiblingOrderParams secondOrder = createOrder(null);
        SiblingOrderParams brandNewOrder = createOrder(null);

        whenFindByRecipientReturn(List.of(firstOrder, secondOrder), null);
        when(orderQueryService.findSiblingsByOrderIdIncludingSelf(anyLong()))
                .thenReturn(List.of(firstOrder, secondOrder, brandNewOrder));

        siblingOrderCommandService.receive(brandNewOrder.getId());

        var captor = ArgumentCaptor.forClass(SiblingGroup.class);
        verify(siblingGroupRepository).save(captor.capture());
        assertThat(captor.getValue().getSiblingOrders()).hasSize(3);
    }

    @Test
    void testDontCreateGroupForSingleOrder() {
        SiblingOrderParams singleOrder = createOrder(null);

        when(orderQueryService.findSiblingsByOrderIdIncludingSelf(anyLong()))
                .thenReturn(List.of(singleOrder));

        siblingOrderCommandService.receive(singleOrder.getId());

        verify(siblingGroupRepository, never()).save(any());
    }

    @Test
    void testCreateOneOrderGroupIfDeliveredOrderExists() {
        OffsetDateTime deliveredAt = OffsetDateTime.now(clock);
        SiblingOrderParams deliveredOrder = createOrder(deliveredAt);
        SiblingOrderParams newOrder = createOrder(null);

        when(orderQueryService.findSiblingsByOrderIdIncludingSelf(anyLong()))
                .thenReturn(List.of(deliveredOrder, newOrder));
        when(siblingGroupRepository.findSiblingGroupByRecipient(anyString(), anyLong(), anyLong()))
                .thenReturn(Optional.empty());

        siblingOrderCommandService.receive(newOrder.getId());

        var captor = ArgumentCaptor.forClass(SiblingGroup.class);
        verify(siblingGroupRepository).save(captor.capture());
        assertThat(captor.getValue().getSiblingOrders()).hasSize(1);
        assertThat(captor.getValue().getDeliveredAt()).isEqualTo(deliveredAt);
    }

    @Test
    void testDeliverOneOfSiblingsShrinksGroupSetsDeliveredAt() {
        OffsetDateTime deliveredAt = OffsetDateTime.now(clock);
        SiblingOrderParams almostDeliveredOrder = createOrder(deliveredAt);
        SiblingOrderParams singleOrder = createOrder(null);

        whenFindByRecipientReturn(List.of(almostDeliveredOrder, singleOrder), null);
        when(orderQueryService.findSiblingsByOrderIdIncludingSelf(anyLong()))
                .thenReturn(List.of(singleOrder, almostDeliveredOrder));

        siblingOrderCommandService.deliver(almostDeliveredOrder, deliveredAt);

        var captor = ArgumentCaptor.forClass(SiblingGroup.class);
        verify(siblingGroupRepository).save(captor.capture());
        assertThat(captor.getValue().getSiblingOrders()).hasSize(1);
        assertThat(captor.getValue().getDeliveredAt()).isEqualTo(deliveredAt);
    }

    @Test
    void testDeliverLastSiblingOrderDeletesGroup() {
        OffsetDateTime deliveredAt = OffsetDateTime.now(clock);
        SiblingOrderParams deliveredOrder = createOrder(deliveredAt);
        SiblingOrderParams almostDeliveredOrder = createOrder(deliveredAt);

        whenFindByRecipientReturn(List.of(almostDeliveredOrder), deliveredAt);
        when(orderQueryService.findSiblingsByOrderIdIncludingSelf(anyLong()))
                .thenReturn(List.of(almostDeliveredOrder, deliveredOrder));

        siblingOrderCommandService.deliver(almostDeliveredOrder, deliveredAt);

        verify(siblingGroupRepository, never()).save(any());
        verify(siblingGroupRepository).delete(any());
    }

    @Test
    void testDeliverSingleOrderDoesNothing() {
        OffsetDateTime deliveredAt = OffsetDateTime.now(clock);
        SiblingOrderParams singleOrder = createOrder(deliveredAt);

        whenFindByRecipientReturnEmpty();

        siblingOrderCommandService.deliver(singleOrder, deliveredAt);

        verify(siblingGroupRepository, never()).save(any());
        verify(siblingGroupRepository, never()).delete(any());
    }

    @Test
    void testCancelOrderFromLargeGroupShrinksGroup() {
        SiblingOrderParams firstOrder = createOrder(null);
        SiblingOrderParams secondOrder = createOrder(null);
        SiblingOrderParams thirdOrder = createOrder(null);

        whenFindByRecipientReturn(List.of(firstOrder, secondOrder, thirdOrder), null);

        siblingOrderCommandService.cancel(thirdOrder);

        verify(siblingGroupRepository, never()).delete(any());
        var captor = ArgumentCaptor.forClass(SiblingGroup.class);
        verify(siblingGroupRepository).save(captor.capture());
        assertThat(captor.getValue().getSiblingOrders()).hasSize(2);
    }

    @Test
    void testCancelOrderFromPairDisbandsGroup() {
        SiblingOrderParams firstOrder = createOrder(null);
        SiblingOrderParams secondOrder = createOrder(null);

        whenFindByRecipientReturn(List.of(firstOrder, secondOrder), null);

        siblingOrderCommandService.cancel(secondOrder);

        verify(siblingGroupRepository).delete(any());
        verify(siblingGroupRepository, never()).save(any());
    }

    @Test
    void testCancelOrderFromPairButWithDeliveredAtShrinksGroup() {
        OffsetDateTime deliveredAt = OffsetDateTime.now(clock);
        SiblingOrderParams firstOrder = createOrder(null);
        SiblingOrderParams almostCancelledOrder = createOrder(null);

        whenFindByRecipientReturn(List.of(firstOrder, almostCancelledOrder), deliveredAt);

        siblingOrderCommandService.cancel(almostCancelledOrder);

        verify(siblingGroupRepository, never()).delete(any());
        var captor = ArgumentCaptor.forClass(SiblingGroup.class);
        verify(siblingGroupRepository).save(captor.capture());
        assertThat(captor.getValue().getSiblingOrders()).hasSize(1);
        assertThat(captor.getValue().getDeliveredAt()).isEqualTo(deliveredAt);
    }

    @Test
    void testCancelSingleOrderDoesNothing() {
        SiblingOrderParams singleOrder = createOrder(null);

        whenFindByRecipientReturnEmpty();

        siblingOrderCommandService.cancel(singleOrder);

        verify(siblingGroupRepository, never()).delete(any());
        verify(siblingGroupRepository, never()).save(any());
    }

    @Test
    void testCancelDeliveryDeletesDeliveredAt() {
        SiblingOrderParams cancelledDeliveryOrder = createOrder(null);
        SiblingOrderParams secondOrder = createOrder(null);
        OffsetDateTime oldDeliveredAt = OffsetDateTime.now(clock);

        whenFindByRecipientReturn(List.of(secondOrder), oldDeliveredAt);
        when(orderQueryService.findSiblingsByOrderIdIncludingSelf(anyLong()))
                .thenReturn(List.of(cancelledDeliveryOrder, secondOrder));

        siblingOrderCommandService.cancelDelivery(cancelledDeliveryOrder);

        verify(siblingGroupRepository, never()).delete(any());
        var captor = ArgumentCaptor.forClass(SiblingGroup.class);
        verify(siblingGroupRepository).save(captor.capture());
        assertThat(captor.getValue().getSiblingOrders()).hasSize(2);
        assertThat(captor.getValue().getDeliveredAt()).isNull();
    }

    @Test
    void testCancelDeliveryRevertsDeliveredAt() {
        OffsetDateTime expectedDeliveredAt = OffsetDateTime.now(clock).minus(10, ChronoUnit.MINUTES);
        OffsetDateTime cancelledDeliveredAt = OffsetDateTime.now(clock);

        SiblingOrderParams deliveredOrder = createOrder(expectedDeliveredAt);
        SiblingOrderParams thirdOrder = createOrder(null);
        SiblingOrderParams cancelledDeliveryOrder = createOrder(null);


        whenFindByRecipientReturn(List.of(thirdOrder), cancelledDeliveredAt);
        when(orderQueryService.findSiblingsByOrderIdIncludingSelf(anyLong()))
                .thenReturn(List.of(deliveredOrder, cancelledDeliveryOrder, thirdOrder));

        siblingOrderCommandService.cancelDelivery(cancelledDeliveryOrder);

        verify(siblingGroupRepository, never()).delete(any());
        var captor = ArgumentCaptor.forClass(SiblingGroup.class);
        verify(siblingGroupRepository).save(captor.capture());
        assertThat(captor.getValue().getSiblingOrders()).hasSize(2);
        assertThat(captor.getValue().getDeliveredAt()).isEqualTo(expectedDeliveredAt);
    }

    private SiblingOrderParams createOrder(OffsetDateTime deliveredAt) {
        // deliveredAt != null => order.status == TRANSMITTED_TO_RECIPIENT
        return new SiblingOrderParams(nextLong(), randomAlphanumeric(6), PICKUP_POINT_ID, RECIPIENT_PHONE,
                BUYER_YANDEX_UID, deliveredAt, null);
    }

    private void whenFindByRecipientReturn(List<SiblingOrderParams> orders, OffsetDateTime deliveredAt) {
        SiblingGroup siblingGroup = createGroup(orders, deliveredAt);
        when(siblingGroupRepository.findSiblingGroupByRecipient(RECIPIENT_PHONE, BUYER_YANDEX_UID, PICKUP_POINT_ID))
                .thenReturn(Optional.ofNullable(siblingGroup));
    }

    private void whenFindByRecipientReturnEmpty() {
        whenFindByRecipientReturn(List.of(), null);
    }

    @Nullable
    private SiblingGroup createGroup(List<SiblingOrderParams> siblingOrderParams, OffsetDateTime deliveredAt) {
        if (siblingOrderParams.isEmpty()) {
            return null;
        }
        SiblingGroup siblingGroup =
                new SiblingGroup(RECIPIENT_PHONE, BUYER_YANDEX_UID, PICKUP_POINT_ID, new ArrayList<>(), deliveredAt);
        siblingOrderParams.stream()
                .map(param -> new SiblingGroupOrder(siblingGroup, param.getId(), param.getExternalId()))
                .forEach(siblingGroup.getSiblingOrders()::add);
        return siblingGroup;
    }
}
