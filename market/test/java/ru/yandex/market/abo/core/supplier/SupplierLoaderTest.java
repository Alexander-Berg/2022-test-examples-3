package ru.yandex.market.abo.core.supplier;

import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.abo.cpa.MbiApiService;
import ru.yandex.market.core.supplier.model.SupplierType;
import ru.yandex.market.mbi.api.client.entity.fulfillment.SupplierInfo;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author antipov93.
 * @date 12.09.18.
 */
@ExtendWith(MockitoExtension.class)
public class SupplierLoaderTest {

    @InjectMocks
    private SupplierLoader supplierLoader;

    @Mock
    private SupplierService supplierService;
    @Mock
    private MbiApiService mbiApiService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(mbiApiService.getSupplierInfoList()).thenReturn(Arrays.asList(
                new SupplierInfo.Builder().setId(1).setName("first").setOrganisationName("first-org").setPrepayRequestId(1L).setSupplierType(SupplierType.FIRST_PARTY).build(),
                new SupplierInfo.Builder().setId(2).setName("second").setOrganisationName("second-org").setPrepayRequestId(1L).setSupplierType(SupplierType.FIRST_PARTY).build(),
                new SupplierInfo.Builder().setId(3).setName("third").setOrganisationName("third-org").setPrepayRequestId(2L).setSupplierType(SupplierType.FIRST_PARTY).build()
        ));
    }

    @Test
    void testLoadSuppliers() {
        supplierLoader.loadSuppliers();
        verify(supplierService, times(1)).saveOrUpdate(Arrays.asList(
                new Supplier().setId(1L).setRequestId(1L).setName("first").setOrganizationName("first-org"),
                new Supplier().setId(2L).setRequestId(1L).setName("second").setOrganizationName("second-org"),
                new Supplier().setId(3L).setRequestId(2L).setName("third").setOrganizationName("third-org")
        ));
    }
}
