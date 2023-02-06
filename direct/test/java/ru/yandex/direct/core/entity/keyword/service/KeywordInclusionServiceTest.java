package ru.yandex.direct.core.entity.keyword.service;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.common.collect.ImmutableMap;
import one.util.streamex.EntryStream;
import one.util.streamex.StreamEx;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.keyword.model.Keyword;
import ru.yandex.direct.core.entity.keyword.model.KeywordText;
import ru.yandex.direct.core.entity.keyword.repository.KeywordRepository;
import ru.yandex.direct.core.entity.stopword.service.StopWordService;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.libs.keywordutils.inclusion.model.KeywordForInclusion;
import ru.yandex.direct.libs.keywordutils.inclusion.model.KeywordWithLemmasFactory;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class KeywordInclusionServiceTest {

    @Autowired
    private ShardHelper shardHelper;

    @Autowired
    private StopWordService stopWordService;

    @Autowired
    private KeywordInclusionService serviceUnderTest;

    @Autowired
    private Steps steps;

    private KeywordRepository keywordRepository;

    private ClientId clientId;
    private AdGroupInfo adGroupInfo;

    @Before
    public void setup() throws Exception {
        clientId = steps.clientSteps().createDefaultClient().getClientId();
        adGroupInfo = steps.adGroupSteps().createDefaultAdGroup();
        keywordRepository = mock(KeywordRepository.class);
        KeywordWithLemmasFactory keywordFactory = new KeywordWithLemmasFactory();

        serviceUnderTest = new KeywordInclusionService(shardHelper, keywordRepository, keywordFactory, stopWordService);
    }

    @Test
    public void getMinusKeywordsIncludedInPlusKeywordsForAdGroup_NoWrongMinusKeywords_ReturnsEmptyMap() {
        setExistingKeywordsForAdGroup(adGroupInfo.getAdGroupId(), "текст ключевой фразы");

        Map<Long, List<String>> minusKeywordsByAdGroupIds = buildKeywordMap(adGroupInfo.getAdGroupId(), "белый текст");

        Map<Long, Collection<String>> wrongMinusKeywordsForAdGroup =
                serviceUnderTest.getMinusKeywordsIncludedInPlusKeywordsForAdGroup(minusKeywordsByAdGroupIds, clientId);

        assertThat(wrongMinusKeywordsForAdGroup.entrySet(), empty());
    }

    @Test
    public void getMinusKeywordsIncludedInPlusKeywordsForCampaign_NoWrongMinusKeywords_ReturnsEmptyMap() {
        setExistingKeywordsForCampaign(adGroupInfo.getCampaignId(), "текст ключевой фразы");

        Map<Long, List<String>> minusKeywordsByCampaignId = buildKeywordMap(adGroupInfo.getCampaignId(), "белый текст");

        Map<Long, Collection<String>> wrongMinusKeywordsForCampaign =
                serviceUnderTest
                        .getMinusKeywordsIncludedInPlusKeywordsForCampaign(minusKeywordsByCampaignId, clientId);

        assertThat(wrongMinusKeywordsForCampaign.entrySet(), empty());
    }

    @Test
    public void getMinusKeywordsIncludedInPlusKeywordsForAdGroup_NoPlusKeywords_ReturnsEmptyMap() {
        setExistingKeywordsForAdGroup(adGroupInfo.getAdGroupId());

        Map<Long, List<String>> minusKeywordsByAdGroupIds = buildKeywordMap(adGroupInfo.getAdGroupId(), "текст");

        Map<Long, Collection<String>> wrongMinusKeywordsForAdGroup =
                serviceUnderTest.getMinusKeywordsIncludedInPlusKeywordsForAdGroup(minusKeywordsByAdGroupIds, clientId);

        assertThat(wrongMinusKeywordsForAdGroup.entrySet(), empty());
    }

    @Test
    public void getMinusKeywordsIncludedInPlusKeywordsForCampaign_NoPlusKeywords_ReturnsEmptyMap() {
        setExistingKeywordsForCampaign(adGroupInfo.getCampaignId());

        Map<Long, List<String>> minusKeywordsByCampaignIds = buildKeywordMap(adGroupInfo.getCampaignId(), "текст");

        Map<Long, Collection<String>> wrongMinusKeywordsForCampaign =
                serviceUnderTest
                        .getMinusKeywordsIncludedInPlusKeywordsForCampaign(minusKeywordsByCampaignIds, clientId);

        assertThat(wrongMinusKeywordsForCampaign.entrySet(), empty());
    }

    @Test
    public void getMinusKeywordsIncludedInPlusKeywordsForAdGroup_WrongMinusKeywordsPresent_ReturnsValidMap() {
        setExistingKeywordsForAdGroup(adGroupInfo.getAdGroupId(), "текст ключевой фразы");

        Map<Long, List<String>> minusKeywordsByAdGroupIds = buildKeywordMap(adGroupInfo.getAdGroupId(), "текст");

        Map<Long, Collection<String>> actual =
                serviceUnderTest.getMinusKeywordsIncludedInPlusKeywordsForAdGroup(minusKeywordsByAdGroupIds, clientId);

        Map<Long, List<String>> expected = new HashMap<>(minusKeywordsByAdGroupIds);
        assertThat(actual, is(equalTo(expected)));
    }

    @Test
    public void getMinusKeywordsIncludedInPlusKeywordsForAdGroup_WrongMinusKeywordsPresentInUpperCase_ReturnsValidMapWithUpperCase() {
        setExistingKeywordsForAdGroup(adGroupInfo.getAdGroupId(), "текст ключевой фразы");

        Map<Long, List<String>> minusKeywordsByAdGroupIds = buildKeywordMap(adGroupInfo.getAdGroupId(), "Тексты");

        Map<Long, Collection<String>> actual =
                serviceUnderTest.getMinusKeywordsIncludedInPlusKeywordsForAdGroup(minusKeywordsByAdGroupIds, clientId);

        Map<Long, List<String>> expected = new HashMap<>(minusKeywordsByAdGroupIds);
        assertThat(actual, is(equalTo(expected)));
    }

    @Test
    public void getMinusKeywordsIncludedInPlusKeywordsForCampaign_WrongMinusKeywordsPresent_ReturnsValidMap() {
        setExistingKeywordsForCampaign(adGroupInfo.getCampaignId(), "текст ключевой фразы");

        Map<Long, List<String>> minusKeywordsByCampaignIds = buildKeywordMap(adGroupInfo.getCampaignId(), "текст");

        Map<Long, Collection<String>> actual =
                serviceUnderTest
                        .getMinusKeywordsIncludedInPlusKeywordsForCampaign(minusKeywordsByCampaignIds, clientId);

        Map<Long, List<String>> expected = new HashMap<>(minusKeywordsByCampaignIds);
        assertThat(actual, is(equalTo(expected)));
    }

    @Test
    public void getMinusKeywordsIncludedInPlusKeywordsForAdGroup_WrongMinusKeywordsPresentInPhraseWithMinusWords_ReturnsValidMap() {
        setExistingKeywordsForAdGroup(adGroupInfo.getAdGroupId(), "текст ключевой фразы -с минус фразой");

        Map<Long, List<String>> minusKeywordsByAdGroupIds = buildKeywordMap(adGroupInfo.getAdGroupId(), "текст");

        Map<Long, Collection<String>> actual =
                serviceUnderTest.getMinusKeywordsIncludedInPlusKeywordsForAdGroup(minusKeywordsByAdGroupIds, clientId);

        Map<Long, List<String>> expected = new HashMap<>(minusKeywordsByAdGroupIds);
        assertThat(actual, is(equalTo(expected)));
    }

    @Test
    public void getMinusKeywordsIncludedInPlusKeywordsForAdGroup_WrongMinusKeywordsPresentInPhraseInMinusWords_ReturnsEmptyMap() {
        setExistingKeywordsForAdGroup(adGroupInfo.getAdGroupId(), "текст ключевой фразы -с минус фразой");

        Map<Long, List<String>> minusKeywordsByAdGroupIds = buildKeywordMap(adGroupInfo.getAdGroupId(), "минус");

        Map<Long, Collection<String>> actual =
                serviceUnderTest.getMinusKeywordsIncludedInPlusKeywordsForAdGroup(minusKeywordsByAdGroupIds, clientId);

        assertThat(actual.entrySet(), empty());
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void safeParsePlusKeyword_success_onCorrectPhrase() {
        Optional<KeywordForInclusion> parsed = serviceUnderTest.safeParsePlusKeyword("фраза -с минус фразой");
        Assertions.assertThat(parsed.isPresent()).isTrue();
        Assertions.assertThat(parsed.get().originString()).isEqualTo("фраза");
    }

    @Test
    public void safeParsePlusKeyword_success_onIncorrectPhrase() {
        Optional<KeywordForInclusion> parsed = serviceUnderTest.safeParsePlusKeyword("неправильная фраза !");
        Assertions.assertThat(parsed.isPresent()).isFalse();
    }

    @Test
    public void safeParseKeywords_success_onCorrectPhrases() {
        Collection<KeywordForInclusion> parsed =
                serviceUnderTest.safeParseKeywords(asList("фраза", "другая фраза"));
        Assertions.assertThat(parsed).hasSize(2);
    }

    @Test
    public void safeParseKeywords_success_onIncorrectPhrases() {
        Collection<KeywordForInclusion> parsed =
                serviceUnderTest.safeParseKeywords(asList("фраза", "неправильная фраза !"));
        Assertions.assertThat(parsed).hasSize(1);
    }

    @Test
    public void getMinusKeywordsNotIncludedInPlusKeywords_NoIncludedMinusKeywords() {
        List<String> minusKeywords = singletonList("белый текст");
        List<String> actual = serviceUnderTest.getMinusKeywordsNotIncludedInPlusKeywords(
                singletonList("текст ключевой фразы"), minusKeywords);
        assertThat(actual, is(equalTo(minusKeywords)));
    }

    @Test
    public void getMinusKeywordsNotIncludedInPlusKeywords_EmptyKeywords() {
        List<String> minusKeywords = singletonList("белый текст");
        List<String> actual = serviceUnderTest.getMinusKeywordsNotIncludedInPlusKeywords(
                emptyList(), minusKeywords);
        assertThat(actual, is(equalTo(minusKeywords)));
    }

    @Test
    public void getMinusKeywordsNotIncludedInPlusKeywords_IncludedMinusKeyword() {
        List<String> minusKeywords = singletonList("текст");
        List<String> actual = serviceUnderTest.getMinusKeywordsNotIncludedInPlusKeywords(
                singletonList("текст ключевой фразы"), minusKeywords);
        assertThat(actual, empty());
    }

    @Test
    public void getMinusKeywordsNotIncludedInPlusKeywords_IncludedMinusKeyword_CapitalLetter() {
        List<String> minusKeywords = singletonList("Тексты");
        List<String> actual = serviceUnderTest.getMinusKeywordsNotIncludedInPlusKeywords(
                singletonList("текст ключевой фразы"), minusKeywords);
        assertThat(actual, empty());
    }

    @Test
    public void getMinusKeywordsNotIncludedInPlusKeywords_IncludedMinusKeyword_KeywordWithMinusWord() {
        List<String> minusKeywords = singletonList("текст");
        List<String> actual = serviceUnderTest.getMinusKeywordsNotIncludedInPlusKeywords(
                singletonList("текст ключевой фразы -с минус фразой"), minusKeywords);
        assertThat(actual, empty());
    }

    @Test
    public void getMinusKeywordsNotIncludedInPlusKeywords_NoIncludedMinusKeywords_KeywordWithMinusWord() {
        List<String> minusKeywords = singletonList("минус");
        List<String> actual = serviceUnderTest.getMinusKeywordsNotIncludedInPlusKeywords(
                singletonList("текст ключевой фразы -с минус фразой"), minusKeywords);
        assertThat(actual, is(equalTo(minusKeywords)));
    }

    private void setExistingKeywordsForAdGroup(long adGroupId, String... keywordTexts) {
        Map<Long, List<String>> existingPlusKeywords = buildKeywordMap(adGroupId, keywordTexts);
        Map<Long, List<KeywordText>> keywordsMap = EntryStream.of(existingPlusKeywords)
                .mapValues(phrases -> buildKeywords(adGroupId, phrases))
                .toMap();
        when(keywordRepository.getKeywordTextsByAdGroupIds(anyInt(), any(ClientId.class), any()))
                .thenReturn(keywordsMap);
    }

    private void setExistingKeywordsForCampaign(long campaignId, String... keywordTexts) {
        Map<Long, List<String>> existingPlusKeywords = buildKeywordMap(campaignId, keywordTexts);
        Map<Long, List<KeywordText>> keywordsMap = EntryStream.of(existingPlusKeywords)
                .mapValues(phrases -> buildKeywords(campaignId, phrases))
                .toMap();
        when(keywordRepository.getKeywordTextsByCampaignIds(anyInt(), any(ClientId.class), any()))
                .thenReturn(keywordsMap);
    }

    private Map<Long, List<String>> buildKeywordMap(long adGroupId, String... strings) {
        return ImmutableMap.of(
                adGroupId, asList(strings)
        );
    }

    private KeywordText buildKeyword(long adGroupId, String phrase) {
        return new Keyword().withAdGroupId(adGroupId).withPhrase(phrase);
    }

    private List<KeywordText> buildKeywords(long adGroupId, Collection<String> phrases) {
        return StreamEx.of(phrases).map(a -> buildKeyword(adGroupId, a)).toList();
    }
}
