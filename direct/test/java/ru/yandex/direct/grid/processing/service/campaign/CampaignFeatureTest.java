package ru.yandex.direct.grid.processing.service.campaign;

import java.util.Collections;
import java.util.Set;

import javax.annotation.ParametersAreNonnullByDefault;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.naming.TestCaseName;
import org.junit.Test;
import org.junit.runner.RunWith;

import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.grid.core.util.GridCampaignTestUtil;
import ru.yandex.direct.grid.model.campaign.GdCampaignFeature;
import ru.yandex.direct.grid.model.campaign.GdiCampaign;
import ru.yandex.direct.grid.model.campaign.GdiCampaignMediaplanStatus;
import ru.yandex.direct.grid.model.campaign.strategy.GdCampaignFlatStrategy;
import ru.yandex.direct.grid.model.entity.campaign.converter.GdCampaignFeatureHelper;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.grid.model.campaign.GdCampaignFeature.HAS_ACTIVE_ADS;
import static ru.yandex.direct.grid.model.campaign.GdCampaignFeature.HAS_ADS;
import static ru.yandex.direct.grid.model.campaign.GdCampaignFeature.HAS_BID_MODIFIERS;
import static ru.yandex.direct.grid.model.campaign.GdCampaignFeature.HAS_COMPLETED_MEDIAPLAN;
import static ru.yandex.direct.grid.model.campaign.GdCampaignFeature.HAS_NOT_ARCHIVED_ADS;
import static ru.yandex.direct.grid.model.campaign.GdCampaignFeature.IS_AIMING_ALLOWED;
import static ru.yandex.direct.grid.model.campaign.GdCampaignFeature.SHOW_GENERAL_PRICE_ON_GROUP_EDIT;
import static ru.yandex.direct.grid.processing.util.CampaignTestDataUtils.defaultGdCampaignStrategyAvgCpa;
import static ru.yandex.direct.grid.processing.util.CampaignTestDataUtils.defaultGdCampaignStrategyManual;

@RunWith(JUnitParamsRunner.class)
@ParametersAreNonnullByDefault
public class CampaignFeatureTest {

    @SuppressWarnings("unused")
    private Object[] parametersForCheckGetCampaignFeatures() {
        return new Object[][]{
                {"campaign with all features", defaultCampaign(), defaultGdCampaignStrategyManual(),
                        Set.of(IS_AIMING_ALLOWED, SHOW_GENERAL_PRICE_ON_GROUP_EDIT, HAS_NOT_ARCHIVED_ADS, HAS_ADS,
                                HAS_ACTIVE_ADS, HAS_BID_MODIFIERS, HAS_COMPLETED_MEDIAPLAN)
                },
                {"campaign without features", campaignWithoutFeatures(), defaultGdCampaignStrategyAvgCpa(),
                        Collections.emptySet()},

        };
    }

    public static GdiCampaign defaultCampaign() {
        return GridCampaignTestUtil.defaultCampaign()
                .withHasBidModifiers(true)
                .withMediaplanStatus(GdiCampaignMediaplanStatus.COMPLETE)
                .withHasNewMediaplan(true)
                .withHasMediaplanBanners(true);
    }

    public static GdiCampaign campaignWithoutFeatures() {
        return GridCampaignTestUtil.defaultCampaign()
                .withType(CampaignType.MOBILE_CONTENT)
                .withHasBidModifiers(false)
                .withMediaplanStatus(null)
                .withHasBanners(false)
                .withHasActiveBanners(false)
                .withHasNotArchiveBanners(false);
    }


    @Test
    @Parameters(method = "parametersForCheckGetCampaignFeatures")
    @TestCaseName("{0}, expectedResult = {2}")
    public void checkGetCampaignFeatures(@SuppressWarnings("unused") String description,
                                         GdiCampaign gdiCampaign, GdCampaignFlatStrategy strategy,
                                         Set<GdCampaignFeature> expectedResult) {
        var actualResult = GdCampaignFeatureHelper.getCampaignFeatures(gdiCampaign, strategy);

        assertThat(actualResult)
                .isEqualTo(expectedResult);
    }

}
