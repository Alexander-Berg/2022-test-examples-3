#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.matcher import Absent, NotEmpty, Contains, Wildcard
from core.types import (
    BlueOffer,
    Currency,
    HyperCategory,
    HyperCategoryType,
    MarketSku,
    MnPlace,
    Model,
    Offer,
    Picture,
    Shop,
    Stream,
    StreamName,
    Tax,
)
from core.testcase import TestCase, main
import hashlib
import base64

SHOP_NAMES = [
    '0 Internet Shop',
    '1 Internet Shop',
    '2 Internet Shop',
    '3 Internet Shop',
    '4 Internet Shop',
    '5 Internet Shop',
    '6 Internet Shop',
    '7 Internet Shop',
    '8 Internet Shop',
    '9 Internet Shop',
    'A Internet Shop',
    'B Internet Shop',
    'C Internet Shop',
    'D Internet Shop',
    'E Internet Shop',
    'F Internet Shop',
]


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']
        # do not set default values
        # @see market/report/lite/core/report.py set_default_search_experiment_flags
        cls.settings.ignore_search_experiment_flags += [
            'market_category_redirect_formula',
            'market_category_relevance_formula',
            'market_category_redirect_treshold',
        ]
        cls.settings.default_search_experiment_flags += ['market_write_category_redirect_features=20']

        cls.matrixnet.set_defaults = False
        cls.matrixnet.on_place(MnPlace.PRODUCT_CLASSIFIER, 0).respond(0.7)

        cls.index.shops += [
            Shop(fesh=1980, priority_region=213, regions=[225], name=SHOP_NAMES[0]),
            Shop(fesh=1981, priority_region=213, regions=[225], name=SHOP_NAMES[1]),
            Shop(fesh=1982, priority_region=213, regions=[225], name=SHOP_NAMES[2]),
            Shop(fesh=1983, priority_region=213, regions=[225], name=SHOP_NAMES[3]),
            Shop(fesh=1984, priority_region=213, regions=[225], name=SHOP_NAMES[4]),
            Shop(fesh=1985, priority_region=213, regions=[225], name=SHOP_NAMES[5]),
            Shop(fesh=1986, priority_region=213, regions=[225], name=SHOP_NAMES[6]),
            Shop(fesh=1987, priority_region=213, regions=[225], name=SHOP_NAMES[7]),
            Shop(fesh=1988, priority_region=213, regions=[225], name=SHOP_NAMES[8]),
            Shop(fesh=1989, priority_region=213, regions=[225], name=SHOP_NAMES[9]),
            Shop(fesh=1990, priority_region=213, regions=[225], name=SHOP_NAMES[10]),
            Shop(fesh=1991, priority_region=213, regions=[225], name=SHOP_NAMES[11]),
            Shop(fesh=1992, priority_region=213, regions=[225], name=SHOP_NAMES[12]),
            Shop(fesh=1993, priority_region=213, regions=[225], name=SHOP_NAMES[13]),
            Shop(fesh=1994, priority_region=213, regions=[225], name=SHOP_NAMES[14]),
            Shop(fesh=1995, priority_region=213, regions=[225], name=SHOP_NAMES[15]),
        ]

        pics = [
            Picture(
                picture_id=base64.b64encode(hashlib.md5(str(i)).digest())[:22], width=100, height=100, group_id=1234
            )
            for i in range(10)
        ]
        cls.index.offers += [
            Offer(hyperid=1, title='kiyanka 1980-1-1', fesh=1980, picture=pics[0], picture_flags=1, randx=1, hid=1),
            Offer(hyperid=1, title='kiyanka 1980-3-1', fesh=1980, picture=pics[0], picture_flags=1, randx=2, hid=1),
            Offer(title='kiyanka 1981-3-1', fesh=1981, picture=pics[0], picture_flags=1, randx=3, hid=1),
            Offer(title='kiyanka 1980-2-1', fesh=1980, picture=pics[0], picture_flags=1, randx=4),
            Offer(title='kiyanka 100-0-1', fesh=1983, picture=pics[0], picture_flags=1, randx=5),
            Offer(title='kiyanka 1980-4-2', fesh=1980, picture=pics[1], picture_flags=2, randx=6),
            Offer(title='kiyanka 102-2-1', fesh=1985, picture=pics[0], picture_flags=1, randx=7),
            Offer(title='kiyanka 1980-5-2', fesh=1980, picture=pics[1], picture_flags=2, randx=8),
            Offer(title='kiyanka 103-3-1', fesh=1986, picture=pics[0], picture_flags=1, randx=9),
            Offer(title='kiyanka 1981-1-1', fesh=1981, picture=pics[0], picture_flags=1, randx=10),
            Offer(title='kiyanka 1987-1-1', fesh=1987, picture=pics[0], picture_flags=1, randx=11),
            Offer(title='kiyanka 1988-1-1', fesh=1988, picture=pics[0], picture_flags=1, randx=12),
            Offer(title='kiyanka 1989-1-1', fesh=1989, picture=pics[0], picture_flags=1, randx=13),
            Offer(title='kiyanka 1990-1-1', fesh=1990, picture=pics[0], picture_flags=1, randx=14),
            Offer(title='kiyanka 1991-1-1', fesh=1991, picture=pics[0], picture_flags=1, randx=15),
            Offer(title='kiyanka 1992-1-1', fesh=1992, picture=pics[0], picture_flags=1, randx=16),
            Offer(title='kiyanka 1993-1-1', fesh=1993, picture=pics[0], picture_flags=1, randx=17),
            Offer(title='kiyanka 1994-1-1', fesh=1994, picture=pics[0], picture_flags=1, randx=18),
            Offer(title='kiyanka 1995-1-1', fesh=1995, picture=pics[0], picture_flags=1, randx=19),
            Offer(title='MARKETOUT-8860 1', fesh=1994, picture=pics[0], picture_flags=1, randx=11),
            Offer(title='MARKETOUT-8860 2', fesh=1994, picture=pics[1], picture_flags=2, randx=12),
            Offer(title='MARKETOUT-8860 3', fesh=1994, picture=pics[2], picture_flags=3, randx=13),
            Offer(title='MARKETOUT-8860 4', fesh=1994, picture=pics[3], picture_flags=4, randx=14),
            Offer(title='MARKETOUT-8860 5', fesh=1994, picture=pics[4], picture_flags=5, randx=15),
            Offer(title='MARKETOUT-8860 6', fesh=1994, picture=pics[5], picture_flags=6, randx=16),
            Offer(title='MARKETOUT-8860 7', fesh=1994, picture=pics[6], picture_flags=7, randx=17),
            Offer(title='MARKETOUT-8860 8', fesh=1994, picture=pics[7], picture_flags=8, randx=18),
            Offer(title='MARKETOUT-8860 9', fesh=1994, picture=pics[8], picture_flags=9, randx=19),
            Offer(title='MARKETOUT-8860 10', fesh=1994, picture=pics[9], picture_flags=10, randx=20),
        ]

        cls.index.shops += [
            Shop(
                fesh=1,
                datafeed_id=1,
                priority_region=213,
                name='virtual_shop',
                currency=Currency.RUR,
                tax_system=Tax.OSN,
                fulfillment_virtual=True,
                cpa=Shop.CPA_REAL,
                virtual_shop_color=Shop.VIRTUAL_SHOP_BLUE,
            ),
            Shop(
                fesh=2,
                datafeed_id=2,
                priority_region=213,
                name='blur_shop',
                currency=Currency.RUR,
                tax_system=Tax.OSN,
                cpa=Shop.CPA_REAL,
                warehouse_id=145,
            ),
        ]
        cls.index.mskus += [
            MarketSku(hid=1, sku=1, fesh=1, hyperid=1, title='blue kiy', blue_offers=[BlueOffer(ts=1000001)]),
            MarketSku(hid=2, sku=2, hyperid=2, title='blue circle', blue_offers=[BlueOffer(ts=2000002)]),
        ]

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 1000001).respond(0.5)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 2000002).respond(0.5)

        cls.index.models += [
            Model(title="kiyanka M 1"),
            Model(title="kiyanka M 2"),
            Model(title="kiyanka M 3"),
        ]

        cls.index.hypertree += [
            HyperCategory(hid=3, output_type=HyperCategoryType.GURU, pessimize_offers=True, show_offers=True)
        ]

        cls.index.shops += [
            Shop(fesh=3000, priority_region=2, regions=[225]),
        ]

        cls.index.models += [
            Model(hyperid=31, title='cocosanka 1', hid=3, ts=31),
            Model(hyperid=32, title='cocosanka 2', hid=3, ts=32),
        ]

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 31).respond(0.5)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 32).respond(0.4)

        cls.matrixnet.on_place(MnPlace.META_REARRANGE, 31).respond(0.3)
        cls.matrixnet.on_place(MnPlace.META_REARRANGE, 32).respond(0.4)

        cls.index.offers += [
            Offer(hyperid=31, fesh=1980, hid=3),
            Offer(hyperid=31, fesh=3000, hid=3, title='cocosanka 1 offer'),
            Offer(hyperid=32, fesh=1981, hid=3),
            Offer(fesh=1983, title='cocosanka offer', hid=3, ts=101983),
            Offer(fesh=1984, hid=3),
        ]

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 101983).respond(0.6)
        cls.matrixnet.on_place(MnPlace.META_REARRANGE, 101983).respond(0.2)

    def _check_using_grouping(self, response, grouping_name):
        self.assertFragmentIn(response, {"g": [Contains(grouping_name)]})

    def test_no_mn_algo(self):
        response = self.report.request_json(
            'place=prime&text=kiyanka&debug=1&rids=213&rearr-factors=market_disable_mn_algo=da'
        )

        self.assertFragmentIn(response, 'Not using a MatrixNet formula')
        self.assertFragmentIn(response, {"rankedWith": "MNA_Trivial"})

    def test_mn_algo_default_parallel(self):
        """Проверяем, что формула включена по умолчанию на параллельном.
        Проверяем дефолтную формулу.
        """
        # отключаем флаг market_parallel_fuzzy - но он неважен с Пантерой
        req = 'place=parallel&text=kiyanka&debug=1&rids=213&rearr-factors=market_parallel_fuzzy=0;'
        response = self.report.request_bs(req)
        self.assertFragmentIn(
            response, 'Using MatrixNet formula for search ranking: MNA_wiz_offer_785353_017_x_803473_083'
        )

        # запрашиваем несуществующие слова и флаг market_parallel_fuzzy (включен по умолчанию), чтобы включился fuzzy -
        # но с Пантерой fuzzy не работает, используется обычная формула
        req = 'place=parallel&text=kiyanka+qwertyuiop&debug=1&rids=213'
        response = self.report.request_bs(req)
        self.assertFragmentIn(
            response, 'Using MatrixNet formula for search ranking: MNA_wiz_offer_785353_017_x_803473_083'
        )

    def test_model_mn_algo_default_parallel(self):
        """Проверяем, что формула для моделей включена по умолчанию на параллельном.
        Проверяем дефолтную формулу.
        """
        request = 'place=parallel&text=kiyanka&debug=1&rids=213'

        # формула по умолчанию
        response = self.report.request_bs(request)
        self.assertFragmentIn(
            response, 'Using MatrixNet formula for model search ranking: MNA_wiz_model_785353_015_x_722610_085'
        )
        # изменение формулы
        response = self.report.request_bs(request + '&rearr-factors=market_model_search_mn_algo=MNA_Relevance')
        self.assertFragmentIn(response, 'Using MatrixNet formula for model search ranking: MNA_Relevance')

    def test_mn_algo_default_prime(self):
        """На запросах с текстом используется формула base_859067_0_3__859045_0_7"""
        response = self.report.request_json('place=prime&text=kiyanka&debug=1&rids=213')
        self.assertFragmentIn(
            response,
            'Using MatrixNet formula: base_859067_0_3__859045_0_7',
        )
        self.assertFragmentIn(response, {"rankedWith": "base_859067_0_3__859045_0_7"})

    def test_mn_algo_default_prime_app(self):
        """На запросах с текстом используется формула base_859067_0_3__859045_0_7"""
        response = self.report.request_json('place=prime&text=kiyanka&debug=1&rids=213&client=ANDROID')
        self.assertFragmentIn(
            response,
            'Using MatrixNet formula: base_859067_0_3__859045_0_7',
        )
        self.assertFragmentIn(
            response,
            {"rankedWith": "base_859067_0_3__859045_0_7"},
        )

        request = 'place=prime&text=kiyanka&debug=1&rids=213&api=content'
        for api_client in ('101', '18932'):
            response = self.report.request_json(request + '&content-api-client=' + api_client)
            self.assertFragmentIn(
                response,
                'Using MatrixNet formula: base_859067_0_3__859045_0_7',
            )
            self.assertFragmentIn(
                response,
                {"rankedWith": "base_859067_0_3__859045_0_7"},
            )

    def test_mn_algo_default_blue_prime(self):
        """На запросах с текстом на синем используется формула MNA_fml_formula_0625_order_yeti_715651x0375_binary_logloss_715653_-4_v2"""
        response = self.report.request_json('place=prime&text=blue+kiy&debug=1&rids=213&rgb=blue')
        self.assertFragmentIn(
            response, 'Using MatrixNet formula: MNA_fml_formula_0625_order_yeti_715651x0375_binary_logloss_715653_-4_v2'
        )
        self.assertFragmentIn(
            response, {"rankedWith": "MNA_fml_formula_0625_order_yeti_715651x0375_binary_logloss_715653_-4_v2"}
        )

    def test_mn_algo_default_prime_api(self):
        """
        На запросах из API используется формула base_859067_0_3__859045_0_7
        Не такая же как на прайме, потому что откатили в https://st.yandex-team.ru/MARKETOUT-42413
        """
        request = 'place=prime&text=kiyanka&debug=1&rids=213&api=content'

        response = self.report.request_json(request)
        self.assertFragmentIn(response, 'Using MatrixNet formula: base_859067_0_3__859045_0_7')
        self.assertFragmentIn(response, {"rankedWith": "base_859067_0_3__859045_0_7"})

        response = self.report.request_json(request + '&client=sovetnik')
        self.assertFragmentIn(response, 'Using MatrixNet formula: MNA_CommonThreshold_v2_251519_m10_x_245752_044')
        self.assertFragmentIn(response, {"rankedWith": "MNA_CommonThreshold_v2_251519_m10_x_245752_044"})

        # кроме запросов на приложения (см test_mn_algo_default_prime_app)

    def test_mn_algo_default_prime_non_text_search(self):
        '''На бестекстовых запросах используется формула base___common_851473_0_3__851440_0_7__fashion_859286_0_6__859190_0_4'''
        response = self.report.request_json('place=prime&hid=1&debug=1&rids=213')
        self.assertFragmentIn(
            response, 'Using MatrixNet formula: base___common_851473_0_3__851440_0_7__fashion_859286_0_6__859190_0_4'
        )
        self.assertFragmentIn(
            response, {"rankedWith": "base___common_851473_0_3__851440_0_7__fashion_859286_0_6__859190_0_4"}
        )

    def test_mn_algo_default_prime_app_non_text_search(self):
        '''На бестекстовых запросах на приложении используется формула base___common_851473_0_3__851440_0_7__fashion_859286_0_6__859190_0_4'''
        response = self.report.request_json('place=prime&hid=1&client=ANDROID&debug=1&rids=213')
        self.assertFragmentIn(
            response, 'Using MatrixNet formula: base___common_851473_0_3__851440_0_7__fashion_859286_0_6__859190_0_4'
        )
        self.assertFragmentIn(
            response, {"rankedWith": "base___common_851473_0_3__851440_0_7__fashion_859286_0_6__859190_0_4"}
        )

    def test_mn_algo_default_prime_blue_non_text_search(self):
        ''' 'На бестекстовых запросах в синий маркет используется формула без порога base___common_851473_0_3__851440_0_7__fashion_859286_0_6__859190_0_4'''
        response = self.report.request_json('place=prime&hid=1&debug=1&rids=213&rgb=blue')
        self.assertFragmentIn(
            response, 'Using MatrixNet formula: base___common_851473_0_3__851440_0_7__fashion_859286_0_6__859190_0_4'
        )
        self.assertFragmentIn(
            response, {"rankedWith": "base___common_851473_0_3__851440_0_7__fashion_859286_0_6__859190_0_4"}
        )

    def test_mn_algo_default_aprice_prime(self):
        """На запросах с текстом при сортировке по цене используется формула base_859067_0_3__859045_0_7"""
        response = self.report.request_json('place=prime&how=aprice&text=kiyanka&debug=1&rids=213')
        self.assertFragmentIn(
            response,
            'Using MatrixNet formula: base_859067_0_3__859045_0_7',
        )
        self.assertFragmentIn(response, {"rankedWith": "base_859067_0_3__859045_0_7"})

    def test_mn_algo_default_app_aprice_prime(self):
        """На запросах с текстом при сортировке по цене на приложении используется формула base_859067_0_3__859045_0_7"""  # noqa
        response = self.report.request_json('place=prime&how=aprice&text=kiyanka&debug=1&rids=213&client=ANDROID')
        self.assertFragmentIn(
            response,
            'Using MatrixNet formula: base_859067_0_3__859045_0_7',
        )
        self.assertFragmentIn(
            response,
            {"rankedWith": "base_859067_0_3__859045_0_7"},
        )

    def test_mn_algo_default_blue_aprice_prime(self):
        """На запросах с текстом при сортировке по цене на синем используется формула MNA_fml_formula_0625_order_yeti_715651x0375_binary_logloss_715653_-4_v2"""
        response = self.report.request_json('place=prime&text=blue+kiy&debug=1&rids=213&how=aprice&rgb=blue')
        self.assertFragmentIn(
            response, 'Using MatrixNet formula: MNA_fml_formula_0625_order_yeti_715651x0375_binary_logloss_715653_-4_v2'
        )
        self.assertFragmentIn(
            response, {"rankedWith": "MNA_fml_formula_0625_order_yeti_715651x0375_binary_logloss_715653_-4_v2"}
        )

    def test_mn_algo_default_prime_sovetnik(self):
        """На запросах с текстом для Советника используется формула MNA_CommonThreshold_v2_251519_m10_x_245752_044"""
        response = self.report.request_json('place=prime&text=kiyanka&debug=1&rids=213&client=sovetnik')
        self.assertFragmentIn(response, 'Using MatrixNet formula: MNA_CommonThreshold_v2_251519_m10_x_245752_044')
        self.assertFragmentIn(response, {"rankedWith": "MNA_CommonThreshold_v2_251519_m10_x_245752_044"})

    def test_mn_algo_default_prime_aprice_sovetnik(self):
        """На запросах с текстом при сортировке по цене для Советника используется формула MNA_CommonThreshold_v2_251519_m10_x_245752_044"""
        response = self.report.request_json('place=prime&how=aprice&text=kiyanka&debug=1&rids=213&client=sovetnik')
        self.assertFragmentIn(response, 'Using MatrixNet formula: MNA_CommonThreshold_v2_251519_m10_x_245752_044')
        self.assertFragmentIn(response, {"rankedWith": "MNA_CommonThreshold_v2_251519_m10_x_245752_044"})

    def test_mn_algo_default_bids_recommender(self):
        """На запросах с текстом для bids_recommender
        используется формула DbD_loss_699480_046_x_Click_699743_x_Binary_699599_030 (такая же, как и на поиске)
        """
        response = self.report.request_xml(
            'place=bids_recommender&text=kiyanka&debug=1&rids=213&fesh=1980&hyperid=1&type=market_search'
        )
        self.assertFragmentIn(
            response, 'Using MatrixNet formula: DbD_loss_699480_046_x_Click_699743_x_Binary_699599_030'
        )

    def test_mn_algo_default_bids_recommender_aprice(self):
        """На запросах с текстом при сортировке по цене для bids_recommender
        используется формула DbD_loss_699480_046_x_Click_699743_x_Binary_699599_030
        https://st.yandex-team.ru/MARKETOUT-18770
        ! На самом деле сортировка по цене в bids_recommender не используется !
        """
        response = self.report.request_xml(
            'place=bids_recommender&how=aprice&text=kiyanka&debug=1&rids=213&fesh=1980&hyperid=1&type=market_search'
        )
        self.assertFragmentIn(
            response, 'Using MatrixNet formula: DbD_loss_699480_046_x_Click_699743_x_Binary_699599_030'
        )

    def test_mn_algo_prime(self):
        mn_algorithms = [
            'MNA_CtrClassification',
            'MNA_Trivial',
            'MNA_Relevance',
            'MNA_RelevanceAndCtr',
        ]

        for mn_algo in mn_algorithms:
            response = self.report.request_json(
                'place=prime&text=kiyanka&debug=1&rids=213&rearr-factors=market_search_mn_algo={}'.format(mn_algo)
            )

            self.assertFragmentIn(response, 'Using MatrixNet formula: {}'.format(mn_algo))
            self.assertFragmentIn(response, {"rankedWith": mn_algo})

    def test_filtered_mn_algo_non_text_search(self):
        '''При бестекстовом запросе фильтрующие алгоритмы не применяется
        вместо них используется текущий дефолтный алгоритм
        '''
        mn_algo = 'TESTALGO_filter'
        response = self.report.request_json(
            'place=prime&hid=1&debug=1&rids=213&rearr-factors=market_search_mn_algo={}'.format(mn_algo)
        )
        self.assertFragmentNotIn(response, {"rankedWith": mn_algo})
        self.assertFragmentIn(
            response, {"rankedWith": "base___common_851473_0_3__851440_0_7__fashion_859286_0_6__859190_0_4"}
        )

    def test_non_filtered_mn_algo_non_text_search(self):
        '''При бестекстовом запросе нефильтрующие алгоритмы применяется'''
        mn_algorithms = [
            'MNA_CtrClassification',
        ]
        for mn_algo in mn_algorithms:
            response = self.report.request_json(
                'place=prime&hid=1&debug=1&rids=213&rearr-factors=market_textless_search_mn_algo={}'.format(mn_algo)
            )
            self.assertFragmentIn(response, {"rankedWith": mn_algo})

    def test_auction_search_algos(self):
        mn_algo = 'MNA_CtrClassification'
        '''на прайм флаги не влияют'''
        res_prime = self.report.request_json(
            'place=prime&hid=1&debug=1&rids=213&rearr-factors=market_textless_auction_search_mn_algo={}'.format(mn_algo)
        )
        self.assertFragmentNotIn(res_prime, {"rankedWith": mn_algo})
        self.assertFragmentIn(
            res_prime, {"rankedWith": "base___common_851473_0_3__851440_0_7__fashion_859286_0_6__859190_0_4"}
        )

        '''флаги влияют на advertising_carousel и cpa_shop_incut'''
        res_csi = self.report.request_json(
            'place=cpa_shop_incut&hid=1&debug=1&rids=213&rearr-factors=market_textless_auction_search_mn_algo={}'.format(
                mn_algo
            )
        )
        self.assertFragmentIn(res_csi, "FillPrimeFilteringSortingSpec(): Using MatrixNet formula: {}".format(mn_algo))
        res_ac = self.report.request_json(
            'place=advertising_carousel&text=молот&ranking-mode=mn&debug=1&rids=213&rearr-factors=market_text_auction_search_mn_algo={}'.format(
                mn_algo
            )
        )
        self.assertFragmentIn(res_ac, "FillPrimeFilteringSortingSpec(): Using MatrixNet formula: {}".format(mn_algo))

    def test_category_ranking_formula(self):
        '''Для ранжирования категорий используется формула DEFAULT_CATEGORY_RELEVANCE_FORMULA
        Порог проверяется по формуле редиректа с порогом DEFALUT_CATEGORY_REDIRECT_THRESHOLD
        Формулы могут быть разными, но сейчас они одинаковые.
        Под флагом market_category_redirect_reranking=1 (по умолчанию) выбор и проверка порога делается
        по одной формуле редиректа'''

        response = self.report.request_json('place=prime&text=blue+kiy+circle&debug=1&rids=213&cpa=real&cvredirect=1')

        self.assertFragmentIn(
            response,
            {
                "categories_ranking_json": [
                    {
                        'relevanceFormula': 'category_fml_formula_855249',
                        'redirectFormula': 'category_fml_formula_855249',
                    }
                ]
            },
        )

        self.assertFragmentIn(
            response,
            {
                "logicTrace": [
                    Contains(
                        "CheckCategoryRedirection",
                        "Redirect formula category_fml_formula_855249",
                        "threshold:0.5108613372",
                    )
                ]
            },
        )

        response = self.report.request_json(
            'place=prime&text=blue+kiy+circle&debug=1&rids=213&cpa=real&cvredirect=1'
            '&rearr-factors=market_category_redirect_reranking=0'
        )

        self.assertFragmentIn(
            response,
            {
                "categories_ranking_json": [
                    {
                        'relevanceFormula': 'category_fml_formula_855249',
                        'redirectFormula': 'category_fml_formula_855249',
                    }
                ]
            },
        )

        self.assertFragmentIn(
            response,
            {
                "logicTrace": [
                    Contains(
                        "Use relevance formula category_fml_formula_855249 to select redirect category and redirect formula category_fml_formula_855249 to check redirect threshold"
                    ),
                    Contains(
                        "CheckCategoryRedirection",
                        "Redirect formula category_fml_formula_855249",
                        "threshold:0.5108613372",
                    ),
                ]
            },
        )

    def test_mn_algo_pic_dups_count_no_flag(self):
        response = self.report.request_json(
            'place=prime&text=kiyanka&debug=1&rids=213&rearr-factors=market_search_mn_algo=MNA_CtrClassification&numdoc=100'
        )

        self.assertFragmentIn(response, 'Max duplicates count: 1', preserve_order=True)  # default value

        shop_pics = {}

        for el in response.root["search"]["results"]:
            if ("entity" in el) and el["entity"] == "offer":
                shop_id = el["shop"]["id"]
                title = el["titles"]["raw"]
                pic_id = int(title.split('-')[-1])

                shop_pics.setdefault(shop_id, {})
                shop_pics[shop_id].setdefault(pic_id, 0)
                shop_pics[shop_id][pic_id] += 1

        max_dups_count = 0
        for shop_id, pics in shop_pics.items():
            for pic_id, count in pics.items():
                max_dups_count = max(count, max_dups_count)

        self.assertGreaterEqual(max_dups_count, 1)

    def test_mn_algo_pic_dups_count_2(self):
        response = self.report.request_json(
            'place=prime&text=kiyanka&debug=1&rids=213'
            '&rearr-factors=market_search_mn_algo=MNA_CtrClassification;market_max_pics_duplicates_count=2&numdoc=4'
        )

        self.assertFragmentIn(response, 'Max duplicates count: 2', preserve_order=True)

        shop_pics = {}

        for el in response:
            if ("entity" in el) and el["entity"] == "offer":
                shop_id = el["shop"]["id"]
                title = el["titles"]["raw"]
                pic_id = int(title.split('-')[-1])

                shop_pics.setdefault(shop_id, {})
                shop_pics[shop_id].setdefault(pic_id, 0)
                shop_pics[shop_id][pic_id] += 1

        max_dups_count = 0
        for shop_id, pics in shop_pics.items():
            for pic_id, count in pics.items():
                max_dups_count = max(count, max_dups_count)

        self.assertLessEqual(max_dups_count, 2)

    def _test_max_offers_per_shop_count(self, count, request=None):
        if not request:
            request = (
                'place=prime&text=kiyanka&debug=1&rids=213&numdoc=100'
                '&rearr-factors=market_max_offers_per_shop_count={};market_new_cpm_iterator=0'
            ).format(count)

        response = self.report.request_json(request)
        self.assertFragmentIn(response, 'Offers per shop count: {}'.format(count), preserve_order=True)

        for shop_name in SHOP_NAMES:
            self.assertLessEqual(
                response.count(
                    {
                        "entity": "offer",
                        "shop": {"name": shop_name},
                        "debug": {"tech": {"docPriority": Wildcard("Doc*Head")}},
                    }
                ),
                count,
            )

        self._check_using_grouping(response, "dsrcid")

    def test_max_offers_per_shop_count_1(self):
        return self._test_max_offers_per_shop_count(1)

    def test_max_offers_per_shop_count_2(self):
        return self._test_max_offers_per_shop_count(2)

    def test_max_offers_per_shop_count_3(self):
        return self._test_max_offers_per_shop_count(3)

    def test_max_offers_per_shop_count_no_flag(self):
        return self._test_max_offers_per_shop_count(
            999, request='place=prime&text=kiyanka&debug=1&rids=213&rearr-factors=market_new_cpm_iterator=0'
        )  # 999 is default value

    def test_new_cpm_iterator_4(self):
        """market_new_cpm_iterator=4 заменяет TCpmIterator на TRearrangeableIterator
        а также отрывает dsrcid и yg ygg группировки (yg остается только при cvredirect=1)
        """

        # на текстовом поиске без редиректов запрашиваем только hyper_ts в количестве необходимом для переранжирования
        response = self.report.request_json(
            'place=prime&text=cocosanca&rids=213&pp=18&debug=da&allow-collapsing=1'
            '&rearr-factors=market_new_cpm_iterator=4'
            '&rearr-factors=market_metadoc_search=no'
        )
        self.assertFragmentIn(
            response, {"logicTrace": [Contains("rearrangeable.cpp", "Iterate(): Start iterate documents")]}
        )
        self.assertFragmentIn(response, {"g": ["1.hyper_ts.160.1.-1"]}, allow_different_len=False)

        # на бестексте запрашиваем только hyper_ts с 60 документами для обновления статистики
        response = self.report.request_json(
            'place=prime&hid=3&rids=213&pp=18&debug=da&allow-collapsing=1'
            '&rearr-factors=market_new_cpm_iterator=4'
            '&rearr-factors=market_metadoc_search=no'
        )

        # на текстовом поиске с редиректом запрашиваем hyper_ts и yg
        response = self.report.request_json(
            'place=prime&text=cocosanca&rids=213&pp=18&debug=da&allow-collapsing=1'
            '&rearr-factors=market_new_cpm_iterator=4&cvredirect=1'
            '&rearr-factors=market_metadoc_search=no'
        )
        self.assertFragmentIn(response, {"g": ["1.hyper_ts.160.1.-1"]}, allow_different_len=False)

        # на текстовом поиске с редиректом-уточнением запрашиваем только hyper_ts
        response = self.report.request_json(
            'place=prime&text=cocosanca&hid=3&rids=213&pp=18&debug=da&allow-collapsing=1'
            '&rearr-factors=market_new_cpm_iterator=4&cvredirect=3'
            '&rearr-factors=market_metadoc_search=no'
        )
        self.assertFragmentIn(response, {"g": ["1.hyper_ts.160.1.-1"]}, allow_different_len=False)

    def test_mn_algo_max_offers_per_shop_count_MARKETOUT_29780(self):
        return self._test_max_offers_per_shop_count(
            1,
            request='place=prime&text=kiyanka&debug=1&rids=213&rearr-factors=market_search_mn_algo=MNA_CtrClassification;'
            'market_max_offers_per_shop_count=1;market_new_cpm_iterator=0',
        )

    def test_mn_algo_max_offers_per_shop_count_max_pics_duplicates_count_1_MARKETOUT_29780(self):
        response = self.report.request_json(
            'place=prime&text=kiyanka&debug=1&rids=213'
            '&rearr-factors=market_search_mn_algo=MNA_CtrClassification;market_max_offers_per_shop_count=1;'
            'market_max_pics_duplicates_count=1;market_new_cpm_iterator=0'
        )

        # Check offers dups

        self.assertFragmentIn(response, 'Max duplicates count: 1', preserve_order=True)

        shop_pics = {}

        for el in response:
            if ("entity" in el) and el["entity"] == "offer":
                shop_id = el["shop"]["id"]
                title = el["titles"]["raw"]
                pic_id = int(title.split('-')[-1])

                shop_pics.setdefault(shop_id, {})
                shop_pics[shop_id].setdefault(pic_id, 0)
                shop_pics[shop_id][pic_id] += 1

        max_dups_count = 0
        for shop_id, pics in shop_pics.items():
            for pic_id, count in pics.items():
                max_dups_count = max(count, max_dups_count)

        self.assertLessEqual(max_dups_count, 1)

        # Check max offers per shop

        for shop_name in SHOP_NAMES:
            self.assertLessEqual(response.count({"entity": "offer", "shop": {"name": shop_name}}), 1)

    def test_mn_algo_max_offers_per_shop_count_max_pics_duplicates_count_2_MARKETOUT_29780(self):
        response = self.report.request_json(
            'place=prime&text=kiyanka&debug=1&rids=213&rearr-factors=market_search_mn_algo=MNA_CtrClassification;'
            'market_max_offers_per_shop_count=3;market_max_pics_duplicates_count=2;market_new_cpm_iterator=0'
        )

        # Check offers dups

        self.assertFragmentIn(response, 'Max duplicates count: 2', preserve_order=True)

        shop_pics = {}

        for el in response:
            if ("entity" in el) and el["entity"] == "offer":
                shop_id = el["shop"]["id"]
                title = el["titles"]["raw"]
                pic_id = int(title.split('-')[-1])

                shop_pics.setdefault(shop_id, {})
                shop_pics[shop_id].setdefault(pic_id, 0)
                shop_pics[shop_id][pic_id] += 1

        max_dups_count = 0
        for shop_id, pics in shop_pics.items():
            for pic_id, count in pics.items():
                max_dups_count = max(count, max_dups_count)

        self.assertLessEqual(max_dups_count, 2)

        # Check max offers per shop

        for shop_name in SHOP_NAMES:
            self.assertLessEqual(response.count({"entity": "offer", "shop": {"name": shop_name}}), 3)

        self._check_using_grouping(response, "dsrcid")

    def test_parallel_formula(self):
        response = self.report.request_bs(
            'place=parallel&text=kiyanka&debug=1&rids=213&rearr-factors=market_search_mn_algo=MNA_CtrClassification'
        )

        self.assertFragmentIn(response, 'Using MatrixNet formula for search ranking: MNA_CtrClassification')

    def test_parallel_formula2(self):
        response = self.report.request_bs(
            'place=parallel&text=kiyanka&debug=1&rids=213&rearr-factors=market_search_mn_algo=MNA_ParallelFuzzySearchAssessorRanker'
        )

        self.assertFragmentIn(
            response, 'Using MatrixNet formula for search ranking: MNA_ParallelFuzzySearchAssessorRanker'
        )

    def test_no_disable_offers_per_shop_limit_MARKETOUT_29780(self):
        response = self.report.request_json(
            'place=prime&text=MARKETOUT-8860&debug=1&rids=213&rearr-factors=market_new_cpm_iterator=0;market_max_offers_per_shop_count=5'
        )

        self.assertEqual(response.count({"entity": "offer", "shop": {"name": SHOP_NAMES[14]}}), 10)
        self.assertEqual(response.count({"docPriority": "DocRangeHead"}), 5)  # 5 - default value
        self._check_using_grouping(response, "dsrcid")
        self._check_using_grouping(response, "ts")

    @classmethod
    def prepare_guru_formula_sorting(cls):
        """Подготовка данных для проверки работы сортировки по формуле на безтектсовых запросах в гуру категориях,
        https://st.yandex-team.ru/MARKETOUT-16843
        https://st.yandex-team.ru/MARKETOUT-18949
        """
        # готовим гуру категорию и две модели в ней.
        # Одна модель с большим количеством кликов (популярная),
        # но с меньшим значением matrix net
        # Вторая с маленьким количеством кликов (непопулярная),
        # но с бОльшим значением matrix net
        cls.index.hypertree += [HyperCategory(hid=100, output_type=HyperCategoryType.GURU)]
        cls.index.models += [
            Model(hid=100, hyperid=110, ts=120, title='PopularModel', model_clicks=1000),
            Model(hid=100, hyperid=111, ts=121, title='UnpopularModel', model_clicks=100),
        ]
        cls.index.offers += [
            Offer(hyperid=110),
            Offer(hyperid=111),
        ]
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 120).respond(0.1)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 121).respond(0.2)

    def test_guru_formula_sorting(self):
        """Проверка работы сортировки по формуле на безтектсовых запросах в гуру категориях,
        и обратного флага, заменяющего сортировку по формуле на сортировку по популярности
        https://st.yandex-team.ru/MARKETOUT-16843
        https://st.yandex-team.ru/MARKETOUT-18949
        """
        # задаем запрос в категорию, проверям, что сортировка по формуле,
        # т.е. первой идет модель с бОльшим значением matrix net
        response = self.report.request_json('place=prime&hid=100')

        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"entity": "product", "id": 111},
                    {"entity": "product", "id": 110},
                ]
            },
            preserve_order=True,
        )

    @classmethod
    def prepare_not_filling_first_click_factors(cls):
        cls.index.offers += [
            Offer(
                title='Настенный пылесос',
                waremd5='ZRK9Q9nKpuAsmQsKgmUtyg',
                fesh=1,
                factor_streams=[
                    Stream(name=StreamName.FIRST_CLICK_DT_XF, region=225, annotation='настенный пылесос', weight=0.64)
                ],
            )
        ]
        cls.settings.use_factorann = True
        cls.index.shops += [Shop(fesh=1, priority_region=213)]

    def test_not_filling_first_click_factors(self):
        """Проверка зануления FIRST_CLICK_DT_XF факторов под флагом
        https://st.yandex-team.ru/MARKETOUT-25815
        """
        # Базовый поиск
        request = 'place=prime&text=Настенный+пылесос&debug=da&rids=213'
        flag = '&rearr-factors=market_disable_first_click_dt_xf=1;market_relevance_formula_threshold=0'

        response = self.report.request_json(request)
        self.feature_log.expect(
            first_click_dt_xf_all_wcm_match95_avg_value=NotEmpty(), ware_md5='ZRK9Q9nKpuAsmQsKgmUtyg'
        )

        response = self.report.request_json(request + flag)
        self.feature_log.expect(first_click_dt_xf_all_wcm_match95_avg_value=Absent(), ware_md5='ZRK9Q9nKpuAsmQsKgmUtyg')

        # Параллельный поиск
        request = 'place=parallel&rids=213&text=Настенный+пылесос&rearr-factors=market_calc_text_machine_factors=1'
        flag = ';market_disable_first_click_dt_xf=1;market_relevance_formula_threshold=0'

        response = self.report.request_bs(request)
        self.assertFragmentIn(
            response, {"market_factors": [{"base.first_click_dt_xf_all_wcm_match95_avg_value_avg": NotEmpty()}]}
        )

        response = self.report.request_bs(request + flag)
        self.assertFragmentIn(
            response, {"market_factors": [{"base.first_click_dt_xf_all_wcm_match95_avg_value_avg": Absent()}]}
        )

    @classmethod
    def prepare_goods_parallel_exp_tm_factors(cls):
        cls.index.offers += [
            Offer(
                title='гречка с сахаром',
                waremd5='eSkHEoEfSqZccMq_Jzspbw',
                fesh=1,
                factor_streams=[
                    Stream(name=StreamName.NHOP, region=225, annotation='гречка с сахаром', weight=1.0),
                    Stream(name=StreamName.SIMPLE_CLICK, region=225, annotation='гречка с сахаром', weight=1.0),
                ],
            )
        ]
        cls.settings.use_factorann = True
        cls.index.shops += [Shop(fesh=1, priority_region=213)]

    def test_goods_parallel_exp_tm_factors(self):
        """Проверка зануления фичей потенциально нежгущих стримов в параллельном
        https://st.yandex-team.ru/ECOMQUALITY-328
        """
        # Базовый поиск
        request = 'place=prime&text=гречка+с+сахаром&debug=da&rids=213'
        flag = '&rearr-factors=goods_use_best_text_machine_streams_on_parallel=1;market_relevance_formula_threshold=0'

        response = self.report.request_json(request)
        self.feature_log.expect(nhop_is_final_atten_v1_bm15_k001=NotEmpty(), ware_md5='eSkHEoEfSqZccMq_Jzspbw')

        response = self.report.request_json(request + flag)
        self.feature_log.expect(nhop_is_final_atten_v1_bm15_k001=NotEmpty(), ware_md5='eSkHEoEfSqZccMq_Jzspbw')

        # Параллельный поиск
        request = 'place=parallel&rids=213&text=гречка+с+сахаром&rearr-factors=market_calc_text_machine_factors=1'
        flag = ';goods_use_best_text_machine_streams_on_parallel=1;market_relevance_formula_threshold=0'

        response = self.report.request_bs(request)
        self.assertFragmentIn(response, {"market_factors": [{"base.nhop_is_final_atten_v1_bm15_k001_avg": NotEmpty()}]})

        response = self.report.request_bs(request + flag)
        self.assertFragmentIn(response, {"market_factors": [{"base.nhop_is_final_atten_v1_bm15_k001_avg": Absent()}]})
        self.assertFragmentIn(
            response, {"market_factors": [{"base.qfuf_all_max_wf_simple_click_bclm_mix_plain_k1e_05_avg": NotEmpty()}]}
        )

    @classmethod
    def prepare_not_filling_redundant_tm_factors(cls):
        cls.index.offers += [
            Offer(
                title='портативный гараж',
                waremd5='VZ5_m1o38hVbuXevO9zLQQ',
                fesh=1,
                factor_streams=[Stream(name=StreamName.NHOP, region=225, annotation='портативный гараж', weight=0.64)],
            )
        ]
        cls.settings.use_factorann = True
        cls.index.shops += [Shop(fesh=1, priority_region=213)]

    def test_not_filling_redundant_tm_factors(self):
        """Проверка зануления потенциально нежгущих факторов текстовой машины
        https://st.yandex-team.ru/ECOMQUALITY-13
        """
        # Базовый поиск
        request = 'place=prime&text=портативный+гараж&debug=da&rids=213'
        flag = '&rearr-factors=market_disable_redundant_tm_factors=1;market_relevance_formula_threshold=0'

        response = self.report.request_json(request)
        self.feature_log.expect(nhop_is_final_atten_v1_bm15_k001=NotEmpty(), ware_md5='VZ5_m1o38hVbuXevO9zLQQ')

        response = self.report.request_json(request + flag)
        self.feature_log.expect(nhop_is_final_atten_v1_bm15_k001=Absent(), ware_md5='VZ5_m1o38hVbuXevO9zLQQ')

        # Параллельный поиск
        request = 'place=parallel&rids=213&text=портативный+гараж&rearr-factors=market_calc_text_machine_factors=1'
        flag = ';market_disable_redundant_tm_factors=1;market_relevance_formula_threshold=0'

        response = self.report.request_bs(request)
        self.assertFragmentIn(response, {"market_factors": [{"base.nhop_is_final_atten_v1_bm15_k001_avg": NotEmpty()}]})

        response = self.report.request_bs(request + flag)
        self.assertFragmentIn(response, {"market_factors": [{"base.nhop_is_final_atten_v1_bm15_k001_avg": Absent()}]})

    def test_mn_algo_cpa_real(self):
        # проверяем переопределение формул для запросов с cpa=real
        request = 'place=prime&text=blue+kiy&debug=1&rids=213'
        cpa_real = '&cpa=real'
        flag = '&rearr-factors=market_search_mn_algo=MNA_Trivial'
        response = self.report.request_json(request + cpa_real + flag)
        self.assertFragmentIn(response, {"rankedWith": "MNA_Trivial"})

        # флаг переопределит формулу для запроса с cpa=real
        flag = '&rearr-factors=market_search_mn_algo=MNA_Trivial;market_cpa_search_mn_algo=MNA_Relevance'
        response = self.report.request_json(request + flag)
        self.assertFragmentIn(response, {"rankedWith": "MNA_Trivial"})
        self.assertFragmentNotIn(response, {"rankedWith": "MNA_Relevance"})

        response = self.report.request_json(request + cpa_real + flag)
        self.assertFragmentIn(response, {"rankedWith": "MNA_Relevance"})
        self.assertFragmentNotIn(response, {"rankedWith": "MNA_Trivial"})

    def test_mn_algo_cpa_real_textless(self):
        # проверяем переопределение формул для бестекстовых запросов с cpa=real
        request = 'place=prime&hid=1&debug=1&rids=213'
        cpa_real = '&cpa=real'
        flag = '&rearr-factors=market_textless_search_mn_algo=MNA_Trivial'
        response = self.report.request_json(request + cpa_real + flag)
        self.assertFragmentIn(response, {"rankedWith": "MNA_Trivial"})

        # флаг переопределит формулу для запроса с cpa=real
        flag = (
            '&rearr-factors=market_textless_search_mn_algo=MNA_Trivial;market_cpa_textless_search_mn_algo=MNA_Relevance'
        )
        response = self.report.request_json(request + flag)
        self.assertFragmentIn(response, {"rankedWith": "MNA_Trivial"})
        self.assertFragmentNotIn(response, {"rankedWith": "MNA_Relevance"})

        response = self.report.request_json(request + cpa_real + flag)
        self.assertFragmentIn(response, {"rankedWith": "MNA_Relevance"})
        self.assertFragmentNotIn(response, {"rankedWith": "MNA_Trivial"})

    def test_dj_factor_request(self):
        """Проверяем, что под флагом на тексте и бестексе происходит запрос в DJ
        (для расчета и логирования на стороне DJ
        """
        # проверяем, что есть поход в диджей на тексте:
        response = self.report.request_json(
            'place=prime&text=kiyanka&debug=1&rids=213&yandexuid=258907&debug=da&rearr-factors=market_dj_prime_request_from_meta=1'
        )
        self.assertFragmentIn(response, {'logicTrace': [Contains('Send factor request to dj')]})

        # проверяем, что он есть на бестексте:
        response = self.report.request_json(
            'place=prime&hid=1&debug=1&rids=213&pp=18&yandexuid=258907&debug=da&rearr-factors=market_dj_prime_request_from_meta=1'
        )
        self.assertFragmentIn(response, {'logicTrace': [Contains('Send factor request to dj')]})

        # проверяем, что он есть на бестексте (auction, pp=7):
        response = self.report.request_json(
            'place=prime&hid=1&debug=1&rids=213&pp=7&yandexuid=258907&debug=da&rearr-factors=market_dj_prime_request_from_meta=1'
        )
        self.assertFragmentIn(response, {'logicTrace': [Contains('Send factor request to dj')]})

        # проверяем, что нет на параллельном:
        response = self.report.request_bs(
            'place=parallel&text=kiyanka&debug=1&rids=213&debug=da&rearr-factors=market_dj_prime_request_from_meta=1'
        )
        self.assertFragmentNotIn(response, {'logicTrace': [Contains('Send factor request to dj')]})


if __name__ == '__main__':
    main()
