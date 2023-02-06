package ui_tests.src.test.java.Classes.order;


import java.util.HashMap;

public class PaymentProperties {
    // Плательщик
    private String payer;
    // Итого за товары
    private String orderAmount;
    // Стоимость доставки
    private String costDelivery;
    // Общая стоимость
    private String totalCostOrder;
    // Способ оплаты
    private String typePayment;
    // Способ платежа и сумма платежа
    private HashMap<String, String> typePaymentAndPaymentAmount;
    // Накопленный кэшбэк
    private String accruedCashBack;
    // Списано кэшбека
    private String cashBackSpent;

    public String getCashBackSpent() {
        return cashBackSpent;
    }

    public PaymentProperties setCashBackSpent(String cashBackSpent) {
        this.cashBackSpent = cashBackSpent;
        return this;
    }

    /**
     * Получить значение Накопленный кэшбэк
     *
     * @return
     */
    public String getAccruedCashBack() {
        return accruedCashBack;
    }

    /**
     * Указать значение Накопленный кэшбэк
     *
     * @param accruedCashBack Накопленный кэшбэк
     * @return
     */
    public PaymentProperties setAccruedCashBack(String accruedCashBack) {
        this.accruedCashBack = accruedCashBack;
        return this;
    }

    /**
     * Задать способы платежа и сумму платежа
     *
     * @param map - мапа с способом платежа и суммой платежа
     * @return
     */
    public PaymentProperties setTypePaymentAndPaymentAmount(HashMap<String, String> map) {
        typePaymentAndPaymentAmount = map;
        return this;
    }

    /**
     * Получить способы платежа и сумму платежа
     *
     * @return
     */
    public HashMap<String, String> getTypePaymentAndPaymentAmount() {
        return typePaymentAndPaymentAmount;
    }


    /**
     * получить значение поля  Плательщик
     *
     * @return значение поля  Плательщик
     */
    public String getPayer() {
        return payer;
    }

    /**
     * Указать Плательщика
     *
     * @param payer значение поля Плательщик
     * @return
     */
    public PaymentProperties setPayer(String payer) {
        this.payer = payer;
        return this;
    }

    /**
     * Получить Итого за товары
     *
     * @return значение поля Итого за товары
     */
    public String getOrderAmount() {
        return orderAmount;
    }

    /**
     * Указать Итого за товары
     *
     * @param orderAmount значение поля "Итого за товары"
     * @return
     */
    public PaymentProperties setOrderAmount(String orderAmount) {
        this.orderAmount = orderAmount;
        return this;
    }

    /**
     * Получить значение поля Стоимость доставки
     *
     * @return значение поля Стоимость доставки
     */
    public String getCostDelivery() {
        return costDelivery;
    }

    /**
     * Получить значение поля Стоимость доставки
     *
     * @param costDelivery значение поля Стоимость доставки
     * @return
     */
    public PaymentProperties setCostDelivery(String costDelivery) {
        this.costDelivery = costDelivery;
        return this;
    }

    /**
     * Получить значение поля Общая стоимость заказа
     *
     * @return значение поля Общая стоимость заказа
     */
    public String getTotalCostOrder() {
        return totalCostOrder;
    }

    /**
     * Указать значение поля Общая стоимость заказа
     *
     * @param totalCostOrder значение поля Общая стоимость заказа
     * @return
     */
    public PaymentProperties setTotalCostOrder(String totalCostOrder) {
        this.totalCostOrder = totalCostOrder;
        return this;
    }

    /**
     * Получить значене поля Тип оплаты
     *
     * @return значене поля Тип оплаты
     */
    public String getTypePayment() {
        return typePayment;
    }

    /**
     * Способ оплаты
     *
     * @param typePayment
     * @return
     */
    public PaymentProperties setTypePayment(String typePayment) {
        this.typePayment = typePayment;
        return this;
    }

    @Override
    public boolean equals(Object actual) {
        if (actual == this) {
            return true;
        }
        if (actual == null || actual.getClass() != this.getClass()) {
            return false;
        }

        PaymentProperties actualPaymentProperties = (PaymentProperties) actual;

        if (this.payer != null) {
            if (!payer.equals(actualPaymentProperties.payer)) {
                return false;
            }
        }
        if (this.orderAmount != null) {
            if (!orderAmount.equals(actualPaymentProperties.orderAmount)) {
                return false;
            }
        }
        if (this.costDelivery != null) {
            if (!costDelivery.equals(actualPaymentProperties.costDelivery)) {
                return false;
            }
        }
        if (this.totalCostOrder != null) {
            if (!totalCostOrder.equals(actualPaymentProperties.totalCostOrder)) {
                return false;
            }
        }
        if (this.typePayment != null) {
            if (!typePayment.equals(actualPaymentProperties.typePayment)) {
                return false;
            }
        }
        if (this.typePaymentAndPaymentAmount != null) {
            if (!typePaymentAndPaymentAmount.equals(actualPaymentProperties.getTypePaymentAndPaymentAmount())) {
                return false;
            }
        }

        if (this.accruedCashBack != null) {
            if (!accruedCashBack.equals(actualPaymentProperties.accruedCashBack)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public String toString() {
        return "PaymentProperties{" +
                "payer='" + payer + '\'' +
                ", orderAmount='" + orderAmount + '\'' +
                ", costDelivery='" + costDelivery + '\'' +
                ", totalCostOrder='" + totalCostOrder + '\'' +
                ", typePayment='" + typePayment + '\'' +
                ", typePaymentAndPaymentAmount=" + typePaymentAndPaymentAmount +
                ", accruedCashBack='" + accruedCashBack + '\'' +
                '}';
    }
}
