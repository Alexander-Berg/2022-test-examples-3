package ru.yandex.market.ff.controller.api;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.ff.base.MvcIntegrationTest;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Интеграционные тесты для {@link CustomerReturnController}.
 *
 * @author avetokhin 01/02/18.
 */
class CustomerReturnControllerTest extends MvcIntegrationTest {

    private static final String RETURN_ID = "return1";
    private static final String RETURN_WITH_UPLOADING_FINISH_DATE_ID = "return2";
    private static final String RETURN_RESPONSE =
            "{\"id\":\"return1\",\"createdAt\":\"2018-01-01T10:10:10\",\"requestId\":10,\"serviceId\":555}";
    private static final String RETURN_RESPONSE_WITH_UPLOADING_FINISH_DATE =
            "{\"id\":\"return2\",\"createdAt\":\"2018-01-01T10:10:10\",\"requestId\":10,\"serviceId\":555," +
                    "\"uploadingFinishDate\":\"2020-10-12T09:00:00+03:00\"}";
    private static final String CREATE_REQUEST =
            "{\"serviceId\":555, \"items\":[{\"supplierId\":1,\"article\":\"article1\"}]}";
    private static final String CREATE_REQUEST_WITH_UPLOADING_FINISH_DATE =
            "{\"serviceId\":555, \"items\":[{\"supplierId\":1,\"article\":\"article1\"}], " +
                    "\"uploadingFinishDate\":\"2020-10-12T06:00:00Z\"}";

    @Test
    @DatabaseSetup("classpath:controller/return-api/before.xml")
    @ExpectedDatabase(value = "classpath:controller/return-api/after-create.xml", assertionMode = NON_STRICT)
    void create() throws Exception {
        performPostNewReturn(CREATE_REQUEST)
            .andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup("classpath:controller/return-api/before.xml")
    @ExpectedDatabase(value = "classpath:controller/return-api/after-create.xml", assertionMode = NON_STRICT)
    void createWithUploadingFinishDate() throws Exception {
        performPostNewReturn(CREATE_REQUEST_WITH_UPLOADING_FINISH_DATE)
                .andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup("classpath:controller/return-api/after-create.xml")
    @ExpectedDatabase(value = "classpath:controller/return-api/after-create.xml", assertionMode = NON_STRICT)
    void createSecondTime() throws Exception {
        performPostNewReturn(CREATE_REQUEST)
            .andExpect(status().isAlreadyReported());

    }

    @Test
    @DatabaseSetup("classpath:controller/return-api/returns.xml")
    void getById() throws Exception {
        String result = performGet(RETURN_ID)
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
        assertThat(result, equalTo(RETURN_RESPONSE));
    }

    @Test
    @DatabaseSetup("classpath:controller/return-api/returns.xml")
    void getByIdWithUploadingFinishDate() throws Exception {
        String result = performGet(RETURN_WITH_UPLOADING_FINISH_DATE_ID)
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertThat(result, equalTo(RETURN_RESPONSE_WITH_UPLOADING_FINISH_DATE));
    }

    @Test
    @DatabaseSetup("classpath:controller/return-api/returns.xml")
    void getByIdNotFound() throws Exception {
        performGet("no_return_found")
            .andExpect(status().isNotFound());
    }

    @Test
    void createEmpty() throws Exception {
        mockMvc.perform(
            post("/returns/" + RETURN_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"serviceId\":555, \"items\":[]}")
        )
            .andDo(print())
            .andExpect(status().isBadRequest())
            .andExpect(content().string("{\"message\":\"items may not be empty\"}"));
    }

    @Test
    @DatabaseSetup("classpath:controller/return-api/before.xml")
    void createWithUnknownSupplier() throws Exception {
        mockMvc.perform(
            post("/returns/" + RETURN_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"serviceId\":555, \"items\":[{\"supplierId\":1,\"article\":\"article1\"}, "
                    + "{\"supplierId\":2,\"article\":\"article2\"}]}")
        )
            .andDo(print())
            .andExpect(status().isNotFound())
            .andExpect(content().string("{\"message\":\"Failed to find [SUPPLIER] with id [2]\","
                + "\"resourceType\":\"SUPPLIER\",\"identifier\":\"2\"}"));

    }

    private ResultActions performGet(String id) throws Exception {
        return mockMvc.perform(
            get("/returns/" + id)
                .contentType(MediaType.APPLICATION_JSON))
            .andDo(print());
    }

    private ResultActions performPostNewReturn(String request) throws Exception {
        return mockMvc.perform(
            post("/returns/" + RETURN_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(request)
        )
            .andDo(print());
    }

}
