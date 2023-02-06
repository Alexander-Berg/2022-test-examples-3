# coding: utf-8
import os
import tempfile

import pytest
from datetime import datetime
from hamcrest import assert_that, is_not, has_item

from market.pylibrary.proto_utils import message_from_data
from market.proto.content.mbo.Restrictions_pb2 import RestrictionsData, Restriction, Category

from market.idx.yatf.matchers.protobuf_matchers import IsProtobufMap, IsSerializedProtobuf, IsProtobuf  # noqa
from market.idx.yatf.resources.datacamp.datacamp_tables import DataCampMskuTable
from market.idx.yatf.resources.tovar_tree_pb import TovarTreePb, MboCategory
from market.idx.yatf.resources.category_restrictions_pb import CategoryRestrictions
from market.idx.yatf.resources.datacamp.datacamp_tables import DataCampPartnersTable

import market.idx.datacamp.proto.offer.DataCampOffer_pb2 as DTC
from market.idx.datacamp.proto.api.DatacampMessage_pb2 import DatacampMessage
from market.idx.datacamp.proto.models.MarketSkuMboContent_pb2 import MarketSkuMboContent
from market.idx.datacamp.miner.yatf.resources.config import MinerConfig
from market.idx.datacamp.miner.yatf.test_env import MinerTestEnv
from market.idx.datacamp.yatf.utils import dict2tskv
from market.idx.datacamp.yatf.matchers.matchers import HasSerializedDatacampMessages


BUSINESS_ID = 111
WAREHOUSE_ID = 145
SHOP_WITH_LICENCE = 1
SHOP_WITHOUT_LICENCE = 2
TS = 1618763231
MSKU_TS = datetime.utcfromtimestamp(TS).strftime('%Y-%m-%dT%H:%M:%SZ')


@pytest.fixture(scope='module')
def partner_data():
    return [
        {
            'shop_id': SHOP_WITH_LICENCE,
            'mbi': dict2tskv({
                'shop_id': SHOP_WITH_LICENCE,
                'business_id': BUSINESS_ID,
                'is_alive': True,
                'is_enabled': True,
                'sells_medicine': 'REAL',
                'united_catalog_status': 'SUCCESS',
            }),
            'status': 'publish',
        },
        {
            'shop_id': SHOP_WITHOUT_LICENCE,
            'mbi': dict2tskv({
                'shop_id': SHOP_WITHOUT_LICENCE,
                'business_id': BUSINESS_ID,
                'is_alive': True,
                'is_enabled': True,
                'united_catalog_status': 'SUCCESS',
            }),
            'status': 'publish',
        },
    ]

OFFERS = [
    {
        'offer_id': 'full_medicine_and_has_licence',
        'shop_id': SHOP_WITH_LICENCE,
        'msku': 101,
        'category_id': None,
        'approved_category_id': 21,
    },
    {
        'offer_id': 'full_medicine_and_no_licence',
        'shop_id': SHOP_WITHOUT_LICENCE,
        'msku': 101,
        'category_id': 31,  # эту категорию положено игнорить, но мы на нее не посмотрим - тк возьмем из approved
        'approved_category_id': 21
    },
    {
        'offer_id': 'not_medicine_and_no_licence',
        'shop_id': SHOP_WITHOUT_LICENCE,
        'msku': 101,
        'category_id': 31,  # категория наследник упомянутой в restrictions с include_subtree=False => игнорим её
        'approved_category_id': None
    },
    {
        'offer_id': 'supplement_and_no_licence',
        'shop_id': SHOP_WITHOUT_LICENCE,
        'msku': 102,
        'category_id': None,
        'approved_category_id': 21
    },
    {
        'offer_id': 'null_in_med_cargotype_and_no_licence',
        'shop_id': SHOP_WITHOUT_LICENCE,
        'msku': 103,
        'category_id': None,
        'approved_category_id': 21
    },
    {
        'offer_id': 'dbs_full_med_and_no_licence',
        'shop_id': SHOP_WITHOUT_LICENCE,
        'msku': 101,
        'category_id': None,
        'approved_category_id': 21
    },
]


@pytest.fixture(scope='module')
def datacamp_msku_table_data():
    meta = {'timestamp': MSKU_TS, 'source': DTC.MARKET_MBO}

    return [
        {
            # мед кагортип cargoType900=true => лекарство
            'id': 101,
            'mbo_content': message_from_data({
                'meta': meta,
                'msku': {
                    'parameter_values': [{'bool_value': True, 'xsl_name': 'cargoType900'}]
                }
            }, MarketSkuMboContent()).SerializeToString()
        },
        {
            # мед кагортип cargoType900=false => не лекарство, возможно БАД
            'id': 102,
            'mbo_content': message_from_data({
                'meta': meta,
                'msku': {
                    'parameter_values': [{'bool_value': False, 'xsl_name': 'cargoType900'}]
                }
            }, MarketSkuMboContent()).SerializeToString()
        },
        {
            # мед кагортип cargoType900 не указан => есть риск, что лекарство
            'id': 103,
            'mbo_content': message_from_data({
                'meta': meta,
                'msku': {
                    'parameter_values': [{'bool_value': True, 'xsl_name': 'cargoType300'}]
                }
            }, MarketSkuMboContent()).SerializeToString()
        }
    ]


RESTRICTIONS_BASE_DIR = tempfile.mkdtemp()


@pytest.yield_fixture(scope='module')
def categories_tree():
    return TovarTreePb(
        categories=[
            MboCategory(hid=1, tovar_id=1, parent_hid=0,
                        unique_name="Все товары", name="Все товары",
                        output_type=MboCategory.GURULIGHT),

            MboCategory(hid=2, tovar_id=2, parent_hid=1,
                        unique_name="Лекарства 1->2", name="Лекарства 1->2",
                        output_type=MboCategory.GURULIGHT),

            MboCategory(hid=21, tovar_id=21, parent_hid=2,
                        unique_name="Лекарства 2->21", name="Лекарства 2->21",
                        output_type=MboCategory.GURULIGHT),

            MboCategory(hid=3, tovar_id=3, parent_hid=1,
                        unique_name="Лекарства 1->3", name="Лекарства 1->3",
                        output_type=MboCategory.GURULIGHT),

            MboCategory(hid=31, tovar_id=31, parent_hid=3,
                        unique_name="Лекарства 3->31", name="Лекарства 3->31",
                        output_type=MboCategory.GURULIGHT),

            MboCategory(hid=4, tovar_id=4, parent_hid=1,
                        unique_name="Не_лекарства", name="Не_лекарства",
                        output_type=MboCategory.GURULIGHT),
        ],
        preset_file_path=RESTRICTIONS_BASE_DIR,
    )


@pytest.fixture(scope='module')
def category_restrictions():
    return CategoryRestrictions(RestrictionsData(
        restriction=[
            Restriction(
                name='medicine',
                category=[
                    Category(
                        id=2,
                        include_subtree=True  # 21 - категория наследник
                    ),
                    Category(
                        id=3,
                        include_subtree=False  # 31 - категория наследник
                    )
                ]
            ),
        ]
    ), preset_file_path=RESTRICTIONS_BASE_DIR)


@pytest.fixture(scope='module')
def miner_config(log_broker_stuff,
                 categories_tree,
                 category_restrictions,
                 datacamp_msku_table_path,
                 input_topic,
                 offers_blog_topic,
                 output_topic,
                 partner_info_table_path,
                 yt_server,
                 yt_token):
    cfg = MinerConfig()
    reader = cfg.create_lbk_reader_batch_processor(log_broker_stuff, input_topic, united=True)
    writer = cfg.create_lbk_writer_processor(log_broker_stuff, output_topic)
    unpacker = cfg.create_proto_unpacker_processor()
    adapter_converter = cfg.create_offer_adapter_converter()
    cfg.create_datacamp_logger_initializer(log_broker_stuff, offers_blog_topic, enable_trace_log=False)
    cfg.create_miner_initializer(
        yt_server=yt_server,
        categories_tree_path=os.path.join(categories_tree.preset_file_path, categories_tree.filename),
        partners_table_path=partner_info_table_path
    )
    datacamp_msku_enricher = cfg.create_datacamp_msku_enricher_processor(
        yt_server=yt_server,
        yt_token=yt_token.path,
        yt_table_path=datacamp_msku_table_path,
    )

    category_restriction_filepath = os.path.join(RESTRICTIONS_BASE_DIR, category_restrictions.filename)
    category_restrictions_validator = cfg.create_category_restrictions_validator(
        category_restriction_filepath=category_restriction_filepath,
        enable_medicine_restrictions_processing=True
    )
    ware_md5_creator = cfg.create_ware_md5_white_creator()

    cfg.create_link(reader, unpacker)
    cfg.create_link(unpacker, adapter_converter)
    cfg.create_link(adapter_converter, datacamp_msku_enricher)
    # ware_md5_creator значим только как промежуточный *какой-то* процессор
    # между двумя значимыми процессорами сообщающимися через miningContext
    cfg.create_link(datacamp_msku_enricher, ware_md5_creator)
    cfg.create_link(ware_md5_creator, category_restrictions_validator)
    cfg.create_link(category_restrictions_validator, writer)

    return cfg


@pytest.yield_fixture(scope='module')
def miner(categories_tree,
          category_restrictions,
          datacamp_msku_table_data,
          datacamp_msku_table_path,
          input_topic,
          miner_config,
          offers_blog_topic,
          output_topic,
          partner_data,
          partner_info_table_path,
          yt_server):
    resources = {
        'miner_cfg': miner_config,
        'input_topic': input_topic,
        'output_topic': output_topic,
        'category_restrictions': category_restrictions,
        'categories_tree': categories_tree,
        'offers_blog_topic': offers_blog_topic,
        'datacamp_msku_table': DataCampMskuTable(
            yt_stuff=yt_server,
            path=datacamp_msku_table_path,
            data=datacamp_msku_table_data
        ),
        'partners_table': DataCampPartnersTable(
            yt_stuff=yt_server,
            path=partner_info_table_path,
            data=partner_data
        )
    }
    with MinerTestEnv(**resources) as miner:
        miner.verify()
        yield miner


@pytest.yield_fixture(scope='module')
def messages(input_topic, output_topic):
    message = message_from_data({
        'united_offers': [{
            'offer': [{
                'basic': {
                    'identifiers': {
                        'business_id': BUSINESS_ID,
                        'offer_id': offer['offer_id'],
                    },
                    'content': {
                        'binding': {
                            'approved': {
                                'market_sku_id': offer['msku'] if offer['offer_id'] != 'dbs_full_med_and_no_licence' else None,
                                'market_category_id': offer['approved_category_id'],
                            },
                            'uc_mapping': {'market_sku_id': offer['msku']} if offer['offer_id'] == 'dbs_full_med_and_no_licence' else None
                        },
                        'market': {
                            'enriched_offer': {
                                'category_id': offer['category_id']
                            }
                        }
                    },
                },
                'service': {
                    offer['shop_id']: {
                        'identifiers': {
                            'business_id': BUSINESS_ID,
                            'offer_id': offer['offer_id'],
                            'shop_id': offer['shop_id'],
                            'warehouse_id': WAREHOUSE_ID,
                        },
                        'meta': {'rgb': DTC.BLUE if offer['offer_id'] != 'dbs_full_med_and_no_licence' else DTC.WHITE},
                        'status': {
                            'united_catalog': {
                                'flag': True
                            },
                        },
                        'partner_info': None if offer['offer_id'] != 'dbs_full_med_and_no_licence' else {'is_dsbs': True},
                    }}
            } for offer in OFFERS]
        }]}, DatacampMessage())

    input_topic.write(message.SerializeToString())
    return output_topic.read(1, wait_timeout=10)


def test_medicine_offers_cargotype_and_licence(miner, messages, output_topic):
    for offer_id in ['full_medicine_and_no_licence', 'null_in_med_cargotype_and_no_licence', 'dbs_full_med_and_no_licence']:
        assert_that(messages, HasSerializedDatacampMessages([{
            'united_offers': [{
                'offer': [{
                    'basic': {
                        'identifiers': {
                            'business_id': BUSINESS_ID,
                            'offer_id': offer_id,
                        },
                    },
                    'service': IsProtobufMap({
                        SHOP_WITHOUT_LICENCE: {
                            'identifiers': {
                                'business_id': BUSINESS_ID,
                                'offer_id': offer_id,
                                'shop_id': SHOP_WITHOUT_LICENCE,
                                'warehouse_id': WAREHOUSE_ID,
                            },
                            'resolution': {
                                'by_source': [{
                                    'meta': {
                                        'source': DTC.MARKET_IDX,
                                    },
                                    'verdict': [{
                                        'results': [{
                                            'is_banned': True,
                                            'messages': [{
                                                'code': '49m'
                                            }]
                                        }]
                                    }]
                                }]
                            },
                        }
                    })
                }]
            }]
        }]))

    offers_without_error = ['full_medicine_and_has_licence', 'not_medicine_and_no_licence', 'supplement_and_no_licence']
    for offer_id, shop_id in zip(offers_without_error, [SHOP_WITH_LICENCE, SHOP_WITHOUT_LICENCE, SHOP_WITHOUT_LICENCE]):
        assert_that(messages, HasSerializedDatacampMessages([{
            'united_offers': [{
                'offer': [{
                    'basic': {
                        'identifiers': {
                            'business_id': BUSINESS_ID,
                            'offer_id': offer_id,
                        },
                    },
                    'service': IsProtobufMap({
                        shop_id: {
                            'identifiers': {
                                'business_id': BUSINESS_ID,
                                'offer_id': offer_id,
                                'shop_id': shop_id,
                            },
                            'resolution': {
                                'by_source': is_not(has_item(IsProtobuf({
                                    'meta': {
                                        'source': DTC.MARKET_IDX,
                                    },
                                    'verdict': [{
                                        'results': [{
                                            'is_banned': True,
                                            'messages': [{
                                                'code': '49m',
                                            }]
                                        }]
                                    }]
                                })))
                            }
                        },
                    })
                }]
            }]
        }]))
