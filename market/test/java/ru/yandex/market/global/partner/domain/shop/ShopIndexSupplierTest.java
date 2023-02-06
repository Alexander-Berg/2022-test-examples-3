package ru.yandex.market.global.partner.domain.shop;

import java.util.Arrays;
import java.util.List;

import io.github.benas.randombeans.api.EnhancedRandom;
import lombok.RequiredArgsConstructor;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.global.common.elastic.IndexedEntity;
import ru.yandex.market.global.db.jooq.enums.EPermission;
import ru.yandex.market.global.db.jooq.enums.EPermissionTargetType;
import ru.yandex.market.global.db.jooq.tables.pojos.Contacts;
import ru.yandex.market.global.db.jooq.tables.pojos.Permission;
import ru.yandex.market.global.partner.BaseFunctionalTest;
import ru.yandex.market.global.partner.domain.contacts.ContactsCommandService;
import ru.yandex.market.global.partner.domain.permission.PermissionService;
import ru.yandex.market.global.partner.domain.shop.model.ShopModel;
import ru.yandex.market.global.partner.util.AssertjUtil;
import ru.yandex.market.global.partner.util.RandomDataGenerator;
import ru.yandex.market.global.partner.util.TestPartnerFactory;
import ru.yandex.mj.generated.server.model.ShopExportDto;

import static ru.yandex.market.global.partner.util.AssertjUtil.DEFAULT_COMPARISON_CONFIGURATION;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class ShopIndexSupplierTest extends BaseFunctionalTest {

    private static final EnhancedRandom RANDOM = RandomDataGenerator.dataRandom(ShopIndexSupplierTest.class).build();

    private final ShopIndexSupplier shopIndexSupplier;
    private final TestPartnerFactory testPartnerFactory;
    private final ContactsCommandService contactsCommandService;
    private final PermissionService permissionService;

    @Test
    public void testGetExportCorrectValues() {
        ShopModel shopModel = testPartnerFactory.createShopAndAllRequired();
        IndexedEntity<Long, ShopExportDto> indexedEntity = shopIndexSupplier.get(shopModel.getShop().getId());

        Assertions.assertThat(indexedEntity.getDto())
                .usingRecursiveComparison(AssertjUtil.createDefaultComparisonConfigurationBuilder()
                        .withIgnoredFields("schedule", "address", "hiddenExceptUids", "ezcountApiKey",
                                "ezcountRefreshToken", "taxId", "trustProductId", "trustRegionId", "visualCategories",
                                "notificationUsers")
                        .build()
                )
                .isEqualTo(shopModel.getShop());

        Assertions.assertThat(indexedEntity.getDto())
                .usingRecursiveComparison(AssertjUtil.createDefaultComparisonConfigurationBuilder()
                        .withComparedFields("ezcountApiKey", "ezcountRefreshToken", "taxId", "trustProductId",
                                "trustRegionId")
                        .build()
                )
                .isEqualTo(shopModel.getLegalEntity());

        Assertions.assertThat(indexedEntity.getDto().getVisualCategories())
                .usingRecursiveComparison().isEqualTo(Arrays.asList(shopModel.getShop().getVisualCategories()));

        Assertions.assertThat(indexedEntity.getDto().getHiddenExceptUids())
                .usingRecursiveComparison(DEFAULT_COMPARISON_CONFIGURATION)
                .isEqualTo(List.of(shopModel.getShop().getHiddenExceptUids()));

        Assertions.assertThat(indexedEntity.getDto().getAddress())
                .usingRecursiveComparison(DEFAULT_COMPARISON_CONFIGURATION)
                .isEqualTo(shopModel.getShop().getAddress());

        Assertions.assertThat(indexedEntity.getDto().getPermissions())
                .usingRecursiveComparison(DEFAULT_COMPARISON_CONFIGURATION)
                .isEqualTo(shopModel.getPermissions());

        Assertions.assertThat(indexedEntity.getDto().getSchedule())
                .usingRecursiveComparison(AssertjUtil.createDefaultComparisonConfigurationBuilder()
                        .withEqualsForFields(AssertjUtil::timeStrEqualsExtended, "startAt", "endAt")
                        .build()
                )
                .isEqualTo(shopModel.getSchedule());
    }

    @Test
    public void testGetExportPermissions() {
        ShopModel shopModel = testPartnerFactory.createShopAndAllRequired(
                TestPartnerFactory.CreateShopBuilder.builder()
                        .setupPermissions(it -> List.of())
                        .build());

        List<Permission> permissions = List.of(
                new Permission(null, 1010111L, EPermission.ADMINISTRATE, EPermissionTargetType.BUSINESS,
                        shopModel.getShop().getBusinessId()),
                new Permission(null, 1010112L, EPermission.ADMINISTRATE, EPermissionTargetType.SHOP,
                        shopModel.getShop().getId()),
                new Permission(null, 1010113L, EPermission.OPERATE, EPermissionTargetType.BUSINESS,
                        shopModel.getShop().getBusinessId()),
                new Permission(null, 1010114L, EPermission.ADMINISTRATE, EPermissionTargetType.BUSINESS,
                        shopModel.getShop().getBusinessId()));

        permissions.forEach(permissionService::createPermission);

        List<Contacts> contacts =
                List.of(RANDOM.nextObject(Contacts.class).setId(null).setUid(1010111L).setOrderSmsEnabled(true),
                        RANDOM.nextObject(Contacts.class).setId(null).setUid(1010113L).setOrderSmsEnabled(true),
                        RANDOM.nextObject(Contacts.class).setId(null).setUid(1010114L).setOrderSmsEnabled(false));

        contacts.forEach(contactsCommandService::createContact);


        IndexedEntity<Long, ShopExportDto> indexedEntity = shopIndexSupplier.get(shopModel.getShop().getId());

        Assertions.assertThat(indexedEntity.getDto().getPermissions())
                .usingRecursiveComparison(DEFAULT_COMPARISON_CONFIGURATION)
                .isEqualTo(permissions);

        Assertions.assertThat(indexedEntity.getDto().getNotificationUsers())
                .usingRecursiveComparison(DEFAULT_COMPARISON_CONFIGURATION)
                .isEqualTo(List.of(1010111L, 1010112L, 1010113L));

    }
}
