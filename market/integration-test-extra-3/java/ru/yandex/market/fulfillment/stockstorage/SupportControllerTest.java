package ru.yandex.market.fulfillment.stockstorage;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@DatabaseSetup("classpath:database/states/support_controller/setup.xml")
public class SupportControllerTest extends AbstractContextualTest {

    @ExpectedDatabase(
            value = "classpath:database/expected/support_controller/park.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    @Test
    public void park() throws Exception {
        mockMvc.perform(post("/support/execution-queue/park")
                .content("{\"ids\" : [1, 2, 3, 4], \"days\" : 2}")
                .contentType("application/json"))
                .andExpect(status().isOk())
                .andReturn().getResponse()
                .getContentAsString();
    }

    @ExpectedDatabase(
            value = "classpath:database/expected/support_controller/delete.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    @Test
    public void delete() throws Exception {
        mockMvc.perform(
                post("/support/execution-queue/delete")
                        .content("{\"ids\":[2, 3, 4]}")
                        .contentType("application/json"))
                .andExpect(status().isOk())
                .andReturn().getResponse()
                .getContentAsString();
    }

    @Test
    @ExpectedDatabase(
            value = "classpath:database/states/support_controller/setup.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void deleteFail() throws Exception {
        mockMvc.perform(
                post("/support/execution-queue/delete")
                        .content("{\"ids\":[1, 2, 3, 4]}")
                        .contentType("application/json"))
                .andExpect(status().isBadRequest())
                .andReturn().getResponse()
                .getContentAsString();
    }

}
