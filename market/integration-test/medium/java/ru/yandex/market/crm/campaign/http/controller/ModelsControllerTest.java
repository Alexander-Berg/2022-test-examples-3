package ru.yandex.market.crm.campaign.http.controller;

import java.nio.charset.StandardCharsets;

import javax.inject.Inject;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MvcResult;

import ru.yandex.market.crm.campaign.test.AbstractControllerMediumTest;
import ru.yandex.market.crm.mapreduce.domain.subscriptions.ModelInfo;
import ru.yandex.market.crm.json.serialization.JsonDeserializer;
import ru.yandex.market.mcrm.http.HttpResponse;
import ru.yandex.market.mcrm.http.ResponseMock;
import ru.yandex.market.mcrm.utils.date.Localization;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.mcrm.http.HttpRequest.get;

/**
 * @author apershukov
 */
public class ModelsControllerTest extends AbstractControllerMediumTest {

    @Inject
    private JsonDeserializer jsonDeserializer;

    private static String money(double value) {
        return String.format(Localization.RUSSIAN, "%,.0f", value);
    }

    @Test
    public void testGetSkuPrices() throws Exception {
        httpEnvironment.when(
                get("http://warehouse-report.vs.market.yandex.net:17051/yandsearch?rgb=BLUE&place=sku_offers&" +
                        "market-sku=100334386839&rids=213&show-models=1&show-urls=&pp=18&pg=18&bsformat=2")
        )
                .then(new HttpResponse(
                        new ResponseMock(IOUtils.toByteArray(getClass().getResourceAsStream("sku.json")))
                ));


        MvcResult result = mockMvc.perform(get("/api/models/{id}", "100334386839")
                .param("color", "BLUE"))
                .andExpect(status().isOk())
                .andDo(print())
                .andReturn();

        MockHttpServletResponse response = result.getResponse();
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        ModelInfo modelInfo = jsonDeserializer.readObject(ModelInfo.class, response.getContentAsString());
        Assertions.assertEquals(money(9_770), modelInfo.getPrice());
        Assertions.assertEquals(money(11_110), modelInfo.getOldPrice());
        Assertions.assertEquals("12", modelInfo.getDiscount());
    }
}
