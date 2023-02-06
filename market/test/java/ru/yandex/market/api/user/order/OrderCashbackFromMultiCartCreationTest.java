package ru.yandex.market.api.user.order;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.Test;

import ru.yandex.market.api.domain.v2.PerkStatus;
import ru.yandex.market.api.matchers.GrowingCashbackMatcher;
import ru.yandex.market.api.matchers.PaymentSystemCashbackMatcher;
import ru.yandex.market.api.matchers.WelcomeCashbackMatcher;
import ru.yandex.market.api.user.order.builders.MultiCartBuilder;
import ru.yandex.market.api.user.order.builders.OrderBuilder;
import ru.yandex.market.api.user.order.cashback.Cashback;
import ru.yandex.market.api.user.order.cashback.CashbackPermission;
import ru.yandex.market.api.user.order.cashback.CashbackType;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.cashback.model.CashbackOption;
import ru.yandex.market.checkout.checkouter.cashback.model.CashbackOptions;
import ru.yandex.market.checkout.checkouter.cashback.model.CashbackProfile;
import ru.yandex.market.checkout.checkouter.cashback.model.CashbackProfileDelivery;
import ru.yandex.market.checkout.checkouter.cashback.model.CashbackProfilePayment;
import ru.yandex.market.checkout.checkouter.cashback.model.CashbackPromoResponse;
import ru.yandex.market.checkout.checkouter.cashback.model.CashbackThreshold;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.loyalty.api.model.CashbackPermision;
import ru.yandex.market.loyalty.api.model.perk.PerkType;

import static org.junit.Assert.assertThat;
import static ru.yandex.market.api.matchers.CashbackMatcher.applicableCashback;
import static ru.yandex.market.api.matchers.CashbackMatcher.balance;
import static ru.yandex.market.api.matchers.CashbackMatcher.cashback;
import static ru.yandex.market.api.matchers.CashbackMatcher.cashbackProfiles;
import static ru.yandex.market.api.matchers.CashbackMatcher.growingCashback;
import static ru.yandex.market.api.matchers.CashbackMatcher.paymentSystemCashback;
import static ru.yandex.market.api.matchers.CashbackMatcher.selectedCashbackOption;
import static ru.yandex.market.api.matchers.CashbackMatcher.welcomeCashback;
import static ru.yandex.market.api.matchers.CashbackOptionMatcher.amount;
import static ru.yandex.market.api.matchers.CashbackOptionMatcher.cashbackOption;
import static ru.yandex.market.api.matchers.CashbackOptionMatcher.promoKey;
import static ru.yandex.market.api.matchers.CashbackOptionMatcher.type;
import static ru.yandex.market.api.matchers.CashbackOptionMatcher.version;
import static ru.yandex.market.api.matchers.CashbackOptionsMatcher.cashbackOptions;
import static ru.yandex.market.api.matchers.CashbackOptionsMatcher.emit;
import static ru.yandex.market.api.matchers.CashbackOptionsMatcher.spend;
import static ru.yandex.market.api.matchers.WelcomeCashbackMatcher.minMultiCartTotal;
import static ru.yandex.market.api.matchers.WelcomeCashbackMatcher.remainingMultiCartTotal;

public class OrderCashbackFromMultiCartCreationTest {

    @Test
    public void createCashbackBalance() {
        MultiCart multiCart = new MultiCartBuilder().random().build();
        multiCart.setCashbackBalance(BigDecimal.valueOf(100));
        Cashback cashback = Cashback.fromMultiCart(multiCart, Collections.emptyList());

        assertThat(cashback, cashback(balance(BigDecimal.valueOf(100))));
    }

    @Test
    public void createEmitSelectedOption() {
        MultiCart multiCart = new MultiCartBuilder().random().build();
        multiCart.setSelectedCashbackOption(CashbackOption.EMIT);
        Cashback cashback = Cashback.fromMultiCart(multiCart, Collections.emptyList());

        assertThat(cashback, cashback(selectedCashbackOption(CashbackType.EMIT)));
    }

    @Test
    public void createSpendSelectedOption() {
        MultiCart multiCart = new MultiCartBuilder().random().build();
        multiCart.setSelectedCashbackOption(CashbackOption.SPEND);
        Cashback cashback = Cashback.fromMultiCart(multiCart, Collections.emptyList());

        assertThat(cashback, cashback(selectedCashbackOption(CashbackType.SPEND)));
    }

    @Test
    public void createApplicableEmitCashback() {
        MultiCart multiCart = new MultiCartBuilder().random().build();
        CashbackOptions emit = new CashbackOptions(
                "emitPromoKey",
                123,
                BigDecimal.TEN,
                null,
                null,
                CashbackPermision.ALLOWED,
                null,
                null,
                null,
                null
        );
        multiCart.setCashback(new ru.yandex.market.checkout.checkouter.cashback.model.Cashback(emit, null));
        Cashback cashback = Cashback.fromMultiCart(multiCart, Collections.emptyList());

        assertThat(cashback, cashback(
                applicableCashback(
                        cashbackOptions(
                                emit(
                                        cashbackOption(
                                                promoKey("emitPromoKey"),
                                                type(CashbackPermission.ALLOWED),
                                                version(123),
                                                amount(BigDecimal.TEN)
                                        )
                                )
                        )
                )
        ));
    }

    @Test
    public void createApplicableSpendCashback() {
        MultiCart multiCart = new MultiCartBuilder().random().build();
        CashbackOptions spend = new CashbackOptions(
                "spendPromoKey",
                456,
                new BigDecimal(221),
                null,
                null,
                CashbackPermision.RESTRICTED,
                null,
                null,
                null,
                null
        );
        multiCart.setCashback(new ru.yandex.market.checkout.checkouter.cashback.model.Cashback(null, spend));
        Cashback cashback = Cashback.fromMultiCart(multiCart, Collections.emptyList());

        assertThat(cashback, cashback(
                applicableCashback(
                        cashbackOptions(
                                spend(
                                        cashbackOption(
                                                promoKey("spendPromoKey"),
                                                type(CashbackPermission.RESTRICTED),
                                                version(456),
                                                amount(new BigDecimal(221))
                                        )
                                )
                        )
                )
        ));
    }

    @Test
    public void createCashbackProfiles() {
        MultiCart multiCart = new MultiCartBuilder().random().build();
        List<CashbackProfile> profiles = new ArrayList<CashbackProfile>(2) {
            {
                add(
                        new CashbackProfile(
                                Collections.EMPTY_SET,
                                Collections.emptyList(),
                                new CashbackProfilePayment(Collections.emptyList()),
                                new CashbackProfileDelivery(Collections.emptyList()),
                                null,
                                Collections.emptyList()
                        )
                );
                add(
                        new CashbackProfile(
                                Collections.EMPTY_SET,
                                Collections.emptyList(),
                                new CashbackProfilePayment(Collections.emptyList()),
                                new CashbackProfileDelivery(Collections.emptyList()),
                                null,
                                Collections.emptyList()
                        )
                );
            }
        };
        multiCart.setCashbackOptionsProfiles(profiles);

        Cashback cashback = Cashback.fromMultiCart(multiCart, Collections.emptyList());

        assertThat(cashback, cashback(cashbackProfiles(cast(Matchers.hasSize(2)))));
    }

    @Test
    public void createWelcomeCashback() {
        MultiCart multiCart = new MultiCartBuilder().random().build();
        CashbackOptions emit = new CashbackOptions(
                "welcomePromoKey",
                1,
                BigDecimal.TEN,
                null,
                null,
                CashbackPermision.ALLOWED,
                null,
                null,
                Collections.singletonList(new CashbackThreshold(
                        "welcomePromoKey",
                        Collections.singleton(PerkType.WELCOME_CASHBACK),
                        new BigDecimal(100),
                        new BigDecimal(500),
                        new BigDecimal(200),
                        123
                )),
                null
        );
        multiCart.setCashback(new ru.yandex.market.checkout.checkouter.cashback.model.Cashback(emit, null));
        PerkStatus perkStatus = new PerkStatus();
        perkStatus.setType(ru.yandex.market.api.domain.v2.PerkType.WELCOME_CASHBACK.getId());
        Cashback cashback = Cashback.fromMultiCart(multiCart, Collections.singletonList(perkStatus));

        assertThat(cashback, cashback(
                welcomeCashback(
                        WelcomeCashbackMatcher.welcomeCashback(
                                WelcomeCashbackMatcher.promoKey("welcomePromoKey"),
                                remainingMultiCartTotal(new BigDecimal(100)),
                                minMultiCartTotal(new BigDecimal(500)),
                                WelcomeCashbackMatcher.amount(new BigDecimal(200)),
                                WelcomeCashbackMatcher.agitationPriority(123)
                        )
                )));
    }

    @Test
    public void notCreateWelcomeCashbackIfHasNoRequiredPerk() {
        MultiCart multiCart = new MultiCartBuilder().random().build();
        CashbackOptions emit = new CashbackOptions(
                "welcomePromoKey",
                1,
                BigDecimal.TEN,
                null,
                null,
                CashbackPermision.ALLOWED,
                null,
                null,
                Collections.singletonList(new CashbackThreshold(
                        "welcomePromoKey",
                        Collections.singleton(PerkType.WELCOME_CASHBACK),
                        new BigDecimal(100),
                        new BigDecimal(500),
                        new BigDecimal(200),
                        1
                )),
                null
        );
        multiCart.setCashback(new ru.yandex.market.checkout.checkouter.cashback.model.Cashback(emit, null));
        Cashback cashback = Cashback.fromMultiCart(multiCart, Collections.emptyList());

        assertThat(cashback, cashback(welcomeCashback(Matchers.equalTo(null))));
    }

    @Test
    public void createCardSystemCashback() {
        MultiCart multiCart = new MultiCartBuilder().random().build();

        Set<PaymentMethod> paymentOptions = new HashSet<PaymentMethod>(2) {
            {
                add(PaymentMethod.YANDEX);
                add(PaymentMethod.CASH_ON_DELIVERY);
            }
        };
        Order order = new OrderBuilder().random().build();
        order.setPaymentOptions(paymentOptions);
        multiCart.setCarts(Collections.singletonList(order));

        List<CashbackProfile> profiles = Collections.singletonList(new CashbackProfile(
                Collections.emptySet(),
                Collections.emptyList(),
                new CashbackProfilePayment(Collections.emptyList()),
                new CashbackProfileDelivery(Collections.emptyList()),
                new ru.yandex.market.checkout.checkouter.cashback.model.Cashback(
                        new CashbackOptions(
                                "paymentSystemPromo",
                                1,
                                new BigDecimal(600),
                                null,
                                Collections.singletonList(new CashbackPromoResponse(
                                        new BigDecimal(500),
                                        "paymentSystemPromo",
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        Collections.emptyList(),
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        234,
                                        null,
                                        null,
                                        null,
                                        null
                                )),
                                null,
                                null,
                                null,
                                null,
                                null
                        ),
                        null
                ),
                Collections.emptyList()
        ));
        multiCart.setCashbackOptionsProfiles(profiles);
        PerkStatus perkStatus = new PerkStatus();
        perkStatus.setType(ru.yandex.market.api.domain.v2.PerkType.PAYMENT_SYSTEM_EXTRA_CASHBACK.getId());
        perkStatus.setPromoKey("paymentSystemPromo");
        perkStatus.setCashbackPercentNominal(10);
        perkStatus.setPaymentSystem("mastercard");
        perkStatus.setPurchased(true);
        Cashback cashback = Cashback.fromMultiCart(multiCart, Collections.singletonList(perkStatus));

        assertThat(cashback, cashback(
                paymentSystemCashback(
                        PaymentSystemCashbackMatcher.paymentSystemCashback(
                                PaymentSystemCashbackMatcher.promoKey("paymentSystemPromo"),
                                PaymentSystemCashbackMatcher.system("mastercard"),
                                PaymentSystemCashbackMatcher.cashbackPercent(10),
                                PaymentSystemCashbackMatcher.amount(new BigDecimal(500)),
                                PaymentSystemCashbackMatcher.agitationPriority(234)
                        )
                )));
    }

    @Test
    public void notCreateCardSystemCashbackIfHasNoRequiredPerk() {
        MultiCart multiCart = new MultiCartBuilder().random().build();

        Set<PaymentMethod> paymentOptions = new HashSet<PaymentMethod>(2) {
            {
                add(PaymentMethod.YANDEX);
                add(PaymentMethod.CASH_ON_DELIVERY);
            }
        };
        Order order = new OrderBuilder().random().build();
        order.setPaymentOptions(paymentOptions);
        multiCart.setCarts(Collections.singletonList(order));

        List<CashbackProfile> profiles = Collections.singletonList(new CashbackProfile(
                Collections.EMPTY_SET,
                Collections.emptyList(),
                new CashbackProfilePayment(Collections.emptyList()),
                new CashbackProfileDelivery(Collections.emptyList()),
                new ru.yandex.market.checkout.checkouter.cashback.model.Cashback(
                        new CashbackOptions(
                                "paymentSystemPromo",
                                1,
                                new BigDecimal(600),
                                null,
                                Collections.singletonList(new CashbackPromoResponse(
                                        new BigDecimal(500),
                                        "paymentSystemPromo",
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        Collections.emptyList(),
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null
                                )),
                                null,
                                null,
                                null,
                                null,
                                null
                        ),
                        null
                ),
                Collections.emptyList()
        ));
        multiCart.setCashbackOptionsProfiles(profiles);
        Cashback cashback = Cashback.fromMultiCart(multiCart, Collections.emptyList());

        assertThat(cashback, cashback(paymentSystemCashback(Matchers.equalTo(null))));
    }

    @Test
    public void notCreateCardSystemCashbackIfRequiredPerkIsNotPurchased() {
        MultiCart multiCart = new MultiCartBuilder().random().build();
        List<CashbackProfile> profiles = Collections.singletonList(new CashbackProfile(
                Collections.EMPTY_SET,
                Collections.emptyList(),
                new CashbackProfilePayment(Collections.emptyList()),
                new CashbackProfileDelivery(Collections.emptyList()),
                new ru.yandex.market.checkout.checkouter.cashback.model.Cashback(
                        new CashbackOptions(
                                "paymentSystemPromo",
                                1,
                                new BigDecimal(600),
                                null,
                                Collections.singletonList(new CashbackPromoResponse(
                                        new BigDecimal(500),
                                        "paymentSystemPromo",
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        Collections.emptyList(),
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null
                                )),
                                null,
                                null,
                                null,
                                null,
                                null
                        ),
                        null
                ),
                Collections.emptyList()
        ));
        multiCart.setCashbackOptionsProfiles(profiles);
        PerkStatus perkStatus = new PerkStatus();
        perkStatus.setType(ru.yandex.market.api.domain.v2.PerkType.PAYMENT_SYSTEM_EXTRA_CASHBACK.getId());
        perkStatus.setPromoKey("paymentSystemPromo");
        perkStatus.setCashbackPercentNominal(10);
        perkStatus.setPaymentSystem("mastercard");
        perkStatus.setPurchased(false);
        Cashback cashback = Cashback.fromMultiCart(multiCart, Collections.emptyList());

        assertThat(cashback, cashback(paymentSystemCashback(Matchers.equalTo(null))));
    }

    @Test
    public void notCreateCardSystemCashbackIfHasNoRequiredPromo() {
        MultiCart multiCart = new MultiCartBuilder().random().build();

        Set<PaymentMethod> paymentOptions = new HashSet<PaymentMethod>(2) {
            {
                add(PaymentMethod.YANDEX);
                add(PaymentMethod.CASH_ON_DELIVERY);
            }
        };
        Order order = new OrderBuilder().random().build();
        order.setPaymentOptions(paymentOptions);
        multiCart.setCarts(Collections.singletonList(order));

        List<CashbackProfile> profiles = Collections.singletonList(new CashbackProfile(
                Collections.EMPTY_SET,
                Collections.emptyList(),
                new CashbackProfilePayment(Collections.emptyList()),
                new CashbackProfileDelivery(Collections.emptyList()),
                new ru.yandex.market.checkout.checkouter.cashback.model.Cashback(
                        new CashbackOptions(
                                "notMatchedPromo",
                                1,
                                new BigDecimal(600),
                                null,
                                Collections.singletonList(new CashbackPromoResponse(
                                        new BigDecimal(500),
                                        "notMatchedPromo",
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        Collections.emptyList(),
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null
                                )),
                                null,
                                null,
                                null,
                                null,
                                null
                        ),
                        null
                ),
                Collections.emptyList()
        ));
        multiCart.setCashbackOptionsProfiles(profiles);
        PerkStatus perkStatus = new PerkStatus();
        perkStatus.setType(ru.yandex.market.api.domain.v2.PerkType.PAYMENT_SYSTEM_EXTRA_CASHBACK.getId());
        perkStatus.setPromoKey("paymentSystemPromo");
        perkStatus.setCashbackPercentNominal(10);
        perkStatus.setPaymentSystem("mastercard");
        Cashback cashback = Cashback.fromMultiCart(multiCart, Collections.singletonList(perkStatus));

        assertThat(cashback, cashback(paymentSystemCashback(Matchers.equalTo(null))));
    }

    @Test
    public void notCreateCardSystemCashbackIfHasNoCardOnlinePayment() {
        MultiCart multiCart = new MultiCartBuilder().random().build();

        Set<PaymentMethod> paymentOptions = new HashSet<PaymentMethod>(1) {
            {
                add(PaymentMethod.CASH_ON_DELIVERY);
            }
        };
        Order order = new OrderBuilder().random().build();
        order.setPaymentOptions(paymentOptions);
        multiCart.setCarts(Collections.singletonList(order));

        List<CashbackProfile> profiles = Collections.singletonList(new CashbackProfile(
                Collections.emptySet(),
                Collections.emptyList(),
                new CashbackProfilePayment(Collections.emptyList()),
                new CashbackProfileDelivery(Collections.emptyList()),
                new ru.yandex.market.checkout.checkouter.cashback.model.Cashback(
                        new CashbackOptions(
                                "paymentSystemPromo",
                                1,
                                new BigDecimal(600),
                                null,
                                Collections.singletonList(new CashbackPromoResponse(
                                        new BigDecimal(500),
                                        "paymentSystemPromo",
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        Collections.emptyList(),
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null
                                )),
                                null,
                                null,
                                null,
                                null,
                                null
                        ),
                        null
                ),
                Collections.emptyList()
        ));
        multiCart.setCashbackOptionsProfiles(profiles);
        PerkStatus perkStatus = new PerkStatus();
        perkStatus.setType(ru.yandex.market.api.domain.v2.PerkType.PAYMENT_SYSTEM_EXTRA_CASHBACK.getId());
        perkStatus.setPromoKey("paymentSystemPromo");
        perkStatus.setCashbackPercentNominal(10);
        perkStatus.setPaymentSystem("mastercard");
        Cashback cashback = Cashback.fromMultiCart(multiCart, Collections.singletonList(perkStatus));

        assertThat(cashback, cashback(paymentSystemCashback(Matchers.equalTo(null))));
    }

    @Test
    public void notCreateCardSystemCashbackIfOneOrderHasNoCardOnlinePayment() {
        MultiCart multiCart = new MultiCartBuilder().random().build();

        Set<PaymentMethod> firstOrderPaymentOptions = new HashSet<PaymentMethod>(2) {
            {
                add(PaymentMethod.YANDEX);
                add(PaymentMethod.CASH_ON_DELIVERY);
            }
        };
        Order firstOrder = new OrderBuilder().random().build();
        firstOrder.setPaymentOptions(firstOrderPaymentOptions);

        Set<PaymentMethod> secondOrderPaymentOptions = new HashSet<PaymentMethod>(1) {
            {
                add(PaymentMethod.CASH_ON_DELIVERY);
            }
        };
        Order secondOrder = new OrderBuilder().random().build();
        secondOrder.setPaymentOptions(secondOrderPaymentOptions);

        List<Order> orders = new ArrayList<Order>(2) {
            {
                add(firstOrder);
                add(secondOrder);
            }
        };
        multiCart.setCarts(orders);

        List<CashbackProfile> profiles = Collections.singletonList(new CashbackProfile(
                Collections.emptySet(),
                Collections.emptyList(),
                new CashbackProfilePayment(Collections.emptyList()),
                new CashbackProfileDelivery(Collections.emptyList()),
                new ru.yandex.market.checkout.checkouter.cashback.model.Cashback(
                        new CashbackOptions(
                                "paymentSystemPromo",
                                1,
                                new BigDecimal(600),
                                null,
                                Collections.singletonList(new CashbackPromoResponse(
                                        new BigDecimal(500),
                                        "paymentSystemPromo",
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        Collections.emptyList(),
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null
                                )),
                                null,
                                null,
                                null,
                                null,
                                null
                        ),
                        null
                ),
                Collections.emptyList()
        ));
        multiCart.setCashbackOptionsProfiles(profiles);
        PerkStatus perkStatus = new PerkStatus();
        perkStatus.setType(ru.yandex.market.api.domain.v2.PerkType.PAYMENT_SYSTEM_EXTRA_CASHBACK.getId());
        perkStatus.setPromoKey("paymentSystemPromo");
        perkStatus.setCashbackPercentNominal(10);
        perkStatus.setPaymentSystem("mastercard");
        Cashback cashback = Cashback.fromMultiCart(multiCart, Collections.singletonList(perkStatus));

        assertThat(cashback, cashback(paymentSystemCashback(Matchers.equalTo(null))));
    }

    @Test
    public void createGrowingCashback() {
        MultiCart multiCart = new MultiCartBuilder().random().build();
        CashbackOptions emit = new CashbackOptions(
                "growingCashback",
                1,
                BigDecimal.TEN,
                null,
                null,
                CashbackPermision.ALLOWED,
                null,
                null,
                Collections.singletonList(new CashbackThreshold(
                        "growingCashback",
                        Collections.singleton(PerkType.GROWING_CASHBACK),
                        new BigDecimal(100),
                        new BigDecimal(500),
                        new BigDecimal(200),
                        453
                )),
                null
        );
        multiCart.setCashback(new ru.yandex.market.checkout.checkouter.cashback.model.Cashback(emit, null));
        PerkStatus perkStatus = new PerkStatus();
        perkStatus.setType(ru.yandex.market.api.domain.v2.PerkType.GROWING_CASHBACK.getId());
        perkStatus.setNextStepPromoKey("growingCashback");
        perkStatus.setPurchased(true);
        Cashback cashback = Cashback.fromMultiCart(multiCart, Collections.singletonList(perkStatus));

        assertThat(cashback, cashback(
                growingCashback(
                        GrowingCashbackMatcher.growingCashback(
                                GrowingCashbackMatcher.promoKey("growingCashback"),
                                GrowingCashbackMatcher.remainingMultiCartTotal(new BigDecimal(100)),
                                GrowingCashbackMatcher.minMultiCartTotal(new BigDecimal(500)),
                                GrowingCashbackMatcher.amount(new BigDecimal(200)),
                                GrowingCashbackMatcher.agitationPriority(453)
                        )
                )));
    }

    @Test
    public void notCreateGrowingCashbackIfHasNoRequiredPerk() {
        MultiCart multiCart = new MultiCartBuilder().random().build();
        CashbackOptions emit = new CashbackOptions(
                "growingCashback",
                1,
                BigDecimal.TEN,
                null,
                null,
                CashbackPermision.ALLOWED,
                null,
                null,
                Collections.singletonList(new CashbackThreshold(
                        "growingCashback",
                        Collections.singleton(PerkType.GROWING_CASHBACK),
                        new BigDecimal(100),
                        new BigDecimal(500),
                        new BigDecimal(200),
                        1
                )),
                null
        );
        multiCart.setCashback(new ru.yandex.market.checkout.checkouter.cashback.model.Cashback(emit, null));
        PerkStatus perkStatus = new PerkStatus();
        perkStatus.setType(ru.yandex.market.api.domain.v2.PerkType.GROWING_CASHBACK.getId());
        perkStatus.setNextStepPromoKey("growingCashback");
        perkStatus.setPurchased(false);
        Cashback cashback = Cashback.fromMultiCart(multiCart, Collections.singletonList(perkStatus));

        assertThat(cashback, cashback(growingCashback(Matchers.equalTo(null))));
    }

    @Test
    public void notCreateGrowingCashbackIfRequiredPerkIsNotPurchased() {
        MultiCart multiCart = new MultiCartBuilder().random().build();
        CashbackOptions emit = new CashbackOptions(
                "growingCashback",
                1,
                BigDecimal.TEN,
                null,
                null,
                CashbackPermision.ALLOWED,
                null,
                null,
                Collections.singletonList(new CashbackThreshold(
                        "growingCashback",
                        Collections.singleton(PerkType.GROWING_CASHBACK),
                        new BigDecimal(100),
                        new BigDecimal(500),
                        new BigDecimal(200),
                        1
                )),
                null
        );
        multiCart.setCashback(new ru.yandex.market.checkout.checkouter.cashback.model.Cashback(emit, null));
        Cashback cashback = Cashback.fromMultiCart(multiCart, Collections.emptyList());

        assertThat(cashback, cashback(growingCashback(Matchers.equalTo(null))));
    }

    @Test
    public void notCreateGrowingCashbackIfPromoKesNotMatch() {
        MultiCart multiCart = new MultiCartBuilder().random().build();
        CashbackOptions emit = new CashbackOptions(
                "growingCashback",
                1,
                BigDecimal.TEN,
                null,
                null,
                CashbackPermision.ALLOWED,
                null,
                null,
                Collections.singletonList(new CashbackThreshold(
                        "growingCashback",
                        Collections.singleton(PerkType.GROWING_CASHBACK),
                        new BigDecimal(100),
                        new BigDecimal(500),
                        new BigDecimal(200),
                        1
                )),
                null
        );
        multiCart.setCashback(new ru.yandex.market.checkout.checkouter.cashback.model.Cashback(emit, null));
        PerkStatus perkStatus = new PerkStatus();
        perkStatus.setType(ru.yandex.market.api.domain.v2.PerkType.GROWING_CASHBACK.getId());
        perkStatus.setNextStepPromoKey("growingCashback1");
        Cashback cashback = Cashback.fromMultiCart(multiCart, Collections.singletonList(perkStatus));

        assertThat(cashback, cashback(growingCashback(Matchers.equalTo(null))));
    }

    private static <In, Out> Matcher<Out> cast(Matcher<? extends In> matcher) {
        return (Matcher<Out>) matcher;
    }
}
