package ru.yandex.market.global.partner.util;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZoneOffset;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.function.Supplier;

import io.github.benas.randombeans.api.EnhancedRandom;
import lombok.Builder;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.global.db.jooq.tables.pojos.Business;
import ru.yandex.market.global.db.jooq.tables.pojos.LegalEntity;
import ru.yandex.market.global.db.jooq.tables.pojos.Permission;
import ru.yandex.market.global.db.jooq.tables.pojos.Shop;
import ru.yandex.market.global.db.jooq.tables.pojos.ShopSchedule;
import ru.yandex.market.global.partner.domain.business.BusinessRepository;
import ru.yandex.market.global.partner.domain.legal_entity.LegalEntityRepository;
import ru.yandex.market.global.partner.domain.shop.ShopCommandServiceImpl;
import ru.yandex.market.global.partner.domain.shop.model.ShopModel;

@Transactional
public class TestPartnerFactory {
    private static final EnhancedRandom RANDOM = RandomDataGenerator.dataRandom(TestPartnerFactory.class).build();

    @Autowired
    private ShopCommandServiceImpl shopCommandService;

    @Autowired
    private BusinessRepository businessRepository;

    @Autowired
    private LegalEntityRepository legalEntityRepository;

    @Autowired
    private Clock clock;

    private static final String[] VISUAL_CATEGORIES = new String[] {"alcohol", "flowers", "home-and-kitchen"};

    public Business createBusiness() {
        return createBusiness(Function.identity());
    }

    public Business createBusiness(Function<Business, Business> setupBusiness) {
        Business business = setupBusiness.apply(RANDOM.nextObject(Business.class));
        businessRepository.insert(business);
        return business;
    }

    public LegalEntity createLegalEntity(long businessId, Function<LegalEntity, LegalEntity> setupLegalEntity) {
        LegalEntity legalEntity = setupLegalEntity.apply(RANDOM.nextObject(LegalEntity.class))
                .setBusinessId(businessId);
        legalEntityRepository.insert(legalEntity);
        return legalEntity;
    }

    public LegalEntity createLegalEntity(long businessId) {
        return createLegalEntity(businessId, Function.identity());
    }

    public ShopModel createShopAndAllRequired() {
        return createShopAndAllRequired(CreateShopBuilder.builder().build());
    }

    public ShopModel createShop(long businessId, long legalEntityId) {
        return createShop(businessId, legalEntityId, CreateShopBuilder.builder().build());
    }

    public ShopModel createShop(long businessId, long legalEntityId, CreateShopBuilder builder) {
        return createShop(businessId, legalEntityId, builder, List::of);
    }

    public ShopModel createShop(long businessId, long legalEntityId, CreateShopBuilder builder,
                                Supplier<List<Permission>> permissionBuilder) {
        Shop shop = builder.setupShop.apply(RANDOM.nextObject(Shop.class)
                .setBusinessId(businessId)
                .setLegalEntityId(legalEntityId)
                .setVisualCategories(VISUAL_CATEGORIES)
        );

        OffsetDateTime now = OffsetDateTime.now(clock);
        String today = now.getDayOfWeek()
                .getDisplayName(TextStyle.SHORT, Locale.ENGLISH).toUpperCase();
        String yesterday = now.minus(1, ChronoUnit.DAYS).getDayOfWeek()
                .getDisplayName(TextStyle.SHORT, Locale.ENGLISH).toUpperCase();
        String tomorrow = now.plus(1, ChronoUnit.DAYS).getDayOfWeek()
                .getDisplayName(TextStyle.SHORT, Locale.ENGLISH).toUpperCase();

        List<ShopSchedule> schedule = List.of(
                new ShopSchedule()
                        .setDay(yesterday)
                        .setOpen(true)
                        .setStartAt(OffsetTime.of(0, 0, 0, 0, ZoneOffset.UTC))
                        .setEndAt(OffsetTime.of(23, 59, 59, 0, ZoneOffset.UTC)),
                new ShopSchedule()
                        .setDay(today)
                        .setOpen(true)
                        .setStartAt(OffsetTime.of(0, 0, 0, 0, ZoneOffset.UTC))
                        .setEndAt(OffsetTime.of(23, 59, 59, 0, ZoneOffset.UTC)),
                new ShopSchedule()
                        .setDay(tomorrow)
                        .setOpen(true)
                        .setStartAt(OffsetTime.of(0, 0, 0, 0, ZoneOffset.UTC))
                        .setEndAt(OffsetTime.of(23, 59, 59, 0, ZoneOffset.UTC))
        );

        List<Permission> permissions = builder.setupPermissions.apply(permissionBuilder.get());

        ShopModel shopModel = new ShopModel()
                .setShop(shop)
                .setSchedule(schedule)
                .setPermissions(permissions);

        shopCommandService.create(shopModel);
        return shopModel;
    }

    public ShopModel createShopAndAllRequired(CreateShopBuilder builder) {
        Business business = createBusiness();
        LegalEntity legalEntity = createLegalEntity(business.getId());
        ShopModel shopModel = createShop(business.getId(), legalEntity.getId(), builder);
        return shopModel.setLegalEntity(legalEntity);
    }

    @Data
    @Builder
    public static class CreateShopBuilder {
        @Builder.Default
        private Function<Shop, Shop> setupShop = Function.identity();

        @Builder.Default
        private Function<List<ShopSchedule>, List<ShopSchedule>> setupSchedule = Function.identity();

        @Builder.Default
        private Function<List<Permission>, List<Permission>> setupPermissions = Function.identity();
    }
}
