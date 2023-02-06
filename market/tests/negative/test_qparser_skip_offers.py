# coding: utf-8

import pytest

from concurrent.futures import TimeoutError

from market.idx.datacamp.parser.yatf.env import WorkersEnv, make_input_task, UpdateTaskServiceMock
from market.idx.datacamp.parser.yatf.fake_mds import FakeMds
from market.idx.datacamp.parser.yatf.qp_mocks import QParserTestLauncherMock
from market.idx.datacamp.parser.yatf.resources.config_mock import PushParserConfigMock
from market.idx.datacamp.proto.api.UpdateTask_pb2 import FEED_CLASS_SELECTIVE_BASIC_PATCH_UPDATE_SERVICE_PATCH_UPDATE
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


@pytest.fixture()
def config(tmpdir_factory, log_broker_stuff, yt_server, output_topic, input_topic, datacamp_output_topic):
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
            'complete_feed_finish_command_batch_size': 1000,
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
        feed_format='csv',
        feed_info={
            'cpa': 'REAL',
        },
    )


@pytest.fixture()
def push_parser(monkeypatch, config, qp_runner):
    with monkeypatch.context() as m:
        m.setattr("market.idx.datacamp.parser.lib.worker.ParsingTaskWorker.process_task", qp_runner.process_task)

        yield WorkersEnv(
            config=config,
            parsing_service=UpdateTaskServiceMock
        )


@pytest.fixture(scope='function', params=[
    FEED_CLASS_SELECTIVE_BASIC_PATCH_UPDATE_SERVICE_PATCH_UPDATE
])
def feed_type(request):
    return request.param


def test_qparser_skip_cpc_offer(push_parser, input_topic, output_topic, mds, feed_type):
    """
    Check that cpc offers is correctly skipped for cpa shop
    """

    mds.generate_feed(FEED_ID, is_blue=True, offer_count=1, is_advanced_blue=True, is_csv=True, cpa=False)

    feed_parsing_task = make_input_task(
        mds,
        FEED_ID,
        BUSINESS_ID,
        SHOP_ID,
        warehouse_id=WAREHOUSE_ID,
        task_type=feed_type,
    )
    input_topic.write(feed_parsing_task.SerializeToString())
    push_parser.run(total_sessions=1)

    with pytest.raises(TimeoutError):
        output_topic.read(count=1)
