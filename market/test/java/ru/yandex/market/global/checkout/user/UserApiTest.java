package ru.yandex.market.global.checkout.user;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.Map;

import io.github.benas.randombeans.api.EnhancedRandom;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.global.checkout.BaseApiTest;
import ru.yandex.market.global.checkout.api.CartApiService;
import ru.yandex.market.global.checkout.api.UserApiService;
import ru.yandex.market.global.checkout.util.RandomDataGenerator;
import ru.yandex.mj.generated.server.model.CartDto;
import ru.yandex.mj.generated.server.model.PreferredUser;

import static ru.yandex.market.global.common.test.TestUtil.createCheckedUserTicket;
import static ru.yandex.market.global.common.test.TestUtil.mockRequestAttributes;
import static ru.yandex.market.starter.tvm.filters.UserTicketFilter.CHECKED_USER_TICKET_ATTRIBUTE;

@SuppressWarnings("ConstantConditions")
public class UserApiTest extends BaseApiTest {
    private static final EnhancedRandom RANDOM = RandomDataGenerator.dataRandom(UserApiTest.class).build();
    private static final long UID = 10L;
    private static final String YA_TAXI_USERID = "some-ya-taxi-userid";
    private static final String UID_USER_TICKET = "some_user_ticket";

    @Autowired
    private CartApiService cartApiService;

    @Autowired
    private UserApiService userApiService;

    @Test
    public void testMerge() {
        CartDto cart1 = RANDOM.nextObject(CartDto.class);
        CartDto cart2 = RANDOM.nextObject(CartDto.class);

        mockRequestAttributes(Map.of(CHECKED_USER_TICKET_ATTRIBUTE, createCheckedUserTicket(UID)));
        CartDto loggedInCart = cartApiService.apiV1CartUpdatePost(
                UID_USER_TICKET,
                null,
                cart1
        ).getBody();

        mockRequestAttributes(Map.of());
        CartDto loggedOutCart = cartApiService.apiV1CartUpdatePost(
                null,
                YA_TAXI_USERID,
                cart2
        ).getBody();

        mockRequestAttributes(Map.of(CHECKED_USER_TICKET_ATTRIBUTE, createCheckedUserTicket(UID)));
        userApiService.apiV1UserLoginPost(PreferredUser.LOGGEDIN, UID_USER_TICKET, YA_TAXI_USERID);

        mockRequestAttributes(Map.of());
        CartDto mergedCart = cartApiService.apiV1CartGet(
                null,
                YA_TAXI_USERID).getBody();

        Assertions.assertThat(mergedCart)
                .usingComparatorForType(Comparator.comparing(OffsetDateTime::toEpochSecond), OffsetDateTime.class)
                .usingRecursiveComparison()
                .ignoringExpectedNullFields()
                .ignoringCollectionOrder()
                .isEqualTo(loggedInCart);
    }
}
