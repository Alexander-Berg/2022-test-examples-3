package ru.yandex.market.notifier.jobs.zk.processors;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

import com.google.common.collect.Iterables;
import com.google.protobuf.Timestamp;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.xmlunit.builder.DiffBuilder;

import ru.yandex.common.util.collections.Pair;
import ru.yandex.market.mbi.orderservice.proto.event.model.AcceptMethod;
import ru.yandex.market.mbi.orderservice.proto.event.model.MerchantOrder;
import ru.yandex.market.mbi.orderservice.proto.event.model.OrderCancelledPayload;
import ru.yandex.market.mbi.orderservice.proto.event.model.OrderEvent;
import ru.yandex.market.mbi.orderservice.proto.event.model.OrderKey;
import ru.yandex.market.mbi.orderservice.proto.event.model.OrderStateChangedPayload;
import ru.yandex.market.mbi.orderservice.proto.event.model.Status;
import ru.yandex.market.mbi.orderservice.proto.event.model.Substatus;
import ru.yandex.market.notifier.application.AbstractWebTestBase;
import ru.yandex.market.notifier.entity.ChannelType;
import ru.yandex.market.notifier.entity.Notification;
import ru.yandex.market.notifier.service.InboxService;
import ru.yandex.market.request.trace.RequestContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static ru.yandex.market.notifier.jobs.zk.processors.ExternalOrderNotificationProcessor.NOTIFY_EXTERNAL_ORDER_CREATION_FAILED;
import static ru.yandex.market.notifier.jobs.zk.processors.ExternalOrderNotificationProcessor.NOTIFY_EXTERNAL_ORDER_DELIVERED;

public class ExternalOrderEventProcessorTest extends AbstractWebTestBase {

    private static final Timestamp TEST_EVENT_CREATED_AT = Timestamp.newBuilder().setSeconds(1651363200).build();
    private static final Timestamp TEST_EVENT_DELIVERED_AT = Timestamp.newBuilder().setSeconds(1651463600).build();

    private ExternalOrderEventProcessor processorUnderTest;
    @Value("${market.notifier.checkout.keepSentDays}")
    Integer daysToKeepSentNotifications;
    @Autowired
    private InboxService inboxService;

    @BeforeEach
    void beforeAll() {
        processorUnderTest = new ExternalOrderNotificationProcessor(
                inboxService,
                daysToKeepSentNotifications
        );
    }

    @Test
    @DisplayName("verify that non-notification payloads are skipped")
    void testMessageSKipped() {
        var event = skippedEvent();
        assertThat(runImport(event)).isEqualTo(ProcessEventResult.skipped());
    }

    @Test
    @DisplayName("verify that invalid payloads are skipped")
    void testInvalidMessageSkipped() {
        var event = invalidEvent();
        assertThat(runImport(event)).isEqualTo(ProcessEventResult.skipped());
    }

    @Test
    void testOrderCreationFailed() throws IOException {
        var event = makeEvent(
                Pair.of(Status.PROCESSING, Substatus.STARTED),
                Pair.of(Status.CANCELLED_IN_PROCESSING, Substatus.SERVICE_FAULT));
        checkNotification(
                event,
                NOTIFY_EXTERNAL_ORDER_CREATION_FAILED,
                "/files/external-order-creation-failed.xml"
        );
    }

    @Test
    void testOrderDelivered() throws IOException {
        var event = makeEvent(
                Pair.of(Status.DELIVERY, Substatus.DELIVERY_SERVICE_RECEIVED),
                Pair.of(Status.DELIVERED, Substatus.DELIVERY_SERVICE_DELIVERED));
        checkNotification(
                event,
                NOTIFY_EXTERNAL_ORDER_DELIVERED,
                "/files/external-order-delivered.xml"
        );
    }

    private void checkNotification(OrderEvent event, String type, String expectedFileName) throws IOException {
        assertThat(runImport(event)).isEqualTo(ProcessEventResult.ok());
        assertThat(inboxService.getDeliveryStatisticsFull()).hasSize(1);
        var actualInbox = inboxService.findNewForOrderWithTypes(777L, List.of(type));
        assertThat(actualInbox)
                .hasSize(1)
                .element(0)
                .extracting(
                        Notification::getType,
                        (notification -> notification.getDeliveryChannels().get(0).getType()),
                        (notification -> notification.getDeliveryChannels().get(0).getAddress()),
                        Notification::getKeepSentDays
                )
                .containsExactly(
                        type,
                        ChannelType.MBI_API,
                        "1337",
                        21
                );
        var expected = IOUtils.toString(
                Objects.requireNonNull(getClass().getResourceAsStream(expectedFileName)),
                StandardCharsets.UTF_8
        );
        var diff = DiffBuilder.compare(expected)
                .withTest(Iterables.getFirst(actualInbox, null).getData())
                .ignoreWhitespace()
                .build();

        if (diff.hasDifferences()) {
            fail(diff.getDifferences().toString());
        }
    }

    private ProcessEventResult runImport(OrderEvent event) {
        RequestContextHolder.createNewContext();
        return processorUnderTest.process(event);
    }

    private OrderEvent makeEvent(Pair<Status, Substatus> before, Pair<Status, Substatus> after) {
        return OrderEvent.newBuilder()
                .setCreatedAt(TEST_EVENT_CREATED_AT)
                .setId(123)
                .setTraceId("a/b/1")
                .setOrderStateChangedPayload(
                        OrderStateChangedPayload.newBuilder()
                                .setOrderKey(OrderKey.newBuilder()
                                        .setShopId(1337L)
                                        .setOrderId(777)
                                        .build()
                                )
                                .setBefore(buildOrder(before.first, before.second))
                                .setAfter(buildOrder(after.first, after.second))
                                .build()
                ).build();
    }

    private MerchantOrder buildOrder(Status newStatus, Substatus newSubstatus) {
        var builder = MerchantOrder.newBuilder()
                .setAcceptMethod(AcceptMethod.WEB_INTERFACE)
                .setStatus(newStatus)
                .setSubstatus(newSubstatus)
                .setMerchantOrderId("abc1337");
        if (newStatus.equals(Status.DELIVERED)) {
            builder.setDeliveredAt(TEST_EVENT_DELIVERED_AT);
        }
        return builder.build();
    }

    private OrderEvent skippedEvent() {
        return OrderEvent.newBuilder()
                .setCreatedAt(TEST_EVENT_CREATED_AT)
                .setId(123)
                .setTraceId("a/b/1")
                .setOrderCancelledPayload(
                        OrderCancelledPayload.newBuilder()
                                .setOrderKey(OrderKey.newBuilder()
                                        .setShopId(1337L)
                                        .setOrderId(777)
                                        .build()
                                )
                                .build()
                ).build();
    }

    private OrderEvent invalidEvent() {
        return OrderEvent.newBuilder()
                .setCreatedAt(TEST_EVENT_CREATED_AT)
                .setId(123)
                .setTraceId("a/b/1")
                .setOrderStateChangedPayload(
                        OrderStateChangedPayload.newBuilder()
                                .setOrderKey(OrderKey.newBuilder()
                                        .setShopId(1337L)
                                        .setOrderId(777)
                                        .build()
                                )
                                .setBefore(
                                        MerchantOrder.newBuilder()
                                                .setAcceptMethod(AcceptMethod.WEB_INTERFACE)
                                                .setStatus(Status.PROCESSING)
                                                .setSubstatus(Substatus.STARTED)
                                                .build()
                                )
                                .build()
                ).build();
    }
}
