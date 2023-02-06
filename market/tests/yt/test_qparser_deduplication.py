# coding: utf-8

import pytest
import uuid

from datetime import datetime, timedelta
from hamcrest import assert_that, empty

from market.idx.yatf.resources.datacamp.datacamp_tables import DataCampBasicOffersTable, DataCampServiceOffersTable
from market.idx.datacamp.parser.yatf.env import WorkersEnv, make_input_task, UpdateTaskServiceMock
from market.idx.datacamp.parser.yatf.fake_mds import FakeMds
from market.idx.datacamp.parser.yatf.qp_mocks import QParserTestLauncherMock
from market.idx.datacamp.parser.yatf.resources.config_mock import PushParserConfigMock
import market.idx.datacamp.proto.offer.DataCampOffer_pb2 as DTC
from market.idx.datacamp.yatf.matchers.matchers import HasSerializedDatacampMessages
from market.idx.pylibrary.datacamp.conversion import offer_to_basic_row, offer_to_service_row
from market.idx.datacamp.proto.api.UpdateTask_pb2 import FEED_CLASS_SELECTIVE_BASIC_PATCH_UPDATE_SERVICE_PATCH_UPDATE

from market.idx.yatf.resources.lbk_topic import LbkTopic
from market.idx.yatf.utils.utils import create_pb_timestamp, create_timestamp_from_json
from market.idx.datacamp.yatf.utils import create_update_meta
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
def basic_table(yt_server, config):
    return DataCampBasicOffersTable(yt_server, yt.ypath_join('//home/test_datacamp', str(uuid.uuid4())), data=[
        offer_to_basic_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                offer_id='{}xXx{}'.format(FEED_ID, 0),
                business_id=BUSINESS_ID
            ),
            pictures=DTC.OfferPictures(
                partner=DTC.PartnerPictures(
                    original=DTC.SourcePictures(
                        source=[
                            DTC.SourcePicture(
                                url='url1.com/',
                                source=DTC.DIRECT_LINK,
                            )
                        ]
                    )
                )
            )
        )),
        offer_to_basic_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                offer_id='{}xXx{}'.format(FEED_ID, 2),
                business_id=BUSINESS_ID
            ),
            pictures=DTC.OfferPictures(
                partner=DTC.PartnerPictures(
                    original=DTC.SourcePictures(
                        source=[
                            DTC.SourcePicture(
                                url='url3.com/',
                                source=DTC.DIRECT_LINK,
                            )
                        ]
                    ),
                    actual={
                        'url3.com/': DTC.MarketPicture(
                            original=DTC.MarketPicture.Picture(
                                url='avatars.mds.yandex.net/get-marketpictesting/810725/market_QPGBdfgmVprzuklCHP-PuQ/orig',
                            )
                        )
                    }
                )
            )
        )),
    ])


@pytest.fixture(scope='module')
def service_table(yt_server, config):
    return DataCampServiceOffersTable(yt_server, yt.ypath_join('//home/test_datacamp', str(uuid.uuid4())), data=[
        offer_to_service_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=BUSINESS_ID,
                offer_id='{}xXx{}'.format(FEED_ID, 3),
                shop_id=SHOP_ID
            ),
            stock_info=DTC.OfferStockInfo(
                partner_stocks=DTC.OfferStocks(
                    meta=create_update_meta(current_ts.seconds),
                    count=5
                )
            ),
            content=DTC.OfferContent(
                partner=DTC.PartnerContent(
                    original=DTC.OriginalSpecification(
                        direct_category=DTC.PartnerCategory(
                            name='same1',
                            path_category_names='root\\same1',
                            parent_id=0,
                            path_category_ids='0\\2',
                            id=2,
                            meta=DTC.UpdateMeta(
                                source=DTC.PUSH_PARTNER_FEED,
                                applier=DTC.QPARSER
                            )
                        )
                    )
                )
            )
        )),
        offer_to_service_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=BUSINESS_ID,
                offer_id='{}xXx{}'.format(FEED_ID, 4),
                shop_id=SHOP_ID
            ),
            stock_info=DTC.OfferStockInfo(
                partner_stocks=DTC.OfferStocks(
                    meta=create_update_meta(future_ts.seconds),
                    count=4
                )
            )
        )),
        offer_to_service_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=BUSINESS_ID,
                offer_id='{}xXx{}'.format(FEED_ID, 5),
                shop_id=SHOP_ID
            ),
            stock_info=DTC.OfferStockInfo(
                partner_stocks=DTC.OfferStocks(
                    meta=create_update_meta(past_ts.seconds),
                    count=7
                )
            )
        )),
    ])


@pytest.fixture(scope='module')
def actual_service_table(yt_server, config):
    return DataCampServiceOffersTable(yt_server, yt.ypath_join('//home/test_datacamp', str(uuid.uuid4())), data=[
        offer_to_service_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=BUSINESS_ID,
                offer_id='{}xXx{}'.format(FEED_ID, 3),
                shop_id=SHOP_ID,
                warehouse_id=WAREHOUSE_ID
            ),
            stock_info=DTC.OfferStockInfo(
                partner_stocks=DTC.OfferStocks(
                    meta=create_update_meta(current_ts.seconds),
                    count=5
                )
            )
        )),
        offer_to_service_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=BUSINESS_ID,
                offer_id='{}xXx{}'.format(FEED_ID, 4),
                shop_id=SHOP_ID,
                warehouse_id=WAREHOUSE_ID
            ),
            stock_info=DTC.OfferStockInfo(
                partner_stocks=DTC.OfferStocks(
                    meta=create_update_meta(future_ts.seconds),
                    count=4
                )
            )
        )),
        offer_to_service_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=BUSINESS_ID,
                offer_id='{}xXx{}'.format(FEED_ID, 5),
                shop_id=SHOP_ID,
                warehouse_id=WAREHOUSE_ID
            ),
            stock_info=DTC.OfferStockInfo(
                partner_stocks=DTC.OfferStocks(
                    meta=create_update_meta(past_ts.seconds),
                    count=7
                )
            )
        )),

    ])


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
def qp_runner(config, yt_server, basic_table, actual_service_table, service_table, log_broker_stuff):
    yield QParserTestLauncherMock(
        log_broker_stuff=log_broker_stuff,
        config=config,
        color='direct',
        yt_server=yt_server,
        basic_table=basic_table,
        actual_service_table=actual_service_table,
        service_table=service_table
    )


@pytest.fixture(scope='module')
def push_parser(monkeymodule, config, qp_runner):
    with monkeymodule.context() as m:
        m.setattr("market.idx.datacamp.parser.lib.worker.ParsingTaskWorker.process_task", qp_runner.process_task)

        yield WorkersEnv(
            config=config,
            parsing_service=UpdateTaskServiceMock
        )


def test_qparser_deduplication(push_parser, input_topic, output_topic, mds, datacamp_output_topic, config, qp_runner):
    yml_date = current_time
    yml_platform = 'market test YML from_dicts'
    yml_version = 'test version 2.1'

    categories_with_duplicates = dict()

    # "1", "2" - дубликаты, не будем сохнанять "2"
    # "4", "5", "8" - дубликаты, не будем сохнанять "5", "8"
    # у вершины "7" теперь будет родитель "4"
    categories_with_duplicates[0] = FakeMds.gen_category(id=0, name="root")
    categories_with_duplicates[1] = FakeMds.gen_category(id=1, parent_id=0, name="same1")
    categories_with_duplicates[2] = FakeMds.gen_category(id=2, parent_id=0, name="same1")
    categories_with_duplicates[3] = FakeMds.gen_category(id=3, parent_id=0, name="not same")
    categories_with_duplicates[4] = FakeMds.gen_category(id=4, parent_id=1, name="same2")
    categories_with_duplicates[5] = FakeMds.gen_category(id=5, parent_id=2, name="same2")
    categories_with_duplicates[6] = FakeMds.gen_category(id=6, parent_id=3, name="same2")
    categories_with_duplicates[7] = FakeMds.gen_category(id=7, parent_id=5, name="same2 child")
    categories_with_duplicates[8] = FakeMds.gen_category(id=8, parent_id=2, name="same2")

    mds.generate_feed(
        FEED_ID,
        offer_count=6,
        shop_dict={
            'date': yml_date,
            'platform': yml_platform,
            'version': yml_version,
        },
        offer_stock_counts={
            3: 5,
            4: 7,
            5: 3
        },
        offers_dict={
            0: {
                'picture': 'http://url1.com/',
            },
            1: {
                'picture': 'http://url2.com/'
            },
            2: {
                'picture': 'http://avatars.mds.yandex.net/get-marketpictesting/810725/market_QPGBdfgmVprzuklCHP-PuQ/orig'
            }
        },
        categories_count=5,
        categories=categories_with_duplicates

    )

    input_topic.write(make_input_task(mds, FEED_ID, BUSINESS_ID, SHOP_ID, warehouse_id=WAREHOUSE_ID,
                                      shops_dat_parameters=ShopsDatParameters(
                                          color=DTC.DIRECT_STANDBY,
                                          vat=7,
                                          direct_standby=True,
                                          direct_feed_id=FEED_ID
                                      ),
                                      task_type=FEED_CLASS_SELECTIVE_BASIC_PATCH_UPDATE_SERVICE_PATCH_UPDATE,
                                      timestamp=TIMESTAMP
                                      ).SerializeToString())
    push_parser.run(total_sessions=1)

    data = output_topic.read(count=6)

    assert_that(data, HasSerializedDatacampMessages([{
        'united_offers': [
            {
                'offer': [
                    {
                        'basic': {
                            'identifiers': {
                                'business_id': BUSINESS_ID,
                                'offer_id': '{}xXx{}'.format(FEED_ID, 1),
                            },
                            'pictures': {
                                'partner': {
                                    'original': {
                                        'source': [
                                            {
                                                'url': 'url2.com/'
                                            }
                                        ]
                                    }
                                }
                            },
                            'content': {
                                'partner': {
                                    'original': {
                                        'direct_category': None
                                    }
                                }
                            },
                            'tech_info': {
                                'first_parsing': {
                                    'feed_timestamp': TIMESTAMP,
                                }
                            }
                        },
                    },
                ]
            }
        ]
    }]))

    assert_that(data, HasSerializedDatacampMessages([{
        'united_offers': [
            {
                'offer': [
                    {
                        'basic': {
                            'identifiers': {
                                'business_id': BUSINESS_ID,
                                'offer_id': '{}xXx{}'.format(FEED_ID, 0),
                            },
                            'pictures': None,
                            'content': {
                                'partner': {
                                    'original': {
                                        'direct_category': None
                                    }
                                }
                            }
                        },
                    },
                ]
            },
        ]
    }]))

    assert_that(data, HasSerializedDatacampMessages([{
        'united_offers': [
            {
                'offer': [
                    {
                        'basic': {
                            'identifiers': {
                                'business_id': BUSINESS_ID,
                                'offer_id': '{}xXx{}'.format(FEED_ID, 2),
                            },
                            'pictures': None,
                            'content': {
                                'partner': {
                                    'original': {
                                        'direct_category': None
                                    }
                                }
                            }
                        },
                    }
                ]
            }
        ]
    }]))

    assert_that(data, HasSerializedDatacampMessages([{
        'united_offers': [
            {
                'offer': [
                    {
                        'basic': {
                            'identifiers': {
                                'business_id': BUSINESS_ID,
                                'offer_id': '{}xXx{}'.format(FEED_ID, 3),
                            },
                            'content': {
                                'partner': {
                                    'original': {
                                        'direct_category': None
                                    }
                                }
                            }
                        },
                        'service': IsProtobufMap({
                            SHOP_ID: {
                                'stock_info': {},
                                'content': {
                                    'partner': {
                                        'original': {
                                            'direct_category': None
                                        }
                                    }
                                }
                            }
                        }),
                        'actual': empty()
                    }
                ]
            }
        ]
    }]))

    assert_that(data, HasSerializedDatacampMessages([{
        'united_offers': [
            {
                'offer': [
                    {
                        'basic': {
                            'identifiers': {
                                'business_id': BUSINESS_ID,
                                'offer_id': '{}xXx{}'.format(FEED_ID, 4),
                            },
                            'content': {
                                'partner': {
                                    'original': {
                                        'direct_category': None
                                    }
                                }
                            }
                        },
                        'service': IsProtobufMap({
                            SHOP_ID: {
                                'identifiers': {
                                    'feed_id': FEED_ID,
                                    'business_id': BUSINESS_ID,
                                    'shop_id': SHOP_ID,
                                    'warehouse_id': WAREHOUSE_ID,
                                    'offer_id': '{}xXx{}'.format(FEED_ID, 4)
                                },
                                'stock_info': {},
                                'content': {
                                    'partner': {
                                        'original': {
                                            'direct_category': {
                                                'name': 'not same',
                                                'path_category_names': 'root\\not same',
                                                'parent_id': 0,
                                                'path_category_ids': '0\\3',
                                                'id': 3
                                            }
                                        }
                                    }
                                }
                            }
                        }),
                        'actual': empty()
                    }
                ]
            }
        ]
    }]))

    assert_that(data, HasSerializedDatacampMessages([{
        'united_offers': [
            {
                'offer': [
                    {
                        'basic': {
                            'identifiers': {
                                'business_id': BUSINESS_ID,
                                'offer_id': '{}xXx{}'.format(FEED_ID, 5),
                            },
                            'content': {
                                'partner': {
                                    'original': {
                                        'direct_category': None
                                    }
                                }
                            }
                        },
                        'service': IsProtobufMap({
                            SHOP_ID: {
                                'identifiers': {
                                    'feed_id': FEED_ID,
                                    'business_id': BUSINESS_ID,
                                    'shop_id': SHOP_ID,
                                    'warehouse_id': WAREHOUSE_ID,
                                    'offer_id': '{}xXx{}'.format(FEED_ID, 5)
                                },
                                'stock_info': {
                                    'partner_stocks': {
                                        'count': 3
                                    }
                                },
                                'content': {
                                    'partner': {
                                        'original': {
                                            'direct_category': {
                                                'name': 'same2',
                                                'path_category_names': 'root\\same1\\same2',
                                                'parent_id': 1,
                                                'path_category_ids': '0\\1\\4',
                                                'id': 4
                                            }
                                        }
                                    }
                                }
                            }
                        }),
                        'actual': empty()
                    }
                ]
            }
        ]
    }]))
