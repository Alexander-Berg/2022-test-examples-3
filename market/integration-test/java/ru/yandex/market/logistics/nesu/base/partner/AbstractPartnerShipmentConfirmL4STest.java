package ru.yandex.market.logistics.nesu.base.partner;

import java.util.List;

import javax.annotation.Nonnull;

import org.mockito.ArgumentCaptor;

import ru.yandex.market.logistics.nesu.model.L4ShopsFactory;
import ru.yandex.market.logistics.nesu.model.TMFactory;
import ru.yandex.market.logistics4shops.client.model.OutboundConfirmRequest;
import ru.yandex.market.logistics4shops.client.model.OutboundsListDto;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.safeRefEq;

public abstract class AbstractPartnerShipmentConfirmL4STest extends AbstractPartnerShipmentConfirmTest {

    private final ArgumentCaptor<OutboundConfirmRequest> confirmCaptor = ArgumentCaptor.forClass(
        OutboundConfirmRequest.class
    );

    @Override
    protected void mockOutbounds() {
        when(outboundApi.searchOutbounds(safeRefEq(L4ShopsFactory.SEARCH_OUTBOUND_REQUEST)))
            .thenReturn(new OutboundsListDto()
                .outbounds(List.of(L4ShopsFactory.outbound(TMFactory.outboundId(), null, null)))
            );
    }

    @Override
    protected void mockInvalidOutbounds() {
        when(outboundApi.searchOutbounds(safeRefEq(L4ShopsFactory.SEARCH_OUTBOUND_REQUEST)))
            .thenReturn(new OutboundsListDto()
                .outbounds(List.of(L4ShopsFactory.outbound(List.of(Long.toString(INVALID_ORDER_ID)))))
            );
    }

    @Override
    protected void mockEmptyOutbounds() {
        when(outboundApi.searchOutbounds(safeRefEq(L4ShopsFactory.SEARCH_OUTBOUND_REQUEST)))
            .thenReturn(new OutboundsListDto().outbounds(List.of()));
    }

    @Override
    protected void verifyGetOutbound() {
        verify(outboundApi).searchOutbounds(safeRefEq(L4ShopsFactory.SEARCH_OUTBOUND_REQUEST));
    }

    @Override
    protected void verifyConfirmOutbound(String externalId) {
        verify(outboundApi).confirmOutbound(confirmCaptor.capture());
        softly.assertThat(confirmCaptor.getValue())
            .usingRecursiveComparison()
            .isEqualTo(defaultConfirmRequest(externalId));
    }

    @Nonnull
    private OutboundConfirmRequest defaultConfirmRequest(String externalId) {
        return new OutboundConfirmRequest()
            .yandexId(TMFactory.outboundId())
            .externalId(externalId)
            .orderIds(List.of(Long.toString(VALID_ORDER_ID)));
    }
}
