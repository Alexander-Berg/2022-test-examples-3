package ru.yandex.direct.api.v5.entity.keywordbids.validation;

import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.collect.ImmutableList;
import com.yandex.direct.api.v5.keywordbids.KeywordBidSetItem;
import com.yandex.direct.api.v5.keywordbids.SetRequest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.api.v5.entity.bids.validation.BidsInternalValidationService;
import ru.yandex.direct.api.v5.entity.keywordbids.delegate.SetKeywordBidsDelegate;
import ru.yandex.direct.api.v5.testing.configuration.Api5Test;
import ru.yandex.direct.api.v5.validation.DefectType;
import ru.yandex.direct.core.entity.bids.container.SetBidItem;
import ru.yandex.direct.core.entity.campaign.service.accesschecker.CampaignAccessibiltyChecker;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.KeywordInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.validation.result.ValidationResult;
import ru.yandex.qatools.allure.annotations.Description;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.mockito.MockitoAnnotations.initMocks;
import static ru.yandex.direct.api.v5.entity.bids.validation.BidsDefectTypes.requiredAnyOfSetBidFields;
import static ru.yandex.direct.api.v5.validation.Matchers.validationError;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@Api5Test
@RunWith(SpringRunner.class)
@ParametersAreNonnullByDefault
public class SetKeywordBidsRequestInternalValidationTest {

    @Autowired
    public SetKeywordBidsDelegate delegate;

    @Autowired
    private Steps steps;

    @Autowired
    private BidsInternalValidationService bidsInternalValidationService;

    @Before
    public void before() {
        initMocks(this);
    }

    @Test
    @Description("Запрос, как по идентификаторам ставки, так и группы объявлений")
    public void validate_RequestBothAdGroupIdAndBidSpecified_Error() {
        CampaignInfo campaignInfo = steps.campaignSteps().createActiveTextCampaign();
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup(campaignInfo);
        KeywordInfo keyword1 = steps.keywordSteps().createKeyword(adGroupInfo);
        List<KeywordBidSetItem> bidsList = ImmutableList.of(
                new KeywordBidSetItem().withAdGroupId(keyword1.getAdGroupId()),
                new KeywordBidSetItem().withSearchBid(1L));
        SetRequest request = new SetRequest().withKeywordBids(bidsList);
        CampaignAccessibiltyChecker accessibiltyChecker = delegate.getCampaignAccessibiltyChecker();
        ValidationResult<List<SetBidItem>, DefectType> result = bidsInternalValidationService
                .validateInternalRequest(delegate.convertRequest(delegate.validateRequest(request).getValue()),
                        campaignInfo.getUid(), campaignInfo.getClientId(), accessibiltyChecker);
        assertThat(result.hasErrors()).isFalse();
        assertThat(result.flattenErrors()).is(matchedBy(
                contains(validationError(path(index(1)), requiredAnyOfSetBidFields()))));
    }

    @Test
    @Description("Запрос, как по идентификаторам ставки, так и кампании")
    public void validate_RequestBothCampaignIdAndBidSpecified_Error() {
        CampaignInfo campaignInfo = steps.campaignSteps().createActiveTextCampaign();
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup(campaignInfo);
        KeywordInfo keyword1 = steps.keywordSteps().createKeyword(adGroupInfo);
        List<KeywordBidSetItem> bidsList = ImmutableList.of(
                new KeywordBidSetItem().withCampaignId(keyword1.getCampaignId()),
                new KeywordBidSetItem().withSearchBid(1L));
        SetRequest request = new SetRequest().withKeywordBids(bidsList);
        CampaignAccessibiltyChecker accessibiltyChecker = delegate.getCampaignAccessibiltyChecker();
        ValidationResult<List<SetBidItem>, DefectType> result = bidsInternalValidationService
                .validateInternalRequest(delegate.convertRequest(delegate.validateRequest(request).getValue()),
                        campaignInfo.getUid(), campaignInfo.getClientId(), accessibiltyChecker);
        assertThat(result.hasErrors()).isFalse();
        assertThat(result.flattenErrors()).is(matchedBy(
                contains(validationError(path(index(1)), requiredAnyOfSetBidFields()))));
    }
}
