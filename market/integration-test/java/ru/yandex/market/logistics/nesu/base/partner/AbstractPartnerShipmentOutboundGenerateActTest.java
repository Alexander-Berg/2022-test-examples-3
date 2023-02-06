package ru.yandex.market.logistics.nesu.base.partner;

import java.math.BigDecimal;
import java.util.List;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.nesu.model.L4ShopsFactory;
import ru.yandex.market.logistics.nesu.model.TMFactory;
import ru.yandex.market.logistics.nesu.utils.ValidationErrorData;
import ru.yandex.market.logistics.werewolf.model.entity.RtaOrdersData;
import ru.yandex.market.logistics4shops.client.model.OutboundsListDto;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static ru.yandex.market.logistics.nesu.utils.MatcherUtils.validationErrorMatcher;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.safeRefEq;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;
import static ru.yandex.market.logistics.werewolf.model.enums.DocumentFormat.PDF;

public abstract class AbstractPartnerShipmentOutboundGenerateActTest extends AbstractPartnerShipmentGenerateActTest {
    @Test
    @DisplayName("АПП второго партнёра")
    void secondPartnerShipment() throws Exception {
        doReturn(TMFactory.transportation(
            TMFactory.defaultOutbound()
                .partner(TMFactory.transportationPartner(TMFactory.SECOND_PARTNER_ID, TMFactory.SECOND_PARTNER_NAME))
                .registers(List.of(TMFactory.outboundRegister().build()))
                .build(),
            TMFactory.defaultMovement().build(),
            TMFactory.defaultInbound()
                .partner(TMFactory.transportationPartner(null, "Получатель"))
                .build()
        ))
            .when(transportManagerClient)
            .getTransportation(TMFactory.SHIPMENT_ID);

        mockTransportationUnit();
        mockSearchOrders(createLomOrders(TMFactory.SECOND_PARTNER_ID, BigDecimal.valueOf(100)));

        RtaOrdersData rtaOrdersData = createRtaOrdersData(TMFactory.SECOND_PARTNER_NAME, BigDecimal.valueOf(100));
        try (var ignored = mockGenerateAct(rtaOrdersData)) {
            generateShipmentAct(SHOP_ID)
                .andExpect(status().isOk())
                .andExpect(content().bytes(BYTES));
        }

        verifyCheckouterGetOrders();
        verifyLomGetOrders();
        verifyGetOutbound();
    }

    @Test
    @DisplayName("Отгрузка не найдена")
    void outboundNotFound() throws Exception {
        when(outboundApi.searchOutbounds(safeRefEq(L4ShopsFactory.outboundIds(1))))
            .thenReturn(new OutboundsListDto());

        generateShipmentAct(SHOP_ID)
            .andExpect(status().isBadRequest())
            .andExpect(validationErrorMatcher(ValidationErrorData.objectError(
                "shipment",
                "must be confirmed",
                "ValidShipmentStatus"
            )));
        verifyGetOutbound();
    }

    @Nonnull
    protected AutoCloseable mockGenerateAct(RtaOrdersData data) {
        when(wwClient.generateReceptionTransferAct(data, PDF)).thenReturn(BYTES);
        return () -> verify(wwClient).generateReceptionTransferAct(data, PDF);
    }

    @Test
    @DisplayName("Отгрузка не подтверждена")
    void outboundNotConfirmed() throws Exception {
        when(outboundApi.searchOutbounds(safeRefEq(L4ShopsFactory.outboundIds(1))))
            .thenReturn(new OutboundsListDto().outbounds(List.of(L4ShopsFactory.outbound(null))));

        generateShipmentAct(SHOP_ID)
            .andExpect(status().isBadRequest())
            .andExpect(validationErrorMatcher(ValidationErrorData.objectError(
                "shipment",
                "must be confirmed",
                "ValidShipmentStatus"
            )));
        verifyGetOutbound();
    }
}
