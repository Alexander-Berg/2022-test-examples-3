package ru.yandex.market.wms.api.controller;

import java.nio.charset.StandardCharsets;

import javax.servlet.http.Cookie;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import ru.yandex.market.wms.common.model.enums.AuthenticationParam;
import ru.yandex.market.wms.common.spring.IntegrationTest;
import ru.yandex.market.wms.common.spring.utils.FileContentUtils;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.wms.common.spring.utils.FileContentUtils.getFileContent;

public class PrinterControllerTest extends IntegrationTest {

    @Test
    @ExpectedDatabase(value = "/printer/after-insert.xml", assertionMode = NON_STRICT)
    public void addPrinter() throws Exception {
        assertApiCallOk(null, get("/printers/addPrinter")
                .param("name", "name")
                .param("labelSize", "1")
                .param("type", "1"));
    }

    @Test
    @ExpectedDatabase(value = "/printer/after-insert.xml", assertionMode = NON_STRICT)
    public void addPrinter_duplicate_resultSame() throws Exception {
        MockHttpServletRequestBuilder requestBuilder = get("/printers/addPrinter")
                .param("name", "name")
                .param("labelSize", "1")
                .param("type", "1");
        assertApiCallOk(null, requestBuilder);
        assertApiCallOk(null, requestBuilder);
    }

    @Test
    public void addPrinter_dbException_keyIsTooLong() throws Exception {
        jdbc = null;
        assertApiCallError(null, get("/printers/addPrinter")
                        .param("name", StringUtils.repeat('a', 46))
                        .param("labelSize", "1")
                        .param("type", "1"),
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Value too long for column");
    }

    @Test
    @ExpectedDatabase(value = "/printer/after-insert.xml", assertionMode = NON_STRICT)
    public void addPrinterPost() throws Exception {
        assertApiCallOk("printer/add-ok.json",
                post("/printers/addPrinter"));
    }

    @Test
    @ExpectedDatabase(value = "/printer/after-insert.xml", assertionMode = NON_STRICT)
    public void addPrinterDuplicateResultSamePost() throws Exception {
        MockHttpServletRequestBuilder requestBuilder = post("/printers/addPrinter");
        assertApiCallOk("printer/add-ok.json", requestBuilder);
        assertApiCallOk("printer/add-ok.json", requestBuilder);
    }

    @Test
    public void addPrinterDbExceptionKeyIsTooLongPost() throws Exception {
        jdbc = null;
        assertApiCallError("printer/add-error-long-value.json",
                post("/printers/addPrinter"),
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Value too long for column");
    }

    @Test
    @DatabaseSetup("/printer/before-delete.xml")
    @ExpectedDatabase(value = "/printer/after-delete.xml", assertionMode = NON_STRICT)
    public void deletePrinterOk() throws Exception {
        assertApiCallOk("printer/delete-ok.json",
                post("/printers/deletePrinter"));
    }

    @Test
    @DatabaseSetup("/printer/before-delete.xml")
    @ExpectedDatabase(value = "/printer/before-delete.xml", assertionMode = NON_STRICT)
    public void deletePrinterOkButNotExists() throws Exception {
        assertApiCallOk("printer/delete-ok-not-exists.json",
                post("/printers/deletePrinter"));
    }

    @Test
    @DatabaseSetup("/printer/before-delete.xml")
    @ExpectedDatabase(value = "/printer/before-delete.xml", assertionMode = NON_STRICT)
    public void deletePrinterBlankName() throws Exception {
        assertApiCallError("printer/delete-error-empty.json",
                post("/printers/deletePrinter"),
                HttpStatus.BAD_REQUEST,
                "Field error in object 'deletePrinterRequest' on field 'name': rejected value []");
    }

    @Test
    @DatabaseSetup("/printer/getting/several/before.xml")
    public void getSeveralPrinters() throws Exception {
        String responseJsonPath = "printer/getting/several/response.json";
        mockMvc.perform(get("/printers/getPrinters"))
                .andExpect(status().isOk())
                .andExpect(content().json(FileContentUtils.getFileContent(responseJsonPath)));
    }

    @Test
    @DatabaseSetup("/printer/getting/empty/before.xml")
    public void getPrintersEmptyList() throws Exception {
        String responseJsonPath = "printer/getting/empty/response.json";
        mockMvc.perform(get("/printers/getPrinters"))
                .andExpect(status().isOk())
                .andExpect(content().json(FileContentUtils.getFileContent(responseJsonPath)));
    }

    private void assertApiCallOk(String requestFile,
                                 MockHttpServletRequestBuilder request) throws Exception {
        assertApiCall(requestFile, request, HttpStatus.OK, null);
    }

    private void assertApiCallError(String requestFile,
                                    MockHttpServletRequestBuilder request,
                                    HttpStatus status,
                                    String errorDescription) throws Exception {
        assertApiCall(requestFile, request, status, errorDescription);
    }

    private void assertApiCall(String requestFile,
                               MockHttpServletRequestBuilder request,
                               HttpStatus status,
                               String errorDescription) throws Exception {
        if (null != requestFile) {
            request.contentType(MediaType.APPLICATION_JSON)
                    .content(getFileContent(requestFile));
        }

        MvcResult mvcResult = mockMvc.perform(request
                .cookie(new Cookie(AuthenticationParam.USERNAME.getCode(), "TEST"))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is(status.value()))
                .andReturn();

        if (errorDescription != null) {
            assertions.assertThat(mvcResult.getResponse().getContentAsString(StandardCharsets.UTF_8))
                    .contains(errorDescription);
        }
    }
}
