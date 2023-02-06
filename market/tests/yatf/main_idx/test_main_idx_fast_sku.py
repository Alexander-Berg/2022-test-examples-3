# coding: utf-8

import pytest
import datetime

from hamcrest import (
    all_of,
    assert_that,
    has_entries,
    has_property,
    is_not,
)
from yt.wrapper import ypath_join

from market.idx.datacamp.proto.offer.OfferMapping_pb2 import Mapping as MappingPb
from market.idx.offers.yatf.test_envs.main_idx import Or3MainIdxTestEnv
from market.idx.offers.yatf.resources.idx_prepare_offers.config import Or3Config
from market.idx.offers.yatf.resources.idx_prepare_offers.data_raw_tables import (
    BlueOffersRawTable,
    Offers2ModelTable,
    Offers2ParamTable,
    OffersRawTable,
)
from market.idx.generation.yatf.resources.prepare.in_picrobot_success import PicrobotSuccessTable
from market.idx.generation.yatf.resources.prepare.offer2pic import Offer2PicTable
from market.idx.generation.yatf.utils.common import create_ware_md5
from market.idx.generation.yatf.utils.fixtures import (
    make_offer_proto_str,
    make_uc_proto_str,
)

from market.idx.yatf.matchers.user_friendly_dict_matcher import user_friendly_dict_equal_to
from market.idx.yatf.resources.msku_table import MskuContexTable
from market.idx.yatf.resources.tovar_tree_pb import (
    MboCategory,
    TovarTreePb,
)
from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix


GENERATION = datetime.datetime.now().strftime('%Y%m%d_%H%M')
MI3_TYPE = 'main'
COUNT_SHARDS = 1
HALF_MODE = False
PROCESSED_TIME = 1000*5000
PARAM_1 = 555
FAKE_VENDOR_ID = 16644882
VENDOR_FILTER_ID = 7893318
MARKET_SKU_TYPE_FAST = MappingPb.MarketSkuType.MARKET_SKU_TYPE_FAST


def make_default_uc_proto_str(
    model_id=125,
    category_id=None,
    processed_time=None,
    vendor_id=None,
    params=None,
):
    if params is None:
        params = []

    data = {
        'model_id': model_id,
        'matched_id': model_id,
        'category_id': category_id,
        'processed_time': processed_time,
        'vendor_id': vendor_id,
        'params': [dict(param_id=p) for p in params],
    }
    return make_uc_proto_str(**data)


@pytest.fixture(scope="module")
def tovar_tree():
    return [
        MboCategory(
            hid=1,
            tovar_id=0,
            unique_name="Все товары",
            name="Все товары",
            output_type=MboCategory.GURULIGHT,
        ),
    ]


@pytest.fixture(scope="module")
def or3_config_data(yt_server):
    home_dir = get_yt_prefix()
    return {
        'yt': {
            'home_dir': home_dir,
        },
        'misc': {
            'blue_offers_enabled': 'true',
            'enrich_blue_offers_from_fast_sku': 'true',
            'patch_uc_part_for_fast_sku_offer': 'true',
        },
    }


@pytest.fixture(scope="module")
def or3_config(or3_config_data):
    return Or3Config(**or3_config_data)


@pytest.fixture(scope="module")
def source_offers_raw():
    return [
        {
            'feed_id': 2000,
            'offer_id': 'white_offer',
            'session_id': 30,
            'offer': make_offer_proto_str(
                price=20003,
                ware_md5=create_ware_md5(0.5),
            ),
            'uc': make_default_uc_proto_str(model_id=135, category_id=321, processed_time=PROCESSED_TIME),
        },
    ]


@pytest.fixture(scope="module")
def source_blue_offers_raw():
    return [
        {
            'msku': 111,
            'feed_id': 3000,
            'offer_id': 'blue_offer',
            'session_id': 30,
            'offer': make_offer_proto_str(
                price=30004,
                is_blue_offer=True,
                ware_md5=create_ware_md5(0.1)
            ),
            'uc': make_default_uc_proto_str(model_id=136, category_id=322, processed_time=PROCESSED_TIME),
        },
        {
            'msku': 500,
            'feed_id': 3000,
            'offer_id': 'blue_offer_with_fast_sku',
            'session_id': 30,
            'offer': make_offer_proto_str(
                price=30004,
                is_blue_offer=True,
                market_sku_type=MARKET_SKU_TYPE_FAST,
                ware_md5=create_ware_md5(0.2),
            ),
            'uc': make_default_uc_proto_str(model_id=137, category_id=323, vendor_id=567, processed_time=PROCESSED_TIME, params=[PARAM_1]),
        },
        {
            'msku': 600,
            'feed_id': 3000,
            'offer_id': 'blue_offer_with_fast_sku_without_vendor',
            'session_id': 30,
            'offer': make_offer_proto_str(
                price=30004,
                is_blue_offer=True,
                market_sku_type=MARKET_SKU_TYPE_FAST,
                ware_md5=create_ware_md5(0.3),
            ),
            'uc': make_default_uc_proto_str(model_id=138, category_id=324, vendor_id=0, processed_time=PROCESSED_TIME, params=[PARAM_1]),
        },
    ]


@pytest.fixture(scope="module")
def source_msku_contex():
    return [
        {
            'msku': 111,
            'msku_exp': 0,
            'msku_experiment_id': '',
            'experimental_model_id': 0,
            'feed_id': 99999,
            'offer_id': 'MS111',
            'offer': make_offer_proto_str(
                is_fake_msku_offer=True,
                ware_md5=create_ware_md5(0.1)
            ),
            'uc': make_default_uc_proto_str(model_id=125),
        },
    ]


@pytest.fixture(scope="module")
def source_yt_tables(
        yt_server,
        or3_config,
        source_offers_raw,
        source_blue_offers_raw,
        source_msku_contex
):

    yt_home_path = or3_config.options['yt']['home_dir']
    tables = {
        'offers_raw': OffersRawTable(
            yt_stuff=yt_server,
            path=ypath_join(yt_home_path, 'mi3', MI3_TYPE, GENERATION, 'work', 'offers_raw'),
            data=source_offers_raw
        ),
        'offer2pic_unsorted': Offer2PicTable(
            yt_stuff=yt_server,
            path=ypath_join(yt_home_path, 'mi3', MI3_TYPE, GENERATION, 'work', 'offer2pic_unsorted'),
            data={}
        ),
        'offer2model_unsorted': Offers2ModelTable(
            yt_stuff=yt_server,
            path=ypath_join(yt_home_path, 'mi3', MI3_TYPE, GENERATION, 'work', 'offer2model_unsorted'),
            data={}
        ),
        'offer2param_unsorted': Offers2ParamTable(
            yt_stuff=yt_server,
            path=ypath_join(yt_home_path, 'mi3', MI3_TYPE, GENERATION, 'work', 'offer2param_unsorted'),
            data={}
        ),
        'blue_offers_raw': BlueOffersRawTable(
            yt_stuff=yt_server,
            path=ypath_join(yt_home_path, 'mi3', MI3_TYPE, GENERATION, 'work', 'blue_offers_raw'),
            data=source_blue_offers_raw
        ),
        'msku': MskuContexTable(
            yt_stuff=yt_server,
            path=ypath_join(yt_home_path, 'mi3', MI3_TYPE, GENERATION, 'input', 'msku_contex'),
            data=source_msku_contex,
        ),
    }

    for table in tables.values():
        table.create()
        path = table.get_path()
        assert_that(yt_server.get_yt_client().exists(path), "Table {} doesn\'t exist".format(path))

    return tables


@pytest.yield_fixture(scope="module")
def main_idx(yt_server, or3_config, source_yt_tables, tovar_tree):
    yt_home_path = or3_config.options['yt']['home_dir']
    resources = {
        'config': or3_config,
        'in_picrobot_success': PicrobotSuccessTable(
            yt_stuff=yt_server,
            path=ypath_join(yt_home_path, 'in', 'picrobot', 'success', 'recent'),
            data=[]
        ),
        'offer2pic': Offer2PicTable(
            yt_stuff=yt_server,
            path=ypath_join(yt_home_path, 'mi3', MI3_TYPE, GENERATION, 'work', 'offer2pic'),
            data=[]
        ),
        'tovar_tree_pb': TovarTreePb(tovar_tree),
    }

    with Or3MainIdxTestEnv(yt_server, GENERATION, MI3_TYPE, COUNT_SHARDS, HALF_MODE, **resources) as mi:
        mi.verify()
        mi.execute()
        yield mi


def test_offres_uc_cache(main_idx):
    """ Проверяет, что белые оффера получают обновление из uc
    """
    offers = main_idx.outputs['offers_by_offer_id']

    assert_that(
        offers['white_offer']['uc'],
        user_friendly_dict_equal_to({
            'model_id': 135,
            'matched_id': 135,
            'category_id': 321,
            'processed_time': str(PROCESSED_TIME)
        })
    )


def test_blue_offres_uc_cache(main_idx):
    """ Проверяет, что синие оффера получают обновление из uc
    """
    offers = main_idx.outputs['offers_by_offer_id']

    assert_that(
        offers['blue_offer']['uc'],
        user_friendly_dict_equal_to({
            'model_id': 136,
            'matched_id': 136,
            'category_id': 322,
            'processed_time': str(PROCESSED_TIME)
        })
    )


def test_blue_offres_with_fast_sku_uc_cache_patch(main_idx):
    """ Проверяет, что синие оффера с Быстрой карточкой получают пропатченное обновление из uc
    """
    offers = main_idx.outputs['offers_by_offer_id']

    assert_that(
        offers['blue_offer_with_fast_sku']['uc'],
        all_of(
            has_entries({
                'category_id': 323,
                'processed_time': str(PROCESSED_TIME),
                'vendor_id': 567,
                'params': [
                    dict(param_id=PARAM_1),
                ]
            }),
            is_not(has_property('model_id')),
            is_not(has_property('matched_id')),
        )
    )


def test_blue_offres_with_fast_sku_without_vendor_id_uc_cache_patch(main_idx):
    """ Проверяет, что синие оффера с Быстрой карточкой и без вендора получают пропатченное обновление из uc
    """
    offers = main_idx.outputs['offers_by_offer_id']

    assert_that(
        offers['blue_offer_with_fast_sku_without_vendor']['uc'],
        all_of(
            has_entries({
                'category_id': 324,
                'processed_time': str(PROCESSED_TIME),
                'vendor_id': FAKE_VENDOR_ID,
                'params': [
                    dict(param_id=PARAM_1),
                    dict(param_id=VENDOR_FILTER_ID, option_id=FAKE_VENDOR_ID, value_id=FAKE_VENDOR_ID),
                ]
            }),
            is_not(has_property('model_id')),
            is_not(has_property('matched_id')),
        )
    )
