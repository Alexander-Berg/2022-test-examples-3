package ru.yandex.direct.grid.processing.service.deal;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.direct.core.entity.deal.model.CompleteReason;
import ru.yandex.direct.core.entity.deal.model.StatusDirect;
import ru.yandex.direct.grid.core.entity.deal.model.GdiDeal;
import ru.yandex.direct.grid.core.entity.deal.model.GdiDealOrderByField;
import ru.yandex.direct.grid.model.Order;
import ru.yandex.direct.grid.processing.model.deal.GdDealOrderBy;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

@RunWith(Parameterized.class)
public class DealDataServiceTest {

    @InjectMocks
    DealDataService dealDataService;

    private List<GdiDeal> deals;

    @Parameterized.Parameter(0)
    public Order sortOrder;

    @Parameterized.Parameter(1)
    public List<GdiDeal> dealsSorted;

    @Parameterized.Parameters(name = "sort order = {0}")
    public static Collection<Object[]> data() {
        List<GdiDeal> dealsSortedAsc = Arrays.asList(
                new GdiDeal()
                        .withStatus(StatusDirect.RECEIVED),
                new GdiDeal()
                        .withStatus(StatusDirect.ACTIVE),
                new GdiDeal()
                        .withStatus(StatusDirect.COMPLETED)
                        .withCompleteReason(CompleteReason.BY_CLIENT),
                new GdiDeal()
                        .withStatus(StatusDirect.COMPLETED)
                        .withCompleteReason(CompleteReason.BY_PUBLISHER),
                new GdiDeal()
                        .withStatus(StatusDirect.COMPLETED)
                        .withCompleteReason(CompleteReason.BY_DATE),
                new GdiDeal()
                        .withStatus(StatusDirect.ARCHIVED)
        );

        List<GdiDeal> dealsSortedDesc = Lists.reverse(dealsSortedAsc);

        return Arrays.asList(new Object[][]{
                {Order.ASC, dealsSortedAsc},
                {Order.DESC, dealsSortedDesc},
        });
    }

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);

        deals = Arrays.asList(
                new GdiDeal()
                        .withStatus(StatusDirect.ACTIVE),
                new GdiDeal()
                        .withStatus(StatusDirect.COMPLETED)
                        .withCompleteReason(CompleteReason.BY_DATE),
                new GdiDeal()
                        .withStatus(StatusDirect.ARCHIVED),
                new GdiDeal()
                        .withStatus(StatusDirect.COMPLETED)
                        .withCompleteReason(CompleteReason.BY_CLIENT),
                new GdiDeal()
                        .withStatus(StatusDirect.RECEIVED),
                new GdiDeal()
                        .withStatus(StatusDirect.COMPLETED)
                        .withCompleteReason(CompleteReason.BY_PUBLISHER)
        );
    }

    @Test
    public void testSortOrder() {
        List<GdDealOrderBy> orderBy = singletonList(new GdDealOrderBy()
                .withField(GdiDealOrderByField.STATUS)
                .withOrder(sortOrder));

        List<GdiDeal> result = dealDataService.getFilteredDeals(deals, null, null, orderBy);

        assertThat(result)
                .is(matchedBy(beanDiffer(dealsSorted)
                        .useCompareStrategy(DefaultCompareStrategies.onlyExpectedFields())
                ));
    }
}
