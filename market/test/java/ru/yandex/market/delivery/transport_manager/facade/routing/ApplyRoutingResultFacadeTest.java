package ru.yandex.market.delivery.transport_manager.facade.routing;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ru.yandex.market.delivery.transport_manager.converter.prefix.IdPrefixConverter;
import ru.yandex.market.delivery.transport_manager.domain.entity.Transportation;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationUnit;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationUnitType;
import ru.yandex.market.delivery.transport_manager.service.TransportationService;

class ApplyRoutingResultFacadeTest {

    @Test
    void allUnique() {
        ApplyRoutingResultFacade routingResultFacade = new ApplyRoutingResultFacade(
            null, null, null, null, null, null
        );
        Assertions.assertTrue(routingResultFacade.allUnique(List.of()));
        Assertions.assertTrue(routingResultFacade.allUnique(List.of(1, 2, 3)));
        Assertions.assertFalse(routingResultFacade.allUnique(List.of(1, 2, 3, 1)));
    }

    @Test
    void getUnitIndexes() {
        TransportationService transportationService = Mockito.mock(TransportationService.class);
        ApplyRoutingResultFacade routingResultFacade = new ApplyRoutingResultFacade(
            null,
            transportationService,
            null,
            null,
            null,
            new IdPrefixConverter()
        );
        List<Long> outboundUnitOrderedIds = List.of(2L, 1L, 3L);

        TransportationUnit outbound1 = new TransportationUnit().setId(1L).setType(TransportationUnitType.OUTBOUND);
        TransportationUnit inbound1 = new TransportationUnit().setId(11L).setType(TransportationUnitType.INBOUND);
        TransportationUnit outbound2 = new TransportationUnit().setId(2L).setType(TransportationUnitType.OUTBOUND);
        TransportationUnit inbound2 = new TransportationUnit().setId(22L).setType(TransportationUnitType.INBOUND);
        TransportationUnit outbound3 = new TransportationUnit().setId(3L).setType(TransportationUnitType.OUTBOUND);
        TransportationUnit inbound3 = new TransportationUnit().setId(33L).setType(TransportationUnitType.INBOUND);

        Mockito
            .when(transportationService.getByUnitIds(Mockito.eq(outboundUnitOrderedIds)))
            .thenReturn(Set.of(
                new Transportation()
                    .setOutboundUnit(outbound1)
                    .setInboundUnit(inbound1),
                new Transportation()
                    .setOutboundUnit(outbound2)
                    .setInboundUnit(inbound2),
                new Transportation()
                    .setOutboundUnit(outbound3)
                    .setInboundUnit(inbound3)
            ));

        Map<Integer, TransportationUnit> unitIndexes = routingResultFacade.getUnitIndexes(outboundUnitOrderedIds);

        Assertions.assertEquals(
            Map.of(
                0, outbound2,
                1, outbound1,
                2, outbound3,
                3, inbound3,
                4, inbound1,
                5, inbound2
            ),
            unitIndexes
        );
    }
}
