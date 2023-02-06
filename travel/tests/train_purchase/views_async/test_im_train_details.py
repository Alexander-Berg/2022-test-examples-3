# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

from datetime import datetime
from decimal import Decimal

import mock
import pytest
from django.db import models
from hamcrest import assert_that, has_entries, contains_inanyorder, has_items, contains

from travel.proto.dicts.trains.coach_binding_pb2 import TCoachBinding
from travel.proto.dicts.trains.coach_type_pb2 import TCoachType
from common.apps.train_order.enums import CoachType
from common.tester.factories import create_station, create_country
from common.tester.utils.replace_setting import replace_dynamic_setting, replace_dynamic_setting_key
from common.utils.date import MSK_TZ
from travel.rasp.train_api.train_partners.base.train_details.coach_schemas import BestSchemaFinder, ProtoSchemaProxy
from travel.rasp.train_api.train_partners.base.train_details.models import CoachSchema, CoachSchemaBinding
from travel.rasp.train_api.train_partners.im.factories.utils import mock_im
from travel.rasp.train_api.train_partners.im.train_details.receiver import IM_TRAIN_DETAILS_METHOD
from travel.rasp.train_api.train_purchase.core.factories import ClientContractsFactory, ClientContractFactory
from travel.rasp.train_api.train_purchase.utils import fee_calculator

pytestmark = [pytest.mark.dbuser,
              pytest.mark.mongouser,
              pytest.mark.usefixtures('full_tariff_info')]

NEAR_PET_TARIFF = 1200.
WITH_PET_TARIFF = 1400.
UPPER_TARIFF = 1500.
LOWER_TARIFF = 1600.
SERVICE_TARIFF = 100.
NON_REFUNDABLE_MIN = 3700.
NON_REFUNDABLE_MAX = 4100.

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
            'CarNumber': '03',
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
            'InternationalServiceClass': '4/4',
            'IsAdditionalPassengerAllowed': False,
            'IsBeddingSelectionPossible': False,
            'IsChildTariffTypeAllowed': True,
            'IsSaleForbidden': False,
            'IsThreeHoursReservationAvailable': True,
            'IsTwoStorey': False,
            'MaxPrice': NEAR_PET_TARIFF,
            'MinPrice': NEAR_PET_TARIFF,
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
            'RailwayCarSchemeId': 244,
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
            'InternationalServiceClass': '4/4',
            'IsAdditionalPassengerAllowed': False,
            'IsBeddingSelectionPossible': False,
            'IsChildTariffTypeAllowed': True,
            'IsSaleForbidden': False,
            'IsThreeHoursReservationAvailable': True,
            'IsTwoStorey': False,
            'MaxPrice': WITH_PET_TARIFF,
            'MinPrice': WITH_PET_TARIFF,
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
            'RailwayCarSchemeId': 244,
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
            'HasNoInterchange': False,
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
            'IsThreeHoursReservationAvailable': False,
            'IsTwoStorey': False,
            'MaxPrice': UPPER_TARIFF,
            'MinPrice': UPPER_TARIFF,
            'PlaceQuantity': 3,
            'PlaceReservationType': 'Usual',
            'Road': 'РЖД/ОКТ',
            'RzhdCardTypes': [
                'RzhdBonus'
            ],
            'ServiceClass': '3Д',
            'ServiceClassTranscript': None,
            'ServiceCost': SERVICE_TARIFF,
            'Services': [],
            'TrainNumber': '751Б',
            'AvailabilityIndication': 'Available',
            'IsTransitDocumentRequired': False,
            'RailwayCarSchemeId': 211,
        },
        {
            'ArrivalDateTime': '2017-10-12T09:05:00',
            'CarDescription': 'Ж',
            'CarNumber': '07',
            'CarPlaceType': 'Lower',
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
            'FreePlaces': '1, 3, 5',
            'HasDynamicPricing': False,
            'HasElectronicRegistration': True,
            'HasGenderCabins': False,
            'HasNoInterchange': False,
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
            'IsThreeHoursReservationAvailable': False,
            'IsTwoStorey': False,
            'MaxPrice': LOWER_TARIFF,
            'MinPrice': LOWER_TARIFF,
            'PlaceQuantity': 3,
            'PlaceReservationType': 'Usual',
            'Road': 'РЖД/ОКТ',
            'RzhdCardTypes': ['RzhdBonus'],
            'ServiceClass': '3Д',
            'ServiceClassTranscript': None,
            'ServiceCost': SERVICE_TARIFF,
            'Services': [],
            'TrainNumber': '751Б',
            'AvailabilityIndication': 'Available',
            'IsTransitDocumentRequired': False,
            'RailwayCarSchemeId': 211,
        },
        {
            'ArrivalDateTime': '2017-10-12T09:05:00',
            'CarDescription': 'У1',
            'CarNumber': '09',
            'CarPlaceType': 'NoValue',
            'CarSubType': '18М',
            'CarType': 'Soft',
            'CarTypeName': 'ЛЮКС',
            'Carrier': 'ФПК',
            'Discounts': [],
            'FreePlaces': '5, 6, 7, 8',
            'FreePlacesByCompartments': [
                {
                    'CompartmentNumber': '3',
                    'Places': '5, 6'
                },
                {
                    'CompartmentNumber': '4',
                    'Places': '7, 8'
                },
            ],
            'HasDynamicPricing': False,
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
            'IsBeddingSelectionPossible': True,
            'IsChildTariffTypeAllowed': True,
            'IsSaleForbidden': False,
            'IsThreeHoursReservationAvailable': False,
            'IsTwoStorey': False,
            'MaxPrice': LOWER_TARIFF + SERVICE_TARIFF,
            'MinPrice': LOWER_TARIFF,
            'PlaceQuantity': 4,
            'PlaceReservationType': 'TwoPlacesAtOnce',
            'Road': 'РЖД/ОКТ',
            'RzhdCardTypes': ['RzhdBonus'],
            'ServiceClass': '1М',
            'ServiceClassTranscript': None,
            'ServiceCost': SERVICE_TARIFF,
            'Services': [],
            'TrainNumber': '751Б',
            'AvailabilityIndication': 'Available',
            'IsTransitDocumentRequired': False,
            'RailwayCarSchemeId': 222,
        },
        {
            "DestinationStationCode": "2020500",
            "CarType": "Compartment",
            "CarSubType": "66К",
            "CarTypeName": "КУПЕ",
            "CarNumber": "11",
            "ServiceClass": "2Т",
            "InternationalServiceClass": "",
            "CarDescription": "У1",
            "ServiceClassTranscript": "",
            "FreePlaces": "3",
            "FreePlacesByCompartments": [
                {
                    "CompartmentNumber": "1",
                    "Places": "3"
                },
            ],
            "PlaceQuantity": 1,
            "IsTwoStorey": False,
            "Services": [
                "Meal",
                "AirConditioning",
                "BioToilet",
                "HygienicKit",
                "Press"
            ],
            "MinPrice": NON_REFUNDABLE_MIN,
            "MaxPrice": NON_REFUNDABLE_MAX,
            "ServiceCost": SERVICE_TARIFF,
            "PlaceReservationType": "Usual",
            "Carrier": "ФПК",
            "HasGenderCabins": False,
            "RzhdCardTypes": [
                "RzhdBonus"
            ],
            "TrainNumber": "001И",
            "ArrivalDateTime": "2020-07-31T08:15:00",
            "LocalArrivalDateTime": "2020-07-31T09:15:00",
            "HasNoInterchange": False,
            "HasPlaceNumeration": True,
            "IsBeddingSelectionPossible": False,
            "HasElectronicRegistration": True,
            "HasDynamicPricing": True,
            "HasPlacesNearBabies": False,
            "HasPlacesNearPlayground": False,
            "HasPlacesNearPets": False,
            "HasNonRefundableTariff": True,
            "IsAdditionalPassengerAllowed": False,
            "IsChildTariffTypeAllowed": True,
            "CarPlaceType": "Lower",
            "CarPlaceDirection": None,
            "Discounts": [],
            "IsSaleForbidden": False,
            "AvailabilityIndication": "Available",
            "IsThreeHoursReservationAvailable": False,
            "Road": "РЖД/ПРИВ",
            "InfoRequestSchema": "StandardExcludingInvalids",
            "PassengerSpecifyingRules": "Standard",
            "IsMealOptionPossible": True,
            "IsAdditionalMealOptionPossible": True,
            "IsOnRequestMealOptionPossible": False,
            "MealSalesOpenedTill": "2020-07-28T00:00:00",
            "IsTransitDocumentRequired": False,
            "IsInterstate": False,
            "ClientFeeCalculation": None,
            "AgentFeeCalculation": {
                "Charge": 31.70,
                "Profit": 0.0
            },
            "IsBranded": True,
            "IsBuffet": False,
            "TripDirection": "Internal",
            "IsFromUkrainianCalcCenter": False,
            "IsForDisabledPersons": False,
            "IsCarWithoutPlaces": False,
            "IsSpecialSaleMode": False,
            "BoardingSystemType": "PassengerBoardingControl",
            'RailwayCarSchemeId': 233,
        }
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

IM_CAR_PRICING_EMPTY_PLACES_RESPONSE = {
    "OriginCode": "2004001",
    "DestinationCode": "2004400",
    "OriginTimeZoneDifference": 0,
    "DestinationTimeZoneDifference": 0,
    "Cars": [
        {
            "DestinationStationCode": "2004400",
            "CarType": "Shared",
            "RailwayCarSchemeId": None,
            "CarSubType": "82О",
            "CarTypeName": "ОБЩ",
            "CarSchemeName": "82О",
            "CarNumber": "03",
            "ServiceClass": "3В",
            "ServiceClassNameRu": None,
            "ServiceClassNameEn": None,
            "InternationalServiceClass": "",
            "CarDescription": "",
            "ServiceClassTranscript": "",
            "FreePlaces": "",
            "FreePlacesWithCoordinates": None,
            "FreePlacesByCompartments": [],
            "PlaceQuantity": 5,
            "IsTwoStorey": False,
            "Services": [],
            "PetTransportationShortDescription": None,
            "PetTransportationFullDescription": None,
            "MinPrice": WITH_PET_TARIFF,
            "MaxPrice": WITH_PET_TARIFF,
            "ServiceCost": 0.0,
            "PlaceReservationType": "Usual",
            "Carrier": "СЗППК",
            "CarrierDisplayName": "СЗППК",
            "HasGenderCabins": False,
            "RzhdCardTypes": [],
            "TrainNumber": "801А",
            "ArrivalDateTime": "2022-01-03T10:17:00",
            "LocalArrivalDateTime": "2022-01-03T10:17:00",
            "HasNoInterchange": False,
            "HasPlaceNumeration": True,
            "IsBeddingSelectionPossible": False,
            "HasElectronicRegistration": True,
            "HasDynamicPricing": False,
            "HasPlacesNearBabies": False,
            "HasPlacesNearPlayground": False,
            "HasPlacesNearPets": False,
            "HasNonRefundableTariff": False,
            "OnlyNonRefundableTariff": False,
            "IsAdditionalPassengerAllowed": False,
            "IsChildTariffTypeAllowed": True,
            "CarPlaceType": "NoValue",
            "CarPlaceDirection": None,
            "Discounts": [],
            "IsSaleForbidden": False,
            "AvailabilityIndication": "Available",
            "IsThreeHoursReservationAvailable": False,
            "Road": "РЖД/ОКТ",
            "InfoRequestSchema": "StandardIncludingInvalids",
            "PassengerSpecifyingRules": "Standard",
            "IsInTripPaymentAvailable": False,
            "IsMealOptionPossible": False,
            "IsAdditionalMealOptionPossible": False,
            "IsOnRequestMealOptionPossible": False,
            "MealSalesOpenedTill": "2022-01-01T00:00:00",
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
            "BoardingSystemType": "NoValue",
            "AvailableBaggageTypes": []
        }
    ],
    "RoutePolicy": "Internal",
    "TrainInfo": {
        "TrainNumber": "801А",
        "TrainNumberToGetRoute": "801А",
        "DisplayTrainNumber": "7001А",
        "TrainDescription": "СК",
        "TrainFrequency": None,
        "TrainName": "",
        "TransportType": "Train",
        "IsCarPricingSupported": True,
        "OriginName": "С-ПЕТЕР-ГЛ",
        "InitialStationName": "С-ПЕТЕР-ГЛ",
        "OriginStationCode": "2004001",
        "InitialTrainStationCode": "2004001",
        "DestinationName": "ВЕЛИК.НОВГОР",
        "FinalStationName": "ВЕЛИК.НОВГОР",
        "DestinationStationCode": "2004400",
        "FinalTrainStationCode": "2004400",
        "DestinationNames": [
            "ВЕЛИК.НОВГОР"
        ],
        "FinalStationNames": [
            "ВЕЛИК.НОВГОР"
        ],
        "DepartureDateTime": "2022-01-03T07:26:00",
        "LocalDepartureDateTime": "2022-01-03T07:26:00",
        "ArrivalDateTime": "2022-01-03T10:17:00",
        "LocalArrivalDateTime": "2022-01-03T10:17:00",
        "ArrivalDateTimes": [
            "2022-01-03T10:17:00"
        ],
        "LocalArrivalDateTimes": [
            "2022-01-03T10:17:00"
        ],
        "DepartureDateFromFormingStation": "2022-01-03T00:00:00",
        "DepartureStopTime": 0,
        "ArrivalStopTime": 0,
        "TripDuration": 171,
        "TripDistance": 192,
        "IsSuburban": True,
        "IsComponent": False,
        "CarServices": [],
        "IsSaleForbidden": False,
        "IsTicketPrintRequiredForBoarding": False,
        "BookingSystem": "Express3",
        "Provider": "P1",
        "IsVrStorageSystem": False,
        "BuybackDate": None,
        "PlacesStorageType": "Russia",
        "BoardingSystemTypes": [
            "NoValue"
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


def _add_commission(tariff, contract, yandex_uid=None):
    return _add_commission_with_service(tariff, Decimal(0), contract, yandex_uid=yandex_uid)


def _add_commission_with_service(tariff, service_tariff, contract, yandex_uid=None):
    ticket_cost = fee_calculator.calculate_ticket_cost(
        contract, CoachType.PLATZKARTE.value, Decimal(tariff),
        service_amount=Decimal(service_tariff), yandex_uid=yandex_uid,
    )
    return float(ticket_cost.full_amount), float(ticket_cost.full_bedding_amount)


@replace_dynamic_setting('TRAIN_PURCHASE_COVID_CERT_REQ_COUNTRIES', [149])
@replace_dynamic_setting('TRAIN_PURCHASE_BANDIT_LOGGING', True)
@replace_dynamic_setting('TRAIN_PURCHASE_ENABLE_NON_REFUNDABLE', True)
@pytest.mark.parametrize('protobufs_setting_value', (True, False))
@mock.patch.object(BestSchemaFinder, 'find_best_schema', autospec=True)
def test_im_full(m_find_best_schema, httpretty, async_urlconf_client, protobufs_setting_value):
    with replace_dynamic_setting_key('TRAIN_BACKEND_USE_PROTOBUFS', 'schemas', protobufs_setting_value):
        mock_im(httpretty, IM_TRAIN_DETAILS_METHOD, json=IM_CAR_PRICING_RESPONSE)

        if protobufs_setting_value:
            schema = ProtoSchemaProxy.create(Id=2, Name='Схема')
            m_find_best_schema.return_value = (schema, TCoachBinding(
                Klass=TCoachType.EType.Value('COMPARTMENT'),
                SchemaId=schema.id,
                CoachNumber='03'
            ))
        else:
            image = mock.MagicMock(spec=models.ImageField, width=100, height=30, _committed=True,
                                   read=mock.MagicMock(return_value="fake file contents"))
            schema = CoachSchema(id=2, name='Схема', image=image)
            m_find_best_schema.return_value = (schema, CoachSchemaBinding(
                klass='compartment',
                schema=schema,
                coach_number='03',
                im_car_scheme_id='233|234',
            ))

        contract = ClientContractFactory(partner_commission_sum=Decimal('10.0'),
                                         partner_commission_sum2=Decimal('10.0'))
        ClientContractsFactory(contracts=[contract])
        create_country(id=149)
        station_from = create_station(__={'codes': {'express': '100500'}}, country_id=149, settlement={})
        station_to = create_station(__={'codes': {'express': '200300'}})
        params = {
            'stationFrom': station_from.id,
            'stationTo': station_to.id,
            'number': '100',
            'when': '2000-01-01T00:00:00',
            'includePriceFee': '1',
            'partner': 'im',
            'yandex_uid': 'someUid',
        }

        response = async_urlconf_client.get('/ru/api/train-details/', params)

        assert response.status_code == 200
        assert m_find_best_schema.call_count == 5
        assert len(response.data['trainDetails']['schemas']) == 1
        assert response.data['trainDetails']['stationFrom']['title']
        assert response.data['trainDetails']['stationFrom']['settlement']['title']
        assert response.data['trainDetails']['isCovidCertificateRequired']
        assert_that(response.data['trainDetails'], has_entries({
            'arrival': MSK_TZ.localize(datetime(2017, 10, 12, 11, 5)),
            'departure': MSK_TZ.localize(datetime(2017, 10, 12, 7, 15)),
            'rawTrainName': 'ЛАСТОЧКА',
            'rawTrainCategory': 'СКРСТ ФИРМ',
            'isFirm': True,
            'startNumber': '751А',
            'electronicTicket': True,
            'ticketNumber': '752А',
            'isSuburban': False,
        }))

        near_pet = {
            'adultTariff': {'currency': 'RUR',
                            'value': _add_commission(NEAR_PET_TARIFF, contract)[0]},
            'beddingTariff': {
                'currency': 'RUR',
                'value': 0
            },
            'number': '03',
            'trainStartNumber': '751А',
            'petInCoach': True,
            'petsAllowed': False,
            'petsSegregated': True,
            'hasDynamicPricing': True,
            'placeCounts': {'total': 4, 'upperSide': 0, 'lowerSide': 0, 'upperCoupe': 0, 'lowerCoupe': 0},
            'coachSubtypeCode': '60С',
            'arrival': MSK_TZ.localize(datetime(2017, 10, 12, 9, 5)),
            'placesByType': {'NearTable': [17, 18, 19, 20]},
            'placesByCompartment': [[17, 18], [19, 20]],
            'placeReservationType': 'usual',
            'serviceClass': has_entries({'code': '2С', 'internationalCode': '4/4'}),
            'placeRequirementsAvailable': True,
        }
        with_pet = {
            'adultTariff': {'currency': 'RUR',
                            'value': _add_commission(WITH_PET_TARIFF, contract)[0]},
            'beddingTariff': {
                'currency': 'RUR',
                'value': 0
            },
            'number': '03',
            'petInCoach': False,
            'petsAllowed': True,
            'petsSegregated': True,
            'hasDynamicPricing': True,
            'placeCounts': {'total': 4, 'upperSide': 0, 'lowerSide': 0, 'upperCoupe': 0, 'lowerCoupe': 0},
            'coachSubtypeCode': '60С',
            'arrival': MSK_TZ.localize(datetime(2017, 10, 12, 9, 5)),
            'placesByType': {'WithPets': [1, 2, 3, 4]},
            'placeReservationType': 'usual',
            'serviceClass': has_entries({'code': '2С', 'internationalCode': '4/4'}),
            'placeRequirementsAvailable': True,
        }
        adult_tariff, bedding_tariff = _add_commission_with_service(UPPER_TARIFF, SERVICE_TARIFF, contract)
        with_service = {
            'adultTariff': {
                'currency': 'RUR',
                'value': adult_tariff - bedding_tariff,
            },
            'beddingTariff': {
                'currency': 'RUR',
                'value': bedding_tariff,
            },
            'canChooseBedding': True,
            'trainStartNumber': '751А',
            'number': '07',
            'hasDynamicPricing': False,
            'placeCounts': {'total': 6, 'upperSide': 0, u'lowerSide': 0, 'upperCoupe': 3, 'lowerCoupe': 3},
            'coachSubtypeCode': '41П',
            'arrival': MSK_TZ.localize(datetime(2017, 10, 12, 9, 5)),
            'placesByType': {'Upper': [2, 4, 6], 'Lower': [1, 3, 5]},
            'placeReservationType': 'usual',
            'serviceClass': has_entries({'code': '3Д', 'internationalCode': '1/6'}),
            'placeRequirementsAvailable': True,
        }
        min_adult_tariff, _ = _add_commission_with_service(LOWER_TARIFF, SERVICE_TARIFF, contract)
        max_adult_tariff, _ = _add_commission_with_service(LOWER_TARIFF + SERVICE_TARIFF, SERVICE_TARIFF * 2, contract)
        soft = {
            'number': '09',
            'placesByType': {'NoValue': [5, 6, 7, 8]},
            'placesByCompartment': [[5, 6], [7, 8]],
            'placeReservationType': 'two_places_at_once',
            'serviceClass': has_entries({'code': '1М', 'internationalCode': '1/2'}),
            'reservationVariants': contains_inanyorder(
                {
                    'passengers': {'full': 1, 'baby': 0, 'child': 0},
                    'amount': min_adult_tariff,
                    'placesCount': 2,
                    'giveChildWithoutPlace': False,
                    'nonRefundableAmount': None,
                },
                {
                    'passengers': {'full': 1, 'baby': 1, 'child': 0},
                    'amount': min_adult_tariff,
                    'placesCount': 2,
                    'giveChildWithoutPlace': False,
                    'nonRefundableAmount': None,
                },
                {
                    'passengers': {'full': 1, 'baby': 0, 'child': 1},
                    'amount': min_adult_tariff,
                    'placesCount': 2,
                    'giveChildWithoutPlace': True,
                    'nonRefundableAmount': None,
                },
                {
                    'passengers': {'full': 2, 'baby': 0, 'child': 0},
                    'amount': max_adult_tariff,
                    'placesCount': 2,
                    'giveChildWithoutPlace': False,
                    'nonRefundableAmount': None,
                },
                {
                    'passengers': {'full': 1, 'baby': 0, 'child': 1},
                    'amount': max_adult_tariff,
                    'placesCount': 2,
                    'giveChildWithoutPlace': False,
                    'nonRefundableAmount': None,
                },
                {
                    'passengers': {'full': 2, 'baby': 1, 'child': 0},
                    'amount': max_adult_tariff,
                    'placesCount': 2,
                    'giveChildWithoutPlace': False,
                    'nonRefundableAmount': None,
                },
                {
                    'passengers': {'full': 2, 'baby': 0, 'child': 1},
                    'amount': max_adult_tariff,
                    'placesCount': 2,
                    'giveChildWithoutPlace': True,
                    'nonRefundableAmount': None,
                },
                {
                    'passengers': {'full': 1, 'baby': 1, 'child': 1},
                    'amount': max_adult_tariff,
                    'placesCount': 2,
                    'giveChildWithoutPlace': False,
                    'nonRefundableAmount': None,
                },
                {
                    'passengers': {'full': 2, 'baby': 2, 'child': 0},
                    'amount': max_adult_tariff,
                    'placesCount': 2,
                    'giveChildWithoutPlace': False,
                    'nonRefundableAmount': None,
                },
                {
                    'passengers': {'full': 2, 'baby': 1, 'child': 1},
                    'amount': max_adult_tariff,
                    'placesCount': 2,
                    'giveChildWithoutPlace': True,
                    'nonRefundableAmount': None,
                }
            ),
            'placeRequirementsAvailable': False,
        }
        non_refundable = {
            'adultTariff': {'currency': 'RUR',
                            'value': _add_commission(NON_REFUNDABLE_MAX, contract)[0]},
            'beddingTariff': {
                'currency': 'RUR',
                'value': 0
            },
            'places': contains(has_entries({
                'number': 3,
                'adultTariff': {
                    'currency': "RUR",
                    'value': _add_commission(NON_REFUNDABLE_MAX, contract)[0],
                },
                'adultNonRefundableTariff': {
                    'currency': "RUR",
                    'value': _add_commission(NON_REFUNDABLE_MIN, contract)[0],
                }
            })),
            'number': '11',
            'petsAllowed': False,
            'hasNonRefundableTariff': True,
            'placeCounts': {'total': 1, 'upperSide': 0, 'lowerSide': 0, 'upperCoupe': 0, 'lowerCoupe': 1},
            'coachSubtypeCode': '66К',
            'placeReservationType': 'usual',
            'serviceClass': has_entries({'code': '2Т'}),
            'placeRequirementsAvailable': True,
        }
        assert_that(response.data['trainDetails']['coaches'], contains(
            has_entries(with_pet),
            has_entries(near_pet),
            has_entries(with_service),
            has_entries(soft),
            has_entries(non_refundable),
        ))


@replace_dynamic_setting('TRAIN_PURCHASE_EXPERIMENTAL_DELTA_FEE', '0.03')
@pytest.mark.parametrize('yandex_uid, price_exp_id, expected_adult_tariff_value', [
    (None, None, 1332.0),
    ('some_uid', None, 1332.0),
    (None, 'some_id', 1332.0),
    ('some_uid', 'some_id', 1345.8),
])
def test_im_with_experiment(httpretty, async_urlconf_client, yandex_uid, price_exp_id, expected_adult_tariff_value):
    mock_im(httpretty, IM_TRAIN_DETAILS_METHOD, json=IM_CAR_PRICING_RESPONSE)

    contract = ClientContractFactory(partner_commission_sum=Decimal('10.0'), partner_commission_sum2=Decimal('10.0'))
    ClientContractsFactory(contracts=[contract])
    station_from = create_station(__={'codes': {'express': '100500'}})
    station_to = create_station(__={'codes': {'express': '200300'}})
    params = {
        'stationFrom': station_from.id,
        'stationTo': station_to.id,
        'number': '100',
        'when': '2000-01-01T00:00:00',
        'includePriceFee': '1',
        'partner': 'im',
    }
    if yandex_uid:
        params['yandex_uid'] = yandex_uid
    if price_exp_id:
        params['priceExpId'] = price_exp_id

    response = async_urlconf_client.get('/ru/api/train-details/', params)

    assert response.status_code == 200
    assert_that(response.data['trainDetails']['coaches'], has_items(
        has_entries('adultTariff', has_entries('value', expected_adult_tariff_value)),
    ))


@replace_dynamic_setting('TRAIN_PURCHASE_COVID_CERT_REQ_COUNTRIES', [149])
@replace_dynamic_setting('TRAIN_PURCHASE_BANDIT_LOGGING', True)
@replace_dynamic_setting('TRAIN_PURCHASE_ENABLE_NON_REFUNDABLE', True)
@pytest.mark.parametrize('protobufs_setting_value', (True, False))
@mock.patch.object(BestSchemaFinder, 'find_best_schema', autospec=True)
def test_internal_full(m_find_best_schema, httpretty, async_urlconf_client, protobufs_setting_value):
    with replace_dynamic_setting_key('TRAIN_BACKEND_USE_PROTOBUFS', 'schemas', protobufs_setting_value):
        mock_im(httpretty, IM_TRAIN_DETAILS_METHOD, json=IM_CAR_PRICING_RESPONSE)

        if protobufs_setting_value:
            schema = ProtoSchemaProxy.create(Id=2, Name='Схема')
            m_find_best_schema.return_value = (schema, TCoachBinding(
                Klass=TCoachType.EType.Value('COMPARTMENT'),
                SchemaId=schema.id,
                CoachNumber='03'
            ))
        else:
            image = mock.MagicMock(spec=models.ImageField, width=100, height=30, _committed=True,
                                   read=mock.MagicMock(return_value="fake file contents"))
            schema = CoachSchema(id=2, name='Схема', image=image)
            m_find_best_schema.return_value = (schema, CoachSchemaBinding(
                klass='compartment',
                schema=schema,
                coach_number='03'
            ))

        contract = ClientContractFactory(partner_commission_sum=Decimal('10.0'),
                                         partner_commission_sum2=Decimal('10.0'))
        ClientContractsFactory(contracts=[contract])
        create_country(id=149)
        create_station(__={'codes': {'express': '100500'}}, country_id=149, settlement={})  # station_from
        create_station(__={'codes': {'express': '200300'}})  # station_to
        params = {
            'stationFrom': '100500',
            'stationTo': '200300',
            'number': '100',
            'when': '2000-01-01T00:00:00',
        }

        response = async_urlconf_client.get('/ru/api/internal/train-details/', params)

        print(response.data)
        assert response.status_code == 200
        assert m_find_best_schema.call_count == 5
        assert len(response.data['trainDetails']['schemas']) == 1
        assert response.data['trainDetails']['stationFrom']['title']
        assert response.data['trainDetails']['stationFrom']['settlement']['title']
        assert response.data['trainDetails']['isCovidCertificateRequired']
        assert_that(response.data['trainDetails'], has_entries({
            'arrival': MSK_TZ.localize(datetime(2017, 10, 12, 11, 5)),
            'departure': MSK_TZ.localize(datetime(2017, 10, 12, 7, 15)),
            'rawTrainName': 'ЛАСТОЧКА',
            'rawTrainCategory': 'СКРСТ ФИРМ',
            'isFirm': True,
            'startNumber': '751А',
            'electronicTicket': True,
            'ticketNumber': '752А',
            'isSuburban': False,
        }))


def test_im_empty_places(httpretty, async_urlconf_client):
    mock_im(httpretty, IM_TRAIN_DETAILS_METHOD, json=IM_CAR_PRICING_EMPTY_PLACES_RESPONSE)

    contract = ClientContractFactory(partner_commission_sum=Decimal('10.0'), partner_commission_sum2=Decimal('10.0'))
    ClientContractsFactory(contracts=[contract])
    station_from = create_station(__={'codes': {'express': '100500'}}, settlement={})
    station_to = create_station(__={'codes': {'express': '200300'}})
    params = {
        'stationFrom': station_from.id,
        'stationTo': station_to.id,
        'number': '100',
        'when': '2000-01-01T00:00:00',
        'includePriceFee': '1',
        'partner': 'im',
        'yandex_uid': 'someUid',
    }

    response = async_urlconf_client.get('/ru/api/train-details/', params)

    assert response.status_code == 200
    assert_that(response.data['trainDetails'], has_entries({
        'startNumber': '801А',
        'electronicTicket': True,
        'ticketNumber': '7001А',
        'isSuburban': True,
    }))
    assert_that(response.data['trainDetails']['coaches'][0],
                has_entries({
                    'adultTariff': {
                        'currency': 'RUR',
                        'value': _add_commission(WITH_PET_TARIFF, contract)[0]
                    },
                    'places': [],
                    'priceWithoutPlaces': has_entries({
                        "adultTariff": {
                            "currency": "RUR",
                            "value": _add_commission(WITH_PET_TARIFF, contract)[0]
                        },
                    }),
                    'beddingTariff': {
                        'currency': 'RUR',
                        'value': 0
                    },
                    'number': '03',
                    'placeCounts': {'total': 5, 'upperSide': 0, 'lowerSide': 0, 'upperCoupe': 0, 'lowerCoupe': 0},
                    'coachSubtypeCode': '82О',
                    'placeReservationType': 'usual',
                    'serviceClass': has_entries({'code': '3В'}),
                }),
                )
