package ru.yandex.market.logistics.logistics4shops.logbroker.lom.processor;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.logistics4shops.AbstractIntegrationTest;
import ru.yandex.market.logistics.logistics4shops.event.model.LogisticEvent;
import ru.yandex.market.logistics.logistics4shops.event.model.OrderChangeRequestUpdatePayload;
import ru.yandex.market.logistics.logistics4shops.logbroker.LomEventMessageHandler;
import ru.yandex.market.logistics.logistics4shops.logging.code.LomEventCode;
import ru.yandex.market.logistics.logistics4shops.utils.LogisticEventUtil;
import ru.yandex.market.logistics.logistics4shops.utils.LomEventFactory;
import ru.yandex.market.logistics.logistics4shops.utils.ProtobufAssertionsUtils;
import ru.yandex.market.logistics.logistics4shops.utils.logging.TskvLogRecord;

import static ru.yandex.market.logistics.logistics4shops.utils.logging.BackLogAssertions.logEqualsTo;

@DisplayName("Переход заявки на удаление товаров в терминальный статус")
class OrderItemsRemovalFinishedProcessorTest extends AbstractIntegrationTest {
    private static final Predicate<TskvLogRecord<?>> PROCESSING_ERROR_LOG_MATCHER = logEqualsTo(
        TskvLogRecord.error("Error during extracting payload from changeOrderRequest 1 status PROCESSING")
            .setLoggingCode(LomEventCode.EVENT_PROCESSING_ERROR)
            .setEntities(Map.of("changeOrderRequest", List.of("1")))
    );

    @Autowired
    private LomEventMessageHandler lomEventMessageHandler;
    @Autowired
    private LogisticEventUtil logisticEventUtil;

    @Test
    @DisplayName("Успешная обработка события - событие успеха")
    @ExpectedDatabase(
        value = "/logbroker/lom/event/itemsremoval/finished/db/after/event_saved.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void successProcessingSuccessStatus() {
        lomEventMessageHandler.handle(List.of(
            LomEventFactory.eventDto(
                "logbroker/lom/event/itemsremoval/finished/diff/success_status.json",
                "logbroker/lom/event/itemsremoval/finished/snapshot/success_status.json"
            )
        ));
        LogisticEvent eventPayload = logisticEventUtil.getEventPayload(1L);
        ProtobufAssertionsUtils.prepareProtobufAssertion(
            softly.assertThat(eventPayload.getOrderChangeRequestUpdatePayload())
        )
            .isEqualTo(
                OrderChangeRequestUpdatePayload.newBuilder()
                    .setOrderId(2L)
                    .setShopId(101L)
                    .setChangeRequestId(102)
                    .setChangeRequestStatus(OrderChangeRequestUpdatePayload.ChangeRequestStatus.APPROVED)
                    .build()
            );
    }

    @Test
    @DisplayName("Успешная обработка события - событие неуспеха")
    @ExpectedDatabase(
        value = "/logbroker/lom/event/itemsremoval/finished/db/after/event_saved.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void successProcessingFailStatus() {
        lomEventMessageHandler.handle(List.of(
            LomEventFactory.eventDto(
                "logbroker/lom/event/itemsremoval/finished/diff/fail_status.json",
                "logbroker/lom/event/itemsremoval/finished/snapshot/fail_status.json"
            )
        ));
        LogisticEvent eventPayload = logisticEventUtil.getEventPayload(1L);
        ProtobufAssertionsUtils.prepareProtobufAssertion(
            softly.assertThat(eventPayload.getOrderChangeRequestUpdatePayload())
        )
            .isEqualTo(
                OrderChangeRequestUpdatePayload.newBuilder()
                    .setOrderId(2L)
                    .setShopId(101L)
                    .setChangeRequestId(102)
                    .setChangeRequestStatus(OrderChangeRequestUpdatePayload.ChangeRequestStatus.REJECTED)
                    .build()
            );
    }

    @Test
    @DisplayName("Заказ не FaaS")
    @ExpectedDatabase(
        value = "/logbroker/lom/event/common/db/after/event_not_saved.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void orderIsNotFaaS() {
        lomEventMessageHandler.handle(List.of(
            LomEventFactory.eventDto(
                "logbroker/lom/event/itemsremoval/inforeceived/diff/fail_status.json",
                "logbroker/lom/event/itemsremoval/inforeceived/snapshot/not_faas.json"
            )
        ));
    }

    @Test
    @DisplayName("Неверный статусный переход заявки")
    @ExpectedDatabase(
        value = "/logbroker/lom/event/common/db/after/event_not_saved.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void failStatus() {
        lomEventMessageHandler.handle(List.of(
            LomEventFactory.eventDto(
                "logbroker/lom/event/itemsremoval/finished/diff/from_final_status_to_final_status.json",
                "logbroker/lom/event/itemsremoval/finished/snapshot/fail_status.json"
            )
        ));
    }

    @Test
    @DisplayName("Нет payload с нужным статусом")
    @ExpectedDatabase(
        value = "/logbroker/lom/event/common/db/after/event_not_saved.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void noRequiredPayload() {
        lomEventMessageHandler.handle(List.of(
            LomEventFactory.eventDto(
                "logbroker/lom/event/itemsremoval/finished/diff/success_status.json",
                "logbroker/lom/event/itemsremoval/finished/snapshot/no_processing_payload.json"
            )
        ));
        assertLogs().anyMatch(PROCESSING_ERROR_LOG_MATCHER);
    }

    @Test
    @DisplayName("Payload нельзя распарсить")
    @ExpectedDatabase(
        value = "/logbroker/lom/event/common/db/after/event_not_saved.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void noParsablePayload() {
        lomEventMessageHandler.handle(List.of(
            LomEventFactory.eventDto(
                "logbroker/lom/event/itemsremoval/finished/diff/success_status.json",
                "logbroker/lom/event/itemsremoval/finished/snapshot/non_parsable_payload.json"
            )
        ));
        assertLogs().anyMatch(PROCESSING_ERROR_LOG_MATCHER);
    }
}
