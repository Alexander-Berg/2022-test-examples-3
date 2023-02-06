# coding: utf-8
from __future__ import absolute_import, division, print_function, unicode_literals

import pytest
from django.test.utils import override_settings

try:
    import ydb.public.api.python.common  # noqa
except ImportError:
    pytestmark = pytest.mark.skip('kikimr client is not installed')

try:
    from common.db.kikimr.client import get_client, get_table_path
except ImportError:
    pass


def test_get_client(m_kikimr_client):
    with override_settings(KIKIMR_HOST='dummy-kikimr-host', KIKIMR_PORT=100500):
        assert get_client() == m_kikimr_client('dummy-kikimr-host', 100500)


def test_get_table_path():
    with override_settings(KIKIMR_PATH='dummy/path'):
        assert get_table_path('table') == 'dummy/path/table'
