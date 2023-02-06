package ru.yandex.market.wms.shippingsorter.sorting.repository;

import java.math.BigDecimal;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import ru.yandex.market.wms.shippingsorter.configuration.ShippingSorterSecurityTestConfiguration;
import ru.yandex.market.wms.shippingsorter.sorting.IntegrationTest;

@Import(ShippingSorterSecurityTestConfiguration.class)
public class SorterBoxWeightDeviationRepositoryTest extends IntegrationTest {

    @Autowired
    private SorterBoxWeightDeviationRepository sorterBoxWeightDeviationRepository;

    @Test
    @DatabaseSetup("/sorting/repository/sorter-order/box-weight-deviation/one-interval.xml")
    public void getDeviation_oneInterval() {
        BigDecimal deviation = sorterBoxWeightDeviationRepository.getDeviation(10);
        Assertions.assertEquals(2.0, deviation.doubleValue(), 0.0);
    }

    @Test
    @DatabaseSetup("/sorting/repository/sorter-order/box-weight-deviation/one-interval.xml")
    public void getDeviation_outOfBounds_returnsZero() {
        BigDecimal deviation = sorterBoxWeightDeviationRepository.getDeviation(3000);
        Assertions.assertEquals(0.0, deviation.doubleValue(), 0.0);
    }

    @Test
    @DatabaseSetup("/sorting/repository/sorter-order/box-weight-deviation/intersection.xml")
    public void getDeviation_intersection() {
        BigDecimal deviation = sorterBoxWeightDeviationRepository.getDeviation(60);
        Assertions.assertEquals(2.0, deviation.doubleValue(), 0.0);
    }

    @Test
    @DatabaseSetup("/sorting/repository/sorter-order/box-weight-deviation/upper-border.xml")
    public void getDeviation_upperBorder() {
        BigDecimal deviation = sorterBoxWeightDeviationRepository.getDeviation(100);
        Assertions.assertEquals(10.0, deviation.doubleValue(), 0.0);
    }
}
