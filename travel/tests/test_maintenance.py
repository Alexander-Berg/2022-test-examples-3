# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from builtins import str
from contextlib import contextmanager

import mock
import pytest
from django.db import connection

from travel.rasp.library.python.common23.db.maintenance import read_conf, write_conf, update_conf, swap, UnknownRoleError
from travel.rasp.library.python.common23.tester.utils.replace_setting import replace_setting


@contextmanager
def create_maintenance_db(data):
    """Создаем таблицу conf прямо в тестовой базе."""
    try:
        cursor = connection.cursor()
        cursor.execute('DROP TABLE IF EXISTS `conf`;')
        sql_create = """
            CREATE TABLE `conf` (
                `name` varchar(255) NOT NULL,
                `value` varchar(255) DEFAULT NULL,
                `description` varchar(255) NOT NULL,
                PRIMARY KEY (`name`)
            ) ENGINE=MyISAM DEFAULT CHARSET=utf8
        """

        cursor.execute(sql_create)
        for name, value in data.items():
            cursor.execute("""insert into conf (name, value) values (%s, %s)""", (name, value))

        with replace_setting('MAINTENANCE_DB_ENABLED', True), replace_setting('MAINTENANCE_DB', 'default'):
            yield
    finally:
        cursor = connection.cursor()
        cursor.execute('DROP TABLE `conf`;')


@pytest.mark.dbuser
def test_read_conf():
    data = {'a': '123', 'work_db': 'rasp', 'service_db': 'rasp_3'}
    with create_maintenance_db(data), mock.patch.object(connection, 'close', autospec=connection.close) as m_close:
        assert read_conf() == data
        m_close.assert_called_once_with()


@pytest.mark.dbuser
def test_write_conf():
    data = {'a': '123', 'work_db': 'rasp', 'service_db': 'rasp_3'}
    with create_maintenance_db(data):
        new_data = {'b': '43', 'rasp': 'db43'}
        write_conf(new_data)

        with mock.patch.object(connection, 'close', autospec=connection.close):
            assert read_conf() == new_data


@pytest.mark.dbuser
def test_update_conf():
    data = {'a': '123', 'work_db': 'rasp', 'service_db': 'rasp_3'}
    with create_maintenance_db(data), mock.patch.object(connection, 'close'):
        new_data = {'b': '43', 'rasp': 'db43'}
        update_conf(new_data)
        data.update(new_data)
        assert read_conf() == data


@pytest.mark.dbuser
def test_swap():
    data = {'a': '123', 'work_db': 'rasp', 'service_db': 'rasp_3'}
    with create_maintenance_db(data), mock.patch.object(connection, 'close'):
        with pytest.raises(UnknownRoleError) as ex:
            swap('work_db123', 'service_db')
            assert 'work_db123' in str(ex)

        with pytest.raises(UnknownRoleError) as ex:
            swap('work_db', 'service_db123')
            assert 'service_db123' in str(ex)

        swap('work_db', 'service_db')

        expected = {'a': '123', 'work_db': 'rasp_3', 'service_db': 'rasp'}

        assert read_conf() == expected
