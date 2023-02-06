/* syntax version 1 */
$script = @@from yamarec1.udfs import *@@;
$make_host_suffixes = CustomPython::make_host_suffixes($script);

SELECT $make_host_suffixes("ru.moscow.pokupki.market.yandex.ru")
UNION ALL
SELECT $make_host_suffixes("en.kazan.ozon.ru")
;
