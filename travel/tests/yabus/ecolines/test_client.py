# coding: utf-8
from __future__ import absolute_import, division, print_function, unicode_literals

import mock
import pytest

import hamcrest

from travel.buses.connectors.tests. yabus.common.library.test_utils import converter_patch
from yabus.ecolines.client import Client


@pytest.fixture
def mock_client_get():
    with mock.patch.object(Client, "get", create=True) as m_get:
        yield m_get


class TestClient(object):
    def test_ride_details(self, mock_client_get):
        method_result = {
            "stops": [
                {"id": 620, "title": "Station 1"},
                {"id": 95, "title": "Station 2"},
            ],
            "legs": [
                {
                    "id": "3639323933307c3632302d3935",
                    "origin": 620,
                    "destination": 95,
                    "arrival": "2020-08-08 05:30",
                    "departure": "2020-08-07 21:55",
                }
            ],
            "fares": [
                {"tariff": 1, "amount": 62000},
                {"tariff": 10, "amount": 31000},
                {"tariff": 14, "amount": 56000},
            ],
            "seats": [
                {"id": "1", "row": 1, "column": 1, "busy": True},
                {"id": "2", "row": 1, "column": 2, "busy": False},
            ],
        }
        mock_client_get.side_effect = lambda method, params: method_result[method]
        client = Client()

        with converter_patch("ecolines"):
            data = client.ride_details({"ride_sid": mock.sentinel.ride_sid})

            hamcrest.assert_that(
                data,
                hamcrest.has_entries(
                    {
                        "ticketTypes": [
                            {
                                "code": "1",
                                "type": {"id": 1, "name": "full"},
                                "price": 620.0,
                                "fee": None,
                                "onlinePrice": None,
                            },
                            {
                                "code": "10",
                                "type": {"id": 2, "name": "child"},
                                "price": 310.0,
                                "fee": None,
                                "onlinePrice": None,
                            },
                        ],
                        "seats": [{"code": "2", "number": "2"}],
                        "map": [
                            {
                                "status": {"id": 1, "name": "occupied"},
                                "number": "1",
                                "y": 1,
                                "x": 1,
                                "type": {"id": 1, "name": "seat"},
                            },
                            {
                                "status": {"id": 0, "name": "free"},
                                "number": "2",
                                "y": 1,
                                "x": 2,
                                "type": {"id": 1, "name": "seat"},
                            },
                        ],
                        "from": {"id": "backmap(620)", "desc": "Station 1", "supplier_id": "620"},
                        "to": {"id": "backmap(95)", "desc": "Station 2", "supplier_id": "95"},
                        "departure": "2020-08-07T21:55:00",
                        "arrival": "2020-08-08T05:30:00",
                        "currency": "RUB",
                        "price": 620.0,
                        "fee": None,
                    }
                ),
            )
