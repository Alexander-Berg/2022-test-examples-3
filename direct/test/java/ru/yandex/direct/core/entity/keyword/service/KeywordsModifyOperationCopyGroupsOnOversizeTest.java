package ru.yandex.direct.core.entity.keyword.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.CompareStrategy;
import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.adgroup.container.ComplexTextAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.StatusBLGenerated;
import ru.yandex.direct.core.entity.adgroup.model.StatusModerate;
import ru.yandex.direct.core.entity.adgroup.model.StatusPostModerate;
import ru.yandex.direct.core.entity.adgroup.model.StatusShowsForecast;
import ru.yandex.direct.core.entity.adgroup.model.TextAdGroup;
import ru.yandex.direct.core.entity.adgroup.service.complex.ComplexAdGroupService;
import ru.yandex.direct.core.entity.banner.container.ComplexBanner;
import ru.yandex.direct.core.entity.banner.model.BannerDisplayHrefStatusModerate;
import ru.yandex.direct.core.entity.banner.model.BannerStatusModerate;
import ru.yandex.direct.core.entity.banner.model.BannerStatusPostModerate;
import ru.yandex.direct.core.entity.banner.model.BannerStatusSitelinksModerate;
import ru.yandex.direct.core.entity.banner.model.BannerTurboLandingStatusModerate;
import ru.yandex.direct.core.entity.banner.model.BannerVcardStatusModerate;
import ru.yandex.direct.core.entity.banner.model.BannerWithBannerImage;
import ru.yandex.direct.core.entity.banner.model.BannerWithHref;
import ru.yandex.direct.core.entity.banner.model.CpcVideoBanner;
import ru.yandex.direct.core.entity.banner.model.ImageBanner;
import ru.yandex.direct.core.entity.banner.model.StatusBannerImageModerate;
import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldCpcVideoBanner;
import ru.yandex.direct.core.entity.banner.repository.BannerTypedRepository;
import ru.yandex.direct.core.entity.banner.type.href.BannersUrlHelper;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierType;
import ru.yandex.direct.core.entity.bidmodifiers.service.BidModifierLimits;
import ru.yandex.direct.core.entity.bidmodifiers.service.BidModifierLimitsAdvanced;
import ru.yandex.direct.core.entity.client.model.ClientLimits;
import ru.yandex.direct.core.entity.keyword.container.AddedKeywordInfo;
import ru.yandex.direct.core.entity.keyword.container.KeywordsModificationContainer;
import ru.yandex.direct.core.entity.keyword.container.KeywordsModificationResult;
import ru.yandex.direct.core.entity.keyword.model.Keyword;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.BannerCreativeInfo;
import ru.yandex.direct.core.testing.info.BannerImageInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ImageHashBannerInfo;
import ru.yandex.direct.core.testing.info.ImageInfo;
import ru.yandex.direct.core.testing.info.TextBannerInfo;
import ru.yandex.direct.dbutil.model.UidAndClientId;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.direct.result.Result;
import ru.yandex.direct.test.utils.differ.BigDecimalDiffer;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.allFieldsExcept;
import static ru.yandex.direct.core.entity.adgroup.service.validation.AdGroupDefectIds.Number.MAX_ADGROUPS_IN_CAMPAIGN;
import static ru.yandex.direct.core.entity.keyword.container.KeywordsModificationContainer.COPY_AD_GROUPS_LIST_NAME;
import static ru.yandex.direct.core.entity.keyword.container.KeywordsModificationContainer.addWithCopyOnOversize;
import static ru.yandex.direct.core.entity.keyword.service.AddKeywordMatchers.isAdded;
import static ru.yandex.direct.core.entity.keyword.service.AddKeywordMatchers.isAddedToAdGroup;
import static ru.yandex.direct.core.entity.keyword.service.KeywordsModifyOperationFillComplexAdGroupTest.buildComplexAdGroup;
import static ru.yandex.direct.core.testing.data.TestBanners.activeImageHashBanner;
import static ru.yandex.direct.core.testing.data.TestBanners.activeTextBanner;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeTextCampaign;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.test.utils.matcher.LocalDateTimeMatcher.approximatelyNow;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.utils.FunctionalUtils.listToSet;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringRunner.class)
public class KeywordsModifyOperationCopyGroupsOnOversizeTest extends KeywordsModifyOperationBaseTest {

    @Autowired
    private ComplexAdGroupService complexAdGroupService;

    @Autowired
    private BannerTypedRepository bannerTypedRepository;
    @Autowired
    private BannersUrlHelper bannersUrlHelper;

    @Before
    public void before() {
        super.before();

        createTwoActiveAdGroups();
        setKeywordsCountLimit(1L);
    }

    private void setKeywordsCountLimit(long count) {
        steps.clientSteps().updateClientLimits(clientInfo
                .withClientLimits((ClientLimits) new ClientLimits().withClientId(clientInfo.getClientId())
                        .withKeywordsCountLimit(count)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void prepare_EmptyAddList_CheckArgumentsFailedOnCreateOperation() {
        KeywordsModificationContainer container =
                addWithCopyOnOversize(emptyList());

        createOperation(container);
    }

    @Test
    public void prepare_OperationWithIgnoreOversize_ValidStateReadyToApply() {
        List<Keyword> keywords = asList(validClientKeyword1(adGroupInfo1), validClientKeyword2(adGroupInfo1));

        KeywordsModificationContainer container = addWithCopyOnOversize(keywords);
        KeywordsModifyOperation operation = createOperation(container);

        Optional<Result<KeywordsModificationResult>> prepareResult = operation.prepare();
        assertThat(prepareResult.isPresent(), is(false));
    }

    @Test
    public void prepare_OversizeGroupsInCampaign_ValidationResultHasDefect() {
        steps.clientSteps().updateClientLimits(clientInfo
                .withClientLimits((ClientLimits) new ClientLimits().withClientId(clientInfo.getClientId())
                        .withBannersCountLimit(1L)
                        .withKeywordsCountLimit(1L)));

        List<Keyword> keywords = asList(validClientKeyword1(adGroupInfo1), validClientKeyword2(adGroupInfo1));
        KeywordsModifyOperation operation = createOperation(addWithCopyOnOversize(keywords));
        Optional<Result<KeywordsModificationResult>> result = operation.prepare();
        assertTrue(result.isPresent());

        ValidationResult<?, Defect> vrCopyAdGroups =
                result.get().getValidationResult().getSubResults().get(field(COPY_AD_GROUPS_LIST_NAME));
        assertThat(vrCopyAdGroups,
                hasDefectDefinitionWith(validationError(path(index(0)), MAX_ADGROUPS_IN_CAMPAIGN)));
    }

    @Test
    public void apply_OversizeByAdGroup_ResultIsSuccessful() {
        List<Keyword> keywords = asList(validClientKeyword1(adGroupInfo1), validClientKeyword2(adGroupInfo1));
        Result<KeywordsModificationResult> result = executeAddWithOversize(keywords);
        assertAddResultIsSuccessful(result, asList(isAdded(PHRASE_1), isAdded(PHRASE_2)));
    }

    @Test
    public void apply_NotOversizeByAdGroup_ResultIsSuccessful() {
        List<Keyword> keywords = singletonList(validClientKeyword1(adGroupInfo1));
        Result<KeywordsModificationResult> result = executeAddWithOversize(keywords);

        assertAddResultIsSuccessful(result, singletonList(isAdded(PHRASE_1)));
    }

    @Test
    public void apply_OversizeByAdGroup_GetSourceAdGroupIdByCopiedAdGroupIdContainsInfoAboutCopiedGroup() {
        List<Keyword> keywords = asList(validClientKeyword1(adGroupInfo1), validClientKeyword2(adGroupInfo1));
        Result<KeywordsModificationResult> result = executeAddWithOversize(keywords);
        assertAddResultIsSuccessful(result,
                asList(isAddedToAdGroup(PHRASE_1, adGroupInfo1.getAdGroupId()), isAdded(PHRASE_2)));

        KeywordsModificationResult keywordsModificationResult = result.getResult();
        assertThat(keywordsModificationResult.getSourceAdGroupIdByCopiedAdGroupId().size(), is(1));
        assertThat(keywordsModificationResult.getSourceAdGroupIdByCopiedAdGroupId().keySet(),
                not(contains(adGroupInfo1.getAdGroupId())));
        assertThat(keywordsModificationResult.getSourceAdGroupIdByCopiedAdGroupId().values(),
                contains(adGroupInfo1.getAdGroupId()));
    }

    @Test
    public void apply_OversizeByAdGroupBannerWithDomainByManager_NewAdGroupCopiedWithCopiedDomain() {
        operatorClientInfo = steps.clientSteps().createDefaultClientWithRole(RbacRole.MANAGER);
        clientInfo = steps.clientSteps().createDefaultClientUnderManager(operatorClientInfo);
        adGroupInfo1 = steps.adGroupSteps().createActiveTextAdGroup(new CampaignInfo()
                .withClientInfo(clientInfo)
                .withCampaign(activeTextCampaign(clientInfo.getClientId(), clientInfo.getUid())
                        .withManagerUid(operatorClientInfo.getUid())));

        String manualDomain = "manual-domain.ru";
        steps.bannerSteps().createBanner(activeTextBanner().withDomain(manualDomain), adGroupInfo1);

        List<Keyword> keywords = asList(validClientKeyword1(adGroupInfo1), validClientKeyword2(adGroupInfo1));
        Result<KeywordsModificationResult> result = executeAddWithOversize(keywords);
        assumeOperation(result, asList(isAdded(PHRASE_1), isAdded(PHRASE_2)));

        Long newAdGroupId = result.getResult().getAddResults().get(1).getAdGroupId();
        var banners =
                bannerTypedRepository.getBannersByGroupIds(clientInfo.getShard(), singletonList(newAdGroupId));
        assumeThat(banners, hasSize(1));

        assertThat("установленный вручную домен сохранился при копировании группы менеджером",
                ((BannerWithHref) banners.get(0)).getDomain(), is(manualDomain));
    }

    @Test
    public void apply_OversizeByAdGroupBannerWithDomainByClient_NewAdGroupCopiedWithProcessedDomain() {

        String manualDomain = "manual-domain.ru";
        TextBannerInfo bannerInfo =
                steps.bannerSteps().createBanner(activeTextBanner().withDomain(manualDomain), adGroupInfo1);

        List<Keyword> keywords = asList(validClientKeyword1(adGroupInfo1), validClientKeyword2(adGroupInfo1));
        Result<KeywordsModificationResult> result = executeAddWithOversize(keywords);
        assumeOperation(result, asList(isAdded(PHRASE_1), isAdded(PHRASE_2)));

        Long newAdGroupId = result.getResult().getAddResults().get(1).getAdGroupId();
        var banners =
                bannerTypedRepository.getBannersByGroupIds(clientInfo.getShard(), singletonList(newAdGroupId));
        assumeThat(banners, hasSize(1));

        assertThat("установленный вручную домен перевычислился при копировании группы пользователем",
                ((BannerWithHref) banners.get(0)).getDomain(),
                is(bannersUrlHelper.extractHostFromHrefWithWwwOrNull(bannerInfo.getBanner().getHref())));
    }

    @Test
    public void apply_OversizeByAdGroup_NewAdGroupCopiedInCampaign() {
        Long adGroupsCountBefore = getAdGroupsCountByCampaign(adGroupInfo1.getShard(), adGroupInfo1.getCampaignId());

        List<Keyword> keywords = asList(validClientKeyword1(adGroupInfo1), validClientKeyword2(adGroupInfo1));
        Result<KeywordsModificationResult> result = executeAddWithOversize(keywords);
        assumeOperation(result, asList(isAdded(PHRASE_1), isAdded(PHRASE_2)));

        Long adGroupsCountAfter = getAdGroupsCountByCampaign(adGroupInfo1.getShard(), adGroupInfo1.getCampaignId());
        assertThat(adGroupsCountAfter, is(adGroupsCountBefore + 1));
    }

    @Test
    public void apply_OversizeTwiceLimit_CreateTwoAdGroupsAndKeywordsSplitByDifferentAdGroups() {
        List<Keyword> keywords = asList(validClientKeyword1(adGroupInfo1),
                validClientKeyword2(adGroupInfo1),
                validClientKeyword3(adGroupInfo1));
        Result<KeywordsModificationResult> result = executeAddWithOversize(keywords);

        Long adGroupId = adGroupInfo1.getAdGroupId();
        assumeOperation(result, asList(isAddedToAdGroup(PHRASE_1, adGroupId), isAdded(PHRASE_2), isAdded(PHRASE_3)));

        List<Long> adGroupIds = mapList(result.getResult().getAddResults(), AddedKeywordInfo::getAdGroupId);
        assertThat(adGroupIds, contains(is(adGroupId), not(adGroupId), not(adGroupId)));
    }

    @Test
    public void apply_OversizeWithNoFreePlaceInExistAdGroup_CreateAdditionalAdGroup() {
        createKeyword(adGroupInfo1);

        List<Keyword> keywords = singletonList(validClientKeyword1(adGroupInfo1));
        Result<KeywordsModificationResult> result = executeAddWithOversize(keywords);
        assumeOperation(result, singletonList(isAdded(PHRASE_1)));

        assertThat(result.getResult().getAddResults(), hasSize(1));
        assertThat(result.getResult().getAddResults().get(0).getAdGroupId(), not(is(adGroupInfo1.getAdGroupId())));

    }

    /*
    Тест на максимальное заполнение полей ComplexAdGroup со всеми зависимыми подобъектами.
    Может быть достаточно хрупким, т.к. структура очень развесистая.

    Необходим для проверки корректного копирования комплексной группы.
    (операция копирвания группы используется при создании новых групп во время добавления КФ с переполнением лимита)

    Если тест часто ломается/глючит/непонятно как себя ведёт - писать @artyrian

    Сейчас есть несколько исключений в проверках.
    */
    @Test
    public void apply_AdGroupWithOtherComponents_FieldsExceptKeywordsCopiedOnOversize() {
        ComplexTextAdGroup complexAdGroup = buildComplexAdGroup(steps, clientInfo);
        long adGroupId = complexAdGroup.getAdGroup().getId();
        setKeywordsCountLimit(1L);

        Keyword keyword = new Keyword()
                .withPhrase(PHRASE_1)
                .withAdGroupId(adGroupId)
                .withPrice(BigDecimal.valueOf(DEFAULT_PRICE))
                .withPriceContext(BigDecimal.valueOf(DEFAULT_PRICE));

        // Разрешаем установку корректировки по пробкам на любые группы/кампании
        // Необходимо для тестирования expression-корректировки, пока нет ни одной реальной корректировки,
        // которая была бы разрешена на ТГО группе. В дальнейшем (когда такая корректировка появится)
        // можно будет пробки заменить на неё, и удалить этот костыль
        BidModifierLimitsAdvanced.addTestOverride(BidModifierType.EXPRESS_TRAFFIC_MULTIPLIER,
                (campaignType, adGroupWithType, clientId, featuresProvider) ->
                        new BidModifierLimits(0, 1300, true, 20));

        Result<KeywordsModificationResult> result = executeAddWithOversize(singletonList(keyword));
        assumeOperation(result, singletonList(isAdded(PHRASE_1)));

        long newAdGroupId = result.getResult().getAddResults().get(0).getAdGroupId();
        ComplexTextAdGroup copiedComplexAdGroup = getComplexAdGroup(newAdGroupId);

        complexAdGroup.getComplexBanners().forEach(x -> {
            var banner = ((TextBanner) x.getBanner());

            banner.withAdGroupId(newAdGroupId)
                    .withBsBannerId(0L)
                    .withStatusActive(false)
                    .withStatusBsSynced(StatusBsSynced.NO)
                    .withDisplayHrefStatusModerate(BannerDisplayHrefStatusModerate.READY)
                    .withImageStatusModerate(StatusBannerImageModerate.READY)
                    .withTurboLandingStatusModerate(BannerTurboLandingStatusModerate.READY)
                    .withVcardStatusModerate(BannerVcardStatusModerate.READY)
                    .withStatusSitelinksModerate(BannerStatusSitelinksModerate.READY)
                    .withStatusModerate(BannerStatusModerate.READY)
                    .withStatusPostModerate(BannerStatusPostModerate.NO);
        });

        complexAdGroup.getTargetInterests().forEach(targetInterest ->
                targetInterest.withAdGroupId(newAdGroupId)
                        .withStatusBsSynced(StatusBsSynced.NO));

        complexAdGroup.getComplexBidModifier().getDemographyModifier().withAdGroupId(newAdGroupId);
        complexAdGroup.getComplexBidModifier().getRetargetingModifier().withAdGroupId(newAdGroupId);
        complexAdGroup.getComplexBidModifier().getVideoModifier().withAdGroupId(newAdGroupId);
        complexAdGroup.getComplexBidModifier().getMobileModifier().withAdGroupId(newAdGroupId);
        complexAdGroup.getComplexBidModifier().getExpressionModifiers().forEach(x -> x.withAdGroupId(newAdGroupId));

        complexAdGroup.getRelevanceMatches().forEach(x -> x.withAdGroupId(newAdGroupId));
        complexAdGroup.getOfferRetargetings().forEach(x -> x.withAdGroupId(newAdGroupId));

        ((TextAdGroup) complexAdGroup.getAdGroup())
                .withId(newAdGroupId)
                .withStatusModerate(StatusModerate.READY)
                .withStatusBsSynced(StatusBsSynced.NO)
                .withPriorityId(0L)
                .withStatusPostModerate(StatusPostModerate.NO)
                .withStatusShowsForecast(StatusShowsForecast.NEW)
                .withStatusBLGenerated(StatusBLGenerated.YES);

        CompareStrategy compareStrategy = allFieldsExcept(
                // ключевики не копируются, добавляются новые
                newPath("keywords"),

                // устаревшее поле, больше не поддерживается
                newPath("complexBanners", "\\d+", "banner", "yaContextCategories"),

                //
                newPath("adGroup", "hasPhraseIdHref"),
                newPath("adGroup", "restrictedGeo"),
                newPath("adGroup", "minusGeo"),
                newPath("adGroup", "effectiveGeo"),

                // вычислимые поля
                newPath("complexBanners", "\\d+", "banner", "domain"),
                newPath("complexBanners", "\\d+", "banner", "reverseDomain"),
                newPath("complexBanners", "\\d+", "banner", "filterDomain"),
                newPath("complexBanners", "\\d+", "banner", "creativeRelationId"),
                newPath("complexBanners", "\\d+", "banner", "creativeStatusModerate"),
                newPath("complexBanners", "\\d+", "banner", "turboLanding", "metrikaCounters"),
                newPath("complexBanners", "\\d+", "banner", "language"),
                newPath("complexBanners", "\\d+", "banner", "showTitleAndBody"),

                newPath("complexBanners", "\\d+", "vcard", "permalink"),
                newPath("complexBanners", "\\d+", "vcard", "countryGeoId"),
                newPath("complexBanners", "\\d+", "vcard", "metroName"),

                // при чтении в репозитории свойство не отдаётся
                newPath("complexBanners", "\\d+", "sitelinkSet", "sitelinks", "\\d+", "orderNum"),
                // чтение турболендингов сайтлинков в репозитории запрещено, поэтому возвращается null, несмотря на
                // существующую запись
                newPath("complexBanners", "\\d+", "sitelinkSet", "sitelinks", "\\d+", "turboLanding"),

                // новые идентификаторы
                newPath("relevanceMatches", "\\d+", "id"),
                newPath("offerRetargetings", "\\d+", "id"),
                newPath("targetInterests", "\\d+", "id"),

                newPath("complexBidModifier", "demographyModifier", "id"),
                newPath("complexBidModifier", "demographyModifier", "demographicsAdjustments", "\\d+", "id"),

                newPath("complexBidModifier", "retargetingModifier", "id"),
                newPath("complexBidModifier", "retargetingModifier", "retargetingAdjustments", "\\d+", "id"),

                newPath("complexBidModifier", "videoModifier", "id"),
                newPath("complexBidModifier", "videoModifier", "videoAdjustment", "id"),

                newPath("complexBidModifier", "mobileModifier", "id"),
                newPath("complexBidModifier", "mobileModifier", "mobileAdjustment", "id"),

                newPath("complexBidModifier", "expressionModifiers", "\\d+", "id"),
                newPath("complexBidModifier", "expressionModifiers", "\\d+", "expressionAdjustments", "\\d+", "id"),

                newPath("complexBanners", "\\d+", "banner", "id"),
                newPath("complexBanners", "\\d+", "banner", "domainId"),
                newPath("complexBanners", "\\d+", "banner", "imageId"),
                newPath("complexBanners", "\\d+", "banner", "turboLandingParams", "bannerId"),
                newPath("complexBanners", "\\d+", "banner", "showTitleAndBody"))

                .forFields(
                        newPath("adGroup", "lastChange"),
                        newPath("relevanceMatches", "\\d+", "lastChangeTime"),
                        newPath("offerRetargetings", "\\d+", "lastChangeTime"),
                        newPath("targetInterests", "\\d+", "lastChangeTime"),
                        newPath("complexBanners", "\\d+", "vcard", "lastChange"),
                        newPath("complexBanners", "\\d+", "vcard", "lastDissociation"),
                        newPath("complexBanners", "\\d+", "banner", "lastChange"),
                        newPath("complexBanners", "\\d+", "banner", "imageDateAdded"),

                        newPath("complexBidModifier", "demographyModifier", "lastChange"),
                        newPath("complexBidModifier", "demographyModifier", "demographicsAdjustments", "\\d+",
                                "lastChange"),
                        newPath("complexBidModifier", "retargetingModifier", "lastChange"),
                        newPath("complexBidModifier", "retargetingModifier", "retargetingAdjustments", "\\d+",
                                "lastChange"),
                        newPath("complexBidModifier", "videoModifier", "lastChange"),
                        newPath("complexBidModifier", "mobileModifier", "lastChange"),
                        newPath("complexBidModifier", "mobileModifier", "mobileAdjustment", "lastChange"),
                        newPath("complexBidModifier", "expressionModifiers", "\\d+", "lastChange"),
                        newPath("complexBidModifier", "expressionModifiers", "\\d+", "expressionAdjustments", "\\d+",
                                "lastChange")
                ).useMatcher(approximatelyNow())

                .forFields(
                        newPath("targetInterests", "\\d+", "priceContext"),
                        newPath("complexBanners", "\\d+", "banner", "bannerPrice", "priceOld"),
                        newPath("complexBanners", "\\d+", "banner", "bannerPrice", "price")
                ).useDiffer(new BigDecimalDiffer());


        assertThat(copiedComplexAdGroup, beanDiffer(complexAdGroup).useCompareStrategy(compareStrategy));
    }

    @Test
    public void apply_AdGroupHasBannerWithBannerImage_CreateNewOnCopy() {
        adGroupInfo1 = steps.adGroupSteps().createActiveTextAdGroup(adGroupInfo1.getClientInfo());
        createKeyword(adGroupInfo1);

        TextBannerInfo bannerInfo = steps.bannerSteps().createDefaultBanner(adGroupInfo1);
        BannerImageInfo bannerImageInfo = steps.bannerSteps().createBannerImage(bannerInfo);

        setKeywordsCountLimit(1L);

        List<Keyword> keywords = singletonList(validClientKeyword1(adGroupInfo1));
        Result<KeywordsModificationResult> result = executeAddWithOversize(keywords);
        assumeOperation(result, singletonList(isAdded(PHRASE_1)));

        long newAdGroupId = result.getResult().getAddResults().get(0).getAdGroupId();
        ComplexTextAdGroup copiedComplexAdGroup = getComplexAdGroup(newAdGroupId);
        ComplexBanner copiedComplexBanner = copiedComplexAdGroup.getComplexBanners().get(0);

        assertThat("изображение должно быть создано новое",
                ((BannerWithBannerImage) copiedComplexBanner.getBanner()).getImageId(),
                is(not(bannerImageInfo.getBannerImageId())));
    }

    @Test
    public void apply_AdGroupHasBannerWithImage_CreateNewOnCopy() {
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup(adGroupInfo1.getClientInfo());
        createKeyword(adGroupInfo);
        ImageHashBannerInfo bannerInfo = steps.bannerSteps().createActiveImageHashBanner(
                activeImageHashBanner(adGroupInfo.getCampaignId(), adGroupInfo.getAdGroupId()),
                adGroupInfo);
        ImageInfo imageInfo = steps.bannerSteps().createImage(bannerInfo);

        setKeywordsCountLimit(1L);

        List<Keyword> keywords = singletonList(validClientKeyword1(adGroupInfo));
        Result<KeywordsModificationResult> result = executeAddWithOversize(keywords);
        assumeOperation(result, singletonList(isAdded(PHRASE_1)));

        long newAdGroupId = result.getResult().getAddResults().get(0).getAdGroupId();
        ComplexTextAdGroup copiedComplexAdGroup = getComplexAdGroup(newAdGroupId);
        ComplexBanner copiedComplexBanner = copiedComplexAdGroup.getComplexBanners().get(0);

        assertThat("изображение должно быть создано новое",
                ((ImageBanner) copiedComplexBanner.getBanner()).getImageId(),
                is(not(imageInfo.getBannerImageId())));
    }

    @Test
    public void apply_AdGroupHasCpcVideoBanner_CreateNewOnCopy() {
        BannerCreativeInfo<OldCpcVideoBanner> bannerCreativeInfo =
                steps.bannerCreativeSteps().createCpcVideoBannerCreative(clientInfo);

        AdGroupInfo adGroupInfo = bannerCreativeInfo.getBannerInfo().getAdGroupInfo();
        createKeyword(adGroupInfo);
        setKeywordsCountLimit(1L);

        List<Keyword> keywords = singletonList(validClientKeyword1(adGroupInfo));
        Result<KeywordsModificationResult> result = executeAddWithOversize(keywords);
        assumeOperation(result, singletonList(isAdded(PHRASE_1)));

        long newAdGroupId = result.getResult().getAddResults().get(0).getAdGroupId();
        ComplexTextAdGroup copiedComplexAdGroup = getComplexAdGroup(newAdGroupId);
        ComplexBanner copiedComplexBanner = copiedComplexAdGroup.getComplexBanners().get(0);
        assumeThat(copiedComplexBanner.getBanner().getId(), is(not(bannerCreativeInfo.getBannerId())));

        assertThat("креатив в новом баннере остаётся тот же",
                ((CpcVideoBanner) copiedComplexBanner.getBanner()).getCreativeId(),
                is(bannerCreativeInfo.getCreativeId()));
    }

    @Test
    public void apply_OversizeWithOtherLimit_CreateAdditionalGroups() {
        createKeyword(adGroupInfo1);
        createKeyword(adGroupInfo1);
        setKeywordsCountLimit(3L);

        List<Keyword> keywords = asList(
                clientKeyword(adGroupInfo1, "первая новая фраза", DEFAULT_PRICE),
                clientKeyword(adGroupInfo1, "вторая новая фраза", DEFAULT_PRICE),
                clientKeyword(adGroupInfo1, "третья новая фраза", DEFAULT_PRICE),
                clientKeyword(adGroupInfo1, "четвертая новая фраза", DEFAULT_PRICE),
                clientKeyword(adGroupInfo1, "пятая новая фраза", DEFAULT_PRICE));

        Result<KeywordsModificationResult> result = executeAddWithOversize(keywords);
        assumeOperation(result, asList(
                isAddedToAdGroup(keywords.get(0).getPhrase(), adGroupInfo1.getAdGroupId()),
                isAdded(keywords.get(1).getPhrase()),
                isAdded(keywords.get(2).getPhrase()),
                isAdded(keywords.get(3).getPhrase()),
                isAdded(keywords.get(4).getPhrase())));

        long adGroupId = adGroupInfo1.getAdGroupId();
        assertThat(result.getResult().getAddResults(), hasSize(5));
        assertThat(mapList(result.getResult().getAddResults(), AddedKeywordInfo::getAdGroupId),
                contains(is(adGroupId),
                        not(is(adGroupId)),
                        not(is(adGroupId)),
                        not(is(adGroupId)),
                        not(is(adGroupId))));

        assertThat(result.getResult().getSourceAdGroupIdByCopiedAdGroupId().keySet(), hasSize(2));
        assertThat(result.getResult().getSourceAdGroupIdByCopiedAdGroupId().values(), contains(adGroupId, adGroupId));
    }

    @Test
    public void apply_TwoAdGroupsOversize_CreateTwoAdditionalAdGroups() {
        setKeywordsCountLimit(2L);

        createKeyword(adGroupInfo1);
        createKeyword(adGroupInfo1);
        createKeyword(adGroupInfo2);
        createKeyword(adGroupInfo2);

        List<Keyword> keywords = asList(validClientKeyword1(adGroupInfo1), validClientKeyword2(adGroupInfo2));
        Result<KeywordsModificationResult> result = executeAddWithOversize(keywords);
        assumeOperation(result, asList(isAdded(PHRASE_1), isAdded(PHRASE_2)));

        assertThat("должны быть созданы 2 новые группы при копировании",
                listToSet(result.getResult().getAddResults(), AddedKeywordInfo::getAdGroupId), hasSize(2));
        assertThat(mapList(result.getResult().getAddResults(), AddedKeywordInfo::getAdGroupId),
                contains(not(is(adGroupInfo1.getAdGroupId())), not(is(adGroupInfo2.getAdGroupId()))));
    }


    @Test
    public void apply_SaveInTwoAdGroupsOversizeInOne_CreateOneAdditionalAdGroups() {
        createKeyword(adGroupInfo1);

        List<Keyword> keywords = asList(validClientKeyword1(adGroupInfo1), validClientKeyword2(adGroupInfo2));
        Result<KeywordsModificationResult> result = executeAddWithOversize(keywords);
        assumeOperation(result, asList(isAdded(PHRASE_1), isAdded(PHRASE_2)));

        assertThat(result.getResult().getAddResults(), hasSize(2));
        assertThat(mapList(result.getResult().getAddResults(), AddedKeywordInfo::getAdGroupId),
                contains(not(is(adGroupInfo1.getAdGroupId())), is(adGroupInfo2.getAdGroupId())));
    }

    private Long getAdGroupsCountByCampaign(int shard, long campaignId) {
        return adGroupRepository.getAdGroupCountByCampaignIds(shard, singleton(campaignId)).get(campaignId);
    }

    private ComplexTextAdGroup getComplexAdGroup(Long newAdGroupId) {
        List<ComplexTextAdGroup> complexAdGroups = complexAdGroupService.getComplexAdGroupsWithoutKeywords(
                clientInfo.getUid(), UidAndClientId.of(clientInfo.getUid(), clientInfo.getClientId()),
                singleton(newAdGroupId));
        assumeThat(complexAdGroups, hasSize(1));

        return complexAdGroups.get(0);
    }

    private void assumeOperation(Result<KeywordsModificationResult> result, List<Matcher<AddedKeywordInfo>> matchers) {
        KeywordsModificationResult resultValue = result.getResult();
        assumeThat(result.getValidationResult(), hasNoDefectsDefinitions());
        List<AddedKeywordInfo> addResults = resultValue.getAddResults();

        assumeThat("количество результатов добавления не соответствует ожидаемому",
                addResults, hasSize(matchers.size()));

        for (int i = 0; i < addResults.size(); i++) {
            assumeThat(addResults.get(i), matchers.get(i));
            assertThat(addResults.get(i), matchers.get(i));
        }
    }
}
