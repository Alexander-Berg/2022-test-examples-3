package ru.yandex.direct.core.entity.keyword.service;

import java.util.List;
import java.util.Map;

import one.util.streamex.StreamEx;

import ru.yandex.direct.core.entity.campaign.model.Campaign;
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.entity.keyword.model.Keyword;
import ru.yandex.direct.core.testing.steps.ClientSteps;

import static ru.yandex.direct.utils.FunctionalUtils.listToMap;

public class KeywordTestUtils {

    private KeywordTestUtils() {
    }

    /**
     * ВНИМАНИЕ! метод сработает только для ключевых слов в шарде {@code ClientSteps.DEFAULT_SHARD}
     */
    public static Map<Long, Campaign> getCampaignMap(CampaignRepository campaignRepository, Keyword... keywords) {
        List<Long> campaignIds = StreamEx.of(keywords).map(Keyword::getCampaignId).toList();
        List<Campaign> campaigns = campaignRepository.getCampaigns(ClientSteps.DEFAULT_SHARD, campaignIds);
        return listToMap(campaigns, Campaign::getId);
    }
}
