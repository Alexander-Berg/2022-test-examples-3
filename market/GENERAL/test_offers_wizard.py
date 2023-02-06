#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

import json
import urllib

from core.types import (
    CategoryRestriction,
    ClickType,
    Disclaimer,
    GLParam,
    GLType,
    GLValue,
    HyperCategory,
    HyperCategoryType,
    MnPlace,
    Model,
    ModelDescriptionTemplates,
    NavCategory,
    Offer,
    Opinion,
    RegionalRestriction,
    Shop,
)
from core.testcase import TestCase, main

from core.matcher import Absent, Contains, LikeUrl, NoKey, NotEmpty


class T(TestCase):
    @classmethod
    def prepare_offers_to_collapsing(cls):
        cls.settings.default_search_experiment_flags += ["market_money_disable_bids=0"]
        cls.index.hypertree += [HyperCategory(hid=1, output_type=HyperCategoryType.GURU)]

        cls.index.navtree += [NavCategory(nid=1111, hid=1)]

        cls.index.models += [
            Model(
                hyperid=5,
                title='iphone 5',
                no_add_picture=True,
                hid=1,
                picinfo='//avatars.mds.yandex.net/get-mpic/group1/iphone_5/orig#100#100',
                opinion=Opinion(rating=4.0, rating_count=15, total_count=30),
            ),
            Model(
                hyperid=6,
                title='iphone 6',
                no_add_picture=True,
                picinfo='//avatars.mds.yandex.net/get-mpic/group1/iphone_6/orig#100#100',
                opinion=Opinion(rating=4.5, rating_count=20, total_count=40),
            ),
            Model(
                hyperid=7,
                title='iphone 7',
                no_add_picture=True,
                picinfo='//avatars.mds.yandex.net/get-mpic/group1/iphone_7/orig#100#100',
                opinion=Opinion(rating=5.0, rating_count=25, total_count=50),
            ),
        ]

        cls.index.offers += [
            Offer(title='iphone 5 256GB', hyperid=5, price=12000, ts=1, bid=100, url='http://appleshop.ru/iphone5/3'),
            Offer(title='iphone 6 256GB', hyperid=6, price=15000, ts=2, bid=110, url='http://appleshop.ru/iphone6/3'),
            Offer(
                title='iphone 7 256GB', hyperid=7, price=18000, ts=3, bid=150, url='http://appleshop.ru/iphone7/3'
            ),  # bid максимальный - будет премиальный, и есть hyperid - будет схлопывание
            Offer(title='iphone 8 256GB', price=21000, sku='12345', ts=4, bid=115, url='http://appleshop.ru/iphone8/3'),
            Offer(title='iphone 9 256GB', price=24000, ts=5, bid=95, url='http://appleshop.ru/iphone9/3'),
            Offer(title='iphone 5 64GB', hyperid=5, price=11000, ts=6, bid=90, url='http://appleshop.ru/iphone5/2'),
            Offer(title='iphone 5 32GB', hyperid=5, price=10000, ts=7, bid=110, url='http://appleshop.ru/iphone5/1'),
            Offer(title='iphone 6 64GB', hyperid=6, price=14000, ts=8, bid=120, url='http://appleshop.ru/iphone6/2'),
            Offer(title='iphone 6 32GB', hyperid=6, price=13000, ts=9, bid=140, url='http://appleshop.ru/iphone6/1'),
            Offer(title='iphone 7 32GB', hyperid=7, price=16000, ts=10, bid=130, url='http://appleshop.ru/iphone7/1'),
        ]

        for i in range(1, 10):
            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, i).respond(0.9 - 0.01 * i)

    def test_offers_wizard_collapsing(self):
        """https://st.yandex-team.ru/MARKETOUT-28719
        If flag market_offers_wizard_collapsing=1
        then if it is possible offers in incut will be collapsed to models
        Flag market_offers_wizard_collapsing_docs_min_size set
        minimum count of documents in incut after collapsing
        """

        request = 'place=parallel&text=iphone&rearr-factors=market_offers_incut_show_always=1;'
        response = self.report.request_bs_pb(request)

        self.assertFragmentIn(
            response,
            {
                'market_offers_wizard': {
                    'showcase': {
                        'items': [
                            {
                                'title': {
                                    'text': {'__hl': {'text': 'iphone 5 256GB'}},
                                    'url': Contains('//market.yandex.ru/offer/'),
                                },
                                'thumb': {
                                    'source': '//avatars.mdst.yandex.net/get-marketpic/1/market_iyC3nHslqLtqZJLygVAHeA/100x100'
                                },
                                'price': {'priceMax': '12000'},
                                'modelId': '5',
                                'rating': Absent(),
                            },
                            {
                                'title': {
                                    'text': {'__hl': {'text': 'iphone 6 256GB'}},
                                    'url': Contains('//market.yandex.ru/offer/'),
                                },
                                'thumb': {
                                    'source': '//avatars.mdst.yandex.net/get-marketpic/1/market_iyC3nHslqLtqZJLygVAHeA/100x100'
                                },
                                'price': {'priceMax': '15000'},
                                'modelId': '6',
                                'rating': Absent(),
                            },
                            {
                                'title': {
                                    'text': {'__hl': {'text': 'iphone 7 256GB'}},
                                    'url': Contains('//market.yandex.ru/offer/'),
                                },
                                'thumb': {
                                    'source': '//avatars.mdst.yandex.net/get-marketpic/1/market_iyC3nHslqLtqZJLygVAHeA/100x100'
                                },
                                'price': {'priceMax': '18000'},
                                'modelId': '7',
                                'rating': Absent(),
                            },
                            {
                                'title': {
                                    'text': {'__hl': {'text': 'iphone 8 256GB'}},
                                    'url': Contains('//market.yandex.ru/offer/'),
                                },
                                'thumb': {
                                    'source': '//avatars.mdst.yandex.net/get-marketpic/1/market_iyC3nHslqLtqZJLygVAHeA/100x100'
                                },
                                'price': {'priceMax': '21000'},
                                'modelId': Absent(),
                                'skuId': '12345',
                                'rating': Absent(),
                            },
                            {
                                'title': {
                                    'text': {'__hl': {'text': 'iphone 9 256GB'}},
                                    'url': Contains('//market.yandex.ru/offer/'),
                                },
                                'thumb': {
                                    'source': '//avatars.mdst.yandex.net/get-marketpic/1/market_iyC3nHslqLtqZJLygVAHeA/100x100'
                                },
                                'price': {'priceMax': '24000'},
                                'modelId': Absent(),
                                'rating': Absent(),
                            },
                        ]
                    }
                }
            },
            preserve_order=True,
        )

        # Проверка что под флагом работает замена офферов на модели
        # выравнивание при схлопывании отключено
        # флаг market_offers_wizard_collapsing_docs_min_size=0 по умолчанию
        request += 'market_offers_wizard_collapsing=1;'
        response = self.report.request_bs_pb(request)

        self.assertFragmentIn(
            response,
            {
                'market_offers_wizard_center_incut': {
                    'showcase': {
                        'items': [
                            {
                                'title': {
                                    'text': {'__hl': {'text': 'iphone 5'}},
                                    'url': LikeUrl.of(
                                        '//market.yandex.ru/product--iphone-5-256gb/5?text=iphone&clid=832'
                                    ),
                                },
                                'thumb': {'source': '//avatars.mds.yandex.net/get-mpic/group1/iphone_5/2hq'},
                                'price': {'priceMax': '10000'},
                                'rating': {'value': '4'},
                                'isCollapsed': '1',
                            },
                            {
                                'title': {
                                    'text': {'__hl': {'text': 'iphone 6'}},
                                    'url': LikeUrl.of(
                                        '//market.yandex.ru/product--iphone-6-256gb/6?text=iphone&clid=832'
                                    ),
                                },
                                'thumb': {'source': '//avatars.mds.yandex.net/get-mpic/group1/iphone_6/2hq'},
                                'price': {'priceMax': '13000'},
                                'rating': {'value': '4.5'},
                                'isCollapsed': '1',
                            },
                            {
                                'title': {
                                    'text': {'__hl': {'text': 'iphone 7'}},
                                    'url': LikeUrl.of(
                                        '//market.yandex.ru/product--iphone-7-256gb/7?text=iphone&clid=832'
                                    ),
                                },
                                'thumb': {'source': '//avatars.mds.yandex.net/get-mpic/group1/iphone_7/2hq'},
                                'price': {'priceMax': '16000'},
                                'rating': {'value': '5'},
                                'isCollapsed': '1',
                            },
                            {
                                'title': {
                                    'text': {'__hl': {'text': 'iphone 8 256GB'}},
                                    'url': Contains('//market.yandex.ru/offer/'),
                                },
                                'thumb': {
                                    'source': '//avatars.mdst.yandex.net/get-marketpic/1/market_iyC3nHslqLtqZJLygVAHeA/100x100'
                                },
                                'price': {'priceMax': '21000'},
                                'rating': Absent(),
                                'isCollapsed': NoKey('isCollapsed'),
                            },
                            {
                                'title': {
                                    'text': {'__hl': {'text': 'iphone 9 256GB'}},
                                    'url': Contains('//market.yandex.ru/offer/'),
                                },
                                'thumb': {
                                    'source': '//avatars.mdst.yandex.net/get-marketpic/1/market_iyC3nHslqLtqZJLygVAHeA/100x100'
                                },
                                'price': {'priceMax': '24000'},
                                'rating': Absent(),
                                'isCollapsed': NoKey('isCollapsed'),
                            },
                        ]
                    }
                }
            },
            allow_different_len=False,
            preserve_order=True,
        )

        # если минимальное количество меньше либо равно чем количество документов - выводим все что есть
        response = self.report.request_bs_pb(request + 'market_offers_wizard_collapsing_docs_min_size=4')
        self.assertFragmentIn(
            response,
            {
                'market_offers_wizard_center_incut': {
                    'showcase': {
                        'items': [
                            {'title': {'text': {'__hl': {'text': 'iphone 5'}}}},
                            {'title': {'text': {'__hl': {'text': 'iphone 6'}}}},
                            {'title': {'text': {'__hl': {'text': 'iphone 7'}}}},
                            {'title': {'text': {'__hl': {'text': 'iphone 8 256GB'}}}},
                            {'title': {'text': {'__hl': {'text': 'iphone 9 256GB'}}}},
                        ]
                    }
                }
            },
            allow_different_len=False,
            preserve_order=True,
        )

        # если минимальное количество больше чем количество документов - выводим без схлопывания
        response = self.report.request_bs_pb(request + 'market_offers_wizard_collapsing_docs_min_size=9')
        self.assertFragmentNotIn(
            response,
            {
                'market_offers_wizard': {
                    'showcase': {
                        'items': [
                            {'title': {'text': {'__hl': {'text': 'iphone 5'}}}},
                            {'title': {'text': {'__hl': {'text': 'iphone 6'}}}},
                            {'title': {'text': {'__hl': {'text': 'iphone 7'}}}},
                        ]
                    }
                }
            },
        )

    def test_premium_and_collapsing_offers(self):
        """https://st.yandex-team.ru/MARKETOUT-27070
        https://st.yandex-team.ru/MARKETOUT-28719
        Проверяем корректность пересечения функциональностей
        премиальных в офферном и схлопывания в модели
        """
        request = 'place=parallel&text=iphone&rearr-factors=market_offers_incut_show_always=1;'

        # Смотрим как выглядит офферный без схлопывания и премиальных
        response = self.report.request_bs_pb(request)
        self.assertFragmentIn(
            response,
            {
                'market_offers_wizard': {
                    'showcase': {
                        'items': [
                            {
                                'title': {'text': {'__hl': {'text': 'iphone 5 256GB'}}},
                                'isCollapsed': NoKey('isCollapsed'),
                                'isPremium': NoKey('isPremium'),
                            },
                            {
                                'title': {'text': {'__hl': {'text': 'iphone 6 256GB'}}},
                                'isCollapsed': NoKey('isCollapsed'),
                                'isPremium': NoKey('isPremium'),
                            },
                            {
                                'title': {'text': {'__hl': {'text': 'iphone 7 256GB'}}},
                                'isCollapsed': NoKey('isCollapsed'),
                                'isPremium': NoKey('isPremium'),
                            },
                            {
                                'title': {'text': {'__hl': {'text': 'iphone 8 256GB'}}},
                                'isCollapsed': NoKey('isCollapsed'),
                                'isPremium': NoKey('isPremium'),
                            },
                            {
                                'title': {'text': {'__hl': {'text': 'iphone 9 256GB'}}},
                                'isCollapsed': NoKey('isCollapsed'),
                                'isPremium': NoKey('isPremium'),
                            },
                        ]
                    }
                }
            },
            preserve_order=True,
        )

        # офферная врезка со схлопыванием
        response = self.report.request_bs_pb(request + 'market_offers_wizard_collapsing=1;')
        self.assertFragmentIn(
            response,
            {
                'market_offers_wizard': {
                    'showcase': {
                        'items': [
                            {
                                'title': {'text': {'__hl': {'text': 'iphone 5'}}},
                                'isCollapsed': '1',
                                'isPremium': NoKey('isPremium'),
                            },
                            {
                                'title': {'text': {'__hl': {'text': 'iphone 6'}}},
                                'isCollapsed': '1',
                                'isPremium': NoKey('isPremium'),
                            },
                            {
                                'title': {'text': {'__hl': {'text': 'iphone 7'}}},
                                'isCollapsed': '1',
                                'isPremium': NoKey('isPremium'),
                            },
                            {
                                'title': {'text': {'__hl': {'text': 'iphone 8 256GB'}}},
                                'isCollapsed': NoKey('isCollapsed'),
                                'isPremium': NoKey('isPremium'),
                            },
                            {
                                'title': {'text': {'__hl': {'text': 'iphone 9 256GB'}}},
                                'isCollapsed': NoKey('isCollapsed'),
                                'isPremium': NoKey('isPremium'),
                            },
                        ]
                    }
                }
            },
            preserve_order=True,
        )

        # офферная врезка с премиальными
        response = self.report.request_bs_pb(
            request + 'market_premium_offer_in_offers_wizard=1;market_offers_wizard_premium_offers_count=9;'
        )
        self.assertFragmentIn(
            response,
            {
                'market_offers_wizard': {
                    'showcase': {
                        'items': [
                            {
                                'title': {'text': {'__hl': {'text': 'iphone 5 256GB'}}},
                                'isCollapsed': NoKey('isCollapsed'),
                                'isPremium': NoKey('isPremium'),
                            },
                            {
                                'title': {'text': {'__hl': {'text': 'iphone 6 256GB'}}},
                                'isCollapsed': NoKey('isCollapsed'),
                                'isPremium': NoKey('isPremium'),
                            },
                            {
                                'title': {'text': {'__hl': {'text': 'iphone 8 256GB'}}},
                                'isCollapsed': NoKey('isCollapsed'),
                                'isPremium': NoKey('isPremium'),
                            },
                            {
                                'title': {'text': {'__hl': {'text': 'iphone 9 256GB'}}},
                                'isCollapsed': NoKey('isCollapsed'),
                                'isPremium': NoKey('isPremium'),
                            },
                        ],
                        'premiumOffers': [
                            {
                                'title': {'text': {'__hl': {'text': 'iphone 7 256GB'}}},
                                'isCollapsed': NoKey('isCollapsed'),
                                'isPremium': '1',
                            },
                        ],
                    }
                }
            },
            preserve_order=True,
        )

        # офферная врезка со схлопыванием и премиальными - премиальные отрабатывают раньше схлопывания
        response = self.report.request_bs_pb(
            request
            + 'market_offers_wizard_collapsing=1;market_premium_offer_in_offers_wizard=1;market_offers_wizard_premium_offers_count=9;'
        )
        self.assertFragmentIn(
            response,
            {
                'market_offers_wizard': {
                    'showcase': {
                        'items': [
                            {
                                'title': {'text': {'__hl': {'text': 'iphone 5'}}},
                                'isCollapsed': '1',
                                'isPremium': NoKey('isPremium'),
                            },
                            {
                                'title': {'text': {'__hl': {'text': 'iphone 6'}}},
                                'isCollapsed': '1',
                                'isPremium': NoKey('isPremium'),
                            },
                            {
                                'title': {'text': {'__hl': {'text': 'iphone 8 256GB'}}},
                                'isCollapsed': NoKey('isCollapsed'),
                                'isPremium': NoKey('isPremium'),
                            },
                            {
                                'title': {'text': {'__hl': {'text': 'iphone 9 256GB'}}},
                                'isCollapsed': NoKey('isCollapsed'),
                                'isPremium': NoKey('isPremium'),
                            },
                        ],
                        'premiumOffers': [
                            {
                                'title': {'text': {'__hl': {'text': 'iphone 7 256GB'}}},
                                'isCollapsed': NoKey('isCollapsed'),
                                'isPremium': '1',
                            },
                        ],
                    }
                }
            },
            preserve_order=True,
        )

    def test_base_one_shop_offers_filtering(self):
        """Check not filtering all offers with flag
        https://st.yandex-team.ru/MARKETOUT-32989
        """
        request = 'place=parallel&text=iphone&rearr-factors=market_offers_incut_show_always=1;'
        response = self.report.request_bs_pb(request)
        self.assertFragmentIn(response, {'market_offers_wizard': {'showcase': {'items': NotEmpty()}}})

        request += 'market_parallel_max_offers_per_shop_count_on_base_search=1'
        response = self.report.request_bs_pb(request)
        self.assertFragmentIn(response, {'market_offers_wizard': {'showcase': {'items': NotEmpty()}}})

    @classmethod
    def prepare_offers_wizard_documents_count_flags(cls):
        cls.index.models.append(Model(hyperid=100500, title='godlike model'))
        cls.index.offers += [
            Offer(title='godlike offer {}'.format(1 + i), bid=1000 - 50 * i, hyperid=100500, ts=100500 + i)
            for i in range(10)
        ]
        for i in range(10):
            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 100500 + i).respond(0.9 - 0.01 * i)

    def test_offers_wizard_documents_count_flags(self):
        """https://st.yandex-team.ru/MARKETOUT-27591
        Проверяем работоспособность флагов для задания количества документов во врезке офферного
        Также смотрим, что нормально работает с премиальными офферами,
        и что новые флаги имеют приоритет перед старыми
        """

        request = (
            'place=parallel&text=godlike&debug=1&rearr-factors=' 'market_offers_incut_show_always=1;' 'offers_touch=1;'
        )

        def do_test(flags, name, offersCount, premiumOffersCount):
            if 'device=touch' in flags:
                response = self.report.request_bs_pb('touch=1&' + request + flags)
            else:
                response = self.report.request_bs_pb(request + flags)

            items = [
                {
                    'title': {'text': {'__hl': {'text': 'godlike offer {}'.format(1 + i)}}},
                    'isPremium': NoKey('isPremium'),
                }
                for i in range(premiumOffersCount, offersCount)
            ]
            premiumOffers = [
                {
                    'title': {'text': {'__hl': {'text': 'godlike offer {}'.format(1 + i)}}},
                    'isPremium': '1',
                }
                for i in range(premiumOffersCount)
            ]

            self.assertFragmentIn(
                response,
                {
                    name: {
                        'showcase': {
                            'premiumOffers': premiumOffers if premiumOffersCount else NoKey('premiumOffers'),
                            'items': items,
                        }
                    }
                },
                allow_different_len=False,
                preserve_order=True,
            )

        maxFlags = 'market_offers_incut_align=1;market_offers_incut_max_count=4;'
        zeroFlags = 'market_offers_incut_max_count=0;market_offers_incut_align=1;'

        enablePremiumOffers = (
            'market_premium_offer_in_offers_wizard=1;'
            'market_offers_wizard_premium_offers_count={0};'
            'market_offers_wizard_premium_offers_adg_count={0};'
            'market_offers_wizard_premium_offers_center_count={0};'
            'market_offers_wizard_premium_offers_right_count={0};'
            'market_offers_wizard_premium_offers_touch_count={0};'
        )
        enableCenterIncut = 'market_enable_offers_wiz_center_incut=1;market_offers_wizard_offers_center_incut_count={};'
        enableRightIncut = 'market_enable_offers_wiz_right_incut=1;market_offers_wizard_offers_right_incut_count={};'
        enableAdGIncut = 'market_enable_offers_adg_wiz=1;market_offers_wizard_offers_adg_count={};'
        touchIncut = 'device=touch;market_offers_wizard_offers_touch_count={};'

        # align is 1
        moreThanDefault = 5
        lessThanDefault = 3

        tests = [
            ('market_offers_wizard_offers_incut_count=3;', 'market_offers_wizard', lessThanDefault, 0),
            ('market_offers_wizard_offers_incut_count=5;', 'market_offers_wizard', moreThanDefault, 0),
            (enableCenterIncut.format(moreThanDefault), 'market_offers_wizard_center_incut', moreThanDefault, 0),
            (enableRightIncut.format(moreThanDefault), 'market_offers_wizard_right_incut', moreThanDefault, 0),
            (enableAdGIncut.format(lessThanDefault), 'market_offers_adg_wizard', lessThanDefault, 0),
            (enableAdGIncut.format(moreThanDefault), 'market_offers_adg_wizard', moreThanDefault, 0),
            (touchIncut.format(lessThanDefault), 'market_offers_wizard', lessThanDefault, 0),
            (touchIncut.format(moreThanDefault), 'market_offers_wizard', moreThanDefault, 0),
            (
                'market_offers_wizard_offers_incut_count=3;' + enablePremiumOffers.format(1),
                'market_offers_wizard',
                lessThanDefault,
                1,
            ),
            (
                'market_offers_wizard_offers_incut_count=5;' + enablePremiumOffers.format(1),
                'market_offers_wizard',
                moreThanDefault,
                1,
            ),
            (
                enableCenterIncut.format(moreThanDefault) + enablePremiumOffers.format(1),
                'market_offers_wizard_center_incut',
                moreThanDefault,
                1,
            ),
            (
                enableRightIncut.format(moreThanDefault) + enablePremiumOffers.format(1),
                'market_offers_wizard_right_incut',
                moreThanDefault,
                1,
            ),
            (
                enableAdGIncut.format(lessThanDefault) + enablePremiumOffers.format(1),
                'market_offers_adg_wizard',
                lessThanDefault,
                1,
            ),
            (
                enableAdGIncut.format(moreThanDefault) + enablePremiumOffers.format(1),
                'market_offers_adg_wizard',
                moreThanDefault,
                1,
            ),
            (
                touchIncut.format(lessThanDefault) + enablePremiumOffers.format(1),
                'market_offers_wizard',
                lessThanDefault,
                1,
            ),
            (
                touchIncut.format(moreThanDefault) + enablePremiumOffers.format(1),
                'market_offers_wizard',
                moreThanDefault,
                1,
            ),
        ]

        # priority of exact flags is more than of common flags
        for flags, name, offersCount, premiumOffersCount in list(tests):
            tests.append([flags + maxFlags, name, offersCount, premiumOffersCount])
            tests.append([flags + zeroFlags, name, offersCount, premiumOffersCount])

        for flags, name, modelsCount, blueIncutCount in tests:
            do_test(flags, name, modelsCount, blueIncutCount)

    @classmethod
    def prepare_medicine_offers(cls):
        cls.index.category_restrictions += [
            CategoryRestriction(
                name='medicine',
                hids=[201, 203],
                regional_restrictions=[
                    RegionalRestriction(
                        show_offers=False,
                        display_only_matched_offers=True,
                        delivery=False,
                        disclaimers=[
                            Disclaimer(
                                name='medicine',
                                text='Есть противопоказания, посоветуйтесь с врачом',
                                short_text='Есть противопоказания, посоветуйтесь с врачом',
                                default_warning=False,
                            ),
                            Disclaimer(
                                name='medicine_recipe',
                                text='Есть противопоказания, посоветуйтесь с врачом. Отпускается по рецепту врача',
                                short_text='Есть противопоказания, посоветуйтесь с врачом. Отпускается по рецепту врача',
                                default_warning=False,
                            ),
                        ],
                    ),
                ],
            ),
            CategoryRestriction(
                name='assortment',
                hids=[201, 203],
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
        ]

        cls.index.hypertree += [
            HyperCategory(hid=201, name='Лекарства'),
            HyperCategory(hid=202, name='Не лекарства'),
            HyperCategory(hid=203, name='БАД'),
        ]

        cls.index.models += [
            Model(
                hyperid=50, hid=201, title='Лекарство', ts=50, disclaimers_model='medicine', opinion=Opinion(rating=4.4)
            ),
            Model(
                hyperid=51,
                hid=201,
                title='Лекарство рецептурное',
                ts=51,
                disclaimers_model=['medicine', 'medicine_recipe'],
            ),  # фильтруется по medicine_recipe
            Model(hyperid=52, hid=202, title='Не лекарство', ts=52),
            Model(
                hyperid=53,
                hid=203,
                title='Лекарство без цвета, вкуса и запаха',
                ts=53,
                disclaimers_model=['medicine', 'assortment'],
            ),  # фильтруется по assortment
        ]
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 50).respond(0.9)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 51).respond(0.85)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 52).respond(0.8)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 53).respond(0.75)

        cls.index.offers += [
            Offer(hid=201, title='Лекарство 1', hyperid=50, price=12, ts=100, fesh=1),
            Offer(hid=201, title='Лекарство 2', hyperid=50, price=15, ts=101, fesh=2),
            Offer(hid=201, title='Лекарство рецептурное 1', hyperid=51, price=18, ts=102, fesh=4),
            Offer(hid=201, title='Лекарство рецептурное 2', hyperid=51, price=18, ts=103, fesh=5),
            Offer(hid=202, title='Не лекарство 1', hyperid=52, price=10, ts=104, fesh=7),
            Offer(hid=202, title='Не лекарство 2', hyperid=52, price=14, ts=105, fesh=8),
            Offer(hid=203, title='Лекарство без цвета 1', hyperid=53, price=100, ts=106, fesh=10),
            Offer(hid=203, title='Лекарство без цвета 2', hyperid=53, price=101, ts=107, fesh=11),
        ]

        for i in range(8):
            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 100 + i).respond(0.9 - 0.01 * i)

    def test_medicine_offers(self):
        """https://st.yandex-team.ru/MARKETOUT-30482
        Проверяем, что под флагом в офферной врезке появляются медицинские офферы
        """

        def make_item(title, warning=None):
            warning = warning or []
            return {
                'title': {'text': {'__hl': {'text': title}}},
                'notice': {'type': warning[0], 'text': warning[1], 'shortText': warning[2]}
                if warning
                else NoKey('notice'),
            }

        medicine = [
            'medicine',
            'Есть противопоказания, посоветуйтесь с врачом',
            'Есть противопоказания, посоветуйтесь с врачом',
        ]
        medicine_recipe = [
            'medicine_recipe',
            'Есть противопоказания, посоветуйтесь с врачом. Отпускается по рецепту врача',
            'Есть противопоказания, посоветуйтесь с врачом. Отпускается по рецепту врача',
        ]
        assortment = ['assortment', 'Пожалуйста, обратите внимание — для этого товара нельзя выбрать цвет и дизайн', '']

        request = (
            'place=parallel&text=лекарство&rearr-factors=market_offers_incut_show_always=1;'
            'market_offers_incut_align=1;'
            'market_check_offers_incut_size=0;'
            'market_parallel_improved_warning_filtering=0;'
            'market_parallel_show_warning_text_for_offers_wizard=1;'
        )
        response = self.report.request_bs_pb(request + 'market_parallel_allow_not_prescription_offers=0')
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    'showcase': {'items': [make_item('Не лекарство {}'.format(i + 1)) for i in range(2)]}
                }
            },
        )

        for title, warning in [
            ('Лекарство {}', medicine),
            ('Лекарство рецептурное {}', medicine),
            ('Лекарство рецептурное {}', medicine_recipe),
            ('Лекарство без цвета {}', medicine),
            ('Лекарство без цвета {}', assortment),
        ]:
            for i in range(2):
                self.assertFragmentNotIn(
                    response,
                    {"market_offers_wizard": {'showcase': {'items': [make_item(title.format(i + 1), warning)]}}},
                )

        # market_parallel_allow_not_prescription_offers=1 по умолчанию
        response = self.report.request_bs_pb(request)
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    'showcase': {
                        'items': [make_item('Лекарство {}'.format(i + 1), medicine) for i in range(2)]
                        + [make_item('Не лекарство {}'.format(i + 1)) for i in range(2)]
                    }
                }
            },
        )

        for title, warning in [
            ('Лекарство рецептурное {}', medicine),
            ('Лекарство рецептурное {}', medicine_recipe),
            ('Лекарство без цвета {}', medicine),
            ('Лекарство без цвета {}', assortment),
        ]:
            for i in range(2):
                self.assertFragmentNotIn(
                    response,
                    {"market_offers_wizard": {'showcase': {'items': [make_item(title.format(i + 1), warning)]}}},
                )

    def test_prescription_filtering_on_base_search(self):
        """Фильтрация рецептурных лекарственных офферов на базовом поиске
        https://st.yandex-team.ru/MARKETOUT-35468
        https://st.yandex-team.ru/MARKETOUT-30482
        """
        request = (
            'place=parallel&text=лекарство&trace_wizard=1&rearr-factors=market_offers_incut_show_always=1;'
            'market_offers_incut_align=1;'
            'market_check_offers_incut_size=0;'
            'market_parallel_improved_warning_filtering=0;'
            'market_parallel_allow_not_prescription_offers=1;'
        )
        response = self.report.request_bs_pb(request + 'market_parallel_filter_prescription_offers_on_base_search=0')
        self.assertIn(
            'Offer has warning: medicine_recipe', response.get_trace_wizard()
        )  # trace происходит на мета-поиске, значит рецептурный оффер не был отфильтрован на базовом

        response = self.report.request_bs_pb(request + 'market_parallel_filter_prescription_offers_on_base_search=1')
        self.assertNotIn(
            'Offer has warning: medicine_recipe', response.get_trace_wizard()
        )  # нет строки в трассировке - оффер был отфильтрован на базовом поиске

    def test_medicine_preview_image_in_text(self):
        """Проверяем, что под флагами market_offers_wizard_show_image_in_text и market_parallel_allow_not_prescription_offers
        будет появляться картинка, меняться описание на новое с учетом мед. категории и возвращаться предупреждение
        https://st.yandex-team.ru/MARKETOUT-30482
        https://st.yandex-team.ru/MARKETOUT-27932
        https://st.yandex-team.ru/MARKETOUT-28995
        """
        request = (
            'place=parallel&text=лекарство&rearr-factors=market_offers_incut_show_always=1;'
            'market_offers_incut_align=1;'
            'market_check_offers_incut_size=0;'
            'market_parallel_allow_not_prescription_offers=1;'
            'market_offers_wizard_show_image_in_text=0;'
            'market_parallel_show_warning_text_for_offers_wizard=1;'
        )
        response = self.report.request_bs_pb(request)
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "snippetPicture": NoKey("snippetPicture"),
                    "warning": NoKey("warning"),
                    "warningType": NoKey("warningType"),
                    "textWhenPicture": NoKey("textWhenPicture"),
                }
            },
        )

        request = (
            'place=parallel&text=лекарство&rearr-factors=market_offers_incut_show_always=1;'
            'market_offers_incut_align=1;'
            'market_check_offers_incut_size=0;'
            'market_parallel_allow_not_prescription_offers=1;'
            'market_offers_wizard_show_image_in_text=1;'
            'market_parallel_show_warning_text_for_offers_wizard=1;'
        )
        response = self.report.request_bs_pb(request)
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "textWhenPicture": {
                        "__hl": {
                            "text": "Товары из магазина SHOP-1 (на фото) и еще 7. Выбор по параметрам.",
                            "raw": True,
                        }
                    },
                    "snippetPicture": "//avatars.mdst.yandex.net/get-marketpic/1/market_iyC3nHslqLtqZJLygVAHeA/100x100",
                    "warning": "Есть противопоказания, посоветуйтесь с врачом",
                    "warningType": "medicine",
                }
            },
        )

    def test_no_preview_image_in_text_cpa_offers_wizard(self):
        """Проверяем, что в колдунщике market_offers_wizard_text_cpa картинка и текст с описанием не появляются
        https://st.yandex-team.ru/MARKETOUT-36904
        """
        request = (
            'place=parallel&text=лекарство&rearr-factors=market_offers_incut_show_always=1;'
            'market_offers_incut_align=1;'
            'market_check_offers_incut_size=0;'
            'market_parallel_allow_not_prescription_offers=1;'
            'market_enable_offers_wiz_text_cpa=1;'
        )
        response = self.report.request_bs_pb(request)
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard_text_cpa": {
                    "snippetPicture": NoKey("snippetPicture"),
                    "warning": NoKey("warning"),
                    "warningType": NoKey("warningType"),
                    "textWhenPicture": NoKey("textWhenPicture"),
                }
            },
        )

        response = self.report.request_bs_pb(request + 'market_offers_wizard_show_image_in_text=1')
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard_text_cpa": {
                    "snippetPicture": NoKey("snippetPicture"),
                    "warning": NoKey("warning"),
                    "warningType": NoKey("warningType"),
                    "textWhenPicture": NoKey("textWhenPicture"),
                }
            },
        )

    def test_preview_image_rating(self):
        """Проверяем, что рейтинг оффера от которого берем картинку для превью возвращается отдельным полем
        https://st.yandex-team.ru/MARKETOUT-35420
        """
        request = (
            'place=parallel&text=лекарство&rearr-factors=market_offers_incut_show_always=1;'
            'market_offers_incut_align=1;'
            'market_check_offers_incut_size=0;'
            'market_parallel_allow_not_prescription_offers=1;'
            'market_parallel_show_warning_text_for_offers_wizard=1;'
        )

        response = self.report.request_bs_pb(request + 'market_offers_wizard_show_image_in_text=1')
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "snippetPicture": "//avatars.mdst.yandex.net/get-marketpic/1/market_iyC3nHslqLtqZJLygVAHeA/100x100",
                    "warning": "Есть противопоказания, посоветуйтесь с врачом",
                    "warningType": "medicine",
                    "rating": "4.4",
                }
            },
        )

    @classmethod
    def prepare_boost_meta_formula_by_multitoken(cls):
        cls.index.offers += [
            Offer(title="filter 1", ts=21),
            Offer(title="filter 2", ts=22),
            Offer(title="filter 3", ts=23),
            Offer(title="filter fz-a60mfe", ts=24),
        ]

        # Base formula
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 21).respond(0.8)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 22).respond(0.7)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 23).respond(0.6)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 24).respond(0.5)

        # Meta formula
        cls.matrixnet.on_place(MnPlace.INCUT_META, 21).respond(0.6)
        cls.matrixnet.on_place(MnPlace.INCUT_META, 22).respond(0.5)
        cls.matrixnet.on_place(MnPlace.INCUT_META, 23).respond(0.4)
        cls.matrixnet.on_place(MnPlace.INCUT_META, 24).respond(0.3)

    def test_boost_meta_formula_by_multitoken(self):
        """Проверяем, что под флагом market_parallel_boost_completely_match_multitoken_coef в офферном колдунщике
        для офферов с совпадением по мультитокену бустятся значения базовой и мета формул
        https://st.yandex-team.ru/MARKETOUT-31295
        """
        qtree = {
            'Market': {
                'qtree4market': 'cHic7VS_q9NgFL3nJi3hs0h58qAElFKXIDwIDrUIgnQqovh803uZGtFHF5c3lU6FJ1JExV8Iios_OlrKA8HFwc0x4O7g4Oq_4P2SL2mShgeC4GK23HO_85177knUFdVwqEktarPHPm2QSx06R-fpYlonj3y6XBvUtmmXhjTCY9Ar0FvQEegzSJ6voAi33OdQ1w2dI8c0He9PXGxljJxjpAFpxtHPH42UUbpLnD710L_goEmugB3y4GP7GSdCDlwlxZYuUo926GZd2sg7NXLGPGGHpyBhdt9D7Sl9N2ey6mHXv7N_W2bFlpFWr5D2-sX3D5yKM2eqBF6KBZoGI_KpbUSeUQaoFEoTSmR2EoXctnSXpg0Oa8502tzUl7m_bDX4s1Uds6iPUFdLi0KYNwMVZnyyUz6E6ybY_T05BRdheUmnFcJWTCWj86MoWI1PYxZ7BIaBrdl8UcT1Hocwm5xCXSsHrOvnbEjyhYLuaBWvrr8uvNY_K8fgCtg54adP14-HMImfUCbhC9SNkgQrSVKqwarw7sHLJ9k2rMoQcX8Y-2etEpQ52Fa6KiYl8eHZIsA8MwliYqkDs4Dni3xH3sZi0tZzZ7psXfcoeJNL4cPaX0zht4ptRsscXVUKjzazbUbLqm9xnPwsoqVx8R3vYshxDqUYBxE93sFhIWdr8L3j4btFWOz9n9F_llFb3u6joWw98obtnDzQ4G_fTxRY'  # noqa
            }
        }

        request = "place=parallel&text=filter+fz-a60mfe&wizard-rules={}&trace_wizard=1".format(
            urllib.quote(json.dumps(qtree))
        )

        # 1. Без флага market_parallel_boost_completely_match_multitoken_coef бустинга базовой и мета формул нет
        response = self.report.request_bs_pb(request)
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "showcase": {
                        "items": [
                            {"title": {"text": {"__hl": {"text": "filter 1", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "filter 2", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "filter 3", "raw": True}}}},
                        ]
                    }
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )
        self.assertIn('10 1 Top 4 MatrixNet sum: 2.6', response.get_trace_wizard())  # Base 0.8 + 0.7 + 0.6 + 0.5
        self.assertIn('10 1 Top 3 meta MatrixNet sum: 1.5', response.get_trace_wizard())  # Meta 0.6 + 0.5 + 0.4

        # 2. Под флагом market_parallel_boost_completely_match_multitoken_coef для оффера "filter fz-a60mfe" есть совпадение по мультитокену
        # значения базовой и мета формул бустятся
        response = self.report.request_bs_pb(
            request + '&rearr-factors=market_parallel_boost_completely_match_multitoken_coef=2'
        )
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "showcase": {
                        "items": [
                            {"title": {"text": {"__hl": {"text": "filter fz-a60mfe", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "filter 1", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "filter 2", "raw": True}}}},
                        ]
                    }
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )
        self.assertIn('10 1 Top 4 MatrixNet sum: 3.1', response.get_trace_wizard())  # Base 0.8 + 0.7 + 0.6 + 0.5*2
        self.assertIn('10 1 Top 3 meta MatrixNet sum: 1.7', response.get_trace_wizard())  # Meta 0.6 + 0.5 + 0.3*2

    def test_not_onstock_parameter_in_urls(self):
        """Проверяем, что под флагом market_parallel_not_onstock_in_urls=1 в ссылки офферного колдунщика,
        ведущие на search добавляется параметр &onstock=0
        https://st.yandex-team.ru/MARKETOUT-32130
        """
        request = (
            'place=parallel&text=iphone&rearr-factors=market_top_categories_in_offers_wizard=1;'
            'market_offers_wizard_top_shops_max_count=1;market_offers_wizard_add_sortings=1;market_parallel_not_onstock_in_urls=1'
        )
        response = self.report.request_bs_pb(request)
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "url": LikeUrl.of("//market.yandex.ru/search?text=iphone&lr=0&clid=545&onstock=0"),
                    "urlTouch": LikeUrl.of("//m.market.yandex.ru/search?text=iphone&lr=0&clid=708&onstock=0"),
                    "adGUrl": LikeUrl.of("//market.yandex.ru/search?text=iphone&lr=0&clid=913&onstock=0"),
                    "adGUrlTouch": LikeUrl.of("//m.market.yandex.ru/search?text=iphone&lr=0&clid=919&onstock=0"),
                    "url_for_category_name": LikeUrl.of("//market.yandex.ru/search?text=iphone&clid=545&onstock=0"),
                    "snippetUrl": LikeUrl.of("//market.yandex.ru/search?text=iphone&clid=545&onstock=0"),
                    "snippetUrlTouch": LikeUrl.of("//m.market.yandex.ru/search?text=iphone&clid=708&onstock=0"),
                    "snippetAdGUrl": LikeUrl.of("//market.yandex.ru/search?text=iphone&clid=913&onstock=0"),
                    "snippetAdGUrlTouch": LikeUrl.of("//m.market.yandex.ru/search?text=iphone&clid=919&onstock=0"),
                    "greenUrl": [
                        {
                            "url": LikeUrl.of("//market.yandex.ru/search?text=iphone&lr=0&clid=545&onstock=0"),
                            "urlTouch": LikeUrl.of("//m.market.yandex.ru/search?text=iphone&lr=0&clid=708&onstock=0"),
                            "adGUrl": LikeUrl.of("//market.yandex.ru/search?text=iphone&lr=0&clid=913&onstock=0"),
                            "adGUrlTouch": LikeUrl.of(
                                "//m.market.yandex.ru/search?text=iphone&lr=0&clid=919&onstock=0"
                            ),
                            "snippetUrl": LikeUrl.of("//market.yandex.ru/search?text=iphone&clid=545&onstock=0"),
                            "snippetUrlTouch": LikeUrl.of("//m.market.yandex.ru/search?text=iphone&clid=708&onstock=0"),
                            "snippetAdGUrl": LikeUrl.of("//market.yandex.ru/search?text=iphone&clid=913&onstock=0"),
                            "snippetAdGUrlTouch": LikeUrl.of(
                                "//m.market.yandex.ru/search?text=iphone&clid=919&onstock=0"
                            ),
                        }
                    ],
                    "button": [
                        {
                            "url": LikeUrl.of("//market.yandex.ru/search?text=iphone&lr=0&clid=545&onstock=0"),
                            "urlTouch": LikeUrl.of("//m.market.yandex.ru/search?text=iphone&lr=0&clid=708&onstock=0"),
                            "adGUrl": LikeUrl.of("//market.yandex.ru/search?text=iphone&lr=0&clid=913&onstock=0"),
                            "adGUrlTouch": LikeUrl.of(
                                "//m.market.yandex.ru/search?text=iphone&lr=0&clid=919&onstock=0"
                            ),
                        }
                    ],
                    "showcase": {
                        "items": [
                            {
                                "title": {
                                    "urlForCounter": LikeUrl.of(
                                        "//market.yandex.ru/search?text=iphone&clid=545&cvredirect=0&onstock=0"
                                    )
                                },
                                "thumb": {
                                    "urlForCounter": LikeUrl.of(
                                        "//market.yandex.ru/search?text=iphone&clid=545&cvredirect=0&onstock=0"
                                    )
                                },
                            }
                        ],
                        "top_shops_url": LikeUrl.of("//market.yandex.ru/search?text=iphone&clid=913&onstock=0"),
                        "top_shops": [{"url": LikeUrl.of("//market.yandex.ru/search?text=iphone&clid=913&onstock=0")}],
                        "sortings": [
                            {"url": LikeUrl.of("//market.yandex.ru/search?text=iphone&clid=913&how=aprice&onstock=0")},
                            {
                                "url": LikeUrl.of(
                                    "//market.yandex.ru/search?text=iphone&clid=913&how=discount_p&onstock=0"
                                )
                            },
                            {"url": LikeUrl.of("//market.yandex.ru/search?text=iphone&clid=913&how=quality&onstock=0")},
                        ],
                    },
                    "sitelinks": [
                        {
                            "url": LikeUrl.of("//market.yandex.ru/search?text=iphone&clid=545&onstock=0"),
                            "urlTouch": LikeUrl.of("//m.market.yandex.ru/search?text=iphone&clid=708&onstock=0"),
                            "adGUrl": LikeUrl.of("//market.yandex.ru/search?text=iphone&clid=913&onstock=0"),
                            "adGUrlTouch": LikeUrl.of("//m.market.yandex.ru/search?text=iphone&clid=919&onstock=0"),
                        }
                    ],
                }
            },
        )

    def test_region_in_offers_incut_urls(self):
        """Проверяем, что под флагом market_region_in_offers_incut_urls=1 в ссылки офферного колдунщика,
        ведущие на search и catalog добавляется параметр &lr
        https://st.yandex-team.ru/MARKETOUT-33258
        """
        request = 'place=parallel&text=iphone&rearr-factors=market_region_in_offers_incut_urls=1;'

        response = self.report.request_bs_pb(request)
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "showcase": {
                        "items": [
                            {
                                "title": {
                                    "urlForCounter": LikeUrl.of(
                                        "//market.yandex.ru/search?text=iphone&lr=0&clid=545&cvredirect=0"
                                    )
                                },
                                "thumb": {
                                    "urlForCounter": LikeUrl.of(
                                        "//market.yandex.ru/search?text=iphone&lr=0&clid=545&cvredirect=0"
                                    )
                                },
                            }
                        ]
                    }
                }
            },
        )

        response = self.report.request_bs_pb(request + 'market_offers_wizard_incut_url_type=NailedInCatalog')
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "showcase": {
                        "items": [
                            {
                                "title": {
                                    "urlForCounter": LikeUrl.of(
                                        "//market.yandex.ru/catalog/1111/list?hid=1&text=iphone&lr=0&clid=545"
                                    )
                                },
                                "thumb": {
                                    "urlForCounter": LikeUrl.of(
                                        "//market.yandex.ru/catalog/1111/list?hid=1&text=iphone&lr=0&clid=545"
                                    )
                                },
                            }
                        ]
                    }
                }
            },
        )

    @classmethod
    def prepare_restricted_offers(cls):
        cls.index.hypertree += [
            HyperCategory(hid=301, name='Сувенирное оружие'),
            HyperCategory(hid=302, name='Ювелирные изделия'),
            HyperCategory(hid=303, name='Книги'),
            HyperCategory(hid=304, name='Детское питание'),
            HyperCategory(hid=305, name='Парфюм'),
        ]

        cls.index.category_restrictions += [
            CategoryRestriction(
                name='weapons',
                hids=[301],
                regional_restrictions=[
                    RegionalRestriction(
                        disclaimers=[
                            Disclaimer(
                                name='weapons',
                                text='Конструктивно сходные с оружием изделия.',
                                short_text='Конструктивно сходные с оружием изделия.',
                            ),
                        ]
                    ),
                ],
            ),
            CategoryRestriction(
                name='jewelry',
                hids=[302],
            ),
            CategoryRestriction(
                name='age',
                hids=[303],
                regional_restrictions=[
                    RegionalRestriction(
                        disclaimers=[
                            Disclaimer(
                                name='age',
                                text='Возрастное ограничение',
                                short_text='Возрастное ограничение',
                                default_warning=True,
                            ),
                            Disclaimer(
                                name='age_18', text='Возрастное ограничение', short_text='Возрастное ограничение'
                            ),
                            Disclaimer(
                                name='age_16', text='Возрастное ограничение', short_text='Возрастное ограничение'
                            ),
                        ]
                    ),
                ],
            ),
            CategoryRestriction(
                name='childfood',
                hids=[304],
                regional_restrictions=[
                    RegionalRestriction(
                        disclaimers=[
                            Disclaimer(
                                name='childfood0',
                                text='Необходима консультация специалистов. Для питания детей с рождения.',
                                short_text='Для детей с рождения, уточните у врача.',
                            ),
                            Disclaimer(
                                name='childfood24',
                                text='Необходима консультация специалистов. Для питания детей с 2 лет.',
                                short_text='Для детей с 2 лет, уточните у врача.',
                            ),
                            Disclaimer(
                                name='nonchildfood',
                                text='Необходима консультация специалиста.',
                                short_text='Необходима консультация специалиста.',
                            ),
                        ]
                    ),
                ],
            ),
            CategoryRestriction(
                name='perfum',
                hids=[305],
                regional_restrictions=[
                    RegionalRestriction(
                        disclaimers=[
                            Disclaimer(
                                name='perfum',
                                text='Информацию об обязательном подтверждении соответствия товаров требованиям законодательства РФ запрашивайте в магазине',
                                short_text='Информацию об обязательном подтверждении соответствия товаров требованиям законодательства РФ запрашивайте в магазине',
                            ),
                        ]
                    ),
                ],
            ),
        ]

        cls.index.models += [
            Model(hyperid=50001, hid=301, title='Самурайская катана', ts=50001, disclaimers_model='weapons'),
            Model(hyperid=50002, hid=302, title='Кольцо на интеллект', ts=50002),
            Model(hyperid=50003, hid=303, title='Книга', ts=50003, disclaimers_model='age'),
            Model(hyperid=50004, hid=303, title='Книга 18+', ts=50004, disclaimers_model='age_18'),
            Model(hyperid=50005, hid=303, title='Книга 16+', ts=50005, disclaimers_model='age_16'),
            Model(hyperid=50006, hid=304, title='Детская еда с рождения', ts=50006, disclaimers_model='childfood0'),
            Model(hyperid=50007, hid=304, title='Детская еда с 2 лет', ts=50007, disclaimers_model='childfood24'),
            Model(hyperid=50008, hid=304, title='Не совсем детская еда', ts=50008, disclaimers_model='nonchildfood'),
            Model(hyperid=50009, hid=305, title='Духи классные', ts=50009, disclaimers_model='perfum'),
        ]

        for i in range(9):
            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 50001 + i).respond(0.99 - 0.01 * i)

        OFFERS_TO_MODEL_COUNT = 3
        OFFERS_START_TS = 50501
        for i in range(OFFERS_TO_MODEL_COUNT):
            cls.index.offers += [
                Offer(
                    hid=301,
                    title='Самурайская катана {}-го ранга'.format(i + 1),
                    hyperid=50001,
                    price=100 + i * 20,
                    ts=OFFERS_START_TS + i,
                    fesh=1001 + i,
                ),
                Offer(
                    hid=302,
                    title='Кольцо на +{}0 к интеллекту'.format(i + 1),
                    hyperid=50002,
                    price=10 ** (i + 1),
                    ts=OFFERS_START_TS + OFFERS_TO_MODEL_COUNT + i,
                    fesh=1001 + OFFERS_TO_MODEL_COUNT + i,
                ),
                Offer(
                    hid=303,
                    title='Книга магии огня {}-й уровень'.format(i + 1),
                    hyperid=50003,
                    price=20 * (10**i),
                    ts=OFFERS_START_TS + 2 * OFFERS_TO_MODEL_COUNT + i,
                    fesh=1001 + 2 * OFFERS_TO_MODEL_COUNT + i,
                ),
                Offer(
                    hid=303,
                    title='Книга по некромантии {}-й уровень (18+)'.format(i + 1),
                    hyperid=50004,
                    price=17 * (10**i),
                    ts=OFFERS_START_TS + 3 * OFFERS_TO_MODEL_COUNT + i,
                    fesh=1001 + 3 * OFFERS_TO_MODEL_COUNT + i,
                ),
                Offer(
                    hid=303,
                    title='Книга магии света {}-й уровень'.format(i + 1),
                    hyperid=50005,
                    price=23 * (10**i),
                    ts=OFFERS_START_TS + 4 * OFFERS_TO_MODEL_COUNT + i,
                    fesh=1001 + 4 * OFFERS_TO_MODEL_COUNT + i,
                ),
                Offer(
                    hid=304,
                    title='Детская еда c рождения {}-е поколение'.format(i + 1),
                    hyperid=50006,
                    price=5 * (3**i),
                    ts=OFFERS_START_TS + 5 * OFFERS_TO_MODEL_COUNT + i,
                    fesh=1001 + 5 * OFFERS_TO_MODEL_COUNT + i,
                ),
                Offer(
                    hid=304,
                    title='Детская еда c 2 лет {}-е поколение'.format(i + 1),
                    hyperid=50007,
                    price=6 * (3**i),
                    ts=OFFERS_START_TS + 6 * OFFERS_TO_MODEL_COUNT + i,
                    fesh=1001 + 6 * OFFERS_TO_MODEL_COUNT + i,
                ),
                Offer(
                    hid=304,
                    title='Не-детская еда {}-е поколение'.format(i + 1),
                    hyperid=50008,
                    price=8 * (3**i),
                    ts=OFFERS_START_TS + 7 * OFFERS_TO_MODEL_COUNT + i,
                    fesh=1001 + 7 * OFFERS_TO_MODEL_COUNT + i,
                ),
                Offer(
                    hid=305,
                    title='Духи классные +{} к обаянию'.format(i * 10 + 5),
                    hyperid=50009,
                    price=50 * (7**i),
                    ts=OFFERS_START_TS + 8 * OFFERS_TO_MODEL_COUNT + i,
                    fesh=1001 + 8 * OFFERS_TO_MODEL_COUNT + i,
                ),
            ]

        for i in range(9 * OFFERS_TO_MODEL_COUNT):
            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 50501 + i).respond(0.99 - 0.01 * i)

    def test_wizard_disable_report_filtering(self):
        """https://st.yandex-team.ru/GOODS-4257
        Проверяем работы флага goods_disable_report_filtering
        """

        def make_item(name, warning_name=None):
            return {
                'title': {'text': {'__hl': {'text': name}}},
                'notice': {
                    'type': warning_name,
                }
                if warning_name
                else NoKey('notice'),
            }

        request = (
            'place=parallel&text={}&rearr-factors='
            'goods_enable_unfamily_goods_filtering=1;'
            'goods_show_unfamily_goods=1;'
        )
        flag = 'goods_disable_report_filtering=1;'

        # фильтруем оружие при выключенном флаге
        response = self.report.request_bs_pb(request.format('Катана'))
        self.assertFragmentNotIn(response, {'market_offers_wizard': {}})

        # не фильтруем оружие при поднятом флаге
        response = self.report.request_bs_pb(request.format('Катана') + flag)
        self.assertFragmentIn(
            response,
            {
                'market_offers_wizard': {
                    'showcase': {
                        'items': [
                            make_item('Самурайская катана 1-го ранга'),
                            make_item('Самурайская катана 2-го ранга'),
                            make_item('Самурайская катана 3-го ранга'),
                        ]
                    }
                }
            },
        )

    def test_wizards_improved_warning_filtering(self):
        """https://st.yandex-team.ru/MARKETOUT-32267
        Проверяем работу флага market_parallel_improved_warning_filtering
        """

        def make_item(name, warning_name=None):
            return {
                'title': {'text': {'__hl': {'text': name}}},
                'notice': {
                    'type': warning_name,
                }
                if warning_name
                else NoKey('notice'),
            }

        request = (
            'place=parallel&text={}&rearr-factors=market_offers_incut_show_always=1;'
            'market_offers_incut_align=1;'
            'market_check_offers_incut_size=0;'
            'market_offers_wizard_offers_incut_count=10;'
            'market_parallel_show_warning_text_for_offers_wizard=1;'
        )
        flag = 'market_parallel_improved_warning_filtering=1'

        # фильтруем оружие вне зависимости от включения флага
        response = self.report.request_bs_pb(request.format('Катана'))
        self.assertFragmentIn(response, {'market_offers_wizard': NoKey('market_offers_wizard')})
        response = self.report.request_bs_pb(request.format('Катана') + flag)
        self.assertFragmentIn(response, {'market_offers_wizard': NoKey('market_offers_wizard')})

        # не фильтруем ювелирку
        response = self.report.request_bs_pb(request.format('Кольцо'))
        self.assertFragmentIn(
            response,
            {
                'market_offers_wizard': {
                    'showcase': {
                        'items': [
                            make_item('Кольцо на +10 к интеллекту'),
                            make_item('Кольцо на +20 к интеллекту'),
                            make_item('Кольцо на +30 к интеллекту'),
                        ]
                    }
                }
            },
        )
        response = self.report.request_bs_pb(request.format('Кольцо') + flag)
        self.assertFragmentIn(
            response,
            {
                'market_offers_wizard': {
                    'showcase': {
                        'items': [
                            make_item('Кольцо на +10 к интеллекту'),
                            make_item('Кольцо на +20 к интеллекту'),
                            make_item('Кольцо на +30 к интеллекту'),
                        ]
                    }
                }
            },
        )

        # фильтруем книги, если 18+ под флагом, иначе фильтруем всегда
        response = self.report.request_bs_pb(request.format('Книга'))
        self.assertFragmentIn(response, {'market_offers_wizard': NoKey('market_offers_wizard')})
        response = self.report.request_bs_pb(request.format('Книга') + flag)
        self.assertFragmentIn(
            response,
            {
                'market_offers_wizard': {
                    'showcase': {
                        'items': [
                            make_item('Книга магии света 1-й уровень', 'age_16'),
                            make_item('Книга магии света 2-й уровень', 'age_16'),
                            make_item('Книга магии света 3-й уровень', 'age_16'),
                        ]
                    }
                }
            },
        )
        # фильтруем age и age_18
        self.assertFragmentNotIn(
            response,
            {"market_offers_wizard": {'showcase': {'items': [make_item('Книга магии огня 1-й уровень', 'age')]}}},
        )
        self.assertFragmentNotIn(
            response,
            {"market_offers_wizard": {'showcase': {'items': [make_item('Книга магии огня 2-й уровень', 'age')]}}},
        )
        self.assertFragmentNotIn(
            response,
            {"market_offers_wizard": {'showcase': {'items': [make_item('Книга магии огня 3-й уровень', 'age')]}}},
        )
        self.assertFragmentNotIn(
            response,
            {
                "market_offers_wizard": {
                    'showcase': {'items': [make_item('Книга по некромантии 1-й уровень (18+)', 'age_18')]}
                }
            },
        )
        self.assertFragmentNotIn(
            response,
            {
                "market_offers_wizard": {
                    'showcase': {'items': [make_item('Книга по некромантии 2-й уровень (18+)', 'age_18')]}
                }
            },
        )
        self.assertFragmentNotIn(
            response,
            {
                "market_offers_wizard": {
                    'showcase': {'items': [make_item('Книга по некромантии 3-й уровень (18+)', 'age_18')]}
                }
            },
        )

        # не фильтруем детское питание, не показываем nonchildfood
        response = self.report.request_bs_pb(request.format('Детская еда'))
        self.assertFragmentIn(response, {'market_offers_wizard': NoKey('market_offers_wizard')})
        response = self.report.request_bs_pb(request.format('Детская еда') + flag)
        self.assertFragmentIn(
            response,
            {
                'market_offers_wizard': {
                    'showcase': {
                        'items': [
                            make_item('Детская еда c рождения 1-е поколение', 'childfood0'),
                            make_item('Детская еда c рождения 2-е поколение', 'childfood0'),
                            make_item('Детская еда c рождения 3-е поколение', 'childfood0'),
                            make_item('Детская еда c 2 лет 1-е поколение', 'childfood24'),
                            make_item('Детская еда c 2 лет 2-е поколение', 'childfood24'),
                            make_item('Детская еда c 2 лет 3-е поколение', 'childfood24'),
                            make_item('Не-детская еда 1-е поколение', None),
                            make_item('Не-детская еда 2-е поколение', None),
                            make_item('Не-детская еда 3-е поколение', None),
                        ]
                    }
                }
            },
        )

        # не фильтруем парфюм, и не показываем дисклеймер
        response = self.report.request_bs_pb(request.format('Духи'))
        self.assertFragmentIn(response, {'market_offers_wizard': NoKey('market_offers_wizard')})
        response = self.report.request_bs_pb(request.format('Духи') + flag)
        self.assertFragmentIn(
            response,
            {
                'market_offers_wizard': {
                    'showcase': {
                        'items': [
                            make_item('Духи классные +5 к обаянию', None),
                            make_item('Духи классные +15 к обаянию', None),
                            make_item('Духи классные +25 к обаянию', None),
                        ]
                    }
                }
            },
        )

    def test_show_warning_text(self):
        """https://st.yandex-team.ru/MARKETOUT-34623"""
        request = (
            'place=parallel&text=Детская еда с рождения&rearr-factors=market_parallel_improved_warning_filtering=1;'
        )
        response = self.report.request_bs_pb(request + 'market_parallel_show_warning_text_for_offers_wizard=1')
        self.assertFragmentIn(
            response,
            {
                'market_offers_wizard': {
                    'showcase': {
                        'items': [
                            {
                                'title': {'text': {'__hl': {'text': 'Детская еда c рождения 1-е поколение'}}},
                                'notice': {
                                    'type': 'childfood0',
                                    'text': 'Необходима консультация специалистов. Для питания детей с рождения.',
                                },
                            }
                        ]
                    }
                }
            },
        )
        response = self.report.request_bs_pb(request + 'market_parallel_show_warning_text_for_offers_wizard=0')
        self.assertFragmentIn(
            response,
            {
                'market_offers_wizard': {
                    'showcase': {
                        'items': [
                            {
                                'title': {'text': {'__hl': {'text': 'Детская еда c рождения 1-е поколение'}}},
                                'notice': NoKey('notice'),
                            }
                        ]
                    }
                }
            },
        )

    def test_filter_showable_warnings(self):
        """https://st.yandex-team.ru/MARKETOUT-34895"""
        request = (
            'place=parallel&text=Детская еда&rearr-factors=market_parallel_improved_warning_filtering=1;'
            'market_offers_incut_show_always=1;'
            'market_offers_incut_align=1;'
            'market_check_offers_incut_size=0;'
            'market_offers_wizard_offers_incut_count=10;'
        )

        response = self.report.request_bs_pb(request)
        self.assertFragmentIn(
            response,
            {
                'market_offers_wizard': {
                    'showcase': {
                        'items': [{'title': {'text': {'__hl': {'text': 'Детская еда c рождения 1-е поколение'}}}}]
                    }
                }
            },
        )
        self.assertFragmentIn(
            response,
            {
                'market_offers_wizard': {
                    'showcase': {'items': [{'title': {'text': {'__hl': {'text': 'Не-детская еда 1-е поколение'}}}}]}
                }
            },
        )

        response = self.report.request_bs_pb(request + 'market_parallel_filter_offers_with_showable_warnings=1')
        self.assertFragmentNotIn(
            response,
            {
                'market_offers_wizard': {
                    'showcase': {
                        'items': [{'title': {'text': {'__hl': {'text': 'Детская еда c рождения 1-е поколение'}}}}]
                    }
                }
            },
        )
        self.assertFragmentIn(
            response,
            {
                'market_offers_wizard': {
                    'showcase': {'items': [{'title': {'text': {'__hl': {'text': 'Не-детская еда 1-е поколение'}}}}]}
                }
            },
        )

    @classmethod
    def prepare_boost_local_offers(cls):

        cls.index.shops += [
            Shop(fesh=111, priority_region=213, regions=[225, 54, 213]),
            Shop(fesh=112, priority_region=213, regions=[225, 54, 213]),
            Shop(fesh=113, priority_region=213, regions=[225, 54, 213]),
            Shop(fesh=114, priority_region=54, regions=[225, 54, 213]),
        ]

        cls.index.offers += [
            Offer(title="sofa 1", ts=21, fesh=111),
            Offer(title="sofa 2", ts=22, fesh=112),
            Offer(title="sofa 3", ts=23, fesh=113),
            Offer(title="sofa 4", ts=24, fesh=114),
        ]

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 21).respond(0.8)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 22).respond(0.7)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 23).respond(0.6)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 24).respond(0.5)

    def test_boost_local_offers(self):
        """
        Проверяем, что для колдунщиков флаг market_local_offers_boost_coef бустит значение формулы для локальных офферов
        https://st.yandex-team.ru/MARKETOUT-34120

        """

        # 1. Без флага market_local_offers_boost_coef значение формулы для локальных офферов не бустится
        response = self.report.request_bs_pb('place=parallel&rids=54&text=sofa&debug=1')
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "showcase": {
                        "items": [
                            {"title": {"text": {"__hl": {"text": "sofa 1", "raw": True}}}},  # Значение формулы  0.8
                            {"title": {"text": {"__hl": {"text": "sofa 2", "raw": True}}}},  # Значение формулы  0.7
                            {"title": {"text": {"__hl": {"text": "sofa 3", "raw": True}}}},  # Значение формулы  0.6
                        ]
                    }
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

        # 2. С флагом market_local_offers_boost_coef значение формулы для локальных офферов бустится
        response = self.report.request_bs_pb(
            'place=parallel&rids=54&text=sofa&debug=1&rearr-factors=market_local_offers_boost_coef=2'
        )
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "showcase": {
                        "items": [
                            {
                                "title": {
                                    "text": {
                                        "__hl": {"text": "sofa 4", "raw": True}
                                    }  # Значение формулы умножается на 2, так как оффер локальный: 0.5*2=1
                                }
                            },
                            {"title": {"text": {"__hl": {"text": "sofa 1", "raw": True}}}},  # Значение формулы  0.8
                            {"title": {"text": {"__hl": {"text": "sofa 2", "raw": True}}}},  # Значение формулы 0.7
                        ]
                    }
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

    @classmethod
    def prepare_offer_specs_description(cls):
        '''
        Готовим данные для тестирования формирования строки с описанием для офферов
        Создаем модель с friendly параметрами
        Создаем 2 оффера, один привязываем к модели. К обоим офферам добавляем GuruLight параметры
        '''

        cls.index.model_description_templates += [
            ModelDescriptionTemplates(
                hid=23,
                friendlymodel=["Телефон {Retina#ifnz}{Retina:с ретиной}{#endif}", "{2SimCards:с 2-мя сим-картами}"],
            )
        ]

        # Создаем фильтры, на которые ссылаются в шаблонах
        cls.index.gltypes += [
            GLType(hid=23, param_id=100, xslname="Retina", gltype=GLType.BOOL, name="Retina"),
            GLType(hid=23, param_id=101, xslname="2SimCards", gltype=GLType.BOOL, name="2 SIM"),
        ]

        cls.index.models += [
            Model(
                hid=23,
                title="gumoful model",
                hyperid=1,
                glparams=[GLParam(param_id=100, value=1), GLParam(param_id=101, value=1)],
            ),
        ]

        cls.index.gltypes += [
            GLType(
                param_id=70001,
                hid=23,
                gltype=GLType.ENUM,
                cluster_filter=True,
                name="Weight",
                values=[GLValue(100, text="500")],
            ),
            GLType(
                param_id=70002,
                hid=23,
                gltype=GLType.ENUM,
                cluster_filter=True,
                name="Memory",
                values=[GLValue(100, text="256GB")],
            ),
            GLType(param_id=70003, hid=23, gltype=GLType.BOOL, name="WIFI"),
            GLType(param_id=70004, hid=23, gltype=GLType.BOOL, name="Radio"),
        ]

        # Создаем оффер и привязываем к ному разные параметры
        cls.index.offers += [
            Offer(
                hid=23,
                hyperid=1,
                title="Everest",
                glparams=[
                    GLParam(
                        param_id=70001, value=100
                    ),  # К описанию добавится weight: 500, потому что значение glParam это просто число
                    GLParam(param_id=70002, value=100),  # К описанию добавится 256GB
                    GLParam(param_id=70003, value=1),  # К описанию добавиться WIFI, так как значение glParam 1
                    GLParam(param_id=70004, value=0),  # К описанию ничего не добавится, так как значение glParam 0
                ],
            )
            for i in [1, 2, 3, 4]
        ]

        cls.index.offers += [
            Offer(
                hid=23,
                title="Kilimanjaro",
                glparams=[
                    GLParam(
                        param_id=70001, value=100
                    ),  # К описанию добавится weight: 500, потому что значение glParam это просто число
                    GLParam(param_id=70002, value=100),  # К описанию добавится 256GB
                    GLParam(param_id=70003, value=1),  # К описанию добавиться WIFI, так как значение glParam 1
                    GLParam(param_id=70004, value=0),  # К описанию ничего не добавится, так как значение glParam 0
                ],
            )
            for i in [1, 2, 3, 4]
        ]

    def test_offer_specs_description(self):
        '''
        https://st.yandex-team.ru/MARKETOUT-34670
        Проверяем как формируется строка с техническим описание для офферов
        Строка с описанием формируется из параметров модели для оффера. Если модели для оффера нету, то строка с описанием формируется из GuruLight параметров
        '''

        response = self.report.request_bs_pb(
            'place=parallel&text=Everest&debug=1&rearr-factors=market_parallel_add_specs_to_offers_wizard=1&show-models-specs=friendly'
        )

        self.assertFragmentIn(
            response,
            {
                "specsText": "Телефон с ретиной, с 2-мя сим-картами"  # Строка формируется из параметров модели, которая привязана к офферу
            },
        )

        response = self.report.request_bs_pb(
            'place=parallel&text=Kilimanjaro&debug=1&rearr-factors=market_parallel_add_specs_to_offers_wizard=1&show-models-specs=friendly'
        )

        self.assertFragmentIn(
            response,
            {
                "specsText": "weight: 500, 256GB, WIFI"  # Строка формируется из GuruLight параметров оффера, так как у оффера нету модели
            },
        )

    def test_pokupki_title(self):
        """Проверяем, что при market_offers_wizard_pokupki_title для текстового оферного
        используется урл Покупок в тайтле.
        https://st.yandex-team.ru/MARKETOUT-35786
        """
        market_host = "//market.yandex.ru"
        market_host_touch = "//m.market.yandex.ru"
        pokupki_host = "//pokupki.market.yandex.ru"
        pokupki_host_touch = "//m.pokupki.market.yandex.ru"
        url_suffix = "/search?text=iphone&clid=545"
        url_suffix_touch = "/search?text=iphone&clid=708"

        market_url = market_host + url_suffix
        market_url_touch = market_host_touch + url_suffix_touch
        pokupki_url = pokupki_host + url_suffix
        pokupki_url_touch = pokupki_host_touch + url_suffix_touch
        market_url_cpa = market_url + '&cpa=1'
        market_url_cpa_touch = market_url_touch + '&cpa=1'

        query = 'place=parallel&text=iphone'

        # по умолчанию нет параметра cpa=1
        response = self.report.request_bs_pb(query)
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "url": LikeUrl.of(market_url, no_params=['cpa']),
                    "urlTouch": LikeUrl.of(market_url_touch, no_params=['cpa']),
                    "snippetUrl": LikeUrl.of(market_url, no_params=['cpa']),
                    "snippetUrlTouch": LikeUrl.of(market_url_touch, no_params=['cpa']),
                    "url_for_category_name": LikeUrl.of(market_url, no_params=['cpa']),
                    "categoryUrlTouch": LikeUrl.of(market_url_touch, no_params=['cpa']),
                    "button": [
                        {
                            "url": LikeUrl.of(market_url, no_params=['cpa']),
                            "urlTouch": LikeUrl.of(market_url_touch, no_params=['cpa']),
                        }
                    ],
                    "greenUrl": [
                        {
                            "url": LikeUrl.of(market_url, no_params=['cpa']),
                            "urlTouch": LikeUrl.of(market_url_touch, no_params=['cpa']),
                            "snippetUrl": LikeUrl.of(market_url, no_params=['cpa']),
                            "snippetUrlTouch": LikeUrl.of(market_url_touch, no_params=['cpa']),
                        }
                    ],
                }
            },
        )

        # market_offers_wizard_pokupki_title=1 - подставляется хост Покупок
        response = self.report.request_bs_pb(query + '&rearr-factors=market_offers_wizard_pokupki_title=1')
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "url": LikeUrl.of(pokupki_url),
                    "urlTouch": LikeUrl.of(pokupki_url_touch),
                    "snippetUrl": LikeUrl.of(pokupki_url),
                    "snippetUrlTouch": LikeUrl.of(pokupki_url_touch),
                    "url_for_category_name": LikeUrl.of(pokupki_url),
                    "categoryUrlTouch": LikeUrl.of(pokupki_url_touch),
                    "button": [
                        {
                            "url": LikeUrl.of(pokupki_url),
                            "urlTouch": LikeUrl.of(pokupki_url_touch),
                        }
                    ],
                    "greenUrl": [
                        {
                            "url": LikeUrl.of(pokupki_url),
                            "urlTouch": LikeUrl.of(pokupki_url_touch),
                            "snippetUrl": LikeUrl.of(pokupki_url),
                            "snippetUrlTouch": LikeUrl.of(pokupki_url_touch),
                        }
                    ],
                }
            },
        )

        # market_offers_wizard_pokupki_title=2 - добавляется параметр &cpa=1
        response = self.report.request_bs_pb(query + '&rearr-factors=market_offers_wizard_pokupki_title=2')
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "url": LikeUrl.of(market_url_cpa),
                    "urlTouch": LikeUrl.of(market_url_cpa_touch),
                    "snippetUrl": LikeUrl.of(market_url_cpa),
                    "snippetUrlTouch": LikeUrl.of(market_url_cpa_touch),
                    "url_for_category_name": LikeUrl.of(market_url_cpa),
                    "categoryUrlTouch": LikeUrl.of(market_url_cpa_touch),
                    "button": [
                        {
                            "url": LikeUrl.of(market_url_cpa),
                            "urlTouch": LikeUrl.of(market_url_cpa_touch),
                        }
                    ],
                    "greenUrl": [
                        {
                            "url": LikeUrl.of(market_url_cpa),
                            "urlTouch": LikeUrl.of(market_url_cpa_touch),
                            "snippetUrl": LikeUrl.of(market_url_cpa),
                            "snippetUrlTouch": LikeUrl.of(market_url_cpa_touch),
                        }
                    ],
                }
            },
        )

        # market_offers_wizard_pokupki_title=3 - подставляется новый урл с хостом Покупок
        response = self.report.request_bs_pb(query + '&rearr-factors=market_offers_wizard_pokupki_title=3')
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "url": LikeUrl.of(market_url, no_params=['cpa']),
                    "urlTouch": LikeUrl.of(market_url_touch, no_params=['cpa']),
                    "snippetUrl": LikeUrl.of(market_url, no_params=['cpa']),
                    "snippetUrlTouch": LikeUrl.of(market_url_touch, no_params=['cpa']),
                    "url_for_category_name": LikeUrl.of(market_url, no_params=['cpa']),
                    "categoryUrlTouch": LikeUrl.of(market_url_touch, no_params=['cpa']),
                    "button": [
                        {
                            "url": LikeUrl.of(market_url, no_params=['cpa']),
                            "urlTouch": LikeUrl.of(market_url_touch, no_params=['cpa']),
                        }
                    ],
                    "greenUrl": [
                        {
                            "url": LikeUrl.of(market_url, no_params=['cpa']),
                            "urlTouch": LikeUrl.of(market_url_touch, no_params=['cpa']),
                            "snippetUrl": LikeUrl.of(market_url, no_params=['cpa']),
                            "snippetUrlTouch": LikeUrl.of(market_url_touch, no_params=['cpa']),
                        }
                    ],
                    "cpaUrl": LikeUrl.of(pokupki_url),
                }
            },
        )

        # market_offers_wizard_pokupki_title=4 - добавляется новый урл с параметром &cpa=1
        response = self.report.request_bs_pb(query + '&rearr-factors=market_offers_wizard_pokupki_title=4')
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "url": LikeUrl.of(market_url, no_params=['cpa']),
                    "urlTouch": LikeUrl.of(market_url_touch, no_params=['cpa']),
                    "snippetUrl": LikeUrl.of(market_url, no_params=['cpa']),
                    "snippetUrlTouch": LikeUrl.of(market_url_touch, no_params=['cpa']),
                    "url_for_category_name": LikeUrl.of(market_url, no_params=['cpa']),
                    "categoryUrlTouch": LikeUrl.of(market_url_touch, no_params=['cpa']),
                    "button": [
                        {
                            "url": LikeUrl.of(market_url, no_params=['cpa']),
                            "urlTouch": LikeUrl.of(market_url_touch, no_params=['cpa']),
                        }
                    ],
                    "greenUrl": [
                        {
                            "url": LikeUrl.of(market_url, no_params=['cpa']),
                            "urlTouch": LikeUrl.of(market_url_touch, no_params=['cpa']),
                            "snippetUrl": LikeUrl.of(market_url, no_params=['cpa']),
                            "snippetUrlTouch": LikeUrl.of(market_url_touch, no_params=['cpa']),
                        }
                    ],
                    "cpaUrl": LikeUrl.of(market_url_cpa),
                }
            },
        )

        # market_offers_wizard_pokupki_title=3 в таче - добавляется урл с хостом Покупок
        response = self.report.request_bs_pb(query + '&rearr-factors=market_offers_wizard_pokupki_title=3;device=touch')
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "cpaUrl": LikeUrl.of(pokupki_url_touch),
                }
            },
        )

        # market_offers_wizard_pokupki_title=4 в таче - добавляется урл с параметром &cpa=1
        response = self.report.request_bs_pb(query + '&rearr-factors=market_offers_wizard_pokupki_title=4;device=touch')
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "cpaUrl": LikeUrl.of(market_url_cpa_touch),
                }
            },
        )

    def test_market_offers_wizard_text_cpa(self):
        """Проверяем колдунщик market_offers_wizard_text_cpa
        https://st.yandex-team.ru/MARKETOUT-36904
        """
        pokupki_url = "//pokupki.market.yandex.ru/search?text=iphone&clid=545&utm_medium=cpc&utm_referrer=wizards"
        pokupki_url_touch = (
            "//m.pokupki.market.yandex.ru/search?text=iphone&clid=708&utm_medium=cpc&utm_referrer=wizards"
        )

        market_url_cpa = "//market.yandex.ru/search?text=iphone&clid=545&cpa=1&utm_medium=cpc&utm_referrer=wizards"
        market_url_cpa_touch = (
            "//m.market.yandex.ru/search?text=iphone&clid=708&cpa=1&utm_medium=cpc&utm_referrer=wizards"
        )

        query = 'place=parallel&text=iphone&rearr-factors=market_enable_offers_wiz_text_cpa=1;'

        # market_offers_wizard_pokupki_title=5 - в колдунщике market_offers_wizard_text_cpa подставляется хост Покупок
        response = self.report.request_bs_pb(query + 'market_offers_wizard_pokupki_title=5')
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard_text_cpa": {
                    "url": LikeUrl.of(pokupki_url),
                    "urlTouch": LikeUrl.of(pokupki_url_touch),
                    "snippetUrl": LikeUrl.of(pokupki_url),
                    "snippetUrlTouch": LikeUrl.of(pokupki_url_touch),
                    "url_for_category_name": LikeUrl.of(pokupki_url),
                    "categoryUrlTouch": LikeUrl.of(pokupki_url_touch),
                    "title": "\7[Iphone\7] с покупкой на Маркете",
                    "text": [
                        {
                            "__hl": {
                                "text": "Удобные покупки с постоянной выгодой. Отзывы покупателей, выбор по параметрам.",
                                "raw": True,
                            }
                        }
                    ],
                    "button": [
                        {
                            "url": LikeUrl.of(pokupki_url),
                            "urlTouch": LikeUrl.of(pokupki_url_touch),
                        }
                    ],
                    "greenUrl": [
                        {
                            "text": "Покупки",
                            "snippetText": "Покупки",
                            "url": LikeUrl.of(pokupki_url),
                            "urlTouch": LikeUrl.of(pokupki_url_touch),
                            "snippetUrl": LikeUrl.of(pokupki_url),
                            "snippetUrlTouch": LikeUrl.of(pokupki_url_touch),
                        }
                    ],
                    "snippetPicture": NoKey("snippetPicture"),
                    "warning": NoKey("warning"),
                    "warningType": NoKey("warningType"),
                    "textWhenPicture": NoKey("textWhenPicture"),
                    "sitelinks": NoKey("sitelinks"),
                    "geo": NoKey("geo"),
                }
            },
        )

        # market_offers_wizard_pokupki_title=6 - в колдунщике market_offers_wizard_text_cpa добавляется параметр &cpa=1
        response = self.report.request_bs_pb(query + 'market_offers_wizard_pokupki_title=6')
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard_text_cpa": {
                    "url": LikeUrl.of(market_url_cpa),
                    "urlTouch": LikeUrl.of(market_url_cpa_touch),
                    "snippetUrl": LikeUrl.of(market_url_cpa),
                    "snippetUrlTouch": LikeUrl.of(market_url_cpa_touch),
                    "url_for_category_name": LikeUrl.of(market_url_cpa),
                    "categoryUrlTouch": LikeUrl.of(market_url_cpa_touch),
                    "title": "\7[Iphone\7] с покупкой на Маркете",
                    "text": [
                        {
                            "__hl": {
                                "text": "Удобные покупки с постоянной выгодой. Отзывы покупателей, выбор по параметрам.",
                                "raw": True,
                            }
                        }
                    ],
                    "button": [
                        {
                            "url": LikeUrl.of(market_url_cpa),
                            "urlTouch": LikeUrl.of(market_url_cpa_touch),
                        }
                    ],
                    "greenUrl": [
                        {
                            "text": "Iphone",
                            "snippetText": "Iphone",
                            "url": LikeUrl.of(market_url_cpa),
                            "urlTouch": LikeUrl.of(market_url_cpa_touch),
                            "snippetUrl": LikeUrl.of(market_url_cpa),
                            "snippetUrlTouch": LikeUrl.of(market_url_cpa_touch),
                        }
                    ],
                    "snippetPicture": NoKey("snippetPicture"),
                    "warning": NoKey("warning"),
                    "warningType": NoKey("warningType"),
                    "textWhenPicture": NoKey("textWhenPicture"),
                    "sitelinks": NoKey("sitelinks"),
                    "geo": NoKey("geo"),
                }
            },
        )

        # Дополнительно обогащение под флагом market_text_cpa_offers_wizard_enrichment
        for pokupki_title_flag in ['market_offers_wizard_pokupki_title=5', 'market_offers_wizard_pokupki_title=6']:
            response = self.report.request_bs_pb(
                query + 'market_text_cpa_offers_wizard_enrichment=1;' + pokupki_title_flag
            )
            self.assertFragmentIn(
                response,
                {
                    "market_offers_wizard_text_cpa": {
                        "sitelinks": [
                            {
                                "text": "Акции",
                                "url": "//pokupki.market.yandex.ru/deals?lr=0&utm_medium=cpc&utm_referrer=wizards&clid=545",
                                "urlTouch": "//m.pokupki.market.yandex.ru/deals?lr=0&utm_medium=cpc&utm_referrer=wizards&clid=708",
                            },
                            {
                                "text": "Бесплатная доставка",
                                "url": "//pokupki.market.yandex.ru/special/free-delivery-1?lr=0&utm_medium=cpc&utm_referrer=wizards&clid=545",
                                "urlTouch": "//m.pokupki.market.yandex.ru/special/free-delivery-1?lr=0&utm_medium=cpc&utm_referrer=wizards&clid=708",
                            },
                            {
                                "text": "Кешбэк от Яндекс.Плюс",
                                "url": "//market.yandex.ru/cashback?lr=0&utm_medium=cpc&utm_referrer=wizards&clid=545",
                                "urlTouch": "//m.market.yandex.ru/cashback?lr=0&utm_medium=cpc&utm_referrer=wizards&clid=708",
                            },
                        ]
                    }
                },
            )

            response = self.report.request_bs_pb(
                query + 'device=touch;market_text_cpa_offers_wizard_enrichment=1;' + pokupki_title_flag
            )
            self.assertFragmentIn(
                response,
                {
                    "market_offers_wizard_text_cpa": {
                        "title": "\7[Iphone\7] с покупкой на Маркете, удобная доставка, выгодные цены",
                    }
                },
            )

    def test_cpa_filter_in_title_url(self):
        """Проверяем что под флагом market_cpa_filter_in_wizard_title_urls=1 в ссылку тайтла добавляется фильтр CPA
        https://st.yandex-team.ru/MARKETOUT-36793
        https://st.yandex-team.ru/MARKETOUT-37552
        """
        query = (
            'place=parallel&text=iphone&rearr-factors=market_enable_offers_wiz_right_incut=1;market_enable_offers_wiz_center_incut=1;'
            'market_cpa_filter_in_wizard_title_urls=1;'
        )

        # Под флагом market_cpa_filter_in_wizard_title_urls=1 CPA фильтр добавляется во все колдунщики
        response = self.report.request_bs_pb(query)
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "url": LikeUrl.of("//market.yandex.ru/search?text=iphone&lr=0&clid=545&cpa=1"),
                    "urlTouch": LikeUrl.of("//m.market.yandex.ru/search?text=iphone&lr=0&clid=708&cpa=1"),
                }
            },
        )
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard_right_incut": {
                    "url": LikeUrl.of("//market.yandex.ru/search?text=iphone&lr=0&clid=830&cpa=1"),
                    "urlTouch": LikeUrl.of("//m.market.yandex.ru/search?text=iphone&lr=0&clid=831&cpa=1"),
                }
            },
        )
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard_center_incut": {
                    "url": LikeUrl.of("//market.yandex.ru/search?text=iphone&lr=0&clid=832&cpa=1"),
                    "urlTouch": LikeUrl.of("//m.market.yandex.ru/search?text=iphone&lr=0&clid=833&cpa=1"),
                }
            },
        )

        # Под флагом market_cpa_filter_in_wizard_title_urls_vt=market_offers_wizard_right_incut,market_offers_wizard_center_incut
        # CPA фильтр добавляется только в колдунщики market_offers_wizard_right_incut и market_offers_wizard_center_incut
        response = self.report.request_bs_pb(
            query
            + 'market_cpa_filter_in_wizard_title_urls_vt=market_offers_wizard_right_incut,market_offers_wizard_center_incut'
        )
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "url": LikeUrl.of("//market.yandex.ru/search?text=iphone&lr=0&clid=545", no_params=['cpa']),
                    "urlTouch": LikeUrl.of("//m.market.yandex.ru/search?text=iphone&lr=0&clid=708", no_params=['cpa']),
                }
            },
        )
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard_right_incut": {
                    "url": LikeUrl.of("//market.yandex.ru/search?text=iphone&lr=0&clid=830&cpa=1"),
                    "urlTouch": LikeUrl.of("//m.market.yandex.ru/search?text=iphone&lr=0&clid=831&cpa=1"),
                }
            },
        )
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard_center_incut": {
                    "url": LikeUrl.of("//market.yandex.ru/search?text=iphone&lr=0&clid=832&cpa=1"),
                    "urlTouch": LikeUrl.of("//m.market.yandex.ru/search?text=iphone&lr=0&clid=833&cpa=1"),
                }
            },
        )

    def test_category_id_field(self):
        """Проверяем наличие поля categoryId у оффера
        https://st.yandex-team.ru/MARKETOUT-37684
        """

        response = self.report.request_bs_pb("place=parallel&text=Лекарство")
        self.assertFragmentIn(response, {"market_offers_wizard": {"showcase": {"items": [{"categoryId": 202}]}}})

    def test_text_offers_wizard_disable(self):

        request = 'place=parallel&text=iphone'
        response = self.report.request_bs_pb(request)

        self.assertFragmentIn(
            response,
            {
                'market_offers_wizard': {
                    "url": LikeUrl.of("//market.yandex.ru/search"),
                },
            },
        )
        self.assertFragmentIn(
            response,
            {
                'market_offers_wizard_center_incut': {
                    "url": LikeUrl.of("//market.yandex.ru/search"),
                },
            },
            preserve_order=True,
        )

        request = 'place=parallel&text=iphone&rearr-factors=market_enable_text_offers_wizard=0'
        response = self.report.request_bs_pb(request)

        self.assertFragmentNotIn(
            response,
            {
                'market_offers_wizard': {
                    "url": LikeUrl.of("//market.yandex.ru/search"),
                },
            },
        )
        self.assertFragmentIn(
            response,
            {
                'market_offers_wizard_center_incut': {
                    "url": LikeUrl.of("//market.yandex.ru/search"),
                },
            },
            preserve_order=True,
        )

    @classmethod
    def prepare_cpc_incut_fix_auction_sorting(cls):
        cls.index.shops += [
            Shop(fesh=1, priority_region=213),
            Shop(fesh=2, priority_region=213),
            Shop(fesh=3, priority_region=213),
            Shop(fesh=4, priority_region=213),
            Shop(fesh=5, priority_region=213),
        ]

        cls.index.offers += [
            Offer(title='Наушники JBL 1', ts=11, bid=10, fee=150, fesh=1),
            Offer(title='Наушники JBL 2', ts=12, bid=20, fee=140, fesh=2),
            Offer(title='Наушники JBL 3', ts=13, bid=30, fee=130, fesh=3),
            Offer(title='Наушники JBL 4', ts=14, bid=40, fee=120, fesh=4),
            Offer(title='Наушники JBL 5', ts=15, bid=50, fee=110, fesh=5),
        ]

        # Base formula
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 11).respond(0.307)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 12).respond(0.305)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 13).respond(0.304)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 14).respond(0.303)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 15).respond(0.301)

        # Meta formula
        cls.matrixnet.on_place(MnPlace.INCUT_META, 11).respond(0.5)
        cls.matrixnet.on_place(MnPlace.INCUT_META, 12).respond(0.4)
        cls.matrixnet.on_place(MnPlace.INCUT_META, 13).respond(0.3)
        cls.matrixnet.on_place(MnPlace.INCUT_META, 14).respond(0.2)
        cls.matrixnet.on_place(MnPlace.INCUT_META, 15).respond(0.1)

    def test_cpc_incut_fix_auction_sorting(self):
        """Проверяем, что под флагом market_cpc_incut_fix_auction_sorting=1
        офферы в CPC офферной врезке отсортрованы по аукциону
        https://st.yandex-team.ru/MARKETOUT-39599
        """
        request = 'place=parallel&text=Наушники+JBL&rids=213&trace_wizard=1&debug=1&rearr-factors=market_offers_wizard_incut_url_type=External;'

        # Под флагом market_cpc_incut_fix_auction_sorting=1 офферы отсортированы по аукциону. Флаг market_cpc_incut_fix_auction_sorting расскатан
        response = self.report.request_bs(request + 'market_cpc_incut_fix_auction_sorting=1')
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": [
                    {
                        "showcase": {
                            "items": [
                                {
                                    "title": {
                                        "text": {"__hl": {"text": "Наушники JBL 5", "raw": True}},
                                        "urlForCounter": Contains(
                                            "//market-click2.yandex.ru/redir/dtype=market/cp=44/cb=50/"
                                        ),
                                    }
                                },
                                {
                                    "title": {
                                        "text": {"__hl": {"text": "Наушники JBL 4", "raw": True}},
                                        "urlForCounter": Contains(
                                            "//market-click2.yandex.ru/redir/dtype=market/cp=32/cb=40/"
                                        ),
                                    }
                                },
                                {
                                    "title": {
                                        "text": {"__hl": {"text": "Наушники JBL 3", "raw": True}},
                                        "urlForCounter": Contains(
                                            "//market-click2.yandex.ru/redir/dtype=market/cp=22/cb=30/"
                                        ),
                                    }
                                },
                            ]
                        }
                    }
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )

        self.assertFragmentIn(response, '10 1 Top 4 MatrixNet sum: 1.213')  # Base 0.301 + 0.303 + 0.304 + 0.305
        self.assertFragmentIn(response, '10 1 Top 3 meta MatrixNet sum: 0.6')  # Meta 0.1 + 0.2 + 0.3

        debug_response = response.extract_debug_response()
        self.assertFragmentIn(
            debug_response, '<how><parallel>.*search_auction_params.*type: SAT_CPC.*</parallel></how>', use_regex=True
        )
        self.assertFragmentIn(
            debug_response, '<trace>.*Using search auction with parameters:.*auction type: 0.*</trace>', use_regex=True
        )  # type SAT_CPC

        self.show_log.expect(title="Наушники JBL 5", bid=50, click_price=44, position=1)
        self.show_log.expect(title="Наушники JBL 4", bid=40, click_price=32, position=2)
        self.show_log.expect(title="Наушники JBL 3", bid=30, click_price=22, position=3)

        self.click_log.expect(ClickType.EXTERNAL, cb=50, cp=44, position=1)
        self.click_log.expect(ClickType.EXTERNAL, cb=40, cp=32, position=2)
        self.click_log.expect(ClickType.EXTERNAL, cb=30, cp=22, position=3)

    def test_show_moderation_flags(self):
        """Проверяем, что под флагом возвращаются moderation_flags в офферах врезки
        https://st.yandex-team.ru/MARKETOUT-39384
        """
        rearr_factors = [
            'market_parallel_improved_warning_filtering=1',
            'market_parallel_show_moderation_flags=1',
        ]
        request = 'place=parallel&text=Детская еда с рождения&rearr-factors={}'.format(';'.join(rearr_factors))
        response = self.report.request_bs_pb(request)
        self.assertFragmentIn(
            response,
            {
                'market_offers_wizard': {
                    'showcase': {
                        'items': [
                            {
                                'title': {'text': {'__hl': {'text': 'Детская еда c рождения 1-е поколение'}}},
                                'moderation_flags': [10001],
                            }
                        ]
                    }
                }
            },
        )

    def test_shop_id_in_offers_incut(self):
        """Проверяем добавление идентификатора магазина во врезку офферного колдунщика
        https://st.yandex-team.ru/MARKETOUT-39681
        """
        response = self.report.request_bs_pb('place=parallel&text=Наушники JBL')
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "showcase": {
                        "items": [
                            {
                                "title": {
                                    "text": {"__hl": {"text": "Наушники JBL 1", "raw": True}},
                                },
                                "greenUrl": {
                                    "text": "SHOP-1",
                                },
                                "shopId": 1,
                            },
                            {
                                "title": {
                                    "text": {"__hl": {"text": "Наушники JBL 2", "raw": True}},
                                },
                                "greenUrl": {
                                    "text": "SHOP-2",
                                },
                                "shopId": 2,
                            },
                            {
                                "title": {
                                    "text": {"__hl": {"text": "Наушники JBL 3", "raw": True}},
                                },
                                "greenUrl": {
                                    "text": "SHOP-3",
                                },
                                "shopId": 3,
                            },
                        ]
                    }
                }
            },
        )


if __name__ == '__main__':
    main()
