package ru.yandex.autotests.direct.cmd.data.excel;

/*
* todo javadoc
*/
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import javax.annotation.Generated;

@Generated("org.jsonschema2pojo")
public class ParseWarningsForExistsCampModel {

    @SerializedName("different_currency")
    @Expose
    private String differentCurrency;

    /**
     *
     * @return
     * The differentCurrency
     */
    public String getDifferentCurrency() {
        return differentCurrency;
    }

    /**
     *
     * @param differentCurrency
     * The different_currency
     */
    public void setDifferentCurrency(String differentCurrency) {
        this.differentCurrency = differentCurrency;
    }

}
