package ru.yandex.market.wms.shippingsorter.sorting.repository;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import ru.yandex.market.wms.shippingsorter.configuration.ShippingSorterSecurityTestConfiguration;
import ru.yandex.market.wms.shippingsorter.sorting.IntegrationTest;
import ru.yandex.market.wms.shippingsorter.sorting.entity.SorterExitEntity;

@Import(ShippingSorterSecurityTestConfiguration.class)
public class SorterExitRepositoryTest extends IntegrationTest {

    @Autowired
    private SorterExitRepository sorterExitRepository;

    @Test
    @DatabaseSetup("/sorting/repository/sorter-exit/common.xml")
    @ExpectedDatabase(value = "/sorting/repository/sorter-exit/common.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void getErrorSorterExitShouldOrderByZone() {
        SorterExitEntity ssortZone = sorterExitRepository.getErrorSorterExit("SSORT_ZONE").orElseThrow();
        SorterExitEntity ssortZone2 = sorterExitRepository.getErrorSorterExit("SSORT_ZN_2").orElseThrow();

        Assertions.assertAll(
                () -> Assertions.assertTrue(ssortZone.isErrorExit()),
                () -> Assertions.assertEquals("SR1_NOK-01", ssortZone.getSorterExitId().getId()),
                () -> Assertions.assertTrue(ssortZone2.isErrorExit()),
                () -> Assertions.assertEquals("SR2_NOK-01", ssortZone2.getSorterExitId().getId())
        );
    }

    @Test
    @DatabaseSetup("/sorting/repository/sorter-exit/common.xml")
    @ExpectedDatabase(value = "/sorting/repository/sorter-exit/common.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void getAlternateSorterExitShouldOrderByZone() {
        SorterExitEntity ssortZone = sorterExitRepository.getAlternateSorterExit("SSORT_ZONE").orElseThrow();
        SorterExitEntity ssortZone2 = sorterExitRepository.getAlternateSorterExit("SSORT_ZN_2").orElseThrow();

        Assertions.assertAll(
                () -> Assertions.assertTrue(ssortZone.isAlternateExit()),
                () -> Assertions.assertEquals("SR1_NOK-01", ssortZone.getSorterExitId().getId()),
                () -> Assertions.assertTrue(ssortZone2.isAlternateExit()),
                () -> Assertions.assertEquals("SR2_NOK-01", ssortZone2.getSorterExitId().getId())
        );
    }
}
