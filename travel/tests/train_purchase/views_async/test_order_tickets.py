# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

import json
from copy import deepcopy
from datetime import datetime
from decimal import Decimal

import mock
import pytest
from hamcrest import assert_that, has_properties, contains, has_entries, empty

from common.apps.train_order.enums import CoachType
from common.tester.factories import create_station
from common.tester.utils.replace_setting import replace_dynamic_setting, replace_setting
from common.utils.date import MSK_TZ, UTC_TZ
from travel.rasp.train_api.train_partners import im
from travel.rasp.train_api.train_partners.base.reserve_tickets import ReserveResponse
from travel.rasp.train_api.train_partners.im.factories.utils import mock_im
from travel.rasp.train_api.train_partners.im.get_route_info import IM_TRAIN_ROUTE_METHOD
from travel.rasp.train_api.train_partners.im.insurance.pricing import IM_INSURANCE_PRICING_METHOD
from travel.rasp.train_api.train_partners.im.reserve_tickets import CREATE_RESERVATION_METHOD
from travel.rasp.train_api.train_purchase import views_async
from travel.rasp.train_api.train_purchase.core.enums import (
    GenderChoice, TrainPartner, OrderStatus, TrainPurchaseSource
)
from travel.rasp.train_api.train_purchase.core.factories import ClientContractsFactory, ClientContractFactory
from travel.rasp.train_api.train_purchase.core.models import TrainOrder

pytestmark = [pytest.mark.dbuser, pytest.mark.mongouser, pytest.mark.usefixtures('mock_trust_client')]

DEFAULT_ORDER_TICKETS_QUERY = {
    'partner': 'im',
    'arrival': '2017-01-02T01:00:00+00:00',
    'departure': '2017-01-01T10:00:00+00:00',
    'places': [
        {
            'isUpper': False,
            'number': 2
        },
        {
            'isUpper': True,
            'number': 3
        }
    ],
    'passengers': [
        {
            'docType': 'ПН',
            'firstName': 'Илья',
            'lastName': 'Ильин',
            'birthDate': '1990-01-01',
            'sex': 'M',
            'docId': '6500112233',
            'patronymic': 'Ильич',
            'citizenship': 'RUS',
            'ageGroup': 'adults',
            'tariff': 'full',
            'loyaltyCards': [{
                'type': 'RzhdB',
                'number': '100500'
            }]
        },
        {
            'docType': 'ПН',
            'firstName': 'Иванна',
            'lastName': 'Иванова',
            'birthDate': '1990-01-02',
            'sex': 'F',
            'docId': '6500223344',
            'patronymic': 'Ивановна',
            'citizenshipGeoId': 225,
            'ageGroup': 'children',
            'tariff': 'full'
        }
    ],
    'serviceClass': '2A',
    'carType': 'compartment',
    'placeDemands': 'with_pet',
    'trainNumber': '001A',
    'trainTicketNumber': '002A',
    'carNumber': '2',
    'carOwner': 'ФПК',
    'gender': 'mixed',
    'electronicRegistration': False,
    'twoStorey': True,
    'userInfo': {
        'ip': '1.2.3.4',
        'regionId': 213,
        'phone': '+71234567890',
        'email': 'user@example.org',
        'yandex_uid': '222222'
    },
    'orderHistory': [
        {'type': 'SET_COACH'},
        {'type': 'SET_ORDER_CREATED', 'data': '6fc20049c9af4f38b9dc6d1c132bff11'},
        {'type': 'SET_COACH', 'coach': '4', 'places': ['1', '2']},
        {'type': 'SET_ORDER_CREATED', 'data': 'c09fe40fac8c4e3f978c27a61ed86008'}
    ],
    'source': {
        'reqId': 'reqId',
        'device': 'desktop',
        'utmSource': 'utmSource',
        'utmMedium': 'utmMedium',
        'utmCampaign': 'utmCampaign',
        'utmTerm': 'utmTerm',
        'utmContent': 'utmContent',
        'from': 'from',
        'gclid': 'gugloid',
        'isTransfer': True,
        'partner': 'partner',
        'subpartner': 'subpartner',
        'partnerUid': 'partnerUid',
        'testId': 'testId',
        'testBuckets': 'testBuckets',
        'icookie': 'icookie',
        'serpUuid': 'serpUuid',
    },
    'requirements': {
        'arrangement': 'compartment',
        'count': {'bottom': 1, 'upper': 0}
    }
}

DEFAULT_RESERVATION_RESPONSE = {
    'OrderId': 1,
    'Customers': [
        {
            'Index': 0,
            'BirthDate': '2000-01-01T00:00:00',
            'OrderCustomerId': 1,
            'CitizenshipCode': 'RU',
            'DocumentNumber': '1111 222222',
            'DocumentType': 'RussianPassport',
            'FirstName': 'Vladimir',
            'MiddleName': 'Vladimirovich',
            'LastName': 'Vladimirov',
            'Sex': 'Male',
        },
        {
            'Index': 1,
            'BirthDate': '2000-01-01T00:00:00',
            'OrderCustomerId': 2,
            'CitizenshipCode': 'RU',
            'DocumentNumber': '3333 444444',
            'DocumentType': 'RussianPassport',
            'FirstName': 'Vladimir',
            'MiddleName': 'Vladimirovich',
            'LastName': 'Ivanov',
            'Sex': 'Male',
        },

    ],
    'ReservationResults': [{
        'CarNumber': '100500',
        'OrderItemId': 1,
        'Amount': 123.45,
        'Blanks': [
            {
                'OrderItemBlankId': 1,
                'TariffInfo': None,
                'ServicePrice': 30.0,
                'VatRateValues': [
                    {'Rate': None, 'Value': None},
                    {'Rate': None, 'Value': None},
                ],
                'TariffType': None,
            },
            {
                'OrderItemBlankId': 2,
                'TariffInfo': None,
                'ServicePrice': 30.0,
                'VatRateValues': [
                    {'Rate': None, 'Value': None},
                    {'Rate': None, 'Value': None},
                ],
                'TariffType': None,
            },
        ],
        'OriginStation': 'МОСКВА',
        'DestinationStation': 'САНКТ-ПЕТЕРБУРГ',
        'Carrier': 'ФПК СЕВЕРНЫЙ',
        'TimeDescription': 'СТАНЦИИ СНГ.. ВРЕМЯ ОТПР МОСКОВСКОЕ',
        'ConfirmTill': '2000-01-01T12:00:00',
        'Passengers': [
            {
                'OrderItemBlankId': 1,
                'OrderCustomerReferenceIndex': 0,
                'PlacesWithType': [{'Number': '01', 'Type': None}],
                'Amount': 300.0,
                'Category': 'Adult',
            },
            {
                'OrderItemBlankId': 2,
                'OrderCustomerReferenceIndex': 1,
                'PlacesWithType': [{'Number': '02', 'Type': None}],
                'Amount': 300.0,
                'Category': 'Adult',
            },
        ],
    }]
}

IM_NO_PLACE_RESPONSE = {
    'Code': 310,
    'Message': 'В запрашиваемую дату поездов нет.',
    'MessageParams': [],
}

IM_INSURANCE_PRICING_RESPONSE = """
{"PricingResult": {"$type": "ApiContracts.Insurance.V1.Messages.Travel.RailwayTravelPricingResult, ApiContracts",
  "ProductPricingInfoList": [{"$type": "ApiContracts.Insurance.V1.Messages.Travel.RailwayTravelProductPricingInfo...",
    "Amount": 100.0,
    "Company": "Renessans",
    "Compensation": 3000.0,
    "OrderCustomerId": 1,
    "OrderItemId": 568141,
    "Package": "AccidentWithFloatPremium",
    "Provider": "P3"},
   {"$type": "ApiContracts.Insurance.V1.Messages.Travel.RailwayTravelProductPricingInfo, ApiContracts",
    "Amount": 70.00,
    "Company": "Renessans",
    "Compensation": 300.0,
    "OrderCustomerId": 2,
    "OrderItemId": 568141,
    "Package": "AccidentWithFloatPremium",
    "Provider": "P3"}]}}
"""


@pytest.fixture(autouse=True)
def _insurance_enabled():
    with replace_dynamic_setting('TRAIN_PURCHASE_INSURANCE_ENABLED', False):
        yield


@pytest.fixture(autouse=True, scope='module')
def m_get_route_info():
    with mock.patch.object(im, 'get_route_info', return_value=None, autospec=True):
        yield


@pytest.fixture
def m_reserve_tickets():
    reserve_response = ReserveResponse(reserved_to=MSK_TZ.localize(datetime(2000, 1, 1)), coach_number='09')
    with mock.patch.object(views_async, 'reserve_tickets',
                           autospec=True, return_value=reserve_response) as m_reserve_tickets:
        yield m_reserve_tickets


@pytest.fixture
def m_run_process():
    with mock.patch.object(views_async, 'run_process') as m_run_process:
        yield m_run_process


def create_order_tickets_query_without_coach_number():
    order_tickets_query_without_coach_number = deepcopy(DEFAULT_ORDER_TICKETS_QUERY)
    station_from = create_station(__={'codes': {'express': '1000'}})
    station_to = create_station(__={'codes': {'express': '2000'}})
    del order_tickets_query_without_coach_number['carNumber']
    order_tickets_query_without_coach_number.update({
        'partner': 'im',
        'stationFromId': station_from.id,
        'stationToId': station_to.id
    })
    return order_tickets_query_without_coach_number


def create_order_tickets_query_insurance():
    query = deepcopy(DEFAULT_ORDER_TICKETS_QUERY)
    station_from = create_station(__={'codes': {'express': '1000'}})
    station_to = create_station(__={'codes': {'express': '2000'}})
    query.update({
        'partner': 'im',
        'stationFromId': station_from.id,
        'stationToId': station_to.id
    })
    return query


def test_partner_errors(httpretty, m_reserve_tickets, m_run_process, async_urlconf_client, full_tariff_info):
    station_from = create_station(title='Откуда', __={'codes': {'express': '1000'}})
    station_to = create_station(title='Куда', __={'codes': {'express': '2000'}})
    order_tickets_query = dict(DEFAULT_ORDER_TICKETS_QUERY,
                               **{'stationFromId': station_from.id, 'stationToId': station_to.id})

    with replace_dynamic_setting('TRAN_PURCHASE_ENABLED_PARTNERS', ''):
        response = async_urlconf_client.post('/ru/api/order-tickets/', json.dumps(order_tickets_query),
                                             content_type='application/json')
        assert response.status_code == 400
        assert response.data == {'errors': {'partner': ['partner is disabled']}}
        assert not m_reserve_tickets.called
        assert not m_run_process.apply_async.called

    response = async_urlconf_client.post('/ru/api/order-tickets/', json.dumps(order_tickets_query),
                                         content_type='application/json')

    assert response.status_code == 503
    assert response.data == {'errors': {'order': 'No active partner contract'}}
    assert not m_reserve_tickets.called
    assert not m_run_process.apply_async.called

    mock_im(httpretty, IM_TRAIN_ROUTE_METHOD, json={})
    ClientContractsFactory(partner=TrainPartner.IM, contracts=[ClientContractFactory(is_active=True)])

    response = async_urlconf_client.post('/ru/api/order-tickets/', json.dumps(order_tickets_query),
                                         content_type='application/json')

    assert response.status_code == 201
    assert m_reserve_tickets.call_count == 1
    assert m_run_process.apply_async.call_count == 1


def test_query_parsing(httpretty, m_run_process, async_urlconf_client, full_tariff_info):
    station_from = create_station(title='Откуда', __={'codes': {'express': '1000'}})
    station_to = create_station(title='Куда', __={'codes': {'express': '2000'}})
    mock_im(httpretty, IM_TRAIN_ROUTE_METHOD, json={})
    mock_im(httpretty, CREATE_RESERVATION_METHOD, json=DEFAULT_RESERVATION_RESPONSE)
    ClientContractsFactory(partner=TrainPartner.IM, contracts=[ClientContractFactory(is_active=True)])

    response = async_urlconf_client.post(
        '/ru/api/order-tickets/',
        json.dumps(dict(DEFAULT_ORDER_TICKETS_QUERY,
                        **{'stationFromId': station_from.id, 'stationToId': station_to.id, 'trainName': 'Сапсан'})),
        content_type='application/json'
    )

    assert response.status_code == 201
    order = TrainOrder.objects.get(uid=response.data['order']['uid'])
    m_run_process.apply_async.assert_called_once_with([
        'train_booking_process', str(order.id), {'order_uid': order.uid}
    ])
    reservation_requests = [r for r in httpretty.latest_requests if CREATE_RESERVATION_METHOD in r.path]
    assert len(reservation_requests) == 1
    assert_that(order, has_properties(
        arrival=datetime(2017, 1, 2, 1),
        coach_number='100500',
        coach_owner='ФПК СЕВЕРНЫЙ',
        displayed_coach_owner='ФПК',
        coach_type=CoachType.COMPARTMENT,
        departure=datetime(2017, 1, 1, 10),
        gender=GenderChoice.MIXED,
        partner=TrainPartner.IM,
        reserved_to=MSK_TZ.localize(datetime(2000, 1, 1, 12)).astimezone(UTC_TZ).replace(tzinfo=None),
        station_from_id=station_from.id,
        station_to_id=station_to.id,
        status=OrderStatus.RESERVED,
        train_number='001A',
        train_ticket_number='002A',
        train_name='Сапсан',
        two_storey=True,
        user_info=has_properties(ip='1.2.3.4', uid=None, region_id=213, is_mobile=False, email='user@example.org',
                                 phone='+71234567890', reversed_phone='+71234567890'[::-1], yandex_uid='222222'),
        orders_created=['c09fe40fac8c4e3f978c27a61ed86008', '6fc20049c9af4f38b9dc6d1c132bff11'],
        source=has_properties(
            req_id='reqId',
            device=TrainPurchaseSource.DESKTOP,
            utm_source='utmSource',
            utm_medium='utmMedium',
            utm_campaign='utmCampaign',
            utm_term='utmTerm',
            utm_content='utmContent',
            from_='from',
            gclid='gugloid',
            is_transfer=True,
            partner='partner',
            subpartner='subpartner',
            partner_uid='partnerUid',
            test_id='testId',
            test_buckets='testBuckets',
            icookie='icookie',
            serp_uuid='serpUuid',
        )
    ))


def test_query_parsing_error(m_reserve_tickets, m_run_process, async_urlconf_client):
    response = async_urlconf_client.post('/ru/api/order-tickets/', '{}', content_type='application/json')

    assert response.status_code == 400
    assert 'errors' in response.data
    assert not m_reserve_tickets.called
    assert not m_run_process.apply_async.called


@pytest.mark.parametrize('noncritical_codes, expected_status', [
    ([310], 400),
    ([], 502),
])
def test_query_noncritical_error(httpretty, async_urlconf_client, noncritical_codes, expected_status):
    station_from = create_station(title='Откуда', __={'codes': {'express': '1000'}})
    station_to = create_station(title='Куда', __={'codes': {'express': '2000'}})
    mock_im(httpretty, IM_TRAIN_ROUTE_METHOD, json={})
    mock_im(httpretty, CREATE_RESERVATION_METHOD, json=IM_NO_PLACE_RESPONSE, status=500)
    ClientContractsFactory(partner=TrainPartner.IM, contracts=[ClientContractFactory(is_active=True)])

    with replace_dynamic_setting('TRAIN_PARTNERS_IM_NONCRITICAL_ERROR_CODES', noncritical_codes):
        response = async_urlconf_client.post(
            '/ru/api/order-tickets/',
            json.dumps(dict(DEFAULT_ORDER_TICKETS_QUERY,
                            **{'stationFromId': station_from.id, 'stationToId': station_to.id})),
            content_type='application/json',
        )

    assert response.status_code == expected_status
    assert_that(response.data, has_entries(
        'errors',
        has_entries('partner_error', has_entries('message', 'В запрашиваемую дату поездов нет.')),
    ))


def test_no_coach_number(httpretty, m_run_process, async_urlconf_client):
    ClientContractsFactory()

    mock_im(httpretty, IM_TRAIN_ROUTE_METHOD, json={})
    mock_im(httpretty, CREATE_RESERVATION_METHOD, json=DEFAULT_RESERVATION_RESPONSE)

    response = async_urlconf_client.post(
        '/ru/api/order-tickets/',
        json.dumps(create_order_tickets_query_without_coach_number()),
        content_type='application/json'
    )

    assert response.status_code == 201

    order = TrainOrder.objects.get(uid=response.data['order']['uid'])
    assert order.coach_number == '100500'

    reservation_request = next(r for r in httpretty.latest_requests if CREATE_RESERVATION_METHOD in r.path)
    assert 'CarNumber' not in reservation_request.parsed_body['ReservationItems']

    m_run_process.apply_async.assert_called_once_with([
        'train_booking_process', str(order.id), {'order_uid': order.uid}
    ])


@replace_dynamic_setting('TRAIN_PURCHASE_INSURANCE_ENABLED', True)
def test_insurance_pricing(httpretty, m_run_process, async_urlconf_client):
    ClientContractsFactory()

    mock_im(httpretty, IM_TRAIN_ROUTE_METHOD, json={})
    mock_im(httpretty, CREATE_RESERVATION_METHOD, json=DEFAULT_RESERVATION_RESPONSE)
    mock_im(httpretty, IM_INSURANCE_PRICING_METHOD, body=IM_INSURANCE_PRICING_RESPONSE)

    response = async_urlconf_client.post(
        '/ru/api/order-tickets/',
        json.dumps(create_order_tickets_query_insurance()),
        content_type='application/json'
    )
    order = TrainOrder.objects.get(uid=response.data['order']['uid'])

    assert response.status_code == 201
    assert m_run_process.apply_async.call_count == 0
    assert_that(response.data['order']['passengers'], contains(
        has_entries(
            'customerId', '1',
            'insurance', has_entries(
                'amount', Decimal('100.0'),
                'compensation', Decimal('3000.0'),
            ),
        ),
        has_entries(
            'customerId', '2',
            'insurance', has_entries(
                'amount', Decimal('70.0'),
                'compensation', Decimal('300.0'),
            )),
    ))
    assert_that(order, has_properties(
        passengers=contains(
            has_properties(
                customer_id='1',
                insurance=has_properties(
                    amount=Decimal('100.0'),
                    company='Renessans',
                    package='AccidentWithFloatPremium',
                    provider='P3',
                    compensation=Decimal('3000.0'),
                    compensation_variants=[]
                ),
            ),
            has_properties(
                customer_id='2',
                insurance=has_properties(
                    amount=Decimal('70.0'),
                    company='Renessans',
                    package='AccidentWithFloatPremium',
                    provider='P3',
                    compensation=Decimal('300.0'),
                    compensation_variants=[]
                ),
            ),
        ),
        insurance_enabled=True,
        process=has_entries(
            'suspended', True,
        ),
    ))


@replace_dynamic_setting('TRAIN_PURCHASE_INSURANCE_ENABLED', True)
def test_insurance_pricing_failed(httpretty, m_run_process, async_urlconf_client):
    ClientContractsFactory()

    mock_im(httpretty, IM_TRAIN_ROUTE_METHOD, json={})
    mock_im(httpretty, CREATE_RESERVATION_METHOD, json=DEFAULT_RESERVATION_RESPONSE)
    mock_im(httpretty, IM_INSURANCE_PRICING_METHOD, body='')

    response = async_urlconf_client.post(
        '/ru/api/order-tickets/',
        json.dumps(create_order_tickets_query_insurance()),
        content_type='application/json'
    )
    order = TrainOrder.objects.get(uid=response.data['order']['uid'])

    assert response.status_code == 201
    assert m_run_process.apply_async.call_count == 0
    assert_that(response.data['order']['passengers'], contains(
        has_entries('insurance', None),
        has_entries('insurance', None),
    ))
    assert_that(order, has_properties(
        passengers=contains(
            has_properties(insurance=None),
            has_properties(insurance=None),
        ),
        insurance_enabled=True,
        process=has_entries(
            'suspended', True,
        ),
    ))


@replace_dynamic_setting('TRAIN_PURCHASE_INSURANCE_ENABLED', False)
def test_insurance_pricing_disabled(httpretty, m_run_process, async_urlconf_client):
    ClientContractsFactory()

    mock_im(httpretty, IM_TRAIN_ROUTE_METHOD, json={})
    mock_im(httpretty, CREATE_RESERVATION_METHOD, json=DEFAULT_RESERVATION_RESPONSE)
    mock_im(httpretty, IM_INSURANCE_PRICING_METHOD, body='')

    response = async_urlconf_client.post(
        '/ru/api/order-tickets/',
        json.dumps(create_order_tickets_query_insurance()),
        content_type='application/json'
    )
    order = TrainOrder.objects.get(uid=response.data['order']['uid'])

    assert response.status_code == 201
    assert m_run_process.apply_async.call_count == 1
    assert_that(response.data['order']['passengers'], contains(
        has_entries('insurance', None),
        has_entries('insurance', None),
    ))
    assert_that(order, has_properties(
        passengers=contains(
            has_properties(insurance=None),
            has_properties(insurance=None),
        ),
        insurance_enabled=False,
        process=empty(),
    ))


@replace_setting('MAX_ORDER_QUERY_TIME', 1)
@replace_setting('MIN_TIME_TO_INSURANCE_PRICING', 2)
@replace_dynamic_setting('TRAIN_PURCHASE_INSURANCE_ENABLED', True)
def test_insurance_pricing_skip(httpretty, m_run_process, async_urlconf_client):
    ClientContractsFactory()

    mock_im(httpretty, IM_TRAIN_ROUTE_METHOD, json={})
    mock_im(httpretty, CREATE_RESERVATION_METHOD, json=DEFAULT_RESERVATION_RESPONSE)
    mock_im(httpretty, IM_INSURANCE_PRICING_METHOD, body='')
    response = async_urlconf_client.post(
        '/ru/api/order-tickets/',
        json.dumps(create_order_tickets_query_insurance()),
        content_type='application/json'
    )
    order = TrainOrder.objects.get(uid=response.data['order']['uid'])

    assert response.status_code == 201
    assert_that(response.data['order']['passengers'], contains(
        has_entries('insurance', None),
        has_entries('insurance', None),
    ))
    assert_that(order, has_properties(
        passengers=contains(
            has_properties(insurance=None),
            has_properties(insurance=None),
        ),
        insurance_enabled=True,
    ))
