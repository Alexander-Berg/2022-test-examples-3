package ru.yandex.market.logistics.logistics4shops.factory;

import java.time.LocalDateTime;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import lombok.experimental.UtilityClass;

import ru.yandex.market.delivery.transport_manager.model.dto.PartialIdDto;
import ru.yandex.market.delivery.transport_manager.model.dto.RegisterDto;
import ru.yandex.market.delivery.transport_manager.model.dto.RegisterUnitDto;
import ru.yandex.market.delivery.transport_manager.model.dto.TransportationDto;
import ru.yandex.market.delivery.transport_manager.model.dto.TransportationPartnerExtendedInfoDto;
import ru.yandex.market.delivery.transport_manager.model.dto.TransportationUnitDto;
import ru.yandex.market.delivery.transport_manager.model.enums.IdType;
import ru.yandex.market.delivery.transport_manager.model.enums.RegisterType;
import ru.yandex.market.delivery.transport_manager.model.enums.UnitType;
import ru.yandex.market.delivery.transport_manager.model.filter.RegisterUnitSearchFilter;

@UtilityClass
public class TmFactory {
    public static final long ORDER_ID_0 = 100L;
    public static final long ORDER_ID_1 = 101L;
    public static final long ORDER_ID_2 = 102L;
    public static final long INVALID_ORDER_ID = 111L;
    public static final String OUTBOUND_YANDEX_ID = "TMU2";

    private static final long PLAN_REGISTER_ID = 1001L;
    private static final long FACT_REGISTER_ID = 1002L;

    @Nonnull
    public RegisterUnitDto registerUnitDto() {
        final List<PartialIdDto> partialIdDtos = List.of(
            PartialIdDto.builder().idType(IdType.BOX_ID).value(String.valueOf(INVALID_ORDER_ID)).build(),
            PartialIdDto.builder().idType(IdType.ORDER_ID).value(String.valueOf(ORDER_ID_0)).build(),
            PartialIdDto.builder().idType(IdType.ORDER_ID).value(String.valueOf(ORDER_ID_1)).build(),
            PartialIdDto.builder().idType(IdType.ORDER_ID).value(String.valueOf(ORDER_ID_2)).build()
        );
        return RegisterUnitDto.builder().partialIds(partialIdDtos).build();
    }

    @Nonnull
    public RegisterUnitSearchFilter itemSearchFilter() {
        return RegisterUnitSearchFilter.builder()
            .registerId(PLAN_REGISTER_ID)
            .unitType(UnitType.ITEM)
            .build();
    }

    @Nonnull
    public TransportationDto transportation() {
        return transportation(null);
    }

    @Nonnull
    public TransportationDto transportation(@Nullable Long partnerId) {
        TransportationDto transportationDto = new TransportationDto();
        List<RegisterDto> registers = List.of(
            RegisterDto.builder().type(RegisterType.FACT).id(FACT_REGISTER_ID).build(),
            RegisterDto.builder().type(RegisterType.PLAN).id(PLAN_REGISTER_ID).build()
        );

        TransportationUnitDto transportationUnitDto = TransportationUnitDto.builder()
            .partner(TransportationPartnerExtendedInfoDto.builder().id(partnerId).build())
            .yandexId(OUTBOUND_YANDEX_ID)
            .registers(registers)
            .logisticPointId(1L)
            .plannedIntervalEnd(LocalDateTime.of(2021, 12, 16, 15, 0))
            .build();
        transportationDto.setOutbound(transportationUnitDto);
        return transportationDto;
    }
}
