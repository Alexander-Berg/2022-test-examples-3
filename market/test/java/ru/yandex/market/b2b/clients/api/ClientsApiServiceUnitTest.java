package ru.yandex.market.b2b.clients.api;

import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.b2b.clients.ClientProxyService;
import ru.yandex.market.b2b.clients.CustomersStorageService;
import ru.yandex.market.b2b.clients.EdoRoamingService;
import ru.yandex.mj.generated.server.model.CheckResponseDto;
import ru.yandex.mj.generated.server.model.CreateClientRequestDto;
import ru.yandex.mj.generated.server.model.CreateClientResponseDto;
import ru.yandex.mj.generated.server.model.EdoProvider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class ClientsApiServiceUnitTest {

    @Mock
    private ClientProxyService proxyService;

    @Mock
    private CustomersStorageService customersStorageService;

    @Mock
    private EdoRoamingService edoRoamingService;

    @InjectMocks
    private ClientsApiService apiService;

    @BeforeEach
    void setUp() {
        doReturn(CompletableFuture.completedFuture(null)).when(edoRoamingService)
                .asyncSendEdoConnectionIfNeedBy(any(), any(EdoRoamingService.CustomerDto.class));
        doReturn(CompletableFuture.completedFuture(null)).when(edoRoamingService)
                .asyncSendEdoConnectionIfNeedBy(any(), any(String.class));
    }

    @AfterEach
    void tearDown() {
        Mockito.reset(edoRoamingService);
    }

    @Test
    public void createClient_callsProxy() {
        String remoteIp = "remoteApi";
        CreateClientRequestDto requestDto = makeRequest();

        apiService.createClient(remoteIp, requestDto);

        verify(proxyService).create(remoteIp, requestDto);
    }

    private CreateClientRequestDto makeRequest() {
        CreateClientRequestDto request = new CreateClientRequestDto();
        request.setCity("City");
        request.setCompanyName("Company Name");
        request.setMarketPassportLogin("login");
        request.setPassportUid("uid");
        return request;
    }

    private CreateClientRequestDto makeRequestWithEdo(EdoProvider edoProvider) {
        CreateClientRequestDto request = makeRequest();
        request.setCompanyTin("90000000011");
        request.setCompanyCio("9000000001");
        request.setEdoOperator(edoProvider);
        return request;
    }

    @Test
    public void createClient_withEdo() {
        String remoteIp = "remoteApi";
        CreateClientRequestDto requestDto = makeRequestWithEdo(EdoProvider.DIADOC);

        apiService.createClient(remoteIp, requestDto);

        verify(proxyService)
                .create(remoteIp, requestDto);
        verify(edoRoamingService, times(1))
                .asyncSendEdoConnectionIfNeedBy(
                        Mockito.eq(requestDto.getEdoOperator()),
                        Mockito.<EdoRoamingService.CustomerDto>argThat(argument ->
                                argument.getUid().equals(requestDto.getPassportUid()) &&
                                        argument.getPassportLogin().equals(requestDto.getMarketPassportLogin()) &&
                                        argument.getName().equals(requestDto.getCompanyName()) &&
                                        argument.getInn().equals(requestDto.getCompanyTin()) &&
                                        argument.getKpp().equals(requestDto.getCompanyCio())));
    }

    @Test
    public void createClient_withEdoRoaming() {
        String remoteIp = "remoteApi";
        CreateClientRequestDto requestDto = makeRequestWithEdo(EdoProvider.EVATORPLATFORM);

        apiService.createClient(remoteIp, requestDto);

        verify(proxyService)
                .create(remoteIp, requestDto);
        verify(edoRoamingService, times(1))
                .asyncSendEdoConnectionIfNeedBy(
                        Mockito.eq(requestDto.getEdoOperator()),
                        Mockito.<EdoRoamingService.CustomerDto>argThat(argument ->
                                argument.getUid().equals(requestDto.getPassportUid()) &&
                                        argument.getPassportLogin().equals(requestDto.getMarketPassportLogin()) &&
                                        argument.getName().equals(requestDto.getCompanyName()) &&
                                        argument.getInn().equals(requestDto.getCompanyTin()) &&
                                        argument.getKpp().equals(requestDto.getCompanyCio())));
    }

    @Test
    public void createClient_returnsProxyResponse() {
        String remoteIp = "remoteApi";
        CreateClientRequestDto requestDto = makeRequest();
        CreateClientResponseDto responseDto = makeCreateClientResponse();
        doReturn(responseDto).when(proxyService)
                .create(any(String.class), any(CreateClientRequestDto.class));

        ResponseEntity<CreateClientResponseDto> response = apiService.createClient(remoteIp, requestDto);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(responseDto, response.getBody());
    }

    private CreateClientResponseDto makeCreateClientResponse() {
        CreateClientResponseDto response = new CreateClientResponseDto();
        response.setClientId("client id");
        return response;
    }

    @Test
    public void checkUserCustomer_callsStorage() {
        String uid = "uid";
        String id = "id";

        apiService.checkUserCustomer(uid, id);

        verify(customersStorageService).checkCustomer(uid, id);
    }

    @Test
    public void checkUserCustomer_returnsStorageResponse() {
        String uid = "uid";
        String id = "id";
        CheckResponseDto responseDto = makeCheckCustomerResponse();
        doReturn(responseDto).when(customersStorageService)
                .checkCustomer(anyString(), anyString());

        ResponseEntity<CheckResponseDto> response = apiService.checkUserCustomer(uid, id);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(responseDto, response.getBody());
    }

    private CheckResponseDto makeCheckCustomerResponse() {
        CheckResponseDto response = new CheckResponseDto();
        response.setUid("uid");
        return response;
    }
}
