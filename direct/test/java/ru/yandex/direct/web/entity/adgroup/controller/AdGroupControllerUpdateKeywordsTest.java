package ru.yandex.direct.web.entity.adgroup.controller;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.keyword.model.Keyword;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.KeywordInfo;
import ru.yandex.direct.web.configuration.DirectWebTest;
import ru.yandex.direct.web.entity.adgroup.model.WebTextAdGroup;
import ru.yandex.direct.web.entity.keyword.model.WebKeyword;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.web.testing.data.TestAdGroups.randomNameWebAdGroup;
import static ru.yandex.direct.web.testing.data.TestKeywords.randomPhraseKeyword;

@SuppressWarnings("ConstantConditions")
@DirectWebTest
@RunWith(SpringRunner.class)
public class AdGroupControllerUpdateKeywordsTest extends TextAdGroupControllerTestBase {

    @Test
    public void update_AdGroupWithAddedKeyword_KeywordIsAdded() {
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup(campaignInfo);

        long adGroupId = adGroupInfo.getAdGroupId();

        WebTextAdGroup requestAdGroup = randomNameWebAdGroup(adGroupId, null);
        WebKeyword requestKeyword = randomPhraseKeyword(null);
        requestAdGroup.withKeywords(singletonList(requestKeyword));

        updateAndCheckResult(singletonList(requestAdGroup));

        List<Keyword> keywords = findKeywords();
        assertThat("должна быть добавлена одна ключевая фраза", keywords, hasSize(1));
        assertThat("данные добавленной фразы отличаются от ожидаемых",
                keywords.get(0).getPhrase(), equalTo(requestKeyword.getPhrase()));
    }

    @Test
    public void update_AdGroupWithUpdatedKeyword_KeywordIsUpdated() {
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup(campaignInfo);
        KeywordInfo keywordInfo = steps.keywordSteps().createKeyword(adGroupInfo);

        long adGroupId = adGroupInfo.getAdGroupId();

        WebTextAdGroup requestAdGroup = randomNameWebAdGroup(adGroupId, null);
        WebKeyword requestKeyword = randomPhraseKeyword(keywordInfo.getId());
        requestAdGroup.withKeywords(singletonList(requestKeyword));

        updateAndCheckResult(singletonList(requestAdGroup));

        List<Keyword> keywords = findKeywords();
        assertThat("должна быть одна ключевая фраза", keywords, hasSize(1));
        assertThat("данные обновленной фразы отличаются от ожидаемых",
                keywords.get(0).getPhrase(), equalTo(requestKeyword.getPhrase()));
    }

    @Test
    public void update_AdGroupWithDeletedKeyword_KeywordIsDeleted() {
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup(campaignInfo);
        steps.keywordSteps().createKeyword(adGroupInfo);

        long adGroupId = adGroupInfo.getAdGroupId();

        WebTextAdGroup requestAdGroup = randomNameWebAdGroup(adGroupId, null);

        updateAndCheckResult(singletonList(requestAdGroup));

        List<Keyword> keywords = findKeywords();
        assertThat("ключевая фраза должна быть удалена", keywords, emptyIterable());
    }

    @Test
    public void update_AdGroupWithAddedAndUpdatedAndDeletedKeywords_KeywordIsUpdated() {
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup(campaignInfo);
        KeywordInfo keywordInfo1 = steps.keywordSteps().createKeyword(adGroupInfo);
        steps.keywordSteps().createKeyword(adGroupInfo);

        long adGroupId = adGroupInfo.getAdGroupId();

        WebTextAdGroup requestAdGroup = randomNameWebAdGroup(adGroupId, null);
        WebKeyword requestAddedKeyword = randomPhraseKeyword(null);
        WebKeyword requestUpdatedKeyword = randomPhraseKeyword(keywordInfo1.getId());
        requestAdGroup.withKeywords(asList(requestUpdatedKeyword, requestAddedKeyword));

        updateAndCheckResult(singletonList(requestAdGroup));

        List<Keyword> keywords = findKeywords();
        assertThat("должно быть две ключевые фразы", keywords, hasSize(2));

        Keyword updatedKeyword = keywords.stream()
                .filter(k -> k.getId().equals(keywordInfo1.getId()))
                .findAny()
                .orElse(null);
        assertThat("данные обновленной фразы отличаются от ожидаемых",
                updatedKeyword.getPhrase(), equalTo(requestUpdatedKeyword.getPhrase()));

        Keyword addedKeyword = keywords.stream()
                .filter(k -> k != updatedKeyword)
                .findAny()
                .orElse(null);
        assertThat("данные добавленной фразы отличаются от ожидаемых",
                addedKeyword.getPhrase(), equalTo(requestAddedKeyword.getPhrase()));
    }
}
