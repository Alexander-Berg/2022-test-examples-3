package ru.yandex.market.api.user.order.cashback;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.Test;

import ru.yandex.market.checkout.checkouter.cashback.model.Cashback;
import ru.yandex.market.checkout.checkouter.cashback.model.CashbackOptions;
import ru.yandex.market.checkout.checkouter.cashback.model.CashbackProfile;
import ru.yandex.market.checkout.checkouter.cashback.model.CashbackProfileDelivery;
import ru.yandex.market.checkout.checkouter.cashback.model.CashbackProfilePayment;
import ru.yandex.market.checkout.checkouter.cashback.model.OrderCashback;
import ru.yandex.market.loyalty.api.model.CashbackOptionsPrecondition;
import ru.yandex.market.loyalty.api.model.CashbackType;
import ru.yandex.market.loyalty.api.model.PaymentType;
import ru.yandex.market.loyalty.api.model.delivery.DeliveryType;

import static org.junit.Assert.assertThat;
import static ru.yandex.market.api.matchers.CashbackOptionMatcher.promoKey;
import static ru.yandex.market.api.matchers.CashbackOptionProfileMathcher.cashbackProfile;
import static ru.yandex.market.api.matchers.CashbackOptionProfileMathcher.deliveryTypes;
import static ru.yandex.market.api.matchers.CashbackOptionProfileMathcher.orders;
import static ru.yandex.market.api.matchers.CashbackOptionProfileMathcher.paymentTypes;
import static ru.yandex.market.api.matchers.CashbackOptionProfileMathcher.preconditions;
import static ru.yandex.market.api.matchers.CashbackOptionProfileMathcher.profileCashback;
import static ru.yandex.market.api.matchers.CashbackOptionProfileMathcher.types;
import static ru.yandex.market.api.matchers.CashbackOptionsMatcher.cashbackOptions;
import static ru.yandex.market.api.matchers.CashbackOptionsMatcher.emit;
import static ru.yandex.market.api.matchers.CashbackOptionsMatcher.spend;
import static ru.yandex.market.api.matchers.CashbackOrderMatcher.cartId;
import static ru.yandex.market.api.matchers.CashbackOrderMatcher.orderCashback;

public class CashbackOptionProfileCreationTest {

    @Test
    public void testCreateFromCheckouter() {
        CashbackProfile cashbackProfile = new CashbackProfile(
                new HashSet<CashbackType>(CashbackType.values().length) {
                    {
                        addAll(Arrays.asList(CashbackType.values()));
                    }
                },
                new ArrayList<CashbackOptionsPrecondition>(CashbackOptionsPrecondition.values().length) {
                    {
                        this.addAll(Arrays.asList(CashbackOptionsPrecondition.values()));
                    }
                },
                new CashbackProfilePayment(
                        new ArrayList<PaymentType>(PaymentType.values().length) {
                            {
                                this.addAll(Arrays.asList(PaymentType.values()));
                            }
                        }
                ),
                new CashbackProfileDelivery(
                        new ArrayList<DeliveryType>(DeliveryType.values().length) {
                            {
                                this.addAll(Arrays.asList(DeliveryType.values()));
                            }
                        }
                ),
                new Cashback(
                        createEmptyCashbackOption("emit"),
                        createEmptyCashbackOption("spend")
                ),
                new ArrayList<OrderCashback>(2) {
                    {
                        add(createEmptyOrderCashback("cart1"));
                        add(createEmptyOrderCashback("cart2"));
                    }
                }

        );
        CashbackOptionProfile cashbackOptionProfile = CashbackOptionProfile.fromCheckouter(cashbackProfile);

        assertThat(cashbackOptionProfile, cashbackProfile(
                types(cast(Matchers.containsInAnyOrder(
                        ru.yandex.market.api.user.order.cashback.CashbackType.EMIT,
                        ru.yandex.market.api.user.order.cashback.CashbackType.SPEND)
                )),
                preconditions(cast(Matchers.containsInAnyOrder(CashbackOptionPrecondition.values()))),
                paymentTypes(cast(Matchers.containsInAnyOrder(PaymentType.values()))),
                deliveryTypes(cast(Matchers.containsInAnyOrder(DeliveryType.values()))),
                profileCashback(
                        cashbackOptions(
                                emit(promoKey("emit")),
                                spend(promoKey("spend"))
                        )
                ),
                orders(
                        cast(Matchers.containsInAnyOrder(
                                orderCashback(cartId("cart1")),
                                orderCashback(cartId("cart2")))
                        )
                )
        ));
    }

    private CashbackOptions createEmptyCashbackOption(final String promoKey) {
        return new CashbackOptions(
                promoKey,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }

    private OrderCashback createEmptyOrderCashback(final String cardId) {
        return new OrderCashback(
                cardId,
                null,
                null,
                null
        );
    }

    private static <In, Out> Matcher<Out> cast(Matcher<? extends In> matcher) {
        return (Matcher<Out>) matcher;
    }
}
