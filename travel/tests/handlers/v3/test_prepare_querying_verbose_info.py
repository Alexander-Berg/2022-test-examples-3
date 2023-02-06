# -*- coding: utf-8 -*-
from travel.avia.ticket_daemon_api.jsonrpc.handlers.v3.views import _prepare_querying_verbose_info
from travel.avia.ticket_daemon_api.jsonrpc.response_models.partner_querying_info import PartnerQueryingInfo
from travel.avia.library.python.tester.testcase import TestCase

from travel.avia.ticket_daemon_api.jsonrpc.lib.result.collector.variants_fabric import ApiVariants


class TestPrepareVerboseQueryingInfo(TestCase):
    def test_merging_querying_info(self):
        partners_results = {
            'partner1': ApiVariants(qid='qid1', flights={}, fares={}, status='done', query_time=1, revision=1),
            'partner2': ApiVariants(qid='qid2', flights={}, fares={}, status='failed', query_time=2, revision=2),
        }

        expected = {
            'partner1': PartnerQueryingInfo('done', 1, 'qid1'),
            'partner2': PartnerQueryingInfo('failed', 2, 'qid2'),
        }

        assert _prepare_querying_verbose_info(partners_results) == expected
