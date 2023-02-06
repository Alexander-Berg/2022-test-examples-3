#!/usr/bin/env python
# -*- coding: utf-8 -*-

import runner  # noqa


from core.types import Model, Region
from core.testcase import (
    TestCase,
    main,
)
from core.types.sku import (
    MarketSku,
    BlueOffer,
)
from core.types.taxes import (
    Vat,
)
from core.matcher import (
    LikeUrl,
    NotEmpty,
    Contains,
)


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.index.regiontree += [
            Region(
                rid=1,
                name='Московская область',
                region_type=Region.FEDERATIVE_SUBJECT,
                children=[
                    Region(rid=213, name='Москва'),
                    Region(rid=2, name='Санкт-Петербург'),
                ],
            )
        ]

        cls.index.models += [
            Model(hyperid=2, hid=1, title='Samsung Galaxy S10'),
        ]

        cls.index.mskus += [
            MarketSku(
                title='Samsung Galaxy S10 Black',
                hyperid=2,
                sku=3,
                blue_offers=[
                    BlueOffer(
                        price=11, vat=Vat.VAT_10, offerid='Shop1_sku3', feedid=3, waremd5='gwgfZ3JflzQelx9tFVgDqQ'
                    ),
                ],
            ),
        ]

    def test_bundle_url(self):
        """На синем должен возвращаться корректный turbo bundle url"""
        response = self.report.request_json(
            'place=prime&text=samsung&show-urls=turboBundle&allow-collapsing=0&rgb=blue&rids=0'
        )

        offer_url = (
            '//beru.ru/bundle/3?data=offer%2CgwgfZ3JflzQelx9tFVgDqQ%2C1&fromTurbo=1&lr=0&schema=type%2CobjId%2Ccount'
        )
        self.assertFragmentIn(
            response,
            {
                'entity': 'offer',
                'model': {'id': 2},
                'urls': {
                    'directTurboBundle': LikeUrl.of(offer_url),
                    'encryptedTurboBundle': NotEmpty(),
                },
            },
        )

    def test_pokupki_bundle_url(self):
        """На синем должен возвращаться pokupki.market.yandex.ru turbo bundle url"""
        response = self.report.request_json(
            'place=prime&text=samsung&show-urls=turboBundle&allow-collapsing=0&rgb=blue&rids=0&rearr-factors=pokupki_instead_beru_enabled=1'
        )

        offer_url = '//pokupki.market.yandex.ru/bundle/3?data=offer%2CgwgfZ3JflzQelx9tFVgDqQ%2C1&fromTurbo=1&lr=0&schema=type%2CobjId%2Ccount'
        self.assertFragmentIn(
            response,
            {
                'entity': 'offer',
                'model': {'id': 2},
                'urls': {
                    'directTurboBundle': LikeUrl.of(offer_url),
                    'encryptedTurboBundle': NotEmpty(),
                },
            },
        )

    def test_mobile_bundle_url(self):
        """На синем таче должены быть бандл урлы с правильным доменом"""
        response = self.report.request_json(
            'place=prime&text=samsung&show-urls=turboBundle&allow-collapsing=0&rgb=blue&rids=0&touch=1'
        )

        offer_url = (
            '//m.beru.ru/bundle/3?data=offer%2CgwgfZ3JflzQelx9tFVgDqQ%2C1&fromTurbo=1&lr=0&schema=type%2CobjId%2Ccount'
        )
        self.assertFragmentIn(
            response,
            {
                'entity': 'offer',
                'model': {'id': 2},
                'urls': {
                    'directTurboBundle': LikeUrl.of(offer_url),
                    'encryptedTurboBundle': NotEmpty(),
                },
            },
        )

    def test_pokupki_mobile_bundle_url(self):
        """На синем таче должены быть бандл урлы с доменом m.pokupki.market.yandex.ru"""
        response = self.report.request_json(
            'place=prime&text=samsung&show-urls=turboBundle&allow-collapsing=0&rgb=blue&rids=0&touch=1&rearr-factors=pokupki_instead_beru_enabled=1'
        )

        offer_url = '//m.pokupki.market.yandex.ru/bundle/3?data=offer%2CgwgfZ3JflzQelx9tFVgDqQ%2C1&fromTurbo=1&lr=0&schema=type%2CobjId%2Ccount'
        self.assertFragmentIn(
            response,
            {
                'entity': 'offer',
                'model': {'id': 2},
                'urls': {
                    'directTurboBundle': LikeUrl.of(offer_url),
                    'encryptedTurboBundle': NotEmpty(),
                },
            },
        )

    def test_bundle_url_for_white(self):
        """На белом bundle url возвращаться не должен"""
        for url in [
            'place=prime&text=samsung&show-urls=turboBundle&allow-collapsing=0&rids=0&debug=1',
            'place=prime&text=samsung&show-urls=turboBundle&allow-collapsing=0&rids=0&debug=1&rearr-factors=pokupki_instead_beru_enabled=1',
        ]:
            response = self.report.request_json(url)

            self.assertFragmentIn(
                response,
                {
                    'debug': {
                        'report': {
                            'logicTrace': [
                                Contains('Bundle url may be requested for blue market only'),
                            ],
                        },
                    },
                },
            )
            self.assertFragmentNotIn(
                response,
                {
                    'urls': {
                        'directTurboBundle': NotEmpty(),
                        'encryptedTurboBundle': NotEmpty(),
                    }
                },
            )

    def test_purchase_referrer_in_turbo_bundle(self):
        """
        Если есть флаг rearr-factors=market_purchase_referrer=beru_in_portal, то в bundle url должен быть добавлен
        параметр purchase-referrer=beru_in_portal
        """
        response = self.report.request_json(
            'place=prime&text=samsung&show-urls=turboBundle&allow-collapsing=0&rgb=blue&rids=0&rearr-factors=market_purchase_referrer=beru_in_portal'
        )

        offer_url = '//beru.ru/bundle/3?data=offer%2CgwgfZ3JflzQelx9tFVgDqQ%2C1&fromTurbo=1&lr=0&schema=type%2CobjId%2Ccount&purchase-referrer=beru_in_portal'
        self.assertFragmentIn(
            response,
            {
                'entity': 'offer',
                'model': {'id': 2},
                'urls': {
                    'directTurboBundle': LikeUrl.of(offer_url),
                    'encryptedTurboBundle': NotEmpty(),
                },
            },
        )


if __name__ == '__main__':
    main()
