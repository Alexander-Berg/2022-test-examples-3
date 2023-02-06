package ru.yandex.market.global.checkout.api;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.global.checkout.BaseApiTest;
import ru.yandex.market.global.checkout.api.exception.NotFoundException;
import ru.yandex.market.global.checkout.factory.TestReferralFactory;
import ru.yandex.market.global.db.jooq.tables.pojos.Referral;
import ru.yandex.mj.generated.server.model.PromoDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static ru.yandex.market.global.common.test.TestUtil.createCheckedUserTicket;
import static ru.yandex.market.global.common.test.TestUtil.mockRequestAttributes;
import static ru.yandex.market.starter.tvm.filters.UserTicketFilter.CHECKED_USER_TICKET_ATTRIBUTE;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class ReferralApiServiceTest extends BaseApiTest {
    private static final long UID = 1L;

    private final ReferralApiService referralApiService;
    private final TestReferralFactory referralFactory;

    @Test
    void testGetExistingReferral() {
        Referral referral = referralFactory.createReferral();
        assertThat(referralApiService.apiV1ReferralReferralIdGet(referral.getId()).getBody())
                .usingRecursiveComparison()
                .isEqualTo(referral);
    }

    @Test
    void testGetNonExistingReferral() {
        assertThatThrownBy(() -> referralApiService.apiV1ReferralReferralIdGet(999L))
                .isExactlyInstanceOf(NotFoundException.class);
    }

    @Test
    void testGetExistingReferralByName() {
        Referral referral = referralFactory.createReferral();
        assertThat(referralApiService.apiV1ReferralGet(referral.getName()).getBody())
                .usingRecursiveComparison()
                .isEqualTo(referral);
    }

    @Test
    void testGetNonExistingReferralByName() {
        assertThatThrownBy(() -> referralApiService.apiV1ReferralGet("some-non-existing-referral"))
                .isExactlyInstanceOf(NotFoundException.class);
    }


    @Test
    public void testCreatePromoFromTemplateOnce() {
        mockRequestAttributes(Map.of(CHECKED_USER_TICKET_ATTRIBUTE, createCheckedUserTicket(UID)));

        PromoDto promoFirst = referralApiService
                .apiV1ReferralPromoTemplateCreateUserFirstOrderDiscount50Post("ut")
                .getBody();

        PromoDto promoSecond = referralApiService
                .apiV1ReferralPromoTemplateCreateUserFirstOrderDiscount50Post("ut")
                .getBody();

        assertThat(promoSecond)
                .usingRecursiveComparison(RecursiveComparisonConfiguration.builder()
                        .withComparatorForType(Comparator.comparing(OffsetDateTime::toInstant), OffsetDateTime.class)
                        .build())
                .isEqualTo(promoFirst);
    }


}
