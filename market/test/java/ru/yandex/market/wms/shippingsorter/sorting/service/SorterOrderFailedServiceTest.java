package ru.yandex.market.wms.shippingsorter.sorting.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import ru.yandex.market.wms.common.spring.dto.ParcelIdDto;
import ru.yandex.market.wms.shippingsorter.configuration.ShippingSorterSecurityTestConfiguration;
import ru.yandex.market.wms.shippingsorter.sorting.IntegrationTest;

@Import(ShippingSorterSecurityTestConfiguration.class)
public class SorterOrderFailedServiceTest extends IntegrationTest {

    @Autowired
    private SorterOrderFailedService sorterOrderFailedService;

    @Test
    @DatabaseSetup("/sorting/service/sorter-order/failed/happy/before.xml")
    @ExpectedDatabase(value = "/sorting/service/sorter-order/failed/happy/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void processFailedParcelsHappyPath() {
        List<ParcelIdDto> parcelIds = Arrays.asList(new ParcelIdDto("P0001"), new ParcelIdDto("P0002"));
        sorterOrderFailedService.processFailedParcels(parcelIds);
    }

    @Test
    @DatabaseSetup("/sorting/service/sorter-order/failed/empty/before.xml")
    @ExpectedDatabase(value = "/sorting/service/sorter-order/failed/empty/before.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void processFailedParcelsEmptyParcelIds() {
        sorterOrderFailedService.processFailedParcels(new ArrayList<>());
    }
}
