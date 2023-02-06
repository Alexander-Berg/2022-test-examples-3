package ru.yandex.market.tsup;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import lombok.experimental.UtilityClass;

import ru.yandex.market.delivery.transport_manager.model.dto.MovementDto;
import ru.yandex.market.delivery.transport_manager.model.dto.PartnerTransportDto;
import ru.yandex.market.delivery.transport_manager.model.dto.TransportationPartnerExtendedInfoDto;
import ru.yandex.market.delivery.transport_manager.model.dto.TransportationSearchDto;
import ru.yandex.market.delivery.transport_manager.model.enums.MovementStatus;
import ru.yandex.market.delivery.transport_manager.model.page.Page;
import ru.yandex.market.logistics.management.entity.request.point.filter.LogisticsPointFilter;
import ru.yandex.market.tsup.service.data_provider.primitive.external.lms.logistic_point.dto.LogisticPointDto;

@UtilityClass
@SuppressWarnings("HideUtilityClassConstructor")
public class TestFactory {

    public static final PartnerTransportDto TRANSPORT = new PartnerTransportDto()
        .setId(1L)
        .setDuration(Duration.ofHours(5))
        .setFromPointId(1L)
        .setToPointId(2L)
        .setPartnerId(3L)
        .setPrice(1000L)
        .setPalletCount(33);

    public static final TransportationPartnerExtendedInfoDto MOVER_INFO = TransportationPartnerExtendedInfoDto.builder()
        .id(3L)
        .name("Mover")
        .inn("inn")
        .ogrn("ogrn")
        .legalAddress("address")
        .marketId(1L)
        .legalName("legalName")
        .legalType("OOO")
        .build();

    public static final ru.yandex.market.delivery.transport_manager.model.dto.MovementCourierDto COURIER_DTO =
        ru.yandex.market.delivery.transport_manager.model.dto.MovementCourierDto.builder()
            .carModel("model")
            .carNumber("123F")
            .externalId("1")
            .name("Jack")
            .surname("Smith")
            .phone("88005553535")
            .build();

    public static final MovementDto MOVEMENT_DTO = MovementDto.builder()
        .partner(MOVER_INFO)
        .transport(TRANSPORT)
        .courier(COURIER_DTO)
        .status(MovementStatus.NEW)
        .plannedIntervalStart(LocalDateTime.of(2021, 1, 1, 0, 0))
        .plannedIntervalEnd(LocalDateTime.of(2021, 1, 1, 1, 0))
        .build();

    public static LogisticPointDto logisticPointDto(Long id) {
        return logisticPointDto(id, null, null, null);
    }

    public static LogisticPointDto logisticPointDto(Long id, String name, String partnerName, Long partnerId) {
        return LogisticPointDto.builder()
            .id(id)
            .name(name)
            .partnerName(partnerName)
            .partnerId(partnerId)
            .phones(Set.of())
            .schedule(Set.of())
            .marketBranded(false)
            .build();
    }

    public static LogisticsPointFilter logisticsPointFilter(Set<Long> ids) {
        return LogisticsPointFilter.newBuilder()
            .ids(ids)
            .build();
    }

    public static Page<TransportationSearchDto> page(List<TransportationSearchDto> dtos) {
        Page<TransportationSearchDto> page = new Page<>();
        page.setData(dtos).setTotalElements(dtos.size());

        return page;
    }
}
