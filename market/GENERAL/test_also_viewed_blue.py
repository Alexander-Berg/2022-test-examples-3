#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import Currency, GLParam, GLType, Model, Shop, YamarecPlace, YamarecSettingPartition
from core.testcase import TestCase, main
from core.matcher import ElementCount, Absent
from core.types.picture import to_mbo_picture
from core.types import DynamicMarketSku
from core.types.sku import BlueOffer, MarketSku
from core.types.taxes import Vat, Tax

import random


class _Offers(object):
    waremd5s = [
        'Sku1Price5-IiLVm1Goleg',
        'Sku2Price50-iLVm1Goleg',
        'Sku3Price45-iLVm1Goleg',
        'Sku4Price36-iLVm1Goleg',
        'Sku5Price15-iLVm1Goleg',
    ]
    feed_ids = [1] * len(waremd5s)
    prices = [5, 50, 45, 36, 15]
    shop_skus = ['Feed_{feedid}_sku{i}'.format(feedid=feedid, i=i + 1) for i, feedid in enumerate(feed_ids)]
    sku_offers = [
        BlueOffer(price=price, vat=Vat.VAT_10, offerid=shop_sku, feedid=feedid, waremd5=waremd5)
        for feedid, waremd5, price, shop_sku in zip(feed_ids, waremd5s, prices, shop_skus)
    ]
    model_ids = list(range(1, len(waremd5s) + 1))

    shop_sku_8 = 'Feed_1_sku8'
    sku_offer_8 = BlueOffer(price=20, vat=Vat.VAT_10, offerid=shop_sku_8, feedid=1, waremd5='Sku8Price20-iLVm1Goleg')


class T(TestCase):
    """
    Набор тестов для "Видевшие  также смотрели"
    place=also_viewed
    """

    @classmethod
    def prepare(cls):
        """
        Модели, офферы и конфигурация для выдачи place=also_viewed
        """

        cls.settings.rgb_blue_is_cpa = True

        # shops
        cls.index.shops += [
            Shop(
                fesh=431782,
                datafeed_id=1,
                priority_region=213,
                name='blue_shop_1',
                currency=Currency.RUR,
                fulfillment_virtual=True,
                virtual_shop_color=Shop.VIRTUAL_SHOP_BLUE,
                tax_system=Tax.OSN,
                cpa=Shop.CPA_REAL,
                blue=Shop.BLUE_REAL,
            ),
        ]

        # models with randomly selected ts
        random.seed(0)
        random_ts = list(range(1, len(_Offers.model_ids) + 1))
        random.shuffle(random_ts)
        cls.index.models += [Model(hyperid=hyperid, ts=ts) for hyperid, ts in zip(_Offers.model_ids, random_ts)]

        # market skus
        cls.index.mskus += [
            MarketSku(
                title='Blue offer {sku}'.format(sku=shop_sku),
                hyperid=hyperid,
                sku=hyperid * 100,
                waremd5='Sku{i}-wdDXWsIiLVm1goleg'.format(i=hyperid),
                blue_offers=[sku_offer],
            )
            for hyperid, shop_sku, sku_offer in zip(_Offers.model_ids, _Offers.shop_skus, _Offers.sku_offers)
        ]

        # yamarec configuration for place=also_viewed
        cls.index.yamarec_places += [
            YamarecPlace(
                name=YamarecPlace.Name.ALSO_VIEWED_PRODUCTS,
                kind=YamarecPlace.Type.SETTING,
                split_rule=YamarecPlace.SplitRule.ABT,
                partitions=[
                    # no partition with split 'noconfig'
                    # empty matching partition
                    YamarecSettingPartition(splits=[{'split': 'empty'}]),
                    # partition with data
                    YamarecSettingPartition(params={'version': '1'}, splits=[{'split': 'normal'}]),
                    YamarecSettingPartition(params={'version': 'MODEL/MSKUv1'}, splits=[{'split': 'model_with_msku'}]),
                ],
            ),
        ]
        # also_viewed is based on product accessories ichwill method
        cls.recommender.on_request_accessory_models(model_id=1, item_count=1000, version='1').respond(
            {'models': ['4', '2', '3', '5']}
        )

    def test_noconfig(self):
        """
        Проверка корректности работы конфига при отсутствии сплита или данных
        """
        noconfig_request = 'place=also_viewed&rgb=blue&rearr-factors=split=noconfig&hyperid=1'
        response = self.report.request_json(noconfig_request)
        self.assertFragmentIn(response, {'total': 0})
        self.error_log.ignore("Ichwill: can not get accessories for product_id=1")

        self.assertEqualJsonResponses(
            request1=noconfig_request, request2='place=also_viewed&rearr-factors=split=empty&hyperid=1'
        )

    def test_total_renderable(self):
        """
        Проверяется, что общее количество для показа = total
        """

        for place_id in ['place=also_viewed&rgb=blue', 'place=also_viewed&fesh=431782']:
            request = '{}&cpa=real&rearr-factors=split=normal&hyperid=1'.format(place_id)
            response = self.report.request_json(request)
            self.assertFragmentIn(response, {'total': 4})

            response = self.report.request_json(request + '&numdoc=2')
            self.assertFragmentIn(response, {'total': 4})
        self.access_log.expect(total_renderable='4').times(4)

        """
        Проверка поля url_hash в show log
        """
        self.show_log_tskv.expect(url_hash=ElementCount(32))

    def test_order(self):
        """
        Порядок выдачи должен соответствовать порядку рекомендаций от ichwill
        В индексе модели размещены в другом порядке (см. поле ts)
        """
        response = self.report.request_json('place=also_viewed&rgb=blue&cpa=real&rearr-factors=split=normal&hyperid=1')
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'total': 4,
                    'results': [
                        {'entity': 'product', 'id': 4},
                        {'entity': 'product', 'id': 2},
                        {'entity': 'product', 'id': 3},
                        {'entity': 'product', 'id': 5},
                    ],
                }
            },
            preserve_order=True,
        )

    @classmethod
    def prepare_short_format(cls):
        cls.index.gltypes += [GLType(param_id=201, hid=103, gltype=GLType.NUMERIC)]

        cls.index.models += [
            Model(hyperid=7, hid=103),
            Model(
                hyperid=8,
                hid=103,
                glparams=[GLParam(param_id=201, value=2)],
                proto_add_pictures=[
                    to_mbo_picture(Model.DEFAULT_ADD_PIC_URL + '#100#200'),
                    to_mbo_picture(Model.DEFAULT_ADD_PIC_URL + '#200#200'),
                ],
            ),
        ]

        cls.index.mskus += [
            MarketSku(
                title='BlueOffer8',
                hyperid=8,
                sku=8,
                waremd5='Sku8-wdDXWsIiLVm1goleg',
                blue_offers=[_Offers.sku_offer_8],
            )
        ]

        cls.recommender.on_request_accessory_models(model_id=7, item_count=1000, version='1').respond({'models': ['8']})

    def test_short_format(self):
        """Проверяем, что с параметром also-viewed-short-format=1, не отдаются filters,
        а картинки и дефолтные оффера отдаются полностью независимо от этого параметра.
        Параметр &also-viewed-short-format действует как на белом, и дополнительно режет картинки и оффера
        """

        response = self.report.request_json(
            'place=also_viewed&rgb=blue&cpa=real&hyperid=7&hid=103&rearr-factors=split=normal'
        )
        self.assertFragmentIn(response, {'filters': []})
        self.assertFragmentIn(
            response,
            {'entity': 'product', 'id': 8, 'pictures': ElementCount(2), 'offers': {'count': 1, 'items': []}},
        )

        response = self.report.request_json(
            'place=also_viewed&rgb=blue&cpa=real&hyperid=7&hid=103&rearr-factors=split=normal&also-viewed-short-format=1'
        )
        self.assertFragmentNotIn(response, {'filters': []})
        self.assertFragmentIn(
            response,
            {'entity': 'product', 'id': 8, 'pictures': ElementCount(1), 'offers': {'count': 1, 'items': Absent()}},
        )

    @classmethod
    def prepare_recom_response_with_msku(cls):
        num_sku = 5
        for i in range(1, num_sku + 1):
            cls.index.mskus += [
                MarketSku(hyperid=hyperid, sku=(hyperid * 100) + i, blue_offers=[BlueOffer()])
                for hyperid in _Offers.model_ids[:-1]
            ]

        recom_response = {'models': ['4/401', '2/202', '3/303', '5']}
        cls.recommender.on_request_accessory_models_with_msku(
            msku_id=100, item_count=1000, version='MODEL/MSKUv1'
        ).respond(recom_response)
        cls.recommender.on_request_accessory_models_with_msku(
            model_id=1, item_count=1000, version='MODEL/MSKUv1'
        ).respond(recom_response)
        cls.recommender.on_request_accessory_models_with_msku(
            model_id=1, msku_id=100, item_count=1000, version='MODEL/MSKUv1'
        ).respond(recom_response)

    def test_recom_response_with_msku(self):
        """
        Проверяем, что ску от рекоммендера учитывается в ответе.
        """
        expected_response = {
            'search': {
                'total': 4,
                'results': [
                    {'entity': 'product', 'id': 4, 'offers': {'items': [{'marketSku': "401"}]}},
                    {'entity': 'product', 'id': 2, 'offers': {'items': [{'marketSku': "202"}]}},
                    {'entity': 'product', 'id': 3, 'offers': {'items': [{'marketSku': "303"}]}},
                    {'entity': 'product', 'id': 5, 'offers': {'items': [{'marketSku': "500"}]}},
                ],
            }
        }

        for search_mode in ['model', 'msku', 'all']:
            response = self.report.request_json(
                'place=also_viewed&rgb=blue&cpa=real'
                '&rearr-factors=split=model_with_msku;recom_search_mode={}&hyperid=1&market-sku=100'.format(search_mode)
            )
            self.assertFragmentIn(
                response,
                expected_response,
                preserve_order=True,
            )

    @classmethod
    def prepare_show_model_do_if_not_found_do_for_msku_from_recommender(cls):
        cls.index.mskus += [
            MarketSku(hyperid=10001, sku=100011, blue_offers=[BlueOffer(price=10)]),  # ДО модели; МСКУ из рекоммендера
            MarketSku(hyperid=10001, sku=100012, blue_offers=[BlueOffer(price=100)]),
            MarketSku(hyperid=10002, sku=100021, blue_offers=[BlueOffer(price=10)]),  # ДО модели
            MarketSku(hyperid=10002, sku=100022, blue_offers=[BlueOffer(price=100)]),  # МСКУ из рекоммендера
            MarketSku(hyperid=10003, sku=100031, blue_offers=[BlueOffer(price=10)]),
            MarketSku(
                hyperid=10003, sku=100032, blue_offers=[BlueOffer(price=100)]
            ),  # оффер скрыт; МСКУ из рекоммендера
            MarketSku(hyperid=10004, sku=100041, blue_offers=[BlueOffer()]),  # оффер скрыт
        ]

        cls.dynamic.market_dynamic.disabled_market_sku += [
            DynamicMarketSku(market_sku=str(100032)),
            DynamicMarketSku(market_sku=str(100041)),
        ]

        cls.recommender.on_request_accessory_models_with_msku(
            msku_id=10000, item_count=1000, version='MODEL/MSKUv1'
        ).respond({'models': ['10001/100011', '10002/100022', '10003/100032', '10004/100041']})

    def test_show_model_do_if_not_found_do_for_msku_from_recommender(self):
        """
        Проверяем работу флага recom_fallback_on_model_do.
        Позволяет выбрать ДО модели, в случае отсутствия ДО для МСКУ из ответа рекомменера.
        """

        for enable_exp in [0, 1]:
            request = (
                'place=also_viewed&rgb=blue&cpa=real'
                + '&rearr-factors=split=model_with_msku;recom_search_mode=msku'
                + ';recom_fallback_on_model_do={enable_exp}'
                + '&market-sku=10000'
            )

            expected_results = [
                {'entity': 'product', 'id': 10001, 'offers': {'items': [{'marketSku': "100011"}]}},
                {'entity': 'product', 'id': 10002, 'offers': {'items': [{'marketSku': "100022"}]}},
            ]

            if enable_exp:
                expected_results.append(
                    {'entity': 'product', 'id': 10003, 'offers': {'items': [{'marketSku': "100031"}]}},
                )

            response = self.report.request_json(request.format(enable_exp=enable_exp))
            self.assertFragmentIn(
                response,
                {
                    'search': {
                        'results': expected_results,
                    }
                },
                preserve_order=True,
                allow_different_len=False,
            )


if __name__ == '__main__':
    main()
