package ru.yandex.direct.core.entity.masterreport;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.bannersystem.BannerSystemClient;
import ru.yandex.direct.bannersystem.container.masterreport.MasterReportDimension;
import ru.yandex.direct.bannersystem.container.masterreport.MasterReportGroupByDate;
import ru.yandex.direct.bannersystem.container.masterreport.MasterReportMetric;
import ru.yandex.direct.bannersystem.container.masterreport.MasterReportRequest;
import ru.yandex.direct.bannersystem.container.masterreport.MasterReportResponse;
import ru.yandex.direct.bannersystem.container.masterreport.MasterReportRow;
import ru.yandex.direct.core.aggregatedstatuses.AggregatedStatusesViewService;
import ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusEnum;
import ru.yandex.direct.core.entity.aggregatedstatuses.campaign.AggregatedStatusCampaignData;
import ru.yandex.direct.core.entity.campaign.model.CampaignAttributionModel;
import ru.yandex.direct.core.entity.campaign.model.CampaignCalcType;
import ru.yandex.direct.core.entity.campaign.service.CampaignService;
import ru.yandex.direct.core.entity.client.service.ClientService;
import ru.yandex.direct.core.entity.masterreport.model.MasterReportFilters;
import ru.yandex.direct.core.entity.masterreport.model.MasterReportMapping;
import ru.yandex.direct.core.entity.masterreport.model.MasterReportPeriod;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.multitype.entity.LimitOffset;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.entity.masterreport.MasterReportService.SUCCESSFUL_RESPONSE_CODE;
import static ru.yandex.direct.regions.Region.RUSSIA_REGION_ID;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class MasterReportServiceTest {

    private static final LocalDate FROM = LocalDate.of(2021, 11, 20);
    private static final LocalDate TO = LocalDate.of(2021, 11, 30);

    @Autowired
    protected Steps steps;

    @Autowired
    BannerSystemClient bannerSystemClient;
    @Autowired
    ClientService clientService;
    @Autowired
    CampaignService campaignService;
    @Mock
    private AggregatedStatusesViewService aggregatedStatusesViewService;

    MasterReportService masterReportService;

    @Captor
    ArgumentCaptor<MasterReportRequest> requestCaptor;

    UserInfo user;

    @Before
    public void before() {
        MockitoAnnotations.openMocks(this);
        masterReportService = new MasterReportService(
                bannerSystemClient,
                clientService,
                campaignService,
                aggregatedStatusesViewService
        );
        user = steps.userSteps().createDefaultUser();

        Mockito.when(bannerSystemClient.doRequest(any(), any(), any(), any()))
                .thenReturn(
                        new MasterReportResponse()
                                .setStatus(SUCCESSFUL_RESPONSE_CODE)
                                .setData(emptyList())
                                .setTotals(new MasterReportRow())
                );
    }

    @After
    public void after() {
        Mockito.clearInvocations(bannerSystemClient);
    }

    @Test
    public void getStatistics_checkSimpleRequest() {
        var campaign = steps.campaignSteps().createActiveCampaign(user.getClientInfo()).getCampaign();
        when(aggregatedStatusesViewService.getCampaignStatusesByIds(any(), anyCollection()))
                .thenReturn(
                        Map.of(
                                campaign.getId(),
                                new AggregatedStatusCampaignData(GdSelfStatusEnum.RUN_OK)
                        )
                );
        var dimensions = Set.of(MasterReportDimension.PLATFORM);
        var metrics = Set.of(MasterReportMetric.COST);
        masterReportService.getStatistics(
                user.getClientId(),
                user.getChiefUid(),
                new MasterReportPeriod(FROM, TO),
                MasterReportGroupByDate.NONE,
                CampaignAttributionModel.FIRST_CLICK,
                new MasterReportFilters(
                        emptySet(),
                        emptySet(),
                        emptySet(),
                        emptySet(),
                        Set.of(RUSSIA_REGION_ID),
                        CampaignCalcType.CPC,
                        emptySet()
                ),
                dimensions,
                metrics,
                LimitOffset.limited(10)
        );
        Mockito.verify(bannerSystemClient).doRequest(any(), requestCaptor.capture(), any(), any());

        var expectedRequest = new MasterReportRequest()
                .setWithVat(0)
                .setWithPerformanceCoverage(0)
                .setWithDiscount(1)
                .setDontGroupAndFilterZerosForTotals(1)
                .setCountableFieldsByTargettype(emptyList())
                .setDateFrom("20211120")
                .setDateTo("20211130")
                .setGroupByDate(MasterReportGroupByDate.NONE)
                .setGroupBy(dimensions)
                .setCountableFields(metrics)
                .setCurrency("RUB")
                .setMapping(
                        Map.of(
                                MasterReportMapping.TARGET_TYPE.toString(),
                                new MasterReportRequest.MappingItem()
                                        .setBy(MasterReportMapping.TARGET_TYPE.getByName())
                                        .setItemDefault(MasterReportMapping.TARGET_TYPE.getDefaultValue())
                                        .setDataType(MasterReportMapping.TARGET_TYPE.getDataType())
                                        .setMap(Map.of(campaign.getOrderId().toString(), campaign.getId().toString()))
                        )
                )
                .setFiltersPre(
                        Map.of(
                                MasterReportRequest.ReportColumn.ATTRIBUTION_TYPE,
                                Map.of(MasterReportRequest.FilterOperator.EQ, "1"),

                                MasterReportRequest.ReportColumn.REGION_ID,
                                Map.of(MasterReportRequest.FilterOperator.EQ, List.of(225L))
                        )
                )
                .setOrderIds(List.of(campaign.getOrderId().toString()))
                .setLimits(new MasterReportRequest.Limits().setLimit(10));

        assertThat(requestCaptor.getValue()).is(matchedBy(beanDiffer(expectedRequest)));

        steps.campaignSteps().deleteCampaign(user.getShard(), campaign.getId());
    }

}
