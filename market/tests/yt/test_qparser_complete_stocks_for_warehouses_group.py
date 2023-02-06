# coding: utf-8

import pytest
import uuid

from hamcrest import assert_that, equal_to

from market.idx.yatf.resources.datacamp.datacamp_tables import (
    DataCampBasicOffersTable, DataCampServiceOffersTable,
    DataCampServiceSearchOffersTable, DataCampActualServiceSearchOffersTable
)
from market.idx.datacamp.parser.yatf.env import WorkersEnv, make_input_task, UpdateTaskServiceMock
from market.idx.datacamp.parser.yatf.fake_mds import FakeMds
from market.idx.datacamp.parser.yatf.qp_mocks import QParserTestLauncherMock
from market.idx.datacamp.parser.yatf.resources.config_mock import PushParserConfigMock
import market.idx.datacamp.proto.offer.DataCampOffer_pb2 as DTC
from market.idx.datacamp.yatf.matchers.matchers import HasSerializedDatacampMessages
from market.idx.pylibrary.datacamp.conversion import offer_to_basic_row, offer_to_service_row
from market.idx.datacamp.proto.api.UpdateTask_pb2 import (
    FEED_CLASS_STOCK
)
from market.idx.datacamp.proto.api.DatacampMessage_pb2 import DatacampMessage

from market.idx.yatf.resources.lbk_topic import LbkTopic
from market.idx.yatf.utils.utils import create_pb_timestamp
from market.idx.yatf.matchers.protobuf_matchers import IsProtobufMap
from market.idx.datacamp.proto.api.UpdateTask_pb2 import ShopsDatParameters
import yt.wrapper as yt


GROUP_BUSINESS_ID = 1
GROUP_SHOP_ID_1 = 11
GROUP_SHOP_ID_2 = 22
GROUP_WHS_ID_1 = 111
GROUP_WHS_ID_2 = 222
GROUP_FEED_ID_1 = 1111
GROUP_FEED_ID_2 = 2222


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
def complete_offers_topic(log_broker_stuff):
    topic = LbkTopic(log_broker_stuff)
    topic.create()
    return topic


def generate_offer(offer_id, business_id, shop_id=None, warehouse_id=None):
    ids = DTC.OfferIdentifiers()
    ids.offer_id = offer_id
    ids.business_id = business_id

    if shop_id:
        ids.shop_id = shop_id
    if warehouse_id:
        ids.warehouse_id = warehouse_id

    return DTC.Offer(
        identifiers=ids
    )


def offer_to_search_service_row(offer):
    row = {
        'business_id': offer.identifiers.business_id,
        'shop_id': offer.identifiers.shop_id,
        'shop_sku': offer.identifiers.offer_id,
    }
    return row


def offer_to_search_actual_row(offer, no_stocks=False):
    row = {
        'business_id': offer.identifiers.business_id,
        'shop_id': offer.identifiers.shop_id,
        'warehouse_id': offer.identifiers.warehouse_id,
        'shop_sku': offer.identifiers.offer_id,
    }
    if no_stocks:
        row['partner_stocks'] = 0
    else:
        row['partner_stocks'] = 10
    return row


@pytest.fixture(scope='module')
def basic_table(yt_server, config):
    return DataCampBasicOffersTable(yt_server, yt.ypath_join('//home/test_datacamp/basic', str(uuid.uuid4())), data=[
        offer_to_basic_row(generate_offer('NotInFeed1', business_id=GROUP_BUSINESS_ID)),
        offer_to_basic_row(generate_offer('InFeed1', business_id=GROUP_BUSINESS_ID)),
        offer_to_basic_row(generate_offer('NotInFeed12', business_id=GROUP_BUSINESS_ID)),
        offer_to_basic_row(generate_offer('InFeed2', business_id=GROUP_BUSINESS_ID))
    ])


@pytest.fixture(scope='module')
def service_table(yt_server, config):
    return DataCampServiceOffersTable(yt_server, yt.ypath_join('//home/test_datacamp/service', str(uuid.uuid4())), data=[
        offer_to_service_row(generate_offer('NotInFeed1', business_id=GROUP_BUSINESS_ID, shop_id=GROUP_SHOP_ID_1)),
        offer_to_service_row(generate_offer('InFeed1', business_id=GROUP_BUSINESS_ID, shop_id=GROUP_SHOP_ID_1)),
        offer_to_service_row(generate_offer('NotInFeed12', business_id=GROUP_BUSINESS_ID, shop_id=GROUP_SHOP_ID_1)),
        offer_to_service_row(generate_offer('NotInFeed12', business_id=GROUP_BUSINESS_ID, shop_id=GROUP_SHOP_ID_2)),
        offer_to_service_row(generate_offer('InFeed2', business_id=GROUP_BUSINESS_ID, shop_id=GROUP_SHOP_ID_2))
    ])


@pytest.fixture(scope='module')
def actual_service_table(yt_server, config):
    return DataCampServiceOffersTable(yt_server, yt.ypath_join('//home/test_datacamp/actual', str(uuid.uuid4())), data=[
        offer_to_service_row(generate_offer('NotInFeed1', business_id=GROUP_BUSINESS_ID, shop_id=GROUP_SHOP_ID_1, warehouse_id=GROUP_WHS_ID_1)),
        offer_to_service_row(generate_offer('InFeed1', business_id=GROUP_BUSINESS_ID, shop_id=GROUP_SHOP_ID_1, warehouse_id=GROUP_WHS_ID_1)),
        offer_to_service_row(generate_offer('NotInFeed12', business_id=GROUP_BUSINESS_ID, shop_id=GROUP_SHOP_ID_1, warehouse_id=GROUP_WHS_ID_1)),
        offer_to_service_row(generate_offer('NotInFeed12', business_id=GROUP_BUSINESS_ID, shop_id=GROUP_SHOP_ID_2, warehouse_id=GROUP_WHS_ID_2)),
        offer_to_service_row(generate_offer('InFeed2', business_id=GROUP_BUSINESS_ID, shop_id=GROUP_SHOP_ID_2, warehouse_id=GROUP_WHS_ID_2))
    ])


@pytest.fixture(scope='module')
def service_search_table(yt_server, config):
    return DataCampServiceSearchOffersTable(yt_server, yt.ypath_join('//home/test_datacamp/search', str(uuid.uuid4())), data=[
        offer_to_search_service_row(generate_offer('NotInFeed1', business_id=GROUP_BUSINESS_ID, shop_id=GROUP_SHOP_ID_1)),
        offer_to_search_service_row(generate_offer('InFeed1', business_id=GROUP_BUSINESS_ID, shop_id=GROUP_SHOP_ID_1)),
        offer_to_search_service_row(generate_offer('NotInFeed12', business_id=GROUP_BUSINESS_ID, shop_id=GROUP_SHOP_ID_1)),
        offer_to_search_service_row(generate_offer('NotInFeed12', business_id=GROUP_BUSINESS_ID, shop_id=GROUP_SHOP_ID_2)),
        offer_to_search_service_row(generate_offer('InFeed2', business_id=GROUP_BUSINESS_ID, shop_id=GROUP_SHOP_ID_2))
    ])


@pytest.fixture(scope='module')
def actual_service_search_table(yt_server, config):
    return DataCampActualServiceSearchOffersTable(yt_server, yt.ypath_join('//home/test_datacamp/actual_service_search', str(uuid.uuid4())), data=[
        offer_to_search_actual_row(generate_offer('NotInFeed1', business_id=GROUP_BUSINESS_ID, shop_id=GROUP_SHOP_ID_1, warehouse_id=GROUP_WHS_ID_1)),
        offer_to_search_actual_row(generate_offer('InFeed1', business_id=GROUP_BUSINESS_ID, shop_id=GROUP_SHOP_ID_1, warehouse_id=GROUP_WHS_ID_1)),
        offer_to_search_actual_row(generate_offer('NotInFeed12', business_id=GROUP_BUSINESS_ID, shop_id=GROUP_SHOP_ID_1, warehouse_id=GROUP_WHS_ID_1)),
        offer_to_search_actual_row(generate_offer('NotInFeed12', business_id=GROUP_BUSINESS_ID, shop_id=GROUP_SHOP_ID_2, warehouse_id=GROUP_WHS_ID_2)),
        offer_to_search_actual_row(generate_offer('InFeed2', business_id=GROUP_BUSINESS_ID, shop_id=GROUP_SHOP_ID_2, warehouse_id=GROUP_WHS_ID_2))
    ])


@pytest.fixture(scope='module')
def config(tmpdir_factory, log_broker_stuff, yt_server, output_topic, input_topic, datacamp_output_topic, complete_offers_topic):
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
def qp_runner(config, yt_server, basic_table, service_table, actual_service_table, service_search_table, actual_service_search_table, log_broker_stuff,
              output_topic, datacamp_output_topic, complete_offers_topic):
    yield QParserTestLauncherMock(
        log_broker_stuff=log_broker_stuff,
        config=config,
        yt_server=yt_server,
        basic_table=basic_table,
        service_table=service_table,
        actual_service_table=actual_service_table,
        service_search_table=service_search_table,
        actual_service_search_table=actual_service_search_table,
        qparser_config={
            'qparser': {
                'with_logging': True,
                'log_level': 'debug',
            },
            'logbroker': {
                'complete_feed_finish_command_batch_size': 1000,
                'topic': output_topic.topic,
                'datacamp_messages_topic': datacamp_output_topic.topic,
                'datacamp_messages_writers_count': 1,
                'qoffers_quick_pipeline_messages_topic': complete_offers_topic.topic,
                'qoffers_quick_pipeline_messages_writers_count': 1
            },
            'feature': {
                # 'enable_quick_pipeline': True,
                'complete_feed_explicit_disabling': True,
                'complete_feed_explicit_stocks': True,
            }
        },
    )


@pytest.fixture(scope='module')
def push_parser(monkeymodule, config, qp_runner):
    with monkeymodule.context() as m:
        m.setattr("market.idx.datacamp.parser.lib.worker.ParsingTaskWorker.process_task", qp_runner.process_task)

        yield WorkersEnv(
            config=config,
            parsing_service=UpdateTaskServiceMock
        )


def test_qparser_stock_for_warehouses_group(push_parser, input_topic, output_topic, mds, datacamp_output_topic, complete_offers_topic, config, qp_runner):
    offers = {
        i: {'id': offer_id, 'shop-sku': offer_id} for i, offer_id in enumerate(['InFeed1', 'InFeed2'])
    }
    partner_whs_group = [
        # feed_id, shop_id, warehouse_id, color
        (GROUP_FEED_ID_1, GROUP_SHOP_ID_1, GROUP_WHS_ID_1, DTC.WHITE),
        (GROUP_FEED_ID_2, GROUP_SHOP_ID_2, GROUP_WHS_ID_2, DTC.BLUE)
    ]
    main_feed_id, main_shop_id, main_warehouse_id, main_color = partner_whs_group[0]
    mds.generate_feed(main_feed_id, offer_count=len(offers), offers_dict=offers)

    input_topic.write(
        make_input_task(
            mds, main_feed_id, GROUP_BUSINESS_ID, main_shop_id,
            warehouse_id=main_warehouse_id,
            shops_dat_parameters=ShopsDatParameters(
                color=main_color,
                vat=7,
                is_upload=True
            ),
            is_regular_parsing=False,
            task_type=FEED_CLASS_STOCK,
            timestamp=TIMESTAMP,
            real_feed_id=main_feed_id,
            partner_whs_group=partner_whs_group
        ).SerializeToString()
    )

    push_parser.run(total_sessions=1)
    data = complete_offers_topic.read(count=1)

    assert_that(len(data), equal_to(1))

    message = DatacampMessage()
    message.ParseFromString(data[0])
    assert_that(len(message.united_offers), equal_to(1))
    assert_that(len(message.united_offers[0].offer), equal_to(2))

    assert_that(data, HasSerializedDatacampMessages([{
        'united_offers': [
            {
                'offer': [
                    {
                        'basic': {
                            'identifiers': {
                                'business_id': GROUP_BUSINESS_ID,
                                'offer_id': 'NotInFeed12',
                            }
                        },
                        # NotInFeed12 присутствует на обоих складах из группы, но отсутствует в фиде - зануляем стоки на обоих складах
                        'actual': IsProtobufMap({
                            GROUP_SHOP_ID_1: {
                                'warehouse': IsProtobufMap({
                                    GROUP_WHS_ID_1: {
                                        'stock_info': {
                                            'partner_stocks': {
                                                'count': 0,
                                                'meta': {
                                                    'source': DTC.PUSH_PARTNER_FEED,
                                                }
                                            }
                                        }
                                    }
                                })
                            },
                            GROUP_SHOP_ID_2: {
                                'warehouse': IsProtobufMap({
                                    GROUP_WHS_ID_2: {
                                        'stock_info': {
                                            'partner_stocks': {
                                                'count': 0,
                                                'meta': {
                                                    'source': DTC.PUSH_PARTNER_FEED,
                                                }
                                            }
                                        }
                                    }
                                })
                            }
                        })
                    },
                ],
            },
            {
                'offer': [
                    {
                        'basic': {
                            'identifiers': {
                                'business_id': GROUP_BUSINESS_ID,
                                'offer_id': 'NotInFeed1',
                            }
                        },
                        # NotInFeed1 есть только на одном складе - зануляем стоки только на нём
                        'actual': IsProtobufMap({
                            GROUP_SHOP_ID_1: {
                                'warehouse': IsProtobufMap({
                                    GROUP_WHS_ID_1: {
                                        'stock_info': {
                                            'partner_stocks': {
                                                'count': 0,
                                                'meta': {
                                                    'source': DTC.PUSH_PARTNER_FEED,
                                                }
                                            }
                                        }
                                    }
                                })
                            }
                        })
                    },
                ]
            }
        ]
    }]))
