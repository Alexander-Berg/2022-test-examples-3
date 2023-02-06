package ru.yandex.direct.api.v5.entity.adgroups.delegate;

import java.util.List;
import java.util.Set;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import ru.yandex.direct.api.v5.converter.ResultConverter;
import ru.yandex.direct.api.v5.entity.adgroups.AdGroupTypeValidationService;
import ru.yandex.direct.api.v5.entity.adgroups.converter.DeleteAdGroupsRequestConverter;
import ru.yandex.direct.api.v5.security.ApiAuthenticationSource;
import ru.yandex.direct.api.v5.validation.DefectType;
import ru.yandex.direct.core.entity.adgroup.model.AdGroupType;
import ru.yandex.direct.core.entity.adgroup.service.AdGroupService;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Collections.singletonList;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ParametersAreNonnullByDefault
public class DeleteAdGroupsDelegateValidateTest {
    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule().silent();

    @Mock
    private ApiAuthenticationSource apiAuthenticationSource;
    @Mock
    private DeleteAdGroupsRequestConverter requestConverter;
    @Mock
    private AdGroupService adGroupService;
    @Mock
    private ResultConverter resultConverter;
    @Mock
    private AdGroupTypeValidationService typeValidationService;

    @InjectMocks
    private DeleteAdGroupsDelegate delegate;

    @Test
    public void validateInternalRequest_adGroupTypeIsValidated() {
        List<Long> adGroupIds = singletonList(2L);
        Set<AdGroupType> allowedAdGroupTypes = delegate.getAllowedAdGroupTypes();
        when(typeValidationService.validate(adGroupIds, allowedAdGroupTypes)).thenReturn(new ValidationResult<>(adGroupIds));

        ValidationResult<List<Long>, DefectType> vr = delegate.validateInternalRequest(adGroupIds);

        verify(typeValidationService, atLeastOnce()).validate(adGroupIds, allowedAdGroupTypes);
    }
}
