package ru.yandex.market.logistics.nesu.controller.warehouse;

import java.util.List;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.management.entity.request.partnerRelation.PartnerRelationFilter;
import ru.yandex.market.logistics.management.entity.type.ShipmentType;
import ru.yandex.market.logistics.nesu.enums.ShipmentPointType;

import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Интеграционный тест внутреннего контроллера бизнес складов")
@DatabaseSetup({
    "/service/shop/prepare_database.xml",
    "/jobs/consumer/switch_shipment_logistic_point/shop_partner_settings.xml",
    "/jobs/consumer/switch_shipment_logistic_point/logistic_point_availability.xml",
})
class InternalSwitchShipmentLogisticPointTest extends AbstractSwitchShipmentLogisticPointTest {

    @BeforeEach
    void setup() {
        mockGetPartnerRelation();
        mockSearchAvailableShipmentOptions();
        mockSetPartnerRelation();
        mockBannerRemoving();
    }

    @Test
    @DisplayName("Успешное переключение магазина на ближайший к  текущей точке сдаче СЦ")
    void switchToClosestPointSuccess() throws Exception {
        switchToClosestPoint()
            .andExpect(status().isOk())
            .andExpect(jsonContent(
                "controller/internal/switch-shipment-logistic-point/response/switch_to_closest_point_success.json"
            ));

        verifySwitchLogisticPoint();
    }

    @Test
    @DisplayName("Ошибка переключения магазина, не найдено ближайших вариантов отгрузки")
    void switchToClosestPointError() throws Exception {
        mockNotFoundPoint();
        mockNotFoundPartners();
        when(lmsClient.searchPartnerRelation(
            PartnerRelationFilter.newBuilder()
                .fromPartnerId(PARTNER_ID)
                .build()
        )).thenReturn(List.of(createPartnerRelation(ShipmentType.WITHDRAW)));

        switchToClosestPoint()
            .andExpect(status().is4xxClientError())
            .andExpect(jsonContent(
                "controller/internal/switch-shipment-logistic-point/response/not_found.json"
            ));

        verifyLmsScNotFound();
    }

    @Nonnull
    private ResultActions switchToClosestPoint()
        throws Exception {
        return mockMvc.perform(
            post("/internal/business-warehouse/" + PARTNER_ID + "/switch-to-closest-point")
                .param("shopId", String.valueOf(SHOP_ID))
                .param("targetShipmentPointTypes", ShipmentPointType.SORTING_CENTER.name())
        );
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(lmsClient);
    }
}
