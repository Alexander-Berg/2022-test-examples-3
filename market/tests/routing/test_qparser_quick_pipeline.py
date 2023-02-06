# coding: utf-8

from datetime import datetime
from hamcrest import assert_that, is_not
import pytest
import six

from market.idx.datacamp.parser.yatf.env import WorkersEnv, make_input_task, UpdateTaskServiceMock
from market.idx.datacamp.parser.yatf.fake_mds import FakeMds
from market.idx.datacamp.parser.yatf.qp_mocks import QParserTestLauncherMock
from market.idx.datacamp.parser.yatf.resources.config_mock import PushParserConfigMock
from market.idx.datacamp.proto.api.UpdateTask_pb2 import FEED_CLASS_SELECTIVE_BASIC_PATCH_UPDATE_SERVICE_FULL_COMPLETE
from market.idx.datacamp.yatf.matchers.matchers import HasSerializedDatacampMessages
from market.idx.yatf.matchers.protobuf_matchers import IsProtobufMap
from market.idx.yatf.resources.lbk_topic import LbkTopic


BUSINESS_ID = 10
SHOP_ID = 101
FEED_ID = 1001


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
def quick_pipeline_topic(log_broker_stuff):
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
def qp_runner_yml(config, log_broker_stuff, datacamp_output_topic, quick_pipeline_topic):
    yield QParserTestLauncherMock(
        log_broker_stuff=log_broker_stuff,
        config=config,
        feed_info={
            'market_color': 'white',
        },
        qparser_config={
            'logbroker': {
                'complete_feed_finish_command_batch_size': 1000,
                'datacamp_messages_topic': datacamp_output_topic.topic,
                'datacamp_messages_writers_count': 1,
                'qoffers_quick_pipeline_messages_topic': quick_pipeline_topic.topic,
                'qoffers_quick_pipeline_messages_writers_count': 1
            },
            'feature': {
                "enable_quick_pipeline": True
            }
        },
    )


@pytest.fixture()
def push_parser_yml(monkeypatch, config, qp_runner_yml):
    with monkeypatch.context() as m:
        m.setattr(
            "market.idx.datacamp.parser.lib.worker.ParsingTaskWorker.process_task",
            qp_runner_yml.process_task
        )

        yield WorkersEnv(
            config=config,
            parsing_service=UpdateTaskServiceMock
        )


def test_qparser_quick_pipeline(push_parser_yml, input_topic, quick_pipeline_topic, output_topic, mds):
    mds.generate_feed(
        FEED_ID,
        is_blue=True,
        is_csv=False,
        offer_count=1,
        offers_dict={
            0: {
                'price': '110',
                'currencyId': 'USD',
                'weight': '1',
                'dimensions': '12211/1212/123123'
            }
        },
        is_advanced_blue=True,
        shop_dict={
            'name': six.ensure_text('Магазин  Audio-Video'),
            'company': 'Audio-Video',
            'date': str(datetime.now()),
        },
    )
    input_topic.write(
        make_input_task(
            mds,
            FEED_ID,
            BUSINESS_ID,
            SHOP_ID,
            task_type=FEED_CLASS_SELECTIVE_BASIC_PATCH_UPDATE_SERVICE_FULL_COMPLETE
        ).SerializeToString()
    )
    push_parser_yml.run(total_sessions=1)
    data = quick_pipeline_topic.read(count=1)

    full_data = output_topic.read(count=1)
    assert_that(data, HasSerializedDatacampMessages([{
        'united_offers': [{
            'offer': [{
                'basic': {
                    'identifiers': {
                        'business_id': BUSINESS_ID,
                        'offer_id': '1001xXx0'
                    },
                },
                'service': IsProtobufMap({
                    101: {
                        'identifiers': {
                            'business_id': BUSINESS_ID,
                            'offer_id': '1001xXx0'
                        },
                        'price': {
                            'basic': {
                                'binary_price': {
                                    'price': 1100000000,
                                    'rate': 'CBRF',
                                    'id': 'USD'
                                },
                            }
                        }
                    }
                })
            }]
        }]}
    ]))

    assert_that(full_data, HasSerializedDatacampMessages([{
        'united_offers': [{
            'offer': [{
                'basic': {
                    'identifiers': {
                        'business_id': BUSINESS_ID,
                        'offer_id': '1001xXx0'
                    },
                    'content': {
                        'partner': {
                            'original': {
                                'weight': {
                                    'value_mg': 1000000
                                },
                                'dimensions': {
                                    'width_mkm': 20000,
                                    'length_mkm': 10000,
                                    'height_mkm': 30000
                                }
                            }
                        }
                    }
                }
            }]
        }]}
    ]))

    assert_that(data, is_not(HasSerializedDatacampMessages([{
        'united_offers': [{
            'offer': [{
                'basic': {
                    'identifiers': {
                        'business_id': BUSINESS_ID,
                        'offer_id': '1001xXx0',
                    },
                    'content': {
                        'partner': {
                            'original': {
                                'weight': {
                                    'value_mg': 1000000
                                },
                                'dimensions': {
                                    'width_mkm': 20000,
                                    'length_mkm': 10000,
                                    'height_mkm': 30000
                                }
                            }
                        }
                    }
                },
            }]
        }]}
    ])))
