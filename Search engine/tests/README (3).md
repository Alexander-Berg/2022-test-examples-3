# Тесты для парсеров

Тесты используют `pytest`.

Соглашения (на примере гипотетического `FooParser` из модуля `foo_parser`):

* Тесты нужно положить в файл `tests/foo_parser_test.py`.
* Тестовые данные нужно положить в `tests/foo_parser_data/`.
* Тесты можно оформить 
    * Как методы класса `TestFooParser` (отнаследованного от `TestParser` из `tests.test_utils`), названия которых начинаются либо кончаются на `test`. Если хочется проверить результат вызовов `parse` на некоторых вводных данных, этот способ предпочтителен, так как предоставляет метод `parse_file`, автоматически подставляющий соответствующий парсер и нужную директорию для тестовых файлов. Автоматика опирается на указанные соглашения.
    * Как функции, названия которых начинаются либо кончаются на `test`.

Конечно, тест нужно добавить в `ya.make`.
