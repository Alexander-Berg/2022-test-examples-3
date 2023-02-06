#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa


from core.matcher import Regex, NotEmptyList, Absent, NotEmpty, Contains, EqualToOneOf
from core.types import (
    BlueOffer,
    Currency,
    HyperCategory,
    MarketSku,
    MnPlace,
    Model,
    NewShopRating,
    Offer,
    Picture,
    Shop,
    Tax,
)
from core.testcase import TestCase, main
from core.types.model import UngroupedModel
from core.types.picture import thumbnails_config


NO_MODEL_OFFERS_COUNT = 10


def ts_for_no_model_offer(i):
    return 300 + i


def title_for_no_model_offer(i):
    # название не менять!
    return 'not ungrouped no model white offer {}'.format(i)


NO_MODEL_OFFERS = [
    Offer(ts=ts_for_no_model_offer(i), title=title_for_no_model_offer(i), fesh=3, hid=3)
    for i in range(0, NO_MODEL_OFFERS_COUNT)
]


class T(TestCase):
    def test_group_attribute(self):
        """
        Проверяем используемый для расхлапывания группировочный атрибут
        """
        request = (
            'place=prime&text=ungrouped&debug=da&allow-collapsing=1'
            '&allow-ungrouping=1&allow-collapsing=1&rearr-factors=market_metadoc_search=no'
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(response, {'g': ['1._virtual96.100.1.-1']})
        self.assertFragmentNotIn(response, {'g': ['1._virtual97.100.1.-1']})
        response = self.report.request_json(request + '&rearr-factors=generate_kadavers=1')
        self.assertFragmentIn(response, {'g': ['1._virtual97.100.1.-1']})

    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ['market_write_category_redirect_features=20']
        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']

    @classmethod
    def prepare_ungrouped_output(cls):
        cls.settings.default_search_experiment_flags += ['market_new_cpm_iterator=0']

        pic = Picture(
            picture_id="KdwwrYb4czANgt9-3poEQQ",
            width=500,
            height=600,
            thumb_mask=thumbnails_config.get_mask_by_names(['1x1', '100x100']),
            group_id=1234,
        )
        pic2 = Picture(
            picture_id="KdwwrYb4caANgt9-3poEQQ",
            width=400,
            height=700,
            thumb_mask=thumbnails_config.get_mask_by_names(['1x1', '100x100']),
            group_id=1234,
        )
        pic3 = Picture(
            picture_id="KdwwrYb4caANgt9-3poEQQ",
            width=500,
            height=700,
            thumb_mask=thumbnails_config.get_mask_by_names(['1x1', '100x100']),
            group_id=1234,
        )
        pic4 = Picture(
            picture_id="KdwwrYb4caANgt9-3poEQQ",
            width=700,
            height=600,
            thumb_mask=thumbnails_config.get_mask_by_names(['1x1', '100x100']),
            group_id=1234,
        )

        cls.index.shops += [
            Shop(
                fesh=1,
                datafeed_id=1,
                priority_region=213,
                name='virtual_shop',
                currency=Currency.RUR,
                tax_system=Tax.OSN,
                fulfillment_virtual=True,
                virtual_shop_color=Shop.VIRTUAL_SHOP_BLUE,
                cpa=Shop.CPA_REAL,
            ),
            Shop(
                fesh=2,
                datafeed_id=2,
                priority_region=213,
                name='supplier_shop',
                currency=Currency.RUR,
                tax_system=Tax.OSN,
                blue='REAL',
                supplier_type=Shop.FIRST_PARTY,
            ),
            Shop(
                fesh=3,
                priority_region=213,
            ),
        ]

        cls.index.models += [
            Model(
                hyperid=1,
                hid=1,
                title="Исходная модель 1",
                ungrouped_blue=[
                    UngroupedModel(
                        group_id=1,
                        title="Расхлопнутая модель 1.1",
                        key='1_1',
                    ),
                    UngroupedModel(
                        group_id=2,
                        title="Расхлопнутая модель 1.2",
                        key='1_2',
                    ),
                ],
            ),
            Model(
                hyperid=2,
                hid=1,
                title="Исходная модель без ску 2",
            ),
            Model(
                hyperid=3,
                hid=1,
                title="not ungrouped исходная модель без офферов 3",
                ts=1,
            ),
            Model(
                hyperid=4,
                hid=1,
                title="Исходная модель с белыми офферами 4",
            ),
            Model(
                hyperid=5,
                hid=1,
                title="not ungrouped Исходная модель с ску 5",
                ts=2,
            ),
        ]

        cls.index.mskus += [
            MarketSku(
                fesh=1,
                title='ungrouped 1.1 cheap',
                hyperid=1,
                sku=1,
                blue_offers=[
                    BlueOffer(price=5, feedid=2, ts=3, waremd5='Ws3Jyl2Zrmav3-HuoOOyaw'),
                ],
                ungrouped_model_blue=1,
                picture=pic,
            ),
            MarketSku(
                fesh=1,
                title='ungrouped 1.1 relevant & white only',
                hyperid=1,
                sku=2,
                blue_offers=[],
                ungrouped_model_blue=1,
                picture=pic,
            ),
            MarketSku(
                fesh=1,
                title='ungrouped 1.2',
                hyperid=1,
                sku=3,
                blue_offers=[
                    BlueOffer(price=15, feedid=2, ts=8, waremd5='tbqpwjrD4BWtKHsXo9Svow'),
                ],
                ungrouped_model_blue=2,
                picture=pic2,
            ),
            MarketSku(
                fesh=1,
                title='not ungrouped 5.1',
                hyperid=5,
                sku=4,
                blue_offers=[
                    BlueOffer(price=15, feedid=2, ts=9, waremd5='1aKPgYNLbfK2220-XiY1xw'),
                ],
                picture=pic3,
            ),
            MarketSku(
                fesh=1,
                title='not ungrouped 5.2',
                hyperid=5,
                sku=5,
                blue_offers=[
                    BlueOffer(price=15, feedid=2, ts=6, waremd5='iWX3ZjLXZy59PPKch-yqDA'),
                ],
                picture=pic4,
            ),
        ]

        cls.index.offers += [
            Offer(
                hyperid=1,
                ts=5,
                sku=2,
                ungrouped_model_blue=1,
                title='ungrouped white offer',
                fesh=3,
                waremd5='eGfWVXuC7TtdYeZZGiov0w',
            ),
            Offer(
                hyperid=1,
                ts=10,
                sku=2,
                ungrouped_model_blue=1,
                title='ungrouped white offer 2',
                fesh=3,
                waremd5='KXGI8T3GP_pqjgdd7HfoHQ',
            ),
            Offer(
                hyperid=1,
                ts=10,
                ungrouped_model_blue=1,
                title='ungrouped white offer non sku 3',
                fesh=3,
                waremd5='yRgmzyBD4j8r4rkCby6Iuw',
            ),
            Offer(
                hyperid=4, ts=4, title='not ungrouped white offer with model', waremd5='3msc8_sKOqEkTTD3uMKcQQ', fesh=3
            ),  # no sku offer
        ] + NO_MODEL_OFFERS

        for place in [MnPlace.BASE_SEARCH, MnPlace.META_REARRANGE]:
            cls.matrixnet.on_place(place, 1).respond(0.9)
            cls.matrixnet.on_place(place, 2).respond(0.8)
            cls.matrixnet.on_place(place, 5).respond(0.7)
            cls.matrixnet.on_place(place, 10).respond(0.65)
            cls.matrixnet.on_place(place, 3).respond(0.6)
            cls.matrixnet.on_place(place, 4).respond(0.5)
            cls.matrixnet.on_place(place, 6).respond(0.4)
            cls.matrixnet.on_place(place, 8).respond(0.3)
            cls.matrixnet.on_place(place, 20).respond(0.15)
            cls.matrixnet.on_place(place, 9).respond(0.1)
            for i in range(0, NO_MODEL_OFFERS_COUNT):
                cls.matrixnet.on_place(place, ts_for_no_model_offer(i)).respond(0.2 - i * 0.001)

        cls.index.hypertree += [HyperCategory(hid=hid) for hid in range(1, 10)]

    def test_ungrouped_output(self):
        """
        Проверяем, что на выдаче расхлопываются скушные модели
        """

        def check_results_list(response, *ids):
            return self.assertFragmentIn(
                response,
                {
                    'results': [
                        {
                            'entity': 'product' if isinstance(id_, int) else 'offer',
                            'id': id_ if isinstance(id_, int) else Absent(),
                            'titles': NotEmpty() if isinstance(id_, int) else {'raw': id_},
                        }
                        for id_ in ids
                    ]
                },
                allow_different_len=False,
                preserve_order=True,
            )

        request_no_clps = (
            'place=prime&text=ungrouped&debug=da&'
            'platform=touch&use-default-offers=1&numdoc=48&rearr-factors=market_metadoc_search=no'
        )
        request = request_no_clps + '&allow-collapsing=1'
        flag = '&rearr-factors=market_white_ungrouping=1'
        param = '&allow-ungrouping=1'

        no_model_offers_titles = [title_for_no_model_offer(i) for i in range(0, NO_MODEL_OFFERS_COUNT)]
        for cgi in (flag, param):
            response = self.report.request_json(request)
            check_results_list(response, 3, 5, 1, 4, *no_model_offers_titles)

            for f in ('', cgi):
                response = self.report.request_json(request_no_clps + f)
                args = [3, 5] + [Regex(r'\w+')] * 17
                check_results_list(response, *args)

            response = self.report.request_json(request + cgi)

            # Модель 3, без офферов
            # Модель 5, ску 5 "not ungrouped 5.2", 2 оффера
            # Модель 1, ску 2, "ungrouped 1.1 relevant & white only", 5 офферов
            # Модель 4, ску 0, "not ungrouped white offer with model", 1 оффер
            # Модель 1, ску 3, "ungrouped 1.2", 5 офферов
            # Белый оффер 'not ungrouped no model white offer 0'
            check_results_list(response, 3, 5, 1, 4, 1, *no_model_offers_titles)
            self.assertFragmentIn(
                response,
                {
                    'logicTrace': [
                        Contains(
                            'DocRangeNonLocalTail market.yandex.ru/product/3 g:_virtual96', 'ModeId:3 WareMd5:None'
                        )
                    ]
                },
            )

            response = self.report.request_json(request + cgi + '&onstock=1')
            check_results_list(response, 5, 1, 4, 1, *no_model_offers_titles)

            response = self.report.request_json(request + cgi)
            self.assertFragmentIn(
                response,
                {
                    'results': [
                        {
                            'entity': 'product',
                            'id': 3,
                        },
                        {
                            'entity': 'product',
                            'id': 5,
                        },
                        {
                            'entity': 'product',
                            'id': 1,
                            'skuOffersCount': 2,
                            'offers': {
                                'items': [
                                    {
                                        'marketSku': '2',
                                        'wareId': 'eGfWVXuC7TtdYeZZGiov0w',
                                        'skuAwareTitles': {'raw': 'ungrouped 1.1 relevant & white only'},
                                        'skuAwarePictures': [
                                            {
                                                'original': {'width': 500},
                                            },
                                        ],
                                    }
                                ],
                            },
                        },
                        {
                            'entity': 'product',
                            'id': 4,
                        },
                        {
                            'entity': 'product',
                            'id': 1,
                            'skuOffersCount': 1,
                            'offers': {
                                'items': [
                                    {
                                        'marketSku': '3',
                                        'wareId': 'tbqpwjrD4BWtKHsXo9Svow',
                                        'skuAwareTitles': {'raw': 'ungrouped 1.2'},
                                        'skuAwarePictures': [
                                            {
                                                'original': {'width': 400},
                                            },
                                        ],
                                    }
                                ],
                            },
                        },
                    ]
                    + [
                        {
                            'entity': 'offer',
                            'titles': {'raw': title},
                        }
                        for title in no_model_offers_titles
                    ]
                },
                allow_different_len=False,
                preserve_order=True,
            )

    @classmethod
    def prepare_ungrouped_skus_with_same_key(cls):
        # Модель 11 с двумя ключами - 1.1 и 1.2
        # Модель 12 с двумя ключами - 1.1 и 1.2
        #
        # MSKU 11 c синим оффером, ungrouped_model_blue = 1
        # MSKU 12 c синим оффером, ungrouped_model_blue = 2
        # MSKU 13 c синим оффером, ungrouped_model_blue = 1
        # MSKU 13 c синим оффером, ungrouped_model_blue = 2

        cls.index.models += [
            Model(
                hyperid=11,
                hid=1,
                title="Модель 11",
                ungrouped_blue=[
                    UngroupedModel(
                        group_id=1,
                        title="Расхлопнутая модель 1.1",
                        key='1_1',
                    ),
                    UngroupedModel(
                        group_id=2,
                        title="Расхлопнутая модель 1.2",
                        key='1_2',
                    ),
                ],
            ),
            Model(
                hyperid=12,
                hid=1,
                title="Модель 12",
                ungrouped_blue=[
                    UngroupedModel(
                        group_id=1,
                        title="Расхлопнутая модель 1.1",
                        key='1_1',
                    ),
                    UngroupedModel(
                        group_id=2,
                        title="Расхлопнутая модель 1.2",
                        key='1_2',
                    ),
                ],
            ),
        ]

        cls.index.mskus += [
            MarketSku(
                fesh=1,
                title='duplicate 1.1 - part 1',
                hyperid=11,
                sku=11,
                blue_offers=[
                    BlueOffer(price=5, feedid=2, ts=3, waremd5='Ws3Jyl2Zrmav3-HuoO_yaw'),
                ],
                ungrouped_model_blue=1,
            ),
            MarketSku(
                fesh=1,
                title='duplicate 1.1 - part 2',
                hyperid=11,
                sku=12,
                blue_offers=[
                    BlueOffer(price=10, feedid=2, ts=4, waremd5='Uhgfml2Zrmav3-HuoO_yaw'),
                ],
                ungrouped_model_blue=2,
            ),
            MarketSku(
                fesh=1,
                title='duplicate 1.2 - part 1',
                hyperid=12,
                sku=13,
                blue_offers=[
                    BlueOffer(price=15, feedid=2, ts=8, waremd5='tbqpwjrD4BWtKEsXo9Svow'),
                ],
                ungrouped_model_blue=1,
            ),
            MarketSku(
                fesh=1,
                title='duplicate 1.2 - part 2',
                hyperid=12,
                sku=14,
                blue_offers=[
                    BlueOffer(price=25, feedid=2, ts=18, waremd5='TBQpwjrD4BWtKEsXo9Svow'),
                ],
                ungrouped_model_blue=2,
            ),
        ]

    def test_ungrouped_skus_with_same_key(self):
        """
        Проверяем, что на выдаче при расхлопывании не теряются оффера из-за совпадения ключа UngroupedModelKeyBlue
        """

        request = (
            'place=prime&text=duplicate&debug=1&'
            'platform=touch&use-default-offers=1'
            '&allow-collapsing=1&use_kadavers=1'
            '&allow-ungrouping=1'
            '&rearr-factors=market_metadoc_search=no'
        )

        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        "entity": "product",
                        "offers": {
                            "items": [
                                {
                                    "titles": {"raw": "duplicate 1.1 - part 1"},
                                    "offerColor": "blue",
                                    "prices": {
                                        "rawValue": "5",
                                    },
                                }
                            ]
                        },
                    },
                    {
                        "entity": "product",
                        "offers": {
                            "items": [
                                {
                                    "titles": {"raw": "duplicate 1.1 - part 2"},
                                    "offerColor": "blue",
                                    "prices": {
                                        "rawValue": "10",
                                    },
                                }
                            ]
                        },
                    },
                    {
                        "entity": "product",
                        "offers": {
                            "items": [
                                {
                                    "titles": {"raw": "duplicate 1.2 - part 1"},
                                    "offerColor": "blue",
                                    "prices": {
                                        "rawValue": "15",
                                    },
                                }
                            ]
                        },
                    },
                    {
                        "entity": "product",
                        "offers": {
                            "items": [
                                {
                                    "titles": {"raw": "duplicate 1.2 - part 2"},
                                    "offerColor": "blue",
                                    "prices": {
                                        "rawValue": "25",
                                    },
                                }
                            ]
                        },
                    },
                ]
            },
            allow_different_len=False,
            preserve_order=False,
        )

    @classmethod
    def prepare_ungrouped_output_with_kadavers(cls):
        cls.index.models += [
            Model(
                hyperid=7,
                hid=3,
                title='table IKEA',
                ts=100,
            ),
            Model(
                hyperid=8,
                hid=4,
                title='chair',
                ts=104,
            ),
            Model(
                hyperid=9,
                hid=3,
                title='table HOFF',
                ts=107,
            ),
        ]

        cls.index.mskus += [
            MarketSku(
                title='black table IKEA',
                hyperid=7,
                sku=71,
            ),
            MarketSku(
                title='black chair',
                hyperid=8,
                sku=81,
            ),
            MarketSku(
                title='white table HOFF',
                hyperid=9,
                sku=91,
            ),
        ]

        cls.index.offers += [
            Offer(
                hyperid=7,
                ts=101,
                sku=71,
                fesh=1,
                title='black table IKEA offer',
                waremd5='OFFER_1_WITH_SKUZZGiov',
                price=100,
            ),
            Offer(
                hyperid=7,
                ts=102,
                fesh=2,
                title='table IKEA without sku 1',
                waremd5='OFFER_1_WITHOUT_SKUiov',
                price=200,
            ),
            Offer(
                hyperid=7, ts=103, fesh=3, title='table IKEA without sku 2', waremd5='OFFER_2_WITHOUT_SKUiov', price=300
            ),
            Offer(
                hyperid=8,
                ts=105,
                sku=81,
                fesh=1,
                title='black chair offer from shop 1',
                waremd5='OFFER_2_WITH_SKUZZGiov',
                price=300,
            ),
            Offer(
                hyperid=8,
                ts=106,
                sku=81,
                fesh=2,
                title='black chair offer from shop 2',
                waremd5='OFFER_3_WITH_SKUZZGiov',
                price=400,
            ),
            Offer(
                hyperid=9,
                ts=108,
                sku=91,
                fesh=1,
                title='white table HOFF offer',
                waremd5='OFFER_4_WITH_SKUZZGiov',
                price=500,
            ),
            Offer(
                hyperid=9,
                ts=109,
                fesh=2,
                title='table HOFF without sku 1',
                waremd5='OFFER_3_WITHOUT_SKUiov',
                price=600,
            ),
        ]

    def test_ungrouped_output_with_kadavers(self):
        """
        Проверяем, что в случае наличия офферов без скю на выдаче генерятся кадавры
        """

        def gen_requests(hid, generate_kadavers):
            return (
                'place=prime&debug=da&hid={}&numdoc=48&'
                'platform=touch&use-default-offers=1&allow-collapsing=1'
                '&rearr-factors=market_white_ungrouping=1;'
                'generate_kadavers={}'
                '&rearr-factors=market_metadoc_search=no'
            ).format(hid, generate_kadavers)

        # ===== столы =====
        # без generate_kadavers
        response = self.report.request_json(gen_requests(hid=3, generate_kadavers=0))
        self.assertFragmentNotIn(response, {'g': ['1._virtual97.100.1.-1']})
        ikea_table_prices = {'min': '100', 'max': '300'}
        hoff_table_prices = {'min': '500', 'max': '600'}
        no_model_offers = [
            {
                'entity': 'offer',
                'titles': {'raw': title_for_no_model_offer(i)},
            }
            for i in range(0, NO_MODEL_OFFERS_COUNT)
        ]
        self.assertFragmentIn(
            response,
            [
                {
                    'entity': 'product',
                    'id': 7,
                    'titles': {'raw': 'table IKEA'},
                    'offers': {'count': 3},
                    'prices': ikea_table_prices,
                },
                {
                    'entity': 'product',
                    'id': 9,
                    'titles': {'raw': 'table HOFF'},
                    'offers': {'count': 2},
                    'prices': hoff_table_prices,
                },
            ]
            + no_model_offers,
            allow_different_len=False,
        )

        # столы c generate_kadavers
        # У стола есть оффер с скю, и два оффера без скю. Поэтому будет показан кадавр
        response = self.report.request_json(gen_requests(hid=3, generate_kadavers=1))
        self.assertFragmentIn(response, {'g': ['1._virtual97.100.1.-1']})
        self.assertFragmentIn(
            response,
            [
                {
                    'entity': 'product',
                    'id': 7,
                    'titles': {'raw': 'table IKEA'},
                    'offers': {
                        'count': 3,  # тут количество офферов у всей модели
                        'items': [
                            {
                                'sku': '71',
                                'marketSku': '71',
                                'skuKadaver': False,
                                'wareId': 'OFFER_1_WITH_SKUZZGiog',  # Почему *iog, а не *iov?!!!
                            }
                        ],
                    },
                    'prices': ikea_table_prices,
                    'skuOffersCount': 1,
                    'skuPrices': {'min': '100', 'max': '100'},
                },
                {
                    'entity': 'product',
                    'id': 7,
                    'titles': {'raw': 'table IKEA'},
                    'offers': {
                        'count': 3,
                        'items': [
                            {
                                'sku': Absent(),
                                'marketSku': Absent(),
                                'skuKadaver': True,
                                'wareId': Regex('OFFER_._WITHOUT_SKUiog'),  # и тут iog
                            }
                        ],
                    },
                    'prices': ikea_table_prices,
                    'skuOffersCount': 2,
                    'skuPrices': {'min': '200', 'max': '300'},
                },
                {
                    'entity': 'product',
                    'id': 9,
                    'titles': {'raw': 'table HOFF'},
                    'offers': {
                        'count': 2,
                        'items': [
                            {'sku': '91', 'marketSku': '91', 'skuKadaver': False, 'wareId': 'OFFER_4_WITH_SKUZZGiog'}
                        ],
                    },
                    'prices': hoff_table_prices,
                    'skuOffersCount': 1,
                    'skuPrices': {'min': '500', 'max': '500'},
                },
                {
                    'entity': 'product',
                    'id': 9,
                    'titles': {'raw': 'table HOFF'},
                    'offers': {
                        'count': 2,
                        'items': [
                            {
                                'sku': Absent(),
                                'marketSku': Absent(),
                                'skuKadaver': True,
                                'wareId': 'OFFER_3_WITHOUT_SKUiog',
                            }
                        ],
                    },
                    'prices': hoff_table_prices,
                    'skuOffersCount': 1,
                    'skuPrices': {'min': '600', 'max': '600'},
                },
            ]
            + no_model_offers,
            allow_different_len=False,
        )

        # ===== стулья =====
        # У стула нет офферов без скю, на выдаче только один продукт
        for generate_kadavers in (0, 1):
            response = self.report.request_json(gen_requests(hid=4, generate_kadavers=generate_kadavers))
            if generate_kadavers:
                self.assertFragmentIn(response, {'g': ['1._virtual97.100.1.-1']})
            else:
                self.assertFragmentNotIn(response, {'g': ['1._virtual97.100.1.-1']})
            self.assertFragmentIn(
                response,
                [
                    {
                        'entity': 'product',
                        'id': 8,
                        'titles': {'raw': 'chair'},
                        'offers': {
                            'count': 2,
                        },
                    },
                ],
                allow_different_len=False,
            )

    @classmethod
    def prepare_default_offers(cls):
        cls.index.shops += [
            Shop(  # плохой магазин с дешевым товарами
                fesh=4,
                priority_region=213,
                new_shop_rating=NewShopRating(new_rating_total=2.0),
            ),
            Shop(  # дорогой магазин с высоким рейтингом
                fesh=5,
                priority_region=213,
                new_shop_rating=NewShopRating(new_rating_total=5.0, rec_and_nonrec_pub_count=20000),
            ),
            Shop(  # магазин с самими релевантными названиями и средним рейтингом
                fesh=6,
                priority_region=213,
                new_shop_rating=NewShopRating(new_rating_total=4.0, rec_and_nonrec_pub_count=5000),
            ),
        ]

        cls.index.models += [
            Model(
                hyperid=10,
                hid=5,
                ts=201,
                title='Kitfort КТ-1315',
                ungrouped_blue=[
                    UngroupedModel(
                        group_id=11,
                        title="Расхлопнутая модель 10.1",
                        key='10_1',
                    ),
                    UngroupedModel(
                        group_id=12,
                        title="Расхлопнутая модель 10.2",
                        key='10_2',
                    ),
                ],
            )
        ]

        cls.index.mskus += [
            MarketSku(
                hyperid=10,
                sku=101,
                title='кофемолка Kitfort красная',
                ungrouped_model_blue=11,
            ),
            MarketSku(
                hyperid=10,
                sku=102,
                title='кофемолка Kitfort синяя',
                ungrouped_model_blue=12,
            ),
        ]

        cls.index.offers += [
            Offer(
                hyperid=10,
                ts=203,
                sku=101,
                ungrouped_model_blue=11,
                fesh=4,
                price=1000,
                title='дешевая красная кофемолка Kitfort',
                waremd5='cuh9-Rs2Izo9u62bWxHR0g',
            ),
            Offer(
                hyperid=10,
                ts=204,
                sku=101,
                ungrouped_model_blue=11,
                fesh=5,
                price=2000,
                title='красная кофемолка Kitfort из хорошего магазина',
                waremd5='bZiAfvAvKsLcwmHBBYc85g',
            ),
            Offer(
                hyperid=10,
                ts=205,
                sku=101,
                ungrouped_model_blue=11,
                fesh=6,
                price=1200,
                title='красная кофемолка Kitfort со средней ценой и наибольшей релевантностью',
                waremd5='OnRg6gAbOo9N16I0UumPCg',
            ),
            Offer(
                hyperid=10,
                ts=206,
                sku=102,
                ungrouped_model_blue=12,
                fesh=4,
                price=1500,
                title='дешевая синяя кофемолка Kitfort',
                waremd5='KJKzv45bgfBA_DNU6En_Tg',
            ),
            Offer(
                hyperid=10,
                ts=207,
                sku=102,
                ungrouped_model_blue=12,
                fesh=5,
                price=2500,
                title='синяя кофемолка Kitfort из хорошего магазина',
                waremd5='jd4Oce8wUIpQslw8nD-Ssw',
            ),
            Offer(
                hyperid=10,
                ts=208,
                sku=102,
                ungrouped_model_blue=12,
                fesh=6,
                price=2000,
                title='синяя кофемолка Kitfort со средней ценой и наибольшей релевантностью',
                waremd5='wm72uIY_kLJS94wR9Vdzgw',
            ),
        ]

        for place in [MnPlace.BASE_SEARCH, MnPlace.META_REARRANGE]:
            cls.matrixnet.on_place(place, 201).respond(0.9)
            cls.matrixnet.on_place(place, 202).respond(0.8)
            cls.matrixnet.on_place(place, 203).respond(0.7)
            cls.matrixnet.on_place(place, 204).respond(0.6)
            cls.matrixnet.on_place(place, 205).respond(0.75)
            cls.matrixnet.on_place(place, 206).respond(0.65)
            cls.matrixnet.on_place(place, 207).respond(0.55)
            cls.matrixnet.on_place(place, 208).respond(0.7)

    def test_default_offers(self):
        '''Проверяем, что ДО находится для скю, а не для моделей. И что он не обязан совпадать с оффером,
        из которого взяли эту скюшку, а выбирается по стандартной логике поиска ДО.
        Для этого проверим поиск ДО минимального по цене, и поиск ДО как оффера в хорошем магазине
        '''

        def gen_req(good_shop):
            return (
                'place=prime&debug=da&hid=5&text=кофемолка'
                '&platform=touch&use-default-offers=1&allow-collapsing=1'
                '&rearr-factors=market_white_ungrouping=1;'
                'market_default_offer_by_min_price={};'
                'market_metadoc_search=no;'
                'market_default_offer_by_min_price_in_good_shop={}'.format(1 - good_shop, good_shop)
            )

        for good_shop in (0, 1):
            # В трассировке мы проверим, что скю взялись из самых релевантных офферов.
            # Но эти релевантные оффера не являются ни самыми дешевыми, ни предложениями лучшего магазина,
            # поэтому в ДО они никогда не попадают
            if good_shop:
                default_offer_101 = {'sku': '101', 'shop': {'id': 5}, 'wareId': 'bZiAfvAvKsLcwmHBBYc85g'}
                default_offer_102 = {'sku': '102', 'shop': {'id': 5}, 'wareId': 'jd4Oce8wUIpQslw8nD-Ssw'}
            else:
                default_offer_101 = {'sku': '101', 'shop': {'id': 4}, 'wareId': 'cuh9-Rs2Izo9u62bWxHR0g'}
                default_offer_102 = {'sku': '102', 'shop': {'id': 4}, 'wareId': 'KJKzv45bgfBA_DNU6En_Tg'}
            response = self.report.request_json(gen_req(good_shop))
            str_response = str(response)
            # модель 10 схлопывалась из самых релевантных офферов для каждой группы
            assert 'g:dsrcid:6 ModeId:10 WareMd5:OnRg6gAbOo9N16I0UumPCg (collapsed)' in str_response
            assert 'g:dsrcid:6 ModeId:10 WareMd5:wm72uIY_kLJS94wR9Vdzgw (collapsed)' in str_response
            # у модели 10 есть скю 101 и 102, для каждой из них в UngroupedDefaultOffers хранится ДО,
            # но не по ts самого ДО, а по по ts оффера, из которого получили скюшку (более релевантного)
            assert (
                'Got ungrouped {}, SKU: 101, MODEL: 10 for ts 205'.format(default_offer_101['wareId']) in str_response
            )
            assert (
                'Got ungrouped {}, SKU: 102, MODEL: 10 for ts 208'.format(default_offer_102['wareId']) in str_response
            )

            self.assertFragmentIn(
                response,
                [
                    {'entity': 'product', 'id': 10, 'offers': {'items': [default_offer_101]}},
                    {'entity': 'product', 'id': 10, 'offers': {'items': [default_offer_102]}},
                ],
                preserve_order=True,
                allow_different_len=False,
            )

    def test_ungrouping_doesnt_affect_redirects(self):
        """
        Проверяем, что расхлопывание ску не влияет на редиректы
        """

        request = 'place=prime&text=ungrouped&debug=da&platform=touch&use-default-offers=1&allow-collapsing=1'
        param = '&allow-ungrouping=1'

        response = self.report.request_json(request)
        self.assertFragmentIn(response, {'debug': {'categories_ranking_json': NotEmptyList()}})
        expected_categories_factors = response.root['debug']['categories_ranking_json']

        # TODO(ants): maybe fix this factor either if it causes problems
        for e in expected_categories_factors:
            e['factors'].pop('GOODS_RATIO_IN_TOP', None)

        response = self.report.request_json(request + param)
        self.assertFragmentIn(response, {'debug': {'categories_ranking_json': expected_categories_factors}})

    @classmethod
    def prepare_no_duplicate_skus(cls):
        cls.index.models += [
            Model(
                hyperid=6,
                hid=2,
                title="skunodup 6",
                ts=10,
                ungrouped_blue=[
                    UngroupedModel(
                        group_id=10,
                        title="skunodup 6.10",
                        key='6_10',
                    ),
                    UngroupedModel(
                        group_id=11,
                        title="skunodup 6.11",
                        key='6_11',
                    ),
                ],
            ),
        ]

        cls.index.mskus += [
            MarketSku(
                fesh=1,
                title='skunodup 61',
                hyperid=6,
                sku=61,
                blue_offers=[
                    BlueOffer(price=5, ts=11),
                ],
                ungrouped_model_blue=10,
            ),
            MarketSku(
                fesh=1,
                title='skunodup 62',
                hyperid=6,
                sku=62,
                blue_offers=[
                    BlueOffer(price=5, ts=12),
                ],
                ungrouped_model_blue=11,
            ),
        ]

        for place in [MnPlace.BASE_SEARCH, MnPlace.META_REARRANGE]:
            cls.matrixnet.on_place(place, 10).respond(0.9)
            cls.matrixnet.on_place(place, 11).respond(0.8)
            cls.matrixnet.on_place(place, 12).respond(0.7)

    def test_no_duplicate_skus(self):
        """
        Проверяем, что на выдаче не дублируются ску, даже когда сама модель
        найдена в группировке yg
        """

        request = (
            'place=prime&text=skunodup&debug=da&'
            'platform=touch&allow-ungrouping=1&'
            'use-default-offers=1&allow-collapsing=1'
            '&rearr-factors=market_metadoc_search=no'
        )
        response = self.report.request_json(request)

        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'entity': 'product',
                        'id': 6,
                        'offers': {
                            'items': [
                                {
                                    'marketSku': '61',
                                }
                            ],
                        },
                    },
                    {
                        'entity': 'product',
                        'id': 6,
                        'offers': {
                            'items': [
                                {
                                    'marketSku': '62',
                                }
                            ],
                        },
                    },
                ]
            },
        )

    def test_ungrouped_do_show_log(self):
        request = (
            'place=prime&text=skunodup&debug=da&'
            'platform=touch&allow-ungrouping=1&'
            'use-default-offers=1&allow-collapsing=1&'
            'rearr-factors=market_metadoc_search=no'
        )
        _ = self.report.request_json(request)
        self.show_log_tskv.expect(
            show_uid="04884192001117778888816001",
            record_type=1,
            url_type=16,
            position=1,
            super_uid="04884192001117778888816001",
            inclid=0,
        )
        self.show_log_tskv.expect(
            show_uid="04884192001117778888806000",
            record_type=0,
            url_type=6,
            super_uid="04884192001117778888816001",
        )
        self.show_log_tskv.expect(
            show_uid="04884192001117778888816002",
            record_type=1,
            url_type=16,
            position=2,
            super_uid="04884192001117778888816002",
            inclid=0,
        )
        self.show_log_tskv.expect(
            show_uid="04884192001117778888806000",  # в проде шоуюиды разные за счет разных шоублокид
            record_type=0,
            url_type=6,
            super_uid="04884192001117778888816002",
        )

    @classmethod
    def prepare_ungrouped_skus_total_fix(cls):
        # Модель 11 с двумя ключами - 1.1 и 1.2
        # Модель 12 с двумя ключами - 1.1 и 1.2
        #
        # MSKU 11 c синим оффером, ungrouped_model_blue = 1
        # MSKU 12 c синим оффером, ungrouped_model_blue = 2
        # MSKU 13 c синим оффером, ungrouped_model_blue = 1
        # MSKU 13 c синим оффером, ungrouped_model_blue = 2

        cls.index.models += [
            Model(
                hyperid=4177701,
                hid=4177700,
                ts=4177701,
                title="Модель 101",
                ungrouped_blue=[
                    UngroupedModel(
                        group_id=4177711,
                        title="Расхлопнутая модель 41777.1",
                        key='41777_1',
                    ),
                    UngroupedModel(
                        group_id=4177712,
                        title="Расхлопнутая модель 41777.2",
                        key='41777_2',
                    ),
                ],
            ),
            Model(
                hyperid=4177702,
                hid=4177700,
                ts=4177702,
                title="Модель 102",
                ungrouped_blue=[
                    UngroupedModel(
                        group_id=4177711,
                        title="oneofthemany model 41777.1",
                        key='41777_1',
                    ),
                    UngroupedModel(
                        group_id=4177712,
                        title="oneofthemany model 41777.2",
                        key='41777_2',
                    ),
                ],
            ),
        ]

        cls.index.mskus += [
            MarketSku(
                fesh=1,
                title='oneofthemany 41777.1 - part 1',
                hyperid=4177701,
                sku=41777011,
                ts=4177703,
                blue_offers=[BlueOffer(price=5, feedid=2, ts=4177704)],
                ungrouped_model_blue=4177711,
            ),
            MarketSku(
                fesh=1,
                title='oneofthemany 41777.1 - part 2',
                hyperid=4177701,
                sku=41777012,
                ts=4177705,
                blue_offers=[BlueOffer(price=10, feedid=2, ts=4177706)],
                ungrouped_model_blue=4177712,
            ),
            MarketSku(
                fesh=1,
                title='oneofthemany 41777.2 - part 1',
                hyperid=4177702,
                sku=41777021,
                ts=4177707,
                blue_offers=[BlueOffer(price=15, feedid=2, ts=4177708)],
                ungrouped_model_blue=4177711,
            ),
            MarketSku(
                fesh=1,
                title='oneofthemany 41777.2 - part 2',
                hyperid=4177702,
                sku=41777022,
                ts=4177709,
                blue_offers=[BlueOffer(price=25, feedid=2, ts=4177710)],
                ungrouped_model_blue=4177712,
            ),
        ]

        cls.index.offers += [
            Offer(
                hyperid=4177701,
                ts=4177720 + seq,
                sku=41777011,
                ungrouped_model_blue=4177711,
                title='oneofthemany white offer {}'.format(seq),
                fesh=3,
            )
            for seq in range(1, 79)
        ]

        for place in [MnPlace.BASE_SEARCH, MnPlace.META_REARRANGE]:
            for i in range(0, 100):
                cls.matrixnet.on_place(place, 4177700 + seq).respond(0.2 - i * 0.001)

    def test_ungrouped_skus_total_fix(self):
        request = (
            'pp=7&platform=touch&hid=4177700&text=oneofthemany'
            '&rearr-factors=market_white_ungrouping=1'
            '&place=prime&&use-default-offers=1'
            '&rearr-factors=market_metadoc_search=no'
        )

        # ожидаем честное количество результатов
        # вне зависимости от market_prime_ungrouping_fix_total
        # TODO: флаг market_prime_ungrouping_fix_total, вероятно, уже не актуален
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 4,
                    "results": [
                        {"entity": "product"},
                        {"entity": "product"},
                        {"entity": "product"},
                        {"entity": "product"},
                    ],
                }
            },
            allow_different_len=False,
        )

        response = self.report.request_json(request + '&rearr-factors=market_prime_ungrouping_fix_total=1')
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 4,
                    "results": [
                        {"entity": "product"},
                        {"entity": "product"},
                        {"entity": "product"},
                        {"entity": "product"},
                    ],
                }
            },
            allow_different_len=False,
        )

    @classmethod
    def prepare_ungrouping_by_ungrouped_hyper_blue(cls):
        # варианты
        # все имеют один и тот же ungrouped_hyper_blue
        # M1      -
        # M1.OFF1 -
        # M1.SKU1 -
        # имеют разные ungrouped_hyper_blue
        # M2
        # M2.OFF2 - имеет тот же ungrouped_hyper_blue что и M2
        # M2.OFF3 - имеет другой ungrouped_hyper_blue чем M2
        # M2.SKU21
        # M2.SKU22
        # Тестируем: что при схлапывании на выдаче будут
        # без ДО:       с ДО:
        # M1            M1
        # M2            M2 -> ДО=M2.SKU21 или M2.SKU22 или M2.OFF3
        #               M2 -> ДО=M2.SKU21 или M2.SKU22 или M2.OFF3
        #               M2 -> ДО=M2.SKU21
        #               M2 -> ДО=M2.SKU22
        # При схлопывании с ДО (в зависимости от того какой оффер попадет в ДО для модели M2)
        # может остаться либо 3 документа от M2 (если ДО будет без ску)
        # либо останутся только M2.SKU21 и M2.SKU22

        cls.index.models += [
            Model(hyperid=56893001, title="Сдувшаяся рыба-еж", hid=568, ts=56801),
            Model(
                hyperid=56893002,
                title="Раздувшаяся рыба-еж",
                hid=568,
                ts=56802,
                ungrouped_blue=[
                    UngroupedModel(group_id=568930021, title="Раздувшаяся рыба-еж 10см", key='56893002_1'),
                    UngroupedModel(group_id=568930022, title="Раздувшаяся рыба-еж 25см", key='56893002_2'),
                    UngroupedModel(
                        group_id=568930023, title="Раздувшаяся рыба-еж непонятного размера", key='56893002_3'
                    ),
                ],
            ),
        ]

        cls.index.mskus += [
            MarketSku(
                fesh=1,
                title='Сдувшаяся рыба-еж (ску 1)',
                hyperid=56893001,
                sku=568930011,
                hid=568,
                blue_offers=[BlueOffer(price=5, feedid=2)],
            ),
            MarketSku(
                fesh=1,
                title='Раздувшаяся рыба-еж 10см (ску 1)',
                hyperid=56893002,
                sku=568930021,
                hid=568,
                blue_offers=[BlueOffer(price=10, feedid=2)],
                ungrouped_model_blue=568930021,
            ),
            MarketSku(
                fesh=1,
                title='Раздувшаяся рыба-еж 25см (ску 2)',
                hyperid=56893002,
                sku=568930022,
                hid=568,
                blue_offers=[BlueOffer(price=10, feedid=2)],
                ungrouped_model_blue=568930022,
            ),
        ]

        cls.index.offers += [
            Offer(hyperid=56893001, hid=568, title='Сдувшаяся рыба-еж (без ску)', fesh=3, ts=568001),
            Offer(
                hyperid=56893002,
                hid=568,
                title='Раздувшаяся рыба-еж непонятного размера (без ску и без фильтров)',
                fesh=3,
                ts=568002,
            ),
            Offer(
                hyperid=56893002,
                hid=568,
                title='Раздувшаяся рыба-еж непонятного размера (без ску и с частью фильтров)',
                fesh=3,
                ts=568003,
                ungrouped_model_blue=568930023,
            ),
        ]

        # сделаем офферы чуть более приоритетными чем модели
        for place in [MnPlace.BASE_SEARCH, MnPlace.META_REARRANGE]:
            cls.matrixnet.on_place(place, 56801).respond(0.2)
            cls.matrixnet.on_place(place, 56802).respond(0.2)
            cls.matrixnet.on_place(place, 568001).respond(0.4)
            cls.matrixnet.on_place(place, 568002).respond(0.4)
            cls.matrixnet.on_place(place, 568003).respond(0.4)

    def test_without_default_offers(self):
        """Если нет ДО - то отличить модели от скух невозможно"""
        response = self.report.request_json(
            'place=prime&text=рыба-еж&allow-collapsing=1&allow-ungrouping=1&use-default-offers=0&debug=da&rearr-factors=market_metadoc_search=no'
        )

        self.assertFragmentIn(
            response,
            {
                'results': [
                    {"titles": {"raw": "Сдувшаяся рыба-еж"}},  # M1
                    {"titles": {"raw": "Раздувшаяся рыба-еж"}},  # M2
                ]
            },
            allow_different_len=False,
            preserve_order=False,
        )

    def test_with_default_offers(self):
        response = self.report.request_json(
            'place=prime&text=рыба-еж&allow-collapsing=1&allow-ungrouping=1&use-default-offers=1&debug=da&rearr-factors=market_metadoc_search=no'
        )

        self.assertFragmentIn(
            response,
            {
                'results': [
                    {"titles": {"raw": "Сдувшаяся рыба-еж"}},  # M1 + M1.SKU1 + M1.OFF1
                    {
                        "titles": {"raw": "Раздувшаяся рыба-еж"},
                        "offers": {"items": [{"titles": {"raw": "Раздувшаяся рыба-еж 10см (ску 1)"}}]},
                    },  # M2
                    {
                        "titles": {"raw": "Раздувшаяся рыба-еж"},
                        "offers": {"items": [{"titles": {"raw": "Раздувшаяся рыба-еж 25см (ску 2)"}}]},
                    },  # M2
                ]
            },
            allow_different_len=False,
            preserve_order=False,
        )

    def test_metadoc_skus_without_default_offers(self):
        """Поскольку ДО нет то модель это отдельный документ на выдаче отличный от документа скухи"""
        # TODO(ants) тут в скушной выдаче отсутствуют тайтлы у скух
        response = self.report.request_json(
            'place=prime&text=рыба-еж&allow-collapsing=1&allow-ungrouping=1&use-default-offers=0&debug=da&rearr-factors=market_metadoc_search=skus'
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {"titles": {"raw": "Сдувшаяся рыба-еж"}},  # M1
                    {"titles": {"raw": "Раздувшаяся рыба-еж"}},  # M2 + M2.OFF2 + M2.OFF3
                    {"entity": "sku", "id": "568930021"},  # M2.SKU21
                    {"entity": "sku", "id": "568930022"},  # M2.SKU22
                ]
            },
            allow_different_len=False,
            preserve_order=False,
        )

    def test_metadoc_skus_with_default_offers(self):
        """Модель получает ДО равное ДО одной из скух и они схлапываются в один документ на выдаче"""
        # TODO(ants)
        response = self.report.request_json(
            'place=prime&text=рыба-еж&allow-collapsing=1&allow-ungrouping=1&use-default-offers=1&debug=da&rearr-factors=market_metadoc_search=skus'
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {"titles": {"raw": "Сдувшаяся рыба-еж"}},  # M1
                    {"titles": {"raw": "Раздувшаяся рыба-еж"}},  # M2 + одна из скух
                    {"entity": "sku", "id": EqualToOneOf("568930021", "568930022")},  # M2.SKU21 или M2.SKU22
                ]
            },
            allow_different_len=False,
            preserve_order=False,
        )

    @classmethod
    def prepare_filter_offer_with_model_without_sku(cls):
        cls.index.offers += [
            Offer(
                hyperid=10,
                ts=303,
                sku=101,
                fesh=4,
                price=1000,
                title='чайник с крышечкой',
            ),
            Offer(
                hyperid=10,
                ts=304,
                fesh=5,
                price=2000,
                title='чайник без крышечки',
            ),
            Offer(
                ts=304,
                fesh=5,
                price=2000,
                title='чайник безмодельный',
            ),
        ]

    def test_filter_offer_with_model_without_sku(self):
        """проверяем, что при включении флага на базовых фильтруются офферы с моделью и без ску.
        Флаг market_filter_offers_with_model_without_sku=1 раскатан по дефолту.
        https://st.yandex-team.ru/MARKETOUT-43289
        """

        response = self.report.request_json(
            'place=prime&text=чайник&allow-collapsing=0&use-default-offers=1&debug=da&rearr-factors=market_filter_offers_with_model_without_sku=1;market_metadoc_search=no'
        )
        self.assertFragmentIn(response, {"filters": {"OFFER_FILTERED_OUT_BY_HAS_MODEL_AND_WITHOUT_MSKU": 1}})
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {"titles": {"raw": "чайник с крышечкой"}},
                    {"titles": {"raw": "чайник безмодельный"}},
                ]
            },
            allow_different_len=False,
            preserve_order=False,
        )
        response = self.report.request_json(
            'place=prime&text=чайник&allow-collapsing=0&use-default-offers=1&debug=da&rearr-factors=market_filter_offers_with_model_without_sku=1;market_metadoc_search=offers'
        )
        self.assertFragmentIn(response, {"filters": {"OFFER_FILTERED_OUT_BY_HAS_MODEL_AND_WITHOUT_MSKU": 1}})


if __name__ == '__main__':
    main()
