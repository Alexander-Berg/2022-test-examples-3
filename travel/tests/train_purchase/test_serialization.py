# coding: utf-8

from __future__ import unicode_literals, absolute_import, division, print_function

import json
from datetime import datetime, date
from decimal import Decimal

import mock
import pytest
from hamcrest import assert_that, contains, has_entries, has_properties, contains_inanyorder, empty

from common.apps.train.models import TariffInfo
from common.apps.train_order.enums import CoachType
from common.models.geo import Country
from common.tester.factories import create_station
from common.utils.date import UTC_TZ
from travel.rasp.train_api.train_partners import im
from travel.rasp.train_api.train_partners.base import RzhdStatus
from travel.rasp.train_api.train_partners.base.get_route_info import RouteInfo, RouteStopInfo
from travel.rasp.train_api.train_purchase.core.enums import (
    AgeGroup, LoyaltyCardType, TrainPurchaseSource, TrainPartner, GenderChoice, Arrangement, DocumentType, Gender,
    TrainPartnerCredentialId, InsuranceStatus, RoutePolicy
)
from travel.rasp.train_api.train_purchase.core.factories import (
    PassengerFactory, TicketFactory, TrainOrderFactory, TrainRefundFactory, RefundPaymentFactory,
    PartnerDataFactory, InsuranceFactory, ClientContractFactory, ClientContractsFactory, PaymentFactory,
    OrderRouteInfoFactory, StationInfoFactory, InsuranceProcessFactory, TicketRefundFactory
)
from travel.rasp.train_api.train_purchase.core.models import RefundStatus, Ticket, RefundPaymentStatus
from travel.rasp.train_api.train_purchase.serialization import (
    TicketSchema, UserInfoSchema, ReserveTicketsPassengerSchema, OrderSchema, SourceSchema, ReserveTicketsSchema,
)

pytestmark = [pytest.mark.mongouser, pytest.mark.dbuser]


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
            'phone': '79222020222',
            'email': 'email@email.com',
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
            'citizenship': 'RUS',
            'ageGroup': 'children',
            'tariff': 'full'
        }
    ],
    'serviceClass': '2A',
    'internationalServiceClass': '2/4',
    'isCppk': True,
    'carType': 'compartment',
    'placeDemands': None,
    'trainNumber': '001A',
    'trainTicketNumber': '002A',
    'carNumber': '2',
    'gender': 'mixed',
    'electronicRegistration': False,
    'twoStorey': True,
    'schemeId': 555,
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
    },
    'priceExpId': 'priceExpId',
    'enableRebooking': True,
    'routePolicy': 'internal',
}


@pytest.mark.parametrize('rzhd_status, expected_isRefundable, expected', (
    (None, False, None),
    (RzhdStatus.REMOTE_CHECK_IN, True, 'REMOTE_CHECK_IN'),
    (RzhdStatus.CANCELLED, False, 'CANCELLED'),
    (RzhdStatus.REFUNDED, False, 'REFUNDED')
))
def test_rzhd_status_values(rzhd_status, expected_isRefundable, expected):
    result, errors = TicketSchema().dump(Ticket(rzhd_status=rzhd_status))

    assert not errors
    assert result['isRefundable'] == expected_isRefundable
    assert result['rzhdStatus'] == expected


def test_user_info_schema():
    info = """{"ip": "1.1.1.1", "region_id": 54, "email": "aaa@example.com", "uid": "1234567",
               "phone": "+71234567890", "yandex_uid": "222222"}"""
    data, errors = UserInfoSchema().loads(info)
    assert not errors
    assert_that(data, has_entries({
        'ip': '1.1.1.1',
        'region_id': 54,
        'email': 'aaa@example.com',
        'uid': '1234567',
        'is_mobile': False,
        'yandex_uid': '222222',
    }))


def test_user_info_schema_is_mobile():
    info = """{"ip": "1.1.1.1", "region_id": 54, "email": "aaa@example.com", "uid": "1234567", "isMobile": true,
               "phone": "+71234567890"}"""
    data, errors = UserInfoSchema().loads(info)
    assert not errors
    assert_that(data, has_entries({
        'ip': '1.1.1.1',
        'region_id': 54,
        'email': 'aaa@example.com',
        'uid': '1234567',
        'is_mobile': True
    }))


def test_user_info_schema_uid_null():
    json = """{"ip": "1.1.1.1", "region_id": "54", "email": "aaa@example.com", "uid": null,
               "phone": "+71234567890"}"""
    data, errors = UserInfoSchema().loads(json)
    assert not errors
    assert data['uid'] is None


def test_user_info_schema_no_yandex_uid():
    json = """{"ip": "1.1.1.1", "region_id": "54", "email": "aaa@example.com", "uid": "someUid",
               "phone": "+71234567890"}"""
    data, errors = UserInfoSchema().loads(json)
    assert not errors
    assert data['yandex_uid'] is None


@pytest.mark.parametrize('info, expected_error', [
    ({'ip': '1.1.1.1', 'region_id': 54, 'email': 'кириллица@email.com', 'uid': '1234567'}, 'email'),
    ({'ip': '1.1.1.1', 'region_id': 54, 'email': 'invalid_email.com', 'uid': '1234567'}, 'email'),
])
def test_invalid_user_info_schema(info, expected_error):
    data, errors = UserInfoSchema().load(info)
    assert expected_error in errors


@pytest.mark.parametrize('info, expected', [
    (
        """{"reqId": "reqId", "device": "touch", "utmSource": "utmSource", "utmCampaign": "utmCampaign",
            "utmTerm": "utmTerm", "utmContent": "utmContent", "from": "from", "gclid": "_gclid_",
            "terminal": "someTerminal"}""",
        has_entries({
            'req_id': 'reqId',
            'device': TrainPurchaseSource.TOUCH,
            'utm_source': 'utmSource',
            'utm_campaign': 'utmCampaign',
            'utm_term': 'utmTerm',
            'utm_content': 'utmContent',
            'from_': 'from',
            'gclid': '_gclid_',
            'terminal': 'someTerminal',
        })
    ),
    (
        """{"device": "desktop"}""",
        has_entries({
            'device': TrainPurchaseSource.DESKTOP,
        })
    ),
    (
        """{"device": "unknown"}""",
        has_entries({
            'device': None,
        })
    ),
    (
        """{
            "utmSource": ["zhd_google","zhd_google"],
            "utmMedium": ["search","search"],
            "utmCampaign": ["SSA_13-8_200_desktop|1645407723","1645407723"],
            "utmTerm": ["63986486878|+казань +москва +поезд","63986486878|+казань +москва +поезд"],
            "utmContent": ["315865967013|1t2","315865967013|1t2"]
        }""",
        has_entries({
            'utm_source': 'zhd_google',
            'utm_medium': 'search',
            'utm_campaign': 'SSA_13-8_200_desktop|1645407723',
            'utm_term': '63986486878|+казань +москва +поезд',
            'utm_content': '315865967013|1t2'
        })
    )
])
def test_source_schema(info, expected):
    data, errors = SourceSchema().loads(info)
    assert not errors
    assert_that(data, expected)


@pytest.mark.parametrize('info, expected', [
    (
        """{"reqId": "reqId", "isTransfer": "True"}""",
        has_entries({
            'req_id': 'reqId',
            'is_transfer': True,
        }),
    ),
    (
        """{"reqId": "reqId", "isTransfer": "Invalid value"}""",
        empty(),
    ),
])
def test_invalid_source_schema(info, expected):
    data, errors = SourceSchema().loads(info)
    assert not errors
    assert_that(data, expected)


@pytest.mark.parametrize('data, expected', [
    (
        {'ageGroup': 'children', 'tariff': 'full', 'citizenship': 'RUS'},
        has_entries(
            age_group=AgeGroup.CHILDREN,
            tariff_info=has_properties(code=TariffInfo.FULL_CODE),
            citizenship_country=has_properties(id=Country.RUSSIA_ID),
            loyalty_cards=[]
        )
    ),
    (
        {'tariff': 'full', 'citizenshipGeoId': 225, 'loyaltyCard': {'type': 'RzhdB', 'number': '10'}},
        has_entries(
            age_group=AgeGroup.ADULTS,
            tariff_info=has_properties(code=TariffInfo.FULL_CODE),
            citizenship_country=has_properties(id=Country.RUSSIA_ID),
            loyalty_cards=contains(
                has_entries(type=LoyaltyCardType.RZHD_BONUS, number='10')
            )
        )
    ),
    (
        {'tariff': 'full', 'citizenship': 'RUS', 'loyaltyCards': [{'type': 'RzhdB', 'number': '10'}]},
        has_entries(
            age_group=AgeGroup.ADULTS,
            tariff_info=has_properties(code=TariffInfo.FULL_CODE),
            citizenship_country=has_properties(id=Country.RUSSIA_ID),
            loyalty_cards=contains(
                has_entries(type=LoyaltyCardType.RZHD_BONUS, number='10')
            )
        )
    ),
    (
        {
            'tariff': 'full',
            'citizenship': 'UKR',
            'citizenshipGeoId': 225,
            'loyaltyCard': {'type': 'RzhdB', 'number': '10'},
            'loyaltyCards': [{'type': 'RzhdU', 'number': '20'}]
        },
        has_entries(
            age_group=AgeGroup.ADULTS,
            tariff_info=has_properties(code=TariffInfo.FULL_CODE),
            citizenship_country=has_properties(id=Country.RUSSIA_ID),
            loyalty_cards=contains(
                has_entries(type=LoyaltyCardType.UNIVERSAL, number='20')
            )
        )
    ),
    (
        {
            'tariff': 'full',
            'citizenship': 'RUS',
            'loyaltyCards': [
                {'type': 'RzhdU', 'number': '20'},
                {'type': 'RzhdB', 'number': '10'}
            ]
        },
        has_entries(
            age_group=AgeGroup.ADULTS,
            tariff_info=has_properties(code=TariffInfo.FULL_CODE),
            citizenship_country=has_properties(id=Country.RUSSIA_ID),
            loyalty_cards=contains(
                has_entries(type=LoyaltyCardType.UNIVERSAL, number='20'),
                has_entries(type=LoyaltyCardType.RZHD_BONUS, number='10')
            )
        )
    )
])
def test_reserve_tickets_passenger_schema(data, expected, full_tariff_info):
    base_passenger_attributes = {
        'docId': '42',
        'firstName': 'Poluekt',
        'lastName': 'Poluektov',
        'docType': 'ПН',
        'birthDate': '2000-01-01',
        'sex': 'M'
    }
    data.update(base_passenger_attributes)
    result, errors = ReserveTicketsPassengerSchema().load(data)
    assert_that(result, expected)


def test_reserve_tickets_passenger_schema_same_loyalty_cards_type(full_tariff_info):
    result, errors = ReserveTicketsPassengerSchema().load({
        'docId': '42',
        'firstName': 'Poluekt',
        'lastName': 'Poluektov',
        'docType': 'ПН',
        'birthDate': '2000-01-01',
        'sex': 'M',
        'tariff': 'full',
        'citizenship': 'RUS',
        'loyaltyCards': [
            {'type': 'RzhdU', 'number': '20'},
            {'type': 'RzhdU', 'number': '10'}
        ]
    })
    assert errors == {'loyalty_cards': ['loyalty_cards should has different types']}


@pytest.mark.parametrize('order_params, refund_params, expected_tickets', (
    (
        {},
        None,
        contains(
            has_entries({'isRefundable': True, 'isRefunding': False})
        )
    ),
    (
        {'passengers': [PassengerFactory(tickets=[TicketFactory(blank_id='1'),
                                                  TicketFactory(blank_id='2'),
                                                  TicketFactory(blank_id='3')])]},
        {'blank_ids': ['1', '3'], 'status': RefundStatus.NEW},
        contains_inanyorder(
            has_entries({'blankId': '1', 'isRefundable': False, 'isRefunding': True}),
            has_entries({'blankId': '2', 'isRefundable': True, 'isRefunding': False}),
            has_entries({'blankId': '3', 'isRefundable': False, 'isRefunding': True})
        )
    ),
    (
        {'passengers': [PassengerFactory(tickets=[TicketFactory(blank_id='1'),
                                                  TicketFactory(blank_id='2'),
                                                  TicketFactory(blank_id='3')])]},
        {'blank_ids': ['1', '3'], 'status': RefundStatus.FAILED},
        contains_inanyorder(
            has_entries({'blankId': '1', 'isRefundable': True, 'isRefunding': False}),
            has_entries({'blankId': '2', 'isRefundable': True, 'isRefunding': False}),
            has_entries({'blankId': '3', 'isRefundable': True, 'isRefunding': False})
        )
    ),
    (
        {'passengers': [PassengerFactory(tickets=[TicketFactory(blank_id='1', rzhd_status=RzhdStatus.REFUNDED),
                                                  TicketFactory(blank_id='2'),
                                                  TicketFactory(blank_id='3', rzhd_status=RzhdStatus.REFUNDED)])]},
        {'blank_ids': ['1', '3'], 'status': RefundStatus.DONE},
        contains_inanyorder(
            has_entries({'blankId': '1', 'isRefundable': False, 'isRefunding': False}),
            has_entries({'blankId': '2', 'isRefundable': True, 'isRefunding': False}),
            has_entries({'blankId': '3', 'isRefundable': False, 'isRefunding': False})
        )
    ),
))
def test_tickets_refunding_status(order_params, refund_params, expected_tickets):
    order = TrainOrderFactory(partner=TrainPartner.UFS, **order_params)
    if refund_params is not None:
        TrainRefundFactory(order_uid=order.uid, is_active=True, **refund_params)

    result, errors = OrderSchema().dump(order)

    assert not errors
    assert_that(result, has_entries('passengers', contains(has_entries('tickets', expected_tickets))))


@pytest.mark.parametrize('order_params, refunds_params, expected_tickets', (
    (
        {},
        None,
        contains(has_entries({'isExternalRefund': False}))
    ),
    (
        {
            'passengers': [PassengerFactory(tickets=[
                TicketFactory(blank_id='1'),
                TicketFactory(blank_id='2'),
                TicketFactory(blank_id='3')
            ])]
        },
        [{'blank_ids': ['1', '3'], 'status': RefundStatus.NEW, 'is_external': True}],
        contains(
            has_entries({'blankId': '1', 'isExternalRefund': True}),
            has_entries({'blankId': '2', 'isExternalRefund': False}),
            has_entries({'blankId': '3', 'isExternalRefund': True})
        )
    ),
    (
        {
            'passengers': [PassengerFactory(tickets=[
                TicketFactory(blank_id='1'),
                TicketFactory(blank_id='2'),
                TicketFactory(blank_id='3')
            ])]
        },
        [
            {'blank_ids': ['1'], 'status': RefundStatus.DONE},
            {'blank_ids': ['2'], 'status': RefundStatus.FAILED},
            {'blank_ids': ['3'], 'status': RefundStatus.FAILED, 'is_external': True},
        ],
        contains(
            has_entries({'blankId': '1', 'isExternalRefund': False}),
            has_entries({'blankId': '2', 'isExternalRefund': False}),
            has_entries({'blankId': '3', 'isExternalRefund': True})
        )
    ),
))
def test_tickets_external_refund(order_params, refunds_params, expected_tickets):
    order = TrainOrderFactory(partner=TrainPartner.UFS, **order_params)
    if refunds_params is not None:
        for refund_params in refunds_params:
            TrainRefundFactory(order_uid=order.uid, **refund_params)

    result, errors = OrderSchema().dump(order)

    assert not errors
    assert_that(result, has_entries('passengers', contains(has_entries('tickets', expected_tickets))))


def test_reserve_tickets_schema(full_tariff_info):
    station_from = create_station(title='Откуда', __={'codes': {'express': '1000'}})
    station_to = create_station(title='Куда', __={'codes': {'express': '2000'}})
    order_tickets_query = dict(DEFAULT_ORDER_TICKETS_QUERY, **{
        'partner': TrainPartner.IM.value,
        'stationFromId': station_from.id,
        'stationToId': station_to.id,
        'requirements': {
            'arrangement': 'compartment',
            'count': {'bottom': 1, 'upper': 0},
            'storey': 1,
        }
    })
    ClientContractsFactory(contracts=[ClientContractFactory(is_active=True)])
    data, errors = ReserveTicketsSchema().loads(json.dumps(order_tickets_query))
    assert not errors
    assert_that(data, has_entries({
        'partner': TrainPartner.IM,
        'bedding': False,
        'coach_number': '2',
        'coach_type': CoachType.COMPARTMENT,
        'electronic_registration': False,
        'gender': GenderChoice.MIXED,
        'place_demands': None,
        'train_number': '001A',
        'departure': UTC_TZ.localize(datetime(2017, 1, 1, 10)),
        'places': contains({'number': 2, 'is_upper': False}, {'number': 3, 'is_upper': True}),
        'requirements': has_entries({
            'arrangement': Arrangement.COMPARTMENT,
            'count': has_entries({'upper': 0, 'bottom': 1}),
            'storey': 1,
        }),
        'service_class': '2A',
        'international_service_class': '2/4',
        'is_cppk': True,
        'station_from': station_from,
        'station_to': station_to,
        'passengers': contains(
            has_entries({
                'loyalty_cards': [{'type': LoyaltyCardType.RZHD_BONUS, 'number': '100500'}],
                'age_group': AgeGroup.ADULTS,
                'birth_date': date(1990, 1, 1),
                'citizenship_country': has_properties(id=Country.RUSSIA_ID),
                'doc_id': '6500112233',
                'doc_type': DocumentType.RUSSIAN_PASSPORT,
                'first_name': 'Илья',
                'sex': Gender.MALE,
                'last_name': 'Ильин',
                'patronymic': 'Ильич',
                'phone': '79222020222',
                'email': 'email@email.com',
                'tariff_info': has_properties(code=TariffInfo.FULL_CODE)
            }),
            has_entries({
                'loyalty_card': None,
                'age_group': AgeGroup.CHILDREN,
                'birth_date': date(1990, 1, 2),
                'citizenship_country': has_properties(id=Country.RUSSIA_ID),
                'doc_id': '6500223344',
                'doc_type': DocumentType.RUSSIAN_PASSPORT,
                'first_name': 'Иванна',
                'sex': Gender.FEMALE,
                'last_name': 'Иванова',
                'patronymic': 'Ивановна',
                'tariff_info': has_properties(code=TariffInfo.FULL_CODE)
            }),
        ),
        'price_exp_id': 'priceExpId',
        'enable_rebooking': True,
        'scheme_id': 555,
        'route_policy': RoutePolicy.INTERNAL,
    }))


@pytest.mark.parametrize('is_reservation_prolonged', (True, False))
def test_order_schema_is_reservation_prolonged(is_reservation_prolonged):
    order = TrainOrderFactory(
        partner=TrainPartner.UFS,
        partner_data=PartnerDataFactory(is_reservation_prolonged=is_reservation_prolonged)
    )
    data, errors = OrderSchema().dump(order)
    assert not errors
    assert data['isReservationProlonged'] is is_reservation_prolonged


@pytest.mark.parametrize('is_suburban', (True, False, None))
def test_order_schema_is_suburban(is_suburban):
    order = TrainOrderFactory(partner=TrainPartner.UFS, partner_data=PartnerDataFactory(is_suburban=is_suburban))
    data, errors = OrderSchema().dump(order)
    assert not errors
    assert data['isSuburban'] is is_suburban


@pytest.mark.parametrize('request_im, call_count, awaited_start_ufs_title, awaited_end_ufs_title', (
    (True, 1, 'IM_ОтправлениеПоезда', 'IM_ПунктНазначения'),  # кейс для заказов с неудачным TrainRoute
    (False, 0, 'DB_ОтправлениеПоезда', 'DB_Конечная'),
))
@mock.patch.object(
    im, 'get_route_info_for_order', autospec=True,
    return_value=RouteInfo(
        RouteStopInfo('3000', 'IM_ОтправлениеПоезда', None),
        RouteStopInfo('4000', 'IM_ПунктНазначения', None)
    ),
)
def test_order_schema_start_station(m_get_route_info, request_im, call_count,
                                    awaited_start_ufs_title, awaited_end_ufs_title):
    station_from = create_station(title='Откуда')
    station_to = create_station(title='Куда')
    start_station = create_station(title='ОтправлениеПоезда', __={'codes': {'express': '3000'}},
                                   settlement={'_geo_id': 123123123, 'title': 'городОтправления'})
    end_station = create_station(title='Конечная', __={'codes': {'express': '4000'}},
                                 settlement={'_geo_id': 124124124, 'title': 'городКонечный'})
    partner_data = PartnerDataFactory(start_station_title='DB_ОтправлениеПоезда', end_station_title='DB_Конечная')
    order = TrainOrderFactory(
        station_from_id=station_from.id, station_to_id=station_to.id,
        partner=TrainPartner.IM,
        partner_data_history=[partner_data],
        route_info=OrderRouteInfoFactory(
            start_station=StationInfoFactory(id=start_station.id) if not request_im else None,
            end_station=StationInfoFactory(id=end_station.id),
        ),
    )
    data, errors = OrderSchema().dump(order)

    assert not errors
    assert m_get_route_info.call_count == call_count
    assert data['startStation']['id'] == start_station.id
    assert data['startStation']['title'] == start_station.title
    assert data['startStation']['ufsTitle'] == awaited_start_ufs_title
    assert data['startStation']['settlementGeoId'] == 123123123
    assert data['startStation']['settlementTitle'] == 'городОтправления'

    assert data['endStation']['id'] == end_station.id
    assert data['endStation']['title'] == end_station.title
    assert data['endStation']['ufsTitle'] == awaited_end_ufs_title
    assert data['endStation']['settlementGeoId'] == 124124124
    assert data['endStation']['settlementTitle'] == 'городКонечный'


@pytest.mark.parametrize('rebooking_info, expected', (
    ({}, True),
    (None, False),
))
def test_order_schema_rebooking_available(rebooking_info, expected):
    order = TrainOrderFactory(partner=TrainPartner.UFS, rebooking_info=rebooking_info)
    data, errors = OrderSchema().dump(order)
    assert not errors
    assert data['rebookingAvailable'] == expected


def test_order_schema_passenger_customer_id():
    order = TrainOrderFactory(partner=TrainPartner.UFS, passengers=[PassengerFactory(customer_id='555')])
    data, errors = OrderSchema().dump(order)
    assert not errors
    assert data['passengers'][0]['customerId'] == '555'


@pytest.mark.parametrize('train_name', [None, 'Сапсан'])
def test_order_schema_train_name(train_name):
    order = TrainOrderFactory(partner=TrainPartner.UFS, train_name=train_name)
    data, errors = OrderSchema().dump(order)
    assert not errors
    if train_name is None:
        assert data['trainName'] is None
    else:
        assert data['trainName'] == train_name


@pytest.mark.parametrize('enabled, return_value', [
    (True, True),
    (False, False),
])
def test_insurance_enabled(enabled, return_value):
    order = TrainOrderFactory(partner=TrainPartner.UFS, insurance_enabled=enabled)
    data, errors = OrderSchema().dump(order)
    assert not errors
    assert data['insuranceEnabled'] == return_value


def test_insurance_schema():
    order = TrainOrderFactory(
        partner=TrainPartner.UFS,
        passengers=[
            PassengerFactory(insurance=InsuranceFactory(operation_id=None)),
            PassengerFactory(insurance=InsuranceFactory(
                amount=Decimal(50), trust_order_id='some_order_id', operation_id='999999'
            )),
            PassengerFactory(insurance=InsuranceFactory(
                amount=Decimal(250), trust_order_id='some_order_id', operation_id='999998',
                refund_uuid='refund_uuid_refund_uuid_refund_uuid'
            )),
        ],
    )
    data, errors = OrderSchema().dump(order)
    assert not errors
    assert_that(data['passengers'], contains(
        has_entries('insurance', has_entries({'amount': Decimal(100), 'ordered': False, 'refunded': False,
                                              'operationId': None})),
        has_entries('insurance', has_entries({'amount': Decimal(50), 'ordered': True, 'refunded': False,
                                              'operationId': '999999'})),
        has_entries('insurance', has_entries({'amount': Decimal(250), 'ordered': True, 'refunded': True,
                                              'operationId': '999998'})),
    ))


@pytest.mark.parametrize('partner, utm_source, expected_credential_id', [
    (TrainPartner.IM, 'some_source', TrainPartnerCredentialId.IM),
    (TrainPartner.IM, 'suburbans', TrainPartnerCredentialId.IM_SUBURBAN),
    (TrainPartner.UFS, 'some_source', None),
])
def test_credential_id(partner, utm_source, expected_credential_id):
    station_from = create_station(title='Откуда', __={'codes': {'express': '1000'}})
    station_to = create_station(title='Куда', __={'codes': {'express': '2000'}})
    order_tickets_query = dict(DEFAULT_ORDER_TICKETS_QUERY, **{
        'stationFromId': station_from.id,
        'stationToId': station_to.id,
        'source': {'utmSource': utm_source},
        'partner': partner.value,
    })

    if not expected_credential_id:
        with pytest.raises(NotImplementedError):
            ReserveTicketsSchema().loads(json.dumps(order_tickets_query))
    else:
        data, errors = ReserveTicketsSchema().loads(json.dumps(order_tickets_query))
        assert not errors
        assert data['partner_credential_id'] == expected_credential_id


@pytest.mark.parametrize('order_blank_ids, refund_payments_params, expected_money_refunded_blank_ids', [
    (['1'], [{'refund_blank_ids': ['1'], 'refund_payment_status': RefundPaymentStatus.DONE}], ['1']),
    (['1'], [{'refund_blank_ids': ['1'], 'refund_payment_status': RefundPaymentStatus.UNKNOWN}], []),
    (['1'], [{'refund_blank_ids': ['1'], 'refund_payment_status': RefundPaymentStatus.NEW}], []),
    (['1'], [{'refund_blank_ids': ['1'], 'refund_payment_status': RefundPaymentStatus.FAILED}], []),
    (['1', '2', '3'], [
        {'refund_blank_ids': ['1'], 'refund_payment_status': RefundPaymentStatus.DONE},
        {'refund_blank_ids': ['1'], 'refund_payment_status': RefundPaymentStatus.FAILED},
        {'refund_blank_ids': ['2'], 'refund_payment_status': RefundPaymentStatus.DONE},
        {'refund_blank_ids': ['3'], 'refund_payment_status': RefundPaymentStatus.UNKNOWN},
    ], ['1', '2']),
    (['1', '2', '3'], [], []),
    (['1', '2', '3'], [{'refund_blank_ids': ['1', '2'], 'refund_payment_status': RefundPaymentStatus.DONE}], ['1', '2'])
])
@mock.patch.object(im, 'get_route_info', autospec=True, return_value=None)
def test_tickets_money_refunded(_, order_blank_ids, refund_payments_params, expected_money_refunded_blank_ids):
    order = TrainOrderFactory(passengers=[
        PassengerFactory(tickets=[TicketFactory(blank_id=b) for b in order_blank_ids])
    ])
    refund = TrainRefundFactory(order_uid=order.uid)
    for rp_params in refund_payments_params:
        RefundPaymentFactory(order_uid=order.uid, refund_uuid=refund.uuid, **rp_params)

    result, errors = OrderSchema().dump(order)

    assert not errors
    expected_tickets = contains(*[has_entries({
        'blankId': blank_id,
        'isMoneyReturned': blank_id in expected_money_refunded_blank_ids
    }) for blank_id in order_blank_ids])
    assert_that(result, has_entries('passengers', contains(has_entries('tickets', expected_tickets))))


@pytest.mark.parametrize('insurance_auto_return_uuid, expected', (
    ('refund_uuid_refund_uuid_refund_uuid', True),
    (None, False),
))
@mock.patch.object(im, 'get_route_info', autospec=True, return_value=None)
def test_order_schema_insurance_auto_return(_, insurance_auto_return_uuid, expected):
    order = TrainOrderFactory(insurance_auto_return_uuid=insurance_auto_return_uuid)
    data, errors = OrderSchema().dump(order)
    assert not errors
    assert data['insuranceAutoReturn'] == expected


@mock.patch.object(im, 'get_route_info', autospec=True, return_value=None)
def test_get_payment_receipt_url(m_get_route_info):
    order = TrainOrderFactory()
    payment = PaymentFactory(
        order_uid=order.uid,
        purchase_token='42',
        _receipt_url='https://trust-test.yandex.ru/receipts/some/?mode=pdf',
    )
    result, errors = OrderSchema().dump(order)

    assert not errors
    assert result['paymentReceiptUrl'] == payment.receipt_url


@mock.patch.object(im, 'get_route_info', autospec=True, return_value=None)
def test_new_payment(m_get_route_info):
    order = TrainOrderFactory()
    PaymentFactory(order_uid=order.uid, purchase_token=None)
    result, errors = OrderSchema().dump(order)

    assert not errors
    assert result['paymentReceiptUrl'] is None


@mock.patch.object(im, 'get_route_info', autospec=True, return_value=None)
def test_payment_refunds(m_get_route_info):
    order = TrainOrderFactory(passengers=[
        PassengerFactory(tickets=[TicketFactory(blank_id='101')], insurance=InsuranceFactory(operation_id='1001')),
        PassengerFactory(tickets=[TicketFactory(blank_id='102')], insurance=InsuranceFactory(operation_id='1002')),
        PassengerFactory(tickets=[TicketFactory(blank_id='103')], insurance=InsuranceFactory(operation_id='1003')),
    ])
    refund1 = TrainRefundFactory(order_uid=order.uid, blank_ids=[], insurance_ids=['1001', '1002', '1003'])
    refund2 = TrainRefundFactory(order_uid=order.uid, blank_ids=['101', '102'], insurance_ids=[])
    RefundPaymentFactory(
        order_uid=order.uid, refund_uuid=refund1.uuid, purchase_token='token777',
        trust_refund_id='1refund1', refund_payment_status=RefundPaymentStatus.FAILED,
        refund_blank_ids=['1001', '1002', '1003'], refund_insurance_ids=['101', '102', '103'],
    )
    refunded_payment = RefundPaymentFactory(
        order_uid=order.uid, refund_uuid=refund1.uuid, purchase_token='token777',
        trust_refund_id='1refund2', refund_payment_status=RefundPaymentStatus.DONE,
        refund_blank_ids=[], refund_insurance_ids=['101', '102', '103'],
        _refund_receipt_url='https://trust-test.yandex.ru/refunds/some/?mode=pdf'
    )
    resized_payment = RefundPaymentFactory(
        order_uid=order.uid, refund_uuid=refund2.uuid, purchase_token='token777',
        trust_reversal_id='2refund1', refund_payment_status=RefundPaymentStatus.DONE,
        refund_blank_ids=['101', '102'], refund_insurance_ids=[], payment_resized=True,
        _refund_receipt_url='https://trust-test.yandex.ru/clearing/some/?mode=pdf',
    )

    result, errors = OrderSchema().dump(order)

    assert not errors
    assert_that(result, has_entries('refundPayments', contains_inanyorder(
        has_entries({
            'refundPaymentStatus': RefundPaymentStatus.DONE,
            'blankIds': [],
            'insuranceIds': ['101', '102', '103'],
            'paymentRefundReceiptUrl': refunded_payment.refund_receipt_url,
        }),
        has_entries({
            'refundPaymentStatus': RefundPaymentStatus.DONE,
            'blankIds': ['101', '102'],
            'insuranceIds': [],
            'paymentRefundReceiptUrl': resized_payment.refund_receipt_url,
        }),
    )))


def test_insurance_status():
    order = TrainOrderFactory(insurance=InsuranceProcessFactory(status=InsuranceStatus.ACCEPTED))
    data, errors = OrderSchema().dump(order)

    assert not errors
    assert data['insuranceStatus'] == 'accepted'


def test_no_insurance_status():
    order = TrainOrderFactory()
    data, errors = OrderSchema().dump(order)

    assert not errors
    assert data['insuranceStatus'] is None

    order = TrainOrderFactory(insurance=InsuranceProcessFactory())
    data, errors = OrderSchema().dump(order)

    assert not errors
    assert data['insuranceStatus'] is None


@pytest.mark.parametrize('refund_yandex_fee, expected_amount', [
    (None, Decimal(100)),
    (Decimal(50), Decimal(150)),
])
def test_refund_amount_with_yandex_fee(refund_yandex_fee, expected_amount):
    order = TrainOrderFactory(
        passengers=[
            PassengerFactory(
                tickets=[
                    TicketFactory(
                        refund=TicketRefundFactory(
                            amount=Decimal(100),
                            refund_yandex_fee_amount=refund_yandex_fee,
                        ),
                    ),
                ],
            ),
        ],
    )
    data, errors = OrderSchema().dump(order)
    assert not errors
    assert_that(data['passengers'], contains(
        has_entries('tickets', contains(
            has_entries('refund', has_entries('amount', expected_amount))
        )),
    ))
