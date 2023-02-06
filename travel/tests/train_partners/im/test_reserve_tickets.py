# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import decimal
from datetime import date, datetime

import mock
import pytest
from hamcrest import assert_that, contains, has_entries, has_properties, anything

from common.apps.train.models import TariffInfo
from common.apps.train_order.enums import CoachType
from common.models.geo import Country
from common.tester.factories import create_station
from common.tester.utils.replace_setting import replace_dynamic_setting
from common.tester.matchers import has_json
from common.utils.date import MSK_TZ
from travel.rasp.train_api.train_partners.base import Tax
from travel.rasp.train_api.train_partners.base.reserve_tickets import ReserveResponse
from travel.rasp.train_api.train_partners.im import base as im_base
from travel.rasp.train_api.train_partners.im.base import ImError
from travel.rasp.train_api.train_partners.im.factories.create_reservation import ImCreateReservationFactory
from travel.rasp.train_api.train_partners.im.factories.utils import mock_im
from travel.rasp.train_api.train_partners.im.reserve_tickets import (
    CREATE_RESERVATION_METHOD, ReservationManager, _get_notice_parts
)
from travel.rasp.train_api.train_purchase.core.enums import (
    AgeGroup, DocumentType, Gender, GenderChoice, LoyaltyCardType, PlacesOption, PlacesType, Arrangement
)
from travel.rasp.train_api.train_purchase.factories import ReserveOrderDataFactory

pytestmark = pytest.mark.dbuser('module')


@pytest.yield_fixture
def m_parse_response():
    with mock.patch.object(ReservationManager, '_parse_response') as m_parse_response:
        yield m_parse_response


def create_reservation_manager(**kwargs):
    order_data = ReserveOrderDataFactory(**kwargs)
    return ReservationManager.from_order_data(order_data)


@pytest.mark.parametrize('create_reservation_manager_func, expected', (
    (
        lambda: create_reservation_manager(
            station_from=create_station(__={'codes': {'express': '200300'}}),
            station_to=create_station(__={'codes': {'express': '100500'}}),
            bedding=True,
            coach_type=CoachType.COMPARTMENT,
            coach_number='10',
            electronic_registration=True,
            service_class='3У',
            train_number='076Э',
            places=[dict(number=1, is_upper=False)],
            requirements=None,
        ),
        has_entries({
            'Customers': contains(anything()),
            'ReservationItems': contains(has_entries({
                '$type': 'ApiContracts.Railway.V1.Messages.Reservation.RailwayReservationRequest, ApiContracts',
                'Index': 0,

                # поезд
                'OriginCode': '200300',
                'DestinationCode': '100500',
                'TrainNumber': '076Э',

                # требования к вагону и местам
                'CarNumber': '10',
                'CarType': 'Compartment',
                'LowerPlaceQuantity': 1,
                'UpperPlaceQuantity': 0,
                'Bedding': True,
                'PlaceRange': has_entries(From=1, To=1),
                'ServiceClass': '3У',
                'InternationalServiceClass': '1/2',
                'CabinGenderKind': 'NoValue',
                'CabinPlaceDemands': 'NoValue',
                'AdditionalPlaceRequirements': 'NoValue',
                'CarStorey': 'NoValue',

                # регистрация и оплата
                'SetElectronicRegistration': True,
                'ProviderPaymentForm': 'Card',

                # пассажиры
                'Passengers': contains(anything()),
            }))
        })
    ),
    (
        lambda: create_reservation_manager(coach_type=CoachType.PLATZKARTE),
        has_entries('ReservationItems', contains(has_entries('CarType', 'ReservedSeat')))
    ),
    (
        lambda: create_reservation_manager(places=[], requirements__arrangement=Arrangement.COMPARTMENT),
        has_entries('ReservationItems', contains(has_entries('CabinPlaceDemands', 'InOneCabin')))
    ),
    (
        lambda: create_reservation_manager(places=[], requirements__arrangement=Arrangement.COMPARTMENT.value),
        has_entries('ReservationItems', contains(has_entries('CabinPlaceDemands', 'InOneCabin')))
    ),
    (
        lambda: create_reservation_manager(places=[], requirements__storey=2),
        has_entries('ReservationItems', contains(has_entries('PlaceRange', None, 'CarStorey', 'Second')))
    ),
    (
        lambda: create_reservation_manager(places=[{'number': 1}]),
        has_entries('ReservationItems', contains(has_entries('PlaceRange', {'From': 1, 'To': 1})))
    ),
    (
        lambda: create_reservation_manager(places=[{'number': 1}, {'number': 5}], requirements__storey=1),
        has_entries('ReservationItems', contains(has_entries('PlaceRange', {'From': 1, 'To': 5}, 'CarStorey', 'First')))
    ),
    (
        lambda: create_reservation_manager(gender=GenderChoice.MIXED),
        has_entries('ReservationItems', contains(has_entries('CabinGenderKind', 'Mixed')))
    ),
    (
        lambda: create_reservation_manager(additional_place_requirements='some-strange-requirements'),
        has_entries('ReservationItems', contains(has_entries('AdditionalPlaceRequirements', 'NoValue')))
    ),
    (
        lambda: create_reservation_manager(additional_place_requirements=PlacesOption.WITH_PET),
        has_entries('ReservationItems', contains(has_entries('AdditionalPlaceRequirements', 'WithPetsPlaces')))
    ),
    (
        lambda: create_reservation_manager(additional_place_requirements='WithPetsPlaces'),
        has_entries('ReservationItems', contains(has_entries('AdditionalPlaceRequirements', 'WithPetsPlaces')))
    ),
    (
        lambda: create_reservation_manager(give_child_without_place=True),
        has_entries('ReservationItems', contains(has_entries('GiveAdditionalTariffForChildIfPossible', True)))
    ),
    (
        lambda: create_reservation_manager(passengers=(
            {},
            dict(
                birth_date=date(1990, 1, 1),
                doc_id='200300',
                doc_type=DocumentType.RUSSIAN_INTERNATIONAL_PASSPORT,
                first_name='Елена',
                sex=Gender.FEMALE,
                last_name='Еленова',
                patronymic='Еленовна',
            )
        )),
        has_entries({
            'Customers': contains(
                anything(),
                has_entries({
                    '$type': 'ApiContracts.Order.V1.Reservation.OrderFullCustomerRequest, ApiContracts',
                    'Index': 1,
                    'DocumentNumber': '200300',
                    'FirstName': 'Елена',
                    'MiddleName': 'Еленовна',
                    'LastName': 'Еленова',
                    'DocumentType': 'RussianForeignPassport',
                    'CitizenshipCode': 'RU',
                    'Sex': 'Female',
                    'Birthday': '1990-01-01T00:00:00',
                })
            ),
            'ReservationItems': contains(has_entries('Passengers', contains(
                has_entries({
                    'OrderCustomerIndex': 0,
                    'Category': 'Adult',
                    'PreferredAdultTariffType': 'Full',
                    'RailwayBonusCards': None
                }),
                has_entries({
                    'OrderCustomerIndex': 1,
                    'Category': 'Adult',
                    'PreferredAdultTariffType': 'Full',
                    'RailwayBonusCards': None
                })
            )))
        }),
    ),
    (
        lambda: create_reservation_manager(is_cppk=True),
        has_entries('ReservationItems', contains(
            has_entries({
                'GiveAdditionalTariffForChildIfPossible': False,
                'OnRequestMeal': False,
                'Bedding': False,
                'CabinGenderKind': 'NoValue',
                'CabinPlaceDemands': 'NoValue',
                'ClientCharge': None,
                'InternationalServiceClass': None,
                'SpecialPlacesDemand': 'NoValue',
                'CarStorey': 'NoValue',
                'AdditionalPlaceRequirements': 'NoValue',
                'Passengers': contains(
                    has_entries({
                        'Category': 'Adult',
                        'PreferredAdultTariffType': 'Full',
                        'IsNonRefundableTariff': False,
                        'IsInvalid': False,
                        'DisabledPersonId': None,
                        'TransitDocument': 'NoValue',
                        'TransportationRequirement': None,
                        'RailwayBonusCards': None,
                    }),
                ),
            }),
        )),
    ),
))
def test_build_request_params(httpretty, m_parse_response, create_reservation_manager_func, expected):
    mock_im(httpretty, CREATE_RESERVATION_METHOD, json={})
    create_reservation_manager_func().reserve()

    assert_that(httpretty.last_request.body, has_json(expected))


@pytest.mark.parametrize('passenger_data, expected', (
    (dict(age_group=AgeGroup.CHILDREN), has_entries('Category', 'Child')),

    (dict(tariff_info=mock.Mock(im_request_code='Junior')), has_entries('PreferredAdultTariffType', 'Junior')),

    (dict(loyalty_cards=[]), has_entries({'RailwayBonusCards': None})),
    (dict(loyalty_cards=[dict(type=LoyaltyCardType.RZHD_BONUS, number='1234')]),
     has_entries({'RailwayBonusCards': [{'CardType': 'RzhdBonus', 'CardNumber': '1234'}]})),
    (dict(loyalty_cards=[dict(type=LoyaltyCardType.RZHD_BONUS, number='1234'),
                         dict(type=LoyaltyCardType.UNIVERSAL, number='5678')]),
     has_entries({'RailwayBonusCards': [{'CardType': 'RzhdBonus', 'CardNumber': '1234'},
                                        {'CardType': 'UniversalRzhdCard', 'CardNumber': '5678'}]})),

))
def test_build_request_params_passengers(httpretty, m_parse_response, passenger_data, expected):
    mock_im(httpretty, CREATE_RESERVATION_METHOD, json={})
    create_reservation_manager(passengers=[passenger_data]).reserve()

    assert_that(httpretty.last_request.body,
                has_json(has_entries('ReservationItems', contains(has_entries('Passengers', contains(expected))))))


def test_parse_response(httpretty):
    mock_im(httpretty, CREATE_RESERVATION_METHOD, body='''{
        "OrderId": 1,
        "Customers": [],
        "ReservationResults": [{
            "OrderItemId": 1,
            "Amount": 123.45,
            "Blanks": [],
            "OriginStation": "МОСКВА",
            "DestinationStation": "САНКТ-ПЕТЕРБУРГ",
            "Carrier": "ФПК",
            "CarNumber": "2",
            "TimeDescription": null,
            "ConfirmTill": "2000-01-01T12:00:00",
            "Passengers": [],
            "IsThreeHoursReservationAvailable": true,
            "Blanks": [],
            "IsSuburban": true,
            "IsOnlyFullReturnPossible": true
        }]
    }''')

    assert create_reservation_manager(gender=GenderChoice.MIXED).reserve() == ReserveResponse(
        im_order_id=1,
        operation_id='1',
        amount=decimal.Decimal('123.45'),
        departure_station_title='МОСКВА',
        arrival_station_title='САНКТ-ПЕТЕРБУРГ',
        coach_owner='ФПК',
        coach_number='2',
        reserved_to=MSK_TZ.localize(datetime(2000, 1, 1, 12)),
        is_three_hours_reservation_available=True,
        compartment_gender=GenderChoice.MIXED,
        is_suburban=True,
        is_only_full_return_possible=True,
    )


def test_parse_special_notice(httpretty):
    mock_im(httpretty, CREATE_RESERVATION_METHOD, json={
        'OrderId': 1,
        'Customers': [],
        'ReservationResults': [{
            'OrderItemId': 1,
            'Amount': 123.45,
            'Blanks': [],
            'OriginStation': 'МОСКВА',
            'DestinationStation': 'САНКТ-ПЕТЕРБУРГ',
            'Carrier': 'ФПК',
            "CarNumber": "2",
            'TimeDescription': 'СТАНЦИИ СНГ.. ВРЕМЯ ОТПР МОСКОВСКОЕ',
            'ConfirmTill': '2000-01-01T12:00:00',
            'Passengers': [],
        }]
    })

    assert_that(create_reservation_manager().reserve(),
                has_properties(special_notice='СТАНЦИИ СНГ',
                               time_notice='ВРЕМЯ ОТПР МОСКОВСКОЕ'))


@mock.patch.object(im_base.log, 'info')
def test_parse_response_tickets(m_info, httpretty, full_tariff_info):
    mock_im(httpretty, CREATE_RESERVATION_METHOD, json={
        'OrderId': 1,
        'Customers': [{
            'Index': 1,
            'FirstName': 'Елена',
            'MiddleName': 'Еленовна',
            'LastName': 'Еленова',
            'Sex': 'Female',
            'BirthDate': '1990-01-01T00:00:00',
            'DocumentNumber': '200300',
            'DocumentType': 'DiplomaticPassport',
            'CitizenshipCode': 'RU',
            'OrderCustomerId': 666,
        }, {
            'Index': 0,
            'FirstName': 'Иван',
            'MiddleName': 'Иванович',
            'LastName': 'Иванов',
            'Sex': 'NoValue',
            'BirthDate': '1980-01-01T00:00:00',
            'DocumentNumber': '100500',
            'DocumentType': 'RussianPassport',
            'CitizenshipCode': 'RU',
            'OrderCustomerId': 555,
        }, {
            'Index': 2,
            'FirstName': 'Лялька',
            'MiddleName': 'Ивановна',
            'LastName': 'Иванова',
            'Sex': 'Male',
            'BirthDate': '1999-01-01T00:00:00',
            'DocumentNumber': '100500',
            'DocumentType': 'BirthCertificate',
            'CitizenshipCode': 'RU',
            'OrderCustomerId': 777,
        }],
        'ReservationResults': [{
            'OrderItemId': 1,
            'Amount': 123.45,
            'Blanks': [{
                'AdditionalPrice': 755.6,
                'Amount': 1000,
                'ServicePrice': 100,
                'BaseFare': 868.4,
                'FareInfo': {'CarrierTin': 'ИНН ПЕРЕВОЗЧИКА'},
                'OrderItemBlankId': 123,
                'TariffInfo': {'TariffName': 'ПОЛНЫЙ', 'TariffType': 'Full'},
                'TariffType': 'Full',
                'VatRateValues': [
                    {'Rate': 1.0, 'Value': 1.0},
                    {'Rate': 2.0, 'Value': 2.0},
                    {'Rate': 3.0, 'Value': 3.0},
                ]
            }, {
                'AdditionalPrice': 1755.6,
                'Amount': 2000,
                'ServicePrice': 100,
                'BaseFare': 1868.4,
                'OrderItemBlankId': 456,
                'TariffInfo': None,
                'TariffType': 'Junior',
                'VatRateValues': [
                    {'Rate': 4.0, 'Value': 4.0},
                    {'Rate': 5.0, 'Value': 5.0}
                ]
            }],
            'OriginStation': 'МОСКВА',
            'DestinationStation': 'САНКТ-ПЕТЕРБУРГ',
            'Carrier': 'ФПК',
            "CarNumber": "2",
            'TimeDescription': None,
            'ConfirmTill': '2000-01-01T12:00:00',
            'Passengers': [{
                'Category': 'Adult',
                'PlacesWithType': [],
                'Amount': 100,
                'OrderItemBlankId': 123,
                'OrderCustomerReferenceIndex': 0
            }, {
                'Category': 'Adult',
                'PlacesWithType': [{
                    'Number': '1',
                    'Type': 'Upper'
                }],
                'Amount': 200,
                'OrderItemBlankId': 456,
                'OrderCustomerReferenceIndex': 1
            }, {
                'Category': 'BabyWithoutPlace',
                'PlacesWithType': [],
                'Amount': 0,
                'OrderItemBlankId': 123,
                'OrderCustomerReferenceIndex': 2
            }]
        }]
    })
    russia = Country.objects.get(id=Country.RUSSIA_ID)
    junior_tariff_info = TariffInfo.objects.create(code='junior', title_ru='Юниор', im_response_codes='Junior')
    baby_tariff_info = TariffInfo.objects.create(code=TariffInfo.BABY_CODE, title_ru='Детский без места',
                                                 im_response_codes='FreeChild')

    assert_that(create_reservation_manager().reserve(), has_properties(tickets=contains(
        has_properties(
            amount=decimal.Decimal('100'),
            service_price=decimal.Decimal('100'),
            blank_id='123',
            customer_id='555',
            passenger_birth_date=datetime(1980, 1, 1),
            passenger_citizenship_country=russia,
            passenger_document_number='100500',
            passenger_document_type=DocumentType.RUSSIAN_PASSPORT,
            passenger_first_name='Иван',
            passenger_gender=None,
            passenger_last_name='Иванов',
            passenger_patronymic='Иванович',
            places=[],
            places_type=None,
            raw_tariff_title='ПОЛНЫЙ',
            tariff_info=full_tariff_info,
            tariff_vat=Tax(amount=1, rate=1),
            service_vat=Tax(amount=2, rate=2),
            commission_fee_vat=Tax(amount=3, rate=3),
            carrier_inn='ИНН ПЕРЕВОЗЧИКА',
        ),
        has_properties(
            amount=decimal.Decimal('200'),
            service_price=decimal.Decimal('100'),
            blank_id='456',
            customer_id='666',
            passenger_birth_date=datetime(1990, 1, 1),
            passenger_citizenship_country=russia,
            passenger_document_number='200300',
            passenger_document_type=None,
            passenger_first_name='Елена',
            passenger_gender=Gender.FEMALE,
            passenger_last_name='Еленова',
            passenger_patronymic='Еленовна',
            places=['1'],
            places_type=PlacesType.UPPER_TIER,
            raw_tariff_title=None,
            tariff_info=junior_tariff_info,
            tariff_vat=Tax(amount=4, rate=4),
            service_vat=Tax(amount=5, rate=5),
            commission_fee_vat=None,
            carrier_inn=None,
        ),
        has_properties(
            amount=decimal.Decimal(0),
            blank_id='123',
            customer_id='777',
            passenger_birth_date=datetime(1999, 1, 1),
            passenger_citizenship_country=russia,
            passenger_document_number='100500',
            passenger_document_type=DocumentType.BIRTH_CERTIFICATE,
            passenger_first_name='Лялька',
            passenger_gender=Gender.MALE,
            passenger_last_name='Иванова',
            passenger_patronymic='Ивановна',
            places=[],
            places_type=None,
            raw_tariff_title='ПОЛНЫЙ',
            tariff_info=baby_tariff_info,
            tariff_vat=None,
            service_vat=None,
            commission_fee_vat=None,
            carrier_inn='ИНН ПЕРЕВОЗЧИКА',
        ),
    )))
    for log_info_args in m_info.call_args_list:
        msg = '\n'.join(log_info_args[0])
        for secret_data in ['200300', '100500', '1999-01-01', '1980-01-01', '1990-01-01']:
            assert secret_data not in msg


@pytest.mark.parametrize('raw_notice, expected_notice_parts', [
    ('СТАНЦИИ СНГ..  ВРЕМЯ ОТПР МОСКОВСКОЕ', ['СТАНЦИИ СНГ', 'ВРЕМЯ ОТПР МОСКОВСКОЕ']),
    ('. ВРЕМЯ ОТПР И ПРИБ МОСКОВСКОЕ. ..  .', ['ВРЕМЯ ОТПР И ПРИБ МОСКОВСКОЕ']),
    ('. ВОЗВРАТ ТОЛЬКО В КАССАХ АГЕНТА "ТВЕРСКОЙ ЭКСПРЕСС" ТЕЛ.8-800-7777-020',
     ['ВОЗВРАТ ТОЛЬКО В КАССАХ АГЕНТА "ТВЕРСКОЙ ЭКСПРЕСС" ТЕЛ', '8-800-7777-020'])
])
def test_get_notice_parts(raw_notice, expected_notice_parts):
    assert _get_notice_parts(raw_notice) == expected_notice_parts


@pytest.mark.parametrize('train_purchase_transmit_phone_email_to_im, expected', [
    (
        True,
        has_entries({
            'Phone': '79222020123',
            'ContactEmailOrPhone': 'email@email.com',
            'IsMarketingNewsletterAllowed': False,
        }),
    ),
    (
        False,
        has_entries({
            'Phone': None,
            'ContactEmailOrPhone': None,
            'IsMarketingNewsletterAllowed': False,
        }),
    ),
])
def test_request_with_phone_and_email(httpretty, m_parse_response, train_purchase_transmit_phone_email_to_im, expected):
    mock_im(httpretty, CREATE_RESERVATION_METHOD, json={})
    with replace_dynamic_setting('TRAIN_PURCHASE_TRANSMIT_PHONE_EMAIL_TO_IM',
                                 train_purchase_transmit_phone_email_to_im):
        create_reservation_manager().reserve()

    assert_that(httpretty.last_request.body, has_json(
        has_entries(
            'ReservationItems',
            contains(has_entries('Passengers', contains(expected))),
        )
    ))


@replace_dynamic_setting('TRAIN_PURCHASE_TRANSMIT_PHONE_EMAIL_TO_IM', True)
@pytest.mark.parametrize('im_error, expected', [
    (
        '{"Code": 1385, "Message": "Invalid phone", "MessageParams": []}',
        has_entries({
            'Phone': None,
            'ContactEmailOrPhone': 'email@email.com',
        }),
    ),
    (
        '{"Code": 1386, "Message": "Invalid email", "MessageParams": []}',
        has_entries({
            'Phone': '79222020123',
            'ContactEmailOrPhone': None,
        }),
    ),
])
def test_request_with_invalid_phone_or_email(httpretty, m_parse_response, im_error, expected):
    responses = [
        httpretty.Response(im_error, status=500),
        httpretty.Response('{}', status=200),
    ]
    mock_im(httpretty, CREATE_RESERVATION_METHOD, responses=responses)
    create_reservation_manager().reserve()

    assert len(httpretty.latest_requests) == 2
    assert_that(httpretty.last_request.body, has_json(
        has_entries(
            'ReservationItems',
            contains(has_entries('Passengers', contains(expected)))
        )
    ))


@replace_dynamic_setting('TRAIN_PURCHASE_TRANSMIT_PHONE_EMAIL_TO_IM', True)
def test_request_with_usual_raise(httpretty, m_parse_response):
    responses = [
        httpretty.Response('{"Code": 666, "Message": "Unknown error", "MessageParams": []}', status=500),
    ]
    mock_im(httpretty, CREATE_RESERVATION_METHOD, responses=responses)
    with pytest.raises(ImError):
        create_reservation_manager().reserve()


@replace_dynamic_setting('TRAIN_PURCHASE_TRANSMIT_PHONE_EMAIL_TO_IM', False)
@pytest.mark.parametrize('im_error', [
    (
        '{"Code": 1385, "Message": "Invalid phone", "MessageParams": []}',
    ),
    (
        '{"Code": 1386, "Message": "Invalid email", "MessageParams": []}',
    ),
])
def test_request_with_invalid_phone_or_email_without_retry(httpretty, m_parse_response, im_error):
    responses = [
        httpretty.Response(im_error, status=500),
        httpretty.Response('{}', status=200),
    ]
    mock_im(httpretty, CREATE_RESERVATION_METHOD, responses=responses)
    with pytest.raises(ImError):
        create_reservation_manager().reserve()


@replace_dynamic_setting('TRAIN_PURCHASE_TRANSMIT_PHONE_EMAIL_TO_IM', True)
def test_parse_response_with_retries(httpretty):
    mock_im(httpretty, CREATE_RESERVATION_METHOD, json=ImCreateReservationFactory())

    result = create_reservation_manager().reserve()

    assert_that(result, has_properties(
        operation_id='1',
    ))
