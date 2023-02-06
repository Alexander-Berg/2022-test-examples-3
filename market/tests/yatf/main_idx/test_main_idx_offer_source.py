# coding: utf-8
"""
    Проверяем, что main-idx выставляет источники офферов
"""

import pytest
import datetime

from hamcrest import all_of, assert_that, has_entries

from yt.wrapper import ypath_join

from market.idx.pylibrary.offer_flags.flags import OfferFlags
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
from market.idx.yatf.resources.tovar_tree_pb import (
    MboCategory,
    TovarTreePb,
)
from market.proto.feedparser.deprecated.OffersData_pb2 import Offer
from market.idx.offers.yatf.resources.offers_indexer.genlog_offers_table import OfferData2GenlogProtobuf


GENERATION = datetime.datetime.now().strftime('%Y%m%d_%H%M')
MI3_TYPE = 'main'
COUNT_SHARDS = 1

ROOT_CATEGORY = 90401
BLUE_FEED_ID = 1
BLUE_SHOP_ID = 2


def offers():
    offer = Offer(
        binary_price={'price': 10000},
        ware_md5=create_ware_md5(0.1),
        offer_flags64=OfferFlags.BLUE_OFFER.value,
        market_category_id=ROOT_CATEGORY,
        HasDelivery=True,
        pickup='false',
        store='false',
        title='blue offer',
        cpa=4,
        feed_id=BLUE_FEED_ID,
        yx_shop_offer_id='market_blue_offer',
    )
    offer.genlog.CopyFrom(OfferData2GenlogProtobuf(offer))

    offers = [{
        'feed_id': BLUE_FEED_ID,
        'offer_id': 'market_blue_offer',
        'session_id': 0,
        'offer': offer.SerializeToString(),
    }]
    return offers


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
        },
        'misc': {
            'fill_offer_source': 'true'
        }
    }


@pytest.yield_fixture(scope="module")
def or3_config(or3_config_data):
    return Or3Config(**or3_config_data)


@pytest.fixture(scope="module")
def shops_dat():
    return ShopsDat([{
        'datafeed_id': BLUE_FEED_ID,
        'shop_id': BLUE_SHOP_ID,
        'cpa': 'REAL',
        'cpc': 'NO',
    }])


@pytest.yield_fixture(scope="module")
def source_yt_tables(yt_server, or3_config):
    yt_home_path = or3_config.options['yt']['home_dir']
    return {
        'offers_raw': OffersRawTable(
            yt_stuff=yt_server,
            path=ypath_join(yt_home_path, 'mi3', MI3_TYPE, GENERATION, 'work', 'offers_raw'),
            data=offers()
        ),
        'offer2param_unsorted': Offers2ParamTable(
            yt_stuff=yt_server,
            path=ypath_join(yt_home_path, 'mi3', MI3_TYPE, GENERATION, 'work', 'offer2param_unsorted'),
            data={}
        ),
        'offer2pic_unsorted': Offer2PicTable(
            yt_stuff=yt_server,
            path=ypath_join(yt_home_path, 'mi3', MI3_TYPE, GENERATION, 'work', 'offer2pic_unsorted'),
            data={}
        ),
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
            yt_stuff=yt_server,
            path=ypath_join(yt_home_path, 'in', 'picrobot', 'success', 'recent'),
            data=[]
        ),
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


def test_offer_source(main_idx):
    assert_that(
        main_idx.outputs['offers_by_offer_id'],
        has_entries({
            'market_blue_offer': all_of(
                has_entries({
                    'offer_id': 'market_blue_offer',
                    'genlog': has_entries({
                        'title': 'blue offer',
                        'offer_source': 'market.cpa'
                    }),
                }),
            )
        })
    )
