# coding: utf-8

import base64
import pytest
import six
from datetime import datetime, timedelta
from hamcrest import assert_that, has_items, not_

import market.idx.datacamp.proto.offer.DataCampOffer_pb2 as DTC
from market.idx.datacamp.controllers.piper.yatf.resources.config_mock import PiperConfigMock
from market.idx.datacamp.controllers.piper.yatf.test_env_old import PiperTestEnv
from market.idx.datacamp.proto.api.DatacampMessage_pb2 import DatacampMessage
from market.idx.pylibrary.datacamp.utils import wait_until
from market.idx.datacamp.yatf.utils import dict2tskv
from market.idx.yatf.matchers.protobuf_matchers import IsSerializedProtobuf, IsProtobufMap
from market.idx.yatf.matchers.yt_rows_matchers import HasOffers
from market.idx.yatf.resources.datacamp.datacamp_tables import DataCampPartnersTable
from market.idx.yatf.resources.lbk_topic import LbkTopic
from market.idx.yatf.utils.utils import create_timestamp_from_json
from market.pylibrary.proto_utils import message_from_data


NOW_UTC = datetime.utcnow()  # JSON serializer should always use UTC
PAST_UTC = NOW_UTC - timedelta(minutes=45)
FUTURE_UTC = NOW_UTC + timedelta(minutes=45)
time_pattern = "%Y-%m-%dT%H:%M:%SZ"

current_time = NOW_UTC.strftime(time_pattern)
current_ts = create_timestamp_from_json(current_time)

past_time = PAST_UTC.strftime(time_pattern)
past_ts = create_timestamp_from_json(past_time)

future_time = FUTURE_UTC.strftime(time_pattern)
future_ts = create_timestamp_from_json(future_time)

future_time_2 = (FUTURE_UTC + timedelta(minutes=45)).strftime(time_pattern)
future_ts_2 = create_timestamp_from_json(future_time_2)

BY_NEW = six.ensure_str(base64.b64encode(six.ensure_binary('by new')))
BY_NULL = six.ensure_str(base64.b64encode(six.ensure_binary('by null')))
BY_OLD = six.ensure_str(base64.b64encode(six.ensure_binary('by old')))

PARTNERS = [
    {
        'shop_id': 1,
        'status': 'publish',
        'mbi': dict2tskv({
            'shop_id': 1,
            'business_id': 1,
            'datafeed_id': 4321,
            'warehouse_id': 145,
            'is_push_partner': True,
        })
    },
    {
        'shop_id': 1,
        'status': 'publish',
        'mbi': dict2tskv({
            'shop_id': 1,
            'business_id': 1,
            'datafeed_id': 1234,
            'warehouse_id': 111,
            'is_push_partner': True,
        })
    },
]

OFFERS = [
    {
        'identifiers': {
            'shop_id': 1,
            'offer_id': 'TestBidsOriginalUpdate',
            'warehouse_id': 42,
            'business_id': 1,
        },
        'meta': {
            'ts_created': NOW_UTC.strftime(time_pattern),
        },
        'price': {
            'basic': {
                'meta': {
                    'timestamp': past_time,
                    'source': DTC.PUSH_PARTNER_FEED,
                },
            },
        },
    },
    {
        'identifiers': {
            'shop_id': 1,
            'offer_id': 'TestBidsActualUpdate',
            'warehouse_id': 42,
            'business_id': 1,
        },
        'meta': {
            'ts_created': NOW_UTC.strftime(time_pattern),
        },
        'price': {
            'basic': {
                'meta': {
                    'timestamp': past_time,
                    'source': DTC.PUSH_PARTNER_FEED,
                },
            },
        },
    },
    {
        'identifiers': {
            'shop_id': 1,
            'offer_id': 'TestBidsFeeUpdate',
            'warehouse_id': 42,
            'business_id': 1,
        },
        'price': {
            'basic': {
                'meta': {
                    'timestamp': past_time,
                    'source': DTC.PUSH_PARTNER_FEED,
                },
            },
        },
    },
    {
        'identifiers': {
            'shop_id': 1,
            'offer_id': 'TestBidsFlagDontPullUpBidsUpdate',
            'warehouse_id': 42,
            'business_id': 1,
        },
        'price': {
            'basic': {
                'meta': {
                    'timestamp': past_time,
                    'source': DTC.PUSH_PARTNER_FEED,
                },
            },
        },
    },
    {
        'identifiers': {
            'shop_id': 1,
            'offer_id': 'TestBidsAmoreDataUpdate',
            'warehouse_id': 42,
            'business_id': 1,
        },
        'price': {
            'basic': {
                'meta': {
                    'timestamp': past_time,
                    'source': DTC.PUSH_PARTNER_FEED,
                },
            },
        },
    },
    {
        'identifiers': {
            'shop_id': 1,
            'offer_id': 'TestBidsAmoreBeruSupplierDataUpdate',
            'warehouse_id': 42,
            'business_id': 1,
        },
        'price': {
            'basic': {
                'meta': {
                    'timestamp': past_time,
                    'source': DTC.PUSH_PARTNER_FEED,
                },
            },
        },
    },
    {
        'identifiers': {
            'shop_id': 1,
            'offer_id': 'TestBidsAmoreBeruVendorDataUpdate',
            'warehouse_id': 42,
            'business_id': 1,
        },
        'price': {
            'basic': {
                'meta': {
                    'timestamp': past_time,
                    'source': DTC.PUSH_PARTNER_FEED,
                },
            },
        },
    },
]


@pytest.fixture(scope='module')
def offers():
    return [message_from_data(offer, DTC.Offer()) for offer in OFFERS]


@pytest.fixture(
    scope='module',
    params=[
        # bids - bid
        {
            'offer_id': 'TestBidsOriginalUpdate',
            'parent_field_name': 'bids',
            'field_name': 'bid',
            'update_null_data': {
                'value': 10,
                'meta': {
                    'timestamp': current_time,
                },
            },
            'update_by_old_data': {
                'value': 20,
                'meta': {
                    'timestamp': past_time,
                },
            },
            'update_by_new_data': {
                'value': 30,
                'meta': {
                    'timestamp': future_time,
                },
            },
            'expected_after_update_null': message_from_data({
                'identifiers': {
                    'shop_id': 1,
                    'offer_id': 'TestBidsOriginalUpdate',
                },
                'bids': {
                    'bid': {
                        'meta': {
                            'timestamp': current_time
                        },
                        'value': 10
                    }
                },
            }, DTC.Offer()),
            'expected_price_after_update_null': message_from_data({
                'identifiers': {
                    'shop_id': 1,
                    'offer_id': 'TestBidsOriginalUpdate',
                },
                'price': {
                    'basic': {
                        'meta': {
                            'timestamp': past_time
                        },
                    },
                },
            }, DTC.Offer()),
            'expected_after_update_by_new': message_from_data({
                'identifiers': {
                    'shop_id': 1,
                    'offer_id': 'TestBidsOriginalUpdate',
                },
                'bids': {
                    'bid': {
                        'meta': {
                            'timestamp': future_time,
                        },
                        'value': 30
                    }
                },
            }, DTC.Offer()),
        },
        # bids actual
        {
            'offer_id': 'TestBidsActualUpdate',
            'parent_field_name': 'bids',
            'field_name': 'bid_actual',
            'update_null_data': {
                'value': 10,
                'meta': {
                    'timestamp': current_time,
                },
            },
            'update_by_old_data': {
                'value': 20,
                'meta': {
                    'timestamp': past_time,
                },
            },
            'update_by_new_data': {
                'value': 30,
                'meta': {
                    'timestamp': future_time,
                },
            },
            'expected_after_update_null': message_from_data({
                'identifiers': {
                    'shop_id': 1,
                    'offer_id': 'TestBidsActualUpdate',
                },
                'bids': {
                    'bid_actual': {
                        'meta': {
                            'timestamp': current_time
                        },
                        'value': 10
                    }
                },
            }, DTC.Offer()),
            'expected_price_after_update_null': message_from_data({
                'identifiers': {
                    'shop_id': 1,
                    'offer_id': 'TestBidsActualUpdate',
                },
                'price': {
                    'basic': {
                        'meta': {
                            'timestamp': past_time
                        },
                    },
                },
            }, DTC.Offer()),
            'expected_after_update_by_new': message_from_data({
                'identifiers': {
                    'shop_id': 1,
                    'offer_id': 'TestBidsActualUpdate',
                },
                'bids': {
                    'bid_actual': {
                        'meta': {
                            'timestamp': future_time
                        },
                        'value': 30
                    }
                },
            }, DTC.Offer()),
        },
        # bids - fee
        {
            'offer_id': 'TestBidsFeeUpdate',
            'parent_field_name': 'bids',
            'field_name': 'fee',
            'update_null_data': {
                'value': 10,
                'meta': {
                    'timestamp': current_time,
                },
            },
            'update_by_old_data': {
                'value': 20,
                'meta': {
                    'timestamp': past_time,
                },
            },
            'update_by_new_data': {
                'value': 30,
                'meta': {
                    'timestamp': future_time,
                },
            },
            'expected_after_update_null': message_from_data({
                'identifiers': {
                    'shop_id': 1,
                    'offer_id': 'TestBidsFeeUpdate',
                },
                'bids': {
                    'fee': {
                        'meta': {
                            'timestamp': current_time,
                        },
                        'value': 10
                    }
                },
            }, DTC.Offer()),
            'expected_price_after_update_null': message_from_data({
                'identifiers': {
                    'shop_id': 1,
                    'offer_id': 'TestBidsFeeUpdate',
                },
                'price': {
                    'basic': {
                        'meta': {
                            'timestamp': past_time
                        },
                    },
                },
            }, DTC.Offer()),
            'expected_after_update_by_new': message_from_data({
                'identifiers': {
                    'shop_id': 1,
                    'offer_id': 'TestBidsFeeUpdate',
                },
                'bids': {
                    'fee': {
                        'meta': {
                            'timestamp': future_time,
                        },
                        'value': 30
                    }
                },
            }, DTC.Offer()),
        },
        # bids - flag_dont_pull_up_bids
        {
            'offer_id': 'TestBidsFlagDontPullUpBidsUpdate',
            'parent_field_name': 'bids',
            'field_name': 'flag_dont_pull_up_bids',
            'update_null_data': {
                'flag': True,
                'meta': {
                    'timestamp': current_time,
                },
            },
            'update_by_old_data': {
                'flag': False,
                'meta': {
                    'timestamp': past_time,
                },
            },
            'update_by_new_data': {
                'flag': False,
                'meta': {
                    'timestamp': future_time,
                },
            },
            'expected_after_update_null': message_from_data({
                'identifiers': {
                    'shop_id': 1,
                    'offer_id': 'TestBidsFlagDontPullUpBidsUpdate',
                },
                'bids': {
                    'flag_dont_pull_up_bids': {
                        'meta': {
                            'timestamp': current_time
                        },
                        'flag': True
                    }
                }
            }, DTC.Offer()),
            'expected_price_after_update_null': message_from_data({
                'identifiers': {
                    'shop_id': 1,
                    'offer_id': 'TestBidsFlagDontPullUpBidsUpdate',
                },
                'price': {
                    'basic': {
                        'meta': {
                            'timestamp': past_time
                        },
                    },
                },
            }, DTC.Offer()),
            'expected_after_update_by_new': message_from_data({
                'identifiers': {
                    'shop_id': 1,
                    'offer_id': 'TestBidsFlagDontPullUpBidsUpdate',
                },
                'bids': {
                    'flag_dont_pull_up_bids': {
                        'meta': {
                            'timestamp': future_time
                        },
                        'flag': False
                    }
                },
            }, DTC.Offer()),
        },
        # bids - amore_data
        {
            'offer_id': 'TestBidsAmoreDataUpdate',
            'parent_field_name': 'bids',
            'field_name': 'amore_data',
            'update_null_data': {
                'value': BY_NULL,
                'meta': {
                    'timestamp': current_time,
                },
            },
            'update_by_old_data': {
                'value': BY_OLD,
                'meta': {
                    'timestamp': past_time,
                },
            },
            'update_by_new_data': {
                'value': BY_NEW,
                'meta': {
                    'timestamp': future_time,
                },
            },
            'expected_after_update_null': message_from_data({
                'identifiers': {
                    'shop_id': 1,
                    'offer_id': 'TestBidsAmoreDataUpdate',
                },
                'bids': {
                    'amore_data': {
                        'meta': {
                            'timestamp': current_time
                        },
                        'value': BY_NULL,
                    }
                },
            }, DTC.Offer()),
            'expected_price_after_update_null': message_from_data({
                'identifiers': {
                    'shop_id': 1,
                    'offer_id': 'TestBidsAmoreDataUpdate',
                },
                'price': {
                    'basic': {
                        'meta': {
                            'timestamp': past_time
                        },
                    },
                },
            }, DTC.Offer()),
            'expected_after_update_by_new': message_from_data({
                'identifiers': {
                    'shop_id': 1,
                    'offer_id': 'TestBidsAmoreDataUpdate',
                },
                'bids': {
                    'amore_data': {
                        'meta': {
                            'timestamp': future_time
                        },
                        'value': BY_NEW,
                    }
                },
            }, DTC.Offer()),
        },
        # bids - amore_beru_supplier_data
        {
            'offer_id': 'TestBidsAmoreBeruSupplierDataUpdate',
            'parent_field_name': 'bids',
            'field_name': 'amore_beru_supplier_data',
            'update_null_data': {
                'value': BY_NULL,
                'meta': {
                    'timestamp': current_time,
                },
            },
            'update_by_old_data': {
                'value': BY_OLD,
                'meta': {
                    'timestamp': past_time,
                },
            },
            'update_by_new_data': {
                'value': BY_NEW,
                'meta': {
                    'timestamp': future_time,
                },
            },
            'expected_after_update_null': message_from_data({
                'identifiers': {
                    'shop_id': 1,
                    'offer_id': 'TestBidsAmoreBeruSupplierDataUpdate',
                },
                'bids': {
                    'amore_beru_supplier_data': {
                        'meta': {
                            'timestamp': current_time
                        },
                        'value': BY_NULL,
                    }
                }
            }, DTC.Offer()),
            'expected_price_after_update_null': message_from_data({
                'identifiers': {
                    'shop_id': 1,
                    'offer_id': 'TestBidsAmoreBeruSupplierDataUpdate',
                },
                'price': {
                    'basic': {
                        'meta': {
                            'timestamp': past_time
                        },
                    },
                },
            }, DTC.Offer()),
            'expected_after_update_by_new': message_from_data({
                'identifiers': {
                    'shop_id': 1,
                    'offer_id': 'TestBidsAmoreBeruSupplierDataUpdate',
                },
                'bids': {
                    'amore_beru_supplier_data': {
                        'meta': {
                            'timestamp': future_time,
                        },
                        'value': BY_NEW,
                    }
                },
            }, DTC.Offer()),
        },
        # bids - amore_beru_vendor_data
        {
            'offer_id': 'TestBidsAmoreBeruVendorDataUpdate',
            'parent_field_name': 'bids',
            'field_name': 'amore_beru_vendor_data',
            'update_null_data': {
                'value': BY_NULL,
                'meta': {
                    'timestamp': current_time,
                },
            },
            'update_by_old_data': {
                'value': BY_OLD,
                'meta': {
                    'timestamp': past_time,
                },
            },
            'update_by_new_data': {
                'value': BY_NEW,
                'meta': {
                    'timestamp': future_time,
                },
            },
            'expected_after_update_null': message_from_data({
                'identifiers': {
                    'shop_id': 1,
                    'offer_id': 'TestBidsAmoreBeruVendorDataUpdate',
                },
                'bids': {
                    'amore_beru_vendor_data': {
                        'meta': {
                            'timestamp': current_time
                        },
                        'value': BY_NULL,
                    }
                },
            }, DTC.Offer()),
            'expected_price_after_update_null': message_from_data({
                'identifiers': {
                    'shop_id': 1,
                    'offer_id': 'TestBidsAmoreBeruVendorDataUpdate',
                },
                'price': {
                    'basic': {
                        'meta': {
                            'timestamp': past_time
                        },
                    },
                },
            }, DTC.Offer()),
            'expected_after_update_by_new': message_from_data({
                'identifiers': {
                    'shop_id': 1,
                    'offer_id': 'TestBidsAmoreBeruVendorDataUpdate',
                },
                'bids': {
                    'amore_beru_vendor_data': {
                        'meta': {
                            'timestamp': future_time,
                        },
                        'value': BY_NEW,
                    }
                }
            }, DTC.Offer()),
        },
    ],
    ids=[
        'test_bids_original_update',
        'test_bids_actual_update',
        'test_bids_fee_update',
        'test_bids_flag_dont_pull_up_bids_update',
        'test_bids_amore_data_update',
        'test_bids_amore_beru_supplier_data_update',
        'test_bids_amore_beru_vendor_data_update',
    ]
)
def gen_data(request):
    return request.param


@pytest.fixture(scope='module')
def partners_table(yt_server, config):
    return DataCampPartnersTable(yt_server, config.yt_partners_tablepath, PARTNERS)


@pytest.fixture(scope='module')
def offers_topic(log_broker_stuff):
    topic = LbkTopic(log_broker_stuff, 'offers')
    return topic


@pytest.fixture(scope='module')
def offer_bids_topic(log_broker_stuff):
    topic = LbkTopic(log_broker_stuff, 'offer_bids')
    return topic


@pytest.fixture(scope='module')
def qoffers_topic(log_broker_stuff):
    topic = LbkTopic(log_broker_stuff, 'qoffers')
    return topic


@pytest.fixture(scope='module')
def config(yt_server, log_broker_stuff, offers_topic, offer_bids_topic, qoffers_topic):
    cfg = {
        'logbroker': {
            'offers_topic': offers_topic.topic,
            'offer_bids_topic': offer_bids_topic.topic,
            'qoffers_topic': qoffers_topic.topic
        },
        'general': {
            'color': 'blue',
        }
    }
    return PiperConfigMock(yt_server=yt_server,
                           log_broker_stuff=log_broker_stuff,
                           config=cfg)


@pytest.yield_fixture(scope='module')
def piper(yt_server, log_broker_stuff, config, offers_topic, offer_bids_topic, qoffers_topic, partners_table):
    resources = {
        'config': config,
        'offers_topic': offers_topic,
        'offer_bids_topic': offer_bids_topic,
        'qoffers_topic': qoffers_topic,
        'partners_table': partners_table,
    }
    with PiperTestEnv(yt_server, log_broker_stuff, **resources) as piper_env:
        piper_env.verify()
        yield piper_env


@pytest.fixture(scope='module')
def inserter(offers, piper, offers_topic):
    for offer in offers:
        offers_topic.write(offer.SerializeToString())

    wait_until(lambda: piper.united_offers_processed >= len(offers))


def create_update_part(shop_id, offer_id, warehouse_id, case, gen_data):
    identifiers = {
        'shop_id': shop_id,
        'offer_id': offer_id,
        'warehouse_id': warehouse_id,
    }

    if gen_data['parent_field_name']:
        update_part = {
            'identifiers': identifiers,
            gen_data['parent_field_name']: {
                gen_data['field_name']: gen_data[case],
            },
        }
    else:
        update_part = {
            'identifiers': identifiers,
            gen_data['field_name']: gen_data[case]
        }

    return update_part


def test_offer_partly_update(inserter, offers, piper, offers_topic, gen_data):
    shop_id = 1
    offer_id = gen_data['offer_id']
    actual = gen_data['field_name'] == 'bid_actual'

    # Шаг 1 : запишем данные для офера, в котором отсутствует обновляемое поле
    update_part = create_update_part(shop_id, offer_id, 42, 'update_null_data', gen_data)

    offers_processed = piper.united_offers_processed
    offers_topic.write(message_from_data(update_part, DTC.Offer()).SerializeToString())
    wait_until(lambda: piper.united_offers_processed >= offers_processed + 1)

    # Шаг 2 : проверим, что изменение появилось в хранилище
    assert_that(piper.actual_service_offers_table.data if actual else piper.service_offers_table.data, HasOffers([
        gen_data['expected_after_update_null']
    ]))

    # Шаг 3 : проверим, что другие данные не изменились
    assert_that(piper.service_offers_table.data, HasOffers([
        gen_data['expected_price_after_update_null']
    ]))

    # Шаг 4 : попробуем записать в хранилище старые данные
    update_part = create_update_part(shop_id, offer_id, 42, 'update_by_old_data', gen_data)

    offers_topic.write(message_from_data(update_part, DTC.Offer()).SerializeToString())
    wait_until(lambda: piper.united_offers_processed >= offers_processed + 2)

    # Шаг 5 : проверим, что изменение НЕ появилось в хранилище
    assert_that(piper.actual_service_offers_table.data if actual else piper.service_offers_table.data, HasOffers([
        gen_data['expected_after_update_null']
    ]))

    # Шаг 6 : попробуем записать в хранилище новые данные
    update_part = create_update_part(shop_id, offer_id, 42, 'update_by_new_data', gen_data)

    offers_topic.write(message_from_data(update_part, DTC.Offer()).SerializeToString())
    wait_until(lambda: piper.united_offers_processed >= offers_processed + 3)

    # Шаг 7 : проверим, что изменение появилось в хранилище
    assert_that(piper.actual_service_offers_table.data if actual else piper.service_offers_table.data, HasOffers([
        gen_data['expected_after_update_by_new']
    ]))


def test_offer_bids_update(inserter, offers, piper, offers_topic, offer_bids_topic, qoffers_topic):
    shop_id = 1
    feed_id = 1234
    offer_id = 'TestQuickBidsUpdate'
    bid_data = {
        'identifiers': {
            'shop_id': shop_id,
            'feed_id': feed_id,
            'offer_id': offer_id,
        },
        'bids': {
            'bid': {
                'meta': {
                    'timestamp': future_time
                },
                'value': 10
            }
        },
        'meta': {
            'rgb': DTC.BLUE,
            'modification_ts': future_ts.seconds,
        }
    }

    # Шаг 1: применяем ставку первый раз, к офферу которого нет в таблице, смотрим что оффер НЕ создался
    offers_processed = piper.united_offers_processed
    offer_bids_topic.write(message_from_data({'offer': [bid_data]}, DTC.OffersBatch()).SerializeToString())
    wait_until(lambda: piper.united_offers_processed >= offers_processed + 1)

    assert_that(piper.service_offers_table.data, not_(HasOffers([
        message_from_data({
            'identifiers': {
                'shop_id': 1,
                'offer_id': 'TestQuickBidsUpdate',
                'extra': {
                    'shop_sku': 'TestQuickBidsUpdate',
                },
            },
            'bids': {
                'bid': {
                    'meta': {
                        'timestamp': future_time
                    },
                    'value': 10
                }
            },
        }, DTC.Offer())
    ])))

    # Шаг 2: создадим офер, через другой топик
    offers_processed = piper.united_offers_processed
    offers_topic.write(message_from_data(bid_data, DTC.Offer()).SerializeToString())
    wait_until(lambda: piper.united_offers_processed >= offers_processed + 1)

    # тут сообщение ставки все равно отправляется в топик qoffers(мы еще не знаем новый ли это оффер)
    qoffers_topic.read(count=1)

    # Шаг 3: меняем ставку у существующего оффера, смотрим что она изменилась и цвет не изменился
    bid_data['bids']['bid']['value'] = 15
    bid_data['bids']['bid']['meta']['timestamp'] = future_time_2
    bid_data['meta']['rgb'] = DTC.WHITE
    offers_processed = piper.united_offers_processed
    offer_bids_topic.write(message_from_data({'offer': [bid_data]}, DTC.OffersBatch()).SerializeToString())
    wait_until(lambda: piper.united_offers_processed >= offers_processed + 1)

    assert_that(piper.service_offers_table.data, HasOffers([
        message_from_data({
            'identifiers': {
                'shop_id': 1,
                'offer_id': 'TestQuickBidsUpdate',
                'extra': {
                    'shop_sku': 'TestQuickBidsUpdate',
                },
            },
            'bids': {
                'bid': {
                    'meta': {
                        'timestamp': future_time_2
                    },
                    'value': 15
                },
            },
            'meta': {
                'rgb': DTC.BLUE
            }
        }, DTC.Offer())
    ]))
    # Проверяем, что переотправили запись в топик qoffers
    assert_that(qoffers_topic.read(count=1), has_items(*[
        IsSerializedProtobuf(DatacampMessage, {
            'united_offers': [{
                'offer': [
                    {
                        'basic': {
                            'identifiers': {
                                'business_id': 1,
                                'offer_id': 'TestQuickBidsUpdate'
                            }
                        },
                        'service': IsProtobufMap({
                            1: {
                                'identifiers': {
                                    'business_id': 1,
                                    'offer_id': 'TestQuickBidsUpdate',
                                    'shop_id': 1,
                                },
                                'bids': {
                                    'bid': {
                                        'value': 15
                                    }
                                }
                            }
                        })
                    }
                ]
            }]
        })
    ]))
