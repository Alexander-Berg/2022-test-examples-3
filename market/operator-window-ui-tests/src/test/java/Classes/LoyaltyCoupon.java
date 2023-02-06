package ui_tests.src.test.java.Classes;

import java.util.Objects;

public class LoyaltyCoupon {
    private String discount;
    private String nominal;
    private String couponValueType;
    private String startDate;
    private String dueDate;
    private String restrictions;

    public String getDiscount() {
        return discount;
    }

    public LoyaltyCoupon setDiscount(String discount) {
        this.discount = discount;
        return this;
    }

    public String getNominal() {
        return nominal;
    }

    public LoyaltyCoupon setNominal(String nominal) {
        this.nominal = nominal;
        return this;
    }

    public String getCouponValueType() {
        return couponValueType;
    }

    public LoyaltyCoupon setCouponValueType(String couponValueType) {
        this.couponValueType = couponValueType;
        return this;
    }

    public String getStartDate() {
        return startDate;
    }

    public LoyaltyCoupon setStartDate(String startDate) {
        this.startDate = startDate;
        return this;
    }

    public String getDueDate() {
        return dueDate;
    }

    public LoyaltyCoupon setDueDate(String dueDate) {
        this.dueDate = dueDate;
        return this;
    }

    public String getRestrictions() {
        return restrictions;
    }

    public LoyaltyCoupon setRestrictions(String restrictions) {
        this.restrictions = restrictions;
        return this;
    }

    @Override
    public String toString() {
        return "LoyaltyCoupon{" +
                "discount='" + discount + '\'' +
                ", nominal='" + nominal + '\'' +
                ", couponValueType='" + couponValueType + '\'' +
                ", startDate='" + startDate + '\'' +
                ", dueDate='" + dueDate + '\'' +
                ", restrictions='" + restrictions + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LoyaltyCoupon that = (LoyaltyCoupon) o;
        if (this.couponValueType!=null){
            if (!this.couponValueType.equals(that.couponValueType)){
                return false;
            }
        }
        if (this.discount!=null){
            if (!this.discount.equals(that.discount)){
                return false;
            }
        }
        if (this.dueDate!=null){
            if (!this.dueDate.equals(that.dueDate)){
                return false;
            }
        }
        if (this.nominal!=null){
            if (!this.nominal.equals(that.nominal)){
                return false;
            }
        }
        if (this.restrictions!=null){
            if (!this.restrictions.equals(that.restrictions)){
                return false;
            }
        }
        if (this.startDate!=null){
            if (!this.startDate.equals(that.startDate)){
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        return Objects.hash(discount, nominal, couponValueType, startDate, dueDate, restrictions);
    }
}
