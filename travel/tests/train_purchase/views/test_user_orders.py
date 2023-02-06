# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

import json
from datetime import datetime, timedelta

import mock
import pytest
from django.test import Client
from hamcrest import assert_that, contains, has_entries

from common.tester.utils.datetime import replace_now
from common.tester.utils.replace_setting import replace_setting
from common.tester.factories import create_station, create_settlement
from common.utils.title_generator import DASH
from travel.rasp.train_api.train_purchase import views
from travel.rasp.train_api.train_purchase.core.enums import OrderStatus, TrainPartner, TravelOrderStatus
from travel.rasp.train_api.train_purchase.core.factories import (
    TrainOrderFactory, UserInfoFactory, PassengerFactory, PartnerDataFactory, OrderRouteInfoFactory, StationInfoFactory
)

pytestmark = [pytest.mark.dbuser, pytest.mark.mongouser]


@pytest.fixture(autouse=True)
def do_not_check_tvm_service_ticket():
    with replace_setting('CHECK_TVM_SERVICE_TICKET', False), replace_now('2019-01-01 10:00:00'):
        yield


@pytest.mark.parametrize('query_params, expected_orders, expected_counts', [
    (
        '?getCountByStatuses=true',
        [{'trainNumber': '004', 'travelStatus': 'Done'}, {'trainNumber': '001', 'travelStatus': 'Done'}],
        {'Cancelled': 1, 'Done': 2},
    ),
    (
        '?travelStatus=Done',
        [{'trainNumber': '004', 'travelStatus': 'Done'}, {'trainNumber': '001', 'travelStatus': 'Done'}],
        None,
    ),
    (
        '?travelStatus=Cancelled&getCountByStatuses=true',
        [{'trainNumber': '002', 'travelStatus': 'Cancelled'}],
        {'Cancelled': 1, 'Done': 2},
    ),
    (
        '?travelStatus=Reserved&getCountByStatuses=true',
        [],
        {'Cancelled': 1, 'Done': 2},
    ),
    (
        '?travelStatus=Reserved,Cancelled',
        [{'trainNumber': '002', 'travelStatus': 'Cancelled'}],
        None,
    ),
    (
        '?travelStatus=Done,Cancelled',
        [
            {'trainNumber': '004', 'travelStatus': 'Done'},
            {'trainNumber': '002', 'travelStatus': 'Cancelled'},
            {'trainNumber': '001', 'travelStatus': 'Done'},
        ],
        None,
    ),
])
def test_order_exists(query_params, expected_orders, expected_counts):
    TrainOrderFactory(
        partner=TrainPartner.UFS,
        status=OrderStatus.DONE,
        travel_status=TravelOrderStatus.DONE,
        finished_at=datetime(2000, 1, 1, 9),
        user_info=UserInfoFactory(uid='999999999'),
        train_number='001',
        departure=datetime(2000, 2, 1),
    )
    TrainOrderFactory(
        partner=TrainPartner.UFS,
        status=OrderStatus.DONE,
        travel_status=TravelOrderStatus.DONE,
        finished_at=datetime(2000, 1, 5, 9),
        user_info=UserInfoFactory(uid='999999999'),
        train_number='004',
        departure=datetime(2000, 2, 4),
    )
    TrainOrderFactory(
        partner=TrainPartner.UFS,
        status=OrderStatus.CANCELLED,
        travel_status=TravelOrderStatus.CANCELLED,
        finished_at=None,
        user_info=UserInfoFactory(uid='999999999'),
        train_number='002',
        departure=datetime(2000, 2, 2),
    )
    TrainOrderFactory(
        partner=TrainPartner.UFS,
        status=OrderStatus.DONE,
        travel_status=TravelOrderStatus.DONE,
        finished_at=datetime(2000, 1, 10, 9),
        user_info=UserInfoFactory(uid='111111111'),
        train_number='003',
        departure=datetime(2000, 2, 3),
    )

    with mock.patch.object(views, '_get_user_uid') as m__get_user_uid:
        m__get_user_uid.return_value = '999999999'
        response = Client().get(
            '/ru/api/user-orders/{}'.format(query_params),
            **{'HTTP_X_YA_USER_TICKET': 'some-ticket'}
        )
    assert response.status_code == 200
    data = json.loads(response.content)
    assert len(data['results']) == len(expected_orders)
    assert_that(data['results'], contains(*[has_entries(o) for o in expected_orders]))
    if expected_counts:
        assert_that(data['counts'], has_entries(expected_counts))
    else:
        assert 'counts' not in data


@pytest.mark.parametrize('coach_owner, displayed_coach_owner, expected_coach_owner, expected_displayed_coach_owner', (
    ('ФПК СЕВЕРНЫЙ', 'ФПК', 'ФПК СЕВЕРНЫЙ', 'ФПК'),
    ('ФПК СЕВЕРНЫЙ', '', 'ФПК СЕВЕРНЫЙ', 'ФПК СЕВЕРНЫЙ'),
    ('ФПК СЕВЕРНЫЙ', None, 'ФПК СЕВЕРНЫЙ', 'ФПК СЕВЕРНЫЙ'),
))
def test_coach_owner(coach_owner, displayed_coach_owner, expected_coach_owner, expected_displayed_coach_owner):
    TrainOrderFactory(
        status=OrderStatus.DONE,
        travel_status=TravelOrderStatus.DONE,
        user_info=UserInfoFactory(uid='999999999'),
        coach_owner=coach_owner,
        displayed_coach_owner=displayed_coach_owner,
    )

    with mock.patch.object(views, '_get_user_uid') as m__get_user_uid:
        m__get_user_uid.return_value = '999999999'
        response = Client().get(
            '/ru/api/user-orders/?travelStatus=Done',
            **{'HTTP_X_YA_USER_TICKET': 'some-ticket'}
        )

    assert response.status_code == 200
    data = json.loads(response.content)

    assert len(data['results']) == 1
    assert_that(data['results'][0], has_entries(
        coachOwner=expected_coach_owner,
        displayedCoachOwner=expected_displayed_coach_owner,
    ))


def test_titles():
    start_station = create_station(settlement=create_settlement(title='Старт'))
    end_station = create_station(settlement=create_settlement(title='Финиш'))
    TrainOrderFactory(
        status=OrderStatus.DONE,
        travel_status=TravelOrderStatus.DONE,
        user_info=UserInfoFactory(uid='999999999'),
        train_name='Малахит',
        route_info=OrderRouteInfoFactory(
            start_station=StationInfoFactory(station=start_station),
            end_station=StationInfoFactory(station=end_station),
        )
    )

    with mock.patch.object(views, '_get_user_uid') as m__get_user_uid:
        m__get_user_uid.return_value = '999999999'
        response = Client().get(
            '/ru/api/user-orders/?travelStatus=Done',
            **{'HTTP_X_YA_USER_TICKET': 'some-ticket'}
        )

    assert response.status_code == 200
    data = json.loads(response.content)

    assert len(data['results']) == 1
    assert_that(data['results'][0], has_entries(
        brandTitle='Малахит',
        trainTitle='Старт {} Финиш'.format(DASH),
    ))


def test_no_orders():
    TrainOrderFactory(
        status=OrderStatus.PAID,
        travel_status=TravelOrderStatus.IN_PROGRESS,
        finished_at=datetime(2000, 1, 1, 9),
        user_info=UserInfoFactory(uid='999999999'),
        train_number='001'
    )
    with mock.patch.object(views, '_get_user_uid') as m__get_user_uid:
        m__get_user_uid.return_value = '1111111111'
        response = Client().get('/ru/api/user-orders/', **{'HTTP_X_YA_USER_TICKET': 'some-ticket'})

    assert response.status_code == 200
    data = json.loads(response.content)
    assert data['results'] == []


def test_removed_order():
    TrainOrderFactory(
        removed=True,
        status=OrderStatus.DONE,
        travel_status=TravelOrderStatus.DONE,
        user_info=UserInfoFactory(uid='999999999'),
        train_number='001'
    )
    with mock.patch.object(views, '_get_user_uid') as m__get_user_uid:
        m__get_user_uid.return_value = '999999999'
        response = Client().get('/ru/api/user-orders/', **{'HTTP_X_YA_USER_TICKET': 'some-ticket'})

    assert response.status_code == 200
    data = json.loads(response.content)
    assert data['results'] == []


def test_paging():
    [TrainOrderFactory(
        partner=TrainPartner.UFS,
        status=OrderStatus.DONE,
        travel_status=TravelOrderStatus.DONE,
        finished_at=datetime(2000, 1, 1, 9) + timedelta(days=i),
        user_info=UserInfoFactory(uid='999999999'),
        train_number='001'
    ) for i in range(10)]
    with mock.patch.object(views, '_get_user_uid') as m__get_user_uid:
        m__get_user_uid.return_value = '999999999'
        response = Client().get('/ru/api/user-orders/?offset=2&limit=3', **{'HTTP_X_YA_USER_TICKET': 'some-ticket'})

    assert response.status_code == 200
    data = json.loads(response.content)
    assert len(data['results']) == 3


def test_tvm_exception():
    with mock.patch.object(views, 'tvm_factory', autospec=True) as m_tvm_factory:
        m_tvm_factory.get_provider.return_value.check_user_ticket.side_effect = [Exception()]
        response = Client().get('/ru/api/user-orders/', **{'HTTP_X_YA_USER_TICKET': 'some-ticket'})
        m_tvm_factory.get_provider.return_value.check_user_ticket.assert_called_once_with('some-ticket')
    assert response.status_code == 403


@pytest.mark.parametrize('query_params, expected_orders', [
    (
        '?findString=evg',
        [{'trainNumber': '004'}, {'trainNumber': '001'}]
    ),
    (
        '?findString=312',
        [{'trainNumber': '002'}, {'trainNumber': '001'}]
    ),
    (
        '?findString=city',
        [{'trainNumber': '003'}, {'trainNumber': '002'}, {'trainNumber': '001'}]
    ),
    (
        '?findString=empty',
        []
    ),
    (
        '?findString=.',
        []
    ),
    (
        '?findString=shadr',
        [{'trainNumber': '004'}]
    ),
])
def test_find_string(query_params, expected_orders):
    TrainOrderFactory(
        partner=TrainPartner.UFS,
        status=OrderStatus.DONE,
        travel_status=TravelOrderStatus.DONE,
        finished_at=datetime(2000, 1, 1, 9),
        user_info=UserInfoFactory(uid='999999999'),
        train_number='001',
        passengers=[PassengerFactory(first_name='Evgeniy')],
        partner_data_history=[PartnerDataFactory(order_num='000000000')],
        route_info=OrderRouteInfoFactory(start_station=StationInfoFactory(settlement_title='City312')),
        departure=datetime(2000, 2, 1),
    )
    TrainOrderFactory(
        partner=TrainPartner.UFS,
        status=OrderStatus.DONE,
        travel_status=TravelOrderStatus.DONE,
        finished_at=datetime(2000, 1, 1, 9),
        user_info=UserInfoFactory(uid='999999999'),
        train_number='002',
        passengers=[PassengerFactory(last_name='McClane')],
        partner_data_history=[PartnerDataFactory(order_num='312')],
        route_info=OrderRouteInfoFactory(end_station=StationInfoFactory(settlement_title='City54')),
        departure=datetime(2000, 2, 2),
    )
    TrainOrderFactory(
        partner=TrainPartner.UFS,
        status=OrderStatus.DONE,
        travel_status=TravelOrderStatus.DONE,
        finished_at=datetime(2000, 1, 10, 9),
        user_info=UserInfoFactory(uid='999999999'),
        train_number='003',
        passengers=[PassengerFactory(first_name='Евгений')],
        partner_data_history=[PartnerDataFactory(order_num='000000000')],
        route_info=OrderRouteInfoFactory(from_station=StationInfoFactory(title='City17')),
        departure=datetime(2000, 2, 3),
    )
    TrainOrderFactory(
        partner=TrainPartner.UFS,
        status=OrderStatus.DONE,
        travel_status=TravelOrderStatus.DONE,
        finished_at=datetime(2000, 1, 5, 9),
        user_info=UserInfoFactory(uid='999999999'),
        train_number='004',
        passengers=[PassengerFactory(patronymic='Evgenievitch')],
        partner_data_history=[PartnerDataFactory(order_num='000000000')],
        route_info=OrderRouteInfoFactory(to_station=StationInfoFactory(title='Shadrinsk')),
        departure=datetime(2000, 2, 4),
    )

    with mock.patch.object(views, '_get_user_uid') as m__get_user_uid:
        m__get_user_uid.return_value = '999999999'
        response = Client().get(
            '/ru/api/user-orders/{}'.format(query_params),
            **{'HTTP_X_YA_USER_TICKET': 'some-ticket'}
        )
    assert response.status_code == 200
    data = json.loads(response.content)
    assert len(data['results']) == len(expected_orders)
    assert_that(data['results'], contains(*[has_entries(o) for o in expected_orders]))


def test_order_by():
    TrainOrderFactory(
        uid='55555555555555555555555555555550',
        user_info=UserInfoFactory(uid='999999999'),
        travel_status=TravelOrderStatus.DONE,
        departure=datetime(2019, 1, 1),
        train_number='001',
    )
    TrainOrderFactory(
        uid='55555555555555555555555555555551',
        user_info=UserInfoFactory(uid='999999999'),
        travel_status=TravelOrderStatus.DONE,
        departure=datetime(2019, 1, 1, 12),
        train_number='002',
    )
    TrainOrderFactory(
        uid='55555555555555555555555555555552',
        user_info=UserInfoFactory(uid='999999999'),
        travel_status=TravelOrderStatus.DONE,
        departure=datetime(2019, 1, 2),
        train_number='003',
    )
    TrainOrderFactory(
        uid='55555555555555555555555555555553',
        user_info=UserInfoFactory(uid='999999999'),
        travel_status=TravelOrderStatus.DONE,
        departure=datetime(2019, 1, 3),
        train_number='004',
    )
    TrainOrderFactory(
        uid='55555555555555555555555555555554',
        user_info=UserInfoFactory(uid='999999999'),
        travel_status=TravelOrderStatus.DONE,
        departure=datetime(2018, 12, 30),
        train_number='005',
    )
    TrainOrderFactory(
        uid='55555555555555555555555555555555',
        user_info=UserInfoFactory(uid='999999999'),
        travel_status=TravelOrderStatus.DONE,
        departure=datetime(2018, 12, 31),
        train_number='006',
    )
    TrainOrderFactory(
        uid='05555555555555555555555555555556',
        user_info=UserInfoFactory(uid='999999999'),
        travel_status=TravelOrderStatus.DONE,
        departure=datetime(2018, 12, 30),
        train_number='007',
    )
    TrainOrderFactory(
        uid='a5555555555555555555555555555557',
        user_info=UserInfoFactory(uid='999999999'),
        travel_status=TravelOrderStatus.DONE,
        departure=datetime(2019, 1, 1),
        train_number='008',
    )

    with mock.patch.object(views, '_get_user_uid') as m__get_user_uid:
        m__get_user_uid.return_value = '999999999'
        response = Client().get(
            '/ru/api/user-orders/',
            **{'HTTP_X_YA_USER_TICKET': 'some-ticket'}
        )

    assert response.status_code == 200
    data = json.loads(response.content)
    assert_that(data['results'], contains(
        has_entries({'trainNumber': '001'}),
        has_entries({'trainNumber': '008'}),
        has_entries({'trainNumber': '002'}),
        has_entries({'trainNumber': '003'}),
        has_entries({'trainNumber': '004'}),
        has_entries({'trainNumber': '006'}),
        has_entries({'trainNumber': '007'}),
        has_entries({'trainNumber': '005'}),
    ))
