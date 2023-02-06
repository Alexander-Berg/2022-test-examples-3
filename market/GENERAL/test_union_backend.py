#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa


from core.testcase import TestCase, main
from core.types import BlueOffer, MarketSku, MnPlace, Offer, Region, Shop
from core.matcher import NoKey


class T(TestCase):
    @classmethod
    def prepare_force_white_market(cls):
        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']

        cls.index.regiontree += [Region(rid=213, name='Москва')]

        cls.index.shops += [
            Shop(fesh=7301, cpa=Shop.CPA_REAL),
        ]

        for i in range(62):
            cls.index.shops += [Shop(fesh=38165000 + i, cpa=Shop.CPA_REAL)]

            cls.index.offers += [
                Offer(
                    price=20000 - i,
                    title='forced offer {}'.format(i),
                    ts=3816500 + i,
                    hyperid=3816500 + i,
                    fesh=38165000 + i,
                    hid=1,
                    cpa=Offer.CPA_REAL,
                )
            ]

        cls.index.mskus += [
            MarketSku(
                sku=38165620,
                title='1p forced tail оффер 3816562',
                hyperid=3816562,
                hid=1,
                blue_offers=[
                    BlueOffer(
                        price=10000,  # To prevent GOOD_PRICE_IN_MEARKETPLACE
                        offerid='Shop777_sku{}'.format(38165620),
                        ts=3816563,
                    )
                ],
            ),
            MarketSku(
                sku=38165640,
                title='1p default оффер 3816564',
                hyperid=3816564,
                hid=1,
                blue_offers=[
                    BlueOffer(
                        price=3500,  # To prevent GOOD_PRICE_IN_MEARKETPLACE
                        offerid='Shop777_sku{}'.format(38165640),
                        ts=3816566,
                    )
                ],
            ),
        ]

        cls.index.offers += [
            Offer(
                price=7000,
                hid=1,
                title='default offer matched',
                ts=3816562,
                hyperid=3816562,
                fesh=7301,
                randx=500,
                cpa=Offer.CPA_REAL,
            ),
            Offer(
                price=6000,
                hid=1,
                title='default offer matched 2',
                ts=3816564,
                hyperid=3816563,
                fesh=7301,
                randx=500,
                cpa=Offer.CPA_REAL,
                has_url=False,
            ),
            Offer(
                price=5000,
                hid=1,
                title='forced tail offer matched 2',
                ts=3816565,
                hyperid=3816563,
                fesh=7301,
                randx=500,
                cpa=Offer.CPA_REAL,
            ),
            Offer(
                price=4000,
                hid=1,
                title='forced tail offer matched 3',
                ts=3816567,
                hyperid=3816564,
                fesh=7301,
                randx=500,
                cpa=Offer.CPA_REAL,
            ),
            Offer(
                price=3000,
                hid=1,
                title='forced tail offer skutched',
                ts=3816568,
                sku=123,
                fesh=7301,
                randx=600,
                cpa=Offer.CPA_REAL,
            ),
            Offer(
                price=2000,
                hid=1,
                title='forced tail offer alone',
                ts=3816569,
                fesh=7301,
                randx=700,
                cpa=Offer.CPA_REAL,
            ),
            Offer(
                price=1000,
                hid=1,
                title='forced tail dsbs offer alone',
                ts=3816570,
                fesh=7301,
                randx=700,
                cpa=Offer.CPA_REAL,
                has_url=False,
            ),
        ]

        for i in range(71):
            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 3816500 + i).respond(0.95 - (i * 0.001))
            cls.matrixnet.on_place(MnPlace.META_REARRANGE, 3816500 + i).respond(0.95 - (i * 0.001))

    def test_force_white_market_rearrange(self):
        '''Проверяем, что выполняется дозапрос за ДО для белых и синих офферов, в т.ч. на сортировке по цене
        и что на поиске присутствуют неприматченные офферы, в т.ч. dsbs
        '''
        for onstock in ['', '&onstock=1']:
            rearr_flags = [
                'market_force_white_on=70,80,81,98,99,102,103,113,151',
                'market_ranging_blue_offer_priority_eq_dsbs=0',
            ]
            response = self.report.request_json(
                'place=prime&rgb=blue&cpa=real&text=tail&allow-collapsing=1&use-default-offers=1&{}&rearr-factors={}'.format(
                    onstock, ';'.join(rearr_flags)
                )
            )
            self.assertFragmentIn(response, {"total": 6})

            response = self.report.request_json(
                'place=prime&numdoc=10&page=7&rgb=blue&cpa=real&text=tail&allow-collapsing=1&use-default-offers=1&{}&rearr-factors={}'.format(
                    onstock, ';'.join(rearr_flags)
                )
            )
            self.assertFragmentIn(
                response,
                {
                    "results": [
                        {
                            "entity": "product",  # "default offer matched" не выбран из-за флагов use_offer_type_priority_as_main_factor_in_do=1 и market_ranging_blue_offer_priority_eq_dsbs=1. Если приоритет MD убрать, то будет выбран он.  # noqa
                            "offers": {"items": [{"titles": {"raw": "1p forced tail оффер 3816562"}}]},
                        },
                        {
                            "entity": "product",
                            "offers": {"items": [{"titles": {"raw": "default offer matched 2"}}]},
                        },
                        {
                            "entity": "product",
                            "offers": {"items": [{"titles": {"raw": "1p default оффер 3816564"}}]},
                        },
                        {
                            "entity": "offer",
                            "titles": {"raw": "forced tail offer skutched"},
                        },
                        {
                            "entity": "product",  # Виртуальная модель
                            "offers": {"items": [{"titles": {"raw": "forced tail offer alone"}}]},
                        },
                        {
                            "entity": "product",  # Виртуальная модель
                            "offers": {"items": [{"titles": {"raw": "forced tail dsbs offer alone"}}]},
                        },
                    ]
                },
                preserve_order=True,
                allow_different_len=False,
            )

            # Проверяем сортировку по цене
            response = self.report.request_json(
                'place=prime&how=aprice&rgb=blue&cpa=real&text=tail&allow-collapsing=1&use-default-offers=1&{}&rearr-factors={}'.format(
                    onstock, ';'.join(rearr_flags)
                )
            )
            self.assertFragmentIn(response, {"total": 6})

            response = self.report.request_json(
                'place=prime&how=aprice&numdoc=10&page=7&rgb=blue&cpa=real&text=tail&allow-collapsing=1&use-default-offers=1&{}&rearr-factors={}'.format(
                    onstock, ';'.join(rearr_flags)
                )
            )
            self.assertFragmentIn(
                response,
                {
                    "results": [
                        {
                            "entity": "product",  # Виртуальная модель
                            "offers": {"items": [{"titles": {"raw": "forced tail dsbs offer alone"}}]},
                        },
                        {
                            "entity": "product",  # Виртуальная модель
                            "offers": {"items": [{"titles": {"raw": "forced tail offer alone"}}]},
                        },
                        {
                            "entity": "offer",
                            "titles": {"raw": "forced tail offer skutched"},
                        },
                        {
                            "entity": "product",
                            "offers": {"items": [{"titles": {"raw": "1p default оффер 3816564"}}]},
                        },
                        {
                            "entity": "product",
                            "offers": {"items": [{"titles": {"raw": "default offer matched 2"}}]},
                        },
                        {
                            "entity": "product",  # "default offer matched" не выбран из-за флага use_offer_type_priority_as_main_factor_in_do
                            "offers": {"items": [{"titles": {"raw": "1p forced tail оффер 3816562"}}]},
                        },
                    ]
                },
                preserve_order=True,
                allow_different_len=False,
            )

    def test_force_white_market_rearrange_after_60(self):
        '''Проверяем, что дозапрос за ДО не выполняется для позиций больше 60,
        и что на поиске присутствуют неприматченные офферы, в т.ч. dsbs
        '''
        for onstock in ['', '&onstock=1']:

            response = self.report.request_json(
                'place=prime&rgb=blue&cpa=real&text=forced&allow-collapsing=1&use-default-offers=1&{}'.format(onstock)
                + '&rearr-factors=market_force_white_on=70,80,81,98,99,102,103,113,151'
            )
            self.assertFragmentIn(response, {"total": 68})

            response = self.report.request_json(
                'place=prime&numdoc=10&page=7&rgb=blue&cpa=real&text=forced&allow-collapsing=1&use-default-offers=1&{}'.format(
                    onstock
                )
                + '&rearr-factors=market_force_white_on=70,80,81,98,99,102,103,113,151'
            )
            self.assertFragmentIn(
                response,
                {
                    "results": [
                        {
                            "entity": "product",
                            "offers": {"items": [{"titles": {"raw": "forced offer 60"}}]},
                        },
                        {
                            "entity": "product",
                            "offers": {"items": [{"titles": {"raw": "forced offer 61"}}]},
                        },
                        {
                            "entity": "product",
                            "offers": {"items": [{"titles": {"raw": "1p forced tail оффер 3816562"}}]},
                        },
                        {
                            "entity": "product",
                            "offers": {"items": [{"titles": {"raw": "forced tail offer matched 2"}}]},
                        },
                        {
                            "entity": "product",
                            "offers": {"items": [{"titles": {"raw": "forced tail offer matched 3"}}]},
                        },
                        {
                            "entity": "offer",
                            "titles": {"raw": "forced tail offer skutched"},
                        },
                        {
                            "entity": "product",  # Виртуальная модель
                            "offers": {"items": [{"titles": {"raw": "forced tail offer alone"}}]},
                        },
                        {
                            "entity": "product",  # Виртуальная модель
                            "offers": {"items": [{"titles": {"raw": "forced tail dsbs offer alone"}}]},
                        },
                    ]
                },
                preserve_order=True,
                allow_different_len=False,
            )

        for onstock in ['&onstock=1']:
            response = self.report.request_json(
                'place=prime&rgb=blue&cpa=real&text=forced&allow-collapsing=1&use-default-offers=1&{}'.format(onstock)
                + '&rearr-factors=market_force_white_on=70,80,81,98,99,102,103,113,151;market_use_collapsed_as_do=0'
            )
            self.assertFragmentIn(response, {"total": 68})

            response = self.report.request_json(
                'place=prime&numdoc=10&page=7&rgb=blue&cpa=real&text=forced&allow-collapsing=1&use-default-offers=1&{}'.format(
                    onstock
                )
                + '&rearr-factors=market_force_white_on=70,80,81,98,99,102,103,113,151;market_use_collapsed_as_do=0'
            )
            self.assertFragmentIn(
                response,
                {
                    "results": [
                        {
                            "entity": "product",
                            "offers": {"items": NoKey("items")},
                        },
                        {
                            "entity": "product",
                            "offers": {"items": NoKey("items")},
                        },
                        {
                            "entity": "product",
                            "offers": {"items": NoKey("items")},
                        },
                        {
                            "entity": "product",
                            "offers": {"items": NoKey("items")},
                        },
                        {
                            "entity": "product",
                            "offers": {"items": NoKey("items")},
                        },
                        {
                            "entity": "offer",
                            "titles": {"raw": "forced tail offer skutched"},
                        },
                        {
                            "entity": "product",  # Виртуальная модель
                            "offers": {"items": [{"titles": {"raw": "forced tail offer alone"}}]},
                        },
                        {
                            "entity": "product",  # Виртуальная модель
                            "offers": {"items": [{"titles": {"raw": "forced tail dsbs offer alone"}}]},
                        },
                    ]
                },
                preserve_order=True,
                allow_different_len=False,
            )

    def test_force_white_market(self):
        """Просто проверяем что не падает хотя бы"""
        for place in range(200):
            for query in [
                'place=productoffers&hyperid=3816562&rids=213',
                'place=prime&text=forced',
                'place=prime&allow-collapsing=1&use-default-offers=1&hid=1&how=aprice',
            ]:
                _ = self.report.request_json(query + '&rgb=blue&rearr-factors=market_force_white_on={}'.format(place))
                _ = self.report.request_json(query + '&rearr-factors=market_force_blue_on={}'.format(place))


if __name__ == '__main__':
    main()
