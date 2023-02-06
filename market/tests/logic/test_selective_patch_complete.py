# coding: utf-8

import pytest
from hamcrest import assert_that
from datetime import datetime

from google.protobuf.timestamp_pb2 import Timestamp

from market.idx.datacamp.parser.yatf.env import WorkersEnv, make_input_task, UpdateTaskServiceMock
from market.idx.datacamp.parser.yatf.fake_mds import FakeMds
from market.idx.datacamp.parser.yatf.qp_mocks import QParserTestLauncherMock
from market.idx.datacamp.proto.api.UpdateTask_pb2 import FEED_CLASS_SELECTIVE_BASIC_PATCH_UPDATE_SERVICE_PATCH_COMPLETE
from market.idx.datacamp.proto.offer.TechCommands_pb2 import COMPLETE_FEED_FINISHED
from market.idx.datacamp.proto.offer import DataCampOffer_pb2

from market.idx.datacamp.parser.yatf.resources.config_mock import PushParserConfigMock
from market.idx.datacamp.yatf.matchers.matchers import HasSerializedDatacampMessages
from market.idx.yatf.matchers.protobuf_matchers import IsProtobufMap
from market.idx.yatf.resources.lbk_topic import LbkTopic


SHOP_ID = 111
BUSINESS_ID = 3824

NOW = datetime.utcnow()
TIMESTAMP = Timestamp()
TIMESTAMP.FromDatetime(NOW)


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
        'logbroker': {
            'topic': output_topic.topic,
            'datacamp_messages_topic': datacamp_output_topic.topic
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
def qp_runner(config, log_broker_stuff, datacamp_output_topic):
    yield QParserTestLauncherMock(
        log_broker_stuff=log_broker_stuff,
        config=config,
        qparser_config={
            'logbroker': {
                'datacamp_messages_topic': datacamp_output_topic.topic,
                'datacamp_messages_writers_count': 1,
            },
        },
        color='blue',
        feed_format='csv',
    )


@pytest.fixture()
def push_parser(monkeypatch, config, qp_runner):
    with monkeypatch.context() as m:
        m.setattr("market.idx.datacamp.parser.lib.worker.ParsingTaskWorker.process_task", qp_runner.process_task)

        yield WorkersEnv(
            config=config,
            parsing_service=UpdateTaskServiceMock
        )


def create_expected_tech_command(feed_id, supplemental_id, timestamp, offers):
    tech_command = {
        'command_type': COMPLETE_FEED_FINISHED,
        'timestamp': {
            'seconds': timestamp.seconds,
        },
        'command_params': {
            'shop_id': SHOP_ID,
            'business_id': BUSINESS_ID,
            'supplemental_id': supplemental_id,
            'feed_id': feed_id,
            'complete_feed_command_params': {
                'untouchable_offers': [
                    offer['offer_id'] for offer in offers if offer['feed_id'] == feed_id
                ],
                'default_offer_values': {
                    'status': {
                        'disabled': [
                            {
                                'flag': True,
                                'meta': {
                                    'source': DataCampOffer_pb2.PUSH_PARTNER_FEED,
                                    'timestamp': {
                                        'seconds': timestamp.seconds,
                                    },
                                },
                            }
                        ]
                    }
                }
            }
        }
    }

    return tech_command


def test_parsing_task_for_complete_feed(push_parser, mds, input_topic, output_topic, datacamp_output_topic):
    offer_count = 10
    feed_id = 123
    parsing_task_warehouse_id = 1234
    tech_commands_supplemental_id = 1234

    mds.generate_feed(feed_id, offer_count=offer_count, is_blue=True, is_csv=True)

    feed_type = FEED_CLASS_SELECTIVE_BASIC_PATCH_UPDATE_SERVICE_PATCH_COMPLETE
    feed_parsing_task = make_input_task(
        mds,
        feed_id,
        BUSINESS_ID,
        SHOP_ID,
        warehouse_id=parsing_task_warehouse_id,
        task_type=feed_type,
        timestamp=TIMESTAMP,
    )

    input_topic.write(feed_parsing_task.SerializeToString())

    push_parser.run(total_sessions=1)

    output = output_topic.read(count=offer_count)

    assert_that(output, HasSerializedDatacampMessages([{
        'united_offers': [
            {
                'offer': [
                    {
                        'basic': {
                            'identifiers': {
                                'business_id': BUSINESS_ID,
                                'offer_id': '{feed_id}xXx{offer_id}'.format(feed_id=feed_id, offer_id=offer_id)
                            },
                            'content': {
                                'partner': {
                                    'original': {
                                        'name': {
                                            'value': 'offer {feed_id}xXx{offer_id}'.format(feed_id=feed_id, offer_id=offer_id)
                                        },
                                        # явно проверяем, что пустое базовое поле не заполняется метой
                                        'vendor': None
                                    }
                                }
                            }
                        },
                        'service': IsProtobufMap({
                            SHOP_ID: {
                                'identifiers': {
                                    'feed_id': feed_id,
                                    'offer_id': '{feed_id}xXx{offer_id}'.format(feed_id=feed_id, offer_id=offer_id),
                                },
                                'content': {
                                    'partner': {
                                        # явно проверяем, что пустое сервисное поле не заполняется метой
                                       'original_terms': None
                                    }
                                }
                            }
                        })
                    }
                ]
            }
        ]
    } for offer_id in range(offer_count)]))

    expected = [{
        'feed_id': int(feed_id),
        'offer_id': '{}xXx{}'.format(feed_id, offer_id)
    } for offer_id in range(offer_count)]
    expected_command = create_expected_tech_command(
        feed_id=feed_id,
        supplemental_id=tech_commands_supplemental_id,
        timestamp=TIMESTAMP,
        offers=sorted(expected, key=lambda x: x['offer_id'])
    )

    datacamp_output_topic_messages = datacamp_output_topic.read(count=2)  # одна категория и одна комлит команда

    # check that command was created with correct structure
    assert_that(datacamp_output_topic_messages, HasSerializedDatacampMessages([{
        'tech_command': [
            expected_command
        ]
    }]))
