package ru.yandex.direct.api.v5.entity.keywordbids.validation;

import java.util.Collections;
import java.util.List;

import com.yandex.direct.api.v5.keywordbids.KeywordBidSetItem;
import com.yandex.direct.api.v5.keywordbids.SetRequest;
import one.util.streamex.LongStreamEx;
import org.junit.Test;

import ru.yandex.direct.api.v5.validation.DefectType;
import ru.yandex.direct.validation.result.ValidationResult;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.api.v5.entity.bids.Constants.MAX_BID_ADGROUPIDS_PER_REQUEST;
import static ru.yandex.direct.api.v5.entity.bids.Constants.MAX_BID_CAMPAIGNIDS_PER_REQUEST;
import static ru.yandex.direct.api.v5.entity.bids.Constants.MAX_BID_IDS_PER_REQUEST;
import static ru.yandex.direct.api.v5.entity.bids.validation.BidsDefectTypes.maxAdGroupsBidsPerRequest;
import static ru.yandex.direct.api.v5.entity.bids.validation.BidsDefectTypes.maxCampBidsPerRequest;
import static ru.yandex.direct.api.v5.entity.bids.validation.BidsDefectTypes.maxKeywordBidsPerRequest;
import static ru.yandex.direct.api.v5.validation.DefectTypes.absentElementInArray;
import static ru.yandex.direct.api.v5.validation.DefectTypes.mixedTypes;
import static ru.yandex.direct.api.v5.validation.Matchers.defectTypeWith;
import static ru.yandex.direct.api.v5.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

public class SetKeywordBidsRequestValidationServiceTest {

    private SetKeywordBidsRequestValidationService service = new SetKeywordBidsRequestValidationService();

    @Test
    public void tooManyKeywordIds() {
        SetRequest setRequest = new SetRequest();

        List<KeywordBidSetItem> keywordBidSetItems = LongStreamEx.range(1, MAX_BID_IDS_PER_REQUEST + 2)
                .mapToObj(t -> new KeywordBidSetItem().withKeywordId(t))
                .toList();
        setRequest.setKeywordBids(keywordBidSetItems);

        ValidationResult<SetRequest, DefectType> validationResult = service.validate(setRequest);

        assertThat(validationResult)
                .has(defectTypeWith(validationError(path(field("KeywordBids")), maxKeywordBidsPerRequest())));
    }

    @Test
    public void tooManyAdGroupIds() {
        SetRequest setRequest = new SetRequest();

        List<KeywordBidSetItem> keywordBidSetItems = LongStreamEx.range(1, MAX_BID_ADGROUPIDS_PER_REQUEST + 2)
                .mapToObj(t -> new KeywordBidSetItem().withAdGroupId(t))
                .toList();
        setRequest.setKeywordBids(keywordBidSetItems);

        ValidationResult<SetRequest, DefectType> validationResult = service.validate(setRequest);

        assertThat(validationResult)
                .has(defectTypeWith(validationError(path(field("KeywordBids")), maxAdGroupsBidsPerRequest())));
    }

    @Test
    public void tooManyAdCampaignIds() {
        SetRequest setRequest = new SetRequest();

        List<KeywordBidSetItem> keywordBidSetItems = LongStreamEx.range(1, MAX_BID_CAMPAIGNIDS_PER_REQUEST + 2)
                .mapToObj(t -> new KeywordBidSetItem().withCampaignId(t))
                .toList();
        setRequest.setKeywordBids(keywordBidSetItems);

        ValidationResult<SetRequest, DefectType> validationResult = service.validate(setRequest);

        assertThat(validationResult)
                .has(defectTypeWith(validationError(path(field("KeywordBids")), maxCampBidsPerRequest())));
    }

    @Test
    public void nullKeywordBid() {
        SetRequest setRequest = new SetRequest();

        List<KeywordBidSetItem> keywordBidSetItems = Collections.singletonList(null);
        setRequest.setKeywordBids(keywordBidSetItems);

        ValidationResult<SetRequest, DefectType> validationResult = service.validate(setRequest);

        assertThat(validationResult)
                .has(defectTypeWith(validationError(path(field("KeywordBids")), absentElementInArray())));
    }

    @Test
    public void mixedTypesAdGroupCampaign() {
        SetRequest setRequest = new SetRequest();
        setRequest
                .withKeywordBids(new KeywordBidSetItem().withAdGroupId(1L), new KeywordBidSetItem().withCampaignId(1L));
        ValidationResult<SetRequest, DefectType> validationResult = service.validate(setRequest);

        assertThat(validationResult).has(defectTypeWith(validationError(path(field("KeywordBids")), mixedTypes())));
    }
}
