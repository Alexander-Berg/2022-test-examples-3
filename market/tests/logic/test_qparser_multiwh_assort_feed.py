# coding coding: utf-8

import pytest
#  import os

from datetime import datetime
from google.protobuf.timestamp_pb2 import Timestamp
from hamcrest import assert_that, is_not

from market.idx.datacamp.parser.yatf.env import WorkersEnv, make_input_task, UpdateTaskServiceMock
from market.idx.datacamp.parser.yatf.fake_mds import FakeMds
from market.idx.datacamp.parser.yatf.qp_mocks import QParserTestLauncherMock
from market.idx.datacamp.parser.yatf.resources.config_mock import PushParserConfigMock
from market.idx.datacamp.proto.api.UpdateTask_pb2 import FEED_CLASS_SELECTIVE_BASIC_PATCH_UPDATE_SERVICE_FULL_COMPLETE
from market.idx.datacamp.proto.offer import DataCampOffer_pb2
from market.idx.datacamp.proto.offer.DataCampOffer_pb2 import UpdateMeta
from market.idx.datacamp.yatf.matchers.matchers import HasSerializedDatacampMessages

from market.idx.yatf.matchers.protobuf_matchers import IsProtobufMap
from market.idx.yatf.resources.lbk_topic import LbkTopic
from market.idx.yatf.utils.utils import create_pb_timestamp


BUSINESS_ID = 321
SHOP_ID = 1111
FEED_ID = 1000
WAREHOUSE_ID = 123
REAL_FEED_ID = 4321
TIMESTAMP = create_pb_timestamp(100500)


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


@pytest.fixture(scope='module')
def config(tmpdir_factory, log_broker_stuff, yt_server, output_topic, input_topic, datacamp_output_topic):
    cfg = {
        'logbroker_in': {
            'topic': input_topic.topic,
        },
        'logbroker': {
            'topic': output_topic.topic,
            'datacamp_messages_topic': datacamp_output_topic.topic,
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


@pytest.yield_fixture(scope='module')
def qp_runner(config, log_broker_stuff):
    yield QParserTestLauncherMock(
        log_broker_stuff=log_broker_stuff,
        config=config,
        feed_info={
            'market_color': 'white',
        }
    )


@pytest.fixture()
def push_parser(monkeymodule, config, qp_runner):
    with monkeymodule.context() as m:
        m.setattr("market.idx.datacamp.parser.lib.worker.ParsingTaskWorker.process_task", qp_runner.process_task)

        yield WorkersEnv(
            config=config,
            parsing_service=UpdateTaskServiceMock
        )


def test_qparser_multi_wh(push_parser, input_topic, output_topic, mds, datacamp_output_topic, config, qp_runner):
    """
    Проверяем, что:
    1. парсится мультискладовая секция
    2. стоки из поля offer.count игнорируются для всех офферов
    """
    date = datetime.utcnow().replace(microsecond=0)
    yml_date = date.strftime('%Y-%m-%d %H:%M:%SZ')
    yml_platform = 'market test YML from_dicts'
    yml_version = 'test version 2.1'
    wh_sections = {
        0: [
            {'partner_wh_id': 'first_wh', 'stock_count': 10},
            {'partner_wh_id': 'second_wh', 'stock_count': 15},
        ],
        1: [
            {'partner_wh_id': 'second_wh', 'stock_count': 20},
        ],
    }
    partner_wh_mapping = {
        'first_wh': (1001, 34),
        'second_wh': (1002, 35),
    }
    offer_stock_counts = {
        2: 10,
    }

    mds.generate_feed(
        FEED_ID,
        offer_count=5,
        shop_dict={
            'date': yml_date,
            'platform': yml_platform,
            'version': yml_version,
        },
        warehouse_sections=wh_sections,
        offer_stock_counts=offer_stock_counts,
    )

    input_topic.write(make_input_task(
        mds,
        FEED_ID,
        BUSINESS_ID,
        SHOP_ID,
        timestamp=TIMESTAMP,
        real_feed_id=REAL_FEED_ID,
        task_type=FEED_CLASS_SELECTIVE_BASIC_PATCH_UPDATE_SERVICE_FULL_COMPLETE,
        partner_wh_mapping=partner_wh_mapping,
    ).SerializeToString())

    push_parser.run(total_sessions=1)

    data = output_topic.read(count=5)

    ts_expected = Timestamp()
    ts_expected.FromDatetime(date)
    META = UpdateMeta(
        source=DataCampOffer_pb2.PUSH_PARTNER_FEED,
        applier=DataCampOffer_pb2.QPARSER,
        timestamp=ts_expected
    )

    united_offers_batch = []
    for offer_idx in [0, 1]:
        united_offers_batch.append({
            'offer': [{
                'basic': {
                    'identifiers': {
                        'business_id': BUSINESS_ID,
                        'offer_id': '{}xXx{}'.format(FEED_ID, offer_idx),
                    },
                    'meta': {
                        'scope': DataCampOffer_pb2.BASIC,
                        'ts_created': ts_expected,
                    }
                },
                'service': IsProtobufMap({
                    SHOP_ID: {
                        'identifiers': {
                            'feed_id': FEED_ID,
                            'real_feed_id': REAL_FEED_ID,
                            'business_id': BUSINESS_ID,
                            'shop_id': SHOP_ID,
                            'offer_id': '{}xXx{}'.format(FEED_ID, offer_idx),
                        },
                    }
                }),
            }]
        })
        actual_part = {}
        for section in wh_sections[offer_idx]:
            partner_wh_id = section['partner_wh_id']
            stock = section['stock_count']
            wh_feed_id, wh_id = partner_wh_mapping[partner_wh_id]
            actual_part[wh_id] = {
                'identifiers': {
                    'feed_id': wh_feed_id,
                    'business_id': BUSINESS_ID,
                    'shop_id': SHOP_ID,
                    'offer_id': '{}xXx{}'.format(FEED_ID, offer_idx),
                    'warehouse_id': wh_id,
                },
                'stock_info': {
                    'partner_stocks': {
                        'count': stock,
                        'meta': META,
                    }
                }
            }
        united_offers_batch[-1]['offer'][-1]['actual'] = IsProtobufMap({
            SHOP_ID: {
                'warehouse': IsProtobufMap(actual_part)
            }
        })

    assert_that(data, HasSerializedDatacampMessages([{'united_offers': [united_offer]} for united_offer in united_offers_batch]))

    # check that stocks aren't written to service part for multiwh offers
    assert_that(data, is_not(HasSerializedDatacampMessages([{
        'united_offers': [{
            'offer': [{
                'service': IsProtobufMap({
                    SHOP_ID: {
                        'identifiers': {
                            'feed_id': FEED_ID,
                            'offer_id': '{}xXx{}'.format(FEED_ID, 2),
                            'shop_id': SHOP_ID,
                            'business_id': BUSINESS_ID,
                            'real_feed_id': REAL_FEED_ID,
                        },
                        'stock_info': {
                            'partner_stocks': {
                                'meta': META,
                            }
                        }
                    }
                })
            }]
        }]
    }])))


def test_qparser_single_wh(push_parser, input_topic, output_topic, mds, datacamp_output_topic, config, qp_runner):
    """
    Проверяем, что:
    1. парсится мультискладовая секция
    2. стоки из поля offer.warehouses имеют приоритет над offer.count
    3. если offer.warehouses нет, стоки берутся из offer.count
    """
    date = datetime.utcnow().replace(microsecond=0)
    yml_date = date.strftime('%Y-%m-%d %H:%M:%SZ')
    yml_platform = 'market test YML from_dicts'
    yml_version = 'test version 2.1'
    wh_sections = {
        0: [
            {'partner_wh_id': 'partner_wh', 'stock_count': 10},
        ],
    }
    partner_wh_mapping = {
        'partner_wh': (1001, WAREHOUSE_ID),
    }
    offer_stock_counts = {
        0: 20,
        1: 15,
    }

    mds.generate_feed(
        FEED_ID,
        offer_count=5,
        shop_dict={
            'date': yml_date,
            'platform': yml_platform,
            'version': yml_version,
        },
        warehouse_sections=wh_sections,
        offer_stock_counts=offer_stock_counts,
    )

    input_topic.write(make_input_task(
        mds,
        FEED_ID,
        BUSINESS_ID,
        SHOP_ID,
        timestamp=TIMESTAMP,
        real_feed_id=REAL_FEED_ID,
        task_type=FEED_CLASS_SELECTIVE_BASIC_PATCH_UPDATE_SERVICE_FULL_COMPLETE,
        partner_wh_mapping=partner_wh_mapping,
        warehouse_id=WAREHOUSE_ID,
    ).SerializeToString())

    push_parser.run(total_sessions=1)

    data = output_topic.read(count=5)

    ts_expected = Timestamp()
    ts_expected.FromDatetime(date)
    META = UpdateMeta(
        source=DataCampOffer_pb2.PUSH_PARTNER_FEED,
        applier=DataCampOffer_pb2.QPARSER,
        timestamp=ts_expected
    )

    united_offers_batch = []
    for offer_idx in [0, 1]:
        united_offers_batch.append({
            'offer': [{
                'basic': {
                    'identifiers': {
                        'business_id': BUSINESS_ID,
                        'offer_id': '{}xXx{}'.format(FEED_ID, offer_idx),
                    },
                    'meta': {
                        'scope': DataCampOffer_pb2.BASIC,
                        'ts_created': ts_expected,
                    }
                }
            }]
        })

        service_part = {
            SHOP_ID: {
                'identifiers': {
                    'feed_id': FEED_ID,
                    'real_feed_id': REAL_FEED_ID,
                    'business_id': BUSINESS_ID,
                    'shop_id': SHOP_ID,
                    'offer_id': '{}xXx{}'.format(FEED_ID, offer_idx),
                },
            }
        }
        if offer_idx not in wh_sections and offer_idx in offer_stock_counts:
            service_part[SHOP_ID]['stock_info'] = {
                'partner_stocks': {
                    'count': offer_stock_counts[offer_idx],
                    'meta': META,
                }
            }
            service_part[SHOP_ID]['identifiers']['warehouse_id'] = WAREHOUSE_ID
        united_offers_batch[-1]['offer'][-1]['service'] = IsProtobufMap(service_part)

        actual_part = {}
        for section in wh_sections.get(offer_idx, {}):
            partner_wh_id = section['partner_wh_id']
            stock = section['stock_count']
            wh_feed_id, wh_id = partner_wh_mapping[partner_wh_id]
            actual_part[wh_id] = {
                'identifiers': {
                    'feed_id': wh_feed_id,
                    'business_id': BUSINESS_ID,
                    'shop_id': SHOP_ID,
                    'offer_id': '{}xXx{}'.format(FEED_ID, offer_idx),
                    'warehouse_id': wh_id,
                },
                'stock_info': {
                    'partner_stocks': {
                        'count': stock,
                        'meta': META,
                    }
                }
            }

        if actual_part:
            united_offers_batch[-1]['offer'][-1]['actual'] = IsProtobufMap({
                SHOP_ID: {
                    'warehouse': IsProtobufMap(actual_part)
                }
            })

        assert_that(data, HasSerializedDatacampMessages([{'united_offers': [united_offer]} for united_offer in united_offers_batch]))
