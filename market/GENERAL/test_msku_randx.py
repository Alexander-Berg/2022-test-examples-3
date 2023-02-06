#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.testcase import TestCase, main
from core.types.sku import MarketSku


class _MSKUs(object):
    msku0 = MarketSku(randx=0, sku=0)
    msku1 = MarketSku(randx=1, sku=1)


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.index.mskus += [_MSKUs.msku0, _MSKUs.msku1]

    def test_msku_search(self):
        """
        Проверка того, что поиском находятся документы, соответствующие MSKU с randx = 0/1
        """
        for msku in (_MSKUs.msku0, _MSKUs.msku1):
            response = self.report.request_json('place=sku_offers&market-sku=' + msku.sku)
            self.assertFragmentIn(response, {'total': 1, 'results': [{"entity": "sku", "id": msku.sku}]})


if __name__ == '__main__':
    main()
