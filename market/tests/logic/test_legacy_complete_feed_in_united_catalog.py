# coding: utf-8

from hamcrest import assert_that
import pytest

from market.idx.datacamp.parser.yatf.env import WorkersEnv, make_input_task, UpdateTaskServiceMock
from market.idx.datacamp.parser.yatf.fake_mds import FakeMds
from market.idx.datacamp.parser.yatf.qp_mocks import QParserTestLauncherMock
from market.idx.datacamp.parser.yatf.resources.config_mock import PushParserConfigMock
from market.idx.datacamp.proto.api.UpdateTask_pb2 import FEED_CLASS_COMPLETE
from market.idx.datacamp.proto.offer.DataCampOffer_pb2 import PUSH_PARTNER_FEED
from market.idx.datacamp.yatf.matchers.matchers import HasSerializedDatacampMessages
from market.idx.yatf.matchers.lb_matchers import HasNoUnreadData
from market.idx.yatf.matchers.protobuf_matchers import IsProtobufMap
from market.idx.yatf.resources.lbk_topic import LbkTopic


BUSINESS_ID = 10
SHOP_ID = 111
FEED_ID = 100

OFFERS = [{
    'id': 'with-content',
    'shop-sku': 'with-content',
    'vendor': 'Default Vendor',
    'manufacturer_warranty': 'False',
    'dimensions': '1.0/2.0/3.0',
    'weight': '1',
    'count': '1',
    'price': '1000',
    'oldprice': '1500',
    'vat': '7',
    'disabled': 'True'
}]


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
                'complete_feed_finish_command_batch_size': 1000,
                'datacamp_messages_topic': datacamp_output_topic.topic,
                'datacamp_messages_writers_count': 1,
            }
        },
        color='blue',
        feed_info={
            'is_discounts_enabled': True
        }
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


def test_ignore_content_in_legacy_complete_feeds(push_parser, input_topic, output_topic, mds):
    mds.generate_feed(
        FEED_ID,
        is_blue=True,
        is_csv=False,
        force_offers=OFFERS,
        is_advanced_blue=True,
    )
    input_topic.write(
        make_input_task(
            mds,
            FEED_ID,
            BUSINESS_ID,
            SHOP_ID,
            task_type=FEED_CLASS_COMPLETE
        ).SerializeToString()
    )
    push_parser.run(total_sessions=1)
    data = output_topic.read(count=1)

    assert_that(data, HasSerializedDatacampMessages([{
        'united_offers': [{
            'offer': [{
                'basic': {
                    'identifiers': {
                        'business_id': BUSINESS_ID,
                        'offer_id': 'with-content',
                    },
                },
                'service': IsProtobufMap({
                    SHOP_ID: {
                        'identifiers': {
                            'business_id': BUSINESS_ID,
                            'offer_id': 'with-content',
                            'shop_id': SHOP_ID,
                        },
                        'content': None,
                        'stock_info': {
                            'partner_stocks_default': {
                                'count': 1,
                            },
                        },
                        'status': {
                            'united_catalog': {
                                'flag': True
                            },
                            'disabled': [{
                                'flag': True,
                                'meta': {
                                    'source': PUSH_PARTNER_FEED
                                }
                            }]
                        },
                        'price': {
                            'basic': {
                                'binary_price': {'price': 1000 * 10**7},
                                'binary_oldprice': {'price': 1500 * 10**7},
                                'vat': 7
                            },
                        },
                    }
                })
            }]
        }]
    }]))

    assert_that(output_topic, HasNoUnreadData())
