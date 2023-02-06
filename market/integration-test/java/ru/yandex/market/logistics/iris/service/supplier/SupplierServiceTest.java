package ru.yandex.market.logistics.iris.service.supplier;

import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.iris.configuration.AbstractContextualTest;
import ru.yandex.market.mbi.api.client.entity.fulfillment.SupplierInfo;

public class SupplierServiceTest extends AbstractContextualTest {

    @Autowired
    private SupplierService supplierService;

    @Test
    @DatabaseSetup("/fixtures/setup/sync/supplier/1.xml")
    @ExpectedDatabase(
            value = "/fixtures/expected/sync/supplier/1.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    public void syncSuppliersSuccess() {
        Mockito.when(mbiApiClient.getSupplierInfoList()).thenReturn(List.of(
                new SupplierInfo.Builder()
                        .setId(1)
                        .setName("Supplier1")
                        .setDropship(true)
                        .setFulfillment(true)
                        .setCrossdock(true)
                        .setClickAndCollect(false)
                        .setDropshipBySeller(false)
                        .build(),
                new SupplierInfo.Builder()
                        .setId(2)
                        .setName("Supplier3")
                        .setDropship(false)
                        .setFulfillment(false)
                        .setCrossdock(false)
                        .setClickAndCollect(true)
                        .setDropshipBySeller(true)
                        .build()
        ));
        supplierService.syncSuppliers();
    }
}
