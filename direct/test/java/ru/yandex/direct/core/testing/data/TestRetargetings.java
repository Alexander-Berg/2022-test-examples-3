package ru.yandex.direct.core.testing.data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.retargeting.model.Retargeting;
import ru.yandex.direct.core.entity.retargeting.model.TargetInterest;

public final class TestRetargetings {
    public static final BigDecimal PRICE_CONTEXT = BigDecimal.valueOf(100);
    public static final int AUTOBUDGET_PRIORITY = 3;

    private TestRetargetings() {
    }

    public static Retargeting defaultRetargeting() {
        return defaultRetargeting(null, null, null);
    }

    public static Retargeting defaultRetargeting(Long campaignId, Long adGroupId, Long retConditionId) {
        return new Retargeting()
                .withCampaignId(campaignId)
                .withAdGroupId(adGroupId)
                .withRetargetingConditionId(retConditionId)
                .withLastChangeTime(LocalDateTime.now().withNano(0))
                .withStatusBsSynced(StatusBsSynced.YES)
                .withAutobudgetPriority(AUTOBUDGET_PRIORITY)
                .withPriceContext(PRICE_CONTEXT)
                .withIsSuspended(false);
    }

    public static TargetInterest defaultTargetInterest() {
        return defaultTargetInterest(null, null, null);
    }

    public static TargetInterest defaultTargetInterest(Long campaignId, Long adGroupId, Long retConditionId) {
        return new TargetInterest()
                .withCampaignId(campaignId)
                .withAdGroupId(adGroupId)
                .withRetargetingConditionId(retConditionId)
                .withLastChangeTime(LocalDateTime.now().withNano(0))
                .withStatusBsSynced(StatusBsSynced.YES)
                .withAutobudgetPriority(AUTOBUDGET_PRIORITY)
                .withPriceContext(PRICE_CONTEXT)
                .withIsSuspended(false);
    }

    public static Retargeting convertToRetargeting(TargetInterest targetInterest) {
        return new Retargeting()
                .withId(targetInterest.getId())
                .withAdGroupId(targetInterest.getAdGroupId())
                .withCampaignId(targetInterest.getCampaignId())
                .withRetargetingConditionId(targetInterest.getRetargetingConditionId())
                .withPriceContext(targetInterest.getPriceContext())
                .withAutobudgetPriority(targetInterest.getAutobudgetPriority())
                .withIsSuspended(targetInterest.getIsSuspended())
                .withStatusBsSynced(targetInterest.getStatusBsSynced())
                .withLastChangeTime(targetInterest.getLastChangeTime());
    }
}
