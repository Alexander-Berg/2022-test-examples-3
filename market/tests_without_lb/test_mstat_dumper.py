# coding: utf-8

from hamcrest import assert_that, equal_to, has_items, has_entries, not_none, is_not
import pytest

import yt.wrapper as yt

from market.idx.datacamp.routines.yatf.resources.config_mock import RoutinesConfigMock
from market.idx.datacamp.routines.yatf.test_env import MStatDumperEnv
import market.idx.datacamp.proto.offer.DataCampOffer_pb2 as DTC
from market.idx.pylibrary.datacamp.conversion import offer_to_service_row
from market.idx.yatf.matchers.yt_rows_matchers import (
    HasDatacampYtUnitedOffersRows,
    HasOffers,
)
from market.idx.yatf.matchers.yson_matcher import IsSerializedYson
from market.pylibrary.proto_utils import message_from_data

SERVICE_OFFERS = [
    # под офферами с business_id [1, 3] есть CPA, должны попасть в выдачу, оффера с business_id [4, 5) не должны попасть в выдачу
    {
        'identifiers': {
            'business_id': business_id,
            'offer_id': 'T600',
            'shop_id': business_id * 10,
            'feed_id': 1000,
        }
    } for business_id in range(1, 5)
] + [
    # два магазина с двумя складами каждый, все с CPA, попасть в выдачу должны по 1 разу
    {
        'identifiers': {
            'business_id': 6,
            'offer_id': 'T100',
            'shop_id': 60 + i,
            'feed_id': 1000,
        },
        'meta': {
            'rgb': DTC.DIRECT_SEARCH_SNIPPET_GALLERY
        }
    } for i in range(0, 2)
] + [
    {
        'identifiers': {
            'business_id': 10,
            'offer_id': 'T1000000',
            'shop_id': 63,
            'feed_id': 1000,
        }
    }
] + [
    {
        'identifiers': {
            'business_id': 10802214,
            'offer_id': 'OfferForClassififerMagicIdCheck',
            'shop_id': 10806906,
            'feed_id': 1000
        }
    }
] + [
    # Офферы должны попасть в выгрузку отсортированными по shop_id внутри одного business_id
    {
        'identifiers': {
            'business_id': 10802215,
            'offer_id': 'OfferForOrderChecking',
            'shop_id': 1,
            'feed_id': 1000
        }
    }
] + [
    {
        'identifiers': {
            'business_id': 10802215,
            'offer_id': 'OfferForOrderChecking',
            'shop_id': 2,
            'feed_id': 1000
        }
    }
] + [
    {
        'identifiers': {
            'business_id': 10802215,
            'offer_id': 'OfferForOrderChecking',
            'shop_id': 3,
            'feed_id': 1000
        }
    }
] + [
    {
        'identifiers': {
            'business_id': 10802215,
            'offer_id': 'OfferForOrderChecking',
            'shop_id': 4,
            'feed_id': 1000
        }
    }
] + [
    # Оффер должен попасть в контрактную выгрузку, не смотря на то, что его нет в актуальной таблице
    {
        'identifiers': {
            'business_id': 10802216,
            'offer_id': 'OfferWithoutActualPartCpa',
            'shop_id': 10806908,
            'feed_id': 1000
        }
    }
] + [
    # В контрактной выгрузке не должно быть дубля из-за того, что этот оффер есть и в актуальной части
    {
        'identifiers': {
            'business_id': 10802217,
            'offer_id': 'OfferWithActualAndOriginalCpa',
            'shop_id': 10806909,
            'feed_id': 1000
        }
    }
] + [
    # Невидимый оффер не должен попадать в выгрузки
    {
        'identifiers': {
            'business_id': 1090,
            'offer_id': 'InvisibleOffer',
            'shop_id': 1091,
            'feed_id': 347
        }
    }
]


@pytest.fixture(scope='module')
def config(yt_server):
    config = RoutinesConfigMock(
        yt_server,
        config={
            'general': {
                'yt_home': '//home/datacamp/united',
            },
            'mstat_dumper': {
                'enable': True,
                'yt_proxies': [yt_server.get_yt_client().config["proxy"]["url"]],
                'output_dir': 'filtered',
            },
        })
    return config


@pytest.fixture(scope='module')
def basic_offers_table_data():
    return [
        # под офферами с business_id [1, 3] есть CPA, должны попасть в выдачу, оффера с business_id [4, 5) не должны попасть в выдачу
        {
            'identifiers': {
                'business_id': business_id,
                'offer_id': 'T600',
                'feed_id': 1000,
            }
        } for business_id in range(1, 5)
    ] + [
        # есть CPA, должны попасть в выдачу
        {
            'identifiers': {
                'business_id': 6,
                'offer_id': 'T100',
            }
        },
        {
            'identifiers': {
                'business_id': 10,
                'offer_id': 'T1000000',
                'feed_id': 1000
            }
        }
    ] + [
        {
            'identifiers': {
                'business_id': 10802214,
                'offer_id': 'OfferForClassififerMagicIdCheck',
                'extra': {
                    'classifier_magic_id2': '2d15e94d598540df785ab10dfc262d89',
                }
            }
        }
    ] + [
        {
            'identifiers': {
                'business_id': 10802215,
                'offer_id': 'OfferForOrderChecking',
                'feed_id': 1000
            }
        }
    ] + [
        {
            'identifiers': {
                'business_id': 10802216,
                'offer_id': 'OfferWithoutActualPartCpa',
                'feed_id': 1000
            }
        }
    ] + [
        {
            'identifiers': {
                'business_id': 10802217,
                'offer_id': 'OfferWithActualAndOriginalCpa',
                'feed_id': 1000
            }
        }
    ] + [
        # Невидимый оффер не должен попадать в выгрузки
        {
            'identifiers': {
                'business_id': 1090,
                'offer_id': 'InvisibleOffer',
                'feed_id': 347
            },
            'status': {
                'invisible': {
                    'flag': True,
                },
            },
        }
    ]


@pytest.fixture(scope='module')
def service_offers_table_data():
    # оффер с пустыми идентификаторами, не должен ломать reduce
    corrupted_offer_row = offer_to_service_row(DTC.Offer())
    corrupted_offer_row['business_id'] = 1
    corrupted_offer_row['shop_sku'] = 'T600'
    corrupted_offer_row['shop_id'] = 11

    data = SERVICE_OFFERS + [corrupted_offer_row]

    return data


@pytest.fixture(scope='module')
def actual_service_offers_table_data():
    return [
        # с CPA
        {
            'identifiers': {
                'business_id': business_id,
                'offer_id': 'T600',
                'warehouse_id': 0,
                'shop_id': business_id * 10,
                'feed_id': 1000,
            },
            'status': {
                'actual_cpa': {
                    'flag': True,
                }
            }
        } for business_id in range(1, 4)
    ] + [
        # без CPA
        {
            'identifiers': {
                'business_id': business_id,
                'offer_id': 'T600',
                'warehouse_id': 0,
                'shop_id': business_id * 10,
                'feed_id': 1000,
            },
            'status': {
                'actual_cpa': {
                    'flag': False,
                }
            }
        } for business_id in range(4, 5)
    ] + [
        # с CPA, два склада
        {
            'identifiers': {
                'business_id': 6,
                'offer_id': 'T100',
                'warehouse_id': i,
                'shop_id': 60,
                'feed_id': 1000,
            },
            'meta': {
                'rgb': DTC.DIRECT_SEARCH_SNIPPET_GALLERY,
            },
            'status': {
                'actual_cpa': {
                    'flag': True,
                }
            }
        } for i in range(0, 2)
    ] + [
        # с CPA, два склада
        {
            'identifiers': {
                'business_id': 6,
                'offer_id': 'T100',
                'warehouse_id': i,
                'shop_id': 61,
                'feed_id': 1000,
            },
            'meta': {
                'rgb': DTC.DIRECT_SEARCH_SNIPPET_GALLERY,
            },
            'status': {
                'actual_cpa': {
                    'flag': True,
                }
            }
        } for i in range(0, 2)
    ] + [
        {
            'identifiers': {
                'business_id': 10,
                'offer_id': 'T1000000',
                'warehouse_id': 0,
                'shop_id': 63,
                'feed_id': 1000,
            },
            'status': {
                'actual_cpa': {
                    'flag': True,
                }
            },
            'delivery': {
                'partner': {
                    'actual': {
                        'delivery_options': {
                            'options': [{
                                'Cost': 10,
                                'DaysMin': 5,
                                'OrderBeforeHour': 7,
                            }]
                        }
                    }
                }
            }
        }
    ] + [
        {
            'identifiers': {
                'business_id': 10802214,
                'offer_id': 'OfferForClassififerMagicIdCheck',
                'extra': {
                    'classifier_magic_id2': '5ccbc240473659cd1df2d5ed5bc8c1c0',
                },
                'shop_id': 10806906,
                'warehouse_id': 0,
                'feed_id': 1000,
            },
            'status': {
                'actual_cpa': {
                    'flag': True,
                }
            }
        }
    ] + [
        {
            'identifiers': {
                'business_id': 10802215,
                'offer_id': 'OfferForOrderChecking',
                'shop_id': 2,
                'feed_id': 1000,
                'warehouse_id': 100,
            },
            'status': {
                'actual_cpa': {
                    'flag': True,
                }
            }
        }
    ] + [
        {
            'identifiers': {
                'business_id': 10802215,
                'offer_id': 'OfferForOrderChecking',
                'shop_id': 4,
                'feed_id': 1000,
                'warehouse_id': 200,
            },
            'status': {
                'actual_cpa': {
                    'flag': True,
                }
            }
        }
    ] + [
        {
            'identifiers': {
                'business_id': 10802217,
                'offer_id': 'OfferWithActualAndOriginalCpa',
                'shop_id': 10806909,
                'warehouse_id': 1,
                'feed_id': 1000
            },
            'status': {
                'actual_cpa': {
                    'flag': True,
                }
            }
        }
    ] + [
        # Невидимый оффер не должен попадать в выгрузки
        {
            'identifiers': {
                'business_id': 1090,
                'offer_id': 'InvisibleOffer',
                'shop_id': 1091,
                'warehouse_id': 1,
                'feed_id': 347
            },
        }
    ]


@pytest.yield_fixture(scope='module')
def dumper(
        yt_server,
        config,
        basic_offers_table,
        service_offers_table,
        actual_service_offers_table
):
    resources = {
        'basic_offers_table': basic_offers_table,
        'service_offers_table': service_offers_table,
        'actual_service_offers_table': actual_service_offers_table,
        'config': config,
    }
    with MStatDumperEnv(yt_server, **resources) as routines_env:
        yield routines_env


def test_dumper(yt_server, config, dumper, service_offers_table):
    yt_client = yt_server.get_yt_client()
    tables = yt_client.list(yt.ypath_join(config.filtered_out_dir, 'recent'))
    assert_that(len(tables), equal_to(5))

    basic = list(yt_client.read_table(yt.ypath_join(config.filtered_out_dir, 'recent', 'basic')))
    service = list(yt_client.read_table(yt.ypath_join(config.filtered_out_dir, 'recent', 'service')))
    actual = list(yt_client.read_table(yt.ypath_join(config.filtered_out_dir, 'recent', 'actual')))
    contract = list(yt_client.read_table(yt.ypath_join(config.filtered_out_dir, 'recent', 'contract')))

    assert_that(basic, HasDatacampYtUnitedOffersRows(
        [
            {
                "business_id": 1,
                "shop_sku": "T600",
            },
            {
                "business_id": 2,
                "shop_sku": "T600",
            },
            {
                "business_id": 3,
                "shop_sku": "T600",
            },
            {
                "business_id": 6,
                "shop_sku": "T100",
            },
            {
                "business_id": 10,
                "shop_sku": "T1000000",
            },
            {
                "business_id": 10802214,
                "shop_sku": "OfferForClassififerMagicIdCheck",
            },
        ]
    ))
    assert_that(basic, is_not(HasDatacampYtUnitedOffersRows(
        [
            {
                'business_id': 1090,
                'shop_sku': 'InvisibleOffer',
            },
        ]
    )))
    assert_that(service, HasDatacampYtUnitedOffersRows(
        [
            {
                "business_id": 1,
                "shop_sku": "T600",
                "shop_id":  10,
            },
            {
                "business_id": 2,
                "shop_sku": "T600",
                "shop_id":  20,
            },
            {
                "business_id": 3,
                "shop_sku": "T600",
                "shop_id":  30,
            },
            {
                "business_id": 6,
                "shop_sku": "T100",
                "shop_id":  60,
            },
            {
                "business_id": 6,
                "shop_sku": "T100",
                "shop_id":  61,
            },
            {
                "business_id": 10,
                "shop_sku": "T1000000",
                "shop_id":  63,
            },
            {
                "business_id": 10802214,
                "shop_sku": "OfferForClassififerMagicIdCheck",
                "shop_id": 10806906,
            },
        ]
    ))
    assert_that(service, is_not(HasDatacampYtUnitedOffersRows(
        [
            {
                'business_id': 1090,
                'shop_sku': 'InvisibleOffer',
                'shop_id': 1091,
            },
        ]
    )))
    assert_that(actual, HasDatacampYtUnitedOffersRows(
        [
            {
                "business_id": 1,
                "shop_sku": "T600",
                "shop_id":  10,
                "warehouse_id": 0,
            },
            {
                "business_id": 2,
                "shop_sku": "T600",
                "shop_id":  20,
                "warehouse_id": 0,
            },
            {
                "business_id": 3,
                "shop_sku": "T600",
                "shop_id":  30,
                "warehouse_id": 0,
            },
            {
                "business_id": 6,
                "shop_sku": "T100",
                "shop_id":  60,
                "warehouse_id": 0,
            },
            {
                "business_id": 6,
                "shop_sku": "T100",
                "shop_id":  60,
                "warehouse_id": 1,
            },
            {
                "business_id": 6,
                "shop_sku": "T100",
                "shop_id":  61,
                "warehouse_id": 0,
            },
            {
                "business_id": 6,
                "shop_sku": "T100",
                "shop_id":  61,
                "warehouse_id": 1,
            },
            {
                "business_id": 10,
                "shop_sku": "T1000000",
                "shop_id":  63,
                "warehouse_id": 0,
            },
            {
                "business_id": 10802214,
                "shop_sku": "OfferForClassififerMagicIdCheck",
                "shop_id": 10806906,
                "warehouse_id": 0,
            },
        ]
    ))
    assert_that(actual, is_not(HasDatacampYtUnitedOffersRows(
        [
            {
                'business_id': 1090,
                'shop_sku': 'InvisibleOffer',
                'shop_id': 1091,
                'warehouse_id': 1,
            },
        ]
    )))

    assert_that(contract, has_items(
        has_entries({
            "business_id": 1,
            "shop_sku": "T600",
            "shop_id":  10,
            "warehouse_id": 0
        }),
        has_entries({
            "business_id": 2,
            "shop_sku": "T600",
            "shop_id":  20,
            "warehouse_id": 0
        }),
        has_entries({
            "business_id": 3,
            "shop_sku": "T600",
            "shop_id":  30,
            "warehouse_id": 0
        }),
        has_entries({
            "business_id": 6,
            "shop_sku": "T100",
            "shop_id":  60,
            "warehouse_id": 0
        }),
        has_entries({
            "business_id": 6,
            "shop_sku": "T100",
            "shop_id":  60,
            "warehouse_id": 1
        }),
        has_entries({
            "business_id": 6,
            "shop_sku": "T100",
            "shop_id":  61,
            "warehouse_id": 0
        }),
        has_entries({
            "business_id": 6,
            "shop_sku": "T100",
            "shop_id":  61,
            "warehouse_id": 1
        }),
        has_entries({
            "business_id": 10,
            "shop_sku": "T1000000",
            "shop_id":  63,
            "warehouse_id": 0,
            "actual_delivery_options": IsSerializedYson(has_items(has_entries({
                "Cost": 10,
                "DaysMin": 5,
                "OrderBeforeHour": 7
            })))
        }),
        has_entries({
            "business_id": 10802214,
            "shop_sku": "OfferForClassififerMagicIdCheck",
            "shop_id": 10806906,
            "warehouse_id": 0,
            "classifier_magic_id": "2d15e94d598540df785ab10dfc262d89"
        }),
        has_entries({
            "business_id": 10802215,
            "shop_sku": "OfferForOrderChecking",
            "shop_id": 1,
            "warehouse_id": 0,
        }),
        has_entries({
            "business_id": 10802215,
            "shop_sku": "OfferForOrderChecking",
            "shop_id": 2,
            "warehouse_id": 100,
        }),
        has_entries({
            "business_id": 10802215,
            "shop_sku": "OfferForOrderChecking",
            "shop_id": 3,
            "warehouse_id": 0,
        }),
        has_entries({
            "business_id": 10802215,
            "shop_sku": "OfferForOrderChecking",
            "shop_id": 4,
            "warehouse_id": 200,
        }),
        has_entries({
            "business_id": 10802216,
            "shop_sku": "OfferWithoutActualPartCpa",
            "shop_id": 10806908,
            "warehouse_id": 0,
        }),
        has_entries({
            "business_id": 10802217,
            "shop_sku": "OfferWithActualAndOriginalCpa",
            "shop_id": 10806909,
            "warehouse_id": 1
        }),
    ))
    assert_that(contract, is_not(has_items(
        has_entries({
            "business_id": 10802217,
            "shop_sku": "OfferWithActualAndOriginalCpa",
            "shop_id": 10806909,
            "warehouse_id": 0
        }),
        has_entries({
            'business_id': 1090,
            'shop_sku': 'InvisibleOffer',
            'shop_id': 1091,
            'warehouse_id': 1,
        }),
    )))


def test_table_content(dumper, basic_offers_table, service_offers_table, actual_service_offers_table):
    basic_offers_table.load()
    service_offers_table.load()
    actual_service_offers_table.load()
    assert_that(basic_offers_table.data, HasOffers([message_from_data({
        'identifiers': {
            'business_id': 1090,
            'offer_id': 'InvisibleOffer',
        }
    }, DTC.Offer())]))
    assert_that(service_offers_table.data, HasOffers([message_from_data({
        'identifiers': {
            'business_id': 1090,
            'offer_id': 'InvisibleOffer',
            'shop_id': 1091,
        }
    }, DTC.Offer())]))
    assert_that(actual_service_offers_table.data, HasOffers([message_from_data({
        'identifiers': {
            'business_id': 1090,
            'offer_id': 'InvisibleOffer',
            'shop_id': 1091,
            'warehouse_id': 1,
        }
    }, DTC.Offer())]))


def test_service_all(yt_server, config, dumper, service_offers_table):
    yt_client = yt_server.get_yt_client()
    service_all = list(yt_client.read_table(yt.ypath_join(config.filtered_out_dir, 'recent', 'service_all')))
    assert_that(len(service_all), len(service_offers_table.data))
    assert_that(service_all, HasDatacampYtUnitedOffersRows(
        [
            {
                "business_id": business_id,
                "offer_id": offer_id,
                "shop_id":  shop_id,
                "integral_offer_status": not_none(),
            } for business_id, shop_id, offer_id in [
                (1, 10, "T600"),
                (1, 11, "T600"),
                (2, 20, "T600"),
                (3, 30, "T600"),
                (4, 40, "T600"),
                (6, 60, "T100"),
                (6, 61, "T100")
            ]
        ]
    ))


def test_classifier_magic_id_from_correct_offer_part(yt_server, config, dumper):
    yt_client = yt_server.get_yt_client()
    tables = yt_client.list(yt.ypath_join(config.filtered_out_dir, 'recent'))
    assert_that(len(tables), equal_to(5))

    contract = list(yt_client.read_table(yt.ypath_join(config.filtered_out_dir, 'recent', 'contract')))
    """
    Тест проверяет, что в выгрузку берется classifier_magic_id из правильной части оффера в зависимости от настройки
    """
    assert_that(contract, assert_that(contract, has_items(
        has_entries({
            'business_id': 10802214,
            'shop_id': 10806906,
            'warehouse_id': 0,
            'shop_sku': 'OfferForClassififerMagicIdCheck',
            'classifier_magic_id': '2d15e94d598540df785ab10dfc262d89',
        })
    )))
