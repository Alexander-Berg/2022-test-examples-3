#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import ClickType, Const, HybridAuctionParam, MarketSku, MnPlace, Offer
from core.testcase import TestCase, main
from core.matcher import Not, Equal, Absent, EqualToOneOfOrAbsent, NoKey, NotEmptyList
import math


class RearrFactors:
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


def one_hyperid_results_maker(offers_lists):
    result = {"search": {"results": offers_lists[0]}}
    return result


def multiple_hyperid_results_maker(offers_lists, model_ids):
    result = {
        "modelIdWithOffers": [
            {"model_id": model_id, "offers": offers} for offers, model_id in zip(offers_lists, model_ids)
        ]
    }
    return result


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ["market_money_disable_bids=0"]
        cls.index.offers += [
            Offer(hyperid=3001, descr='a'),
            Offer(hyperid=3001, descr='b'),
            Offer(hyperid=1101, descr='c'),
            Offer(hyperid=1101, descr='d'),
        ]

        # https://st.yandex-team.ru/MARKETOUT-27536
        cls.index.offers += [
            Offer(hyperid=2001, price=4000, ts=1, bid=200, title="2001_1"),
            Offer(hyperid=2001, price=6000, ts=2, bid=150, title="2001_2"),
            Offer(hyperid=2001, price=5000, ts=3, bid=250, title="2001_3"),
            Offer(hyperid=2002),
        ]

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 1).respond(0.1)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 2).respond(0.3)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 3).respond(0.2)

        cls.index.offers += [
            Offer(hyperid=2003, price=4000, title="2003_1"),
            Offer(hyperid=2003, price=6000, title="2003_2"),
            Offer(hyperid=2003, price=5000, title="2003_3"),
            Offer(hyperid=2004),
        ]

        cls.index.offers += [
            Offer(hyperid=2005, ts=11, title="2005_1"),
            Offer(hyperid=2005, ts=12, title="2005_2"),
            Offer(hyperid=2005, ts=13, title="2005_3"),
            Offer(hyperid=2006),
        ]

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 11).respond(0.5)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 12).respond(0.3)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 13).respond(0.6)

        cls.index.offers += [
            Offer(hyperid=2007, price=10000, fesh=1, bid=10, ts=21, title="2007_1"),  # default
            Offer(hyperid=2007, price=10000, fesh=2, bid=50, ts=22, title="2007_2"),  # premium
            Offer(hyperid=2007, price=10000, fesh=3, bid=30, ts=23, title="2007_3"),
            Offer(hyperid=2008),
        ]
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 21).respond(0.02)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 22).respond(0.01)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 23).respond(0.01)

        cls.index.offers += [
            Offer(hyperid=2009, price=10000, fesh=1, bid=30, ts=31, title="2009_1"),  # default & premium
            Offer(hyperid=2009, price=10000, fesh=2, bid=25, ts=32, title="2009_2"),
            Offer(hyperid=2009, price=10000, fesh=3, bid=25, ts=33, title="2009_3"),
            Offer(hyperid=2010),
        ]
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 31).respond(0.02)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 32).respond(0.01)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 33).respond(0.01)

        # for threshold tests
        cls.index.hybrid_auction_settings += [
            HybridAuctionParam(Const.ROOT_HID),
            HybridAuctionParam(category=1, cpc_ctr_for_cpc=0.033),
        ]
        cls.index.offers += [
            Offer(hyperid=2011, hid=1, fesh=1, price=10000, bid=35, ts=41),  # premium; CPM = 100000 * 35 * 0.01 ~ 35000
            Offer(hyperid=2011, hid=1, fesh=2, price=10000, bid=14, ts=42),  # default; CPM = 100000 * 14 * 0.02 ~ 28000
            Offer(hyperid=2012),
        ]
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 41).respond(0.01)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 42).respond(0.02)

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
            Offer(hyperid=2013, hid=1, fesh=1, price=10000, bid=(offer_bid - 1), ts=51),  # must not become Premium
            Offer(hyperid=2014, hid=1, fesh=1, price=10000, bid=offer_bid, ts=52),  # must become Premium
        ]
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 51).respond(0.01)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 52).respond(0.01)

    def request(self, hyperid, use_multiple_hyperid=False, offers_set=None, flag=None, threshold=None, how=None):
        req = "place=productoffers"

        if offers_set is not None:
            req += "&offers-set={}".format(offers_set)
        if use_multiple_hyperid:
            req += "&use_multiple_hyperid=1&hyperid={}".format(','.join(map(str, hyperid)))
        else:
            req += "&hyperid={}".format(hyperid)
        if how is not None:
            req += "&how={}".format(how)

        rearr = RearrFactors()
        if flag is not None:
            rearr.logic(flag)
        if threshold is not None:
            rearr.threshold_multiplier(threshold)
        return self.report.request_json(req + str(rearr))

    def base_multiple_hyperid_format_check(self, response, results_maker, offers_lists, model_ids):
        self.assertFragmentIn(response, results_maker(offers_lists, model_ids))

        for offers in offers_lists:
            for offer in offers:
                self.assertEqual(1, response.count(offer))

    def test_multiple_hyperid_format(self):
        model_3001_expected = [{"entity": "offer", "description": "a"}, {"entity": "offer", "description": "b"}]
        model_1101_expected = [{"entity": "offer", "description": "c"}, {"entity": "offer", "description": "d"}]
        response = self.request([3001, 1101], True)
        self.base_multiple_hyperid_format_check(
            response, multiple_hyperid_results_maker, [model_3001_expected, model_1101_expected], [3001, 1101]
        )

        response = self.request([3001], True)
        self.base_multiple_hyperid_format_check(response, multiple_hyperid_results_maker, [model_3001_expected], [3001])

    def base_default_order_check(self, response, results_maker, **params):
        expected = [
            {"entity": "offer", "titles": {"raw": "2001_3"}},
            {"entity": "offer", "titles": {"raw": "2001_2"}},
            {"entity": "offer", "titles": {"raw": "2001_1"}},
        ]
        self.assertFragmentIn(response, results_maker([expected], **params), preserve_order=True)
        self.click_log.expect(ClickType.EXTERNAL, cp=226, position=1)
        self.click_log.expect(ClickType.EXTERNAL, cp=67, position=2)
        self.click_log.expect(ClickType.EXTERNAL, cp=6, position=3)

    def test_one_hyperid_default_order(self):
        response = self.request(2001)
        self.base_default_order_check(response, one_hyperid_results_maker)

    def test_multiple_hyperid_default_order(self):
        response = self.request([2001, 2002], True)
        self.base_default_order_check(response, multiple_hyperid_results_maker, model_ids=[2001])

    def base_price_sort_order_check(self, response, results_maker, **params):
        expected = [
            {"entity": "offer", "titles": {"raw": "2003_1"}},
            {"entity": "offer", "titles": {"raw": "2003_3"}},
            {"entity": "offer", "titles": {"raw": "2003_2"}},
        ]
        self.assertFragmentIn(response, results_maker([expected], **params), preserve_order=True)
        self.click_log.expect(ClickType.EXTERNAL, cp=6, min_bid=6, position=1)
        self.click_log.expect(ClickType.EXTERNAL, cp=6, min_bid=6, position=2)
        self.click_log.expect(ClickType.EXTERNAL, cp=6, min_bid=6, position=3)

    def test_one_hyperid_price_sort_order(self):
        response = self.request(2003, how='aprice')
        self.base_price_sort_order_check(response, one_hyperid_results_maker)

    def test_multiple_hyperid_price_sort_order(self):
        response = self.request([2003, 2004], True, how='aprice')
        self.base_price_sort_order_check(response, multiple_hyperid_results_maker, model_ids=[2003])

    def base_price_default_offer_choice_check(self, response, results_maker, **params):
        expected = [{"entity": "offer", "titles": {"raw": "2005_3"}, "benefit": {"type": "default"}}]
        self.assertFragmentIn(response, results_maker([expected], **params))

    def test_one_hyperid_default_offer_choice(self):
        response = self.request(2005, offers_set='default')
        self.base_price_default_offer_choice_check(response, one_hyperid_results_maker)

    def test_multiple_hyperid_default_offer_choice(self):
        response = self.request([2005, 2006], True, offers_set='default')
        self.base_price_default_offer_choice_check(response, multiple_hyperid_results_maker, model_ids=[2005])

    def base_premium_offer_by_default_on_dedicated_place_check(self, response, results_maker, **params):
        """Check that there is Premium Offer by default and when it's explicitly enabled"""

        expected_benefit = [{"benefit": {"type": "premium"}}]
        expected_is_premium = [{"isPremium": True}]
        self.assertFragmentIn(response, results_maker([expected_benefit], **params))
        self.assertFragmentIn(response, results_maker([expected_is_premium], **params))
        self.show_log.expect(is_premium_offer=1)

    def test_one_hyperid_premium_offer_by_default_on_dedicated_place(self):
        for flag in ['add']:
            response = self.request(2007, offers_set='defaultList', flag=flag)
            self.base_premium_offer_by_default_on_dedicated_place_check(response, one_hyperid_results_maker)

    def test_multiple_hyperid_premium_offer_by_default_on_dedicated_place(self):
        for flag in ['add']:
            response = self.request([2007, 2008], True, offers_set='defaultList', flag=flag)
            self.base_premium_offer_by_default_on_dedicated_place_check(
                response, multiple_hyperid_results_maker, model_ids=[2007]
            )

    def base_no_premium_offer_by_default_in_list_check(self, response, results_maker, **params):
        """Check that there are no 'isPremium' mark in list by default and when it's explicitly disabled"""

        not_expected = [{"benefit": Absent(), "isPremium": True}]
        self.assertFragmentNotIn(response, results_maker([not_expected], **params))

    def test_one_hyperid_no_premium_offer_by_default_in_list_check(self):
        for offers_set, flag in zip(
            [
                'all',
                'top',
                'top',
                'top',
                'default,top',
                'default,top',
                'default,top',
                'defaultList,top',
                'defaultList,top',
            ],
            [None, None, 'disabled', 'add', None, 'disabled', 'add', None, 'disabled'],
        ):
            response = self.request(2007, offers_set=offers_set, flag=flag)
            self.base_no_premium_offer_by_default_in_list_check(response, one_hyperid_results_maker)

    def test_multiple_hyperid_no_premium_offer_by_default_in_list_check(self):
        for offers_set, flag in zip(
            [
                'all',
                'top',
                'top',
                'top',
                'default,top',
                'default,top',
                'default,top',
                'defaultList,top',
                'defaultList,top',
            ],
            [None, None, 'disabled', 'add', None, 'disabled', 'add', None, 'disabled'],
        ):
            response = self.request([2007, 2008], True, offers_set=offers_set, flag=flag)
            self.base_no_premium_offer_by_default_in_list_check(
                response, multiple_hyperid_results_maker, model_ids=[2007]
            )

    def base_premium_offer_on_dedicated_place_check(self, response, results_maker, **params):
        expected = [
            {"benefit": {"type": Not(Equal("premium"))}, "shop": {"id": 1}},  # Default Offer
            {"benefit": {"type": "premium"}, "shop": {"id": 2}, "isPremium": True},  # Premium Offer
        ]

        self.assertFragmentIn(response, results_maker([expected, [{}]], **params), preserve_order=True)

        self.click_log.expect(ClickType.EXTERNAL, cp=13, shop_id=1)  # minimal bid for Default Offer
        self.click_log.expect(ClickType.EXTERNAL, cp=31, shop_id=2, prm=1)  # bid brokered down for Premium Offer
        self.show_log.expect(shop_id=1, is_premium_offer=Absent())
        self.show_log.expect(shop_id=2, is_premium_offer=1)

    def test_one_hyperid_premium_offer_on_dedicated_place(self):
        response = self.request(2007, offers_set="defaultList", flag='add', threshold=0)
        self.base_premium_offer_on_dedicated_place_check(response, one_hyperid_results_maker)

    def test_multiple_hyperid_premium_offer_on_dedicated_place(self):
        response = self.request([2007, 2008], True, offers_set="defaultList", flag='add', threshold=0)
        self.base_premium_offer_on_dedicated_place_check(
            response, multiple_hyperid_results_maker, model_ids=[2007, 2008]
        )

    def base_premium_offer_is_default_check(self, response, results_maker, **params):
        """Test if Premium Offer isn't added when it's the same as Default Offer"""
        expected = [
            {
                "benefit": {"type": Not(Equal("premium"))},  # default or cheapest
                "shop": {"id": 1},
                "isPremium": Absent(),
            }
        ]
        self.assertFragmentIn(
            response, results_maker([expected, [{}]], **params), allow_different_len=False, preserve_order=True
        )
        self.click_log.expect(ClickType.EXTERNAL, cp=13, shop_id=1)  # minimal bid for Default Offer (no auction)
        self.show_log.expect(shop_id=1, is_premium_offer=Absent())

    def test_one_hyperid_premium_offer_is_default(self):
        response = self.request(2009, offers_set="defaultList", flag='add', threshold=0)
        self.base_premium_offer_is_default_check(response, one_hyperid_results_maker)

    def test_multiple_hyperid_premium_offer_is_default(self):
        response = self.request([2009, 2010], True, offers_set="defaultList", flag='add', threshold=0)
        self.base_premium_offer_is_default_check(response, multiple_hyperid_results_maker, model_ids=[2009, 2010])

    def base_threshold_for_premium_offer_check(self, response, results_maker, contain, **params):
        expected = [{"shop": {"id": 1}, "isPremium": True}]
        if contain:
            self.assertFragmentIn(response, results_maker([expected], **params))
        else:
            self.assertFragmentNotIn(response, results_maker([expected], **params))

    def test_one_hyperid_threshold_for_premium_offer(self):
        # MinBid = 13, CpcCtr = 0.001
        # CpmThreshold = 100000 * MinBid * CpcCtr ~ 1300
        # CpmOffer ~ 35000
        # To become Premium Offer: CpmOffer > CpmThreshold * Mult; Mult < CpmOffer / CpmThreshold ~ 26.923

        response = self.request(2011, offers_set="defaultList", flag='add', threshold=0.815)
        self.base_threshold_for_premium_offer_check(response, one_hyperid_results_maker, True)
        response = self.request(2011, offers_set="defaultList", flag='add', threshold=0.817)
        self.base_threshold_for_premium_offer_check(response, one_hyperid_results_maker, False)
        response = self.request(2011, offers_set="top", flag='mark', threshold=0.815)
        self.base_threshold_for_premium_offer_check(response, one_hyperid_results_maker, True)
        response = self.request(2011, offers_set="top", flag='mark', threshold=0.817)
        self.base_threshold_for_premium_offer_check(response, one_hyperid_results_maker, False)

    def test_multiple_hyperid_threshold_for_premium_offer(self):
        response = self.request([2011, 2012], True, offers_set="defaultList", flag='add', threshold=0.815)
        self.base_threshold_for_premium_offer_check(response, multiple_hyperid_results_maker, True, model_ids=[2011])
        response = self.request([2011, 2012], True, offers_set="defaultList", flag='add', threshold=0.817)
        self.base_threshold_for_premium_offer_check(response, multiple_hyperid_results_maker, False, model_ids=[2011])
        response = self.request([2011, 2012], True, offers_set="top", flag='mark', threshold=0.815)
        self.base_threshold_for_premium_offer_check(response, multiple_hyperid_results_maker, True, model_ids=[2011])
        response = self.request([2011, 2012], True, offers_set="top", flag='mark', threshold=0.817)
        self.base_threshold_for_premium_offer_check(response, multiple_hyperid_results_maker, False, model_ids=[2011])

    def base_default_threshold_for_premium_offer_check(self, response, results_maker, premium, **params):
        expected = [{"shop": {"id": 1}, "isPremium": EqualToOneOfOrAbsent(False)}]
        if premium:
            expected[0]["isPremium"] = True
        self.assertFragmentIn(response, results_maker([expected], **params))

    def test_one_hyperid_default_threshold_for_premium_offer(self):
        """Check that multiplier of CPM threshold for Premium Offer is correct (=2)"""

        # offer for model 2013 has bid lower than threshold
        response = self.request(2013, offers_set="top", flag="add-and-mark")
        self.base_default_threshold_for_premium_offer_check(response, one_hyperid_results_maker, False)

        # offer for model 2014 has bid higher than threshold
        # offer for model 2014 has bid higher than threshold
        response = self.request(2014, offers_set="top", flag="add-and-mark")
        self.base_default_threshold_for_premium_offer_check(response, one_hyperid_results_maker, True)

    def test_multiple_hyperid_default_threshold_for_premium_offer(self):
        response = self.request([2013, 2014], True, offers_set="top", flag="add-and-mark")
        self.base_default_threshold_for_premium_offer_check(
            response, multiple_hyperid_results_maker, False, model_ids=[2013]
        )
        self.base_default_threshold_for_premium_offer_check(
            response, multiple_hyperid_results_maker, True, model_ids=[2014]
        )

    def base_default_premium_top_together_check(self, response, results_maker, **params):
        expected = [
            {"benefit": {"type": Not(Equal("premium"))}, "shop": {"id": 1}},  # Default Offer
            {"benefit": {"type": "premium"}, "shop": {"id": 2}, "isPremium": True},  # Premium Offer
            {"shop": {"id": 3}},
        ]
        self.assertFragmentIn(response, results_maker([expected], **params))

    def test_one_hyperid_default_premium_top_together(self):
        response = self.request(2007, offers_set="defaultList,top", flag="add", threshold=0)
        self.base_default_premium_top_together_check(response, one_hyperid_results_maker)

    def test_multiple_hyperid_default_premium_top_together(self):
        response = self.request([2007, 2008], True, offers_set="defaultList,top", flag="add", threshold=0)
        self.base_default_premium_top_together_check(response, multiple_hyperid_results_maker, model_ids=[2007])

    @classmethod
    def prepare_multiple_msku(cls):

        cls.index.mskus += [
            MarketSku(hyperid=49876, sku=49876001),
            MarketSku(hyperid=49876, sku=49876002),
            MarketSku(hyperid=49877, sku=49877001),
            MarketSku(hyperid=49877, sku=49877002),
        ]

        cls.index.offers += [
            Offer(hyperid=49870),
            Offer(hyperid=49876, sku=49876001),
            Offer(hyperid=49876, sku=49876002),
            Offer(hyperid=49877, sku=49877001),
            Offer(hyperid=49877, sku=49877002),
        ]

        cls.index.offers += [
            Offer(virtual_model_id=200000000000001),  # виртуальная модель
            Offer(virtual_model_id=200000000000002, sku=200000000000002),  # виртуальная ску
            Offer(virtual_model_id=200000000000003, sku=200000000000003),  # виртуальная ску
        ]

    def test_productoffers_multiple_msku(self):
        """Проверяем что запрос с несколькими msku и несколькими hyperid
        отдает на выдачу отдельно результат для каждого hyperid и для каждой msku"""

        response = self.report.request_json(
            'place=productoffers&use_multiple_hyperid=1'
            '&hyperid=49870&hyperid=49876&hyperid=200000000000001'
            '&market-sku=49876001&market-sku=49877001&market-sku=200000000000002'
        )
        self.assertFragmentIn(
            response,
            {
                'modelIdWithOffers': [
                    {'model_id': 49870, 'marketSku': NoKey('marketSku'), 'offers': NotEmptyList()},
                    {'model_id': 49876, 'marketSku': NoKey('marketSku'), 'offers': NotEmptyList()},
                    {'model_id': 200000000000001, 'marketSku': NoKey('marketSku'), 'offers': NotEmptyList()},
                    {'model_id': 49876, 'marketSku': '49876001', 'offers': NotEmptyList()},
                    {'model_id': 49877, 'marketSku': '49877001', 'offers': NotEmptyList()},
                    {'model_id': 200000000000002, 'marketSku': '200000000000002', 'offers': NotEmptyList()},
                ]
            },
            preserve_order=False,
            allow_different_len=False,
        )


if __name__ == '__main__':
    main()
