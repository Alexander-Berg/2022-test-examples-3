package ru.yandex.market.ff.controller.api;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;

import ru.yandex.market.ff.base.MvcIntegrationTest;
import ru.yandex.market.ff.enums.FileExtension;
import ru.yandex.market.ff.util.FileContentUtils;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;
import static java.lang.ClassLoader.getSystemResourceAsStream;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.fileUpload;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Интеграционный тест для {@link RequestDocsController}.
 *
 * @author avetokhin 18/01/18.
 */
class RequestDocsControllerTest extends MvcIntegrationTest {

    private static final String SEARCH_RESULT = "[{\"id\":1,\"requestId\":1,\"type\":0,"
            + "\"createdAt\":\"1999-09-09T09:09:09\",\"fileUrl\":\"FILE_URL\"}]";

    private static final String GET_BY_ID_RESULT = "{\"id\":1,\"requestId\":1,\"type\":0,"
            + "\"createdAt\":\"1999-09-09T09:09:09\",\"fileUrl\":\"FILE_URL\"}";

    private static final String TEST_URL = "http://localhost:8080/file";

    @BeforeEach
    void init() throws MalformedURLException {
        when(mdsS3Client.getUrl(any())).thenReturn(new URL(TEST_URL));
    }


    @Test
    @DatabaseSetup("classpath:controller/request-doc/before-create.xml")
    @ExpectedDatabase(value = "classpath:controller/request-doc/after-create.xml", assertionMode = NON_STRICT)
    void create() throws Exception {
        final InputStream fileStream = getSystemResourceAsStream("controller/request-doc/mx_1_copy.csv");
        MockMultipartFile file = new MockMultipartFile("file", "file", FileExtension.CSV.getMimeType(), fileStream);
        mockMvc.perform(
                fileUpload("/requests/1/documents")
                        .file(file)
                        .param("type", "2")
        ).andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup("classpath:controller/request-doc/before-create.xml")
    @ExpectedDatabase(value = "classpath:controller/request-doc/after-create-by-urls.xml", assertionMode = NON_STRICT)
    void createByUrls() throws Exception {
        mockMvc.perform(
            put("/requests/1/documents/createByUrls")
                .contentType(MediaType.APPLICATION_JSON)
                .content(FileContentUtils.getFileContent("controller/request-doc/create-by-urls.json"))
        ).andDo(print())
            .andExpect(status().isOk())
            .andExpect(content().json(
                FileContentUtils.getFileContent("controller/request-doc/create-by-url-response.json")));

    }

    @Test
    @DatabaseSetup("classpath:controller/request-doc/after-create-by-urls.xml")
    @ExpectedDatabase(value = "classpath:controller/request-doc/after-rewrite-by-urls.xml", assertionMode = NON_STRICT)
    void rewriteDocumentsByUrls() throws Exception {
        mockMvc.perform(
            put("/requests/1/documents/createByUrls")
                .contentType(MediaType.APPLICATION_JSON)
                .content(FileContentUtils.getFileContent("controller/request-doc/rewrite-by-urls.json"))
        ).andDo(print())
            .andExpect(status().isOk())
            .andExpect(content().json(
                FileContentUtils.getFileContent("controller/request-doc/rewrite-by-url-response.json")));

    }

    @Test
    @DatabaseSetup("classpath:controller/request-doc/before-create.xml")
    @ExpectedDatabase(value = "classpath:controller/request-doc/before-create.xml", assertionMode = NON_STRICT)
    void createByInvalidUrl() throws Exception {
        mockMvc.perform(
            put("/requests/1/documents/createByUrls")
                .contentType(MediaType.APPLICATION_JSON)
                .content(FileContentUtils.getFileContent("controller/request-doc/create-by-invalid-urls.json"))
        ).andDo(print())
            .andExpect(status().isInternalServerError());

    }

    @Test
    @DatabaseSetup("classpath:controller/request-doc/before-create.xml")
    @ExpectedDatabase(value = "classpath:controller/request-doc/before-create.xml", assertionMode = NON_STRICT)
    void creteWithProhibitedDocumentType() throws Exception {
        mockMvc.perform(
                put("/requests/1/documents/createByUrls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(FileContentUtils.getFileContent("controller/request-doc/create-by-prohibited.json"))
        ).andDo(print())
                .andExpect(status().isBadRequest());

    }

    @Test
    @DatabaseSetup("classpath:controller/request-doc/docs.xml")
    @ExpectedDatabase(value = "classpath:controller/request-doc/docs.xml", assertionMode = NON_STRICT)
    void getAll() throws Exception {
        final MvcResult result = mockMvc.perform(
                get("/requests/1/documents")
        ).andDo(print())
                .andExpect(status().isOk())
                .andReturn();

        assertThat(result.getResponse().getContentAsString(), equalTo(SEARCH_RESULT));
    }

    @Test
    @DatabaseSetup("classpath:controller/request-doc/docs.xml")
    @ExpectedDatabase(value = "classpath:controller/request-doc/after-delete.xml", assertionMode = NON_STRICT)
    void deleteDoc() throws Exception {
        mockMvc.perform(
                delete("/requests/2/documents/3")
        ).andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup("classpath:controller/request-doc/docs.xml")
    @ExpectedDatabase(value = "classpath:controller/request-doc/after-delete.xml", assertionMode = NON_STRICT)
    void deleteSupplierDoc() throws Exception {
        mockMvc.perform(
                delete("/suppliers/2/requests/2/documents/3")
        ).andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup("classpath:controller/request-doc/docs.xml")
    @ExpectedDatabase(value = "classpath:controller/request-doc/docs.xml", assertionMode = NON_STRICT)
    void deleteSupplierDocNoFound() throws Exception {
        mockMvc.perform(
                delete("/suppliers/1/requests/2/documents/15")
        ).andExpect(status().isNotFound());
    }

    @Test
    @DatabaseSetup("classpath:controller/request-doc/docs.xml")
    @ExpectedDatabase(value = "classpath:controller/request-doc/docs.xml", assertionMode = NON_STRICT)
    void deleteDocNoFound() throws Exception {
        mockMvc.perform(
                delete("/requests/2/documents/15")
        ).andExpect(status().isNotFound());
    }

    @Test
    @DatabaseSetup("classpath:controller/request-doc/docs.xml")
    @ExpectedDatabase(value = "classpath:controller/request-doc/docs.xml", assertionMode = NON_STRICT)
    void deleteRequestNoFound() throws Exception {
        mockMvc.perform(
                delete("/requests/15/documents/1")
        ).andExpect(status().isNotFound());
    }

    @Test
    @DatabaseSetup("classpath:controller/request-doc/docs.xml")
    @ExpectedDatabase(value = "classpath:controller/request-doc/docs.xml", assertionMode = NON_STRICT)
    void getById() throws Exception {
        final MvcResult result = mockMvc.perform(
                get("/requests/1/documents/1")
        ).andDo(print())
                .andExpect(status().isOk())
                .andReturn();

        assertThat(result.getResponse().getContentAsString(), equalTo(GET_BY_ID_RESULT));
    }

    @Test
    @DatabaseSetup("classpath:controller/request-doc/docs.xml")
    @ExpectedDatabase(value = "classpath:controller/request-doc/docs.xml", assertionMode = NON_STRICT)
    void getByIdNotFound() throws Exception {
        mockMvc.perform(
                get("/requests/1/documents/2")
        ).andDo(print())
                .andExpect(status().isNotFound());
    }

    @Test
    @DatabaseSetup("classpath:controller/request-doc/before-error-doc.xml")
    void shouldntThrowNPE() throws Exception { //test for MARKETFF-5207
        mockMvc.perform(
                get("/requests/1/errors-document")
        ).andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup("classpath:controller/request-doc/before-external-errors-doc-without-params.xml")
    void shouldntThrowNPEInCaseEmptyParamsInExternalErrors() throws Exception {
        mockMvc.perform(
                get("/requests/1/errors-document")
        ).andExpect(status().isOk());
    }

}
