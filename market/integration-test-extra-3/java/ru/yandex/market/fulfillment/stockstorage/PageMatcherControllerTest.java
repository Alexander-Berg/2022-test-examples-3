package ru.yandex.market.fulfillment.stockstorage;

import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class PageMatcherControllerTest extends AbstractContextualTest {

    private final String pageMatchUrl = "/pageMatch";

    @Test
    public void pageMatchNotNullResponseTest() throws Exception {
        mockMvc.perform(get(pageMatchUrl))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().string(not(isEmptyOrNullString())));
    }
}
