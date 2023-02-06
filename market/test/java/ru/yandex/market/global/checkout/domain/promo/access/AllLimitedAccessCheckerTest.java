package ru.yandex.market.global.checkout.domain.promo.access;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;

import io.github.benas.randombeans.api.EnhancedRandom;
import lombok.RequiredArgsConstructor;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.global.checkout.BaseFunctionalTest;
import ru.yandex.market.global.checkout.domain.promo.apply.first_order_discount.FirstOrderDiscountArgs;
import ru.yandex.market.global.checkout.domain.promo.apply.first_order_discount.FirstOrderDiscountCommonState;
import ru.yandex.market.global.checkout.domain.promo.model.PromoType;
import ru.yandex.market.global.checkout.factory.TestOrderFactory;
import ru.yandex.market.global.checkout.factory.TestPromoFactory;
import ru.yandex.market.global.checkout.util.RandomDataGenerator;
import ru.yandex.market.global.db.jooq.enums.EPromoAccessType;
import ru.yandex.market.global.db.jooq.enums.EPromoApplicationType;
import ru.yandex.market.global.db.jooq.tables.pojos.Promo;
import ru.yandex.market.global.db.jooq.tables.pojos.PromoUser;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class AllLimitedAccessCheckerTest extends BaseFunctionalTest {

    private static final EnhancedRandom RANDOM = RandomDataGenerator
            .dataRandom(AllLimitedAccessCheckerTest.class).build();

    public static int LIMITED_COUNT = 4;

    protected final TestPromoFactory testPromoFactory;
    protected final TestOrderFactory testOrderFactory;
    protected final AllLimitedPromoAccessChecker allLimitedPromoAccessChecker;
    protected final Clock clock;

    private Promo createPromo() {
        return createPromo(OffsetDateTime.now(clock).plusDays(14));
    }

    private Promo createPromoExpired() {
        return createPromo(OffsetDateTime.now(clock).minusDays(1));
    }


    private Promo createPromo(OffsetDateTime validTill) {
        return testPromoFactory.createPromo(TestPromoFactory.CreatePromoBuilder.builder()
                .setupPromo(p -> p
                        .setAccessType(EPromoAccessType.ALL_LIMITED)
                        .setType(PromoType.FIXED_DISCOUNT.name())
                        .setApplicationType(EPromoApplicationType.PROMOCODE)
                        .setLimitedUsagesCount(LIMITED_COUNT)
                        .setValidTill(validTill)
                )
                .setupState(() -> new FirstOrderDiscountCommonState()
                        .setBudgetUsed(0)
                )
                .setupArgs((a) -> new FirstOrderDiscountArgs()
                        .setDiscount(30_00L)
                        .setBudget(1000_00L)
                        .setMinTotalItemsCost(30_00L)
                ).build()
        );
    }

    private PromoUser createPromoSubj(Promo promo, boolean used) {
        return new PromoUser()
                .setValidTill(promo.getValidTill())
                .setPromoId(promo.getId())
                .setUid(RANDOM.nextLong())
                .setUsed(used);
    }

    @Test
    public void testGetUsages() {

        Promo promo = createPromo();

        long usagesLeftCount = allLimitedPromoAccessChecker.getUsagesLeftCount(OffsetDateTime.now(clock), promo,
                List.of());
        Assertions.assertThat(usagesLeftCount).isEqualTo((long) promo.getLimitedUsagesCount());

        usagesLeftCount = allLimitedPromoAccessChecker.getUsagesLeftCount(OffsetDateTime.now(clock), promo,
                List.of(createPromoSubj(promo, true)));
        Assertions.assertThat(usagesLeftCount).isEqualTo(0L);

        usagesLeftCount = allLimitedPromoAccessChecker.getUsagesLeftCount(OffsetDateTime.now(clock), promo,
                List.of(createPromoSubj(promo, false)));
        Assertions.assertThat(usagesLeftCount).isEqualTo(1L);

        usagesLeftCount = allLimitedPromoAccessChecker.getUsagesLeftCount(OffsetDateTime.now(clock), promo,
                List.of(createPromoSubj(promo, false),
                        createPromoSubj(promo, false),
                        createPromoSubj(promo, false)));
        Assertions.assertThat(usagesLeftCount).isEqualTo(3L);

        usagesLeftCount = allLimitedPromoAccessChecker.getUsagesLeftCount(OffsetDateTime.now(clock), promo,
                List.of(createPromoSubj(promo, false),
                        createPromoSubj(promo, false),
                        createPromoSubj(promo, false),
                        createPromoSubj(promo, false),
                        createPromoSubj(promo, false)));
        Assertions.assertThat(usagesLeftCount).isEqualTo(5L);

        usagesLeftCount = allLimitedPromoAccessChecker.getUsagesLeftCount(OffsetDateTime.now(clock), promo,
                List.of(createPromoSubj(promo, false),
                        createPromoSubj(promo, false),
                        createPromoSubj(promo, true),
                        createPromoSubj(promo, true),
                        createPromoSubj(promo, false)));
        Assertions.assertThat(usagesLeftCount).isEqualTo(3L);

    }

    @Test
    public void testGetUsagesExpired() {
        Promo promo = createPromoExpired();

        long usagesLeftCount = allLimitedPromoAccessChecker.getUsagesLeftCount(OffsetDateTime.now(clock), promo,
                List.of());
        Assertions.assertThat(usagesLeftCount).isEqualTo(0L);

        usagesLeftCount = allLimitedPromoAccessChecker.getUsagesLeftCount(OffsetDateTime.now(clock), promo,
                List.of(createPromoSubj(promo, false)));
        Assertions.assertThat(usagesLeftCount).isEqualTo(0L);
    }

}
