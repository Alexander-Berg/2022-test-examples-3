# coding: utf-8
import json

import pytest
import os
import yatest
import tempfile
from hamcrest import assert_that, equal_to, empty

from yatest.common.network import PortManager
from market.idx.datacamp.miner.yatf.resources.config import MinerConfig
from market.idx.datacamp.miner.yatf.test_env import MinerTestEnv
from market.idx.datacamp.proto.api.DatacampMessage_pb2 import DatacampMessage
from market.idx.datacamp.yatf.matchers.matchers import HasSerializedDatacampMessages
from market.idx.datacamp.yatf.utils import dict2tskv
from market.idx.feeds.feedparser.yatf.resources.delivery_calc import DeliveryCalcServer
from market.idx.feeds.feedparser.yatf.resources.ucdata_pbs import UcHTTPData
from market.idx.yatf.matchers.lb_matchers import HasNoUnreadData
from market.idx.yatf.resources.categories_dimensions import CategoriesDimensions
from market.idx.yatf.resources.datacamp.datacamp_tables import DataCampPartnersTable
from market.idx.yatf.resources.geo_base_files import ContriesFile
from market.pylibrary.proto_utils import message_from_data

DC_GENERATION = 10
BUSINESS_CATALOG_OFFERS_BUSINESS_ID = 5

DATACAMP_MESSAGES = [{
    'united_offers': [{
        'offer': [
            # Оффера из ассортиментного каталога под бизнесом без сервисных частей
            {
                'basic': {
                    'identifiers': {
                        'business_id': BUSINESS_CATALOG_OFFERS_BUSINESS_ID,
                        'offer_id': 'business_catalog_offer_3',
                    },
                    'meta': {
                        'business_catalog': {
                            'flag': True
                        }
                    },
                    'content': {
                        'partner': {
                            'original': {
                                'name': {
                                    'value': 'business_catalog_offer_3_name',
                                }
                            }
                        }
                    }
                }
            },
        ]
    }]
}]

GEO_BASE_DIR = tempfile.mkdtemp()

CATEGORIES_DIMENSIONS_FILE = {
    'category_id': 100500,
    'weight': 123.23,
    'height': 111,
    'length': 222,
    'width': 333
}

UC_DATA = {
    'category_id': 1009492,
    'classification_type_value': 0,
    'classifier_category_id': 1009492,
    'cluster_created_timestamp': 1558365276902,
    'cluster_id': -1,
    'clutch_type': 103,
    'clutch_vendor_id': 6321244,
    'configuration_id': 0,
    'duplicate_offer_group_id': 0,
    'enrich_type': 0,
    'generated_red_title_status': 1,
    'guru_category_id': 14692853,
    'light_match_type': 2,
    'light_model_id': 0,
    'light_modification_id': 0,
    'long_cluster_id': 100390720808,
    'mapped_id': 90401,
    'model_id': 0,
}


@pytest.fixture(scope='module')
def shop_meta():
    return {
        'generationId': DC_GENERATION,
        'currencies': ['KZT']
    }


def merge_dicts(dict1, dict2):
    final_dict = dict1.copy()
    final_dict.update(dict2)
    return final_dict


@pytest.fixture(scope='module')
def geo_country_file():
    countries = {
        'Россия': 124,
        'Италия': 125,
    }
    return ContriesFile(countries=countries, preset_file_path=GEO_BASE_DIR)


@pytest.fixture(scope='module')
def additional_geo_country_file():
    countries = {
        'Китай': 126,
    }
    return ContriesFile(countries=countries, filename='additional_countries_utf8.c2n', preset_file_path=GEO_BASE_DIR)


@pytest.yield_fixture(scope="module")
def currency_rates_path():
    return os.path.join(
        yatest.common.source_path(),
        'market', 'idx', 'yatf', 'resources', 'stubs', 'getter', 'mbi', 'currency_rates_no_bank_for_EUR.xml'
    )


@pytest.fixture(scope='module')
def partner_data():
    return [
        {
            'shop_id': 1,
            'mbi':  dict2tskv({
                'shop_id': 1,
                'business_id': 1,
                'datafeed_id': 1,
                'vat': 7,
                'is_site_market': 'true'
            }),
            'status': 'publish'
        },
    ]


@pytest.yield_fixture(scope='module')
def uc_server():
    with PortManager() as pm:
        port = pm.get_port()
        server = UcHTTPData.from_dict([UC_DATA], port=port)
        yield server


@pytest.yield_fixture(scope='function')
def dc_server(shop_meta):
    meta_response = json.dumps(shop_meta)
    shop_offers_response = {
        'generation_id': DC_GENERATION,
    }
    with PortManager() as pm:
        port = pm.get_port()

        server = DeliveryCalcServer(feed_response=None,
                                    offer_responses=None,
                                    shop_offers_responses=[shop_offers_response, shop_offers_response, shop_offers_response],
                                    shop_meta_response=[meta_response, meta_response, meta_response],
                                    port=port)
        yield server


@pytest.fixture()
def categories_dimensions(tmpdir):
    catdim = CategoriesDimensions()
    catdim.add_record(CATEGORIES_DIMENSIONS_FILE['category_id'],
                      weight=CATEGORIES_DIMENSIONS_FILE['weight'],
                      length=CATEGORIES_DIMENSIONS_FILE['length'],
                      width=CATEGORIES_DIMENSIONS_FILE['width'],
                      height=CATEGORIES_DIMENSIONS_FILE['height'],
                      courier_exp=True,
                      pickup_exp=True)
    catdim_path = os.path.join(str(tmpdir), 'categories_dimensions.csv')
    catdim.dump(catdim_path)
    return catdim_path, catdim


@pytest.fixture(scope='function')
def miner_config(yt_server, yt_token, partner_info_table_path, offers_blog_topic,
                 log_broker_stuff, input_topic, output_topic, geo_country_file, additional_geo_country_file,
                 uc_server, dc_server, categories_dimensions, currency_rates_path):
    cfg = MinerConfig()
    cfg.create_datacamp_logger_initializer(log_broker_stuff, offers_blog_topic, enable_trace_log=False)
    cfg.create_miner_initializer(
        yt_server=yt_server,
        token=yt_token.path,
        partners_table_path=partner_info_table_path,
        currency_rates=currency_rates_path,
    )

    reader = cfg.create_lbk_reader_batch_processor(log_broker_stuff, input_topic, True)
    writer = cfg.create_lbk_writer_processor(log_broker_stuff, output_topic)
    unpacker = cfg.create_proto_unpacker_processor()
    adapter_converter = cfg.create_offer_adapter_converter()
    ware_md5_creator = cfg.create_ware_md5_white_creator()
    blue_ware_md5_creator = cfg.create_ware_md5_creator()
    category_verifier = cfg.create_category_verifier()
    white_category_tree_validator = cfg.create_category_tree_validator(color='white')
    shopsdat_enricher = cfg.create_shopsdat_enricher(color='white')
    blue_shopsdat_enricher = cfg.create_shopsdat_enricher(color='blue')
    offer_content_converter = cfg.create_offer_content_converter('white')
    offer_price_converter = cfg.create_offer_price_converter()
    geo_enricher = cfg.create_geo_enricher(
        geo_base_dir=GEO_BASE_DIR,
        countries_file=geo_country_file.filename,
        additional_countries_file=additional_geo_country_file.filename
    )
    currency_validator = cfg.create_currency_validator()
    pictures_enricher = cfg.create_pictures_enricher()
    uc_enricher = cfg.create_uc_enricher_processor(uc_server)
    dc_enricher = cfg.create_delivery_calc_enricher_processor(
        dc_server,
        color='white',
        use_average_dimensions_and_weight=True,
        categories_dimensions_path=categories_dimensions[0]
    )
    blue_dc_enricher = cfg.create_delivery_calc_enricher_processor(
        dc_server,
        color='blue',
    )
    category_restrictions_validator = cfg.create_category_restrictions_validator()
    offer_validator = cfg.create_offer_validator(color='white')
    blue_offer_validator = cfg.create_offer_validator(color='blue')

    cfg.create_link(reader, unpacker)
    cfg.create_link(unpacker, adapter_converter)
    cfg.create_link(adapter_converter, shopsdat_enricher)
    cfg.create_link(shopsdat_enricher, blue_shopsdat_enricher)
    cfg.create_link(blue_shopsdat_enricher, ware_md5_creator)
    cfg.create_link(ware_md5_creator, blue_ware_md5_creator)
    cfg.create_link(blue_ware_md5_creator, offer_content_converter)
    cfg.create_link(offer_content_converter, offer_price_converter)
    cfg.create_link(offer_price_converter, geo_enricher)
    cfg.create_link(geo_enricher, currency_validator)
    cfg.create_link(currency_validator, pictures_enricher)
    cfg.create_link(pictures_enricher, uc_enricher)
    cfg.create_link(uc_enricher, dc_enricher)
    cfg.create_link(dc_enricher, blue_dc_enricher)
    cfg.create_link(blue_dc_enricher, category_verifier)
    cfg.create_link(category_verifier, white_category_tree_validator)
    cfg.create_link(white_category_tree_validator, category_restrictions_validator)
    cfg.create_link(category_restrictions_validator, offer_validator)
    cfg.create_link(offer_validator, blue_offer_validator)
    cfg.create_link(blue_offer_validator, writer)

    return cfg


@pytest.yield_fixture(scope='function')
def miner(yt_server, miner_config, input_topic, output_topic, offers_blog_topic, partner_info_table_path,
          partner_data, geo_country_file, additional_geo_country_file, uc_server, dc_server):

    resources = {
        'miner_cfg': miner_config,
        'input_topic': input_topic,
        'output_topic': output_topic,
        'partners_table': DataCampPartnersTable(
            yt_stuff=yt_server,
            path=partner_info_table_path,
            data=partner_data),
        'countries_utf8.c2n': geo_country_file,
        'additional_countries_utf8.c2n': additional_geo_country_file,
        'offers_blog_topic': offers_blog_topic,
        'uc_server': uc_server,
        'dc_server': dc_server
    }
    with MinerTestEnv(**resources) as miner:
        miner.verify()
        yield miner


def write_read_offer_lbk(input_topic, output_topic):
    for m in DATACAMP_MESSAGES:
        input_topic.write(message_from_data(m, DatacampMessage()).SerializeToString())

    result = output_topic.read(count=len(DATACAMP_MESSAGES), wait_timeout=5)
    assert_that(output_topic, HasNoUnreadData())
    return result


def test_united_miner(miner, input_topic, output_topic):
    data = write_read_offer_lbk(input_topic, output_topic)

    assert_that(data, HasSerializedDatacampMessages([{
        'united_offers': [{
            'offer': [{
                'basic': {
                    'identifiers': {
                        'business_id': BUSINESS_CATALOG_OFFERS_BUSINESS_ID,
                        'offer_id': 'business_catalog_offer_3',
                    },
                    'meta': {
                        'business_catalog': {
                            'flag': True
                        }
                    },
                    'content': {
                        'partner': {
                            'actual': {
                                'title': {
                                    'value': 'business_catalog_offer_3_name',
                                },
                            },
                        }
                    },
                },
                'service': empty()
            }]
        }]
    }]))

    # Нет лишних офферов, которые не должны были майниться
    msg = DatacampMessage()
    msg.ParseFromString(data[0])
    assert_that(len(msg.united_offers[0].offer), equal_to(1))
