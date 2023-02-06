# coding: utf-8

import pytest

from hamcrest import assert_that

from market.idx.datacamp.parser.yatf.env import WorkersEnv, make_input_task, UpdateTaskServiceMock
from market.idx.datacamp.parser.yatf.fake_mds import FakeMds
from market.idx.datacamp.parser.yatf.qp_mocks import QParserTestLauncherMock
from market.idx.datacamp.parser.yatf.resources.config_mock import PushParserConfigMock
from market.idx.datacamp.proto.api.UpdateTask_pb2 import (
    ShopsDatParameters,
    FEED_CLASS_UPDATE,
)
import market.idx.datacamp.proto.offer.DataCampOffer_pb2 as DTC
from market.idx.datacamp.yatf.matchers.matchers import HasSerializedDatacampMessages
from market.idx.yatf.matchers.protobuf_matchers import IsProtobufMap
from market.idx.yatf.resources.lbk_topic import LbkTopic

BUSINESS_ID = 10
SHOP_ID = 111
WAREHOUSE_ID = 150
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
def datacamp_output_topic(log_broker_stuff):
    topic = LbkTopic(log_broker_stuff)
    topic.create()
    return topic


@pytest.fixture(scope='module')
def stocks_output_topic(log_broker_stuff):
    topic = LbkTopic(log_broker_stuff)
    topic.create()
    return topic


@pytest.fixture()
def config(tmpdir_factory, log_broker_stuff, yt_server, output_topic, input_topic, stocks_output_topic, datacamp_output_topic):
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
            'stocks_topic' : stocks_output_topic.topic,
            'enable_sending_stocks_to_separate_topic': True,
            'complete_feed_finish_command_batch_size': 1000
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


@pytest.yield_fixture()
def qp_runner(config, log_broker_stuff):
    yield QParserTestLauncherMock(
        log_broker_stuff=log_broker_stuff,
        config=config,
        feed_format='csv'
    )


@pytest.yield_fixture()
def qp_runner_yml(config, log_broker_stuff):
    yield QParserTestLauncherMock(
        log_broker_stuff=log_broker_stuff,
        config=config
    )


@pytest.fixture()
def push_parser_yml(monkeypatch, config, qp_runner_yml):
    with monkeypatch.context() as m:
        m.setattr("market.idx.datacamp.parser.lib.worker.ParsingTaskWorker.process_task", qp_runner_yml.process_task)

        yield WorkersEnv(
            config=config,
            parsing_service=UpdateTaskServiceMock
        )


@pytest.fixture()
def push_parser(monkeypatch, config, qp_runner):
    with monkeypatch.context() as m:
        m.setattr("market.idx.datacamp.parser.lib.worker.ParsingTaskWorker.process_task", qp_runner.process_task)

        yield WorkersEnv(
            config=config,
            parsing_service=UpdateTaskServiceMock
        )


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


@pytest.mark.parametrize('is_synthetic', [False, True])
def test_qparser_synthetic_feed(push_parser_white, input_topic, output_topic, mds, is_synthetic):
    """
    Проверяем, что если задание на парсинг промаркировано флагом синтетичности,
    то выходные офферы и объект-сообщение так же имеют этот флаг.
    И - наоборот - флаг не проставляется, если задание несинтетическое.
    """
    mds.generate_feed(
        FEED_ID,
        is_blue=False,
        offer_count=1,
        is_csv=False
    )

    feed_parsing_task = make_input_task(
        mds,
        FEED_ID,
        BUSINESS_ID,
        SHOP_ID,
        warehouse_id=WAREHOUSE_ID,
        shops_dat_parameters=ShopsDatParameters(
            color=DTC.WHITE,
            is_mock=False,
            is_upload=True,
            vat=7,
        ),
        is_regular_parsing=False,
        task_type=FEED_CLASS_UPDATE,
        is_synthetic=is_synthetic
    )
    input_topic.write(feed_parsing_task.SerializeToString())
    push_parser_white.run(total_sessions=1)
    data = output_topic.read(count=1)

    matcher = HasSerializedDatacampMessages([{
        'tech_info': {
            'synthetic': is_synthetic
        },
        'united_offers': [{
            'offer': [{
                'basic': {
                    'identifiers': {
                        'business_id': BUSINESS_ID,
                        'offer_id': '{}xXx{}'.format(FEED_ID, offer_id),
                    },
                    'meta': {
                        'synthetic': is_synthetic
                    },
                },
                'service': IsProtobufMap({
                    SHOP_ID: {
                        'identifiers': {
                            'feed_id': FEED_ID,
                            'business_id': BUSINESS_ID,
                            'shop_id': SHOP_ID,
                            'warehouse_id': WAREHOUSE_ID,
                            'offer_id': '{}xXx{}'.format(FEED_ID, offer_id),
                        },
                        'meta': {
                            'synthetic': is_synthetic
                        }
                    },
                }),
            } for offer_id in range(1)]
        }]
    }])
    assert_that(data, matcher)
