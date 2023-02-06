# coding: utf-8
'''
Тест проверяет генерацию мета информации о картинки для оффера c скартинкой от нового пикробота.
'''

import datetime
import pytest
from hamcrest import assert_that, has_items

from yt.wrapper import ypath_join

from market.idx.yatf.resources.shops_dat import ShopsDat
from market.idx.yatf.resources.yt_stuff_resource import (
    get_yt_prefix
)
from market.idx.offers.yatf.test_envs.main_idx import Or3MainIdxTestEnv
from market.idx.offers.yatf.resources.idx_prepare_offers.config import Or3Config
from market.idx.offers.yatf.resources.idx_prepare_offers.data_raw_tables import (
    OffersRawTable,
    Offers2ParamTable,
)
from market.idx.generation.yatf.resources.prepare.in_picrobot_success import PicrobotSuccessTable
from market.idx.generation.yatf.resources.prepare.offer2pic import Offer2PicTable
from market.idx.generation.yatf.utils.common import create_ware_md5
from market.idx.generation.yatf.utils.fixtures import (
    make_offer_proto_str,
    make_uc_proto_str,
)
from market.idx.yatf.resources.tovar_tree_pb import (
    MboCategory,
    TovarTreePb,
)
from market.proto.content.pictures_pb2 import Picture

GENERATION = datetime.datetime.now().strftime('%Y%m%d_%H%M')
MI3_TYPE = 'main'
COUNT_SHARDS = 1


SIMPLE_OFFER_ID = 'offer'
SIMPLE_OFFER = {
    'feed_id': 1,
    'offer_id': SIMPLE_OFFER_ID,
    'session_id': 1,
    'offer': make_offer_proto_str(
        price=2,
        ware_md5=create_ware_md5(0.3),
        picURLS='https://dyatkovo.ru/upload/iblock/50a/50ad2c44b6c814e9088919d34a7ac6d4.jpg\thttps://dyatkovo.ru/upload/iblock/208/208c693f14bdc89d28c1802d194ace85.jpg',
        picUrlIds=['picd2531cbaeac135f8644176e90fe86115', 'picbc1dbcd2dc8bd400296ca5990444c331'],
    ),
    'uc': make_uc_proto_str(model_id=1),
}


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
def or3_config_data():
    home_dir = get_yt_prefix()
    return {
        'yt': {
            'home_dir': home_dir,
        }
    }


@pytest.yield_fixture(scope="module")
def or3_config(or3_config_data):
    return Or3Config(**or3_config_data)


@pytest.fixture(scope="module")
def shops_dat():
    return ShopsDat([{"datafeed_id": 1}])


@pytest.yield_fixture(scope="module")
def source_yt_tables(yt_server,
                     or3_config):
    yt_home_path = or3_config.options['yt']['home_dir']
    return {
        'offers_raw': OffersRawTable(
            yt_stuff=yt_server,
            path=ypath_join(yt_home_path, 'mi3', MI3_TYPE, GENERATION, 'work', 'offers_raw'),
            data=[SIMPLE_OFFER]
        ),
        'offer2param_unsorted': Offers2ParamTable(
            yt_stuff=yt_server,
            path=ypath_join(yt_home_path, 'mi3', MI3_TYPE, GENERATION, 'work', 'offer2param_unsorted'),
            data={}
        ),
        'offer2pic_unsorted': Offer2PicTable(
            yt_stuff=yt_server,
            path=ypath_join(yt_home_path, 'mi3', MI3_TYPE, GENERATION, 'work', 'offer2pic_unsorted'),
            data=[
                {'id': 'picbc1dbcd2dc8bd400296ca5990444c331', 'feed_id': 1, 'offer_id': SIMPLE_OFFER_ID, 'descr_url': 'descr'},
                {'id': 'picd2531cbaeac135f8644176e90fe86115', 'feed_id': 1, 'offer_id': SIMPLE_OFFER_ID, 'descr_url': 'descr'},
            ]
        ),
        'in_picrobot_success': PicrobotSuccessTable(
            yt_stuff=yt_server,
            path=ypath_join(yt_home_path, 'in', 'picrobot', 'success', 'recent'),
            data=[
                {
                    'id': 'picbc1dbcd2dc8bd400296ca5990444c331',
                    'is_good_size_pic': True,
                    'pic': Picture(width=900, height=1200, crc='thisiscrc').SerializeToString()
                },
                {
                    'id': 'picd2531cbaeac135f8644176e90fe86115',
                    'is_good_size_pic': True,
                    'pic': Picture(width=800, height=600, crc='thisiscrc').SerializeToString()
                },
            ]
        ),
    }


@pytest.yield_fixture(scope="module")
def main_idx(yt_server, or3_config, source_yt_tables, shops_dat, tovar_tree):
    for table in source_yt_tables.values():
        table.create()
        path = table.get_path()
        assert yt_server.get_yt_client().exists(path), "Table {} doesn\'t exist".format(path)

    yt_home_path = or3_config.options['yt']['home_dir']
    resources = {
        'config': or3_config,
        'offer2pic': Offer2PicTable(
            yt_stuff=yt_server,
            path=ypath_join(yt_home_path, 'mi3', MI3_TYPE, GENERATION, 'work', 'offer2pic'),
            data=[]
        ),
        'shops_dat': shops_dat,
        'tovar_tree_pb': TovarTreePb(tovar_tree),
    }

    with Or3MainIdxTestEnv(yt_server, GENERATION, MI3_TYPE, COUNT_SHARDS, False, **resources) as mi:
        mi.verify()
        mi.execute()
        yield mi


def test_offers_pictures(main_idx):
    assert len(main_idx.outputs['offers_by_offer_id']) == 1
    assert 'pic' in main_idx.outputs['offers_by_offer_id'][SIMPLE_OFFER_ID]
    assert len(main_idx.outputs['offers_by_offer_id'][SIMPLE_OFFER_ID]['pic']) == 2
    assert_that(
        main_idx.outputs['offers_by_offer_id'][SIMPLE_OFFER_ID]['pic'],
        has_items(
            {'width': 900, 'height': 1200, 'crc': 'thisiscrc'},
            {'width': 800, 'height': 600, 'crc': 'thisiscrc'},
        ),
    )
