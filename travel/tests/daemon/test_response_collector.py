# -*- coding: utf-8 -*-
from mock import patch, Mock, PropertyMock, MagicMock

from travel.avia.library.python.tester.testcase import TestCase
from travel.avia.library.python.ticket_daemon.memo import reset_all_caches
from travel.avia.ticket_daemon.ticket_daemon.daemon.response_collector import ResponseCollector
from travel.avia.ticket_daemon.ticket_daemon.daemon_tester import create_query


class TestResponseCollector(TestCase):
    def setUp(self):
        reset_all_caches()
        self.query = create_query()
        self.response_collector = ResponseCollector(self.query)
        self.patches = {
            'travel.avia.ticket_daemon.ticket_daemon.daemon.response_collector.query_aggregate_min_prices_task': Mock(),
            'travel.avia.ticket_daemon.ticket_daemon.daemon.response_collector.processed_qid_logger': Mock(),
            'travel.avia.ticket_daemon.ticket_daemon.daemon.response_collector.get_processed_qid_writer': Mock(),
        }
        self.applied_patches = [
            patch(_patch, data) for _patch, data in self.patches.iteritems()
        ]
        [_patch.start() for _patch in self.applied_patches]

    def tearDown(self):
        patch.stopall()

    @patch('travel.avia.ticket_daemon.ticket_daemon.api.result.cache_backends.ydb_cache.remove_many')
    @patch('travel.avia.ticket_daemon.ticket_daemon.api.query.Query.timeline', new=MagicMock(return_value=Mock()), create=True)
    def test_check_all_replied_should_remove_empty_partners(self, remove_many_mock):
        empty_partners = {'partner1', 'partner2'}
        with patch('travel.avia.ticket_daemon.ticket_daemon.daemon.response_collector.ResponseCollector.all_replied',
                   new=PropertyMock(return_value=True)), \
                patch.object(self.response_collector, 'partners_empty', empty_partners):
            self.response_collector.check_all_replied(Mock(code='current_partner'))

        remove_many_mock.assert_called_with(self.response_collector.q, empty_partners)
