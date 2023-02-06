package ru.yandex.market.logistics.nesu.base.partner;

import java.util.List;
import java.util.stream.Collectors;

import ru.yandex.market.logistics.nesu.model.L4ShopsFactory;
import ru.yandex.market.logistics4shops.client.model.OutboundsListDto;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.safeRefEq;

public abstract class AbstractPartnerShipmentSearchL4STest extends AbstractPartnerShipmentSearchTest {

    @Override
    protected void verifyGetOutbounds(int size) {
        verify(outboundApi).searchOutbounds(safeRefEq(L4ShopsFactory.outboundIds(size)));
    }

    @Override
    protected void mockOutbounds(int size, List<Long> orderIds) {
        when(outboundApi.searchOutbounds(safeRefEq(L4ShopsFactory.outboundIds(size))))
            .thenReturn(new OutboundsListDto().outbounds(
                List.of(L4ShopsFactory.outbound(orderIds.stream().map(Object::toString).collect(Collectors.toList())))
            ));
    }

    @Override
    protected void mockOutbounds(int size, String outboundId, List<Long> orderIds, String confirmed) {
        when(outboundApi.searchOutbounds(safeRefEq(L4ShopsFactory.outboundIds(size))))
            .thenReturn(new OutboundsListDto().outbounds(
                List.of(
                    L4ShopsFactory.outbound(
                        outboundId,
                        orderIds.stream().map(Object::toString).collect(Collectors.toList()),
                        confirmed
                    )
                )
            ));
    }

    @Override
    protected void mockOutbounds(
        int size,
        String outboundId,
        List<Long> orderIds,
        String confirmed,
        String externalId
    ) {
        when(outboundApi.searchOutbounds(safeRefEq(L4ShopsFactory.outboundIds(size))))
            .thenReturn(new OutboundsListDto().outbounds(
                List.of(
                    L4ShopsFactory.outbound(
                        outboundId,
                        orderIds.stream().map(Object::toString).collect(Collectors.toList()),
                        confirmed
                    )
                        .externalId(externalId)
                )
            ));
    }
}
