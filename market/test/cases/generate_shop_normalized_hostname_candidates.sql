/* syntax version 1 */
$script = @@from yamarec1.udfs import *@@;
$generate_shop_normalized_hostname_candidates = CustomPython::generate_shop_normalized_hostname_candidates($script);

SELECT $generate_shop_normalized_hostname_candidates("ru.moscow.pokupki.market.yandex.ru")
UNION ALL
SELECT $generate_shop_normalized_hostname_candidates("en.kazan.ozon.ru")
;
