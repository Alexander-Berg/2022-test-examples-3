package ru.yandex.market.fulfillment.stockstorage.freeze.orders;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import ru.yandex.market.fulfillment.stockstorage.AbstractContextualTest;
import ru.yandex.market.fulfillment.stockstorage.client.entity.StockStorageErrorStatusCode;
import ru.yandex.market.fulfillment.stockstorage.domain.exception.FreezeNotFoundException;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class OldOrderUnfreezeTest extends AbstractContextualTest {

    private static final String UNFREEZE_URL = "/stocks/unfreeze";

    @Test
    @DatabaseSetup("classpath:database/states/stocks_orders_unfreeze_scheduled.xml")
    @ExpectedDatabase(value = "classpath:database/expected/freeze/stocks_unfreeze_scheduled_nothing_is_changed.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void skipUnfreezeIfUnfreezeAlreadyCaught() throws Exception {
        mockMvc.perform(post(UNFREEZE_URL)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("{\"orderId\": \"12345\"}"))
                .andExpect(status().is2xxSuccessful())
                .andReturn();
    }

    @Test
    public void unfreezeStocksFailedDueToNoFreezesFound() throws Exception {
        String contentAsString = mockMvc.perform(post(UNFREEZE_URL)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("{\"orderId\": \"12345\"}"))
                .andExpect(status().is(StockStorageErrorStatusCode.FREEZE_NOT_FOUND.getCode()))
                .andReturn().getResponse()
                .getContentAsString();
        softly
                .assertThat(contentAsString)
                .contains(FreezeNotFoundException.MESSAGE);
    }

    @Test
    @DatabaseSetup("classpath:database/states/stocks_order_refreezed.xml")
    @ExpectedDatabase(value = "classpath:database/expected/freeze/unfreeze_jobs_created.xml", assertionMode =
            NON_STRICT_UNORDERED)
    public void unfreezeScheduled() throws Exception {
        mockMvc.perform(post(UNFREEZE_URL)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("{\"orderId\": \"12345\"}"))
                .andExpect(status().is2xxSuccessful())
                .andReturn();
        mockMvc.perform(post(UNFREEZE_URL)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("{\"orderId\": \"123456\"}"))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        verify(freezeEventAuditService, times(2)).logUnfreezeScheduled(anyList());
        verify(stockEventsHandler, times(2)).handle(anyList());
    }

    @Test
    @DatabaseSetup("classpath:database/states/stocks_order_refreezed_with_unfreeze_jobs.xml")
    @ExpectedDatabase(value = "classpath:database/expected/freeze/unfreeze_new_jobs_created.xml", assertionMode =
            NON_STRICT_UNORDERED)
    public void unfreezeAfterRefreezeWithJob() throws Exception {
        mockMvc.perform(post(UNFREEZE_URL)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("{\"orderId\": \"12345\"}"))
                .andExpect(status().is2xxSuccessful())
                .andReturn();
        mockMvc.perform(post(UNFREEZE_URL)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("{\"orderId\": \"123456\"}"))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        verify(freezeEventAuditService, times(1)).logUnfreezeScheduled(anyList());
        verify(stockEventsHandler, times(1)).handle(anyList());
    }
}
