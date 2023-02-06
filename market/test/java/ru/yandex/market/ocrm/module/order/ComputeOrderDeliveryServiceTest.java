package ru.yandex.market.ocrm.module.order;

import java.util.List;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.jmf.utils.Maps;
import ru.yandex.market.ocrm.module.order.domain.Order;
import ru.yandex.market.ocrm.module.order.test.OrderTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Transactional
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = ModuleOrderTestConfiguration.class)
public class ComputeOrderDeliveryServiceTest {

    @Inject
    OrderTestUtils orderTestUtils;

    @BeforeEach
    void setUp() {
        orderTestUtils.clearCheckouterAPI();
    }

    /**
     * Тест проверяет значение вычислимого атрибута заказа: Способ (код: deliveryServiceFull)
     * (скрипт computeOrderDeliveryService.groovy)
     * <p>
     * Создаем заказ с заполненными атрибутами из {@link #dataForTest()}
     * Проверяем, что атрибут "Способ" равен значению <code>expected</code>
     *
     * @param testTitle         название теста
     * @param deliveryType      атрибут "Тип доставки"
     * @param deliveryService   атрибут "Служба доставки"
     * @param deliveryServiceId атрибут "Идентификатор службы доставки"
     * @param deliveryFeatures  атрибут "Дополнительные опции доставки"
     * @param expected          ожидаемое значение атрибута "Способ"
     */
    @MethodSource("dataForTest")
    @ParameterizedTest(name = "{0}")
    public void testOrderDeliveryService(String testTitle,
                                         String deliveryType,
                                         String deliveryService,
                                         Long deliveryServiceId,
                                         List<String> deliveryFeatures,
                                         String expected) {
        Order order = orderTestUtils.createOrder(Maps.of(
                Order.DELIVERY_TYPE, deliveryType,
                Order.DELIVERY_SERVICE, deliveryService,
                Order.DELIVERY_SERVICE_ID, deliveryServiceId
        ));
        orderTestUtils.mockOrder(order.getOrderId(), Maps.of(Order.DELIVERY_FEATURES, deliveryFeatures));

        assertEquals(expected, order.getDeliveryServiceFull());
    }

    private static Stream<Arguments> dataForTest() {
        return Stream.of(
                Arguments.of(
                        "ПВЗ по клику",
                        "PICKUP",
                        "105",
                        105L,
                        List.of("ON_DEMAND_MARKET_PICKUP"),
                        "ПВЗ по клику"
                ),
                Arguments.of(
                        "Самовывоз",
                        "PICKUP",
                        "105",
                        105L,
                        List.of("ON_DEMAND"),
                        "Самовывоз: Maxima Express"
                ),
                Arguments.of(
                        "Доставка курьером",
                        "DELIVERY",
                        "105",
                        105L,
                        List.of(),
                        "Доставка курьером: Maxima Express"
                ),
                Arguments.of(
                        "Доставка Почтой России",
                        "POST",
                        "105",
                        105L,
                        List.of(),
                        "Доставка Почтой России: Maxima Express"
                ),
                Arguments.of(
                        "Неизвестная СД",
                        "DELIVERY",
                        null,
                        666777888L,
                        List.of(),
                        "Доставка курьером: Служба доставки 666777888"
                )
        );
    }
}
