package ru.yandex.direct.core.testing.steps.campaign.model0;

import java.math.BigDecimal;
import java.util.function.BiConsumer;
import java.util.function.Function;

import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.model.Model;
import ru.yandex.direct.model.ModelProperty;

public class BalanceInfo implements Model {

    public static final ModelProperty<BalanceInfo, CurrencyCode> CURRENCY =
            prop("currency", BalanceInfo::getCurrency, BalanceInfo::setCurrency);
    public static final ModelProperty<BalanceInfo, Boolean> CURRENCY_CONVERTED =
            prop("currencyConverted", BalanceInfo::getCurrencyConverted, BalanceInfo::setCurrencyConverted);
    public static final ModelProperty<BalanceInfo, Long> WALLET_CID =
            prop("walletCid", BalanceInfo::getWalletCid, BalanceInfo::setWalletCid);
    public static final ModelProperty<BalanceInfo, BigDecimal> SUM =
            prop("sum", BalanceInfo::getSum, BalanceInfo::setSum);
    public static final ModelProperty<BalanceInfo, BigDecimal> SUM_BALANCE =
            prop("sumBalance", BalanceInfo::getSumBalance, BalanceInfo::setSumBalance);
    public static final ModelProperty<BalanceInfo, BigDecimal> SUM_SPENT =
            prop("sumSpent", BalanceInfo::getSumSpent, BalanceInfo::setSumSpent);
    public static final ModelProperty<BalanceInfo, BigDecimal> SUM_LAST =
            prop("sumLast", BalanceInfo::getSumLast, BalanceInfo::setSumLast);
    public static final ModelProperty<BalanceInfo, BigDecimal> SUM_TO_PAY =
            prop("sumToPay", BalanceInfo::getSumToPay, BalanceInfo::setSumToPay);
    public static final ModelProperty<BalanceInfo, Long> SUM_UNITS =
            prop("sumUnits", BalanceInfo::getSumUnits, BalanceInfo::setSumUnits);
    public static final ModelProperty<BalanceInfo, Long> SUM_SPENT_UNITS =
            prop("sumSpentUnits", BalanceInfo::getSumSpentUnits, BalanceInfo::setSumSpentUnits);
    public static final ModelProperty<BalanceInfo, Long> BALANCE_TID =
            prop("balanceTid", BalanceInfo::getBalanceTid, BalanceInfo::setBalanceTid);
    public static final ModelProperty<BalanceInfo, Boolean> STATUS_NO_PAY =
            prop("statusNoPay", BalanceInfo::getStatusNoPay, BalanceInfo::setStatusNoPay);
    public static final ModelProperty<BalanceInfo, Long> PRODUCT_ID =
            prop("productId", BalanceInfo::getProductId, BalanceInfo::setProductId);
    public static final ModelProperty<BalanceInfo, Boolean> PAID_BY_CERTIFICATE =
            prop("paidByCertificate", BalanceInfo::getPaidByCertificate, BalanceInfo::setPaidByCertificate);

    private static <V> ModelProperty<BalanceInfo, V> prop(String name,
                                                          Function<BalanceInfo, V> getter, BiConsumer<BalanceInfo, V> setter) {
        return ModelProperty.create(BalanceInfo.class, name, getter, setter);
    }

    /**
     * валюта кампании; могут быть настоящие валюты или у.е.;
     * может не совпадать с clients.work_currency
     */
    private CurrencyCode currency;

    /**
     * была ли кампания сконвертирована копированием;
     * при переходе клиента на реальную валюту
     */
    private Boolean currencyConverted;

    /**
     * FK(wallet_campaigns.wallet_cid);
     * cid кампании общего счета привязанного к данной кампании
     */
    private Long walletCid;

    /**
     * всего оплачено, в валюте кампании;
     * для мультивалютных: живых денег, включает в себя НДС;
     * для Баяна: стоимость купленных показов в фишках
     */
    private BigDecimal sum;

    /**
     * Сумма зачислений из биллинга для обработки в NotifyOrder2
     */
    private BigDecimal sumBalance;

    /**
     * потрачено на кампании, в валюте кампании;
     * для мультивалютных: живых денег, включает в себя НДС, после применения скидки;
     * для Баяна: рассчитанная пропорцией фейковая сумма исходя из зачисленной суммы и доли истраченных показов
     */
    private BigDecimal sumSpent;

    /**
     * сумма последней положительной транзакции (платежи и начисления скидок), в валюте кампании;
     * используется для отправки писем о скором окончании средств;
     * для мультивалютных: живых денег, включает в себя НДС
     */
    private BigDecimal sumLast;

    /**
     * Какую сумму намеревался клиент положить на кампанию во время последнего создания счёта, в валюте кампании;
     * для мультивалютных: без НДС, до применения скидки
     */
    private BigDecimal sumToPay;

    /**
     * всего оплачено, в юнитах (для Директа - смысла не имеет, для Баяна - показы)
     */
    private Long sumUnits;

    /**
     * потрачено на кампании, в юнитах (для Директа - смысла не имеет, для Баяна - показы)
     */
    private Long sumSpentUnits;

    /**
     * номер транзакции в биллинге, которому соответствуют sum и sum_units
     */
    private Long balanceTid;

    /**
     * запрещена ли оплата кампании
     */
    private Boolean statusNoPay;

    /**
     * FK(ppcdict.products.ProductID) ID продукта из биллинга;
     * для мультивалютных кампаний, прошедших конвертацию из у.е. без копирования и остановки;
     * в Балансе останется старый (уешный) продукт
     */
    private Long productId;

    /**
     * оплачивалась ли хоть раз кампания сертификатом (используется для оплаты кампаний внутренней рекламы);
     * принимает значения Yes (оплачивалась) и No (не оплачивалась)
     */
    private Boolean paidByCertificate;

    public CurrencyCode getCurrency() {
        return currency;
    }

    public void setCurrency(CurrencyCode currency) {
        this.currency = currency;
    }

    public Boolean getCurrencyConverted() {
        return currencyConverted;
    }

    public void setCurrencyConverted(Boolean currencyConverted) {
        this.currencyConverted = currencyConverted;
    }

    public Long getWalletCid() {
        return walletCid;
    }

    public void setWalletCid(Long walletCid) {
        this.walletCid = walletCid;
    }

    public BigDecimal getSumToPay() {
        return sumToPay;
    }

    public void setSumToPay(BigDecimal sumToPay) {
        this.sumToPay = sumToPay;
    }

    public BigDecimal getSum() {
        return sum;
    }

    public void setSum(BigDecimal sum) {
        this.sum = sum;
    }

    public BigDecimal getSumBalance() {
        return sumBalance;
    }

    public void setSumBalance(BigDecimal sumBalance) {
        this.sumBalance = sumBalance;
    }

    public BigDecimal getSumSpent() {
        return sumSpent;
    }

    public void setSumSpent(BigDecimal sumSpent) {
        this.sumSpent = sumSpent;
    }

    public BigDecimal getSumLast() {
        return sumLast;
    }

    public void setSumLast(BigDecimal sumLast) {
        this.sumLast = sumLast;
    }

    public Long getSumSpentUnits() {
        return sumSpentUnits;
    }

    public void setSumSpentUnits(Long sumSpentUnits) {
        this.sumSpentUnits = sumSpentUnits;
    }

    public Long getSumUnits() {
        return sumUnits;
    }

    public void setSumUnits(Long sumUnits) {
        this.sumUnits = sumUnits;
    }

    public Long getBalanceTid() {
        return balanceTid;
    }

    public void setBalanceTid(Long balanceTid) {
        this.balanceTid = balanceTid;
    }

    public Boolean getStatusNoPay() {
        return statusNoPay;
    }

    public void setStatusNoPay(Boolean statusNoPay) {
        this.statusNoPay = statusNoPay;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public Boolean getPaidByCertificate() {
        return paidByCertificate;
    }

    public void setPaidByCertificate(Boolean paidByCertificate) {
        this.paidByCertificate = paidByCertificate;
    }

    public BalanceInfo withCurrency(CurrencyCode currency) {
        this.currency = currency;
        return this;
    }

    public BalanceInfo withCurrencyConverted(Boolean currencyConverted) {
        this.currencyConverted = currencyConverted;
        return this;
    }

    public BalanceInfo withWalletCid(Long walletCid) {
        this.walletCid = walletCid;
        return this;
    }

    public BalanceInfo withSumToPay(BigDecimal sumToPay) {
        this.sumToPay = sumToPay;
        return this;
    }

    public BalanceInfo withSum(BigDecimal sum) {
        this.sum = sum;
        return this;
    }

    public BalanceInfo withSumBalance(BigDecimal sumBalance) {
        this.sumBalance = sumBalance;
        return this;
    }

    public BalanceInfo withSumSpent(BigDecimal sumSpent) {
        this.sumSpent = sumSpent;
        return this;
    }

    public BalanceInfo withSumLast(BigDecimal sumLast) {
        this.sumLast = sumLast;
        return this;
    }

    public BalanceInfo withSumSpentUnits(Long sumSpentUnits) {
        this.sumSpentUnits = sumSpentUnits;
        return this;
    }

    public BalanceInfo withSumUnits(Long sumUnits) {
        this.sumUnits = sumUnits;
        return this;
    }

    public BalanceInfo withBalanceTid(Long balanceTid) {
        this.balanceTid = balanceTid;
        return this;
    }

    public BalanceInfo withStatusNoPay(Boolean statusNoPay) {
        this.statusNoPay = statusNoPay;
        return this;
    }

    public BalanceInfo withProductId(Long productId) {
        this.productId = productId;
        return this;
    }

    public BalanceInfo withPaidByCertificate(Boolean paidByCertificate) {
        this.paidByCertificate = paidByCertificate;
        return this;
    }
}
