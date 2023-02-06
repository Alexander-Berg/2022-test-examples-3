package ru.yandex.market.fulfillment.wrap.marschroute.service.calculator;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import ru.yandex.market.fulfillment.wrap.marschroute.entity.service.EntityType;
import ru.yandex.market.fulfillment.wrap.marschroute.entity.service.MarschrouteService;
import ru.yandex.market.fulfillment.wrap.marschroute.entity.service.MarschrouteServiceDiff;
import ru.yandex.market.fulfillment.wrap.marschroute.entity.service.MarschrouteServiceKey;
import ru.yandex.market.fulfillment.wrap.marschroute.service.services.MarschrouteServiceDiffCalculator;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

class MarschrouteServiceDiffCalculatorTest {

    static final MarschrouteServiceKey MARSCHROUTE_SERVICE_KEY = new MarschrouteServiceKey()
        .setEntityId("1")
        .setEntityType(EntityType.ORDER)
        .setServiceDateTime(LocalDate.of(2018, 1, 1).atStartOfDay())
        .setNativeName("Упаковка заказа");

    private final MarschrouteServiceDiffCalculator calculator = new MarschrouteServiceDiffCalculator();

    /**
     * Сценарий, когда и в Маршруте и в прослойке такая услуга ранее не была замечена.
     */
    @Test
    void bothWrapAndMarschrouteServicesMissing() {
        assertThatThrownBy(() -> calculator.calculateDiff(null, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Both marschroute and wrap version of service are null");
    }

    /**
     * Сценарий, в котором в Маршруте появилась услуга, которая ранее не была записана в прослойке.
     * <p>
     * В таком случае калькулятор должен создать diff, идентичной услуге из actualVersion.
     */
    @Test
    void serviceAcquiredFirstTime() {
        BigDecimal sum = BigDecimal.valueOf(200);
        TestRawService testRawService = new TestRawService(
            sum,
            MARSCHROUTE_SERVICE_KEY
        );

        Optional<MarschrouteServiceDiff> optionalDiff = calculator.calculateDiff(
            testRawService,
            null
        );

        assertSoftly(assertions -> {
            assertions.assertThat(optionalDiff)
                .as("Asserting that diff exists")
                .isPresent();

            optionalDiff.ifPresent(diff -> {
                assertions.assertThat(diff.getSum())
                    .as("Asserting sum value")
                    .isEqualByComparingTo(sum);

                assertions.assertThat(diff.getDiff())
                    .as("Asserting diff value")
                    .isEqualByComparingTo(sum);

                assertions.assertThat(diff.getVersion())
                    .as("Asserting version value")
                    .isEqualTo(MarschrouteServiceDiffCalculator.INITIAL_VERSION);
            });
        });
    }

    /**
     * Сценарий, в котором в Маршруте исчезла услуга, которая ранее была записана в прослойке.
     * <p>
     * В таком случае калькулятор должен создать diff, который отменит последнюю существующую в прослойке услугу.
     * (sum = 0, diff = 0 - wrapSum,version = wrapVersion +1)
     */
    @Test
    void serviceRemovedFromMarschroute() {
        Optional<MarschrouteServiceDiff> optionalDiff = calculator.calculateDiff(
            null,
            new MarschrouteService()
                .setServiceKey(MARSCHROUTE_SERVICE_KEY)
                .setServiceDiff(
                    new MarschrouteServiceDiff()
                        .setSum(BigDecimal.valueOf(200))
                        .setDiff(BigDecimal.valueOf(100))
                        .setVersion(5)
                )
        );

        assertSoftly(assertions -> {
            assertions.assertThat(optionalDiff)
                .as("Asserting that diff exists")
                .isPresent();

            optionalDiff.ifPresent(diff -> {
                assertions.assertThat(diff.getSum())
                    .as("Asserting sum value")
                    .isEqualByComparingTo(BigDecimal.ZERO);

                assertions.assertThat(diff.getDiff())
                    .as("Asserting diff value")
                    .isEqualByComparingTo(BigDecimal.valueOf(-200));

                assertions.assertThat(diff.getVersion())
                    .as("Asserting version value")
                    .isEqualTo(6);
            });
        });
    }
}
