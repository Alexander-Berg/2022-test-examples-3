package ru.yandex.market.billing.marketing;

import java.io.InputStream;
import java.util.Objects;
import java.util.function.Consumer;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableMap;

import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.cypress.Cypress;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.tables.YtTables;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.inside.yt.kosher.ytree.YTreeNode;
import ru.yandex.market.billing.util.yt.YtTestUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.market.billing.util.yt.YtTestUtils.readMultipleYtTables;
import static ru.yandex.market.billing.util.yt.YtUtilTest.treeMapNode;

@SuppressWarnings("checkstyle:all")
@ParametersAreNonnullByDefault
public class PartnerMarketingTestUtil {
    public static Yt mockYt(@Nullable InputStream dataSource) {
        final Yt yt = mock(Yt.class);
        final YtTables tables = mock(YtTables.class);
        when(yt.tables()).thenReturn(tables);
        final Cypress cypress = mock(Cypress.class);
        when(yt.cypress()).thenReturn(cypress);

        if (Objects.isNull(dataSource)) {
            doThrow(new RuntimeException("yt.tables().read() error"))
                    .when(tables).read(any(), any(), any(Consumer.class));

            when(cypress.exists(any(YPath.class))).thenThrow(new RuntimeException("yt.cypress().exists() error"));
        } else {
            var data = readMultipleYtTables(dataSource, PartnerMarketingTestUtil::campaignJsonToYtNode);

            doAnswer(invocation -> {
                YPath yPath = invocation.getArgument(0);
                final Consumer<YTreeMapNode> consumer = invocation.getArgument(2);
                data.get(yPath.toString()).forEach(consumer);
                return null;
            }).when(tables).read(any(), any(), any(Consumer.class));

            doAnswer(invocation -> {
                YPath yPath = invocation.getArgument(0);
                return data.containsKey(yPath.toString());
            }).when(cypress).exists(any(YPath.class));
        }

        return yt;
    }

    private static YTreeMapNode campaignJsonToYtNode(JsonNode jsonNode) {
        var anaplanIdNode = jsonNode.get("anaplan_id");
        var anaplanId = anaplanIdNode.isNull() ? null : anaplanIdNode.longValue();

        return treeMapNode(ImmutableMap.<String, YTreeNode>builder()
                .put("campaign_id", YtTestUtils.intNode(jsonNode.get("campaign_id").longValue()))
                .put("partner_id", YtTestUtils.intNode(jsonNode.get("partner_id").longValue()))
                .put("type", YtTestUtils.stringNode(jsonNode.get("type").asText()))
                .put("sum", YtTestUtils.stringNode(jsonNode.get("sum").asText()))
                .put("start_date", YtTestUtils.stringNode(jsonNode.get("start_date").asText()))
                .put("end_date", YtTestUtils.stringNode(jsonNode.get("end_date").asText()))
                .put("anaplan_id", anaplanId == null ? YtTestUtils.nullNode() : YtTestUtils.intNode(anaplanId))
                .put("currency", YtTestUtils.stringNode(jsonNode.get("currency").asText()))
                .put("nds", YtTestUtils.booleanNode(jsonNode.get("nds").booleanValue()))
                .put("brand_id", YtTestUtils.intNode(jsonNode.get("brand_id").longValue()))
                .put("name", YtTestUtils.stringNode(jsonNode.get("name").asText()))
                .put("business_model", YtTestUtils.stringNode(jsonNode.get("business_model").asText()))
                .put("category_id", YtTestUtils.intNode(jsonNode.get("category_id").longValue()))
                .put("approved_by_manager", YtTestUtils.intNode(jsonNode.get("approved_by_manager").longValue()))
                .put("approved_by_partner", YtTestUtils.intNode(jsonNode.get("approved_by_partner").longValue()))
                .put("date", YtTestUtils.stringNode(jsonNode.get("date").asText()))
                .build()
        );
    }

}
