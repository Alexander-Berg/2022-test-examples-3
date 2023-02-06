package ru.yandex.direct.grid.core.entity.deal.service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.direct.core.entity.deal.container.DealStat;
import ru.yandex.direct.core.entity.deal.model.Deal;
import ru.yandex.direct.core.entity.deal.model.DealPlacement;
import ru.yandex.direct.core.entity.placements.model.Placement;
import ru.yandex.direct.currency.Percent;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.grid.core.entity.deal.model.GdiDeal;
import ru.yandex.direct.grid.core.entity.deal.model.GdiDealPlacement;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.testing.data.TestDeals.defaultPrivateDeal;
import static ru.yandex.direct.grid.core.entity.deal.service.GdiDealConverter.addStatToGdiDeal;
import static ru.yandex.direct.grid.core.entity.deal.service.GdiDealConverter.dealToGdiDeal;

public class GdiDealConverterTest {

    private static final ClientId CLIENT_ID = ClientId.fromLong(123L);
    private static final String NAME = "name";
    private static final String ADFOX_NAME = "adfoxName";
    private static final String DESCRIPTION = "description";
    private static final String ADFOX_DESCRIPTION = "adfoxDescription";
    private static final Percent AGENCY_FEE_PERCENT = Percent.fromPercent(BigDecimal.TEN);
    private static final Long CLICKS = 1L;
    private static final Long SHOWS = 5L;
    private static final BigDecimal SPENT = BigDecimal.valueOf(10L);
    private static final BigDecimal CPC = BigDecimal.valueOf(20L);
    private static final BigDecimal CPM = BigDecimal.valueOf(30L);
    private static final Percent CTR = Percent.fromPercent(BigDecimal.ONE);


    private Deal testDeal = defaultPrivateDeal(CLIENT_ID);

    @Test
    public void convertNullableFieldsIfNotNull() {
        GdiDeal expected = new GdiDeal()
                .withName(NAME)
                .withDescription(DESCRIPTION)
                .withAgencyFeePercent(AGENCY_FEE_PERCENT.asPercent());
        testDeal
                .withName(NAME)
                .withDescription(DESCRIPTION)
                .withAgencyFeePercent(AGENCY_FEE_PERCENT);
        GdiDeal actual = dealToGdiDeal(testDeal, new HashMap<>());
        assertThat(actual, beanDiffer(expected).useCompareStrategy(DefaultCompareStrategies.onlyExpectedFields()));
    }

    @Test
    public void convertNullableFieldsIfNull() {
        GdiDeal expected = new GdiDeal()
                .withName(ADFOX_NAME)
                .withDescription(ADFOX_DESCRIPTION)
                .withAgencyFeePercent(BigDecimal.ZERO);
        testDeal
                .withName(null)
                .withAdfoxName(ADFOX_NAME)
                .withDescription(null)
                .withAdfoxDescription(ADFOX_DESCRIPTION)
                .withAgencyFeePercent(null);
        GdiDeal actual = dealToGdiDeal(testDeal, new HashMap<>());
        assertThat(actual, beanDiffer(expected).useCompareStrategy(DefaultCompareStrategies.onlyExpectedFields()));
    }

    @Test
    public void convertPlacementsWithNulls() {
        Long pageId = 4L;
        String domain = "domain";
        DealPlacement dealPlacement1 = new DealPlacement()
                .withPageId(1L)
                .withImpId(asList(2L, 3L));
        DealPlacement dealPlacement2 = new DealPlacement()
                .withPageId(pageId)
                .withImpId(asList(5L, 6L));
        testDeal
                .withPlacements(asList(null, dealPlacement1, dealPlacement2));
        Map<Long, Placement> placementsMap = new HashMap<>();
        Placement placement = new Placement()
                .withPageId(pageId)
                .withDomain(domain);
        placementsMap.put(pageId, placement);
        GdiDeal expected = new GdiDeal()
                .withPlacements(singletonList(new GdiDealPlacement()
                        .withPageId(pageId)
                        .withDomain(domain)));
        GdiDeal actual = dealToGdiDeal(testDeal, placementsMap);
        assertThat(actual, beanDiffer(expected).useCompareStrategy(DefaultCompareStrategies.onlyExpectedFields()));
    }

    @Test
    public void addStatToGdiDealTest() {
        GdiDeal expected = new GdiDeal()
                .withClicks(CLICKS)
                .withShows(SHOWS)
                .withSpent(SPENT)
                .withCpc(CPC)
                .withCpm(CPM)
                .withCtr(CTR.asPercent());
        DealStat dealStat = new DealStat()
                .withShows(SHOWS)
                .withClicks(CLICKS)
                .withSpent(SPENT)
                .withCpc(CPC)
                .withCpm(CPM)
                .withCtr(CTR);
        GdiDeal gdiDeal = new GdiDeal();
        addStatToGdiDeal(gdiDeal, dealStat);
        assertThat(gdiDeal, beanDiffer(expected).useCompareStrategy(DefaultCompareStrategies.onlyExpectedFields()));
    }

    @Test
    public void addStatToGdiDealNullCtrTest() {
        DealStat dealStat = new DealStat()
                .withCtr(null);
        GdiDeal gdiDeal = new GdiDeal();
        addStatToGdiDeal(gdiDeal, dealStat);
        assertThat(gdiDeal.getCtr(), nullValue());
    }
}
