package ru.yandex.market.health.configs.logshatter.url;


import java.io.IOException;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

public class PageTreeTest {

    private PageTree pageTree;

    private void createMarketPageTree() throws Exception {
        pageTree = PageTree.build(PageTreeTest.class.getResourceAsStream("/pageMatcherTree/market-tree.tsv"));
    }

    @Test
    public void testMatch() throws Exception {
        createMarketPageTree();
        check("market_catalogmodels", "GET", "/catalogmodels.xml?CAT_ID=7080917&hid=7070735&banner=samokat&placement" +
            "=mainru&fmt=half");
        check("market_product-reviews", "POST", "product/12061434/reviews?hid=7070735&track=tabs");
        check(null, "HEAD", "product/12061434/reviews/sfasfas/?hid=7070735&track=tabs");
        check("market_product", "GET", "product/12061434/");
        check("market_product", "GET", "pRoDuCt/12061434/");
        check("market_product", "POST", "product/12061434");
        check(null, "HEAD", "product/");
        check(null, "GET", "product");
        check("market_index", "POST", "/");
        check(null, "GET", "/asfsa");
    }

    @Test
    public void testParamsMatch() throws IOException {
        pageTree = PageTree.build(PageTreeTest.class.getResourceAsStream("/pageMatcherTree/crm-tree-params.tsv"));
        check("crm_main", "GET", "/index.php");
        check("crm_contacts", "GET", "/index.php?module=Contacts&action=index&parentTab=Продажи");
        check("crm_contacts_detail_view", "GET", "/index.php?module=Contacts&offset=1&stamp=1473680769059876000" +
            "&return_module=Contacts&action=DetailView&record=1b91322a-256d-7fe4-2ac7-57d2e78bff2f");
    }

    @Test
    public void testMethodMatch() throws IOException {
        pageTree = PageTree.build(PageTreeTest.class.getResourceAsStream("/pageMatcherTree/checkouter.tsv"));
        check("orders_events", "GET", "/orders/events");
        check("orders_orderId_items.GET", "GET", "/orders/123/items");
        check("orders", "GET", "/orders");
        check("orders", "GET", "/orders/");
        check("orders_orderId", "GET", "/orders/123");
        check("orders_orderId", "GET", "/orders/123/");
        check("orders_orderId_delivery_parcels", "GET", "/orders/123/delivery/parcels");
        check("orders_orderId_delivery_parcels", "GET", "/orders/123/delivery/parcels/");
        check("orders_orderId_delivery_parcels_parcelId", "GET", "/orders/123/delivery/parcels/123");
        check("orders_orderId_delivery_parcels_parcelId", "GET", "/orders/123/delivery/parcels/123/");
    }

    public void check(String pageId, String httpMethod, String url) {
        Page page = pageTree.match(httpMethod, url);
        if (pageId == null) {
            assertNull(page);
            return;
        } else {
            assertNotNull(page);
        }
        assertEquals(pageId, page.getId());
    }
}
