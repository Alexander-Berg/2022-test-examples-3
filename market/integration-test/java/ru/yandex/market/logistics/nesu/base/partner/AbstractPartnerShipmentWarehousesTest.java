package ru.yandex.market.logistics.nesu.base.partner;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.delivery.transport_manager.model.filter.TransportationSearchFilter;
import ru.yandex.market.logistics.management.entity.request.point.filter.LogisticsPointFilter;
import ru.yandex.market.logistics.nesu.model.TMFactory;
import ru.yandex.market.logistics.nesu.utils.ValidationErrorData;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static ru.yandex.market.logistics.nesu.utils.MatcherUtils.validationErrorMatcher;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

public abstract class AbstractPartnerShipmentWarehousesTest extends AbstractPartnerShipmentTest {

    @Test
    @DisplayName("Не передан ни один из параметров с идентификатором магазина")
    void noShopId() throws Exception {
        ValidationErrorData.ValidationErrorDataBuilder validationError =
            ValidationErrorData.objectErrorBuilder(
                "Must specify either shopId or shopIds",
                "ValidShopIds"
            );
        mockMvc.perform(get(url()).param("userId", "-1"))
            .andExpect(status().isBadRequest())
            .andExpect(validationErrorMatcher(validationError.forObject("shopIdsHolder")));
    }

    @Test
    @DisplayName("Нет настроек партнёра магазина")
    void noShopPartners() throws Exception {
        getShipmentsWarehouses(SECOND_SHOP_ID)
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find dropship partner for shops [20]"));
    }

    @Test
    @DisplayName("Экспресс-отгрузки исключаются")
    void expressPartners() throws Exception {
        long expressPartnerId = 404;
        mockExpressPartner(expressPartnerId);

        mockTransportationSearch(
            TransportationSearchFilter.builder()
                .outboundPartnerIds(Set.of(TMFactory.PARTNER_ID, TMFactory.SECOND_PARTNER_ID))
                .movementExcludePartnerIds(Set.of(expressPartnerId)),
            List.of(
                TMFactory.transportationSearch(
                    TMFactory.defaultOutbound().build(),
                    TMFactory.defaultMovement().build()
                )
            )
        );

        mockWarehouses();

        getShipmentsWarehouses(SHOP_ID)
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/partner/shipment/warehouses/single.json"));
    }

    @Test
    @DisplayName("Много складов отправки и назначения")
    void multiple() throws Exception {
        mockTransportationSearch(
            TransportationSearchFilter.builder()
                .outboundPartnerIds(Set.of(TMFactory.PARTNER_ID, TMFactory.SECOND_PARTNER_ID)),
            List.of(
                TMFactory.transportationSearch(
                    TMFactory.defaultOutbound().logisticPointId(TMFactory.WAREHOUSE_FROM + 1).build(),
                    TMFactory.defaultMovement().build(),
                    TMFactory.defaultInbound().logisticPointId(TMFactory.WAREHOUSE_TO + 3).build()
                ),
                TMFactory.transportationSearch(
                    TMFactory.defaultOutbound().logisticPointId(TMFactory.WAREHOUSE_FROM + 1).build(),
                    TMFactory.defaultMovement().build(),
                    TMFactory.defaultInbound().logisticPointId(TMFactory.WAREHOUSE_TO + 2).build()
                ),
                TMFactory.transportationSearch(
                    TMFactory.defaultOutbound().logisticPointId(TMFactory.WAREHOUSE_FROM + 2).build(),
                    TMFactory.defaultMovement().build(),
                    TMFactory.defaultInbound().logisticPointId(TMFactory.WAREHOUSE_TO + 1).build()
                )
            )
        );

        when(lmsClient.getLogisticsPoints(any()))
            .thenAnswer(i -> i.<LogisticsPointFilter>getArgument(0).getIds().stream()
                .sorted()
                .map(id -> warehouse(id, "Склад " + id, "Адрес"))
                .collect(Collectors.toList())
            );

        getShipmentsWarehouses(SHOP_ID)
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/partner/shipment/warehouses/multiple.json"));
    }

    @Test
    @DisplayName("Передано несколько идентификаторов магазинов")
    void multipleShopIds() throws Exception {
        mockTransportationSearch(
            TransportationSearchFilter.builder()
                .outboundPartnerIds(Set.of(TMFactory.PARTNER_ID, TMFactory.SECOND_PARTNER_ID)),
            List.of(
                TMFactory.transportationSearch(
                    TMFactory.defaultOutbound().build(),
                    TMFactory.defaultMovement().build()
                )
            )
        );

        mockWarehouses();

        mockMvc.perform(
            get(url())
                .param("shopIds", String.valueOf(SHOP_ID), String.valueOf(SECOND_SHOP_ID))
                .param("userId", "-1")
        )
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/partner/shipment/warehouses/single.json"));
    }

    @Nonnull
    private ResultActions getShipmentsWarehouses(long shopId) throws Exception {
        return mockMvc.perform(
            get(url())
                .param("shopId", String.valueOf(shopId))
                .param("userId", "-1")
        );
    }

    @Nonnull
    protected abstract String url();

}
