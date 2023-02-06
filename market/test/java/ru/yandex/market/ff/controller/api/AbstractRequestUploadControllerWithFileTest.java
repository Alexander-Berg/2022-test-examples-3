package ru.yandex.market.ff.controller.api;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import ru.yandex.market.ff.base.MvcIntegrationTest;
import ru.yandex.market.ff.client.enums.RequestType;
import ru.yandex.market.ff.util.FileContentUtils;

import static java.lang.ClassLoader.getSystemResourceAsStream;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

public abstract class AbstractRequestUploadControllerWithFileTest extends MvcIntegrationTest {

    protected static final String FILE_URL = "http://localhost:8080/file";
    protected static final String VALID_FILE_PATH = "integration/%s/v%s/request";
    protected static final String ERR_FILE_PATH = "integration/%s/v%s/request_err";
    protected static final String UNKNOWN_EXTERNAL_OPERATION_TYPE = "666";

    protected ResultActions upload(long supplierId,
                                   MockMultipartFile file,
                                   String typePath,
                                   Integer inboundType,
                                   Long serviceId,
                                   Map<String, String> params) throws Exception {
        MockHttpServletRequestBuilder upload = multipart("/upload-request-file/" + typePath)
                .file(file)
                .param("supplierId", String.valueOf(supplierId));

        Optional.ofNullable(serviceId).map(Object::toString).ifPresent(value -> upload.param("serviceId", value));
        Optional.ofNullable(inboundType).map(Object::toString).ifPresent(value -> upload.param("type", value));

        if (params != null) {
            params.forEach(upload::param);
        }

        return mockMvc.perform(upload)
                .andDo(print());
    }

    protected void assertJsonResponseCorrect(String filename, MvcResult response) throws IOException {
        JSONAssert.assertEquals(getJsonFromFile(filename), response.getResponse().getContentAsString(),
                JSONCompareMode.NON_EXTENSIBLE);
    }

    protected String getJsonFromFile(String name) throws IOException {
        return FileContentUtils.getFileContent("controller/upload-request/" + name);
    }

    protected MockMultipartFile getValidFile(RequestType type,
                                             String extension,
                                             String mimeType) throws IOException {
        return getValidFile(type, extension, mimeType, 1);
    }

    protected MockMultipartFile getValidFile(RequestType type,
                                             String extension,
                                             String mimeType,
                                             int version) throws IOException {
        return getFile(VALID_FILE_PATH, type, extension, mimeType, version);
    }

    protected MockMultipartFile getFileWithErrors(RequestType type,
                                             String extension,
                                             String mimeType) throws IOException {
        return getFileWithErrors(type, extension, mimeType, 1);
    }

    protected MockMultipartFile getFileWithErrors(RequestType type,
                                                  String extension,
                                                  String mimeType,
                                                  int version) throws IOException {
        return getFile(ERR_FILE_PATH, type, extension, mimeType, version);
    }

    protected MockMultipartFile getFile(String path,
                                        RequestType type,
                                        String extension,
                                        String mimeType) throws IOException {
        return getFile(path, type, extension, mimeType, 1);
    }

    protected MockMultipartFile getFile(String path,
                                        RequestType type,
                                        String extension,
                                        String mimeType,
                                        int version) throws IOException {
        String typePath = String.format(path, type.name().toLowerCase(), version);
        String origFileName = String.format("filename.%s", extension);
        String resourceFileName = String.format("%s.%s", typePath, extension);
        return new MockMultipartFile("file", origFileName, mimeType,
                getSystemResourceAsStream(resourceFileName));
    }
}
