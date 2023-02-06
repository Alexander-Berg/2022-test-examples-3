package dto.requests.checkouter;

@SuppressWarnings("checkstyle:NoWhitespaceBefore")
public enum RearrFactor {
    GLOBAL("combinator=1"),
    ITEMREMOVAL("items_removal_if_missing=1"),
    LAVKA("market_blue_add_delivery_service_options=1;use_delivery_service_id=1"),
    DEFFERED_COURIER("" +
        "market_combinator_deferred_courier_options=1;" +
        "delivery_dates_edit_options_combinator_always=1;" +
        "enable_flat_courier_options=1;" +
        "use_merging_courier_with_deferred=1;" +
        "use_yandex_go_in_deferred_courier=1"
    ),
    EXPRESS(
        "market_express_delivery=1;"
            + "market_show_express_out_of_working_hours=1"
    ),
    COMBINATORONDEMAND("market_combinator_on_demand=1"),
    FORCE_DELIVERY_ID("use_delivery_service_id=1"),
    FASHION("" +
        "enable_cart_split_on_combinator=1;" +
        "market_combinator_split_fashion=1;" +
        "market_combinator_partial_delivery_1p_only=1"
    ),
    RDD_BY_USER("delivery_dates_edit_options_combinator_always=1;enable_flat_courier_options=1"),
    DBS_TO_MARKET_PICKUP("use_dsbs_combinator_response_in_actual_delivery=1;combinator_dsbs_branded_depots=1"),
    ;
    private final String experiment;

    RearrFactor(String experiment) {
        this.experiment = experiment;
    }

    public String getValue() {
        return experiment;
    }
}
