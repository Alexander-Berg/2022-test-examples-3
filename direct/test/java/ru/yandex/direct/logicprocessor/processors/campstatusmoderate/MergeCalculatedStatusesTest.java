package ru.yandex.direct.logicprocessor.processors.campstatusmoderate;

import java.time.Duration;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.common.db.PpcProperty;
import ru.yandex.direct.common.db.PpcPropertyNames;
import ru.yandex.direct.config.DirectConfig;
import ru.yandex.direct.core.aggregatedstatuses.repository.AggregatedStatusesCampaignRepository;
import ru.yandex.direct.core.entity.campaign.aggrstatus.AggregatedStatusCampaign;
import ru.yandex.direct.core.entity.campaign.model.CampaignStatusModerate;
import ru.yandex.direct.core.entity.campaign.model.CampaignStatusPostmoderate;
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.entity.campaign.service.CampaignService;
import ru.yandex.direct.intapi.client.model.response.CampStatusModerate;
import ru.yandex.direct.logicprocessor.processors.campstatusmoderate.handlers.BalanceResyncHandler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MergeCalculatedStatusesTest {
    private AggregatedStatusesCampaignRepository aggregatedStatusesCampaignRepository;
    private CampaignService campaignService;
    private CampaignStatusModerateRepository campaignStatusModerateRepository;
    private CampaignStatusCalcService campaignStatusCalcService;
    private PpcPropertiesSupport ppcPropertiesSupport;
    private DirectConfig directConfig;
    private CampaignRepository campaignRepository;
    private BalanceResyncHandler balanceResyncHandler;

    @BeforeEach
    void before() {
        ppcPropertiesSupport = mock(PpcPropertiesSupport.class);
        directConfig = mock(DirectConfig.class);
        ppcPropertiesSupport = mock(PpcPropertiesSupport.class);

        PpcProperty<Integer> mockedProperty = mock(PpcProperty.class);
        when(mockedProperty.getOrDefault(anyInt())).thenReturn(0);
        when(ppcPropertiesSupport.get(
                eq(PpcPropertyNames.AGGREGATED_CAMP_STATUS_MODERATE_JAVA_PREFER_NEW_CALCULATOR),
                any(Duration.class))).thenReturn(mockedProperty);

        campaignStatusCalcService = new CampaignStatusCalcService(aggregatedStatusesCampaignRepository,
                campaignRepository, List.of(), campaignService, ppcPropertiesSupport, directConfig,
                balanceResyncHandler, null);
    }

    @SuppressWarnings("unused")
    static Stream<TestParameter> params() {
        return Stream.of(
                new TestParameter()
                        .withCampaign(
                                new AggregatedStatusCampaign()
                                        .withClientId(1L)
                                        .withStatusModerate(CampaignStatusModerate.SENT)
                                        .withStatusPostModerate(CampaignStatusPostmoderate.NEW))
                        .withStatus(
                                new CampaignStatus(
                                        CampaignStatusModerate.NO,
                                        CampaignStatusPostmoderate.ACCEPTED,
                                        true))
                        .withPerlStatus(new CampStatusModerate("Yes", "Accepted"))
                        .withExpectedResult(
                                new CampaignStatus(
                                        CampaignStatusModerate.YES, CampaignStatusPostmoderate.ACCEPTED, false)),
                new TestParameter()
                        .withCampaign(
                                new AggregatedStatusCampaign()
                                        .withClientId(1L)
                                        .withStatusModerate(CampaignStatusModerate.YES)
                                        .withStatusPostModerate(CampaignStatusPostmoderate.ACCEPTED))
                        .withStatus(null)
                        .withPerlStatus(new CampStatusModerate("Yes", "Accepted"))
                        .withExpectedResult(null),
                new TestParameter()
                        .withCampaign(
                                new AggregatedStatusCampaign()
                                        .withClientId(1L)
                                        .withStatusModerate(CampaignStatusModerate.SENT)
                                        .withStatusPostModerate(CampaignStatusPostmoderate.NEW))
                        .withStatus(null)
                        .withPerlStatus(new CampStatusModerate("Yes", "Accepted"))
                        .withExpectedResult(
                                new CampaignStatus(
                                        CampaignStatusModerate.YES, CampaignStatusPostmoderate.ACCEPTED, false)),
                new TestParameter()
                        .withCampaign(
                                new AggregatedStatusCampaign()
                                        .withClientId(1L)
                                        .withStatusModerate(CampaignStatusModerate.YES)
                                        .withStatusPostModerate(CampaignStatusPostmoderate.ACCEPTED))
                        .withStatus(null)
                        .withPerlStatus(new CampStatusModerate("Yes", "Accepted"))
                        // perl возвращает тот же статус, что и сейчас, поэтому возвращаем null
                        .withExpectedResult(null),
                new TestParameter()
                        .withCampaign(
                                new AggregatedStatusCampaign()
                                        .withClientId(1L)
                                        .withStatusModerate(CampaignStatusModerate.YES)
                                        .withStatusPostModerate(CampaignStatusPostmoderate.ACCEPTED))
                        .withStatus(null)
                        .withPerlStatus(new CampStatusModerate("Yes", "Accepted"))
                        .withExpectedResult(null)
        );
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("params")
    public void test(TestParameter parameter) {
        CampaignStatus result = campaignStatusCalcService.mergeCalculatedStatuses(
                parameter.campaign,
                parameter.status,
                parameter.perlStatus
        );
        if (parameter.expectedResult == null) {
            assertThat(result).isNull();
        } else {
            assertThat(result.getStatusModerate()).isEqualTo(parameter.expectedResult.getStatusModerate());
            assertThat(result.getStatusPostModerate()).isEqualTo(parameter.expectedResult.getStatusPostModerate());
        }
    }

    private static class TestParameter {
        AggregatedStatusCampaign campaign;
        CampaignStatus status;
        CampStatusModerate perlStatus;
        CampaignStatus expectedResult;

        public TestParameter withCampaign(AggregatedStatusCampaign campaign) {
            this.campaign = campaign;
            return this;
        }

        public TestParameter withStatus(CampaignStatus status) {
            this.status = status;
            return this;
        }

        public TestParameter withPerlStatus(CampStatusModerate perlStatus) {
            this.perlStatus = perlStatus;
            return this;
        }

        public TestParameter withExpectedResult(CampaignStatus expectedResult) {
            this.expectedResult = expectedResult;
            return this;
        }

        @Override
        public String toString() {
            return "TestParameter{" +
                    ", status=" + status +
                    ", perlStatus=" + perlStatus +
                    ", expectedResult=" + expectedResult +
                    '}';
        }
    }
}
