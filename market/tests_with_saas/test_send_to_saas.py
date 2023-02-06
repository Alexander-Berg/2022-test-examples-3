# coding: utf-8

import pytest
import yatest
import os
from hamcrest import assert_that, has_item, equal_to, is_not

import market.idx.datacamp.proto.offer.DataCampOffer_pb2 as DTC
import market.idx.datacamp.proto.subscription.subscription_pb2 as SUBS

from market.idx.datacamp.dispatcher.yatf.resources.config import DispatcherConfig
from market.idx.datacamp.dispatcher.yatf.test_env import DispatcherTestEnv
from market.idx.pylibrary.datacamp.utils import wait_until
from market.idx.yatf.test_envs.saas_env import SaasEnv
from market.pylibrary.proto_utils import message_from_data, proto_path_from_str_path


BUSINESS_ID = 1
SHOP_ID = 10
WAREHOUSE_ID = 100


@pytest.fixture(scope='module')
def basic_offers_table_data():
    return [
        message_from_data({
            'identifiers': {
                'business_id': BUSINESS_ID,
                'offer_id': 'offer.with.vendor'
            },
            'content': {
                'partner': {
                    'original': {
                        'vendor': {
                            'value': 'vendor value',
                        }
                    }
                }
            },
        }, DTC.Offer()),
        message_from_data({
            'identifiers': {
                'business_id': BUSINESS_ID,
                'offer_id': 'offer.with.long.vendor'
            },
            'content': {
                'partner': {
                    'original': {
                        'vendor': {
                            'value': 'x' * 300,
                        }
                    }
                }
            },
        }, DTC.Offer()),
        message_from_data({
            'identifiers': {
                'business_id': BUSINESS_ID,
                'offer_id': 'offer.with.integral.status.update'
            },
            'content': {
                'partner': {
                    'original': {
                        'vendor': {
                            'value': 'vendor value',
                        }
                    }
                }
            },
        }, DTC.Offer()),
    ]


@pytest.fixture(scope='module')
def service_offers_table_data():
    return [
        message_from_data({
            'identifiers': {
                'business_id': BUSINESS_ID,
                'offer_id': 'offer.with.integral.status.update',
                'shop_id': SHOP_ID,
            },
            'meta': {
                'scope': DTC.SERVICE,
                'rgb': DTC.BLUE
            },
        }, DTC.Offer()),
    ]


@pytest.fixture(scope='module')
def actual_service_offers_table_data():
    return [
        message_from_data({
            'identifiers': {
                'business_id': BUSINESS_ID,
                'offer_id': 'offer.with.integral.status.update',
                'shop_id': SHOP_ID,
                'warehouse_id': WAREHOUSE_ID,
            },
            'meta': {
                'scope': DTC.SERVICE,
                'rgb': DTC.BLUE
            },
        }, DTC.Offer()),
    ]


SUBSCRIPTION_MESSAGES = [
    message_from_data({
        'subscription_request': [
            {
                'business_id': BUSINESS_ID,
                'offer_id': 'offer.with.vendor',
                'basic': {
                    'updated_fields': [
                        proto_path_from_str_path(DTC.Offer, 'content.partner.original.vendor')
                    ],
                }
            }
        ]
    }, SUBS.UnitedOffersSubscriptionMessage()),
    message_from_data({
        'subscription_request': [
            {
                'business_id': BUSINESS_ID,
                'offer_id': 'offer.with.long.vendor',
                'basic': {
                    'updated_fields': [
                        proto_path_from_str_path(DTC.Offer, 'content.partner.original.vendor')
                    ],
                }
            }
        ]
    }, SUBS.UnitedOffersSubscriptionMessage()),
    message_from_data({
        'subscription_request': [
            {
                'business_id': BUSINESS_ID,
                'offer_id': 'offer.with.integral.status.update',
                'basic': {
                    'warehouse_price_updated': False
                },
                'basic_changed_for_integral_status': False,
                'service': {
                    SHOP_ID: {
                        'status_update': {
                            'new_status': 1,
                            'old_status': 2,
                        },
                        'warehouse': {
                            WAREHOUSE_ID: {
                                'warehouse_price_updated': False,
                            }
                        }
                    }
                },
            }
        ]
    }, SUBS.UnitedOffersSubscriptionMessage()),
]


@pytest.fixture(scope='module')
def saas():
    rel_path = os.path.join('market', 'idx', 'yatf', 'resources', 'saas', 'stubs', 'market-idxapi')
    with SaasEnv(saas_service_configs=yatest.common.source_path(rel_path), prefixed=True) as saas:
        yield saas


@pytest.fixture(scope='module')
def dispatcher_config(
    yt_token,
    yt_server,
    log_broker_stuff,
    input_topic,
    output_topic,
    basic_offers_table,
    service_offers_table,
    actual_service_offers_table,
    saas
):
    cfg = DispatcherConfig()
    cfg.create_initializer(
        yt_server=yt_server,
        yt_token_path=yt_token.path
    )

    lb_reader = cfg.create_lb_reader(log_broker_stuff, input_topic)
    unpacker = cfg.create_subscription_message_unpacker()
    dispatcher = cfg.create_subscription_dispatcher(
        basic_offers_table.table_path,
        service_offers_table.table_path,
        actual_service_offers_table.table_path
    )
    filter = cfg.create_subscription_filter('SAAS_SUBSCRIBER', extra_params={
        'Mode': 'ORIGINAL',
        'UseActualServiceFields': True,
        'OneServicePerUnitedOffer': False,
        'FillOnlyAffectedOffers': False,
        'IgnoreBlueOffersWithoutContent': True,
        'EnableIntegralStatusTrigger': True,
        'Color': 'UNKNOWN_COLOR;WHITE;BLUE',
    })
    converter = cfg.create_united_saas_docs_converter(
        service_offers_table.table_path,
        actual_service_offers_table.table_path
    )
    sender = cfg.create_united_saas_sender(saas)

    cfg.create_link(lb_reader, unpacker)
    cfg.create_link(unpacker, dispatcher)
    cfg.create_link(dispatcher, filter)
    cfg.create_link(filter, converter)
    cfg.create_link(converter, sender)

    return cfg


@pytest.yield_fixture(scope='module')
def dispatcher(
        dispatcher_config,
        basic_offers_table,
        service_offers_table,
        actual_service_offers_table,
        input_topic,
        output_topic,
):
    resources = {
        'dispatcher_config': dispatcher_config,
        'basic_offers_table': basic_offers_table,
        'service_offers_table': service_offers_table,
        'actual_service_offers_table': actual_service_offers_table,
        'input_topic': input_topic,
        'output_topic': output_topic,
    }
    with DispatcherTestEnv(**resources) as env:
        env.verify()
        yield env


@pytest.yield_fixture(scope='module')
def result_data(dispatcher, input_topic):
    for message in SUBSCRIPTION_MESSAGES:
        input_topic.write(message.SerializeToString())

    wait_until(lambda: dispatcher.subscription_dispatcher_processed >= len(SUBSCRIPTION_MESSAGES), timeout=10)


def test_basic_only_in_saas(result_data, saas):
    business_id = BUSINESS_ID
    offer_id = 'offer.with.vendor'
    saas_doc = saas.kv_client.wait_and_get(
        's/{}/{}'.format(business_id, offer_id),
        {'offer_id', 'vendor'},
        kps=business_id,
        sgkps=business_id
    )
    assert_that(saas_doc['offer_id'], equal_to(offer_id))
    assert_that(saas_doc['vendor'], equal_to('vendor value'))


def test_long_search_literal(result_data, saas):
    business_id = BUSINESS_ID
    offer_id = 'offer.with.long.vendor'
    saas_doc = saas.kv_client.wait_and_get(
        's/{}/{}'.format(business_id, offer_id),
        {'offer_id', 'vendor'},
        kps=business_id,
        sgkps=business_id
    )
    assert_that(saas_doc['offer_id'], equal_to(offer_id))
    assert_that(saas_doc, is_not(has_item('vendor')))


def test_intgral_status_update(result_data, saas):
    """ Синий оффер с контентом в базовой части корректно индексируется при обновлении только значения интегрального
        статуса
    """
    business_id = BUSINESS_ID
    offer_id = 'offer.with.integral.status.update'
    saas_doc = saas.kv_client.wait_and_get(
        's/{}/{}'.format(business_id, offer_id),
        {'offer_id', 'vendor'},
        kps=business_id,
        sgkps=business_id
    )
    assert_that(saas_doc['offer_id'], equal_to(offer_id))
    assert_that(saas_doc['vendor'], equal_to('vendor value'))
