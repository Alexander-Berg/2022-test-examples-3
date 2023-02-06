# coding: utf-8

import pytest

from hamcrest import assert_that, equal_to

from market.idx.datacamp.parser.yatf.env import WorkersEnv, make_input_task, UpdateTaskServiceMock
from market.idx.datacamp.parser.yatf.fake_mds import FakeMds
from market.idx.datacamp.parser.yatf.qp_mocks import QParserTestLauncherMock
from market.idx.datacamp.parser.yatf.resources.config_mock import PushParserConfigMock

from market.idx.yatf.resources.lbk_topic import LbkTopic
from market.idx.yatf.resources.shops_dat import ShopsDat


BUSINESS_ID = 10
SHOP_ID = 111
FEED_ID = 800


@pytest.fixture(scope='module')
def input_topic(log_broker_stuff):
    topic = LbkTopic(log_broker_stuff, partitions_count=2)
    topic.create()
    return topic


@pytest.fixture(scope='module')
def technical_topic(log_broker_stuff):
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
def mbi_report_topic(log_broker_stuff):
    topic = LbkTopic(log_broker_stuff)
    topic.create()
    return topic


@pytest.fixture(scope='module')
def config(tmpdir_factory, log_broker_stuff, yt_server, input_topic, output_topic, datacamp_output_topic, technical_topic, mbi_report_topic):
    cfg = {
        'logbroker_in': {
            'topic': input_topic.topic,
        },
        'logbroker': {
            'topic': output_topic.topic,
            'datacamp_messages_topic': datacamp_output_topic.topic
        },
        'logbroker_technical': {
            'topic': technical_topic.topic,
        },
        'logbroker_mbi_reports': {
            'topic': mbi_report_topic.topic,
        },
    }

    return PushParserConfigMock(
        workdir=tmpdir_factory.mktemp('workdir'),
        yt_server=yt_server,
        log_broker_stuff=log_broker_stuff,
        config=cfg
    )


@pytest.fixture(scope='module')
def mds(tmpdir_factory, config):
    return FakeMds(tmpdir_factory.mktemp('mds'), config)


@pytest.fixture(scope='module')
def shops_dat():
    return {
        'shop_id': SHOP_ID,
        'business_id': BUSINESS_ID,
        'datafeed_id': FEED_ID,
        'is_push_partner': True,
        'direct_status': 'REAL',
        'direct_standby': True
    }


@pytest.yield_fixture(scope='module')
def qp_runner(config, log_broker_stuff, datacamp_output_topic, shops_dat):
    yield QParserTestLauncherMock(
        config=config,
        log_broker_stuff=log_broker_stuff,
        feed_info={
            'market_color': 'direct',
            'shop_disabled_since_ts': 100
        },
        shops_dat=ShopsDat(
            shops=[shops_dat]
        ),
        qparser_config={
            'logbroker': {
                'datacamp_messages_topic': datacamp_output_topic.topic,
                'datacamp_messages_writers_count': 1,
            },
            'disabled_shop': {
                'direct_ttl': 24
            }
        },
    )


@pytest.fixture(scope='function')
def push_parser(monkeypatch, config, qp_runner):
    with monkeypatch.context() as m:
        m.setattr("market.idx.datacamp.parser.lib.worker.ParsingTaskWorker.launch_parsing_process", qp_runner.launch_parsing_process)

        yield WorkersEnv(
            config=config,
            parsing_service=UpdateTaskServiceMock
        )


def test_disabled_shop(push_parser, input_topic, mds, qp_runner):
    mds.generate_feed(FEED_ID)

    input_topic.write([
        make_input_task(mds, FEED_ID, BUSINESS_ID, SHOP_ID).SerializeToString()
    ])

    push_parser.run(total_sessions=1)
    assert_that(qp_runner.last_ret_code, equal_to(122))
