package ru.yandex.travel.orders.services.promo;

import java.time.Clock;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import ru.yandex.travel.orders.entities.promo.PromoAction;
import ru.yandex.travel.orders.entities.promo.PromoCode;
import ru.yandex.travel.orders.entities.promo.PromoCodeActivation;
import ru.yandex.travel.orders.entities.promo.PromoCodeActivationsStrategy;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PromoCodeCheckerTest {

    private static final String PROMO_CODE = "MY_TEST_CODE_ABC";

    private final PromoCodeChecker checker = new PromoCodeChecker(Clock.systemDefaultZone());

    private PromoCode promoCode;

    private PromoCodeActivation activation;

    @Rule
    public ExpectedException expectedEx = ExpectedException.none();

    @Before
    public void setUp() {
        promoCode = new PromoCode();
        promoCode.setCode(PROMO_CODE);
        promoCode.setPromoAction(new PromoAction());
        promoCode.setActivationsStrategy(PromoCodeActivationsStrategy.LIMITED_ACTIVATIONS);
        promoCode.setAllowedActivationsTotal(1);

        activation = new PromoCodeActivation();

    }

    @Test
    public void alreadyAppliedIfInActivationCountIs1() {
        activation.setTimesUsed(1);
        assertTrue(checker.alreadyApplied(promoCode, activation));
    }

    @Test
    public void alreadyAppliedIfInActivationCountIs0() {
        activation.setTimesUsed(0);
        assertFalse(checker.alreadyApplied(promoCode, activation));
    }
}
