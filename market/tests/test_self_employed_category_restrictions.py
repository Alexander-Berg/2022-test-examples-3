# coding: utf-8
import os
import tempfile

import pytest
from hamcrest import assert_that, is_not, has_item

from market.pylibrary.proto_utils import message_from_data
from market.proto.content.mbo.Restrictions_pb2 import RestrictionsData, Restriction, Category

from market.idx.yatf.matchers.protobuf_matchers import IsProtobufMap, IsProtobuf  # noqa
from market.idx.yatf.resources.tovar_tree_pb import TovarTreePb, MboCategory
from market.idx.yatf.resources.category_restrictions_pb import CategoryRestrictions
from market.idx.yatf.resources.datacamp.datacamp_tables import DataCampPartnersTable

import market.idx.datacamp.proto.offer.DataCampOffer_pb2 as DTC
from market.idx.datacamp.proto.api.DatacampMessage_pb2 import DatacampMessage
from market.idx.datacamp.miner.yatf.resources.config import MinerConfig
from market.idx.datacamp.miner.yatf.test_env import MinerTestEnv
from market.idx.datacamp.yatf.utils import dict2tskv
from market.idx.datacamp.yatf.matchers.matchers import HasSerializedDatacampMessages


BUSINESS_ID = 111
WAREHOUSE_ID = 145
SELF_EMPLOYED_SHOP = 1
NORMAL_SHOP = 2


@pytest.fixture(scope='module')
def partner_data():
    return [
        {
            'shop_id': SELF_EMPLOYED_SHOP,
            'mbi': dict2tskv({
                'shop_id': SELF_EMPLOYED_SHOP,
                'business_id': BUSINESS_ID,
                'is_alive': True,
                'is_enabled': True,
                'is_self_employed': True,
                'united_catalog_status': 'SUCCESS',
            }),
            'status': 'publish',
        },
        {
            'shop_id': NORMAL_SHOP,
            'mbi': dict2tskv({
                'shop_id': NORMAL_SHOP,
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
        'offer_id': 'self_employed_allowed_category',
        'shop_id': SELF_EMPLOYED_SHOP,
        'category_id': None,
        'approved_category_id': 3,
    },
    {
        'offer_id': 'self_employed_allowed_child_category',
        'shop_id': SELF_EMPLOYED_SHOP,
        'category_id': 41,
        'approved_category_id': None
    },
    {
        'offer_id': 'self_employed_prohibited_category',
        'shop_id': SELF_EMPLOYED_SHOP,
        'category_id': 2,
        'approved_category_id': None
    },
    {
        'offer_id': 'self_employed_prohibited_child_category',
        'shop_id': SELF_EMPLOYED_SHOP,
        'category_id': None,
        'approved_category_id': 31
    },
    {
        'offer_id': 'normal_shop_allowed_category',
        'shop_id': NORMAL_SHOP,
        'category_id': 4,
        'approved_category_id': None
    },
    {
        'offer_id': 'normal_shop_prohibited_acatagory',
        'shop_id': NORMAL_SHOP,
        'category_id': None,
        'approved_category_id': 2
    },
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
                        unique_name="Iphones 1->2", name="Iphones 1->2",
                        output_type=MboCategory.GURULIGHT),

            MboCategory(hid=21, tovar_id=21, parent_hid=2,
                        unique_name="Iphones 2->21", name="Iphones 2->21",
                        output_type=MboCategory.GURULIGHT),

            MboCategory(hid=3, tovar_id=3, parent_hid=1,
                        unique_name="Услуги 1->3", name="Услуги 1->3",
                        output_type=MboCategory.GURULIGHT),

            MboCategory(hid=31, tovar_id=31, parent_hid=3,
                        unique_name="Аренда яхт 3->31", name="Аренда яхт 3->31",
                        output_type=MboCategory.GURULIGHT),

            MboCategory(hid=4, tovar_id=4, parent_hid=1,
                        unique_name="Лапти 1->4", name="Лапти 1->4",
                        output_type=MboCategory.GURULIGHT),

            MboCategory(hid=41, tovar_id=41, parent_hid=4,
                        unique_name="Лапти 4->41", name="Лапти 4->41",
                        output_type=MboCategory.GURULIGHT),

        ],
        preset_file_path=RESTRICTIONS_BASE_DIR,
    )


@pytest.fixture(scope='module')
def category_restrictions():
    return CategoryRestrictions(RestrictionsData(
        restriction=[
            Restriction(
                name='self_employed',
                category=[
                    Category(
                        id=3,
                        include_subtree=False  # 31 - запрещенная категория наследник
                    ),
                    Category(
                        id=4,
                        include_subtree=True  # 41 - разрешенная категория наследник
                    )
                ]
            ),
        ]
    ), preset_file_path=RESTRICTIONS_BASE_DIR)


@pytest.fixture(scope='module')
def miner_config(log_broker_stuff,
                 categories_tree,
                 category_restrictions,
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

    category_restriction_filepath = os.path.join(RESTRICTIONS_BASE_DIR, category_restrictions.filename)
    category_restrictions_validator = cfg.create_category_restrictions_validator(
        category_restriction_filepath=category_restriction_filepath,
        enable_self_employed_restrictions_processing=True
    )
    ware_md5_creator = cfg.create_ware_md5_white_creator()

    cfg.create_link(reader, unpacker)
    cfg.create_link(unpacker, adapter_converter)
    # ware_md5_creator значим только как промежуточный *какой-то* процессор
    # между двумя значимыми процессорами сообщающимися через miningContext
    cfg.create_link(adapter_converter, ware_md5_creator)
    cfg.create_link(ware_md5_creator, category_restrictions_validator)
    cfg.create_link(category_restrictions_validator, writer)

    return cfg


@pytest.yield_fixture(scope='module')
def miner(categories_tree,
          category_restrictions,
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
                                'market_category_id': offer['approved_category_id'],
                            }
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
                        'meta': {'rgb': DTC.BLUE if offer['offer_id'] in ['self_employed_allowed_category', 'self_employed_prohibited_child_category', 'normal_shop_allowed_category'] else DTC.WHITE},
                        'status': {
                            'united_catalog': {
                                'flag': True
                            },
                        },
                    }}
            } for offer in OFFERS]
        }]}, DatacampMessage())

    input_topic.write(message.SerializeToString())
    return output_topic.read(1, wait_timeout=10)


def test_self_employed_offers_category_restrictions(miner, messages, output_topic):
    for offer_id in ['self_employed_prohibited_category', 'self_employed_prohibited_child_category']:
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
                        SELF_EMPLOYED_SHOP: {
                            'identifiers': {
                                'business_id': BUSINESS_ID,
                                'offer_id': offer_id,
                                'shop_id': SELF_EMPLOYED_SHOP,
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
                                                'code': '49p'
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

    offers_without_error = ['self_employed_allowed_category', 'self_employed_allowed_child_category', 'normal_shop_allowed_category', 'normal_shop_prohibited_acatagory']
    for offer_id, shop_id in zip(offers_without_error, [SELF_EMPLOYED_SHOP, SELF_EMPLOYED_SHOP, NORMAL_SHOP, NORMAL_SHOP]):
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
                                                'code': '49p',
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
