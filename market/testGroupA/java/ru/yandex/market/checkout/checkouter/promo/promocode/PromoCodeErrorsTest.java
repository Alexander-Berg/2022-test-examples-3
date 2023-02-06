package ru.yandex.market.checkout.checkouter.promo.promocode;

import java.math.BigDecimal;
import java.util.Objects;

import io.qameta.allure.Epic;
import io.qameta.allure.Story;
import io.qameta.allure.junit4.Tag;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import ru.yandex.market.checkout.allure.Epics;
import ru.yandex.market.checkout.allure.Stories;
import ru.yandex.market.checkout.allure.Tags;
import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.pay.PaymentType;
import ru.yandex.market.checkout.checkouter.validation.PromoCodeValidationResult;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.test.providers.OrderItemProvider;
import ru.yandex.market.checkout.util.loyalty.LoyaltyDiscount;
import ru.yandex.market.loyalty.api.model.MarketLoyaltyError;
import ru.yandex.market.loyalty.api.model.delivery.DeliveryType;

import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static ru.yandex.market.checkout.checkouter.order.promo.PromoType.YANDEX_PLUS;
import static ru.yandex.market.checkout.checkouter.promo.util.Utils.addCartErrorsChecks;

/**
 * @author Nikolai Iusiumbeli
 * date: 04/08/2017
 */
public class PromoCodeErrorsTest extends AbstractWebTestBase {

    private static final String TEST_PROMO_CODE = "TEST-PROMO-CODE";
    private static final String REAL_PROMO_CODE = "REAL-PROMO-CODE";

    private Parameters parameters;

    @BeforeEach
    public void setUp() throws Exception {
        parameters = new Parameters();
        parameters.configureMultiCart(multiCart -> {
            Order order = multiCart.getCarts().get(0);
            order.addItem(OrderItemProvider.buildOrderItem("2", 1L, 1));
            assertThat(order.getItems(), hasSize(2));

            multiCart.setPaymentMethod(PaymentMethod.YANDEX);
            multiCart.setPaymentType(PaymentType.PREPAID);
            multiCart.setPromoCode(TEST_PROMO_CODE);
        });
        parameters.turnOffErrorChecks();
    }

    /**
     * Проверяем, что в ошибках заказа передается поле userMessage
     *
     * @link https://testpalm.yandex-team.ru/testcase/checkouter-24
     * https://testpalm.yandex-team.ru/testcase/checkouter-34
     */
    @Tag(Tags.PROMO)
    @Epic(Epics.CHECKOUT)
    @Story(Stories.CHECKOUT)
    @DisplayName("Проверяем, что в ошибках заказа передается поле userMessage")
    @Test
    public void testErrorsUserMessage() throws Exception {
        //given
        String promoErrorSeverity = "ERROR";
        String promoErrorType = "PROMO_CODE_ERROR";
        String promoErrorCode = "COUPON_ALREADY_SPENT";
        String userMessage = "userMessage122314 adfkjlsd";

        parameters.getReportParameters().setShopSupportsSubsidies(true);

        //when
        MarketLoyaltyError error = new MarketLoyaltyError(promoErrorCode, "devMessage", userMessage);
        parameters.getLoyaltyParameters().setCustomLoyaltyMockConfiguration(
                loyaltyConfigurer -> loyaltyConfigurer.mockCalcError(error, HttpStatus.UNPROCESSABLE_ENTITY.value())
        );

        addCartErrorsChecks(parameters.cartResultActions(), promoErrorType, promoErrorCode, promoErrorSeverity,
                equalTo(userMessage));

        parameters.setMultiCartChecker(multiCart -> {
            assertThat(multiCart.getValidationErrors(), hasSize(greaterThanOrEqualTo(1)));
            PromoCodeValidationResult multiCartPromoCodeError =
                    (PromoCodeValidationResult) multiCart.getValidationErrors().stream()
                    .filter(result -> Objects.equals(result.getType(), promoErrorType))
                    .findFirst()
                    .orElseGet(null);

            assertThat(multiCartPromoCodeError, notNullValue());
            assertThat(multiCartPromoCodeError.getType(), is(promoErrorType));
            assertThat(multiCartPromoCodeError.getCode(), is(promoErrorCode));
            assertThat(multiCartPromoCodeError.getSeverity().name(), is(promoErrorSeverity));
            assertThat(multiCartPromoCodeError.getUserMessage(), is(userMessage));

        });

        orderCreateHelper.cart(parameters);
    }

    /**
     * Проверяем, что в ошибках заказа не передается поле userMessage, если лоялти не передал
     */

    @Tag(Tags.PROMO)
    @Epic(Epics.CHECKOUT)
    @Story(Stories.CHECKOUT)
    @DisplayName("Проверяем, что в ошибках заказа не передается поле userMessage, если лоялти не передал")
    @Test
    public void testEmptyUserMessage() throws Exception {
        //given
        String promoErrorSeverity = "ERROR";
        String promoErrorType = "PROMO_CODE_ERROR";
        String promoErrorCode = "COUPON_ALREADY_SPENT";
        parameters.getReportParameters().setShopSupportsSubsidies(true);

        //when
        MarketLoyaltyError error = new MarketLoyaltyError(promoErrorCode, "devMessage", null);
        parameters.getLoyaltyParameters().setCustomLoyaltyMockConfiguration(
                loyaltyConfigurer -> loyaltyConfigurer.mockCalcError(error, HttpStatus.UNPROCESSABLE_ENTITY.value())
        );

        addCartErrorsChecks(parameters.cartResultActions(), promoErrorType, promoErrorCode, promoErrorSeverity, null);
        orderCreateHelper.cart(parameters);
    }

    /**
     * В данном тесте в добавок к INVALID_COUPON_CODE проверяется еще и то, что ошибка неверного промокода является
     * приоритетнее
     * по отношению к ошибке неверного способа оплаты
     *
     * @link https://testpalm.yandex-team.ru/testcase/checkouter-25
     * https://testpalm.yandex-team.ru/testcase/checkouter-35
     */

    @Tag(Tags.PROMO)
    @Epic(Epics.CHECKOUT)
    @Story(Stories.CHECKOUT)
    @DisplayName("В данном тесте в добавок к INVALID_COUPON_CODE проверяется еще и то, что ошибка неверного промокода" +
            " является приоритетнее " +
            "по отношению к ошибке неверного способа оплаты")
    @Test
    public void testInvalidCouponError() throws Exception {
        //given
        String wrongPromoCode = "WRONG-PROMO-CODE";
        String promoErrorSeverity = "ERROR";
        String promoErrorType = "PROMO_CODE_ERROR";

        parameters.configureMultiCart(multiCart -> {
            //способ оплаты не Яндекс выбран специально(MARKETCHECKOUT-3751)
            multiCart.setPaymentMethod(PaymentMethod.CASH_ON_DELIVERY);
            multiCart.setPaymentType(PaymentType.POSTPAID);
            multiCart.setPromoCode(wrongPromoCode);
        });
        parameters.getReportParameters().setShopSupportsSubsidies(true);

        //when
        MarketLoyaltyError error = new MarketLoyaltyError("COUPON_NOT_EXISTS", "devMessage", null);
        parameters.getLoyaltyParameters().setCustomLoyaltyMockConfiguration(
                loyaltyConfigurer -> loyaltyConfigurer.mockCalcError(error, HttpStatus.UNPROCESSABLE_ENTITY.value())
        );

        addCartErrorsChecks(parameters.cartResultActions(), promoErrorType, "INVALID_COUPON_CODE",
                promoErrorSeverity, null);
        orderCreateHelper.cart(parameters);
    }

    /**
     * Проверка ошибки для опции оплаты не YANDEX.
     * несмотря на успешный ответ лоялти, должна быть ошибка
     * <p>
     * https://testpalm.yandex-team.ru/testcase/checkouter-36
     */
    @Test
    @Disabled("Кажется, не актуально")
    public void testPrepayType() throws Exception {
        //given
        String promoErrorSeverity = "ERROR";
        String promoErrorType = "PROMO_CODE_ERROR";
        String promoErrorCode = "PAYMENT_METHOD_NOT_ALLOWED";
        parameters.configureMultiCart(multiCart -> {
            multiCart.setPaymentMethod(PaymentMethod.CASH_ON_DELIVERY);
            multiCart.setPaymentType(PaymentType.POSTPAID);
            multiCart.setPromoCode(REAL_PROMO_CODE);
        });
        parameters.getReportParameters().setShopSupportsSubsidies(true);

        //when
        parameters.getLoyaltyParameters().setExpectedPromoCode(REAL_PROMO_CODE);
        parameters.setMockLoyalty(true);

        addCartErrorsChecks(parameters.cartResultActions(), promoErrorType, promoErrorCode, promoErrorSeverity, null);
        orderCreateHelper.cart(parameters);
    }

    @Test
    public void shouldCallRevertTokenOnNotAccept() {
        parameters.setupPromo("PROMO_CODE");
        parameters.turnOffErrorChecks();
        parameters.setAcceptOrder(false);

        orderCreateHelper.createOrder(parameters);
        loyaltyConfigurer.verify(postRequestedFor(urlPathEqualTo("/discount/revert")));
    }

    @Test
    public void shouldNotCallRevertTokenWhenNoRevertDiscountToken() {
        parameters.setupPromo(null);
        //есть другая скидка, не купон и не монета
        parameters.getLoyaltyParameters()
                .addDeliveryDiscount(DeliveryType.COURIER, new LoyaltyDiscount(BigDecimal.valueOf(200), YANDEX_PLUS));
        parameters.turnOffErrorChecks();
        parameters.setAcceptOrder(false);

        orderCreateHelper.createOrder(parameters);
        loyaltyConfigurer.verifyZeroInteractions(postRequestedFor(urlPathEqualTo("/discount/revert")));
    }
}
