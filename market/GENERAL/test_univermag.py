#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.testcase import TestCase, main
from core.matcher import Absent
from core.types import Offer


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.index.offers += [
            Offer(
                title='джинсы универмаг 1',
                waremd5='H7d__is_univermag____g',
                is_univermag=True,
            ),
            Offer(
                title='джинсы универмаг 2',
                waremd5='H0K__is_univermag____g',
                is_univermag=True,
            ),
            Offer(
                title='джинсы не универмаг 1',
                waremd5='H7d_not_is_univermag_g',
                is_univermag=False,
            ),
            Offer(
                title='юбка не универмаг 1',
                hyperid=200,
                waremd5='ZRK9Q9nKpuAsmQsKgmUtyg',
            ),
            Offer(
                title='юбка универмаг 1',
                hyperid=200,
                waremd5='ZRK7Q7nKpuAsmQsKgmUtyg',
                is_univermag=True,
            ),
        ]

    def test_flag_is_univermag(self):
        # Проверяем, что параметр is_univermag пробрасывается на выдачу если значение True
        for offerid, is_univermag in [
            ('H7d__is_univermag____g', True),
            ('H7d_not_is_univermag_g', False),
            ('ZRK9Q9nKpuAsmQsKgmUtyg', False),
        ]:
            response = self.report.request_json(
                'place=offerinfo&offerid={}&rids=0&regset=2&show-urls=external'.format(offerid)
            )

            self.assertFragmentIn(
                response,
                {
                    "entity": "offer",
                    "wareId": offerid,
                    "isUnivermag": True if is_univermag else Absent(),
                },
            )

    def test_univermag_literal_prime(self):
        """
        Проверяем ограничение выдачи только оферами универмага если filter-univermag=1
        """
        request = 'place=prime&text=джинсы'
        for param, results in [
            ('', ['джинсы универмаг 1', 'джинсы универмаг 2', 'джинсы не универмаг 1']),  # Все оферы разрешены
            ('&filter-univermag=1', ['джинсы универмаг 1', 'джинсы универмаг 2']),  # Только универмаг
        ]:
            response = self.report.request_json(request + param)

            self.assertFragmentIn(
                response,
                {'results': [{'entity': 'offer', 'titles': {"raw": result}} for result in results]},
                allow_different_len=False,
            )

    def test_univermag_literal_productoffers(self):
        """
        Проверяем ограничение выдачи только оферами универмага если filter-univermag=1
        """
        request = 'place=productoffers&hyperid=200'
        for param, results in [
            ('', ['юбка универмаг 1', 'юбка не универмаг 1']),  # Все оферы разрешены
            ('&filter-univermag=1', ['юбка универмаг 1']),  # Только универмаг
        ]:
            response = self.report.request_json(request + param)

            self.assertFragmentIn(
                response,
                {'results': [{'entity': 'offer', 'titles': {"raw": result}} for result in results]},
                allow_different_len=False,
            )


if __name__ == '__main__':
    main()
