package ru.yandex.market.core.cutoff;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang.time.DateUtils;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.collections.CollectionFactory;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.cutoff.model.CutoffInfo;
import ru.yandex.market.core.cutoff.model.CutoffType;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.isIn;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

@DbUnitDataSet(before = "GeneralCutoffFunctionalTest.before.csv")
public class GeneralCutoffFunctionalTest extends FunctionalTest {

    private static final long SHOP_ID = 123;
    private static final long ACTION_ID = 789;

    @Autowired
    private CutoffService cutoffService;

    /**
     * Проверка корректности выдачи
     * {@link ru.yandex.market.core.cutoff.CutoffService#countClosedCutoffs(java.util.Collection,
     * ru.yandex.market.core.cutoff.model.CutoffType})
     */
    @Test
    @DbUnitDataSet(before = "countClosedCutoffs.before.csv")
    public void testCountClosedCutoffs() {

        assertTrue(cutoffService.countClosedCutoffs(Collections.singletonList(-1L), CutoffType.CPA_PARTNER).isEmpty());
        assertTrue(cutoffService.countClosedCutoffs(Collections.emptyList(), CutoffType.CPA_PARTNER).isEmpty());
        assertTrue(cutoffService.countClosedCutoffs(Collections.singletonList(1L), CutoffType.CPA_FEED).isEmpty());

        Map<Long, Integer> countClosedCutoffs =
                cutoffService.countClosedCutoffs(Arrays.asList(1L, 2L, 3L, 404L), CutoffType.CPA_PARTNER);

        MatcherAssert.assertThat(countClosedCutoffs.entrySet(),
                everyItem(isIn(ImmutableMap.of(1L, 1, 2L, 1).entrySet())));
    }

    @Test
    @DbUnitDataSet(before = "countClosedCutoffs.before.csv")
    public void testGetClosedCutoffs() throws Exception {
        Date dayBeforeYesterday = DateUtils.addDays(new Date(), -2);
        List<CutoffInfo> closedCutoffs = cutoffService.getClosedCutoffs(1, dayBeforeYesterday,
                CollectionFactory.set(CutoffType.FINANCE, CutoffType.QMANAGER_CHEESY, CutoffType.CPA_PARTNER));
        assertEquals(2, closedCutoffs.size());
        assertThat(closedCutoffs.stream().map(CutoffInfo::getType).collect(Collectors.toList()),
                containsInAnyOrder(CutoffType.FINANCE, CutoffType.CPA_PARTNER));
    }

    @Test
    @DbUnitDataSet(before = "countClosedCutoffs.before.csv")
    public void testGetClosedCutoffs_fromToday() throws Exception {
        List<CutoffInfo> closedCutoffs = cutoffService.getClosedCutoffs(1, new Date(), null);
        assertTrue(closedCutoffs.isEmpty());
    }

    private CutoffInfo openCutoff(CutoffType cutoffType) {
        return cutoffService.openCutoff(SHOP_ID, cutoffType, ACTION_ID);
    }

    private boolean closeCutoff(CutoffType cutoffType) {
        return cutoffService.closeCutoff(SHOP_ID, cutoffType, ACTION_ID);
    }

    private CutoffInfo getCutoff(CutoffType cutoffType) {
        return cutoffService.getCutoff(SHOP_ID, cutoffType);
    }

}
