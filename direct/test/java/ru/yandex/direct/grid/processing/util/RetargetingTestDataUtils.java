package ru.yandex.direct.grid.processing.util;

import javax.annotation.ParametersAreNonnullByDefault;

import ru.yandex.direct.grid.core.entity.showcondition.model.GdiBidsRetargeting;
import ru.yandex.direct.grid.core.entity.showcondition.model.GdiBidsRetargetingStatusBsSynced;
import ru.yandex.direct.grid.core.entity.showcondition.model.GdiShowConditionAutobudgetPriority;
import ru.yandex.direct.grid.processing.model.retargeting.GdRetargeting;
import ru.yandex.direct.grid.processing.model.retargeting.GdRetargetingAccess;
import ru.yandex.direct.grid.processing.model.retargeting.GdRetargetingFilter;
import ru.yandex.direct.grid.processing.model.retargeting.GdRetargetingsContainer;
import ru.yandex.direct.grid.processing.model.showcondition.GdShowConditionAutobudgetPriority;
import ru.yandex.direct.test.utils.RandomNumberUtils;

import static java.util.Collections.emptyList;
import static ru.yandex.direct.grid.processing.util.AdGroupTestDataUtils.defaultGdBaseGroup;
import static ru.yandex.direct.grid.processing.util.InputTestDataUtils.getDefaultLimitOffset;
import static ru.yandex.direct.grid.processing.util.InputTestDataUtils.getDefaultStatRequirements;

@ParametersAreNonnullByDefault
public class RetargetingTestDataUtils {

    public static GdRetargetingsContainer getDefaultGdRetargetingsContainer() {
        return new GdRetargetingsContainer()
                .withFilter(new GdRetargetingFilter())
                .withOrderBy(emptyList())
                .withStatRequirements(getDefaultStatRequirements())
                .withLimitOffset(getDefaultLimitOffset());
    }

    public static GdRetargeting defaultGdRetargeting() {
        return new GdRetargeting()
                .withRetargetingId(RandomNumberUtils.nextPositiveLong())
                .withRetargetingConditionId(RandomNumberUtils.nextPositiveLong())
                .withAdGroupId(RandomNumberUtils.nextPositiveLong())
                .withCampaignId(RandomNumberUtils.nextPositiveLong())
                .withPriceContext(RandomNumberUtils.nextPositiveBigDecimal())
                .withAutoBudgetPriority(GdShowConditionAutobudgetPriority.LOW)
                .withReach(RandomNumberUtils.nextPositiveLong())
                .withIsSuspended(false)
                .withAdGroup(defaultGdBaseGroup())
                .withAccess(new GdRetargetingAccess()
                        .withCanEdit(true));
    }

    public static GdiBidsRetargeting defaultGdiRetargeting() {
        return new GdiBidsRetargeting()
                .withRetargetingId(RandomNumberUtils.nextPositiveLong())
                .withRetargetingConditionId(RandomNumberUtils.nextPositiveLong())
                .withAdGroupId(RandomNumberUtils.nextPositiveLong())
                .withCampaignId(RandomNumberUtils.nextPositiveLong())
                .withAdId(RandomNumberUtils.nextPositiveLong())
                .withPriceContext(RandomNumberUtils.nextPositiveBigDecimal())
                .withStatusBsSynced(GdiBidsRetargetingStatusBsSynced.NO)
                .withIsSuspended(false)
                .withReach(RandomNumberUtils.nextPositiveLong())
                .withAutoBudgetPriority(GdiShowConditionAutobudgetPriority.LOW);
    }
}
