# coding: utf-8

import pytest
from datetime import datetime
from hamcrest import assert_that, has_items, greater_than, not_none

from market.pylibrary.proto_utils import message_from_data

from market.idx.datacamp.proto.api.DatacampMessage_pb2 import DatacampMessage
from market.idx.datacamp.proto.api.ExportMessage_pb2 import ExportMessage
import market.idx.datacamp.proto.offer.DataCampOffer_pb2 as DTC
from market.idx.pylibrary.datacamp.utils import wait_until
from market.idx.datacamp.dispatcher.yatf.test_env import DispatcherTestEnv
from market.idx.datacamp.dispatcher.yatf.resources.config import DispatcherConfig

from market.idx.datacamp.controllers.piper.yatf.test_env_old import PiperTestEnv
from market.idx.datacamp.controllers.piper.yatf.resources.config_mock import PiperConfigMock

from market.idx.yatf.matchers.lb_matchers import HasNoUnreadData
from market.idx.yatf.matchers.protobuf_matchers import IsSerializedProtobuf
from market.idx.datacamp.picrobot.proto.event_pb2 import TImageRequest
from market.idx.yatf.resources.lbk_topic import LbkTopic
from market.idx.yatf.resources.datacamp.datacamp_tables import DataCampBasicOffersTable, DataCampServiceOffersTable
from market.idx.datacamp.yatf.utils import create_meta, create_update_meta
from market.idx.pylibrary.datacamp.conversion import offer_to_service_row, offer_to_basic_row


BUSINESS_ID = 1
OFFER_ID = 'T1000'
SHOP_ID = 2
NEW_SHOP_ID = 3
WAREHOUSE_ID = 3
UC_DATA_VERSION = 1
SERVICE_PRICE = 20
CATEGORY_ID = 1
CATEGORY_NAME = 'cat_name'
OGRN = 'sup_ogrn'
SUPPLIER_NAME = 'sup'

NOW_UTC = datetime.utcnow()
CURRENT_TIME = NOW_UTC.strftime("%Y-%m-%dT%H:%M:%SZ")
CURRENT_TIMESTAMP = int((NOW_UTC - datetime(1970, 1, 1)).total_seconds())


BASIC_TABLE_DATA = [
    offer_to_basic_row(
        DTC.Offer(
            identifiers=DTC.OfferIdentifiers(business_id=BUSINESS_ID, offer_id=OFFER_ID),
            pictures=DTC.OfferPictures(
                partner=DTC.PartnerPictures(
                    original=DTC.SourcePictures(
                        meta=create_update_meta(0),
                        source=[
                            DTC.SourcePicture(url='2000url1/'),
                            DTC.SourcePicture(url='https://2000url2/'),
                            DTC.SourcePicture(url='2000url3/'),
                            DTC.SourcePicture(url='https://2000url4/'),
                        ],
                    )
                )
            ),
            status=DTC.OfferStatus(
                consistency=DTC.ConsistencyStatus(
                    pricelabs_consistency=False
                )
            )
        )
    ),
]

SERVICE_TABLE_DATA = [
    {
        'business_id': BUSINESS_ID,
        'shop_sku': 'T1000',
        'shop_id': SHOP_ID,
        'outlet_id': 0,
        'offer': DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=BUSINESS_ID,
                offer_id='T1000',
                shop_id=SHOP_ID,
            ),
            meta=create_meta(10, color=DTC.WHITE),
            content=DTC.OfferContent(
                partner=DTC.PartnerContent(
                    original=DTC.OriginalSpecification(
                        name=DTC.StringValue(
                            meta=create_update_meta(10),
                            value="name",
                        )
                    )
                )
            ),
        ).SerializeToString(),
    }
]

ACTUAL_SERVICE_TABLE_DATA = [
    offer_to_service_row(
        DTC.Offer(
            identifiers=DTC.OfferIdentifiers(business_id=BUSINESS_ID, offer_id=OFFER_ID, shop_id=SHOP_ID),
        )
    )
]


PICTURES_UPDATE = {
    'united_offers': [
        {
            'offer': [
                {
                    'basic': {
                        'identifiers': {
                            'business_id': BUSINESS_ID,
                            'offer_id': OFFER_ID,
                        },
                        'pictures': {
                            'partner': {
                                'original': {
                                    'meta': {'timestamp': CURRENT_TIME},
                                    'source': [
                                        {'url': 'https://2000url2/'},
                                        {'url': 'https://newsourceurl/'},
                                        {'url': 'new2url/'},
                                        {'url': '2000url3/'},
                                    ],
                                }
                            }
                        },
                    }
                }
            ]
        }
    ]
}

PRICELABS_UPDATE = {
    'united_offers': [
        {
            'offer': [
                {
                    'basic': {
                        'identifiers': {
                            'business_id': BUSINESS_ID,
                            'offer_id': OFFER_ID,
                        },
                        'status': {
                            'version': {'uc_data_version': {'counter': UC_DATA_VERSION}},
                        },
                        'content': {
                            'market': {
                                'real_uc_version': {'counter': UC_DATA_VERSION},
                            },
                            'partner': {
                                'original': {
                                    'category': {
                                        'meta': {'timestamp': CURRENT_TIME},
                                        'id': CATEGORY_ID,
                                        'name': CATEGORY_NAME,
                                    },
                                }
                            },
                        },
                    },
                    'service': {
                        NEW_SHOP_ID: {
                            'identifiers': {
                                'business_id': BUSINESS_ID,
                                'offer_id': OFFER_ID,
                                'shop_id': NEW_SHOP_ID,
                            },
                            'meta': {
                                'ts_created': CURRENT_TIME,
                                'rgb': 1,
                            },
                            'price': {
                                'basic': {
                                    'binary_price': {'price': SERVICE_PRICE},
                                }
                            },
                            'content': {
                                'partner': {'original': {'supplier_info': {'ogrn': OGRN, 'name': SUPPLIER_NAME}}}
                            },
                        }
                    },
                }
            ]
        }
    ]
}


@pytest.fixture(scope='module', params=[True, False])
def send_to_dispatcher_via_qyt(request):
    return request.param


@pytest.fixture(scope='module')
def datacamp_messages_topic(log_broker_stuff, send_to_dispatcher_via_qyt):
    topic = LbkTopic(log_broker_stuff)
    return topic


@pytest.fixture(scope='module')
def subscription_service_topic(log_broker_stuff, send_to_dispatcher_via_qyt):
    topic = LbkTopic(log_broker_stuff, 'subscription-service')
    return topic


@pytest.fixture(scope='module')
def picrobot_topic(log_broker_stuff, send_to_dispatcher_via_qyt):
    topic = LbkTopic(log_broker_stuff)
    return topic


@pytest.fixture(scope='module')
def pricelabs_topic(log_broker_stuff, send_to_dispatcher_via_qyt):
    topic = LbkTopic(log_broker_stuff)
    return topic


@pytest.fixture(scope='module')
def piper_config(
        yt_server, log_broker_stuff, datacamp_messages_topic, subscription_service_topic, send_to_dispatcher_via_qyt, qyt_subscription
):
    cfg = {
        'general': {
            'color': 'white',
            'batch_size': 10,
            'send_to_dispatcher_via_qyt': send_to_dispatcher_via_qyt
        },
        'yt': {
            'dispatcher_queue_path': qyt_subscription.path if send_to_dispatcher_via_qyt else None,
        },
        'logbroker': {
            'datacamp_messages_topic': datacamp_messages_topic.topic,
            'subscription_service_topic': subscription_service_topic.topic,
        },
    }
    return PiperConfigMock(yt_server=yt_server, log_broker_stuff=log_broker_stuff, config=cfg)


@pytest.fixture(scope='module')
def basic_offers_table(yt_server, piper_config):
    return DataCampBasicOffersTable(yt_server, piper_config.yt_basic_offers_tablepath, data=BASIC_TABLE_DATA)


@pytest.fixture(scope='module')
def service_offers_table(yt_server, piper_config):
    return DataCampServiceOffersTable(yt_server, piper_config.yt_service_offers_tablepath, data=SERVICE_TABLE_DATA)


@pytest.fixture(scope='module')
def actual_service_offers_table(yt_server, piper_config):
    return DataCampServiceOffersTable(
        yt_server, piper_config.yt_actual_service_offers_tablepath, data=ACTUAL_SERVICE_TABLE_DATA
    )


@pytest.fixture(scope='module')
def dispatcher_config(
    yt_token,
    yt_server,
    log_broker_stuff,
    piper_config,
    send_to_dispatcher_via_qyt,
    pricelabs_topic,
    picrobot_topic,
    subscription_service_topic,
    qyt_subscription,
    port_manager,
):
    cfg = DispatcherConfig()
    cfg.create_initializer(yt_server=yt_server, yt_token_path=yt_token.path)

    reader = cfg.create_dispatcher_reader(yt_server, qyt_subscription, port_manager) if send_to_dispatcher_via_qyt else cfg.create_lb_reader(log_broker_stuff, subscription_service_topic)
    unpacker = cfg.create_subscription_message_unpacker()
    dispatcher = cfg.create_subscription_dispatcher(
        piper_config.yt_basic_offers_tablepath, piper_config.yt_service_offers_tablepath, piper_config.yt_actual_service_offers_tablepath
    )
    cfg.create_link(reader, unpacker)
    cfg.create_link(unpacker, dispatcher)

    picrobot_filter = cfg.create_subscription_filter('PICROBOT_SUBSCRIBER', extra_params={
        'OfferPictureUrlsAsSet': 'true',
        'Color': 'UNKNOWN_COLOR;WHITE;DIRECT_SEARCH_SNIPPET_GALLERY;VERTICAL_GOODS_ADS'
    })
    picrobot_sender = cfg.create_picrobot_sender()
    picrobot_lb_writer = cfg.create_lb_writer(log_broker_stuff, picrobot_topic)

    cfg.create_link(dispatcher, picrobot_filter)
    cfg.create_link(picrobot_filter, picrobot_sender)
    cfg.create_link(picrobot_sender, picrobot_lb_writer)

    pricelabs_filter = cfg.create_subscription_filter('PRICELABS_SUBSCRIBER')
    pricelabs_sender = cfg.create_subscription_sender(extra_params={'ExportFormat': 'EXTERNAL'})
    pricelabs_lb_writer = cfg.create_lb_writer(log_broker_stuff, pricelabs_topic)

    cfg.create_link(dispatcher, pricelabs_filter)
    cfg.create_link(pricelabs_filter, pricelabs_sender)
    cfg.create_link(pricelabs_sender, pricelabs_lb_writer)

    return cfg


@pytest.yield_fixture(scope='module')
def dispatcher(
    dispatcher_config,
    basic_offers_table,
    service_offers_table,
    actual_service_offers_table,
    subscription_service_topic,
    picrobot_topic,
    pricelabs_topic,
    send_to_dispatcher_via_qyt,
    qyt_subscription
):
    resources = {
        'dispatcher_config': dispatcher_config,
        'basic_offers_table': basic_offers_table,
        'service_offers_table': service_offers_table,
        'actual_service_offers_table': actual_service_offers_table,
        'subscription_service_topic': subscription_service_topic,
        'picrobot_topic': picrobot_topic,
        'pricelabs_topic': pricelabs_topic,
    }
    if send_to_dispatcher_via_qyt:
        resources['qyt'] = qyt_subscription

    with DispatcherTestEnv(**resources) as env:
        env.verify()
        yield env


@pytest.yield_fixture(scope='module')
def piper(
    yt_server,
    log_broker_stuff,
    piper_config,
    datacamp_messages_topic,
    actual_service_offers_table,
    service_offers_table,
    basic_offers_table,
    subscription_service_topic,
):
    resources = {
        'config': piper_config,
        'datacamp_messages_topic': datacamp_messages_topic,
        'actual_service_offers_table': actual_service_offers_table,
        'service_offers_table': service_offers_table,
        'basic_offers_table': basic_offers_table,
        'subscription_service_topic': subscription_service_topic
    }

    with PiperTestEnv(yt_server, log_broker_stuff, **resources) as piper_env:
        piper_env.verify()
        yield piper_env


def test_send_to_picrobot(dispatcher, piper, datacamp_messages_topic, picrobot_topic):
    datacamp_messages_topic.write(message_from_data(PICTURES_UPDATE, DatacampMessage()).SerializeToString())

    wait_until(lambda: dispatcher.subscription_dispatcher_processed >= 1, timeout=120)
    picrobot_data = picrobot_topic.read(2, wait_timeout=120)
    assert_that(
        picrobot_data,
        has_items(
            IsSerializedProtobuf(TImageRequest, {'Url': 'https://newsourceurl/'}),
            IsSerializedProtobuf(TImageRequest, {'Url': 'http://new2url/'}),
        ),
    )
    assert_that(picrobot_topic, HasNoUnreadData())


def test_send_to_pricelabs(dispatcher, piper, datacamp_messages_topic, pricelabs_topic):
    datacamp_messages_topic.write(message_from_data(PRICELABS_UPDATE, DatacampMessage()).SerializeToString())

    wait_until(lambda: dispatcher.subscription_dispatcher_processed >= 1, timeout=120)
    pricelabs_data = pricelabs_topic.read(count=1, wait_timeout=120)

    assert_that(
        pricelabs_data,
        has_items(
            IsSerializedProtobuf(
                ExportMessage,
                {
                    'offer': {
                        'offer_id': OFFER_ID,
                        'business_id': BUSINESS_ID,
                        'offer_yabs_id': not_none(),
                        'original_content': {
                            'shop_category_id': CATEGORY_ID,
                            'shop_category_name': CATEGORY_NAME,
                            'supplier': OGRN,
                        },
                        'price': {'currency': 1, 'price': SERVICE_PRICE},
                        'shop_id': NEW_SHOP_ID,
                        'timestamp': {'seconds': CURRENT_TIMESTAMP},
                        'service': {'platform': 1},
                        'version': {'version': greater_than(1)},
                    }
                },
            )
        ),
    )

    assert_that(pricelabs_topic, HasNoUnreadData())
