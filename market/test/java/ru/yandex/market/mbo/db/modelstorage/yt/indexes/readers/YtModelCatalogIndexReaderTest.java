package ru.yandex.market.mbo.db.modelstorage.yt.indexes.readers;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import ru.yandex.market.mbo.appcontext.UnstableInit;
import ru.yandex.market.mbo.db.modelstorage.yt.YtMockingHelper;
import ru.yandex.market.mbo.db.modelstorage.yt.YtModelColumns;
import ru.yandex.market.yt.util.table.YtTableRpcApi;
import ru.yandex.market.yt.util.table.model.YtTableModel;
import ru.yandex.yt.ytclient.proxy.ApiServiceClient;
import ru.yandex.yt.ytclient.proxy.SelectRowsRequest;
import ru.yandex.yt.ytclient.wire.UnversionedRowset;
import ru.yandex.yt.ytclient.wire.UnversionedValue;

import static ru.yandex.market.mbo.db.modelstorage.yt.YtMockingHelper.buildValue;
import static ru.yandex.yt.ytclient.tables.ColumnValueType.INT64;

/**
 * @author apluhin
 * @created 1/18/21
 */
@SuppressWarnings("checkstyle:magicnumber")
public class YtModelCatalogIndexReaderTest {

    YtModelCatalogIndexReader reader;
    YtTableRpcApi rpcApi;
    ApiServiceClient client;

    @Before
    public void setUp() throws Exception {
        reader = new YtModelCatalogIndexReader(
            Mockito.mock(YtTableModel.class),
            Mockito.mock(UnstableInit.class),
            true
        );
        rpcApi = Mockito.mock(YtTableRpcApi.class);
        client = Mockito.mock(ApiServiceClient.class);
        Mockito.when(rpcApi.getClient()).thenReturn(client);
        ReflectionTestUtils.setField(reader, "rpcApi", rpcApi);
    }

    @Test
    public void testGetModelsAmountByCategory() {
        ArgumentCaptor<SelectRowsRequest> stringArgumentCaptor = ArgumentCaptor.forClass(SelectRowsRequest.class);
        List<Long> result = Arrays.asList(100L, 1L, 2L);

        UnversionedRowset unversionedRowset = YtMockingHelper.buildModelStoreResponse(client, rpcApi,
            Arrays.asList(result), this::convertModelForCategory);
        Mockito.when(client.selectRows(stringArgumentCaptor.capture())).thenReturn(
            CompletableFuture.completedFuture(unversionedRowset));

        Map<Long, YtModelCatalogIndexReader.ModelCounter> modelsAmountByCategory =
            reader.getModelsAmountByCategory(100L);
        String value = stringArgumentCaptor.getValue().getQuery();
        Assertions.assertThat(value).isEqualTo(
            "vendor_id, sum(if(quality = 17693316 , 1, 0)) as models, sum(if(quality != 17693316 , 1 , 0 )) as " +
                "p_models FROM [null] WHERE category_id = 100 AND parent_id = NULL AND current_type in (1,16,11,6) " +
                "GROUP BY vendor_id"
        );
        Assertions.assertThat(modelsAmountByCategory.get(1L).getModels()).isEqualTo(100L);
        Assertions.assertThat(modelsAmountByCategory.get(1L).getPartnerModels()).isEqualTo(2L);
    }

    @Test
    public void testGetModelsAmountByCategoryAndVendor() {
        ArgumentCaptor<SelectRowsRequest> stringArgumentCaptor = ArgumentCaptor.forClass(SelectRowsRequest.class);
        List<Long> result = Arrays.asList(100L, 2L);

        UnversionedRowset unversionedRowset = YtMockingHelper.buildModelStoreResponse(client, rpcApi,
            Arrays.asList(result), this::convertModelForVendorAndCategory);
        Mockito.when(client.selectRows(stringArgumentCaptor.capture())).thenReturn(
            CompletableFuture.completedFuture(unversionedRowset));

        YtModelCatalogIndexReader.ModelCounter modelsAmountByCategory =
            reader.getModelsAmountByCategoryAndVendor(100L, 200L);
        String value = stringArgumentCaptor.getValue().getQuery();
        Assertions.assertThat(value).isEqualTo(
            "sum(if(quality = 17693316 , 1 , 0 )) as models, sum(if(quality != 17693316 , 1 , 0 )) as p_models FROM " +
                "[null] " +
                "WHERE category_id = 100 AND vendor_id = 200 AND " +
                "parent_id = NULL AND current_type in (1,16,11,6)  GROUP BY vendor_id"
        );
        Assertions.assertThat(modelsAmountByCategory.getModels()).isEqualTo(100L);
        Assertions.assertThat(modelsAmountByCategory.getPartnerModels()).isEqualTo(2L);
    }

    private Map<String, UnversionedValue> convertModelForCategory(Integer row, List<Long> rowList) {
        Map<String, UnversionedValue> rowValues = new HashMap<>();
        rowValues.put(YtModelColumns.CATEGORY_ID, buildValue(row, INT64, rowList.get(0)));
        rowValues.put("models", buildValue(row, INT64, rowList.get(1)));
        rowValues.put("p_models", buildValue(row, INT64, rowList.get(2)));
        return rowValues;
    }

    private Map<String, UnversionedValue> convertModelForVendorAndCategory(Integer row, List<Long> rowList) {
        Map<String, UnversionedValue> rowValues = new HashMap<>();
        rowValues.put("models", buildValue(row, INT64, rowList.get(0)));
        rowValues.put("p_models", buildValue(row, INT64, rowList.get(1)));
        return rowValues;
    }


}
