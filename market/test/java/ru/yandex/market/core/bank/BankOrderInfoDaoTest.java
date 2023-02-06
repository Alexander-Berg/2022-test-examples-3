package ru.yandex.market.core.bank;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.order.BankOrderInfoDao;
import ru.yandex.market.core.order.payment.BankOrderInfo;
import ru.yandex.market.core.order.payment.TransactionType;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.market.core.matchers.bank.BankOrderInfoMatcher.hasBankOrderDate;
import static ru.yandex.market.core.matchers.bank.BankOrderInfoMatcher.hasBankOrderId;
import static ru.yandex.market.core.matchers.bank.BankOrderInfoMatcher.hasBankSum;
import static ru.yandex.market.core.matchers.bank.BankOrderInfoMatcher.hasCreationDate;
import static ru.yandex.market.core.matchers.bank.BankOrderInfoMatcher.hasItemCount;
import static ru.yandex.market.core.matchers.bank.BankOrderInfoMatcher.hasItemSum;
import static ru.yandex.market.core.matchers.bank.BankOrderInfoMatcher.hasOfferName;
import static ru.yandex.market.core.matchers.bank.BankOrderInfoMatcher.hasOrderId;
import static ru.yandex.market.core.matchers.bank.BankOrderInfoMatcher.hasPaymentType;
import static ru.yandex.market.core.matchers.bank.BankOrderInfoMatcher.hasServiceOrderId;
import static ru.yandex.market.core.matchers.bank.BankOrderInfoMatcher.hasShopSku;
import static ru.yandex.market.core.matchers.bank.BankOrderInfoMatcher.hasTransactionType;
import static ru.yandex.market.core.matchers.bank.BankOrderInfoMatcher.hasTrantime;
import static ru.yandex.market.core.matchers.bank.BankOrderInfoMatcher.hasTrustId;

@DbUnitDataSet(before = "BankOrderInfoDao.before.csv")
public class BankOrderInfoDaoTest extends FunctionalTest {

    @Autowired
    public BankOrderInfoDao bankOrderInfoDao;

    @DisplayName("Тест на получение информации по платежам по id платежного поручения")
    @Test
    void testBankInfoById() {
        var info = bankOrderInfoDao.getBankOrderInfoByBankId(
                501L,
                485003L,
                LocalDate.parse("2019-03-27"));
        assertThat(info, hasSize(1));
        var item = info.get(0);
        assertThat(item, allOf(
                hasTrantime(LocalDate.parse("2019-03-28")),
                hasBankOrderDate(LocalDate.parse("2019-03-27")),
                hasCreationDate(LocalDate.parse("2017-11-27")),
                hasBankSum(new BigDecimal("1598.50")),
                hasItemSum(new BigDecimal("-1598.50")),
                hasTransactionType(TransactionType.REFUND),
                hasTrustId("trans_id_refund_real_card"),
                hasShopSku("shop_sku_1"),
                hasOfferName("SomeOfferFor501"),
                hasItemCount(2L),
                hasServiceOrderId("5679434-item-769001"),
                hasOrderId(5679434L),
                hasPaymentType("card"),
                hasBankOrderId(485003L)
        ));
    }

    @DisplayName("Тест на получение информации по платежам по id платежного поручения + correction")
    @Test
    void testBankInfoByIdWithCorr() {
        var info = bankOrderInfoDao.getBankOrderInfoByBankId(
                501L,
                20775L,
                LocalDate.parse("2021-12-21"));
        assertThat(info, hasSize(1));
        var item = info.get(0);
        assertThat(item, allOf(
                hasTrantime(LocalDate.parse("2021-12-21")),
                hasBankOrderDate(LocalDate.parse("2021-12-21")),
                hasBankSum(new BigDecimal("352647.00")),
                hasTransactionType(TransactionType.PAYMENT),
                hasTrustId("18432051"),
                hasServiceOrderId("payment-order-589768"),
                hasPaymentType("partner_payment"),
                hasBankOrderId(20775L)
        ));
    }

    @DisplayName("Тест на получение информации по платежам по дате")
    @Test
    void testBankInfoByDate() {
        var info = bankOrderInfoDao.getBankOrderInfoByDate(
                501L,
                LocalDate.parse("2019-01-01"),
                LocalDate.parse("2019-12-31"));
        assertThat(info, hasSize(7));
    }

    @DisplayName("Тест на получение информации по платежам по id платежного поручения для supplier с клоном")
    @Test
    void testBankInfoByIdWithClone() {
        var info = bankOrderInfoDao.getBankOrderInfoByBankId(
                501L,
                485013L,
                LocalDate.parse("2020-01-01"));
        assertThat(info, hasSize(1));
        var item = info.get(0);
        assertThat(item, allOf(
                hasTrantime(LocalDate.parse("2020-01-02")),
                hasBankOrderDate(LocalDate.parse("2020-01-01")),
                hasCreationDate(LocalDate.parse("2017-11-27")),
                hasBankSum(new BigDecimal("3197.00")),
                hasItemSum(new BigDecimal("-1598.50")),
                hasTransactionType(TransactionType.REFUND),
                hasTrustId("trans_id_refund_real_card1"),
                hasShopSku("shop_sku_1"),
                hasOfferName("SomeOfferFor501"),
                hasItemCount(2L),
                hasServiceOrderId("5679434-item-769001"),
                hasOrderId(5679434L),
                hasPaymentType("card"),
                hasBankOrderId(485013L)
        ));
    }

    @DisplayName("Тест на получение информации по платежам по id платежного поручения для supplier с клоном 2")
    @Test
    void testBankInfoByIdWithClone2() {
        var info = bankOrderInfoDao.getBankOrderInfoByBankId(
                502L,
                485013L,
                LocalDate.parse("2020-01-01"));
        assertThat(info, hasSize(1));
        var item = info.get(0);
        assertThat(item, allOf(
                hasTrantime(LocalDate.parse("2020-01-02")),
                hasBankOrderDate(LocalDate.parse("2020-01-01")),
                hasCreationDate(LocalDate.parse("2017-11-27")),
                hasBankSum(new BigDecimal("3197.00")),
                hasItemSum(new BigDecimal("-1598.50")),
                hasTransactionType(TransactionType.REFUND),
                hasTrustId("trans_id_refund_real_card2"),
                hasShopSku("shop_sku_2"),
                hasOfferName("SomeOfferFor502"),
                hasItemCount(2L),
                hasServiceOrderId("5679435-item-769002"),
                hasOrderId(5679435L),
                hasPaymentType("card"),
                hasBankOrderId(485013L)
        ));
    }

    @DisplayName("Тест на получение информации по платежам по id платежного поручения для supplier с клоном. Доставка")
    @Test
    void testBankInfoByIdWithCloneDelivery() {
        var info = bankOrderInfoDao.getBankOrderInfoByBankId(
                502L,
                485014L,
                LocalDate.parse("2020-01-01"));
        assertThat(info, hasSize(1));
        var item = info.get(0);
        assertThat(item, allOf(
                hasTrantime(LocalDate.parse("2020-01-02")),
                hasBankOrderDate(LocalDate.parse("2020-01-01")),
                hasCreationDate(LocalDate.parse("2017-11-27")),
                hasBankSum(new BigDecimal("1598.50")),
                hasItemSum(new BigDecimal("1598.50")),
                hasTransactionType(TransactionType.PAYMENT),
                hasTrustId("trans_id_refund_real_card3"),
                hasShopSku(null),
                hasOfferName(null),
                hasItemCount(null),
                hasServiceOrderId("5679435-delivery"),
                hasOrderId(5679435L),
                hasPaymentType("card"),
                hasBankOrderId(485014L)
        ));
    }

    @DisplayName("Тест на получение информации по платежам по id платежного поручения для supplier с клоном. " +
            "Компенсация")
    @Test
    void testBankInfoByIdWithCloneCompensation() {
        var info = bankOrderInfoDao.getBankOrderInfoByBankId(
                502L,
                485015L,
                LocalDate.parse("2020-01-01"));
        assertThat(info, hasSize(1));
        var item = info.get(0);
        assertThat(item, allOf(
                hasTrantime(LocalDate.parse("2020-01-02")),
                hasBankOrderDate(LocalDate.parse("2020-01-01")),
                hasCreationDate(null),
                hasBankSum(new BigDecimal("1598.50")),
                hasItemSum(new BigDecimal("1598.50")),
                hasTransactionType(TransactionType.PAYMENT),
                hasTrustId("trans_id_refund_real_card4"),
                hasShopSku(null),
                hasOfferName(null),
                hasItemCount(null),
                hasServiceOrderId(null),
                hasOrderId(null),
                hasPaymentType("compensation"),
                hasBankOrderId(485015L)
        ));
    }

    @DisplayName("Тест на получение информации по платежам по дате для supplier с клоном")
    @Test
    void testBankInfoByDateWithClone() {
        var info = bankOrderInfoDao.getBankOrderInfoByDate(
                501L,
                LocalDate.parse("2019-01-01"),
                LocalDate.parse("2020-01-31"));
        assertThat(info, hasSize(8));
    }

    @DisplayName("Тест на получение информации по платежам по дате для supplier с клоном 2")
    @Test
    void testBankInfoByDateWithClone2() {
        var info = bankOrderInfoDao.getBankOrderInfoByDate(
                502L,
                LocalDate.parse("2019-01-01"),
                LocalDate.parse("2020-01-31"));
        assertThat(info, hasSize(3));
    }

    @DisplayName("Тест на получение id платежей по дате для supplier с клоном")
    @Test
    void testGetBankOrderIdsByDate() {
        var ids = bankOrderInfoDao.getBankOrderIdsByDate(
                List.of(501L),
                LocalDate.parse("2019-01-01"),
                LocalDate.parse("2020-01-31")
        ).keySet();
        assertThat(ids, hasSize(8));
    }

    @DisplayName("Тест на получение id платежей по дате для supplier с клоном + corr")
    @Test
    void testGetBankOrderIdsByDateWithCorr() {
        var ids = bankOrderInfoDao.getBankOrderIdsByDate(
                List.of(501L),
                LocalDate.parse("2021-12-20"),
                LocalDate.parse("2021-12-22")
        ).keySet();
        assertThat(ids, hasSize(1));
        assertThat(ids, contains(20775L));
    }

    @DisplayName("Тест на получение id платежей по дате для supplier с клоном 2")
    @Test
    void testGetBankOrderIdsByDate2() {
        var ids = bankOrderInfoDao.getBankOrderIdsByDate(
                List.of(502L),
                LocalDate.parse("2019-01-01"),
                LocalDate.parse("2020-01-31")
        ).keySet();
        assertThat(ids, hasSize(3));
    }

    @DisplayName("Тест на получение информации по платежам по дате для supplier с клоном 2 + corr")
    @Test
    void testBankInfoByDateWithCorrection() {
        var info = bankOrderInfoDao.getBankOrderInfoByDate(
                501L,
                LocalDate.parse("2020-12-20"),
                LocalDate.parse("2020-12-21"));
        assertThat(info, hasSize(1));
        List<BankOrderInfo> infoCorr = info.stream().filter(BankOrderInfo::isCorrection).collect(Collectors.toList());
        assertThat(infoCorr, hasSize(1));
    }

}
