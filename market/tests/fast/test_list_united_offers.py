# coding: utf-8

import pytest
from hamcrest import assert_that, equal_to, has_entries, raises, calling, empty, not_none

import market.idx.datacamp.proto.offer.DataCampOffer_pb2 as DTC
import market.idx.datacamp.proto.api.SyncGetOffer_pb2 as SyncGetOffer
import market.idx.datacamp.controllers.stroller.proto.config_pb2 as Config

from market.idx.datacamp.controllers.stroller.yatf.test_env import make_stroller
from market.idx.datacamp.yatf.matchers.matchers import HasStatus
from market.idx.yatf.matchers.protobuf_matchers import IsSerializedProtobuf, IsProtobuf
from market.idx.pylibrary.datacamp.conversion import offer_to_basic_row, offer_to_service_row


def _gen_row(
    business_id,
    offer_id,
    shop_id=None,
    warehouse_id=None,
    vendor=None,
    category_ids=None,
    cpa=None,
    cpc=None,
    result_content_status=None,
    disabled=None,
    result_status=None,
    partner_status=None,
    supply_plan=None,
    price=None,
    market_stocks=None,
    market_category_id=None,
    allow_model_create_update=None,
    rgb=None
):
    offer = DTC.Offer(
        identifiers=DTC.OfferIdentifiers(
            business_id=business_id,
            offer_id=offer_id
        )
    )
    if shop_id:
        offer.identifiers.shop_id = shop_id
    if warehouse_id:
        offer.identifiers.warehouse_id = warehouse_id
    if vendor:
        offer.content.partner.original.vendor.value = vendor
    if category_ids:
        offer.content.partner.original.category.id = category_ids[-1]
        offer.content.partner.original.category.path_category_ids = '\\'.join(str(x) for x in category_ids)
    if market_category_id:
        offer.content.binding.approved.market_category_id = market_category_id
    if cpa:
        offer.content.status.content_system_status.cpa_state = cpa
    if cpc:
        offer.content.status.content_system_status.cpc_state = cpc
    if result_content_status:
        offer.content.status.result.card_status = result_content_status
    if disabled:
        offer.status.disabled.extend(disabled)
    if result_status:
        offer.status.result = result_status
    if partner_status:
        offer.status.publish_by_partner = partner_status
    if supply_plan:
        offer.content.partner.original_terms.supply_plan.value = supply_plan
    if price:
        offer.price.basic.binary_price.price = price
    if market_stocks:
        offer.stock_info.market_stocks.count = market_stocks
    if allow_model_create_update is not None:
        offer.content.status.content_system_status.allow_model_create_update = allow_model_create_update
    if rgb:
        offer.meta.rgb = rgb

    return offer_to_service_row(offer) if shop_id else offer_to_basic_row(offer)


def _expected_response(business_id, offer_ids, shop_ids, actuals=None, stocks=None, result_statuses=dict(), **kwargs):
    return dict(
        offers=empty() if offer_ids is None else [
            {
                'basic': {
                    'identifiers': {
                        'business_id': business_id,
                        'offer_id': offer_id,
                    },
                },
                'service': has_entries({
                    shop_id: IsProtobuf(
                        {
                            'identifiers': {
                                'business_id': business_id,
                                'offer_id': offer_id,
                                'shop_id': shop_id,
                            },
                            'status': {
                                'result': result_statuses[shop_id] if shop_id in result_statuses else not_none()
                            }
                        }
                    ) for shop_id in shop_ids
                }),
                'actual': empty() if actuals is None else has_entries({
                    shop_id: IsProtobuf({
                        'warehouse': has_entries({
                            warehouse_id: IsProtobuf({
                                'identifiers': {
                                    'business_id': business_id,
                                    'offer_id': offer_id,
                                    'shop_id': shop_id,
                                    'warehouse_id': warehouse_id,
                                }
                            }) for warehouse_id in actuals
                        })
                    }) for shop_id in shop_ids
                }),
            } for offer_id in offer_ids
        ],
        **kwargs
    )


OFFERS = [
    # paging
    {
        'business_id': 1,
        'offer_id': offer_id,
    } for offer_id in ('o01', 'o02', 'o03', 'o04', 'o05', 'o06', 'o07', 'o08', 'o09', 'o10', 'o11')
] + [
    # filtering by identifiers
    {
        'business_id': 2,
        'offer_id': 'o01',
        'service': {
            20: {},
            21: {},
        },
    },
    {
        'business_id': 2,
        'offer_id': 'o02',
        'service': {
            22: {}
        }
    }
] + [
    # filtering by vendor
    {
        'business_id': 3,
        'offer_id': 'o01',
        'basic': {
            'vendor': 'vendor_1'
        },
    },
    {
        'business_id': 3,
        'offer_id': 'o02',
        'basic': {
            'vendor': 'vendor_2'
        },
    },
    {
        'business_id': 3,
        'offer_id': 'o03',
        'basic': {
            'vendor': 'vendor_2'
        },
    },
] + [
    # filtering by partner category
    {
        'business_id': 4,
        'offer_id': 'o01',
        'basic': {
            'category_ids': [1, 2]
        },
    },
    {
        'business_id': 4,
        'offer_id': 'o02',
        'basic': {
            'category_ids': [1, 3]
        },
    },
    {
        'business_id': 4,
        'offer_id': 'o03',
        'basic': {
            'category_ids': [4]
        },
    },
] + [
    # filtering by content cpa state
    {
        'business_id': 5,
        'offer_id': 'o01',
        'basic': {
            'cpa': DTC.OfferContentCpaState.CONTENT_STATE_READY
        },
    },
    {
        'business_id': 5,
        'offer_id': 'o02',
        'basic': {
            'cpa': DTC.OfferContentCpaState.CONTENT_STATE_REJECTED,
        },
    }
] + [
    # filtering by content cpc state
    {
        'business_id': 6,
        'offer_id': 'o01',
        'basic': {
            'cpc': DTC.OfferContentCpcState.CPC_CONTENT_READY,
        },
    },
    {
        'business_id': 6,
        'offer_id': 'o02',
        'basic': {
            'cpc': DTC.OfferContentCpcState.CPC_CONTENT_MISSING,
        },
    }
] + [
    # filtering by disabled status
    {
        'business_id': 7,
        'offer_id': 'o01',
        'service': {
            70: {'disabled': [DTC.Flag(flag=True, meta=DTC.UpdateMeta(source=DTC.PUSH_PARTNER_API))]}
        },
    },
    {
        'business_id': 7,
        'offer_id': 'o02',
        'service': {
            70: {'disabled': [DTC.Flag(flag=False, meta=DTC.UpdateMeta(source=DTC.PUSH_PARTNER_API))]}
        },
    },
    {
        'business_id': 7,
        'offer_id': 'o03',
        'service': {
            70: {'disabled': [DTC.Flag(flag=True, meta=DTC.UpdateMeta(source=DTC.PUSH_PARTNER_FEED))]}
        },
    },
    {
        'business_id': 7,
        'offer_id': 'o04',
        'service': {
            70: {},
        },
    },
] + [
    # filtering by partner status
    {
        'business_id': 8,
        'offer_id': 'o01',
        'service': {
            80: {'partner_status': DTC.SummaryPublicationStatus.AVAILABLE},
        },
    },
    {
        'business_id': 8,
        'offer_id': 'o02',
        'service': {
            80: {'partner_status': DTC.SummaryPublicationStatus.HIDDEN},
        },
    },
    {
        'business_id': 8,
        'offer_id': 'o03',
        'service': {
            90: {'partner_status': DTC.SummaryPublicationStatus.AVAILABLE},
        },
    }
] + [
    # filtering by result status
    {
        'business_id': 12,
        'offer_id': 'o01',
        'service': {
            80: {'result_status': DTC.OfferStatus.ResultStatus.NOT_PUBLISHED_NEED_INFO},
        },
    },
    {
        'business_id': 12,
        'offer_id': 'o02',
        'service': {
            80: {'result_status': DTC.OfferStatus.ResultStatus.NOT_PUBLISHED_CHECKING},
        },
    },
    {
        'business_id': 12,
        'offer_id': 'o03',
        'service': {
            90: {'result_status': DTC.OfferStatus.ResultStatus.PUBLISHED},
        },
    }
] + [
    # filtering by supply plan
    {
        'business_id': 9,
        'offer_id': 'o01',
        'service': {
            90: {'supply_plan': DTC.SupplyPlan.WILL_SUPPLY},
        }
    },
    {
        'business_id': 9,
        'offer_id': 'o02',
        'service': {
            90: {'supply_plan': DTC.SupplyPlan.WONT_SUPPLY},
        },
    }
] + [
    # filtering by include_shop_id
    {
        'business_id': 10,
        'offer_id': 'o01',
        'service': {
            10: {},
            30: {},
        },
    },
    {
        'business_id': 10,
        'offer_id': 'o02',
        'service': {
            20: {},
        },
    },
    {
        'business_id': 10,
        'offer_id': 'o03',
        'service': {
            40: {}
        },
    }
] + [
    # filtering by exclude_shop_id
    {
        'business_id': 11,
        'offer_id': 'o01',
        'service': {
            10: {},
            20: {},
        },
    },
    {
        'business_id': 11,
        'offer_id': 'o02',
        'service': {
            30: {},
        },
    },
] + [
    # filtering by integral content status
    {
        'business_id': 12,
        'offer_id': 'o01',
        'basic': {
            'result_content_status': DTC.ResultContentStatus.HAS_CARD_MARKET
        },
    },
    {
        'business_id': 12,
        'offer_id': 'o02',
        'basic': {
            'result_content_status': DTC.ResultContentStatus.NO_CARD_NEED_CONTENT,
        },
    }
] + [
    # filtering by price
    {
        'business_id': 13,
        'offer_id': 'o01',
        'service': {
            10: {
                'price': 0
            }
        }
    },
    {
        'business_id': 13,
        'offer_id': 'o02',
        'service': {
            10: {
                'price': 123456789
            }
        }
    }
] + [
    # filtering by market_stocks
    {
        'business_id': 14,
        'offer_id': 'o01',
        'service': {
            10: {
                'market_stocks': 0
            }
        }
    },
    {
        'business_id': 14,
        'offer_id': 'o02',
        'service': {
            10: {
                'market_stocks': 10
            }
        }
    },
    {
        'business_id': 14,
        'offer_id': 'o03',
        'service': {
            10: {}
        },
        'actual': {
            10: {
                'warehouse': {
                    1001: {
                        'market_stocks': 0
                    },
                    1002: {
                        'market_stocks': 10
                    }
                }
            }
        }
    },
] + [
    # filtering by market_category_id
    {
        'business_id': 15,
        'offer_id': 'o01',
        'basic': {
            'category_ids': [1, 2],
            'market_category_id':  1
        }
    },
    {
        'business_id': 15,
        'offer_id': 'o02',
        'basic': {
            'category_ids': [1, 2],
            'market_category_id':  2
        }
    }
] + [
    # filtering by price and warehouse_id
    {
        'business_id': 16,
        'offer_id': 'o01',
        'service': {
            10: {
                'price': 123456789
            }
        },
        'actual': {
            10: {
                'warehouse': {
                    1001: {
                        'market_stocks': 0
                    }
                }
            }
        }
    },
    {
        'business_id': 16,
        'offer_id': 'o02',
        'service': {
            10: {
                'price': 0
            }
        },
        'actual': {
            10: {
                'warehouse': {
                    1002: {
                        'market_stocks': 10
                    }
                }
            }
        }
    }
] + [
    # filtering by shop_id and price with positioning
    {
        'business_id': 17,
        'offer_id': 'o01',
        'service': {
            1: {
                'price': 1711
            }
        }
    },
    {
        'business_id': 17,
        'offer_id': 'o02',
        'service': {
            1: {
                'price': 0
            }
        }
    },
    {
        'business_id': 17,
        'offer_id': 'o03',
        'service': {
            1: {
                'price': 0
            }
        }
    },
    {
        'business_id': 17,
        'offer_id': 'o04',
        'service': {
            2: {
                'price': 1724
            }
        }
    },
    {
        'business_id': 17,
        'offer_id': 'o05',
        'service': {
            2: {
                'price': 0
            }
        }
    }
] + [
    # filtering by non-existig shop_id
    {
        'business_id': 823589,
        'offer_id': 'o18',
        'service': {
            n: {
                'price': n
            }
        }
    } for n in range(1, 2000)
] + [
    # filtering by warehouse_id only
    {
        'business_id': 19,
        'offer_id': 'o01',
        'service': {
            10: {
                'price': 123456789
            }
        },
        'actual': {
            10: {
                'warehouse': {
                    1001: {
                        'market_stocks': 0
                    }
                }
            }
        }
    },
    {
        'business_id': 19,
        'offer_id': 'o02',
        'service': {
            10: {
                'price': 456
            },
            20: {
                'price': 987654321
            }
        },
        'actual': {
            10: {
                'warehouse': {
                    1001: {
                        'market_stocks': 0
                    },
                    1002: {
                        'market_stocks': 10
                    }
                }
            },
            20: {
                'warehouse': {
                    1002: {
                        'market_stocks': 10
                    }
                }
            },
            30: {
                'warehouse': {
                    1002: {
                        'market_stocks': 10
                    }
                }
            }
        }
    }
] + [
    # scan_limit and paging
    {
        'business_id': 20,
        'offer_id': '{:05d}'.format(offer_id),
        'service': {
            200: {'price': 0}
        },
    } for offer_id in range(0, Config.TGeneral().SelectSizeInListOffersHandler)
] + [
    # scan_limit and paging
    {
        'business_id': 20,
        'offer_id': '{:05d}'.format(offer_id),
        'service': {
            200: {'price': 20200}
        }
    } for offer_id in range(Config.TGeneral().SelectSizeInListOffersHandler, Config.TGeneral().SelectSizeInListOffersHandler + 500)
] + [
    # scan_limit and paging
    {
        'business_id': 20,
        'offer_id': '{:05d}'.format(offer_id),
        'service': {
            200: {'price': 0}
        },
    } for offer_id in range(Config.TGeneral().SelectSizeInListOffersHandler + 500, Config.TGeneral().SelectSizeInListOffersHandler * 2)
] + [
    # scan_limit and paging
    {
        'business_id': 20,
        'offer_id': '{:05d}'.format(offer_id),
        'service': {
            200: {'price': 20200}
        }
    } for offer_id in range(Config.TGeneral().SelectSizeInListOffersHandler * 2, Config.TGeneral().SelectSizeInListOffersHandler * 3)
] + [
    # scan_limit and paging
    {
        'business_id': 20,
        'offer_id': '{:05d}'.format(offer_id),
        'service': {
            200: {'price': 0}
        }
    } for offer_id in range(Config.TGeneral().SelectSizeInListOffersHandler * 3, Config.TGeneral().SelectSizeInListOffersHandler * 10)
] + [
    # scan_limit and paging
    {
        'business_id': 20,
        'offer_id': '{:05d}'.format(Config.TGeneral().SelectSizeInListOffersHandler * 10 + 1),
        'service': {
            200: {'price': 20200}
        }
    }
] + [
    {
        'business_id': 21,
        'offer_id': 'o03',
        'service': {
            10: {
                'price': 654
            },
            20: {
                'price': 357
            }
        },
        'actual': {
            10: {
                'warehouse': {
                    1002: {
                        'market_stocks': 10
                    }
                }
            }
        }
    }
] + [
    {
        'business_id': 22,
        'offer_id': 'o03',
        'service': {
            10: {
                'price': 654
            },
            20: {
                'price': 357
            }
        },
        'actual': {
            10: {
                'warehouse': {
                    1001: {
                        'market_stocks': 0
                    },
                    1002: {
                        'market_stocks': 10
                    }
                }
            }
        }
    }
] + [
    # filtering by allow_model_create_update
    {
        'business_id': 23,
        'offer_id': 'o231',
        'basic': {
            'allow_model_create_update': True
        }
    },
    {
        'business_id': 23,
        'offer_id': 'o232',
        'basic': {
            'allow_model_create_update': False
        }
    },
    {
        # какая-то базовая часть есть, но без allow_model_create_update => allow_model_create_update=False
        'business_id': 23,
        'offer_id': 'o233',
        'basic': {
            'market_category_id': 23,
        }
    }
] + [
    # for integral status calculating
    {
        'business_id': 24,
        'offer_id': 'o1',
        'basic': {
            'vendor': 'vendor_1'
        },
        'service': {
            10: {
                'rgb': DTC.BLUE,
                'disabled': [
                    DTC.Flag(
                        flag=True,
                        meta=DTC.UpdateMeta(source=DTC.PUSH_PARTNER_FEED)
                    )
                ]
            },
            20: {
                'rgb': DTC.BLUE
            }
        },
        'actual': {
            10: {
                'warehouse': {
                    1000: {},
                }
            },
            20: {
                'warehouse': {
                    1000: {
                        'disabled': [
                            DTC.Flag(
                                flag=True,
                                meta=DTC.UpdateMeta(source=DTC.MARKET_ABO)
                            )
                        ]
                    },
                    2000: {},
                }
            }
        }
    },
]


def crossdock_warehouse_id(shop_id):
    return shop_id * 100


def ff_warehouse_id():
    return 145


@pytest.fixture(scope='module')
def basic_offers():
    return [_gen_row(o['business_id'], o['offer_id'], **o.get('basic', dict())) for o in OFFERS]


@pytest.fixture(scope='module')
def service_offers():
    return [_gen_row(o['business_id'], o['offer_id'], shop_id=shop_id, **so)
            for o in OFFERS for shop_id, so in list(o.get('service', dict()).items()) or [(o['business_id'] * 10, dict())]]


@pytest.fixture(scope='module')
def actual_service_offers():
    offers = []
    for o in OFFERS:
        if 'actual' in o:
            for shop_id, warehouses in list(o['actual'].items()):
                for whid, warehouse in list(warehouses['warehouse'].items()):
                    offers.append(_gen_row(o['business_id'], o['offer_id'], shop_id=shop_id, warehouse_id=whid, **warehouse))
        else:
            for shop_id, so in list(o.get('service', dict()).items()) or [(o['business_id'] * 10, dict())]:
                offers.append(_gen_row(o['business_id'], o['offer_id'], shop_id=shop_id, warehouse_id=crossdock_warehouse_id(shop_id), **so))
                offers.append(_gen_row(o['business_id'], o['offer_id'], shop_id=shop_id, warehouse_id=ff_warehouse_id(), **so))

    return offers


@pytest.yield_fixture(scope='module')
def stroller(
        config,
        yt_server,
        log_broker_stuff,
        basic_offers_table,
        service_offers_table,
        actual_service_offers_table,
):
    with make_stroller(
            config,
            yt_server,
            log_broker_stuff,
            shopsdat_cacher=True,
            basic_offers_table=basic_offers_table,
            service_offers_table=service_offers_table,
            actual_service_offers_table=actual_service_offers_table,
    ) as stroller_env:
        yield stroller_env


def _check_response(stroller, uri, expected_resp):
    response = stroller.get(uri)
    assert_that(response, HasStatus(200))
    assert_that(response.data, IsSerializedProtobuf(SyncGetOffer.GetUnitedOffersResponse, expected_resp))
    assert_that(response.headers['Content-type'], equal_to('application/x-protobuf'))


@pytest.fixture(scope='module', params=[True, False])
def full(request):
    return request.param


def make_actuals(full, shop_id):
    return [ff_warehouse_id(), crossdock_warehouse_id(shop_id)] if full else None


def make_stock(full, shop_id):
    return [crossdock_warehouse_id(shop_id)] if full else None


def test_list_all_offers(stroller, full):
    # провереряем выдачу первой страницы
    expected_resp = _expected_response(1, ['o01', 'o02', 'o03', 'o04', 'o05', 'o06', 'o07', 'o08', 'o09', 'o10'], [10], make_actuals(full, 10), make_stock(full, 10))
    _check_response(stroller, '/v1/partners/1/offers?full={}'.format(full), expected_resp)

    # проверяем выдачу второй страницы
    expected_resp = _expected_response(1, ['o11'], [10], make_actuals(full, 10), make_stock(full, 10))
    _check_response(stroller, '/v1/partners/1/offers?offset=10&full={}'.format(full), expected_resp)


def test_positional_paging(stroller, full):
    # проверяем пагинацию через offer_id
    expected_resp = _expected_response(1, ['o01', 'o02'], [10], make_actuals(full, 10), make_stock(full, 10), previous_page_position=None, current_page_position=None, next_page_position='o03')
    _check_response(stroller, '/v1/partners/1/offers?limit=2&full={}'.format(full), expected_resp)

    expected_resp = _expected_response(1, ['o11'], [10], make_actuals(full, 10), make_stock(full, 10), previous_page_position='o09', current_page_position='o11', next_page_position=None)
    _check_response(stroller, '/v1/partners/1/offers?&limit=2&position=o11&full={}'.format(full), expected_resp)


def test_positional_paging_with_shop_id_filter(stroller, full):
    # проверяем пагинацию через offer_id и фильтре по shop_id
    expected_resp = _expected_response(1, ['o01', 'o02'], [10], make_actuals(full, 10), make_stock(full, 10), previous_page_position=None, current_page_position=None, next_page_position='o03')
    _check_response(stroller, '/v1/partners/1/offers?limit=2&shop_id=10&full={}'.format(full), expected_resp)

    expected_resp = _expected_response(1, ['o11'], [10], make_actuals(full, 10), make_stock(full, 10), previous_page_position='o09', current_page_position='o11', next_page_position=None)
    _check_response(stroller, '/v1/partners/1/offers?&limit=2&position=o11&shop_id=10&full={}'.format(full), expected_resp)


def test_shop_id_filter(stroller):
    # Проверяем, что при фильтре по shop_id
    # 1. в сервисных офферах есть только оффера с shop_id из запроса
    # 2. оффера, которые недоступны в shop_id, отсутствуют в выдаче
    expected_resp = _expected_response(2, ['o01'], [20])
    _check_response(stroller, '/v1/partners/2/offers?shop_id=20', expected_resp)

    not_expected_resp = _expected_response(2, ['o01'], [21])
    assert_that(
        calling(_check_response).with_args(stroller, '/v1/partners/2/offers?shop_id=20', not_expected_resp),
        raises(AssertionError))


def test_return_shop_id_filter(stroller):
    expected_resp = _expected_response(2, ['o01'], [20], next_page_position='o02')
    _check_response(stroller, '/v1/partners/2/offers?return_shop_id=20&limit=1', expected_resp)

    not_expected_resp = _expected_response(2, ['o01'], [21])
    assert_that(
        calling(_check_response).with_args(stroller, '/v1/partners/2/offers?return_shop_id=20&limit=1', not_expected_resp),
        raises(AssertionError))

    expected_resp = _expected_response(2, ['o02'], [], current_page_position='o02')
    _check_response(stroller, '/v1/partners/2/offers?return_shop_id=20&limit=1&position=o02', expected_resp)

    not_expected_resp = _expected_response(2, ['o02'], [22])
    assert_that(
        calling(_check_response).with_args(stroller, '/v1/partners/2/offers?return_shop_id=20&limit=1&position=o02', not_expected_resp),
        raises(AssertionError))


def test_offer_id_filter(stroller, full):
    expected_resp = _expected_response(2, ['o02'], [22], make_actuals(full, 22), make_stock(full, 22))
    _check_response(stroller, '/v1/partners/2/offers?offer_id=o02&full={}'.format(full), expected_resp)


def test_vendor_filter(stroller, full):
    expected_resp = _expected_response(3, ['o01'], [30], make_actuals(full, 30), make_stock(full, 30), current_page_position=None, next_page_position=None)
    _check_response(stroller, '/v1/partners/3/offers?vendor=vendor_1&full={}'.format(full), expected_resp)


def test_vendor_positional_paging(stroller):
    expected_resp = _expected_response(3, ['o02'], [30], previous_page_position=None, current_page_position=None, next_page_position='o03', limit=1)
    _check_response(stroller, '/v1/partners/3/offers?vendor=vendor_2&limit=1', expected_resp)

    expected_resp = _expected_response(3, ['o03'], [30], previous_page_position='o02', current_page_position='o03', next_page_position=None, limit=1)
    _check_response(stroller, '/v1/partners/3/offers?vendor=vendor_2&limit=1&position=o03', expected_resp)


def test_category_id_filter(stroller):
    expected_resp = _expected_response(4, ['o01', 'o02'], [40], current_page_position=None, next_page_position=None)
    _check_response(stroller, '/v1/partners/4/offers?category_id=1', expected_resp)

    expected_resp = _expected_response(4, ['o01'], [40], current_page_position=None, next_page_position=None)
    _check_response(stroller, '/v1/partners/4/offers?category_id=2', expected_resp)

    expected_resp = _expected_response(4, ['o02'], [40], current_page_position=None, next_page_position=None)
    _check_response(stroller, '/v1/partners/4/offers?category_id=3', expected_resp)


def test_content_cpa_state_filter(stroller):
    expected_resp = _expected_response(5, ['o01'], [50], current_page_position=None, next_page_position=None)
    _check_response(stroller, '/v1/partners/5/offers?cpa=8', expected_resp)


def test_content_cpc_state_filter(stroller):
    expected_resp = _expected_response(6, ['o01'], [60], current_page_position=None, next_page_position=None)
    _check_response(stroller, '/v1/partners/6/offers?cpc=1', expected_resp)


def test_integral_content_status_filter(stroller):
    expected_resp = _expected_response(12, ['o01'], [120], current_page_position=None, next_page_position=None)
    _check_response(stroller, '/v1/partners/12/offers?integral_content_status=1', expected_resp)


def test_disabled_by_filter(stroller, full):
    expected_resp = _expected_response(7, ['o01'], [70], make_actuals(full, 70), make_stock(full, 70), current_page_position=None, next_page_position='o03', limit=1)
    _check_response(stroller, '/v1/partners/7/offers?shop_id=70&disabled_by=3&disabled_by=5&limit=1&full={}'.format(full), expected_resp)

    expected_resp = _expected_response(7, ['o03'], [70], make_actuals(full, 70), make_stock(full, 70), current_page_position='o03', next_page_position=None)
    _check_response(stroller, '/v1/partners/7/offers?shop_id=70&disabled_by=3&disabled_by=5&position=o03&full={}'.format(full), expected_resp)


def test_partner_status_filter(stroller):
    expected_resp = _expected_response(8, ['o01'], [80], current_page_position=None, next_page_position=None)
    _check_response(stroller, '/v1/partners/8/offers?shop_id=80&partner_status=1', expected_resp)


def test_result_status_filter(stroller):
    expected_resp = _expected_response(12, ['o02'], [80], current_page_position=None, next_page_position=None)
    _check_response(stroller, '/v1/partners/12/offers?shop_id=80&result_status=3', expected_resp)


def test_supply_plan_filter(stroller):
    expected_resp = _expected_response(9, ['o01'], [90])
    _check_response(stroller, '/v1/partners/9/offers?shop_id=90&supply_plan=2', expected_resp)


def test_include_shop_id_filter(stroller):
    expected_resp = _expected_response(10, ['o01'], [10, 30], current_page_position=None, next_page_position='o02', limit=1)
    _check_response(stroller, '/v1/partners/10/offers?include_shop_id=10&include_shop_id=20&limit=1', expected_resp)

    expected_resp = _expected_response(10, ['o02'], [20], current_page_position='o02', next_page_position=None)
    _check_response(stroller, '/v1/partners/10/offers?include_shop_id=10&include_shop_id=20&position=o02', expected_resp)


def test_exclude_shop_id_filter(stroller):
    expected_resp = _expected_response(11, ['o02'], [30])
    _check_response(stroller, '/v1/partners/11/offers?exclude_shop_id=10', expected_resp)


def test_filter_by_price(stroller):
    no_price_resp = _expected_response(13, ['o01'], [10])
    _check_response(stroller, '/v1/partners/13/offers?shop_id=10&has_price=false', no_price_resp)

    has_price_resp = _expected_response(13, ['o02'], [10])
    _check_response(stroller, '/v1/partners/13/offers?shop_id=10&has_price=true', has_price_resp)


def test_filter_by_market_stocks(stroller):
    whid = crossdock_warehouse_id(10)

    no_stocks_response = _expected_response(14, ['o01'], [10], [whid], [whid])
    _check_response(stroller, '/v1/partners/14/offers?shop_id=10&warehouse_id={}&has_market_stocks=false'.format(whid), no_stocks_response)

    has_stocks_resp = _expected_response(14, ['o02'], [10], [whid], [whid])
    _check_response(stroller, '/v1/partners/14/offers?shop_id=10&warehouse_id={}&has_market_stocks=true'.format(whid), has_stocks_resp)


def test_filter_market_stock_multiple_warehouses(stroller):
    whid = 1001

    no_stocks_response = _expected_response(14, ['o03'], [10], [whid], [])
    _check_response(stroller, '/v1/partners/14/offers?shop_id=10&warehouse_id={}&has_market_stocks=false'.format(whid), no_stocks_response)

    has_stocks_resp = _expected_response(14, None, [])
    _check_response(stroller, '/v1/partners/14/offers?shop_id=10&warehouse_id={}&has_market_stocks=true'.format(whid), has_stocks_resp)

    whid = 1002

    no_stocks_response = _expected_response(14, None, [])
    _check_response(stroller, '/v1/partners/14/offers?shop_id=10&warehouse_id={}&has_market_stocks=false'.format(whid), no_stocks_response)

    has_stocks_resp = _expected_response(14, ['o03'], [10], [whid], [])
    _check_response(stroller, '/v1/partners/14/offers?shop_id=10&warehouse_id={}&has_market_stocks=true'.format(whid), has_stocks_resp)


def test_filter_by_market_category_id_binding(stroller):
    expected_resp = _expected_response(15, ['o01'], [150], current_page_position=None, next_page_position=None)
    _check_response(stroller, '/v1/partners/15/offers?market_category_id=1', expected_resp)


def test_filter_by_price_and_by_market_stocks(stroller):
    whid = 1001

    no_price_no_stocks_response = _expected_response(16, None, [])
    _check_response(stroller, '/v1/partners/16/offers?shop_id=10&has_price=false&has_market_stocks=false', no_price_no_stocks_response)

    has_price_no_stocks_response = _expected_response(16, ['o01'], [10], [whid], [])
    _check_response(stroller, '/v1/partners/16/offers?shop_id=10&has_price=true&has_market_stocks=false', has_price_no_stocks_response)

    whid = 1002

    no_price_has_stocks_response = _expected_response(16, ['o02'], [10], [whid], [])
    _check_response(stroller, '/v1/partners/16/offers?shop_id=10&has_price=false&has_market_stocks=true', no_price_has_stocks_response)

    has_price_has_stocks_response = _expected_response(16, None, [])
    _check_response(stroller, '/v1/partners/16/offers?shop_id=10&has_price=true&has_market_stocks=true', has_price_has_stocks_response)


def test_default_paging_with_shop_id_and_has_price_filter(stroller):
    # проверяем пагинацию по-умолчанию c фильрами по shop_id и наличию цены
    expected_resp = _expected_response(17, ['o04'], [2], current_page_position=None, next_page_position=None, offset=0)
    _check_response(stroller, '/v1/partners/17/offers?limit=2&shop_id=2&has_price=true', expected_resp)

    expected_resp = _expected_response(17, None, [])
    _check_response(stroller, '/v1/partners/17/offers?limit=2&shop_id=2&has_price=true&offset=1', expected_resp)


def test_paging_with_shop_id_and_has_price_filter(stroller):
    # проверяем пагинацию c фильрами по shop_id и наличию цены
    expected_resp = _expected_response(17, ['o04'], [2], current_page_position=None, next_page_position=None, limit=2, offset=0)
    _check_response(stroller, '/v1/partners/17/offers?limit=2&shop_id=2&has_price=true', expected_resp)

    expected_resp = _expected_response(17, None, [])
    _check_response(stroller, '/v1/partners/17/offers?limit=2&shop_id=2&has_price=true&offset=1', expected_resp)


def test_filter_non_existing_shop_id(stroller):
    # проверяем скорость работы ручки, если shop_id из запроса отсутствует в базе
    expected_resp = _expected_response(823589, None, [])
    _check_response(stroller, '/v1/partners/823589/offers?shop_id=589141&limit=1&has_price=true', expected_resp)


def test_filter_by_warehouse_id_only(stroller):
    # если нет ни одной подходящей актульной части, то и связанных сервисных частей быть не должно
    whid1 = 1001
    whid2 = 1002

    expected_resp = _expected_response(19, ['o02'], [10], [whid2], [whid2])
    _check_response(stroller, '/v1/partners/19/offers?shop_id=10&warehouse_id={}'.format(whid2), expected_resp)

    not_expected_resp = _expected_response(19, ['o01'], [10], [whid1], [whid1])
    assert_that(
        calling(_check_response).with_args(stroller, '/v1/partners/19/offers?shop_id=10&warehouse_id={}'.format(whid2), not_expected_resp),
        raises(AssertionError))

    expected_resp = _expected_response(19, ['o02'], [10, 20], [whid2], [whid2])
    _check_response(stroller, '/v1/partners/19/offers?warehouse_id={}'.format(whid2), expected_resp)

    not_expected_resp = _expected_response(19, ['o02'], [10, 20, 30], [whid2], [whid2])
    assert_that(
        calling(_check_response).with_args(stroller, '/v1/partners/19/offers?warehouse_id={}'.format(whid2), not_expected_resp),
        raises(AssertionError))


def test_scan_limit_with_has_price_filter(stroller, full):
    expected_resp = _expected_response(
        20,
        None,
        [200],
        previous_page_position=None,
        current_page_position=None,
        next_page_position='{:05d}'.format(Config.TGeneral().SelectSizeInListOffersHandler - 1),
        limit=2
    )
    _check_response(stroller, '/v1/partners/20/offers?shop_id=200&has_price=true&scan_limit={:05d}&limit=2&full={}'.format(Config.TGeneral().SelectSizeInListOffersHandler, full), expected_resp)

    expected_resp = _expected_response(
        20,
        ['{:05d}'.format(Config.TGeneral().SelectSizeInListOffersHandler), '{:05d}'.format(Config.TGeneral().SelectSizeInListOffersHandler + 1)],
        [200],
        make_actuals(full, 200),
        make_stock(full, 200),
        previous_page_position=None,
        current_page_position='{:05d}'.format(Config.TGeneral().SelectSizeInListOffersHandler),
        next_page_position='{:05d}'.format(Config.TGeneral().SelectSizeInListOffersHandler + 2)
    )
    _check_response(
        stroller,
        '/v1/partners/20/offers?shop_id=200&has_price=true&scan_limit=100000&limit=2&position={:05d}&full={}'.format(Config.TGeneral().SelectSizeInListOffersHandler, full),
        expected_resp
    )

    expected_resp = _expected_response(
        20,
        [
            '{:05d}'.format(offer_id) for offer_id in range(Config.TGeneral().SelectSizeInListOffersHandler + 2, Config.TGeneral().SelectSizeInListOffersHandler + 500)
        ] + [
            '{:05d}'.format(offer_id) for offer_id in range(Config.TGeneral().SelectSizeInListOffersHandler * 2, Config.TGeneral().SelectSizeInListOffersHandler * 3)
        ],
        [200],
        make_actuals(full, 200),
        make_stock(full, 200),
        previous_page_position='{:05d}'.format(Config.TGeneral().SelectSizeInListOffersHandler + 1),
        current_page_position='{:05d}'.format(Config.TGeneral().SelectSizeInListOffersHandler + 2),
        next_page_position='{:05d}'.format(Config.TGeneral().SelectSizeInListOffersHandler * 6 + 1),
    )
    _check_response(
        stroller,
        '/v1/partners/20/offers?shop_id=200&has_price=true&scan_limit=5000&limit=100000&position={:05d}&full={}'.format(Config.TGeneral().SelectSizeInListOffersHandler + 2, full),
        expected_resp
    )


def test_with_missing_actual_parts_1(stroller):
    expected_resp = _expected_response(21, ['o03'], [10], [1002])
    _check_response(stroller, '/v1/partners/21/offers?has_market_stocks=true', expected_resp)

    not_expected_resp = _expected_response(21, ['o03'], [20])
    assert_that(
        calling(_check_response).with_args(stroller, '/v1/partners/21/offers?has_market_stocks=false', not_expected_resp),
        raises(AssertionError))


def test_with_missing_actual_parts_2(stroller):
    expected_resp = _expected_response(22, ['o03'], [10], [1002])
    _check_response(stroller, '/v1/partners/22/offers?has_market_stocks=true', expected_resp)

    expected_resp = _expected_response(22, ['o03'], [10], [1001])
    _check_response(stroller, '/v1/partners/22/offers?has_market_stocks=false', expected_resp)


def test_with_missing_actual_parts_3(stroller):
    expected_resp = _expected_response(22, ['o03'], [10], [1002])
    _check_response(stroller, '/v1/partners/22/offers?shop_id=10&has_market_stocks=true', expected_resp)

    expected_resp = _expected_response(22, ['o03'], [10], [1001])
    _check_response(stroller, '/v1/partners/22/offers?shop_id=10&has_market_stocks=false', expected_resp)


def test_filter_by_allow_model_create_update(stroller):
    expected_resp = _expected_response(23, ['o231'], [230])
    _check_response(stroller, '/v1/partners/23/offers?allow_model_create_update=true', expected_resp)

    expected_resp = _expected_response(23, ['o232', 'o233'], [230])
    _check_response(stroller, '/v1/partners/23/offers?allow_model_create_update=false', expected_resp)


def test_calc_integral_status_without_filters(stroller):
    ''' Проверяем, что для вычисления интегрального статуса зачитываются
    все складские части вне зависимости от их присутствия в ответе ручки
    (у оффера без складских частей интегральный статус = NOT_PUBLISHED_CHECKING)
    '''
    result_statuses = {
        10: DTC.OfferStatus.NOT_PUBLISHED_DISABLED_BY_PARTNER,
        20: DTC.OfferStatus.NOT_PUBLISHED_DISABLED_AUTOMATICALLY
    }

    # without filters
    expected_resp = _expected_response(24, ['o1'], [10, 20], result_statuses=result_statuses)
    _check_response(stroller, '/v1/partners/24/offers', expected_resp)

    expected_resp = _expected_response(24, ['o1'], [10], result_statuses=result_statuses)
    _check_response(stroller, '/v1/partners/24/offers?shop_id=10', expected_resp)

    expected_resp = _expected_response(24, ['o1'], [20], [2000], result_statuses=result_statuses)
    _check_response(stroller, '/v1/partners/24/offers?shop_id=20&warehouse_id=2000', expected_resp)

    # with filters
    expected_resp = _expected_response(24, ['o1'], [10, 20], result_statuses=result_statuses)
    _check_response(stroller, '/v1/partners/24/offers?vendor=vendor_1', expected_resp)

    expected_resp = _expected_response(24, ['o1'], [10], result_statuses=result_statuses)
    _check_response(stroller, '/v1/partners/24/offers?shop_id=10&result_status=4', expected_resp)

    expected_resp = _expected_response(24, None, [])
    _check_response(stroller, '/v1/partners/24/offers?shop_id=10&result_status=5', expected_resp)

    expected_resp = _expected_response(24, ['o1'], [20], [2000], result_statuses=result_statuses)
    _check_response(stroller, '/v1/partners/24/offers?shop_id=20&result_status=8&warehouse_id=2000', expected_resp)
