package ru.yandex.direct.api.v5.entity.adgroups.delegate;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import javax.annotation.ParametersAreNonnullByDefault;

import com.yandex.direct.api.v5.adgroups.AdGroupUpdateItem;
import com.yandex.direct.api.v5.adgroups.UpdateRequest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.api.v5.converter.ResultConverter;
import ru.yandex.direct.api.v5.entity.adgroups.container.AdGroupsContainer;
import ru.yandex.direct.api.v5.entity.adgroups.container.UpdateAdGroupsSimpleContainer;
import ru.yandex.direct.api.v5.entity.adgroups.converter.AdGroupsUpdateRequestConverter;
import ru.yandex.direct.api.v5.result.ApiResult;
import ru.yandex.direct.api.v5.security.ApiAuthenticationSource;
import ru.yandex.direct.api.v5.testing.configuration.Api5Test;
import ru.yandex.direct.api.v5.validation.DefectType;
import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.CpmVideoAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.TextAdGroup;
import ru.yandex.direct.core.entity.user.model.ApiUser;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Collections.nCopies;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.api.v5.entity.adgroups.AdGroupDefectTypes.feedsNotAllowed;
import static ru.yandex.direct.api.v5.entity.adgroups.AdGroupDefectTypes.hyperGeoSettingsWasFlushed;
import static ru.yandex.direct.api.v5.entity.adgroups.AdGroupDefectTypes.maxAdGroupsPerUpdateRequest;
import static ru.yandex.direct.api.v5.entity.adgroups.Constants.MAX_ELEMENTS_PER_UPDATE;
import static ru.yandex.direct.api.v5.validation.AssertJMatcherAdaptors.hasDefectWith;
import static ru.yandex.direct.api.v5.validation.AssertJMatcherAdaptors.hasNoDefects;
import static ru.yandex.direct.api.v5.validation.AssertJMatcherAdaptors.hasOnlyWarningDefectWith;
import static ru.yandex.direct.api.v5.validation.Matchers.validationError;
import static ru.yandex.direct.core.entity.adgroup.model.AdGroupType.BASE;
import static ru.yandex.direct.core.entity.adgroup.model.AdGroupType.CPM_VIDEO;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@Api5Test
@RunWith(SpringRunner.class)
@ParametersAreNonnullByDefault
public class UpdateAdGroupDelegateTest {
    private static final UpdateRequest UPDATE_REQUEST = new UpdateRequest()
            .withAdGroups(new AdGroupUpdateItem().withId(1L), new AdGroupUpdateItem().withId(2L));
    private static final ApiResult<List<ApiResult<Long>>> UPDATE_RESULT = ApiResult.successful(
            Arrays.asList(ApiResult.successful(1L), ApiResult.successful(2L)));

    private static final long AD_GROUP_ID = 2L;

    @Autowired
    private Steps steps;

    @Autowired
    private ApiAuthenticationSource auth;

    @Autowired
    private ResultConverter resultConverter;

    @Autowired
    private AdGroupsUpdateRequestConverter requestConverter;

    @Autowired
    private UpdateAdGroupsDelegate delegate;

    @Before
    public void setUp() {
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();

        when(auth.getSubclient()).thenReturn(new ApiUser().withClientId(clientInfo.getClientId()));

        clearInvocations(resultConverter, requestConverter);
    }

    @After
    public void resetMocks() {
        Mockito.reset(auth);
    }

    @Test
    public void requestIsConvertedUsingRequestConverter() {
        delegate.convertRequest(UPDATE_REQUEST);

        verify(requestConverter, times(UPDATE_REQUEST.getAdGroups().size()))
                .convert(any(AdGroupUpdateItem.class), any());
    }

    @Test
    public void resultIsConvertedUsingResultConverter() {
        delegate.convertResponse(UPDATE_RESULT);

        verify(resultConverter).toActionResults(any(), any());
    }

    @Test
    public void validateRequestSuccess() {
        ValidationResult<UpdateRequest, DefectType> vr = delegate.validateRequest(
                new UpdateRequest()
                        .withAdGroups(new AdGroupUpdateItem()));

        assertThat(vr).is(hasNoDefects());
    }

    @Test
    public void validateRequestTooManyAdGroups() {
        ValidationResult<UpdateRequest, DefectType> vr = delegate.validateRequest(
                new UpdateRequest()
                        .withAdGroups(nCopies(MAX_ELEMENTS_PER_UPDATE + 1, new AdGroupUpdateItem())));

        assertThat(vr).is(
                hasDefectWith(
                        validationError(path(field("AdGroups")),
                                maxAdGroupsPerUpdateRequest(MAX_ELEMENTS_PER_UPDATE))));
    }

    @Test
    public void validateInternalRequest_adGroupWithTypeCpmVideo_Ok() {
        ValidationResult<List<AdGroupsContainer>, DefectType> vr = delegate.validateInternalRequest(
                buildModelChangesContainer(new CpmVideoAdGroup()
                        .withType(CPM_VIDEO)
                        .withId(AD_GROUP_ID), modelChanges -> {}));

        assertThat(vr).is(hasNoDefects());
    }

    @Test
    public void validateInternalRequest_adGroupWithTextAdGroupParams_Ok() {
        ValidationResult<List<AdGroupsContainer>, DefectType> vr = delegate.validateInternalRequest(
                buildModelChangesContainer(new TextAdGroup()
                        .withType(BASE)
                        .withId(AD_GROUP_ID), modelChanges -> {}));

        assertThat(vr).is(hasNoDefects());
    }

    @Test
    public void validateInternalRequest_adGroupWithTextAdGroupFilteredFeedParams_Warning() {
        ValidationResult<List<AdGroupsContainer>, DefectType> vr = delegate.validateInternalRequest(
                buildModelChangesContainer(new TextAdGroup()
                        .withType(BASE)
                        .withId(AD_GROUP_ID), modelChanges -> modelChanges
                        .process(123345L, TextAdGroup.OLD_FEED_ID)
                        .process(List.of(1L, 2L, 3L), TextAdGroup.FEED_FILTER_CATEGORIES))
        );

        assertThat(vr).is(hasOnlyWarningDefectWith(validationError(path(index(0)), feedsNotAllowed())));
    }

    @Test
    public void validateInternalRequest_adGroupWithTextAdGroupFilteredFeedParams_NotOk() {
        ValidationResult<List<AdGroupsContainer>, DefectType> vr = delegate.validateInternalRequest(
                buildModelChangesContainer(new TextAdGroup()
                        .withType(BASE)
                        .withId(AD_GROUP_ID), modelChanges -> modelChanges
                        .process(null, TextAdGroup.OLD_FEED_ID)
                        .process(List.of(1L, 2L, 3L), TextAdGroup.FEED_FILTER_CATEGORIES))
        );

        assertThat(vr).is(hasNoDefects());
    }

    @Test
    public void validateInternalRequest_adGroupWithEmptyTextAdGroupFilteredFeedParams_Ok() {
        ValidationResult<List<AdGroupsContainer>, DefectType> vr = delegate.validateInternalRequest(
                buildModelChangesContainer(new TextAdGroup()
                        .withType(BASE)
                        .withId(AD_GROUP_ID), modelChanges -> modelChanges
                        .process(null, TextAdGroup.FEED_FILTER_CATEGORIES)
                        .process(null, TextAdGroup.OLD_FEED_ID))
        );

        assertThat(vr).is(hasNoDefects());
    }

    @Test
    public void geoIsChangedButHyperGeoIsNotSet() {
        ValidationResult<List<AdGroupsContainer>, DefectType> vr = delegate.validateInternalRequest(
                buildModelChangesContainer(new TextAdGroup()
                        .withType(BASE)
                        .withId(AD_GROUP_ID), modelChanges -> modelChanges
                        .process(List.of(123L), AdGroup.GEO)
                        .process(null, AdGroup.HYPER_GEO_ID))
        );

        assertThat(vr).is(hasNoDefects());
    }

    @Test
    public void geoIsChangedAndHyperGeoSet() {
        ValidationResult<List<AdGroupsContainer>, DefectType> vr = delegate.validateInternalRequest(
                buildModelChangesContainer(new TextAdGroup()
                        .withType(BASE)
                        .withId(AD_GROUP_ID)
                        .withHyperGeoId(2L), modelChanges -> modelChanges
                        .process(List.of(123L), AdGroup.GEO)
                        .process(null, AdGroup.HYPER_GEO_ID))
        );

        assertThat(vr).is(hasOnlyWarningDefectWith(validationError(path(index(0)), hyperGeoSettingsWasFlushed())));
    }

    private static <T extends AdGroup> List<AdGroupsContainer> buildModelChangesContainer(
            T oldAdGroup, Consumer<ModelChanges<? extends T>> consumer) {
        @SuppressWarnings("unchecked")
        ModelChanges<? extends T> modelChanges = new ModelChanges<>(oldAdGroup.getId(),
                (Class<? extends T>) oldAdGroup.getClass());
        consumer.accept(modelChanges);
        return singletonList(new UpdateAdGroupsSimpleContainer(modelChanges, oldAdGroup));
    }
}
