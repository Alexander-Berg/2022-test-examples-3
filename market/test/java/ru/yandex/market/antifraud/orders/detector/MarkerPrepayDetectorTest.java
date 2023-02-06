package ru.yandex.market.antifraud.orders.detector;

import java.util.Optional;
import java.util.Set;

import org.junit.Test;

import ru.yandex.market.antifraud.orders.entity.AntifraudAction;
import ru.yandex.market.antifraud.orders.entity.UserMarkers;
import ru.yandex.market.antifraud.orders.model.OrderDataContainer;
import ru.yandex.market.antifraud.orders.model.OrderDetectorResult;
import ru.yandex.market.antifraud.orders.storage.entity.rules.MarkerDetectorConfiguration;
import ru.yandex.market.antifraud.orders.util.concurrent.FutureValueHolder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author dzvyagin
 */
public class MarkerPrepayDetectorTest {

    @Test
    public void detectPrepay() {
        MarkerPrepayDetector detector = new MarkerPrepayDetector();
        MarkerDetectorConfiguration conf = new MarkerDetectorConfiguration(true, Set.of("test_marker"));
        OrderDataContainer container = OrderDataContainer.builder()
            .userMarkers(new FutureValueHolder<>(
                    Optional.of(
                        UserMarkers.builder().markers(Set.of("test_marker", "test_marker1")).build())
                )
            )
            .build();
        OrderDetectorResult result = detector.detectFraud(container, conf);
        assertThat(result.isFraud()).isTrue();
        assertThat(result.getActions()).contains(AntifraudAction.PREPAID_ONLY);
        assertThat(result.getAnswerText()).isEqualTo("Включена предоплата из-за наличия маркеров: [test_marker]");
        assertThat(result.getReason()).isEqualTo("Включена предоплата из-за наличия маркеров: [test_marker]");
    }

    @Test
    public void shouldNotWork() {
        MarkerPrepayDetector detector = new MarkerPrepayDetector();
        MarkerDetectorConfiguration conf = new MarkerDetectorConfiguration(true, Set.of("test_marker"));
        OrderDataContainer container = OrderDataContainer.builder()
            .userMarkers(new FutureValueHolder<>(
                    Optional.of(
                        UserMarkers.builder().markers(Set.of("test_marker2", "test_marker1")).build())
                )
            )
            .build();
        OrderDetectorResult result = detector.detectFraud(container, conf);
        assertThat(result.isFraud()).isFalse();
    }

}
