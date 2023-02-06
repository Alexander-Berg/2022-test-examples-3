package ru.yandex.market.replenishment.autoorder.api;

import org.junit.Test;
import org.skyscreamer.jsonassert.Customization;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.RegularExpressionValueMatcher;
import org.skyscreamer.jsonassert.comparator.CustomComparator;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import ru.yandex.market.replenishment.autoorder.config.ControllerTest;
import ru.yandex.market.replenishment.autoorder.security.WithMockLogin;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
@WithMockLogin
public class CorrectionReasonControllerTest extends ControllerTest {

    @Test
    public void testGetCorrectionReasons() throws Exception {
        String correctionReasonItem = "{id:\".+\", name:\".+\"},";
        String expectedContent = "[" + String.format("%21s", "").replace(" ", correctionReasonItem) + "]";

        MvcResult result = mockMvc.perform(get("/correction-reasons"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andDo(print())
                .andReturn();

        JSONAssert.assertEquals(expectedContent, result.getResponse().getContentAsString(), new CustomComparator(
                JSONCompareMode.LENIENT,
                new Customization("***", new RegularExpressionValueMatcher<>())
        ));

    }
}
