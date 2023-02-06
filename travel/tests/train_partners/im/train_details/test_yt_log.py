# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

from hamcrest import assert_that, has_items, has_entries

from travel.rasp.train_api.train_partners.im.train_details.yt_log import ImToYtParser

IM_RESPONSE = {
    "OriginCode": None,
    "DestinationCode": None,
    "Cars": [
        {
            "CarType": "Luxury",
            "CarSubType": None,
            "CarTypeName": "СВ",
            "CarNumber": "08",
            "ServiceClass": "1Б",
            "InternationalServiceClass": "",
            "CarDescription": "У1",
            "ServiceClassTranscript": 'Кондиционер.\r\nПерсональный золотой унитаз,\tзавтрак в постель.',
            "FreePlaces": "1, 2, 3, 4Ж, к5, 6, 7, 8, 9, 10, 13, 14, 15, 16, 17, 18",
            "PlaceQuantity": 16,
            "IsTwoStorey": False,
            "Services": ["Meal"],
            "MinPrice": 5911.4,
            "MaxPrice": 5911.4,
            "ServiceCost": 0.0,
            "PlaceReservationType": "Usual",
            "Carrier": "ГРАНД",
            "HasGenderCabins": False,
            "RzhdCardTypes": [],
            "TrainNumber": "054Ч",
            "ArrivalDateTime": "2016-11-02T08:36:00",
            "HasNoInterchange": False,
            "HasPlaceNumeration": True,
            "IsBeddingSelectionPossible": False,
            "HasElectronicRegistration": True,
            "HasDynamicPricing": False,
            "HasPlacesNearBabies": False,
            "HasPlacesNearPlayground": False,
            "HasPlacesNearPets": False,
            "IsAdditionalPassengerAllowed": False,
            "IsChildTariffTypeAllowed": False,
            "CarPlaceType": "Lower",
            "Discounts": [],
            "IsSaleForbidden": False,
            "IsThreeHoursReservationAvailable": False,
            "Road": None,
            "InfoRequestSchema": "StandardExcludingInvalids",
            "PassengerSpecifyingRules": "Standard"
        }
    ],
    "RoutePolicy": "Internal",
    "TrainInfo": {
        "TrainNumber": "054Ч",
        "TrainNumberToGetRoute": "054Ч",
        "DisplayTrainNumber": "054Ч",
        "TrainDescription": "СК",
        "TrainName": "ГРАНД",
        "TransportType": "Train",
        "OriginName": "МОСКВА ОКТ",
        "OriginStationCode": "2000000",
        "DestinationName": "С-ПЕТЕР-ГЛ",
        "DestinationStationCode": "2004000",
        "DestinationNames": ["С-ПЕТЕР-ГЛ"],
        "DepartureDateTime": "2016-11-01T23:40:00",
        "ArrivalDateTime": "2016-11-02T08:36:00",
        "ArrivalDateTimes": ["2016-11-02T08:36:00"],
        "DepartureDateFromFormingStation": "0001-01-01T00:00:00",
        "DepartureStopTime": 0,
        "ArrivalStopTime": 0,
        "TripDuration": 536,
        "TripDistance": 0,
        "IsSuburban": False,
        "IsComponent": False,
        "CarServices": None,
        "IsSaleForbidden": False
    },
    "IsFromUkrain": False,
    "AllowedDocumentTypes": [
        "RussianPassport",
        "BirthCertificate",
        "ForeignPassport",
        "RussianForeignPassport",
        "MilitaryCard",
        "SailorPassport"
    ],
    "ClientFeeCalculation": {
        "Charge": 100.0,
        "Profit": 0.0
    },
    "AgentFeeCalculation": {
        "Charge": 100.0,
        "Profit": 0.0
    },
    "BookingSystem": "Express3"
}


def test_build_log_record():
    parser = ImToYtParser({
        'OriginCode': '333555',
        'DestinationCode': '444666',
        'DepartureDate': '2017-12-10T10:30:00',
        'TrainNumber': '003Ф',
    }, IM_RESPONSE)

    common_values = {'query__OriginCode': '333555', 'query__DestinationCode': '444666',
                     'query__DepartureDate': '2017-12-10T10:30:00', 'query__TrainNumber': '003Ф'}
    common_values.update({
        'TrainInfo__TrainNumber': '054Ч',
        'TrainInfo__TrainNumberToGetRoute': '054Ч',
        'TrainInfo__DisplayTrainNumber': '054Ч',
        'TrainInfo__TrainDescription': 'СК',
        'TrainInfo__TrainDescription__SK': True,
        'TrainInfo__DepartureDateTime': '2016-11-01T23:40:00',
        'TrainInfo__ArrivalDateTime': '2016-11-02T08:36:00',
    })
    common_values.update({
        'Car__CarType': 'Luxury',
        'Car__CarSubType': 'None',
        'Car__CarTypeName': 'СВ',
        'Car__CarNumber': '08',
        'Car__ServiceClass': '1Б',
        'Car__ServiceClassTranscript': 'Кондиционер. <br/>Персональный золотой унитаз, завтрак в постель.',
        'Car__InternationalServiceClass': '',
        'Car__CarDescription': 'У1',
        'Car__CarDescription__U': 1,
    })

    assert_that(list(parser.build_log_records()), has_items(
        has_entries(dict({'Car__FreePlace': '3', 'Car__FreePlace__StringNumber': '3', 'Car__FreePlace__Number': 3,
                          'Car__FreePlace__Prefix': '', 'Car__FreePlace__Suffix': ''}, **common_values)),
        has_entries(dict({'Car__FreePlace': '4Ж', 'Car__FreePlace__StringNumber': '4', 'Car__FreePlace__Number': 4,
                          'Car__FreePlace__Prefix': '', 'Car__FreePlace__Suffix': 'Ж'}, **common_values)),
        has_entries(dict({'Car__FreePlace': 'к5', 'Car__FreePlace__StringNumber': '5', 'Car__FreePlace__Number': 5,
                          'Car__FreePlace__Prefix': 'к', 'Car__FreePlace__Suffix': ''}, **common_values)),
    ))
