package ru.yandex.direct.core.entity.adgroup.service.complex.text.update;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.adgroup.container.ComplexTextAdGroup;
import ru.yandex.direct.core.entity.adgroup.service.complex.model.ComplexTextBanner;
import ru.yandex.direct.core.entity.keyword.model.Keyword;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.KeywordInfo;

import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.adgroup.service.complex.ComplexTextAdGroupTestData.fullTextBannerForAdd;
import static ru.yandex.direct.core.entity.adgroup.service.complex.ComplexTextAdGroupTestData.randomPhraseKeywordForAdd;
import static ru.yandex.direct.core.entity.adgroup.service.complex.ComplexTextAdGroupTestData.randomPhraseKeywordForUpdate;

/**
 * Тесты сохранения и удаления ключевых фраз в соответствующих группах
 */
@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class ComplexUpdateCommonDataTest extends ComplexAdGroupUpdateOperationTestBase {

    @Test
    public void oneAddedKeywordAndOneAddedBanner() {
        Keyword keyword = randomPhraseKeywordForAdd();
        ComplexTextBanner banner = fullTextBannerForAdd();
        ComplexTextAdGroup adGroup = createValidAdGroupForUpdate(adGroupInfo1)
                .withKeywords(singletonList(keyword))
                .withComplexBanners(singletonList(banner));

        updateAndCheckResultIsEntirelySuccessful(adGroup);

        List<String> adGroupKeywords = findKeywordsInAdGroup(adGroupInfo1.getAdGroupId());
        assertThat("в группе должна присутствовать добавленная фраза",
                adGroupKeywords,
                contains(keyword.getPhrase()));

        int clientKeywordsCount = getClientKeywordsCount();
        assertThat("общее количество ключевых фраз клиента не соответствует ожидаемому",
                clientKeywordsCount, is(1));

        List<String> titles = findBannerTitles(adGroupInfo1);
        assertThat(titles, contains(banner.getBanner().getTitle()));

        assertThat(getClientBannersCount(), is(1));
    }

    // обновление

    @Test
    public void oneUpdatedKeywordAndOneAddedBanner() {
        KeywordInfo keywordInfo = createKeyword(adGroupInfo1);

        Keyword keyword = randomPhraseKeywordForUpdate(keywordInfo.getId());
        ComplexTextBanner banner = fullTextBannerForAdd();
        ComplexTextAdGroup adGroup = createValidAdGroupForUpdate(adGroupInfo1)
                .withKeywords(singletonList(keyword))
                .withComplexBanners(singletonList(banner));

        updateAndCheckResultIsEntirelySuccessful(adGroup);

        List<String> adGroupKeywords = findKeywordsInAdGroup(adGroupInfo1.getAdGroupId());
        assertThat("в группе должна присутствовать обновленная фраза",
                adGroupKeywords,
                contains(keyword.getPhrase()));

        int clientKeywordsCount = getClientKeywordsCount();
        assertThat("общее количество ключевых фраз клиента не соответствует ожидаемому",
                clientKeywordsCount, is(1));

        List<String> titles = findBannerTitles(adGroupInfo1);
        assertThat(titles, contains(banner.getBanner().getTitle()));

        assertThat(getClientBannersCount(), is(1));
    }
}
