# coding: utf-8

import pytest
import datetime

from hamcrest import (
    assert_that,
    has_entries,
    has_item,
    all_of,
)

from yt.wrapper import ypath_join

from market.idx.yatf.resources.shops_dat import ShopsDat
from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix
from market.idx.offers.yatf.test_envs.main_idx import Or3MainIdxTestEnv
from market.idx.offers.yatf.resources.idx_prepare_offers.config import Or3Config
from market.idx.offers.yatf.resources.idx_prepare_offers.data_raw_tables import OffersRawTable, IrTsarOfferIndexTable
from market.idx.generation.yatf.resources.prepare.in_picrobot_success import PicrobotSuccessTable
from market.idx.generation.yatf.utils.fixtures import make_offer_proto_str, make_uc_proto_str

from market.idx.yatf.resources.tovar_tree_pb import MboCategory, TovarTreePb

GENERATION = datetime.datetime.now().strftime('%Y%m%d_%H%M')
MI3_TYPE = 'main'
COUNT_SHARDS = 1


@pytest.yield_fixture(scope="module")
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
            'yt_ir_by_offer_table': ypath_join(home_dir, 'mi3', MI3_TYPE, GENERATION, 'work', 'input_ir_tsar_offer_index')
        },
    }


@pytest.yield_fixture(scope="module")
def or3_config(or3_config_data):
    return Or3Config(**or3_config_data)


@pytest.fixture(scope="module")
def shops_dat():
    return ShopsDat([
        {"datafeed_id": 1000, 'business_id': 1, 'real_feed_id': 1000},
        {"datafeed_id": 2000, 'business_id': 2, 'real_feed_id': 2000},
        {"datafeed_id": 3000, 'business_id': 3, 'real_feed_id': 3000},
        {"datafeed_id": 4000, 'business_id': 4, 'real_feed_id': 4000},
        ])


@pytest.fixture(scope="module")
def ir_offer_index():
    return [
        {'business_id': 1, 'offer_id': 'offer_1', 'is_not_tsar': True},
        {'business_id': 2, 'offer_id': 'offer_2', 'is_not_tsar': True},
        {'business_id': 3, 'offer_id': 'offer_3', 'is_not_tsar': False},
        {'business_id': 5, 'offer_id': 'offer_3', 'is_not_tsar': False},
    ]


@pytest.yield_fixture(scope="module")
def source_offers_raw():
    return [
        {
            'feed_id': 1000,
            'offer_id': 'offer_1',
            'session_id': 30,
            'offer': make_offer_proto_str(price=20001, ware_md5='wxdbP0Y7RDCTk1EnsixTfA'),
            'uc': make_uc_proto_str(model_id=125),
        },
        {
            'feed_id': 2000,
            'offer_id': 'offer_2',
            'session_id': 30,
            'offer': make_offer_proto_str(price=20002, ware_md5='wxdbP0Y7RDCTk1EnsixTf2'),
            'uc': make_uc_proto_str(model_id=125),
        },
        {
            'feed_id': 3000,
            'offer_id': 'offer_3',
            'session_id': 30,
            'offer': make_offer_proto_str(price=20003, ware_md5='wxdbP0Y7RDCTk1EnsixTf3'),
            'uc': make_uc_proto_str(model_id=125),
        },
    ]


@pytest.yield_fixture(scope="module")
def source_yt_tables(yt_server, or3_config, source_offers_raw, ir_offer_index):
    yt_home_path = or3_config.options['yt']['home_dir']
    return {
        'offers_raw': OffersRawTable(
            yt_stuff=yt_server,
            path=ypath_join(yt_home_path, 'mi3', MI3_TYPE, GENERATION, 'work', 'offers_raw'),
            data=source_offers_raw,
        ),
        'yt_ir_by_offer_table': IrTsarOfferIndexTable(
            yt_stuff=yt_server,
            path=ypath_join(yt_home_path, 'mi3', MI3_TYPE, GENERATION, 'work', 'input_ir_tsar_offer_index'),
            data=ir_offer_index,
        )
    }


@pytest.yield_fixture(scope="module")
def main_idx(yt_server, or3_config, source_yt_tables, shops_dat, tovar_tree):
    for table in source_yt_tables.values():
        table.create()
        path = table.get_path()
        assert_that(yt_server.get_yt_client().exists(path), "Table {} doesn\'t exist".format(path))

    yt_home_path = or3_config.options['yt']['home_dir']
    resources = {
        'config': or3_config,
        'in_picrobot_success': PicrobotSuccessTable(
            yt_stuff=yt_server, path=ypath_join(yt_home_path, 'in', 'picrobot', 'success', 'recent'), data=[]
        ),
        'shops_dat': shops_dat,
        'tovar_tree_pb': TovarTreePb(tovar_tree),
    }

    with Or3MainIdxTestEnv(
        yt_server, GENERATION, MI3_TYPE, COUNT_SHARDS, False, one_table_mode=True, **resources
    ) as mi:
        mi.verify()
        mi.execute()
        yield mi


def test_ir_tsar_enriching(main_idx):
    assert_that(
        main_idx.outputs['offers'],
        all_of(
            has_item(
                has_entries({
                    'offer': has_entries({
                        'ware_md5': 'wxdbP0Y7RDCTk1EnsixTfA',
                        'is_not_tsar': True,
                    }),
                }),
            ),
            has_item(
                has_entries({
                    'offer': has_entries({
                        'ware_md5': 'wxdbP0Y7RDCTk1EnsixTf2',
                        'is_not_tsar': True,
                    }),
                }),
            ),
            has_item(
                has_entries({
                    'offer': has_entries({
                        'ware_md5': 'wxdbP0Y7RDCTk1EnsixTf3',
                    }),
                }),
            ),
        )
    )
