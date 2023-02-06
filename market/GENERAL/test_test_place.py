#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    BlueOffer,
    Currency,
    HyperCategory,
    HyperCategoryType,
    MarketSku,
    Model,
    Offer,
    Region,
    Shop,
    Tax,
    Vat,
)
from core.testcase import TestCase, main
from core.matcher import ElementCount, Regex


USER_6_PUID = 67282295
USER_1_PUID = 690303013


class _Offers(object):
    waremd5s = [
        'Sku1Price500-LVm1Goleg',
        'Sku2Price50-iLVm1Goleg',
        'Sku3Price45-iLVm1Goleg',
        'Sku4Price36-iLVm1Goleg',
        'Sku5Price15-iLVm1Goleg',
        'Sku6Price16-iLVm1Goleg',
        'Sku7Price11-iLVm1Goleg',
        'Sku8Price12-iLVm1Goleg',
        'Sku9Price10-iLVm1Goleg',
    ]
    feed_ids = [3] * len(waremd5s)
    prices = [500, 50, 45, 36, 15, 16, 11, 12, 10]
    discounts = [9, 10, 45, 36, 15, None, None, None, None]
    shop_skus = ['Feed_{feedid}_sku{i}'.format(feedid=feedid, i=i + 1) for i, feedid in enumerate(feed_ids)]
    model_ids = list(range(1, len(waremd5s) + 1))
    sku_offers = [
        BlueOffer(price=price, vat=Vat.VAT_10, offerid=shop_sku, feedid=feedid, waremd5=waremd5, discount=discount)
        for feedid, waremd5, price, shop_sku, discount in zip(feed_ids, waremd5s, prices, shop_skus, discounts)
    ]


class T(TestCase):
    @classmethod
    def prepare(cls):
        """
        Данные для некоторых блоков
        """

        # regions
        cls.index.regiontree += [
            Region(rid=213, name='Москва'),
            Region(rid=999999, name='Тьмутаракань'),
        ]

        # shops
        cls.index.shops += [
            # blue virtual shop
            Shop(
                fesh=1,
                datafeed_id=1,
                priority_region=213,
                name='blue_shop_1',
                currency=Currency.RUR,
                fulfillment_virtual=True,
                virtual_shop_color=Shop.VIRTUAL_SHOP_BLUE,
                tax_system=Tax.OSN,
                cpa=Shop.CPA_REAL,
            ),
            # green shop
            Shop(fesh=2, priority_region=213),
            # blue supplier shop
            Shop(
                fesh=3,
                datafeed_id=3,
                priority_region=213,
                name='supplier_shop_3',
                currency=Currency.RUR,
                tax_system=Tax.OSN,
                cpa=Shop.CPA_REAL,
                blue=Shop.BLUE_REAL,
            ),
        ]

        # categories
        cls.index.hypertree += [
            HyperCategory(hid=900 + hyperid, output_type=HyperCategoryType.GURU) for hyperid in _Offers.model_ids
        ]
        # noisy categories
        cls.index.hypertree += [
            HyperCategory(hid=hid, output_type=HyperCategoryType.GURU) for hid in range(11100, 11200)
        ]
        cls.index.models += [Model(hyperid=hyperid, hid=900 + hyperid) for hyperid in _Offers.model_ids]

        # market skus
        cls.index.mskus += [
            MarketSku(
                fesh=1,
                ts=hyperid * 1000,
                title='Blue offer {sku}'.format(sku=shop_sku),
                hyperid=hyperid,
                sku=hyperid * 1000,
                waremd5='Sku{i}-wdDXWsIiLVm1goleg'.format(i=hyperid),
                blue_offers=[sku_offer],
            )
            for hyperid, shop_sku, sku_offer in zip(_Offers.model_ids, _Offers.shop_skus, _Offers.sku_offers)
        ]

        # green offers
        cls.index.offers += [Offer(fesh=2, hyperid=hyperid, ts=hyperid * 1000) for hyperid in _Offers.model_ids]

    def test_responses(self):
        """
        Проверка непустой выдачи для тестовых пользователей
        Для всех плэйсов в выдаче есть модели
        """
        for region in [213, 999999]:
            for rgb_param in ['&rgb=blue', '&rgb=green', '&rgb=green_with_blue', '']:
                model_count_for_user = {USER_1_PUID: 1, USER_6_PUID: 6}
                for puid, model_count in model_count_for_user.items():
                    for place in [
                        'also_viewed',
                        'product_accessories',
                        'popular_products',
                        'products_by_history',
                        'deals',
                        'commonly_purchased',
                    ]:
                        response = self.report.request_json(
                            'place={place}&puid={puid}{rgb_param}&rids={rids}&debug=1'.format(
                                place=place, puid=puid, rgb_param=rgb_param, rids=region
                            )
                        )
                        self.assertFragmentIn(
                            response, {'search': {'total': model_count, 'results': ElementCount(model_count)}}
                        )

    def test_default_offer(self):
        """
        Для всех модельных плэйсов тестовый плэйс находит ДО для моделей
        """
        model_count_for_user = {USER_1_PUID: 1, USER_6_PUID: 6}
        for puid, model_count in model_count_for_user.items():
            for place in [
                'also_viewed',
                'product_accessories',
                'popular_products',
                'products_by_history',
                'deals',
                'commonly_purchased',
                'attractive_models',
                'omm_market&omm_place=omm_findings' 'omm_market&omm_place=omm_electro' 'omm_market&omm_place=item2item',
            ]:
                response = self.report.request_json(
                    'place={place}&puid={puid}&rids=213&debug=1'.format(place=place, puid=puid)
                )
                self.assertFragmentIn(
                    response,
                    {
                        'search': {
                            'total': model_count,
                            'results': [{'entity': 'product', 'type': 'model', 'offers': {'items': ElementCount(1)}}],
                        }
                    },
                    allow_different_len=True,
                )

    def test_category_output(self):
        """
        Категорийная выдача
        В результате должны оказаться категории
        """
        for region in [213, 999999]:
            model_count_for_user = {USER_1_PUID: 1, USER_6_PUID: 6}
            for rgb_param in ['&cpa=real', '&rgb=blue', '&rgb=green', '&rgb=green_with_blue', '']:
                for place in ['personal_categories', 'promoted_categories']:
                    for puid, categories_count in model_count_for_user.items():
                        response = self.report.request_json(
                            'place={place}&puid={puid}&rids={rids}'.format(place=place, puid=puid, rids=region)
                        )
                        self.assertFragmentIn(
                            response,
                            {
                                'search': {
                                    'total': categories_count,
                                    'results': [{'link': {'params': {'hid': Regex('[0-9]+')}}}],
                                }
                            },
                            allow_different_len=True,
                        )


if __name__ == '__main__':
    main()
