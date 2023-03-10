package ru.yandex.market.logistics.nesu.controller.own.delivery;

import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.google.common.collect.Sets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.type.PartnerStatus;
import ru.yandex.market.logistics.nesu.AbstractContextualTest;
import ru.yandex.market.logistics.nesu.dto.tarifficator.SearchTariffsFilter;
import ru.yandex.market.logistics.tarifficator.client.TarifficatorClient;
import ru.yandex.market.logistics.tarifficator.model.dto.TariffSearchFilter;
import ru.yandex.market.logistics.tarifficator.model.enums.TariffType;

import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static ru.yandex.market.logistics.nesu.controller.own.delivery.TestOwnDeliveryUtils.OWN_PARTNER_ID;
import static ru.yandex.market.logistics.nesu.controller.own.delivery.TestOwnDeliveryUtils.TARIFF_ID;
import static ru.yandex.market.logistics.nesu.controller.own.delivery.TestOwnDeliveryUtils.partnerBuilder;
import static ru.yandex.market.logistics.nesu.controller.own.delivery.TestOwnDeliveryUtils.tariffBuilder;
import static ru.yandex.market.logistics.nesu.utils.MatcherUtils.validationErrorMatcher;
import static ru.yandex.market.logistics.nesu.utils.ValidationErrorData.fieldError;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.safeRefEq;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("?????????? ?????????????????? ?????????????? ?????????????????????? ????")
@DatabaseSetup("/repository/own-delivery/own_delivery.xml")
class GetOwnTariffTest extends AbstractContextualTest {

    @Autowired
    private LMSClient lmsClient;
    @Autowired
    private TarifficatorClient tarifficatorClient;

    @Test
    @DisplayName("?????????????????? ???????? ?????????????? ????????????????")
    void getShopTariffs() throws Exception {
        TestOwnDeliveryUtils.mockSearchOwnDeliveries(
            List.of(partnerBuilder().build(), partnerBuilder().id(2L).marketId(1L).build()),
            lmsClient
        );
        TariffSearchFilter tariffFilter = new TariffSearchFilter().setPartnerIds(Set.of(OWN_PARTNER_ID, 2L));
        when(tarifficatorClient.searchTariffs(safeRefEq(tariffFilter)))
            .thenReturn(List.of(
                tariffBuilder().id(TARIFF_ID).type(TariffType.OWN_DELIVERY).build(),
                tariffBuilder()
                    .id(13L)
                    .type(TariffType.OWN_DELIVERY)
                    .name("another tariff")
                    .partnerId(2L)
                    .build()
            ));

        execSearch(SearchTariffsFilter.builder().build())
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/own-tariff/get_shop_tariff_response.json"));
    }

    @Test
    @DisplayName("?????????????????? ???????????? ???????????????? ??????????????")
    void getShopEnabledTariffs() throws Exception {
        TestOwnDeliveryUtils.mockSearchOwnDeliveries(
            List.of(partnerBuilder().build(), partnerBuilder().id(2L).marketId(1L).build()),
            lmsClient
        );
        TariffSearchFilter tariffFilter = new TariffSearchFilter()
            .setPartnerIds(Set.of(OWN_PARTNER_ID, 2L));
        tariffFilter.setEnabled(true);

        when(tarifficatorClient.searchTariffs(safeRefEq(tariffFilter)))
            .thenReturn(List.of(
                tariffBuilder().id(TARIFF_ID).type(TariffType.OWN_DELIVERY).build(),
                tariffBuilder()
                    .id(13L)
                    .type(TariffType.OWN_DELIVERY)
                    .name("another tariff")
                    .partnerId(2L)
                    .build()
            ));

        execSearch(SearchTariffsFilter.builder().enabled(true).build())
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/own-tariff/get_shop_tariff_response.json"));
    }

    @Test
    @DisplayName("?????????????????? ???????? ?????????????? ????????????????, ???? ?????????????? ?????????????????????? ????")
    void getShopTariffsNotFoundDeliveries() throws Exception {
        TestOwnDeliveryUtils.mockSearchOwnDeliveries(List.of(), lmsClient);

        execSearch(SearchTariffsFilter.builder().build())
            .andExpect(status().isOk())
            .andExpect(content().json(EMPTY_ARRAY));

        verifyZeroInteractions(tarifficatorClient);
    }

    @Test
    @DisplayName("?????????????????? ???????? ?????????????? ????????????????")
    void getPartnerTariffs() throws Exception {
        TestOwnDeliveryUtils.mockSearchOwnDeliveries(List.of(partnerBuilder().build()), lmsClient);
        TariffSearchFilter tariffFilter = new TariffSearchFilter().setPartnerIds(Set.of(OWN_PARTNER_ID));
        when(tarifficatorClient.searchTariffs(safeRefEq(tariffFilter)))
            .thenReturn(List.of(tariffBuilder().id(TARIFF_ID).type(TariffType.OWN_DELIVERY).build()));

        execSearch(SearchTariffsFilter.builder().partnerIds(Set.of(OWN_PARTNER_ID)).build())
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/own-tariff/get_partner_tariff_response.json"));
    }

    @Test
    @DisplayName("?????????????????? ?????????????? ???????????? ????????????????")
    void getNotOwnedPartnerTariffs() throws Exception {
        TestOwnDeliveryUtils.mockSearchOwnDeliveries(List.of(partnerBuilder().id(2L).build()), lmsClient);

        execSearch(SearchTariffsFilter.builder().partnerIds(Set.of(OWN_PARTNER_ID)).build())
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [PARTNER] with ids [45]"));

        verifyZeroInteractions(tarifficatorClient);
    }

    @Test
    @DisplayName("?????????????????? ?????????????? ?????????????????????? ????????????????")
    void getInactivePartnerTariffs() throws Exception {
        TestOwnDeliveryUtils
            .mockSearchOwnDeliveries(List.of(partnerBuilder().status(PartnerStatus.INACTIVE).build()), lmsClient);
        TariffSearchFilter tariffFilter = new TariffSearchFilter().setPartnerIds(Set.of(OWN_PARTNER_ID));
        when(tarifficatorClient.searchTariffs(safeRefEq(tariffFilter)))
            .thenReturn(List.of(tariffBuilder().id(TARIFF_ID).type(TariffType.OWN_DELIVERY).build()));

        execSearch(SearchTariffsFilter.builder().partnerIds(Set.of(OWN_PARTNER_ID)).build())
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/own-tariff/get_partner_tariff_response.json"));
    }

    @Test
    @DisplayName("???????????????????? ????????????")
    void getTariffsFilterWithNulls() throws Exception {
        execSearch(SearchTariffsFilter.builder().partnerIds(Sets.newHashSet(1L, null)).build())
            .andExpect(status().isBadRequest())
            .andExpect(validationErrorMatcher(fieldError(
                "partnerIds[]",
                "must not be null",
                "searchTariffsFilter",
                "NotNull"
            )));
    }

    @Nonnull
    private ResultActions execSearch(SearchTariffsFilter request) throws Exception {
        return mockMvc.perform(
            put("/back-office/own-tariff/search")
                .param("userId", "1")
                .param("shopId", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        );
    }
}
