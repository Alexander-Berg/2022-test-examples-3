package ru.yandex.market.wms.autostart.validation.orders;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.wms.autostart.validation.ValidationResult;
import ru.yandex.market.wms.common.spring.BaseTest;
import ru.yandex.market.wms.common.spring.dao.entity.Order;
import ru.yandex.market.wms.common.spring.dao.entity.WaveDetails;
import ru.yandex.market.wms.common.spring.dao.implementation.WaveDetailDao;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class OrderIsNotInWaveValidationRuleTest extends BaseTest {

    private WaveDetailDao waveDetailDao;
    private OrderIsNotInWaveValidationRule validationRule;

    @BeforeEach
    public void setup() {
        super.setup();
        waveDetailDao = mock(WaveDetailDao.class);
        validationRule = new OrderIsNotInWaveValidationRule(waveDetailDao);
    }

    @Test
    void validateOrderKeysListIsNull() {
        ValidationResult result = validationRule.validate(null);

        assertions.assertThat(result.isOk()).isTrue();
        assertions.assertThat(result.getMessage()).isNull();
        assertions.assertThat(result.getRuleName()).isEqualTo("OrderIsNotInWaveValidationRule");
        verify(waveDetailDao, never()).findWaveDetailsByOrderKeys(anyList());
    }

    @Test
    void validateOrderKeysListIsEmpty() {
        ValidationResult result = validationRule.validate(Collections.emptyList());

        assertions.assertThat(result.isOk()).isTrue();
        assertions.assertThat(result.getMessage()).isNull();
        assertions.assertThat(result.getRuleName()).isEqualTo("OrderIsNotInWaveValidationRule");
        verify(waveDetailDao, never()).findWaveDetailsByOrderKeys(anyList());
    }

    @Test
    void validateOrdersAreNotInWave() {
        doReturn(new ArrayList<WaveDetails>()).when(waveDetailDao).findWaveDetailsByOrderKeys(anyList());

        List<Order> orders = List.of(Order.builder()
                        .orderKey("order-1")
                        .build(),
                Order.builder()
                        .orderKey("order-2")
                        .build(),
                Order.builder()
                        .orderKey("order-3")
                        .build());
        ValidationResult result = validationRule.validate(orders);

        assertions.assertThat(result.isOk()).isTrue();
        assertions.assertThat(result.getMessage()).isNull();
        assertions.assertThat(result.getRuleName()).isEqualTo("OrderIsNotInWaveValidationRule");
    }

    @Test
    void validateSomeOrdersAreInWave() {
        List<WaveDetails> waveDetails = Arrays.asList(
                WaveDetails.builder()
                        .waveKey("wave-1")
                        .orderKey("order-1")
                        .build(),
                WaveDetails.builder()
                        .waveKey("wave-3")
                        .orderKey("order-3")
                        .build()
        );
        doReturn(waveDetails).when(waveDetailDao).findWaveDetailsByOrderKeys(anyList());

        List<Order> orders = List.of(Order.builder()
                        .orderKey("order-1")
                        .build(),
                Order.builder()
                        .orderKey("order-2")
                        .build(),
                Order.builder()
                        .orderKey("order-3")
                        .build());
        ValidationResult result = validationRule.validate(orders);

        assertions.assertThat(result.isOk()).isFalse();
        assertions.assertThat(result.getMessage())
                .isEqualTo("Некоторые заказы уже добавлены в волну: заказ order-1 входит в волну wave-1, заказ " +
                        "order-3 входит в волну wave-3");
        assertions.assertThat(result.getRuleName()).isEqualTo("OrderIsNotInWaveValidationRule");
    }
}
