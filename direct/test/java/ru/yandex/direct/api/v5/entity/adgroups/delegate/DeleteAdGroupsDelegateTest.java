package ru.yandex.direct.api.v5.entity.adgroups.delegate;

import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import com.yandex.direct.api.v5.adgroups.DeleteRequest;
import com.yandex.direct.api.v5.general.IdsCriteria;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.api.v5.converter.ResultConverter;
import ru.yandex.direct.api.v5.entity.adgroups.AdGroupTypeValidationService;
import ru.yandex.direct.api.v5.entity.adgroups.converter.DeleteAdGroupsRequestConverter;
import ru.yandex.direct.api.v5.result.ApiMassResult;
import ru.yandex.direct.api.v5.security.ApiAuthenticationSource;
import ru.yandex.direct.api.v5.testing.configuration.Api5Test;
import ru.yandex.direct.api.v5.validation.DefectType;
import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository;
import ru.yandex.direct.core.entity.adgroup.service.AdGroupService;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.entity.user.model.ApiUser;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.validation.result.ValidationResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@Api5Test
@RunWith(SpringRunner.class)
@ParametersAreNonnullByDefault
public class DeleteAdGroupsDelegateTest {

    @Autowired
    private Steps steps;
    @Autowired
    private AdGroupService adGroupService;
    @Autowired
    private AdGroupRepository adGroupRepository;
    @Autowired
    private DeleteAdGroupsRequestConverter requestConverter;
    @Autowired
    private ResultConverter resultConverter;
    @Autowired
    private PpcPropertiesSupport ppcPropertiesSupport;
    @Autowired
    private FeatureService featureService;
    @Mock
    private ApiAuthenticationSource auth;

    private DeleteAdGroupsDelegate delegate;

    private ClientInfo clientInfo;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        clientInfo = steps.clientSteps().createDefaultClient();
        ClientId clientId = clientInfo.getClientId();

        when(auth.getSubclient()).thenReturn(new ApiUser().withClientId(clientId));
        when(auth.getChiefSubclient()).thenReturn(new ApiUser().withClientId(clientId));
        when(auth.getOperator()).thenReturn(new ApiUser().withUid(clientInfo.getUid()));
        AdGroupTypeValidationService typeValidationService = new AdGroupTypeValidationService(auth, adGroupService);
        delegate = new DeleteAdGroupsDelegate(auth,
                requestConverter,
                resultConverter,
                adGroupService,
                typeValidationService,
                ppcPropertiesSupport,
                featureService);
    }

    @Test
    public void processList_performanceAdGroup() {
        var adGroupInfo = steps.adGroupSteps().createDefaultPerformanceAdGroup(clientInfo);
        var adGroupId = adGroupInfo.getAdGroupId();
        var idsCriteria = new IdsCriteria().withIds(adGroupId);
        List<Long> adGroupIds =
                delegate.convertRequest(new DeleteRequest().withSelectionCriteria(idsCriteria));
        ValidationResult<List<Long>, DefectType> internalRequest =
                delegate.validateInternalRequest(adGroupIds);
        ApiMassResult<Long> apiResult = delegate.processList(internalRequest.getValue());
        assertThat(apiResult.getErrorCount()).isEqualTo(0);
        var existingIds = adGroupRepository.getExistingAdGroupIds(adGroupInfo.getShard(), List.of(adGroupId));
        assertThat(existingIds).isEmpty();
    }

}
