package ru.yandex.market.vendors.analytics.core.dao.clickhouse;

import java.util.List;
import java.util.Set;

import one.util.streamex.LongStreamEx;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;

import ru.yandex.market.vendors.analytics.core.FunctionalTest;
import ru.yandex.market.vendors.analytics.core.dao.clickhouse.sales.growth.GrowthSalesDAO;
import ru.yandex.market.vendors.analytics.core.model.common.GeoFilters;
import ru.yandex.market.vendors.analytics.core.model.common.IntervalsForComparison;
import ru.yandex.market.vendors.analytics.core.model.common.MoneyCountPair;
import ru.yandex.market.vendors.analytics.core.model.common.StartEndDate;
import ru.yandex.market.vendors.analytics.core.model.common.socdem.SocdemFilter;
import ru.yandex.market.vendors.analytics.core.model.sales.common.CategoryPriceSegmentsFilter;
import ru.yandex.market.vendors.analytics.core.model.sales.growth.SalesChangeInfo;
import ru.yandex.market.vendors.analytics.core.model.sales.growth.brand.RawBrandSalesChange;
import ru.yandex.market.vendors.analytics.core.utils.dbunit.ClickhouseDbUnitDataSet;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;


/**
 * Functional tests for {@link ExternalDataClickhouseDao}}.
 *
 * @author ogonek.
 */
@ClickhouseDbUnitDataSet(before = "ExternalData.ch.before.csv")
public class ExternalDataClickhouseDaoTest extends FunctionalTest {

    @Autowired
    private GrowthSalesDAO growthSalesDAO;

    @SpyBean
    private ExternalDataClickhouseDao externalDataClickhouseDao;

    @Test
    void longQueryTest() {
        Set<Long> modelIds = LongStreamEx.range(1000000000, 1000030000)
                .boxed().toSet();

        CategoryPriceSegmentsFilter categoryPriceSegmentsFilter = CategoryPriceSegmentsFilter.builder()
                .categoryId(91491)
                .build();
        IntervalsForComparison intervalsForComparison = IntervalsForComparison.builder()
                .baseInterval(new StartEndDate("2019-01-01", "2019-01-31"))
                .comparingInterval(new StartEndDate("2019-02-01", "2019-02-28"))
                .build();

        List<RawBrandSalesChange> result = growthSalesDAO.loadBrandsSalesChanges(
                categoryPriceSegmentsFilter,
                Set.of(),
                modelIds,
                intervalsForComparison,
                GeoFilters.empty(),
                SocdemFilter.empty(),
                Set.of()
        );

        SalesChangeInfo data = new SalesChangeInfo(
                MoneyCountPair.builder().money(1000).count(2).build(),
                MoneyCountPair.ZERO
        );
        List<RawBrandSalesChange> expected = List.of(new RawBrandSalesChange(data, 1));
        Assertions.assertEquals(expected, result);
        Mockito.verify(externalDataClickhouseDao, times(1)).executeQueryWithExternalData(any(), any(), any());
    }

}
