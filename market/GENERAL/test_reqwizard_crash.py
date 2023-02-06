#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import Region
from core.testcase import TestCase, main


"""
https://st.yandex-team.ru/MARKETINCIDENTS-3014
Была попытка вывести строку размером -36 символов.
Из-за этого программа прерывалась.
"""


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.index.regiontree += [
            Region(rid=2, name='Питер'),
        ]
        cls.settings.ignore_qtree_decoding_failed_in_error_log = True

    @classmethod
    def prepare_over_original_text(cls):
        cls.reqwizard.on_pure_request('контур плюс тест-полоски №50 купить в санкт-петербурге').respond(
            {
                "OriginalRequest": "контур плюс тест-полоски №50 купить в санкт-петербурге",
                "ProcessedRequest": "контур плюс тест-полоски no50 купить в санкт-петербурге",  # Реквизард добавил одну букву в запрос и поэтому все токены оказались сдвинуты. Репорт же смотрит на оригинальный запрос  # noqa
                "Tokens": [
                    {"Text": "контур", "BeginChar": 0, "EndChar": 6, "BeginByte": 0, "EndByte": 12},
                    {"Text": "плюс", "BeginChar": 7, "EndChar": 11, "BeginByte": 13, "EndByte": 21},
                    {"Text": "тест", "BeginChar": 12, "EndChar": 16, "BeginByte": 22, "EndByte": 30},
                    {"Text": "полоски", "BeginChar": 17, "EndChar": 24, "BeginByte": 31, "EndByte": 45},
                    {"Text": "no", "BeginChar": 25, "EndChar": 27, "BeginByte": 46, "EndByte": 48},
                    {"Text": "50", "BeginChar": 27, "EndChar": 29, "BeginByte": 48, "EndByte": 50},
                    {"Text": "купить", "BeginChar": 30, "EndChar": 36, "BeginByte": 51, "EndByte": 63},
                    {"Text": "в", "BeginChar": 37, "EndChar": 38, "BeginByte": 64, "EndByte": 66},
                    {"Text": "санкт", "BeginChar": 39, "EndChar": 44, "BeginByte": 67, "EndByte": 77},
                    {"Text": "петербурге", "BeginChar": 45, "EndChar": 55, "BeginByte": 78, "EndByte": 98},
                ],
                "Delimiters": [
                    {},
                    {"Text": " ", "BeginChar": 6, "EndChar": 7, "BeginByte": 12, "EndByte": 13},
                    {"Text": " ", "BeginChar": 11, "EndChar": 12, "BeginByte": 21, "EndByte": 22},
                    {"Text": "-", "BeginChar": 16, "EndChar": 17, "BeginByte": 30, "EndByte": 31},
                    {"Text": " ", "BeginChar": 24, "EndChar": 25, "BeginByte": 45, "EndByte": 46},
                    {},
                    {"Text": " ", "BeginChar": 29, "EndChar": 30, "BeginByte": 50, "EndByte": 51},
                    {"Text": " ", "BeginChar": 36, "EndChar": 37, "BeginByte": 63, "EndByte": 64},
                    {"Text": " ", "BeginChar": 38, "EndChar": 39, "BeginByte": 66, "EndByte": 67},
                    {"Text": "-", "BeginChar": 44, "EndChar": 45, "BeginByte": 77, "EndByte": 78},
                    {},
                ],
                "Morph": [
                    {
                        "Tokens": {"Begin": 0, "End": 1},
                        "Lemmas": [
                            {"Text": "контур", "Language": "ru", "Grammems": ["S acc sg m inan", "S nom sg m inan"]}
                        ],
                    },
                    {
                        "Tokens": {"Begin": 1, "End": 2},
                        "Lemmas": [
                            {"Text": "плюс", "Language": "ru", "Grammems": ["CONJ"]},
                            {"Text": "плюс", "Language": "ru", "Grammems": ["S acc sg m inan", "S nom sg m inan"]},
                            {"Text": "плюс", "Language": "ru", "Grammems": ["PR"]},
                        ],
                    },
                    {
                        "Tokens": {"Begin": 2, "End": 3},
                        "Lemmas": [
                            {"Text": "тест", "Language": "ru", "Grammems": ["S acc sg m inan", "S nom sg m inan"]},
                            {"Text": "тесто", "Language": "ru", "Grammems": ["S gen pl n inan"]},
                        ],
                    },
                    {
                        "Tokens": {"Begin": 3, "End": 4},
                        "Lemmas": [
                            {
                                "Text": "полоска",
                                "Language": "ru",
                                "Grammems": ["S acc pl f inan", "S gen sg f inan", "S nom pl f inan"],
                            }
                        ],
                    },
                    {"Tokens": {"Begin": 4, "End": 5}, "Lemmas": [{"Text": "no"}]},
                    {"Tokens": {"Begin": 5, "End": 6}, "Lemmas": [{"Text": "00000000050"}]},
                    {
                        "Tokens": {"Begin": 6, "End": 7},
                        "Lemmas": [{"Text": "купить", "Language": "ru", "Grammems": ["V inf"]}],
                    },
                    {"Tokens": {"Begin": 7, "End": 8}, "Lemmas": [{"Text": "в", "Language": "ru", "Grammems": ["PR"]}]},
                    {
                        "Tokens": {"Begin": 8, "End": 9},
                        "Lemmas": [{"Text": "санкт", "Language": "ru", "Grammems": ["COM"]}],
                    },
                    {
                        "Tokens": {"Begin": 9, "End": 10},
                        "Lemmas": [{"Text": "петербург", "Language": "ru", "Grammems": ["S geo abl sg m inan"]}],
                    },
                ],
                "Extensions": [
                    {"Tokens": {"Begin": 1, "End": 2}, "Type": "TE_TRANSLIT", "ExtendTo": "plus", "Weight": 0.859},
                    {"Tokens": {"Begin": 3, "End": 4}, "Type": "TE_DERIV", "ExtendTo": "полосы", "Weight": 0.232},
                    {"Tokens": {"Begin": 6, "End": 7}, "Type": "TE_DERIV", "ExtendTo": "покупки", "Weight": 0.458},
                    {"Tokens": {"Begin": 6, "End": 7}, "Type": "TE_DERIV", "ExtendTo": "приобрести", "Weight": 0.766},
                    {"Tokens": {"Begin": 6, "End": 7}, "Type": "TE_DERIV", "ExtendTo": "продажа", "Weight": 0.304},
                    {"Tokens": {"Begin": 0, "End": 1}, "Type": "TE_TRANSLIT", "ExtendTo": "kontur", "Weight": 0.77},
                    {"Tokens": {"Begin": 2, "End": 4}, "Type": "TE_MISSPELL", "ExtendTo": "тестполоски", "Weight": 1},
                    {"Tokens": {"Begin": 6, "End": 7}, "Type": "TE_MISSPELL", "ExtendTo": "ку пить", "Weight": 1},
                ],
                "GeoAddr": [
                    {
                        "Tokens": {"Begin": 7, "End": 10},
                        "Fields": [
                            {"Tokens": {"Begin": 8, "End": 10}, "Type": "City", "Name": "санкт-петербург", "Id": [2]}
                        ],
                        "Weight": 0.9968780279,
                        "BestGeoId": 2,
                        "BestInheritedId": 2,
                    }
                ],
                "Onto": [
                    {
                        "Tokens": {"Begin": 0, "End": 1},
                        "Data": {
                            "Type": "org",
                            "TypeId": 1,
                            "Weight": 0.5199998021,
                            "One": 0.0600923,
                            "Rule": "Wares",
                            "Intent": "unknown",
                            "IntWght": 0,
                        },
                    },
                    {
                        "Tokens": {"Begin": 0, "End": 2},
                        "Data": {
                            "Type": "org",
                            "TypeId": 1,
                            "Weight": 0.4399998486,
                            "One": 0.0330911,
                            "Rule": "Wares",
                            "Intent": "unknown",
                            "IntWght": 0,
                        },
                    },
                    {
                        "Tokens": {"Begin": 1, "End": 2},
                        "Data": {
                            "Type": "intent_bit",
                            "TypeId": 26,
                            "Weight": 1,
                            "One": 1,
                            "Rule": "Wares",
                            "Intent": "imported",
                            "IntWght": 1,
                        },
                    },
                    {
                        "Tokens": {"Begin": 2, "End": 3},
                        "Data": {
                            "Type": "food",
                            "TypeId": 16,
                            "Weight": 0.6199997067,
                            "One": 0.9960996,
                            "Rule": "Wares",
                            "Intent": "unknown",
                            "IntWght": 0,
                        },
                    },
                    {
                        "Tokens": {"Begin": 4, "End": 5},
                        "Data": {
                            "Type": "band",
                            "TypeId": 10,
                            "Weight": 0.1199999824,
                            "One": 5.05e-05,
                            "Rule": "Wares",
                            "Intent": "unknown",
                            "IntWght": 0,
                        },
                    },
                    {
                        "Tokens": {"Begin": 6, "End": 7},
                        "Data": {
                            "Type": "intent",
                            "TypeId": 25,
                            "Weight": 0.9199994206,
                            "One": 1,
                            "Rule": "Wares",
                            "Intent": "unknown",
                            "IntWght": 0,
                        },
                    },
                    {
                        "Tokens": {"Begin": 6, "End": 7},
                        "Data": {
                            "Type": "intent_bit",
                            "TypeId": 26,
                            "Weight": 1,
                            "One": 1,
                            "Rule": "Wares",
                            "Intent": "imported",
                            "IntWght": 1,
                        },
                    },
                    {
                        "Tokens": {"Begin": 8, "End": 9},
                        "Data": {
                            "Type": "geo",
                            "TypeId": 2,
                            "Weight": 0.4499998391,
                            "One": 9.98e-05,
                            "Rule": "Wares",
                            "Intent": "unknown",
                            "IntWght": 0,
                        },
                    },
                    {
                        "Tokens": {"Begin": 8, "End": 10},
                        "Data": {
                            "Type": "geo",
                            "TypeId": 2,
                            "Weight": 0.9799993634,
                            "One": 9.98e-05,
                            "Rule": "Wares",
                            "Intent": "unknown",
                            "IntWght": 0,
                        },
                    },
                    {
                        "Tokens": {"Begin": 9, "End": 10},
                        "Data": {
                            "Type": "geo",
                            "TypeId": 2,
                            "Weight": 0.4499998391,
                            "One": 9.99e-05,
                            "Rule": "Wares",
                            "Intent": "unknown",
                            "IntWght": 0,
                        },
                    },
                ],
                "MarketTiresModel": [
                    {"Tokens": {"Begin": 1, "End": 2}, "Data": {"Model": "Golf Plus"}},
                    {"Tokens": {"Begin": 1, "End": 2}, "Data": {"Model": "Golf Plus"}},
                    {"Tokens": {"Begin": 1, "End": 2}, "Data": {"Model": "Golf Plus"}},
                ],
                "MarketShop": [
                    {
                        "Tokens": {"Begin": 0, "End": 1},
                        "Data": {"Id": 267753, "AliasType": "FROM_URL", "ShopIsGoodForMatching": False},
                    },
                    {
                        "Tokens": {"Begin": 6, "End": 7},
                        "Data": {"Id": 468108, "AliasType": "FROM_URL", "ShopIsGoodForMatching": False},
                    },
                    {
                        "Tokens": {"Begin": 6, "End": 7},
                        "Data": {"Id": 452998, "AliasType": "FROM_URL", "ShopIsGoodForMatching": False},
                    },
                ],
            }
        )

    def test_crash_fix(self):
        """
        Проверяется, что программа не падает с коркой, если реквизард изменил запрос, где последним идет регион, а перед ним буква "в"
        """
        response = self.report.request_json(
            'place=productoffers&cvredirect=1&text=контур плюс тест-полоски №50 купить в санкт-петербурге&hyperid=1&debug=1&rids=2'
        )
        self.assertFragmentIn(response, {'search': {'results': []}})

        self.error_log.expect(code=3810)


if __name__ == '__main__':
    main()
