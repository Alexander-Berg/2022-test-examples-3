# -*- coding: utf-8 -*-
import pytest
from hamcrest import assert_that, has_items

from market.idx.yatf.resources.shops_dat import ShopsDat
from market.proto.indexer.qpipe_pb2 import (
    Offer as QPipeRecord,
    Fields as QPipeFields,
    Data as QPipeData,
    API, FEED,
    PRICES,
    WHITE, BLUE, RED,
)
from market.proto.indexer.GenerationLog_pb2 import Record as GenlogRecord

from market.qpipe.yatf.merger_env import MergerTestEnv
from market.qpipe.yatf.resources.qpipe_records import QPipeRecords
from market.qpipe.yatf.resources.genlog_records import GenlogRecords


MERGER_VERSION = 1


@pytest.fixture(scope="module")
def api_records():
    deleted_info = QPipeData(
        source=API,
        type=PRICES,
        timestamp=100,
        fields=QPipeFields(offer_deleted=True)
    )
    return QPipeRecords("api", [
        QPipeRecord(feed_id=100, offer_id="1", data=[deleted_info]),
        QPipeRecord(feed_id=110, offer_id="1", data=[deleted_info]),
        QPipeRecord(feed_id=120, offer_id="1", data=[deleted_info]),
    ])


@pytest.fixture(scope="module")
def feed_records():
    deleted_info = {"offers_robot_session": 100, "flags": 2048}
    return GenlogRecords([
        GenlogRecord(feed_id=200, offer_id="1", **deleted_info),
        GenlogRecord(feed_id=210, offer_id="1", **deleted_info),
        GenlogRecord(feed_id=220, offer_id="1", **deleted_info),
    ])


@pytest.fixture(scope="module")
def dummy_records():
    deleted_info_api = QPipeData(
        source=API,
        type=PRICES,
        timestamp=100,
        fields=QPipeFields(offer_deleted=True)
    )
    deleted_info_feed = QPipeData(
        source=FEED,
        type=PRICES,
        timestamp=100,
        fields=QPipeFields(offer_deleted=True, flags=2048)
    )
    return QPipeRecords("dummy", [
        QPipeRecord(feed_id=300, offer_id="1", data=[deleted_info_api]),
        QPipeRecord(feed_id=300, offer_id="2", data=[deleted_info_feed]),
        QPipeRecord(feed_id=310, offer_id="1", data=[deleted_info_api]),
        QPipeRecord(feed_id=310, offer_id="2", data=[deleted_info_feed]),
        QPipeRecord(feed_id=320, offer_id="1", data=[deleted_info_api]),
        QPipeRecord(feed_id=320, offer_id="2", data=[deleted_info_feed]),
    ])


@pytest.fixture(scope="module")
def shops_dat():
    return ShopsDat([
        # API
        {"datafeed_id": 100},
        {"datafeed_id": 110, "blue_status": "REAL"},
        {"datafeed_id": 120, "red_status": "REAL"},
        # FEED
        {"datafeed_id": 200},
        {"datafeed_id": 210, "blue_status": "REAL"},
        {"datafeed_id": 220, "red_status": "REAL"},
        # DUMMY
        {"datafeed_id": 300},
        {"datafeed_id": 310, "blue_status": "REAL"},
        {"datafeed_id": 320, "red_status": "REAL"},
    ])


@pytest.yield_fixture(scope="module")
def workflow(api_records, feed_records, dummy_records, shops_dat):
    resources = {
        "api-records": api_records,
        "feed-records": feed_records,
        "dummy-records": dummy_records,
        "shops-dat": shops_dat,
    }
    with MergerTestEnv(**resources) as env:
        env.execute()
        env.verify()
        yield env


def test_check_feeds_painted(workflow):
    """
    Проверяем что фиды при передаче shop-dat раскрашиваются,
    то есть им проставляется правильный цвет в поле market_color.
    """
    deleted_info_api = QPipeData(
        source=API,
        type=PRICES,
        timestamp=100,
        fields=QPipeFields(offer_deleted=True)
    )
    deleted_info_feed = QPipeData(
        source=FEED,
        type=PRICES,
        timestamp=100,
        fields=QPipeFields(offer_deleted=True, flags=2048)
    )

    expected_records = [
        QPipeRecord(feed_id=100, offer_id="1", data=[deleted_info_api], market_color=WHITE, merger_version=MERGER_VERSION),
        QPipeRecord(feed_id=110, offer_id="1", data=[deleted_info_api], market_color=BLUE, merger_version=MERGER_VERSION),
        QPipeRecord(feed_id=120, offer_id="1", data=[deleted_info_api], market_color=RED, merger_version=MERGER_VERSION),

        QPipeRecord(feed_id=200, offer_id="1", data=[deleted_info_feed], market_color=WHITE, merger_version=MERGER_VERSION),
        QPipeRecord(feed_id=210, offer_id="1", data=[deleted_info_feed], market_color=BLUE, merger_version=MERGER_VERSION),
        QPipeRecord(feed_id=220, offer_id="1", data=[deleted_info_feed], market_color=RED, merger_version=MERGER_VERSION),
    ]

    records = workflow.outputs["merged-records"]
    assert_that(records, has_items(*expected_records))


def test_dummy_data_added(workflow):
    """
    Проверяем что данные переданные через --dummy доливаются в выходные протобуфы
    При этом
    - у них сохраняется source(API/FEED) преданный из входного протобуфа
    - офера точно так же расклашиваются как и api/feed
    """
    deleted_info_api = QPipeData(
        source=API,
        type=PRICES,
        timestamp=100,
        fields=QPipeFields(offer_deleted=True)
    )
    deleted_info_feed = QPipeData(
        source=FEED,
        type=PRICES,
        timestamp=100,
        fields=QPipeFields(offer_deleted=True, flags=2048)
    )

    expected_records = [
        QPipeRecord(feed_id=300, offer_id="1", data=[deleted_info_api], market_color=WHITE, merger_version=MERGER_VERSION),
        QPipeRecord(feed_id=300, offer_id="2", data=[deleted_info_feed], market_color=WHITE, merger_version=MERGER_VERSION),
        QPipeRecord(feed_id=310, offer_id="1", data=[deleted_info_api], market_color=BLUE, merger_version=MERGER_VERSION),
        QPipeRecord(feed_id=310, offer_id="2", data=[deleted_info_feed], market_color=BLUE, merger_version=MERGER_VERSION),
        QPipeRecord(feed_id=320, offer_id="1", data=[deleted_info_api], market_color=RED, merger_version=MERGER_VERSION),
        QPipeRecord(feed_id=320, offer_id="2", data=[deleted_info_feed], market_color=RED, merger_version=MERGER_VERSION),
    ]

    records = workflow.outputs["merged-records"]
    assert_that(records, has_items(*expected_records))
