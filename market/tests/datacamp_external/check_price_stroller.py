# coding: utf-8

import allure
import yatest
import random

from common import DataCampResponse, Offer, stroller_only, get_stroller_client
from constants import (
    OFFER_STROLLER_ID,
    WHID,
    SHOP_ID
)
from hamcrest import (
    assert_that,
    equal_to,
)

from market.idx.yatf.matchers.protobuf_matchers import IsSerializedProtobuf
from market.idx.datacamp.proto.api.SyncChangeOffer_pb2 import FullOfferResponse
from market.idx.datacamp.proto.offer import DataCampOffer_pb2 as DTC
from market.idx.datacamp.yatf.matchers.matchers import HasStatus

offer = Offer(SHOP_ID, yatest.common.get_param("offer_id") or OFFER_STROLLER_ID, WHID)
datacamp = DataCampResponse()

def expected_offer(
        shop_id,
        offer_id,
        warehouse_id,
        source,
        price=None,
):

    result = {
        'identifiers': {
            'shop_id': shop_id,
            'offer_id': offer_id,
            'warehouse_id': warehouse_id,
        },
        'price': {
            'basic': {
                'meta': {
                    'source': source,
                },
            },
        },
    }

    if price:
        binary_price = {
            'binary_price': {
                'price': price,
            },
        }
        result['price']['basic'].update(binary_price)

    return result


@allure.story('stroller_set_price')
@allure.feature('price')
@allure.feature('threshold')
@allure.issue('https://st.yandex-team.ru/MARKETINDEXER-36863')
@stroller_only
def test_stroller_set_price():
    '''Проверяем, что строллер устанавливает цену'''
    source = DTC.PUSH_PARTNER_OFFICE
    price = random.randint(777, 7777777777)

    with allure.step('Установка цены в строллере на ' + str(price)):
        response = datacamp.stroller_set_price(offer, price)
        assert_that(response, HasStatus(200))

    expected_data = {
        'offer': [expected_offer(
            offer.shop_id, offer.offer_id, offer.whid, price=price, source=source,
        )],
    }
    with allure.step('Проверка цены в строллере'):
        response = datacamp.stroller_response(offer)
        assert_that(response, HasStatus(200))
        assert_that(response.data, IsSerializedProtobuf(FullOfferResponse, expected_data))
