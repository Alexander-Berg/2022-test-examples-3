package ru.yandex.market.logistics.nesu.base.partner;

import java.util.List;

import ru.yandex.market.logistics.nesu.model.L4ShopsFactory;
import ru.yandex.market.logistics4shops.client.model.Outbound;
import ru.yandex.market.logistics4shops.client.model.OutboundsListDto;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.safeRefEq;

public abstract class AbstractPartnerShipmentGenerateTransportationWaybillL4STest
    extends AbstractPartnerShipmentGenerateTransportationWaybillTest {

    @Override
    protected void verifyGetOutbounds() {
        verify(outboundApi).searchOutbounds(safeRefEq(L4ShopsFactory.SEARCH_OUTBOUND_REQUEST));
    }

    @Override
    protected void mockOutbounds() {
        when(outboundApi.searchOutbounds(safeRefEq(L4ShopsFactory.SEARCH_OUTBOUND_REQUEST)))
            .thenReturn(new OutboundsListDto()
                .outbounds(List.of(L4ShopsFactory.outbound(null)))
            );
    }

    @Override
    protected void mockEmptyOutbounds() {
        when(outboundApi.searchOutbounds(safeRefEq(L4ShopsFactory.SEARCH_OUTBOUND_REQUEST)))
            .thenReturn(new OutboundsListDto().outbounds(List.of()));
    }

    @Override
    protected void mockTransportationUnit() {
        Outbound outbound = L4ShopsFactory.outbound(List.of(ORDER_ID));
        outbound.setExternalId("external-id");
        when(outboundApi.searchOutbounds(safeRefEq(L4ShopsFactory.SEARCH_OUTBOUND_REQUEST)))
            .thenReturn(new OutboundsListDto().outbounds(List.of(outbound)));
    }
}
