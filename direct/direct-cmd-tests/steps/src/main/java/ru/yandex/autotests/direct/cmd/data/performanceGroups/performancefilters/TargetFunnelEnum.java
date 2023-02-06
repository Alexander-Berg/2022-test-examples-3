package ru.yandex.autotests.direct.cmd.data.performanceGroups.performancefilters;

public enum TargetFunnelEnum {
    PRODUCT_PAGE_VISIT("product_page_visit"),
    NEW_AUDITORY("new_auditory"),
    SAME_PRODUCTS("same_products");

    private String value;

    TargetFunnelEnum(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
