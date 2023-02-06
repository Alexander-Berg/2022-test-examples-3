package ru.yandex.direct.core.entity.campaign.service;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.banner.type.creative.model.CreativeSizeWithExpand;
import ru.yandex.direct.core.entity.campaign.model.CpmPriceCampaign;
import ru.yandex.direct.core.entity.campaign.model.PriceFlightTargetingsSnapshot;
import ru.yandex.direct.core.entity.pricepackage.model.ViewType;
import ru.yandex.direct.core.testing.configuration.CoreTest;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.entity.campaign.service.CampaignWithPricePackageUtils.collectCampaignCreativeSizesWithExpand;
import static ru.yandex.direct.core.entity.pricepackage.model.ViewType.DESKTOP;
import static ru.yandex.direct.core.entity.pricepackage.model.ViewType.MOBILE;
import static ru.yandex.direct.core.entity.pricepackage.model.ViewType.NEW_TAB;

@CoreTest
@RunWith(Parameterized.class)
public class CampaignWithPricePackageUtilsCollectCampaignCreativeSizesWithExpandTest {

    @Parameterized.Parameter(0)
    public String description;

    @Parameterized.Parameter(1)
    public CpmPriceCampaign campaign;

    @Parameterized.Parameter(2)
    public Set<Set<CreativeSizeWithExpand>> expectedCampaignCreativeSizesWithExpand;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {"десктоп и нтп; расхлоп разрешен", campaign(List.of(DESKTOP, NEW_TAB), true),
                        Set.of(
                                Set.of(new CreativeSizeWithExpand(1456L, 180L, true),
                                        new CreativeSizeWithExpand(1836L, 572L, true)),
                                Set.of(new CreativeSizeWithExpand(1456L, 180L, false))
                        )},
                {"десктоп и нтп; расхлоп запрещен", campaign(List.of(DESKTOP, NEW_TAB), false),
                        Set.of(
                                Set.of(new CreativeSizeWithExpand(1456L, 180L, false),
                                        new CreativeSizeWithExpand(1836L, 572L, false)),
                                Set.of(new CreativeSizeWithExpand(1456L, 180L, false))
                        )},
                {"мобилы; расхлоп разрешен", campaign(List.of(MOBILE), true),
                        Set.of(
                                Set.of(
                                        new CreativeSizeWithExpand(640L, 134L, false),
                                        new CreativeSizeWithExpand(640L, 268L, false),
                                        new CreativeSizeWithExpand(640L, 335L, false),
                                        new CreativeSizeWithExpand(640L, 201L, false)
                                )
                        )},
        });
    }

    private static CpmPriceCampaign campaign(List<ViewType> viewTypes, boolean allowExpandedDesktopCreative) {
        return new CpmPriceCampaign()
                .withFlightTargetingsSnapshot(new PriceFlightTargetingsSnapshot()
                        .withViewTypes(viewTypes)
                        .withAllowExpandedDesktopCreative(allowExpandedDesktopCreative));
    }

    @Test
    public void test() {
        assertThat(collectCampaignCreativeSizesWithExpand(campaign)).isEqualTo(expectedCampaignCreativeSizesWithExpand);
    }
}
