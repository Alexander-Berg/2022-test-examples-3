#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.testcase import TestCase, main
from core.types import (
    BlueOffer,
    DeliveryBucket,
    HyperCategory,
    HyperCategoryType,
    MarketSku,
    Model,
    NewShopRating,
    Offer,
    Outlet,
    PickupBucket,
    PickupOption,
    Region,
    Shop,
)

from core.matcher import Contains, NoKey


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.reqwizard.on_default_request().respond()

        cls.index.regiontree += [
            Region(
                rid=1,
                name='Московская область',
                region_type=Region.FEDERATIVE_SUBJECT,
                children=[
                    Region(rid=213, name='Москва'),
                ],
            )
        ]
        cls.index.shops += [
            Shop(
                fesh=1,
                priority_region=213,
                regions=[225],
                name='Московская пепячечная "Доставляем"',
                new_shop_rating=NewShopRating(new_rating_total=3.0),
            ),
            Shop(
                fesh=2,
                priority_region=213,
                regions=[225],
                name='Московская пепячечная "Доставляем"',
                new_shop_rating=NewShopRating(new_rating_total=4.0),
            ),
        ]

        cls.index.outlets += [
            Outlet(point_id=111, fesh=1, region=213),
            Outlet(point_id=222, fesh=2, region=213),
        ]

        cls.index.pickup_buckets += [
            PickupBucket(
                bucket_id=1001,
                fesh=1,
                options=[PickupOption(outlet_id=111, price=100)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            PickupBucket(
                bucket_id=1002,
                fesh=2,
                options=[PickupOption(outlet_id=222, price=100)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
        ]

        cls.index.hypertree += [
            HyperCategory(hid=8, output_type=HyperCategoryType.GURU),
            HyperCategory(hid=9, output_type=HyperCategoryType.SIMPLE),
        ]

        cls.index.offers += [
            Offer(title='iphone black', bid=100, fesh=1, hid=8, randx=900, price=60, store=True, pickup_buckets=[1001]),
            Offer(
                title='iphone gold',
                bid=1,
                fesh=2,
                hid=8,
                randx=600,
                price=1000000,
                store=True,
                pickup_buckets=[1002],
            ),
            Offer(title='iphone bronze', fesh=1, hid=8, randx=300, price=1000, store=True, pickup_buckets=[1001]),
        ]

        cls.index.mskus += [
            MarketSku(
                sku=1,
                hid=8,
                hyperid=1,
                title='samsung silver',
                blue_offers=[
                    BlueOffer(title='samsung silver', price=1000, pickup_buckets=[1001]),
                ],
            ),
            MarketSku(
                sku=2,
                hid=8,
                hyperid=2,
                title='samsung green',
                blue_offers=[BlueOffer(title='samsung green', price=1000, pickup_buckets=[1001])],
            ),
        ]

        cls.index.models += [
            Model(title='Iphone 6s', hid=8),
            Model(title='Iphone 7', hid=8),
            Model(title='Iphone X', hid=8),
        ]

        cls.index.offers += [
            Offer(title='чехол для iphone', fesh=1, hid=9),
            Offer(title='чехол для iphone', fesh=2, hid=9),
            Offer(title='чехол для iphone', fesh=1, hid=9),
        ]

        cls.settings.ignore_qtree_decoding_failed_in_error_log = True

    def test_panther_index_contents(self):
        response = self.report.request_json('place=prime&text=iphone&pp=42&debug=1&rearr-factors=allow_panther=1')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer",
                    }
                ]
            },
        )
        self.assertFragmentIn(response, {'logicTrace': [Contains("Enabling Panther")]})

    def test_panther_on_geo(self):
        response = self.report.request_json('place=geo&text=iphone&rids=213&debug=da&rearr-factors=geo_allow_panther=1')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer",
                    }
                ]
            },
        )
        self.assertFragmentIn(response, {'logicTrace': [Contains("Enabling Panther")]})

    def test_panther_tpsz(self):
        '''Тестируем флаги
        panther_model_tpsz - количество моделей отбираемых в топ пантеры
        panther_offer_tpsz - количество офферов отбираеых в топ пантеры
        panther_metadoc_search_offer_tpsz - количество офферов отбираемых в топ пантеры при поиске метадокументов
        panther_no_categ_model_tpsz - количество офферов при поиске без категории
        panther_no_categ_offer_tpsz - количество офферов при поиске без категории

        Приоритет выбора:
        для моделей: panther_no_categ_model_tpsz (при отсутствии hid) или panther_model_tpsz
        для офферов: panther_no_categ_offer_tpsz (при отсутствии hid) или panther_metadoc_search_offer_tpsz
        (при установленном флаге market_metadoc_search) или panther_offer_tpsz
        '''
        # запрос без флагов - находятся все офферы и модели
        response = self.report.request_json('place=prime&text=iphone&rearr-factors=market_metadoc_search=no')
        self.assertFragmentIn(response, {'search': {'total': 9, 'totalOffers': 6, 'totalModels': 3}})

        # запрос без категории
        # количество офферов ограничивается panther_offer_tpsz
        response = self.report.request_json(
            'place=prime&text=iphone&rearr-factors=panther_offer_tpsz=1;panther_model_tpsz=2;market_metadoc_search=no'
        )
        self.assertFragmentIn(response, {'search': {'total': 3, 'totalOffers': 1, 'totalModels': 2}})

        response = self.report.request_json(
            'place=prime'
            '&text=iphone'
            '&rearr-factors=panther_offer_tpsz=1'
            '&rearr-factors=panther_model_tpsz=2'
            '&rearr-factors=panther_metadoc_search_offer_tpsz=2'
            '&rearr-factors=market_metadoc_search=offers'
        )
        self.assertFragmentIn(response, {'search': {'total': 4, 'totalOffers': 2, 'totalModels': 2}})

        response = self.report.request_json(
            'place=prime'
            '&text=iphone'
            '&rearr-factors=panther_offer_tpsz=1'
            '&rearr-factors=panther_model_tpsz=2'
            '&rearr-factors=panther_metadoc_search_offer_tpsz=2'
            '&rearr-factors=market_metadoc_search=no'
        )
        self.assertFragmentIn(response, {'search': {'total': 3, 'totalOffers': 1, 'totalModels': 2}})

        # запрос без категории с panther_no_categ_offer_tpsz
        # количество офферов ограничивается panther_no_categ_offer_tpsz, моделей -- panther_model_tpsz
        response = self.report.request_json(
            'place=prime&text=iphone&rearr-factors=panther_no_categ_offer_tpsz=4;panther_model_tpsz=3;market_metadoc_search=no'
        )
        self.assertFragmentIn(response, {'search': {'total': 7, 'totalOffers': 4, 'totalModels': 3}})

        # запрос без категории с panther_no_categ_model_tpsz
        # количество офферов ограничивается panther_offer_tpsz, моделей -- panther_no_categ_model_tpsz
        response = self.report.request_json(
            'place=prime&text=iphone&rearr-factors=panther_offer_tpsz=2;panther_no_categ_model_tpsz=2;market_metadoc_search=no'
        )
        self.assertFragmentIn(response, {'search': {'total': 4, 'totalOffers': 2, 'totalModels': 2}})

        # запрос в категорию hid=9
        # берется значение panther_offer_tpsz, не panther_no_categ_offer_tpsz
        response = self.report.request_json(
            'place=prime&text=iphone&hid=9&rearr-factors=panther_offer_tpsz=1;panther_no_categ_offer_tpsz=3;market_metadoc_search=no'
        )
        self.assertFragmentIn(response, {'search': {'total': 1, 'totalOffers': 1, 'totalModels': 0}})

        # запрос в категорию hid=8
        # берётся значение panther_model_tpsz, не panther_no_categ_model_tpsz
        response = self.report.request_json(
            'place=prime&text=iphone&hid=8&rearr-factors=panther_model_tpsz=2;panther_no_categ_model_tpsz=1;market_metadoc_search=no'
        )
        self.assertFragmentIn(response, {'search': {'total': 5, 'totalOffers': 3, 'totalModels': 2}})

    def test_doc_rel_as_a_factor(self):
        """Прокидываем пантерный doc_rel в факторы.

        https://st.yandex-team.ru/MARKETOUT-16422
        """

        response = self.report.request_json('place=prime&text=iphone&debug=da')

        doc_rel = response.root["search"]["results"][0]["debug"]["docRel"]
        self.assertFragmentIn(response, {"search": {"results": [{"debug": {"factors": {"DOC_REL": doc_rel}}}]}})

    def test_panther_quorum_disabling(self):
        '''
        Просто фиксируем, что по дефолту кворум отключен
        '''
        response = self.report.request_json('place=prime&text=iphone&debug=da')
        self.assertTrue('Disabling Panther Quorum' in str(response))

        response = self.report.request_json('place=prime&text=iphone&debug=da&rearr-factors=disable_panther_quorum=0')
        self.assertFalse('Disabling Panther Quorum' in str(response))

    def test_panther_top_and_collections_white(self):
        '''На белом маркете топ пантеры 250 для офферов и моделей, 120 для синих офферов, поиск идет в SHOP, SHOP_BLUE и MODEL'''

        response = self.report.request_json('place=prime&text=blue&debug=da')
        self.assertFragmentIn(
            response,
            {
                "debug": {
                    "report": {
                        "context": {
                            "collections": {
                                "SHOP": {"pron": ["panther_top_size_=250"]},
                                "SHOP_BLUE": NoKey("SHOP"),
                                "MODEL": {"pron": ["panther_top_size_=250"]},
                            }
                        }
                    }
                }
            },
        )

    def test_panther_top_and_collections_blue(self):
        """Если на белом prime задать поиск с rgb=blue то доки искаться будут в SHOP_BLUE с пантерным топом 400"""
        response = self.report.request_json('place=prime&text=blue&debug=da&rgb=blue')
        self.assertFragmentIn(
            response,
            {
                "debug": {
                    "report": {
                        "context": {
                            "collections": {
                                "SHOP_BLUE": NoKey("SHOP"),
                                "MODEL": NoKey("MODEL"),
                                "SHOP": {"pron": ["panther_top_size_=250"]},
                            }
                        }
                    }
                }
            },
        )


if __name__ == '__main__':
    main()
