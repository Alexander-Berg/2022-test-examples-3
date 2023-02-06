package ru.yandex.market.wms.receiving.usecases;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import ru.yandex.market.wms.common.spring.utils.JsonAssertUtils;
import ru.yandex.market.wms.receiving.ReceivingIntegrationTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.wms.common.spring.utils.FileContentUtils.getFileContent;

@DisplayName("Получить статусы поставок")
public class GetInboundHistoriesTests extends ReceivingIntegrationTest {

    @Test
    @DatabaseSetup(
            value = "/usecases/getinboundhistories/testCorrectStatusesWhenTrailerClosedAfterReceipt/immutable.xml")
    @ExpectedDatabase(
            value = "/usecases/getinboundhistories/testCorrectStatusesWhenTrailerClosedAfterReceipt/immutable.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    @DisplayName("Получить статусы поставки при закрытии трейлера после конца вторичной приемки")
    public void testCorrectStatusesWhenTrailerClosedAfterReceipt() throws Exception {
        assertRequest(
                post("/INFOR_SCPRD_wmwhse1/inbound/get-inbound-histories"),
                status().isOk(),
                "usecases/getinboundhistories/testCorrectStatusesWhenTrailerClosedAfterReceipt/request.json",
                "usecases/getinboundhistories/testCorrectStatusesWhenTrailerClosedAfterReceipt/response.json"
        );
    }

    @Test
    @DatabaseSetup(
            value = "/usecases/getinboundhistories/testCorrectStatusesWhenTrailerClosedBeforeReceiving/immutable.xml")
    @ExpectedDatabase(
            value = "/usecases/getinboundhistories/testCorrectStatusesWhenTrailerClosedBeforeReceiving/immutable.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    @DisplayName("Получить статусы поставки при закрытии трейлера до начала вторичной приемки")
    public void testCorrectStatusesWhenTrailerClosedBeforeReceiving() throws Exception {
        assertRequest(
                post("/INFOR_SCPRD_wmwhse1/inbound/get-inbound-histories"),
                status().isOk(),
                "usecases/getinboundhistories/testCorrectStatusesWhenTrailerClosedBeforeReceiving/request.json",
                "usecases/getinboundhistories/testCorrectStatusesWhenTrailerClosedBeforeReceiving/response.json"
        );
    }

    private void assertRequest(
            MockHttpServletRequestBuilder requestBuilder,
            ResultMatcher status, String requestFile, String responseFile) throws Exception {
        MvcResult result = mockMvc.perform(requestBuilder
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent(requestFile)))
                .andDo(print())
                .andExpect(status)
                .andReturn();
        if (responseFile != null) {
            JsonAssertUtils.assertFileNonExtensibleEquals(responseFile,
                    result.getResponse().getContentAsString());
        }
    }
}
