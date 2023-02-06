# coding: utf8
from datetime import datetime, timedelta

import mock
import pytest

from common.apps.suburban_events.models import LVGD01_TR2PROC_query, LVGD01_TR2PROC, LVGD01_TR2PROC_feed
from common.dynamic_settings.default import conf
from common.tester.utils.datetime import replace_now
from common.tester.utils.replace_setting import replace_dynamic_setting
from travel.rasp.suburban_tasks.suburban_tasks.scripts import update_suburban_events
from travel.rasp.suburban_tasks.suburban_tasks.scripts.update_suburban_events import get_rzd_data
from travel.rasp.suburban_tasks.tests.scripts.update_suburban_events.factories import create_row


def get_rows(n, **kwargs):
    rows, lvgd_objs = [], []
    for i in range(n):
        row, lvgd_obj = create_row(i, **kwargs)
        rows.append(row)
        lvgd_objs.append(lvgd_obj)

    return rows, lvgd_objs


@pytest.mark.mongouser
@pytest.mark.dbuser
class TestGetRzdData(object):
    @replace_dynamic_setting('SUBURBAN_RZD_FETCH_ENABLED', True)
    @replace_dynamic_setting('SUBURBAN_EVENTS_GET_DATA_MAX_RETRIES', 3)
    def test_valid(self):
        rows_dt = datetime(2017, 10, 10, 10, 42)

        # нет предыдущих запросов к РЖД. Новый запрос делается на максимальное время
        with replace_now(datetime(2017, 10, 10, 12, 42)) as msk_now:
            rows, lvgd_objs = get_rows(3, TIMEOPER_N=rows_dt, TIMEOPER_F=rows_dt)

            expected_dt_to = msk_now - timedelta(seconds=conf.SUBURBAN_RZD_SHIFT_FROM_NOW_TO_FETCH)
            expected_dt_from = expected_dt_to - timedelta(seconds=conf.SUBURBAN_RZD_MAX_FETCH_TO_PAST)
            with mock.patch.object(update_suburban_events, 'fetch_data') as m_fetch_data, \
                 mock.patch.object(update_suburban_events, 'send_rows_to_remote_server') as m_send_remote:

                m_fetch_data.return_value = (rows, 1)

                get_rzd_data()
                m_fetch_data.assert_called_once_with(expected_dt_from, expected_dt_to, retries=3)

                query = LVGD01_TR2PROC_query.objects.get()
                assert query.queried_at == msk_now
                assert query.query_to == expected_dt_to
                assert query.query_from == expected_dt_from
                assert query.rows_count == 3
                assert query.new_rows_count == 3
                assert query.tries_count == 1
                assert query.exception is None

                saved_objs = list(LVGD01_TR2PROC.objects())
                saved_objs = sorted(saved_objs, key=lambda r: r.ID_TRAIN)
                assert lvgd_objs == saved_objs

                saved_objs = list(LVGD01_TR2PROC_feed.objects())
                saved_objs = sorted(saved_objs, key=lambda r: r.ID_TRAIN)
                assert lvgd_objs == saved_objs

                m_send_remote.assert_called_once_with(lvgd_objs)

        # есть предыдущий запрос к РЖД. Новый запрос делается с небольшим пересечением по времени с предыдущим.
        with replace_now(datetime(2017, 10, 10, 12, 58)) as msk_now:

            expected_objs = lvgd_objs
            # создаем 3 полных дубля (объекты, которые  уже получали от РЖД), и один новый row
            rows, lvgd_objs = get_rows(4, TIMEOPER_N=rows_dt, TIMEOPER_F=rows_dt)
            expected_objs.append(lvgd_objs[-1])

            expected_dt_from = expected_dt_to - timedelta(seconds=conf.SUBURBAN_RZD_FETCH_RANGE_OVERLAP)
            expected_dt_to = msk_now - timedelta(seconds=conf.SUBURBAN_RZD_SHIFT_FROM_NOW_TO_FETCH)
            with mock.patch.object(update_suburban_events, 'fetch_data') as m_fetch_data, \
                 mock.patch.object(update_suburban_events, 'send_rows_to_remote_server') as m_send_remote:

                m_fetch_data.return_value = (rows, 2)
                get_rzd_data()
                m_fetch_data.assert_called_once_with(expected_dt_from, expected_dt_to, retries=3)

                queries = list(LVGD01_TR2PROC_query.objects.all())
                assert len(queries) == 2

                query = LVGD01_TR2PROC_query.objects.order_by('-id')[0]
                assert query.queried_at == msk_now
                assert query.query_to == expected_dt_to
                assert query.query_from == expected_dt_from
                assert query.rows_count == 4
                assert query.new_rows_count == 1
                assert query.tries_count == 2
                assert query.exception is None

                saved_objs = list(LVGD01_TR2PROC.objects(query=query))
                saved_objs = sorted(saved_objs, key=lambda r: r.ID_TRAIN)
                assert lvgd_objs == saved_objs

                # проверяем, что в фиде сохранился новый объект, а дубли не появились (остались старые объекты)
                saved_objs = list(LVGD01_TR2PROC_feed.objects())
                saved_objs = sorted(saved_objs, key=lambda r: r.ID_TRAIN)
                assert expected_objs == saved_objs

                m_send_remote.assert_called_once_with(expected_objs[-1:])

    @replace_dynamic_setting('SUBURBAN_RZD_FETCH_ENABLED', True)
    @replace_dynamic_setting('SUBURBAN_EVENTS_GET_DATA_MAX_RETRIES', 3)
    def test_fail_on_fetch(self):
        with mock.patch.object(update_suburban_events, 'fetch_data') as m_fetch_data:
            m_fetch_data.side_effect = ValueError(u'Aaaaaa')

            with replace_now(datetime(2017, 10, 10, 12, 42)) as msk_now:
                get_rzd_data()

            expected_dt_to = msk_now - timedelta(seconds=conf.SUBURBAN_RZD_SHIFT_FROM_NOW_TO_FETCH)
            expected_dt_from = expected_dt_to - timedelta(seconds=conf.SUBURBAN_RZD_MAX_FETCH_TO_PAST)
            m_fetch_data.assert_called_once_with(expected_dt_from, expected_dt_to, retries=3)

            query = LVGD01_TR2PROC_query.objects.first()
            assert query.queried_at == msk_now
            assert query.query_to == expected_dt_to
            assert query.query_from == expected_dt_from
            assert query.rows_count is None
            assert query.new_rows_count is None
            assert query.tries_count is None
            assert query.exception

    @replace_dynamic_setting('SUBURBAN_RZD_FETCH_ENABLED', True)
    @replace_dynamic_setting('SUBURBAN_EVENTS_GET_DATA_MAX_RETRIES', 3)
    def test_fail_after_fetch(self):
        with mock.patch.object(update_suburban_events, 'fetch_data') as m_fetch_data, \
                mock.patch.object(update_suburban_events, 'lists_diff') as m_lists_diff:

            rows, lvgd_objs = get_rows(3)
            m_fetch_data.return_value = (rows, 42)
            m_lists_diff.side_effect = ValueError(u'Aaaaaa')

            with replace_now(datetime(2017, 10, 10, 12, 42)) as msk_now:
                get_rzd_data()

            expected_dt_to = msk_now - timedelta(seconds=conf.SUBURBAN_RZD_SHIFT_FROM_NOW_TO_FETCH)
            expected_dt_from = expected_dt_to - timedelta(seconds=conf.SUBURBAN_RZD_MAX_FETCH_TO_PAST)
            m_fetch_data.assert_called_once_with(expected_dt_from, expected_dt_to, retries=3)

            query = LVGD01_TR2PROC_query.objects.first()
            assert query.queried_at == msk_now
            assert query.query_to == expected_dt_to
            assert query.query_from == expected_dt_from
            assert query.rows_count == 3
            assert query.new_rows_count is None
            assert query.tries_count == 42
            assert query.exception
