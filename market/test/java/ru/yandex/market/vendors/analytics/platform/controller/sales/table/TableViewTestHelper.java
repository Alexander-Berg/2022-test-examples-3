package ru.yandex.market.vendors.analytics.platform.controller.sales.table;

import org.springframework.web.util.UriComponentsBuilder;

import ru.yandex.market.common.test.spring.FunctionalTestHelper;
import ru.yandex.market.vendors.analytics.core.model.dashboard.WidgetType;

/**
 * Класс-хелпер для тестов про табличную визуализацию виджетов.
 *
 * @author ogonek
 */
public class TableViewTestHelper {

    private TableViewTestHelper() {
    }

    public static String getTableView(WidgetType widgetType, String body, String baseUrl) {
        var url = UriComponentsBuilder.fromUriString(baseUrl)
                .path("/calculate/widgets/view/table")
                .queryParam("widgetType", widgetType)
                .queryParam("uid", 1)
                .toUriString();
        return FunctionalTestHelper.postForJson(url, body);
    }
}
