package ru.yandex.direct.teststeps.service;

import java.util.List;
import java.util.Set;

import javax.annotation.ParametersAreNonnullByDefault;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ru.yandex.direct.core.entity.keyword.container.CampaignIdAndKeywordIdPair;
import ru.yandex.direct.core.entity.keyword.model.StatusModerate;
import ru.yandex.direct.core.entity.keyword.repository.KeywordRepository;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.repository.TestKeywordRepository;
import ru.yandex.direct.core.testing.steps.KeywordSteps;
import ru.yandex.direct.dbutil.sharding.ShardHelper;

import static java.util.stream.Collectors.toList;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@Service
@ParametersAreNonnullByDefault
public class KeywordStepsService {

    private final KeywordSteps keywordSteps;
    private final InfoHelper infoHelper;
    private final ShardHelper shardHelper;
    private final TestKeywordRepository testKeywordRepository;
    private final KeywordRepository keywordRepository;

    @Autowired
    public KeywordStepsService(KeywordSteps keywordSteps,
                               InfoHelper infoHelper,
                               ShardHelper shardHelper,
                               TestKeywordRepository testKeywordRepository,
                               KeywordRepository keywordRepository) {
        this.keywordSteps = keywordSteps;
        this.infoHelper = infoHelper;
        this.shardHelper = shardHelper;
        this.testKeywordRepository = testKeywordRepository;
        this.keywordRepository = keywordRepository;
    }

    public List<Long> createKeywords(String login, Long adGroupId, List<String> keywords) {
        UserInfo userInfo = infoHelper.getUserInfo(login);
        ClientInfo clientInfo = infoHelper.getClientInfo(login, userInfo.getUid());

        AdGroupInfo adGroupInfo = infoHelper.getFullAdGroupInfo(adGroupId, clientInfo);

        return keywords.stream()
                .map(text -> keywordSteps.createKeywordWithText(text, adGroupInfo).getId())
                .collect(toList());
    }

    public void setStatusModerate(String login, Set<Long> keywordIds, StatusModerate statusModerate) {
        int shard = shardHelper.getShardByLoginStrictly(login);
        testKeywordRepository.setStatusModerate(shard, keywordIds, statusModerate);
    }

    public void suspendKeywords(String login, Set<Long> keywordIds) {
        int shard = shardHelper.getShardByLoginStrictly(login);
        testKeywordRepository.suspendKeywords(shard, keywordIds);
    }

    public void deleteKeywords(String login, Long campaignId, Set<Long> keywordIds) {
        int shard = shardHelper.getShardByLoginStrictly(login);

        List<CampaignIdAndKeywordIdPair> pairs =
                mapList(keywordIds, keywordId -> new CampaignIdAndKeywordIdPair(campaignId, keywordId));
        keywordRepository.deleteKeywords(shard, pairs);
    }
}
