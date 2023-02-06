# coding: utf-8

import pytest

from hamcrest import assert_that

from market.idx.datacamp.parser.yatf.env import WorkersEnv, make_input_task, UpdateTaskServiceMock
from market.idx.datacamp.parser.yatf.resources.config_mock import PushParserConfigMock
from market.idx.yatf.matchers.protobuf_matchers import IsSerializedProtobuf

from market.idx.yatf.resources.lbk_topic import LbkTopic

from market.idx.datacamp.parser.yatf.fake_mds import FakeMds
from market.idx.datacamp.parser.yatf.qp_mocks import QParserTestLauncherMock

from market.idx.datacamp.proto.offer.OfferBlog_pb2 import OfferBlog
from market.proto.common.common_pb2 import EComponent
import market.proto.common.process_log_pb2 as PL


BUSINESS_ID = 10
SHOP_ID = 111
FEED_ID = 100


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
def blog_topic(log_broker_stuff):
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
def qp_runner(config, log_broker_stuff, blog_topic):
    yield QParserTestLauncherMock(
        log_broker_stuff=log_broker_stuff,
        config=config,
        qparser_config={
            'blog': {
                'enable': True,
                'topic': blog_topic.topic,
                'log_level': 'message',
            }
        }
    )


@pytest.fixture()
def push_parser(monkeypatch, config, qp_runner):
    with monkeypatch.context() as m:
        m.setattr("market.idx.datacamp.parser.lib.worker.ParsingTaskWorker.process_task", qp_runner.process_task)

        yield WorkersEnv(
            config=config,
            parsing_service=UpdateTaskServiceMock
        )


def test_qparser_blog(push_parser, input_topic, blog_topic, mds):
    """Проверяем, что при возникновении ошибок при обработке оферов, они пишутся в топик блога"""

    mds.generate_feed(FEED_ID, is_blue=True, bad=True, offer_count=1, is_advanced_blue=True)

    input_topic.write(make_input_task(mds, 100, BUSINESS_ID, SHOP_ID).SerializeToString())

    push_parser.run(total_sessions=1)

    data = blog_topic.read(count=1)

    EXPECTED = {
        'identifiers': {
            'feed_id': FEED_ID,
            'shop_id': SHOP_ID,
            'offer_id': '{}xXx{}'.format(FEED_ID, 0),
        },
        'errors': {
            'error': [{
                'code': '452',
                'level': PL.ERROR,
                'text': 'Invalid offer price: The price aint a double number',
                'namespace': PL.OFFER,
                'source': EComponent.QPARSER,
            }]
        },
    }

    assert_that(data[0], IsSerializedProtobuf(OfferBlog, EXPECTED))
