#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    Offer,
    GLValue,
    GLType,
    GLParam,
)
from core.testcase import TestCase, main
from core.matcher import Absent


class _C:

    hid_sort = 101
    param_id_sort = 2001
    value_id_base_sort = 100

    hid_intop = 102
    param_id_intop = 2002
    value_id_base_intop = 200

    hid_intop_m = 103
    param_id_intop_m = 2003
    value_id_base_intop_m = 300

    hid_o = 104
    param_id_o = 2004
    value_id_base_o = 400


class PI:
    def __init__(self, text, intop=False, offers_count=1):
        self.text = text
        self.intop = intop
        self.offers_count = offers_count


def make_glvalue_query_ext(param_id, values):
    if values:
        res = '&glfilter={}:{}'.format(param_id, ','.join(str(v) for v in values))
    else:
        res = ''
    return res


def make_glfilter_background(cls, hid_, param_id, value_id_base, param_info, top_enum_count=None):
    values_range = [
        GLValue(value_id=value_id_base + idx, text=pinfo.text, short_enum_intop=pinfo.intop)
        for idx, pinfo in enumerate(param_info, start=1)
    ]

    cls.index.gltypes += [
        GLType(
            param_id=param_id,
            hid=hid_,
            gltype=GLType.ENUM,
            cluster_filter=True,
            short_enum_count=top_enum_count,
            values=values_range,
        )
    ]

    for i, pinfo in enumerate(param_info):
        val = values_range[i]
        offers_count = pinfo.offers_count

        for n in range(offers_count):
            cls.index.offers += [
                Offer(
                    hid=hid_,
                    title="offer_{} [{}/{}]".format(hid_, n + 1, offers_count),
                    cpa=Offer.CPA_REAL,
                    price=1000 + (i + 1) * 10,
                    glparams=[
                        GLParam(param_id=param_id, value=val.get_option_id()),
                    ],
                )
            ]


class T(TestCase):
    """
    Тестирование сортировки элементов gl-фильтра.
    """

    @classmethod
    def prepare(cls):
        cls.disable_check_empty_output()

    @classmethod
    def prepare_sort(cls):
        param_info = [
            PI('1 ТБ', False, 1),
            PI('8 ГБ', False, 2),
            PI('4 ГБ', False, 3),
            PI('2 ГБ', False, 4),
            PI('1 ГБ', False, 5),
            PI('16 МБ', False, 6),
            PI('8 МБ', False, 7),
            PI('4 МБ', False, 8),
            PI('2 МБ', False, 9),
            PI('1 МБ', False, 10),
            PI('1 кБ', False, 11),
        ]
        make_glfilter_background(cls, _C.hid_sort, _C.param_id_sort, _C.value_id_base_sort, param_info)

    def test_sort(self):
        """
        Проверяем порядок элементов фильтра
        """

        query = 'place=prime&hid={}'.format(_C.hid_sort)
        response = self.report.request_json(query)

        assert _C.value_id_base_sort == 100
        self.assertFragmentIn(
            response,
            {
                'id': str(_C.param_id_sort),
                'type': 'enum',
                'values': [
                    {'value': '1 кБ', 'id': '111', 'checked': Absent()},
                    {'value': '1 МБ', 'id': '110', 'checked': Absent()},
                    {'value': '2 МБ', 'id': '109', 'checked': Absent()},
                    {'value': '4 МБ', 'id': '108', 'checked': Absent()},
                    {'value': '8 МБ', 'id': '107', 'checked': Absent()},
                    {'value': '16 МБ', 'id': '106', 'checked': Absent()},
                    {'value': '1 ГБ', 'id': '105', 'checked': Absent()},
                    {'value': '2 ГБ', 'id': '104', 'checked': Absent()},
                    {'value': '4 ГБ', 'id': '103', 'checked': Absent()},
                    {'value': '8 ГБ', 'id': '102', 'checked': Absent()},
                    {'value': '1 ТБ', 'id': '101', 'checked': Absent()},
                ],
                'valuesGroups': [
                    {
                        'type': 'top',
                        'valuesIds': [
                            '111',
                            '110',
                            '109',
                            '108',
                            '107',
                        ],
                    },
                    {
                        'type': 'all',
                        'valuesIds': [
                            '111',
                            '110',
                            '109',
                            '108',
                            '107',
                            '106',
                            '105',
                            '104',
                            '103',
                            '102',
                            '101',
                        ],
                    },
                ],
            },
            preserve_order=True,
            allow_different_len=False,
        )

    def test_sort_checked(self):
        """
        Проверяем порядок элементов с учетом выбора
        """

        query = 'place=prime&hid={}'.format(_C.hid_sort)
        query_ext = make_glvalue_query_ext(_C.param_id_sort, [102, 108])
        response = self.report.request_json(query + query_ext)

        assert _C.value_id_base_sort == 100
        self.assertFragmentIn(
            response,
            {
                'id': str(_C.param_id_sort),
                'type': 'enum',
                'values': [
                    {'value': '1 кБ', 'id': '111', 'checked': Absent()},
                    {'value': '1 МБ', 'id': '110', 'checked': Absent()},
                    {'value': '2 МБ', 'id': '109', 'checked': Absent()},
                    {'value': '4 МБ', 'id': '108', 'checked': True},
                    {'value': '8 МБ', 'id': '107', 'checked': Absent()},
                    {'value': '16 МБ', 'id': '106', 'checked': Absent()},
                    {'value': '1 ГБ', 'id': '105', 'checked': Absent()},
                    {'value': '2 ГБ', 'id': '104', 'checked': Absent()},
                    {'value': '4 ГБ', 'id': '103', 'checked': Absent()},
                    {'value': '8 ГБ', 'id': '102', 'checked': True},
                    {'value': '1 ТБ', 'id': '101', 'checked': Absent()},
                ],
                'valuesGroups': [
                    {
                        'type': 'top',
                        'valuesIds': [
                            '108',
                            '102',
                            '111',
                            '110',
                            '109',
                            '107',
                        ],
                    },
                    {
                        'type': 'all',
                        'valuesIds': [
                            '111',
                            '110',
                            '109',
                            '108',
                            '107',
                            '106',
                            '105',
                            '104',
                            '103',
                            '102',
                            '101',
                        ],
                    },
                ],
            },
            preserve_order=True,
            allow_different_len=False,
        )

    def test_sort_checked_more_then_max(self):
        """
        Проверяем порядок элементов с учетом выбора когда выбрано больше чем мест в top
        """

        query = 'place=prime&hid={}'.format(_C.hid_sort)
        query_ext = make_glvalue_query_ext(_C.param_id_sort, [102, 103, 107, 108, 109, 110])
        response = self.report.request_json(query + query_ext)

        assert _C.value_id_base_sort == 100
        self.assertFragmentIn(
            response,
            {
                'id': str(_C.param_id_sort),
                'type': 'enum',
                'values': [
                    {'value': '1 кБ', 'id': '111', 'checked': Absent()},
                    {'value': '1 МБ', 'id': '110', 'checked': True},
                    {'value': '2 МБ', 'id': '109', 'checked': True},
                    {'value': '4 МБ', 'id': '108', 'checked': True},
                    {'value': '8 МБ', 'id': '107', 'checked': True},
                    {'value': '16 МБ', 'id': '106', 'checked': Absent()},
                    {'value': '1 ГБ', 'id': '105', 'checked': Absent()},
                    {'value': '2 ГБ', 'id': '104', 'checked': Absent()},
                    {'value': '4 ГБ', 'id': '103', 'checked': True},
                    {'value': '8 ГБ', 'id': '102', 'checked': True},
                    {'value': '1 ТБ', 'id': '101', 'checked': Absent()},
                ],
                'valuesGroups': [
                    {
                        'type': 'top',
                        'valuesIds': [
                            '110',
                            '109',
                            '108',
                            '107',
                            '103',
                            '102',
                            '111',
                        ],
                    },
                    {
                        'type': 'all',
                        'valuesIds': [
                            '111',
                            '110',
                            '109',
                            '108',
                            '107',
                            '106',
                            '105',
                            '104',
                            '103',
                            '102',
                            '101',
                        ],
                    },
                ],
            },
            preserve_order=True,
            allow_different_len=False,
        )

    def test_sort_checked_more_then_max_no_top(self):
        """
        Проверяем порядок элементов с учетом выбора когда выбрано больше чем мест в top и когда top не отображается
        """

        query = 'place=prime&hid={}'.format(_C.hid_sort)
        query_ext = make_glvalue_query_ext(_C.param_id_sort, [101, 102, 103, 104, 105, 108])
        response = self.report.request_json(query + query_ext)

        assert _C.value_id_base_sort == 100
        self.assertFragmentIn(
            response,
            {
                'id': str(_C.param_id_sort),
                'type': 'enum',
                'values': [
                    {'value': '1 кБ', 'id': '111', 'checked': Absent()},
                    {'value': '1 МБ', 'id': '110', 'checked': Absent()},
                    {'value': '2 МБ', 'id': '109', 'checked': Absent()},
                    {'value': '4 МБ', 'id': '108', 'checked': True},
                    {'value': '8 МБ', 'id': '107', 'checked': Absent()},
                    {'value': '16 МБ', 'id': '106', 'checked': Absent()},
                    {'value': '1 ГБ', 'id': '105', 'checked': True},
                    {'value': '2 ГБ', 'id': '104', 'checked': True},
                    {'value': '4 ГБ', 'id': '103', 'checked': True},
                    {'value': '8 ГБ', 'id': '102', 'checked': True},
                    {'value': '1 ТБ', 'id': '101', 'checked': True},
                ],
                'valuesGroups': [
                    {
                        'type': 'all',
                        'valuesIds': [
                            '111',
                            '110',
                            '109',
                            '108',
                            '107',
                            '106',
                            '105',
                            '104',
                            '103',
                            '102',
                            '101',
                        ],
                    },
                ],
            },
            preserve_order=True,
            allow_different_len=False,
        )
        self.assertFragmentNotIn(response, {'valuesGroups': [{'type': 'top'}]})

    def test_sort_checked_and_active(self):
        """
        Проверяем порядок элементов с учетом выбора и активности (деактивация элементов фильтром по цене)
        """

        query = 'place=prime&hid={}'.format(_C.hid_sort)
        query_ext = make_glvalue_query_ext(_C.param_id_sort, [102, 110])
        query_ext += "&mcpricefrom=1030&mcpriceto=1040"
        response = self.report.request_json(query + query_ext)

        assert _C.value_id_base_sort == 100
        self.assertFragmentIn(
            response,
            {
                'id': str(_C.param_id_sort),
                'type': 'enum',
                'values': [
                    {'value': '1 кБ', 'id': '111', 'checked': Absent()},
                    {'value': '1 МБ', 'id': '110', 'checked': True},
                    {'value': '2 МБ', 'id': '109', 'checked': Absent()},
                    {'value': '4 МБ', 'id': '108', 'checked': Absent()},
                    {'value': '8 МБ', 'id': '107', 'checked': Absent()},
                    {'value': '16 МБ', 'id': '106', 'checked': Absent()},
                    {'value': '1 ГБ', 'id': '105', 'checked': Absent()},
                    {'value': '2 ГБ', 'id': '104', 'checked': Absent()},
                    {'value': '4 ГБ', 'id': '103', 'checked': Absent()},
                    {'value': '8 ГБ', 'id': '102', 'checked': True},
                    {'value': '1 ТБ', 'id': '101', 'checked': Absent()},
                ],
                'valuesGroups': [
                    {
                        'type': 'top',
                        'valuesIds': [
                            '110',
                            '102',
                            '111',
                            '109',
                            '104',
                            '103',
                        ],
                    },
                    {
                        'type': 'all',
                        'valuesIds': [
                            '111',
                            '110',
                            '109',
                            '108',
                            '107',
                            '106',
                            '105',
                            '104',
                            '103',
                            '102',
                            '101',
                        ],
                    },
                ],
            },
            preserve_order=True,
            allow_different_len=False,
        )

    def test_sort_active_more_then_max(self):
        """
        Проверяем порядок элементов с учетом активности (активно элементов больше, чем мест в top)
        """

        query = 'place=prime&hid={}'.format(_C.hid_sort)
        query_ext = "&mcpriceto=1070"
        response = self.report.request_json(query + query_ext)

        assert _C.value_id_base_sort == 100
        self.assertFragmentIn(
            response,
            {
                'id': str(_C.param_id_sort),
                'type': 'enum',
                'values': [
                    {'value': '1 кБ', 'id': '111', 'checked': Absent()},
                    {'value': '1 МБ', 'id': '110', 'checked': Absent()},
                    {'value': '2 МБ', 'id': '109', 'checked': Absent()},
                    {'value': '4 МБ', 'id': '108', 'checked': Absent()},
                    {'value': '8 МБ', 'id': '107', 'checked': Absent()},
                    {'value': '16 МБ', 'id': '106', 'checked': Absent()},
                    {'value': '1 ГБ', 'id': '105', 'checked': Absent()},
                    {'value': '2 ГБ', 'id': '104', 'checked': Absent()},
                    {'value': '4 ГБ', 'id': '103', 'checked': Absent()},
                    {'value': '8 ГБ', 'id': '102', 'checked': Absent()},
                    {'value': '1 ТБ', 'id': '101', 'checked': Absent()},
                ],
                'valuesGroups': [
                    {
                        'type': 'top',
                        'valuesIds': [
                            '107',
                            '106',
                            '105',
                            '104',
                            '103',
                        ],
                    },
                    {
                        'type': 'all',
                        'valuesIds': [
                            '111',
                            '110',
                            '109',
                            '108',
                            '107',
                            '106',
                            '105',
                            '104',
                            '103',
                            '102',
                            '101',
                        ],
                    },
                ],
            },
            preserve_order=True,
            allow_different_len=False,
        )

    @classmethod
    def prepare_intop(cls):
        param_info = [
            PI('1 ТБ', True, 1),
            PI('8 ГБ', False, 2),
            PI('4 ГБ', False, 3),
            PI('2 ГБ', False, 4),
            PI('1 ГБ', False, 5),
            PI('16 МБ', False, 6),
            PI('8 МБ', False, 7),
            PI('4 МБ', False, 8),
            PI('2 МБ', False, 9),
            PI('1 МБ', True, 10),
            PI('1 кБ', False, 11),
        ]
        make_glfilter_background(
            cls, _C.hid_intop, _C.param_id_intop, _C.value_id_base_intop, param_info, top_enum_count=6
        )

    def test_intop(self):
        """
        Проверяем порядок элементов с учетом флага InTop
        """

        query = 'place=prime&hid={}'.format(_C.hid_intop)
        response = self.report.request_json(query)

        assert _C.value_id_base_intop == 200
        self.assertFragmentIn(
            response,
            {
                'id': str(_C.param_id_intop),
                'type': 'enum',
                'values': [
                    {'value': '1 кБ', 'id': '211', 'checked': Absent()},
                    {'value': '1 МБ', 'id': '210', 'checked': Absent()},
                    {'value': '2 МБ', 'id': '209', 'checked': Absent()},
                    {'value': '4 МБ', 'id': '208', 'checked': Absent()},
                    {'value': '8 МБ', 'id': '207', 'checked': Absent()},
                    {'value': '16 МБ', 'id': '206', 'checked': Absent()},
                    {'value': '1 ГБ', 'id': '205', 'checked': Absent()},
                    {'value': '2 ГБ', 'id': '204', 'checked': Absent()},
                    {'value': '4 ГБ', 'id': '203', 'checked': Absent()},
                    {'value': '8 ГБ', 'id': '202', 'checked': Absent()},
                    {'value': '1 ТБ', 'id': '201', 'checked': Absent()},
                ],
                'valuesGroups': [
                    {
                        'type': 'top',
                        'valuesIds': [
                            '211',
                            '210',
                            '209',
                            '208',
                            '207',
                            '201',
                        ],
                    },
                    {
                        'type': 'all',
                        'valuesIds': [
                            '211',
                            '210',
                            '209',
                            '208',
                            '207',
                            '206',
                            '205',
                            '204',
                            '203',
                            '202',
                            '201',
                        ],
                    },
                ],
            },
            preserve_order=True,
            allow_different_len=False,
        )

    def test_intop_checked(self):
        """
        Проверяем порядок элементов с учетом флага InTop и выбора
        """

        query = 'place=prime&hid={}'.format(_C.hid_intop)
        query_ext = make_glvalue_query_ext(_C.param_id_intop, [202, 210])
        response = self.report.request_json(query + query_ext)

        assert _C.value_id_base_intop == 200
        self.assertFragmentIn(
            response,
            {
                'id': str(_C.param_id_intop),
                'type': 'enum',
                'values': [
                    {'value': '1 кБ', 'id': '211', 'checked': Absent()},
                    {'value': '1 МБ', 'id': '210', 'checked': True},
                    {'value': '2 МБ', 'id': '209', 'checked': Absent()},
                    {'value': '4 МБ', 'id': '208', 'checked': Absent()},
                    {'value': '8 МБ', 'id': '207', 'checked': Absent()},
                    {'value': '16 МБ', 'id': '206', 'checked': Absent()},
                    {'value': '1 ГБ', 'id': '205', 'checked': Absent()},
                    {'value': '2 ГБ', 'id': '204', 'checked': Absent()},
                    {'value': '4 ГБ', 'id': '203', 'checked': Absent()},
                    {'value': '8 ГБ', 'id': '202', 'checked': True},
                    {'value': '1 ТБ', 'id': '201', 'checked': Absent()},
                ],
                'valuesGroups': [
                    {
                        'type': 'top',
                        'valuesIds': [
                            '210',
                            '202',
                            '211',
                            '209',
                            '208',
                            '207',
                            '201',
                        ],
                    },
                    {
                        'type': 'all',
                        'valuesIds': [
                            '211',
                            '210',
                            '209',
                            '208',
                            '207',
                            '206',
                            '205',
                            '204',
                            '203',
                            '202',
                            '201',
                        ],
                    },
                ],
            },
            preserve_order=True,
            allow_different_len=False,
        )

    def test_intop_checked_active(self):
        """
        Проверяем порядок элементов с учетом флага InTop, выбора и активности (деактивация элементов фильтром по цене)
        """

        query = 'place=prime&hid={}'.format(_C.hid_intop)
        query_ext = make_glvalue_query_ext(_C.param_id_intop, [202, 210])
        query_ext += "&mcpricefrom=1030&mcpriceto=1040"
        response = self.report.request_json(query + query_ext)

        assert _C.value_id_base_intop == 200
        self.assertFragmentIn(
            response,
            {
                'id': str(_C.param_id_intop),
                'type': 'enum',
                'values': [
                    {'value': '1 кБ', 'id': '211', 'checked': Absent()},
                    {'value': '1 МБ', 'id': '210', 'checked': True},
                    {'value': '2 МБ', 'id': '209', 'checked': Absent()},
                    {'value': '4 МБ', 'id': '208', 'checked': Absent()},
                    {'value': '8 МБ', 'id': '207', 'checked': Absent()},
                    {'value': '16 МБ', 'id': '206', 'checked': Absent()},
                    {'value': '1 ГБ', 'id': '205', 'checked': Absent()},
                    {'value': '2 ГБ', 'id': '204', 'checked': Absent()},
                    {'value': '4 ГБ', 'id': '203', 'checked': Absent()},
                    {'value': '8 ГБ', 'id': '202', 'checked': True},
                    {'value': '1 ТБ', 'id': '201', 'checked': Absent()},
                ],
                'valuesGroups': [
                    {
                        'type': 'top',
                        'valuesIds': [
                            '210',
                            '202',
                            '211',
                            '209',
                            '204',
                            '203',
                            '201',
                        ],
                    },
                    {
                        'type': 'all',
                        'valuesIds': [
                            '211',
                            '210',
                            '209',
                            '208',
                            '207',
                            '206',
                            '205',
                            '204',
                            '203',
                            '202',
                            '201',
                        ],
                    },
                ],
            },
            preserve_order=True,
            allow_different_len=False,
        )

    @classmethod
    def prepare_intop_m(cls):
        param_info = [
            PI('1.8 кг', True, 1),
            PI('1,5 кг', False, 2),
            PI('1. кг', False, 3),
            PI('500 г', True, 4),
            PI('100 г', True, 5),
            PI('40 мг', False, 6),
            PI('1 мкг', True, 7),
            PI('0.5 мкг', False, 8),
        ]
        make_glfilter_background(
            cls, _C.hid_intop_m, _C.param_id_intop_m, _C.value_id_base_intop_m, param_info, top_enum_count=3
        )

    def test_intop_too_much(self):
        """
        Проверяем порядок элементов когда элементов с флагом InTop больше чем мест в top
        """

        query = 'place=prime&hid={}'.format(_C.hid_intop_m)
        response = self.report.request_json(query)

        assert _C.value_id_base_intop_m == 300
        self.assertFragmentIn(
            response,
            {
                'id': str(_C.param_id_intop_m),
                'type': 'enum',
                'values': [
                    {'value': '0.5 мкг', 'id': '308'},
                    {'value': '1 мкг', 'id': '307'},
                    {'value': '40 мг', 'id': '306'},
                    {'value': '100 г', 'id': '305'},
                    {'value': '500 г', 'id': '304'},
                    {'value': '1. кг', 'id': '303'},
                    {'value': '1,5 кг', 'id': '302'},
                    {'value': '1.8 кг', 'id': '301'},
                ],
                'valuesGroups': [
                    {
                        'type': 'top',
                        'valuesIds': [
                            '307',
                            '305',
                            '304',
                        ],
                    },
                    {
                        'type': 'all',
                        'valuesIds': [
                            '308',
                            '307',
                            '306',
                            '305',
                            '304',
                            '303',
                            '302',
                            '301',
                        ],
                    },
                ],
            },
            preserve_order=True,
            allow_different_len=False,
        )

    def test_top_to_all(self):
        """
        Проверяем переход от top+all к all_only
        """

        query = 'place=prime&hid={}'.format(_C.hid_intop_m)
        response = self.report.request_json(query)
        self.assertFragmentIn(
            response,
            {
                'id': str(_C.param_id_intop_m),
                'valuesGroups': [
                    {'type': 'top', 'valuesIds': ['307', '305', '304']},
                    {'type': 'all', 'valuesIds': ['308', '307', '306', '305', '304', '303', '302', '301']},
                ],
            },
            preserve_order=True,
            allow_different_len=False,
        )

        query = 'place=prime&hid={}'.format(_C.hid_intop_m)
        query_ext = make_glvalue_query_ext(_C.param_id_intop_m, [301])
        response = self.report.request_json(query + query_ext)
        self.assertFragmentIn(
            response,
            {
                'id': str(_C.param_id_intop_m),
                'valuesGroups': [
                    {'type': 'top', 'valuesIds': ['301', '307', '305', '304']},
                    {'type': 'all', 'valuesIds': ['308', '307', '306', '305', '304', '303', '302', '301']},
                ],
            },
            preserve_order=True,
            allow_different_len=False,
        )

        query = 'place=prime&hid={}'.format(_C.hid_intop_m)
        query_ext = make_glvalue_query_ext(_C.param_id_intop_m, [301, 302])
        response = self.report.request_json(query + query_ext)
        self.assertFragmentIn(
            response,
            {
                'id': str(_C.param_id_intop_m),
                'valuesGroups': [
                    {'type': 'all', 'valuesIds': ['308', '307', '306', '305', '304', '303', '302', '301']},
                ],
            },
            preserve_order=True,
            allow_different_len=False,
        )

        query = 'place=prime&hid={}'.format(_C.hid_intop_m)
        query_ext = make_glvalue_query_ext(_C.param_id_intop_m, [301, 302, 303, 304, 305, 306, 307, 308])
        response = self.report.request_json(query + query_ext)
        self.assertFragmentIn(
            response,
            {
                'id': str(_C.param_id_intop_m),
                'valuesGroups': [
                    {'type': 'all', 'valuesIds': ['308', '307', '306', '305', '304', '303', '302', '301']},
                ],
            },
            preserve_order=True,
            allow_different_len=False,
        )

    @classmethod
    def prepare_offer_count(cls):
        param_info = [
            PI('1 м', False, 3),  # 401
            PI('2 м', False, 1),  # 402
            PI('1 мм', False, 1),  # 403
            PI('2 мм', False, 2),  # 404
            PI('1 км', False, 1),  # 405
            PI('2 км', False, 1),  # 406
            PI('1 см', False, 3),  # 407
            PI('2 см', False, 1),  # 408
        ]
        make_glfilter_background(cls, _C.hid_o, _C.param_id_o, _C.value_id_base_o, param_info, top_enum_count=4)

    def test_offer_count(self):
        """
        Проверяем порядок элементов, учитывая что в top должны попадать элементы с наибольшим количеством офферов.
        При этом сортировка в top не должна зависеть от количества офферов.
        """

        query = 'place=prime&hid={}'.format(_C.hid_o)
        response = self.report.request_json(query)

        assert _C.value_id_base_o == 400
        self.assertFragmentIn(
            response,
            {
                'id': str(_C.param_id_o),
                'type': 'enum',
                'values': [
                    {'value': '1 мм', 'id': '403'},
                    {'value': '2 мм', 'id': '404'},
                    {'value': '1 см', 'id': '407'},
                    {'value': '2 см', 'id': '408'},
                    {'value': '1 м', 'id': '401'},
                    {'value': '2 м', 'id': '402'},
                    {'value': '1 км', 'id': '405'},
                    {'value': '2 км', 'id': '406'},
                ],
                'valuesGroups': [
                    {
                        'type': 'top',
                        'valuesIds': [
                            '403',
                            '404',
                            '407',
                            '401',
                        ],
                    },
                    {
                        'type': 'all',
                        'valuesIds': [
                            '403',
                            '404',
                            '407',
                            '408',
                            '401',
                            '402',
                            '405',
                            '406',
                        ],
                    },
                ],
            },
            preserve_order=True,
            allow_different_len=False,
        )


if __name__ == '__main__':
    main()
