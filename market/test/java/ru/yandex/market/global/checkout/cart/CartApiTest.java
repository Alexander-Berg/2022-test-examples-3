package ru.yandex.market.global.checkout.cart;

import java.util.List;
import java.util.Map;

import io.github.benas.randombeans.api.EnhancedRandom;
import lombok.RequiredArgsConstructor;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.global.checkout.BaseApiTest;
import ru.yandex.market.global.checkout.api.CartApiService;
import ru.yandex.market.global.checkout.config.properties.CheckoutCommonProperties;
import ru.yandex.market.global.checkout.factory.TestCartFactory;
import ru.yandex.market.global.checkout.util.RandomDataGenerator;
import ru.yandex.mj.generated.server.model.CartClearDto;
import ru.yandex.mj.generated.server.model.CartDto;

import static ru.yandex.market.global.common.test.TestUtil.createCheckedUserTicket;
import static ru.yandex.market.global.common.test.TestUtil.mockRequestAttributes;
import static ru.yandex.market.starter.tvm.filters.UserTicketFilter.CHECKED_USER_TICKET_ATTRIBUTE;

@SuppressWarnings("ConstantConditions")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class CartApiTest extends BaseApiTest {
    private static final EnhancedRandom RANDOM = RandomDataGenerator.dataRandom(CartApiTest.class).build();
    private static final CartDto CART_DTO = RANDOM.nextObject(CartDto.class)
            .createdAt(null)
            .modifiedAt(null)
            .locale("ru-RU")
            .version(1L);
    private static final long BUSINESS_ID = 50L;
    private static final long SHOP_ID = 40L;
    private static final long SHOP_ID_2 = 42L;
    private static final long SHOP_ID_3 = 43L;
    private static final long UID = 10L;
    private static final String YA_TAXI_USERID = "some-ya-taxi-userid";
    private static final String SOME_USER_TICKET = "some_user_ticket";
    private static final RecursiveComparisonConfiguration RECURSIVE_COMPARISON_CONFIGURATION =
            RecursiveComparisonConfiguration.builder()
            .withIgnoreAllExpectedNullFields(true)
            .withIgnoreCollectionOrder(true)
            .build();

    private final CartApiService cartApiService;
    private final TestCartFactory testCartFactory;
    private final CheckoutCommonProperties commonProperties;

    @Test
    public void testGetByUidReturnExistingCart() {
        mockRequestAttributes(Map.of(CHECKED_USER_TICKET_ATTRIBUTE, createCheckedUserTicket(UID)));

        CartDto expected = testCartFactory.createCartDto();
        cartApiService.apiV1CartUpdatePost(SOME_USER_TICKET, YA_TAXI_USERID,
                expected
        );

        CartDto cart = cartApiService.apiV1CartGet(
                SOME_USER_TICKET,
                null
        ).getBody();

        Assertions.assertThat(cart)
                .usingRecursiveComparison(RECURSIVE_COMPARISON_CONFIGURATION)
                .isEqualTo(expected);
    }

    @Test
    public void testGetCartByUidThrowIfNotFound() {
        mockRequestAttributes(Map.of(CHECKED_USER_TICKET_ATTRIBUTE, createCheckedUserTicket(UID)));

        Assertions.assertThatThrownBy(() -> cartApiService.apiV1CartGet(
                SOME_USER_TICKET,
                null
        )).hasMessageContaining("not found");
    }


    @Test
    public void testGetCartByYaTaxiUserid() {
        CartDto expected = testCartFactory.createCartDto();
        cartApiService.apiV1CartUpdatePost(null, YA_TAXI_USERID,
                expected
        );

        CartDto cart = cartApiService.apiV1CartGet(
                null,
                YA_TAXI_USERID
        ).getBody();

        Assertions.assertThat(cart)
                .usingRecursiveComparison()
                .ignoringExpectedNullFields()
                .ignoringCollectionOrder()
                .isEqualTo(expected);
    }


    @Test
    public void testUpdateNewCart() {
        CartDto cart = cartApiService.apiV1CartUpdatePost(null, YA_TAXI_USERID,
                CART_DTO
        ).getBody();

        Assertions.assertThat(cart)
                .usingRecursiveComparison()
                .ignoringExpectedNullFields()
                .ignoringCollectionOrder()
                .isEqualTo(CART_DTO);
    }

    @Test
    public void testUpdateNewCartWithoutLocale() {
        CartDto cartDto = RANDOM.nextObject(CartDto.class)
                .createdAt(null)
                .modifiedAt(null)
                .locale(null)
                .version(1L);

        CartDto cart = cartApiService.apiV1CartUpdatePost(null, YA_TAXI_USERID,
                cartDto
        ).getBody();

        Assertions.assertThat(cart)
                .usingRecursiveComparison()
                .ignoringExpectedNullFields()
                .ignoringCollectionOrder()
                .isEqualTo(cartDto);
        Assertions.assertThat(cart.getLocale()).isEqualTo(commonProperties.getDefaultLocale());
    }

    @Test
    public void testUpdateExistingCart() {
        CartDto cart = cartApiService.apiV1CartUpdatePost(null, YA_TAXI_USERID,
                CART_DTO
        ).getBody();

        CartDto update = RANDOM.nextObject(CartDto.class)
                .version(cart.getVersion() + 1)
                .createdAt(null)
                .modifiedAt(null);
        cart = cartApiService.apiV1CartUpdatePost(
                null,
                YA_TAXI_USERID,
                update
        ).getBody();

        Assertions.assertThat(cart)
                .usingRecursiveComparison()
                .ignoringExpectedNullFields()
                .ignoringFields("version")
                .ignoringCollectionOrder()
                .isEqualTo(update);
    }

    @Test
    public void testUpdateHonorVersion() {
        CartDto cart = cartApiService.apiV1CartUpdatePost(null, YA_TAXI_USERID,
                CART_DTO
        ).getBody();

        CartDto update = RANDOM.nextObject(CartDto.class)
                .version(cart.getVersion())
                .createdAt(null)
                .modifiedAt(null);

        Assertions.assertThatThrownBy(() -> cartApiService.apiV1CartUpdatePost(
                null,
                YA_TAXI_USERID,
                update
        )).hasMessageContaining("changed");
    }

    @Test
    public void testClear() {
        CartDto cart = cartApiService.apiV1CartUpdatePost(
                null,
                YA_TAXI_USERID,
                CART_DTO
        ).getBody();

        CartClearDto clear = RANDOM.nextObject(CartClearDto.class).version(cart.getVersion() + 1);
        CartDto cleared = cartApiService.apiV1CartClearPost(
                null,
                YA_TAXI_USERID,
                clear
        ).getBody();

        Assertions.assertThat(cleared)
                .hasNoNullFieldsOrPropertiesExcept(
                        "id",
                        "version",
                        "items",
                        "createdAt",
                        "modifiedAt",
                        "referralId",
                        "paymentReturnUrl",
                        "plusAction",
                        "plusEarned",
                        "plusSpent",
                        "plusAvailableAmount"
                );

        Assertions.assertThat(cleared.getItems())
                .isEmpty();

        Assertions.assertThat(cleared)
                .usingRecursiveComparison(RecursiveComparisonConfiguration.builder()
                        .withIgnoreAllExpectedNullFields(true)
                        .build()
                )
                .isEqualTo(new CartDto()
                        .deliveryCostForRecipient(0L)
                        .totalCost(0L)
                        .totalItemsCost(0L)
                        .totalItemsCostWithPromo(0L)
                        .promocodes(List.of())
                        .locale(null)
                        .plusAction(null)
                        .plusEarned(null)
                        .plusSpent(null)
                        .plusAvailableAmount(null)
                );
    }

    @Test
    public void testClearHonorVersion() {
        CartDto cart = cartApiService.apiV1CartUpdatePost(null, YA_TAXI_USERID,
                CART_DTO
        ).getBody();

        CartClearDto clear = RANDOM.nextObject(CartClearDto.class).version(cart.getVersion());
        Assertions.assertThatThrownBy(() -> cartApiService.apiV1CartClearPost(
                null,
                YA_TAXI_USERID,
                clear
        )).hasMessageContaining("changed");
    }
}
