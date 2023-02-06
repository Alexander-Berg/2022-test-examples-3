#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa
from core.testcase import main
from core.testcase import TestCase
from core.types import (
    Shop,
    Offer,
    Model,
)

# Провекра фильтрации оферов по заданному shop id и business id в советнике и виджите


class T(TestCase):
    @staticmethod
    def get_request(params, rearr):
        def dict_to_str(data, separator):
            return str(separator).join("{}={}".format(str(k), str(v)) for (k, v) in data.iteritems())

        return "{}&rearr-factors={}".format(dict_to_str(params, '&'), dict_to_str(rearr, ';'))

    """
    Shop Id магазинов для искоючения при показе в Советнике и Виджете
    998745
    3889860
    3888766
    3856769
    """

    @classmethod
    def prepare_shops_to_be_exclude(cls):
        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']

        start_hid = 1281
        cls.index.shops += [
            Shop(fesh=998745, cpa=Shop.CPA_REAL),
            Shop(fesh=3889860, cpa=Shop.CPA_REAL),
            Shop(fesh=3888766, cpa=Shop.CPA_REAL),
            Shop(fesh=3856769, cpa=Shop.CPA_REAL),
        ]
        start_hyper_id = 9326
        cls.index.shops += [Shop(fesh=3856769 + 2 + x, cpa=Shop.CPA_REAL) for x in range(6)]
        cls.index.offers += [
            Offer(fesh=998745, hid=start_hid, title="Izolenta N1", cpa=Offer.CPA_REAL, hyperid=start_hyper_id + 10),
            Offer(fesh=3889860, hid=start_hid, title="Izolenta N1", cpa=Offer.CPA_REAL, hyperid=start_hyper_id + 20),
            Offer(fesh=3888766, hid=start_hid, title="Izolenta N1", cpa=Offer.CPA_REAL, hyperid=start_hyper_id + 30),
            Offer(fesh=3856769, hid=start_hid, title="Izolenta N1", cpa=Offer.CPA_REAL, hyperid=start_hyper_id + 40),
        ]
        cls.index.offers += [
            Offer(
                fesh=3856769 + 2 + x,
                hid=start_hid,
                title="Izolenta N1",
                cpa=Offer.CPA_REAL,
                hyperid=start_hyper_id + x,
                price=1000 + x,
            )
            for x in range(6)
        ]
        cls.index.models += [
            Model(
                hid=start_hid,
                hyperid=start_hyper_id,
                title="Best izolenta{}".format(start_hyper_id),
            )
        ]

    def test_shops_to_be_exclude_sovetnik(self):
        params = {
            "place": "prime",
            "api": "content",
            "client": "sovetnik",
            "hid": 1281,
        }
        rearr_flags = {
            "market_report_filter_by_shops_business_id_for_sovetnik": 1,
        }
        response = self.report.request_json(T.get_request(params, rearr_flags))
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "totalOffers": 6,
                    'shops': 6,
                }
            },
        )

    def test_shops_to_be_exclude_widget(self):
        # проверяем, что указанные shops id не выдаются для виджета
        params = {
            "place": "prime",
            "api": "content",
            "client": "widget",
            "hid": 1281,
        }
        rearr_flags = {
            "market_report_filter_by_shops_business_id_for_widget": 1,
        }
        response = self.report.request_json(T.get_request(params, rearr_flags))
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "totalOffers": 6,
                    "shops": 6,
                }
            },
        )

    @classmethod
    def prepare_all_to_exclude(cls):
        start_hid = 8112
        start_hyper_id = 111315
        cls.index.shops += [
            Shop(fesh=998745, cpa=Shop.CPA_REAL),
            Shop(fesh=3889860, cpa=Shop.CPA_REAL),
            Shop(fesh=3888766, cpa=Shop.CPA_REAL),
            Shop(fesh=3856769, cpa=Shop.CPA_REAL),
        ]
        cls.index.offers += [
            Offer(fesh=998745, hid=start_hid, title="Izolenta N1", cpa=Offer.CPA_REAL, hyperid=start_hyper_id + 10),
            Offer(fesh=3889860, hid=start_hid, title="Izolenta N1", cpa=Offer.CPA_REAL, hyperid=start_hyper_id + 20),
            Offer(fesh=3888766, hid=start_hid, title="Izolenta N1", cpa=Offer.CPA_REAL, hyperid=start_hyper_id + 30),
            Offer(fesh=3856769, hid=start_hid, title="Izolenta N1", cpa=Offer.CPA_REAL, hyperid=start_hyper_id + 40),
        ]
        cls.index.models += [
            Model(
                hid=start_hid,
                hyperid=start_hyper_id,
                title="Best izolenta{}".format(start_hyper_id),
            )
        ]

    # проверяем, что ничего не выдается, если офера были только у магазинов, что должны быть исключены
    def test_all_shops_excluded_sovetnik(self):
        params = {
            "place": "prime",
            "api": "content",
            "client": "sovetnik",
            "hid": 8112,
        }
        rearr_flags = {
            "market_report_filter_by_shops_business_id_for_sovetnik": 1,
        }
        response = self.report.request_json(T.get_request(params, rearr_flags))
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "totalOffers": 0,
                    'shops': 0,
                }
            },
        )

    def test_all_shops_excluded_widget(self):
        # проверяем, что указанные shops id не выдаются для виджета
        params = {
            "place": "prime",
            "api": "content",
            "client": "widget",
            "hid": 8112,
        }
        rearr_flags = {
            "market_report_filter_by_shops_business_id_for_widget": 1,
        }
        response = self.report.request_json(T.get_request(params, rearr_flags))
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "totalOffers": 0,
                    'shops': 0,
                }
            },
        )

    @classmethod
    def prepare_nothing_to_exclude(cls):
        start_hid = 7183
        start_hyper_id = 1317
        cls.index.shops += [Shop(fesh=131783 + 2 + x, cpa=Shop.CPA_REAL) for x in range(10)]
        cls.index.offers += [
            Offer(
                fesh=131783 + 2 + x,
                hid=start_hid,
                title="Izolenta N1",
                cpa=Offer.CPA_REAL,
                hyperid=start_hyper_id + x,
                price=1000 + x,
            )
            for x in range(10)
        ]
        cls.index.models += [
            Model(
                hid=start_hid,
                hyperid=start_hyper_id,
                title="Best izolenta{}".format(start_hyper_id),
            )
        ]

    # Проверяем, что не фильтруем нужные shop id.
    def test_nothing_to_exclude_sovetnik(self):
        params = {
            "place": "prime",
            "api": "content",
            "client": "sovetnik",
            "hid": 7183,
        }
        rearr_flags = {
            "market_report_filter_by_shops_business_id_for_sovetnik": 1,
        }
        response = self.report.request_json(T.get_request(params, rearr_flags))
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "totalOffers": 10,
                    'shops': 10,
                }
            },
        )

    def test_nothing_to_exclude_widget(self):
        params = {
            "place": "prime",
            "api": "content",
            "client": "widget",
            "hid": 7183,
        }
        rearr_flags = {
            "market_report_filter_by_shops_business_id_for_widget": 1,
        }
        response = self.report.request_json(T.get_request(params, rearr_flags))
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "totalOffers": 10,
                    'shops': 10,
                }
            },
        )

    @classmethod
    def prepare_turn_off_flag(cls):
        start_hid = 2347
        cls.index.shops += [
            Shop(fesh=998745, cpa=Shop.CPA_REAL),
            Shop(fesh=3889860, cpa=Shop.CPA_REAL),
            Shop(fesh=3888766, cpa=Shop.CPA_REAL),
            Shop(fesh=3856769, cpa=Shop.CPA_REAL),
        ]
        start_hyper_id = 131781
        cls.index.shops += [Shop(fesh=3856769 + 2 + x, cpa=Shop.CPA_REAL) for x in range(16)]
        cls.index.offers += [
            Offer(fesh=998745, hid=start_hid, title="Izolenta N166", cpa=Offer.CPA_REAL, hyperid=start_hyper_id + 17),
            Offer(fesh=3889860, hid=start_hid, title="Izolenta N177", cpa=Offer.CPA_REAL, hyperid=start_hyper_id + 20),
            Offer(fesh=3888766, hid=start_hid, title="Izolenta N188", cpa=Offer.CPA_REAL, hyperid=start_hyper_id + 30),
            Offer(fesh=3856769, hid=start_hid, title="Izolenta N199", cpa=Offer.CPA_REAL, hyperid=start_hyper_id + 40),
        ]
        cls.index.offers += [
            Offer(
                fesh=3856769 + 2 + x,
                hid=start_hid,
                title="Izolenta N1",
                cpa=Offer.CPA_REAL,
                hyperid=start_hyper_id + x,
                price=1000 + x,
            )
            for x in range(16)
        ]
        cls.index.models += [
            Model(
                hid=start_hid,
                hyperid=start_hyper_id,
                title="Best izolenta{}".format(start_hyper_id),
            )
        ]

    # Проверяем, выключение фильтра работает корректно. Shop id, что должны быть исключены, остаются
    def test_turn_off_flag_sovetnik(self):
        params = {
            "place": "prime",
            "api": "content",
            "client": "sovetnik",
            "hid": 2347,
        }
        rearr_flags = {
            "market_report_filter_by_shops_business_id_for_sovetnik": 0,
        }
        response = self.report.request_json(T.get_request(params, rearr_flags))
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "totalOffers": 20,
                    'shops': 20,
                }
            },
        )

    def test_turn_off_flag_widget(self):
        params = {
            "place": "prime",
            "api": "content",
            "client": "widget",
            "hid": 2347,
        }
        rearr_flags = {
            "market_report_filter_by_shops_business_id_for_widget": 0,
        }
        response = self.report.request_json(T.get_request(params, rearr_flags))
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "totalOffers": 20,
                    'shops': 20,
                }
            },
        )


if __name__ == '__main__':
    main()
