# coding: utf-8
from __future__ import absolute_import, division, print_function, unicode_literals

import pytest
from django.test.utils import override_settings

try:
    import ydb.public.api.python.common  # noqa
except ImportError:
    pytestmark = pytest.mark.skip('kikimr client is not installed')

try:
    from common.db.kikimr.schedule_conf import get_schedule_conf, set_schedule_conf
except ImportError:
    pass


def test_set_schedule_conf(m_kikimr_client):
    with override_settings(KIKIMR_PATH='dummy/path'):
        set_schedule_conf('foo', 'bar')

    m_kikimr_client.return_value.execute_yql.assert_called_once_with('''
        UPSERT INTO [dummy/path/schedule_conf] (name, value) VALUES ('foo', 'bar');
        COMMIT;
    ''')


def test_get_schedule_conf(m_kikimr_client):
    m_kikimr_client.return_value.execute_yql.return_value.result_rows[0].value = 'bar'
    with override_settings(KIKIMR_PATH='dummy/path'):
        result = get_schedule_conf('foo')

    m_kikimr_client.return_value.execute_yql.assert_called_once_with('''
        SELECT value FROM [dummy/path/schedule_conf] WHERE name = 'foo'
    ''')
    assert result == 'bar'


def test_get_schedule_conf_default(m_kikimr_client):
    m_kikimr_client.return_value.execute_yql.return_value.result_rows = []
    with override_settings(KIKIMR_PATH='dummy/path'):
        assert get_schedule_conf('foo') is None
        assert get_schedule_conf('foo', default='default-foo') == 'default-foo'
