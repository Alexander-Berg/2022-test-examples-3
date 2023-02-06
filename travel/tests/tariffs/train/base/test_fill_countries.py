# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

from datetime import date, datetime, time

import pytest
from django.core.urlresolvers import reverse
from django.db import connection
from django.test import Client
from django.test.utils import CaptureQueriesContext
from hamcrest import assert_that, has_entries

from common.dynamic_settings.default import conf
from common.models.geo import StationExpressAlias, Settlement, CityMajority
from common.models.transport import TransportType
from common.tester.factories import create_station, create_settlement, create_thread, create_country
from common.tester.utils.datetime import replace_now
from common.tester.utils.replace_setting import replace_dynamic_setting, replace_dynamic_setting_key
from common.utils.date import MSK_TIMEZONE
from travel.proto.dicts.trains.station_express_alias_pb2 import TStationExpressAlias
from travel.rasp.train_api.tariffs.train.base.worker import TrainTariffsResult
from travel.rasp.train_api.tariffs.train.factories.base import (
    create_www_setting_cache_timeouts, create_train_tariffs_query
)
from travel.rasp.train_api.tariffs.train.factories.im import ImTrainPricingResponseFactory
from travel.rasp.train_api.tariffs.train.im.send_query import (
    get_result_from_partner_and_add_schedule_info, TRAIN_PRICING_ENDPOINT
)
from travel.rasp.train_api.train_partners.im.factories.utils import mock_im
from travel.rasp.train_api.train_purchase.core.models import TrainPartner


def _create_im_response():
    return ImTrainPricingResponseFactory(
        Trains=[{
            'TrainNumber': '020У',
            'DepartureDateTime': datetime(2016, 5, 15, 17, 26),
            'OriginName': 'МОСКВА ОКТ',
            'DestinationName': 'С-ПЕТЕР-ГЛ',
        }]
    )


def _set_up_db_objects(protobuf_data_provider):
    country_aa = create_country(code='AA')
    aa_capital = create_settlement(country=country_aa,  # noqa
                                   majority=CityMajority.CAPITAL_ID, time_zone=MSK_TIMEZONE)
    piter = create_settlement(title='Питер', time_zone=MSK_TIMEZONE)
    station_from = create_station(__={'codes': {'express': '0000000'}}, settlement=Settlement.MOSCOW_ID,
                                  country=country_aa)
    station_to = create_station(__={'codes': {'express': '1111111'}}, settlement=piter)

    create_thread(
        tz_start_time=time(17, 26),
        number='020У',
        t_type=TransportType.TRAIN_ID,
        schedule_v1=[
            [None, 0, station_from],
            [300, None, station_to],
        ],
        __={'calculate_noderoute': True}
    )

    if conf.TRAIN_BACKEND_USE_PROTOBUFS['alias']:
        for alias_proto in (
            TStationExpressAlias(StationId=station_from.id, Alias='МОСКВА ОКТ'),
            TStationExpressAlias(StationId=station_to.id, Alias='С-ПЕТЕР-ГЛ')
        ):
            protobuf_data_provider.alias_repo.add(alias_proto.SerializeToString())
    else:
        StationExpressAlias.objects.create(station=station_from, alias='МОСКВА ОКТ')
        StationExpressAlias.objects.create(station=station_to, alias='С-ПЕТЕР-ГЛ')

    return station_from, station_to


@pytest.mark.dbuser
@replace_now('2016-05-01')
@pytest.mark.parametrize('include_reason_for_missing_prices', (True, False))
@pytest.mark.parametrize('protobufs_setting_value', (True, False))
def test_fill_countries(httpretty, include_reason_for_missing_prices, protobufs_setting_value, protobuf_data_provider):
    with replace_dynamic_setting('TRAIN_PURCHASE_FEATURE_REASON_FOR_MISSING_PRICES', include_reason_for_missing_prices), \
         replace_dynamic_setting_key('TRAIN_BACKEND_USE_PROTOBUFS', 'alias', protobufs_setting_value):  # noqa: E126
        station_from, station_to = _set_up_db_objects(protobuf_data_provider)
        create_www_setting_cache_timeouts()
        mock_im(httpretty, TRAIN_PRICING_ENDPOINT, json=_create_im_response())

        query = create_train_tariffs_query(TrainPartner.IM, station_from, station_to, date(2016, 5, 15),
                                           include_reason_for_missing_prices)
        result = get_result_from_partner_and_add_schedule_info(query, include_reason_for_missing_prices)

        assert result.status == TrainTariffsResult.STATUS_SUCCESS
        assert len(result.segments) == 1
        segment = result.segments[0]
        assert segment.thread.first_country_code == 'AA'
        assert segment.thread.last_country_code is None

        if not conf.TRAIN_PURCHASE_FEATURE_REASON_FOR_MISSING_PRICES:
            # в новом коде не вытаскиваем страну для станции отправления и прибытия,
            # она нужна только для выдачи на все дни
            with CaptureQueriesContext(connection) as captured_queries:
                assert segment.station_from.country.code == 'AA'
                assert segment.station_to.country is None
                assert len(captured_queries) == 0


@pytest.mark.dbuser
@pytest.mark.usefixtures('worker_cache_stub')
@replace_now('2016-05-10 00:00:00')
@pytest.mark.parametrize('protobufs_setting_value', (True, False))
def test_country_in_response(httpretty, worker_stub, protobufs_setting_value, protobuf_data_provider):
    with replace_dynamic_setting_key('TRAIN_BACKEND_USE_PROTOBUFS', 'alias', protobufs_setting_value):
        mock_im(httpretty, TRAIN_PRICING_ENDPOINT, json=_create_im_response())
        station_from, station_to = _set_up_db_objects(protobuf_data_provider)

        create_www_setting_cache_timeouts()

        request_params = {
            'pointFrom': station_from.point_key,
            'pointTo': station_to.point_key,
            'date': '2016-05-15',
            'national_version': 'ru'
        }

        response = Client().get(reverse('train_tariffs'), request_params)

        assert response.status_code == 200
        assert worker_stub.call_count == 1
        assert_that(response.data, has_entries(querying=True))

        worker_stub.call_count = 0
        response = Client().get(reverse('train_tariffs_poll'), request_params)

        assert response.status_code == 200
        assert not worker_stub.call_count
        segments = response.data['segments']
        assert len(segments) == 1
        assert_that(segments[0], has_entries({
            'thread': has_entries({
                'firstCountryCode': 'AA',
                'lastCountryCode': None
            }),
            'stationFrom': has_entries({
                'country': has_entries(code='AA')
            }),
            'stationTo': has_entries(country=None)
        }))
