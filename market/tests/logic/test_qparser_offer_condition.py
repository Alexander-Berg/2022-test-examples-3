# coding: utf-8

from hamcrest import assert_that, is_not, has_items
import pytest
import uuid

from market.idx.datacamp.parser.yatf.env import WorkersEnv, make_input_task, UpdateTaskServiceMock
from market.idx.datacamp.parser.yatf.fake_mds import FakeMds
from market.idx.datacamp.parser.yatf.qp_mocks import QParserTestLauncherMock
from market.idx.datacamp.parser.yatf.resources.config_mock import PushParserConfigMock
from market.idx.datacamp.proto.api.DatacampMessage_pb2 import DatacampMessage
from market.idx.datacamp.proto.api.UpdateTask_pb2 import (
    FEED_CLASS_SELECTIVE_BASIC_PATCH_UPDATE_SERVICE_FULL_COMPLETE,
    FEED_CLASS_ASSORTMENT_BASIC_PATCH_UPDATE_SALE_TERMS_SERVICE_FULL_COMPLETE,
)
from market.idx.datacamp.proto.offer.OfferContent_pb2 import Condition
from market.idx.datacamp.yatf.matchers.matchers import HasSerializedDatacampMessages
from market.idx.yatf.matchers.protobuf_matchers import IsProtobufMap, IsSerializedProtobuf
from market.idx.yatf.matchers.lb_matchers import HasNoUnreadData
from market.idx.yatf.resources.lbk_topic import LbkTopic


BUSINESS_ID = 10
SHOP_ID = 111
FEED_ID = 100

OFFERS = [
    {
        'id': 'no.condition',
    },
    {
        'id': 'likenew.condition',
        'condition': {
            'type': 'likenew',
            'reason': 'some reason'
        },
    },
]


OFFERS_WITH_PRICE = [
    {
        'id': 'no.condition',
        'price': '100',
    },
    {
        'id': 'likenew.condition',
        'condition': {
            'type': 'likenew',
            'reason': 'some reason'
        },
        'price': '100',  # service field must be set even if basic is ignored
    },
]


def make_price(price):
    return {
        'basic': {
            'binary_price': {
                'price': price * 10**7,
            },
        },
    }


@pytest.fixture(
    scope='module',
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


@pytest.fixture(scope='module')
def input_topic(log_broker_stuff):
    topic = LbkTopic(log_broker_stuff)
    topic.create()
    return topic


@pytest.fixture()
def output_topic(log_broker_stuff, is_csv):
    topic = LbkTopic(log_broker_stuff, topic='{}_{}'.format(str(uuid.uuid4()), is_csv))
    topic.create()
    return topic


@pytest.fixture(scope='module')
def datacamp_output_topic(log_broker_stuff):
    topic = LbkTopic(log_broker_stuff)
    topic.create()
    return topic


@pytest.fixture()
def config(
    tmpdir_factory,
    log_broker_stuff,
    yt_server,
    output_topic,
    input_topic,
    datacamp_output_topic,
):
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
def qp_runner(config, log_broker_stuff, datacamp_output_topic, is_csv):
    yield QParserTestLauncherMock(
        log_broker_stuff=log_broker_stuff,
        config=config,
        feed_format='csv' if is_csv else 'xml',
        qparser_config={
            'logbroker': {
                'complete_feed_finish_command_batch_size': 1000,
                'datacamp_messages_topic': datacamp_output_topic.topic,
                'datacamp_messages_writers_count': 1,
            }
        },
        color='white'
    )


@pytest.fixture()
def push_parser(monkeypatch, config, qp_runner):
    with monkeypatch.context() as m:
        m.setattr(
            "market.idx.datacamp.parser.lib.worker.ParsingTaskWorker.process_task",
            qp_runner.process_task
        )

        yield WorkersEnv(
            config=config,
            parsing_service=UpdateTaskServiceMock
        )


def test_ignore_offers_with_condition(
    push_parser,
    input_topic,
    output_topic,
    mds,
    is_csv
):
    mds.generate_feed(
        FEED_ID,
        is_blue=False,
        is_csv=is_csv,
        force_offers=OFFERS,
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
    push_parser.run(total_sessions=1)
    data = output_topic.read(count=1)

    assert_that(data, HasSerializedDatacampMessages([{
        'united_offers': [{
            'offer': [{
                'basic': {
                    'identifiers': {
                        'business_id': BUSINESS_ID,
                        'offer_id': 'no.condition',
                    },
                },
            }]
        }]}
    ]))
    assert_that(data, is_not(HasSerializedDatacampMessages([{
        'united_offers': [{
            'offer': [{
                'basic': {
                    'identifiers': {
                        'business_id': BUSINESS_ID,
                        'offer_id': 'likenew.condition',
                    },
                },
            }]
        }]}
    ])))

    assert_that(output_topic, HasNoUnreadData())


def test_ignore_offers_with_condition_multi_but_not_price(
    push_parser,
    input_topic,
    output_topic,
    mds,
    is_csv
):
    mds.generate_feed(
        FEED_ID,
        is_blue=False,
        is_csv=is_csv,
        force_offers=OFFERS_WITH_PRICE,
    )
    input_topic.write(
        make_input_task(
            mds,
            FEED_ID,
            BUSINESS_ID,
            SHOP_ID,
            task_type=FEED_CLASS_ASSORTMENT_BASIC_PATCH_UPDATE_SALE_TERMS_SERVICE_FULL_COMPLETE
        ).SerializeToString()
    )
    push_parser.run(total_sessions=1)
    data = output_topic.read(count=2)

    assert_that(data, has_items(*[IsSerializedProtobuf(DatacampMessage, {
        'united_offers': [{
            'offer': [{
                'basic': {
                    'identifiers': {
                        'business_id': BUSINESS_ID,
                        'offer_id': 'no.condition',
                    },
                },
                'service': IsProtobufMap({
                    SHOP_ID: {
                        'identifiers': {
                            'business_id': BUSINESS_ID,
                            'offer_id': 'no.condition',
                            'shop_id': SHOP_ID,
                        },
                        'price': make_price(100),
                    },
                }),
            }]
        }]}), IsSerializedProtobuf(DatacampMessage, {
        'united_offers': [{
            'offer': [{
                'service': IsProtobufMap({
                    SHOP_ID: {
                        'identifiers': {
                            'business_id': BUSINESS_ID,
                            'offer_id': 'likenew.condition',
                            'shop_id': SHOP_ID,
                        },
                        'price': make_price(100),
                    },
                }),
            }]
        }]}
    )]))
    assert_that(data, is_not(has_items(IsSerializedProtobuf(DatacampMessage, {
        'united_offers': [{
            'offer': [{
                'basic': {
                    'identifiers': {
                        'business_id': BUSINESS_ID,
                        'offer_id': 'likenew.condition',
                    },
                    'content': {
                        'partner': {
                            'actual': {
                                'condition': {
                                    'type': Condition.LIKENEW,
                                },
                            },
                        },
                    },
                },
            }]
        }]
    }))))

    assert_that(output_topic, HasNoUnreadData())
