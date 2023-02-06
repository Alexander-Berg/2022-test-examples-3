# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import inspect

import mock
import pytest
from django.db import connections, reset_queries

from travel.rasp.library.python.common23.tester import transaction_context
from travel.rasp.library.python.common23.tester.initializer import reinit_db
from travel.rasp.library.python.common23.tester.testcase import TestCase
from travel.rasp.library.python.common23.tester.transaction_context import enter_atomic, rollback_atomic


def pytest_configure(config):
    # register markers
    config.addinivalue_line('markers',
                            'dbuser: Запускает тест функцию в транзакции, откатывает транзакцию после теста')
    config.addinivalue_line('markers',
                            'dbripper: Нужно для запуска тестов, которые ломают базу,'
                            ' база реинициализируется после теста')
    config.addinivalue_line('markers',
                            'dbignore: Тесты не ломают базу, но используют коннект. Просто закрываем коннект в конце')


@pytest.mark.hookwrapper
def pytest_pyfunc_call(pyfuncitem):
    """
    Позволяет запускать обычные тесты pytest в transaction_context и использовать преимущества pytest

    @pytest.mark.dbuser
    @pytest.mark.parametrize("param", [1,2,3])
    def test_something(param):
        pass

    FIXME: замокать django.db.connection, чтобы не было случайных использований базы данных
    """
    is_dbuser = 'dbuser' in pyfuncitem.keywords
    is_dbignore = 'dbignore' in pyfuncitem.keywords
    is_dbripper = 'dbripper' in pyfuncitem.keywords
    if is_dbuser:
        atomic = enter_atomic()
    elif is_dbignore or is_dbripper:
        pass
    else:
        def side_effect():
            pytest.fail('Use pytest.mark.dbuser to access database inside pytest tests')

        patcher = mock.patch('travel.rasp.library.python.common23.db.backends.mysql.base.DatabaseWrapper.cursor', side_effect=side_effect)
        patcher.start()

    try:
        yield
    finally:
        if is_dbuser:
            rollback_atomic(atomic)
            reset_queries()
        elif is_dbignore or is_dbripper:
            for connection in connections.all():
                connection.close()
        else:
            patcher.stop()


"""
@pytest.mark.dbripper
def test_something():
    pass
Можно использовать в классе, только нужно импортировать обычный TestCase из unittest
@pytest.mark.dbripper
class TestSomething(TestCase):
    def setup_class(self):
        print 'setup_class'

    def test_1(self):
        pass

    def test_2(self):
        pass
"""


def pytest_runtest_call(item):
    if 'dbripper' in item.keywords:
        if hasattr(item, 'parent') and (
            (inspect.isclass(item.parent.obj) and issubclass(item.parent.obj, TestCase))
        ):
            pytest.fail(u'DB Ripper не должен использоваться в наследнике {}'.format(TestCase))

        if transaction_context.is_started():  # FIXME: Не работает с атомиками
            pytest.fail(u'DB Ripper не должен использовать фикстуры с transaction_context')


def pytest_runtest_teardown(item):
    if 'dbripper' in item.keywords:
        if transaction_context.is_started():  # FIXME: Не работает с атомиками
            pytest.fail(u'DB Ripper не должен использовать transaction_context')

        print('\nReinitialize database')
        reinit_db()
        print('Reinitialization done')
