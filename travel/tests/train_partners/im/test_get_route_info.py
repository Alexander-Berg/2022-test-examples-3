# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from datetime import datetime

import pytest

from common.tester.factories import create_station
from travel.rasp.train_api.train_partners.im.base import ImError
from travel.rasp.train_api.train_partners.im.factories.utils import mock_im
from travel.rasp.train_api.train_partners.im.get_route_info import IM_TRAIN_ROUTE_METHOD, get_route_info_for_order
from travel.rasp.train_api.train_purchase.core.factories import TrainOrderFactory


TRAIN_ROUTE_RESULT = """
{
  "Routes": [
    {
      "DestintionName": "МОСКВА ОКТ",
      "Name": "ОСНОВНОЙ МАРШРУТ",
      "OriginName": "С-ПЕТЕР-ГЛ",
      "RouteStops": [
        {
          "ArrivalTime": "",
          "CityName": "С-ПЕТЕРБУР",
          "Clarification": null,
          "DepartureTime": "22:50",
          "LocalArrivalTime": "",
          "LocalDepartureTime": "22:50",
          "RouteStopType": "Departure",
          "StationCode": "2004001",
          "StationName": "С-ПЕТЕРБУР (С-ПЕТЕР-ГЛ)",
          "StopDuration": 0,
          "TimeDescription": "Local"
        },
        {
          "ArrivalTime": "23:42",
          "CityName": null,
          "Clarification": null,
          "DepartureTime": "23:43",
          "LocalArrivalTime": "23:42",
          "LocalDepartureTime": "23:43",
          "RouteStopType": "Intermediate",
          "StationCode": "2004599",
          "StationName": "ВТОРАЯ",
          "StopDuration": 1,
          "TimeDescription": "Local"
        },
        {
          "ArrivalTime": "04:32",
          "CityName": null,
          "Clarification": null,
          "DepartureTime": "04:33",
          "LocalArrivalTime": "04:32",
          "LocalDepartureTime": "04:33",
          "RouteStopType": "Intermediate",
          "StationCode": "2004600",
          "StationName": "ТВЕРЬ",
          "StopDuration": 1,
          "TimeDescription": "Local"
        },
        {
          "ArrivalTime": "23:42",
          "CityName": null,
          "Clarification": null,
          "DepartureTime": "23:43",
          "LocalArrivalTime": "23:42",
          "LocalDepartureTime": "23:43",
          "RouteStopType": "Intermediate",
          "StationCode": "2004601",
          "StationName": "ТРЕТЬЯ",
          "StopDuration": 1,
          "TimeDescription": "Local"
        },
        {
          "ArrivalTime": "05:42",
          "CityName": null,
          "Clarification": null,
          "DepartureTime": "05:43",
          "LocalArrivalTime": "05:42",
          "LocalDepartureTime": "05:43",
          "RouteStopType": "Intermediate",
          "StationCode": "2004602",
          "StationName": "ЧЕТВЕРТАЯ",
          "StopDuration": 1,
          "TimeDescription": "Local"
        },
        {
          "ArrivalTime": "06:45",
          "CityName": "МОСКВА",
          "Clarification": null,
          "DepartureTime": "",
          "LocalArrivalTime": "06:45",
          "LocalDepartureTime": "",
          "RouteStopType": "Arrival",
          "StationCode": "2006004",
          "StationName": "МОСКВА (МОСКВА ОКТ)",
          "StopDuration": 0,
          "TimeDescription": "Local"
        }
      ]
    }
  ]
}
"""


@pytest.mark.dbuser
@pytest.mark.mongouser
@pytest.mark.parametrize('departure_express_id, awaited_departure_datetime', [
    ('2004001', datetime(2018, 9, 24, 19, 50)),
    ('2004599', datetime(2018, 9, 24, 19, 50)),
    ('2004600', datetime(2018, 9, 23, 19, 50)),
    ('2004601', datetime(2018, 9, 23, 19, 50)),
    ('2004602', datetime(2018, 9, 22, 19, 50)),
    ('2004604', None),
    ('unknown_code', None),
])
def test_get_route_info(departure_express_id, awaited_departure_datetime, httpretty):
    mock_im(httpretty, IM_TRAIN_ROUTE_METHOD, body=TRAIN_ROUTE_RESULT)
    station_from = create_station(express_id=departure_express_id)

    result = get_route_info_for_order(TrainOrderFactory(
        station_from_id=station_from.id,
        departure=datetime(2018, 9, 24, 19, 50)
    ))

    assert result.first_stop.station_express_code == '2004001'
    assert result.first_stop.station_name == 'С-ПЕТЕРБУР (С-ПЕТЕР-ГЛ)'
    assert result.first_stop.departure_datetime == awaited_departure_datetime
    assert result.last_stop.station_express_code == '2006004'
    assert result.last_stop.station_name == 'МОСКВА (МОСКВА ОКТ)'
    assert result.last_stop.departure_datetime is None
    assert httpretty.last_request.parsed_body['DepartureDate'] == '2018-09-24T22:50:00'


@pytest.mark.dbuser
@pytest.mark.mongouser
def test_get_route_info_no_routes(httpretty):
    mock_im(httpretty, IM_TRAIN_ROUTE_METHOD, body='{}')
    result = get_route_info_for_order(TrainOrderFactory())
    assert result is None


@pytest.mark.dbuser
@pytest.mark.mongouser
def test_get_route_info_null_response(httpretty):
    mock_im(httpretty, IM_TRAIN_ROUTE_METHOD, body=None)
    with pytest.raises(ImError):
        get_route_info_for_order(TrainOrderFactory())
