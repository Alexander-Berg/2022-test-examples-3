package ru.yandex.direct.ytcomponents.statistics.repository;

import java.util.Map;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.ytcomponents.model.CampaignDeal;
import ru.yandex.direct.ytcomponents.model.DealStatsResponse;
import ru.yandex.direct.ytcomponents.spring.YtComponentsConfiguration;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

@Ignore("For manual run")
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = YtComponentsConfiguration.class)
public class CampaignStatRepositorySmokeTest {

    @Autowired
    private CampaignStatRepository campaignStatRepository;

    @Test
    public void campaignStatSmokeTest_withEmptyResult() {
        Map<Long, DealStatsResponse> actual =
                campaignStatRepository.getDealsStatistics(singletonList(new CampaignDeal()
                        .withCampaignId(1L)
                        .withDealId(1L))
                );
        assertThat(actual.keySet()).hasSize(0);
    }

    @Test
    public void campaignStatSmokeTest_withNonEmptyResult() {
        Map<Long, DealStatsResponse> actual =
                campaignStatRepository.getDealsStatistics(asList(
                        new CampaignDeal().withCampaignId(33452474L).withDealId(2000091L),
                        new CampaignDeal().withCampaignId(33452783L).withDealId(2000093L)
                ));
        assertThat(actual.keySet().size()).isEqualTo(2);
    }
}
