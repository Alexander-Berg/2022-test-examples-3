package ru.yandex.market.delivery.transport_manager.service.health.product.status;

import java.time.ZoneId;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;

@DatabaseSetup("/repository/logbroker/unpublished_status_history.xml")
class StatusHistoryPublishingLagHealthCheckerTest extends AbstractContextualTest {
    @Autowired
    private PublishingLagHealthChecker checker;

    @Test
    void checkPublishingLagOk() {
        checkPublishingLag("2020-12-30T14:10:00.0", "0;OK");
    }

    @Test
    void checkPublishingLagWarn() {
        checkPublishingLag("2020-12-30T14:20:00.0", "1;Unpublished to logbroker status created PT10M ago");
    }

    @Test
    void checkPublishingLagErr() {
        checkPublishingLag("2020-12-30T14:40:00.0", "2;Unpublished to logbroker status created PT30M ago");
    }

    private void checkPublishingLag(String time, String message) {
        clock.setFixed(
            toInstant(time),
            ZoneId.systemDefault()
        );
        softly
            .assertThat(checker.checkStatusHistoryPublishingLag())
            .isEqualTo(message);
    }

    @DatabaseSetup("/repository/logbroker/unpublished_status_history_correct_publishing_order.xml")
    @Test
    void checkPublishingOrderOk() {
        softly
            .assertThat(checker.checkStatusHistoryPublishingOrder())
            .isEqualTo("0;OK");
    }

    @Test
    void checkPublishingOrderErr() {
        softly
            .assertThat(checker.checkStatusHistoryPublishingOrder())
            .isEqualTo("2;Wrong order of publishing status history to logbroker");
    }
}
