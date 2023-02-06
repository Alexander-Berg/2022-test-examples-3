# coding: utf-8

import pytest
import six
import time
from hamcrest import assert_that, has_length, equal_to

import market.idx.datacamp.proto.offer.DataCampOffer_pb2 as DTC
from market.idx.datacamp.controllers.stroller.yatf.test_env import make_stroller
from market.idx.datacamp.yatf.matchers.matchers import HasStatus
from market.idx.yatf.matchers.protobuf_matchers import IsProtobufMap
from market.idx.datacamp.yatf.utils import dict2tskv, create_update_meta
from market.idx.datacamp.yatf.matchers.matchers import (
    HasSerializedDatacampMessages,
)
from market.idx.pylibrary.datacamp.conversion import offer_to_service_row


BUSINESS_ID = 1000


PARTNERS_TABLE_DATA = [
    {
        'shop_id': 1,
        'mbi': '\n\n'.join([
            dict2tskv({'shop_id': 1, 'warehouse_id': 145, 'datafeed_id': 1, 'business_id': BUSINESS_ID}),
            dict2tskv({'shop_id': 1, 'warehouse_id': 172, 'datafeed_id': 2, 'business_id': BUSINESS_ID})
        ])
    },
]


SERVICE_OFFERS_TABLE_DATA = [
    offer_to_service_row(DTC.Offer(
        identifiers=DTC.OfferIdentifiers(
            business_id=BUSINESS_ID,
            offer_id='T1001',
            shop_id=1,
        ),
        meta=DTC.OfferMeta(
            rgb=DTC.BLUE,
        ),
        promos=DTC.OfferPromos(anaplan_promos=DTC.MarketPromos(
            all_promos=DTC.Promos(
                promos=[
                    DTC.Promo(
                        id='promo_1'
                    ),
                    DTC.Promo(
                        id="active_promo",
                    )
                ],
                meta=create_update_meta(int(time.time())),
            ),
            active_promos=DTC.Promos(
                promos=[
                    DTC.Promo(
                        id="active_promo",
                    )
                ],
                meta=create_update_meta(int(time.time())),
            ),
        ))
    )),
]


@pytest.fixture(scope='module')
def partners():
    return PARTNERS_TABLE_DATA


@pytest.fixture(scope='module')
def service_offers():
    return SERVICE_OFFERS_TABLE_DATA


@pytest.yield_fixture()
def stroller(
    config,
    yt_server,
    log_broker_stuff,
    service_offers_table,
    partners_table,
    united_offers_topic,
):
    with make_stroller(
        config,
        yt_server,
        log_broker_stuff,
        partners_table=partners_table,
        shopsdat_cacher=True,
        service_offers_table=service_offers_table,
        united_offers_topic=united_offers_topic,
    ) as stroller_env:
        yield stroller_env


def do_request_deactivate_promo(client, shop_id, promo_id):
    return client.post('/shops/{}/offers/deactivate_promo?promo_id={}'.format(shop_id, promo_id))


def test_deactivate_promos(stroller, united_offers_topic):
    """ Проверяем работоспособность ручки выхода из акции всех офферов магазина
    """
    shop_id = 1
    promo_id = 'active_promo'

    response = do_request_deactivate_promo(stroller, shop_id=shop_id, promo_id=promo_id)
    assert_that(response, HasStatus(200))
    assert_that(response.headers['Content-type'], equal_to('text/plain; charset=utf-8'))
    assert_that(six.ensure_str(response.data), equal_to('Action result will be applied in a minutes'))

    messages = united_offers_topic.read(count=1)
    assert_that(messages, has_length(1))

    assert_that(messages, HasSerializedDatacampMessages([{
        'united_offers': [{
            'offer': [{
                'basic': {
                    'identifiers': {
                        'business_id': BUSINESS_ID,
                        'offer_id': 'T1001',
                    }
                },
                'service': IsProtobufMap({
                    shop_id: {
                        'identifiers': {
                            'business_id': BUSINESS_ID,
                            'offer_id': 'T1001',
                            'shop_id': shop_id,
                            'warehouse_id': None,
                        },
                        'meta': {
                            'scope': DTC.SERVICE,
                            'rgb': DTC.BLUE,
                        }
                    }
                }),
            }]
        }]
    }]))
