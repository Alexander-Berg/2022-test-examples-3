# coding: utf-8

from hamcrest import assert_that, equal_to, not_
import pytest
from datetime import datetime

from market.idx.datacamp.proto.offer.DataCampOffer_pb2 import (
    Offer as DatacampOffer,
    PUSH_PARTNER_API,
    DIRECT_LINK,
    BASIC, SERVICE, BLUE
)
from market.idx.datacamp.proto.api.DatacampMessage_pb2 import DatacampMessage
from market.idx.datacamp.controllers.piper.yatf.test_env_old import PiperTestEnv
from market.idx.datacamp.controllers.piper.yatf.resources.config_mock import PiperConfigMock

from market.idx.yatf.resources.lbk_topic import LbkTopic
from market.idx.yatf.resources.datacamp.datacamp_tables import (
    DataCampBasicOffersTable,
    DataCampServiceOffersTable
)

from market.idx.yatf.matchers.yt_rows_matchers import HasOffers

from market.idx.pylibrary.datacamp.utils import wait_until
from market.pylibrary.proto_utils import message_from_data

time_pattern = '%Y-%m-%dT%H:%M:%SZ'
time_stamp = 1638169232

ACTUAL_SOURCES_FOR_BASIC_PART = [
    'title',
    'title_no_vendor',
    'description',
    'country_of_origin_id',
    'offer_params',
    'price_from',
    'adult',
    'age',
    'barcode',
    'expiry',
    'manufacturer_warranty',
    'weight',
    'dimensions',
    'downloadable',
    'type_prefix',
    'type',
    'seller_warranty',
    'isbn',
    'vendor',
    'category',
    'vendor_code',
    'name',
    'model',
    'condition',
    'ingredients',
    'language_tag',
    'cargo_types',
]

ORIGINAL_TERMS_SOURCES_FOR_BASIC_PART = [
    'seller_warranty',
    'quantity_in_pack',
    'box_count',
]

ORIGINAL_SOURCES_FOR_BASIC_PART = [
    'updates_from_feed_by_url',
    'name',
    'description',
    'type_prefix',
    'vendor',
    'model',
    'vendor_code',
    'barcode',
    'offer_params',
    'text_params',
    'group_id',
    'type',
    'downloadable',
    'adult',
    'age',
    'condition',
    'manufacturer_warranty',
    'expiry',
    'country_of_origin',
    'weight',
    'dimensions',
    'weight_net',
    'price_from',
    'isbn',
    'category',
    'group_name',
    'lifespan',
    'certificates',
    'tn_ved_code',
    'animal_products',
    'mercury_guid',
    'manufacturer',
    'name_no_vendor',
    'ingredients',
    'language_tag',
    'cargo_types',
    'original_name',
]

ACTUAL_SOURCES_FOR_SERVICE_PART = [
    'sales_notes',
    'quantity',
    'installment_options',
]

ORIGINAL_SOURCES_FOR_SERVICE_PART = [
    'supplier_info',
    'pricelabs_params',
]

ORIGINAL_TERMS_SOURCES_FOR_SERVICE_PART = [
    'sales_notes',
    'quantity',
    'supply_quantity',
    'supply_plan',
    'transport_unit_size',
    'supply_weekdays',
    'partner_delivery_time',
]

META = {
    'source': PUSH_PARTNER_API,
    'timestamp': datetime.utcfromtimestamp(time_stamp).strftime(time_pattern),
}

DATACAMP_MESSAGES = [{
    'united_offers': [{
        'offer': [{
            'basic': {
                'content': {
                    'partner': {
                        'actual': {
                            key: {
                                'meta': META,
                            } for key in ACTUAL_SOURCES_FOR_BASIC_PART + ACTUAL_SOURCES_FOR_SERVICE_PART
                        },
                        'original': {
                            key: {
                                'meta': META,
                            } for key in ORIGINAL_SOURCES_FOR_BASIC_PART + ORIGINAL_SOURCES_FOR_SERVICE_PART
                        },
                        'original_terms': {
                            key: {
                                'meta': META,
                            } for key in ORIGINAL_TERMS_SOURCES_FOR_BASIC_PART + ORIGINAL_TERMS_SOURCES_FOR_SERVICE_PART
                        },
                    },
                },
                'meta': {
                    'rgb': BLUE
                },
                'identifiers': {
                    'business_id': 920788,
                    'offer_id': '60242',
                    'shop_id': 1983891,
                    'extra': {
                        'shop_sku': '60242'
                    },
                    'feed_id': 1948684,
                    'warehouse_id': 140516
                },
                'pictures': {
                    'partner': {
                        'original': {
                            'meta': META,
                            'source': [
                                {
                                    'source': DIRECT_LINK,
                                    'url': 'https://rc-today.ru/UserFiles/Image/Big/img60242_80841_big.jpg'
                                },
                            ]
                        }
                    }
                },
            },
            'service': {
                1: {
                    'content': {
                        'partner': {
                            'actual': {
                                key: {
                                    'meta': META,
                                } for key in ACTUAL_SOURCES_FOR_BASIC_PART + ACTUAL_SOURCES_FOR_SERVICE_PART
                            },
                            'original': {
                                key: {
                                    'meta': META,
                                } for key in ORIGINAL_SOURCES_FOR_BASIC_PART + ORIGINAL_SOURCES_FOR_SERVICE_PART
                            },
                            'original_terms': {
                                key: {
                                    'meta': META,
                                } for key in ORIGINAL_TERMS_SOURCES_FOR_BASIC_PART + ORIGINAL_TERMS_SOURCES_FOR_SERVICE_PART
                            },
                        }
                    },
                    'identifiers': {
                        'business_id': 920788,
                        'offer_id': '60242',
                        'shop_id': 1983891,
                        'extra': {
                            'shop_sku': '60242'
                        },
                        'feed_id': 1948684,
                        'warehouse_id': 140516
                    },
                    'pictures': {
                        'partner': {
                            'original': {
                                'meta': META,
                                'source': [
                                    {
                                        'source': DIRECT_LINK,
                                        'url': 'https://rc-today.ru/UserFiles/Image/Big/img60242_80841_big.jpg'
                                    },
                                ]
                            }
                        }
                    },
                    'status': {
                        'united_catalog': {
                            'flag': True,
                            'meta': META,
                        }
                    },
                },
            }
        }]
    }]
}]

EXPECTED_SERVICE_OFFERS = [
    message_from_data({
        'content': None,
        'identifiers': {
            'business_id': 920788,
            'offer_id': '60242',
            'shop_id': 1983891,
        },
        'meta': {
            'scope': SERVICE,
        },
        'status': {
            'united_catalog': {
                'flag': True,
                'meta': META,
            }
        },
    }, DatacampOffer()),
]

ACTUAL_UNEXPECTED_SERVICE_OFFERS = [
    message_from_data({
        'content': {
            'partner': {
                'actual': {
                    key: {
                        'meta': META,
                    } for key in ACTUAL_SOURCES_FOR_BASIC_PART
                },
            }
        },
        'identifiers': {
            'business_id': 920788,
            'offer_id': '60242',
            'shop_id': 1983891,
        },
    }, DatacampOffer()),
]

ORIGINAL_UNEXPECTED_SERVICE_OFFERS = [
    message_from_data({
        'content': {
            'partner': {
                'original': {
                    key: {
                        'meta': META,
                    } for key in ORIGINAL_SOURCES_FOR_BASIC_PART
                },
            }
        },
        'identifiers': {
            'business_id': 920788,
            'offer_id': '60242',
            'shop_id': 1983891,
        },
    }, DatacampOffer()),
]

ORIGINAL_TERMS_UNEXPECTED_SERVICE_OFFERS = [
    message_from_data({
        'content': {
            'partner': {
                'original_terms': {
                    key: {
                        'meta': META,
                    } for key in ORIGINAL_TERMS_SOURCES_FOR_BASIC_PART
                },
            }
        },
        'identifiers': {
            'business_id': 920788,
            'offer_id': '60242',
            'shop_id': 1983891,
        },
    }, DatacampOffer()),
]

EXPECTED_BASIC_OFFERS = [
    message_from_data({
        'content': {
            'partner': {
                'actual': {
                    key: {
                        'meta': META,
                    } for key in ACTUAL_SOURCES_FOR_BASIC_PART
                },
                'original': {
                    key: {
                        'meta': META,
                    } for key in ORIGINAL_SOURCES_FOR_BASIC_PART
                },
                'original_terms': {
                    key: {
                        'meta': META,
                    } for key in ORIGINAL_TERMS_SOURCES_FOR_BASIC_PART
                },
            }
        },
        'pictures': {
            'partner': {
                'original': {
                    'source': [
                        {
                            'source': DIRECT_LINK,
                            'url': 'https://rc-today.ru/UserFiles/Image/Big/img60242_80841_big.jpg'
                        },
                    ]
                },
            }
        },
        'identifiers': {
            'business_id': 920788,
            'offer_id': '60242',
        },
        'meta': {
            'scope': BASIC,
        },
    }, DatacampOffer())
]

ACTUAL_UNEXPECTED_BASIC_OFFERS = [
    message_from_data({
        'content': {
            'partner': {
                'actual': {
                    key: {
                        'meta': META,
                    } for key in ACTUAL_SOURCES_FOR_SERVICE_PART
                },
            }
        },
        'identifiers': {
            'business_id': 920788,
            'offer_id': '60242',
        },
    }, DatacampOffer())
]

ORIGINAL_UNEXPECTED_BASIC_OFFERS = [
    message_from_data({
        'content': {
            'partner': {
                'original': {
                    key: {
                        'meta': META,
                    } for key in ORIGINAL_SOURCES_FOR_SERVICE_PART
                },
            }
        },
        'identifiers': {
            'business_id': 920788,
            'offer_id': '60242',
        },
    }, DatacampOffer())
]

ORIGINAL_TERMS_UNEXPECTED_BASIC_OFFERS = [
    message_from_data({
        'content': {
            'partner': {
                'original_terms': {
                    key: {
                        'meta': META,
                    } for key in ORIGINAL_TERMS_SOURCES_FOR_SERVICE_PART
                },
            }
        },
        'identifiers': {
            'business_id': 920788,
            'offer_id': '60242',
        },
    }, DatacampOffer())
]


@pytest.fixture(scope='module')
def basic_offers_table(yt_server, config):
    return DataCampBasicOffersTable(yt_server, config.yt_basic_offers_tablepath)


@pytest.fixture(scope='module')
def service_offers_table(yt_server, config):
    return DataCampServiceOffersTable(yt_server, config.yt_service_offers_tablepath)


@pytest.fixture(scope='session')
def api_assortment_input_topic(log_broker_stuff):
    topic = LbkTopic(log_broker_stuff)
    return topic


@pytest.fixture(scope='session')
def config(yt_server, log_broker_stuff, api_assortment_input_topic):
    cfg = {
        'general': {
            'color': 'blue',
        },
        'logbroker': {
            'api_assortment_input_topic': api_assortment_input_topic.topic,
        },
    }
    return PiperConfigMock(yt_server=yt_server,
                           log_broker_stuff=log_broker_stuff,
                           config=cfg)


@pytest.yield_fixture(scope='module')
def piper(
    yt_server,
    log_broker_stuff,
    config,
    api_assortment_input_topic,
    basic_offers_table,
    service_offers_table,
):
    resources = {
        'config': config,
        'basic_offers_table': basic_offers_table,
        'service_offers_table': service_offers_table,
        'api_assortment_input_topic': api_assortment_input_topic,
    }
    with PiperTestEnv(yt_server, log_broker_stuff, **resources) as piper_env:
        piper_env.verify()
        yield piper_env


def test_api_data_content_deduplication_from_service_offer(
    piper,
    api_assortment_input_topic,
    basic_offers_table,
    service_offers_table,
):
    for message in DATACAMP_MESSAGES:
        api_assortment_input_topic.write(message_from_data(message, DatacampMessage()).SerializeToString())

    wait_until(lambda: piper.united_offers_processed >= len(DATACAMP_MESSAGES))

    service_offers_table.load()
    assert_that(len(service_offers_table.data), equal_to(len(EXPECTED_SERVICE_OFFERS)))
    assert_that(service_offers_table.data, HasOffers(EXPECTED_SERVICE_OFFERS))
    assert_that(service_offers_table.data, not_(HasOffers(ACTUAL_UNEXPECTED_SERVICE_OFFERS)))
    assert_that(service_offers_table.data, not_(HasOffers(ORIGINAL_UNEXPECTED_SERVICE_OFFERS)))
    assert_that(service_offers_table.data, not_(HasOffers(ORIGINAL_TERMS_UNEXPECTED_SERVICE_OFFERS)))

    basic_offers_table.load()
    assert_that(len(basic_offers_table.data), equal_to(len(EXPECTED_BASIC_OFFERS)))
    assert_that(basic_offers_table.data, HasOffers(EXPECTED_BASIC_OFFERS))
    assert_that(basic_offers_table.data, not_(HasOffers(ACTUAL_UNEXPECTED_BASIC_OFFERS)))
    assert_that(basic_offers_table.data, not_(HasOffers(ORIGINAL_UNEXPECTED_BASIC_OFFERS)))
    assert_that(basic_offers_table.data, not_(HasOffers(ORIGINAL_TERMS_UNEXPECTED_BASIC_OFFERS)))
