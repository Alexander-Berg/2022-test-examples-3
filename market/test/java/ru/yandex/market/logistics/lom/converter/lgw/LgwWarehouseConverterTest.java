package ru.yandex.market.logistics.lom.converter.lgw;

import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistic.gateway.common.model.delivery.Location;
import ru.yandex.market.logistic.gateway.common.model.delivery.Person;
import ru.yandex.market.logistic.gateway.common.model.delivery.Phone;
import ru.yandex.market.logistic.gateway.common.model.delivery.ResourceId;
import ru.yandex.market.logistic.gateway.common.model.delivery.TimeInterval;
import ru.yandex.market.logistic.gateway.common.model.delivery.Warehouse;
import ru.yandex.market.logistic.gateway.common.model.delivery.WorkTime;
import ru.yandex.market.logistics.lom.AbstractTest;
import ru.yandex.market.logistics.lom.LmsModelFactory;
import ru.yandex.market.logistics.lom.converter.AddressNormalizer;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;
import ru.yandex.market.logistics.management.entity.type.PointType;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("Конвертация данных склада из LMS в данные склада для LGW")
class LgwWarehouseConverterTest extends AbstractTest {
    private LgwWarehouseConverter converter;

    @BeforeEach
    void setup() {
        PhoneLgwConverter phoneLgwConverter = new PhoneLgwConverter();

        converter = new LgwWarehouseConverter(
            new WorkTimeLgwConverter(),
            new LocationLgwConverter(new AddressNormalizer()),
            phoneLgwConverter,
            new PersonLgwConverter(phoneLgwConverter)
        );
    }

    @Test
    @DisplayName("Склад для забора, список телефонов null")
    void toLgwWarehousePhoneIsNull() {
        LogisticsPointResponse logisticsPoint = getLogisticsPointResponseBuilder()
            .phones(null)
            .build();

        Warehouse warehouse = converter.toLgwWarehouse(logisticsPoint, 2L);

        Warehouse expectedWarehouse = getExpectedWarehouseBuilder().build();
        assertEquals(expectedWarehouse, warehouse);
    }

    @Test
    @DisplayName("Склад для забора, список телефонов пуст")
    void toLgwWarehousePhoneIsEmpty() {
        LogisticsPointResponse logisticsPoint = getLogisticsPointResponseBuilder()
            .phones(Set.of())
            .build();

        Warehouse warehouse = converter.toLgwWarehouse(logisticsPoint, 2L);

        Warehouse expectedWarehouse = getExpectedWarehouseBuilder().build();
        assertEquals(expectedWarehouse, warehouse);
    }

    @Test
    @DisplayName("Склад для самопривоза")
    void toLgwWarehouseSelfExport() {
        LogisticsPointResponse logisticsPoint = getLogisticsPointResponseBuilder().build();
        Warehouse warehouse = converter.toLgwWarehouseSelfExport(logisticsPoint, 2L);

        Warehouse expectedWarehouse = getExpectedWarehouseBuilder("externalId")
            .setPhones(List.of(new Phone("+79232435555", "777")))
            .build();
        assertEquals(expectedWarehouse, warehouse);
    }

    @Nonnull
    private LogisticsPointResponse.LogisticsPointResponseBuilder getLogisticsPointResponseBuilder() {
        return LmsModelFactory.createLogisticsPointResponse(1L, 10L, "warehouse", PointType.WAREHOUSE)
            .instruction("instruction");
    }

    @Nonnull
    private Warehouse.WarehouseBuilder getExpectedWarehouseBuilder() {
        return getExpectedWarehouseBuilder(null);
    }

    @Nonnull
    private Warehouse.WarehouseBuilder getExpectedWarehouseBuilder(String partnerId) {
        return new Warehouse.WarehouseBuilder(
            ResourceId.builder()
                .setYandexId("1")
                .setPartnerId(partnerId)
                .build(),
            new Location.LocationBuilder(
                "Россия",
                "Новосибирск",
                "Регион"
            )
                .setStreet("Николаева")
                .setHouse("1")
                .setBuilding("")
                .setHousing("1")
                .setZipCode("649220")
                .setRoom("")
                .setLocationId(1)
                .build(),
            List.of(new WorkTime(1, List.of(TimeInterval.of(LocalTime.of(10, 0), LocalTime.of(18, 0)))))
        )
            .setInstruction("instruction")
            .setContact(new Person("Иван", "Иванов", "Иванович"));
    }
}
