package ru.yandex.market.global.partner.domain.permission;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import io.github.benas.randombeans.api.EnhancedRandom;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.global.db.jooq.enums.EPermission;
import ru.yandex.market.global.db.jooq.enums.EPermissionTargetType;
import ru.yandex.market.global.db.jooq.tables.pojos.Permission;
import ru.yandex.market.global.partner.BaseFunctionalTest;
import ru.yandex.market.global.partner.domain.permission.model.PermissionTarget;
import ru.yandex.market.global.partner.util.RandomDataGenerator;

import static ru.yandex.market.global.partner.mapper.EntityMapper.MAPPER;

public class PermissionServiceTest extends BaseFunctionalTest {
    private static final EnhancedRandom RANDOM = RandomDataGenerator.dataRandom(PermissionServiceTest.class).build();

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private PermissionRepository permissionRepository;

    @Test
    public void testCreatePermission() {
        Permission toCreate = RANDOM.nextObject(Permission.class);
        permissionService.createPermission(toCreate);

        Assertions.assertThat(permissionRepository.fetchOneById(toCreate.getId()))
                .usingRecursiveComparison()
                .isEqualTo(toCreate);
    }

    @Test
    public void testGetNonExistentBusinessPermission() {
        EPermission actual = permissionService.getUserPermission(
                testData.getSomeUnauthorizedUid(),
                MAPPER.toPermissionTarget(testData.getSomeBusiness())
        );

        Assertions.assertThat(actual).isNull();
    }

    @Test
    public void testExistingBusinessPermission() {
        EPermission actual = permissionService.getUserPermission(
                testData.getSomeBusinessAdminUid(),
                MAPPER.toPermissionTarget(testData.getSomeBusiness())
        );

        Assertions.assertThat(actual).isEqualTo(EPermission.ADMINISTRATE);
    }

    @Test
    public void testExistingShopPermission() {
        EPermission actual = permissionService.getUserPermission(
                testData.getSomeShopsOperatorUid(),
                MAPPER.toPermissionTarget(testData.getSomeShop())
        );

        Assertions.assertThat(actual).isEqualTo(EPermission.OPERATE);
    }

    @Test
    public void testInheritedShopPermission() {
        EPermission actual = permissionService.getUserPermission(
                testData.getSomeBusinessAdminUid(),
                MAPPER.toPermissionTarget(testData.getSomeShop())
        );

        Assertions.assertThat(actual).isEqualTo(EPermission.ADMINISTRATE);
    }

    @Test
    public void testNonExistentShopPermission() {
        EPermission actual = permissionService.getUserPermission(
                testData.getSomeUnauthorizedUid(),
                MAPPER.toPermissionTarget(testData.getSomeShop())
        );

        Assertions.assertThat(actual).isNull();
    }

    @Test
    public void testBusinessPermissionInheritedToShops() {
        Map<PermissionTarget, EPermission> inheritedShopPermissions = testData.getSomeBusinessShops().stream()
                .collect(Collectors.toMap(MAPPER::toPermissionTarget, s -> EPermission.ADMINISTRATE));
        Map<PermissionTarget, EPermission> actual =
                permissionService.getUserPermissions(testData.getSomeBusinessAdminUid());

        Assertions.assertThat(actual)
                .containsAllEntriesOf(inheritedShopPermissions)
                .containsEntry(
                        MAPPER.toPermissionTarget(testData.getSomeBusiness()),
                        EPermission.ADMINISTRATE
                );
    }

    @Test
    public void testExplicitPermissionDoNotTouched() {
        Permission shopPermission = new Permission()
                .setUid(testData.getSomeBusinessAdminUid())
                .setTargetType(EPermissionTargetType.SHOP)
                .setTargetId(testData.getSomeShop().getId())
                .setPermission(EPermission.OPERATE);

        permissionService.createPermission(shopPermission);

        Map<PermissionTarget, EPermission> actual = permissionService.getUserPermissions(
                testData.getSomeBusinessAdminUid()
        );

        Assertions.assertThat(actual)
                .containsEntry(
                        MAPPER.toPermissionTarget(shopPermission),
                        EPermission.OPERATE
                );
    }

    @Test
    public void testGetShopPermissions() {
        Permission shopPermission = new Permission()
                .setUid(testData.getSomeUnauthorizedUid())
                .setTargetType(EPermissionTargetType.SHOP)
                .setTargetId(testData.getOtherShops().get(0).getId())
                .setPermission(EPermission.OPERATE);
        permissionService.createPermission(shopPermission);

        List<Permission> permissions = permissionService.getShopPermissions(testData.getOtherShops().get(0));
        Assertions.assertThat(permissions)
                .usingRecursiveFieldByFieldElementComparator(RecursiveComparisonConfiguration.builder()
                        .withIgnoreAllExpectedNullFields(true)
                        .build()
                )
                .containsExactly(shopPermission);
    }

    @Test
    public void testUserPermissionsIsConsistent() {
        Permission shopPermission = new Permission()
                .setUid(testData.getSomeBusinessAdminUid())
                .setTargetType(EPermissionTargetType.SHOP)
                .setTargetId(testData.getSomeShop().getId())
                .setPermission(EPermission.OPERATE);
        permissionService.createPermission(shopPermission);

        for (Map.Entry<PermissionTarget, EPermission> entry :
                permissionService.getUserPermissions(testData.getSomeBusinessAdminUid()).entrySet()) {
            Assertions.assertThat(permissionService.getUserPermission(
                    testData.getSomeBusinessAdminUid(), entry.getKey()
            )).isEqualTo(entry.getValue());
        }
    }
}
