# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import json
from datetime import date

import pytest
import mock
from django.test import Client
from hamcrest import assert_that, has_entries, has_properties, contains_inanyorder

from common.models.schedule import Company
from common.models.tariffs import (
    TariffTypeCode, SuburbanTariffProvider, SuburbanSellingFlow, SuburbanSellingBarcodePreset
)
from travel.rasp.suburban_selling.selling.tariffs.interfaces import TariffKey
from travel.rasp.suburban_selling.selling.tariffs.selling_companies import SuburbanCarrierCode


pytestmark = [pytest.mark.mongouser, pytest.mark.dbuser]


GET_TARIFFS_RESULT = {
    'selling_tariffs': [
        {
            'provider': SuburbanTariffProvider.MOVISTA,
            'tariffs': [
                {
                    'id': 1,
                    'partner': SuburbanCarrierCode.CPPK,
                    'type': TariffTypeCode.EXPRESS,
                    'price': 560.11,
                    'name': TariffTypeCode.EXPRESS,
                    'max_days': 1,
                    'valid_from': '2020-10-24T00:00:00+03:00',
                    'valid_until': '2020-10-25T03:00:00+03:00',
                    'selling_flow': SuburbanSellingFlow.VALIDATOR
                },
            ]
        },
        {
            'provider': SuburbanTariffProvider.IM,
            'tariffs': [
                {
                    'id': 2,
                    'partner': SuburbanCarrierCode.MTPPK,
                    'type': TariffTypeCode.USUAL,
                    'price': 66.6,
                    'name': TariffTypeCode.USUAL,
                    'max_days': 1,
                    'valid_from': '2020-10-25T00:00:00+03:00',
                    'valid_until': '2020-10-26T03:00:00+03:00',
                    'selling_flow': SuburbanSellingFlow.SIMPLE
                },
                {
                    'id': 3,
                    'partner': SuburbanCarrierCode.MTPPK,
                    'type': TariffTypeCode.EXPRESS,
                    'price': 66.6,
                    'name': TariffTypeCode.EXPRESS,
                    'max_days': 1,
                    'valid_from': '2020-10-25T00:00:00+03:00',
                    'valid_until': '2020-10-26T03:00:00+03:00',
                    'selling_flow': SuburbanSellingFlow.SIMPLE
                },
                {
                    'id': 4,
                    'partner': SuburbanCarrierCode.BASHPPK,
                    'type': TariffTypeCode.USUAL,
                    'price': 77.7,
                    'name': TariffTypeCode.USUAL,
                    'max_days': 1,
                    'valid_from': '2020-10-25T00:00:00+03:00',
                    'valid_until': '2020-10-26T03:00:00+03:00',
                    'selling_flow': SuburbanSellingFlow.SIMPLE
                },
                {
                    'id': 5,
                    'partner': SuburbanCarrierCode.SODRUZHESTVO,
                    'type': TariffTypeCode.USUAL,
                    'price': 88.8,
                    'name': TariffTypeCode.USUAL,
                    'max_days': 1,
                    'valid_from': '2020-10-25T00:00:00+03:00',
                    'valid_until': '2020-10-26T03:00:00+03:00',
                    'selling_flow': SuburbanSellingFlow.SIMPLE
                },
            ]
        }
    ],
    'keys': [
        {
            'key': TariffKey(
                date=date(2020, 10, 24),
                station_from=42,
                station_to=43,
                company=Company.CPPK_ID,
                tariff_type=TariffTypeCode.EXPRESS
            ),
            'provider': SuburbanTariffProvider.MOVISTA,
            'tariff_ids': [1]
        },
        {
            'key': TariffKey(
                date=date(2020, 10, 25),
                station_from=42,
                station_to=43,
                company=Company.MTPPK_ID,
                tariff_type=TariffTypeCode.USUAL
            ),
            'provider': SuburbanTariffProvider.IM,
            'tariff_ids': [2]
        },
        {
            'key': TariffKey(
                date=date(2020, 10, 25),
                station_from=42,
                station_to=43,
                company=Company.MTPPK_ID,
                tariff_type=TariffTypeCode.EXPRESS
            ),
            'provider': SuburbanTariffProvider.IM,
            'tariff_ids': [3]
        },
        {
            'key': TariffKey(
                date=date(2020, 10, 25),
                station_from=42,
                station_to=43,
                company=Company.BASHPPK_ID,
                tariff_type=TariffTypeCode.USUAL
            ),
            'provider': SuburbanTariffProvider.IM,
            'tariff_ids': [4]
        },
        {
            'key': TariffKey(
                date=date(2020, 10, 25),
                station_from=42,
                station_to=43,
                company=Company.SODRUZHESTVO_ID,
                tariff_type=TariffTypeCode.USUAL
            ),
            'provider': SuburbanTariffProvider.IM,
            'tariff_ids': [5]
        },
    ],
    'selling_partners': [
        {
            'code': SuburbanCarrierCode.CPPK,
            'provider': SuburbanTariffProvider.MOVISTA,
            'title': 'ЦППК',
            'ogrn': 'ogrn_cppk',
            'address': 'address_cppk',
            'work_time': 'time_cppk'
        },
        {
            'code': SuburbanCarrierCode.MTPPK,
            'provider': SuburbanTariffProvider.IM,
            'title': 'МТППК',
            'ogrn': 'ogrn_mtppk',
            'address': 'address_mtppk',
            'work_time': 'time_mtppk'
        },
        {
            'code': SuburbanCarrierCode.BASHPPK,
            'provider': SuburbanTariffProvider.IM,
            'title': 'Башкирская ППК',
            'ogrn': 'ogrn_bashppk',
            'address': 'address_bashppk',
            'work_time': 'time_bashppk'
        },
        {
            'code': SuburbanCarrierCode.SODRUZHESTVO,
            'provider': SuburbanTariffProvider.IM,
            'title': 'ППК Содружество',
            'ogrn': 'ogrn_sodruzhestvo',
            'address': 'address_sodruzhestvo',
            'work_time': 'time_sodruzhestvo'
        }
    ]
}


def test_get_tariffs_view():
    data = {
        'keys': [
            {
                'date': '2020-10-24',
                'station_from': 42,
                'station_to': 43,
                'tariff_type': TariffTypeCode.EXPRESS,
                'company': Company.CPPK_ID
            },
            {
                'date': '2020-10-25',
                'station_from': 42,
                'station_to': 43,
                'tariff_type': TariffTypeCode.USUAL,
                'company': Company.MTPPK_ID
            },
            {
                'date': '2020-10-25',
                'station_from': 42,
                'station_to': 43,
                'tariff_type': TariffTypeCode.EXPRESS,
                'company': Company.MTPPK_ID
            },
            {
                'date': '2020-10-25',
                'station_from': 42,
                'station_to': 43,
                'tariff_type': TariffTypeCode.USUAL,
                'company': Company.BASHPPK_ID
            },
            {
                'date': '2020-10-25',
                'station_from': 42,
                'station_to': 43,
                'tariff_type': TariffTypeCode.USUAL,
                'company': Company.SODRUZHESTVO_ID
            },
        ],
        'selling_flows': list(SuburbanSellingFlow.ALL),
        'barcode_presets': [SuburbanSellingBarcodePreset.PDF417_CPPK, SuburbanSellingBarcodePreset.PDF417_SZPPK],
        'tariff_types': {'6666': 1, '7777': 2}
    }

    with mock.patch(
        'travel.rasp.suburban_selling.selling.tariffs.tariffs_views.get_tariffs_for_tariff_keys',
        return_value=GET_TARIFFS_RESULT
    ) as m_get_tariffs_for_tariff_keys:
        response = Client().post(
            '/get_tariffs/',
            data=json.dumps(data),
            content_type='application/json; charset=utf-8'
        )

        assert m_get_tariffs_for_tariff_keys.call_count == 1
        arg_0 = m_get_tariffs_for_tariff_keys.call_args_list[0][0][0]
        arg_1 = m_get_tariffs_for_tariff_keys.call_args_list[0][0][1]
        arg_2 = m_get_tariffs_for_tariff_keys.call_args_list[0][0][2]
        arg_3 = m_get_tariffs_for_tariff_keys.call_args_list[0][0][3]
        assert_that(arg_0, contains_inanyorder(
            has_properties({
                'date': date(2020, 10, 24),
                'station_from': 42,
                'station_to': 43,
                'tariff_type': TariffTypeCode.EXPRESS,
                'company': Company.CPPK_ID
            }),
            has_properties({
                'date': date(2020, 10, 25),
                'tariff_type': TariffTypeCode.USUAL,
                'company': Company.MTPPK_ID,
            }),
            has_properties({
                'date': date(2020, 10, 25),
                'tariff_type': TariffTypeCode.EXPRESS,
                'company': Company.MTPPK_ID
            }),
            has_properties({
                'date': date(2020, 10, 25),
                'tariff_type': TariffTypeCode.USUAL,
                'company': Company.BASHPPK_ID,
            }),
            has_properties({
                'date': date(2020, 10, 25),
                'tariff_type': TariffTypeCode.USUAL,
                'company': Company.SODRUZHESTVO_ID,
            }),
        ))
        assert_that(arg_1, contains_inanyorder(*data['selling_flows']))
        assert_that(arg_2, contains_inanyorder(*data['barcode_presets']))
        assert_that(arg_3, contains_inanyorder(*data['tariff_types']))

        assert response.status_code == 200

        response_json = json.loads(response.content)
        assert response_json['errors'] == {}
        result = response_json['result']

        assert_that(result, has_entries({
            'selling_tariffs': contains_inanyorder(
                has_entries({
                    'provider': SuburbanTariffProvider.MOVISTA,
                    'tariffs': contains_inanyorder(
                        has_entries({
                            'id': 1,
                            'partner': SuburbanCarrierCode.CPPK,
                            'type': TariffTypeCode.EXPRESS,
                            'name': TariffTypeCode.EXPRESS,
                            'price': 560.11,
                            'max_days': 1,
                            'valid_from': '2020-10-24T00:00:00+03:00',
                            'valid_until': '2020-10-25T03:00:00+03:00',
                            'selling_flow': SuburbanSellingFlow.VALIDATOR
                        }),
                    )
                }),
                has_entries({
                    'provider': SuburbanTariffProvider.IM,
                    'tariffs': contains_inanyorder(
                        has_entries({
                            'id': 2,
                            'partner': SuburbanCarrierCode.MTPPK,
                            'type': TariffTypeCode.USUAL,
                            'name': TariffTypeCode.USUAL,
                            'price': 66.6,
                            'max_days': 1,
                            'valid_from': '2020-10-25T00:00:00+03:00',
                            'valid_until': '2020-10-26T03:00:00+03:00',
                            'selling_flow': SuburbanSellingFlow.SIMPLE
                        }),
                        has_entries({
                            'id': 3,
                            'partner': SuburbanCarrierCode.MTPPK,
                            'type': TariffTypeCode.EXPRESS,
                            'name': TariffTypeCode.EXPRESS,
                            'price': 66.6,
                            'max_days': 1,
                            'valid_from': '2020-10-25T00:00:00+03:00',
                            'valid_until': '2020-10-26T03:00:00+03:00',
                            'selling_flow': SuburbanSellingFlow.SIMPLE
                        }),
                        has_entries({
                            'id': 4,
                            'partner': SuburbanCarrierCode.BASHPPK,
                            'type': TariffTypeCode.USUAL,
                            'name': TariffTypeCode.USUAL,
                            'price': 77.7,
                            'max_days': 1,
                            'valid_from': '2020-10-25T00:00:00+03:00',
                            'valid_until': '2020-10-26T03:00:00+03:00',
                            'selling_flow': SuburbanSellingFlow.SIMPLE
                        }),
                        has_entries({
                            'id': 5,
                            'partner': SuburbanCarrierCode.SODRUZHESTVO,
                            'type': TariffTypeCode.USUAL,
                            'name': TariffTypeCode.USUAL,
                            'price': 88.8,
                            'max_days': 1,
                            'valid_from': '2020-10-25T00:00:00+03:00',
                            'valid_until': '2020-10-26T03:00:00+03:00',
                            'selling_flow': SuburbanSellingFlow.SIMPLE
                        }),
                    )
                })
            ),

            'keys': contains_inanyorder(
                has_entries({
                    'key': has_entries({
                        'date': '2020-10-24',
                        'station_from': 42,
                        'station_to': 43,
                        'tariff_type': TariffTypeCode.EXPRESS,
                        'company': Company.CPPK_ID,
                    }),
                    'tariff_ids': [1]
                }),
                has_entries({
                    'key': has_entries({
                        'date': '2020-10-25',
                        'station_from': 42,
                        'station_to': 43,
                        'tariff_type': TariffTypeCode.USUAL,
                        'company': Company.MTPPK_ID,
                    }),
                    'tariff_ids': [2]
                }),
                has_entries({
                    'key': has_entries({
                        'date': '2020-10-25',
                        'station_from': 42,
                        'station_to': 43,
                        'tariff_type': TariffTypeCode.EXPRESS,
                        'company': Company.MTPPK_ID,
                    }),
                    'tariff_ids': [3]
                }),
                has_entries({
                    'key': has_entries({
                        'date': '2020-10-25',
                        'station_from': 42,
                        'station_to': 43,
                        'tariff_type': TariffTypeCode.USUAL,
                        'company': Company.BASHPPK_ID,
                    }),
                    'tariff_ids': [4]
                }),
                has_entries({
                    'key': has_entries({
                        'date': '2020-10-25',
                        'station_from': 42,
                        'station_to': 43,
                        'tariff_type': TariffTypeCode.USUAL,
                        'company': Company.SODRUZHESTVO_ID,
                    }),
                    'tariff_ids': [5]
                }),
            ),

            'selling_partners': contains_inanyorder(
                has_entries({
                    'provider': SuburbanTariffProvider.MOVISTA,
                    'code': SuburbanCarrierCode.CPPK
                }),
                has_entries({
                    'provider': SuburbanTariffProvider.IM,
                    'code': SuburbanCarrierCode.MTPPK
                }),
                has_entries({
                    'provider': SuburbanTariffProvider.IM,
                    'code': SuburbanCarrierCode.BASHPPK
                }),
                has_entries({
                    'provider': SuburbanTariffProvider.IM,
                    'code': SuburbanCarrierCode.SODRUZHESTVO
                })
            )
        }))
