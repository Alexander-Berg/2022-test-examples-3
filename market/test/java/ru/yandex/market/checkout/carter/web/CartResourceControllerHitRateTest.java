package ru.yandex.market.checkout.carter.web;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.concurrent.ThreadLocalRandom;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.market.checkout.carter.CarterMockedDbTestBase;
import ru.yandex.market.checkout.carter.client.Carter;
import ru.yandex.market.checkout.carter.limit.UserCarterRateLimitsChecker;
import ru.yandex.market.checkout.carter.model.Color;
import ru.yandex.market.checkout.carter.model.OwnerKey;
import ru.yandex.market.checkout.carter.report.ReportMockConfigurer;
import ru.yandex.market.checkout.carter.util.converter.CartConverter;
import ru.yandex.market.checkout.carter.utils.CarterTestUtils;
import ru.yandex.market.checkout.carter.utils.serialization.TestSerializationService;
import ru.yandex.market.checkout.checkouter.order.UserGroup;
import ru.yandex.market.checkout.common.ratelimit.RateLimitCheckerHelper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.checkout.carter.model.UserIdType.UID;
import static ru.yandex.market.checkout.carter.web.CarterWebParam.PARAM_RGB;
import static ru.yandex.market.checkout.carter.web.CarterWebParam.PARAM_USER_GROUP;

public class CartResourceControllerHitRateTest extends CarterMockedDbTestBase {

    private static final int COUNT = 2;

    @Autowired
    private Carter carterClient;
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private TestSerializationService testSerializationService;
    @Autowired
    private UserCarterRateLimitsChecker userCarterRateLimitsChecker;
    @Autowired
    private ReportMockConfigurer reportMockConfigurer;

    private UserContext uidContext;

    @BeforeEach
    public void setUp() {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        uidContext = UserContext.of(OwnerKey.of(Color.BLUE, UID, "" + rnd.nextLong(1, Long.MAX_VALUE)));
    }

    @AfterEach
    public void clear() {
        reportMockConfigurer.resetMock();
    }

    @Test
    @Disabled
    public void shouldFailsOnHitRateLimit() throws Exception {
        Clock fixed = Clock.fixed(Instant.now(), ZoneId.systemDefault());

        try {
            userCarterRateLimitsChecker.setClock(fixed);

            Cart cart = CartConverter.convert(carterClient.getCart(uidContext.getUserAnyId(), UID, Color.BLUE));
            int size = cart.getBasketList().getItems().size();

            int requestLimit = 100;
            for (int i = 0; i < requestLimit; ++i) {
                String body = serializeCartList(CartConverter.convert(CarterTestUtils.generateCartList(COUNT)));

                mockMvc.perform(
                        post("/cart/UID/" + uidContext.getUserAnyId() + "/list/-1/items")
                                .param("rgb", "BLUE")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                        .andExpect(status().is2xxSuccessful());
                cart = CartConverter.convert(carterClient.getCart(uidContext.getUserAnyId(), UID, Color.BLUE));
                assertEquals(cart.getBasketList().getItems().size(), size + COUNT);
            }

            RateLimitCheckerHelper.awaitEmptyQueue(userCarterRateLimitsChecker.getExecutorService());

            String body = serializeCartList(CartConverter.convert(CarterTestUtils.generateCartList(COUNT)));
            mockMvc.perform(
                    post("/cart/UID/" + uidContext.getUserAnyId() + "/list/-1/items")
                            .param("rgb", "BLUE")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().is4xxClientError());
        } finally {
            userCarterRateLimitsChecker.setClock(Clock.systemDefaultZone());
        }
    }

    @Test
    public void shouldReturns2xxWhenHitLimitIsUnlimit() throws Exception {
        Cart cart = CartConverter.convert(carterClient.getCart(uidContext.getUserAnyId(), UID, Color.BLUE));
        int size = cart.getBasketList().getItems().size();

        int requestLimit = 100;
        for (int i = 0; i < requestLimit; ++i) {
            String body = serializeCartList(CartConverter.convert(CarterTestUtils.generateCartList(COUNT)));

            mockMvc.perform(
                    post("/cart/UID/" + uidContext.getUserAnyId() + "/list/-1/items")
                            .contentType(MediaType.APPLICATION_JSON)
                            .param("rgb", "BLUE")
                            .param(PARAM_USER_GROUP, UserGroup.ABO.name())
                            .content(body))
                    .andExpect(status().is2xxSuccessful());
            cart = CartConverter.convert(carterClient.getCart(uidContext.getUserAnyId(), UID, Color.BLUE));
            assertEquals(cart.getBasketList().getItems().size(), size + COUNT);
        }

        String body = serializeCartList(CartConverter.convert(CarterTestUtils.generateCartList(COUNT)));
        mockMvc.perform(
                post("/cart/UID/" + uidContext.getUserAnyId() + "/list/-1/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .param(PARAM_USER_GROUP, UserGroup.ABO.name())
                        .param(PARAM_RGB, Color.BLUE.name())
                        .content(body))
                .andExpect(status().is2xxSuccessful());
    }

    private String serializeCartList(CartListViewModel cartList) throws IOException {
        return testSerializationService.serializeCarterObject(cartList);
    }

}
