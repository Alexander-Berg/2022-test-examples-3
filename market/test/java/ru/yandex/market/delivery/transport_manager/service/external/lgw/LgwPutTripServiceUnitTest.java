package ru.yandex.market.delivery.transport_manager.service.external.lgw;

import java.util.List;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ru.yandex.market.delivery.transport_manager.domain.dto.PartnerMarketKey;
import ru.yandex.market.delivery.transport_manager.domain.entity.Address;
import ru.yandex.market.delivery.transport_manager.domain.entity.LogisticsPointMetadata;
import ru.yandex.market.delivery.transport_manager.domain.entity.Movement;
import ru.yandex.market.delivery.transport_manager.domain.entity.Transportation;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationLegalInfo;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationMetadata;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationUnit;
import ru.yandex.market.delivery.transport_manager.domain.enums.TransportationType;
import ru.yandex.market.delivery.transport_manager.provider.map.VolumeStrategyProvider;
import ru.yandex.market.delivery.transport_manager.provider.service.transportation.volume.VolumeStrategy;
import ru.yandex.market.delivery.transport_manager.repository.mappers.TransportationMetadataMapper;
import ru.yandex.market.delivery.transport_manager.service.LegalInfoService;
import ru.yandex.market.delivery.transport_manager.service.checker.PartnerMethodsCheckService;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class LgwPutTripServiceUnitTest {

    private VolumeStrategyProvider volumeStrategyProvider;
    private LegalInfoService legalInfoService;
    private LgwPutTripService lgwPutTripService;
    private TransportationMetadataMapper metadataMapper;
    private PartnerMethodsCheckService partnerMethodsCheckService;

    @BeforeEach
    void setUp() {
        legalInfoService = mock(LegalInfoService.class);
        metadataMapper = Mockito.mock(TransportationMetadataMapper.class);
        volumeStrategyProvider = mock(VolumeStrategyProvider.class);
        lgwPutTripService = new LgwPutTripService(
            null,
            null,
            null,
            null,
            legalInfoService,
            metadataMapper,
            volumeStrategyProvider,
            null,
            null,
            null
        );
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(legalInfoService, metadataMapper, volumeStrategyProvider);
    }

    @Test
    void getMovingPartnerId() {
        Assertions.assertThat(lgwPutTripService.getMovingPartnerId(
                2L,
                List.of(
                    new Transportation().setId(201L).setMovement(new Movement().setPartnerId(10000L)),
                    new Transportation().setId(202L).setMovement(new Movement().setPartnerId(10000L)),
                    new Transportation().setId(202L).setMovement(new Movement().setPartnerId(null))
                )
            ))
            .isEqualTo(10000L);
    }

    @Test
    void getMultipleMovingPartnerIds() {
        Assertions.assertThatThrownBy(() -> lgwPutTripService.getMovingPartnerId(
                2L,
                List.of(
                    new Transportation().setId(201L).setMovement(new Movement().setPartnerId(10000L)),
                    new Transportation().setId(202L).setMovement(new Movement().setPartnerId(10001L)),
                    new Transportation().setId(202L).setMovement(new Movement().setPartnerId(null))
                )
            ))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Multiple moving partners for trip 2: 10000,10001");
    }

    @Test
    void getMovingPartnerIdIsMissed() {
        Assertions.assertThatThrownBy(() -> lgwPutTripService.getMovingPartnerId(
                2L,
                List.of(
                    new Transportation().setId(201L).setMovement(new Movement().setPartnerId(null)),
                    new Transportation().setId(202L).setMovement(new Movement().setPartnerId(null)),
                    new Transportation().setId(202L).setMovement(new Movement().setPartnerId(null))
                )
            ))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Can't detect moving partner for trip 2");
    }

    @Test
    void getLegalInfoByPointId() {
        List<Transportation> transportations = List.of(
            new Transportation()
                .setId(1L)
                .setOutboundUnit(new TransportationUnit().setPartnerId(1L).setLogisticPointId(10L))
                .setInboundUnit(new TransportationUnit().setPartnerId(2L).setLogisticPointId(20L)),
            new Transportation()
                .setId(2L)
                .setOutboundUnit(new TransportationUnit().setPartnerId(1L).setLogisticPointId(10L))
                .setInboundUnit(new TransportationUnit().setPartnerId(3L).setLogisticPointId(30L)),
            new Transportation()
                .setId(3L)
                .setOutboundUnit(new TransportationUnit().setPartnerId(1L).setLogisticPointId(10L))
                .setInboundUnit(new TransportationUnit().setPartnerId(4L).setLogisticPointId(40L))
        );
        TransportationLegalInfo legalInfo1 = new TransportationLegalInfo().setPartnerId(1L).setTransportationId(1L);
        TransportationLegalInfo legalInfo2 = new TransportationLegalInfo().setPartnerId(2L).setTransportationId(1L);
        TransportationLegalInfo legalInfo3 = new TransportationLegalInfo().setPartnerId(1L).setTransportationId(2L);
        TransportationLegalInfo legalInfo4 = new TransportationLegalInfo().setPartnerId(3L).setTransportationId(2L);
        TransportationLegalInfo legalInfo5 = new TransportationLegalInfo().setPartnerId(1L).setTransportationId(3L);
        TransportationLegalInfo moverLegalInfo1 =
            new TransportationLegalInfo().setPartnerId(1000L).setTransportationId(1L);
        TransportationLegalInfo moverLegalInfo2 =
            new TransportationLegalInfo().setPartnerId(1000L).setTransportationId(2L);
        TransportationLegalInfo moverLegalInfo3 =
            new TransportationLegalInfo().setPartnerId(1000L).setTransportationId(3L);
        when(legalInfoService.find(eq(transportations)))
            .thenReturn(Map.of(
                new PartnerMarketKey(1L, 1L, null),
                legalInfo1,
                new PartnerMarketKey(1L, 2L, null),
                legalInfo2,
                new PartnerMarketKey(2L, 1L, null),
                legalInfo3,
                new PartnerMarketKey(2L, 3L, null),
                legalInfo4,
                new PartnerMarketKey(3L, 1L, null),
                legalInfo5,
                new PartnerMarketKey(1L, 1000L, null),
                moverLegalInfo1,
                new PartnerMarketKey(2L, 1000L, null),
                moverLegalInfo2,
                new PartnerMarketKey(3L, 1000L, null),
                moverLegalInfo3
            ));

        Assertions.assertThat(
            lgwPutTripService.getLegalInfoByPointId(transportations)
        ).isEqualTo(Map.of(
            10L, legalInfo5,
            20L, legalInfo2,
            30L, legalInfo4
        ));

        verify(legalInfoService).find(eq(transportations));
    }

    @Test
    void getLogisticsPointMetadataByPointId() {
        List<Transportation> transportations = List.of(
            new Transportation()
                .setId(1L)
                .setOutboundUnit(new TransportationUnit().setId(1L).setPartnerId(1L).setLogisticPointId(10L))
                .setInboundUnit(new TransportationUnit().setId(2L).setPartnerId(2L).setLogisticPointId(20L)),
            new Transportation()
                .setId(2L)
                .setOutboundUnit(new TransportationUnit().setId(3L).setPartnerId(1L).setLogisticPointId(10L))
                .setInboundUnit(new TransportationUnit().setId(4L).setPartnerId(3L).setLogisticPointId(30L)),
            new Transportation()
                .setId(3L)
                .setOutboundUnit(new TransportationUnit().setId(5L).setPartnerId(1L).setLogisticPointId(10L))
                .setInboundUnit(new TransportationUnit().setId(6L).setPartnerId(4L).setLogisticPointId(40L))
        );

        LogisticsPointMetadata metadata1 =
            new LogisticsPointMetadata().setLogisticsPointId(10L).setTransportationUnitId(1L);
        LogisticsPointMetadata metadata2 =
            new LogisticsPointMetadata().setLogisticsPointId(20L).setTransportationUnitId(2L);
        LogisticsPointMetadata metadata3 =
            new LogisticsPointMetadata().setLogisticsPointId(10L).setTransportationUnitId(3L);
        // Заведомо неверный ID
        LogisticsPointMetadata metadata4 =
            new LogisticsPointMetadata().setLogisticsPointId(500L).setTransportationUnitId(4L);
        when(metadataMapper.getLogisticsPointsForUnits(eq(List.of(1L, 2L, 3L, 4L, 5L, 6L))))
            .thenReturn(List.of(
                metadata1,
                metadata2,
                metadata3,
                metadata4
            ));

        Assertions.assertThat(lgwPutTripService.getLogisticsPointMetadataByPointId(transportations))
            .isEqualTo(Map.of(
                10L, metadata3,
                20L, metadata2,
                500L, metadata4
            ));

        verify(metadataMapper).getLogisticsPointsForUnits(eq(List.of(1L, 2L, 3L, 4L, 5L, 6L)));
    }

    @Test
    void getAddressesByPointId() {
        List<Transportation> transportations = List.of(
            new Transportation()
                .setId(1L)
                .setOutboundUnit(new TransportationUnit().setId(1L).setPartnerId(1L).setLogisticPointId(10L))
                .setInboundUnit(new TransportationUnit().setId(2L).setPartnerId(2L).setLogisticPointId(20L)),
            new Transportation()
                .setId(2L)
                .setOutboundUnit(new TransportationUnit().setId(3L).setPartnerId(1L).setLogisticPointId(10L))
                .setInboundUnit(new TransportationUnit().setId(4L).setPartnerId(3L).setLogisticPointId(30L)),
            new Transportation()
                .setId(3L)
                .setOutboundUnit(new TransportationUnit().setId(5L).setPartnerId(1L).setLogisticPointId(10L))
                .setInboundUnit(new TransportationUnit().setId(6L).setPartnerId(4L).setLogisticPointId(40L))
        );

        Address address1 = new Address().setId(2L).setAddressString("ул. Козявкина, 10");
        Address address2 = new Address().setId(3L).setAddressString("ул. Монгольских космонавтов, 2");
        TransportationMetadata metadata1 = new TransportationMetadata()
            .setAddressFrom(new Address().setId(1L).setAddressString("ул. Безымянная"))
            .setAddressTo(address2);
        TransportationMetadata metadata2 = new TransportationMetadata()
            .setAddressFrom(address1);
        when(metadataMapper.get(eq(List.of(1L, 2L, 3L))))
            .thenReturn(Map.of(
                1L, metadata1,
                2L, metadata2
            ));

        Assertions.assertThat(lgwPutTripService.getAddressesByPointId(transportations))
            .isEqualTo(Map.of(
                10L, address1,
                20L, address2
            ));

        verify(metadataMapper).get(eq(List.of(1L, 2L, 3L)));
    }

    @Test
    void getVolumeByTransportationId() {
        Transportation t1 = new Transportation()
            .setId(1L)
            .setTransportationType(TransportationType.XDOC_TRANSPORT);
        Transportation t2 = new Transportation()
            .setId(2L)
            .setTransportationType(TransportationType.INTERWAREHOUSE);
        Transportation t3 = new Transportation()
            .setId(3L)
            .setTransportationType(TransportationType.INTERWAREHOUSE_VIRTUAL);

        List<Transportation> transportations = List.of(t1, t2, t3);

        VolumeStrategy volumeStrategy1 = mock(VolumeStrategy.class);
        VolumeStrategy volumeStrategy2 = mock(VolumeStrategy.class);
        VolumeStrategy volumeStrategy3 = mock(VolumeStrategy.class);

        when(volumeStrategy1.calculateVolume(eq(t1))).thenReturn(1);
        when(volumeStrategy2.calculateVolume(eq(t2))).thenReturn(null);
        when(volumeStrategy3.calculateVolume(eq(t3))).thenReturn(0);

        when(volumeStrategyProvider.provide(eq(TransportationType.XDOC_TRANSPORT))).thenReturn(volumeStrategy1);
        when(volumeStrategyProvider.provide(eq(TransportationType.INTERWAREHOUSE))).thenReturn(volumeStrategy2);
        when(volumeStrategyProvider.provide(eq(TransportationType.INTERWAREHOUSE_VIRTUAL))).thenReturn(volumeStrategy3);

        Assertions.assertThat(lgwPutTripService.getVolumeByTransportationId(transportations))
            .isEqualTo(Map.of(
                1L, 1,
                3L, 0
            ));

        verify(volumeStrategyProvider).provide(eq(TransportationType.XDOC_TRANSPORT));
        verify(volumeStrategyProvider).provide(eq(TransportationType.INTERWAREHOUSE));
        verify(volumeStrategyProvider).provide(eq(TransportationType.INTERWAREHOUSE_VIRTUAL));
    }
}
