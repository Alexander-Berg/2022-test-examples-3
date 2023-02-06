# coding: utf-8

import pytest
from hamcrest import assert_that, equal_to, has_items

from market.idx.datacamp.parser.yatf.env import WorkersEnv, TechnicalTaskServiceMock
from market.idx.datacamp.parser.yatf.qp_mocks import QParserTestLauncherMock, QParserFailedTestLauncher
from market.idx.datacamp.parser.yatf.env import make_input_task, make_reparsing_task

from market.idx.datacamp.parser.yatf.fake_mds import FakeMds
from market.idx.datacamp.parser.yatf.resources.config_mock import PushParserConfigMock
from market.idx.datacamp.parser.yatf.utils import assert_parser_topic_data
from market.idx.yatf.matchers.protobuf_matchers import IsSerializedProtobuf
from market.idx.yatf.resources.lbk_topic import LbkTopic
from market.idx.yatf.utils.utils import create_pb_timestamp

from market.idx.datacamp.proto.api.GeneralizedMessage_pb2 import GeneralizedMessage
from market.idx.datacamp.proto.api.UpdateTask_pb2 import FPR_RETRY, FPR_FAIL


BUSINESS_ID = 10
SHOP_ID = 111


@pytest.fixture(scope='function')
def technical_topic(log_broker_stuff):
    topic = LbkTopic(log_broker_stuff)
    topic.create()
    return topic


@pytest.fixture(scope='function')
def output_topic(log_broker_stuff):
    topic = LbkTopic(log_broker_stuff)
    topic.create()
    return topic


@pytest.fixture(scope='function')
def mbi_report_topic(log_broker_stuff):
    topic = LbkTopic(log_broker_stuff)
    topic.create()
    return topic


@pytest.fixture(scope='function')
def datacamp_output_topic(log_broker_stuff):
    topic = LbkTopic(log_broker_stuff)
    topic.create()
    return topic


@pytest.fixture(scope='function')
def config(tmpdir_factory, log_broker_stuff, yt_server, output_topic, technical_topic, mbi_report_topic, datacamp_output_topic):
    cfg = {
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


@pytest.fixture(scope='function')
def mds(tmpdir_factory, config):
    return FakeMds(tmpdir_factory.mktemp('mds'), config)


@pytest.fixture(scope='function')
def config_one_session(tmpdir_factory, log_broker_stuff, yt_server, output_topic, technical_topic, mbi_report_topic):
    cfg = {
        'logbroker': {
            'topic': output_topic.topic,
            'max_read_count': 1,
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


@pytest.yield_fixture(scope='function')
def qp_runner(config, log_broker_stuff):
    launcher = QParserTestLauncherMock(
        log_broker_stuff=log_broker_stuff,
        config=config,
    )

    yield launcher


@pytest.fixture(scope='function')
def push_parser(monkeypatch, config, qp_runner):
    with monkeypatch.context() as m:
        m.setattr("market.idx.datacamp.parser.lib.worker.TechnicalWorker.launch_parsing_process", qp_runner.launch_parsing_process)

        yield WorkersEnv(
            config=config,
            parsing_service=TechnicalTaskServiceMock
        )


@pytest.fixture(scope='function')
def push_parser_one_session(monkeypatch, config_one_session, qp_runner):
    with monkeypatch.context() as m:
        m.setattr("market.idx.datacamp.parser.lib.worker.TechnicalWorker.launch_parsing_process", qp_runner.launch_parsing_process)

        yield WorkersEnv(
            config=config_one_session,
            parsing_service=TechnicalTaskServiceMock
        )


@pytest.fixture(scope='function')
def push_parser_failed(monkeypatch, config):
    with monkeypatch.context() as m:
        failed_launcher = QParserFailedTestLauncher()
        m.setattr("market.idx.datacamp.parser.lib.worker.TechnicalWorker.launch_parsing_process", failed_launcher.launch_parsing_process)

        yield WorkersEnv(
            config=config,
            parsing_service=TechnicalTaskServiceMock
        )


def test_technical_topic(push_parser, technical_topic, output_topic, mds):
    """Проверяем, что из технического топика вытаскиваем задание на перепарсинг и корректно обрабатываем его"""
    feed_id = 1000
    mds.generate_feed(feed_id)
    task_for_reparsing = make_reparsing_task(make_input_task(mds, feed_id, BUSINESS_ID, SHOP_ID))

    technical_topic.write(task_for_reparsing.SerializeToString())

    push_parser.run(total_sessions=1)

    data = output_topic.read(count=5)

    offer_ids = ['{}xXx{}'.format(feed_id, offer_id) for offer_id in range(5)]
    assert_parser_topic_data(data, BUSINESS_ID, SHOP_ID, offer_ids)


def test_old_task_is_skipped(push_parser_failed, technical_topic, mds, mbi_report_topic):
    """Проверяем, что старое задание из технического топика больше не пойдет на ретрай, в отличие от более свежего"""
    feed_id_new = 1020
    feed_id_old = 1010
    mds.generate_feed(feed_id_old)
    mds.generate_feed(feed_id_new)

    # старое задание, которое не должно быть обработано
    task_for_reparsing_old = make_reparsing_task(make_input_task(mds, feed_id_old, BUSINESS_ID, SHOP_ID, timestamp=create_pb_timestamp(100)))
    # свежее задание, должно быть обработано
    task_for_reparsing = make_reparsing_task(make_input_task(mds, feed_id_new, BUSINESS_ID, SHOP_ID))

    technical_topic.write([
        task_for_reparsing_old.SerializeToString(),
        task_for_reparsing.SerializeToString()
    ])

    push_parser_failed.run(total_sessions=2)

    mbi_topic = mbi_report_topic.read(count=2)
    assert_that(mbi_topic, has_items(
        IsSerializedProtobuf(GeneralizedMessage, {
            'feed_parsing_task_report': {
                'feed_parsing_result': FPR_RETRY,
                'feed_parsing_error_text': 'Test how we deal with very bad situations',
                'feed_parsing_task': {
                    'shop_id': SHOP_ID,
                    'feed_id': feed_id_new,
                },
            }
        }),
        IsSerializedProtobuf(GeneralizedMessage, {
            'feed_parsing_task_report': {
                'feed_parsing_result': FPR_FAIL,
                'feed_parsing_error_text': 'some fatal errors in push-parser for very old task: Test how we deal with very bad situations',
                'feed_parsing_task': {
                    'shop_id': SHOP_ID,
                    'feed_id': feed_id_old,
                },
            }
        }),
    ))


def test_task_processed_with_errors_retry_again(push_parser_one_session, technical_topic, mds):
    """Проверяем, что плохо обработанное задание на парсинг снова будет поставлено в технический топик"""
    task_for_reparsing = make_input_task(mds, 1030, BUSINESS_ID, SHOP_ID, real_feed_id=8765)  # can not download this feed

    technical_topic.write(make_reparsing_task(task_for_reparsing).SerializeToString())
    push_parser_one_session.run(total_sessions=1)

    actual_task = technical_topic.read(count=1)
    assert_that(actual_task[0], IsSerializedProtobuf(GeneralizedMessage, {
        'feed_parsing_task': equal_to(task_for_reparsing)
    }))
