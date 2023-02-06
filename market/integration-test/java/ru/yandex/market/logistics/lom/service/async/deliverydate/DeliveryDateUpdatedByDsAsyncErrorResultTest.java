package ru.yandex.market.logistics.lom.service.async.deliverydate;

import java.time.Instant;
import java.util.List;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.common.util.DateTimeUtils;
import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.jobs.model.ProcessingResult;
import ru.yandex.market.logistics.lom.model.async.GetOrdersDeliveryDateErrorDto;
import ru.yandex.market.logistics.lom.repository.BusinessProcessStateRepository;
import ru.yandex.market.logistics.lom.service.async.ProcessDeliveryDateUpdatedByDsAsyncResultService;

@DisplayName("Обработка неуспешного результата обновления даты заказа")
@DatabaseSetup("/service/async/deliverydates/before/prepare_update_delivery_date_process.xml")
class DeliveryDateUpdatedByDsAsyncErrorResultTest extends AbstractContextualTest {

    @Autowired
    private ProcessDeliveryDateUpdatedByDsAsyncResultService asyncResultService;

    @Autowired
    private BusinessProcessStateRepository repository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @BeforeEach
    void setUp() {
        clock.setFixed(Instant.parse("2021-10-06T12:00:30.00Z"), DateTimeUtils.MOSCOW_ZONE);
    }

    @Test
    @DisplayName("Техническая ошибка, бизнес процесс перевыставляется")
    @ExpectedDatabase(
        value = "/service/async/deliverydates/after/success_process_error.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void techError() {
        ProcessingResult processingResult = processError(true);

        softly.assertThat(processingResult).isEqualTo(ProcessingResult.canBeRestarted(
            "Error getting delivery date. Unexpected technical error. Please check logs for details."
        ));
        softly.assertThat(backLogCaptor.getResults().toString())
            .contains(
                "level=ERROR\t"
                    + "format=plain\t"
                    + "payload=Error getting delivery date. Tech error: true, message: Error getting delivery date. "
                    + "Unexpected technical error. Please check logs for details.\t"
                    + "request_id=1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd\t"
                    + "entity_types=partner,orderIds\t"
                    + "entity_values=partner:48,orderIds:[test barcode]"
            );
    }

    @Test
    @DisplayName("Не техническая ошибка: бизнес процесс не перевыставляется, происходит только запись в лог")
    @ExpectedDatabase(
        value = "/service/async/deliverydates/before/prepare_update_delivery_date_process.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void notTechError() {
        ProcessingResult processingResult = processError(false);

        softly.assertThat(processingResult).isEqualTo(ProcessingResult.success());
        softly.assertThat(backLogCaptor.getResults().toString())
            .contains(
                "level=ERROR\t"
                    + "format=plain\t"
                    + "payload=Error getting delivery date. Tech error: false, message: Some message\t"
                    + "request_id=1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd\t"
                    + "entity_types=partner,orderIds\t"
                    + "entity_values=partner:48,orderIds:[test barcode]"
            );
    }

    private ProcessingResult processError(boolean isTechError) {
        return transactionTemplate.execute(ts -> {
            return asyncResultService.processError(repository.getOne(100L), dateErrorDto(isTechError));
        });
    }

    @Nonnull
    private GetOrdersDeliveryDateErrorDto dateErrorDto(boolean isTechError) {
        return new GetOrdersDeliveryDateErrorDto(
            100L,
            List.of("test barcode"),
            48L,
            isTechError,
            null,
            isTechError ?
                "Error getting delivery date. Unexpected technical error. Please check logs for details."
                : "Some message"
        );
    }
}
