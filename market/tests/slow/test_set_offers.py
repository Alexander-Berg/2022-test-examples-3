# coding: utf-8

import pytest
import time
from hamcrest import assert_that, equal_to, matches_regexp

from google.protobuf.timestamp_pb2 import Timestamp

from market.idx.datacamp.controllers.stroller.yatf.test_env import make_stroller
from market.idx.datacamp.proto.api.SyncChangeOffer_pb2 import ChangeOfferRequest, FullOfferResponse
from market.idx.datacamp.proto.api.OffersBatch_pb2 import (
    OffersBatchRequest,
    RequestMethod
)
from market.idx.datacamp.proto.offer import DataCampOffer_pb2 as DTC
from market.idx.datacamp.yatf.matchers.matchers import HasStatus
from market.idx.yatf.matchers.protobuf_matchers import IsSerializedProtobuf
from market.proto.common.common_pb2 import PriceExpression
from market.idx.datacamp.yatf.utils import create_meta, create_update_meta, dict2tskv
from market.idx.yatf.matchers.yt_rows_matchers import HasOffers
from market.pylibrary.proto_utils import message_from_data
from market.idx.pylibrary.datacamp.conversion import offer_to_basic_row, offer_to_service_row


BUSINESS_ID = 1000
SHOP_ID = 1


def make_colored_offer_id(offer_id, color):
    return '{}.{}'.format(offer_id, color)


@pytest.fixture()
def partners():
    return [
        {
            'shop_id': SHOP_ID,
            'mbi': '\n\n'.join([
                dict2tskv({
                    'shop_id': SHOP_ID,
                    'business_id': BUSINESS_ID,
                    'blue_status': 'REAL',
                }),
            ]),
            'status': 'publish'
        },
        {
            'shop_id': 2,
            'mbi': '\n\n'.join([
                dict2tskv({
                    'shop_id': 2,
                    'business_id': BUSINESS_ID,
                    'blue_status': 'REAL',
                }),
            ]),
        },
    ]


@pytest.fixture()
def warehouse_id():
    return 145


@pytest.fixture()
def basic_offers(color_name):
    return [
        offer_to_basic_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=BUSINESS_ID,
                offer_id=make_colored_offer_id('some.existing.offer', color_name),
            ),
            meta=create_meta(10, scope=DTC.BASIC),
        )),
        offer_to_basic_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=BUSINESS_ID,
                offer_id=make_colored_offer_id('some.offer.with.promos', color_name),
            ),
            content=DTC.OfferContent(
                market=DTC.MarketContent(
                    vendor_id=1000,
                    category_id=100,
                ),
                partner=DTC.PartnerContent(
                    original=DTC.OriginalSpecification(
                        name=DTC.StringValue(
                            value='name placeholder',
                            meta=create_update_meta(10)
                        )
                    )
                )
            ),
            meta=create_meta(10, scope=DTC.BASIC),
        )),
    ]


@pytest.fixture()
def service_offers(color_name):
    return [
        offer_to_service_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=BUSINESS_ID,
                offer_id=make_colored_offer_id('some.existing.offer', color_name),
                shop_id=SHOP_ID,
            ),
            meta=create_meta(10, color=DTC.BLUE, scope=DTC.SERVICE),
        )),
        offer_to_service_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=BUSINESS_ID,
                offer_id=make_colored_offer_id('some.offer.with.promos', color_name),
                shop_id=SHOP_ID,
            ),
            meta=create_meta(10, color=DTC.BLUE, scope=DTC.SERVICE),
            promos=DTC.OfferPromos(anaplan_promos=DTC.MarketPromos(
                all_promos=DTC.Promos(
                    promos=[
                        DTC.Promo(
                            id='promo_1'
                        ),
                        DTC.Promo(
                            id="promo_2",
                        )
                    ],
                    meta=create_update_meta(int(time.time())),
                )
            ),
                partner_promos=DTC.Promos(
                    promos=[
                        DTC.Promo(
                            id='partner_promo_1'
                        )
                    ],
                    meta=create_update_meta(int(time.time())),
                ))
        )),
    ]


@pytest.fixture()
def actual_service_offers(warehouse_id, color_name):
    return [
        offer_to_service_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=BUSINESS_ID,
                offer_id=make_colored_offer_id('some.existing.offer', color_name),
                shop_id=SHOP_ID,
                warehouse_id=warehouse_id,
            ),
            meta=create_meta(10, color=DTC.BLUE, scope=DTC.SERVICE),
        )),
        offer_to_service_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=BUSINESS_ID,
                offer_id=make_colored_offer_id('some.offer.with.promos', color_name),
                shop_id=SHOP_ID,
                warehouse_id=warehouse_id,
            ),
            meta=create_meta(10, color=DTC.BLUE, scope=DTC.SERVICE),
        )),
    ]


@pytest.yield_fixture()
def stroller(
    config,
    yt_server,
    log_broker_stuff,
    basic_offers_table,
    service_offers_table,
    actual_service_offers_table,
    partners_table,
):
    with make_stroller(
        config,
        yt_server,
        log_broker_stuff,
        shopsdat_cacher=True,
        partners_table=partners_table,
        basic_offers_table=basic_offers_table,
        service_offers_table=service_offers_table,
        actual_service_offers_table=actual_service_offers_table,
    ) as stroller_env:
        yield stroller_env


def request_offer(client, shop_id, offer_id, warehouse_id):
    uri = '/shops/{}/offers?offer_id={}'.format(shop_id, offer_id)
    if warehouse_id is not None:
        uri = '{}&warehouse_id={}'.format(uri, warehouse_id)
    response = client.get(uri)
    return response


def request(http_method, client, expected_offer, timestamp, use_batch_request=False):
    shop_id = expected_offer['identifiers']['shop_id']
    offer_id = expected_offer['identifiers'].get('offer_id', None)
    warehouse_id = expected_offer['identifiers'].get('warehouse_id', None)
    business_id = expected_offer['identifiers'].get('business_id', None)

    change_offer_request = ChangeOfferRequest()
    offer = change_offer_request.offer.add()

    offer.identifiers.shop_id = shop_id
    if warehouse_id is not None:
        offer.identifiers.warehouse_id = warehouse_id
    if offer_id is not None:
        offer.identifiers.offer_id = offer_id
    if business_id is not None:
        offer.identifiers.business_id = business_id

    if 'status' in expected_offer and 'publish' in expected_offer['status']:
        offer.status.publish = expected_offer['status']['publish']
    if 'status' in expected_offer and 'united_catalog' in expected_offer['status']:
        offer.status.united_catalog.flag = expected_offer['status']['united_catalog']['flag']
        offer.status.united_catalog.meta.timestamp.FromJsonString(timestamp)

    if 'meta' in expected_offer:
        offer.meta.scope = expected_offer['meta']['scope']

    offer.price.basic.meta.timestamp.FromJsonString(timestamp)
    offer.price.basic.meta.source = expected_offer['price']['basic']['meta']['source']
    offer.price.basic.binary_price.CopyFrom(PriceExpression(
        price=expected_offer['price']['basic']['binary_price']['price']
    ))

    if 'content' in expected_offer:
        if 'partner' in expected_offer['content']:
            offer.content.partner.original.name.value = expected_offer['content']['partner']['original']['name']['value']
            offer.content.partner.original.name.meta.timestamp.FromJsonString(timestamp)

        mapping = expected_offer['content']['binding']['partner']
        offer.content.binding.partner.market_category_id = mapping['market_category_id']
        offer.content.binding.partner.market_model_id = mapping['market_model_id']
        offer.content.binding.partner.market_sku_id = mapping['market_sku_id']
        offer.content.binding.partner.meta.timestamp.FromJsonString(timestamp)
        offer.content.binding.partner.meta.source = mapping['meta']['source']

        smb_mapping = expected_offer['content']['binding']['smb_partner']
        offer.content.binding.smb_partner.market_category_id = smb_mapping['market_category_id']
        offer.content.binding.smb_partner.market_model_id = smb_mapping['market_model_id']
        offer.content.binding.smb_partner.market_sku_id = smb_mapping['market_sku_id']
        offer.content.binding.smb_partner.meta.timestamp.FromJsonString(timestamp)
        offer.content.binding.smb_partner.meta.source = smb_mapping['meta']['source']

    if 'promos' in expected_offer:
        if 'active_promos' in expected_offer['promos']['anaplan_promos']:
            for promo in expected_offer['promos']['anaplan_promos']['active_promos']['promos']:
                p = offer.promos.anaplan_promos.active_promos.promos.add()
                p.id = promo['id']
            offer.promos.anaplan_promos.active_promos.meta.CopyFrom(create_update_meta(10))
        if 'all_promos' in expected_offer['promos']['anaplan_promos']:
            for promo in expected_offer['promos']['anaplan_promos']['all_promos']['promos']:
                p = offer.promos.anaplan_promos.all_promos.promos.add()
                p.id = promo['id']
        if 'partner_promos' in expected_offer['promos']:
            for promo in expected_offer['promos']['partner_promos']['promos']:
                p = offer.promos.partner_promos.promos.add()
                p.id = promo['id']
            offer.promos.partner_promos.meta.CopyFrom(create_update_meta(10))

    uri = '/shops/{}/offers?'.format(shop_id)
    if offer_id is not None:
        uri = '{}&offer_id={}'.format(uri, offer_id)
    if warehouse_id is not None:
        uri = '{}&warehouse_id={}'.format(uri, warehouse_id)
    if use_batch_request:
        batch_request = OffersBatchRequest()
        entry = batch_request.entries.add()
        entry.method = RequestMethod.POST
        entry.business_id = business_id
        entry.offer_id = offer_id
        entry.shop_id = shop_id
        entry.offer.CopyFrom(offer)
        return client.do_request(
            method='post',
            path='/v1/offers/batch',
            data=batch_request.SerializeToString(),
        )
    else:
        return client.do_request(
            method=http_method,
            path=uri,
            data=change_offer_request.SerializeToString())


def expected_offer(
    shop_id,
    offer_id,
    warehouse_id,
    source,
    ts,
    price,
    anaplan_promos=None,
    partner_promos=None,
    business_id=None,
    scope=None,
    status=None,
):
    timestamp = Timestamp()
    timestamp.FromJsonString(ts)

    result = {
        'identifiers': {
            'shop_id': shop_id,
        },
        'price': {
            'basic': {
                'binary_price': {
                    'price': price,
                },
                'meta': {
                    'source': source,
                    'timestamp': {
                        'seconds': timestamp.seconds,
                    }
                },
            },
        },
        'content': {
            'binding': {
                'partner': {
                    'market_category_id': 1,
                    'market_model_id': 2,
                    'market_sku_id': 3,
                    'meta': {
                        'source': source,
                        'timestamp': {
                            'seconds': timestamp.seconds,
                        }
                    },
                },
                'smb_partner': {
                    'market_category_id': 1,
                    'market_model_id': 2,
                    'market_sku_id': 3,
                    'meta': {
                        'source': source,
                        'timestamp': {
                            'seconds': timestamp.seconds,
                        }
                    },
                },
            }
        },
    }

    if warehouse_id:
        result['identifiers']['warehouse_id'] = warehouse_id
    if offer_id is not None:
        result['identifiers']['offer_id'] = offer_id
    if business_id is not None:
        result['identifiers']['business_id'] = business_id
    if scope is not None:
        result['meta'] = {'scope': scope}
    if status is not None:
        result['status'] = {'publish': status}

    if anaplan_promos is not None or partner_promos is not None:
        result['promos'] = {}

    if anaplan_promos is not None:
        result['promos']['anaplan_promos'] = anaplan_promos

    if partner_promos is not None:
        result['promos']['partner_promos'] = partner_promos

    return result


def test_generate_offer_id(stroller, warehouse_id):
    """Проверяем генерацию offer_id для нового оффера"""
    source = DTC.PUSH_PARTNER_OFFICE
    timestamp = '2019-02-15T15:55:55Z'
    shop_id = SHOP_ID
    offer_id = None
    price = 100

    expected = expected_offer(shop_id, offer_id, warehouse_id, price=price, source=source, ts=timestamp)
    expected_data = {
        'offer': [expected],
    }
    response = request('post', stroller, expected, timestamp)
    assert_that(response, HasStatus(200))
    assert_that(response.data, IsSerializedProtobuf(FullOfferResponse, expected_data))

    offers = FullOfferResponse()
    offers.ParseFromString(response.data)
    offer_id = offers.offer[0].identifiers.offer_id

    # Проверяем формат генерируемого идентификатора оффера - символы [a-z0-9]
    assert_that(offer_id, matches_regexp('[a-z0-9]{20,80}'))

    # Проверяем, что оффер записан в таблицу
    response = request_offer(stroller, shop_id=shop_id, offer_id=offer_id, warehouse_id=warehouse_id)
    assert_that(response, HasStatus(200))
    assert_that(response.data, IsSerializedProtobuf(FullOfferResponse, expected_data))


def test_update_without_offer_id(stroller, warehouse_id):
    """Проверяем, что обновление без offer_id невозможно"""
    source = DTC.PUSH_PARTNER_OFFICE
    timestamp = '2019-02-15T15:55:55Z'
    shop_id = SHOP_ID
    offer_id = None
    price = 100

    expected = expected_offer(shop_id, offer_id, warehouse_id, price=price, source=source, ts=timestamp)
    response = request('put', stroller, expected, timestamp)
    assert_that(response, HasStatus(403))


@pytest.mark.parametrize('use_batch_request', [False, True])
def test_set_united_offer(yt_server, stroller, config, use_batch_request, color_name):
    timestamp = '2019-02-15T15:55:55Z'
    basic_offer = expected_offer(2, 'o2', 2, price=100, source=DTC.PUSH_PARTNER_OFFICE,
                                    ts='2019-02-15T15:55:55Z', scope=DTC.BASIC, business_id=1)
    response = request('put', stroller, basic_offer, timestamp, use_batch_request=use_batch_request)
    assert_that(response, HasStatus(200))

    service_offer = expected_offer(2, 'o3', 2, price=100, source=DTC.PUSH_PARTNER_OFFICE,
                                   ts='2019-02-15T15:55:55Z', scope=DTC.SERVICE, business_id=1, status=DTC.HIDDEN)
    response = request('put', stroller, service_offer, timestamp, use_batch_request=use_batch_request)
    assert_that(response, HasStatus(200))

    basic_offers_table_data = stroller.basic_offers_table.data
    assert_that(len(basic_offers_table_data), equal_to(4))
    assert_that(basic_offers_table_data, HasOffers(
        [message_from_data({
            'identifiers': {
                'business_id': 1,
                'offer_id': 'o2',
                'extra': {
                    'recent_business_id': 1,
                }
            },
        }, DTC.Offer())]
    ))

    service_offer['price']['basic']['meta']['timestamp'] = timestamp
    service_offers_table_data = stroller.service_offers_table.data
    assert_that(len(service_offers_table_data), equal_to(3))
    assert_that(service_offers_table_data, HasOffers([
        message_from_data({
            'identifiers': {
                'business_id': 1,
                'offer_id': 'o3',
                'extra': {
                    'recent_business_id': 1,
                }
            },
            'price': service_offer['price']
        }, DTC.Offer())
    ]))
