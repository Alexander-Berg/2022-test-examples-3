# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

from datetime import datetime

import pytest
from hamcrest import has_entry, contains, assert_that, has_entries

from common.models.geo import Country
from common.models.transport import TransportType
from common.tester.factories import create_station, create_settlement
from travel.rasp.train_api.train_partners.im.base import IM_DATETIME_FORMAT
from travel.rasp.train_api.train_partners.im.factories.utils import mock_im
from travel.rasp.train_api.train_partners.im.schedule import IM_SCHEDULE_METHOD


SCHEDULE_RESPONSE = """
{
  "OriginStationCode": "2000000",
  "DestinationStationCode": "2004000",
  "RoutePolicy": "Internal",
  "Schedules": [
    {
      "TrainNumber": "054Ч",
      "TrainNumberToGetRoute": "054Ч",
      "TrainName": "ГРАНД",
      "OriginName": "МОСКВА ОКТ",
      "OriginStationCode": "2000000",
      "DestinationName": "С-ПЕТЕР-ГЛ",
      "DestinationStationCode": "2004000",
      "DepartureTime": "23:40",
      "DepartureStopTime": 0,
      "ArrivalTime": "08:36",
      "ArrivalStopTime": 0,
      "TripDuration": 536,
      "TripDistance": 0,
      "Regularity": "по 10.12 еж",
      "StartSalesDateTime": null
    },
    {
      "TrainNumber": "056Ч",
      "TrainNumberToGetRoute": "056Ч",
      "TrainName": "ГРАНД",
      "OriginName": "МОСКВА ОКТ",
      "OriginStationCode": "2000000",
      "DestinationName": "С-ПЕТЕР-ГЛ",
      "DestinationStationCode": "2004000",
      "DepartureTime": "23:40",
      "DepartureStopTime": 0,
      "ArrivalTime": "08:36",
      "ArrivalStopTime": 0,
      "TripDuration": 536,
      "TripDistance": 0,
      "Regularity": "по 10.12 еж",
      "StartSalesDateTime": "2018-04-25T09:11:08"
    },
    {
      "StartSalesDateTime": "2018-04-26T09:11:08"
    }
  ],
  "StationClarifying": null,
  "OriginTimeZoneDifference": 0,
  "DestinationTimeZoneDifference": 0,
  "NotAllTrainsReturned": false
}
"""


@pytest.mark.dbuser
@pytest.mark.mongouser
def test_im_with_stations(httpretty, async_urlconf_client):
    station_from = create_station(country=Country.RUSSIA_ID, __=dict(codes={'express': '2000000'}))
    station_to = create_station(country=Country.RUSSIA_ID, __=dict(codes={'express': '2004000'}))

    mock_im(httpretty, IM_SCHEDULE_METHOD, body=SCHEDULE_RESPONSE)

    response = async_urlconf_client.get('/ru/api/partner-schedule/',
                                        {'pointFrom': station_from.point_key,
                                         'pointTo': station_to.point_key,
                                         'when': '2018-04-25T00:00:00',
                                         'timeFrom': 0,
                                         'timeTo': 24})

    assert response.status_code == 200
    assert_that(response.data, has_entry("schedule", has_entry("startSalesDateTimes",
                                                               contains("2018-04-25T06:11:08+00:00",
                                                                        "2018-04-26T06:11:08+00:00"))))
    assert_that(httpretty.last_request.parsed_body, has_entries({
        'Origin': '2000000',
        'Destination': '2004000',
        'DepartureDate': datetime(2018, 4, 25).strftime(IM_DATETIME_FORMAT),
        'TimeFrom': 0,
        'TimeTo': 24
    }))


@pytest.mark.dbuser
@pytest.mark.mongouser
def test_im_with_settlements(httpretty, async_urlconf_client):
    settlement_from = create_settlement(country=Country.RUSSIA_ID)
    settlement_to = create_settlement(country=Country.RUSSIA_ID)

    t_type = TransportType.objects.get(id=TransportType.TRAIN_ID)
    create_station(country=Country.RUSSIA_ID, settlement=settlement_from, t_type=t_type,
                   __=dict(codes={'express': '2000000'}))
    create_station(country=Country.RUSSIA_ID, settlement=settlement_to, t_type=t_type,
                   __=dict(codes={'express': '2004000'}))

    mock_im(httpretty, IM_SCHEDULE_METHOD, body=SCHEDULE_RESPONSE)

    response = async_urlconf_client.get('/ru/api/partner-schedule/',
                                        {'pointFrom': settlement_from.point_key,
                                         'pointTo': settlement_to.point_key,
                                         'when': '2018-04-25T00:00:00',
                                         'timeFrom': 0,
                                         'timeTo': 24})

    assert response.status_code == 200
    assert_that(response.data, has_entry("schedule", has_entry("startSalesDateTimes",
                                                               contains("2018-04-25T06:11:08+00:00",
                                                                        "2018-04-26T06:11:08+00:00"))))
    assert_that(httpretty.last_request.parsed_body, has_entries({
        'Origin': '2000000',
        'Destination': '2004000',
        'DepartureDate': datetime(2018, 4, 25).strftime(IM_DATETIME_FORMAT),
        'TimeFrom': 0,
        'TimeTo': 24
    }))


@pytest.mark.dbuser
@pytest.mark.mongouser
def test_im_error_with_settlements_without_codes(httpretty, async_urlconf_client):
    settlement_from = create_settlement(country=Country.RUSSIA_ID)
    settlement_to = create_settlement(country=Country.RUSSIA_ID)

    mock_im(httpretty, IM_SCHEDULE_METHOD, body=SCHEDULE_RESPONSE)

    response = async_urlconf_client.get('/ru/api/partner-schedule/',
                                        {'pointFrom': settlement_from.point_key,
                                         'pointTo': settlement_to.point_key,
                                         'when': '2018-04-25T00:00:00',
                                         'timeFrom': 0,
                                         'timeTo': 24})

    assert response.status_code == 400
    assert not httpretty.latest_requests


@pytest.mark.dbuser
@pytest.mark.mongouser
def test_im_error_trains_not_found(httpretty, async_urlconf_client):
    station_from = create_station(country=Country.RUSSIA_ID, __=dict(codes={'express': '2000000'}))
    station_to = create_station(country=Country.RUSSIA_ID, __=dict(codes={'express': '2004000'}))

    mock_im(httpretty, IM_SCHEDULE_METHOD, status=500, body='''{
        "Code": 310,
        "Message": "В запрашиваемую дату поездов нет",
        "MessageParams": []
    }''')

    response = async_urlconf_client.get('/ru/api/partner-schedule/',
                                        {'pointFrom': station_from.point_key,
                                         'pointTo': station_to.point_key,
                                         'when': '2018-04-25T00:00:00',
                                         'timeFrom': 0,
                                         'timeTo': 24})

    assert response.status_code == 200
    assert response.data == {'schedule': {}}
