package ru.yandex.market.vendors.analytics.platform.controller.sales.table;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.web.util.UriComponentsBuilder;

import ru.yandex.market.common.test.spring.FunctionalTestHelper;
import ru.yandex.market.vendors.analytics.platform.FunctionalTest;
import ru.yandex.market.vendors.analytics.core.utils.json.JsonUnitUtils;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author antipov93.
 */
public class WidgetTableViewControllerTest extends FunctionalTest {

    @Test
    void getSupportedWidgetTypes() {
        var url = UriComponentsBuilder.fromUriString(baseUrl())
                .path("/widgets/view/table/types")
                .toUriString();
        ResponseEntity<String> response = FunctionalTestHelper.get(url);
        assertNotNull(response.getBody());

        var expected = loadFromFile("WidgetTableViewControllerTest.getSupportedTypes.json");
        JsonUnitUtils.assertJsonEqualsIgnoreArrayOrder(expected, response.getBody());
    }
}
