# coding: utf8

from datetime import datetime

import mock
import pytest

from common.apps.suburban_events import dynamic_params
from common.db import maintenance
from common.models.timestamp import Timestamp
from common.tester.utils.datetime import replace_now
from travel.rasp.tasks.suburban_events.update_expected_suburban_events_after_prepare_all import run


@pytest.mark.dbuser
@pytest.mark.mongouser
def test_run():
    Timestamp.objects.create(code='prepare_all', value=datetime(2017, 5, 4, 15))
    with mock.patch('travel.rasp.tasks.suburban_events.update_expected_suburban_events_after_prepare_all.'
                    'precalc_all_expected_thread_events') as m_precalc:

        data = {'last_successful_switch': datetime(2017, 4, 5, 12).strftime('%Y-%m-%d %H:%M:%S')}
        with mock.patch.object(maintenance, 'read_conf') as read_conf:
            read_conf.return_value = data
            dynamic_params.set_param('last_afupdatefile', datetime(2000, 1, 1))

            with replace_now(datetime(2017, 5, 4, 16)):
                run()
                m_precalc.assert_called_once_with(last_run_time=None)
                assert dynamic_params.get_param('last_precalc_expected_thread_events_time') == datetime(2017, 5, 4, 16)

                run()
                assert m_precalc.call_count == 1

                dynamic_params.set_param('last_precalc_expected_thread_events_time', datetime(2017, 5, 4, 14))
                run()
                assert m_precalc.call_count == 2
                assert m_precalc.call_args_list[1] == mock.call(last_run_time=datetime(2017, 5, 4, 14))

                run()
                assert m_precalc.call_count == 2

            with replace_now(datetime(2017, 5, 6, 19)):
                new_data = {'last_successful_switch': datetime(2017, 5, 6, 13).strftime('%Y-%m-%d %H:%M:%S')}
                read_conf.return_value = new_data
                run()
                assert m_precalc.call_count == 3
                assert m_precalc.call_args_list[2] == mock.call(last_run_time=datetime(2017, 5, 4, 16))
                assert dynamic_params.get_param('last_precalc_expected_thread_events_time') == datetime(2017, 5, 6, 19)

            with replace_now(datetime(2019, 1, 2, 10)):
                dynamic_params.set_param('last_afupdatefile', datetime(2019, 1, 1, 13))
                run()
                assert dynamic_params.get_param('last_precalc_expected_thread_events_time') == datetime(2019, 1, 2, 10)
