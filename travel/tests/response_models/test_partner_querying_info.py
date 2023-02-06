# -*- coding: utf-8 -*-
from travel.avia.library.python.tester.testcase import TestCase

from travel.avia.ticket_daemon_api.jsonrpc.response_models.partner_querying_info import PartnerQueryingInfo


class TestPartnerQueryingInfo(TestCase):
    def test_partner_querying_info_to_dict(self):
        querying_info = PartnerQueryingInfo('done', 1.2, 'QID')

        expected_dict = {
            'status': 'done',
            'query_time': 1.2,
            'qid': 'QID',
        }

        assert querying_info.to_dict() == expected_dict
