package ru.yandex.direct.core.entity.keyword.repository;

import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.keyword.model.Keyword;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.KeywordInfo;
import ru.yandex.direct.core.testing.steps.Steps;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.testing.data.TestKeywords.defaultKeyword;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class KeywordRepositoryGetTest {

    @Autowired
    private Steps steps;
    @Autowired
    private KeywordRepository repo;

    @Test
    public void getKeywordsByIds() {
        CampaignInfo campaignInfo = steps.campaignSteps().createDefaultCampaign();
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createDefaultAdGroup(campaignInfo);
        KeywordInfo firstKeywordInfo = steps.keywordSteps().createKeyword(adGroupInfo);
        KeywordInfo secondKeywordInfo = steps.keywordSteps().createKeyword(adGroupInfo);
        List<Keyword> result = repo.getKeywordsByIds(
                adGroupInfo.getShard(), adGroupInfo.getClientId(), Collections.singletonList(firstKeywordInfo.getId()));
        assertThat("ожидаем одно значение", result.size(), equalTo(1));
    }

    @Test
    public void getKeywordsByIds_checkAutotargetingPrefix() {
        CampaignInfo campaignInfo = steps.campaignSteps().createDefaultCampaign();
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createDefaultAdGroup(campaignInfo);
        Keyword keyword = defaultKeyword().withIsAutotargeting(true);
        KeywordInfo firstKeywordInfo = steps.keywordSteps().createKeyword(adGroupInfo, keyword);
        List<Keyword> result = repo.getKeywordsByIds(
                adGroupInfo.getShard(), adGroupInfo.getClientId(), Collections.singletonList(firstKeywordInfo.getId()));
        assertThat("ожидаем одно значение", result.size(), equalTo(1));
        assertThat("ожидаем фразу", result.get(0).getPhrase(), equalTo(keyword.getPhrase()));
        assertThat("ожидаем флаг isAutotargeting", result.get(0).getIsAutotargeting(), is(true));
    }
}
