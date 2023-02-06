package ru.yandex.market.global.checkout.domain.referral;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.global.checkout.BaseFunctionalTest;
import ru.yandex.market.global.checkout.domain.promo.apply.referral_first_order_discount.ReferralFirstOrderDiscountArgs;
import ru.yandex.market.global.db.jooq.enums.EPromoAccessType;
import ru.yandex.market.global.db.jooq.enums.EPromoApplicationType;
import ru.yandex.market.global.db.jooq.enums.EReferralType;
import ru.yandex.market.global.db.jooq.tables.pojos.Promo;
import ru.yandex.market.global.db.jooq.tables.pojos.Referral;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class ReferralCommandServiceTest extends BaseFunctionalTest {
    private static final long UID = 1L;
    private static final RecursiveComparisonConfiguration.Builder COMPARISON =
            RecursiveComparisonConfiguration.builder()
                    .withIgnoreAllExpectedNullFields(true);
    private final ReferralCommandService referralCommandService;
    private final ReferralQueryService referralQueryService;

    @SneakyThrows
    @Test
    public void testCreateUserPromoFirstOrderDiscount50CreateCorrectPromo() {
        Promo promo = referralCommandService.createUserPromoFirstOrderDiscount50(UID);
        ReferralFirstOrderDiscountArgs promoArgs = (ReferralFirstOrderDiscountArgs) promo.getArgs();

        Referral referral = referralQueryService.get(promoArgs.getReferralId());
        Assertions.assertThat(referral)
                .usingRecursiveComparison(COMPARISON.build())
                .isEqualTo(new Referral()
                        .setType(EReferralType.USER)
                        .setReferredByEntityId(String.valueOf(UID))
                );

        Assertions.assertThat(promoArgs)
                .usingRecursiveComparison(COMPARISON
                        .build()
                )
                .isEqualTo(new ReferralFirstOrderDiscountArgs()
                        .setReferralId(referral.getId())
                        .setReferralUid(UID)
                        .setDiscount(75_00L)
                        .setBudget(1000_000_00L)
                        .setMinTotalItemsCost(100_00L)
                );

        Assertions.assertThat(promo)
                .usingRecursiveComparison(COMPARISON.build())
                .isEqualTo(new Promo()
                        .setApplicationType(EPromoApplicationType.PROMOCODE)
                        .setAccessType(EPromoAccessType.ALL_LIMITED)
                        .setLimitedUsagesCount(1)
                );

        Assertions.assertThat(promo.getTags())
                .containsExactly(promo.getName());
    }
}
