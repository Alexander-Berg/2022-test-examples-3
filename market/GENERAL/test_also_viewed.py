#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from unittest import skip

from core.types import (
    BlueOffer,
    Const,
    GLParam,
    GLType,
    GLValue,
    HybridAuctionParam,
    HyperCategory,
    MarketSku,
    MnPlace,
    Model,
    NavCategory,
    Offer,
    Opinion,
    ReportState,
    Shop,
    Vat,
    YamarecPlace,
    YamarecSettingPartition,
)
from core.dj import DjModel
from core.types.picture import to_mbo_picture
from core.testcase import TestCase, main
from core.matcher import Absent, ElementCount, Equal, LikeUrl, NotEmpty
from core.bigb import SkuPurchaseEvent, BeruSkuOrderCountCounter
from market.proto.recom.exported_dj_user_profile_pb2 import (
    TEcomVersionedDjUserProfile,
    TVersionedProfileData,
    TFashionDataV1,
    TFashionSizeDataV1,
)  # noqa pylint: disable=import-error

CLOTHES_SIZE_PARAM_ID = 26417130
CLOTHES_SIZE_PARAM_VALUES = [
    (27016810, 'XS'),
    (27016830, 'S'),
    (27016891, 'M'),
    (27016892, 'L'),
    (27016910, 'XL'),
]

MODEL_IDS_FOR_VISUAL_SEARCH = [415, 416, 417, 418]
MSKU_SHIFT = 1000
MSKU_FOR_VISUAL_ANALOGUE_MIX = MSKU_SHIFT + 1


class T(TestCase):
    """
    Набор тестов для place=also_viewed
    """

    @classmethod
    def prepare(cls):
        """
        Модели, офферы и конфигурация для выдачи place=also_viewed
        """
        model_ids = list(range(1, 6))
        random_ts = [5, 2, 6, 3, 1, 4]
        cls.index.shops += [
            Shop(fesh=13, priority_region=213, fulfillment_virtual=True, virtual_shop_color=Shop.VIRTUAL_SHOP_BLUE),
            Shop(fesh=777, datafeed_id=777, priority_region=213, supplier_type=Shop.THIRD_PARTY, blue=Shop.BLUE_REAL),
        ]
        cls.index.models += [Model(hyperid=x[0], ts=x[1], hid=101, vbid=100) for x in zip(model_ids, random_ts)]
        cls.index.offers += [
            Offer(
                hyperid=hyperid,
                price=hyperid * 100 if hyperid != 2 else 50,
                fesh=13,
                waremd5="DEFAULTA{}AAAAAAAAAAAAA".format(hyperid),
            )
            for hyperid in model_ids
        ]

        cls.index.yamarec_places += [
            YamarecPlace(
                name=YamarecPlace.Name.ALSO_VIEWED_PRODUCTS,
                kind=YamarecPlace.Type.SETTING,
                split_rule=YamarecPlace.SplitRule.ABT,
                partitions=[
                    # no partition with split 'noconfig'
                    # empty matching partition
                    YamarecSettingPartition(splits=[{'split': 'empty'}]),
                    # partitions with data
                    YamarecSettingPartition(params={'version': '1'}, splits=[{'split': 'normal'}]),
                    # partitions with data
                    YamarecSettingPartition(params={'version': 'SIBLINGS1'}, splits=[{'split': 'siblings'}]),
                    YamarecSettingPartition(params={'version': 'MODEL/MSKU1'}, splits=[{'split': 'model_with_msku'}]),
                ],
            ),
            YamarecPlace(
                name=YamarecPlace.Name.COMPETITIVE_MODEL,
                kind=YamarecPlace.Type.SETTING,
                split_rule=YamarecPlace.SplitRule.ABT,
                partitions=[
                    # no partition with split 'noconfig'
                    # partitions with data
                    YamarecSettingPartition(params={'version': 'SIBLINGS1_AUGMENTED'}, splits=[{'split': 'empty'}]),
                ],
            ),
        ]
        cls.recommender.on_request_accessory_models(model_id=1, item_count=1000, version='1').respond(
            {'models': ['4', '2', '3', '5']}
        )

    def test_base_counters(self):
        request = 'place=also_viewed&rearr-factors=split=normal&hyperid=1&show-stats=only_stats'
        response = self.report.request_json(request)
        self.assertFragmentNotIn(response, {'base_cpu_time_us': 0})

    def test_noconfig(self):
        """
        Проверка корректности работы конфига при отсутствии сплита или данных
        """
        noconfig_request = 'place=also_viewed&rearr-factors=split=noconfig&hyperid=1'
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

        request = 'place=also_viewed&rearr-factors=split=normal&hyperid=1'
        response = self.report.request_json(request)
        self.assertFragmentIn(response, {'total': 4})

        response = self.report.request_json(request + '&numdoc=2')
        self.assertFragmentIn(response, {'total': 4})
        self.access_log.expect(total_renderable='4').times(2)

        """
        Проверка поля url_hash в show log
        """
        self.show_log_tskv.expect(url_hash=ElementCount(32))

    def test_minimize_output(self):
        """
        Проверяется, что с параметром minimize-output ответ содержит только нужные поля и одну картинку
        """

        request = 'place=also_viewed&minimize-output=1&rearr-factors=split=normal&hyperid=1'
        response = self.report.request_json(request)
        self.assertEqual(response.count({'entity': 'picture'}), 4)
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'total': 4,
                    'results': [
                        {
                            'entity': 'product',
                            'titles': NotEmpty(),
                            'prices': NotEmpty(),
                        }
                    ],
                }
            },
        )
        self.assertFragmentNotIn(
            response,
            {
                'search': {
                    'results': [
                        {
                            'offers': NotEmpty(),
                        }
                    ],
                }
            },
        )

    def test_product_analogs(self):
        """
        Проверяется, что product_analogs редиректится в also_viewed
        """
        base_requests = [
            'place={}&hyperid=1',
            'place={}&rearr-factors=split=noconfig&hyperid=1',
            'place={}&rearr-factors=split=normal&hyperid=1',
            'place={}&rearr-factors=split=normal&hyperid=1&page=2&numdoc=2',
        ]
        for req in base_requests:
            request_product_analogs = req.format('product_analogs')
            request_also_viewed = req.format('also_viewed')

            self.assertEqualJsonResponses(request_product_analogs, request_also_viewed)

    def test_order(self):
        """
        Порядок выдачи должен соответствовать порядку рекомендаций в выгрузке
        В индексе модели размещены в другом порядке (см. поле ts)
        """
        response = self.report.request_json('place=also_viewed&rearr-factors=split=normal&hyperid=1')
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

    def test_position(self):
        _ = self.report.request_json('place=also_viewed&rearr-factors=split=normal&hyperid=1&page=1&numdoc=2')
        self.show_log.expect(shop_id=99999999, position=0)
        self.show_log.expect(shop_id=99999999, position=1)
        _ = self.report.request_json('place=also_viewed&rearr-factors=split=normal&hyperid=1&page=2&numdoc=2')
        self.show_log.expect(shop_id=99999999, position=2)
        self.show_log.expect(shop_id=99999999, position=3)

    def test_rgb(self):
        """
        Плэйс предназначен для rgb=green маркета, но должен вызываться и работать и для rgb=green_with_blue
        """
        for market_type in ['', 'green', 'green_with_blue']:
            response = self.report.request_json(
                'place=also_viewed&rearr-factors=split=normal&hyperid=1&rgb={}'.format(market_type)
            )
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
    def prepare_shop_siblings(cls):
        cls.recommender.on_request_accessory_models(model_id=1, item_count=1000, version='SIBLINGS1').respond(
            {'models': ['4', '2', '3']}
        )
        cls.recommender.on_request_accessory_models(model_id="13_1", item_count=1000, version='SIBLINGS1').respond(
            {'models': ['4', '2']}
        )

    def test_shop_siblings(self):
        request = 'place=also_viewed&rearr-factors=split=siblings&hyperid=1&fesh=13'
        response = self.report.request_json(request)
        self.assertFragmentIn(response, {'total': 3})

        request = 'place=also_viewed&rearr-factors=split=siblings;shop_model_recommend=1&hyperid=1&fesh=13'
        response = self.report.request_json(request)
        self.assertFragmentIn(response, {'total': 2})

    @classmethod
    def prepare_no_hidfilter(cls):
        cls.index.hypertree += [
            HyperCategory(hid=101),
            HyperCategory(hid=102),
        ]

    def test_no_hidfilter(self):
        """
        Hid-фильтр должен игнорироваться
        """
        response = self.report.request_json('place=also_viewed&rearr-factors=split=normal&hyperid=1&hid=102')
        self.assertFragmentIn(response, {'search': {'total': 4, 'results': ElementCount(4)}})

    @classmethod
    def prepare_yandex_phone(cls):
        cls.recommender.on_request_accessory_models(model_id=177547282, item_count=1000, version='1').respond(
            {'models': ['4', '2', '3', '5']}
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

        cls.index.offers += [Offer(hyperid=8)]

        cls.recommender.on_request_accessory_models(model_id=7, item_count=1000, version='1').respond({'models': ['8']})

    @classmethod
    def prepare_also_viewed_models_for_parallel(cls):
        cls.index.navtree += [NavCategory(nid=338200, hid=33820)]

        cls.index.models += [
            Model(hyperid=338207, hid=33820),
            Model(
                hyperid=338208,
                hid=33820,
                opinion=Opinion(rating=4.5, precise_rating=4.53),
                title='Смартфон Apple iPhone 7 64GB',
                title_no_vendor='Смартфон iPhone 7 64GB',
            ),
        ]

        cls.index.offers += [Offer(hyperid=338208)]

        cls.recommender.on_request_accessory_models(model_id=338207, item_count=1000, version='1').respond(
            {'models': ['338208']}
        )

    def test_also_viewed_models_for_parallel(self):
        """Проверяем выдачу для ручки связанных моделей на серпе
        https://st.yandex-team.ru/MARKETOUT-33085
        """

        # Выдача в формате, совпадающем с модельной и офферной каруселью на серпе
        # Подробнее про них: https://st.yandex-team.ru/MARKETOUT-31669
        for device in ('desktop', 'touch'):
            rearr = 'rearr-factors=device={0}'.format(device)
            if device == 'touch':
                rearr += '&touch=1'
            response = self.report.request_parallel_data(
                'place=also_viewed&hyperid=338207&rearr-factors=split=normal&dj-output-items=parallel_models&{0}'.format(
                    rearr
                )
            )

            desktop_url = "//market.yandex.ru/product/338208?hid=33820&nid=338200&clid=838"
            touch_url = "//m.market.yandex.ru/product/338208?hid=33820&nid=338200&clid=838"

            title = "Apple iPhone 7 64GB"  # категория вырезается, т.к. флаг market_implicit_model_title_no_category по умолчанию включен

            self.assertFragmentIn(
                response,
                [
                    {
                        "titleText": title,
                        "url": LikeUrl.of(desktop_url if device == 'desktop' else touch_url),
                        "modelId": "338208",
                        "ratingValue": 4.53,
                        "priceFromValue": "100",
                        "priceCurrency": "RUR",
                        "image": "//mdata.yandex.net/i?path=b0130135356_img_id2520674011472212068.jpg&size=2",
                        "imageHd": "//mdata.yandex.net/i?path=b0130135356_img_id2520674011472212068.jpg&size=5",
                    }
                ],
                allow_different_len=False,
            )

        # Поддержка игнора уже показанных документов через параметр &slider-ignore-doc=.
        response = self.report.request_parallel_data(
            'place=also_viewed&hyperid=338207&rearr-factors=split=normal&dj-output-items=parallel_models&slider-ignore-doc=338208'
        )
        self.assertFragmentIn(response, [], allow_different_len=False)

    def test_short_format(self):
        """Проверяем сокращенную выдачу с параметром also-viewed-short-format=1"""

        response = self.report.request_json('place=also_viewed&hyperid=7&hid=103&rearr-factors=split=normal')
        self.assertFragmentIn(response, {'filters': []})
        self.assertFragmentIn(
            response, {'entity': 'product', 'id': 8, 'pictures': ElementCount(2), 'offers': {'count': 1, 'items': []}}
        )

        response = self.report.request_json(
            'place=also_viewed&hyperid=7&hid=103&rearr-factors=split=normal&also-viewed-short-format=1'
        )
        self.assertFragmentIn(
            response, {'entity': 'product', 'id': 8, 'pictures': ElementCount(1), 'offers': Equal({'count': 1})}
        )
        self.assertFragmentNotIn(response, {'filters': []})

        response = self.report.request_json(
            'place=also_viewed&hyperid=7&hid=103&rearr-factors=split=normal&also-viewed-short-format=1&touch=1'
        )
        self.assertFragmentIn(
            response,
            {
                'entity': 'product',
                'id': 8,
                'pictures': ElementCount(1),
                'offers': Equal(
                    {
                        'items': [
                            {
                                'prices': {
                                    'currency': 'RUR',
                                    'value': '100',
                                    'isDeliveryIncluded': False,
                                    'isPickupIncluded': False,
                                    'rawValue': '100',
                                }
                            }
                        ]
                    }
                ),
            },
        )

    def test_short_format_with_sku(self):
        """Проверяем сокращенную выдачу с sku с параметром also-viewed-short-format-with-sku=1"""

        response = self.report.request_json('place=also_viewed&hyperid=7&hid=103&rearr-factors=split=normal')
        self.assertFragmentIn(response, {'filters': []})
        self.assertFragmentIn(
            response, {'entity': 'product', 'id': 8, 'pictures': ElementCount(2), 'offers': {'count': 1, 'items': []}}
        )

        response = self.report.request_json(
            'place=also_viewed&hyperid=7&hid=103&rearr-factors=split=normal&also-viewed-short-format-with-sku=1'
        )

        self.assertFragmentNotIn(response, {'filters': []})
        self.assertFragmentIn(
            response,
            {
                'offers': {
                    'count': 1,
                    "cutPriceCount": 0,
                    "items": [
                        {
                            "benefit": {"type": "default"},
                            "entity": "offer",
                            "wareId": NotEmpty(),
                            "prices": NotEmpty(),
                            "pictures": NotEmpty(),
                        }
                    ],
                }
            },
        )

    @classmethod
    def prepare_premium_offer(cls):
        cls.index.hybrid_auction_settings += [HybridAuctionParam(category=Const.ROOT_HID, cpc_ctr_for_cpc=0.003)]

        cls.index.navtree += [NavCategory(nid=2865800, hid=2865800, primary=True)]

        cls.index.shops += [
            Shop(fesh=2865810, name='Белый магазин 1', priority_region=213),
            Shop(fesh=2865820, name='Белый магазин 2', priority_region=213),
            Shop(fesh=2865830, name='Белый магазин 3', priority_region=213),
        ]

        cls.index.models += [
            Model(hyperid=2865811, hid=2865800, vendor_id=28658001),
        ]

        for seq in range(6):
            cls.index.models += [
                Model(hyperid=2865801 + seq, hid=2865800, vendor_id=28658001),
            ]

            cls.index.offers += [
                Offer(
                    hyperid=2865801 + seq,
                    fesh=2865810,
                    price=10000,
                    bid=1,
                    waremd5="AAAAAAAA{}AAAAAAAAAAAAA".format(seq + 1),
                    ts=2865801 + seq,
                ),  # default; CPM = 100000 * 10 * 0.02 ~ 20000
            ]

            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 2865801 + seq).respond(0.001)

            if seq % 3 != 1:
                cls.index.offers += [
                    Offer(
                        hyperid=2865801 + seq,
                        fesh=2865820,
                        title='Offer B' + str(seq + 1),
                        descr='Description Offer B' + str(seq + 1),
                        price=10000,
                        bid=50,
                        vendor_id=28658001,
                        vbid=15,
                        waremd5="BBBBBBBB{}BBBBBBBBBBBBB".format(seq + 1),
                        ts=2865811 + seq,
                    ),  # premium & top-1; CPM = 100000 * 50 * 0.01 ~ 50000
                    Offer(
                        hyperid=2865801 + seq,
                        fesh=2647020,
                        title='Offer D' + str(seq + 1),
                        price=10000,
                        bid=49,
                        vbid=14,
                        waremd5="DDDDDDDD{}DDDDDDDDDDDDD".format(seq + 1),
                        ts=2865831 + seq,
                    ),  # premium & top-1; CPM = 100000 * 50 * 0.01 ~ 50000
                    Offer(
                        hyperid=2647001 + seq,
                        fesh=2865830,
                        price=10000,
                        bid=10,
                        waremd5="CCCCCCCC{}CCCCCCCCCCCCC".format(seq + 1),
                        ts=2647021 + seq,
                    ),  # CPM = 100000 * 10 * 0.01 ~ 10000
                ]
                cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 2865811 + seq).respond(0.02)
                cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 2865831 + seq).respond(0.01)
                cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 2865821 + seq).respond(0.0001)

        for seq in (2, 5):
            cls.index.mskus += [
                MarketSku(
                    hyperid=2865801 + seq,
                    sku=28658000 + seq,
                    blue_offers=[
                        BlueOffer(
                            price=500,
                            title='Blue offer E' + str(seq + 1),
                            descr='Description Blue offer E' + str(seq + 1),
                            bid=1000,
                            vbid=1000,
                            vat=Vat.NO_VAT,
                            feedid=777,
                            vendor_id=28658001,
                            waremd5="EEEEEEEE{}EEEEEEEEEEEEE".format(seq + 1),
                            ts=2865841 + seq,
                        ),
                    ],
                ),
            ]
            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 2865841 + seq).respond(0.01)

        cls.recommender.on_request_accessory_models(model_id=2865811, item_count=1000, version='1').respond(
            {'models': ['2865801', '2865802', '2865803', '2865804', '2865805', '2865806']}
        )

    def test_also_viewed_premium_offer(self):
        """Проверяем выдачу премиальных офферов в плейсе also_viewed, проверяем пейджинг"""

        response = self.report.request_json(
            'place=also_viewed&hyperid=2865811&hid=2865800&rearr-factors=split=normal;market_ranging_cpa_by_ue_in_top_cpa_multiplier=1;market_ranging_cpa_by_ue_in_top=0;&use-premium-offers=1&numdoc=3&show-urls=encrypted'  # noqa
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'entity': 'product',
                        'id': 2865801,
                        'offers': {
                            'items': [
                                {
                                    'urls': {
                                        'encrypted': NotEmpty(),
                                    },
                                    'wareId': 'BBBBBBBB1BBBBBBBBBBBBA',
                                    'isPremium': True,
                                }
                            ]
                        },
                    },
                    {
                        'entity': 'product',
                        'id': 2865802,
                        'offers': {
                            'items': [
                                {
                                    'urls': {
                                        'encrypted': NotEmpty(),
                                    },
                                    'wareId': 'AAAAAAAA2AAAAAAAAAAAAA',
                                    'isPremium': Absent(),
                                }
                            ]
                        },
                    },
                    {
                        'entity': 'product',
                        'id': 2865803,
                        'offers': {
                            'items': [
                                {
                                    'urls': {
                                        'encrypted': NotEmpty(),
                                    },
                                    'wareId': 'EEEEEEEE3EEEEEEEEEEEEA',
                                    'isPremium': True,
                                }
                            ]
                        },
                    },
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )

        self.show_log.expect(
            hyper_id=2865801,
            click_price=13,
            cpm=26000,
            next_offer_cpm=0.13,
            bid=13,
            min_bid=13,
            is_premium_offer=1,
            price=10000,
            hyper_cat_id=2865800,
            nid=2865800,
            position=0,
            vendor_click_price=0,
            vendor_id=28658001,
        )
        self.click_log.expect(
            hyper_id=2865801,
            cp=13,
            cb=13,
            price=10000,
            hyper_cat_id=2865800,
            min_bid=13,
            nav_cat_id=2865800,
            position=0,
            cp_vnd=0,
            vnd_id=28658001,
        )

        self.show_log.expect(
            hyper_id=2865802, click_price=13, bid=13, min_bid=13, price=10000, hyper_cat_id=2865800, nid=2865800
        )
        self.click_log.expect(
            hyper_id=2865802, cp=13, cb=13, price=10000, hyper_cat_id=2865800, min_bid=13, nav_cat_id=2865800
        )

        self.show_log.expect(
            hyper_id=2865803,
            click_price=1,
            cpm=1000,
            next_offer_cpm=0.02,
            bid=1,
            min_bid=1,
            msku=28658002,
            is_blue_offer=1,
            is_premium_offer=1,
            price=500,
            hyper_cat_id=2865800,
            nid=2865800,
            position=0,
            vendor_click_price=0,
            vendor_id=28658001,
        )
        self.click_log.expect(
            hyper_id=2865803,
            cp=1,
            cb=1,
            price=500,
            is_blue=1,
            hyper_cat_id=2865800,
            min_bid=1,
            msku=28658002,
            nav_cat_id=2865800,
            position=0,
            cp_vnd=0,
            vnd_id=28658001,
        )

        response = self.report.request_json(
            'place=also_viewed&hyperid=2865811&hid=2865800&rearr-factors=split=normal;market_ranging_cpa_by_ue_in_top_cpa_multiplier=1;market_ranging_cpa_by_ue_in_top=0;&use-premium-offers=1&numdoc=3&page=2&show-urls=encrypted'  # noqa
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'entity': 'product',
                        'id': 2865804,
                        'offers': {
                            'items': [
                                {
                                    'urls': {
                                        'encrypted': NotEmpty(),
                                    },
                                    'wareId': 'BBBBBBBB4BBBBBBBBBBBBA',
                                    'isPremium': True,
                                }
                            ]
                        },
                    },
                    {
                        'entity': 'product',
                        'id': 2865805,
                        'offers': {
                            'items': [
                                {
                                    'urls': {
                                        'encrypted': NotEmpty(),
                                    },
                                    'wareId': 'AAAAAAAA5AAAAAAAAAAAAA',
                                    'isPremium': Absent(),
                                }
                            ]
                        },
                    },
                    {
                        'entity': 'product',
                        'id': 2865806,
                        'offers': {
                            'items': [
                                {
                                    'urls': {
                                        'encrypted': NotEmpty(),
                                    },
                                    'wareId': 'EEEEEEEE6EEEEEEEEEEEEA',
                                    'isPremium': True,
                                }
                            ]
                        },
                    },
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )

        self.show_log.expect(
            hyper_id=2865804,
            click_price=13,
            cpm=26000,
            next_offer_cpm=0.13,
            bid=13,
            min_bid=13,
            is_premium_offer=1,
            price=10000,
            hyper_cat_id=2865800,
            nid=2865800,
            position=0,
            vendor_click_price=0,
            vendor_id=28658001,
        )
        self.click_log.expect(
            hyper_id=2865804,
            cp=13,
            cb=13,
            price=10000,
            hyper_cat_id=2865800,
            min_bid=13,
            nav_cat_id=2865800,
            position=0,
            cp_vnd=0,
            vnd_id=28658001,
        )

        self.show_log.expect(
            hyper_id=2865805, click_price=13, bid=13, min_bid=13, price=10000, hyper_cat_id=2865800, nid=2865800
        )
        self.click_log.expect(
            hyper_id=2865805, cp=13, cb=13, price=10000, hyper_cat_id=2865800, min_bid=13, nav_cat_id=2865800
        )

        self.show_log.expect(
            hyper_id=2865806,
            click_price=1,
            cpm=1000,
            next_offer_cpm=0.02,
            bid=1,
            min_bid=1,
            msku=28658005,
            is_blue_offer=1,
            is_premium_offer=1,
            price=500,
            hyper_cat_id=2865800,
            nid=2865800,
            position=0,
            vendor_click_price=0,
            vendor_id=28658001,
        )
        self.click_log.expect(
            hyper_id=2865806,
            cp=1,
            cb=1,
            price=500,
            is_blue=1,
            hyper_cat_id=2865800,
            min_bid=1,
            msku=28658005,
            nav_cat_id=2865800,
            position=0,
            cp_vnd=0,
            vnd_id=28658001,
        )

    @classmethod
    def prepare_sku_enrichment_from_bigb(cls):
        cls.index.mskus += [
            MarketSku(
                hid=101,
                hyperid=4,
                sku=170,
                title="MSKU-170",
                blue_offers=[BlueOffer(price=170, fesh=13)],
            ),
            MarketSku(
                hid=101,
                hyperid=4,
                sku=171,
                title="MSKU-171",
                blue_offers=[BlueOffer(price=171, fesh=13)],
            ),
            MarketSku(
                hid=101,
                hyperid=4,
                sku=172,
                title="MSKU-172",
                blue_offers=[BlueOffer(price=172, fesh=13)],
            ),
        ]

        sku_purchases_counter = BeruSkuOrderCountCounter(
            [
                SkuPurchaseEvent(sku_id=70, count=1),
                SkuPurchaseEvent(sku_id=170, count=1),
            ]
        )
        cls.bigb.on_request(yandexuid='007', client='merch-machine').respond(counters=[sku_purchases_counter])
        cls.recommender.on_request_accessory_models_with_msku(
            model_id=1, item_count=1000, version='MODEL/MSKU1'
        ).respond({'models': ['4/172']})

    def test_sku_enrichment_from_bigb(self):
        """
        Если ску модели не определена, но известно, что пользователь покупал какую-то конкретную ску данной модели,
        то в выводе плейса будет предложена именно эта ску. Данные берутся из бигб
        Для этого делаем запрос с yandexuid=007, чтобы было на что счетчик возвращать
        """

        response = self.report.request_json('place=also_viewed&rearr-factors=split=normal&hyperid=1&yandexuid=007')
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 4,
                    "results": [
                        {
                            "id": 4,
                            "offers": {"items": [{"marketSku": "170", "sku": "170", "titles": {"raw": "MSKU-170"}}]},
                            "titles": {"raw": "HYPERID-4"},
                        },
                        {
                            "id": 2,
                            "offers": {"items": [{"marketSku": Absent(), "sku": Absent(), "titles": {"raw": ""}}]},
                            "titles": {"raw": "HYPERID-2"},
                        },
                    ],
                }
            },
        )

        # проверяем, что сохраняется ску из ответа рекоммендера, а не из истории покупок пользователя
        response = self.report.request_json('place=also_viewed&rearr-factors=split=model_with_msku&hyperid=1')
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 1,
                    "results": [
                        {
                            "id": 4,
                            "offers": {"items": [{"marketSku": "172", "sku": "172", "titles": {"raw": "MSKU-172"}}]},
                            "titles": {"raw": "HYPERID-4"},
                        }
                    ],
                }
            },
            preserve_order=True,
        )

    @classmethod
    def prepare_clothes_boosting_from_dj_profile(cls):
        cls.settings.set_default_reqid = False

        cls.index.hypertree += [
            HyperCategory(
                hid=7877999,
                name='Одежда, обувь и аксессуары',
                children=[
                    HyperCategory(
                        hid=7811873,
                        name='Женская одежда',
                        children=[
                            HyperCategory(hid=7811945, name='Женские платья'),
                        ],
                    ),
                ],
            ),
            HyperCategory(hid=11111, name='Какая-то категория не одежды'),
        ]

        cls.index.gltypes += [
            GLType(
                param_id=CLOTHES_SIZE_PARAM_ID,
                hid=7811945,
                gltype=GLType.ENUM,
                xslname='size_clothes_new',
                values=[
                    GLValue(value_id=value_id, text=param_name) for (value_id, param_name) in CLOTHES_SIZE_PARAM_VALUES
                ],
            ),
            GLType(
                param_id=CLOTHES_SIZE_PARAM_ID,
                hid=7811873,
                gltype=GLType.ENUM,
                xslname='size_clothes_new',
                values=[
                    GLValue(value_id=value_id, text=param_name) for (value_id, param_name) in CLOTHES_SIZE_PARAM_VALUES
                ],
            ),
        ]

        cls.index.models += [
            Model(hyperid=10, hid=7812186, title='Куртка мужская Adidas'),
            Model(hyperid=11, hid=7811945, title='Платье женское Baon'),
            Model(hyperid=12, hid=11111, title='Модель не одежды'),
            Model(hyperid=13, hid=11111, title='Модель не одежды 2'),
        ]

        for seq, (value_id, param_name) in enumerate(CLOTHES_SIZE_PARAM_VALUES):
            cls.index.offers += [
                Offer(
                    hid=7811945,
                    hyperid=11,
                    price=100 - seq * 10,
                    title='Платье женское Baon размер ' + param_name,
                    glparams=[
                        GLParam(param_id=CLOTHES_SIZE_PARAM_ID, value=value_id),
                    ],
                    ts=4423750 + seq,
                    randx=seq,
                ),
            ]

        for seq in range(15, 20):
            cls.index.offers += [
                Offer(
                    hid=11111,
                    hyperid=13,
                    price=100,
                    title='Не одежда (hyperid = 13) ' + str(seq),
                    glparams=[
                        GLParam(param_id=15, value=30 - seq),
                    ],
                    ts=4423850 + seq,
                    randx=seq,
                ),
            ]

        for seq in range(0, 50):
            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 4423750 + seq).respond(0.5 - seq * 0.01)
            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 4423850 + seq).respond(0.5 - seq * 0.01)

        # Dj будет рекомендовать одежду при запросе модели #10, и не одежду для модели #12
        cls.recommender.on_request_accessory_models(model_id=10, item_count=1000, version='1').respond(
            {'models': ['11']}
        )
        cls.recommender.on_request_accessory_models(model_id=12, item_count=1000, version='1').respond(
            {'models': ['13']}
        )

        profile = TEcomVersionedDjUserProfile(
            ProfileData=TVersionedProfileData(
                FashionV1=TFashionDataV1(
                    SizeClothesFemale=TFashionSizeDataV1(
                        Sizes={"46": 0.288602501, "48": 0.355698764, "S": 0.395698764, "M": 0.288602501}
                    ),
                )
            )
        )

        cls.bigb.on_request(yandexuid='011', client='merch-machine').respond(counters=[])
        cls.dj.on_request(yandexuid='011', exp='fetch_user_profile_versioned').respond(
            profile_data=profile.SerializeToString(), is_binary_data=True
        )

    @skip('deleted old booster')
    def test_clothes_boosting_from_dj_profile(self):
        """
        Тестируем, что просходит загрузка еком-профиля из Dj. Он используется для бустинга тех офферов, у которых размер
        совпадает с размером одежды пользователя из этого самого профиля
        """

        # если одежда и не выставлены флаги, то ничего не бустим. Побеждает XS, потому что у него значение матрикснет меньше
        for rearr in ('', ';fetch_recom_profile_for_model_place=0'):
            request = 'place=also_viewed&rearr-factors=split=normal{}&hyperid=10&yandexuid=011&debug=1'.format(rearr)
            response = self.report.request_json(request)
            self.assertFragmentIn(
                response,
                {
                    "search": {
                        "results": [
                            {
                                "id": 11,
                                "offers": {
                                    "items": [
                                        {
                                            "titles": {"raw": "Платье женское Baon размер XS"},
                                        }
                                    ]
                                },
                            }
                        ]
                    }
                },
            )

        # если одежда и выставлены флаги, то побеждает оффер размера S, потому что его бустят
        # (потому что он указан в еком профиле)
        rearr_factors = [
            'market_boost_single_personal_gl_param_coeff=1.2',
            'fetch_recom_profile_for_model_place=1',
            'split=normal',
        ]
        request = 'place=also_viewed&rearr-factors={}&hyperid=10&yandexuid=011'.format(';'.join(rearr_factors))
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {
                            "id": 11,
                            "offers": {
                                "items": [
                                    {
                                        "titles": {"raw": "Платье женское Baon размер S"},
                                    }
                                ]
                            },
                        }
                    ]
                }
            },
        )

        # если не одежда - бустинга не происходит, что с флагами, что без
        for rearr in (
            "",
            "&rearr-factors=market_boost_single_personal_gl_param_coeff=1.2;fetch_recom_profile_for_model_place=1",
            "&rearr-factors=market_boost_single_personal_gl_param_coeff=1.2;fetch_recom_profile_for_model_place=0",
        ):
            response = self.report.request_json(
                'place=also_viewed&rearr-factors=split=normal{}&hyperid=12&yandexuid=011'.format(rearr)
            )
            self.assertFragmentIn(response, {"titles": {"raw": "Не одежда (hyperid = 13) 15"}})

    @classmethod
    def prepare_sku_for_serp_boost_test(cls):
        cls.index.models += [
            Model(hyperid=21, hid=7812186),
            Model(hyperid=22, hid=7811945),
            Model(hyperid=23, hid=11111),
            Model(hyperid=24, hid=11111),
        ]

        cls.index.mskus += [
            MarketSku(
                hid=7812186,
                hyperid=21,
                sku=180,
                title="MSKU-180",
                blue_offers=[BlueOffer(price=170, fesh=13)],
            ),
            MarketSku(
                hid=7812186,
                hyperid=21,
                sku=1811,
                title="MSKU-180",
                blue_offers=[BlueOffer(price=170, fesh=13)],
            ),
            MarketSku(
                hid=7811945,
                hyperid=22,
                sku=181,
                title="MSKU-181",
                blue_offers=[BlueOffer(price=171, fesh=13)],
            ),
            MarketSku(
                hid=11111,
                hyperid=23,
                sku=182,
                title="MSKU-182",
                blue_offers=[BlueOffer(price=172, fesh=13)],
            ),
            MarketSku(
                hid=101,
                hyperid=1,
                sku=28658004,
                title="MSKU-180",
                blue_offers=[BlueOffer(price=170, fesh=13)],
            ),
            MarketSku(
                hid=101,
                hyperid=1,
                sku=28658003,
                title="MSKU-180",
                blue_offers=[BlueOffer(price=170, fesh=13)],
            ),
        ]

    def test_serp_from_rs_boost(self):
        """
        https://st.yandex-team.ru/MARKETRECOM-4915
        """
        rs = ReportState.create()
        for m, s in [(21, 180), (22, 181), (1, 28658003), (1, 28658004), (23, 182), (21, 1811)]:
            doc = rs.offer_card_state.also_viewed_docs.add()
            doc.model_id = str(m)
            doc.sku_id = str(s)
        rs_serialized = ReportState.serialize(rs).replace('=', ',')
        response = self.report.request_json(
            'place=also_viewed&rearr-factors=split=normal;also-viewed-serp-everything-except-self=1&hyperid=1&market-sku=28658004&rs={}'.format(
                rs_serialized
            )
        )
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'total': 8,
                    'results': [
                        {'entity': 'product', 'id': 21, "offers": {"items": [{"sku": "180"}]}},
                        {'entity': 'product', 'id': 22},
                        {'entity': 'product', 'id': 1, "offers": {"items": [{"sku": "28658003"}]}},
                        {'entity': 'product', 'id': 23},
                        {'entity': 'product', 'id': 4},
                        {'entity': 'product', 'id': 2},
                        {'entity': 'product', 'id': 3},
                        {'entity': 'product', 'id': 5},
                    ],
                }
            },
            preserve_order=True,
        )
        response = self.report.request_json(
            'place=also_viewed&rearr-factors=split=normal;also-viewed-serp-post-self=1&hyperid=1&market-sku=28658004&rs={}'.format(
                rs_serialized
            )
        )
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'total': 6,
                    'results': [
                        {'entity': 'product', 'id': 23},
                        {'entity': 'product', 'id': 21, "offers": {"items": [{"sku": "1811"}]}},
                        {'entity': 'product', 'id': 4},
                        {'entity': 'product', 'id': 2},
                        {'entity': 'product', 'id': 3},
                        {'entity': 'product', 'id': 5},
                    ],
                }
            },
            preserve_order=True,
        )

        # without sku
        response = self.report.request_json(
            'place=also_viewed&rearr-factors=split=normal;also-viewed-serp-post-self=1&hyperid=1&rs={}'.format(
                rs_serialized
            )
        )
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
    def prepare_visual_analogue_mix(cls):
        """чтобы работал place=dj"""
        cls.settings.set_default_reqid = False
        """если нет модели, то выдача dj будет пустой"""
        cls.index.models += [Model(hyperid=model_id, hid=101) for model_id in MODEL_IDS_FOR_VISUAL_SEARCH]
        cls.index.mskus += [
            MarketSku(hyperid=model_id, sku=MSKU_SHIFT + model_id) for model_id in MODEL_IDS_FOR_VISUAL_SEARCH
        ]
        cls.index.offers += [Offer(hyperid=model_id) for model_id in MODEL_IDS_FOR_VISUAL_SEARCH]
        cls.dj.on_request(market_sku=MSKU_FOR_VISUAL_ANALOGUE_MIX).respond(
            [DjModel(id=model_id) for model_id in MODEL_IDS_FOR_VISUAL_SEARCH]
        )

    def base_results_list(self):
        return [
            {'entity': 'product', 'id': 4},
            {'entity': 'product', 'id': 2},
            {'entity': 'product', 'id': 3},
            {'entity': 'product', 'id': 5},
        ]

    def test_visual_analogue_mix_on(self):
        """
        Проверяем, что при включении подмешивания визуально похожих в ответ добавляются модели из DJ
        https://st.yandex-team.ru/MARKETYA-737
        """
        base_request = (
            'place=also_viewed&rearr-factors=split=normal'
            + '&hyperid=1'
            + '&market-sku=%d' % MSKU_FOR_VISUAL_ANALOGUE_MIX
            + '&hid=101'
            + '&rearr-factors=market_visual_search_departments=101'
            + '&visual-analogue-mix=1'
            + '&rearr-factors=market_visual_similar_for_analogue_mix_count='
        )

        def results_for_dj_models_count(count):
            dj_models_list = [{'entity': 'product', 'id': id} for id in range(415, 415 + count)]
            return dj_models_list

        for market_visual_similar_for_analogue_mix_count in [2, 4]:
            response = self.report.request_json(base_request + str(market_visual_similar_for_analogue_mix_count))
            self.assertFragmentIn(
                response,
                {
                    'search': {
                        'total': 4 + market_visual_similar_for_analogue_mix_count,
                        'results': results_for_dj_models_count(market_visual_similar_for_analogue_mix_count),
                    }
                },
                preserve_order=True,
            )
            self.assertFragmentIn(response, {'search': {'results': self.base_results_list()}}, preserve_order=True)

    def test_visual_analogue_mix_not_enough_models(self):
        """
        Проверяем, что если нужное число моделей на DJ не набралось, то ничего не добавляется
        https://st.yandex-team.ru/MARKETYA-737
        """
        base_request = (
            'place=also_viewed&rearr-factors=split=normal'
            + '&hyperid=1'
            + '&market-sku=%d' % MSKU_FOR_VISUAL_ANALOGUE_MIX
            + '&hid=101'
            + '&rearr-factors=market_visual_similar_for_analogue_mix_count=999'
            + '&rearr-factors=market_visual_search_departments=101'
        )
        response = self.report.request_json(base_request + '&visual-analogue-mix=1')
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'total': 4,
                    'results': self.base_results_list(),
                }
            },
            preserve_order=True,
        )

    def test_visual_analogue_mix_off(self):
        """
        Проверяем, что
        а) без включения подмешивания
        или
        б) для категорий не fashion и не указанных в market_visual_search_departments
        модели из DJ к ответу не добавляются
        https://st.yandex-team.ru/MARKETYA-737
        """
        base_request = (
            'place=also_viewed&rearr-factors=split=normal'
            + '&hyperid=1'
            + '&market-sku=%d' % MSKU_FOR_VISUAL_ANALOGUE_MIX
            + '&hid=101'
            + '&rearr-factors=market_visual_similar_for_analogue_mix_count=2'
        )
        for request in [
            base_request + '&visual-analogue-mix=0&rearr-factors=market_visual_search_departments=101',
            base_request + '&visual-analogue-mix=1',
        ]:
            response = self.report.request_json(request)
            self.assertFragmentIn(
                response,
                {
                    'search': {
                        'total': 4,
                        'results': self.base_results_list(),
                    }
                },
                preserve_order=True,
            )

    def test_visual_analogue_mix_paging(self):
        """
        Проверяем работу пейджинга
        https://st.yandex-team.ru/MARKETYA-737
        """
        request_base = (
            'place=also_viewed&rearr-factors=split=normal'
            + '&hyperid=1'
            + '&market-sku=%d' % MSKU_FOR_VISUAL_ANALOGUE_MIX
            + '&hid=101'
            + '&rearr-factors=market_visual_search_departments=101'
            + '&visual-analogue-mix=1'
            + '&rearr-factors=market_visual_similar_for_analogue_mix_count=2'
        )
        response = self.report.request_json(request_base + '&on-page=3&page-no=1')
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'total': 6,
                    'results': [
                        {'entity': 'product', 'id': 415},
                        {'entity': 'product', 'id': 416},
                        {'entity': 'product', 'id': 4},
                    ],
                }
            },
            preserve_order=True,
        )
        self.assertFragmentNotIn(response, {'entity': 'product', 'id': 2})
        self.assertFragmentNotIn(response, {'entity': 'product', 'id': 3})
        self.assertFragmentNotIn(response, {'entity': 'product', 'id': 5})

        response = self.report.request_json(request_base + '&on-page=3&page-no=2')
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'total': 6,
                    'results': [
                        {'entity': 'product', 'id': 2},
                        {'entity': 'product', 'id': 3},
                        {'entity': 'product', 'id': 5},
                    ],
                }
            },
            preserve_order=True,
        )
        self.assertFragmentNotIn(response, {'entity': 'product', 'id': 415})
        self.assertFragmentNotIn(response, {'entity': 'product', 'id': 416})
        self.assertFragmentNotIn(response, {'entity': 'product', 'id': 4})

    @classmethod
    def prepare_also_viewed_filtrations(cls):
        cls.index.yamarec_places += [
            YamarecPlace(
                name=YamarecPlace.Name.ALSO_VIEWED_BEST_PRICE,
                kind=YamarecPlace.Type.SETTING,
                split_rule=YamarecPlace.SplitRule.ABT,
                partitions=[
                    YamarecSettingPartition(params={'version': 'ANALOGS2_BEST_PRICE'}, splits=[{}]),
                ],
            ),
            YamarecPlace(
                name=YamarecPlace.Name.ALSO_VIEWED_BEST_RATING,
                kind=YamarecPlace.Type.SETTING,
                split_rule=YamarecPlace.SplitRule.ABT,
                partitions=[
                    YamarecSettingPartition(params={'version': 'ANALOGS2_BEST_RATING'}, splits=[{}]),
                ],
            ),
            YamarecPlace(
                name=YamarecPlace.Name.ALSO_VIEWED_EXPRESS,
                kind=YamarecPlace.Type.SETTING,
                split_rule=YamarecPlace.SplitRule.ABT,
                partitions=[
                    YamarecSettingPartition(params={'version': 'ANALOGS2_EXPRESS'}, splits=[{}]),
                ],
            ),
        ]
        cls.recommender.on_request_accessory_models(model_id=1, item_count=1000, version='ANALOGS2_BEST_PRICE').respond(
            {'models': ['2', '3', '4']}
        )
        cls.recommender.on_request_accessory_models(
            model_id=1, item_count=1000, version='ANALOGS2_BEST_RATING'
        ).respond({'models': ['3']})
        cls.recommender.on_request_accessory_models(model_id=1, item_count=1000, version='ANALOGS2_EXPRESS').respond(
            {'models': ['4']}
        )

    def do_test_also_viewed_filtration(self, yamarec_place, additional_params, recommended_model):
        response = self.report.request_json(
            'place=also_viewed&hyperid=1&debug=1&yamarec-place-id={}'.format(yamarec_place) + additional_params
        )
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'total': 1,
                    'results': [
                        {
                            'entity': 'product',
                            'id': recommended_model,
                        }
                    ],
                }
            },
        )

    def test_also_viewed_best_price(self):
        self.do_test_also_viewed_filtration("also-viewed-best-price", "&offerid=DEFAULTA1AAAAAAAAAAAAA", 2)

    def test_also_viewed_best_rating(self):
        self.do_test_also_viewed_filtration("also-viewed-best-rating", "", 3)

    def test_also_viewed_express_incorrect_cgi(self):
        response = self.report.request_json(
            'place=also_viewed&hyperid=1&yamarec-place-id=also-viewed-express', strict=False
        )
        self.assertFragmentIn(
            response,
            {
                'error': {
                    'code': 'INVALID_USER_CGI',
                    'message': 'filter-express-delivery=1 parameter is required with yamarec-place=id=also-viewed-express',
                }
            },
        )

    def test_also_viewed_best_price_incorrect_cgi(self):
        response = self.report.request_json(
            'place=also_viewed&hyperid=1&yamarec-place-id=also-viewed-best-price', strict=False
        )
        self.assertFragmentIn(
            response,
            {
                'error': {
                    'code': 'INVALID_USER_CGI',
                    'message': 'offerid parameter with exactly one offer is required with yamarec-place=id=also-viewed-best-price',
                }
            },
        )

    def test_also_viewed_best_price_incorrect_offerid(self):
        response = self.report.request_json(
            'place=also_viewed&hyperid=1&yamarec-place-id=also-viewed-best-price&offerid=abacaba', strict=False
        )
        self.assertFragmentIn(
            response,
            {
                'error': {
                    'code': 'EMPTY_REQUEST',
                }
            },
        )

    @classmethod
    def prepare_top_competitive_filtration(cls):
        cls.settings.memcache_enabled = True
        cls.recommender.on_request_accessory_models(model_id=1, item_count=1000, version='SIBLINGS1_AUGMENTED').respond(
            {'models': ['4:0.1', '2:0.2', '3:0.3', '5:0.44']}
        )

    def test_also_viewed_top_competitive_filtration(self):
        _rearr_factors = (
            "&rearr-factors="
            "market_competitive_model_card_do_not_bill=0"
            ";market_competitive_model_card_closeness_threshold=10"
            ";split=empty"
            ";market_competitive_model_card_disable_remove_empty_do=1"
            ";also-viewed-filter-competitive-model-card=1"
        )

        request = 'place=competitive_model_card' + _rearr_factors + '&hyperid=1&debug=1&yandexuid=007&dj-cacher-ttl=10'
        response = self.report.request_json(request)
        self.assertFragmentIn(response, {"modelId": 5})

        request = 'place=also_viewed&rearr-factors=split=normal&hyperid=1&yandexuid=007&rearr-factors=also-viewed-filter-competitive-model-card=1'
        response = self.report.request_json(request)

        # if the 10-seconds time interval got broken we just try another request
        if response['search']['total'] != 3:
            request = 'place=competitive_model_card' + _rearr_factors + '&hyperid=1&debug=1&yandexuid=007'
            response = self.report.request_json(request)
            request = 'place=also_viewed&rearr-factors=split=normal&hyperid=1&yandexuid=007&rearr-factors=also-viewed-filter-competitive-model-card=1'
            response = self.report.request_json(request)
            self.assertFragmentIn(response, {'total': 3})

    @classmethod
    def prepare_top_competitive_filtration_max_filtered_6(cls):
        cls.index.models += [
            Model(hyperid=70, ts=70, hid=107, vendor_id=71, vbid=100),  # vendorFee = 6000 (60%)
            Model(hyperid=71, ts=71, hid=107, vendor_id=72, vbid=80),  # vendorFee = 4800 (48%)
            Model(hyperid=72, ts=72, hid=107, vendor_id=73, vbid=70),  # vendorFee = 4200 (42%)
            Model(hyperid=73, ts=73, hid=107, vendor_id=73, vbid=60),  # vendorFee = 3600 (36%)
            Model(hyperid=74, ts=74, hid=107, vendor_id=73, vbid=50),  # vendorFee = 3000 (30%)
            Model(hyperid=75, ts=75, hid=107, vendor_id=75, vbid=40),  # vendorFee = 2400 (24%)
            Model(hyperid=76, ts=76, hid=107, vendor_id=77, vbid=6),  # vendorFee = 360 (3.6%)
            Model(hyperid=77, ts=77, hid=107, vendor_id=77, vbid=5),  # vendorFee = 300 (3%)
        ]

        cls.index.offers += [
            Offer(hyperid=70, fesh=14, fee=1000, price=1000, waremd5='RcSMzi4xxxxqGvxRx8atJg'),
            Offer(hyperid=71, fesh=14, fee=500, price=1000),
            Offer(hyperid=72, fesh=14, fee=0, price=1000),
            Offer(hyperid=73, fesh=14, fee=0, price=1000),
            Offer(hyperid=74, fesh=14, fee=100, price=1000),
            Offer(hyperid=75, fesh=14, fee=0, price=1000),
            Offer(hyperid=76, fesh=15, fee=100, price=1000),
            Offer(hyperid=77, fesh=14, fee=500, price=1000),
        ]

        cls.recommender.on_request_accessory_models(model_id=2, item_count=1000, version='SIBLINGS1_AUGMENTED').respond(
            {'models': ['70:0.5', '71:0.5', '72:0.5', '73:0.5', '74:0.5', '75:0.5', '76:0.5', '77:0.5']}
        )

        cls.recommender.on_request_accessory_models(model_id=2, item_count=1000, version='1').respond(
            {'models': ['70', '71', '72', '73', '74', '75', '76', '77']}
        )

    def test_also_viewed_top_competitive_filtration_max_filtered_6(self):
        _rearr_factors = (
            "&rearr-factors="
            "market_competitive_model_card_do_not_bill=0"
            ";market_competitive_model_card_closeness_threshold=10"
            ";split=empty"
            ";market_competitive_model_card_disable_remove_empty_do=1"
            ";also-viewed-filter-competitive-model-card=1"
        )

        request = (
            'place=competitive_model_card'
            + _rearr_factors
            + '&hyperid=2&debug=1&yandexuid=007&numdoc=10&dj-cacher-ttl=30'
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(response, {"results": ElementCount(8)})

        request = 'place=also_viewed&rearr-factors=split=normal&hyperid=2&yandexuid=007&rearr-factors=also-viewed-filter-competitive-model-card=1'
        response = self.report.request_json(request)

        # if the 30-seconds time interval got broken we just try another request
        if response['search']['total'] != 2:
            request = (
                'place=competitive_model_card'
                + _rearr_factors
                + '&hyperid=2&debug=1&yandexuid=007&numdoc=10&dj-cacher-ttl=30'
            )
            response = self.report.request_json(request)
            request = 'place=also_viewed&rearr-factors=split=normal&hyperid=2&yandexuid=007&rearr-factors=also-viewed-filter-competitive-model-card=1'
            response = self.report.request_json(request)
            self.assertFragmentIn(response, {'total': 2})


if __name__ == '__main__':
    main()
