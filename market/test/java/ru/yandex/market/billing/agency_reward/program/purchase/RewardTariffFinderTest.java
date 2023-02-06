package ru.yandex.market.billing.agency_reward.program.purchase;

import java.util.Optional;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.billing.agency_reward.program.purchase.model.RewardTariff;
import ru.yandex.market.billing.agency_reward.program.purchase.tariff.AgencyCategoryTariffCacheService;
import ru.yandex.market.billing.agency_reward.program.purchase.tariff.RewardTariffFinder;
import ru.yandex.market.billing.agency_reward.program.purchase.tariff.TariffNotFoundException;
import ru.yandex.market.core.category.CategoryWalker;
import ru.yandex.market.core.fulfillment.model.ValueType;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

/**
 * Тесты для {@link RewardTariffFinder}.
 */
public class RewardTariffFinderTest {
    private AgencyCategoryTariffCacheService tariffCacheService;

    private RewardTariffFinder tariffFinder;

    @Before
    public void init() {
        final CategoryWalker categoryWalker = mock(CategoryWalker.class);
        doReturn(Optional.empty()).when(categoryWalker).getParentHyperIdForHyperId(anyLong());

        tariffCacheService = mock(AgencyCategoryTariffCacheService.class);
        doReturn(Optional.empty()).when(tariffCacheService).getTariff(anyLong(), anyLong());
        doReturn(Optional.of(1L)).when(categoryWalker).getParentHyperIdForHyperId(eq(2L));
        doReturn(Optional.of(2L)).when(categoryWalker).getParentHyperIdForHyperId(eq(3L));

        tariffFinder = new RewardTariffFinder(tariffCacheService, categoryWalker);
    }

    @Test
    public void thereIsCategoryWithAgency() {
        RewardTariff rewardTariff = new RewardTariff(3L, 991L, 600, ValueType.RELATIVE);
        doReturn(Optional.of(rewardTariff)).when(tariffCacheService).getTariff(eq(3L), eq(991L));
        assertEquals(rewardTariff, tariffFinder.find(3L, 991L));
    }

    @Test
    public void thereIsParentCategoryWithAgency() {
        RewardTariff rewardTariff = new RewardTariff(1L, 992L, 700, ValueType.RELATIVE);
        doReturn(Optional.of(rewardTariff)).when(tariffCacheService).getTariff(eq(1L), eq(992L));
        assertEquals(rewardTariff, tariffFinder.find(3L, 992L));
    }

    @Test
    public void thereIsCategoryWithNullAgency() {
        RewardTariff rewardTariff = new RewardTariff(3L, null, 800, ValueType.RELATIVE);
        doReturn(Optional.of(rewardTariff)).when(tariffCacheService).getTariff(eq(3L), isNull());
        assertEquals(rewardTariff, tariffFinder.find(3L, 993L));
    }

    @Test
    public void thereIsParentCategoryWithNullAgency() {
        RewardTariff rewardTariff = new RewardTariff(1L, null, 900, ValueType.RELATIVE);
        doReturn(Optional.of(rewardTariff)).when(tariffCacheService).getTariff(eq(1L), isNull());
        assertEquals(rewardTariff, tariffFinder.find(3L, 994L));
    }

    @Test(expected = TariffNotFoundException.class)
    public void noRootCategoryWithNullAgency() {
        RewardTariff rewardTariff = new RewardTariff(3L, 991L, 600, ValueType.RELATIVE);
        doReturn(Optional.of(rewardTariff)).when(tariffCacheService).getTariff(eq(3L), eq(991L));
        tariffFinder.find(3L, 994L);
    }
}
