# coding: utf8
from datetime import datetime, timedelta

import ibm_db
import mock
import pytest

from common.apps.suburban_events import dynamic_params
from common.apps.suburban_events.models import LVGD01_TR2PROC_query, LVGD01_TR2PROC
from travel.rasp.library.python.common23.date import environment
from common.tester.utils.datetime import replace_now
from common.tester.utils.replace_setting import replace_setting, replace_dynamic_setting

from travel.rasp.suburban_tasks.suburban_tasks.scripts import update_suburban_events
from travel.rasp.suburban_tasks.suburban_tasks.scripts.update_suburban_events import is_fetch_possible, lists_diff, fetch_data
from travel.rasp.suburban_tasks.tests.scripts.update_suburban_events.factories import create_rzd_query


def enable_fetch(enable=True):
    dynamic_params.set_param('rzd_fetch_enabled', enable)


@pytest.mark.mongouser
class TestIsFetchPossible(object):
    def test_fetch_flag(self):
        assert is_fetch_possible() is False

        with replace_dynamic_setting('SUBURBAN_RZD_FETCH_ENABLED', True):
            assert is_fetch_possible() is True

        with replace_dynamic_setting('SUBURBAN_RZD_FETCH_ENABLED', False):
            assert is_fetch_possible() is False

    def test_by_prev_queries(self):
        enable_fetch()

        with replace_dynamic_setting('SUBURBAN_RZD_MIN_FETCH_INTERVAL', 120), \
             replace_dynamic_setting('SUBURBAN_RZD_FETCH_ENABLED', True):

            # старые предыдущие запросы
            for i in range(3):
                create_rzd_query(queried_at=environment.now() - timedelta(seconds=130 - i))
            assert is_fetch_possible() is True

            # недавний ошибочный запрос
            create_rzd_query(queried_at=environment.now() - timedelta(seconds=119), exception='aaaaaa')
            assert is_fetch_possible() is True

            # недавний валидный запрос
            create_rzd_query(queried_at=environment.now() - timedelta(seconds=119))
            assert is_fetch_possible() is False


def test_lists_diff():
    def v(value):
        return LVGD01_TR2PROC(**{'ID_TRAIN': value, 'NAMESTO': str(value * 2)})

    res = lists_diff(
        [v(1), v(2), v(4), v(5)],
        [v(2), v(3), v(5)],
    )
    assert res == [v(1), v(4)]

    res = lists_diff(
        [v(2), v(4), v(5)],
        [v(1), v(2), v(4), v(5)],
    )
    assert res == []


class TestFetchData(object):
    @replace_dynamic_setting('SUBURBAN_EVENTS_GET_RZD_DATA_RETRY_DELAY', 0.01)
    def test_valid(self):
        with replace_now(datetime(2017, 10, 10, 12, 42, 6)) as msk_now, \
                replace_setting('RZD_SUBURBAN_EVENTS_DB', 'mydb42'), \
                mock.patch.object(ibm_db, 'commit') as m_commit, \
                mock.patch.object(update_suburban_events, 'get_connect') as m_get_connect, \
                mock.patch.object(update_suburban_events, 'rzd_sql_callproc') as m_rzd_sql_callproc, \
                mock.patch.object(update_suburban_events, 'get_rowdicts_from_resource') as m_get_rowdicts_from_resource:

            # сначала получаем 3 ошибки - проверяем ретрай
            m_rzd_sql_callproc.side_effect = [ValueError, ValueError, ValueError, [42, 43, 44]]
            m_get_connect.return_value = mock.sentinel.connection
            m_get_rowdicts_from_resource.return_value = mock.sentinel.rows

            result, tries_count = fetch_data(msk_now - timedelta(minutes=20), msk_now - timedelta(minutes=10), retries=4)
            assert tries_count == 4
            assert result == mock.sentinel.rows

            assert m_get_connect.call_args_list == [mock.call(db_name='mydb42') for _ in range(4)]
            m_commit.assert_called_once_with(mock.sentinel.connection)

            proc_call = mock.call(
                mock.sentinel.connection,
                '@LVGD01.TR2PROC',
                (2, '2017-10-10-12.22', '2017-10-10-12.32')
            )
            assert m_rzd_sql_callproc.call_args_list == [proc_call for _ in range(4)]
            m_get_rowdicts_from_resource.assert_called_once_with(42)

    @replace_dynamic_setting('SUBURBAN_EVENTS_GET_RZD_DATA_RETRY_DELAY', 0.01)
    def test_retries_fail(self):
        with mock.patch.object(update_suburban_events, 'get_connect') as m_get_connect, mock.patch.object(update_suburban_events, 'rzd_sql_callproc') as m_rzd_sql_callproc:

            m_get_connect.return_value = mock.sentinel.connection
            m_rzd_sql_callproc.side_effect = ValueError('Can not fetch data')

            with pytest.raises(ValueError):
                fetch_data(datetime.now(), datetime.now(), retries=4)

            assert len(m_get_connect.call_args_list) == 4
            assert len(m_rzd_sql_callproc.call_args_list) == 4
