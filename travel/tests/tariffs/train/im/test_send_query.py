# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

import json
import mock
import pytest
from datetime import date
from hamcrest import assert_that, has_entries

from common.models.tariffs import Setting
from common.tester.factories import create_station
from common.tester.utils.datetime import replace_now
from common.tester.utils.replace_setting import replace_setting
from common.utils.yasmutil import YasmMetricSender
from travel.rasp.train_api.tariffs.train.base.worker import TrainTariffsResult
from travel.rasp.train_api.tariffs.train.factories.base import (
    create_www_setting_cache_timeouts, create_train_tariffs_query
)
from travel.rasp.train_api.tariffs.train.factories.im import ImTrainPricingResponseFactory
from travel.rasp.train_api.tariffs.train.im.send_query import (
    do_im_query, TRAIN_PRICING_ENDPOINT
)
from travel.rasp.train_api.train_partners.im.factories.utils import mock_im
from travel.rasp.train_api.train_purchase.core.models import TrainPartner

pytestmark = [pytest.mark.dbuser]


def create_response():
    return ImTrainPricingResponseFactory(**{
        "Trains": [{}, {}, {}]
    })


@replace_now('2017-05-18 01:00:00')
@replace_setting('YASMAGENT_ENABLE_MEASURABLE', True)
@pytest.mark.usefixtures('worker_cache_stub')
@mock.patch.object(YasmMetricSender, 'send_many')
def test_do_im_query_with_cache(m_send_many, worker_stub, httpretty):
    create_www_setting_cache_timeouts()
    mock_im(httpretty, TRAIN_PRICING_ENDPOINT, json=create_response())

    station_from = create_station(__=dict(codes={'express': '2000001'}))
    station_to = create_station(__=dict(codes={'express': '2000002'}))
    train_query = create_train_tariffs_query(TrainPartner.IM, station_from, station_to,
                                             departure_date=date(2017, 5, 20))
    do_im_query(train_query, include_reason_for_missing_prices=True)

    result = TrainTariffsResult.get_from_cache(train_query)
    assert len(result.segments) == 3
    assert result.cache_timeout == Setting.get('UFS_CACHE_TIMEOUT') * 60

    assert worker_stub.call_count == 1
    assert len(httpretty.latest_requests)
    assert_that(json.loads(httpretty.last_request.body), has_entries({
        'Origin': '2000001',
        'Destination': '2000002',
        'DepartureDate': '2017-05-20T00:00:00',
        'CarGrouping': 'DontGroup',
        'GetByLocalTime': True,
    }))
    assert m_send_many.call_count == 2
