package ru.yandex.market.global.partner.domain.shop;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import io.github.benas.randombeans.api.EnhancedRandom;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.global.db.jooq.tables.pojos.Permission;
import ru.yandex.market.global.partner.BaseFunctionalTest;
import ru.yandex.market.global.partner.domain.shop.model.ShopModel;
import ru.yandex.market.global.partner.util.RandomDataGenerator;
import ru.yandex.market.global.partner.util.TestPartnerFactory;
import ru.yandex.market.global.partner.util.TestPermissionFactory;

import static ru.yandex.market.global.db.jooq.enums.EPermission.ADMINISTRATE;
import static ru.yandex.market.global.db.jooq.enums.EPermission.OPERATE;

public class ShopServiceTest extends BaseFunctionalTest {
    private static final EnhancedRandom RANDOM = RandomDataGenerator.dataRandom(ShopServiceTest.class).build();

    private static final String[] VISUAL_CATEGORIES = new String[] {"books", "home-and-kitchen"};
    private static final RecursiveComparisonConfiguration RECURSIVE_COMPARISON_CONFIGURATION =
            RecursiveComparisonConfiguration.builder()
                    .withComparatorForType(
                            Comparator.comparing(o -> o.atDate(LocalDate.now(ZoneId.of("UTC"))).toEpochSecond()),
                            OffsetTime.class
                    )
                    .withComparatorForType(Comparator.comparing(OffsetDateTime::toInstant), OffsetDateTime.class)
                    .withIgnoreAllExpectedNullFields(true)
                    .build();

    @Autowired
    private Clock clock;

    @Autowired
    private ShopQueryService shopQueryService;

    @Autowired
    private ShopCommandServiceIndexingImpl shopCommandService;

    @Autowired
    private TestPermissionFactory testPermissionFactory;

    @Autowired
    private TestPartnerFactory testPartnerFactory;

    @Test
    public void testGetPermissionsReturnAll() {
        ShopModel shopModel = shopQueryService.getModel(testData.getSomeShop());
        Assertions.assertThat(shopModel.getPermissions())
                .usingRecursiveFieldByFieldElementComparator(
                        RecursiveComparisonConfiguration.builder()
                                .withIgnoreAllExpectedNullFields(true)
                                .build()
                )
                .containsExactlyInAnyOrderElementsOf(List.of(
                        new Permission().setPermission(ADMINISTRATE).setUid(testData.getSomeBusinessAdminUid()),
                        new Permission().setPermission(OPERATE).setUid(testData.getSomeShopsOperatorUid())
                ));
    }

    @Test
    public void testGetPermissionsReturnEmpty() {
        ShopModel toCreate = RANDOM.nextObject(ShopModel.class);
        toCreate.getShop()
                .setLegalEntityId(testData.getOtherShopModels().get(0).getLegalEntity().getId())
                .setBusinessId(testData.getOtherShopModels().get(0).getShop().getBusinessId());

        shopCommandService.create(toCreate);

        Assertions.assertThat(shopQueryService.getModel(toCreate.getShop().getId()).getPermissions())
                .isEmpty();
    }

    @Test
    public void testGetAdminShops() {
        List<ShopModel> expected = testData.getSomeBusinessShopModels();
        List<ShopModel> actual = shopQueryService.getModelsByUserId(testData.getSomeBusinessAdminUid());
        Assertions.assertThat(actual)
                .usingRecursiveFieldByFieldElementComparatorOnFields("id")
                .containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    public void testGetOpertorShops() {
        ShopModel shopModel = testPartnerFactory.createShopAndAllRequired();
        Permission permission = testPermissionFactory
                .createShopPermission(shopModel.getShop().getId(), p -> p.setPermission(OPERATE));

        List<ShopModel> expected = List.of(shopModel);
        List<ShopModel> actual = shopQueryService.getModelsByUserId(permission.getUid());
        Assertions.assertThat(actual)
                .map(ShopModel::getLegalEntity)
                .usingRecursiveFieldByFieldElementComparator(RECURSIVE_COMPARISON_CONFIGURATION)
                .containsExactlyInAnyOrderElementsOf(expected.stream().map(ShopModel::getLegalEntity).collect(Collectors.toList()));
    }

    @Test
    public void testCreateShop() {
        ShopModel toCreate = RANDOM.nextObject(ShopModel.class)
                .setLegalEntity(null);

        toCreate.getShop()
                .setBusinessId(testData.getOtherShops().get(0).getBusinessId())
                .setLegalEntityId(testData.getOtherShops().get(0).getLegalEntityId())
                .setVisualCategories(VISUAL_CATEGORIES);

        shopCommandService.create(toCreate);
        Assertions.assertThat(shopQueryService.getModel(toCreate.getShop().getId()))
                .usingRecursiveComparison(RECURSIVE_COMPARISON_CONFIGURATION)
                .isEqualTo(toCreate);
    }

    @Test
    public void testUpdateShop() {
        ShopModel toUpdate = RANDOM.nextObject(ShopModel.class);
        toUpdate.getShop()
                .setBusinessId(testData.getSomeBusiness().getId())
                .setLegalEntityId(testData.getSomeShop().getLegalEntityId())
                .setId(testData.getSomeShop().getId())
                .setVisualCategories(VISUAL_CATEGORIES);

        shopCommandService.update(toUpdate);
        Assertions.assertThat(shopQueryService.getModel(toUpdate.getShop().getId()))
                .usingRecursiveComparison(RecursiveComparisonConfiguration.builder()
                        .withComparatorForType(Comparator.comparing(OffsetDateTime::toEpochSecond), OffsetDateTime.class)
                        .build()
                )
                .ignoringFields("permissions", "legalEntity")
                .isEqualTo(toUpdate);
    }
}
