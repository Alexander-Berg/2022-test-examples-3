package ru.yandex.direct.api.v5.entity.smartadtargets.delegate;

import java.util.Collections;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import com.yandex.direct.api.v5.general.ExceptionNotification;
import com.yandex.direct.api.v5.general.IdsCriteria;
import com.yandex.direct.api.v5.smartadtargets.DeleteRequest;
import com.yandex.direct.api.v5.smartadtargets.DeleteResponse;
import one.util.streamex.LongStreamEx;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.api.v5.context.ApiContext;
import ru.yandex.direct.api.v5.context.ApiContextHolder;
import ru.yandex.direct.api.v5.converter.ResultConverter;
import ru.yandex.direct.api.v5.entity.ApiValidationException;
import ru.yandex.direct.api.v5.entity.GenericApiService;
import ru.yandex.direct.api.v5.entity.smartadtargets.validation.SmartAdTargetValidationService;
import ru.yandex.direct.api.v5.security.ApiAuthenticationSource;
import ru.yandex.direct.api.v5.service.accelinfo.AccelInfoHeaderSetter;
import ru.yandex.direct.api.v5.testing.configuration.Api5Test;
import ru.yandex.direct.api.v5.units.ApiUnitsService;
import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.core.entity.campaign.service.accesschecker.RequestCampaignAccessibilityCheckerProvider;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.entity.performancefilter.container.PerformanceFiltersQueryFilter;
import ru.yandex.direct.core.entity.performancefilter.model.PerformanceFilter;
import ru.yandex.direct.core.entity.performancefilter.repository.PerformanceFilterRepository;
import ru.yandex.direct.core.entity.performancefilter.service.PerformanceFilterService;
import ru.yandex.direct.core.entity.user.model.ApiUser;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.PerformanceFilterInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Collections.singleton;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.core.testing.data.TestPerformanceFilters.compareFilters;

@ParametersAreNonnullByDefault
@Api5Test
@RunWith(SpringRunner.class)
public class DeleteSmartAdTargetDelegateTest {
    @Autowired
    private Steps steps;
    @Autowired
    private SmartAdTargetValidationService validationService;
    @Autowired
    private PerformanceFilterService performanceFilterService;
    @Autowired
    private ResultConverter resultConverter;
    @Autowired
    private PerformanceFilterRepository performanceFilterRepository;
    @Autowired
    private PpcPropertiesSupport ppcPropertiesSupport;
    @Autowired
    private FeatureService featureService;

    private GenericApiService genericApiService;
    private DeleteSmartAdTargetDelegate delegate;

    private Integer shard;
    private Long filterId;
    private PerformanceFiltersQueryFilter queryFilter;

    private static DeleteRequest createRequest(Long filterId) {
        return new DeleteRequest()
                .withSelectionCriteria(
                        new IdsCriteria()
                                .withIds(Collections.singleton(filterId)));
    }

    @Before
    public void before() {
        PerformanceFilterInfo filterInfo = steps.performanceFilterSteps().createDefaultPerformanceFilter();
        filterId = filterInfo.getFilterId();
        shard = filterInfo.getShard();
        ClientInfo clientInfo = filterInfo.getClientInfo();
        Long uid = clientInfo.getUid();
        ClientId clientId = clientInfo.getClientId();

        queryFilter = PerformanceFiltersQueryFilter.newBuilder()
                .withPerfFilterIds(singleton(filterId))
                .build();

        ApiUser user = new ApiUser()
                .withUid(uid)
                .withClientId(clientId);
        ApiAuthenticationSource auth = mock(ApiAuthenticationSource.class);
        when(auth.getOperator()).thenReturn(user);
        when(auth.getChiefSubclient()).thenReturn(user);
        ApiContextHolder apiContextHolder = mock(ApiContextHolder.class);
        when(apiContextHolder.get()).thenReturn(new ApiContext());
        genericApiService = new GenericApiService(apiContextHolder,
                mock(ApiUnitsService.class),
                mock(AccelInfoHeaderSetter.class),
                mock(RequestCampaignAccessibilityCheckerProvider.class));
        delegate = new DeleteSmartAdTargetDelegate(auth, validationService, performanceFilterService,
                resultConverter, ppcPropertiesSupport, featureService);
    }

    @Test
    public void delete_success() {
        PerformanceFilter startFilter = performanceFilterRepository.getFilters(shard, queryFilter).get(0);
        checkState(!startFilter.getIsDeleted());

        PerformanceFilter expectedFilter = new PerformanceFilter()
                .withId(filterId)
                .withIsDeleted(true);

        DeleteRequest request = createRequest(filterId);
        DeleteResponse response = genericApiService.doAction(delegate, request);
        List<ExceptionNotification> errors = response.getDeleteResults().get(0).getErrors();
        checkState(errors.isEmpty(), "Unexpected error");

        PerformanceFilter actualFilter = performanceFilterRepository.getFilters(shard, queryFilter).get(0);
        compareFilters(actualFilter, expectedFilter);
    }

    @Test
    public void delete_whenFilterNotExist_failure() {
        long notExistFilterId = Integer.MAX_VALUE - 1L;
        DeleteRequest request = createRequest(notExistFilterId);
        DeleteResponse response = genericApiService.doAction(delegate, request);
        List<ExceptionNotification> errors = response.getDeleteResults().get(0).getErrors();
        Assertions.assertThat(errors).isNotEmpty();
    }

    @Test(expected = ApiValidationException.class)
    public void suspend_whenIdsIsTooMany_failure() {
        List<Long> ids = LongStreamEx.range(1L, 10_002L)
                .boxed()
                .toList();
        DeleteRequest request = new DeleteRequest()
                .withSelectionCriteria(
                        new IdsCriteria()
                                .withIds(ids));
        genericApiService.doAction(delegate, request);
    }

}
