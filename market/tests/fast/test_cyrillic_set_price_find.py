# coding: utf-8

import pytest
from hamcrest import assert_that
from six.moves.urllib.parse import quote

from market.idx.datacamp.controllers.stroller.yatf.test_env import make_stroller
from market.idx.datacamp.controllers.stroller.yatf.utils import gen_service_row
from market.idx.datacamp.proto.api.SyncChangeOffer_pb2 import ChangeOfferRequest, FullOfferResponse
from market.idx.datacamp.proto.offer import DataCampOffer_pb2
from market.idx.datacamp.yatf.matchers.matchers import HasStatus
from market.idx.datacamp.yatf.utils import dict2tskv
from market.idx.yatf.matchers.protobuf_matchers import IsSerializedProtobuf
from market.idx.yatf.matchers.yt_rows_matchers import HasOffers
from market.idx.yatf.resources.datacamp.datacamp_tables import DataCampServiceOffersTable, DataCampPartnersTable
from market.proto.common.common_pb2 import PriceExpression
from market.pylibrary.proto_utils import message_from_data


BUSINESS_ID = 1000
SHOP_ID = 1
WAREHOUSE_ID = 100


SERVICE_OFFERS = [
    gen_service_row(BUSINESS_ID, SHOP_ID, 'T1000', 10),
    gen_service_row(BUSINESS_ID, SHOP_ID, 'Тэ-четыреста', 10),
    gen_service_row(BUSINESS_ID, SHOP_ID, 'Тэ-пятьсот', 10),
    gen_service_row(BUSINESS_ID, SHOP_ID, 'Тэ/со/слешом', 10),
    gen_service_row(BUSINESS_ID, SHOP_ID, '/begin-slash', 10),
    gen_service_row(BUSINESS_ID, SHOP_ID, 'end-slash/', 10),
    gen_service_row(BUSINESS_ID, SHOP_ID, 'русскийоффер', 10),
]


SHOPS = [
    {
        'shop_id': SHOP_ID,
        'mbi': '\n\n'.join([
            dict2tskv({
                'shop_id': SHOP_ID,
                'warehouse_id': WAREHOUSE_ID,
                'datafeed_id': 100,
                'business_id': BUSINESS_ID,
                'is_discounts_enabled': 'true',
            }),
        ]),
    }
]


@pytest.fixture(scope='module')
def service_offers_table(yt_server, config):
    return DataCampServiceOffersTable(
        yt_server,
        config.yt_service_offers_tablepath,
        data=SERVICE_OFFERS
    )


@pytest.fixture(scope='module')
def partners_table(yt_server, config):
    return DataCampPartnersTable(
        yt_server,
        config.yt_partners_tablepath,
        data=SHOPS
    )


@pytest.yield_fixture(scope='module')
def stroller(
        config,
        yt_server,
        log_broker_stuff,
        partners_table,
        service_offers_table,
):
    with make_stroller(
            config,
            yt_server,
            log_broker_stuff,
            shopsdat_cacher=True,
            partners_table=partners_table,
            service_offers_table=service_offers_table,
    ) as stroller_env:
        yield stroller_env


def request_offer(client, shop_id, offer_id):
    response = client.get('/shops/{}/offers?offer_id={}'.format(shop_id, offer_id))
    return response


def do_request_get_offers(client, offer_id, shop_id=SHOP_ID, warehouse_id=WAREHOUSE_ID):
    return client.get('/shops/{}/offers?offer_id={}&warehouse_id={}'.format(shop_id, offer_id, warehouse_id))


def do_request_set_price(client, offer_id, data, shop_id=SHOP_ID, warehouse_id=WAREHOUSE_ID):
    return client.put('/shops/{}/offers/price?offer_id={}&warehouse_id={}'.format(shop_id, offer_id, warehouse_id), data=data.SerializeToString())


def request_set_price(client, offer_id, source, ts, price):
    body = ChangeOfferRequest()
    offer = body.offer.add()
    offer.price.basic.meta.timestamp.FromSeconds(ts)
    offer.price.basic.meta.source = source
    offer.price.basic.binary_price.CopyFrom(PriceExpression(price=price))

    response = do_request_set_price(client, offer_id=offer_id, data=body)
    return response


def test_get_cyrillic_offer_id(stroller):
    """Тест проверяет, что офера, с русскими offer_id находятся, если в урле передать русские буквы"""
    offer_id = 'Тэ-четыреста'

    response = do_request_get_offers(stroller, offer_id=offer_id)
    assert_that(response, HasStatus(200))
    assert_that(response.data, IsSerializedProtobuf(FullOfferResponse, {
        'offer': [
            {
                'identifiers': {
                    'shop_id': SHOP_ID,
                    'offer_id': 'Тэ-четыреста',
                    'warehouse_id': WAREHOUSE_ID,
                },
            },
        ],
    }))


def test_get_encoded_cyrillic_offer_id(stroller):
    """Тест проверяет, что офера, с русскими offer_id находятся, если в урле передать закодированные русские буквы"""
    offer_id = quote('Тэ-пятьсот')  # '%D0%A2%D1%8D-%D0%BF%D1%8F%D1%82%D1%8C%D1%81%D0%BE%D1%82'

    response = do_request_get_offers(stroller, offer_id=offer_id)
    assert_that(response, HasStatus(200))
    assert_that(response.data, IsSerializedProtobuf(FullOfferResponse, {
        'offer': [
            {
                'identifiers': {
                    'shop_id': SHOP_ID,
                    'offer_id': 'Тэ-пятьсот',
                    'warehouse_id': WAREHOUSE_ID,
                },
            },
        ],
    }))


@pytest.mark.skip('TODO: error in shiny stroller')
def test_set_cyrillic_offer_id(stroller):
    """Тест проверяет выставлении цены новому оферу с русским offer_id"""
    source = DataCampOffer_pb2.PUSH_PARTNER_API
    timestamp = 1000
    offer_id = 'Тэ-тыща'
    encoded_offer_id = quote('Тэ-тыща')  # '%D0%A2%D1%8D-%D1%82%D1%8B%D1%89%D0%B0'
    price = 100

    response = request_set_price(stroller, offer_id=offer_id, price=price, source=source, ts=timestamp)
    assert_that(response, HasStatus(200))
    assert_that(response.data, IsSerializedProtobuf(FullOfferResponse, {
        'offer': [{
            'identifiers': {
                'shop_id': SHOP_ID,
                'offer_id': offer_id,
            },
            'price': {
                'basic': {
                    'meta': {
                        'timestamp': {
                            'seconds': timestamp,
                        },
                        'source': source,
                    },
                    'binary_price': {
                        'price': price,
                    }
                },
            }
        }]}))

    response = do_request_get_offers(stroller, offer_id=offer_id)
    assert_that(response, HasStatus(200))
    assert_that(response.data, IsSerializedProtobuf(FullOfferResponse, {
        'offer': [
            {
                'identifiers': {
                    'shop_id': SHOP_ID,
                    'offer_id': offer_id,
                    'warehouse_id': WAREHOUSE_ID,
                },
            },
        ],
    }))

    encoded_response = do_request_get_offers(stroller, offer_id=encoded_offer_id)
    assert_that(encoded_response, HasStatus(200))
    assert_that(encoded_response.data, IsSerializedProtobuf(FullOfferResponse, {
        'offer': [
            {
                'identifiers': {
                    'shop_id': SHOP_ID,
                    'offer_id': offer_id,
                    'warehouse_id': WAREHOUSE_ID,
                },
            },
        ],
    }))


@pytest.mark.skip('TODO: error in shiny stroller')
def test_set_encoded_cyrillic_offer_id(stroller):
    """Тест проверяет выставлении цены новому оферу с русским offer_id передавая закодированный offer_id"""
    source = DataCampOffer_pb2.PUSH_PARTNER_API
    timestamp = 1000
    offer_id = 'Тэ-две-тыщи'
    encoded_offer_id = quote('Тэ-две-тыщи')  # '%D0%A2%D1%8D-%D0%B4%D0%B2%D0%B5-%D1%82%D1%8B%D1%89%D0%B8'
    price = 100

    response = request_set_price(stroller, offer_id=encoded_offer_id, price=price, source=source, ts=timestamp)
    assert_that(response, HasStatus(200))
    assert_that(response.data, IsSerializedProtobuf(FullOfferResponse, {
        'offer': [{
            'identifiers': {
                'shop_id': SHOP_ID,
                'offer_id': offer_id,
            },
            'price': {
                'basic': {
                    'meta': {
                        'timestamp': {
                            'seconds': timestamp,
                        },
                        'source': source,
                    },
                    'binary_price': {
                        'price': price,
                    }
                },
            }
        }]}))

    response = do_request_get_offers(stroller, offer_id=offer_id)
    assert_that(response, HasStatus(200))
    assert_that(response.data, IsSerializedProtobuf(FullOfferResponse, {
        'offer': [
            {
                'identifiers': {
                    'shop_id': SHOP_ID,
                    'offer_id': offer_id,
                    'warehouse_id': WAREHOUSE_ID,
                },
            },
        ],
    }))

    encoded_response = do_request_get_offers(stroller, offer_id=encoded_offer_id)
    assert_that(encoded_response, HasStatus(200))
    assert_that(encoded_response.data, IsSerializedProtobuf(FullOfferResponse, {
        'offer': [
            {
                'identifiers': {
                    'shop_id': SHOP_ID,
                    'offer_id': offer_id,
                    'warehouse_id': WAREHOUSE_ID,
                },
            },
        ],
    }))


def test_get_offer_id_with_slashes(stroller):
    """Тест проверяет, что офера, с offer_id со слешом находятся"""
    offer_id = quote('Тэ/со/слешом', safe='')  # '%D0%A2%D1%8D%2F%D1%81%D0%BE%2F%D1%81%D0%BB%D0%B5%D1%88%D0%BE%D0%BC'

    response = do_request_get_offers(stroller, offer_id=offer_id)
    assert_that(response, HasStatus(200))
    assert_that(response.data, IsSerializedProtobuf(FullOfferResponse, {
        'offer': [
            {
                'identifiers': {
                    'shop_id': SHOP_ID,
                    'offer_id': 'Тэ/со/слешом',
                    'warehouse_id': WAREHOUSE_ID,
                },
            },
        ],
    }))


def test_get_offer_id_with_begin_slashes(stroller):
    """Тест проверяет, что офера, с offer_id со слешом находятся"""
    offer_id = quote('/begin-slash', safe='')  # '%2Fbegin-slash'

    response = do_request_get_offers(stroller, offer_id=offer_id)
    assert_that(response, HasStatus(200))
    assert_that(response.data, IsSerializedProtobuf(FullOfferResponse, {
        'offer': [
            {
                'identifiers': {
                    'shop_id': SHOP_ID,
                    'offer_id': '/begin-slash',
                    'warehouse_id': WAREHOUSE_ID,
                },
            },
        ],
    }))


def test_get_offer_id_with_end_slashes(stroller):
    """Тест проверяет, что офера, с offer_id со слешом находятся"""
    offer_id = quote('end-slash/', safe='')  # 'end-slash%2F'

    response = do_request_get_offers(stroller, offer_id=offer_id)
    assert_that(response, HasStatus(200))
    assert_that(response.data, IsSerializedProtobuf(FullOfferResponse, {
        'offer': [
            {
                'identifiers': {
                    'shop_id': SHOP_ID,
                    'offer_id': 'end-slash/',
                    'warehouse_id': WAREHOUSE_ID,
                },
            },
        ],
    }))


def test_update_service_of_russian_offer(stroller, service_offers_table):
    """
    Устанавливаем цену сервисной части для оффера с русскими буквами в shopsku
    """
    offer_id = 'русскийоффер'
    offer_id_encoded = quote(offer_id, safe='')

    assert_that(
        stroller.service_offers_table.data,
        HasOffers([
            message_from_data({
                'identifiers': {
                    'business_id': BUSINESS_ID,
                    'shop_id': SHOP_ID,
                    'offer_id': offer_id,
                },
            }, DataCampOffer_pb2.Offer())
        ]),
    )

    response = stroller.post(
        '/v1/partners/{}/offers/services/{}?offer_id={}'.format(BUSINESS_ID, SHOP_ID, offer_id_encoded),
        data=message_from_data({
            'identifiers': {
                'business_id': BUSINESS_ID,
                'shop_id': SHOP_ID,
                'offer_id': offer_id,
            },
            'price': {
                'basic': {
                    'meta': {
                        'timestamp': '2020-08-16T17:25:55Z',
                    },
                    'binary_price': {
                        'price': 100,
                    }
                },
            }
        }, DataCampOffer_pb2.Offer()).SerializeToString()
    )
    assert_that(response, HasStatus(200))
    assert_that(response.data, IsSerializedProtobuf(DataCampOffer_pb2.Offer, {
        'identifiers': {
            'business_id': BUSINESS_ID,
            'shop_id': SHOP_ID,
            'offer_id': offer_id,
        },
        'price': {
            'basic': {
                'binary_price': {
                    'price': 100,
                }
            },
        }
    }))

    assert_that(
        stroller.service_offers_table.data,
        HasOffers([
            message_from_data({
                'identifiers': {
                    'business_id': BUSINESS_ID,
                    'shop_id': SHOP_ID,
                    'offer_id': offer_id,
                },
                'price': {
                    'basic': {
                        'binary_price': {
                            'price': 100,
                        }
                    },
                }
            }, DataCampOffer_pb2.Offer())
        ]),
    )
