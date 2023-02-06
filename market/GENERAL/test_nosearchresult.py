#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.testcase import TestCase, main
from core.types import Offer, Shop


class T(TestCase):

    # https://st.yandex-team.ru/MARKETINCIDENTS-3476
    @classmethod
    def prepare_data(cls):
        cls.index.shops += [
            Shop(fesh=3476000),
        ]

        cls.index.offers += [
            Offer(title='offer-3476000', fesh=3476000),
        ]

    def test_text_query(self):
        response = self.report.request_json('place=prime&nosearchresults=1&text=offer')
        self.assertFragmentIn(response, {"results": []}, allow_different_len=False)
        self.error_log.not_expect(code=3714)

    def test_shop_query(self):
        response = self.report.request_json('place=prime&nosearchresults=1&fesh=3476000')
        self.assertFragmentIn(response, {"results": []}, allow_different_len=False)
        self.error_log.not_expect(code=3714)


if __name__ == '__main__':
    main()
