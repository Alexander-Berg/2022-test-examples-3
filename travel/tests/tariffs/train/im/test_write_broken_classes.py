# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

from datetime import date

import mock
import pytest
from hamcrest import assert_that, has_entries, has_properties

from common.apps.train.tariff_error import TariffError
from common.models.geo import StationExpressAlias
from common.tester.factories import create_station
from common.tester.utils.datetime import replace_now
from common.tester.utils.replace_setting import replace_setting, replace_dynamic_setting, replace_dynamic_setting_key
from common.utils.yasmutil import YasmMetricSender
from travel.proto.dicts.trains.station_express_alias_pb2 import TStationExpressAlias
from travel.rasp.train_api.tariffs.train.base.worker import TrainTariffsResult
from travel.rasp.train_api.tariffs.train.factories.base import (
    create_www_setting_cache_timeouts, create_train_tariffs_query
)
from travel.rasp.train_api.tariffs.train.factories.im import ImTrainPricingResponseFactory
from travel.rasp.train_api.tariffs.train.im.send_query import do_im_query, TRAIN_PRICING_ENDPOINT
from travel.rasp.train_api.train_partners.im.factories.utils import mock_im
from travel.rasp.train_api.train_purchase.core.models import TrainPartner

pytestmark = [pytest.mark.dbuser]

TRAIN_PRICING_NOT_AVAILABLE_IN_WEB = ImTrainPricingResponseFactory(**{
    "OriginCode": "2028160",
    "OriginStationCode": "2028160",
    "DestinationCode": "2044001",
    "DestinationStationCode": "2044001",
    "Trains": [{
        "CarGroups": [{
            "CarType": "Sedentary",
            "AvailabilityIndication": "NotAvailableInWeb",
        }],
        "OriginName": "НОВОКУЗНЕЦ",
        "InitialStationName": "НОВОКУЗНЕЦ",
        "OriginStationCode": "2028160",
        "DestinationName": "НОВОСИБ ГЛ",
        "FinalStationName": "НОВОСИБ ГЛ",
        "DestinationStationCode": "2044001",
        "DepartureDateTime": "2020-01-31T03:05:00",
        "ArrivalDateTime": "2020-01-31T09:10:00",
        "IsSaleForbidden": True,
    }],
})


@replace_now('2020-01-24 01:00:00')
@replace_setting('YASMAGENT_ENABLE_MEASURABLE', True)
@pytest.mark.usefixtures('worker_cache_stub', 'worker_stub')
@mock.patch.object(YasmMetricSender, 'send_many')
@pytest.mark.parametrize('include_reason_for_missing_prices', (True, False))
@pytest.mark.parametrize('protobufs_setting_value', (True, False))
def test_not_available_in_web(m_send_many, httpretty, include_reason_for_missing_prices,
                              protobufs_setting_value, protobuf_data_provider):
    with replace_dynamic_setting('TRAIN_PURCHASE_FEATURE_REASON_FOR_MISSING_PRICES', include_reason_for_missing_prices), \
         replace_dynamic_setting_key('TRAIN_BACKEND_USE_PROTOBUFS', 'alias', protobufs_setting_value):  # noqa: E126
        create_www_setting_cache_timeouts()
        mock_im(httpretty, TRAIN_PRICING_ENDPOINT, json=TRAIN_PRICING_NOT_AVAILABLE_IN_WEB)

        station_from = create_station(__=dict(codes={'express': '2000001'}))
        station_to = create_station(__=dict(codes={'express': '2000002'}))
        if protobufs_setting_value:
            for alias_proto in (
                TStationExpressAlias(StationId=station_from.id, Alias='НОВОКУЗНЕЦ'),
                TStationExpressAlias(StationId=station_to.id, Alias='НОВОСИБ ГЛ')
            ):
                protobuf_data_provider.alias_repo.add(alias_proto.SerializeToString())
        else:
            StationExpressAlias.objects.create(station=station_from, alias='НОВОКУЗНЕЦ')
            StationExpressAlias.objects.create(station=station_to, alias='НОВОСИБ ГЛ')
        train_query = create_train_tariffs_query(TrainPartner.IM, station_from, station_to,
                                                 departure_date=date(2020, 1, 31),
                                                 include_reason_for_missing_prices=include_reason_for_missing_prices)

        do_im_query(train_query, include_reason_for_missing_prices)

        result = TrainTariffsResult.get_from_cache(train_query)

        assert len(result.segments) == 1
        assert_that(result.segments[0], has_properties(
            tariffs=has_entries(
                classes={},
                broken_classes=has_entries(sitting=[TariffError.NOT_AVAILABLE_IN_WEB])
            )
        ))
