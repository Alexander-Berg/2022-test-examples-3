package ru.yandex.market.api.partner.request;

import java.security.AccessControlException;
import java.util.stream.Stream;

import javax.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.api.partner.context.FunctionalTest;
import ru.yandex.market.api.resource.ApiResource;
import ru.yandex.market.api.resource.ApiResourceAccessLevel;

import static org.mockito.Mockito.mock;

/**
 * Тест проверяет корректность проверки уровней доступа запроса и метода.
 * Если уровень доступа запроса ниже чем уровень доступа метода, то запрос отклоняется.
 */
public class RwAccessInterceptorTest extends FunctionalTest {

    private RwAccessInterceptor interceptor;

    public static Stream<Arguments> accessLevels() {
        return Stream.of(
                Arguments.of(ApiResourceAccessLevel.READ_WRITE, ApiResourceAccessLevel.READ_WRITE, true),
                Arguments.of(ApiResourceAccessLevel.READ_ONLY, ApiResourceAccessLevel.READ_WRITE, false),
                Arguments.of(ApiResourceAccessLevel.READ_WRITE, ApiResourceAccessLevel.READ_ONLY, true),
                Arguments.of(ApiResourceAccessLevel.READ_WRITE, ApiResourceAccessLevel.READ_WRITE, true)
        );
    }

    @BeforeEach
    void setUp() {
        interceptor = new RwAccessInterceptor();
    }

    @ParameterizedTest
    @MethodSource("accessLevels")
    void checkRwAccess(ApiResourceAccessLevel requestAL, ApiResourceAccessLevel resourceAL, boolean expected)
            throws Exception
    {
        HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);
        PartnerServletRequest req = new PartnerServletRequest(httpServletRequest, 1000);
        req.setApiResource(new ApiResource(1, 1, "/endpoint/*/*", "GET", null, resourceAL));
        req.setAccessLevel(requestAL);
        if (expected) {
            Assertions.assertEquals(interceptor.preHandle(req, null, null), expected);
        } else {
            Assertions.assertThrows(AccessControlException.class, () -> interceptor.preHandle(req, null, null));
        }
    }
}
