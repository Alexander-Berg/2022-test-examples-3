# coding: utf-8

import logging
import os
import pytest
import requests
import time

from hamcrest import assert_that, equal_to, not_
from market.idx.datacamp.controllers.scanner.yatf.test_env import make_scanner
from market.idx.pylibrary.datacamp.utils import wait_until
from market.idx.pylibrary.datacamp.conversion import offer_to_service_row, offer_to_basic_row
from market.idx.datacamp.yatf.utils import create_meta, create_update_meta

from market.idx.yatf.matchers.yt_rows_matchers import HasOffers
import market.idx.datacamp.proto.tables.PiperQueueDumpSchema_pb2 as PQDS
import market.idx.datacamp.proto.offer.DataCampOffer_pb2 as DTC


log = logging.getLogger(__name__)
os.environ['BATCH_SIZE'] = '10'
BATCH_SIZE = int(os.environ.get('BATCH_SIZE'))


def now():
    return int(time.time())


def gen_offer(
    business_id,
    offer_id,
    timestamp=None,
    shop_id=None,
    vendor=None,
    price=None,
):
    offer = PQDS.Offer(
        identifiers=PQDS.OfferIdentifiers(
            business_id=business_id,
            offer_id=offer_id
        )
    )
    if timestamp:
        offer.meta.CopyFrom(create_meta(timestamp))
    if shop_id:
        offer.identifiers.shop_id = shop_id
        if price:
            offer.price.basic.binary_price.price = price
            if timestamp:
                offer.price.basic.meta.CopyFrom(create_update_meta(timestamp))
    else:
        if vendor:
            offer.content.partner.original.vendor.value = vendor
            if timestamp:
                offer.content.partner.original.vendor.meta.CopyFrom(create_update_meta(timestamp))
    return offer


def basic_offer(business_id, shop_sku, timestamp=None, vendor=None):
    return gen_offer(business_id, shop_sku, timestamp=timestamp, vendor=vendor)


def service_offer(business_id, shop_sku, shop_id, timestamp=None, price=None):
    return gen_offer(business_id, shop_sku, shop_id=shop_id, timestamp=timestamp, price=price)


def united_offer(business_id, shop_sku, shop_id, timestamp=None, price=None, vendor=None):
    return PQDS.UnitedOffer(
        basic=basic_offer(business_id, shop_sku, timestamp=timestamp, vendor=vendor),
        service={
            shop_id: service_offer(business_id, shop_sku, shop_id=shop_id, timestamp=timestamp, price=price)
        }
    )


def gen_row(business_id, shop_sku, shop_id, timestamp=None, price=None, vendor=None):
    return {
        'business_id': business_id,
        'shop_sku': shop_sku,
        'timestamp': timestamp,
        'offer': united_offer(business_id, shop_sku, shop_id=shop_id, timestamp=timestamp, vendor=vendor, price=price).SerializeToString()
    }


ROWS = [
    # basic test
    # +1 processed
    gen_row(1, '1', 1, timestamp=now()+i, price=i) for i in range(BATCH_SIZE)
] + [
    # test two batches with the same business_id + shop_id, but different updates
    # +1 processed
    gen_row(2, '2', 2, timestamp=now()+i, vendor=str(i)) for i in range(BATCH_SIZE)]+[
    gen_row(2, '2', 2, timestamp=now()+i, price=i) for i in range(BATCH_SIZE)
] + [
    # test one batch and one line
    # +1 processed
    gen_row(3, '3', 3, timestamp=now()+i, price=i) for i in range(BATCH_SIZE+1)]+[
] + [
    # test three keys in two batches
    # +2 processed
    gen_row(4, '4', 4, timestamp=now()+i, price=i) for i in range(int(BATCH_SIZE*2/3))]+[
    gen_row(5, '5', 5, timestamp=now()+i, price=i) for i in range(int(BATCH_SIZE*2/3), int(BATCH_SIZE*4/3))]+[
    gen_row(6, '6', 6, timestamp=now()+i, price=i) for i in range(int(BATCH_SIZE*4/3), int(BATCH_SIZE*2))
] + [
    # test small key split between two batches
    # +2 processed
    gen_row(7, '7', 7, timestamp=now()+i, price=i) for i in range(BATCH_SIZE-1)]+[
    gen_row(8, '8', 8, timestamp=now()+i, price=i, vendor=str(i)) for i in range(BATCH_SIZE-1, BATCH_SIZE+1)]+[
    gen_row(9, '9', 9, timestamp=now()+i, price=i) for i in range(BATCH_SIZE*2)
] + [
    # test rows count less than batch size
    # +1 processed
    gen_row(10, '10', 10, timestamp=now()+i, price=i) for i in [10]]+[
    gen_row(11, '11', 11, timestamp=now()+i, price=i, vendor=str(i)) for i in [11]]+[
    gen_row(12, '12', 12, timestamp=now()+i, price=i) for i in [12]]+[
    gen_row(11, '11', 11, timestamp=now()+i, vendor=str(i)) for i in [1111]
]


@pytest.fixture(scope='module')
def piper_queue_dump_table_data(color):
    return ROWS


@pytest.fixture(scope='module')
def basic_offers_table_data(color):
    offers = set()
    for row in ROWS:
        offer = PQDS.UnitedOffer()
        offer.ParseFromString(row['offer'])
        offers.add((
            offer.basic.identifiers.business_id,
            offer.basic.identifiers.offer_id
        ))

    return [
        offer_to_basic_row(
            DTC.Offer(
                identifiers=DTC.OfferIdentifiers(
                    business_id=business_id,
                    offer_id=offer_id,
                ),
            )
        )
        for business_id, offer_id in offers
    ]


@pytest.fixture(scope='module')
def service_offers_table_data(color):
    offers = set()
    for row in ROWS:
        offer = PQDS.UnitedOffer()
        offer.ParseFromString(row['offer'])
        offers.add((
            list(offer.service.values())[0].identifiers.business_id,
            list(offer.service.values())[0].identifiers.offer_id,
            list(offer.service.values())[0].identifiers.shop_id
        ))

    return [
        offer_to_service_row(
            DTC.Offer(
                identifiers=DTC.OfferIdentifiers(
                    business_id=business_id,
                    shop_id=shop_id,
                    offer_id=offer_id,
                ),
            )
        )
        for business_id, offer_id, shop_id in offers
    ]


@pytest.fixture(scope='module')
def scanner(
    log_broker_stuff,
    yt_server,
    scanner_resources,
    color,
):
    with make_scanner(
        yt_server,
        log_broker_stuff,
        color,
        shopsdat_cacher=True,
        **scanner_resources
    ) as scanner_env:
        wait_until(lambda: scanner_env.united_offers_processed == len(ROWS))
        yield scanner_env


def test_one_batch(scanner, color):
    # basic
    assert_that(scanner.basic_offers_table.data, HasOffers([basic_offer(1, '1')]))
    # sevice
    assert_that(scanner.service_offers_table.data, not_(HasOffers([service_offer(1, '1', 1, price=i) for i in range(BATCH_SIZE-1)])))
    assert_that(scanner.service_offers_table.data, HasOffers([service_offer(1, '1', 1, price=BATCH_SIZE-1)]))


def test_two_batches(scanner, color):
    # basic
    assert_that(scanner.basic_offers_table.data, not_(HasOffers([basic_offer(2, '2', vendor=str(i)) for i in range(BATCH_SIZE-1)])))
    assert_that(scanner.basic_offers_table.data, HasOffers([basic_offer(2, '2', vendor=str(BATCH_SIZE-1))]))
    # sevice
    assert_that(scanner.service_offers_table.data, not_(HasOffers([service_offer(2, '2', 2, price=i) for i in range(BATCH_SIZE-1)])))
    assert_that(scanner.service_offers_table.data, HasOffers([service_offer(2, '2', 2, price=BATCH_SIZE-1)]))


def test_one_batch_and_one_line(scanner, color):
    # basic
    assert_that(scanner.basic_offers_table.data, HasOffers([basic_offer(3, '3')]))
    # sevice
    assert_that(scanner.service_offers_table.data, not_(HasOffers([service_offer(3, '3', 3, price=i) for i in range(BATCH_SIZE)])))
    assert_that(scanner.service_offers_table.data, HasOffers([service_offer(3, '3', 3, price=BATCH_SIZE)]))


def test_three_keys_int_two_batches(scanner, color):
    # basic
    assert_that(scanner.basic_offers_table.data, HasOffers([basic_offer(4, '4')]))
    assert_that(scanner.basic_offers_table.data, HasOffers([basic_offer(5, '5')]))
    assert_that(scanner.basic_offers_table.data, HasOffers([basic_offer(6, '6')]))
    # sevice
    assert_that(scanner.service_offers_table.data, not_(HasOffers([service_offer(4, '4', 4, price=i) for i in range(int(BATCH_SIZE*2/3)-1)])))
    assert_that(scanner.service_offers_table.data, HasOffers([service_offer(4, '4', 4, price=int(BATCH_SIZE*2/3)-1)]))
    assert_that(scanner.service_offers_table.data, not_(HasOffers([service_offer(5, '5', 5, price=i) for i in range(int(BATCH_SIZE*2/3), int(BATCH_SIZE*4/3)-1)])))
    assert_that(scanner.service_offers_table.data, HasOffers([service_offer(5, '5', 5, price=int(BATCH_SIZE*4/3)-1)]))
    assert_that(scanner.service_offers_table.data, not_(HasOffers([service_offer(6, '6', 6, price=i) for i in range(int(BATCH_SIZE*4/3), int(BATCH_SIZE*2)-1)])))
    assert_that(scanner.service_offers_table.data, HasOffers([service_offer(6, '6', 6, price=int(BATCH_SIZE*2)-1)]))


def test_small_key_split_between_two_batches(scanner, color):
    # basic
    assert_that(scanner.basic_offers_table.data, HasOffers([basic_offer(7, '7')]))
    assert_that(scanner.basic_offers_table.data, not_(HasOffers([basic_offer(8, '8', vendor=str(i)) for i in range(BATCH_SIZE-1)])))
    assert_that(scanner.basic_offers_table.data, HasOffers([basic_offer(8, '8', vendor=str(BATCH_SIZE))]))
    assert_that(scanner.basic_offers_table.data, HasOffers([basic_offer(9, '9')]))
    # sevice
    assert_that(scanner.service_offers_table.data, not_(HasOffers([service_offer(7, '7', 7, price=i) for i in range(BATCH_SIZE-2)])))
    assert_that(scanner.service_offers_table.data, HasOffers([service_offer(7, '7', 7, price=BATCH_SIZE-2)]))
    assert_that(scanner.service_offers_table.data, HasOffers([service_offer(8, '8', 8, price=BATCH_SIZE)]))
    assert_that(scanner.service_offers_table.data, not_(HasOffers([service_offer(9, '9', 9, price=i) for i in range(BATCH_SIZE+1, BATCH_SIZE*2-1)])))
    assert_that(scanner.service_offers_table.data, HasOffers([service_offer(9, '9', 9, price=BATCH_SIZE*2-1)]))


def test_rows_count_less_than_batch_size(scanner, color):
    # basic
    assert_that(scanner.basic_offers_table.data, HasOffers([basic_offer(10, '10')]))
    assert_that(scanner.basic_offers_table.data, not_(HasOffers([basic_offer(11, '11', vendor=str(11))])))
    assert_that(scanner.basic_offers_table.data, HasOffers([basic_offer(11, '11', vendor=str(1111))]))
    assert_that(scanner.basic_offers_table.data, HasOffers([basic_offer(12, '12')]))
    # sevice
    assert_that(scanner.service_offers_table.data, HasOffers([service_offer(10, '10', 10, price=10)]))
    assert_that(scanner.service_offers_table.data, HasOffers([service_offer(11, '11', 11, price=11)]))
    assert_that(scanner.service_offers_table.data, HasOffers([service_offer(12, '12', 12, price=12)]))


def test_processed_count(scanner, color):
    url = 'http://localhost:{port}?command=get_info_server'.format(port=scanner.controller_port)
    response = requests.get(url).json()
    processed = int(response['result']['processors']['UnitedOffersUpdater']['processed_count'])
    assert_that(processed, equal_to(7))
