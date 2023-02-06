#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import Offer

from core.testcase import TestCase, main

from core.matcher import Round


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.disable_check_empty_output()
        cls.settings.report_subrole = 'goods'
        cls.index.offers += [
            Offer(
                title='offer_bucket_region',
                offer_region_source_flag=1,
                offerid=1,
                feedid=1,
            ),
        ]
        cls.index.offers += [
            Offer(title='offer_shops_dat_region', offer_region_source_flag=2),
        ]
        cls.index.offers += [
            Offer(title='offer_external_table_region', offer_region_source_flag=4),
        ]

        cls.index.offers += [
            Offer(title='offer_earth_region', offer_region_source_flag=8),
        ]

        cls.index.offers += [
            Offer(title='offer_unknown_region_source'),
        ]

    def test_region_sources(self):
        """
        Не дебустим те офферы, региональность которых нам известна из бакетов доставки или shops.dat,
        а также офферы с неизвестным источником регионов https://st.yandex-team.ru/ECOMQUALITY-211
        Дебустим оффера, региональность которых известна из внешней таблицы и те, которым проставили регион
        Земля
        """
        rearr_external_table = '&rearr-factors=goods_external_table_regions_base_boost_coeff=0.5&rearr-factors=goods_external_table_regions_meta_boost_coeff=0.1'
        rearr_earth = '&rearr-factors=goods_earth_regions_base_boost_coeff=0.5&rearr-factors=goods_earth_regions_meta_boost_coeff=0.1'
        request = 'place=prime&text=offer_bucket_region&debug=da'
        response = self.report.request_json(request + rearr_external_table + rearr_earth)

        self.assertFragmentIn(
            response,
            {
                'entity': 'offer',
                'titles': {'raw': 'offer_bucket_region'},
                'trace': {
                    'fullFormulaInfo': [{'tag': 'Default', 'value': '0.3'}],
                },
                'debug': {
                    'properties': {
                        'BOOST_MULTIPLIER': Round('1.0'),
                    },
                    'metaProperties': {
                        'BOOST_MULTIPLIER': Round('1.0'),
                    },
                },
            },
        )

        request = 'place=prime&text=offer_shops_dat_region&debug=da'
        response = self.report.request_json(request + rearr_external_table + rearr_earth)

        self.assertFragmentIn(
            response,
            {
                'entity': 'offer',
                'titles': {'raw': 'offer_bucket_region'},
                'trace': {
                    'fullFormulaInfo': [{'tag': 'Default', 'value': '0.3'}],
                },
                'debug': {
                    'properties': {
                        'BOOST_MULTIPLIER': Round('1.0'),
                    },
                    'metaProperties': {
                        'BOOST_MULTIPLIER': Round('1.0'),
                    },
                },
            },
        )

        request = 'place=prime&text=offer_external_table_region&debug=da'

        response = self.report.request_json(request + rearr_external_table)
        self.assertFragmentIn(
            response,
            {
                'entity': 'offer',
                'titles': {'raw': 'offer_external_table_region'},
                'trace': {
                    'fullFormulaInfo': [
                        {'tag': 'Default', 'value': '0.15'},
                    ],
                },
                'debug': {
                    'properties': {
                        'BOOST_MULTIPLIER': Round('0.5'),
                    },
                    'metaProperties': {
                        'BOOST_MULTIPLIER': Round('0.1'),
                    },
                },
            },
        )

        response = self.report.request_json(request + rearr_earth)
        self.assertFragmentIn(
            response,
            {
                'entity': 'offer',
                'titles': {'raw': 'offer_external_table_region'},
                'trace': {
                    'fullFormulaInfo': [{'tag': 'Default', 'value': '0.3'}],
                },
                'debug': {
                    'properties': {
                        'BOOST_MULTIPLIER': Round('1.0'),
                    },
                    'metaProperties': {
                        'BOOST_MULTIPLIER': Round('0.9'),
                    },
                },
            },
        )

        request = 'place=prime&text=offer_earth_region&debug=da'

        response = self.report.request_json(request + rearr_earth)
        self.assertFragmentIn(
            response,
            {
                'entity': 'offer',
                'titles': {'raw': 'offer_earth_region'},
                'trace': {
                    'fullFormulaInfo': [
                        {'tag': 'Default', 'value': '0.15'},
                    ],
                },
                'debug': {
                    'properties': {
                        'BOOST_MULTIPLIER': Round('0.5'),
                    },
                    'metaProperties': {
                        'BOOST_MULTIPLIER': Round('0.1'),
                    },
                },
            },
        )

        response = self.report.request_json(request + rearr_external_table)
        self.assertFragmentIn(
            response,
            {
                'entity': 'offer',
                'titles': {'raw': 'offer_earth_region'},
                'trace': {
                    'fullFormulaInfo': [{'tag': 'Default', 'value': '0.24'}],
                },
                'debug': {
                    'properties': {
                        'BOOST_MULTIPLIER': Round('0.8'),
                    },
                    'metaProperties': {
                        'BOOST_MULTIPLIER': Round('0.8'),
                    },
                },
            },
        )

        request = 'place=prime&text=offer_unknown_region_source&debug=da'
        response = self.report.request_json(request + rearr_external_table + rearr_earth)

        self.assertFragmentIn(
            response,
            {
                'entity': 'offer',
                'titles': {'raw': 'offer_unknown_region_source'},
                'trace': {
                    'fullFormulaInfo': [{'tag': 'Default', 'value': '0.3'}],
                },
                'debug': {
                    'properties': {
                        'BOOST_MULTIPLIER': Round('1.0'),
                    },
                    'metaProperties': {
                        'BOOST_MULTIPLIER': Round('1.0'),
                    },
                },
            },
        )

    def test_print_doc(self):
        """
        print_doc выводит источник региональности
        """
        response = self.report.request_json('place=print_doc&feed_shoffer_id=1-1&debug=1')
        self.assertFragmentIn(response, {"offerRegionSourceFlag": "1"})


if __name__ == '__main__':
    main()
