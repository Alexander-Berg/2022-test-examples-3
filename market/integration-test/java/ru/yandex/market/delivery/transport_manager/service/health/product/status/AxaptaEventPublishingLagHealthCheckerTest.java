package ru.yandex.market.delivery.transport_manager.service.health.product.status;

import java.time.ZoneId;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;

@DatabaseSetup("/repository/logbroker/unpublished_axapta_event.xml")
public class AxaptaEventPublishingLagHealthCheckerTest extends AbstractContextualTest {
    @Autowired
    private PublishingLagHealthChecker checker;

    @Test
    void checkPublishingLagOk() {
        checkPublishingLag("2020-12-30T14:10:00.0", "0;OK");
    }

    @Test
    void checkPublishingLagWarn() {
        checkPublishingLag("2020-12-30T14:17:00.0", "1;Unpublished to logbroker status created PT10M ago");
    }

    @Test
    void checkPublishingLagErr() {
        checkPublishingLag("2020-12-30T14:37:00.0", "2;Unpublished to logbroker status created PT30M ago");
    }

    private void checkPublishingLag(String time, String message) {
        clock.setFixed(
            toInstant(time),
            ZoneId.systemDefault()
        );
        softly
            .assertThat(checker.checkAxaptaEventPublishingLag())
            .isEqualTo(message);
    }

    @DatabaseSetup("/repository/logbroker/unpublished_axapta_event_correct_publishing_order.xml")
    @Test
    void checkPublishingOrderOk() {
        softly
            .assertThat(checker.checkAxaptaEventPublishingOrder())
            .isEqualTo("0;OK");
    }

    @Test
    void checkPublishingOrderErr() {
        softly
            .assertThat(checker.checkAxaptaEventPublishingOrder())
            .isEqualTo("2;Wrong order of publishing axapta events to logbroker");
    }
}
