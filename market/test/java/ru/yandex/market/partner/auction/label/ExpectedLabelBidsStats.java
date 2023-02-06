package ru.yandex.market.partner.auction.label;

/**
 * @author vbudnev
 */

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * Контейнер для ожидаемых значений в тестах {@link LabelCommon#testLabelModificationMethod}
 */
public class ExpectedLabelBidsStats {
    public static final ExpectedLabelBidsStats UPDATE_OK = new ExpectedLabelBidsStats(
            0,
            0,
            1,
            0
    );
    public static final ExpectedLabelBidsStats RECOMMENDED_OK = new ExpectedLabelBidsStats(
            0,
            1,
            0,
            0
    );

    public static final ExpectedLabelBidsStats HAS_WARNINGS = new ExpectedLabelBidsStats(
            1,
            0,
            0,
            0
    );

    public Integer warningCount;
    public Integer recommendedCount;
    public Integer updatedCount;
    public Integer groupOnlyChange;

    public ExpectedLabelBidsStats(
            Integer warningCount,
            Integer recommendedCount,
            Integer updatedCount,
            Integer groupOnlyChange
    ) {
        this.warningCount = warningCount;
        this.recommendedCount = recommendedCount;
        this.updatedCount = updatedCount;
        this.groupOnlyChange = groupOnlyChange;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }

}
