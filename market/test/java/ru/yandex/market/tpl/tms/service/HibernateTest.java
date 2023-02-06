package ru.yandex.market.tpl.tms.service;

import java.util.stream.Collectors;
import java.util.stream.LongStream;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.tpl.core.domain.order.OrderRepository;
import ru.yandex.market.tpl.tms.test.TplTmsAbstractTest;


@RequiredArgsConstructor
public class HibernateTest extends TplTmsAbstractTest {

    private final OrderRepository orderRepository;

    @Test
    public void testSelectInHugeAmountOfValues() {
        var lotsOfIds = LongStream.rangeClosed(1, 30000)
                .boxed().collect(Collectors.toList());
        Assertions.assertDoesNotThrow(() -> orderRepository.findAllById(lotsOfIds));
    }
}
