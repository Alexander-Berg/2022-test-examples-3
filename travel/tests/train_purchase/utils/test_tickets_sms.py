# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from datetime import datetime

import mock
import pytest
from hamcrest import assert_that, has_entries

import travel.rasp.train_api.train_purchase.utils.tickets_sms
from common.tester.factories import create_station
from common.tester.utils.replace_setting import replace_setting, replace_dynamic_setting
from travel.rasp.train_api.train_purchase.core.factories import PassengerFactory, TrainOrderFactory, UserInfoFactory, SourceFactory
from travel.rasp.train_api.train_purchase.core.models import TrainOrder
from travel.rasp.train_api.train_purchase.utils.tickets_sms import make_order_purchase_sms_args, send_tickets_sms

pytestmark = [pytest.mark.mongouser, pytest.mark.dbuser]
SHORTENED_URL = 'http://some.shortened.url'


@replace_setting('YASMS_DONT_SEND_ANYTHING', True)
@mock.patch.object(travel.rasp.train_api.train_purchase.utils.tickets_sms, 'ClckClient')
def test_send_tickets_sms(m_clck_client):
    m_clck_client().shorten.return_value = SHORTENED_URL
    order = TrainOrderFactory()
    yasms_id = send_tickets_sms(order.uid)
    assert yasms_id == 'log_only'
    assert m_clck_client().shorten.called


@replace_dynamic_setting('TRAIN_FRONT_URL', 'train.yandex.ru')
@replace_dynamic_setting('TRAVEL_FRONT_URL', 'travel.yandex.ru')
@mock.patch.object(travel.rasp.train_api.train_purchase.utils.tickets_sms, 'ClckClient')
@pytest.mark.parametrize('terminal, expected_url', [
    (None, 'https://train.yandex.ru/orders/{order_uid}/'),
    ('travel', 'https://travel.yandex.ru/my/order/{order_uid}/'),
])
def test_make_sms_args(m_clck_client, terminal, expected_url):
    url_type = 'booking'
    m_clck_client.LONG_TYPE = url_type
    m_shorten = m_clck_client().shorten
    m_shorten.return_value = SHORTENED_URL

    order = TrainOrderFactory(
        user_info=UserInfoFactory(email='kateov@yandex-team.ru'),
        departure=datetime(2013, 9, 20, 23, 43),
        arrival=datetime(2013, 9, 23, 13, 31),
        train_ticket_number='003A',
        coach_number='05',
        passengers=[
            PassengerFactory(tickets=[dict(blank_id='1', places=['001', '2', '3'])]),
            PassengerFactory(tickets=[dict(blank_id='2', places=['014'])])
        ],
        station_from=create_station(title='Откуда', time_zone='Asia/Yekaterinburg'),
        source=SourceFactory(terminal=terminal),
    )
    TrainOrder.fetch_stations([order])
    args = make_order_purchase_sms_args(order)

    assert_that(args, has_entries({
        'departure_date': '21\xa0сентября 2013',
        'station_to_title': 'Куда',
        'station_from_title': 'Откуда',
        'departure_time': '05:43',
        'train_number': '003A',
        'short_url': SHORTENED_URL,
    }))
    assert m_clck_client.call_args == mock.call(url_type=url_type)
    m_shorten.assert_called_once_with(expected_url.format(order_uid=order.uid))
