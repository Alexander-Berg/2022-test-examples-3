package ru.yandex.market.mbo.reactui.service.audit.tree;

import org.junit.Test;
import ru.yandex.market.mbo.db.billing.dao.FullPaidEntry;
import ru.yandex.market.mbo.reactui.dto.billing.AuditNode;
import ru.yandex.market.mbo.reactui.dto.billing.components.DecimalText;
import ru.yandex.market.mbo.reactui.dto.billing.components.Text;
import ru.yandex.market.mbo.reactui.service.audit.ColumnDefinition;
import ru.yandex.market.mbo.reactui.service.audit.FormatUtils;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author dergachevfv
 * @since 12/26/19
 */
public class OrphanedPaidEntryNodeMakerTest {

    public static final long OPERATOR_UID = 1L;
    public static final long INSPECTOR_UID = 2L;
    public static final long UNKNOWN_UID = 3L;

    public static final double PRICE = 100D;
    public static final BigDecimal DECIMAL_PRICE = BigDecimal.valueOf(PRICE);

    public static final long OPERATION_ID = 1L;
    public static final String OPERATION_DESCR = "OPERATION_DESCR";
    public static final String UNKNOWN_OPERATION = "<Неизвестное действие>";

    public static final String TITLE = "Действие";
    public static final String ACTION = "Изменение";

    @Test
    public void testWhenOperatorEntry() {
        OrphanedPaidEntryNodeContext context = new OrphanedPaidEntryNodeContext(
            OPERATOR_UID,
            INSPECTOR_UID,
            FullPaidEntry.newBuilder()
                .uid(OPERATOR_UID)
                .price(PRICE)
                .operationDescr(OPERATION_DESCR)
                .build()
        );

        AuditNode.Builder nodeBuilder = new OrphanedPaidEntryNodeMaker().apply(context);

        assertThat(nodeBuilder).isNotNull();
        AuditNode node = nodeBuilder.build();
        assertThat(node.getTitle()).usingRecursiveFieldByFieldElementComparator()
            .containsExactlyInAnyOrder(new Text(TITLE));
        assertThat(node.getData()).containsOnlyKeys(
            ColumnDefinition.OPERATOR_ACTION.name(),
            ColumnDefinition.OPERATOR_CHANGES.name(),
            ColumnDefinition.OPERATOR_PRICE.name()
        );
        assertThat(node.getData().get(ColumnDefinition.OPERATOR_ACTION.name()))
            .usingFieldByFieldElementComparator()
            .containsExactlyInAnyOrder(new Text(ACTION));
        assertThat(node.getData().get(ColumnDefinition.OPERATOR_CHANGES.name()))
            .usingFieldByFieldElementComparator()
            .containsExactlyInAnyOrder(new Text(OPERATION_DESCR));
        assertThat(node.getData().get(ColumnDefinition.OPERATOR_PRICE.name()))
            .usingFieldByFieldElementComparator()
            .containsExactlyInAnyOrder(new DecimalText(DECIMAL_PRICE, FormatUtils::formatPrice));
    }

    @Test
    public void testWhenInspectorEntry() {
        OrphanedPaidEntryNodeContext context = new OrphanedPaidEntryNodeContext(
            OPERATOR_UID,
            INSPECTOR_UID,
            FullPaidEntry.newBuilder()
                .uid(INSPECTOR_UID)
                .price(PRICE)
                .operationDescr(OPERATION_DESCR)
                .build()
        );

        AuditNode.Builder nodeBuilder = new OrphanedPaidEntryNodeMaker().apply(context);

        assertThat(nodeBuilder).isNotNull();
        AuditNode node = nodeBuilder.build();
        assertThat(node.getTitle()).usingRecursiveFieldByFieldElementComparator()
            .containsExactlyInAnyOrder(new Text(TITLE));
        assertThat(node.getData()).containsOnlyKeys(
            ColumnDefinition.INSPECTOR_ACTION.name(),
            ColumnDefinition.INSPECTOR_CHANGES.name(),
            ColumnDefinition.INSPECTOR_PRICE.name()
        );
        assertThat(node.getData().get(ColumnDefinition.INSPECTOR_ACTION.name()))
            .usingFieldByFieldElementComparator()
            .containsExactlyInAnyOrder(new Text(ACTION));
        assertThat(node.getData().get(ColumnDefinition.INSPECTOR_CHANGES.name()))
            .usingFieldByFieldElementComparator()
            .containsExactlyInAnyOrder(new Text(OPERATION_DESCR));
        assertThat(node.getData().get(ColumnDefinition.INSPECTOR_PRICE.name()))
            .usingFieldByFieldElementComparator()
            .containsExactlyInAnyOrder(new DecimalText(DECIMAL_PRICE, FormatUtils::formatPrice));
    }

    @Test
    public void testWhenUnknownUserEntry() {
        OrphanedPaidEntryNodeContext context = new OrphanedPaidEntryNodeContext(
            OPERATOR_UID,
            INSPECTOR_UID,
            FullPaidEntry.newBuilder()
                .uid(UNKNOWN_UID)
                .price(PRICE)
                .operationDescr(OPERATION_DESCR)
                .build()
        );

        AuditNode.Builder nodeBuilder = new OrphanedPaidEntryNodeMaker().apply(context);

        assertThat(nodeBuilder).isNotNull();
        AuditNode node = nodeBuilder.build();
        assertThat(node.getTitle()).usingRecursiveFieldByFieldElementComparator()
            .containsExactlyInAnyOrder(new Text(TITLE, "red"));
        assertThat(node.getData()).containsOnlyKeys(
            ColumnDefinition.OPERATOR_ACTION.name(),
            ColumnDefinition.OPERATOR_CHANGES.name(),
            ColumnDefinition.OPERATOR_PRICE.name()
        );
        assertThat(node.getData().get(ColumnDefinition.OPERATOR_ACTION.name()))
            .usingFieldByFieldElementComparator()
            .containsExactlyInAnyOrder(new Text(ACTION));
        assertThat(node.getData().get(ColumnDefinition.OPERATOR_CHANGES.name()))
            .usingFieldByFieldElementComparator()
            .containsExactlyInAnyOrder(new Text(OPERATION_DESCR));
        assertThat(node.getData().get(ColumnDefinition.OPERATOR_PRICE.name()))
            .usingFieldByFieldElementComparator()
            .containsExactlyInAnyOrder(new DecimalText(DECIMAL_PRICE, FormatUtils::formatPrice));
    }

    @Test
    public void testWhenUnknownOperation() {
        OrphanedPaidEntryNodeContext context = new OrphanedPaidEntryNodeContext(
            OPERATOR_UID,
            INSPECTOR_UID,
            FullPaidEntry.newBuilder()
                .uid(OPERATOR_UID)
                .price(PRICE)
                .operationId(OPERATION_ID)
                .build()
        );

        AuditNode.Builder nodeBuilder = new OrphanedPaidEntryNodeMaker().apply(context);

        assertThat(nodeBuilder).isNotNull();
        AuditNode node = nodeBuilder.build();
        assertThat(node.getTitle()).usingRecursiveFieldByFieldElementComparator()
            .containsExactlyInAnyOrder(new Text(TITLE));
        assertThat(node.getData()).containsOnlyKeys(
            ColumnDefinition.OPERATOR_ACTION.name(),
            ColumnDefinition.OPERATOR_CHANGES.name(),
            ColumnDefinition.OPERATOR_PRICE.name()
        );
        assertThat(node.getData().get(ColumnDefinition.OPERATOR_ACTION.name()))
            .usingFieldByFieldElementComparator()
            .containsExactlyInAnyOrder(new Text(ACTION));
        assertThat(node.getData().get(ColumnDefinition.OPERATOR_CHANGES.name()))
            .usingFieldByFieldElementComparator()
            .containsExactlyInAnyOrder(
                new Text(String.join(" ", UNKNOWN_OPERATION, Long.toString(OPERATION_ID)), "red"));
        assertThat(node.getData().get(ColumnDefinition.OPERATOR_PRICE.name()))
            .usingFieldByFieldElementComparator()
            .containsExactlyInAnyOrder(new DecimalText(DECIMAL_PRICE, FormatUtils::formatPrice));
    }
}
