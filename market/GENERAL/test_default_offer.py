#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    ClickType,
    DeliveryBucket,
    DeliveryOption,
    DynamicShop,
    GLParam,
    GLType,
    HyperCategory,
    HyperCategoryType,
    MinBidsCategory,
    MinBidsPriceGroup,
    MnPlace,
    Model,
    ModelGroup,
    NewShopRating,
    Offer,
    Outlet,
    PickupBucket,
    PickupOption,
    Promo,
    PromoType,
    Region,
    RegionalDelivery,
    RegionalModel,
    Shop,
    UrlType,
)
from core.testcase import TestCase, main
from core.matcher import NotEmpty, NoKey, Contains

import json


class T(TestCase):
    """
    Tests force default offer ranking
    https://st.yandex-team.ru/MARKETOUT-7837
    https://st.yandex-team.ru/MARKETOUT-7842
    """

    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']

        cls.index.creation_time = 1583830801  # Tue Mar 10 12:00:00 STD 2020 , full on individual min bids
        cls.matrixnet.on_default_place(MnPlace.BASE_SEARCH).respond(0.001)

        # MARKETOUT-8932
        cls.index.gltypes += [
            GLType(
                param_id=201, hid=13, gltype=GLType.ENUM, values=[1, 2, 3, 4], unit_name='Size', cluster_filter=True
            ),
            GLType(param_id=202, hid=14, gltype=GLType.BOOL),
            GLType(param_id=203, hid=14, gltype=GLType.ENUM, values=[10, 15, 20]),
            GLType(param_id=204, hid=14, gltype=GLType.NUMERIC),
        ]

        cls.index.models += [
            Model(hyperid=1001, hid=14),
            Model(
                hyperid=1004,
                hid=14,
                glparams=[
                    GLParam(param_id=202, value=0),
                    GLParam(param_id=203, value=10),
                    GLParam(param_id=204, value=100),
                ],
            ),
        ]

        cls.index.shops += [
            Shop(fesh=1104, priority_region=213, regions=[225], category='A&B'),
            Shop(fesh=1105, priority_region=213, regions=[225], phone_display_options='-'),
            Shop(fesh=1106, priority_region=213, regions=[225], tariff="FIX", online=False),
        ]

        cls.index.offers += [
            Offer(hyperid=1001, fesh=1104, glparams=[GLParam(param_id=201, value=1)]),
            Offer(hyperid=1002, fesh=1105),
            Offer(hyperid=1003, fesh=1106),
            Offer(
                title='For glfilter',
                hyperid=1004,
                hid=14,
                glparams=[
                    GLParam(param_id=202, value=0),
                    GLParam(param_id=203, value=10),
                    GLParam(param_id=204, value=100),
                ],
            ),
            Offer(fesh=1104, vclusterid=1000000001),
        ]

        cls.index.shops += [
            Shop(
                fesh=1,
                priority_region=213,
                regions=[225],
                name='Moscow Quality 5',
                cpa=Shop.CPA_REAL,
                new_shop_rating=NewShopRating(new_rating_total=5.0),
            ),
            Shop(
                fesh=2,
                priority_region=213,
                regions=[225],
                name='Moscow Quality 4',
                cpa=Shop.CPA_REAL,
                new_shop_rating=NewShopRating(new_rating_total=4.0),
            ),
            Shop(
                fesh=3,
                priority_region=213,
                regions=[225],
                name='Moscow Quality 3',
                cpa=Shop.CPA_REAL,
                new_shop_rating=NewShopRating(new_rating_total=3.0),
            ),
            Shop(
                fesh=4,
                priority_region=2,
                regions=[213],
                name='Piter Quality 5',
                cpa=Shop.CPA_REAL,
                new_shop_rating=NewShopRating(new_rating_total=5.0),
            ),
            Shop(
                fesh=5,
                priority_region=11,
                regions=[225],
                name='Ryazan Quality 5',
                cpa=Shop.CPA_REAL,
                new_shop_rating=NewShopRating(new_rating_total=5.0),
            ),
            Shop(
                fesh=6,
                priority_region=11,
                regions=[225],
                name='Ryazan Quality 5',
                cpa=Shop.CPA_REAL,
                new_shop_rating=NewShopRating(new_rating_total=5.0),
            ),
            Shop(
                fesh=7,
                priority_region=213,
                regions=[225],
                name='Moscow Quality 5 A',
                cpa=Shop.CPA_REAL,
                new_shop_rating=NewShopRating(new_rating_total=5.0),
            ),
            Shop(
                fesh=8,
                priority_region=213,
                regions=[225],
                name='Moscow Quality 5 B',
                cpa=Shop.CPA_REAL,
                new_shop_rating=NewShopRating(new_rating_total=5.0),
            ),
        ]

        cls.index.models += [
            Model(hyperid=100, hid=15),
            Model(hyperid=600, hid=16),
        ]

        cls.index.offers += [
            # desktop/mobile on stock sort
            Offer(
                fesh=1,
                title='Moscow Quality 5',
                hyperid=100,
                price=64000,
                bid=12,
                delivery_options=[DeliveryOption(price=0, day_from=10, day_to=20, order_before=24)],
            ),
            Offer(fesh=2, title='Moscow Quality 4', hyperid=100, price=65000, bid=13),
            # desktop price
            # 8'th row price interval from min, min price set to 10000 in statistics
            # 100 rur delivery included in line calculation for moscow
            Offer(fesh=1, title='Moscow Quality 5', hyperid=210, price=10300, bid=14, randx=100, fee=0),
            Offer(fesh=2, title='Moscow Quality 4', hyperid=210, price=10400, bid=15, randx=200, fee=0),
            Offer(fesh=3, title='Moscow Quality 3', hyperid=210, price=11000, bid=15, randx=300, fee=0),
            Offer(
                title='promo 1',
                fesh=11,
                hyperid=217,
                hid=216,
                price_history=100,
                benefit_price=150,
                promo=Promo(
                    promo_type=PromoType.N_PLUS_ONE, key='xMpCOKC5I4INzFCab3WEmw', required_quantity=3, free_quantity=34
                ),
            ),
            Offer(fesh=1, title='Moscow Quality 5', hyperid=219, price=13000, hid=218, discount=6, randx=100),
            Offer(fesh=2, title='Moscow Quality 2', hyperid=219, price=13001, hid=218, discount=10, randx=200),
            Offer(fesh=3, title='Moscow Quality 3', hyperid=219, price=22000, hid=218, discount=15, randx=150),
            # 6'th row price interval from min, min price set to 10000 in statistics
            # 100 rur delivery included in line calculation for moscow
            Offer(fesh=1, title='Moscow Quality 5', hyperid=220, price=10900, bid=14, randx=100, fee=0),
            Offer(fesh=2, title='Moscow Quality 4', hyperid=220, price=11000, bid=15, randx=200, fee=0),
            Offer(fesh=3, title='Moscow Quality 3', hyperid=220, price=13000, bid=15, randx=300, fee=0),
            # 6'th and 8'th row price interval from min, min price set to 10000 in statistics
            # 100 rur delivery included in line calculation for moscow
            Offer(fesh=1, title='Moscow Quality 5', hyperid=230, price=10300, bid=14, randx=100, fee=0),
            Offer(fesh=2, title='Moscow Quality 4', hyperid=230, price=11000, bid=15, randx=200, fee=0),
            Offer(fesh=3, title='Moscow Quality 3', hyperid=230, price=13000, bid=15, randx=300, fee=0),
            # desktop/mobile locality
            Offer(fesh=3, title='Moscow Quality 3', hyperid=300, price=65000, bid=16, fee=0),
            Offer(fesh=4, title='Piter Quality 5', hyperid=300, price=65000, bid=17, fee=0),
            # desktop/mobile shop quality
            Offer(fesh=1, title='Moscow Quality 5', hyperid=400, price=65000, bid=18, randx=100, fee=0),
            Offer(fesh=2, title='Moscow Quality 4', hyperid=400, price=65000, bid=19, randx=200, fee=0),
            Offer(fesh=3, title='Moscow Quality 3', hyperid=400, price=65000, bid=20, randx=300, fee=0),
            # desktop cpa
            Offer(
                fesh=1, title='Moscow Quality 5', hyperid=500, price=65000, bid=21, cpa=Offer.CPA_REAL, randx=100, fee=0
            ),
            Offer(
                fesh=2, title='Moscow Quality 4', hyperid=500, price=65000, bid=22, cpa=Offer.CPA_NO, randx=100, fee=0
            ),
            # mobile cpa
            Offer(
                fesh=1, title='Moscow Quality 5', hyperid=600, price=65000, bid=23, cpa=Offer.CPA_REAL, randx=100, fee=0
            ),
            Offer(
                fesh=2, title='Moscow Quality 4', hyperid=600, price=64000, bid=24, cpa=Offer.CPA_NO, randx=100, fee=0
            ),
            # 12% price interval from min, min price set to 10000 in statistics
            Offer(fesh=1, title='Moscow Quality 5', hyperid=720, price=10900, bid=24, randx=100, fee=0),
            Offer(fesh=2, title='Moscow Quality 4', hyperid=720, price=11000, bid=25, randx=200, fee=0),
            Offer(fesh=3, title='Moscow Quality 3', hyperid=720, price=13000, bid=25, randx=300, fee=0),
            # regression
            Offer(fesh=1, title='Moscow Quality 5', hyperid=800, price=65000, bid=20, randx=100, fee=0),
            Offer(fesh=2, title='Moscow Quality 4', hyperid=800, price=65000, bid=27, randx=100, fee=0),
            # desktop Ryazan (non Moscow/Piter)
            Offer(fesh=5, title='Ryazan Quality 5', hyperid=900, price=10400, bid=15, randx=100, fee=0),
            Offer(fesh=6, title='Ryazan Quality 4', hyperid=900, price=10500, bid=15, randx=200, fee=0),
            # MARKETOUT-8923 fee-based offers rotation
            Offer(
                fesh=7,
                title='Moscow Quality 5 A',
                hyperid=1000,
                price=10400,
                bid=15,
                fee=100,
                randx=101,
                ts=235463,
                waremd5="aaaaaaaaaaaaaaaaaaaaaa",
            ),
            Offer(
                fesh=8,
                title='Moscow Quality 5 B',
                hyperid=1000,
                price=10400,
                bid=15,
                fee=200,
                randx=100,
                ts=354134,
                waremd5="bbbbbbbbbbbbbbbbbbbbbb",
            ),
        ]

        cls.index.regional_models += [
            RegionalModel(hyperid=210, offers=10, price_min=10000),
            RegionalModel(hyperid=220, offers=10, price_min=10000),
            RegionalModel(hyperid=230, offers=10, price_min=10000),
            RegionalModel(hyperid=900, offers=10, price_min=10000),
        ]

        # Для тестирования групповой модели
        cls.index.models += [
            Model(hid=401, hyperid=311, group_hyperid=310),
            Model(hid=401, hyperid=312, group_hyperid=310),
        ]
        cls.index.offers += [
            Offer(fesh=1104, hid=401, hyperid=310, bid=10),
            Offer(fesh=1104, hid=401, hyperid=311, bid=30, ts=3111104),
            Offer(fesh=1104, hid=401, hyperid=312, bid=20),
        ]
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 3111104).respond(0.005)

        # data for test_default_offer_click_price_after_threshold_position
        cls.index.shops += [
            Shop(fesh=2001, priority_region=213, name='Moscow 1'),
            Shop(fesh=2002, priority_region=213, name='Moscow 2'),
            Shop(fesh=2003, priority_region=213, name='Moscow 3'),
            Shop(fesh=2004, priority_region=213, name='Moscow 4'),
            Shop(fesh=2005, priority_region=213, name='Moscow 5'),
            Shop(fesh=2006, priority_region=213, name='Moscow 6'),
            Shop(fesh=2007, priority_region=213, name='Moscow 7'),
            Shop(fesh=2008, priority_region=213, name='Moscow 8', cpa=Shop.CPA_REAL),
            Shop(fesh=2009, priority_region=213, name='Moscow 9'),
        ]

        cls.index.offers += [
            Offer(title='offer 1', hyperid=901, bid=100, fesh=2001, cpa=Offer.CPA_NO),
            Offer(title='offer 2', hyperid=901, bid=90, fesh=2002, cpa=Offer.CPA_NO),
            Offer(title='offer 3', hyperid=901, bid=80, fesh=2003, cpa=Offer.CPA_NO),
            Offer(title='offer 4', hyperid=901, bid=70, fesh=2004, cpa=Offer.CPA_NO),
            Offer(title='offer 5', hyperid=901, bid=60, fesh=2005, cpa=Offer.CPA_NO),
            Offer(title='offer 6', hyperid=901, bid=50, fesh=2006, cpa=Offer.CPA_NO),
            Offer(title='offer 7', hyperid=901, bid=40, fesh=2007, cpa=Offer.CPA_NO),
            Offer(
                title='offer 8',
                hyperid=901,
                bid=30,
                fesh=2008,
                cpa=Offer.CPA_REAL,
                waremd5='NiOHSU9r5mE9rqZDp3BqQA',
                pickup_buckets=[5008],
            ),
            Offer(title='offer 9', hyperid=901, bid=20, fesh=2009, cpa=Offer.CPA_NO),
        ]

        # MARKETOUT-9085: outlet in defaultoffer
        cls.index.outlets += [
            Outlet(
                fesh=2008,
                point_id=123456,
                region=213,
                locality_name='Moscow',
                thoroughfare_name='Karl Marx av.',
                premise_number='1',
                block='2',
                km='3',
                estate='4',
                office_number='5',
                building='6',
            ),
        ]

        cls.index.offers += [Offer(title='offer with outlets', hyperid=902, fesh=2016, pickup_buckets=[5016])]

        cls.index.regiontree += [Region(rid=2, region_type=Region.CITY)]

        cls.index.outlets += [
            Outlet(fesh=2016, point_id=1, region=2),  # should fit for &rids=2
            Outlet(fesh=2016, point_id=2, region=2),
            Outlet(fesh=2016, point_id=3, region=2),
            Outlet(fesh=2016, point_id=4, region=2),
            Outlet(fesh=2016, point_id=5, region=3),  # should NOT fit for &rids=2
            Outlet(fesh=2016, point_id=6, region=3),  # should NOT fit for &rids=2
        ]

        cls.index.pickup_buckets += [
            PickupBucket(
                bucket_id=5008,
                fesh=2008,
                carriers=[99],
                options=[PickupOption(outlet_id=123456)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            PickupBucket(
                bucket_id=5016,
                fesh=2016,
                carriers=[99],
                options=[
                    PickupOption(outlet_id=1),
                    PickupOption(outlet_id=2),
                    PickupOption(outlet_id=3),
                    PickupOption(outlet_id=4),
                    PickupOption(outlet_id=5),
                    PickupOption(outlet_id=6),
                ],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
        ]

    # Basic tests
    def test_xml_output(self):
        response = self.report.request_xml('place=defaultoffer&show-urls=showPhone&hyperid=1001&rids=213&pp=200')
        self.assertFragmentIn(
            response,
            '''
            <search_results>
                <offer shop-id="1104">
                    <url-show-phone/>
                    <hyper_id>1001</hyper_id>
                    <ds_category>A&B</ds_category>
                </offer>
            </search_results>
            ''',
        )

    def test_xml_output_mobicard(self):
        response = self.report.request_xml(
            'place=defaultoffer&show-urls=callPhone&hyperid=1001&rids=213&pp=200&phone=1'
        )
        self.assertFragmentIn(
            response,
            '''
            <search_results>
                <offer shop-id="1104">
                    <url-mobicard/>
                    <hyper_id>1001</hyper_id>
                </offer>
            </search_results>
            ''',
        )

    # Shop has forbidden show phone: model 1002 -> offer shop id 1105
    def test_xml_output_no_phone(self):
        for phone in (0, 1):
            response = self.report.request_xml('place=defaultoffer&hyperid=1002&rids=213&pp=200&phone={}'.format(phone))
            self.assertFragmentNotIn(response, "<url-mobicard/>", preserve_order=True)
            self.assertFragmentNotIn(response, "<url-show-phone/>", preserve_order=True)

    def test_json_output(self):
        response = self.report.request_json('place=defaultoffer&hyperid=1001&rids=213&pp=200')

        self.assertFragmentIn(
            response,
            {"results": [{"model": {"id": 1001}, "entity": "offer", "shop": {"id": 1104}}]},
            preserve_order=True,
        )

        # у той же модели тот же дефолтный оффер на prime
        response = self.report.request_json('place=prime&rids=213&pp=200&bsformat=2&hid=14&use-default-offers=1')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"entity": "product", "id": 1001, "offers": {"items": [{"entity": "offer", "shop": {"id": 1104}}]}}
                ]
            },
            preserve_order=True,
        )

    def test_no_offline_shops(self):
        response = self.report.request_xml('place=defaultoffer&hyperid=1003&rids=213&pp=200&debug=1')
        self.assertFragmentIn(
            response,
            '''
                <search_results total="0">
                    <debug>
                        <basesearch>
                            <counters>
                                <counter name="TOTAL_DOCUMENTS_PROCESSED" value="1"/>
                                <counter name="TOTAL_DOCUMENTS_ACCEPTED" value="0"/>
                            </counters>
                        </basesearch>
                    </debug>
                </search_results>
        ''',
            preserve_order=True,
        )

    def test_group_model_expansion(self):
        # Просим групповую модель, запрос расширяется с учетом модификаций и
        # возвращается модификация с наибольшей релевантностью.
        response = self.report.request_xml('place=defaultoffer&hyperid=310')
        self.assertEqual(1, response.count("<offer/>"))
        self.assertFragmentIn(response, "<offer><hyper_id>311</hyper_id></offer>")

    def test_expensive_discount(self):
        fragment = {"entity": "offer", "benefit": {"type": "default"}, "prices": {"discount": {"percent": 10}}}
        response = self.report.request_json(
            'place=prime&use-default-offers=1&rearr-factors=expensive_discount=true&discount-check-min-price=50&hid=218'
        )
        self.assertFragmentIn(response, fragment)
        response = self.report.request_json(
            'place=prime&use-default-offers=1&rearr-factors=expensive_discount=true&hid=218'
        )
        self.assertFragmentIn(response, fragment)
        response = self.report.request_json('place=prime&use-default-offers=1&discount-check-min-price=0&hid=218')
        self.assertFragmentIn(response, fragment)
        response = self.report.request_json(
            'place=prime&use-default-offers=1&rearr-factors=expensive_discount=true&discount-check-min-price=0&hid=218'
        )
        self.assertFragmentNotIn(response, fragment)
        # проверяем, что при запросе с promo-type срез бэйджей выключается
        response = self.report.request_json(
            'place=prime&use-default-offers=1&rearr-factors=expensive_discount=true&discount-check-min-price=0&hid=218&promo-type=market'
        )
        self.assertFragmentIn(response, fragment)
        response = self.report.request_json(
            'place=defaultoffer&rearr-factors=expensive_discount=true&discount-check-min-price=0&hyperid=219'
        )
        fragment = {"entity": "offer", "prices": {"discount": {"percent": 10}}}
        self.assertFragmentIn(response, fragment)

    def test_low_quality(self):
        fragment = {"entity": "offer", "benefit": {"type": "default"}, "promos": [{"type": "n-plus-m"}]}
        response = self.report.request_json('place=prime&use-default-offers=1&hid=216')
        self.assertFragmentIn(response, fragment)
        response = self.report.request_json(
            'place=prime&use-default-offers=1&rearr-factors=expensive_discount=true&promo-min-quality=1&hid=216'
        )
        self.assertFragmentNotIn(response, fragment)
        response = self.report.request_json(
            'place=prime&use-default-offers=1&rearr-factors=expensive_discount=true&hid=216'
        )
        self.assertFragmentNotIn(response, fragment)
        # проверяем, что при запросе с promo-type срез бэйджей выключается
        response = self.report.request_json(
            'place=prime&use-default-offers=1&rearr-factors=expensive_discount=true&hid=216&promo-type=market'
        )
        self.assertFragmentIn(response, fragment)
        fragment = {"entity": "offer", "promos": [{"type": "n-plus-m"}]}
        response = self.report.request_json('place=defaultoffer&rearr-factors=expensive_discount=true&hyperid=217')
        self.assertFragmentIn(response, fragment)

    def test_filters_in_output(self):
        response = self.report.request_json('place=defaultoffer&hyperid=1001')
        self.assertFragmentIn(
            response, {"entity": "offer", "model": {"id": 1001}, "filters": [{"id": "201", "values": [{"id": "1"}]}]}
        )

    def test_outlet_in_defaultoffer(self):
        response = self.report.request_json('place=defaultoffer&hyperid=901&fesh=2008')
        self.assertFragmentIn(
            response, {"entity": "offer", "model": {"id": 901}, "outlet": {"entity": "outlet", "id": "123456"}}
        )

    def check_glfilter_request(self, glfilter, should_remain):
        fragment = '<offer><raw-title>For glfilter</raw-title></offer>'
        response = self.report.request_xml('place=defaultoffer&hid=14&hyperid=1004&glfilter=' + glfilter)
        if should_remain:
            return self.assertFragmentIn(response, fragment)
        else:
            return self.assertFragmentNotIn(response, fragment)

    def check_glfilter_request_prime(self, glfilter, should_remain):
        fragment = {
            "results": [
                {
                    "entity": "product",
                    "id": 1004,
                    "offers": {"items": [{"entity": "offer", "titles": {"raw": "For glfilter"}}]},
                }
            ]
        }
        response = self.report.request_json('place=prime&hid=14&use-default-offers=1&glfilter=' + glfilter)
        if should_remain:
            return self.assertFragmentIn(response, fragment)
        else:
            return self.assertFragmentNotIn(response, fragment)

    def test_bool_glfilter(self):
        self.check_glfilter_request('202:0', True)
        self.check_glfilter_request('202:1', False)

    def test_enum_glfilter(self):
        self.check_glfilter_request('203:10', True)
        self.check_glfilter_request('203:15', False)

    def test_numeric_glfilter(self):
        self.check_glfilter_request('204:50~200', True)
        self.check_glfilter_request('204:10~50', False)
        self.check_glfilter_request('204:200~500', False)

    def test_gl_filters_on_prime(self):
        self.check_glfilter_request_prime('202:0', True)
        self.check_glfilter_request_prime('202:1', False)

        self.check_glfilter_request_prime('203:10', True)
        self.check_glfilter_request_prime('203:15', False)

        self.check_glfilter_request_prime('204:50~200', True)
        self.check_glfilter_request_prime('204:10~50', False)
        self.check_glfilter_request_prime('204:200~500', False)

    def test_isdeliveryincluded_not_specified(self):
        response = self.report.request_json('place=defaultoffer&hyperid=220')
        self.assertFragmentIn(response, {"search": {"isDeliveryIncluded": False}})
        self.assertFragmentIn(
            response, {"results": [{"entity": "offer", "model": {"id": 220}, "prices": {"isDeliveryIncluded": False}}]}
        )

    def test_isdeliveryincluded_specified_and_true(self):
        response = self.report.request_json('place=defaultoffer&hyperid=220&deliveryincluded=1')
        self.assertFragmentIn(response, {"search": {"isDeliveryIncluded": True}})
        self.assertFragmentIn(
            response, {"results": [{"entity": "offer", "model": {"id": 220}, "prices": {"isDeliveryIncluded": True}}]}
        )

    def test_isdeliveryincluded_specified_and_false(self):
        response = self.report.request_json('place=defaultoffer&hyperid=220&deliveryincluded=0')
        self.assertFragmentIn(response, {"search": {"isDeliveryIncluded": False}})
        self.assertFragmentIn(
            response, {"results": [{"entity": "offer", "model": {"id": 220}, "prices": {"isDeliveryIncluded": False}}]}
        )

    def test_geo_url(self):
        response_json = self.report.request_json('place=defaultoffer&hyperid=901&show-urls=geo&fesh=2008')
        parsed = json.loads(str(response_json))
        geo_url = None
        try:
            for result in parsed['search']['results']:
                if result['titles']['raw'] == 'offer 8':
                    geo_url = result['urls']['geo']
                    break
        except KeyError:
            geo_url = None

        self.assertFalse(geo_url is None)

        response_xml = self.report.request_xml('place=defaultoffer&hyperid=901&show-urls=geo&fesh=2008')
        self.assertFragmentIn(
            response_xml, '<offer><raw-title>offer 8</raw-title><url-shop>{}</url-shop></offer>'.format(geo_url)
        )

    def test_invalid_glfilter_log_message(self):
        self.report.request_xml('place=defaultoffer&hyperid=1001&rids=213&pp=200&glfilter=123:456')
        self.error_log.expect('Error in glfilters syntax:').once()

    def test_missing_pp(self):
        self.report.request_xml(
            'place=defaultoffer&hyperid=1001&rids=213&ip=127.0.0.1', strict=False, add_defaults=False
        )
        self.error_log.expect('Some client has not set PP value').once()
        self.error_log.expect(code=3043)

    def test_missing_hyperid(self):
        """
        Проверка, что при отсувтии параметра hyperid в лог записывается верная ошибка, выдача пустая
        """
        response = self.report.request_xml('place=defaultoffer&rids=213&ip=127.0.0.1')
        self.assertFragmentIn(response, '<search_results total="0"/>')
        self.error_log.expect(code='3629').once()
        self.error_log.expect(code=3043)

    def test_no_vcluster_grouping(self):
        # Что тестируем: на выдаче остается один оффер для случая, когда у оффера есть vcluster_id
        # MARKETOUT-9648
        response_xml = self.report.request_xml('place=defaultoffer&hyperid=1000000001&rids=213&ip=127.0.0.1')
        self.assertEqual(response_xml.count('<offer/>'), 1)

    def test_outlets_count_on_prime(self):
        rids_outletcounts = [('2', 4), ('3', 2), ('', 6)]
        for rid, outletcount in rids_outletcounts:
            response = self.report.request_json('place=defaultoffer&hyperid=902&rids={}'.format(rid))

            self.assertFragmentIn(
                response,
                {
                    "entity": "offer",
                    "titles": {
                        "raw": "offer with outlets",
                    },
                    "shop": {
                        "id": 2016,
                        "outletsCount": outletcount,
                    },
                },
            )

    def test_total_renderable(self):
        request = 'place=defaultoffer&hyperid={0}&rids=213&pp=200'
        response = self.report.request_xml(request.format(1001))
        self.assertFragmentIn(
            response, '<search_results adult="*" book-now-detected="*" sales-detected="*" total="1"></search_results>'
        )
        self.access_log.expect(total_renderable='1')
        response = self.report.request_xml(request.format(1005))
        self.assertFragmentIn(
            response, '<search_results adult="*" book-now-detected="*" sales-detected="*" total="0"/>'
        )
        self.access_log.expect(total_renderable='0')

    @classmethod
    def prepare_default_offer_mn(cls):
        cls.index.shops += [
            Shop(fesh=9001, priority_region=213, cpa=Shop.CPA_REAL),
            Shop(fesh=9002, priority_region=213, cpa=Shop.CPA_REAL),
            Shop(fesh=9003, priority_region=213, cpa=Shop.CPA_REAL),
            Shop(fesh=9004, priority_region=213, cpa=Shop.CPA_REAL),
            Shop(fesh=9005, priority_region=213, cpa=Shop.CPA_REAL, cpc=Shop.CPC_NO),
        ]

        cls.index.offers += [
            # low MN over CPA
            Offer(hyperid=10001, fee=1000, bid=200, price=10000, fesh=9001, ts=1000101, cpa=Offer.CPA_REAL),
            Offer(hyperid=10001, bid=100, price=10000, fesh=9002, ts=1000102, cpa=Offer.CPA_NO),
            # high MN over CPA
            Offer(
                hyperid=10001,
                fee=500,
                bid=40,
                price=10000,
                fesh=9003,
                ts=1000103,
                cpa=Offer.CPA_REAL,
                waremd5='wgrU12_pd1mqJ6DJm_9nEA',
            ),
            Offer(
                hyperid=10001,
                bid=60,
                price=10000,
                fesh=9004,
                ts=1000104,
                cpa=Offer.CPA_NO,
                waremd5='5sxI46jbm2GFkqhqV5YBZA',
            ),
            # low MN, CPC-only
            Offer(hyperid=10002, bid=200, price=10000, fesh=9001, ts=1000201, cpa=Offer.CPA_NO),
            # high MN, CPA-only
            Offer(hyperid=10002, bid=100, price=10000, fesh=9005, ts=1000202, cpa=Offer.CPA_REAL, has_url=False),
            Offer(hyperid=10003, bid=200, price=10000, fesh=9001, ts=1000301, cpa=Offer.CPA_NO),
        ]

        # hyperid=10001
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 1000101).respond(0.001)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 1000102).respond(0.005)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 1000103).respond(0.003)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 1000104).respond(0.007)

        # hyperid=10002
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 1000201).respond(0.001)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 1000202).respond(0.004)

        # hyperid=10003
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 1000301).respond(0.001)

    def test_default_offer_mn(self):
        """
        see MARKETOUT-14602
        Проверяем следующие факты для карточки 10001(есть CPA предложения):
        1) Ранжирование по формуле, по MN значению, без приоритетa CPA
        2) Используются правильные формулы для предсказания покупок и в CPC и в CPA части
        3) Стоимость клика и комиссия без автоброкера

        Делаем запрос в place=productoffers т.к. именно оттуда десктоп и тач берут ДО ныне
        """
        response = self.report.request_json(
            'place=productoffers&hyperid=10001&rids=213&pp=21&offers-set=default&show-urls=external,cpa&debug=1'
        )
        # Проверяем запись в фича лог обоих матрикснетов
        self.feature_log.expect(ware_md5='wgrU12_pd1mqJ6DJm_9nEA', other={'all_matrixnet_values': 'CpcBuy:0.003;'})
        # Проверяем выбранные формулы
        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                "benefit": {"type": "default"},
                "debug": {
                    "fullFormulaInfo": [
                        {"tag": "CpcBuy", "name": "MNA_DO_20190325_simple_factors_6w_shops99m_QuerySoftMax"},
                    ]
                },
            },
        )
        # Проверяем правильность расчитанного значения (только CPC_MN, т.к. оффер не CPA)
        self.assertFragmentIn(
            response,
            {
                "properties": {"TS": "1000103"},
                "rank": [
                    {
                        "name": "MATRIXNET_VALUE",
                        "width": "30",
                        "value": "3221225",  # 0.003 * 2^30
                    },
                ],
            },
        )
        # Проверяем стоимость клика
        self.click_log.expect(ClickType.EXTERNAL, hyper_id=10001, cp=13, cb=13, min_bid=13, shop_id=9003)

    def test_default_offer_mn_only_cpc_model(self):
        """
        see MARKETOUT-14602
        Проверяем что для карточки 10003 (нет CPA предложений) используются правильная формула

        Делаем запрос в place=productoffers т.к. именно оттуда десктоп и тач берут ДО ныне
        """
        response = self.report.request_json(
            'place=productoffers&hyperid=10003&rids=213&pp=21&offers-set=default&show-urls=external,cpa&debug=1'
        )
        # Проверяем выбранные формулы
        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                "benefit": {"type": "default"},
                "debug": {
                    "fullFormulaInfo": [
                        {"tag": "CpcBuy", "name": "MNA_DO_20190720_shift_goal_all_factors_shops90m_c0_QSM_1000iter"},
                    ]
                },
            },
        )

    def test_default_offer_mn_with_relax_filters(self):
        """
        see MARKETOUT-14602
        Проверяем следующие факты для карточки 10001(есть CPA предложения):
        1) Ранжирование по формуле, по MN значению c проиритетом CPA
        2) Используются правильные формулы для предсказания покупок и в CPA части

        Делаем запрос в place=productoffers т.к. именно оттуда десктоп и тач берут ДО ныне
        Запрос с relax-filters=1
        """
        response = self.report.request_json(
            'place=productoffers&hyperid=10001&rids=213&pp=21&offers-set=default&show-urls=external,cpa&debug=1'
            '&relax-filters=1'
        )
        # Проверяем запись в фича лог обоих матрикснетов
        self.feature_log.expect(ware_md5='wgrU12_pd1mqJ6DJm_9nEA', other={'all_matrixnet_values': 'CpcBuy:0.003;'})
        # Проверяем выбранные формулы
        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                "benefit": {"type": "default"},
                "debug": {
                    "fullFormulaInfo": [
                        {"tag": "CpcBuy", "name": "MNA_DO_20190325_simple_factors_6w_shops99m_QuerySoftMax"},
                    ]
                },
            },
        )
        # Проверяем правильность расчитанного значения (только CPC_MN, т.к. оффер не CPA)
        self.assertFragmentIn(
            response,
            {
                "properties": {"TS": "1000103"},
                "rank": [
                    {
                        "name": "MATRIXNET_VALUE",
                        "width": "30",
                        "value": "3221225",  # 0.003 * 2^30
                    },
                ],
            },
        )
        # Проверяем стоимость клика
        self.click_log.expect(ClickType.EXTERNAL, hyper_id=10001, cp=13, cb=13, min_bid=13, shop_id=9003)

    def test_default_offer_mn_cpc_multiplier(self):
        """
        see MARKETOUT-14602
        Проверяем, что переданный множитель для CPC применился.

        Делаем запрост в place=productoffers т.к. именно оттуда десктоп и тач берут ДО ныне
        """
        response = self.report.request_json(
            'place=productoffers&hyperid=10001&rids=213&pp=21&offers-set=default&show-urls=external,cpa&debug=1'
            '&rearr-factors=market_default_offer_mn_ranking_cpc_multiplier=0.5;use_offer_type_priority_as_main_factor_in_do=0;'
        )
        # Проверяем правильность расчитанного значения (CPC_MN * 0.5)
        self.assertFragmentIn(
            response,
            {
                "properties": {"TS": "1000104"},
                "rank": [
                    {
                        "name": "MATRIXNET_VALUE",
                        "width": "30",
                        "value": "3758096",  # (0.007 * 0.5) * 2^30
                    },
                ],
            },
        )

    def test_default_offer_experiment_mn(self):
        """
        see MARKETOUT-15359
        Проверяем, что формула берётся из реарр-флага

        Делаем запрост в place=productoffers т.к. именно оттуда десктоп и тач берут ДО ныне
        """
        response = self.report.request_json(
            'place=productoffers&hyperid=10003&rids=213&pp=21&offers-set=default&show-urls=external,cpa&debug=1'
            '&rearr-factors=market_default_offer_cpc_formula=MNA_HybridAuctionCpcCtr2430'
        )
        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                "benefit": {"type": "default"},
                "debug": {
                    "fullFormulaInfo": [
                        {"tag": "CpcBuy", "name": "MNA_HybridAuctionCpcCtr2430"},
                    ]
                },
            },
        )

        response_desktop = self.report.request_json(
            'place=productoffers&hyperid=10001&rids=213&pp=21&offers-set=default&show-urls=external,cpa&debug=1'
            '&rearr-factors=market_default_offer_cpa_formula=MNA_HybridAuctionCpaCtr2430'
        )
        self.assertFragmentIn(
            response_desktop,
            {
                "entity": "offer",
                "benefit": {"type": "default"},
                "debug": {
                    "fullFormulaInfo": [
                        {"tag": "CpcBuy", "name": "MNA_HybridAuctionCpaCtr2430"},
                    ]
                },
            },
        )

    def test_offercard_url(self):
        """Что тестируем: плейс defaultoffer умеет отдавать offercard-ссылки и писать их в лог показов
        Задаем запрос с show-urls=offercard и проверяем выдачу и лог
        """
        response = self.report.request_xml('place=defaultoffer&show-urls=offercard&hyperid=1001&rids=213&pp=200')
        self.assertFragmentIn(
            response,
            '''
            <search_results>
                <offer shop-id="1104">
                    <url-offercard/>
                    <hyper_id>1001</hyper_id>
                </offer>
            </search_results>
            ''',
        )

        response = self.report.request_json(
            'place=defaultoffer&bsformat=2&show-urls=offercard&hyperid=1001&rids=213&pp=200'
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"entity": "offer", "model": {"id": 1001}, "shop": {"id": 1104}, "urls": {"offercard": NotEmpty()}}
                ]
            },
        )

        self.show_log.expect(url_type=UrlType.OFFERCARD).times(2)

    @classmethod
    def prepare_sort_models_by_default_offer_price(cls):

        cls.index.shops += [
            Shop(
                fesh=251,
                priority_region=213,
                regions=[225],
                name='Moscow Quality 5',
                cpa=Shop.CPA_REAL,
                new_shop_rating=NewShopRating(new_rating_total=5.0),
            ),
            Shop(
                fesh=252,
                priority_region=213,
                regions=[225],
                name='Moscow Quality 4',
                cpa=Shop.CPA_NO,
                new_shop_rating=NewShopRating(new_rating_total=3.0),
            ),
        ]

        for model_id in range(100):
            hyper_id = 7001 + model_id
            cls.index.models += [Model(hyperid=hyper_id, hid=900)]

            cls.index.offers += [
                Offer(
                    fesh=251,
                    title='offer n%d-251' % model_id,
                    hyperid=hyper_id,
                    price=model_id * 100 + 250 + 300 * (model_id % 2),
                    delivery_options=[DeliveryOption(price=300 * (model_id % 3), day_to=0)],
                    cpa=Shop.CPA_REAL,
                    ts=7001000 + model_id,
                ),
                Offer(
                    fesh=252,
                    title='offer n%d-252' % model_id,
                    hyperid=hyper_id,
                    price=model_id * 100 + 50,
                    delivery_options=[DeliveryOption(price=model_id * 10, day_to=3)],
                ),
            ]
            # Choose default offer
            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 7001000 + model_id).respond(0.002)

    def check_default_offer_order(self, response, items):
        self.assertFragmentIn(
            response,
            [
                {
                    'entity': 'product',
                    'id': item[0],
                    'offers': {'items': [{'entity': 'offer', 'prices': {'value': str(item[1])}}]},
                }
                for item in items
            ],
            preserve_order=True,
        )

    def test_sort_models_by_default_offer_price(self):
        """
        Исходные данные задаем таким образом, чтоб цена дефолтного оффера не коррелировала с минимальной ценой модели
        Запрашиваем с сортировку по цене из дефолтного оффера
        Проверяем порядок возрастания цены дефолтного в ответе и то, что модельки не потерялись из выдачи
        """
        model_id = 7001
        offer_price = 250

        # первые 50 элементов осортированные гарантированно правильно
        for page in range(1, 6):
            response = self.report.request_json(
                'place=prime&rids=213&pp=200&bsformat=2&hid=900&how=aprice&use-default-offers=1&page=%d&numdoc=10'
                % page
            )
            items = []
            # странный цикл, чтобы перемешать модели
            # весь его смысле - цена возрастает и никогда не уменьшается
            for index in range(10):
                items.append((model_id, offer_price))

                if model_id == 7001:
                    model_id = 7003
                    offer_price = 450
                elif model_id % 2 == 0:
                    model_id += 3
                else:
                    model_id -= 1
                    offer_price += 200

            self.check_default_offer_order(response, items)

        # на 6-й странице заканчиваются модели с дефолтным оффером, поэтому закономерность нарушена, провреим порядок вручную
        response = self.report.request_json(
            'place=prime&rids=213&pp=200&bsformat=2&hid=900&how=aprice&use-default-offers=1&page=6&numdoc=10'
        )
        self.assertFragmentIn(
            response,
            [
                {'entity': 'product', 'id': mod_id, 'offers': {'items': [{'entity': 'offer'}]}}
                for mod_id in [7050, 7053, 7052, 7055, 7054, 7057, 7056, 7059, 7058, 7060]
            ],
            preserve_order=True,
        )

        # на 7-й странице нет дефолтных офферов и все отсортировано по цене модели
        response = self.report.request_json(
            'place=prime&rids=213&pp=200&bsformat=2&hid=900&how=aprice&use-default-offers=1&page=7&numdoc=10'
        )
        self.assertFragmentIn(
            response,
            [
                {'entity': 'product', 'id': mod_id, 'offers': {'items': NoKey('items')}}
                for mod_id in [7061, 7062, 7063, 7064, 7065, 7066, 7067, 7068, 7069, 7070]
            ],
            preserve_order=True,
        )

    def test_sort_models_by_default_offer_price_delivery_included(self):
        """
        Задаем стоимость доставки таким образом, чтобы цена с учетом доставки не коррелировала
         с ценой без учета доставки, проверяем, что цена в дефолтном оффере возрастает
        """

        response = self.report.request_json(
            'place=prime&rids=213&pp=200&bsformat=2&hid=900&how=aprice&use-default-offers=1&page=1&numdoc=10&deliveryincluded=1'
        )

        self.check_default_offer_order(
            response,
            [
                (7001, 250),
                (7004, 850),
                (7007, 850),
                (7002, 950),
                (7005, 950),
                (7003, 1050),
                (7010, 1450),
                (7013, 1450),
                (7008, 1550),
                (7011, 1550),
            ],
        )

    @classmethod
    def prepare_sort_models_with_cutprice_by_default_offer_price(cls):
        cls.index.models += [
            Model(hyperid=7200, hid=950),
            Model(hyperid=7201, hid=950),
            Model(hyperid=7202, hid=950),
        ]
        cls.index.offers += [
            Offer(title='7200 cutprice offer', hyperid=7200, price=100, is_cutprice=True),
            Offer(title='7200 DO', hyperid=7200, price=150),
            Offer(title='7201 DO', hyperid=7201, price=120),
            Offer(title='7202 cutprice DO', hyperid=7202, price=110, is_cutprice=True),
        ]

    def test_sort_models_with_cutprice_by_default_offer_price(self):
        """
        Убеждаемся, что при сортировке по цене учитывается цена оффера, соответствующего параметру good-state
        """
        # good-state=<empty>:
        # 1) modelid=7202 DO_price=110
        # 2) modelid=7201 DO_price=120
        # 3) modelid=7200 DO_price=150 (без фильтрации по good-state в качестве DO выбирается новый оффер)
        response = self.report.request_json(
            "place=prime&hid=950&use-default-offers=1&how=aprice&allow-collapsing=1&show-cutprice=1"
        )
        self.assertFragmentIn(
            response,
            [
                {
                    "entity": "product",
                    "id": 7202,
                    "offers": {
                        "items": [
                            {
                                "titles": {"raw": "7202 cutprice DO"},
                                "prices": {"value": "110"},
                            }
                        ],
                    },
                },
                {
                    "entity": "product",
                    "id": 7201,
                    "offers": {
                        "items": [
                            {
                                "titles": {"raw": "7201 DO"},
                                "prices": {"value": "120"},
                            }
                        ],
                    },
                },
                {
                    "entity": "product",
                    "id": 7200,
                    "offers": {
                        "items": [
                            {
                                "titles": {"raw": "7200 DO"},
                                "prices": {"value": "150"},
                            }
                        ],
                    },
                },
            ],
            preserve_order=True,
            allow_different_len=False,
        )

        # good-state=new&onstock=1:
        # 1) modelid=7201 DO_price=120
        # 2) modelid=7200 DO_price=150
        response = self.report.request_json(
            "place=prime&hid=950&use-default-offers=1&how=aprice&allow-collapsing=1&show-cutprice=1"
            "&onstock=1&good-state=new"
        )
        self.assertFragmentIn(
            response,
            [
                {
                    "entity": "product",
                    "id": 7201,
                    "offers": {
                        "items": [
                            {
                                "titles": {"raw": "7201 DO"},
                                "prices": {"value": "120"},
                            }
                        ],
                    },
                },
                {
                    "entity": "product",
                    "id": 7200,
                    "offers": {
                        "items": [
                            {
                                "titles": {"raw": "7200 DO"},
                                "prices": {"value": "150"},
                            }
                        ],
                    },
                },
            ],
            preserve_order=True,
            allow_different_len=False,
        )

        # good-state=cutprice&onstock=1:
        # 1) modelid=7200 DO_price=100
        # 2) modelid=7202 DO_price=110
        response = self.report.request_json(
            "place=prime&hid=950&use-default-offers=1&how=aprice&allow-collapsing=1&show-cutprice=1"
            "&onstock=1&good-state=cutprice"
        )
        self.assertFragmentIn(
            response,
            [
                {
                    "entity": "product",
                    "id": 7200,
                    "offers": {
                        "items": [
                            {
                                "titles": {"raw": "7200 cutprice offer"},
                                "prices": {"value": "100"},
                            }
                        ],
                    },
                },
                {
                    "entity": "product",
                    "id": 7202,
                    "offers": {
                        "items": [
                            {
                                "titles": {"raw": "7202 cutprice DO"},
                                "prices": {"value": "110"},
                            }
                        ],
                    },
                },
            ],
            preserve_order=True,
            allow_different_len=False,
        )

    @classmethod
    def prepare_sort_group_and_nogroup_models(cls):
        cls.index.shops += [
            Shop(
                fesh=391,
                priority_region=213,
                regions=[225],
                name='Moscow Quality 5',
                cpa=Shop.CPA_REAL,
                new_shop_rating=NewShopRating(new_rating_total=5.0),
            ),
            Shop(
                fesh=392,
                priority_region=213,
                regions=[225],
                name='Moscow Quality 4',
                cpa=Shop.CPA_NO,
                new_shop_rating=NewShopRating(new_rating_total=3.0),
            ),
            Shop(
                fesh=393,
                priority_region=213,
                regions=[225],
                name='Moscow Quality 4',
                cpa=Shop.CPA_NO,
                new_shop_rating=NewShopRating(new_rating_total=3.0),
            ),
        ]

        cls.index.hypertree += [
            HyperCategory(hid=850, output_type=HyperCategoryType.GURU, has_groups=True),
        ]

        cls.index.model_groups += [ModelGroup(hyperid=1599, title='Apple MacBook Air', hid=850)]

        cls.index.models += [
            Model(hyperid=1500, hid=850),
            Model(hyperid=1501, hid=850),
            Model(hyperid=1510, hid=850, group_hyperid=1599),
        ]

        cls.index.offers += [
            Offer(
                fesh=391,
                title='offer 1500 1',
                hyperid=1500,
                price=200,
                delivery_options=[DeliveryOption(price=100, day_to=0)],
                ts=1005001,
            ),
            Offer(
                fesh=392,
                title='offer 1500 2',
                hyperid=1500,
                price=300,
                delivery_options=[DeliveryOption(price=100, day_to=0)],
                ts=1005002,
            ),
            Offer(
                fesh=391,
                title='offer 1500 1',
                hyperid=1501,
                price=250,
                delivery_options=[DeliveryOption(price=100, day_to=0)],
                ts=1005011,
            ),
            Offer(
                fesh=392,
                title='offer 1500 2',
                hyperid=1501,
                price=351,
                delivery_options=[DeliveryOption(price=100, day_to=0)],
                ts=1005012,
            ),
            Offer(
                fesh=393,
                title='offer 1500 1',
                hyperid=1510,
                price=150,
                delivery_options=[DeliveryOption(price=100, day_to=0)],
                ts=1005021,
            ),
            Offer(
                fesh=391,
                title='offer 1500 1',
                hyperid=1510,
                price=350,
                delivery_options=[DeliveryOption(price=100, day_to=0)],
                ts=1005021,
            ),
            Offer(
                fesh=392,
                title='offer 1500 2',
                hyperid=1510,
                price=500,
                delivery_options=[DeliveryOption(price=100, day_to=0)],
                ts=1005022,
            ),
        ]

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 1005001).respond(0.001)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 1005002).respond(0.02)

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 1005011).respond(0.001)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 1005012).respond(0.02)

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 1005021).respond(0.001)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 1005022).respond(0.02)

        cls.dynamic.market_dynamic.disabled_cpc_shops += [DynamicShop(393)]

    def test_sort_group_category_aprice(self):
        """
        Проверяем, что модели отсортированы по дефолтной цене оффера, а групповые модели - по минимальной цене групповой модели
        У модификации магазин с минимальной ценой отключен по динамику. Т.е. сразу проверяем, что минимальная цена в групповой модели
        учитывает динамические статистики
        """
        # модификации отфильтровываются ещё на релевантности, см. MARKETOUT-19760
        response = self.report.request_json(
            'place=prime&hid=850&use-default-offers=1&allow-collapsing=1&how=aprice&rids=213'
            '&rearr-factors=market_metadoc_search=no'
        )
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {
                            "entity": "product",
                            "type": "model",
                            "id": 1500,
                            "offers": {"items": [{"entity": "offer", "prices": {"value": "300"}}]},
                        },
                        {"entity": "product", "type": "group", "id": 1599, "prices": {"min": "350"}},
                        {
                            "entity": "product",
                            "type": "model",
                            "id": 1501,
                            "offers": {"items": [{"entity": "offer", "prices": {"value": "351"}}]},
                        },
                    ]
                },
                "intents": [{"ownCount": 3}],
            },
            preserve_order=True,
        )

    @classmethod
    def prepare_default_order_with_default_offer(cls):
        cls.index.regiontree += [
            Region(rid=25, name='Tver'),
        ]

        cls.index.delivery_buckets += [
            DeliveryBucket(
                bucket_id=211,
                fesh=254,
                carriers=[99],
                regional_options=[
                    RegionalDelivery(rid=25, options=[DeliveryOption(price=400, day_from=1, day_to=3, order_before=23)])
                ],
            ),
        ]

        cls.index.shops += [
            Shop(
                fesh=253,
                priority_region=25,
                regions=[],
                name='Regional 5',
                cpa=Shop.CPA_NO,
                new_shop_rating=NewShopRating(new_rating_total=5.0),
            ),
            Shop(
                fesh=254,
                priority_region=213,
                regions=[225],
                name='Moscow',
                cpa=Shop.CPA_NO,
                new_shop_rating=NewShopRating(new_rating_total=5.0),
            ),
        ]

        # offer in region 225
        cls.index.offers += [
            Offer(
                fesh=253,
                title='local offer',
                hid=901,
                price=10000,
                delivery_options=[DeliveryOption(day_to=2, price=50)],
            )
        ]

        # model with offer in 213
        cls.index.models += [Model(hyperid=8000, hid=901, title='moscow model')]

        cls.index.offers += [
            Offer(
                fesh=254,
                title='moscow offer',
                hyperid=8000,
                price=10000,
                delivery_options=[DeliveryOption(day_to=2, price=50)],
                delivery_buckets=[211],
            )
        ]

    def test_default_order_with_default_offer(self):
        '''
        Проверяем, что дефолтная сортировка работает одинаково
        в эксперименте с сортировкой по цене дефолтного оффера и вне его
        при выключенной галке local-offers-first
        '''
        for query in [
            'place=prime&rids=25&pp=200&bsformat=2&hid=901&local-offers-first=0&use-default-offers=1',
            'place=prime&rids=25&pp=200&bsformat=2&hid=901&local-offers-first=0',
        ]:
            response = self.report.request_json(query)
            self.assertFragmentIn(
                response,
                [
                    {'entity': 'product', 'titles': {'raw': 'moscow model'}},
                    {'entity': 'offer', 'titles': {'raw': 'local offer'}},
                    {'entity': 'offer', 'titles': {'raw': 'moscow offer'}},
                ],
                preserve_order=True,
            )

        # и при включенной
        for query in [
            'place=prime&rids=25&pp=200&bsformat=2&hid=901&local-offers-first=1&use-default-offers=1',
            'place=prime&rids=25&pp=200&bsformat=2&hid=901&local-offers-first=1',
        ]:
            response = self.report.request_json(query)
            self.assertFragmentIn(
                response,
                [
                    {'entity': 'offer', 'titles': {'raw': 'local offer'}},
                    {'entity': 'regionalDelimiter'},
                    {'entity': 'product', 'titles': {'raw': 'moscow model'}},
                    {'entity': 'offer', 'titles': {'raw': 'moscow offer'}},
                ],
                preserve_order=True,
            )

    @classmethod
    def prepare_default_offer_experment_mn_with_promo(cls):
        cls.index.shops += [
            Shop(fesh=9091, priority_region=213, regions=[49], cpa=Shop.CPA_REAL),
            Shop(fesh=9092, priority_region=213, cpa=Shop.CPA_REAL),
            Shop(fesh=9093, priority_region=213, cpa=Shop.CPA_REAL),
            Shop(fesh=9094, priority_region=213, cpa=Shop.CPA_REAL),
            Shop(fesh=9095, priority_region=213, cpa=Shop.CPA_REAL, cpc=Shop.CPC_NO),
            Shop(fesh=9096, priority_region=49, cpa=Shop.CPA_REAL),
            Shop(fesh=9097, priority_region=213, cpa=Shop.CPA_REAL, cpa20=True),
        ]

        cls.index.offers += [
            # high MN over CPA
            Offer(
                hyperid=10041,
                fee=500,
                bid=40,
                price=10000,
                fesh=9093,
                ts=1000143,
                cpa=Offer.CPA_REAL,
                title='ip1',
                randx=32,
            ),
            Offer(hyperid=10041, bid=60, price=10000, fesh=9094, ts=1000144, cpa=Offer.CPA_NO, title='ip2', randx=33),
        ]

        # region 49
        cls.index.offers += [
            Offer(
                hyperid=10041,
                fee=1000,
                bid=200,
                price=10000,
                fesh=9096,
                ts=1000145,
                cpa=Offer.CPA_REAL,
                randx=35,
                title='ip8',
            )
        ]

        # offer promo
        cls.index.offers += [
            Offer(
                hyperid=10043,
                fee=1000,
                bid=200,
                price=10000,
                fesh=9097,
                ts=1000342,
                cpa=Offer.CPA_REAL,
                randx=37,
                title='ip10',
            )
        ]

        # hyperid=10041
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 1000141).respond(0.001)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 1000142).respond(0.005)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 1000143).respond(0.003)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 1000144).respond(0.006)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 1000145).respond(0.006)

        # hyperid=10042
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 1000241).respond(0.001)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 1000242).respond(0.004)

        # hyperid=10043
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 1000341).respond(0.001)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 1000342).respond(0.004)

    def test_default_offer_promo_local_delivery_priority(self):
        """
        Проверяем, что оффер с локальной доставкой приоритетнее, чем промо оффер в другом регионе
        """
        response = self.report.request_json(
            'place=productoffers&hyperid=10041&rids=49&pp=21&offers-set=default&show-urls=external,cpa'
            '&rearr-factors=market_default_offer_mn_ranking_cpa_priority=1'
        )
        self.assertFragmentIn(response, {"entity": "offer", "titles": {"raw": "ip8"}})

    def test_pp_substitution(self):
        """
        Проверяем, что репорт подменяется pp мест, где ДО запрашивается вместе со списком офферов,
        на pp дефолтного оффера.
        """
        yandex_uid = 0
        for provided_pp, do_pp in [
            (6, 200),
            (13, 201),
            (21, 201),
            (61, 206),
            (62, 208),
            (63, 207),
            (64, 209),
            (65, 205),
            (46, 632),
            (606, 630),
            (613, 630),
            (706, 730),
            (713, 730),
            (721, 730),
            (806, 830),
            (813, 830),
            (821, 830),
            (6746, 6730),
            (6846, 6830),
            (1706, 1730),
            (1806, 1830),
            (1746, 1732),
            (1846, 1832),
        ]:
            # Что бы отличать записи в show log
            yandex_uid += 1
            _ = self.report.request_json(
                'place=productoffers&hyperid=1001&pp={pp}&offers-set=default&show-urls=external&yandexuid={yuid}'.format(
                    pp=provided_pp, yuid=yandex_uid
                )
            )
            self.show_log.expect(pp=do_pp, yandex_uid=str(yandex_uid))

    @classmethod
    def prepare_home_region_filter(cls):
        """Создаем регион Пекин и два магазина - один из России, а другой из Китая
        Создаем офферы этих магазинов в двух моделях
        """
        cls.index.regiontree += [
            Region(
                rid=134,
                name='Китай',
                tz_offset=28800,
                region_type=Region.COUNTRY,
                children=[
                    Region(rid=10590, name='Пекин', tz_offset=28800),
                ],
            ),
        ]

        cls.index.shops += [
            Shop(fesh=18001, priority_region=10590, cpa=Shop.CPA_REAL, regions=[225], home_region=134),
            Shop(fesh=18002, priority_region=213, cpa=Shop.CPA_REAL, home_region=225),
        ]

        cls.index.offers += [
            Offer(hyperid=10141, fesh=18001, cpa=Offer.CPA_REAL, randx=111, title='global 1'),
            Offer(hyperid=10141, fesh=18002, cpa=Offer.CPA_REAL, randx=222, title='local 1'),
            Offer(hyperid=10142, fesh=18001, cpa=Offer.CPA_REAL, randx=333, title='global 2'),
            Offer(hyperid=10142, fesh=18002, cpa=Offer.CPA_REAL, randx=444, title='local 2'),
        ]

    def test_home_region_filter(self):
        """Проверяем, что при запросе с home_region_filter=134 на выдаче появляются
        офферы из китайского магазина, а без фильтра - офферы из российского (за
        счет большего randx)
        Проверяем в вариантах с одним hyperid и с несколькимим
        """
        response = self.report.request_json(
            'place=defaultoffer&hyperid=10141&hyperid=10142&rids=213&home_region_filter=134'
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"entity": "offer", "titles": {"raw": "global 1"}},
                    {"entity": "offer", "titles": {"raw": "global 2"}},
                ]
            },
        )
        self.assertFragmentNotIn(response, {"titles": {"raw": "local 1"}})
        self.assertFragmentNotIn(response, {"titles": {"raw": "local 2"}})

        response = self.report.request_json('place=defaultoffer&hyperid=10141&rids=213&home_region_filter=134')
        self.assertFragmentIn(response, {"results": [{"entity": "offer", "titles": {"raw": "global 1"}}]})
        self.assertFragmentNotIn(response, {"titles": {"raw": "local 1"}})

        # Запросы без фильтра
        response = self.report.request_json('place=defaultoffer&hyperid=10141&hyperid=10142&rids=213')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"entity": "offer", "titles": {"raw": "local 1"}},
                    {"entity": "offer", "titles": {"raw": "local 2"}},
                ]
            },
        )
        self.assertFragmentNotIn(response, {"titles": {"raw": "global 1"}})
        self.assertFragmentNotIn(response, {"titles": {"raw": "global 2"}})

        response = self.report.request_json('place=defaultoffer&hyperid=10141&rids=213')
        self.assertFragmentIn(response, {"results": [{"entity": "offer", "titles": {"raw": "local 1"}}]})
        self.assertFragmentNotIn(response, {"titles": {"raw": "global 1"}})

    @classmethod
    def prepare_bulk_group_model(cls):
        cls.index.models += [Model(hyperid=19101, group_hyperid=19111)]

        cls.index.offers += [
            Offer(hyperid=19101, ts=19101),  # High MN, from modification
            Offer(hyperid=19111, ts=19111),  # Low MN, from group model
        ]
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 19101).respond(0.1)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 19111).respond(0.001)

    def test_bulk_group_model(self):
        """
        Проверяем, что при запросе с несколькими hyperid, id групповой модели расширяется и находится ДО среди всех модификаций.
        Унификация с работой с указанием одного hyperid.
        """
        response = self.report.request_json('place=defaultoffer&show-urls=external,cpa' '&hyperid=19111&hyperid=19102')
        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                "model": {
                    "id": 19101,
                    "parentId": 19111,
                },
            },
        )

    @classmethod
    def prepare_xml_encoding_for_title(cls):
        cls.index.models += [Model(hyperid=20101)]

        cls.index.offers += [
            Offer(hyperid=20101, title='A & B'),
        ]

    def test_xml_encoding_for_title(self):
        """
        Проверяем, что при запросе с несколькими hyperid, id групповой модели расширяется и находится ДО среди всех модификаций.
        Унификация с работой с указанием одного hyperid.
        """
        response = self.report.request_xml('place=defaultoffer' '&hyperid=20101')
        self.assertFragmentIn(
            response,
            '''
            <name>A & B</name>
        ''',
        )

    @classmethod
    def prepare_default_offer_exp(cls):
        cls.index.offers += [Offer(hyperid=889901, price=900, ts=190001), Offer(hyperid=889901, price=800, ts=190002)]

        cls.index.shops += [
            Shop(
                fesh=8001,
                priority_region=213,
                name='Moscow 1',
                new_shop_rating=NewShopRating(new_rating_total=3.0, rec_and_nonrec_pub_count=200000),
            ),
            Shop(
                fesh=8002,
                priority_region=213,
                name='Moscow 2',
                new_shop_rating=NewShopRating(new_rating_total=4.0, rec_and_nonrec_pub_count=60000),
            ),
            Shop(
                fesh=8003,
                priority_region=213,
                name='Moscow 3',
                new_shop_rating=NewShopRating(new_rating_total=4.0, rec_and_nonrec_pub_count=70000),
            ),
            Shop(
                fesh=8004,
                priority_region=213,
                name='Moscow 4',
                new_shop_rating=NewShopRating(new_rating_total=5.0, rec_and_nonrec_pub_count=500),
            ),
            Shop(
                fesh=8005,
                priority_region=213,
                name='Moscow 5',
                new_shop_rating=NewShopRating(new_rating_total=4.0, rec_and_nonrec_pub_count=10000),
            ),
        ]

        cls.index.offers += [
            Offer(hyperid=889902, fesh=8001, price=400),
            Offer(hyperid=889902, fesh=8002, price=700),
            Offer(hyperid=889902, fesh=8003, price=900),
            Offer(hyperid=889902, fesh=8004, price=500),
            Offer(hyperid=889902, fesh=8005, price=600),
        ]

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 190001).respond(0.1)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 190002).respond(0.001)

    def test_default_offer_min_price_exp(self):
        """
        Проверяем, что дефолтный оффер с большей ценой вне экспе,
        и с меньшей - в экспе
        Проверяем рабоыт параметра и реарр-флага
        """
        response = self.report.request_json("place=productoffers&hyperid=889901&offers-set=default")
        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                "benefit": {"type": "default"},
                "prices": {"currency": "RUR", "value": "900"},
            },
        )

        response = self.report.request_json(
            "place=productoffers&hyperid=889901&offers-set=default&default-offer-min-price=1"
        )
        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                "benefit": {"type": "default"},
                "prices": {"currency": "RUR", "value": "800"},
            },
        )

        response = self.report.request_json(
            "place=productoffers&hyperid=889901&offers-set=default&rearr-factors=market_default_offer_by_min_price=1"
        )
        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                "benefit": {"type": "default"},
                "prices": {"currency": "RUR", "value": "800"},
            },
        )

    def test_default_offer_min_price_in_good_shop(self):
        response = self.report.request_json(
            "place=productoffers&hyperid=889902&offers-set=default&rearr-factors=market_default_offer_by_min_price_in_good_shop=1"
        )
        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                "benefit": {"type": "default"},
                "prices": {"currency": "RUR", "value": "700"},
            },
        )

    @classmethod
    def prepare_default_offer_auction(cls):
        cls.index.offers += [
            Offer(fesh=8005, hyperid=889903, title='Fang-class', price=100, bid=100, ts=190003),
            Offer(fesh=8006, hyperid=889903, title='VCX-100', price=100, bid=46, ts=190004),
            Offer(fesh=8006, hyperid=889903, title='VCX-100', price=100, bid=30, ts=190008),
        ]

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 190003).respond(0.055)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 190004).respond(0.055)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 190008).respond(0.057)

        cls.index.shops += [
            Shop(fesh=8005, priority_region=213, name='MandalMotors'),
            Shop(fesh=8006, priority_region=213, name='Corellian Engineering Corporation'),
        ]

    def request_string_default_offer_auction(self, hyper_id, alpha, beta, gamma, offers_set=None):
        if offers_set is None:
            offers_set = 'default'
        req = "place=productoffers&hyperid={}&offers-set={}&debug=1&rearr-factors=market_enable_default_offer_auction=1;".format(
            hyper_id, offers_set
        )
        req += "market_default_offer_sigmoid_alpha={};market_default_offer_sigmoid_beta={};market_default_offer_sigmoid_gamma={}".format(
            alpha, beta, gamma
        )
        return req

    def test_default_offer_auction_disabled(self):
        """
        Проверяем, что без флагов ДО обычный
        """
        response = self.report.request_json("place=productoffers&hyperid=889903&offers-set=default")
        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                "titles": {"raw": "VCX-100"},
                "benefit": {"type": "default"},
            },
        )

    @classmethod
    def prepare_default_offer_auction_autbrocker_with_min_bids(cls):
        cls.index.regiontree += [
            Region(rid=213, name='Москва'),
        ]
        cls.index.hypertree += [
            HyperCategory(hid=100, output_type=HyperCategoryType.GURU),
        ]
        cls.index.models += [
            Model(hyperid=100000, hid=100, title='Lightsaber'),
            Model(hyperid=100001, hid=100, title='Blaster pistol'),
        ]
        cls.index.offers += [
            Offer(title='Sith lightsaber', hid=100, hyperid=100000, price=15000, bid=90, ts=1),
            Offer(title='Darksaber', hid=100, hyperid=100000, price=3000000, bid=9000, ts=2),
            Offer(title='DL-44', hid=100, hyperid=100001, price=10000, bid=50),
            Offer(title='WESTAR-34', hid=100, hyperid=100001, price=15000, bid=51, ts=3),
        ]
        cls.index.min_bids_category_stats += [
            MinBidsCategory(
                category_id=100,
                geo_group_id=0,
                price_group_id=100,
                drr=0.1,
                search_conversion=0.01,
                card_conversion=0.01,
                full_card_conversion=1.0,
            ),
        ]
        cls.index.min_bids_price_groups += [
            MinBidsPriceGroup(0),
            MinBidsPriceGroup(100),
        ]

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 1).respond(0.056)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 2).respond(0.055)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 3).respond(0.056)

    def test_default_offer_auction_autbrocker_with_min_bids_equal_bid(self):
        """
        Проверяем, что нельзя получить клик прайс меньше мин ставки
        """
        _ = self.report.request_json(self.request_string_default_offer_auction(100001, 0.4, 0.05, 1.0))

        self.show_log.expect(bid=50, click_price=50, min_bid=50)  # min_bid = ceil(0.01 * 0.1 * 15000 / 0.3) = 50

    def test_same_model_min_bids_are_used(self):
        """
        Проверяем, что при рассчете релевантности и в автоброкере используются модельные мин ставки
        и они одинаковые
        """
        response = self.report.request_json(
            self.request_string_default_offer_auction(100000, 0.54, 0.04, 1.0) + "&debug=1"
        )

        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                "titles": {"raw": "Darksaber"},
                "debug": {"properties": {"MODEL_MIN_BID": "50"}},
            },
        )
        self.assertFragmentIn(
            response,
            {
                "logicTrace": [
                    Contains("Default offer auction autobroker uses min bid equal to 90."),
                    Contains("Default offer auction autobroker uses model min bid equal to 50."),
                ]
            },
        )


if __name__ == '__main__':
    main()
