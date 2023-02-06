package ru.yandex.market.ff.repository;

import java.util.Arrays;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.core.supplier.model.SupplierType;
import ru.yandex.market.ff.base.IntegrationTest;
import ru.yandex.market.ff.model.entity.Supplier;
import ru.yandex.market.ff.model.entity.SupplierBusinessType;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;

/**
 * @author Vadim Lyalin
 */
public class SupplierRepositoryTest extends IntegrationTest {
    @Autowired
    private SupplierRepository supplierRepository;

    @Test
    @DatabaseSetup("classpath:repository/supplier/setup.xml")
    @ExpectedDatabase(value = "classpath:repository/supplier/save.xml", assertionMode = NON_STRICT)
    public void testSave() {
        supplierRepository.save(Arrays.asList(
                new Supplier(100L, "поставщик", "ООО поставщик", 200L, SupplierType.THIRD_PARTY,
                        new SupplierBusinessType(false, false, true, true, true)),
                new Supplier(123L, "supplier", "ИП supplier", null, null,
                        new SupplierBusinessType(true, true, false, false, false))
        ));
    }
}
