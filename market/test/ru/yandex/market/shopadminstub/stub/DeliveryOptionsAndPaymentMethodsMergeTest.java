package ru.yandex.market.shopadminstub.stub;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.helpers.CartHelper;
import ru.yandex.market.helpers.CartParameters;
import ru.yandex.market.providers.CartRequestProvider;
import ru.yandex.market.providers.ItemDeliveryOptionProvider;
import ru.yandex.market.providers.ItemProvider;
import ru.yandex.market.shopadminstub.application.AbstractTestBase;
import ru.yandex.market.shopadminstub.model.CartRequest;
import ru.yandex.market.shopadminstub.model.Item;
import ru.yandex.market.shopadminstub.model.ItemDeliveryOption;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.util.Locale;
import java.util.Objects;

import static java.util.Collections.emptyList;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.xpath;
import static ru.yandex.market.shopadminstub.stub.StubPushApiTestUtils.ALL_POSTPAID;
import static ru.yandex.market.shopadminstub.stub.StubPushApiTestUtils.ONLY_CARD;
import static ru.yandex.market.shopadminstub.stub.StubPushApiTestUtils.ONLY_CASH;
import static ru.yandex.market.shopadminstub.stub.StubPushApiTestUtils.ONLY_YANDEX;
import static ru.yandex.market.shopadminstub.stub.StubPushApiTestUtils.checkDeliveryOption;
import static ru.yandex.market.shopadminstub.stub.StubPushApiTestUtils.checkDeliveryOptionsCount;
import static ru.yandex.market.shopadminstub.stub.StubPushApiTestUtils.checkItem;

public class DeliveryOptionsAndPaymentMethodsMergeTest extends AbstractTestBase {
    @Autowired
    private CartHelper cartHelper;

    /**
     * Стаб должен отдавать доставку в родной регион
     * <p>
     * Подготовка:
     * 1. Кладем в настройки домашний регион = спб
     * 2. Подкладываем ответ репорта
     * 3. Подкладываем ответ фиддиспатчера
     * <p>
     * Действие:
     * 1. Дергаем ручку /{shopId}/cart
     * <p>
     * Проверка:
     * 1. Проверяем, что есть опция доставки, которую вернул репорт
     */
    @Test
    public void testSkipsDeliveryOptionsWhenNoCommonPaymentOptions() throws Exception {
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.of(today, LocalTime.of(20, 15, 30));

        CartRequest cartRequest = CartRequestProvider.buildCartRequest(
                ItemProvider.buildItem(383182L, "1",
                        ItemDeliveryOptionProvider.buildFree(),
                        ItemDeliveryOptionProvider.buildAverage(),
                        ItemDeliveryOptionProvider.buildFastest()
                ),
                ItemProvider.buildItem(383183L, "2",
                        ItemDeliveryOptionProvider.buildFree(),
                        ItemDeliveryOptionProvider.buildAverage(ONLY_CARD),
                        ItemDeliveryOptionProvider.buildFastest()
                )
        );
        cartRequest.setRegionId(CartRequestProvider.DEFAULT_REGION_2);

        CartParameters cartParameters = new CartParameters(cartRequest);
        cartParameters.setFakeNow(now);

        ResultActions resultActions = cartHelper.cart(cartParameters)
                .andDo(log())
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(xpath("/cart/@delivery-currency").string(Currency.RUR.name()));

        checkItem(resultActions, 1, 383182L, "1");
        checkItem(resultActions, 2, 383183L, "2");

        checkDeliveryOptionsCount(resultActions, 3);
        checkDeliveryOption(resultActions, today, 1, ALL_POSTPAID, "Курьер", "DELIVERY", "100.00", 1, 2);
        checkDeliveryOption(resultActions, today, 2, ONLY_CASH, "Курьер", "DELIVERY", "0.00", 31, null);
        checkDeliveryOption(resultActions, today, 3, ONLY_CARD, "Самовывоз", "PICKUP", "50", 30, null);
    }
    @Test
    public void testReturnsDeliveryOptionsWhenNoPaymentOptions() throws Exception {
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.of(today, LocalTime.of(20, 15, 30));

        CartRequest cartRequest = CartRequestProvider.buildCartRequest(
                ItemProvider.buildItem(383182L, "1",
                                       ItemDeliveryOptionProvider.buildFree(emptyList()),
                                       ItemDeliveryOptionProvider.buildAverage(emptyList()),
                                       ItemDeliveryOptionProvider.buildFastest(emptyList())
                )
        );
        cartRequest.setRegionId(CartRequestProvider.DEFAULT_REGION_2);

        CartParameters cartParameters = new CartParameters(cartRequest);
        cartParameters.setFakeNow(now);

        ResultActions resultActions = cartHelper.cart(cartParameters)
                .andDo(log())
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(xpath("/cart/@delivery-currency").string(Currency.RUR.name()));

        checkItem(resultActions, 1, 383182L, "1");
        checkDeliveryOptionsCount(resultActions, 4);
        checkDeliveryOption(resultActions, today, 1, emptyList(), "Курьер", "DELIVERY", "100.00", 1, 2);
        checkDeliveryOption(resultActions, today, 2, emptyList(), "Курьер", "DELIVERY", "50.00", 14, 14);
        checkDeliveryOption(resultActions, today, 3, emptyList(), "Курьер", "DELIVERY", "0.00", 31, null);
        checkDeliveryOption(resultActions, today, 4, ONLY_CARD, "Самовывоз", "PICKUP", "50", 30, null);
    }

    @Test
    public void testReturnsNoCommonPaymentMethodsFromDeliveryOptions() throws Exception {
        CartRequest cartRequest = CartRequestProvider.buildCartRequest(
                ItemProvider.buildItem(383182L, "1",
                        ItemDeliveryOptionProvider.buildFree(ONLY_CASH),
                        ItemDeliveryOptionProvider.buildAverage(ONLY_CASH),
                        ItemDeliveryOptionProvider.buildFastest(ONLY_CASH)
                ),
                ItemProvider.buildItem(383183L, "2",
                        ItemDeliveryOptionProvider.buildFree(ONLY_CARD),
                        ItemDeliveryOptionProvider.buildAverage(ONLY_CARD),
                        ItemDeliveryOptionProvider.buildFastest(ONLY_CARD)
                )
        );
        cartRequest.setRegionId(CartRequestProvider.DEFAULT_REGION_2);

        LocalDateTime now = LocalDateTime.of(2018, Month.FEBRUARY, 6, 16, 16, 16);
        LocalDate today = now.toLocalDate();

        CartParameters cartParameters = new CartParameters(cartRequest);
        cartParameters.setFakeNow(now);

        ResultActions resultActions = cartHelper.cart(cartParameters)
                .andDo(log())
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(xpath("/cart/@delivery-currency").string(Currency.RUR.name()));

        checkItem(resultActions, 1, 383182L, "1");
        checkItem(resultActions, 2, 383183L, "2");

        checkDeliveryOptionsCount(resultActions, 1);
        checkDeliveryOption(resultActions, today, 1, ONLY_CARD, "Самовывоз", "PICKUP", "50", 30, null);

        resultActions
                .andExpect(xpath("/cart/delivery-options/delivery[1]/outlets/outlet/@code").string("69"))
                .andExpect(xpath("/cart/delivery-options/delivery[1]/payment-methods/payment-method/text()").string(PaymentMethod.CARD_ON_DELIVERY.name()));
    }

    @Test
    public void testReturnsSingleDeliveryOptionWhenOnlyOrderBeforeDiffers() throws Exception {
        ItemDeliveryOption freeButOrderBefore18 = ItemDeliveryOptionProvider.buildFree(ONLY_CASH);
        freeButOrderBefore18.setOrderBefore(18);
        freeButOrderBefore18.setFromDay(1);
        freeButOrderBefore18.setToDay(null);

        ItemDeliveryOption fastestButOrderBefore21 = ItemDeliveryOptionProvider.buildFastest(ONLY_CASH);
        fastestButOrderBefore21.setOrderBefore(21);
        fastestButOrderBefore21.setFromDay(1);
        fastestButOrderBefore21.setToDay(null);

        CartRequest cartRequest = CartRequestProvider.buildCartRequest(
                ItemProvider.buildItem(383182L, "1", freeButOrderBefore18, fastestButOrderBefore21)
        );
        cartRequest.setRegionId(CartRequestProvider.DEFAULT_REGION_2);

        LocalDateTime now = LocalDateTime.parse("2017-10-13T15:15:30");

        CartParameters cartParameters = new CartParameters(cartRequest);
        cartParameters.setFakeNow(now);

        ResultActions resultActions = cartHelper.cart(cartParameters)
                .andDo(log())
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(xpath("/cart/@delivery-currency").string(Currency.RUR.name()))
                .andExpect(xpath("/cart/delivery-options/delivery[2]/outlets/outlet/@code").string("69"));

        checkItem(resultActions, 1, 383182L, "1");
        LocalDate today = now.toLocalDate();
        checkDeliveryOptionsCount(resultActions, 2);
        checkDeliveryOption(resultActions, today, 1, ONLY_CASH, "Курьер", "DELIVERY", "0.00", 1, null);
        checkDeliveryOption(resultActions, today, 2, ONLY_CARD, "Самовывоз", "PICKUP", "50", 30, null);
    }

    /**
     * Из двух опций доставки различающихся только ценой для двух товаров выбирается опция с большей ценой
     */
    @Test
    public void testReturnsSingleDeliveryOptionWhenOnlyPriceDiffers() throws Exception {
        ItemDeliveryOption withLowerPrice = ItemDeliveryOptionProvider.buildFastest(ONLY_CASH);
        ItemDeliveryOption withHigherPrice = ItemDeliveryOptionProvider.buildFastest(ONLY_CASH);
        withHigherPrice.setPrice(withHigherPrice.getPrice().add(BigDecimal.TEN));
        String expectedPrice = new DecimalFormat("#0.00", DecimalFormatSymbols.getInstance(Locale.US)).format(withHigherPrice.getPrice());
        CartRequest cartRequest = CartRequestProvider.buildCartRequest(
                ItemProvider.buildItem(383182L, "1", withLowerPrice),
                ItemProvider.buildItem(383183L, "2", withHigherPrice)
        );
        CartParameters cartParameters = new CartParameters(cartRequest);
        ResultActions resultActions = cartHelper.cart(cartParameters)
                .andDo(log())
                .andExpect(MockMvcResultMatchers.status().isOk());

        checkItem(resultActions, 1, 383182L, "1");
        LocalDate today = LocalDate.now(Clock.systemDefaultZone());
        checkDeliveryOptionsCount(resultActions, 1);
        checkDeliveryOption(resultActions, today, 1, ONLY_CASH, "Курьер", "DELIVERY", expectedPrice, 1, null);
    }

    private void validateYandexAccountDelivery(ResultActions resultActions) throws Exception {
        checkItem(resultActions, 1, 383182L, "1");
        LocalDate today = LocalDate.now(Clock.systemDefaultZone());
        checkDeliveryOptionsCount(resultActions, 1);
        checkDeliveryOption(resultActions, today, 1, ONLY_YANDEX, "Электронная доставка",
                "DIGITAL", "0", 0, 1);
    }

}
