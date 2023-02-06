package ru.yandex.market.delivery.transport_manager.converter;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.dto.order.full.ItemDto;
import ru.yandex.market.delivery.transport_manager.domain.dto.order.full.KorobyteDto;
import ru.yandex.market.delivery.transport_manager.domain.dto.order.full.LocationDto;
import ru.yandex.market.delivery.transport_manager.domain.dto.order.full.MonetaryDto;
import ru.yandex.market.delivery.transport_manager.domain.dto.order.full.OrderDto;
import ru.yandex.market.delivery.transport_manager.domain.dto.order.full.OrderItemBoxDto;
import ru.yandex.market.delivery.transport_manager.domain.dto.order.full.ShipmentDto;
import ru.yandex.market.delivery.transport_manager.domain.dto.order.full.WaybillSegmentDto;
import ru.yandex.market.delivery.transport_manager.domain.enums.CargoType;
import ru.yandex.market.delivery.transport_manager.domain.enums.OrderStatus;
import ru.yandex.market.delivery.transport_manager.domain.enums.SegmentType;
import ru.yandex.market.delivery.transport_manager.domain.enums.ShipmentType;
import ru.yandex.market.delivery.transport_manager.domain.enums.VatType;

public class LomConverterTest extends AbstractContextualTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void writeLomOrderToJsonAndReadTmOrder() throws IOException {
        String lomJson = objectMapper.writeValueAsString(createLomOrderDto());

        assertThatModelEquals(
            createTmOrderDto(),
            objectMapper.readValue(lomJson, OrderDto.class)
        );
    }

    @Nonnull
    private ru.yandex.market.logistics.lom.model.dto.OrderDto createLomOrderDto() {
        return new ru.yandex.market.logistics.lom.model.dto.OrderDto()
            .setId(101L)
            .setBarcode("103XX-56")
            .setStatus(ru.yandex.market.logistics.lom.model.enums.OrderStatus.PROCESSING)
            .setSenderId(200L)
            .setMarketIdFrom(300L)
            .setReturnSortingCenterId(400L)
            .setItems(List.of(
                ru.yandex.market.logistics.lom.model.dto.ItemDto.builder()
                    .name("Тестовый товар")
                    .vendorId(500L)
                    .article("12445204-B")
                    .count(4)
                    .price(
                        ru.yandex.market.logistics.lom.model.dto.MonetaryDto.builder()
                            .value(BigDecimal.TEN)
                            .build()
                    )
                    .vatType(ru.yandex.market.logistics.lom.model.enums.VatType.VAT_20)
                    .dimensions(
                        ru.yandex.market.logistics.lom.model.dto.KorobyteDto.builder()
                            .height(10)
                            .length(20)
                            .width(30)
                            .weightGross(new BigDecimal("33.5"))
                            .build()
                    )
                    .boxes(List.of(
                        ru.yandex.market.logistics.lom.model.dto.OrderItemBoxDto.builder()
                            .storageUnitExternalIds(Set.of("1000-SE"))
                            .build()
                    ))
                    .removableIfAbsent(false)
                    .instances(List.of(Map.of("TT203", "203")))
                    .categoryName("Тестовая категория")
                    .cargoTypes(Set.of(ru.yandex.market.logistics.lom.model.enums.CargoType.ART))
                    .build()
            ))
            .setWaybill(List.of(
                ru.yandex.market.logistics.lom.model.dto.WaybillSegmentDto.builder()
                    .partnerId(77L)
                    .externalId("28951-SK")
                    .shipment(
                        ru.yandex.market.logistics.lom.model.dto.WaybillSegmentDto.ShipmentDto.builder()
                            .type(ru.yandex.market.logistics.lom.model.enums.ShipmentType.IMPORT)
                            .date(LocalDate.parse("2020-12-03"))
                            .locationFrom(
                                ru.yandex.market.logistics.lom.model.dto.LocationDto.builder()
                                    .warehouseId(88L)
                                    .build()
                            )
                            .locationTo(
                                ru.yandex.market.logistics.lom.model.dto.LocationDto.builder()
                                    .warehouseId(99L)
                                    .build()
                            )
                            .build()
                    )
                    .segmentType(ru.yandex.market.logistics.lom.model.enums.SegmentType.MOVEMENT)
                    .build()
            ));
    }


    @Nonnull
    private OrderDto createTmOrderDto() {
        return new OrderDto()
            .setId(101L)
            .setBarcode("103XX-56")
            .setStatus(OrderStatus.PROCESSING)
            .setSenderId(200L)
            .setMarketIdFrom(300L)
            .setReturnSortingCenterId(400L)
            .setItems(List.of(
                new ItemDto()
                    .setName("Тестовый товар")
                    .setVendorId(500L)
                    .setArticle("12445204-B")
                    .setCount(4)
                    .setPrice(new MonetaryDto().setValue(BigDecimal.TEN))
                    .setVatType(VatType.VAT_20)
                    .setDimensions(
                        new KorobyteDto()
                            .setHeight(10)
                            .setLength(20)
                            .setWidth(30)
                            .setWeightGross(new BigDecimal("33.5"))
                    )
                    .setBoxes(List.of(
                        new OrderItemBoxDto().setStorageUnitExternalIds(Set.of("1000-SE"))
                    ))
                    .setRemovableIfAbsent(false)
                    .setInstances(List.of(Map.of("TT203", "203")))
                    .setCategoryName("Тестовая категория")
                    .setCargoTypes(Set.of(CargoType.ART))
            ))
            .setWaybill(List.of(
                new WaybillSegmentDto()
                    .setPartnerId(77L)
                    .setExternalId("28951-SK")
                    .setShipment(
                        new ShipmentDto()
                            .setType(ShipmentType.IMPORT)
                            .setDate(LocalDate.parse("2020-12-03"))
                            .setLocationFrom(new LocationDto().setWarehouseId(88L))
                            .setLocationTo(new LocationDto().setWarehouseId(99L))
                    )
                    .setSegmentType(SegmentType.MOVEMENT)
            ));
    }
}
