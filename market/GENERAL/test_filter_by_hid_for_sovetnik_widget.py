#!/usr/bin/env python
# -*- coding: utf-8 -*-

import runner  # noqa
from core.testcase import main
from core.testcase import TestCase
from core.types import (
    Offer,
    Model,
)

"""
Список с hid для исключения в советнике
"""
hids_to_exclude_soventik = [
    14352315,
    989022,
    17364732,
    16727842,
    16440141,
    16155381,
    15758037,
    90407,
    16634808,
    91500,
    17364738,
    226665,
    13876322,
    16155466,
    16155647,
    13755058,
    16155455,
    13626140,
    16761766,
    13876433,
    13876421,
    17878060,
    15720372,
    15720379,
    15720387,
    17878048,
    15720347,
    10682597,
    2212983,
    17364722,
    922144,
    15625452,
    15510364,
    16155448,
    13876298,
    16155587,
    16155526,
    13876313,
    16155651,
    16360983,
    14979737,
    14910546,
    13858309,
    16727832,
    91514,
    13876419,
    13876448,
    13876352,
    16155476,
    12341935,
    13482742,
    638258,
    10833347,
    18024163,
    14808696,
    91470,
    20719590,
    15510336,
    13876334,
    15984085,
    16440108,
    16440100,
    23960430,
    16155504,
    10682608,
    294675,
    5081621,
    14340627,
    4827843,
    18601530,
    18024136,
    18024156,
    7774309,
    17364727,
    4317343,
    17691691,
    280637,
    13626085,
    13876406,
    16155560,
    13876288,
    13946516,
    16761723,
    90525,
    15043354,
    17279413,
    13098001,
    14415181,
    7775900,
    15708223,
    15189948,
    16065440,
    17280486,
    15618745,
    15032315,
    90744,
    14245867,
    14993540,
    14415405,
    16074646,
    6527203,
    16312426,
    7812175,
    10553774,
    91522,
    278339,
    7339056,
    179657,
    91306,
    16206090,
    15995346,
    13776188,
    1009482,
    91657,
    15448926,
    16395651,
]
"""
Список с hid для исключения в виджите
"""
hids_to_exclude_widget = [
    16395651,
]
"""
Список с hid для отображения (данные hid не должны исключаться)
"""
hids_to_include = []
for i in range(5):
    hids_to_include.append(8080 + i)

"""
База для запросов в советник/виджет
"""
base_params_sovetnik = {"place": "prime", "api": "content", "client": "sovetnik", "debug": 1}
base_params_widget = {"place": "prime", "api": "content", "client": "widget", "debug": 1}


class T(TestCase):
    @staticmethod
    def get_request(params, rearr):
        def dict_to_str(data, separator):
            return str(separator).join("{}={}".format(str(k), str(v)) for (k, v) in data.iteritems())

        return "{}&rearr-factors={}".format(dict_to_str(params, '&'), dict_to_str(rearr, ';'))

    @classmethod
    def prepare_hids_to_be_excluded(cls):
        """
        Список оферов с моделями для исключения при запросе в советник/виджет
        """
        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']

        start_hyper_id = 131215
        for i in range(len(hids_to_exclude_soventik)):
            cls.index.offers += [
                Offer(
                    hid=hids_to_exclude_soventik[i],
                    title="Plush Hugehog{}".format(1),
                    cpa=Offer.CPA_REAL,
                    hyperid=start_hyper_id + i,
                    price=1000 + i,
                )
            ]
            cls.index.models += [
                Model(
                    hid=hids_to_exclude_soventik[i],
                    hyperid=start_hyper_id + i,
                    title="Plush Hugehog{}".format(i),
                )
            ]

        """
        Список оферов с моделями для отображения при запросе в советник/виджет
        """
        start_hyper_id2 = 171923
        for i in range(len(hids_to_include)):
            cls.index.offers += [
                Offer(
                    hid=hids_to_include[i],
                    title="Plush apple{}".format(i),
                    cpa=Offer.CPA_REAL,
                    hyperid=start_hyper_id2 + i,
                    price=150 + i * 10,
                )
            ]
            cls.index.models += [
                Model(
                    hid=hids_to_include[i],
                    hyperid=start_hyper_id2 + i,
                    title="Plush apple{}".format(i),
                )
            ]

    """
    Проверка работы флага - проверяем, что категории из списка будут исключены при показе в советнике
    """

    def test_hids_to_be_excluded_sovetnik(self):
        for i in range(len(hids_to_exclude_soventik)):
            base_params_sovetnik["hid"] = hids_to_exclude_soventik[i]
            rearr_flags = {
                "market_report_filter_by_hid_for_sovetnik": 1,
            }
            response = self.report.request_json(T.get_request(base_params_sovetnik, rearr_flags))
            self.assertFragmentIn(
                response,
                {
                    "search": {
                        "totalOffers": 0,
                        "shops": 0,
                    }
                },
            )

    """
    Проверка работы флага - проверяем, что категории из списка будут исключены при показе в виджете
    """

    def test_hids_to_be_excluded_widget(self):
        for i in range(len(hids_to_exclude_widget)):
            base_params_widget["hid"] = hids_to_exclude_widget[i]
            rearr_flags = {
                "market_report_filter_by_hid_for_widget": 1,
            }
            response = self.report.request_json(T.get_request(base_params_widget, rearr_flags))
            self.assertFragmentIn(
                response,
                {
                    "search": {
                        "totalOffers": 0,
                        "shops": 0,
                    }
                },
            )

    """
    Проверка отлючения флага - все категррии из списка отображаются при показе в советнике
    """

    def test_turn_off_filter_sovetnik(self):
        for i in range(len(hids_to_exclude_soventik)):
            base_params_sovetnik["hid"] = hids_to_exclude_soventik[i]
            base_params_sovetnik["adult"] = 1
            rearr_flags = {
                "market_report_filter_by_hid_for_sovetnik": 0,
            }
            response = self.report.request_json(T.get_request(base_params_sovetnik, rearr_flags))
            self.assertFragmentIn(
                response,
                {
                    "search": {
                        "totalOffers": 1,
                    }
                },
            )

    """
    Проверка отлючения флага - все категррии из списка отображаются при показе в виджете
    """

    def test_turn_off_filter_widget(self):
        for i in range(len(hids_to_exclude_widget)):
            base_params_widget["hid"] = hids_to_exclude_widget[i]
            base_params_widget["adult"] = 1
            rearr_flags = {
                "market_report_filter_by_hid_for_widget": 0,
            }
            response = self.report.request_json(T.get_request(base_params_widget, rearr_flags))
            self.assertFragmentIn(
                response,
                {
                    "search": {
                        "totalOffers": 1,
                    }
                },
            )

    """
    Проверяем, что не фильтруем ничего лишнего для советника
    """

    def test_hids_to_be_included_sovetnik(self):
        for i in range(len(hids_to_include)):
            base_params_sovetnik["hid"] = hids_to_include[i]
            rearr_flags = {
                "market_report_filter_by_hid_for_sovetnik": 1,
            }
            response = self.report.request_json(T.get_request(base_params_sovetnik, rearr_flags))
            self.assertFragmentIn(
                response,
                {
                    "search": {
                        "totalOffers": 1,
                    }
                },
            )

    """
    Проверяем, что не фильтруем ничего лишнего для виджета
    """

    def test_hids_to_be_included_widget(self):
        for i in range(len(hids_to_include)):
            base_params_widget["hid"] = hids_to_include[i]
            rearr_flags = {
                "market_report_filter_by_hid_for_widget": 1,
            }
            response = self.report.request_json(T.get_request(base_params_widget, rearr_flags))
            self.assertFragmentIn(
                response,
                {
                    "search": {
                        "totalOffers": 1,
                    }
                },
            )


if __name__ == '__main__':
    main()
