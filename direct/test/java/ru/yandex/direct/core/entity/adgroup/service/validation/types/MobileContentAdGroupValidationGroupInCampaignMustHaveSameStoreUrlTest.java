package ru.yandex.direct.core.entity.adgroup.service.validation.types;

import java.util.HashMap;
import java.util.List;

import org.assertj.core.api.SoftAssertions;
import org.junit.Test;

import ru.yandex.direct.core.entity.adgroup.model.MobileContentAdGroup;
import ru.yandex.direct.validation.builder.ListValidationBuilder;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.PathNode;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.not;
import static ru.yandex.direct.core.entity.adgroup.service.validation.AdGroupDefects.storeUrlMustBeTheSameForAllGroupsInMobileContentCampaign;
import static ru.yandex.direct.core.entity.adgroup.service.validation.types.MobileContentAdGroupValidation.groupsInCampaignMustHaveSameStoreUrl;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.anyValidationErrorOnPathStartsWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoErrorsAndWarnings;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

public class MobileContentAdGroupValidationGroupInCampaignMustHaveSameStoreUrlTest {
    private static final String URL1 = "https://itunes.apple.com/ru/app/angry-birds/id343200656?mt=8";
    private static final String URL2 = "https://itunes.apple.com/ru/app/angry-birds/id343200655?mt=8";
    private static final long CAMPAIGN_ID1 = 1L;
    private static final long CAMPAIGN_ID2 = 2L;
    private static final PathNode.Field STORE_URL_FIELD = field("storeUrl");
    private static final String VALID_ITUNES_STORE_URL = "https://itunes.apple.com/ru/app/angry-birds/id343200656?mt=8";
    private static final String VALID_APP_STORE_URL = "https://apps.apple.com/ru/app/angry-birds/id343200656?mt=8";
    private static final String VALID_PLAY_MARKET_STORE_URL = "https://play.google.com/store/apps/details?id=ru" +
            ".aviasales";
    private static final String VALID_PLAY_MARKET_STORE_URL_HL_EN = "https://play.google.com/store/apps/details?id=ru" +
            ".aviasales&hl=en";
    private static final String VALID_PLAY_MARKET_STORE_URL_GL_US = "https://play.google.com/store/apps/details?id=ru" +
            ".aviasales&gl=us";

    @Test
    public void validate_Empty_ReturnsSuccessfulResult() {
        ValidationResult<List<MobileContentAdGroup>, Defect> result = validateGroups(emptyList());
        assertThat(result).is(matchedBy(hasNoErrorsAndWarnings()));
    }

    @Test
    public void validate_TwoGroupsWithSameUrlInOneCampaign_ReturnsSuccessfulResult() {
        ValidationResult<List<MobileContentAdGroup>, Defect> result = validateGroups(asList(
                createMobileContentAdGroup(CAMPAIGN_ID1, URL1),
                createMobileContentAdGroup(CAMPAIGN_ID1, URL1)
        ));
        assertThat(result).is(matchedBy(hasNoErrorsAndWarnings()));
    }

    @Test
    public void validate_TwoGroupsWithDifferentUrlInOneCampaign_ReturnsErrorsForBoth() {
        ValidationResult<List<MobileContentAdGroup>, Defect> result = validateGroups(asList(
                createMobileContentAdGroup(CAMPAIGN_ID1, URL1),
                createMobileContentAdGroup(CAMPAIGN_ID1, URL2)
        ));
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(result).is(matchedBy(hasDefectWithDefinition(
                    validationError(path(index(0), STORE_URL_FIELD),
                            storeUrlMustBeTheSameForAllGroupsInMobileContentCampaign()))));
            softly.assertThat(result).is(matchedBy(hasDefectWithDefinition(
                    validationError(path(index(1), STORE_URL_FIELD),
                            storeUrlMustBeTheSameForAllGroupsInMobileContentCampaign()))));
        });
    }

    @Test
    public void validate_ThreeGroupsWithDifferentUrlInDifferentCampaigns_ReturnsErrorsForAllGroupsInCampaignWithDifferentUrl() {
        ValidationResult<List<MobileContentAdGroup>, Defect> result = validateGroups(asList(
                createMobileContentAdGroup(CAMPAIGN_ID1, URL1),
                createMobileContentAdGroup(CAMPAIGN_ID1, URL2),
                createMobileContentAdGroup(CAMPAIGN_ID2, URL2)
        ));
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(result).is(matchedBy(hasDefectWithDefinition(
                    validationError(path(index(0), STORE_URL_FIELD),
                            storeUrlMustBeTheSameForAllGroupsInMobileContentCampaign()))));
            softly.assertThat(result).is(matchedBy(hasDefectWithDefinition(
                    validationError(path(index(1), STORE_URL_FIELD),
                            storeUrlMustBeTheSameForAllGroupsInMobileContentCampaign()))));
            softly.assertThat(result).is(matchedBy(not(hasDefectWithDefinition(
                    anyValidationErrorOnPathStartsWith(path(index(2)))))));
        });
    }

    @Test
    public void validate_GroupsWithDifferentUrlInSameCampaign_ReturnsNoErrorsWhenPlayMarketUrlWithDifferentHlParams() {
        ValidationResult<List<MobileContentAdGroup>, Defect> result = validateGroups(asList(
                createMobileContentAdGroup(CAMPAIGN_ID1, VALID_PLAY_MARKET_STORE_URL),
                createMobileContentAdGroup(CAMPAIGN_ID1, VALID_PLAY_MARKET_STORE_URL_HL_EN)
        ));
        assertThat(result).is(matchedBy(hasNoErrorsAndWarnings()));
    }

    @Test
    public void validate_GroupsWithDifferentUrlInSameCampaign_ReturnsErrorsWhenPlayMarketUrlWithDifferentGlParams() {
        ValidationResult<List<MobileContentAdGroup>, Defect> result = validateGroups(asList(
                createMobileContentAdGroup(CAMPAIGN_ID1, VALID_PLAY_MARKET_STORE_URL),
                createMobileContentAdGroup(CAMPAIGN_ID1, VALID_PLAY_MARKET_STORE_URL_GL_US)
                ));
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(result).is(matchedBy(hasDefectWithDefinition(
                    validationError(path(index(0), STORE_URL_FIELD),
                            storeUrlMustBeTheSameForAllGroupsInMobileContentCampaign()))));
            softly.assertThat(result).is(matchedBy(hasDefectWithDefinition(
                    validationError(path(index(1), STORE_URL_FIELD),
                            storeUrlMustBeTheSameForAllGroupsInMobileContentCampaign()))));
        });
    }

    @Test
    public void validate_GroupsWithDifferentUrlInSameCampaign_ReturnsNoErrorsWhenAppStoreUrlWithDifferentDomains() {
        ValidationResult<List<MobileContentAdGroup>, Defect> result = validateGroups(asList(
                createMobileContentAdGroup(CAMPAIGN_ID1, VALID_APP_STORE_URL),
                createMobileContentAdGroup(CAMPAIGN_ID1, VALID_ITUNES_STORE_URL)
        ));
        assertThat(result).is(matchedBy(hasNoErrorsAndWarnings()));
    }

    private ValidationResult<List<MobileContentAdGroup>, Defect> validateGroups(List<MobileContentAdGroup> adGroups) {
        ListValidationBuilder<MobileContentAdGroup, Defect> vb = ListValidationBuilder.of(adGroups);
        vb.checkEachBy(groupsInCampaignMustHaveSameStoreUrl(adGroups, new HashMap<>()));
        return vb.getResult();
    }

    private static MobileContentAdGroup createMobileContentAdGroup(long campaignId, String url) {
        return new MobileContentAdGroup().withCampaignId(campaignId).withStoreUrl(url);
    }

}
