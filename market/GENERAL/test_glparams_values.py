#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.testcase import TestCase, main
from core.types import BlueOffer, GLParam, GLType, GLValue, MarketSku, Model, Offer, Vendor


# We do different things with param values having initial-found=0 in different cases:
#   * hide them (e.g., document-level GL filters);
#   * show them (e.g., root-level GL filters on prime).
# See https://st.yandex-team.ru/MARKETOUT-7851 for more details.
# Note that if a boolean param has at least one value with initial-value != 0, both values are output.


class T(TestCase):
    @classmethod
    def prepare(cls):
        # Numeration rules:
        # - hid = {101, 102, 103}
        # - glparam = {201, 300} & {100500}
        # - glvalue = {301, 500} & {100501, 100600}
        # cluster_filter=True is set to ensure all parameters (but one) are shown in the offer-level GL filters
        # in addition to those attached to the output root.

        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']

        cls.index.gltypes = [
            # A simple enum.
            GLType(
                param_id=201,
                hid=101,
                gltype=GLType.ENUM,
                cluster_filter=True,
                values=[
                    301,
                    302,
                    303,
                ],
            ),
            # A simple boolean.
            GLType(param_id=202, hid=101, gltype=GLType.BOOL, cluster_filter=True),
            GLType(param_id=204, hid=101, gltype=GLType.BOOL, cluster_filter=True),
            # A simple numeric.
            GLType(param_id=203, hid=101, gltype=GLType.NUMERIC, cluster_filter=True),
            GLType(
                param_id=207,
                hid=101,
                gltype=GLType.ENUM,
                cluster_filter=True,
                subtype='size',
                unit_param_id=210,
                values=[
                    GLValue(value_id=1, text='X', unit_value_id=1),
                    GLValue(value_id=2, text='X', unit_value_id=1),
                    GLValue(value_id=3, text='Y', unit_value_id=1),
                    GLValue(value_id=4, text='Y', unit_value_id=1),
                    GLValue(value_id=5, text='Z', unit_value_id=2),
                    GLValue(value_id=6, text='Z', unit_value_id=2),
                ],
            ),
            GLType(
                param_id=208,
                hid=113,
                gltype=GLType.ENUM,
                cluster_filter=True,
                subtype='diopt',
                model_filter_index=1,
                values=[
                    GLValue(value_id=1, text='+1', unit_value_id=1),
                    GLValue(value_id=2, text='-1', unit_value_id=1),
                    GLValue(value_id=3, text='+50,3', unit_value_id=1),
                    GLValue(value_id=4, text='23 we', unit_value_id=1),
                    GLValue(value_id=5, text='qq25', unit_value_id=1),
                    GLValue(value_id=6, text='-2.6.7', unit_value_id=1),
                ],
            ),
            # A vendor enum.
            GLType(
                param_id=208,
                hid=101,
                gltype=GLType.ENUM,
                cluster_filter=True,
                vendor=True,
                values=[
                    401,
                    402,
                    403,
                ],
            ),
            # Should not be shown in offer-level GL filters.
            GLType(param_id=209, hid=101, gltype=GLType.ENUM),
            GLType(
                param_id=210,
                hid=101,
                gltype=GLType.ENUM,
                position=None,
                values=[GLValue(value_id=1, text='size'), GLValue(value_id=2, text='megasize')],
            ),
        ]

        cls.index.models += [
            Model(hid=113, hyperid=105, title="Model"),
        ]

        cls.index.offers = [
            Offer(
                title='visual offer',
                hid=101,
                vclusterid=1000000001,
                glparams=[
                    GLParam(param_id=201, value=301),
                    GLParam(param_id=202, value=0),
                    GLParam(param_id=203, value=30),
                    GLParam(param_id=204, value=1),
                    GLParam(param_id=207, value=1),
                    GLParam(param_id=208, value=401),
                ],
            ),
            Offer(
                title='model offer',
                hid=101,
                hyperid=18,
                glparams=[
                    GLParam(param_id=207, value=1),
                ],
            ),
        ]

        cls.index.mskus += [
            MarketSku(
                hid=113,
                hyperid=105,
                sku=101,
                blue_offers=[BlueOffer(ts=10, price=3)],
                glparams=[GLParam(param_id=208, value=1)],
            ),
            MarketSku(
                hid=113,
                hyperid=105,
                sku=102,
                blue_offers=[BlueOffer(ts=10, price=3)],
                glparams=[GLParam(param_id=208, value=2)],
            ),
            MarketSku(
                hid=113,
                hyperid=105,
                sku=103,
                blue_offers=[BlueOffer(ts=10, price=3)],
                glparams=[GLParam(param_id=208, value=3)],
            ),
            MarketSku(
                hid=113,
                hyperid=105,
                sku=104,
                blue_offers=[BlueOffer(ts=10, price=3)],
                glparams=[GLParam(param_id=208, value=4)],
            ),
            MarketSku(
                hid=113,
                hyperid=105,
                sku=105,
                blue_offers=[BlueOffer(ts=10, price=3)],
                glparams=[GLParam(param_id=208, value=5)],
            ),
            MarketSku(
                hid=113,
                hyperid=105,
                sku=106,
                blue_offers=[BlueOffer(ts=10, price=3)],
                glparams=[GLParam(param_id=208, value=6)],
            ),
        ]

        # vendors have ids from 1 to 16 (inclusive)
        vendor_range = list(range(1, 1 + 16))

        # For testing &vendor-max-values= (hid=102).
        cls.index.gltypes += [
            GLType(
                param_id=100500,
                hid=102,
                gltype=GLType.ENUM,
                vendor=True,
                cluster_filter=True,
                short_enum_count=3,
                values=vendor_range,
            )
        ]

        for offer_count, vendor_id in enumerate(vendor_range, start=1):
            for _ in range(offer_count):
                cls.index.offers += [
                    Offer(
                        hid=102,
                        glparams=[
                            GLParam(param_id=100500, value=vendor_id),
                        ],
                    )
                ]

        # For testing top valued params (hid=103)
        top_values = [105, 106, 107]
        vendor_range = [GLValue(value_id=vid, top_value=vid in top_values) for vid in range(100, 100 + 40)]
        cls.index.gltypes += [
            GLType(param_id=2000000, hid=103, gltype=GLType.ENUM, vendor=True, values=vendor_range, cluster_filter=True)
        ]

        for offer_count, vendor_id in enumerate(vendor_range, start=1):
            for _ in range(offer_count):
                cls.index.offers += [
                    Offer(
                        hid=103,
                        glparams=[
                            GLParam(param_id=2000000, value=vendor_id),
                        ],
                    )
                ]

        cls.index.models += [
            Model(hid=103, hyperid=123456, model_clicks=10, vendor_id=108),
            Model(hid=103, hyperid=123457, model_clicks=5, vendor_id=109),
            Model(hid=103, hyperid=123458, model_clicks=7, vendor_id=109),
        ]

        # 1 offer for each model.
        cls.index.offers += [
            Offer(hyperid=123456),
            Offer(hyperid=123457),
            Offer(hyperid=123458),
        ]

        # test_vendor_sorting
        vrange = list(range(501, 506 + 1))

        cls.index.gltypes += [
            GLType(param_id=100501, hid=103, gltype=GLType.ENUM, vendor=True, cluster_filter=True, values=vrange)
        ]
        cls.index.vendors += [
            Vendor(vendor_id=502, name="aBC"),
            Vendor(vendor_id=501, name="aaa"),
            Vendor(vendor_id=503, name="DEF"),
            Vendor(vendor_id=505, name="аБВ"),
            Vendor(vendor_id=504, name="ааа"),
            Vendor(vendor_id=506, name="ГДЕ"),
        ]

        cls.index.offers += [
            Offer(hid=103, glparams=[GLParam(param_id=100501, value=vendor_id, vendor=True)]) for vendor_id in vrange
        ]

    def test_vendor_sorting(self):
        response = self.report.request_json('place=prime&hid=103')
        self.assertFragmentIn(
            response,
            {
                "filters": [
                    {
                        "id": "100501",
                        "name": "GLPARAM-100501",
                        "noffers": 6,
                        "position": 1,
                        "subType": "",
                        "type": "enum",
                        "values": [
                            {"id": "501", "value": "aaa"},
                            {"id": "502", "value": "aBC"},
                            {"id": "503", "value": "DEF"},
                            {"id": "504", "value": "ааа"},
                            {"id": "505", "value": "аБВ"},
                            {"id": "506", "value": "ГДЕ"},
                        ],
                    }
                ]
            },
        )

    def test_numeric_enum_sorting(self):
        fragment = {
            "filters": [
                {
                    "id": "208",
                    "name": "GLPARAM-208",
                    "position": 1,
                    "subType": "",
                    "type": "enum",
                    "values": [
                        {
                            "id": "6",
                        },
                        {
                            "id": "2",
                        },
                        {
                            "id": "1",
                        },
                        {
                            "id": "4",
                        },
                        {
                            "id": "5",
                        },
                        {
                            "id": "3",
                        },
                    ],
                }
            ]
        }

        response = self.report.request_json('place=prime&hid=113&rearr-factors=sort_numeric_enum_filters=208')
        self.assertFragmentIn(response, fragment, preserve_order=True)

        response = self.report.request_json(
            'place=productoffers&hyperid=105&hid=113&rearr-factors=sort_numeric_enum_filters=208'
        )
        self.assertFragmentIn(response, fragment, preserve_order=True)

    def test_prime_unchecked(self):
        response = self.report.request_json('place=prime&hid=101')

        # The offer-level glFilters should only contain the values attached to our offer.
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {"entity": "product"},
                        {
                            "entity": "offer",
                            "filters": [
                                {"values": [{"id": "301"}], "id": "201"},
                                {"values": [{"value": "0"}, {"value": "1"}], "id": "202"},
                                {
                                    "values": [{"id": "found"}],
                                    "id": "203",
                                },
                                {"units": [{"values": [{"id": "1"}], "unitId": "size"}], "id": "207"},
                                {"values": [{"id": "401"}], "id": "208"},
                            ],
                        },
                    ]
                }
            },
        )

    def test_prime_checked(self):
        response = self.report.request_json(
            'place=prime&hid=101'
            '&glfilter=201:301,302'
            '&glfilter=207:1,3'
            '&glfilter=208:401,402'.format(**globals())
        )

        # The offer-level glFilters should not be affected by &glfilter= in any way.
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {
                            "entity": "offer",
                            "filters": [
                                {"values": [{"id": "301"}], "id": "201"},
                                {"values": [{"value": "0"}, {"value": "1"}], "id": "202"},
                                {
                                    "values": [{"id": "found"}],
                                    "id": "203",
                                },
                                {"units": [{"values": [{"id": "1"}], "unitId": "size"}], "id": "207"},
                                {"values": [{"id": "401"}], "id": "208"},
                            ],
                        }
                    ]
                }
            },
        )

    def test_numeric_filters_dont_return_inf(self):
        # MARKETOUT_7826
        response = self.report.request_json('place=prime&hid=101&glfilter=204:0')
        # получим только один оффер у которого не задан numeric-фильтр 203
        # значения min max должны быть пустыми, а не -inf, inf
        self.assertFragmentIn(
            response,
            {
                "search": {"total": 1},
                "filters": [
                    {"id": "203", "type": "number", "values": [{"initialMax": "30", "initialMin": "30", "id": "found"}]}
                ],
            },
        )

        self.assertFragmentNotIn(response, {"type": "number", "values": [{"min": "-inf"}]})
        self.assertFragmentNotIn(response, {"type": "number", "values": [{"max": "inf"}]})

    def test_prime_supports_new_sizes(self):
        # задаем запрос с енумом-размером, проверяем, что нашлись как оффферы, так и модели/кластера
        response = self.report.request_json('place=prime&hid=101&glfilter=207:1')
        self.assertFragmentIn(
            response,
            [
                {"entity": "product", "id": 1000000001},
                {"entity": "product", "id": 18},
                {"entity": "offer", "titles": {"raw": "visual offer"}},
                {"entity": "offer", "titles": {"raw": "model offer"}},
            ],
        )

        # задаем запрос с фильтром-размером, которого нет для офферов/моделей/кластеров.
        # Проверяем, что ничего не нашлось
        response = self.report.request_json('place=prime&hid=101&glfilter=207:3')
        self.assertFragmentIn(response, {"results": []}, allow_different_len=False)

    def test_vendors(self):
        response = self.report.request_json('place=prime&hid=102')
        # Show only 12 vendors sorted alphabetically (the number 12 is hardcoded because the frontend guys wanted it).
        # These are the vendors having the greatest number of offers in the search results ([5...16]).
        self.assertFragmentIn(
            response,
            {
                "filters": [
                    {
                        "valuesCount": 16,
                        "values": [
                            {"value": "VENDOR-10", "id": "10"},
                            {"value": "VENDOR-11", "id": "11"},
                            {"value": "VENDOR-12", "id": "12"},
                            {"value": "VENDOR-13", "id": "13"},
                            {"value": "VENDOR-14", "id": "14"},
                            {"value": "VENDOR-15", "id": "15"},
                            {"value": "VENDOR-16", "id": "16"},
                            {"value": "VENDOR-5", "id": "5"},
                            {"value": "VENDOR-6", "id": "6"},
                            {"value": "VENDOR-7", "id": "7"},
                            {"value": "VENDOR-8", "id": "8"},
                            {"value": "VENDOR-9", "id": "9"},
                        ],
                        "id": "100500",
                    }
                ]
            },
        )

        response = self.report.request_json('place=prime&hid=102&showVendors=all')
        # Show all 16 vendors. The 4 vendors that didn't make it to the top 12 should be on the bottom of the list
        # and sorted alphabetically.
        self.assertFragmentIn(
            response,
            {
                "filters": [
                    {
                        "valuesCount": 16,
                        "values": [
                            {"value": "VENDOR-10", "id": "10"},
                            {"value": "VENDOR-11", "id": "11"},
                            {"value": "VENDOR-12", "id": "12"},
                            {"value": "VENDOR-13", "id": "13"},
                            {"value": "VENDOR-14", "id": "14"},
                            {"value": "VENDOR-15", "id": "15"},
                            {"value": "VENDOR-16", "id": "16"},
                            {"value": "VENDOR-5", "id": "5"},
                            {"value": "VENDOR-6", "id": "6"},
                            {"value": "VENDOR-7", "id": "7"},
                            {"value": "VENDOR-8", "id": "8"},
                            {"value": "VENDOR-9", "id": "9"},
                            {"value": "VENDOR-1", "id": "1"},
                            {"value": "VENDOR-2", "id": "2"},
                            {"value": "VENDOR-3", "id": "3"},
                            {"value": "VENDOR-4", "id": "4"},
                        ],
                        "id": "100500",
                    }
                ]
            },
        )

        response = self.report.request_json('place=prime&hid=102&glfilter=100500:4,16')
        # 16 was in the list already, just moved to the top; 4 was not in the list.
        self.assertFragmentIn(
            response,
            {
                "filters": [
                    {
                        "values": [
                            {"value": "VENDOR-16", "id": "16"},
                            {"value": "VENDOR-4", "id": "4"},
                            {"value": "VENDOR-10", "id": "10"},
                            {"value": "VENDOR-11", "id": "11"},
                            {"value": "VENDOR-12", "id": "12"},
                            {"value": "VENDOR-13", "id": "13"},
                            {"value": "VENDOR-14", "id": "14"},
                            {"value": "VENDOR-15", "id": "15"},
                            {"value": "VENDOR-6", "id": "6"},
                            {"value": "VENDOR-7", "id": "7"},
                            {"value": "VENDOR-8", "id": "8"},
                            {"value": "VENDOR-9", "id": "9"},
                        ],
                        "id": "100500",
                    }
                ]
            },
        )

        response = self.report.request_json('place=prime&hid=102&glfilter=100500:4,16&showVendors=all')
        # Show all 16 vendors with 4 and 16 moved to the top.
        self.assertFragmentIn(
            response,
            {
                "filters": [
                    {
                        "values": [
                            {"value": "VENDOR-16", "id": "16"},
                            {"value": "VENDOR-4", "id": "4"},
                            {"value": "VENDOR-10", "id": "10"},
                            {"value": "VENDOR-11", "id": "11"},
                            {"value": "VENDOR-12", "id": "12"},
                            {"value": "VENDOR-13", "id": "13"},
                            {"value": "VENDOR-14", "id": "14"},
                            {"value": "VENDOR-15", "id": "15"},
                            {"value": "VENDOR-6", "id": "6"},
                            {"value": "VENDOR-7", "id": "7"},
                            {"value": "VENDOR-8", "id": "8"},
                            {"value": "VENDOR-9", "id": "9"},
                            {"value": "VENDOR-1", "id": "1"},
                            {"value": "VENDOR-2", "id": "2"},
                            {"value": "VENDOR-3", "id": "3"},
                            {"value": "VENDOR-5", "id": "5"},
                        ],
                        "id": "100500",
                    }
                ]
            },
        )

    def test_prime_popular_vendors(self):
        response = self.report.request_json('place=prime&hid=103')

        # The top_value vendors (105, 106, 107) come first, then come the vendors
        # with models, then the others.
        self.assertFragmentIn(
            response,
            {
                "id": "2000000",
                "values": [
                    {"id": "105"},
                    {"id": "106"},
                    {"id": "107"},
                    {"id": "108"},
                    {"id": "109"},
                    {"id": "133"},
                    {"id": "134"},
                    {"id": "135"},
                    {"id": "136"},
                    {"id": "137"},
                    {"id": "138"},
                    {"id": "139"},
                ],
            },
            preserve_order=True,
        )


if __name__ == '__main__':
    main()
