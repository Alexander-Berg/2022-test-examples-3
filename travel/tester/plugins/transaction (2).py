# -*- coding: utf-8 -*-

import mock
import pytest
import inspect

from django.db import connections

from travel.avia.stat_admin.tester.testcase import TestCase
from travel.avia.stat_admin.tester import transaction_context
from travel.avia.stat_admin.tester.initializer import reinit_db
from travel.avia.stat_admin.tester.transaction_context import enter_atomic, rollback_atomic


def pytest_configure(config):
    # register markers
    config.addinivalue_line(u'markers',
                            u'dbuser: Запускает тест функцию в транзакции, откатывает транзакцию после теста')
    config.addinivalue_line(u'markers',
                            u'dbripper: Нужно для запуска тестов, которые ломают базу,'
                            u' база реинициализируется после теста')
    config.addinivalue_line(u'markers',
                            u'dbignore: Тесты не ломают базу, но используют коннект. Просто закрываем коннект в конце')


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

        patcher = mock.patch('django.db.backends.mysql.base.DatabaseWrapper.cursor', side_effect=side_effect)
        patcher.start()

    try:
        yield
    finally:
        if is_dbuser:
            rollback_atomic(atomic)
        elif is_dbignore or is_dbripper:
            for connection in connections.all():
                connection.close()
        else:
            patcher.stop()


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

        print '\nReinitialize database'
        reinit_db()
        print 'Reinitialization done'
