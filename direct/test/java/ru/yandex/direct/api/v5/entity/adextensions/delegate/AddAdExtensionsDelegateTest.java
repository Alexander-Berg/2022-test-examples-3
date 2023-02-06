package ru.yandex.direct.api.v5.entity.adextensions.delegate;

import java.util.List;

import com.yandex.direct.api.v5.adextensions.AddRequest;
import com.yandex.direct.api.v5.adextensions.AddResponse;
import com.yandex.direct.api.v5.general.ActionResult;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import ru.yandex.direct.api.v5.converter.ResultConverter;
import ru.yandex.direct.api.v5.result.ApiMassResult;
import ru.yandex.direct.api.v5.security.ApiAuthenticationSource;
import ru.yandex.direct.api.v5.validation.DefectType;
import ru.yandex.direct.core.entity.addition.callout.model.Callout;
import ru.yandex.direct.core.entity.addition.callout.service.CalloutService;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.result.MassResult;
import ru.yandex.direct.validation.result.PathConverter;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static ru.yandex.direct.api.v5.entity.adextensions.delegate.AddAdExtensionsDelegateTestData.anyMassResultOfLongs;
import static ru.yandex.direct.api.v5.entity.adextensions.delegate.AddAdExtensionsDelegateTestData.createActionResults;
import static ru.yandex.direct.api.v5.entity.adextensions.delegate.AddAdExtensionsDelegateTestData.createAddRequest;
import static ru.yandex.direct.api.v5.entity.adextensions.delegate.AddAdExtensionsDelegateTestData.createListOfCallouts;
import static ru.yandex.direct.api.v5.entity.adextensions.delegate.AddAdExtensionsDelegateTestData.successfulApiMassResult;
import static ru.yandex.direct.api.v5.entity.adextensions.delegate.AddAdExtensionsDelegateTestData.successfulMassResult;
import static ru.yandex.direct.api.v5.validation.Matchers.validationError;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

public class AddAdExtensionsDelegateTest {

    private static final int MAX_CALLOUTS_PER_ADD = 1_000;
    private final ClientId clientId = ClientId.fromLong(131313);
    @Mock
    private CalloutService service;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ApiAuthenticationSource authentication;
    @Mock
    private ResultConverter resultConverter;
    @InjectMocks
    private AddAdExtensionsDelegate delegate;

    @Before
    public void reset() {
        initMocks(this);

        when(authentication.getChiefSubclient().getClientId()).thenReturn(clientId);
    }

    @Test
    public void convertRequest_shouldConvertCallouts() {
        AddRequest request = createAddRequest();

        List<Callout> result = delegate.convertRequest(request);

        List<String> expectedCalloutTexts = request.getAdExtensions().stream()
                .map(ae -> ae.getCallout().getCalloutText())
                .collect(toList());
        List<String> actualCalloutTexts = result.stream()
                .map(Callout::getText)
                .collect(toList());

        assertThat(actualCalloutTexts).containsAll(expectedCalloutTexts);
    }

    @Test
    public void convertRequest_shouldSetClientId() {
        List<Callout> result = delegate.convertRequest(createAddRequest());

        result.forEach(c -> assertThat(c.getClientId()).isEqualTo(clientId.asLong()));
    }

    @Test
    public void processRequest_shouldCallServiceAndReturnConvertedResult() {
        MassResult<Long> serviceResult = successfulMassResult();
        ApiMassResult<Long> expectedResult = successfulApiMassResult();
        List<Callout> internalRequest = createListOfCallouts(clientId, MAX_CALLOUTS_PER_ADD);

        when(resultConverter.toApiMassResult(same(serviceResult))).thenReturn(expectedResult);
        when(service.addCalloutsPartial(eq(clientId), any())).thenReturn(serviceResult);

        ApiMassResult<Long> actualResult = delegate.processRequest(internalRequest);

        assertThat(actualResult).isSameAs(expectedResult);
    }

    @Test
    public void validateRequest_shouldReturnBrokenResultIfMaxElementsCountExceeded() {
        ValidationResult<AddRequest, DefectType> result
                = delegate.validateRequest(createAddRequest(MAX_CALLOUTS_PER_ADD + 1));

        assertThat(result.flattenErrors()).is(matchedBy(contains(validationError(9300))));
    }

    @Test
    public void convertResponse_shouldReturnResponseWithActionResultFromResultConverter() {
        List<ActionResult> expectedResults = createActionResults();
        when(resultConverter.toActionResults(anyMassResultOfLongs(), any(PathConverter.class)))
                .thenReturn(expectedResults);

        AddResponse response = delegate.convertResponse(successfulApiMassResult());

        assertThat(response.getAddResults()).containsAll(expectedResults);
    }

}
