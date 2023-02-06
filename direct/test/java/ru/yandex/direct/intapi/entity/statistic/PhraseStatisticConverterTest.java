package ru.yandex.direct.intapi.entity.statistic;

import java.math.BigInteger;
import java.time.Period;
import java.util.Arrays;
import java.util.Collection;

import org.jooq.types.ULong;
import org.junit.Test;

import ru.yandex.direct.intapi.entity.statistic.model.PhraseStatisticConverter;
import ru.yandex.direct.intapi.entity.statistic.model.dynamic.GetDynamicStatisticsRequest;
import ru.yandex.direct.intapi.entity.statistic.model.dynamic.GetDynamicStatisticsRequestItem;
import ru.yandex.direct.intapi.entity.statistic.model.performance.GetPerformanceStatisticsRequest;
import ru.yandex.direct.intapi.entity.statistic.model.performance.GetPerformanceStatisticsRequestItem;
import ru.yandex.direct.intapi.entity.statistic.model.phrase.GetPhraseStatisticsRequest;
import ru.yandex.direct.intapi.entity.statistic.model.phrase.GetPhraseStatisticsRequestItem;
import ru.yandex.direct.intapi.entity.statistic.model.retargeting.GetRetargetingStatisticsRequest;
import ru.yandex.direct.intapi.entity.statistic.model.retargeting.GetRetargetingStatisticsRequestItem;
import ru.yandex.direct.ytcomponents.statistics.model.DateRange;
import ru.yandex.direct.ytcomponents.statistics.model.PhraseStatisticsRequest;
import ru.yandex.direct.ytcomponents.statistics.model.RetargetingStatisticsRequest;
import ru.yandex.direct.ytcomponents.statistics.model.ShowConditionStatisticsRequest;

import static com.google.common.primitives.Longs.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

public class PhraseStatisticConverterTest {

    @Test
    public void getDateRangeFromInterval_fromInclusiveCorrect() {
        int oneDayInterval = 1;
        DateRange dateRangeFromInterval =
                PhraseStatisticConverter.getDateRangeFromPeriod(Period.ofDays(oneDayInterval));
        assertThat(dateRangeFromInterval.getFromInclusive()).isEqualTo(dateRangeFromInterval.getToInclusive());
    }

    @Test
    public void toYtPhraseStatisticRequest_forGetPhraseStatisticsRequestWithBsPhraseId() {
        GetPhraseStatisticsRequest getPhraseStatisticsRequest =
                new GetPhraseStatisticsRequest()
                        .withPhraseStatisticsRequestItems(singletonList(
                                new GetPhraseStatisticsRequestItem()
                                        .withCampaignId(1L)
                                        .withAdGroupId(2L)
                                        .withBsPhraseIds(Arrays.asList(BigInteger.ONE, BigInteger.TEN))
                        ));

        Collection<PhraseStatisticsRequest> phraseStatisticsRequests =
                PhraseStatisticConverter.toYtPhraseStatisticRequest(getPhraseStatisticsRequest);
        assertThat(phraseStatisticsRequests)
                .extracting("campaignId", "adGroupId", "bannerId", "phraseId", "bsPhraseId")
                .containsExactly(
                        tuple(1L, 2L, null, null, ULong.valueOf(1L)),
                        tuple(1L, 2L, null, null, ULong.valueOf(10L))
                );
    }

    @Test
    public void toYtPhraseStatisticRequest_forGetPhraseStatisticsRequestWithPhraseId() {
        GetPhraseStatisticsRequest getPhraseStatisticsRequest =
                new GetPhraseStatisticsRequest()
                        .withPhraseStatisticsRequestItems(singletonList(
                                new GetPhraseStatisticsRequestItem()
                                        .withCampaignId(1L)
                                        .withAdGroupId(2L)
                                        .withPhraseIds(asList(11L, 12L))
                        ));

        Collection<PhraseStatisticsRequest> phraseStatisticsRequests =
                PhraseStatisticConverter.toYtPhraseStatisticRequest(getPhraseStatisticsRequest);
        assertThat(phraseStatisticsRequests)
                .extracting("campaignId", "adGroupId", "bannerId", "phraseId", "bsPhraseId")
                .containsExactly(
                        tuple(1L, 2L, null, 11L, null),
                        tuple(1L, 2L, null, 12L, null)
                );
    }

    @Test
    public void toYtPhraseStatisticRequest_forGetPhraseStatisticsRequestWithBannerIdAndBsPhraseId() {
        GetPhraseStatisticsRequest getPhraseStatisticsRequest =
                new GetPhraseStatisticsRequest()
                        .withPhraseStatisticsRequestItems(singletonList(
                                new GetPhraseStatisticsRequestItem()
                                        .withCampaignId(1L)
                                        .withAdGroupId(2L)
                                        .withBannerId(3L)
                                        .withBsPhraseIds(Arrays.asList(BigInteger.ONE, BigInteger.TEN))
                        ));

        Collection<PhraseStatisticsRequest> phraseStatisticsRequests =
                PhraseStatisticConverter.toYtPhraseStatisticRequest(getPhraseStatisticsRequest);
        assertThat(phraseStatisticsRequests)
                .extracting("campaignId", "adGroupId", "bannerId", "phraseId", "bsPhraseId")
                .containsExactly(
                        tuple(1L, 2L, 3L, null, ULong.valueOf(1L)),
                        tuple(1L, 2L, 3L, null, ULong.valueOf(10L))
                );
    }

    @Test
    public void toYtPhraseStatisticRequest_forGetPhraseStatisticsRequestWithBannerIdAndPhraseId() {
        GetPhraseStatisticsRequest getPhraseStatisticsRequest =
                new GetPhraseStatisticsRequest()
                        .withPhraseStatisticsRequestItems(singletonList(
                                new GetPhraseStatisticsRequestItem()
                                        .withCampaignId(1L)
                                        .withAdGroupId(2L)
                                        .withBannerId(3L)
                                        .withPhraseIds(asList(11L, 12L))
                        ));

        Collection<PhraseStatisticsRequest> phraseStatisticsRequests =
                PhraseStatisticConverter.toYtPhraseStatisticRequest(getPhraseStatisticsRequest);
        assertThat(phraseStatisticsRequests)
                .extracting("campaignId", "adGroupId", "bannerId", "phraseId", "bsPhraseId")
                .containsExactly(
                        tuple(1L, 2L, 3L, 11L, null),
                        tuple(1L, 2L, 3L, 12L, null)
                );
    }

    @Test
    public void toYtRetargetingStatisticRequest_forGetRetargetingStatisticsRequest() {
        GetRetargetingStatisticsRequest request =
                new GetRetargetingStatisticsRequest()
                        .withSelectionCriteria(singletonList(
                                new GetRetargetingStatisticsRequestItem()
                                        .withCampaignId(1L)
                                        .withAdGroupId(2L)
                                        .withRetargetingConditionIds(asList(11L, 12L))
                        ));

        Collection<RetargetingStatisticsRequest> internalRequests =
                PhraseStatisticConverter.toYtRetargetingStatisticRequest(request);
        assertThat(internalRequests)
                .extracting("campaignId", "adGroupId", "retargetingConditionId")
                .containsExactly(
                        tuple(1L, 2L, 11L),
                        tuple(1L, 2L, 12L)
                );
    }

    @Test
    public void toYtShowConditionStatisticRequest_forGetDynamicStatisticsRequest() {
        GetDynamicStatisticsRequest request =
                new GetDynamicStatisticsRequest()
                        .withSelectionCriteria(singletonList(
                                new GetDynamicStatisticsRequestItem()
                                        .withCampaignId(1L)
                                        .withAdGroupId(2L)
                                        .withDynamicConditionIds(asList(11L, 12L))
                        ));

        Collection<ShowConditionStatisticsRequest> internalRequests =
                PhraseStatisticConverter.toYtShowConditionStatisticRequest(request);
        assertThat(internalRequests)
                .extracting("campaignId", "adGroupId", "showConditionId")
                .containsExactly(
                        tuple(1L, 2L, 11L),
                        tuple(1L, 2L, 12L)
                );
    }

    @Test
    public void toYtShowConditionStatisticRequest_forGetPerformanceStatisticsRequest() {
        GetPerformanceStatisticsRequest request =
                new GetPerformanceStatisticsRequest()
                        .withSelectionCriteria(singletonList(
                                new GetPerformanceStatisticsRequestItem()
                                        .withCampaignId(1L)
                                        .withAdGroupId(2L)
                                        .withPerformanceFilterIds(asList(11L, 12L))
                        ));

        Collection<ShowConditionStatisticsRequest> internalRequests =
                PhraseStatisticConverter.toYtShowConditionStatisticRequest(request);
        assertThat(internalRequests)
                .extracting("campaignId", "adGroupId", "showConditionId")
                .containsExactly(
                        tuple(1L, 2L, 11L),
                        tuple(1L, 2L, 12L)
                );
    }
}
