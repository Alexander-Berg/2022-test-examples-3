import flaky
import yatest.common
import json
from datetime import datetime

import pytest


def flaky_test(retries=3):
    """
    Декоратор предписывает запускать тест повторно, если он не прошёл с первого
    раза.
    Тест будет запускаться до первого успешного прохода или пока не кончатся
    попытки.
    Можно использовать как с целым классом TestCase, так и отдельными тестам
    в нём.
    Например,
    @flaky_test(retries=3)
    class MyTest(TestCase):
        def test1(self):
            assert False
        @flaky_test(retries=2)
        def test2(self):
            assert False
    test1 будет запущен 3 раза
    test2 будет запущен 2 раза
    Одна попытка запуска теста включает в себя:
        - setUp
        - вызов теста
        - tearDown
    """

    return flaky.flaky(max_runs=retries)


@flaky_test(3)
class BaseTestClass:
    DEFAULT_TIMEOUT = int(yatest.common.get_param("timeout", 300))


