package ui_tests.src.test.java.Classes;

import java.util.List;
import java.util.Objects;

public class BonusReason {
    private String title;
    private String code;
    private String defaultPromoValue;
    private String defaultPromoGid;
    private List<String> additionalPromoValue;

    public List<String> getAdditionalPromoValue() {
        return additionalPromoValue;
    }

    public BonusReason setAdditionalPromoValue(List<String> additionalPromoValue) {
        this.additionalPromoValue = additionalPromoValue;
        return this;
    }

    public String getDefaultPromoGid() {
        return defaultPromoGid;
    }

    public BonusReason setDefaultPromoGid(String defaultPromoGid) {
        this.defaultPromoGid = defaultPromoGid;
        return this;
    }

    public String getTitle() {
        return title;
    }

    public BonusReason setTitle(String title) {
        this.title = title;
        return this;
    }

    public String getCode() {
        return code;
    }

    public BonusReason setCode(String code) {
        this.code = code;
        return this;
    }

    public String getDefaultPromoValue() {
        return defaultPromoValue;
    }

    public BonusReason setDefaultPromoValue(String defaultPromoValue) {
        this.defaultPromoValue = defaultPromoValue;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BonusReason that = (BonusReason) o;
        if (this.code!=null){
            if (!this.code.equals(that.code)){
                return false;
            }
        }
        if (this.defaultPromoValue !=null){
            if (!this.defaultPromoValue.equals(that.defaultPromoValue)){
                return false;
            }
        }
        if (this.title!=null){
            if (!this.title.equals(that.title)){
                return false;
            }
        }
        if (this.defaultPromoGid !=null){
            if (!this.defaultPromoGid.equals(that.defaultPromoGid)) {
                return false;
            }
        }

        if (this.defaultPromoValue !=null){
            if (!this.defaultPromoValue.equals(that.defaultPromoValue)) {
                return false;
            }
        }

        if (this.additionalPromoValue !=null){
            if (!this.additionalPromoValue.containsAll(that.additionalPromoValue)){
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        return Objects.hash(title, code, defaultPromoValue);
    }

    @Override
    public String toString() {
        return "BonusReason{" +
                "title='" + title + '\'' +
                ", code='" + code + '\'' +
                ", defaultPromoValue='" + defaultPromoValue + '\'' +
                ", defaultPromoGid='" + defaultPromoGid + '\'' +
                ", additionalPromoGid='" + additionalPromoValue + '\'' +
                '}';
    }
}
