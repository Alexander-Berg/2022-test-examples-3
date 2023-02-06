package ru.yandex.market.wms.api.provider;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;

import ru.yandex.market.wms.common.model.enums.OrderType;
import ru.yandex.market.wms.common.spring.domain.dto.OrderDTO;
import ru.yandex.market.wms.common.spring.domain.dto.OrderDetailDTO;
import ru.yandex.market.wms.common.spring.domain.dto.OrderPackDetailDTO;

public class OrderDtoProvider {

    private OrderDtoProvider() {
    }

    public static OrderDTO getOrderDTO(String originOrderKey,
                                       OffsetDateTime scheduledShipDate,
                                       List<OrderPackDetailDTO> orderPackDetailDTOS,
                                       String status,
                                       int totalQty) {
        OrderDTO expectedOrderDTO = OrderDTO.builder()
                .status(status)
                .orderkey(originOrderKey)
                .externorderkey(originOrderKey + "EXT")
                .door("door")
                .carriercode("code")
                .carriername("name")
                .scheduledshipdate(scheduledShipDate)
                .totalqty(BigDecimal.valueOf(totalQty))
                .susr2("1")
                .type(OrderType.OUTBOUND_AUTO.getCode())
                .maxAbsentItemsPricePercent(99.0F)
                .orderdetails(Collections.emptyList())
                .orderPacks(orderPackDetailDTOS)
                .build();

        return expectedOrderDTO;
    }

    public static OrderDTO getOrderDTO(String originOrderKey,
                                       OffsetDateTime scheduledShipDate,
                                       List<OrderPackDetailDTO> orderPackDetailDTOS,
                                       String status,
                                       int totalQty,
                                       List<OrderDetailDTO> orderDetailDTOS) {
        OrderDTO expectedOrderDTO = OrderDTO.builder()
                .status(status)
                .orderkey(originOrderKey)
                .externorderkey(originOrderKey + "EXT")
                .door("door")
                .carriercode("code")
                .carriername("name")
                .scheduledshipdate(scheduledShipDate)
                .totalqty(BigDecimal.valueOf(totalQty))
                .susr2("1")
                .type(OrderType.OUTBOUND_AUTO.getCode())
                .maxAbsentItemsPricePercent(99.0F)
                .orderdetails(orderDetailDTOS)
                .orderPacks(orderPackDetailDTOS)
                .build();

        return expectedOrderDTO;
    }
}
