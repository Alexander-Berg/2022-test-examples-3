package ru.yandex.market.tpl.core.domain.promo;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.tpl.api.model.ListWrapper;
import ru.yandex.market.tpl.api.model.promo.PromoInfoDto;
import ru.yandex.market.tpl.core.CoreTest;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.user.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;

@CoreTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class PromoServiceTest {

    private static final String UNPUBLISHED_PROMO_ID = "123";
    private static final String PUBLISHED_PROMO_ID = "456";

    private final PromoTestHelper promoTestHelper;
    private final TestUserHelper testUserHelper;
    private final UserRepository userRepository;

    private final PromoService promoService;


    private User courier;

    @BeforeEach
    void init() {
        promoTestHelper.createPromo(UNPUBLISHED_PROMO_ID, PromoStatus.NEW, 1L);
        promoTestHelper.createPromo(PUBLISHED_PROMO_ID, PromoStatus.PUBLISHED, 10L);

        courier = createUser();
    }


    @Test
    void getAvailablePreviewPromos() {
        ListWrapper<PromoInfoDto> availablePromos = promoService.getAvailablePromos(courier, true);

        assertThat(availablePromos.getItems())
                .extracting(PromoInfoDto::getId)
                .containsOnly(UNPUBLISHED_PROMO_ID, PUBLISHED_PROMO_ID);
    }

    @Test
    void getAvailablePublishedPromos() {
        ListWrapper<PromoInfoDto> availablePromos = promoService.getAvailablePromos(courier, false);

        assertThat(availablePromos.getItems())
                .extracting(PromoInfoDto::getId)
                .containsOnly(PUBLISHED_PROMO_ID);
    }

    @Test
    void hideReadedPromo() {
        promoService.markAsRead(PUBLISHED_PROMO_ID, courier);

        ListWrapper<PromoInfoDto> availablePromos = promoService.getAvailablePromos(courier, false);
        assertThat(availablePromos.getItems()).isEmpty();
    }

    @Test
    void getAvailablePromoWithHighestPriority() {
        promoTestHelper.createPromo("789", PromoStatus.PUBLISHED, 100L);

        ListWrapper<PromoInfoDto> availablePromos = promoService.getAvailablePromos(courier, false);

        assertThat(availablePromos.getItems())
                .extracting(PromoInfoDto::getId)
                .containsExactly("789", PUBLISHED_PROMO_ID);
    }

    private User createUser() {
        User user = testUserHelper.createUserWithoutSchedule(1123);

        userRepository.save(user);
        return user;
    }

}
