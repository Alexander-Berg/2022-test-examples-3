package ru.yandex.direct.grid.processing.service.campaign;

import javax.annotation.ParametersAreNonnullByDefault;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.naming.TestCaseName;
import org.junit.Test;
import org.junit.runner.RunWith;

import ru.yandex.direct.grid.model.campaign.GdiCampaign;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.grid.core.util.GridCampaignTestUtil.defaultCampaign;

@RunWith(JUnitParamsRunner.class)
@ParametersAreNonnullByDefault
public class GridCampaignAggregationFieldsServiceAllowDomainMonitoringTest {

    @Test
    @Parameters(method = "testData")
    @TestCaseName("{0}")
    public void check_isAllowDomainMonitoringForCampaign(@SuppressWarnings("unused") String testCaseName,
                                                         GdiCampaign campaign, boolean expected) {
        assertThat(GridCampaignAggregationFieldsService.isAllowDomainMonitoringForCampaign(campaign))
                .isEqualTo(expected);
    }

    @SuppressWarnings("unused")
    private Object[] testData() {
        return new Object[][]{
                {
                        "allowDomainMonitoringForCampaign",
                        defaultCampaign(1)
                                .withEmpty(false)
                                .withArchived(false)
                                .withHasSiteMonitoring(true),
                        true
                },
                {
                        "Empty campaign",
                        defaultCampaign(1)
                                .withEmpty(true)
                                .withArchived(false)
                                .withHasSiteMonitoring(true),
                        false
                },
                {
                        "Archived campaign",
                        defaultCampaign(1)
                                .withEmpty(false)
                                .withArchived(true)
                                .withHasSiteMonitoring(true),
                        false
                },
                {
                        "Has No site monitoring",
                        defaultCampaign(1)
                                .withEmpty(false)
                                .withArchived(false)
                                .withHasSiteMonitoring(false),
                        false
                },
        };
    }

}
