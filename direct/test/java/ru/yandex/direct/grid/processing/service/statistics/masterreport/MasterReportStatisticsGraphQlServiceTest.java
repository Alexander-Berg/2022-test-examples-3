package ru.yandex.direct.grid.processing.service.statistics.masterreport;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import graphql.ExecutionResult;
import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.bannersystem.container.masterreport.MasterReportDimension;
import ru.yandex.direct.bannersystem.container.masterreport.MasterReportResponse;
import ru.yandex.direct.core.entity.campaign.model.CampaignsPlatform;
import ru.yandex.direct.core.entity.masterreport.MasterReportService;
import ru.yandex.direct.core.entity.masterreport.model.MasterReportPeriod;
import ru.yandex.direct.core.entity.metrika.service.MetrikaGoalsService;
import ru.yandex.direct.core.entity.retargeting.model.Goal;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.entity.user.service.UserService;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.grid.model.campaign.GdCampaignAttributionModel;
import ru.yandex.direct.grid.model.campaign.GdCampaignPlatform;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext;
import ru.yandex.direct.grid.processing.model.campaign.GdCalcType;
import ru.yandex.direct.grid.processing.model.masterreport.GdMasterReportStatisticsContainer;
import ru.yandex.direct.grid.processing.model.masterreport.GdMasterReportStatisticsFilter;
import ru.yandex.direct.grid.processing.model.masterreport.GdMasterReportStatisticsGroupBy;
import ru.yandex.direct.grid.processing.model.masterreport.GdMasterReportStatisticsGroupByDate;
import ru.yandex.direct.grid.processing.model.statistics.GdCampaignStatisticsPeriod;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.service.dataloader.GridContextProvider;
import ru.yandex.direct.grid.processing.service.statistics.service.MasterReportStatisticsServiceKt;
import ru.yandex.direct.grid.processing.util.ContextHelper;
import ru.yandex.direct.utils.JsonUtils;
import ru.yandex.misc.io.ClassPathResourceInputStreamSource;

import static java.util.Collections.emptySet;
import static org.assertj.core.util.BigDecimalComparator.BIG_DECIMAL_COMPARATOR;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.grid.processing.service.statistics.masterreport.MasterReportStatisticsRowBuilderKt.buildRow;
import static ru.yandex.direct.grid.processing.service.statistics.masterreport.MasterReportStatisticsRowBuilderKt.buildTotalRow;
import static ru.yandex.direct.grid.processing.util.GraphQLUtils.getDataValue;
import static ru.yandex.direct.grid.processing.util.GraphQLUtils.map;
import static ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.graphQlSerialize;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class MasterReportStatisticsGraphQlServiceTest {

    private static final String QUERY_TEMPLATE = "{\n" +
            "  client(searchBy: {login: \"%s\"}) {\n" +
            "    masterReportStatistics(input: %s) {\n" +
            "      totals {\n" +
            "        shows {\n" +
            "          value\n" +
            "          valueAbsDelta\n" +
            "        }\n" +
            "        clicks {\n" +
            "          value\n" +
            "          valueAbsDelta\n" +
            "        }\n" +
            "        ctr {\n" +
            "          value\n" +
            "          valueAbsDelta\n" +
            "        }\n" +
            "        conversions {\n" +
            "          value\n" +
            "          valueAbsDelta\n" +
            "        }\n" +
            "        conversionRate {\n" +
            "          value\n" +
            "          valueAbsDelta\n" +
            "        }\n" +
            "        cost {\n" +
            "          value\n" +
            "          valueAbsDelta\n" +
            "        }\n" +
            "        income {\n" +
            "          value\n" +
            "          valueAbsDelta\n" +
            "        }\n" +
            "        uniqViewers {\n" +
            "          value\n" +
            "          valueAbsDelta\n" +
            "        }\n" +
            "        roi {\n" +
            "          value\n" +
            "          valueAbsDelta\n" +
            "        }\n" +
            "        profit {\n" +
            "          value\n" +
            "          valueAbsDelta\n" +
            "        }\n" +
            "        costPerConversion {\n" +
            "          value\n" +
            "          valueAbsDelta\n" +
            "        }\n" +
            "        crr {\n" +
            "          value\n" +
            "          valueAbsDelta\n" +
            "        }\n" +
            "        avgCpc {\n" +
            "          value\n" +
            "          valueAbsDelta\n" +
            "        }\n" +
            "        bounceRatio {\n" +
            "          value\n" +
            "          valueAbsDelta\n" +
            "        }\n" +
            "        depth {\n" +
            "          value\n" +
            "          valueAbsDelta\n" +
            "        }\n" +
            "      }\n" +
            "      rowset {\n" +
            "        period\n" +
            "        campaign {\n" +
            "          id\n" +
            "          name\n" +
            "        }\n" +
            "        platform\n" +
            "        columnValues {\n" +
            "          goalStatistics {\n" +
            "            goalId\n" +
            "            name\n" +
            "            costPerConversion {\n" +
            "              value\n" +
            "            }\n" +
            "            conversionRate {\n" +
            "              value\n" +
            "            }\n" +
            "            conversions {\n" +
            "              value\n" +
            "            }\n" +
            "            income {\n" +
            "              value\n" +
            "            }\n" +
            "            profit {\n" +
            "              value\n" +
            "            }\n" +
            "            crr {\n" +
            "              value\n" +
            "            }\n" +
            "            roi {\n" +
            "              value\n" +
            "            }\n" +
            "          }\n" +
            "          shows {\n" +
            "            value\n" +
            "          }\n" +
            "          clicks {\n" +
            "            value\n" +
            "          }\n" +
            "          ctr {\n" +
            "            value\n" +
            "          }\n" +
            "          conversions {\n" +
            "            value\n" +
            "          }\n" +
            "          conversionRate {\n" +
            "            value\n" +
            "          }\n" +
            "          cost {\n" +
            "            value\n" +
            "          }\n" +
            "          income {\n" +
            "            value\n" +
            "          }\n" +
            "          uniqViewers {\n" +
            "            value\n" +
            "          }\n" +
            "          roi {\n" +
            "            value\n" +
            "          }\n" +
            "          profit {\n" +
            "            value\n" +
            "          }\n" +
            "          costPerConversion {\n" +
            "            value\n" +
            "          }\n" +
            "          crr {\n" +
            "            value\n" +
            "          }\n" +
            "          avgCpc {\n" +
            "            value\n" +
            "          }\n" +
            "          bounceRatio {\n" +
            "            value\n" +
            "          }\n" +
            "          depth {\n" +
            "            value\n" +
            "          }\n" +
            "        }\n" +
            "      }" +
            "    }" +
            "  }" +
            "}";
    private static final LocalDate FROM = LocalDate.of(2021, 11, 10);
    private static final LocalDate TO = LocalDate.of(2021, 11, 11);
    private static final GdCampaignStatisticsPeriod DEFAULT_PERIOD =
            new GdCampaignStatisticsPeriod().withFrom(FROM).withTo(TO);

    @Autowired
    private GridContextProvider gridContextProvider;

    @Autowired
    @Qualifier(GRAPH_QL_PROCESSOR)
    private GridGraphQLProcessor processor;
    @Autowired
    private Steps steps;
    @Autowired
    private UserService userService;
    @Autowired
    private MasterReportService masterReportService;
    @Autowired
    private MetrikaGoalsService metrikaGoalsService;

    private GridGraphQLContext context;

    private CampaignInfo firstCampaign;
    private CampaignInfo secondCampaign;

    @Before
    public void setUp() {
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        User user = userService.getUser(clientInfo.getUid());

        context = ContextHelper.buildContext(user);
        gridContextProvider.setGridContext(context);

        firstCampaign = steps.campaignSteps().createActiveTextCampaign(clientInfo);
        steps.campaignSteps().setPlatform(firstCampaign, CampaignsPlatform.CONTEXT);

        secondCampaign = steps.campaignSteps().createActiveTextCampaign(clientInfo);
    }

    @After
    public void after() {
        Mockito.clearInvocations(masterReportService);
    }

    @Test
    public void get_byCampaign() throws IOException {
        var source = new ClassPathResourceInputStreamSource("statistics.masterreport/by_campaign.json");
        var response = JsonUtils.MAPPER.readValue(
                source.getInput(),
                MasterReportResponse.class
        );
        var rows = response.getData();
        rows.get(0).setCampaignId(firstCampaign.getCampaignId());
        rows.get(1).setCampaignId(secondCampaign.getCampaignId());
        doReturn(response)
                .when(masterReportService).getStatistics(
                        any(), anyLong(), any(), any(), any(), any(), anySet(), anySet(), any()
                );

        var input = new GdMasterReportStatisticsContainer()
                .withGroupBy(GdMasterReportStatisticsGroupBy.CAMPAIGN)
                .withAttributionModel(GdCampaignAttributionModel.FIRST_CLICK)
                .withFilter(
                        new GdMasterReportStatisticsFilter()
                                .withPeriod(DEFAULT_PERIOD)
                                .withCalcType(GdCalcType.CPC)
                )
                .withNeedComparePeriods(false);

        var data = processQuery(input);

        var totals = buildTotalRow(
                14381, 457, 3.18, 559, 122.32, 996.26, 49696, 6232, 48.88, 48699.74, 1.78, 2, 2.18, 16.59, 3.01
        );

        var firstRow = buildRow(
                14291, 451, 3.16, 554, 122.84, 989.73, 49696, 6170, 49.21, 48706.27, 1.79, 1.99, 2.19, 16.78, 3.03, null
        );

        var secondRow = buildRow(
                90, 6, 6.67, 5, 83.33, 6.53, 0, 70, null, -6.53, 1.31, null, 1.09, 0, 1.6, null
        );

        var expected = getExpectedPayload(
                totals,
                getExpectedRow(null, firstCampaign, null, firstRow),
                getExpectedRow(null, secondCampaign, null, secondRow)
        );
        Assertions.assertThat(data)
                .usingComparatorForType(BIG_DECIMAL_COMPARATOR, BigDecimal.class)
                .usingRecursiveComparison()
                .isEqualTo(expected);
    }

    @Test
    public void get_byPlatform() throws IOException {
        var pSource = new ClassPathResourceInputStreamSource("statistics.masterreport/platform/by_platform.json");
        var platformResponse = JsonUtils.MAPPER.readValue(
                pSource.getInput(),
                MasterReportResponse.class
        );
        Mockito.when(
                masterReportService.getStatistics(
                        any(),
                        anyLong(),
                        any(),
                        any(),
                        any(),
                        any(),
                        eq(Set.of(MasterReportDimension.PLATFORM)),
                        anySet(),
                        any()
                )
        ).thenReturn(platformResponse);

        var cSource = new ClassPathResourceInputStreamSource("statistics.masterreport/platform/by_campaign.json");
        var campaignResponse = JsonUtils.MAPPER.readValue(
                cSource.getInput(),
                MasterReportResponse.class
        );
        var rows = campaignResponse.getData();
        assumeThat(rows, hasSize(3));
        rows.get(0).setCampaignId(firstCampaign.getCampaignId());
        rows.get(1).setCampaignId(firstCampaign.getCampaignId());
        rows.get(2).setCampaignId(secondCampaign.getCampaignId());
        Mockito.when(
                masterReportService.getStatistics(
                        any(),
                        anyLong(),
                        any(),
                        any(),
                        any(),
                        any(),
                        eq(Set.of(MasterReportDimension.CAMPAIGN, MasterReportDimension.PLATFORM)),
                        anySet(),
                        any()
                )
        ).thenReturn(campaignResponse);

        var input = new GdMasterReportStatisticsContainer()
                .withGroupBy(GdMasterReportStatisticsGroupBy.PLATFORM)
                .withAttributionModel(GdCampaignAttributionModel.FIRST_CLICK)
                .withFilter(
                        new GdMasterReportStatisticsFilter()
                                .withPeriod(new GdCampaignStatisticsPeriod().withFrom(FROM).withTo(TO))
                                .withCalcType(GdCalcType.CPC)
                )
                .withNeedComparePeriods(false);

        var data = processQuery(input);

        var totals = buildTotalRow(
                14381, 457, 3.18, 559, 122.32, 996.26, 49696, 6232, 48.88, 48699.74, 1.78, 2, 2.18, 16.59, 3.01
        );
        var row1 = buildRow(
                1299, 107, 8.24, 144, 134.58, 330.07, 44746, 930, 134.56, 44415.93, 2.29, 0.74, 3.08, 12.39, 3.47, null
        );
        var row2 = buildRow(
                13082, 350, 2.68, 415, 118.57, 666.19, 4950, 5470, 6.43, 4283.81, 1.61, 13.46, 1.9, 17.95, 2.87, null
        );
        var row3 = buildRow(
                1209, 101, 8.35, 139, 137.62, 323.54, 44746, 864, 137.3, 44422.46, 2.33, 0.72, 3.2, 12.96, 3.56, null
        );
        var row4 = buildRow(
                13082, 350, 2.68, 415, 118.57, 666.19, 4950, 5470, 6.43, 4283.81, 1.61, 13.46, 1.9, 17.95, 2.87, null
        );
        var row5 = buildRow(
                90, 6, 6.67, 5, 83.33, 6.53, 0, 70, null, -6.53, 1.31, null, 1.09, 0, 1.6, null
        );
        var expected = getExpectedPayload(
                totals,
                getExpectedRow(null, null, GdCampaignPlatform.SEARCH.name(), row1),
                getExpectedRow(null, null, GdCampaignPlatform.CONTEXT.name(), row2),
                getExpectedRow(null, firstCampaign, GdCampaignPlatform.SEARCH.name(), row3),
                getExpectedRow(null, firstCampaign, GdCampaignPlatform.CONTEXT.name(), row4),
                getExpectedRow(null, secondCampaign, GdCampaignPlatform.SEARCH.name(), row5)
        );
        Assertions.assertThat(data)
                .usingComparatorForType(BIG_DECIMAL_COMPARATOR, BigDecimal.class)
                .usingRecursiveComparison()
                .isEqualTo(expected);
    }

    @Test
    public void get_byDay() throws IOException {
        var source = new ClassPathResourceInputStreamSource("statistics.masterreport/day/by_day.json");
        var response = JsonUtils.MAPPER.readValue(
                source.getInput(),
                MasterReportResponse.class
        );
        Mockito.when(
                masterReportService.getStatistics(
                        any(),
                        anyLong(),
                        eq(new MasterReportPeriod(FROM, TO)),
                        any(),
                        any(),
                        any(),
                        eq(emptySet()),
                        anySet(),
                        any()
                )
        ).thenReturn(response);

        var cSource = new ClassPathResourceInputStreamSource("statistics.masterreport/day/by_day_compare.json");
        var cResponse = JsonUtils.MAPPER.readValue(
                cSource.getInput(),
                MasterReportResponse.class
        );
        Mockito.when(
                masterReportService.getStatistics(
                        any(),
                        anyLong(),
                        eq(new MasterReportPeriod(FROM.minusDays(2), FROM.minusDays(1))),
                        any(),
                        any(),
                        any(),
                        eq(emptySet()),
                        anySet(),
                        any()
                )
        ).thenReturn(cResponse);

        var input = new GdMasterReportStatisticsContainer()
                .withGroupByDate(GdMasterReportStatisticsGroupByDate.DAY)
                .withAttributionModel(GdCampaignAttributionModel.FIRST_CLICK)
                .withFilter(
                        new GdMasterReportStatisticsFilter()
                                .withPeriod(new GdCampaignStatisticsPeriod().withFrom(FROM).withTo(TO))
                                .withCalcType(GdCalcType.CPC)
                )
                .withNeedComparePeriods(true);

        var data = processQuery(input);

        var totals = buildTotalRow(
                32686, 1223, 1162, -52, 3.56, -0.3, 1418, -129, 122.03, -5.4, 2516.16, -34.04, 147003, 126860,
                13498, 230, 57.42, 50.52, 144486.84, 126894.04, 1.77, 0.13, 1.71, -10.95, 2.17, 0.06, 14.52, 0.04, 3.02,
                -0.25
        );

        var firstRow = buildRow(
                16353, 585, 3.58, 702, 120, 1230.81, 50296, 7549, 39.86, 49065.19, 1.75, 2.45, 2.1, 15.41, 2.86, null
        );

        var secondRow = buildRow(
                16333, 577, 3.53, 716, 124.09, 1285.36, 96707, 7947, 74.24, 95421.64, 1.80, 1.33, 2.23, 13.68, 3.18,
                null
        );

        var firstExpectedRow = getExpectedRow("2021-11-10", null, null, firstRow);
        var secondExpectedRow = getExpectedRow("2021-11-11", null, null, secondRow);

        var expected = getExpectedPayload(totals, firstExpectedRow, secondExpectedRow);
        Assertions.assertThat(data)
                .usingComparatorForType(BIG_DECIMAL_COMPARATOR, BigDecimal.class)
                .usingRecursiveComparison()
                .isEqualTo(expected);
    }

    @Test
    public void get_compareWithoutPrev() throws IOException {
        var source = new ClassPathResourceInputStreamSource("statistics.masterreport/day/by_day.json");
        var response = JsonUtils.MAPPER.readValue(
                source.getInput(),
                MasterReportResponse.class
        );
        Mockito.when(
                masterReportService.getStatistics(
                        any(),
                        anyLong(),
                        eq(new MasterReportPeriod(FROM, TO)),
                        any(),
                        any(),
                        any(),
                        eq(emptySet()),
                        anySet(),
                        any()
                )
        ).thenReturn(response);

        var cSource = new ClassPathResourceInputStreamSource("statistics.masterreport/day/empty_compare.json");
        var cResponse = JsonUtils.MAPPER.readValue(
                cSource.getInput(),
                MasterReportResponse.class
        );
        Mockito.when(
                masterReportService.getStatistics(
                        any(),
                        anyLong(),
                        eq(new MasterReportPeriod(FROM.minusDays(2), FROM.minusDays(1))),
                        any(),
                        any(),
                        any(),
                        eq(emptySet()),
                        anySet(),
                        any()
                )
        ).thenReturn(cResponse);

        var input = new GdMasterReportStatisticsContainer()
                .withGroupByDate(GdMasterReportStatisticsGroupByDate.DAY)
                .withAttributionModel(GdCampaignAttributionModel.FIRST_CLICK)
                .withFilter(
                        new GdMasterReportStatisticsFilter()
                                .withPeriod(new GdCampaignStatisticsPeriod().withFrom(FROM).withTo(TO))
                                .withCalcType(GdCalcType.CPC)
                )
                .withNeedComparePeriods(true);

        var data = processQuery(input);
        var actualTotals = getDataValue(data, "client/masterReportStatistics/totals/");

        var expectedTotals = buildTotalRow(
                32686, null, 1162, null, 3.56, null, 1418, null, 122.03, null, 2516.16, null, 147003, null,
                13498, null, 57.42, null, 144486.84, null, 1.77, null, 1.71, null, 2.17, null, 14.52, null, 3.02,
                null
        );
        Assertions.assertThat(actualTotals)
                .usingComparatorForType(BIG_DECIMAL_COMPARATOR, BigDecimal.class)
                .usingRecursiveComparison()
                .isEqualTo(expectedTotals);
    }

    @Test
    public void get_byQuarter() throws IOException {
        var source = new ClassPathResourceInputStreamSource("statistics.masterreport/quarter/by_quarter.json");
        var response = JsonUtils.MAPPER.readValue(
                source.getInput(),
                MasterReportResponse.class
        );
        Mockito.when(
                masterReportService.getStatistics(
                        any(),
                        anyLong(),
                        eq(new MasterReportPeriod(FROM.minusMonths(4), FROM)),
                        any(),
                        any(),
                        any(),
                        eq(emptySet()),
                        anySet(),
                        any()
                )
        ).thenReturn(response);

        var input = new GdMasterReportStatisticsContainer()
                .withGroupByDate(GdMasterReportStatisticsGroupByDate.QUARTER)
                .withAttributionModel(GdCampaignAttributionModel.FIRST_CLICK)
                .withFilter(
                        new GdMasterReportStatisticsFilter()
                                .withPeriod(new GdCampaignStatisticsPeriod().withFrom(FROM.minusMonths(4)).withTo(FROM))
                                .withCalcType(GdCalcType.CPC)
                )
                .withNeedComparePeriods(false);

        var data = processQuery(input);

        var totals = buildTotalRow(
                1123849, 36947, 3.29, 34990, 94.7, 78206.44, 0, 305449, null, -78206.44, 2.24, null, 2.12, 16.43, 2.64
        );

        var firstRow = buildRow(
                596004, 20071, 3.37, 18357, 91.46, 40961.33, 0, 192169, null, -40961.33, 2.23, null, 2.04, 17.34, 2.56,
                null
        );

        var secondRow = buildRow(
                527845, 16876, 3.2, 16633, 98.56, 37245.11, 0, 136378, null, -37245.11, 2.24, null, 2.21, 15.22, 2.74,
                null
        );

        var firstExpectedRow = getExpectedRow("2021-07-01", null, null, firstRow);
        var secondExpectedRow = getExpectedRow("2021-10-01", null, null, secondRow);

        var expected = getExpectedPayload(totals, firstExpectedRow, secondExpectedRow);
        Assertions.assertThat(data)
                .usingComparatorForType(BIG_DECIMAL_COMPARATOR, BigDecimal.class)
                .usingRecursiveComparison()
                .isEqualTo(expected);
    }

    @Test
    public void get_byGoal() throws IOException {
        var gSource = new ClassPathResourceInputStreamSource("statistics.masterreport/goal/by_goal.json");
        var goalResponse = JsonUtils.MAPPER.readValue(
                gSource.getInput(),
                MasterReportResponse.class
        );
        doReturn(goalResponse)
                .when(masterReportService).getStatistics(
                        any(),
                        anyLong(),
                        any(),
                        any(),
                        any(),
                        any(),
                        eq(emptySet()),
                        eq(MasterReportStatisticsServiceKt.MULTI_GOALS_METRICS),
                        any()
                );

        var cSource = new ClassPathResourceInputStreamSource("statistics.masterreport/goal/by_campaign.json");
        var campaignResponse = JsonUtils.MAPPER.readValue(
                cSource.getInput(),
                MasterReportResponse.class
        );
        var rows = campaignResponse.getData();
        assumeThat(rows, hasSize(2));
        rows.get(0).setCampaignId(firstCampaign.getCampaignId());
        rows.get(1).setCampaignId(secondCampaign.getCampaignId());

        var firstGoal = (Goal) (new Goal().withId(16361955L).withName("Цель первая"));
        var secondGoal = (Goal) (new Goal().withId(16261155L).withName("Цель вторая"));
        doReturn(Set.of(firstGoal, secondGoal))
                .when(metrikaGoalsService).getAvailableMetrikaGoalsForClient(any(), any());

        doReturn(campaignResponse)
                .when(masterReportService).getStatistics(
                        any(),
                        anyLong(),
                        any(),
                        any(),
                        any(),
                        any(),
                        eq(Set.of(MasterReportDimension.CAMPAIGN)),
                        eq(MasterReportStatisticsServiceKt.MULTI_GOALS_METRICS),
                        any()
                );

        var input = new GdMasterReportStatisticsContainer()
                .withGroupBy(GdMasterReportStatisticsGroupBy.GOAL)
                .withAttributionModel(GdCampaignAttributionModel.FIRST_CLICK)
                .withFilter(
                        new GdMasterReportStatisticsFilter()
                                .withPeriod(new GdCampaignStatisticsPeriod().withFrom(FROM).withTo(TO))
                                .withCalcType(GdCalcType.CPC)
                )
                .withNeedComparePeriods(false);

        var data = processQuery(input);

        var totals = buildTotalRow(
                null, null, null, 17, null, 252, null, null, null, null, 4.6, null, null, null, null
        );
        var row1 = buildRow(
                null, null, null, 17, null, 252, null, null, null, null, 4.6, null, null, null, null,
                List.of(
                        buildExpectedGoalStatistics(secondGoal, 11, 25.58, 7.12, 7252, -0.78, 444.91, 7000),
                        buildExpectedGoalStatistics(firstGoal, 6, 13.95, 13.05, 1252, -0.68, 56.98, 1000)
                )
        );
        var row2 = buildRow(
                null, null, null, 8, null, 202, null, null, null, null, 1.17, null, null, null, null,
                List.of(
                        buildExpectedGoalStatistics(firstGoal, 6, 75, 1.56, 1152, -0.68, 56.98, 950),
                        buildExpectedGoalStatistics(secondGoal, 2, 25, 4.67, 52, -0.28, 200.91, -150)
                )
        );
        var row3 = buildRow(
                null, null, null, 9, null, 50, null, null, null, null, 7.66, null, null, null, null,
                List.of(
                        buildExpectedGoalStatistics(secondGoal, 9, 25.71, 7.66, 6000, -0.48, 244.91, 5950)
                )
        );
        var expected = getExpectedPayload(
                totals,
                getExpectedRow(null, null, null, row1),
                getExpectedRow(null, firstCampaign, null, row2),
                getExpectedRow(null, secondCampaign, null, row3)
        );
        Assertions.assertThat(data)
                .usingComparatorForType(BIG_DECIMAL_COMPARATOR, BigDecimal.class)
                .usingRecursiveComparison()
                .isEqualTo(expected);
    }

    private Map<String, Object> getExpectedPayload(
            Map<String, Map<String, Number>> totals,
            Map<String, Object>... rows
    ) {
        return map(
                "client", map(
                        "masterReportStatistics", map(
                                "totals", totals,
                                "rowset", Arrays.asList(rows)
                        )
                )
        );
    }

    private Map<String, Object> getExpectedRow(
            String period,
            CampaignInfo campaignInfo,
            String platform,
            Map<String, Object> values
    ) {
        var campaignMap = campaignInfo == null
                ? null
                : map(
                "id", campaignInfo.getCampaignId(),
                "name", campaignInfo.getCampaign().getName()
        );
        return map(
                "period", period,
                "campaign", campaignMap,
                "platform", platform,
                "columnValues", values
        );
    }

    private Map<String, Object> buildExpectedGoalStatistics(
            Goal goal,
            int conversions,
            double conversionRate,
            double costPerConversion,
            double income,
            double roi,
            double crr,
            double profit
    ) {
        return map(
                "goalId", goal.getId(),
                "name", goal.getName(),
                "conversions", conversions,
                "conversionRate", conversionRate,
                "costPerConversion", costPerConversion,
                "income", income,
                "roi", roi,
                "crr", crr,
                "profit", profit
        );
    }

    private Map<String, Object> processQuery(GdMasterReportStatisticsContainer input) {
        var query = String.format(QUERY_TEMPLATE, context.getOperator().getLogin(), graphQlSerialize(input));
        ExecutionResult result = processor.processQuery(null, query, null, context);
        assumeThat(result.getErrors(), hasSize(0));
        return result.getData();
    }

}
