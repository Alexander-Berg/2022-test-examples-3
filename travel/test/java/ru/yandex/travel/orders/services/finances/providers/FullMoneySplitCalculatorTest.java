package ru.yandex.travel.orders.services.finances.providers;

import java.math.BigDecimal;

import org.junit.Test;

import ru.yandex.travel.orders.services.finances.proto.EMoneyRefundMode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static ru.yandex.travel.orders.services.finances.providers.ProviderTestHelper.fullSplit;
import static ru.yandex.travel.orders.services.finances.providers.ProviderTestHelper.partnerSplit;
import static ru.yandex.travel.orders.services.finances.providers.ProviderTestHelper.partnerSplitPostPay;
import static ru.yandex.travel.orders.services.finances.providers.ProviderTestHelper.sourcesSplit;
import static ru.yandex.travel.testing.misc.TestBaseObjects.rub;

public class FullMoneySplitCalculatorTest {
    private final FullMoneySplitCalculator calculator = new FullMoneySplitCalculator();

    @Test
    public void paymentEvent_happyPath() {
        // no promo code
        assertThat(calculator.calculatePaymentWithPromoMoney(partnerView(8600, 1400), rub(0)))
                .isEqualTo(fullSplit(8600, 1400, 0, 0));

        // promo code covers only the cost component
        assertThat(calculator.calculatePaymentWithPromoMoney(partnerView(8600, 1400), rub(3000)))
                .isEqualTo(fullSplit(5600, 1400, 3000, 0));

        // promo code covers the full cost and a part of the reward component
        assertThat(calculator.calculatePaymentWithPromoMoney(partnerView(8600, 1400), rub(9000)))
                .isEqualTo(fullSplit(0, 1000, 8600, 400));

        // promo code covers the full cost and reward component
        assertThat(calculator.calculatePaymentWithPromoMoney(partnerView(8600, 1400), rub(10000)))
                .isEqualTo(fullSplit(0, 0, 8600, 1400));
    }

    @Test
    public void paymentEvent_cornerCases() {
        // rounding just in case
        assertThat(calculator.calculatePaymentWithPromoMoney(partnerView(8600, 1400), rub(0.123)))
                .isEqualTo(fullSplit(8599.88, 1400, 0.12, 0));

        // promo code exceeds the service cost
        assertThatThrownBy(() -> calculator.calculatePaymentWithPromoMoney(partnerView(4000, 1000), rub(6000)))
                .hasMessageContaining("Negative user money: RUB -1000");
    }

    @Test
    public void paymentEvent_plusPoints() {
        assertThat(calculator.calculatePayment(sourcesSplit(9000, 1000, 0), partnerSplit(8600, 1400)))
                .isEqualTo(fullSplit(7600, 1400, 1000, 0, 0, 0));

        assertThat(calculator.calculatePayment(sourcesSplit(1000, 9000, 0), partnerSplit(8600, 1400)))
                .isEqualTo(fullSplit(0, 1000, 8600, 400, 0, 0));

        // with promo
        assertThat(calculator.calculatePayment(sourcesSplit(6000, 1000, 3000), partnerSplit(8600, 1400)))
                .isEqualTo(fullSplit(4600, 1400, 1000, 0, 3000, 0));
    }

    @Test
    public void paymentEvent_postPay() {
        // postpay reward is money we get from partner after the order has been finished
        // so we need to tell partner to take less money from user when post paying
        assertThat(calculator.calculatePayment(sourcesSplit(0, 0, 0, 10000, 1400), partnerSplitPostPay(10000, 1400)))
                .isEqualTo(fullSplit(0, 0, 0, 0, 0, 0, 10000, 1400));

        // TODO reconsider these after 'postpay+promo/plus' scheme is deemed operational
        assertThat(calculator.calculatePayment(sourcesSplit(0, 1000, 0, 9000, 1400), partnerSplitPostPay(10000, 1400)))
                .isEqualTo(fullSplit(0, 0, 1000, 0, 0, 0, 9000, 1400));

        assertThat(calculator.calculatePayment(sourcesSplit(0, 0, 1000, 9000, 1400), partnerSplitPostPay(10000, 1400)))
                .isEqualTo(fullSplit(0, 0, 0, 0, 1000, 0, 9000, 1400));

        assertThat(calculator.calculatePayment(sourcesSplit(0, 0, 9000, 1000, 1400), partnerSplitPostPay(10000, 1400)))
                .isEqualTo(fullSplit(0, 0, 0, 0, 9000, 0, 1000, 1400));

        assertThat(calculator.calculatePayment(sourcesSplit(0, 0, 10000, 0, 1400), partnerSplitPostPay(10000, 1400)))
                .isEqualTo(fullSplit(0, 0, 0, 0, 10000, 0, 0, 1400));
    }

    @Test
    public void refundEvent_happyPath_noPromoCode() {
        // 10_000 order / no promo code
        FullMoneySplit sourcePayment = fullSplit(8600, 1400, 0, 0);

        // no penalty
        assertThat(calculator.calculateRefundWithPromoMoney(sourcePayment, defaultRefundSplit(10000)))
                .isEqualTo(fullSplit(8600, 1400, 0, 0));
        // some penalty
        assertThat(calculator.calculateRefundWithPromoMoney(sourcePayment, defaultRefundSplit(9000)))
                .isEqualTo(fullSplit(7740, 1260, 0, 0));
        // 100% penalty
        assertThat(calculator.calculateRefundWithPromoMoney(sourcePayment, defaultRefundSplit(0)))
                .isEqualTo(fullSplit(0, 0, 0, 0));
    }

    @Test
    public void refundEvent_happyPath_promoCost() {
        // 10_000 (8600 + 1400) order / 3_000 promo code
        FullMoneySplit sourcePayment = fullSplit(5600, 1400, 3000, 0);

        // no penalty
        assertThat(calculator.calculateRefundWithPromoMoney(sourcePayment, defaultRefundSplit(10000)))
                .isEqualTo(fullSplit(5600, 1400, 3000, 0));

        // promo code fully returned with some user money:
        //              total = cost + reward + pr.cost + pr.reward
        // payment      10000 = 5600 +   1400 +    3000 +         0
        // penalty       1000 =  860 +    140 +       0 +         0
        // refund        9000 = 4740 +   1260 +    3000 +         0
        assertThat(calculator.calculateRefundWithPromoMoney(sourcePayment, defaultRefundSplit(9000)))
                .isEqualTo(fullSplit(4740, 1260, 3000, 0));

        // only promo code returned -> should re-distribute penalty money
        //              total = cost + reward + pr.cost + pr.reward
        // payment      10000 = 5600 +   1400 +    3000 +         0
        // penalty       7000 = 6020 +    980 +       0 +         0
        // refund        3000 = -420 +    420 +    3000 +         0
        assertThat(calculator.calculateRefundWithPromoMoney(sourcePayment, defaultRefundSplit(3000)))
                .isEqualTo(fullSplit(-420, 420, 3000, 0));

        // promo code partially returned -> should re-distribute penalty money
        //              total = cost + reward + pr.cost + pr.reward
        // payment      10000 = 5600 +   1400 +    3000 +         0
        // penalty       9000 = 5740 +   1260 +    2000 +         0
        // refund        1000 = -140 +    140 +    1000 +         0
        assertThat(calculator.calculateRefundWithPromoMoney(sourcePayment, defaultRefundSplit(1000)))
                .isEqualTo(fullSplit(-140, 140, 1000, 0));

        // 100% penalty
        assertThat(calculator.calculateRefundWithPromoMoney(sourcePayment, defaultRefundSplit(0)))
                .isEqualTo(fullSplit(0, 0, 0, 0));
    }

    @Test
    public void refundEvent_happyPath_promoCostAndReward() {
        // 10_000 (8600 + 1400) order / 9_000 promo code
        FullMoneySplit sourcePayment = fullSplit(0, 1000, 8600, 400);

        // no penalty
        assertThat(calculator.calculateRefundWithPromoMoney(sourcePayment, defaultRefundSplit(10000)))
                .isEqualTo(fullSplit(0, 1000, 8600, 400));

        // promo code fully returned with some user money:
        //              total = cost + reward + pr.cost + pr.reward
        // payment      10000 =    0 +   1000 +    8600 +       400
        // penalty        500 =  430 +     70 +       0 +         0
        // refund        9500 = -430 +    930 +    8600 +       400
        assertThat(calculator.calculateRefundWithPromoMoney(sourcePayment, defaultRefundSplit(9500)))
                .isEqualTo(fullSplit(-430, 930, 8600, 400));

        // only promo code returned:
        //              total = cost + reward + pr.cost + pr.reward
        // payment      10000 =    0 +   1000 +    8600 +       400
        // penalty       1000 =  860 +    140 +       0 +         0
        // refund        9000 = -860 +    860 +    8600 +       400
        assertThat(calculator.calculateRefundWithPromoMoney(sourcePayment, defaultRefundSplit(9000)))
                .isEqualTo(fullSplit(-860, 860, 8600, 400));

        // promo code partially returned (cost + reward):
        //              total = cost + reward + pr.cost + pr.reward
        // payment      10000 =    0 +   1000 +    8600 +       400
        // penalty       5000 =  300 +    700 +    4000 +         0
        // refund        5000 = -300 +    300 +    4600 +       400
        assertThat(calculator.calculateRefundWithPromoMoney(sourcePayment, defaultRefundSplit(5000)))
                .isEqualTo(fullSplit(-300, 300, 4600, 400));

        // only promo code cost returned:
        //              total = cost + reward + pr.cost + pr.reward
        // payment      10000 =    0 +   1000 +    8600 +       400
        // penalty       9600 =    0 +   1000 +    8256 +       344
        // refund         400 =    0 +      0 +     344 +        56
        assertThat(calculator.calculateRefundWithPromoMoney(sourcePayment, defaultRefundSplit(400)))
                .isEqualTo(fullSplit(0, 0, 344, 56));

        // only promo code cost partially returned:
        //              total = cost + reward + pr.cost + pr.reward
        // payment      10000 =    0 +   1000 +    8600 +       400
        // penalty       9800 =    0 +   1000 +    8428 +       372
        // refund         200 =    0 +      0 +     172 +        28
        assertThat(calculator.calculateRefundWithPromoMoney(sourcePayment, defaultRefundSplit(200)))
                .isEqualTo(fullSplit(0, 0, 172, 28));

        // 100% penalty
        assertThat(calculator.calculateRefundWithPromoMoney(sourcePayment, defaultRefundSplit(0)))
                .isEqualTo(fullSplit(0, 0, 0, 0));
    }

    @Test
    public void refundEvent_happyPath_noUserMoney() {
        // 10_000 order / 10_000 promo code
        FullMoneySplit sourcePayment = fullSplit(0, 0, 8600, 1400);

        // no penalty
        assertThat(calculator.calculateRefundWithPromoMoney(sourcePayment, defaultRefundSplit(10000)))
                .isEqualTo(fullSplit(0, 0, 8600, 1400));
        // some penalty
        assertThat(calculator.calculateRefundWithPromoMoney(sourcePayment, defaultRefundSplit(9000)))
                .isEqualTo(fullSplit(0, 0, 7740, 1260));
        // 100% penalty
        assertThat(calculator.calculateRefundWithPromoMoney(sourcePayment, defaultRefundSplit(0)))
                .isEqualTo(fullSplit(0, 0, 0, 0));
    }

    @Test
    public void refundEvent_supportPath_returnUserMoneyFirst() {
        // 10_000 order / 1_000 promo code
        MoneySplit partnerPaymentMoneySplit = defaultRefundSplit(10000); // same rate as refund
        FullMoneySplit sourcePayment = calculator.calculatePaymentWithPromoMoney(partnerPaymentMoneySplit, rub(1000));
        assertThat(sourcePayment).isEqualTo(fullSplit(7600, 1400, 1000, 0));
        EMoneyRefundMode mode = EMoneyRefundMode.MRM_USER_MONEY_FIRST;

        // no penalty
        assertThat(calculator.calculateRefundWithPromoMoney(sourcePayment, defaultRefundSplit(10000), mode))
                .isEqualTo(fullSplit(7600, 1400, 1000, 0));
        // some penalty & user money left
        assertThat(calculator.calculateRefundWithPromoMoney(sourcePayment, defaultRefundSplit(8000), mode))
                .isEqualTo(fullSplit(6880, 1120, 0, 0));
        // some penalty & no user money left (promo money correction is needed)
        assertThat(calculator.calculateRefundWithPromoMoney(sourcePayment, defaultRefundSplit(9000), mode))
                .isEqualTo(fullSplit(7600, 1400, 140, -140));
        // some penalty & no user money left & some promo code money returned (promo money correction is needed)
        assertThat(calculator.calculateRefundWithPromoMoney(sourcePayment, defaultRefundSplit(9500), mode))
                .isEqualTo(fullSplit(7600, 1400, 570, -70));
        // 100% penalty
        assertThat(calculator.calculateRefundWithPromoMoney(sourcePayment, defaultRefundSplit(0), mode))
                .isEqualTo(fullSplit(0, 0, 0, 0));
    }

    @Test
    public void refundEvent_happyPath_plusCost() {
        // 10_000 (8600 + 1400) order / 3_000 plus points
        FullMoneySplit sourcePayment = fullSplit(5600, 1400, 3000, 0, 0, 0);

        // no penalty
        assertThat(calculator.calculateRefund(sourcePayment, sourcesSplit(0, 0, 0), defaultRefundSplit(10000)))
                .isEqualTo(fullSplit(5600, 1400, 3000, 0, 0, 0));

        // plus points fully returned with some user money:
        //              total = cost + reward + plus cost + plus reward
        // payment      10000 = 5600 +   1400 +      3000 +           0
        // penalty       1000 =  860 +    140 +         0 +           0
        // refund        9000 = 4740 +   1260 +      3000 +           0
        assertThat(calculator.calculateRefund(sourcePayment, sourcesSplit(1000, 0, 0), defaultRefundSplit(9000)))
                .isEqualTo(fullSplit(4740, 1260, 3000, 0, 0, 0));

        // only plus points returned -> should re-distribute penalty money
        //              total = cost + reward + plus cost + plus reward
        // payment      10000 = 5600 +   1400 +      3000 +           0
        // penalty       7000 = 6020 +    980 +         0 +           0
        // refund        3000 = -420 +    420 +      3000 +           0
        assertThat(calculator.calculateRefund(sourcePayment, sourcesSplit(7000, 0, 0), defaultRefundSplit(3000)))
                .isEqualTo(fullSplit(-420, 420, 3000, 0, 0, 0));

        // plus points partially returned -> should re-distribute penalty money
        //              total = cost + reward + plus cost + plus reward
        // payment      10000 = 5600 +   1400 +      3000 +           0
        // penalty       9000 = 5740 +   1260 +      2000 +           0
        // refund        1000 = -140 +    140 +      1000 +           0
        assertThat(calculator.calculateRefund(sourcePayment, sourcesSplit(7000, 2000, 0), defaultRefundSplit(1000)))
                .isEqualTo(fullSplit(-140, 140, 1000, 0, 0, 0));

        // 100% penalty
        assertThat(calculator.calculateRefund(sourcePayment, sourcesSplit(7000, 3000, 0), defaultRefundSplit(0)))
                .isEqualTo(fullSplit(0, 0, 0, 0, 0, 0));
    }

    @Test
    public void refundEvent_happyPath_plusCostAndReward() {
        // 10_000 (8600 + 1400) order / 9_000 plus points
        FullMoneySplit sourcePayment = fullSplit(0, 1000, 8600, 400, 0, 0);

        // no penalty
        assertThat(calculator.calculateRefund(sourcePayment, sourcesSplit(0, 0, 0), defaultRefundSplit(10000)))
                .isEqualTo(fullSplit(0, 1000, 8600, 400, 0, 0));

        // plus points fully returned with some user money:
        //              total = cost + reward + plus cost + plus reward
        // payment      10000 =    0 +   1000 +      8600 +         400
        // penalty        500 =  430 +     70 +         0 +           0
        // refund        9500 = -430 +    930 +      8600 +         400
        assertThat(calculator.calculateRefund(sourcePayment, sourcesSplit(500, 0, 0), defaultRefundSplit(9500)))
                .isEqualTo(fullSplit(-430, 930, 8600, 400, 0, 0));

        // only plus points returned:
        //              total = cost + reward + plus cost + plus reward
        // payment      10000 =    0 +   1000 +      8600 +         400
        // penalty       1000 =  860 +    140 +         0 +           0
        // refund        9000 = -860 +    860 +      8600 +         400
        assertThat(calculator.calculateRefund(sourcePayment, sourcesSplit(1000, 0, 0), defaultRefundSplit(9000)))
                .isEqualTo(fullSplit(-860, 860, 8600, 400, 0, 0));

        // plus points partially returned (cost + reward):
        //              total = cost + reward + plus cost + plus reward
        // payment      10000 =    0 +   1000 +      8600 +         400
        // penalty       5000 =  300 +    700 +      4000 +           0
        // refund        5000 = -300 +    300 +      4600 +         400
        assertThat(calculator.calculateRefund(sourcePayment, sourcesSplit(1000, 4000, 0), defaultRefundSplit(5000)))
                .isEqualTo(fullSplit(-300, 300, 4600, 400, 0, 0));

        // only some plus points returned:
        //              total = cost + reward + plus cost + plus reward
        // payment      10000 =    0 +   1000 +      8600 +         400
        // penalty       9600 =    0 +   1000 +      8256 +         344
        // refund         400 =    0 +      0 +       344 +          56
        assertThat(calculator.calculateRefund(sourcePayment, sourcesSplit(1000, 8600, 0), defaultRefundSplit(400)))
                .isEqualTo(fullSplit(0, 0, 344, 56, 0, 0));

        // 100% penalty
        assertThat(calculator.calculateRefund(sourcePayment, sourcesSplit(1000, 9000, 0), defaultRefundSplit(0)))
                .isEqualTo(fullSplit(0, 0, 0, 0, 0, 0));
    }

    @Test
    public void refundEvent_happyPath_plusAndPromo() {
        // 10_000 (8600 + 1400) order / 1_500 plus points / 1_500 promo points
        FullMoneySplit sourcePayment = fullSplit(5600, 1400, 1500, 0, 1500, 0);

        // no penalty
        assertThat(calculator.calculateRefund(sourcePayment, sourcesSplit(0, 0, 0), defaultRefundSplit(10000)))
                .isEqualTo(fullSplit(5600, 1400, 1500, 0, 1500, 0));

        // promo & plus fully returned:
        //              total = cost + reward + plus cost + plus reward + pr.cost + pr.reward
        // payment      10000 = 5600 +   1400 +      1500 +           0 +    1500 +         0
        // penalty       1000 =  860 +    140 +         0 +           0 +       0 +         0
        // refund        9000 = 4740 +   1260 +      1500 +           0 +    1500 +         0
        assertThat(calculator.calculateRefund(sourcePayment, sourcesSplit(1000, 0, 0), defaultRefundSplit(9000)))
                .isEqualTo(fullSplit(4740, 1260, 1500, 0, 1500, 0));

        // promo & some plus returned:
        //              total = cost + reward + plus cost + plus reward + pr.cost + pr.reward
        // payment      10000 = 5600 +   1400 +      1500 +           0 +    1500 +         0
        // penalty       8000 = 6380 +   1120 +      1000 +           0 +       0 +         0
        // refund        2000 = -280 +    280 +       500 +           0 +    1500 +         0
        assertThat(calculator.calculateRefund(sourcePayment, sourcesSplit(7000, 1000, 0), defaultRefundSplit(2000)))
                .isEqualTo(fullSplit(-280, 280, 500, 0, 1500, 0));

        // only some promo returned:
        //              total = cost + reward + plus cost + plus reward + pr.cost + pr.reward
        // payment      10000 = 5600 +   1400 +      1500 +           0 +    1500 +         0
        // penalty       9000 = 6380 +   1120 +      1500 +           0 +     500 +         0
        // refund        1000 = -140 +    140 +         0 +           0 +    1000 +         0
        assertThat(calculator.calculateRefund(sourcePayment, sourcesSplit(7000, 1500, 500), defaultRefundSplit(1000)))
                .isEqualTo(fullSplit(-140, 140, 0, 0, 1000, 0));

        // 100% penalty
        assertThat(calculator.calculateRefund(sourcePayment, sourcesSplit(7000, 1500, 1500), defaultRefundSplit(0)))
                .isEqualTo(fullSplit(0, 0, 0, 0, 0, 0));
    }

    private static MoneySplit defaultRefundSplit(int total) {
        double partnerRate = 0.86;
        BigDecimal partner = new BigDecimal(total * partnerRate);
        return partnerSplit(partner, total - partner.intValueExact());
    }

    private static MoneySplit partnerView(Number partner, Number fee) {
        return partnerSplit(partner, fee);
    }
}
