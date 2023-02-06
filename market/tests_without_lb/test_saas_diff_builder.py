# coding: utf-8

import pytest
from hamcrest import assert_that, equal_to, has_items, has_entry, not_none, matches_regexp

from kernel.search_zone.protos.searchzone_pb2 import TZone, TAttribute
from saas.protos.rtyserver_pb2 import TMessage
import yt.wrapper as yt

from market.idx.yatf.matchers.protobuf_matchers import IsSerializedProtobuf
from market.idx.yatf.resources.yt_tables.saas_table import SaasTable
from market.idx.yatf.resources.datacamp.datacamp_tables import (
    DataCampServiceOffersTable,
    DataCampPartnersTable
)

import market.idx.datacamp.proto.offer.DataCampOffer_pb2 as DTC
from market.idx.datacamp.routines.yatf.test_env import SaasDiffBuilderEnv
from market.idx.datacamp.routines.yatf.resources.config_mock import RoutinesConfigMock


SAAS_DOCS = [
    TMessage.TDocument(
        DocumentProperties=([
            {'Name': 'offer_id', 'Value': 'OFFER_IN_SAAS'},
            {'Name': 'shop_id', 'Value': '10'},
            {'Name': 'doc_type', 'Value': 'offer'},
        ]),
        KeyPrefix=1,
        Url='s/' + str(1) + '/' + 'OFFER_IN_SAAS',
        SearchAttributes=([
            {'Name': 's_offer_id', 'Value': 'OFFER_IN_SAAS', 'Type': TAttribute.TAttributeType.LITERAL_ATTRIBUTE},
            {'Name': 's_doc_type', 'Value': 'offer', 'Type': TAttribute.TAttributeType.LITERAL_ATTRIBUTE},
            {'Name': 'i_content_cpa_status', 'Value': '0', 'Type': TAttribute.TAttributeType.INTEGER_ATTRIBUTE},
            {'Name': 'i_integral_content_status', 'Value': '0', 'Type': TAttribute.TAttributeType.INTEGER_ATTRIBUTE},
            {'Name': 'i_united_catalog', 'Value': '1', 'Type': TAttribute.TAttributeType.INTEGER_ATTRIBUTE},
            {'Name': 's_shop_id', 'Value': '10', 'Type': TAttribute.TAttributeType.LITERAL_ATTRIBUTE},
            {'Name': 'i_result_status_10', 'Value': '3', 'Type': TAttribute.TAttributeType.INTEGER_ATTRIBUTE},
            {'Name': 'i_creation_hour_ts', 'Value': '0', 'Type': TAttribute.TAttributeType.INTEGER_ATTRIBUTE},
        ]),
        GroupAttributes=([
            {'Name': 'offer_id_ga', 'Value': 'offer_in_saas', 'Type': TAttribute.TAttributeType.LITERAL_ATTRIBUTE},
            {'Name': 'layer', 'Value': '1', 'Type': TAttribute.TAttributeType.INTEGER_ATTRIBUTE},
            {'Name': 'i_result_status', 'Value': '0', 'Type': TAttribute.TAttributeType.INTEGER_ATTRIBUTE},
            {'Name': 'doc_type', 'Value': 'offer', 'Type': TAttribute.TAttributeType.LITERAL_ATTRIBUTE},
            {'Name': 'creation_ts_sort_relevance', 'Value': '953888970', 'Type': TAttribute.TAttributeType.INTEGER_ATTRIBUTE}
        ]),
        Realtime=True,
        RootZone=TZone(Children=[
            TZone(Name='z_offer_id', Text='OFFER_IN_SAAS'),
        ])
    ),
    TMessage.TDocument(
        DocumentProperties=([
            {'Name': 'offer_id', 'Value': 'OFFER_IN_SAAS_WITH_DIFF'},
            {'Name': 'shop_id', 'Value': '10'},
            {'Name': 'doc_type', 'Value': 'offer'},
        ]),
        KeyPrefix=1,
        Url='s/' + str(1) + '/' + 'OFFER_IN_SAAS_WITH_DIFF',
        SearchAttributes=([
            {'Name': 's_offer_id', 'Value': 'OFFER_IN_SAAS_WITH_DIFF', 'Type': TAttribute.TAttributeType.LITERAL_ATTRIBUTE},
            {'Name': 's_doc_type', 'Value': 'offer', 'Type': TAttribute.TAttributeType.LITERAL_ATTRIBUTE},
            {'Name': 'i_content_cpa_status', 'Value': '0', 'Type': TAttribute.TAttributeType.INTEGER_ATTRIBUTE},
            {'Name': 'i_integral_content_status', 'Value': '0', 'Type': TAttribute.TAttributeType.INTEGER_ATTRIBUTE},
            {'Name': 'i_united_catalog', 'Value': '1', 'Type': TAttribute.TAttributeType.INTEGER_ATTRIBUTE},
            {'Name': 's_shop_id', 'Value': '10', 'Type': TAttribute.TAttributeType.LITERAL_ATTRIBUTE},
            {'Name': 'i_result_status_10', 'Value': '3', 'Type': TAttribute.TAttributeType.INTEGER_ATTRIBUTE}
        ]),
        GroupAttributes=([
            {'Name': 'offer_id_ga', 'Value': 'offer_in_saas_with_diff', 'Type': TAttribute.TAttributeType.LITERAL_ATTRIBUTE},
            {'Name': 'layer', 'Value': '1', 'Type': TAttribute.TAttributeType.INTEGER_ATTRIBUTE},
            {'Name': 'i_result_status', 'Value': '0', 'Type': TAttribute.TAttributeType.INTEGER_ATTRIBUTE},
            {'Name': 'doc_type', 'Value': 'offer', 'Type': TAttribute.TAttributeType.LITERAL_ATTRIBUTE},
            {'Name': 'creation_ts_sort_relevance', 'Value': '220832611', 'Type': TAttribute.TAttributeType.INTEGER_ATTRIBUTE}
        ]),
        Realtime=True,
        RootZone=TZone(Children=[
            TZone(Name='z_offer_id', Text='OFFER_IN_SAAS_WITH_DIFF'),
        ])
    ),
    TMessage.TDocument(
        KeyPrefix=1,
        Url='s/1/LOSTIE_DOCUMENT',
    ),
    TMessage.TDocument(
        DocumentProperties=([
            {'Name': 'offer_id', 'Value': 'SYNTHETIC_OFFER_IN_SAAS'},
            {'Name': 'shop_id', 'Value': '10'},
            {'Name': 'doc_type', 'Value': 'offer'},
        ]),
        KeyPrefix=1,
        Url='s/' + str(1) + '/' + 'SYNTHETIC_OFFER_IN_SAAS',
        SearchAttributes=([
            {'Name': 's_offer_id', 'Value': 'SYNTHETIC_OFFER_IN_SAAS', 'Type': TAttribute.TAttributeType.LITERAL_ATTRIBUTE},
            {'Name': 's_doc_type', 'Value': 'offer', 'Type': TAttribute.TAttributeType.LITERAL_ATTRIBUTE},
            {'Name': 'i_content_cpa_status', 'Value': '0', 'Type': TAttribute.TAttributeType.INTEGER_ATTRIBUTE},
            {'Name': 'i_integral_content_status', 'Value': '0', 'Type': TAttribute.TAttributeType.INTEGER_ATTRIBUTE},
            {'Name': 'i_united_catalog', 'Value': '1', 'Type': TAttribute.TAttributeType.INTEGER_ATTRIBUTE},
            {'Name': 's_shop_id', 'Value': '10', 'Type': TAttribute.TAttributeType.LITERAL_ATTRIBUTE},
            {'Name': 'i_result_status_10', 'Value': '3', 'Type': TAttribute.TAttributeType.INTEGER_ATTRIBUTE},
            {'Name': 'i_creation_hour_ts', 'Value': '0', 'Type': TAttribute.TAttributeType.INTEGER_ATTRIBUTE},
        ]),
        GroupAttributes=([
            {'Name': 'offer_id_ga', 'Value': 'synthetic_offer_in_saas', 'Type': TAttribute.TAttributeType.LITERAL_ATTRIBUTE},
            {'Name': 'layer', 'Value': '1', 'Type': TAttribute.TAttributeType.INTEGER_ATTRIBUTE},
            {'Name': 'i_result_status', 'Value': '0', 'Type': TAttribute.TAttributeType.INTEGER_ATTRIBUTE},
            {'Name': 'doc_type', 'Value': 'offer', 'Type': TAttribute.TAttributeType.LITERAL_ATTRIBUTE},
            {'Name': 'creation_ts_sort_relevance', 'Value': '953888970', 'Type': TAttribute.TAttributeType.INTEGER_ATTRIBUTE}
        ]),
        Realtime=True,
        RootZone=TZone(Children=[
            TZone(Name='z_offer_id', Text='SYNTHETIC_OFFER_IN_SAAS'),
        ])
    ),
]


def document_to_saas_row(doc):
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
            'saas_diff_builder': {
                'enable': True,
                'yt_proxies': [yt_server.get_yt_client().config["proxy"]["url"]],
                'output_dir': 'saas_diff',
                'output_rows_limit': 50,
            }
        })
    return config


def create_partner_content():
    return {
        'original': {
            'barcode': {
                'value': [
                    '123',
                    '321',
                ],
            },
        },
    }


@pytest.fixture(scope='module')
def basic_offers_table_data():
    return [
        {
            'identifiers': {
                'business_id': 1,
                'offer_id': 'OFFER_IN_SAAS',
                'feed_id': 1000,
            },
            'content': {
                'partner': create_partner_content(),
            },
        },
        {
            'identifiers': {
                'business_id': 1,
                'offer_id': 'OFFER_IN_SAAS_WITH_DIFF',
                'feed_id': 1000,
            },
            'content': {
                'binding': {
                    'approved': {
                        'market_sku_id': 123,
                    }
                },
                'partner': create_partner_content(),
                'status': {
                    'content_system_status': {
                        'sku_mapping_confidence': DTC.MAPPING_CONFIDENCE_AUTO,
                    }
                }
            }
        },
        {
            'identifiers': {
                'business_id': 2,
                'offer_id': 'OFFER_NOT_IN_SAAS',
                'feed_id': 1000,
            },
            'content': {
                'partner': create_partner_content(),
            },
        },
        {
            'identifiers': {
                'business_id': 2,
                'offer_id': 'OFFER_NOT_IN_SAAS_WITHOUT_CONTENT',
                'feed_id': 1000,
            },
        },
        {
            'identifiers': {
                'business_id': 1,
                'offer_id': 'SYNTHETIC_OFFER_IN_SAAS',
                'feed_id': 1000,
            },
            'content': {
                'partner': create_partner_content(),
            },
            'meta': {
                'synthetic': True
            },
        },
    ]


@pytest.fixture(scope='module')
def service_offers_table_data():
    return [
        {
            'identifiers': {
                'business_id': 1,
                'offer_id': 'OFFER_IN_SAAS',
                'warehouse_id': 0,
                'shop_id': 1 * 10,
                'feed_id': 1000,
            },
            'status': {
                'united_catalog': {
                    'flag': True,
                }
            }
        },
        {
            'identifiers': {
                'business_id': 1,
                'offer_id': 'OFFER_IN_SAAS_WITH_DIFF',
                'warehouse_id': 0,
                'shop_id': 1 * 10,
                'feed_id': 1000,
            },
            'status': {
                'united_catalog': {
                    'flag': True,
                }
            }
        },
        {
            'identifiers': {
                'business_id': 2,
                'offer_id': 'OFFER_NOT_IN_SAAS',
                'warehouse_id': 0,
                'shop_id': 2 * 10,
                'feed_id': 1000,
            },
            'status': {
                'united_catalog': {
                    'flag': True,
                }
            }
        },
        {
            'identifiers': {
                'business_id': 2,
                'offer_id': 'OFFER_NOT_IN_SAAS_WITHOUT_CONTENT',
                'warehouse_id': 0,
                'shop_id': 2 * 10,
                'feed_id': 1000,
            },
            'status': {
                'united_catalog': {
                    'flag': True,
                }
            }
        },
        {
            'identifiers': {
                'business_id': 1,
                'offer_id': 'SYNTHETIC_OFFER_IN_SAAS',
                'warehouse_id': 0,
                'shop_id': 1 * 10,
                'feed_id': 1000,
            },
            'status': {
                'united_catalog': {
                    'flag': True,
                }
            },
            'meta': {
                'synthetic': True
            },
        },
    ]


@pytest.fixture(scope='module')
def actual_service_offers_table(yt_server, config):
    return DataCampServiceOffersTable(yt_server, config.yt_actual_service_offers_tablepath)


@pytest.fixture(scope='module')
def partners_table(yt_server, config):
    return DataCampPartnersTable(yt_server, config.yt_partners_tablepath)


@pytest.fixture(scope='module')
def saas_table(yt_server, config):
    return SaasTable(yt_server, config.yt_saas_tablepath, data=[document_to_saas_row(msg) for msg in SAAS_DOCS])


@pytest.yield_fixture(scope='module')
def diff_builder(
        yt_server,
        config,
        basic_offers_table,
        service_offers_table,
        actual_service_offers_table,
        saas_table,
        partners_table
):
    resources = {
        'basic_offers_table': basic_offers_table,
        'service_offers_table': service_offers_table,
        'actual_service_offers_table': actual_service_offers_table,
        'saas_table': saas_table,
        'config': config,
        'partners_table': partners_table
    }
    with SaasDiffBuilderEnv(yt_server, **resources) as routines_env:
        yield routines_env


# если после правок в CreateSaasDocumentByBusinessId у вас упали тесты, отредактируйте SAAS_DOCS
def test_saas_diff_builder(yt_server, config, diff_builder):
    yt_client = yt_server.get_yt_client()
    expected_offer = {
        'identifiers': {
            'business_id': 2,
            'offer_id': 'OFFER_NOT_IN_SAAS',
        },
        'meta': {
            'scope': DTC.BASIC,
            'saas_force_send': not_none(),
        },
    }

    expected_diff_regexp = '.*deleted: SearchAttributes.*{ Name: "i_current_mapping_state" Value: "1" Type: INTEGER_ATTRIBUTE }.*'

    results = list(yt_client.read_table(yt.ypath_join(config.saas_diff_builder_output_dir, 'recent')))
    assert_that(len(results), equal_to(2))
    assert_that(results, has_items(has_entry('offer', IsSerializedProtobuf(DTC.Offer, expected_offer))))
    assert_that(results, has_items(has_entry('diff', matches_regexp(expected_diff_regexp))))

    full_results = list(yt_client.read_table(yt.ypath_join(config.saas_diff_builder_output_dir, 'full', 'recent')))
    assert_that(full_results, has_items(has_entry('offer', IsSerializedProtobuf(DTC.Offer, expected_offer))))
    assert_that(len(full_results), equal_to(2))


def test_delete_lostie_documents(yt_server, config, diff_builder):
    yt_client = yt_server.get_yt_client()
    results = list(yt_client.read_table(yt.ypath_join(config.saas_diff_builder_output_dir, 'lostie', 'recent')))
    assert_that(len(results), equal_to(2))
    assert_that(
        results,
        has_items(
            has_entry(
                'value',
                IsSerializedProtobuf(TMessage, {
                    'MessageType': TMessage.TMessageType.DELETE_DOCUMENT,
                    'Document': {
                        'Url': 's/1/LOSTIE_DOCUMENT',
                    }
                })
            ),
            has_entry(
                'value',
                IsSerializedProtobuf(TMessage, {
                    'MessageType': TMessage.TMessageType.DELETE_DOCUMENT,
                    'Document': {
                        'Url': 's/1/SYNTHETIC_OFFER_IN_SAAS',
                    }
                })
            ),
        )
    )
