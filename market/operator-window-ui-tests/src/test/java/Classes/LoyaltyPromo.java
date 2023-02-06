package ui_tests.src.test.java.Classes;

public class LoyaltyPromo {
    private String code;
    private String promoId;
    private String promoValue;
    private String condition;

    /**
     * Получить код
     * @return
     */
    public String getCode() {
        return code;
    }

    /**
     * Указать код элемента
     * @param code
     * @return
     */
    public LoyaltyPromo setCode(String code) {
        this.code = code;
        return this;
    }

    public String getPromoId() {
        return promoId;
    }

    public String getCondition() {
        return condition;
    }

    public LoyaltyPromo setCondition(String condition) {
        this.condition = condition;
        return this;
    }

    public LoyaltyPromo setPromoId(String promoId) {
        this.promoId = promoId;
        return this;
    }

    public String getPromoValue() {
        return promoValue;
    }


    /**
     * указать номинал
     * @param promoValue номинал акции. Указывается в формате NN,NN
     * @return
     */
    public LoyaltyPromo setPromoValue(String promoValue) {
        this.promoValue = promoValue;
        return this;
    }

    @Override
    public String toString() {
        return "LoyaltyPromo{" +
                "code='" + code + '\'' +
                ", promoId='" + promoId + '\'' +
                ", promoValue='" + promoValue + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object promo) {
        if (this == promo) return true;
        if (promo == null || getClass() != promo.getClass()) return false;
        LoyaltyPromo that = (LoyaltyPromo) promo;
        if (this.promoId !=null){
            if (!this.promoId.equals(that.promoId)){
                return false;
            }
        }
        if (this.code !=null){
            if (!this.code.equals(that.code)){
                return false;
            }
        }
        if (this.promoValue!=null){
            if (!this.promoValue.equals(that.promoValue)){
                return false;
            }
        }
        if (this.condition!=null){
            if (!this.condition.equals(that.condition)){
                return false;
            }
        }
        return true;
    }

}
