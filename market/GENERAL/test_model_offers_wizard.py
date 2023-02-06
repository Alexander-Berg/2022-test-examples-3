#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    BlueOffer,
    CategoryRestriction,
    DeliveryBucket,
    DeliveryOption,
    Disclaimer,
    HyperCategory,
    MarketSku,
    MnPlace,
    Model,
    NavCategory,
    Offer,
    Opinion,
    RegionalDelivery,
    RegionalRestriction,
    Shop,
)
from core.testcase import TestCase, main

from core.matcher import LikeUrl, Contains, Absent


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.index.hypertree += [HyperCategory(hid=1, name='Root Category')]
        cls.index.navtree += [NavCategory(nid=1, hid=1)]
        cls.index.shops += [
            Shop(fesh=1, name="shop 1", priority_region=213),
            Shop(
                fesh=431782,
                fulfillment_virtual=True,
                virtual_shop_color=Shop.VIRTUAL_SHOP_BLUE,
                priority_region=213,
                name='Яндекс.Маркет',
            ),
        ]
        cls.index.delivery_buckets += [
            DeliveryBucket(
                bucket_id=101,
                fesh=431782,
                carriers=[1, 3],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
                regional_options=[
                    RegionalDelivery(rid=213, options=[DeliveryOption(price=500, day_from=1, day_to=3)]),
                ],
            ),
        ]
        cls.index.models += [
            Model(hid=1, hyperid=101, ts=101, title="Magic Model 1", opinion=Opinion(total_count=10)),
            Model(hid=1, hyperid=102, ts=102, title="Magic Model 2"),
            Model(hid=1, hyperid=103, ts=103, title="Magic Model 3"),
            Model(hid=1, hyperid=104, ts=104, title="Magic Model 4"),
            Model(hid=1, hyperid=105, ts=105, title="Simple Model 5"),
            Model(hid=1, hyperid=106, ts=106, title="Magic Model 6"),  # no offers
            Model(hid=1, hyperid=107, ts=107, title="Blue Model 7"),
        ]
        cls.index.offers += [
            Offer(hid=1, ts=121, title='Magic Offer 1', price=100, hyperid=101),
            Offer(hid=1, ts=122, title='Magic Offer 2', price=100, hyperid=102),
            Offer(hid=1, ts=123, title='Magic Offer 3', price=100, hyperid=103),
            Offer(hid=1, ts=124, title='Simple Offer 4', price=100, hyperid=104),
            Offer(hid=1, ts=125, title='Magic Offer 5', price=100, hyperid=105),
            Offer(
                hid=1, ts=126, title='Magic Offer 6', price=100, fesh=1, price_old=110, waremd5='JXXIV9-YOZbUlVDz6SR1Xw'
            ),  # no model
        ]

        cls.index.mskus += [
            MarketSku(
                sku=1070,
                title="blue 1",
                hyperid=107,
                blue_offers=[
                    BlueOffer(ts=100101, waremd5='gTL-3D5IXpiHAL-CvNRmNQ', feedid=431782, delivery_buckets=[101])
                ],
            ),
        ]

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 101).respond(0.95)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 102).respond(0.85)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 103).respond(0.75)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 104).respond(0.65)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 105).respond(0.55)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 106).respond(0.45)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 107).respond(0.55)

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 121).respond(0.9)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 122).respond(0.8)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 123).respond(0.9)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 124).respond(0.6)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 125).respond(0.5)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 126).respond(0.4)

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 100101).respond(0.55)

    def test_model_offers_wizard(self):
        """https://st.yandex-team.ru/MARKETOUT-31135"""
        request = (
            'place=parallel&text=magic&rearr-factors=market_enable_model_offers_wizard=1;'
            'market_implicit_model_sitelink_reviews=1;'
            'market_implicit_model_sitelink_map=1;'
            'market_implicit_model_sitelink_next_day_delivery=1;'
        )
        response = self.report.request_bs_pb(request)

        # test common format
        self.assertFragmentIn(
            response,
            {
                'market_model_offers_wizard': {
                    'title': '\7[Magic\7]',
                    'url': LikeUrl.of(
                        '//market.yandex.ru/search?lr=0&text=magic&clid=545&utm_medium=cpc&utm_referrer=wizards',
                        ignore_len=False,
                        ignore_params=['rs'],
                    ),
                    'urlTouch': LikeUrl.of(
                        '//m.market.yandex.ru/search?lr=0&text=magic&clid=708&utm_medium=cpc&utm_referrer=wizards',
                        ignore_len=False,
                        ignore_params=['rs'],
                    ),
                    'text': [{'__hl': {'text': 'Цены, характеристики, отзывы на magic. Выбор по параметрам.'}}],
                    'sitelinks': [
                        {
                            'url': '//market.yandex.ru/search?show-reviews=1&text=magic&lr=0&utm_medium=cpc&utm_referrer=wizards&clid=545',
                            'urlTouch': '//m.market.yandex.ru/search?show-reviews=1&text=magic&lr=0&utm_medium=cpc&utm_referrer=wizards&clid=708',
                            'text': 'Отзывы',
                        },
                        {
                            'url': '//market.yandex.ru/geo?text=magic&lr=0&utm_medium=cpc&utm_referrer=wizards&clid=545',
                            'urlTouch': '//m.market.yandex.ru/geo?text=magic&lr=0&utm_medium=cpc&utm_referrer=wizards&clid=708',
                            'text': 'На карте',
                        },
                        {
                            'url': '//market.yandex.ru/search?delivery-interval=1&text=magic&lr=0&utm_medium=cpc&utm_referrer=wizards&clid=545',
                            'urlTouch': '//m.market.yandex.ru/search?delivery-interval=1&text=magic&lr=0&utm_medium=cpc&utm_referrer=wizards&clid=708',
                            'text': 'С доставкой завтра',
                        },
                    ],
                    'greenUrl': [
                        {
                            'text': 'Magic',
                            'url': LikeUrl.of(
                                '//market.yandex.ru/search?lr=0&text=magic&clid=545&utm_medium=cpc&utm_referrer=wizards',
                                ignore_len=False,
                                ignore_params=['rs'],
                            ),
                            'urlTouch': LikeUrl.of(
                                '//m.market.yandex.ru/search?lr=0&text=magic&clid=708&utm_medium=cpc&utm_referrer=wizards',
                                ignore_len=False,
                                ignore_params=['rs'],
                            ),
                        }
                    ],
                }
            },
        )

        # test format of docs
        self.assertFragmentIn(
            response,
            {
                'market_model_offers_wizard': {
                    'showcase': {
                        'items': [
                            {
                                'categoryId': 1,
                                'title': {
                                    'text': {'__hl': {'text': 'Magic Model 1'}},
                                    'url': LikeUrl.of(
                                        '//market.yandex.ru/product--magic-model-1/101?hid=1&lr=0&nid=1&text=magic&utm_medium=cpc&utm_referrer=wizards&clid=545'
                                    ),
                                    'urlTouch': LikeUrl.of(
                                        '//m.market.yandex.ru/product--magic-model-1/101?hid=1&lr=0&nid=1&text=magic&utm_medium=cpc&utm_referrer=wizards&clid=708'
                                    ),
                                },
                                'thumb': {
                                    'source': '//mdata.yandex.net/i?path=b0130135356_img_id2520674011472212068.jpg&size=2',
                                    'retinaSource': '//mdata.yandex.net/i?path=b0130135356_img_id2520674011472212068.jpg&size=5',
                                    'url': LikeUrl.of(
                                        '//market.yandex.ru/product--magic-model-1/101?hid=1&lr=0&nid=1&text=magic&utm_medium=cpc&utm_referrer=wizards&clid=545'
                                    ),
                                    'urlTouch': LikeUrl.of(
                                        '//m.market.yandex.ru/product--magic-model-1/101?hid=1&lr=0&nid=1&text=magic&utm_medium=cpc&utm_referrer=wizards&clid=708'
                                    ),
                                },
                                'price': {
                                    'currency': 'RUR',
                                    'priceMin': '100',
                                },
                                'reviews': {
                                    'count': '10',
                                    'url': LikeUrl.of(
                                        '//market.yandex.ru/product--magic-model-1/101/reviews?lr=0&text=magic&utm_medium=cpc&utm_referrer=wizards&clid=545'
                                    ),
                                    'urlTouch': LikeUrl.of(
                                        '//m.market.yandex.ru/product--magic-model-1/101/reviews?lr=0&text=magic&utm_medium=cpc&utm_referrer=wizards&clid=708'
                                    ),
                                },
                                'offersCount': 1,
                                'documentType': 'model',
                            },
                            {
                                'title': {
                                    'text': {'__hl': {'text': 'Magic Offer 6'}},
                                    'url': LikeUrl.of(
                                        '//market.yandex.ru/search?cvredirect=0&lr=0&text=magic&utm_medium=cpc&utm_referrer=wizards&utm_medium=cpc&utm_referrer=wizards&clid=545',
                                        ignore_len=False,
                                        ignore_params=['rs'],
                                    ),
                                },
                                'thumb': {
                                    'source': 'http://avatars.mdst.yandex.net/get-marketpic/1/market_iyC3nHslqLtqZJLygVAHeA/100x100',
                                    'retinaSource': 'http://avatars.mdst.yandex.net/get-marketpic/1/market_iyC3nHslqLtqZJLygVAHeA/100x100',
                                    'url': LikeUrl.of(
                                        '//market.yandex.ru/search?cvredirect=0&lr=0&text=magic&utm_medium=cpc&utm_referrer=wizards&utm_medium=cpc&utm_referrer=wizards&clid=545',
                                        ignore_len=False,
                                        ignore_params=['rs'],
                                    ),
                                },
                                'price': {
                                    'currency': 'RUR',
                                    'priceMin': '100',
                                },
                                'discount': {
                                    'percent': '9',
                                    'oldprice': '110',
                                },
                                'greenUrl': {
                                    'text': 'shop 1',
                                    'url': LikeUrl.of(
                                        '//market.yandex.ru/shop--shop-1/1?lr=0&utm_medium=cpc&utm_referrer=wizards&clid=545',
                                        ignore_len=False,
                                        ignore_params=['cmid'],
                                    ),
                                    'urlTouch': LikeUrl.of(
                                        '//m.market.yandex.ru/shop--shop-1/1?lr=0&utm_medium=cpc&utm_referrer=wizards&clid=708',
                                        ignore_len=False,
                                        ignore_params=['cmid'],
                                    ),
                                },
                                'documentType': 'offer',
                            },
                        ]
                    }
                }
            },
        )

        # test collapsing and ranking
        response = self.report.request_bs_pb(request + 'market_model_offers_wizard_use_offer_factors=0;')
        self.assertFragmentIn(
            response,
            {
                'market_model_offers_wizard': {
                    'showcase': {
                        'items': [
                            {'title': {'text': {'__hl': {'text': 'Magic Model 1'}}}},
                            {'title': {'text': {'__hl': {'text': 'Magic Model 2'}}}},
                            {'title': {'text': {'__hl': {'text': 'Magic Model 3'}}}},
                            {'title': {'text': {'__hl': {'text': 'Magic Model 4'}}}},
                            {'title': {'text': {'__hl': {'text': 'Simple Model 5'}}}},
                            {'title': {'text': {'__hl': {'text': 'Magic Offer 6'}}}},
                        ]
                    }
                }
            },
            allow_different_len=False,
            preserve_order=True,
        )

        response = self.report.request_bs_pb(request + 'market_model_offers_wizard_use_offer_factors=1;')
        self.assertFragmentIn(
            response,
            {
                'market_model_offers_wizard': {
                    'showcase': {
                        'items': [
                            {'title': {'text': {'__hl': {'text': 'Magic Model 1'}}}},
                            {'title': {'text': {'__hl': {'text': 'Magic Model 3'}}}},
                            {'title': {'text': {'__hl': {'text': 'Magic Model 2'}}}},
                            {'title': {'text': {'__hl': {'text': 'Magic Model 4'}}}},
                            {'title': {'text': {'__hl': {'text': 'Simple Model 5'}}}},
                            {'title': {'text': {'__hl': {'text': 'Magic Offer 6'}}}},
                        ]
                    }
                }
            },
            allow_different_len=False,
            preserve_order=True,
        )

    def test_category_name_in_green_url(self):
        """https://st.yandex-team.ru/MARKETOUT-35915"""
        request = 'place=parallel&text=magic&rearr-factors=market_enable_model_offers_wizard=1;'

        response = self.report.request_bs_pb(request)
        self.assertFragmentIn(response, {'market_model_offers_wizard': {'greenUrl': [{'text': 'Magic'}]}})

        response = self.report.request_bs_pb(request + "market_model_offers_wizard_use_category_in_green_url=1")
        self.assertFragmentIn(response, {'market_model_offers_wizard': {'greenUrl': [{'text': 'Root Category'}]}})

    def test_model_offers_offer_url(self):
        """Проверяем отображение урла на КО для офферов модельно-офферного
        https://st.yandex-team.ru/MARKETOUT-35568
        """
        # NailedInSearch
        request = 'place=parallel&text=magic&rearr-factors=market_enable_model_offers_wizard=1;market_model_offers_wizard_incut_url_type=NailedInSearch;'
        response = self.report.request_bs_pb(request)
        self.assertFragmentIn(
            response,
            {
                'market_model_offers_wizard': {
                    'showcase': {
                        'items': [
                            {
                                'title': {
                                    'text': {'__hl': {'text': 'Magic Offer 6'}},
                                    'url': LikeUrl.of(
                                        '//market.yandex.ru/search?cvredirect=0&lr=0&text=magic&clid=545'
                                    ),
                                }
                            }
                        ]
                    }
                }
            },
        )

        response = self.report.request_bs_pb(request + '&touch=1&rearr-factors=device=touch')
        self.assertFragmentIn(
            response,
            {
                'market_model_offers_wizard': {
                    'showcase': {
                        'items': [
                            {
                                'title': {
                                    'text': {'__hl': {'text': 'Magic Offer 6'}},
                                    'urlTouch': LikeUrl.of(
                                        '//m.market.yandex.ru/search?cvredirect=0&lr=0&text=magic&clid=708'
                                    ),
                                }
                            }
                        ]
                    }
                }
            },
        )

        # OfferCard
        request = 'place=parallel&text=magic&rearr-factors=market_enable_model_offers_wizard=1;market_model_offers_wizard_incut_url_type=OfferCard;'
        response = self.report.request_bs_pb(request)
        self.assertFragmentIn(
            response,
            {
                'market_model_offers_wizard': {
                    'showcase': {
                        'items': [
                            {
                                'title': {
                                    'text': {'__hl': {'text': 'Magic Offer 6'}},
                                    'url': Contains('//market-click2.yandex.ru/redir/dtype=offercard/'),
                                }
                            }
                        ]
                    }
                }
            },
        )

        response = self.report.request_bs_pb(request + '&touch=1&rearr-factors=device=touch')
        self.assertFragmentIn(
            response,
            {
                'market_model_offers_wizard': {
                    'showcase': {
                        'items': [
                            {
                                'title': {
                                    'text': {'__hl': {'text': 'Magic Offer 6'}},
                                    'urlTouch': Contains('//market-click2.yandex.ru/redir/dtype=offercard/'),
                                }
                            }
                        ]
                    }
                }
            },
        )

    def test_cpa_filter_in_title_url(self):
        """Проверяем что под флагом market_cpa_filter_in_wizard_title_urls=1 в ссылку тайтла добавляется фильтр CPA
        https://st.yandex-team.ru/MARKETOUT-36793
        https://st.yandex-team.ru/MARKETOUT-37552
        """
        query = 'place=parallel&text=magic&rearr-factors=market_enable_model_offers_wizard=1;market_cpa_filter_in_wizard_title_urls=1;'
        response = self.report.request_bs_pb(query)
        self.assertFragmentIn(
            response,
            {
                "market_model_offers_wizard": {
                    "url": LikeUrl.of("//market.yandex.ru/search?text=magic&lr=0&clid=545&cpa=1"),
                    "urlTouch": LikeUrl.of("//m.market.yandex.ru/search?text=magic&lr=0&clid=708&cpa=1"),
                }
            },
        )

        # Под флагом market_cpa_filter_in_wizard_title_urls_vt=market_model_offers_wizard CPA фильтр добавляется
        response = self.report.request_bs_pb(
            query + 'market_cpa_filter_in_wizard_title_urls_vt=market_model_offers_wizard'
        )
        self.assertFragmentIn(
            response,
            {
                "market_model_offers_wizard": {
                    "url": LikeUrl.of("//market.yandex.ru/search?text=magic&lr=0&clid=545&cpa=1"),
                    "urlTouch": LikeUrl.of("//m.market.yandex.ru/search?text=magic&lr=0&clid=708&cpa=1"),
                }
            },
        )

        # Без флага market_cpa_filter_in_wizard_title_urls_vt=market_model_offers_wizard CPA фильтр не добавляется
        response = self.report.request_bs_pb(query + 'market_cpa_filter_in_wizard_title_urls_vt=market_offers_wizard')
        self.assertFragmentIn(
            response,
            {
                "market_model_offers_wizard": {
                    "url": LikeUrl.of("//market.yandex.ru/search?text=magic&lr=0&clid=545", no_params=['cpa']),
                    "urlTouch": LikeUrl.of("//m.market.yandex.ru/search?text=magic&lr=0&clid=708", no_params=['cpa']),
                }
            },
        )

    @classmethod
    def prepare_models_for_title_no_vendor_no_category(cls):
        cls.index.models += [
            Model(hyperid=1, title=' Смартфон Apple iPhone 7 64GB', title_no_vendor='Смартфон iPhone 7 64GB'),
            Model(
                hyperid=2, title='xxxxxx yyyxxx yyyyyy iPhone 8 64GB ', title_no_vendor=' xxxxxx yyyyyy iPhone 8 64GB '
            ),
            Model(hyperid=3, title='Смартфон Apple iPhone 9 64GB'),
        ]
        cls.index.offers += [
            Offer(title='apple 1', hyperid=1, price=10),
            Offer(title='apple 2', hyperid=2, price=20),
            Offer(title='apple 3', hyperid=3, price=30),
        ]

    def test_model_title_no_vendor_no_category(self):
        """https://st.yandex-team.ru/MARKETOUT-29030
        Check how works deleting category and vendor from titles in imlicit model wizard
        """

        request = 'place=parallel&text=iphone&rearr-factors=market_enable_model_offers_wizard=1;'
        withCategory = 'market_model_offers_wizard_title_no_category=0;'
        noVendor = 'market_model_offers_wizard_title_no_vendor=1;'

        # Проставляем market_model_offers_wizard_right_incut_title_no_category, так как он по умолчанию включен
        response = self.report.request_bs_pb(request + withCategory)
        self.assertFragmentIn(
            response,
            {
                "market_model_offers_wizard": {
                    "showcase": {
                        "items": [
                            {"title": {"text": {"__hl": {"text": "Смартфон Apple iPhone 7 64GB", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "xxxxxx yyyxxx yyyyyy iPhone 8 64GB", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "Смартфон Apple iPhone 9 64GB", "raw": True}}}},
                        ]
                    }
                }
            },
        )

        response = self.report.request_bs_pb(request)  # market_model_offers_wizard_right_incut_title_no_category=1
        self.assertFragmentIn(
            response,
            {
                "market_model_offers_wizard": {
                    "showcase": {
                        "items": [
                            {"title": {"text": {"__hl": {"text": "Apple iPhone 7 64GB", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "yyyxxx yyyyyy iPhone 8 64GB", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "Смартфон Apple iPhone 9 64GB", "raw": True}}}},
                        ]
                    }
                }
            },
        )

        # Проставляем market_implicit_model_title_no_category, так как он по умолчанию включен
        response = self.report.request_bs_pb(
            request + noVendor + withCategory
        )  # market_model_offers_wizard_right_incut_title_no_category=1
        self.assertFragmentIn(
            response,
            {
                "market_model_offers_wizard": {
                    "showcase": {
                        "items": [
                            {"title": {"text": {"__hl": {"text": "Смартфон iPhone 7 64GB", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "xxxxxx yyyyyy iPhone 8 64GB", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "Смартфон Apple iPhone 9 64GB", "raw": True}}}},
                        ]
                    }
                }
            },
        )

        response = self.report.request_bs_pb(
            request + noVendor
        )  # market_model_offers_wizard_right_incut_title_no_category=1
        self.assertFragmentIn(
            response,
            {
                "market_model_offers_wizard": {
                    "showcase": {
                        "items": [
                            {"title": {"text": {"__hl": {"text": "iPhone 7 64GB", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "yyyyyy iPhone 8 64GB", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "Смартфон Apple iPhone 9 64GB", "raw": True}}}},
                        ]
                    }
                }
            },
        )

    @classmethod
    def prepare_warnings(cls):
        cls.index.category_restrictions += [
            CategoryRestriction(
                name='appearance',
                hids=[3720210],
                regional_restrictions=[
                    RegionalRestriction(
                        show_offers=True,
                        display_only_matched_offers=False,
                        delivery=True,
                        disclaimers=[
                            Disclaimer(
                                name='appearance',
                                text='Внешний вид товаров и/или упаковки может быть изменён изготовителем и отличаться от изображенных на Яндекс.Маркете.',
                                short_text='Внешний вид товаров и/или упаковки может быть изменён изготовителем и отличаться от изображенных на Яндекс.Маркете.',
                                default_warning=False,
                            ),
                        ],
                    ),
                ],
            ),
            CategoryRestriction(
                name='assortment',
                hids=[3720220],
                regional_restrictions=[
                    RegionalRestriction(
                        show_offers=True,
                        display_only_matched_offers=True,
                        delivery=True,
                        disclaimers=[
                            Disclaimer(
                                name='assortment',
                                text='Пожалуйста, обратите внимание — для этого товара нельзя выбрать цвет и дизайн',
                                short_text='',
                                default_warning=False,
                            ),
                        ],
                    ),
                ],
            ),
            CategoryRestriction(
                name='zoo_medicine',
                hids=[3720230],
                regional_restrictions=[
                    RegionalRestriction(
                        show_offers=True,
                        display_only_matched_offers=False,
                        delivery=True,
                        disclaimers=[
                            Disclaimer(
                                name='zoo_medicine',
                                text='Есть противопоказания, посоветуйтесь с врачом.',
                                short_text='Есть противопоказания, посоветуйтесь с врачом.',
                                default_warning=False,
                            ),
                        ],
                    ),
                ],
            ),
        ]

        cls.index.hypertree += [
            HyperCategory(hid=3720210),
            HyperCategory(hid=3720220),
        ]

        cls.index.models += [
            Model(
                hyperid=3720221,
                hid=3720220,
                title='Модель с дисклеймером с ассортиментом',
                ts=3720221,
                disclaimers_model=['assortment'],
            ),
        ]

        cls.index.offers += [
            Offer(hid=3720210, title='Оффер с непоказываемым дисклеймером', ts=3720212),
            Offer(hid=3720230, title='Оффер с показываемым дисклеймером', ts=3720213),
            Offer(hid=3720220, title='Оффер с дисклеймером без модели в ассортименте', ts=3720222),
            Offer(hid=3720220, title='Оффер с дисклеймером с моделью в ассортименте', hyperid=3720221, ts=3720223),
        ]

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 3720211).respond(0.9)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 3720212).respond(0.85)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 3720213).respond(0.84)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 3720221).respond(0.8)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 3720222).respond(0.75)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 3720223).respond(0.7)

    def test_warnings(self):
        """https://st.yandex-team.ru/MARKETOUT-37202
        Проверяем отображение дисклеймеров
        """

        request = 'place=parallel&text=дисклеймер&rearr-factors=market_enable_model_offers_wizard=1;'
        withDisclaimers = 'market_model_offers_wizard_enable_disclaimers=1;'

        # без флага
        response = self.report.request_bs_pb(request)
        self.assertFragmentIn(
            response,
            {
                "market_model_offers_wizard": {
                    "showcase": {
                        "items": [
                            {
                                "title": {
                                    "text": {"__hl": {"text": "Оффер с непоказываемым дисклеймером", "raw": True}}
                                },
                                "notice": Absent(),
                            },
                            {
                                "title": {"text": {"__hl": {"text": "Оффер с показываемым дисклеймером", "raw": True}}},
                                "notice": Absent(),
                            },
                            {
                                "title": {
                                    "text": {"__hl": {"text": "Модель с дисклеймером с ассортиментом", "raw": True}}
                                },
                                "notice": Absent(),
                            },
                        ]
                    }
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

        # с флагом market_model_offers_wizard_enable_disclaimers=1
        response = self.report.request_bs_pb(request + withDisclaimers)
        self.assertFragmentIn(
            response,
            {
                "market_model_offers_wizard": {
                    "showcase": {
                        "items": [
                            {
                                "title": {
                                    "text": {"__hl": {"text": "Оффер с непоказываемым дисклеймером", "raw": True}}
                                },
                                "notice": Absent(),
                            },
                            {
                                "title": {"text": {"__hl": {"text": "Оффер с показываемым дисклеймером", "raw": True}}},
                                "notice": {
                                    "shortText": "Есть противопоказания, посоветуйтесь с врачом.",
                                    "text": "Есть противопоказания, посоветуйтесь с врачом.",
                                    "type": "zoo_medicine",
                                },
                            },
                            {
                                "title": {
                                    "text": {"__hl": {"text": "Модель с дисклеймером с ассортиментом", "raw": True}}
                                },
                                "notice": Absent(),
                            },
                        ]
                    }
                }
            },
            preserve_order=False,
            allow_different_len=False,
        )

    def test_model_offers_sku_id(self):
        """https://st.yandex-team.ru/MARKETOUT-37437
        Проверяем, что skuId есть на выдаче
        """
        request = 'place=parallel&text=blue&rearr-factors=market_enable_model_offers_wizard=1;market_implicit_model_wizard_meta_threshold=-100'
        response = self.report.request_bs_pb(request)
        self.assertFragmentIn(
            response,
            {
                "market_model_offers_wizard": {
                    "showcase": {
                        "items": [
                            {
                                "title": {"text": {"__hl": {"text": "Blue Model 7", "raw": True}}},
                                "skuId": "1070",
                            }
                        ]
                    }
                }
            },
        )

    def test_offers_id_in_showcase_items(self):
        """https://st.yandex-team.ru/MARKETOUT-38356
        Проверяем, что для документов - офферов, offer_id есть в выдаче
        """
        request = 'place=parallel&text=magic&rearr-factors=market_enable_model_offers_wizard=1'
        response = self.report.request_bs_pb(request)

        self.assertFragmentIn(
            response,
            {
                'market_model_offers_wizard': {
                    'showcase': {'items': [{"documentType": "offer", "offerId": "JXXIV9-YOZbUlVDz6SR1Xw"}]}
                }
            },
        )

    def test_incut_replacement(self):
        """Проверяем подмену врезки в модельно-офферном на офферную или неявную
        https://st.yandex-team.ru/MARKETOUT-38245
        """

        request = 'place=parallel&text=magic&rearr-factors=market_enable_model_offers_wizard=1;'

        # Модельно-офферная врезка - есть и модели и офферы
        response = self.report.request_bs_pb(request)
        self.assertFragmentIn(
            response,
            {
                'market_model_offers_wizard': {
                    'showcase': {
                        'items': [
                            {'title': {'text': {'__hl': {'text': 'Magic Model 1'}}}},
                            {'title': {'text': {'__hl': {'text': 'Magic Offer 6'}}}},
                        ]
                    }
                }
            },
        )

        # Неявная врезка - нет офферов
        response = self.report.request_bs_pb(request + 'market_model_offers_wizard_with_implicit_model_incut=1')
        for offerName in [
            "Magic Offer 1",
            "Magic Offer 2",
            "Magic Offer 3",
            "Simple Offer 4",
            "Magic Offer 5",
            "Magic Offer 6",
        ]:
            self.assertFragmentNotIn(
                response,
                {
                    'market_model_offers_wizard': {
                        'showcase': {'items': [{'title': {'text': {'__hl': {'text': offerName}}}}]}
                    }
                },
            )

        # Офферная врезка - нет моделей
        response = self.report.request_bs_pb(request + 'market_model_offers_wizard_with_offers_incut=1')
        for modelName in [
            "Magic Model 1",
            "Magic Model 2",
            "Magic Model 3",
            "Magic Model 4",
            "Simple Model 5",
            "Magic Model 6",
        ]:
            self.assertFragmentNotIn(
                response,
                {
                    'market_model_offers_wizard': {
                        'showcase': {'items': [{'title': {'text': {'__hl': {'text': modelName}}}}]}
                    }
                },
            )


if __name__ == '__main__':
    main()
