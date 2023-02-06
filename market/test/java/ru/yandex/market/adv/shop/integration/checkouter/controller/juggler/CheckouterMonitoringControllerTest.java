package ru.yandex.market.adv.shop.integration.checkouter.controller.juggler;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;

import ru.yandex.market.adv.shop.integration.AbstractShopIntegrationTest;
import ru.yandex.market.adv.shop.integration.checkouter.yt.entity.click.MarketOrderItemClick;
import ru.yandex.market.adv.yt.test.annotation.YtUnitDataSet;
import ru.yandex.market.adv.yt.test.annotation.YtUnitScheme;
import ru.yandex.market.common.test.db.DbUnitDataSet;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Date: 24.06.2022
 * Project: adv-shop-integration
 *
 * @author alexminakov
 */
class CheckouterMonitoringControllerTest extends AbstractShopIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @DbUnitDataSet(
            before = "CheckouterMonitoringController/csv/" +
                    "checkOrderLinkStatus_empty_ok.csv"
    )
    @DisplayName("Мониторинг на то, что job связи заказов и кликов работает исправно, если еще не было запусков. " +
            "Вернули OK.")
    @Test
    void checkOrderLinkStatus_empty_ok() {
        check("link/status", "checkOrderLinkStatus_empty_ok", status().isOk());
    }

    @DbUnitDataSet(
            before = "CheckouterMonitoringController/csv/" +
                    "checkOrderLinkStatus_existed_ok.csv"
    )
    @DisplayName("Мониторинг на то, что job связи заказов и кликов работает исправно, если она уже запускалась. " +
            "Вернули OK.")
    @Test
    void checkOrderLinkStatus_existed_ok() {
        check("link/status", "checkOrderLinkStatus_existed_ok", status().isOk());
    }

    @DbUnitDataSet(
            before = "CheckouterMonitoringController/csv/" +
                    "checkOrderLinkStatus_oldVendorTable_warn.csv"
    )
    @DisplayName("Мониторинг на то, что job связи заказов и кликов зависла более чем на 6 часов из-за кликов. " +
            "Вернули ОШИБКУ.")
    @Test
    void checkOrderLinkStatus_oldVendorTable_warn() {
        check("link/status", "checkOrderLinkStatus_oldVendorTable_warn", status().isExpectationFailed());
    }

    @DbUnitDataSet(
            before = "CheckouterMonitoringController/csv/" +
                    "checkOrderLinkStatus_oldOrderClick_warn.csv"
    )
    @DisplayName("Мониторинг на то, что job связи заказов и кликов зависла более чем на 6 часов из-за заказов. " +
            "Вернули ОШИБКУ.")
    @Test
    void checkOrderLinkStatus_oldOrderClick_warn() {
        check("link/status", "checkOrderLinkStatus_oldOrderClick_warn", status().isExpectationFailed());
    }

    @DbUnitDataSet(
            before = "CheckouterMonitoringController/csv/" +
                    "checkOrderLinkStatus_oldBoth_warn.csv"
    )
    @DisplayName("Мониторинг на то, что job связи заказов и кликов зависла более чем на 6 часов. " +
            "Вернули ОШИБКУ.")
    @Test
    void checkOrderLinkStatus_oldBoth_warn() {
        check("link/status", "checkOrderLinkStatus_oldBoth_warn", status().isExpectationFailed());
    }

    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = MarketOrderItemClick.class,
                    path = "//tmp/checkUnreadOrder_empty_ok_market_order_item_click"
            ),
            before = "CheckouterMonitoringController/json/yt/MarketOrderItemClick/" +
                    "checkUnreadOrder_empty_ok.json"
    )
    @DisplayName("Мониторинг на то, что количество непрочитанных изменений ставок меньше заданного значения, " +
            "вернул OK, если изначально таблица была пустой.")
    @Test
    void checkUnreadOrder_empty_ok() {
        run("checkUnreadOrder_empty_ok_", () ->
                check("unread/status", "checkUnreadOrder_empty_ok", status().isOk())
        );
    }

    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = MarketOrderItemClick.class,
                    path = "//tmp/checkUnreadOrder_existed_ok_market_order_item_click"
            ),
            before = "CheckouterMonitoringController/json/yt/MarketOrderItemClick/" +
                    "checkUnreadOrder_existed_ok.json"
    )
    @DisplayName("Мониторинг на то, что количество непрочитанных изменений ставок меньше заданного значения, " +
            "вернул OK, если изначально были записи.")
    @Test
    void checkUnreadOrder_existed_ok() {
        run("checkUnreadOrder_existed_ok_", () ->
                check("unread/status", "checkUnreadOrder_existed_ok", status().isOk())
        );
    }

    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = MarketOrderItemClick.class,
                    path = "//tmp/checkUnreadOrder_moreThreshold_warn_market_order_item_click"
            ),
            before = "CheckouterMonitoringController/json/yt/MarketOrderItemClick/" +
                    "checkUnreadOrder_moreThreshold_warn.json"
    )
    @DisplayName("Мониторинг на то, что количество непрочитанных изменений ставок больше заданного значения, " +
            "вернуло ОШИБКУ.")
    @Test
    void checkUnreadOrder_moreThreshold_warn() {
        run("checkUnreadOrder_moreThreshold_warn_", () ->
                check("unread/status", "checkUnreadOrder_moreThreshold_warn", status().isExpectationFailed())
        );
    }

    private void check(String url, String methodName, ResultMatcher resultMatcher) {
        try {
            mvc.perform(
                            get("/juggler/job/order/" + url)
                                    .contentType(MediaType.TEXT_PLAIN_VALUE)
                    )
                    .andExpect(resultMatcher)
                    .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN_VALUE))
                    .andExpect(content().string(
                                    loadFile("CheckouterMonitoringController/txt/response/" + methodName + ".txt")
                                            .trim()
                            )
                    );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
