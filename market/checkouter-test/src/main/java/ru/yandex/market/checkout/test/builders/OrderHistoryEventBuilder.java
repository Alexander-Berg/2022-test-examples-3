package ru.yandex.market.checkout.test.builders;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Objects;
import java.util.function.Consumer;

import javax.annotation.Nonnull;

import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.event.RefundHistoryEvent;
import ru.yandex.market.checkout.checkouter.order.Order;

/**
 * @author Nicolai Iusiumbeli <armor@yandex-team.ru>
 * date: 27/01/2017
 */
public final class OrderHistoryEventBuilder extends AbstractTestDataBuilder<OrderHistoryEvent> {

    private Long id;
    private HistoryEventType eventType;
    private ClientInfo clientInfo;
    private Date fromDate;
    private Date toDate;
    private Date tranDate;
    private String host;
    private Order order;
    private Order orderBefore;
    private Order orderAfter;
    private Consumer<Order> orderBeforeModifier;
    private Consumer<Order> orderAfterModifier;
    private Long refundId;
    private BigDecimal refundPlanned;
    private BigDecimal refundActual;
    private BigDecimal subsidyRefundPlanned;
    private BigDecimal subsidyRefundActual;
    private RefundHistoryEvent refundEvent;
    private RefundHistoryEvent subsidyRefundEvent;
    private Long returnId;
    private ClientInfo cancellationRequestAuthor;

    private OrderHistoryEventBuilder() {
    }

    public static OrderHistoryEventBuilder anOrderHistoryEvent() {
        return new OrderHistoryEventBuilder();
    }

    public OrderHistoryEventBuilder withId(Long id) {
        this.id = id;
        return this;
    }

    public OrderHistoryEventBuilder withEventType(HistoryEventType eventType) {
        this.eventType = eventType;
        return this;
    }

    public OrderHistoryEventBuilder withClientInfo(ClientInfo clientInfo) {
        this.clientInfo = clientInfo;
        return this;
    }

    public OrderHistoryEventBuilder withFromDate(Date fromDate) {
        this.fromDate = fromDate;
        return this;
    }

    public OrderHistoryEventBuilder withToDate(Date toDate) {
        this.toDate = toDate;
        return this;
    }

    public OrderHistoryEventBuilder withTranDate(Date tranDate) {
        this.tranDate = tranDate;
        return this;
    }

    public OrderHistoryEventBuilder withHost(String host) {
        this.host = host;
        return this;
    }

    public OrderHistoryEventBuilder withOrderBefore(Order orderBefore) {
        this.orderBefore = orderBefore;
        return this;
    }

    public OrderHistoryEventBuilder withOrderAfter(Order orderAfter) {
        this.orderAfter = orderAfter;
        return this;
    }

    public OrderHistoryEventBuilder withOrder(Order order) {
        this.order = order;
        return this;
    }

    public OrderHistoryEventBuilder withOrderBeforeModifier(Consumer<Order> orderBeforeModifier) {
        this.orderBeforeModifier = orderBeforeModifier;
        return this;
    }

    public OrderHistoryEventBuilder withOrderAfterModifier(Consumer<Order> orderAfterModifier) {
        this.orderAfterModifier = orderAfterModifier;
        return this;
    }

    public OrderHistoryEventBuilder withRefundId(Long refundId) {
        this.refundId = refundId;
        return this;
    }

    public OrderHistoryEventBuilder withRefundPlanned(BigDecimal refundPlanned) {
        this.refundPlanned = refundPlanned;
        return this;
    }

    public OrderHistoryEventBuilder withRefundActual(BigDecimal refundActual) {
        this.refundActual = refundActual;
        return this;
    }

    public OrderHistoryEventBuilder withSubsidyRefundPlanned(BigDecimal subsidyRefundPlanned) {
        this.subsidyRefundPlanned = subsidyRefundPlanned;
        return this;
    }

    public OrderHistoryEventBuilder withSubsidyRefundActual(BigDecimal subsidyRefundActual) {
        this.subsidyRefundActual = subsidyRefundActual;
        return this;
    }

    public OrderHistoryEventBuilder withRefundEvent(RefundHistoryEvent refundEvent) {
        this.refundEvent = refundEvent;
        return this;
    }

    public OrderHistoryEventBuilder withSubsidyRefundEvent(RefundHistoryEvent subsidyRefundEvent) {
        this.subsidyRefundEvent = subsidyRefundEvent;
        return this;
    }

    public OrderHistoryEventBuilder withReturnId(Long returnId) {
        this.returnId = returnId;
        return this;
    }

    public OrderHistoryEventBuilder withCancellationRequestAuthor(@Nonnull ClientInfo cancellationRequestAuthor) {
        this.cancellationRequestAuthor = Objects.requireNonNull(cancellationRequestAuthor);
        return this;
    }

    public OrderHistoryEventBuilder but() {
        return anOrderHistoryEvent().withId(id).withEventType(eventType).withClientInfo(clientInfo)
                .withFromDate(fromDate)
                .withToDate(toDate).withTranDate(tranDate).withHost(host).withOrderBefore(orderBefore)
                .withOrderAfter(orderAfter).withRefundId(refundId).withRefundPlanned(refundPlanned)
                .withRefundActual(refundActual).withSubsidyRefundPlanned(subsidyRefundPlanned)
                .withSubsidyRefundActual(subsidyRefundActual).withRefundEvent(refundEvent)
                .withSubsidyRefundEvent(subsidyRefundEvent);
    }

    @Override
    public OrderHistoryEvent build() {
        OrderHistoryEvent orderHistoryEvent = new OrderHistoryEvent();
        orderHistoryEvent.setId(randomIfNull(id));
        orderHistoryEvent.setType(eventType);
        orderHistoryEvent.setAuthor(clientInfo);
        orderHistoryEvent.setFromDate(randomIfNull(fromDate));
        orderHistoryEvent.setToDate(randomIfNull(toDate));
        orderHistoryEvent.setTranDate(randomIfNull(tranDate));
        orderHistoryEvent.setHost(randomIfNull(host));
        orderHistoryEvent.setRefundId(refundId);
        orderHistoryEvent.setRefundPlanned(refundPlanned);
        orderHistoryEvent.setRefundActual(refundActual);
        orderHistoryEvent.setSubsidyRefundPlanned(subsidyRefundPlanned);
        orderHistoryEvent.setSubsidyRefundActual(subsidyRefundActual);
        orderHistoryEvent.setRefundEvent(refundEvent);
        orderHistoryEvent.setSubsidyRefundEvent(subsidyRefundEvent);
        orderHistoryEvent.setReturnId(returnId);
        orderHistoryEvent.setCancellationRequestAuthor(cancellationRequestAuthor);

        Order orderLocal;
        if (orderBefore != null) {
            orderHistoryEvent.setOrderBefore(orderBefore);
        } else if (order != null && orderBeforeModifier != null) {
            orderLocal = order.clone();
            orderBeforeModifier.accept(orderLocal);
            orderHistoryEvent.setOrderBefore(orderLocal);
        }

        if (orderAfter != null) {
            orderHistoryEvent.setOrderAfter(orderAfter);
        } else if (order != null && orderAfterModifier != null) {
            orderLocal = order.clone();
            orderAfterModifier.accept(orderLocal);
            orderHistoryEvent.setOrderAfter(orderLocal);
        } else if (order != null) {
            orderHistoryEvent.setOrderAfter(order);
        }
        return orderHistoryEvent;
    }
}
