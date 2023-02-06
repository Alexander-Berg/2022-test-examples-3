package ru.yandex.market.antifraud.orders.service.processors;

import java.util.List;

import org.junit.Test;

import ru.yandex.market.antifraud.orders.web.dto.checkouter.CartRequestDto;
import ru.yandex.market.antifraud.orders.web.dto.checkouter.MultiCartRequestDto;
import ru.yandex.market.antifraud.orders.web.dto.checkouter.OrderItemRequestDto;
import ru.yandex.market.antifraud.orders.web.dto.checkouter.OrderRequestDto;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * @author Alexander Novikov <a href="mailto:hronos@yandex-team.ru"></a>
 * @date 13.10.2020
 */
public class ZeroCountItemsRemoverTest {
    @Test
    public void process() {
        OrderCheckPreprocessor processor = new ZeroCountItemsRemover();
        var requestDto = MultiCartRequestDto.builder()
            .carts(List.of(
                CartRequestDto.builder()
                    .items(List.of(
                        OrderItemRequestDto.builder().offerId("1").count(2).build(),
                        OrderItemRequestDto.builder().offerId("2").count(0).build()
                    ))
                    .build()))
            .build();
        requestDto = processor.process(requestDto);
        assertThat(requestDto.getCarts().get(0).getItems())
                .containsExactly(
                        OrderItemRequestDto.builder().offerId("1").count(2).build()
                );
    }
}
