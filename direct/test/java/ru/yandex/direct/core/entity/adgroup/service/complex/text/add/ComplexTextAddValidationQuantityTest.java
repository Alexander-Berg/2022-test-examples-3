package ru.yandex.direct.core.entity.adgroup.service.complex.text.add;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.adgroup.container.ComplexTextAdGroup;
import ru.yandex.direct.core.entity.banner.container.ComplexBanner;
import ru.yandex.direct.core.entity.client.service.ClientLimitsService;
import ru.yandex.direct.core.entity.keyword.model.Keyword;
import ru.yandex.direct.core.entity.retargeting.Constants;
import ru.yandex.direct.core.entity.retargeting.model.TargetInterest;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.RetConditionInfo;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.Path;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.adgroup.container.ComplexTextAdGroup.COMPLEX_BANNERS;
import static ru.yandex.direct.core.entity.adgroup.container.ComplexTextAdGroup.KEYWORDS;
import static ru.yandex.direct.core.entity.adgroup.container.ComplexTextAdGroup.OFFER_RETARGETINGS;
import static ru.yandex.direct.core.entity.adgroup.container.ComplexTextAdGroup.RELEVANCE_MATCHES;
import static ru.yandex.direct.core.entity.adgroup.service.complex.ComplexTextAdGroupTestData.defaultOfferRetargeting;
import static ru.yandex.direct.core.entity.adgroup.service.complex.ComplexTextAdGroupTestData.defaultRelevanceMatch;
import static ru.yandex.direct.core.entity.adgroup.service.complex.ComplexTextAdGroupTestData.fullTextBannerForAdd;
import static ru.yandex.direct.core.entity.adgroup.service.complex.ComplexTextAdGroupTestData.randomPriceRetargeting;
import static ru.yandex.direct.core.entity.banner.service.validation.BannerConstants.MAX_BANNERS_IN_ADGROUP;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.maxBannersInAdGroup;
import static ru.yandex.direct.core.entity.keyword.service.validation.KeywordDefects.maxKeywordsPerAdGroupExceeded;
import static ru.yandex.direct.core.entity.offerretargeting.validation.OfferRetargetingDefects.tooManyOfferRetargetingsInAdGroup;
import static ru.yandex.direct.core.entity.relevancematch.valdiation.RelevanceMatchDefects.maxRelevanceMatchesInAdGroup;
import static ru.yandex.direct.core.entity.retargeting.service.validation2.RetargetingDefects.maxCollectionSizeAdGroup;
import static ru.yandex.direct.core.testing.data.TestKeywords.defaultClientKeyword;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringRunner.class)
public class ComplexTextAddValidationQuantityTest extends ComplexTextAddValidationTestBase {

    @Autowired
    private ClientLimitsService clientLimitsService;

    @Test
    public void hasErrorWhenTooManyBanners() {
        List<ComplexBanner> banners = new ArrayList<>();
        for (int i = 0; i <= MAX_BANNERS_IN_ADGROUP; i++) {
            banners.add(fullTextBannerForAdd());
        }
        ComplexTextAdGroup invalidAdGroup = fullAdGroup().withComplexBanners(banners);
        ComplexTextAdGroup validAdGroup = fullAdGroup();

        ValidationResult<?, Defect> vr = prepareAndCheckResultIsFailed(validAdGroup, invalidAdGroup);
        Path errPath = path(index(1), field(COMPLEX_BANNERS.name()));
        assertThat(vr, hasDefectDefinitionWith(validationError(errPath, maxBannersInAdGroup(MAX_BANNERS_IN_ADGROUP))));
        vr.flattenErrors().forEach(validationErrorLogger::info);
        assertThat("должна присутствовать всего одна ошибка", vr.flattenErrors(), hasSize(1));
    }

    @Test
    public void hasErrorsWhenTooManyKeywords() {
        int keywordsLimit = (int) (long) clientLimitsService
                .massGetClientLimits(singletonList(campaign.getClientId()))
                .iterator().next().getKeywordsCountLimitOrDefault();
        List<Keyword> keywords = new ArrayList<>();
        for (int i = 0; i <= keywordsLimit; i++) {
            keywords.add(defaultClientKeyword().withPhrase(randomAlphanumeric(10)));
        }
        ComplexTextAdGroup invalidAdGroup = fullAdGroup().withKeywords(keywords);
        ComplexTextAdGroup validAdGroup = fullAdGroup();

        ValidationResult<?, Defect> vr = prepareAndCheckResultIsFailed(validAdGroup, invalidAdGroup);
        Path errPath = path(index(1), field(KEYWORDS.name()), index(0));
        assertThat(vr,
                hasDefectDefinitionWith(validationError(errPath, maxKeywordsPerAdGroupExceeded(keywordsLimit, 1))));
        vr.flattenErrors().forEach(validationErrorLogger::info);
        assertThat("должны присутствовать ошибки для каждой ключевой фразы",
                vr.flattenErrors(), hasSize(keywordsLimit + 1));
    }

    @Test
    public void hasErrorsWhenTooManyRelevanceMatches() {
        ComplexTextAdGroup invalidAdGroup = fullAdGroup()
                .withRelevanceMatches(asList(defaultRelevanceMatch(), defaultRelevanceMatch()));
        ComplexTextAdGroup validAdGroup = fullAdGroup();

        ValidationResult<?, Defect> vr = prepareAndCheckResultIsFailed(validAdGroup, invalidAdGroup);
        Path errPath = path(index(1), field(RELEVANCE_MATCHES.name()), index(0));
        assertThat(vr,
                hasDefectDefinitionWith(validationError(errPath, maxRelevanceMatchesInAdGroup())));
        vr.flattenErrors().forEach(validationErrorLogger::info);
        assertThat("должны присутствовать ошибки для каждого бесфразного таргетинга",
                vr.flattenErrors(), hasSize(2));
    }

    @Test
    public void hasErrorsWhenTooManyOfferRetargetings() {
        ComplexTextAdGroup invalidAdGroup = fullAdGroup()
                .withOfferRetargetings(asList(defaultOfferRetargeting(), defaultOfferRetargeting()));
        ComplexTextAdGroup validAdGroup = fullAdGroup();
        var vr = prepareAndCheckResultIsFailed(validAdGroup, invalidAdGroup);
        Path errPath = path(index(1), field(OFFER_RETARGETINGS.name()), index(0));
        assertThat(vr,
                hasDefectDefinitionWith(validationError(errPath, tooManyOfferRetargetingsInAdGroup())));
        vr.flattenErrors().forEach(validationErrorLogger::info);
        assertThat("должны присутствовать ошибки для каждого офферного ретаргетинга",
                vr.flattenErrors(), hasSize(2));
    }

    @Test
    public void hasErrorsWhenTooManyRetargetings() {
        int retargetingsLimit = Constants.MAX_RETARGETINGS_IN_ADGROUP;
        List<TargetInterest> targetInterests = new ArrayList<>();
        for (int i = 0; i <= retargetingsLimit; i++) {
            RetConditionInfo retConditionInfo = steps.retConditionSteps()
                    .createBigRetCondition(campaign.getClientInfo());
            targetInterests.add(randomPriceRetargeting(retConditionInfo.getRetConditionId()));
        }
        ComplexTextAdGroup invalidAdGroup = fullAdGroup()
                .withTargetInterests(targetInterests);
        ComplexTextAdGroup validAdGroup = fullAdGroup();

        ValidationResult<?, Defect> vr = prepareAndCheckResultIsFailed(validAdGroup, invalidAdGroup);
        Path errPath = path(index(1), field(ComplexTextAdGroup.TARGET_INTERESTS.name()), index(0));
        assertThat(vr,
                hasDefectDefinitionWith(validationError(errPath, maxCollectionSizeAdGroup(retargetingsLimit))));
        vr.flattenErrors().forEach(validationErrorLogger::info);
        assertThat("должны присутствовать ошибки для каждого ретаргетинга",
                vr.flattenErrors(), hasSize(retargetingsLimit + 1));
    }
}
