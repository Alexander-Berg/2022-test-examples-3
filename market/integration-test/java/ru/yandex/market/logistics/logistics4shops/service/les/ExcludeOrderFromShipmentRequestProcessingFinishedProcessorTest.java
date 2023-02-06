package ru.yandex.market.logistics.logistics4shops.service.les;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.les.lom.ExcludeOrderFromShipmentRequestProcessingFinished;
import ru.yandex.market.logistics.les.lom.enums.ExcludeOrderFromShipmentResult;
import ru.yandex.market.logistics.logistics4shops.AbstractIntegrationTest;
import ru.yandex.market.logistics.logistics4shops.service.les.processor.ExcludeOrderFromShipmentRequestProcessingFinishedProcessor;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;

@DisplayName("Обработчик событий LES: результат заявки на исключение заказа из отгрузки")
@DatabaseSetup("/service/les/excludeorderfromshipment/export/before/prepare.xml")
class ExcludeOrderFromShipmentRequestProcessingFinishedProcessorTest extends AbstractIntegrationTest {
    @Autowired
    private ExcludeOrderFromShipmentRequestProcessingFinishedProcessor processor;

    @Test
    @DisplayName("Обработать успех обработки заявки")
    @ExpectedDatabase(
        value = "/service/les/excludeorderfromshipment/export/after/queue_task_success_created.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void processingSuccessEvent() {
        processor.process(
            new ExcludeOrderFromShipmentRequestProcessingFinished(
                "100",
                1L,
                ExcludeOrderFromShipmentResult.SUCCESS
            ),
            "1"
        );
    }

    @Test
    @DisplayName("Обработать ошибку обработки заявки")
    @ExpectedDatabase(
        value = "/service/les/excludeorderfromshipment/export/after/queue_task_fail_created.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void processingFailEvent() {
        processor.process(
            new ExcludeOrderFromShipmentRequestProcessingFinished(
                "100",
                1L,
                ExcludeOrderFromShipmentResult.FAIL
            ),
            "1"
        );
    }

    @Test
    @DisplayName("Обработать завершение обработки заявки - событие невалидное")
    @ExpectedDatabase(value = "/jobs/no_tasks.xml", assertionMode = NON_STRICT_UNORDERED)
    void processingNullEvent() {
        processor.process(
            new ExcludeOrderFromShipmentRequestProcessingFinished(
                "100",
                1L,
                null
            ),
            "1"
        );
    }
}
