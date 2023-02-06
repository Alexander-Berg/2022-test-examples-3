#!/usr/bin/env python
# -*- coding: utf-8 -*-

import runner  # noqa

from core.types import (
    GLType,
    Model,
    MarketSku,
    BlueOffer,
    GLParam,
)
from core.testcase import TestCase, main
from core.matcher import Absent
from collections import namedtuple


class T(TestCase):
    @classmethod
    def prepare_mskus_with_and_without_full_set_of_2nd_kind_params(cls):
        cls.index.gltypes += [
            GLType(
                hid=1,
                param_id=1,
                name="Size",
                xslname="size",
                cluster_filter=True,
                gltype=GLType.ENUM,
                values=[30, 32],
                model_filter_index=1,
                position=10,
            ),
            GLType(
                hid=1,
                param_id=2,
                name="Height",
                xslname="height",
                cluster_filter=True,
                gltype=GLType.ENUM,
                values=[170, 175],
                model_filter_index=2,
                position=20,
            ),
        ]

        cls.index.models += [
            Model(
                hid=1,
                title="Model",
                hyperid=1,
            ),
        ]

        cls.index.mskus += [
            MarketSku(
                hid=1,
                hyperid=1,
                sku=1,
                title="Concrete MSKU 1",
                blue_offers=[
                    BlueOffer(ts=1, price=200),
                ],
                glparams=[
                    GLParam(param_id=1, value=30),
                    GLParam(param_id=2, value=170),
                ],
            ),
            MarketSku(
                hid=1,
                hyperid=1,
                sku=2,
                title="Concrete MSKU 2",
                blue_offers=[
                    BlueOffer(ts=2, price=200),
                ],
                glparams=[
                    GLParam(param_id=1, value=32),
                    GLParam(param_id=2, value=170),
                ],
            ),
            MarketSku(
                hid=1,
                hyperid=1,
                sku=3,
                title="Concrete MSKU 3",
                blue_offers=[
                    BlueOffer(ts=3, price=200),
                ],
                glparams=[
                    GLParam(param_id=1, value=30),
                    GLParam(param_id=2, value=175),
                ],
            ),
            MarketSku(
                hid=1,
                hyperid=1,
                sku=4,
                title="Concrete MSKU 4",
                blue_offers=[
                    BlueOffer(ts=4, price=200),
                ],
                glparams=[
                    GLParam(param_id=1, value=32),
                    GLParam(param_id=2, value=175),
                ],
            ),
            MarketSku(
                hid=1,
                hyperid=1,
                sku=5,
                title="Other Size 32 MSKU 1",
                blue_offers=[
                    BlueOffer(ts=5, price=200),
                ],
                glparams=[
                    GLParam(param_id=1, value=32),
                ],
            ),
            MarketSku(
                hid=1,
                hyperid=1,
                sku=6,
                title="Other Size 32 MSKU 2",
                blue_offers=[
                    BlueOffer(ts=6, price=200),
                ],
                glparams=[
                    GLParam(param_id=1, value=32),
                ],
            ),
            MarketSku(
                hid=1,
                hyperid=1,
                sku=7,
                title="Other Height 175 MSKU 1",
                blue_offers=[
                    BlueOffer(ts=7, price=200),
                ],
                glparams=[
                    GLParam(param_id=2, value=175),
                ],
            ),
            MarketSku(
                hid=1,
                hyperid=1,
                sku=8,
                title="Other All MSKU 1",
                blue_offers=[
                    BlueOffer(ts=8, price=200),
                ],
            ),
        ]

    # +======+=========================+======+========+=================+
    # | Msku |          Title          | Size | Height | Is Modification |
    # +======+=========================+======+========+=================+
    # |    1 | Concrete MSKU 1         | 30   | 170    | x               |
    # +------+-------------------------+------+--------+-----------------+
    # |    2 | Concrete MSKU 2         | 32   | 170    | x               |
    # +------+-------------------------+------+--------+-----------------+
    # |    3 | Concrete MSKU 3         | 30   | 175    | x               |
    # +------+-------------------------+------+--------+-----------------+
    # |    4 | Concrete MSKU 4         | 32   | 175    | x               |
    # +------+-------------------------+------+--------+-----------------+
    # |    5 | Other Size 32 MSKU 1    | 32   | ?      | ✓               |
    # +------+-------------------------+------+--------+-----------------+
    # |    6 | Other Size 32 MSKU 2    | 32   | ?      | ✓               |
    # +------+-------------------------+------+--------+-----------------+
    # |    7 | Other Height 175 MSKU 1 | ?    | 175    | ✓               |
    # +------+-------------------------+------+--------+-----------------+
    # |    8 | Other All MSKU 1        | ?    | ?      | ✓               |
    # +------+-------------------------+------+--------+-----------------+

    def test_jump_table_with_modifications(self):

        concerete_msku_1 = 1
        concerete_msku_2 = 2
        concerete_msku_3 = 3
        concerete_msku_4 = 4
        other_size_32_msku_1 = 5
        other_size_32_msku_2 = 6
        other_height_175_msku_1 = 7
        other_all_msku_1 = 8

        v = namedtuple('JumpTableValue', ['sku', 'fuzzy'])

        jump_table_values_for_size_30 = [
            v(concerete_msku_1, 0),  # 1
            v(concerete_msku_1, 0),  # 2
            v(concerete_msku_3, 0),  # 3
            v(concerete_msku_3, 0),  # 4
            v(concerete_msku_1, 1),  # 5
            v(concerete_msku_1, 1),  # 6
            v(concerete_msku_3, 0),  # 7
            v(concerete_msku_1, 1),  # 8
        ]

        jump_table_values_for_size_32 = [
            v(concerete_msku_2, 0),  # 1
            v(concerete_msku_2, 0),  # 2
            v(concerete_msku_4, 0),  # 3
            v(concerete_msku_4, 0),  # 4
            v(other_size_32_msku_1, 0),  # 5
            v(other_size_32_msku_2, 0),  # 6
            v(concerete_msku_4, 0),  # 7
            v(other_size_32_msku_1, 0),  # 8
        ]

        jump_table_values_for_size_other = [
            v(other_height_175_msku_1, 1),  # 1
            v(other_height_175_msku_1, 1),  # 2
            v(other_height_175_msku_1, 0),  # 3
            v(other_height_175_msku_1, 0),  # 4
            v(other_all_msku_1, 0),  # 5
            v(other_all_msku_1, 0),  # 6
            v(other_height_175_msku_1, 0),  # 7
            v(other_all_msku_1, 0),  # 8
        ]

        jump_table_values_for_height_170 = [
            v(concerete_msku_1, 0),  # 1
            v(concerete_msku_2, 0),  # 2
            v(concerete_msku_1, 0),  # 3
            v(concerete_msku_2, 0),  # 4
            v(concerete_msku_2, 0),  # 5
            v(concerete_msku_2, 0),  # 6
            v(concerete_msku_1, 1),  # 7
            v(concerete_msku_1, 1),  # 8
        ]

        jump_table_values_for_height_175 = [
            v(concerete_msku_3, 0),  # 1
            v(concerete_msku_4, 0),  # 2
            v(concerete_msku_3, 0),  # 3
            v(concerete_msku_4, 0),  # 4
            v(concerete_msku_4, 0),  # 5
            v(concerete_msku_4, 0),  # 6
            v(other_height_175_msku_1, 0),  # 7
            v(other_height_175_msku_1, 0),  # 8
        ]

        jump_table_values_for_height_other = [
            v(other_all_msku_1, 1),  # 1
            v(other_size_32_msku_1, 0),  # 2
            v(other_all_msku_1, 1),  # 3
            v(other_size_32_msku_1, 0),  # 4
            v(other_size_32_msku_1, 0),  # 5
            v(other_size_32_msku_2, 0),  # 6
            v(other_all_msku_1, 0),  # 7
            v(other_all_msku_1, 0),  # 8
        ]

        modifications = {
            other_size_32_msku_1: {
                other_size_32_msku_1: 'Other Size 32 MSKU 1',
                other_size_32_msku_2: 'Other Size 32 MSKU 2',
            },
            other_size_32_msku_2: {
                other_size_32_msku_1: 'Other Size 32 MSKU 1',
                other_size_32_msku_2: 'Other Size 32 MSKU 2',
            },
            other_height_175_msku_1: {other_height_175_msku_1: 'Other Height 175 MSKU 1'},
            other_all_msku_1: {other_all_msku_1: 'Other All MSKU 1'},
        }

        request_no_msku = 'place=productoffers&hyperid=1&rearr-factors=market_jumptable_show_modifications=1&hid=1&debug=da&onstock=0&add-sku-stats=1'

        # Проверяем карту переходов с модификациями для каждой мску
        for i in range(8):
            selected_msku = i + 1
            request = request_no_msku + '&market-sku=' + str(selected_msku)
            response = self.report.request_json(request)

            self.assertFragmentIn(
                response,
                {
                    "filters": [
                        {
                            "id": "1",
                            "values": [
                                {
                                    "id": "30",
                                    "marketSku": str(jump_table_values_for_size_30[i].sku),
                                    "value": "VALUE-30",
                                    "checked": True
                                    if jump_table_values_for_size_30[i].sku == selected_msku
                                    else Absent(),
                                    "fuzzy": True if jump_table_values_for_size_30[i].fuzzy else Absent(),
                                },
                                {
                                    "id": "32",
                                    "marketSku": str(jump_table_values_for_size_32[i].sku),
                                    "value": "VALUE-32",
                                    "checked": True
                                    if jump_table_values_for_size_32[i].sku == selected_msku
                                    else Absent(),
                                    "fuzzy": True if jump_table_values_for_size_32[i].fuzzy else Absent(),
                                },
                                {
                                    "id": "1_Other",
                                    "marketSku": str(jump_table_values_for_size_other[i].sku),
                                    "value": "Другое",
                                    "checked": True
                                    if jump_table_values_for_size_other[i].sku == selected_msku
                                    else Absent(),
                                    "fuzzy": True if jump_table_values_for_size_other[i].fuzzy else Absent(),
                                },
                            ],
                        },
                        {
                            "id": "2",
                            "values": [
                                {
                                    "id": "170",
                                    "marketSku": str(jump_table_values_for_height_170[i].sku),
                                    "value": "VALUE-170",
                                    "checked": True
                                    if jump_table_values_for_height_170[i].sku == selected_msku
                                    else Absent(),
                                    "fuzzy": True if jump_table_values_for_height_170[i].fuzzy else Absent(),
                                },
                                {
                                    "id": "175",
                                    "marketSku": str(jump_table_values_for_height_175[i].sku),
                                    "value": "VALUE-175",
                                    "checked": True
                                    if jump_table_values_for_height_175[i].sku == selected_msku
                                    else Absent(),
                                    "fuzzy": True if jump_table_values_for_height_175[i].fuzzy else Absent(),
                                },
                                {
                                    "id": "2_Other",
                                    "marketSku": str(jump_table_values_for_height_other[i].sku),
                                    "value": "Другое",
                                    "checked": True
                                    if jump_table_values_for_height_other[i].sku == selected_msku
                                    else Absent(),
                                    "fuzzy": True if jump_table_values_for_height_other[i].fuzzy else Absent(),
                                },
                            ],
                        },
                    ]
                },
            )

            if selected_msku in modifications and len(modifications[selected_msku]) > 1:
                self.assertFragmentIn(
                    response,
                    {
                        "filters": [
                            {
                                "id": "modifications",
                                "valuesCount": len(modifications[selected_msku]),
                                "values": [
                                    {
                                        "value": modification[1],
                                        "marketSku": str(modification[0]),
                                        "id": str(modification[0]),
                                        "checked": True if modification[0] == selected_msku else Absent(),
                                    }
                                    for modification in modifications[selected_msku].items()
                                ],
                            }
                        ]
                    },
                )
            else:
                self.assertFragmentNotIn(response, {"filters": [{"id": "modifications"}]})


if __name__ == '__main__':
    main()
