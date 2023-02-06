package ru.yandex.market.ff.controller.api;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.ff.base.MvcIntegrationTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Функциональный тест для {@link UploadErrorController}.
 *
 * @author avetokhin 06.06.18.
 */
@DatabaseSetup("classpath:controller/upload-error/before.xml")
class UploadErrorControllerTest extends MvcIntegrationTest {

    private static final String UPLOAD_ERROR_404_RESPONSE =
        "{\"message\":\"Failed to find [UPLOAD_ERROR] with id [2]\","
            + "\"resourceType\":\"UPLOAD_ERROR\",\"identifier\":\"2\"}";
    private static final String UPLOAD_ERROR_RESPONSE = "{\"id\":1,\"fileUrl\":\"http://localhost:6060\"}";
    private static final long EXISTING_ID = 1L;
    private static final long NOT_EXISTING_ID = 2L;

    @Test
    void getById() throws Exception {
        final MvcResult mvcResult = doGet(EXISTING_ID)
                .andExpect(status().isOk())
                .andDo(print())
                .andReturn();
        assertThat(mvcResult.getResponse().getContentAsString(), equalTo(UPLOAD_ERROR_RESPONSE));
    }

    @Test
    void getByIdNotFound() throws Exception {
        final MvcResult mvcResult = doGet(NOT_EXISTING_ID)
                .andExpect(status().isNotFound())
                .andDo(print())
                .andReturn();
        assertThat(mvcResult.getResponse().getContentAsString(), equalTo(UPLOAD_ERROR_404_RESPONSE));
    }

    private ResultActions doGet(long id) throws Exception {
        return mockMvc.perform(
                get("/upload-errors/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
        );
    }

}
