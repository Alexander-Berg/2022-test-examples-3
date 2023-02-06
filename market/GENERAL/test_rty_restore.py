#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import Offer, RtyOffer
from core.testcase import TestCase, main
from market.pylibrary.lite.process import run_external_tool


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.rty_qpipe = True
        cls.index.offers += [
            Offer(title='iphone', fesh=21, feedid=25, offerid='fff %s' % num, price=300) for num in range(10)
        ]

    def _check_price(self, price):
        response = self.report.request_json('place=prime&text=iphone&rearr-factors=rty_qpipe=1')
        self.assertFragmentIn(
            response,
            [
                {
                    'entity': 'offer',
                    'shop': {'feed': {'id': '25', 'offerId': 'fff %s' % num}},
                    'prices': {'currency': 'RUR', 'value': str(price)},
                }
                for num in range(10)
            ],
        )

    def test_restore(self):
        """
        Проверяем, что файлы rty_market восстанавливаются корректно
        """
        self._check_price(300)

        self.rty.offers += [RtyOffer(feedid=25, offerid='fff %s' % num, price=400) for num in range(10)]

        # проверяем цену в in-memory индексе
        self._check_price(400)

        self.rty_controller.stop()
        run_external_tool(['rm', '-rf', '%s/*/report*' % self.meta_paths.rty_index], '/dev/null')
        self.rty_controller.restart()

        # проверяем цену в final индексе
        self._check_price(400)


if __name__ == '__main__':
    main()
