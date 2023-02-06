package ru.yandex.market.mbo.reactui.service.audit;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import ru.yandex.market.mbo.reactui.dto.billing.AuditNode;
import ru.yandex.market.mbo.reactui.dto.billing.components.BaseComponent;
import ru.yandex.market.mbo.reactui.dto.billing.components.DecimalText;
import ru.yandex.market.mbo.reactui.dto.billing.components.Text;
import ru.yandex.market.mbo.reactui.service.audit.tree.TreeDepthFirstVisitor;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author dergachevfv
 * @since 12/25/19
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class AuditCountsAggregationHelperTest {

    @Test
    public void testAggregateAndFillCounts() {
        //   item 1
        //     item 11 action = test
        //             price = 1
        //     item 12
        //       item 121 action = test
        //                price = 2
        //       item 122 action = test
        //                price = 3
        AuditNode.Builder tree =
            AuditNode.newBuilder().title(title("1"))
                .addItems(Arrays.asList(
                    AuditNode.newBuilder().title(title("11"))
                        .addData(ColumnDefinition.OPERATOR_ACTION.name(), stub())
                        .addData(ColumnDefinition.OPERATOR_PRICE.name(), floating(1)),
                    AuditNode.newBuilder().title(title("12"))
                        .addItems(Arrays.asList(
                            AuditNode.newBuilder().title(title("121"))
                                .addData(ColumnDefinition.OPERATOR_ACTION.name(), stub())
                                .addData(ColumnDefinition.OPERATOR_PRICE.name(), floating(2)),
                            AuditNode.newBuilder().title(title("122"))
                                .addData(ColumnDefinition.OPERATOR_ACTION.name(), stub())
                                .addData(ColumnDefinition.OPERATOR_PRICE.name(), floating(3))
                        )),
                    AuditNode.newBuilder().title(title("13"))
                        .addData(ColumnDefinition.OPERATOR_ACTION.name(), stub())
                        .addData(ColumnDefinition.OPERATOR_PRICE.name(), floating(4))
                        .setIsAggregated(false)
                ));

        AuditCountsAggregationHelper.aggregateAndFillCounts(tree);

        TreeDepthFirstVisitor<AuditNode.Builder> checkAndFilter = node -> {
            Map<String, List<BaseComponent>> data = node.getData();

            if (node.hasChildren()) {
                // parent nodes shoul contain aggregates for all columns
                List<String> allColumns = Stream.of(ColumnDefinition.values())
                    .map(ColumnDefinition::name)
                    .collect(Collectors.toList());
                Assertions.assertThat(data.keySet())
                    .containsExactlyInAnyOrderElementsOf(allColumns);
            }

            Map<String, List<BaseComponent>> filteredData = data.entrySet().stream()
                .filter(e -> ColumnDefinition.OPERATOR_ACTION.name().equals(e.getKey())
                    || ColumnDefinition.OPERATOR_PRICE.name().equals(e.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            node.setAllData(filteredData);
        };
        checkAndFilter.visit(tree);

        //   expected:
        //   item 1 action = 3 - aggregated
        //          price = 6 - aggregated
        //     item 11 action = test
        //             price = 1
        //     item 12 action = 2 - aggregated
        //             price = 5 - aggregated
        //       item 121 action = test
        //                price = 2
        //       item 122 action = test
        //                price = 3
        Assertions.assertThat(tree)
            .isEqualToComparingFieldByFieldRecursively(
                AuditNode.newBuilder().title(title("1"))
                    .addData(ColumnDefinition.OPERATOR_ACTION.name(), count(3))
                    .addData(ColumnDefinition.OPERATOR_PRICE.name(), sum(6))
                    .addItems(Arrays.asList(
                        AuditNode.newBuilder().title(title("11"))
                            .addData(ColumnDefinition.OPERATOR_ACTION.name(), stub())
                            .addData(ColumnDefinition.OPERATOR_PRICE.name(), floating(1)),
                        AuditNode.newBuilder().title(title("12"))
                            .addData(ColumnDefinition.OPERATOR_ACTION.name(), count(2))
                            .addData(ColumnDefinition.OPERATOR_PRICE.name(), sum(5))
                            .addItems(Arrays.asList(
                                AuditNode.newBuilder().title(title("121"))
                                    .addData(ColumnDefinition.OPERATOR_ACTION.name(), stub())
                                    .addData(ColumnDefinition.OPERATOR_PRICE.name(), floating(2)),
                                AuditNode.newBuilder().title(title("122"))
                                    .addData(ColumnDefinition.OPERATOR_ACTION.name(), stub())
                                    .addData(ColumnDefinition.OPERATOR_PRICE.name(), floating(3))
                            )),
                        AuditNode.newBuilder().title(title("13"))
                            .addData(ColumnDefinition.OPERATOR_ACTION.name(), stub())
                            .addData(ColumnDefinition.OPERATOR_PRICE.name(), floating(4))
                            .setIsAggregated(false)
                    ))
            );
    }

    private BaseComponent count(int v) {
        return new DecimalText(BigDecimal.valueOf(v), toIntString(v) + " шт.");
    }

    private BaseComponent sum(int v) {
        return new DecimalText(BigDecimal.valueOf(v), toFloatingString(v) + " р.");
    }

    private static String toFloatingString(int value) {
        return new DecimalFormat("0.00").format(value);
    }

    private static String toIntString(int value) {
        return new DecimalFormat("0").format(value);
    }

    private BaseComponent floating(int v) {
        return new DecimalText(BigDecimal.valueOf(v), FormatUtils::formatPrice);
    }

    private BaseComponent stub() {
        return new Text("test");
    }

    private BaseComponent title(String title) {
        return new Text(title);
    }
}
