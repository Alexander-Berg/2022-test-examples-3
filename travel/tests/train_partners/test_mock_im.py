# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

import pytest
from datetime import date

from common.models.geo import Country, StationMajority
from common.models.transport import TransportType
from common.tester.factories import create_settlement, create_station
from travel.rasp.train_api.tariffs.train.factories.base import (
    create_train_tariffs_query
)
from travel.rasp.train_api.tariffs.train.factories.im import ImTrainPricingResponseFactory
from travel.rasp.train_api.train_partners.mock_im import update_mock_im_train_pricing
from travel.rasp.train_api.train_purchase.core.models import TrainPartner

pytestmark = [pytest.mark.dbuser]


def test_update_mock_im_train_pricing():
    t_train = TransportType.objects.get(pk=TransportType.TRAIN_ID)
    ekb = create_settlement(title='Екатеринбург', country=Country.RUSSIA_ID, time_zone='Asia/Yekaterinburg')
    kemerovo = create_settlement(title='Кемерово', country=Country.RUSSIA_ID, time_zone='Asia/Novokuznetsk')
    station_from = create_station(__={'codes': {'express': '2030000'}}, settlement=ekb,
                                  t_type=t_train, majority=StationMajority.MAIN_IN_CITY_ID)
    station_to = create_station(__={'codes': {'express': '2028000'}}, settlement=kemerovo,
                                t_type=t_train, majority=StationMajority.EXPRESS_FAKE_ID)
    train_query = create_train_tariffs_query(TrainPartner.IM, station_from, station_to,
                                             departure_date=date(2021, 10, 20), mock_im_path='not-empty')
    raw_response = ImTrainPricingResponseFactory(**{
        "OriginStationCode": "2030000",
        "DestinationStationCode": "2028000",
        "Trains": [{
            "TrainNumber": "118Н",
            "DepartureDateTime": "2021-10-07T22:10:00",
            "LocalDepartureDateTime": "2021-10-08T00:10:00",
            "ArrivalDateTime": "2021-10-09T01:36:00",
            "LocalArrivalDateTime": "2021-10-09T05:36:00",
        }]
    })
    result = update_mock_im_train_pricing(raw_response, train_query)
    assert result["Trains"][0]["TrainNumber"] == "118Н"
    assert result["Trains"][0]["DepartureDateTime"] == "2021-10-20T22:10:00"
    assert result["Trains"][0]["ArrivalDateTime"] == "2021-10-22T01:36:00"
    assert result["Trains"][0]["LocalDepartureDateTime"] == "2021-10-21T00:10:00"
    assert result["Trains"][0]["LocalArrivalDateTime"] == "2021-10-22T05:36:00"
