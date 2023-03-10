#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import HyperCategory, HyperCategoryType, MnPlace, Model, Offer, Shop
from core.testcase import TestCase, main
from core.matcher import Absent


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ["market_money_disable_bids=0"]
        cls.index.hypertree += [
            HyperCategory(
                hid=1,
                children=[
                    HyperCategory(hid=252920101, output_type=HyperCategoryType.SIMPLE),  # work
                    HyperCategory(hid=252920102, output_type=HyperCategoryType.SIMPLE),  # empty
                    HyperCategory(hid=252920103, output_type=HyperCategoryType.SIMPLE),
                    HyperCategory(hid=2529202, output_type=HyperCategoryType.GURULIGHT),
                    HyperCategory(hid=2529203, output_type=HyperCategoryType.GURU),  # guru
                    HyperCategory(hid=2529204, output_type=HyperCategoryType.CLUSTERS),
                ],
            ),
        ]

        cls.index.shops += [
            Shop(fesh=2529201, priority_region=213),
            Shop(fesh=2529202, priority_region=213),
            Shop(fesh=2529203, priority_region=213),
            Shop(fesh=2529204, priority_region=213),
            Shop(fesh=2529205, priority_region=213),
            Shop(fesh=2529206, priority_region=213),
            Shop(fesh=2529207, priority_region=213),
            Shop(fesh=2529208, priority_region=213),
        ]

        # non-guru
        cls.index.offers += [
            Offer(fesh=2529201, ts=1001, title="offer iphone 1", hid=252920101, bid=10),
            Offer(fesh=2529202, ts=1002, title="offer iphone 2", hid=252920101, bid=10),
            Offer(fesh=2529203, ts=1003, title="offer iphone 3", hid=252920101, bid=10),
            Offer(fesh=2529204, ts=1004, title="offer iphone 4", hid=252920101, bid=10),
            Offer(fesh=2529205, ts=1005, title="offer iphone 5", hid=252920101, bid=10),
            Offer(fesh=2529206, ts=1006, title="offer iphone 6", hid=252920101, bid=10),
        ]

        # guru
        cls.index.models += [
            Model(hyperid=2529201, hid=2529203),
        ]

        cls.index.offers += [
            Offer(fesh=2529201, ts=2001, title="offer iphone 21", hyperid=2529201, hid=2529203, bid=10),
            Offer(fesh=2529202, ts=2002, title="offer iphone 22", hyperid=2529201, hid=2529203, bid=10),
            Offer(fesh=2529203, ts=2003, title="offer iphone 23", hyperid=2529201, hid=2529203, bid=10),
            Offer(fesh=2529204, ts=2004, title="offer iphone 24", hyperid=2529201, hid=2529203, bid=10),
            Offer(fesh=2529205, ts=2005, title="offer iphone 25", hyperid=2529201, hid=2529203, bid=10),
            Offer(fesh=2529206, ts=2006, title="offer iphone 26", hyperid=2529201, hid=2529203, bid=10),
        ]

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 1001).respond(0.01)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 1002).respond(0.02)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 1003).respond(0.03)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 1004).respond(0.04)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 1005).respond(0.05)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 1006).respond(0.06)

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 2001).respond(0.01)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 2002).respond(0.02)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 2003).respond(0.03)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 2004).respond(0.04)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 2005).respond(0.05)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 2006).respond(0.06)

        cls.matrixnet.on_place(MnPlace.MATRIXNET_CLICK_VALUE_FORMULA, 1001).respond(0.06)
        cls.matrixnet.on_place(MnPlace.MATRIXNET_CLICK_VALUE_FORMULA, 1002).respond(0.05)
        cls.matrixnet.on_place(MnPlace.MATRIXNET_CLICK_VALUE_FORMULA, 1003).respond(0.04)
        cls.matrixnet.on_place(MnPlace.MATRIXNET_CLICK_VALUE_FORMULA, 1004).respond(0.03)
        cls.matrixnet.on_place(MnPlace.MATRIXNET_CLICK_VALUE_FORMULA, 1005).respond(0.02)
        cls.matrixnet.on_place(MnPlace.MATRIXNET_CLICK_VALUE_FORMULA, 1006).respond(0.01)

        cls.matrixnet.on_place(MnPlace.MATRIXNET_CLICK_VALUE_FORMULA, 2001).respond(0.06)
        cls.matrixnet.on_place(MnPlace.MATRIXNET_CLICK_VALUE_FORMULA, 2002).respond(0.05)
        cls.matrixnet.on_place(MnPlace.MATRIXNET_CLICK_VALUE_FORMULA, 2003).respond(0.04)
        cls.matrixnet.on_place(MnPlace.MATRIXNET_CLICK_VALUE_FORMULA, 2004).respond(0.03)
        cls.matrixnet.on_place(MnPlace.MATRIXNET_CLICK_VALUE_FORMULA, 2005).respond(0.02)
        cls.matrixnet.on_place(MnPlace.MATRIXNET_CLICK_VALUE_FORMULA, 2006).respond(0.01)

    def test_default_search(self):
        """
        ?????????????? ?????????????? ???? 6 ?? 1
        """
        response = self.report.request_json('place=prime&pp=7&hid=252920101')
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {
                            "shop": {
                                "id": 2529206,
                            }
                        },
                        {
                            "shop": {
                                "id": 2529205,
                            }
                        },
                        {
                            "shop": {
                                "id": 2529204,
                            }
                        },
                        {
                            "shop": {
                                "id": 2529203,
                            }
                        },
                        {
                            "shop": {
                                "id": 2529202,
                            }
                        },
                        {
                            "shop": {
                                "id": 2529201,
                            }
                        },
                    ]
                }
            },
            preserve_order=True,
        )

    def test_default_search_pp7_with_rearrfactor(self):
        """
        ?????????? ???????????????????? ?????????? ???? 2529204, ??.??. ???? top 3 (??.??. topN ???? ?????????????????? 3)
        ?? ?????????????????? isPremium
        """
        response = self.report.request_json('place=prime&pp=7&hid=252920101&rearr-factors=market_premium_in_nonguru=1')
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {
                            "shop": {
                                "id": 2529204,
                            },
                            "isPremium": True,
                        },
                        {
                            "shop": {
                                "id": 2529206,
                            }
                        },
                        {
                            "shop": {
                                "id": 2529205,
                            }
                        },
                        {
                            "shop": {
                                "id": 2529203,
                            }
                        },
                        {
                            "shop": {
                                "id": 2529202,
                            }
                        },
                        {
                            "shop": {
                                "id": 2529201,
                            }
                        },
                    ]
                }
            },
            preserve_order=True,
        )

    def test_default_search_pp48_with_rearrfactor(self):
        """
        ?????????? ???????????????????? ?????????? ???? 2529204, ??.??. ???? top 3 (??.??. topN ???? ?????????????????? 3)
        """
        response = self.report.request_json('place=prime&pp=48&hid=252920101&rearr-factors=market_premium_in_nonguru=1')
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {
                            "shop": {
                                "id": 2529204,
                            },
                            "isPremium": True,
                        },
                        {
                            "shop": {
                                "id": 2529206,
                            }
                        },
                        {
                            "shop": {
                                "id": 2529205,
                            }
                        },
                        {
                            "shop": {
                                "id": 2529203,
                            }
                        },
                        {
                            "shop": {
                                "id": 2529202,
                            }
                        },
                        {
                            "shop": {
                                "id": 2529201,
                            }
                        },
                    ]
                }
            },
            preserve_order=True,
        )

    def test_default_search_pp18_with_rearrfactor(self):
        """
        ?????????? ???????????? ???? ???????????????????? (??.??. pp ???? 7 ?? ???? 48)
        """
        response = self.report.request_json('place=prime&pp=18&hid=252920101&rearr-factors=market_premium_in_nonguru=1')
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {
                            "shop": {
                                "id": 2529206,
                            },
                            "isPremium": Absent(),
                        },
                        {
                            "shop": {
                                "id": 2529205,
                            }
                        },
                        {
                            "shop": {
                                "id": 2529204,
                            }
                        },
                        {
                            "shop": {
                                "id": 2529203,
                            }
                        },
                        {
                            "shop": {
                                "id": 2529202,
                            }
                        },
                        {
                            "shop": {
                                "id": 2529201,
                            }
                        },
                    ]
                }
            },
            preserve_order=True,
        )

    def test_default_search_pp7_with_rearrfactor_and_top6(self):
        """
        ?????????? ???????????????????? ?????????? ???? 2529201, ??.??. ???? top 6
        ?? ?????????????????? isPremium
        """
        response = self.report.request_json(
            'place=prime&pp=7&hid=252920101&rearr-factors=market_premium_in_nonguru=1;market_premium_in_nonguru_top=6'
        )
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {
                            "shop": {
                                "id": 2529201,
                            },
                            "isPremium": True,
                        },
                        {
                            "shop": {
                                "id": 2529206,
                            }
                        },
                        {
                            "shop": {
                                "id": 2529205,
                            }
                        },
                        {
                            "shop": {
                                "id": 2529204,
                            }
                        },
                        {
                            "shop": {
                                "id": 2529203,
                            }
                        },
                        {
                            "shop": {
                                "id": 2529202,
                            }
                        },
                    ]
                }
            },
            preserve_order=True,
        )

    def test_default_search_pp7_with_rearrfactor_and_top100500(self):
        """
        ?????????? ???????????????????? ?????????? ???? 2529201, ??.??. ???? top 100500, ???? ?? ?????? ?????????? 6
        ?? ?????????????????? isPremium
        """
        response = self.report.request_json(
            'place=prime&pp=7&hid=252920101&rearr-factors=market_premium_in_nonguru=1;market_premium_in_nonguru_top=100500'
        )
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {
                            "shop": {
                                "id": 2529201,
                            },
                            "isPremium": True,
                        },
                        {
                            "shop": {
                                "id": 2529206,
                            }
                        },
                        {
                            "shop": {
                                "id": 2529205,
                            }
                        },
                        {
                            "shop": {
                                "id": 2529204,
                            }
                        },
                        {
                            "shop": {
                                "id": 2529203,
                            }
                        },
                        {
                            "shop": {
                                "id": 2529202,
                            }
                        },
                    ]
                }
            },
            preserve_order=True,
        )
        self.click_log.expect(pp=220, position=1)
        self.click_log.expect(pp=7, position=2)
        self.click_log.expect(pp=7, position=3)
        self.click_log.expect(pp=7, position=4)
        self.click_log.expect(pp=7, position=5)
        self.click_log.expect(pp=7, position=6)

    def test_empty_cat(self):
        """
        ??????????????????, ?????? ???? ???????????? ?????????????????? ???? ????????????
        """
        response = self.report.request_json('place=prime&pp=7&hid=252920102&rearr-factors=market_premium_in_nonguru=1')
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 0,
                }
            },
        )

    def test_guru_cat(self):
        """
        ??????????????????, ?????? ???? ????????-?????????????????? ?????? ????????????????
        """
        response = self.report.request_json(
            'place=prime&pp=7&hid=2529203&rearr-factors=market_premium_in_nonguru=1;market_premium_in_nonguru_top=100500'
        )
        self.assertFragmentNotIn(
            response,
            {
                "search": {
                    "results": [
                        {
                            "isPremium": True,
                        },
                    ]
                }
            },
        )

    def test_text_without_hid(self):
        """
        ?????????????????? ?????? ?? ?????????????????? ?????????????? ?????? ?????????????????? ?????? ????????????????
        """
        response = self.report.request_json(
            'place=prime&pp=7&text=iphone&rearr-factors=market_premium_in_nonguru=1;market_premium_in_nonguru_top=100500'
        )
        self.assertFragmentNotIn(
            response,
            {
                "search": {
                    "results": [
                        {
                            "isPremium": True,
                        },
                    ]
                }
            },
        )

    def test_text_with_hid(self):
        """
        ?????????????????? ?????? ?? ?????????????????? ?????????????? ?? hid'???? (???? ????????) ???????? ????????????????
        """
        response = self.report.request_json(
            'place=prime&pp=7&text=iphone&hid=252920101&rearr-factors=market_premium_in_nonguru=1;market_premium_in_nonguru_top=100500'
        )
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {
                            "isPremium": True,
                        },
                    ]
                }
            },
        )

    @classmethod
    def prepare_premium_on_search(cls):
        cls.index.hypertree += [
            HyperCategory(hid=2603601, output_type=HyperCategoryType.SIMPLE),
        ]

        cls.index.shops += [
            Shop(fesh=2603601, priority_region=213),
            Shop(fesh=2603602, priority_region=213),
            Shop(fesh=2603603, priority_region=213),
        ]

        cls.index.offers += [
            Offer(fesh=2603601, ts=2603601, title="offer premium 2603601", hid=2603601, bid=10),
            Offer(fesh=2603602, ts=2603602, title="offer premium 2603602", hid=2603601, bid=1000),  # should be premium
            Offer(fesh=2603603, ts=2603603, title="offer premium 2603603", hid=2603601, bid=1000),
        ]

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 2603601).respond(0.1)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 2603602).respond(0.02)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 2603603).respond(0.01)
        cls.matrixnet.on_place(MnPlace.MATRIXNET_CLICK_VALUE_FORMULA, 2603601).respond(0.1)
        cls.matrixnet.on_place(MnPlace.MATRIXNET_CLICK_VALUE_FORMULA, 2603602).respond(0.02)
        cls.matrixnet.on_place(MnPlace.MATRIXNET_CLICK_VALUE_FORMULA, 2603603).respond(0.01)

    def test_fuid_on(self):
        """
        ??????????????????, ?????? ?? fuid ?????????????????? MaxCpm, ???????????? ?????????????? ?? ???????? ??????????
        ?? ?????? ?????????????????? prm
        """
        _ = self.report.request_json(
            'show-urls=external&place=prime&numdoc=10&pp=7&hid=2603601&rearr-factors=market_premium_in_nonguru=1;market_premium_in_nonguru_top=10'
        )
        self.show_log_tskv.expect(fuid='1606=20;2;500', is_premium_offer='1', position=1)
        self.click_log.expect(fuid="1606=20;2;500", prm=1, position=1)

    def test_change_pp_7_to_220(self):
        """
        ??????????????????, ?????? ?????????????????? pp 7 ???? 220
        """
        _ = self.report.request_json(
            'show-urls=external&place=prime&numdoc=10&pp=7&hid=2603601&rearr-factors=market_premium_in_nonguru=1;market_premium_in_nonguru_top=10'
        )
        self.show_log_tskv.expect(pp=220, is_premium_offer=1)
        self.click_log.expect(pp=220, prm=1)

    def test_change_pp_48_to_620(self):
        """
        ??????????????????, ?????? ?????????????????? pp 48 ???? 620
        """
        _ = self.report.request_json(
            'show-urls=external&place=prime&numdoc=10&pp=48&hid=2603601&rearr-factors=market_premium_in_nonguru=1;market_premium_in_nonguru_top=10'
        )
        self.show_log_tskv.expect(pp=620, is_premium_offer=1)
        self.click_log.expect(pp=620, prm=1)

    # MARKETOUT-26016
    @classmethod
    def prepare_min_ctr(cls):
        cls.index.hypertree += [
            HyperCategory(
                hid=2,
                children=[
                    HyperCategory(hid=2601601, output_type=HyperCategoryType.SIMPLE),
                    HyperCategory(hid=2601602, output_type=HyperCategoryType.SIMPLE),
                ],
            ),
        ]

        cls.index.shops += [
            Shop(fesh=2601601, priority_region=213),
            Shop(fesh=2601602, priority_region=213),
        ]

        cls.index.offers += [
            Offer(fesh=2601601, ts=2601601, title="offer iphone 2601601", hid=2601601, bid=10),
            Offer(fesh=2601602, ts=2601602, title="offer iphone 2601602", hid=2601602, bid=10),
        ]

        cls.matrixnet.on_place(MnPlace.MATRIXNET_CLICK_VALUE_FORMULA, 2601601).respond(0.0)
        cls.matrixnet.on_place(MnPlace.MATRIXNET_CLICK_VALUE_FORMULA, 2601602).respond(-100500.0)

    def test_zero_ctr(self):
        """
        ???????????????? ???? ????????, ???? ???????????????? 0.005 ?? ???????????????? CTR, ?????????? ???????????????????? ???? ???????????? - 0.05 (~0.049999997)
        """
        _ = self.report.request_json(
            'show-urls=external&place=prime&numdoc=10&pp=7&hid=2601601&rearr-factors=market_premium_in_nonguru=1;market_premium_in_nonguru_top=48'
        )
        self.show_log_tskv.expect(pp=220, is_premium_offer=1, fuid="1606=0.049999997;1;1")
        self.click_log.expect(pp=220, prm=1, fuid="1606=0.049999997;1;1")

    def test_negative_ctr(self):
        """
        ???????????????? ???? ?????????????????????????? ???????????????? ??????????????, ???? ???????????????? 0.005 ?? ???????????????? CTR, ?????????? ???????????????????? ???? ???????????? - 0.05 (~0.049999997)
        """

        # TODO: ???????????????????????????????? ?????????????? ?? ???????????????? ????????, ?????????????????? ???? ?????????????? ?????????????? MARKETPROJECT1606_EMPTYSEARCH.
        # ????????????????, ???????? ?????????????? ??????????????-????????????????????????????????, ???????? ???? ????????????????????????.dd
        _ = self.report.request_json(
            'show-urls=external&place=prime&numdoc=10&pp=7&hid=2601602&rearr-factors=market_premium_in_nonguru=1;market_premium_in_nonguru_top=48'
        )
        self.show_log_tskv.expect(pp=220, is_premium_offer=1, fuid="1606=0.049999997;1;1")
        self.click_log.expect(pp=220, prm=1, fuid="1606=0.049999997;1;1")


if __name__ == '__main__':
    main()
