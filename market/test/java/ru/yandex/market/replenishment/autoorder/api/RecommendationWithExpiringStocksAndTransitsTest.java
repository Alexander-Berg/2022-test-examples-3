package ru.yandex.market.replenishment.autoorder.api;

import java.time.LocalDate;

import org.junit.Before;
import org.junit.Test;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.replenishment.autoorder.config.ControllerTest;
import ru.yandex.market.replenishment.autoorder.model.StockCoverType;
import ru.yandex.market.replenishment.autoorder.security.WithMockLogin;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WithMockLogin
public class RecommendationWithExpiringStocksAndTransitsTest extends ControllerTest {

    @Before
    public void setUp() {
        setTestTime(LocalDate.of(2021, 9, 14));
    }

    @Test
    @DbUnitDataSet(before = "RecommendationWithExpiringStocksAndTransitsTest.before.csv")
    public void testGetExpiringStocksBackward() throws Exception {
        testGetExpiringStocks(StockCoverType.BACKWARD);
    }

    @Test
    @DbUnitDataSet(before = "RecommendationWithExpiringStocksAndTransitsTest.pastTransits.before.csv")
    public void testGetExpiringStocksBackwardWithPastTransits() throws Exception {
        testGetExpiringStocksWithPastTransits(StockCoverType.BACKWARD);
    }

    @Test
    @DbUnitDataSet(before = "RecommendationWithExpiringStocksAndTransitsTest.before.csv")
    public void testGetExpiringStocksForward() throws Exception {
        testGetExpiringStocks(StockCoverType.FORWARD);
    }

    @Test
    @DbUnitDataSet(before = "RecommendationWithExpiringStocksAndTransitsTest.pastTransits.before.csv")
    public void testGetExpiringStocksForwardWithPastTransits() throws Exception {
        testGetExpiringStocksWithPastTransits(StockCoverType.FORWARD);
    }

    private void testGetExpiringStocks(StockCoverType stockCoverType) throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/recommendation/1/expiring-stocks" +
                "?demandType=TYPE_1P&stockCoverType=" + stockCoverType))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(66)))

            // Initial day
            .andExpect(jsonPath("$[0].date").value("2021-09-14"))
            .andExpect(jsonPath("$[0].currentStock").value(85))
            .andExpect(jsonPath("$[0].expiredStock").value(0))
            .andExpect(jsonPath("$[0].incomingItems").value(0))

            // First expiry + first transit
            .andExpect(jsonPath("$[2].date").value("2021-09-16"))
            .andExpect(jsonPath("$[2].currentStock").value(70))
            .andExpect(jsonPath("$[2].expiredStock").value(13))
            .andExpect(jsonPath("$[2].incomingItems").value(11))

            // Second transit arrives, first transit added to stock
            .andExpect(jsonPath("$[3].date").value("2021-09-17"))
            .andExpect(jsonPath("$[3].currentStock").value(80))
            .andExpect(jsonPath("$[3].expiredStock").value(0))
            .andExpect(jsonPath("$[3].incomingItems").value(13))

            // Second transit added to stock
            .andExpect(jsonPath("$[4].date").value("2021-09-18"))
            .andExpect(jsonPath("$[4].currentStock").value(92))
            .andExpect(jsonPath("$[4].expiredStock").value(0))
            .andExpect(jsonPath("$[4].incomingItems").value(0))

            // Second expiry
            .andExpect(jsonPath("$[5].date").value("2021-09-19"))
            .andExpect(jsonPath("$[5].currentStock").value(54))
            .andExpect(jsonPath("$[5].expiredStock").value(37))
            .andExpect(jsonPath("$[5].incomingItems").value(0))

            // Transit from the recommendation
            .andExpect(jsonPath("$[17].date").value("2021-10-01"))
            .andExpect(jsonPath("$[17].currentStock").value(42))
            .andExpect(jsonPath("$[17].expiredStock").value(0))
            .andExpect(jsonPath("$[17].incomingItems").value(9))

            // Third (last) expiry
            .andExpect(jsonPath("$[33].date").value("2021-10-17"))
            .andExpect(jsonPath("$[33].currentStock").value(33))
            .andExpect(jsonPath("$[33].expiredStock").value(2))
            .andExpect(jsonPath("$[33].incomingItems").value(0));
    }

    private void testGetExpiringStocksWithPastTransits(StockCoverType stockCoverType) throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/recommendation/1/expiring-stocks" +
                "?demandType=TYPE_1P&stockCoverType=" + stockCoverType))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(66)))

            // Initial day + past stock as transit
            .andExpect(jsonPath("$[0].date").value("2021-09-14"))
            .andExpect(jsonPath("$[0].currentStock").value(85))
            .andExpect(jsonPath("$[0].expiredStock").value(0))
            .andExpect(jsonPath("$[0].incomingItems").value(11))

            // First day, past transits added to stock
            .andExpect(jsonPath("$[1].date").value("2021-09-15"))
            .andExpect(jsonPath("$[1].currentStock").value(95))
            .andExpect(jsonPath("$[1].expiredStock").value(0))
            .andExpect(jsonPath("$[1].incomingItems").value(0))

            // First expiry
            .andExpect(jsonPath("$[2].date").value("2021-09-16"))
            .andExpect(jsonPath("$[2].currentStock").value(81))
            .andExpect(jsonPath("$[2].expiredStock").value(13))
            .andExpect(jsonPath("$[2].incomingItems").value(0))

            // Second transit arrives
            .andExpect(jsonPath("$[3].date").value("2021-09-17"))
            .andExpect(jsonPath("$[3].currentStock").value(80))
            .andExpect(jsonPath("$[3].expiredStock").value(0))
            .andExpect(jsonPath("$[3].incomingItems").value(13))

            // Second transit added to stock
            .andExpect(jsonPath("$[4].date").value("2021-09-18"))
            .andExpect(jsonPath("$[4].currentStock").value(92))
            .andExpect(jsonPath("$[4].expiredStock").value(0))
            .andExpect(jsonPath("$[4].incomingItems").value(0))

            // Second expiry
            .andExpect(jsonPath("$[5].date").value("2021-09-19"))
            .andExpect(jsonPath("$[5].currentStock").value(54))
            .andExpect(jsonPath("$[5].expiredStock").value(37))
            .andExpect(jsonPath("$[5].incomingItems").value(0))

            // Transit from the recommendation
            .andExpect(jsonPath("$[17].date").value("2021-10-01"))
            .andExpect(jsonPath("$[17].currentStock").value(42))
            .andExpect(jsonPath("$[17].expiredStock").value(0))
            .andExpect(jsonPath("$[17].incomingItems").value(9))

            // Third (last) expiry
            .andExpect(jsonPath("$[33].date").value("2021-10-17"))
            .andExpect(jsonPath("$[33].currentStock").value(33))
            .andExpect(jsonPath("$[33].expiredStock").value(2))
            .andExpect(jsonPath("$[33].incomingItems").value(0));
    }

}
