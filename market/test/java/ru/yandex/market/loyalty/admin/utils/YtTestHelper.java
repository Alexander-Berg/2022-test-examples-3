package ru.yandex.market.loyalty.admin.utils;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import com.google.common.collect.ImmutableMap;
import org.hamcrest.Matcher;
import org.mockito.ArgumentMatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Component;

import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.common.GUID;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.impl.ytree.YTreeMapNodeImpl;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.inside.yt.kosher.operations.Operation;
import ru.yandex.inside.yt.kosher.operations.OperationStatus;
import ru.yandex.inside.yt.kosher.operations.specs.MergeSpec;
import ru.yandex.inside.yt.kosher.ytree.YTreeNode;
import ru.yandex.market.loyalty.admin.service.bunch.generator.AbstractFileBasedBunchGenerator.BatchFileCountingConsumer;
import ru.yandex.market.loyalty.admin.yt.YtClient;
import ru.yandex.market.loyalty.api.model.promogroup.PromoGroupType;
import ru.yandex.market.loyalty.core.config.YtHahn;
import ru.yandex.market.loyalty.core.model.coin.BunchGenerationRequest;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.model.promogroup.PromoGroupImpl;
import ru.yandex.market.loyalty.core.model.promogroup.PromoGroupPromo;
import ru.yandex.market.loyalty.core.model.wallet.YandexWalletNewTransaction;
import ru.yandex.market.loyalty.core.service.coin.CoinService;
import ru.yandex.market.loyalty.core.service.promogroup.PromoGroupService;

import static java.util.Collections.singletonList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.inside.yt.kosher.operations.OperationStatus.COMPLETED;
import static ru.yandex.market.loyalty.admin.utils.files.YtInputFilesValidator.MODIFICATION_TIME_ATTRIBUTE;
import static ru.yandex.market.loyalty.admin.utils.files.YtInputFilesValidator.ROW_COUNT_ATTRIBUTE;
import static ru.yandex.market.loyalty.core.dao.coin.BunchGenerationRequestParamName.ERROR_OUTPUT_TABLE;
import static ru.yandex.market.loyalty.core.dao.coin.BunchGenerationRequestParamName.OUTPUT_TABLE;
import static ru.yandex.qe.yt.cypress.objects.CypressObjectType.FOLDER;
import static ru.yandex.qe.yt.cypress.objects.CypressObjectType.TABLE;

@Component
public class YtTestHelper {
    public static final GUID DEFAULT_GUID = GUID.valueOf("1-8e9c4f69-a3bc6964-3e99ab10");
    public static final YPath DEFAULT_DEREF_PATH = YPath.simple("//some/test/path");
    @Autowired
    @YtHahn
    private JdbcTemplate yqlJdbcTemplate;
    @Autowired
    private CoinService coinService;
    @Autowired
    private PromoGroupService promoGroupService;
    @Autowired
    @YtHahn
    private Yt yt;

    @Autowired
    private YPath basePath;

    public static YPathToName ytPathToName(Matcher<String> matcher) {
        return new YPathToName(matcher);
    }

    public void mockYtInputTableReads(YPath path, List<Long> uids) {
        doAnswer(invocation -> {
            feedUidsToConsumer(uids, invocation.getArgument(2));
            return null;
        })
                .when(yqlJdbcTemplate)
                .query(
                        contains(path.toString()),
                        any(Object[].class),
                        any(RowCallbackHandler.class)
                );

        doAnswer(invocation -> {
            feedUidsToConsumer(uids, invocation.getArgument(1));
            return null;
        })
                .when(yqlJdbcTemplate)
                .query(
                        contains(path.toString()),
                        any(RowCallbackHandler.class)
                );
    }

    public static void mockYtTableExistence(
            YtClient ytClient, boolean value, YPath ytPath
    ) {
        when(ytClient.exists(ytPath)).thenReturn(value);
    }

    public static void mockYtClientDereferenceLink(YtClient ytClient) {
        doAnswer(invocation -> DEFAULT_DEREF_PATH)
                .when(ytClient)
                .dereferenceLink(any(), any());
    }

    @SafeVarargs
    public static void mockYtClientAttributes(
            YtClient ytClient,
            YPath path,
            Consumer<Map<String, YTreeNode>>... attributeBuilders
    ) {
        var attributes = new HashMap<String, YTreeNode>();
        Arrays.stream(attributeBuilders).forEach(c -> c.accept(attributes));
        doAnswer(invocation -> attributes)
                .when(ytClient)
                .getAttributes(path);
    }

    public static Consumer<Map<String, YTreeNode>> modificationTimeAttribute(Instant modificationTime) {
        return map -> map.put(MODIFICATION_TIME_ATTRIBUTE, YTree.builder().value(modificationTime.toString()).build());
    }

    public static Consumer<Map<String, YTreeNode>> rowCountAttribute(long rowCount) {
        return map -> map.put(ROW_COUNT_ATTRIBUTE, YTree.builder().value(rowCount).build());
    }

    public static void mockYtObjectsExistence(
            YtClient ytClient, BunchGenerationRequest request
    ) {
        mockYtTableExistence(ytClient, false, YPath.simple(request
                .getParam(OUTPUT_TABLE)
                .orElseThrow(IllegalArgumentException::new)));
        mockYtTableExistence(ytClient, false, YPath
                .simple(request
                        .getParam(ERROR_OUTPUT_TABLE)
                        .orElseThrow(IllegalArgumentException::new)));
    }

    private String getTableName(String relativePath) {
        return basePath.child(relativePath).toString();
    }

    public void mockYtCoinExportedTablesIds(JdbcTemplate jdbcTemplate, Promo promo) {
        when(
                jdbcTemplate.queryForObject(
                        eq("SELECT MAX(id) FROM `" + getTableName("internal/coin/current") + '`'),
                        eq(Long.class)
                )
        )
                .thenReturn(coinService.search.getMaxCoinId(promo.getId()));
        when(
                jdbcTemplate.queryForObject(
                        eq("SELECT MAX(id) FROM `" + getTableName("internal/coin_props/current") + '`'),
                        eq(Long.class)
                )
        )
                .thenReturn(coinService.search.getMaxCoinPropsId(promo.getId()));
        when(
                jdbcTemplate.queryForObject(
                        eq("SELECT MAX(id) FROM `" + getTableName("internal/coin_description/current") + '`'),
                        eq(Long.class)
                )
        )
                .thenReturn(coinService.search.getMaxCoinDescriptionId(promo.getId()));
    }


    public void mockYtClientAttributes(YPath path, Map<String, YTreeNode> type) {
        when(yt.cypress().get(eq(path.allAttributes())))
                .thenReturn(new YTreeMapNodeImpl(type, Map.of()));
    }

    public void mockYtClientAttributes(ArgumentMatcher<YPath> matcher, Map<String, YTreeNode> type) {
        when(yt.cypress().get(argThat(matcher)))
                .thenReturn(new YTreeMapNodeImpl(type, Map.of()));
    }

    @SuppressWarnings("unchecked")
    public void mockYtMergeOperation() {
        when(yt.operations().merge(any(Optional.class), anyBoolean(), any(MergeSpec.class)))
                .thenReturn(DEFAULT_GUID);
        when(yt.operations().getOperation(any(GUID.class))).thenReturn(new OperationMock(DEFAULT_GUID, COMPLETED));
        when(yt.transactions().start(any(Optional.class), anyBoolean(), any(Duration.class), any(Map.class)))
                .thenReturn(DEFAULT_GUID);

    }

    @SuppressWarnings("unchecked")
    public void mockYtClientList(YPath path, String node) {
        when(yt.cypress().list(any(Optional.class), eq(false), eq(path)))
                .thenReturn(singletonList(node));
    }

    @SuppressWarnings("SameParameterValue")
    public void mockYtInputTable(String basePath, String path, List<Long> data) {
        if (!path.startsWith(basePath) || path.endsWith("/")) {
            throw new IllegalArgumentException();
        }

        String relativePath = path.substring(basePath.length());

        if (relativePath.startsWith("/")) {
            relativePath = relativePath.substring(1);
        }

        final String[] parts = relativePath.split("/");

        YPath yPath = YPath.simple(basePath);

        for (int i = 0; i < parts.length; i++) {
            mockYtClientList(yPath, parts[i]);
            mockYtClientAttributes(yPath, ImmutableMap.of("type",
                    YTree.builder().value(FOLDER.getYtTypeName()).build()));
            yPath = yPath.child(parts[i]);
            if (i == parts.length - 1) {
                mockYtClientAttributes(
                        yPath,
                        ImmutableMap.<String, YTreeNode>builder()
                                .put("type", YTree.builder().value(TABLE.getYtTypeName()).build())
                                .put("sorted", YTree.builder().value(true).build())
                                .put("dynamic", YTree.builder().value(false).build())
                                .put("row_count", YTree.builder().value(data.size()).build())
                                .put("schema", new YTreeMapNodeImpl(Map.of("strict", YTree.booleanNode(true)),
                                        Map.of()))
                                .build()
                );
            }
        }
    }


    @SuppressWarnings("rawtypes")
    private static void feedUidsToConsumer(List<Long> uids, RowCallbackHandler callbackHandler) throws SQLException {
        BatchFileCountingConsumer c = (BatchFileCountingConsumer) callbackHandler;
        for (Long uid : uids.subList(c.getProcessedCount(), Math.min(uids.size(),
                c.getProcessedCount() + c.getBatchSize()))) {
            ResultSet resultSet = mock(ResultSet.class);
            when(resultSet.getLong(eq("uid"))).thenReturn(uid);
            callbackHandler.processRow(resultSet);
        }
    }

    public void setupMergeTagForPromo(Promo promo, String mergeTag) {
        final PromoGroupImpl promoGroup = new PromoGroupImpl(
                PromoGroupType.YANDEX_PLUS, mergeTag,
                LocalDateTime.now(),
                LocalDateTime.now().plus(1, ChronoUnit.DAYS),
                "test", null
        );
        final long promoGroupId = promoGroupService.insertPromoGroupAndGetPromoGroupId(promoGroup);
        promoGroupService.replacePromoGroupPromos(promoGroupId,
                Collections.singletonList(new PromoGroupPromo(promoGroupId, promo.getId(), 0))
        );
    }

    public static List<YandexWalletNewTransaction> createWalletTopUps(int size) {
        return LongStream.range(0, size)
                .mapToObj(uid -> new YandexWalletNewTransaction(
                        uid,
                        BigDecimal.valueOf(100),
                        Long.toString(uid),
                        "{\"is_employee\": \"true\",\"product_id\": \"product\"}",
                        "product",
                        null
                ))
                .collect(Collectors.toList());
    }

    private static class OperationMock implements Operation {

        private final GUID guid;
        private final OperationStatus status;

        public OperationMock(GUID guid, OperationStatus status) {
            this.guid = guid;
            this.status = status;
        }

        @Override
        public GUID getId() {
            return guid;
        }

        @Override
        public OperationStatus getStatus() {
            return status;
        }

        @Override
        public YTreeNode getResult() {
            return null;
        }

        @Override
        public void await() {

        }

        @Override
        public void await(Duration timeout) {

        }

        @Override
        public void abort() {

        }
    }
}
