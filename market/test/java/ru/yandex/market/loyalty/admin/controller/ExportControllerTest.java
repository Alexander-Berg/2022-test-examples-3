package ru.yandex.market.loyalty.admin.controller;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.loyalty.admin.report.TestReportData;
import ru.yandex.market.loyalty.admin.test.LoyaltyAdminReportTest;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.model.promo.PromoPurpose;
import ru.yandex.market.loyalty.core.rule.RuleType;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.utils.PromoUtils;
import ru.yandex.market.loyalty.test.TestFor;

import java.io.Reader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.loyalty.core.utils.CommonTestUtils.NEW_LINE_CRNL;

/**
 * @author Ivan Anisimov
 * valter@yandex-team.ru
 * 15.06.17
 */
@TestFor(ExportController.class)
public class ExportControllerTest extends LoyaltyAdminReportTest {
    @Autowired
    private MockMvc mockMvc;
    private Map<String, String> csv;
    @Autowired
    private PromoManager promoManager;

    @Test
    public void spendingCouponHistory() throws Exception {
        TestReportData data = prepareCouponDataForReport(RuleType.SINGLE_USE_COUPON_RULE);

        String result = mockMvc
                .perform(get("/api/export/spendingHistory/" + data.promoId))
                .andDo(log())
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        csv = csvToMap(result);

        checkHeaders();
        checkKey("123144231", "Id заказа");
        checkKey(PromoUtils.DEFAULT_NAME, "Название акции");
        checkKey(OrderStatus.PENDING.name(), "Статус");
        checkKey("120", "Id магазина");
        checkKey("http://shop-url", "URL магазина");
        checkKey("400.00", "Сумма заказа с учетом скидки");
        checkKey("300.00", "Сумма скидки");
    }

    @Test
    public void exportPromoList() throws Exception {
        promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultFixed(BigDecimal.valueOf(100L))
                .setPromoPurpose(PromoPurpose.DEAD_STOCK_SALE));

        String result = mockMvc
                .perform(get("/api/export/promos/"))
                .andDo(log())
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        csv = csvToMap(result);
        checkKey("DEAD_STOCK_SALE", "Назначение промо");

    }

    @Test
    public void generateCoupons() throws Exception {
        final Promo couponPromo = promoManager.createCouponPromo(PromoUtils.Coupon.defaultSingleUse());

        String key = "key";
        String result = mockMvc
                .perform(get("/api/export/" + couponPromo.getId() + "/generateCoupons/" + key))
                .andDo(log())
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        try (Reader reader = new StringReader(result); CSVParser parser = new CSVParser(reader, CSVFormat.EXCEL)) {
            final List<CSVRecord> records = parser.getRecords();
            assertThat(records, hasSize(1));
        }
    }


    private void checkHeaders() {
        assertEquals(new TreeSet<>(
                Arrays.asList("Id заказа", "Название акции", "Код купона", "Статус", "Id магазина", "URL магазина",
                        "Id региона доставки", "" +
                                "Сумма заказа с учетом скидки", "Сумма скидки", "Состав заказа (Id категории)",
                        "Время создания купона/флэш-акции", "" +
                                "Время использования", "Время отмены транзакции", "Время передачи в Баланс", "Регион " +
                                "пользователя"
                )), new TreeSet<>(csv.keySet()));
    }

    private void checkKey(String expected, String key) {
        assertEquals(key, expected, csv.get(key));
    }

    public static Map<String, String> csvToMap(String csv) {
        String[] rows = NEW_LINE_CRNL.split(csv);
        assertEquals(2, rows.length);
        String[] headers = rows[0].split(",");
        String[] data = rows[1].split(",");
        Map<String, String> result = new HashMap<>();
        for (int i = 0; i < headers.length; i++) {
            String header = headers[i];
            String datum;
            if (i < data.length) {
                datum = data[i];
            } else {
                datum = "";
            }
            result.put(header, datum);
        }
        return result;
    }
}
