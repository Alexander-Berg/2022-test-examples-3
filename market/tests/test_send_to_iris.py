# coding: utf-8

import pytest
from datetime import datetime
from hamcrest import assert_that, equal_to, not_

from market.idx.datacamp.proto.api.DatacampMessage_pb2 import DatacampMessage
import market.idx.datacamp.proto.offer.DataCampOffer_pb2 as DTC
from market.idx.datacamp.controllers.piper.yatf.resources.config_mock import PiperConfigMock
from market.idx.datacamp.controllers.piper.yatf.test_env_old import PiperTestEnv
from market.idx.pylibrary.datacamp.utils import wait_until
from market.idx.datacamp.yatf.utils import create_meta, create_update_meta
from market.idx.yatf.resources.datacamp.datacamp_tables import DataCampBasicOffersTable, DataCampServiceOffersTable
from market.idx.yatf.matchers.lb_matchers import HasNoUnreadData
from market.idx.yatf.matchers.protobuf_matchers import IsSerializedProtobuf, IsProtobufMap
from market.idx.yatf.resources.lbk_topic import LbkTopic
from market.pylibrary.proto_utils import message_from_data
from market.idx.pylibrary.datacamp.conversion import offer_to_service_row, offer_to_basic_row
from market.idx.yatf.resources.datacamp.datacamp_tables import DataCampPartnersTable
from market.idx.datacamp.yatf.utils import dict2tskv


NOW_UTC = datetime.utcnow()  # JSON serializer should always use UTC
time_pattern = "%Y-%m-%dT%H:%M:%SZ"
current_time = NOW_UTC.strftime(time_pattern)

BUSINESS_ID = 1
# синий магазин с офферами, размещающимися в едином каталоге ->
# должны отправлять его оффера
BLUE_SHOP_ID = 2
# синий магазин с офферами, не размещающимися в едином каталоге ->
# НЕ должны отправлять его оффера
BLUE_SHOP_ID_NOT_IN_UC = 3
# НЕ должны отправлять не-синие оффера
WHITE_SHOP_ID = 4
WAREHOUSE_ID = 145

SHOP_ID_WITH_FULFULLMENT_PROGRAM_NOT_IN_UC = 222
SHOP_ID_WITH_FULFULLMENT_PROGRAM = 8884
SHOP_ID_WITH_CROSS_DOCK_PROGRAM = 774
SHOP_ID_WITH_CLICK_N_COLLECT = 44
PARTNER_DATA = [
    {
        'shop_id': SHOP_ID_WITH_FULFULLMENT_PROGRAM,
        'mbi':  dict2tskv({
            'shop_id': SHOP_ID_WITH_FULFULLMENT_PROGRAM,
            'blue_status': 'REAL',
            'datafeed_id': 888,
            'warehouse_id': 77,
            'ff_program': 'REAL',
            'direct_shipping': True,
        }),
        'status': 'publish'
    },
    {
        'shop_id': SHOP_ID_WITH_FULFULLMENT_PROGRAM_NOT_IN_UC,
        'mbi':  dict2tskv({
            'shop_id': SHOP_ID_WITH_FULFULLMENT_PROGRAM_NOT_IN_UC,
            'blue_status': 'REAL',
            'datafeed_id': 888,
            'warehouse_id': 77,
            'ff_program': 'REAL',
            'direct_shipping': True,
        }),
        'status': 'publish'
    },
    {
        'shop_id': SHOP_ID_WITH_CROSS_DOCK_PROGRAM,
        'mbi':  dict2tskv({
            'blue_status': 'REAL',
            'shop_id': SHOP_ID_WITH_CROSS_DOCK_PROGRAM,
            'datafeed_id': 7334,
            'ff_program': 'REAL',
        }),
        'status': 'publish'
    },
    {
        'shop_id': SHOP_ID_WITH_CLICK_N_COLLECT,
        'mbi':  dict2tskv({
            'blue_status': 'REAL',
            'shop_id': SHOP_ID_WITH_CLICK_N_COLLECT,
            'datafeed_id': 2343,
            'ignore_stocks': True
        }),
        'status': 'publish'
    },
]


def service_part(offer_id, shop_id, rgb):
    return {
        'identifiers': {
            'business_id': BUSINESS_ID,
            'offer_id': offer_id,
            'shop_id': shop_id,
            'warehouse_id': WAREHOUSE_ID,
        },
        'meta': {
            'scope': DTC.SERVICE,
            'rgb': rgb,
            'ts_created': NOW_UTC.strftime(time_pattern)
        },
    }


DATACAMP_MESSAGES = [
    {
        'united_offers': [
            {
                'offer': [
                    {
                        'basic': {
                            'identifiers': {
                                'business_id': BUSINESS_ID,
                                'offer_id': 'T1000',
                            },
                            'meta': {'scope': DTC.BASIC, 'ts_created': current_time},
                            'content': {
                                'master_data': {
                                    'box_count': {
                                        'meta': {
                                            'timestamp': current_time,
                                        },
                                        'value': 2
                                    }
                                }
                            }
                        },
                        'service': {
                            WHITE_SHOP_ID: service_part('T1000', WHITE_SHOP_ID, DTC.WHITE),
                            SHOP_ID_WITH_CLICK_N_COLLECT: service_part('T1000', SHOP_ID_WITH_CLICK_N_COLLECT, DTC.BLUE),
                            SHOP_ID_WITH_CROSS_DOCK_PROGRAM: service_part('T1000', SHOP_ID_WITH_CROSS_DOCK_PROGRAM, DTC.BLUE),
                            SHOP_ID_WITH_FULFULLMENT_PROGRAM: service_part('T1000', SHOP_ID_WITH_FULFULLMENT_PROGRAM, DTC.BLUE),
                            SHOP_ID_WITH_FULFULLMENT_PROGRAM_NOT_IN_UC: service_part('T1000', SHOP_ID_WITH_FULFULLMENT_PROGRAM_NOT_IN_UC, DTC.BLUE)
                        },
                    }
                ]
            },
            {
                'offer': [
                    {
                        'basic': {
                            'identifiers': {
                                'business_id': BUSINESS_ID,
                                'offer_id': 'Offer1',
                            },
                            'meta': {'scope': DTC.BASIC, 'ts_created': current_time},
                            'content': {
                                'master_data': {
                                    'box_count': {
                                        'meta': {
                                            'timestamp': current_time,
                                        },
                                        'value': 2
                                    }
                                }
                            }
                        },
                        'service': {
                            SHOP_ID_WITH_CLICK_N_COLLECT: service_part('Offer1', SHOP_ID_WITH_CLICK_N_COLLECT, DTC.BLUE)
                        }
                    }
                ]
            }
        ]
    }
]


@pytest.fixture(scope='module')
def datacamp_messages_topic(log_broker_stuff):
    topic = LbkTopic(log_broker_stuff)
    return topic


@pytest.fixture(scope='module')
def iris_topic(log_broker_stuff):
    topic = LbkTopic(log_broker_stuff)
    return topic


@pytest.fixture(scope='module')
def config(yt_server, log_broker_stuff, datacamp_messages_topic, iris_topic):
    cfg = {
        'general': {
            'color': 'white',
        },
        'logbroker': {
            'datacamp_messages_topic': datacamp_messages_topic.topic,
            'iris_topic': iris_topic.topic
        },
    }
    return PiperConfigMock(yt_server=yt_server, log_broker_stuff=log_broker_stuff, config=cfg)


@pytest.fixture(scope='module')
def basic_offers_table(yt_server, config):
    return DataCampBasicOffersTable(
        yt_server,
        config.yt_basic_offers_tablepath,
        data=[
            offer_to_basic_row(DTC.Offer(
                identifiers=DTC.OfferIdentifiers(
                    business_id=BUSINESS_ID,
                    offer_id='T1000',
                ),
                meta=create_meta(10),
                content=DTC.OfferContent(
                    master_data=DTC.MarketMasterData(
                        box_count=DTC.Ui32Value(
                            meta=create_update_meta(10),
                            value=4,
                        )
                    )
                )
            )),
            offer_to_basic_row(DTC.Offer(
                identifiers=DTC.OfferIdentifiers(
                    business_id=BUSINESS_ID,
                    offer_id='Offer1',
                ),
                meta=create_meta(10),
                content=DTC.OfferContent(
                    master_data=DTC.MarketMasterData(
                        box_count=DTC.Ui32Value(
                            meta=create_update_meta(10),
                            value=4,
                        )
                    )
                )
            ))
        ])


def service_row(offer_id, shop_id, in_uc_flag, color):
    return offer_to_service_row(DTC.Offer(
        identifiers=DTC.OfferIdentifiers(
            business_id=BUSINESS_ID,
            offer_id=offer_id,
            shop_id=shop_id,
        ),
        meta=create_meta(10, color=color),
        status=DTC.OfferStatus(
            united_catalog=DTC.Flag(
                flag=in_uc_flag,
            )
        ),
    ))


@pytest.fixture(scope='module')
def service_offers_table(yt_server, config):
    return DataCampServiceOffersTable(
        yt_server,
        config.yt_service_offers_tablepath,
        data=[
            service_row('T1000', SHOP_ID_WITH_CROSS_DOCK_PROGRAM, True, DTC.BLUE),
            service_row('T1000', SHOP_ID_WITH_FULFULLMENT_PROGRAM, True, DTC.BLUE),
            service_row('T1000', SHOP_ID_WITH_FULFULLMENT_PROGRAM_NOT_IN_UC, False, DTC.BLUE),
            service_row('T1000', SHOP_ID_WITH_CLICK_N_COLLECT, True, DTC.BLUE),
            service_row('T1000', WHITE_SHOP_ID, False, DTC.WHITE),
            service_row('Offer1', SHOP_ID_WITH_CLICK_N_COLLECT, True, DTC.BLUE),
            ])


def actual_service_row(offer_id, shop_id, color):
    return offer_to_service_row(DTC.Offer(
        identifiers=DTC.OfferIdentifiers(
            business_id=BUSINESS_ID,
            offer_id=offer_id,
            shop_id=shop_id,
            warehouse_id=WAREHOUSE_ID
        ),
        meta=create_meta(10, color=color),
    ))


@pytest.fixture(scope='module')
def actual_service_offers_table(yt_server, config):
    return DataCampServiceOffersTable(
        yt_server,
        config.yt_actual_service_offers_tablepath,
        data=[
            actual_service_row('T1000', SHOP_ID_WITH_CROSS_DOCK_PROGRAM, DTC.BLUE),
            actual_service_row('T1000', SHOP_ID_WITH_FULFULLMENT_PROGRAM, DTC.BLUE),
            actual_service_row('T1000', SHOP_ID_WITH_FULFULLMENT_PROGRAM_NOT_IN_UC, DTC.BLUE),
            actual_service_row('T1000', SHOP_ID_WITH_CLICK_N_COLLECT, DTC.BLUE),
            actual_service_row('T1000', WHITE_SHOP_ID, DTC.WHITE),
            actual_service_row('Offer1', SHOP_ID_WITH_CLICK_N_COLLECT, DTC.BLUE)
            ])


@pytest.yield_fixture(scope='module')
def piper(
    yt_server,
    log_broker_stuff,
    config,
    datacamp_messages_topic,
    iris_topic,
    basic_offers_table,
    service_offers_table,
    actual_service_offers_table
):
    resources = {
        'config': config,
        'datacamp_messages_topic': datacamp_messages_topic,
        'iris_topic': iris_topic,
        'basic_offers_table': basic_offers_table,
        'service_offers_table': service_offers_table,
        'actual_service_offers_table': actual_service_offers_table,
        'partners_table': DataCampPartnersTable(
            yt_stuff=yt_server,
            path=config.yt_partners_tablepath,
            data=PARTNER_DATA)
    }
    with PiperTestEnv(yt_server, log_broker_stuff, **resources) as piper_env:
        piper_env.verify()
        yield piper_env


def test_write_to_iris(piper, datacamp_messages_topic, iris_topic):
    for message in DATACAMP_MESSAGES:
        datacamp_messages_topic.write(message_from_data(message, DatacampMessage()).SerializeToString())
    wait_until(lambda: piper.united_offers_processed >= 2)
    data = iris_topic.read(count=1)

    m = DatacampMessage()
    m.ParseFromString(data[0])
    print('Received message:\n{}'.format(m))

    assert_that(len(data), equal_to(1))
    assert_that(
        data[0],
        IsSerializedProtobuf(
            DatacampMessage,
            {
                'united_offers': [
                    {
                        'offer': [
                            {
                                'basic': {
                                    'identifiers': {
                                        'business_id': BUSINESS_ID,
                                        'offer_id': 'T1000',
                                    },
                                    'content': {
                                        'master_data': {
                                            'box_count': {'value': 2}
                                        }
                                    }
                                },
                                'service': IsProtobufMap(
                                    {
                                        shop_id: {
                                            'identifiers': {
                                                'business_id': BUSINESS_ID,
                                                'shop_id': shop_id,
                                                'offer_id': 'T1000',
                                            }
                                        }
                                    }
                                )
                            } for shop_id in [SHOP_ID_WITH_CROSS_DOCK_PROGRAM, SHOP_ID_WITH_FULFULLMENT_PROGRAM]
                        ]
                    }
                ]
            },
        ),
    )

    for shop_id, offer_id in [(SHOP_ID_WITH_CLICK_N_COLLECT, 'T1000'), (WHITE_SHOP_ID, 'T1000'), (SHOP_ID_WITH_CLICK_N_COLLECT, 'Offer1')]:
        assert_that(
            data[0],
            not_(
                IsSerializedProtobuf(
                    DatacampMessage,
                    {
                        'united_offers': [
                            {
                                'offer': [
                                    {
                                        'service': IsProtobufMap(
                                            {
                                                shop_id: {
                                                    'identifiers': {
                                                        'business_id': BUSINESS_ID,
                                                        'shop_id': shop_id,
                                                        'offer_id': offer_id,
                                                    },
                                                },
                                            }
                                        ),
                                    }
                                ]
                            }
                        ]
                    },
                )
            ),
        )

    # проверяем, что в топике больше нет данных, которые мы можем вычитать
    assert_that(iris_topic, HasNoUnreadData())
