package ru.yandex.market.b2b.clients.api;

import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.b2b.clients.ClientProxyService;
import ru.yandex.market.b2b.clients.common.InternalException;
import ru.yandex.mj.generated.server.model.ErrorCreateClientDto;
import ru.yandex.mj.generated.server.model.ErrorDto;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RestExceptionHandlerUnitTest {

    private RestExceptionHandler exceptionHandler = new RestExceptionHandler();

    @Test
    public void handleInvoiceError() {
        InternalException exception = new InternalException("error message");

        ResponseEntity<ErrorDto> errorResponse = exceptionHandler.handleEntityNotFound(exception);

        assertEquals(HttpStatus.BAD_REQUEST, errorResponse.getStatusCode());
        assertEquals(exception.getMessage(), errorResponse.getBody().getCode());
        assertEquals(Arrays.toString(exception.getStackTrace()), errorResponse.getBody().getMessage());
    }

    @Test
    public void handleCreateClient_400() {
        ClientProxyService.ClientProxyException exception = exception(400);

        ResponseEntity<ErrorCreateClientDto> errorResponse = exceptionHandler.handleCreateClient(exception);

        assertEquals(HttpStatus.BAD_REQUEST, errorResponse.getStatusCode());
        assertEquals(exception.getError(), errorResponse.getBody());
    }

    private ClientProxyService.ClientProxyException exception(int statusCode) {
        ErrorCreateClientDto error = new ErrorCreateClientDto();
        error.setCode(String.valueOf(statusCode));
        error.setMessage("message from proxy API");
        return new ClientProxyService.ClientProxyException("Error", statusCode, "json of error", error);
    }

    @Test
    public void handleCreateClient_409() {
        ClientProxyService.ClientProxyException exception = exception(409);

        ResponseEntity<ErrorCreateClientDto> errorResponse = exceptionHandler.handleCreateClient(exception);

        assertEquals(HttpStatus.CONFLICT, errorResponse.getStatusCode());
        assertEquals(exception.getError(), errorResponse.getBody());
    }

    @Test
    public void handleCreateClient_429() {
        ClientProxyService.ClientProxyException exception = exception(429);

        ResponseEntity<ErrorCreateClientDto> errorResponse = exceptionHandler.handleCreateClient(exception);

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, errorResponse.getStatusCode());
        assertEquals(exception.getError(), errorResponse.getBody());
    }
}
