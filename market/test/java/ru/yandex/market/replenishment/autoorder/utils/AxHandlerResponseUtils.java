package ru.yandex.market.replenishment.autoorder.utils;

import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.replenishment.autoorder.model.AxHandlerResponse;

public class AxHandlerResponseUtils {
    private AxHandlerResponseUtils() {
    }

    public static ResponseEntity<?> getMockedResponseEntity(String responseId) {
        return getMockedResponseEntity(responseId, null);
    }

    public static ResponseEntity<?> getMockedResponseEntity(String responseId, String error) {
        ResponseEntity<?> response = Mockito.mock(ResponseEntity.class);
        Mockito.doReturn(HttpStatus.OK).when(response).getStatusCode();
        Mockito.doReturn(new AxHandlerResponse(error == null, responseId, error)).when(response).getBody();
        return response;
    }
}
