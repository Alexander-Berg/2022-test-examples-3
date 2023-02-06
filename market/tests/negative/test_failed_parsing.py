# coding: utf-8

import pytest

from market.idx.datacamp.parser.yatf.env import WorkersEnv, make_input_task, UpdateTaskServiceMock
from market.idx.datacamp.parser.yatf.fake_mds import FakeMds
from market.idx.datacamp.parser.yatf.qp_mocks import QParserTestLauncherMock
from market.idx.datacamp.parser.yatf.resources.config_mock import PushParserConfigMock
from market.idx.datacamp.parser.yatf.utils import assert_parser_topic_data
from market.idx.datacamp.proto.api.UpdateTask_pb2 import FeedParsingTask

from market.idx.yatf.resources.lbk_topic import LbkTopic


BUSINESS_ID = 10
SHOP_ID = 111


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


@pytest.yield_fixture(scope='module')
def qp_runner(config, log_broker_stuff, datacamp_output_topic):
    yield QParserTestLauncherMock(
        config=config,
        log_broker_stuff=log_broker_stuff,
        qparser_config={
            'logbroker': {
                'datacamp_messages_topic': datacamp_output_topic.topic,
                'datacamp_messages_writers_count': 1,
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


def test_trash_input(push_parser, input_topic, output_topic, mds):
    """
    Что проверяем - что исходная задача в кривом формате не ломает работу, а просто пропускается
    """
    feed_id = 700

    mds.generate_feed(feed_id)

    malformed_task = FeedParsingTask(ts_ms=1)

    input_topic.write([
        malformed_task.SerializeToString(),
        make_input_task(mds, feed_id, BUSINESS_ID, SHOP_ID).SerializeToString()
    ])

    push_parser.run(total_sessions=2)

    data = output_topic.read(count=5)

    # Из топика парсера получаем офферы валидного фида feed_id
    offer_ids = ['{}xXx{}'.format(feed_id, offer_id) for offer_id in range(5)]
    assert_parser_topic_data(data, BUSINESS_ID, SHOP_ID, offer_ids)


def test_session_fail(push_parser, input_topic, output_topic, mds):
    """
    Что проверяем - что падение сессии не ломает работу
    """
    feed_id = 800
    mds.generate_feed(feed_id)

    input_topic.write([
        make_input_task(mds, 404, BUSINESS_ID, SHOP_ID).SerializeToString(),  # no feed 404 in mds!
        make_input_task(mds, feed_id, BUSINESS_ID, SHOP_ID).SerializeToString()
    ])

    push_parser.run(total_sessions=2)

    data = output_topic.read(count=5)

    # Из топика парсера получаем офферы валидного фида feed_id
    offer_ids = ['{}xXx{}'.format(feed_id, offer_id) for offer_id in range(5)]
    assert_parser_topic_data(data, BUSINESS_ID, SHOP_ID, offer_ids)


def test_bad_feed(push_parser, input_topic, output_topic, mds):
    """
    Что проверяем - что падение qparser-a не ломает работу
    """
    feed_id = 1000
    mds.generate_feed(900, bad=True)  # <price>qwe</price>
    mds.generate_feed(feed_id)

    input_topic.write([
        make_input_task(mds, 900, BUSINESS_ID, SHOP_ID).SerializeToString(),
        make_input_task(mds, feed_id, BUSINESS_ID, SHOP_ID).SerializeToString()
    ])

    push_parser.run(total_sessions=2)

    # Логика qparser отличается
    data = output_topic.read(count=10)

    # Из топика парсера получаем офферы валидного фида feed_id
    offer_ids = ['{}xXx{}'.format(feed_id, offer_id) for offer_id in range(5)]
    assert_parser_topic_data(data, BUSINESS_ID, SHOP_ID, offer_ids)

    # В Qparser логе 551 - No Valid Offers, при этом в топик приходят офферы от фида 900. Насколько это ожидаемое поведение?
    offer_ids = ['{}xXx{}'.format(900, offer_id) for offer_id in range(5)]
    assert_parser_topic_data(data, BUSINESS_ID, SHOP_ID, offer_ids)


def test_parsing_task_with_no_basic_options(push_parser, input_topic, output_topic, mds):
    """
    Что проверяем - что задание на парсинг без таймстемпа, shop_id, типа будет отброшено
    """
    feed_id = 1200
    feed_id_2 = 1300

    mds.generate_feed(1100)
    mds.generate_feed(1110)
    mds.generate_feed(1120)
    mds.generate_feed(feed_id)
    mds.generate_feed(feed_id_2)

    input_topic.write([
        make_input_task(mds, 1100, BUSINESS_ID, SHOP_ID, timestamp=None).SerializeToString(),  # bad formed task (no ts)
        make_input_task(mds, 1110, BUSINESS_ID, SHOP_ID, task_type=None).SerializeToString(),  # bad formed task (no type)
        make_input_task(mds, 1120, BUSINESS_ID, shop_id=None).SerializeToString(),  # bad formed task (no shop_id)
        make_input_task(mds, feed_id,   BUSINESS_ID, SHOP_ID).SerializeToString(),
        make_input_task(mds, feed_id_2, BUSINESS_ID, SHOP_ID, legacy_ts=True).SerializeToString(),
    ])

    push_parser.run(total_sessions=5)

    data = output_topic.read(count=10)

    # Из топика парсера получаем офферы фидов feed_id и feed_id_2
    offer_ids = ['{}xXx{}'.format(feed_id, offer_id) for offer_id in range(5)]
    offer_ids += ['{}xXx{}'.format(feed_id_2, offer_id) for offer_id in range(5)]
    assert_parser_topic_data(data, BUSINESS_ID, SHOP_ID, offer_ids)
