package ru.yandex.market.logistics.nesu.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import lombok.experimental.UtilityClass;

import ru.yandex.market.logistics.lom.model.dto.CarDto;
import ru.yandex.market.logistics.lom.model.dto.ContactDto;
import ru.yandex.market.logistics.lom.model.dto.CourierDto;
import ru.yandex.market.logistics.lom.model.dto.KorobyteDto;
import ru.yandex.market.logistics.lom.model.dto.ShipmentApplicationDto;
import ru.yandex.market.logistics.lom.model.dto.ShipmentDto;
import ru.yandex.market.logistics.lom.model.dto.TimeIntervalDto;
import ru.yandex.market.logistics.lom.model.enums.CourierType;
import ru.yandex.market.logistics.lom.model.enums.ShipmentApplicationStatus;
import ru.yandex.market.logistics.lom.model.enums.ShipmentType;

@UtilityClass
@ParametersAreNonnullByDefault
public class LomShipmentApplicationFactory {

    @Nonnull
    public ShipmentApplicationDto.ShipmentApplicationDtoBuilder createImportShipmentApplication(@Nullable Long id) {
        return ShipmentApplicationDto.builder()
            .id(id)
            .shipment(ShipmentDto.builder()
                .id(id)
                .marketIdFrom(2L)
                .marketIdTo(7L)
                .partnerIdTo(5L)
                .shipmentType(ShipmentType.IMPORT)
                .shipmentDate(LocalDate.of(2019, 6, 10))
                .warehouseFrom(2L)
                .warehouseTo(1L)
                .fake(false)
                .build()
            )
            .requisiteId("2")
            .externalId(null)
            .interval(new TimeIntervalDto(
                LocalTime.of(12, 0),
                LocalTime.of(13, 0)
            ))
            .status(ShipmentApplicationStatus.NEW)
            .korobyteDto(new KorobyteDto(
                10,
                15,
                40,
                new BigDecimal(5.5)
            ))
            .courier(CourierDto.builder()
                .type(CourierType.COURIER)
                .contact(new ContactDto("lname", "fname", "mname", "+79998887766", null, null, null))
                .car(new CarDto("aa777a", "toyota"))
                .build())
            .cost(BigDecimal.ZERO)
            .comment("comment")
            .balanceContractId(2L)
            .balancePersonId(102L);
    }

    @Nonnull
    public ShipmentApplicationDto.ShipmentApplicationDtoBuilder createWithdrawShipmentApplication(
        @Nullable Long id,
        long partnerIdTo,
        @Nullable Long warehouseTo
    ) {
        return createWithdrawShipmentApplication(id, 100L, partnerIdTo, 1L, warehouseTo);
    }

    @Nonnull
    public ShipmentApplicationDto.ShipmentApplicationDtoBuilder createWithdrawShipmentApplication(
        @Nullable Long id,
        long marketIdFrom,
        long partnerIdTo,
        long warehouseFrom,
        @Nullable Long warehouseTo
    ) {
        return ShipmentApplicationDto.builder()
            .id(id)
            .shipment(ShipmentDto.builder()
                .marketIdFrom(marketIdFrom)
                .marketIdTo(5L)
                .partnerIdTo(partnerIdTo)
                .shipmentType(ShipmentType.WITHDRAW)
                .shipmentDate(LocalDate.of(2019, 6, 12))
                .warehouseFrom(warehouseFrom)
                .warehouseTo(warehouseTo)
                .fake(false)
                .build()
            )
            .requisiteId("1")
            .externalId(null)
            .interval(new TimeIntervalDto(
                LocalTime.of(12, 0),
                LocalTime.of(13, 0)
            ))
            .status(ShipmentApplicationStatus.NEW)
            .korobyteDto(new KorobyteDto(
                10,
                15,
                40,
                new BigDecimal("5.5")
            ))
            .courier(null)
            .cost(new BigDecimal(295))
            .comment(null)
            .balanceContractId(1L)
            .balancePersonId(101L);
    }
}
