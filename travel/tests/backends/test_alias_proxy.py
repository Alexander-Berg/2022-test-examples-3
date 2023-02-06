# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from threading import Thread

import mock
import pytest
from django.db import connection, connections
from mock import sentinel

import travel.rasp.library.python.common23.db.backends.alias_proxy.base as alias_proxy_base
from travel.rasp.library.python.common23.db.backends.alias_proxy.base import DatabaseWrapper, in_main_thread
from travel.rasp.library.python.common23.db.switcher import switcher

from travel.rasp.library.python.common23.tester.utils.django_databases import mock_django_connection


class FakeWrapper(object):
    def __init__(self):
        self.settings_dict = {}

    def get_new_connection(self, conn_params):
        assert conn_params is sentinel.params
        return connection

    def get_connection_params(self):
        return sentinel.connection_params

    def get_db_name(self):
        return sentinel.db_name

    def get_hosts(self):
        return sentinel.hosts

    def get_all_hosts(self):
        return sentinel.all_hosts

    def get_cluster(self):
        return sentinel.cluster

    def get_connection_to_host(self, host):
        return sentinel.connection_to_host, host


def test_getattr():
    """
    Проверяем, что конструктор правильно получает атрибут из модуля,
    что атрибут используется как callable для получения алиаса,
    и что с помощью него получаем из connections нужный объект проксируемого враппера.
    """
    fake_wrapper = FakeWrapper()
    with mock.patch('travel.rasp.library.python.common23.db.backends.alias_proxy.base.connections', {'alias42': fake_wrapper}):
        dw = DatabaseWrapper({'ALIAS_GETTER': 'travel.rasp.library.python.common23.db.tests.backends.get_alias'})
        conn = dw.get_new_connection(sentinel.params)
        assert conn is connection

        assert dw.get_connection_params() == sentinel.connection_params
        assert dw.get_db_name() == sentinel.db_name
        assert dw.get_hosts() == sentinel.hosts
        assert dw.get_all_hosts() == sentinel.all_hosts
        assert dw.get_cluster() == sentinel.cluster

        host = 'host1'
        assert dw.get_connection_to_host(host) == (sentinel.connection_to_host, host)

        # проверяем, что settings_dict берется из FakeWrapper'а, и что перезапись на это не влияет
        dw.settings_dict = {}
        assert dw.settings_dict is fake_wrapper.settings_dict


@pytest.mark.dbuser
class TestCloseOnSwitchSignal(object):
    def test_close_on_switch_signal(self):
        with mock.patch('travel.rasp.library.python.common23.db.backends.alias_proxy.base.connections', {'alias42': connection}):
            dw = DatabaseWrapper({
                'ALIAS_GETTER': 'travel.rasp.library.python.common23.db.tests.backends.get_alias',
                'CLOSE_ON_SWITCH': True
            })

            try:
                dw.ensure_connection()
                conn = dw.connection
                assert conn

                # после свитча должен вызваться close, а потом соединение переоткрыться
                with mock.patch.object(dw, 'close', wraps=dw.close) as m_close:
                    switcher.db_switched.send(switcher)
                    m_close.assert_called_once_with()
                    assert not dw.connection
            finally:
                # убираем подписку нашего локального dw, чтобы не влияло на другие тесты
                switcher.db_switched.disconnect(dw.on_db_switched)

    def test_no_close_on_switch_signal_in_thread(self):
        # проверяем, что ничего не пытается закрываться для DatabaseWrapper в потоках
        with mock.patch('travel.rasp.library.python.common23.db.backends.alias_proxy.base.connections', {'alias42': connection}):
            with mock.patch.object(alias_proxy_base, 'in_main_thread') as m_in_main_thread:
                m_in_main_thread.return_value = False
                dw = DatabaseWrapper({
                    'ALIAS_GETTER': 'travel.rasp.library.python.common23.db.tests.backends.get_alias',
                    'CLOSE_ON_SWITCH': True
                })

            try:
                dw.ensure_connection()
                conn = dw.connection
                assert conn

                with mock.patch.object(dw, 'close', wraps=dw.close) as m_close:
                    switcher.db_switched.send(switcher)
                    assert len(m_close.call_args_list) == 0
                    assert dw.connection
            finally:
                # убираем подписку нашего локального dw, чтобы не влияло на другие тесты
                switcher.db_switched.disconnect(dw.on_db_switched)


in_main_thread_check = None


def test_in_main_thread():
    def check():
        global in_main_thread_check
        in_main_thread_check = in_main_thread()

    check()
    assert in_main_thread_check is True

    thread = Thread(target=check)
    thread.start()
    thread.join()

    assert in_main_thread_check is False


@pytest.mark.dbripper
def test_chain_proxy_connection():
    """
    Проверяем проксирование алиасов proxy1 -> alias42 -> default
    - proxy1 должен устанавливать соединение туда же, куда default
    - но это должны быть разные соединения
    """

    proxy_1_conf = {
        'ENGINE': 'travel.rasp.library.python.common23.db.backends.alias_proxy',
        'ALIAS_GETTER': 'travel.rasp.library.python.common23.db.tests.backends.get_alias'}
    alias42_conf = {
        'ENGINE': 'travel.rasp.library.python.common23.db.backends.alias_proxy',
        'ALIAS_GETTER': 'travel.rasp.library.python.common23.db.tests.backends.get_alias_to_default'}

    mock_proxy1 = mock_django_connection('proxy1', proxy_1_conf)
    mock_proxy2 = mock_django_connection('alias42', alias42_conf)
    with mock_proxy1, mock_proxy2:
        try:
            cursor_default = connections['default'].cursor()
            cursor_default.execute('drop table if exists `flags1`;')
            cursor_default.execute('create table if not exists flags1 (name text, state integer)')

            # делаем запись через default, но не коммитим
            cursor_default.execute('set autocommit=0;')

            cursor_default.execute('insert into flags1 values ("value42", 42)')

            # создается соединение через proxy1
            cursor_proxy = connections['proxy1'].cursor()
            assert connections['proxy1'].connection != connections['default'].connection

            # в proxy1 не видно иземенений, т.к. транзакция не закоммичена
            cursor_proxy.execute('select state from flags1 where name="value42"')
            result = list(cursor_proxy.fetchall())
            assert len(result) == 0

            # коммитим - видим изменение
            cursor_default.execute('commit')
            cursor_proxy.execute('select state from flags1 where name="value42"')
            result = list(cursor_proxy.fetchall())
            assert len(result) == 1
            assert result[0][0] == 42
        finally:
            connections['default'].cursor().execute('drop table if exists `flags1`;')
