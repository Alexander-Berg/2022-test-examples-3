/* syntax version 1 */
$script = @@from yamarec1.udfs import *@@;
$strip_mobile_and_www_prefixes = CustomPython::strip_mobile_and_www_prefixes($script);

SELECT $strip_mobile_and_www_prefixes("m.pokupki.market.yandex.ru/product/naushniki-apple-airpods-2-bez-besprovodnoi-zariadki-chekhla-white/100607784828?show-uid=1111")
UNION ALL
SELECT $strip_mobile_and_www_prefixes("www.ozon.ru/context/detail/id/152094543/")
;
