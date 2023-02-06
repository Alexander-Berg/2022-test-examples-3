# coding: utf-8

import pytest
from hamcrest import assert_that, has_items

from market.idx.datacamp.proto.api.DatacampMessage_pb2 import DatacampMessage
from market.idx.datacamp.routines.yatf.resources.config_mock import RoutinesConfigMock
from market.idx.datacamp.routines.yatf.test_env import HttpRoutinesTestEnv
from market.idx.datacamp.yatf.matchers.matchers import HasStatus
from market.idx.yatf.matchers.protobuf_matchers import IsSerializedProtobuf
from market.idx.yatf.resources.lbk_topic import LbkTopic
from market.idx.yatf.resources.yt_table_resource import YtTableResource
from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix
from yt.wrapper import ypath_join


@pytest.fixture(scope='module')
def mboc_offers_datacamp_msku_topic(log_broker_stuff):
    topic = LbkTopic(log_broker_stuff)
    return topic


@pytest.fixture(scope='module')
def msku_ids_table(yt_server):
    return YtTableResource(yt_server, ypath_join(get_yt_prefix(), 'msku_ids'), data=[{'msku_id': 3}], fail_on_exists=False)


@pytest.fixture(scope='function')
def config(yt_server, log_broker_stuff, mboc_offers_datacamp_msku_topic):
    cfg = {
        'general': {
            'color': 'white',
        },
        'mboc_offers': {
            'datacamp_msku_topic': mboc_offers_datacamp_msku_topic.topic
        },
    }
    return RoutinesConfigMock(yt_server=yt_server, log_broker_stuff=log_broker_stuff, config=cfg)


@pytest.yield_fixture(scope='function')
def routines_http(yt_server, config, mboc_offers_datacamp_msku_topic, msku_ids_table):
    resources = {
        'config': config,
        'mboc_offers_datacamp_msku_topic': mboc_offers_datacamp_msku_topic,
        'msku_ids_table': msku_ids_table,
    }
    with HttpRoutinesTestEnv(yt_server, **resources) as routines_http_env:
        yield routines_http_env


def test_cgi_params(mboc_offers_datacamp_msku_topic, routines_http):
    response = routines_http.post('/send_msku?msku_id=1')
    assert_that(response, HasStatus(200))

    data = mboc_offers_datacamp_msku_topic.read(1)

    assert_that(data, has_items(*[
        IsSerializedProtobuf(DatacampMessage, {
            'market_skus': {
                'msku': [
                    {'id': 1}
                ]}
            }
    )]))


def test_body_params(mboc_offers_datacamp_msku_topic, routines_http):
    response = routines_http.post('/send_msku',
                                  data='{"msku_ids": [2]}',
                                  headers={'Content-Type': 'application/json; charset=utf-8'})
    assert_that(response, HasStatus(200))

    data = mboc_offers_datacamp_msku_topic.read(1)

    assert_that(data, has_items(*[
        IsSerializedProtobuf(DatacampMessage, {
            'market_skus': {
                'msku': [
                    {'id': 2}
                ]}
        }
     )]))


def test_from_yt_table(yt_server, msku_ids_table, mboc_offers_datacamp_msku_topic, routines_http):
    response = routines_http.post('/send_msku?yt-proxy={}&yt-table-path={}'.format(yt_server.yt_proxy, msku_ids_table.table_path))
    assert_that(response, HasStatus(200))

    data = mboc_offers_datacamp_msku_topic.read(1)

    assert_that(data, has_items(*[
        IsSerializedProtobuf(DatacampMessage, {
            'market_skus': {
                'msku': [
                    {'id': 3}
                ]}
        }
    )]))
