# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import mock
import pytest

from travel.rasp.library.python.common23.db import connect as common_db_connect
from travel.rasp.library.python.common23.db.connect import ConnectionWrapper, get_connection, get_params, Database


def test_connection_wrapper():
    class FakeConnection(object):
        attr = 123

    params = {'some_item': 42}
    cw = ConnectionWrapper(FakeConnection(), params)

    assert cw.conn_params == params
    assert cw.attr == 123


@pytest.mark.dbuser
def test_get_connection():
    with mock.patch.object(common_db_connect, 'get_params', autospec=True) as m_get_params, \
            mock.patch.object(Database, 'connect', autospec=True) as m_db_connect:

        m_get_params.return_value = {'host': 42, 'db': 43}
        m_db_connect.return_value = fake_connection = mock.MagicMock()

        result = get_connection(1, 2, 3)

        m_get_params.assert_called_once_with(1, 2, 3)
        m_db_connect.assert_called_once_with(host=42, db=43)

        assert isinstance(result, ConnectionWrapper)
        assert result.connection == fake_connection
        assert result.conn_params == {'host': 42, 'db': 43}


def test_get_params():
    result = get_params({
        'HOST': 'host42',
        'PORT': '14',
        'NAME': 'some name',
        'USER': 'usr',
        'PASSWORD': 'pwd',
        'OPTIONS': {'a': 1, 'b': 2},
    })
    assert result['host'] == 'host42'
    assert result['port'] == 14
    assert result['db'] == 'some name'
    assert result['user'] == 'usr'
    assert result['passwd'] == 'pwd'
    assert result['a'] == 1
    assert result['b'] == 2

    result = get_params({'HOST': '/host42'})
    assert result['unix_socket'] == '/host42'
    assert 'host' not in result

    result = get_params({'HOST': 'host42', 'NAME': 'db1'}, host='host43', db_name='db2')
    assert result['host'] == 'host43'
    assert result['db'] == 'db2'
