# coding: utf-8

from hamcrest import assert_that, equal_to, not_none, has_items, has_entry
import pytest

import yt.wrapper as yt

from market.idx.datacamp.routines.yatf.resources.config_mock import RoutinesConfigMock
from market.idx.yatf.matchers.protobuf_matchers import IsSerializedProtobuf
from market.idx.yatf.resources.yt_tables.saas_table import SaasTable

from market.idx.datacamp.routines.yatf.test_env import PromoSaasDiffBuilderEnv

import market.idx.datacamp.proto.offer.DataCampOffer_pb2 as DTC
from kernel.search_zone.protos.searchzone_pb2 import TZone, TAttribute
from saas.protos.rtyserver_pb2 import TMessage


IN_SAAS_NOT_IN_DATACAMP_BUSINESS_ID = 1100
IN_SAAS_NOT_IN_DATACAMP_OFFER_ID = "IN_SAAS_NOT_IN_DATACAMP_OFFER_ID"
IN_SAAS_NOT_IN_DATACAMP_SHOP_ID = 1101
IN_SAAS_NOT_IN_DATACAMP_MARKET_CATEGORY_ID = '1110'
IN_SAAS_NOT_IN_DATACAMP_MARKET_VENDOR_ID = '1111'

IN_SAAS_IN_DATACAMP_NOT_VALID_BUSINESS_ID = 2200
IN_SAAS_IN_DATACAMP_NOT_VALID_OFFER_ID = "IN_SAAS_IN_DATACAMP_NOT_VALID_OFFER_ID"
IN_SAAS_IN_DATACAMP_NOT_VALID_SHOP_ID = 2202
IN_SAAS_IN_DATACAMP_NOT_VALID_MARKET_CATEGORY_ID = '2220'
IN_SAAS_IN_DATACAMP_NOT_VALID_MARKET_VENDOR_ID = '2222'

SAAS_DOCS = [
    TMessage.TDocument(
        DocumentProperties=(
            [
                {'Name': 'offer_id', 'Value': 'OFFER_IN_SAAS_SHOULD_SEND'},
                {'Name': 'doc_type', 'Value': 'offer'},
                {'Name': 'market_category_id', 'Value': '10'},
                {'Name': 'market_vendor_id', 'Value': '5'},
            ]
        ),
        KeyPrefix=1,
        Url='s/' + str(1) + '/' + 'OFFER_IN_SAAS_SHOULD_SEND',
        SearchAttributes=(
            [
                {
                    'Name': 's_offer_id',
                    'Value': 'OFFER_IN_SAAS_SHOULD_SEND',
                    'Type': TAttribute.TAttributeType.LITERAL_ATTRIBUTE,
                },
                {'Name': 's_doc_type', 'Value': 'offer', 'Type': TAttribute.TAttributeType.LITERAL_ATTRIBUTE},
                {'Name': 's_market_category_id', 'Value': '10', 'Type': TAttribute.TAttributeType.LITERAL_ATTRIBUTE},
                {'Name': 's_market_vendor_id', 'Value': '5', 'Type': TAttribute.TAttributeType.LITERAL_ATTRIBUTE},
            ]
        ),
        GroupAttributes=(
            [
                {'Name': 'market_category_id', 'Value': '10', 'Type': TAttribute.TAttributeType.INTEGER_ATTRIBUTE},
                {'Name': 'market_vendor_id', 'Value': '5', 'Type': TAttribute.TAttributeType.INTEGER_ATTRIBUTE},
                {'Name': 'doc_type', 'Value': 'offer', 'Type': TAttribute.TAttributeType.LITERAL_ATTRIBUTE},
            ]
        ),
        Realtime=True,
        RootZone=TZone(
            Children=[
                TZone(Name='z_offer_id', Text='OFFER_IN_SAAS_SHOULD_SEND'),
                TZone(Name='z_title', Text=''),
            ]
        ),
    ),
    TMessage.TDocument(
        DocumentProperties=(
            [
                {'Name': 'offer_id', 'Value': 'OFFER_IN_SAAS_SHOULD_NOT_SEND'},
                {'Name': 'doc_type', 'Value': 'offer'},
                {'Name': 'market_category_id', 'Value': '10'},
                {'Name': 'market_vendor_id', 'Value': '5'},
                {'Name': 'promo_id', 'Value': 'promo1'},
            ]
        ),
        KeyPrefix=15,
        Url='s/' + str(15) + '/' + 'OFFER_IN_SAAS_SHOULD_NOT_SEND',
        SearchAttributes=(
            [
                {'Name': 's_promo_id', 'Value': 'promo1', 'Type': TAttribute.TAttributeType.LITERAL_ATTRIBUTE},
                {
                    'Name': 's_offer_id',
                    'Value': 'OFFER_IN_SAAS_SHOULD_NOT_SEND',
                    'Type': TAttribute.TAttributeType.LITERAL_ATTRIBUTE,
                },
                {'Name': 's_doc_type', 'Value': 'offer', 'Type': TAttribute.TAttributeType.LITERAL_ATTRIBUTE},
                {'Name': 's_market_category_id', 'Value': '10', 'Type': TAttribute.TAttributeType.LITERAL_ATTRIBUTE},
                {'Name': 's_market_vendor_id', 'Value': '5', 'Type': TAttribute.TAttributeType.LITERAL_ATTRIBUTE},
            ]
        ),
        GroupAttributes=(
            [
                {'Name': 'market_category_id', 'Value': '10', 'Type': TAttribute.TAttributeType.INTEGER_ATTRIBUTE},
                {'Name': 'market_vendor_id', 'Value': '5', 'Type': TAttribute.TAttributeType.INTEGER_ATTRIBUTE},
                {'Name': 'promo_id', 'Value': 'promo1', 'Type': TAttribute.TAttributeType.LITERAL_ATTRIBUTE},
                {'Name': 'doc_type', 'Value': 'offer', 'Type': TAttribute.TAttributeType.LITERAL_ATTRIBUTE},
            ]
        ),
        Realtime=True,
        RootZone=TZone(
            Children=[
                TZone(Name='z_offer_id', Text='OFFER_IN_SAAS_SHOULD_NOT_SEND'),
                TZone(Name='z_title', Text=''),
            ]
        ),
    ),
    TMessage.TDocument(
        DocumentProperties=(
            [
                {'Name': 'offer_id', 'Value': IN_SAAS_IN_DATACAMP_NOT_VALID_OFFER_ID},
                {'Name': 'doc_type', 'Value': 'offer'},
                {'Name': 'market_category_id', 'Value': IN_SAAS_IN_DATACAMP_NOT_VALID_MARKET_CATEGORY_ID},
                {'Name': 'market_vendor_id', 'Value': IN_SAAS_IN_DATACAMP_NOT_VALID_MARKET_VENDOR_ID},
            ]
        ),
        KeyPrefix=IN_SAAS_IN_DATACAMP_NOT_VALID_SHOP_ID ,
        Url='s/' + str(IN_SAAS_IN_DATACAMP_NOT_VALID_SHOP_ID) + '/' + IN_SAAS_IN_DATACAMP_NOT_VALID_OFFER_ID,
        SearchAttributes=(
            [
                {
                    'Name': 's_offer_id',
                    'Value': IN_SAAS_IN_DATACAMP_NOT_VALID_OFFER_ID,
                    'Type': TAttribute.TAttributeType.LITERAL_ATTRIBUTE,
                },
                {'Name': 's_doc_type', 'Value': 'offer', 'Type': TAttribute.TAttributeType.LITERAL_ATTRIBUTE},
                {'Name': 's_market_category_id', 'Value': IN_SAAS_IN_DATACAMP_NOT_VALID_MARKET_CATEGORY_ID , 'Type': TAttribute.TAttributeType.LITERAL_ATTRIBUTE},
                {'Name': 's_market_vendor_id', 'Value': IN_SAAS_IN_DATACAMP_NOT_VALID_MARKET_VENDOR_ID , 'Type': TAttribute.TAttributeType.LITERAL_ATTRIBUTE},
            ]
        ),
        GroupAttributes=(
            [
                {'Name': 'market_category_id', 'Value': IN_SAAS_IN_DATACAMP_NOT_VALID_MARKET_CATEGORY_ID , 'Type': TAttribute.TAttributeType.INTEGER_ATTRIBUTE},
                {'Name': 'market_vendor_id', 'Value': IN_SAAS_IN_DATACAMP_NOT_VALID_MARKET_VENDOR_ID , 'Type': TAttribute.TAttributeType.INTEGER_ATTRIBUTE},
                {'Name': 'doc_type', 'Value': 'offer', 'Type': TAttribute.TAttributeType.LITERAL_ATTRIBUTE},
            ]
        ),
        Realtime=True,
        RootZone=TZone(
            Children=[
                TZone(Name='z_offer_id', Text=IN_SAAS_IN_DATACAMP_NOT_VALID_OFFER_ID),
                TZone(Name='z_title', Text=''),
            ]
        ),
    ),
    TMessage.TDocument(
        DocumentProperties=(
            [
                {'Name': 'offer_id', 'Value': IN_SAAS_NOT_IN_DATACAMP_OFFER_ID},
                {'Name': 'doc_type', 'Value': 'offer'},
                {'Name': 'market_category_id', 'Value': IN_SAAS_NOT_IN_DATACAMP_MARKET_CATEGORY_ID},
                {'Name': 'market_vendor_id', 'Value': IN_SAAS_NOT_IN_DATACAMP_MARKET_VENDOR_ID},
            ]
        ),
        KeyPrefix=IN_SAAS_NOT_IN_DATACAMP_SHOP_ID ,
        Url='s/' + str(IN_SAAS_NOT_IN_DATACAMP_SHOP_ID) + '/' + IN_SAAS_NOT_IN_DATACAMP_OFFER_ID,
        SearchAttributes=(
            [
                {
                    'Name': 's_offer_id',
                    'Value': IN_SAAS_NOT_IN_DATACAMP_OFFER_ID,
                    'Type': TAttribute.TAttributeType.LITERAL_ATTRIBUTE,
                },
                {'Name': 's_doc_type', 'Value': 'offer', 'Type': TAttribute.TAttributeType.LITERAL_ATTRIBUTE},
                {'Name': 's_market_category_id', 'Value': IN_SAAS_NOT_IN_DATACAMP_MARKET_CATEGORY_ID , 'Type': TAttribute.TAttributeType.LITERAL_ATTRIBUTE},
                {'Name': 's_market_vendor_id', 'Value': IN_SAAS_NOT_IN_DATACAMP_MARKET_VENDOR_ID , 'Type': TAttribute.TAttributeType.LITERAL_ATTRIBUTE},
            ]
        ),
        GroupAttributes=(
            [
                {'Name': 'market_category_id', 'Value': IN_SAAS_NOT_IN_DATACAMP_MARKET_CATEGORY_ID , 'Type': TAttribute.TAttributeType.INTEGER_ATTRIBUTE},
                {'Name': 'market_vendor_id', 'Value': IN_SAAS_NOT_IN_DATACAMP_MARKET_VENDOR_ID , 'Type': TAttribute.TAttributeType.INTEGER_ATTRIBUTE},
                {'Name': 'doc_type', 'Value': 'offer', 'Type': TAttribute.TAttributeType.LITERAL_ATTRIBUTE},
            ]
        ),
        Realtime=True,
        RootZone=TZone(
            Children=[
                TZone(Name='z_offer_id', Text=IN_SAAS_NOT_IN_DATACAMP_OFFER_ID),
                TZone(Name='z_title', Text=''),
            ]
        ),
    ),
]


def document_to_ssas_row(doc):
    row = {
        'KeyPrefix': doc.KeyPrefix,
        'Url': doc.Url,
        'ProtoMessage': TMessage(Document=doc, MessageType=TMessage.TMessageType.ADD_DOCUMENT).SerializeToString(),
    }
    return row


@pytest.fixture(scope='module')
def config(yt_server):
    config = RoutinesConfigMock(
        yt_server,
        config={
            'general': {
                'yt_home': '//home/datacamp/united',
            },
            'promo_saas_diff_builder': {
                'enable': True,
                'output_dir': 'promo_saas_diff',
                'removed_deadline': 24 * 60 * 60,
                'yt_proxies': [yt_server.get_yt_client().config["proxy"]["url"]],
            },
        },
    )
    return config


@pytest.fixture(scope='module')
def basic_offers_table_data():
    return [
        {
            'identifiers': {
                'business_id': 1,
                'offer_id': 'OFFER_IN_SAAS_SHOULD_SEND',
                'feed_id': 1000,
            },
            'meta': {
                'rgb': DTC.WHITE,
            },
        },
        {
            'identifiers': {
                'business_id': 11,
                'offer_id': 'OFFER_NOT_IN_SAAS',
                'feed_id': 1001,
            },
            'meta': {
                'rgb': DTC.WHITE,
            }
        },
        {
            'identifiers': {
                'business_id': 111,
                'offer_id': 'OFFER_IN_SAAS_SHOULD_NOT_SEND',
                'feed_id': 1002,
            },
            'meta': {
                'rgb': DTC.WHITE,
            }
        },
    ]


@pytest.fixture(scope='module')
def service_offers_table_data():
    return [
        # valid saas offer, has data missing in saas, should be present in output table
        {
            'identifiers': {
                'business_id': 1,
                'offer_id': 'OFFER_IN_SAAS_SHOULD_SEND',
                'warehouse_id': 0,
                'shop_id': 1,
                'feed_id': 1000,
            },
            'price': {
                'basic': {
                    'binary_price': {
                        'price': 20 * 10 ** 7,
                        'id': 'RUR',
                    }
                }
            },
            'promos': {
                'anaplan_promos': {
                    'all_promos': {
                        'promos': [
                            {
                                'id': 'promo1',
                                'discount_oldprice': {
                                    'id': 'RUR',
                                    'price': 120,
                                }
                            }
                        ]
                    }
                }
            },
            'content': {
                'market': {
                    'category_id': 10,
                    'vendor_id': 5,
                },
                'partner': {
                    'original': {
                        'name': {},
                    }
                }
            },
            'meta': {
                'rgb': DTC.WHITE,
            }
        },
        # not valid saas offer (promos missing), should not be present in output table
        {
            'identifiers': {
                'business_id': 11,
                'offer_id': 'OFFER_HAS_NO_PROMOS',
                'warehouse_id': 0,
                'shop_id': 5,
                'feed_id': 1001,
            },
            'price': {
                'basic': {
                    'binary_price': {
                        'price': 20 * 10 ** 7,
                        'id': 'RUR',
                    }
                }
            },
            'content': {
                'market': {
                    'category_id': 10,
                    'vendor_id': 5,
                },
                'partner': {
                    'original': {
                        'name': {},
                    }
                }
            },
            'meta': {
                'rgb': DTC.WHITE,
            }
        },
        # valid saas offer, all data in saas is actual, should not be present in output table
        {
            'identifiers': {
                'business_id': 111,
                'offer_id': 'OFFER_IN_SAAS_SHOULD_NOT_SEND',
                'warehouse_id': 0,
                'shop_id': 15,
                'feed_id': 1002,
            },
            'price': {
                'basic': {
                    'binary_price': {
                        'price': 20 * 10 ** 7,
                        'id': 'RUR',
                    }
                }
            },
            'promos': {
                'anaplan_promos': {
                    'all_promos': {
                        'promos': [
                            {
                                'id': 'promo1',
                                'discount_oldprice': {
                                    'id': 'RUR',
                                    'price': 120,
                                }
                            }
                        ]
                    }
                }
            },
            'content': {
                'market': {
                    'category_id': 10,
                    'vendor_id': 5,
                },
                'partner': {
                    'original': {
                        'name': {}
                    }
                },
            },
            'meta': {
                'rgb': DTC.WHITE,
            },
        },
    ]


@pytest.fixture(scope='module')
def promo_saas_table(yt_server, config):
    return SaasTable(yt_server, config.yt_promo_saas_tablepath, data=[document_to_ssas_row(msg) for msg in SAAS_DOCS])


@pytest.yield_fixture(scope='module')
def diff_builder(yt_server, config, basic_offers_table, service_offers_table, promo_saas_table):
    resources = {
        'basic_offers_table': basic_offers_table,
        'service_offers_table': service_offers_table,
        'promo_saas_table': promo_saas_table,
        'config': config,
    }
    with PromoSaasDiffBuilderEnv(yt_server, **resources) as routines_env:
        yield routines_env


# если после правок в CreateSaasDocumentByBusinessId у вас упали тесты, отредактируйте SAAS_DOCS
def test_saas_diff_builder(yt_server, config, diff_builder):
    yt_client = yt_server.get_yt_client()
    expected_offer = {
        'identifiers': {
            'business_id': 1,
            'offer_id': 'OFFER_IN_SAAS_SHOULD_SEND',
            'shop_id': 1,
        },
        'meta': {
            'scope': DTC.SERVICE,
            'promo_saas_force_send': not_none(),
        },
    }

    results = list(yt_client.read_table(yt.ypath_join(config.promo_saas_diff_builder_output_dir, 'recent')))
    assert_that(len(results), equal_to(1))
    assert_that(results, has_items(has_entry('offer', IsSerializedProtobuf(DTC.Offer, expected_offer))))

    full_results = list(yt_client.read_table(yt.ypath_join(config.promo_saas_diff_builder_output_dir, 'full', 'recent')))
    assert_that(len(full_results), equal_to(1))
    assert_that(full_results, has_items(has_entry('offer', IsSerializedProtobuf(DTC.Offer, expected_offer))))


def test_delete_lostie_documents(yt_server, config, diff_builder):
    yt_client = yt_server.get_yt_client()
    results = list(yt_client.read_table(yt.ypath_join(config.promo_saas_diff_builder_output_dir, 'lostie', 'recent')))
    assert_that(len(results), equal_to(2))
    assert_that(results, has_items(
        has_entry('value', IsSerializedProtobuf(TMessage, {
            'MessageType': TMessage.TMessageType.DELETE_DOCUMENT,
            'Document': {
                'Url': 's/' + str(IN_SAAS_IN_DATACAMP_NOT_VALID_SHOP_ID) + '/' + IN_SAAS_IN_DATACAMP_NOT_VALID_OFFER_ID,
            }
        })),
        has_entry('value', IsSerializedProtobuf(TMessage, {
            'MessageType': TMessage.TMessageType.DELETE_DOCUMENT,
            'Document': {
                'Url': 's/' + str(IN_SAAS_NOT_IN_DATACAMP_SHOP_ID) + '/' + IN_SAAS_NOT_IN_DATACAMP_OFFER_ID,
            }
        })),
    ))
