# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

import json
import pytest
from datetime import time
from django.conf import settings
from django.test import Client

from common.apps.train_order.enums import CoachType
from common.models.geo import Country, Settlement, StationMajority
from common.models.transport import TransportType
from common.tester.factories import (
    create_settlement, create_station, create_thread, create_currency
)
from common.tester.utils.datetime import replace_now
from common.tester.utils.replace_setting import replace_dynamic_setting, replace_setting
from common.utils.date import MSK_TIMEZONE
from common.utils.title_generator import build_default_title_common
from travel.rasp.train_api.tariffs.train.factories.base import create_www_setting_cache_timeouts
from travel.rasp.train_api.tariffs.train.factories.im import ImTrainPricingResponseFactory

pytestmark = [pytest.mark.dbuser, pytest.mark.mongouser]


def _create_im_response_msk_spb():
    return ImTrainPricingResponseFactory(**{
        'Trains': [{
            'DepartureDateTime': '2017-09-30T17:26:00',
            'ArrivalDateTime': '2017-10-01T02:09:00',
            'TrainNumber': '059А',
            'TrainNumberToGetRoute': '059',
            'DisplayTrainNumber': '058*А',
            'CarGroups': [
                {
                    'CarType': CoachType.PLATZKARTE,
                    'MaxPrice': 1929.2,
                    'MinPrice': 1212.8,
                    'TotalPlaceQuantity': 138,
                },
                {
                    'CarType': CoachType.COMPARTMENT,
                    'MaxPrice': 3708.0,
                    'MinPrice': 3075.9,
                    'TotalPlaceQuantity': 34,
                }
            ],
            'TrainDescription': 'СК ФИРМ',
            'TrainName': 'Волга',
            'OriginName': 'С-ПЕТЕР-ГЛ',
            'DestinationName': 'Н.НОВГОРОД М',
            'DestinationNames': ['Н.НОВГОРОД М'],
            'Provider': 'P1',
        }]
    })


@replace_now('2021-09-29')
@replace_dynamic_setting('TRAIN_PURCHASE_FEATURE_REASON_FOR_MISSING_PRICES', True)
@replace_setting('ENABLE_MOCK_IM', True)
@pytest.mark.usefixtures('worker_cache_stub')
def test_init_and_poll_query_im_to_msk(httpretty, worker_stub):
    mock_im_path = 'travel/mock-im/train-pricing1'
    response_body = json.dumps(_create_im_response_msk_spb())
    httpretty.register_uri(httpretty.GET, '{url}v1/cat?node={path}'.format(url=settings.BUNKER_URL, path=mock_im_path),
                           body=response_body)
    create_www_setting_cache_timeouts()
    create_currency(code='RUR', iso_code='RUB')

    t_train = TransportType.objects.get(pk=TransportType.TRAIN_ID)
    piter = create_settlement(title='Питер', country=Country.RUSSIA_ID, time_zone=MSK_TIMEZONE)
    create_station(__={'codes': {'express': '2000000'}}, settlement=Settlement.MOSCOW_ID,
                   t_type=t_train, majority=StationMajority.EXPRESS_FAKE_ID)
    station_to = create_station(__={'codes': {'express': '2000001'}}, settlement=Settlement.MOSCOW_ID,
                                t_type=t_train, majority=StationMajority.MAIN_IN_CITY_ID, id=2000001)
    station_from = create_station(__={'codes': {'express': '2004001'}}, settlement=piter,
                                  t_type=t_train, majority=StationMajority.MAIN_IN_CITY_ID)
    create_thread(
        number='059А',
        t_type=t_train,
        tz_start_time=time(17, 26),
        schedule_v1=[
            [None, 0, station_from],
            [300, None, station_to],
        ],
        __={'calculate_noderoute': True},
        title_common=build_default_title_common(t_train, [station_from, station_to]),
        title='Питер - Москва'
    )
    params = {
        'pointFrom': 'c{}'.format(piter.id),
        'pointTo': 'c{}'.format(Settlement.MOSCOW_ID),
        'startTime': '2021-09-30T10:00:00+03:00',
        'endTime': '2021-09-30T20:00:00+03:00',
        'partner': 'im',
        'national_version': 'ru',
        'mockImPath': mock_im_path,
    }
    response = Client().get('/ru/api/segments/train-tariffs/', params)
    assert worker_stub.call_count == 1
    assert response.status_code == 200

    response = Client().get('/ru/api/segments/train-tariffs/poll/', params)
    assert worker_stub.call_count == 1
    assert response.status_code == 200
    poll_result = response.data

    assert not poll_result['querying']
    assert len(poll_result['segments']) == 1
    assert poll_result['segments'][0]['departure'] == "2021-09-30T14:26:00+00:00"
    assert poll_result['segments'][0]['tariffs']['classes']['compartment']['price'] == {
        "currency": "RUB",
        "value": 3075.9
    }
