# coding: utf-8
import os
import random
import string
import json

import pytest
from hamcrest import assert_that
from google.protobuf.json_format import ParseDict

from market.idx.datacamp.lib.drop_filter.proto import drop_filter_pb2
from market.idx.datacamp.routines.yatf.resources.config_mock import RoutinesConfigMock
from market.idx.datacamp.routines.yatf.test_env import HttpRoutinesTestEnv
from market.idx.datacamp.yatf.matchers.matchers import HasStatus


@pytest.fixture(scope='module')
def config(yt_server, log_broker_stuff):
    cfg = {
        'general': {
            'color': 'white',
        },
        'ydb': {
            'database_end_point': os.getenv('YDB_ENDPOINT'),
            'database_path': os.getenv('YDB_DATABASE'),
            'mining_coordination_node_path': 'coordination',
            "drop_filter_coordination_node_path": "drop_filter",
            "drop_filter_publishing_semaphore_name": "publishSem",
            "drop_filter_blocking_semaphore_name": "blockSem",
        },
    }
    return RoutinesConfigMock(
        yt_server=yt_server,
        log_broker_stuff=log_broker_stuff,
        config=cfg)


@pytest.yield_fixture(scope='module')
def routines_http(yt_server, config):
    resources = {
        'config': config,
    }
    with HttpRoutinesTestEnv(yt_server, **resources) as routines_http_env:
        yield routines_http_env


def test_drop_filter(routines_http, config):  # name like that to run test after test_set
    RANDOM_STRING = ''.join(random.choice(string.ascii_uppercase + string.digits) for _ in range(15))
    json_string = json.dumps({
        'dropSynthetic': True,
        'filterInside': False,
        'color': RANDOM_STRING,
        'businessId': [1, 228, 1337],
    }, ensure_ascii=False)

    response = routines_http.put('/set_drop_filter', data=json_string)
    assert_that(response, HasStatus(200))

    response = routines_http.get('/get_drop_filter')
    assert_that(response, HasStatus(200))

    drop_filter = ParseDict(response.json, drop_filter_pb2.DropFilter())
    assert drop_filter.color == RANDOM_STRING
    assert drop_filter.drop_synthetic
    assert not drop_filter.filter_inside
    assert drop_filter.business_id == [1, 228, 1337]
