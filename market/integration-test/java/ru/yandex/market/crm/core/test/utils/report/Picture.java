package ru.yandex.market.crm.core.test.utils.report;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author apershukov
 */
class Picture {

    @JsonProperty("original")
    private Image original;

    public Image getOriginal() {
        return original;
    }

    public void setOriginal(Image original) {
        this.original = original;
    }
}
