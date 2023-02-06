#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import Offer, RtyOffer
from core.testcase import TestCase, main


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.rty_qpipe = True
        cls.index.offers += [
            Offer(title='iphone', fesh=21, feedid=25, offerid='fff', price=300),
            Offer(title='iphone', fesh=21, feedid=25, offerid='ggg', price=300),
        ]

    def _check(self):
        response = self.report.request_json('place=prime&text=iphone&rearr-factors=rty_qpipe=1')
        self.assertFragmentIn(
            response,
            [
                {
                    'entity': 'offer',
                    'shop': {'feed': {'id': '25', 'offerId': 'fff'}},
                    'prices': {'currency': 'RUR', 'value': '400'},
                },
                {
                    'entity': 'offer',
                    'shop': {'feed': {'id': '25', 'offerId': 'ggg'}},
                    'prices': {'currency': 'RUR', 'value': '400'},
                },
            ],
        )

    def test_merge(self):
        """
        Проверяем, что два сегмента в rtyserver смерджились нормально
        """
        self.rty.offers += [RtyOffer(feedid=25, offerid='fff', price=400)]

        self.rty_controller.reopen_indexes()
        self.rty.offers += [RtyOffer(feedid=25, offerid='ggg', price=400)]

        self.rty_controller.reopen_indexes()
        self._check()

        info = self.rty_controller.info_server()
        # before merge must by 4 indexes (2 final, 1 memory and 1 disk)
        self.assertEqual(len(info.get('indexes')), 4)
        self.rty_controller.do_merge()

        info = self.rty_controller.info_server()
        # after merge must by 3 indexes (1 final, 1 memory and 1 disk)
        self.assertEqual(len(info.get('indexes')), 3)
        self._check()


if __name__ == '__main__':
    main()
