package ru.yandex.travel.orders.services.promo;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import ru.yandex.travel.commons.proto.ErrorException;
import ru.yandex.travel.orders.entities.promo.PromoAction;
import ru.yandex.travel.orders.entities.promo.PromoCode;
import ru.yandex.travel.orders.entities.promo.PromoCodeActivationsStrategy;

import static org.hamcrest.Matchers.equalTo;

public class PromoCodeCheckerEnsureCanBeActivatedTest {

    private static final String PROMO_CODE = "MY_TEST_CODE_ABC";

    private final PromoCodeChecker checker = new PromoCodeChecker(Clock.systemDefaultZone());

    private PromoCode promoCode;

    @Rule
    public ExpectedException expectedEx = ExpectedException.none();

    @Before
    public void setUp() {
        promoCode = new PromoCode();
        promoCode.setCode(PROMO_CODE);
        promoCode.setPromoAction(new PromoAction());
    }

    @Test
    public void doNotThrowErrorWhenNoError() {
        checker.ensurePromoCodeCanBeActivated(promoCode);
    }

    @Test
    public void throwsErrorForBlacklisted() {
        expectedEx.expectMessage(equalTo("Promo code MY_TEST_CODE_ABC is blacklisted"));
        expectedEx.expect(ErrorException.class);

        promoCode.setBlacklisted(true);

        checker.ensurePromoCodeCanBeActivated(promoCode);
    }

    @Test
    public void throwsErrorForNotStarted() {
        expectedEx.expectMessage(equalTo("Activation dates for promo code MY_TEST_CODE_ABC has not started"));
        expectedEx.expect(ErrorException.class);

        promoCode.setValidFrom(Instant.now().plus(2, ChronoUnit.HOURS));

        checker.ensurePromoCodeCanBeActivated(promoCode);
    }

    @Test
    public void throwsErrorForExpired() {
        expectedEx.expectMessage(equalTo("Activation dates for promo code MY_TEST_CODE_ABC has ended"));
        expectedEx.expect(ErrorException.class);

        promoCode.setValidTill(Instant.now().minus(2, ChronoUnit.HOURS));

        checker.ensurePromoCodeCanBeActivated(promoCode);
    }

    @Test
    public void throwsErrorForActivationStrategy() {
        expectedEx.expectMessage(equalTo("Promo code MY_TEST_CODE_ABC has no activations left"));
        expectedEx.expect(ErrorException.class);

        promoCode.setActivationsStrategy(PromoCodeActivationsStrategy.LIMITED_ACTIVATIONS);
        promoCode.setAllowedActivationsCount(2);
        promoCode.setAllowedActivationsTotal(1);

        checker.ensurePromoCodeCanBeActivated(promoCode);
    }
}
