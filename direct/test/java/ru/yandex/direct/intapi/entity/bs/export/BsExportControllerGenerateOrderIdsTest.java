package ru.yandex.direct.intapi.entity.bs.export;

import java.util.HashMap;
import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.commons.lang3.RandomUtils;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import ru.yandex.direct.core.testing.MockMvcCreator;
import ru.yandex.direct.intapi.configuration.IntApiTest;
import ru.yandex.direct.utils.JsonUtils;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.direct.intapi.ErrorResponse.ErrorCode.BAD_PARAM;

@RunWith(SpringJUnit4ClassRunner.class)
@IntApiTest
public class BsExportControllerGenerateOrderIdsTest {
    private static final String URL = "/bsexport/generate-order-ids";
    private static final String ASSERT_REASON = "count param must be between 1 and 1000";
    private static final int CORRECT_BSORDERID_COUNT_MIN = 1;
    private static final int CORRECT_BSORDERID_COUNT_MAX = 1000;
    private static final int NEGATIVE_BSORDERID_COUNT = -1;

    @Autowired
    private BsExportController controller;
    @Autowired
    private MockMvcCreator mockMvcCreator;

    private MockMvc mockMvc;

    @Before
    public void before() {
        mockMvc = mockMvcCreator.setup(controller).build();
    }

    @Test
    public void testControllerReturnsExpectedResponse() throws Exception {
        List<Long> firstResponse = getListResponse(CORRECT_BSORDERID_COUNT_MIN);
        List<Long> secondResponse = getListResponse(CORRECT_BSORDERID_COUNT_MIN);
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(firstResponse.size()).isEqualTo(CORRECT_BSORDERID_COUNT_MIN);
        softly.assertThat(secondResponse.size()).isEqualTo(CORRECT_BSORDERID_COUNT_MIN);
        softly.assertThat(secondResponse.get(0)).isGreaterThan(firstResponse.get(0));
        softly.assertAll();
    }

    @Test
    public void testControllerReturnsExpectedResponseMax() throws Exception {
        checkGoodParam(CORRECT_BSORDERID_COUNT_MAX);
    }

    @Test
    public void testControllerReturnsExpectedResponseRandom() throws Exception {
        int count = RandomUtils.nextInt(CORRECT_BSORDERID_COUNT_MIN, CORRECT_BSORDERID_COUNT_MAX + 1);
        checkGoodParam(count);
    }

    @Test
    public void testControllerReturnsBadResponseTooBigCount() throws Exception {
        checkBadParam(CORRECT_BSORDERID_COUNT_MAX + 1);
    }

    @Test
    public void testControllerReturnsBadResponseNegativeCount() throws Exception {
        checkBadParam(NEGATIVE_BSORDERID_COUNT);
    }

    @Test
    public void testControllerReturnsBadResponseTooLittleCount() throws Exception {
        checkBadParam(CORRECT_BSORDERID_COUNT_MIN - 1);
    }

    private void checkGoodParam(int count) throws Exception {
        List<Long> response = getListResponse(count);
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(response.size()).isEqualTo(count);
        softly.assertAll();
    }

    private void checkBadParam(int count) throws Exception {
        HashMap<String, String> mapResponse = getMapResponse(count);
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(mapResponse).containsEntry("code", BAD_PARAM.name());
        softly.assertThat(mapResponse).containsEntry("message", ASSERT_REASON);
        softly.assertAll();
    }

    private List<Long> getListResponse(int count) throws Exception {
        String stringResponse = performRequest(count)
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonUtils.fromJson(stringResponse, new TypeReference<List<Long>>() {
        });
    }

    private HashMap<String, String> getMapResponse(int count) throws Exception {
        String stringResponse = performRequest(count)
                .andExpect(status().is4xxClientError())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonUtils.fromJson(stringResponse, new TypeReference<HashMap<String, String>>() {
        });
    }

    private ResultActions performRequest(int count) throws Exception {
        MockHttpServletRequestBuilder requestBuilder = post(URL)
                .contentType(MediaType.APPLICATION_JSON)
                .param("count", Integer.toString(count));
        return mockMvc.perform(requestBuilder);
    }
}
