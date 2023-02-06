# coding: utf-8

import pytest

from datetime import datetime
from google.protobuf.timestamp_pb2 import Timestamp
from hamcrest import assert_that

from market.idx.datacamp.parser.yatf.env import WorkersEnv, make_input_task, UpdateTaskServiceMock
from market.idx.datacamp.parser.yatf.fake_mds import FakeMds
from market.idx.datacamp.parser.yatf.qp_mocks import QParserTestLauncherMock
from market.idx.datacamp.parser.yatf.resources.config_mock import PushParserConfigMock
from market.idx.datacamp.proto.offer import DataCampOffer_pb2
from market.idx.datacamp.proto.offer.DataCampOffer_pb2 import UpdateMeta
from market.idx.datacamp.yatf.matchers.matchers import HasSerializedDatacampMessages

from market.idx.yatf.matchers.protobuf_matchers import IsProtobufMap
from market.idx.yatf.resources.lbk_topic import LbkTopic
from market.idx.yatf.utils.utils import create_pb_timestamp


BUSINESS_ID = 10
SHOP_ID = 111
FEED_ID = 100
REAL_FEED_ID = 736
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
def config(tmpdir_factory, log_broker_stuff, yt_server, output_topic, input_topic, datacamp_output_topic):
    cfg = {
        'logbroker_in': {
            'topic': input_topic.topic,
        },
        'logbroker': {
            'topic': output_topic.topic,
            'datacamp_messages_topic': datacamp_output_topic.topic,
        },
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


def qp_runner(config, log_broker_stuff, datacamp_output_topic, force_restore_offer):
    return QParserTestLauncherMock(
        log_broker_stuff=log_broker_stuff,
        config=config,
        feed_info={
            'market_color': 'blue',
        },
        qparser_config={
            'logbroker': {
                'complete_feed_finish_command_batch_size': 1000,
                'datacamp_messages_topic': datacamp_output_topic.topic,
                'datacamp_messages_writers_count': 1,
            },
            "feature": {
                "force_restore_offer": force_restore_offer
            }
        },
    )


@pytest.yield_fixture(scope='module')
def qp_runner_with_force_restore(config, log_broker_stuff, datacamp_output_topic):
    yield qp_runner(config, log_broker_stuff, datacamp_output_topic, True)


@pytest.yield_fixture(scope='module')
def qp_runner_without_force_restore(config, log_broker_stuff, datacamp_output_topic):
    yield qp_runner(config, log_broker_stuff, datacamp_output_topic, False)


@pytest.fixture(scope='module')
def push_parser_with_force_restore(monkeymodule, config, qp_runner_with_force_restore):
    with monkeymodule.context() as m:
        m.setattr("market.idx.datacamp.parser.lib.worker.ParsingTaskWorker.process_task", qp_runner_with_force_restore.process_task)
        yield WorkersEnv(config=config, parsing_service=UpdateTaskServiceMock)


@pytest.fixture(scope='module')
def push_parser_without_force_restore(monkeymodule, config, qp_runner_without_force_restore):
    with monkeymodule.context() as m:
        m.setattr("market.idx.datacamp.parser.lib.worker.ParsingTaskWorker.process_task", qp_runner_without_force_restore.process_task)
        yield WorkersEnv(config=config, parsing_service=UpdateTaskServiceMock)


def run_qparser(push_parser, input_topic, output_topic, mds, datacamp_output_topic, config, qp_runner_with_force_restore, must_be_restored):
    date = datetime.utcnow().replace(microsecond=0)
    yml_date = date.strftime('%Y-%m-%d %H:%M:%SZ')
    yml_platform = 'market test YML from_dicts'
    yml_version = 'test version 2.1'
    mds.generate_feed(
        FEED_ID,
        is_blue=True,
        offer_count=5,
        is_advanced_blue=True,
        shop_dict={
            'date': yml_date,
            'platform': yml_platform,
            'version': yml_version,
        }
    )

    input_topic.write(make_input_task(mds, FEED_ID, BUSINESS_ID, SHOP_ID, timestamp=TIMESTAMP, real_feed_id=REAL_FEED_ID).SerializeToString())

    push_parser.run(total_sessions=1)

    data = output_topic.read(count=5)

    ts_expected = Timestamp()
    ts_expected.FromDatetime(date)
    META = UpdateMeta(
        source=DataCampOffer_pb2.PUSH_PARTNER_FEED,
        applier=DataCampOffer_pb2.QPARSER,
        timestamp=ts_expected)

    expected_value_removed = None if not must_be_restored else {
        'flag': False,
        'meta': META,
    }

    assert_that(data, HasSerializedDatacampMessages([{
        'united_offers': [{
            'offer': [{
                'basic': {
                    'identifiers': {
                        'business_id': BUSINESS_ID,
                        'offer_id': '{}xXx{}'.format(FEED_ID, offer_id),
                    },
                    'meta': {
                        'scope': DataCampOffer_pb2.BASIC,
                        'ts_created': ts_expected,
                    }
                },
                'service': IsProtobufMap({
                    SHOP_ID: {
                        'identifiers': {
                            'feed_id': FEED_ID,
                            'real_feed_id': REAL_FEED_ID,
                            'business_id': BUSINESS_ID,
                            'shop_id': SHOP_ID,
                            'offer_id': '{}xXx{}'.format(FEED_ID, offer_id),
                        },
                        'stock_info': {
                            'partner_stocks_default': {
                                'count': offer_id + 1,
                                'meta': META,
                            }
                        },
                        'status': {
                            'removed': expected_value_removed
                        }
                    }
                })
            }]
        }] for offer_id in range(5)
    }]))


def test_qparser_with_force_restore(push_parser_with_force_restore, input_topic, output_topic, mds, datacamp_output_topic, config, qp_runner_with_force_restore):
    run_qparser(push_parser_with_force_restore, input_topic, output_topic, mds, datacamp_output_topic, config, qp_runner_with_force_restore, True)


def test_qparser_without_force_restore(push_parser_without_force_restore, input_topic, output_topic, mds, datacamp_output_topic, config, qp_runner_without_force_restore):
    run_qparser(push_parser_without_force_restore, input_topic, output_topic, mds, datacamp_output_topic, config, qp_runner_without_force_restore, False)
