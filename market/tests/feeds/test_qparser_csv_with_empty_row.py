# coding: utf-8

import pytest
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


import yatest.common


BUSINESS_ID = 10
SHOP_ID = 111
FEED_ID = 100
TIMESTAMP = create_pb_timestamp(100500)
META = UpdateMeta(
    source=DataCampOffer_pb2.PUSH_PARTNER_FEED,
    applier='QPARSER',
    timestamp=TIMESTAMP
)


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


@pytest.yield_fixture(scope='module')
def qp_runner(config, log_broker_stuff):
    yield QParserTestLauncherMock(
        log_broker_stuff=log_broker_stuff,
        config=config,
        feed_format='csv',
    )


@pytest.fixture(scope='module')
def push_parser(monkeymodule, config, qp_runner):
    with monkeymodule.context() as m:
        m.setattr("market.idx.datacamp.parser.lib.worker.ParsingTaskWorker.process_task", qp_runner.process_task)

        yield WorkersEnv(
            config=config,
            parsing_service=UpdateTaskServiceMock
        )


def test_qparser_csv_with_empty_row(push_parser, input_topic, output_topic, mds, datacamp_output_topic, config, qp_runner):
    """Тест проверяет, что csv фид с пустой колонкой (некоторый партнеры в конце хедера ставят ';') обработается нормально """
    business_id = 10000
    shop_id = 12345
    feed_id = 54321
    mds.setup_push_feed(
        feed_id,
        yatest.common.source_path('market/idx/datacamp/parser/tests/feeds/data/MARKETINDEXER-37806.csv')
    )

    parsing_feed_task = make_input_task(
        mds,
        feed_id,
        business_id,
        shop_id,
        timestamp=TIMESTAMP
    )

    input_topic.write(parsing_feed_task.SerializeToString())

    push_parser.run(total_sessions=1)

    data = output_topic.read(count=5)
    expected_offer_ids = ['024391', '0294591', '026448', '024122', '029468']
    assert_that(data, HasSerializedDatacampMessages([{
        'united_offers': [{
            'offer': [{
                'basic': {
                    'identifiers': {
                        'business_id': business_id,
                        'offer_id': offer_id,
                    }
                },
                'service': IsProtobufMap({
                    shop_id: {
                        'identifiers': {
                            'feed_id': feed_id,
                            'business_id': business_id,
                            'shop_id': shop_id,
                            'offer_id': offer_id,
                        },
                    }
                })
            }]
        }] for offer_id in expected_offer_ids
    }]))
