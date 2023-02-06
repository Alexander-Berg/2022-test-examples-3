#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import Offer, RtyOffer
from core.testcase import TestCase, main
from core.matcher import NotEmpty


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.rty_qpipe = True
        cls.index.offers += [Offer(title='symbian', feedid=28, offerid='fff', price=300)]

    def _check_offer(self, text, price, vat=None, pricefrom=False):
        response = self.report.request_json('place=prime&text={}&rearr-factors=rty_qpipe=1'.format(text))

        price_value_field = 'value' if not pricefrom else 'min'
        self.assertFragmentIn(response, {'prices': {'currency': 'RUR', price_value_field: str(price)}})

        if vat:
            self.assertFragmentIn(response, {'vat': str(vat)})
        else:
            self.assertFragmentNotIn(response, {'vat': NotEmpty()})

    def test_reuse_docids(self):
        """
        Проверяем, что при переиспользовании docid в memory_search все остается как надо
        """
        self._check_offer('symbian', 300)

        # индексируем в memory
        self.rty.offers += [RtyOffer(feedid=28, offerid='fff', price=400)]
        self._check_offer('symbian', 400)

        # освобождаем docid
        self.rty.flush()
        self._check_offer('symbian', 400)

        # занимаем docid посторонним документом
        self.rty.offers += [RtyOffer(feedid=99, offerid='asdd', price=500)]
        self._check_offer('symbian', 400)


if __name__ == '__main__':
    main()
