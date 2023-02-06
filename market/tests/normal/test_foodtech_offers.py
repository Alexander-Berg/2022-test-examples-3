# coding: utf-8

from datetime import datetime
from hamcrest import all_of, assert_that, is_not
import pytest

from market.idx.datacamp.controllers.scanner.yatf.test_env import make_scanner
from market.idx.pylibrary.datacamp.utils import wait_until
from market.idx.yatf.matchers.yt_rows_matchers import HasOffers
from market.idx.yatf.utils.utils import create_pb_timestamp
from market.pylibrary.proto_utils import message_from_data

from market.idx.datacamp.proto.api.ExportMessage_pb2 import ExportMessage
from market.idx.datacamp.proto.common.Types_pb2 import Currency, FormalDimensions
from market.idx.datacamp.proto.external.Offer_pb2 import (
    Offer as ExternalOffer,
    IdentifiedPrice,
    IdentifiedStatus,
    OfferPrice,
    PartnerOfferContent,
)
from market.idx.datacamp.proto.offer.DataCampOffer_pb2 import DataSource, Offer
from market.idx.datacamp.proto.offer.OfferMeta_pb2 import Flag, MarketColor, UpdateMeta
from market.idx.datacamp.proto.external.Offer_pb2 import NavigationNode as ExternalNavigationNode, NavigationPath as ExternalNavigationPath


LAVKA_BUSINESS_ID = 41
EDA_BUSINESS_ID = 42
SHOP1_ID = 1
SHOP2_ID = 2
SHOP3_ID = 3
SHOP4_ID = 4

EXPECTED_UNITED_OFFERS = 3


def make_expected_ts(ts):
    time_pattern = "%Y-%m-%dT%H:%M:%SZ"
    return datetime.utcfromtimestamp(ts).strftime(time_pattern)


def make_identified_price(shop_id, price, ts=0):
    return IdentifiedPrice(
        shop_id=shop_id,
        price=OfferPrice(price=price, currency=Currency.RUR),
        meta=UpdateMeta(timestamp=create_pb_timestamp(ts))
    )


def make_identified_status(shop_id, flag, ts=0):
    return IdentifiedStatus(
        shop_id=shop_id,
        disable_status={
            DataSource.PUSH_PARTNER_API: Flag(
                flag=flag,
                meta=UpdateMeta(timestamp=create_pb_timestamp(ts))
            )
        },
    )


def make_navigation_path(path):
    return ExternalNavigationPath(nodes=[ExternalNavigationNode(id=node_id, name=node_name) for node_id, node_name in path])


@pytest.fixture(scope='module')
def lavka_basic_offers_table_data():
    return [
        {
            'business_id': LAVKA_BUSINESS_ID,
            'offer_id': 'lavka_multiprice_1',
            'data': ExportMessage(
                offer=ExternalOffer(
                    business_id=LAVKA_BUSINESS_ID,
                    offer_id='lavka_multiprice_1',
                    original_content=dict(
                        name='lavka offer 2',
                        description='lavka description 2',
                    ),
                    shop_prices=[
                        make_identified_price(SHOP3_ID, 456*10**7, ts=100500),
                        make_identified_price(SHOP4_ID, 678*10**7, ts=100501),
                    ],
                    shop_statuses=[
                        make_identified_status(SHOP4_ID, True, ts=100700)
                    ]
                )
            ).SerializeToString()
        }
    ]


@pytest.fixture(scope='module')
def eda_basic_offers_table_data():
    return [
        {
            'business_id': EDA_BUSINESS_ID,
            'offer_id': 'eda_normalized_1',
            'data': ExportMessage(
                offer=ExternalOffer(
                    timestamp=create_pb_timestamp(100600),
                    business_id=EDA_BUSINESS_ID,
                    offer_id='eda_normalized_1',
                    original_content=PartnerOfferContent(
                        name='eda offer 1',
                        description='eda description 1',
                        brutto_weight_in_grams=1500,
                        brutto_dimensions=FormalDimensions(length_mkm=1230000, width_mkm=3210000, height_mkm=1320000)
                    )
                )
            ).SerializeToString()
        }
    ]


@pytest.fixture(scope='module')
def eda_service_offers_table_data():
    return [
        {
            'business_id': EDA_BUSINESS_ID,
            'offer_id': 'eda_normalized_1',
            'shop_id': SHOP1_ID,
            'data': ExportMessage(
                offer=ExternalOffer(
                    timestamp=create_pb_timestamp(100601),
                    business_id=EDA_BUSINESS_ID,
                    offer_id='eda_normalized_1',
                    shop_id=SHOP1_ID,
                    price=OfferPrice(price=123*10**7, currency=Currency.USD),
                    navigation_paths=[
                        make_navigation_path([(1, 'Super cat'), (2, 'Cat')]),
                    ],
                )
            ).SerializeToString()
        },
        {
            'business_id': EDA_BUSINESS_ID,
            'offer_id': 'eda_normalized_1',
            'shop_id': SHOP2_ID,
            'data': ExportMessage(
                offer=ExternalOffer(
                    timestamp=create_pb_timestamp(100602),
                    business_id=EDA_BUSINESS_ID,
                    offer_id='eda_normalized_1',
                    shop_id=SHOP2_ID,
                    price=OfferPrice(price=987*10**7, currency=Currency.USD),
                    disable_status={DataSource.PUSH_PARTNER_API: Flag(flag=True)},
                    navigation_paths=[
                        make_navigation_path([(3, 'Uber cat')]),
                    ],
                )
            ).SerializeToString()
        },
        {
            'business_id': EDA_BUSINESS_ID,
            'offer_id': 'eda_normalized_1',
            'shop_id': SHOP3_ID,
            'data': ExportMessage(
                offer=ExternalOffer(
                    business_id=EDA_BUSINESS_ID,
                    offer_id='eda_normalized_1',
                    shop_id=SHOP3_ID,
                    shop_prices=[
                        make_identified_price(SHOP3_ID, 555*10**7, ts=100700),
                    ],
                    shop_statuses=[
                        make_identified_status(SHOP3_ID, True, ts=100700)
                    ]
                )
            ).SerializeToString()
        },
    ]


@pytest.fixture(scope='module')
def scanner(
    log_broker_stuff,
    yt_server,
    scanner_resources,
):
    with make_scanner(yt_server, log_broker_stuff, 'white', shopsdat_cacher=True, **scanner_resources) as scanner_env:
        wait_until(lambda: scanner_env.united_offers_processed == EXPECTED_UNITED_OFFERS, timeout=10)
        yield scanner_env


def test_lavka_offers(scanner):
    assert_that(
        scanner.basic_offers_table.data,
        all_of(
            HasOffers([
                message_from_data({
                    'identifiers': {
                        'business_id': LAVKA_BUSINESS_ID,
                        'offer_id': 'lavka_multiprice_1',
                    },
                    'content': {
                        'partner': {
                            'original': {
                                'name': {
                                    'value': 'lavka offer 2',
                                },
                                'description': {
                                    'value': 'lavka description 2'
                                }
                            }
                        }
                    }
                }, Offer()),
            ]),
            is_not(HasOffers([
                message_from_data({'price': {}}, Offer())
            ])),
            is_not(HasOffers([
                message_from_data({
                    'status': {
                        'disabled': [{'flag': True}]
                    }
                }, Offer())
            ])),
        )
    )

    assert_that(
        scanner.service_offers_table.data,
        HasOffers([
            message_from_data({
                'identifiers': {
                    'business_id': LAVKA_BUSINESS_ID,
                    'offer_id': 'lavka_multiprice_1',
                    'shop_id': SHOP3_ID
                },
                'meta': {
                    'rgb': MarketColor.LAVKA,
                },
                'price': {
                    'basic': {
                        'meta': {
                            'timestamp': make_expected_ts(100500)
                        },
                        'binary_price': {
                            'price': 456 * 10**7
                        }
                    }
                }
            }, Offer()),
            message_from_data({
                'identifiers': {
                    'business_id': LAVKA_BUSINESS_ID,
                    'offer_id': 'lavka_multiprice_1',
                    'shop_id': SHOP4_ID
                },
                'meta': {
                    'rgb': MarketColor.LAVKA,
                },
                'price': {
                    'basic': {
                        'meta': {
                            'timestamp': make_expected_ts(100501)
                        },
                        'binary_price': {
                            'price': 678 * 10**7
                        }
                    }
                },
                'status': {
                    'disabled': [{
                        'meta': {
                            'source': DataSource.PUSH_PARTNER_API,
                            'timestamp': make_expected_ts(100700)
                        },
                        'flag': True
                    }]
                }
            }, Offer()),
        ])
    )


def test_eda_offers(scanner):
    assert_that(
        scanner.basic_offers_table.data,
        all_of(
            HasOffers([
                message_from_data({
                    'identifiers': {
                        'business_id': EDA_BUSINESS_ID,
                        'offer_id': 'eda_normalized_1',
                    },
                    'content': {
                        'partner': {
                            'original': {
                                'name': {
                                    'value': 'eda offer 1',
                                },
                                'description': {
                                    'value': 'eda description 1'
                                },
                                'weight': {
                                    'value_mg': 1500000,
                                },
                                'dimensions': {
                                    'length_mkm': 1230000,
                                    'width_mkm': 3210000,
                                    'height_mkm': 1320000
                                },
                            }
                        }
                    }
                }, Offer())
            ]),
            is_not(HasOffers([
                message_from_data({'price': {}}, Offer())
            ])),
            is_not(HasOffers([
                message_from_data({
                    'status': {
                        'disabled': [{'flag': True}]
                    }
                }, Offer())
            ])),
        )
    )

    assert_that(
        scanner.service_offers_table.data,
        HasOffers([
            message_from_data({
                'identifiers': {
                    'business_id': EDA_BUSINESS_ID,
                    'offer_id': 'eda_normalized_1',
                    'shop_id': SHOP1_ID
                },
                'meta': {
                    'rgb': MarketColor.EDA,
                },
                'price': {
                    'basic': {
                        'meta': {
                            'timestamp': make_expected_ts(100601)
                        },
                        'binary_price': {
                            'price': 123 * 10**7
                        }
                    }
                },
                'content': {
                    'binding': {
                        'navigation': {
                            "paths": [{
                                "nodes": [{
                                    "id": 1,
                                    "name": "Super cat"
                                }, {
                                    "id": 2,
                                    "name": "Cat"
                                }]
                            }],
                        }
                    }
                }
            }, Offer()),
            message_from_data({
                'identifiers': {
                    'business_id': EDA_BUSINESS_ID,
                    'offer_id': 'eda_normalized_1',
                    'shop_id': SHOP2_ID
                },
                'meta': {
                    'rgb': MarketColor.EDA,
                },
                'price': {
                    'basic': {
                        'meta': {
                            'timestamp': make_expected_ts(100602)
                        },
                        'binary_price': {
                            'price': 987 * 10**7
                        }
                    }
                },
                'status': {
                    'disabled': [{
                        'meta': {
                            'source': DataSource.PUSH_PARTNER_API,
                            'timestamp': make_expected_ts(100602)
                        },
                        'flag': True
                    }]
                },
                'content': {
                    'binding': {
                        'navigation': {
                            "paths": [{
                                "nodes": [{
                                    "id": 3,
                                    "name": "Uber cat"
                                }]
                            }]
                        }
                    }
                }
            }, Offer()),
            message_from_data({
                'identifiers': {
                    'business_id': EDA_BUSINESS_ID,
                    'offer_id': 'eda_normalized_1',
                    'shop_id': SHOP3_ID
                },
                'meta': {
                    'rgb': MarketColor.EDA,
                },
                'price': {
                    'basic': {
                        'meta': {
                            'timestamp': make_expected_ts(100700)
                        },
                        'binary_price': {
                            'price': 555 * 10**7
                        }
                    }
                },
                'status': {
                    'disabled': [{
                        'meta': {
                            'source': DataSource.PUSH_PARTNER_API,
                            'timestamp': make_expected_ts(100700)
                        },
                        'flag': True
                    }]
                }
            }, Offer()),
        ])
    )


def test_enrich_fields(scanner):
    assert_that(
        scanner.service_offers_table.data,
        HasOffers([
            message_from_data({
                'status': {
                    'original_cpa': {
                        'flag': True
                    }
                },
                'delivery': {
                    'partner': {
                        'original': {
                            'delivery_options': {
                                'options': [{
                                    'DaysMin': 2,
                                    'DaysMax': 3,
                                    'Cost': 100
                                }]
                            }
                        }
                    }
                },
                'price': {
                    'basic': {
                        'vat': None  # no default VAT!
                    }
                }
            }, Offer()),
        ])
    )
