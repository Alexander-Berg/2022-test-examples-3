package ru.yandex.direct.intapi.entity.mobilecontent.controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import ru.yandex.direct.intapi.configuration.IntApiTest;
import ru.yandex.direct.intapi.entity.mobilecontent.model.ApiMobileContentRequest;
import ru.yandex.direct.utils.JsonUtils;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.direct.intapi.entity.mobilecontent.controller.MobileAppApiController.MAX_API_MOBILE_CONTENT_ITEMS;

@IntApiTest
@RunWith(SpringJUnit4ClassRunner.class)
public class MobileAppApiControllerListTest {
    private static final String INVALID_STORE = "notItunes";
    private static final String VALID_STORE = "gplay";

    @Autowired
    private MobileAppApiController controller;

    private MockMvc mockMvc;

    @Before
    public void before() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    public void invalidStore() throws Exception {
        var singleRequest = singletonList(new ApiMobileContentRequest.App(INVALID_STORE, ""));
        String responseContent = call(singleRequest);
        checkException(responseContent, "There is no store called " + INVALID_STORE);
    }

    @Test
    public void invalidIdsNumber() throws Exception {
        var multiRequest = new ArrayList<>(Collections.nCopies(MAX_API_MOBILE_CONTENT_ITEMS + 1,
                new ApiMobileContentRequest.App(VALID_STORE, "")));
        String responseContent = call(multiRequest);
        checkException(responseContent, "Number of requested appIds exceeds " + MAX_API_MOBILE_CONTENT_ITEMS);
    }

    private String call(List<ApiMobileContentRequest.App> apps) throws Exception {
        return mockMvc
                .perform(post("/mobile_app_api/list")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JsonUtils.toJson(new ApiMobileContentRequest(apps))))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andReturn().getResponse().getContentAsString();
    }

    private void checkException(String responseContent, String exceptionText) {
        var response = JsonUtils.fromJson(responseContent, ErrorResponse.class);

        assertSoftly(softly -> {
            softly.assertThat(response.getSuccess()).isFalse();
            softly.assertThat(response.getCode()).isEqualTo(500);
            softly.assertThat(response.getDescription()).isNull();
            softly.assertThat(response.getText()).contains(exceptionText);
        });
    }

    // WebErrorResponse не распаковывается из json
    public static class ErrorResponse {
        @JsonProperty
        private Boolean success;
        @JsonProperty
        private Integer code;
        @JsonProperty
        private String text;
        @JsonProperty
        private String description;

        public Boolean getSuccess() {
            return success;
        }

        public void setSuccess(Boolean success) {
            this.success = success;
        }

        public Integer getCode() {
            return code;
        }

        public void setCode(Integer code) {
            this.code = code;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }

}
