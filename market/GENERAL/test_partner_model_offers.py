#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

import re

from core.types import (
    CpaCategory,
    CpaCategoryType,
    Currency,
    DeliveryBucket,
    DeliveryOption,
    DynamicShop,
    ExchangeRate,
    HyperCategory,
    HyperCategoryType,
    Model,
    NewShopRating,
    Offer,
    Outlet,
    PickupBucket,
    PickupOption,
    Promo,
    PromoType,
    Region,
    Shop,
)
from core.testcase import TestCase, main


class T(TestCase):

    title_regex = re.compile(r'(<offer[^>]*title=")[^"]+("[^>]*>)')

    @classmethod
    def drop_title_if_needed(cls, fragment):
        if not cls.settings.disable_snippet_request:
            return fragment
        return cls.title_regex.sub(r'\1\2', fragment)

    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ["market_money_disable_bids=0"]
        # RANDX randomizing is disabled because these tests don't work with it. See MARKETOUT-21319
        cls.disable_randx_randomize()

    @classmethod
    def prepare_format_fix(cls):
        cls.index.offers += [
            Offer(
                hyperid=1,
                adult=True,
                title='leonardo',
                bid=100,
                fesh=1,
                delivery_options=[DeliveryOption(price=345)],
                ts=1,
            ),
            Offer(
                hyperid=1,
                adult=False,
                title='donatello',
                bid=50,
                fesh=2,
                delivery_options=[DeliveryOption(price=0)],
                ts=2,
            ),
            Offer(hyperid=1, adult=False, title='rafael', bid=10, fesh=3, price=444, price_old=555, ts=3),
            Offer(hyperid=2, title='michelangelo', bid=200, fesh=1, ts=4),
            Offer(hyperid=2, title='april', bid=100, fesh=2, ts=5),
            Offer(hyperid=2, title='splinter', bid=10, fesh=3, ts=6),
        ]

        cls.index.shops += [
            Shop(fesh=1, priority_region=10, new_shop_rating=NewShopRating(new_rating_total=5.0)),
            Shop(fesh=2, priority_region=10, new_shop_rating=NewShopRating(new_rating_total=4.0)),
            Shop(
                fesh=3, priority_region=34, new_shop_rating=NewShopRating(new_rating_total=3.0), pickup_buckets=[5001]
            ),
        ]

        cls.index.outlets += [
            Outlet(fesh=3, point_type=Outlet.FOR_PICKUP, region=10, point_id=501),
        ]

        cls.index.pickup_buckets += [
            PickupBucket(
                bucket_id=5001,
                fesh=3,
                carriers=[99],
                options=[PickupOption(outlet_id=501)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
        ]

    def test_format_fix(self):
        response = self.report.request_xml('place=partner_model_offers&hyperid=1,2&rids=10&adult=1')
        self.assertFragmentIn(
            response,
            self.drop_title_if_needed(
                '''
        <search_results index-generation="+" currency="RUR">
            <model hyperid="1">
                <offers count="3">
                    <offer title="leonardo" onstock="0" price="100">
                        <shop name="SHOP-1" priority_region="10" rating="5"/>
                        <delivery region="10" price="345"/>
                    </offer>
                    <offer title="donatello" onstock="0" price="100">
                        <shop name="SHOP-2" priority_region="10" rating="4"/>
                        <delivery region="10" price="0"/>
                    </offer>
                    <offer title="rafael" onstock="1" price="444" old_price="555" discount_percent="20">
                        <shop name="SHOP-3" priority_region="34" rating="3"/>
                    </offer>
                </offers>
            </model>
            <model hyperid="2">
                <offers count="3">
                    <offer title="michelangelo" onstock="1" price="100">
                        <shop name="SHOP-1" priority_region="10" rating="5"/>
                        <delivery region="10" price="100"/>
                    </offer>
                    <offer title="april" onstock="1" price="100">
                        <shop name="SHOP-2" priority_region="10" rating="4"/>
                        <delivery region="10" price="100"/>
                    </offer>
                    <offer title="splinter" onstock="1" price="100">
                        <shop name="SHOP-3" priority_region="34" rating="3"/>
                    </offer>
                </offers>
            </model>
        </search_results>
        '''
            ),
            preserve_order=False,
            allow_different_len=False,
        )

    @classmethod
    def prepare_sorting_like_model_card(cls):
        '''
        Offer with ts=301 has greates CPM, at the same time offer with ts=302 has lowerest CPM,
        but autobroker performes re-sorting and in result they are orderred by BID.

        Also check that offer-shipping is store,delivery,pickup
        '''
        cls.index.offers += [
            Offer(hyperid=3, title='chip', bid=103, fesh=4, ts=301, has_delivery_options=False, pickup=True),
            Offer(hyperid=3, title='dale', bid=105, fesh=5, ts=302, has_delivery_options=False, store=True),
            Offer(hyperid=3, title='monty', bid=102, fesh=6, ts=303),
            Offer(hyperid=3, title='gadget', bid=104, fesh=7, ts=304),
        ]

        cls.index.shops += [
            Shop(fesh=4, priority_region=10, pickup_buckets=[5002]),
            Shop(fesh=5, priority_region=10, pickup_buckets=[5003]),
            Shop(fesh=6, priority_region=34, regions=[10]),
            Shop(fesh=7, priority_region=34, regions=[10], cpa=Shop.CPA_REAL),
        ]

        cls.index.outlets += [
            Outlet(fesh=4, point_type=Outlet.FOR_PICKUP, region=10, point_id=502),
            Outlet(fesh=5, point_type=Outlet.FOR_STORE, region=10, point_id=503),
        ]

        cls.index.pickup_buckets += [
            PickupBucket(
                bucket_id=5002,
                fesh=4,
                carriers=[99],
                options=[PickupOption(outlet_id=502)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            PickupBucket(
                bucket_id=5003,
                fesh=5,
                carriers=[99],
                options=[PickupOption(outlet_id=503)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
        ]

    def test_sorting_like_model_card(self):
        response = self.report.request_json('place=productoffers&hyperid=3&rids=10')
        self.assertFragmentIn(
            response,
            [
                {'titles': {'raw': 'dale'}},
                {'titles': {'raw': 'chip'}},
                {'titles': {'raw': 'gadget'}},
                {'titles': {'raw': 'monty'}},
            ],
            preserve_order=True,
        )

        response = self.report.request_xml('place=partner_model_offers&hyperid=3&rids=10')
        self.assertFragmentIn(
            response,
            self.drop_title_if_needed(
                '''
        <offers>
            <offer title="dale"/>
            <offer title="chip"/>
            <offer title="gadget"/>
            <offer title="monty"/>
        </offers>
        '''
            ),
            preserve_order=True,
        )

    @classmethod
    def prepare_region_filter(cls):
        cls.index.offers += [
            Offer(hyperid=4, title='timon', fesh=8),
            Offer(hyperid=4, title='pumba', fesh=9),
        ]

        cls.index.shops += [
            Shop(fesh=8, priority_region=10),
            Shop(fesh=9, priority_region=20),
        ]

    def test_region_filter(self):
        response = self.report.request_xml('place=partner_model_offers&hyperid=4&rids=20')
        self.assertFragmentIn(
            response,
            self.drop_title_if_needed(
                '''
        <offers count="1">
            <offer title="pumba"/>
        </offers>
        '''
            ),
        )

        response = self.report.request_xml('place=partner_model_offers&hyperid=4&rids=10')
        self.assertFragmentIn(
            response,
            self.drop_title_if_needed(
                '''
        <offers count="1">
            <offer title="timon"/>
        </offers>
        '''
            ),
        )

    @classmethod
    def prepare_currency(cls):
        cls.index.regiontree += [Region(rid=23, region_type=Region.COUNTRY, children=[Region(rid=30)])]

        cls.index.currencies += [
            Currency(name=Currency.BYR, country=23, exchange_rates=[ExchangeRate(fr=Currency.RUR, rate=5)])
        ]

        cls.index.offers += [
            Offer(hyperid=5, title='belorus', price=25, fesh=10),
        ]

        cls.index.shops += [
            Shop(fesh=10, priority_region=30, regions=[20]),
        ]

    def test_currency(self):
        response = self.report.request_xml('place=partner_model_offers&hyperid=5&rids=30')
        self.assertFragmentIn(
            response,
            self.drop_title_if_needed(
                '''
        <search_results currency="BYR">
            <model>
                <offers>
                    <offer title="belorus" onstock="1" price="125">
                        <delivery region="30" price="500"/>
                    </offer>
                </offers>
            </model>
        </search_results>
        '''
            ),
        )

    def test_not_found(self):
        response = self.report.request_xml('place=partner_model_offers&hyperid=100500')
        self.assertFragmentIn(
            response,
            '''
        <search_results index-generation="*">
            <model hyperid="100500">
                <offers count="0"/>
            </model>
        </search_results>
        ''',
        )

    @classmethod
    def prepare_cpa_category_aware_models(cls):
        """Создаем две категории, одна из которых - CPC_AND_CPA,
        а другая - CPA_WITH_CPC_PESSIMIZATION
        Создаем модели и офферы в трех разных магазинах,
        различающиеся по bid и fee
        """
        cls.index.regiontree += [
            Region(rid=213, name='Москва', region_type=Region.FEDERATIVE_SUBJECT, tz_offset=10800),
        ]

        cls.index.hypertree += [
            HyperCategory(hid=101, output_type=HyperCategoryType.GURU),
            HyperCategory(hid=102, output_type=HyperCategoryType.GURU),
        ]

        cls.index.cpa_categories += [
            CpaCategory(hid=101, regions=[213], cpa_type=CpaCategoryType.CPC_AND_CPA),
            CpaCategory(hid=102, regions=[213], cpa_type=CpaCategoryType.CPA_WITH_CPC_PESSIMIZATION),
        ]

        cls.index.models += [
            Model(hyperid=101000, title='CPC_AND_CPA model', hid=101),
            Model(hyperid=102000, title='CPA_WITH_CPC_PESS model', hid=102),
        ]

        cls.index.shops += [
            Shop(fesh=20, priority_region=213, cpa=Shop.CPA_REAL),
            Shop(fesh=21, priority_region=213, cpa=Shop.CPA_REAL),
            Shop(fesh=22, priority_region=213, cpa=Shop.CPA_REAL),
        ]

        cls.index.offers += [
            Offer(title='low bid, high fee CPC_AND_CPA', bid=10, hyperid=101000, fesh=20, cpa=Offer.CPA_REAL),
            Offer(title='medium bid, medium fee CPC_AND_CPA', bid=50, hyperid=101000, fesh=21, cpa=Offer.CPA_REAL),
            Offer(title='high bid, low fee CPC_AND_CPA', bid=100, hyperid=101000, fesh=22, cpa=Offer.CPA_REAL),
            Offer(title='high bid, low fee CPA_WITH_CPC_PESS', bid=100, hyperid=102000, fesh=22, cpa=Offer.CPA_REAL),
            Offer(
                title='medium bid, medium fee CPA_WITH_CPC_PESS', bid=50, hyperid=102000, fesh=21, cpa=Offer.CPA_REAL
            ),
            Offer(title='low bid, high fee CPA_WITH_CPC_PESS', bid=10, hyperid=102000, fesh=20, cpa=Offer.CPA_REAL),
        ]

    @classmethod
    def prepare_offers_the_same_in_batch_and_singlemode(cls):
        """
        Создаем 11 моделей
        Для каждой модели задаем уникальную категорию
        Для каждой модели создаем по 11 офферов, сра, возрастающую fee и убывающую ставку
        Для каждого оффера создаем свой магазин в регионе 213

        see https://st.yandex-team.ru/MARKETOUT-11362
        """
        for i in range(11):
            hyperid = 103000 + i
            hid = 10 + i
            cls.index.models += [Model(hyperid=hyperid, hid=hid)]
            for j in range(11):
                fesh = 3000 + 11 * i + j
                cls.index.shops += [Shop(fesh=fesh, priority_region=213, cpa=Shop.CPA_REAL)]
                cls.index.offers += [
                    Offer(
                        title='{}-{}'.format(i, j),
                        hyperid=hyperid,
                        cpa=Offer.CPA_REAL,
                        fee=100 * i + 1,
                        bid=1000 / (i + 1),
                        fesh=fesh,
                    )
                ]

            cls.index.cpa_categories += [
                CpaCategory(hid=hid, cpa_type=CpaCategoryType.CPA_WITH_CPC_PESSIMIZATION, regions=[213])
            ]

    def test_offers_the_same_in_batch_and_singlemode(self):
        """
        Запрашиваем топ офферов для одной модели
        Запрашиваем топ офферов для всех моделей
        Сравниваем сортировку проблемной (до фикса бага) модели -- убеждаемся, что сортировка совпадает
        """
        expected_order = self.drop_title_if_needed(
            '''
        <offers>
            <offer title="8-10"/>
            <offer title="8-9"/>
            <offer title="8-8"/>
            <offer title="8-7"/>
            <offer title="8-6"/>
            <offer title="8-5"/>
            <offer title="8-4"/>
            <offer title="8-3"/>
            <offer title="8-2"/>
            <offer title="8-1"/>
        </offers>
        '''
        )

        response = self.report.request_xml(
            'place=partner_model_offers&hyperid=103008&rids=213&rearr-factors=use_offer_type_priority_as_main_factor_in_top=0'
        )
        self.assertFragmentIn(response, expected_order, preserve_order=True)

        response = self.report.request_xml(
            'place=partner_model_offers&hyperid=103000,103001,103002,103003,103004,'
            '103005,103006,103007,103008,103009,103010&debug=1&rids=213&rearr-factors=use_offer_type_priority_as_main_factor_in_top=0'
        )
        self.assertFragmentIn(response, expected_order, preserve_order=True)

    def test_relevance_cpm_value(self):
        """
        Проверяем что значения CPM в релевантности одинаковое для
        partner_model_offers, bids_recommender и productoffers.
        Т.е. проверяем что используется одинаковое ранжирование и предрассчитанные факторы.
        """
        expected_props_xml = '''
        <document>
            <properties>
                <TS value="1"/>
                <CPM value="3000000"/>
            </properties>
            <rank>
                <DELIVERY_TYPE value="3" width="2"/>
                <CPM value="3000000" width="30"/>
                <ONSTOCK value="0" width="1"/>
                <QUALITY_RATING value="5" width="3"/>
            </rank>
        </document>
        '''
        expected_props_json = {
            "properties": {"CPM": "3000000", "TS": "1"},
            "rank": [
                {"name": "DELIVERY_TYPE", "value": "3"},
                {"name": "CPM", "value": "3000000"},
                {"name": "ONSTOCK", "value": "0"},
                {"name": "QUALITY_RATING", "value": "5"},
            ],
        }

        response = self.report.request_xml(
            'place=partner_model_offers&rearr-factors=market_ranging_cpa_by_ue_in_top_addition_constant_d=0;&hyperid=1&rids=10&ip-rids=10&adult=1&debug=1&debug-doc-count=100'
        )
        self.assertFragmentIn(response, expected_props_xml, preserve_order=True)

        response = self.report.request_xml(
            'place=bids_recommender&rearr-factors=market_ranging_cpa_by_ue_in_top_addition_constant_d=0;&hyperid=1&fesh=1&rids=10&ip-rids=10&adult=1&debug=1&debug-doc-count=100'
        )
        self.assertFragmentIn(response, expected_props_xml, preserve_order=True)

        response = self.report.request_json(
            'place=productoffers&rearr-factors=market_ranging_cpa_by_ue_in_top_addition_constant_d=0;&hyperid=1&fesh=1&rids=10&ip-rids=10&adult=1&debug=1&debug-doc-count=100&pp=6'
        )
        self.assertFragmentIn(response, expected_props_json, preserve_order=True)

    @classmethod
    def prepare_max_min_avg(cls):
        cls.index.models += [Model(hyperid=200300, hid=5000)]
        cls.index.shops += [Shop(fesh=901, priority_region=213, cpa=Shop.CPA_REAL)]
        cls.index.shops += [Shop(fesh=902, priority_region=213, cpa=Shop.CPA_REAL)]

        cls.index.offers += [
            Offer(title='Cheap offer', hyperid=200300, cpa=Offer.CPA_REAL, bid=500, fesh=901, price=100),
            Offer(title='Normal offer', hyperid=200300, cpa=Offer.CPA_REAL, bid=500, fesh=901, price=200),
            Offer(title='Expensive offer', hyperid=200300, cpa=Offer.CPA_REAL, bid=500, fesh=902, price=360),
        ]

        pass

    # MARKETOUT-12737, MARKETOUT-20092
    def test_max_min_name(self):
        """
        Проверяем, значения атрибутов avg, min, max, name для модели 200300

        MARKETOUT-20092 - средняя цена должна считаться так же как в modelinfo. Т.е. берётся медиана цен из региональной
        статистики по модели и ограничивается мин/макс ценами из обновлённой статистики.
        """

        response = self.report.request_xml('place=partner_model_offers&hyperid=200300&rids=213')
        self.assertFragmentIn(response, '<model avg="200" min="100" max="360" name="HYPERID-200300"/>')

        # check that modelinfo outputs the same min/max/avg prices
        response = self.report.request_xml('place=modelinfo&hyperid=200300&rids=213')
        self.assertFragmentIn(response, '<model id="200300"><prices avg="200" max="360" min="100"/></model>')

    def test_dynamic_model_stats(self):
        """
        Предположим, что все 3 магазина были отключены
        Должны получить модель у которой 0 офферов и отсутствуют min,max,avg
        """
        self.dynamic.market_dynamic.disabled_cpc_shops += [DynamicShop(901), DynamicShop(902)]

        self.dynamic.market_dynamic.disabled_cpa_shops += [DynamicShop(901), DynamicShop(902)]

        response = self.report.request_xml('place=partner_model_offers&hyperid=200300&rids=213')
        self.assertFragmentIn(response, '<model hyperid="200300" name="HYPERID-200300"><offers count="0"/></model>')

    @classmethod
    def prepare_group_model_offers(cls):
        cls.index.models += [
            Model(hid=200, hyperid=200410, group_hyperid=200400),
            Model(hid=200, hyperid=200420, group_hyperid=200400),
        ]
        cls.index.offers += [
            Offer(hyperid=200400, title="parent"),
            Offer(hyperid=200410, title="child-1"),
            Offer(hyperid=200420, title="child-2"),
        ]

    def test_group_model_offers(self):
        """
        Проверям, что для групповых моделей выдаются офферы в том числе и конкретных моделей.
        Т.е. так же как для productoffers.
        """

        response = self.report.request_xml("place=partner_model_offers&hyperid=200400")
        self.assertFragmentIn(
            response,
            (
                self.drop_title_if_needed(
                    '''
            <model hyperid="200400"><offers count="3">
            <offer title="parent"/>
            <offer title="child-1"/>
            <offer title="child-2"/>
            </offers></model>
        '''
                )
            ),
            preserve_order=False,
            allow_different_len=False,
        )

    @classmethod
    def prepare_price_statistic_for_long_model_list(cls):
        for id in range(200500, 200600):
            cls.index.models += [Model(hyperid=id, hid=200500)]
            cls.index.offers += [
                Offer(hyperid=id, fesh=200500, price=100),
                Offer(hyperid=id, fesh=200501, price=200),
                Offer(hyperid=id, fesh=200502, price=300),
            ]

    def test_price_statistic_for_long_model_list(self):
        """Check price statistic for models requested within long list"""
        ids = range(200500, 200600)

        request = "place=partner_model_offers&hyperid=" + ",".join(str(id) for id in ids)
        response = self.report.request_xml(request)

        expectation = "<search_results>"
        for id in ids:
            expectation += '<model hyperid="{}" avg="200" max="300" min="100"></model>'.format(id)
        expectation += "</search_results>"

        self.assertFragmentIn(response, expectation)

    @classmethod
    def prepare_price_sort_dont_take_promo_price(cls):
        cls.index.offers += [
            Offer(
                hyperid=39608830,
                title='Камин Electrolux Sphere EFP/P - 2620RLS',
                bid=100,
                fesh=701,
                price=17940,
                delivery_options=[DeliveryOption(price=490)],
                promo=Promo(promo_type=PromoType.PROMO_CODE, key='promo_01012020', discount_value=30),
                ts=1,
            ),
            Offer(
                hyperid=39608830,
                title='Электрический очаг Electrolux Sphere EFP/P - 2620RLS',
                bid=50,
                fesh=702,
                price=12979,
                price_old=16500,
                delivery_options=[DeliveryOption(price=950)],
                ts=2,
            ),
            Offer(
                hyperid=39608830,
                title='Очаг электрический ELECTROLUX Sphere EFP/P-2620RLS',
                bid=10,
                fesh=703,
                price=12990,
                delivery_options=[DeliveryOption(price=800)],
                ts=3,
            ),
        ]

        cls.index.shops += [
            Shop(
                fesh=701,
                priority_region=213,
                new_shop_rating=NewShopRating(new_rating_total=4.0),
                name='Торговая компания CITY',
            ),
            Shop(
                fesh=702, priority_region=213, new_shop_rating=NewShopRating(new_rating_total=4.0), name='KaminDom.ru'
            ),
            Shop(
                fesh=703,
                priority_region=213,
                new_shop_rating=NewShopRating(new_rating_total=5.0),
                name='ОНЛАЙН ТРЕЙД.РУ',
            ),
        ]

    def test_price_sort_dont_take_promo_price(self):
        response = self.report.request_xml('place=partner_model_offers&rids=213&pp=18&hyperid=39608830&how=aprice')
        expected_response = self.drop_title_if_needed(
            '''
        <offers count="3">
            <offer onstock="0" price="12979" title="Электрический очаг Electrolux Sphere EFP/P - 2620RLS">
                <shop name="KaminDom.ru" priority_region="213" rating="4"/>
                <delivery region="213"/>
            </offer>
            <offer onstock="0" price="12990" title="Очаг электрический ELECTROLUX Sphere EFP/P-2620RLS">
                <shop name="ОНЛАЙН ТРЕЙД.РУ" priority_region="213" rating="5"/>
                <delivery region="213"/>
            </offer>
            <offer onstock="0" price="17940" title="Камин Electrolux Sphere EFP/P - 2620RLS">
                <shop name="Торговая компания CITY" priority_region="213" rating="4"/>
                <delivery region="213"/>
            </offer>
        </offers>
        '''
        )
        self.assertFragmentIn(response, expected_response, preserve_order=True)

    @classmethod
    def prepare_offer_id(cls):
        cls.index.models += [Model(hid=201, hyperid=20412)]

        cls.index.offers += [Offer(hyperid=20412, title="pelmeni", offerid=1234, feedid=10930)]

    def test_offer_id(self):
        """
        Проверям, что у офферов есть пара feed_id - offer_id.
        """
        response = self.report.request_xml("place=partner_model_offers&hyperid=20412")
        self.assertFragmentIn(
            response,
            (
                self.drop_title_if_needed(
                    '''
            <model hyperid="20412"><offers count="1">
            <offer offer_id="1234" feed_id="10930" onstock="1" price="100" title="pelmeni"/>
            </offers></model>
        '''
                )
            ),
            preserve_order=False,
            allow_different_len=False,
        )

    @classmethod
    def prepare_shop_id(cls):
        cls.index.models += [Model(hid=200, hyperid=200412)]

        cls.index.shops += [Shop(fesh=9011, priority_region=213, name='first')]

        cls.index.offers += [Offer(hyperid=200412, title="supchek", offerid=1234, fesh=9011)]

    def test_shop_id(self):
        """
        Проверям, что у офферов есть магазин и у него указан fesh.
        """
        response = self.report.request_xml("place=partner_model_offers&hyperid=200412")
        self.assertFragmentIn(
            response,
            (
                self.drop_title_if_needed(
                    '''
            <model hyperid="200412"><offers count="1">
            <offer offer_id="1234" onstock="1" price="100" title="supchek">
            <shop name="first" shop_id="9011"/>
            </offer>
            </offers></model>
        '''
                )
            ),
            preserve_order=False,
            allow_different_len=False,
        )

    @classmethod
    def prepare_delivery_info(cls):
        cls.index.models += [Model(hid=200, hyperid=210412)]

        cls.index.shops += [
            Shop(fesh=1121, priority_region=213, name='first'),
        ]

        cls.index.offers += [
            Offer(
                fesh=1121,
                hyperid=210412,
                title="Pancakes",
                delivery_options=[
                    DeliveryOption(day_from=1, day_to=9, order_before=12, price=100500, shop_delivery_price=22)
                ],
            )
        ]

    def test_delivery_info(self):
        """
        Проверяем, что информация о доставке попала в оффер
        """
        response = self.report.request_xml("place=partner_model_offers&hyperid=210412&rids=213")
        self.assertFragmentIn(
            response,
            (
                self.drop_title_if_needed(
                    '''
        <model hyperid="210412">
        <offers count="1">
        <offer title="Pancakes">
        <shop shop_id="1121"/>
        <delivery currency="RUR" day-from="1" day-to="9" order-before="12" price="100500" region="213"/>
        </offer>
        </offers>
        </model>
        '''
                )
            ),
            preserve_order=False,
            allow_different_len=False,
        )

    @classmethod
    def prepare_too_low_bid(cls):
        cls.index.models += [Model(hid=200, hyperid=210500)]

        cls.index.offers += [Offer(fesh=1, hyperid=210500, bid=1, price=100500, pull_to_min_bid=False)]

    def test_too_low_bid(self):
        """
        Проверяем, оффера с флагом "не подтягивать до мин ставки" не подтягиваются до мин ставки, не смотря на флаг &api=partner
        """
        response = self.report.request_xml("place=partner_model_offers&hyperid=210500&rids=10&api=partner")
        self.assertFragmentIn(
            response,
            '''
        <model hyperid="210500">
            <offers count="0" />
        </model>
        ''',
        )
        self.assertFragmentNotIn(
            response,
            '''
        <offer />
        ''',
        )

    @classmethod
    def prepare_paging(cls):
        cls.index.offers += [Offer(hyperid=300001, bid=i, title="#{}".format(i)) for i in range(0, 100)]

        cls.index.offers += [Offer(hyperid=300002, bid=i, title="#{}".format(i)) for i in range(0, 5)]

    def test_paging(self):
        response300001_1 = self.report.request_xml("place=partner_model_offers&hyperid=300001&page=1")
        response300001_10 = self.report.request_xml("place=partner_model_offers&hyperid=300001&page=10")
        response300002_1 = self.report.request_xml("place=partner_model_offers&hyperid=300002&page=1")
        response300002_10 = self.report.request_xml("place=partner_model_offers&hyperid=300002&page=10")

        self.assertFragmentIn(
            response300001_1,
            (
                self.drop_title_if_needed(
                    '''
            <model hyperid="300001"><offers count="100">
            <offer title="#90"/>
            <offer title="#99"/>
            </offers></model>
        '''
                )
            ),
        )

        self.assertFragmentIn(
            response300001_10,
            (
                self.drop_title_if_needed(
                    '''
            <model hyperid="300001"><offers count="100">
            <offer title="#0"/>
            <offer title="#9"/>
            </offers></model>
        '''
                )
            ),
        )

        self.assertFragmentIn(
            response300002_1,
            (
                self.drop_title_if_needed(
                    '''
            <model hyperid="300002"><offers count="5">
            <offer title="#0"/>
            <offer title="#4"/>
            </offers></model>
        '''
                )
            ),
        )

        self.assertFragmentIn(
            response300002_10,
            (
                self.drop_title_if_needed(
                    '''
                <model hyperid="300002"><offers count="5">
                <offer title="#0"/>
                <offer title="#4"/>
                </offers></model>
            '''
                )
            ),
        )


if __name__ == '__main__':
    main()
