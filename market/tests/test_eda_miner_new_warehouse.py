# coding: utf-8

from hamcrest import assert_that, equal_to
from datetime import datetime, timedelta
import pytest


import market.idx.datacamp.proto.offer.DataCampOffer_pb2 as DTC
from market.idx.datacamp.proto.api.DatacampMessage_pb2 import DatacampMessage

from market.idx.datacamp.controllers.piper.yatf.test_env_old import PiperTestEnv
from market.idx.datacamp.controllers.piper.yatf.resources.config_mock import PiperConfigMock

from market.idx.yatf.resources.datacamp.datacamp_tables import DataCampBasicOffersTable, DataCampServiceOffersTable, DataCampPartnersTable

from market.idx.yatf.resources.lbk_topic import LbkTopic

from market.idx.pylibrary.datacamp.utils import wait_until
from market.idx.yatf.utils.utils import create_timestamp_from_json
from market.pylibrary.proto_utils import message_from_data

from market.idx.pylibrary.datacamp.conversion import offer_to_service_row, offer_to_basic_row

from market.idx.datacamp.yatf.utils import dict2tskv


NOW_UTC = datetime.utcnow()  # JSON serializer should always use UTC
time_pattern = "%Y-%m-%dT%H:%M:%SZ"
current_time = NOW_UTC.strftime(time_pattern)
PREV_UTC = NOW_UTC - timedelta(days=10)
prev_time = PREV_UTC.strftime(time_pattern)
prev_ts = create_timestamp_from_json(prev_time)
NEXT_UTC = NOW_UTC + timedelta(days=10)
next_time = NEXT_UTC.strftime(time_pattern)

BASIC_TABLE_DATA = [
    offer_to_basic_row(DTC.Offer(
        identifiers=DTC.OfferIdentifiers(business_id=1, offer_id='T1000'),
        tech_info=DTC.OfferTechInfo(
            last_mining=DTC.MiningTrace(
                meta=DTC.UpdateMeta(
                    timestamp=prev_ts
                ),
                send_to_mining=prev_ts
            ),
        ),
        meta=DTC.OfferMeta(
            rgb=DTC.EDA
        )
    )),
    offer_to_basic_row(DTC.Offer(
        identifiers=DTC.OfferIdentifiers(business_id=2, offer_id='T2000'),
        tech_info=DTC.OfferTechInfo(
            last_mining=DTC.MiningTrace(
                meta=DTC.UpdateMeta(
                    timestamp=prev_ts
                ),
                send_to_mining=prev_ts
            ),
        ),
        status=DTC.OfferStatus(
            removed=DTC.Flag(
                flag=True
            )
        ),
        meta=DTC.OfferMeta(
            rgb=DTC.EDA
        )
    )),
]

SERVICE_TABLE_DATA = [
    offer_to_service_row(DTC.Offer(
        identifiers=DTC.OfferIdentifiers(business_id=1, offer_id='T1000', shop_id=1),
        tech_info=DTC.OfferTechInfo(
            last_mining=DTC.MiningTrace(
                meta=DTC.UpdateMeta(
                    timestamp=prev_ts
                ),
                send_to_mining=prev_ts
            ),
        ),
        meta=DTC.OfferMeta(
            rgb=DTC.EDA
        )
    )),
    offer_to_service_row(DTC.Offer(
        identifiers=DTC.OfferIdentifiers(business_id=2, offer_id='T2000', shop_id=2),
        tech_info=DTC.OfferTechInfo(
            last_mining=DTC.MiningTrace(
                meta=DTC.UpdateMeta(
                    timestamp=prev_ts
                ),
                send_to_mining=prev_ts
            ),
        ),
        status=DTC.OfferStatus(
            removed=DTC.Flag(
                flag=True
            )
        ),
        meta=DTC.OfferMeta(
            rgb=DTC.EDA
        )
    ))
]

ACTUAL_SERVICE_TABLE_DATA = [
    offer_to_service_row(DTC.Offer(
        identifiers=DTC.OfferIdentifiers(business_id=1, offer_id='T1000', shop_id=1, warehouse_id=0),
        tech_info=DTC.OfferTechInfo(
            last_mining=DTC.MiningTrace(
                meta=DTC.UpdateMeta(
                    timestamp=prev_ts
                ),
                send_to_mining=prev_ts
            ),
        ),
        delivery=DTC.OfferDelivery(
            delivery_info=DTC.DeliveryInfo(
                has_delivery=True,
                pickup=True,
                store=True
            ),
            market=DTC.MarketDelivery(
                use_yml_delivery=DTC.Flag(
                    flag=True,
                )
            ),
        ),
        meta=DTC.OfferMeta(
            rgb=DTC.EDA
        )
    ))
]


def datacamp_messages_update(business_id, offer_id, shop_id, warehouse_id, removed, time):
    return [
        {
            'united_offers': [{
                'offer': [{
                    'basic': {
                        'identifiers': {
                            'business_id': business_id,
                            'offer_id': offer_id,
                        },
                        'meta': {
                            'scope': DTC.BASIC,
                            'ts_created': time
                        },
                        # 'status': {
                        #     'removed': removed
                        # },
                        'tech_info': {
                            'last_mining': {
                                'meta': {
                                    'timestamp': time
                                },
                                'send_to_mining': time,
                            },
                        },
                    },
                    'service': {
                        shop_id: {
                            'identifiers': {
                                'business_id': business_id,
                                'offer_id': offer_id,
                                'shop_id': shop_id,
                                'warehouse_id': warehouse_id,
                                'extra' : {
                                    'shop_sku': offer_id,
                                }
                            },
                            'tech_info': {
                                'last_mining': {
                                    'meta': {
                                        'timestamp': time
                                    },
                                    'send_to_mining': time,
                                },
                            },
                            # 'status': {
                            #     'removed': removed
                            # },
                            'meta': {
                                'scope': DTC.SERVICE,
                                'rgb': DTC.EDA,
                                'ts_created': time
                            },
                        }
                    }
                }]
            }]
        }
    ]


@pytest.fixture(scope='module')
def partners_table(yt_server, config):
    return DataCampPartnersTable(
        yt_server,
        config.yt_partners_tablepath,
        data=[
            {
                'shop_id': 1,
                'mbi': '\n\n'.join([
                    dict2tskv({
                        'shop_id': 1,
                        'business_id': 1,
                        'is_discounts_enabled':	True,
                        'is_eats': True,
                        'is_enabled': True,
                        'is_online': True,
                        'warehouse_id': 61059,
                        'cpc': 'NO',
                        'united_catalog_status': 'SUCCESS',
                        'is_site_market': 'true',
                    }),
                ]),
            },
        ],
    )


@pytest.fixture(scope='module')
def miner_input_topic(log_broker_stuff):
    topic = LbkTopic(log_broker_stuff)
    return topic


@pytest.fixture(scope='module')
def basic_offers_table(yt_server, config):
    return DataCampBasicOffersTable(
        yt_server, config.yt_basic_offers_tablepath, data=BASIC_TABLE_DATA
    )


@pytest.fixture(scope='module')
def service_offers_table(yt_server, config):
    return DataCampServiceOffersTable(
        yt_server, config.yt_service_offers_tablepath, data=SERVICE_TABLE_DATA
    )


@pytest.fixture(scope='module')
def actual_service_offers_table(yt_server, config):
    return DataCampServiceOffersTable(
        yt_server, config.yt_actual_service_offers_tablepath, data=ACTUAL_SERVICE_TABLE_DATA
    )


@pytest.fixture(scope='module')
def config(yt_server, log_broker_stuff, miner_input_topic):
    cfg = {
        'general': {
            'color': 'white',
        },
        'logbroker': {
            'miner_input_topic': miner_input_topic.topic,
        },
    }
    return PiperConfigMock(yt_server=yt_server, log_broker_stuff=log_broker_stuff, config=cfg)


@pytest.yield_fixture(scope='module')
def piper(yt_server, log_broker_stuff, config, miner_input_topic, basic_offers_table, service_offers_table, actual_service_offers_table, partners_table):
    resources = {
        'config': config,
        'miner_input_topic': miner_input_topic,
        'basic_offers_table': basic_offers_table,
        'service_offers_table': service_offers_table,
        'actual_service_offers_table': actual_service_offers_table,
        'partners_table': partners_table,
    }
    with PiperTestEnv(yt_server, log_broker_stuff, None, True, **resources) as piper_env:
        piper_env.verify()
        yield piper_env


def test_miner_to_white_piper_new_warehouse(
    piper,
    miner_input_topic,
):
    """
    Проверяем, что данные из miner'а для еды создадут оффер с новым складом
    """
    for offer in datacamp_messages_update(1, 'T1000', 1, 1, False, current_time):
        miner_input_topic.write(message_from_data(offer, DatacampMessage()).SerializeToString())

    wait_until(lambda: piper.united_offers_processed >= 1)

    assert_that(len(piper.basic_offers_table.data), equal_to(2))
    assert_that(len(piper.service_offers_table.data), equal_to(2))
    assert_that(len(piper.actual_service_offers_table.data), equal_to(2))

    for offer in datacamp_messages_update(1, 'T1000', 1, 1, False, next_time):
        miner_input_topic.write(message_from_data(offer, DatacampMessage()).SerializeToString())

    wait_until(lambda: piper.united_offers_processed >= 2)

    assert_that(len(piper.basic_offers_table.data), equal_to(2))
    assert_that(len(piper.service_offers_table.data), equal_to(2))
    assert_that(len(piper.actual_service_offers_table.data), equal_to(2))


def test_miner_to_piper_removed(
    piper,
    miner_input_topic
):
    """
    Проверяем, что не будут создаваться новые актуальные части, если оффер помечен на удаление
    """
    for offer in datacamp_messages_update(2, 'T2000', 1, 1, False, current_time):
        miner_input_topic.write(message_from_data(offer, DatacampMessage()).SerializeToString())

    wait_until(lambda: piper.united_offers_processed >= 1)

    assert_that(len(piper.basic_offers_table.data), equal_to(2))
    assert_that(len(piper.service_offers_table.data), equal_to(3))
    assert_that(len(piper.actual_service_offers_table.data), equal_to(2))
