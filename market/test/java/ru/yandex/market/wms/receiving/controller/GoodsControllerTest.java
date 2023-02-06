package ru.yandex.market.wms.receiving.controller;

import java.util.Set;

import javax.servlet.http.Cookie;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import ru.yandex.market.logistics.cte.client.FulfillmentCteClientApi;
import ru.yandex.market.logistics.cte.client.dto.QualityAttributeDTO;
import ru.yandex.market.logistics.cte.client.dto.QualityAttributesResponseDTO;
import ru.yandex.market.logistics.cte.client.enums.QualityAttributeType;
import ru.yandex.market.wms.common.model.enums.AuthenticationParam;
import ru.yandex.market.wms.common.spring.utils.JsonAssertUtils;
import ru.yandex.market.wms.receiving.ReceivingIntegrationTest;
import ru.yandex.market.wms.receiving.service.returns.GoodsService;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.wms.common.spring.utils.FileContentUtils.getFileContent;

@SpyBean(value = {
        FulfillmentCteClientApi.class,
        GoodsService.class
})
public class GoodsControllerTest extends ReceivingIntegrationTest {

    @Autowired
    protected FulfillmentCteClientApi cteClient;
    @Autowired
    private GoodsService goodsService;

    @AfterEach
    public void resetMocks() {
        Mockito.reset(cteClient);
        Mockito.reset(goodsService);
    }

    @Test
    @DatabaseSetup("/controller/goods-controller/before.xml")
    @ExpectedDatabase(value = "/controller/goods-controller/before.xml", assertionMode = NON_STRICT)
    public void getItemQualityAttributes() throws Exception {

        doReturn(new QualityAttributesResponseDTO(
                Set.of(new QualityAttributeDTO(
                        1L,
                        "test attribute",
                        "TA-Title",
                        "TA-refId",
                        QualityAttributeType.ITEM,
                        "TA-Descr"))
        )).when(cteClient)
                .resolveQualityAttributes(eq(null), eq(102L), eq("sku.sku"), any());

        assertApiCallOk(
                "controller/goods-controller/request/empty-request.json",
                "controller/goods-controller/response/quality-attributes-response.json",
                get("/goods/get-quality-attributes")
                        .param("sku", "shopSku2")
                        .param("storerKey", "102")
                        .param("receiptKey", "receipt123")
        );
    }

    @Test
    @DatabaseSetup("/controller/goods-controller/before-empty-category-id.xml")
    @ExpectedDatabase(value = "/controller/goods-controller/before-empty-category-id.xml", assertionMode = NON_STRICT)
    public void getItemQualityAttributesWithEmptyCategoryId() throws Exception {

        doReturn(new QualityAttributesResponseDTO(
                Set.of(new QualityAttributeDTO(
                        1L,
                        "test attribute",
                        "TA-Title",
                        "TA-refId",
                        QualityAttributeType.ITEM,
                        "TA-Descr"))
        )).when(cteClient)
                .resolveQualityAttributes(eq(null), eq(102L), eq("sku.sku"), any());

        assertApiCallOk(
                "controller/goods-controller/request/empty-request.json",
                "controller/goods-controller/response/quality-attributes-response.json",
                get("/goods/get-quality-attributes")
                        .param("sku", "shopSku2")
                        .param("storerKey", "102")
                        .param("receiptKey", "receipt123")
        );
    }

    @Test
    @DatabaseSetup("/controller/goods-controller/before-multiple-items-with-the-same-sku.xml")
    @ExpectedDatabase(value = "/controller/goods-controller/before-multiple-items-with-the-same-sku.xml",
            assertionMode = NON_STRICT)
    public void getItemQualityAttributesWithMultipleItemsOfTheSameSkuInReturnReceipt() throws Exception {

        doReturn(new QualityAttributesResponseDTO(
                Set.of(new QualityAttributeDTO(
                        1L,
                        "test attribute",
                        "TA-Title",
                        "TA-refId",
                        QualityAttributeType.ITEM,
                        "TA-Descr"))
        )).when(cteClient)
                .resolveQualityAttributes(eq(null), eq(102L), eq("sku.sku"), any());

        assertApiCallOk(
                "controller/goods-controller/request/empty-request.json",
                "controller/goods-controller/response/quality-attributes-response.json",
                get("/goods/get-quality-attributes")
                        .param("sku", "shopSku2")
                        .param("storerKey", "102")
                        .param("receiptKey", "receipt123")
        );
    }

    private void assertApiCallOk(String requestFile, String responseFile,
                                 MockHttpServletRequestBuilder request) throws Exception {
        MvcResult mvcResult = mockMvc.perform(request
                        .cookie(new Cookie(AuthenticationParam.USERNAME.getCode(), "TEST"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent(requestFile)))
                .andExpect(status().isOk())
                .andReturn();
        if (responseFile != null) {
            JsonAssertUtils.assertFileNonExtensibleEquals(responseFile,
                    mvcResult.getResponse().getContentAsString());
        }
    }
}
