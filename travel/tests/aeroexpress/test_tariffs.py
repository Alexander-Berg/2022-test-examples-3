# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from datetime import datetime, date
from dateutil import parser

import factory
from bson.decimal128 import Decimal128
from freezegun import freeze_time
from hamcrest import assert_that, contains_inanyorder, has_entries, has_properties

from common.models.schedule import Company
from common.models.tariffs import TariffTypeCode, SuburbanSellingFlow, SuburbanTariffProvider
from common.tester.utils.replace_setting import replace_dynamic_setting

from travel.rasp.suburban_selling.selling.aeroexpress.factories import ClientContractsFactory
from travel.rasp.suburban_selling.selling.aeroexpress.factories import (
    AeroexTariffFactory, TariffKeyFactory, TariffDataFactory
)
from travel.rasp.suburban_selling.selling.aeroexpress.tariffs import AeroexTariffsProvider
from travel.rasp.suburban_selling.selling.tariffs.interfaces import TariffKey
from travel.rasp.suburban_selling.selling.tariffs.selling_companies import (
    SUBURBAN_CARRIERS_BY_CODES, SUBURBAN_CARRIERS_BY_COMPANIES_ID, SuburbanCarrierCode
)
from travel.rasp.suburban_selling.selling.tariffs.tariffs_getter import TariffsGetter
from travel.rasp.suburban_selling.selling.tariffs.tariffs_configuration import TariffsConfiguration


class TestAeroexTariffsProvider(object):
    def test_get_tariffs(self):
        tariffs_configuration = TariffsConfiguration(
            carriers_by_codes={'aeroexpress': SUBURBAN_CARRIERS_BY_CODES['aeroexpress']},
            carrier_codes_by_company_id=SUBURBAN_CARRIERS_BY_COMPANIES_ID
        )
        aeroex_provider = AeroexTariffsProvider(tariffs_configuration=tariffs_configuration)
        TariffsGetter(
            providers={SuburbanTariffProvider.AEROEXPRESS: aeroex_provider},
            selling_flows=[SuburbanSellingFlow.AEROEXPRESS],
            barcode_presets=[],
            tariffs_configuration=tariffs_configuration
        )

        AeroexTariffFactory(
            key=TariffKeyFactory(
                station_from=42,
                station_to=43,
            ),
            data=TariffDataFactory(
                menu_id=14,
                name='стандарт',
                price=Decimal128('420'),
                order_type=25,
                description='на стандарте',
                max_days=90,
                begin_dt=datetime(2020, 10, 19, 21),
                end_dt=datetime(2021, 1, 17, 21)
            )
        )

        AeroexTariffFactory(
            key=TariffKeyFactory(
                station_from=43,
                station_to=42,
            ),
            data=TariffDataFactory(
                menu_id=2,
                name='обратно',
                price=Decimal128('840'),
                order_type=34,
                description='на туда-сюда',
                max_days=90,
                begin_dt=datetime(2020, 10, 19, 21),
                end_dt=datetime(2021, 1, 17, 21)
            )
        )

        class SellingTariffKeyFactory(factory.Factory):
            class Meta:
                model = TariffKey

            date = date(2020, 10, 24)
            station_from = 42
            station_to = 43
            company = Company.AEROEXPRESS_ID
            tariff_type = 'aeroexpress'

        key1 = SellingTariffKeyFactory()
        key2 = SellingTariffKeyFactory(company=Company.CPPK_AEROEX_ID)
        key3 = SellingTariffKeyFactory(station_from=43, station_to=42)
        key4 = SellingTariffKeyFactory(company=5555)
        key5 = SellingTariffKeyFactory(station_to=44)
        key6 = SellingTariffKeyFactory(date=date(2021, 1, 20))

        tariff_keys = {key1, key2, key3, key4, key5, key6}

        with freeze_time(datetime(2020, 10, 24)):
            ClientContractsFactory()
            with replace_dynamic_setting('SUBURBAN_SELLING__AEROEX_TARIFFS_ENABLED', False):
                tariffs_by_tariff_keys = aeroex_provider.get_tariffs(tariff_keys)
                assert not tariffs_by_tariff_keys

            tariffs_by_tariff_keys = aeroex_provider.get_tariffs(tariff_keys)

        for key in [key1, key2]:
            assert len(tariffs_by_tariff_keys[key].tariffs) == 1
            assert_that(tariffs_by_tariff_keys[key].tariffs, contains_inanyorder(
                has_properties({
                    'partner': SuburbanCarrierCode.AEROEXPRESS,
                    'tariff_type': TariffTypeCode.AEROEXPRESS,
                    'name': 'стандарт',
                    'description': 'на стандарте',
                    'price': 420,
                    'max_days': 90,
                    'valid_from': parser.parse('2020-10-24T00:00:00+03:00'),
                    'valid_until': parser.parse('2020-11-23T03:00:00+03:00'),
                    'book_data': has_entries({
                        'menu_id': 14,
                        'order_type': 25
                    }),
                    'selling_flow': SuburbanSellingFlow.AEROEXPRESS
                })
            ))

        assert key4 not in tariffs_by_tariff_keys
        assert len(tariffs_by_tariff_keys[key5].tariffs) == 0
        assert len(tariffs_by_tariff_keys[key6].tariffs) == 0

        assert len(tariffs_by_tariff_keys[key3].tariffs) == 1
        assert_that(tariffs_by_tariff_keys[key3].tariffs, contains_inanyorder(
            has_properties({
                'partner': SuburbanCarrierCode.AEROEXPRESS,
                'tariff_type': TariffTypeCode.AEROEXPRESS,
                'name': 'обратно',
                'description': 'на туда-сюда',
                'price': 840,
                'max_days': 90,
                'valid_from': parser.parse('2020-10-24T00:00:00+03:00'),
                'valid_until': parser.parse('2020-11-23T03:00:00+03:00'),
                'book_data': has_entries({
                    'menu_id': 2,
                    'order_type': 34
                }),
                'selling_flow': SuburbanSellingFlow.AEROEXPRESS
            })
        ))
