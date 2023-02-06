package ru.yandex.market.partner.mvc.controller.billing;

import java.time.LocalDate;
import java.time.ZoneId;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.util.UriComponentsBuilder;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.core.util.DateTimes;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Тесты для {@link NettingBillingController}
 */
public class NettingBillingControllerTest extends FunctionalTest {
    private static final long CAMPAIGN_ID = 200001L;
    private static final long CAMPAIGN_ID_WITHOUT_BONUS = 300001L;

    @Autowired
    private TestableClock clock;

    @BeforeEach
    void setUp() {
        clock.setFixed(DateTimes.toInstantAtDefaultTz(2021, 7, 13, 12, 0, 0), ZoneId.systemDefault());
    }

    @Test
    @DbUnitDataSet(before = "csv/NettingBillingControllerTest.testResponse.before.csv")
    void testResponse() {
        String url = UriComponentsBuilder.fromUriString(baseUrl + "/netting/billing-info")
                .queryParam("campaign_id", CAMPAIGN_ID)
                .build()
                .toString();
        ResponseEntity<String> stringResponseEntity = FunctionalTestHelper.get(url);
        JsonElement jsonElement = JsonTestUtil.parseJson(stringResponseEntity.getBody());
        JsonObject result = jsonElement.getAsJsonObject().get("result").getAsJsonObject();

        assertEquals(500_00L, result.get("remainBonus").getAsLong());
        assertEquals(Currency.RUR.name(), result.get("currency").getAsString());
        assertEquals(LocalDate.of(2021, 9, 2).toString(), result.get("bonusExpiredAt").getAsString());
        assertEquals(400L, result.get("spentAmount").getAsLong());
        assertEquals(0L, result.get("blockedAmount").getAsLong());
    }

    @Test
    @DbUnitDataSet(before = "csv/NettingBillingControllerTest.testEmptyResponse.before.csv")
    void testEmptyResponse() {
        String url = UriComponentsBuilder.fromUriString(baseUrl + "/netting/billing-info")
                .queryParam("campaign_id", CAMPAIGN_ID_WITHOUT_BONUS)
                .build()
                .toString();
        ResponseEntity<String> stringResponseEntity = FunctionalTestHelper.get(url);
        JsonElement jsonElement = JsonTestUtil.parseJson(stringResponseEntity.getBody());
        JsonObject result = jsonElement.getAsJsonObject().get("result").getAsJsonObject();

        assertNull(result.get("remainBonus"));
        assertEquals(Currency.RUR.name(), result.get("currency").getAsString());
        assertNull(result.get("bonusExpiredAt"));
        assertEquals(0L, result.get("spentAmount").getAsLong());
        assertEquals(0L, result.get("blockedAmount").getAsLong());
    }
}
