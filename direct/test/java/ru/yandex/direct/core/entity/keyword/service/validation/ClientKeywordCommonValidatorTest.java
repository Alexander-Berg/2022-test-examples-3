package ru.yandex.direct.core.entity.keyword.service.validation;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.adgroup.model.AdGroupType;
import ru.yandex.direct.core.entity.adgroup.model.CriterionType;
import ru.yandex.direct.core.entity.campaign.model.Campaign;
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.entity.keyword.container.InternalKeyword;
import ru.yandex.direct.core.entity.keyword.model.Keyword;
import ru.yandex.direct.core.entity.keyword.service.internal.InternalKeywordFactory;
import ru.yandex.direct.core.entity.stopword.service.StopWordService;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.KeywordInfo;
import ru.yandex.direct.core.testing.info.TextBannerInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.libs.keywordutils.inclusion.model.KeywordWithLemmasFactory;
import ru.yandex.direct.validation.builder.Validator;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Collections.singleton;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.keyword.service.validation.KeywordDefects.adGroupNotFound;
import static ru.yandex.direct.core.entity.keyword.service.validation.KeywordDefects.notAcceptableAdGroupType;
import static ru.yandex.direct.core.testing.data.TestBanners.activeTextBanner;
import static ru.yandex.direct.core.testing.data.TestCampaigns.manualStrategy;
import static ru.yandex.direct.core.testing.data.TestCampaigns.newTextCampaign;
import static ru.yandex.direct.core.validation.defects.RightsDefects.noRights;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.matchesWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.CommonDefects.objectNotFound;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class ClientKeywordCommonValidatorTest {

    @Autowired
    private Steps steps;

    @Autowired
    private CampaignRepository campaignRepository;

    @Autowired
    private StopWordService stopWordService;

    @Autowired
    private KeywordWithLemmasFactory keywordWithLemmasFactory;

    @Autowired
    private InternalKeywordFactory internalKeywordFactory;

    private Map<Long, Long> adGroupIdToCampaignId = new HashMap<>();
    private Map<Long, Campaign> writableCampaignsById = new HashMap<>();
    private Map<Long, AdGroupType> adGroupTypesById = new HashMap<>();
    private Map<Long, CriterionType> cpmAdGroupsWithCriterionType = new HashMap<>();

    private Keyword keyword;

    @Before
    public void before() {
        CampaignInfo campaignInfo = steps.campaignSteps()
                .createCampaign(newTextCampaign(null, null).withStrategy(manualStrategy().withSeparateBids(false)));
        TextBannerInfo bannerInfo = steps.bannerSteps().createBanner(activeTextBanner(), campaignInfo);
        KeywordInfo keywordInfo = steps.keywordSteps().createKeyword(bannerInfo.getAdGroupInfo());

        adGroupIdToCampaignId.put(bannerInfo.getAdGroupId(), bannerInfo.getCampaignId());
        Campaign campaign = campaignRepository.getCampaigns(
                bannerInfo.getShard(), singleton(bannerInfo.getCampaignId())).get(0);
        writableCampaignsById.put(bannerInfo.getCampaignId(), campaign);
        adGroupTypesById.put(bannerInfo.getAdGroupId(), AdGroupType.BASE);

        keyword = keywordInfo.getKeyword().withPrice(new BigDecimal(10)).withPriceContext(new BigDecimal(10));
        cpmAdGroupsWithCriterionType = new HashMap<>();
    }

    //default
    @Test
    public void build_OneValidKeyword_NoErrors() {
        ValidationResult<Keyword, Defect> vr = buildAndApply(keyword);
        assertThat(vr, hasNoDefectsDefinitions());
    }

    //adGroup
    @Test
    public void build_KeywordWithoutAdGroup_NotFoundDefectDefinition() {
        keyword.withAdGroupId(null);
        ValidationResult<Keyword, Defect> vr = buildAndApply(keyword);
        assertSingleKeywordError(vr, "adGroupId", objectNotFound());
    }

    @Test
    public void build_KeywordWithDynamicAdGroupType_NotAcceptableAdGroupTypeDefinition() {
        adGroupTypesById.put(keyword.getAdGroupId(), AdGroupType.DYNAMIC);
        ValidationResult<Keyword, Defect> vr = buildAndApply(keyword);
        assertSingleKeywordError(vr, "adGroupId", notAcceptableAdGroupType());
    }

    @Test
    public void build_KeywordWithCpmYndxFrontpageAdGroupType_NotAcceptableAdGroupTypeDefinition() {
        adGroupTypesById.put(keyword.getAdGroupId(), AdGroupType.CPM_YNDX_FRONTPAGE);
        ValidationResult<Keyword, Defect> vr = buildAndApply(keyword);
        assertSingleKeywordError(vr, "adGroupId", notAcceptableAdGroupType());
    }

    @Test
    public void build_KeywordWithCpmAdGroupWithUserProfileType_NotAcceptableAdGroupTypeDefinition() {
        adGroupTypesById.put(keyword.getAdGroupId(), AdGroupType.CPM_BANNER);
        cpmAdGroupsWithCriterionType.put(keyword.getAdGroupId(), CriterionType.USER_PROFILE);
        ValidationResult<Keyword, Defect> vr = buildAndApply(keyword);
        assertSingleKeywordError(vr, "adGroupId", notAcceptableAdGroupType());
    }

    @Test
    public void build_KeywordWithCpmAdGroupWithKeywordsType_NoErrors() {
        adGroupTypesById.put(keyword.getAdGroupId(), AdGroupType.CPM_BANNER);
        cpmAdGroupsWithCriterionType.put(keyword.getAdGroupId(), CriterionType.KEYWORD);
        ValidationResult<Keyword, Defect> vr = buildAndApply(keyword);
        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void build_KeywordWithCpmGeoproductAdGroup_NotAcceptableAdGroupTypeDefinition() {
        adGroupTypesById.put(keyword.getAdGroupId(), AdGroupType.CPM_GEOPRODUCT);
        ValidationResult<Keyword, Defect> vr = buildAndApply(keyword);
        assertSingleKeywordError(vr, "adGroupId", notAcceptableAdGroupType());
    }

    @Test
    public void build_KeywordWithContentPromotionVideoAdGroup_NoErrors() {
        adGroupTypesById.put(keyword.getAdGroupId(), AdGroupType.CONTENT_PROMOTION_VIDEO);
        ValidationResult<Keyword, Defect> vr = buildAndApply(keyword);
        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void build_KeywordWithContentPromotionAdGroup_NoErrors() {
        adGroupTypesById.put(keyword.getAdGroupId(), AdGroupType.CONTENT_PROMOTION);
        ValidationResult<Keyword, Defect> vr = buildAndApply(keyword);
        assertThat(vr, hasNoDefectsDefinitions());
    }

    //campaign
    @Test
    public void build_CampaignExists_NotFoundDefectDefinition() {
        keyword.withAdGroupId(123L);
        ValidationResult<Keyword, Defect> vr = buildAndApply(keyword);
        assertThat(vr, hasDefectDefinitionWith(matchesWith(adGroupNotFound())));
    }

    @Test
    public void build_AccessCheckerReturnsError_ResultHasError() {
        ValidationResult<Keyword, Defect> vr =
                buildAndApply(keyword, adGroupId -> ValidationResult.failed(adGroupId, noRights()));
        assertThat(vr, hasDefectDefinitionWith(matchesWith(noRights())));
    }

    private ValidationResult<Keyword, Defect> buildAndApply(Keyword keyword) {
        return buildAndApply(keyword, ValidationResult::success);
    }

    private ValidationResult<Keyword, Defect> buildAndApply(
            Keyword keyword, Validator<Long, Defect> accessChecker) {
        InternalKeyword internalKeyword = internalKeywordFactory.createInternalKeyword(keyword);
        return ClientKeywordCommonValidator
                .build(stopWordService, keywordWithLemmasFactory, internalKeyword,
                        adGroupIdToCampaignId, writableCampaignsById, adGroupTypesById,
                        cpmAdGroupsWithCriterionType, accessChecker, true, false)
                .apply(keyword);
    }

    private void assertSingleKeywordError(ValidationResult<Keyword, Defect> validationResult,
                                          String field, Defect expectedDefect) {
        assertThat(validationResult, hasDefectDefinitionWith(validationError(path(field(field)), expectedDefect)));
    }
}
