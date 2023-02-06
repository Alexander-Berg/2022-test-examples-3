package ru.yandex.direct.api.v5.entity.bids.validation;

import java.util.function.Consumer;

import com.yandex.direct.api.v5.bids.BidSetItem;
import com.yandex.direct.api.v5.bids.SetRequest;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.direct.api.v5.entity.bids.service.validation.SetBidsRequestValidationService;
import ru.yandex.direct.api.v5.validation.DefectType;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Collections.nCopies;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static ru.yandex.direct.api.v5.entity.bids.Constants.MAX_BID_ADGROUPIDS_PER_REQUEST;
import static ru.yandex.direct.api.v5.entity.bids.Constants.MAX_BID_CAMPAIGNIDS_PER_REQUEST;
import static ru.yandex.direct.api.v5.entity.bids.Constants.MAX_BID_IDS_PER_REQUEST;
import static ru.yandex.direct.api.v5.entity.bids.validation.BidsDefectTypes.maxAdGroupsBidsPerRequest;
import static ru.yandex.direct.api.v5.entity.bids.validation.BidsDefectTypes.maxCampBidsPerRequest;
import static ru.yandex.direct.api.v5.entity.bids.validation.BidsDefectTypes.maxKeywordBidsPerRequest;
import static ru.yandex.direct.api.v5.validation.Matchers.validationError;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

public class SetBidsRequestValidationServiceTest {
    private SetBidsRequestValidationService service;

    @Before
    public void setUp() {
        service = new SetBidsRequestValidationService();
    }

    @Test
    public void validate_idsAtTheLimit_noError() {
        SetRequest request = createRequest(MAX_BID_IDS_PER_REQUEST, item -> item.setKeywordId(1L));

        ValidationResult<SetRequest, DefectType> result = service.validate(request);

        assertThat(result.hasAnyErrors()).isFalse();
    }

    @Test
    public void validate_idsOverlowTheLimit_errorIsGenerated() {
        SetRequest request = createRequest(MAX_BID_IDS_PER_REQUEST + 1, item -> item.setKeywordId(1L));

        ValidationResult<SetRequest, DefectType> result = service.validate(request);

        assertThat(result.flattenErrors()).is(matchedBy(
                contains(validationError(path(field("Bids")), maxKeywordBidsPerRequest()))));
    }

    @Test
    public void validate_adGroupIdsAtTheLimit_noError() {
        SetRequest request = createRequest(MAX_BID_ADGROUPIDS_PER_REQUEST, item -> item.setAdGroupId(1L));

        ValidationResult<SetRequest, DefectType> result = service.validate(request);

        assertThat(result.hasAnyErrors()).isFalse();
    }

    @Test
    public void validate_adGroupIdsOverlowTheLimit_errorIsGenerated() {
        SetRequest request =
                createRequest(MAX_BID_ADGROUPIDS_PER_REQUEST + 1, item -> item.setAdGroupId(1L));
        ValidationResult<SetRequest, DefectType> result = service.validate(request);

        assertThat(result.flattenErrors()).is(matchedBy(
                contains(validationError(path(field("Bids")), maxAdGroupsBidsPerRequest()))));
    }

    @Test
    public void validate_campaignIdsAtTheLimit_noError() {
        SetRequest request = createRequest(MAX_BID_CAMPAIGNIDS_PER_REQUEST, item -> item.setCampaignId(1L));

        ValidationResult<SetRequest, DefectType> result = service.validate(request);

        assertThat(result.hasAnyErrors()).isFalse();
    }

    @Test
    public void validate_campaignIdsOverlowTheLimit_errorIsGenerated() {
        SetRequest request =
                createRequest(MAX_BID_CAMPAIGNIDS_PER_REQUEST + 1, item -> item.setCampaignId(1L));
        ValidationResult<SetRequest, DefectType> result = service.validate(request);

        assertThat(result.flattenErrors()).is(matchedBy(
                contains(validationError(path(field("Bids")), maxCampBidsPerRequest()))));
    }

    private static SetRequest createRequest(int numberOfIds, Consumer<BidSetItem> setId) {
        BidSetItem item = new BidSetItem();
        setId.accept(item);
        return new SetRequest().withBids(nCopies(numberOfIds, item));
    }

}
