package ru.yandex.direct.grid.processing.service.campaign;

import java.util.List;
import java.util.Set;

import javax.annotation.ParametersAreNonnullByDefault;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.naming.TestCaseName;
import org.junit.Test;
import org.junit.runner.RunWith;

import ru.yandex.direct.grid.model.campaign.GdCampaign;
import ru.yandex.direct.grid.model.campaign.GdCampaignType;
import ru.yandex.direct.grid.processing.model.campaign.GdCampaignsFeatures;
import ru.yandex.direct.grid.processing.util.CampaignTestDataUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.grid.model.campaign.GdCampaignAction.EDIT_METRICA_COUNTERS;
import static ru.yandex.direct.grid.model.campaign.GdCampaignAction.RESET_FLIGHT_STATUS_APPROVE;
import static ru.yandex.direct.grid.model.campaign.GdCampaignPrimaryStatusDesc.IS_NOT_RECOVERED;

@RunWith(JUnitParamsRunner.class)
@ParametersAreNonnullByDefault
public class CampaignsFeaturesTest {

    @SuppressWarnings("unused")
    private Object[] parametersForCheckGetCampaignsFeatures() {
        return new Object[][]{
                {"campaigns with all features",
                        List.of(defaultCampaign()),
                        new GdCampaignsFeatures()
                                .withHasCurrencyArchivedCampaigns(true)
                                .withCanBeEditedMetrikaCountersCampaignsCount(1)
                                .withCanResetCampaignFlightStatusApproveCount(1)
                                .withCampaignTypes(Set.of(GdCampaignType.TEXT))
                },
                {"campaigns without features",
                        List.of(campaignWithoutFeatures()),
                        new GdCampaignsFeatures()
                                .withHasCurrencyArchivedCampaigns(false)
                                .withCanBeEditedMetrikaCountersCampaignsCount(0)
                                .withCanResetCampaignFlightStatusApproveCount(0)
                                .withCampaignTypes(Set.of(GdCampaignType.TEXT))
                },
                {"mixed campaigns",
                        List.of(defaultCampaign(), defaultCampaign(),
                                campaignWithoutFeatures().withType(GdCampaignType.CPM_PRICE)),
                        new GdCampaignsFeatures()
                                .withHasCurrencyArchivedCampaigns(true)
                                .withCanBeEditedMetrikaCountersCampaignsCount(2)
                                .withCanResetCampaignFlightStatusApproveCount(2)
                                .withCampaignTypes(Set.of(GdCampaignType.TEXT, GdCampaignType.CPM_PRICE))
                },

        };
    }

    public static GdCampaign defaultCampaign() {
        GdCampaign gdCampaign = CampaignTestDataUtils.defaultGdCampaign();
        gdCampaign.getStatus().withPrimaryStatusDesc(IS_NOT_RECOVERED);
        gdCampaign.getAccess().setActions(Set.of(EDIT_METRICA_COUNTERS, RESET_FLIGHT_STATUS_APPROVE));
        return gdCampaign;
    }

    public static GdCampaign campaignWithoutFeatures() {
        return CampaignTestDataUtils.defaultGdCampaign();
    }


    @Test
    @Parameters(method = "parametersForCheckGetCampaignsFeatures")
    @TestCaseName("{0}")
    public void checkGetCampaignsFeatures(@SuppressWarnings("unused") String description,
                                          List<GdCampaign> gdCampaigns, GdCampaignsFeatures expectedResult) {
        GdCampaignsFeatures actualResult = CampaignFeatureCalculator.FEATURE_CALCULATOR.apply(gdCampaigns);

        assertThat(actualResult)
                .isEqualTo(expectedResult);
    }

}
