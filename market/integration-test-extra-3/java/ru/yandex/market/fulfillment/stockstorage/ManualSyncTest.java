package ru.yandex.market.fulfillment.stockstorage;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import ru.yandex.market.fulfillment.stockstorage.configuration.AsyncTestConfiguration;
import ru.yandex.market.fulfillment.stockstorage.service.health.monitoring.jobs.SyncJobName;
import ru.yandex.market.fulfillment.stockstorage.util.AsyncWaiterService;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@Import(AsyncTestConfiguration.class)
public class ManualSyncTest extends AbstractContextualTest {
    private static final String SUPPORT_SYNC_URL = "/support/sync";

    private static final String SYNC_TYPE_PARAM = "syncType";
    private static final String WAREHOUSE_ID_PARAM = "warehouseId";
    private static final String SS_TOKEN_HEADER = "SS-Token";
    private static final String BATCH_SIZE_PARAM = "batchSize";

    private static final String INVALID_SYNC_TYPE_PARAM_VALUE = "invalidSyncType";
    private static final int WAREHOUSE_ID_PARAM_VALUE = 42;
    private static final String SS_TOKEN_HEADER_VALUE = "test_support_token";
    private static final int BATCH_SIZE_PARAM_VALUE = 250;

    @Autowired
    private AsyncWaiterService asyncWaiterService;

    @Test
    public void requiredParamsNotSpecified() throws Exception {
        mockMvc.perform(
                post(SUPPORT_SYNC_URL)
                        .param(SYNC_TYPE_PARAM, SyncJobName.FULL_SYNC.getValue())
                        .header(SS_TOKEN_HEADER, SS_TOKEN_HEADER_VALUE)
        )
                .andExpect(status().is4xxClientError())
                .andExpect(content().string(
                        containsString("Required request parameter 'warehouseId' for " +
                                "method parameter type Integer is not present"))
                );
    }

    @Test
    public void invalidSyncTypeParam() throws Exception {
        mockMvc.perform(
                post(SUPPORT_SYNC_URL)
                        .param(SYNC_TYPE_PARAM, INVALID_SYNC_TYPE_PARAM_VALUE)
                        .param(WAREHOUSE_ID_PARAM, String.valueOf(WAREHOUSE_ID_PARAM_VALUE))
                        .header(SS_TOKEN_HEADER, SS_TOKEN_HEADER_VALUE)
        )
                .andExpect(status().is4xxClientError())
                .andExpect(content().string(
                        containsString("Unknown value for SyncType: invalidSyncType. Possible values are:"))
                );
    }

    @Test
    public void executeAllSyncs() throws Exception {
        for (SyncJobName syncType : SyncJobName.values()) {
            executeSuccessScenario(syncType);
        }

        asyncWaiterService.awaitTasks(1000);
    }

    private void executeSuccessScenario(SyncJobName syncType) throws Exception {
        mockMvc.perform(
                post(SUPPORT_SYNC_URL)
                        .param(SYNC_TYPE_PARAM, syncType.getValue())
                        .param(WAREHOUSE_ID_PARAM, String.valueOf(WAREHOUSE_ID_PARAM_VALUE))
                        .param(BATCH_SIZE_PARAM, String.valueOf(BATCH_SIZE_PARAM_VALUE))
                        .header(SS_TOKEN_HEADER, SS_TOKEN_HEADER_VALUE)
        )
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().string("Sync with type " + syncType.getValue() +
                        " on warehouse " + WAREHOUSE_ID_PARAM_VALUE + " scheduled"));
    }
}
