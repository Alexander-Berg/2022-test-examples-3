package ru.yandex.market.logistics.iris.controller;

import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.logistics.iris.configuration.AbstractContextualTest;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class SupportControllerTest extends AbstractContextualTest {
    private static final String URL = "/support/sync";

    @Test
    public void executeSyncSuccess() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders.post(URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent("fixtures/controller/request/support/manual-sync-request-success.json"))
        ).andExpect(status().is2xxSuccessful());
    }

    @Test
    public void executeSyncErrorNoSourceId() throws Exception {
        MockHttpServletRequestBuilder httpOperation = MockMvcRequestBuilders.post(URL)
            .contentType(MediaType.APPLICATION_JSON)
            .content(extractFileContent("fixtures/controller/request/support/manual-sync-request-error-no-source-id.json"));
        String result = httpOperationWithResult(httpOperation, status().is4xxClientError());
        assertions().assertThat(result).isEqualTo("Following validation errors occurred:\nField: sourceId, message: must not be empty");
    }

    @Test
    public void executeSyncErrorInvalidQueueTypeValue() throws Exception {
        MockHttpServletRequestBuilder httpOperation = MockMvcRequestBuilders.post(URL)
            .contentType(MediaType.APPLICATION_JSON)
            .content(extractFileContent("fixtures/controller/request/support/manual-sync-request-error-invalid-queue-type-value.json"));
        String result = httpOperationWithResult(httpOperation, status().is4xxClientError());
        assertions().assertThat(result).contains("Unknown value for QueueType: unsupported-queue-type. Possible values are:");
    }

    @Test
    public void executeSyncErrorInvalidSourceTypeValue() throws Exception {
        MockHttpServletRequestBuilder httpOperation = MockMvcRequestBuilders.post(URL)
            .contentType(MediaType.APPLICATION_JSON)
            .content(extractFileContent("fixtures/controller/request/support/manual-sync-request-error-invalid-source-type-value.json"));
        String result = httpOperationWithResult(httpOperation, status().is4xxClientError());
        assertions().assertThat(result).contains("Unknown value for SourceType: unsupported-source-type. Possible values are:");
    }
}
