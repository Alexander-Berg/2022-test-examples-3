# coding: utf-8

import pytest
from hamcrest import assert_that, empty
from datetime import datetime
from google.protobuf.timestamp_pb2 import Timestamp
from concurrent.futures import TimeoutError

from market.idx.datacamp.parser.yatf.env import WorkersEnv, make_basic_assortment_input_task, UpdateTaskServiceMock
from market.idx.datacamp.parser.yatf.fake_mds import FakeMds
from market.idx.datacamp.parser.yatf.qp_mocks import QParserTestLauncherMock
from market.idx.datacamp.proto.api.UpdateTask_pb2 import FEED_CLASS_ASSORTMENT_BASIC_PATCH_UPDATE
from market.idx.datacamp.parser.yatf.resources.config_mock import PushParserConfigMock
from market.idx.datacamp.yatf.matchers.matchers import HasSerializedDatacampMessages
from market.idx.yatf.resources.lbk_topic import LbkTopic

BUSINESS_ID = 1
FEED_ID = 10

OFFERS = [{
    'id': 'o1',
    'price': None,
    'currencyId': None
}]

NOW = datetime.utcnow()
TIMESTAMP = Timestamp()
TIMESTAMP.FromDatetime(NOW)


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


@pytest.fixture()
def config(tmpdir_factory, log_broker_stuff, yt_server, output_topic, input_topic, datacamp_output_topic):
    cfg = {
        'logbroker_in': {
            'topic': input_topic.topic,
        },
        'logbroker': {
            'topic': output_topic.topic,
            'datacamp_messages_topic': datacamp_output_topic.topic
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
def qp_runner(config, log_broker_stuff, datacamp_output_topic):
    yield QParserTestLauncherMock(
        log_broker_stuff=log_broker_stuff,
        config=config,
        qparser_config={
            'logbroker': {
                'datacamp_messages_topic': datacamp_output_topic.topic,
                'datacamp_messages_writers_count': 1,
            },
        },
        color='no color',
        feed_format='xml',
    )


@pytest.fixture()
def push_parser(monkeypatch, config, qp_runner):
    with monkeypatch.context() as m:
        m.setattr("market.idx.datacamp.parser.lib.worker.ParsingTaskWorker.process_task", qp_runner.process_task)

        yield WorkersEnv(
            config=config,
            parsing_service=UpdateTaskServiceMock
        )


def test_parsing_task_for_assortment_basic_feed(push_parser, mds, input_topic, output_topic, datacamp_output_topic):
    mds.generate_feed(FEED_ID, force_offers=OFFERS, is_csv=False)

    feed_type = FEED_CLASS_ASSORTMENT_BASIC_PATCH_UPDATE
    feed_parsing_task = make_basic_assortment_input_task(
        mds,
        BUSINESS_ID,
        FEED_ID,
        task_type=feed_type,
        timestamp=TIMESTAMP
    )

    input_topic.write(feed_parsing_task.SerializeToString())
    push_parser.run(total_sessions=1)

    output = output_topic.read(count=1)
    assert_that(output, HasSerializedDatacampMessages([{
        'united_offers': [
            {
                'offer': [
                    {
                        'basic': {
                            'identifiers': {
                                'business_id': BUSINESS_ID,
                                'offer_id': 'o1'
                            },
                            'meta': {
                                'business_catalog': {
                                    'flag': True
                                }
                            }
                        },
                        'service': empty()
                    }
                ]
            }
        ]
    }]))

    # отправляются категории
    datacamp_output_topic_messages = datacamp_output_topic.read(count=1)
    assert_that(datacamp_output_topic_messages, HasSerializedDatacampMessages([{
        'partner_categories': [{
            'categories': [{
                'business_id': BUSINESS_ID,
                'id': 1
            }]
        }]
    }]))

    # нет никаких комплит команд
    with pytest.raises(TimeoutError):
        datacamp_output_topic.read(count=1)
