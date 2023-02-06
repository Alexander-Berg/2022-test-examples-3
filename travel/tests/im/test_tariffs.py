# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from datetime import datetime, date, timedelta, time

import pytest
from django.conf import settings
from freezegun import freeze_time
from hamcrest import assert_that, has_properties, has_entries, contains_inanyorder

from common.tester.utils.replace_setting import replace_dynamic_setting, replace_setting
from common.tester.factories import create_station
from common.models.tariffs import TariffTypeCode, SuburbanSellingFlow
from common.utils.date import MSK_TZ
from common.models.schedule import Company

from travel.rasp.suburban_selling.selling.tariffs.selling_companies import (
    SuburbanCarrierCode, SuburbanCarrierCompany, SuburbanTariffType
)
from travel.rasp.suburban_selling.selling.tariffs.from_to_key_cache_provider import TariffFromToKey
from travel.rasp.suburban_selling.selling.tariffs.tariffs_configuration import TariffsConfiguration
from travel.rasp.suburban_selling.selling.tariffs.interfaces import TariffKeyDataStatus
from travel.rasp.suburban_selling.selling.im.tariffs import ImTariffsProvider, ImTariffsData, ImBookData, ImTariff
from travel.rasp.suburban_selling.selling.im.factories import ImTariffsFactory, BookDataFactory, ImTrainFactory
from travel.rasp.suburban_selling.selling.im.models import ImTariffs


pytestmark = [pytest.mark.dbuser]


def test_im_tariff():
    im_tariff = ImTariff('carrier', 'type', 111, 'P9', 'Europe/Moscow')

    assert im_tariff.carrier == 'carrier'
    assert im_tariff.type_code == 'type'
    assert im_tariff.price == 111
    assert im_tariff.im_provider == 'P9'
    assert im_tariff.station_from_tz == 'Europe/Moscow'
    assert im_tariff.times_by_train_numbers == {}

    im_tariff.times_by_train_numbers = {
        '11': '2021-09-23T01:00:00',
        '33': '2021-09-23T03:00:00',
    }
    train_number, departure_dt = im_tariff.get_canonical_train()
    assert train_number == '33'
    assert departure_dt.isoformat() == '2021-09-23T03:00:00'


@replace_setting('IM_TARIFFS_TTL', 3600)
def test_im_tariffs_data():
    now = datetime(2021, 7, 23, 12, 30)
    with freeze_time(now):
        book_data = ImBookData('2021-07-24', '100', '200')

        im_tariffs_data = ImTariffsData('tariffs', book_data, None, None)
        assert im_tariffs_data.status == TariffKeyDataStatus.NO_DATA
        assert im_tariffs_data.need_update is True

        im_tariffs_data = ImTariffsData(
            'tariffs', book_data, updated=None, update_started=now
        )
        assert im_tariffs_data.status == TariffKeyDataStatus.NO_DATA
        assert im_tariffs_data.need_update is False

        im_tariffs_data = ImTariffsData(
            'tariffs', book_data, updated=datetime(2021, 7, 23, 11, 20), update_started=datetime(2021, 7, 23, 11, 10)
        )
        assert im_tariffs_data.status == TariffKeyDataStatus.OLD
        assert im_tariffs_data.need_update is True

        im_tariffs_data = ImTariffsData(
            'tariffs', book_data, updated=datetime(2021, 7, 23, 11, 20), update_started=datetime(2021, 7, 23, 12, 25)
        )
        assert im_tariffs_data.status == TariffKeyDataStatus.OLD
        assert im_tariffs_data.need_update is False

        im_tariffs_data = ImTariffsData(
            'tariffs', book_data, updated=datetime(2021, 7, 23, 11, 20), update_started=datetime(2021, 7, 23, 12, 10)
        )
        assert im_tariffs_data.status == TariffKeyDataStatus.OLD
        assert im_tariffs_data.need_update is True

        im_tariffs_data = ImTariffsData(
            'tariffs', book_data, updated=datetime(2021, 7, 23, 12, 20), update_started=datetime(2021, 7, 23, 12, 10)
        )
        assert im_tariffs_data.status == TariffKeyDataStatus.ACTUAL
        assert im_tariffs_data.need_update is False

        im_tariffs_data = ImTariffsData(
            'tariffs', book_data, updated=datetime(2021, 7, 23, 12, 20), update_started=now
        )
        assert im_tariffs_data.status == TariffKeyDataStatus.ACTUAL
        assert im_tariffs_data.need_update is False


class CarrierStub(object):
    def __init__(self, code, code_in_provider):
        self.code = code
        self.code_in_provider = code_in_provider
        self.tariff_types = []


def test_im_tariffs_provider_init():
    carrier_1 = CarrierStub(SuburbanCarrierCode.SZPPK, 'СЗППК')
    carrier_2 = CarrierStub(SuburbanCarrierCode.MTPPK, 'МТППК')

    tariffs_configuration = TariffsConfiguration(
        carriers_by_codes={
            SuburbanCarrierCode.SZPPK: carrier_1,
            SuburbanCarrierCode.MTPPK: carrier_2
        },
        carrier_codes_by_company_id={}
    )

    provider = ImTariffsProvider({'number': 1} , tariffs_configuration)
    assert provider.tariff_type_ids_by_numbers == {'number': 1}

    assert provider.carriers_by_im_code == {
        'СЗППК': carrier_1, 'МТППК': carrier_2
    }

    with replace_dynamic_setting('SUBURBAN_SELLING__IM_TARIFFS_ENABLED', False):
        assert provider.check_get_tariffs_enabled() is False

    with replace_dynamic_setting('SUBURBAN_SELLING__IM_TARIFFS_ENABLED', True):
        assert provider.check_get_tariffs_enabled() is True


def test_get_train_numbers():
    tariffs_configuration = TariffsConfiguration(
        carriers_by_codes={},
        carrier_codes_by_company_id={}
    )
    provider = ImTariffsProvider({} , tariffs_configuration)

    numbers = provider.get_train_numbers('1111')
    assert numbers == ['1111']

    numbers = provider.get_train_numbers('1111f')
    assert numbers == ['1111']

    numbers = provider.get_train_numbers('1111ю')
    assert numbers == ['1111']

    numbers = provider.get_train_numbers('1111/2222')
    assert numbers == ['1111', '2222']

    numbers = provider.get_train_numbers('1111 / 2222я / 3333z')
    assert numbers == ['1111', '2222', '3333']


def _make_tariffs_provider():
    tariffs_configuration = TariffsConfiguration(
        carrier_codes_by_company_id={
            Company.SZPPK_ID: [SuburbanCarrierCode.SZPPK],
            Company.MTPPK_ID: [SuburbanCarrierCode.MTPPK]
        },
        carriers_by_codes={
            SuburbanCarrierCode.SZPPK: SuburbanCarrierCompany(
                code=SuburbanCarrierCode.SZPPK,
                code_in_provider='СЗППК',
                provider='im',
                title='АО «Северо-Западная ППК» (АО «СЗППК»)',
                selling_enabled_prod=True,
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
            SuburbanCarrierCode.MTPPK: SuburbanCarrierCompany(
                code=SuburbanCarrierCode.MTPPK,
                code_in_provider='МТППК',
                provider='im',
                title='АО «Московско-Тверская ППК» (АО «МТППК»)',
                selling_enabled_prod=True,
                selling_enabled_testing=True,
                tariff_types=[
                    SuburbanTariffType(
                        code='etrain',
                        tariff_type_id=1,
                        description='Билет подходит для электричек',
                    )
                ]
            )
        }
    )
    tariff_type_ids_by_numbers = {
        '6000': 1,
        '6001': 1,
        '7000': 2,
        '8000': 1
    }
    return ImTariffsProvider(tariff_type_ids_by_numbers, tariffs_configuration)


def test_make_tariffs_from_trains():
    provider = _make_tariffs_provider()
    trains = [
        ImTrainFactory(is_sale_forbidden=True),
        ImTrainFactory(availability_indication='Not Available'),
        ImTrainFactory(carrier_im_code='ЛевоеППК'),
        ImTrainFactory(train_number='9999'),
        ImTrainFactory(
            carrier_im_code='СЗППК',
            train_number='6000',
            price=600,
            departure_dt='2021-09-24T06:00:00'
        ),
        ImTrainFactory(
            carrier_im_code='СЗППК',
            train_number='6001',
            price=600,
            departure_dt='2021-09-24T06:10:00'
        ),
        ImTrainFactory(
            carrier_im_code='СЗППК',
            train_number='7000',
            price=700,
            departure_dt='2021-09-24T07:00:00'
        ),
        ImTrainFactory(
            carrier_im_code='МТППК',
            train_number='8000',
            price=800,
            im_provider='P9',
            departure_dt='2021-09-24T08:00:00'
        )
    ]
    with freeze_time(datetime(2021, 7, 24)):
        tariffs = provider.make_tariffs_from_trains(trains, 'Europe/Moscow')

        assert len(tariffs) == 3
        assert_that(tariffs, contains_inanyorder(
            has_properties({
                'carrier': SuburbanCarrierCode.SZPPK,
                'type_code': 'etrain',
                'price': 600,
                'im_provider': 'P6',
                'times_by_train_numbers': has_entries({
                    '6000': '2021-09-24T06:00:00',
                    '6001': '2021-09-24T06:10:00'
                })
            }),
            has_properties({
                'carrier': SuburbanCarrierCode.SZPPK,
                'type_code': 'express',
                'price': 700,
                'im_provider': 'P6',
                'times_by_train_numbers': has_entries({
                    '7000': '2021-09-24T07:00:00'
                })
            }),
            has_properties({
                'carrier': SuburbanCarrierCode.MTPPK,
                'type_code': 'etrain',
                'price': 800,
                'im_provider': 'P9',
                'times_by_train_numbers': has_entries({
                    '8000': '2021-09-24T08:00:00'
                })
            })
        ))


def test_get_tariffs_data_from_cache():
    create_station(id=100, time_zone='Etc/GMT-3')
    create_station(id=300, time_zone='Etc/GMT-3')

    # Ключ (100, 200, 2021-09-24)
    ImTariffsFactory(
        station_from=100,
        station_to=200,
        date=date(2021, 9, 24),
        book_data=BookDataFactory(
            date='2021-09-24',
            station_from_express_id='1000',
            station_to_express_id='2000'
        ),
        updated=datetime(2021, 9, 24, 5, 12),
        update_started=datetime(2021, 9, 24, 5, 11),
        im_trains=[
            ImTrainFactory(
                carrier_im_code='СЗППК',
                train_number='6000',
                price=600,
                departure_dt='2021-09-24T06:00:00'
            ),
            ImTrainFactory(
                carrier_im_code='СЗППК',
                train_number='7000',
                price=700,
                departure_dt='2021-09-24T07:00:00'
            )
        ]
    )

    # Ключ (300, 400, 2021-09-25), новые данные
    ImTariffsFactory(
        station_from=300,
        station_to=400,
        date=date(2021, 9, 25),
        book_data=BookDataFactory(
            date='2021-09-25',
            station_from_express_id='3000',
            station_to_express_id='4000'
        ),
        updated=datetime(2021, 9, 24, 5, 14),
        update_started=datetime(2021, 9, 24, 5, 13),
        im_trains=[
            ImTrainFactory(
                carrier_im_code='МТППК',
                train_number='8000',
                price=800,
                im_provider='P9',
                departure_dt='2021-09-25T08:00:00'
            )
        ]
    )
    with freeze_time(datetime(2021, 7, 24)):
        provider = _make_tariffs_provider()
        keys = [
            TariffFromToKey(station_from=100, station_to=200, date=date(2021, 9, 24)),
            TariffFromToKey(station_from=300, station_to=400, date=date(2021, 9, 25)),
            TariffFromToKey(station_from=500, station_to=600, date=date(2021, 9, 26))  # ключа нет в базе
        ]
        keys_with_tariffs = provider.get_tariffs_data_from_cache(keys)

        assert len(keys_with_tariffs) == 2
        assert_that(keys_with_tariffs, has_entries({
            keys[0]: has_properties({
                'updated': datetime(2021, 9, 24, 5, 12),
                'update_started': datetime(2021, 9, 24, 5, 11),
                'book_data': has_properties({
                    'date': '2021-09-24',
                    'station_from_express_id': '1000',
                    'station_to_express_id': '2000'
                }),
                'tariffs': contains_inanyorder(
                    has_properties({
                        'carrier': SuburbanCarrierCode.SZPPK,
                        'type_code': 'etrain',
                        'price': 600,
                        'im_provider': 'P6',
                        'times_by_train_numbers': has_entries({
                            '6000': '2021-09-24T06:00:00'
                        })
                    }),
                    has_properties({
                        'carrier': SuburbanCarrierCode.SZPPK,
                        'type_code': 'express',
                        'price': 700,
                        'im_provider': 'P6',
                        'times_by_train_numbers': has_entries({
                            '7000': '2021-09-24T07:00:00'
                        })
                    })
                )
            }),
            keys[1]: has_properties({
                'updated': datetime(2021, 9, 24, 5, 14),
                'update_started': datetime(2021, 9, 24, 5, 13),
                'book_data': has_properties({
                    'date': '2021-09-25',
                    'station_from_express_id': '3000',
                    'station_to_express_id': '4000'
                }),
                'tariffs': contains_inanyorder(
                    has_properties({
                        'carrier': SuburbanCarrierCode.MTPPK,
                        'type_code': 'etrain',
                        'price': 800,
                        'im_provider': 'P9',
                        'times_by_train_numbers': has_entries({
                            '8000': '2021-09-25T08:00:00'
                        })
                    })
                )
            })
        }))


def test_prepare_base_tariff_key_data_in_cache():
    provider = _make_tariffs_provider()

    create_station(id=100, express_id='1000')
    create_station(id=200, express_id='2000')
    from_to_key = TariffFromToKey(station_from=100, station_to=200, date=date(2021, 9, 24))

    with freeze_time(datetime(2021, 7, 24, 12)):
        provider.prepare_base_tariff_key_data_in_cache(from_to_key)

    im_tariffs_list = list(ImTariffs.objects.filter(station_from=100, station_to=200, date=date(2021, 9, 24)))

    assert len(im_tariffs_list) == 1
    assert_that(im_tariffs_list[0], has_properties({
        'station_from': 100,
        'station_to': 200,
        'date': date(2021, 9, 24),
        'book_data': has_entries({
            'date': '2021-09-24',
            'station_from_express_id': '1000',
            'station_to_express_id': '2000',
        }),
        'update_started': datetime(2021, 7, 24, 12)
    }))

    with freeze_time(datetime(2021, 7, 24, 13)):
        provider.prepare_base_tariff_key_data_in_cache(from_to_key)

    im_tariffs_list = list(ImTariffs.objects.filter(station_from=100, station_to=200, date=date(2021, 9, 24)))

    assert len(im_tariffs_list) == 1
    assert_that(im_tariffs_list[0], has_properties({
        'book_data': has_entries({
            'date': '2021-09-24',
            'station_from_express_id': '1000',
            'station_to_express_id': '2000',
        }),
        'update_started': datetime(2021, 7, 24, 13)
    }))


def test_make_selling_tariffs_from_tariffs_data():
    provider = _make_tariffs_provider()

    tariff_1_1 = ImTariff(
        carrier=SuburbanCarrierCode.SZPPK,
        type_code=TariffTypeCode.USUAL,
        price=101,
        im_provider='P6',
        station_from_tz='Europe/Moscow'
    )
    tariff_1_1.times_by_train_numbers = {'1111': '2020-10-25T11:11:11'}

    tariff_1_2 = ImTariff(
        carrier=SuburbanCarrierCode.SZPPK,
        type_code=TariffTypeCode.EXPRESS,
        price=102,
        im_provider='P6',
        station_from_tz='Europe/Moscow'
    )
    tariff_1_2.times_by_train_numbers = {'1112': '2020-10-25T12:12:12'}

    key_1 = TariffFromToKey(date=date(2021, 7, 25), station_from=11, station_to=22)
    tariffs_data_1 = ImTariffsData(
        book_data=ImBookData(
            date='2021-07-25',
            station_from_express_id='111',
            station_to_express_id='222',
        ),
        tariffs=[tariff_1_1, tariff_1_2],
        updated=datetime(2021, 7, 24, 12)
    )

    tariff_2_1 = ImTariff(
        carrier=SuburbanCarrierCode.SZPPK,
        type_code=TariffTypeCode.USUAL,
        price=202,
        im_provider='P7',
        station_from_tz='Europe/Moscow'
    )
    tariff_2_1.times_by_train_numbers = {'2222': '2020-10-26T22:22:22'}

    key_2 = TariffFromToKey(date=date(2021, 7, 26), station_from=11, station_to=33)
    tariffs_data_2 = ImTariffsData(
        book_data=ImBookData(
            date='2021-07-26',
            station_from_express_id='111',
            station_to_express_id='333',
        ),
        tariffs=[tariff_2_1],
        updated=datetime(2021, 7, 24, 12)
    )

    valid_from = MSK_TZ.localize(datetime(2021, 7, 25))
    valid_until = MSK_TZ.localize(
        datetime.combine(
            date(2021, 7, 25) + timedelta(days=settings.IM_TICKET_VALID_DAYS),
            time(settings.IM_TICKET_VALID_HOURS_IN_LAST_DAY)
        )
    )

    with freeze_time(datetime(2021, 7, 24, 12)):
        tariffs_by_from_to_keys = provider.make_selling_tariffs_from_tariffs_data({
            key_1: tariffs_data_1,
            key_2: tariffs_data_2
        })

        assert len(tariffs_by_from_to_keys) == 2
        assert len(tariffs_by_from_to_keys[key_1].tariffs) == 2
        assert len(tariffs_by_from_to_keys[key_2].tariffs) == 1

        assert_that(tariffs_by_from_to_keys, has_entries({
            key_1: has_properties({
                'tariffs': contains_inanyorder(
                    has_properties({
                        'partner': SuburbanCarrierCode.SZPPK,
                        'tariff_type': TariffTypeCode.USUAL,
                        'name': TariffTypeCode.USUAL,
                        'description': 'Билет подходит для электричек',
                        'price': 101.0,
                        'max_days': None,
                        'valid_from': valid_from,
                        'valid_until': valid_until,
                        'book_data': has_entries({
                            'date': '2021-07-25',
                            'station_from_express_id': '111',
                            'station_to_express_id': '222',
                            'train_number': '1111',
                            'departure_dt': '2020-10-25T11:11:11',
                            'departure_tz': 'Europe/Moscow',
                            'im_provider': 'P6'
                        }),
                        'selling_flow': SuburbanSellingFlow.SIMPLE
                    }),
                    has_properties({
                        'partner': SuburbanCarrierCode.SZPPK,
                        'tariff_type': TariffTypeCode.EXPRESS,
                        'name': TariffTypeCode.EXPRESS,
                        'description': 'Билет подходит для электричек и экспрессов',
                        'price': 102.0,
                        'max_days': None,
                        'valid_from': valid_from,
                        'valid_until': valid_until,
                        'book_data': has_entries({
                            'date': '2021-07-25',
                            'station_from_express_id': '111',
                            'station_to_express_id': '222',
                            'train_number': '1112',
                            'departure_dt': '2020-10-25T12:12:12',
                            'departure_tz': 'Europe/Moscow',
                            'im_provider': 'P6'
                        }),
                        'selling_flow': SuburbanSellingFlow.SIMPLE
                    })
                ),
            }),

            key_2: has_properties({
                'tariffs': contains_inanyorder(
                    has_properties({
                        'partner': SuburbanCarrierCode.SZPPK,
                        'tariff_type': TariffTypeCode.USUAL,
                        'name': TariffTypeCode.USUAL,
                        'description': 'Билет подходит для электричек',
                        'price': 202.0,
                        'max_days': None,
                        'valid_from': valid_from + timedelta(days=1),
                        'valid_until': valid_until + timedelta(days=1),
                        'book_data': has_entries({
                            'date': '2021-07-26',
                            'station_from_express_id': '111',
                            'station_to_express_id': '333',
                            'train_number': '2222',
                            'departure_dt': '2020-10-26T22:22:22',
                            'departure_tz': 'Europe/Moscow',
                            'im_provider': 'P7'
                        }),
                        'selling_flow': SuburbanSellingFlow.SIMPLE
                    })
                )
            })
        }))
