# coding: utf-8

from datetime import datetime
import pytest

from hamcrest import assert_that, equal_to
from market.idx.datacamp.controllers.scanner.yatf.test_env import make_scanner
from market.idx.pylibrary.datacamp.utils import wait_until
from market.idx.pylibrary.datacamp.conversion import offer_to_service_row
from market.idx.datacamp.yatf.utils import dict2tskv, create_meta
from market.idx.yatf.matchers.protobuf_matchers import IsSerializedProtobuf, IsProtobufMap
from market.idx.yatf.matchers.yt_rows_matchers import HasOffers, HasDatacampMskuRows
from market.idx.datacamp.proto.models.MarketSku_pb2 import ModelStatus
import market.idx.datacamp.proto.offer.DataCampOffer_pb2 as DTC
from market.pylibrary.proto_utils import message_from_data

time_pattern = "%Y-%m-%dT%H:%M:%SZ"

BUSINESS_ID = 1
SHOP_ID = 2
SHOP_ID_NOT_IN_SHOPSDATA = 3
WAREHOUSE_ID = 145


@pytest.fixture(scope='module')
def partners_table_data():
    return [{
        'shop_id': SHOP_ID,
        'mbi': '\n\n'.join([
            dict2tskv({
                'shop_id': SHOP_ID,
                'business_id': BUSINESS_ID,
                'warehouse_id': WAREHOUSE_ID,
            }),
        ]),
        'status': 'publish'
    }]


@pytest.fixture(scope='module')
def abo_hidings_table_data():
    return [
        {
            # активное скрытие shop_id + shop_sku
            'shop_id': SHOP_ID,
            'shop_sku': 'o1',
            'is_deleted': False,
            'reason': 'MANUALLY_HIDDEN',
            'timestamp': 1535096546123,
        },
        {
            # снятое скрытие shop_id + shop_sku
            'shop_id': SHOP_ID,
            'shop_sku': 'o2',
            'is_deleted': True,
            'reason': 'MISSING_ITEM',
            'timestamp': 100,
        },
        {
            # активное скрытие shop_id + shop_sku, в шопсдате нет business_id, но есть в таблице
            'business_id': BUSINESS_ID,
            'shop_id': SHOP_ID_NOT_IN_SHOPSDATA,
            'shop_sku': 'o1',
            'is_deleted': False,
            'reason': 'MANUALLY_HIDDEN',
            'timestamp': 100,
        },
        {
            # активное скрытие market_sku
            'market_sku': 1234,
            'is_deleted': False,
            'reason': 'WRONG_SKU_MAPPING',
            'timestamp': 100,
        },
        {
            # снятое скрытие market_sku
            'market_sku': 5678,
            'is_deleted': True,
            'reason': 'LEGAL',
            'timestamp': 100,
        },
        {
            # активное скрытие market_sku + shop_id
            'market_sku': 1234,
            'shop_id': SHOP_ID,
            'is_deleted': False,
            'reason': 'OTHER',
            'timestamp': 100,
        },
        {
            # снятое скрытие market_sku + shop_id
            'market_sku': 5678,
            'shop_id': SHOP_ID,
            'is_deleted': True,
            'reason': 'CANCELLED_ORDER',
            'timestamp': 100,
        },
        {
            # активное скрытие market_sku + shop_id + shop_sku
            # aka shop_id + shop_sku
            'market_sku': 1011,
            'shop_id': SHOP_ID,
            'shop_sku': 'o3',
            'is_deleted': False,
            'reason': 'MANUALLY_HIDDEN',
            'timestamp': 100,
        },
        {
            # невалидное скрытие
            'market_sku': 1213,
            'shop_sku': 'o3',
            'is_deleted': False,
            'reason': 'MISSING_ITEM',
            'timestamp': 100,
        },
        {
            # активное скрытие shop_id + shop_sku, заполнен вердикт
            'business_id': BUSINESS_ID,
            'shop_id': SHOP_ID,
            'shop_sku': 'o4',
            'is_deleted': False,
            'reason': 'MANUALLY_HIDDEN',
            'timestamp': 100,
            'verdict': DTC.Verdict(
                results=[DTC.ValidationResult(
                    is_banned=True,
                    abo_reason=DTC.MANUALLY_HIDDEN,
                    messages=[DTC.Explanation(
                        namespace='ABO_WHITE',
                        level=DTC.Explanation.ERROR,
                        code='BAD_QUALITY_129',
                        params=[DTC.Explanation.Param(
                            name='assessor_comment',
                            value='very bad quality'
                        )]
                    )]
                )]
            ).SerializeToString()
        },
        {
            # активное скрытие market_sku, заполнен вердикт
            'market_sku': 91011,
            'is_deleted': False,
            'reason': 'WRONG_SKU_MAPPING',
            'timestamp': 100,
            'verdict': DTC.Verdict(
                results=[DTC.ValidationResult(
                    is_banned=True,
                    abo_reason=DTC.WRONG_SKU_MAPPING,
                )]
            ).SerializeToString()
        },
    ]


@pytest.fixture(scope='module')
def service_offers_table_data():
    return [
        offer_to_service_row(
            DTC.Offer(
                identifiers=DTC.OfferIdentifiers(
                    business_id=business_id,
                    shop_id=shop_id,
                    offer_id=offer_id,
                ),
                meta=create_meta(10, DTC.BLUE)
            )
        )
        for business_id, shop_id, offer_id in [
            (BUSINESS_ID, SHOP_ID, 'o1'),
            (BUSINESS_ID, SHOP_ID, 'o2'),
            (BUSINESS_ID, SHOP_ID, 'o3'),
            (BUSINESS_ID, SHOP_ID, 'o4'),
            (BUSINESS_ID, SHOP_ID_NOT_IN_SHOPSDATA, 'o1'),
        ]
    ]


@pytest.fixture(scope='module')
def scanner(
    log_broker_stuff,
    yt_server,
    scanner_resources,
    color,
):
    with make_scanner(yt_server, log_broker_stuff, color, shopsdat_cacher=True, **scanner_resources) as scanner_env:
        yield scanner_env


def test_update_abo_hidings(scanner, datacamp_msku_table):
    # проверяем, что сканнер вычитывает скрытия abo из таблицы
    # и записывает в офферную таблицу пооферные скрытия (shop_id + shop_sku),
    # в msku-шную таблицу msku-шные скрытия (msku, msku + shop_id)
    wait_until(lambda: scanner.united_offers_processed > 0, timeout=10)
    assert_that(scanner.united_offers_processed, equal_to(5))

    assert_that(len(scanner.service_offers_table.data), equal_to(5))
    assert_that(scanner.service_offers_table.data, HasOffers([
                message_from_data({
                    'identifiers': {
                        'business_id': BUSINESS_ID,
                        'shop_id': SHOP_ID,
                        'offer_id': 'o1',
                    },
                    'status': {
                        'disabled': [{
                            'flag': True,
                            'meta': {
                                'source': DTC.MARKET_ABO_SHOP_SKU,
                                'timestamp': datetime.utcfromtimestamp(1535096546).strftime(time_pattern)
                            }
                        }]
                    },
                    'resolution': {
                        'by_source': [{
                            'verdict': [{
                                'results': [{
                                    'is_banned': True,
                                    'abo_reason': DTC.MANUALLY_HIDDEN,
                                    'messages': [{
                                        'namespace': 'ABO',
                                        'level': DTC.Explanation.ERROR,
                                        'code': "MANUALLY_HIDDEN"
                                    }]
                                }]
                            }],
                            'meta': {
                                'source': DTC.MARKET_ABO_SHOP_SKU
                            }
                        }]
                    }
                }, DTC.Offer()), message_from_data({
                    'identifiers': {
                        'business_id': BUSINESS_ID,
                        'shop_id': SHOP_ID,
                        'offer_id': 'o2',
                    },
                    'status': {
                        'disabled': [{
                            'flag': False,
                            'meta': {
                                'source': DTC.MARKET_ABO_SHOP_SKU
                            }
                        }]
                    },
                    'resolution': None,
                }, DTC.Offer()), message_from_data({
                    'identifiers': {
                        'business_id': BUSINESS_ID,
                        'shop_id': SHOP_ID,
                        'offer_id': 'o3',
                    },
                    'status': {
                        'disabled': [{
                            'flag': True,
                            'meta': {
                                'source': DTC.MARKET_ABO_SHOP_SKU
                            }
                        }]
                    },
                    'resolution': {
                        'by_source': [{
                            'verdict': [{
                                'results': [{
                                    'is_banned': True,
                                    'abo_reason': DTC.MANUALLY_HIDDEN
                                }]
                            }],
                            'meta': {
                                'source': DTC.MARKET_ABO_SHOP_SKU
                            }
                        }]
                    }
                }, DTC.Offer()), message_from_data({
                    'identifiers': {
                        'business_id': BUSINESS_ID,
                        'shop_id': SHOP_ID_NOT_IN_SHOPSDATA,
                        'offer_id': 'o1',
                    },
                    'status': {
                        'disabled': [{
                            'flag': True,
                            'meta': {
                                'source': DTC.MARKET_ABO_SHOP_SKU
                            }
                        }]
                    },
                }, DTC.Offer()), message_from_data({
                    'identifiers': {
                        'business_id': BUSINESS_ID,
                        'shop_id': SHOP_ID,
                        'offer_id': 'o4',
                    },
                    'status': {
                        'disabled': [{
                            'flag': True,
                            'meta': {
                                'source': DTC.MARKET_ABO_SHOP_SKU,
                            }
                        }]
                    },
                    'resolution': {
                        'by_source': [{
                            'verdict': [{
                                'results': [{
                                    'is_banned': True,
                                    'abo_reason': DTC.MANUALLY_HIDDEN,
                                    'messages': [{
                                        'namespace': 'ABO_WHITE',
                                        'level': DTC.Explanation.ERROR,
                                        'code': 'BAD_QUALITY_129',
                                        'params': [{
                                            'name': 'assessor_comment',
                                            'value': 'very bad quality'
                                        }]
                                    }]
                                }]
                            }],
                            'meta': {
                                'source': DTC.MARKET_ABO_SHOP_SKU
                            }
                        }]
                    }
                }, DTC.Offer())]))

    wait_until(lambda: scanner.mskus_processed > 0, timeout=10)
    datacamp_msku_table.load()
    assert_that(len(datacamp_msku_table.data), equal_to(3))
    assert_that(datacamp_msku_table.data, HasDatacampMskuRows([
                {
                    'id': 1234,
                    'status': IsSerializedProtobuf(ModelStatus, {
                        'abo_status': {
                            'meta': {
                                'source': DTC.MARKET_ABO_MSKU
                            },
                            'reason': DTC.WRONG_SKU_MAPPING,
                            'verdict': None,
                        },
                        'abo_shop_status': IsProtobufMap({
                            SHOP_ID: {
                                'meta': {
                                    'source': DTC.MARKET_ABO_MSKU_SHOP
                                },
                                'reason': DTC.UNKNOWN_HREASON
                            }
                        })
                    }),
                }, {
                    'id': 5678,
                    'status': IsSerializedProtobuf(ModelStatus, {
                        'abo_status': {
                            'meta': {
                                'source': DTC.MARKET_ABO_MSKU
                            },
                            'reason': None
                        },
                        'abo_shop_status': IsProtobufMap({
                            SHOP_ID: {
                                'meta': {
                                    'source': DTC.MARKET_ABO_MSKU_SHOP
                                },
                                'reason': None
                            }
                        })
                    }),
                }, {
                    'id': 91011,
                    'status': IsSerializedProtobuf(ModelStatus, {
                        'abo_status': {
                            'meta': {
                                'source': DTC.MARKET_ABO_MSKU
                            },
                            'reason': DTC.WRONG_SKU_MAPPING,
                            'verdict': {
                                'results': [{
                                    'is_banned': True,
                                    'abo_reason': DTC.WRONG_SKU_MAPPING,
                                }]
                            },
                        },
                    }),
                }]))
