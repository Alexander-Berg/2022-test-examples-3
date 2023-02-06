package ru.yandex.market.replenishment.autoorder.config;

import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

public class ExcelTestingHelper {

    private ControllerTest test;

    public ExcelTestingHelper(ControllerTest test) {
        this.test = test;
    }

    public ResultActions upload(String method, String apiUrl, String excelFilename) throws Exception {
        return this.uploadWithHeaderAndParams(method, apiUrl, excelFilename, null, null);
    }

    public ResultActions uploadWithHeaders(String method, String apiUrl, String excelFilename,
                                           Map<String, Object> headers
    ) throws Exception {
        return this.uploadWithHeaderAndParams(method, apiUrl, excelFilename, null, headers);
    }

    public ResultActions uploadWithParams(String method, String apiUrl, String excelFilename,
                                          Map<String, Object> parameters) throws Exception {
        return this.uploadWithHeaderAndParams(method, apiUrl, excelFilename, parameters, null);
    }

    public ResultActions uploadWithHeaderAndParams(String method, String apiUrl, String excelFilename,
                                                   Map<String, Object> parameters, Map<String, Object> headers
    ) throws Exception {
        byte[] bytes = test.getClass().getResourceAsStream(excelFilename).readAllBytes();
        return this.uploadWithHeaderAndParams(method, apiUrl, excelFilename, bytes, parameters, headers);
    }

    public ResultActions uploadWithHeaderAndParams(String method, String apiUrl, String excelFilename, byte[] content,
                                                   Map<String, Object> parameters, Map<String, Object> headers
    ) throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "files", excelFilename, "application/vnd.ms-excel", content);
        MockMultipartHttpServletRequestBuilder builder =
            MockMvcRequestBuilders.multipart(apiUrl);
        builder.with(request -> {
            request.setMethod(method);
            if (parameters != null && !parameters.isEmpty()) {
                request.addParameters(parameters);
            }
            return request;
        });
        builder
            .file(file)
            .contentType(MediaType.MULTIPART_FORM_DATA_VALUE);
        if (headers != null) {
            headers.forEach(builder::header);
        }
        return test.mockMvc.perform(builder);
    }

}
