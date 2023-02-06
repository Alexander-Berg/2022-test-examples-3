# coding=utf-8

"""
Актуально для Товарной Вертикали
Тест проверяет подклеивание ску из внешней таблицы c исправлениями sku, предоставляемой ТВ
Ключ для сопоставления - пара (feed_id, offer_id)
При наличии в этой таблице sku из нее применяется независимо от наличия других источников
"""

import pytest
import time
from datetime import datetime
from hamcrest import (
    assert_that,
    has_entries,
    equal_to,
    all_of,
    is_not,
    has_key,
)

from market.idx.offers.yatf.test_envs.main_idx import Or3MainIdxTestEnv
from market.idx.offers.yatf.resources.idx_prepare_offers.config import Or3Config
from market.idx.offers.yatf.resources.idx_prepare_offers.data_raw_tables import (
    OffersRawTable,
    OffersToSkuExternalTable,
    OffersToFixedSkuTable
)
from market.idx.generation.yatf.resources.prepare.in_picrobot_success import PicrobotSuccessTable
from market.idx.generation.yatf.resources.prepare.offer2pic import Offer2PicTable
from market.idx.generation.yatf.utils.fixtures import make_offer_proto_str

from market.idx.yatf.resources.shops_dat import ShopsDat
from market.idx.yatf.resources.tovar_tree_pb import (
    MboCategory,
    TovarTreePb,
)
from yt.wrapper import ypath_join


GENERATION = datetime.now().strftime('%Y%m%d_%H%M')
MI3_TYPE = 'main'
SESSION_ID = int(time.time())

DEFAULT_GENLOG_CHECKER = has_entries({
    "model_id": 10,
    "category_id": 20,
    "vendor_id": '30',
})
EXTERNAL_MATCHING_GENLOG = {
    "model_id": 1,
    "category_id": 2,
    "vendor_id": 3,
}
EXTERNAL_MATCHING_GENLOG_CHECKER = has_entries({
    "model_id": 1,
    "category_id": 2,
    "vendor_id": '3',
})
FIXED_MATCHING_GENLOG = {
    "model_id": 100,
    "category_id": 200,
    "vendor_id": 300,
}
FIXED_MATCHING_GENLOG_CHECKER = has_entries({
    "model_id": 100,
    "category_id": 200,
    "vendor_id": '300',
})


def default_genlog(market_sku=None):
    res = {
        "model_id": 10,
        "category_id": 20,
        "vendor_id": 30,
        "ware_md5": 'hc1cVZiClnllcxjhGX0_cQ',
    }
    if market_sku is not None:
        res['market_sku'] = market_sku
    return res


@pytest.fixture(scope='module')
def or3_config_data(yt_server):
    home_dir = yt_server.get_yt_client().config['prefix']
    config = {
        'yt': {
            'home_dir': home_dir
        },
        'external_sku': {
            'offers2sku_external_table_path': ypath_join(home_dir, 'sku_external', 'recent'),
            'use_offers2sku_external_table': True,
            'offers2fixed_sku_table_path': ypath_join(home_dir, 'sku_fixed', 'recent'),
            'use_offers2fixed_sku_table': True,
            'sku_source_field_enable': True
        }
    }
    return config


@pytest.fixture(scope='module')
def or3_config(or3_config_data):
    return Or3Config(**or3_config_data)


@pytest.yield_fixture(scope="module")
def source_offers_raw():
    return [
        {
            'feed_id': 1000,
            'offer_id': 'no_uc_no_sm_no_fix',
            'offer': make_offer_proto_str(
                price=20001,
                genlog=default_genlog()
            ),
        },
        {
            'feed_id': 1000,
            'offer_id': 'no_uc_no_sm_is_fix',
            'offer': make_offer_proto_str(
                price=20001,
                genlog=default_genlog()
            ),
        },
        {
            'feed_id': 1000,
            'offer_id': 'no_uc_is_sm_no_fix',
            'offer': make_offer_proto_str(
                price=20001,
                genlog=default_genlog()
            ),
        },
        {
            'feed_id': 1000,
            'offer_id': 'no_uc_is_sm_is_fix',
            'offer': make_offer_proto_str(
                price=20001,
                genlog=default_genlog()
            ),
        },
        {
            'feed_id': 1000,
            'offer_id': 'is_uc_no_sm_no_fix',
            'offer': make_offer_proto_str(
                price=20001,
                genlog=default_genlog(market_sku=111)
            ),
        },
        {
            'feed_id': 1000,
            'offer_id': 'is_uc_no_sm_is_fix',
            'offer': make_offer_proto_str(
                price=20001,
                genlog=default_genlog(market_sku=222)
            ),
        },
        {
            'feed_id': 1000,
            'offer_id': 'is_uc_is_sm_no_fix',
            'offer': make_offer_proto_str(
                price=20001,
                genlog=default_genlog(market_sku=333)
            ),
        },
        {
            'feed_id': 1000,
            'offer_id': 'is_uc_is_sm_is_fix',
            'offer': make_offer_proto_str(
                price=20001,
                genlog=default_genlog(market_sku=444)
            ),
        },

    ]


@pytest.fixture(scope="module")
def offer2sku():
    return [
        {
            "feed_id": 1000,
            "offer_id": 'no_uc_is_sm_no_fix',
            "business_id": 0,
            "confidence": 1.,
            "is_match": True,
            "shop_id": 0,
            "sku_id": 10001,
            "sku_parent_model_id": 1,
            "category_id": 2,
            "vendor_id": 3,
            "target_type": "PSKU",
            "ts": 0
        },
        {
            "feed_id": 1000,
            "offer_id": 'no_uc_is_sm_is_fix',
            "business_id": 0,
            "confidence": 1.,
            "is_match": True,
            "shop_id": 0,
            "sku_id": 10002,
            "sku_parent_model_id": 1,
            "category_id": 2,
            "vendor_id": 3,
            "target_type": "PSKU",
            "ts": 0
        },
        {
            "feed_id": 1000,
            "offer_id": 'is_uc_is_sm_no_fix',
            "business_id": 0,
            "confidence": 1.,
            "is_match": True,
            "shop_id": 0,
            "sku_id": 10003,
            "sku_parent_model_id": 1,
            "category_id": 2,
            "vendor_id": 3,
            "target_type": "PSKU",
            "ts": 0
        },
        {
            "feed_id": 1000,
            "offer_id": 'is_uc_is_sm_is_fix',
            "business_id": 0,
            "confidence": 1.,
            "is_match": True,
            "shop_id": 0,
            "sku_id": 10004,
            "sku_parent_model_id": 1,
            "category_id": 2,
            "vendor_id": 3,
            "target_type": "PSKU",
            "ts": 0
        },
    ]


@pytest.fixture(scope="module")
def offer2fixedsku():
    return [
        {
            "feed_id": 1000,
            "offer_id": 'no_uc_no_sm_is_fix',
            "business_id": 0,
            "shop_id": 0,
            "fixed_sku_id": 20001,
            "sku_parent_model_id": 100,
            "category_id": 200,
            "vendor_id": 300,
            "comment": "comment",
        },
        {
            "feed_id": 1000,
            "offer_id": 'no_uc_is_sm_is_fix',
            "business_id": 0,
            "shop_id": 0,
            "fixed_sku_id": 20002,
            "sku_parent_model_id": 100,
            "category_id": 200,
            "vendor_id": 300,
            "comment": "comment",
        },
        {
            "feed_id": 1000,
            "offer_id": 'is_uc_no_sm_is_fix',
            "business_id": 0,
            "shop_id": 0,
            "fixed_sku_id": 20003,
            "sku_parent_model_id": 100,
            "category_id": 200,
            "vendor_id": 300,
            "comment": "comment",
        },
        {
            "feed_id": 1000,
            "offer_id": 'is_uc_is_sm_is_fix',
            "business_id": 0,
            "shop_id": 0,
            "fixed_sku_id": 20004,
            "sku_parent_model_id": 100,
            "category_id": 200,
            "vendor_id": 300,
            "comment": "comment",
        },
    ]


@pytest.fixture(scope='module')
def source_yt_tables(yt_server, or3_config, source_offers_raw, offer2sku, offer2fixedsku):
    yt_home_path = or3_config.options['yt']['home_dir']
    return {
        'offers_raw': OffersRawTable(
            yt_stuff=yt_server,
            path=ypath_join(yt_home_path, 'mi3', MI3_TYPE, GENERATION, 'work', 'offers_raw'),
            data=source_offers_raw
        ),
        'offer2pic_unsorted': Offer2PicTable(
            yt_stuff=yt_server,
            path=ypath_join(yt_home_path, 'mi3', MI3_TYPE, GENERATION, 'work', 'offer2pic_unsorted'),
            data=[]
        ),
        'in_picrobot_success': PicrobotSuccessTable(
            yt_stuff=yt_server,
            path=ypath_join(yt_home_path, 'in', 'picrobot', 'success', 'recent'),
            data=[]
        ),
        'offers2sku_external_table_path': OffersToSkuExternalTable(
            yt_stuff=yt_server,
            path=ypath_join(yt_home_path, 'sku_external', 'recent'),
            data=offer2sku
        ),
        'offers2fixed_sku_table_path': OffersToFixedSkuTable(
            yt_stuff=yt_server,
            path=ypath_join(yt_home_path, 'sku_fixed', 'recent'),
            data=offer2fixedsku
        ),
    }


@pytest.yield_fixture(scope='module')
def tovar_tree():
    return [
        MboCategory(
            hid=1,
            tovar_id=0,
            unique_name='Все товары',
            name='Все товары',
            output_type=MboCategory.GURULIGHT,
        ),
    ]


@pytest.fixture(scope="module")
def shops_dat():
    return ShopsDat([])


@pytest.yield_fixture(scope="module")
def main_idx(yt_server, or3_config, source_yt_tables, shops_dat, tovar_tree):
    for table in source_yt_tables.values():
        table.create()
        path = table.get_path()
        assert_that(yt_server.get_yt_client().exists(path), "Table {} doesn\'t exist".format(path))

    resources = {
        'config': or3_config,
        'shops_dat': shops_dat,
        'tovar_tree_pb': TovarTreePb(tovar_tree),
    }

    with Or3MainIdxTestEnv(yt_server, GENERATION, MI3_TYPE, 1, False, **resources) as mi:
        mi.verify()
        mi.execute()
        yield mi


def test_quantity(main_idx):
    offers = main_idx.outputs['offers_by_offer_id']
    assert_that(len(offers), equal_to(8))


def test_no_uc_no_sm_no_fix(main_idx):
    offers = main_idx.outputs['offers_by_offer_id']
    assert_that(
        offers,
        has_entries({
            "no_uc_no_sm_no_fix": all_of(
                has_entries({
                    'offer_id': "no_uc_no_sm_no_fix",
                    'msku': 0,
                    'genlog': all_of(
                        has_entries({
                            'sku_source': 'DEFAULT'
                        }),
                        is_not(has_key('market_sku')),
                        DEFAULT_GENLOG_CHECKER
                    ),
                }),
            )
        })
    )


def test_no_uc_no_sm_is_fix(main_idx):
    offers = main_idx.outputs['offers_by_offer_id']
    assert_that(
        offers,
        has_entries({
            "no_uc_no_sm_is_fix": all_of(
                has_entries({
                    'offer_id': "no_uc_no_sm_is_fix",
                    'msku': 20001,
                    'genlog': all_of(
                        has_entries({
                            'market_sku': '20001',
                            'sku_source': 'FIXED',
                        }),
                        FIXED_MATCHING_GENLOG_CHECKER
                    ),
                }),
            )
        })
    )


def test_no_uc_is_sm_no_fix(main_idx):
    offers = main_idx.outputs['offers_by_offer_id']
    assert_that(
        offers,
        has_entries({
            "no_uc_is_sm_no_fix": all_of(
                has_entries({
                    'offer_id': "no_uc_is_sm_no_fix",
                    'msku': 10001,
                    'genlog': all_of(
                        has_entries({
                            'market_sku': '10001',
                            'sku_source': 'EXTERNAL',
                        }),
                        EXTERNAL_MATCHING_GENLOG_CHECKER
                    ),
                }),
            )
        })
    )


def test_no_uc_is_sm_is_fix(main_idx):
    offers = main_idx.outputs['offers_by_offer_id']
    assert_that(
        offers,
        has_entries({
            "no_uc_is_sm_is_fix": all_of(
                has_entries({
                    'offer_id': "no_uc_is_sm_is_fix",
                    'msku': 20002,
                    'genlog': all_of(
                        has_entries({
                            'market_sku': '20002',
                            'sku_source': 'FIXED',
                        }),
                        FIXED_MATCHING_GENLOG_CHECKER
                    ),
                }),
            )
        })
    )


def test_is_uc_no_sm_no_fix(main_idx):
    offers = main_idx.outputs['offers_by_offer_id']
    assert_that(
        offers,
        has_entries({
            "is_uc_no_sm_no_fix": all_of(
                has_entries({
                    'offer_id': "is_uc_no_sm_no_fix",
                    'msku': 111,
                    'genlog': all_of(
                        has_entries({
                            'market_sku': '111',
                            'sku_source': 'DEFAULT',
                        }),
                        DEFAULT_GENLOG_CHECKER
                    ),
                }),
            )
        })
    )


def test_is_uc_no_sm_is_fix(main_idx):
    offers = main_idx.outputs['offers_by_offer_id']
    assert_that(
        offers,
        has_entries({
            "is_uc_no_sm_is_fix": all_of(
                has_entries({
                    'offer_id': "is_uc_no_sm_is_fix",
                    'msku': 20003,
                    'genlog': all_of(
                        has_entries({
                            'market_sku': '20003',
                            'sku_source': 'FIXED',
                        }),
                        FIXED_MATCHING_GENLOG_CHECKER
                    ),
                }),
            )
        })
    )


def test_is_uc_is_sm_no_fix(main_idx):
    offers = main_idx.outputs['offers_by_offer_id']
    assert_that(
        offers,
        has_entries({
            "is_uc_is_sm_no_fix": all_of(
                has_entries({
                    'offer_id': "is_uc_is_sm_no_fix",
                    'msku': 333,
                    'genlog': all_of(
                        has_entries({
                            'market_sku': '333',
                            'sku_source': 'DEFAULT',
                        }),
                        DEFAULT_GENLOG_CHECKER
                    ),
                }),
            )
        })
    )


def test_is_uc_is_sm_is_fix(main_idx):
    offers = main_idx.outputs['offers_by_offer_id']
    assert_that(
        offers,
        has_entries({
            "is_uc_is_sm_is_fix": all_of(
                has_entries({
                    'offer_id': "is_uc_is_sm_is_fix",
                    'msku': 20004,
                    'genlog': all_of(
                        has_entries({
                            'market_sku': '20004',
                            'sku_source': 'FIXED',
                        }),
                        FIXED_MATCHING_GENLOG_CHECKER
                    ),
                }),
            ),
        })
    )
