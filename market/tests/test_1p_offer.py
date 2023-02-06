# coding: utf-8

import pytest
import tempfile
from hamcrest import assert_that, has_items
import logging
from yt.wrapper import ypath_join

from market.idx.datacamp.miner.yatf.resources.config import MinerConfig
from market.idx.datacamp.miner.yatf.test_env import MinerTestEnv
from market.idx.datacamp.proto.api.DatacampMessage_pb2 import DatacampMessage
from market.idx.datacamp.proto.offer.DataCampOffer_pb2 import BLUE
from market.idx.datacamp.yatf.utils import dict2tskv
from market.idx.yatf.matchers.lb_matchers import HasNoUnreadData
from market.idx.yatf.matchers.protobuf_matchers import IsProtobufMap, IsSerializedProtobuf
from market.idx.yatf.resources.datacamp.datacamp_tables import DataCampPartnersTable
from market.idx.yatf.resources.geo_base_files import ContriesFile
from market.idx.yatf.resources.msku_table import MskuTable
from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix
from market.proto.feedparser.deprecated.OffersData_pb2 import Offer as OfferPb
from market.proto.ir.UltraController_pb2 import EnrichedOffer as EnrichedOfferPb
from market.pylibrary.proto_utils import message_from_data


BUSINESS_ID = 10447296
SHOP_ID = 10264169
WAREHOUSE_ID = 145
MSKU_ID = 552690647


DATACAMP_MESSAGES = [
    {
        'united_offers': [{
            'offer': [{
                'basic': {
                    'identifiers': {
                        'business_id': BUSINESS_ID,
                        'offer_id': 'o1',
                    },
                    'content': {
                        'binding': {
                            'approved': {
                                'market_sku_id': MSKU_ID
                            }
                        },
                        'partner': {
                            'original': {
                                'name': {
                                    'value': 'Test Offer for 1p',
                                },
                                'description': {
                                    'value': 'Description Offer for 1p',
                                },
                            }
                        },
                    },
                },
                'service': {
                    SHOP_ID: {
                        'identifiers': {
                            'business_id': BUSINESS_ID,
                            'offer_id': 'o1',
                            'shop_id': SHOP_ID,
                            'warehouse_id': WAREHOUSE_ID,
                        },
                        'meta': {
                            'rgb': BLUE
                        },
                        'status': {
                            'united_catalog': {
                                'flag': True
                            },
                        },
                        'price': {
                            'basic': {
                                'binary_price': {
                                    'price': 5000000000,
                                }
                            }
                        },
                    }
                }
            }]
        }]
    }
]


GEO_BASE_DIR = tempfile.mkdtemp()


@pytest.fixture(scope='module')
def geo_country_file():
    countries = {
        'Россия': 124,
    }
    return ContriesFile(countries=countries, preset_file_path=GEO_BASE_DIR)


@pytest.fixture(scope='module')
def partner_data():
    return [
        {
            'shop_id': SHOP_ID,
            'mbi':  dict2tskv({
                'shop_id': SHOP_ID,
                'business_id': BUSINESS_ID,
                'datafeed_id': 1,
                'vat': 7,
                'tax_system': 0,
                'blue_status': 'REAL',
                'supplier_type': 1,
                'warehouse_id': 145,
            }),
            'status': 'publish'
        },
    ]


@pytest.fixture(scope='module')
def msku_table_data():
    return [
        {
            'msku': MSKU_ID,
            'feed_id': 20,
            'offer': message_from_data({
                'genlog': {
                    'title': 'msku title from msku table',
                }
            }, OfferPb()).SerializeToString(),
            'uc': message_from_data({
                'vendor_id': 100,
                'category_id': 1000,
            }, EnrichedOfferPb()).SerializeToString(),
        },
    ]


@pytest.fixture(scope='function')
def msku_table_path():
    return ypath_join(get_yt_prefix(), 'in', 'msku', 'recent')


@pytest.fixture(scope='function')
def miner_config(yt_server, yt_token, partner_info_table_path, offers_blog_topic,
                 log_broker_stuff, input_topic, output_topic, geo_country_file,
                 msku_table_path):
    cfg = MinerConfig()
    cfg.create_datacamp_logger_initializer(log_broker_stuff, offers_blog_topic, enable_trace_log=False)
    cfg.create_miner_initializer(
        yt_server=yt_server,
        token=yt_token.path,
        partners_table_path=partner_info_table_path,
    )

    reader = cfg.create_lbk_reader_batch_processor(log_broker_stuff, input_topic, True)
    writer = cfg.create_lbk_writer_processor(log_broker_stuff, output_topic)
    unpacker = cfg.create_proto_unpacker_processor()
    adapter_converter = cfg.create_offer_adapter_converter()
    blue_ware_md5_creator = cfg.create_ware_md5_creator()
    blue_shopsdat_enricher = cfg.create_shopsdat_enricher(color='blue')
    offer_content_converter = cfg.create_offer_content_converter('white')
    blue_offer_content_converter = cfg.create_offer_content_converter('blue')
    offer_price_converter = cfg.create_offer_price_converter()
    geo_enricher = cfg.create_geo_enricher(
        geo_base_dir=GEO_BASE_DIR,
        countries_file=geo_country_file.filename,
    )
    currency_validator = cfg.create_currency_validator()
    uc_enricher_blue = cfg.create_blue_uc_enricher_processor()
    blue_offer_validator = cfg.create_offer_validator(color='blue')

    cfg.create_link(reader, unpacker)
    cfg.create_link(unpacker, adapter_converter)
    cfg.create_link(adapter_converter, blue_shopsdat_enricher)
    cfg.create_link(blue_shopsdat_enricher, offer_content_converter)
    cfg.create_link(offer_content_converter, blue_offer_content_converter)
    cfg.create_link(blue_offer_content_converter, offer_price_converter)
    cfg.create_link(offer_price_converter, geo_enricher)
    cfg.create_link(geo_enricher, currency_validator)
    cfg.create_link(currency_validator, uc_enricher_blue)
    cfg.create_link(uc_enricher_blue, blue_ware_md5_creator)
    cfg.create_link(blue_ware_md5_creator, blue_offer_validator)
    cfg.create_link(blue_offer_validator, writer)

    return cfg


def write_read_offer_lbk(input_topic, output_topic):
    for m in DATACAMP_MESSAGES:
        input_topic.write(message_from_data(m, DatacampMessage()).SerializeToString())

    result = output_topic.read(len(DATACAMP_MESSAGES))
    assert_that(output_topic, HasNoUnreadData())
    return result


@pytest.yield_fixture(scope='function')
def miner(yt_server, miner_config, input_topic, output_topic, offers_blog_topic, partner_info_table_path,
          partner_data, geo_country_file, msku_table_data, msku_table_path):

    resources = {
        'miner_cfg': miner_config,
        'input_topic': input_topic,
        'output_topic': output_topic,
        'partners_table': DataCampPartnersTable(
            yt_stuff=yt_server,
            path=partner_info_table_path,
            data=partner_data),
        'countries_utf8.c2n': geo_country_file,
        'offers_blog_topic': offers_blog_topic,
        'msku_table': MskuTable(
            yt_stuff=yt_server,
            path=msku_table_path,
            data=msku_table_data
        ),
    }
    with MinerTestEnv(**resources) as miner:
        miner.verify()
        yield miner


def test_united_miner(miner, input_topic, output_topic):
    '''Проверяем, что оффера 1р обрабатываются united miner-ом'''

    data = write_read_offer_lbk(input_topic, output_topic)

    msg = DatacampMessage()
    msg.ParseFromString(data[0])
    logging.debug('offer = %s', msg)

    assert_that(data, has_items(IsSerializedProtobuf(DatacampMessage, {
        'united_offers': [{
            'offer': [{
                'basic': {
                    'identifiers': {
                        'business_id': BUSINESS_ID,
                        'offer_id': 'o1',
                    },
                    'content': {
                        'partner': {
                            'actual': {
                                'title': {
                                    # Miner переложил из ориг. партнерских данных
                                    'value': 'Test Offer for 1p',
                                },
                                'description': {
                                    # Miner переложил из ориг. партнерских данных
                                    'value': 'Description Offer for 1p',
                                }
                            },
                        },
                        'binding': {
                            'approved': {
                                'market_sku_id': MSKU_ID,
                            },
                            'blue_uc_mapping': {
                                # Заполняется процессором UcEnricherBlue
                                # Когда все 1p офферы мигрируют в ЕК (united_catalog=true), MARKETINDEXER-42272 - можно будет избавляться от blue_uc_mapping
                                'market_sku_id': MSKU_ID,
                            }
                        }

                    },
                },
                'service': IsProtobufMap({
                    SHOP_ID: {
                        'identifiers': {
                            'business_id': BUSINESS_ID,
                            'offer_id': 'o1',
                            'shop_id': SHOP_ID,
                        },
                        'price': {
                            'basic': {
                                'binary_price': {
                                    'price': 5000000000,
                                }
                            }
                        },
                    }
                })
            }]
        }]})))
