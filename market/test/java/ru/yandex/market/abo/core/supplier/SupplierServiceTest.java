package ru.yandex.market.abo.core.supplier;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.EmptyTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author antipov93.
 * @date 12.09.18.
 */
public class SupplierServiceTest extends EmptyTest {

    @Autowired
    private SupplierService supplierService;
    @Autowired
    private SupplierRepo supplierRepo;

    @Test
    void testSave() {
        supplierService.saveOrUpdate(Arrays.asList(
                new Supplier().setId(1L).setRequestId(1L).setName("first").setOrganizationName("org1"),
                new Supplier().setId(2L).setRequestId(2L).setName("second").setOrganizationName("org2")
        ));
        flushAndClear();
        assertEquals(2, supplierRepo.findAll().size());

        Supplier firstChanged = new Supplier().setId(1L).setRequestId(4L).setName("first-changed").setOrganizationName("org1-changed");

        supplierService.saveOrUpdate(Arrays.asList(
                new Supplier().setId(3L).setRequestId(3L).setName("thrd").setOrganizationName("org3"),
                firstChanged
        ));
        flushAndClear();
        assertEquals(3, supplierRepo.findAll().size());
        assertEquals(firstChanged, supplierRepo.findByIdOrNull(1L));
    }

    @Test
    void testLoadByName() {
        supplierService.saveOrUpdate(Arrays.asList(
                new Supplier().setId(1L).setRequestId(1L).setName("first").setOrganizationName("org1"),
                new Supplier().setId(2L).setRequestId(2L).setName("second").setOrganizationName("org2"),
                new Supplier().setId(3L).setRequestId(3L).setName("first").setOrganizationName("org3")
        ));
        flushAndClear();
        assertEquals(2, supplierService.loadByName("i").size());
    }

    @Test
    void testLoadByRequest() {
        supplierService.saveOrUpdate(List.of(
                new Supplier().setId(7L).setRequestId(11L),
                new Supplier().setId(8L).setRequestId(11L),
                new Supplier().setId(9L).setRequestId(12L)
        ));
        flushAndClear();
        assertEquals(2, supplierService.loadByRequest(11L).size());
    }
}
