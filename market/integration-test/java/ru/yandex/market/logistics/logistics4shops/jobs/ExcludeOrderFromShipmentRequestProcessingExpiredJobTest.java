package ru.yandex.market.logistics.logistics4shops.jobs;

import java.time.Instant;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.util.DateTimeUtils;
import ru.yandex.market.logistics.logistics4shops.AbstractIntegrationTest;
import ru.yandex.market.logistics.logistics4shops.logging.code.ExcludeOrderFromShipmentRequestEventCode;
import ru.yandex.market.logistics.logistics4shops.utils.logging.TskvLogRecord;

import static ru.yandex.market.logistics.logistics4shops.utils.logging.BackLogAssertions.logEqualsTo;

@DisplayName("Фоновая задача мониторинга зависших заявок на перенос заказа из отгрузки")
class ExcludeOrderFromShipmentRequestProcessingExpiredJobTest extends AbstractIntegrationTest {
    @Autowired
    private ExcludeOrderFromShipmentRequestProcessingExpiredJob job;

    @BeforeEach
    void setUp() {
        clock.setFixed(Instant.parse("2021-12-16T11:30:00.00Z"), DateTimeUtils.MOSCOW_ZONE);
    }

    @Test
    @DisplayName("Заявка уже в терминальном статусе")
    @DatabaseSetup("/jobs/exclude_order_from_shipment_request/before/request_in_terminal_status.xml")
    void requestInTerminalStatus() throws JobExecutionException {
        job.execute(null);
        assertLogs().isEmpty();
    }

    @Test
    @DisplayName("Заявка в активном статусе - обновлялась недавно")
    @DatabaseSetup("/jobs/exclude_order_from_shipment_request/before/request_is_processing_recently.xml")
    void requestInActiveStatusRecently() throws JobExecutionException {
        job.execute(null);
        assertLogs().isEmpty();
    }

    @Test
    @DisplayName("Заявка в активном статусе - обновлялась давно")
    @DatabaseSetup("/jobs/exclude_order_from_shipment_request/before/request_is_processing.xml")
    void requestInActiveStatusForLong() throws JobExecutionException {
        job.execute(null);
        assertLogs().anyMatch(logEqualsTo(
            TskvLogRecord.error("Requests [1] processing timeout expired")
                .setLoggingCode(ExcludeOrderFromShipmentRequestEventCode.REQUEST_PROCESSING_TIMEOUT_EXPIRED)
        ));
    }

    @Test
    @DisplayName("Несколько заявок попадает в мониторинг")
    @DatabaseSetup("/jobs/exclude_order_from_shipment_request/before/multiple_requests.xml")
    void multipleExpiredRequests() throws JobExecutionException {
        job.execute(null);
        assertLogs().anyMatch(logEqualsTo(
            TskvLogRecord.error("Requests [1, 3] processing timeout expired")
                .setLoggingCode(ExcludeOrderFromShipmentRequestEventCode.REQUEST_PROCESSING_TIMEOUT_EXPIRED)
        ));
    }
}
