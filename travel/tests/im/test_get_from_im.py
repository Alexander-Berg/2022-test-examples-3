# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from datetime import datetime, date

import mock
from freezegun import freeze_time
from hamcrest import assert_that, has_properties, has_entries, contains_inanyorder

from travel.rasp.suburban_selling.selling.im.factories import ImTariffsFactory, BookDataFactory
from travel.rasp.suburban_selling.selling.im.models import IM_TRAIN_IS_AVAILABLE, ImTariffs
from travel.rasp.suburban_selling.selling.im.get_from_im import (
    make_im_train_data_from_segment, get_tariffs_data_from_provider, save_data_to_cache,
    ImTrainData, get_tariffs_from_im
)


def test_make_im_train_data_from_segment():
    im_train_data = make_im_train_data_from_segment({
        'Carriers': ['СЗППК'],
        'Provider': 'P6',
        'IsSaleForbidden': False,
        'TrainNumber': '111/112',
        'IsSuburban': True,
        'CarGroups': [{
            'MinPrice': 100,
            'AvailabilityIndication': IM_TRAIN_IS_AVAILABLE
        }],
        'LocalDepartureDateTime': '2021-07-25T01:00:00'
    })

    assert_that(im_train_data, has_properties({
        'carrier_im_code': 'СЗППК',
        'train_number': '111/112',
        'departure_dt': '2021-07-25T01:00:00',
        'price': 100,
        'im_provider': 'P6',
        'is_sale_forbidden': False,
        'availability_indication': IM_TRAIN_IS_AVAILABLE
    }))


class ImClientStub(object):
    def __init__(self, train_pricing_response):
        self.train_pricing_response = train_pricing_response

    def train_pricing(self, station_from_express_id, station_to_express_id, date):
        assert station_from_express_id
        assert station_to_express_id
        assert date

        return self.train_pricing_response

    def dt_to_im_date(self, dt):
        return dt.strftime('%Y-%m-%d')


class BookDataStub(object):
    def __init__(self, station_from_express_id, station_to_express_id, date):
        self.station_from_express_id = station_from_express_id
        self.station_to_express_id = station_to_express_id
        self.date = date


class ITariffsStub(object):
    def __init__(self, book_data):
        self.book_data = book_data


def test_get_tariffs_data_from_provider():
    book_data = BookDataStub(
        station_from_express_id='express1',
        station_to_express_id='express2',
        date='2021-07-25'
    )

    im_client = ImClientStub({
        'Trains': [
            {
                'Carriers': ['СЗППК'],
                'Provider': 'P6',
                'IsSaleForbidden': False,
                'TrainNumber': '666/667',
                'IsSuburban': True,
                'CarGroups': [{
                    'MinPrice': 100,
                    'AvailabilityIndication': IM_TRAIN_IS_AVAILABLE
                }],
                'LocalDepartureDateTime': '2021-07-25T01:00:00'
            },
            {
                'Carriers': ['МТППК'],
                'Provider': 'P7',
                'IsSaleForbidden': True,
                'TrainNumber': '668',
                'IsSuburban': True,
                'CarGroups': [{
                    'MinPrice': 200,
                    'AvailabilityIndication': 'Error'
                }],
                'LocalDepartureDateTime': '2021-07-25T02:00:00'
            },
            {
                'Carriers': ['МТППК'],
                'Provider': 'P8',
                'IsSaleForbidden': False,
                'TrainNumber': '669',
                'IsSuburban': False,
                'CarGroups': [{
                    'MinPrice': 200,
                    'AvailabilityIndication': IM_TRAIN_IS_AVAILABLE
                }],
                'LocalDepartureDateTime': '2021-07-25T02:00:00'
            }
        ]
    })

    now = datetime(2021, 7, 23, 12, 30)
    with freeze_time(now):
        trains = list(get_tariffs_data_from_provider(im_client, book_data))

        assert len(trains) == 2
        assert_that(trains, contains_inanyorder(
            has_properties({
                'carrier_im_code': 'СЗППК',
                'train_number': '666/667',
                'departure_dt': '2021-07-25T01:00:00',
                'price': 100,
                'im_provider': 'P6',
                'is_sale_forbidden': False,
                'availability_indication': IM_TRAIN_IS_AVAILABLE
            }),
            has_properties({
                'carrier_im_code': 'МТППК',
                'train_number': '668',
                'departure_dt': '2021-07-25T02:00:00',
                'price': 200,
                'im_provider': 'P7',
                'is_sale_forbidden': True,
                'availability_indication': 'Error'
            })
        ))


def test_save_data_to_cache():
    ImTariffsFactory(
        date=datetime(2021, 10, 31),
        station_from=100,
        station_to=200,
    )
    now = datetime(2021, 10, 31, 12)
    with freeze_time(now):
        save_data_to_cache(
            departure_date=datetime(2021, 10, 31),
            station_from=100,
            station_to=200,
            trains=[
                ImTrainData(
                    carrier_im_code='СЗППК',
                    train_number='6000',
                    departure_dt='2021-10-31T16:15:00',
                    price=100,
                    im_provider='P6',
                    is_sale_forbidden=False,
                    availability_indication=IM_TRAIN_IS_AVAILABLE
                ),
                ImTrainData(
                    carrier_im_code='МТППК',
                    train_number='7000',
                    departure_dt='2021-10-31T17:15:00',
                    price=200,
                    im_provider='P7',
                    is_sale_forbidden=True,
                    availability_indication='Error'
                )
            ]
        )

    im_tariffs_list = list(ImTariffs.objects.filter(station_from=100, station_to=200, date=date(2021, 10, 31)))

    assert len(im_tariffs_list) == 1
    assert_that(im_tariffs_list[0], has_properties({
        'station_from': 100,
        'station_to': 200,
        'date': date(2021, 10, 31),
        'updated': datetime(2021, 10, 31, 12),
        'im_trains': contains_inanyorder(
            has_entries({
                'carrier_im_code': 'СЗППК',
                'train_number': '6000',
                'departure_dt': '2021-10-31T16:15:00',
                'price': 100,
                'im_provider': 'P6',
                'is_sale_forbidden': False,
                'availability_indication': IM_TRAIN_IS_AVAILABLE
            }),
            has_entries({
                'carrier_im_code': 'МТППК',
                'train_number': '7000',
                'departure_dt': '2021-10-31T17:15:00',
                'price': 200,
                'im_provider': 'P7',
                'is_sale_forbidden': True,
                'availability_indication': 'Error'
            }),
        )
    }))


def test_get_tariffs_from_im():
    ImTariffsFactory(
        date=datetime(2021, 10, 30),
        station_from=100,
        station_to=200,
        book_data=BookDataFactory(
            date='2020-10-30',
            station_from_express_id='1000',
            station_to_express_id='2000'
        )
    )
    with mock.patch(
        'travel.rasp.suburban_selling.selling.im.get_from_im.get_tariffs_data_from_provider', return_value='trains'
    ) as m_get_tariffs_data_from_provider:
        with mock.patch(
            'travel.rasp.suburban_selling.selling.im.get_from_im.save_data_to_cache'
        ) as m_save_data_to_cache:
            get_tariffs_from_im(datetime(2021, 10, 30), 100, 200)

            assert m_get_tariffs_data_from_provider.call_count == 1
            assert len(m_get_tariffs_data_from_provider.call_args_list[0][0]) == 2
            assert_that(m_get_tariffs_data_from_provider.call_args_list[0][0][1], has_properties({
                'date': '2020-10-30',
                'station_from_express_id': '1000',
                'station_to_express_id': '2000'
            }))

            assert m_save_data_to_cache.call_count == 1
            assert m_save_data_to_cache.call_args_list[0][0][0] == datetime(2021, 10, 30)
            assert m_save_data_to_cache.call_args_list[0][0][1] == 100
            assert m_save_data_to_cache.call_args_list[0][0][2] == 200
            assert m_save_data_to_cache.call_args_list[0][0][3] == 'trains'
