package ru.yandex.market.aliasmaker.offers.deep_matcher;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.bolts.collection.ListF;
import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.cypress.Cypress;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.tables.YTableEntryType;
import ru.yandex.inside.yt.kosher.tables.YtTables;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.market.aliasmaker.AliasMaker;
import ru.yandex.market.aliasmaker.cache.deep_matcher.DeepMatcherProcessedItemService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author york
 * @since 19.11.2019
 */
@SuppressWarnings("checkstyle:magicnumber")
public class DeepMatcherProcessedItemServiceTest {
    private static final int CATEGORY_ID = 1;
    private static final int CATEGORY_ID1 = 2;

    private Map<Integer, Multimap<Long, String>> processedItems;
    private DeepMatcherProcessedItemService service;

    @Before
    public void init() {
        NamedParameterJdbcTemplate yqlTemplate = mock(NamedParameterJdbcTemplate.class);
        doAnswer(invocation -> {
            Integer categoryId = (Integer) invocation.getArgument(1, Map.class).get("category_id");
            RowCallbackHandler rch = invocation.getArgument(2, RowCallbackHandler.class);
            processedItems.getOrDefault(categoryId, ArrayListMultimap.create()).forEach((modelId, goodId) -> {
                try {
                    ResultSet rs = mock(ResultSet.class);
                    when(rs.getLong(anyString())).thenReturn(modelId);
                    when(rs.getString(anyString())).thenReturn(goodId);
                    rch.processRow(rs);
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            });
            return null;
        }).when(yqlTemplate).query(any(String.class), any(Map.class), any(RowCallbackHandler.class));
        Yt yt = mock(Yt.class);
        Cypress cypress = mock(Cypress.class);
        when(cypress.exists(any(YPath.class))).thenReturn(true);
        when(yt.cypress()).thenReturn(cypress);
        YtTables ytTables = mock(YtTables.class);
        doAnswer(invocation -> {
            ListF listF = invocation.getArgument(2, ListF.class);
            listF.forEach(entry -> {
                YTreeMapNode node = (YTreeMapNode) entry;
                writeToYt(
                        node.getInt(DeepMatcherSuggestColumns.CATEGORY_ID),
                        node.getLong(DeepMatcherSuggestColumns.MODEL_ID),
                        node.getString(DeepMatcherSuggestColumns.CLASSIFIER_GOOD_ID)
                );
            });
            return null;
        }).when(ytTables).write(any(YPath.class), any(YTableEntryType.class), any(ListF.class));
        when(yt.tables()).thenReturn(ytTables);

        service = new DeepMatcherProcessedItemService(
                yt,
                yqlTemplate,
                "//home/mmq"
        );
        processedItems = new HashMap<>();
    }

    @Test
    public void testMultiGoodId() {
        service.writeProcessedItems(Arrays.asList(
                AliasMaker.DeepMatcherProcessedItem.newBuilder()
                        .setCategoryId(CATEGORY_ID)
                        .setModelId(1)
                        .setClassifierGoodId("good1")
                        .setClassifierMagicId("magic1")
                        .build(),
                AliasMaker.DeepMatcherProcessedItem.newBuilder()
                        .setCategoryId(CATEGORY_ID)
                        .setModelId(1)
                        .setClassifierGoodId("good2")
                        .setClassifierMagicId("magic2")
                        .build()
        ));
        service.writeProcessedItems(Arrays.asList(
                AliasMaker.DeepMatcherProcessedItem.newBuilder()
                        .setCategoryId(CATEGORY_ID1)
                        .setModelId(2)
                        .setClassifierGoodId("good5")
                        .setClassifierMagicId("magic5")
                        .build()
        ));
        service.writeProcessedItems(Arrays.asList(
                AliasMaker.DeepMatcherProcessedItem.newBuilder()
                        .setCategoryId(CATEGORY_ID)
                        .setModelId(1)
                        .setClassifierGoodId("good3")
                        .setClassifierMagicId("magic3")
                        .build()
        ));
        assertThat(service.getCategoryProcessedGoodIds(CATEGORY_ID)).containsExactly(
                Maps.immutableEntry(1L, Sets.newHashSet("good1", "good2", "good3"))
        );
        assertThat(service.getCategoryProcessedGoodIds(CATEGORY_ID1)).containsExactly(
                Maps.immutableEntry(2L, Sets.newHashSet("good5"))
        );
    }


    @Test
    public void testWriteBeforeInit() {
        writeToYt(CATEGORY_ID, 1, "good1");
        writeToYt(CATEGORY_ID, 1, "good2");
        writeToYt(CATEGORY_ID, 2, "good1");
        writeToYt(CATEGORY_ID, 2, "good3");
        service.getCategoryProcessedGoodIds(CATEGORY_ID);

        service.writeProcessedItems(Arrays.asList(
                AliasMaker.DeepMatcherProcessedItem.newBuilder()
                        .setCategoryId(CATEGORY_ID)
                        .setModelId(1)
                        .setClassifierGoodId("good5")
                        .setClassifierMagicId("magic5")
                        .build(),
                AliasMaker.DeepMatcherProcessedItem.newBuilder()
                        .setCategoryId(CATEGORY_ID)
                        .setModelId(1)
                        .setClassifierGoodId("good2")
                        .setClassifierMagicId("magic2")
                        .build(),
                AliasMaker.DeepMatcherProcessedItem.newBuilder()
                        .setCategoryId(CATEGORY_ID)
                        .setModelId(3)
                        .setClassifierGoodId("good1")
                        .setClassifierMagicId("magic1")
                        .build()
        ));
        assertThat(service.getCategoryProcessedGoodIds(CATEGORY_ID)).containsExactly(
                Maps.immutableEntry(1L, Sets.newHashSet("good1", "good2", "good5")),
                Maps.immutableEntry(2L, Sets.newHashSet("good1", "good3")),
                Maps.immutableEntry(3L, Sets.newHashSet("good1"))
        );
    }

    private void writeToYt(Integer categoryId, long modelId, String... goodIds) {
        processedItems.computeIfAbsent(categoryId, (x) -> ArrayListMultimap.create())
                .putAll(modelId, Arrays.asList(goodIds));
    }
}
