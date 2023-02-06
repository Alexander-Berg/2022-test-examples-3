package ru.yandex.market.wms.receiving.controller;

import java.nio.charset.Charset;

import javax.servlet.http.Cookie;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import ru.yandex.market.wms.common.model.enums.AuthenticationParam;
import ru.yandex.market.wms.common.spring.utils.JsonAssertUtils;
import ru.yandex.market.wms.receiving.ReceivingIntegrationTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.wms.common.spring.utils.FileContentUtils.getFileContent;

@DatabaseSetup(value = "/service/lost-item-restore/db.xml", connection = "wmwhseConnection")
@DatabaseSetup(value = "/service/lost-item-restore/archive.xml", connection = "archiveWmwhseConnection")
class LostItemControllerTest extends ReceivingIntegrationTest {

    @Test
    void getInfo() throws Exception {
        assertApiCallOk(
                "get-info/ok-request.json",
                "get-info/ok-response.json",
                post("/lostitem/get-info")
        );
    }

    @Test
    void getInfoError() throws Exception {
        assertApiCallError(
                "get-info/error-request.json",
                "get-info/error-response.json",
                post("/lostitem/get-info")
        );
    }

    @Test
    void getInfoNotFound() throws Exception {
        assertApiCallNotFound("get-info/notfound-request.json", post("/lostitem/get-info"));
    }

    @Test
    @ExpectedDatabase(value = "/service/lost-item-restore/after-writtenoff-restore-db.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void restore() throws Exception {
        assertApiCallOk("restore/request.json", null, post("/lostitem/restore"));
    }

    @Test
    void restoreWrongNotEmptyCart() throws Exception {
        assertApiCallError("restore/request-wrong-cart.json",
                "restore/response-wrong-cart.json",
                post("/lostitem/restore"));
    }

    @Test
    void restoreExpiredTest() throws Exception {
        assertApiCallError(
                "restore/request-expired.json",
                "restore/response-expired.json",
                post("/lostitem/restore")
        );
    }


    private void assertApiCallNotFound(String requestFile, MockHttpServletRequestBuilder request) throws Exception {
        assertApiCall(requestFile, null, request, status().isNotFound(), null);
    }

    private void assertApiCallOk(String requestFile, String responseFile,
                                 MockHttpServletRequestBuilder request) throws Exception {
        assertApiCall(requestFile, responseFile, request, status().isOk(), JSONCompareMode.STRICT);
    }

    private void assertApiCallError(String requestFile, String responseFile,
                                    MockHttpServletRequestBuilder request) throws Exception {
        assertApiCall(requestFile, responseFile, request, status().isBadRequest(), JSONCompareMode.STRICT);
    }

    private void assertApiCall(String requestFile, String responseFile,
                               MockHttpServletRequestBuilder request, ResultMatcher status,
                               JSONCompareMode mode) throws Exception {
        String path = "controller/lost-item/";
        MvcResult mvcResult = mockMvc.perform(request
                .cookie(new Cookie(AuthenticationParam.USERNAME.getCode(), "TEST"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent(path + requestFile)))
                .andExpect(status)
                .andReturn();
        if (responseFile != null) {
            JsonAssertUtils.assertFileEquals(
                    path + responseFile,
                    mvcResult.getResponse().getContentAsString(Charset.defaultCharset()),
                    mode
            );
        }
    }

}
