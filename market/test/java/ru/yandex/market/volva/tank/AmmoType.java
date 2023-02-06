package ru.yandex.market.volva.tank;

import lombok.Getter;

/**
 * @author dzvyagin
 */
public enum AmmoType {
    ADDRESSES("addresses"),
    CATEGORIES("categories"),
    EVENTS("events"),
    FAQ("faq"),
    FLAGS("flags"),
    OFFER_INFO("offer-info"),
    OFFER_SIMILAR("offer-similar"),
    ORDERS("orders"),
    SEARCH_2("search2"),
    SUBSCRIPTIONS("subscriptions"),
    SUGGEST("suggest"),
    TOP_SEARCHES("top-searches"),
    USER_PROFILE("user/profile"),
    UNKNOWN("");

    @Getter
    private final String endpoint;

    AmmoType(String endpoint){
        this.endpoint = endpoint;
    }

    public static AmmoType fromId(String id){
        for (AmmoType ammoType : AmmoType.values()){
            if (ammoType != UNKNOWN){
                if (id.contains(ammoType.getEndpoint())){
                    return ammoType;
                }
            }
        }
        return AmmoType.UNKNOWN;
    }
}
