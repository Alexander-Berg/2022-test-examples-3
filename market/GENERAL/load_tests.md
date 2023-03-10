# Нагрузочное тестирование

Нагрузочные тесты написаны на [Go](https://golang.org/), код лежит в аркадии [wms-load](https://a.yandex-team.ru/arc/trunk/arcadia/market/logistics/wms-load)
Технические подробности про тесты можно почитать в [README.md](https://a.yandex-team.ru/arc/trunk/arcadia/market/logistics/wms-load/README.md)

## TL;DR

Сейчас есть несколько сценариев нагрузочного тестирования: приемка, отгрузка, админка приемки.
Тесты работают в два этапа. Первый этап - генерация патронов. На выходе получается несколько текстовых файлов с патронами и файл конфигурации для [Pandora](https://wiki.yandex-team.ru/Load/Pandora/)
Второй этап - стрельба, т. е. отправка http-запросов из патронов в тестируемый сервис.
На втором этапе запускаются пушки, начинают брать патроны из файлов и отправлять запросы

Запустить тестирование можно с помощью bash-скриптов из папки [wms-load/sc](https://a.yandex-team.ru/arc/trunk/arcadia/market/logistics/wms-load/sc).
Скрипты запускаются локально. Запросы в тестируемый сервис можно отправлять либо локально либо с использованием танка.
Локальный запуск:
```
./with_gen.sh sc/<scenario>/<filename>
```
Запуск через танк:
```
./with_tank.sh sc/<scenario>/<filename>
```
В последнем случае будет собран код, сгенерированны и загружены в Sandbox патроны и запущено тестирование в Лунапарке.

Результаты стрельб можно увидеть на [дашборде в Графане](https://grafana.yandex-team.ru/d/CUM8zQV7z/wms-load-health?orgId=1&refresh=1m), в phout-файлах и в отчете в Лунапарке при запуске в танке.


