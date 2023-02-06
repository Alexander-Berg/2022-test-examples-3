package ru.yandex.market.logistics.nesu.base.partner;

import java.util.List;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.delivery.transport_manager.model.dto.PartialIdDto;
import ru.yandex.market.delivery.transport_manager.model.dto.RegisterUnitDto;
import ru.yandex.market.delivery.transport_manager.model.enums.IdType;
import ru.yandex.market.delivery.transport_manager.model.enums.RegisterType;
import ru.yandex.market.delivery.transport_manager.model.enums.UnitType;
import ru.yandex.market.delivery.transport_manager.model.filter.RegisterUnitSearchFilter;
import ru.yandex.market.delivery.transport_manager.model.page.Page;
import ru.yandex.market.delivery.transport_manager.model.page.PageRequest;
import ru.yandex.market.logistics.nesu.model.TMFactory;
import ru.yandex.market.logistics.werewolf.model.entity.RtaOrdersData;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;
import static ru.yandex.market.logistics.werewolf.model.enums.DocumentFormat.PDF;

public abstract class AbstractPartnerShipmentInboundGenerateActTest extends AbstractPartnerShipmentGenerateActTest {

    @Test
    @DisplayName("Нет фактического реестра")
    void noInboundFactRegiter() throws Exception {
        when(transportManagerClient.getTransportation(TMFactory.SHIPMENT_ID))
            .thenReturn(TMFactory.transportation(
                TMFactory.defaultOutbound()
                    .externalId("external-id")
                    .registers(List.of(TMFactory.outboundRegister().build()))
                    .build(),
                TMFactory.defaultMovement().build(),
                TMFactory.defaultInbound()
                    .id(INBOUND_ID)
                    .externalId("external-id")
                    .partner(TMFactory.transportationPartner(null, "Получатель"))
                    .registers(List.of(TMFactory.inboundRegister().type(RegisterType.PLAN).build()))
                    .build()
            ));

        generateShipmentAct(SHOP_ID)
            .andExpect(status().isBadRequest())
            .andExpect(errorMessage("Transportation unit with id=300 has no 'FACT' register"));
    }

    @Test
    @DisplayName("В фактическом реестре нет заказов")
    void outboundNotFound() throws Exception {
        when(transportManagerClient.searchRegisterUnits(
            createRegisterUnitSearchFilter(),
            new PageRequest(0, Integer.MAX_VALUE)
        )).thenReturn(new Page<RegisterUnitDto>().setData(List.of()));

        generateShipmentAct(SHOP_ID)
            .andExpect(status().isBadRequest())
            .andExpect(errorMessage("Register must contains at least one order"));
    }

    @Override
    protected void mockTransportationUnit() {
        when(transportManagerClient.searchRegisterUnits(
            createRegisterUnitSearchFilter(),
            new PageRequest(0, Integer.MAX_VALUE)
        )).thenReturn(createResponseFromTm());
    }

    @Nonnull
    private Page<RegisterUnitDto> createResponseFromTm() {
        return new Page<RegisterUnitDto>().setData(List.of(
                RegisterUnitDto.builder()
                    .partialIds(List.of(
                        PartialIdDto.builder()
                            .idType(IdType.ORDER_ID)
                            .value(String.valueOf(ORDER_ID))
                            .build()
                    ))
                    .build()
            )
        );
    }

    @Nonnull
    protected AutoCloseable mockGenerateAct(RtaOrdersData data) {
        when(wwClient.generateAcceptanceAct(data, PDF)).thenReturn(BYTES);
        return () -> verify(wwClient).generateAcceptanceAct(data, PDF);
    }

    @Nonnull
    private RegisterUnitSearchFilter createRegisterUnitSearchFilter() {
        return RegisterUnitSearchFilter.builder()
            .registerId(INBOUND_FACT_REGISTER_ID)
            .unitType(UnitType.BOX)
            .build();
    }
}
