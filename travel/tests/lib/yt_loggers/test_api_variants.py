# -*- coding: utf-8 -*-
from mock import Mock, patch
from typing import cast

from travel.avia.library.python.tester.factories import create_partner
from travel.avia.library.python.tester.testcase import TestCase

from travel.avia.library.python.ticket_daemon.memo import reset_all_caches
from travel.avia.ticket_daemon_api.jsonrpc.lib.result.continuation import get_cont_details
from travel.avia.ticket_daemon_api.jsonrpc.lib.yt_loggers.api_variants import ApiVariantsLogger
from travel.avia.ticket_daemon_api.jsonrpc.lib.yt_loggers.yt_logger import YtLogger
from travel.avia.ticket_daemon_api.tests.daemon_tester import create_query
from travel.avia.ticket_daemon_api.tests.fixtures.api_variants import get_api_variant


class TestApiVariantsLogger(TestCase):
    def setUp(self):
        reset_all_caches()

    def test_api_variants(self):
        yt_logger = cast(YtLogger, Mock())
        with patch(
            'travel.avia.ticket_daemon_api.jsonrpc.lib.yt_loggers.api_variants.'
            'YtLogger.get_or_create_yt_process_logger',
            return_value=yt_logger,
        ):
            logger = ApiVariantsLogger('name', Mock())

            query = create_query()
            partner = create_partner(code='test_partner')

            _, partner_variants = get_api_variant(partner)
            api_results = {partner.code: partner_variants}
            continuation = get_cont_details(query.id, 0)
            completed_partners = [partner.code]
            statuses = {partner.code: 'done'}
            logger.log(
                query=query,
                api_results=api_results,
                continuation=continuation,
                completed_partners=completed_partners,
                statuses=statuses,
            )

            expected_call = {
                'qid': query.id,
                'variants': [
                    {
                        'partner': partner.code,
                        'route': [[u'1804100050UT489'], []],
                        'tag': '26bbe905789ccf726f5dbcd2574229ba',
                        'tariff': {u'currency': u'RUR', u'value': 1990.0},
                        'created': 1518149935
                    },
                ],
                'cont': 0,
                'statuses': statuses,
                'partners': completed_partners,
            }
            yt_logger.log.assert_called_once_with(expected_call)
