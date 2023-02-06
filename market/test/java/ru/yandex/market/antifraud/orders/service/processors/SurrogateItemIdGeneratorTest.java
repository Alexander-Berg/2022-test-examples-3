package ru.yandex.market.antifraud.orders.service.processors;

import java.util.List;

import org.junit.Test;

import ru.yandex.market.antifraud.orders.web.dto.checkouter.CartRequestDto;
import ru.yandex.market.antifraud.orders.web.dto.checkouter.MultiCartRequestDto;
import ru.yandex.market.antifraud.orders.web.dto.checkouter.OrderItemRequestDto;
import ru.yandex.market.antifraud.orders.web.dto.checkouter.OrderRequestDto;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author dzvyagin
 */
public class SurrogateItemIdGeneratorTest {

    @Test
    public void process() {
        OrderCheckPreprocessor processor = new SurrogateItemIdGenerator();
        var requestDto = MultiCartRequestDto.builder()
            .carts(List.of(
                CartRequestDto.builder()
                    .items(List.of(
                        OrderItemRequestDto.builder().offerId("1").build(),
                        OrderItemRequestDto.builder().offerId("2").build()
                    ))
                    .build()))
            .build();
        requestDto = processor.process(requestDto);
        assertThat(requestDto.getCarts().get(0).getItems())
            .containsExactly(
                OrderItemRequestDto.builder().id(-1L).offerId("1").build(),
                OrderItemRequestDto.builder().id(-2L).offerId("2").build()
            );
    }

}
