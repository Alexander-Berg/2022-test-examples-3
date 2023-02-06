# coding: utf-8
from __future__ import absolute_import, division, print_function, unicode_literals

from copy import deepcopy
from datetime import datetime
from decimal import Decimal

import mock
import pytest
from hamcrest import (
    anything, assert_that, contains, contains_inanyorder, empty, has_entries, has_properties, none, not_
)

from common.apps.train.models import TariffInfo
from common.apps.train.tariff_error import TariffError
from common.tester.factories import create_station
from common.tester.utils.replace_setting import replace_dynamic_setting, replace_setting
from common.utils.date import MSK_TZ, smart_localize
from travel.proto import commons_pb2
from travel.rasp.train_api.train_bandit_api.client import api_pb2, api_pb2_grpc
from travel.rasp.train_api.train_partners.base.train_details import TrainDetailsQuery, parsers as base_parsers
from travel.rasp.train_api.train_partners.im.factories.utils import mock_im
from travel.rasp.train_api.train_partners.im.train_details import parsers as im_parsers, receiver
from travel.rasp.train_api.train_partners.im.train_details.receiver import IM_TRAIN_DETAILS_METHOD
from travel.rasp.train_api.train_purchase.core.enums import RoutePolicy, TrainPartner

pytestmark = [pytest.mark.dbuser]

TRAIN_WIZARD_API_INDEXER_HOST = 'train-wizard-api.net'

IM_CAR_PRICING_RESPONSE = {
    'AgentFeeCalculation': {
        'Charge': 33.87,
        'Profit': 0.0
    },
    'AllowedDocumentTypes': ['RussianPassport', 'BirthCertificate', 'ForeignPassport', 'RussianForeignPassport',
                             'MilitaryCard', 'SailorPassport', 'NonExistingPassportType'],
    'BookingSystem': 'Express3',
    'Cars': [
        {
            'ArrivalDateTime': '2017-10-12T09:05:00',
            'CarDescription': 'Ж',
            'CarNumber': '01',
            'CarPlaceType': 'NearTable',
            'CarSubType': '60С',
            'CarType': 'Sedentary',
            'CarTypeName': 'СИД',
            'Carrier': 'ДОСС',
            'Discounts': [
                {
                    'Description': 'Скидка по тарифу Senior',
                    'DiscountType': 'Junior'
                },
                {
                    'Description': 'Скидка по тарифу Junior',
                    'DiscountType': 'Senior'
                },
                {
                    'Description': 'Скидка при оформлении Туда/обратно',
                    'DiscountType': 'RoundTrip'
                },
                {
                    'Description': 'Скидка по универсальной карте',
                    'DiscountType': 'UniversalCard'
                }
            ],
            'FreePlaces': '17, 18, 19, 20',
            'FreePlacesByCompartments': [
                {
                    'CompartmentNumber': '9',
                    'Places': '17, 18'
                },
                {
                    'CompartmentNumber': '10',
                    'Places': '19, 20'
                },
            ],
            'HasDynamicPricing': True,
            'HasElectronicRegistration': True,
            'HasGenderCabins': False,
            'HasNoInterchange': False,
            'HasNonRefundableTariff': False,
            'HasPlaceNumeration': True,
            'HasPlacesNearBabies': False,
            'HasPlacesNearPets': True,
            'HasPlacesNearPlayground': False,
            'InfoRequestSchema': 'StandardExcludingInvalids',
            'InternationalServiceClass': '2/4',
            'IsAdditionalPassengerAllowed': False,
            'IsBeddingSelectionPossible': False,
            'IsChildTariffTypeAllowed': True,
            'IsSaleForbidden': False,
            'IsThreeHoursReservationAvailable': True,
            'IsTwoStorey': False,
            'MaxPrice': 1200.,
            'MinPrice': 10.,
            'PlaceQuantity': 4,
            'PlaceReservationType': 'Usual',
            'Road': 'РЖД/ОКТ',
            'RzhdCardTypes': ['RzhdBonus', 'UniversalRzhdCard'],
            'ServiceClass': '2С',
            'ServiceClassTranscript': None,
            'ServiceCost': 0.0,
            'Services': [],
            'TrainNumber': '751А',
            'AvailabilityIndication': 'Available',
            'IsTransitDocumentRequired': False,
        },
        {
            'ArrivalDateTime': '2017-10-12T09:05:00',
            'CarDescription': '* Ж',
            'CarNumber': '03',
            'CarPlaceType': 'WithPets',
            'CarSubType': '60С',
            'CarType': 'Sedentary',
            'CarTypeName': 'СИД',
            'Carrier': 'ДОСС',
            'Discounts': [
                {'Description': 'Скидка по тарифу Senior', 'DiscountType': 'Junior'},
                {'Description': 'Скидка по тарифу Junior', 'DiscountType': 'Senior'},
                {'Description': 'Скидка при оформлении Туда/обратно', 'DiscountType': 'RoundTrip'},
                {'Description': 'Скидка по универсальной карте', 'DiscountType': 'UniversalCard'}
            ],
            'FreePlaces': '1, 3',
            'HasDynamicPricing': True,
            'HasElectronicRegistration': True,
            'HasGenderCabins': False,
            'HasNoInterchange': False,
            'HasNonRefundableTariff': True,
            'HasPlaceNumeration': True,
            'HasPlacesNearBabies': False,
            'HasPlacesNearPets': False,
            'HasPlacesNearPlayground': False,
            'InfoRequestSchema': 'StandardExcludingInvalids',
            'InternationalServiceClass': '1/1',
            'IsAdditionalPassengerAllowed': False,
            'IsBeddingSelectionPossible': False,
            'IsChildTariffTypeAllowed': True,
            'IsSaleForbidden': False,
            'IsThreeHoursReservationAvailable': True,
            'IsTwoStorey': False,
            'MaxPrice': 1700.,
            'MinPrice': 1700.,
            'PlaceQuantity': 2,
            'PlaceReservationType': 'Usual',
            'Road': 'РЖД/ОКТ',
            'RzhdCardTypes': [
                'RzhdBonus',
                'UniversalRzhdCard'
            ],
            'ServiceClass': '2С',
            'ServiceClassTranscript': None,
            'ServiceCost': 0.0,
            'Services': [],
            'TrainNumber': '751А',
            'AvailabilityIndication': 'Available',
            'IsTransitDocumentRequired': False,
        },
        {
            'ArrivalDateTime': '2017-10-12T09:05:00',
            'CarDescription': '* Ж',
            'CarNumber': '03',
            'CarPlaceType': 'WithPets',
            'CarSubType': '60С',
            'CarType': 'Sedentary',
            'CarTypeName': 'СИД',
            'Carrier': 'ДОСС',
            'Discounts': [
                {'Description': 'Скидка по тарифу Senior', 'DiscountType': 'Junior'},
                {'Description': 'Скидка по тарифу Junior', 'DiscountType': 'Senior'},
                {'Description': 'Скидка при оформлении Туда/обратно', 'DiscountType': 'RoundTrip'},
                {'Description': 'Скидка по универсальной карте', 'DiscountType': 'UniversalCard'}
            ],
            'FreePlaces': '1, 2, 3, 4',
            'HasDynamicPricing': True,
            'HasElectronicRegistration': True,
            'HasGenderCabins': False,
            'HasNoInterchange': False,
            'HasNonRefundableTariff': False,
            'HasPlaceNumeration': True,
            'HasPlacesNearBabies': False,
            'HasPlacesNearPets': False,
            'HasPlacesNearPlayground': False,
            'InfoRequestSchema': 'StandardExcludingInvalids',
            'InternationalServiceClass': '1/2',
            'IsAdditionalPassengerAllowed': False,
            'IsBeddingSelectionPossible': False,
            'IsChildTariffTypeAllowed': True,
            'IsSaleForbidden': False,
            'IsThreeHoursReservationAvailable': True,
            'IsTwoStorey': False,
            'MaxPrice': 1400.,
            'MinPrice': 1400.,
            'PlaceQuantity': 4,
            'PlaceReservationType': 'Usual',
            'Road': 'РЖД/ОКТ',
            'RzhdCardTypes': [
                'RzhdBonus',
                'UniversalRzhdCard'
            ],
            'ServiceClass': '2С',
            'ServiceClassTranscript': None,
            'ServiceCost': 0.0,
            'Services': [],
            'TrainNumber': '751А',
            'AvailabilityIndication': 'Available',
            'IsTransitDocumentRequired': False,
        },
        {
            'ArrivalDateTime': '2017-10-12T09:05:00',
            'CarDescription': 'Ж',
            'CarNumber': '07',
            'CarPlaceType': 'Upper',
            'CarSubType': '41П',
            'CarType': 'ReservedSeat',
            'CarTypeName': 'ПЛАЦ',
            'Carrier': 'ФПК',
            'Discounts': [
                {
                    'Description': 'Скидка по школьному тарифу',
                    'DiscountType': 'Pupil'
                }
            ],
            'FreePlaces': '2, 4, 6',
            'HasDynamicPricing': False,
            'HasElectronicRegistration': True,
            'HasGenderCabins': False,
            'HasNoInterchange': True,
            'HasNonRefundableTariff': False,
            'HasPlaceNumeration': True,
            'HasPlacesNearBabies': False,
            'HasPlacesNearPets': True,
            'HasPlacesNearPlayground': False,
            'InfoRequestSchema': 'StandardExcludingInvalids',
            'InternationalServiceClass': '1/6',
            'IsAdditionalPassengerAllowed': False,
            'IsBeddingSelectionPossible': True,
            'IsChildTariffTypeAllowed': True,
            'IsSaleForbidden': False,
            'IsSpecialSaleMode': True,
            'IsThreeHoursReservationAvailable': False,
            'IsTwoStorey': False,
            'MaxPrice': 1500.,
            'MinPrice': 1500.,
            'PlaceQuantity': 3,
            'PlaceReservationType': 'Usual',
            'Road': 'РЖД/ОКТ',
            'RzhdCardTypes': [
                'RzhdBonus'
            ],
            'ServiceClass': '3Д',
            'ServiceClassTranscript': None,
            'ServiceCost': 100.,
            'Services': [],
            'TrainNumber': '751Б',
            'AvailabilityIndication': 'Available',
            'IsTransitDocumentRequired': False,
        },
    ],
    'ClientFeeCalculation': None,
    'DestinationCode': '2060001',
    'IsFromUkrain': False,
    'OriginCode': '2000001',
    'RoutePolicy': 'Internal',
    'TrainInfo': {
        'ArrivalDateTime': '2017-10-12T11:05:00',
        'ArrivalDateTimes': [
            '2017-10-12T11:05:00'
        ],
        'ArrivalStopTime': 0,
        'CarServices': [],
        'DepartureDateFromFormingStation': '2017-10-12T00:00:00',
        'DepartureDateTime': '2017-10-12T07:15:00',
        'DepartureStopTime': 0,
        'DestinationName': 'Н.НОВГОРОД М',
        'DestinationNames': [
            'Н.НОВГОРОД М'
        ],
        'DestinationStationCode': '2060001',
        'InitialStationName': 'МОСКВА ЯР',
        'FinalStationName': 'ВЛАДИВОСТ',
        'IsComponent': False,
        'IsSaleForbidden': False,
        'IsSuburban': False,
        'OriginName': 'МОСКВА КУР',
        'OriginStationCode': '2000001',
        'TrainDescription': 'СКРСТ ФИРМ',
        'TrainName': 'ЛАСТОЧКА',
        'TrainNumber': '751А',
        'TrainNumberToGetRoute': '751А',
        'DisplayTrainNumber': '752*А',
        'TransportType': 'Train',
        'TripDistance': 442,
        'TripDuration': 230
    }
}

IM_CAR_PRICING_RESPONSE_FOUR_PLACES_AT_ONCE_NON_REFUNDABLE = {
    "OriginCode": "2006004",
    "DestinationCode": "2004001",
    "OriginTimeZoneDifference": 0,
    "DestinationTimeZoneDifference": 0,
    "Cars": [
        {
            "DestinationStationCode": "2004001",
            "CarType": "Sedentary",
            "RailwayCarSchemeId": None,
            "CarSubType": "91С",
            "CarTypeName": "СИД",
            "CarSchemeName": "91С",
            "CarNumber": "01",
            "ServiceClass": "1Р",
            "ServiceClassNameRu": "VIР-КУПЕ",
            "ServiceClassNameEn": None,
            "InternationalServiceClass": "",
            "CarDescription": "У1",
            "ServiceClassTranscript": "",
            "FreePlaces": "27, 28, 29, 30",
            "FreePlacesWithCoordinates": None,
            "FreePlacesByCompartments": [
                {
                    "CompartmentNumber": "14",
                    "Places": "27, 28, 29, 30"
                }
            ],
            "PlaceQuantity": 4,
            "IsTwoStorey": False,
            "Services": [
                "Meal",
                "AirConditioning"
            ],
            "PetTransportationShortDescription": None,
            "PetTransportationFullDescription": None,
            "MinPrice": 42107.0,
            "MaxPrice": 57655.2,
            "ServiceCost": 3762.6,
            "PlaceReservationType": "FourPlacesAtOnce",
            "Carrier": "ДОСС",
            "CarrierDisplayName": "ДОСС",
            "HasGenderCabins": False,
            "RzhdCardTypes": [
                "RzhdBonus"
            ],
            "TrainNumber": "752А",
            "ArrivalDateTime": "2021-09-21T09:17:00",
            "LocalArrivalDateTime": "2021-09-21T09:17:00",
            "HasNoInterchange": False,
            "HasPlaceNumeration": True,
            "IsBeddingSelectionPossible": False,
            "HasElectronicRegistration": True,
            "HasDynamicPricing": False,
            "HasPlacesNearBabies": False,
            "HasPlacesNearPlayground": False,
            "HasPlacesNearPets": False,
            "HasNonRefundableTariff": True,
            "OnlyNonRefundableTariff": False,
            "IsAdditionalPassengerAllowed": False,
            "IsChildTariffTypeAllowed": True,
            "CarPlaceType": "NearTable",
            "CarPlaceDirection": None,
            "Discounts": [
                {
                    "DiscountType": "RoundTrip",
                    "Description": "Скидка при оформлении Туда/обратно"
                }
            ],
            "IsSaleForbidden": False,
            "AvailabilityIndication": "Available",
            "IsThreeHoursReservationAvailable": False,
            "Road": "РЖД/ОКТ",
            "InfoRequestSchema": "StandardIncludingInvalids",
            "PassengerSpecifyingRules": "FourPlacesAtOnce",
            "IsInTripPaymentAvailable": False,
            "IsMealOptionPossible": True,
            "IsAdditionalMealOptionPossible": False,
            "IsOnRequestMealOptionPossible": False,
            "MealSalesOpenedTill": "2021-09-20T00:00:00",
            "IsTransitDocumentRequired": False,
            "IsInterstate": False,
            "ClientFeeCalculation": None,
            "AgentFeeCalculation": {
                "Charge": 45.26,
                "Profit": 0.0
            },
            "IsBranded": False,
            "IsBuffet": False,
            "TripDirection": "Internal",
            "IsFromUkrainianCalcCenter": False,
            "IsForDisabledPersons": False,
            "IsCarWithoutPlaces": False,
            "IsSpecialSaleMode": False,
            "BoardingSystemType": "ElectronicTicketControl",
            "AvailableBaggageTypes": []
        }
    ],
    "RoutePolicy": "Internal",
    "TrainInfo": {
        "TrainNumber": "752А",
        "TrainNumberToGetRoute": "752А",
        "DisplayTrainNumber": "752А",
        "TrainDescription": "В-СКОР",
        "TrainFrequency": None,
        "TrainName": "САПСАН",
        "TransportType": "Train",
        "IsCarPricingSupported": True,
        "OriginName": "МОСКВА ОКТ",
        "InitialStationName": "МОСКВА ОКТ",
        "OriginStationCode": "2006004",
        "InitialTrainStationCode": "2006004",
        "DestinationName": "С-ПЕТЕР-ГЛ",
        "FinalStationName": "С-ПЕТЕР-ГЛ",
        "DestinationStationCode": "2004001",
        "FinalTrainStationCode": "2004001",
        "DestinationNames": [
            "С-ПЕТЕР-ГЛ"
        ],
        "FinalStationNames": [
            "С-ПЕТЕР-ГЛ"
        ],
        "DepartureDateTime": "2021-09-21T05:45:00",
        "LocalDepartureDateTime": "2021-09-21T05:45:00",
        "ArrivalDateTime": "2021-09-21T09:17:00",
        "LocalArrivalDateTime": "2021-09-21T09:17:00",
        "ArrivalDateTimes": [
            "2021-09-21T09:17:00"
        ],
        "LocalArrivalDateTimes": [
            "2021-09-21T09:17:00"
        ],
        "DepartureDateFromFormingStation": "2021-09-21T00:00:00",
        "DepartureStopTime": 0,
        "ArrivalStopTime": 0,
        "TripDuration": 212,
        "TripDistance": 650,
        "IsSuburban": False,
        "IsComponent": False,
        "CarServices": [
            "Meal"
        ],
        "IsSaleForbidden": False,
        "IsTicketPrintRequiredForBoarding": False,
        "BookingSystem": "Express3",
        "Provider": "P1",
        "IsVrStorageSystem": False,
        "BuybackDate": None,
        "PlacesStorageType": "Russia",
        "BoardingSystemTypes": [
            "ElectronicTicketControl"
        ]
    },
    "IsFromUkrain": False,
    "AllowedDocumentTypes": [
        "ForeignPassport",
        "RussianForeignPassport",
        "StatelessPersonIdentityCard",
        "DiplomaticPassport",
        "ServicePassport",
        "ReturnToCisCertificate",
        "RussianPassport",
        "UssrPassport",
        "BirthCertificate",
        "MilitaryCard",
        "SailorPassport",
        "PrisonReleaseCertificate",
        "LostPassportCertificate",
        "ResidencePermit",
        "MilitaryOfficerCard",
        "RussianTemporaryIdentityCard",
        "MedicalBirthCertificate"
    ],
    "ClientFeeCalculation": None,
    "AgentFeeCalculation": {
        "Charge": 45.26,
        "Profit": 0.0
    },
    "BookingSystem": "Express3",
    "CarTariffPrices": None
}


@pytest.mark.parametrize('railway_dt, expected_date_value', (
    (datetime(2000, 1, 1), '2000-01-01T00:00:00'),
    (datetime(2000, 1, 1, 12, 15, 15), '2000-01-01T12:15:00'),
))
def test_get_train_details_params(httpretty, railway_dt, expected_date_value):
    station_from = create_station(__={'codes': {'express': '100500'}})
    station_to = create_station(__={'codes': {'express': '200300'}})
    mock_im(httpretty, IM_TRAIN_DETAILS_METHOD, json={})

    try:
        receiver.get_train_details(TrainDetailsQuery(
            TrainPartner.IM, station_from, station_to, railway_dt, '1234',
            provider='P1',
        ))
    except Exception:
        pass

    assert httpretty.last_request.parsed_body == {
        'OriginCode': '100500',
        'DestinationCode': '200300',
        'DepartureDate': expected_date_value,
        'TrainNumber': '1234',
        'TariffType': 'Full',
        'Provider': 'P1',
    }


@replace_setting('TRAIN_WIZARD_API_INDEXER_HOST', TRAIN_WIZARD_API_INDEXER_HOST)
def test_get_train_details(httpretty):
    station_from = create_station(__={'codes': {'express': '100500'}})
    station_to = create_station(__={'codes': {'express': '200300'}})
    railway_dt = datetime(2000, 1, 1)
    mock_im(httpretty, IM_TRAIN_DETAILS_METHOD, json=IM_CAR_PRICING_RESPONSE)
    httpretty.register_uri(httpretty.POST, 'https://{}/indexer/public-api/train/'.format(TRAIN_WIZARD_API_INDEXER_HOST))

    result = receiver.get_train_details(
        TrainDetailsQuery(TrainPartner.IM, station_from, station_to, railway_dt, '1234'))

    assert_that(
        result,
        has_properties(
            broken_coaches=contains_inanyorder(
                has_properties(
                    number='01',
                    errors=contains(TariffError.TOO_CHEAP),
                    service_class=has_properties(international_code='2/4'),
                    is_special_sale_mode=False,
                    through_arrival=None,
                ),
            ),
            coaches=contains_inanyorder(
                has_properties(
                    number='03',
                    errors=empty(),
                    min_tariff=1700.,
                    service_class=has_properties(international_code='1/1'),
                    is_special_sale_mode=False,
                    through_arrival=None,
                    has_non_refundable_tariff=True,
                ),
                has_properties(
                    number='03',
                    errors=empty(),
                    min_tariff=1400.,
                    service_class=has_properties(international_code='1/2'),
                    is_special_sale_mode=False,
                    through_arrival=None,
                    has_non_refundable_tariff=False,
                ),
                has_properties(
                    number='07',
                    errors=empty(),
                    min_tariff=1500.,
                    service_class=has_properties(international_code='1/6'),
                    is_special_sale_mode=True,
                    through_arrival=smart_localize(datetime(2017, 10, 12, 9, 5), MSK_TZ),
                    has_non_refundable_tariff=False,
                ),
            ),
            route_policy=RoutePolicy.INTERNAL.value,
            im_initial_station_name='МОСКВА ЯР',
            im_final_station_name='ВЛАДИВОСТ',
        ),
    )

    assert_that(httpretty.last_request.parsed_body, has_entries({
        'electronic_ticket': True,
        'coaches': contains_inanyorder(
            has_entries({
                'number': '01',
                'errors': contains(TariffError.TOO_CHEAP.value),
                'places': contains(
                    has_entries({'number': 17, 'price': has_entries({'value': 1200.0})}),
                    has_entries({'number': 18, 'price': has_entries({'value': 1200.0})}),
                    has_entries({'number': 19, 'price': has_entries({'value': 1200.0})}),
                    has_entries({'number': 20, 'price': has_entries({'value': 1200.0})}),
                ),
            }),
            has_entries({
                'number': '03',
                'errors': empty(),
                'places': contains(
                    has_entries({'number': 1, 'price': has_entries({'value': 1700.0})}),
                    has_entries({'number': 3, 'price': has_entries({'value': 1700.0})}),
                ),
            }),
            has_entries({
                'number': '03',
                'errors': empty(),
                'places': contains(
                    has_entries({'number': 1, 'price': has_entries({'value': 1400.0})}),
                    has_entries({'number': 2, 'price': has_entries({'value': 1400.0})}),
                    has_entries({'number': 3, 'price': has_entries({'value': 1400.0})}),
                    has_entries({'number': 4, 'price': has_entries({'value': 1400.0})}),
                ),
            }),
            has_entries({
                'number': '07',
                'errors': empty(),
                'places': contains(
                    has_entries({'number': 2, 'price': has_entries({'value': 1400.0})}),
                    has_entries({'number': 4, 'price': has_entries({'value': 1400.0})}),
                    has_entries({'number': 6, 'price': has_entries({'value': 1400.0})}),
                ),
            }),
        ),
    }))


@replace_setting('TRAIN_WIZARD_API_INDEXER_HOST', None)
@pytest.mark.parametrize('is_suburban, predicate', (
    (True, lambda a, b: a == b),
    (False, lambda a, b: a != b),
))
def test_get_train_details_suburban_child_pricing(httpretty, is_suburban, predicate):
    station_from = create_station(__={'codes': {'express': '100500'}})
    station_to = create_station(__={'codes': {'express': '200300'}})
    railway_dt = datetime(2000, 1, 1)
    im_response = deepcopy(IM_CAR_PRICING_RESPONSE)
    im_response['TrainInfo']['IsSuburban'] = is_suburban
    mock_im(httpretty, IM_TRAIN_DETAILS_METHOD, json=im_response)

    result = receiver.get_train_details(
        TrainDetailsQuery(TrainPartner.IM, station_from, station_to, railway_dt, '1234'))

    assert all(
        predicate(place.child_tariff, place.adult_tariff)
        for coach in result.coaches
        for place in coach.places
    )


def _fill_tariff_info():
    TariffInfo.objects.create(code=TariffInfo.FULL_CODE, title_ru='Полный', im_request_code='Full')
    TariffInfo.objects.create(code=TariffInfo.CHILD_CODE, title_ru='Детский', im_request_code='Full')
    TariffInfo.objects.create(code='senior', title_ru='Старше 60 лет', im_request_code='Senior')


@replace_dynamic_setting('TRAIN_PURCHASE_TARIFF_DIRECTORIES_ENABLED', True)
def test_get_train_details_with_tariff_directories(httpretty):
    station_from = create_station(__={'codes': {'express': '100500'}})
    station_to = create_station(__={'codes': {'express': '200300'}})
    railway_dt = datetime(2000, 1, 1)
    mock_im(httpretty, IM_TRAIN_DETAILS_METHOD, json=IM_CAR_PRICING_RESPONSE)
    _fill_tariff_info()

    result = receiver.get_train_details(
        TrainDetailsQuery(TrainPartner.IM, station_from, station_to, railway_dt, '1234'))

    assert_that(result, has_properties(
        tariff_categories=contains(
            has_properties(code='baby', route_policy='internal'),
            has_properties(code='child', route_policy='internal'),
            has_properties(code='full', route_policy='internal'),
        ),
        broken_coaches=contains_inanyorder(
            has_properties(
                number='01',
                tariff_types=contains_inanyorder(
                    has_properties(code='senior', route_policy='internal'),
                    has_properties(code='full', route_policy='internal'),
                ),
            ),
        ),
        coaches=contains_inanyorder(
            has_properties(
                number='03',
                tariff_types=contains_inanyorder(
                    has_properties(code='senior', route_policy='internal'),
                    has_properties(code='full', route_policy='internal'),
                ),
            ),
            has_properties(
                number='03',
                tariff_types=contains_inanyorder(
                    has_properties(code='senior', route_policy='internal'),
                    has_properties(code='full', route_policy='internal'),
                ),
            ),
            has_properties(
                number='07',
                tariff_types=contains_inanyorder(
                    has_properties(code='full', route_policy='internal'),
                ),
            ),
        ),
    ))


@replace_dynamic_setting('TRAIN_PURCHASE_TARIFF_DIRECTORIES_ENABLED', False)
def test_get_train_details_without_tariff_directories(httpretty):
    station_from = create_station(__={'codes': {'express': '100500'}})
    station_to = create_station(__={'codes': {'express': '200300'}})
    railway_dt = datetime(2000, 1, 1)
    mock_im(httpretty, IM_TRAIN_DETAILS_METHOD, json=IM_CAR_PRICING_RESPONSE)
    _fill_tariff_info()

    result = receiver.get_train_details(
        TrainDetailsQuery(TrainPartner.IM, station_from, station_to, railway_dt, '1234'))

    assert_that(result, has_properties(
        tariff_categories=none(),
        broken_coaches=contains_inanyorder(
            has_properties(number='01', tariff_types=none()),
        ),
        coaches=contains_inanyorder(
            has_properties(number='03', tariff_types=none()),
            has_properties(number='03', tariff_types=none()),
            has_properties(number='07', tariff_types=none()),
        ),
    ))


@replace_dynamic_setting('TRAIN_PURCHASE_TARIFF_DIRECTORIES_ENABLED', True)
@replace_dynamic_setting('TRAIN_PURCHASE_ENABLE_CPPK_CONDITIONS', False)
def test_get_suburban_train_details_with_tariff_directories(httpretty):
    im_car_pricing_response_copy = deepcopy(IM_CAR_PRICING_RESPONSE)
    im_car_pricing_response_copy['TrainInfo']['IsSuburban'] = True

    station_from = create_station(__={'codes': {'express': '100500'}})
    station_to = create_station(__={'codes': {'express': '200300'}})
    railway_dt = datetime(2000, 1, 1)
    mock_im(httpretty, IM_TRAIN_DETAILS_METHOD, json=im_car_pricing_response_copy)
    _fill_tariff_info()

    result = receiver.get_train_details(
        TrainDetailsQuery(TrainPartner.IM, station_from, station_to, railway_dt, '1234'))

    assert_that(result, has_properties(
        is_cppk=False,
        tariff_categories=contains(
            has_properties(code='baby', route_policy='suburban'),
            has_properties(code='child', route_policy='suburban'),
            has_properties(code='full', route_policy='suburban'),
        ),
    ))


@replace_dynamic_setting('TRAIN_PURCHASE_TARIFF_DIRECTORIES_ENABLED', True)
@replace_dynamic_setting('TRAIN_PURCHASE_ENABLE_CPPK_CONDITIONS', True)
def test_get_cppk_train_details_with_tariff_directories(httpretty):
    im_car_pricing_response_copy = deepcopy(IM_CAR_PRICING_RESPONSE)
    im_car_pricing_response_copy['TrainInfo']['IsSuburban'] = True
    im_car_pricing_response_copy['TrainInfo']['Provider'] = 'P2'

    station_from = create_station(__={'codes': {'express': '100500'}})
    station_to = create_station(__={'codes': {'express': '200300'}})
    railway_dt = datetime(2000, 1, 1)
    mock_im(httpretty, IM_TRAIN_DETAILS_METHOD, json=im_car_pricing_response_copy)
    _fill_tariff_info()

    result = receiver.get_train_details(
        TrainDetailsQuery(TrainPartner.IM, station_from, station_to, railway_dt, '1234'))

    assert_that(result, has_properties(
        is_cppk=True,
        tariff_categories=contains(
            has_properties(code='child', route_policy='cppk', min_age=0),
            has_properties(code='full', route_policy='cppk'),
        ),
        broken_coaches=contains(
            has_properties(
                number='01',
                tariff_types=contains(
                    has_properties(code='full', route_policy='internal'),
                ),
            ),
        ),
        coaches=contains_inanyorder(
            has_properties(
                number='03',
                tariff_types=contains(
                    has_properties(code='full', route_policy='internal'),
                ),
            ),
            has_properties(
                number='03',
                tariff_types=contains(
                    has_properties(code='full', route_policy='internal'),
                ),
            ),
            has_properties(
                number='07',
                tariff_types=contains(
                    has_properties(code='full', route_policy='internal'),
                ),
            ),
        ),
    ))


def _create_bandit_response_lazy():
    return api_pb2.TGetChargeResponse(
        ChargesByContexts=[
            api_pb2.TCharge(
                InternalId=0,
                Permille=110,
                TicketFees={
                    0: api_pb2.TTicketFee(
                        Fee=commons_pb2.TPrice(Amount=16500, Precision=2),
                        ServiceFee=commons_pb2.TPrice(Amount=1100, Precision=2),
                        IsBanditFeeApplied=True,
                    ),
                    1: api_pb2.TTicketFee(
                        Fee=commons_pb2.TPrice(Amount=6320, Precision=2),
                        ServiceFee=commons_pb2.TPrice(Amount=1100, Precision=2),
                        IsBanditFeeApplied=True,
                    ),
                    2: api_pb2.TTicketFee(
                        Fee=commons_pb2.TPrice(Amount=15400, Precision=2),
                        ServiceFee=commons_pb2.TPrice(Amount=1100, Precision=2),
                        IsBanditFeeApplied=True,
                    ),
                    # second car:
                    3: api_pb2.TTicketFee(
                        Fee=commons_pb2.TPrice(Amount=15400, Precision=2),
                        ServiceFee=commons_pb2.TPrice(Amount=2200, Precision=2),
                        IsBanditFeeApplied=True,
                    ),
                    4: api_pb2.TTicketFee(
                        Fee=commons_pb2.TPrice(Amount=16000, Precision=2),
                        ServiceFee=commons_pb2.TPrice(Amount=1100, Precision=2),
                        IsBanditFeeApplied=True,
                    ),
                },
                BanditType='BanditType',
                BanditVersion=100500,
            ),
        ])


@replace_dynamic_setting('TRAIN_PURCHASE_BANDIT_CHARGING', True)
@replace_dynamic_setting('TRAIN_PURCHASE_LAZY_BANDIT_CHARGING', True)
@mock.patch.object(api_pb2_grpc, 'BanditApiServiceV1Stub', autospec=True)
@mock.patch.object(base_parsers, 'calculate_ticket_cost', autospec=True)
@mock.patch.object(im_parsers, 'calculate_ticket_cost', autospec=True)
def test_bandit_charging_train_details_lazy(m_calc1, m_calc2, m_stub, httpretty):
    im_car_pricing_response_copy = deepcopy(IM_CAR_PRICING_RESPONSE)
    del im_car_pricing_response_copy['Cars'][0:3]
    im_car_pricing_response_copy['Cars'][0]['CarNumber'] = "07"
    im_car_pricing_response_copy['Cars'][0]['PlaceReservationType'] = 'TwoPlacesAtOnce'
    im_car_pricing_response_copy['Cars'][0]['MaxPrice'] = 1600.
    im_car_pricing_response_copy['Cars'].append(deepcopy(im_car_pricing_response_copy['Cars'][0]))
    im_car_pricing_response_copy['Cars'][1]['CarNumber'] = "08"
    im_car_pricing_response_copy['Cars'][1]['MaxPrice'] = 1700.
    im_car_pricing_response_copy['Cars'][1]['MinPrice'] = 1600.
    im_car_pricing_response_copy['Cars'][1]['PlaceReservationType'] = 'Usual'
    im_car_pricing_response_copy['Cars'].append(deepcopy(im_car_pricing_response_copy['Cars'][1]))
    im_car_pricing_response_copy['Cars'][2]['CarNumber'] = "09"
    im_car_pricing_response_copy['Cars'][2]['FreePlaces'] = ""
    im_car_pricing_response_copy['Cars'][2]['FreePlacesByCompartments'] = []

    station_from = create_station(__={'codes': {'express': '100500'}}, id=1005000)
    station_to = create_station(__={'codes': {'express': '200300'}}, id=2003000)
    railway_dt = datetime(2000, 1, 1)
    mock_im(httpretty, IM_TRAIN_DETAILS_METHOD, json=im_car_pricing_response_copy)
    _fill_tariff_info()
    query_data = {'icookie': 'someCookie', 'bandit_type': 'banditType'}
    m_stub.return_value.GetCharge = mock.Mock(return_value=_create_bandit_response_lazy())

    response = receiver.get_train_details(
        TrainDetailsQuery(TrainPartner.IM, station_from, station_to, railway_dt, '1234',
                          yandex_uid='someUid', raw_query_data=query_data))

    assert m_calc1.call_count == 0
    assert m_calc2.call_count == 0
    assert m_stub.return_value.GetCharge.call_count == 1
    assert_that(
        m_stub.return_value.GetCharge.call_args.args[0],
        has_properties(
            BanditType='banditType',
            ContextsWithPrices=contains(
                has_properties(
                    Context=has_properties(CarType='platzkarte'),
                    TicketPrices=has_entries({
                        0: has_properties(
                            Amount=has_properties(Amount=150000, Precision=2, Currency=commons_pb2.C_RUB),
                            ServiceAmount=has_properties(Amount=10000, Precision=2,
                                                         Currency=commons_pb2.C_RUB),
                        ),
                        1: has_properties(
                            Amount=has_properties(Amount=62500, Precision=2, Currency=commons_pb2.C_RUB),
                            ServiceAmount=has_properties(Amount=10000, Precision=2,
                                                         Currency=commons_pb2.C_RUB),
                        ),
                        2: has_properties(
                            Amount=has_properties(Amount=160000, Precision=2, Currency=commons_pb2.C_RUB),
                            ServiceAmount=has_properties(Amount=20000, Precision=2,
                                                         Currency=commons_pb2.C_RUB),
                        ),
                        # second car:
                        3: has_properties(
                            Amount=has_properties(Amount=170000, Precision=2, Currency=commons_pb2.C_RUB),
                            ServiceAmount=has_properties(Amount=10000, Precision=2,
                                                         Currency=commons_pb2.C_RUB),
                        ),
                        4: has_properties(
                            Amount=has_properties(Amount=66000, Precision=2, Currency=commons_pb2.C_RUB),
                            ServiceAmount=has_properties(Amount=10000, Precision=2,
                                                         Currency=commons_pb2.C_RUB),
                        ),
                    }),
                ),
            ),
        )
    )
    assert_that(response.coaches, contains(
        has_properties(
            number='07',
            adult_tariff=has_properties(value=Decimal('1565.00')),
            bedding_tariff_with_fee=has_properties(value=Decimal('111.00')),
            fee_calculation_token=not_(empty()),
            reservation_variants=contains_inanyorder(
                has_properties(amount=Decimal('1676.00')),
                has_properties(amount=Decimal('1676.00')),
                has_properties(amount=Decimal('1676.00')),
                has_properties(amount=Decimal('1765.00')),
                has_properties(amount=Decimal('1765.00')),
                has_properties(amount=Decimal('1765.00')),
                has_properties(amount=Decimal('1765.00')),
                has_properties(amount=Decimal('1765.00')),
                has_properties(amount=Decimal('1765.00')),
                has_properties(amount=Decimal('1765.00')),
            ),
            places=contains_inanyorder(
                has_properties(
                    is_bandit_fee_applied=True,
                    adult_tariff=has_properties(value=Decimal('1565.00')),
                    bedding_tariff_with_fee=has_properties(value=Decimal('111.00')),
                    yandex_fee_percent=Decimal('0.11'),
                    bandit_type='BanditType',
                    bandit_version=100500,
                ),
                has_properties(
                    is_bandit_fee_applied=True,
                    adult_tariff=has_properties(value=Decimal('1565.00')),
                    bedding_tariff_with_fee=has_properties(value=Decimal('111.00')),
                    yandex_fee_percent=Decimal('0.11'),
                    bandit_type='BanditType',
                    bandit_version=100500,
                ),
                has_properties(
                    is_bandit_fee_applied=True,
                    adult_tariff=has_properties(value=Decimal('1565.00')),
                    bedding_tariff_with_fee=has_properties(value=Decimal('111.00')),
                    yandex_fee_percent=Decimal('0.11'),
                    bandit_type='BanditType',
                    bandit_version=100500,
                ),
            ),
        ),
        has_properties(
            number='08',
            adult_tariff=has_properties(value=Decimal('1754.00')),
            bedding_tariff_with_fee=has_properties(value=Decimal('122.00')),
            places=contains_inanyorder(
                has_properties(
                    is_bandit_fee_applied=True,
                    adult_tariff=has_properties(value=Decimal('1754.00')),
                    bedding_tariff_with_fee=has_properties(value=Decimal('122.00')),
                    yandex_fee_percent=Decimal('0.11'),
                    bandit_type='BanditType',
                    bandit_version=100500,
                ),
                has_properties(
                    is_bandit_fee_applied=True,
                    adult_tariff=has_properties(value=Decimal('1754.00')),
                    bedding_tariff_with_fee=has_properties(value=Decimal('122.00')),
                    yandex_fee_percent=Decimal('0.11'),
                    bandit_type='BanditType',
                    bandit_version=100500,
                ),
                has_properties(
                    is_bandit_fee_applied=True,
                    adult_tariff=has_properties(value=Decimal('1754.00')),
                    bedding_tariff_with_fee=has_properties(value=Decimal('122.00')),
                    yandex_fee_percent=Decimal('0.11'),
                    bandit_type='BanditType',
                    bandit_version=100500,
                ),
            ),
        ),
        has_properties(
            number='09',
            adult_tariff=has_properties(value=Decimal('1754.00')),
            bedding_tariff_with_fee=has_properties(value=Decimal('122.00')),
            fee_calculation_token=not_(empty()),
            places=empty(),
        ),
    ))


def mockGetCharge(*args, **kwargs):
    request = args[0]  # api_pb2.TGetChargeRequest
    return api_pb2.TGetChargeResponse(
        ChargesByContexts=[
            api_pb2.TCharge(
                InternalId=charge_rq.InternalId,
                Permille=110,
                TicketFees={
                    price_id: api_pb2.TTicketFee(
                        Fee=commons_pb2.TPrice(Amount=int(ticket_price_rq.Amount.Amount * 0.11), Precision=2),
                        ServiceFee=commons_pb2.TPrice(Amount=0, Precision=2),
                        IsBanditFeeApplied=True,
                    ) for price_id, ticket_price_rq in charge_rq.TicketPrices.items()
                },
                BanditType=request.BanditType,
                BanditVersion=100500,
            ) for charge_rq in request.ContextsWithPrices])


@replace_dynamic_setting('TRAIN_PURCHASE_BANDIT_CHARGING', True)
@replace_dynamic_setting('TRAIN_PURCHASE_LAZY_BANDIT_CHARGING', True)
@replace_dynamic_setting('TRAIN_PURCHASE_ENABLE_NON_REFUNDABLE', True)
@mock.patch.object(api_pb2_grpc, 'BanditApiServiceV1Stub', autospec=True)
@mock.patch.object(base_parsers, 'calculate_ticket_cost', autospec=True)
@mock.patch.object(im_parsers, 'calculate_ticket_cost', autospec=True)
def test_train_details_reservation_variants_non_refundable(m_calc1, m_calc2, m_stub, httpretty):
    im_car_pricing_response_copy = deepcopy(IM_CAR_PRICING_RESPONSE_FOUR_PLACES_AT_ONCE_NON_REFUNDABLE)
    m_stub.return_value.GetCharge = mock.Mock(side_effect=mockGetCharge)
    mock_im(httpretty, IM_TRAIN_DETAILS_METHOD, json=im_car_pricing_response_copy)
    _fill_tariff_info()

    query_data = {'icookie': 'someCookie', 'bandit_type': 'banditType'}
    station_from = create_station(__={'codes': {'express': '2006004'}}, id=1005000)
    station_to = create_station(__={'codes': {'express': '2004001'}}, id=2003000)
    railway_dt = datetime(2000, 1, 1)
    response = receiver.get_train_details(
        TrainDetailsQuery(TrainPartner.IM, station_from, station_to, railway_dt, '1234',
                          yandex_uid='someUid', raw_query_data=query_data))

    assert m_calc1.call_count == 0
    assert m_calc2.call_count == 0
    assert_that(response.coaches, contains(
        has_properties(
            number='01',
            fee_calculation_token=not_(empty()),
            reservation_variants=contains(
                has_properties(amount=Decimal('51467.91'),
                               non_refundable_amount=Decimal('46738.77'),
                               passengers={"baby": 0, "full": 1, "child": 0}),
                anything(),
                anything(),
                anything(),
                has_properties(amount=Decimal('55644.39'),
                               non_refundable_amount=Decimal('50915.25'),
                               passengers={"baby": 0, "full": 2, "child": 0}),
                anything(),
                anything(),
                anything(),
                anything(),
                anything(),
                anything(),
                has_properties(amount=Decimal('59820.88'),
                               non_refundable_amount=Decimal('55091.74'),
                               passengers={"baby": 0, "full": 3, "child": 0}),
                anything(),
                anything(),
                anything(),
                anything(),
                has_properties(amount=Decimal('63997.37'),
                               non_refundable_amount=Decimal('59268.22'),
                               passengers={"baby": 0, "full": 4, "child": 0}),
            ),
        )
    ))
