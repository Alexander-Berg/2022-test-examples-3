# coding: utf-8

import pytest
from hamcrest import assert_that

from market.idx.datacamp.parser.yatf.env import WorkersEnv, make_input_task, UpdateTaskServiceMock
from market.idx.datacamp.parser.yatf.resources.config_mock import PushParserConfigMock
from market.idx.datacamp.yatf.matchers.matchers import HasSerializedDatacampMessages
from market.idx.datacamp.proto.api.UpdateTask_pb2 import FEED_CLASS_COMPLETE

from market.idx.datacamp.parser.yatf.fake_mds import FakeMds
from market.idx.datacamp.parser.yatf.qp_mocks import QParserTestLauncherMock
from market.idx.yatf.matchers.lb_matchers import HasNoUnreadData
from market.idx.yatf.resources.lbk_topic import LbkTopic
from market.idx.yatf.utils.utils import create_pb_timestamp


BUSINESS_ID = 10
SHOP_ID = 111
FEED_ID = 100
WAREHOUSE_ID = 133

COMPLETE_FEED_FINISH_COMMAND_BATCH_SIZE = 10


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
        'general': {
            'complete_feed_finish_command_batch_size': COMPLETE_FEED_FINISH_COMMAND_BATCH_SIZE,
            'categories_batch_size': 0
        },
        'logbroker_in': {
            'topic': input_topic.topic,
        },
        'logbroker': {
            'topic': output_topic.topic,
            'datacamp_messages_topic': datacamp_output_topic.topic
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
def qp_runner(config, log_broker_stuff, datacamp_output_topic):
    yield QParserTestLauncherMock(
        log_broker_stuff=log_broker_stuff,
        config=config,
        qparser_config={
            'logbroker': {
                'datacamp_messages_topic': datacamp_output_topic.topic,
                'complete_feed_finish_command_batch_size': COMPLETE_FEED_FINISH_COMMAND_BATCH_SIZE
            }
        },
        is_corrupted_feed=True
    )


@pytest.fixture()
def push_parser(monkeypatch, config, qp_runner):
    with monkeypatch.context() as m:
        m.setattr("market.idx.datacamp.parser.lib.worker.ParsingTaskWorker.process_task", qp_runner.process_task)

        yield WorkersEnv(
            config=config,
            parsing_service=UpdateTaskServiceMock,
        )


def test_invalid_complete_feed(push_parser, mds, input_topic, output_topic, datacamp_output_topic):
    timestamp = create_pb_timestamp(12345)
    offers_count = 5

    # Ломаем структуру фида, стирая закрывающий тег </yml_catalog>.
    # Все оффера успешно обрабатываются, однако в конце парсинга возникает фатальная ошибка.
    # Комплит команда о завершении фида не отправляется.
    mds.generate_feed(FEED_ID, is_blue=True, is_csv=False, offer_count=offers_count, is_corrupted=True)
    input_task = make_input_task(
        mds,
        FEED_ID,
        BUSINESS_ID,
        SHOP_ID,
        warehouse_id=WAREHOUSE_ID,
        task_type=FEED_CLASS_COMPLETE,
        timestamp=timestamp
    )
    input_topic.write(input_task.SerializeToString())

    push_parser.run(total_sessions=1)

    # Проверяем, что все оффера успешно обработались
    data = output_topic.read(count=offers_count)
    assert_that(data, HasSerializedDatacampMessages([{
        'united_offers': [{
            'offer': [{
                'basic': {
                    'identifiers': {
                        'business_id': BUSINESS_ID,
                        'offer_id': '{}xXx{}'.format(FEED_ID, offer_id),
                    }
                }
            }]
        }] for offer_id in range(offers_count)
    }]))

    # Проверяем, что комплит команда не приходит, +1 из-за сообщения с категориями
    assert_that(datacamp_output_topic, HasNoUnreadData(count=2))
