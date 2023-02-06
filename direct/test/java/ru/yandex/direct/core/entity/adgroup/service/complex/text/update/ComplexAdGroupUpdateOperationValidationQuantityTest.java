package ru.yandex.direct.core.entity.adgroup.service.complex.text.update;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.adgroup.container.ComplexTextAdGroup;
import ru.yandex.direct.core.entity.banner.container.ComplexBanner;
import ru.yandex.direct.core.entity.banner.model.old.Image;
import ru.yandex.direct.core.entity.client.service.ClientLimitsService;
import ru.yandex.direct.core.entity.keyword.model.Keyword;
import ru.yandex.direct.core.entity.retargeting.Constants;
import ru.yandex.direct.core.entity.retargeting.model.TargetInterest;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.BannerImageFormat;
import ru.yandex.direct.core.testing.info.KeywordInfo;
import ru.yandex.direct.core.testing.info.RetConditionInfo;
import ru.yandex.direct.core.testing.info.RetargetingInfo;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.Path;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.adgroup.container.ComplexTextAdGroup.COMPLEX_BANNERS;
import static ru.yandex.direct.core.entity.adgroup.container.ComplexTextAdGroup.KEYWORDS;
import static ru.yandex.direct.core.entity.adgroup.container.ComplexTextAdGroup.OFFER_RETARGETINGS;
import static ru.yandex.direct.core.entity.adgroup.container.ComplexTextAdGroup.RELEVANCE_MATCHES;
import static ru.yandex.direct.core.entity.adgroup.service.complex.ComplexTextAdGroupTestData.defaultOfferRetargeting;
import static ru.yandex.direct.core.entity.adgroup.service.complex.ComplexTextAdGroupTestData.defaultRelevanceMatch;
import static ru.yandex.direct.core.entity.adgroup.service.complex.ComplexTextAdGroupTestData.emptyAdGroupWithModelForUpdate;
import static ru.yandex.direct.core.entity.adgroup.service.complex.ComplexTextAdGroupTestData.emptyBannerForAdd;
import static ru.yandex.direct.core.entity.adgroup.service.complex.ComplexTextAdGroupTestData.fullAdGroupForUpdate;
import static ru.yandex.direct.core.entity.adgroup.service.complex.ComplexTextAdGroupTestData.randomPhraseKeywordForAdd;
import static ru.yandex.direct.core.entity.adgroup.service.complex.ComplexTextAdGroupTestData.randomPriceRetargeting;
import static ru.yandex.direct.core.entity.banner.service.validation.BannerConstants.MAX_BANNERS_IN_ADGROUP;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.maxBannersInAdGroup;
import static ru.yandex.direct.core.entity.keyword.service.validation.KeywordDefects.maxKeywordsPerAdGroupExceeded;
import static ru.yandex.direct.core.entity.offerretargeting.validation.OfferRetargetingDefects.tooManyOfferRetargetingsInAdGroup;
import static ru.yandex.direct.core.entity.relevancematch.valdiation.RelevanceMatchDefects.maxRelevanceMatchesInAdGroup;
import static ru.yandex.direct.core.entity.retargeting.service.validation2.RetargetingDefects.maxCollectionSizeAdGroup;
import static ru.yandex.direct.core.testing.data.TestBanners.activeImageHashBanner;
import static ru.yandex.direct.core.testing.data.TestBanners.imageAdImageFormat;
import static ru.yandex.direct.core.testing.data.TestImages.defaultImage;
import static ru.yandex.direct.core.testing.data.TestRetargetings.defaultRetargeting;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringRunner.class)
public class ComplexAdGroupUpdateOperationValidationQuantityTest extends
        ComplexAdGroupUpdateOperationValidationTestBase {

    @Autowired
    private ClientLimitsService clientLimitsService;

    @Test
    public void hasOneErrorWhenTooManyBanners() {
        createSecondAdGroup();
        createImageHashBanner(adGroupInfo1);

        List<ComplexBanner> banners = new ArrayList<>();
        for (int i = 0; i < MAX_BANNERS_IN_ADGROUP; i++) {
            banners.add(emptyBannerForAdd());
        }
        ComplexTextAdGroup invalidAdGroup = emptyAdGroupWithModelForUpdate(adGroupId, banners, COMPLEX_BANNERS);
        ComplexTextAdGroup validAdGroup = fullAdGroupForUpdate(adGroupInfo2.getAdGroupId(), retConditionId);

        ValidationResult<?, Defect> vr = updateAndCheckResultIsFailed(validAdGroup, invalidAdGroup);
        Path errPath = path(index(1), field(COMPLEX_BANNERS.name()));
        assertThat(vr, hasDefectDefinitionWith(validationError(errPath, maxBannersInAdGroup(MAX_BANNERS_IN_ADGROUP))));
        vr.flattenErrors().forEach(validationErrorLogger::info);
        assertThat("ожидается только одна ошибка", vr.flattenErrors(), hasSize(1));
    }

    @Test
    public void hasErrorsWhenTooManyKeywords() {
        createSecondAdGroup();

        int keywordsLimit = (int) (long) clientLimitsService
                .massGetClientLimits(singletonList(campaignInfo.getClientId()))
                .iterator().next().getKeywordsCountLimitOrDefault();
        KeywordInfo keywordInfo = steps.keywordSteps().createKeywordWithText("abc", adGroupInfo1);

        List<Keyword> keywords = new ArrayList<>();
        Keyword updatedKeyword = new Keyword().withId(keywordInfo.getId()).withPhrase("abcde")
                .withPrice(BigDecimal.valueOf(12.0)).withPriceContext(BigDecimal.valueOf(12.0));
        keywords.add(updatedKeyword);
        for (int i = 0; i < keywordsLimit; i++) {
            keywords.add(randomPhraseKeywordForAdd());
        }
        ComplexTextAdGroup invalidAdGroup = emptyAdGroupWithModelForUpdate(adGroupId, keywords, KEYWORDS);
        ComplexTextAdGroup validAdGroup = fullAdGroupForUpdate(adGroupInfo2.getAdGroupId(), retConditionId);

        ValidationResult<?, Defect> vr = updateAndCheckResultIsFailed(validAdGroup, invalidAdGroup);
        Path errPath = path(index(1), field(KEYWORDS.name()), index(1));
        assertThat(vr,
                hasDefectDefinitionWith(validationError(errPath, maxKeywordsPerAdGroupExceeded(keywordsLimit, 1))));
        vr.flattenErrors().forEach(validationErrorLogger::info);
        assertThat("должны присутствовать ошибки для каждой добавляемой ключевой фразы",
                vr.flattenErrors(), hasSize(keywordsLimit));
    }

    @Test
    public void hasErrorsWhenTooManyRelevanceMatches() {
        createSecondAdGroup();

        ComplexTextAdGroup invalidAdGroup = emptyAdGroupWithModelForUpdate(adGroupId,
                asList(defaultRelevanceMatch(), defaultRelevanceMatch()), RELEVANCE_MATCHES);
        ComplexTextAdGroup validAdGroup = fullAdGroupForUpdate(adGroupInfo2.getAdGroupId(), retConditionId);

        ValidationResult<?, Defect> vr = updateAndCheckResultIsFailed(validAdGroup, invalidAdGroup);
        Path errPath = path(index(1), field(RELEVANCE_MATCHES.name()), index(0));
        assertThat(vr,
                hasDefectDefinitionWith(validationError(errPath, maxRelevanceMatchesInAdGroup())));
        vr.flattenErrors().forEach(validationErrorLogger::info);
        assertThat("должны присутствовать ошибки для каждого бесфразного таргетинга",
                vr.flattenErrors(), hasSize(2));
    }

    @Test
    public void hasErrorsWhenTooManyOfferRetargetings() {
        createSecondAdGroup();

        ComplexTextAdGroup invalidAdGroup = emptyAdGroupWithModelForUpdate(adGroupId,
                asList(defaultOfferRetargeting(), defaultOfferRetargeting()), OFFER_RETARGETINGS);
        ComplexTextAdGroup validAdGroup = fullAdGroupForUpdate(adGroupInfo2.getAdGroupId(), retConditionId);

        var vr = updateAndCheckResultIsFailed(validAdGroup, invalidAdGroup);
        Path errPath = path(index(1), field(OFFER_RETARGETINGS.name()), index(0));

        assertThat(vr, hasDefectDefinitionWith((validationError(errPath, tooManyOfferRetargetingsInAdGroup()))));
        vr.flattenErrors().forEach(validationErrorLogger::info);
        assertThat("должны присутствовать ошибки для каждого офферного ретаргетинга",
                vr.flattenErrors(), hasSize(2));
    }

    @Test
    public void hasErrorsWhenTooManyRetargetings() {
        createSecondAdGroup();

        RetargetingInfo retargetingInfo = steps.retargetingSteps()
                .createRetargeting(defaultRetargeting(campaignId, adGroupId, retConditionId), adGroupInfo1);

        int retargetingsLimit = Constants.MAX_RETARGETINGS_IN_ADGROUP;
        List<TargetInterest> targetInterests = new ArrayList<>();

        TargetInterest retargetingToUpdate = new TargetInterest()
                .withId(retargetingInfo.getRetargetingId())
                .withRetargetingConditionId(retConditionId);
        targetInterests.add(retargetingToUpdate);

        for (int i = 0; i < retargetingsLimit; i++) {
            RetConditionInfo retConditionInfo = steps.retConditionSteps()
                    .createBigRetCondition(campaignInfo.getClientInfo());
            targetInterests.add(randomPriceRetargeting(retConditionInfo.getRetConditionId()));
        }

        ComplexTextAdGroup invalidAdGroup = fullAdGroupForUpdate(adGroupId, retConditionId)
                .withTargetInterests(targetInterests);
        ComplexTextAdGroup validAdGroup = fullAdGroupForUpdate(adGroupInfo2.getAdGroupId(), retConditionId);

        ValidationResult<?, Defect> vr = updateAndCheckResultIsFailed(validAdGroup, invalidAdGroup);
        Path errPath = path(index(1), field(ComplexTextAdGroup.TARGET_INTERESTS.name()), index(1));
        assertThat(vr,
                hasDefectDefinitionWith(validationError(errPath, maxCollectionSizeAdGroup(retargetingsLimit))));
        vr.flattenErrors().forEach(validationErrorLogger::info);
        assertThat("должны присутствовать ошибки для каждого добавляемого ретаргетинга",
                vr.flattenErrors(), hasSize(retargetingsLimit));
    }

    private void createImageHashBanner(AdGroupInfo adGroupInfo) {
        BannerImageFormat imageFormat = steps.bannerSteps()
                .createBannerImageFormat(campaignInfo.getClientInfo(), imageAdImageFormat(null));
        Image image = defaultImage(campaignId, adGroupId).withImageHash(imageFormat.getImageHash());
        steps.bannerSteps().createBanner(activeImageHashBanner(null, null).withImage(image), adGroupInfo);
    }
}
