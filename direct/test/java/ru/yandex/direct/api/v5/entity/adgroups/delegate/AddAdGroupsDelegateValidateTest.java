package ru.yandex.direct.api.v5.entity.adgroups.delegate;

import java.util.List;
import java.util.function.Consumer;

import javax.annotation.ParametersAreNonnullByDefault;

import com.yandex.direct.api.v5.adgroups.AdGroupAddItem;
import com.yandex.direct.api.v5.adgroups.AddRequest;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.api.v5.entity.adgroups.container.AdGroupsContainer;
import ru.yandex.direct.api.v5.entity.adgroups.container.AdGroupsValidationSignalContainer;
import ru.yandex.direct.api.v5.entity.adgroups.converter.AdGroupsAddRequestConverter;
import ru.yandex.direct.api.v5.security.ApiAuthenticationSource;
import ru.yandex.direct.api.v5.testing.configuration.Api5Test;
import ru.yandex.direct.api.v5.validation.DefectType;
import ru.yandex.direct.api.v5.validation.DefectTypes;
import ru.yandex.direct.core.entity.user.model.ApiUser;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Collections.nCopies;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.api.v5.entity.adgroups.Constants.MAX_ELEMENTS_PER_ADD;
import static ru.yandex.direct.api.v5.validation.Matchers.hasDefectWith;
import static ru.yandex.direct.api.v5.validation.Matchers.hasNoDefects;
import static ru.yandex.direct.api.v5.validation.Matchers.validationError;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@Api5Test
@RunWith(SpringJUnit4ClassRunner.class)
@ParametersAreNonnullByDefault
public class AddAdGroupsDelegateValidateTest {
    private static final ClientId CLIENT_ID = ClientId.fromLong(141414);
    private static final long CAMPAIGN_ID = 131313;

    @Autowired
    private AdGroupsAddRequestConverter requestConverter;

    @Autowired
    private ApiAuthenticationSource auth;

    @Autowired
    private AddAdGroupsDelegate delegate;

    private static AddRequest createRequest(int numberOfIds, Consumer<AdGroupAddItem> setId) {
        AdGroupAddItem item = new AdGroupAddItem();
        setId.accept(item);
        return new AddRequest().withAdGroups(nCopies(numberOfIds, item));
    }

    @Before
    public void setUp() {
        when(auth.getChiefSubclient()).thenReturn(new ApiUser().withClientId(CLIENT_ID));

        clearInvocations(requestConverter);
    }

    @After
    public void resetMocks() {
        Mockito.reset(auth);
    }

    @Test
    public void validate_idsAtTheLimit_noError() {
        AddRequest request = createRequest(MAX_ELEMENTS_PER_ADD, item -> item.setCampaignId(CAMPAIGN_ID));

        ValidationResult<AddRequest, DefectType> result = delegate.validateRequest(request);

        assertThat(result).is(matchedBy(hasNoDefects()));
    }

    @Test
    public void validate_idsOverlowTheLimit_errorIsGenerated() {
        AddRequest request = createRequest(MAX_ELEMENTS_PER_ADD + 1, item -> item.setCampaignId(CAMPAIGN_ID));

        ValidationResult<AddRequest, DefectType> result = delegate.validateRequest(request);

        assertThat(result).is(matchedBy(hasDefectWith(validationError(9300))));
    }

    @Test
    public void validate_groupsContainNull_errorIsGenerated() {
        AddRequest request = new AddRequest().withAdGroups(new AdGroupAddItem().withCampaignId(1L),
                null);

        ValidationResult<AddRequest, DefectType> result = delegate.validateRequest(request);

        assertThat(result).is(matchedBy(hasDefectWith(validationError(8000))));
    }

    @Test
    public void requestIsConvertedUsingRequestConverter() {
        AddRequest request = new AddRequest()
                .withAdGroups(new AdGroupAddItem(), new AdGroupAddItem());
        delegate.convertRequest(request);

        verify(requestConverter, times(request.getAdGroups().size()))
                .convertItem(any(AdGroupAddItem.class));
    }

    @Test
    public void validateInternalRequestConvertsValidationSignals() {
        ValidationResult<List<AdGroupsContainer>, DefectType> vr = delegate.validateInternalRequest(
                singletonList(new AdGroupsValidationSignalContainer(DefectTypes.possibleOnlyOneField())));

        Assert.assertThat(
                vr,
                hasDefectWith(
                        validationError(path(index(0)),
                                DefectTypes.possibleOnlyOneField())));
    }

}
