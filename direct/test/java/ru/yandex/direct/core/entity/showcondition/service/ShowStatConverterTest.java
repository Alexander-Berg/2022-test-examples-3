package ru.yandex.direct.core.entity.showcondition.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import one.util.streamex.StreamEx;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.advq.Device;
import ru.yandex.direct.advq.SearchRequest;
import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository;
import ru.yandex.direct.core.entity.adgroup.service.MinusKeywordPreparingTool;
import ru.yandex.direct.core.entity.campaign.service.CampaignService;
import ru.yandex.direct.core.entity.minuskeywordspack.model.MinusKeywordsPack;
import ru.yandex.direct.core.entity.minuskeywordspack.repository.MinusKeywordsPackRepository;
import ru.yandex.direct.core.entity.showcondition.model.ShowStatRequest;
import ru.yandex.direct.core.entity.stopword.service.StopWordService;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.libs.keywordutils.inclusion.model.KeywordWithLemmasFactory;
import ru.yandex.direct.regions.Region;

import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.advq.Device.ALL;
import static ru.yandex.direct.advq.Device.DESKTOP;
import static ru.yandex.direct.advq.Device.PHONE;
import static ru.yandex.direct.advq.Device.TABLET;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.minusphrase.MinusPhraseConstraints.CAMPAIGN_MINUS_KEYWORDS_MAX_LENGTH;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.minusphrase.MinusPhraseConstraints.GROUP_MINUS_KEYWORDS_MAX_LENGTH;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

@CoreTest
@RunWith(Parameterized.class)
public class ShowStatConverterTest {

    private static final String SEPARATOR = " -";
    private static final String DEFAULT_GEO = String.valueOf(Region.RUSSIA_REGION_ID);
    private static final List<Long> NON_DEFAULT_GEO = List.of(Region.RUSSIA_REGION_ID, Region.TURKEY_REGION_ID);

    private static final String PHRASE_ONE = "купить слона";
    private static final String PHRASE_TWO = "купить жирафа";
    private static final String PHRASE_WITH_SEPARATOR = "санкт-петербург";
    private static final String MINUS_WORD_ONE = "большой";
    private static final String MINUS_WORD_TWO = "маленький";
    private static final String MINUS_WORD_INCLUDED_TO_PHRASE_ONE = "слон";
    private static final String MAX_MINUS_PHRASE = RandomStringUtils.randomAlphabetic(
            CAMPAIGN_MINUS_KEYWORDS_MAX_LENGTH + GROUP_MINUS_KEYWORDS_MAX_LENGTH + 4096);

    private static final Long CAMPAIGN_ID_1 = 1L;
    private static final Long CAMPAIGN_ID_2 = 2L;
    private static final String MINUS_WORD_CAMPAIGN_1 = "средний";
    private static final String MINUS_WORD_CAMPAIGN_2 = MINUS_WORD_TWO;

    private static final Long AD_GROUP_ID = 3L;
    private static final String MINUS_WORD_AD_GROUP_1 = "огромный";
    private static final String MINUS_WORD_AD_GROUP_1_OTHER_FORM = "огромная";
    private static final String MINUS_WORD_AD_GROUP_2 = MINUS_WORD_ONE;

    private static final Long LIBRARY_MINUS_PHRASES_ID = 4L;
    private static final String MINUS_WORD_LIBRARY_1 = "среднее";
    private static final MinusKeywordsPack LIBRARY_PACK =
            new MinusKeywordsPack().withMinusKeywords(List.of(MINUS_WORD_LIBRARY_1));

    @ClassRule
    public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();

    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    @Mock
    private ShardHelper shardHelper;

    @Mock
    private CampaignService campaignService;

    @Mock
    private AdGroupRepository adGroupRepository;

    @Mock
    public MinusKeywordsPackRepository minusKeywordsPackRepository;

    @Autowired
    private MinusKeywordPreparingTool minusKeywordPreparingTool;

    @Autowired
    private StopWordService stopWordService;

    private ShowStatConverter showStatConverter;

    @Parameterized.Parameter
    public ShowStatRequest request;

    @Parameterized.Parameter(1)
    public SearchRequest expectedRequest;

    @Parameterized.Parameter(2)
    public String expectedMessage;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(campaignService.getMinusKeywordsByCampaignId(anyInt(), eq(CAMPAIGN_ID_1)))
                .thenReturn(List.of(MINUS_WORD_CAMPAIGN_1));
        when(campaignService.getMinusKeywordsByCampaignId(anyInt(), eq(CAMPAIGN_ID_2)))
                .thenReturn(List.of(MINUS_WORD_CAMPAIGN_2));
        when(adGroupRepository.getAdGroups(anyInt(), anyCollection())).thenReturn(
                List.of(new AdGroup().withId(AD_GROUP_ID)
                        .withCampaignId(CAMPAIGN_ID_1)
                        .withMinusKeywords(List.of(MINUS_WORD_AD_GROUP_1, MINUS_WORD_AD_GROUP_2))));
        Mockito.when(minusKeywordsPackRepository.getMinusKeywordsPacks(anyInt(), any(), anyCollection()))
                .thenReturn(emptyMap());
        when(minusKeywordsPackRepository.getMinusKeywordsPacks(anyInt(), any(), anyCollection()))
                .thenReturn(Map.of(LIBRARY_MINUS_PHRASES_ID, LIBRARY_PACK));
        KeywordWithLemmasFactory keywordFactory = new KeywordWithLemmasFactory();
        showStatConverter = new ShowStatConverter(minusKeywordPreparingTool, keywordFactory, stopWordService,
                minusKeywordsPackRepository, shardHelper, adGroupRepository, campaignService);
    }

    private static Collection<Object[]> paramsForOnePhrase() {
        return List.of(new Object[][]{
                {
                        defaultRequest(PHRASE_ONE),
                        defaultSearchRequest(PHRASE_ONE),
                        "one phrase without minus-phrases"
                },
                {
                        requestWithNonDefaultGeo(PHRASE_WITH_SEPARATOR),
                        searchRequestWithNonDefaultGeo(PHRASE_WITH_SEPARATOR),
                        "one phrase with -"
                },
                {
                        defaultRequest(PHRASE_ONE + SEPARATOR + MINUS_WORD_ONE),
                        defaultSearchRequest(PHRASE_ONE + SEPARATOR + MINUS_WORD_ONE),
                        "one phrase with minus-phrases"
                },
                {
                        defaultRequest(List.of(PHRASE_ONE), List.of(MINUS_WORD_ONE, MINUS_WORD_TWO)),
                        defaultSearchRequest(PHRASE_ONE + SEPARATOR + MINUS_WORD_ONE + SEPARATOR + MINUS_WORD_TWO),
                        "one phrase with common minus-phrases"
                },
                {
                        defaultRequest(List.of(PHRASE_ONE + SEPARATOR + MINUS_WORD_ONE), List.of(MINUS_WORD_ONE)),
                        defaultSearchRequest(PHRASE_ONE + SEPARATOR + MINUS_WORD_ONE),
                        "one phrase with same minus-phrase"
                },
                {
                        defaultRequest(List.of(PHRASE_ONE), List.of(MAX_MINUS_PHRASE)),
                        defaultSearchRequest(PHRASE_ONE + SEPARATOR + MAX_MINUS_PHRASE),
                        "one phrase with max minus-phrase"
                },
                {
                        defaultRequestWithAdGroupId(List.of(PHRASE_ONE), List.of(MINUS_WORD_AD_GROUP_1_OTHER_FORM)),
                        defaultSearchRequest(PHRASE_ONE + SEPARATOR + MINUS_WORD_AD_GROUP_2 + SEPARATOR + MINUS_WORD_AD_GROUP_1_OTHER_FORM + SEPARATOR + MINUS_WORD_CAMPAIGN_1),
                        "one phrase with adGroup minus-phrases and common same minus-phrase but other norm form"
                },
                {
                        defaultRequest(List.of(PHRASE_ONE), List.of(MINUS_WORD_INCLUDED_TO_PHRASE_ONE)),
                        defaultSearchRequest(PHRASE_ONE),
                        "one phrase with included minus-phrase"
                },
        });
    }

    private static Collection<Object[]> paramsForTwoPhrase() {
        return List.of(new Object[][]{
                {
                        defaultRequest(List.of(PHRASE_ONE, PHRASE_TWO)),
                        defaultSearchRequest(List.of(PHRASE_ONE, PHRASE_TWO)),
                        "two phrases without minus-phrase"
                },
                {
                        defaultRequest(List.of(PHRASE_WITH_SEPARATOR, PHRASE_ONE)),
                        defaultSearchRequest(List.of(PHRASE_WITH_SEPARATOR, PHRASE_ONE)),
                        "two phrases and one with -"
                },
                {
                        defaultRequest(List.of(PHRASE_ONE + SEPARATOR + MINUS_WORD_ONE, PHRASE_TWO)),
                        defaultSearchRequest(List.of(PHRASE_ONE + SEPARATOR + MINUS_WORD_ONE, PHRASE_TWO)),
                        "two phrases and one with minus-phrases"
                },
                {
                        defaultRequest(List.of(PHRASE_ONE + SEPARATOR + MINUS_WORD_ONE, PHRASE_TWO),
                                List.of(MINUS_WORD_TWO)),
                        defaultSearchRequest(List.of(PHRASE_ONE + SEPARATOR + MINUS_WORD_ONE + SEPARATOR + MINUS_WORD_TWO,
                                PHRASE_TWO + SEPARATOR + MINUS_WORD_TWO)),
                        "two phrases and one with minus-phrases and common minus-phrases"
                },
                {
                        defaultRequest(List.of(PHRASE_ONE, PHRASE_TWO), List.of(MINUS_WORD_ONE, MINUS_WORD_TWO)),
                        defaultSearchRequest(List.of(PHRASE_ONE + SEPARATOR + MINUS_WORD_ONE + SEPARATOR + MINUS_WORD_TWO,
                                PHRASE_TWO + SEPARATOR + MINUS_WORD_ONE + SEPARATOR + MINUS_WORD_TWO)),
                        "two phrases with common minus-phrases"
                },
                {
                        defaultRequest(List.of(PHRASE_ONE + SEPARATOR + MINUS_WORD_ONE,
                                PHRASE_TWO + SEPARATOR + MINUS_WORD_ONE),
                                List.of(MINUS_WORD_ONE)),
                        defaultSearchRequest(List.of(PHRASE_ONE + SEPARATOR + MINUS_WORD_ONE,
                                PHRASE_TWO + SEPARATOR + MINUS_WORD_ONE)),
                        "two phrases with same minus-phrase"
                },
                {
                        defaultRequest(List.of(PHRASE_ONE + SEPARATOR + MINUS_WORD_ONE,
                                PHRASE_TWO + SEPARATOR + MINUS_WORD_TWO),
                                List.of(MINUS_WORD_ONE, MINUS_WORD_TWO)),
                        defaultSearchRequest(List.of(PHRASE_ONE + SEPARATOR + MINUS_WORD_ONE + SEPARATOR + MINUS_WORD_TWO,
                                PHRASE_TWO + SEPARATOR + MINUS_WORD_TWO + SEPARATOR + MINUS_WORD_ONE)),
                        "two phrases with cross same minus-phrase"
                },
                {
                        defaultRequest(List.of(PHRASE_ONE, PHRASE_TWO), List.of(MINUS_WORD_INCLUDED_TO_PHRASE_ONE)),
                        defaultSearchRequest(List.of(PHRASE_ONE,
                                PHRASE_TWO + SEPARATOR + MINUS_WORD_INCLUDED_TO_PHRASE_ONE)),
                        "two phrases and one with included minus-phrase"
                },
        });
    }

    private static Collection<Object[]> paramsForTwoPhraseWithCampaignId() {
        return List.of(new Object[][]{
                {
                        defaultRequestWithCampaignId(List.of(PHRASE_ONE, PHRASE_TWO), CAMPAIGN_ID_1),
                        defaultSearchRequest(List.of(
                                PHRASE_ONE + SEPARATOR + MINUS_WORD_CAMPAIGN_1,
                                PHRASE_TWO + SEPARATOR + MINUS_WORD_CAMPAIGN_1)),
                        "two phrases without minus-phrase with campaign minus-phrase"
                },
                {
                        defaultRequestWithCampaignId(List.of(PHRASE_ONE + SEPARATOR + MINUS_WORD_TWO, PHRASE_TWO),
                                CAMPAIGN_ID_2),
                        defaultSearchRequest(List.of(
                                PHRASE_ONE + SEPARATOR + MINUS_WORD_TWO,
                                PHRASE_TWO + SEPARATOR + MINUS_WORD_CAMPAIGN_2)),
                        "two phrase with same campaign minus-phrase for one"
                },
                {
                        defaultRequestWithCampaignId(List.of(PHRASE_ONE + SEPARATOR + MINUS_WORD_ONE, PHRASE_TWO),
                                List.of(MINUS_WORD_TWO), CAMPAIGN_ID_2),
                        defaultSearchRequest(List.of(
                                PHRASE_ONE + SEPARATOR + MINUS_WORD_ONE + SEPARATOR + MINUS_WORD_TWO,
                                PHRASE_TWO + SEPARATOR + MINUS_WORD_TWO)
                        ),
                        "two phrases and common minus-phrases with same campaign minus-phrase"
                },
                {
                        defaultRequestWithCampaignId(List.of(PHRASE_ONE, PHRASE_TWO),
                                List.of(MINUS_WORD_ONE, MINUS_WORD_TWO), CAMPAIGN_ID_1),
                        defaultSearchRequest(List.of(
                                String.join(
                                        SEPARATOR,
                                        PHRASE_ONE,
                                        MINUS_WORD_ONE,
                                        MINUS_WORD_TWO,
                                        MINUS_WORD_CAMPAIGN_1),
                                String.join(
                                        SEPARATOR,
                                        PHRASE_TWO,
                                        MINUS_WORD_ONE,
                                        MINUS_WORD_TWO,
                                        MINUS_WORD_CAMPAIGN_1))),
                        "two phrases with common minus-phrases with campaign minus-phrase"
                },
        });
    }

    private static Collection<Object[]> paramsForTwoPhraseWithAdGroupId() {
        return List.of(new Object[][]{
                {
                        defaultRequestWithAdGroupId(List.of(PHRASE_ONE, PHRASE_TWO)),
                        defaultSearchRequest(List.of(
                                PHRASE_ONE + SEPARATOR + MINUS_WORD_AD_GROUP_2 + SEPARATOR + MINUS_WORD_AD_GROUP_1 + SEPARATOR + MINUS_WORD_CAMPAIGN_1,
                                PHRASE_TWO + SEPARATOR + MINUS_WORD_AD_GROUP_2 + SEPARATOR + MINUS_WORD_AD_GROUP_1 + SEPARATOR + MINUS_WORD_CAMPAIGN_1)),
                        "two phrase with adGroup minus-phrases without campaign"
                },
                {
                        defaultRequestWithAdGroupId(List.of(PHRASE_ONE + SEPARATOR + MINUS_WORD_AD_GROUP_1,
                                PHRASE_TWO)),
                        defaultSearchRequest(List.of(
                                PHRASE_ONE + SEPARATOR + MINUS_WORD_AD_GROUP_1 + SEPARATOR + MINUS_WORD_AD_GROUP_2 + SEPARATOR + MINUS_WORD_CAMPAIGN_1,
                                PHRASE_TWO + SEPARATOR + MINUS_WORD_AD_GROUP_2 + SEPARATOR + MINUS_WORD_AD_GROUP_1 + SEPARATOR + MINUS_WORD_CAMPAIGN_1)),
                        "two phrase with same adGroup minus-phrases for one"
                },
                {
                        defaultRequestWithAdGroupId(List.of(PHRASE_ONE + SEPARATOR + MINUS_WORD_CAMPAIGN_1,
                                PHRASE_TWO)),
                        defaultSearchRequest(List.of(
                                PHRASE_ONE + SEPARATOR + MINUS_WORD_CAMPAIGN_1 + SEPARATOR + MINUS_WORD_AD_GROUP_2 + SEPARATOR + MINUS_WORD_AD_GROUP_1,
                                PHRASE_TWO + SEPARATOR + MINUS_WORD_AD_GROUP_2 + SEPARATOR + MINUS_WORD_AD_GROUP_1 + SEPARATOR + MINUS_WORD_CAMPAIGN_1)),
                        "two phrase with adGroup minus-phrases (same minus-phrases in campaign) for one"
                },
                {
                        defaultRequestWithAdGroupIdAndCampaignId(List.of(PHRASE_ONE, PHRASE_TWO), CAMPAIGN_ID_2),
                        defaultSearchRequest(List.of(
                                PHRASE_ONE + SEPARATOR + MINUS_WORD_AD_GROUP_2 + SEPARATOR + MINUS_WORD_AD_GROUP_1 + SEPARATOR + MINUS_WORD_CAMPAIGN_1,
                                PHRASE_TWO + SEPARATOR + MINUS_WORD_AD_GROUP_2 + SEPARATOR + MINUS_WORD_AD_GROUP_1 + SEPARATOR + MINUS_WORD_CAMPAIGN_1)),
                        "two phrase with adGroup minus-phrases with unlinked campaign2"
                },
        });
    }

    private static Collection<Object[]> paramsForOnePhraseWithLibraryIdAndCampaignId() {
        return List.of(new Object[][]{
                {
                        defaultRequestWithLibraryIdAndCampaignId(List.of(PHRASE_ONE), CAMPAIGN_ID_1),
                        defaultSearchRequest(PHRASE_ONE + SEPARATOR + MINUS_WORD_LIBRARY_1),
                        "one phrase with library minus-phrase and same campaign minus-phrase"
                },
                {
                        defaultRequestWithLibraryIdAndCampaignId(List.of(PHRASE_ONE), List.of(MINUS_WORD_CAMPAIGN_2),
                                CAMPAIGN_ID_1),
                        defaultSearchRequest(PHRASE_ONE + SEPARATOR + MINUS_WORD_CAMPAIGN_2 + SEPARATOR + MINUS_WORD_LIBRARY_1),
                        "one phrase with library minus-phrase and same campaign minus-phrase and common minus phrase"
                },
                {
                        defaultRequestWithLibraryIdAndCampaignId(List.of(PHRASE_ONE + SEPARATOR + MINUS_WORD_CAMPAIGN_2), CAMPAIGN_ID_2),
                        defaultSearchRequest(PHRASE_ONE + SEPARATOR + MINUS_WORD_CAMPAIGN_2 + SEPARATOR + MINUS_WORD_LIBRARY_1),
                        "one phrase with library minus-phrase and campaign minus phrase (same as phrase minus-phrase)"
                },
                {
                        defaultRequestWithLibraryIdAndCampaignId(List.of(PHRASE_ONE), List.of(MINUS_WORD_CAMPAIGN_2),
                                CAMPAIGN_ID_2),
                        defaultSearchRequest(PHRASE_ONE + SEPARATOR + MINUS_WORD_CAMPAIGN_2 + SEPARATOR + MINUS_WORD_LIBRARY_1),
                        "one phrase with library minus-phrase and campaign minus phrase (same as common minus-phrase)"
                },
        });
    }

    private static Collection<Object[]> paramsForOnePhraseWithDeviceTypes() {
        return List.of(new Object[][]{
                {
                        defaultRequestWithDeviceTypes(List.of(PHRASE_ONE), null),
                        defaultSearchRequest(PHRASE_ONE),
                        "one phrase with null device types"
                },
                {
                        defaultRequestWithDeviceTypes(List.of(PHRASE_ONE), emptySet()),
                        defaultSearchRequest(PHRASE_ONE)
                                .withDeviceTypes(emptySet()),
                        "one phrase with empty device types"
                },
                {
                        defaultRequestWithDeviceTypes(List.of(PHRASE_ONE), Set.of(PHONE)),
                        defaultSearchRequest(PHRASE_ONE)
                                .withDeviceTypes(Set.of(PHONE)),
                        "one phrase with single device type"
                },
                {
                        defaultRequestWithDeviceTypes(List.of(PHRASE_ONE), Set.of(PHONE, TABLET, DESKTOP)),
                        defaultSearchRequest(PHRASE_ONE)
                                .withDeviceTypes(Set.of(PHONE, TABLET, DESKTOP)),
                        "one phrase with multiple device types"
                },
                {
                        defaultRequestWithDeviceTypes(List.of(PHRASE_ONE), Set.of(ALL)),
                        defaultSearchRequest(PHRASE_ONE)
                                .withDeviceTypes(Set.of(ALL)),
                        "one phrase with all device type"
                },
        });
    }

    @Parameterized.Parameters(name = "{2}")
    public static Collection<Object[]> params() {
        List<Object[]> params = new ArrayList<>();
        params.addAll(paramsForOnePhrase());
        params.addAll(paramsForTwoPhrase());
        params.addAll(paramsForTwoPhraseWithCampaignId());
        params.addAll(paramsForTwoPhraseWithAdGroupId());
        params.addAll(paramsForOnePhraseWithLibraryIdAndCampaignId());
        params.addAll(paramsForOnePhraseWithDeviceTypes());
        return params;
    }

    @Test
    public void convertToAdvqRequest() {
        SearchRequest actualRequest = showStatConverter.convertRequest(request);

        assertThat(actualRequest).is(matchedBy(beanDiffer(expectedRequest)));
    }

    private static ShowStatRequest defaultRequest(List<String> phrases) {
        return new ShowStatRequest()
                .withPhrases(phrases)
                .withGeo(DEFAULT_GEO);
    }

    private static ShowStatRequest defaultRequest(String phrase) {
        return defaultRequest(List.of(phrase));
    }

    @SuppressWarnings("SameParameterValue")
    private static ShowStatRequest defaultRequestWithAdGroupIdAndCampaignId(List<String> phrases,
                                                                            Long campaignId) {
        return new ShowStatRequest()
                .withPhrases(phrases)
                .withCampaignId(campaignId)
                .withAdGroupId(AD_GROUP_ID)
                .withGeo(DEFAULT_GEO);
    }

    private static ShowStatRequest defaultRequestWithAdGroupId(List<String> phrases) {
        return new ShowStatRequest()
                .withPhrases(phrases)
                .withAdGroupId(AD_GROUP_ID)
                .withGeo(DEFAULT_GEO);
    }

    private static ShowStatRequest defaultRequestWithAdGroupId(List<String> phrases,
                                                               List<String> minusPhrases) {
        return new ShowStatRequest()
                .withPhrases(phrases)
                .withCommonMinusPhrases(minusPhrases)
                .withAdGroupId(AD_GROUP_ID)
                .withGeo(DEFAULT_GEO);
    }

    private static ShowStatRequest defaultRequestWithCampaignId(List<String> phrases, Long campaignId) {
        return new ShowStatRequest()
                .withPhrases(phrases)
                .withCampaignId(campaignId)
                .withGeo(DEFAULT_GEO);
    }

    private static ShowStatRequest defaultRequestWithCampaignId(List<String> phrases,
                                                                List<String> minusPhrases,
                                                                Long campaignId) {
        return new ShowStatRequest()
                .withPhrases(phrases)
                .withCommonMinusPhrases(minusPhrases)
                .withCampaignId(campaignId)
                .withGeo(DEFAULT_GEO);
    }

    private static ShowStatRequest defaultRequestWithDeviceTypes(List<String> phrases,
                                                                 Set<Device> deviceTypes) {
        return new ShowStatRequest()
                .withPhrases(phrases)
                .withDeviceTypes(deviceTypes)
                .withGeo(DEFAULT_GEO);
    }

    private static ShowStatRequest defaultRequestWithLibraryIdAndCampaignId(List<String> phrases,
                                                                            Long campaignId) {
        return new ShowStatRequest()
                .withPhrases(phrases)
                .withCampaignId(campaignId)
                .withLibraryMinusPhrasesIds(List.of(LIBRARY_MINUS_PHRASES_ID))
                .withGeo(DEFAULT_GEO);
    }

    private static ShowStatRequest defaultRequestWithLibraryIdAndCampaignId(List<String> phrases,
                                                                            List<String> minusPhrases,
                                                                            Long campaignId) {
        return new ShowStatRequest()
                .withPhrases(phrases)
                .withCommonMinusPhrases(minusPhrases)
                .withCampaignId(campaignId)
                .withLibraryMinusPhrasesIds(List.of(LIBRARY_MINUS_PHRASES_ID))
                .withGeo(DEFAULT_GEO);
    }

    private static ShowStatRequest defaultRequest(List<String> phrases, List<String> minusPhrases) {
        return new ShowStatRequest()
                .withPhrases(phrases)
                .withCommonMinusPhrases(minusPhrases)
                .withGeo(DEFAULT_GEO);
    }

    @SuppressWarnings("SameParameterValue")
    private static ShowStatRequest requestWithNonDefaultGeo(String phrase) {
        String geos = StreamEx.of(NON_DEFAULT_GEO).map(Objects::toString).joining(",");
        return new ShowStatRequest()
                .withPhrases(List.of(phrase))
                .withGeo(geos);
    }

    private static SearchRequest defaultSearchRequest(String phrase) {
        return defaultSearchRequest(List.of(phrase));
    }

    private static SearchRequest defaultSearchRequest(List<String> phrases) {
        return SearchRequest.fromPhrases(phrases, List.of(Long.valueOf(DEFAULT_GEO)));
    }

    @SuppressWarnings("SameParameterValue")
    private static SearchRequest searchRequestWithNonDefaultGeo(String phrase) {
        return SearchRequest.fromPhrases(List.of(phrase), NON_DEFAULT_GEO);
    }
}
