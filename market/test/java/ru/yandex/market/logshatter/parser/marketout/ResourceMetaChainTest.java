package ru.yandex.market.logshatter.parser.marketout;


import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ResourceMetaChainTest {
    @Test
    public void testDetection() throws Exception {
        ResourceMetaChain goodSample1 = ResourceMetaChain.parse("@mobile/ProductSpecsPreview[/]->getProductsInfo");
        assertEquals("@mobile/ProductSpecsPreview", goodSample1.getWidgetName());
        assertEquals("/", goodSample1.getWidgetId());
        assertEquals("getProductsInfo", goodSample1.getResourceName());

        ResourceMetaChain goodSample2 = ResourceMetaChain.parse("@MarketNode/ProductPage[/content]->fetchSkus");
        assertEquals("@MarketNode/ProductPage", goodSample2.getWidgetName());
        assertEquals("/content", goodSample2.getWidgetId());
        assertEquals("fetchSkus", goodSample2.getResourceName());

        ResourceMetaChain badSample1 = ResourceMetaChain.parse("rketNode/ProductPage-fetchSkus");
        assertEquals("-", badSample1.getWidgetName());
        assertEquals("-", badSample1.getWidgetId());
        assertEquals("-", badSample1.getResourceName());
    }
}
