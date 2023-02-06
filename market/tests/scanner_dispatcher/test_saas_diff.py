# coding: utf-8

import pytest
import yatest
import os
from datetime import datetime

from market.idx.yatf.test_envs.saas_env import SaasEnv
from hamcrest import assert_that, equal_to
from market.idx.yatf.resources.lbk_topic import LbkTopic
from market.idx.datacamp.dispatcher.yatf.test_env import DispatcherTestEnv
from market.idx.datacamp.dispatcher.yatf.resources.config import DispatcherConfig
from market.idx.datacamp.controllers.scanner.yatf.test_env import make_scanner
from market.idx.datacamp.yatf.utils import (
    create_meta, dict2tskv, create_update_meta,
)
from market.idx.yatf.matchers.yt_rows_matchers import HasOffers
import market.idx.datacamp.proto.offer.DataCampOffer_pb2 as DTC
from market.pylibrary.proto_utils import message_from_data
from market.idx.pylibrary.datacamp.utils import wait_until
from market.idx.pylibrary.datacamp.conversion import offer_to_basic_row, offer_to_service_row

BUSINESS_ID = 1000
ONLY_BASIC_OFFER_ID = 'only_basic_offer'
WITH_SERVICE_OFFER_ID = 'with_service_offer'
SHOP_ID = 1


@pytest.fixture(scope='module')
def partners_table_data():
    return [
        {
            'shop_id': SHOP_ID,
            'mbi': '\n\n'.join([
                dict2tskv({
                    'shop_id': SHOP_ID,
                    'business_id': BUSINESS_ID,
                    'united_catalog_status': 'SUCCESS',
                }),
            ]),
            'status': 'publish'
        }
    ]


@pytest.fixture(scope='module')
def basic_offers_table_data(color):
    return [
        offer_to_basic_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=BUSINESS_ID,
                offer_id=ONLY_BASIC_OFFER_ID,
            ),
            content=DTC.OfferContent(
                partner=DTC.PartnerContent(
                    original=DTC.OriginalSpecification(
                        name=DTC.StringValue(
                            value="Name",
                            meta=create_update_meta(10)
                        ),
                    )
                )
            ),
            meta=create_meta(10, DTC.BASIC),
        )),
        offer_to_basic_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=BUSINESS_ID,
                offer_id=WITH_SERVICE_OFFER_ID,
            ),
            content=DTC.OfferContent(
                partner=DTC.PartnerContent(
                    original=DTC.OriginalSpecification(
                        name=DTC.StringValue(
                            value="Name",
                            meta=create_update_meta(10)
                        ),
                    )
                )
            ),
            meta=create_meta(10, DTC.BASIC),
        ))
    ]


@pytest.fixture(scope='module')
def service_offers_table_data(color):
    return [
        offer_to_service_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=BUSINESS_ID,
                offer_id=WITH_SERVICE_OFFER_ID,
                shop_id=SHOP_ID,
            ),
            meta=create_meta(10, DTC.SERVICE),
        ))
    ]


@pytest.fixture(scope='module')
def saas_diff_table_data():
    meta = create_meta(10, scope=DTC.BASIC)
    meta.saas_force_send.meta.timestamp.FromSeconds(10)
    meta.saas_force_send.ts.FromSeconds(10)

    return [{
        'offer': DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=BUSINESS_ID,
                offer_id=ONLY_BASIC_OFFER_ID,
            ),
            meta=meta
        ).SerializeToString()
    }, {
        'offer': DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=BUSINESS_ID,
                offer_id=WITH_SERVICE_OFFER_ID,
            ),
            meta=meta
        ).SerializeToString()
    }]


@pytest.fixture(scope='module')
def saas():
    rel_path = os.path.join('market', 'idx', 'yatf', 'resources', 'saas', 'stubs', 'market-idxapi')
    with SaasEnv(saas_service_configs=yatest.common.source_path(rel_path), prefixed=True) as saas:
        yield saas


@pytest.fixture(scope='module')
def subscription_service_topic(log_broker_stuff):
    topic = LbkTopic(log_broker_stuff, 'subscription-service')
    return topic


@pytest.fixture(scope='module')
def dispatcher_config(
    yt_token,
    yt_server,
    log_broker_stuff,
    subscription_service_topic,
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

    lb_reader = cfg.create_lb_reader(log_broker_stuff, subscription_service_topic)
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
        subscription_service_topic,
):
    resources = {
        'dispatcher_config': dispatcher_config,
        'basic_offers_table': basic_offers_table,
        'service_offers_table': service_offers_table,
        'actual_service_offers_table': actual_service_offers_table,
        'subscription_service_topic': subscription_service_topic,
    }
    with DispatcherTestEnv(**resources) as env:
        env.verify()
        yield env


@pytest.fixture(scope='module')
def scanner(
    log_broker_stuff,
    yt_server,
    scanner_resources,
    color,
    saas,
    subscription_service_topic,
):
    with make_scanner(
        yt_server,
        log_broker_stuff,
        color,
        local_saas=saas,
        shopsdat_cacher=True,
        subscription_service_topic=subscription_service_topic,
        **scanner_resources
    ) as scanner_env:
        wait_until(lambda: scanner_env.united_offers_processed == 2, timeout=60)
        yield scanner_env


def test_basic_only_update(dispatcher, scanner, saas):
    assert_that(scanner.basic_offers_table.data, HasOffers([message_from_data({
        'identifiers': {
            'business_id': BUSINESS_ID,
            'offer_id': ONLY_BASIC_OFFER_ID,
        },
        'meta': {
            'saas_force_send': {
                'ts': datetime.utcfromtimestamp(10).strftime("%Y-%m-%dT%H:%M:%SZ")
            }
        }
    }, DTC.Offer())]))
    assert_that(scanner.united_offers_processed, equal_to(2))

    saas_doc = saas.kv_client.wait_and_get(
        's/{}/{}'.format(BUSINESS_ID, ONLY_BASIC_OFFER_ID),
        {'offer_id'},
        kps=BUSINESS_ID,
        sgkps=BUSINESS_ID
    )
    assert_that(saas_doc['offer_id'], equal_to(ONLY_BASIC_OFFER_ID))
