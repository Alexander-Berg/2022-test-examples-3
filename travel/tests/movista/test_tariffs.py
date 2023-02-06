# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from dateutil import parser
from datetime import datetime, date, timedelta
from decimal import Decimal

import httpretty
import factory
import mock
import pytest
from freezegun import freeze_time
from hamcrest import assert_that, contains_inanyorder, has_properties, has_entries, contains
from django.conf import settings

from common.data_api.movista import instance
from common.models.schedule import Company
from common.models.tariffs import (
    TariffTypeCode, SuburbanSellingFlow, SuburbanTariffProvider, SuburbanSellingBarcodePreset
)
from common.tester.factories import create_station
from common.tester.utils.replace_setting import replace_dynamic_setting
from common.tester.utils.datetime import replace_now
from travel.rasp.library.python.api_clients.movista import MovistaClient

from travel.rasp.suburban_selling.selling.movista.factories import MovistaTariffsFactory, TariffFactory, BookDataFactory
from travel.rasp.suburban_selling.selling.movista.tariffs import (
    MovistaTariffsProvider, MovistaTariffsData, MovistaBookData, MovistaTariff
)
from travel.rasp.suburban_selling.selling.movista.tariffs import TariffFromToKey
from travel.rasp.suburban_selling.selling.tariffs.api import TariffKey
from travel.rasp.suburban_selling.selling.tariffs.tariffs_getter import TariffsGetter
from travel.rasp.suburban_selling.selling.tariffs.tariffs_configuration import TariffsConfiguration
from travel.rasp.suburban_selling.selling.tariffs.selling_companies import (
    SUBURBAN_CARRIERS_BY_CODES, SUBURBAN_CARRIERS_BY_COMPANIES_ID, SuburbanCarrierCode
)


pytestmark = [pytest.mark.mongouser, pytest.mark.dbuser]


class MovistaClientMock(MovistaClient):
    def __init__(self):
        self.fares_response = {}

    def fares(self, date, from_express_id, to_express_id):
        result = self.fares_response.get((date, from_express_id, to_express_id))
        if not result:
            result = {'fares': [], 'sale': False}
        return result

    def add_fares(self, date, from_express_id, to_express_id, fares):
        assert 'sale' in fares
        for fare_data in fares['fares']:
            assert set(fare_data.keys()) == {
                'price', 'fareId', 'fromExpressId', 'toExpressId', 'farePlan', 'ticketType'
            }

        self.fares_response[(date, from_express_id, to_express_id)] = fares


def get_movista_client_mock():
    create_station(id=42, express_id='4242')
    create_station(id=43, express_id='4343')

    movista_client = MovistaClientMock()
    movista_client.add_fares(date(2020, 10, 24), '4242', '4343', {
        'sale': True,
        'fares': [
            {
                'price': 48.99,
                'fareId': 40371,
                'fromExpressId': 4242,
                'toExpressId': 4343,
                'farePlan': 'Пассажирский',
                'ticketType': 'Разовый полный'
            },
            {
                'price': 96.01,
                'fareId': 250436,
                'fromExpressId': 4242,
                'toExpressId': 4343,
                'farePlan': 'ЭКСПРЕСС',
                'ticketType': 'Разовый полный'
            },
        ]
    })

    return movista_client


class TestMovistaTariffsProvider(object):
    def test_get_tariffs_for_tariffs_keys(self):
        create_station(id=42, express_id='4242')
        create_station(id=43, express_id='4343')

        now = datetime(2020, 10, 24, 14, 46, 18)
        with freeze_time(now):
            # просто тариф
            MovistaTariffsFactory(
                date=date(2020, 10, 24),
                station_from=42,
                station_to=43,
                book_data=BookDataFactory(
                    date='2020-10-24',
                    station_from_express_id='4242',
                    station_to_express_id='4343'
                ),
                tariffs=[
                    TariffFactory(type=TariffTypeCode.USUAL, price=420, fare_id=1234),
                    TariffFactory(type=TariffTypeCode.EXPRESS, price=560.11, fare_id=8888),
                ]
            )

            # тариф на другое направление
            MovistaTariffsFactory(
                date=date(2020, 10, 24),
                station_from=42,
                station_to=44,
                book_data=BookDataFactory(
                    date='2020-10-24',
                    station_from_express_id='4242',
                    station_to_express_id='4444'
                ),
                tariffs=[
                    TariffFactory(type=TariffTypeCode.EXPRESS, price=560.11, fare_id=9999),
                ]
            )

            # такую дату не запрашиваем -> не должен попасть в выдачу
            MovistaTariffsFactory(
                date=date(2020, 10, 25),
                station_from=42,
                station_to=44,
                book_data=BookDataFactory(
                    date='2020-10-24',
                    station_from_express_id='4242',
                    station_to_express_id='4444'
                ),
                tariffs=[
                    TariffFactory(type=TariffTypeCode.USUAL, price=111, fare_id=1111),
                    TariffFactory(type=TariffTypeCode.EXPRESS, price=222, fare_id=22222),
                ]
            )

            # такое направление не запрашиваем -> не должен попасть в выдачу
            MovistaTariffsFactory(
                date=date(2020, 10, 24),
                station_from=43,
                station_to=44,
                book_data=BookDataFactory(
                    date='2020-10-24',
                    station_from_express_id='4242',
                    station_to_express_id='4444'
                ),
                tariffs=[
                    TariffFactory(type=TariffTypeCode.USUAL, price=111, fare_id=1111),
                    TariffFactory(type=TariffTypeCode.EXPRESS, price=222, fare_id=2222),
                ]
            )

        movista = MovistaTariffsProvider(
            movista_client=instance.movista_client,
            tariffs_ttl=3600,
            tariffs_configuration=TariffsConfiguration({}, {})
        )

        with freeze_time(now + timedelta(seconds=3500)):
            key = TariffFromToKey(date=date(2020, 10, 24), station_from=42, station_to=43)
            tariffs_data = movista.get_tariffs_data_from_cache({key})[key]

            assert isinstance(tariffs_data, MovistaTariffsData)
            assert_that(tariffs_data, has_properties(
                can_sell=True,
                book_data=MovistaBookData(
                    date='2020-10-24',
                    station_from_express_id='4242',
                    station_to_express_id='4343'
                ),
                tariffs=contains_inanyorder(
                    MovistaTariff(type='etrain', price=Decimal('420.0'), fare_id=1234),
                    MovistaTariff(type='express', price=Decimal('560.11'), fare_id=8888),
                ),
            ))

            key = TariffFromToKey(date=date(2020, 10, 24), station_from=42, station_to=44)
            tariffs_data = movista.get_tariffs_data_from_cache({key})[key]

            assert isinstance(tariffs_data, MovistaTariffsData)
            assert_that(tariffs_data, has_properties(
                can_sell=True,
                book_data=MovistaBookData(
                    date='2020-10-24',
                    station_from_express_id='4242',
                    station_to_express_id='4444'
                ),
                tariffs=contains_inanyorder(
                    MovistaTariff(type='express', price=Decimal('560.11'), fare_id=9999),
                ),
            ))

        # ttl тарифов прошел
        with freeze_time(now + timedelta(seconds=3601)):
            key = TariffFromToKey(date=date(2020, 10, 24), station_from=42, station_to=43)
            tariffs_data = movista.get_tariffs_data_from_cache({key})

            assert not tariffs_data

    def test_save_tariffs_to_cache(self):
        movista = MovistaTariffsProvider(
            movista_client=instance.movista_client,
            tariffs_ttl=3600,
            tariffs_configuration=TariffsConfiguration({}, {})
        )

        tariff_key1 = TariffFromToKey(date=date(2020, 10, 24), station_from=42, station_to=43)
        tariff_key2 = TariffFromToKey(date=date(2020, 10, 25), station_from=42, station_to=43)

        now = datetime(2020, 10, 24, 14, 46, 18)

        # сохраняем один ключ
        with freeze_time(now):
            tariff_data = MovistaTariffsData(
                can_sell=True,
                book_data=MovistaBookData(
                    date='2020-10-24',
                    station_from_express_id='4242',
                    station_to_express_id='4343'
                ),
                tariffs=[
                    MovistaTariff(type='etrain', price=Decimal('420.0'), fare_id=1234),
                    MovistaTariff(type='express', price=Decimal('560.11'), fare_id=8888),
                ]
            )
            movista.save_tariffs_data_to_cache(tariff_key1, tariff_data)
            tariffs = movista.get_tariffs_data_from_cache({tariff_key1, tariff_key2})

        assert len(tariffs) == 1
        assert_that(tariffs[tariff_key1], has_properties(
            can_sell=True,
            book_data=MovistaBookData(
                date='2020-10-24',
                station_from_express_id='4242',
                station_to_express_id='4343'
            ),
            tariffs=contains_inanyorder(
                MovistaTariff(type='etrain', price=Decimal('420.0'), fare_id=1234),
                MovistaTariff(type='express', price=Decimal('560.11'), fare_id=8888),
            ),
            updated=now,
        ))

        # сохраняем второй ключ
        with freeze_time(now + timedelta(seconds=3)):
            tariff_data = MovistaTariffsData(
                can_sell=True,
                book_data=MovistaBookData(
                    date='2020-10-25',
                    station_from_express_id='4242',
                    station_to_express_id='4343'
                ),
                tariffs=[
                    MovistaTariff(type='etrain', price=Decimal('1420.0'), fare_id=1234),
                    MovistaTariff(type='express', price=Decimal('1560.11'), fare_id=8888),
                ]
            )
            movista.save_tariffs_data_to_cache(tariff_key2, tariff_data)
            tariffs = movista.get_tariffs_data_from_cache({tariff_key1, tariff_key2})

        assert len(tariffs) == 2
        assert_that(tariffs[tariff_key1], has_properties(
            can_sell=True,
            book_data=MovistaBookData(
                date='2020-10-24',
                station_from_express_id='4242',
                station_to_express_id='4343'
            ),
            tariffs=contains_inanyorder(
                MovistaTariff(type='etrain', price=Decimal('420.0'), fare_id=1234),
                MovistaTariff(type='express', price=Decimal('560.11'), fare_id=8888),
            ),
            updated=now,
        ))
        assert_that(tariffs[tariff_key2], has_properties(
            can_sell=True,
            book_data=MovistaBookData(
                date='2020-10-25',
                station_from_express_id='4242',
                station_to_express_id='4343'
            ),
            tariffs=contains_inanyorder(
                MovistaTariff(type='etrain', price=Decimal('1420.0'), fare_id=1234),
                MovistaTariff(type='express', price=Decimal('1560.11'), fare_id=8888),
            ),
            updated=now + timedelta(seconds=3)
        ))

        # сохраняем первый ключ с апдейтом
        with freeze_time(now + timedelta(seconds=12)):
            tariff_data = MovistaTariffsData(
                can_sell=False,
                book_data=MovistaBookData(
                    date='2020-10-24',
                    station_from_express_id='4242',
                    station_to_express_id='4343'
                ),
                tariffs=[
                    MovistaTariff(type='etrain', price=Decimal('777.0'), fare_id=555),
                ]
            )
            movista.save_tariffs_data_to_cache(tariff_key1, tariff_data)
            tariffs = movista.get_tariffs_data_from_cache({tariff_key1, tariff_key2})

        assert len(tariffs) == 2
        assert_that(tariffs[tariff_key1], has_properties(
            can_sell=False,
            book_data=MovistaBookData(
                date='2020-10-24',
                station_from_express_id='4242',
                station_to_express_id='4343'
            ),
            tariffs=contains_inanyorder(
                MovistaTariff(type='etrain', price=Decimal('777.0'), fare_id=555),
            ),
            updated=now + timedelta(seconds=12)
        ))
        assert_that(tariffs[tariff_key2], has_properties(
            can_sell=True,
            book_data=MovistaBookData(
                date='2020-10-25',
                station_from_express_id='4242',
                station_to_express_id='4343'
            ),
            tariffs=contains_inanyorder(
                MovistaTariff(type='etrain', price=Decimal('1420.0'), fare_id=1234),
                MovistaTariff(type='express', price=Decimal('1560.11'), fare_id=8888),
            ),
            updated=now + timedelta(seconds=3)
        ))

    def test_get_tariffs_from_movista(self):
        movista = MovistaTariffsProvider(
            movista_client=get_movista_client_mock(),
            tariffs_ttl=3600,
            tariffs_configuration=TariffsConfiguration({}, {})
        )

        create_station(id=45, express_id=None)

        # не существует станция
        tariff_key = TariffFromToKey(date=date(2020, 10, 24), station_from=44, station_to=43)
        tariffs_data = movista.get_tariffs_data_from_provider(tariff_key)
        assert not tariffs_data
        assert not movista.get_tariffs_data_from_cache({tariff_key})

        # нет express_id у станции
        tariff_key = TariffFromToKey(date=date(2020, 10, 24), station_from=45, station_to=43)
        tariffs_data = movista.get_tariffs_data_from_provider(tariff_key)
        assert not tariffs_data
        assert not movista.get_tariffs_data_from_cache({tariff_key})

        # должны получить тарифы
        tariff_key = TariffFromToKey(date=date(2020, 10, 24), station_from=42, station_to=43)
        tariffs_data = movista.get_tariffs_data_from_provider(tariff_key)
        assert_that(tariffs_data, has_properties(
            can_sell=True,
            book_data=MovistaBookData(date='2020-10-24', station_from_express_id='4242', station_to_express_id='4343'),
            tariffs=contains_inanyorder(
                MovistaTariff(type='etrain', price=Decimal('48.99'), fare_id=40371),
                MovistaTariff(type='express', price=Decimal('96.01'), fare_id=250436),
            ),
        ))

        # нет тарифов на дату -> тоже валидный ответ; надо в том числе закэшировать
        tariff_key = TariffFromToKey(date=date(2020, 10, 25), station_from=42, station_to=43)
        tariffs_data = movista.get_tariffs_data_from_provider(tariff_key)
        assert_that(tariffs_data, has_properties(
            can_sell=False,
            book_data=MovistaBookData(date='2020-10-25', station_from_express_id='4242', station_to_express_id='4343'),
            tariffs=[],
        ))

        with replace_dynamic_setting('SUBURBAN_SELLING__MOVISTA_API_CALL_ENABLED', False):
            assert not movista.get_tariffs_data_from_provider(tariff_key)

    @httpretty.activate
    def test_get_tariffs_from_movista_bad_response(self):
        create_station(id=42, express_id='4242')
        create_station(id=43, express_id='4343')

        movista = MovistaTariffsProvider(
            movista_client=instance.movista_client,
            tariffs_ttl=3600,
            tariffs_configuration=TariffsConfiguration({}, {})
        )
        tariff_key = TariffFromToKey(date=date(2020, 10, 26), station_from=42, station_to=43)
        tariffs_data = MovistaTariffsData(
            can_sell=True,
            book_data=MovistaBookData(date='2020-10-26', station_from_express_id='4242', station_to_express_id='4343'),
            tariffs=[MovistaTariff(type='etrain', price=Decimal('777.0'), fare_id=555)]
        )
        movista.save_tariffs_data_to_cache(tariff_key, tariffs_data)
        tariff = movista.get_tariffs_data_from_cache({tariff_key})[tariff_key]

        assert_that(tariff, has_properties(
            book_data=MovistaBookData(date='2020-10-26', station_from_express_id='4242', station_to_express_id='4343'),
            tariffs=contains_inanyorder(MovistaTariff(type='etrain', price=Decimal('777.0'), fare_id=555)),
        ))

        # не удалось получть тарифы из Мовисты, не затираем кэш
        httpretty.register_uri(
            httpretty.POST, '{}api/v1/fares'.format(settings.MOVISTA_API_HOST),
            status=500, content_type='application/json', body={}
        )

        tariffs_data = movista.get_tariffs_data_from_provider(tariff_key)
        assert not tariffs_data

        tariff = movista.get_tariffs_data_from_cache({tariff_key})[tariff_key]
        assert_that(tariff, has_properties(
            book_data=MovistaBookData(date='2020-10-26', station_from_express_id='4242', station_to_express_id='4343'),
            tariffs=contains_inanyorder(MovistaTariff(type='etrain', price=Decimal('777.0'), fare_id=555)),
        ))

    def test_get_movista_tariffs(self):
        tariffs_configuration = TariffsConfiguration(
            carriers_by_codes={'cppk': SUBURBAN_CARRIERS_BY_CODES['cppk']},
            carrier_codes_by_company_id=SUBURBAN_CARRIERS_BY_COMPANIES_ID
        )
        movista_provider = MovistaTariffsProvider(
            movista_client=get_movista_client_mock(),
            tariffs_ttl=3600,
            tariffs_configuration=tariffs_configuration
        )
        TariffsGetter(
            providers={SuburbanTariffProvider.MOVISTA: movista_provider},
            selling_flows=[SuburbanSellingFlow.VALIDATOR],
            barcode_presets=[SuburbanSellingBarcodePreset.PDF417_CPPK],
            tariffs_configuration=tariffs_configuration
        )

        MovistaTariffsFactory(
            date=date(2020, 10, 25),
            station_from=42,
            station_to=44,
            book_data=BookDataFactory(date='2020-10-25'),
            tariffs=[TariffFactory(fare_id=56)]
        )

        tariff_key1 = TariffFromToKey(date=date(2020, 10, 24), station_from=42, station_to=43)
        tariff_key2 = TariffFromToKey(date=date(2020, 10, 25), station_from=42, station_to=44)
        tariff_key3 = TariffFromToKey(date=date(2020, 10, 26), station_from=42, station_to=44)

        with mock.patch.object(
            MovistaTariffsProvider,
            'get_tariffs_data_from_provider',
            wraps=movista_provider.get_tariffs_data_from_provider
        ) as m_get_tariffs_data_from_provider:
            movista_tariffs = movista_provider.get_selling_tariffs_by_from_to_keys({tariff_key1, tariff_key2})

        # получили из MovistaClientMock
        tariffs = movista_tariffs[tariff_key1].tariffs
        assert len(tariffs) == 2
        assert tariffs[0].book_data['date'] == '2020-10-24'
        assert tariffs[0].book_data['fare_id'] == 40371

        # получили из кэша
        tariffs = movista_tariffs[tariff_key2].tariffs
        assert tariffs[0].book_data['date'] == '2020-10-25'
        assert tariffs[0].book_data['fare_id'] == 56

        # не получили
        assert not movista_tariffs.get(tariff_key3)

        # проверяем, что не ходили в Мовисту лишний раз
        assert len(m_get_tariffs_data_from_provider.call_args_list) == 1

    @replace_now('2020-10-23 12:35:00')
    @replace_dynamic_setting('SUBURBAN_SELLING__MOVISTA_TARIFFS_ENABLED', True)
    def test_get_tariffs(self):
        tariffs_configuration = TariffsConfiguration(
            carriers_by_codes={'cppk': SUBURBAN_CARRIERS_BY_CODES['cppk']},
            carrier_codes_by_company_id=SUBURBAN_CARRIERS_BY_COMPANIES_ID
        )
        movista_provider = MovistaTariffsProvider(
            movista_client=get_movista_client_mock(),
            tariffs_ttl=3600,
            tariffs_configuration=tariffs_configuration
        )
        TariffsGetter(
            providers={SuburbanTariffProvider.MOVISTA: movista_provider},
            selling_flows=[SuburbanSellingFlow.VALIDATOR],
            barcode_presets=[SuburbanSellingBarcodePreset.PDF417_CPPK],
            tariffs_configuration=tariffs_configuration
        )

        MovistaTariffsFactory(
            date=date(2020, 10, 25),
            station_from=44,
            station_to=45,
            book_data=BookDataFactory(date='2020-10-25', station_from_express_id='4444', station_to_express_id='4545'),
            tariffs=[TariffFactory(fare_id=56, type=TariffTypeCode.EXPRESS, price=Decimal('10.66'))]
        )

        MovistaTariffsFactory(
            date=date(2020, 10, 24),
            station_from=42,
            station_to=44,
            can_sell=False,
            tariffs=[TariffFactory(type=TariffTypeCode.USUAL)]
        )

        class SellingTariffKeyFactory(factory.Factory):
            class Meta:
                model = TariffKey

            date = date(2020, 10, 24)
            station_from = 42
            station_to = 43
            company = Company.CPPK_ID
            tariff_type = TariffTypeCode.USUAL

        # ключ с существующими тарифами (из MovistaClientMock) -> обычный тариф
        key1 = SellingTariffKeyFactory()

        # тот же ключ, но с другим подходящим перевозчиком -> обычный тариф
        key2 = SellingTariffKeyFactory(company=Company.CPPK_AEROEX_ID)

        # тот же ключ, но с другим типом тарифа -> тариф экспресса
        key3 = SellingTariffKeyFactory(tariff_type=TariffTypeCode.EXPRESS)

        # для 42-44 тарифы есть в кэше, но can_sell=False -> тарифов нет
        key4 = SellingTariffKeyFactory(station_to=44)

        # неизвестный Мовисте тип тарифа -> тарифов нет
        key5 = SellingTariffKeyFactory(tariff_type=TariffTypeCode.AEROEXPRESS)

        # другая дата -> тарифов нет
        key6 = SellingTariffKeyFactory(date=date(2020, 12, 24))

        # не подходящий перевозчик -> тарифов нет, даже не ищем их
        key7 = SellingTariffKeyFactory(company=Company.AEROEXPRESS_ID)

        # другой ключ с существующими тарифами (из кэша)
        key8 = SellingTariffKeyFactory(
            date=date(2020, 10, 25), station_from=44, station_to=45,
            tariff_type=TariffTypeCode.EXPRESS,
        )

        tariff_keys = {key1, key2, key3, key4, key5, key6, key7, key8}

        tariffs_by_tariff_keys = movista_provider.get_tariffs(tariff_keys)

        assert len(tariffs_by_tariff_keys[key1].tariffs) == 1
        assert_that(tariffs_by_tariff_keys[key1].tariffs, contains(
            has_properties({
                'partner': SuburbanCarrierCode.CPPK,
                'tariff_type': TariffTypeCode.USUAL,
                'name': TariffTypeCode.USUAL,
                'description': 'Билет подходит для электричек, «Стандарт плюс», «Иволга» и Аэроэкспрессов на участке Одинцово – Окружная',
                'price': 48.99,
                'max_days': None,
                'valid_from': parser.parse('2020-10-24T00:00:00+03:00'),
                'valid_until': parser.parse('2020-10-25T03:00:00+03:00'),
                'book_data': has_entries({
                    'date': '2020-10-24',
                    'station_from_express_id': '4242',
                    'station_to_express_id': '4343',
                    'fare_id': 40371,
                }),
                'selling_flow': SuburbanSellingFlow.VALIDATOR
            })
        ))

        assert len(tariffs_by_tariff_keys[key2].tariffs) == 1
        assert_that(tariffs_by_tariff_keys[key2].tariffs, contains(
            has_properties({'name': 'etrain', 'price': 48.99})
        ))

        assert len(tariffs_by_tariff_keys[key3].tariffs) == 1
        assert_that(tariffs_by_tariff_keys[key3].tariffs, contains(
            has_properties({
                'partner': SuburbanCarrierCode.CPPK,
                'tariff_type': TariffTypeCode.EXPRESS,
                'name': TariffTypeCode.EXPRESS,
                'description': 'Билет подходит для поездов «РЭКС», «Спутник», «Скорый», а также обычных электричек и «гибридных» фирменных экспрессов',
                'price': 96.01,
                'max_days': None,
                'valid_from': parser.parse('2020-10-24T00:00:00+03:00'),
                'valid_until': parser.parse('2020-10-25T03:00:00+03:00'),
                'book_data': has_entries({
                    'date': '2020-10-24',
                    'station_from_express_id': '4242',
                    'station_to_express_id': '4343',
                    'fare_id': 250436,
                }),
                'selling_flow': SuburbanSellingFlow.VALIDATOR
            })
        ))

        assert tariffs_by_tariff_keys[key4].tariffs == []
        assert tariffs_by_tariff_keys[key5].tariffs == []
        assert tariffs_by_tariff_keys[key6].tariffs == []
        assert tariffs_by_tariff_keys[key7].tariffs == []

        assert len(tariffs_by_tariff_keys[key8].tariffs) == 1
        assert_that(tariffs_by_tariff_keys[key8].tariffs, contains(
            has_properties({
                'partner': SuburbanCarrierCode.CPPK,
                'tariff_type': TariffTypeCode.EXPRESS,
                'name': TariffTypeCode.EXPRESS,
                'description': 'Билет подходит для поездов «РЭКС», «Спутник», «Скорый», а также обычных электричек и «гибридных» фирменных экспрессов',
                'price': 10.66,
                'max_days': None,
                'valid_from': parser.parse('2020-10-25T00:00:00+03:00'),
                'valid_until': parser.parse('2020-10-26T03:00:00+03:00'),
                'book_data': has_entries({
                    'date': '2020-10-25',
                    'station_from_express_id': '4444',
                    'station_to_express_id': '4545',
                    'fare_id': 56,
                }),
                'selling_flow': SuburbanSellingFlow.VALIDATOR
            })
        ))

        with replace_dynamic_setting('SUBURBAN_SELLING__MOVISTA_TARIFFS_ENABLED', False):
            tariffs_by_tariff_keys = movista_provider.get_tariffs(tariff_keys)
            assert not tariffs_by_tariff_keys
