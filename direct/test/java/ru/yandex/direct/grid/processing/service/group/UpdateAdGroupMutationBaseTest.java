package ru.yandex.direct.grid.processing.service.group;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableSet;
import one.util.streamex.StreamEx;
import org.assertj.core.api.SoftAssertions;
import org.junit.After;
import org.junit.Rule;
import org.junit.rules.ExpectedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.context.SecurityContextHolder;

import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.service.AdGroupService;
import ru.yandex.direct.core.entity.banner.model.Banner;
import ru.yandex.direct.core.entity.banner.model.BannerWithSystemFields;
import ru.yandex.direct.core.entity.banner.service.BannerService;
import ru.yandex.direct.core.entity.bidmodifier.BidModifier;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierDemographics;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierType;
import ru.yandex.direct.core.entity.client.service.ClientGeoService;
import ru.yandex.direct.core.entity.keyword.model.Keyword;
import ru.yandex.direct.core.entity.keyword.model.StatusModerate;
import ru.yandex.direct.core.entity.keyword.service.KeywordService;
import ru.yandex.direct.core.entity.offerretargeting.model.OfferRetargeting;
import ru.yandex.direct.core.entity.offerretargeting.repository.OfferRetargetingRepository;
import ru.yandex.direct.core.entity.offerretargeting.service.OfferRetargetingService;
import ru.yandex.direct.core.entity.relevancematch.model.RelevanceMatch;
import ru.yandex.direct.core.entity.relevancematch.repository.RelevanceMatchRepository;
import ru.yandex.direct.core.entity.relevancematch.service.RelevanceMatchService;
import ru.yandex.direct.core.entity.retargeting.model.Retargeting;
import ru.yandex.direct.core.entity.retargeting.repository.RetargetingRepository;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.testing.info.AdGroupBidModifierInfo;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.KeywordInfo;
import ru.yandex.direct.core.testing.info.NewTextBannerInfo;
import ru.yandex.direct.core.testing.info.RetargetingInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.steps.TextBannerSteps;
import ru.yandex.direct.grid.processing.model.bidmodifier.mutation.GdUpdateBidModifierDemographics;
import ru.yandex.direct.grid.processing.model.bidmodifier.mutation.GdUpdateBidModifierDemographicsAdjustmentItem;
import ru.yandex.direct.grid.processing.model.bidmodifier.mutation.GdUpdateBidModifiers;
import ru.yandex.direct.grid.processing.model.group.mutation.GdUpdateAdGroupKeywordItem;
import ru.yandex.direct.grid.processing.model.group.mutation.GdUpdateAdGroupOfferRetargetingItem;
import ru.yandex.direct.grid.processing.model.group.mutation.GdUpdateAdGroupRelevanceMatchItem;
import ru.yandex.direct.grid.processing.model.group.mutation.GdUpdateTextAdGroup;
import ru.yandex.direct.grid.processing.model.group.mutation.GdUpdateTextAdGroupItem;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.util.GraphQlJsonUtils;
import ru.yandex.direct.grid.processing.util.TestAuthHelper;
import ru.yandex.direct.grid.processing.util.UserHelper;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.assertj.core.util.BigDecimalComparator.BIG_DECIMAL_COMPARATOR;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.grid.processing.service.bidmodifier.BidModifierDataConverter.toGdAge;
import static ru.yandex.direct.grid.processing.service.bidmodifier.BidModifierDataConverter.toGdBidModifierType;
import static ru.yandex.direct.grid.processing.service.bidmodifier.BidModifierDataConverter.toGdGender;
import static ru.yandex.direct.regions.Region.KAZAKHSTAN_REGION_ID;
import static ru.yandex.direct.utils.CommonUtils.nvl;
import static ru.yandex.direct.utils.FunctionalUtils.listToMap;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@SuppressWarnings({"squid:S2970", "squid:S2187"})
public class UpdateAdGroupMutationBaseTest {

    protected static final String MUTATION_NAME = "updateTextAdGroup";
    private static final String MUTATION_TEMPLATE = ""
            + "mutation {\n"
            + "  %s (input: %s) {\n"
            + "    updatedAdGroupItems {\n"
            + "         adGroupId,\n"
            + "    }\n"
            + "    validationResult {\n"
            + "      errors {\n"
            + "        code\n"
            + "        params\n"
            + "        path\n"
            + "      }\n"
            + "      warnings {\n"
            + "        code\n"
            + "        params\n"
            + "        path\n"
            + "      }\n"
            + "    }\n"
            + "  }\n"
            + "}";

    protected Long adGroupId;
    protected User operator;
    protected ClientInfo clientInfo;
    AdGroupInfo textAdGroupInfo;
    NewTextBannerInfo textBannerInfo;

    AdGroupBidModifierInfo adGroupBidModifierInfo;

    protected KeywordInfo[] keywordInfo = new KeywordInfo[2];

    RelevanceMatch relevanceMatch;

    OfferRetargeting offerRetargeting;

    protected RetargetingInfo retargetingInfo;

    protected static final String[] KEYWORD = {"sex", "drug"};
    private static final List<Long> DEFAULT_GEO = singletonList(KAZAKHSTAN_REGION_ID);
    private static final List<String> PAGE_GROUP_TAGS = singletonList("page_group_tag");
    private static final List<String> TARGET_TAGS = singletonList("target_tag");

    @Autowired
    @Qualifier(GRAPH_QL_PROCESSOR)
    protected GridGraphQLProcessor processor;

    @Autowired
    protected AdGroupService adGroupService;

    @Autowired
    protected BannerService bannerService;

    @Autowired
    protected KeywordService keywordService;

    @Autowired
    protected RetargetingRepository retargetingRepository;

    @Autowired
    protected RelevanceMatchService relevanceMatchService;

    @Autowired
    protected RelevanceMatchRepository relevanceMatchRepository;

    @Autowired
    protected OfferRetargetingService offerRetargetingService;

    @Autowired
    protected OfferRetargetingRepository offerRetargetingRepository;

    @Autowired
    protected ClientGeoService clientGeoService;

    @Autowired
    protected TextBannerSteps textBannerSteps;

    @Autowired
    protected Steps steps;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    SoftAssertions softAssertions = new SoftAssertions();

    protected void initTestData() {
        clientInfo = steps.clientSteps().createDefaultClient();

        // Создаем текстовую группу с 2мя ключевыми фразами, ретаргетингом, автотаргетингом и корректировкой.
        // Все промодерированно и синхронизированно с БК
        textAdGroupInfo = steps.adGroupSteps().createDefaultAdGroup(clientInfo);
        //ставим непустые pageGroupTags и targetTags для более наглядной проверки
        steps.adGroupSteps().setAdGroupProperty(textAdGroupInfo, AdGroup.PAGE_GROUP_TAGS, PAGE_GROUP_TAGS);
        steps.adGroupSteps().setAdGroupProperty(textAdGroupInfo, AdGroup.TARGET_TAGS, TARGET_TAGS);
        //ставим не Россию, чтобы не мешал транслокальный Крым
        steps.adGroupSteps().setAdGroupProperty(textAdGroupInfo, AdGroup.GEO, DEFAULT_GEO);

        NewTextBannerInfo newTextBannerInfo =
                new NewTextBannerInfo().withAdGroupInfo(textAdGroupInfo);
        textBannerInfo = textBannerSteps.createBanner(newTextBannerInfo);

        keywordInfo[0] = steps.keywordSteps().createKeywordWithText(KEYWORD[0], textAdGroupInfo);
        keywordInfo[1] = steps.keywordSteps().createKeywordWithText(KEYWORD[1], textAdGroupInfo);
        //ключевые фразы промодерированы
        steps.keywordSteps()
                .updateKeywordsProperty(Arrays.asList(keywordInfo), Keyword.STATUS_MODERATE, StatusModerate.YES);

        adGroupBidModifierInfo = steps.bidModifierSteps().createDefaultAdGroupBidModifierDemographics(textAdGroupInfo);

        adGroupId = textAdGroupInfo.getAdGroupId();

        operator = UserHelper.getUser(textAdGroupInfo);
        TestAuthHelper.setDirectAuthentication(operator);
    }

    @After
    public void afterTest() {
        SecurityContextHolder.clearContext();
    }

    GdUpdateTextAdGroup createRequest(AdGroupInfo adGroup) {
        List<GdUpdateAdGroupKeywordItem> keywordItems = mapList(Arrays.asList(keywordInfo),
                kw -> createKeywordInputItem(kw.getId(), kw.getKeyword().getPhrase()));

        List<GdUpdateTextAdGroupItem> adGroupItems = singletonList(new GdUpdateTextAdGroupItem()
                .withAdGroupId(adGroup.getAdGroupId())
                .withAdGroupName(adGroup.getAdGroup().getName())
                .withAdGroupMinusKeywords(adGroup.getAdGroup().getMinusKeywords())
                .withLibraryMinusKeywordsIds(adGroup.getAdGroup().getLibraryMinusKeywordsIds())
                .withRegionIds(mapList(adGroup.getAdGroup().getGeo(), Long::intValue))
                .withKeywords(keywordItems));

        return new GdUpdateTextAdGroup()
                .withUpdateItems(adGroupItems);
    }

    GdUpdateAdGroupKeywordItem createKeywordInputItem(@Nullable Long keywordId, String phrase) {
        return new GdUpdateAdGroupKeywordItem()
                .withId(keywordId)
                .withPhrase(phrase);
    }

    GdUpdateAdGroupKeywordItem createKeywordInputItem(KeywordInfo keywordInfo) {
        return new GdUpdateAdGroupKeywordItem()
                .withId(keywordInfo.getId())
                .withPhrase(keywordInfo.getKeyword().getPhrase());
    }


    protected List<GdUpdateAdGroupKeywordItem> createKeywordInputItems(KeywordInfo... keywordInfo) {
        return StreamEx.of(Arrays.asList(keywordInfo))
                .map(kwInfo -> new GdUpdateAdGroupKeywordItem()
                        .withId(kwInfo.getId())
                        .withPhrase(kwInfo.getKeyword().getPhrase()))
                .toList();
    }

    protected GdUpdateTextAdGroup createSingleAdGroupUpdateRequest(AdGroupInfo adGroup, @Nullable String name,
                                                                   @Nullable List<String> minusKeywords,
                                                                   @Nullable List<Integer> regionIds) {
        List<GdUpdateTextAdGroupItem> adGroupItems = singletonList(new GdUpdateTextAdGroupItem()
                .withAdGroupId(adGroup.getAdGroupId())
                .withAdGroupName(nvl(name, adGroup.getAdGroup().getName()))
                .withAdGroupMinusKeywords(nvl(minusKeywords, Collections.emptyList()))
                .withLibraryMinusKeywordsIds(adGroup.getAdGroup().getLibraryMinusKeywordsIds())
                .withRegionIds(nvl(regionIds, mapList(adGroup.getAdGroup().getGeo(), Long::intValue))));
        return new GdUpdateTextAdGroup()
                .withUpdateItems(adGroupItems);
    }

    void checkAdGroupsDbState(AdGroup expectedAdGroup) {
        checkAdGroupsDbState(expectedAdGroup, "lastChange");
    }

    private void checkAdGroupsDbState(AdGroup expectedAdGroup, String... ignoreFields) {
        List<AdGroup> adGroups =
                adGroupService.getAdGroups(operator.getClientId(), singletonList(expectedAdGroup.getId()));

        softAssertions.assertThat(adGroups)
                .usingElementComparatorIgnoringFields(ignoreFields)
                .containsExactly(expectedAdGroup);
    }

    void checkKeywordDbState(Collection<Keyword> expectedKeywords) {
        List<Keyword> actualKeywords = keywordService
                .getKeywords(clientInfo.getClientId(), mapList(expectedKeywords, Keyword::getId));

        softAssertions.assertThat(actualKeywords)
                .usingComparatorForElementFieldsWithType(BIG_DECIMAL_COMPARATOR, BigDecimal.class)
                .usingElementComparatorIgnoringFields("modificationTime")
                .containsExactlyElementsOf(expectedKeywords);
    }

    List<BidModifier> getActualBidModifiers() {
        return steps.bidModifierSteps().getAdGroupBidModifiers(clientInfo.getShard(), textAdGroupInfo.getCampaignId(),
                textAdGroupInfo.getAdGroupId(),
                ImmutableSet.copyOf(BidModifierType.values())).getBidModifiers();
    }

    /**
     * Проверяем, что BidModifiers соответствуют ожидаемым
     * Adjustment'ы не проверяются
     *
     * @param expectedBidModifiers
     */
    void checkBidModifiersDbState(List<BidModifier> expectedBidModifiers) {
        List<BidModifier> bidModifiers = getActualBidModifiers();
        bidModifiers.sort(Comparator.comparing(BidModifier::getType));
        expectedBidModifiers.sort(Comparator.comparing(BidModifier::getType));
        softAssertions.assertThat(bidModifiers)
                .usingComparatorForElementFieldsWithType((o1, o2) -> 0, LocalDateTime.class)
                //adjustments проверяем отдельно
                .usingComparatorForElementFieldsWithType((o1, o2) -> 0, List.class)
                .usingComparatorForElementFieldsWithType((o1, o2) -> 0, BidModifierAdjustment.class)
                .usingElementComparatorIgnoringFields("id")
                .containsExactlyElementsOf(expectedBidModifiers);
    }

    /**
     * Проверка нового ключевого слова (с неизвестным id)
     * <p>
     * Новые ключевые слова определяем вычитанием из всех ключевиков группы исходных
     * ключевых фраз {@param existentKeywords}
     *
     * @param expectedNewKeyword ожидаемое состояние нового ключевого слова, будут проверены все не-{@code null} поля
     * @param initialKeywords    существующие ключевые слова группы.
     *                           eсли не заданы, вычисляются по {@param UpdateAdGroupMutationBaseTest.keywordInfo}
     */
    void checkNewKeywordDbState(Keyword expectedNewKeyword,
                                @Nullable Collection<Keyword> initialKeywords,
                                Long adGroupId) {
        Map<Long, List<Keyword>> allKeywords = keywordService
                .getKeywordsByAdGroupIds(clientInfo.getClientId(), singleton(adGroupId));

        softAssertions.assertThat(allKeywords).containsOnlyKeys(adGroupId);

        Map<Long, Keyword> origKeywordsByIds =
                listToMap(nvl(initialKeywords, mapList(Arrays.asList(keywordInfo), KeywordInfo::getKeyword)),
                        Keyword::getId,
                        Function.identity());

        List<Keyword> newKeywords = StreamEx.of(allKeywords.get(adGroupId))
                .filter(kw -> !origKeywordsByIds.containsKey(kw.getId()))
                .toList();

        softAssertions.assertThat(newKeywords)
                .hasSize(1);

        softAssertions.assertThat(newKeywords)
                .element(0)
                .usingComparatorForType(BIG_DECIMAL_COMPARATOR, BigDecimal.class)
                .isEqualToIgnoringNullFields(expectedNewKeyword);
    }

    void checkAdGroupRetargetingDbState(Retargeting expectedRetargeting) {
        List<Retargeting> retargetings = retargetingRepository
                .getRetargetingsByAdGroups(clientInfo.getShard(), singletonList(expectedRetargeting.getAdGroupId()));

        softAssertions.assertThat(retargetings)
                .hasSize(1);

        softAssertions.assertThat(retargetings)
                .element(0)
                .usingComparatorForType(BIG_DECIMAL_COMPARATOR, BigDecimal.class)
                .isEqualToIgnoringGivenFields(expectedRetargeting, "lastChangeTime");
    }

    void checkRelevanceMatchDbState(RelevanceMatch expectedRelevanceMatch) {
        List<RelevanceMatch> actualRelevanceMatch =
                relevanceMatchService.getRelevanceMatchByIds(clientInfo.getClientId(),
                        singletonList(expectedRelevanceMatch.getId()));

        softAssertions.assertThat(actualRelevanceMatch)
                .hasSize(1);

        softAssertions.assertThat(actualRelevanceMatch)
                .element(0)
                .usingComparatorForType(BIG_DECIMAL_COMPARATOR, BigDecimal.class)
                .isEqualToIgnoringNullFields(expectedRelevanceMatch);
    }

    void checkNewRelevanceMatch(Long adGroupId, RelevanceMatch expectedRelevanceMatch) {
        Map<Long, RelevanceMatch> relevanceMatchMap = relevanceMatchRepository
                .getRelevanceMatchesByAdGroupIds(clientInfo.getShard(), clientInfo.getClientId(),
                        singletonList(adGroupId));

        softAssertions.assertThat(relevanceMatchMap)
                .containsOnlyKeys(adGroupId);

        softAssertions.assertThat(relevanceMatchMap.get(adGroupId))
                .usingComparatorForType(BIG_DECIMAL_COMPARATOR, BigDecimal.class)
                .isEqualToIgnoringNullFields(expectedRelevanceMatch);
    }

    void checkRelevanceMatchIsDeleted(Collection<Long> adGroupId) {
        Map<Long, RelevanceMatch> relevanceMatchMap = relevanceMatchRepository
                .getRelevanceMatchesByAdGroupIds(clientInfo.getShard(), clientInfo.getClientId(), adGroupId);

        softAssertions.assertThat(relevanceMatchMap)
                .isEmpty();
    }

    void checkOfferRetargetingDbState(OfferRetargeting expectedOfferRetargeting) {
        List<OfferRetargeting> actualOfferRetargetings =
                StreamEx.of(offerRetargetingRepository.getOfferRetargetingsByIds(clientInfo.getShard(),
                        clientInfo.getClientId(), singletonList(expectedOfferRetargeting.getId())).values()).toList();

        softAssertions.assertThat(actualOfferRetargetings).hasSize(1);

        softAssertions.assertThat(actualOfferRetargetings)
                .element(0)
                .usingComparatorForType(BIG_DECIMAL_COMPARATOR, BigDecimal.class)
                .isEqualToIgnoringNullFields(expectedOfferRetargeting);
    }

    void checkNewOfferRetargeting(Long adGroupId, OfferRetargeting expectedOfferRetargeting) {
        Map<Long, OfferRetargeting> offerRetargetingMap = offerRetargetingRepository
                .getOfferRetargetingsByAdGroupIds(clientInfo.getShard(), clientInfo.getClientId(),
                        singletonList(adGroupId));

        softAssertions.assertThat(offerRetargetingMap)
                .containsOnlyKeys(adGroupId);

        softAssertions.assertThat(offerRetargetingMap.get(adGroupId))
                .usingComparatorForType(BIG_DECIMAL_COMPARATOR, BigDecimal.class)
                .isEqualToIgnoringNullFields(expectedOfferRetargeting);
    }

    void checkOfferRetargetingIsDeleted(Long adGroupId) {
        Map<Long, OfferRetargeting> offerRetargetingMap = offerRetargetingRepository
                .getOfferRetargetingsByAdGroupIds(clientInfo.getShard(), clientInfo.getClientId(),
                        singletonList(adGroupId));

        softAssertions.assertThat(offerRetargetingMap).isEmpty();
    }

    void checkBannerDbState(Banner expectedBanner) {

        List<BannerWithSystemFields> actualBanners =
                bannerService.getBannersByIds(ImmutableSet.of(expectedBanner.getId()));

        softAssertions.assertThat(actualBanners)
                .hasSize(1);

        softAssertions.assertThat(actualBanners)
                .element(0)
                .isEqualToIgnoringGivenFields(expectedBanner, "lastChange");
    }

    protected void checkAdGroupInvariability() {
        checkAdGroupsDbState(textAdGroupInfo.getAdGroup(), new String[]{});
    }

    public UpdateAdGroupItemRequestBuilder requestBuilder() {
        return new UpdateAdGroupItemRequestBuilder();
    }

    String getQuery(GdUpdateTextAdGroup request) {
        return String.format(MUTATION_TEMPLATE, MUTATION_NAME, GraphQlJsonUtils.graphQlSerialize(request));
    }

    public class UpdateAdGroupItemRequestBuilder {

        private GdUpdateTextAdGroupItem adGroupItem = new GdUpdateTextAdGroupItem();

        UpdateAdGroupItemRequestBuilder getRequestFromOriginalAdGroupParams() {
            List<GdUpdateAdGroupKeywordItem> keywordItems = mapList(Arrays.asList(keywordInfo),
                    kw -> createKeywordInputItem(kw.getId(), kw.getKeyword().getPhrase()));

            adGroupItem
                    .withAdGroupId(textAdGroupInfo.getAdGroupId())
                    .withAdGroupName(textAdGroupInfo.getAdGroup().getName())
                    .withAdGroupMinusKeywords(textAdGroupInfo.getAdGroup().getMinusKeywords())
                    .withLibraryMinusKeywordsIds(textAdGroupInfo.getAdGroup().getLibraryMinusKeywordsIds())
                    .withRegionIds(mapList(textAdGroupInfo.getAdGroup().getGeo(), Long::intValue))
                    .withRelevanceMatch(getGdUpdateAdGroupRelevanceMatchItem(relevanceMatch))
                    .withOfferRetargeting(getGdUpdateAdGroupOfferRetargetingItem(offerRetargeting))
                    .withBidModifiers(getGdUpdateBidModifiers(adGroupBidModifierInfo))
                    .withUseBidModifiers(true)
                    .withKeywords(keywordItems);
            return this;
        }

        UpdateAdGroupItemRequestBuilder addKeyword(@Nullable Long keywordId, String phrase) {
            adGroupItem.getKeywords().add(new GdUpdateAdGroupKeywordItem()
                    .withId(keywordId)
                    .withPhrase(phrase));
            return this;
        }

        UpdateAdGroupItemRequestBuilder removeLastKeywordIfExists() {
            List<GdUpdateAdGroupKeywordItem> keywords = adGroupItem.getKeywords();
            if (!keywords.isEmpty()) {
                keywords.remove(keywords.size() - 1);
            }
            return this;
        }

        GdUpdateAdGroupRelevanceMatchItem getGdUpdateAdGroupRelevanceMatchItem(RelevanceMatch relevanceMatch) {
            if (relevanceMatch != null) {
                return getGdUpdateAdGroupRelevanceMatchItem(!relevanceMatch.getIsDeleted(), relevanceMatch.getId());
            } else {
                return getGdUpdateAdGroupRelevanceMatchItem(false, null);
            }
        }

        GdUpdateAdGroupOfferRetargetingItem getGdUpdateAdGroupOfferRetargetingItem(OfferRetargeting offerRetargeting) {
            if (offerRetargeting != null) {
                return getGdUpdateAdGroupOfferRetargetingItem(!offerRetargeting.getIsDeleted(),
                        offerRetargeting.getId());
            } else {
                return null;
            }
        }

        GdUpdateBidModifiers getGdUpdateBidModifiers(AdGroupBidModifierInfo adGroupBidModifierInfo) {
            return new GdUpdateBidModifiers().withBidModifierDemographics(
                    new GdUpdateBidModifierDemographics()
                            .withAdGroupId(adGroupBidModifierInfo.getAdGroupId())
                            .withCampaignId(adGroupBidModifierInfo.getCampaignId())
                            .withEnabled(adGroupBidModifierInfo.getBidModifier().getEnabled())
                            .withId(adGroupBidModifierInfo.getBidModifier().getId())
                            .withType(toGdBidModifierType(adGroupBidModifierInfo.getBidModifier().getType()))
                            .withAdjustments(mapList(((BidModifierDemographics) adGroupBidModifierInfo.getBidModifier())
                                    .getDemographicsAdjustments(), demographicsAdjustment ->
                                    new GdUpdateBidModifierDemographicsAdjustmentItem()
                                            .withAge(toGdAge(demographicsAdjustment.getAge()))
                                            .withGender(toGdGender(demographicsAdjustment.getGender()))
                                            .withId(demographicsAdjustment.getId())
                                            .withPercent(demographicsAdjustment.getPercent()))
                            )
            );
        }

        public UpdateAdGroupItemRequestBuilder setBidModifiers(GdUpdateBidModifiers bidModifiers) {
            adGroupItem.withBidModifiers(bidModifiers);
            return this;
        }

        UpdateAdGroupItemRequestBuilder setUseBidModifiers(@Nullable Boolean useBidModifiers) {
            adGroupItem.withUseBidModifiers(useBidModifiers);
            return this;
        }


        GdUpdateAdGroupRelevanceMatchItem getGdUpdateAdGroupRelevanceMatchItem(Boolean isActive, @Nullable Long id) {
            return new GdUpdateAdGroupRelevanceMatchItem()
                    .withIsActive(isActive)
                    .withId(id);
        }

        GdUpdateAdGroupOfferRetargetingItem getGdUpdateAdGroupOfferRetargetingItem(Boolean isActive,
                                                                                   @Nullable Long id) {
            return new GdUpdateAdGroupOfferRetargetingItem()
                    .withIsActive(isActive)
                    .withId(id);
        }

        public UpdateAdGroupItemRequestBuilder setKeywords(
                List<GdUpdateAdGroupKeywordItem> gdUpdateAdGroupKeywordItems) {
            adGroupItem.withKeywords(gdUpdateAdGroupKeywordItems);
            return this;
        }

        public UpdateAdGroupItemRequestBuilder setKeywords(GdUpdateAdGroupKeywordItem... gdUpdateAdGroupKeywordItem) {
            adGroupItem.withKeywords(Arrays.asList(gdUpdateAdGroupKeywordItem));
            return this;
        }

        UpdateAdGroupItemRequestBuilder setRelevanceMatch(Boolean isActive, @Nullable Long id) {
            adGroupItem.withRelevanceMatch(getGdUpdateAdGroupRelevanceMatchItem(isActive, id));
            return this;
        }

        UpdateAdGroupItemRequestBuilder disableRelevanceMatch() {
            adGroupItem.getRelevanceMatch().withIsActive(false);
            return this;
        }

        UpdateAdGroupItemRequestBuilder setOfferRetargeting(Boolean isActive, @Nullable Long id) {
            adGroupItem.withOfferRetargeting(getGdUpdateAdGroupOfferRetargetingItem(isActive, id));
            return this;
        }

        UpdateAdGroupItemRequestBuilder disableOfferRetargeting() {
            adGroupItem.getOfferRetargeting().withIsActive(false);
            return this;
        }

        UpdateAdGroupItemRequestBuilder setGeneralPrice(BigDecimal generalPrice) {
            adGroupItem.withGeneralPrice(generalPrice);
            return this;
        }

        public UpdateAdGroupItemRequestBuilder setGeo(List<Long> geo) {
            adGroupItem.withRegionIds(mapList(geo, Long::intValue));
            return this;
        }

        public UpdateAdGroupItemRequestBuilder setMinusKeywords(List<String> minusKeywords) {
            adGroupItem.withAdGroupMinusKeywords(minusKeywords);
            return this;
        }

        UpdateAdGroupItemRequestBuilder setLibraryMinusKeywordsIds(List<Long> libraryMinusKeywordsIds) {
            adGroupItem.withLibraryMinusKeywordsIds(libraryMinusKeywordsIds);
            return this;
        }

        public UpdateAdGroupItemRequestBuilder setName(String name) {
            adGroupItem.withAdGroupName(name);
            return this;
        }

        public UpdateAdGroupItemRequestBuilder setPageGroupTags(List<String> pageGroupTags) {
            adGroupItem.withPageGroupTags(pageGroupTags);
            return this;
        }

        public UpdateAdGroupItemRequestBuilder setTargetTags(List<String> targetTags) {
            adGroupItem.withTargetTags(targetTags);
            return this;
        }

        public GdUpdateTextAdGroup build() {
            return new GdUpdateTextAdGroup()
                    .withUpdateItems(singletonList(adGroupItem));
        }
    }
}
