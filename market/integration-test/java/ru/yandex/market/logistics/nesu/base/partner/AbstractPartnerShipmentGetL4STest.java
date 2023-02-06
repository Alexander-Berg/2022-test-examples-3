package ru.yandex.market.logistics.nesu.base.partner;

import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import ru.yandex.market.logistics.nesu.model.L4ShopsFactory;
import ru.yandex.market.logistics.nesu.model.TMFactory;
import ru.yandex.market.logistics4shops.client.model.MdsFilePath;
import ru.yandex.market.logistics4shops.client.model.OutboundsListDto;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.safeRefEq;

public abstract class AbstractPartnerShipmentGetL4STest extends AbstractPartnerShipmentGetTest {
    @Override
    protected void mockOutbounds(List<Long> orderIds) {
        when(outboundApi.searchOutbounds(safeRefEq(L4ShopsFactory.SEARCH_OUTBOUND_REQUEST)))
            .thenReturn(new OutboundsListDto().outbounds(
                List.of(L4ShopsFactory.outbound(orderIds.stream().map(Object::toString).collect(Collectors.toList())))
            ));
    }

    @Override
    protected void mockOutbounds(List<Long> orderIds, String externalId) {
        when(outboundApi.searchOutbounds(safeRefEq(L4ShopsFactory.SEARCH_OUTBOUND_REQUEST)))
            .thenReturn(new OutboundsListDto().outbounds(
                List.of(
                    L4ShopsFactory
                        .outbound(orderIds.stream().map(Object::toString).collect(Collectors.toList()))
                        .externalId(externalId))
            ));
    }

    @Override
    protected void mockOutboundsWithDiscrepancyActIsReady(@Nullable MdsFilePath mdsFilePath) {
        when(outboundApi.searchOutbounds(safeRefEq(L4ShopsFactory.SEARCH_OUTBOUND_REQUEST)))
            .thenReturn(new OutboundsListDto().outbounds(
                List.of(
                    L4ShopsFactory
                        .outbound(TMFactory.outboundId(), null, null)
                        .discrepancyActIsReady(true)
                        .discrepancyActPath(mdsFilePath)
                )
            ));
    }

    @Override
    protected void verifyGetOutbounds() {
        verify(outboundApi).searchOutbounds(safeRefEq(L4ShopsFactory.SEARCH_OUTBOUND_REQUEST));
    }

    @Override
    protected void mockNonConfirmedOutbound() {
        when(outboundApi.searchOutbounds(safeRefEq(L4ShopsFactory.SEARCH_OUTBOUND_REQUEST)))
            .thenReturn(new OutboundsListDto().outbounds(
                List.of(L4ShopsFactory.outbound(TMFactory.outboundId(), null, null))
            ));
    }
}
