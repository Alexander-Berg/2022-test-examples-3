#!/usr/bin/env python
# -*- coding: utf-8 -*-

import runner  # noqa


from core.types import ClickType, HybridAuctionParam, MnPlace, Model, Offer, Shop
from core.testcase import TestCase, main
from core.matcher import Not, Equal, Absent, EqualToOneOfOrAbsent
from core.types.autogen import Const
import math


class RearrFactors:
    LogicDisabled = "disabled"
    LogicAdd = "add"
    LogicMark = "mark"
    LogicMarkTouch = "mark-touch"

    def __str__(self):
        return self.__factors

    def __init__(self):
        self.__factors = "&rearr-factors="

    def logic(self, x):
        self.__factors += "market_premium_offer_logic={};".format(x)
        return self

    def threshold_multiplier(self, x):
        self.__factors += "market_premium_offer_threshold_mult={};".format(x)
        return self

    def not_duplicate_do_shop(self, x):
        self.__factors += "market_premium_offer_not_duplicate_do_shop={};".format(x)
        return self


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ["market_money_disable_bids=0"]
        cls.index.shops += [
            Shop(fesh=1, priority_region=213, cpa=Shop.CPA_REAL),
            Shop(fesh=2, priority_region=213, cpa=Shop.CPA_REAL),
            Shop(fesh=3, priority_region=213, cpa=Shop.CPA_REAL),
            Shop(fesh=4, priority_region=312, cpa=Shop.CPA_REAL, regions=[213]),
            Shop(fesh=5, priority_region=312, cpa=Shop.CPA_REAL, regions=[213]),
        ]

        cls.index.models += [
            Model(hyperid=101),
            Model(hyperid=102),
        ]

        cls.index.hybrid_auction_settings += [HybridAuctionParam(category=Const.ROOT_HID, cpc_ctr_for_cpc=0.033)]

        cls.index.offers += [
            Offer(hyperid=101, fesh=1, price=10000, bid=10, ts=101001),  # default; CPM = 100000 * 10 * 0.02 ~ 20000
            Offer(
                hyperid=101, fesh=2, price=10000, bid=50, ts=101002
            ),  # premium & top-1; CPM = 100000 * 50 * 0.01 ~ 50000
            Offer(hyperid=101, fesh=3, price=10000, bid=30, ts=101003),  # CPM = 100000 * 30 * 0.01 ~ 30000
        ]
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 101001).respond(0.02)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 101002).respond(0.01)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 101003).respond(0.01)

        cls.index.offers += [
            Offer(hyperid=102, fesh=1, price=10000, bid=30, ts=102001),  # default & premium
            Offer(hyperid=102, fesh=2, price=10000, bid=25, ts=102002),
            Offer(hyperid=102, fesh=3, price=10000, bid=25, ts=102003),
        ]
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 102001).respond(0.02)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 102002).respond(0.01)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 102003).respond(0.01)

        # with the same offer id
        cls.index.offers += [
            Offer(hyperid=103, fesh=1, offerid=103000, price=10000, bid=10, ts=103001),  # default
            Offer(hyperid=103, fesh=2, offerid=103000, price=10000, bid=50, ts=103002),  # premium
            Offer(hyperid=103, fesh=3, offerid=103000, price=10000, bid=30, ts=103003),
        ]
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 103001).respond(0.02)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 103002).respond(0.01)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 103003).respond(0.01)

        # with cutprice
        cls.index.offers += [
            Offer(hyperid=104, fesh=1, price=10000, bid=10, ts=104001),  # default
            Offer(hyperid=104, fesh=2, price=10000, bid=1000, ts=104002, is_cutprice=True),
            Offer(hyperid=104, fesh=3, price=10000, bid=30, ts=104003),  # premium
        ]
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 104001).respond(0.02)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 104002).respond(0.01)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 104003).respond(0.01)

        # for threshold tests
        cls.index.offers += [
            Offer(hyperid=105, fesh=1, price=10000, bid=35, ts=105001),  # premium; CPM = 100000 * 35 * 0.01 ~ 35000
            Offer(hyperid=105, fesh=2, price=10000, bid=14, ts=105002),  # default; CPM = 100000 * 14 * 0.02 ~ 28000
        ]
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 105001).respond(0.01)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 105002).respond(0.02)

        # for default threshold multiplier test
        # MinBid = 13, CardCpcCtr = 0.033, OfferCpcCtr = 0.01
        # CpmThreshold = 100000 * MinBid * CardCpcCtr ~ 42900
        # CpmOffer = 100000 * OfferBid * OfferCpcCtr = 1000 * OfferBid
        # To become Premium Offer):
        #       CpmOffer > CpmThreshold * Mult
        #       OfferBid > 42.9 * Mult

        threshold_mult = 2
        offer_bid = int(math.ceil(42.9 * threshold_mult))
        cls.index.offers += [
            Offer(hyperid=106, fesh=1, price=10000, bid=(offer_bid - 1), ts=106001),  # must not become Premium
            Offer(hyperid=107, fesh=1, price=10000, bid=offer_bid, ts=107001),  # must become Premium
        ]
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 106001).respond(0.01)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 107001).respond(0.01)

        # for multiple auction blocks
        cls.index.offers += [
            Offer(hyperid=109, fesh=1, price=10000, bid=60, ts=109001),
            Offer(hyperid=109, fesh=2, price=10000, bid=30, ts=109002),
            Offer(hyperid=109, fesh=4, price=10000, bid=60, ts=109004),
            Offer(hyperid=109, fesh=5, price=10000, bid=30, ts=109005),
        ]

        # the same shop in DO in RO
        cls.index.offers += [
            Offer(hyperid=110, fesh=1, price=10000, bid=10, waremd5="dododododododododododg", ts=110001),  # Default
            Offer(hyperid=110, fesh=1, price=10000, bid=50, waremd5="popopopopopopopopopopg", ts=110002),  # Premium
        ]
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 110001).respond(0.02)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 110002).respond(0.01)

    def requestString(
        self,
        hyperid,
        offers_set,
        flag=None,
        pp=None,
        extra=None,
        threshold=None,
        reqid=None,
        not_duplicate_do_shop=None,
    ):
        req = "place=productoffers&hyperid={}&offers-set={}&show-urls=external".format(hyperid, offers_set)

        if pp is not None:
            req += "&pp={}".format(pp)
        if extra is not None:
            req += "&" + extra
        if reqid is not None:
            req += "&reqid={}".format(reqid)

        rearr = RearrFactors()
        if flag is not None:
            rearr.logic(flag)
        if threshold is not None:
            rearr.threshold_multiplier(threshold)
        if not_duplicate_do_shop is not None:
            rearr.not_duplicate_do_shop(not_duplicate_do_shop)
        return req + str(rearr)

    def request(
        self,
        hyperid,
        offers_set,
        flag=None,
        pp=None,
        extra=None,
        threshold=None,
        reqid=None,
        not_duplicate_do_shop=None,
    ):
        return self.report.request_json(
            self.requestString(hyperid, offers_set, flag, pp, extra, threshold, reqid, not_duplicate_do_shop)
        )

    def check_offer(self, response, shop_id, benefit, pp):
        self.assertFragmentIn(response, {'entity': 'offer', 'shop': {'id': shop_id}, 'benefit': {'type': benefit}})
        self.show_log.expect(shop_id=shop_id, pp=pp)

    def test_no_premium_offer_by_default_in_list(self):
        """Check that there are no 'isPremium' mark in list by default and when it's explicitly disabled"""

        def check(hyperid, offers_set, flag=None):
            response = self.request(hyperid, offers_set, flag, threshold=0)
            self.assertFragmentNotIn(response, {"results": [{"benefit": Absent(), "isPremium": True}]})  # in list

        check(101, "all")
        check(101, "top")
        check(101, "top", RearrFactors.LogicDisabled)
        check(101, "top", RearrFactors.LogicAdd)
        check(101, "default,top")
        check(101, "default,top", RearrFactors.LogicDisabled)
        check(101, "default,top", RearrFactors.LogicAdd)
        check(101, "defaultList,top")
        check(101, "defaultList,top", RearrFactors.LogicDisabled)
        # check(101, "defaultList,top", RearrFactors.LogicAdd)

    def test_premium_offer_on_dedicated_place(self):
        response = self.request(101, "defaultList", RearrFactors.LogicAdd, threshold=0)
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"benefit": {"type": Not(Equal("premium"))}, "shop": {"id": 1}},  # Default Offer
                    {"benefit": {"type": "premium"}, "shop": {"id": 2}, "isPremium": True},  # Premium Offer
                ]
            },
            allow_different_len=False,
            preserve_order=True,
        )

        self.click_log.expect(ClickType.EXTERNAL, cp=13, shop_id=1)  # minimal bid for Default Offer
        self.click_log.expect(ClickType.EXTERNAL, cp=31, shop_id=2, prm=1)  # bid brokered down for Premium Offer
        self.show_log.expect(shop_id=1, is_premium_offer=Absent())
        self.show_log.expect(shop_id=2, is_premium_offer=1)

    def test_premium_offer_is_default(self):
        """Test if Premium Offer isn't added when it's the same as Default Offer"""
        response = self.request(102, "defaultList", RearrFactors.LogicAdd, threshold=0)
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "benefit": {"type": Not(Equal("premium"))},  # default or cheapest
                        "shop": {"id": 1},
                        "isPremium": Absent(),
                    }
                ]
            },
            allow_different_len=False,
            preserve_order=True,
        )
        self.click_log.expect(ClickType.EXTERNAL, cp=13, shop_id=1)  # minimal bid for Default Offer (no auction)
        self.show_log.expect(shop_id=1, is_premium_offer=Absent())

    def test_premium_offer_is_default_nested_benefit_types(self):
        """Проверяем, что если ДО==ПО то в ответе есть поле с всеми бенефитами оффера"""
        response = self.request(102, "defaultList", RearrFactors.LogicAdd, threshold=0)
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {  # Premium is Default Offer
                        "benefit": {
                            "type": Not(Equal("premium")),
                            "nestedTypes": ["cheapest", "premium"],
                        },
                        "shop": {"id": 1},
                        "isPremium": Absent(),
                    }
                ]
            },
            allow_different_len=False,
            preserve_order=True,
        )

    def test_premium_offer_is_not_default_nested_benefit_types(self):
        response = self.request(101, "defaultList", RearrFactors.LogicAdd, threshold=0)
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {  # Default Offer
                        "benefit": {
                            "type": Not(Equal("premium")),
                            "nestedTypes": ["cheapest"],
                        },
                        "shop": {"id": 1},
                    },
                    {  # Premium Offer
                        "benefit": {
                            "type": "premium",
                            "nestedTypes": ["premium"],
                        },
                        "shop": {"id": 2},
                        "isPremium": True,
                    },
                ]
            },
            allow_different_len=False,
            preserve_order=True,
        )

    def test_premium_offer_in_list_only(self):
        offers_sets = ("all", "top", "top,default")
        for offers_set in offers_sets:
            response = self.request(101, offers_set, RearrFactors.LogicMark, threshold=0)
            self.assertFragmentIn(
                response, {"results": [{"benefit": Absent(), "shop": {"id": 2}, "isPremium": True}]}  # in list
            )
        self.show_log.expect(shop_id=2, is_premium_offer=1).times(len(offers_sets))

    def test_premium_offer_in_list_for_touch_base(self):
        """
        Проверяем, что при флаге mark-touch НЕ в таче премиальный в список НЕ добавляется
        """
        offers_sets = ("all", "top", "top,default")
        for offers_set in offers_sets:
            response = self.request(101, offers_set, RearrFactors.LogicMarkTouch, threshold=0)
            self.assertFragmentNotIn(
                response, {"results": [{"benefit": Absent(), "shop": {"id": 2}, "isPremium": True}]}  # in list
            )
        self.show_log.expect(shop_id=2, is_premium_offer=1).times(0)

    def test_premium_offer_in_list_for_touch(self):
        """
        Проверяем, что при флаге mark-touch в таче премиальный в список добавляется
        """
        offers_sets = ("all", "top", "top,default")
        for offers_set in offers_sets:
            response = self.request(101, offers_set, RearrFactors.LogicMarkTouch, threshold=0, extra="touch=1")
            self.assertFragmentIn(
                response, {"results": [{"benefit": Absent(), "shop": {"id": 2}, "isPremium": True}]}  # in list
            )
        self.show_log.expect(shop_id=2, is_premium_offer=1).times(len(offers_sets))

    def test_premium_offer_in_list_and_dedicated_place(self):
        offers_sets = ("all,defaultList", "top,defaultList")
        for offers_set in offers_sets:
            response = self.request(101, offers_set, "add-and-mark", threshold=0)
            self.assertFragmentIn(
                response,
                {
                    "results": [
                        {"benefit": Absent(), "shop": {"id": 2}, "isPremium": True},  # in list
                        {"benefit": {"type": "premium"}, "shop": {"id": 2}, "isPremium": True},  # on dedicated place
                    ]
                },
                preserve_order=True,
            )
        self.show_log.expect(shop_id=2, is_premium_offer=1).times(len(offers_sets) * 2)  # in list & on dedicated place

    def test_premium_offer_in_list_when_its_default(self):
        offers_sets = ("all", "top", "top,default", "top,defaultList")
        for offers_set in offers_sets:
            response = self.request(102, offers_set, RearrFactors.LogicMark, threshold=0)
            self.assertFragmentIn(
                response, {"results": [{"benefit": Absent(), "shop": {"id": 1}, "isPremium": True}]}  # in list
            )
        self.show_log.expect(shop_id=1, is_premium_offer=1).times(len(offers_sets))  # in list only

    def test_offers_with_equal_ids(self):
        offers_sets = ("all,defaultList", "top,defaultList")
        for offers_set in offers_sets:
            response = self.request(103, offers_set, "add-and-mark", threshold=0)
            self.assertFragmentIn(
                response,
                {
                    "results": [
                        {"benefit": Absent(), "shop": {"id": 2}, "isPremium": True},  # in list
                        {"benefit": {"type": "premium"}, "shop": {"id": 2}, "isPremium": True},  # on dedicated place
                    ]
                },
                preserve_order=True,
            )
        self.show_log.expect(shop_id=2, is_premium_offer=1).times(len(offers_sets) * 2)  # in list & on dedicated place

    def test_with_show_cutprice(self):
        response = self.request(103, "top", RearrFactors.LogicMark, extra="&show-cutprice=1", threshold=0)
        self.assertFragmentIn(response, {"results": [{"shop": {"id": 2}, "isPremium": True}]}, preserve_order=True)
        self.show_log.expect(shop_id=2, is_premium_offer=1)

    def test_no_cutprice_as_premium_offer(self):
        offers_sets = ("top", "defaultList,all")
        for offers_set in offers_sets:
            response = self.request(104, offers_set, "add-and-mark", extra="&show-cutprice=1", threshold=0)
            self.assertFragmentIn(
                response,
                {"results": [{"shop": {"id": 3}, "isCutPrice": EqualToOneOfOrAbsent(False), "isPremium": True}]},
            )

    def test_no_premium_offer_for_cutprice(self):
        offers_sets = ("top", "defaultList,all")
        for offers_set in offers_sets:
            response = self.request(104, offers_set, "add-and-mark", extra="&good-state=cutprice", threshold=0)
            self.assertFragmentNotIn(response, {"results": [{"isPremium": True}]})

    def test_premium_offer_on_dedicated_place_for_touch(self):
        """
        MARKETOUT-28922 - теперь премиумный оффер должен попадать в выдачу даже для тача
        """
        response_touch = self.request(101, "defaultList", flag=RearrFactors.LogicAdd, extra="&touch=1", threshold=0)
        self.assertFragmentIn(response_touch, {"results": [{"isPremium": True}]})

        """
        Проверяем, что флаг touch не влияет на выдачу в данном случае
        """
        self.assertEqualJsonResponses(
            self.requestString(101, "defaultList", flag=RearrFactors.LogicAdd, extra="&touch=1", threshold=0),
            self.requestString(101, "defaultList", flag=RearrFactors.LogicAdd, threshold=0),
            None,
            True,
        )

    def test_premium_offer_pp(self):
        """Check if Premium Offer overrides incoming pp properly
        see MARKETOUT-19533, MARKETOUT-26945 for pp
        """

        # touch
        for pp in (46, 606, 613):
            response = self.request(101, "defaultList", "add", pp, threshold=0)
            self.check_offer(response, shop_id=2, benefit="premium", pp=610)

        """
        MARKETOUT-28922 - проверяем разные pp в ответе в зависимости от pp в реквесте, и то что бенефит примиальный
        """
        response = self.request(101, "defaultList", "add", 601, threshold=0)
        self.check_offer(response, shop_id=2, benefit="premium", pp=656)

        response = self.request(101, "defaultList", "add", 602, threshold=0)
        self.check_offer(response, shop_id=2, benefit="premium", pp=658)

        response = self.request(101, "defaultList", "add", 603, threshold=0)
        self.check_offer(response, shop_id=2, benefit="premium", pp=657)

        response = self.request(101, "defaultList", "add", 604, threshold=0)
        self.check_offer(response, shop_id=2, benefit="premium", pp=659)

        # android
        for pp in (706, 713, 721):
            response = self.request(101, "defaultList", "add", pp, threshold=0)
            self.check_offer(response, shop_id=2, benefit="premium", pp=710)

        # ios
        for pp in (806, 813, 821):
            response = self.request(101, "defaultList", "add", pp, threshold=0)
            self.check_offer(
                response,
                shop_id=2,
                benefit="premium",
                pp=810,
            )

        # desktop
        for pp_in, pp_out in ((21, 241), (61, 246), (63, 247), (62, 248), (64, 249)):
            response = self.request(101, "defaultList", "add", pp_in, threshold=0)
            self.check_offer(
                response,
                shop_id=2,
                benefit="premium",
                pp=pp_out,
            )

        # widgets
        for pp_in, pp_out in (
            (901, 931),
            (902, 932),
            (903, 933),
            (904, 934),
            (908, 935),
            (910, 936),
            (917, 937),
            (914, 937),
            (928, 937),
            (918, 938),
            (919, 939),
        ):
            response = self.request(101, "defaultList", "add", pp_in, threshold=0)
            self.check_offer(
                response,
                shop_id=2,
                benefit="premium",
                pp=pp_out,
            )

        # advisor
        for pp_in, pp_out in ((484, 484),):
            response = self.request(101, "defaultList", "add", pp_in, threshold=0)
            self.check_offer(
                response,
                shop_id=2,
                benefit="premium",
                pp=pp_out,
            )

    def test_threshold_for_premium_offer(self):
        # MinBid = 13, CpcCtr = 0.033
        # CpmThreshold = 100000 * MinBid * CpcCtr ~ 42900
        # CpmOffer ~ 35000
        # To become Premium Offer: CpmOffer > CpmThreshold * Mult; Mult < CpmOffer / CpmThreshold ~ 0.816

        # on dedicated place
        response = self.request(105, "defaultList", flag=RearrFactors.LogicAdd, threshold=0.815)
        self.assertFragmentIn(response, {"results": [{"shop": {"id": 1}, "isPremium": True}]})

        response = self.request(105, "defaultList", flag=RearrFactors.LogicAdd, threshold=0.817)
        self.assertFragmentNotIn(response, {"results": [{"shop": {"id": 1}, "isPremium": True}]})

        # on Top place
        response = self.request(105, "top", flag=RearrFactors.LogicMark, threshold=0.815)
        self.assertFragmentIn(response, {"results": [{"shop": {"id": 1}, "isPremium": True}]})

        response = self.request(105, "top", flag=RearrFactors.LogicMark, threshold=0.817)
        self.assertFragmentNotIn(response, {"results": [{"shop": {"id": 1}, "isPremium": True}]})

    def test_default_threshold_for_premium_offer(self):
        """Check that multiplier of CPM threshold for Premium Offer is correct (=2)"""

        # offer for model 106 has bid lower than threshold
        response = self.request(106, "top", flag="add-and-mark")
        self.assertFragmentIn(response, {"results": [{"shop": {"id": 1}, "isPremium": EqualToOneOfOrAbsent(False)}]})

        # offer for model 107 has bid higher than threshold
        response = self.request(107, "top", flag="add-and-mark")
        self.assertFragmentIn(response, {"results": [{"shop": {"id": 1}, "isPremium": True}]})

    def test_bids_recommender_without_threshold(self):
        """ """

        request = (
            "place=bids_recommender&fesh=1&rids=0&hyperid=105&type=card&rearr-factors=market_uncollapse_supplier=0"
        )

        # check without CPM threshold
        response = self.report.request_xml(request + str(RearrFactors().threshold_multiplier(0)))
        self.assertFragmentIn(
            response,
            '''
            <card-top-recommendations>
                <position bid="29" pos="1"/>
                <position bid="13" pos="2"/>
            </card-top-recommendations>
        ''',
            preserve_order=True,
        )

        # check with CPM threshold
        # MinBid = 13, CardCpcCtr = 0.033, OfferCpcCtr = 0.01
        # CpmThreshold = 100000 * MinBid * CardCpcCtr ~ 42900
        # CpmOffer = 100000 * OfferBid * OfferCpcCtr = 1000 * OfferBid
        # To get top-1 (i.e. become Premium Offer):
        #       CpmOffer > CpmThreshold * Mult
        #       OfferBid > 42.9 * Mult

        # OfferBid = 43 > 42.9 * 1
        response = self.report.request_xml(request + str(RearrFactors().threshold_multiplier(1)))
        self.assertFragmentIn(
            response,
            '''
            <card-top-recommendations>
                <position bid="43" pos="1"/>
                <position bid="13" pos="2"/>
            </card-top-recommendations>
        ''',
            preserve_order=True,
        )

        # OfferBid = 86 > 42.9 * 2
        response = self.report.request_xml(request + str(RearrFactors().threshold_multiplier(2)))
        self.assertFragmentIn(
            response,
            '''
            <card-top-recommendations>
                <position bid="86" pos="1"/>
                <position bid="13" pos="2"/>
            </card-top-recommendations>
        ''',
            preserve_order=True,
        )

    def test_on_touch__low_cpm(self):
        """Top-1 offer has too low CPM to get Premium - it's bid is broken down as usual."""

        response = self.request(101, "top", threshold=2, flag=RearrFactors.LogicMark)
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"shop": {"id": 2}, "isPremium": EqualToOneOfOrAbsent(False)},
                    {"shop": {"id": 3}, "isPremium": EqualToOneOfOrAbsent(False)},
                ]
            },
            preserve_order=True,
        )
        self.show_log.expect(shop_id=2, is_premium_offer=Absent(), click_price=31)
        self.show_log.expect(shop_id=3, is_premium_offer=Absent(), click_price=27)

    def test_on_touch__got_premium(self):
        """Top-1 offer enough CPM to get Premium - it's bid is broken down to CPM threshold"""

        # CpmThreshold ~ 42900; OfferCpm >= CpmThreshold; OfferCpm = OfferBid * 100000 * 0.01
        # OfferBid >= CpmThreshold/(100000 * 0.01) = 42.9

        response = self.request(101, "top", threshold=1, flag=RearrFactors.LogicMark)
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"shop": {"id": 2}, "isPremium": True},
                    {"shop": {"id": 3}, "isPremium": EqualToOneOfOrAbsent(False)},
                ]
            },
            preserve_order=True,
        )
        self.show_log.expect(shop_id=2, is_premium_offer=1, click_price=43)
        self.show_log.expect(shop_id=3, is_premium_offer=Absent(), click_price=27)

    def test_click_logs(self):
        # on dedicated place
        self.request(101, "defaultList", flag=RearrFactors.LogicAdd, threshold=0, reqid="1")
        self.click_log.expect(ClickType.EXTERNAL, reqid="1", shop_id=2, prm=1)

        # on Top place
        self.request(101, "top", flag=RearrFactors.LogicMark, threshold=0, reqid="2")
        self.click_log.expect(ClickType.EXTERNAL, reqid="2", shop_id=2, prm=1)

    def test_multiple_offers_block(self):
        response = self.request(109, "top", flag=RearrFactors.LogicMark, threshold=0, extra="rids=213")
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"shop": {"id": 1}, "isPremium": True},
                    {"shop": {"id": 2}, "isPremium": EqualToOneOfOrAbsent(False)},
                    {"entity": "regionalDelimiter"},
                    {"shop": {"id": 4}, "isPremium": EqualToOneOfOrAbsent(False)},
                    {"shop": {"id": 5}, "isPremium": EqualToOneOfOrAbsent(False)},
                ]
            },
        )

    def test_restrict_premium_on_touch(self):
        """See MARKETOUT-25951 - Premium Offer may only be returned for РР=46 (and 18 for debug purposes)"""

        for pp in (18, 46):
            response = self.request(101, "top", threshold=0, flag=RearrFactors.LogicMark, pp=pp)
            self.assertFragmentIn(response, {"results": [{"isPremium": True}]})
        for pp in (601, 602, 604):
            response = self.request(101, "top", threshold=0, flag=RearrFactors.LogicMark, pp=pp)
            self.assertFragmentNotIn(response, {"results": [{"isPremium": True}]})

    def test_pp_for_premium_on_touch(self):
        """See MARKETOUT-25951 - Premium Offer has pp=610 on touch
        but top-6 offers on touch has pp=46
        """

        # top-1 has pp=46 if it's Premium Offer
        self.request(101, "top", threshold=0, flag=RearrFactors.LogicMark, pp=46)
        self.show_log_tskv.expect(shop_id=2, pp=46)
        self.show_log_tskv.expect(shop_id=3, pp=46)
        self.click_log.expect(shop_id=2, pp=46)
        self.click_log.expect(shop_id=3, pp=46)

        # top-1 has original pp if it's not Premium Offer
        self.request(101, "top", threshold=1000, flag=RearrFactors.LogicMark, pp=46)
        self.show_log_tskv.expect(shop_id=2, pp=46)
        self.show_log_tskv.expect(shop_id=3, pp=46)
        self.click_log.expect(shop_id=2, pp=46)
        self.click_log.expect(shop_id=3, pp=46)


if __name__ == '__main__':
    main()
