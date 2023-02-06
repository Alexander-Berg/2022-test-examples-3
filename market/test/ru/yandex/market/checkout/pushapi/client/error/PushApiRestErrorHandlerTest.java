package ru.yandex.market.checkout.pushapi.client.error;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.client.MockClientHttpResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.checkout.common.rest.ErrorCodeException;

@ExtendWith(SpringExtension.class)
@ContextConfiguration({"classpath:clientTests.xml", "classpath:WEB-INF/push-api-client.xml"})
public class PushApiRestErrorHandlerTest {

    @Autowired
    private PushApiRestErrorHandler handler;

    @Test
    public void testThrowsShopErrorExceptionIfCodeIs502() {
        Assertions.assertThrows(ShopErrorException.class, () -> {
            final MockClientHttpResponse response = new MockClientHttpResponse(
                    ("<error>" +
                            "    <code>HTTP</code>" +
                            " <shop-admin>false</shop-admin>" +
                            "</error>").getBytes(),
                    HttpStatus.UNPROCESSABLE_ENTITY
            );
            handler.handleError(response);
        });
    }

    @Test
    public void testThrowsErrorCodeExceptionIfCodeIsNot502() {
        Assertions.assertThrows(ErrorCodeException.class, () -> {
            final MockClientHttpResponse response = new MockClientHttpResponse(
                    ("<error>" +
                            "    <code>UNKNOWN</code>" +
                            " <shop-admin>false</shop-admin>" +
                            "</error>").getBytes(),
                    HttpStatus.BAD_REQUEST
            );
            handler.handleError(response);
        });
    }

    @Test
    public void testThrowsErrorCodeExceptionIfCodeIs502AndCantParseShopErrorException() {
        Assertions.assertThrows(ErrorCodeException.class, () -> {
            final MockClientHttpResponse response = new MockClientHttpResponse(
                    ("<error>" +
                            "    <code>BAD_CODE</code>" +
                            " <shop-admin>false</shop-admin>" +
                            "</error>").getBytes(),
                    HttpStatus.UNPROCESSABLE_ENTITY
            );
            handler.handleError(response);
        });
    }
}
