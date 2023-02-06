package ru.yandex.market.b2b.clients.impl;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.b2b.clients.ClientProxyService;
import ru.yandex.market.b2b.clients.common.InternalException;
import ru.yandex.market.common.retrofit.CommonRetrofitHttpExecutionException;
import ru.yandex.market.common.retrofit.ExecuteCall;
import ru.yandex.mj.generated.client.taxi_corp_request_market_offer.api.TaxiCorpRequestMarketOfferApiClient;
import ru.yandex.mj.generated.client.taxi_corp_request_market_offer.model.AddressFields;
import ru.yandex.mj.generated.client.taxi_corp_request_market_offer.model.CreateMarketClientRequest;
import ru.yandex.mj.generated.client.taxi_corp_request_market_offer.model.CreateMarketClientResponse;
import ru.yandex.mj.generated.client.taxi_corp_request_market_offer.model.Error;
import ru.yandex.mj.generated.client.taxi_corp_request_market_offer.model.ErrorDetails;
import ru.yandex.mj.generated.client.taxi_corp_request_market_offer.model.FieldError;
import ru.yandex.mj.generated.server.model.AddressFieldsDto;
import ru.yandex.mj.generated.server.model.ClientRegistrationUtm;
import ru.yandex.mj.generated.server.model.CreateClientRequestDto;
import ru.yandex.mj.generated.server.model.CreateClientResponseDto;
import ru.yandex.mj.generated.server.model.EdoProvider;
import ru.yandex.mj.generated.server.model.ErrorCreateClientDto;
import ru.yandex.mj.generated.server.model.ErrorCreateClientDtoDetails;
import ru.yandex.mj.generated.server.model.ErrorCreateClientFieldDto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class ClientProxyServiceImplTest {

    @Mock
    private ObjectMapper mapper;

    @Mock
    private TaxiCorpRequestMarketOfferApiClient taxiApi;

    @InjectMocks
    private ClientProxyServiceImpl proxyService;

    @Test
    public void create_callsTaxiApiWithCorrectArguments() {
        // Given
        String remoteIp = "remoteIp";
        CreateClientRequestDto request = makeRequest();
        CreateMarketClientResponse taxiResponse = new CreateMarketClientResponse().clientId("clientId");
        doReturn(asCall(taxiResponse)).when(taxiApi).v1MarketClientCreatePost(any(), any());

        // When
        proxyService.create(remoteIp, request);

        // Then
        ArgumentCaptor<CreateMarketClientRequest> requestCaptor =
                ArgumentCaptor.forClass(CreateMarketClientRequest.class);
        verify(taxiApi).v1MarketClientCreatePost(eq(remoteIp), requestCaptor.capture());

        CreateMarketClientRequest actual = requestCaptor.getValue();
        assertEquals(request.getCity(), actual.getCity());
        assertEquals(request.getCompanyName(), actual.getCompanyName());
        assertEquals(request.getContactName(), actual.getContactName());
        assertEquals(request.getContactPhone(), actual.getContactPhone());
        assertEquals(request.getContactEmail(), actual.getContactEmail());
        assertEquals(request.getEnterpriseName(), actual.getEnterpriseName());
        assertEquals(request.getEnterpriseNameFull(), actual.getEnterpriseNameFull());
        assertEquals(request.getEnterpriseNameShort(), actual.getEnterpriseNameShort());
        assertEquals(request.getLegalForm(), actual.getLegalForm());
        assertEquals(request.getCompanyTin(), actual.getCompanyTin());
        assertEquals(request.getCompanyCio(), actual.getCompanyCio());
        assertEquals(request.getCompanyOgrn(), actual.getCompanyOgrn());
        assertEquals(request.getSignerPosition(), actual.getSignerPosition());
        assertEquals(request.getSignerName(), actual.getSignerName());
        assertEquals(request.getSignerGender(), actual.getSignerGender());
        assertEquals(request.getBankBic(), actual.getBankBic());
        assertEquals(request.getBankAccountNumber(), actual.getBankAccountNumber());
        assertEquals(request.getBankName(), actual.getBankName());
        assertEquals(request.getAutofilledFields(), actual.getAutofilledFields());
        assertEquals(request.getMarketPassportLogin(), actual.getMarketPassportLogin());

        AddressFields actualLegalAddress = actual.getLegalAddressInfo();
        AddressFieldsDto requestLegalAddress = request.getLegalAddressInfo();
        assertEquals(requestLegalAddress.getCity(), actualLegalAddress.getCity());
        assertEquals(requestLegalAddress.getStreet(), actualLegalAddress.getStreet());
        assertEquals(requestLegalAddress.getHouse(), actualLegalAddress.getHouse());
        assertEquals(requestLegalAddress.getPostIndex(), actualLegalAddress.getPostIndex());

        AddressFields actualMailingAddress = actual.getMailingAddressInfo();
        AddressFieldsDto requestMailingAddress = request.getMailingAddressInfo();
        assertEquals(requestMailingAddress.getCity(), actualMailingAddress.getCity());
        assertEquals(requestMailingAddress.getStreet(), actualMailingAddress.getStreet());
        assertEquals(requestMailingAddress.getHouse(), actualMailingAddress.getHouse());
        assertEquals(requestMailingAddress.getPostIndex(), actualMailingAddress.getPostIndex());

        ru.yandex.mj.generated.client.taxi_corp_request_market_offer.model.ClientRegistrationUtm actualUtm =
                actual.getUtm();
        ClientRegistrationUtm requestUtm = request.getUtm();
        assertEquals(requestUtm.getUtmSource(), actualUtm.getUtmSource());
        assertEquals(requestUtm.getUtmMedium(), actualUtm.getUtmMedium());
        assertEquals(requestUtm.getUtmCampaign(), actualUtm.getUtmCampaign());
        assertEquals(requestUtm.getUtmTerm(), actualUtm.getUtmTerm());
        assertEquals(requestUtm.getUtmContent(), actualUtm.getUtmContent());

        assertEquals(request.getEdoOperator().name(), actual.getEdoOperator().name());
    }

    private CreateClientRequestDto makeRequest() {
        return new CreateClientRequestDto()
                .city("City")
                .companyName("Company name")
                .contactName("Contract name")
                .contactPhone("Contact phone")
                .contactEmail("Contact email")
                .enterpriseName("Enterprise name")
                .enterpriseNameFull("Enterprise full name")
                .enterpriseNameShort("Enterprise short name")
                .legalForm("Lagal form")
                .legalAddressInfo(addressInfo("City1", "House1", "PostIndex1", "Street1"))
                .mailingAddressInfo(addressInfo("City2", "House2", "PostIndex2", "Street2"))
                .companyTin("Company tin")
                .companyCio("Company cio")
                .companyOgrn("Company ogrn")
                .companyRegistrationDate(OffsetDateTime.now())
                .signerPosition("Signer position")
                .signerName("Signer name")
                .signerGender("Signer gender")
                .bankBic("Bank bic")
                .bankAccountNumber("Bank account number")
                .bankName("Bank name")
                .edoOperator(EdoProvider.DIADOC)
                .marketPassportLogin("Market passport login")
                .utm(utm("Source", "Medium", "Campaign", "Term", "Content"))
                .autofilledFields(List.of("Field1", "Field2"));
    }

    private AddressFieldsDto addressInfo(String city, String house, String postIndex, String street) {
        return new AddressFieldsDto()
                .city(city)
                .house(house)
                .postIndex(postIndex)
                .street(street);
    }

    private ClientRegistrationUtm utm(String source, String medium, String campaign, String term, String content) {
        return new ClientRegistrationUtm()
                .utmSource(source)
                .utmMedium(medium)
                .utmCampaign(campaign)
                .utmTerm(term)
                .utmContent(content);
    }

    @Test
    public void create_returnsTaxApiResponse() {
        String remoteIp = "remoteIp";
        CreateClientRequestDto request = makeRequest();
        CreateMarketClientResponse taxiResponse = new CreateMarketClientResponse().clientId("clientId");
        doReturn(asCall(taxiResponse)).when(taxiApi).v1MarketClientCreatePost(any(), any());

        CreateClientResponseDto response = proxyService.create(remoteIp, request);

        assertEquals(taxiResponse.getClientId(), response.getClientId());
    }

    private ExecuteCall asCall(CreateMarketClientResponse response) {
        ExecuteCall call = Mockito.mock(ExecuteCall.class);
        Mockito.doReturn(CompletableFuture.completedFuture(response)).when(call).schedule();
        return call;
    }

    @Test
    public void create_handlesParsableApiErrors() throws IOException {
        // Given
        int httpStatusOfResponse = 400;
        String remoteIp = "remoteIp";
        Error parsableApiError = error(httpStatusOfResponse);
        CreateClientRequestDto request = makeRequest();
        CommonRetrofitHttpExecutionException exceptionResponse = exceptionResponse(httpStatusOfResponse);
        ExecuteCall call = asCall(exceptionResponse);
        doReturn(parsableApiError).when(mapper)
                .readValue(exceptionResponse.getErrorBody(), Error.class);
        doReturn(call).when(taxiApi).v1MarketClientCreatePost(any(), any());

        // When
        ClientProxyService.ClientProxyException thrownException =
                Assertions.assertThrows(ClientProxyService.ClientProxyException.class,
                        () -> proxyService.create(remoteIp, request));

        // Then
        ErrorCreateClientDto expectedError = expectedError(httpStatusOfResponse);
        assertEquals(httpStatusOfResponse, thrownException.getHttpCode());
        ErrorCreateClientDto actualError = thrownException.getError();
        assertEqual(expectedError, actualError);
    }

    private ExecuteCall asCall(CommonRetrofitHttpExecutionException exception) {
        ExecuteCall call = Mockito.mock(ExecuteCall.class);
        Mockito.doReturn(CompletableFuture.failedFuture(exception)).when(call).schedule();
        return call;
    }

    private CommonRetrofitHttpExecutionException exceptionResponse(int httpStatus) {
        CommonRetrofitHttpExecutionException exception = new CommonRetrofitHttpExecutionException(
                "Error message",
                httpStatus,
                new RuntimeException("Generic exception"),
                "Possible error body"
        );
        return exception;
    }

    private Error error(int httpStatus) {
        return new Error()
                .code(String.valueOf(httpStatus))
                .message("An API error")
                .details(errorDetails());
    }

    private ErrorDetails errorDetails() {
        return new ErrorDetails()
                .reason("A reason of API error")
                .addFieldsItem(fieldError());
    }

    private FieldError fieldError() {
        return new FieldError()
                .field("A field name")
                .addMessagesItem("A message for the field");
    }

    private ErrorCreateClientDto expectedError(int httpStatus) {
        return new ErrorCreateClientDto()
                .code(String.valueOf(httpStatus))
                .message("An API error")
                .details(expectedErrorDetails());
    }

    private ErrorCreateClientDtoDetails expectedErrorDetails() {
        return new ErrorCreateClientDtoDetails()
                .reason("A reason of API error")
                .addFieldsItem(expectedFieldError());
    }

    private ErrorCreateClientFieldDto expectedFieldError() {
        return new ErrorCreateClientFieldDto()
                .field("A field name")
                .addMessagesItem("A message for the field");
    }

    private void assertEqual(ErrorCreateClientDto expectedError, ErrorCreateClientDto actualError) {
        assertEquals(expectedError.getCode(), actualError.getCode());
        assertEquals(expectedError.getMessage(), actualError.getMessage());
        assertEquals(expectedError.getDetails().getReason(), actualError.getDetails().getReason());
        assertEquals(expectedError.getDetails().getFields().size(),
                actualError.getDetails().getFields().size());
        assertEquals(expectedError.getDetails().getFields().get(0).getField(),
                actualError.getDetails().getFields().get(0).getField());
        assertEquals(expectedError.getDetails().getFields().get(0).getMessages(),
                actualError.getDetails().getFields().get(0).getMessages());
    }

    @Test
    public void create_handlesUnexpectedExceptions() {
        // Given
        String remoteIp = "remoteIp";
        CreateClientRequestDto request = makeRequest();
        ExecuteCall call = callWithUnknownException();
        doReturn(call).when(taxiApi).v1MarketClientCreatePost(any(), any());

        // When + Then
        Assertions.assertThrows(InternalException.class, () -> proxyService.create(remoteIp, request));

    }

    private ExecuteCall callWithUnknownException() {
        ExecuteCall call = Mockito.mock(ExecuteCall.class);
        Mockito.doReturn(CompletableFuture.failedFuture(new RuntimeException("Unexpected exception"))).when(call).schedule();
        return call;
    }

    @Test
    public void create_handlesMapperErrors() throws IOException {
        // Given
        String remoteIp = "remoteIp";
        CreateClientRequestDto request = makeRequest();
        CommonRetrofitHttpExecutionException exceptionResponse = exceptionResponse(400);
        ExecuteCall call = asCall(exceptionResponse);
        doThrow(new IllegalStateException("Parse error")).when(mapper).readValue(any(String.class), any(Class.class));
        doReturn(call).when(taxiApi).v1MarketClientCreatePost(any(), any());

        // When + Then
        Assertions.assertThrows(InternalException.class, () -> proxyService.create(remoteIp, request));
    }
}
