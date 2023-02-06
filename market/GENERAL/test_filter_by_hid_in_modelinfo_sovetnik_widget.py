#!/usr/bin/env python
# -*- coding: utf-8 -*-

import runner  # noqa
from core.testcase import main
from core.testcase import TestCase
from core.types import (
    Offer,
    Model,
    Shop,
)

base_params_widget = {"api": "content", "client": "widget", "debug": 1, "rids": 213}
base_params_sovetnik = {"api": "content", "client": "sovetnik", "debug": 1, "rids": 213}


class T(TestCase):
    @staticmethod
    def get_request(params, rearr):
        def dict_to_str(data, separator):
            return str(separator).join("{}={}".format(str(k), str(v)) for (k, v) in data.iteritems())

        return "{}&rearr-factors={}".format(dict_to_str(params, '&'), dict_to_str(rearr, ';'))

    @classmethod
    def prepare_hids_to_be_excluded(cls):
        """
        Модель для исключения в виджете и советнике
        """
        start_hyper_id = 131215
        cls.index.shops += [
            Shop(
                fesh=9987455,
                regions=[213],
                name="Vacuum-Horse-SHop-N100500",
                cpa=Shop.CPA_REAL,
            )
        ]
        cls.index.offers += [
            Offer(
                fesh=9987455,
                hid=16395651,
                title="Plush Hugehog{}".format(1),
                cpa=Offer.CPA_REAL,
                hyperid=start_hyper_id,
                price=1000,
            )
        ]
        cls.index.models += [
            Model(
                hid=16395651,
                hyperid=start_hyper_id,
                title="Shikimori",
            )
        ]

    """
    Тесты - виджет
    """

    def test_hids_to_be_excluded_widget_prime(self):
        """
        Проверяем, что не выдается ничего - ни модель, ни оффер в prime
        """
        base_params_widget["place"] = "prime"
        base_params_widget["hid"] = 16395651
        base_params_widget["hyperid"] = 131215
        rearr_flags = {
            "market_report_filter_by_hid_for_widget": 1,
        }
        response = self.report.request_json(T.get_request(base_params_widget, rearr_flags))
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 0,
                    "totalOffers": 0,
                    "totalModels": 0,
                }
            },
        )

    def test_hids_to_be_excluded_widget_modelinfo(self):
        """
        Проверяем, что нет выдачи в modelinfo
        """
        base_params_widget["place"] = "modelinfo"
        base_params_widget["hid"] = 16395651
        base_params_widget["hyperid"] = 131215
        rearr_flags = {
            "market_report_filter_by_hid_for_widget": 1,
        }
        response = self.report.request_json(T.get_request(base_params_widget, rearr_flags))
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 0,
                    "totalOffers": 0,
                    "totalModels": 0,
                }
            },
        )

    def test_hids_to_be_excluded_widget_productoffers(self):
        """
        Проверяем, что нет выдачи в producoffers
        """
        base_params_widget["place"] = "productoffers"
        base_params_widget["hid"] = 16395651
        base_params_widget["hyperid"] = 131215
        rearr_flags = {
            "market_report_filter_by_hid_for_widget": 1,
        }
        response = self.report.request_json(T.get_request(base_params_widget, rearr_flags))
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 0,
                    "totalOffers": 0,
                    "totalModels": 0,
                }
            },
        )

    """
    Проверка выключенного фильтра - во всех тестах должна вернуться 1 моедль из запрещенной категории с заданным id
    """

    def test_turn_off_filter_widget_modelinfo(self):

        base_params_widget["place"] = "modelinfo"
        base_params_widget["hid"] = 16395651
        base_params_widget["hyperid"] = 131215
        rearr_flags = {
            "market_report_filter_by_hid_for_widget": 0,
        }
        response = self.report.request_json(T.get_request(base_params_widget, rearr_flags))
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 1,
                    "results": [
                        {
                            "entity": "product",
                            "id": 131215,
                            "categories": [
                                {
                                    "id": 16395651,
                                }
                            ],
                        }
                    ],
                },
            },
        )

    def test_turn_off_filter_widget_productoffers(self):
        base_params_widget["place"] = "productoffers"
        base_params_widget["hid"] = 16395651
        base_params_widget["hyperid"] = 131215
        rearr_flags = {
            "market_report_filter_by_hid_for_widget": 0,
        }
        response = self.report.request_json(T.get_request(base_params_widget, rearr_flags))
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 1,
                    "results": [
                        {
                            "entity": "offer",
                            "categories": [
                                {
                                    "id": 16395651,
                                }
                            ],
                        }
                    ],
                },
            },
        )

    """
    Тесты - советник
    """

    def test_hids_to_be_excluded_sovetnik_modelinfo(self):
        """
        Проверка выдачи в modelinfo - ничего не должно вернуться
        """
        base_params_sovetnik["place"] = "modelinfo"
        base_params_sovetnik["hid"] = 16395651
        base_params_sovetnik["hyperid"] = 131215
        rearr_flags = {
            "market_report_filter_by_hid_for_sovetnik": 1,
        }
        response = self.report.request_json(T.get_request(base_params_sovetnik, rearr_flags))
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 0,
                    "totalOffers": 0,
                    "totalModels": 0,
                }
            },
        )

    def test_hids_to_be_excluded_sovetnik_prime(self):
        """
        Проверка выдачи в prime - ничего не должно вернуться
        """
        base_params_sovetnik["place"] = "prime"
        base_params_sovetnik["hid"] = 16395651
        base_params_sovetnik["hyperid"] = 131215
        rearr_flags = {
            "market_report_filter_by_hid_for_sovetnik": 1,
        }
        response = self.report.request_json(T.get_request(base_params_sovetnik, rearr_flags))
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 0,
                    "totalOffers": 0,
                    "totalModels": 0,
                }
            },
        )

    def test_hids_to_be_excluded_sovetnik_productoffer(self):
        """
        Проверка выдачи в productoffers - ничего не должно вернуться
        """
        base_params_sovetnik["place"] = "productoffers"
        base_params_sovetnik["hid"] = 16395651
        base_params_sovetnik["hyperid"] = 131215
        rearr_flags = {
            "market_report_filter_by_hid_for_sovetnik": 1,
        }
        response = self.report.request_json(T.get_request(base_params_sovetnik, rearr_flags))
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 0,
                    "totalOffers": 0,
                    "totalModels": 0,
                }
            },
        )

    """
    Проверка выключенного фильтра в советнике - должна вернуться 1 моедль из запрещенной категории с заданным id
    """

    def test_turn_off_filter_sovetnik(self):
        base_params_sovetnik["place"] = "modelinfo"
        base_params_sovetnik["hid"] = 16395651
        base_params_sovetnik["hyperid"] = 131215
        rearr_flags = {
            "market_report_filter_by_hid_for_sovetnik": 0,
        }
        response = self.report.request_json(T.get_request(base_params_sovetnik, rearr_flags))
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 1,
                    "results": [
                        {
                            "entity": "product",
                            "id": 131215,
                            "categories": [
                                {
                                    "id": 16395651,
                                }
                            ],
                        }
                    ],
                },
            },
        )

    def test_turn_off_filter_sovetnik_productoffers(self):
        base_params_sovetnik["place"] = "productoffers"
        base_params_sovetnik["hid"] = 16395651
        base_params_sovetnik["hyperid"] = 131215
        rearr_flags = {
            "market_report_filter_by_hid_for_sovetnik": 0,
        }
        response = self.report.request_json(T.get_request(base_params_sovetnik, rearr_flags))
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 1,
                    "results": [
                        {
                            "entity": "offer",
                            "categories": [
                                {
                                    "id": 16395651,
                                }
                            ],
                        }
                    ],
                },
            },
        )


if __name__ == '__main__':
    main()
