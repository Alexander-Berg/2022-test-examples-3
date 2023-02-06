package ru.yandex.market.delivery.transport_manager.facade.transportation;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.model.dto.MovementDto;
import ru.yandex.market.delivery.transport_manager.model.dto.TransportationCreationDto;
import ru.yandex.market.delivery.transport_manager.model.dto.TransportationDto;
import ru.yandex.market.delivery.transport_manager.model.dto.TransportationPartnerExtendedInfoDto;
import ru.yandex.market.delivery.transport_manager.model.dto.TransportationUnitDto;
import ru.yandex.market.delivery.transport_manager.model.enums.MovementStatus;
import ru.yandex.market.delivery.transport_manager.model.enums.TransportationType;
import ru.yandex.market.delivery.transport_manager.model.enums.TransportationUnitStatus;
import ru.yandex.market.delivery.transport_manager.model.enums.TransportationUnitType;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.request.partner.PartnerTransportFilter;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerTransportDto;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;

public class TransportationCreationFacadeTest extends AbstractContextualTest {
    @Autowired
    private TransportationCreationFacade transportationCreationFacade;

    @Autowired
    private LMSClient lmsClient;

    private final TransportationCreationDto dto =
        new TransportationCreationDto()
            .setPartnerFromId(1L)
            .setPartnerToId(2L)
            .setPointFromId(11L)
            .setPointToId(22L)
            .setTransportId(6L)
            .setTransportationType(TransportationType.XDOC_TRANSPORT)
            .setOutboundStart(LocalDateTime.of(2021, 10, 21, 6, 54))
            .setOutboundEnd(LocalDateTime.of(2021, 10, 21, 7, 0));

    @BeforeEach
    void init() {
        Mockito.when(lmsClient.getLogisticsPoint(11L)).thenReturn(Optional.of(
            LogisticsPointResponse.newBuilder().partnerId(1L).id(11L).build()
        ));
        Mockito.when(lmsClient.getLogisticsPoint(22L)).thenReturn(Optional.of(
            LogisticsPointResponse.newBuilder().partnerId(2L).id(22L).build()
        ));
    }

    @Test
    @ExpectedDatabase(
        value = "/repository/transportation/after_external_transportation_creation.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testCreate() {
        Mockito.when(lmsClient.getPartnerTransport(
            PartnerTransportFilter.builder().ids(Set.of(6L)).build()
        )).thenReturn(List.of(
            PartnerTransportDto.newBuilder().id(1L)
                .logisticsPointTo(LogisticsPointResponse.newBuilder().id(22L).build())
                .logisticsPointFrom(LogisticsPointResponse.newBuilder().id(11L).build())
                .partner(PartnerResponse.newBuilder().id(10L).build())
                .duration(Duration.ofHours(1L))
                .price(1000L)
                .palletCount(10)
                .build()
        ));

        TransportationDto created = transportationCreationFacade.create(dto);

        TransportationDto expected = new TransportationDto()
            .setId(1L)
            .setOutbound(TransportationUnitDto.builder()
                .id(2L)
                .yandexId("TMU2")
                .transportationId(1L)
                .logisticPointId(11L)
                .type(TransportationUnitType.OUTBOUND)
                .status(TransportationUnitStatus.NEW)
                .partner(TransportationPartnerExtendedInfoDto.builder().id(1L).build())
                .plannedIntervalStart(LocalDateTime.of(2021, 10, 21, 6, 54))
                .plannedIntervalEnd(LocalDateTime.of(2021, 10, 21, 7, 0))
                .registers(List.of())
                .build()
            )
            .setInbound(TransportationUnitDto.builder()
                .id(1L)
                .yandexId("TMU1")
                .transportationId(1L)
                .logisticPointId(22L)
                .registers(List.of())
                .type(TransportationUnitType.INBOUND)
                .status(TransportationUnitStatus.NEW)
                .partner(TransportationPartnerExtendedInfoDto.builder().id(2L).build())
                .plannedIntervalStart(LocalDateTime.of(2021, 10, 21, 6, 54))
                .plannedIntervalEnd(LocalDateTime.of(2021, 10, 21, 7, 0))
                .build()
            )
            .setMovement(MovementDto.builder()
                .id(1L)
                .partner(TransportationPartnerExtendedInfoDto.builder().build())
                .volume(0)
                .weight(0)
                .registers(List.of())
                .status(MovementStatus.DRAFT)
                .plannedIntervalStart(LocalDateTime.of(2021, 10, 21, 6, 54))
                .plannedIntervalEnd(LocalDateTime.of(2021, 10, 21, 7, 0))
                .build()
            )
            .setTags(List.of());

        softly.assertThat(created).isEqualTo(expected);
    }

    @Test
    void validationFailedNoTransport() {
        softly.assertThatThrownBy(() -> transportationCreationFacade.create(dto));
    }

}
