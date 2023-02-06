package ru.yandex.market.indexer.yt.category;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.collections.CollectionUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.bolts.collection.impl.DefaultMapF;
import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.cypress.Cypress;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.impl.ytree.YTreeEntityNodeImpl;
import ru.yandex.inside.yt.kosher.impl.ytree.YTreeIntegerNodeImpl;
import ru.yandex.inside.yt.kosher.impl.ytree.YTreeMapNodeImpl;
import ru.yandex.inside.yt.kosher.impl.ytree.YTreeStringNodeImpl;
import ru.yandex.inside.yt.kosher.tables.YtTables;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.inside.yt.kosher.ytree.YTreeNode;
import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.environment.ActiveParamService;
import ru.yandex.market.core.indexer.db.meta.GenerationMetaService;
import ru.yandex.market.core.indexer.model.GenerationMeta;
import ru.yandex.market.core.yt.indexer.YtFactory;
import ru.yandex.market.mbi.environment.EnvironmentService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Тесты для {@link IndexerFeedCategoriesImportService}.
 *
 * @author avetokhin 13/10/17.
 */
class IndexerFeedCategoriesImportServiceTest extends FunctionalTest {

    private static final int BATCH_SIZE = 2;
    private static final int CATEGORY_NAME_LENGTH_MAX = 20;

    private static final String STRATOCASTER = "stratocaster";
    private static final String GIBSON = "gibson";
    private static final String CURRENT_IMPORT_INFO = GIBSON + "_" + "20171016_0925";

    private static final String CATEGORIES_PATH_TEMPLATE = "//home/market/testing/indexer/%s/out/shop_categories";

    private static final YTreeNode TREE_NODE_MOCK = mock(YTreeNode.class);

    private static final Map<String, YTreeNode> GIBSON_FILES =
            ImmutableMap.<String, YTreeNode>builder()
                    .put("recent", TREE_NODE_MOCK)
                    .put("20171016_0925", TREE_NODE_MOCK)
                    .put("20171016_0325", TREE_NODE_MOCK)
                    .put("20171015_2125", TREE_NODE_MOCK)
                    .put("20171015_1525", TREE_NODE_MOCK)
                    .build();

    private static final Map<String, YTreeNode> STRATOCASTER_FILES =
            ImmutableMap.<String, YTreeNode>builder()
                    .put("recent", TREE_NODE_MOCK)
                    .put("20171016_0925", TREE_NODE_MOCK)
                    .put("20171016_0325", TREE_NODE_MOCK)
                    .put("20171015_2125", TREE_NODE_MOCK)
                    .put("20171015_1525", TREE_NODE_MOCK)
                    .build();

    private static final YTreeMapNode DATA_1 =
            treeMapNode(ImmutableMap.<String, YTreeNode>builder()
                    .put("feed_id", new YTreeIntegerNodeImpl(true, 100, null))
                    .put("category_id", new YTreeStringNodeImpl("11", null))
                    .put("name", new YTreeStringNodeImpl("name_1", null))
                    .put("parent_id", new YTreeStringNodeImpl("13", null))
                    .build());
    private static final YTreeMapNode DATA_2 =
            treeMapNode(ImmutableMap.<String, YTreeNode>builder()
                    .put("feed_id", new YTreeIntegerNodeImpl(true, 200, null))
                    .put("category_id", new YTreeStringNodeImpl("21", null))
                    .put("name", new YTreeStringNodeImpl("name_2", null))
                    .put("parent_id", new YTreeEntityNodeImpl(null))
                    .build());
    private static final YTreeMapNode DATA_3 =
            treeMapNode(ImmutableMap.<String, YTreeNode>builder()
                    .put("feed_id", new YTreeIntegerNodeImpl(true, 200, null))
                    .put("category_id", new YTreeStringNodeImpl("31", null))
                    .put("name", new YTreeStringNodeImpl("name_1", null))
                    .put("parent_id", new YTreeStringNodeImpl("33333", null))
                    .build());
    private static final YTreeMapNode DATA_4 =
            treeMapNode(ImmutableMap.<String, YTreeNode>builder()
                    .put("feed_id", new YTreeIntegerNodeImpl(true, 200, null))
                    .put("category_id", new YTreeStringNodeImpl("41", null))
                    .put("name", new YTreeStringNodeImpl(
                            "name_2", null))
                    .put("parent_id", new YTreeStringNodeImpl("324232", null))
                    .build());
    private static final YTreeMapNode DATA_5 =
            treeMapNode(ImmutableMap.<String, YTreeNode>builder()
                    .put("feed_id", new YTreeIntegerNodeImpl(true, 200, null))
                    .put("category_id", new YTreeStringNodeImpl("41", null))
                    .put("name", new YTreeStringNodeImpl(
                            "name_2", null))
                    .put("parent_id", new YTreeStringNodeImpl("324232", null))
                    .build());
    private static final YTreeMapNode DATA_6 =
            treeMapNode(ImmutableMap.<String, YTreeNode>builder()
                    .put("feed_id", new YTreeIntegerNodeImpl(true, 300, null))
                    .put("category_id", new YTreeStringNodeImpl("129", null))
                    .put("name", new YTreeStringNodeImpl(
                            "", null))
                    .put("parent_id", new YTreeStringNodeImpl("5444", null))
                    .build());

    @Autowired
    private IndexerFeedCategoryDao indexerFeedCategoryDao;

    private GenerationMetaService generationMetaService;

    @Autowired
    private EnvironmentService environmentService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private YtFactory ytFactory;

    private static YTreeMapNode treeMapNode(final Map<String, YTreeNode> attributes) {
        final YTreeMapNodeImpl entries = new YTreeMapNodeImpl(null);
        attributes.forEach(entries::put);
        return entries;
    }

    private static Yt mockYt(final String indexerType, final Map<String, YTreeNode> files,
                             final List<YTreeMapNode> tableData) {
        final YTreeNode treeNode = mock(YTreeNode.class);
        when(treeNode.asMap()).thenReturn(DefaultMapF.wrap(files));

        final String path = String.format(CATEGORIES_PATH_TEMPLATE, indexerType);
        final Cypress cypress = mock(Cypress.class);
        when(cypress.get(YPath.simple(path))).thenReturn(treeNode);

        final Yt yt = mock(Yt.class);
        when(yt.cypress()).thenReturn(cypress);

        if (CollectionUtils.isNotEmpty(tableData)) {
            final YtTables tables = mock(YtTables.class);
            when(yt.tables()).thenReturn(tables);
            doAnswer(invocation -> {
                final Function<Iterator<YTreeMapNode>, ?> callback = invocation.getArgument(2);
                return callback.apply(tableData.iterator());
            }).when(tables).read(any(), any(), any(Function.class));
        }

        return yt;
    }

    @BeforeEach
    void init() {
        final GenerationMeta generationMeta = mock(GenerationMeta.class);
        when(generationMeta.getMitype()).thenReturn(GIBSON);
        generationMetaService = mock(GenerationMetaService.class);
        when(generationMetaService.getLastFullGeneration(any(), any())).thenReturn(generationMeta);

        final Yt stratocaster = mockYt(STRATOCASTER, STRATOCASTER_FILES, null);
        final Yt gibson = mockYt(GIBSON, GIBSON_FILES, Arrays.asList(DATA_1, DATA_2, DATA_3, DATA_4, DATA_5, DATA_6));

        ytFactory = mock(YtFactory.class);
        when(ytFactory.getYt(STRATOCASTER)).thenReturn(stratocaster);
        when(ytFactory.getYt(GIBSON)).thenReturn(gibson);
        when(ytFactory.getYtCluster(STRATOCASTER)).thenReturn("hahn");
        when(ytFactory.getYtCluster(GIBSON)).thenReturn("hahn");
    }

    /**
     * Проверить флаг игнорирования дубликатов.
     */
    @Test
    @DbUnitDataSet(
            before = "IndexerFeedCategoriesImportServiceTest.before.csv")
    void testImportIgnoreDuplicates() {
        environmentService.setValue(IndexerFeedCategoriesImportService.LAST_IMPORT_DATA_PARAM, "some_values");
        environmentService.setValue(IndexerFeedCategoriesImportService.IGNORE_DUPLICATES, "true");

        final IndexerFeedCategoriesImportService service = createService();

        Assertions.assertThrows(DuplicateKeyException.class, service::importCategories);
    }

    /**
     * Проверить кейз с нормальным импортом категорий.
     */
    @Test
    @DbUnitDataSet(
            before = "IndexerFeedCategoriesImportServiceTest.before.csv",
            after = "IndexerFeedCategoriesImportServiceTest.after.csv")
    void testImport() {
        environmentService.setValue(IndexerFeedCategoriesImportService.LAST_IMPORT_DATA_PARAM, "some_values");
        environmentService.setValue(IndexerFeedCategoriesImportService.IGNORE_DUPLICATES, "false");

        final IndexerFeedCategoriesImportService service = createService();

        service.importCategories();

        Assertions.assertEquals(CURRENT_IMPORT_INFO,
                environmentService.getValue(IndexerFeedCategoriesImportService.LAST_IMPORT_DATA_PARAM, null));
    }

    /**
     * Проверить кейз когда в импорте нет необходимости.
     */
    @Test
    @DbUnitDataSet(
            before = "IndexerFeedCategoriesImportServiceTest.before.csv",
            after = "IndexerFeedCategoriesImportServiceTest.before.csv")
    void testSkip() {
        environmentService.setValue(IndexerFeedCategoriesImportService.LAST_IMPORT_DATA_PARAM, CURRENT_IMPORT_INFO);

        final IndexerFeedCategoriesImportService service = createService();
        service.importCategories();

        Assertions.assertEquals(CURRENT_IMPORT_INFO,
                environmentService.getValue(IndexerFeedCategoriesImportService.LAST_IMPORT_DATA_PARAM, null));
    }

    private IndexerFeedCategoriesImportService createService() {
        return new IndexerFeedCategoriesImportService(
                ytFactory,
                indexerFeedCategoryDao,
                generationMetaService,
                environmentService,
                CATEGORIES_PATH_TEMPLATE,
                new ActiveParamService(jdbcTemplate,
                        "shops_web.feed_categories_1",
                        "shops_web.feed_categories_2",
                        "shops_web.feed_categories_pointer"),
                BATCH_SIZE,
                CATEGORY_NAME_LENGTH_MAX
        );
    }
}
