# coding: utf-8

import pytest

from market.idx.datacamp.parser.yatf.env import WorkersEnv, make_input_task, UpdateTaskServiceMock
from market.idx.datacamp.parser.yatf.fake_mds import FakeMds
from market.idx.datacamp.parser.yatf.qp_mocks import QParserTestLauncherMock
from market.idx.datacamp.parser.yatf.resources.config_mock import PushParserConfigMock
from market.idx.datacamp.proto.api.UpdateTask_pb2 import (
    ShopsDatParameters,
    FEED_CLASS_STOCK
)
import market.idx.datacamp.proto.offer.DataCampOffer_pb2 as DTC
from market.idx.datacamp.proto.api.ExportMessage_pb2 import ExportMessagesBatch
from market.idx.datacamp.proto.common.Consumer_pb2 import Platform
from market.idx.yatf.resources.lbk_topic import LbkTopic
from market.idx.yatf.utils.utils import create_pb_timestamp

import yatest.common

BUSINESS_ID = 10
SHOP_ID = 111
WAREHOUSE_ID = 150
FEED_ID = 100
TIMESTAMP = create_pb_timestamp(100500)


@pytest.fixture(scope='module')
def input_topic(log_broker_stuff):
    topic = LbkTopic(log_broker_stuff)
    topic.create()
    return topic


@pytest.fixture(scope='module')
def output_topic(log_broker_stuff):
    topic = LbkTopic(log_broker_stuff)
    topic.create()
    return topic


@pytest.fixture(scope='module')
def datacamp_output_topic(log_broker_stuff):
    topic = LbkTopic(log_broker_stuff)
    topic.create()
    return topic


@pytest.fixture(scope='module')
def sort_dc_offer_data_output_topic(log_broker_stuff):
    topic = LbkTopic(log_broker_stuff)
    topic.create()
    return topic


@pytest.fixture()
def config(tmpdir_factory, log_broker_stuff, yt_server, output_topic, input_topic, sort_dc_offer_data_output_topic, datacamp_output_topic):
    cfg = {
        'logbroker_in': {
            'topic': input_topic.topic,
        },
        'general': {
            'complete_feed_finish_command_batch_size': 1000,
        },
        'logbroker': {
            'topic': output_topic.topic,
            'datacamp_messages_topic': datacamp_output_topic.topic,
            'sort_dc_offer_data_topic' : sort_dc_offer_data_output_topic.topic,
            'sort_dc_offer_data_write_probability_percent': 100,
            'sort_dc_offer_data_writers_count': 1,
            'sort_dc_offer_data_batch_size': 2,
            'complete_feed_finish_command_batch_size': 1000,
        },
        'feature': {
            'enable_sort_dc_offer_data': True,
            'enable_sort_dc_offer_data_compression': False
        }
    }

    return PushParserConfigMock(
        workdir=tmpdir_factory.mktemp('workdir'),
        yt_server=yt_server,
        log_broker_stuff=log_broker_stuff,
        config=cfg
    )


@pytest.fixture()
def mds(tmpdir_factory, config):
    return FakeMds(tmpdir_factory.mktemp('mds'), config)


@pytest.yield_fixture()
def qp_runner_white(config, log_broker_stuff):
    yield QParserTestLauncherMock(
        log_broker_stuff=log_broker_stuff,
        config=config,
        feed_format='xml',
        color='white'
    )


@pytest.fixture()
def push_parser_white(monkeypatch, config, qp_runner_white):
    with monkeypatch.context() as m:
        m.setattr('market.idx.datacamp.parser.lib.worker.ParsingTaskWorker.process_task', qp_runner_white.process_task)

        yield WorkersEnv(
            config=config,
            parsing_service=UpdateTaskServiceMock
        )


def test_qparser_offer_data_feed(push_parser_white, input_topic, sort_dc_offer_data_output_topic, mds):
    mds.setup_push_feed(
        FEED_ID,
        yatest.common.source_path('market/idx/datacamp/parser/tests/feeds/data/offer_data_feed.xml')
    )

    real_feed_id = 100500
    feed_parsing_task = make_input_task(
        mds,
        FEED_ID,
        BUSINESS_ID,
        SHOP_ID,
        warehouse_id=WAREHOUSE_ID,
        task_type=FEED_CLASS_STOCK,
        shops_dat_parameters=ShopsDatParameters(
            vat=7,
            color=DTC.WHITE,
            is_mock=False,
            is_upload=True,
            local_region_tz_offset=10800,
            vertical_share=True,
        ),
        real_feed_id=real_feed_id,
        timestamp=TIMESTAMP,
        is_regular_parsing=False,
    )
    input_topic.write(feed_parsing_task.SerializeToString())
    push_parser_white.run(total_sessions=1)

    data = sort_dc_offer_data_output_topic.read(count=3)

    export_messages_batch = ExportMessagesBatch()

    expectedOffers = ('offer1', 'offer2')
    expectedUrls = ('url1', 'url2')
    export_messages_batch.ParseFromString(data[0])
    for i, message in enumerate(export_messages_batch.messages):
        assert message.offer.offer_id == expectedOffers[i]
        assert message.offer.original_content.url == expectedUrls[i]
        assert message.offer.feed_id == real_feed_id
        assert message.offer.service.platform == Platform.VERTICAL_GOODS
        assert message.offer.shop_id == SHOP_ID
        assert message.offer.business_id == BUSINESS_ID
        assert message.offer.timestamp == TIMESTAMP

    expectedOffers = ('offer3', 'offer4')
    expectedUrls = ('url3', 'url4')
    export_messages_batch.ParseFromString(data[1])
    for i, message in enumerate(export_messages_batch.messages):
        assert message.offer.offer_id == expectedOffers[i]
        assert message.offer.original_content.url == expectedUrls[i]
        assert message.offer.feed_id == real_feed_id
        assert message.offer.service.platform == Platform.VERTICAL_GOODS
        assert message.offer.shop_id == SHOP_ID
        assert message.offer.business_id == BUSINESS_ID
        assert message.offer.timestamp == TIMESTAMP

    export_messages_batch.ParseFromString(data[2])
    message = export_messages_batch.messages[0]
    assert message.offer.offer_id == 'offer5'
    assert message.offer.original_content.url == 'url5'
    assert message.offer.feed_id == real_feed_id
    assert message.offer.service.platform == Platform.VERTICAL_GOODS
    assert message.offer.shop_id == SHOP_ID
    assert message.offer.business_id == BUSINESS_ID
    assert message.offer.timestamp == TIMESTAMP
