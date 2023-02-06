package ui_tests.src.test.java.Classes;

import java.util.Objects;

public class OrderItem {
    private String accruedCashBack;
    private String title;
    private String cashBackSpent;
    private String plannedCashback;
    private String orderMarketTitle;

    /**
     * Получить плановый кэшбек
     * @return
     */
    public String getPlannedCashback() {
        return plannedCashback;
    }

    /**
     * Указать плановый кэшбек
     * @param plannedCashback плановый кэшбэк
     * @return
     */
    public OrderItem setPlannedCashback(String plannedCashback) {
        this.plannedCashback = plannedCashback;
        return this;
    }

    /**
     * Получить, сколько списано кэшбека
     */

    public String getCashBackSpent() {
        return cashBackSpent;
    }

    /**
     * Указать, сколько списано кэшбека
     */
    public OrderItem setCashBackSpent(String cashBackSpent) {
        this.cashBackSpent = cashBackSpent;
        return this;
    }

    /**
     * Получить значение начисленного кэшбека
     *
     * @return
     */
    public String getAccruedCashBack() {
        return accruedCashBack;
    }

    /**
     * Указать значение кэшбека
     *
     * @param accruedCashBack
     * @return
     */
    public OrderItem setAccruedCashBack(String accruedCashBack) {
        this.accruedCashBack = accruedCashBack;
        return this;
    }

    /**
     * Получить название товара
     *
     * @return
     */
    public String getTitle() {
        return title;
    }

    /**
     * Указать название товара
     *
     * @param title
     * @return
     */
    public OrderItem setTitle(String title) {
        this.title = title;
        return this;
    }

    /**
     * Получить маркер товарной позиции
     *
     * @return
     */
    public String getOrderMarketTitle() {
        return orderMarketTitle;
    }

    /**
     * Указать маркер товарной позиции
     *
     * @param orderMarketTitle
     * @return
     */
    public OrderItem setOrderMarketTitle(String orderMarketTitle) {
        this.orderMarketTitle = orderMarketTitle;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OrderItem orderItem = (OrderItem) o;
        if (this.title != null) {
            if (!this.title.equals(orderItem.title)) {
                return false;
            }
        }
        if (this.accruedCashBack != null) {
            if (!this.accruedCashBack.equals(orderItem.accruedCashBack)) {
                return false;
            }
        }
        if (this.cashBackSpent != null) {
            if (!this.cashBackSpent.equals(orderItem.cashBackSpent)) {
                return false;
            }
        }
        if (this.plannedCashback!=null){
            if (!this.plannedCashback.equals(orderItem.plannedCashback)){
                return false;
            }
        }
        if (this.orderMarketTitle!=null){
            if(!this.orderMarketTitle.equals(orderItem.orderMarketTitle)){
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        return Objects.hash(accruedCashBack, title);
    }

    @Override
    public String toString() {
        return "OrderItem{" +
                "accruedCashBack='" + accruedCashBack + '\'' +
                ", title='" + title + '\'' +
                ", cashBackSpent='" + cashBackSpent + '\'' +
                ", plannedCashback='" + plannedCashback + '\'' +
                ", orderMarketTitle='" + orderMarketTitle + '\'' +
                '}';
    }
}
