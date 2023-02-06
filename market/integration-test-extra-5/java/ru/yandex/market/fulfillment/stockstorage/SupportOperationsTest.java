package ru.yandex.market.fulfillment.stockstorage;

import java.util.Arrays;
import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import ru.yandex.market.fulfillment.stockstorage.domain.entity.FreezeReason;
import ru.yandex.market.fulfillment.stockstorage.domain.entity.FreezeReasonType;
import ru.yandex.market.fulfillment.stockstorage.domain.entity.Sku;
import ru.yandex.market.fulfillment.stockstorage.domain.entity.UnitId;
import ru.yandex.market.fulfillment.stockstorage.repository.SkuRepository;
import ru.yandex.market.fulfillment.stockstorage.repository.UnfreezeJobRepository;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static java.util.Collections.singletonList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class SupportOperationsTest extends AbstractContextualTest {

    private static final String FORCE_UPDATE_KOROBYTE = "/support/force-update/korobytes";
    private static final String DELETE_UNFREEZE_JOB = "/support/unfreeze-job";
    private static final String EXPORT_SKUS_AVAILABILITY_URL = "/support/saas-hub/export/skus";
    private static final String EXPORT_SKUS_AVAILABLE_AMOUNT_URL = "/support/saas-hub/export/skus/available-amount";
    private static final String EXPORT_SKUS_AVAILABLE_AMOUNT_FOR_ALL_STOCK_TYPES_URL =
            "/support/saas-hub/export/skus/available-amount-for-all-stock-types";

    private static final String EXPORT_DATA = "{" +
            "    \"totalLimit\": 1000," +
            "    \"pageSize\": 3," +
            "    \"offsetId\": 0" +
            "}";

    @Autowired
    private UnfreezeJobRepository unfreezeJobRepository;
    @Autowired
    private SkuRepository skuRepository;
    @Captor
    private ArgumentCaptor<List<Sku>> skuListCaptor;

    @Test
    @DatabaseSetup("classpath:database/states/stocks_orders_unfreeze_scheduled.xml")
    public void deleteUnfreezeJob() throws Exception {
        softly
                .assertThat(unfreezeJobRepository.findById(10011L).orElse(null).getExecuted())
                .isNull();

        mockMvc.perform(delete(DELETE_UNFREEZE_JOB)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("{\"ids\":[10011]}"))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        softly
                .assertThat(unfreezeJobRepository.findById(10011L).orElse(null).getExecuted())
                .isNotNull();

        verify(freezeEventAuditService).logUnfreezeJobsCanceled(
                eq(singletonList(FreezeReason.of("12345", FreezeReasonType.ORDER))),
                eq("Manually deleting unfreeze jobs."));
    }

    @Test
    @DatabaseSetup("classpath:database/states/stocks_pushed.xml")
    @ExpectedDatabase(value = "classpath:database/expected/korobytes/korobytes_just_pushed.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void forcePushKorobytesOnEmpty() throws Exception {
        mockMvc.perform(post(FORCE_UPDATE_KOROBYTE)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .header("SS-Token", "test_support_token")
                .content(extractFileContent("requests/stocks/push_korobytes.json")))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        List<Sku> skus = skuRepository.findAllByUnitIdIn(Arrays.asList(
                new UnitId("sku0", 12L, 1),
                new UnitId("sku1", 12L, 1)
        ));

        verify(skuEventAuditService).logKorobytesPushed(skuListCaptor.capture());

        softly.assertThat(skuListCaptor.getValue()).hasSameElementsAs(skus);
    }

    @Test
    @DatabaseSetup("classpath:database/states/korobyte/before_update.xml")
    @ExpectedDatabase(value = "classpath:database/states/korobyte/after_update.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void updateKorobytesOnMultipleWarehouses() throws Exception {
        mockMvc.perform(post(FORCE_UPDATE_KOROBYTE)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .header("SS-Token", "test_support_token")
                .content(extractFileContent("requests/support/update_korobytes.json")))
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse()
                .getContentAsString();
    }

    @Test
    @DatabaseSetup("classpath:database/states/korobyte/before_update.xml")
    @ExpectedDatabase(value = "classpath:database/states/korobyte/before_update.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void updateKorobytesForTwoSameUnitIds() throws Exception {
        mockMvc.perform(post(FORCE_UPDATE_KOROBYTE)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .header("SS-Token", "test_support_token")
                .content(extractFileContent("requests/support/update_kotobytes_same_unit_ids.json")))
                .andExpect(status().is4xxClientError())
                .andExpect(content().string(
                        "{\"code\":\"BAD_REQUEST\",\"message\":\"[Request contains some units more than once: " +
                                "[UnitId{sku='sku1', vendorId=12, warehouseId=10}]]\"}"));
    }

    @Test
    public void forceUnfreezeStocksUnfreezeTokenFail() throws Exception {
        String response = mockMvc.perform(post("/support/force-unfreeze/stocks")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .header("SS-Token", "wrong_token")
                .content(extractFileContent("requests/stocks/force_unfreeze_stocks.json")))
                .andExpect(status().is4xxClientError())
                .andReturn().getResponse().getContentAsString();
        softly
                .assertThat(response)
                .is(jsonMatching(extractFileContent("response/errors/token_validation_error.json")));
    }

    @Test
    public void forceUnfreezeStocksUnfreezeCommentFormatFailed() throws Exception {
        String response = mockMvc.perform(post("/support/force-unfreeze/stocks")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .header("SS-Token", "test_support_token")
                .content(extractFileContent("requests/stocks/force_unfreeze_stocks_broken_comment.json")))
                .andExpect(status().is4xxClientError())
                .andReturn().getResponse().getContentAsString();
        softly
                .assertThat(response)
                .is(jsonMatching(extractFileContent("response/errors/additional_comment_pattern_mismatch.json")));
    }

    @Test
    @DatabaseSetup("classpath:database/states/stocks_frozen_orders_with_defects.xml")
    @ExpectedDatabase(value = "classpath:database/expected/freeze/forced_unfreezed.xml", assertionMode =
            NON_STRICT_UNORDERED)
    public void forceUnfreezeStocksUnfreezeMultipleSuccess() throws Exception {
        mockMvc.perform(post("/support/force-unfreeze/stocks")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .header("SS-Token", "test_support_token")
                .content(extractFileContent("requests/stocks/force_unfreeze_stocks.json")))
                .andExpect(status().is2xxSuccessful())
                .andReturn();
    }

    @Test
    @DatabaseSetup("classpath:database/states/stocks_frozen_orders_with_defects.xml")
    @ExpectedDatabase(value = "classpath:database/states/stocks_frozen_orders_with_defects.xml", assertionMode =
            NON_STRICT_UNORDERED)
    public void forceUnfreezeStocksUnfreezeInvalidSkusFailing() throws Exception {
        String response = mockMvc.perform(post("/support/force-unfreeze/stocks")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .header("SS-Token", "test_support_token")
                .param("additionalComment", "DELIVERY-5051 comment")
                .content(extractFileContent("requests/stocks/force_unfreeze_stocks_extended.json")))
                .andExpect(status().is4xxClientError())
                .andReturn().getResponse().getContentAsString();
        softly
                .assertThat(response)
                .is(jsonMatching(extractFileContent("response/errors/stock_validation_error.json")));
    }

    @Test
    @DatabaseSetup({
            "classpath:database/states/system_property.xml",
            "classpath:database/states/export_skus/1.xml"
    })
    @ExpectedDatabase(
            value = "classpath:database/expected/export_skus/1/queue_state.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void exportAllSkusInOneBatch() throws Exception {
        MockHttpServletRequestBuilder request = post(EXPORT_SKUS_AVAILABILITY_URL)
                .header("SS-Token", "test_support_token");

        mockMvc.perform(request)
                .andExpect(status().is2xxSuccessful());
    }

    @Test
    @DatabaseSetup({
            "classpath:database/states/system_property.xml",
            "classpath:database/states/export_skus/2.xml"
    })
    @ExpectedDatabase(
            value = "classpath:database/expected/export_skus/2/queue_state.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void exportAllSkusInMultipleBatches() throws Exception {
        MockHttpServletRequestBuilder request = post(EXPORT_SKUS_AVAILABILITY_URL)
                .header("SS-Token", "test_support_token");

        mockMvc.perform(request)
                .andExpect(status().is2xxSuccessful());
    }

    @Test
    @DatabaseSetup({
            "classpath:database/states/system_property.xml",
            "classpath:database/states/export_skus/1.xml"
    })
    @ExpectedDatabase(
            value = "classpath:database/expected/export_skus/3/queue_state.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void exportAllSkusAvailableAmountInOneBatch() throws Exception {
        MockHttpServletRequestBuilder request = post(EXPORT_SKUS_AVAILABLE_AMOUNT_URL)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(EXPORT_DATA)
                .header("SS-Token", "test_support_token");

        mockMvc.perform(request)
                .andExpect(status().is2xxSuccessful());
    }

    @Test
    @DatabaseSetup({
            "classpath:database/states/system_property.xml",
            "classpath:database/states/export_skus/2.xml"
    })
    @ExpectedDatabase(
            value = "classpath:database/expected/export_skus/4/queue_state.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void exportAllSkusAvailableAmountInMultipleBatches() throws Exception {
        MockHttpServletRequestBuilder request = post(EXPORT_SKUS_AVAILABLE_AMOUNT_URL)
                .header("SS-Token", "test_support_token");

        mockMvc.perform(request)
                .andExpect(status().is2xxSuccessful());
    }

    @Test
    @DatabaseSetup({
            "classpath:database/states/system_property.xml",
            "classpath:database/states/export_skus/1.xml"
    })
    @ExpectedDatabase(
            value = "classpath:database/expected/export_skus/5/queue_state.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void exportAllSkusAvailableAmountForAllStocksInOneBatch() throws Exception {
        MockHttpServletRequestBuilder request = post(EXPORT_SKUS_AVAILABLE_AMOUNT_FOR_ALL_STOCK_TYPES_URL)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(EXPORT_DATA)
                .header("SS-Token", "test_support_token");

        mockMvc.perform(request)
                .andExpect(status().is2xxSuccessful());
    }

    @Test
    @DatabaseSetup({
            "classpath:database/states/system_property.xml",
            "classpath:database/states/export_skus/2.xml"
    })
    @ExpectedDatabase(
            value = "classpath:database/expected/export_skus/6/queue_state.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void exportAllSkusAvailableAmountForAllStocksInMultipleBatches() throws Exception {
        MockHttpServletRequestBuilder request = post(EXPORT_SKUS_AVAILABLE_AMOUNT_FOR_ALL_STOCK_TYPES_URL)
                .header("SS-Token", "test_support_token");

        mockMvc.perform(request)
                .andExpect(status().is2xxSuccessful());
    }

    @Test
    @DatabaseSetup({
            "classpath:database/states/system_property.xml",
            "classpath:database/states/export_skus/1.xml"
    })
    @ExpectedDatabase(
            value = "classpath:database/expected/export_skus/7/queue_state.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void exportAllSkusAvailableAmountForAllStocksInOneBatchWithOffsetId() throws Exception {
        MockHttpServletRequestBuilder request = post(EXPORT_SKUS_AVAILABLE_AMOUNT_FOR_ALL_STOCK_TYPES_URL)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("{" +
                        "    \"offsetId\": 10001" +
                        "}")
                .header("SS-Token", "test_support_token");

        mockMvc.perform(request)
                .andExpect(status().is2xxSuccessful());
    }
}
