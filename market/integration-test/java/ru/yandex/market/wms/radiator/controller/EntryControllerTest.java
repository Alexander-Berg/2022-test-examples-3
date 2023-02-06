package ru.yandex.market.wms.radiator.controller;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DatabaseSetups;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import ru.yandex.market.wms.radiator.test.IntegrationTestFrontend;
import ru.yandex.market.wms.radiator.util.StringUtil;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.xmlunit.matchers.CompareMatcher.isIdenticalTo;

class EntryControllerTest extends IntegrationTestFrontend {

    @Test
    @DatabaseSetups({
            @DatabaseSetup(value = "/fixtures/dbStocks/sku-1.xml", connection = "wh1Connection"),
            @DatabaseSetup(value = "/fixtures/dbStocks/sku-1.xml", connection = "wh2Connection"),
            @DatabaseSetup(value = "/fixtures/dbSqlConfig/fallback-enabled.xml", connection = "wh1Connection")
    })
    void testGetStocks_byRange_all() throws Exception {
        String requestXml= StringUtil.resourceAsString("/fixtures/getStocksRequest/all.xml");
        String responseXml = mockMvc.perform(post("/query-gateway")
                .contentType(MediaType.TEXT_XML_VALUE)
                .content(requestXml)).andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String expected = StringUtil.resourceAsString("/fixtures/getStocksResponse/all.xml");
        assertThat(responseXml, isIdenticalTo(expected).normalizeWhitespace());
    }
}
