# coding: utf-8
'''
Тест проверяет получение информации о картинке из пикробота.
'''
import datetime
import logging
import pytest

from yt.wrapper import ypath_join

from market.idx.offers.yatf.test_envs.main_idx import Or3MainIdxTestEnv
from market.idx.offers.yatf.resources.idx_prepare_offers.config import Or3Config
from market.idx.offers.yatf.resources.idx_prepare_offers.data_raw_tables import (
    OffersRawTable,
    Offers2ParamTable,
)
from market.idx.generation.yatf.resources.prepare.in_picrobot_success import PicrobotSuccessTable
from market.idx.generation.yatf.resources.prepare.offer2pic import Offer2PicTable
from market.idx.yatf.resources.tovar_tree_pb import (
    MboCategory,
    TovarTreePb,
)
from market.idx.generation.yatf.utils.common import create_ware_md5
from market.idx.generation.yatf.utils.fixtures import (
    make_offer_proto_str
)
from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix
from market.proto.content.pictures_pb2 import Picture

logger = logging.getLogger()

GENERATION = datetime.datetime.now().strftime('%Y%m%d_%H%M')
MI3_TYPE = 'main'
COUNT_SHARDS = 2
HALF_MODE = False
FEED_ID = 1
OFFER_ID = 'offer_1'
PIC_ID = '0GDMbKY19ULEtDjcmNPVFQ'
PIC_URL='https://yandex.net/image.jpg'

OFFERS = [{
    'feed_id': FEED_ID,
    'offer_id': OFFER_ID,
    'offer': make_offer_proto_str(
        price=1,
        ware_md5=create_ware_md5(0.1),
        picURLS=PIC_URL,
        picUrlIds=[PIC_ID],
    )
}]

IN_PICROBOT_SUCCESS = [
    {
        'id': PIC_ID,
        'is_good_size_pic': True,
        'pic': Picture(
            width=800,
            height=600,
            crc='thisiscrc'
        ).SerializeToString()
    }
]


OFFER_2_PIC = [
    {
        'id': PIC_ID,
        'feed_id': FEED_ID,
        'offer_id': OFFER_ID
    }
]


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
        }
    }


@pytest.yield_fixture(scope="module")
def or3_config(or3_config_data):
    return Or3Config(**or3_config_data)


@pytest.yield_fixture(scope="module")
def source_yt_tables(yt_server, or3_config):
    yt_home_path = or3_config.options['yt']['home_dir']
    return {
        'offers_raw': OffersRawTable(
            yt_stuff=yt_server,
            path=ypath_join(yt_home_path, 'mi3', MI3_TYPE, GENERATION, 'work', 'offers_raw'),
            data=OFFERS
        ),
        'offer2param_unsorted': Offers2ParamTable(
            yt_stuff=yt_server,
            path=ypath_join(yt_home_path, 'mi3', MI3_TYPE, GENERATION, 'work', 'offer2param_unsorted'),
            data=[]
        ),
        'offer2pic_unsorted': Offer2PicTable(
            yt_stuff=yt_server,
            path=ypath_join(yt_home_path, 'mi3', MI3_TYPE, GENERATION, 'work', 'offer2pic_unsorted'),
            data=OFFER_2_PIC
        ),
        'in_picrobot_success': PicrobotSuccessTable(
            yt_stuff=yt_server,
            path=ypath_join(yt_home_path, 'in', 'picrobot', 'success', 'recent'),
            data=IN_PICROBOT_SUCCESS
        ),
    }


@pytest.yield_fixture(scope="module")
def main_idx(yt_server, source_yt_tables, or3_config, tovar_tree):
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
        'tovar_tree_pb': TovarTreePb(tovar_tree),
    }
    with Or3MainIdxTestEnv(yt_server, GENERATION, MI3_TYPE, COUNT_SHARDS, HALF_MODE,  **resources) as mi:
        mi.verify()
        mi.execute()
        yield mi


def test_offer_pic(main_idx, yt_server, or3_config):
    assert main_idx.outputs['offers_by_offer_id'][OFFER_ID]['pic'] == [{'width': 800, 'height': 600, 'crc': 'thisiscrc'}]
