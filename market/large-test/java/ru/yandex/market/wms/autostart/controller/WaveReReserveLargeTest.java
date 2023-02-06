package ru.yandex.market.wms.autostart.controller;

import java.util.Set;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;

import ru.yandex.market.wms.autostart.utils.TestcontainersConfiguration;
import ru.yandex.market.wms.common.spring.dao.entity.SkuId;
import ru.yandex.market.wms.ordermanagement.client.OrderManagementClient;
import ru.yandex.market.wms.replenishment.client.ReplenishmentClient;
import ru.yandex.market.wms.replenishment.core.dto.stock.SkuStockWithAnyHold;
import ru.yandex.market.wms.replenishment.core.dto.stock.StocksWithHoldsRequest;
import ru.yandex.market.wms.replenishment.core.dto.stock.StocksWithHoldsResponse;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.wms.common.spring.utils.FileContentUtils.getFileContent;

/**
 * Тест на перерезервирование волн
 */
public class WaveReReserveLargeTest extends TestcontainersConfiguration {

    @MockBean
    @Autowired
    private ReplenishmentClient replenishmentClient;

    @MockBean
    @Autowired
    private OrderManagementClient orderManagementClient;

    @BeforeEach
    void resetMocks() {
        Mockito.reset(replenishmentClient, orderManagementClient);
    }

    @Test
    @DatabaseSetup("/testcontainers/controller/waves/db/rereserve/before-reserve-wave.xml")
    @ExpectedDatabase(value = "/testcontainers/controller/waves/db/rereserve/after-reserve-wave.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    public void testReserveWave() throws Exception {
        executeRequest("WAVE-001",
                status().isOk(),
                "testcontainers/controller/waves/response/reserve-successful.json");
    }

    @Test
    @DatabaseSetup("/testcontainers/controller/waves/db/rereserve/immutable-state-reserve-out-of-stock.xml")
    @ExpectedDatabase(
            value = "/testcontainers/controller/waves/db/rereserve/immutable-state-reserve-out-of-stock.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    public void testReserveIfNotStartedItemsNotExists() throws Exception {
        executeRequest("WAVE-001",
                status().isOk(),
                "testcontainers/controller/waves/response/reserve-out-of-stock.json");
    }

    @Test
    @DatabaseSetup("/testcontainers/controller/waves/db/rereserve/before-reserve-wave-not-enough-items-on-stock.xml")
    @ExpectedDatabase(
            value = "/testcontainers/controller/waves/db/rereserve/after-reserve-wave-not-enough-items-on-stock.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    public void testReserveWaveNotEnoughItemsOnStock() throws Exception {
        executeRequest("WAVE-001",
                status().isOk(),
                "testcontainers/controller/waves/response/reserve-not-enough-items-on-stock.json");
    }

    @Test
    @DatabaseSetup(
            "/testcontainers/controller/waves/db/rereserve/before-reserve-wd-wave-not-enough-on-stock.xml"
    )
    @ExpectedDatabase(
            value = "/testcontainers/controller/waves/db/rereserve/after-reserve-wd-wave-not-enough-on-stock.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    public void testReserveWdWaveNotEnoughItemsOnStock() throws Exception {
        SkuId sku = SkuId.of("STORER-001", "SKU-001");
        StocksWithHoldsRequest request = new StocksWithHoldsRequest(Set.of(sku), Set.of()); //OUTBOUND_FIT
        StocksWithHoldsResponse response = new StocksWithHoldsResponse(Set.of(new SkuStockWithAnyHold(sku, 0, 0)));
        Mockito.when(replenishmentClient.getStocks(request)).thenReturn(response);

        executeRequest("WAVE-001",
                status().isOk(),
                "testcontainers/controller/waves/response/reserve-not-enough-items-on-stock-wd.json");
    }


    @Test
    @DatabaseSetup(
            "/testcontainers/controller/waves/db/rereserve/before-reserve-partial-wd-wave-wrong-holds.xml"
    )
    @ExpectedDatabase(
            value = "/testcontainers/controller/waves/db/rereserve/after-reserve-partial-wd-wave-wrong-holds.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    public void testReserveWdWaveNotEnoughItemsOnStockMultipleWrongHolds() throws Exception {
        SkuId sku = SkuId.of("STORER-001", "SKU-001");
        StocksWithHoldsRequest request = new StocksWithHoldsRequest(Set.of(sku), Set.of()); //OUTBOUND_FIT
        StocksWithHoldsResponse response = new StocksWithHoldsResponse(Set.of(new SkuStockWithAnyHold(sku, 2, 6)));
        Mockito.when(replenishmentClient.getStocks(request)).thenReturn(response);

        executeRequest("WAVE-001",
                status().isOk(),
                "testcontainers/controller/waves/response/reserve-partial-wd-wrong-holds.json");
    }

    @Test
    @DatabaseSetup(value = {
            "/testcontainers/controller/waves/db/rereserve/disabled-consolidation/before.xml",
            "/testcontainers/controller/waves/db/immutable-state-stations.xml",
            "/testcontainers/controller/waves/db/immutable-state-nsqlconfig.xml"
    })
    @ExpectedDatabase(value = "/testcontainers/controller/waves/db/rereserve/disabled-consolidation/after.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    public void testReserveWaveWithDisabledConsolidation() throws Exception {
        executeRequest("WAVE-001",
                status().isOk(),
                "testcontainers/controller/waves/response/reserve-successful.json");
    }

    @Test
    @DatabaseSetup(value = {
            "/testcontainers/controller/waves/db/rereserve/immutable-state.xml",
            "/testcontainers/controller/waves/db/rereserve/part-release/before.xml",
            "/testcontainers/controller/waves/db/immutable-state-stations.xml",
    })
    @ExpectedDatabase(value = "/testcontainers/controller/waves/db/rereserve/part-release/after.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    public void testReserveWhenWaveHasPartReleasedPickDetails() throws Exception {
        executeRequest("WAVE-001",
                status().isOk(),
                "testcontainers/controller/waves/response/reserve-successful.json");
    }

    @Test
    @DatabaseSetup(value = {
            "/testcontainers/controller/waves/db/rereserve/immutable-state.xml",
            "/testcontainers/controller/waves/db/rereserve/part-picked/before.xml"
    })
    @ExpectedDatabase(
            value = "/testcontainers/controller/waves/db/rereserve/part-picked/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    public void testReserveWhenWaveHasPartPickedPickDetails() throws Exception {
        executeRequest("WAVE-001",
                status().isOk(),
                "testcontainers/controller/waves/response/reserve-successful.json");
    }

    private void executeRequest(String waveKey, ResultMatcher expectedStatus, String pathWithResult) throws Exception {
        ResultActions result = mockMvc.perform(put(String.format("/waves/reserve/%s", waveKey)));
        result.andExpect(expectedStatus).andExpect(content().json(getFileContent(pathWithResult)));
    }
}
