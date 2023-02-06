package ru.yandex.direct.web.entity.adgroup.controller;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import one.util.streamex.StreamEx;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.keyword.model.AutoBudgetPriority;
import ru.yandex.direct.core.entity.keyword.model.Keyword;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.KeywordInfo;
import ru.yandex.direct.web.configuration.DirectWebTest;
import ru.yandex.direct.web.entity.adgroup.model.WebTextAdGroup;
import ru.yandex.direct.web.entity.keyword.model.WebKeyword;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.core.testing.data.TestKeywords.defaultKeyword;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.web.testing.data.TestAdGroups.randomNameWebAdGroup;
import static ru.yandex.direct.web.testing.data.TestKeywords.keyword;
import static ru.yandex.direct.web.testing.data.TestKeywords.randomPhraseKeyword;

@DirectWebTest
@RunWith(SpringRunner.class)
public class AdGroupControllerCopyKeywordsTest extends AdGroupControllerCopyTestBase {

    @Test
    public void copiedKeywordPriceIsCopied() {
        Long keywordId = createAdGroupWithKeyword().getId();

        WebTextAdGroup requestAdGroup = randomNameWebAdGroup(adGroupId, campaignInfo.getCampaignId())
                .withKeywords(singletonList(randomPhraseKeyword(keywordId)));

        List<Long> adGroupCopiesIds = copyAndCheckResult(singletonList(requestAdGroup));

        List<Keyword> keywordsCopies = keywordRepository.getKeywordsByAdGroupId(shard, adGroupCopiesIds.get(0));
        assumeThat(keywordsCopies, hasSize(1));

        Keyword expectedKeyword = new Keyword()
                .withPrice(PRICE_SEARCH)
                .withPriceContext(PRICE_CONTEXT)
                .withAutobudgetPriority(PRIORITY);
        Keyword keywordCopy = keywordsCopies.get(0);
        assertThat(keywordCopy, beanDiffer(expectedKeyword).useCompareStrategy(onlyExpectedFields()));
    }

    @Test
    public void newKeywordIsCreatedWithoutCopiedKeywords() {
        createAdGroup();

        WebTextAdGroup requestAdGroup = randomNameWebAdGroup(adGroupId, campaignInfo.getCampaignId())
                .withKeywords(singletonList(randomPhraseKeyword(null)));

        List<Long> adGroupCopiesIds = copyAndCheckResult(singletonList(requestAdGroup));

        List<Keyword> newKeywords = keywordRepository.getKeywordsByAdGroupId(shard, adGroupCopiesIds.get(0));
        assertThat("количество созданных ключевых фраз не соответствует ожидаемому", newKeywords, hasSize(1));
    }

    @Test
    public void keywordPriceThatIsGreaterThanMaxShowBidIsCopied() {
        adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup(campaignInfo);
        final BigDecimal priceSearch = clientCurrency.getMaxShowBid()
                .add(BigDecimal.TEN)
                .setScale(2, RoundingMode.DOWN);
        final BigDecimal priceContext = clientCurrency.getMaxShowBid()
                .add(BigDecimal.TEN)
                .add(BigDecimal.TEN)
                .setScale(2, RoundingMode.DOWN);
        KeywordInfo keywordInfo = addKeywordToAdGroup(priceSearch, priceContext, 3);

        WebTextAdGroup requestAdGroup = randomNameWebAdGroup(adGroupId, campaignInfo.getCampaignId())
                .withKeywords(singletonList(keyword(keywordInfo.getId(), keywordInfo.getKeyword().getPhrase())));

        List<Long> adGroupCopiesIds = copyAndCheckResult(singletonList(requestAdGroup));

        List<Keyword> keywordsCopies = keywordRepository.getKeywordsByAdGroupId(shard, adGroupCopiesIds.get(0));
        assumeThat(keywordsCopies, hasSize(1));

        Keyword expectedKeyword = new Keyword()
                .withPrice(priceSearch)
                .withPriceContext(priceContext);
        Keyword keywordCopy = keywordsCopies.get(0);
        assertThat(keywordCopy, beanDiffer(expectedKeyword).useCompareStrategy(onlyExpectedFields()));
    }

    @Test
    public void oldKeywordPriceIsCopiedAndNewKeywordPriceIsCopiedFromOldKeyword() {
        Long keywordId = createAdGroupWithKeyword().getId();

        WebKeyword newKeyword = randomPhraseKeyword(null)
                .withPrice(null)
                .withPriceContext(null)
                .withAutobudgetPriority(null);
        WebTextAdGroup requestAdGroup = randomNameWebAdGroup(adGroupId, campaignInfo.getCampaignId())
                .withKeywords(asList(randomPhraseKeyword(keywordId), newKeyword));

        List<Long> adGroupCopiesIds = copyAndCheckResult(singletonList(requestAdGroup));

        List<Keyword> keywordsCopies = keywordRepository.getKeywordsByAdGroupId(shard, adGroupCopiesIds.get(0));
        assumeThat(keywordsCopies, hasSize(2));

        Keyword expectedKeyword = new Keyword()
                .withPrice(PRICE_SEARCH)
                .withPriceContext(PRICE_CONTEXT)
                .withAutobudgetPriority(PRIORITY);
        assertThat(keywordsCopies.get(0), beanDiffer(expectedKeyword).useCompareStrategy(onlyExpectedFields()));
        assertThat(keywordsCopies.get(1), beanDiffer(expectedKeyword).useCompareStrategy(onlyExpectedFields()));
    }

    @Test
    public void sameOldKeywordsPricesAreCopiedAndNewKeywordPriceIsCopiedFromOldKeywords() {
        Long keywordId1 = createAdGroupWithKeyword().getId();
        Long keywordId2 = addKeywordToAdGroup().getId();

        WebKeyword newKeyword = randomPhraseKeyword(null)
                .withPrice(null)
                .withPriceContext(null)
                .withAutobudgetPriority(null);
        WebTextAdGroup requestAdGroup = randomNameWebAdGroup(adGroupId, campaignInfo.getCampaignId())
                .withKeywords(asList(
                        randomPhraseKeyword(keywordId1),
                        newKeyword,
                        randomPhraseKeyword(keywordId2)));

        List<Long> adGroupCopiesIds = copyAndCheckResult(singletonList(requestAdGroup));

        List<Keyword> keywordsCopies = keywordRepository.getKeywordsByAdGroupId(shard, adGroupCopiesIds.get(0));
        assumeThat(keywordsCopies, hasSize(3));

        Keyword expectedKeyword = new Keyword()
                .withPrice(PRICE_SEARCH)
                .withPriceContext(PRICE_CONTEXT)
                .withAutobudgetPriority(PRIORITY);
        assertThat(keywordsCopies.get(0), beanDiffer(expectedKeyword).useCompareStrategy(onlyExpectedFields()));
        assertThat(keywordsCopies.get(1), beanDiffer(expectedKeyword).useCompareStrategy(onlyExpectedFields()));
        assertThat(keywordsCopies.get(2), beanDiffer(expectedKeyword).useCompareStrategy(onlyExpectedFields()));
    }

    @Test
    public void differentOldKeywordsPricesAreCopiedAndNewKeywordPriceIsCalculated() {
        final BigDecimal priceSearchAlt = BigDecimal.valueOf(239.4).setScale(2, RoundingMode.DOWN);
        final BigDecimal priceContextAlt = BigDecimal.valueOf(78.1).setScale(2, RoundingMode.DOWN);
        final int priorityAlt = AutoBudgetPriority.LOW.getTypedValue();
        KeywordInfo keywordInfo1 = createAdGroupWithKeyword();
        KeywordInfo keywordInfo2 = addKeywordToAdGroup(priceSearchAlt, priceContextAlt, priorityAlt);

        WebKeyword newKeyword = randomPhraseKeyword(null)
                .withPrice(null)
                .withPriceContext(null)
                .withAutobudgetPriority(null);
        WebTextAdGroup requestAdGroup = randomNameWebAdGroup(adGroupId, campaignInfo.getCampaignId())
                .withKeywords(asList(
                        keyword(keywordInfo1.getId(), keywordInfo1.getKeyword().getPhrase()),
                        newKeyword,
                        keyword(keywordInfo2.getId(), keywordInfo2.getKeyword().getPhrase())));

        List<Long> adGroupCopiesIds = copyAndCheckResult(singletonList(requestAdGroup));

        List<Keyword> keywordsCopies = keywordRepository.getKeywordsByAdGroupId(shard, adGroupCopiesIds.get(0));
        assumeThat(keywordsCopies, hasSize(3));

        Keyword expectedCopiedKeyword1 = new Keyword()
                .withPrice(PRICE_SEARCH)
                .withPriceContext(PRICE_CONTEXT)
                .withAutobudgetPriority(PRIORITY);
        assertThat(findKeyword(keywordsCopies, keywordInfo1.getKeyword().getPhrase()),
                beanDiffer(expectedCopiedKeyword1).useCompareStrategy(onlyExpectedFields()));

        Keyword expectedCopiedKeyword2 = new Keyword()
                .withPrice(priceSearchAlt)
                .withPriceContext(priceContextAlt)
                .withAutobudgetPriority(priorityAlt);
        assertThat(findKeyword(keywordsCopies, keywordInfo2.getKeyword().getPhrase()),
                beanDiffer(expectedCopiedKeyword2).useCompareStrategy(onlyExpectedFields()));

        assertThat("ставка у новой фразы не должна совпадать со ставкой у копируемой фразы",
                findKeyword(keywordsCopies, newKeyword.getPhrase()).getPrice(),
                not(equalTo(expectedCopiedKeyword1.getPrice())));
        assertThat("ставка у новой фразы не должна совпадать со ставкой у копируемой фразы",
                findKeyword(keywordsCopies, newKeyword.getPhrase()).getPrice(),
                not(equalTo(expectedCopiedKeyword2.getPrice())));

        assertThat("контекстная ставка у новой фразы не должна совпадать со ставкой у копируемой фразы",
                findKeyword(keywordsCopies, newKeyword.getPhrase()).getPriceContext(),
                not(equalTo(expectedCopiedKeyword1.getPriceContext())));
        assertThat("контекстная ставка у новой фразы не должна совпадать со ставкой у копируемой фразы",
                findKeyword(keywordsCopies, newKeyword.getPhrase()).getPriceContext(),
                not(equalTo(expectedCopiedKeyword2.getPriceContext())));
    }

    @Test
    public void generalPriceIsSetForCopiedKeywordAndPriorityIsCopiedFromOldKeyword() {
        KeywordInfo keywordInfo = createAdGroupWithKeyword();

        WebTextAdGroup requestAdGroup = randomNameWebAdGroup(adGroupId, campaignInfo.getCampaignId())
                .withKeywords(singletonList(keyword(keywordInfo.getId(), keywordInfo.getKeyword().getPhrase())))
                .withGeneralPrice(GENERAL_PRICE_DOUBLE);

        List<Long> adGroupCopiesIds = copyAndCheckResult(singletonList(requestAdGroup));

        List<Keyword> keywordsCopies = keywordRepository.getKeywordsByAdGroupId(shard, adGroupCopiesIds.get(0));
        assumeThat(keywordsCopies, hasSize(1));

        Keyword expectedKeyword = new Keyword()
                .withPrice(GENERAL_PRICE)
                .withPriceContext(GENERAL_PRICE)
                .withAutobudgetPriority(PRIORITY);
        Keyword keywordCopy = keywordsCopies.get(0);
        assertThat(keywordCopy, beanDiffer(expectedKeyword).useCompareStrategy(onlyExpectedFields()));
    }

    @Test
    public void generalPriceIsSetForOneCopiedKeywordAndOneNewAndPriorityIsCopiedFromOldKeyword() {
        Long keywordId = createAdGroupWithKeyword().getId();

        WebKeyword newKeyword = randomPhraseKeyword(null)
                .withPrice(null)
                .withPriceContext(null)
                .withAutobudgetPriority(null);
        WebTextAdGroup requestAdGroup = randomNameWebAdGroup(adGroupId, campaignInfo.getCampaignId())
                .withKeywords(asList(randomPhraseKeyword(keywordId), newKeyword))
                .withGeneralPrice(GENERAL_PRICE_DOUBLE);

        List<Long> adGroupCopiesIds = copyAndCheckResult(singletonList(requestAdGroup));

        List<Keyword> keywordsCopies = keywordRepository.getKeywordsByAdGroupId(shard, adGroupCopiesIds.get(0));
        assumeThat(keywordsCopies, hasSize(2));

        Keyword expectedKeyword = new Keyword()
                .withPrice(GENERAL_PRICE)
                .withPriceContext(GENERAL_PRICE)
                .withAutobudgetPriority(PRIORITY);
        assertThat(keywordsCopies.get(0), beanDiffer(expectedKeyword).useCompareStrategy(onlyExpectedFields()));
        assertThat(keywordsCopies.get(1), beanDiffer(expectedKeyword).useCompareStrategy(onlyExpectedFields()));
    }

    @Test
    public void generalPriceIsSetForOneNewKeywordWhenExistingKeywordIsNotCopied() {
        createAdGroupWithKeyword();

        WebKeyword newKeyword = randomPhraseKeyword(null)
                .withPrice(null)
                .withPriceContext(null)
                .withAutobudgetPriority(null);
        WebTextAdGroup requestAdGroup = randomNameWebAdGroup(adGroupId, campaignInfo.getCampaignId())
                .withKeywords(singletonList(newKeyword))
                .withGeneralPrice(GENERAL_PRICE_DOUBLE);

        List<Long> adGroupCopiesIds = copyAndCheckResult(singletonList(requestAdGroup));

        List<Keyword> keywordsCopies = keywordRepository.getKeywordsByAdGroupId(shard, adGroupCopiesIds.get(0));
        assumeThat(keywordsCopies, hasSize(1));

        Keyword expectedKeyword = new Keyword()
                .withPrice(GENERAL_PRICE)
                .withPriceContext(GENERAL_PRICE);
        assertThat(keywordsCopies.get(0), beanDiffer(expectedKeyword).useCompareStrategy(onlyExpectedFields()));
    }

    @Test
    public void prioritiesAreCopiedForCopiedKeywordsAndNotSetForNewKeywordWhenGeneralPriceIsSet() {
        final BigDecimal priceSearchAlt = BigDecimal.valueOf(239.4).setScale(2, RoundingMode.DOWN);
        final BigDecimal priceContextAlt = BigDecimal.valueOf(78.1).setScale(2, RoundingMode.DOWN);
        final int priorityAlt = AutoBudgetPriority.LOW.getTypedValue();
        KeywordInfo keywordInfo1 = createAdGroupWithKeyword();
        KeywordInfo keywordInfo2 = addKeywordToAdGroup(priceSearchAlt, priceContextAlt, priorityAlt);

        WebKeyword newKeyword = randomPhraseKeyword(null)
                .withPrice(null)
                .withPriceContext(null)
                .withAutobudgetPriority(null);
        WebTextAdGroup requestAdGroup = randomNameWebAdGroup(adGroupId, campaignInfo.getCampaignId())
                .withKeywords(asList(
                        keyword(keywordInfo1.getId(), keywordInfo1.getKeyword().getPhrase()),
                        newKeyword,
                        keyword(keywordInfo2.getId(), keywordInfo2.getKeyword().getPhrase())))
                .withGeneralPrice(GENERAL_PRICE_DOUBLE);

        List<Long> adGroupCopiesIds = copyAndCheckResult(singletonList(requestAdGroup));

        List<Keyword> keywordsCopies = keywordRepository.getKeywordsByAdGroupId(shard, adGroupCopiesIds.get(0));
        assumeThat(keywordsCopies, hasSize(3));

        Keyword expectedCopiedKeyword1 = new Keyword()
                .withPrice(GENERAL_PRICE)
                .withPriceContext(GENERAL_PRICE)
                .withAutobudgetPriority(PRIORITY);
        assertThat(findKeyword(keywordsCopies, keywordInfo1.getKeyword().getPhrase()),
                beanDiffer(expectedCopiedKeyword1).useCompareStrategy(onlyExpectedFields()));

        Keyword expectedCopiedKeyword2 = new Keyword()
                .withPrice(GENERAL_PRICE)
                .withPriceContext(GENERAL_PRICE)
                .withAutobudgetPriority(priorityAlt);
        assertThat(findKeyword(keywordsCopies, keywordInfo2.getKeyword().getPhrase()),
                beanDiffer(expectedCopiedKeyword2).useCompareStrategy(onlyExpectedFields()));

        Keyword expectedNewKeyword = new Keyword()
                .withPrice(GENERAL_PRICE)
                .withPriceContext(GENERAL_PRICE);
        Keyword actualNewKeyword = findKeyword(keywordsCopies, newKeyword.getPhrase());
        assertThat(actualNewKeyword,
                beanDiffer(expectedNewKeyword).useCompareStrategy(onlyExpectedFields()));
        assertThat(actualNewKeyword.getAutobudgetPriority(), nullValue());
    }

    @Test
    public void generalPricesInSeveralAdGroupsAreSet() {
        KeywordInfo keywordInfo = createAdGroupWithKeyword();

        AdGroupInfo adGroupInfo2 = steps.adGroupSteps().createDefaultAdGroup(campaignInfo);
        final Double generalPriceDouble2 = GENERAL_PRICE_DOUBLE + 100;
        final BigDecimal generalPrice2 = BigDecimal.valueOf(generalPriceDouble2)
                .setScale(2, RoundingMode.DOWN);

        WebTextAdGroup requestAdGroup1 = randomNameWebAdGroup(adGroupId, campaignInfo.getCampaignId())
                .withKeywords(singletonList(keyword(keywordInfo.getId(), keywordInfo.getKeyword().getPhrase())))
                .withGeneralPrice(GENERAL_PRICE_DOUBLE);
        WebTextAdGroup requestAdGroup2 = randomNameWebAdGroup(adGroupInfo2.getAdGroupId(), campaignInfo.getCampaignId())
                .withKeywords(singletonList(randomPhraseKeyword(null)))
                .withGeneralPrice(generalPriceDouble2);

        List<Long> adGroupCopiesIds = copyAndCheckResult(asList(requestAdGroup1, requestAdGroup2));

        List<Keyword> keywordsCopies1 = keywordRepository.getKeywordsByAdGroupId(shard, adGroupCopiesIds.get(0));
        assumeThat(keywordsCopies1, hasSize(1));

        Keyword expectedKeyword1 = new Keyword()
                .withPrice(GENERAL_PRICE)
                .withPriceContext(GENERAL_PRICE)
                .withAutobudgetPriority(PRIORITY);
        Keyword keywordCopy = keywordsCopies1.get(0);
        assertThat(keywordCopy, beanDiffer(expectedKeyword1).useCompareStrategy(onlyExpectedFields()));

        List<Keyword> keywordsCopies2 = keywordRepository.getKeywordsByAdGroupId(shard, adGroupCopiesIds.get(1));
        assumeThat(keywordsCopies2, hasSize(1));

        Keyword expectedKeyword2 = new Keyword()
                .withPrice(generalPrice2)
                .withPriceContext(generalPrice2);
        Keyword newKeyword = keywordsCopies2.get(0);
        assertThat(newKeyword, beanDiffer(expectedKeyword2).useCompareStrategy(onlyExpectedFields()));
    }

    @Test
    public void generalPriceOfOneAdGroupDoesNotAffectAnotherAdGroup() {
        KeywordInfo keywordInfo = createAdGroupWithKeyword();

        AdGroupInfo adGroupInfo2 = steps.adGroupSteps().createDefaultAdGroup(campaignInfo);

        WebTextAdGroup requestAdGroup1 = randomNameWebAdGroup(adGroupId, campaignInfo.getCampaignId())
                .withKeywords(singletonList(keyword(keywordInfo.getId(), keywordInfo.getKeyword().getPhrase())))
                .withGeneralPrice(GENERAL_PRICE_DOUBLE);
        WebTextAdGroup requestAdGroup2 = randomNameWebAdGroup(adGroupInfo2.getAdGroupId(), campaignInfo.getCampaignId())
                .withKeywords(singletonList(randomPhraseKeyword(null)));

        List<Long> adGroupCopiesIds = copyAndCheckResult(asList(requestAdGroup1, requestAdGroup2));

        List<Keyword> keywordsCopies1 = keywordRepository.getKeywordsByAdGroupId(shard, adGroupCopiesIds.get(0));
        assumeThat(keywordsCopies1, hasSize(1));

        Keyword expectedKeyword1 = new Keyword()
                .withPrice(GENERAL_PRICE)
                .withPriceContext(GENERAL_PRICE)
                .withAutobudgetPriority(PRIORITY);
        Keyword keywordCopy = keywordsCopies1.get(0);
        assertThat(keywordCopy, beanDiffer(expectedKeyword1).useCompareStrategy(onlyExpectedFields()));

        List<Keyword> keywordsCopies2 = keywordRepository.getKeywordsByAdGroupId(shard, adGroupCopiesIds.get(1));
        assumeThat(keywordsCopies2, hasSize(1));

        Keyword newKeyword = keywordsCopies2.get(0);
        assertThat(newKeyword.getPrice(), not(equalTo(GENERAL_PRICE)));
    }

    private KeywordInfo createAdGroupWithKeyword() {
        createAdGroup();
        return addKeywordToAdGroup();
    }

    private KeywordInfo addKeywordToAdGroup() {
        return addKeywordToAdGroup(PRICE_SEARCH, PRICE_CONTEXT, PRIORITY);
    }

    private KeywordInfo addKeywordToAdGroup(BigDecimal priceSearch, BigDecimal priceContext, int priority) {
        Keyword keyword = defaultKeyword()
                .withPrice(priceSearch)
                .withPriceContext(priceContext)
                .withAutobudgetPriority(priority);
        return steps.keywordSteps().createKeyword(adGroupInfo, keyword);
    }

    private Keyword findKeyword(List<Keyword> keywords, String phrase) {
        return StreamEx.of(keywords).findFirst(k -> k.getPhrase().equals(phrase)).orElse(null);
    }
}
