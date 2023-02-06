package ru.yandex.market.replenishment.autoorder.api;

import org.junit.Test;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.replenishment.autoorder.config.ControllerTest;
import ru.yandex.market.replenishment.autoorder.security.WithMockLogin;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WithMockLogin
public class MskuStatisticControllerTest extends ControllerTest {

    @Test
    @DbUnitDataSet(before = "MskuStatisticControllerTest.before.csv")
    public void testGetMskuStatistic() throws Exception {
        mockMvc.perform(get("/api/v1/msku/123/daily-statistic?warehouseId=171&dateFrom=2019-07-10&dateTo=2019-08-03"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(5))
            .andExpect(jsonPath("$.msku").value(123L))
            .andExpect(jsonPath("$.title").value("кларнет"))

            .andExpect(jsonPath("$.statistics.length()").value(24))

            .andExpect(jsonPath("$.statistics[0].date").value("2019-07-10"))
            .andExpect(jsonPath("$.statistics[0].stocks1P").value(0L))
            .andExpect(jsonPath("$.statistics[0].stocks3P").value(3L))
            .andExpect(jsonPath("$.statistics[0].sales1P").value(1L))
            .andExpect(jsonPath("$.statistics[0].sales3P").value(6L))
            .andExpect(jsonPath("$.statistics[0].missedOrder").value(0.0))
            .andExpect(jsonPath("$.statistics[0].forecast").value(38.82))

            .andExpect(jsonPath("$.statistics[5].date").value("2019-07-15"))
            .andExpect(jsonPath("$.statistics[5].stocks1P").value(0L))
            .andExpect(jsonPath("$.statistics[5].stocks3P").value(3L))
            .andExpect(jsonPath("$.statistics[5].sales1P").value(2L))
            .andExpect(jsonPath("$.statistics[5].sales3P").value(4L))
            .andExpect(jsonPath("$.statistics[5].missedOrder").value(2.2))
            .andExpect(jsonPath("$.statistics[5].forecast").value(5.63))

            .andExpect(jsonPath("$.statistics[10].date").value("2019-07-20"))
            .andExpect(jsonPath("$.statistics[10].stocks1P").value(0L))
            .andExpect(jsonPath("$.statistics[10].stocks3P").value(0L))
            .andExpect(jsonPath("$.statistics[10].sales1P").value(0L))
            .andExpect(jsonPath("$.statistics[10].sales3P").value(0L))
            .andExpect(jsonPath("$.statistics[10].missedOrder").value(0.0))
            .andExpect(jsonPath("$.statistics[10].forecast").value(14.18))

            .andExpect(jsonPath("$.statistics[23].date").value("2019-08-02"))
            .andExpect(jsonPath("$.statistics[23].stocks1P").value(0L))
            .andExpect(jsonPath("$.statistics[23].stocks3P").value(3L))
            .andExpect(jsonPath("$.statistics[23].sales1P").value(0L))
            .andExpect(jsonPath("$.statistics[23].sales3P").value(0L))
            .andExpect(jsonPath("$.statistics[23].missedOrder").value(0.0))
            .andExpect(jsonPath("$.statistics[23].forecast").value(33.34))

            .andExpect(jsonPath("$.promoPurchase.length()").value(1))
            .andExpect(jsonPath("$.promoPurchase.[0].start").value("2019-07-10"))
            .andExpect(jsonPath("$.promoPurchase.[0].end").value("2019-08-03"))

            .andExpect(jsonPath("$.promoSales.length()").value(2))
            .andExpect(jsonPath("$.promoSales.[0].start").value("2019-07-10"))
            .andExpect(jsonPath("$.promoSales.[0].end").value("2019-07-15"))
            .andExpect(jsonPath("$.promoSales.[1].start").value("2019-07-17"))
            .andExpect(jsonPath("$.promoSales.[1].end").value("2019-08-03"));

    }

    @Test
    @DbUnitDataSet(before = "MskuStatisticControllerTest.before.csv")
    public void testGetMskuStatisticPromo() throws Exception {
        mockMvc.perform(get("/api/v1/msku/123/daily-statistic?warehouseId=171&dateFrom=2019-05-10&dateTo=2019-08-02"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(5))
            .andExpect(jsonPath("$.msku").value(123L))
            .andExpect(jsonPath("$.title").value("кларнет"))
            .andExpect(jsonPath("$.statistics").exists())

            .andExpect(jsonPath("$.promoPurchase.length()").value(1))
            .andExpect(jsonPath("$.promoPurchase.[0].start").value("2019-06-02"))
            .andExpect(jsonPath("$.promoPurchase.[0].end").value("2019-08-02"))

            .andExpect(jsonPath("$.promoSales.length()").value(2))
            .andExpect(jsonPath("$.promoSales.[0].start").value("2019-06-02"))
            .andExpect(jsonPath("$.promoSales.[0].end").value("2019-07-15"))
            .andExpect(jsonPath("$.promoSales.[1].start").value("2019-07-17"))
            .andExpect(jsonPath("$.promoSales.[1].end").value("2019-08-02"));
    }

    @Test
    @DbUnitDataSet(before = "MskuStatisticControllerTest.before.csv")
    public void testGetMskuStatisticPromoWithoutDate() throws Exception {
        mockMvc.perform(get("/api/v1/msku/123/daily-statistic?warehouseId=171"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(5))
            .andExpect(jsonPath("$.msku").value(123L))
            .andExpect(jsonPath("$.title").value("кларнет"))
            .andExpect(jsonPath("$.statistics").exists())
            .andExpect(jsonPath("$.promoPurchase").isEmpty())
            .andExpect(jsonPath("$.promoSales").isEmpty());
    }

    @Test
    public void testGetMskuStatistic_WithoutWarehouse() throws Exception {
        mockMvc.perform(get("/api/v1/msku/123/daily-statistic"))
            .andExpect(status().isBadRequest());
    }
}
