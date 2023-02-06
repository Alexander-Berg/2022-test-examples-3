package ru.yandex.market.api.partner.auth.impl.tvm2;

import java.util.function.Function;

import javax.servlet.http.HttpServletRequest;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Tags;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.api.partner.auth.AuthPrincipal;
import ru.yandex.market.api.partner.auth.UnauthorizedException;
import ru.yandex.market.api.partner.request.PartnerServletRequest;
import ru.yandex.market.api.resource.ApiLimitType;
import ru.yandex.market.tags.Components;
import ru.yandex.market.tags.Tests;
import ru.yandex.passport.tvmauth.TicketStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Tags({
        @Tag(Components.MBI_PARTNER_API),
        @Tag(Tests.COMPONENT)
})
public class Tvm2AuthorizationServiceTest {

    private static final String TEST_TICKET_STRING = "test-ticket";
    private static final long TEST_USER_ID = 1502361976489L;

    private static final int TEST_PRICE_LABS1_TVM_CLIENT_ID = 354531;
    private static final int TEST_PRICE_LABS2_TVM_CLIENT_ID = 354534;
    private static final int TEST_UNKNOWN_CLIENT_ID = 98739847;

    private Tvm2AuthorizationService tvmAuthorizationService;
    private Function<String, Tvm2CheckResult> check;
    private HttpServletRequest httpRequest;
    private PartnerServletRequest request;

    private static Object[][] knownUsersAndTypes() {
        return new Object[][]{
                {TEST_PRICE_LABS1_TVM_CLIENT_ID, ApiLimitType.PRICE_LABS},
                {TEST_PRICE_LABS2_TVM_CLIENT_ID, ApiLimitType.PRICE_LABS_V2}};
    }

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        check = mock(Function.class);

        tvmAuthorizationService = new Tvm2AuthorizationService(
                check,
                ImmutableMap.<Integer, ApiLimitType>builder()
                        .put(TEST_PRICE_LABS1_TVM_CLIENT_ID, ApiLimitType.PRICE_LABS)
                        .put(TEST_PRICE_LABS2_TVM_CLIENT_ID, ApiLimitType.PRICE_LABS_V2)
                        .build(),
                123
        );

        httpRequest = mock(HttpServletRequest.class);
        request = new PartnerServletRequest(httpRequest, 1000);
    }

    @Test
    @DisplayName("Проверка, что clientId, которого нет в списке доверенных проваливает аутентификацию")
    void testAuthenticationFailureForUnknownClientId() {
        setUpTestTicket(TEST_UNKNOWN_CLIENT_ID, TicketStatus.OK);
        Assertions.assertThrows(UnauthorizedException.class, () -> tvmAuthorizationService.authorizeUser(request));
    }

    @ParameterizedTest
    @MethodSource("knownUsersAndTypes")
    @DisplayName("Проверка, что некорректный TVM2 тикет проваливает аутентификацию")
    void testAuthenticationFailureForInvalidTicket(int clientId) {
        setUpTestTicket(clientId, TicketStatus.EXPIRED);
        Assertions.assertThrows(UnauthorizedException.class, () -> tvmAuthorizationService.authorizeUser(request));
    }

    @ParameterizedTest
    @MethodSource("knownUsersAndTypes")
    @DisplayName("Проверка, что отсутствие UserId проваливает аутентификацию")
    void testAuthenticationFailureForAbsentUserId(int clientId) {
        setUpTestTicket(null, clientId, TicketStatus.EXPIRED);
        Assertions.assertThrows(UnauthorizedException.class, () -> tvmAuthorizationService.authorizeUser(request));
    }

    @ParameterizedTest
    @MethodSource("knownUsersAndTypes")
    @DisplayName("Проверка, что некорректный UserId проваливает аутентификацию")
    void testAuthenticationFailureForInvalidUserId(int clientId) {
        setUpTestTicket("User100", clientId, TicketStatus.EXPIRED);
        Assertions.assertThrows(UnauthorizedException.class, () -> tvmAuthorizationService.authorizeUser(request));
    }

    @ParameterizedTest
    @MethodSource("knownUsersAndTypes")
    @DisplayName("Проверка, что запрос помечается, как запрос от Price Labs в случае, если передан clientId Price Labs")
    void testMarkRequestFromPriceLabs(int clientId, ApiLimitType apiLimitType) {
        setUpTestTicket(clientId, TicketStatus.OK);

        AuthPrincipal authPrincipal = tvmAuthorizationService.authorizeUser(request);

        assertEquals(apiLimitType, request.getApiLimitType());
        assertEquals(TEST_USER_ID, authPrincipal.getUid());
    }

    private void setUpTestTicket(int clientId, TicketStatus status) {
        setUpTestTicket(String.valueOf(TEST_USER_ID), clientId, status);
    }

    private void setUpTestTicket(String userId, int clientId, TicketStatus status) {
        when(httpRequest.getHeader(Tvm2AuthorizationService.TVM2_SERVICE_TICKET_HEADER)).thenReturn(TEST_TICKET_STRING);
        when(httpRequest.getHeader(Tvm2AuthorizationService.TVM2_USER_ID_HEADER)).thenReturn(userId);

        when(check.apply(eq(TEST_TICKET_STRING))).thenReturn(new Tvm2CheckResult(status, clientId));
    }
}
