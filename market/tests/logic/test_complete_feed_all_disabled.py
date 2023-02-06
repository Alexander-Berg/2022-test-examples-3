# coding: utf-8

import pytest
from hamcrest import assert_that, equal_to

from market.idx.datacamp.parser.yatf.env import WorkersEnv, make_input_task, UpdateTaskServiceMock
from market.idx.datacamp.parser.yatf.fake_mds import FakeMds
from market.idx.datacamp.parser.yatf.qp_mocks import QParserTestLauncherMock
from market.idx.datacamp.parser.yatf.utils import assert_parser_topic_data
from market.idx.datacamp.proto.api.UpdateTask_pb2 import FEED_CLASS_SELECTIVE_BASIC_PATCH_UPDATE_SERVICE_FULL_COMPLETE
from market.idx.datacamp.proto.offer.TechCommands_pb2 import COMPLETE_FEED_FINISHED
from market.idx.datacamp.proto.offer.TechCommands_pb2 import TechCommand, TechCommandParams, CompleteFeedCommandParams
from market.idx.datacamp.proto.offer.DataCampOffer_pb2 import Offer as DataCampOffer
from market.idx.datacamp.proto.offer.DataCampOffer_pb2 import OfferStatus, Flag, UpdateMeta
from market.idx.datacamp.proto.offer import DataCampOffer_pb2
from market.proto.common.common_pb2 import EComponent

from market.idx.datacamp.parser.yatf.resources.config_mock import PushParserConfigMock
from market.idx.datacamp.yatf.matchers.matchers import HasSerializedDatacampMessages
from market.idx.yatf.resources.lbk_topic import LbkTopic
from market.idx.yatf.utils.utils import create_pb_timestamp


BUSINESS_ID = 10
SHOP_ID = 111
COMPLETE_FEED_FINISH_COMMAND_BATCH_SIZE = 10

TIMESTAMP = create_pb_timestamp(12345)


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
def config(tmpdir_factory, log_broker_stuff, yt_server, input_topic, output_topic, datacamp_output_topic):
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


@pytest.fixture(
    params=[
        True,
        False
    ],
    ids=[
        'csv',
        'yml'
    ]
)
def is_csv(request):
    return request.param


@pytest.yield_fixture()
def qp_runner(is_csv, config, log_broker_stuff):
    launcher = QParserTestLauncherMock(
        config=config,
        log_broker_stuff=log_broker_stuff,
        feed_format='csv' if is_csv else 'yml'
    )

    yield launcher


@pytest.fixture()
def push_parser(monkeypatch, config, qp_runner):
    with monkeypatch.context() as m:
        m.setattr("market.idx.datacamp.parser.lib.worker.ParsingTaskWorker.launch_parsing_process", qp_runner.launch_parsing_process)

        yield WorkersEnv(
            config=config,
            parsing_service=UpdateTaskServiceMock
        )


def create_expected_tech_commands(feed_id, supplemental_id, timestamp, offers):
    batches = []
    for i in range(0, len(offers), COMPLETE_FEED_FINISH_COMMAND_BATCH_SIZE):
        batches.append(offers[i:i + COMPLETE_FEED_FINISH_COMMAND_BATCH_SIZE])

    if len(batches) == 0:
        # if there is no offers in feed it means that partner wants to disable all his offers
        # that would be one command to finish complete feed without untouchable offers
        batches.append([])

    expected_tech_commands = []
    for i, batch in enumerate(batches):
        tech_command = TechCommand(
            timestamp=timestamp,
            command_type=COMPLETE_FEED_FINISHED,
            command_params=TechCommandParams(
                business_id=BUSINESS_ID,
                shop_id=SHOP_ID,
                supplemental_id=supplemental_id,
                feed_id=feed_id,
                complete_feed_command_params=CompleteFeedCommandParams(
                    untouchable_offers=[offer['offer_id'] for offer in batch if offer['feed_id'] == feed_id],
                    is_upload_feed=False,
                    default_offer_values=DataCampOffer(
                        status=OfferStatus(
                            disabled=[
                                Flag(
                                    flag=True,
                                    meta=UpdateMeta(
                                        source=DataCampOffer_pb2.PUSH_PARTNER_FEED,
                                        timestamp=timestamp,
                                        applier=EComponent.COMPLETE_OPERATION
                                    )
                                ),
                            ]
                        ),
                    )
                )
            )
        )
        if i != 0:
            tech_command.command_params.complete_feed_command_params.start_offer_id = batch[0]['offer_id']

        if i != len(batches) - 1:
            tech_command.command_params.complete_feed_command_params.last_offer_id = batches[i + 1][0]['offer_id']

        expected_tech_commands.append(tech_command)

    return expected_tech_commands


def test_blue_complete_feed_all_offers_disabled(is_csv, push_parser, mds, input_topic, output_topic, datacamp_output_topic):
    """
    Проверяем случай с фидом, в котором все офферы с disabled=true.
    Все оффера будут отправлены в топик с сторону пайпера,
    а так же туда будет отправлена команда о завершении комплит фида.
    """
    offer_count = COMPLETE_FEED_FINISH_COMMAND_BATCH_SIZE
    categories_count = 1  # dont change https://st.yandex-team.ru/MARKETINDEXER-36993
    feed_id = 100500
    parsing_task_warehouse_id = 123
    tech_commands_supplemental_id = 123

    mds.generate_feed(feed_id, offer_count=offer_count, categories_count=categories_count, is_blue=True, is_csv=is_csv, all_disabled=True)

    feed_parsing_task = make_input_task(
        mds,
        feed_id,
        BUSINESS_ID,
        SHOP_ID,
        warehouse_id=parsing_task_warehouse_id,
        task_type=FEED_CLASS_SELECTIVE_BASIC_PATCH_UPDATE_SERVICE_FULL_COMPLETE,
        timestamp=TIMESTAMP
    )

    input_topic.write(feed_parsing_task.SerializeToString())

    push_parser.run(total_sessions=1)

    output = output_topic.read(count=offer_count)

    # check that offers were correctly sent
    offer_ids = ['{}xXx{}'.format(feed_id, offer_id) for offer_id in range(offer_count)]
    assert_parser_topic_data(output, BUSINESS_ID, SHOP_ID, offer_ids)

    # test new topic
    expected = [{
        'feed_id': int(feed_id),
        'offer_id': '{}xXx{}'.format(feed_id, offer_id)
    } for offer_id in range(offer_count)]
    expected_commands = create_expected_tech_commands(
        feed_id=feed_id,
        supplemental_id=tech_commands_supplemental_id,
        timestamp=TIMESTAMP,
        offers=sorted(expected, key=lambda x: x['offer_id'])
    )

    datacamp_output_topic_messages = datacamp_output_topic.read(count=len(expected_commands)+1)

    # check categories send https://st.yandex-team.ru/MARKETINDEXER-36993
    assert_that(datacamp_output_topic_messages, HasSerializedDatacampMessages([{
        'partner_categories': [{
            'categories': [
                {
                    'business_id': BUSINESS_ID,
                    'name': 'category 1' if is_csv else 'root category',  # values from FakeMds
                },
            ]
        }]
    }]))

    # check that command was created with correct structure
    assert_that(datacamp_output_topic_messages[1:], HasSerializedDatacampMessages([{
        'tech_command': [
            equal_to(expected_command)
        ]
    } for expected_command in expected_commands]))
