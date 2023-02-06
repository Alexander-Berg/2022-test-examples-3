package ru.yandex.market.delivery.transport_manager.client;

import java.time.LocalDateTime;
import java.util.List;

import javax.annotation.Nonnull;

import lombok.experimental.UtilityClass;

import ru.yandex.market.delivery.transport_manager.model.dto.MovementDto;
import ru.yandex.market.delivery.transport_manager.model.dto.RegisterDto;
import ru.yandex.market.delivery.transport_manager.model.dto.TransportationPartnerExtendedInfoDto;
import ru.yandex.market.delivery.transport_manager.model.dto.TransportationUnitDto;
import ru.yandex.market.delivery.transport_manager.model.enums.MovementStatus;
import ru.yandex.market.delivery.transport_manager.model.enums.PartnerType;
import ru.yandex.market.delivery.transport_manager.model.enums.RegisterStatus;
import ru.yandex.market.delivery.transport_manager.model.enums.RegisterType;
import ru.yandex.market.delivery.transport_manager.model.enums.TransportationUnitStatus;
import ru.yandex.market.delivery.transport_manager.model.enums.TransportationUnitType;

@UtilityClass
public class TransportationsFactory {

    @Nonnull
    public TransportationUnitDto newInboundUnit() {
        return getInboundUnitBuilder()
            .registers(List.of(
                RegisterDto.builder()
                    .id(2L)
                    .type(RegisterType.PLAN)
                    .status(RegisterStatus.PREPARING)
                    .externalId("register1")
                    .documentId("abc123")
                    .partnerId(2L)
                    .comment("Очень важный комментарий")
                    .build()
            ))
            .build();
    }

    @Nonnull
    public TransportationUnitDto newInboundUnitWithRegisters() {
        return getInboundUnitBuilder()
            .registers(List.of(RegistersFactory.newRegister()))
            .partner(newPartnerInfo(2L, "Partner 2"))
            .build();
    }

    @Nonnull
    private static TransportationUnitDto.TransportationUnitDtoBuilder getInboundUnitBuilder() {
        return TransportationUnitDto.builder()
            .id(2L)
            .status(TransportationUnitStatus.SENT)
            .type(TransportationUnitType.INBOUND)
            .logisticPointId(2L)
            .plannedIntervalStart(LocalDateTime.of(2021, 3, 2, 12, 0))
            .plannedIntervalEnd(LocalDateTime.of(2021, 3, 3, 14, 0));
    }

    @Nonnull
    public TransportationUnitDto newOutboundUnit(Long partnerId, String partnerName) {
        return TransportationUnitDto.builder()
            .id(1L)
            .status(TransportationUnitStatus.SENT)
            .type(TransportationUnitType.OUTBOUND)
            .logisticPointId(1L)
            .plannedIntervalStart(LocalDateTime.of(2021, 3, 2, 12, 0))
            .plannedIntervalEnd(LocalDateTime.of(2021, 3, 3, 14, 0))
            .registers(List.of())
            .partner(newPartnerInfo(partnerId, partnerName))
            .build();
    }

    @Nonnull
    public MovementDto newMovement(Long partnerId, String partnerName) {
        return MovementDto.builder()
            .id(1L)
            .status(MovementStatus.NEW)
            .weight(10)
            .volume(24)
            .registers(List.of())
            .partner(newPartnerInfo(partnerId, partnerName))
            .plannedIntervalStart(LocalDateTime.of(2021, 3, 2, 12, 0))
            .plannedIntervalEnd(LocalDateTime.of(2021, 3, 3, 14, 0))
            .build();
    }

    @Nonnull
    public TransportationPartnerExtendedInfoDto newPartnerInfo(Long partnerId, String name) {
        return TransportationPartnerExtendedInfoDto.builder()
            .id(partnerId)
            .name(name)
            .type(PartnerType.DELIVERY)
            .build();
    }
}
