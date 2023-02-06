# coding: utf-8

import datetime
import pytest
from hamcrest import (
    assert_that,
    equal_to,
)

from yt.wrapper import ypath_join

from market.idx.offers.yatf.test_envs.main_idx import Or3MainIdxTestEnv
from market.idx.offers.yatf.resources.idx_prepare_offers.config import Or3Config
from market.idx.offers.yatf.resources.idx_prepare_offers.data_raw_tables import (
    BlueOffersRawTable,
    IrByMskuTable,
    Offers2ModelTable,
    Offers2ParamTable,
    OffersRawTable,
)
from market.idx.generation.yatf.resources.prepare.in_picrobot_success import PicrobotSuccessTable
from market.idx.generation.yatf.resources.prepare.offer2pic import Offer2PicTable
from market.idx.generation.yatf.utils.fixtures import make_msku_contex_dict

from market.idx.yatf.resources.msku_table import MskuContexTable
from market.idx.yatf.resources.tovar_tree_pb import (
    MboCategory,
    TovarTreePb,
)
from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix
from market.idx.generation.yatf.utils.fixtures import (
    make_offer_proto_str,
    make_uc_proto_str,
)
from market.idx.generation.yatf.utils.common import create_ware_md5


GENERATION = datetime.datetime.now().strftime('%Y%m%d_%H%M')
MI3_TYPE = 'main'
COUNT_SHARDS = 2
BERU_SHOP = 431782
BERU_FEED = 475690
HALF_MODE = False


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


def make_raw_blue_offer_dict(
    offer_id,
    msku,
    feed_id=1000,
):
    return {
        'msku': msku,
        'feed_id': feed_id,
        'offer_id': offer_id,
        'session_id': 30,
        'offer': make_offer_proto_str(
            price=3000,
            is_blue_offer=True,
            ware_md5=create_ware_md5(0.1)
        ),
        'uc': make_uc_proto_str(model_id=125),
    }


@pytest.fixture(scope="module")
def source_blue_offers_raw():
    return [
        make_raw_blue_offer_dict(msku=111, offer_id="offer1"),
        make_raw_blue_offer_dict(msku=112, offer_id="offer2"),
        make_raw_blue_offer_dict(msku=113, offer_id="offer3"),
    ]


@pytest.fixture(scope="module")
def source_msku_contex():
    return [
        make_msku_contex_dict(msku=111, feed_id=BERU_FEED, shop_id=BERU_SHOP, title="msku1"),
        make_msku_contex_dict(msku=112, feed_id=BERU_FEED, shop_id=BERU_SHOP, title="msku2"),
        make_msku_contex_dict(msku=113, feed_id=BERU_FEED, shop_id=BERU_SHOP, title="msku3"),
    ]


@pytest.fixture(scope="module")
def source_ir_by_msku():
    return [
        {'msku': 111, 'is_not_tsar': True},
        {'msku': 112, 'is_not_tsar': False},
        # no msku=113
    ]


@pytest.fixture(scope="module")
def or3_config_data():
    home_dir = get_yt_prefix()
    ir_by_msku_table = ypath_join(home_dir, 'in', 'blue', 'ir_by_msku', 'recent')
    return {
        'yt': {
            'home_dir': home_dir,
            'yt_ir_by_msku_table': ir_by_msku_table,
        },
        'misc': {
            'blue_offers_enabled': 'true',
        }
    }


@pytest.fixture(scope="module")
def or3_config(or3_config_data):
    return Or3Config(**or3_config_data)


@pytest.fixture(scope="module")
def source_yt_tables(
    yt_server,
    or3_config,
    source_blue_offers_raw,
    source_msku_contex,
    source_ir_by_msku,
):
    yt_home_path = or3_config.options['yt']['home_dir']
    return {
        'offers_raw': OffersRawTable(
            yt_stuff=yt_server,
            path=ypath_join(yt_home_path, 'mi3', MI3_TYPE, GENERATION, 'work', 'offers_raw'),
            data=[]
        ),
        'offer2pic_unsorted': Offer2PicTable(
            yt_stuff=yt_server,
            path=ypath_join(yt_home_path, 'mi3', MI3_TYPE, GENERATION, 'work', 'offer2pic_unsorted'),
            data=[]
        ),
        'offer2model_unsorted': Offers2ModelTable(
            yt_stuff=yt_server,
            path=ypath_join(yt_home_path, 'mi3', MI3_TYPE, GENERATION, 'work', 'offer2model_unsorted'),
            data=[]
        ),
        'offer2param_unsorted': Offers2ParamTable(
            yt_stuff=yt_server,
            path=ypath_join(yt_home_path, 'mi3', MI3_TYPE, GENERATION, 'work', 'offer2param_unsorted'),
            data=[]
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
        'ir_by_msku': IrByMskuTable(
            yt_stuff=yt_server,
            path=ypath_join(yt_home_path, 'in', 'blue', 'ir_by_msku', 'recent'),
            data=source_ir_by_msku,
        ),
    }


@pytest.fixture(scope="module")
def create_source_yt_tables(yt_server, source_yt_tables):
    yt_client = yt_server.get_yt_client()

    for table in source_yt_tables.values():
        table.create()
        path = table.get_path()
        assert_that(yt_client.exists(path), "Table {} doesn\'t exist".format(path))


@pytest.fixture(scope="module")
def main_idx(yt_server, or3_config, create_source_yt_tables, tovar_tree):
    yt_home_path = ypath_join(or3_config.options['yt']['home_dir'])
    resources = {
        'config': or3_config,
        'in_picrobot_success': PicrobotSuccessTable(
            yt_stuff=yt_server,
            path=ypath_join(yt_home_path, 'in', 'picrobot', 'success', 'recent'),
            data=[]
        ),
        'tovar_tree_pb': TovarTreePb(tovar_tree),
    }
    with Or3MainIdxTestEnv(yt_server, GENERATION, MI3_TYPE, COUNT_SHARDS, HALF_MODE, **resources) as mi:
        mi.verify()
        mi.execute()
        yield mi


@pytest.mark.parametrize("offer_id, expected", [
    ("offer1", True),
    ("offer2", False),
    ("offer3", False),
])
def test_is_not_tsar_property(main_idx, offer_id, expected):
    actual = main_idx.outputs['offers_by_offer_id'][offer_id]['offer']
    assert_that(
        actual.get('is_not_tsar', False),
        equal_to(expected),
        offer_id,
    )
