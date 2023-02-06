/* syntax version 1 */
$script = @@from yamarec1.udfs import *@@;
$parse_metrika_log_record = CustomPython::parse_metrika_log_record($script);

$make_optional_string = ($value) -> {
    return IF($value IS NULL, NULL, CAST($value AS String));
};

SELECT 
    $parse_metrika_log_record(
        AsDict(
            AsTuple("action", $make_optional_string("click")),
            AsTuple("yandexuid", $make_optional_string("987654321")),
            AsTuple("host", $make_optional_string("m.pokupki.market.yandex.ru")),
            AsTuple("goal", $make_optional_string("BLUE-MARKET-CATALOG_CATALOG_CATEGORY-POPULAR-ITEMS_SNIPPET_NAVIGATE")),
            AsTuple("params", $make_optional_string("{\"cmsPageId\":48776,\"props\":{\"title\":\"aaaa\"},\"cmsWidgetId\":10547902,\"name\":\"ScrollBox\",\"garsons\":[{\"id\":\"PopularProductsByCategory\",\"limit\":15,\"params\":{\"rgb\":\"BLUE\",\"returnZeroIfLess\":6}}],\"entity\":\"sku\",\"skuId\":\"10547902\",\"position\":1,\"reqId\":\"19b67040c48341eb90377d8eff782bce\"}")),
            AsTuple("referrer", $make_optional_string("/")),
            AsTuple("hit_id", $make_optional_string("1067438003")),
            AsTuple("counter_id", $make_optional_string("160656")),
            AsTuple("market_buckets", $make_optional_string("")),
            AsTuple("client_ip", $make_optional_string("::ffff:91.107.117.120")),
            AsTuple("user_interface", $make_optional_string("TOUCH"))
        ));
