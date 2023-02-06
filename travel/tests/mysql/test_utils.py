# coding: utf8
from __future__ import absolute_import, division, print_function, unicode_literals

import mock

from contextlib import contextmanager

from travel.rasp.library.python.db.mysql.utils import get_master_host_from_conn


def test_get_master_host_from_conn():
    m_conn = mock.Mock()
    m_cursor = mock.Mock()

    @contextmanager
    def with_cursor():
        yield m_cursor

    m_conn.cursor = with_cursor

    m_cursor.fetchall.side_effect = [
        [
            ['Waiting for master to send event', 'sas-50yfsknyaz6wun6d.db.yandex.net', 'repl', ]  # тут еще много колонок, они не важны
        ],
        []
    ]

    assert get_master_host_from_conn(m_conn) == 'sas-50yfsknyaz6wun6d.db.yandex.net'
    assert get_master_host_from_conn(m_conn) is None
