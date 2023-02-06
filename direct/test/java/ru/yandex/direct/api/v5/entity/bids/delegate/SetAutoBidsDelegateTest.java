package ru.yandex.direct.api.v5.entity.bids.delegate;

import java.util.List;
import java.util.function.Consumer;

import com.yandex.direct.api.v5.bids.BidSetAutoItem;
import com.yandex.direct.api.v5.bids.SetAutoRequest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.api.v5.converter.ResultConverter;
import ru.yandex.direct.api.v5.entity.bids.converter.BidsHelperConverter;
import ru.yandex.direct.api.v5.result.ApiMassResult;
import ru.yandex.direct.api.v5.result.ApiResult;
import ru.yandex.direct.api.v5.result.ApiResultState;
import ru.yandex.direct.api.v5.security.ApiAuthenticationSource;
import ru.yandex.direct.api.v5.testing.configuration.Api5Test;
import ru.yandex.direct.api.v5.validation.DefectType;
import ru.yandex.direct.api.v5.validation.DefectTypes;
import ru.yandex.direct.core.entity.bids.container.SetAutoBidItem;
import ru.yandex.direct.core.entity.bids.service.BidService;
import ru.yandex.direct.validation.result.DefectInfo;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.nCopies;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.mockito.Mockito.mock;
import static ru.yandex.direct.api.v5.entity.bids.Constants.MAX_BID_ADGROUPIDS_PER_REQUEST;
import static ru.yandex.direct.api.v5.entity.bids.Constants.MAX_BID_CAMPAIGNIDS_PER_REQUEST;
import static ru.yandex.direct.api.v5.entity.bids.Constants.MAX_BID_IDS_PER_REQUEST;
import static ru.yandex.direct.api.v5.validation.Matchers.validationError;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

@Api5Test
@RunWith(SpringRunner.class)
public class SetAutoBidsDelegateTest {
    private SetAutoBidsDelegate delegate;

    @Autowired
    private ResultConverter resultConverter;

    private static SetAutoRequest createRequest(int numberOfIds, Consumer<BidSetAutoItem> setId) {
        BidSetAutoItem item = new BidSetAutoItem();
        setId.accept(item);
        return new SetAutoRequest().withBids(nCopies(numberOfIds, item));
    }

    @Before
    public void setUp() {
        delegate = new SetAutoBidsDelegate(mock(BidsHelperConverter.class),
                mock(ApiAuthenticationSource.class), mock(BidService.class),
                resultConverter);
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

    @Test
    public void filterExtraSetAutoErrors_success() {
        List<ApiResult<SetAutoBidItem>> apiResults =
                singletonList(new ApiResult<>(null,
                        asList(new DefectInfo<>(path(field("searchByPosition"), field("increasePercent")), null,
                                        DefectTypes.invalidValue()),
                                new DefectInfo<>(path(field("networkByCoverage"), field("increasePercent")), null,
                                        DefectTypes.invalidValue()),
                                new DefectInfo<>(path(field("searchByPosition"), field("maxBid")), null,
                                        DefectTypes.invalidValue()),
                                new DefectInfo<>(path(field("networkByCoverage"), field("maxBid")), null,
                                        DefectTypes.invalidValue())),
                        emptyList(), ApiResultState.BROKEN));
        ApiResult<List<ApiResult<SetAutoBidItem>>> actual = delegate.filterExtraSetAutoErrors(
                new ApiMassResult<>(apiResults, emptyList(), emptyList(), ApiResultState.SUCCESSFUL));

        assertThat(actual.getResult()).first().satisfies(
                r -> assertThat(r.getErrors()).hasSize(2)
        );
    }
}
