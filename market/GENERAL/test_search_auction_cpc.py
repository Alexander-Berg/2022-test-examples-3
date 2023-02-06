#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import ClickType, DeliveryBucket, GLParam, GLType, HyperCategory, MnPlace, Model, Offer, Region, Shop
from core.testcase import TestCase, main
from core.matcher import Contains

import math


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ["market_money_disable_bids=0"]
        cls.settings.default_search_experiment_flags += ['market_new_cpm_iterator=0']
        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']

        cls.index.regiontree += [
            Region(
                rid=149,
                name='Беларусь',
                region_type=Region.COUNTRY,
                children=[
                    Region(rid=20729, name='Бобруйск'),
                ],
            ),
            Region(
                rid=187,
                name='Украина',
                region_type=Region.COUNTRY,
                children=[
                    Region(rid=144, name='Львов'),
                ],
            ),
            Region(
                rid=159,
                name='Казахстан',
                region_type=Region.COUNTRY,
                children=[
                    Region(rid=163, name='Астана'),
                ],
            ),
            Region(
                rid=21227,
                name='Сомали',
                region_type=Region.COUNTRY,
                children=[
                    Region(rid=37179, name='Могадишо'),
                ],
            ),
        ]

        cls.index.models += [
            Model(title="hirel model", ts=1, randx=11, hid=102, hyperid=700),
            Model(title="midrel model", ts=2, randx=12, hid=102, hyperid=800),
            Model(title="lowrel model", ts=3, randx=13, hid=102, hyperid=900),
            Model(ts=4, randx=14, hid=101, hyperid=500),
            Model(ts=5, randx=15, hid=101, hyperid=600),
        ]

        cls.index.offers += [
            Offer(fesh=1, hyperid=500, ts=6, randx=16, hid=101, picture_flags=1, bid=15, price=12000),
            Offer(fesh=2, hyperid=500, ts=7, randx=17, hid=101, picture_flags=1, bid=10, price=5300),
            Offer(fesh=3, hyperid=600, ts=8, randx=18, hid=101, picture_flags=1, bid=10, price=4300),
            Offer(fesh=4, hyperid=600, ts=9, randx=19, hid=101, picture_flags=1, bid=12, price=3300),
        ]

        cls.index.hypertree += [HyperCategory(hid=102, children=[HyperCategory(hid=10002)])]

        cls.index.shops += [
            Shop(fesh=1, priority_region=213, regions=[225]),
            Shop(fesh=2, priority_region=213, regions=[225]),
            Shop(fesh=3, priority_region=213, regions=[225]),
            Shop(fesh=4, priority_region=213, regions=[225]),
            Shop(fesh=123, priority_region=213),
            Shop(fesh=124, priority_region=213),
            Shop(fesh=234, priority_region=2, regions=[213]),
            Shop(fesh=235, priority_region=2, regions=[213]),
        ]

        cls.index.delivery_buckets += [
            DeliveryBucket.default(bucket_id=234, fesh=234),
            DeliveryBucket.default(bucket_id=235, fesh=235),
        ]

        cls.index.offers += [
            Offer(fesh=1, title="bid 35", ts=10, hid=102, picture_flags=1, bid=35, price=8000, hyperid=700),
            Offer(fesh=2, title="bid 25", ts=11, hid=102, picture_flags=1, bid=25, price=8000, hyperid=700),
            Offer(fesh=3, title="bid 15", ts=12, hid=102, picture_flags=1, bid=15, price=8000, hyperid=800),
            Offer(fesh=4, title="bid 05", ts=13, hid=102, picture_flags=1, bid=5, price=8000, hyperid=900),
        ]

        cls.index.offers += [
            # local head
            Offer(title="bid 75", fesh=123, ts=14, hid=103, picture_flags=1, bid=75, price=8000),
            # local tail
            Offer(title="bid 65", fesh=123, ts=15, hid=103, picture_flags=1, bid=65, price=8000),
            # non-local head
            Offer(
                title="bid 55", fesh=234, ts=16, hid=103, picture_flags=1, bid=55, price=8000, delivery_buckets=[234]
            ),
            # non-local tail
            Offer(
                title="bid 45", fesh=234, ts=17, hid=103, picture_flags=1, bid=45, price=8000, delivery_buckets=[234]
            ),
            # local head
            Offer(title="bid 35", fesh=124, ts=18, hid=103, picture_flags=1, bid=35, price=8000),
            # local tail
            Offer(title="bid 25", fesh=124, ts=19, hid=103, picture_flags=1, bid=25, price=8000),
            # non-local-head
            Offer(
                title="bid 15", fesh=235, ts=20, hid=103, picture_flags=1, bid=15, price=8000, delivery_buckets=[235]
            ),
            # non-local tail
            Offer(title="bid 05", fesh=235, ts=21, hid=103, picture_flags=1, bid=5, price=8000, delivery_buckets=[235]),
        ]

        cls.matrixnet.on_default_place(MnPlace.BASE_SEARCH).respond(0.1)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 1).respond(0.2)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 2).respond(0.107)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 3).respond(0.01)

    def test_auction_formula_calculation(self):
        def auction(bid, min_bid, min_bid_multiplier):
            alpha = 0.61
            beta = 0.01
            gamma = 2.742
            return 1.0 + alpha * (
                1.0 / (1.0 + gamma * math.exp(-beta * (bid - min_bid * min_bid_multiplier))) - 1.0 / (1.0 + gamma)
            )

        # main search
        results = [(4, 0, 0), (5, 0, 0), (6, 15, 9), (7, 10, 9), (8, 10, 6), (9, 12, 6)]  # (ts, bid, min_bid)

        response = self.report.request_json(
            'place=prime&hid=101&rearr-factors=market_force_search_auction=Cpc;market_disable_auction_for_offers_with_model=0&debug-doc-count=60&debug=da&rids=213'
        )
        for ts, bid, min_bid in results:
            if bid != 0:  # offer
                mul = auction(bid, min_bid, 1.0)
                self.assertFragmentIn(
                    response,
                    {
                        "properties": {"TS": str(ts), "BID": str(bid), "MIN_BID": str(min_bid)},
                        "rank": [{"name": "CPM", "value": str(int(10000 * mul))}],
                    },
                    preserve_order=False,
                )
            else:  # model
                self.assertFragmentIn(
                    response,
                    {
                        "properties": {
                            "TS": str(ts),
                            "VBID": "0",
                        },
                        "rank": [{"name": "CPM", "value": "10000"}],
                    },
                    preserve_order=False,
                )

        # parallel
        response = self.report.request_bs(
            'place=parallel&hid=101&rearr-factors='
            'market_force_search_auction=Cpc;market_no_restrictions=1&'
            'debug-doc-count=6&debug=da&rids=213'
        )
        debug_response = response.extract_debug_response()

        for ts, bid, min_bid in results:
            mul = auction(bid, min_bid, 1.0)
            self.assertFragmentIn(
                debug_response,
                '''
            <document>
                <properties>
                    <RANDX value="{0}"/>
                    <BID value="{1}"/>
                    <MIN_BID value="{2}"/>
                </properties>
                <rank>
                    <CPM value="{3}"/>
                </rank>
            </document>'''.format(
                    ts + 10, bid, min_bid, int(10000 * mul)
                ),
                preserve_order=False,
            )

    def test_search_auction_countries(self):
        cpc_regions = ['20729', '163']
        no_auction_regions = ['144', '37179']

        for region in cpc_regions:
            response = self.report.request_json(
                'place=prime&text=formula+calculation&rids={0}&debug=da&rearr-factors=market_force_search_auction=Cpc;'
                'market_tweak_search_auction_params=0.13,0.1,6;market_tweak_search_auction_cpa_params=0.43,0.3,4'.format(
                    region
                )
            )
            self.assertFragmentIn(
                response,
                {
                    "logicTrace": [
                        Contains(
                            "Using search auction with parameters:",
                            "cpaAlpha=0.43 cpaBeta=0.3 cpaGamma=4",
                            "cpcAlpha=0.13 cpcBeta=0.1 cpcGamma=6",
                            "auction type: 0",
                        )
                    ]
                },
            )

        for region in no_auction_regions:
            response = self.report.request_json(
                'place=prime&text=formula+calculation&rids={0}&debug=da&rearr-factors=market_force_search_auction=Cpc;'
                'market_tweak_search_auction_params=0.13,0.1,6;market_tweak_search_auction_cpa_params=0.43,0.3,4'.format(
                    region
                )
            )
            self.assertFragmentNotIn(response, {"logicTrace": [Contains("Using search auction")]})

    def test_auction_formula_tweaks(self):
        response = self.report.request_json(
            'place=prime&hid=101&debug=da&rids=213&rearr-factors='
            'market_force_search_auction=Cpc;market_tweak_search_auction_params=0.13,0.1,6'
        )
        self.assertFragmentIn(
            response,
            {
                "logicTrace": [
                    Contains(
                        "Using search auction with parameters:",
                        "cpaAlpha=0.4 cpaBeta=0.4 cpaGamma=2",
                        "cpcAlpha=0.13 cpcBeta=0.1 cpcGamma=6",
                        "auction type: 0",
                    )
                ]
            },
        )
        response = self.report.request_bs(
            'place=parallel&hid=101&debug=da&rids=213&rearr-factors='
            'market_force_search_auction=Cpc;market_tweak_search_auction_params=0.13,0.1,6'
        )
        self.assertFragmentIn(
            response,
            "Using search auction with parameters:"
            " cpaAlpha=0.4 cpaBeta=0.4 cpaGamma=2"
            " cpcAlpha=0.13 cpcBeta=0.1 cpcGamma=6",
        )
        self.assertFragmentIn(response, " auction type: 0")

    def test_auction_autobroker_basic_prime(self):
        response = self.report.request_json(
            'place=prime&hid=102&rearr-factors=market_force_search_auction=Cpc;market_tweak_search_auction_params=0.13,0.14,5;market_disable_auction_for_offers_with_model=0&rids=213&show-urls=external'  # noqa
        )

        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"titles": {"raw": "hirel model"}},
                    {"titles": {"raw": "bid 35"}},
                    {"titles": {"raw": "midrel model"}},
                    {"titles": {"raw": "bid 25"}},
                    {"titles": {"raw": "bid 15"}},
                    {"titles": {"raw": "bid 05"}},
                    {"titles": {"raw": "lowrel model"}},
                ]
            },
            preserve_order=True,
        )

        self.show_log.expect(title="hirel model", position=1)
        self.show_log.expect(title="bid 35", bid=35, click_price=29, position=2)
        self.show_log.expect(title="midrel model", position=3)
        self.show_log.expect(title="bid 25", bid=25, click_price=16, position=4)
        self.show_log.expect(title="bid 15", bid=15, click_price=12, position=5)
        self.show_log.expect(title="bid 05", bid=11, click_price=11, position=6)
        self.show_log.expect(title="lowrel model", position=7)
        self.click_log.expect(ClickType.EXTERNAL, cb=35, cp=29, position=2)
        self.click_log.expect(ClickType.EXTERNAL, cb=25, cp=16, position=4)
        self.click_log.expect(ClickType.EXTERNAL, cb=15, cp=12, position=5)
        self.click_log.expect(ClickType.EXTERNAL, cb=11, cp=11, position=6)

    def test_auction_autobroker_basic_parallel(self):
        response = self.report.request_bs(
            'place=parallel&bsformat=1&hid=102&rearr-factors='
            'market_force_search_auction=Cpc;'
            'market_offers_incut_threshold=0.0;market_tweak_search_auction_params=0.13,0.14,5&rids=213'
        )

        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": [
                    {
                        "showcase": {
                            "items": [
                                {"title": {"text": {"__hl": {"text": "bid 35", "raw": True}}}},
                                {"title": {"text": {"__hl": {"text": "bid 25", "raw": True}}}},
                                {"title": {"text": {"__hl": {"text": "bid 15", "raw": True}}}},
                            ]
                        }
                    }
                ]
            },
            preserve_order=True,
        )
        self.show_log.expect(title="bid 35", bid=35, click_price=26, position=1)
        self.show_log.expect(title="bid 25", bid=25, click_price=16, position=2)
        self.show_log.expect(title="bid 15", bid=15, click_price=12, position=3)
        self.click_log.expect(ClickType.EXTERNAL, cb=35, cp=26, position=1)
        self.click_log.expect(ClickType.EXTERNAL, cb=25, cp=16, position=2)
        self.click_log.expect(ClickType.EXTERNAL, cb=15, cp=12, position=3)

    def test_auction_autobroker_cauldrons_prime(self):
        # cauldrons are used in prime only for text ranking
        response = self.report.request_json(
            'place=prime&hid=103&text=bid&rids=213&rearr-factors=market_force_search_auction=Cpc&show-urls=external'
        )

        self.assertFragmentIn(
            response,
            {
                "results": [
                    # local head
                    {"titles": {"raw": "bid 75"}, "shop": {"id": 123}},
                    {"titles": {"raw": "bid 35"}, "shop": {"id": 124}},
                    # local tail (second offer from the same shop)
                    {"titles": {"raw": "bid 65"}, "shop": {"id": 123}},
                    {"titles": {"raw": "bid 25"}, "shop": {"id": 124}},
                    {"entity": "regionalDelimiter"},
                    # non-local head
                    {"titles": {"raw": "bid 55"}, "shop": {"id": 234}},
                    {"titles": {"raw": "bid 15"}, "shop": {"id": 235}},
                    # non-local tail
                    {"titles": {"raw": "bid 45"}, "shop": {"id": 234}},
                    {"titles": {"raw": "bid 05"}, "shop": {"id": 235}},
                ]
            },
            preserve_order=True,
        )

        self.show_log.expect(title="bid 75", bid=75, click_price=36, position=1)
        self.show_log.expect(title="bid 35", bid=35, click_price=11, position=2)
        self.show_log.expect(title="bid 65", bid=65, click_price=26, position=3)
        self.show_log.expect(title="bid 25", bid=25, click_price=11, position=4)
        self.show_log.expect(title="bid 55", bid=55, click_price=16, position=5)
        self.show_log.expect(title="bid 15", bid=15, click_price=11, position=6)
        self.show_log.expect(title="bid 45", bid=45, click_price=12, position=7)
        self.show_log.expect(title="bid 05", bid=11, click_price=11, position=8)
        self.click_log.expect(ClickType.EXTERNAL, cb=75, cp=36, position=1)
        self.click_log.expect(ClickType.EXTERNAL, cb=35, cp=11, position=2)
        self.click_log.expect(ClickType.EXTERNAL, cb=65, cp=26, position=3)
        self.click_log.expect(ClickType.EXTERNAL, cb=25, cp=11, position=4)
        self.click_log.expect(ClickType.EXTERNAL, cb=55, cp=16, position=5)
        self.click_log.expect(ClickType.EXTERNAL, cb=15, cp=11, position=6)
        self.click_log.expect(ClickType.EXTERNAL, cb=45, cp=12, position=7)
        self.click_log.expect(ClickType.EXTERNAL, cb=11, cp=11, position=8)

    @classmethod
    def prepare_autobroker_in_filtering(cls):
        cls.index.hypertree += [HyperCategory(hid=1102)]

        cls.index.models += [
            Model(title="hirel glfilter model", ts=1, randx=11, hid=1102, hyperid=1700),
            Model(title="midrel glfilter model", ts=2, randx=12, hid=1102, hyperid=1800),
            Model(title="lowrel glfilter model", ts=3, randx=13, hid=1102, hyperid=1900),
        ]

        cls.index.gltypes = [
            GLType(param_id=202, hid=1102, gltype=GLType.ENUM, values=list(range(40, 51)), unit_name="ENUM"),
            GLType(param_id=204, hid=1102, gltype=GLType.BOOL, unit_name="BOOL"),
            GLType(param_id=211, hid=1102, gltype=GLType.NUMERIC, unit_name="NUMERIC"),
        ]

        cls.index.offers += [
            Offer(
                fesh=1,
                title="bid 35",
                ts=10,
                hid=1102,
                picture_flags=1,
                bid=35,
                price=8000,
                hyperid=1700,
                glparams=[GLParam(param_id=202, value=51)],
            ),
            Offer(
                fesh=2,
                title="bid 25",
                ts=11,
                hid=1102,
                picture_flags=1,
                bid=25,
                price=8000,
                hyperid=1700,
                glparams=[GLParam(param_id=204, value=1), GLParam(param_id=211, value=0.5)],
            ),
            Offer(
                fesh=3,
                title="bid 15",
                ts=12,
                hid=1102,
                picture_flags=1,
                bid=15,
                price=8000,
                hyperid=1800,
                glparams=[GLParam(param_id=211, value=0.5)],
            ),
            Offer(
                fesh=4,
                title="bid 05",
                ts=13,
                hid=1102,
                picture_flags=1,
                bid=5,
                price=8000,
                hyperid=1900,
                glparams=[GLParam(param_id=202, value=51), GLParam(param_id=204, value=1)],
            ),
        ]

    def test_autobroker_glfiltering_enum_prime(self):
        """
        Проверяется, что в случае gl фильтрации (тип фильтра enum) автоброкер работает для показанных документов
        """
        response = self.report.request_json(
            'place=prime&hid=1102&rearr-factors=market_force_search_auction=Cpc;market_tweak_search_auction_params=0.13,0.14,5;market_disable_auction_for_offers_with_model=0&rids=213&show-urls=external&glfilter=202:51'  # noqa
        )
        self.assertFragmentIn(
            response, {"results": [{"titles": {"raw": "bid 35"}}, {"titles": {"raw": "bid 05"}}]}, preserve_order=True
        )
        self.show_log.expect(title="bid 35", bid=35, click_price=12, position=1)
        self.show_log.expect(title="bid 05", bid=11, click_price=11, position=2)
        self.click_log.expect(ClickType.EXTERNAL, cb=35, cp=12, position=1)
        self.click_log.expect(ClickType.EXTERNAL, cb=11, cp=11, position=2)

    def test_autobroker_glfiltering_bool_prime(self):
        """
        Проверяется, что в случае gl фильтрации (тип фильтра bool) автоброкер работает для показанных документов
        """
        response = self.report.request_json(
            'place=prime&hid=1102&rearr-factors=market_force_search_auction=Cpc;market_tweak_search_auction_params=0.13,0.14,5;market_disable_auction_for_offers_with_model=0&rids=213&show-urls=external&glfilter=204:1'  # noqa
        )
        self.assertFragmentIn(
            response, {"results": [{"titles": {"raw": "bid 25"}}, {"titles": {"raw": "bid 05"}}]}, preserve_order=True
        )
        self.show_log.expect(title="bid 25", bid=25, click_price=12, position=1)
        self.show_log.expect(title="bid 05", bid=11, click_price=11, position=2)
        self.click_log.expect(ClickType.EXTERNAL, cb=25, cp=12, position=1)
        self.click_log.expect(ClickType.EXTERNAL, cb=11, cp=11, position=2)

    def test_autobroker_glfiltering_num_prime(self):
        """
        Проверяется, что в случае gl фильтрации (тип фильтра numeric) автоброкер работает для показанных документов
        """
        response = self.report.request_json(
            'place=prime&hid=1102&rearr-factors=market_force_search_auction=Cpc;market_tweak_search_auction_params=0.13,0.14,5;market_disable_auction_for_offers_with_model=0&rids=213&show-urls=external&glfilter=211:0,5'  # noqa
        )
        self.assertFragmentIn(
            response, {"results": [{"titles": {"raw": "bid 25"}}, {"titles": {"raw": "bid 15"}}]}, preserve_order=True
        )
        self.show_log.expect(title="bid 25", bid=25, click_price=16, position=1)
        self.show_log.expect(title="bid 15", bid=15, click_price=11, position=2)
        self.click_log.expect(ClickType.EXTERNAL, cb=25, cp=16, position=1)
        self.click_log.expect(ClickType.EXTERNAL, cb=15, cp=11, position=2)

    def test_parallel_default_auction_type(self):
        """Проверяем, что на параллельном поиске по умолчанию включен Cpc-аукцион,
        если не обратный эксперимент.
        """
        response = self.report.request_bs('place=parallel&hid=101&debug=da&rids=213')
        self.assertFragmentIn(response, "auction type: 0")
        response = self.report.request_bs(
            'place=parallel&hid=101&debug=da&rids=213&rearr-factors=market_force_search_auction=Cpa'
        )
        self.assertFragmentIn(response, "auction type: 1")

    def test_parallel_auction_formula_tweaks(self):
        """Проверяем, что флаг market_parallel_tweak_search_auction_params
        задает параметры аукциона на параллельном
        https://st.yandex-team.ru/MARKETOUT-40784
        """
        response = self.report.request_bs(
            'place=parallel&hid=101&debug=da&rids=213&rearr-factors='
            'market_tweak_search_auction_params=0.13,0.1,6;market_force_search_auction=Cpc;'
        )
        self.assertFragmentIn(
            response,
            "Using search auction with parameters:"
            " cpaAlpha=0.4 cpaBeta=0.4 cpaGamma=2"
            " cpcAlpha=0.13 cpcBeta=0.1 cpcGamma=6",
        )
        self.assertFragmentIn(response, "auction type: 0")


if __name__ == '__main__':
    main()
