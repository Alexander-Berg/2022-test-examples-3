# coding: utf-8

from hamcrest import assert_that, equal_to, has_items, empty
from datetime import datetime
import pytest

import market.idx.datacamp.proto.offer.DataCampOffer_pb2 as DTC
from market.idx.datacamp.proto.api.DatacampMessage_pb2 import DatacampMessage

from market.idx.datacamp.controllers.piper.yatf.test_env_old import PiperTestEnv
from market.idx.datacamp.controllers.piper.yatf.resources.config_mock import PiperConfigMock

from market.idx.yatf.matchers.lb_matchers import HasNoUnreadData
from market.idx.yatf.matchers.protobuf_matchers import IsSerializedProtobuf, IsProtobufMap
from market.idx.yatf.matchers.yt_rows_matchers import HasOffers
from market.idx.yatf.resources.lbk_topic import LbkTopic

from market.idx.pylibrary.datacamp.utils import wait_until
from market.idx.yatf.utils.utils import create_timestamp_from_json
from market.pylibrary.proto_utils import message_from_data


NOW_UTC = datetime.utcnow()  # JSON serializer should always use UTC
time_pattern = "%Y-%m-%dT%H:%M:%SZ"
current_time = NOW_UTC.strftime(time_pattern)
current_ts = create_timestamp_from_json(current_time)

FF_WAREHOUSE = 145
NOT_FF_WAREHOUSE = 100


@pytest.fixture(scope='function', params=[True, False])
def ff_warehouse(request):
    return request.param


DATACAMP_OFFERS = [{
    'identifiers': {
        'business_id': 111,
        'shop_id': 222,
        'feed_id': 333,
        'offer_id': '444'
    },
    'status': {
        'disabled': [
            {
                'flag': False,
                'meta': {
                    'timestamp': current_time,
                    'source': DTC.PUSH_PARTNER_FEED,
                },
            },
        ],
    },
    'meta': {
        'rgb': DTC.BLUE,
        'ts_created': current_time
    },
    'price': {
        'basic': {
            'binary_price': {
                'price': 20
            },
            'meta': {
                'timestamp': current_time,
                'source': DTC.PUSH_PARTNER_FEED
            }
        }
    },
    'stock_info': {
        'partner_stocks': {
            'count': 10,
            'meta': {
                'timestamp': current_time,
                'source': DTC.PUSH_PARTNER_FEED
            }
        },
        'partner_stocks_default': {
            'count': 20,
            'meta': {
                'timestamp': current_time,
                'source': DTC.PUSH_PARTNER_FEED
            }
        },
    },
    'content': {
        'partner': {
            'original': {
                'direct_category': {
                    'name': 'category',
                    'path_category_names': 'root\\category',
                    'parent_id': 0,
                    'path_category_ids': '0\\2',
                    'id': 2
                }
            }
        }
    }
}]


def create_identifiers(ff_warehouse, identifiers, scope, table=False):
    '''Для fullfillment-складов затирается warehouse_id в сервисном оффере'''
    identifiers = identifiers.copy()  # идентификаторы без warehouse_id
    warehouse_id = FF_WAREHOUSE if ff_warehouse else NOT_FF_WAREHOUSE

    if scope == DTC.BASIC:
        identifiers.pop('shop_id')
    elif scope == DTC.SERVICE or scope == DTC.STOCK:
        if not ff_warehouse:
            identifiers['warehouse_id'] = warehouse_id
    else:
        identifiers.pop('business_id')

    if table:
        identifiers['shop_sku'] = identifiers.pop('offer_id')
        identifiers.pop('feed_id')
        if scope == DTC.SERVICE:
            identifiers['warehouse_id'] = 0 if ff_warehouse else warehouse_id

    return identifiers


def make_united(ff_warehouse, offer):
    ''' Для НЕ fullfillment-склада верно:
        - стоки пишутся в сервисный оффер;
        - проставляется warehouse_id для сервисного оффера.
    '''
    def set_meta_and_identifiers(ff_warehouse, offre, scope):
        res_offer = {'identifiers': create_identifiers(ff_warehouse, offer['identifiers'], scope)}
        res_offer['meta'] = offer['meta'].copy()
        res_offer['meta']['scope'] = scope
        res_offer['status'] = offer['status']
        return res_offer

    basic = set_meta_and_identifiers(ff_warehouse, offer, DTC.BASIC)

    service = set_meta_and_identifiers(ff_warehouse, offer, DTC.SERVICE)
    service['price'] = offer['price']
    service['content'] = offer['content']
    if not ff_warehouse:
        service['stock_info'] = offer['stock_info']

    united = {
        'basic': basic,
        'service': {
            offer['identifiers']['shop_id']: service
        }
    }

    return united


@pytest.fixture()
def expected_basic_offers():
    expected_offers = []
    for offer in DATACAMP_OFFERS:
        expected = message_from_data({
            'identifiers': {
                'offer_id': offer['identifiers']['offer_id'],
                'business_id': offer['identifiers']['business_id'],
            },
        }, DTC.Offer())
        expected_offers.append(expected)
    return expected_offers


@pytest.fixture(scope='function')
def expected_service_offers(ff_warehouse):
    expected_offers = []
    for offer in DATACAMP_OFFERS:
        expected = message_from_data({
            'identifiers': {
                'offer_id': offer['identifiers']['offer_id'],
                'business_id': offer['identifiers']['business_id'],
                'shop_id': offer['identifiers']['shop_id'],
            },
            'price': {
                'basic': {
                    'binary_price': {
                        'price': offer['price']['basic']['binary_price']['price']
                    },
                    'meta': {
                        'timestamp': current_time,
                        'source': DTC.PUSH_PARTNER_FEED
                    }
                },
                'enable_auto_discounts': {
                    'flag': True,
                },
            },
            'stock_info': None if ff_warehouse else {
                'partner_stocks_default': {
                    'count': offer['stock_info']['partner_stocks_default']['count'],
                    'meta': {
                        'timestamp': current_time,
                        'source': DTC.PUSH_PARTNER_FEED
                    }
                }
            }
        }, DTC.Offer())
        expected_offers.append(expected)
    return expected_offers


@pytest.fixture(scope='function')
def expected_actual_service_offers(ff_warehouse):
    if ff_warehouse:
        return []

    expected_offers = []
    for offer in DATACAMP_OFFERS:
        expected = message_from_data({
            'identifiers': {
                'offer_id': offer['identifiers']['offer_id'],
                'business_id': offer['identifiers']['business_id'],
                'shop_id': offer['identifiers']['shop_id'],
                'warehouse_id': NOT_FF_WAREHOUSE,
            },
            'meta': {
                'scope': DTC.SERVICE,
                'ts_created': current_time
            },
            'stock_info': {
                'partner_stocks': {
                    'count': offer['stock_info']['partner_stocks']['count'],
                    'meta': {
                        'timestamp': current_time,
                        'source': DTC.PUSH_PARTNER_FEED
                    }
                }
            }
        }, DTC.Offer())
        expected_offers.append(expected)
    return expected_offers


def change_offer_id_alias(identifiers):
    identifiers = identifiers.copy()
    identifiers['offer_id'] = identifiers.pop('shop_sku')
    return identifiers


@pytest.fixture(scope='function')
def qoffers_topic(log_broker_stuff):
    topic = LbkTopic(log_broker_stuff)
    return topic


@pytest.fixture(scope='function')
def united_miner_topic(log_broker_stuff):
    topic = LbkTopic(log_broker_stuff)
    return topic


@pytest.fixture(scope='function')
def config(yt_server, log_broker_stuff, qoffers_topic, united_miner_topic):
    cfg = {
        'general': {
            'color': 'white',
        },
        'logbroker': {
            'qoffers_topic': qoffers_topic.topic,
        },
        'miner': {
            'united_topic': united_miner_topic.topic,
        }
    }
    return PiperConfigMock(yt_server=yt_server, log_broker_stuff=log_broker_stuff, config=cfg)


@pytest.yield_fixture(scope='function')
def piper(yt_server, log_broker_stuff, config, qoffers_topic, united_miner_topic):
    resources = {
        'config': config,
        'qoffers_topic': qoffers_topic,
        'united_miner_topic': united_miner_topic,
    }
    with PiperTestEnv(yt_server, log_broker_stuff, **resources) as piper_env:
        piper_env.verify()
        yield piper_env


def test_qoffers_to_white_piper(
    ff_warehouse,
    piper,
    qoffers_topic,
    expected_basic_offers,
    expected_service_offers,
    expected_actual_service_offers,
    united_miner_topic,
):
    """
    Проверяем, что белый piper читает оффера из топика qparser-а и корректно записывает их в таблицы:
    для ff-складов:
      - создает оффер в BasicOffers;
      - создает оффер без цены в BasicOffers (цена содержит enable_auto_discounts);
      - создает оффер в ServiceOffers;
    для НЕ ff-скаладов дополнительно:
      - записывает стоки в сервисный оффер
      - создает оффер в ActualServiceOffers;

    Оффера берутся из поля united_offers, поле offers игнорируется
    """
    for offer in DATACAMP_OFFERS:
        message = message_from_data({
            'offers': [{'offer': [offer]}],
            'united_offers': [{'offer': [make_united(ff_warehouse, offer)]}],
        }, DatacampMessage())
        qoffers_topic.write(message.SerializeToString())

    wait_until(lambda: piper.united_offers_processed >= 1)

    assert_that(len(piper.basic_offers_table.data), equal_to(len(expected_basic_offers)))
    assert_that(piper.basic_offers_table.data, HasOffers(expected_basic_offers))

    assert_that(len(piper.service_offers_table.data), equal_to(len(expected_service_offers)))
    assert_that(piper.service_offers_table.data, HasOffers(expected_service_offers))

    assert_that(len(piper.actual_service_offers_table.data), equal_to(len(expected_actual_service_offers)))
    assert_that(piper.actual_service_offers_table.data, HasOffers(expected_actual_service_offers))

    data = united_miner_topic.read(count=1)

    assert_that(data, has_items(IsSerializedProtobuf(DatacampMessage, {
        'united_offers': [{
            'offer': [
                {
                    'basic': {
                        'identifiers': {
                            'business_id': 111,
                            'offer_id': '444',
                        },
                        'content': {
                            'partner': None
                        }
                    },
                    'service': IsProtobufMap({
                        222: {
                            'identifiers': {
                                'shop_id': 222,
                                'business_id': 111,
                                'offer_id': '444',
                            },
                            'content': {
                                'partner': {
                                    'original': {
                                        'direct_category': {
                                            'name': 'category',
                                            'path_category_names': 'root\\category',
                                            'parent_id': 0,
                                            'path_category_ids': '0\\2',
                                            'id': 2
                                        }
                                    }
                                }
                            }
                        }
                    }),
                    'actual': empty()
                }
            ]
        }]
    })))

    # Проверяем, что в топике больше нет данных
    assert_that(united_miner_topic, HasNoUnreadData())
