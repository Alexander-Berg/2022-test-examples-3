package ru.yandex.market.fps.module.supplier.security.test;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.crm.util.Randoms;
import ru.yandex.market.fps.module.supplier.security.SupplierAccess;
import ru.yandex.market.fps.module.supplier.security.SupplierAccessAction;
import ru.yandex.market.fps.module.supplier.security.SupplierAccessPermission;
import ru.yandex.market.fps.module.supplier.security.SupplierAccessRole;
import ru.yandex.market.fps.module.supplier.security.SupplierSecurityService;
import ru.yandex.market.fps.module.supplier1p.Supplier1p;
import ru.yandex.market.fps.module.supplier1p.test.impl.SupplierTestUtils;
import ru.yandex.market.jmf.bcp.BcpService;
import ru.yandex.market.jmf.entity.EntityStorageService;
import ru.yandex.market.jmf.entity.query.Filters;
import ru.yandex.market.jmf.entity.query.Query;

@Transactional
@SpringJUnitConfig(InternalModuleSupplierSecurityTestConfiguration.class)
public class SupplierSecurityServiceTest {
    private static final int UID1 = 12345;
    private static final int UID2 = 12346;

    private final SupplierSecurityService supplierSecurityService;
    private final SupplierTestUtils supplierTestUtils;
    private final BcpService bcpService;
    private final EntityStorageService entityStorageService;

    public SupplierSecurityServiceTest(SupplierSecurityService supplierSecurityService,
                                       SupplierTestUtils supplierTestUtils, BcpService bcpService,
                                       EntityStorageService entityStorageService) {
        this.supplierSecurityService = supplierSecurityService;
        this.supplierTestUtils = supplierTestUtils;
        this.bcpService = bcpService;
        this.entityStorageService = entityStorageService;
    }

    private SupplierAccessPermission findPermission(String role, String action) {
        return entityStorageService.<SupplierAccessPermission>list(Query.of(SupplierAccessPermission.FQN)
                .withFilters(
                        Filters.eq(SupplierAccessPermission.ROLE, role),
                        Filters.eq(SupplierAccessPermission.ACTION, action)
                )).get(0);
    }

    private SupplierAccessPermission setPermission(String role, String action, boolean allowed) {
        return bcpService.edit(findPermission(role, action), Map.of(
                SupplierAccessPermission.ALLOWED, allowed
        ));
    }

    @Test
    public void testGetAllPermissions() {
        Supplier1p supplier = supplierTestUtils.createSupplier();

        setPermission("a", "testAction1", true);
        setPermission("b", "testAction2", true);

        bcpService.create(SupplierAccess.FQN, Map.of(
                SupplierAccess.LOGIN, Randoms.string(),
                SupplierAccess.SUPPLIER, supplier,
                SupplierAccess.UID, UID1,
                SupplierAccess.ROLES, List.of("a", "b")
        ));

        Map<String, Boolean> permissions = supplierSecurityService.getPermissionsForUser(supplier, UID1);

        Assertions.assertEquals(Map.of(
                "testAction1", true,
                "testAction2", true
        ), permissions);
    }

    @Test
    public void testGetAllPermissions2() {
        Supplier1p supplier = supplierTestUtils.createSupplier();

        setPermission("a", "testAction1", true);
        setPermission("b", "testAction2", true);

        bcpService.create(SupplierAccess.FQN, Map.of(
                SupplierAccess.LOGIN, Randoms.string(),
                SupplierAccess.SUPPLIER, supplier,
                SupplierAccess.UID, UID1,
                SupplierAccess.ROLES, List.of("a")
        ));

        Map<String, Boolean> permissions = supplierSecurityService.getPermissionsForUser(supplier, UID1);

        Assertions.assertEquals(Map.of(
                "testAction1", true,
                "testAction2", false
        ), permissions);
    }

    @Test
    public void testGetAllPermissions_archiveRole() {
        Supplier1p supplier = supplierTestUtils.createSupplier();

        setPermission("a", "testAction1", true);
        var permission = setPermission("b", "testAction2", true);

        bcpService.edit(permission.getRole(), SupplierAccessRole.STATUS, "archived");

        bcpService.create(SupplierAccess.FQN, Map.of(
                SupplierAccess.LOGIN, Randoms.string(),
                SupplierAccess.SUPPLIER, supplier,
                SupplierAccess.UID, UID1,
                SupplierAccess.ROLES, List.of("a", "b")
        ));

        Map<String, Boolean> permissions = supplierSecurityService.getPermissionsForUser(supplier, UID1);

        Assertions.assertEquals(Map.of(
                "testAction1", true,
                "testAction2", false
        ), permissions);
    }

    @Test
    public void testGetAllPermissions_archiveAction() {
        Supplier1p supplier = supplierTestUtils.createSupplier();

        setPermission("a", "testAction1", true);
        var permission = setPermission("b", "testAction2", true);

        bcpService.edit(permission.getAction(), SupplierAccessAction.STATUS, "archived");

        bcpService.create(SupplierAccess.FQN, Map.of(
                SupplierAccess.LOGIN, Randoms.string(),
                SupplierAccess.SUPPLIER, supplier,
                SupplierAccess.UID, UID1,
                SupplierAccess.ROLES, List.of("a", "b")
        ));

        Map<String, Boolean> permissions = supplierSecurityService.getPermissionsForUser(supplier, UID1);

        Assertions.assertEquals(Map.of(
                "testAction1", true
        ), permissions);
    }
}
