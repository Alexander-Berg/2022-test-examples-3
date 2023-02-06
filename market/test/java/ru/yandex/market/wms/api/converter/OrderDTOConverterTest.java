package ru.yandex.market.wms.api.converter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.wms.common.spring.converter.OrderDTOConverter;
import ru.yandex.market.wms.common.spring.dao.entity.Order;
import ru.yandex.market.wms.common.spring.domain.dto.OrderDTO;

@ExtendWith(MockitoExtension.class)
public class OrderDTOConverterTest {

    @InjectMocks
    private OrderDTOConverter orderDTOConverter;

    @ParameterizedTest
    @MethodSource("convertTestProvider")
    void convertTest(List<Order> orders, OrderDTO expectedOrderDto) {
        Optional<OrderDTO> orderDTO = orderDTOConverter.aggregateAndConvert(orders);

        Assertions.assertEquals(expectedOrderDto, orderDTO.get());
    }

    private static Stream<Arguments> convertTestProvider() {
        OffsetDateTime scheduledShipDate = OffsetDateTime
                .parse("2016-10-02T20:15:30+01:00", DateTimeFormatter.ISO_DATE_TIME);

        Order order1 = Order.builder()
                .orderKey("A000135435")
                .originOrderKey("0000135435")
                .externalOrderKey("1626784050773001")
                .status("-1")
                .door("")
                .carrierCode("107")
                .carrierName("PickPoint Длинное название перевозчика")
                .scheduledShipDate(scheduledShipDate)
                .totalqty(BigDecimal.valueOf(2))
                .susr2(null)
                .type("0")
                .maxAbsentItemsPricePercent(null)
                .build();

        Order order2 = Order.builder()
                .orderKey("B000135435")
                .originOrderKey("0000135435")
                .externalOrderKey("1626784050773001")
                .status("02")
                .door("")
                .carrierCode("107")
                .carrierName("PickPoint Длинное название перевозчика")
                .scheduledShipDate(scheduledShipDate)
                .totalqty(BigDecimal.valueOf(6))
                .susr2(null)
                .type("0")
                .maxAbsentItemsPricePercent(null)
                .build();

        OrderDTO orderDTO1 = OrderDTO.builder()
                .orderkey("0000135435")
                .externorderkey("1626784050773001")
                .status("-1")
                .door("")
                .carriercode("107")
                .carriername("PickPoint Длинное название перевозчика")
                .scheduledshipdate(scheduledShipDate)
                .totalqty(BigDecimal.valueOf(8))
                .susr2(null)
                .type("0")
                .maxAbsentItemsPricePercent(null)
                .build();

        Order order3 = Order.builder()
                .orderKey("A000135406")
                .originOrderKey("0000135406")
                .externalOrderKey("32782798")
                .status("98")
                .door("")
                .carrierCode("239")
                .carrierName("Маркет Курьерка")
                .scheduledShipDate(scheduledShipDate)
                .totalqty(BigDecimal.valueOf(0))
                .susr2(null)
                .type("0")
                .maxAbsentItemsPricePercent(BigDecimal.valueOf(99.0))
                .build();

        Order order4 = Order.builder()
                .orderKey("B000135406")
                .originOrderKey("0000135406")
                .externalOrderKey("32782798")
                .status("95")
                .door("")
                .carrierCode("239")
                .carrierName("Маркет Курьерка")
                .scheduledShipDate(scheduledShipDate)
                .totalqty(BigDecimal.valueOf(2))
                .susr2(null)
                .type("0")
                .maxAbsentItemsPricePercent(BigDecimal.valueOf(99.0))
                .build();

        OrderDTO orderDTO2 = OrderDTO.builder()
                .orderkey("0000135406")
                .externorderkey("32782798")
                .status("95")
                .door("")
                .carriercode("239")
                .carriername("Маркет Курьерка")
                .scheduledshipdate(scheduledShipDate)
                .totalqty(BigDecimal.valueOf(2))
                .susr2(null)
                .type("0")
                .maxAbsentItemsPricePercent(99.0f)
                .build();

        return Stream.of(
                Arguments.of(
                        Arrays.asList(order1, order2),
                        orderDTO1
                ),
                Arguments.of(
                        Arrays.asList(order3, order4),
                        orderDTO2
                )
        );
    }
}
