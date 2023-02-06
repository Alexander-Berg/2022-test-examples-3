# coding: utf-8
from __future__ import absolute_import, division, print_function, unicode_literals

import hamcrest
import mock
import pytest
from datetime import date

from travel.buses.connectors.tests.yabus.common.library.test_utils import matching_provider_patch
from travel.proto.dicts.buses.register_type_pb2 import TRegisterType
from travel.proto.dicts.buses.supplier_pb2 import TSupplier

from yabus.busfor import defaults
from yabus.busfor.client import Client
from yabus.busfor.entities import Ride
from yabus.common.exceptions import InvalidRide
from yabus.providers import register_type_provider


@pytest.fixture
def mock_client_get():
    with mock.patch.object(Client, "get", create=True) as mocked_get:
        yield mocked_get


@pytest.fixture
def expect_get_calls(mock_client_get):
    expected_calls = {}

    def side_effect(method, params, **_):
        assert method in expected_calls, "Unexpected method: {}".format(method)
        expected_params, result = expected_calls.pop(method)
        assert params == expected_params, "Unexpected params: {}".format(params)
        return result

    mock_client_get.side_effect = side_effect

    yield lambda c: expected_calls.update(c)
    assert not expected_calls, "Expected methods are not called: {}".format(expected_calls.keys())


class TestClient(object):
    def test_endpoints(self, expect_get_calls):
        expect_get_calls(
            {
                "geo/v2/locations": (
                    {"limit": defaults.LOCATIONS_LIMIT},
                    {
                        "pages_info": {"page_count": 2, "page_uuid": "locations_uuid"},
                        "dictionaries": {"ru": {"location_subtypes": [{"id": 6, "short_name": "обл."}]}},
                        "locations": [
                            {
                                "default_name": "City without translation",
                                "id": 1,
                                "latitude": 1.2345,
                                "longitude": 6.7890,
                                "parent_id": 2,
                                "subtype_id": 0,
                                "timezone": "Europe/Moscow",
                                "translations": [],
                            },
                            {
                                "default_name": "City with translation and region",
                                "id": 2,
                                "latitude": 1.2345,
                                "longitude": 6.7890,
                                "parent_id": 2,
                                "subtype_id": 0,
                                "timezone": "Europe/Moscow",
                                "translations": [
                                    {
                                        "lang": "ru",
                                        "name": "Город с переводом названия",
                                        "region_name": "Область",
                                        "country_name": "Россия",
                                    }
                                ],
                            },
                        ],
                        "parent_locations": [
                            {
                                "id": 1,
                                "type_id": 1,
                                "subtype_id": 0,
                                "default_name": "Russia",
                                "location_data": [{"id": 3, "value": "RU"}],
                                "translations": [{"lang": "ru", "name": "Россия"}],
                            },
                            {
                                "id": 2,
                                "type_id": 2,
                                "subtype_id": 6,
                                "parent_id": 1,
                                "default_name": "Some region",
                                "translations": [{"lang": "ru", "name": "Свердловская"}],
                            },
                        ],
                    },
                ),
                "geo/v2/locations/page": (
                    {"page_uuid": "locations_uuid", "number_page": 2},
                    {
                        "locations": [
                            {
                                "default_name": "City without parents",
                                "id": 3,
                                "latitude": 1.2345,
                                "longitude": 6.7890,
                                "parent_id": None,
                                "subtype_id": 0,
                                "timezone": "Europe/Moscow",
                                "translations": [],
                            },
                        ],
                        "parent_locations": [],
                    },
                ),
                "geo/v2/points": (
                    {"limit": defaults.POINTS_LIMIT},
                    {
                        "pages_info": {"page_count": 2, "page_uuid": "points_uuid"},
                        "points": [
                            {
                                "default_name": "Station without translation",
                                "default_address": "Address",
                                "id": 1,
                                "latitude": 1.2345,
                                "longitude": 6.7890,
                                "location_id": 99,
                                "translations": [],
                            },
                        ],
                    },
                ),
                "geo/v2/points/page": (
                    {"page_uuid": "points_uuid", "number_page": 2},
                    {
                        "points": [
                            {
                                "default_name": "Station with translation",
                                "default_address": "Address",
                                "id": 2,
                                "latitude": 1.2345,
                                "longitude": 6.7890,
                                "location_id": 99,
                                "translations": [
                                    {"address": "Адрес", "lang": "ru", "name": "Станция с переводом названия"}
                                ],
                            },
                        ]
                    },
                ),
            }
        )

        client = Client()
        hamcrest.assert_that(
            client.endpoints(),
            hamcrest.contains_inanyorder(
                {
                    "supplier_id": "L1",
                    "type": "city",
                    "title": "City without translation",
                    "latitude": 1.2345,
                    "longitude": 6.7890,
                    "country": "Россия",
                    "country_code": "RU",
                    "city_id": None,
                    "region": "Свердловская обл.",
                    "region_code": None,
                    "timezone_info": "Europe/Moscow",
                    "extra_info": None,
                    "description": "Свердловская обл., Россия",
                    "city_title": None,
                    "district": None,
                },
                {
                    "supplier_id": "L2",
                    "type": "city",
                    "title": "Город с переводом названия",
                    "latitude": 1.2345,
                    "longitude": 6.7890,
                    "country": "Россия",
                    "country_code": "RU",
                    "city_id": None,
                    "region": "Свердловская обл.",
                    "region_code": None,
                    "timezone_info": "Europe/Moscow",
                    "extra_info": None,
                    "description": "Свердловская обл., Россия",
                    "city_title": None,
                    "district": None,
                },
                {
                    "supplier_id": "L3",
                    "type": "city",
                    "title": "City without parents",
                    "latitude": 1.2345,
                    "longitude": 6.7890,
                    "country": None,
                    "country_code": None,
                    "city_id": None,
                    "region": None,
                    "region_code": None,
                    "timezone_info": "Europe/Moscow",
                    "extra_info": None,
                    "description": "",
                    "city_title": None,
                    "district": None,
                },
                {
                    "supplier_id": "P1",
                    "type": "station",
                    "title": "Station without translation",
                    "latitude": 1.2345,
                    "longitude": 6.7890,
                    "country": None,
                    "country_code": None,
                    "city_id": "99",
                    "region": None,
                    "region_code": None,
                    "timezone_info": None,
                    "extra_info": None,
                    "description": "Address",
                    "city_title": None,
                    "district": None,
                },
                {
                    "supplier_id": "P2",
                    "type": "station",
                    "title": "Станция с переводом названия",
                    "latitude": 1.2345,
                    "longitude": 6.7890,
                    "country": None,
                    "country_code": None,
                    "city_id": "99",
                    "region": None,
                    "region_code": None,
                    "timezone_info": None,
                    "extra_info": None,
                    "description": "Адрес",
                    "city_title": None,
                    "district": None,
                },
            ),
        )

    def test_segments(self, mock_client_get, busfor_converter):
        raw_segments = {
            "pairs": [
                {"location_id_from": 100, "location_id_to": [110, 120, 130]},
                {"location_id_from": 200, "location_id_to": [100]},
                {"location_id_from": 300, "location_id_to": [350, 100, 200]},
            ]
        }

        expected = [
            ("relations_backmap(L100)", "relations_backmap(L110)"),
            ("relations_backmap(L100)", "relations_backmap(L120)"),
            ("relations_backmap(L100)", "relations_backmap(L130)"),
            ("relations_backmap(L200)", "relations_backmap(L100)"),
            ("relations_backmap(L300)", "relations_backmap(L350)"),
            ("relations_backmap(L300)", "relations_backmap(L100)"),
            ("relations_backmap(L300)", "relations_backmap(L200)"),
        ]

        mock_client_get.return_value = raw_segments

        busfor = Client()
        segments = busfor.segments()

        assert len(segments) == len(expected)
        hamcrest.assert_that(segments, hamcrest.contains_inanyorder(*expected)),
        mock_client_get.assert_called_once_with("search/v2/pairs")

    def test_ride_details(self, expect_get_calls, busfor_converter):
        expect_get_calls(
            {
                "search/v2/trips/trip_id": (
                    {"date": "trip_date"},
                    {
                        "points": [
                            {
                                "id": "1407171",
                                "name": 'Остановка "МакДональдс" (стоянка)',
                                "address": "метро Дарница; проспект Броварской; дом 27",
                            },
                            {
                                "id": "1136731",
                                "name": 'Железнодорожный вокзал "Одесса-Главная"',
                                "address": "Привокзальная площадь; дом 2",
                            },
                        ],
                        "segments": [
                            {
                                "price": {
                                    "currency": "RUB",
                                    "currency_code": "643",
                                    "tariffs": [{"code": "Y", "cost": 79066}],
                                    "total": 79066,
                                },
                                "arrivalDateTime": "2020-12-10 12:25",
                                "arrival_point_id": "1136731",
                                "departureDateTime": "2020-12-10 06:20",
                                "departure_point_id": "1407171",
                            }
                        ],
                    },
                ),
                "search/v2/trips/trip_id/seats": (
                    {"date": "trip_date"},
                    {
                        "maps_seat": [
                            {
                                "map_seat": [
                                    {
                                        "id": "11543027485",
                                        "num": "",
                                        "seat_type": 0,
                                        "status": 0,
                                        "x": 0,
                                        "y": 0,
                                        "z": 0,
                                    },
                                    {
                                        "id": "11543027489",
                                        "num": "В1",
                                        "seat_type": 2,
                                        "status": 0,
                                        "x": 1,
                                        "y": 0,
                                        "z": 0,
                                    },
                                    {
                                        "id": "11543027445",
                                        "num": "5С",
                                        "seat_type": 1,
                                        "status": 1,
                                        "x": 2,
                                        "y": 0,
                                        "z": 0,
                                    },
                                ]
                            },
                        ]
                    },
                ),
            }
        )

        hamcrest.assert_that(
            Client().ride_details({"ride_sid": "trip_id", "ride_date": "trip_date"}),
            hamcrest.has_entries(
                {
                    "docTypes": hamcrest.instance_of(list),
                    "genderTypes": hamcrest.instance_of(list),
                    "ticketTypes": hamcrest.contains(
                        hamcrest.has_entries({"code": "Y", "price": 790.66, "type": {"id": 1, "name": "full"}}),
                    ),
                    "citizenships": hamcrest.instance_of(list),
                    "seats": [
                        {"code": "11543027445", "number": u"5С"},
                    ],
                    "map": [
                        {"type": {"id": 2, "name": "passage"}, "status": None, "number": "", "x": 0, "y": 0},
                        {"type": {"id": 0, "name": "driver"}, "status": None, "number": u'В1', "x": 1, "y": 0},
                        {
                            "type": {"id": 1, "name": "seat"},
                            "status": {"id": 0, "name": "free"},
                            "number": u'5С',
                            "x": 2,
                            "y": 0,
                        },
                    ],
                    "departure": "2020-12-10T06:20:00",
                    "arrival": "2020-12-10T12:25:00",
                    "from": {
                        "id": "backmap(P1407171)",
                        "desc": 'Остановка "МакДональдс" (стоянка), метро Дарница; проспект Броварской; дом 27',
                    },
                    "to": {
                        "id": "backmap(P1136731)",
                        "desc": 'Железнодорожный вокзал "Одесса-Главная", Привокзальная площадь; дом 2',
                    },
                }
            ),
        )

    @pytest.mark.parametrize("segments_count, maps_count", ((2, 1), (1, 2), (0, 1)))
    def test_ride_details_invalid(self, expect_get_calls, segments_count, maps_count):
        expect_get_calls(
            {
                "search/v2/trips/trip_id": ({"date": "trip_date"}, {"segments": [mock.MagicMock()] * segments_count},),
                "search/v2/trips/trip_id/seats": (
                    {"date": "trip_date"},
                    {"maps_seat": [mock.MagicMock()] * maps_count},
                ),
            }
        )

        with pytest.raises(InvalidRide):
            Client().ride_details({"ride_sid": "trip_id", "ride_date": "trip_date"}),

    def test_search(self, expect_get_calls, busfor_supplier_provider):
        # from c213 = [L1, P100]
        # to s200 = [P50], parents = [L2]

        map_side_effect = [{'L1', 'P100'}, {'s200'}]
        parent_side_effect = [set(), {'L2'}]
        backmap_side_effect = ['s200', 's200']

        from_id = 'c213'
        to_id = 's200'
        supp_from = '1'
        supp_to = '2'
        test_date = date(2020, 12, 10)

        params = {
            "limit": defaults.SEARCH_LIMIT,
            'search_mode': 'direct',
            'date': test_date.strftime('%Y-%m-%d'),
            'from_id': supp_from,
            'to_id': supp_to,
        }

        supplier = TSupplier(
            actual_address="117638, г.Москва, ул.Одесская, д.2, корп.А, этаж 9",
            code="busfor",
            id=80,
            legal_address="117638, г.Москва, ул.Одесская, д.2, корп.А, этаж 9",
            legal_name="ООО «БАСФОР»",
            name="ООО «БАСФОР»",
            register_number="111746469433",
            register_type_id=1,
            taxation_number="7705952996",
        )

        register_type = TRegisterType(code="ОГРН", id=1, title="Основной государственный регистрационный номер")

        expect_get_calls(
            {
                "search/v2/trips": (
                    params,
                    {
                        "carriers": [
                            {
                                "egrul_egis": "",
                                "id": "6AD13CCDBABA48D0E0530300F00A7952_791987477",
                                "inn": "5130237973",
                                "legal_address": "г Краков, Малопольское, Краков, ул Sw.Filipa, д 23/4",
                                "name": "Ideal Travel Mariya Mirna/PL \"Идеал Тревел Мария Мирна\"",
                                "physical_address": "г Краков, Малопольское, Краков, ул Sw.Filipa, д 23/4",
                            }
                        ],
                        "locations": [
                            {
                                "id": "32795",
                                "name": "Кошалин",
                            },
                            {
                                "id": "1429",
                                "name": "Новоград-Волынский",
                            }
                        ],
                        "pages_info": {
                            "expires_in": "2020-12-09T06:45:38.363Z",
                            "page_count": 1,
                            "page_number": 1,
                            "page_uuid": "FE753684B2F49529E3CCE850C5990CC0"
                        },
                        "points": [
                            {
                                "address": "улица Шевченко; дом 45",
                                "id": "102400",
                                "name": "Автовокзал Новоград-Волынский",
                                "parent_id": "1429"
                            },
                            {
                                "address": "улица Звичества; дом 1",
                                "id": "81160",
                                "name": "Автовокзал \"Кошалин\"",
                                "parent_id": "32795"
                            }
                        ],
                        "segments": [
                            {
                                "arrivalDateTime": "2020-12-11 08:40",
                                "arrival_point_id": "102400",
                                "bus_options": [
                                    {
                                        "option_id": "2",
                                        "option_value": "Климат контроль"
                                    },
                                    {
                                        "option_id": "8",
                                        "option_value": "Wi-fi"
                                    },
                                    {
                                        "option_id": "17",
                                        "option_value": "Индивидуальное освещение для каждого пассажира"
                                    }
                                ],
                                "can_discount": True,
                                "cancel_time": 25,
                                "carrier_id": "6AD13CCDBABA48D0E0530300F00A7952_791987477",
                                "departureDateTime": "2020-12-10 07:30",
                                "departure_point_id": "81160",
                                "free_booking_enable": False,
                                "free_seats": 19,
                                "has_hub": False,
                                "id": "QlNGdGVzdGFnZW50fjMyNzk1fjE0Mjl-MTsxMH5-MH4zMzM2RUVBMzJDRTYzODM4MkYzQzY0Nzg1RD"
                                      "A1MkQ5Qg",
                                "is_back_trip": False,
                                "is_regular": True,
                                "location_id_from": "32795",
                                "location_id_to": "1429",
                                "name": None,
                                "number": "MD_TEST - IDT7222 Колобжег - Белгород-Днестровский",
                                "platform": "",
                                "price": {
                                    "currency": "RUB",
                                    "currency_code": "643",
                                    "netto": 0,
                                    "tariffs": [
                                        {
                                            "class": None,
                                            "code": "Y",
                                            "commissions": [
                                                {
                                                    "commission_name": "service fee",
                                                    "commission_value": 0
                                                }
                                            ],
                                            "cost": 382294,
                                            "description_tariff": "Базовый тариф стоимости проезда одного пассажира в "
                                                                  "одном направлении\nСтоимость перевозки багажа более "
                                                                  "одного места составляет 8 pln",
                                            "end_date": None,
                                            "free_booking": False,
                                            "id": None,
                                            "is_exclusive_price": False,
                                            "name": "Базовый тариф",
                                            "note": "",
                                            "refund_conditions": [
                                                {
                                                    "condition_description": "Срыв рейса Возвращается 100% стоимости "
                                                                             "тарифа. Ориентировочная сумма возврата "
                                                                             "3822.94",
                                                },
                                                {
                                                    "condition_description": "Добровольно, более чем за 3 д до отправле"
                                                                             "ния рейса. Удерживается при возврате: 10%"
                                                                             " тарифа. Ориентировочная сумма возврата "
                                                                             "2735.31",
                                                },
                                                {
                                                    "condition_description": "Добровольно, более чем за 24 ч и менее "
                                                                             "чем за 72 ч до отправления рейса. Удержи"
                                                                             "вается при возврате: 50% тарифа. Ориенти"
                                                                             "ровочная сумма возврата 1206.13",
                                                },
                                                {
                                                    "condition_description": "Принудительно, при отмене рейса. Возвращ"
                                                                             "ается 100% стоимости билета. Ориентировоч"
                                                                             "ная сумма возврата 3822.94",
                                                }
                                            ],
                                            "start_date": None
                                        }
                                    ],
                                    "total": 382294
                                },
                                "redirect_url": "",
                                "reservation_enable": False,
                                "reservation_lifetime": 25,
                                "resource_id": "0",
                                "return_enable": True,
                                "route": [
                                    {
                                        "arrivalDateTime": "2020-12-10 07:20",
                                        "departureDateTime": "2020-12-10 07:30",
                                        "distance": 50,
                                        "is_hub": False,
                                        "point_id": "81160",
                                    },
                                    {
                                        "arrivalDateTime": "2020-12-11 08:40",
                                        "departureDateTime": "2020-12-11 08:45",
                                        "distance": 4200,
                                        "is_hub": False,
                                        "point_id": "102400",
                                    }
                                ],
                                "sale_enable": True,
                                "stop_reservation_time": None,
                                "travel_time": "24:10",
                                "trip_options": [],
                                "trip_type": "international",
                                "vehicle_id": "U0NBTklBIENlbnR1cnktMTcwNzQ5ODQ5Mzk5MDU4MzE1OQ=="
                            }
                        ],
                        "trips": [
                            {
                                "class": "direct",
                                "direct_trip": [
                                    "QlNGdGVzdGFnZW50fjMyNzk1fjE0Mjl-MTsxMH5-MH4zMzM2RUVBMzJDRTYzODM4MkYzQzY0Nzg1RDA1MkQ5Qg"
                                ],
                                "rate": {},
                                "tripSeatsMap": True,
                                "trip_id": "QlNGdGVzdGFnZW50fjMyNzk1fjE0Mjl-MTsxMH5-MH4zMzM2RUVBMzJDRTYzODM4MkYzQzY0Nzg1RDA1MkQ5Qg"
                            }
                        ],
                        "vehicle_options": [],
                        "vehicles": [
                            {
                                "capacity": 49,
                                "id": "U0NBTklBIENlbnR1cnktMTcwNzQ5ODQ5Mzk5MDU4MzE1OQ==",
                                "model": "SCANIA Century",
                                "number": None,
                                "vehicle_options": []
                            }
                        ]
                    }
                )
            }
        )

        expected_result = {
            "@id": "eyJyaWRlX2RhdGUiOiAiMjAyMC0xMi0xMCIsICJyaWRlX3NpZCI6ICJRbE5HZEdWemRHRm5aVzUwZmpNeU56azFmakUwTWpsLU1"
                   "Uc3hNSDUtTUg0ek16TTJSVVZCTXpKRFJUWXpPRE00TWtZelF6WTBOemcxUkRBMU1rUTVRZyJ9",
            "arrival": "2020-12-11T08:40:00",
            "benefits": [
                {"id": 7, "name": "conditioner"},
                {"id": 4, "name": "wi-fi"}
            ],
            "bookFields": [
                "name",
                "gender",
                "birthDate",
                "phone",
                "document",
                "email"
            ],
            "bookOnly": False,
            "bus": "SCANIA Century",
            "canPayOffline": False,
            "carrier": "Ideal Travel Mariya Mirna/PL \"Идеал Тревел Мария Мирна\"",
            "carrierID": "6AD13CCDBABA48D0E0530300F00A7952_791987477",
            "carrierModel": {
                "registerNumber": None,
                "name": "Ideal Travel Mariya Mirna/PL \"Идеал Тревел Мария Мирна\"",
                "actualAddress": "г Краков, Малопольское, Краков, ул Sw.Filipa, д 23/4",
                "inn": "5130237973",
                "legalName": "Ideal Travel Mariya Mirna/PL \"Идеал Тревел Мария Мирна\"",
                "legalAddress": "г Краков, Малопольское, Краков, ул Sw.Filipa, д 23/4",
                "timetable": "пн-пт: 10:00–18:00",
            },
            "connector": "busfor",
            "currency": "RUB",
            "departure": "2020-12-10T07:30:00",
            "fee": 0,
            "freeSeats": 19,
            "from": {
                "desc": "Автовокзал \"Кошалин\", улица Звичества; дом 1",
                "id": "s200",
                "supplier_id": "P81160",
            },
            "name": None,
            "number": "MD_TEST - IDT7222 Колобжег - Белгород-Днестровский",
            "onlinePrice": None,
            "onlineRefund": True,
            "partner": "busfor",
            "partnerEmail": None,
            "partnerName": "Busfor",
            "partnerPhone": None,
            "price": 3822.94,
            "refundConditions": "Срыв рейса Возвращается 100% стоимости тарифа.\nДобровольно, более чем за 3 д до "
                                "отправления рейса. Удерживается при возврате: 10% тарифа.\nДобровольно, более чем за"
                                " 24 ч и менее чем за 72 ч до отправления рейса. Удерживается при возврате: 50% тарифа."
                                "\nПринудительно, при отмене рейса. Возвращается 100% стоимости билета.",
            "status": {
                "id": 1,
                "name": "sale"
            },
            "supplierModel": {
                "actualAddress": "117638, г.Москва, ул.Одесская, д.2, корп.А, этаж 9",
                "code": "busfor",
                "id": 80,
                "legalAddress": "117638, г.Москва, ул.Одесская, д.2, корп.А, этаж 9",
                "legalName": "ООО «БАСФОР»",
                "name": "ООО «БАСФОР»",
                "registerNumber": "111746469433",
                "registerTypeId": 1,
                "registerType": {
                    "code": "ОГРН",
                    "id": 1,
                    "title": "Основной государственный регистрационный номер"
                },
                "taxationNumber": "7705952996",
            },
            "ticketLimit": 5,
            "to": {
                "desc": "Автовокзал Новоград-Волынский, улица Шевченко; дом 45",
                "id": "s200",
                "supplier_id": "P102400",
            }
        }

        busfor = Client()
        with mock.patch.object(busfor.converter, 'map', side_effect=map_side_effect, create=True), \
             mock.patch.object(busfor.converter, 'map_to_parents', side_effect=parent_side_effect,
                               create=True), \
             mock.patch.object(busfor.converter, 'backmap', side_effect=backmap_side_effect, create=True), \
             mock.patch.object(Ride.supplier_provider, 'get_by_code', return_value=supplier), \
             mock.patch.object(register_type_provider, 'get_by_id', return_value=register_type), \
             mock.patch.object(Ride.carrier_matcher, 'get_carrier', return_value=None), \
             matching_provider_patch({}):
            rides = busfor.search(from_id, to_id, test_date)
        hamcrest.assert_that(
            rides,
            hamcrest.contains(hamcrest.has_entries(expected_result))
        )
