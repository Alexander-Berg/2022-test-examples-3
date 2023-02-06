# coding: utf-8

from datetime import datetime
from hamcrest import assert_that
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
def qp_runner_yml(config, log_broker_stuff, datacamp_output_topic):
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


def test_qparser_white_yml(push_parser_yml, input_topic, output_topic, mds):
    mds.generate_feed(
        FEED_ID,
        is_blue=False,
        is_csv=False,
        offer_count=3,
        offers_dict={
            0: {
                'pickup-options': [{
                    'cost': '100100100',  # ошибка по большой стоимости доставки пороставится в майнере
                    'days': '2'
                }]
            },
            1: {
                'pickup-options': [{
                    'cost': '200.0',
                    'days': '2-3'
                }]
            }
        },
        is_advanced_blue=True,
        shop_dict={
            'name': six.ensure_text('Магазин  Audio-Video'),
            'company': 'Audio-Video',
            'url': 'http://www.aydio-video.ru',
            'local_delivery_cost': '100',
            'cpa': '1',
            'date': str(datetime.now()),
            'pickup': 'true',
            'store': 'true',
            'delivery': 'true',
            'pickup-options': [{
                'cost': '100.0'
            }],
            'adult': 'yes',
            'enable_auto_discounts': 'true',
            'deliveryIncluded': 'true'
        },
        currencies=[
            {
                'id': 'RUB',
                'rate': '1'
            },
        ]
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
    data = output_topic.read(count=3)

    assert_that(data, HasSerializedDatacampMessages([
        {
            'united_offers': [{
                'offer': [{
                    'basic': {
                        'identifiers': {
                            'business_id': BUSINESS_ID,
                            'offer_id': '{}xXx{}'.format(FEED_ID, 0),
                        },
                    },
                    'service': IsProtobufMap({
                        SHOP_ID: {
                            'identifiers': {
                                'feed_id': FEED_ID,
                                'business_id': BUSINESS_ID,
                                'shop_id': SHOP_ID,
                                'offer_id': '{}xXx{}'.format(FEED_ID, 0),
                            },
                            'delivery': {
                                'partner': {
                                    'original': {
                                        'pickup_options': {
                                            'options': [{
                                                'Cost': 100100100.0,
                                                'DaysMin': 2,
                                                'DaysMax': 2,
                                            }]
                                        },
                                    },
                                },
                            },
                        },
                    })
                }]
            }]
        },
        {
            'united_offers': [{
                'offer': [{
                    'basic': {
                        'identifiers': {
                            'business_id': BUSINESS_ID,
                            'offer_id': '{}xXx{}'.format(FEED_ID, 1),
                        },
                        'content': {
                            'partner': {
                                'original': {
                                    'adult': {'flag': True}
                                },
                            },
                        },
                    },
                    'service': IsProtobufMap({
                        SHOP_ID: {
                            'identifiers': {
                                'feed_id': FEED_ID,
                                'business_id': BUSINESS_ID,
                                'shop_id': SHOP_ID,
                                'offer_id': '{}xXx{}'.format(FEED_ID, 1),
                            },
                            'status': {
                                'original_cpa': {'flag': True},
                            },
                            'delivery': {
                                'partner': {
                                    'original': {
                                        'pickup_options': {
                                            'options': [{
                                                'Cost': 200.0,
                                                'DaysMin': 2,
                                                'DaysMax': 3,
                                            }]
                                        },
                                        'pickup': {'flag': True},
                                        'store': {'flag': True},
                                        'delivery': {'flag': True},
                                    },
                                },
                            },
                            'price': {
                                'enable_auto_discounts': {'flag': True},
                                'basic': {'delivery_included': True},
                            },
                        },
                    })
                }]
            }]
        }, {
            'united_offers': [{
                'offer': [{
                    'basic': {
                        'identifiers': {
                            'business_id': BUSINESS_ID,
                            'offer_id': '{}xXx{}'.format(FEED_ID, 2),
                        },
                        'content': {
                            'partner': {
                                'original': {
                                    'adult': {'flag': True}
                                },
                            },
                        },
                    },
                    'service': IsProtobufMap({
                        SHOP_ID: {
                            'identifiers': {
                                'feed_id': FEED_ID,
                                'business_id': BUSINESS_ID,
                                'shop_id': SHOP_ID,
                                'offer_id': '{}xXx{}'.format(FEED_ID, 2),
                            },
                            'status': {
                                'original_cpa': {'flag': True},
                            },
                            'delivery': {
                                'partner': {
                                    'original': {
                                        'pickup_options': {'options': [{
                                            'Cost': 100.0,
                                        }]},
                                        'pickup': {'flag': True},
                                        'store': {'flag': True},
                                        'delivery': {'flag': True},
                                    },
                                },
                            },
                            'price': {
                                'enable_auto_discounts': {'flag': True},
                                'basic': {'delivery_included': True},
                            },
                        },
                    })
                }]
            }]
    }]))
