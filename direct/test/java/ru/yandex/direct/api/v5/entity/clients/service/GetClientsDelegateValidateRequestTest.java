package ru.yandex.direct.api.v5.entity.clients.service;

import java.util.List;

import com.yandex.direct.api.v5.clients.ClientFieldEnum;
import com.yandex.direct.api.v5.clients.GetRequest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Answers;
import org.mockito.Mock;

import ru.yandex.direct.api.v5.context.ApiContextHolder;
import ru.yandex.direct.api.v5.security.ApiAuthenticationSource;
import ru.yandex.direct.api.v5.validation.DefectType;
import ru.yandex.direct.validation.result.ValidationResult;

import static com.yandex.direct.api.v5.clients.ClientFieldEnum.ACCOUNT_QUALITY;
import static com.yandex.direct.api.v5.clients.ClientFieldEnum.CLIENT_ID;
import static com.yandex.direct.api.v5.clients.ClientFieldEnum.CURRENCY;
import static com.yandex.direct.api.v5.clients.ClientFieldEnum.GRANTS;
import static com.yandex.direct.api.v5.clients.ClientFieldEnum.OVERDRAFT_SUM_AVAILABLE;
import static com.yandex.direct.api.v5.clients.ClientFieldEnum.RESTRICTIONS;
import static com.yandex.direct.api.v5.clients.ClientFieldEnum.SETTINGS;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static ru.yandex.direct.api.v5.validation.AssertJMatcherAdaptors.hasDefectWith;
import static ru.yandex.direct.api.v5.validation.AssertJMatcherAdaptors.hasNoDefects;
import static ru.yandex.direct.api.v5.validation.Matchers.validationError;


@RunWith(Parameterized.class)
public class GetClientsDelegateValidateRequestTest {
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ApiAuthenticationSource apiAuthenticationSource;
    @Mock
    private ApiContextHolder apiContextHolder;
    @Mock
    private ClientDataFetcher clientDataFetcher;

    private GetClientsDelegate getClientsDelegate;

    private ClientFieldEnum fieldToRequest;
    private boolean isForbiddenForAgency;

    public GetClientsDelegateValidateRequestTest(ClientFieldEnum fieldToRequest, boolean isForbiddenForAgency) {
        this.fieldToRequest = fieldToRequest;
        this.isForbiddenForAgency = isForbiddenForAgency;
    }

    @Parameterized.Parameters(name = "{0}")
    public static List<Object[]> parameters() {
        return asList(new Object[][]{
                {CURRENCY, true},
                {GRANTS, true},
                {OVERDRAFT_SUM_AVAILABLE, true},
                {RESTRICTIONS, true},
                {SETTINGS, true},
                {ACCOUNT_QUALITY, true},
                {CLIENT_ID, false},
        });
    }

    @Before
    public void before() {
        openMocks(this);
        getClientsDelegate = new GetClientsDelegate(apiAuthenticationSource, apiContextHolder, clientDataFetcher);
    }

    @Test
    public void validateRequest_requestFieldsAsClient_hasNoErrors() {
        when(apiAuthenticationSource.getSubclient().isAgency()).thenReturn(false);
        ValidationResult<GetRequest, DefectType> vr =
                getClientsDelegate.validateRequest(new GetRequest().withFieldNames(fieldToRequest));
        assertThat(vr).is(hasNoDefects());
    }

    @Test
    public void validateRequest_requestIgnoredFieldsAsAgency_returnsExpected() {
        when(apiAuthenticationSource.getSubclient().isAgency()).thenReturn(true);
        ValidationResult<GetRequest, DefectType> vr =
                getClientsDelegate.validateRequest(new GetRequest().withFieldNames(fieldToRequest));
        if (isForbiddenForAgency) {
            assertThat(vr).is(hasDefectWith(validationError(8000)));
        } else {
            assertThat(vr).is(hasNoDefects());
        }
    }

    @Test
    public void validateRequest_requestIgnoredFieldsWithClientIdAsAgency_hasNoErrors() {
        when(apiAuthenticationSource.getSubclient().isAgency()).thenReturn(true);
        ValidationResult<GetRequest, DefectType> vr =
                getClientsDelegate.validateRequest(new GetRequest().withFieldNames(
                        ClientFieldEnum.CLIENT_ID, fieldToRequest));
        assertThat(vr).is(hasNoDefects());
    }
}
