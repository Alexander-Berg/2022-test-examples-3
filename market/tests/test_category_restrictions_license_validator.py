# coding: utf-8
import os
import tempfile

import pytest
from hamcrest import assert_that, has_item, is_not

from market.idx.datacamp.miner.yatf.resources.config import MinerConfig
from market.idx.datacamp.miner.yatf.test_env import MinerTestEnv
from market.idx.datacamp.proto.api.DatacampMessage_pb2 import DatacampMessage
from market.idx.datacamp.proto.offer.UnitedOffer_pb2 import (
    MARKET_IDX,
    BLUE,
    WHITE,
)
from market.proto.content.mbo.Restrictions_pb2 import (
    RestrictionsData,
    Restriction,
    RegionalRestrictions,
    Category,
    Region,
)

from market.idx.datacamp.yatf.matchers.matchers import HasSerializedDatacampMessages
from market.idx.yatf.matchers.protobuf_matchers import IsProtobufMap, IsProtobuf
from market.idx.datacamp.yatf.utils import dict2tskv
from market.pylibrary.proto_utils import message_from_data

from market.idx.yatf.resources.category_restrictions_pb import CategoryRestrictions
from market.idx.yatf.resources.tovar_tree_pb import TovarTreePb, MboCategory
from market.idx.yatf.resources.datacamp.datacamp_tables import DataCampPartnersTable

RESTRICTIONS_BASE_DIR = tempfile.mkdtemp()

JEWELRY = 'jewelry'
JEWELRY_WEIGHT = 'jewelry_weight'

SHOP_WITH_ALL_LICENSES = 333
SHOP_WITHOUT_JEWELRY_LICENSE = 131313
WHITE_SHOP = 1234
SHOP_NOT_IN_SHOPSDAT = 8764

BLUE_BUSINESS = 9000
WHITE_BUSINESS = 4321
DSBS_BUSINESS = 9001
ADV_BUSINESS = 2

DBS_RESTRICTION_NAME = 'dbs_restriction_name'
DBS_CATEGORY_ID = 11223344
NOT_DBS_CATEGORY_ID = 11223355
DBS_MARKET_SKU_ID = 44332211


@pytest.fixture(scope='module')
def partner_data():
    return [
        {
            'shop_id': WHITE_SHOP,
            'mbi': dict2tskv({
                'shop_id': WHITE_SHOP,
                'business_id': WHITE_BUSINESS,
                'is_alive': True,
                'is_enabled': True,
                'united_catalog_status': 'SUCCESS',
            }),
            'status': 'publish',
        },
        {
            'shop_id': SHOP_WITH_ALL_LICENSES,
            'mbi': dict2tskv({
                'shop_id': SHOP_WITH_ALL_LICENSES,
                'business_id': BLUE_BUSINESS,
                'blue_status': 'REAL',
                'is_alive': True,
                'is_enabled': True,
                'sells_jewelry': 'REAL',
                'united_catalog_status': 'SUCCESS',
            }),
            'status': 'publish',
        },
        {
            'shop_id': SHOP_WITH_ALL_LICENSES,
            'mbi': dict2tskv({
                'shop_id': SHOP_WITH_ALL_LICENSES,
                'business_id': DSBS_BUSINESS,
                'is_dsbs': 'REAL',
                'is_alive': True,
                'is_enabled': True,
                'sells_jewelry': 'REAL',
                'united_catalog_status': 'SUCCESS',
            }),
            'status': 'publish',
        },
        {
            'shop_id': SHOP_WITH_ALL_LICENSES,
                'mbi': dict2tskv({
                'shop_id': SHOP_WITH_ALL_LICENSES,
                'business_id': ADV_BUSINESS,
                'is_alive': True,
                'is_enabled': True,
                'sells_jewelry': 'REAL',
                'united_catalog_status': 'SUCCESS',
            }),
            'status': 'publish',
        },
        {
            'shop_id': SHOP_WITHOUT_JEWELRY_LICENSE,
            'mbi': dict2tskv({
                'shop_id': SHOP_WITHOUT_JEWELRY_LICENSE,
                'business_id': DSBS_BUSINESS,
                'is_alive': True,
                'is_enabled': True,
                'sells_jewelry': 'NO',
                'united_catalog_status': 'SUCCESS',
            }),
            'status': 'publish',
        },
    ]


@pytest.yield_fixture(scope='module')
def categories_tree():
    return TovarTreePb(
        categories=[
            MboCategory(hid=1, tovar_id=1, parent_hid=0,
                        unique_name="Все товары", name="Все товары",
                        output_type=MboCategory.GURULIGHT),

            MboCategory(hid=91273, tovar_id=1, parent_hid=1,
                        unique_name="Ювелирные украшения", name="Ювелирные украшения",
                        output_type=MboCategory.GURULIGHT),

            MboCategory(hid=91274, tovar_id=2, parent_hid=91273,
                        unique_name="Ювелирные кольца и перстни", name="Кольца и перстни",
                        output_type=MboCategory.GURULIGHT),

            MboCategory(hid=6206932, tovar_id=101, parent_hid=1,
                        unique_name="Посуда и сувениры из драгоценных металлов", name="Ювелирная посуда и сувениры",
                        output_type=MboCategory.GURULIGHT),

            MboCategory(hid=6206933, tovar_id=102, parent_hid=6206932,
                        unique_name="Ювелирные вилки", name="Вилки",
                        output_type=MboCategory.GURULIGHT),

            MboCategory(hid=8475840, tovar_id=123456789, parent_hid=1,
                        unique_name="Обсидиановые топоры", name="Топоры",
                        output_type=MboCategory.GURULIGHT),

            MboCategory(hid=DBS_CATEGORY_ID, tovar_id=DBS_CATEGORY_ID, parent_hid=1,
                        unique_name="Dbs category id", name="Dbs category id",
                        output_type=MboCategory.GURULIGHT),
        ],
        preset_file_path=RESTRICTIONS_BASE_DIR,
    )


@pytest.fixture(scope='module')
def category_restrictions():
    return CategoryRestrictions(RestrictionsData(
        restriction=[
            Restriction(
                name=JEWELRY,
                category=[
                    Category(
                        id=6206932,  # subcat is 6206933
                    ),
                    Category(
                        id=91273,  # subcat is 91274
                        include_subtree=True
                    )
                ],
                regional_restriction=[
                    RegionalRestrictions(
                        region=[
                            Region(
                                id=149,
                                include_subtree=True,
                            ),
                            Region(
                                id=159,
                                include_subtree=True,
                            ),
                        ],
                        show_content=True,
                        display_only_matched_offers=False,
                        delivery=False,
                        on_blue=True,
                        on_white=True,
                        defaultClassificationHid=0,
                        banned=False,
                    )
                ],
            ),
            Restriction(
                name=JEWELRY_WEIGHT,
                category=[
                    Category(
                        id=91274,
                        include_subtree=True
                    ),
                    Category(
                        id=6206933,
                        include_subtree=True
                    ),
                ],
                regional_restriction=[
                    RegionalRestrictions(
                        show_content=True,
                        display_only_matched_offers=False,
                        delivery=True,
                        on_blue=True,
                        on_white=True,
                        defaultClassificationHid=0,
                        banned=False,
                    )
                ],
            ),
            Restriction(
                name=DBS_RESTRICTION_NAME,
                category=[
                    Category(
                        id=DBS_CATEGORY_ID,
                        include_subtree=False
                    )
                ],
            ),
        ]
    ), preset_file_path=RESTRICTIONS_BASE_DIR)


@pytest.fixture(scope='module')
def miner_config(
    yt_server,
    log_broker_stuff,
    input_topic, output_topic, offers_blog_topic,
    category_restrictions, categories_tree,
    partner_info_table_path
):
    cfg = MinerConfig()
    cfg.create_datacamp_logger_initializer(log_broker_stuff, offers_blog_topic, enable_trace_log=False)

    category_restriction_filepath = os.path.join(RESTRICTIONS_BASE_DIR, category_restrictions.filename)

    reader = cfg.create_lbk_reader_batch_processor(log_broker_stuff, input_topic, united=True)
    unpacker = cfg.create_proto_unpacker_processor()
    adapter_converter = cfg.create_offer_adapter_converter()
    cfg.create_miner_initializer(
        yt_server=yt_server,
        categories_tree_path=os.path.join(categories_tree.preset_file_path, categories_tree.filename),
        partners_table_path=partner_info_table_path
    )
    category_restrictions_validator = cfg.create_category_restrictions_validator(
        category_restriction_filepath=category_restriction_filepath,
        enable_jewelry_restrictions=True,
        enable_dbs_without_mapping_restrictions=True,
        dbs_restriction_name=DBS_RESTRICTION_NAME,
    )
    writer = cfg.create_lbk_writer_processor(log_broker_stuff, output_topic)

    cfg.create_link(reader, unpacker)
    cfg.create_link(unpacker, adapter_converter)
    cfg.create_link(adapter_converter, category_restrictions_validator)
    cfg.create_link(category_restrictions_validator, writer)

    return cfg


@pytest.yield_fixture(scope='module')
def miner(
    yt_server,
    miner_config,
    input_topic, output_topic, offers_blog_topic,
    category_restrictions, categories_tree,
    partner_info_table_path, partner_data
):
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


def test_offer_out_of_restrictions_passes(miner, input_topic, output_topic):
    offer_id = 'offer_out_of_restrictions'
    category_id = 8475840
    model_id = 123456789
    matched_id = 111

    request = message_from_data({
        'united_offers': [{
            'offer': [{
                'basic': {
                    'identifiers': {
                        'business_id': WHITE_BUSINESS,
                        'offer_id': offer_id,
                    },
                    'content': {
                        'binding': {
                            'approved': {
                                'market_category_id': category_id
                            }
                        },
                        'market': {
                            'enriched_offer': {
                                'category_id': category_id+100,  # эту категорию не возьмем, будет использована из approved
                                'model_id': model_id,
                                'matched_id': matched_id,
                            },
                            'ir_data': {
                                'matched_id': matched_id,
                            },
                        },
                    },
                },
                'service': {
                    WHITE_SHOP: {
                        'identifiers': {
                            'business_id': WHITE_BUSINESS,
                            'offer_id': offer_id,
                            'shop_id': WHITE_SHOP,
                        },
                        'status': {
                            'disabled': [{
                                'flag': False,
                                'meta': {
                                    'source': MARKET_IDX
                                }
                            }],
                            'united_catalog': {
                                'flag': True,
                            },
                        },
                        'meta': {
                            'rgb': WHITE,
                        },
                    }
                }
            }]
        }]
    }, DatacampMessage())

    input_topic.write(request.SerializeToString())
    data = output_topic.read(count=1)

    assert_that(data, HasSerializedDatacampMessages([{
        'united_offers': [{
            'offer': [{
                'basic': {
                    'identifiers': {
                        'business_id': WHITE_BUSINESS,
                        'offer_id': offer_id,
                    },
                },
                'service': IsProtobufMap({
                    WHITE_SHOP: {
                        'identifiers': {
                            'business_id': WHITE_BUSINESS,
                            'offer_id': offer_id,
                            'shop_id': WHITE_SHOP,
                        },
                        'meta': {
                            'rgb': WHITE,
                        },
                        'resolution': is_not(has_item(IsProtobuf({
                            'by_source': [{
                                'meta': {
                                    'source': MARKET_IDX,
                                },
                                'verdict': [{
                                    'results': [{
                                        'is_banned': True,
                                        'messages': [{
                                            'code': '49l'
                                        }]
                                    }]
                                }]
                            }]
                        }))),
                    }
                })
            }]
        }]
    }]))


def test_shop_with_license(miner, input_topic, output_topic):
    """Проверяем, что майнер пропускает оффера из категории jewelry для магазинов с sells_jewelry REAL"""
    jewelry_tovar_ids = [1, 2, 101, 102]

    request = message_from_data({
        'united_offers': [{
            'offer': [{
                'basic': {
                    'identifiers': {
                        'business_id': BLUE_BUSINESS,
                        'offer_id': 'sells_jewelry_' + str(shop_sku),
                    },
                    'content': {
                        'market': {
                            'enriched_offer': {
                                'category_id': category_id,
                                'model_id': shop_sku,
                                'matched_id': shop_sku,
                            },
                            'ir_data': {
                                'matched_id': shop_sku,
                            },
                        },
                    },
                },
                'service': {
                    SHOP_WITH_ALL_LICENSES: {
                        'identifiers': {
                            'business_id': BLUE_BUSINESS,
                            'offer_id': 'sells_jewelry_' + str(shop_sku),
                            'shop_id': SHOP_WITH_ALL_LICENSES,
                        },
                        'status': {
                            'disabled': [{
                                'flag': False,
                                'meta': {
                                    'source': MARKET_IDX
                                }
                            }],
                            'united_catalog': {
                                'flag': True,
                            },
                        },
                        'meta': {
                            'rgb': BLUE,
                        },
                    }
                }
            }]
        } for category_id, shop_sku in zip([91273, 91274, 6206932, 6206933], jewelry_tovar_ids)]
    }, DatacampMessage())

    input_topic.write(request.SerializeToString())
    data = output_topic.read(count=1)

    for shop_sku in jewelry_tovar_ids:
        assert_that(data, HasSerializedDatacampMessages([{
            'united_offers': [{
                'offer': [{
                    'basic': {
                        'identifiers': {
                            'business_id': BLUE_BUSINESS,
                            'offer_id': 'sells_jewelry_' + str(shop_sku),
                        },
                    },
                    'service': IsProtobufMap({
                        SHOP_WITH_ALL_LICENSES: {
                            'identifiers': {
                                'business_id': BLUE_BUSINESS,
                                'offer_id': 'sells_jewelry_' + str(shop_sku),
                                'shop_id': SHOP_WITH_ALL_LICENSES,
                            },
                            'meta': {
                                'rgb': BLUE,
                            },
                            'resolution': is_not(has_item(IsProtobuf({
                                'by_source': [{
                                    'meta': {
                                        'source': MARKET_IDX,
                                    },
                                    'verdict': [{
                                        'results': [{
                                            'is_banned': True,
                                            'messages': [{
                                                'code': '49l'
                                            }]
                                        }]
                                    }]
                                }]
                            }))),
                        }
                    })
                }]
            }]
        }]))


def test_shop_without_license(miner, input_topic, output_topic):
    jewelry_tovar_ids = [1, 2, 101, 102]

    request = message_from_data({
        'united_offers': [{
            'offer': [{
                'basic': {
                    'identifiers': {
                        'business_id': DSBS_BUSINESS,
                        'offer_id': 'subcategories_' + str(shop_sku),
                    },
                    'content': {
                        'market': {
                            'enriched_offer': {
                                'category_id': category_id,
                                'model_id': shop_sku,
                                'matched_id': shop_sku,
                            },
                            'ir_data': {
                                'matched_id': shop_sku,
                            },
                        },
                    },
                },
                'service': {
                    SHOP_WITHOUT_JEWELRY_LICENSE: {
                        'identifiers': {
                            'business_id': DSBS_BUSINESS,
                            'offer_id': 'subcategories_' + str(shop_sku),
                            'shop_id': SHOP_WITHOUT_JEWELRY_LICENSE,
                        },
                        'status': {
                            'disabled': [{
                                'flag': False,
                                'meta': {
                                    'source': MARKET_IDX
                                }
                            }],
                            'united_catalog': {
                                'flag': True,
                            },
                        },
                        'meta': {
                            'rgb': BLUE,
                        },
                    }
                }
            }]
        } for category_id, shop_sku in zip([91273, 91274, 6206932, 6206933], jewelry_tovar_ids)]
    }, DatacampMessage())

    input_topic.write(request.SerializeToString())
    data = output_topic.read(count=1)

    assert_that(data, HasSerializedDatacampMessages([{
        'united_offers': [{
            'offer': [{
                'basic': {
                    'identifiers': {
                        'business_id': DSBS_BUSINESS,
                        'offer_id': 'subcategories_' + str(102),
                    },
                },
                'service': IsProtobufMap({
                    SHOP_WITHOUT_JEWELRY_LICENSE: {
                        'identifiers': {
                            'business_id': DSBS_BUSINESS,
                            'offer_id': 'subcategories_' + str(102),
                            'shop_id': SHOP_WITHOUT_JEWELRY_LICENSE,
                        },
                        'meta': {
                            'rgb': BLUE,
                        },
                        'resolution': is_not(has_item(IsProtobuf({
                            'by_source': [{
                                'meta': {
                                    'source': MARKET_IDX,
                                },
                                'verdict': [{
                                    'results': [{
                                        'is_banned': True,
                                        'messages': [{
                                            'code': '49l'
                                        }]
                                    }]
                                }]
                            }]
                        }))),
                    }
                })
            }]
        }]
    }]))

    for shop_sku in [1, 2, 101]:
        assert_that(data, HasSerializedDatacampMessages([{
            'united_offers': [{
                'offer': [{
                    'basic': {
                        'identifiers': {
                            'business_id': DSBS_BUSINESS,
                            'offer_id': 'subcategories_' + str(shop_sku),
                        },
                    },
                    'service': IsProtobufMap({
                        SHOP_WITHOUT_JEWELRY_LICENSE: {
                            'identifiers': {
                                'business_id': DSBS_BUSINESS,
                                'offer_id': 'subcategories_' + str(shop_sku),
                                'shop_id': SHOP_WITHOUT_JEWELRY_LICENSE,
                            },
                            'meta': {
                                'rgb': BLUE,
                            },
                            'resolution': {
                                'by_source': [{
                                    'meta': {
                                        'source': MARKET_IDX,
                                    },
                                    'verdict': [{
                                        'results': [{
                                            'is_banned': True,
                                            'messages': [{
                                                'code': '49l'
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


def test_shop_not_found_in_shops_dat(miner, input_topic, output_topic):
    jewelry_tovar_ids = [1, 2, 101, 102]

    request = message_from_data({
        'united_offers': [{
            'offer': [{
                'basic': {
                    'identifiers': {
                        'business_id': DSBS_BUSINESS,
                        'offer_id': 'jewelry_without_shops_dat_' + str(shop_sku),
                    },
                    'content': {
                        'market': {
                            'enriched_offer': {
                                'category_id': category_id,
                                'model_id': shop_sku,
                                'matched_id': shop_sku,
                            },
                            'ir_data': {
                                'matched_id': shop_sku,
                            },
                        },
                    },
                },
                'service': {
                    SHOP_NOT_IN_SHOPSDAT: {
                        'identifiers': {
                            'business_id': DSBS_BUSINESS,
                            'offer_id': 'jewelry_without_shops_dat_' + str(shop_sku),
                            'shop_id': SHOP_NOT_IN_SHOPSDAT,
                        },
                        'status': {
                            'disabled': [{
                                'flag': False,
                                'meta': {
                                    'source': MARKET_IDX
                                }
                            }],
                            'united_catalog': {
                                'flag': True,
                            },
                        },
                        'meta': {
                            'rgb': BLUE,
                        },
                    }
                }
            }]
        } for category_id, shop_sku in zip([91273, 91274, 6206932, 6206933], jewelry_tovar_ids)]
    }, DatacampMessage())

    input_topic.write(request.SerializeToString())
    data = output_topic.read(count=1)

    assert_that(data, HasSerializedDatacampMessages([{
        'united_offers': [{
            'offer': [{
                'basic': {
                    'identifiers': {
                        'business_id': DSBS_BUSINESS,
                        'offer_id': 'jewelry_without_shops_dat_' + str(102),
                    },
                },
                'service': IsProtobufMap({
                    SHOP_NOT_IN_SHOPSDAT: {
                        'identifiers': {
                            'business_id': DSBS_BUSINESS,
                            'offer_id': 'jewelry_without_shops_dat_' + str(102),
                            'shop_id': SHOP_NOT_IN_SHOPSDAT,
                        },
                        'meta': {
                            'rgb': BLUE,
                        },
                        'resolution': is_not(has_item(IsProtobuf({
                            'by_source': [{
                                'meta': {
                                    'source': MARKET_IDX,
                                },
                                'verdict': [{
                                    'results': [{
                                        'is_banned': True,
                                        'messages': [{
                                            'code': '49l'
                                        }]
                                    }]
                                }]
                            }]
                        }))),
                    }
                })
            }]
        }]
    }]))

    for shop_sku in [1, 2, 101]:
        assert_that(data, HasSerializedDatacampMessages([{
            'united_offers': [{
                'offer': [{
                    'basic': {
                        'identifiers': {
                            'business_id': DSBS_BUSINESS,
                            'offer_id': 'jewelry_without_shops_dat_' + str(shop_sku),
                        },
                    },
                    'service': IsProtobufMap({
                        SHOP_NOT_IN_SHOPSDAT: {
                            'identifiers': {
                                'business_id': DSBS_BUSINESS,
                                'offer_id': 'jewelry_without_shops_dat_' + str(shop_sku),
                                'shop_id': SHOP_NOT_IN_SHOPSDAT,
                            },
                            'meta': {
                                'rgb': BLUE,
                            },
                            'resolution': {
                                'by_source': [{
                                    'meta': {
                                        'source': MARKET_IDX,
                                    },
                                    'verdict': [{
                                        'results': [{
                                            'is_banned': True,
                                            'messages': [{
                                                'code': '49l'
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


def test_dbs_offer_without_mapping(miner, input_topic, output_topic):
    request = message_from_data({
        'united_offers': [{
            'offer': [{
                'basic': {
                    'identifiers': {
                        'business_id': DSBS_BUSINESS,
                        'offer_id': 'dbs_offer_without_mapping_banned_category',
                    },
                    'content': {
                        'market': {
                            'enriched_offer': {
                                'category_id': DBS_CATEGORY_ID,
                            },
                        },
                    },
                },
                'service': {
                    SHOP_WITH_ALL_LICENSES: {
                        'identifiers': {
                            'business_id': DSBS_BUSINESS,
                            'offer_id': 'dbs_offer_without_mapping_banned_category',
                            'shop_id': SHOP_WITH_ALL_LICENSES,
                        },
                        'meta': {
                            'rgb': WHITE,
                        },
                        'partner_info': {
                            'is_dsbs': True,
                        },
                        'status': {
                            'disabled': [{
                                'flag': False,
                                'meta': {
                                    'source': MARKET_IDX
                                }
                            }],
                            'united_catalog': {
                                'flag': True,
                            },
                        },
                    }
                }
            }]
        },
        {
            'offer': [{
                'basic': {
                    'identifiers': {
                        'business_id': DSBS_BUSINESS,
                        'offer_id': 'dbs_offer_with_mapping_banned_category',
                    },
                    'content': {
                        'binding': {
                            'approved': {
                                'market_sku_id': DBS_MARKET_SKU_ID,
                            },
                        },
                    },
                },
                'service': {
                    SHOP_WITH_ALL_LICENSES: {
                        'identifiers': {
                            'business_id': DSBS_BUSINESS,
                            'offer_id': 'dbs_offer_with_mapping_banned_category',
                            'shop_id': SHOP_WITH_ALL_LICENSES,
                        },
                        'meta': {
                            'rgb': WHITE,
                        },
                        'partner_info': {
                            'is_dsbs': True,
                        },
                        'status': {
                            'disabled': [{
                                'flag': False,
                                'meta': {
                                    'source': MARKET_IDX
                                }
                            }],
                            'united_catalog': {
                                'flag': True,
                            },
                        },
                    }
                }
            }]
        },
        {
            'offer': [{
                'basic': {
                    'identifiers': {
                        'business_id': DSBS_BUSINESS,
                        'offer_id': 'dbs_offer_without_mapping_not_banned_category',
                    },
                    'content': {
                        'market': {
                            'enriched_offer': {
                                'category_id': NOT_DBS_CATEGORY_ID,
                            },
                        },
                    },
                },
                'service': {
                    SHOP_WITH_ALL_LICENSES: {
                        'identifiers': {
                            'business_id': DSBS_BUSINESS,
                            'offer_id': 'dbs_offer_without_mapping_not_banned_category',
                            'shop_id': SHOP_WITH_ALL_LICENSES,
                        },
                        'meta': {
                            'rgb': WHITE,
                        },
                        'partner_info': {
                            'is_dsbs': True,
                        },
                        'status': {
                            'disabled': [{
                                'flag': False,
                                'meta': {
                                    'source': MARKET_IDX
                                }
                            }],
                            'united_catalog': {
                                'flag': True,
                            },
                        },
                    }
                }
            }]
        },
        ]
    }, DatacampMessage())

    input_topic.write(request.SerializeToString())
    data = output_topic.read(count=1)

    assert_that(data, HasSerializedDatacampMessages([{
        'united_offers': [{
            'offer': [{
                'basic': {
                    'identifiers': {
                        'business_id': DSBS_BUSINESS,
                        'offer_id': 'dbs_offer_without_mapping_banned_category',
                    },
                },
                'service': IsProtobufMap({
                    SHOP_WITH_ALL_LICENSES: {
                        'identifiers': {
                            'business_id': DSBS_BUSINESS,
                            'offer_id': 'dbs_offer_without_mapping_banned_category',
                            'shop_id': SHOP_WITH_ALL_LICENSES,
                        },
                        'meta': {
                            'rgb': WHITE,
                        },
                        'resolution': {
                            'by_source': [{
                                'meta': {
                                    'source': MARKET_IDX,
                                },
                                'verdict': [{
                                    'results': [{
                                        'is_banned': True,
                                        'messages': [{
                                            'code': '49r'
                                        }]
                                    }]
                                }]
                            }]
                        },
                        'status': {
                            'disabled': [{
                                'flag': True,
                                'meta': {
                                    'source': MARKET_IDX
                                }
                            }],
                            'united_catalog': {
                                'flag': True,
                            },
                        },
                    }
                })
            }]
        }]
    }]))
    assert_that(data, HasSerializedDatacampMessages([{
        'united_offers': [{
            'offer': [{
                'basic': {
                    'identifiers': {
                        'business_id': DSBS_BUSINESS,
                        'offer_id': 'dbs_offer_with_mapping_banned_category',
                    },
                },
                'service': IsProtobufMap({
                    SHOP_WITH_ALL_LICENSES: {
                        'identifiers': {
                            'business_id': DSBS_BUSINESS,
                            'offer_id': 'dbs_offer_with_mapping_banned_category',
                            'shop_id': SHOP_WITH_ALL_LICENSES,
                        },
                        'meta': {
                            'rgb': WHITE,
                        },
                        'resolution': is_not(has_item(IsProtobuf({
                            'by_source': [{
                                'meta': {
                                    'source': MARKET_IDX,
                                },
                                'verdict': [{
                                    'results': [{
                                        'is_banned': True,
                                        'messages': [{
                                            'code': '49r'
                                        }]
                                    }]
                                }]
                            }]
                        }))),
                    }
                })
            }]
        }]
    }]))
    assert_that(data, HasSerializedDatacampMessages([{
        'united_offers': [{
            'offer': [{
                'basic': {
                    'identifiers': {
                        'business_id': DSBS_BUSINESS,
                        'offer_id': 'dbs_offer_without_mapping_not_banned_category',
                    },
                },
                'service': IsProtobufMap({
                    SHOP_WITH_ALL_LICENSES: {
                        'identifiers': {
                            'business_id': DSBS_BUSINESS,
                            'offer_id': 'dbs_offer_without_mapping_not_banned_category',
                            'shop_id': SHOP_WITH_ALL_LICENSES,
                        },
                        'meta': {
                            'rgb': WHITE,
                        },
                        'resolution': is_not(has_item(IsProtobuf({
                            'by_source': [{
                                'meta': {
                                    'source': MARKET_IDX,
                                },
                                'verdict': [{
                                    'results': [{
                                        'is_banned': True,
                                        'messages': [{
                                            'code': '49r'
                                        }]
                                    }]
                                }]
                            }]
                        }))),
                    }
                })
            }]
        }]
    }]))
