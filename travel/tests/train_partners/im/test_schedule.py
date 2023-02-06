# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

from datetime import datetime

import pytest
from hamcrest import assert_that, contains_inanyorder

from common.utils.date import MSK_TZ
from travel.rasp.train_api.train_partners.im.factories.utils import mock_im
from travel.rasp.train_api.train_partners.im.schedule import get_schedule_im, IM_SCHEDULE_METHOD


pytestmark = [pytest.mark.mongouser, pytest.mark.dbuser]


SCHEDULE_RESPONSE = '''
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
'''


@pytest.mark.dbuser
def test_schedule(httpretty):
    mock_im(httpretty, IM_SCHEDULE_METHOD, body=SCHEDULE_RESPONSE)

    schedule = get_schedule_im('2000000', '2004000', datetime(2018, 4, 25), 12, 24, MSK_TZ)

    assert_that(schedule, schedule['start_sales_date_times'],
                contains_inanyorder(datetime(2018, 4, 25, 9, 11, 8),
                                    datetime(2018, 4, 26, 9, 11, 8)))


@pytest.mark.dbuser
def test_schedule_not_found(httpretty):
    mock_im(httpretty, IM_SCHEDULE_METHOD, status=500, body='''{
        "Code": 310,
        "Message": "В запрашиваемую дату поездов нет",
        "MessageParams": []
    }''')

    assert get_schedule_im('100500', '200300', datetime(2018, 4, 25), 12, 24, MSK_TZ) is None
