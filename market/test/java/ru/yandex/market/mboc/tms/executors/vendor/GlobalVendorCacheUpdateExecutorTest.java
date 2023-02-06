package ru.yandex.market.mboc.tms.executors.vendor;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.LongNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.common.GUID;
import ru.yandex.inside.yt.kosher.cypress.Cypress;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.impl.ytree.YTreeStringNodeImpl;
import ru.yandex.inside.yt.kosher.tables.CloseableIterator;
import ru.yandex.inside.yt.kosher.tables.YTableEntryTypes;
import ru.yandex.inside.yt.kosher.tables.YtTables;
import ru.yandex.inside.yt.kosher.transactions.YtTransactions;
import ru.yandex.inside.yt.kosher.ytree.YTreeNode;
import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.mbo.http.MboVendors;
import ru.yandex.market.mbo.storage.StorageKeyValueService;
import ru.yandex.market.mboc.common.infrastructure.util.UnstableInit;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;
import ru.yandex.market.mboc.common.vendor.models.CachedGlobalVendor;
import ru.yandex.market.mboc.common.vendor.repository.GlobalVendorsCacheRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static ru.yandex.market.mboc.tms.executors.vendor.GlobalVendorCacheUpdateExecutor.CACHE_ITERATOR_BATCH_SIZE;
import static ru.yandex.market.mboc.tms.executors.vendor.GlobalVendorCacheUpdateExecutor.CREATION_TIME_YT_ATTRIBUTE;

public class GlobalVendorCacheUpdateExecutorTest extends BaseDbTestClass {
    private static final String tablePath = "//home/test/table";

    @Autowired
    public StorageKeyValueService keyValueService;
    @Autowired
    public GlobalVendorsCacheRepository repository;

    private UnstableInit<Yt> ytInit;
    private Yt yt;
    private Cypress cypress;
    private YtTransactions transactions;
    private YtTables tables;
    private YTreeNode tableNode;

    private List<JsonNode> returned;

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() {
        ytInit = (UnstableInit<Yt>) mock(UnstableInit.class);
        yt = mock(Yt.class);
        doReturn(true).when(ytInit).isAvailable();
        doReturn(yt).when(ytInit).get(anyLong(), any(TimeUnit.class));

        cypress = mock(Cypress.class);
        doReturn(cypress).when(yt).cypress();
        tableNode = mock(YTreeNode.class);
        doReturn(tableNode).when(cypress).get(eq(YPath.simple(tablePath)), any(List.class));

        transactions = mock(YtTransactions.class);
        doReturn(transactions).when(yt).transactions();
        doReturn(GUID.create()).when(transactions).start(any(Duration.class));

        tables = mock(YtTables.class);
        doReturn(tables).when(yt).tables();

        doAnswer(invocation -> CloseableIterator.wrap(returned)).when(tables)
            .read(any(Optional.class), eq(false), eq(YPath.simple(tablePath)), eq(YTableEntryTypes.JACKSON));
    }

    @Test
    public void updatingFromYtOnlyWhenCreationTimeIsNewer() {
        keyValueService.putOffsetDateTime(GlobalVendorCacheUpdateExecutor.CREATION_TIME_KEY, OffsetDateTime.now());
        doReturn(new YTreeStringNodeImpl(OffsetDateTime.now().minusSeconds(1).toString(), Map.of()))
            .when(tableNode).getAttributeOrThrow(eq(CREATION_TIME_YT_ATTRIBUTE));
        var exec = new GlobalVendorCacheUpdateExecutor(keyValueService, ytInit, repository, tablePath);

        exec.execute();
        verify(tables, times(0)).read(any(), any());

        doReturn(new YTreeStringNodeImpl(OffsetDateTime.now().plusSeconds(1).toString(), Map.of()))
            .when(tableNode).getAttributeOrThrow(eq(CREATION_TIME_YT_ATTRIBUTE));
        returned = Collections.emptyList();

        exec.execute();
        //noinspection unchecked
        verify(tables, times(1))
            .read(any(Optional.class), eq(false), eq(YPath.simple(tablePath)), same(YTableEntryTypes.JACKSON));
    }

    @Test
    public void updatingFromYt() {
        doReturn(new YTreeStringNodeImpl(OffsetDateTime.now().minusHours(1).toString(), Map.of()))
            .when(tableNode).getAttributeOrThrow(eq(CREATION_TIME_YT_ATTRIBUTE));

        returned = new ArrayList<>();
        var toInsert = new ArrayList<CachedGlobalVendor>();
        for (int i = 0; i < CACHE_ITERATOR_BATCH_SIZE + 100; i++) {
            var json = JsonNodeFactory.instance.objectNode();
            json.set("id", new LongNode(i));
            json.set("data", new TextNode(new String(MboVendors.GlobalVendor.newBuilder()
                .setId(i)
                .addName(MboParameters.Word.newBuilder().setLangId(123).setName("updated name").build())
                .setIsRequireGtinBarcodes((i & 1) == 0)
                .build().toByteArray(), StandardCharsets.ISO_8859_1))
            );
            returned.add(json);

            if (i % 2 == 0) {
                toInsert.add(new CachedGlobalVendor(i, "cached name", LocalDateTime.now()));
            }
        }
        repository.insertBatch(toInsert);

        var exec = new GlobalVendorCacheUpdateExecutor(keyValueService, ytInit, repository, tablePath);
        exec.execute();

        //noinspection unchecked
        verify(tables, times(1))
            .read(any(Optional.class), eq(false), eq(YPath.simple(tablePath)), same(YTableEntryTypes.JACKSON));

        var all = repository.findAllFromIdOrdered(0, 100000);
        assertThat(all).hasSize(CACHE_ITERATOR_BATCH_SIZE + 100);
        for (int i = 0; i < all.size(); i++) {
            var vendor = all.get(i);
            assertThat(vendor.getId()).isEqualTo(i);
            assertThat(vendor.getNames()).containsExactly(new CachedGlobalVendor.Word("updated name", 123));
        }

        verify(transactions, times(1)).start(any(Duration.class));
        verify(transactions, times(1)).commit(any(GUID.class));
    }
}
