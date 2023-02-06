#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import BlueOffer, HyperCategory, MarketSku, MnPlace, Model, NavCategory, Offer, Region, Shop
from core.testcase import TestCase, main
from core.matcher import LikeUrl, Contains, EmptyList, NotEmptyList, NoKey


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.index.regiontree += [Region(rid=213, name='Москва', genitive='Москвы', locative='Москве', preposition='в')]
        cls.index.shops += [
            Shop(fesh=1, priority_region=213),
            Shop(fesh=2, priority_region=213),
            Shop(fesh=3, priority_region=213),
            Shop(fesh=4, priority_region=213),
        ]
        cls.index.navtree += [NavCategory(nid=100, hid=100)]
        cls.index.offers += [
            Offer(
                title='kiyanka 1',
                fesh=1,
                ts=1,
                hyperid=1,
                url='http://www.shop-1.ru/kiyanka',
                waremd5='m5sFBy3SDRzD5UYHuEyImw',
                hid=100,
            ),
            Offer(title='kiyanka 2', fesh=2, ts=2),
            Offer(title='kiyanka 3', fesh=3, ts=3),
            Offer(title='kiyanka 4', fesh=4, ts=4),
        ]

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 1).respond(0.9)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 2).respond(0.8)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 3).respond(0.7)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 4).respond(0.6)

        cls.index.mskus += [
            MarketSku(
                sku=10001, title='kiyanka sku 1', blue_offers=[BlueOffer(ts=10001, waremd5='76q6e2qfUDADMzcHSIS34A')]
            ),
            MarketSku(sku=10002, title='kiyanka sku 2', blue_offers=[BlueOffer(ts=10002)]),
            MarketSku(sku=10003, title='kiyanka sku 3', blue_offers=[BlueOffer(ts=10003)]),
        ]

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 10001).respond(0.19)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 10002).respond(0.18)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 10003).respond(0.17)

    def test_market_offers_adg_wizard(self):
        """Проверяем наличие колдунщика офферной Рекламной Галереи
        под флагом market_enable_offers_adg_wiz.
        https://st.yandex-team.ru/MARKETOUT-24441
        """
        # Проставляем market_region_in_offers_incut_urls=0, так как он по умолчанию true
        response = self.report.request_bs_pb(
            'place=parallel&text=kiyanka&rids=213'
            '&rearr-factors=market_offers_incut_threshold_disable=1;'
            'market_enable_offers_adg_wiz=1;market_region_in_offers_incut_urls=0'
        )
        self.assertFragmentIn(
            response,
            {
                "market_offers_adg_wizard": {
                    "type": "market_constr",
                    "subtype": "market_offers_adg_wizard",
                    "counter": {"path": "/snippet/market/market_offers_adg_wizard"},
                    "title": "\7[Kiyanka\7] в Москве",
                    "snippetTitle": {"__hl": {"text": "Kiyanka", "raw": True}},
                    "url_for_category_name": LikeUrl.of(
                        "//market.yandex.ru/search?text=kiyanka&clid=913&lr=213&utm_medium=cpc&utm_referrer=wizards",
                        ignore_len=False,
                    ),
                    "url": LikeUrl.of(
                        "//market.yandex.ru/search?text=kiyanka&clid=913&lr=213&utm_medium=cpc&utm_referrer=wizards",
                        ignore_len=False,
                        ignore_params=['rs'],
                    ),
                    "urlTouch": LikeUrl.of(
                        "//m.market.yandex.ru/search?text=kiyanka&clid=919&lr=213&utm_medium=cpc&utm_referrer=wizards",
                        ignore_len=False,
                        ignore_params=['rs'],
                    ),
                    "snippetUrl": LikeUrl.of(
                        "//market.yandex.ru/search?text=kiyanka&clid=913&lr=213&utm_medium=cpc&utm_referrer=wizards",
                        ignore_len=False,
                    ),
                    "snippetUrlTouch": LikeUrl.of(
                        "//m.market.yandex.ru/search?text=kiyanka&clid=919&lr=213&utm_medium=cpc&utm_referrer=wizards",
                        ignore_len=False,
                    ),
                    "adGUrl": LikeUrl.of(
                        "//market.yandex.ru/search?text=kiyanka&clid=913&lr=213&utm_medium=cpc&utm_referrer=wizards",
                        ignore_len=False,
                        ignore_params=['rs'],
                    ),
                    "adGUrlTouch": LikeUrl.of(
                        "//m.market.yandex.ru/search?text=kiyanka&clid=919&lr=213&utm_medium=cpc&utm_referrer=wizards",
                        ignore_len=False,
                        ignore_params=['rs'],
                    ),
                    "snippetAdGUrl": LikeUrl.of(
                        "//market.yandex.ru/search?text=kiyanka&clid=913&lr=213&utm_medium=cpc&utm_referrer=wizards",
                        ignore_len=False,
                    ),
                    "snippetAdGUrlTouch": LikeUrl.of(
                        "//m.market.yandex.ru/search?text=kiyanka&clid=919&lr=213&utm_medium=cpc&utm_referrer=wizards",
                        ignore_len=False,
                    ),
                    "greenUrl": [
                        {
                            "text": "Яндекс.Маркет",
                            "url": LikeUrl.of(
                                "//market.yandex.ru?clid=913&lr=213&utm_medium=cpc&utm_referrer=wizards",
                                ignore_len=False,
                            ),
                            "urlTouch": LikeUrl.of(
                                "//m.market.yandex.ru?clid=919&lr=213&utm_medium=cpc&utm_referrer=wizards",
                                ignore_len=False,
                            ),
                            "adGUrl": LikeUrl.of(
                                "//market.yandex.ru?clid=913&lr=213&utm_medium=cpc&utm_referrer=wizards",
                                ignore_len=False,
                            ),
                            "adGUrlTouch": LikeUrl.of(
                                "//m.market.yandex.ru?clid=919&lr=213&utm_medium=cpc&utm_referrer=wizards",
                                ignore_len=False,
                            ),
                            "snippetText": "Яндекс.Маркет",
                            "snippetUrl": LikeUrl.of(
                                "//market.yandex.ru?clid=913&lr=213&utm_medium=cpc&utm_referrer=wizards",
                                ignore_len=False,
                            ),
                            "snippetUrlTouch": LikeUrl.of(
                                "//m.market.yandex.ru?clid=919&lr=213&utm_medium=cpc&utm_referrer=wizards",
                                ignore_len=False,
                            ),
                            "snippetAdGUrl": LikeUrl.of(
                                "//market.yandex.ru?clid=913&lr=213&utm_medium=cpc&utm_referrer=wizards",
                                ignore_len=False,
                            ),
                            "snippetAdGUrlTouch": LikeUrl.of(
                                "//m.market.yandex.ru?clid=919&lr=213&utm_medium=cpc&utm_referrer=wizards",
                                ignore_len=False,
                            ),
                        },
                        {
                            "text": "Kiyanka в Москве",
                            "url": LikeUrl.of(
                                "//market.yandex.ru/search?text=kiyanka&clid=913&lr=213&utm_medium=cpc&utm_referrer=wizards",
                                ignore_len=False,
                            ),
                            "urlTouch": LikeUrl.of(
                                "//m.market.yandex.ru/search?text=kiyanka&clid=919&lr=213&utm_medium=cpc&utm_referrer=wizards",
                                ignore_len=False,
                            ),
                            "adGUrl": LikeUrl.of(
                                "//market.yandex.ru/search?text=kiyanka&clid=913&lr=213&utm_medium=cpc&utm_referrer=wizards",
                                ignore_len=False,
                            ),
                            "adGUrlTouch": LikeUrl.of(
                                "//m.market.yandex.ru/search?text=kiyanka&clid=919&lr=213&utm_medium=cpc&utm_referrer=wizards",
                                ignore_len=False,
                            ),
                            "snippetText": "Kiyanka",
                            "snippetUrl": LikeUrl.of(
                                "//market.yandex.ru/search?text=kiyanka&clid=913&lr=213&utm_medium=cpc&utm_referrer=wizards",
                                ignore_len=False,
                            ),
                            "snippetUrlTouch": LikeUrl.of(
                                "//m.market.yandex.ru/search?text=kiyanka&clid=919&lr=213&utm_medium=cpc&utm_referrer=wizards",
                                ignore_len=False,
                            ),
                            "snippetAdGUrl": LikeUrl.of(
                                "//market.yandex.ru/search?text=kiyanka&clid=913&lr=213&utm_medium=cpc&utm_referrer=wizards",
                                ignore_len=False,
                            ),
                            "snippetAdGUrlTouch": LikeUrl.of(
                                "//m.market.yandex.ru/search?text=kiyanka&clid=919&lr=213&utm_medium=cpc&utm_referrer=wizards",
                                ignore_len=False,
                            ),
                        },
                    ],
                    "favicon": {"faviconDomain": "market.yandex.ru"},
                    "text": [
                        {
                            "__hl": {
                                "text": "4 магазина. Выбор по параметрам. Доставка из магазинов Москвы и других регионов.",
                                "raw": True,
                            }
                        }
                    ],
                    "button": [
                        {
                            "text": "Еще 4 предложения",
                            "urlTouch": LikeUrl.of(
                                "//m.market.yandex.ru/search?text=kiyanka&clid=919&lr=213&utm_medium=cpc&utm_referrer=wizards",
                                ignore_len=False,
                                ignore_params=['rs'],
                            ),
                            "url": LikeUrl.of(
                                "//market.yandex.ru/search?text=kiyanka&clid=913&lr=213&utm_medium=cpc&utm_referrer=wizards",
                                ignore_len=False,
                                ignore_params=['rs'],
                            ),
                            "adGUrl": LikeUrl.of(
                                "//market.yandex.ru/search?text=kiyanka&clid=913&lr=213&utm_medium=cpc&utm_referrer=wizards",
                                ignore_len=False,
                                ignore_params=['rs'],
                            ),
                            "adGUrlTouch": LikeUrl.of(
                                "//m.market.yandex.ru/search?text=kiyanka&clid=919&lr=213&utm_medium=cpc&utm_referrer=wizards",
                                ignore_len=False,
                                ignore_params=['rs'],
                            ),
                        }
                    ],
                    "offer_count": 4,
                    "showcase": {
                        "items": [
                            {
                                "thumb": {
                                    "source": Contains("//avatars.mdst.yandex.net/get-marketpic/"),
                                    "retinaSource": Contains("//avatars.mdst.yandex.net/get-marketpic/"),
                                    "height": "100",
                                    "width": "100",
                                    "url": LikeUrl.of(
                                        "//market.yandex.ru/offer/m5sFBy3SDRzD5UYHuEyImw?text=kiyanka&lr=213&hid=100&hyperid=1&modelid=1&nid=100&clid=913&utm_medium=cpc&utm_referrer=wizards&utm_medium=cpc&utm_referrer=wizards",  # noqa
                                        ignore_len=False,
                                        ignore_params=['rs', 'cpc'],
                                    ),
                                    "urlTouch": LikeUrl.of(
                                        "//market.yandex.ru/offer/m5sFBy3SDRzD5UYHuEyImw?text=kiyanka&lr=213&hid=100&hyperid=1&modelid=1&nid=100&clid=919&utm_medium=cpc&utm_referrer=wizards&utm_medium=cpc&utm_referrer=wizards",  # noqa
                                        ignore_len=False,
                                        ignore_params=['rs', 'cpc'],
                                    ),
                                    "adGUrl": LikeUrl.of(
                                        "//market.yandex.ru/search?text=kiyanka&lr=213&cvredirect=0&clid=913&utm_medium=cpc&utm_referrer=wizards&utm_medium=cpc&utm_referrer=wizards",
                                        ignore_len=False,
                                        ignore_params=['rs'],
                                    ),
                                    "adGUrlTouch": LikeUrl.of(
                                        "//market.yandex.ru/search?text=kiyanka&lr=213&cvredirect=0&clid=919&utm_medium=cpc&utm_referrer=wizards&utm_medium=cpc&utm_referrer=wizards",
                                        ignore_len=False,
                                        ignore_params=['rs'],
                                    ),
                                    "urlForCounter": LikeUrl.of(
                                        "//market.yandex.ru/search?text=kiyanka&cvredirect=0&clid=913&utm_medium=cpc&utm_referrer=wizards",
                                        ignore_len=False,
                                        ignore_params=['rs'],
                                    ),
                                    "offercardUrl": Contains("//market-click2.yandex.ru/redir/dtype=offercard/"),
                                },
                                "price": {"type": "average", "priceMax": "100", "currency": "RUR"},
                                "title": {
                                    "text": {"__hl": {"text": "kiyanka 1", "raw": True}},
                                    "url": LikeUrl.of(
                                        "//market.yandex.ru/offer/m5sFBy3SDRzD5UYHuEyImw?text=kiyanka&lr=213&hid=100&hyperid=1&modelid=1&nid=100&clid=913&utm_medium=cpc&utm_referrer=wizards&utm_medium=cpc&utm_referrer=wizards",  # noqa
                                        ignore_len=False,
                                        ignore_params=['rs', 'cpc'],
                                    ),
                                    "urlTouch": LikeUrl.of(
                                        "//market.yandex.ru/offer/m5sFBy3SDRzD5UYHuEyImw?text=kiyanka&lr=213&hid=100&hyperid=1&modelid=1&nid=100&clid=919&utm_medium=cpc&utm_referrer=wizards&utm_medium=cpc&utm_referrer=wizards",  # noqa
                                        ignore_len=False,
                                        ignore_params=['rs', 'cpc'],
                                    ),
                                    "adGUrl": LikeUrl.of(
                                        "//market.yandex.ru/search?text=kiyanka&lr=213&cvredirect=0&clid=913&utm_medium=cpc&utm_referrer=wizards&utm_medium=cpc&utm_referrer=wizards",
                                        ignore_len=False,
                                        ignore_params=['rs'],
                                    ),
                                    "adGUrlTouch": LikeUrl.of(
                                        "//market.yandex.ru/search?text=kiyanka&lr=213&cvredirect=0&clid=919&utm_medium=cpc&utm_referrer=wizards&utm_medium=cpc&utm_referrer=wizards",
                                        ignore_len=False,
                                        ignore_params=['rs'],
                                    ),
                                    "urlForCounter": LikeUrl.of(
                                        "//market.yandex.ru/search?text=kiyanka&cvredirect=0&clid=913&utm_medium=cpc&utm_referrer=wizards",
                                        ignore_len=False,
                                        ignore_params=['rs'],
                                    ),
                                    "offercardUrl": Contains("//market-click2.yandex.ru/redir/dtype=offercard/"),
                                },
                                "greenUrl": {
                                    "text": "SHOP-1",
                                    "url": LikeUrl.of(
                                        "//market.yandex.ru/shop--shop-1/1/reviews?cmid=m5sFBy3SDRzD5UYHuEyImw&clid=913&lr=213&utm_medium=cpc&utm_referrer=wizards",
                                        ignore_len=False,
                                    ),
                                    "urlTouch": LikeUrl.of(
                                        "//m.market.yandex.ru/grades-shop.xml?shop_id=1&cmid=m5sFBy3SDRzD5UYHuEyImw&clid=919&lr=213&utm_medium=cpc&utm_referrer=wizards",
                                        ignore_len=False,
                                    ),
                                    "adGUrl": LikeUrl.of(
                                        "//market.yandex.ru/shop--shop-1/1/reviews?cmid=m5sFBy3SDRzD5UYHuEyImw&clid=913&lr=213&utm_medium=cpc&utm_referrer=wizards",
                                        ignore_len=False,
                                    ),
                                    "adGUrlTouch": LikeUrl.of(
                                        "//m.market.yandex.ru/grades-shop.xml?shop_id=1&cmid=m5sFBy3SDRzD5UYHuEyImw&clid=919&lr=213&utm_medium=cpc&utm_referrer=wizards",
                                        ignore_len=False,
                                    ),
                                },
                            }
                        ],
                        "isAdv": 1,
                    },
                }
            },
        )

        # Проверяем что офферный колдунщик формируется
        self.assertFragmentIn(response, {"market_offers_wizard": {}})

        self.access_log.expect(
            wizards=Contains('market_offers_adg_wizard'), wizard_elements=Contains('market_offers_adg_wizard')
        )

    def test_market_offers_adg_wizard_threshold(self):
        """Проверяем отдельный порог ТОП-4 по базовой формуле на формирование вьютайпа офферной РГ
        под флагом market_adg_offers_incut_threshold
        https://st.yandex-team.ru/MARKETOUT-26291
        """

        # С порогами market_offers_incut_threshold=0 и market_adg_offers_incut_threshold=0
        # формируются вьютайпы market_offers_wizard с врезкой и market_offers_adg_wizard
        response = self.report.request_bs_pb(
            'place=parallel&text=kiyanka&rids=213'
            '&rearr-factors=market_offers_incut_threshold=0;'
            'market_enable_offers_adg_wiz=1;market_adg_offers_incut_threshold=0'
        )
        self.assertFragmentIn(response, {"market_offers_wizard": {"showcase": {"items": NotEmptyList()}}})
        self.assertFragmentIn(response, {"market_offers_adg_wizard": {"showcase": {"items": NotEmptyList()}}})

        # С порогами market_offers_incut_threshold=10 и market_adg_offers_incut_threshold=0
        # формируются вьютайпы market_offers_wizard без врезки и market_offers_adg_wizard
        response = self.report.request_bs_pb(
            'place=parallel&text=kiyanka&rids=213'
            '&rearr-factors=market_offers_incut_threshold=10;'
            'market_enable_offers_adg_wiz=1;market_adg_offers_incut_threshold=0'
            '&trace_wizard=1'
        )
        self.assertFragmentIn(response, {"market_offers_wizard": {"showcase": {"items": EmptyList()}}})
        self.assertFragmentIn(response, {"market_offers_adg_wizard": {"showcase": {"items": NotEmptyList()}}})
        self.assertIn('Top 4 MatrixNet sum: 3', response.get_trace_wizard())
        self.assertIn(
            'OffersWizard.TopOffersMnValue.Base.Threshold: market_offers_incut_threshold=10',
            response.get_trace_wizard(),
        )
        self.assertIn('Did not pass: top 4 MatrixNet sum is too low', response.get_trace_wizard())
        self.assertIn('Offers AdG wizard Top 4 MatrixNet sum: 3', response.get_trace_wizard())
        self.assertIn(
            'OffersWizard.TopOffersMnValue.AdGIncut.Meta.Threshold: market_adg_offers_incut_threshold=0',
            response.get_trace_wizard(),
        )
        self.assertNotIn('Offers AdG wizard did not pass: top 4 MatrixNet sum is too low', response.get_trace_wizard())

        # С порогами market_offers_incut_threshold=0 и market_adg_offers_incut_threshold=10
        # формируtтся вьютайп market_offers_wizard c врезкjq, market_offers_adg_wizard не формируется
        response = self.report.request_bs_pb(
            'place=parallel&text=kiyanka&rids=213'
            '&rearr-factors=market_offers_incut_threshold=0;'
            'market_enable_offers_adg_wiz=1;market_adg_offers_incut_threshold=10'
            '&trace_wizard=1'
        )
        self.assertFragmentIn(response, {"market_offers_wizard": {"showcase": {"items": NotEmptyList()}}})
        self.assertFragmentNotIn(response, {"market_offers_adg_wizard": {}})
        self.assertIn('Top 4 MatrixNet sum: 3', response.get_trace_wizard())
        self.assertIn(
            'OffersWizard.TopOffersMnValue.Base.Threshold: market_offers_incut_threshold=0', response.get_trace_wizard()
        )
        self.assertNotIn('Did not pass: top 4 MatrixNet sum is too low', response.get_trace_wizard())
        self.assertIn('Offers AdG wizard Top 4 MatrixNet sum: 3', response.get_trace_wizard())
        self.assertIn(
            'OffersWizard.TopOffersMnValue.AdGIncut.Meta.Threshold: market_adg_offers_incut_threshold=10',
            response.get_trace_wizard(),
        )
        self.assertIn('Offers AdG wizard did not pass: top 4 MatrixNet sum is too low', response.get_trace_wizard())

    def test_blue_offers_in_adg_wizard(self):
        """Проверяем приоритетный показ синих офферов в РГ
        https://st.yandex-team.ru/MARKETOUT-24401
        """
        request = (
            'place=parallel&text=kiyanka&rearr-factors=market_offers_incut_threshold_disable=1;'
            'market_parallel_use_blue_base_search=0'
        )

        # Под флагом market_adg_blue_offers_count=1 добавляется 1 синий оффер
        for touch_flag, clid, host in [
            ('', 913, '//market.yandex.ru'),
            (';offers_touch=1&touch=1', 919, '//m.market.yandex.ru'),
        ]:
            response = self.report.request_bs_pb(
                request + '&rearr-factors=market_enable_offers_adg_wiz=1;market_adg_blue_offers_count=1' + touch_flag
            )
            self.assertFragmentIn(
                response,
                {
                    "market_offers_adg_wizard": {
                        "showcase": {
                            "items": [
                                {
                                    "title": {
                                        "text": {"__hl": {"text": "kiyanka sku 1", "raw": True}},
                                        "url": LikeUrl.of(
                                            host + "/offer/76q6e2qfUDADMzcHSIS34A?text=kiyanka&lr=0&clid=913"
                                        ),
                                        "adGUrl": LikeUrl.of(host + "/search?text=kiyanka&lr=0&cvredirect=0&clid=913"),
                                        "urlTouch": LikeUrl.of(
                                            host + "/offer/76q6e2qfUDADMzcHSIS34A?text=kiyanka&lr=0&clid=919"
                                        ),
                                        "adGUrlTouch": LikeUrl.of(
                                            host + "/search?text=kiyanka&lr=0&cvredirect=0&clid=919"
                                        ),
                                        "offercardUrl": LikeUrl.of(
                                            host
                                            + "/offer/76q6e2qfUDADMzcHSIS34A?text=kiyanka&lr=0&clid={0}".format(clid)
                                        ),
                                    },
                                    "thumb": {
                                        "offercardUrl": LikeUrl.of(
                                            host
                                            + "/offer/76q6e2qfUDADMzcHSIS34A?text=kiyanka&lr=0&clid={0}".format(clid)
                                        )
                                    },
                                },
                                {"title": {"text": {"__hl": {"text": "kiyanka 1", "raw": True}}}},
                                {"title": {"text": {"__hl": {"text": "kiyanka 2", "raw": True}}}},
                            ]
                        }
                    }
                },
                preserve_order=True,
            )

        # Под флагом market_adg_blue_offers_count=2 добавляется 2 синих оффера
        response = self.report.request_bs_pb(
            request + '&rearr-factors=market_enable_offers_adg_wiz=1;market_adg_blue_offers_count=2'
        )
        self.assertFragmentIn(
            response,
            {
                "market_offers_adg_wizard": {
                    "showcase": {
                        "items": [
                            {"title": {"text": {"__hl": {"text": "kiyanka sku 1", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "kiyanka sku 2", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "kiyanka 1", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "kiyanka 2", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "kiyanka 3", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "kiyanka 4", "raw": True}}}},
                        ]
                    }
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

        # Под флагом market_adg_blue_offers_count=3 добавляется 3 синих оффера
        response = self.report.request_bs_pb(
            request + '&rearr-factors=market_enable_offers_adg_wiz=1;market_adg_blue_offers_count=3'
        )
        self.assertFragmentIn(
            response,
            {
                "market_offers_adg_wizard": {
                    "showcase": {
                        "items": [
                            {"title": {"text": {"__hl": {"text": "kiyanka sku 1", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "kiyanka sku 2", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "kiyanka sku 3", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "kiyanka 1", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "kiyanka 2", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "kiyanka 3", "raw": True}}}},
                        ]
                    }
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

        # Без флага market_adg_blue_offers_count синие офферы не добавляются
        response = self.report.request_bs_pb(request + '&rearr-factors=market_enable_offers_adg_wiz=1')
        self.assertFragmentIn(
            response,
            {
                "market_offers_adg_wizard": {
                    "showcase": {
                        "items": [
                            {"title": {"text": {"__hl": {"text": "kiyanka 1", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "kiyanka 2", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "kiyanka 3", "raw": True}}}},
                        ]
                    }
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

    def test_threshold_for_blue_offers_in_adg_wizard(self):
        """Проверяем порог для приоритетного показа синих офферов в РГ
        https://st.yandex-team.ru/MARKETOUT-24401
        """
        request = (
            'place=parallel&text=kiyanka&trace_wizard=1'
            '&rearr-factors='
            'market_offers_incut_threshold_disable=1;'
            'market_parallel_use_blue_base_search=0;'
            'market_enable_offers_adg_wiz=1;'
            'market_adg_blue_offers_count=3'
        )

        # Cумма значений метаформулы синих офферов больше порога, синие офферы добавляются
        response = self.report.request_bs_pb(request + '&rearr-factors=market_blue_offers_incut_threshold=0.8')
        self.assertFragmentIn(
            response,
            {
                "market_offers_adg_wizard": {
                    "showcase": {
                        "items": [
                            {"title": {"text": {"__hl": {"text": "kiyanka sku 1", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "kiyanka sku 2", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "kiyanka sku 3", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "kiyanka 1", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "kiyanka 2", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "kiyanka 3", "raw": True}}}},
                        ]
                    }
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )
        self.assertIn('10 1 Top 3 meta MatrixNet sum for blue offers incut: 0.9', response.get_trace_wizard())
        self.assertIn(
            '10 1 OfferIncut.TopBlueOffersMnValue.Meta.Threshold: market_blue_offers_incut_threshold=0.8',
            response.get_trace_wizard(),
        )

        # Cумма значений метаформулы синих офферов меньше порога, синие офферы не добавляются
        response = self.report.request_bs_pb(request + '&rearr-factors=market_blue_offers_incut_threshold=1')
        self.assertFragmentIn(
            response,
            {
                "market_offers_adg_wizard": {
                    "showcase": {
                        "items": [
                            {"title": {"text": {"__hl": {"text": "kiyanka 1", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "kiyanka 2", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "kiyanka 3", "raw": True}}}},
                        ]
                    }
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )
        self.assertIn('10 1 Top 3 meta MatrixNet sum for blue offers incut: 0.9', response.get_trace_wizard())
        self.assertIn(
            '10 1 OfferIncut.TopBlueOffersMnValue.Meta.Threshold: market_blue_offers_incut_threshold=1',
            response.get_trace_wizard(),
        )
        self.assertIn(
            '10 4 Did not pass: top 3 meta MatrixNet sum for blue offers incut is too low', response.get_trace_wizard()
        )

    def test_model_content_in_blue_offers_in_adg_wizard(self):
        """Проверяем, что под флагом market_offers_wizard_model_content для приоритетных синих офферов в РГ
        добавляются пустые модельные данные. Это необходимо чтобы офферы не отфильтровались на верхнем.
        https://st.yandex-team.ru/MARKETOUT-24401
        """
        empty_model_content = {
            "title": "",
            "pictureUrl": "",
            "pictureWidth": "",
            "pictureHeight": "",
            "pictureHdUrl": "",
            "rating": "",
            "opinion_count": "",
        }

        response = self.report.request_bs_pb(
            'place=parallel&text=kiyanka'
            '&rearr-factors=market_offers_incut_threshold_disable=1;'
            'market_parallel_use_blue_base_search=0;'
            'market_enable_offers_adg_wiz=1;'
            'market_adg_blue_offers_count=3;'
            'market_offers_wizard_model_content=1'
        )
        self.assertFragmentIn(
            response,
            {
                "market_offers_adg_wizard": {
                    "showcase": {
                        "items": [
                            {
                                "title": {"text": {"__hl": {"text": "kiyanka sku 1", "raw": True}}},
                                "modelContent": empty_model_content,
                            },
                            {
                                "title": {"text": {"__hl": {"text": "kiyanka sku 2", "raw": True}}},
                                "modelContent": empty_model_content,
                            },
                            {
                                "title": {"text": {"__hl": {"text": "kiyanka sku 3", "raw": True}}},
                                "modelContent": empty_model_content,
                            },
                            {"title": {"text": {"__hl": {"text": "kiyanka 1", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "kiyanka 2", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "kiyanka 3", "raw": True}}}},
                        ]
                    }
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

    @classmethod
    def prepare_implicit_model_adg_data(cls):
        cls.index.hypertree += [HyperCategory(hid=1, name='Телефоны')]
        cls.index.navtree += [NavCategory(nid=1, hid=1)]

        cls.index.shops += [
            Shop(fesh=10, priority_region=213),
            Shop(fesh=11, priority_region=213),
        ]

        cls.index.models += [
            Model(hyperid=101, ts=101, title="iphone model 1", hid=1),
            Model(hyperid=102, ts=102, title="iphone model 2", hid=1),
        ]

        cls.index.offers += [
            Offer(title='iphone offer 1', ts=111, hyperid=101, fesh=10),
            Offer(title='iphone offer 2', ts=112, hyperid=102, fesh=11),
        ]

    def test_implicit_model_adg_wizard(self):
        """Проверяем наличие колдунщика неявной модели для Рекламной Галереи
        под флагом market_enable_implicit_model_adg_wiz.
        https://st.yandex-team.ru/MARKETOUT-26580
        """
        response = self.report.request_bs_pb(
            'place=parallel&text=iphone&rids=213' '&rearr-factors=market_enable_implicit_model_adg_wiz=1'
        )
        self.assertFragmentIn(
            response,
            {
                "market_implicit_model_adg_wizard": {
                    "type": "market_constr",
                    "subtype": "market_implicit_model_adg_wizard",
                    "counter": {"path": "/snippet/market/market_implicit_model_adg_wizard"},
                    "title": "\7[Iphone\7] в Москве",
                    "url": LikeUrl.of(
                        "//market.yandex.ru/search?text=iphone&clid=915&lr=213&utm_medium=cpc&utm_referrer=wizards",
                        ignore_len=False,
                        ignore_params=['rs'],
                    ),
                    "urlTouch": LikeUrl.of(
                        "//m.market.yandex.ru/search?text=iphone&clid=921&lr=213&utm_medium=cpc&utm_referrer=wizards",
                        ignore_len=False,
                        ignore_params=['rs'],
                    ),
                    "adGUrl": LikeUrl.of(
                        "//market.yandex.ru/search?text=iphone&clid=915&lr=213&utm_medium=cpc&utm_referrer=wizards",
                        ignore_len=False,
                        ignore_params=['rs'],
                    ),
                    "adGUrlTouch": LikeUrl.of(
                        "//m.market.yandex.ru/search?text=iphone&clid=921&lr=213&utm_medium=cpc&utm_referrer=wizards",
                        ignore_len=False,
                        ignore_params=['rs'],
                    ),
                    "greenUrl": [
                        {
                            "text": "Яндекс.Маркет",
                            "url": LikeUrl.of(
                                "//market.yandex.ru?clid=915&lr=213&utm_medium=cpc&utm_referrer=wizards",
                                ignore_len=False,
                            ),
                            "urlTouch": LikeUrl.of(
                                "//m.market.yandex.ru?clid=921&lr=213&utm_medium=cpc&utm_referrer=wizards",
                                ignore_len=False,
                            ),
                            "adGUrl": LikeUrl.of(
                                "//market.yandex.ru?clid=915&lr=213&utm_medium=cpc&utm_referrer=wizards",
                                ignore_len=False,
                            ),
                            "adGUrlTouch": LikeUrl.of(
                                "//m.market.yandex.ru?clid=921&lr=213&utm_medium=cpc&utm_referrer=wizards",
                                ignore_len=False,
                            ),
                        },
                        {
                            "text": "Iphone в Москве",
                            "url": LikeUrl.of(
                                "//market.yandex.ru/search?text=iphone&clid=915&lr=213&utm_medium=cpc&utm_referrer=wizards",
                                ignore_len=False,
                                ignore_params=['rs'],
                            ),
                            "urlTouch": LikeUrl.of(
                                "//m.market.yandex.ru/search?text=iphone&clid=921&lr=213&utm_medium=cpc&utm_referrer=wizards",
                                ignore_len=False,
                                ignore_params=['rs'],
                            ),
                            "adGUrl": LikeUrl.of(
                                "//market.yandex.ru/search?text=iphone&clid=915&lr=213&utm_medium=cpc&utm_referrer=wizards",
                                ignore_len=False,
                                ignore_params=['rs'],
                            ),
                            "adGUrlTouch": LikeUrl.of(
                                "//m.market.yandex.ru/search?text=iphone&clid=921&lr=213&utm_medium=cpc&utm_referrer=wizards",
                                ignore_len=False,
                                ignore_params=['rs'],
                            ),
                        },
                    ],
                    "favicon": {"faviconDomain": "market.yandex.ru"},
                    "text": [
                        {
                            "__hl": {
                                "text": "Цены, характеристики, отзывы на iphone в Москве. Выбор по параметрам. 2 магазина. Доставка из магазинов Москвы и других регионов.",
                                "raw": True,
                            }
                        }
                    ],
                    "button": [
                        {
                            "text": "Еще 2 предложения",
                            "url": LikeUrl.of(
                                "//market.yandex.ru/search?text=iphone&clid=915&lr=213&utm_medium=cpc&utm_referrer=wizards",
                                ignore_len=False,
                                ignore_params=['rs'],
                            ),
                            "urlTouch": LikeUrl.of(
                                "//m.market.yandex.ru/search?text=iphone&clid=921&lr=213&utm_medium=cpc&utm_referrer=wizards",
                                ignore_len=False,
                                ignore_params=['rs'],
                            ),
                            "adGUrl": LikeUrl.of(
                                "//market.yandex.ru/search?text=iphone&clid=915&lr=213&utm_medium=cpc&utm_referrer=wizards",
                                ignore_len=False,
                                ignore_params=['rs'],
                            ),
                            "adGUrlTouch": LikeUrl.of(
                                "//m.market.yandex.ru/search?text=iphone&clid=921&lr=213&utm_medium=cpc&utm_referrer=wizards",
                                ignore_len=False,
                                ignore_params=['rs'],
                            ),
                        }
                    ],
                    "offer_count": 2,
                    "sitelinks": [
                        {
                            "url": LikeUrl.of(
                                "//market.yandex.ru/search?show-reviews=1&text=iphone&lr=213&clid=915&utm_medium=cpc&utm_referrer=wizards",
                                ignore_len=False,
                                ignore_params=['rs'],
                            ),
                            "urlTouch": LikeUrl.of(
                                "//m.market.yandex.ru/search?show-reviews=1&text=iphone&lr=213&clid=921&utm_medium=cpc&utm_referrer=wizards",
                                ignore_len=False,
                                ignore_params=['rs'],
                            ),
                            "adGUrl": LikeUrl.of(
                                "//market.yandex.ru/search?show-reviews=1&text=iphone&lr=213&clid=915&utm_medium=cpc&utm_referrer=wizards",
                                ignore_len=False,
                                ignore_params=['rs'],
                            ),
                            "adGUrlTouch": LikeUrl.of(
                                "//m.market.yandex.ru/search?show-reviews=1&text=iphone&lr=213&clid=921&utm_medium=cpc&utm_referrer=wizards",
                                ignore_len=False,
                                ignore_params=['rs'],
                            ),
                            "text": "Отзывы",
                        },
                        {
                            "url": LikeUrl.of(
                                "//market.yandex.ru/geo?text=iphone&lr=213&clid=915&utm_medium=cpc&utm_referrer=wizards",
                                ignore_len=False,
                                ignore_params=['rs'],
                            ),
                            "urlTouch": LikeUrl.of(
                                "//m.market.yandex.ru/geo?text=iphone&lr=213&clid=921&utm_medium=cpc&utm_referrer=wizards",
                                ignore_len=False,
                                ignore_params=['rs'],
                            ),
                            "adGUrl": LikeUrl.of(
                                "//market.yandex.ru/geo?text=iphone&lr=213&clid=915&utm_medium=cpc&utm_referrer=wizards",
                                ignore_len=False,
                                ignore_params=['rs'],
                            ),
                            "adGUrlTouch": LikeUrl.of(
                                "//m.market.yandex.ru/geo?text=iphone&lr=213&clid=921&utm_medium=cpc&utm_referrer=wizards",
                                ignore_len=False,
                                ignore_params=['rs'],
                            ),
                            "text": "На карте",
                        },
                        {
                            "url": LikeUrl.of(
                                "//market.yandex.ru/search?delivery-interval=1&text=iphone&lr=213&clid=915&utm_medium=cpc&utm_referrer=wizards",
                                ignore_len=False,
                                ignore_params=['rs'],
                            ),
                            "urlTouch": LikeUrl.of(
                                "//m.market.yandex.ru/search?delivery-interval=1&text=iphone&lr=213&clid=921&utm_medium=cpc&utm_referrer=wizards",
                                ignore_len=False,
                                ignore_params=['rs'],
                            ),
                            "adGUrl": LikeUrl.of(
                                "//market.yandex.ru/search?delivery-interval=1&text=iphone&lr=213&clid=915&utm_medium=cpc&utm_referrer=wizards",
                                ignore_len=False,
                                ignore_params=['rs'],
                            ),
                            "adGUrlTouch": LikeUrl.of(
                                "//m.market.yandex.ru/search?delivery-interval=1&text=iphone&lr=213&clid=921&utm_medium=cpc&utm_referrer=wizards",
                                ignore_len=False,
                                ignore_params=['rs'],
                            ),
                            "text": "С доставкой завтра",
                        },
                    ],
                    "shop_count": "2",
                    "model_count": "2",
                    "showcase": {
                        "items": [
                            {
                                "thumb": {
                                    "url": LikeUrl.of(
                                        "//market.yandex.ru/product--iphone-model-1/101?text=iphone&hid=1&nid=1&clid=915&lr=213&utm_medium=cpc&utm_referrer=wizards",
                                        ignore_len=False,
                                        ignore_params=['rs'],
                                    ),
                                    "urlTouch": LikeUrl.of(
                                        "//m.market.yandex.ru/product--iphone-model-1/101?text=iphone&hid=1&nid=1&clid=921&lr=213&utm_medium=cpc&utm_referrer=wizards",
                                        ignore_len=False,
                                        ignore_params=['rs'],
                                    ),
                                    "adGUrl": LikeUrl.of(
                                        "//market.yandex.ru/product--iphone-model-1/101?text=iphone&hid=1&nid=1&clid=915&lr=213&utm_medium=cpc&utm_referrer=wizards",
                                        ignore_len=False,
                                        ignore_params=['rs'],
                                    ),
                                    "adGUrlTouch": LikeUrl.of(
                                        "//m.market.yandex.ru/product--iphone-model-1/101?text=iphone&hid=1&nid=1&clid=921&lr=213&utm_medium=cpc&utm_referrer=wizards",
                                        ignore_len=False,
                                        ignore_params=['rs'],
                                    ),
                                },
                                "price": {"type": "min", "priceMin": "100", "currency": "RUR"},
                                "title": {
                                    "text": {"__hl": {"text": "iphone model 1", "raw": True}},
                                    "url": LikeUrl.of(
                                        "//market.yandex.ru/product--iphone-model-1/101?text=iphone&hid=1&nid=1&clid=915&lr=213&utm_medium=cpc&utm_referrer=wizards",
                                        ignore_len=False,
                                        ignore_params=['rs'],
                                    ),
                                    "urlTouch": LikeUrl.of(
                                        "//m.market.yandex.ru/product--iphone-model-1/101?text=iphone&hid=1&nid=1&clid=921&lr=213&utm_medium=cpc&utm_referrer=wizards",
                                        ignore_len=False,
                                        ignore_params=['rs'],
                                    ),
                                    "adGUrl": LikeUrl.of(
                                        "//market.yandex.ru/product--iphone-model-1/101?text=iphone&hid=1&nid=1&clid=915&lr=213&utm_medium=cpc&utm_referrer=wizards",
                                        ignore_len=False,
                                        ignore_params=['rs'],
                                    ),
                                    "adGUrlTouch": LikeUrl.of(
                                        "//m.market.yandex.ru/product--iphone-model-1/101?text=iphone&hid=1&nid=1&clid=921&lr=213&utm_medium=cpc&utm_referrer=wizards",
                                        ignore_len=False,
                                        ignore_params=['rs'],
                                    ),
                                },
                            }
                        ],
                    },
                }
            },
        )

        # Проверяем что колдунщик неявной модели формируется
        self.assertFragmentIn(response, {"market_implicit_model": {}})

        # Проверяем, что есть запись в access.log
        self.access_log.expect(
            wizard_elements=Contains("market_implicit_model_adg_incut"),
            wizards=Contains("market_implicit_model_adg_incut"),
        )

    @classmethod
    def prepare_blue_models_in_implicit_model_adg_data(cls):
        cls.index.models += [
            Model(title='Acuvue OASYS model 1', hyperid=111),
            Model(title='Acuvue OASYS model 2', hyperid=112),
            Model(title="Acuvue OASYS model 3", hyperid=113, has_blue_offers=True),
            Model(title="Acuvue OASYS model 4", hyperid=114, has_blue_offers=True),
        ]

        cls.index.offers += [
            Offer(title='Acuvue OASYS offer 1', hyperid=111, fesh=10),
            Offer(title='Acuvue OASYS offer 2', hyperid=112, fesh=11),
        ]

        cls.index.mskus += [
            MarketSku(sku=1130, title="Acuvue OASYS blue model 1", hyperid=113, blue_offers=[BlueOffer()]),
            MarketSku(sku=1140, title="Acuvue OASYS blue model 2", hyperid=114, blue_offers=[BlueOffer()]),
        ]

    def test_blue_models_in_implicit_model_adg(self):
        """Проверяем отсутствие в колдунщике неявной модели для Рекламной Галереи
        отдельных Синих моделей.
        https://st.yandex-team.ru/MARKETOUT-26580
        """
        response = self.report.request_bs_pb(
            'place=parallel&text=acuvue+oasys&rearr-factors='
            'market_enable_implicit_model_adg_wiz=1;'
            'market_implicit_blue_incut_model_count=3;'
        )
        white_models = [
            {
                "title": {"text": {"__hl": {"text": "Acuvue OASYS model 1", "raw": True}}},
                "is_blue": NoKey("is_blue"),
            },
            {
                "title": {"text": {"__hl": {"text": "Acuvue OASYS model 2", "raw": True}}},
                "is_blue": NoKey("is_blue"),
            },
            {
                "title": {"text": {"__hl": {"text": "Acuvue OASYS model 3", "raw": True}}},
                "is_blue": NoKey("is_blue"),
            },
            {
                "title": {"text": {"__hl": {"text": "Acuvue OASYS model 4", "raw": True}}},
                "is_blue": NoKey("is_blue"),
            },
        ]
        blue_models = [
            {
                "title": {"text": {"__hl": {"text": "Acuvue OASYS model 3", "raw": True}}},
                "is_blue": "1",
            },
            {
                "title": {"text": {"__hl": {"text": "Acuvue OASYS model 4", "raw": True}}},
                "is_blue": "1",
            },
        ]
        self.assertFragmentIn(
            response,
            {
                "market_implicit_model": {
                    "showcase": {
                        "items": white_models + blue_models,
                    }
                }
            },
            allow_different_len=False,
        )

        self.assertFragmentIn(
            response,
            {
                "market_implicit_model_adg_wizard": {
                    "showcase": {
                        "items": white_models,
                    }
                }
            },
            allow_different_len=False,
        )

    def test_adg_clids_in_offers_wizard(self):
        """Проверяем проверяем правильную расстановку clid'ов РГ в офферном колдунщике
        https://st.yandex-team.ru/MARKETOUT-27420
        """

        def do_test(extra_rearr, wizard_name, expected_clids):
            clid, clid_touch, adg_clid, adg_clid_touch = expected_clids

            response = self.report.request_bs_pb(
                'place=parallel&text=kiyanka&rids=213'
                '&rearr-factors=market_offers_incut_threshold=0;'
                'market_offers_wizard_incut_url_type=OfferCard' + extra_rearr
            )

            self.assertFragmentIn(
                response,
                {
                    wizard_name: {
                        "url": LikeUrl.of(
                            "//market.yandex.ru/search?text=kiyanka&clid={0}&lr=213&utm_medium=cpc&utm_referrer=wizards".format(
                                clid
                            ),
                            ignore_len=False,
                            ignore_params=['rs'],
                        ),
                        "urlTouch": LikeUrl.of(
                            "//m.market.yandex.ru/search?text=kiyanka&clid={0}&lr=213&utm_medium=cpc&utm_referrer=wizards".format(
                                clid_touch
                            ),
                            ignore_len=False,
                            ignore_params=['rs'],
                        ),
                        "snippetUrl": LikeUrl.of(
                            "//market.yandex.ru/search?text=kiyanka&clid={0}&lr=213&utm_medium=cpc&utm_referrer=wizards".format(
                                clid
                            ),
                            ignore_len=False,
                        ),
                        "snippetUrlTouch": LikeUrl.of(
                            "//m.market.yandex.ru/search?text=kiyanka&clid={0}&lr=213&utm_medium=cpc&utm_referrer=wizards".format(
                                clid_touch
                            ),
                            ignore_len=False,
                        ),
                        "adGUrl": LikeUrl.of(
                            "//market.yandex.ru/search?text=kiyanka&clid={0}&lr=213&utm_medium=cpc&utm_referrer=wizards".format(
                                adg_clid
                            ),
                            ignore_len=False,
                            ignore_params=['rs'],
                        ),
                        "adGUrlTouch": LikeUrl.of(
                            "//m.market.yandex.ru/search?text=kiyanka&clid={0}&lr=213&utm_medium=cpc&utm_referrer=wizards".format(
                                adg_clid_touch
                            ),
                            ignore_len=False,
                            ignore_params=['rs'],
                        ),
                        "snippetAdGUrl": LikeUrl.of(
                            "//market.yandex.ru/search?text=kiyanka&clid={0}&lr=213&utm_medium=cpc&utm_referrer=wizards".format(
                                adg_clid
                            ),
                            ignore_len=False,
                        ),
                        "snippetAdGUrlTouch": LikeUrl.of(
                            "//m.market.yandex.ru/search?text=kiyanka&clid={0}&lr=213&utm_medium=cpc&utm_referrer=wizards".format(
                                adg_clid_touch
                            ),
                            ignore_len=False,
                        ),
                        "greenUrl": [
                            {
                                "url": LikeUrl.of(
                                    "//market.yandex.ru?clid={0}&lr=213&utm_medium=cpc&utm_referrer=wizards".format(
                                        clid
                                    ),
                                    ignore_len=False,
                                ),
                                "urlTouch": LikeUrl.of(
                                    "//m.market.yandex.ru?clid={0}&lr=213&utm_medium=cpc&utm_referrer=wizards".format(
                                        clid_touch
                                    ),
                                    ignore_len=False,
                                ),
                                "adGUrl": LikeUrl.of(
                                    "//market.yandex.ru?clid={0}&lr=213&utm_medium=cpc&utm_referrer=wizards".format(
                                        adg_clid
                                    ),
                                    ignore_len=False,
                                ),
                                "adGUrlTouch": LikeUrl.of(
                                    "//m.market.yandex.ru?clid={0}&lr=213&utm_medium=cpc&utm_referrer=wizards".format(
                                        adg_clid_touch
                                    ),
                                    ignore_len=False,
                                ),
                                "snippetUrl": LikeUrl.of(
                                    "//market.yandex.ru?clid={0}&lr=213&utm_medium=cpc&utm_referrer=wizards".format(
                                        clid
                                    ),
                                    ignore_len=False,
                                ),
                                "snippetUrlTouch": LikeUrl.of(
                                    "//m.market.yandex.ru?clid={0}&lr=213&utm_medium=cpc&utm_referrer=wizards".format(
                                        clid_touch
                                    ),
                                    ignore_len=False,
                                ),
                                "snippetAdGUrl": LikeUrl.of(
                                    "//market.yandex.ru?clid={0}&lr=213&utm_medium=cpc&utm_referrer=wizards".format(
                                        adg_clid
                                    ),
                                    ignore_len=False,
                                ),
                                "snippetAdGUrlTouch": LikeUrl.of(
                                    "//m.market.yandex.ru?clid={0}&lr=213&utm_medium=cpc&utm_referrer=wizards".format(
                                        adg_clid_touch
                                    ),
                                    ignore_len=False,
                                ),
                            },
                            {
                                "url": LikeUrl.of(
                                    "//market.yandex.ru/search?text=kiyanka&clid={0}&lr=213&utm_medium=cpc&utm_referrer=wizards".format(
                                        clid
                                    ),
                                    ignore_len=False,
                                ),
                                "urlTouch": LikeUrl.of(
                                    "//m.market.yandex.ru/search?text=kiyanka&clid={0}&lr=213&utm_medium=cpc&utm_referrer=wizards".format(
                                        clid_touch
                                    ),
                                    ignore_len=False,
                                ),
                                "adGUrl": LikeUrl.of(
                                    "//market.yandex.ru/search?text=kiyanka&clid={0}&lr=213&utm_medium=cpc&utm_referrer=wizards".format(
                                        adg_clid
                                    ),
                                    ignore_len=False,
                                ),
                                "adGUrlTouch": LikeUrl.of(
                                    "//m.market.yandex.ru/search?text=kiyanka&clid={0}&lr=213&utm_medium=cpc&utm_referrer=wizards".format(
                                        adg_clid_touch
                                    ),
                                    ignore_len=False,
                                ),
                                "snippetUrl": LikeUrl.of(
                                    "//market.yandex.ru/search?text=kiyanka&clid={0}&lr=213&utm_medium=cpc&utm_referrer=wizards".format(
                                        clid
                                    ),
                                    ignore_len=False,
                                ),
                                "snippetUrlTouch": LikeUrl.of(
                                    "//m.market.yandex.ru/search?text=kiyanka&clid={0}&lr=213&utm_medium=cpc&utm_referrer=wizards".format(
                                        clid_touch
                                    ),
                                    ignore_len=False,
                                ),
                                "snippetAdGUrl": LikeUrl.of(
                                    "//market.yandex.ru/search?text=kiyanka&clid={0}&lr=213&utm_medium=cpc&utm_referrer=wizards".format(
                                        adg_clid
                                    ),
                                    ignore_len=False,
                                ),
                                "snippetAdGUrlTouch": LikeUrl.of(
                                    "//m.market.yandex.ru/search?text=kiyanka&clid={0}&lr=213&utm_medium=cpc&utm_referrer=wizards".format(
                                        adg_clid_touch
                                    ),
                                    ignore_len=False,
                                ),
                            },
                        ],
                        "button": [
                            {
                                "url": LikeUrl.of(
                                    "//market.yandex.ru/search?text=kiyanka&clid={0}&lr=213&utm_medium=cpc&utm_referrer=wizards".format(
                                        clid
                                    ),
                                    ignore_len=False,
                                    ignore_params=['rs'],
                                ),
                                "urlTouch": LikeUrl.of(
                                    "//m.market.yandex.ru/search?text=kiyanka&clid={0}&lr=213&utm_medium=cpc&utm_referrer=wizards".format(
                                        clid_touch
                                    ),
                                    ignore_len=False,
                                    ignore_params=['rs'],
                                ),
                                "adGUrl": LikeUrl.of(
                                    "//market.yandex.ru/search?text=kiyanka&clid={0}&lr=213&utm_medium=cpc&utm_referrer=wizards".format(
                                        adg_clid
                                    ),
                                    ignore_len=False,
                                    ignore_params=['rs'],
                                ),
                                "adGUrlTouch": LikeUrl.of(
                                    "//m.market.yandex.ru/search?text=kiyanka&clid={0}&lr=213&utm_medium=cpc&utm_referrer=wizards".format(
                                        adg_clid_touch
                                    ),
                                    ignore_len=False,
                                    ignore_params=['rs'],
                                ),
                            }
                        ],
                        "showcase": {
                            "items": [
                                {
                                    "thumb": {
                                        "url": LikeUrl.of(
                                            "//market.yandex.ru/offer/m5sFBy3SDRzD5UYHuEyImw?clid={0}&lr=213&utm_medium=cpc&utm_referrer=wizards".format(
                                                clid
                                            )
                                        ),
                                        "urlTouch": LikeUrl.of(
                                            "//market.yandex.ru/offer/m5sFBy3SDRzD5UYHuEyImw?clid={0}&lr=213&utm_medium=cpc&utm_referrer=wizards".format(
                                                clid_touch
                                            )
                                        ),
                                        "adGUrl": LikeUrl.of(
                                            "//market.yandex.ru/offer/m5sFBy3SDRzD5UYHuEyImw?clid={0}&lr=213&utm_medium=cpc&utm_referrer=wizards".format(
                                                adg_clid
                                            )
                                        ),
                                        "adGUrlTouch": LikeUrl.of(
                                            "//market.yandex.ru/offer/m5sFBy3SDRzD5UYHuEyImw?clid={0}&lr=213&utm_medium=cpc&utm_referrer=wizards".format(
                                                adg_clid_touch
                                            )
                                        ),
                                        "urlForCounter": Contains("//market-click2.yandex.ru/redir/dtype=offercard/"),
                                        "offercardUrl": Contains("//market-click2.yandex.ru/redir/dtype=offercard/"),
                                    },
                                    "title": {
                                        "url": LikeUrl.of(
                                            "//market.yandex.ru/offer/m5sFBy3SDRzD5UYHuEyImw?clid={0}&lr=213&utm_medium=cpc&utm_referrer=wizards".format(
                                                clid
                                            )
                                        ),
                                        "urlTouch": LikeUrl.of(
                                            "//market.yandex.ru/offer/m5sFBy3SDRzD5UYHuEyImw?clid={0}&lr=213&utm_medium=cpc&utm_referrer=wizards".format(
                                                clid_touch
                                            )
                                        ),
                                        "adGUrl": LikeUrl.of(
                                            "//market.yandex.ru/offer/m5sFBy3SDRzD5UYHuEyImw?clid={0}&lr=213&utm_medium=cpc&utm_referrer=wizards".format(
                                                adg_clid
                                            )
                                        ),
                                        "adGUrlTouch": LikeUrl.of(
                                            "//market.yandex.ru/offer/m5sFBy3SDRzD5UYHuEyImw?clid={0}&lr=213&utm_medium=cpc&utm_referrer=wizards".format(
                                                adg_clid_touch
                                            )
                                        ),
                                        "urlForCounter": Contains("//market-click2.yandex.ru/redir/dtype=offercard/"),
                                        "offercardUrl": Contains("//market-click2.yandex.ru/redir/dtype=offercard/"),
                                    },
                                    "greenUrl": {
                                        "url": LikeUrl.of(
                                            "//market.yandex.ru/shop--shop-1/1/reviews?cmid=m5sFBy3SDRzD5UYHuEyImw&clid={0}&lr=213&utm_medium=cpc&utm_referrer=wizards".format(
                                                clid
                                            )
                                        ),
                                        "urlTouch": LikeUrl.of(
                                            "//m.market.yandex.ru/grades-shop.xml?shop_id=1&cmid=m5sFBy3SDRzD5UYHuEyImw&clid={0}&lr=213&utm_medium=cpc&utm_referrer=wizards".format(
                                                clid_touch
                                            )
                                        ),
                                        "adGUrl": LikeUrl.of(
                                            "//market.yandex.ru/shop--shop-1/1/reviews?cmid=m5sFBy3SDRzD5UYHuEyImw&clid={0}&lr=213&utm_medium=cpc&utm_referrer=wizards".format(
                                                adg_clid
                                            )
                                        ),
                                        "adGUrlTouch": LikeUrl.of(
                                            "//m.market.yandex.ru/grades-shop.xml?shop_id=1&cmid=m5sFBy3SDRzD5UYHuEyImw&clid={0}&lr=213&utm_medium=cpc&utm_referrer=wizards".format(
                                                adg_clid_touch
                                            )
                                        ),
                                    },
                                }
                            ],
                        },
                    }
                },
            )

        do_test('', 'market_offers_wizard', [545, 708, 913, 919])
        do_test(
            ';market_enable_offers_adg_wiz=1;market_adg_offers_incut_threshold=0',
            'market_offers_adg_wizard',
            [913, 919, 913, 919],
        )

    def test_adg_nailed_in_serch_url_type_in_offers_wizard(self):
        """Проверяем, что ссылки в РГ офферного колдунщика ведут на серч с прибитым оффером
        https://st.yandex-team.ru/MARKETOUT-33500
        """
        rs = 'eJwzUvCS4xLLNS12c6o0DnYJqnIxDY30KHWt9Mwtl2BUYNBgAACc1Ai3'
        for device in ['desktop', 'touch']:
            response = self.report.request_bs_pb(
                'place=parallel&text=kiyanka&rids=213'
                '&rearr-factors=market_enable_offers_adg_wiz=1;offers_touch=1;device=' + device
            )
            self.assertFragmentIn(
                response,
                {
                    "market_offers_adg_wizard": {
                        "showcase": {
                            "items": [
                                {
                                    "thumb": {
                                        "adGUrl": LikeUrl.of(
                                            "//market.yandex.ru/search?text=kiyanka&lr=213&clid=913&cvredirect=0&rs={}&utm_medium=cpc&utm_referrer=wizards&utm_medium=cpc&utm_referrer=wizards".format(
                                                rs
                                            ),
                                            ignore_len=False,
                                        ),
                                        "adGUrlTouch": LikeUrl.of(
                                            "//market.yandex.ru/search?text=kiyanka&lr=213&clid=919&cvredirect=0&rs={}&utm_medium=cpc&utm_referrer=wizards&utm_medium=cpc&utm_referrer=wizards".format(
                                                rs
                                            ),
                                            ignore_len=False,
                                        ),
                                    },
                                    "title": {
                                        "text": {"__hl": {"text": "kiyanka 1", "raw": True}},
                                        "adGUrl": LikeUrl.of(
                                            "//market.yandex.ru/search?text=kiyanka&lr=213&clid=913&cvredirect=0&rs={}&utm_medium=cpc&utm_referrer=wizards&utm_medium=cpc&utm_referrer=wizards".format(
                                                rs
                                            ),
                                            ignore_len=False,
                                        ),
                                        "adGUrlTouch": LikeUrl.of(
                                            "//market.yandex.ru/search?text=kiyanka&lr=213&clid=919&cvredirect=0&rs={}&utm_medium=cpc&utm_referrer=wizards&utm_medium=cpc&utm_referrer=wizards".format(
                                                rs
                                            ),
                                            ignore_len=False,
                                        ),
                                    },
                                }
                            ]
                        }
                    }
                },
            )


if __name__ == '__main__':
    main()
