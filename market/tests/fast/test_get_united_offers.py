# coding: utf-8

import json
import logging
import pytest

from datetime import datetime
from hamcrest import assert_that, equal_to, contains_string, all_of, not_, is_
from itertools import product
from six.moves.urllib.parse import quote

import market.idx.datacamp.proto.api.OffersBatch_pb2 as OffersBatch
import market.idx.datacamp.proto.api.SyncGetOffer_pb2 as SyncGetOffer
import market.idx.datacamp.proto.offer.DataCampOffer_pb2 as DTC

from market.idx.datacamp.controllers.stroller.yatf.test_env import make_stroller
from market.idx.pylibrary.datacamp.conversion import offer_to_basic_row, offer_to_service_row
from market.idx.datacamp.yatf.matchers.matchers import HasStatus
from market.idx.datacamp.yatf.utils import create_meta, create_update_meta
from market.idx.yatf.matchers.protobuf_matchers import IsSerializedProtobuf, IsProtobufMap


log = logging.getLogger(__name__)

NOW_UTC = datetime.utcnow()  # JSON serializer should always use UTC
time_pattern = "%Y-%m-%dT%H:%M:%SZ"

BUSINESS_ID = 1
OFFER_ID = 'o1'
OFFER_ID_WITH_SPECIAL_SYMBOLS = 'ЧудоOffer123.,/\\/()[]-="'
OFFER_IDS = (OFFER_ID, OFFER_ID_WITH_SPECIAL_SYMBOLS)

SHOP_ID = 2
SHOP_ID_SECOND = 22222
SHOP_IDS = (SHOP_ID, SHOP_ID_SECOND)
BASIC_OFFERS = [
    offer_to_basic_row(DTC.Offer(
        identifiers=DTC.OfferIdentifiers(
            business_id=BUSINESS_ID,
            offer_id=offer_id
        ),
        price=DTC.OfferPrice(
            basic=DTC.PriceBundle(
                binary_price=DTC.PriceExpression(price=10),
                meta=create_update_meta(10)
            )),
        content=DTC.OfferContent(
            partner=DTC.PartnerContent(
                original=DTC.OriginalSpecification(
                    name=DTC.StringValue(
                        value="original name",
                        meta=create_update_meta(10)
                    ),
                ),
                actual=DTC.ProcessedSpecification(
                    title=DTC.StringValue(
                        value="actual title",
                        meta=create_update_meta(10)
                    ),
                )
            )),
        meta=create_meta(10),
    )) for offer_id in OFFER_IDS
]

SERVICE_OFFERS = [
    offer_to_service_row(DTC.Offer(
        identifiers=DTC.OfferIdentifiers(
            business_id=BUSINESS_ID,
            offer_id=offer_id,
            shop_id=shop_id,
            warehouse_id=0,
        ),
        stock_info=DTC.OfferStockInfo(
            partner_stocks=DTC.OfferStocks(
                count=1,
                meta=create_update_meta(10)
            )
        ),
        meta=create_meta(10),
        status=DTC.OfferStatus(
            publish_by_partner=DTC.AVAILABLE,
        )
    )) for offer_id, shop_id in list(product(OFFER_IDS, SHOP_IDS))
]

ACTUAL_SERVICE_OFFERS = [
    offer_to_service_row(DTC.Offer(
        identifiers=DTC.OfferIdentifiers(
            business_id=BUSINESS_ID,
            offer_id=offer_id,
            shop_id=shop_id,
            warehouse_id=0,
            extra=DTC.OfferExtraIdentifiers(
                ware_md5='waremd5'
            )
        ),
        meta=create_meta(10),
        status=DTC.OfferStatus(
            publish=DTC.HIDDEN,
        )
    )) for offer_id, shop_id in list(product(OFFER_IDS, SHOP_IDS))
]


@pytest.fixture(scope='module')
def basic_offers():
    return BASIC_OFFERS


@pytest.fixture(scope='module')
def service_offers():
    return SERVICE_OFFERS


@pytest.fixture(scope='module')
def basic_search_offers():
    return [{
        'business_id': BUSINESS_ID,
        'shop_sku': offer_id,
        'removed': False
    } for offer_id in OFFER_IDS]


@pytest.fixture(scope='module')
def service_search_offers():
    return [{
        'business_id': BUSINESS_ID,
        'shop_id': shop_id,
        'shop_sku': offer_id,
        'removed': False
    } for offer_id, shop_id in list(product(OFFER_IDS, SHOP_IDS))]


@pytest.fixture(scope='module')
def actual_service_offers():
    return ACTUAL_SERVICE_OFFERS


@pytest.yield_fixture(scope='module')
def stroller(
        config,
        yt_server,
        log_broker_stuff,
        basic_offers_table,
        service_offers_table,
        actual_service_offers_table,
        basic_search_offers_table,
        service_search_offers_table,
):
    with make_stroller(
            config,
            yt_server,
            log_broker_stuff,
            shopsdat_cacher=True,
            basic_offers_table=basic_offers_table,
            service_offers_table=service_offers_table,
            actual_service_offers_table=actual_service_offers_table,
            basic_search_offers_table=basic_search_offers_table,
            service_search_offers_table=service_search_offers_table,
    ) as stroller_env:
        yield stroller_env


@pytest.fixture(scope='module', params=OFFER_IDS, ids=['base_case', 'special_symbols'])
def offer_id(request):
    return request.param


@pytest.fixture(scope='module', params=SHOP_IDS, ids=['first_shop', 'second_shop'])
def shop_id(request):
    return request.param


def test_get_basic_offer(stroller, offer_id):
    expected_resp = {
        'offers': [
            {
                'identifiers': {
                    'business_id': BUSINESS_ID,
                    'offer_id': offer_id,
                },
                'content': {
                    'partner': {
                        'original': {
                            'name': {
                                'value': 'original name'
                            }
                        },
                        'actual': {
                            'title': {
                                'value': 'actual title'
                            }
                        }
                    }
                },
                'price': {
                    'basic': {
                        'binary_price': {
                            'price': 10
                        }
                    }
                },
            },
        ],
    }

    response = stroller.get("/v1/partners/{}/offers/basic?offer_id={}".format(BUSINESS_ID, quote(offer_id, safe='')))
    assert_that(response, HasStatus(200))
    assert_that(response.data, IsSerializedProtobuf(SyncGetOffer.GetOffersResponse, expected_resp))
    assert_that(response.headers['Content-type'], equal_to('application/x-protobuf'))


def test_get_service_offer(stroller, offer_id, shop_id):
    expected_resp = {
        'offers': [
            {
                'identifiers': {
                    'business_id': BUSINESS_ID,
                    'offer_id': offer_id,
                    'shop_id': shop_id,
                    'extra': {
                        'ware_md5': 'waremd5'
                    }
                },
            }
        ],
    }

    response = stroller.get("/v1/partners/{business_id}/offers/services/{shop_id}?offer_id={offer_id}".format(business_id=BUSINESS_ID, offer_id=quote(offer_id, safe=''), shop_id=shop_id))
    assert_that(response, HasStatus(200))
    assert_that(response.data, IsSerializedProtobuf(SyncGetOffer.GetOffersResponse, expected_resp))
    assert_that(response.headers['Content-type'], equal_to('application/x-protobuf'))


def test_get_service_offers_batch(stroller, shop_id):
    expected_resp = {
        'entries': [
            {
                'offers': [
                    {
                        'identifiers': {
                            'business_id': BUSINESS_ID,
                            'offer_id': OFFER_ID,
                            'shop_id': shop_id
                        },
                    }
                ]
            }
        ]
    }

    request = OffersBatch.OffersBatchRequest()
    entry = request.entries.add()
    entry.method = OffersBatch.RequestMethod.GET
    entry.business_id = BUSINESS_ID
    entry.offer_id = OFFER_ID
    entry.shop_id = shop_id

    response = stroller.post('/v1/offers/batch', data=request.SerializeToString())
    assert_that(response, HasStatus(200))
    assert_that(response.data, IsSerializedProtobuf(OffersBatch.OffersBatchResponse, expected_resp))
    assert_that(response.headers['Content-type'], equal_to('application/x-protobuf'))

    json_request = {
        'entries': [
            {
                'method': 'GET',
                'business_id': BUSINESS_ID,
                'offer_id': OFFER_ID,
                'shop_id': shop_id,
            }
        ]
    }
    json_response = stroller.post('/v1/offers/batch?format=json', data=json.dumps(json_request), headers={'Content-Type': 'application/json'})
    assert_that(json_response.headers['Content-type'], contains_string('application/json'))
    assert_that(json_response.json()['entries'][0]['offers'][0]['identifiers']['shop_id'], equal_to(shop_id))


@pytest.mark.parametrize('filter_mode', [None, 'shop_id'])
def test_get_united_offers_batch(stroller, filter_mode):
    shop_matcher = not_ if filter_mode == 'shop_id' else is_
    expected_resp = {
        'entries': [
            {
                'united_offer': {
                    'basic': {
                        'identifiers': {
                            'business_id': BUSINESS_ID,
                            'offer_id': offer_id
                        },
                    },
                    'service': all_of(
                        IsProtobufMap({
                            shop_id: {
                                'identifiers': {
                                    'business_id': BUSINESS_ID,
                                    'offer_id': offer_id,
                                    'shop_id': shop_id,
                                }
                            } for shop_id in [SHOP_ID_SECOND]
                        }),
                        shop_matcher(IsProtobufMap({
                            shop_id: {
                                'identifiers': {
                                    'business_id': BUSINESS_ID,
                                    'offer_id': offer_id,
                                    'shop_id': shop_id,
                                }
                            } for shop_id in [SHOP_ID]
                        }))
                    ),
                    'actual': all_of(
                        IsProtobufMap({
                            shop_id: {
                                'warehouse': IsProtobufMap({
                                    0: {
                                        'identifiers': {
                                            'business_id': BUSINESS_ID,
                                            'offer_id': offer_id,
                                            'shop_id': shop_id,
                                        }
                                    }
                                })
                            } for shop_id in [SHOP_ID_SECOND]
                        }),
                        shop_matcher(IsProtobufMap({
                            shop_id: {
                                'warehouse': IsProtobufMap({
                                    0: {
                                        'identifiers': {
                                            'business_id': BUSINESS_ID,
                                            'offer_id': offer_id,
                                            'shop_id': shop_id,
                                        }
                                    }
                                })
                            } for shop_id in [SHOP_ID]
                        }))
                    )
                } for offer_id in OFFER_IDS
            }
        ]
    }

    request = OffersBatch.UnitedOffersBatchRequest()
    for offer_id in OFFER_IDS:
        entry = request.entries.add()
        entry.method = OffersBatch.RequestMethod.GET
        entry.business_id = BUSINESS_ID
        entry.offer_id = offer_id
        if filter_mode == 'shop_id':
            entry.shop_id = SHOP_ID_SECOND

    response = stroller.post('/v1/offers/united/batch', data=request.SerializeToString())
    assert_that(response, HasStatus(200))
    assert_that(response.data, IsSerializedProtobuf(OffersBatch.UnitedOffersBatchResponse, expected_resp))
    assert_that(response.headers['Content-type'], equal_to('application/x-protobuf'))


@pytest.mark.parametrize('filter_mode', [None, 'shop_id'])
def test_get_united_offers_batch_with_merge(stroller, filter_mode):
    shop_matcher = not_ if filter_mode == 'shop_id' else is_
    expected_resp = {
        'entries': [
            {
                'united_offer': {
                    'basic': {
                        'identifiers': {
                            'business_id': BUSINESS_ID,
                            'offer_id': offer_id
                        },
                    },
                    'service': all_of(
                        IsProtobufMap({
                            shop_id: {
                                'identifiers': {
                                    'business_id': BUSINESS_ID,
                                    'offer_id': offer_id,
                                    'shop_id': shop_id,
                                },
                                'status': {
                                    'publish': DTC.HIDDEN,
                                    'publish_by_partner': DTC.AVAILABLE,
                                },
                            } for shop_id in [SHOP_ID_SECOND]
                        }),
                        shop_matcher(IsProtobufMap({
                            shop_id: {
                                'identifiers': {
                                    'business_id': BUSINESS_ID,
                                    'offer_id': offer_id,
                                    'shop_id': shop_id,
                                },
                                'status': {
                                    'publish': DTC.HIDDEN,
                                    'publish_by_partner': DTC.AVAILABLE,
                                },
                            } for shop_id in [SHOP_ID]
                        }))
                    ),
                    'actual': all_of(
                        IsProtobufMap({
                            shop_id: {
                                'warehouse': IsProtobufMap({
                                    0: {
                                        'identifiers': {
                                            'business_id': BUSINESS_ID,
                                            'offer_id': offer_id,
                                            'shop_id': shop_id,
                                        },
                                        'status': {
                                            'publish': DTC.HIDDEN,
                                        },
                                    }
                                })
                            } for shop_id in [SHOP_ID_SECOND]
                        }),
                        shop_matcher(IsProtobufMap({
                            shop_id: {
                                'warehouse': IsProtobufMap({
                                    0: {
                                        'identifiers': {
                                            'business_id': BUSINESS_ID,
                                            'offer_id': offer_id,
                                            'shop_id': shop_id,
                                        },
                                        'status': {
                                            'publish': DTC.HIDDEN,
                                        },
                                    }
                                })
                            } for shop_id in [SHOP_ID]
                        }))
                    )
                } for offer_id in OFFER_IDS
            }
        ]
    }

    request = OffersBatch.UnitedOffersBatchRequest()
    for offer_id in OFFER_IDS:
        entry = request.entries.add()
        entry.method = OffersBatch.RequestMethod.GET
        entry.business_id = BUSINESS_ID
        entry.offer_id = offer_id
        if filter_mode == 'shop_id':
            entry.shop_id = SHOP_ID_SECOND

    response = stroller.post('/v1/offers/united/batch', data=request.SerializeToString())
    assert_that(response, HasStatus(200))
    assert_that(response.data, IsSerializedProtobuf(OffersBatch.UnitedOffersBatchResponse, expected_resp))
    assert_that(response.headers['Content-type'], equal_to('application/x-protobuf'))


@pytest.mark.parametrize('filter_mode', [None, 'shop_id'])
def test_get_united_offers_batch_without_merge(stroller, filter_mode):
    shop_matcher = not_ if filter_mode == 'shop_id' else is_
    expected_resp = {
        'entries': [
            {
                'united_offer': {
                    'basic': {
                        'identifiers': {
                            'business_id': BUSINESS_ID,
                            'offer_id': offer_id
                        },
                    },
                    'service': all_of(
                        IsProtobufMap({
                            shop_id: {
                                'identifiers': {
                                    'business_id': BUSINESS_ID,
                                    'offer_id': offer_id,
                                    'shop_id': shop_id,
                                },
                                'status': {
                                    'publish': None,
                                    'publish_by_partner': DTC.AVAILABLE,
                                },
                            } for shop_id in [SHOP_ID_SECOND]
                        }),
                        shop_matcher(IsProtobufMap({
                            shop_id: {
                                'identifiers': {
                                    'business_id': BUSINESS_ID,
                                    'offer_id': offer_id,
                                    'shop_id': shop_id,
                                },
                                'status': {
                                    'publish': None,
                                    'publish_by_partner': DTC.AVAILABLE,
                                },
                            } for shop_id in [SHOP_ID]
                        }))
                    ),
                    'actual': all_of(
                        IsProtobufMap({
                            shop_id: {
                                'warehouse': IsProtobufMap({
                                    0: {
                                        'identifiers': {
                                            'business_id': BUSINESS_ID,
                                            'offer_id': offer_id,
                                            'shop_id': shop_id,
                                        },
                                        'status': {
                                            'publish_by_partner': None,
                                        },
                                    }
                                })
                            } for shop_id in [SHOP_ID_SECOND]
                        }),
                        shop_matcher(IsProtobufMap({
                            shop_id: {
                                'warehouse': IsProtobufMap({
                                    0: {
                                        'identifiers': {
                                            'business_id': BUSINESS_ID,
                                            'offer_id': offer_id,
                                            'shop_id': shop_id,
                                        },
                                        'status': {
                                            'publish_by_partner': None,
                                        },
                                    }
                                })
                            } for shop_id in [SHOP_ID]
                        }))
                    )
                } for offer_id in OFFER_IDS
            }
        ]
    }

    request = OffersBatch.UnitedOffersBatchRequest()
    for offer_id in OFFER_IDS:
        entry = request.entries.add()
        entry.method = OffersBatch.RequestMethod.GET
        entry.business_id = BUSINESS_ID
        entry.offer_id = offer_id
        if filter_mode == 'shop_id':
            entry.shop_id = SHOP_ID_SECOND

    response = stroller.post('/v1/offers/united/batch?legacy=false', data=request.SerializeToString())
    assert_that(response, HasStatus(200))
    assert_that(response.data, IsSerializedProtobuf(OffersBatch.UnitedOffersBatchResponse, expected_resp))
    assert_that(response.headers['Content-type'], equal_to('application/x-protobuf'))


def test_get_united_offers_batch_same_offer_id(stroller):
    expected_resp = {
        'entries': [
            {
                'united_offer': {
                    # basic не возвращается из-за query
                    'basic': None,
                    'actual': all_of(
                        IsProtobufMap({
                            SHOP_ID: {
                                'warehouse': IsProtobufMap({
                                    0: {
                                        'identifiers': {
                                            'business_id': BUSINESS_ID,
                                            'offer_id': OFFER_ID,
                                            'shop_id': SHOP_ID,
                                        },
                                        'status': {
                                            'publish': DTC.HIDDEN,
                                        },
                                    }
                                })
                            }
                        })
                    )
                },
            },
            {
                'united_offer': {
                    'basic': None,
                    'actual': all_of(
                        IsProtobufMap({
                            SHOP_ID_SECOND: {
                                'warehouse': IsProtobufMap({
                                    0: {
                                        'identifiers': {
                                            'business_id': BUSINESS_ID,
                                            'offer_id': OFFER_ID,
                                            'shop_id': SHOP_ID_SECOND,
                                        },
                                        'status': {
                                            'publish': DTC.HIDDEN,
                                        },
                                    }
                                })
                            }
                        })
                    )
                }
            }
        ]
    }
    request = OffersBatch.UnitedOffersBatchRequest()
    for shop_id in [SHOP_ID, SHOP_ID_SECOND]:
        entry = request.entries.add()
        entry.method = OffersBatch.RequestMethod.GET
        entry.business_id = BUSINESS_ID
        entry.offer_id = OFFER_ID
        entry.shop_id = shop_id

    response = stroller.post('/v1/offers/united/batch?query={entries{united_offer{actual{warehouse{identifiers, status}}}}}', data=request.SerializeToString())
    assert_that(response, HasStatus(200))
    assert_that(response.data, IsSerializedProtobuf(OffersBatch.UnitedOffersBatchResponse, expected_resp))
    assert_that(response.headers['Content-type'], equal_to('application/x-protobuf'))


@pytest.mark.parametrize('search_tables', [0, 1])
def test_get_full_offer(stroller, offer_id, search_tables):
    expected_resp = {
        'total': 1,
        'limit': 10,
        'offset': 0,
        'current_page_position': None,
        'next_page_position': None,
        'previous_page_position': None,
        'offers': [
            {
                'basic': {
                    'identifiers': {
                        'business_id': BUSINESS_ID,
                        'offer_id': offer_id
                    },
                    'content': {
                        'partner': {
                            'original': {
                                'name': {
                                    'value': 'original name'
                                }
                            },
                            'actual': {
                                'title': {
                                    'value': 'actual title'
                                }
                            }
                        }
                    }
                },
                'service': IsProtobufMap(
                    {
                        shop_id: {
                            'identifiers': {
                                'business_id': BUSINESS_ID,
                                'offer_id': offer_id,
                                'shop_id': shop_id
                            },
                            'status': {
                                'publish_by_partner': DTC.AVAILABLE
                            }
                        } for shop_id in SHOP_IDS
                    }
                ),
            }
        ]
    }

    response = stroller.get(
        '/v1/partners/{}/offers?offer_id={}&full=true&search_tables={}'.format(BUSINESS_ID, offer_id, search_tables)
    )
    assert_that(response, HasStatus(200))
    assert_that(response.headers['Content-type'], equal_to('application/x-protobuf'))
    assert_that(response.data, IsSerializedProtobuf(SyncGetOffer.GetUnitedOffersResponse, expected_resp))


@pytest.mark.parametrize('by_shop', [False, True])
def test_select_search_index_basic(stroller, by_shop):
    """ Поиск по индексным таблицам """
    response = stroller.get('/v1/partners/{}/offers?full=true&search_tables=1{}'.format(
        BUSINESS_ID,
        '&shop_id={}'.format(SHOP_ID) if by_shop else ''
    ))
    shop_matcher = not_ if by_shop else is_
    assert_that(response, HasStatus(200))
    assert_that(response.headers['Content-type'], equal_to('application/x-protobuf'))
    assert_that(response.data, IsSerializedProtobuf(SyncGetOffer.GetUnitedOffersResponse, {
        'limit': 10,
        'offset': 0,
        'total': 2,
        'current_page_position': None,
        'next_page_position': None,
        'previous_page_position': None,
        'offers': [
            {
                'basic': {
                    'identifiers': {
                        'business_id': BUSINESS_ID,
                        'offer_id': offer_id
                    },
                },
                'service': all_of(IsProtobufMap({
                    SHOP_ID: {
                        'identifiers': {
                            'business_id': BUSINESS_ID,
                            'offer_id': offer_id,
                            'shop_id': SHOP_ID
                        },
                    }
                }), shop_matcher(IsProtobufMap({
                    SHOP_ID_SECOND: {
                        'identifiers': {
                            'business_id': BUSINESS_ID,
                            'offer_id': offer_id,
                            'shop_id': SHOP_ID_SECOND
                        },
                    }
                }))),
            } for offer_id in OFFER_IDS
        ]
    }))


@pytest.mark.parametrize('by_shop', [False, True])
def test_select_search_index_basic_paging(stroller, by_shop):
    """ Поиск по индексным таблицам с пагинацией """
    response = stroller.get('/v1/partners/{}/offers?full=true&search_tables=1&limit=1{}'.format(
        BUSINESS_ID,
        '&shop_id={}'.format(SHOP_ID) if by_shop else ''
    ))
    shop_matcher = not_ if by_shop else is_
    assert_that(response, HasStatus(200))
    assert_that(response.headers['Content-type'], equal_to('application/x-protobuf'))
    assert_that(response.data, IsSerializedProtobuf(SyncGetOffer.GetUnitedOffersResponse, {
        'limit': 1,
        'offset': 0,
        'total': 2,
        'current_page_position': None,
        'next_page_position': OFFER_ID_WITH_SPECIAL_SYMBOLS,
        'previous_page_position': None,
        'offers': [
            {
                'basic': {
                    'identifiers': {
                        'business_id': BUSINESS_ID,
                        'offer_id': offer_id
                    },
                },
                'service': all_of(IsProtobufMap({
                    SHOP_ID: {
                        'identifiers': {
                            'business_id': BUSINESS_ID,
                            'offer_id': offer_id,
                            'shop_id': SHOP_ID
                        },
                    }
                }), shop_matcher(IsProtobufMap({
                    SHOP_ID_SECOND: {
                        'identifiers': {
                            'business_id': BUSINESS_ID,
                            'offer_id': offer_id,
                            'shop_id': SHOP_ID_SECOND
                        },
                    }
                }))),
            } for offer_id in [OFFER_ID]
        ]
    }))
    assert_that(response.data, not_(IsSerializedProtobuf(SyncGetOffer.GetUnitedOffersResponse, {
        'offers': [{
            'basic': {
                'identifiers': {
                    'business_id': BUSINESS_ID,
                    'offer_id': OFFER_ID_WITH_SPECIAL_SYMBOLS
                },
            },
        }]
    })))

    response = stroller.get('/v1/partners/{}/offers?full=true&search_tables=1&limit=1&position={}{}'.format(
        BUSINESS_ID,
        OFFER_ID_WITH_SPECIAL_SYMBOLS,
        '&shop_id={}'.format(SHOP_ID) if by_shop else ''
    ))
    assert_that(response, HasStatus(200))
    assert_that(response.headers['Content-type'], equal_to('application/x-protobuf'))
    assert_that(response.data, IsSerializedProtobuf(SyncGetOffer.GetUnitedOffersResponse, {
        'limit': 1,
        'offset': 0,
        'total': 2,
        'current_page_position': OFFER_ID_WITH_SPECIAL_SYMBOLS,
        'next_page_position': None,
        'previous_page_position': OFFER_ID,
        'offers': [
            {
                'basic': {
                    'identifiers': {
                        'business_id': BUSINESS_ID,
                        'offer_id': offer_id
                    },
                },
                'service': all_of(IsProtobufMap({
                    SHOP_ID: {
                        'identifiers': {
                            'business_id': BUSINESS_ID,
                            'offer_id': offer_id,
                            'shop_id': SHOP_ID
                        },
                    }
                }), shop_matcher(IsProtobufMap({
                    SHOP_ID_SECOND: {
                        'identifiers': {
                            'business_id': BUSINESS_ID,
                            'offer_id': offer_id,
                            'shop_id': SHOP_ID_SECOND
                        },
                    }
                }))),
            } for offer_id in [OFFER_ID_WITH_SPECIAL_SYMBOLS]
        ]
    }))
    assert_that(response.data, not_(IsSerializedProtobuf(SyncGetOffer.GetUnitedOffersResponse, {
        'offers': [{
            'basic': {
                'identifiers': {
                    'business_id': BUSINESS_ID,
                    'offer_id': OFFER_ID
                },
            },
        }]
    })))
