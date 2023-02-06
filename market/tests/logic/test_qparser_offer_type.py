# coding: utf-8

from hamcrest import assert_that
import pytest

from market.idx.datacamp.parser.yatf.env import WorkersEnv, make_input_task, UpdateTaskServiceMock
from market.idx.datacamp.parser.yatf.fake_mds import FakeMds
from market.idx.datacamp.parser.yatf.qp_mocks import QParserTestLauncherMock
from market.idx.datacamp.parser.yatf.resources.config_mock import PushParserConfigMock
from market.idx.datacamp.proto.api.UpdateTask_pb2 import FEED_CLASS_SELECTIVE_BASIC_PATCH_UPDATE_SERVICE_FULL_COMPLETE
from market.idx.datacamp.yatf.matchers.matchers import HasSerializedDatacampMessages
from market.idx.yatf.matchers.lb_matchers import HasNoUnreadData
from market.idx.yatf.resources.lbk_topic import LbkTopic


BUSINESS_ID = 10
SHOP_ID = 111
FEED_ID = 100

OFFERS = [
    {
        'id': 'without-type',
        'shop-sku': 'without-type',
    },
    {
        'id': 'with-good-type',
        'shop-sku': 'with-good-type',
        'type': 'simple'
    },
    {
        'id': 'with-bad-type',
        'shop-sku': 'with-bad-type',
        'type': 'book'
    }
]


@pytest.fixture()
def input_topic(log_broker_stuff):
    topic = LbkTopic(log_broker_stuff)
    topic.create()
    return topic


@pytest.fixture()
def output_topic(log_broker_stuff):
    topic = LbkTopic(log_broker_stuff)
    topic.create()
    return topic


@pytest.fixture()
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
def qparser_config(datacamp_output_topic):
    return {
        'logbroker': {
            'complete_feed_finish_command_batch_size': 1000,
            'datacamp_messages_topic': datacamp_output_topic.topic,
            'datacamp_messages_writers_count': 1,
        }
    }


@pytest.yield_fixture()
def qp_runner(config, log_broker_stuff, qparser_config):
    yield QParserTestLauncherMock(
        log_broker_stuff=log_broker_stuff,
        config=config,
        qparser_config=qparser_config,
        color='white'
    )


@pytest.yield_fixture()
def qp_runner_market_color_blue(config, log_broker_stuff, qparser_config):
    yield QParserTestLauncherMock(
        log_broker_stuff=log_broker_stuff,
        config=config,
        qparser_config=qparser_config,
        color='blue'
    )


@pytest.fixture()
def push_parser(monkeypatch, config, qp_runner):
    with monkeypatch.context() as m:
        m.setattr(
            "market.idx.datacamp.parser.lib.worker.ParsingTaskWorker.process_task",
            qp_runner.process_task
        )

        yield WorkersEnv(
            config=config,
            parsing_service=UpdateTaskServiceMock
        )


@pytest.fixture()
def push_parser_market_color_blue(monkeypatch, config, qp_runner_market_color_blue):
    with monkeypatch.context() as m:
        m.setattr(
            "market.idx.datacamp.parser.lib.worker.ParsingTaskWorker.process_task",
            qp_runner_market_color_blue.process_task
        )

        yield WorkersEnv(
            config=config,
            parsing_service=UpdateTaskServiceMock
        )


def test_ignore_offers_with_wrong_type(push_parser, input_topic, output_topic, mds):
    mds.generate_feed(
        FEED_ID,
        is_blue=False,
        is_csv=False,
        force_offers=OFFERS,
    )
    input_topic.write(
        make_input_task(
            mds,
            FEED_ID,
            BUSINESS_ID,
            SHOP_ID,
            task_type=FEED_CLASS_SELECTIVE_BASIC_PATCH_UPDATE_SERVICE_FULL_COMPLETE
        ).SerializeToString()
    )
    push_parser.run(total_sessions=1)
    data = output_topic.read(count=2)

    assert_that(data, HasSerializedDatacampMessages([{
        'united_offers': [{
            'offer': [{
                'basic': {
                    'identifiers': {
                        'business_id': BUSINESS_ID,
                        'offer_id': 'without-type',
                    },
                },
            }]
        }]}, {
        'united_offers': [{
            'offer': [{
                'basic': {
                    'identifiers': {
                        'business_id': BUSINESS_ID,
                        'offer_id': 'with-good-type',
                    },
                },
            }]
        }]
    }]))

    assert_that(output_topic, HasNoUnreadData())


def test_accept_blue_offers_with_special_types(push_parser_market_color_blue, input_topic, output_topic, mds):
    """ Для синих офферов нет никаких ограничений по типу оффера """
    mds.generate_feed(
        FEED_ID,
        is_blue=True,
        is_csv=False,
        force_offers=OFFERS,
    )
    input_topic.write(
        make_input_task(
            mds,
            FEED_ID,
            BUSINESS_ID,
            SHOP_ID,
            task_type=FEED_CLASS_SELECTIVE_BASIC_PATCH_UPDATE_SERVICE_FULL_COMPLETE
        ).SerializeToString()
    )
    push_parser_market_color_blue.run(total_sessions=1)
    data = output_topic.read(count=3)

    assert_that(data, HasSerializedDatacampMessages([{
        'united_offers': [{
            'offer': [{
                'basic': {
                    'identifiers': {
                        'business_id': BUSINESS_ID,
                        'offer_id': offer_id,
                    },
                },
            }]
        }]} for offer_id in ['without-type', 'with-good-type', 'with-bad-type']]
    ))

    assert_that(output_topic, HasNoUnreadData())
