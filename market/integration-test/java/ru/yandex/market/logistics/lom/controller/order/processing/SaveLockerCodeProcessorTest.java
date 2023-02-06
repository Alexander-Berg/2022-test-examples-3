package ru.yandex.market.logistics.lom.controller.order.processing;

import java.time.Instant;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.util.DateTimeUtils;
import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.checker.QueueTaskChecker;
import ru.yandex.market.logistics.lom.jobs.model.LockerCodePayload;
import ru.yandex.market.logistics.lom.jobs.model.ProcessingResult;
import ru.yandex.market.logistics.lom.jobs.model.ProcessingResultStatus;
import ru.yandex.market.logistics.lom.jobs.model.QueueType;
import ru.yandex.market.logistics.lom.jobs.processor.SaveLockerCodeProcessor;
import ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;

@ParametersAreNonnullByDefault
@DatabaseSetup("/service/les/before/order.xml")
@DisplayName("Обработчик задач очереди SAVE_LOCKER_CODE")
class SaveLockerCodeProcessorTest extends AbstractContextualTest {
    private static final Instant FIXED_TIME = Instant.parse("2019-06-12T00:00:00Z");

    @Autowired
    QueueTaskChecker queueTaskChecker;

    @Autowired
    SaveLockerCodeProcessor saveLockerCodeProcessor;

    @BeforeEach
    void setup() {
        clock.setFixed(FIXED_TIME, DateTimeUtils.MOSCOW_ZONE);
    }

    @Test
    @DisplayName("Сохранение кода постамата")
    @ExpectedDatabase(
        value = "/service/les/after/order_code_changed.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void saveLockerCode() {
        ProcessingResult result = saveLockerCodeProcessor.processPayload(getPayload("code"));
        softly.assertThat(result.getStatus()).isEqualTo(ProcessingResultStatus.SUCCESS);

        queueTaskChecker.assertQueueTaskNotCreated(QueueType.EXPORT_ORDER_ARRIVED_PICKUP_POINT);
    }

    @Test
    @DisplayName("Сохранение кода постамата для заказа ЯДо")
    @DatabaseSetup(
        value = "/service/les/before/yado_order.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/service/les/after/order_code_changed.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void saveLockerCodeYadoOrder() {
        ProcessingResult result = saveLockerCodeProcessor.processPayload(getPayload("code"));
        softly.assertThat(result.getStatus()).isEqualTo(ProcessingResultStatus.SUCCESS);

        queueTaskChecker.assertQueueTaskCreated(
            QueueType.EXPORT_ORDER_ARRIVED_PICKUP_POINT,
            PayloadFactory.lesOrderArrivedPickupPointEventPayload(1, 1, FIXED_TIME, "1", 1)
        );
    }

    @Test
    @DisplayName("Сохранение кода постамата для заказа GO")
    @DatabaseSetup(
        value = "/service/les/before/go_order.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/service/les/after/order_code_changed.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void saveLockerCodeGoOrder() {
        ProcessingResult result = saveLockerCodeProcessor.processPayload(getPayload("code"));
        softly.assertThat(result.getStatus()).isEqualTo(ProcessingResultStatus.SUCCESS);

        queueTaskChecker.assertQueueTaskCreated(
            QueueType.EXPORT_ORDER_ARRIVED_PICKUP_POINT,
            PayloadFactory.lesOrderArrivedPickupPointEventPayload(1, 1, FIXED_TIME, "1", 1)
        );
    }

    @Test
    @DisplayName("Код в заказе отличается от полученного")
    @DatabaseSetup(
        value = "/service/les/before/order_with_code.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/service/les/after/order_code_unchanged.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void differentCodeAlreadyExist() {
        ProcessingResult result = saveLockerCodeProcessor.processPayload(getPayload("another_code"));
        softly.assertThat(result).isEqualTo(ProcessingResult.unprocessed(
            "Code 'code' in order and new code 'another_code' are different"
        ));
    }

    @Test
    @DisplayName("Код в заказе не отличается от полученного")
    @DatabaseSetup(
        value = "/service/les/before/order_with_code.xml",
        type = DatabaseOperation.UPDATE
    )
    @DatabaseSetup(
        value = "/service/les/before/yado_order.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/service/les/after/order_code_unchanged.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void sameCodeAlreadyExist() {
        ProcessingResult result = saveLockerCodeProcessor.processPayload(getPayload("code"));
        softly.assertThat(result.getStatus()).isEqualTo(ProcessingResultStatus.SUCCESS);
        queueTaskChecker.assertQueueTaskNotCreated(QueueType.EXPORT_ORDER_ARRIVED_PICKUP_POINT);
    }

    @Nonnull
    private LockerCodePayload getPayload(String code) {
        return new LockerCodePayload(REQUEST_ID, "LO-123", code);
    }
}
