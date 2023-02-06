package ru.yandex.direct.api.v5.entity.clients.service;

import java.util.List;

import com.yandex.direct.api.v5.clients.UpdateRequest;
import com.yandex.direct.api.v5.clients.UpdateResponse;
import com.yandex.direct.api.v5.generalclients.ClientUpdateItem;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.direct.api.v5.converter.ResultConverter;
import ru.yandex.direct.api.v5.entity.clients.validation.UpdateRequestValidator;
import ru.yandex.direct.api.v5.security.ApiAuthenticationSource;
import ru.yandex.direct.api.v5.units.ApiUnitsService;
import ru.yandex.direct.api.v5.validation.DefectType;
import ru.yandex.direct.api.v5.validation.DefectTypes;
import ru.yandex.direct.api.v5.validation.ValidationException;
import ru.yandex.direct.core.entity.client.model.Client;
import ru.yandex.direct.core.entity.client.service.ClientService;
import ru.yandex.direct.core.entity.client.service.validation.ClientValidationService;
import ru.yandex.direct.core.entity.user.model.ApiUser;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.entity.user.service.UserService;
import ru.yandex.direct.core.entity.user.service.validation.UserValidationService;
import ru.yandex.direct.core.units.OperationSummary;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.model.AppliedChanges;
import ru.yandex.direct.validation.defect.CommonDefects;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.DefectInfo;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;


public class UpdateClientOperationTest {
    private static final long UID = 11L;
    private static final long CLIENT_ID = 22L;
    private static final String LOGIN = "xxx";
    private static final String PROPERTY_CLIENT_ID = "clientId";
    private UpdateClientOperation operationUnderTest;
    private UpdateRequestValidator requestValidator;
    private ApiUnitsService apiUnitsService;
    private ApiUser targetUser;
    private ApiAuthenticationSource apiAuthenticationSource;
    private UserValidationService userValidationService;
    private ClientValidationService clientValidationService;
    private ValidationResult<Client, Defect> clientVr;
    private ValidationResult<User, Defect> userVr;
    private ResultConverter resultConverter;
    private UserService userService;
    private ClientService clientService;

    @Before
    public void setUp() throws Exception {
        requestValidator = mock(UpdateRequestValidator.class);
        ValidationResult vr = mock(ValidationResult.class);
        when(vr.hasAnyErrors()).thenReturn(false);
        when(vr.flattenErrors()).thenReturn(emptyList());
        when(requestValidator.validate(any())).thenReturn(vr);

        targetUser = mock(ApiUser.class);
        when(targetUser.getUid()).thenReturn(UID);
        when(targetUser.getId()).thenReturn(UID);
        when(targetUser.getClientId()).thenReturn(ClientId.fromLong(CLIENT_ID));
        when(targetUser.getLogin()).thenReturn(LOGIN);
        when(targetUser.isClient()).thenReturn(true);

        apiAuthenticationSource = mock(ApiAuthenticationSource.class);
        when(apiAuthenticationSource.getChiefOperator()).thenReturn(targetUser);
        when(apiAuthenticationSource.getOperator()).thenReturn(targetUser);
        when(apiAuthenticationSource.getSubclient()).thenReturn(targetUser);

        userVr = mock(ValidationResult.class);
        when(userVr.hasAnyErrors()).thenReturn(false);
        userValidationService = mock(UserValidationService.class);
        when(userValidationService.validate(any(AppliedChanges.class))).thenReturn(userVr);

        clientVr = mock(ValidationResult.class);
        when(clientVr.hasAnyErrors()).thenReturn(false);
        clientValidationService = mock(ClientValidationService.class);
        when(clientValidationService.validate(any())).thenReturn(clientVr);

        apiUnitsService = mock(ApiUnitsService.class);
        resultConverter = mock(ResultConverter.class);
        userService = mock(UserService.class);
        clientService = mock(ClientService.class);
        operationUnderTest = new UpdateClientOperation(
                apiAuthenticationSource, apiUnitsService, userService, userValidationService, clientService,
                clientValidationService, resultConverter, requestValidator);
    }

    @Test(expected = ValidationException.class)
    public void throwValidationExceptionWhenRequestIsInvalid() {
        ValidationResult vr = createErrorValidationResult();
        when(requestValidator.validate(any())).thenReturn(vr);
        operationUnderTest.perform(new UpdateRequest());
    }

    @Test
    public void withdrawUnitsWhenRequestIsInvalid() {
        ValidationResult vr = createErrorValidationResult();
        when(requestValidator.validate(any())).thenReturn(vr);
        try {
            operationUnderTest.perform(new UpdateRequest());
        } catch (ValidationException ignore) {
        }
        verify(apiUnitsService).withdraw(eq(OperationSummary.unsuccessful()));
    }

    @Test
    public void addErrorWhenRequestHasInvalidUserData() {
        when(userVr.hasAnyErrors()).thenReturn(true);
        List<DefectInfo<Defect>> errorsDt = singletonList(
                new DefectInfo<>(path(field("userField")), null, CommonDefects.invalidValue()));
        List<DefectInfo<DefectType>> errorsDd = singletonList(
                new DefectInfo<>(path(field("userField")), null, DefectTypes.invalidValue()));
        when(userVr.flattenErrors()).thenReturn(errorsDt);
        when(resultConverter.toDefectInfo(errorsDt.get(0), null))
                .thenReturn(errorsDd.get(0));
        UpdateResponse response = operationUnderTest.perform(new UpdateRequest().withClients(new ClientUpdateItem()));
        verify(clientService, never()).update(any());
        verify(userService, never()).update(any());
        verify(resultConverter).addErrors(any(), eq(errorsDd), any());
        assertThat(response.getUpdateResults(), contains(hasProperty(PROPERTY_CLIENT_ID, is(nullValue()))));
    }

    @Test
    public void addErrorWhenRequestHasInvalidClientData() {
        when(clientVr.hasAnyErrors()).thenReturn(true);
        List<DefectInfo<Defect>> errorsDt = singletonList(
                new DefectInfo<>(path(field("clientField")), null, CommonDefects.invalidValue()));
        List<DefectInfo<DefectType>> errorsDd = singletonList(
                new DefectInfo<>(path(field("clientField")), null, DefectTypes.invalidValue()));
        when(clientVr.flattenErrors()).thenReturn(errorsDt);
        when(resultConverter.toDefectInfo(errorsDt.get(0), null))
                .thenReturn(errorsDd.get(0));
        UpdateResponse response = operationUnderTest.perform(new UpdateRequest().withClients(new ClientUpdateItem()));
        verify(clientService, never()).update(any());
        verify(userService, never()).update(any());
        verify(resultConverter).addErrors(any(), eq(errorsDd), any());
        assertThat(response.getUpdateResults(), contains(hasProperty(PROPERTY_CLIENT_ID, is(nullValue()))));
    }

    @Test
    public void withdrawUnitsWhenRequestHasInvalidData() {
        when(clientVr.hasAnyErrors()).thenReturn(true);
        List<DefectInfo<Defect>> errors = singletonList(
                new DefectInfo<>(path(field("clientField")), null, CommonDefects.invalidValue()));
        when(clientVr.flattenErrors()).thenReturn(errors);
        operationUnderTest.perform(new UpdateRequest().withClients(new ClientUpdateItem()));
        verify(apiUnitsService).withdraw(eq(OperationSummary.unsuccessful()));
    }

    @Test
    public void successUpdate() {
        UpdateResponse response = operationUnderTest.perform(new UpdateRequest().withClients(new ClientUpdateItem()));
        verify(clientService).update(any());
        verify(userService).update(any());
        assertThat(response.getUpdateResults(), contains(hasProperty(PROPERTY_CLIENT_ID, equalTo(CLIENT_ID))));
    }

    @Test
    public void withdrawUnitsWhenUpdateIsSuccess() {
        operationUnderTest.perform(new UpdateRequest().withClients(new ClientUpdateItem()));
        verify(apiUnitsService).withdraw(eq(OperationSummary.successful(1)));
    }

    private ValidationResult createErrorValidationResult() {
        ValidationResult vr = mock(ValidationResult.class);
        when(vr.hasAnyErrors()).thenReturn(true);
        when(vr.flattenErrors()).thenReturn(singletonList(
                new DefectInfo<>(path(field("xxx")), null, DefectTypes.invalidValue())));
        return vr;
    }
}
