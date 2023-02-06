package ru.yandex.market.vendors.analytics.platform.controller.billing.management.add;

import java.time.Clock;
import java.util.Map;

import one.util.streamex.EntryStream;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.web.util.UriComponentsBuilder;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.spring.FunctionalTestHelper;
import ru.yandex.market.vendors.analytics.core.model.billing.TariffType;
import ru.yandex.market.vendors.analytics.platform.controller.billing.BalanceFunctionalTest;

/**
 * @author antipov93.
 */
@DbUnitDataSet(before = "../../Balance.common.before.csv")
public class AbstractAddTest extends BalanceFunctionalTest {

    @MockBean(name = "clock")
    protected Clock clock;

    public String executeAddCategoriesRequest(long vendorId, long uid, String body) {
        var url = UriComponentsBuilder.fromHttpUrl(baseUrl())
                .pathSegment("vendors", "{vendorId}", "categories")
                .queryParam("uid", uid)
                .buildAndExpand(vendorId)
                .toUriString();
        return FunctionalTestHelper.postForJson(url, body);
    }

    public static String addCategoriesRequest(Map<Long, TariffType> categories) {
        return EntryStream.of(categories)
                .mapKeyValue((categoryId, tariff) -> "{"
                        + "\"hid\":" + categoryId + ","
                        + "\"tariffType\": \"" + tariff + "\""
                        + "}")
                .joining(",", "{\"categories\":[", "]}");
    }
}
