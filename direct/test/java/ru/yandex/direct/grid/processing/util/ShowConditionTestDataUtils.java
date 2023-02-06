package ru.yandex.direct.grid.processing.util;

import javax.annotation.ParametersAreNonnullByDefault;

import ru.yandex.direct.grid.core.entity.model.GdiEntityStats;
import ru.yandex.direct.grid.core.entity.showcondition.model.GdiShowCondition;
import ru.yandex.direct.grid.core.entity.showcondition.model.GdiShowConditionType;
import ru.yandex.direct.grid.core.util.stats.GridStatNew;
import ru.yandex.direct.grid.processing.model.showcondition.GdShowConditionFilter;
import ru.yandex.direct.grid.processing.model.showcondition.GdShowConditionsContainer;
import ru.yandex.direct.test.utils.RandomNumberUtils;

import static java.util.Collections.emptyList;
import static ru.yandex.direct.grid.processing.util.InputTestDataUtils.getDefaultLimitOffset;
import static ru.yandex.direct.grid.processing.util.InputTestDataUtils.getDefaultStatRequirements;

@ParametersAreNonnullByDefault
public class ShowConditionTestDataUtils {

    public static GdShowConditionsContainer getDefaultGdShowConditionsContainer() {
        return new GdShowConditionsContainer()
                .withFilter(new GdShowConditionFilter())
                .withOrderBy(emptyList())
                .withStatRequirements(getDefaultStatRequirements())
                .withLimitOffset(getDefaultLimitOffset());
    }

    public static GdiShowCondition defaultGdiShowCondition() {
        return new GdiShowCondition()
                .withId(RandomNumberUtils.nextPositiveLong())
                .withGroupId(RandomNumberUtils.nextPositiveLong())
                .withCampaignId(RandomNumberUtils.nextPositiveLong())
                .withPrice(RandomNumberUtils.nextPositiveBigDecimal())
                .withPriceContext(RandomNumberUtils.nextPositiveBigDecimal())
                .withArchived(false)
                .withDeleted(false)
                .withSuspended(false)
                .withType(GdiShowConditionType.KEYWORD)
                .withStat(GridStatNew.addZeros(new GdiEntityStats()));
    }

}
