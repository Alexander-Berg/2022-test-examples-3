package ru.yandex.market.logistics.management.controller.businessWarehouse;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.entity.request.marketId.MarketIdDto;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static ru.yandex.market.logistics.management.util.TestUtil.pathToJson;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Проставление marketId бизнес-складу, если marketId не заполнен")
@DatabaseSetup({
    "/data/controller/businessWarehouse/prepare.xml",
    "/data/controller/businessWarehouse/prepare_market_id_setting.xml",
})
public class BusinessWarehouseSetMarketIdIfNotExistTest extends AbstractContextualTest {
    @Test
    @DisplayName("Успех")
    @ExpectedDatabase(
        value = "/data/controller/businessWarehouse/after_market_id_update.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void success() throws Exception {
        performUpdate(1L, 200L).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Успех - у партнера уже проставлен переданный marketId")
    @ExpectedDatabase(
        value = "/data/controller/businessWarehouse/after_market_id_in_warehouse_update.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void successMarketIdAlreadySet() throws Exception {
        performUpdate(3L, 300L).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Партнер не найден")
    @ExpectedDatabase(
        value = "/data/controller/businessWarehouse/prepare_market_id_setting.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void failNoPartner() throws Exception {
        performUpdate(200L, 300L)
            .andExpect(status().isNotFound())
            .andExpect(status().reason("Can't find Partner with id=200"));
    }

    @Test
    @DisplayName("У партнера больше одного склада")
    @ExpectedDatabase(
        value = "/data/controller/businessWarehouse/prepare_market_id_setting.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void failMoreThanOneWarehouses() throws Exception {
        performUpdate(4L, 300L)
            .andExpect(status().isBadRequest())
            .andExpect(status().reason("Partner 4 has more than one active warehouse"));
    }

    @Test
    @DisplayName("Смена существующего маркет-ид")
    @ExpectedDatabase(
        value = "/data/controller/businessWarehouse/after_market_id_reupdate.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void changeMarketId() throws Exception {
        performUpdate(3L, 500L)
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("У партнера нет складов")
    @ExpectedDatabase(
        value = "/data/controller/businessWarehouse/prepare_market_id_setting.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void failNoWarehouses() throws Exception {
        performUpdate(5L, 300L)
            .andExpect(status().isNotFound())
            .andExpect(status().reason("Can not find warehouse for partner 5"));
    }

    @Test
    @DisplayName("Неправильный тип партнера")
    @ExpectedDatabase(
        value = "/data/controller/businessWarehouse/prepare_market_id_setting.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void failInvalidPartnerType() throws Exception {
        performUpdate(6L, 300L)
            .andExpect(status().isNotFound())
            .andExpect(status().reason("Can't find Partner with id=6"));
    }

    @Test
    @DisplayName("Не указан marketId")
    @ExpectedDatabase(
        value = "/data/controller/businessWarehouse/prepare_market_id_setting.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void noMarketId() throws Exception {
        performUpdate(6L, null)
            .andExpect(status().isBadRequest())
            .andExpect(content().json(pathToJson("data/controller/businessWarehouse/response/no_market_id.json")));
    }

    @Nonnull
    private ResultActions performUpdate(Long partnerId, Long marketId) throws Exception {
        return mockMvc.perform(MockMvcRequestBuilders.put(
            "/externalApi/business-warehouse/{partnerId}/market-id",
            partnerId
        )
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(MarketIdDto.of(marketId))));
    }
}
