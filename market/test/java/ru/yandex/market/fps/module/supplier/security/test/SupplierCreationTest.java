package ru.yandex.market.fps.module.supplier.security.test;

import java.util.List;
import java.util.Map;

import javax.transaction.Transactional;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import ru.yandex.market.fps.module.supplier.security.SupplierAccess;
import ru.yandex.market.fps.module.supplier.security.SupplierAccessRole;
import ru.yandex.market.fps.module.supplier1p.Supplier1p;
import ru.yandex.market.fps.module.supplier1p.test.impl.SupplierTestUtils;
import ru.yandex.market.jmf.entity.EntityStorageService;
import ru.yandex.market.jmf.entity.query.Query;
import ru.yandex.market.jmf.entity.test.assertions.EntityCollectionAssert;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;

@Transactional
@SpringJUnitConfig(InternalModuleSupplierSecurityTestConfiguration.class)
public class SupplierCreationTest {
    private final SupplierTestUtils supplierTestUtils;
    private final EntityStorageService entityStorageService;

    public SupplierCreationTest(SupplierTestUtils supplierTestUtils, EntityStorageService entityStorageService) {
        this.supplierTestUtils = supplierTestUtils;
        this.entityStorageService = entityStorageService;
    }

    @Test
    public void testThatSupplierAccessIsCreatedForNewSupplier() {
        long uid = 1234;
        String login = "login";
        Supplier1p supplier = supplierTestUtils.createSupplier(Map.of(
                Supplier1p.TITLE, login,
                Supplier1p.UID, uid
        ));

        List<SupplierAccess> supplierAccesses = entityStorageService.list(Query.of(SupplierAccess.FQN));

        EntityCollectionAssert.assertThat(supplierAccesses)
                .hasSize(1)
                .allHasAttributes(
                        SupplierAccess.SUPPLIER, supplier,
                        SupplierAccess.LOGIN, login,
                        SupplierAccess.UID, uid,
                        SupplierAccess.ROLES, allOf(
                                hasSize(1),
                                contains(hasProperty(SupplierAccessRole.CODE, equalTo("owner")))
                        )
                );
    }
}
