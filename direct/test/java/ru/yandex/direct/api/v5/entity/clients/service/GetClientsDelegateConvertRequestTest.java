package ru.yandex.direct.api.v5.entity.clients.service;

import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.yandex.direct.api.v5.clients.ClientFieldEnum;
import com.yandex.direct.api.v5.clients.GetRequest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Answers;
import org.mockito.Mock;

import ru.yandex.direct.api.v5.context.ApiContextHolder;
import ru.yandex.direct.api.v5.entity.agencyclients.service.RequestedField;
import ru.yandex.direct.api.v5.security.ApiAuthenticationSource;

import static com.yandex.direct.api.v5.clients.ClientFieldEnum.CLIENT_ID;
import static com.yandex.direct.api.v5.clients.ClientFieldEnum.CURRENCY;
import static com.yandex.direct.api.v5.clients.ClientFieldEnum.TYPE;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

@RunWith(Parameterized.class)
public class GetClientsDelegateConvertRequestTest {
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ApiAuthenticationSource apiAuthenticationSource;
    @Mock
    private ApiContextHolder apiContextHolder;
    @Mock
    private ClientDataFetcher clientDataFetcher;

    private GetClientsDelegate getClientsDelegate;

    @Parameterized.Parameter()
    public List<ClientFieldEnum> origFields;

    @Parameterized.Parameter(1)
    public Set<RequestedField> expectedAgencyFields;

    @Parameterized.Parameter(2)
    public Set<RequestedField> expectedClientFields;

    @Parameterized.Parameters(name = "{0}")
    public static List<Object[]> parameters() {
        return asList(new Object[][]{
                {
                        asList(CLIENT_ID, CURRENCY),
                        ImmutableSet.of(RequestedField.CLIENT_ID),
                        ImmutableSet.of(RequestedField.CLIENT_ID, RequestedField.CURRENCY)
                },
                {
                        asList(CLIENT_ID, TYPE),
                        ImmutableSet.of(RequestedField.CLIENT_ID, RequestedField.TYPE),
                        ImmutableSet.of(RequestedField.CLIENT_ID, RequestedField.TYPE)
                },
        });
    }

    @Before
    public void before() {
        initMocks(this);
        getClientsDelegate = new GetClientsDelegate(apiAuthenticationSource, apiContextHolder, clientDataFetcher);
    }

    @Test
    public void convertRequest_requestFieldsAsClient_returnsExpected() {
        when(apiAuthenticationSource.getSubclient().isAgency()).thenReturn(false);
        Set<RequestedField> requestedFields =
                getClientsDelegate.convertRequest(new GetRequest().withFieldNames(origFields));
        assertThat(requestedFields).containsExactlyInAnyOrderElementsOf(expectedClientFields);
    }

    @Test
    public void convertRequest_requestFieldsAsAgency_returnsExpected() {
        when(apiAuthenticationSource.getSubclient().isAgency()).thenReturn(true);
        Set<RequestedField> requestedFields =
                getClientsDelegate.convertRequest(new GetRequest().withFieldNames(origFields));
        assertThat(requestedFields).containsExactlyInAnyOrderElementsOf(expectedAgencyFields);
    }
}
