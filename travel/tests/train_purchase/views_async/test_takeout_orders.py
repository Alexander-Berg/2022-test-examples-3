# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import json
from collections import namedtuple
from datetime import datetime
from decimal import Decimal

import mock
import pytest
from hamcrest import assert_that, contains, has_entries
from rest_framework import status
from travel.library.python.tvm_ticket_provider import FakeTvmTicketProvider

from common.tester.factories import create_station
from common.utils import gen_hex_uuid
from travel.rasp.train_api.train_purchase.core.enums import OrderStatus, TravelOrderStatus
from travel.rasp.train_api.train_purchase.core.factories import (
    TrainOrderFactory, PartnerDataFactory, UserInfoFactory, PassengerFactory, TicketFactory, TicketPaymentFactory,
    InsuranceFactory, TicketRefundFactory
)


pytestmark = [pytest.mark.dbuser, pytest.mark.mongouser]

ServiceTicket = namedtuple('ServiceTicket', 'src')
developers_ticket = ServiceTicket(src=2001121)
not_developers_ticket = ServiceTicket(src=-1000)


def _create_orders():
    TrainOrderFactory(
        user_info=UserInfoFactory(uid='some_uid'),
        partner_data_history=[PartnerDataFactory(order_num='11')],
        station_from_id=create_station(title='Откуда').id,
        station_to_id=create_station(title='Куда', time_zone='Europe/Kiev').id,
        status=OrderStatus.DONE, travel_status=TravelOrderStatus.DONE, train_number='21',
        departure=datetime(2018, 2, 19, 12, 15), arrival=datetime(2018, 2, 19, 17, 30),
        passengers=[
            PassengerFactory(
                first_name='Васисуалий', last_name='Лоханкин', patronymic='Андреевич',
                insurance=InsuranceFactory(amount=Decimal('30'), trust_order_id='purchased'),
                tickets=[
                    TicketFactory(
                        places=['01', '02'],
                        payment=TicketPaymentFactory(amount=Decimal('100'), fee=Decimal('10')),
                        raw_tariff_title='Детский',
                        rzhd_status=0,
                    ),
                    TicketFactory(
                        places=['03', '04'],
                        payment=TicketPaymentFactory(amount=Decimal('100'), fee=Decimal('10')),
                        raw_tariff_title='Полный',
                        rzhd_status=1,
                    )
                ],
            ),
            PassengerFactory(
                first_name='Иван', last_name='Петров',
                insurance=InsuranceFactory(amount=Decimal('30'), trust_order_id='purchased',
                                           refund_uuid=gen_hex_uuid()),
                tickets=[
                    TicketFactory(
                        places=['05', '06'],
                        payment=TicketPaymentFactory(amount=Decimal('100'), fee=Decimal('10')),
                        raw_tariff_title='Полный',
                        rzhd_status=1,
                    ),
                ],
            ),
        ]
    )
    TrainOrderFactory(
        user_info=UserInfoFactory(uid='some_uid'),
        partner_data_history=[PartnerDataFactory(order_num='12')],
        station_from_id=create_station(title='Откуда-Юж.').id,
        station_to_id=create_station(title='Куда-Сев.', time_zone='Europe/Kiev').id,
        status=OrderStatus.CANCELLED, travel_status=TravelOrderStatus.CANCELLED, train_number='22',
        departure=datetime(2018, 6, 19, 12, 15), arrival=datetime(2018, 6, 19, 17, 30),
        passengers=[
            PassengerFactory(
                first_name='Васисуалий', last_name='Лоханкин', patronymic='Андреевич',
                insurance=InsuranceFactory(amount=Decimal('30')),
                tickets=[
                    TicketFactory(
                        places=['01'],
                        payment=TicketPaymentFactory(amount=Decimal('100'), fee=Decimal('10')),
                        raw_tariff_title='Полный',
                        rzhd_status=2,
                        refund=TicketRefundFactory(amount=Decimal('70')),
                    ),
                ]
            ),
        ],
    )
    TrainOrderFactory(
        user_info=UserInfoFactory(uid='some_uid'),
        partner_data_history=[PartnerDataFactory(order_num='13')],
        station_from_id=create_station(title='Откуда-Пасс.').id,
        station_to_id=create_station(title='Куда-Тов.', time_zone='Europe/Kiev').id,
        status=OrderStatus.PAID, travel_status=TravelOrderStatus.IN_PROGRESS, train_number='23',
        departure=datetime(2018, 2, 18, 12, 15), arrival=datetime(2018, 2, 19, 17, 30),
        passengers=[
            PassengerFactory(
                first_name='Васисуалий', last_name='Лоханкин', patronymic='Андреевич',
                insurance=InsuranceFactory(amount=Decimal('30'), trust_order_id='purchased',
                                           refund_uuid=gen_hex_uuid()),
                tickets=[
                    TicketFactory(
                        places=['01'],
                        payment=TicketPaymentFactory(amount=Decimal('100'), fee=Decimal('10')),
                        raw_tariff_title='Полный',
                        rzhd_status=4,
                        refund=TicketRefundFactory(amount=Decimal('70')),
                    ),
                ]
            ),
        ],
    )


@mock.patch.object(FakeTvmTicketProvider, 'check_service_ticket', return_value=developers_ticket)
def test_takeout_orders(m_check_service_ticket, async_urlconf_client):
    _create_orders()

    response = async_urlconf_client.post(
        path='/ru/api/takeout-orders/',
        data='uid=some_uid&unixtime=123456789',
        content_type='application/x-www-form-urlencoded',
        **{'HTTP_X_YA_SERVICE_TICKET': 'rasp_developers'}
    )

    m_check_service_ticket.assert_called_once_with(ticket='rasp_developers')
    assert response.status_code == status.HTTP_200_OK
    assert_that(response.data, has_entries('status', 'ok'))
    assert_that(
        json.loads(response.data['data']['orders.json']),
        contains(
            has_entries(
                'arrivalDatetime', '2018-02-19 19:30',
                'arrivalStation', 'Куда',
                'coachNumber', '2',
                'coachType', 'Купе',
                'departureDatetime', '2018-02-19 15:15',
                'departureStation', 'Откуда',
                'orderNum', '11',
                'orderStatus', 'Выполнен',
                'total', '390.00',
                'totalFee', '30.00',
                'totalRefund', '30.00',
                'trainNumber', '21',
                'travelTime', '05:15',
                'passengers', contains(
                    has_entries(
                        'amount', '200.00',
                        'fee', '20.00',
                        'fio', 'Лоханкин Васисуалий Андреевич',
                        'insuranceAmount', '30.00',
                        'places', '01, 02, 03, 04',
                        'tariffs', 'Детский, Полный',
                        'ticketStatus', 'Без электронной регистрации, Электронная регистрация'
                    ),
                    has_entries(
                        'amount', '100.00',
                        'fee', '10.00',
                        'fio', 'Петров Иван',
                        'insuranceAmount', '30.00',
                        'places', '05, 06',
                        'tariffs', 'Полный',
                        'ticketStatus', 'Электронная регистрация',
                    ),
                ),
            ),
            has_entries(
                'arrivalDatetime', '2018-06-19 20:30',
                'arrivalStation', 'Куда-Сев.',
                'coachNumber', '2',
                'coachType', 'Купе',
                'departureDatetime', '2018-06-19 15:15',
                'departureStation', 'Откуда-Юж.',
                'orderNum', '12',
                'orderStatus', 'Отменен',
                'total', '110.00',
                'totalFee', '10.00',
                'totalRefund', '0',
                'trainNumber', '22',
                'travelTime', '05:15',
                'passengers', contains(
                    has_entries(
                        'amount', '100.00',
                        'fee', '10.00',
                        'fio', 'Лоханкин Васисуалий Андреевич',
                        'insuranceAmount', '0',
                        'places', '01',
                        'tariffs', 'Полный',
                        'ticketStatus', 'Оплата не подтверждена',
                    ),
                ),
            ),
            has_entries(
                'arrivalDatetime', '2018-02-19 19:30',
                'arrivalStation', 'Куда-Тов.',
                'coachNumber', '2',
                'coachType', 'Купе',
                'departureDatetime', '2018-02-18 15:15',
                'departureStation', 'Откуда-Пасс.',
                'orderNum', '13',
                'orderStatus', 'В обработке',
                'total', '140.00',
                'totalFee', '10.00',
                'totalRefund', '100.00',
                'trainNumber', '23',
                'travelTime', '1.05:15',
                'passengers', contains(
                    has_entries(
                        'amount', '100.00',
                        'fee', '10.00',
                        'fio', 'Лоханкин Васисуалий Андреевич',
                        'insuranceAmount', '30.00',
                        'places', '01',
                        'tariffs', 'Полный',
                        'ticketStatus', 'Возвращен',
                    ),
                ),
            ),
        )
    )


@mock.patch.object(FakeTvmTicketProvider, 'check_service_ticket', return_value=developers_ticket)
def test_takeout_orders_no_data(m_check_service_ticket, async_urlconf_client):
    _create_orders()

    response = async_urlconf_client.post(
        path='/ru/api/takeout-orders/',
        data='uid=not_that_uid&unixtime=123456789',
        content_type='application/x-www-form-urlencoded',
        **{'HTTP_X_YA_SERVICE_TICKET': 'rasp_developers'}
    )

    assert response.status_code == status.HTTP_200_OK
    m_check_service_ticket.assert_called_once_with(ticket='rasp_developers')
    assert_that(response.data, has_entries('status', 'no_data'))


@mock.patch.object(FakeTvmTicketProvider, 'check_service_ticket', return_value=developers_ticket)
def test_takeout_orders_no_token(m_check_service_ticket, async_urlconf_client):
    _create_orders()

    response = async_urlconf_client.post(
        path='/ru/api/takeout-orders/',
        data='uid=some_uid&unixtime=123456789',
        content_type='application/x-www-form-urlencoded',
    )

    assert response.status_code == status.HTTP_200_OK
    assert not m_check_service_ticket.call_count
    assert_that(response.data, has_entries(
        'status', 'error',
        'error', 'TVM error: Service ticket is undefined.',
    ))


@mock.patch.object(FakeTvmTicketProvider, 'check_service_ticket', return_value=developers_ticket)
def test_takeout_orders_no_uid(m_check_service_ticket, async_urlconf_client):
    _create_orders()

    response = async_urlconf_client.post(
        path='/ru/api/takeout-orders/',
        data='not_uid=at_all&unixtime=123456789',
        content_type='application/x-www-form-urlencoded',
        **{'HTTP_X_YA_SERVICE_TICKET': 'rasp_developers'}
    )

    assert response.status_code == status.HTTP_200_OK
    m_check_service_ticket.assert_called_once_with(ticket='rasp_developers')
    assert_that(response.data, has_entries(
        'status', 'error',
        'error', 'UID is undefined.',
    ))


@mock.patch.object(FakeTvmTicketProvider, 'check_service_ticket', side_effect=[Exception('Boom!')])
def test_takeout_orders_token_exception(m_check_service_ticket, async_urlconf_client):
    _create_orders()

    response = async_urlconf_client.post(
        path='/ru/api/takeout-orders/',
        data='uid=some_uid&unixtime=123456789',
        content_type='application/x-www-form-urlencoded',
        **{'HTTP_X_YA_SERVICE_TICKET': 'rasp_developers'}
    )

    assert response.status_code == status.HTTP_200_OK
    m_check_service_ticket.assert_called_once_with(ticket='rasp_developers')
    assert_that(response.data, has_entries(
        'status', 'error',
        'error', 'TVM error: Boom!',
    ))


@mock.patch.object(FakeTvmTicketProvider, 'check_service_ticket', return_value=not_developers_ticket)
def test_takeout_orders_invalid_token(m_check_service_ticket, async_urlconf_client):
    _create_orders()

    response = async_urlconf_client.post(
        path='/ru/api/takeout-orders/',
        data='uid=some_uid&unixtime=123456789',
        content_type='application/x-www-form-urlencoded',
        **{'HTTP_X_YA_SERVICE_TICKET': 'not_rasp_developers'}
    )

    assert response.status_code == status.HTTP_200_OK
    m_check_service_ticket.assert_called_once_with(ticket='not_rasp_developers')
    assert_that(response.data, has_entries(
        'status', 'error',
        'error', 'TVM error: Service ticket is not allowed.',
    ))


@mock.patch.object(FakeTvmTicketProvider, 'check_service_ticket', return_value=developers_ticket)
def test_takeout_orders_500_error(m_check_service_ticket, async_urlconf_client):
    _create_orders()

    from travel.rasp.train_api.train_purchase.views import TrainOrder
    with mock.patch.object(TrainOrder, 'objects') as m_objects:
        m_objects.filter.side_effect = Exception('BOOM!')
        response = async_urlconf_client.post(
            path='/ru/api/takeout-orders/',
            data='uid=some_uid&unixtime=123456789',
            content_type='application/x-www-form-urlencoded',
            **{'HTTP_X_YA_SERVICE_TICKET': 'rasp_developers'}
        )

    assert response.status_code == status.HTTP_500_INTERNAL_SERVER_ERROR
    m_check_service_ticket.assert_called_once_with(ticket='rasp_developers')
    assert_that(response.data, has_entries(
        'status', 'error',
        'error', 'Неизвестная ошибка выгрузки Takeout',
    ))
