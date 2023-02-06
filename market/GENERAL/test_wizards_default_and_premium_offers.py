#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import BlueOffer, DeliveryOption, MarketSku, MnPlace, Model, Offer, Picture, Shop
from core.testcase import TestCase, main
from core.matcher import NoKey, Contains, LikeUrl, ElementCount, Not, EmptyList, NotEmpty


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ["market_money_disable_bids=0"]
        cls.settings.nordstream_autogenerate = False
        cls.settings.default_search_experiment_flags += ['market_nordstream=0']

    @classmethod
    def prepare_premium_offers_in_model_wizard(cls):
        cls.index.models += [Model(hyperid=301090, title="lenovo p780")]
        cls.index.shops += [
            Shop(fesh=1, priority_region=213, regions=[225], cpa=Shop.CPA_REAL),
            Shop(fesh=2, priority_region=213, regions=[225], cpa=Shop.CPA_REAL),
            Shop(fesh=3, priority_region=213, regions=[225], cpa=Shop.CPA_REAL),
            Shop(fesh=4, priority_region=213, regions=[225], cpa=Shop.CPA_REAL),
            Shop(fesh=5, priority_region=213, regions=[225], cpa=Shop.CPA_REAL),
        ]
        cls.index.offers += [
            Offer(
                title='lenovo p780 cpa offer 1',
                hyperid=301090,
                fesh=1,
                bid=90,
                price=8000,
                waremd5='H__rIZIhXqNM4Kq2NlBTSg',
            ),
            Offer(
                title='lenovo p780 cpa offer 2',
                hyperid=301090,
                fesh=2,
                bid=80,
                price=8000,
                waremd5='RxxQrRoVbXJW7d_XR9d5MQ',
            ),
            Offer(
                title='lenovo p780 cpa offer 3',
                hyperid=301090,
                fesh=3,
                bid=100,
                price=8000,
                waremd5='1ZSxqUW11kXlniH4i9LYOw',
            ),
            Offer(
                title='lenovo p780 cpa offer 4',
                hyperid=301090,
                fesh=4,
                bid=70,
                price=8000,
                waremd5='SlM1kXY9-nQ6E6_6sahXkw',
            ),
            Offer(
                title='lenovo p780 cpa offer 5',
                hyperid=301090,
                fesh=5,
                bid=60,
                price=8000,
                waremd5='ZD3nz3unacdGbMvErf1_rA',
            ),
        ]

    def test_premium_offers_in_model_wizard(self):
        """Помечаем премиальный оффер во врезке модельного колдунщика
        https://st.yandex-team.ru/MARKETOUT-26648
        https://st.yandex-team.ru/MARKETOUT-27062
        """
        response = self.report.request_json(
            'place=productoffers&hyperid=301090&offers-set=all&show-urls=external&rearr-factors=market_premium_offer_logic=mark&rids=213'
        )
        self.assertFragmentIn(
            response,
            {"results": [{"shop": {"id": 3}, "wareId": "1ZSxqUW11kXlniH4i9LYOw", "isPremium": True}]},
            preserve_order=False,
        )

        # Премиальный оффер поднимается на первое место
        # Под конструктором
        response = self.report.request_bs_pb(
            'place=parallel&text=lenovo p780&rids=213'
            '&rearr-factors=market_premium_offer_in_model_wizard=1;showcase_universal_model=1'
        )
        self.assertFragmentIn(
            response,
            {
                "market_model": {
                    "showcase": {
                        "items": [
                            {
                                "title": {"text": {"__hl": {"text": "lenovo p780 cpa offer 3", "raw": True}}},
                                "isPremium": "1",
                            },
                            {
                                "title": {"text": {"__hl": {"text": "lenovo p780 cpa offer 1", "raw": True}}},
                                "isPremium": NoKey("isPremium"),
                            },
                            {
                                "title": {"text": {"__hl": {"text": "lenovo p780 cpa offer 2", "raw": True}}},
                                "isPremium": NoKey("isPremium"),
                            },
                            {
                                "title": {"text": {"__hl": {"text": "lenovo p780 cpa offer 4", "raw": True}}},
                                "isPremium": NoKey("isPremium"),
                            },
                            {
                                "title": {"text": {"__hl": {"text": "lenovo p780 cpa offer 5", "raw": True}}},
                                "isPremium": NoKey("isPremium"),
                            },
                        ]
                    }
                }
            },
            preserve_order=True,
        )

    def test_premium_offers_urls_in_model_wizard(self):
        """Проверяем ссылки премиального оффера во врезке модельного колдунщика
        под флагом market_model_wizard_premium_offer_url_type
        https://st.yandex-team.ru/MARKETOUT-27062
        """
        # По умолчанию и под флагом market_model_wizard_premium_offer_url_type=External ссылка ведет в магазин,
        # под флагом market_model_wizard_premium_offer_url_type=OfferCard ссылка ведет на карточку оффера
        for flag, url_type in [
            ('', 'market'),
            ('market_model_wizard_premium_offer_url_type=External', 'market'),
            ('market_model_wizard_premium_offer_url_type=OfferCard', 'offercard'),
        ]:
            response = self.report.request_bs_pb(
                'place=parallel&text=lenovo p780&rids=213&rearr-factors=market_premium_offer_in_model_wizard=1;'
                'showcase_universal_model=1;' + flag
            )
            self.assertFragmentIn(
                response,
                {
                    "market_model": {
                        "showcase": {
                            "items": [
                                {
                                    "title": {
                                        "text": {"__hl": {"text": "lenovo p780 cpa offer 3", "raw": True}},
                                        "urlForCounter": Contains(
                                            "//market-click2.yandex.ru/redir/dtype={}/".format(url_type)
                                        ),
                                        "offercardUrl": Contains(
                                            "//market-click2.yandex.ru/redir/dtype={}/".format(url_type)
                                        ),
                                    },
                                    "thumb": {
                                        "urlForCounter": Contains(
                                            "//market-click2.yandex.ru/redir/dtype={}/".format(url_type)
                                        ),
                                        "offercardUrl": Contains(
                                            "//market-click2.yandex.ru/redir/dtype={}/".format(url_type)
                                        ),
                                    },
                                    "isPremium": "1",
                                }
                            ]
                        }
                    }
                },
            )

    def test_bill_min_bid_in_premium_offers_in_model_wizard(self):
        """По умолчанию и под флагом market_parallel_bill_minbid_in_premium_offers=1 списывается минставка,
        а примеальная ставка пишется в fuid=premium_cp
        Под флагом market_parallel_bill_minbid_in_premium_offers=0 списывается премиальная ставка
        https://st.yandex-team.ru/MARKETOUT-27062
        Для карточки оффера примеальная ставка пишется в параметр &rs, а fuid=premium_cp не добавляется.
        https://st.yandex-team.ru/MARKETOUT-28790
        """

        def test(request, url):

            # Под конструктором
            response = self.report.request_bs_pb(request + 'showcase_universal_model=1;')
            self.assertFragmentIn(
                response,
                {
                    "market_model": {
                        "showcase": {
                            "items": [
                                {
                                    "title": {
                                        "text": {"__hl": {"text": "lenovo p780 cpa offer 3", "raw": True}},
                                        "urlForCounter": url,
                                        "offercardUrl": url,
                                    },
                                    "thumb": {
                                        "urlForCounter": url,
                                        "offercardUrl": url,
                                    },
                                    "isPremium": "1",
                                }
                            ]
                        }
                    }
                },
            )

        has_fuid = Contains('/fuid=premium_cp=91/')
        no_fuid = Not(Contains('/fuid=premium_cp='))

        for device in ('desktop', 'touch', 'tablet'):
            for url_type in ('', 'External', 'OfferCard'):
                for flag, cp, fuid in [
                    ('', '/cp=11/', has_fuid),
                    ('market_parallel_bill_minbid_in_premium_offers=1', '/cp=11/', has_fuid),
                    ('market_parallel_bill_minbid_in_premium_offers=0', '/cp=91/', no_fuid),
                ]:

                    request = (
                        'place=parallel&text=lenovo p780&rids=213'
                        '&rearr-factors=market_premium_offer_in_model_wizard=1;'
                        'device={};{};'.format(device, flag)
                    )
                    if url_type:
                        request += 'market_model_wizard_premium_offer_url_type={};'.format(url_type)

                    if url_type == 'OfferCard':
                        fuid = no_fuid

                    test(request, Contains(cp))
                    test(request, fuid)

    @classmethod
    def prepare_beru_premium_offers_in_model_wizard(cls):
        cls.index.models += [
            Model(hyperid=185467, title="hakuna matata", ts=185467, has_blue_offers=True),
            Model(hyperid=185468, title="hakuna matata 2", ts=185468, has_blue_offers=True),
        ]
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 185467).respond(0.6)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 185468).respond(0.5)

        cls.index.mskus += [
            MarketSku(
                sku=18546700,
                title="hakuna matata",
                hyperid=185467,
                use_pokupki_domain=True,
                blue_offers=[BlueOffer(ts=1854670)],
            ),
            MarketSku(
                sku=18546800,
                title="hakuna matata 2",
                hyperid=185468,
                use_pokupki_domain=True,
                blue_offers=[BlueOffer(ts=1854680)],
            ),
        ]
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 1854670).respond(0.6)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 1854680).respond(0.5)

    def test_beru_premium_offers_in_model_wizard(self):
        """Если премиальный оффер колдунщика это оффер Беру или КО, пробрасываем в него клид
        модельного колдунщика.
        https://st.yandex-team.ru/MARKETOUT-27622

        Для РГ и других колдунщиков пока что тоже будет прорастать этот же клид.
        Чинить будем в https://st.yandex-team.ru/MARKETOUT-27451
        """

        # 1. Проверяем проброс клида в оффер Беру
        # 1.1 Десктоп
        response = self.report.request_bs(
            'place=parallel&text=hakuna+matata&reqid=1&rearr-factors=market_premium_offer_in_model_wizard=1'
        )
        self.assertFragmentIn(
            response,
            {
                "market_model": [
                    {
                        "showcase": {
                            "items": [
                                {
                                    "isPremium": "1",
                                    "title": {"url": Contains("clid=502"), "offercardUrl": Contains("clid%3D502")},
                                }
                            ]
                        }
                    }
                ]
            },
        )
        self.click_log.expect(reqid=1, dtype="market", position=0, data_url=Contains("clid%3D502"))

        # 1.2 Тач
        response = self.report.request_bs(
            'place=parallel&text=hakuna+matata&reqid=2&touch=1&rearr-factors=market_premium_offer_in_model_wizard=1'
        )
        self.assertFragmentIn(
            response,
            {
                "market_model": [
                    {
                        "showcase": {
                            "items": [
                                {
                                    "isPremium": "1",
                                    "title": {"url": Contains("clid=502"), "offercardUrl": Contains("clid%3D704")},
                                }
                            ]
                        }
                    }
                ]
            },
        )
        self.click_log.expect(reqid=2, dtype="market", position=0, data_url=Contains("clid%3D704"))

        # 2. Проверяем проброс клида в КО
        # 2.1 Десктоп
        response = self.report.request_bs(
            'place=parallel&text=lenovo p780&rids=213&reqid=3'
            '&rearr-factors=market_premium_offer_in_model_wizard=1;'
            'market_model_wizard_premium_offer_url_type=OfferCard'
        )
        self.assertFragmentIn(
            response,
            {
                "market_model": [
                    {
                        "showcase": {
                            "items": [
                                {
                                    "isPremium": "1",
                                    "title": {"offercardUrl": Contains("clid%3D502")},
                                }
                            ]
                        }
                    }
                ]
            },
        )
        self.click_log.expect(reqid=3, dtype="offercard", position=0, data_url=Contains("clid%3D502"))
        # 2.2 Тач
        response = self.report.request_bs(
            'place=parallel&text=lenovo p780&rids=213&reqid=4&touch=1'
            '&rearr-factors=market_premium_offer_in_model_wizard=1;'
            'market_model_wizard_premium_offer_url_type=OfferCard'
        )
        self.assertFragmentIn(
            response,
            {
                "market_model": [
                    {
                        "showcase": {
                            "items": [
                                {
                                    "isPremium": "1",
                                    "title": {"offercardUrl": Contains("clid%3D704")},
                                }
                            ]
                        }
                    }
                ]
            },
        )
        self.click_log.expect(reqid=4, dtype="offercard", position=0, data_url=Contains("clid%3D704"))

    def test_wprid_in_premium_offers_urls_in_model_wizard(self):
        """Проверяем wprid в ссылке премиального оффера во врезке модельного колдунщика
        под флагом market_model_wizard_premium_offer_url_type=OfferCard
        https://st.yandex-team.ru/MARKETOUT-27062
        https://st.yandex-team.ru/MARKETOUT-32791
        """
        # 2. Под конструктором
        # 2.1 С флагом market_parallel_wprids=0 wprid в зашифрованный урл не прокидывается.
        # При этом вне зависимости от наличия флага при наличии &wprid= в исходном запросе он
        # будет прописан в поле wprid= кликлога.
        response = self.report.request_bs(
            'place=parallel&text=lenovo p780&rids=213&reqid=3&wprid=user_wprid'
            '&rearr-factors=showcase_universal_model=1;'
            'market_premium_offer_in_model_wizard=1;'
            'market_parallel_wprids=0;'
            'market_model_wizard_premium_offer_url_type=OfferCard'
        )
        self.assertFragmentIn(
            response,
            {
                "market_model": [
                    {
                        "showcase": {
                            "items": [
                                {
                                    "title": {
                                        "text": {"__hl": {"text": "lenovo p780 cpa offer 3", "raw": True}},
                                        "urlForCounter": Contains("/wprid=user_wprid/"),
                                        "offercardUrl": Contains("/wprid=user_wprid/"),
                                    },
                                    "thumb": {
                                        "urlForCounter": Contains("/wprid=user_wprid/"),
                                        "offercardUrl": Contains("/wprid=user_wprid/"),
                                    },
                                    "isPremium": "1",
                                }
                            ]
                        }
                    }
                ]
            },
        )
        self.click_log.expect(
            reqid=3,
            dtype="offercard",
            position=0,
            ware_md5='1ZSxqUW11kXlniH4i9LYOw',
            data_url=Not(Contains("wprid%3Duser_wprid")),
            wprid='user_wprid',
        )

        # 2.2 Без флага market_parallel_wprids wprid в зашифрованный урл прокидывается.
        response = self.report.request_bs(
            'place=parallel&text=lenovo p780&rids=213&reqid=4&wprid=user_wprid'
            '&rearr-factors=showcase_universal_model=1;'
            'market_premium_offer_in_model_wizard=1;'
            'market_model_wizard_premium_offer_url_type=OfferCard'
        )
        self.assertFragmentIn(
            response,
            {
                "market_model": [
                    {
                        "showcase": {
                            "items": [
                                {
                                    "title": {
                                        "text": {"__hl": {"text": "lenovo p780 cpa offer 3", "raw": True}},
                                        "urlForCounter": Contains("/wprid=user_wprid/"),
                                        "offercardUrl": Contains("/wprid=user_wprid/"),
                                    },
                                    "thumb": {
                                        "urlForCounter": Contains("/wprid=user_wprid/"),
                                        "offercardUrl": Contains("/wprid=user_wprid/"),
                                    },
                                    "isPremium": "1",
                                }
                            ]
                        }
                    }
                ]
            },
        )
        self.click_log.expect(
            reqid=4,
            dtype="offercard",
            position=0,
            ware_md5='1ZSxqUW11kXlniH4i9LYOw',
            data_url=Contains("wprid%3Duser_wprid"),
            wprid='user_wprid',
        )

    def test_pp_in_premium_offers_urls_in_model_wizard(self):
        """Проверяем pp премиального оффера во врезке модельного колдунщика
        https://st.yandex-team.ru/MARKETOUT-27062
        """
        # На десктопе и паде pp=413, на таче pp=414
        for flag, pp in [('desktop', '413'), ('tablet', '413'), ('touch', '414')]:
            # Под конструктором
            response = self.report.request_bs_pb(
                'place=parallel&text=lenovo p780&rids=213'
                '&rearr-factors=showcase_universal_model=1;'
                'market_premium_offer_in_model_wizard=1;'
                'device=' + flag
            )
            self.assertFragmentIn(
                response,
                {
                    "market_model": {
                        "showcase": {
                            "items": [
                                {
                                    "title": {
                                        "text": {"__hl": {"text": "lenovo p780 cpa offer 3", "raw": True}},
                                        "urlForCounter": Contains("/pp={}/".format(pp)),
                                        "offercardUrl": Contains("/pp={}/".format(pp)),
                                    },
                                    "thumb": {
                                        "urlForCounter": Contains("/pp={}/".format(pp)),
                                        "offercardUrl": Contains("/pp={}/".format(pp)),
                                    },
                                    "isPremium": "1",
                                }
                            ]
                        }
                    }
                },
            )

    @classmethod
    def prepare_premium_offers_in_implicit_model_wizard(cls):
        cls.index.models += [
            Model(hyperid=101, title="implicit 1", ts=1),
            Model(hyperid=102, title="implicit 2", ts=2),
            Model(hyperid=103, title="implicit 3", ts=3),
            Model(hyperid=104, title="implicit 4", ts=4),
            Model(hyperid=105, title="implicit 5", ts=5),
            Model(hyperid=106, title="implicit 6", ts=6),
            Model(hyperid=107, title="implicit 7", ts=7),
        ]
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 1).respond(0.9)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 2).respond(0.8)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 3).respond(0.7)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 4).respond(0.6)

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 5).respond(0.5)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 6).respond(0.4)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 7).respond(0.3)

        cls.index.offers += [
            Offer(
                title='implicit 1 offer 1',
                hyperid=101,
                fesh=1,
                bid=100,
                delivery_options=[DeliveryOption(price=0)],
                picture=Picture(picture_id='iyC3nHslqLtqZJLygVAHeA', width=200, height=200, group_id=101),
                waremd5='9U1RL6CLQy4ydyWFfjWE2w',
            ),
            Offer(title='implicit 1 offer 2', hyperid=101, fesh=2, bid=90),
            Offer(title='implicit 2 offer 1', hyperid=102, fesh=1, bid=70),
            Offer(
                title='implicit 2 offer 2', hyperid=102, fesh=2, bid=80, delivery_options=[DeliveryOption(price=100)]
            ),
            Offer(title='implicit 3 offer 1', hyperid=103, fesh=1, bid=60),
            Offer(title='implicit 3 offer 2', hyperid=103, fesh=2, bid=50),
            Offer(title='implicit 4 offer 1', hyperid=104, fesh=1, bid=60),
            Offer(title='implicit 4 offer 2', hyperid=104, fesh=2, bid=50),
            Offer(title='implicit 5 offer 1', hyperid=105, fesh=1, bid=60),
            Offer(title='implicit 6 offer 1', hyperid=106, fesh=1, bid=60),
            Offer(title='implicit 7 offer 1', hyperid=107, fesh=1, bid=60),
        ]

    def test_premium_offers_in_implicit_model_wizard(self):
        """Проверяем премиальные офферы в колдунщике неявной модели
        под флагом market_premium_offer_in_implicit_model_wizard=1
        https://st.yandex-team.ru/MARKETOUT-27069
        """
        response = self.report.request_bs_pb(
            'place=parallel&text=implicit&rids=213&wprid=user_wprid'
            '&rearr-factors=market_premium_offer_in_implicit_model_wizard=1;'
            'market_implicit_model_wizard_premium_offers_count=4'
        )
        self.assertFragmentIn(
            response,
            {
                "market_implicit_model": {
                    "showcase": {
                        "premiumOffers": [
                            {
                                "thumb": {
                                    "source": Contains(
                                        "//avatars.mdst.yandex.net/get-marketpic/101/market_iyC3nHslqLtqZJLygVAHeA/100x100"
                                    ),
                                    "retinaSource": Contains(
                                        "//avatars.mdst.yandex.net/get-marketpic/101/market_iyC3nHslqLtqZJLygVAHeA/200x200"
                                    ),
                                    "height": "100",
                                    "width": "100",
                                    "url": Contains("http://www.shop-1.ru/"),
                                    "urlTouch": Contains("http://www.shop-1.ru/"),
                                    "urlForCounter": Contains(
                                        "//market-click2.yandex.ru/redir/dtype=market/", "/wprid=user_wprid/"
                                    ),
                                    "offercardUrl": Contains(
                                        "//market-click2.yandex.ru/redir/dtype=market/", "/wprid=user_wprid/"
                                    ),
                                },
                                "price": {"type": "average", "priceMax": "100", "currency": "RUR"},
                                "title": {
                                    "text": {"__hl": {"text": "implicit 1 offer 1", "raw": True}},
                                    "url": Contains("http://www.shop-1.ru/"),
                                    "urlTouch": Contains("http://www.shop-1.ru/"),
                                    "urlForCounter": Contains(
                                        "//market-click2.yandex.ru/redir/dtype=market/", "/wprid=user_wprid/"
                                    ),
                                    "offercardUrl": Contains(
                                        "//market-click2.yandex.ru/redir/dtype=market/", "/wprid=user_wprid/"
                                    ),
                                },
                                "greenUrl": {
                                    "text": "SHOP-1",
                                    "url": LikeUrl.of("//market.yandex.ru/shop--shop-1/1/reviews?clid=698"),
                                    "urlTouch": LikeUrl.of("//m.market.yandex.ru/grades-shop.xml?shop_id=1&clid=721"),
                                },
                                "delivery": {"text": "Доставка бесплатно"},
                                "isPremium": "1",
                            },
                            {
                                "title": {
                                    "text": {"__hl": {"text": "implicit 2 offer 2", "raw": True}},
                                },
                                "delivery": {"price": "100", "currency": "RUR"},
                                "isPremium": "1",
                            },
                            {
                                "title": {
                                    "text": {"__hl": {"text": "implicit 3 offer 1", "raw": True}},
                                },
                                "isPremium": "1",
                            },
                            {
                                "title": {
                                    "text": {"__hl": {"text": "implicit 4 offer 1", "raw": True}},
                                },
                                "isPremium": "1",
                            },
                        ]
                    }
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

    def test_premium_offers_urls_in_implicit_model_wizard(self):
        """Проверяем ссылки премиальных офферов в колдунщике неявной модели
        под флагом market_implicit_model_wizard_premium_offers_url_type
        https://st.yandex-team.ru/MARKETOUT-27069
        """
        request = (
            'place=parallel&text=implicit&rids=213'
            '&rearr-factors=market_premium_offer_in_implicit_model_wizard=1;'
            'market_implicit_model_wizard_premium_offers_count=1;'
            'market_implicit_model_wizard_premium_offers_touch_count=1'
        )

        # Под флагом market_implicit_model_wizard_premium_offers_url_type=External ссылки ведут в магазин.
        # Под флагом market_implicit_model_wizard_premium_offers_url_type=OfferCard ссылки ведут на карточку оффера.
        for flag, url_type in [('External', 'market'), ('OfferCard', 'offercard')]:
            response = self.report.request_bs_pb(
                request + ';market_implicit_model_wizard_premium_offers_url_type=' + flag
            )
            self.assertFragmentIn(
                response,
                {
                    "market_implicit_model": {
                        "showcase": {
                            "premiumOffers": [
                                {
                                    "title": {
                                        "urlForCounter": Contains(
                                            "//market-click2.yandex.ru/redir/dtype={}/".format(url_type)
                                        ),
                                        "offercardUrl": Contains(
                                            "//market-click2.yandex.ru/redir/dtype={}/".format(url_type)
                                        ),
                                    },
                                    "thumb": {
                                        "urlForCounter": Contains(
                                            "//market-click2.yandex.ru/redir/dtype={}/".format(url_type)
                                        ),
                                        "offercardUrl": Contains(
                                            "//market-click2.yandex.ru/redir/dtype={}/".format(url_type)
                                        ),
                                    },
                                    "isPremium": "1",
                                }
                            ]
                        }
                    }
                },
            )

        # По умолчанию ссылки ведут на десктопе в магазин, на таче - на карточку оффера
        for device, url_type in [('desktop', 'offercard'), ('touch', 'market')]:
            response = self.report.request_bs_pb(request + ';device=' + device)
            self.assertFragmentIn(
                response,
                {
                    "market_implicit_model": {
                        "showcase": {
                            "premiumOffers": [
                                {
                                    "title": {
                                        "urlForCounter": Contains(
                                            "//market-click2.yandex.ru/redir/dtype={}/".format(url_type)
                                        ),
                                        "offercardUrl": Contains(
                                            "//market-click2.yandex.ru/redir/dtype={}/".format(url_type)
                                        ),
                                    },
                                    "thumb": {
                                        "urlForCounter": Contains(
                                            "//market-click2.yandex.ru/redir/dtype={}/".format(url_type)
                                        ),
                                        "offercardUrl": Contains(
                                            "//market-click2.yandex.ru/redir/dtype={}/".format(url_type)
                                        ),
                                    },
                                    "isPremium": "1",
                                }
                            ]
                        }
                    }
                },
            )

    def test_filter_premium_offer_models_in_implicit_model_wizard(self):
        """Проверяем что в колдунщике неявной модели под флагом market_implicit_model_filter_premium_offer_model
        не показываются модели от которых взяты премиальные офферы
        https://st.yandex-team.ru/MARKETOUT-29172
        """
        request = (
            'place=parallel&text=implicit&rids=213&debug-doc-count=4'
            '&rearr-factors=market_premium_offer_in_implicit_model_wizard=1;'
            'market_implicit_model_wizard_premium_offers_count=4;'
        )

        # Без флага market_implicit_model_filter_premium_offer_model в выдачу попадают премиальные офферы
        # и модели, от которых они получены
        response = self.report.request_bs_pb(request)
        self.assertFragmentIn(
            response,
            {
                "market_implicit_model": {
                    "showcase": {
                        "items": [
                            {"title": {"text": {"__hl": {"text": "implicit 1", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "implicit 2", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "implicit 3", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "implicit 4", "raw": True}}}},
                        ],
                        "premiumOffers": [
                            {
                                "title": {"text": {"__hl": {"text": "implicit 1 offer 1", "raw": True}}},
                                "isPremium": "1",
                            },
                            {
                                "title": {"text": {"__hl": {"text": "implicit 2 offer 2", "raw": True}}},
                                "isPremium": "1",
                            },
                            {
                                "title": {"text": {"__hl": {"text": "implicit 3 offer 1", "raw": True}}},
                                "isPremium": "1",
                            },
                            {
                                "title": {"text": {"__hl": {"text": "implicit 4 offer 1", "raw": True}}},
                                "isPremium": "1",
                            },
                        ],
                    }
                }
            },
            allow_different_len=False,
        )

        # Под флагом market_implicit_model_filter_premium_offer_model=1 в выдачу не попадают модели,
        # от которых получены премиальные офферы
        response = self.report.request_bs_pb(request + 'market_implicit_model_filter_premium_offer_model=1')
        self.assertFragmentIn(
            response,
            {
                "market_implicit_model": {
                    "showcase": {
                        "items": EmptyList(),
                        "premiumOffers": [
                            {
                                "title": {"text": {"__hl": {"text": "implicit 1 offer 1", "raw": True}}},
                                "isPremium": "1",
                            },
                            {
                                "title": {"text": {"__hl": {"text": "implicit 2 offer 2", "raw": True}}},
                                "isPremium": "1",
                            },
                            {
                                "title": {"text": {"__hl": {"text": "implicit 3 offer 1", "raw": True}}},
                                "isPremium": "1",
                            },
                            {
                                "title": {"text": {"__hl": {"text": "implicit 4 offer 1", "raw": True}}},
                                "isPremium": "1",
                            },
                        ],
                    }
                }
            },
            allow_different_len=False,
        )

        # Под флагом market_implicit_model_filter_premium_offer_model=2 в выдачу не попадают модели,
        # от которых получены премиальные офферы. Запрашиваются дополнительные модели
        response = self.report.request_bs_pb(request + 'market_implicit_model_filter_premium_offer_model=2')
        self.assertFragmentIn(
            response,
            {
                "market_implicit_model": {
                    "showcase": {
                        "items": [
                            {"title": {"text": {"__hl": {"text": "implicit 5", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "implicit 6", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "implicit 7", "raw": True}}}},
                        ],
                        "premiumOffers": [
                            {
                                "title": {"text": {"__hl": {"text": "implicit 1 offer 1", "raw": True}}},
                                "isPremium": "1",
                            },
                            {
                                "title": {"text": {"__hl": {"text": "implicit 2 offer 2", "raw": True}}},
                                "isPremium": "1",
                            },
                            {
                                "title": {"text": {"__hl": {"text": "implicit 3 offer 1", "raw": True}}},
                                "isPremium": "1",
                            },
                            {
                                "title": {"text": {"__hl": {"text": "implicit 4 offer 1", "raw": True}}},
                                "isPremium": "1",
                            },
                        ],
                    }
                }
            },
            allow_different_len=False,
        )

    def test_filter_premium_offer_blue_models_in_implicit_model_wizard(self):
        """Проверяем что в колдунщике неявной модели под флагом market_implicit_model_filter_premium_offer_model
        не показываются синие модели от которых взяты премиальные офферы
        https://st.yandex-team.ru/MARKETOUT-29172
        """
        request = (
            'place=parallel&text=hakuna+matata'
            '&rearr-factors=market_implicit_blue_incut_model_count=1;'
            'market_premium_offer_in_implicit_model_wizard=1;'
            'market_implicit_model_wizard_premium_offers_count=1;'
        )

        # Без флага market_implicit_model_filter_premium_offer_model в выдачу попадают премиальные офферы
        # и модели, от которых они получены
        response = self.report.request_bs_pb(request)
        self.assertFragmentIn(
            response,
            {
                "market_implicit_model": {
                    "showcase": {
                        "items": [
                            {
                                "title": {"text": {"__hl": {"text": "hakuna matata", "raw": True}}},
                                "is_blue": NoKey("is_blue"),
                            },
                            {
                                "title": {"text": {"__hl": {"text": "hakuna matata 2", "raw": True}}},
                                "is_blue": NoKey("is_blue"),
                            },
                            {"title": {"text": {"__hl": {"text": "hakuna matata", "raw": True}}}, "is_blue": "1"},
                        ],
                        "premiumOffers": [
                            {"title": {"text": {"__hl": {"text": "hakuna matata", "raw": True}}}, "isPremium": "1"}
                        ],
                    }
                }
            },
            allow_different_len=False,
        )

        # Под флагом market_implicit_model_filter_premium_offer_model=1 в выдачу не попадают модели,
        # от которых получены премиальные офферы
        response = self.report.request_bs_pb(request + 'market_implicit_model_filter_premium_offer_model=1')
        self.assertFragmentIn(
            response,
            {
                "market_implicit_model": {
                    "showcase": {
                        "items": [
                            {
                                "title": {"text": {"__hl": {"text": "hakuna matata 2", "raw": True}}},
                                "is_blue": NoKey("is_blue"),
                            },
                            {"title": {"text": {"__hl": {"text": "hakuna matata 2", "raw": True}}}, "is_blue": "1"},
                        ],
                        "premiumOffers": [
                            {"title": {"text": {"__hl": {"text": "hakuna matata", "raw": True}}}, "isPremium": "1"}
                        ],
                    }
                }
            },
            allow_different_len=False,
        )

        # Под флагом market_implicit_model_filter_premium_offer_model=2 в выдачу не попадают модели,
        # от которых получены премиальные офферы. Запрашиваются дополнительные модели
        response = self.report.request_bs_pb(request + 'market_implicit_model_filter_premium_offer_model=2')
        self.assertFragmentIn(
            response,
            {
                "market_implicit_model": {
                    "showcase": {
                        "items": [
                            {
                                "title": {"text": {"__hl": {"text": "hakuna matata 2", "raw": True}}},
                                "is_blue": NoKey("is_blue"),
                            },
                            {"title": {"text": {"__hl": {"text": "hakuna matata 2", "raw": True}}}, "is_blue": "1"},
                        ],
                        "premiumOffers": [
                            {"title": {"text": {"__hl": {"text": "hakuna matata", "raw": True}}}, "isPremium": "1"}
                        ],
                    }
                }
            },
            allow_different_len=False,
        )

    def test_bill_min_bid_in_premium_offers_in_implicit_model_wizard(self):
        """По умолчанию и под флагом market_parallel_bill_minbid_in_premium_offers=1 списывается минставка,
        а примеальная ставка пишется в fuid=premium_cp.
        Под флагом market_parallel_bill_minbid_in_premium_offers=0 списывается премиальная ставка
        https://st.yandex-team.ru/MARKETOUT-27069
        Для карточки оффера примеальная ставка пишется в параметр &rs, а fuid=premium_cp не добавляется.
        https://st.yandex-team.ru/MARKETOUT-28790
        """

        def test(response, url):
            self.assertFragmentIn(
                response,
                {
                    "market_implicit_model": {
                        "showcase": {
                            "premiumOffers": [
                                {
                                    "title": {
                                        "text": {"__hl": {"text": "implicit 1 offer 1", "raw": True}},
                                        "urlForCounter": url,
                                        "offercardUrl": url,
                                    },
                                    "thumb": {
                                        "urlForCounter": url,
                                        "offercardUrl": url,
                                    },
                                    "isPremium": "1",
                                }
                            ]
                        }
                    }
                },
            )

        has_fuid = Contains('/fuid=premium_cp=91/')
        no_fuid = Not(Contains('/fuid=premium_cp='))

        for device in ('desktop', 'touch', 'tablet'):
            for url_type in ('', 'External', 'OfferCard'):
                for flag, cp, fuid in [
                    ('', '/cp=1/', has_fuid),
                    ('market_parallel_bill_minbid_in_premium_offers=1', '/cp=1/', has_fuid),
                    ('market_parallel_bill_minbid_in_premium_offers=0', '/cp=91/', no_fuid),
                ]:

                    request = (
                        'place=parallel&text=implicit&rids=213'
                        '&rearr-factors=market_premium_offer_in_implicit_model_wizard=1;'
                        'market_implicit_model_wizard_premium_offers_count=1;'
                        'market_implicit_model_wizard_premium_offers_touch_count=1;'
                        'device={};{};'.format(device, flag)
                    )
                    if url_type:
                        request += 'market_implicit_model_wizard_premium_offers_url_type=' + url_type

                    # Если тип ссылки не задан, то на десктопе ведем на КО
                    if url_type == 'OfferCard' or (url_type == '' and device == 'desktop'):
                        fuid = no_fuid

                    response = self.report.request_bs_pb(request)
                    test(response, Contains(cp))
                    test(response, fuid)

    def test_pp_in_premium_offers_urls_in_implicit_model_wizard(self):
        """Проверяем pp премиальных офферов в колдунщике неявной модели
        https://st.yandex-team.ru/MARKETOUT-27069
        """
        # На десктопе и паде pp=413, на таче pp=414
        for flag, pp in [('desktop', '413'), ('tablet', '413'), ('touch', '414')]:
            response = self.report.request_bs_pb(
                'place=parallel&text=implicit&rids=213'
                '&rearr-factors=market_premium_offer_in_implicit_model_wizard=1;'
                'market_implicit_model_wizard_premium_offers_count=1;'
                'market_implicit_model_wizard_premium_offers_touch_count=1;'
                'device=' + flag
            )
            self.assertFragmentIn(
                response,
                {
                    "market_implicit_model": {
                        "showcase": {
                            "premiumOffers": [
                                {
                                    "title": {
                                        "urlForCounter": Contains("/pp={}/".format(pp)),
                                        "offercardUrl": Contains("/pp={}/".format(pp)),
                                    },
                                    "thumb": {
                                        "urlForCounter": Contains("/pp={}/".format(pp)),
                                        "offercardUrl": Contains("/pp={}/".format(pp)),
                                    },
                                    "isPremium": "1",
                                }
                            ]
                        }
                    }
                },
            )

    def test_premium_offers_count_in_implicit_model_wizard(self):
        """Проверка количества премиальных офферов для вьютайпов колдунщика неявной модели
        https://st.yandex-team.ru/MARKETOUT-27069?
        """
        request = (
            'place=parallel&text=implicit&rids=213&&rearr-factors='
            'market_enable_implicit_model_wiz_center_incut=1;'
            'market_enable_implicit_model_adg_wiz=1;'
            'market_premium_offer_in_implicit_model_wizard=1;'
            'market_implicit_model_wizard_premium_offers_count=4;'
            'market_implicit_model_wizard_premium_offers_central_count=3;'
            'market_implicit_model_wizard_premium_offers_touch_count=2;'
            'market_implicit_model_wizard_premium_offers_adg_count=1;'
            'market_implicit_model_wizard_center_incut_meta_threshold=0;'
        )

        # Для десктопа
        response = self.report.request_bs_pb(request + 'device=desktop')
        self.assertFragmentIn(response, {"market_implicit_model": {"showcase": {"premiumOffers": ElementCount(4)}}})
        self.assertFragmentIn(
            response, {"market_implicit_model_center_incut": {"showcase": {"premiumOffers": ElementCount(3)}}}
        )
        self.assertFragmentIn(
            response, {"market_implicit_model_adg_wizard": {"showcase": {"premiumOffers": ElementCount(1)}}}
        )

        # Для тача
        response = self.report.request_bs_pb(request + 'device=touch')
        self.assertFragmentIn(response, {"market_implicit_model": {"showcase": {"premiumOffers": ElementCount(2)}}})
        self.assertFragmentIn(
            response, {"market_implicit_model_adg_wizard": {"showcase": {"premiumOffers": ElementCount(1)}}}
        )

    def test_premium_offers_clids(self):
        """Проверяем генерацию кликурлов премиального оффера с разными клидами
        для разных колдунщиков.

        https://st.yandex-team.ru/MARKETOUT-27451
        https://st.yandex-team.ru/MARKETOUT-28091
        """

        def do_test(request, urls_type, is_touch, expected_model_clid, expected_implicit_clid):
            request += (
                '&rearr-factors=market_premium_offer_in_model_wizard=1;market_premium_offer_in_implicit_model_wizard=1'
            )
            request += ';market_model_wizard_premium_offer_url_type={0};market_implicit_model_wizard_premium_offers_url_type={1}'.format(
                urls_type, urls_type
            )
            request += ';market_implicit_model_wizard_premium_offers_count=1;market_implicit_model_wizard_premium_offers_touch_count=1'

            if is_touch:
                request += '&touch=1'

            implicit_clid = (
                Contains("clid%3D{0}".format(expected_implicit_clid))
                if expected_implicit_clid is not None
                else Not(Contains("clid"))
            )
            model_clid = (
                Contains("clid={0}".format(expected_model_clid))
                if expected_model_clid is not None
                else Not(Contains("clid"))
            )
            model_clid_encr = (
                Contains("clid%3D{0}".format(expected_model_clid))
                if expected_model_clid is not None
                else Not(Contains("clid"))
            )

            if is_touch:
                model_clid = (
                    Contains("clid={0}".format(502)) if expected_model_clid is not None else Not(Contains("clid"))
                )

            response = self.report.request_bs_pb(request)
            self.assertFragmentIn(
                response,
                {
                    "market_implicit_model": {
                        "showcase": {
                            "premiumOffers": [
                                {
                                    "title": {
                                        "urlForCounter": implicit_clid,
                                    },
                                    "thumb": {
                                        "urlForCounter": implicit_clid,
                                    },
                                    "isPremium": "1",
                                }
                            ]
                        }
                    }
                },
            )
            self.assertFragmentIn(
                response,
                {
                    "market_model": {
                        "showcase": {
                            "items": [
                                {
                                    "title": {
                                        "url": model_clid,
                                        "offercardUrl": model_clid_encr,
                                    }
                                }
                            ]
                        }
                    }
                },
            )

        white_request = 'place=parallel&text=implicit&rids=213'  # Запрос для белого премиального оффера
        blue_request = 'place=parallel&text=hakuna+matata'  # Запрос для синего премиального оффера

        # 1. Для внешних кликурлов в белом и синем случае генерация отличается
        # 1.1 Белые внешние ссылки генерируются без клидов
        # Деск
        do_test(
            request=white_request,
            urls_type='External',
            is_touch=False,
            expected_model_clid=None,
            expected_implicit_clid=None,
        )
        # Тач
        do_test(
            request=white_request,
            urls_type='External',
            is_touch=True,
            expected_model_clid=None,
            expected_implicit_clid=None,
        )

        # 1.2 Синие внешние ссылки генерируются с клидами
        # Деск
        do_test(
            request=blue_request,
            urls_type='External',
            is_touch=False,
            expected_model_clid=502,
            expected_implicit_clid=698,
        )
        # Тач
        do_test(
            request=blue_request,
            urls_type='External',
            is_touch=True,
            expected_model_clid=704,
            expected_implicit_clid=721,
        )

    def test_premium_offers_implicit_clids(self):
        """Проверяем генерацию кликурлов премиальных офферов для разных вьютайпов
        колдунщика неявной модели

        https://st.yandex-team.ru/MARKETOUT-28204
        """

        def do_check(extra_flag, touch, viewtype, expected_implicit_clid, expected_implicit_adg_clid):
            request = 'place=parallel&text=implicit&rids=213'
            request += '&rearr-factors=market_premium_offer_in_implicit_model_wizard=1'
            request += ';market_implicit_model_wizard_premium_offers_url_type=OfferCard'

            request += ';market_implicit_model_wizard_premium_offers_count=1'
            request += ';market_implicit_model_wizard_premium_offers_central_count=1'
            request += ';market_implicit_model_wizard_premium_offers_touch_count=1'
            request += ';market_implicit_model_wizard_premium_offers_adg_count=1'

            request += extra_flag
            if touch:
                request += '&touch=1'

            response = self.report.request_bs_pb(request)
            implicit_clid = Contains("clid%3D{0}".format(expected_implicit_clid))
            implicit_adg_clid = Contains("clid%3D{0}".format(expected_implicit_adg_clid))
            self.assertFragmentIn(
                response,
                {
                    viewtype: {
                        "showcase": {
                            "premiumOffers": [
                                {
                                    "title": {
                                        "urlForCounter": implicit_clid,
                                        "offercardUrl": implicit_adg_clid,
                                    },
                                    "thumb": {
                                        "urlForCounter": implicit_clid,
                                        "offercardUrl": implicit_adg_clid,
                                    },
                                    "isPremium": "1",
                                }
                            ]
                        }
                    }
                },
            )

        # 1. Обычная неявная модель
        # 1.1 Деск
        do_check(
            extra_flag='',
            touch=False,
            viewtype='market_implicit_model',
            expected_implicit_clid=698,
            expected_implicit_adg_clid=915,
        )
        # 1.2 Тач
        do_check(
            extra_flag='',
            touch=True,
            viewtype='market_implicit_model',
            expected_implicit_clid=721,
            expected_implicit_adg_clid=921,
        )

        # 2. Центральная неявная модель
        # 2.1 Обычный вьютайп, деск
        do_check(
            extra_flag=';market_enable_implicit_model_wiz_center_incut=1',
            touch=False,
            viewtype='market_implicit_model',
            expected_implicit_clid=698,
            expected_implicit_adg_clid=915,
        )
        # 2.2 Обычный вьютайп, тач
        do_check(
            extra_flag=';market_enable_implicit_model_wiz_center_incut=1',
            touch=True,
            viewtype='market_implicit_model',
            expected_implicit_clid=721,
            expected_implicit_adg_clid=921,
        )
        # 2.3 Центральный вьютайп, деск
        do_check(
            extra_flag=';market_enable_implicit_model_wiz_center_incut=1',
            touch=False,
            viewtype='market_implicit_model_center_incut',
            expected_implicit_clid=836,
            expected_implicit_adg_clid=915,
        )
        # 2.4 Центральный вьютайп, тач: в этом сценарии не воспроизвелся, но не вижу в этом трагедии

        # 3. РГ-неявная модель
        # 3.1 Обычный вьютайп, деск
        do_check(
            extra_flag=';market_enable_implicit_model_adg_wiz=1',
            touch=False,
            viewtype='market_implicit_model',
            expected_implicit_clid=698,
            expected_implicit_adg_clid=915,
        )
        # 3.2 Обычный вьютайп, тач
        do_check(
            extra_flag=';market_enable_implicit_model_adg_wiz=1',
            touch=True,
            viewtype='market_implicit_model',
            expected_implicit_clid=721,
            expected_implicit_adg_clid=921,
        )
        # 3.3 РГ-вьютайп, деск
        do_check(
            extra_flag=';market_enable_implicit_model_adg_wiz=1',
            touch=False,
            viewtype='market_implicit_model_adg_wizard',
            expected_implicit_clid=915,
            expected_implicit_adg_clid=915,
        )
        # 3.4 РГ-вьютайп, тач
        do_check(
            extra_flag=';market_enable_implicit_model_adg_wiz=1',
            touch=True,
            viewtype='market_implicit_model_adg_wizard',
            expected_implicit_clid=921,
            expected_implicit_adg_clid=921,
        )

        # 4. Вьютайп-врезка. Проверяем, что при этом обычный вьютайп не ломается.
        # 4.1 Деск
        do_check(
            extra_flag=';market_enable_implicit_model_wiz_without_incut=1',
            touch=False,
            viewtype='market_implicit_model',
            expected_implicit_clid=698,
            expected_implicit_adg_clid=915,
        )
        # 4.2 Тач
        do_check(
            extra_flag=';market_enable_implicit_model_wiz_without_incut=1',
            touch=True,
            viewtype='market_implicit_model',
            expected_implicit_clid=721,
            expected_implicit_adg_clid=921,
        )

    def test_premium_offers_urls_pp_in_model_wizard(self):
        """Проверяем, что в модельном колдунщике ссылка премиального оффера на КО в параметре &rs содержит pp=116
        и fuid_premium_cp=91
        https://st.yandex-team.ru/MARKETOUT-27948
        https://st.yandex-team.ru/MARKETOUT-28790
        """
        # The hardcoded value rs[0] represents the following:
        # python -c 'import base64, sys, zlib; sys.stdout.write(zlib.decompress(base64.urlsafe_b64decode("eJyz4uIoEWKQYFBi0IgGAAnpAZA=")))' | protoc --decode_raw
        # 1: 116 (premium offercard)
        # 2: "" (test-buckets from the request)
        # 3: 0 (this offer's position; 2, 3, 4 for the rest)
        # 4: "" (subreqid from the request)
        # 5: 91 (fuid premium cp)
        rs = '%26rs%3DeJyz4uIoEWKQYFBi0IgGAAnpAZA'

        request = (
            'place=parallel&text=lenovo p780&rids=213&rearr-factors=market_premium_offer_in_model_wizard=1;'
            'market_model_wizard_premium_offer_url_type=OfferCard;'
        )
        for device in ('device=desktop;', 'device=touch;'):
            # Модельный колдунщик под конструктором
            response = self.report.request_bs_pb(request + device + 'showcase_universal_model=1;')
            self.assertFragmentIn(
                response,
                {
                    "market_model": {
                        "showcase": {
                            "items": [
                                {
                                    "title": {
                                        "urlForCounter": Contains(
                                            "//market-click2.yandex.ru/redir/dtype=offercard/", rs
                                        ),
                                        "offercardUrl": Contains(
                                            "//market-click2.yandex.ru/redir/dtype=offercard/", rs
                                        ),
                                    },
                                    "thumb": {
                                        "urlForCounter": Contains(
                                            "//market-click2.yandex.ru/redir/dtype=offercard/", rs
                                        ),
                                        "offercardUrl": Contains(
                                            "//market-click2.yandex.ru/redir/dtype=offercard/", rs
                                        ),
                                    },
                                    "isPremium": "1",
                                }
                            ]
                        }
                    }
                },
            )

    def test_premium_offers_urls_pp_in_implicit_model_wizard(self):
        """Проверяем, что в колдунщике неявной модели ссылки премиальных офферов на КО в параметре &rs содержат pp=116
        и fuid_premium_cp=91
        https://st.yandex-team.ru/MARKETOUT-27948
        https://st.yandex-team.ru/MARKETOUT-28790
        """
        # The hardcoded value rs[0] represents the following:
        # python -c 'import base64, sys, zlib; sys.stdout.write(zlib.decompress(base64.urlsafe_b64decode("eJyz4uIoEWKQYFBi0IgGAAnpAZA=")))' | protoc --decode_raw
        # 1: 116 (premium offercard)
        # 2: "" (test-buckets from the request)
        # 3: 0 (this offer's position; 2, 3, 4 for the rest)
        # 4: "" (subreqid from the request)
        # 5: 91 (fuid premium cp)
        rs = '%26rs%3DeJyz4uIoEWKQYFBi0IgGAAnpAZA'

        request = (
            'place=parallel&text=implicit&rids=213&rearr-factors=market_premium_offer_in_implicit_model_wizard=1;'
            'market_implicit_model_wizard_premium_offers_url_type=OfferCard;'
            'market_implicit_model_wizard_premium_offers_count=1;'
            'market_implicit_model_wizard_premium_offers_touch_count=1;'
        )

        for device in ('device=desktop;', 'device=touch;'):
            response = self.report.request_bs_pb(request + device)
            self.assertFragmentIn(
                response,
                {
                    "market_implicit_model": {
                        "showcase": {
                            "premiumOffers": [
                                {
                                    "title": {
                                        "urlForCounter": Contains(
                                            "//market-click2.yandex.ru/redir/dtype=offercard/", rs
                                        ),
                                        "offercardUrl": Contains(
                                            "//market-click2.yandex.ru/redir/dtype=offercard/", rs
                                        ),
                                    },
                                    "thumb": {
                                        "urlForCounter": Contains(
                                            "//market-click2.yandex.ru/redir/dtype=offercard/", rs
                                        ),
                                        "offercardUrl": Contains(
                                            "//market-click2.yandex.ru/redir/dtype=offercard/", rs
                                        ),
                                    },
                                    "isPremium": "1",
                                }
                            ]
                        }
                    }
                },
            )

    def test_offercard_premium_offers_url(self):
        """Проверяем, что премиальный оффер в ссылке с карточки оффера в магазин содержит параметры cp, fuid=premium_cp= и pp=116
        Эти параметры приходят на КО в параметре &rs в ссылке с колдунщика
        https://st.yandex-team.ru/MARKETOUT-28790
        """
        # The hardcoded value rs[0] represents the following:
        # python -c 'import base64, sys, zlib; sys.stdout.write(zlib.decompress(base64.urlsafe_b64decode("eJyz4uIoEWKQYFBi0IgGAAnpAZA=")))' | protoc --decode_raw
        # 1: 116 (premium offercard)
        # 2: "" (test-buckets from the request)
        # 3: 0 (this offer's position; 2, 3, 4 for the rest)
        # 4: "" (subreqid from the request)
        # 5: 91 (fuid premium cp)
        rs = 'eJyz4uIoEWKQYFBi0IgGAAnpAZA='

        waremd5 = '9U1RL6CLQy4ydyWFfjWE2w'
        request = 'place=offerinfo&offerid={}&rids=213&regset=1&show-urls=external&rs={}'.format(waremd5, rs)
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {
                            "entity": "offer",
                            "urls": {
                                "encrypted": Contains("/cp=1/", "/fuid=premium_cp=91/", "/pp=116/"),
                            },
                        }
                    ]
                }
            },
        )

    @classmethod
    def prepare_premium_offers_in_offers_wizard(cls):
        cls.index.models += [Model(hyperid=401090, title="alcatel q146"), Model(hyperid=401091, title="alcatel t147")]
        cls.index.shops += [
            Shop(fesh=11, priority_region=213, regions=[225], cpa=Shop.CPA_REAL),
            Shop(fesh=12, priority_region=213, regions=[225], cpa=Shop.CPA_REAL),
            Shop(fesh=13, priority_region=213, regions=[225], cpa=Shop.CPA_REAL),
            Shop(fesh=14, priority_region=213, regions=[225], cpa=Shop.CPA_REAL),
            Shop(fesh=15, priority_region=213, regions=[225], cpa=Shop.CPA_REAL),
        ]

        cls.index.offers += [
            Offer(title='alcatel q146 cpa offer 1', hyperid=401090, fesh=11, bid=90),
            Offer(title='alcatel j148 cpa offer 2', fesh=12, bid=80),
            Offer(title='alcatel q146 cpa offer 3', hyperid=401090, fesh=13, bid=100),
            Offer(title='alcatel t147 cpa offer 4', hyperid=401091, fesh=14, bid=70),
            Offer(title='alcatel q146 cpa offer 5', hyperid=401090, fesh=15, bid=60),
        ]

    def test_premium_offers_in_offers_wizard(self):
        """Премиальные офферы в офферном колдунщике

        https://st.yandex-team.ru/MARKETOUT-27070 (тест будет изменяться при разработке подтикетов)
        """

        def do_test(request, expected_offers, expected_premium_offers=[]):
            response = self.report.request_bs_pb(request)
            self.assertFragmentIn(
                response,
                {
                    'market_offers_wizard': {
                        "showcase": {
                            "items": [
                                {
                                    "title": {
                                        "text": {"__hl": {"text": title}},
                                    },
                                    "isPremium": NoKey("isPremium"),
                                }
                                for title in expected_offers
                            ]
                        }
                    }
                },
                preserve_order=True,
                allow_different_len=False,
            )

            if expected_premium_offers:
                self.assertFragmentIn(
                    response,
                    {
                        'market_offers_wizard': {
                            "showcase": {
                                "premiumOffers": [
                                    {
                                        "title": {
                                            "text": {"__hl": {"text": title}},
                                        },
                                        "isPremium": "1",
                                    }
                                    for title in expected_premium_offers
                                ]
                            }
                        }
                    },
                    preserve_order=True,
                    allow_different_len=False,
                )
            else:
                self.assertFragmentIn(
                    response, {'market_offers_wizard': {"showcase": {"premiumOffers": NoKey("premiumOffers")}}}
                )

        request = 'place=parallel&text=alcatel&rids=213'

        # 1. Дефолтный align (3)
        # 1.1 Без флагов
        do_test(
            request,
            [
                ('alcatel q146 cpa offer 3'),
                ('alcatel q146 cpa offer 1'),
                ('alcatel j148 cpa offer 2'),
            ],
        )

        # 1.2 Проверяем добавление моделей в начало врезки
        do_test(
            request + '&rearr-factors=market_offers_wizard_model_docs_min_size=1',
            [
                # Добавилась только 1 модель, хотя могла бы быть и вторая.
                # https://st.yandex-team.ru/MARKETOUT-28726#5df0d030aff2244362ffe82c
                ('alcatel q146'),  # model
                ('alcatel q146 cpa offer 3'),
                ('alcatel q146 cpa offer 1'),
                ('alcatel j148 cpa offer 2'),
                ('alcatel t147 cpa offer 4'),
                ('alcatel q146 cpa offer 5'),
            ],
        )

        # 1.3 Включаем премиальный в офферном (добавляются оба)
        do_test(
            request
            + '&rearr-factors=market_premium_offer_in_offers_wizard=1;market_offers_wizard_premium_offers_count=9',
            [
                # usual offers
                ('alcatel q146 cpa offer 1'),
                ('alcatel j148 cpa offer 2'),
                ('alcatel q146 cpa offer 5'),
            ],
            [
                # premium offers
                ('alcatel q146 cpa offer 3'),
                ('alcatel t147 cpa offer 4'),
            ],
        )

        # 1.4 При включении одновременно премиального и добавления моделей, если один и тот же
        # оффер образует и модель, и премиальный, то при добавлении премиального побеждается и обычный оффер, и модель
        # https://st.yandex-team.ru/MARKETOUT-28726#5dd46df329af1d001c7ef355
        do_test(
            request
            + '&rearr-factors=market_offers_wizard_model_docs_min_size=1;market_premium_offer_in_offers_wizard=1;market_offers_wizard_premium_offers_count=9',
            [
                # usual offers
                ('alcatel q146 cpa offer 1'),
                ('alcatel j148 cpa offer 2'),
                ('alcatel q146 cpa offer 5'),
            ],
            [
                # premium offers
                ('alcatel q146 cpa offer 3'),
                ('alcatel t147 cpa offer 4'),
            ],
        )

        # 2. Без align
        request += '&rearr-factors=market_offers_incut_align=0'
        request += ';market_offers_wizard_premium_offers_count=9'

        # 2.1 Без флагов
        do_test(
            request,
            [
                ('alcatel q146 cpa offer 3'),
                ('alcatel q146 cpa offer 1'),
                ('alcatel j148 cpa offer 2'),
                ('alcatel t147 cpa offer 4'),
                ('alcatel q146 cpa offer 5'),
            ],
        )

        # 2.2 Проверяем добавление моделей в начало врезки (добавляются обе)
        do_test(
            request + ';market_offers_wizard_model_docs_min_size=1',
            [
                ('alcatel q146'),  # model
                ('alcatel t147'),  # model
                ('alcatel q146 cpa offer 3'),
                ('alcatel q146 cpa offer 1'),
                ('alcatel j148 cpa offer 2'),
                ('alcatel t147 cpa offer 4'),
                ('alcatel q146 cpa offer 5'),
            ],
        )

        # 2.3 Включаем премиальный в офферном (добавляются оба)
        do_test(
            request + ';market_premium_offer_in_offers_wizard=1',
            [
                # usual offers
                ('alcatel q146 cpa offer 1'),
                ('alcatel j148 cpa offer 2'),
                ('alcatel q146 cpa offer 5'),
            ],
            [
                # premium offers
                ('alcatel q146 cpa offer 3'),
                ('alcatel t147 cpa offer 4'),
            ],
        )

        # 2.4 При включении одновременно премиального и добавления моделей, если один и тот же
        # оффер образует и модель, и премиальный, то при добавлении премиального побеждается и обычный оффер, и модель
        # https://st.yandex-team.ru/MARKETOUT-28726#5dd46df329af1d001c7ef355
        do_test(
            request + ';market_offers_wizard_model_docs_min_size=1;market_premium_offer_in_offers_wizard=1',
            [
                # usual offers
                ('alcatel q146 cpa offer 1'),
                ('alcatel j148 cpa offer 2'),
                ('alcatel q146 cpa offer 5'),
            ],
            [
                # premium offers
                ('alcatel q146 cpa offer 3'),
                ('alcatel t147 cpa offer 4'),
            ],
        )

        # 3. Проверяем легкую ручку
        # https://st.yandex-team.ru/MARKETOUT-28188
        do_test(
            request
            + '&rearr-factors=market_premium_offer_in_offers_wizard=1;market_offers_wizard_light_premium=1;market_offers_wizard_premium_offers_count=9',
            [
                # usual offers
                ('alcatel q146 cpa offer 1'),
                ('alcatel j148 cpa offer 2'),
                ('alcatel q146 cpa offer 5'),
            ],
            [
                # premium offers
                ('alcatel q146 cpa offer 3'),
                ('alcatel t147 cpa offer 4'),
            ],
        )

    @classmethod
    def prepare_premium_offers_count_in_offers_wizard(cls):
        cls.index.models += [
            Model(hyperid=501090, title="cybertruck 1"),
            Model(hyperid=501091, title="cybertruck 2"),
            Model(hyperid=501092, title="cybertruck 3"),
            Model(hyperid=501093, title="cybertruck 4"),
        ]
        cls.index.shops += [
            Shop(fesh=21, priority_region=213, regions=[225], cpa=Shop.CPA_REAL),
            Shop(fesh=22, priority_region=213, regions=[225], cpa=Shop.CPA_REAL),
            Shop(fesh=23, priority_region=213, regions=[225], cpa=Shop.CPA_REAL),
            Shop(fesh=24, priority_region=213, regions=[225], cpa=Shop.CPA_REAL),
            Shop(fesh=25, priority_region=213, regions=[225], cpa=Shop.CPA_REAL),
            Shop(fesh=26, priority_region=213, regions=[225], cpa=Shop.CPA_REAL),
            Shop(fesh=27, priority_region=213, regions=[225], cpa=Shop.CPA_REAL),
            Shop(fesh=28, priority_region=213, regions=[225], cpa=Shop.CPA_REAL),
            Shop(fesh=29, priority_region=213, regions=[225], cpa=Shop.CPA_REAL),
        ]

        cls.index.offers += [
            Offer(title='cybertruck 1 offer 1', hyperid=501090, fesh=21, bid=90),
            Offer(title='cybertruck no model offer 2', fesh=22, bid=80),
            Offer(title='cybertruck 1 offer 3', hyperid=501090, fesh=23, bid=100),
            Offer(title='cybertruck 2 offer 4', hyperid=501091, fesh=24, bid=70),
            Offer(title='cybertruck 1 offer 5', hyperid=501090, fesh=25, bid=60),
            Offer(title='cybertruck 3 offer 6', hyperid=501092, fesh=26, bid=50),
            Offer(title='cybertruck 3 offer 7', hyperid=501092, fesh=27, bid=40),
            Offer(title='cybertruck 4 offer 8', hyperid=501093, fesh=28, bid=30),
            Offer(title='cybertruck no model offer 9', fesh=29, bid=20),
        ]

    def test_premium_offers_count_in_offers_wizard(self):
        """Тестируем флаги для задания количества премиальных офферов в разных вьютайпах
        https://st.yandex-team.ru/MARKETOUT-28189
        """

        def do_test(request, wizard_key, premium_offers_count):
            all_usual_offers = [
                'cybertruck 1 offer 3',
                'cybertruck 1 offer 1',
                'cybertruck no model offer 2',
                'cybertruck 2 offer 4',
                'cybertruck 1 offer 5',
                'cybertruck 3 offer 6',
                'cybertruck 3 offer 7',
                'cybertruck 4 offer 8',
                'cybertruck no model offer 9',
            ]
            all_premium_offers = [
                'cybertruck 1 offer 3',
                'cybertruck 2 offer 4',
                'cybertruck 3 offer 6',
                'cybertruck 4 offer 8',
            ]

            expected_premium_offers = all_premium_offers[:premium_offers_count]
            expected_offers = [x for x in all_usual_offers if x not in expected_premium_offers]

            response = self.report.request_bs_pb(request)
            self.assertFragmentIn(
                response,
                {
                    wizard_key: {
                        "showcase": {
                            "items": [
                                {
                                    "title": {
                                        "text": {"__hl": {"text": title}},
                                    },
                                    "isPremium": NoKey("isPremium"),
                                }
                                for title in expected_offers
                            ]
                        }
                    }
                },
                preserve_order=True,
                allow_different_len=False,
            )

            if expected_premium_offers:
                self.assertFragmentIn(
                    response,
                    {
                        wizard_key: {
                            "showcase": {
                                "premiumOffers": [
                                    {
                                        "title": {
                                            "text": {"__hl": {"text": title}},
                                        },
                                        "isPremium": "1",
                                    }
                                    for title in expected_premium_offers
                                ]
                            }
                        }
                    },
                    preserve_order=True,
                    allow_different_len=False,
                )
            else:
                self.assertFragmentIn(response, {wizard_key: {"showcase": {"premiumOffers": NoKey("premiumOffers")}}})

        request = 'place=parallel&text=cybertruck&rids=213'
        request += '&rearr-factors=market_offers_incut_align=0'  # тестируем без выравнивания

        # 1. Без флага market_premium_offer_in_offers_wizard=1 ни в какой из вьютайпов
        # премиальные офферы не добавляются
        # 1.1 Проверям вьютайп market_offers_wizard
        do_test(request, 'market_offers_wizard', premium_offers_count=0)
        # 1.2 Проверям тач
        do_test(
            request + ';market_offers_wizard_premium_offers_touch_count=3;offers_touch=1&touch=1',
            'market_offers_wizard',
            premium_offers_count=0,
        )
        # 1.3 Проверям вьютайп market_offers_adg_wizard
        do_test(
            request + ';market_enable_offers_adg_wiz=1;market_offers_wizard_premium_offers_adg_count=2',
            'market_offers_adg_wizard',
            premium_offers_count=0,
        )
        # 1.4 Проверям вьютайп market_offers_wizard_right_incut
        do_test(
            request + ';market_enable_offers_wiz_right_incut=1;market_offers_wizard_premium_offers_right_count=3',
            'market_offers_wizard_right_incut',
            premium_offers_count=0,
        )
        # 1.5 Проверям вьютайп market_offers_wizard_center_incut
        do_test(
            request + ';market_enable_offers_wiz_center_incut=1;market_offers_wizard_premium_offers_center_count=4',
            'market_offers_wizard_center_incut',
            premium_offers_count=0,
        )

        # 2. Включаем премиальный
        request += ';market_premium_offer_in_offers_wizard=1'

        # 2.1 Проверям вьютайп market_offers_wizard
        do_test(
            request + ';market_offers_wizard_premium_offers_count=2', 'market_offers_wizard', premium_offers_count=2
        )

        # 2.2 Проверям тач
        do_test(
            request + ';market_offers_wizard_premium_offers_touch_count=3;offers_touch=1&touch=1',
            'market_offers_wizard',
            premium_offers_count=3,
        )

        # 2.3 Проверям вьютайп market_offers_adg_wizard
        do_test(
            request
            + ';market_enable_offers_adg_wiz=1;market_offers_wizard_premium_offers_count=3;market_offers_wizard_premium_offers_adg_count=2',
            'market_offers_wizard',
            premium_offers_count=3,
        )
        do_test(
            request
            + ';market_enable_offers_adg_wiz=1;market_offers_wizard_premium_offers_count=3;market_offers_wizard_premium_offers_adg_count=2',
            'market_offers_adg_wizard',
            premium_offers_count=2,
        )

        # 2.4 Проверям вьютайп market_offers_wizard_right_incut
        do_test(
            request
            + ';market_enable_offers_wiz_right_incut=1;market_offers_wizard_premium_offers_count=2;market_offers_wizard_premium_offers_right_count=3',
            'market_offers_wizard',
            premium_offers_count=2,
        )
        do_test(
            request
            + ';market_enable_offers_wiz_right_incut=1;market_offers_wizard_premium_offers_count=2;market_offers_wizard_premium_offers_right_count=3',
            'market_offers_wizard_right_incut',
            premium_offers_count=3,
        )

        # 2.5 Проверям вьютайп market_offers_wizard_center_incut
        do_test(
            request
            + ';market_enable_offers_wiz_center_incut=1;market_offers_wizard_premium_offers_count=1;market_offers_wizard_premium_offers_center_count=4',
            'market_offers_wizard',
            premium_offers_count=1,
        )
        do_test(
            request
            + ';market_enable_offers_wiz_center_incut=1;market_offers_wizard_premium_offers_count=1;market_offers_wizard_premium_offers_center_count=4',
            'market_offers_wizard_center_incut',
            premium_offers_count=4,
        )

    def test_request_all_premium_offers_in_offers_wizard(self):
        """Отрыв ограничения на количество запрашиваемых премиальных офферов в
        офферном колдунщике
        https://st.yandex-team.ru/MARKETOUT-29157
        """

        def check_requested_premium_models(response, models_lst, check_presense):
            for model_id in models_lst:
                trace = 'OFFERS_PREMIUM_OFFERS Запрошен премиальный оффер для модели {0}'.format(model_id)
                if check_presense:
                    self.assertIn(trace, response.get_trace_wizard())
                else:
                    self.assertNotIn(trace, response.get_trace_wizard())

        request = 'place=parallel&text=cybertruck&rids=213&trace_wizard=1'
        request += '&rearr-factors=market_offers_incut_align=0'

        request += ';market_premium_offer_in_offers_wizard=1;market_offers_wizard_premium_offers_count=2'
        expected_premium_offers = ['cybertruck 1 offer 3', 'cybertruck 2 offer 4']

        # 1. Запрашиваем 2 премиальных оффера. Получаем 2 премиальных оффера.
        # При этом запрашиваются премиальные офферы только для 2 моделей.
        response = self.report.request_bs_pb(request)
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "showcase": {
                        "premiumOffers": [
                            {
                                "title": {"text": {"__hl": {"text": title}}},
                            }
                            for title in expected_premium_offers
                        ]
                    }
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )
        check_requested_premium_models(response, [501090, 501091], check_presense=True)
        check_requested_premium_models(response, [501092, 501093], check_presense=False)

        # 2. Запрашиваем 2 премиальных оффера. Получаем 2 премиальных оффера.
        # При этом с флагом market_offers_wizard_premium_offers_request_all=1
        # запрашиваются премиальные офферы уже для всех моделей.
        response = self.report.request_bs_pb(request + ';market_offers_wizard_premium_offers_request_all=1')
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "showcase": {
                        "premiumOffers": [
                            {
                                "title": {"text": {"__hl": {"text": title}}},
                            }
                            for title in expected_premium_offers
                        ]
                    }
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )
        check_requested_premium_models(response, [501090, 501091, 501092, 501093], check_presense=True)

    def test_premium_offers_clids_in_offers_wizard(self):
        """Проверка клидов в премиальных офферах в разных вьютайпах офферного колдунщика
        https://st.yandex-team.ru/MARKETOUT-28831
        """

        def do_test(request, wizard_key, clid):
            response = self.report.request_bs_pb(request)
            self.assertFragmentIn(
                response,
                {
                    wizard_key: {
                        "showcase": {
                            "premiumOffers": [
                                {
                                    "title": {
                                        "text": {"__hl": {"text": "cybertruck 1 offer 3", "raw": True}},
                                        "urlForCounter": Contains("clid%3D{0}".format(clid)),
                                    },
                                    "isPremium": "1",
                                }
                            ]
                        }
                    }
                },
                preserve_order=True,
                allow_different_len=False,
            )

        request = 'place=parallel&text=cybertruck&rids=213'
        request += '&rearr-factors=market_premium_offer_in_offers_wizard=1'
        request += ';market_offers_wizard_premium_offers_url_type=OfferCard'

        # 1. Деск
        do_test(request + ';market_offers_wizard_premium_offers_count=1', 'market_offers_wizard', 545)
        # 2. Тач
        do_test(
            request + ';market_offers_wizard_premium_offers_touch_count=1;offers_touch=1&touch=1',
            'market_offers_wizard',
            708,
        )
        # 3. РГ (деск и тач)
        do_test(
            request + ';market_enable_offers_adg_wiz=1;market_offers_wizard_premium_offers_adg_count=1',
            'market_offers_adg_wizard',
            913,
        )
        do_test(
            request
            + ';market_enable_offers_adg_wiz=1;market_offers_wizard_premium_offers_adg_count=1;offers_touch=1&touch=1',
            'market_offers_adg_wizard',
            919,
        )
        # 4. Правая врезка
        do_test(
            request + ';market_enable_offers_wiz_right_incut=1;market_offers_wizard_premium_offers_right_count=1',
            'market_offers_wizard_right_incut',
            830,
        )
        # 5. Центральная врезка
        do_test(
            request + ';market_enable_offers_wiz_center_incut=1;market_offers_wizard_premium_offers_center_count=1',
            'market_offers_wizard_center_incut',
            832,
        )

    def test_no_clid_910_in_premium_offers(self):
        """Проверяем что, в премиальные офферы колдунщиков не прорастает clid=910

        https://st.yandex-team.ru/MARKETOUT-28868
        """

        def do_test(
            is_touch,
            expected_model_clid,
            expected_implicit_clid,
            expected_implicit_adg_clid,
            expected_offers_clid,
            expected_offers_adg_clid,
        ):

            request = (
                'place=parallel&text=hakuna+matata'
                '&rearr-factors=market_premium_offer_in_model_wizard=1'
                ';market_premium_offer_in_implicit_model_wizard=1'
                ';market_premium_offer_in_offers_wizard=1'
                ';market_model_wizard_premium_offer_url_type=External'
                ';market_implicit_model_wizard_premium_offers_url_type=External'
                ';market_offers_wizard_premium_offers_url_type=External'
            )

            request += (
                ';market_offers_wizard_premium_offers_count=1'
                ';market_implicit_model_wizard_premium_offers_count=1'
                ';market_implicit_model_wizard_premium_offers_touch_count=1'
            )

            # форсируем показ врезки в офферном
            request += ';market_offers_incut_align=0'
            request += ';market_offers_incut_threshold_disable=1'

            if is_touch:
                request += ';market_offers_wizard_premium_offers_touch_count=1;offers_touch=1&touch=1'

            response = self.report.request_bs_pb(request)

            no_encrypted_910 = Not(Contains("clid%3D910"))
            no_910 = Not(Contains("clid=910"))

            # 1. Неявная модель
            implicit_encrypted_clid = Contains("clid%3D{0}".format(expected_implicit_clid))
            implicit_encrypted_adg_clid = Contains("clid%3D{0}".format(expected_implicit_adg_clid))
            implicit_desktop = Contains("clid=698")
            implicit_touch = Contains("clid=721")
            implicit_adg_desktop = Contains("clid=915")
            implicit_adg_touch = Contains("clid=921")

            self.assertFragmentIn(
                response,
                {
                    "market_implicit_model": {
                        "showcase": {
                            "premiumOffers": [
                                {
                                    "isPremium": "1",
                                    "thumb": {
                                        "url": implicit_desktop,
                                        "urlTouch": implicit_touch,
                                        "urlForCounter": implicit_encrypted_clid,
                                        "adGUrl": implicit_adg_desktop,
                                        "adGUrlTouch": implicit_adg_touch,
                                        "offercardUrl": implicit_encrypted_adg_clid,
                                    },
                                    "title": {
                                        "text": {"__hl": {"text": "hakuna matata", "raw": True}},
                                        "url": implicit_desktop,
                                        "urlTouch": implicit_touch,
                                        "urlForCounter": implicit_encrypted_clid,
                                        "adGUrl": implicit_adg_desktop,
                                        "adGUrlTouch": implicit_adg_touch,
                                        "offercardUrl": implicit_encrypted_adg_clid,
                                    },
                                }
                            ]
                        }
                    }
                },
            )
            self.assertFragmentIn(
                response,
                {
                    "market_implicit_model": {
                        "showcase": {
                            "premiumOffers": [
                                {
                                    "isPremium": "1",
                                    "thumb": {
                                        "url": no_910,
                                        "urlTouch": no_910,
                                        "urlForCounter": no_encrypted_910,
                                        "adGUrl": no_910,
                                        "adGUrlTouch": no_910,
                                        "offercardUrl": no_encrypted_910,
                                    },
                                    "title": {
                                        "text": {"__hl": {"text": "hakuna matata", "raw": True}},
                                        "url": no_910,
                                        "urlTouch": no_910,
                                        "urlForCounter": no_encrypted_910,
                                        "adGUrl": no_910,
                                        "adGUrlTouch": no_910,
                                        "offercardUrl": no_encrypted_910,
                                    },
                                }
                            ]
                        }
                    }
                },
            )

            # 2. Модельный
            model_clid = Contains("clid={0}".format(expected_model_clid))
            model_encrypted_clid = Contains("clid%3D{0}".format(expected_model_clid))
            if is_touch:
                model_clid = Contains("clid={0}".format(502))

            self.assertFragmentIn(
                response,
                {
                    "market_model": {
                        "showcase": {
                            "items": [
                                {
                                    "isPremium": "1",
                                    "title": {
                                        "text": {"__hl": {"raw": True, "text": "hakuna matata"}},
                                        "url": model_clid,
                                        "offercardUrl": model_encrypted_clid,
                                        "directOffercardUrl": Not(model_clid),
                                    },
                                }
                            ]
                        }
                    }
                },
            )

            self.assertFragmentIn(
                response,
                {
                    "market_model": {
                        "showcase": {
                            "items": [
                                {
                                    "isPremium": "1",
                                    "title": {
                                        "text": {"__hl": {"raw": True, "text": "hakuna matata"}},
                                        "url": no_encrypted_910,
                                        "offercardUrl": no_encrypted_910,
                                        "directOffercardUrl": no_910,
                                    },
                                }
                            ]
                        }
                    }
                },
            )

            # 3. Офферный
            offers_desktop = Contains("clid=545")
            offers_touch = Contains("clid=708")
            offers_adg_desktop = Contains("clid=913")
            offers_adg_touch = Contains("clid=919")
            offers_encrypted_clid = Contains("clid%3D{0}".format(expected_offers_clid))
            offers_encrypted_adg_clid = Contains("clid%3D{0}".format(expected_offers_adg_clid))

            self.assertFragmentIn(
                response,
                {
                    "market_offers_wizard": {
                        "showcase": {
                            "premiumOffers": [
                                {
                                    "isPremium": "1",
                                    "thumb": {
                                        "urlForCounter": offers_encrypted_clid,
                                        "offercardUrl": offers_encrypted_adg_clid,
                                        "url": offers_desktop,
                                        "urlTouch": offers_touch,
                                        "adGUrl": offers_adg_desktop,
                                        "adGUrlTouch": offers_adg_touch,
                                    },
                                    "title": {
                                        "text": {"__hl": {"text": "hakuna matata", "raw": True}},
                                        "urlForCounter": offers_encrypted_clid,
                                        "offercardUrl": offers_encrypted_adg_clid,
                                        "url": offers_desktop,
                                        "urlTouch": offers_touch,
                                        "adGUrl": offers_adg_desktop,
                                        "adGUrlTouch": offers_adg_touch,
                                    },
                                }
                            ]
                        }
                    }
                },
            )
            self.assertFragmentIn(
                response,
                {
                    "market_offers_wizard": {
                        "showcase": {
                            "premiumOffers": [
                                {
                                    "isPremium": "1",
                                    "thumb": {
                                        "url": no_910,
                                        "urlTouch": no_910,
                                        "urlForCounter": no_encrypted_910,
                                        "adGUrl": no_910,
                                        "adGUrlTouch": no_910,
                                        "offercardUrl": no_encrypted_910,
                                    },
                                    "title": {
                                        "text": {"__hl": {"text": "hakuna matata", "raw": True}},
                                        "url": no_910,
                                        "urlTouch": no_910,
                                        "urlForCounter": no_encrypted_910,
                                        "adGUrl": no_910,
                                        "adGUrlTouch": no_910,
                                        "offercardUrl": no_encrypted_910,
                                    },
                                }
                            ]
                        }
                    }
                },
            )

        do_test(
            is_touch=False,
            expected_model_clid=502,
            expected_implicit_clid=698,
            expected_implicit_adg_clid=915,
            expected_offers_clid=545,
            expected_offers_adg_clid=913,
        )
        do_test(
            is_touch=True,
            expected_model_clid=704,
            expected_implicit_clid=721,
            expected_implicit_adg_clid=921,
            expected_offers_clid=708,
            expected_offers_adg_clid=919,
        )

    def test_premium_offer_offercard_direct_url(self):
        """Проверяем подмену url / UrlTouch на КО при генерации ссылки премиального оффера на КО

        https://st.yandex-team.ru/MARKETOUT-29002
        """

        def make_request(url_type):
            request = (
                'place=parallel&text=hakuna+matata'
                '&rearr-factors=market_model_wizard_premium_offer_url_type={0}'
                ';market_implicit_model_wizard_premium_offers_url_type={0}'
                ';market_offers_wizard_premium_offers_url_type={0}'
                ';market_offers_wizard_incut_url_type={0}'.format(url_type)
            )

            request += (
                ';market_offers_wizard_premium_offers_count=1' ';market_implicit_model_wizard_premium_offers_count=1'
            )

            # форсируем показ врезки в офферном
            request += ';market_offers_incut_align=0'
            request += ';market_offers_incut_threshold_disable=1'

            return request

        turn_on_premium = (
            ';market_premium_offer_in_model_wizard=1'
            ';market_premium_offer_in_implicit_model_wizard=1'
            ';market_premium_offer_in_offers_wizard=1'
        )

        offercard = Contains("//market.yandex.ru/offer")

        # 1. Usual offers, OfferCard urls
        response = self.report.request_bs_pb(make_request(url_type='OfferCard'))
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "showcase": {
                        "items": [
                            {
                                "title": {
                                    "text": {"__hl": {"text": "hakuna matata", "raw": True}},
                                    "url": offercard,
                                    "urlTouch": offercard,
                                    "adGUrl": offercard,
                                    "adGUrlTouch": offercard,
                                }
                            }
                        ]
                    }
                }
            },
        )

        self.assertFragmentIn(
            response,
            {
                "market_model": {
                    "showcase": {
                        "items": [
                            {
                                "title": {
                                    "text": {"__hl": {"raw": True, "text": "hakuna matata"}},
                                    "url": Not(offercard),
                                    "directOffercardUrl": offercard,
                                }
                            }
                        ]
                    }
                }
            },
        )

        # 2. Usual offers, External urls
        response = self.report.request_bs_pb(make_request(url_type='External'))
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "showcase": {
                        "items": [
                            {
                                "title": {
                                    "text": {"__hl": {"text": "hakuna matata", "raw": True}},
                                    "url": Not(offercard),
                                    "urlTouch": Not(offercard),
                                    # maybe will fix it https://st.yandex-team.ru/MARKETOUT-29002#5de966ddf17a104457bb4089
                                    "adGUrl": Not(offercard),
                                    "adGUrlTouch": Not(offercard),
                                }
                            }
                        ]
                    }
                }
            },
        )

        self.assertFragmentIn(
            response,
            {
                "market_model": {
                    "showcase": {
                        "items": [
                            {
                                "title": {
                                    "text": {"__hl": {"raw": True, "text": "hakuna matata"}},
                                    "url": Not(offercard),
                                    "directOffercardUrl": offercard,
                                }
                            }
                        ]
                    }
                }
            },
        )

        # 3. Premium offers, OfferCard urls
        response = self.report.request_bs_pb(make_request(url_type='OfferCard') + turn_on_premium)
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "showcase": {
                        "premiumOffers": [
                            {
                                "title": {
                                    "text": {"__hl": {"text": "hakuna matata", "raw": True}},
                                    "url": offercard,
                                    "urlTouch": offercard,
                                    "adGUrl": offercard,
                                    "adGUrlTouch": offercard,
                                }
                            }
                        ]
                    }
                }
            },
        )
        self.assertFragmentIn(
            response,
            {
                "market_implicit_model": {
                    "showcase": {
                        "premiumOffers": [
                            {
                                "title": {
                                    "text": {"__hl": {"text": "hakuna matata", "raw": True}},
                                    "url": offercard,
                                    "urlTouch": offercard,
                                    "adGUrl": offercard,
                                    "adGUrlTouch": offercard,
                                }
                            }
                        ]
                    }
                }
            },
        )

        self.assertFragmentIn(
            response,
            {
                "market_model": {
                    "showcase": {
                        "items": [
                            {
                                "title": {
                                    "text": {"__hl": {"raw": True, "text": "hakuna matata"}},
                                    "url": Not(offercard),
                                    "directOffercardUrl": offercard,
                                }
                            }
                        ]
                    }
                }
            },
        )

        # 4. Premium offers, External urls
        response = self.report.request_bs_pb(make_request(url_type='External') + turn_on_premium)
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "showcase": {
                        "premiumOffers": [
                            {
                                "title": {
                                    "text": {"__hl": {"text": "hakuna matata", "raw": True}},
                                    "url": Not(offercard),
                                    "urlTouch": Not(offercard),
                                    # maybe will fix it https://st.yandex-team.ru/MARKETOUT-29002#5de966ddf17a104457bb4089
                                    "adGUrl": Not(offercard),
                                    "adGUrlTouch": Not(offercard),
                                }
                            }
                        ]
                    }
                }
            },
        )
        self.assertFragmentIn(
            response,
            {
                "market_implicit_model": {
                    "showcase": {
                        "premiumOffers": [
                            {
                                "title": {
                                    "text": {"__hl": {"text": "hakuna matata", "raw": True}},
                                    "url": Not(offercard),
                                    "urlTouch": Not(offercard),
                                    # maybe will fix it https://st.yandex-team.ru/MARKETOUT-29002#5de966ddf17a104457bb4089
                                    "adGUrl": Not(offercard),
                                    "adGUrlTouch": Not(offercard),
                                }
                            }
                        ]
                    }
                }
            },
        )

        self.assertFragmentIn(
            response,
            {
                "market_model": {
                    "showcase": {
                        "items": [
                            {
                                "title": {
                                    "text": {"__hl": {"raw": True, "text": "hakuna matata"}},
                                    "url": Not(offercard),
                                    "directOffercardUrl": offercard,
                                }
                            }
                        ]
                    }
                }
            },
        )

        # 5. Universal model wizard
        for request in [make_request(url_type='OfferCard'), make_request(url_type='External')]:
            for extra_flag, expect_premium in [('', NoKey("is_premium")), (turn_on_premium, "1")]:
                response = self.report.request_bs_pb(request + extra_flag + ';showcase_universal_model=1')
                self.assertFragmentIn(
                    response,
                    {
                        "market_model": {
                            "showcase": {
                                "items": [
                                    {
                                        "isPremium": expect_premium,
                                        "title": {
                                            "text": {"__hl": {"text": "hakuna matata", "raw": True}},
                                            # https://st.yandex-team.ru/MARKETOUT-29002#5defc1ec713bc74bc04c2fef
                                            "url": Not(offercard),
                                            "urlTouch": Not(offercard),
                                            "adGUrl": Not(offercard),
                                            "adGUrlTouch": Not(offercard),
                                            # https://st.yandex-team.ru/MARKETOUT-27421
                                            "directOffercardUrl": offercard,
                                        },
                                    }
                                ]
                            }
                        }
                    },
                )

    @classmethod
    def prepare_request_text_in_premium_offercard_url(cls):
        cls.index.models += [
            Model(hyperid=291610, title="christmas"),
        ]
        cls.index.shops += [
            Shop(fesh=291611, priority_region=213, regions=[225], cpa=Shop.CPA_REAL),
            Shop(fesh=291612, priority_region=213, regions=[225], cpa=Shop.CPA_REAL),
        ]

        cls.index.offers += [
            Offer(title='christmas', hyperid=291610, fesh=291611, bid=90),
            Offer(title='samtsirhc', hyperid=291610, fesh=291612, bid=100),
        ]

    def test_request_text_in_premium_offercard_url(self):
        """Проверяем, что в КО премиального оффера пробрасывается запрос
        https://st.yandex-team.ru/MARKETOUT-29161
        """

        request = (
            'place=parallel&text=christmas'
            '&rearr-factors=market_premium_offer_in_offers_wizard=1'
            ';market_offers_wizard_premium_offers_url_type=OfferCard'
            ';market_offers_wizard_premium_offers_count=1'
            ';device=desktop'
        )

        # форсируем показ врезки в офферном
        request += ';market_offers_incut_align=0;market_offers_incut_threshold_disable=1'

        response = self.report.request_bs_pb(request)
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "showcase": {
                        "premiumOffers": [
                            {
                                "title": {
                                    "text": {"__hl": {"text": "samtsirhc", "raw": True}},
                                    "url": Contains("&text=christmas"),
                                }
                            }
                        ]
                    }
                }
            },
        )

    @classmethod
    def prepare_offers_wizard_moving_from_blue_to_premium(cls):
        cls.index.mskus += [
            MarketSku(
                sku=29176100,
                title="warner chappell 1 blue",
                hyperid=291760,
                blue_offers=[BlueOffer(price=2917671, ts=2917610, bid=245)],
            ),
            MarketSku(
                sku=29176200,
                title="warner chappell 2 blue",
                hyperid=291761,
                blue_offers=[BlueOffer(price=2917671, ts=2917620)],
            ),
            MarketSku(
                sku=29176300,
                title="warner chappell 3 blue",
                hyperid=291761,
                blue_offers=[BlueOffer(price=2917671, ts=2917630)],
            ),
        ]
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 2917610).respond(0.6)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 2917620).respond(0.5)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 2917630).respond(0.4)

        cls.index.shops += [
            Shop(fesh=291761, priority_region=213, regions=[225], cpa=Shop.CPA_REAL),
            Shop(fesh=291762, priority_region=213, regions=[225], cpa=Shop.CPA_REAL),
            Shop(fesh=291763, priority_region=213, regions=[225], cpa=Shop.CPA_REAL),
            Shop(fesh=291764, priority_region=213, regions=[225], cpa=Shop.CPA_REAL),
            Shop(fesh=291765, priority_region=213, regions=[225], cpa=Shop.CPA_REAL),
            Shop(fesh=291766, priority_region=213, regions=[225], cpa=Shop.CPA_REAL),
            Shop(fesh=291767, priority_region=213, regions=[225], cpa=Shop.CPA_REAL),
        ]
        cls.index.offers += [
            Offer(title='warner chappell 1 offer 1', hyperid=291760, fesh=291761, bid=90, ts=291761),
            Offer(title='warner chappell 1 offer 3', hyperid=291760, fesh=291762, bid=100, ts=291762),
            Offer(title='warner chappell 2 offer 4', hyperid=291761, fesh=291763, bid=70, ts=291763),
            Offer(title='warner chappell 1 offer 5', hyperid=291760, fesh=291764, bid=60, ts=291764),
            Offer(title='warner chappell 3 offer 6', hyperid=291762, fesh=291765, bid=50, ts=291765),
            Offer(title='warner chappell 3 offer 7', hyperid=291762, fesh=291766, bid=40, ts=291766),
            Offer(title='warner chappell 4 offer 8', hyperid=291763, fesh=291767, bid=30, ts=291767),
        ]

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 291761).respond(0.970)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 291762).respond(0.969)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 291763).respond(0.968)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 291764).respond(0.967)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 291765).respond(0.966)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 291766).respond(0.965)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 291767).respond(0.964)

    def test_offers_wizard_moving_from_blue_to_premium(self):
        """Фиксируем перетягивание офферов из синей врезки в премиальную
        Пока не чиним https://st.yandex-team.ru/MARKETOUT-29176#5e1dca496b3c261ed100d1e7

        https://st.yandex-team.ru/MARKETOUT-29176
        """

        request = 'place=parallel&text=warner+chappell'
        request += '&rearr-factors=use_offer_type_priority_as_main_factor_in_top=0;market_offers_incut_align=5;market_ranging_cpa_by_ue_in_top=0;prefer_do_with_sku=0'  # для проверок по выравниванию

        turn_on_premium = ';market_premium_offer_in_offers_wizard=1;market_offers_wizard_premium_offers_count=2'

        # 1. Без доп. флагов получаем обычную выровненную выдачу.
        response = self.report.request_bs_pb(request)
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "showcase": {
                        "items": [
                            {
                                "title": {"text": {"__hl": {"text": "warner chappell 1 offer 1", "raw": True}}},
                            },
                            {
                                "title": {"text": {"__hl": {"text": "warner chappell 1 offer 3", "raw": True}}},
                            },
                            {
                                "title": {"text": {"__hl": {"text": "warner chappell 2 offer 4", "raw": True}}},
                            },
                            {
                                "title": {"text": {"__hl": {"text": "warner chappell 1 offer 5", "raw": True}}},
                            },
                            {
                                "title": {"text": {"__hl": {"text": "warner chappell 3 offer 6", "raw": True}}},
                            },
                        ]
                    }
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

        # 2. Запрашиваем 2 премиальных оффера.
        # Один из премиальных офферов уже был во врезке, ему на смену в обычную врезку приходит другой оффер.
        response = self.report.request_bs_pb(request + turn_on_premium)
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "showcase": {
                        "premiumOffers": [
                            {
                                "title": {
                                    "text": {"__hl": {"text": "warner chappell 1 blue", "raw": True}},
                                },
                            },
                            {
                                "title": {"text": {"__hl": {"text": "warner chappell 2 offer 4", "raw": True}}},
                            },
                        ]
                    }
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "showcase": {
                        "items": [
                            {
                                "title": {"text": {"__hl": {"text": "warner chappell 1 offer 1", "raw": True}}},
                            },
                            {
                                "title": {"text": {"__hl": {"text": "warner chappell 1 offer 3", "raw": True}}},
                            },
                            # Здесь был оффер "warner chappell 2 offer 4", но он стал премиальным
                            {
                                "title": {"text": {"__hl": {"text": "warner chappell 1 offer 5", "raw": True}}},
                            },
                            {
                                "title": {"text": {"__hl": {"text": "warner chappell 3 offer 6", "raw": True}}},
                            },
                            # Этот оффер докинулся вместо уехавшего в премиальные "warner chappell 2 offer 4"
                            {
                                "title": {"text": {"__hl": {"text": "warner chappell 3 offer 7", "raw": True}}},
                            },
                        ]
                    }
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

        # 3. Добавление синей врезки.
        # Запрашиваем синюю врезку из двух офферов. При этом при формировании синей врезки отключается выравнивание.
        response = self.report.request_bs_pb(request + ';market_offers_wizard_blue_offers_count=2')
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "showcase": {
                        "items": [
                            {
                                "title": {"text": {"__hl": {"text": "warner chappell 1 offer 1", "raw": True}}},
                            },
                            {
                                "title": {"text": {"__hl": {"text": "warner chappell 1 offer 3", "raw": True}}},
                            },
                            {
                                "title": {"text": {"__hl": {"text": "warner chappell 2 offer 4", "raw": True}}},
                            },
                            {
                                "title": {"text": {"__hl": {"text": "warner chappell 1 offer 5", "raw": True}}},
                            },
                            {
                                "title": {"text": {"__hl": {"text": "warner chappell 3 offer 6", "raw": True}}},
                            },
                            # Результат несрабатывающего выравнивания
                            {
                                "title": {"text": {"__hl": {"text": "warner chappell 3 offer 7", "raw": True}}},
                            },
                            {
                                "title": {"text": {"__hl": {"text": "warner chappell 4 offer 8", "raw": True}}},
                            },
                            # Синяя врезка
                            {
                                "title": {"text": {"__hl": {"text": "warner chappell 1 blue", "raw": True}}},
                            },
                            {
                                "title": {"text": {"__hl": {"text": "warner chappell 2 blue", "raw": True}}},
                            },
                        ]
                    }
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

        # 4. Перетягивание синего оффера в премиальный.
        response = self.report.request_bs_pb(request + ';market_offers_wizard_blue_offers_count=2' + turn_on_premium)
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "showcase": {
                        "premiumOffers": [
                            {
                                "title": {
                                    "text": {"__hl": {"text": "warner chappell 1 blue", "raw": True}},
                                },
                            },
                            {
                                "title": {"text": {"__hl": {"text": "warner chappell 2 offer 4", "raw": True}}},
                            },
                        ]
                    }
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "showcase": {
                        "items": [
                            {
                                "title": {"text": {"__hl": {"text": "warner chappell 1 offer 1", "raw": True}}},
                            },
                            {
                                "title": {"text": {"__hl": {"text": "warner chappell 1 offer 3", "raw": True}}},
                            },
                            # Здесь был оффер "warner chappell 2 offer 4", но он стал премиальным
                            {
                                "title": {"text": {"__hl": {"text": "warner chappell 1 offer 5", "raw": True}}},
                            },
                            {
                                "title": {"text": {"__hl": {"text": "warner chappell 3 offer 6", "raw": True}}},
                            },
                            {
                                "title": {"text": {"__hl": {"text": "warner chappell 3 offer 7", "raw": True}}},
                            },
                            # Результат несрабатывающего выравнивания
                            {
                                "title": {"text": {"__hl": {"text": "warner chappell 4 offer 8", "raw": True}}},
                            },
                            # Синяя врезка теперь состоит из 1 оффера, хотя запрашивали 2
                            # Здесь был оффер "warner chappell 1 blue", но он стал премиальным
                            {
                                "title": {"text": {"__hl": {"text": "warner chappell 2 blue", "raw": True}}},
                            },
                        ]
                    }
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

    @classmethod
    def prepare_filter_premium_offers_without_picture(cls):
        cls.index.models += [
            Model(hyperid=296231, title="coronavirus 1", ts=296231),
            Model(hyperid=296232, title="coronavirus 2", ts=296232),
        ]
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 296231).respond(0.9)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 296232).respond(0.8)

        cls.index.offers += [
            Offer(
                title='coronavirus 1 offer 1', hyperid=296231, fesh=1, bid=100, no_picture=True
            ),  # премиальный оффер без картинки
            Offer(title='coronavirus 1 offer 2', hyperid=296231, fesh=2, bid=90),
            Offer(title='coronavirus 2 offer 1', hyperid=296232, fesh=1, bid=70),
            Offer(title='coronavirus 2 offer 2', hyperid=296232, fesh=2, bid=80),
        ]

    def test_filter_premium_offers_without_picture(self):
        """Проверяем фильтрацию премиальных офферов без картинок
        https://st.yandex-team.ru/MARKETOUT-29623
        """

        response = self.report.request_bs_pb(
            'place=parallel&text=coronavirus&rids=213'
            '&rearr-factors=market_premium_offer_in_implicit_model_wizard=1;'
            'market_implicit_model_wizard_premium_offers_count=2&trace_wizard=1'
        )
        self.assertFragmentIn(
            response,
            {
                "market_implicit_model": {
                    "showcase": {
                        "premiumOffers": [
                            {
                                "title": {
                                    "text": {"__hl": {"text": "coronavirus 2 offer 2", "raw": True}},
                                },
                                "isPremium": "1",
                            },
                        ]
                    }
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

        self.assertIn(
            "IMPLICIT_PREMIUM_OFFERS Премиальный оффер для модели 296231 получен, но отфильтрован",
            response.get_trace_wizard(),
        )

    @classmethod
    def prepare_do_price_in_implicit_model_wizard(cls):
        cls.index.models += [
            Model(hyperid=297951, title="doppelganger 1", ts=297951),
            Model(hyperid=297952, title="doppelganger 2", ts=297952),
            Model(hyperid=297953, title="doppelganger 3", ts=297953),
            Model(hyperid=297954, title="doppelganger 4", ts=297954),
            Model(hyperid=297955, title="doppelganger 5", ts=297955),
            Model(hyperid=297956, title="doppelganger 6", ts=297956),
            Model(hyperid=297957, title="doppelganger 7", ts=297957),
        ]
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 297951).respond(0.9)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 297952).respond(0.8)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 297953).respond(0.7)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 297954).respond(0.6)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 297955).respond(0.5)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 297956).respond(0.4)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 297957).respond(0.3)

        cls.index.offers += [
            Offer(title='doppelganger 1 offer 1', hyperid=297951, fesh=1),
            Offer(title='doppelganger 1 offer 2', hyperid=297951, fesh=2),
            Offer(title='doppelganger 2 offer 1', hyperid=297952, fesh=1),
            Offer(title='doppelganger 2 offer 2', hyperid=297952, fesh=2),
            Offer(title='doppelganger 3 offer 1', hyperid=297953, fesh=1),
            Offer(title='doppelganger 3 offer 2', hyperid=297953, fesh=2),
            Offer(title='doppelganger 4 offer 1', hyperid=297954, fesh=1),
            Offer(title='doppelganger 4 offer 2', hyperid=297954, fesh=2),
            Offer(title='doppelganger 5 offer 1', hyperid=297955, fesh=1),
            Offer(title='doppelganger 5 offer 2', hyperid=297955, fesh=2),
            Offer(title='doppelganger 6 offer 1', hyperid=297956, fesh=1),
            Offer(title='doppelganger 6 offer 2', hyperid=297956, fesh=2),
            Offer(title='doppelganger 7 offer 1', hyperid=297957, fesh=1),
            Offer(title='doppelganger 7 offer 2', hyperid=297957, fesh=2),
        ]

    def test_do_price_in_implicit_model_wizard(self):
        """Показываем цену дефолтного оффера в колдунщике неявной модели
        https://st.yandex-team.ru/MARKETOUT-29795
        """

        # NB: не завязываюсь на конкретные ДО-офферы, т.к. не хочу здесь углубляться
        # в логику ДО и проверка на индексе лайта не даст здесь гарантии

        # 1. Сейчас в колдунщике неявной модели показывается "цена от",
        # она имеет тип "min" и хранится в поле "priceMin"
        response = self.report.request_bs_pb('place=parallel&text=doppelganger&rids=213')
        self.assertFragmentIn(
            response,
            {
                "market_implicit_model": {
                    "showcase": {
                        "items": [
                            {
                                "title": {
                                    "text": {"__hl": {"text": "doppelganger 1", "raw": True}},
                                },
                                "price": {"priceMin": NotEmpty(), "currency": NotEmpty(), "type": "min"},
                            },
                            {
                                "title": {
                                    "text": {"__hl": {"text": "doppelganger 2", "raw": True}},
                                },
                                "price": {"type": "min"},
                            },
                            {
                                "title": {
                                    "text": {"__hl": {"text": "doppelganger 3", "raw": True}},
                                },
                                "price": {"type": "min"},
                            },
                            {
                                "title": {
                                    "text": {"__hl": {"text": "doppelganger 4", "raw": True}},
                                },
                                "price": {"type": "min"},
                            },
                            {
                                "title": {
                                    "text": {"__hl": {"text": "doppelganger 5", "raw": True}},
                                },
                                "price": {"type": "min"},
                            },
                            {
                                "title": {
                                    "text": {"__hl": {"text": "doppelganger 6", "raw": True}},
                                },
                                "price": {"type": "min"},
                            },
                        ],
                        "extra_models": [
                            {
                                "title": {
                                    "text": {"__hl": {"text": "doppelganger 7", "raw": True}},
                                },
                                "price": {"type": "min"},
                            },
                        ],
                    }
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

        # 2. Под флагом market_do_price_in_implicit_model_wizard=1 после сбора моделей для всех моделей
        # из блока showcase->items запрашивается ДО. На модели из блока showcase->extra_models это не распространяется.
        # При этом тип цены меняется на "average" и цена приезжает в поле "priceMax"
        response = self.report.request_bs_pb(
            'place=parallel&text=doppelganger&rids=213&rearr-factors=market_do_price_in_implicit_model_wizard=1'
        )
        self.assertFragmentIn(
            response,
            {
                "market_implicit_model": {
                    "showcase": {
                        "items": [
                            {
                                "title": {
                                    "text": {"__hl": {"text": "doppelganger 1", "raw": True}},
                                },
                                "price": {"priceMax": NotEmpty(), "currency": NotEmpty(), "type": "average"},
                            },
                            {
                                "title": {
                                    "text": {"__hl": {"text": "doppelganger 2", "raw": True}},
                                },
                                "price": {"type": "average"},
                            },
                            {
                                "title": {
                                    "text": {"__hl": {"text": "doppelganger 3", "raw": True}},
                                },
                                "price": {"type": "average"},
                            },
                            {
                                "title": {
                                    "text": {"__hl": {"text": "doppelganger 4", "raw": True}},
                                },
                                "price": {"type": "average"},
                            },
                            {
                                "title": {
                                    "text": {"__hl": {"text": "doppelganger 5", "raw": True}},
                                },
                                "price": {"type": "average"},
                            },
                            {
                                "title": {
                                    "text": {"__hl": {"text": "doppelganger 6", "raw": True}},
                                },
                                "price": {"type": "average"},
                            },
                        ],
                        "extra_models": [
                            {
                                "title": {
                                    "text": {"__hl": {"text": "doppelganger 7", "raw": True}},
                                },
                                "price": {"type": "min"},
                            },
                        ],
                    }
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

        # 3. При указании флага market_do_price_in_implicit_model_wizard_count=N
        # на этапе подготовки данных запрашиваются ДО у N моделей.
        response = self.report.request_bs_pb(
            'place=parallel&text=doppelganger&rids=213&rearr-factors=market_do_price_in_implicit_model_wizard_count=3'
        )
        self.assertFragmentIn(
            response,
            {
                "market_implicit_model": {
                    "showcase": {
                        "items": [
                            {
                                "title": {
                                    "text": {"__hl": {"text": "doppelganger 1", "raw": True}},
                                },
                                "price": {"type": "average"},
                            },
                            {
                                "title": {
                                    "text": {"__hl": {"text": "doppelganger 2", "raw": True}},
                                },
                                "price": {"type": "average"},
                            },
                            {
                                "title": {
                                    "text": {"__hl": {"text": "doppelganger 3", "raw": True}},
                                },
                                "price": {"type": "average"},
                            },
                            {
                                "title": {
                                    "text": {"__hl": {"text": "doppelganger 4", "raw": True}},
                                },
                                "price": {"type": "min"},
                            },
                            {
                                "title": {
                                    "text": {"__hl": {"text": "doppelganger 5", "raw": True}},
                                },
                                "price": {"type": "min"},
                            },
                            {
                                "title": {
                                    "text": {"__hl": {"text": "doppelganger 6", "raw": True}},
                                },
                                "price": {"type": "min"},
                            },
                        ],
                        "extra_models": [
                            {
                                "title": {
                                    "text": {"__hl": {"text": "doppelganger 7", "raw": True}},
                                },
                                "price": {"type": "min"},
                            },
                        ],
                    }
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

        # 3.1 При этом если модель в результате попадет в блок showcase->extra_models,
        # её цена не заменится на цену ДО.
        response = self.report.request_bs_pb(
            'place=parallel&text=doppelganger&rids=213&rearr-factors=market_do_price_in_implicit_model_wizard_count=7&trace_wizard=1'
        )
        self.assertFragmentIn(
            response,
            {
                "market_implicit_model": {
                    "showcase": {
                        "items": [
                            {
                                "title": {
                                    "text": {"__hl": {"text": "doppelganger 1", "raw": True}},
                                },
                                "price": {"type": "average"},
                            },
                            {
                                "title": {
                                    "text": {"__hl": {"text": "doppelganger 2", "raw": True}},
                                },
                                "price": {"type": "average"},
                            },
                            {
                                "title": {
                                    "text": {"__hl": {"text": "doppelganger 3", "raw": True}},
                                },
                                "price": {"type": "average"},
                            },
                            {
                                "title": {
                                    "text": {"__hl": {"text": "doppelganger 4", "raw": True}},
                                },
                                "price": {"type": "average"},
                            },
                            {
                                "title": {
                                    "text": {"__hl": {"text": "doppelganger 5", "raw": True}},
                                },
                                "price": {"type": "average"},
                            },
                            {
                                "title": {
                                    "text": {"__hl": {"text": "doppelganger 6", "raw": True}},
                                },
                                "price": {"type": "average"},
                            },
                        ],
                        "extra_models": [
                            {
                                "title": {
                                    "text": {"__hl": {"text": "doppelganger 7", "raw": True}},
                                },
                                "price": {"type": "min"},
                            },
                        ],
                    }
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

        # По трассировке видно, что цена ДО модели 297955 получена (но не используется в блоке extra_models)
        self.assertIn("DEFAULT_OFFERS Получен дефолтный оффер для модели 297955", response.get_trace_wizard())

        # 4 Проверка адекватной работы при включении обоих флагов
        response = self.report.request_bs_pb(
            'place=parallel&text=doppelganger&rids=213&rearr-factors=market_do_price_in_implicit_model_wizard=1;market_do_price_in_implicit_model_wizard_count=3'
        )
        self.assertFragmentIn(
            response,
            {
                "market_implicit_model": {
                    "showcase": {
                        "items": [
                            {
                                "title": {
                                    "text": {"__hl": {"text": "doppelganger 1", "raw": True}},
                                },
                                "price": {"priceMax": NotEmpty(), "currency": NotEmpty(), "type": "average"},
                            },
                            {
                                "title": {
                                    "text": {"__hl": {"text": "doppelganger 2", "raw": True}},
                                },
                                "price": {"type": "average"},
                            },
                            {
                                "title": {
                                    "text": {"__hl": {"text": "doppelganger 3", "raw": True}},
                                },
                                "price": {"type": "average"},
                            },
                            {
                                "title": {
                                    "text": {"__hl": {"text": "doppelganger 4", "raw": True}},
                                },
                                "price": {"type": "average"},
                            },
                            {
                                "title": {
                                    "text": {"__hl": {"text": "doppelganger 5", "raw": True}},
                                },
                                "price": {"type": "average"},
                            },
                            {
                                "title": {
                                    "text": {"__hl": {"text": "doppelganger 6", "raw": True}},
                                },
                                "price": {"type": "average"},
                            },
                        ],
                        "extra_models": [
                            {
                                "title": {
                                    "text": {"__hl": {"text": "doppelganger 7", "raw": True}},
                                },
                                "price": {"type": "min"},
                            },
                        ],
                    }
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

    def test_light_premium_offers_in_implicit_model_wizard(self):
        """Проверяем премиальные офферы в колдунщике неявной модели на легкой ручке
        https://st.yandex-team.ru/MARKETOUT-28188
        """
        response = self.report.request_bs_pb(
            'place=parallel&text=implicit&rids=213'
            '&rearr-factors=market_premium_offer_in_implicit_model_wizard=1;'
            'market_implicit_model_wizard_premium_offers_count=4;'
            'market_implicit_wizard_light_premium=1'
        )
        self.assertFragmentIn(
            response,
            {
                "market_implicit_model": {
                    "showcase": {
                        "premiumOffers": [
                            {
                                "thumb": {
                                    "source": Contains(
                                        "//avatars.mdst.yandex.net/get-marketpic/101/market_iyC3nHslqLtqZJLygVAHeA/100x100"
                                    ),
                                    "retinaSource": Contains(
                                        "//avatars.mdst.yandex.net/get-marketpic/101/market_iyC3nHslqLtqZJLygVAHeA/200x200"
                                    ),
                                    "height": "100",
                                    "width": "100",
                                    "url": Contains("http://www.shop-1.ru/"),
                                    "urlTouch": Contains("http://www.shop-1.ru/"),
                                    "urlForCounter": Contains("//market-click2.yandex.ru/redir/dtype=market/"),
                                    "offercardUrl": Contains("//market-click2.yandex.ru/redir/dtype=market/"),
                                },
                                "price": {"type": "average", "priceMax": "100", "currency": "RUR"},
                                "title": {
                                    "text": {"__hl": {"text": "implicit 1 offer 1", "raw": True}},
                                    "url": Contains("http://www.shop-1.ru/"),
                                    "urlTouch": Contains("http://www.shop-1.ru/"),
                                    "urlForCounter": Contains("//market-click2.yandex.ru/redir/dtype=market/"),
                                    "offercardUrl": Contains("//market-click2.yandex.ru/redir/dtype=market/"),
                                },
                                "greenUrl": {
                                    "text": "SHOP-1",
                                    "url": LikeUrl.of("//market.yandex.ru/shop--shop-1/1/reviews?clid=698"),
                                    "urlTouch": LikeUrl.of("//m.market.yandex.ru/grades-shop.xml?shop_id=1&clid=721"),
                                },
                                # в отличие от тяжелой ручки информации о доставке нет
                                "delivery": {},
                                "isPremium": "1",
                            },
                            {
                                "title": {
                                    "text": {"__hl": {"text": "implicit 2 offer 2", "raw": True}},
                                },
                                # в отличие от тяжелой ручки информации о доставке нет
                                "delivery": {},
                                "isPremium": "1",
                            },
                            {
                                "title": {
                                    "text": {"__hl": {"text": "implicit 3 offer 1", "raw": True}},
                                },
                                "isPremium": "1",
                            },
                            {
                                "title": {
                                    "text": {"__hl": {"text": "implicit 4 offer 1", "raw": True}},
                                },
                                "isPremium": "1",
                            },
                        ]
                    }
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

    def test_model_premium_in_offers_wizard(self):
        """Добавляем премиальный оффер из модельного колдунщика в офферный колдунщик
        под флагом market_premium_offer_in_model_wizard={3,4}.

        https://st.yandex-team.ru/MARKETOUT-30321
        """
        # Проверяем выдачу по всем возможным значениям флага market_premium_offer_in_model_wizard
        for flag_value, offers_wizard_has_premium in [(0, False), (1, False), (2, False), (3, True), (4, True)]:
            request = 'place=parallel&text=lenovo p780&rids=213'
            request += '&rearr-factors=market_premium_offer_in_model_wizard={0}'.format(flag_value)
            request += ';market_model_wizard_premium_offer_url_type=External'
            request += ';market_offers_wizard_premium_offers_url_type=OfferCard'

            # Проверяем все вьютайпы
            for extra_flags, viewtype in [
                (';market_offers_wizard_premium_offers_count=1', 'market_offers_wizard'),
                (';market_offers_wizard_premium_offers_touch_count=1;offers_touch=1&touch=1', 'market_offers_wizard'),
                (
                    ';market_enable_offers_adg_wiz=1;market_offers_wizard_premium_offers_adg_count=1',
                    'market_offers_adg_wizard',
                ),
                (
                    ';market_enable_offers_wiz_right_incut=1;market_offers_wizard_premium_offers_right_count=1',
                    'market_offers_wizard_right_incut',
                ),
                (
                    ';market_enable_offers_wiz_center_incut=1;market_offers_wizard_premium_offers_center_count=1',
                    'market_offers_wizard_center_incut',
                ),
            ]:
                response = self.report.request_bs_pb(request + extra_flags)
                if flag_value > 0:
                    # Проверяем формирование премиального оффера в модельном колдунщике
                    model_with_premium = {
                        "market_model": {
                            "showcase": {
                                "items": [
                                    {
                                        "title": {
                                            "text": {"__hl": {"raw": True, "text": "lenovo p780 cpa offer 3"}},
                                            "urlForCounter": Contains("//market-click2.yandex.ru/redir/dtype=market"),
                                        },
                                        "isPremium": "1",
                                    }
                                ]
                            }
                        }
                    }
                    self.assertFragmentIn(response, model_with_premium)

                offers_with_premium = {
                    viewtype: {
                        "showcase": {
                            "premiumOffers": [
                                {
                                    "title": {
                                        "text": {"__hl": {"text": "lenovo p780 cpa offer 3", "raw": True}},
                                        "urlForCounter": Contains("//market-click2.yandex.ru/redir/dtype=offercard"),
                                    },
                                    "isPremium": "1",
                                }
                            ]
                        }
                    }
                }
                if offers_wizard_has_premium:
                    self.assertFragmentIn(response, offers_with_premium)
                else:
                    self.assertFragmentNotIn(response, offers_with_premium)

    def test_model_premium_in_implicit_wizard(self):
        """Добавляем премиальный оффер из модельного колдунщика в колдунщик неявной модели
        под флагом market_premium_offer_in_model_wizard={2,4}.

        https://st.yandex-team.ru/MARKETOUT-30321
        """
        # Проверяем выдачу по всем возможным значениям флага market_premium_offer_in_model_wizard
        for flag_value, implicit_wizard_has_premium in [(0, False), (1, False), (2, True), (3, False), (4, True)]:
            request = 'place=parallel&text=implicit&rids=213'
            request += '&rearr-factors=market_premium_offer_in_model_wizard={0}'.format(flag_value)
            request += ';market_model_wizard_premium_offer_url_type=External'
            request += ';market_implicit_model_wizard_premium_offers_url_type=OfferCard'

            # Проверяем все вьютайпы
            for extra_flags, viewtype in [
                (';market_implicit_model_wizard_premium_offers_count=1', 'market_implicit_model'),
                (
                    ';market_enable_implicit_model_adg_wiz=1;market_implicit_model_wizard_premium_offers_adg_count=1',
                    'market_implicit_model_adg_wizard',
                ),
                (
                    ';market_enable_implicit_model_wiz_center_incut=1;market_implicit_model_wizard_premium_offers_central_count=1',
                    'market_implicit_model_center_incut',
                ),
                (';device=touch;market_implicit_model_wizard_premium_offers_touch_count=1', 'market_implicit_model'),
            ]:
                response = self.report.request_bs_pb(request + extra_flags)
                if flag_value > 0:
                    # Проверяем формирование премиального оффера в модельном колдунщике
                    model_with_premium = {
                        "market_model": {
                            "showcase": {
                                "items": [
                                    {
                                        "title": {
                                            "text": {"__hl": {"raw": True, "text": "implicit 1 offer 1"}},
                                            "urlForCounter": Contains("//market-click2.yandex.ru/redir/dtype=market"),
                                        },
                                        "isPremium": "1",
                                    }
                                ]
                            }
                        }
                    }
                    self.assertFragmentIn(response, model_with_premium)

                implicit_with_premium = {
                    viewtype: {
                        "showcase": {
                            "premiumOffers": [
                                {
                                    "title": {
                                        "text": {"__hl": {"text": "implicit 1 offer 1", "raw": True}},
                                        "urlForCounter": Contains("//market-click2.yandex.ru/redir/dtype=offercard"),
                                    },
                                    "isPremium": "1",
                                }
                            ]
                        }
                    }
                }
                if implicit_wizard_has_premium:
                    self.assertFragmentIn(response, implicit_with_premium)
                else:
                    self.assertFragmentNotIn(response, implicit_with_premium)


if __name__ == '__main__':
    main()
