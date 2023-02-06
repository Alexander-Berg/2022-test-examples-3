# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from datetime import datetime

import pytest
from hamcrest import assert_that, has_properties, has_entries

from common.tester.utils.replace_setting import replace_dynamic_setting
from common.tester.utils.datetime import replace_now
from common.tester.factories import create_station

from travel.rasp.library.python.api_clients.travel_api.providers.movista import WicketTypeCode

from travel.rasp.suburban_selling.selling.order.helpers import ForbiddenOrderError, WrongOrderDataError
from travel.rasp.suburban_selling.selling.movista.order import (
    get_movista_wicket_device, WrongWicketError, MovistaServiceFactory
)
from travel.rasp.suburban_selling.selling.movista.factories import MovistaStationsFactory
from travel.rasp.suburban_selling.selling.movista.models import MovistaStations


pytestmark = [pytest.mark.dbuser]


def test_get_movista_wicket_device():
    MovistaStations.objects().delete()
    MovistaStationsFactory(station_id=100, has_wicket=True, wicket_type='MID2Tutorial')
    MovistaStationsFactory(station_id=200, has_wicket=True, wicket_type='PA2validatorTutorial')
    MovistaStationsFactory(station_id=300, has_wicket=True, wicket_type='validatorTutorial')
    MovistaStationsFactory(station_id=400, has_wicket=True, wicket_type='unknown')

    wicket_device = get_movista_wicket_device(100)
    assert_that(wicket_device, has_properties({
        'wicket_type_code': WicketTypeCode.TURNSTILE,
        'device_code': 'MID2Turnstile'
    }))

    wicket_device = get_movista_wicket_device(200)
    assert_that(wicket_device, has_properties({
        'wicket_type_code': WicketTypeCode.VALIDATOR,
        'device_code': 'PA2Validator'
    }))

    wicket_device = get_movista_wicket_device(300)
    assert_that(wicket_device, has_properties({
        'wicket_type_code': WicketTypeCode.VALIDATOR,
        'device_code': 'OldValidator'
    }))

    with pytest.raises(WrongWicketError) as ex:
        get_movista_wicket_device(400)
    assert ex.value.message == 'Wrong type of wicket: unknown, station: 400'


def test_movista_service_raise_if_order_forbidden():
    with replace_dynamic_setting('SUBURBAN_SELLING__MOVISTA_ORDER_ENABLED', False):
        with pytest.raises(ForbiddenOrderError) as ex:
            MovistaServiceFactory().raise_if_order_forbidden()
        assert ex.value.message == 'Movista order is forbidden by the setting SUBURBAN_SELLING__MOVISTA_ORDER_ENABLED'

    with replace_dynamic_setting('SUBURBAN_SELLING_ENABLED', False):
        with pytest.raises(ForbiddenOrderError) as ex:
            MovistaServiceFactory().raise_if_order_forbidden()
        assert ex.value.message == 'Movista order is forbidden by the setting SUBURBAN_SELLING_ENABLED'

    with replace_dynamic_setting('SUBURBAN_SELLING__MOVISTA_ORDER_ENABLED', True):
        with replace_dynamic_setting('SUBURBAN_SELLING_ENABLED', True):
            MovistaServiceFactory().raise_if_order_forbidden()


@replace_now(datetime(2021, 8, 27, 2))
def test_movista_service_make_book_data():
    MovistaStations.objects().delete()
    MovistaStationsFactory(station_id=101, has_wicket=True, wicket_type='MID2Tutorial')

    order_data = {
        'station_from': create_station(id=101),
        'book_data': {
            'date': '2021-08-27',
            'station_from_express_id': '1111',
            'station_to_express_id': '2222',
            'fare_id': 3333
        }
    }
    book_data = MovistaServiceFactory().make_book_data(order_data)

    assert_that(book_data.to_json(), has_entries({
        'date': '2021-08-27',
        'station_from_express_id': 1111,
        'station_to_express_id': 2222,
        'fare_id': 3333,
        'wicket': has_entries({
            'type': WicketTypeCode.TURNSTILE,
            'device_type': 'MID2Turnstile'
        })
    }))

    order_data['book_data']['date'] = '2021-08-26'
    with pytest.raises(WrongOrderDataError):
        MovistaServiceFactory().make_book_data(order_data)
