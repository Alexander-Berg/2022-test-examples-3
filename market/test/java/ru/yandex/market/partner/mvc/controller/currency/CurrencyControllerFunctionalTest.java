package ru.yandex.market.partner.mvc.controller.currency;

import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.util.UriComponentsBuilder;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.core.balance.BalanceService;
import ru.yandex.market.core.balance.model.ClientInfo;
import ru.yandex.market.common.balance.model.ClientType;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

import static org.mockito.Mockito.when;

/**
 * Функциональные тесты для {@link CurrencyController}.
 */
@DbUnitDataSet(before = "currencyControllerFunctionalTest.before.csv")
public class CurrencyControllerFunctionalTest extends FunctionalTest {

    private static final String DEFAULT_RATES = "{\n" +
            "  \"ueCurrencyRates\": [\n" +
            "    {\n" +
            "      \"currency\": \"RUR\",\n" +
            "      \"rate\": \"30\",\n" +
            "      \"cnt\": \"1\"\n" +
            "    },\n" +
            "    {\n" +
            "      \"currency\": \"BYN\",\n" +
            "      \"rate\": \"0.8\",\n" +
            "      \"cnt\": \"1\"\n" +
            "    },\n" +
            "    {\n" +
            "      \"currency\": \"KZT\",\n" +
            "      \"rate\": \"105\",\n" +
            "      \"cnt\": \"1\"\n" +
            "    }\n" +
            "  ]\n" +
            "}";

    @Mock
    @Autowired
    private BalanceService balanceService;

    @Test
    void testNotAgency() {
        when(balanceService.getClient(1000)).thenReturn(new ClientInfo(1000L, ClientType.OOO, false, -1L));
        ResponseEntity<String> response = FunctionalTestHelper.get(getUrl(1248L, 774L));
        JsonTestUtil.assertEquals(response, DEFAULT_RATES);
    }

    @Test
    void testAgency() {
        when(balanceService.getClient(1000)).thenReturn(new ClientInfo(1000L, ClientType.OOO, true, -1L));
        ResponseEntity<String> response = FunctionalTestHelper.get(getUrl(1248L, 774L));
        JsonTestUtil.assertEquals(response, DEFAULT_RATES);
    }


    private String getUrl(long euid, long datasourceId) {
        return UriComponentsBuilder.fromUriString(baseUrl + "/currencyRate")
                .queryParam("euid", euid)
                .queryParam("datasource_id", datasourceId)
                .toUriString();
    }
}
