package ru.yandex.market.global.checkout.domain.actualize.actualizer;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import io.github.benas.randombeans.api.EnhancedRandom;
import lombok.RequiredArgsConstructor;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.global.checkout.BaseFunctionalTest;
import ru.yandex.market.global.checkout.domain.actualize.OrderActualization;
import ru.yandex.market.global.checkout.domain.order.OrderModel;
import ru.yandex.market.global.checkout.domain.promo.model.PromoType;
import ru.yandex.market.global.checkout.domain.promo.subject.PromoShopRepository;
import ru.yandex.market.global.checkout.domain.promo.subject.PromoUserRepository;
import ru.yandex.market.global.checkout.factory.TestOrderFactory;
import ru.yandex.market.global.checkout.factory.TestPromoFactory;
import ru.yandex.market.global.checkout.util.RandomDataGenerator;
import ru.yandex.market.global.db.jooq.enums.EPromoAccessType;
import ru.yandex.market.global.db.jooq.enums.EPromoApplicationType;
import ru.yandex.market.global.db.jooq.tables.pojos.Promo;
import ru.yandex.market.global.db.jooq.tables.pojos.PromoShop;
import ru.yandex.market.global.db.jooq.tables.pojos.PromoUser;

import static ru.yandex.market.global.checkout.factory.TestOrderFactory.buildOrderActualization;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class PromoInitialAllLimitedTest extends BaseFunctionalTest {

    private static final EnhancedRandom RANDOM = RandomDataGenerator
            .dataRandom(PromoInitialAllLimitedTest.class).build();

    private final PromoInitialAllLimitedActualizer actualizer;
    private final TestPromoFactory testPromoFactory;
    private final TestOrderFactory testOrderFactory;
    private final PromoUserRepository promoUserRepository;
    private final PromoShopRepository promoShopRepository;
    private final Clock clock;

    private Promo createAllLimitedPromo(Integer count, OffsetDateTime validTill) {
        return testPromoFactory.createPromo(TestPromoFactory.CreatePromoBuilder.builder()
                .setupPromo(it -> it
                        .setLimitedUsagesCount(count)
                        .setAccessType(EPromoAccessType.ALL_LIMITED)
                        .setApplicationType(EPromoApplicationType.PROMOCODE)
                        .setType(PromoType.FIXED_DISCOUNT.name())
                        .setValidTill(validTill)
                        .setValidFrom(OffsetDateTime.now(clock).minusDays(100))
                ).build());
    }

    private List<PromoUser> findUsages(long uid, Promo promo) {
        return promoUserRepository.fetchByUid(uid).stream()
                .filter(it -> it.getPromoId().equals(promo.getId()))
                .collect(Collectors.toList());
    }


    @Test
    public void testValid() {
        Long uid = RANDOM.nextObject(Long.class);

        Promo allLimitedPromo = createAllLimitedPromo(3, OffsetDateTime.now(clock).plusDays(100));

        OrderActualization actualization = buildOrderActualization(
                TestOrderFactory.CreateOrderActualizationBuilder.builder()
                        .setupOrder(it -> it.setPromocodes(allLimitedPromo.getName()).setUid(uid))
                        .setupPromoApplications(null)
                        .build()
        );
        List<PromoUser> promoUsers = findUsages(uid, allLimitedPromo);
        Assertions.assertThat(promoUsers).isNotNull().isEmpty();

        actualizer.actualize(actualization);
        promoUsers = findUsages(uid, allLimitedPromo);
        Assertions.assertThat(promoUsers).isNotNull().hasSize(3);
    }

    @Test
    public void testExpired() {
        Long uid = RANDOM.nextObject(Long.class);

        Promo allLimitedPromo = createAllLimitedPromo(3, OffsetDateTime.now(clock).minusDays(100));

        OrderActualization actualization = buildOrderActualization(
                TestOrderFactory.CreateOrderActualizationBuilder.builder()
                        .setupOrder(it -> it.setPromocodes(allLimitedPromo.getName()).setUid(uid))
                        .setupPromoApplications(null)
                        .build()
        );
        List<PromoUser> promoUsers = findUsages(uid, allLimitedPromo);
        Assertions.assertThat(promoUsers).isNotNull().isEmpty();

        actualizer.actualize(actualization);
        promoUsers = findUsages(uid, allLimitedPromo);
        Assertions.assertThat(promoUsers).isNotNull().isEmpty();
    }

    @Test
    public void testAllUnlimitedAndIssued() {
        Long uid = RANDOM.nextObject(Long.class);

        Promo unlimitedPromo = testPromoFactory.createPromo(TestPromoFactory.CreatePromoBuilder.builder()
                .setupPromo(it -> it
                        .setAccessType(EPromoAccessType.ALL_UNLIMITED)
                        .setApplicationType(EPromoApplicationType.PROMOCODE)
                        .setType(PromoType.FIXED_DISCOUNT.name())
                        .setValidTill(OffsetDateTime.now(clock).plusDays(100))
                        .setValidFrom(OffsetDateTime.now(clock).minusDays(100))
                ).build());
        Promo issuedPromo = testPromoFactory.createPromo(TestPromoFactory.CreatePromoBuilder.builder()
                .setupPromo(it -> it
                        .setAccessType(EPromoAccessType.ISSUED)
                        .setApplicationType(EPromoApplicationType.PROMOCODE)
                        .setType(PromoType.FIXED_DISCOUNT.name())
                        .setValidTill(OffsetDateTime.now(clock).plusDays(100))
                        .setValidFrom(OffsetDateTime.now(clock).minusDays(100))
                ).build());
        testPromoFactory.createUsageUserRecord(it -> it.setUid(uid).setPromoId(issuedPromo.getId()));

        Promo allLimitedPromo = createAllLimitedPromo(3, OffsetDateTime.now(clock).minusDays(100));

        OrderActualization actualization = buildOrderActualization(
                TestOrderFactory.CreateOrderActualizationBuilder.builder()
                        .setupOrder(it -> it.setPromocodes(allLimitedPromo.getName()).setUid(uid))
                        .setupPromoApplications(null)
                        .build()
        );
        List<PromoUser> promoUsers = promoUserRepository.fetchByUid(uid);
        Assertions.assertThat(promoUsers).isNotNull().hasSize(1); // only issued

        actualizer.actualize(actualization);
        promoUsers = promoUserRepository.fetchByUid(uid);
        Assertions.assertThat(promoUsers).isNotNull().hasSize(1); // only issued
    }

    @Test
    public void testAlreadyUsed() {
        Long uid = RANDOM.nextObject(Long.class);

        OrderModel order = testOrderFactory.createOrder();
        Promo allLimitedPromo = createAllLimitedPromo(3, OffsetDateTime.now(clock).minusDays(100));
        Function<PromoUser, PromoUser> setupPromoUser =
                it -> it.setUid(uid).setPromoId(allLimitedPromo.getId()).setUsed(true)
                        .setOrderId(order.getOrder().getId());
        testPromoFactory.createUsageUserRecord(setupPromoUser);
        testPromoFactory.createUsageUserRecord(setupPromoUser);
        testPromoFactory.createUsageUserRecord(setupPromoUser);

        OrderActualization actualization = buildOrderActualization(
                TestOrderFactory.CreateOrderActualizationBuilder.builder()
                        .setupOrder(it -> it.setPromocodes(allLimitedPromo.getName()).setUid(uid))
                        .setupPromoApplications(null)
                        .build()
        );
        List<PromoUser> promoUsers = findUsages(uid, allLimitedPromo);
        Assertions.assertThat(promoUsers).isNotNull().hasSize(3);

        actualizer.actualize(actualization);
        List<PromoUser> promoUsers2 = findUsages(uid, allLimitedPromo);
        Assertions.assertThat(promoUsers).isNotNull().hasSize(3);
        Assertions.assertThat(promoUsers).containsExactlyElementsOf(promoUsers2);
    }

    @Test
    public void testAlreadyUsedOne() {
        Long uid = RANDOM.nextObject(Long.class);

        OrderModel order = testOrderFactory.createOrder();
        Promo allLimitedPromo = createAllLimitedPromo(3, OffsetDateTime.now(clock).minusDays(100));
        Function<PromoUser, PromoUser> setupPromoUser =
                it -> it.setUid(uid).setPromoId(allLimitedPromo.getId()).setUsed(true)
                        .setOrderId(order.getOrder().getId());
        testPromoFactory.createUsageUserRecord(setupPromoUser);

        OrderActualization actualization = buildOrderActualization(
                TestOrderFactory.CreateOrderActualizationBuilder.builder()
                        .setupOrder(it -> it.setPromocodes(allLimitedPromo.getName()).setUid(uid))
                        .setupPromoApplications(null)
                        .build()
        );
        List<PromoUser> promoUsers = findUsages(uid, allLimitedPromo);
        Assertions.assertThat(promoUsers).isNotNull().hasSize(1);

        actualizer.actualize(actualization);
        List<PromoUser> promoUsers2 = findUsages(uid, allLimitedPromo);
        Assertions.assertThat(promoUsers).isNotNull().hasSize(1);
        Assertions.assertThat(promoUsers).containsExactlyElementsOf(promoUsers2);
    }

    @Test
    public void testShop() {
        Long shopId = RANDOM.nextObject(Long.class);

        Promo allLimitedPromo = testPromoFactory.createPromo(TestPromoFactory.CreatePromoBuilder.builder()
                .setupPromo(it -> it
                        .setLimitedUsagesCount(3)
                        .setAccessType(EPromoAccessType.ALL_LIMITED)
                        .setApplicationType(EPromoApplicationType.PROMOCODE)
                        .setType(PromoType.FREE_DELIVERY_SHOP.name())
                        .setValidTill(OffsetDateTime.now(clock).plusDays(100))
                        .setValidFrom(OffsetDateTime.now(clock).minusDays(100))
                ).build());

        OrderActualization actualization = buildOrderActualization(
                TestOrderFactory.CreateOrderActualizationBuilder.builder()
                        .setupOrder(it -> it.setPromocodes(allLimitedPromo.getName()).setShopId(shopId))
                        .setupPromoApplications(null)
                        .setupShop(it -> it.id(shopId))
                        .build()
        );
        List<PromoShop> promoShops = promoShopRepository.fetchByShopId(shopId);
        Assertions.assertThat(promoShops).isNotNull().isEmpty();

        actualizer.actualize(actualization);
        promoShops = promoShopRepository.fetchByShopId(shopId);
        Assertions.assertThat(promoShops).isNotNull().hasSize(3);
    }

}
