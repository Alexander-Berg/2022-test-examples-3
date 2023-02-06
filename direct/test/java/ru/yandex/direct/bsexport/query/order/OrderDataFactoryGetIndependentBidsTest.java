package ru.yandex.direct.bsexport.query.order;

import org.junit.jupiter.api.Test;

import ru.yandex.direct.core.entity.campaign.model.CampOptionsStrategy;
import ru.yandex.direct.core.entity.campaign.model.CommonCampaign;
import ru.yandex.direct.core.entity.campaign.model.DbStrategy;
import ru.yandex.direct.core.entity.campaign.model.DynamicCampaign;
import ru.yandex.direct.core.entity.campaign.model.TextCampaign;
import ru.yandex.direct.core.entity.campaign.model.TextCampaignWithCustomStrategy;
import ru.yandex.direct.core.entity.campaign.model.WalletTypedCampaign;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.bsexport.query.order.OrderDataFactory.getIndependentBids;

class OrderDataFactoryGetIndependentBidsTest {

    private CommonCampaign campaign;

    @Test
    void walletCampaignDoesNotHaveStrategy_isNotIndependentBids() {
        campaign = new WalletTypedCampaign();

        assertThat(getIndependentBids(campaign)).isEqualTo(0);
    }

    @Test
    void commonCampaignDoesNotHaveStrategy_isNotIndependentBids() {
        campaign = mock(CommonCampaign.class);

        assertThat(getIndependentBids(campaign)).isEqualTo(0);
    }

    @Test
    void textCampaignWithoutDifferentPlaces_isNotIndependentBids() {
        DbStrategy dbStrategy = new DbStrategy();
        campaign = new TextCampaign().withStrategy(dbStrategy);

        assertThat(getIndependentBids(campaign)).isEqualTo(0);
    }

    @Test
    void textCampaignWithDifferentPlaces_isIndependentBids() {
        DbStrategy dbStrategy = new DbStrategy();
        dbStrategy.setStrategy(CampOptionsStrategy.DIFFERENT_PLACES);
        campaign = new TextCampaign().withStrategy(dbStrategy);

        assertThat(getIndependentBids(campaign)).isEqualTo(1);
    }

    @Test
    void dynamicCampaignWithoutDifferentPlaces_isNotIndependentBids() {
        DbStrategy dbStrategy = new DbStrategy();
        campaign = new DynamicCampaign().withStrategy(dbStrategy);

        assertThat(getIndependentBids(campaign)).isEqualTo(0);
    }

    @Test
    void dynamicCampaignWithDifferentPlaces_isIndependentBids() {
        DbStrategy dbStrategy = new DbStrategy();
        dbStrategy.setStrategy(CampOptionsStrategy.DIFFERENT_PLACES);
        campaign = new DynamicCampaign().withStrategy(dbStrategy);

        assertThat(getIndependentBids(campaign)).isEqualTo(1);
    }

    @Test
    void fakePerformanceCampaignWithoutDifferentPlaces_isNotIndependentBids() {
        DbStrategy dbStrategy = new DbStrategy();
        TextCampaignWithCustomStrategy mock = mock(TextCampaignWithCustomStrategy.class);
        when(mock.getStrategy()).thenReturn(dbStrategy);
        campaign = mock;

        assertThat(getIndependentBids(campaign)).isEqualTo(0);
    }

    @Test
    void fakePerformanceCampaignAutoBudgetAvgCpcPerFilter_isNotIndependentBids() {
        DbStrategy dbStrategy = new DbStrategy();
        dbStrategy.setStrategy(CampOptionsStrategy.AUTOBUDGET_AVG_CPC_PER_FILTER);
        TextCampaignWithCustomStrategy mock = mock(TextCampaignWithCustomStrategy.class);
        when(mock.getStrategy()).thenReturn(dbStrategy);
        campaign = mock;

        assertThat(getIndependentBids(campaign)).isEqualTo(0);
    }

    /**
     * Кажется, что стратегия должна быть всегда (в кампании со стратегией),
     * поэтому фиксируем поведение (NPE) в тесте
     */
    @Test
    void getIndependentBids_throwsExceptionIfStrategyIsNull() {
        campaign = new TextCampaign();

        assertThrows(NullPointerException.class, () -> getIndependentBids(campaign));
    }
}
