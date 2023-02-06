# coding: utf-8

import pytest
import six
from hamcrest import assert_that, equal_to, has_entries, raises, calling, empty, not_none

import yt.yson as yson

import market.idx.datacamp.proto.offer.DataCampOffer_pb2 as DTC
import market.idx.datacamp.proto.api.SyncGetOffer_pb2 as SyncGetOffer
import market.idx.datacamp.controllers.stroller.proto.config_pb2 as Config

from market.idx.datacamp.controllers.stroller.yatf.test_env import make_stroller
from market.idx.datacamp.yatf.matchers.matchers import HasStatus
from market.idx.yatf.matchers.protobuf_matchers import IsSerializedProtobuf, IsProtobuf
from market.idx.pylibrary.datacamp.conversion import offer_to_basic_row, offer_to_service_row


def _gen_basic_search_row(business_id, shop_sku, offer=None):
    row = {}
    row['business_id'] = business_id
    row['shop_sku'] = shop_sku
    row['removed'] = False

    # значения для дефолтного интегрального статуса NOT_PUBLISHED_PARTNER_IS_DISABLED
    row['integral_status_fields'] = {
        'actual_content_version': yson.YsonUint64(11),
        'has_market_sku_id': True,
        'master_data_version': yson.YsonUint64(33),
        'original_partner_data_version': yson.YsonUint64(22),
        'version_from_content_system': yson.YsonUint64(11),
        'version_from_master_data': yson.YsonUint64(33)
    }

    row['ts_first_added'] = 1648982724

    if offer:
        row.update(offer)
    return row


def _gen_service_search_row(business_id, shop_sku, shop_id, warehouse_id=0, default_integral_status=True, offer=None):
    row = {}
    row['business_id'] = business_id
    row['shop_sku'] = shop_sku
    row['shop_id'] = shop_id
    row['removed'] = False

    if warehouse_id:
        row['warehouse_id'] = warehouse_id
        row['outlet_id'] = 0
    else:
        # значения для дефолтного интегрального статуса PUBLISHED
        row['disabled_by'] = []
        row['partner_info'] = {
            "has_warehousing": False,
            "is_disabled": False,
            "is_ignore_stocks": False,
            "is_preproduction": False,
            "program_type": yson.YsonUint64(0)
        }
        row['integral_status_fields'] = {
            'master_data_version': yson.YsonUint64(1),
            'original_partner_data_version': yson.YsonUint64(1),
            'rgb': yson.YsonUint64(2),  # BLUE
            # CONTENT_STATE_READY if default_integral_status else CONTENT_STATE_REJECTED
            'service_offer_state': yson.YsonUint64(8) if default_integral_status else yson.YsonUint64(6),
            'version_from_master_data': yson.YsonUint64(0),
            'warehouse_params': {
                'all_actual_disabled_by_stock': False,
                'all_partner_changes_are_not_mined': False,
                'all_partner_changes_are_not_published': True,
                'has_approved_disables_by_publication': False,
                'has_approved_miner_blue_msku_problems': False,
                'has_approved_miner_disables': False,
                'has_auto_disabled_warehouse_parts_by_not_versioned_source': False,
                'has_no_zero_warehouse': False,
                'has_partner_changes_are_not_mined': False,
                'has_partner_changes_are_not_published': False,
                'has_partner_changes_successfully_published': True,
                'has_successfully_published': True,
                'has_warehouse_parts': True,
                'miner_errors': []
            }
        }

    if offer:
        row.update(offer)
    return row


def _gen_service_row(business_id, shop_sku, shop_id, warehouse_id=0):
    return offer_to_service_row(DTC.Offer(
        identifiers=DTC.OfferIdentifiers(
            business_id=business_id,
            shop_id=shop_id,
            warehouse_id=warehouse_id,
            offer_id=shop_sku
        )
    ))


def _expected_response(business_id, offer_ids, shop_ids=None, actuals=None, stocks=None, result_statuses=dict(), **kwargs):
    return dict(
        offers=empty() if offer_ids is None else [
            {
                'basic': {
                    'identifiers': {
                        'business_id': business_id,
                        'offer_id': offer_id,
                    },
                },
                'service': empty() if shop_ids is None else has_entries({
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
            'vendor': 'vendor_2',
            'removed': True
        },
    },
    {
        'business_id': 3,
        'offer_id': 'o04',
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
            'partner_category_ids': [yson.YsonUint64(1), yson.YsonUint64(2)]
        },
    },
    {
        'business_id': 4,
        'offer_id': 'o02',
        'basic': {
            'partner_category_ids': [yson.YsonUint64(1), yson.YsonUint64(3)]
        },
    },
    {
        'business_id': 4,
        'offer_id': 'o03',
        'basic': {
            'partner_category_ids': [yson.YsonUint64(4)]
        },
    },
] + [
    # filtering by content cpa state
    {
        'business_id': 5,
        'offer_id': 'o01',
        'basic': {
            'cpa_state': DTC.OfferContentCpaState.CONTENT_STATE_READY
        },
    },
    {
        'business_id': 5,
        'offer_id': 'o02',
        'basic': {
            'cpa_state': DTC.OfferContentCpaState.CONTENT_STATE_REJECTED,
        },
    }
] + [
    # filtering by disabled status
    {
        'business_id': 7,
        'offer_id': 'o01',
        'service': {
            70: {'disabled_by': [yson.YsonUint64(DTC.PUSH_PARTNER_API)]}
        },
    },
    {
        'business_id': 7,
        'offer_id': 'o02',
        'service': {
            70: {'disabled_by': []}
        },
    },
    {
        'business_id': 7,
        'offer_id': 'o03',
        'service': {
            70: {'disabled_by': [yson.YsonUint64(DTC.PUSH_PARTNER_FEED)]}
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
            80: {'disabled_by_partner': False},
        },
    },
    {
        'business_id': 8,
        'offer_id': 'o02',
        'service': {
            80: {'disabled_by_partner': True},
        },
    },
    {
        'business_id': 8,
        'offer_id': 'o03',
        'service': {
            90: {'disabled_by_partner': False},
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
            'integral_content_status': DTC.ResultContentStatus.HAS_CARD_MARKET
        },
    },
    {
        'business_id': 12,
        'offer_id': 'o02',
        'basic': {
            'integral_content_status': DTC.ResultContentStatus.NO_CARD_NEED_CONTENT,
        },
    }
] + [
    # filtering by price
    {
        'business_id': 13,
        'offer_id': 'o01',
        'service': {
            10: {
                'has_price': False
            }
        }
    },
    {
        'business_id': 13,
        'offer_id': 'o02',
        'service': {
            10: {
                'has_price': True
            }
        }
    }
] + [
    # filtering by market_stocks
    {
        'business_id': 14,
        'offer_id': 'o01',
        'service': {
            10: {}
        },
        'actual': {
            10: {
                'warehouse': {
                    1001: {
                        'has_market_stocks': False
                    },
                }
            }
        }
    },
    {
        'business_id': 14,
        'offer_id': 'o02',
        'service': {
            10: {}
        },
        'actual': {
            10: {
                'warehouse': {
                    1001: {
                        'has_market_stocks': True
                    },
                    1002: {
                        'has_market_stocks': True
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
            'partner_category_ids': [yson.YsonUint64(1), yson.YsonUint64(2)],
            'market_category_id':  1
        }
    },
    {
        'business_id': 15,
        'offer_id': 'o02',
        'basic': {
            'partner_category_ids': [yson.YsonUint64(1), yson.YsonUint64(2)],
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
                'has_price': True
            }
        },
        'actual': {
            10: {
                'warehouse': {
                    1001: {
                        'has_market_stocks': False
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
                'has_price': False
            }
        },
        'actual': {
            10: {
                'warehouse': {
                    1002: {
                        'has_market_stocks': True
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
                'has_price': True
            }
        }
    },
    {
        'business_id': 17,
        'offer_id': 'o02',
        'service': {
            1: {
                'has_price': False
            }
        }
    },
    {
        'business_id': 17,
        'offer_id': 'o03',
        'service': {
            1: {
                'has_price': False
            }
        }
    },
    {
        'business_id': 17,
        'offer_id': 'o04',
        'service': {
            2: {
                'has_price': True
            }
        }
    },
    {
        'business_id': 17,
        'offer_id': 'o05',
        'service': {
            2: {
                'has_price': False
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
                'has_price': True
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
                'has_price': True
            }
        },
        'actual': {
            10: {
                'warehouse': {
                    1001: {
                        'has_market_stocks': False
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
                'has_price': True
            },
            20: {
                'has_price': True
            }
        },
        'actual': {
            10: {
                'warehouse': {
                    1001: {
                        'has_market_stocks': False
                    },
                    1002: {
                        'has_market_stocks': True
                    }
                }
            },
            20: {
                'warehouse': {
                    1002: {
                        'has_market_stocks': True
                    }
                }
            },
            30: {
                'warehouse': {
                    1002: {
                        'has_market_stocks': True
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
        'default_integral_status': False,
        'service': {
            200: {}
        },
    } for offer_id in range(0, Config.TGeneral().SelectSizeInListOffersHandler)
] + [
    # scan_limit and paging
    {
        'business_id': 20,
        'offer_id': '{:05d}'.format(offer_id),
        'default_integral_status': True,
        'service': {
            200: {}
        }
    } for offer_id in range(Config.TGeneral().SelectSizeInListOffersHandler, Config.TGeneral().SelectSizeInListOffersHandler + 500)
] + [
    # scan_limit and paging
    {
        'business_id': 20,
        'offer_id': '{:05d}'.format(offer_id),
        'default_integral_status': False,
        'service': {
            200: {}
        },
    } for offer_id in range(Config.TGeneral().SelectSizeInListOffersHandler + 500, Config.TGeneral().SelectSizeInListOffersHandler * 2)
] + [
    # scan_limit and paging
    {
        'business_id': 20,
        'offer_id': '{:05d}'.format(offer_id),
        'default_integral_status': True,
        'service': {
            200: {}
        }
    } for offer_id in range(Config.TGeneral().SelectSizeInListOffersHandler * 2, Config.TGeneral().SelectSizeInListOffersHandler * 3)
] + [
    # scan_limit and paging
    {
        'business_id': 20,
        'offer_id': '{:05d}'.format(offer_id),
        'default_integral_status': False,
        'service': {
            200: {}
        }
    } for offer_id in range(Config.TGeneral().SelectSizeInListOffersHandler * 3, Config.TGeneral().SelectSizeInListOffersHandler * 10)
] + [
    # scan_limit and paging
    {
        'business_id': 20,
        'offer_id': '{:05d}'.format(Config.TGeneral().SelectSizeInListOffersHandler * 10 + 1),
        'default_integral_status': True,
        'service': {
            200: {}
        }
    }
] + [
    {
        'business_id': 21,
        'offer_id': 'o03',
        'service': {
            10: {
                'has_price': True
            },
            20: {
                'has_price': True
            }
        },
        'actual': {
            10: {
                'warehouse': {
                    1002: {
                        'has_market_stocks': True
                    }
                }
            },
            20: {
                'warehouse': {
                    1002: {
                        'has_market_stocks': False
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
                'has_price': True
            },
            20: {
                'has_price': True
            }
        },
        'actual': {
            10: {
                'warehouse': {
                    1001: {
                        'has_market_stocks': False
                    },
                    1002: {
                        'has_market_stocks': True
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
    }
] + [
    # filtering by include_shop_id + exclude_shop_id
    {
        'business_id': 24,
        'offer_id': 'o01',
        'service': {
            10: {},
        },
    },
    {
        'business_id': 24,
        'offer_id': 'o02',
        'basic': {
            'market_category_id':  1
        },
        'service': {
            10: {},
            30: {},
        },
    },
    {
        'business_id': 24,
        'offer_id': 'o03',
        'service': {
            20: {},
            40: {},
        },
    },
    {
        'business_id': 24,
        'offer_id': 'o04',
        'service': {
            40: {},
        },
    }
] + [
    {
        'business_id': 22,
        'offer_id': 'o33',
        'service': {
            10: {
            },
            20: {
            }
        },
        'actual': {
            10: {
                'warehouse': {
                    1001: {
                        'has_market_stocks': False
                    },
                    1002: {
                        'has_market_stocks': True
                    }
                }
            }
        }
    }
] + [
    # filtering by verdicts
    {
        'business_id': 26,
        'offer_id': 'o01',
        'service': {
            30: {},
        },
    },
    {
        'business_id': 26,
        'offer_id': 'o02',
        'basic': {
            'vendor': 'vendor_2'
        },
        'service': {
            30: {},
        },
    },
    {
        'business_id': 26,
        'offer_id': 'o03',
        'basic': {
            'removed': True
        },
        'service': {
            30: {},
        },
    },
    {
        'business_id': 26,
        'offer_id': 'o04',
        'service': {
            30: {},
        },
    },
]


def crossdock_warehouse_id(shop_id):
    return shop_id * 100


def ff_warehouse_id():
    return 145


@pytest.fixture(scope='module', params=[True])
def enable_search_tables(request):
    return request.param


@pytest.fixture(scope='module')
def basic_offers():
    offers = [offer_to_basic_row(DTC.Offer(
        identifiers=DTC.OfferIdentifiers(
            business_id=o['business_id'],
            offer_id=o['offer_id']
        ))) for o in OFFERS
    ]

    offers.extend([
        # for integral status in handler`s answer
        offer_to_basic_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=25,
                offer_id='o1'
            )
        ))
    ])

    offers.extend([
        # for verdicts filtering
        offer_to_basic_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=26,
                offer_id='o01'
            ),
        resolution=DTC.Resolution(
            by_source=[
                DTC.Verdicts(
                    meta=DTC.UpdateMeta(source=DTC.MARKET_NORDSTREAM),
                    verdict=[DTC.Verdict(results=[DTC.ValidationResult(
                        messages=[DTC.Explanation(code='39A')]
                    )])]
                )
            ]
        )))
    ])

    offers.extend([
        # for verdicts filtering with mdm specific logic
        offer_to_basic_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=26,
                offer_id='o04'
            ),
        resolution=DTC.Resolution(
            by_source=[
                DTC.Verdicts(
                    meta=DTC.UpdateMeta(source=DTC.MARKET_MDM),
                    verdict=[DTC.Verdict(results=[DTC.ValidationResult(
                        applications=[DTC.DBS],
                        messages=[DTC.Explanation(code='test.error')]
                    )])]
                )
            ]
        )))
    ])

    return offers


@pytest.fixture(scope='module')
def service_offers():
    offers = [_gen_service_row(o['business_id'], o['offer_id'], shop_id, 0)
              for o in OFFERS for shop_id in list(o.get('service', dict()).keys()) or [(o['business_id'] * 10)]]

    offers.extend([
        # for integral status in handler`s answer
        offer_to_service_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=25,
                shop_id=10,
                offer_id='o1'
            ),
            meta=DTC.OfferMeta(
                rgb=DTC.BLUE
            ),
            status=DTC.OfferStatus(
                disabled=[DTC.Flag(
                    flag=True,
                    meta=DTC.UpdateMeta(source=DTC.PUSH_PARTNER_FEED)
                )]
            )
        )),
        offer_to_service_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=25,
                shop_id=20,
                offer_id='o1'
            ),
            meta=DTC.OfferMeta(
                rgb=DTC.BLUE
            ),
        ))
    ])

    offers.extend([
        offer_to_service_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=26,
                shop_id=30,
                warehouse_id=0,
                offer_id='o02'
            ),
            resolution=DTC.Resolution(
                by_source=[
                    DTC.Verdicts(
                        meta=DTC.UpdateMeta(source=DTC.MARKET_IDX),
                        verdict=[DTC.Verdict(results=[DTC.ValidationResult(
                            messages=[DTC.Explanation(code='451')]
                        )])]
                    )
                ]
            ),
        ))
    ])

    offers.extend([
        # for verdicts filtering with mdm specific logic
        offer_to_service_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=26,
                shop_id=30,
                warehouse_id=0,
                offer_id='o04'
            ),
            partner_info=DTC.PartnerInfo(is_dsbs=True),
        ))
    ])

    return offers


@pytest.fixture(scope='module')
def actual_service_offers():
    offers = []
    for o in OFFERS:
        if 'actual' in o:
            for shop_id, warehouses in list(o['actual'].items()):
                for whid in list(warehouses['warehouse'].keys()):
                    offers.append(_gen_service_row(o['business_id'], o['offer_id'], shop_id, whid))
        else:
            for shop_id in list(o.get('service', dict()).keys()) or [(o['business_id'] * 10)]:
                offers.append(_gen_service_row(o['business_id'], o['offer_id'], shop_id, crossdock_warehouse_id(shop_id)))
                offers.append(_gen_service_row(o['business_id'], o['offer_id'], shop_id, ff_warehouse_id()))

    offers.extend([
        # for integral status in handler`s answer
        offer_to_service_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=25,
                shop_id=10,
                warehouse_id=1000,
                offer_id='o1'
            ),
        )),
        offer_to_service_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=25,
                shop_id=20,
                warehouse_id=2000,
                offer_id='o1'
            ),
        )),
        offer_to_service_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=25,
                shop_id=20,
                warehouse_id=1000,
                offer_id='o1'
            ),
            status=DTC.OfferStatus(
                disabled=[DTC.Flag(
                    flag=True,
                    meta=DTC.UpdateMeta(source=DTC.MARKET_ABO)
                )]
            )
        ))
    ])

    return offers


@pytest.fixture(scope='module')
def basic_search_offers():
    result = [_gen_basic_search_row(o['business_id'], o['offer_id'], o.get('basic', dict())) for o in OFFERS]
    result += [_gen_basic_search_row(25, 'o1')]
    return result


@pytest.fixture(scope='module')
def service_search_offers():
    result = [_gen_service_search_row(o['business_id'], o['offer_id'], shop_id, 0, o.get('default_integral_status', True), so)
              for o in OFFERS for shop_id, so in list(o.get('service', dict()).items()) or [(o['business_id'] * 10, dict())]]
    result += [_gen_service_search_row(25, 'o1', 20, 0), _gen_service_search_row(25, 'o1', 10, 0)]
    return result


@pytest.fixture(scope='module')
def actual_service_search_offers():
    offers = []
    for o in OFFERS:
        if 'actual' in o:
            for shop_id, warehouses in list(o['actual'].items()):
                for whid, warehouse in list(warehouses['warehouse'].items()):
                    offers.append(_gen_service_search_row(o['business_id'], o['offer_id'], shop_id, whid, offer=warehouse))
        else:
            for shop_id in list(o.get('service', dict()).keys()) or [(o['business_id'] * 10)]:
                offers.append(_gen_service_search_row(o['business_id'], o['offer_id'], shop_id, crossdock_warehouse_id(shop_id)))
                offers.append(_gen_service_search_row(o['business_id'], o['offer_id'], shop_id, ff_warehouse_id()))

    offers += [
        _gen_service_search_row(25, 'o1', 20, 1000),
        _gen_service_search_row(25, 'o1', 20, 2000),
        _gen_service_search_row(25, 'o1', 10, 1000)
    ]
    return offers


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
        actual_service_search_offers_table
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
            actual_service_search_offers_table=actual_service_search_offers_table,
    ) as stroller_env:
        yield stroller_env


def _check_response(stroller, uri, expected_resp):
    response = stroller.get(uri)
    assert_that(response, HasStatus(200))
    assert_that(response.data, IsSerializedProtobuf(SyncGetOffer.GetUnitedOffersResponse, expected_resp))
    assert_that(response.headers['Content-type'], equal_to('application/x-protobuf'))


def _check_failed_response(stroller, uri, code):
    response = stroller.get(uri)
    assert_that(response, HasStatus(code))
    assert_that(six.ensure_str(response.data), equal_to(''))


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
    expected_resp = _expected_response(3, ['o02'], [30], previous_page_position=None, current_page_position=None, next_page_position='o04', limit=1)
    _check_response(stroller, '/v1/partners/3/offers?vendor=vendor_2&limit=1', expected_resp)

    expected_resp = _expected_response(3, ['o04'], [30], previous_page_position='o02', current_page_position='o04', next_page_position=None, limit=1)
    _check_response(stroller, '/v1/partners/3/offers?vendor=vendor_2&limit=1&position=o04', expected_resp)


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


def test_include_and_exclude_shop_id_filter(stroller):
    expected_resp = _expected_response(24, ['o02'], [10, 30], current_page_position=None, next_page_position=None)
    _check_response(stroller, '/v1/partners/24/offers?include_shop_id=20&include_shop_id=30&exclude_shop_id=40&market_category_id=1', expected_resp)


def test_filter_by_price(stroller):
    no_price_resp = _expected_response(13, ['o01'], [10])
    _check_response(stroller, '/v1/partners/13/offers?shop_id=10&has_price=false', no_price_resp)

    has_price_resp = _expected_response(13, ['o02'], [10])
    _check_response(stroller, '/v1/partners/13/offers?shop_id=10&has_price=true', has_price_resp)


def test_filter_by_market_stocks(stroller):
    whid = 1001

    no_stocks_response = _expected_response(14, ['o01'], [10], [1001], [1001])
    _check_response(stroller, '/v1/partners/14/offers?shop_id=10&warehouse_id={}&has_market_stocks=false'.format(whid), no_stocks_response)

    has_stocks_response = _expected_response(14, ['o02'], [10], [1001], [1001])
    _check_response(stroller, '/v1/partners/14/offers?shop_id=10&warehouse_id={}&has_market_stocks=true'.format(whid), has_stocks_response)

    whid = 1002

    no_stocks_resp = _expected_response(14, None, [])
    _check_response(stroller, '/v1/partners/14/offers?shop_id=10&warehouse_id={}&has_market_stocks=false'.format(whid), no_stocks_resp)

    has_stocks_resp = _expected_response(14, ['o02'], [10], [1002], [1002])
    _check_response(stroller, '/v1/partners/14/offers?shop_id=10&warehouse_id={}&has_market_stocks=true'.format(whid), has_stocks_resp)


def test_filter_by_market_category_id_binding(stroller):
    expected_resp = _expected_response(15, ['o01'], [150], current_page_position=None, next_page_position=None)
    _check_response(stroller, '/v1/partners/15/offers?market_category_id=1', expected_resp)


def test_filter_by_price_and_by_market_stocks(stroller):
    no_price_no_stocks_response = _expected_response(16, None, [])
    _check_response(stroller, '/v1/partners/16/offers?shop_id=10&warehouse_id=1001&has_price=false&has_market_stocks=false', no_price_no_stocks_response)

    has_price_no_stocks_response = _expected_response(16, ['o01'], [10], [1001], [])
    _check_response(stroller, '/v1/partners/16/offers?shop_id=10&warehouse_id=1001&has_price=true&has_market_stocks=false', has_price_no_stocks_response)

    no_price_has_stocks_response = _expected_response(16, ['o02'], [10], [1002], [])
    _check_response(stroller, '/v1/partners/16/offers?shop_id=10&warehouse_id=1002&has_price=false&has_market_stocks=true', no_price_has_stocks_response)

    has_price_has_stocks_response = _expected_response(16, None, [])
    _check_response(stroller, '/v1/partners/16/offers?shop_id=10&warehouse_id=1002&has_price=true&has_market_stocks=true', has_price_has_stocks_response)


def test_default_paging_with_shop_id_and_has_price_filter(stroller):
    # проверяем пагинацию по-умолчанию c фильрами по shop_id и наличию цены
    expected_resp = _expected_response(17, ['o04'], [2], current_page_position=None, next_page_position=None, offset=0)
    _check_response(stroller, '/v1/partners/17/offers?limit=2&shop_id=2&has_price=true', expected_resp)

    expected_resp = _expected_response(17, None, [])
    _check_response(stroller, '/v1/partners/17/offers?limit=2&shop_id=2&has_price=true&offset=1', expected_resp)


def test_paging_with_shop_id_and_has_price_filter(stroller):
    # проверяем пагинацию c фильтрами по shop_id и наличию цены
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


def test_scan_limit_with_integral_status_filter(stroller, full):
    # Параметр scan_limit имеет смысл только при запросах с фильтром по интегральному статусу
    expected_resp = _expected_response(
        20,
        None,
        [200],
        previous_page_position=None,
        current_page_position=None,
        next_page_position='{:05d}'.format(Config.TGeneral().SelectSizeInListOffersHandler - 1),
        limit=2
    )
    _check_response(stroller, '/v1/partners/20/offers?shop_id=200&result_status=1&scan_limit={:05d}&limit=2&full={}'.format(Config.TGeneral().SelectSizeInListOffersHandler, full), expected_resp)

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
        '/v1/partners/20/offers?shop_id=200&result_status=1&scan_limit=100000&limit=2&position={:05d}&full={}'.format(Config.TGeneral().SelectSizeInListOffersHandler, full),
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
        previous_page_position='{:05d}'.format(Config.TGeneral().SelectSizeInListOffersHandler),
        current_page_position='{:05d}'.format(Config.TGeneral().SelectSizeInListOffersHandler + 2),
        next_page_position='{:05d}'.format(Config.TGeneral().SelectSizeInListOffersHandler * 6 + 1),
    )
    _check_response(
        stroller,
        '/v1/partners/20/offers?shop_id=200&result_status=1&scan_limit=5000&limit=100000&position={:05d}&full={}'.format(Config.TGeneral().SelectSizeInListOffersHandler + 2, full),
        expected_resp
    )


def test_filter_by_allow_model_create_update(stroller):
    expected_resp = _expected_response(23, ['o231'], [230])
    _check_response(stroller, '/v1/partners/23/offers?allow_model_create_update=true', expected_resp)

    expected_resp = _expected_response(23, ['o232'], [230])
    _check_response(stroller, '/v1/partners/23/offers?allow_model_create_update=false', expected_resp)


def test_filter_by_integral_status(stroller):
    offer_id = '{:05d}'.format(0)
    expected_resp = _expected_response(20, [offer_id], [200])
    _check_response(stroller, '/v1/partners/20/offers?shop_id=200&result_status=6&limit=1', expected_resp)

    offer_id = '{:05d}'.format(Config.TGeneral().SelectSizeInListOffersHandler)
    expected_resp = _expected_response(20, [offer_id], [200])
    _check_response(stroller, '/v1/partners/20/offers?shop_id=200&result_status=1&limit=1', expected_resp)

    expected_resp = _expected_response(20, None, [200])
    _check_response(stroller, '/v1/partners/20/offers?shop_id=200&result_status=2&limit=1', expected_resp)


def test_calc_integral_status_without_filters(stroller):
    ''' Интегральный статус не возвращается для запросов с фильтрами.
    Проверяем, что для вычисления интегрального статуса зачитываются
    все складские части вне зависимости от их присутствия в ответе ручки
    (у оффера без складских частей интегральный статус = NOT_PUBLISHED_CHECKING)
    '''
    result_statuses = {
        10: DTC.OfferStatus.NOT_PUBLISHED_DISABLED_BY_PARTNER,
        20: DTC.OfferStatus.NOT_PUBLISHED_DISABLED_AUTOMATICALLY
    }

    expected_resp = _expected_response(25, ['o1'], [10, 20], result_statuses=result_statuses)
    _check_response(stroller, '/v1/partners/25/offers', expected_resp)

    expected_resp = _expected_response(25, ['o1'], [10], result_statuses=result_statuses)
    _check_response(stroller, '/v1/partners/25/offers?shop_id=10', expected_resp)

    expected_resp = _expected_response(25, ['o1'], [20], [2000], result_statuses=result_statuses)
    _check_response(stroller, '/v1/partners/25/offers?shop_id=20&warehouse_id=2000', expected_resp)

    expected_resp = _expected_response(25, ['o1'], [20], result_statuses=result_statuses)
    _check_response(stroller, '/v1/partners/25/offers?shop_id=20&offer_id=o1', expected_resp)


def test_ignore_removed_offers(stroller):
    expected_resp = _expected_response(3, ['o02', 'o04'], [30], current_page_position=None, next_page_position=None)
    _check_response(stroller, '/v1/partners/3/offers?vendor=vendor_2', expected_resp)


def test_allow_removed_without_filters(stroller):
    expected_resp = _expected_response(3, ['o01', 'o02', 'o03', 'o04'], [30], current_page_position=None, next_page_position=None)
    _check_response(stroller, '/v1/partners/3/offers?allow_removed=true', expected_resp)


def test_allow_removed_with_filters(stroller):
    expected_resp = _expected_response(3, ['o02', 'o03', 'o04'], [30], current_page_position=None, next_page_position=None)
    _check_response(stroller, '/v1/partners/3/offers?vendor=vendor_2&allow_removed=true', expected_resp)


def test_first_added_filter(stroller):
    expected_resp = _expected_response(3, ['o02', 'o04'], [30], current_page_position=None, next_page_position=None)
    _check_response(stroller, '/v1/partners/3/offers?ts_first_added_from=1648982724&ts_first_added_to=1650020000', expected_resp)


def test_first_added_filter_empty(stroller):
    expected_resp = _expected_response(3, [], [30], current_page_position=None, next_page_position=None)
    _check_response(stroller, '/v1/partners/3/offers?ts_first_added_to=1247020000', expected_resp)


def test_first_added_filter_from(stroller):
    expected_resp = _expected_response(3, ['o02', 'o04'], [30], current_page_position=None, next_page_position=None)
    _check_response(stroller, '/v1/partners/3/offers?ts_first_added_from=1247020000', expected_resp)


def test_only_basic(stroller):
    '''
    Тест параметра only_basic=true. Убеждаемся, что:
    - получаем только базовую часть оффера;
    - разрешены фильтры по базовой части (vendor, category_id, cpa, ...);
    - запрещены фильтры по сервисной части (disabled_by, result_status, ...)
      и актуальной сервисной части (has_market_stocks);
      Т.е если хочешь фильтровать по ним, то грузи их
    - комбинация параметров full=true и only_basic=true невалидна
    '''
    _check_response(stroller, '/v1/partners/3/offers?vendor=vendor_1&only_basic=true', _expected_response(3, ['o01']))
    _check_response(stroller, '/v1/partners/3/offers?ts_first_added_from=1648982724&ts_first_added_to=1650020000&only_basic=true', _expected_response(3, ['o02', 'o04']))
    _check_response(stroller, '/v1/partners/4/offers?category_id=1&only_basic=true', _expected_response(4, ['o01', 'o02']))
    _check_response(stroller, '/v1/partners/4/offers?category_id=2&only_basic=true', _expected_response(4, ['o01']))
    _check_response(stroller, '/v1/partners/4/offers?category_id=3&only_basic=true', _expected_response(4, ['o02']))
    _check_response(stroller, '/v1/partners/5/offers?cpa=8&only_basic=true', _expected_response(5, ['o01']))
    _check_response(stroller, '/v1/partners/12/offers?integral_content_status=1&only_basic=true', _expected_response(12, ['o01']))
    _check_response(stroller, '/v1/partners/15/offers?market_category_id=1&only_basic=true', _expected_response(15, ['o01']))
    _check_response(stroller, '/v1/partners/23/offers?allow_model_create_update=true&only_basic=true', _expected_response(23, ['o231']))
    _check_response(stroller, '/v1/partners/23/offers?allow_model_create_update=false&only_basic=true', _expected_response(23, ['o232']))

    _check_failed_response(stroller, '/v1/partners/7/offers?disabled_by=3&limit=1&only_basic=true', 400)
    _check_failed_response(stroller, '/v1/partners/14/offers?has_market_stocks=false&only_basic=true', 400)
    _check_failed_response(stroller, '/v1/partners/9/offers?supply_plan=2&only_basic=true', 400)
    _check_failed_response(stroller, '/v1/partners/13/offers?has_price=false&only_basic=true', 400)

    _check_failed_response(stroller, '/v1/partners/3/offers?only_basic=true&full=true', 400)


def test_verdict_parameter(stroller):
    # параметр verdict работает только в режиме search_tables=true
    _check_failed_response(stroller, '/v1/partners/26/offers?verdict=34&category_id=3&search_tables=false', 400)

    # проверяем, что работает вместе с другими фильтрами (vendor)
    expected_resp = _expected_response(26, ['o02'], [30], current_page_position=None, next_page_position=None)
    _check_response(stroller, '/v1/partners/26/offers?shop_id=30&verdict=477300286&verdict=23182371&vendor=vendor_2', expected_resp)
    # проверяем, что работает в режиме с offer_id
    _check_response(stroller, '/v1/partners/26/offers?shop_id=30&verdict=23182371&offer_id=o02&offer_id=o03', expected_resp)
    # проверяем, что работает режим фильтрации без доп. фильтров
    _check_response(stroller, '/v1/partners/26/offers?shop_id=30&verdict=6789', _expected_response(26, None, [30]))

    # проверяем фильтр по вердиктам - мдм вердикт в базовой части оффера должен "прыгнуть" на дбс сервисную часть
    expected_resp = _expected_response(26, ['o04'], [30], current_page_position=None, next_page_position=None)
    _check_response(stroller, '/v1/partners/26/offers?shop_id=30&verdict=3020481658', expected_resp)
