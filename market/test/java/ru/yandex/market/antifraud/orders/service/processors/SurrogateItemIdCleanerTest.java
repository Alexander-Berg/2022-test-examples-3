package ru.yandex.market.antifraud.orders.service.processors;

import java.util.List;

import org.junit.Test;

import ru.yandex.market.antifraud.orders.entity.OrderVerdict;
import ru.yandex.market.antifraud.orders.web.dto.checkouter.OrderItemResponseDto;
import ru.yandex.market.antifraud.orders.web.dto.checkouter.OrderResponseDto;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * @author dzvyagin
 */
public class SurrogateItemIdCleanerTest {

    @Test
    public void process() {
        OrderCheckPostprocessor postprocessor = new SurrogateItemIdCleaner();
        OrderVerdict verdict = OrderVerdict.builder()
                .fixedOrder(new OrderResponseDto(
                        List.of(
                                OrderItemResponseDto.builder().id(-1L).offerId("-1").build(),
                                OrderItemResponseDto.builder().id(null).offerId("null").build(),
                                OrderItemResponseDto.builder().id(1L).offerId("1").build()
                        )
                ))
                .isDegradation(false)
                .build();
        verdict = postprocessor.process(null, null, verdict);
        assertThat(verdict.getFixedOrder().getItems()).containsExactly(
                OrderItemResponseDto.builder().id(null).offerId("-1").build(),
                OrderItemResponseDto.builder().id(null).offerId("null").build(),
                OrderItemResponseDto.builder().id(1L).offerId("1").build()
        );
    }

    @Test
    public void processEmpty() {
        OrderCheckPostprocessor postprocessor = new SurrogateItemIdCleaner();
        OrderVerdict verdict = OrderVerdict.EMPTY;
        assertThat(postprocessor.process(null, null, verdict)).isEqualTo(verdict);
    }
}
