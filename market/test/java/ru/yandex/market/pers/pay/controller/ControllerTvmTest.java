package ru.yandex.market.pers.pay.controller;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pers.pay.PersPayTest;
import ru.yandex.market.pers.pay.mvc.GradePaymentMvcMocks;
import ru.yandex.market.pers.tvm.TvmChecker;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static ru.yandex.market.pers.tvm.TvmUtils.SERVICE_TICKET_HEADER;

/**
 * @author Ilya Kislitsyn / ilyakis@ / 11.03.2021
 */
public class ControllerTvmTest extends PersPayTest {
    public static final long USER_ID = 9364172;
    public static final long MODEL_ID = 41434;

    @Autowired
    private TvmChecker tvmChecker;
    @Autowired
    private GradePaymentMvcMocks paymentMvc;

    @Test
    public void testTvm() {
        mockPersAuthorAgitations(USER_ID, List.of(MODEL_ID));

        String expectedToken = "token";


        Mockito.doAnswer(invocation -> {
            Object token = invocation.getArgument(0);

            if (token != null && token.equals("token")) {
                return null;
            }
            throw new IllegalArgumentException(String.format("Invalid token: %s", token));
        })
            .when(tvmChecker).checkTvm(any(), anyList());

        // fail without header
        String error = paymentMvc.showPaymentOfferMvcNoTvm(USER_ID, MODEL_ID, error400, x->x);
        Assertions.assertTrue(error.contains("Invalid token: null"));

        // fail with invalid header
        error = paymentMvc.showPaymentOfferMvcNoTvm(USER_ID, MODEL_ID, error400,
            x->x.header(SERVICE_TICKET_HEADER, "invalid"));
        Assertions.assertTrue(error.contains("Invalid token: invalid"));

        // ok
        paymentMvc.showPaymentOfferMvcNoTvm(USER_ID, MODEL_ID, isOk,
            x->x.header(SERVICE_TICKET_HEADER, expectedToken));
    }
}
