# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

from datetime import datetime, timedelta
from decimal import Decimal

import mock
import pytest
from hamcrest import (
    anything, assert_that, contains, has_length, has_properties, not_, empty, has_entries
)
from httpretty import Response

import travel.rasp.train_api.train_purchase.utils.order_tickets  # noqa
from common.apps.train_order.enums import CoachType
from common.data_api.billing.trust_client import TrustClient, TrustPaymentStatuses
from common.models.geo import Country
from common.test_utils.workflow import create_process
from common.tester.factories import create_station, create_settlement
from common.tester.utils.datetime import replace_now
from common.tester.utils.replace_setting import replace_dynamic_setting
from common.utils.date import MSK_TZ, UTC_TZ
from travel.rasp.library.python.common23.date.environment import now_aware
from travel.rasp.train_api.train_partners.base import create_tax
from travel.rasp.train_api.train_partners.base.get_order_info import OrderInfoResult, PassengerInfo
from travel.rasp.train_api.train_partners.base.reserve_tickets import ReserveResponse, ReserveResponseTicket
from travel.rasp.train_api.train_partners.im import ReservationManager
from travel.rasp.train_api.train_partners.im.factories.create_reservation import ImCreateReservationFactory
from travel.rasp.train_api.train_partners.im.factories.utils import mock_im
from travel.rasp.train_api.train_partners.im.get_route_info import IM_TRAIN_ROUTE_METHOD
from travel.rasp.train_api.train_partners.im.reserve_tickets import CREATE_RESERVATION_METHOD, WrongPlaces, PlacesInfo
from travel.rasp.train_api.train_purchase.core.enums import (
    AgeGroup, DocumentType, Gender, GenderChoice, LoyaltyCardType, PlacesType, TrainPartner, OrderStatus, Arrangement,
    RebookingStatus, PlacesOption, TrainPartnerCredentialId, TravelOrderStatus, RoutePolicy
)
from travel.rasp.train_api.train_purchase.core.factories import (
    ClientContractFactory, PaymentFactory, PassengerFactory, TrainOrderFactory
)
from travel.rasp.train_api.train_purchase.core.models import TicketPayment, UserInfo, Tax, PartnerData
from travel.rasp.train_api.train_purchase.factories import ReserveOrderDataFactory
from travel.rasp.train_api.train_purchase.utils import order_tickets
from travel.rasp.train_api.train_purchase.utils.fee_calculator import TicketCost
from travel.rasp.train_api.train_purchase.utils.order_tickets import (
    _make_ticket_payment, create_order, reserve_tickets, rebooking, apply_personal_data, RebookingError,
    _fill_points_ids_and_names
)

pytestmark = [pytest.mark.mongouser, pytest.mark.dbuser]


IM_TRAIN_ROUTE_RESPONSE = [Response(body="""
{
  "Routes": [
    {
      "RouteStops": [
        {
          "StationCode": 3000,
          "StationName": "ОТКУДА ПОЕЗД",
          "CityName": "ГОРОД 17",
          "DepartureTime": "23:40",
          "ArrivalTime": "",
          "LocalArrivalTime": null,
          "RouteStopType": "Departure",
          "StopDuration": 0,
          "TimeDescription": "NoValue",
          "LocalDepartureTime": "23:40",
          "Clarification": null
        },
        {
          "ArrivalTime": "12:59",
          "CityName": null,
          "Clarification": null,
          "DepartureTime": "13:00",
          "LocalArrivalTime": "12:59",
          "LocalDepartureTime": "13:00",
          "RouteStopType": "Intermediate",
          "StationCode": "1000",
          "StationName": "ОТКУДА",
          "StopDuration": 1,
          "TimeDescription": "Local"
        },
        {
          "ArrivalTime": "04:00",
          "CityName": null,
          "Clarification": null,
          "DepartureTime": "04:01",
          "LocalArrivalTime": "04:00",
          "LocalDepartureTime": "04:01",
          "RouteStopType": "Intermediate",
          "StationCode": "2000",
          "StationName": "КУДА",
          "StopDuration": 1,
          "TimeDescription": "Local"
        },
        {
          "StationCode": 4000,
          "StationName": "КУДА ПОЕЗД",
          "CityName": "ГОРОД 18",
          "DepartureTime": "",
          "ArrivalTime": "08:36",
          "LocalArrivalTime": "08:36",
          "RouteStopType": "Arrival",
          "StopDuration": 0,
          "TimeDescription": "NoValue",
          "LocalDepartureTime": null,
          "Clarification": null
        }
      ],
      "Name": "ОСНОВНОЙ МАРШРУТ",
      "OriginName": "МОСКВА ОКТ",
      "DestinationName": "С-ПЕТЕР-ГЛ"
    }
  ]
}
""")]


def create_order_data(
        station_from=None, station_to=None, coach_type=CoachType.COMPARTMENT, bedding=False,
        bedding_tariff=Decimal(100), country=None, price_exp_id='priceExpId', yandex_uid='someUid',
):
    return {
        'arrival': UTC_TZ.localize(datetime(2017, 1, 2, 1)),
        'bedding': bedding,
        'bedding_tariff': bedding_tariff,
        'coach_number': '2',
        'coach_type': coach_type,
        'departure': UTC_TZ.localize(datetime(2017, 1, 1, 10)),
        'electronic_registration': True,
        'enable_rebooking': False,
        'experiment': None,
        'gender': GenderChoice.MIXED,
        'is_cppk': False,
        'partner': TrainPartner.IM,
        'partner_credential_id': TrainPartnerCredentialId.IM,
        'passengers': [
            {
                'doc_id': '100500',
                'doc_type': DocumentType.RUSSIAN_PASSPORT,
                'age_group': AgeGroup.ADULTS,
                'birth_date': datetime(1980, 1, 1),
                'citizenship_country': country,
                'first_name': 'Иван',
                'last_name': 'Иванов',
                'patronymic': 'Иванович',
                'sex': Gender.MALE,
                'phone': '79222020001',
                'email': 'email1@email.com',
                'loyalty_cards': [{
                    'type': LoyaltyCardType.RZHD_BONUS,
                    'number': '12345',
                }],
                'tariff_info': mock.Mock(im_request_code='Full'),
            },
            {
                'doc_id': '200300',
                'doc_type': DocumentType.RUSSIAN_INTERNATIONAL_PASSPORT,
                'age_group': AgeGroup.CHILDREN,
                'birth_date': datetime(1990, 2, 1),
                'citizenship_country': country,
                'first_name': 'Сидора',
                'last_name': 'Сидорова',
                'patronymic': 'Сидоровна',
                'sex': Gender.FEMALE,
                'phone': '79222020002',
                'email': 'email2@email.com',
                'loyalty_cards': [{
                    'type': LoyaltyCardType.UNIVERSAL,
                    'number': '54321',
                }],
                'tariff_info': mock.Mock(im_request_code='Junior'),
            },
            {
                'doc_id': '400600',
                'doc_type': DocumentType.BIRTH_CERTIFICATE,
                'age_group': AgeGroup.BABIES,
                'birth_date': datetime(2016, 1, 1),
                'citizenship_country': country,
                'first_name': 'Петр',
                'last_name': 'Петров',
                'patronymic': 'Петрович',
                'sex': Gender.MALE,
                'phone': None,
                'email': None,
                'loyalty_cards': [],
                'tariff_info': mock.Mock(im_request_code='Baby'),
            },
        ],
        'place_demands': PlacesOption.WITH_PET,
        'places': [
            {'number': 1, 'is_upper': True},
            {'number': 5, 'is_upper': False},
        ],
        'price_exp_id': price_exp_id,
        'requirements': {
            'arrangement': 'compartment',
            'count': {
                'upper': 1,
                'bottom': 1,
            },
            'storey': 2,
        },
        'additional_place_requirements': 'some-strange-requirements',
        'route_policy': RoutePolicy.INTERNAL,
        'scheme_id': 555,
        'source': {
            'terminal': 'someTerminal',
        },
        'service_class': '2М',
        'international_service_class': '2/4',
        'station_from': station_from or create_station(title='Откуда', __={'codes': {'express': '1000'}}),
        'station_to': station_to or create_station(title='Куда', __={'codes': {'express': '2000'}}),
        'train_number': '001A',
        'train_ticket_number': '002A',
        'two_storey': True,
        'give_child_without_place': False,
        'user_info': {
            'ip': '1.2.3.4',
            'region_id': 213,
            'phone': '+71234567890',
            'email': 'user@example.org',
            'yandex_uid': yandex_uid
        },
    }


def create_order_info_response(reserved_to, num=1):
    return OrderInfoResult(
        buy_operation_id=None,
        expire_set_er=None,
        status=None,
        order_num=None,
        reserved_to=reserved_to,
        passengers=[
            PassengerInfo(
                blank_id='300',
                doc_id='100500',
                birth_date=datetime(1980, 1, 1),
                customer_id='1' * num,
            ),
            PassengerInfo(
                blank_id='400',
                doc_id='200300',
                birth_date=datetime(1990, 2, 1),
                customer_id='2' * num,
            ),
            PassengerInfo(
                blank_id='300',
                doc_id='400600',
                birth_date=datetime(2016, 1, 1),
                customer_id='3' * num,
            ),
        ],
    )


def create_reserve_response(country, tarrif_info, num=1):
    return ReserveResponse(
        coach_owner='ФПК',
        coach_number='2',
        compartment_gender=GenderChoice.MIXED,
        is_three_hours_reservation_available=True,
        im_order_id=100,
        operation_id='200',
        reserved_to=MSK_TZ.localize(datetime(2016, 12, 1)),
        special_notice='ПРИМЕЧАНИЕ',
        departure_station_title='ВОКЗАЛ ОТКУДА',
        arrival_station_title='СТАНЦИЯ КУДА',
        time_notice='ВРЕМЯ ОТПР И ПРИБ МОСКОВСКОЕ',
        is_only_full_return_possible=True,
        tickets=[
            ReserveResponseTicket(
                index=0,
                customer_id='1' * num,
                amount=Decimal('1000.50'),
                blank_id='300',
                carrier_inn='0123456789',
                passenger_birth_date=datetime(1980, 1, 1),
                passenger_citizenship_country=country,
                passenger_document_number='100500',
                passenger_document_type=DocumentType.RUSSIAN_PASSPORT,
                passenger_first_name='Иван',
                passenger_gender=Gender.MALE,
                passenger_last_name='Иванов',
                passenger_patronymic='Иванович',
                places=['01'],
                places_type=PlacesType.LOWER_TIER,
                tariff_info=tarrif_info,
                raw_tariff_title='Взрослый'
            ),
            ReserveResponseTicket(
                index=1,
                customer_id='2' * num,
                amount=Decimal('1400.50'),
                blank_id='400',
                passenger_birth_date=datetime(1990, 2, 1),
                passenger_citizenship_country=country,
                passenger_document_number='200300',
                passenger_document_type=DocumentType.RUSSIAN_INTERNATIONAL_PASSPORT,
                passenger_first_name='Сидора',
                passenger_gender=Gender.FEMALE,
                passenger_last_name='Сидорова',
                passenger_patronymic='Сидоровна',
                places=['02'],
                places_type=PlacesType.UPPER_TIER,
                raw_tariff_title='Юниор',
            ),
            ReserveResponseTicket(
                index=2,
                customer_id='3' * num,
                amount=Decimal('0'),
                blank_id='300',
                carrier_inn='0123456789',
                passenger_birth_date=datetime(2016, 1, 1),
                passenger_citizenship_country=country,
                passenger_document_number='400600',
                passenger_document_type=DocumentType.BIRTH_CERTIFICATE,
                passenger_first_name='Петр',
                passenger_gender=Gender.MALE,
                passenger_last_name='Петров',
                passenger_patronymic='Петрович',
                tariff_info=tarrif_info,
                raw_tariff_title='Младенец',
            ),
        ]
    )


def create_reserve_response_ticket(tariff_info, country=None, amount=Decimal('1000.50'),
                                   service_price=Decimal(0), service_vat=None):
    return ReserveResponseTicket(
        index=0,
        amount=amount,
        service_price=service_price,
        service_vat=service_vat or create_tax(rate=Decimal(0), amount=Decimal(0)),
        blank_id='300',
        passenger_birth_date=datetime(1980, 1, 1),
        passenger_citizenship_country=country or Country.objects.get(id=Country.RUSSIA_ID),
        passenger_document_number='100500',
        passenger_document_type=DocumentType.RUSSIAN_PASSPORT,
        passenger_first_name='Иван',
        passenger_gender=Gender.MALE,
        passenger_last_name='Иванов',
        passenger_patronymic='Иванович',
        places=['01'],
        places_type=PlacesType.LOWER_TIER,
        tariff_info=tariff_info,
        raw_tariff_title='Взрослый'
    )


@pytest.fixture(autouse=True)
def m_train_route(httpretty):
    mock_im(httpretty, IM_TRAIN_ROUTE_METHOD, responses=IM_TRAIN_ROUTE_RESPONSE)


@pytest.fixture(autouse=True)
def m_delta_fee():
    with replace_dynamic_setting('TRAIN_PURCHASE_EXPERIMENTAL_DELTA_FEE', '0.03'):
        yield


@pytest.mark.parametrize('places, coach_type, requirements, expected', (
    (
        [],
        CoachType.PLATZKARTE,
        None,
        has_properties(
            gender=None,
            storey=None,
            lower_places_quantity=None,
            upper_places_quantity=None,
            max_place_number=None,
            min_place_number=None,
            cabin_place_demands=None,
            additional_place_requirements=None,
        ),
    ),
    (
        [{'number': 1, 'is_upper': True}, {'number': 2, 'is_upper': True}],
        CoachType.PLATZKARTE,
        None,
        has_properties(
            gender=None,
            storey=None,
            lower_places_quantity=0,
            upper_places_quantity=2,
            max_place_number=2,
            min_place_number=1,
            cabin_place_demands=None,
            additional_place_requirements=None,
        ),
    ),
    (
        [{'number': 1, 'is_upper': True}, {'number': 4, 'is_upper': True}],
        CoachType.PLATZKARTE,
        {
            'arrangement': Arrangement.COMPARTMENT,
            'count': {'upper': 1, 'bottom': 1, 'near_passage': None, 'near_window': None},
            'storey': 0
        },
        has_properties(
            gender=None,
            storey=0,
            lower_places_quantity=1,
            upper_places_quantity=1,
            max_place_number=4,
            min_place_number=1,
            cabin_place_demands=Arrangement.COMPARTMENT,
            additional_place_requirements=None,
        ),
    ),
    (
        [{'number': 1, 'is_upper': True}, {'number': 2, 'is_upper': False}],
        CoachType.COMPARTMENT,
        None,
        has_properties(
            gender=None,
            storey=None,
            lower_places_quantity=1,
            upper_places_quantity=1,
            max_place_number=2,
            min_place_number=1,
            cabin_place_demands=None,
            additional_place_requirements=None,
        ),
    ),
    (
        [{'number': 1, 'is_upper': True}, {'number': 10, 'is_upper': True}, {'number': 5, 'is_upper': False}],
        CoachType.SUITE,
        None,
        has_properties(
            gender=None,
            storey=None,
            lower_places_quantity=1,
            upper_places_quantity=2,
            max_place_number=10,
            min_place_number=1,
            cabin_place_demands=None,
            additional_place_requirements=None,
        ),
    ),
    (
        [{'number': 1, 'is_upper': False}, {'number': 2, 'is_upper': True}, {'number': 3, 'is_upper': False}],
        CoachType.SITTING,
        {
            'arrangement': Arrangement.NEAREST,
            'count': {'upper': None, 'bottom': None, 'near_passage': 1, 'near_window': 2},
            'storey': 1
        },
        has_properties(
            gender=None,
            storey=1,
            lower_places_quantity=2,
            upper_places_quantity=1,
            max_place_number=3,
            min_place_number=1,
            cabin_place_demands=Arrangement.NEAREST,
            additional_place_requirements=None,
        ),
    ),
    (
        [{'number': 1, 'is_upper': True}, {'number': 2, 'is_upper': True}, {'number': 3, 'is_upper': False}],
        CoachType.COMMON,
        None,
        has_properties(
            gender=None,
            storey=None,
            lower_places_quantity=None,
            upper_places_quantity=None,
            max_place_number=3,
            min_place_number=1,
            cabin_place_demands=None,
            additional_place_requirements=None,
        ),
    ),
    (
        [{'number': 1, 'is_upper': True}, {'number': 4, 'is_upper': True}],
        CoachType.PLATZKARTE,
        {
            'arrangement': Arrangement.COMPARTMENT,
            'count': None,
            'storey': 0
        },
        has_properties(
            gender=None,
            storey=0,
            lower_places_quantity=None,
            upper_places_quantity=None,
            max_place_number=4,
            min_place_number=1,
            cabin_place_demands=Arrangement.COMPARTMENT,
            additional_place_requirements=None,
        ),
    ),
))
def test_places_info(places, coach_type, requirements, expected):
    places_info = PlacesInfo(
        coach_type=coach_type,
        gender=None,
        number_and_is_upper_for_places=places,
        place_demands=None,
        requirements=requirements,
        additional_place_requirements=None,
    )

    assert_that(places_info, expected)


@pytest.mark.parametrize('additional_place_requirements, place_demands, requirements, places, expected', [
    (
        'Forward',
        None,
        {
            'arrangement': Arrangement.COMPARTMENT,
            'count': {'upper': 1, 'bottom': 1, 'near_passage': None, 'near_window': None},
            'storey': None
        },
        [{'number': 1, 'is_upper': True}, {'number': 4, 'is_upper': True}],
        has_properties(
            gender=None,
            storey=None,
            lower_places_quantity=1,
            upper_places_quantity=1,
            max_place_number=4,
            min_place_number=1,
            cabin_place_demands=Arrangement.COMPARTMENT,
            additional_place_requirements='Forward',
        ),
    ),
    (
        None,
        PlacesOption.WITH_PET,
        {
            'arrangement': Arrangement.COMPARTMENT,
            'count': {'upper': 1, 'bottom': 1, 'near_passage': None, 'near_window': None},
            'storey': None
        },
        [{'number': 1, 'is_upper': True}, {'number': 4, 'is_upper': True}],
        has_properties(
            gender=None,
            storey=None,
            lower_places_quantity=1,
            upper_places_quantity=1,
            max_place_number=4,
            min_place_number=1,
            cabin_place_demands=Arrangement.COMPARTMENT,
            additional_place_requirements=PlacesOption.WITH_PET,
        ),
    ),
    (
        'WithPetsPlaces',
        None,
        None,
        None,
        has_properties(
            gender=None,
            storey=None,
            lower_places_quantity=None,
            upper_places_quantity=None,
            max_place_number=None,
            min_place_number=None,
            cabin_place_demands=None,
            additional_place_requirements='WithPetsPlaces',
        ),
    ),
])
def test_places_info_additional_place_requirements(additional_place_requirements, place_demands, requirements,
                                                   places, expected):
    places_info = PlacesInfo(
        coach_type=None,
        gender=None,
        number_and_is_upper_for_places=places,
        place_demands=place_demands,
        requirements=requirements,
        additional_place_requirements=additional_place_requirements,
    )

    assert_that(places_info, expected)


@pytest.mark.parametrize('gender', (GenderChoice.MALE, GenderChoice.FEMALE, GenderChoice.MIXED))
def test_places_info_gender(gender):
    places_info = PlacesInfo(
        coach_type=CoachType.COMPARTMENT,
        gender=gender,
        number_and_is_upper_for_places=[],
        place_demands=None,
        requirements=None,
        additional_place_requirements=None,
    )

    assert_that(places_info, has_properties(
        gender=gender,
        storey=None,
        lower_places_quantity=None,
        upper_places_quantity=None,
        max_place_number=None,
        min_place_number=None,
        cabin_place_demands=None,
        additional_place_requirements=None,
    ))


def test_places_info_same_numbers():
    with pytest.raises(WrongPlaces):
        PlacesInfo(
            coach_type=CoachType.PLATZKARTE,
            gender=None,
            number_and_is_upper_for_places=[{'number': 1, 'is_upper': True}, {'number': 1, 'is_upper': True}],
            place_demands=None,
            requirements=None,
            additional_place_requirements=None,
        )


@mock.patch.object(order_tickets, 'calculate_ticket_cost', autospec=True, return_value=TicketCost(
    amount_without_fee=Decimal(1000), bedding_amount_without_fee=Decimal(200),
    yandex_fee_percent=Decimal('0.11'), main_fee=Decimal(110), bedding_fee=Decimal(22)
))
def test_make_ticket_payment_platzkarte_with_bedding(m_fee_calculator):
    contract = ClientContractFactory()
    bedding_tariff = Decimal(200)
    payment_amount = Decimal(1000)
    reserved_ticket = ReserveResponseTicket(amount=payment_amount, service_price=bedding_tariff,
                                            service_vat=create_tax(rate=Decimal(18), amount=Decimal(36)))

    assert _make_ticket_payment(
        {'bedding': True, 'bedding_tariff': Decimal(1), 'coach_type': CoachType.PLATZKARTE},
        reserved_ticket, contract
    ) == TicketPayment(
        amount=payment_amount,
        service_amount=Decimal(200),
        service_vat=Tax(rate=Decimal(18), amount=Decimal(36)),
        fee=Decimal(132),
        service_fee=Decimal(22),
        fee_percent=Decimal('0.11'),
        fee_percent_range=Decimal('0.03'),
        partner_fee=contract.partner_commission_sum,
        partner_refund_fee=contract.partner_commission_sum2
    )
    m_fee_calculator.assert_called_once_with(contract, CoachType.PLATZKARTE.value, payment_amount, bedding_tariff,
                                             yandex_uid=None)


@mock.patch.object(order_tickets, 'calculate_ticket_cost', autospec=True, return_value=TicketCost(
    amount_without_fee=Decimal(1000), bedding_amount_without_fee=Decimal(0),
    yandex_fee_percent=Decimal('0.11'), main_fee=Decimal(110), bedding_fee=Decimal(0)
))
def test_make_ticket_payment_platzkarte_without_bedding(m_fee_calculator):
    contract = ClientContractFactory()
    bedding_tariff = Decimal(0)
    payment_amount = Decimal(1000)
    reserved_ticket = ReserveResponseTicket(amount=payment_amount, service_price=bedding_tariff,
                                            service_vat=create_tax(rate=Decimal(0), amount=Decimal(0)))

    assert _make_ticket_payment(
        {'coach_type': CoachType.PLATZKARTE, 'bedding_tariff': Decimal(100500), 'bedding': False},
        reserved_ticket, contract
    ) == TicketPayment(
        amount=payment_amount,
        service_amount=Decimal(0),
        service_vat=Tax(rate=Decimal(0), amount=Decimal(0)),
        fee=Decimal(110),
        service_fee=bedding_tariff,
        fee_percent=Decimal('0.11'),
        fee_percent_range=Decimal('0.03'),
        partner_fee=contract.partner_commission_sum,
        partner_refund_fee=contract.partner_commission_sum2
    )
    m_fee_calculator.assert_called_once_with(contract, CoachType.PLATZKARTE.value, payment_amount, bedding_tariff,
                                             yandex_uid=None)


@mock.patch.object(order_tickets, 'calculate_ticket_cost', autospec=True, return_value=TicketCost(
    amount_without_fee=Decimal(1000), bedding_amount_without_fee=Decimal(200),
    yandex_fee_percent=Decimal('0.11'), main_fee=Decimal(110), bedding_fee=Decimal(0)
))
def test_make_ticket_payment_compartment(m_fee_calculator):
    contract = ClientContractFactory()
    bedding_tariff = Decimal(200)
    payment_amount = Decimal(1000)
    reserved_ticket = ReserveResponseTicket(amount=payment_amount, service_price=bedding_tariff,
                                            service_vat=create_tax(rate=Decimal(18), amount=Decimal(36)))
    ticket_fee = m_fee_calculator.return_value

    assert _make_ticket_payment(
        {'coach_type': CoachType.COMPARTMENT, 'bedding_tariff': bedding_tariff, 'bedding': True},
        reserved_ticket, contract
    ) == TicketPayment(
        amount=payment_amount,
        service_amount=Decimal(200),
        service_vat=Tax(rate=Decimal(18), amount=Decimal(36)),
        fee=ticket_fee.full_fee,
        service_fee=ticket_fee.bedding_fee,
        fee_percent=ticket_fee.yandex_fee_percent,
        fee_percent_range=Decimal('0.03'),
        partner_fee=contract.partner_commission_sum,
        partner_refund_fee=contract.partner_commission_sum2
    )
    m_fee_calculator.assert_called_once_with(contract, CoachType.COMPARTMENT.value, payment_amount, Decimal(200),
                                             yandex_uid=None)


@mock.patch.object(order_tickets, 'calculate_ticket_cost', autospec=True, return_value=TicketCost(
    amount_without_fee=Decimal(0), bedding_amount_without_fee=Decimal(0),
    yandex_fee_percent=Decimal('0.11'), main_fee=Decimal(110), bedding_fee=Decimal(0)
))
def test_make_ticket_payment_free(m_fee_calculator):
    contract = ClientContractFactory()
    payment_amount = Decimal(0)
    reserved_ticket = ReserveResponseTicket(amount=payment_amount)

    assert _make_ticket_payment(
        {'coach_type': CoachType.COMPARTMENT},
        reserved_ticket, contract
    ) == TicketPayment(
        amount=payment_amount,
        service_amount=Decimal(0),
        fee=Decimal(0),
        fee_percent=Decimal(0),
        fee_percent_range=Decimal('0.03'),
        partner_fee=Decimal(0),
        partner_refund_fee=Decimal(0)
    )
    assert not m_fee_calculator.called


def test_reserve_tickets(httpretty, full_tariff_info):
    mock_im(httpretty, CREATE_RESERVATION_METHOD, json=ImCreateReservationFactory(
        OrderId=123,
        Customers=[dict(
            Index=2,
            FirstName='Елена',
            MiddleName='Еленовна',
            LastName='Еленова',
            Sex='Female',
            BirthDate='1991-02-02T00:00:00',
            DocumentNumber='6509999999',
            DocumentType='RussianPassport',
            CitizenshipCode='RU',
            OrderCustomerId=77,
        )],
        ReservationResults=[dict(
            OrderItemId=456,
            Amount=200,
            OriginStation='STATION_FROM',
            DestinationStation='STATION TO',
            Carrier='ББ',
            CarNumber='33',
            ConfirmTill='2000-12-07T12:00:00',
            Passengers=[dict(
                Category='Adult',
                OrderItemBlankId=32,
                OrderCustomerReferenceIndex=2,
                Amount=111,
                PlacesWithType=[{'Number': '138', 'Type': 'NotNearTable'}]
            )],
            Blanks=[dict(
                ServicePrice=50,
                OrderItemBlankId=32,
                TariffInfo={'TariffName': 'ПОЛНЫЙ', 'TariffType': 'Full'},
                TariffType='Full',
                VatRateValues=[
                    {'Rate': 1.0, 'Value': 11.0},
                    {'Rate': 2.0, 'Value': 22.0},
                    {'Rate': 3.0, 'Value': 33.0},
                ],
            )],
        )],
    ))

    reserve_response = reserve_tickets(ReserveOrderDataFactory())

    assert_that(reserve_response, has_properties(
        amount=200,
        departure_station_title='STATION_FROM',
        arrival_station_title='STATION TO',
        coach_owner='ББ',
        coach_number='33',
        compartment_gender=None,
        im_order_id=123,
        operation_id='456',
        reserved_to=MSK_TZ.localize(datetime(2000, 12, 7, 12, 0)),
        special_notice=None,
        time_notice=None,
        is_three_hours_reservation_available=False,
        is_suburban=False,
        tickets=contains(has_properties(
            index=2,
            customer_id='77',
            amount=111,
            service_price=50,
            blank_id='32',
            tariff_vat=has_properties(rate=Decimal('1.0'), amount=Decimal('11.0')),
            service_vat=has_properties(rate=Decimal('2.0'), amount=Decimal('22.0')),
            commission_fee_vat=has_properties(rate=Decimal('3.0'), amount=Decimal('33.0')),
            passenger_birth_date=datetime(1991, 2, 2),
            passenger_citizenship_country=Country.objects.get(code='RU'),
            passenger_document_number='6509999999',
            passenger_document_type=DocumentType.RUSSIAN_PASSPORT,
            passenger_first_name='Елена',
            passenger_gender=Gender.FEMALE,
            passenger_last_name='Еленова',
            passenger_patronymic='Еленовна',
            places=contains('138'),
            places_type=PlacesType.NOT_NEAR_TABLE,
            raw_tariff_title='ПОЛНЫЙ',
            tariff_info=full_tariff_info,
        )),
    ))


@replace_dynamic_setting('TRAIN_PURCHASE_REBOOKING_ENABLED', False)
@pytest.mark.parametrize('remaining_seconds, reserve_request_count, cancel_order_count, rebooking_status', (
    (15, 2, 1, RebookingStatus.DONE),
    (90, 1, 0, RebookingStatus.SKIPPED),
    (0, 2, 0, RebookingStatus.DONE),
))
def test_order_reserve_create_and_rebooking(
        remaining_seconds, reserve_request_count, cancel_order_count, rebooking_status, full_tariff_info
):
    russia = Country.objects.get(id=Country.RUSSIA_ID)
    station_from = create_station(title='Откуда', __={'codes': {'express': '1000'}}, express_id='1000')
    station_to = create_station(title='Куда', __={'codes': {'express': '2000'}}, express_id='2000')
    start_station = create_station(title='Откуда поезд', __={'codes': {'express': '3000'}})
    end_station = create_station(title='Куда поезд', __={'codes': {'express': '4000'}})
    contract = ClientContractFactory(is_active=True)

    order_uid = '1' * 32
    order_data = create_order_data(station_from=station_from, station_to=station_to, country=russia)
    order_data['give_child_without_place'] = True

    with mock.patch.object(
            ReservationManager, 'reserve', autospec=True,
            side_effect=[
                create_reserve_response(country=russia, tarrif_info=full_tariff_info, num=1),
                create_reserve_response(country=russia, tarrif_info=full_tariff_info, num=2),
            ]
    ) as m_reserve_request, mock.patch.object(
        order_tickets, 'get_order_info', autospec=True,
        side_effect=[
            create_order_info_response(reserved_to=now_aware() + timedelta(seconds=remaining_seconds), num=1),
        ]
    ) as m_get_order_info, mock.patch.object(
        order_tickets, 'send_event_to_order', autospec=True
    ) as m_send_event, mock.patch.object(
        order_tickets, 'cancel_order', autospec=True
    ) as m_cancel_order, mock.patch.object(
        TrustClient, 'get_payment_status', autospec=True, return_value=TrustPaymentStatuses.AUTHORIZED
    ) as m_get_payment_status:
        reservation_result = reserve_tickets(order_data)

        order, personal_data = create_order(
            order_uid,
            order_data,
            reservation_result,
            contract
        )

        assert_that(order, has_properties(
            uid=order_uid,
            status=OrderStatus.RESERVED,
            travel_status=TravelOrderStatus.RESERVED,
            passengers=contains(
                has_properties(
                    birth_date_hash=has_length(64),
                    doc_id_hash=has_length(64),

                    first_name='Иван',
                    last_name='Иванов',
                    patronymic='Иванович',
                    sex=Gender.MALE,
                    age=Decimal(37),
                    doc_type=DocumentType.RUSSIAN_PASSPORT,
                    citizenship_country_id=Country.RUSSIA_ID,
                    customer_id='1',
                    phone='79222020001',
                    email='email1@email.com',

                    rebooking_info=has_properties(
                        age_group=AgeGroup.ADULTS,
                        loyalty_cards=contains(has_properties(
                            type=LoyaltyCardType.RZHD_BONUS,
                            number='12345',
                        )),
                        tariff_info=has_properties(
                            im_request_code='Full',
                        ),
                    ),

                    tickets=contains(has_properties(
                        blank_id='300',
                        carrier_inn='0123456789',
                        places=['01'],
                        places_type=PlacesType.LOWER_TIER,
                        payment=anything(),
                        tariff_info_code=full_tariff_info.code,
                        raw_tariff_title='Взрослый'
                    )),
                ),
                has_properties(
                    birth_date_hash=has_length(64),
                    doc_id_hash=has_length(64),

                    first_name='Сидора',
                    last_name='Сидорова',
                    patronymic='Сидоровна',
                    sex=Gender.FEMALE,
                    age=Decimal(26),
                    doc_type=DocumentType.RUSSIAN_INTERNATIONAL_PASSPORT,
                    citizenship_country_id=Country.RUSSIA_ID,
                    customer_id='2',
                    phone='79222020002',
                    email='email2@email.com',

                    rebooking_info=has_properties(
                        age_group=AgeGroup.CHILDREN,
                        loyalty_cards=contains(has_properties(
                            type=LoyaltyCardType.UNIVERSAL,
                            number='54321',
                        )),
                        tariff_info=has_properties(
                            im_request_code='Junior',
                        ),
                    ),

                    tickets=contains(has_properties(
                        blank_id='400',
                        carrier_inn=None,
                        places=['02'],
                        places_type=PlacesType.UPPER_TIER,
                        payment=anything(),
                        tariff_info_code=None,
                        raw_tariff_title='Юниор'
                    )),
                ),
                has_properties(
                    birth_date_hash=has_length(64),
                    doc_id_hash=has_length(64),

                    first_name='Петр',
                    last_name='Петров',
                    patronymic='Петрович',
                    sex=Gender.MALE,
                    age=Decimal(1),
                    doc_type=DocumentType.BIRTH_CERTIFICATE,
                    citizenship_country_id=Country.RUSSIA_ID,
                    customer_id='3',

                    rebooking_info=has_properties(
                        age_group=AgeGroup.BABIES,
                        loyalty_cards=empty(),
                        tariff_info=has_properties(
                            im_request_code='Baby',
                        ),
                    ),

                    tickets=contains(has_properties(
                        blank_id='300',
                        carrier_inn='0123456789',
                        places=[],
                        payment=anything(),
                        tariff_info_code=full_tariff_info.code,
                        raw_tariff_title='Младенец'
                    )),
                ),
            ),
            station_from_id=station_from.id,
            station_to_id=station_to.id,
            coach_owner='ФПК',
            reserved_to=datetime(2016, 11, 30, 21),
            current_partner_data=has_properties(
                compartment_gender=GenderChoice.MIXED,
                is_three_hours_reservation_available=True,
                im_order_id=100,
                operation_id='200',
                special_notice='ПРИМЕЧАНИЕ',
                station_from_title='ВОКЗАЛ ОТКУДА',
                station_to_title='СТАНЦИЯ КУДА',
                time_notice='ВРЕМЯ ОТПР И ПРИБ МОСКОВСКОЕ',
                start_station_title='ОТКУДА ПОЕЗД',
                end_station_title='КУДА ПОЕЗД',
                is_only_full_return_possible=True,
            ),
            arrival=datetime(2017, 1, 2, 1),
            coach_number='2',
            coach_type=CoachType.COMPARTMENT,
            departure=datetime(2017, 1, 1, 10),
            gender=GenderChoice.MIXED,
            partner=TrainPartner.IM,
            price_exp_id='priceExpId',
            route_policy=RoutePolicy.INTERNAL,
            scheme_id=555,
            train_number='001A',
            train_ticket_number='002A',
            two_storey=True,
            user_info=has_properties(
                ip='1.2.3.4', uid=None, region_id=213, is_mobile=False, email='user@example.org', yandex_uid='someUid'
            ),
            use_new_partner_data=True,
            use_new_payments=True,
            use_booking_workflow=True,

            rebooking_info=has_properties(
                enabled=False,
                cycle_until=None,
                bedding=False,
                electronic_registration=True,
                place_demands=PlacesOption.WITH_PET,
                service_class='2М',
                international_service_class='2/4',
                is_cppk=False,
                places=contains(
                    {'number': 1, 'is_upper': True},
                    {'number': 5, 'is_upper': False},
                ),
                requirements=has_properties(
                    arrangement=Arrangement.COMPARTMENT,
                    count={
                        'upper': 1,
                        'bottom': 1,
                    },
                    storey=2,
                ),
                additional_place_requirements='some-strange-requirements',
                give_child_without_place=True,
            ),
            route_info=has_properties(
                from_station=has_properties(
                    id=station_from.id,
                    title='Откуда',
                    settlement_title=None,
                    departure=None,
                ),
                to_station=has_properties(
                    id=station_to.id,
                    title='Куда',
                    settlement_title=None,
                    departure=None,
                ),
                start_station=has_properties(
                    id=start_station.id,
                    title='Откуда поезд',
                    settlement_title=None,
                    departure=datetime(2016, 12, 31, 20, 40),
                ),
                end_station=has_properties(
                    id=end_station.id,
                    title='Куда поезд',
                    settlement_title=None,
                    departure=None,
                ),
            ),
            source=has_properties(
                terminal='someTerminal',
            ),
        ))
        assert order.partner_data_history

        # временное хранение персональных данных
        assert_that(personal_data, contains(
            dict(doc_id='100500', birth_date=datetime(1980, 1, 1)),
            dict(doc_id='200300', birth_date=datetime(1990, 2, 1)),
            dict(doc_id='400600', birth_date=datetime(2016, 1, 1)),
        ))
        order.reload()
        assert_that(order.passengers, contains(
            not_(has_properties(doc_id=anything(), birth_date=anything())),
            not_(has_properties(doc_id=anything(), birth_date=anything())),
            not_(has_properties(doc_id=anything(), birth_date=anything())),
        ))

        # перебронирование
        PaymentFactory(order_uid=order.uid, process={})

        rebooking(order)
        order.reload()

        assert order.rebooking_info.status == rebooking_status
        assert len(order.partner_data_history) == reserve_request_count
        assert len(m_reserve_request.call_args_list) == reserve_request_count

        assert all(args[0][0].partner_credential_id == TrainPartnerCredentialId.IM
                   for args in m_reserve_request.call_args_list)

        if reserve_request_count > 1:
            original_reservation_manager = m_reserve_request.call_args_list[0][0][0]
            original_request_params = original_reservation_manager._build_request_params()
            for call_args in m_reserve_request.call_args_list[1:]:
                current_reservation_manager = call_args[0][0]
                current_request_params = current_reservation_manager._build_request_params()
                assert original_request_params == current_request_params

            assert_that(order.passengers, contains(
                has_properties(
                    customer_id='11',
                ),
                has_properties(
                    customer_id='22',
                ),
                has_properties(
                    customer_id='33',
                ),
            ))

        assert m_cancel_order.call_count == cancel_order_count
        assert m_get_payment_status.call_count == 1
        assert m_get_order_info.call_count == 1
        assert m_send_event.call_count == 1


def test_rebooking_condition():
    order = TrainOrderFactory(rebooking_info=None)
    with pytest.raises(RebookingError):
        rebooking(order)
    assert order.reload().rebooking_info.status == RebookingStatus.FAILED

    order = TrainOrderFactory(rebooking_info={'status': RebookingStatus.IN_PROCESS})
    with pytest.raises(RebookingError):
        rebooking(order)
    assert order.reload().rebooking_info.status == RebookingStatus.FAILED

    order = TrainOrderFactory(status=OrderStatus.DONE)
    with pytest.raises(RebookingError):
        rebooking(order)
    assert order.reload().rebooking_info.status == RebookingStatus.FAILED

    order = TrainOrderFactory()
    order.update(**{'set__rebooking_info__status': RebookingStatus.IN_PROCESS})
    with pytest.raises(RebookingError):
        rebooking(order)
    assert order.reload().rebooking_info.status == RebookingStatus.FAILED

    order = TrainOrderFactory()
    with pytest.raises(RebookingError), mock.patch.object(
        TrustClient, 'get_payment_status', autospec=True, return_value=TrustPaymentStatuses.CANCELED
    ):
        rebooking(order)
    assert order.reload().rebooking_info.status == RebookingStatus.FAILED

    order = TrainOrderFactory()
    order.process = create_process({'state': 'unhandled_exception_state'})
    with pytest.raises(RebookingError):
        rebooking(order)
    assert order.reload().rebooking_info.status == RebookingStatus.FAILED


def test_apply_personal_data():
    order = TrainOrderFactory(passengers=[PassengerFactory(), PassengerFactory()])
    personal_data = [
        dict(doc_id='100500', birth_date=datetime(1980, 1, 1)),
        dict(doc_id='200300', birth_date=datetime(1990, 2, 1))
    ]

    apply_personal_data(order, personal_data)

    assert_that(order.passengers, contains(
        has_properties(doc_id='100500', birth_date=datetime(1980, 1, 1)),
        has_properties(doc_id='200300', birth_date=datetime(1990, 2, 1))
    ))
    order.reload()
    assert_that(order.passengers, contains(
        not_(has_properties(doc_id=anything(), birth_date=anything())),
        not_(has_properties(doc_id=anything(), birth_date=anything()))
    ))


@pytest.mark.parametrize(
    'coach_type, bedding, service_price, service_vat_amount, expected_service_amount',
    [
        (CoachType.PLATZKARTE, True, Decimal(300), Decimal(54), Decimal(300)),
        (CoachType.PLATZKARTE, False, Decimal(0), Decimal(0), Decimal(0)),
        (CoachType.COMPARTMENT, True, Decimal(300), Decimal(54), Decimal(300)),
    ]
)
def test_service_amount_in_create_order(full_tariff_info, coach_type, bedding,
                                        service_price, service_vat_amount, expected_service_amount):
    service_vat_rate = Decimal(18) if service_vat_amount else Decimal(0)
    contract = ClientContractFactory(is_active=True)
    order_uid = '1' * 32
    order_data = create_order_data(
        coach_type=coach_type,
        bedding=bedding,
        bedding_tariff=Decimal(100)
    )
    reservation_result = ReserveResponse(
        reserved_to=MSK_TZ.localize(datetime(2016, 12, 1)),
        tickets=[create_reserve_response_ticket(
            tariff_info=full_tariff_info,
            amount=Decimal('1000.50'),
            service_price=service_price,
            service_vat=create_tax(rate=service_vat_rate, amount=service_vat_amount)
        )],
        coach_number='2',
    )

    order, personal_data = create_order(order_uid, order_data, reservation_result, contract)

    assert_that(order, has_properties(
        uid=order_uid,
        passengers=contains(has_properties(tickets=contains(has_properties(payment=has_properties(
            amount=Decimal('1000.50'),
            service_amount=expected_service_amount,
            service_vat=has_properties(rate=service_vat_rate, amount=service_vat_amount)
        )))))
    ))


@replace_now('2019-05-01')
@replace_dynamic_setting('TRAIN_PURCHASE_REBOOKING_ENABLED', True)
@replace_dynamic_setting('TRAIN_PURCHASE_RESERVATION_PARTNER_TIMEOUT', 14)
@replace_dynamic_setting('TRAIN_PURCHASE_RESERVATION_MAX_CYCLES', 2)
@pytest.mark.parametrize('three_hours_available, prolong_reservation, expected_reserved_to, expected_cycle_until', [
    (True, 20, datetime(2019, 5, 1), None),
    (False, 20, datetime(2019, 4, 30, 21, 20), datetime(2019, 4, 30, 21, 14)),
    (False, 30, datetime(2019, 4, 30, 21, 28), datetime(2019, 4, 30, 21, 14)),
])
def test_reserved_to_in_create_order(full_tariff_info, three_hours_available, prolong_reservation, expected_reserved_to,
                                     expected_cycle_until):
    contract = ClientContractFactory(is_active=True)
    order_uid = '1' * 32
    order_data = create_order_data()
    order_data['enable_rebooking'] = True
    reservation_result = ReserveResponse(
        is_three_hours_reservation_available=three_hours_available,
        reserved_to=UTC_TZ.localize(datetime(2019, 5, 1)),
        tickets=[create_reserve_response_ticket(full_tariff_info)],
        coach_number='02',
    )

    with replace_dynamic_setting('TRAIN_PURCHASE_PROLONG_RESERVATION_MINUTES', prolong_reservation):
        order, _ = create_order(order_uid, order_data, reservation_result, contract)

    assert order.reserved_to == expected_reserved_to
    assert order.rebooking_info.enabled
    assert order.rebooking_info.cycle_until == expected_cycle_until


@pytest.mark.parametrize('enable_rebooking, setting_rebooking_enabled, expected_state', [
    (True, False, False),
    (False, True, False),
    (True, True, True),
])
def test_rebooking_enabled_in_create_order(full_tariff_info, enable_rebooking, setting_rebooking_enabled,
                                           expected_state):
    contract = ClientContractFactory(is_active=True)
    order_uid = '1' * 32
    order_data = create_order_data()
    order_data['enable_rebooking'] = enable_rebooking
    reservation_result = ReserveResponse(
        is_three_hours_reservation_available=False,
        reserved_to=UTC_TZ.localize(datetime(2019, 5, 1)),
        tickets=[create_reserve_response_ticket(full_tariff_info)],
        coach_number='02',
    )

    with replace_dynamic_setting('TRAIN_PURCHASE_REBOOKING_ENABLED', setting_rebooking_enabled):
        order, _ = create_order(order_uid, order_data, reservation_result, contract)

    assert order.rebooking_info.enabled == expected_state
    assert (order.rebooking_info.cycle_until is not None) == expected_state


@pytest.mark.parametrize(
    'yandex_uid, delta, price_exp_id, expected_fee, expected_fee_percent', [
        ('some_uid', '0.03', 'some_id', Decimal('121.56'), Decimal('0.1215')),
        (None, '0.03', 'some_id', Decimal('110.06'), Decimal('0.11')),
        ('some_uid', '0', 'some_id', Decimal('110.06'), Decimal('0.11')),
        ('some_uid', '0.03', 'some_id', Decimal('121.56'), Decimal('0.1215')),
        ('some_uid', '0.03', None, Decimal('110.06'), Decimal('0.11')),
    ]
)
def test_fee_in_create_order_with_experiment_delta(full_tariff_info, yandex_uid, delta, price_exp_id, expected_fee,
                                                   expected_fee_percent):
    contract = ClientContractFactory(is_active=True)
    order_uid = '1' * 32
    order_data = create_order_data(price_exp_id=price_exp_id, yandex_uid=yandex_uid)
    reservation_result = ReserveResponse(reserved_to=MSK_TZ.localize(datetime(2016, 12, 1)),
                                         tickets=[create_reserve_response_ticket(full_tariff_info)],
                                         coach_number='2')

    with replace_dynamic_setting('TRAIN_PURCHASE_EXPERIMENTAL_DELTA_FEE', delta):
        order, personal_data = create_order(order_uid, order_data, reservation_result, contract)

    assert_that(order, has_properties(
        uid=order_uid,
        passengers=contains(has_properties(
            tickets=contains(has_properties(
                payment=has_properties(
                    fee=expected_fee,
                    fee_percent=expected_fee_percent,
                    fee_percent_range=Decimal(delta),
                )))))
    ))


def test_email_field():
    order = TrainOrderFactory()
    order.user_info = UserInfo(email='user-98@mail.ru', ip='1', region_id=1)
    order.save()
    order.reload()
    assert order.user_info.email == 'user-98@mail.ru'

    order.user_info = UserInfo(email='юзер69@рф.рф.рф', ip='1', region_id=1)
    order.save()
    order.reload()
    assert order.user_info.email == 'юзер69@рф.рф.рф'


def test_route_info():
    russia = Country.objects.get(id=Country.RUSSIA_ID)
    station_from = create_station(title='Откуда', __={'codes': {'express': '1000'}},
                                  settlement=create_settlement(title='Город Откуда'))
    station_to = create_station(title='Куда', __={'codes': {'express': '2000'}},
                                settlement=create_settlement(title='Город Куда'))
    start_station = create_station(title='Откуда поезд', __={'codes': {'express': '3000'}},
                                   settlement=create_settlement(title='Город Откуда поезд'))
    end_station = create_station(title='Куда поезд', __={'codes': {'express': '4000'}},
                                 settlement=create_settlement(title='Город Куда поезд'))
    order_data = create_order_data(station_from=station_from, station_to=station_to, country=russia)
    partner_data = PartnerData()
    order_data = _fill_points_ids_and_names(order_data, partner_data)

    assert_that(order_data, has_entries(
        'route_info', has_properties(
            from_station=has_properties(id=station_from.id, title='Откуда', settlement_title='Город Откуда'),
            to_station=has_properties(id=station_to.id, title='Куда', settlement_title='Город Куда'),
            start_station=has_properties(id=start_station.id, title='Откуда поезд',
                                         settlement_title='Город Откуда поезд'),
            end_station=has_properties(id=end_station.id, title='Куда поезд', settlement_title='Город Куда поезд'),
        ),
    ))
    assert_that(partner_data, has_properties(start_station_title='ОТКУДА ПОЕЗД', end_station_title='КУДА ПОЕЗД'))


def test_route_info_with_missing_station():
    russia = Country.objects.get(id=Country.RUSSIA_ID)
    station_from = create_station(title='Откуда', __={'codes': {'express': '1000'}},
                                  settlement=create_settlement(title='Город Откуда'))
    station_to = create_station(title='Куда', __={'codes': {'express': '2000'}},
                                settlement=create_settlement(title='Город Куда'))
    start_station = create_station(title='Откуда поезд', __={'codes': {'express': '3000'}},
                                   settlement=create_settlement(title='Город Откуда поезд'))
    create_station(title='Куда поезд', __={'codes': {'express': '5000'}},
                   settlement=create_settlement(title='Город Куда поезд'))
    order_data = create_order_data(station_from=station_from, station_to=station_to, country=russia)
    partner_data = PartnerData()
    order_data = _fill_points_ids_and_names(order_data, partner_data)

    assert_that(order_data, has_entries(
        'route_info', has_properties(
            from_station=has_properties(id=station_from.id, title='Откуда', settlement_title='Город Откуда'),
            to_station=has_properties(id=station_to.id, title='Куда', settlement_title='Город Куда'),
            start_station=has_properties(id=start_station.id, title='Откуда поезд',
                                         settlement_title='Город Откуда поезд'),
            end_station=None,
        ),
    ))
    assert_that(partner_data, has_properties(start_station_title='ОТКУДА ПОЕЗД', end_station_title='КУДА ПОЕЗД'))


def test_missing_route_info(httpretty):
    mock_im(httpretty, IM_TRAIN_ROUTE_METHOD)

    russia = Country.objects.get(id=Country.RUSSIA_ID)
    station_from = create_station(title='Откуда', __={'codes': {'express': '1000'}},
                                  settlement=create_settlement(title='Город Откуда'))
    station_to = create_station(title='Куда', __={'codes': {'express': '2000'}},
                                settlement=create_settlement(title='Город Куда'))
    create_station(title='Откуда поезд', __={'codes': {'express': '3000'}},
                   settlement=create_settlement(title='Город Откуда поезд'))
    create_station(title='Куда поезд', __={'codes': {'express': '4000'}},
                   settlement=create_settlement(title='Город Куда поезд'))
    order_data = create_order_data(station_from=station_from, station_to=station_to, country=russia)
    partner_data = PartnerData()
    order_data = _fill_points_ids_and_names(order_data, partner_data)

    assert_that(order_data, has_entries(
        'route_info', has_properties(
            from_station=has_properties(id=station_from.id, title='Откуда', settlement_title='Город Откуда'),
            to_station=has_properties(id=station_to.id, title='Куда', settlement_title='Город Куда'),
            start_station=None,
            end_station=None,
        ),
    ))
    assert_that(partner_data, has_properties(start_station_title=None, end_station_title=None))
