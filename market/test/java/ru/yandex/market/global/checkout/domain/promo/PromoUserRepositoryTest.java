package ru.yandex.market.global.checkout.domain.promo;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;

import io.github.benas.randombeans.api.EnhancedRandom;
import lombok.RequiredArgsConstructor;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.global.checkout.BaseFunctionalTest;
import ru.yandex.market.global.checkout.domain.order.OrderModel;
import ru.yandex.market.global.checkout.domain.promo.model.PromoWithUser;
import ru.yandex.market.global.checkout.factory.TestOrderFactory;
import ru.yandex.market.global.checkout.factory.TestPromoFactory;
import ru.yandex.market.global.checkout.util.RandomDataGenerator;
import ru.yandex.market.global.db.jooq.enums.EPromoAccessType;
import ru.yandex.market.global.db.jooq.enums.EPromoCommunicationType;
import ru.yandex.market.global.db.jooq.tables.pojos.Promo;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class PromoUserRepositoryTest extends BaseFunctionalTest {

    private static final EnhancedRandom RANDOM = RandomDataGenerator.dataRandom(PromoUserRepositoryTest.class).build();

    private final PromoRepository repository;
    private final TestPromoFactory testPromoFactory;
    private final TestOrderFactory testOrderFactory;
    private final Clock clock;

    private Promo createPromoIssued(OffsetDateTime from, OffsetDateTime to, boolean used,
                                    EPromoCommunicationType[] communicationTypes) {
        return createPromo(EPromoAccessType.ISSUED, from, to, used, communicationTypes);
    }

    private Promo createPromo(EPromoAccessType accessType, OffsetDateTime from, OffsetDateTime to, boolean used,
                                            EPromoCommunicationType[] communicationTypes) {
        OrderModel order = used ? testOrderFactory.createOrder() : null;
        Promo promo = testPromoFactory.createPromo(TestPromoFactory.CreatePromoBuilder.builder()
                .setupPromo(it -> it
                        .setAccessType(accessType)
                        .setValidFrom(from)
                        .setValidTill(to)
                )
                .setupCommunicationTypes(it -> communicationTypes)
                .build());
        testPromoFactory.createUsageUserRecord(it -> it
                .setPromoId(promo.getId())
                .setUsed(used)
                .setOrderId(order != null ? order.getOrder().getId() : it.getOrderId())
                .setUid(RANDOM.nextLong())
                .setValidTill(promo.getValidTill()));
        return promo;
    }

    private Promo createUnusedPromoIssuedWithPush(OffsetDateTime from, OffsetDateTime to) {
        return createPromoIssued(from, to, false, new EPromoCommunicationType[] {EPromoCommunicationType.PUSH});
    }

    @Test
    public void testAll() {
        Promo promo = createUnusedPromoIssuedWithPush(OffsetDateTime.now(clock).minusDays(20),
                OffsetDateTime.now(clock).plusHours(30));

        List<PromoWithUser> almostExpiredIssuedPromos = getAlmostExpiredIssuedPromosForUser(2,
                OffsetDateTime.now(clock));

        Assertions.assertThat(almostExpiredIssuedPromos).anyMatch(it -> promo.getId().equals(it.getPromo().getId()));
    }

    private List<PromoWithUser> getAlmostExpiredIssuedPromosForUser(int days, OffsetDateTime now) {
        return repository.getIssuedPromosExpiredBetween(now.plusDays(days - 1),
                now.plusDays(days));
    }

    @Test
    public void testFarExpired() {
        Promo promo = createUnusedPromoIssuedWithPush(OffsetDateTime.now(clock).minusDays(20),
                OffsetDateTime.now(clock).plusHours(60));

        List<PromoWithUser> almostExpiredIssuedPromos = getAlmostExpiredIssuedPromosForUser(2,
                OffsetDateTime.now(clock));

        Assertions.assertThat(almostExpiredIssuedPromos).noneMatch(it -> promo.getId().equals(it.getPromo().getId()));
    }

    @Test
    public void testNotStarted() {

        Promo promo = createUnusedPromoIssuedWithPush(OffsetDateTime.now(clock).plusHours(1),
                OffsetDateTime.now(clock).plusHours(30));

        List<PromoWithUser> almostExpiredIssuedPromos = getAlmostExpiredIssuedPromosForUser(2,
                OffsetDateTime.now(clock));

        Assertions.assertThat(almostExpiredIssuedPromos).noneMatch(it -> promo.getId().equals(it.getPromo().getId()));
    }

    @Test
    public void testUsed() {

        Promo promo = createPromoIssued(OffsetDateTime.now(clock).minusDays(1),
                OffsetDateTime.now(clock).plusHours(30),
                true,
                new EPromoCommunicationType[] {EPromoCommunicationType.PUSH} );

        List<PromoWithUser> almostExpiredIssuedPromos = getAlmostExpiredIssuedPromosForUser(2,
                OffsetDateTime.now(clock));

        Assertions.assertThat(almostExpiredIssuedPromos).noneMatch(it -> promo.getId().equals(it.getPromo().getId()));

    }

    @Test
    public void testAllTypes() {

        Promo promo = createPromoIssued(OffsetDateTime.now(clock).minusDays(1),
                OffsetDateTime.now(clock).plusHours(30),
                false,
                new EPromoCommunicationType[] {
                        EPromoCommunicationType.PUSH,
                        EPromoCommunicationType.INFORMER,
                        EPromoCommunicationType.CHECKOUT
        });

        List<PromoWithUser> almostExpiredIssuedPromos = getAlmostExpiredIssuedPromosForUser(2,
                OffsetDateTime.now(clock));

        Assertions.assertThat(almostExpiredIssuedPromos).anyMatch(it -> promo.getId().equals(it.getPromo().getId()));

    }


    @Test
    public void testNotIssued() {

        Promo promo = createPromo(EPromoAccessType.ALL_LIMITED,
                OffsetDateTime.now(clock).minusDays(1),
                OffsetDateTime.now(clock).plusHours(30),
                false,
                new EPromoCommunicationType[] {
                        EPromoCommunicationType.PUSH,
                        EPromoCommunicationType.CHECKOUT
                });

        List<PromoWithUser> almostExpiredIssuedPromos = getAlmostExpiredIssuedPromosForUser(2,
                OffsetDateTime.now(clock));

        Assertions.assertThat(almostExpiredIssuedPromos).noneMatch(it -> promo.getId().equals(it.getPromo().getId()));
    }

}
