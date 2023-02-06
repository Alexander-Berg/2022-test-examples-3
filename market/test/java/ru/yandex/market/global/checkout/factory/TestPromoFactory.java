package ru.yandex.market.global.checkout.factory;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.function.Function;
import java.util.function.Supplier;

import io.github.benas.randombeans.api.EnhancedRandom;
import lombok.Builder;
import lombok.Data;
import lombok.SneakyThrows;
import org.jooq.JSONB;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.global.checkout.domain.promo.KnownPromos;
import ru.yandex.market.global.checkout.domain.promo.PromoRepository;
import ru.yandex.market.global.checkout.domain.promo.PromoUtil;
import ru.yandex.market.global.checkout.domain.promo.apply.PromoArgs;
import ru.yandex.market.global.checkout.domain.promo.apply.free_delivery.shop.ShopFreeDeliveryArgs;
import ru.yandex.market.global.checkout.domain.promo.apply.free_delivery.shop.ShopFreeDeliveryCommonState;
import ru.yandex.market.global.checkout.domain.promo.model.PromoType;
import ru.yandex.market.global.checkout.domain.promo.subject.PromoShopRepository;
import ru.yandex.market.global.checkout.domain.promo.subject.PromoUserRepository;
import ru.yandex.market.global.checkout.mapper.JsonMapper;
import ru.yandex.market.global.checkout.util.RandomDataGenerator;
import ru.yandex.market.global.common.util.EnumUtil;
import ru.yandex.market.global.db.jooq.enums.EPromoAccessType;
import ru.yandex.market.global.db.jooq.enums.EPromoApplicationType;
import ru.yandex.market.global.db.jooq.enums.EPromoCommunicationType;
import ru.yandex.market.global.db.jooq.tables.pojos.Promo;
import ru.yandex.market.global.db.jooq.tables.pojos.PromoShop;
import ru.yandex.market.global.db.jooq.tables.pojos.PromoUser;
import ru.yandex.mj.generated.server.model.PromoCommunicationArgsDto;

import static ru.yandex.market.global.db.jooq.enums.EPromoCommunicationType.CHECKOUT;
import static ru.yandex.market.global.db.jooq.enums.EPromoCommunicationType.INFORMER;
import static ru.yandex.market.global.db.jooq.enums.EPromoCommunicationType.PUSH;

@Transactional
public class TestPromoFactory {
    private static final EnhancedRandom RANDOM = RandomDataGenerator.dataRandom(TestPromoFactory.class).build();

    @Autowired
    private PromoRepository promoRepository;

    @Autowired
    private PromoUserRepository promoUserRepository;

    @Autowired
    private PromoShopRepository promoShopRepository;

    @Autowired
    private Clock clock;

    @SneakyThrows
    public Promo createPromo(TestPromoFactory.CreatePromoBuilder builder) {
        OffsetDateTime now = OffsetDateTime.now(clock);
        JSONB state = JSONB.valueOf(JsonMapper.DB_JSON_MAPPER.writeValueAsString(builder.setupState.get()));

        Promo promo = RANDOM.nextObject(Promo.class)
                .setValidFrom(now.minus(1, ChronoUnit.DAYS))
                .setValidTill(now.plus(1, ChronoUnit.DAYS))
                .setTags((String[]) null)
                .setState(state)
                .setLimitedUsagesCount(1);
        promo = promo.setCommunicationTypes(CHECKOUT, INFORMER, PUSH);
        promo = promo.setCommunicationArgs(new PromoCommunicationArgsDto()
                        .checkout(PromoUtil.createDefaultCheckoutCommunication(promo))
                        .informer(PromoUtil.createDefaultInformerCommunication(promo))
                        .push(PromoUtil.createDefaultIssuedPushCommunication())
                );
        promo = builder.setupPromo.apply(promo);



        promo.setCommunicationTypes(builder.setupCommunicationTypes.apply(promo.getCommunicationTypes()));
        promo.setCommunicationArgs(builder.setupCommunicationArgs.apply(promo.getCommunicationArgs()));

        promo.setArgs(builder.setupArgs.apply(
                RANDOM.nextObject(EnumUtil.tryGetValue(PromoType.class, promo.getType()).getArgsClass())
        ));

        promoRepository.insert(promo);
        return promo;
    }

    public PromoUser createUsageUserRecord(Function<PromoUser, PromoUser> setupPromoUser) {
        PromoUser promoUser = new PromoUser()
                .setValidTill(OffsetDateTime.now(clock).plus(1, ChronoUnit.DAYS))
                .setUsed(false)
                .setUsedAt(null);
        promoUser = setupPromoUser.apply(promoUser);
        promoUserRepository.insert(promoUser);

        return promoUser;
    }

    public PromoShop createUsageShopRecord(Function<PromoShop, PromoShop> setupPromoShop) {
        PromoShop promoShop = new PromoShop()
                .setValidTill(OffsetDateTime.now(clock).plus(1, ChronoUnit.DAYS))
                .setUsed(false)
                .setUsedAt(null);
        promoShop = setupPromoShop.apply(promoShop);
        promoShopRepository.insert(promoShop);

        return promoShop;
    }

    @SneakyThrows
    public Promo createPromo() {
        return createPromo(CreatePromoBuilder.builder().build());
    }

    /**
     * Создаёт точно такое же промо, как и в
     * --changeset denr01:add_shop_free_delivery_promo
     */
    public Promo createShopFreeDeliveryPromo() {
        return createPromo(CreatePromoBuilder.builder()
                .setupPromo(p -> p
                        .setName(KnownPromos.SHOP_FREE_DELIVERY_FOR_NEW_USERS)
                        .setDescription("Free delivery for shop for each referred user")
                        .setType(PromoType.FREE_DELIVERY_SHOP.name())
                        .setAccessType(EPromoAccessType.ISSUED)
                        .setApplicationType(EPromoApplicationType.UNCONDITIONAL)
                        .setValidFrom(OffsetDateTime.now(clock).minusDays(1))
                        .setValidTill(OffsetDateTime.now(clock).plusYears(20)))
                .setupArgs((a) -> new ShopFreeDeliveryArgs()
                        .setBudgetCount(1000000)
                )
                .setupState(ShopFreeDeliveryCommonState::new)
                .build());
    }

    @Data
    @Builder
    public static class CreatePromoBuilder {
        @Builder.Default
        private Function<Promo, Promo> setupPromo = Function.identity();
        @Builder.Default
        private Function<PromoArgs, PromoArgs> setupArgs = Function.identity();
        @Builder.Default
        private Function<EPromoCommunicationType[], EPromoCommunicationType[]> setupCommunicationTypes =
                Function.identity();
        @Builder.Default
        private Function<PromoCommunicationArgsDto, PromoCommunicationArgsDto> setupCommunicationArgs =
                Function.identity();
        @Builder.Default
        private Supplier<Object> setupState = () -> null;
    }
}
