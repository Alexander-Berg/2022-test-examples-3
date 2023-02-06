package ru.yandex.travel.orders.services.finances.providers;

import java.util.List;

import lombok.experimental.UtilityClass;

import ru.yandex.travel.commons.streams.CustomCollectors;
import ru.yandex.travel.orders.entities.finances.FinancialEvent;
import ru.yandex.travel.orders.entities.finances.FinancialEventType;

import static ru.yandex.travel.testing.misc.TestBaseObjects.rub;

@UtilityClass
public class ProviderTestHelper {
    public static MoneySourceSplit sourcesSplit(Number user, Number plus, Number promo) {
        return sourcesSplit(user, plus, promo, 0);
    }

    public static MoneySourceSplit sourcesSplit(Number user, Number plus, Number promo, Number postPay) {
        return sourcesSplit(user, plus, promo, postPay, 0);
    }

    public static MoneySourceSplit sourcesSplit(Number user, Number plus, Number promo, Number postPay, Number reverseFee) {
        return MoneySourceSplit.builder()
                .user(rub(user))
                .plus(rub(plus))
                .promo(rub(promo))
                .userPostPay(rub(postPay))
                .partnerReverseFee(rub(reverseFee))
                .build();
    }

    public static MoneySplit partnerSplit(Number partner, Number fee) {
        return new MoneySplit(rub(partner), rub(fee));
    }

    public static MoneySplit partnerSplitPostPay(Number partner, Number reverseFee) {
        return new MoneySplit(rub(partner), rub(0), rub(reverseFee));
    }

    public static FullMoneySplit fullSplit(Number userCost, Number userReward, Number promoCost, Number promoReward) {
        return fullSplit(userCost, userReward, 0, 0, promoCost, promoReward);
    }

    public static FullMoneySplit fullSplit(Number userCost, Number userReward,
                                           Number plusCost, Number plusReward,
                                           Number promoCost, Number promoReward) {
        return fullSplit(userCost, userReward, plusCost, plusReward, promoCost, promoReward, 0, 0);
    }

    public static FullMoneySplit fullSplit(Number userCost, Number userReward,
                                           Number plusCost, Number plusReward,
                                           Number promoCost, Number promoReward,
                                           Number userPostPayCost, Number userPostPayReward) {
        return new FullMoneySplit(
                partnerSplit(userCost, userReward),
                partnerSplit(plusCost, plusReward),
                partnerSplit(promoCost, promoReward),
                partnerSplitPostPay(userPostPayCost, userPostPayReward)
        );
    }

    public static FinancialEvent getOnlyEvent(List<FinancialEvent> events, FinancialEventType type) {
        return events.stream()
                .filter(e -> e.getType() == type)
                .collect(CustomCollectors.exactlyOne());
    }
}
