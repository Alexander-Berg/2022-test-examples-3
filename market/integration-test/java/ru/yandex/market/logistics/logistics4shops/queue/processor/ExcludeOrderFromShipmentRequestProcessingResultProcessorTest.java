package ru.yandex.market.logistics.logistics4shops.queue.processor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.logistics4shops.AbstractIntegrationTest;
import ru.yandex.market.logistics.logistics4shops.model.entity.enums.ExcludeOrderFromShipmentRequestStatus;
import ru.yandex.market.logistics.logistics4shops.model.exception.ResourceNotFoundException;
import ru.yandex.market.logistics.logistics4shops.queue.payload.ExcludeOrderFromShipmentRequestProcessingFinishedPayload;
import ru.yandex.market.logistics.logistics4shops.utils.logging.TskvLogRecord;

import static com.github.springtestdbunit.annotation.DatabaseOperation.REFRESH;
import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static ru.yandex.market.logistics.logistics4shops.model.entity.enums.ExcludeOrderFromShipmentRequestStatus.FAIL;
import static ru.yandex.market.logistics.logistics4shops.model.entity.enums.ExcludeOrderFromShipmentRequestStatus.SUCCESS;
import static ru.yandex.market.logistics.logistics4shops.utils.logging.BackLogAssertions.logEqualsTo;

@DisplayName("Обработка результата заявки на исключение заказа из отгрузки")
@DatabaseSetup("/service/les/excludeorderfromshipment/result/before/prepare.xml")
@ParametersAreNonnullByDefault
class ExcludeOrderFromShipmentRequestProcessingResultProcessorTest extends AbstractIntegrationTest {
    @Autowired
    private ExcludeOrderFromShipmentRequestProcessingResultProcessor processor;

    @Test
    @DisplayName("Обработать успех обработки заявки")
    @ExpectedDatabase(
        value = "/service/les/excludeorderfromshipment/result/after/process_success_result.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void processSuccessResult() {
        processor.execute(buildPayload(1L, "100", SUCCESS));
    }

    @Test
    @DisplayName("Обработать ошибку обработки заявки")
    @ExpectedDatabase(
        value = "/service/les/excludeorderfromshipment/result/after/process_fail_result.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void processFailResult() {
        processor.execute(buildPayload(1L, "100", FAIL));
    }

    @Test
    @DisplayName("Обработать завершение обработки заявки - событие невалидно")
    @ExpectedDatabase(
        value = "/service/les/excludeorderfromshipment/result/before/prepare.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void processResultPayloadIsInvalid() {
        softly.assertThatThrownBy(() -> processor.execute(buildPayload(null, null, FAIL)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("RequestId and orderId must not be null");
    }

    @Test
    @DisplayName("Обработать завершение обработки несуществующей заявки")
    @ExpectedDatabase(
        value = "/service/les/excludeorderfromshipment/result/before/prepare.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void processResultRequestNotFound() {
        softly.assertThatThrownBy(() -> processor.execute(buildPayload(2L, "100", FAIL)))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Failed to find [EXCLUDE_ORDER_FROM_SHIPMENT_REQUEST] with id [2]");
    }

    @Test
    @DisplayName("Обработать завершение обработки заявки - заявка не связана с заказом")
    @ExpectedDatabase(
        value = "/service/les/excludeorderfromshipment/result/before/prepare.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void processResultRequestDoeNotMatchWithOrderId() {
        softly.assertThatThrownBy(() -> processor.execute(buildPayload(1L, "200", FAIL)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Request 1 does not match with order 200");
    }

    @Test
    @DisplayName("Обработать завершение обработки заявки - заявка уже в терминальном статусе")
    @DatabaseSetup(
        value = "/service/les/excludeorderfromshipment/result/before/request_in_terminal_status.xml",
        type = REFRESH
    )
    @ExpectedDatabase(
        value = "/service/les/excludeorderfromshipment/result/after/request_in_terminal_status.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void processResultRequestAlreadyInTerminalStatus() {
        processor.execute(buildPayload(1L, "100", FAIL));
        assertLogs().anyMatch(logEqualsTo(TskvLogRecord.warn("Request 1 already has terminal status SUCCESS")));
    }

    @Nonnull
    private ExcludeOrderFromShipmentRequestProcessingFinishedPayload buildPayload(
        @Nullable Long requestId,
        @Nullable String orderBarcode,
        ExcludeOrderFromShipmentRequestStatus status
    ) {
        return ExcludeOrderFromShipmentRequestProcessingFinishedPayload.builder()
            .excludeOrderFromShipmentRequestId(requestId)
            .orderBarcode(orderBarcode)
            .status(status)
            .build();
    }
}
