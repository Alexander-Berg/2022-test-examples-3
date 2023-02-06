# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from hamcrest import assert_that, has_entries, has_properties, contains_inanyorder

from common.models.schedule import Company
from travel.rasp.suburban_selling.selling.tariffs.selling_companies import (
    SuburbanCarrierCode, SuburbanCarrierCompany, SuburbanTariffType
)
from travel.rasp.suburban_selling.selling.tariffs.tariffs_configuration import TariffsConfiguration


def test_tariffs_configuration():
    carrier_codes_by_company_id = {
        Company.SZPPK_ID: [SuburbanCarrierCode.SZPPK],
        Company.CPPK_ID: [SuburbanCarrierCode.CPPK],
        Company.CPPK_AEROEX_ID: [SuburbanCarrierCode.CPPK, SuburbanCarrierCode.AEROEXPRESS]
    }
    carriers_by_codes = {
        SuburbanCarrierCode.SZPPK: SuburbanCarrierCompany(
            code=SuburbanCarrierCode.SZPPK,
            provider='im',
            title='СЗППК',
            selling_enabled_testing=True,
            tariff_types=[
                SuburbanTariffType(
                    code='etrain',
                    tariff_type_id=1,
                    description='Билет подходит для электричек'
                ),
                SuburbanTariffType(
                    code='express',
                    tariff_type_id=2,
                    description='Билет подходит для электричек и экспрессов'
                )
            ]
        ),
        SuburbanCarrierCode.CPPK: SuburbanCarrierCompany(
            code=SuburbanCarrierCode.CPPK,
            provider='movista',
            title='ЦППК',
            selling_enabled_testing=True,
            tariff_types=[
                SuburbanTariffType(
                    code='etrain',
                    tariff_type_id=1,
                    description='Билет подходит для электричек'
                )
            ]
        ),
        SuburbanCarrierCode.AEROEXPRESS: SuburbanCarrierCompany(
            code=SuburbanCarrierCode.AEROEXPRESS,
            provider='aeroexpress',
            title='Аэроэкспресс',
            selling_enabled_testing=True,
            tariff_types=[
                SuburbanTariffType(
                    code='aeroexpress',
                    tariff_type_id=58,
                    description='Билет подходит для аэроэкспресса'
                )
            ]
        )
    }

    tariffs_configuration = TariffsConfiguration(
        carrier_codes_by_company_id=carrier_codes_by_company_id,
        carriers_by_codes=carriers_by_codes
    )

    assert tariffs_configuration.carrier_codes_by_company_id == carrier_codes_by_company_id

    assert_that(tariffs_configuration.tariff_types_by_carriers_and_codes, has_entries({
        SuburbanCarrierCode.SZPPK: has_entries({
            'etrain': has_properties({
                'code': 'etrain',
                'tariff_type_id': 1,
                'description': 'Билет подходит для электричек'
            }),
            'express': has_properties({'code': 'express'})
        }),
        SuburbanCarrierCode.CPPK: has_entries({
            'etrain': has_properties({'code': 'etrain'})
        }),
        SuburbanCarrierCode.AEROEXPRESS: has_entries({
            'aeroexpress': has_properties({'code': 'aeroexpress'})
        })
    }))

    assert_that(tariffs_configuration.tariff_types_by_carriers_and_ids, has_entries({
        SuburbanCarrierCode.SZPPK: has_entries({
            1: has_properties({
                'code': 'etrain',
                'tariff_type_id': 1,
                'description': 'Билет подходит для электричек'
            }),
            2: has_properties({'code': 'express'})
        }),
        SuburbanCarrierCode.CPPK: has_entries({
            1: has_properties({'code': 'etrain'})
        }),
        SuburbanCarrierCode.AEROEXPRESS: has_entries({
            58: has_properties({'code': 'aeroexpress'})
        })
    }))

    assert tariffs_configuration.carriers_by_codes == carriers_by_codes

    assert_that(tariffs_configuration.provider_codes_by_companies_id, has_entries({
        Company.SZPPK_ID: ['im'],
        Company.CPPK_ID: ['movista'],
        Company.CPPK_AEROEX_ID: contains_inanyorder('movista', 'aeroexpress')
    }))

    assert tariffs_configuration.get_company_provider_codes(Company.SZPPK_ID) == ['im']
    assert_that(
        tariffs_configuration.get_company_provider_codes(Company.CPPK_AEROEX_ID),
        contains_inanyorder('movista', 'aeroexpress')
    )
