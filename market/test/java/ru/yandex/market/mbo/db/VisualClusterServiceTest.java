package ru.yandex.market.mbo.db;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import ru.yandex.market.mbo.db.modelstorage.ModelStorageService;
import ru.yandex.market.mbo.db.modelstorage.index.GenericField;
import ru.yandex.market.mbo.db.modelstorage.index.MboIndexesFilter;
import ru.yandex.market.mbo.db.modelstorage.index.Operation;
import ru.yandex.market.mbo.gwt.models.GwtPair;
import ru.yandex.market.mbo.gwt.models.cluster.SearchClusterFilter;
import ru.yandex.market.mbo.user.AutoUser;

import static org.mockito.Mockito.times;
import static ru.yandex.market.mbo.db.modelstorage.index.GenericField.CHECKED;
import static ru.yandex.market.mbo.db.modelstorage.index.GenericField.CLUSTERIZER_OFFER_COUNT;
import static ru.yandex.market.mbo.db.modelstorage.index.GenericField.CLUSTERIZER_OFFER_IDS;
import static ru.yandex.market.mbo.db.modelstorage.index.GenericField.CREATED_DATE;
import static ru.yandex.market.mbo.db.modelstorage.index.GenericField.CURRENT_TYPE;
import static ru.yandex.market.mbo.db.modelstorage.index.GenericField.DELETED;
import static ru.yandex.market.mbo.db.modelstorage.index.GenericField.MODEL_ID;
import static ru.yandex.market.mbo.db.modelstorage.index.GenericField.SHOP_COUNT;
import static ru.yandex.market.mbo.db.modelstorage.index.GenericField.TITLE;
import static ru.yandex.market.mbo.db.modelstorage.index.GenericField.VENDOR_ID;
import static ru.yandex.market.mbo.db.modelstorage.index.Operation.EQ;
import static ru.yandex.market.mbo.db.modelstorage.index.Operation.FULL_TEXT;
import static ru.yandex.market.mbo.db.modelstorage.index.Operation.GTE;
import static ru.yandex.market.mbo.db.modelstorage.index.Operation.IN;
import static ru.yandex.market.mbo.db.modelstorage.index.Operation.LTE;
import static ru.yandex.market.mbo.db.modelstorage.index.Operation.NOT_EQ;
import static ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel.Source.CLUSTER;

/**
 * @author apluhin
 * @created 12/14/20
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class VisualClusterServiceTest {

    VisualClusterService service;
    TovarTreeForVisualService visualService;
    ModelStorageService modelStorageService;
    AutoUser autoUser;

    @Before
    public void setUp() throws Exception {
        visualService = Mockito.mock(TovarTreeForVisualService.class);
        modelStorageService = Mockito.mock(ModelStorageService.class);
        autoUser = Mockito.mock(AutoUser.class);
        service = new VisualClusterService(
            visualService,
            modelStorageService,
            autoUser
        );
    }

    @Test
    public void testSearchCluster() {
        Instant instantFrom = Instant.now().minusSeconds(3600);
        Instant instantTo = Instant.now();
        Date testFrom = new Date(instantFrom.toEpochMilli());
        Date testTo = new Date(instantTo.toEpochMilli());

        Map<SearchClusterFilter.SortingFiled, Boolean> sort = new LinkedHashMap<>();
        sort.put(SearchClusterFilter.SortingFiled.CLUSTER_ID, true);
        sort.put(SearchClusterFilter.SortingFiled.MODIFIED, false);

        SearchClusterFilter filter = new SearchClusterFilter();
        filter.setChecked(true);
        filter.setGeneratedDate(GwtPair.create(testFrom, testTo));
        filter.setName("test name");
        filter.setClusterIds(Collections.singletonList(1L));
        filter.setVendorIds(Collections.singletonList(2L));
        filter.setOfferIds(Collections.singletonList("offer1"));
        filter.setOfferCount(GwtPair.create(1L, 10L));
        filter.setShopCount(GwtPair.create(5L, 9L));
        filter.setSort(sort);

        service.searchClusters(
            filter, 100, 50
        );

        ArgumentCaptor<MboIndexesFilter> captor = ArgumentCaptor.forClass(MboIndexesFilter.class);
        Mockito.verify(modelStorageService, times(1)).count(captor.capture());

        MboIndexesFilter value = captor.getValue();
        //test by zone in saas
        check(value, TITLE, FULL_TEXT, "test name");
        check(value, DELETED, NOT_EQ, true);
        check(value, MODEL_ID, IN, Arrays.asList(1L));
        check(value, VENDOR_ID, IN, Arrays.asList(2L));
        check(value, CLUSTERIZER_OFFER_IDS, IN, Arrays.asList("offer1"));
        check(value, CLUSTERIZER_OFFER_COUNT, GTE, 1L);
        check(value, CLUSTERIZER_OFFER_COUNT, LTE, 10L);
        check(value, SHOP_COUNT, GTE, 5L);
        check(value, SHOP_COUNT, LTE, 9L);
        check(value, CHECKED, EQ, true);
        check(value, CREATED_DATE, GTE, instantFrom.toEpochMilli());
        check(value, CREATED_DATE, LTE, instantTo.toEpochMilli());
        check(value, CURRENT_TYPE, IN, Collections.singleton(CLUSTER));
    }

    private void check(MboIndexesFilter f, GenericField field, Operation operation, Object value) {
        Assertions.assertThat(f.getOperation(field, operation).get().getValue()).isEqualTo(value);
    }
}
