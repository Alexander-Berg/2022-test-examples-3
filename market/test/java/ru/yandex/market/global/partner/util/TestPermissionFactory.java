package ru.yandex.market.global.partner.util;

import java.util.function.Function;

import io.github.benas.randombeans.api.EnhancedRandom;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.global.db.jooq.enums.EPermission;
import ru.yandex.market.global.db.jooq.enums.EPermissionTargetType;
import ru.yandex.market.global.db.jooq.tables.pojos.Permission;
import ru.yandex.market.global.partner.domain.permission.PermissionRepository;

@Transactional
public class TestPermissionFactory {
    private static final EnhancedRandom RANDOM = RandomDataGenerator.dataRandom(TestPermissionFactory.class).build();

    @Autowired
    private PermissionRepository permissionRepository;

    public Permission createShopPermission(long shopId, Function<Permission, Permission> setupPermission) {
        Permission permission = setupPermission.apply(RANDOM.nextObject(Permission.class))
                .setTargetId(shopId)
                .setTargetType(EPermissionTargetType.SHOP);
        permissionRepository.insert(permission);
        return permission;
    }

    public Permission createShopOperatePermission(long shopId, Function<Permission, Permission> setupPermission) {
        Permission permission = setupPermission.apply(RANDOM.nextObject(Permission.class))
                .setTargetId(shopId)
                .setTargetType(EPermissionTargetType.SHOP)
                .setPermission(EPermission.OPERATE);
        permissionRepository.insert(permission);
        return permission;
    }

}
