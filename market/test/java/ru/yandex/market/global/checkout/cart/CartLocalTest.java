package ru.yandex.market.global.checkout.cart;

import java.util.List;
import java.util.Map;

import io.github.benas.randombeans.api.EnhancedRandom;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.global.checkout.BaseLocalTest;
import ru.yandex.market.global.checkout.api.CartApiService;
import ru.yandex.market.global.checkout.util.RandomDataGenerator;
import ru.yandex.mj.generated.server.model.AddressDto;
import ru.yandex.mj.generated.server.model.CartActualizeDto;
import ru.yandex.mj.generated.server.model.CartActualizeErrorDto;
import ru.yandex.mj.generated.server.model.CartActualizeResultDto;
import ru.yandex.mj.generated.server.model.CartItemActualizeDto;
import ru.yandex.mj.generated.server.model.CartItemDto;

import static ru.yandex.market.global.common.test.TestUtil.createCheckedUserTicket;
import static ru.yandex.market.global.common.test.TestUtil.mockRequestAttributes;
import static ru.yandex.market.starter.tvm.filters.UserTicketFilter.CHECKED_USER_TICKET_ATTRIBUTE;

@Disabled
public class CartLocalTest extends BaseLocalTest {
    private static final EnhancedRandom RANDOM = RandomDataGenerator.dataRandom(CartLocalTest.class).build();
    private static final AddressDto RECIPIENT_ADDRESS = RANDOM.nextObject(AddressDto.class);

    private static final CartItemActualizeDto EXISTING_OFFER_1 = new CartItemActualizeDto()
            .businessId(11308928L)
            .shopId(11308927L)
            .offerId("45572")
            .count(2L);

    private static final CartItemActualizeDto EXISTING_OFFER_2 = new CartItemActualizeDto()
            .businessId(11308928L)
            .shopId(11308927L)
            .offerId("69036")
            .count(1L);

    private static final CartItemActualizeDto UNEXISTING_OFFER = new CartItemActualizeDto()
            .businessId(11308928L)
            .shopId(11308927L)
            .offerId("Should be not found")
            .count(1L);

    private static final CartActualizeDto CART_ACTUALIZE_DTO = RANDOM.nextObject(CartActualizeDto.class)
            .businessId(11308928L)
            .shopId(11308927L)
            .recipientAddress(RECIPIENT_ADDRESS)
            .items(List.of(EXISTING_OFFER_1, EXISTING_OFFER_2, UNEXISTING_OFFER));

    private static final String USER_TICKET = "CartIntegrationTest-user-ticket";
    private static final String YA_TAXI_USER_ID = "CartIntegrationTest-taxi-user";
    private static final long UID = 1234567890;

    @Autowired
    public CartApiService cartApiService;

    @Test
    public void testActualize() {
        mockRequestAttributes(
                Map.of(CHECKED_USER_TICKET_ATTRIBUTE, createCheckedUserTicket(UID))
        );

        CartActualizeResultDto cartActualizeResultDto = cartApiService.apiV1CartActualizePost(
                USER_TICKET, YA_TAXI_USER_ID, CART_ACTUALIZE_DTO
        ).getBody();

        Assertions.assertThat(cartActualizeResultDto.getCart().getItems())
                .map(CartItemDto::getOfferId)
                .contains(EXISTING_OFFER_1.getOfferId(), EXISTING_OFFER_2.getOfferId());

        Assertions.assertThat(cartActualizeResultDto.getErrors())
                .map(CartActualizeErrorDto::getOfferId)
                .containsExactlyInAnyOrder(UNEXISTING_OFFER.getOfferId());

        Assertions.assertThat(cartActualizeResultDto.getCart().getRecipientAddress())
                .usingRecursiveComparison()
                .isEqualTo(RECIPIENT_ADDRESS);
    }

    @BeforeEach
    public void mockRequest() {
        mockRequestAttributes(Map.of());
    }
}
