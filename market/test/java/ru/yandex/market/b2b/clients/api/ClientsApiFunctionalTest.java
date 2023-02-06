package ru.yandex.market.b2b.clients.api;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import com.google.gson.Gson;
import net.spy.memcached.MemcachedClientIF;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.util.MultiValueMap;

import ru.yandex.market.b2b.balance.model.PersonStructure;
import ru.yandex.market.b2b.balance.service.BalanceService;
import ru.yandex.market.b2b.clients.AbstractFunctionalTest;
import ru.yandex.mj.generated.client.taxi_authproxy.model.CorpClientIdResponse;
import ru.yandex.mj.generated.client.taxi_can_order.model.V1ClientCanOrderStatus;
import ru.yandex.mj.generated.client.taxi_can_order.model.V1ClientsCanOrderResponse;
import ru.yandex.mj.generated.client.taxi_corp_contracts.model.Contract;
import ru.yandex.mj.generated.client.taxi_corp_contracts.model.ContractsResponse;
import ru.yandex.mj.generated.client.taxi_corp_request_market_offer.model.CreateMarketClientResponse;
import ru.yandex.mj.generated.client.taxi_corp_request_market_offer.model.Error;
import ru.yandex.mj.generated.client.taxi_corp_request_market_offer.model.ErrorDetails;
import ru.yandex.mj.generated.client.taxi_corp_request_market_offer.model.FieldError;
import ru.yandex.mj.generated.client.taxi_corp_request_market_offer.model.MarketClientStatusResponse;
import ru.yandex.mj.generated.client.uzedo.model.InviteResponseDto;
import ru.yandex.mj.generated.server.model.AddressFieldsDto;
import ru.yandex.mj.generated.server.model.CheckResponseDto;
import ru.yandex.mj.generated.server.model.ClientRegistrationUtm;
import ru.yandex.mj.generated.server.model.CreateClientRequestDto;
import ru.yandex.mj.generated.server.model.CreateClientResponseDto;
import ru.yandex.mj.generated.server.model.EdoProvider;
import ru.yandex.mj.generated.server.model.EdoStatus;
import ru.yandex.mj.generated.server.model.ErrorCreateClientDto;
import ru.yandex.mj.generated.server.model.ErrorCreateClientDtoDetails;
import ru.yandex.mj.generated.server.model.ErrorCreateClientFieldDto;
import ru.yandex.mj.generated.server.model.RegistrationStatus;
import ru.yandex.mj.generated.server.model.RegistrationStatusDto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;
import static ru.yandex.mj.generated.client.uzedo.model.InviteStatus.FRIENDS;

@Disabled
@ExtendWith(SpringExtension.class)
public class ClientsApiFunctionalTest extends AbstractFunctionalTest {


    @Autowired
    private WireMockServer mockServer;

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private Gson gsonMapper;

    @Autowired
    private ObjectMapper jacksonMapper;

    /**
     * Mocked to not fail on cache calls
     */
    @MockBean
    private MemcachedClientIF memcachedClient;

    /**
     * Mocked to not set up RPC calls
     */
    @MockBean
    private BalanceService balanceService;

    @AfterEach
    public void afterEach() {
        mockServer.resetAll();
    }

    @Test
    public void createClientReturnsOkResponse() throws Exception {
        // Given
        CreateMarketClientResponse apiResponse = createApiResponse("client-id");
        stubOkResponse(apiResponse);

        CreateClientRequestDto requestDto = makeRequest();
        MultiValueMap header = new HttpHeaders();
        header.add("X-Remote-Ip", "Some IP");
        header.add("Content-Type", "application/json");
        HttpEntity request = new HttpEntity(jacksonMapper.writeValueAsString(requestDto), header);

        // When
        CreateClientResponseDto responseDto = restTemplate.postForObject(
                "http://localhost:" + port + "/v1/market-client/create",
                request,
                CreateClientResponseDto.class);

        // Then
        assertEquals(apiResponse.getClientId(), responseDto.getClientId());
        verifyRequestBody(requestDto);
    }

    private void stubOkResponse(CreateMarketClientResponse response) throws Exception {
        mockServer.stubFor(WireMock.post("/v1/market-client/create")
                .withHeader("Content-Type", WireMock.containing("application/json"))
                .willReturn(WireMock.ok()
                        .withHeader("Content-Type", "application/json")
                        .withBody(gsonMapper.toJson(response))));
    }

    private CreateMarketClientResponse createApiResponse(String clientId) {
        return new CreateMarketClientResponse().clientId(clientId);
    }

    private void verifyRequestBody(CreateClientRequestDto requestDto) throws Exception {
        LoggedRequest postRequest = mockServer.getAllServeEvents().get(0).getRequest();
        String jsonBody = new String(postRequest.getBody());
        Map mapBody = gsonMapper.getAdapter(Map.class).fromJson(jsonBody);
        assertEquals(requestDto.getCity(), mapBody.get("city"));
        assertEquals(requestDto.getCompanyName(), mapBody.get("company_name"));
        assertEquals(requestDto.getContactName(), mapBody.get("contact_name"));
        assertEquals(requestDto.getContactPhone(), mapBody.get("contact_phone"));
        assertEquals(requestDto.getContactEmail(), mapBody.get("contact_email"));
        assertEquals(requestDto.getEnterpriseName(), mapBody.get("enterprise_name"));
        assertEquals(requestDto.getEnterpriseNameFull(), mapBody.get("enterprise_name_full"));
        assertEquals(requestDto.getEnterpriseNameShort(), mapBody.get("enterprise_name_short"));
        assertEquals(requestDto.getLegalForm(), mapBody.get("legal_form"));
        assertEquals(requestDto.getCompanyTin(), mapBody.get("company_tin"));
        assertEquals(requestDto.getCompanyCio(), mapBody.get("company_cio"));
        assertEquals(requestDto.getCompanyOgrn(), mapBody.get("company_ogrn"));
        assertEquals(requestDto.getSignerPosition(), mapBody.get("signer_position"));
        assertEquals(requestDto.getSignerName(), mapBody.get("signer_name"));
        assertEquals(requestDto.getSignerGender(), mapBody.get("signer_gender"));
        assertEquals(requestDto.getBankBic(), mapBody.get("bank_bic"));
        assertEquals(requestDto.getBankAccountNumber(), mapBody.get("bank_account_number"));
        assertEquals(requestDto.getBankName(), mapBody.get("bank_name"));
        assertEquals(requestDto.getAutofilledFields(), mapBody.get("autofilled_fields"));
        assertEquals(requestDto.getEdoOperator().getValue().toLowerCase(), mapBody.get("edo_operator"));
        assertEquals(requestDto.getMarketPassportLogin(), mapBody.get("market_passport_login"));

        Map legalAddressInfo = (Map) mapBody.get("legal_address_info");
        assertEquals(requestDto.getLegalAddressInfo().getCity(), legalAddressInfo.get("city"));
        assertEquals(requestDto.getLegalAddressInfo().getStreet(), legalAddressInfo.get("street"));
        assertEquals(requestDto.getLegalAddressInfo().getHouse(), legalAddressInfo.get("house"));
        assertEquals(requestDto.getLegalAddressInfo().getPostIndex(), legalAddressInfo.get("post_index"));

        Map mailingAddressInfo = (Map) mapBody.get("mailing_address_info");
        assertEquals(requestDto.getMailingAddressInfo().getCity(), mailingAddressInfo.get("city"));
        assertEquals(requestDto.getMailingAddressInfo().getStreet(), mailingAddressInfo.get("street"));
        assertEquals(requestDto.getMailingAddressInfo().getHouse(), mailingAddressInfo.get("house"));
        assertEquals(requestDto.getMailingAddressInfo().getPostIndex(), mailingAddressInfo.get("post_index"));

        Map utm = (Map) mapBody.get("utm");
        assertEquals(requestDto.getUtm().getUtmSource(), utm.get("utm_source"));
        assertEquals(requestDto.getUtm().getUtmCampaign(), utm.get("utm_campaign"));
        assertEquals(requestDto.getUtm().getUtmMedium(), utm.get("utm_medium"));
        assertEquals(requestDto.getUtm().getUtmTerm(), utm.get("utm_term"));
        assertEquals(requestDto.getUtm().getUtmContent(), utm.get("utm_content"));
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
                .signerPosition("Signer position")
                .signerName("Signer name")
                .signerGender("Signer gender")
                .bankBic("Bank bic")
                .bankAccountNumber("Bank account number")
                .bankName("Bank name")
                .companyRegistrationDate(OffsetDateTime.now())
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

    /**
     * Значения установлены не только в поля ({@link ClientRegistrationUtm#setUtmSource(String)} и т.д.), так как при
     * десериализации/сериализации с ClientRegistrationUtm объодятся как с Map, а не как с объектом.
     * <p>
     * Чуть-чуть подробнее:
     * https://github.com/swagger-api/swagger-codegen/issues/11619
     * https://stackoverflow.com/questions/47850441/openapi-swagger-codegen-additionnalproperties-extends-hashmap
     * -playjackson-de
     */
    private ClientRegistrationUtm utm(String source, String medium, String campaign, String term, String content) {
        ClientRegistrationUtm utm = new ClientRegistrationUtm()
                .utmSource(source)
                .utmMedium(medium)
                .utmCampaign(campaign)
                .utmTerm(term)
                .utmContent(content);

        return utm;
    }

    @Test
    public void createClientReturnsErrorResponse() throws Exception {
        // Given
        Error apiError = createError(400);
        stub400Response(apiError);
        MultiValueMap header = new HttpHeaders();
        header.add("X-Remote-Ip", "Some IP");
        header.add("Content-Type", "application/json");
        HttpEntity request = new HttpEntity(makeRequest(), header);

        // When
        ErrorCreateClientDto errorResponse = restTemplate.postForObject(
                "http://localhost:" + port + "/v1/market-client/create",
                request,
                ErrorCreateClientDto.class);

        // Then
        assertEqual(expectedError(400), errorResponse);
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

    private void stub400Response(Error error) throws Exception {
        mockServer.stubFor(WireMock.post("/v1/market-client/create")
                .withHeader("Content-Type", WireMock.containing("application/json"))
                .willReturn(WireMock.badRequest()
                        .withHeader("Content-Type", "application/json")
                        .withBody(gsonMapper.toJson(error))));
    }

    private Error createError(int httpStatus) {
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

    @Test
    public void checkCustomer() {
        // Given
        String uid = "client_uid";
        String id = "1234567890";

        String corpId = "corp_id";

        stubAuthProxy(uid, corpId);
        stubCanOrderProxy(corpId, true);
        setUpContracts(corpId, contract(1, true, id, List.of("market")));
        setUpStatusProxy(corpId, MarketClientStatusResponse.StatusEnum.SUCCESS);
        mockCustomer(id, person("inn", "kpp"));
        setUpEnabledEdo("inn", "kpp");

        // When
        CheckResponseDto responseDto = restTemplate.getForObject(
                "http://localhost:" + port + "/users/" + uid + "/customers/" + id + "/check",
                CheckResponseDto.class);

        // Then
        Assertions.assertEquals(uid, responseDto.getUid());
        Assertions.assertTrue(responseDto.getRegistered());
        Assertions.assertTrue(responseDto.getHasCustomers());
        Assertions.assertTrue(responseDto.getHasContract());
        Assertions.assertTrue(responseDto.getCanOrder());
        Assertions.assertTrue(responseDto.getHasEDO());
        Assertions.assertEquals(
                new RegistrationStatusDto().status(RegistrationStatus.SUCCESS),
                responseDto.getRegistrationStatus());
    }

    private void stubAuthProxy(String uid, String corpId) {
        mockServer.stubFor(WireMock.post("/v1/authproxy/corp_client_id")
                .withHeader("Content-Type", WireMock.containing("application/json"))
                .withRequestBody(WireMock.containing(uid))
                .willReturn(WireMock.ok()
                        .withHeader("Content-Type", "application/json")
                        .withBody(gsonMapper.toJson(new CorpClientIdResponse().corpClientId(corpId)))));
    }

    private void stubCanOrderProxy(String corpId, Boolean canOrder) {
        mockServer.stubFor(WireMock.post("/v1/clients/can_order/market")
                .withHeader("Content-Type", WireMock.containing("application/json"))
                .withRequestBody(WireMock.containing(corpId))
                .willReturn(WireMock.ok()
                        .withHeader("Content-Type", "application/json")
                        .withBody(gsonMapper.toJson(new V1ClientsCanOrderResponse()
                                .addStatusesItem(new V1ClientCanOrderStatus().canOrder(canOrder))))));
    }

    private void setUpContracts(String corpId, Contract contact) {
        mockServer.stubFor(WireMock.get("/v1/contracts?client_id=" + corpId + "&is_active=true")
                .willReturn(WireMock.ok()
                        .withHeader("Content-Type", "application/json")
                        .withBody(gsonMapper.toJson(new ContractsResponse().addContractsItem(contact)))));
    }

    private Contract contract(Integer contactId, Boolean isActive, String billingClientId, List<String> services) {
        return new Contract()
                .contractId(contactId)
                .services(services)
                .isActive(isActive)
                .billingClientId(billingClientId);
    }

    private void setUpStatusProxy(String corpId, MarketClientStatusResponse.StatusEnum status) {
        mockServer.stubFor(WireMock.post("/v1/market-client/status?client_id=" + corpId)
                .willReturn(WireMock.ok()
                        .withHeader("Content-Type", "application/json")
                        .withBody(gsonMapper.toJson(new MarketClientStatusResponse()
                                .status(status)))));
    }

    private void mockCustomer(String id, PersonStructure person) {
        doReturn(List.of(person)).when(balanceService).getClientPersons(Long.parseLong(id),
                PersonStructure.TYPE_GENERAL);
    }

    private PersonStructure person(String inn, String kpp) {
        PersonStructure person = Mockito.mock(PersonStructure.class);
        doReturn(false).when(person).isHidden();
        doReturn(123L).when(person).getPersonId();
        doReturn(inn).when(person).getInn();
        doReturn(kpp).when(person).getKpp();
        return person;
    }

    private void setUpEnabledEdo(String inn, String kpp) {
        mockServer.stubFor(WireMock.get("/client_api/yandex/invite?operatorCode=2BM&inn=" + inn + "&kpp=" + kpp)
                .willReturn(WireMock.ok()
                        .withHeader("Content-Type", "application/json")
                        .withBody(gsonMapper.toJson(new InviteResponseDto()
                                .edoId("edoId")
                                .status(FRIENDS)))
                )
        );
    }
}
