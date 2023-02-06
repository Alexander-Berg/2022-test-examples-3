package ru.yandex.direct.logicprocessor.processors.campstatusmoderate;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.common.db.PpcProperty;
import ru.yandex.direct.common.db.PpcPropertyNames;
import ru.yandex.direct.config.DirectConfig;
import ru.yandex.direct.core.aggregatedstatuses.repository.AggregatedStatusesCampaignRepository;
import ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusEnum;
import ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusReason;
import ru.yandex.direct.core.entity.aggregatedstatuses.adgroup.AdGroupStatesEnum;
import ru.yandex.direct.core.entity.aggregatedstatuses.campaign.AggregatedStatusCampaignData;
import ru.yandex.direct.core.entity.aggregatedstatuses.campaign.CampaignCounters;
import ru.yandex.direct.core.entity.campaign.aggrstatus.AggregatedStatusCampaign;
import ru.yandex.direct.core.entity.campaign.model.CampaignStatusModerate;
import ru.yandex.direct.core.entity.campaign.model.CampaignStatusPostmoderate;
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.entity.campaign.service.CampaignService;
import ru.yandex.direct.logicprocessor.processors.campstatusmoderate.handlers.BalanceResyncHandler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusEnum.PAUSE_CRIT;
import static ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusEnum.PAUSE_OK;
import static ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusEnum.RUN_OK;
import static ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusEnum.RUN_WARN;
import static ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusEnum.STOP_CRIT;
import static ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusEnum.STOP_PROCESSING;
import static ru.yandex.direct.core.entity.aggregatedstatuses.campaign.CampaignStatesEnum.PAYED;

class CampaignStatusEventsProcessorTest {
    private AggregatedStatusesCampaignRepository aggregatedStatusesCampaignRepository;
    private CampaignService campaignService;
    private CampaignStatusModerateRepository campaignStatusModerateRepository;
    private CampaignStatusCalcService campaignStatusCalcService;
    private PpcPropertiesSupport ppcPropertiesSupport;
    private DirectConfig directConfig;
    private CampaignRepository campaignRepository;
    private BalanceResyncHandler balanceResyncHandler;

    static Stream<TestParameter> params() {
        return Stream.of(
                new TestParameter()
                        .setWouldBeMoneyBlocked(false)
                        .setGroupStatuses(List.of(RUN_OK))
                        .setCurrentStatusModerate(CampaignStatusModerate.SENT)
                        .setCurrentCampaignStatusPostmoderate(CampaignStatusPostmoderate.NEW)
                        .setExpectedResult(new CampaignStatus(CampaignStatusModerate.YES,
                                CampaignStatusPostmoderate.ACCEPTED, true)),
                new TestParameter()
                        .setWouldBeMoneyBlocked(false)
                        .setGroupStatuses(List.of(RUN_OK))
                        .setCurrentStatusModerate(CampaignStatusModerate.SENT)
                        .setCurrentCampaignStatusPostmoderate(CampaignStatusPostmoderate.NO)
                        .setExpectedResult(new CampaignStatus(CampaignStatusModerate.YES,
                                CampaignStatusPostmoderate.ACCEPTED, true)),

                new TestParameter()
                        .setWouldBeMoneyBlocked(false)
                        .setGroupStatuses(List.of(RUN_WARN))
                        .setCurrentStatusModerate(CampaignStatusModerate.SENT)
                        .setCurrentCampaignStatusPostmoderate(CampaignStatusPostmoderate.NEW)
                        .setExpectedResult(new CampaignStatus(CampaignStatusModerate.YES,
                                CampaignStatusPostmoderate.ACCEPTED,
                                true)),

                new TestParameter()
                        .setWouldBeMoneyBlocked(false)
                        .setGroupStatuses(List.of(RUN_OK))
                        .setCurrentStatusModerate(CampaignStatusModerate.NO)
                        .setCurrentCampaignStatusPostmoderate(CampaignStatusPostmoderate.NO)
                        .setExpectedResult(new CampaignStatus(CampaignStatusModerate.YES,
                                CampaignStatusPostmoderate.ACCEPTED, true)),
                new TestParameter()
                        .setWouldBeMoneyBlocked(false)
                        .setGroupStatuses(List.of(STOP_CRIT))
                        .setCurrentStatusModerate(CampaignStatusModerate.SENT)
                        .setCurrentCampaignStatusPostmoderate(CampaignStatusPostmoderate.NEW)
                        .setExpectedResult(new CampaignStatus(CampaignStatusModerate.NO,
                                CampaignStatusPostmoderate.NO, true)),

                new TestParameter()
                        .setWouldBeMoneyBlocked(false)
                        .setGroupStatuses(List.of(STOP_CRIT))
                        .setCurrentStatusModerate(CampaignStatusModerate.NO)
                        .setCurrentCampaignStatusPostmoderate(CampaignStatusPostmoderate.NO)
                        .setExpectedResult(null),
                new TestParameter()
                        .setWouldBeMoneyBlocked(false)
                        .setGroupStatuses(List.of(STOP_CRIT))
                        .setCurrentStatusModerate(CampaignStatusModerate.YES)
                        .setCurrentCampaignStatusPostmoderate(CampaignStatusPostmoderate.ACCEPTED)
                        .setExpectedResult(new CampaignStatus(CampaignStatusModerate.NO,
                                CampaignStatusPostmoderate.ACCEPTED, false)),


                new TestParameter()
                        .setWouldBeMoneyBlocked(false)
                        .setGroupStatuses(List.of(STOP_CRIT, PAUSE_CRIT))
                        .setCurrentStatusModerate(CampaignStatusModerate.NO)
                        .setCurrentCampaignStatusPostmoderate(CampaignStatusPostmoderate.NO)
                        .setExpectedResult(null),


                new TestParameter()
                        .setWouldBeMoneyBlocked(false)
                        .setGroupStatuses(List.of(PAUSE_OK))
                        .setCurrentStatusModerate(CampaignStatusModerate.YES)
                        .setCurrentCampaignStatusPostmoderate(CampaignStatusPostmoderate.ACCEPTED)
                        .setExpectedResult(null),


                new TestParameter()
                        .setWouldBeMoneyBlocked(false)
                        .setGroupStatuses(List.of(PAUSE_OK))
                        .setCurrentStatusModerate(CampaignStatusModerate.YES)
                        .setCurrentCampaignStatusPostmoderate(CampaignStatusPostmoderate.NEW)
                        .setExpectedResult(null),


                new TestParameter()
                        .setWouldBeMoneyBlocked(false)
                        .setGroupStatuses(List.of(RUN_OK, STOP_CRIT))
                        .setCurrentStatusModerate(CampaignStatusModerate.SENT)
                        .setCurrentCampaignStatusPostmoderate(CampaignStatusPostmoderate.NEW)
                        .setExpectedResult(new CampaignStatus(CampaignStatusModerate.YES,
                                CampaignStatusPostmoderate.ACCEPTED, true)),

                // особый случай для ДО
                new TestParameter()
                        .setWouldBeMoneyBlocked(false)
                        .setGroupStatuses(List.of(STOP_PROCESSING))
                        .setCurrentStatusModerate(CampaignStatusModerate.SENT)
                        .setCurrentCampaignStatusPostmoderate(CampaignStatusPostmoderate.NEW)
                        .setExpectedResult(new CampaignStatus(CampaignStatusModerate.YES,
                                CampaignStatusPostmoderate.ACCEPTED, true))
        );
    }

    @BeforeEach
    void before() {
        aggregatedStatusesCampaignRepository = mock(AggregatedStatusesCampaignRepository.class);
        campaignService = mock(CampaignService.class);
        campaignStatusModerateRepository = mock(CampaignStatusModerateRepositoryImpl.class);
        ppcPropertiesSupport = mock(PpcPropertiesSupport.class);
        directConfig = mock(DirectConfig.class);

        PpcProperty<Integer> mockedProperty = mock(PpcProperty.class);
        when(mockedProperty.getOrDefault(anyInt())).thenReturn(100);
        when(ppcPropertiesSupport.get(eq(PpcPropertyNames.AGGREGATED_CAMP_STATUS_MODERATE), any(Duration.class))).thenReturn(mockedProperty);

        campaignStatusCalcService = new CampaignStatusCalcService(aggregatedStatusesCampaignRepository,
                campaignRepository, List.of(), campaignService, ppcPropertiesSupport, directConfig,
                balanceResyncHandler, null);
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("params")
    void check(TestParameter parameter) {
        AggregatedStatusCampaign aggregatedStatusCampaign = makeAggregatedStatusCampaign(parameter);
        var result = campaignStatusCalcService.processOneCampaign(aggregatedStatusCampaign,
                parameter.wouldBeMoneyBlocked);
        Assertions.assertThat(result).isEqualTo(parameter.expectedResult);
    }

    private AggregatedStatusCampaign makeAggregatedStatusCampaign(TestParameter parameter) {
        return new AggregatedStatusCampaign()
                .withId(8964L)
                .withStatusModerate(parameter.currentStatusModerate)
                .withStatusPostModerate(parameter.currentCampaignStatusPostmoderate)
                .withAggregatedStatus(makeAggregatedStatus(parameter));
    }

    //{"r": ["NOTHING"], "s": "RUN_OK", "sts": ["PAYED"], "cnts": {"s": {"RUN_OK": 1}, "grps": 1}}

    private AggregatedStatusCampaignData makeAggregatedStatus(TestParameter parameter) {
        Map<GdSelfStatusEnum, Integer> statuses = new HashMap<>();
        Map<AdGroupStatesEnum, Integer> states = new HashMap<>();

        for (var status : parameter.groupStatuses) {
            statuses.put(status, 1);
        }

        return new AggregatedStatusCampaignData(List.of(PAYED),
                new CampaignCounters(parameter.groupStatuses.size(), statuses, states),
                RUN_OK,
                List.of(GdSelfStatusReason.NOTHING)
        );
    }

    private static class TestParameter {
        Boolean wouldBeMoneyBlocked;
        List<GdSelfStatusEnum> groupStatuses;
        CampaignStatusModerate currentStatusModerate;
        CampaignStatusPostmoderate currentCampaignStatusPostmoderate;
        CampaignStatus expectedResult;

        TestParameter setWouldBeMoneyBlocked(Boolean wouldBeMoneyBlocked) {
            this.wouldBeMoneyBlocked = wouldBeMoneyBlocked;
            return this;
        }

        TestParameter setGroupStatuses(List<GdSelfStatusEnum> groupStatuses) {
            this.groupStatuses = groupStatuses;
            return this;
        }

        TestParameter setCurrentStatusModerate(CampaignStatusModerate currentStatusModerate) {
            this.currentStatusModerate = currentStatusModerate;
            return this;
        }

        TestParameter setCurrentCampaignStatusPostmoderate(CampaignStatusPostmoderate currentCampaignStatusPostmoderate) {
            this.currentCampaignStatusPostmoderate = currentCampaignStatusPostmoderate;
            return this;
        }

        TestParameter setExpectedResult(CampaignStatus expectedResult) {
            this.expectedResult = expectedResult;
            return this;
        }

        @Override
        public String toString() {
            return "TestParameter{" +
                    "groupStatuses=" + groupStatuses +
                    ", currentStatusModerate=" + currentStatusModerate +
                    ", currentCampaignStatusPostmoderate=" + currentCampaignStatusPostmoderate +
                    '}';
        }
    }
}
