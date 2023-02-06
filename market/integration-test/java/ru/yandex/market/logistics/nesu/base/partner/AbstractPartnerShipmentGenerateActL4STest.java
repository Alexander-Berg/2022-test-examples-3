package ru.yandex.market.logistics.nesu.base.partner;

import java.util.List;

import ru.yandex.market.logistics.nesu.model.L4ShopsFactory;
import ru.yandex.market.logistics4shops.client.model.Outbound;
import ru.yandex.market.logistics4shops.client.model.OutboundsListDto;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.safeRefEq;

public abstract class AbstractPartnerShipmentGenerateActL4STest
    extends AbstractPartnerShipmentOutboundGenerateActTest {
    @Override
    protected void verifyGetOutbound() {
        verify(outboundApi).searchOutbounds(safeRefEq(L4ShopsFactory.SEARCH_OUTBOUND_REQUEST));
    }

    @Override
    protected void mockTransportationUnit() {
        Outbound outbound = L4ShopsFactory.outbound(List.of(ORDER_ID));
        outbound.setExternalId("external-id");
        when(outboundApi.searchOutbounds(safeRefEq(L4ShopsFactory.SEARCH_OUTBOUND_REQUEST)))
            .thenReturn(new OutboundsListDto().outbounds(List.of(outbound)));
    }
}
