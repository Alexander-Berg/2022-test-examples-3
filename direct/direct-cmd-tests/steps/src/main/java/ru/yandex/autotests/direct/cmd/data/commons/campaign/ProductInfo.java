package ru.yandex.autotests.direct.cmd.data.commons.campaign;

import com.google.gson.annotations.SerializedName;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

public class ProductInfo {

    @SerializedName("type")
    private String type;

    @SerializedName("currency")
    private String currency;

    @SerializedName("EngineID")
    private String EngineID;

    @SerializedName("ProductID")
    private String ProductID;

    @SerializedName("NDS")
    private String NDS;

    @SerializedName("Price")
    private String Price;

    @SerializedName("product_type")
    private String productType;

    @SerializedName("UnitName")
    private String UnitName;

    @SerializedName("Rate")
    private String Rate;

    /**
     * 
     * @return
     *     The type
     */
    public String getType() {
        return type;
    }

    /**
     * 
     * @param type
     *     The type
     */
    public void setType(String type) {
        this.type = type;
    }

    public ProductInfo withType(String type) {
        this.type = type;
        return this;
    }

    /**
     * 
     * @return
     *     The currency
     */
    public String getCurrency() {
        return currency;
    }

    /**
     * 
     * @param currency
     *     The currency
     */
    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public ProductInfo withCurrency(String currency) {
        this.currency = currency;
        return this;
    }

    /**
     * 
     * @return
     *     The EngineID
     */
    public String getEngineID() {
        return EngineID;
    }

    /**
     * 
     * @param EngineID
     *     The EngineID
     */
    public void setEngineID(String EngineID) {
        this.EngineID = EngineID;
    }

    public ProductInfo withEngineID(String EngineID) {
        this.EngineID = EngineID;
        return this;
    }

    /**
     * 
     * @return
     *     The ProductID
     */
    public String getProductID() {
        return ProductID;
    }

    /**
     * 
     * @param ProductID
     *     The ProductID
     */
    public void setProductID(String ProductID) {
        this.ProductID = ProductID;
    }

    public ProductInfo withProductID(String ProductID) {
        this.ProductID = ProductID;
        return this;
    }

    /**
     * 
     * @return
     *     The NDS
     */
    public String getNDS() {
        return NDS;
    }

    /**
     * 
     * @param NDS
     *     The NDS
     */
    public void setNDS(String NDS) {
        this.NDS = NDS;
    }

    public ProductInfo withNDS(String NDS) {
        this.NDS = NDS;
        return this;
    }

    /**
     * 
     * @return
     *     The Price
     */
    public String getPrice() {
        return Price;
    }

    /**
     * 
     * @param Price
     *     The Price
     */
    public void setPrice(String Price) {
        this.Price = Price;
    }

    public ProductInfo withPrice(String Price) {
        this.Price = Price;
        return this;
    }

    /**
     * 
     * @return
     *     The productType
     */
    public String getProductType() {
        return productType;
    }

    /**
     * 
     * @param productType
     *     The product_type
     */
    public void setProductType(String productType) {
        this.productType = productType;
    }

    public ProductInfo withProductType(String productType) {
        this.productType = productType;
        return this;
    }

    /**
     * 
     * @return
     *     The UnitName
     */
    public String getUnitName() {
        return UnitName;
    }

    /**
     * 
     * @param UnitName
     *     The UnitName
     */
    public void setUnitName(String UnitName) {
        this.UnitName = UnitName;
    }

    public ProductInfo withUnitName(String UnitName) {
        this.UnitName = UnitName;
        return this;
    }

    /**
     * 
     * @return
     *     The Rate
     */
    public String getRate() {
        return Rate;
    }

    /**
     * 
     * @param Rate
     *     The Rate
     */
    public void setRate(String Rate) {
        this.Rate = Rate;
    }

    public ProductInfo withRate(String Rate) {
        this.Rate = Rate;
        return this;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(type).append(currency).append(EngineID).append(ProductID).append(NDS).append(Price).append(productType).append(UnitName).append(Rate).toHashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof ProductInfo) == false) {
            return false;
        }
        ProductInfo rhs = ((ProductInfo) other);
        return new EqualsBuilder().append(type, rhs.type).append(currency, rhs.currency).append(EngineID, rhs.EngineID).append(ProductID, rhs.ProductID).append(NDS, rhs.NDS).append(Price, rhs.Price).append(productType, rhs.productType).append(UnitName, rhs.UnitName).append(Rate, rhs.Rate).isEquals();
    }

}
