# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

from datetime import date, datetime
from decimal import Decimal

import pytest

from common.apps.train_order.enums import CoachType
from common.models.currency import Price
from common.models.geo import Country, Settlement
from common.tester.factories import create_settlement
from common.utils.date import MSK_TZ
from travel.rasp.train_api.tariffs.train.base.availability_indication import AvailabilityIndication
from travel.rasp.train_api.tariffs.train.base.models import TrainTariff, PlaceReservationType
from travel.rasp.train_api.tariffs.train.factories.base import create_train_tariffs_query
from travel.rasp.train_api.tariffs.train.factories.im import ImTrainPricingResponseFactory
from travel.rasp.train_api.tariffs.train.im.parser import build_train_segments
from travel.rasp.train_api.train_purchase.core.models import TrainPartner


# TODO: Протестировать местное и жд время на Самара - Актобе
# TODO: Протестировать типы вагонов и тарифы - плацкарт, купе, СВ
# TODO: Протестировать типы вагонов и тарифы - Ласточки
# TODO: Протестировать "купе целиком" в Стрижах Мск - НН и купе-переговорные в Сапсанах.
# TODO: Протестировать наличие * в номере поезда и смену номера поезда в пути.


def create_response():
    return ImTrainPricingResponseFactory(**{
        "OriginCode": "2004660",
        "OriginStationCode": "2004660",
        "DestinationCode": "2004600",
        "DestinationStationCode": "2004600",
        "Trains": [
            {
                "HasElectronicRegistration": True,
                "HasDynamicPricingCars": True,
                "HasTwoStoreyCars": True,
                "Carriers": ["ФПК"],
                "CarGroups": [
                    {
                        "CarType": "Compartment",
                        "CarTypeName": "КУПЕ",
                        "LowerPlaceQuantity": 2,
                        "UpperPlaceQuantity": 2,
                        "MinPrice": 677,
                        "MaxPrice": 1030,
                        "Carriers": ["ФПК"],
                        "CarDescriptions": ["Ж"],
                        "ServiceClasses": ["2Л"],
                        "ServiceCosts": [148],
                        "IsBeddingSelectionPossible": False,
                        "HasElectronicRegistration": True,
                        "HasGenderCabins": False,
                        "HasPlaceNumeration": True,
                        "HasPlacesNearPlayground": False,
                        "HasPlacesNearPets": True,
                        "HasPlacesNearBabies": False,
                        "HasNonRefundableTariff": True,
                        "InfoRequestSchema": "StandardExcludingInvalids",
                        "TotalPlaceQuantity": 4,
                        "PlaceReservationTypes": ["Usual"],
                        "AvailabilityIndication": "Available",
                        "IsTransitDocumentRequired": False
                    },
                    {
                        "CarType": "Compartment",
                        "CarTypeName": "КУПЕ",
                        "LowerPlaceQuantity": 14,
                        "UpperPlaceQuantity": 17,
                        "MinPrice": 688,
                        "MaxPrice": 1048,
                        "Carriers": ["ФПК"],
                        "CarDescriptions": ["Ж"],
                        "ServiceClasses": ["2У"],
                        "ServiceCosts": [148],
                        "IsBeddingSelectionPossible": False,
                        "HasElectronicRegistration": True,
                        "HasGenderCabins": False,
                        "HasPlaceNumeration": True,
                        "HasPlacesNearPlayground": False,
                        "HasPlacesNearPets": True,
                        "HasPlacesNearBabies": False,
                        "HasNonRefundableTariff": False,
                        "InfoRequestSchema": "StandardExcludingInvalids",
                        "TotalPlaceQuantity": 31,
                        "PlaceReservationTypes": ["Usual"],
                        "AvailabilityIndication": "Available",
                        "IsTransitDocumentRequired": False
                    },
                ],
                "TrainNumber": "042Ч",
                "TrainNumberToGetRoute": "042Ч",
                "DisplayTrainNumber": "042Ч",
                "TrainDescription": "СК",
                "TrainName": "",
                "TransportType": "Train",
                "OriginName": "НОВГОР ВОЛ",
                "OriginStationCode": "2004660",
                "DestinationName": "МОСКВА ОКТ",
                "DestinationStationCode": "2004600",
                "DestinationNames": ["МОСКВА ОКТ"],
                "DepartureDateTime": "2017-09-20T01:35:00",
                "ArrivalDateTime": "2017-09-20T03:27:00",
                "DepartureDateFromFormingStation": "2017-09-19T00:00:00",
                "IsSuburban": False,
                "IsComponent": False,
                "IsSaleForbidden": False,
                "Provider": "P1",
            },
            {
                "HasElectronicRegistration": False,
                "HasDynamicPricingCars": False,
                "HasTwoStoreyCars": False,
                "Carriers": ["ТКС", "ФПК"],
                "CarGroups": [
                    {
                        "CarType": "Compartment",
                        "CarTypeName": "КУПЕ",
                        "LowerPlaceQuantity": 2,
                        "UpperPlaceQuantity": 2,
                        "MinPrice": 1145,
                        "MaxPrice": 1406,
                        "Carriers": ["ФПК"],
                        "CarDescriptions": ["Ж"],
                        "ServiceClasses": ["2К"],
                        "ServiceCosts": [218],
                        "IsBeddingSelectionPossible": False,
                        "HasElectronicRegistration": False,
                        "HasGenderCabins": False,
                        "HasPlaceNumeration": True,
                        "HasPlacesNearPlayground": False,
                        "HasPlacesNearPets": True,
                        "HasPlacesNearBabies": False,
                        "HasNonRefundableTariff": False,
                        "InfoRequestSchema": "StandardExcludingInvalids",
                        "TotalPlaceQuantity": 4,
                        "PlaceReservationTypes": ["Usual"],
                        "AvailabilityIndication": "Available",
                        "IsTransitDocumentRequired": False
                    },
                    {
                        "CarType": "Compartment",
                        "CarTypeName": "КУПЕ",
                        "LowerPlaceQuantity": 12,
                        "UpperPlaceQuantity": 17,
                        "MinPrice": 1624,
                        "MaxPrice": 1885,
                        "Carriers": ["ТКС"],
                        "CarDescriptions": ["У1"],
                        "ServiceClasses": ["2Т"],
                        "ServiceCosts": [697],
                        "IsBeddingSelectionPossible": False,
                        "HasElectronicRegistration": False,
                        "HasGenderCabins": False,
                        "HasPlaceNumeration": True,
                        "HasPlacesNearPlayground": False,
                        "HasPlacesNearPets": False,
                        "HasPlacesNearBabies": False,
                        "HasNonRefundableTariff": False,
                        "InfoRequestSchema": "StandardExcludingInvalids",
                        "TotalPlaceQuantity": 29,
                        "PlaceReservationTypes": ["Usual"],
                        "AvailabilityIndication": "Available",
                        "IsTransitDocumentRequired": False
                    },
                    {
                        "CarType": "Compartment",
                        "CarTypeName": "КУПЕ",
                        "LowerPlaceQuantity": 16,
                        "UpperPlaceQuantity": 16,
                        "MinPrice": 1625,
                        "MaxPrice": 1886,
                        "Carriers": ["ФПК"],
                        "CarDescriptions": ["МЖ У1"],
                        "ServiceClasses": ["2Э"],
                        "ServiceCosts": [698],
                        "IsBeddingSelectionPossible": False,
                        "HasElectronicRegistration": False,
                        "HasGenderCabins": True,
                        "HasPlaceNumeration": True,
                        "HasPlacesNearPlayground": False,
                        "HasPlacesNearPets": False,
                        "HasPlacesNearBabies": False,
                        "HasNonRefundableTariff": False,
                        "InfoRequestSchema": "StandardExcludingInvalids",
                        "TotalPlaceQuantity": 32,
                        "PlaceReservationTypes": ["Usual"],
                        "AvailabilityIndication": "Available",
                        "IsTransitDocumentRequired": False
                    },
                ],
                "TrainNumber": "010Ч",
                "TrainNumberToGetRoute": "010Ч",
                "DisplayTrainNumber": "010Ч",
                "TrainDescription": "СК ФИРМ",
                "TrainName": "Псков",
                "TransportType": "Train",
                "OriginName": "ПСКОВ ПАСС",
                "OriginStationCode": "2004660",
                "DestinationName": "МОСКВА ОКТ",
                "DestinationStationCode": "2004600",
                "DestinationNames": ["МОСКВА ОКТ"],
                "DepartureDateTime": "2017-09-20T02:21:00",
                "ArrivalDateTime": "2017-09-20T04:07:00",
                "DepartureDateFromFormingStation": "2017-09-19T00:00:00",
                "DepartureStopTime": 30,
                "ArrivalStopTime": 1,
                "TripDuration": 106,
                "TripDistance": 164,
                "IsSuburban": True,
                "IsComponent": False,
                "IsSaleForbidden": False,
                "Provider": "P1",
            },
            {
                "HasElectronicRegistration": True,
                "HasDynamicPricingCars": True,
                "HasTwoStoreyCars": True,
                "Carriers": ["ФПК"],
                "CarGroups": [
                    {
                        "CarType": "Compartment",
                        "CarTypeName": "КУПЕ",
                        "LowerPlaceQuantity": 2,
                        "UpperPlaceQuantity": 2,
                        "MinPrice": 677,
                        "MaxPrice": 1030,
                        "Carriers": ["ФПК"],
                        "CarDescriptions": ["Ж"],
                        "ServiceClasses": ["2Л"],
                        "ServiceCosts": [148],
                        "IsBeddingSelectionPossible": False,
                        "HasElectronicRegistration": True,
                        "HasGenderCabins": False,
                        "HasPlaceNumeration": True,
                        "HasPlacesNearPlayground": False,
                        "HasPlacesNearPets": True,
                        "HasPlacesNearBabies": False,
                        "HasNonRefundableTariff": False,
                        "InfoRequestSchema": "StandardExcludingInvalids",
                        "TotalPlaceQuantity": 4,
                        "PlaceReservationTypes": ["Usual"],
                        "AvailabilityIndication": "Available",
                        "IsTransitDocumentRequired": False
                    },
                    {
                        "CarType": "Compartment",
                        "CarTypeName": "КУПЕ",
                        "LowerPlaceQuantity": 14,
                        "UpperPlaceQuantity": 17,
                        "MinPrice": 688,
                        "MaxPrice": 1048,
                        "Carriers": ["ФПК"],
                        "CarDescriptions": ["Ж"],
                        "ServiceClasses": ["2У"],
                        "ServiceCosts": [148],
                        "IsBeddingSelectionPossible": False,
                        "HasElectronicRegistration": True,
                        "HasGenderCabins": False,
                        "HasPlaceNumeration": True,
                        "HasPlacesNearPlayground": False,
                        "HasPlacesNearPets": True,
                        "HasPlacesNearBabies": False,
                        "HasNonRefundableTariff": False,
                        "InfoRequestSchema": "StandardExcludingInvalids",
                        "TotalPlaceQuantity": 31,
                        "PlaceReservationTypes": ["Usual"],
                        "AvailabilityIndication": "Available",
                        "IsTransitDocumentRequired": False
                    },
                ],
                "TrainNumber": "042Ч",
                "TrainNumberToGetRoute": "042Ч",
                "DisplayTrainNumber": "042Ч",
                "TrainDescription": None,
                "TrainName": "",
                "TransportType": "Train",
                "OriginName": "НОВГОР ВОЛ",
                "OriginStationCode": "2004660",
                "DestinationName": "МОСКВА ОКТ",
                "DestinationStationCode": "2004600",
                "DestinationNames": ["МОСКВА ОКТ"],
                "DepartureDateTime": "2017-09-20T01:35:00",
                "ArrivalDateTime": "2017-09-20T03:27:00",
                "DepartureDateFromFormingStation": "2017-09-19T00:00:00",
                "DepartureStopTime": 15,
                "ArrivalStopTime": 1,
                "TripDuration": 112,
                "TripDistance": 164,
                "IsSuburban": False,
                "IsComponent": False,
                "IsSaleForbidden": False,
                "Provider": "P1",
            }
        ],
        "DepartureTimeDescription": "Moscow",
        "ArrivalTimeDescription": "Moscow",
        "IsFromUkrain": False,
        "NotAllTrainsReturned": False,
    })


@pytest.mark.dbuser
def test_bologoe_tver():
    spb = create_settlement(country_id=Country.RUSSIA_ID, title='Санкт-Петербург')
    query = create_train_tariffs_query(TrainPartner.IM, Settlement.objects.get(id=Settlement.MOSCOW_ID),
                                       spb, date(2017, 9, 20))
    segments = build_train_segments(create_response(), query)
    assert len(segments) == 3

    check_segment(
        segments[0], '042Ч', '042Ч', 'НОВГОР ВОЛ \N{em dash} МОСКВА ОКТ', 'НОВГОР ВОЛ', 'МОСКВА ОКТ',
        station_from_express_code='2004660', station_to_express_code='2004600',
        possible_numbers=['041Ч', '042Ч'],
        railway_departure=MSK_TZ.localize(datetime(2017, 9, 20, 1, 35)),
        railway_arrival=MSK_TZ.localize(datetime(2017, 9, 20, 3, 27)),
        departure=MSK_TZ.localize(datetime(2017, 9, 20, 1, 35)),
        arrival=MSK_TZ.localize(datetime(2017, 9, 20, 3, 27)),
        is_deluxe=False,
        coach_owners=['ФПК'],
        electronic_ticket=True,
        has_dynamic_pricing=True,
        two_storey=True,
        is_suburban=False,
        provider='P1',
        tariffs_classes={
            'compartment': TrainTariff(
                CoachType.COMPARTMENT,
                Price(Decimal(677), 'RUB'),
                Price(Decimal(148), 'RUB'),
                seats=35,
                lower_seats=16,
                upper_seats=19,
                max_seats_in_the_same_car=31,
                several_prices=True,
                place_reservation_type=PlaceReservationType.USUAL,
                is_transit_document_required=False,
                availability_indication=AvailabilityIndication.AVAILABLE,
                service_class='2Л',
                has_non_refundable_tariff=True,
            ),
        }
    )

    check_segment(
        segments[1], '010Ч', '010Ч', 'ПСКОВ ПАСС \N{em dash} МОСКВА ОКТ', 'ПСКОВ ПАСС', 'МОСКВА ОКТ',
        station_from_express_code='2004660', station_to_express_code='2004600',
        possible_numbers=['009Ч', '010Ч'],
        railway_departure=MSK_TZ.localize(datetime(2017, 9, 20, 2, 21)),
        railway_arrival=MSK_TZ.localize(datetime(2017, 9, 20, 4, 7)),
        departure=MSK_TZ.localize(datetime(2017, 9, 20, 2, 21)),
        arrival=MSK_TZ.localize(datetime(2017, 9, 20, 4, 7)),
        is_deluxe=True,
        coach_owners=['ФПК', 'ТКС'],
        electronic_ticket=False,
        has_dynamic_pricing=False,
        two_storey=False,
        is_suburban=True,
        provider='P1',
        tariffs_classes={
            'compartment': TrainTariff(
                CoachType.COMPARTMENT,
                Price(Decimal(1145), 'RUB'),
                Price(Decimal(218), 'RUB'),
                seats=65,
                lower_seats=30,
                upper_seats=35,
                max_seats_in_the_same_car=32,
                several_prices=True,
                place_reservation_type=PlaceReservationType.USUAL,
                is_transit_document_required=False,
                availability_indication=AvailabilityIndication.AVAILABLE,
                service_class='2К',
                has_non_refundable_tariff=False,
            ),
        }
    )

    assert not segments[2].is_deluxe


def check_segment(segment, number, original_number, ufs_title,
                  start_express_title_or_code, end_express_title_or_code,
                  station_from_express_code, station_to_express_code, possible_numbers,
                  railway_departure, railway_arrival, departure, arrival, is_deluxe,
                  coach_owners, electronic_ticket, has_dynamic_pricing, two_storey, is_suburban,
                  provider, tariffs_classes):
    assert segment.original_number == original_number
    assert segment.number == number
    assert segment.ufs_title == ufs_title
    assert segment.start_express_title_or_code == start_express_title_or_code
    assert segment.end_express_title_or_code == end_express_title_or_code
    assert segment.station_from_express_code == station_from_express_code
    assert segment.station_to_express_code == station_to_express_code
    assert set(segment.possible_numbers) == set(possible_numbers)
    assert segment.railway_departure == railway_departure
    assert segment.railway_arrival == railway_arrival
    assert segment.departure == departure
    assert segment.arrival == arrival
    assert segment.is_deluxe == is_deluxe
    assert set(segment.coach_owners) == set(coach_owners)
    assert segment.has_dynamic_pricing == has_dynamic_pricing
    assert segment.two_storey == two_storey
    assert segment.is_suburban == is_suburban
    assert segment.provider == provider
    assert segment.tariffs['electronic_ticket'] == electronic_ticket
    assert segment.tariffs['classes']['compartment'] == tariffs_classes['compartment']
