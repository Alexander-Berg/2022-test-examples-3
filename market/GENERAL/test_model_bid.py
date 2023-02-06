#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.testcase import TestCase, main
from core.types import (
    DynamicVendorModelBid,
    DynamicVendorOfferBid,
    HyperCategory,
    HyperCategoryType,
    MnPlace,
    Model,
    Offer,
    Region,
    Shop,
    VCluster,
)
from core.matcher import Absent, Contains

import math


class T(TestCase):
    @classmethod
    def prepare_model_bid(cls):
        """Заведем несколько моделей с ставки ..."""
        cls.settings.default_search_experiment_flags += ["market_money_disable_bids=0"]
        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']

        cls.index.models += [
            Model(hyperid=307, hid=1, title='iphone0', vbid=10, datasource_id=1),
            Model(hyperid=308, hid=1, title='iphone42', vbid=17, datasource_id=2),
            Model(hyperid=407, hid=1, title='iphone100', datasource_id=1),
            Model(hyperid=408, hid=1, title='iphone142', datasource_id=1),
        ]

    def _test_model_bid(self, text, model_id, vbid=0, datasource_id=1, add_rearr=''):
        """... и проверим, что у этих моделек в дебаг выдаче будут соответствующие ставки"""
        response = self.report.request_json(
            'place=prime&debug=da&rearr-factors=market_force_use_vendor_bid=1&text='
            + text
            + '&rearr-factors=disable_panther_quorum=0'
            + add_rearr
        )

        # это означает, что мы можем пользоваться модельной ставкой на метапоиске
        self.assertFragmentIn(
            response, {"debug": {"sale": {"vBid": vbid}, "tech": {"vendorDatasourceId": datasource_id}}}
        )

        # это означает, что мы можем пользоваться модельной ставкой на базовом
        # (фактором точно можно пользоваться на базовом)

        self.assertFragmentIn(response, {"debug": {"factors": {"VBID": str(vbid) if vbid else Absent()}}})

        # и в фичалог фактор тоже записывается!
        self.feature_log.expect(model_id=model_id, vbid=vbid or Absent())

    def test_model_bid(self):
        self._test_model_bid("iphone0", model_id=307, vbid=10)
        self._test_model_bid("iphone42", model_id=308, vbid=17, datasource_id=2)

        self._test_model_bid("iphone100", model_id=407)
        self._test_model_bid("iphone142", model_id=408)

        # Проверим, что эксперимент по отключение вендорских ставок на оффер не аффекти ставки на модель
        self._test_model_bid("iphone0", model_id=307, vbid=10, add_rearr=';market_disable_vendor_offer_bids=1')
        self._test_model_bid(
            "iphone42", model_id=308, vbid=17, datasource_id=2, add_rearr=';market_disable_vendor_offer_bids=1'
        )

    def test_model_bid_with_text(self):
        """
        Проверяем, что на текстовом поиске вендорная ставка для моделей по умолчанию используется
        но может быть отключена флагом market_use_vendor_bid_for_models_on_text_search=0
        """
        response = self.report.request_json(
            'place=prime&debug=da&text=iphone0' '&rearr-factors=disable_panther_quorum=0'
        )

        # vbid у модели не 0, но в выдаче оказывается 0
        self.assertFragmentIn(response, {"id": 307, "debug": {"sale": {"vBid": 10}}})

        response = self.report.request_json(
            'place=prime&debug=da&text=iphone0'
            '&rearr-factors=disable_panther_quorum=0;market_use_vendor_bid_for_models_on_text_search=0'
        )

        # vbid у модели не 0, но в выдаче оказывается 0
        self.assertFragmentIn(response, {"id": 307, "debug": {"sale": {"vBid": 0}}})

    def test_show_log(self):
        """
        Проверяем, что в логе показов есть признак того, могла ли использоваться вендорная ставка на модель в этом запросе.
        """
        # текстовый запрос, флаг отключает ставку для модели - ставка не должна использоваться
        _ = self.report.request_json(
            'place=prime&debug=da&text=iphone0&reqid=1'
            '&rearr-factors=disable_panther_quorum=0;market_use_vendor_bid_for_models_on_text_search=0'
        )
        self.show_log.expect(original_query='iphone0', use_vendor_bid_for_models=0, reqid=1)

        # текстовый запрос - ставка по умолчанию должна использоваться
        _ = self.report.request_json(
            'place=prime&debug=da&text=iphone0&reqid=2' '&rearr-factors=disable_panther_quorum=0;'
        )
        self.show_log.expect(original_query='iphone0', use_vendor_bid_for_models=1, reqid=2)

        # бестекстовый запрос, флаг отключает ставку для модели - ставка не должна использоваться
        _ = self.report.request_json(
            'place=prime&debug=da&hid=1&reqid=3'
            '&rearr-factors=disable_panther_quorum=0;market_use_vendor_bid_for_models_on_empty_search=0'
        )
        self.show_log.expect(original_query='', use_vendor_bid_for_models=0, reqid=3)

        # бестекстовый запрос - ставка должна использоваться
        _ = self.report.request_json('place=prime&debug=da&hid=1&reqid=4' '&rearr-factors=disable_panther_quorum=0')
        self.show_log.expect(original_query='', use_vendor_bid_for_models=1, reqid=4)

    @classmethod
    def prepare_model_bid_auction(cls):
        cls.matrixnet.on_default_place(MnPlace.BASE_SEARCH).respond(0.1)

        cls.index.regiontree += [
            Region(rid=213, name='Москва'),
        ]

        cls.index.hypertree += [
            HyperCategory(hid=10, output_type=HyperCategoryType.GURU),
            HyperCategory(hid=100, output_type=HyperCategoryType.CLUSTERS, visual=True),
        ]

        cls.index.shops += [
            Shop(fesh=1, priority_region=213, regions=[225]),
            Shop(fesh=2, priority_region=213, regions=[225]),
            Shop(fesh=3, priority_region=213, regions=[225]),
            Shop(fesh=4, priority_region=213, regions=[225]),
            Shop(fesh=5, priority_region=213, regions=[225]),
            Shop(fesh=6, priority_region=213, regions=[225]),
            Shop(fesh=7, priority_region=213, regions=[225]),
            Shop(fesh=8, priority_region=213, regions=[225]),
        ]

        cls.index.models += [
            Model(hyperid=1000, hid=10, title='iphone', vbid=10, ts=1, model_clicks=1000),
            Model(hyperid=1001, hid=10, title='iphone', vbid=8, ts=2, model_clicks=1000),
            Model(hyperid=1002, hid=10, title='iphone', vbid=0, ts=3, model_clicks=1000),
            Model(hyperid=1003, hid=10, title='iphone', ts=4, model_clicks=1000),
            Model(
                hyperid=1004, hid=10, title='iphone', vbid=1, ts=23, model_clicks=1000
            ),  # ставка должна подняться до новой минимальной
        ]

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 1).respond(0.4)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 2).respond(0.3)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 3).respond(0.2)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 4).respond(0.1)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 23).respond(0.5)

        cls.index.vclusters += [
            VCluster(title='iphone cluster', hid=100, vclusterid=1000000001, vbid=10, ts=5),
            VCluster(title='iphone cluster', hid=100, vclusterid=1000000002, vbid=8, ts=6),
            VCluster(title='iphone cluster', hid=100, vclusterid=1000000003, ts=7),
            VCluster(title='iphone cluster', hid=100, vclusterid=1000000004, ts=8),
        ]

        cls.index.offers += [
            Offer(title='iphone', fesh=1, hyperid=1000, price=5000, hid=10, ts=9),
            Offer(title='iphone', fesh=2, hyperid=1000, price=6000, hid=10, ts=10),
            Offer(title='iphone', fesh=3, hyperid=1001, price=5000, hid=10, ts=11),
            Offer(title='iphone', fesh=4, hyperid=1001, price=10000, hid=10, ts=12),
            Offer(title='iphone', fesh=5, hyperid=1001, price=15000, hid=10, ts=13),
            Offer(title='iphone', fesh=6, hyperid=1002, price=7777, hid=10, ts=14),
            Offer(title='iphone', fesh=7, ts=15),
            Offer(title='iphone', fesh=8, ts=16),
            Offer(title='iphone', fesh=1, vclusterid=1000000001, price=5000, hid=100, ts=17),
            Offer(title='iphone', fesh=2, vclusterid=1000000001, price=6000, hid=100, ts=18),
            Offer(title='iphone', fesh=3, vclusterid=1000000002, price=5000, hid=100, ts=19),
            Offer(title='iphone', fesh=4, vclusterid=1000000002, price=10000, hid=100, ts=20),
            Offer(title='iphone', fesh=5, vclusterid=1000000002, price=15000, hid=100, ts=21),
            Offer(title='iphone', fesh=6, vclusterid=1000000003, price=7777, hid=100, ts=22),
            # новая мин ставка модели = 3, до нее будет поднята фактическая ставка, старая минстака = 4
            Offer(title='iphone', fesh=6, hyperid=1004, price=3000, hid=10, ts=24),
        ]

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 9).respond(0.6)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 10).respond(0.5)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 11).respond(0.4)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 12).respond(0.3)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 13).respond(0.2)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 14).respond(0.1)

    def offer_auction(self, bid, min_bid):
        if bid < min_bid:
            return 1

        alpha = 0.61
        beta = 0.01
        gamma = 2.742
        return 1.0 + alpha * (1.0 / (1.0 + gamma * math.exp(-beta * (bid - min_bid))) - 1.0 / (1.0 + gamma))

    def model_auction(self, bid, min_bid, alpha=0.5, beta=0.04, gamma=1.0, boost=1.04):
        if bid == 0:
            return 1

        if bid <= min_bid:
            return boost
        return boost + alpha * (1.0 / (1.0 + gamma * math.exp(-beta * (bid - min_bid))) - 1.0 / (1.0 + gamma))

    def test_model_bid_auction_calculation(self):
        """проверяем, что для моделей с вендорской ставкой большей минставки считается аукцион по правильной формуле
        минставка вендора берется как минставка для среднего по цене оффера
        для офферов считается обычный аукцион"""

        # (ts, noffers, bid, min_bid, mn_value)
        # Почему для модели 1001 ставка поднялась с 8 до 10:
        # 1. при ранжировании испольщуются СТАРЫЕ мин ставки, для модели 1001: MIN_VBID=8
        # 2. для биллинга используются НОВЫЕ мин ставки, для модели 1001 minVBid = 10
        # 3. в случае, если ненулевая ставка на модель меньше НОВОЙ мин ставки - поднимаем ставку до НОВОЙ мин ставки
        result_models = [
            (23, 1, 3, 4, 0.5),
            (1, 2, 10, 6, 0.4),
            (2, 3, 10, 8, 0.3),
            (3, 1, 0, 7, 0.2),
            (4, 0, 0, 1, 0.1),
        ]

        # (ts, bid, min_bid, mn_value)
        result_offers = [
            (9, 10, 8, 0.6),
            (10, 10, 8, 0.5),
            (11, 10, 8, 0.4),
            (12, 10, 8, 0.3),
            (13, 10, 8, 0.2),
            (14, 10, 11, 0.1),
        ]

        rearr = '&rearr-factors=market_disable_auction_for_offers_with_model=0;'

        response = self.report.request_json('place=prime&hid=10&debug-doc-count=60&debug=da&rids=213' + rearr)

        for ts, _, vbid, min_vbid, mn in result_models:
            mul = self.model_auction(vbid, min_vbid)

            self.assertFragmentIn(
                response,
                {
                    "properties": {
                        "TS": str(ts),
                        "VBID": str(vbid),
                        "MIN_VBID": str(min_vbid),
                        "DOCUMENT_AUCTION_TYPE": "MODEL_VENDOR",
                    },
                    "rank": [{"name": "CPM", "value": str(int(100000 * mul * mn))}],
                },
                preserve_order=False,
            )

        for ts, bid, min_bid, mn in result_offers:
            mul = self.offer_auction(bid, min_bid)
            self.assertFragmentIn(
                response,
                {
                    "properties": {
                        "TS": str(ts),
                        "BID": str(bid),
                        "MIN_BID": str(min_bid),
                        "DOCUMENT_AUCTION_TYPE": "CPC",
                    },
                    "rank": [{"name": "CPM", "value": str(int(100000 * mul * mn))}],
                },
                preserve_order=False,
            )

        rearr = '&rearr-factors=market_disable_auction_for_offers_with_model=0;'
        response = self.report.request_json('place=prime&hid=100&debug-doc-count=60&debug=da&rids=213' + rearr)

        # (ts, vbid, min_vbid)
        # Почему для кластера 1000000002 ставка поднялась с 8 до 10 - аналогично модели 1001 выше
        result_clusters = [(5, 10, 6), (6, 10, 8), (7, 0, 7), (8, 0, 1)]

        for ts, vbid, min_vbid in result_clusters:
            mul = self.model_auction(vbid, min_vbid)

            self.assertFragmentIn(
                response,
                {
                    "properties": {
                        "TS": str(ts),
                        "VBID": str(vbid),
                        "MIN_VBID": str(min_vbid),
                        "DOCUMENT_AUCTION_TYPE": "MODEL_VENDOR",
                    },
                    "rank": [{"name": "CPM", "value": str(int(10000 * mul))}],
                },
                preserve_order=False,
            )

    def test_search_auction_vendor(self):
        """Проверяем что передаются параметры аукциона для моделей под флагом
        market_tweak_search_auction_vendor_params
        market_tweak_search_auction_vendor_boost
        """

        response = self.report.request_json(
            'place=prime&hid=10&debug-doc-count=60&debug=da&rids=213&debug-doc-count=60&rearr-factors='
            'market_tweak_search_auction_vendor_params=0.35,0.1,4;market_tweak_search_auction_vendor_boost=1.08;'
            'market_disable_auction_for_offers_with_model=0'
        )
        self.assertFragmentIn(
            response,
            {
                "logicTrace": [
                    Contains(
                        "Using search auction with parameters:",
                        "vendorAlpha=0.35 vendorBeta=0.1 vendorGamma=4 vendorBoost=1.08",
                        "auction type: 0",
                    )
                ]
            },
        )

        params = {'alpha': 0.35, 'beta': 0.1, 'gamma': 4, 'boost': 1.08}

        # (ts, noffers, bid, min_bid, mn_value)
        result_models = [
            (23, 1, 3, 4, 0.5),
            (1, 2, 10, 6, 0.4),
            (2, 3, 10, 8, 0.3),
            (3, 1, 0, 7, 0.2),
            (4, 0, 0, 1, 0.1),
        ]
        # (ts, bid, min_bid, mn_value)
        result_offers = [
            (9, 10, 8, 0.6),
            (10, 10, 8, 0.5),
            (11, 10, 8, 0.4),
            (12, 10, 8, 0.3),
            (13, 10, 8, 0.2),
            (14, 10, 11, 0.1),
        ]

        for ts, _, vbid, min_vbid, mn in result_models:
            mul = self.model_auction(vbid, min_vbid, **params)

            self.assertFragmentIn(
                response,
                {
                    "properties": {
                        "TS": str(ts),
                        "VBID": str(vbid),
                        "MIN_VBID": str(min_vbid),
                        "DOCUMENT_AUCTION_TYPE": "MODEL_VENDOR",
                    },
                    "rank": [{"name": "CPM", "value": str(int(100000 * mul * mn))}],
                },
            )

        for ts, bid, min_bid, mn in result_offers:
            mul = self.offer_auction(bid, min_bid)
            self.assertFragmentIn(
                response,
                {
                    "properties": {
                        "TS": str(ts),
                        "BID": str(bid),
                        "MIN_BID": str(min_bid),
                        "DOCUMENT_AUCTION_TYPE": "CPC",
                    },
                    "rank": [{"name": "CPM", "value": str(int(100000 * mul * mn))}],
                },
                preserve_order=False,
            )

    @classmethod
    def prepare_model_bid_cutoff(cls):
        """Заведем несколько моделей и офферов с вендорскими ставками"""

        cls.index.models += [
            Model(hyperid=500, hid=1, title='iphone0', vbid=10, datasource_id=1),
            Model(hyperid=501, hid=1, title='iphone42', vbid=17, datasource_id=2),
            Model(hyperid=502, hid=1, title='iphone44', vbid=23, datasource_id=3),
        ]

        cls.index.offers += [
            Offer(title='iphoneOffer0', vbid=11, datasource_id=1),
            Offer(title='iphoneOffer42', vbid=18, datasource_id=2),
            Offer(title='iphoneOffer44', vbid=24, datasource_id=3),
        ]

    def _test_model_bid_cutoff(self, text, vbid=0, datasource_id=0):
        """... и проверим, что у них в дебаг выдаче будут соответствующие ставки"""
        response = self.report.request_json(
            'place=prime&debug=da&rearr-factors=market_force_use_vendor_bid=1&text='
            + text
            + '&rearr-factors=disable_panther_quorum=0'
        )

        # это означает, что мы можем пользоваться вендорской ставкой на метапоиске
        self.assertFragmentIn(
            response, {"debug": {"sale": {"vBid": vbid}, "tech": {"vendorDatasourceId": datasource_id}}}
        )

        # это означает, что мы можем пользоваться вендорской ставкой на базовом
        # (фактором точно можно пользоваться на базовом)
        self.assertFragmentIn(response, {"debug": {"factors": {"VBID": str(vbid) if vbid else Absent()}}})

    def test_model_bid_cutoff(self):
        """Проверяем, что быстроотключение вендорских ставок на модели работает для модельных
        ставок (сбрасывает их в 0)
        """
        self.dynamic.disabled_vendor_model_bids += [
            DynamicVendorModelBid(1),
            DynamicVendorModelBid(2),
        ]

        self._test_model_bid_cutoff("iphone0", vbid=0, datasource_id=1)
        self._test_model_bid_cutoff("iphone42", vbid=0, datasource_id=2)
        self._test_model_bid_cutoff("iphone44", vbid=23, datasource_id=3)

        self.dynamic.disabled_vendor_model_bids.clear()
        self._test_model_bid_cutoff("iphone0", vbid=10, datasource_id=1)
        self._test_model_bid_cutoff("iphone42", vbid=17, datasource_id=2)
        self._test_model_bid_cutoff("iphone44", vbid=23, datasource_id=3)

    @classmethod
    def prepare_model_bid_prop_by_min_bid(cls):
        # Офферы для задания мин. вендорской ставки моделей
        cls.index.offers += [
            Offer(title='offer', hyperid=600, price=5000),
            Offer(title='offer2', hyperid=601, price=6000),
        ]

        cls.index.models += [
            Model(hyperid=600, hid=1, title='model', vbid=1),  # minVendorBid=5
            Model(hyperid=601, hid=1, title='model2', vbid=0),  # minVendorBid=6
        ]

    def test_model_bid_prop_by_min_bid(self):
        """Проверяем, что ненулевая ставка на модель меньше минимальной (новой минимальной - дял биллинга) -
        она устанавливается равной минимальной
        Нулевая ставка на модель - в любом случае остается нулевой
        """
        response = self.report.request_json('place=prime&debug=da&hid=1')

        # проверяем на метапоиске (sale) и на базовом (factors)
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "titles": {"raw": "model"},
                        "debug": {"sale": {"vBid": 5, "minVBid": 5}, "factors": {"VBID": "5"}},
                    },
                    {
                        "titles": {"raw": "model2"},
                        "debug": {"sale": {"vBid": 0, "minVBid": 6}, "factors": {"VBID": Absent()}},
                    },
                ]
            },
        )

        self.show_log.expect(hyper_id=600, vc_bid=5, min_vc_bid=5).once()
        self.show_log.expect(hyper_id=601, vc_bid=0, min_vc_bid=6).once()

    def test_vendor_offer_cutoff(self):
        self.dynamic.disabled_vendor_offer_bids += [
            DynamicVendorOfferBid(1),
            DynamicVendorOfferBid(2),
        ]


if __name__ == '__main__':
    main()
