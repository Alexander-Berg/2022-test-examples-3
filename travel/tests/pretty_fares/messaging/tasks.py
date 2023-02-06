# -*- coding: utf-8 -*-

import mock

from travel.avia.ticket_daemon_processing.pretty_fares.messaging import tasks
from travel.avia.ticket_daemon_processing.pretty_fares.messaging.tasks import store_processed_qid, store_variants_to_price_index
from travel.avia.ticket_daemon_processing.pretty_fares.messaging.daemon_tasks import TicketDaemonException


def test_should_stop_execution_if_get_full_results_throws_TicketDaemon_Exception():
    with mock.patch('time.sleep', return_value=None):
        with mock.patch.object(tasks, 'get_full_results', side_effect=TicketDaemonException) as get_full_results_mock:
            with mock.patch.object(store_variants_to_price_index, 'apply_async') as store_variants_to_price_index_mock:

                store_processed_qid.apply(args=('qid', 'timestamp', 'meta'))

                assert get_full_results_mock.called
                assert not store_variants_to_price_index_mock.called


def test_should_stop_execution_if_get_full_results_throws_TicketDaemon_Exception2():
    with mock.patch('time.sleep', return_value=None):
        with mock.patch.object(tasks, 'get_full_results', return_value=('full_results', 'json_result')) as get_full_results_mock:
            with mock.patch.object(store_variants_to_price_index, 'apply_async') as store_variants_to_price_index_mock:

                store_processed_qid.apply(args=('qid', 'timestamp', 'meta'))

                get_full_results_mock.assert_called_with('qid')
                store_variants_to_price_index_mock.assert_called_with(kwargs={'qid': 'qid', 'json_result': 'json_result'})
