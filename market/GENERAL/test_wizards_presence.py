#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    Book,
    CardCategory,
    HyperCategory,
    HyperCategoryType,
    Model,
    NavCategory,
    Offer,
    Region,
    Shop,
    Vendor,
)
from core.testcase import TestCase, main
from core.matcher import LikeUrl
import json


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ["market_money_disable_bids=0"]
        cls.index.regiontree += [Region(rid=213, name='Москва', genitive='Москвы')]

        cls.index.shops += [
            Shop(fesh=1, priority_region=213),
        ]

    @classmethod
    def prepare_market_model_book(cls):
        cls.index.books += [Book(hyperid=104, hid=304, isbn='978-5-699-08426-5')]

        cls.index.navtree += [
            NavCategory(nid=404, hid=304),
        ]

        cls.index.offers += [
            Offer(hyperid=104, price=888, title="Fairytales", fesh=1, is_book=1),
        ]

    def test_market_model_wiz_present_for_book(self):
        response = self.report.request_bs('place=parallel&text=978-5-699-08426-5&rids=213&ignore-mn=1')
        self.assertFragmentIn(
            response,
            {
                "market_model": [
                    {
                        "url": LikeUrl.of(
                            "//market.yandex.ru/product--book-writer-book-304/104?clid=502&hid=304&nid=404"
                        ),
                    }
                ]
            },
        )

    @classmethod
    def prepare_ext_category_wizard(cls):
        cls.index.vendors += [
            Vendor(vendor_id=10, name='bakery'),
            Vendor(vendor_id=11, name='dairy'),
            Vendor(vendor_id=12, name='butcher'),
        ]

        cls.index.hypertree += [HyperCategory(hid=100, name='food', output_type=HyperCategoryType.GURU)]

        cls.index.navtree += [NavCategory(hid=100, nid=100)]

        cls.index.cards += [CardCategory(hid=100, vendor_ids=[10, 11, 12])]

    def test_ext_category_wizard_present(self):
        """
        Проверка наличия основных полей в колдунщике market_ext_category и в island_cards
        """
        response = self.report.request_bs("place=parallel&text=food")
        self.assertFragmentIn(
            response,
            {
                "market_ext_category": [
                    {
                        "url": LikeUrl.of(
                            "//market.yandex.ru/catalog--food/100?hid=100&clid=500&lr=0&utm_medium=cpc&utm_referrer=wizards",
                            ignore_len=False,
                        ),
                        "title": {"__hl": {"text": "Food на Маркете", "raw": True}},
                        "greenUrl": [
                            {
                                "url": LikeUrl.of(
                                    "//market.yandex.ru?clid=500&lr=0&utm_medium=cpc&utm_referrer=wizards",
                                    ignore_len=False,
                                ),
                                "urlTouch": LikeUrl.of(
                                    "//m.market.yandex.ru?clid=707&lr=0&utm_medium=cpc&utm_referrer=wizards",
                                    ignore_len=False,
                                ),
                                "text": "Яндекс.Маркет",
                            },
                            {
                                "url": LikeUrl.of(
                                    "//market.yandex.ru/catalog--food/100?hid=100&clid=500&lr=0&utm_medium=cpc&utm_referrer=wizards",
                                    ignore_len=False,
                                ),
                                "urlTouch": LikeUrl.of(
                                    "//m.market.yandex.ru/catalog?hid=100&nid=100&clid=707&lr=0&utm_medium=cpc&utm_referrer=wizards",
                                    ignore_len=False,
                                ),
                                "text": "Food",
                            },
                        ],
                        "showcase": {
                            "items": [
                                {
                                    "thumb": {
                                        "source": "//mdata.yandex.net/i?path=vendor_10_in_hid_100.jpg&size=2",
                                        "retinaSource": "//mdata.yandex.net/i?path=vendor_10_in_hid_100.jpg&size=5",
                                    },
                                    "title": {
                                        "url": "//market.yandex.ru/catalog--food-bakery/100/list?glfilter=7893318%3A10&hid=100&lr=0&utm_medium=cpc&utm_referrer=wizards&clid=500",
                                        "urlTouch": "//m.market.yandex.ru/catalog--food-bakery/100/list?glfilter=7893318%3A10&hid=100&lr=0&utm_medium=cpc&utm_referrer=wizards&clid=707",
                                        "text": {"__hl": {"text": "bakery", "raw": True}},
                                    },
                                    "label": {"text": "100 моделей"},
                                },
                                {
                                    "thumb": {
                                        "source": "//mdata.yandex.net/i?path=vendor_11_in_hid_100.jpg&size=2",
                                        "retinaSource": "//mdata.yandex.net/i?path=vendor_11_in_hid_100.jpg&size=5",
                                    },
                                    "title": {
                                        "url": "//market.yandex.ru/catalog--food-dairy/100/list?glfilter=7893318%3A11&hid=100&lr=0&utm_medium=cpc&utm_referrer=wizards&clid=500",
                                        "urlTouch": "//m.market.yandex.ru/catalog--food-dairy/100/list?glfilter=7893318%3A11&hid=100&lr=0&utm_medium=cpc&utm_referrer=wizards&clid=707",
                                        "text": {"__hl": {"text": "dairy", "raw": True}},
                                    },
                                    "label": {"text": "100 моделей"},
                                },
                                {
                                    "thumb": {
                                        "source": "//mdata.yandex.net/i?path=vendor_12_in_hid_100.jpg&size=2",
                                        "retinaSource": "//mdata.yandex.net/i?path=vendor_12_in_hid_100.jpg&size=5",
                                    },
                                    "title": {
                                        "url": "//market.yandex.ru/catalog--food-butcher/100/list?glfilter=7893318%3A12&hid=100&lr=0&utm_medium=cpc&utm_referrer=wizards&clid=500",
                                        "urlTouch": "//m.market.yandex.ru/catalog--food-butcher/100/list?glfilter=7893318%3A12&hid=100&lr=0&utm_medium=cpc&utm_referrer=wizards&clid=707",
                                        "text": {"__hl": {"text": "butcher", "raw": True}},
                                    },
                                    "label": {"text": "100 моделей"},
                                },
                            ]
                        },
                    }
                ]
            },
        )

    @classmethod
    def prepare_cut_region_from_request(cls):
        """Подготовка данных для проверки вырезания региона из запроса в колдунщиках
        https://st.yandex-team.ru/MARKETOUT-11869
        """

        # создаем регион
        cls.index.regiontree += [
            Region(rid=2, name="Санкт-Петербург"),
        ]
        # создаем магазин
        cls.index.shops += [
            Shop(fesh=101, priority_region=2),
        ]
        # создаем модели
        cls.index.models += [
            Model(hyperid=1301, title="iphone 5s 16gb"),
        ]
        # создаем оффер
        cls.index.offers += [
            Offer(hyperid=1301, fesh=101, title="iphone 5s 16gb"),
        ]

    def test_cut_region_from_request(self):
        """Проверка вырезания региона из запроса в колдунщиках
        https://st.yandex-team.ru/MARKETOUT-11869
        """
        # Добавляем дерево запроса, т.к. так это работает в проде
        qtree_markup = {
            "Market": {
                "qtree4market": "cHicvVbPa1RXFD7nvJfn7U2QR2JgfNQyGQVfBctM60xjQVKiiyCiIYuibyXBH3HRWDIUJIsSbYW0"
                "3VRji5QURIlOBKMmiFVUXIkIwgtuShdd9M9wIT33vvtuZt68_NjYbO679557zne_755vIg_KLtHh"
                "ewUoYkhl6IYASrALPoUvugT4wOsQQhm-7BjqGIajcBzG8BeE3xGuIywiPEXgv5cIMZ4IzjvyK5kc"
                "E3xMpfPOfDN2dvxkgEWb1WvKCkOgso5d-CxNag5kUpehHwdnSaAPgYkoQYhlHJ6hBNTEbmk2Csgb"
                "0A8jNL0Q4dwo1wsg7BkVPNLOeuiM4Tma2N4W7kzPLUSQhuuggbYgnI5obqE95-gmnwJMPiDAnaZK"
                "X3uCqUyN_e0hP0Z0bSnC-7aMznlCJ8ekygdczjkzfjqpM0mCphCYw-A7eSgjAVZZUiwa9lGzjy3s"
                "v7WSYrWdeGewJEjVrZY6y_avqtk3L2ISjqOpfwnbAdSb5ccc-V-4FkC9HQANVvmUunhW860S6wUw"
                "tK3QKhJGLKY_UL9L8sliciu106MKliXGzcE1-9s_dyiFpo_kvcu9-lnqbQPwqmsAfij1ci5GmIQE"
                "X0mRQgUqOmFHWc3IzLCc2VPlor9JiN5_j319IHhJ8lSG6674Xvxo-Yf4Wfw0fhDgxyUM12i7lvu1"
                "HM275xvUF22JMxe-kSpyEaVc2S_ocrq3nr-K6HbaOE7SOPE9-_XEfv2pvyig5YvpV_yAe0DyC3A5"
                "6aJeRXtGRX6fri3_zA0B53Bih2yBuYoAbChGAlexGELUQDE1JXrVzYMrKEcyz4aq9ZZHQ-uRygfy"
                "qNyjmeRNw1_B0BdIXlvntbhc1WWolx2G6idQ35Eckqlh00aMfC0bR3k486yoUms2Ecoxkb-kvXOl"
                "luci27WL8GaTjVRqq9jIrzkYkoaFNahvCIshr1Vp8HNtJGQb1ToJE79Km7ZYSWtzKimtKJ7vsSiv"
                "HXFeizJ5IHjkaFHUATB3eB-ihP-bKHHSEM0Y3HgmvqR8Zi1ZHm-2HqrC84QZ18K48VJ8P7UUNNIc"
                "kXo56yV3lZ9E1OCxweMtHm_xeJPHmzzO8zjP49zd5h9s5Q2q6zeopxs9RKMnN9kNlEczfuBVapqA"
                "FVXX_RkxR_JcYV_y_w0HrNAwndLwkTQb69qDRs6zn7BPal_r3iow6BVe77dbPhnY1rgd9xdht6pd"
                "5pgeHUPdnUKMbBLY7RwePpmsur5nVqlp1bOraWyXyeCKzROQTF3fbZ6qM3rKAP8D1AXjfg,,",
            }
        }
        response = self.report.request_bs(
            "place=parallel&text=iphone+5s+16gb+санкт-петербург&rids=2&askreqwizard=1"
            "&rearr-factors=market_parallel_wizard=1"
            "&wizard-rules={0}".format(json.dumps(qtree_markup))
        )
        self.assertFragmentIn(
            response,
            {
                "market_model": [
                    {
                        "showcase": {
                            "items": [
                                {
                                    "title": {"text": {"__hl": {"text": "iphone 5s 16gb", "raw": True}}},
                                }
                            ]
                        }
                    }
                ]
            },
        )

    @classmethod
    def prepare_region_in_title_offer_wizard(cls):
        """Подготовка для проверки региона в заголовке офферного колдунщика под конструктором
        https://st.yandex-team.ru/MARKETOUT-14237
        """
        # создаем регион
        cls.index.regiontree += [
            Region(rid=55, name="Тюмень", preposition="в", locative="Тюмени"),
        ]
        # создаем магазин в регионе
        cls.index.shops += [
            Shop(fesh=102, priority_region=55),
        ]
        # создаем оффер
        cls.index.offers += [
            Offer(title="region test offer", fesh=102),
        ]

    def test_region_in_title_offer_wizard(self):
        """Проверка региона в заголовке офферного колдунщика
        https://st.yandex-team.ru/MARKETOUT-14237
        """
        # Добавляем дерево запроса, т.к. так это работает в проде
        qtree_markup = {
            "Market": {
                "qtree4market": "cHicxZWxaxRBFMbfm91LxvEM6wXhmMbjmqxWh1UQNBJTBAs5gkhYLMKRzV2aBC4pQqozRj2NhSBY"
                "RAVJFC1i1ByBJJgQC4MGwd3G0k7wD7CycnZmd7mdLBYR9Jp78-Z938378R7HLrEsPWZ15aGANilBDjgU4TScgbNZChaI"
                "PNhQgguZwUwZhmEEangfYRFhCWENYRtBfD4ieDjK95ANMyWjQhbYddTd6vjkBMdCEQuhbUebLQxCYFv7ujIR2YYSzbwE"
                "vdg_QtECHhYUwcYSlh8Q9ap6Hwsv8tK6F4aw6ZDnqxXxexzs7goV36RnyjYqnRbhqALg2GMbNZghs4SSBoJ4Bn9G2BWt"
                "EXPanZpOtGGmtPFz83jUhhSkNfEeZRfyXu_hHJPpPB6qg3q_Lr_jkKctB98mHdxRITwiHIzxiar0MKSZ9DipezQciOQa"
                "piZhVzVMmcmxMbcuZijGlEnB9GvvQ2fESSnSQC0rUKpAJ3WRqfxf9Xr-gMkhJ6YY9MnypGDamZJ2CnpxNgmlJ_aX3AH-"
                "gjBXg5b1G9661_I2vB1vm-OpItp_WJUfi_dIBC-hTGPYUgwTdSHK5QjlArLEfbw_RnN33yEvVw8y8d4IFCp6F0cbMiKc"
                "-DejyFsXEbOQm8J4TWYx1gSVN6KcvyCZomCKIVMzgGSD8zlg1_d9dIBvEXZNY0f9OW_dv-7PJbil7ebWYjbCFovSkH1R"
                "yOIaHddtZPFdPDj_A1W9yFj0EG8nHzwzeEqwrSi3lc5AAiexiMD5OsDJjOoAf0JYRcPJvB1_3p8Xpo0E0LQtfvzw26t4"
                "EtuEaVBXFNS2Kh3rLWRtt3E3ZvPR7qd_RhW1ATQt0wZxuovdLBhHM3eU0qFOSnLG5bKrsiTOYpjNylrxn0G76qCOAn37"
                "Ufiqo_iZ368In7k,",
                "NonRegionQuery": "region test offer",
                "FoundCities": "55",
            }
        }
        # Для проверки параметра lr в url
        lr_param = LikeUrl(url_params={"lr": "55"})

        # Проверка под коструктором
        # Если регион из запроса и регион пользователя совпадают, то регион вырезается из тайтла
        response = self.report.request_bs(
            'place=parallel&rids=55&text=Тюмень+region+test+offer'
            '&rearr-factors=market_parallel_wizard=1'
            '&wizard-rules={0}'.format(json.dumps(qtree_markup))
        )
        self.assertFragmentIn(response, {"market_offers_wizard": [{"title": "\7[Region test offer\7] в Тюмени"}]})
        # Если регион из запроса и регион пользователя не совпадают, то в тайтл добавляется регион
        response = self.report.request_bs(
            'place=parallel&rids=2&text=Тюмень+region+test+offer'
            '&rearr-factors=market_parallel_wizard=1'
            '&wizard-rules={0}'.format(json.dumps(qtree_markup))
        )
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": [
                    {
                        "title": "\7[Region test offer\7] в Тюмени",
                        "url": lr_param,
                        "urlTouch": lr_param,
                        "button": [
                            {
                                "url": lr_param,
                                "urlTouch": lr_param,
                            }
                        ],
                        "greenUrl": [
                            {
                                "url": lr_param,
                                "urlTouch": lr_param,
                            },
                            {
                                "url": lr_param,
                                "urlTouch": lr_param,
                            },
                        ],
                    }
                ]
            },
        )


if __name__ == '__main__':
    main()  # каждую группу тестов можно запускать независимо по файлу
