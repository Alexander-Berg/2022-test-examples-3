package ru.yandex.direct.core.entity.adgroup.service.complex.text.update.targetinterest;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.core.entity.adgroup.service.complex.text.update.ComplexAdGroupUpdateOperationTestBase;
import ru.yandex.direct.core.entity.retargeting.model.Retargeting;
import ru.yandex.direct.core.entity.retargeting.model.TargetInterest;
import ru.yandex.direct.core.entity.retargeting.service.RetargetingService;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.RetConditionInfo;
import ru.yandex.direct.core.testing.info.RetargetingInfo;
import ru.yandex.direct.currency.Currencies;
import ru.yandex.direct.currency.Currency;
import ru.yandex.direct.currency.CurrencyCode;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.RandomUtils.nextLong;
import static ru.yandex.direct.core.entity.retargeting.service.RetargetingUtils.convertRetargetingsToTargetInterests;
import static ru.yandex.direct.core.testing.data.TestRetargetings.defaultRetargeting;

public class ComplexUpdateTargetInterestTestBase extends ComplexAdGroupUpdateOperationTestBase {

    protected static final Currency CURRENCY = Currencies.getCurrency(CurrencyCode.RUB);

    @Autowired
    private RetargetingService retargetingService;

    protected RetargetingInfo createRetargeting(AdGroupInfo adGroupInfo, RetConditionInfo retConditionInfo) {
        RetargetingInfo retargetingInfo = new RetargetingInfo()
                .withAdGroupInfo(adGroupInfo)
                .withRetConditionInfo(retConditionInfo)
                .withRetargeting(defaultRetargeting().withPriceContext(BigDecimal.TEN));
        return steps.retargetingSteps().createRetargeting(retargetingInfo);
    }

    protected RetConditionInfo createRandomRetCondition() {
        return steps.retConditionSteps().createBigRetCondition(campaignInfo.getClientInfo());
    }

    protected TargetInterest getRetargetingWithUpdatedPrice(RetargetingInfo sourceRetargetingInfo) {
        Long newPrice = sourceRetargetingInfo.getRetargeting().getPriceContext().longValue() + 10;
        Retargeting retargeting = new Retargeting()
                .withId(sourceRetargetingInfo.getRetargetingId())
                .withRetargetingConditionId(sourceRetargetingInfo.getRetConditionId())
                .withPriceContext(BigDecimal.valueOf(newPrice));
        return convertRetargetingsToTargetInterests(singletonList(retargeting), emptyList()).get(0);
    }

    protected TargetInterest randomPriceRetargeting(RetConditionInfo retConditionInfo) {
        Retargeting retargeting = defaultRetargeting(null, null, retConditionInfo.getRetConditionId())
                .withPriceContext(BigDecimal.valueOf(nextLong(100, CURRENCY.getMaxPrice().longValue())));
        return convertRetargetingsToTargetInterests(singletonList(retargeting), emptyList()).get(0);
    }

    protected List<TargetInterest> findTargetInterestsInAdGroup(AdGroupInfo adGroupInfo) {
        return retargetingService.getTargetInterestsWithInterestByAdGroupIds(
                singletonList(adGroupInfo.getAdGroupId()), clientId, shard);
    }
}
