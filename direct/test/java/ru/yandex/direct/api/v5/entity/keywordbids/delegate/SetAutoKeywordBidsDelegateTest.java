package ru.yandex.direct.api.v5.entity.keywordbids.delegate;

import java.util.function.Consumer;

import com.yandex.direct.api.v5.keywordbids.KeywordBidSetAutoItem;
import com.yandex.direct.api.v5.keywordbids.SetAutoRequest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.api.v5.converter.ResultConverter;
import ru.yandex.direct.api.v5.entity.keywordbids.converter.SetAutoKeywordBidsConverter;
import ru.yandex.direct.api.v5.security.ApiAuthenticationSource;
import ru.yandex.direct.api.v5.testing.configuration.Api5Test;
import ru.yandex.direct.api.v5.validation.DefectType;
import ru.yandex.direct.core.entity.bids.service.BidService;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Collections.nCopies;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.mockito.Mockito.mock;
import static ru.yandex.direct.api.v5.entity.bids.Constants.MAX_BID_ADGROUPIDS_PER_REQUEST;
import static ru.yandex.direct.api.v5.entity.bids.Constants.MAX_BID_CAMPAIGNIDS_PER_REQUEST;
import static ru.yandex.direct.api.v5.entity.bids.Constants.MAX_BID_IDS_PER_REQUEST;
import static ru.yandex.direct.api.v5.validation.Matchers.validationError;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

@Api5Test
@RunWith(SpringRunner.class)
public class SetAutoKeywordBidsDelegateTest {
    private SetAutoKeywordBidsDelegate delegate;

    @Autowired
    private ResultConverter resultConverter;

    private static SetAutoRequest createRequest(int numberOfIds, Consumer<KeywordBidSetAutoItem> setId) {
        KeywordBidSetAutoItem item = new KeywordBidSetAutoItem();
        setId.accept(item);
        return new SetAutoRequest().withKeywordBids(nCopies(numberOfIds, item));
    }

    @Before
    public void setUp() {
        delegate = new SetAutoKeywordBidsDelegate(
                mock(ApiAuthenticationSource.class),
                mock(BidService.class),
                new SetAutoKeywordBidsConverter(resultConverter), resultConverter);
    }

    @Test
    public void validate_idsAtTheLimit_noError() {
        SetAutoRequest request = createRequest(MAX_BID_IDS_PER_REQUEST, item -> item.setKeywordId(1L));

        ValidationResult<SetAutoRequest, DefectType> result = delegate.validateRequest(request);

        assertThat(result.hasAnyErrors()).isFalse();
    }

    @Test
    public void validate_idsOverlowTheLimit_errorIsGenerated() {
        SetAutoRequest request = createRequest(MAX_BID_IDS_PER_REQUEST + 1, item -> item.setKeywordId(1L));

        ValidationResult<SetAutoRequest, DefectType> result = delegate.validateRequest(request);

        assertThat(result.flattenErrors()).is(matchedBy(contains(validationError(9300))));
    }

    @Test
    public void validate_adGroupIdsAtTheLimit_noError() {
        SetAutoRequest request = createRequest(MAX_BID_ADGROUPIDS_PER_REQUEST, item -> item.setAdGroupId(1L));

        ValidationResult<SetAutoRequest, DefectType> result = delegate.validateRequest(request);

        assertThat(result.hasAnyErrors()).isFalse();
    }

    @Test
    public void validate_adGroupIdsOverlowTheLimit_errorIsGenerated() {
        SetAutoRequest request =
                createRequest(MAX_BID_ADGROUPIDS_PER_REQUEST + 1, item -> item.setAdGroupId(1L));
        ValidationResult<SetAutoRequest, DefectType> result = delegate.validateRequest(request);

        assertThat(result.flattenErrors()).is(matchedBy(contains(validationError(9300))));
    }

    @Test
    public void validate_campaignIdsAtTheLimit_noError() {
        SetAutoRequest request = createRequest(MAX_BID_CAMPAIGNIDS_PER_REQUEST, item -> item.setCampaignId(1L));

        ValidationResult<SetAutoRequest, DefectType> result = delegate.validateRequest(request);

        assertThat(result.hasAnyErrors()).isFalse();
    }

    @Test
    public void validate_campaignIdsOverlowTheLimit_errorIsGenerated() {
        SetAutoRequest request =
                createRequest(MAX_BID_CAMPAIGNIDS_PER_REQUEST + 1, item -> item.setCampaignId(1L));
        ValidationResult<SetAutoRequest, DefectType> result = delegate.validateRequest(request);

        assertThat(result.flattenErrors()).is(matchedBy(contains(validationError(9300))));
    }
}
