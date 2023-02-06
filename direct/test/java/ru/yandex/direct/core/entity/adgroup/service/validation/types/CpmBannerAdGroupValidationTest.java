package ru.yandex.direct.core.entity.adgroup.service.validation.types;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.adgroup.model.CpmBannerAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.CriterionType;
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.PathNode;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.entity.adgroup.service.validation.AdGroupDefects.minusKeywordsNotAllowed;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class CpmBannerAdGroupValidationTest {

    private CpmBannerAdGroup cpmBannerAdGroup;
    private CpmBannerAdGroupValidation validation;
    private ClientInfo clientInfo;

    @Autowired
    private ShardHelper shardHelper;
    @Autowired
    private CampaignRepository campaignRepository;
    @Autowired
    private AdGroupPriceSalesValidatorFactory priceSalesValidatorFactory;
    @Autowired
    private Steps steps;

    @Before
    public void setUp() {
        cpmBannerAdGroup = new CpmBannerAdGroup();
        clientInfo = steps.clientSteps().createDefaultClient();
        validation = new CpmBannerAdGroupValidation(shardHelper, campaignRepository, priceSalesValidatorFactory);
    }

    @Test
    public void validateAdGroup_noMinusKeywordsWithKeywordsCriterionType_noErrors() {
        ValidationResult<List<CpmBannerAdGroup>, Defect> result =
                validation.validateAdGroups(clientInfo.getClientId(), singletonList(cpmBannerAdGroup
                        .withCriterionType(CriterionType.KEYWORD)));

        assertThat(result).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void validateAdGroup_noMinusKeywordsWithUserProfileCriterionType_noError() {
        ValidationResult<List<CpmBannerAdGroup>, Defect> result =
                validation.validateAdGroups(clientInfo.getClientId(),
                        singletonList(cpmBannerAdGroup
                                .withCriterionType(CriterionType.USER_PROFILE)));

        assertThat(result).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void validateAdGroup_MinusKeywordsWithKeywordsCriterionType_noErrors() {
        ValidationResult<List<CpmBannerAdGroup>, Defect> result =
                validation.validateAdGroups(clientInfo.getClientId(),
                        singletonList(cpmBannerAdGroup
                                .withCriterionType(CriterionType.KEYWORD)
                                .withMinusKeywords(singletonList("blalala"))));

        assertThat(result).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void validateAdGroup_MinusKeywordsWithUserProfileCriterionType_validationError() {
        ValidationResult<List<CpmBannerAdGroup>, Defect> result =
                validation.validateAdGroups(clientInfo.getClientId(),
                        singletonList(cpmBannerAdGroup
                                .withCriterionType(CriterionType.USER_PROFILE)
                                .withMinusKeywords(singletonList("blalala"))));

        assertThat(result).is(matchedBy(hasDefectWithDefinition(
                validationError(path(new PathNode.Index(0), field(CpmBannerAdGroup.MINUS_KEYWORDS.name())),
                        minusKeywordsNotAllowed()))));
    }

    @Test
    public void validateAdGroup_LibraryMinusKeywordsWithKeywordsCriterionType_noErrors() {
        ValidationResult<List<CpmBannerAdGroup>, Defect> result =
                validation.validateAdGroups(clientInfo.getClientId(),
                        singletonList(cpmBannerAdGroup
                                .withCriterionType(CriterionType.KEYWORD)
                                .withLibraryMinusKeywordsIds(singletonList(1L))));

        assertThat(result).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void validateAdGroup_LibraryMinusKeywordsWithUserProfileCriterionType_validationError() {
        ValidationResult<List<CpmBannerAdGroup>, Defect> result =
                validation.validateAdGroups(clientInfo.getClientId(),
                        singletonList(cpmBannerAdGroup
                                .withCriterionType(CriterionType.USER_PROFILE)
                                .withLibraryMinusKeywordsIds(singletonList(1L))));

        assertThat(result).is(matchedBy(hasDefectWithDefinition(
                validationError(path(new PathNode.Index(0), field(CpmBannerAdGroup.LIBRARY_MINUS_KEYWORDS_IDS)),
                        minusKeywordsNotAllowed()))));
    }
}
