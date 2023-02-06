package ru.yandex.market.logistic.api.service;

import java.util.Arrays;
import java.util.Collections;
import java.util.function.Consumer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.ImmutableList;
import org.apache.http.HttpHeaders;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import ru.yandex.market.logistic.api.exceptions.RequestStateErrorException;
import ru.yandex.market.logistic.api.exceptions.ValidationException;
import ru.yandex.market.logistic.api.model.common.ErrorCode;
import ru.yandex.market.logistic.api.model.common.ErrorPair;
import ru.yandex.market.logistic.api.model.common.PartnerMethod;
import ru.yandex.market.logistic.api.model.common.request.AbstractRequest;
import ru.yandex.market.logistic.api.model.common.request.RequestState;
import ru.yandex.market.logistic.api.model.common.request.RequestWrapper;
import ru.yandex.market.logistic.api.model.common.response.AbstractResponse;
import ru.yandex.market.logistic.api.model.common.response.ResponseWrapper;
import ru.yandex.market.logistic.api.model.fulfillment.ResourceId;
import ru.yandex.market.logistic.api.model.fulfillment.request.CreateInboundRequest;
import ru.yandex.market.logistic.api.model.fulfillment.request.GetInboundsStatusRequest;
import ru.yandex.market.logistic.api.model.fulfillment.response.CreateInboundResponse;
import ru.yandex.market.logistic.api.model.fulfillment.response.GetInboundsStatusResponse;
import ru.yandex.market.logistic.api.model.fulfillment.response.GetStocksResponse;
import ru.yandex.market.logistic.api.model.fulfillment.response.entities.InboundStatus;
import ru.yandex.market.logistic.api.model.fulfillment.response.entities.InboundStatusType;
import ru.yandex.market.logistic.api.model.properties.PartnerProperties;
import ru.yandex.market.logistic.api.utils.DateTime;
import ru.yandex.market.logistic.api.utils.HttpTemplate;
import ru.yandex.market.logistic.api.utils.UniqService;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistic.api.service.RequestService.ACCESS_TOKEN_AUTHORIZATION_TYPE;
import static ru.yandex.market.logistic.api.service.RequestService.NO_ERROR_DESCRIPTION_RETURNED;
import static ru.yandex.market.logistic.api.service.RequestService.SERVICE_TICKET_HEADER;

/**
 * Тест для {@link RequestService}.
 */
@ExtendWith(MockitoExtension.class)
class RequestServiceTest {

    private static final TypeReference<ResponseWrapper<AbstractResponse>> RESPONSE_TYPE =
        new TypeReference<ResponseWrapper<AbstractResponse>>() {
        };

    private static final String UNIQ = "056O5sTu33EdNWdJL83lSAHpl8ptKVqc";

    private static final String TOKEN = "xxxxxxxxxxxxxxxxxxxxxxmarschrouteT.T.Tokenxxxxxxxxxxxxxxxxxxxxxxxxxx";

    private static final String PARTNER_URL = "/mock";

    private static final AbstractRequest REQUEST = new AbstractRequest(PartnerMethod.GET_INBOUNDS_STATUS_FF) {
    };

    @Mock
    private HttpTemplate httpTemplate;

    @Mock
    private UniqService uniqService;

    private RequestService requestService;

    @BeforeEach
    void setup() {
        requestService = new RequestService(httpTemplate, uniqService);
        when(uniqService.generate()).thenReturn(UNIQ);
    }

    @Test
    void testSuccessRequest() {
        ResourceId inboundId = new ResourceId.ResourceIdBuilder().setYandexId("123").setFulfillmentId("A123").build();
        GetInboundsStatusRequest request = new GetInboundsStatusRequest(Collections.singletonList(inboundId));

        GetInboundsStatusResponse response = new GetInboundsStatusResponse(
            Collections.singletonList(new InboundStatus(
                inboundId,
                InboundStatusType.ACCEPTED,
                new DateTime("2019-02-07T11:22:59+03:00")
            )));

        ResponseWrapper<GetInboundsStatusResponse> responseWrapper = new ResponseWrapper<>();
        responseWrapper.setResponse(response);
        responseWrapper.setRequestState(new RequestState().setIsError(false));

        when(httpTemplate.executePost(
            any(RequestWrapper.class),
            eq(RESPONSE_TYPE),
            eq(PARTNER_URL),
            any(MultiValueMap.class)
        ))
            .thenReturn(responseWrapper);

        executeRequest(request);
    }

    @Test
    void testRequestValidation() {
        CreateInboundRequest invalidRequest = new CreateInboundRequest(null);
        assertThatThrownBy(() -> executeRequest(invalidRequest))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("must not be null");
    }

    @Test
    void testResponseValidation() {

        CreateInboundResponse invalidResponse = new CreateInboundResponse(null);

        ResponseWrapper<CreateInboundResponse> responseWrapper = new ResponseWrapper<>();
        responseWrapper.setResponse(invalidResponse);
        responseWrapper.setRequestState(new RequestState().setIsError(false));

        when(httpTemplate.executePost(
            any(RequestWrapper.class),
            eq(RESPONSE_TYPE),
            eq(PARTNER_URL),
            any(MultiValueMap.class)
        ))
            .thenReturn(responseWrapper);

        assertThatThrownBy(this::executeRequest)
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("must not be null");
    }

    @Test
    void testValidationExceptionWithoutMessages() {
        bindRequestStateToExecution((requestState) -> {
        });
        assertThatThrownBy(this::executeRequest)
            .isInstanceOf(RequestStateErrorException.class)
            .hasMessage(NO_ERROR_DESCRIPTION_RETURNED);
    }

    @Test
    void testValidationExceptionWithErrorCodes() {
        bindRequestStateToExecution((requestState) -> requestState.setErrorCodes(Arrays.asList(
            new ErrorPair(ErrorCode.SERVICE_UNAVAILABLE, "some message"),
            new ErrorPair(ErrorCode.SERVICE_UNAVAILABLE, "another message", "some description")
        )));

        assertThatThrownBy(this::executeRequest)
            .isInstanceOf(RequestStateErrorException.class)
            .hasMessage("code 1000: some message, code 1000: another message, some description");
    }

    @Test
    void testValidationExceptionWithErrors() {
        bindRequestStateToExecution((requestState) ->
            requestState.setErrors(Arrays.asList("erROR", "ErroR")));

        assertThatThrownBy(this::executeRequest)
            .isInstanceOf(RequestStateErrorException.class)
            .hasMessage("erROR, ErroR");
    }

    @Test
    void testValidationExceptionWithErrorCodesAndErrors() {
        bindRequestStateToExecution((requestState) -> {
            requestState.setErrors(Arrays.asList("erROR", "ErroR"));
            requestState.setErrorCodes(Arrays.asList(
                new ErrorPair(ErrorCode.SERVICE_UNAVAILABLE, "some message"),
                new ErrorPair(ErrorCode.SERVICE_UNAVAILABLE, "another message", "some description")
            ));
        });

        assertThatThrownBy(this::executeRequest)
            .isInstanceOf(RequestStateErrorException.class)
            .hasMessage("erROR, ErroR, code 1000: some message, code 1000: another message, some description");
    }

    @Test
    void testHeaders() {
        ResponseWrapper<GetStocksResponse> responseWrapper = new ResponseWrapper<>();
        responseWrapper.setResponse(new GetStocksResponse(ImmutableList.of()));
        responseWrapper.setRequestState(new RequestState().setIsError(false));

        String ticket = "service-ticket";
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.put(SERVICE_TICKET_HEADER, ImmutableList.of(ticket));
        headers.put(HttpHeaders.AUTHORIZATION, ImmutableList.of(ACCESS_TOKEN_AUTHORIZATION_TYPE + " " + TOKEN));

        when(httpTemplate.executePost(
            any(RequestWrapper.class),
            eq(RESPONSE_TYPE),
            eq(PARTNER_URL),
            eq(headers)
        ))
            .thenReturn(responseWrapper);

        requestService.executeRequest(REQUEST, new PartnerProperties(TOKEN, PARTNER_URL, ticket), RESPONSE_TYPE);
    }

    private void bindRequestStateToExecution(Consumer<RequestState> requestStateConsumer) {
        RequestState requestState = new RequestState();
        requestState.setIsError(true);

        requestStateConsumer.accept(requestState);

        ResponseWrapper responseWrapper = new ResponseWrapper();
        responseWrapper.setRequestState(requestState);

        when(httpTemplate.executePost(
            any(RequestWrapper.class),
            eq(RESPONSE_TYPE),
            eq(PARTNER_URL),
            any(MultiValueMap.class)
        ))
            .thenReturn(responseWrapper);
    }

    private void executeRequest() {
        executeRequest(REQUEST);
    }

    private <Request extends AbstractRequest> void executeRequest(Request request) {
        requestService.executeRequest(request, getPartnerProperties(), RESPONSE_TYPE);
    }

    private PartnerProperties getPartnerProperties() {
        return new PartnerProperties(TOKEN, PARTNER_URL);
    }
}
