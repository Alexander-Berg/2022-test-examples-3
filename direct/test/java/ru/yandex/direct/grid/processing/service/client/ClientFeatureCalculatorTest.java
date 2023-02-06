package ru.yandex.direct.grid.processing.service.client;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.naming.TestCaseName;
import org.junit.Test;
import org.junit.runner.RunWith;

import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.grid.model.campaign.GdCampaignType;
import ru.yandex.direct.grid.model.campaign.GdiBaseCampaign;
import ru.yandex.direct.grid.model.campaign.GdiCampaignMediaplanStatus;
import ru.yandex.direct.grid.model.campaign.GdiCampaignStrategyName;
import ru.yandex.direct.grid.processing.model.client.GdClientFeatures;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.grid.core.util.GridCampaignTestUtil.defaultCampaign;
import static ru.yandex.direct.grid.model.campaign.GdiCampaignSource.DIRECT;
import static ru.yandex.direct.grid.model.campaign.GdiCampaignSource.UAC;
import static ru.yandex.direct.grid.processing.service.client.ClientFeatureCalculator.FEATURE_CALCULATOR;

@RunWith(JUnitParamsRunner.class)
@ParametersAreNonnullByDefault
public class ClientFeatureCalculatorTest {

    private static final List<GdiBaseCampaign> TEST_CAMPAIGNS = ImmutableList.of(
            defaultCampaign()
                    .withWalletId(15L)
                    .withMetrikaCounters(null)
                    .withType(CampaignType.TEXT)
                    .withIsUniversal(true)
                    .withSource(DIRECT),
            defaultCampaign()
                    .withStrategyName(GdiCampaignStrategyName.AUTOBUDGET_AVG_CPA)
                    .withMetrikaCounters(List.of())
                    .withType(CampaignType.PERFORMANCE)
                    .withIsUniversal(true)
                    .withSource(UAC),
            defaultCampaign()
                    .withMediaplanStatus(GdiCampaignMediaplanStatus.COMPLETE)
                    .withHasMediaplanBanners(true)
                    .withHasNewMediaplan(true)
                    .withHasEcommerce(true)
                    .withMetrikaCounters(null)
                    .withType(CampaignType.MCBANNER)
                    .withSource(DIRECT),
            defaultCampaign()
                    .withMetrikaCounters(List.of(1))
                    .withType(CampaignType.TEXT)
                    .withShows(10L)
                    .withSource(UAC)
    );

    public static Collection<Object[]> parameters() {
        return Arrays.asList(new Object[][]{
                {
                        0, 0,
                        new GdClientFeatures()
                                .withHasEcommerce(false)
                                .withHasCampaignsWithCPAStrategy(false)
                                .withHasMetrikaCounters(false)
                                .withIsWalletEnabled(false)
                                .withHasCampaignsWithStats(false)
                                .withHasCampaignsWithShows(false)
                                .withHasCampaignsWithCompletedMediaplan(false)
                                .withCampaignIdForVCardsManagement(null)
                                .withUsedCampaignTypes(Collections.emptySet())
                                .withCampaignManagers(Collections.emptySet())
                                .withHasNonUniversalCampaign(false)
                                .withHasUniversalCampaign(false)
                },
                {
                        0, 2,
                        new GdClientFeatures()
                                .withHasEcommerce(false)
                                .withHasCampaignsWithCPAStrategy(true)
                                .withHasMetrikaCounters(false)
                                .withIsWalletEnabled(true)
                                .withHasCampaignsWithStats(true)
                                .withHasCampaignsWithShows(false)
                                .withHasCampaignsWithCompletedMediaplan(false)
                                .withCampaignIdForVCardsManagement(TEST_CAMPAIGNS.get(0).getId())
                                .withUsedCampaignTypes(ImmutableSet.of(GdCampaignType.TEXT, GdCampaignType.PERFORMANCE))
                                .withCampaignManagers(Collections.emptySet())
                                .withHasNonUniversalCampaign(false)
                                .withHasUniversalCampaign(true)
                },
                {
                        0, TEST_CAMPAIGNS.size(),
                        new GdClientFeatures()
                                .withHasEcommerce(true)
                                .withHasCampaignsWithCPAStrategy(true)
                                .withHasMetrikaCounters(true)
                                .withIsWalletEnabled(true)
                                .withHasCampaignsWithStats(true)
                                .withHasCampaignsWithShows(true)
                                .withHasCampaignsWithCompletedMediaplan(true)
                                .withCampaignIdForVCardsManagement(TEST_CAMPAIGNS.get(0).getId())
                                .withUsedCampaignTypes(ImmutableSet
                                        .of(GdCampaignType.TEXT, GdCampaignType.PERFORMANCE, GdCampaignType.MCBANNER))
                                .withCampaignManagers(Collections.emptySet())
                                .withHasNonUniversalCampaign(true)
                                .withHasUniversalCampaign(true)
                },
        });
    }


    @Test
    @Parameters(method = "parameters")
    @TestCaseName("{0} - {1}")
    public void testCalculator(int firstIndex, int lastIndex, GdClientFeatures expectedFeatures) {
        assertThat(FEATURE_CALCULATOR.apply(TEST_CAMPAIGNS.subList(firstIndex, lastIndex)))
                .isEqualTo(expectedFeatures);
    }
}
