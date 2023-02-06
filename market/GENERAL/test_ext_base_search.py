#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.testcase import TestCase, main
from core.types import Offer
from core.matcher import Greater


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.index.offers += [
            Offer(title='iphone'),
        ]

    def test_backends_alive(self):
        self.report.check_alive()
        self.base_search_client.check_alive()

    def test_simple_request(self):
        self.report.request_json(
            'place=prime&text=iphone',
            headers={
                'mYHeadeERR': 'ShineBrightLikeADiamond',
                'X-Market-Req-ID': 'test_req_id',
            },
        )
        self.access_log.expect(
            total_documents_processed=1,
            total_documents_accepted=1,
            x_market_req_id='test_req_id',
            partial_answer=0,
            base_search_elapsed=Greater(0),
        )


if __name__ == '__main__':
    main()
