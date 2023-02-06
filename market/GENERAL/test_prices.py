#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import BlueOffer, HyperCategory, MarketSku, Model, Offer, Promo, PromoType, RtyOffer, Shop, VCluster
from core.testcase import TestCase, main
from core.matcher import NoKey
from core.types.offer_promo import PromoDirectDiscount
from core.logs import ErrorCodes


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.rty_qpipe = True  # Будем проверять обнуление цен через RTY QPipe
        cls.settings.default_search_experiment_flags += ['enable_fast_promo_matcher=0;enable_fast_promo_matcher_test=0']
        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']

        # hid: [1, 100]
        # vclusterid: [101, 200]
        # fesh: [201, 300]
        # model id: [301, 400]

        # Output with price_from, base id = 0
        cls.index.hypertree += [
            HyperCategory(hid=1, visual=True),
            HyperCategory(hid=2),
        ]
        cls.index.vclusters += [
            VCluster(vclusterid=1000000101, hid=1),
            VCluster(vclusterid=1000000102, hid=1),
        ]
        cls.index.models += [
            Model(hyperid=301, hid=2),
            Model(hyperid=302, hid=2),
        ]
        cls.index.offers += [
            Offer(vclusterid=1000000101, pricefrom=True, fesh=201, price=100),
            Offer(vclusterid=1000000101, pricefrom=False, fesh=202, price=100),
            Offer(vclusterid=1000000102, pricefrom=False, fesh=203, price=100),
            Offer(hyperid=301, pricefrom=True, fesh=204, price=100),
            Offer(hyperid=301, pricefrom=False, fesh=205, price=100),
            Offer(hyperid=302, pricefrom=False, fesh=206, price=100),
        ]

        # Промо, обнуляющее цену,
        # которое не сможет обнулить цену так как есть проверка на размер скидки
        PROMO_1 = Promo(
            promo_type=PromoType.DIRECT_DISCOUNT,
            key="promo1",
            feed_id=200,
            direct_discount=PromoDirectDiscount(
                items=[
                    {
                        'feed_id': 200,
                        'offer_id': "2002",  # Тут обязательно должна быть строка!
                        'discount_price': {'value': 0, 'currency': 'RUR'},
                        'old_price': {'value': 100, 'currency': 'RUR'},
                    },
                ],
            ),
        )

        cls.index.offers += [
            # Простой белый офер: грошовая цена сама округлится до 0 (в RUR)
            Offer(hid=3, price=0.00001, waremd5="WhiteOffer___________g"),
            # Белый DSBS офер: цену обнулим через RTY QPipe
            Offer(
                hid=3,
                price=100,
                fesh=10,
                feedid=100,
                offerid=1001,
                sku=1,
                cpa=Offer.CPA_REAL,
                waremd5="DsbsOffer____________g",
            ),
        ]

        cls.index.mskus += [
            MarketSku(sku=1, hid=3),
            MarketSku(
                sku=2,
                hid=3,
                blue_offers=[
                    # Синий офер: цену обнулим через RTY QPipe
                    BlueOffer(hid=3, price=100, fesh=20, feedid=200, offerid=2001, waremd5="BlueOffer____________g"),
                ],
            ),
            MarketSku(
                sku=3,
                hid=3,
                blue_offers=[
                    # Синий офер с промо, обнуляющим цену (сейчас такое возможно, скидка 100% не запрещена)
                    BlueOffer(
                        hid=3,
                        price=100,
                        fesh=20,
                        feedid=200,
                        offerid=2002,
                        promo=PROMO_1,
                        waremd5="BlueOffer_with_promo_g",
                    ),
                ],
            ),
        ]

        cls.index.shops += [
            Shop(fesh=10, datafeed_id=100, priority_region=213, cpa=Shop.CPA_REAL),  # DSBS shop
            Shop(fesh=20, datafeed_id=200, priority_region=213, cpa=Shop.CPA_REAL, blue=Shop.BLUE_REAL),  # Blue shop
        ]

    def test_price_from_prime(self):
        response = self.report.request_json('place=prime&hid=1')
        # VCluster with price_from.
        self.assertFragmentIn(
            response,
            {'entity': 'product', 'id': 1000000101, 'prices': {'min': "100", 'max': NoKey('max')}},
            preserve_order=False,
        )

        # VCluster without price_from.
        self.assertFragmentIn(
            response,
            {
                'entity': 'product',
                'id': 1000000102,
                'prices': {
                    'max': "100",
                },
            },
            preserve_order=False,
        )

        response = self.report.request_json('place=prime&hid=2')
        # Offer with price_from.
        self.assertFragmentIn(
            response, {'entity': 'offer', 'shop': {'id': 204}, 'prices': {'min': "100", 'value': NoKey('value')}}
        )
        # Offer without price_from.
        self.assertFragmentIn(
            response,
            {
                'entity': 'offer',
                'shop': {'id': 205},
                'prices': {
                    'min': NoKey('min'),
                    'value': "100",
                },
            },
        )

        # Model with price_from.
        self.assertFragmentIn(response, {'type': 'model', 'id': 301, 'prices': {'min': "100", 'max': NoKey('max')}})
        # Model without price_from.
        self.assertFragmentIn(response, {'type': 'model', 'id': 302, 'prices': {'min': "100", 'max': "100"}})

    def check_zero_price(self, offers, is_blue=False):
        # Обнуляем цену через RTY Qpipe
        self.rty.offers += [
            RtyOffer(feedid=100, offerid=1001, price=0.001),  # Просто пеередать price=0 через RTY невозможно,
            RtyOffer(
                feedid=200, offerid=2001, price=0.001
            ),  # но грошовая цена передается, а потом округляется до 0 RUR
        ]

        for ignore_has_gone in [None, False, True]:
            response = self.report.request_json(
                "place=prime&rids=213&regset=1&allow-collapsing=0&hid=3&rearr-factors=market_metadoc_search=no{}{}".format(
                    "&ignore-has-gone={}".format(1 if ignore_has_gone else 0) if ignore_has_gone is not None else "",
                    "&rgb=blue" if is_blue else "",
                )
            )

            # Параметр &ignore-has-gone= (несмотря на свое название) отключает не только фильтр по стокам,
            # но и любьые другие скрытия, включая наше скрытие по нулевой цене

            if not ignore_has_gone:
                self.assertFragmentNotIn(response, [{'entity': "offer", 'prices': {'value': "0"}}])
            else:
                self.assertFragmentIn(
                    response,
                    [
                        {
                            'entity': "offer",
                            'prices': {'value': "0"},
                            'wareId': ware_id,
                        }
                        for ware_id in offers
                    ],
                )

        # Предполагаем, что в норме скрытия в проде массово не должны отключаться, поэтому
        # оферы с нулевой ценой, отдаваемые под &ignore-has-gone= будут логгировать ошибку
        # OFFER_HAS_NO_PRICE (4012) — ожидаем по 1 ошибке на каждый отдаваемый офер

        self.base_logs_storage.error_log.expect(code=ErrorCodes.OFFER_HAS_NO_PRICE).times(len(offers))

    def test_zero_price_on_white(self):
        self.check_zero_price(
            [
                "WhiteOffer___________g",
                "DsbsOffer____________g",
                "BlueOffer____________g",
            ]
        )

    def test_zero_price_on_blue(self):
        self.check_zero_price(
            [
                "DsbsOffer____________g",
                "BlueOffer____________g",
            ],
            is_blue=True,
        )


if __name__ == '__main__':
    main()
