# coding: utf-8

import pytest
from hamcrest import assert_that, calling, equal_to, raises

from google.protobuf.timestamp_pb2 import Timestamp
import market.idx.datacamp.proto.offer.DataCampOffer_pb2 as DTC

from market.idx.datacamp.dispatcher.yatf.test_env import DispatcherTestEnv
from market.idx.datacamp.dispatcher.yatf.resources.config import DispatcherConfig
from market.idx.datacamp.controllers.stroller.yatf.test_env import make_stroller
from market.idx.datacamp.proto.api.SyncChangeOffer_pb2 import ChangeOfferRequest
from market.idx.datacamp.proto.api import SyncChangeOffer_pb2 as SyncApi
from market.idx.datacamp.yatf.utils import create_meta, create_update_meta, create_status, create_price, dict2tskv
from market.idx.datacamp.yatf.matchers.matchers import HasStatus
from market.idx.yatf.matchers.protobuf_matchers import IsSerializedProtobuf, IsProtobufMap
from market.idx.yatf.matchers.yt_rows_matchers import HasOffers
from market.pylibrary.proto_utils import message_from_data
from market.idx.pylibrary.datacamp.conversion import offer_to_basic_row, offer_to_service_row
from market.idx.pylibrary.datacamp.utils import wait_until

BUSINESS_ID = 100
SHOP_ID = 200
SHOP_ID_FOR_CACHBACK = 300
WAREHOUSE_ID = 300

UPDATED_OFFERS = [
    {
        'offer_id': 'changed_price',
        'scope': DTC.SERVICE,
        'price': create_price(999, 100, source=DTC.PUSH_PARTNER_OFFICE),
    },
    {
        'offer_id': 'offer_for_promo_test',
        'scope': DTC.SERVICE,
        'price': create_price(333, 100, source=DTC.PUSH_PARTNER_OFFICE),
        'status': create_status(True, 100),
        'promos': DTC.OfferPromos(
            anaplan_promos=DTC.MarketPromos(
                all_promos=DTC.Promos(
                    promos=[
                        DTC.Promo(
                            id='promo_test'
                        )
                    ],
                    meta=DTC.UpdateMeta(timestamp=Timestamp(seconds=12))
                ),
                active_promos=DTC.Promos(
                    promos=[
                        DTC.Promo(
                            id='promo_test'
                        )
                    ],
                    meta=DTC.UpdateMeta(timestamp=Timestamp(seconds=12))
                )
            ),
            partner_promos=DTC.Promos(
                promos=[
                    DTC.Promo(
                        id='partner_promo_test'
                    )
                ],
                meta=DTC.UpdateMeta(timestamp=Timestamp(seconds=12))
            ),
            partner_cashback_promos=DTC.Promos(
                promos=[
                    DTC.Promo(
                        id='partner_cashback_promo_test1'
                    ),
                    DTC.Promo(
                        id='partner_cashback_promo_test2'
                    )
                ],
                meta=DTC.UpdateMeta(timestamp=Timestamp(seconds=12))
            )
        ),
    },
    {
        'offer_id': 'new_service',
        'scope': DTC.SERVICE,
        'price': create_price(333, 100, source=DTC.PUSH_PARTNER_OFFICE),
        'status': create_status(True, 100),
        'promos': DTC.OfferPromos(anaplan_promos=DTC.MarketPromos(
            all_promos=DTC.Promos(
                promos=[
                    DTC.Promo(
                        id='promo1'
                    )
                ],
                meta=DTC.UpdateMeta(timestamp=Timestamp(seconds=1))
            )
        )),
    },
]


def set_offer_meta_and_ids(offer_id, scope, shop_id=SHOP_ID):
    identifiers = {
        'business_id': BUSINESS_ID,
        'offer_id': offer_id
    }
    if scope == DTC.SERVICE:
        identifiers['shop_id'] = shop_id
        identifiers['warehouse_id'] = WAREHOUSE_ID

    offer = {
        'identifiers': DTC.OfferIdentifiers(**identifiers),
        'meta': create_meta(100, color=DTC.BLUE, scope=scope),
    }
    return offer


def create_updated_offer(offer_id, scope, updated_parts, shop_id=SHOP_ID):
    offer = set_offer_meta_and_ids(offer_id, scope, shop_id=shop_id)
    for key, value in updated_parts.items():
        offer[key] = value

    return DTC.Offer(**offer)


@pytest.fixture(scope='module')
def basic_offers():
    def create_basic(offer_id):
        offer = set_offer_meta_and_ids(offer_id, DTC.BASIC)
        offer['content'] = DTC.OfferContent(
            market=DTC.MarketContent(
                category_id=111,
                vendor_id=333,
                meta=create_update_meta(10)
            ),
            partner=DTC.PartnerContent(
                original=DTC.OriginalSpecification(
                    name=DTC.StringValue(
                        value='name placeholder',
                        meta=create_update_meta(10)
                    )
                )
            )
        )
        return DTC.Offer(**offer)

    return [offer_to_basic_row(create_basic(offer['offer_id'])) for offer in UPDATED_OFFERS]


@pytest.fixture(scope='module')
def service_offers():
    def create_service(offer_id):
        offer = set_offer_meta_and_ids(offer_id, DTC.SERVICE)
        offer['status'] = create_status(False, 10)
        offer['price'] = create_price(2000, 10)
        offer['promos'] = DTC.OfferPromos(anaplan_promos=DTC.MarketPromos(
            all_promos=DTC.Promos(
                promos=[
                    DTC.Promo(
                        id='promo1'
                    )
                ],
                meta=DTC.UpdateMeta(timestamp=Timestamp(seconds=1))
            )
        ))
        o = DTC.Offer(**offer)
        o.identifiers.warehouse_id = 0
        return o

    return [offer_to_service_row(create_service(offer['offer_id'])) for offer in UPDATED_OFFERS[:-1]]


@pytest.fixture(scope='module')
def partners(config):
    return [
        {
            'shop_id': SHOP_ID,
            'mbi': '\n\n'.join([
                dict2tskv({
                    'shop_id': SHOP_ID,
                    'blue_status': 'REAL',
                    'business_id': BUSINESS_ID,
                    'warehouse_id': WAREHOUSE_ID,
                    'supplier_type': 3,
                }),
            ]),
            'status': 'publish'
        },
    ]


@pytest.yield_fixture(scope='module')
def stroller(
        config,
        yt_server,
        log_broker_stuff,
        partners_table,
        basic_offers_table,
        service_offers_table,
        saas
):
    with make_stroller(
            config,
            yt_server,
            log_broker_stuff,
            local_saas=saas,
            shopsdat_cacher=True,
            partners_table=partners_table,
            basic_offers_table=basic_offers_table,
            service_offers_table=service_offers_table
    ) as stroller_env:
        yield stroller_env


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
    converter = cfg.create_saas_for_shops_converter()
    sender = cfg.create_united_saas_sender(saas)

    cfg.create_link(lb_reader, unpacker)
    cfg.create_link(unpacker, dispatcher)
    cfg.create_link(dispatcher, converter)
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


def test_send_to_united_saas_for_shop(dispatcher, stroller, saas, service_offers_table):
    change_offer_request = ChangeOfferRequest()
    for offer in UPDATED_OFFERS:
        updated = create_updated_offer(offer.pop('offer_id'), offer.pop('scope'), offer)
        change_offer_request.offer.extend([updated])

    response = stroller.post(
        '/shops/{}/offers?warehouse_id={}'.format(SHOP_ID, WAREHOUSE_ID),
        data=change_offer_request.SerializeToString()
    )
    assert_that(response, HasStatus(200))

    assert_that(calling(saas.kv_client.wait_and_get).with_args(
        's/{}/{}'.format(SHOP_ID, 'changed_price'),
        {'offer_id'},
        kps=SHOP_ID,
        sgkps=SHOP_ID
    ), raises(RuntimeError))

    saas_doc = saas.kv_client.wait_and_get(
        's/{}/{}'.format(SHOP_ID, 'new_service'),
        {'offer_id'},
        kps=SHOP_ID,
        sgkps=SHOP_ID
    )
    assert_that(saas_doc['offer_id'], equal_to('new_service'))

    saas_doc = saas.kv_client.wait_and_get(
        's/{}/{}'.format(SHOP_ID, 'offer_for_promo_test'),
        {'offer_id', 'active_promo_id', 'promo_id', 'partner_cashback_promo_id'},
        kps=SHOP_ID,
        sgkps=SHOP_ID
    )
    assert_that(saas_doc['offer_id'], equal_to('offer_for_promo_test'))
    assert_that(saas_doc['active_promo_id'], equal_to(['partner_promo_test', 'promo_test']))
    assert_that(saas_doc['partner_cashback_promo_id'], equal_to(['partner_cashback_promo_test1', 'partner_cashback_promo_test2']))

    service_offers_table.load()
    assert_that(service_offers_table.data, HasOffers([
        message_from_data({
            'identifiers': {
                'business_id': BUSINESS_ID,
                'offer_id': 'offer_for_promo_test',
                'shop_id': SHOP_ID,
            },
            'promos': {
                'anaplan_promos': {
                    'all_promos': {
                        'promos': [
                            {
                                'id': 'promo_test'
                            }
                        ],
                    },
                    'active_promos': {
                        'promos': [
                            {
                                'id': 'promo_test'
                            }
                        ],
                    },
                },
                'partner_promos': {
                    'promos': [
                        {
                            'id': 'partner_promo_test'
                        }
                    ]
                },
                'partner_cashback_promos': {
                    'promos': [
                        {
                            'id': 'partner_cashback_promo_test1'
                        },
                        {
                            'id': 'partner_cashback_promo_test2'
                        }
                    ]
                }
            }
        }, DTC.Offer())
    ]))

    offer_for_promo_test2 = {
        'offer_id': 'offer_for_promo_test',
        'scope': DTC.SERVICE,
        'price': create_price(333, 100, source=DTC.PUSH_PARTNER_OFFICE),
        'status': create_status(True, 100),
        'promos': DTC.OfferPromos(
            anaplan_promos=DTC.MarketPromos(
                all_promos=DTC.Promos(
                    promos=[
                        DTC.Promo(
                            id='promo_test'
                        )
                    ],
                    meta=DTC.UpdateMeta(timestamp=Timestamp(seconds=100))
                ),
                active_promos=DTC.Promos(
                    promos=[],
                    meta=DTC.UpdateMeta(timestamp=Timestamp(seconds=100))
                )
            ),
            partner_promos=DTC.Promos(
                promos=[],
                meta=DTC.UpdateMeta(timestamp=Timestamp(seconds=100))
            ),
            partner_cashback_promos=DTC.Promos(
                promos=[],
                meta=DTC.UpdateMeta(timestamp=Timestamp(seconds=100))
            )
        )
    }
    change_offer_request_2 = ChangeOfferRequest()
    updated_2 = create_updated_offer(offer_for_promo_test2.pop('offer_id'), offer_for_promo_test2.pop('scope'), offer_for_promo_test2)
    change_offer_request_2.offer.extend([updated_2])
    response_2 = stroller.post(
        '/shops/{}/offers?warehouse_id={}'.format(SHOP_ID, WAREHOUSE_ID),
        data=change_offer_request_2.SerializeToString()
    )
    assert_that(response_2, HasStatus(200))

    service_offers_table.load()
    assert_that(service_offers_table.data, HasOffers([
        message_from_data({
            'identifiers': {
                'business_id': BUSINESS_ID,
                'offer_id': 'offer_for_promo_test',
                'shop_id': SHOP_ID,
            },
            'promos': {
                'anaplan_promos': {
                    'all_promos': {
                        'promos': [{'id': 'promo_test'}],
                    },
                    'active_promos': {'promos': []},
                },
                'partner_promos': {
                    'promos': []
                },
                'partner_cashback_promos': {
                    'promos': []
                }
            }
        }, DTC.Offer())
    ]))

    saas_doc = saas.kv_client.wait_and_get(
        's/{}/{}'.format(SHOP_ID, 'offer_for_promo_test'),
        {'offer_id', 'active_promo_id', 'promo_id', 'partner_cashback_promo_id'},
        kps=SHOP_ID,
        sgkps=SHOP_ID
    )
    # в обновленном доке нет active_promo_id, тк активную акцию убрали
    assert_that('active_promo_id' not in saas_doc.keys())
    # Партнерскую КБ акцию удалили
    assert_that('partner_cashback_promo_id' not in saas_doc.keys())

    # Далее проверяем, что оффер только с промоакцией partner_cashback_promos тоже индексируется
    offer_partner_cashback_promos = {
        'price': create_price(333, 100, source=DTC.PUSH_PARTNER_OFFICE),
        'status': create_status(True, 100),
        'promos': DTC.OfferPromos(
            partner_cashback_promos=DTC.Promos(
                promos=[
                    DTC.Promo(
                        id='promo_test'
                    )
                ],
                meta=DTC.UpdateMeta(timestamp=Timestamp(seconds=100))
            )
        )
    }
    change_offer_request = ChangeOfferRequest()
    change_offer_request.offer.extend([create_updated_offer(
        'offer_for_promo_test',
        DTC.SERVICE,
        offer_partner_cashback_promos,
        shop_id=SHOP_ID_FOR_CACHBACK
    )])
    response_2 = stroller.post(
        '/shops/{}/offers?warehouse_id={}'.format(SHOP_ID, WAREHOUSE_ID),
        data=change_offer_request.SerializeToString()
    )
    assert_that(response_2, HasStatus(200))

    service_offers_table.load()
    assert_that(service_offers_table.data, HasOffers([
        message_from_data({
            'identifiers': {
                'business_id': BUSINESS_ID,
                'offer_id': 'offer_for_promo_test',
                'shop_id': SHOP_ID_FOR_CACHBACK,
            },
            'promos': {
                'partner_cashback_promos': {
                    'promos': [
                        {
                            'id': 'promo_test'
                        }
                    ]
                }
            }
        }, DTC.Offer())
    ]))

    saas_doc = saas.kv_client.wait_and_get(
        's/{}/{}'.format(SHOP_ID_FOR_CACHBACK, 'offer_for_promo_test'),
        {'offer_id', 'active_promo_id', 'promo_id', 'partner_cashback_promo_id'},
        kps=SHOP_ID_FOR_CACHBACK,
        sgkps=SHOP_ID_FOR_CACHBACK
    )
    assert_that(saas_doc['offer_id'], equal_to('offer_for_promo_test'))
    assert_that(saas_doc['partner_cashback_promo_id'], equal_to('promo_test'))

    # Удаляем документ из хранилища, должен удалиться из saas
    response = stroller.post('/v1/partners/{business_id}/offers/basic/remove?offer_id={offer_id}'.format(
        business_id=BUSINESS_ID,
        offer_id='offer_for_promo_test'
    ))
    assert_that(response, HasStatus(200))
    assert_that(response.data, IsSerializedProtobuf(SyncApi.DeleteOfferResponse, {
        'result': IsProtobufMap({
            'offer_for_promo_test': {
                'status': SyncApi.DeleteOfferResponse.SUCCESS
            }
        }),
    }))

    # Ждем, пока удалится из saas
    def is_removed():
        return not saas.kv_client.get(
            's/{}/{}'.format(SHOP_ID, 'offer_for_promo_test'),
            {'offer_id'},
            kps=SHOP_ID,
            sgkps=SHOP_ID,
            timeout=2
        )
    wait_until(is_removed, timeout=10)
