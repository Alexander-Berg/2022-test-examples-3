# coding: utf-8
from __future__ import absolute_import, division, print_function, unicode_literals

import datetime
import mock
from decimal import Decimal

import hamcrest
import pytest

from travel.buses.connectors.tests.yabus.common.library.test_utils import converter_patch
from yabus.common import SoapClient
from yabus.noy import client

DUMMY_ORDER = {
    "Departure": {
        "Id": "217bd650-9657-11e8-699f-d00d4cbcd401",
        "Name": "Волгоград Центральный АВ г.",
    },
    "Destination": {
        "Id": "56fc165a-dd6d-11e6-811b-001c429bd626",
        "Name": "Москва Международный АВ Южные Ворота",
    },
    "OccupiedSeats": [{"Type": "Passenger", "Number": "1"}],
    "DepartureTime": datetime.datetime(2020, 8, 19, 22, 0),
    "Trip": {
        "Fares": [
            {"Caption": "Пассажирский", "Cost": Decimal("1800")},
            {"Caption": "Детский", "Cost": Decimal("900")},
            {"Caption": "Багажный", "Cost": Decimal("0")},
            {"Caption": "Скидка", "Cost": Decimal("1620")},
        ],
        "Bus": {"SeatsScheme": [{"SeatNum": 1}, {"SeatNum": 2}, {"SeatNum": 0}]},
    },
}


@pytest.fixture
def noy_client():
    with mock.patch.object(SoapClient, "__init__"), mock.patch.object(
        client, "get_secret", return_value={"noy-login": "foo", "noy-token": "bar"}
    ):
        yield client.Client()


class TestClient(object):
    def test_ride_details(self, noy_client):
        with converter_patch("noy"), mock.patch.object(
            noy_client, "call", return_value=DUMMY_ORDER
        ) as m_call:
            data = noy_client.ride_details(
                {
                    "ride_sid": mock.sentinel.ride_sid,
                    "from_sid": mock.sentinel.from_sid,
                    "to_sid": mock.sentinel.to_sid,
                }
            )

            m_call.assert_called_once_with(
                "StartSaleSession",
                [
                    mock.sentinel.ride_sid,
                    mock.sentinel.from_sid,
                    mock.sentinel.to_sid,
                    None,
                ],
            )
            hamcrest.assert_that(
                data,
                hamcrest.has_entries(
                    {
                        "ticketTypes": [
                            {
                                "code": "Пассажирский",
                                "type": {"id": 1, "name": "full"},
                                "price": 1800.0,
                                "fee": None,
                                "onlinePrice": None,
                            },
                            {
                                "code": "Детский",
                                "type": {"id": 2, "name": "child"},
                                "price": 900.0,
                                "fee": None,
                                "onlinePrice": None,
                            },
                        ],
                        "seats": [{"code": "2", "number": "2"}],
                        "from": {
                            "id": "backmap(217bd650-9657-11e8-699f-d00d4cbcd401)",
                            "desc": "Волгоград Центральный АВ г.",
                        },
                        "to": {
                            "id": "backmap(56fc165a-dd6d-11e6-811b-001c429bd626)",
                            "desc": "Москва Международный АВ Южные Ворота",
                        },
                        "departure": "2020-08-19T22:00:00",
                        "arrival": None,
                        "currency": "RUB",
                        "price": 1800.0,
                        "fee": None,
                    }
                ),
            )

    def test_book_regression(self, noy_client):
        method_result = {
            "StartSaleSession": mock.MagicMock(),
            "AddTickets": mock.MagicMock(),
            "SetTicketData": mock.MagicMock(),
            "ReserveOrder": {"SecondsToUnlockSeats": 0},
        }

        with converter_patch("noy"), mock.patch.object(
            noy_client,
            "call",
            side_effect=lambda method, _params: method_result[method],
        ):
            assert noy_client.book(
                {
                    "ride_sid": mock.sentinel.ride_sid,
                    "from_sid": mock.sentinel.from_sid,
                    "to_sid": mock.sentinel.to_sid,
                },
                passengers=mock.MagicMock(),
                pay_offline=False,
            )
