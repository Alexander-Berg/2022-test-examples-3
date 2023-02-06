#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.testcase import TestCase, main
from core.types import Offer, Shop
from core.matcher import Absent, NotEmpty


class T(TestCase):
    @classmethod
    def prepare(cls):
        # В этом репорте магазин с cpc=sandbox должен становиться real
        cls.settings.cpc_sandbox_is_real = True

    # MARKETOUT-12495
    @classmethod
    def prepare_shop_cpc_sandbox(cls):
        cls.index.shops += [
            # CPC only
            Shop(fesh=1249501, cpc=Shop.CPC_SANDBOX),
            # CPA+CPC
            Shop(fesh=1249502, cpa=Shop.CPA_REAL, cpc=Shop.CPC_SANDBOX),
            # CPA only
            Shop(fesh=1249503, cpa=Shop.CPA_REAL, cpc=Shop.CPC_NO),
        ]

        cls.index.offers += [
            # CPC only
            Offer(fesh=1249501, hyperid=1249501),
            # CPA+CPC
            Offer(fesh=1249502, hyperid=1249502, cpa=Offer.CPA_REAL),
            # CPA only
            Offer(fesh=1249503, hyperid=1249503, cpa=Offer.CPA_REAL),
            # CPC offer for CPA-only shop
            Offer(fesh=1249503, hyperid=1249504),
        ]

    def test_shop_cpc_sandbox_must_be_real(self):
        """
        Магазин CPC sandbox, превращается в REAL
        Оффер CPC

        Делаем запрос оффера, оффер не должен быть скрыт, шифрованный урл есть
        """
        response = self.report.request_json('place=productoffers&hyperid=1249501&show-urls=encrypted')
        self.assertFragmentIn(
            response,
            {
                'entity': 'offer',
                'model': {
                    'id': 1249501,
                },
                'urls': {
                    'encrypted': NotEmpty(),
                },
            },
        )

    def test_shop_cpa_and_cpc_sandbox_must_be_real(self):
        """
        Магазин CPA + CPC sandbox, превращается в CPA+CPC
        Оффер CPA+CPC

        Делаем запрос оффера, оффер не должен быть скрыт, шифрованный урл есть
        """
        response = self.report.request_json('place=productoffers&hyperid=1249502&show-urls=encrypted')
        self.assertFragmentIn(
            response,
            {
                'entity': 'offer',
                'model': {
                    'id': 1249502,
                },
                'urls': {
                    'encrypted': NotEmpty(),
                },
            },
        )

    def test_shop_cpa_only_cpc_absent(self):
        """
        Магазин CPA only
        Оффер CPA+CPC

        Делаем запрос оффера, CPC-часть оффера должна отсутстовать: шифрованного урла не должно быть
        """
        response = self.report.request_json('place=productoffers&hyperid=1249503&show-urls=encrypted')
        self.assertFragmentIn(
            response,
            {
                'entity': 'offer',
                'model': {
                    'id': 1249503,
                },
                'urls': {
                    'encrypted': Absent(),
                },
            },
        )

    def test_shop_cpa_only_and_offer_is_cpc(self):
        """
        Магазин CPA only
        Оффер CPC

        Делаем запрос оффера, так как оффер CPCшный, то он весь должен отсутствовать
        """
        response = self.report.request_json('place=productoffers&hyperid=1249504&show-urls=encrypted')
        self.assertFragmentNotIn(
            response,
            {
                'entity': 'offer',
                'model': {
                    'id': 1249504,
                },
            },
        )


if __name__ == '__main__':
    main()
