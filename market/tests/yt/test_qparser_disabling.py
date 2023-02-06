# coding: utf-8

import pytest
import uuid

from datetime import datetime, timedelta
from hamcrest import assert_that, equal_to

from market.idx.yatf.resources.datacamp.datacamp_tables import DataCampBasicOffersTable, DataCampServiceOffersTable, DataCampServiceSearchOffersTable
from market.idx.datacamp.parser.yatf.env import WorkersEnv, make_input_task, UpdateTaskServiceMock
from market.idx.datacamp.parser.yatf.fake_mds import FakeMds
from market.idx.datacamp.parser.yatf.qp_mocks import QParserTestLauncherMock
from market.idx.datacamp.parser.yatf.resources.config_mock import PushParserConfigMock
import market.idx.datacamp.proto.offer.DataCampOffer_pb2 as DTC
from market.idx.datacamp.yatf.matchers.matchers import HasSerializedDatacampMessages
from market.idx.pylibrary.datacamp.conversion import offer_to_basic_row, offer_to_service_row
from market.idx.datacamp.proto.api.UpdateTask_pb2 import FEED_CLASS_SELECTIVE_BASIC_PATCH_UPDATE_SERVICE_FULL_COMPLETE
from market.idx.datacamp.proto.api.DatacampMessage_pb2 import DatacampMessage

from market.idx.yatf.resources.lbk_topic import LbkTopic
from market.idx.yatf.utils.utils import create_pb_timestamp, create_timestamp_from_json
from market.idx.yatf.matchers.protobuf_matchers import IsProtobufMap
from market.idx.datacamp.proto.api.UpdateTask_pb2 import ShopsDatParameters
import yt.wrapper as yt

BUSINESS_ID = 10
SHOP_ID = 111
FEED_ID = 100
WAREHOUSE_ID=333
TIMESTAMP = create_pb_timestamp(100500)

NOW_UTC = datetime.utcnow().replace(microsecond=0)  # JSON serializer should always use UTC
PAST_UTC = NOW_UTC - timedelta(minutes=45)
FUTURE_UTC = NOW_UTC + timedelta(minutes=45)
time_pattern = "%Y-%m-%dT%H:%M:%SZ"

current_time = NOW_UTC.strftime(time_pattern)
current_ts = create_timestamp_from_json(current_time)

past_time = PAST_UTC.strftime(time_pattern)
past_ts = create_timestamp_from_json(past_time)

future_time = FUTURE_UTC.strftime(time_pattern)
future_ts = create_timestamp_from_json(future_time)


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


def generate_offer(offer_id):
    return DTC.Offer(
        identifiers=DTC.OfferIdentifiers(
            business_id=BUSINESS_ID,
            offer_id='{}xXx{}'.format(FEED_ID, offer_id),
            shop_id=SHOP_ID
        )
    )


def offer_to_search_service_row(offer, disabled_by_partner=False, removed=False, real_feed_id=FEED_ID):
    row = {
        'business_id': offer.identifiers.business_id,
        'shop_id': offer.identifiers.shop_id,
        'shop_sku': offer.identifiers.offer_id,
    }
    if disabled_by_partner:
        row['disabled_by_partner'] = disabled_by_partner
    row['real_feed_id'] = real_feed_id
    if removed:
        row['removed'] = removed
    return row


@pytest.fixture(scope='module')
def basic_table(yt_server, config):
    return DataCampBasicOffersTable(yt_server, yt.ypath_join('//home/test_datacamp/basic', str(uuid.uuid4())), data=[
        offer_to_basic_row(generate_offer('FeedEnabled1'))
    ])


@pytest.fixture(scope='module')
def service_table(yt_server, config):
    return DataCampServiceOffersTable(yt_server, yt.ypath_join('//home/test_datacamp/service', str(uuid.uuid4())), data=[
        offer_to_service_row(generate_offer('FeedEnabled1'))
    ])


@pytest.fixture(scope='module')
def actual_service_table(yt_server, config):
    return DataCampServiceOffersTable(yt_server, yt.ypath_join('//home/test_datacamp/actual', str(uuid.uuid4())), data=[
        offer_to_service_row(generate_offer('FeedEnabled1'))
    ])


@pytest.fixture(scope='module')
def service_search_table(yt_server, config):
    return DataCampServiceSearchOffersTable(yt_server, yt.ypath_join('//home/test_datacamp/search', str(uuid.uuid4())), data=[
        offer_to_search_service_row(generate_offer('FeedEnabled1')),
        offer_to_search_service_row(generate_offer('table_disabled_by_partner'), True),
        offer_to_search_service_row(generate_offer('feed_disabled_1')),
        offer_to_search_service_row(generate_offer('table_removed'), False, True),
        offer_to_search_service_row(generate_offer('table_another_real_feed_id'), False, False, FEED_ID+1),
        # Проверяем, что скроется оффер, у которого offer_id < минимального в фиде offer_id
        offer_to_search_service_row(generate_offer('100')),
        # Оффер, созданный из ПИ, с real_feed_id=0, должен также скрываться комплит-командой
        offer_to_search_service_row(generate_offer('pi_disabled_3'), disabled_by_partner=False, removed=False, real_feed_id=0),
    ])


def generate_feed_offers():
    return {
        0: {'id': '{}xXx{}'.format(FEED_ID, 'FeedEnabled1'), 'shop-sku': '{}xXx{}'.format(FEED_ID, 'FeedEnabled1')},
        1: {'id': '{}xXx{}'.format(FEED_ID, 'FeedEnabled2'), 'shop-sku': '{}xXx{}'.format(FEED_ID, 'FeedEnabled2')},
        }


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
def qp_runner(config, yt_server, basic_table, service_table, actual_service_table, service_search_table, log_broker_stuff,
              output_topic, datacamp_output_topic, complete_offers_topic):
    yield QParserTestLauncherMock(
        log_broker_stuff=log_broker_stuff,
        config=config,
        yt_server=yt_server,
        basic_table=basic_table,
        service_table=service_table,
        actual_service_table=actual_service_table,
        service_search_table=service_search_table,
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


def test_qparser_disabling(push_parser, input_topic, output_topic, mds, datacamp_output_topic, complete_offers_topic, config, qp_runner):
    # todo test the same feed for several regional shops
    offers = generate_feed_offers()
    mds.generate_feed(FEED_ID, offer_count=len(offers), offers_dict=offers)

    input_topic.write(make_input_task(mds, FEED_ID, BUSINESS_ID, SHOP_ID, warehouse_id=WAREHOUSE_ID,
                                      shops_dat_parameters=ShopsDatParameters(
                                          color=DTC.WHITE,
                                          vat=7
                                      ),
                                      task_type=FEED_CLASS_SELECTIVE_BASIC_PATCH_UPDATE_SERVICE_FULL_COMPLETE,
                                      timestamp=TIMESTAMP,
                                      real_feed_id=FEED_ID
                                      ).SerializeToString())
    push_parser.run(total_sessions=1)
    data = complete_offers_topic.read(count=1)
    assert_that(len(data), equal_to(1))

    message = DatacampMessage()
    message.ParseFromString(data[0])
    assert_that(len(message.united_offers), equal_to(1))
    assert_that(len(message.united_offers[0].offer), equal_to(3))

    assert_that(data, HasSerializedDatacampMessages([{
        'united_offers': [
            {
                'offer': [
                    {
                        'basic': {
                            'identifiers': {
                                'business_id': BUSINESS_ID,
                                'offer_id': '{}xXx{}'.format(FEED_ID, 'feed_disabled_1'),
                            }
                        },
                        'service': IsProtobufMap({
                            SHOP_ID: {
                                'identifiers': {
                                    'business_id': BUSINESS_ID,
                                    'offer_id': '{}xXx{}'.format(FEED_ID, 'feed_disabled_1'),
                                    'shop_id': SHOP_ID,
                                },
                                'status': {
                                    'disabled': [
                                        {
                                            'flag': True,
                                            'meta': {
                                                'source': DTC.PUSH_PARTNER_FEED,
                                            },
                                        }
                                    ]
                                }
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
                                'business_id': BUSINESS_ID,
                                'offer_id': '{}xXx{}'.format(FEED_ID, '100'),
                            }
                        },
                        'service': IsProtobufMap({
                            SHOP_ID: {
                                'identifiers': {
                                    'business_id': BUSINESS_ID,
                                    'offer_id': '{}xXx{}'.format(FEED_ID, '100'),
                                    'shop_id': SHOP_ID,
                                },
                                'status': {
                                    'disabled': [
                                        {
                                            'flag': True,
                                            'meta': {
                                                'source': DTC.PUSH_PARTNER_FEED,
                                            },
                                        }
                                    ]
                                }
                            }
                        })
                    },
                ]
            },
            {
                'offer': [
                    {
                        'basic': {
                            'identifiers': {
                                'business_id': BUSINESS_ID,
                                'offer_id': '{}xXx{}'.format(FEED_ID, 'pi_disabled_3'),
                            }
                        },
                        'service': IsProtobufMap({
                            SHOP_ID: {
                                'identifiers': {
                                    'business_id': BUSINESS_ID,
                                    'offer_id': '{}xXx{}'.format(FEED_ID, 'pi_disabled_3'),
                                    'shop_id': SHOP_ID,
                                },
                                'status': {
                                    'disabled': [
                                        {
                                            'flag': True,
                                            'meta': {
                                                'source': DTC.PUSH_PARTNER_FEED,
                                            },
                                        }
                                    ]
                                }
                            }
                        })
                    },
                ]
            },
        ]
    }]))
