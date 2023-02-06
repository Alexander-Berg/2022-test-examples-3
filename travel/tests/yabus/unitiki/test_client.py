# coding: utf-8
from __future__ import absolute_import, division, print_function, unicode_literals

import datetime

import hamcrest
import mock
import pytest

from travel.buses.connectors.tests.yabus.common.library.test_utils import converter_patch
from travel.buses.connectors.tests.yabus.unitiki.data import RAW_RIDE, RESULT_RIDE
from yabus.unitiki import Client
from yabus.unitiki.entities import Ride


@pytest.fixture
def expect_get_calls():
    expected_calls = {}

    def side_effect(method, args, raw_content=False):
        assert method in expected_calls, "Unexpected method: {}".format(method)
        expected_args, result = expected_calls.pop(method)
        assert args == expected_args, "Unexpected args: {}".format(args)
        return result

    with mock.patch.object(Client, "get", create=True, side_effect=side_effect):
        yield lambda c: expected_calls.update(c)

    assert not expected_calls, "Expected methods are not called: {}".format(expected_calls.keys())


class TestClient(object):
    @pytest.mark.parametrize('identity_list, expected_doc_types', (
        (
            [{"card_identity_id": "1"}],
            [
                {"code": "1", "type": {"id": 1, "name": "id"}},
                {"code": "1", "type": {"id": 4, "name": "foreign passport"}},
            ]
        ),
        (
            [{"card_identity_id": "1"}, {"card_identity_id": "3"}, {"card_identity_id": "8"}],
            [
                {"code": "1", "type": {"id": 1, "name": "id"}},
                {"code": "1", "type": {"id": 4, "name": "foreign passport"}},
                {"code": "3", "type": {"id": 2, "name": "birth certificate"}},
                {"code": "8", "type": {"id": 3, "name": "passport"}}
            ]
        ),
        (
            [{"card_identity_id": "1"}, {"card_identity_id": "3"},
             {"card_identity_id": "8"}, {"card_identity_id": "9"}],
            [
                {"code": "1", "type": {"id": 1, "name": "id"}},
                {"code": "1", "type": {"id": 4, "name": "foreign passport"}},
                {"code": "3", "type": {"id": 2, "name": "birth certificate"}},
                {"code": "8", "type": {"id": 3, "name": "passport"}}
            ]
        ),
    ))
    def test_ride_details(self, expect_get_calls, identity_list, expected_doc_types):
        expect_get_calls(
            {
                "citizenship/list": ({}, {}),
                "ride": (
                    {"ride_segment_id": mock.sentinel.ride_sid, "from_cache": "0"},
                    {
                        "ride": {
                            "currency_source_id": 1,
                            "datetime_end": "2020-11-11 01:00:00",
                            "datetime_start": "2020-11-10 07:00:00",
                            "place_cnt": 6,
                            "price_source_tariff": 100,
                            "price_unitiki": 120,
                            "station_end": {
                                "city_title": "Санкт-Петербург",
                                "station_id": 8,
                                "station_title": "метро Обводный канал",
                            },
                            "station_start": {
                                "city_title": "Москва",
                                "station_id": 1,
                                "station_title": "Автостанция Центральная",
                            },
                            "ticket_refund_info": "• До отправления осталось более 2 часов – возвращается 95 %.\n• До "
                                                  "отправления осталось от 2 часов до 1 часа – возвращается 85 %.\n• "
                                                  "До отправления осталось менее 1 часа – денежные средства не возвращ"
                                                  "аются.\n• В любое время после отправления – денежные средства не "
                                                  "возвращаются.",
                        }
                    },
                ),
                "ride/position/free": ({"ride_segment_id": mock.sentinel.ride_sid}, {}),
                "ride/card_identity/list": (
                    {"ride_segment_id": mock.sentinel.ride_sid},
                    {"card_identity_list": identity_list},
                ),
                "ride/bus/scheme/place": ({"ride_segment_id": mock.sentinel.ride_sid}, {}),
            }
        )
        client = Client()

        with converter_patch("unitiki"):
            data = client.ride_details({"ride_sid": mock.sentinel.ride_sid})

            hamcrest.assert_that(
                data,
                hamcrest.has_entries(
                    {
                        "arrival": "2020-11-11T01:00:00",
                        "citizenships": [],
                        "currency": "RUB",
                        "departure": "2020-11-10T07:00:00",
                        "dischargeStops": None,
                        "docTypes": expected_doc_types,
                        "fee": 20.0,
                        "from": {
                            "desc": "Москва, Автостанция Центральная",
                            "id": "backmap(s1)",
                        },
                        "genderTypes": [
                            {"code": "0", "type": {"id": 2, "name": "female"}},
                            {"code": "1", "type": {"id": 1, "name": "male"}},
                        ],
                        "map": None,
                        "pickupStops": None,
                        "price": 120.0,
                        "refundConditions": "• До отправления осталось более 2 часов – возвращается 95 %.\n• До отправ"
                                            "ления осталось от 2 часов до 1 часа – возвращается 85 %.\n• До отправлен"
                                            "ия осталось менее 1 часа – денежные средства не возвращаются.\n• В любое"
                                            " время после отправления – денежные средства не возвращаются.",
                        "seats": None,
                        "ticketTypes": [{
                            "code": "0",
                            "type": {"id": 1, "name": "full"},
                            "price": 120.0,
                            "fee": 20.0,
                        }],
                        "to": {
                            "desc": "Санкт-Петербург, метро Обводный канал",
                            "id": "backmap(s8)",
                        },
                    }
                ),
            )

    def test_realtime_search(self, expect_get_calls):
        from_sid = 'c1'
        to_sid = 'c6'
        date = datetime.datetime(2021, 2, 18)
        expect_get_calls(
            {
                "citizenship/list": ({}, {}),
                "ride/search/result": (
                    {'search_id': mock.sentinel.search_id},
                    {
                        'ride_list': [RAW_RIDE],
                        'ride_search': {
                            'status': 1,
                            'city_id_end': 6,
                            'search_id': mock.sentinel.search_id,
                            'city_id_start': 1
                        }
                    }
                ),
            }
        )

        create_search_params = {
            'city_id_start': from_sid[1:],
            'city_id_end': to_sid[1:],
            'date': date.strftime('%Y-%m-%d')
        }
        create_search_result = {
            'ride_list': [],
            'ride_search': {
                'status': 0,
                'city_id_end': 6,
                'search_id': mock.sentinel.search_id,
                'city_id_start': 1
            }
        }
        client = Client()
        with converter_patch("unitiki"), \
                mock.patch.object(client, 'post', return_value=create_search_result), \
                mock.patch.object(Ride.carrier_matcher, 'get_carrier', return_value=None):
            data = client.realtime_search(from_sid, to_sid, date)

            client.post.assert_called_with('ride/search/request/create', create_search_params)

        hamcrest.assert_that(
            data,
            hamcrest.contains(hamcrest.has_entries(RESULT_RIDE))
        )

    def test_realtime_search_timeout(self, unitiki_converter, expect_get_calls):
        # realtime_search must return partial result when timeouted
        from_sid = 'c1'
        to_sid = 'c6'
        date = datetime.datetime(2021, 2, 18)
        expect_get_calls(
            {
                "citizenship/list": ({}, {}),
                "ride/search/result": (
                    {'search_id': mock.sentinel.search_id},
                    {
                        'ride_list': [RAW_RIDE],
                        'ride_search': {
                            'status': 0,  # not ready!
                            'city_id_end': 6,
                            'search_id': mock.sentinel.search_id,
                            'city_id_start': 1
                        }
                    }
                ),
            }
        )

        create_search_params = {
            'city_id_start': from_sid[1:],
            'city_id_end': to_sid[1:],
            'date': date.strftime('%Y-%m-%d')
        }
        post_side_effect = [
            {
                'ride_list': [],
                'ride_search': {
                    'status': 0,
                    'city_id_end': 6,
                    'search_id': mock.sentinel.search_id,
                    'city_id_start': 1
                }
            },
            {
                'ride_list': [RAW_RIDE],
                'ride_search': {
                    'status': 0,
                    'city_id_end': 6,
                    'search_id': mock.sentinel.search_id,
                    'city_id_start': 1
                }
            },
        ]

        client = Client()
        with mock.patch.object(client, 'post', side_effect=post_side_effect), \
                mock.patch.object(Ride.carrier_matcher, 'get_carrier', return_value=None):
            client.SEARCH_TIMEOUT = 3
            client.SEARCH_POLLING_INTERVAL = 2
            data = client.realtime_search(from_sid, to_sid, date)

            calls = [
                mock.call('ride/search/request/create', create_search_params),
                mock.call('ride/search/request/cancel', {'search_id': mock.sentinel.search_id})
            ]
            client.post.assert_has_calls(calls)

        hamcrest.assert_that(
            data,
            hamcrest.contains(hamcrest.has_entries(RESULT_RIDE))
        )

    def test_ticket_blank(self, expect_get_calls):
        expect_get_calls({
            "citizenship/list": ({}, {}),
            'operation': (
                {'operation_id': mock.sentinel.order_sid},
                {'operation': {'hash': mock.sentinel.operation_hash}}
            ),
            'operation/pdf': (
                {'operation_id': mock.sentinel.order_sid, 'operation_hash': mock.sentinel.operation_hash},
                'PDF_CONTENT'
            ),
        })
        blank = Client().ticket_blank({'order_sid': mock.sentinel.order_sid})

        hamcrest.assert_that(blank, hamcrest.has_properties(
            status_code=200,
            data='PDF_CONTENT',
            headers=hamcrest.has_entry('Content-Type', 'application/pdf'),
        ))
