package ru.yandex.market.fps.module.supplier.security.test;

import java.util.List;
import java.util.Map;

import javax.transaction.Transactional;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import ru.yandex.market.crm.util.Randoms;
import ru.yandex.market.fps.module.supplier.security.SupplierAccess;
import ru.yandex.market.fps.module.supplier1p.Supplier1p;
import ru.yandex.market.fps.module.supplier1p.test.impl.SupplierTestUtils;
import ru.yandex.market.jmf.bcp.BcpService;
import ru.yandex.market.jmf.entity.EntityStorageService;
import ru.yandex.market.jmf.entity.query.Filters;
import ru.yandex.market.jmf.entity.query.Query;
import ru.yandex.market.jmf.entity.test.assertions.EntityCollectionAssert;

@Transactional
@SpringJUnitConfig(InternalModuleSupplierSecurityTestConfiguration.class)
public class SuppliersListTest {
    private static final int OWNER_UID = 12345;
    private final EntityStorageService entityStorageService;
    private final BcpService bcpService;
    private final SupplierTestUtils supplierTestUtils;

    public SuppliersListTest(EntityStorageService entityStorageService, BcpService bcpService,
                             SupplierTestUtils supplierTestUtils) {
        this.entityStorageService = entityStorageService;
        this.bcpService = bcpService;
        this.supplierTestUtils = supplierTestUtils;
    }

    @Test
    public void testThatSupplierReturnedOnlyIfUserHasAccessToTheSupplier() {
        Supplier1p supplier1 = supplierTestUtils.createSupplier();

        supplierTestUtils.createSupplier();

        bcpService.create(SupplierAccess.FQN, Map.of(
                SupplierAccess.LOGIN, Randoms.string(),
                SupplierAccess.SUPPLIER, supplier1,
                SupplierAccess.UID, OWNER_UID,
                SupplierAccess.ROLES, "owner"
        ));

        List<Supplier1p> suppliersWithAccess = entityStorageService.list(Query.of(Supplier1p.FQN)
                .withFilters(Filters.eq(Supplier1p.UID, OWNER_UID)));

        EntityCollectionAssert.assertThat(suppliersWithAccess)
                .hasSize(1)
                .allHasAttributes(Supplier1p.GID, supplier1.getGid());

        List<Supplier1p> suppliersWithoutAccess = entityStorageService.list(Query.of(Supplier1p.FQN)
                .withFilters(Filters.eq(Supplier1p.UID, OWNER_UID + 1)));

        EntityCollectionAssert.assertThat(suppliersWithoutAccess)
                .hasSize(0);
    }
}
