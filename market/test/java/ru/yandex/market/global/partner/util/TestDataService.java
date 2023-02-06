package ru.yandex.market.global.partner.util;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import io.github.benas.randombeans.api.EnhancedRandom;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.global.common.test.RandomUtil;
import ru.yandex.market.global.db.jooq.enums.EPermission;
import ru.yandex.market.global.db.jooq.enums.EPermissionTargetType;
import ru.yandex.market.global.db.jooq.tables.pojos.Business;
import ru.yandex.market.global.db.jooq.tables.pojos.LegalEntity;
import ru.yandex.market.global.db.jooq.tables.pojos.Permission;
import ru.yandex.market.global.db.jooq.tables.pojos.Shop;
import ru.yandex.market.global.db.jooq.tables.pojos.ShopSchedule;
import ru.yandex.market.global.partner.domain.business.BusinessRepository;
import ru.yandex.market.global.partner.domain.legal_entity.LegalEntityRepository;
import ru.yandex.market.global.partner.domain.permission.PermissionRepository;
import ru.yandex.market.global.partner.domain.permission.PermissionService;
import ru.yandex.market.global.partner.domain.shop.ShopRepository;
import ru.yandex.market.global.partner.domain.shop.ShopScheduleRepository;
import ru.yandex.market.global.partner.domain.shop.model.ShopModel;

import static ru.yandex.market.global.db.jooq.tables.Business.BUSINESS;
import static ru.yandex.market.global.db.jooq.tables.Permission.PERMISSION;
import static ru.yandex.market.global.db.jooq.tables.Shop.SHOP;

@RequiredArgsConstructor
public class TestDataService {
    private static final EnhancedRandom RANDOM = RandomDataGenerator.dataRandom(TestDataService.class).build();
    private static final int BUSINESS_COUNT = 5;
    private static final int SHOPS_COUNT = 10;
    private static final int MAX_UID = 100000000;

    @Autowired
    private final DSLContext ctx;

    @Autowired
    private final BusinessRepository businessRepository;

    @Autowired
    private final LegalEntityRepository legalEntityRepository;

    @Autowired
    private final ShopRepository shopRepository;

    @Autowired
    private final ShopScheduleRepository shopScheduleRepository;

    @Autowired
    private final PermissionRepository permissionRepository;

    @Autowired
    private final PermissionService permissionService;

    public TestData saveTestData() {
        Business someBusiness = RANDOM.nextObject(Business.class).setId(11308928L);
        businessRepository.insert(someBusiness);

        LegalEntity someLegalEntity = RANDOM.nextObject(LegalEntity.class)
                .setBusinessId(someBusiness.getId());
        legalEntityRepository.insert(someLegalEntity);

        List<ShopModel> someBusinessShopModels = RANDOM.objects(ShopModel.class, SHOPS_COUNT)
                .peek(m -> m.getShop()
                        .setBusinessId(someBusiness.getId())
                        .setLegalEntityId(someLegalEntity.getId())
                )
                .peek(m -> m.setLegalEntity(someLegalEntity))
                .peek(m -> shopRepository.insert(m.getShop()))
                .peek(m -> m.getSchedule().forEach(s -> s.setShopId(m.getShop().getId())))
                .peek(m -> shopScheduleRepository.insert(m.getSchedule().toArray(ShopSchedule[]::new)))
                .collect(Collectors.toUnmodifiableList());

        ShopModel someShopModel = someBusinessShopModels.get(0);

        long adminUid = RANDOM.nextInt(MAX_UID);
        long operatorUid = adminUid + 1;
        long someUid = adminUid + 2;

        permissionRepository.insert(
                new Permission()
                        .setTargetType(EPermissionTargetType.BUSINESS)
                        .setTargetId(someShopModel.getShop().getBusinessId())
                        .setPermission(EPermission.ADMINISTRATE)
                        .setUid(adminUid),
                new Permission()
                        .setTargetType(EPermissionTargetType.SHOP)
                        .setTargetId(someShopModel.getShop().getId())
                        .setPermission(EPermission.OPERATE)
                        .setUid(operatorUid)
        );
        someBusinessShopModels
                .forEach(m -> m.setPermissions(permissionService.getShopPermissions(m.getShop())));

        List<Business> otherBusinesses = RANDOM.objects(Business.class, BUSINESS_COUNT)
                .peek(businessRepository::insert)
                .collect(Collectors.toUnmodifiableList());

        Map<Long, LegalEntity> otherLegalEntities = otherBusinesses.stream()
                .map(b -> RANDOM.nextObject(LegalEntity.class).setBusinessId(b.getId()))
                .peek(legalEntityRepository::insert)
                .collect(Collectors.toMap(LegalEntity::getBusinessId, Function.identity()));

        List<ShopModel> otherShopModels = RANDOM.objects(ShopModel.class, SHOPS_COUNT)
                .peek(m -> m.getShop().setBusinessId(RandomUtil.randomItem(RANDOM, otherBusinesses).getId()))
                .peek(m -> m.getShop().setLegalEntityId(otherLegalEntities.get(m.getShop().getBusinessId()).getId()))
                .peek(m -> m.setLegalEntity(otherLegalEntities.get(m.getShop().getBusinessId())))
                .peek(m -> shopRepository.insert(m.getShop()))
                .peek(m -> m.getSchedule().forEach(s -> s.setShopId(m.getShop().getId())))
                .peek(m -> shopScheduleRepository.insert(m.getSchedule().toArray(ShopSchedule[]::new)))
                .collect(Collectors.toUnmodifiableList());

        return new TestData()
                .setSomeBusiness(someBusiness)
                .setSomeBusinessShops(
                        someBusinessShopModels.stream().map(ShopModel::getShop).collect(Collectors.toList())
                )
                .setSomeBusinessShopModels(someBusinessShopModels)
                .setSomeShop(someShopModel.getShop())
                .setSomeShopModel(someShopModel)
                .setSomeBusinessAdminUid(adminUid)
                .setSomeShopsOperatorUid(operatorUid)
                .setSomeUnauthorizedUid(someUid)
                .setOtherBusinesses(otherBusinesses)
                .setOtherShops(
                        otherShopModels.stream().map(ShopModel::getShop).collect(Collectors.toList())
                )
                .setOtherShopModels(otherShopModels);
    }

    public void cleanTestData() {
        ctx.truncate(PERMISSION).cascade().execute();
        ctx.truncate(BUSINESS).cascade().execute();
        ctx.truncate(SHOP).cascade().execute();
    }

    @Data
    @Accessors(chain = true)
    public static final class TestData {
        private List<Business> otherBusinesses;
        private List<ShopModel> otherShopModels;
        private List<Shop> otherShops;
        private List<ShopModel> someBusinessShopModels;
        private List<Shop> someBusinessShops;
        private Business someBusiness;
        private Shop someShop;
        private ShopModel someShopModel;
        private long someBusinessAdminUid;
        private long someShopsOperatorUid;
        private long someUnauthorizedUid;
    }
}
