# coding: utf-8

import pytest
import itertools

from market.idx.datacamp.parser.lib.parsing_services import UpdateTaskService
from market.idx.datacamp.parser.yatf.env import WorkersEnv, make_input_task
from market.idx.datacamp.parser.yatf.resources.config_mock import PushParserConfigMock
from market.idx.datacamp.parser.yatf.utils import assert_parser_topic_data

from market.idx.yatf.resources.lbk_topic import LbkTopic

from market.idx.datacamp.parser.yatf.fake_mds import FakeMds
from market.idx.datacamp.parser.yatf.qp_mocks import QParserTestLauncherMock


BUSINESS_ID = 10
SHOP_ID = 111


@pytest.fixture(scope='function')
def input_topic(log_broker_stuff):
    topic = LbkTopic(log_broker_stuff, partitions_count=2)
    topic.create()
    return topic


@pytest.fixture(scope='function')
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
def config(tmpdir_factory, log_broker_stuff, yt_server, input_topic, output_topic, datacamp_output_topic):
    cfg = {
        'logbroker_in': {
            'topic': input_topic.topic,
        },
        'logbroker': {
            'topic': output_topic.topic,
            'datacamp_messages_topic': datacamp_output_topic.topic
        },
        'general': {
            'concurrent_process': 2,
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
def qp_runner(config, log_broker_stuff):
    launcher = QParserTestLauncherMock(
        config=config,
        log_broker_stuff=log_broker_stuff,
    )

    yield launcher


@pytest.fixture()
def push_parser(monkeypatch, config, qp_runner):
    with monkeypatch.context() as m:
        m.setattr("market.idx.datacamp.parser.lib.worker.ParsingTaskWorker.launch_parsing_process", qp_runner.launch_parsing_process)

        yield WorkersEnv(
            config=config,
            parsing_service=UpdateTaskService
        )


def test_parallel_parsers(push_parser, input_topic, output_topic, mds):
    """Проверяем, что парсер корректно работает с более чем одним воркером для обработки"""

    mds.generate_feed(300)
    mds.generate_feed(400)
    mds.generate_feed(500)
    mds.generate_feed(600)

    input_topic.write(
        data=[
            make_input_task(mds, 300, BUSINESS_ID, SHOP_ID).SerializeToString(),
            make_input_task(mds, 400, BUSINESS_ID, SHOP_ID).SerializeToString(),
        ],
        partition=1
    )

    input_topic.write(
        data=[
            make_input_task(mds, 500, BUSINESS_ID, SHOP_ID).SerializeToString(),
            make_input_task(mds, 600, BUSINESS_ID, SHOP_ID).SerializeToString(),
        ],
        partition=2
    )

    push_parser.run(total_sessions=4)

    data = output_topic.read(count=20)

    offer_ids = ['{}xXx{}'.format(feed_id, offer_id) for feed_id, offer_id in itertools.product([300, 400, 500, 600], list(range(5)))]
    assert_parser_topic_data(data, BUSINESS_ID, SHOP_ID, offer_ids)
